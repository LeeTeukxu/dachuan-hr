package com.tianye.hrsystem.imple;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.common.EmployeeHolder;
import com.tianye.hrsystem.entity.bo.AddDeptBO;
import com.tianye.hrsystem.entity.bo.QueryDeptListBO;
import com.tianye.hrsystem.entity.bo.QueryEmployeeByDeptIdBO;
import com.tianye.hrsystem.entity.vo.DeptVO;
import com.tianye.hrsystem.entity.vo.DeptLeaderVO;
import com.tianye.hrsystem.entity.vo.QueryEmployeeListByDeptIdVO;
import com.tianye.hrsystem.entity.vo.SimpleHrmDeptVO;
import com.tianye.hrsystem.enums.DataAuthEnum;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.enums.MenuIdConstant;
import com.tianye.hrsystem.mapper.HrmDeptMapper;
import com.tianye.hrsystem.entity.po.HrmDept;
import com.tianye.hrsystem.entity.vo.DeptEmployeeVO;
import com.tianye.hrsystem.service.IHrmDeptService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import com.tianye.hrsystem.util.EmployeeUtil;
import com.tianye.hrsystem.util.RecursionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import cn.hutool.core.util.ObjectUtil;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmDeptServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月21日 20:44
 **/
@Service
public class HrmDeptServiceImpl extends BaseServiceImpl<HrmDeptMapper, HrmDept> implements IHrmDeptService {

    @Autowired
    private HrmDeptMapper deptMapper;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    private EmployeeUtil employeeUtil;

    @Override
    public void addOrUpdate(AddDeptBO AddDeptBo) {
        // 验证：不能将上级组织设置为自己
        if (AddDeptBo.getDeptId() != null && AddDeptBo.getDeptId().equals(AddDeptBo.getParentId())) {
            throw new CrmException(HrmCodeEnum.PARENT_DEPT_CANNOT_BE_SELF);
        }

        HrmDept hrmDept = BeanUtil.copyProperties(AddDeptBo, HrmDept.class);
        // 只有在新增时才重新生成code，编辑时保持原有code
        if (AddDeptBo.getDeptId() == null) {
            hrmDept.setCode(generateCode(null));
        } else {
            // 编辑时获取原有的code
            HrmDept existingDept = getById(AddDeptBo.getDeptId());
            if (existingDept != null) {
                hrmDept.setCode(existingDept.getCode());
                // 编制(planNum)若前端未传(null)，保留原值，避免编辑时丢失编制数据
                // （2026-09-10，4 部门统计编制真实化）
                if (AddDeptBo.getPlanNum() == null) {
                    hrmDept.setPlanNum(existingDept.getPlanNum());
                }
            } else {
                hrmDept.setCode(generateCode(null));
            }
        }
        saveOrUpdate(hrmDept);
        // 保存分管领导后，部门下所有员工的直属上级同步为该分管领导
        syncDeptLeaderToEmployees(hrmDept.getDeptId(), AddDeptBo.getLeaderEmployeeId());
    }

    @Override
    public DeptLeaderVO queryDeptLeader(Long deptId) {
        DeptLeaderVO leaderVO = new DeptLeaderVO();
        if (deptId == null) {
            return leaderVO;
        }
        HrmDept dept = getById(deptId);
        if (dept == null || dept.getLeaderEmployeeId() == null) {
            return leaderVO;
        }
        leaderVO.setLeaderEmployeeId(dept.getLeaderEmployeeId());
        HrmEmployee leader = employeeService.getById(dept.getLeaderEmployeeId());
        if (leader != null) {
            leaderVO.setLeaderName(leader.getEmployeeName());
        }
        return leaderVO;
    }

    /**
     * 分管领导变更时，把部门下未删除员工的直属上级统一改为该分管领导；分管领导为空不改动。
     * 分管领导本人也是部门员工时不改他自己的直属上级（自己不能是自己的上级）
     */
    private void syncDeptLeaderToEmployees(Long deptId, Long leaderEmployeeId) {
        if (deptId == null || leaderEmployeeId == null) {
            return;
        }
        employeeService.lambdaUpdate()
                .eq(HrmEmployee::getDeptId, deptId)
                .eq(HrmEmployee::getIsDel, 0)
                .ne(HrmEmployee::getEmployeeId, leaderEmployeeId)
                .set(HrmEmployee::getParentId, leaderEmployeeId)
                .update();
    }

    @Override
    public String generateCode(Long deptId) {
        Set<Integer> usedCodes = list().stream()
                .filter(dept -> deptId == null || !Objects.equals(dept.getDeptId(), deptId))
                .map(HrmDept::getCode)
                .map(this::parsePositiveCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int code = 1;
        while (usedCodes.contains(code)) {
            code++;
        }
        return String.valueOf(code);
    }

    private Integer parsePositiveCode(String code) {
        if (StrUtil.isBlank(code) || !code.matches("\\d+")) {
            return null;
        }
        try {
            int value = Integer.parseInt(code);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public DeptVO queryById(Long deptId) {
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.EMPLOYEE_MENU_ID);
        boolean exists = false;
        if (CollUtil.isNotEmpty(employeeIds)) {
            exists = employeeService.lambdaQuery().eq(HrmEmployee::getDeptId, deptId).in(HrmEmployee::getEmployeeId, employeeIds).exists();
        }
        if (exists) {
            return deptMapper.queryById(deptId, employeeIds);
        }
        return deptMapper.queryNoEmployeeDept(deptId);
    }

    @Override
    public List<DeptVO> queryTreeList(QueryDeptListBO QueryDeptListBo) {
        Integer dataAuthType = DataAuthEnum.ALL.getValue();
        List<DeptVO> DeptVoList = new ArrayList<>();
        if (EmployeeHolder.isHrmAdmin() || DataAuthEnum.ALL.getValue().equals(dataAuthType)) {
            String tree = "tree";
            if (tree.equals(QueryDeptListBo.getType()) || QueryDeptListBo.getType() == null) {
                List<DeptVO> deptList = deptMapper.queryList(QueryDeptListBo);
                if (StrUtil.isNotEmpty(QueryDeptListBo.getName())) {
                    List<HrmDept> list = lambdaQuery().select(HrmDept::getDeptId).like(HrmDept::getName, QueryDeptListBo.getName()).list();
                    for (HrmDept dept : list) {
                        DeptVoList.addAll(createTree1(dept.getDeptId(), deptList));
                    }
                } else {
                    DeptVoList = createTree(0L, deptList);
                }
            }
            String update = "update";
            if (update.equals(QueryDeptListBo.getType())) {
                List<HrmDept> deptList = list();
                DeptVoList = deptList.stream().map(dept -> {
                    DeptVO DeptVo = new DeptVO();
                    DeptVo.setDeptId(dept.getDeptId());
                    DeptVo.setName(dept.getName());
                    DeptVo.setParentId(dept.getParentId());
                    return DeptVo;
                }).collect(Collectors.toList());
                List<Long> ids = RecursionUtil.getChildList(DeptVoList, "parentId", QueryDeptListBo.getId(), "deptId", "deptId");
                ids.add(QueryDeptListBo.getId());
                DeptVoList.removeIf(dept -> ids.contains(dept.getDeptId()));
            }
        } else {
            if (dataAuthType != null) {
                DeptVoList = getDataAuthDeptList(QueryDeptListBo, dataAuthType);
            }
        }
        return DeptVoList;
    }

    /**
     * 部门数据权限搜索
     */
    private List<DeptVO> getDataAuthDeptList(QueryDeptListBO QueryDeptListBo, Integer dataAuthType) {
        List<DeptVO> DeptVoList = new ArrayList<>();
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpId(dataAuthType);
        if (CollUtil.isEmpty(employeeIds)) {
            return DeptVoList;
        }
        List<DeptVO> queryDept = deptMapper.queryDeptByEmpIds(employeeIds);
        Map<Long, DeptVO> deptIdMap = queryDept.stream().collect(Collectors.toMap(DeptVO::getDeptId, e -> e));
        Set<Long> deptIds = employeeUtil.queryDataAuthDeptId(dataAuthType);
        List<DeptVO> deptList = deptMapper.queryList(QueryDeptListBo);
        for (DeptVO DeptVo : deptList) {
            if (deptIdMap.containsKey(DeptVo.getDeptId())) {
                DeptVO dept = deptIdMap.get(DeptVo.getDeptId());
                DeptVo.setAllNum(dept.getAllNum());
                DeptVo.setMyAllNum(dept.getMyAllNum());
                DeptVo.setFullTimeNum(dept.getFullTimeNum());
                DeptVo.setMyFullTimeNum(dept.getMyFullTimeNum());
                DeptVo.setNuFullTimeNum(dept.getNuFullTimeNum());
                DeptVo.setMyNuFullTimeNum(dept.getNuFullTimeNum());
            } else {
                DeptVo.setAllNum(0);
                DeptVo.setMyAllNum(0);
                DeptVo.setFullTimeNum(0);
                DeptVo.setMyFullTimeNum(0);
                DeptVo.setNuFullTimeNum(0);
                DeptVo.setMyNuFullTimeNum(0);
            }
        }
        if (StrUtil.isNotEmpty(QueryDeptListBo.getName())) {
            List<HrmDept> list = lambdaQuery().select(HrmDept::getDeptId).in(HrmDept::getDeptId, deptIds).like(HrmDept::getName, QueryDeptListBo.getName()).list();
            for (HrmDept dept : list) {
                DeptVoList.addAll(createTree1(dept.getDeptId(), deptList));
            }
            return DeptVoList;
        } else {
            List<DeptVO> treeDept = createTree(0L, deptList);
            DeptVO root = new DeptVO();
            if (CollUtil.isNotEmpty(treeDept)) {
                filterNode(root, treeDept.get(0), deptIds);
            }
            DeptVoList = root.getChildren();
            return DeptVoList;
        }
    }

    /**
     * 树结构根据指定部门重组
     *
     * @param result      返回接过
     * @param node        全部树结构
     * @param findDeptIds 需要筛选的部门id
     */
    private static void filterNode(DeptVO result, DeptVO node, Set<Long> findDeptIds) {
        List<DeptVO> childList = node.getChildren();
        if (findDeptIds.contains(node.getDeptId())) {
            DeptVO newNode = BeanUtil.copyProperties(node, DeptVO.class);
            newNode.setChildren(null);
            if (CollUtil.isEmpty(result.getChildren())) {
                result.setChildren(Arrays.asList(newNode));
            } else {
                List<DeptVO> childList1 = result.getChildren();
                childList1.add(newNode);
            }
            findDeptIds.remove(node.getDeptId());
            if (CollUtil.isEmpty(childList)) {
                return;
            }
            for (DeptVO DeptVo : childList) {
                filterNode(newNode, DeptVo, findDeptIds);
            }
        } else {
            if (CollUtil.isEmpty(childList)) {
                return;
            }
            for (DeptVO DeptVo : childList) {
                filterNode(result, DeptVo, findDeptIds);
            }
        }
    }

    private List<DeptVO> createTree(Long pid, List<DeptVO> deptList) {
        List<DeptVO> treeDept = new ArrayList<>();
        for (DeptVO dept : deptList) {
            if (pid.equals(dept.getParentId())) {
                treeDept.add(dept);
                List<DeptVO> children = createTree(dept.getDeptId(), deptList);
                for (DeptVO child : children) {
                    dept.setAllNum(dept.getAllNum() + child.getAllNum());
                    dept.setFullTimeNum(dept.getFullTimeNum() + child.getFullTimeNum());
                    dept.setNuFullTimeNum(dept.getNuFullTimeNum() + child.getNuFullTimeNum());
                }
                dept.setChildren(children);
            }
        }
        return treeDept;
    }

    private List<DeptVO> createTree1(Long id, List<DeptVO> deptList) {
        List<DeptVO> treeDept = new ArrayList<>();
        for (DeptVO dept : deptList) {
            if (id.equals(dept.getDeptId())) {
                treeDept.add(dept);
                List<DeptVO> children = createTree(dept.getDeptId(), deptList);
                for (DeptVO child : children) {
                    dept.setAllNum(dept.getAllNum() + child.getAllNum());
                    dept.setFullTimeNum(dept.getFullTimeNum() + child.getFullTimeNum());
                    dept.setNuFullTimeNum(dept.getNuFullTimeNum() + child.getNuFullTimeNum());
                }
            }
        }
        return treeDept;
    }

    @Override
    public List<SimpleHrmDeptVO> querySimpleDeptList(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return new ArrayList<>();
        }
        return lambdaQuery().select(HrmDept::getDeptId, HrmDept::getName)
                .in(HrmDept::getDeptId, deptIds)
                .list()
                .stream().map(dept -> {
                    SimpleHrmDeptVO simpleHrmDeptVo = new SimpleHrmDeptVO();
                    simpleHrmDeptVo.setDeptId(dept.getDeptId());
                    simpleHrmDeptVo.setDeptName(dept.getName());
                    return simpleHrmDeptVo;
                }).collect(Collectors.toList());
    }

    @Override
    public SimpleHrmDeptVO querySimpleDept(Long deptId) {
        if (deptId == null) {
            return new SimpleHrmDeptVO();
        }
        HrmDept hrmDept = lambdaQuery().select(HrmDept::getDeptId, HrmDept::getName).eq(HrmDept::getDeptId, deptId).one();
        SimpleHrmDeptVO simpleHrmDeptVo = new SimpleHrmDeptVO();
        simpleHrmDeptVo.setDeptId(hrmDept.getDeptId());
        simpleHrmDeptVo.setDeptName(hrmDept.getName());
        return simpleHrmDeptVo;
    }

    @Override
    public BasePage<QueryEmployeeListByDeptIdVO> queryEmployeeByDeptId(QueryEmployeeByDeptIdBO employeeByDeptIdBO) {
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.DEPT_MENU_ID);
        return deptMapper.queryEmployeeByDeptId(employeeByDeptIdBO.parse(), employeeByDeptIdBO, employeeIds);
    }

    @Override
    public void deleteDeptById(String deptId)  {
        boolean exists =
                employeeService.lambdaQuery().in(HrmEmployee::getEntryStatus, 1, 3)
                        .eq(HrmEmployee::getDeptId,deptId).eq(HrmEmployee::getIsDel, 0).exists();
        if (exists) {
            throw new CrmException(HrmCodeEnum.THERE_ARE_EMPLOYEES_UNDER_THE_DEPARTMENT);
        }
        boolean childExists = lambdaQuery().eq(HrmDept::getParentId, deptId).exists();
        if (childExists) {
            throw new CrmException(HrmCodeEnum.THERE_ARE_SUB_DEPARTMENTS_THAT_CANNOT_BE_DELETED);
        }
        HrmDept dept = getById(deptId);
        String one = "1";
        if (one.equals(dept.getCode())) {
            throw new CrmException(HrmCodeEnum.TOP_LEVEL_DEPARTMENT_CANNOT_BE_DELETED);
        }
        removeById(deptId);
    }

    @Override
    public Set<Long> queryChildDeptId(Collection<Long> deptIds) {
        Set<Long> newDeptIds = new HashSet<>(deptIds);
        if (CollUtil.isNotEmpty(deptIds)) {
            deptIds.forEach(deptId -> {
                List<Long> childList = RecursionUtil.getChildList(list(), "parentId", deptId, "deptId", "deptId");
                newDeptIds.addAll(childList);
            });
        }
        return newDeptIds;
    }

    @Override
    public Set<Long> queryParentDeptId(Long pid) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(pid);
        Optional<HrmDept> hrmDeptOpt = lambdaQuery().select(HrmDept::getDeptId, HrmDept::getParentId).eq(HrmDept::getDeptId, pid).oneOpt();
        if (hrmDeptOpt.isPresent()) {
            HrmDept hrmDept = hrmDeptOpt.get();
            if (ObjectUtil.notEqual(0L, hrmDept.getParentId())) {
                Set<Long> integers = queryParentDeptId(hrmDept.getParentId());
                deptIds.addAll(integers);
            }
        }
        return deptIds;
    }

    @Override
    public List<DeptEmployeeVO> queryDeptEmployeeList() {
        return deptMapper.queryDeptEmployeeList();
    }
}
