package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.vo.EmployeeAttendanceDaysStaticVo;
import com.tianye.hrsystem.entity.vo.EmployeeLeaveStaticVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryDayVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryVo;
import com.tianye.hrsystem.mapper.HrmAttendanceReportDataMapper;
import com.tianye.hrsystem.model.HrmAttendanceReportData;
import com.tianye.hrsystem.service.IHrmAttendanceReportDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * 钉钉考勤统计 服务类
 */
@Service
public class HrmAttendanceReportDataImpl extends BaseServiceImpl<HrmAttendanceReportDataMapper, HrmAttendanceReportData>
        implements IHrmAttendanceReportDataService
{

    @Autowired
    private HrmAttendanceReportDataMapper reportDataMapper;

    /**
     * 获取员工月考勤情况  统计数据
     * @param params
     * @return
     */
    @Override
    public List<HrmAttendanceSummaryVo> getEmpAttendanceSummaryList(HashMap<String,Object> params)
    {
        return  reportDataMapper.getEmpAttendanceSummaryList(params);
    }

    /**
     * 查询员工的请假统计数据
     */
    @Override
    public List<EmployeeLeaveStaticVo> getEmpLeaveStaticList(HashMap<String, Object> params)
    {
        return reportDataMapper.getEmpLeaveStaticList(params);
    }

    /**
     * 每日考勤统计数据
     * @param params
     * @return
     */
    @Override
    public List<HrmAttendanceSummaryDayVo> getEmpAttendanceSummaryDayList(HashMap<String, Object> params)
    {
        return reportDataMapper.getEmpAttendanceDaySummaryList(params);
    }


}
