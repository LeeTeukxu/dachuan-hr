package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmDeptRepository  extends JpaRepository<HrmDept,Long>  {
    Integer countByCode(String Code);
    Integer countByCodeAndDeptIdNot(String Code,Long DepId);
    Integer countByParentId(Long ID);
    List<HrmDept> findAllByDeptIdIn(List<Long> IDS);

    List<HrmDept> findAllByName(String Name);
    List<HrmDept> findAllByParentIdAndName(Long parentId,String Name);

    List<HrmDept> findAllByNameOrderByDeptId(String Name);

    HrmDept getAllByDeptId(Long DeptId);

    /**
     * 按部门名称批量查编制（plan_num），用于部门统计 /mp/dashboard/deptOverview
     * 注入 plan_num 字段以恢复"缺编/超编/已满"角标（4 部门统计编制真实化，2026-09-10）。
     */
    @org.springframework.data.jpa.repository.Query("SELECT d.name AS name, d.planNum AS planNum FROM HrmDept d WHERE d.name IN :names")
    List<java.util.Map<String, Object>> findPlanNumByNames(@org.springframework.data.repository.query.Param("names") java.util.Collection<String> names);
}
