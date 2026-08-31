package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.bo.QueryEmployeeOvertimeNightDetailBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightDailyDetailPageBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightStatisticsPageBO;
import com.tianye.hrsystem.entity.bo.UpdateOvertimeNightAttendanceBO;
import com.tianye.hrsystem.entity.vo.EmployeeOvertimeNightMonthlyDetailVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightDailyDetailPageVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;
import com.tianye.hrsystem.service.IHrmOvertimeNightStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/hrmOvertimeNightStatistics")
@Api(tags = "考勤管理-加班夜班统计")
public class HrmOvertimeNightStatisticsController {

    @Autowired
    private IHrmOvertimeNightStatisticsService overtimeNightStatisticsService;

    @PostMapping("/queryPageList")
    @ApiOperation("查询加班夜班统计列表")
    public Result<BasePage<QueryOvertimeNightStatisticsPageVO>> queryPageList(@RequestBody QueryOvertimeNightStatisticsPageBO queryBO) {
        return Result.OK(overtimeNightStatisticsService.queryPageList(queryBO));
    }

    @PostMapping("/startStatistics")
    @ApiOperation("开始统计指定月份的加班夜班数据")
    public Result<BasePage<QueryOvertimeNightStatisticsPageVO>> startStatistics(@RequestBody QueryOvertimeNightStatisticsPageBO queryBO) {
        return Result.OK(overtimeNightStatisticsService.startStatistics(queryBO));
    }

    @PostMapping("/startStatisticsForEmployee")
    @ApiOperation("单人统计：对指定员工重新统计指定月份的加班夜班数据")
    public Result<List<EmployeeOvertimeNightMonthlyDetailVO>> startStatisticsForEmployee(@RequestBody QueryOvertimeNightStatisticsPageBO queryBO) {
        return Result.OK(overtimeNightStatisticsService.startStatisticsForEmployee(queryBO));
    }

    @PostMapping("/queryEmployeeMonthlyDetail")
    @ApiOperation("查询员工月度加班夜班明细")
    public Result<List<EmployeeOvertimeNightMonthlyDetailVO>> queryEmployeeMonthlyDetail(@RequestBody QueryEmployeeOvertimeNightDetailBO queryBO) {
        return Result.OK(overtimeNightStatisticsService.queryEmployeeMonthlyDetail(queryBO));
    }

    @PostMapping("/queryDailyDetailPageList")
    @ApiOperation("查询指定月份的每日加班夜班明细列表")
    public Result<BasePage<QueryOvertimeNightDailyDetailPageVO>> queryDailyDetailPageList(@RequestBody QueryOvertimeNightDailyDetailPageBO queryBO) {
        return Result.OK(overtimeNightStatisticsService.queryDailyDetailPageList(queryBO));
    }

    @PostMapping("/updateAttendanceSummary")
    @ApiOperation("保存员工月度出勤时间")
    public Result updateAttendanceSummary(@RequestBody UpdateOvertimeNightAttendanceBO updateBO) {
        try {
            overtimeNightStatisticsService.updateAttendanceSummary(updateBO);
            return Result.OK();
        } catch (Exception ex) {
            ex.printStackTrace();
            return Result.Error(ex);
        }
    }

    @PostMapping("/exportStatistics")
    @ApiOperation("导出指定月份的加班夜班统计（当前前端未启用）")
    public void exportStatistics(@RequestBody QueryOvertimeNightStatisticsPageBO queryBO, HttpServletResponse response) throws IOException {
        // 导出能力先保留，当前页面已切换为“开始统计后落库，再从数据库查询”。
        overtimeNightStatisticsService.exportStatistics(queryBO, response);
    }

    @GetMapping("/downloadStatisticsTemplate")
    @ApiOperation("导出统计数据（临时固定文件）")
    public void downloadStatisticsTemplate(HttpServletResponse response) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/jbtj.xlsx")) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "jbtj.xlsx not found");
                return;
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("jbtj.xlsx", "UTF-8"));
            response.setHeader("Set-Cookie", "fileDownload=true; path=/");
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        }
    }
}
