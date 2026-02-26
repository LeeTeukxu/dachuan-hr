package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmAttendanceDateShift;
import com.tianye.hrsystem.model.HrmAttendancePlan;

import java.util.HashMap;
import java.util.List;

/**
 * 员工每日排班计划 服务类
 */
public interface IHrmAttendanceEmpShiftPlanService extends BaseService<HrmAttendancePlan>
{
    /**
     * 查询员工的排班记录
     * @param params
     * @return
     */
    List<HrmAttendancePlan> getAttendanceShiftPlan(HashMap<String,Object> params);
}
