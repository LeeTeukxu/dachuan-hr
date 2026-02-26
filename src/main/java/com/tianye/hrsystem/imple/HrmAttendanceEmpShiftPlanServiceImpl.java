package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.HrmActionRecord;
import com.tianye.hrsystem.mapper.HrmActionRecordMapper;
import com.tianye.hrsystem.mapper.HrmAttendancePlanMapper;
import com.tianye.hrsystem.model.HrmAttendancePlan;
import com.tianye.hrsystem.service.IHrmActionRecordService;
import com.tianye.hrsystem.service.IHrmAttendanceEmpShiftPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * 排班计划
 */
@Service
public class HrmAttendanceEmpShiftPlanServiceImpl extends BaseServiceImpl<HrmAttendancePlanMapper, HrmAttendancePlan> implements IHrmAttendanceEmpShiftPlanService
{

    @Autowired
    private HrmAttendancePlanMapper attendancePlanMapper;
    @Override
    public List<HrmAttendancePlan> getAttendanceShiftPlan(HashMap<String, Object> params)
    {
        return attendancePlanMapper.getAttendanceShiftPlan(params);
    }
}
