package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddDeptBO;
import com.tianye.hrsystem.entity.bo.QueryDeptListBO;
import com.tianye.hrsystem.entity.bo.QueryEmployeeByDeptIdBO;
import com.tianye.hrsystem.entity.vo.DeptVO;
import com.tianye.hrsystem.entity.vo.QueryEmployeeListByDeptIdVO;
import com.tianye.hrsystem.entity.vo.SimpleHrmDeptVO;

import com.tianye.hrsystem.entity.vo.DeptEmployeeVO;
import com.tianye.hrsystem.entity.po.HrmDept;


import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IHrmDeptService extends BaseService<HrmDept> {

    /**
     * 添加活修改部门
     *
     * @param addDeptBO
     */
    void addOrUpdate(AddDeptBO addDeptBO);

    /**
     * 查询部门详情
     *
     * @param deptId
     * @return
     */
    DeptVO queryById(Long deptId);

    /**
     * 查询部门列表
     *
     * @param queryDeptListBO
     * @return
     */
    List<DeptVO> queryTreeList(QueryDeptListBO queryDeptListBO);

    /**
     * 查询部门简要字段列表
     *
     * @param deptIds
     * @return
     */
    List<SimpleHrmDeptVO> querySimpleDeptList(Collection<Long> deptIds);

    /**
     * 查询部门简要字段
     *
     * @param deptId
     * @return
     */
    SimpleHrmDeptVO querySimpleDept(Long deptId);

    /**
     * 通过部门id查询员工列表
     *
     * @param employeeByDeptIdBO
     * @return
     */
    BasePage<QueryEmployeeListByDeptIdVO> queryEmployeeByDeptId(QueryEmployeeByDeptIdBO employeeByDeptIdBO);

    /**
     * 删除部门
     *
     * @param deptId
     */
    void deleteDeptById(String deptId);

    /**
     * 查询部门下的子部门id
     *
     * @param deptIds
     * @return
     */
    Set<Long> queryChildDeptId(Collection<Long> deptIds);

    /**
     * 查询部门下的父部门id
     *
     * @param deptId
     * @return
     */
    Set<Long> queryParentDeptId(Long deptId);

    /**
     * 查询所有部门(员工部门表单使用)
     *
     * @return
     */
    List<DeptEmployeeVO> queryDeptEmployeeList();
}
