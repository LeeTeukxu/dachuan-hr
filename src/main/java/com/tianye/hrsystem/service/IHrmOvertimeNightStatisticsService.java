package com.tianye.hrsystem.service;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryEmployeeOvertimeNightDetailBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightDailyDetailPageBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightStatisticsPageBO;
import com.tianye.hrsystem.entity.bo.UpdateOvertimeNightAttendanceBO;
import com.tianye.hrsystem.entity.vo.EmployeeOvertimeNightMonthlyDetailVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightDailyDetailPageVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface IHrmOvertimeNightStatisticsService {

    BasePage<QueryOvertimeNightStatisticsPageVO> queryPageList(QueryOvertimeNightStatisticsPageBO queryBO);

    BasePage<QueryOvertimeNightStatisticsPageVO> startStatistics(QueryOvertimeNightStatisticsPageBO queryBO);

    List<EmployeeOvertimeNightMonthlyDetailVO> startStatisticsForEmployee(QueryOvertimeNightStatisticsPageBO queryBO);

    List<EmployeeOvertimeNightMonthlyDetailVO> queryEmployeeMonthlyDetail(QueryEmployeeOvertimeNightDetailBO queryBO);

    BasePage<QueryOvertimeNightDailyDetailPageVO> queryDailyDetailPageList(QueryOvertimeNightDailyDetailPageBO queryBO);

    void updateAttendanceSummary(UpdateOvertimeNightAttendanceBO updateBO);

    void exportStatistics(QueryOvertimeNightStatisticsPageBO queryBO, HttpServletResponse response) throws IOException;
}
