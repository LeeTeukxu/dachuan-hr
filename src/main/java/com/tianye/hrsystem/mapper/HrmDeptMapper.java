package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryDeptListBO;
import com.tianye.hrsystem.entity.bo.QueryEmployeeByDeptIdBO;
import com.tianye.hrsystem.entity.po.HrmDept;
import com.tianye.hrsystem.entity.vo.DeptVO;
import com.tianye.hrsystem.entity.vo.QueryEmployeeListByDeptIdVO;
import com.tianye.hrsystem.entity.vo.DeptEmployeeVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 部门表 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface HrmDeptMapper extends BaseMapper<HrmDept> {

    @InterceptorIgnore(tenantLine = "true")
    DeptVO queryById(@Param("deptId") Long deptId, @Param("employeeIds") Collection<Long> employeeIds);

    @InterceptorIgnore(tenantLine = "true")
    List<DeptVO> queryList(@Param("queryDeptListBO") QueryDeptListBO queryDeptListBO);

    BasePage<QueryEmployeeListByDeptIdVO> queryEmployeeByDeptId(BasePage<QueryEmployeeListByDeptIdVO> parse, @Param("data") QueryEmployeeByDeptIdBO employeeByDeptIdBO, @Param("employeeIds") Collection<Long> employeeIds);
    @InterceptorIgnore(tenantLine = "true")
    List<DeptEmployeeVO> queryDeptEmployeeList();

    @InterceptorIgnore(tenantLine = "true")
    List<DeptVO> queryDeptByEmpIds(@Param("employeeIds") Collection<Long> employeeIds);
    DeptVO queryNoEmployeeDept(Long deptId);

    @Select(value="Select t.userName from (select count(0) as Num,userName from tbattendanceuser group by  userName)t where t.Num>1")
    List<String> getRepeatUser();


    @Select(value=" Select distinct userId from tbattendanceuser where userid not in(Select distinct user_id from hrm_attendance_plan where work_date>='${Begin}' And work_date<='${End}')")
    List<String> getEmptyPlanUser(String  Begin, String  End);
}
