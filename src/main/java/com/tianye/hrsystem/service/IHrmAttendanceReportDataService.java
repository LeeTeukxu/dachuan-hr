package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.vo.EmployeeAttendanceDaysStaticVo;
import com.tianye.hrsystem.entity.vo.EmployeeLeaveStaticVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryDayVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryVo;
import com.tianye.hrsystem.model.HrmAttendanceReportData;

import java.util.HashMap;
import java.util.List;

/**
 * 钉钉 考勤数据统计服务
 */
public interface IHrmAttendanceReportDataService extends BaseService<HrmAttendanceReportData>
{
    /**
     * 获取员工的月考勤 情况数据
     * @param params
     * @return
     */
    List<HrmAttendanceSummaryVo> getEmpAttendanceSummaryList(HashMap<String,Object> params);

    /**
     * 查询员工的请假统计数据
     */
    List<EmployeeLeaveStaticVo> getEmpLeaveStaticList(HashMap<String,Object> params);

//    /**
//     * 统计员工应出勤数据与实际出勤数据
//     * @param params
//     * @return
//     */
//    List<EmployeeAttendanceDaysStaticVo> getEmpAttendanceDaysStaticList(HashMap<String,Object> params);


    /**
     * 获取员工的每日考勤 情况数据
     * @param params
     * @return
     */
    List<HrmAttendanceSummaryDayVo> getEmpAttendanceSummaryDayList(HashMap<String,Object> params);


}
