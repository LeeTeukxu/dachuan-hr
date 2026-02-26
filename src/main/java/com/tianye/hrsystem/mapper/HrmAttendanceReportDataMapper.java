package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.vo.EmployeeAttendanceDaysStaticVo;
import com.tianye.hrsystem.entity.vo.EmployeeLeaveStaticVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryDayVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryVo;
import com.tianye.hrsystem.model.HrmAttendancePlan;
import com.tianye.hrsystem.model.HrmAttendanceReportData;

import java.util.HashMap;
import java.util.List;

/**
 * 钉钉考勤 统计
 */
public interface HrmAttendanceReportDataMapper extends BaseMapper<HrmAttendanceReportData>
{

    /**
     * 获取某月员工的考勤情况 统计数据
     * @param params
     * @return
     */
    List<HrmAttendanceSummaryVo> getEmpAttendanceSummaryList(HashMap<String,Object> params);

    /**
     * 查询员工的请假统计数据
     */
    List<EmployeeLeaveStaticVo> getEmpLeaveStaticList(HashMap<String,Object> params);

    /**
     * 统计员工应出勤数据与实际出勤数据
     * @param params
     * @return
     */
    List<EmployeeAttendanceDaysStaticVo> getEmpAttendanceDaysStaticList(HashMap<String,Object> params);


    /**
     * 获取某月 员工每天的考勤统计数据
     */
    List<HrmAttendanceSummaryDayVo> getEmpAttendanceDaySummaryList(HashMap<String,Object> params);


}
