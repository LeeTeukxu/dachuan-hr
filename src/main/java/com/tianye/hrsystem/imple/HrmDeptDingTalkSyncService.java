package com.tianye.hrsystem.imple;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.request.OapiV2UserGetbymobileRequest;
import com.dingtalk.api.request.OapiV2UserListRequest;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserGetResponse;
import com.dingtalk.api.response.OapiV2UserGetbymobileResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import com.tianye.hrsystem.entity.po.HrmDept;
import com.tianye.hrsystem.entity.po.HrmDeptSyncConfig;
import com.tianye.hrsystem.mapper.HrmDeptSyncConfigMapper;
import com.tianye.hrsystem.service.IHrmDeptService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门配置-同步钉钉部门数据
 * 规则：按部门名称匹配 upsert，归属关系以钉钉为准覆盖(本地根节点自身不动)；
 * 全量模式 = 完全覆盖：本地不在钉钉的部门(根节点除外)删除，员工自动转移到根部门；
 * 分公司模式 = 命中排除关键字的部门(含子部门)跳过同步且本地一并删除(员工转移根部门)。
 */
@Service
public class HrmDeptDingTalkSyncService {

    private static final Logger logger = LoggerFactory.getLogger(HrmDeptDingTalkSyncService.class);

    public static final String CONFIG_KEY_EXCLUDE_NAMES = "dingtalk_exclude_names";

    @Autowired
    private IAccessToken tokenCreator;

    @Autowired
    private IHrmDeptService deptService;

    @Autowired
    private HrmDeptSyncConfigMapper deptSyncConfigMapper;

    @Autowired
    private com.tianye.hrsystem.service.employee.IHrmEmployeeService employeeService;

    @Autowired
    private com.tianye.hrsystem.service.IAdminMessageService adminMessageService;

    /**
     * 查询排除关键字配置
     */
    public String getExcludeNames() {
        HrmDeptSyncConfig config = deptSyncConfigMapper.selectOne(
                Wrappers.<HrmDeptSyncConfig>lambdaQuery().eq(HrmDeptSyncConfig::getConfigKey, CONFIG_KEY_EXCLUDE_NAMES));
        return config == null ? "" : Optional.ofNullable(config.getConfigValue()).orElse("");
    }

    /**
     * 保存排除关键字配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveExcludeNames(String excludeNames) {
        HrmDeptSyncConfig config = deptSyncConfigMapper.selectOne(
                Wrappers.<HrmDeptSyncConfig>lambdaQuery().eq(HrmDeptSyncConfig::getConfigKey, CONFIG_KEY_EXCLUDE_NAMES));
        if (config == null) {
            config = new HrmDeptSyncConfig()
                    .setConfigKey(CONFIG_KEY_EXCLUDE_NAMES)
                    .setConfigValue(excludeNames)
                    .setUpdateTime(LocalDateTime.now());
            deptSyncConfigMapper.insert(config);
        } else {
            config.setConfigValue(excludeNames).setUpdateTime(LocalDateTime.now());
            deptSyncConfigMapper.updateById(config);
        }
    }

    /**
     * 同步钉钉部门
     * @param syncType headquarters=全量 / branch=分公司模式(按排除关键字过滤)
     * @param excludeNames 排除关键字，逗号分隔
     */
    public Map<String, Object> syncDingTalkDept(String syncType, String excludeNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        try {
            String token = tokenCreator.Refresh();
            List<String> keywords = new ArrayList<>();
            if ("branch".equals(syncType)) {
                keywords = Arrays.stream((excludeNames == null ? "" : excludeNames).split("[,，]"))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            }
            // 本地部门索引: 名称 → 部门
            List<HrmDept> allLocalDepts = deptService.list();
            Map<String, HrmDept> localByName = allLocalDepts.stream()
                    .collect(Collectors.toMap(HrmDept::getName, d -> d, (a, b) -> a));
            // 本地根部门(顶级为0)：多个顶级时优先选公司节点(deptType=1)且创建最早的，保证选根稳定；
            // 本地没有任何部门时(如全新租户)允许同步，钉钉一级部门将作为顶级部门(parentId=0)插入
            HrmDept localRoot = deptService.list().stream()
                    .filter(d -> d.getParentId() == null || d.getParentId() == 0L)
                    .sorted(Comparator
                            .comparing((HrmDept d) -> Integer.valueOf(1).equals(d.getDeptType()) ? 0 : 1)
                            .thenComparing(HrmDept::getCreateTime,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                    .findFirst().orElse(null);

            // 递归拉取钉钉部门(排除关键字命中的部门及其子部门)
            List<OapiV2DepartmentListsubResponse.DeptBaseResponse> dingDepts = new ArrayList<>();
            loadDepts(token, 1L, keywords, dingDepts, warnings);
            int excludedCount = warnings.size();
            warnings.clear();

            // 钉钉部门名称(排除自身与其父) → 钉钉父名称，用于归属映射
            Map<Long, OapiV2DepartmentListsubResponse.DeptBaseResponse> dingById = new LinkedHashMap<>();
            for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : dingDepts) {
                dingById.put(dept.getDeptId(), dept);
            }

            int insertCount = 0;
            int updateCount = 0;
            // 逐个处理，名称→本地id 的映射随插入动态补充(父子可一次同步对齐)
            Map<String, Long> nameToLocalId = new HashMap<>();
            localByName.forEach((name, dept) -> nameToLocalId.put(name, dept.getDeptId()));
            // 同名部门索引：钉钉可能有多个同名部门(如多个"总经办")，本地同名行可被认领多次
            Map<String, List<HrmDept>> localByNameMulti = new HashMap<>();
            allLocalDepts.stream()
                    .filter(d -> d.getName() != null)
                    .forEach(d -> localByNameMulti.computeIfAbsent(d.getName().trim(), k -> new ArrayList<>()).add(d));
            Set<Long> claimedLocalDeptIds = new HashSet<>();
            for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : dingDepts) {
                String name = dept.getName();
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                Long rootId = localRoot == null ? null : localRoot.getDeptId();
                Long localParentId = resolveLocalParent(dept, dingById, nameToLocalId, rootId);
                // 同名部门(如多个"总经办")：优先认领未被本次同步占用的本地同名行，全部占用则新增
                HrmDept existing = localByNameMulti.getOrDefault(name, Collections.emptyList()).stream()
                        .filter(d -> !claimedLocalDeptIds.contains(d.getDeptId()))
                        .findFirst().orElse(null);
                if (existing != null) {
                    // 归属关系以钉钉为准覆盖；仅本地根节点自身不改动(它是树的挂载点)
                    boolean isLocalRoot = localRoot != null && existing.getDeptId().equals(localRoot.getDeptId());
                    if (!isLocalRoot && localParentId != null && !localParentId.equals(existing.getParentId())) {
                        existing.setParentId(localParentId);
                        deptService.updateById(existing);
                        updateCount++;
                    }
                    claimedLocalDeptIds.add(existing.getDeptId());
                } else {
                    HrmDept deptPo = new HrmDept();
                    deptPo.setName(name);
                    deptPo.setParentId(localParentId == null ? (localRoot == null ? 0L : localRoot.getDeptId()) : localParentId);
                    // 本地无根部门时，钉钉一级部门作为公司节点(deptType=1)，其余为部门(deptType=2)
                    boolean topLevelWithoutRoot = localRoot == null && dingById.get(dept.getParentId()) == null;
                    deptPo.setDeptType(topLevelWithoutRoot ? 1 : 2);
                    deptPo.setCode(deptService.generateCode(null));
                    deptService.save(deptPo);
                    nameToLocalId.put(name, deptPo.getDeptId());
                    localByName.put(name, deptPo);
                    claimedLocalDeptIds.add(deptPo.getDeptId());
                    insertCount++;
                }
            }

            // 同步后本地与钉钉对齐：
            // 全量模式 = 完全覆盖，本地不在钉钉的部门(根节点除外)删除，员工转移到根部门；
            // 分公司模式 = 命中排除关键字的部门(含子部门)删除，员工转移到根部门
            Set<String> dingKeptNames = dingDepts.stream()
                    .map(OapiV2DepartmentListsubResponse.DeptBaseResponse::getName)
                    .filter(n -> n != null && !n.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toSet());
            deleteLocalDeptsNotInDing(syncType, keywords, dingKeptNames, localRoot, warnings);

            // 同步部门后：计算该部门下员工(系统内)应归属部门的变更清单，随结果返回(本步只算不落库)
            Map<String, Object> empSync = collectEmployeeDeptChanges(token, dingDepts, warnings);
            result.put("empChangeCount", empSync.get("changeCount"));
            result.put("empChangeList", empSync.get("changeList"));
            result.put("empUnmatchedCount", empSync.get("unmatchedCount"));
            result.put("empUnmatchedList", empSync.get("unmatchedList"));

            result.put("dingCount", dingDepts.size());
            result.put("insertCount", insertCount);
            result.put("updateCount", updateCount);
            result.put("excludedCount", excludedCount);
            result.put("warnings", warnings);
        } catch (Exception ex) {
            logger.error("同步钉钉部门失败", ex);
            throw new RuntimeException("同步钉钉部门失败: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 删除本地需要清理的部门及其子部门，子树内的员工自动转移到本地根部门：
     * 全量模式删除不在钉钉的部门；分公司模式删除命中排除关键字的部门。本地根节点永不删除。
     */
    private void deleteLocalDeptsNotInDing(String syncType, List<String> keywords, Set<String> dingKeptNames,
                                           HrmDept localRoot, List<String> warnings) {
        List<HrmDept> allDepts = deptService.list();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (HrmDept d : allDepts) {
            childrenMap.computeIfAbsent(d.getParentId() == null ? 0L : d.getParentId(), k -> new ArrayList<>()).add(d.getDeptId());
        }
        Set<Long> toDelete = new LinkedHashSet<>();
        for (HrmDept d : allDepts) {
            if (localRoot != null && d.getDeptId().equals(localRoot.getDeptId())) {
                continue;
            }
            String name = d.getName() == null ? "" : d.getName().trim();
            boolean shouldDelete;
            if ("headquarters".equals(syncType)) {
                shouldDelete = !dingKeptNames.contains(name);
            } else {
                shouldDelete = keywords.stream().anyMatch(name::contains);
            }
            if (shouldDelete) {
                // 收集子树
                LinkedList<Long> stack = new LinkedList<>();
                stack.push(d.getDeptId());
                while (!stack.isEmpty()) {
                    Long id = stack.pop();
                    if (toDelete.add(id)) {
                        for (Long child : childrenMap.getOrDefault(id, Collections.emptyList())) {
                            stack.push(child);
                        }
                    }
                }
            }
        }
        if (toDelete.isEmpty()) {
            return;
        }
        Long movedToDeptId = localRoot == null ? 0L : localRoot.getDeptId();
        List<com.tianye.hrsystem.entity.po.HrmEmployee> empInDepts = employeeService.lambdaQuery()
                .in(com.tianye.hrsystem.entity.po.HrmEmployee::getDeptId, toDelete)
                .list();
        if (!empInDepts.isEmpty()) {
            long activeCount = empInDepts.stream().filter(e -> e.getIsDel() == null || e.getIsDel() == 0).count();
            for (com.tianye.hrsystem.entity.po.HrmEmployee emp : empInDepts) {
                emp.setDeptId(movedToDeptId);
            }
            employeeService.updateBatchById(empInDepts);
            warnings.add("本次删除的部门下有 " + empInDepts.size() + " 名员工（其中在职 "
                    + activeCount + " 名），已全部转移到[" + (localRoot == null ? "顶级" : localRoot.getName()) + "]，请自行调整归属");
        }
        deptService.removeByIds(toDelete);
        warnings.add("已删除本地命中排除关键字的部门 " + toDelete.size() + " 个（含子部门）");
    }

    /**
     * 解析钉钉部门的本地父部门id：父部门按名称映射，映射不到则挂到本地根部门
     */
    private Long resolveLocalParent(OapiV2DepartmentListsubResponse.DeptBaseResponse dept,
                                    Map<Long, OapiV2DepartmentListsubResponse.DeptBaseResponse> dingById,
                                    Map<String, Long> nameToLocalId, Long rootId) {
        OapiV2DepartmentListsubResponse.DeptBaseResponse parent = dingById.get(dept.getParentId());
        if (parent == null || parent.getName() == null) {
            // 父级是钉钉根部门(不在同步列表中) → 挂到本地根(本地无根部门时为顶级 0)
            return rootId == null ? 0L : rootId;
        }
        return nameToLocalId.getOrDefault(parent.getName(), rootId == null ? 0L : rootId);
    }

    /**
     * 确认落库：把前端回传的"员工→目标部门"变更清单批量更新到 hrm_employee.dept_id。
     * 入参 changes 项取 employeeId + targetDeptId(其余展示字段忽略)；校验部门存在、员工存在未删除后更新。
     * unmatched 为本批认不到/未匹配的员工(name/dingMobile/sysMobile/reason)，落库成功后若非空则发一条 type=208 系统通知(点开列明细)。
     * @return {appliedCount, skippedCount, skipped[]}
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyEmployeeDeptChanges(List<Map<String, String>> changes,
                                                        List<Map<String, String>> unmatched) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (changes == null || changes.isEmpty()) {
            result.put("appliedCount", 0);
            result.put("skippedCount", 0);
            result.put("skipped", new ArrayList<>());
            notifyUnmatchedDeptSync(unmatched);
            return result;
        }
        // 本地部门 id 集合(仅存在且未删除的部门才可挂员工)
        Set<Long> validDeptIds = deptService.list().stream()
                .map(HrmDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<com.tianye.hrsystem.entity.po.HrmEmployee> toUpdate = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();
        for (Map<String, String> item : changes) {
            String empIdStr = item == null ? null : item.get("employeeId");
            String deptIdStr = item == null ? null : item.get("targetDeptId");
            String name = item == null ? null : item.get("name");
            if (empIdStr == null || empIdStr.trim().isEmpty() || deptIdStr == null || deptIdStr.trim().isEmpty()) {
                skipped.add(mapOf2("name", name, "reason", "缺失员工或目标部门"));
                continue;
            }
            Long empId;
            Long deptId;
            try {
                empId = Long.parseLong(empIdStr.trim());
                deptId = Long.parseLong(deptIdStr.trim());
            } catch (NumberFormatException e) {
                skipped.add(mapOf2("name", name, "reason", "id格式错误"));
                continue;
            }
            if (!validDeptIds.contains(deptId)) {
                skipped.add(mapOf2("name", name, "reason", "目标部门不存在"));
                continue;
            }
            com.tianye.hrsystem.entity.po.HrmEmployee emp = employeeService.getById(empId);
            if (emp == null || (emp.getIsDel() != null && emp.getIsDel() != 0)) {
                skipped.add(mapOf2("name", name, "reason", "员工不存在或已删除"));
                continue;
            }
            if (deptId.equals(emp.getDeptId())) {
                continue; // 已一致，无需更新
            }
            emp.setDeptId(deptId);
            toUpdate.add(emp);
        }
        int applied = 0;
        if (!toUpdate.isEmpty()) {
            employeeService.updateBatchById(toUpdate);
            applied = toUpdate.size();
        }
        result.put("appliedCount", applied);
        result.put("skippedCount", skipped.size());
        result.put("skipped", skipped);
        // 落库成功后有未匹配员工 → 发一条 type=208 系统通知(供 HR 后续点开处理)
        notifyUnmatchedDeptSync(unmatched);
        return result;
    }

    /**
     * 发送"同步钉钉部门-员工未匹配提醒"系统通知(type=208)。
     * content 存 JSON：{"count":N,"items":[{"name","dingMobile","sysMobile","reason"},...]}，供前端通知中心点"查看"弹窗解析。
     * recipientUser=0 系统级，所有能看到人资通知的操作员可见；仅当存在未匹配时发送。
     */
    private void notifyUnmatchedDeptSync(List<Map<String, String>> unmatched) {
        try {
            if (unmatched == null || unmatched.isEmpty()) {
                return;
            }
            List<Map<String, String>> items = new ArrayList<>();
            for (Map<String, String> u : unmatched) {
                if (u == null) {
                    continue;
                }
                Map<String, String> item = new LinkedHashMap<>();
                item.put("name", u.get("name") == null ? "" : u.get("name"));
                // 钉钉手机号(旧字段 mobile 作为兼容来源)
                String dingMobile = u.get("dingMobile");
                if (dingMobile == null) {
                    dingMobile = u.get("mobile");
                }
                item.put("dingMobile", dingMobile == null ? "" : dingMobile);
                item.put("sysMobile", u.get("sysMobile") == null ? "" : u.get("sysMobile"));
                item.put("reason", u.get("reason") == null ? "" : u.get("reason"));
                items.add(item);
            }
            if (items.isEmpty()) {
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("count", items.size());
            payload.put("items", items);
            String content = com.alibaba.fastjson.JSON.toJSONString(payload);

            com.tianye.hrsystem.entity.po.AdminMessage message = new com.tianye.hrsystem.entity.po.AdminMessage();
            message.setTitle("同步钉钉部门-员工未匹配提醒");
            message.setContent(content);
            message.setLabel(8); // 人资
            message.setType(208); // HRM_DEPT_SYNC_UNMATCHED
            message.setCreateUser(0L); // 系统
            message.setRecipientUser(0L); // 系统级通知
            message.setIsRead(0);
            message.setCreateTime(LocalDateTime.now());
            adminMessageService.save(message);
            logger.info("发送部门同步未匹配通知: {} 名员工", items.size());
        } catch (Exception ex) {
            logger.warn("发送部门同步未匹配通知失败: {}", ex.getMessage());
        }
    }

    /**
     * 同步部门后计算"该部门下员工应归属部门的变更清单 + 未匹配清单"(仅计算，不落库)。
     * 认人规则(重名员工靠 dingtalk_user_id 唯一区分)：dingtalk_user_id 绑定优先，其次钉钉手机号与系统手机号一致兜底；
     * 不用姓名认人(同名不同人会认错刷错部门)。
     * 目标部门 = 员工在钉钉的"主部门"映射到本地部门；员工归属多个本地部门时用 user/get 精取主部门。
     * @return changeCount / changeList[{name,mobile,oldDept,newDept}] / unmatchedCount / unmatchedList[{name,dingMobile,sysMobile,reason}]
     */
    private Map<String, Object> collectEmployeeDeptChanges(
            String token,
            List<OapiV2DepartmentListsubResponse.DeptBaseResponse> dingDepts,
            List<String> warnings) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> changeList = new ArrayList<>();
        List<Map<String, String>> unmatchedList = new ArrayList<>();
        try {
            // 本地部门索引(含本次部门树同步落库后的新建部门)
            List<HrmDept> localDepts = deptService.list();
            Map<String, Long> localByName = localDepts.stream()
                    .filter(d -> d.getName() != null)
                    .collect(Collectors.toMap(HrmDept::getName, HrmDept::getDeptId, (a, b) -> a));
            Map<Long, String> localById = localDepts.stream()
                    .filter(d -> d.getDeptId() != null)
                    .collect(Collectors.toMap(HrmDept::getDeptId, d -> d.getName() == null ? "" : d.getName(), (a, b) -> a));
            Long rootId = localDepts.stream()
                    .filter(d -> d.getParentId() == null || d.getParentId() == 0L)
                    .findFirst().map(HrmDept::getDeptId).orElse(null);
            // 钉钉部门 id → 名称(用于把员工从属的钉钉部门 id 映射到本地部门)
            Map<Long, String> dingDeptIdToName = dingDepts.stream()
                    .filter(d -> d.getName() != null && d.getDeptId() != null)
                    .collect(Collectors.toMap(OapiV2DepartmentListsubResponse.DeptBaseResponse::getDeptId,
                            OapiV2DepartmentListsubResponse.DeptBaseResponse::getName, (a, b) -> a));

            // 本地员工索引
            List<com.tianye.hrsystem.entity.po.HrmEmployee> localEmps = employeeService.lambdaQuery()
                    .eq(com.tianye.hrsystem.entity.po.HrmEmployee::getIsDel, 0).list();
            Map<String, List<com.tianye.hrsystem.entity.po.HrmEmployee>> byUserId = localEmps.stream()
                    .filter(e -> e.getDingtalkUserId() != null && !e.getDingtalkUserId().isEmpty())
                    .collect(Collectors.groupingBy(com.tianye.hrsystem.entity.po.HrmEmployee::getDingtalkUserId));
            Map<String, List<com.tianye.hrsystem.entity.po.HrmEmployee>> byMobile = localEmps.stream()
                    .filter(e -> e.getMobile() != null && !e.getMobile().isEmpty())
                    .collect(Collectors.groupingBy(com.tianye.hrsystem.entity.po.HrmEmployee::getMobile));

            // 逐钉钉部门拉取员工并聚合 userid(去重)；多部门员工会在多个部门下重复返回
            Map<String, OapiV2UserListResponse.ListUserResponse> dingUserById = new LinkedHashMap<>();
            for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : dingDepts) {
                List<OapiV2UserListResponse.ListUserResponse> users = loadDeptUsers(token, dept.getDeptId());
                for (OapiV2UserListResponse.ListUserResponse u : users) {
                    if (u.getUserid() != null) {
                        dingUserById.putIfAbsent(u.getUserid(), u);
                    }
                }
            }

            for (OapiV2UserListResponse.ListUserResponse dingUser : dingUserById.values()) {
                String userid = dingUser.getUserid();
                String dingName = dingUser.getName();
                String dingMobile = dingUser.getMobile() == null ? "" : dingUser.getMobile().trim();

                // 认人：优先 dingtalk_user_id 精确绑定。userid 唯一命中直接用；
                // 同一 userid 在系统绑了多个档案(脏数据:常为在职+离职重录)时，用每个候选档案自己的手机号去钉钉
                // getbymobile 反查，能反查回本 userid 的档案即钉钉本人；仍无法唯一确认则进未匹配。
                com.tianye.hrsystem.entity.po.HrmEmployee emp = null;
                String reason = null;
                // 系统侧手机号(可能多个候选档案)只作展示参考，不参与认人；多个用顿号拼接
                String sysMobile = "";
                List<com.tianye.hrsystem.entity.po.HrmEmployee> uidHits = byUserId.get(userid);
                if (uidHits != null && !uidHits.isEmpty()) {
                    if (uidHits.size() == 1) {
                        emp = uidHits.get(0);
                    } else {
                        List<com.tianye.hrsystem.entity.po.HrmEmployee> mobileMatch = new ArrayList<>();
                        for (com.tianye.hrsystem.entity.po.HrmEmployee cand : uidHits) {
                            String candMobile = cand.getMobile() == null ? "" : cand.getMobile().trim();
                            if (candMobile.isEmpty()) {
                                continue;
                            }
                            String reverseUid = resolveDingUseridByMobile(token, candMobile);
                            if (userid.equals(reverseUid)) {
                                mobileMatch.add(cand);
                            }
                        }
                        sysMobile = joinMobiles(uidHits); // 展示参考：该 userid 在系统绑的所有档案手机号
                        if (mobileMatch.size() == 1) {
                            emp = mobileMatch.get(0);
                        } else if (mobileMatch.size() > 1) {
                            reason = "userid命中" + uidHits.size() + "个系统档案，其中" + mobileMatch.size()
                                    + "个档案手机号都能在钉钉反查回该userid，无法唯一确认";
                        } else {
                            reason = "userid命中" + uidHits.size() + "个系统档案，但各档案手机号均无法在钉钉反查回该userid";
                        }
                    }
                } else {
                    // 系统无档案绑定此 userid → 钉钉手机号在系统唯一命中兜底(不按姓名认人)
                    if (!dingMobile.isEmpty()) {
                        List<com.tianye.hrsystem.entity.po.HrmEmployee> mobHits = byMobile.get(dingMobile);
                        if (mobHits != null && mobHits.size() == 1) {
                            emp = mobHits.get(0);
                        } else if (mobHits != null && mobHits.size() > 1) {
                            sysMobile = joinMobiles(mobHits); // 展示参考：手机号相同的多条系统档案
                            reason = "手机号[" + dingMobile + "]在系统存在" + mobHits.size() + "条，无法确认";
                        } else {
                            reason = "系统无此钉钉手机号[" + (dingMobile.isEmpty() ? "空" : dingMobile) + "]";
                        }
                    } else {
                        reason = "钉钉未返回手机号，且系统无档案绑定该userid";
                    }
                }
                if (emp == null) {
                    unmatchedList.add(unmatchedOf(dingName, dingMobile, sysMobile,
                            reason == null ? "未绑定dingtalk_user_id" : reason));
                    continue;
                }
                String empMobile = emp.getMobile() == null ? "" : emp.getMobile().trim();

                // 目标部门：该员工在钉钉的主部门 映射到本地 deptId(单部门直接判定；多部门歧义才用 user/get 精取主部门)
                Long targetDeptId = resolveDingMainDept(token, userid, dingUser.getDeptIdList(),
                        dingDeptIdToName, localByName, localById, rootId);
                if (targetDeptId == null) {
                    unmatchedList.add(unmatchedOf(dingName, dingMobile, empMobile, "钉钉部门未在系统中建立(可先执行部门同步)"));
                    continue;
                }
                // 与系统当前部门比对(未删除员工才有 dept_id 归属；dept_id 为空的视为待归属)
                Long curDeptId = emp.getDeptId();
                String curDeptName = curDeptId == null ? "(未设置)" : localById.getOrDefault(curDeptId, String.valueOf(curDeptId));
                if (targetDeptId.equals(curDeptId)) {
                    continue; // 部门一致，无需变更
                }
                String newDeptName = localById.getOrDefault(targetDeptId, String.valueOf(targetDeptId));
                Map<String, String> item = new LinkedHashMap<>();
                item.put("employeeId", String.valueOf(emp.getEmployeeId()));
                item.put("name", emp.getEmployeeName() == null ? dingName : emp.getEmployeeName());
                item.put("mobile", empMobile);
                item.put("oldDept", curDeptName);
                item.put("newDept", newDeptName);
                item.put("targetDeptId", String.valueOf(targetDeptId));
                changeList.add(item);
            }

            result.put("changeCount", changeList.size());
            result.put("changeList", changeList);
            result.put("unmatchedCount", unmatchedList.size());
            result.put("unmatchedList", unmatchedList);
        } catch (Exception ex) {
            logger.warn("同步部门后计算员工部门变更清单失败(不影响部门树同步结果): {}", ex.getMessage());
            result.put("changeCount", 0);
            result.put("changeList", changeList);
            result.put("unmatchedCount", 0);
            result.put("unmatchedList", new ArrayList<>());
        }
        return result;
    }

    private Map<String, String> mapOf(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> m = new LinkedHashMap<>();
        if (v1 != null) { m.put(k1, v1); }
        if (v2 != null) { m.put(k2, v2); }
        if (v3 != null) { m.put(k3, v3); }
        return m;
    }

    private Map<String, String> mapOf2(String k1, String v1, String k2, String v2) {
        Map<String, String> m = new LinkedHashMap<>();
        if (v1 != null) { m.put(k1, v1); }
        if (v2 != null) { m.put(k2, v2); }
        return m;
    }

    /**
     * 构造一条"部门同步-员工未匹配"展示项(仅展示参考，不参与认人)。
     * 统一结构：{name(钉钉姓名), dingMobile(钉钉手机号), sysMobile(系统手机号,可能多个), reason}
     */
    private Map<String, String> unmatchedOf(String name, String dingMobile, String sysMobile, String reason) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name == null ? "" : name);
        m.put("dingMobile", dingMobile == null ? "" : dingMobile);
        m.put("sysMobile", sysMobile == null ? "" : sysMobile);
        m.put("reason", reason == null ? "" : reason);
        return m;
    }

    /**
     * 把多个系统档案的手机号用顿号去重拼接(仅作未匹配明细的展示参考)。
     */
    private String joinMobiles(List<com.tianye.hrsystem.entity.po.HrmEmployee> emps) {
        if (emps == null || emps.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (com.tianye.hrsystem.entity.po.HrmEmployee e : emps) {
            String mob = e.getMobile() == null ? "" : e.getMobile().trim();
            if (mob.isEmpty() || !seen.add(mob)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(mob);
        }
        return sb.toString();
    }

    /**
     * 解析某钉钉员工在系统中的目标部门(deptId)。
     * 员工从属部门列表 dingDeptIds(钉钉 id) 逐一映射到本地部门：
     *  - 恰好映射到 1 个本地部门 → 直接采用(该部门即目标)，不额外调接口；
     *  - 映射到多个本地部门(多部门员工) → 调 user/get 以其 deptIdList 主部门(首位)为准；
     *  - 映射不到 → null。
     */
    private Long resolveDingMainDept(String token, String userid, List<Long> dingDeptIds,
                                     Map<Long, String> dingDeptIdToName, Map<String, Long> localByName,
                                     Map<Long, String> localById, Long rootId) {
        try {
            // 把钉钉从属部门 id → 名称 → 本地部门 id，去重并去掉根部门
            LinkedHashSet<Long> candidates = new LinkedHashSet<>();
            if (dingDeptIds != null) {
                for (Long ddId : dingDeptIds) {
                    String ddName = dingDeptIdToName.get(ddId);
                    if (ddName == null) {
                        continue;
                    }
                    Long localId = localByName.get(ddName);
                    if (localId != null && (rootId == null || !localId.equals(rootId))) {
                        candidates.add(localId);
                    }
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            }
            // 多部门歧义：用 user/get 权威主部门(deptIdList 首位)判定
            OapiV2UserGetRequest req = new OapiV2UserGetRequest();
            req.setUserid(userid);
            OapiV2UserGetResponse resp = null;
            for (int retry = 0; retry < 3; retry++) {
                resp = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/get").execute(req, token);
                if (resp != null && resp.isSuccess() && resp.getResult() != null) {
                    break;
                }
                if (retry == 2) {
                    return candidates.iterator().next(); // 拿不到精确主部门则退回第一候选
                }
                Thread.sleep(500L);
            }
            if (resp == null || resp.getResult() == null || resp.getResult().getDeptIdList() == null) {
                return candidates.iterator().next();
            }
            for (Long mainDingId : resp.getResult().getDeptIdList()) {
                String mainName = dingDeptIdToName.get(mainDingId);
                Long localId = mainName == null ? null : localByName.get(mainName);
                if (localId != null && (rootId == null || !localId.equals(rootId))) {
                    return localId;
                }
            }
            return candidates.iterator().next();
        } catch (Exception ex) {
            logger.warn("解析钉钉员工目标部门失败 userid={}: {}", userid, ex.getMessage());
            return null;
        }
    }

    /**
     * 钉钉按手机号反查 userid(user/get_by_mobile)。用于仲裁"同一钉钉 userid 在系统绑了多个档案"时，
     * 判断哪个档案的手机号在钉钉里就是本人(能反查回该 userid)。查不到或接口异常返回 null。
     */
    private String resolveDingUseridByMobile(String token, String mobile) {
        if (token == null || mobile == null || mobile.isEmpty()) {
            return null;
        }
        try {
            OapiV2UserGetbymobileRequest req = new OapiV2UserGetbymobileRequest();
            req.setMobile(mobile);
            OapiV2UserGetbymobileResponse resp = null;
            for (int retry = 0; retry < 3; retry++) {
                resp = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/getbymobile").execute(req, token);
                if (resp != null && resp.isSuccess() && resp.getResult() != null) {
                    return resp.getResult().getUserid();
                }
                // errcode=60121 找不到该用户 → 直接返回 null，不重试
                if (resp != null && (resp.getErrcode() == null || resp.getErrcode() == 60121L)) {
                    return null;
                }
                if (retry == 2) {
                    logger.warn("钉钉按手机号反查userid失败 mobile={}: errcode={},{}",
                            mobile, resp == null ? null : resp.getErrcode(), resp == null ? "" : resp.getErrmsg());
                    return null;
                }
                Thread.sleep(500L);
            }
            return null;
        } catch (Exception ex) {
            logger.warn("钉钉按手机号反查userid异常 mobile={}: {}", mobile, ex.getMessage());
            return null;
        }
    }

    /**
     * 拉取某钉钉部门下的全部员工(分页)
     */
    private List<OapiV2UserListResponse.ListUserResponse> loadDeptUsers(String token, Long deptId) throws Exception {
        List<OapiV2UserListResponse.ListUserResponse> users = new ArrayList<>();
        long cursor = 0L;
        while (true) {
            OapiV2UserListRequest request = new OapiV2UserListRequest();
            request.setDeptId(deptId);
            request.setCursor(cursor);
            request.setSize(100L);
            OapiV2UserListResponse response = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/list").execute(request, token);
            // 该接口需企业账号权限，普通应用无权限时100%失败；不再重试浪费钉钉配额，直接跳过该部门
            if (response == null || !response.isSuccess() || response.getResult() == null) {
                String err = response == null ? "返回null" : "errcode=" + response.getErrcode() + "," + response.getErrmsg();
                logger.warn("拉取钉钉部门人员(deptId={})失败，跳过该部门(无接口权限将不再重试): {}", deptId, err);
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

    private void loadDepts(String token, Long parentDeptId, List<String> excludeKeywords,
                           List<OapiV2DepartmentListsubResponse.DeptBaseResponse> collector,
                           List<String> warnings) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
        OapiV2DepartmentListsubRequest request = new OapiV2DepartmentListsubRequest();
        request.setDeptId(parentDeptId);
        // 失败重试3次，仍失败则中止整个同步(静默返回会导致子树悄悄丢失)
        OapiV2DepartmentListsubResponse response = null;
        for (int retry = 0; retry < 3; retry++) {
            response = client.execute(request, token);
            if (response != null && response.isSuccess() && response.getResult() != null) {
                break;
            }
            String err = response == null ? "返回null" : "errcode=" + response.getErrcode() + "," + response.getErrmsg();
            logger.warn("拉取钉钉子部门(parentId={})第{}次失败({})", parentDeptId, retry + 1, err);
            if (retry == 2) {
                if (com.tianye.hrsystem.common.EmployeeNotInDingTalkException.isDingTalkPermissionError(err)) {
                    throw new RuntimeException(com.tianye.hrsystem.common.EmployeeNotInDingTalkException.DINGTALK_PERMISSION_GUIDANCE);
                }
                throw new RuntimeException("拉取钉钉部门(parentId=" + parentDeptId + ")连续3次失败: " + err);
            }
            Thread.sleep(500L);
        }
        for (OapiV2DepartmentListsubResponse.DeptBaseResponse dept : response.getResult()) {
            String name = dept.getName() == null ? "" : dept.getName().trim();
            boolean excluded = excludeKeywords.stream().anyMatch(name::contains);
            if (excluded) {
                warnings.add("部门[" + name + "]命中排除关键字，及其子部门已跳过");
                continue;
            }
            collector.add(dept);
            loadDepts(token, dept.getDeptId(), excludeKeywords, collector, warnings);
        }
    }
}
