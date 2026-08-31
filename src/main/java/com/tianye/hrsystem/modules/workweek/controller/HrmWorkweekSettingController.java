package com.tianye.hrsystem.modules.workweek.controller;

import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.workweek.bo.InitWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.workweek.bo.SaveWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.workweek.bo.UpdateWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.service.HrmWorkweekSettingService;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekMonthCalendarVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekYearSettingVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hrmWorkweekSetting")
@Api(tags = "考勤管理-单双休设置")
public class HrmWorkweekSettingController {

    @Autowired
    private HrmWorkweekSettingService workweekSettingService;

    @PostMapping("/queryYearSettings")
    @ApiOperation("查询指定年份单双休设置")
    public Result<WorkweekYearSettingVO> queryYearSettings(@RequestBody QueryWorkweekSettingBO queryBO) {
        try {
            return Result.OK(workweekSettingService.queryYearSettings(queryBO));
        } catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/queryMonthCalendar")
    @ApiOperation("查询指定月份每日上班休息日历")
    public Result<WorkweekMonthCalendarVO> queryMonthCalendar(@RequestBody QueryWorkweekMonthCalendarBO queryBO) {
        try {
            return Result.OK(workweekSettingService.queryMonthCalendar(queryBO));
        } catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/initYearSettings")
    @ApiOperation("初始化指定年份单双休设置")
    public Result<WorkweekYearSettingVO> initYearSettings(@RequestBody InitWorkweekSettingBO initBO) {
        try {
            return Result.OK(workweekSettingService.initYearSettings(initBO));
        } catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/updateWeekType")
    @ApiOperation("修改某周单双休并向后重算")
    public Result<WorkweekYearSettingVO> updateWeekType(@RequestBody UpdateWorkweekSettingBO updateBO) {
        try {
            return Result.OK(workweekSettingService.updateWeekType(updateBO));
        } catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/saveMonthCalendar")
    @ApiOperation("保存指定月份每日上班休息日历")
    public Result<WorkweekMonthCalendarVO> saveMonthCalendar(@RequestBody SaveWorkweekMonthCalendarBO saveBO) {
        try {
            return Result.OK(workweekSettingService.saveMonthCalendar(saveBO));
        } catch (Exception ax) {
            return Result.Error(ax);
        }
    }
}
