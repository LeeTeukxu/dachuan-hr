package com.tianye.hrsystem.imple;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
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
