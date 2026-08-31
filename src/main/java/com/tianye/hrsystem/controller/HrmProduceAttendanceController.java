package com.tianye.hrsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.bo.QueryMonthAttendanceBO;
import com.tianye.hrsystem.entity.bo.SyncProduceAttendanceBO;
import com.tianye.hrsystem.entity.bo.UpdateProduceAttendanceCellBO;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import com.tianye.hrsystem.service.IHrmProduceAttendanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/hrmProduceAttendance")
@Api(tags = "考勤管理-生产部考勤")
public class HrmProduceAttendanceController {

    @Autowired
    IHrmProduceAttendanceService iHrmProduceAttendanceService;

    /**
     * 导入生产部考勤报表数据
     * @param
     * @return
     */
    @PostMapping("/importProduceAttendance")
    @ApiOperation(value = "导入生产部考勤报表数据")
    public Result importProduceAttendance(@ApiParam("生产部考勤") @RequestParam(name = "produceAttendanceFile", required = false) MultipartFile produceAttendanceFile,
                                          @RequestParam(name = "dates") String dates) {
        try {
            iHrmProduceAttendanceService.resolveProduceAttendanceData(produceAttendanceFile, dates);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    @PostMapping("/queryMonthAttendanceList")
    @ApiOperation("查询每月考勤统计列表")
    public Result<Page<QueryMonthAttendanceVO>> queryProduceAttendanceList(@RequestBody QueryMonthAttendanceBO queryMonthAttendanceBO) {
        try {
            Page<QueryMonthAttendanceVO> page = iHrmProduceAttendanceService.queryProduceAttendanceList(queryMonthAttendanceBO);
            return com.tianye.hrsystem.common.Result.OK(page);
        }catch (Exception ax) {
            ax.printStackTrace();
            return com.tianye.hrsystem.common.Result.Error(ax);
        }
    }

    @PostMapping("/downloadAdministrativeAttendance")
    @ApiOperation("下载行政体系考勤")
    public void downloadAdministrativeAttendance(@RequestBody QueryMonthAttendanceBO queryMonthAttendanceBO,
                                                 HttpServletResponse response) throws Exception {
        iHrmProduceAttendanceService.downloadAdministrativeAttendance(queryMonthAttendanceBO, response);
    }

    @PostMapping("/syncFromOvertimeNightStatistics")
    @ApiOperation("从加班/夜班统计同步上传考勤")
    public Result<Integer> syncFromOvertimeNightStatistics(@RequestBody SyncProduceAttendanceBO syncProduceAttendanceBO) {
        try {
            return Result.OK(iHrmProduceAttendanceService.syncFromOvertimeNightStatistics(syncProduceAttendanceBO));
        } catch (Exception ax) {
            ax.printStackTrace();
            return Result.Error(ax);
        }
    }

    @PostMapping("/updateCell")
    @ApiOperation("保存上传考勤单元格")
    public Result updateCell(@RequestBody UpdateProduceAttendanceCellBO updateProduceAttendanceCellBO) {
        try {
            iHrmProduceAttendanceService.updateProduceAttendanceCell(updateProduceAttendanceCellBO);
            return Result.OK();
        } catch (Exception ax) {
            ax.printStackTrace();
            return Result.Error(ax);
        }
    }
}
