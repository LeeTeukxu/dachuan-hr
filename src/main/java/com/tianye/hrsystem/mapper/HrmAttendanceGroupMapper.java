package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmAttendanceGroup;
import com.tianye.hrsystem.model.tbattendanceuser;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 考勤组表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-13
 */
public interface HrmAttendanceGroupMapper extends BaseMapper<HrmAttendanceGroup>
{
    /**
     * 从钉钉数据查询 员工对应的考勤组
     * @param employeeId
     * @return
     */
    HrmAttendanceGroup getEmployeeAttendanceGroupDingding(Long employeeId);

    /**
     * 批量查询在钉钉考勤组中的员工ID
     */
    List<Long> queryEmployeeIdsInAttendanceGroupDingding(@Param("employeeIds") Collection<Long> employeeIds);
}
