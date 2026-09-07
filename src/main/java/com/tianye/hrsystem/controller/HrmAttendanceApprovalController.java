package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO;
import com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalDurationBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalStatisticsStatusBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalSubtypeBO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.IHrmAttendanceApprovalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hrmAttendanceApproval")
@Api(tags = "考勤管理-审批数据")
public class HrmAttendanceApprovalController {

    @Autowired
    private IHrmAttendanceApprovalService attendanceApprovalService;

    @Autowired
    private com.tianye.hrsystem.task.AttendanceSyncTaskLauncher syncTaskLauncher;

    @PostMapping("/queryPageList")
    @ApiOperation("分页查询审批数据")
    public Result<BasePage<QueryAttendanceApprovalPageVO>> queryPageList(@RequestBody QueryAttendanceApprovalPageBO queryBO) {
        return Result.ok(attendanceApprovalService.queryPageList(queryBO));
    }

    @PostMapping("/checkMonthData")
    @ApiOperation("检查指定月份审批数据是否已获取")
    public Result<Object> checkMonthData(@RequestBody AttendanceApprovalMonthBO queryBO) {
        return Result.ok(attendanceApprovalService.checkMonthData(queryBO));
    }

    @PostMapping("/fetchMonthData")
    @ApiOperation("手工获取指定月份审批数据")
    public Result<Object> fetchMonthData(@RequestBody AttendanceApprovalMonthBO queryBO) {
        com.tianye.hrsystem.model.LoginUserInfo info = com.tianye.hrsystem.config.CompanyContext.get();
        String companyId = info != null && info.getCompanyId() != null ? info.getCompanyId() : "unknown";
        // 按公司互斥：提交路径同步抢占，重复触发当场返回“进行中”
        if (!attendanceApprovalService.tryBeginFetch(companyId)) {
            // 任务正在进行中，返回特殊状态码202（Accepted），而不是抛异常
            // 前端会根据这个状态码自动切换到查看进度模式
            java.util.Map<String, Object> runningData = new java.util.HashMap<>();
            runningData.put("alreadyRunning", true);
            runningData.put("queued", true);
            Result<Object> runningResult = Result.ok(runningData);
            runningResult.setCode(202);
            runningResult.setMsg("审批数据获取正在进行中，已自动切换到查看进度模式");
            return runningResult;
        }
        try {
            attendanceApprovalService.beginFetchProgress();
            boolean submitted = syncTaskLauncher.submit(companyId, info, () -> {
                try {
                    attendanceApprovalService.fetchMonthDataWithAutoRetry(queryBO);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(HrmAttendanceApprovalController.class)
                            .error("公司{}审批数据获取后台任务失败: {}", companyId, e.getMessage(), e);
                } finally {
                    attendanceApprovalService.finishFetch(companyId);
                }
            });
            if (!submitted) {
                throw new IllegalStateException("当前同步任务较多，请稍后再试");
            }
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("queued", true);
            return Result.ok(data);
        } catch (RuntimeException ex) {
            attendanceApprovalService.finishFetch(companyId);
            throw ex;
        }
    }

    @PostMapping("/queryFetchProgress")
    @ApiOperation("查询审批数据获取进度")
    public Result<Object> queryFetchProgress() {
        return Result.ok(attendanceApprovalService.queryFetchProgress());
    }

    @PostMapping("/querySubtypeOptions")
    @ApiOperation("查询审批子类型下拉选项")
    public Result<Object> querySubtypeOptions() {
        return Result.ok(attendanceApprovalService.querySubtypeOptions());
    }

    @PostMapping("/updateSubtype")
    @ApiOperation("修改审批子类型")
    public Result<Object> updateSubtype(@RequestBody UpdateAttendanceApprovalSubtypeBO updateBO) {
        return Result.ok(attendanceApprovalService.updateSubtype(updateBO));
    }

    @PostMapping("/updateDuration")
    @ApiOperation("修改审批时长")
    public Result<Object> updateDuration(@RequestBody UpdateAttendanceApprovalDurationBO updateBO) {
        return Result.ok(attendanceApprovalService.updateDuration(updateBO));
    }

    @PostMapping("/updateStatisticsStatus")
    @ApiOperation("修改审批统计状态")
    public Result<Object> updateStatisticsStatus(@RequestBody UpdateAttendanceApprovalStatisticsStatusBO updateBO) {
        return Result.ok(attendanceApprovalService.updateStatisticsStatus(updateBO));
    }

    @PostMapping("/addManual")
    @ApiOperation("手工添加审批数据")
    public Result<Object> addManualApproval(@RequestBody AddAttendanceApprovalBO addBO) {
        return Result.ok(attendanceApprovalService.addManualApproval(addBO));
    }

    @PostMapping("/delete")
    @ApiOperation("删除审批数据")
    public Result<Object> deleteApproval(@RequestBody DeleteAttendanceApprovalBO deleteBO) {
        return Result.ok(attendanceApprovalService.deleteApproval(deleteBO));
    }
}
