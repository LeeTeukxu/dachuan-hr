package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmAttendanceGroup;
import com.tianye.hrsystem.model.HrmAttendancePlan;

import java.util.HashMap;
import java.util.List;

/**
 * 员工排班记录
 */
public interface HrmAttendancePlanMapper extends BaseMapper<HrmAttendancePlan>
{
    /**
     * 查询员工的排班记录
     * @param params
     * @return
     */
    List<HrmAttendancePlan> getAttendanceShiftPlan(HashMap<String,Object> params);

    /**
     * 获取行政考勤1（大小周）的应工作天数
     */
    Integer getAdministrationAttendanceDays(HashMap<String,Object> params);
}
