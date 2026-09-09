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
import com.tianye.hrsystem.common.MonthlyFullSyncGuard;
import com.tianye.hrsystem.common.ResumableJobCheckpoint;
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

    @Autowired
    private MonthlyFullSyncGuard monthlyFullSyncGuard;

    @Autowired
    private ResumableJobCheckpoint resumableJobCheckpoint;

    /** 业务码：本月该范围已完成一次全量获取，禁止再次全量（前端据此弹提示引导定向补拉） */
    private static final int CODE_MONTHLY_FULL_DONE = 4001;
    /** 业务码：无可续传任务 / 任务仍在进行（用于「继续」按钮提示） */
    private static final int CODE_NOT_RESUMABLE = 4002;

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
        // 自然月「每月一次」闸门：未勾选员工(=将获取全部员工)且本月该范围已完成一次成功获取 → 直接拒绝，引导定向补拉
        boolean targetAllEmployees = queryBO == null
                || queryBO.getEmployeeIds() == null || queryBO.getEmployeeIds().isEmpty();
        if (targetAllEmployees
                && monthlyFullSyncGuard.isMonthlyFullSynced(MonthlyFullSyncGuard.BIZ_APPROVAL, companyId)) {
            Result<Object> locked = Result.ok();
            locked.setCode(CODE_MONTHLY_FULL_DONE);
            locked.setMsg("本月该范围审批数据已完成获取，如需补拉请在右侧勾选指定员工后再提交");
            return locked;
        }
        return submitApprovalFetch(queryBO, companyId, info);
    }

    @PostMapping("/continueFetch")
    @ApiOperation("从上次失败处继续获取审批数据（读断点参数原地续传）")
    public Result<Object> continueFetch() {
        com.tianye.hrsystem.model.LoginUserInfo info = com.tianye.hrsystem.config.CompanyContext.get();
        String companyId = info != null && info.getCompanyId() != null ? info.getCompanyId() : "unknown";
        // 仅当确有可续传断点(上次 FAILED 或 RUNNING)才允许；上次已成功(SUCCESS)则拒绝，避免绕过月全量闸门重复全量
        if (!resumableJobCheckpoint.hasResumable(
                ResumableJobCheckpoint.BIZ_APPROVAL, companyId)) {
            Result<Object> noResume = Result.ok();
            noResume.setCode(CODE_NOT_RESUMABLE);
            noResume.setMsg("暂无可继续的任务，请重新发起获取");
            return noResume;
        }
        String payload = resumableJobCheckpoint.getSavedPayload(
                ResumableJobCheckpoint.BIZ_APPROVAL, companyId);
        AttendanceApprovalMonthBO bo = parseApprovalPayload(payload);
        if (bo == null) {
            Result<Object> noResume = Result.ok();
            noResume.setCode(CODE_NOT_RESUMABLE);
            noResume.setMsg("暂无可继续的任务，请重新发起获取");
            return noResume;
        }
        // 「继续」读断点续传，不再做自然月全量闸门拦截：
        // 存在 FAILED 断点说明本月该范围尚未成功全量(markFullSynced 仅成功后打)，故不会与闸门冲突。
        return submitApprovalFetch(bo, companyId, info);
    }

    /** 审批获取的公共提交：按公司互斥抢占 + 后台任务自动重试，成功/失败都释放运行锁。 */
    private Result<Object> submitApprovalFetch(AttendanceApprovalMonthBO queryBO, String companyId,
                                               com.tianye.hrsystem.model.LoginUserInfo info) {
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

    /**
     * 把断点 canonical payload 反解回 {@link AttendanceApprovalMonthBO}。
     * 格式与 HrmAttendanceApprovalSyncServiceImpl#buildApprovalJobKey 一致：
     * {@code month=yyyy-MM|start=<ms|NA>|end=<ms|NA>|types=a,b|emps=1,2|ALL}
     * 无法解析/为空 → 返回 null。
     */
    private AttendanceApprovalMonthBO parseApprovalPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        AttendanceApprovalMonthBO bo = new AttendanceApprovalMonthBO();
        java.util.List<Long> emps = new java.util.ArrayList<>();
        java.util.List<String> types = new java.util.ArrayList<>();
        for (String seg : payload.split("\\|")) {
            int eq = seg.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = seg.substring(0, eq);
            String v = seg.substring(eq + 1);
            switch (k) {
                case "month":
                    bo.setMonth("NA".equals(v) ? null : v);
                    break;
                case "start":
                    bo.setFetchStartTime("NA".equals(v) || v.isEmpty() ? null : Long.valueOf(v));
                    break;
                case "end":
                    bo.setFetchEndTime("NA".equals(v) || v.isEmpty() ? null : Long.valueOf(v));
                    break;
                case "types":
                    for (String t : v.split(",")) {
                        if (!t.trim().isEmpty()) {
                            types.add(t.trim());
                        }
                    }
                    bo.setApprovalTypes(types);
                    break;
                case "emps":
                    if (!"ALL".equals(v) && !v.isEmpty()) {
                        for (String e : v.split(",")) {
                            if (!e.trim().isEmpty()) {
                                emps.add(Long.valueOf(e.trim()));
                            }
                        }
                    }
                    bo.setEmployeeIds(emps);
                    break;
                default:
                    break;
            }
        }
        return bo;
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
