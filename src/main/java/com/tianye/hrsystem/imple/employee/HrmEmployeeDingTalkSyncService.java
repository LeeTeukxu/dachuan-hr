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
        // 钉钉权限缺失收集器(本次同步内传递，避免多租户并发下用实例字段)
        PermissionIssues permIssues = new PermissionIssues();

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
            loadAllDepts(token, 1L, dingDepts, permIssues);
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
                List<OapiV2UserListResponse.ListUserResponse> users = loadDeptUsers(token, dept.getDeptId(), permIssues);
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

            // 预统计：钉钉拉到多少人、其中多少人带手机号(用于"缺手机号字段权限"启发式)
            int dingWithMobile = 0;
            for (OapiV2UserListResponse.ListUserResponse u : dingUserMap.values()) {
                if (u.getMobile() != null && !u.getMobile().trim().isEmpty()) {
                    dingWithMobile++;
                }
            }

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

            fillPermissionTips(result, permIssues, dingUserMap.size(), dingWithMobile);

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

    private void loadAllDepts(String token, Long parentDeptId, List<OapiV2DepartmentListsubResponse.DeptBaseResponse> collector,
                              PermissionIssues permIssues) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
        OapiV2DepartmentListsubRequest request = new OapiV2DepartmentListsubRequest();
        request.setDeptId(parentDeptId);
        // 失败重试3次；权限类错误(errcode=88/60011/未开通)不重试、记录缺失权限后跳过该分支，避免整棵树崩掉导致一个员工都同步不到
        OapiV2DepartmentListsubResponse response = null;
        for (int retry = 0; retry < 3; retry++) {
            response = client.execute(request, token);
            if (response != null && response.isSuccess() && response.getResult() != null) {
                break;
            }
            String errmsg = response == null ? "返回null" : ("errcode=" + response.getErrcode() + "," + response.getErrmsg());
            if (isDingPermissionError(response == null ? 0 : response.getErrcode(),
                    response == null ? "" : response.getErrmsg())) {
                permIssues.addFromError(errmsg);
                logger.warn("拉取钉钉子部门(parentId={})失败：钉钉权限未开通，跳过该分支。{}", parentDeptId, errmsg);
                return;
            }
            logger.warn("拉取钉钉子部门(parentId={})第{}次失败({})", parentDeptId, retry + 1, errmsg);
            if (retry == 2) {
                throw new RuntimeException("拉取钉钉部门(parentId=" + parentDeptId + ")连续3次失败: " + errmsg);
            }
            Thread.sleep(500L);
        }
        for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : response.getResult()) {
            collector.add(dept);
            loadAllDepts(token, dept.getDeptId(), collector, permIssues);
        }
    }

    private List<OapiV2UserListResponse.ListUserResponse> loadDeptUsers(String token, Long deptId, PermissionIssues permIssues) throws Exception {
        List<OapiV2UserListResponse.ListUserResponse> users = new ArrayList<>();
        long cursor = 0L;
        while (true) {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/list");
            OapiV2UserListRequest request = new OapiV2UserListRequest();
            request.setDeptId(deptId);
            request.setCursor(cursor);
            request.setSize(100L);
            OapiV2UserListResponse response = client.execute(request, token);
            if (response == null || !response.isSuccess() || response.getResult() == null) {
                String err = response == null ? "返回null" : "errcode=" + response.getErrcode() + "," + response.getErrmsg();
                // 该接口需通讯录成员读权限，普通应用无权限时100%失败；权限类错误记录缺失权限便于提示操作者，其余不再重试浪费钉钉配额
                if (isDingPermissionError(response == null ? 0 : response.getErrcode(),
                        response == null ? "" : response.getErrmsg())) {
                    permIssues.addFromError(err);
                    logger.warn("拉取钉钉部门人员(deptId={})失败：钉钉权限未开通，跳过该部门。{}", deptId, err);
                } else {
                    logger.warn("拉取钉钉部门人员(deptId={})失败，跳过该部门(无接口权限将不再重试): {}", deptId, err);
                }
                return users;
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

    /**
     * 钉钉同步权限缺失收集器(单次 syncRoster 内传递；不落实例字段避免多租户并发串扰)。
     * scope 用 LinkedHashSet 保序去重；fieldMobile(手机号字段)缺失钉钉不报错，由主流程启发式补入。
     */
    private static class PermissionIssues {
        final Set<String> scopes = new LinkedHashSet<>();
        final List<String> rawMsgs = new ArrayList<>();

        /** 从钉钉报错文本(errmsg/sub_msg)里提取缺失权限点 scope 并登记 */
        void addFromError(String errMsg) {
            if (errMsg == null) {
                return;
            }
            rawMsgs.add(errMsg);
            // 钉钉错误形如: subcode=60011,submsg=应用尚未开通所需的权限：[qyapi_get_department_member],点击链接申请...
            // 也可能 errmsg 包裹: ding talk error[subcode=60011,submsg=...[qyapi_xxx]...申请...]
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[([a-zA-Z_][a-zA-Z0-9_]*)\\]")
                    .matcher(errMsg);
            while (m.find()) {
                String scope = m.group(1);
                // 只收集典型的钉钉权限 scope，避免把部门名/其它方括号内容误收
                if (scope.startsWith("qyapi_") || scope.startsWith("field") || scope.startsWith("Contact")
                        || scope.contains("Mobile") || scope.contains("mobile")) {
                    scopes.add(scope);
                }
            }
        }

        void addScope(String scope) {
            if (scope != null && !scope.isEmpty()) {
                scopes.add(scope);
            }
        }

        boolean isEmpty() {
            return scopes.isEmpty();
        }
    }

    /** 判断是否为钉钉"权限未开通/无权限"类错误(errmsg 含 60011 / 尚未开通所需权限 / 无权限调用) */
    private boolean isDingPermissionError(long errcode, String errmsg) {
        if (errcode == 88) {
            return true;
        }
        if (errmsg == null) {
            return false;
        }
        return errmsg.contains("60011") || errmsg.contains("尚未开通所需") || errmsg.contains("尚未开通所需权限")
                || errmsg.contains("无权限调用") || errmsg.contains("无权限");
    }

    /** 权限点 → 人话展示名(未收录的原样保留) */
    private String dingScopeName(String scope) {
        switch (scope) {
            case "qyapi_get_department_list":
                return "通讯录部门信息读(拉取部门列表)";
            case "qyapi_get_department_member":
                return "通讯录部门成员读(拉取各部门员工列表)";
            case "qyapi_get_member":
                return "通讯录成员信息读(查单个员工详情)";
            case "qyapi_get_member_by_mobile":
            case "qyapi_get_member_by_moblie":
                return "按手机号查询成员(getbymobile)";
            case "fieldMobile":
            case "Contact.User.mobile":
                return "企业员工手机号信息(返回员工手机号字段)";
            case "fieldEmail":
                return "邮箱等个人信息(返回邮箱字段)";
            default:
                return scope;
        }
    }

    /** 从钉钉报错文本提取"申请开通权限"直达链接(形如 https://open-dev.dingtalk.com/appscope/apply?content=...#scope) */
    private String extractApplyUrl(String errMsg) {
        if (errMsg == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("https://open-dev\\.dingtalk\\.com/appscope/apply[^\\s,\"\\]]*").matcher(errMsg);
        return m.find() ? m.group(0) : null;
    }

    /** 把收集到的缺失权限组装成前端可见的 permissionTips 数组 */
    private List<Map<String, String>> assemblePermissionTips(PermissionIssues permIssues) {
        List<Map<String, String>> tips = new ArrayList<>();
        if (permIssues == null || permIssues.isEmpty()) {
            return tips;
        }
        List<String> rawMsgs = new ArrayList<>(new LinkedHashSet<>(permIssues.rawMsgs));
        for (String scope : permIssues.scopes) {
            // 找一条含该 scope 的原始报错，用于提取申请链接
            String hitRaw = null;
            for (String raw : rawMsgs) {
                if (raw.contains("[" + scope + "]")) {
                    hitRaw = raw;
                    break;
                }
            }
            String applyUrl = hitRaw == null ? null : extractApplyUrl(hitRaw);
            Map<String, String> tip = new LinkedHashMap<>();
            tip.put("scope", scope);
            tip.put("name", dingScopeName(scope));
            tip.put("effect", dingPermissionEffect(scope));
            tip.put("applyUrl", applyUrl == null ? "" : applyUrl);
            tip.put("guide", dingPermissionGuide(scope));
            tips.add(tip);
        }
        return tips;
    }

    /** 各权限缺失时的"现象"文案(供操作者理解缺了会怎样) */
    private String dingPermissionEffect(String scope) {
        switch (scope) {
            case "qyapi_get_department_list":
                return "无法读取钉钉部门树，同步将无法进行";
            case "qyapi_get_department_member":
                return "无法读取各部门员工，钉钉员工总数会显示为0，同步不到任何人";
            case "qyapi_get_member":
                return "无法读取单个员工详情(影响部分字段)，建议一并开通";
            case "qyapi_get_member_by_mobile":
                return "无法按手机号反查钉钉用户，手机号兜底匹配会失效";
            case "fieldMobile":
            case "Contact.User.mobile":
                return "员工手机号字段返回为空，已匹配到的大量员工会报\"未返回手机号，已跳过\"";
            default:
                return "该权限缺失会导致相关同步步骤失败";
        }
    }

    /** 各权限的后台开通指引(含"通讯录管理"子页这一步) */
    private String dingPermissionGuide(String scope) {
        switch (scope) {
            case "qyapi_get_department_list":
            case "qyapi_get_department_member":
            case "qyapi_get_member":
            case "qyapi_get_member_by_mobile":
                return "钉钉开放平台→企业内部开发→该应用→开发配置→权限管理，在接口权限列表找到并开通[" + dingScopeName(scope) + "]对应权限";
            case "fieldMobile":
            case "Contact.User.mobile":
                return "钉钉开放平台→企业内部开发→该应用→开发配置→权限管理→【通讯录管理】页，勾选\"企业员工手机号信息\"后点\"申请权限\"(手机号/邮箱属敏感字段，接口权限开了也拿不到，必须在此单独勾选)";
            default:
                return "钉钉开放平台→该应用→权限管理→搜索对应权限并开通";
        }
    }

    /** 主流程收尾：根据收集结果 + 手机号全空启发式，产出 permissionTips 写入 result */
    private void fillPermissionTips(Map<String, Object> result, PermissionIssues permIssues, int dingUserCount, int dingWithMobile) {
        // 启发式补 fieldMobile：能拉到人(dingUserCount>0) 但所有人手机号都为空 → 极可能是缺手机号字段权限。
        // 仅当"有人但0个带手机号"才触发，避免个别员工没录号(其余有号)时误报为权限问题。
        boolean allMobileEmpty = dingUserCount > 0 && dingWithMobile == 0;
        if (allMobileEmpty) {
            permIssues.addScope("fieldMobile");
        }
        List<Map<String, String>> tips = assemblePermissionTips(permIssues);
        result.put("permissionIssues", !tips.isEmpty());
        result.put("permissionTips", tips);
    }
}
