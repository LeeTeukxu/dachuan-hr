package com.tianye.hrsystem.imple.employee;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.request.OapiV2UserListRequest;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import com.tianye.hrsystem.entity.po.HrmDept;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.mapper.HrmDeptMapper;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工管理-同步钉钉员工(花名册)
 * 匹配规则：dingtalk_user_id 优先 → 手机号 → 姓名(唯一时兜底)；
 * 匹配不唯一或手机号缺失的一律跳过并报告，不做静默处理。
 * 字段口径：预检差异报告仅姓名/手机号/部门；姓名/手机号/部门/工号以系统为准绝不修改；
 * 源头字段(岗位/邮箱/入职日期/工作地点/userid)以钉钉为准覆盖(钉钉值为空不覆盖)；系统独有字段一律不动。
 */
@Service
public class HrmEmployeeDingTalkSyncService {

    private static final Logger logger = LoggerFactory.getLogger(HrmEmployeeDingTalkSyncService.class);

    @Autowired
    private IAccessToken tokenCreator;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    private HrmDeptMapper hrmDeptMapper;

    public Map<String, Object> syncRoster(boolean dryRun) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> fieldDiffs = new ArrayList<>();
        List<String> newEmployees = new ArrayList<>();

        try {
            String token = tokenCreator.Refresh();
            // 本地部门: 名称 → deptId
            List<HrmDept> localDepts = hrmDeptMapper.selectList(null);
            Map<String, Long> localDeptNameMap = localDepts.stream()
                    .collect(Collectors.toMap(HrmDept::getName, HrmDept::getDeptId, (a, b) -> a));
            Map<Long, String> localDeptIdNameMap = localDepts.stream()
                    .collect(Collectors.toMap(HrmDept::getDeptId, HrmDept::getName, (a, b) -> a));

            // 递归拉取钉钉全部部门
            List<OapiV2DepartmentListsubResponse.DeptBaseResponse> dingDepts = new ArrayList<>();
            loadAllDepts(token, 1L, dingDepts);
            Map<Long, String> dingDeptIdNameMap = dingDepts.stream()
                    .filter(d -> d.getName() != null)
                    .collect(Collectors.toMap(OapiV2DepartmentListsubResponse.DeptBaseResponse::getDeptId, OapiV2DepartmentListsubResponse.DeptBaseResponse::getName, (a, b) -> a));
            Set<String> seenDeptNames = new HashSet<>();
            for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : dingDepts) {
                if (dept.getName() != null && !localDeptNameMap.containsKey(dept.getName()) && seenDeptNames.add(dept.getName())) {
                    warnings.add("钉钉部门[" + dept.getName() + "]在系统中不存在，该部门下的员工将不关联部门(可先执行部门同步)");
                }
            }

            // 按部门拉取钉钉员工
            Map<String, OapiV2UserListResponse.ListUserResponse> dingUserMap = new LinkedHashMap<>();
            Map<String, Long> userDeptMap = new HashMap<>();
            for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : dingDepts) {
                List<OapiV2UserListResponse.ListUserResponse> users = loadDeptUsers(token, dept.getDeptId());
                for (OapiV2UserListResponse.ListUserResponse user : users) {
                    if (user.getUserid() == null || dingUserMap.containsKey(user.getUserid())) {
                        continue;
                    }
                    dingUserMap.put(user.getUserid(), user);
                    userDeptMap.put(user.getUserid(), localDeptNameMap.get(dept.getName()));
                }
            }

            // 本地员工索引
            List<HrmEmployee> localEmployees = employeeService.lambdaQuery()
                    .eq(HrmEmployee::getIsDel, 0).list();
            Map<String, List<HrmEmployee>> byUserId = localEmployees.stream()
                    .filter(e -> e.getDingtalkUserId() != null && !e.getDingtalkUserId().isEmpty())
                    .collect(Collectors.groupingBy(HrmEmployee::getDingtalkUserId));
            Map<String, List<HrmEmployee>> byMobile = localEmployees.stream()
                    .filter(e -> e.getMobile() != null && !e.getMobile().isEmpty())
                    .collect(Collectors.groupingBy(HrmEmployee::getMobile));
            Map<String, List<HrmEmployee>> byName = localEmployees.stream()
                    .filter(e -> e.getEmployeeName() != null)
                    .collect(Collectors.groupingBy(HrmEmployee::getEmployeeName));

            int updateCount = 0;
            int insertCount = 0;
            List<HrmEmployee> toInsert = new ArrayList<>();
            List<HrmEmployee> toUpdate = new ArrayList<>();

            for (OapiV2UserListResponse.ListUserResponse user : dingUserMap.values()) {
                String userid = user.getUserid();
                String name = user.getName();
                String mobile = user.getMobile() == null ? "" : user.getMobile().trim();
                if (mobile.isEmpty()) {
                    conflicts.add("钉钉员工[" + name + "](userid=" + userid + ")未返回手机号，已跳过");
                    continue;
                }
                // 钉钉侧手机号重复：只处理第一条，其余报告
                if (dingUserMap.values().stream().anyMatch(u -> u != user && userid.compareTo(u.getUserid()) > 0
                        && u.getMobile() != null && u.getMobile().trim().equals(mobile))) {
                    continue;
                }
                long sameMobileDing = dingUserMap.values().stream()
                        .filter(u -> u.getMobile() != null && u.getMobile().trim().equals(mobile)).count();
                if (sameMobileDing > 1) {
                    conflicts.add("钉钉存在多条手机号为[" + mobile + "]的员工，已跳过");
                    continue;
                }

                HrmEmployee matched = null;
                // 1. userid 绑定优先
                List<HrmEmployee> userIdHits = byUserId.get(userid);
                if (userIdHits != null && userIdHits.size() == 1) {
                    matched = userIdHits.get(0);
                }
                // 2. 手机号
                if (matched == null) {
                    List<HrmEmployee> mobileHits = byMobile.get(mobile);
                    if (mobileHits != null && mobileHits.size() == 1) {
                        matched = mobileHits.get(0);
                    } else if (mobileHits != null && mobileHits.size() > 1) {
                        conflicts.add("手机号[" + mobile + "]在系统中存在 " + mobileHits.size() + " 条记录，钉钉员工[" + name + "]未同步，请先处理系统内重复数据");
                        continue;
                    }
                }
                // 3. 姓名兜底(仅系统内唯一同名时)
                if (matched == null) {
                    List<HrmEmployee> nameHits = byName.get(name);
                    if (nameHits != null && nameHits.size() == 1) {
                        matched = nameHits.get(0);
                    } else if (nameHits != null && nameHits.size() > 1) {
                        conflicts.add("系统内存在 " + nameHits.size() + " 名[" + name + "]，钉钉员工(手机号" + mobile + ")无法确认对应关系，未同步");
                        continue;
                    }
                }

                Long deptId = userDeptMap.get(userid);
                if (matched == null) {
                    // 新增入库
                    HrmEmployee employee = new HrmEmployee();
                    employee.setEmployeeName(name);
                    employee.setMobile(mobile);
                    employee.setDingtalkUserId(userid);
                    employee.setDeptId(deptId);
                    employee.setPost(nonBlank(user.getTitle()));
                    employee.setEmail(nonBlank(user.getEmail()));
                    employee.setJobNumber(nonBlank(user.getJobNumber()));
                    employee.setWorkAddress(nonBlank(user.getWorkPlace()));
                    employee.setEntryTime(parseEntryTime(user.getHiredDate()));
                    employee.setStatus(1);
                    employee.setEntryStatus(1);
                    employee.setIsDel(0);
                    toInsert.add(employee);
                    newEmployees.add(name + "(" + mobile + ")");
                    insertCount++;
                } else {
                    /*
                     * 字段口径（2026-09-06 与客户确认）：
                     * - 预检差异报告：仅姓名/手机号/部门（系统值 vs 钉钉值），只提示不修改；
                     * - 不允许修改：姓名/手机号/部门/工号，一律以系统为准；
                     * - 源头字段（岗位/邮箱/入职日期/工作地点/userid）：以钉钉为准覆盖，钉钉值为空不覆盖；
                     * - 系统独有字段：一律不动。
                     */
                    boolean changed = false;
                    List<String> diffs = new ArrayList<>();
                    if (!userid.equals(matched.getDingtalkUserId())) {
                        matched.setDingtalkUserId(userid);
                        changed = true;
                    }
                    if (nonBlank(name) != null && !name.equals(matched.getEmployeeName())) {
                        diffs.add("姓名: 系统[" + matched.getEmployeeName() + "] 钉钉[" + name + "]");
                    }
                    if (!mobile.equals(matched.getMobile())) {
                        diffs.add("手机号: 系统[" + matched.getMobile() + "] 钉钉[" + mobile + "]");
                    }
                    if (deptId != null && !deptId.equals(matched.getDeptId())) {
                        diffs.add("部门: 系统[" + localDeptIdNameMap.getOrDefault(matched.getDeptId(), String.valueOf(matched.getDeptId()))
                                + "] 钉钉[" + dingDeptIdNameMap.getOrDefault(userDeptMap.get(userid), "") + "]");
                    }
                    if (!diffs.isEmpty()) {
                        fieldDiffs.add(matched.getEmployeeName() + "（钉钉手机号 " + mobile + "）：" + String.join("；", diffs));
                    }
                    if (nonBlank(user.getTitle()) != null && !user.getTitle().equals(matched.getPost())) {
                        matched.setPost(user.getTitle());
                        changed = true;
                    }
                    if (nonBlank(user.getEmail()) != null && !user.getEmail().equals(matched.getEmail())) {
                        matched.setEmail(user.getEmail());
                        changed = true;
                    }
                    if (nonBlank(user.getWorkPlace()) != null && !user.getWorkPlace().equals(matched.getWorkAddress())) {
                        matched.setWorkAddress(user.getWorkPlace());
                        changed = true;
                    }
                    LocalDate hiredDate = parseEntryTime(user.getHiredDate());
                    if (hiredDate != null && !hiredDate.equals(matched.getEntryTime())) {
                        matched.setEntryTime(hiredDate);
                        changed = true;
                    }
                    if (changed) {
                        toUpdate.add(matched);
                        updateCount++;
                    }
                }
            }

            result.put("dingCount", dingUserMap.size());
            result.put("insertCount", insertCount);
            result.put("updateCount", updateCount);
            result.put("newEmployees", newEmployees);
            result.put("fieldDiffs", fieldDiffs);
            result.put("conflicts", conflicts);
            result.put("warnings", warnings);
            result.put("dryRun", dryRun);

            if (!dryRun) {
                if (!toInsert.isEmpty()) {
                    employeeService.saveBatch(toInsert);
                }
                if (!toUpdate.isEmpty()) {
                    employeeService.updateBatchById(toUpdate);
                }
            }
        } catch (Exception ex) {
            logger.error("同步钉钉员工失败", ex);
            throw new RuntimeException("同步钉钉员工失败: " + ex.getMessage(), ex);
        }
        return result;
    }

    private void loadAllDepts(String token, Long parentDeptId, List<OapiV2DepartmentListsubResponse.DeptBaseResponse> collector) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
        OapiV2DepartmentListsubRequest request = new OapiV2DepartmentListsubRequest();
        request.setDeptId(parentDeptId);
        // 失败重试3次，仍失败抛异常中止(静默返回会漏部门/漏人)
        OapiV2DepartmentListsubResponse response = null;
        for (int retry = 0; retry < 3; retry++) {
            response = client.execute(request, token);
            if (response != null && response.isSuccess() && response.getResult() != null) {
                break;
            }
            String err = response == null ? "返回null" : "errcode=" + response.getErrcode() + "," + response.getErrmsg();
            logger.warn("拉取钉钉子部门(parentId={})第{}次失败({})", parentDeptId, retry + 1, err);
            if (retry == 2) {
                throw new RuntimeException("拉取钉钉部门(parentId=" + parentDeptId + ")连续3次失败: " + err);
            }
            Thread.sleep(500L);
        }
        for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : response.getResult()) {
            collector.add(dept);
            loadAllDepts(token, dept.getDeptId(), collector);
        }
    }

    private List<OapiV2UserListResponse.ListUserResponse> loadDeptUsers(String token, Long deptId) throws Exception {
        List<OapiV2UserListResponse.ListUserResponse> users = new ArrayList<>();
        long cursor = 0L;
        while (true) {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/list");
            OapiV2UserListRequest request = new OapiV2UserListRequest();
            request.setDeptId(deptId);
            request.setCursor(cursor);
            request.setSize(100L);
            // 失败重试3次，仍失败抛异常中止(静默返回会漏人，预检结果失真)
            OapiV2UserListResponse response = null;
            for (int retry = 0; retry < 3; retry++) {
                response = client.execute(request, token);
                if (response != null && response.isSuccess() && response.getResult() != null) {
                    break;
                }
                String err = response == null ? "返回null" : "errcode=" + response.getErrcode() + "," + response.getErrmsg();
                logger.warn("拉取钉钉部门人员(deptId={})第{}次失败({})", deptId, retry + 1, err);
                if (retry == 2) {
                    if (com.tianye.hrsystem.common.EmployeeNotInDingTalkException.isDingTalkPermissionError(err)) {
                        throw new RuntimeException(com.tianye.hrsystem.common.EmployeeNotInDingTalkException.DINGTALK_PERMISSION_GUIDANCE);
                    }
                    throw new RuntimeException("拉取钉钉部门人员(deptId=" + deptId + ")连续3次失败: " + err);
                }
                Thread.sleep(500L);
            }
            List<OapiV2UserListResponse.ListUserResponse> list = response.getResult().getList();
            if (list != null) {
                users.addAll(list);
            }
            if (response.getResult().getHasMore() == null || !response.getResult().getHasMore()) {
                break;
            }
            cursor = response.getResult().getNextCursor() == null ? cursor : response.getResult().getNextCursor();
        }
        return users;
    }

    private String nonBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private LocalDate parseEntryTime(Long hiredDate) {
        if (hiredDate == null || hiredDate <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(hiredDate).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
