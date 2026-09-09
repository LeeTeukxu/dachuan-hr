package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.MonthlyFullSyncGuard;
import com.tianye.hrsystem.common.ProgressTracker;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO;
import com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalDurationBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalStatisticsStatusBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalSubtypeBO;
import com.tianye.hrsystem.entity.vo.AttendanceApprovalMonthPortionVO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO;
import com.tianye.hrsystem.mapper.HrmAttendanceApprovalMapper;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmAttendanceApprovalService;
import com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HrmAttendanceApprovalServiceImpl implements IHrmAttendanceApprovalService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HrmAttendanceApprovalServiceImpl.class);

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int FETCH_VERSION = 1;
    private static final String STATISTICS_STATUS_CANCELLED = "取消至统计";
    private static final String STATISTICS_STATUS_INCLUDED_LABEL = "添加至统计";
    private static final long HOUR_MILLIS = 60L * 60L * 1000L;
    private static final String FETCH_STATUS_IDLE = "IDLE";
    private static final String FETCH_STATUS_RUNNING = "RUNNING";
    private static final String FETCH_STATUS_SUCCESS = "SUCCESS";
    private static final String FETCH_STATUS_FAILED = "FAILED";
    // ProgressTracker 前缀
    private static final String FETCH_KEY_PREFIX = "attendance:fetch";
    // 按公司运行标记：同一公司同时只允许一个"获取审批数据"任务；超过 2 小时视为异常残留自动放行
    private static final Map<String, java.util.concurrent.atomic.AtomicReference<Long>> FETCH_RUNNING_MAP = new ConcurrentHashMap<>();
    private static final long STALE_FETCH_RUNNING_MILLIS = 2L * HOUR_MILLIS;
    // 审批获取失败自动重试：次数与退避基数可配（默认 2 次自动重试、60s 起步指数退避、封顶 5 分钟）
    @Value("${hrm.approval-fetch.auto-retry.attempts:2}")
    private int fetchAutoRetryAttempts = 2;
    @Value("${hrm.approval-fetch.auto-retry.backoff-ms:60000}")
    private long fetchAutoRetryBackoffMs = 60000L;
    private static final List<String> DURATION_UNIT_OPTIONS = Arrays.asList("小时", "分钟", "天");
    private static final List<String> DEFAULT_SUBTYPE_OPTIONS = Arrays.asList(
            "事假", "调休", "病假", "婚假", "丧假", "产假", "陪产假", "年假", "补休"
    );

    @Autowired
    private HrmAttendanceApprovalMapper attendanceApprovalMapper;

    @Autowired
    private tbattendanceapproveRepository approvalRepository;

    @Autowired
    private tbattendanceuserRepository attendanceUserRepository;

    @Autowired
    private IHrmAttendanceApprovalSyncService approvalSyncService;

    @Autowired
    private hrmAttendanceApprovalFetchMarkRepository fetchMarkRepository;

    @Autowired
    private com.tianye.hrsystem.modules.workweek.service.HrmWorkweekSettingService workweekSettingService;

    @Autowired
    private hrmEmployeeRepository hrmEmployeeRepository;

    @Autowired
    private tbPlanListRepository tbPlanListRepository;

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private Redis redis;

    @Autowired
    private MonthlyFullSyncGuard monthlyFullSyncGuard;

    @Autowired
    private com.tianye.hrsystem.common.ResumableJobCheckpoint resumableJobCheckpoint;

    @Autowired
    private AdminMessageServiceImpl adminMessageService;

    @Override
    public BasePage<QueryAttendanceApprovalPageVO> queryPageList(QueryAttendanceApprovalPageBO queryBO) {
        BasePage<QueryAttendanceApprovalPageVO> page = attendanceApprovalMapper.queryPageList(queryBO.parse(), queryBO);
        clampCrossMonthRowsForDisplay(page, queryBO);
        return page;
    }

    /**
     * 跨月单按月拆分显示：审批区间与所选月部分相交时，把展示的 beginTime/endTime 截断为该月内区间，
     * 时长按工作日占比折算（工作日判定复用单双休/节假日日历）。仅改展示值，库内记录与统计不受影响。
     */
    private void clampCrossMonthRowsForDisplay(BasePage<QueryAttendanceApprovalPageVO> page,
                                               QueryAttendanceApprovalPageBO queryBO) {
        if (page == null || page.getList() == null || page.getList().isEmpty()
                || queryBO.getTimes() == null || queryBO.getTimes().size() < 2
                || queryBO.getTimes().get(0) == null || queryBO.getTimes().get(1) == null) {
            return;
        }
        java.time.LocalDate monthStart = queryBO.getTimes().get(0);
        java.time.LocalDate monthEnd = queryBO.getTimes().get(1);
        Date monthStartDay = Date.from(monthStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Date monthEndDay = Date.from(monthEnd.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant());
        java.time.YearMonth month = java.time.YearMonth.from(monthStart);
        for (QueryAttendanceApprovalPageVO vo : page.getList()) {
            AttendanceApprovalMonthPortionVO portion = calculateMonthPortion(vo.getEmployeeId(),
                    vo.getBeginTime(), vo.getEndTime(), vo.getDuration(), vo.getDurationDay(), month);
            if (portion == null) {
                continue;
            }
            vo.setBeginTime(portion.getDisplayBeginTime());
            vo.setEndTime(portion.getDisplayEndTime());
            vo.setDuration(portion.getDuration());
            vo.setDurationDay(portion.getDurationDay());
        }
    }

    @Override
    public AttendanceApprovalMonthPortionVO calculateMonthPortion(Long employeeId,
                                                                  Date beginTime,
                                                                  Date endTime,
                                                                  String duration,
                                                                  String durationDay,
                                                                  java.time.YearMonth month) {
        if (month == null || beginTime == null || endTime == null || !beginTime.before(endTime)) {
            return null;
        }
        Date monthStartDay = Date.from(month.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Date monthEndDay = Date.from(month.atEndOfMonth().atTime(23, 59, 59)
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        if (endTime.before(monthStartDay) || beginTime.after(monthEndDay)) {
            return null;
        }
        HrmEmployee employee = resolveEmployee(employeeId);
        long totalDays = workdaySpanDays(employee, beginTime, endTime);
        if (totalDays <= 0) {
            return null;
        }
        Date clampedBegin = beginTime.before(monthStartDay) ? monthStartDay : beginTime;
        Date clampedEnd = endTime.after(monthEndDay) ? monthEndDay : endTime;
        long monthDays = workdaySpanDays(employee, clampedBegin, clampedEnd);
        AttendanceApprovalMonthPortionVO portion = new AttendanceApprovalMonthPortionVO();
        portion.setDisplayBeginTime(clampedBegin);
        portion.setDisplayEndTime(clampedEnd);
        portion.setTotalWorkDays(totalDays);
        portion.setMonthWorkDays(monthDays);
        applyProratedDuration(portion, duration, durationDay, monthDays, totalDays);
        return portion;
    }

    private HrmEmployee resolveEmployee(Long employeeId) {
        return employeeId == null ? null : hrmEmployeeRepository.findById(employeeId).orElse(null);
    }

    /** 员工区间内工作日数：行政单双休（固定工时）直接走日历；其他员工先剔除排班休息，再回落日历；日历不可用时退化为自然天数。 */
    private long workdaySpanDays(HrmEmployee employee, Date begin, Date end) {
        java.time.LocalDate beginDate = toLocalDateSafe(begin);
        java.time.LocalDate endDate = toLocalDateSafe(end);
        if (beginDate == null || endDate == null || endDate.isBefore(beginDate)) {
            return 0;
        }
        int workDays = workweekSettingService.countWorkDays(beginDate, endDate,
                queryEmployeeScheduledDayStatus(employee, beginDate, endDate));
        if (workDays > 0) {
            return workDays;
        }
        return daysInclusive(begin, end);
    }

    /**
     * 员工排班覆盖（TRUE=排班上班、FALSE=排班休息；未覆盖日期由日历判定）。
     * 判定顺序：先看员工属性——行政体系(1)且休息制度为行政单双休(1)的固定工时员工不查排班，
     * 休息完全由单双休设置/节假日决定；其余员工（固定月休4天、生产体系等）查 tbplanlist 排班，
     * 排班标"休"且 UserID 含该员工钉钉ID的日期剔除，排了班次的日期计为工作日。
     */
    private Map<java.time.LocalDate, Boolean> queryEmployeeScheduledDayStatus(HrmEmployee employee,
                                                                              java.time.LocalDate beginDate,
                                                                              java.time.LocalDate endDate) {
        Map<java.time.LocalDate, Boolean> status = new java.util.HashMap<>();
        if (employee == null
                || (Integer.valueOf(1).equals(employee.getAffiliationSystem())
                    && Integer.valueOf(1).equals(employee.getRestType()))) {
            return status;
        }
        String dingUserId = employee.getDingtalkUserId();
        if (dingUserId == null || dingUserId.trim().isEmpty()) {
            return status;
        }
        Date begin = Date.from(beginDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Date endExclusive = Date.from(endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Set<java.time.LocalDate> scheduledRest = new java.util.HashSet<>();
        Set<java.time.LocalDate> scheduledWork = new java.util.HashSet<>();
        List<tbplanlist> planRows = tbPlanListRepository.findAllByWorkDateBetweenOrderByIdDesc(begin, endExclusive);
        for (tbplanlist row : planRows) {
            if (row == null || row.getWorkDate() == null || row.getUserId() == null) {
                continue;
            }
            boolean belongsToEmployee = false;
            for (String userId : row.getUserId().split(",")) {
                if (dingUserId.equals(userId.trim())) {
                    belongsToEmployee = true;
                    break;
                }
            }
            if (!belongsToEmployee) {
                continue;
            }
            java.time.LocalDate day = toLocalDateSafe(row.getWorkDate());
            if ("rest".equals(row.getShiftType())) {
                scheduledRest.add(day);
            } else {
                scheduledWork.add(day);
            }
        }
        for (java.time.LocalDate day : scheduledRest) {
            status.put(day, Boolean.FALSE);
        }
        for (java.time.LocalDate day : scheduledWork) {
            status.putIfAbsent(day, Boolean.TRUE);
        }
        return status;
    }

    /**
     * 按天填的请假（总时长为 8 小时整数倍）：月部分 = 该月工作日数 × 8 小时，封顶不超过原单总时长，
     * 保证跨月显示的是"整天"；按小时填的请假退化为工作日占比折算（0.5 小时粒度）。
     */
    private void applyProratedDuration(AttendanceApprovalMonthPortionVO portion, String duration, String durationDay,
                                       long monthDays, long totalDays) {
        BigDecimal totalHours = parsePositiveDecimal(duration);
        if (totalHours != null && totalHours.remainder(BigDecimal.valueOf(8)).compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal monthHours = BigDecimal.valueOf(monthDays)
                    .multiply(BigDecimal.valueOf(8))
                    .min(totalHours);
            portion.setDuration(monthHours.stripTrailingZeros().toPlainString());
            portion.setDurationDay(monthHours.divide(BigDecimal.valueOf(8), 1, java.math.RoundingMode.DOWN)
                    .stripTrailingZeros().toPlainString());
            return;
        }
        portion.setDuration(prorateDuration(duration, monthDays, totalDays));
        portion.setDurationDay(prorateDuration(durationDay, monthDays, totalDays));
    }

    private BigDecimal parsePositiveDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(value.trim());
            return decimal.compareTo(BigDecimal.ZERO) > 0 ? decimal : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private java.time.LocalDate toLocalDateSafe(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private long daysInclusive(Date begin, Date end) {
        long millis = end.getTime() - begin.getTime();
        if (millis < 0) {
            return 0;
        }
        return millis / 86400000L + 1;
    }

    private String prorateDuration(String durationText, long monthDays, long totalDays) {
        if (durationText == null || durationText.trim().isEmpty()) {
            return durationText;
        }
        try {
            BigDecimal total = new BigDecimal(durationText.trim());
            BigDecimal portion = total.multiply(BigDecimal.valueOf(monthDays))
                    .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
            // 0.5 小时粒度
            portion = portion.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP).stripTrailingZeros();
            return portion.toPlainString();
        } catch (NumberFormatException ex) {
            return durationText;
        }
    }

    @Override
    public Map<String, Object> checkMonthData(AttendanceApprovalMonthBO queryBO) {
        YearMonth month = parseMonth(queryBO);
        List<Long> employeeIds = normalizeEmployeeIds(queryBO);
        List<String> approvalTypes = normalizeApprovalTypes(queryBO);
        FetchMarkSummary summary = summarizeFetchMarks(month, employeeIds, approvalTypes);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month.format(MONTH_FORMATTER));
        result.put("exists", summary.completed);
        result.put("count", summary.count);
        return result;
    }

    @Override
    public Map<String, Object> fetchMonthData(AttendanceApprovalMonthBO queryBO) throws Exception {
        YearMonth month = parseMonth(queryBO);
        List<Long> employeeIds = normalizeEmployeeIds(queryBO);
        List<String> approvalTypes = normalizeApprovalTypes(queryBO);
        if (approvalTypes.isEmpty()) {
            throw new IllegalArgumentException("请选择审批类型");
        }
        String progressKey = buildFetchProgressKey();
        updateFetchProgress(progressKey, 5, FETCH_STATUS_RUNNING, "正在准备获取审批数据", false, false, 0L, null);
        
        // 写入通知：获取审批数据开始
        try {
            AdminMessage startMsg = new AdminMessage();
            startMsg.setTitle("获取审批数据中");
            startMsg.setContent("准备中... 0%");
            startMsg.setLabel(8);
            startMsg.setType(203); // HRM_APPROVAL_FETCH_RUNNING
            startMsg.setLinkUrl("/hrm/attendance/approval");
            startMsg.setCreateUser(0L);
            startMsg.setRecipientUser(0L);
            startMsg.setCreateTime(LocalDateTime.now());
            startMsg.setIsRead(0);
            adminMessageService.save(startMsg);
            // 保存通知ID到Redis，用于进度更新时同步通知内容
            redis.setex("attendance:notification:fetch:" + progressKey, 7200, String.valueOf(startMsg.getMessageId()));
            logger.info("[审批获取进度] 已写入开始通知, messageId={}", startMsg.getMessageId());
        } catch (Exception e) {
            logger.error("[审批获取进度] 写入开始通知失败", e);
        }
        
        try {
            updateFetchProgress(progressKey, 35, FETCH_STATUS_RUNNING, "正在从钉钉获取审批数据", false, false, 0L, null);
            // 透传前端发起的"审批发起时间窗口"(fetchStartTime/fetchEndTime, Long 毫秒)；
            // 未传(老调用方/定时任务)时由 sync 回退到目标业务月窗口。
            Long fetchStartTime = queryBO != null ? queryBO.getFetchStartTime() : null;
            Long fetchEndTime = queryBO != null ? queryBO.getFetchEndTime() : null;
            long insertedCount;
            // 每次点击"获取审批数据"都是真实全量抓取（不区分强制/普通，无员工冷却跳过）。
            // 降钉钉 API 配额靠运营"月全量≤2次 + 补拉用定向指定人"，不在代码层做冷却/强制分流。
            insertedCount = approvalSyncService.fetchMonthData(month, fetchStartTime, fetchEndTime,
                    employeeIds, approvalTypes);
            // 本次为覆盖全部员工的成功获取 → 打自然月标记：本月该范围已完成一次（用于下次拦截）
            if (employeeIds.isEmpty()) {
                monthlyFullSyncGuard.markFullSynced(MonthlyFullSyncGuard.BIZ_APPROVAL, getCompanyId());
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("month", month.format(MONTH_FORMATTER));
            result.put("insertedCount", insertedCount);
            updateFetchProgress(progressKey, 100, FETCH_STATUS_SUCCESS,
                    "审批数据获取完成，请确认后关闭进度条", true, true, insertedCount, null);
            
            // 写入通知：获取审批数据完成
            try {
                AdminMessage completeMsg = new AdminMessage();
                completeMsg.setTitle("获取审批数据完成");
                completeMsg.setContent("点击查看");
                completeMsg.setLabel(8); // 人资
                completeMsg.setType(204); // HRM_APPROVAL_FETCH_COMPLETE
                completeMsg.setLinkUrl("/hrm/attendance/approval");
                completeMsg.setCreateUser(0L); // 系统
                completeMsg.setRecipientUser(0L); // 系统级通知
                completeMsg.setCreateTime(LocalDateTime.now());
                completeMsg.setIsRead(0);
                adminMessageService.save(completeMsg);
                logger.info("[审批获取进度] 已写入完成通知");
            } catch (Exception e) {
                logger.error("[审批获取进度] 写入完成通知失败", e);
            }
            
            return result;
        } catch (Exception ex) {
            String message = toFriendlyApprovalFetchMessage(ex);
            updateFetchProgress(progressKey, null, FETCH_STATUS_FAILED, message, true, false, 0L,
                    Collections.singletonList(message));
            throw new IllegalStateException(message, ex);
        }
    }

    @Override
    public Map<String, Object> queryFetchProgress() {
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = progressTracker.queryProgress(FETCH_KEY_PREFIX, companyId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("progress", state.progress);
        result.put("status", state.status);
        result.put("message", state.message);
        result.put("done", state.done);
        result.put("success", state.success);
        result.put("error", ProgressTracker.STATUS_FAILED.equals(state.status));
        // 读取 insertedCount（审批获取特有字段）
        String insertedCountStr = redis.get(FETCH_KEY_PREFIX + ":inserted:" + companyId);
        long insertedCount = 0;
        try {
            insertedCount = insertedCountStr != null ? Long.parseLong(insertedCountStr) : 0;
        } catch (NumberFormatException e) {
            // ignore
        }
        result.put("insertedCount", insertedCount);
        result.put("errors", state.errors);
        return result;
    }

    private String buildFetchProgressKey() {
        if (CompanyContext.get() != null && CompanyContext.get().getCompanyId() != null) {
            return CompanyContext.get().getCompanyId();
        }
        return "default";
    }

    /** 获取当前公司 ID */
    private String getCompanyId() {
        if (CompanyContext.get() != null && CompanyContext.get().getCompanyId() != null) {
            return CompanyContext.get().getCompanyId();
        }
        return "default";
    }

    private void updateFetchProgress(String key,
                                     Integer progress,
                                     String status,
                                     String message,
                                     boolean done,
                                     boolean success,
                                     long insertedCount,
                                     List<String> errors) {
        ProgressTracker.ProgressState state = new ProgressTracker.ProgressState();
        state.progress = progress != null ? Math.max(0, Math.min(100, progress)) : 0;
        state.status = status;
        state.message = message;
        state.done = done;
        state.success = success;
        state.errors = errors == null ? Collections.emptyList() : new ArrayList<>(errors);
        if (progressTracker != null) {
            progressTracker.saveProgress(FETCH_KEY_PREFIX, key, state);
        }
        // 保留 insertedCount（审批获取特有字段）
        if (insertedCount > 0 && redis != null) {
            redis.setex(FETCH_KEY_PREFIX + ":inserted:" + key, 7200, String.valueOf(insertedCount));
        }
        // 同步更新通知中心的进度内容
        updateNotificationProgress("attendance:notification:fetch:" + key, progress, done, success, message);
    }

    /**
     * 更新通知中心的进度内容（实时百分比 + 当前处理明细；done 时写稳定终态标记）
     */
    private void updateNotificationProgress(String redisKey, Integer progress, boolean done, boolean success, String detailMessage) {
        try {
            String messageIdStr = redis.get(redisKey);
            if (messageIdStr == null || messageIdStr.isEmpty()) {
                return;
            }
            Long messageId = Long.parseLong(messageIdStr);
            String content;
            if (done) {
                content = success ? "已完成" : "获取失败";
                // 完成后删除Redis中的通知ID标记
                redis.del(redisKey);
            } else if (progress != null) {
                content = "进度 " + progress + "%";
                String detail = detailMessage == null ? "" : detailMessage.trim();
                if (!detail.isEmpty() && !detail.equals(content)) {
                    content = content + " · " + detail;
                }
            } else {
                return;
            }
            adminMessageService.updateContent(messageId, content);
        } catch (Exception e) {
            logger.warn("[审批获取进度] 更新通知内容失败", e);
        }
    }

    private String toFriendlyApprovalFetchMessage(Throwable throwable) {
        String message = collectThrowableMessage(throwable);
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (message.contains("请选择审批类型")) {
            return "请选择审批类型后再获取审批数据";
        }
        if (message.contains("请选择") || message.contains("月份")) {
            return "请选择获取审批数据的月份后重试";
        }
        if (message.contains("钉钉用户不存在") || message.contains("员工缺少") || message.contains("钉钉userId")) {
            return "部分员工缺少有效的钉钉用户信息，请维护员工钉钉信息后重试";
        }
        if (message.contains("限流") || message.contains("频控") || lower.contains("limitedfrequency")) {
            return "钉钉审批接口繁忙，请稍后重试或缩小员工范围";
        }
        if (message.contains("权限") || lower.contains("permission") || lower.contains("forbidden")) {
            return "当前钉钉应用未开通审批读取权限，请联系管理员开通后重试";
        }
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("unknown column") || lower.contains("database")) {
            return "审批数据获取失败，数据库连接或表结构需要管理员检查，处理后可重新获取";
        }
        return "获取审批数据失败，请处理提示中的数据问题后重试";
    }

    private String collectThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            builder.append(current.getClass().getName()).append(' ');
            current = current.getCause();
        }
        return builder.toString();
    }

    /** 提交路径同步抢占按公司运行标记；false = 该公司已有获取任务在运行 */
    public boolean tryBeginFetch(String companyId) {
        java.util.concurrent.atomic.AtomicReference<Long> flag =
                FETCH_RUNNING_MAP.computeIfAbsent(companyId, key -> new java.util.concurrent.atomic.AtomicReference<>(null));
        long now = System.currentTimeMillis();
        boolean[] acquired = {false};
        flag.updateAndGet(current -> {
            if (current == null || now - current > STALE_FETCH_RUNNING_MILLIS) {
                acquired[0] = true;
                return now;
            }
            return current;
        });
        return acquired[0];
    }

    /** 后台任务结束（成功/失败/提交失败）后归还可公司运行标记 */
    public void finishFetch(String companyId) {
        // 重试释放：最多3次，间隔递增
        for (int i = 0; i < 3; i++) {
            java.util.concurrent.atomic.AtomicReference<Long> flag = FETCH_RUNNING_MAP.get(companyId);
            if (flag != null) {
                Long current = flag.get();
                if (current != null) {
                    flag.set(null);
                    logger.info("[审批获取锁] 公司{} 释放锁成功（第{}次尝试）", companyId, i + 1);
                    return;
                }
            }
            logger.warn("[审批获取锁] 公司{} 锁已释放或不存在（第{}次尝试）", companyId, i + 1);
            try {
                Thread.sleep(100L * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** 检查并清理残留的锁（超过2小时视为异常） */
    public void checkAndCleanupStaleLocks() {
        long now = System.currentTimeMillis();
        FETCH_RUNNING_MAP.forEach((companyId, flag) -> {
            Long timestamp = flag.get();
            if (timestamp != null && now - timestamp > STALE_FETCH_RUNNING_MILLIS) {
                logger.warn("[审批获取锁] 清理公司{} 残留锁，锁龄={}ms", companyId, now - timestamp);
                flag.set(null);
            }
        });
    }

    /**
     * 带自动重试的审批获取入口（/fetchMonthData 使用）：
     * 重复拉取幂等（按审批实例主键跳过已入库记录），失败后指数退避自动重试（默认 2 次）；
     * 重试等待期进度保持 RUNNING 并显示重试文案，仅最终失败置为 FAILED。
     */
    @Override
    public Map<String, Object> fetchMonthDataWithAutoRetry(AttendanceApprovalMonthBO queryBO) throws Exception {
        int maxAttempts = 1 + Math.max(0, fetchAutoRetryAttempts);
        long backoffMs = Math.max(1000L, fetchAutoRetryBackoffMs);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<String, Object> data = fetchMonthData(queryBO);
                if (attempt > 1) {
                    logger.info("审批数据获取第{}次自动重试成功", attempt);
                }
                return data;
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? "" : ex.getMessage();
                if (attempt >= maxAttempts || message.contains("请选择") || message.contains("重复员工")) {
                    // 终态失败：自动重试全部耗尽(或不可重试的参数类错误)。此刻任务彻底结束、运行锁将释放，
                    // 才把断点置为 FAILED，前端据此显示「继续」(手动断点续传)。与考勤语义一致：仅最终失败才 FAILED。
                    try {
                        resumableJobCheckpoint.markTerminalFailed(
                                com.tianye.hrsystem.common.ResumableJobCheckpoint.BIZ_APPROVAL, getCompanyId());
                    } catch (Exception ckEx) {
                        logger.warn("审批获取终态失败时打断点FAILED异常(忽略): {}", ckEx.getMessage());
                    }
                    throw ex; // ProgressTracker/通知 的终态 FAILED 由 fetchMonthData 内 catch 标好
                }
                logger.warn("审批数据获取第{}次运行失败，{}ms 后自动重试: {}", attempt, backoffMs, message);
                updateFetchProgress(buildFetchProgressKey(), null, FETCH_STATUS_RUNNING,
                        "获取中断，正在自动重试（第" + (attempt + 1) + "次）...", false, false, 0L, null);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
                backoffMs = Math.min(backoffMs * 2, 300_000L);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /** 提交后、后台任务启动前同步写入 RUNNING 进度，覆盖上一次运行的旧完成状态，避免前端轮询瞬间假完成 */
    public void beginFetchProgress() {
        String companyId = getCompanyId();
        // 清理旧进度（ProgressTracker 会自动覆盖）
        updateFetchProgress(companyId, 2, FETCH_STATUS_RUNNING,
                "已提交获取审批数据请求，正在排队执行", false, false, 0L, null);
    }

    @Override
    public List<String> querySubtypeOptions() {
        Set<String> options = new LinkedHashSet<>();
        for (String option : DEFAULT_SUBTYPE_OPTIONS) {
            addSubtypeOption(options, option);
        }
        List<String> existingOptions = approvalRepository.findDistinctSubTypes();
        if (existingOptions != null) {
            for (String option : existingOptions) {
                addSubtypeOption(options, option);
            }
        }
        return new ArrayList<>(options);
    }

    @Override
    public Map<String, Object> updateSubtype(UpdateAttendanceApprovalSubtypeBO updateBO) {
        if (updateBO == null || updateBO.getApprovalId() == null || updateBO.getApprovalId().trim().isEmpty()) {
            throw new IllegalArgumentException("审批数据ID不能为空");
        }
        String subType = normalizeSubtype(updateBO.getSubType());
        if (subType.isEmpty()) {
            throw new IllegalArgumentException("审批子类型不能为空");
        }
        if (subType.length() > 50) {
            throw new IllegalArgumentException("审批子类型不能超过50个字符");
        }
        tbattendanceapprove approval = approvalRepository.findById(updateBO.getApprovalId().trim())
                .orElseThrow(() -> new IllegalArgumentException("审批数据不存在"));
        approval.setSubType(subType);
        approvalRepository.save(approval);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalId", approval.getId());
        result.put("subType", approval.getSubType());
        return result;
    }

    @Override
    public Map<String, Object> updateDuration(UpdateAttendanceApprovalDurationBO updateBO) {
        if (updateBO == null || updateBO.getApprovalId() == null || updateBO.getApprovalId().trim().isEmpty()) {
            throw new IllegalArgumentException("审批数据ID不能为空");
        }
        String duration = normalizeDuration(updateBO.getDuration());
        String durationUnit = normalizeDurationUnit(updateBO.getDurationUnit());
        tbattendanceapprove approval = approvalRepository.findById(updateBO.getApprovalId().trim())
                .orElseThrow(() -> new IllegalArgumentException("审批数据不存在"));
        approval.setDuration(duration);
        approval.setDurationUnit(durationUnit);
        approval.setDurationDay(toStoredDurationDay(duration, durationUnit));
        approvalRepository.save(approval);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalId", approval.getId());
        result.put("duration", approval.getDuration());
        result.put("durationUnit", approval.getDurationUnit());
        result.put("durationDay", approval.getDurationDay());
        return result;
    }

    @Override
    public Map<String, Object> updateStatisticsStatus(UpdateAttendanceApprovalStatisticsStatusBO updateBO) {
        if (updateBO == null || updateBO.getApprovalId() == null || updateBO.getApprovalId().trim().isEmpty()) {
            throw new IllegalArgumentException("审批数据ID不能为空");
        }
        String statisticsStatus = normalizeStatisticsStatus(updateBO.getStatisticsStatus());
        tbattendanceapprove approval = approvalRepository.findById(updateBO.getApprovalId().trim())
                .orElseThrow(() -> new IllegalArgumentException("审批数据不存在"));
        approval.setStatisticsStatus(statisticsStatus);
        approvalRepository.save(approval);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalId", approval.getId());
        result.put("statisticsStatus", approval.getStatisticsStatus() == null ? "" : approval.getStatisticsStatus());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> addManualApproval(AddAttendanceApprovalBO addBO) {
        List<Long> employeeIds = normalizeEmployeeIds(addBO == null ? null : addBO.getEmployeeIds());
        if (employeeIds.isEmpty()) {
            throw new IllegalArgumentException("请选择人员");
        }
        String tagName = normalizeSubtype(addBO.getTagName());
        if (tagName.isEmpty()) {
            throw new IllegalArgumentException("请选择审批类型");
        }
        String subType = normalizeSubtype(addBO.getSubType());
        if (subType.isEmpty()) {
            throw new IllegalArgumentException("请选择审批子类型");
        }
        BigDecimal manualDurationHours = resolveManualDurationHours(addBO);
        List<ManualApprovalRange> approvalRanges = resolveManualApprovalRanges(addBO, manualDurationHours);
        Date createTime = new Date();
        List<tbattendanceuser> attendanceUsers = attendanceUserRepository.findAllByEmpIdIn(employeeIds);
        Map<Long, tbattendanceuser> userMap = buildAttendanceUserMap(employeeIds, attendanceUsers);
        List<Long> missingEmployeeIds = employeeIds.stream()
                .filter(employeeId -> !userMap.containsKey(employeeId))
                .collect(Collectors.toList());
        if (!missingEmployeeIds.isEmpty()) {
            throw new IllegalArgumentException("以下员工未同步考勤映射，无法添加审批数据: " + missingEmployeeIds);
        }

        int insertedCount = 0;
        for (Long employeeId : employeeIds) {
            tbattendanceuser attendanceUser = userMap.get(employeeId);
            for (ManualApprovalRange approvalRange : approvalRanges) {
                tbattendanceapprove approval = new tbattendanceapprove();
                approval.setId(buildManualApprovalId(employeeId, createTime, insertedCount));
                approval.setTagName(tagName);
                approval.setSubType(subType);
                approval.setBizType(resolveBizType(tagName));
                approval.setBeginTime(approvalRange.beginTime);
                approval.setEndTime(approvalRange.endTime);
                approval.setDuration(approvalRange.duration);
                approval.setDurationUnit("小时");
                approval.setDurationDay(toStoredDurationDay(approvalRange.duration, "小时"));
                approval.setUserId(attendanceUser.getUserId());
                approval.setGroupId(attendanceUser.getGroupId());
                approval.setCreateTime(createTime);
                approval.setWorkDate(approvalRange.workDate);
                approvalRepository.save(approval);
                insertedCount++;
            }
        }

        BigDecimal totalDurationHours = approvalRanges.stream()
                .map(range -> range.durationHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .stripTrailingZeros();
        BigDecimal firstDurationHours = approvalRanges.get(0).durationHours;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("insertedCount", insertedCount);
        result.put("rangeCount", approvalRanges.size());
        result.put("duration", firstDurationHours.stripTrailingZeros().toPlainString());
        result.put("durationUnit", "小时");
        result.put("durationDays", firstDurationHours.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        result.put("totalDuration", totalDurationHours.toPlainString());
        result.put("totalDurationDays", totalDurationHours.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        result.put("createTime", createTime);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> deleteApproval(DeleteAttendanceApprovalBO deleteBO) {
        if (deleteBO == null || deleteBO.getApprovalId() == null || deleteBO.getApprovalId().trim().isEmpty()) {
            throw new IllegalArgumentException("审批数据ID不能为空");
        }
        String approvalId = deleteBO.getApprovalId().trim();
        tbattendanceapprove approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("审批数据不存在"));
        approvalRepository.delete(approval);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalId", approvalId);
        result.put("deleted", Boolean.TRUE);
        return result;
    }

    private void addSubtypeOption(Set<String> options, String option) {
        String normalized = normalizeSubtype(option);
        if (!normalized.isEmpty()) {
            options.add(normalized);
        }
    }

    private String normalizeSubtype(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeDuration(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("审批时长不能为空");
        }
        normalized = normalized
                .replace("小时", "")
                .replace("分钟", "")
                .replace("天", "")
                .trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("审批时长不能为空");
        }
        BigDecimal duration;
        try {
            duration = new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("审批时长格式不合法");
        }
        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("审批时长必须大于0");
        }
        return duration.stripTrailingZeros().toPlainString();
    }

    private String normalizeDurationUnit(String value) {
        String normalized = value == null ? "" : value.trim();
        if ("日".equals(normalized)) {
            normalized = "天";
        }
        if (!DURATION_UNIT_OPTIONS.contains(normalized)) {
            throw new IllegalArgumentException("审批时长单位不合法");
        }
        return normalized;
    }

    /**
     * 由存储的 (时长, 单位) 推导一致的"天"口径（durationDay 派生列），供手动新增/修改写入。
     * 口径：小时→/8、分钟→/60/8、天→原值，保留 2 位小数。
     */
    private String toStoredDurationDay(String duration, String unit) {
        if (duration == null || duration.trim().isEmpty()) {
            return "";
        }
        BigDecimal value;
        try {
            value = new BigDecimal(normalizeDuration(duration));
        } catch (IllegalArgumentException ex) {
            return "";
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        BigDecimal days;
        if ("分钟".equals(unit)) {
            days = value.divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(8), 6, RoundingMode.HALF_UP);
        } else if ("天".equals(unit)) {
            days = value;
        } else {
            days = value.divide(BigDecimal.valueOf(8), 6, RoundingMode.HALF_UP);
        }
        return days.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String normalizeStatisticsStatus(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || STATISTICS_STATUS_INCLUDED_LABEL.equals(normalized)) {
            return "";
        }
        if (STATISTICS_STATUS_CANCELLED.equals(normalized)) {
            return STATISTICS_STATUS_CANCELLED;
        }
        throw new IllegalArgumentException("审批统计状态不合法");
    }

    private BigDecimal resolveManualDurationHours(AddAttendanceApprovalBO addBO) {
        if (addBO == null || addBO.getDuration() == null || addBO.getDuration().trim().isEmpty()) {
            return null;
        }
        BigDecimal duration = new BigDecimal(normalizeDuration(addBO.getDuration()));
        String unitText = addBO.getDurationUnit() == null || addBO.getDurationUnit().trim().isEmpty()
                ? "小时"
                : addBO.getDurationUnit();
        String durationUnit = normalizeDurationUnit(unitText);
        if ("分钟".equals(durationUnit)) {
            return duration.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
        if ("天".equals(durationUnit)) {
            return duration.multiply(BigDecimal.valueOf(8)).setScale(2, RoundingMode.HALF_UP);
        }
        return duration.setScale(2, RoundingMode.HALF_UP);
    }

    private List<ManualApprovalRange> resolveManualApprovalRanges(AddAttendanceApprovalBO addBO, BigDecimal manualDurationHours) {
        List<ManualApprovalRange> ranges = new ArrayList<>();
        List<AddAttendanceApprovalBO.ApprovalRangeBO> requestRanges = addBO.getApprovalRanges();
        if (requestRanges != null && !requestRanges.isEmpty()) {
            int index = 0;
            for (AddAttendanceApprovalBO.ApprovalRangeBO rangeBO : requestRanges) {
                index++;
                if (rangeBO == null) {
                    continue;
                }
                Date beginTime = rangeBO.getBeginTime();
                Date endTime = rangeBO.getEndTime();
                ranges.add(buildManualApprovalRange(beginTime, endTime, "第" + index + "个审批时间段"));
            }
        } else {
            ranges.add(buildManualApprovalRange(addBO.getBeginTime(), addBO.getEndTime(), ""));
        }
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("请选择开始时间和结束时间");
        }
        return manualDurationHours == null ? ranges : applyManualDurationHours(ranges, manualDurationHours);
    }

    private List<ManualApprovalRange> applyManualDurationHours(List<ManualApprovalRange> ranges, BigDecimal manualDurationHours) {
        if (ranges.size() == 1) {
            return Collections.singletonList(copyManualApprovalRangeWithDuration(ranges.get(0), manualDurationHours));
        }
        BigDecimal autoTotalHours = ranges.stream()
                .map(range -> range.durationHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingHours = manualDurationHours;
        List<ManualApprovalRange> adjustedRanges = new ArrayList<>();
        for (int index = 0; index < ranges.size(); index++) {
            ManualApprovalRange range = ranges.get(index);
            BigDecimal currentHours;
            if (index == ranges.size() - 1) {
                currentHours = remainingHours;
            } else if (autoTotalHours.compareTo(BigDecimal.ZERO) > 0) {
                currentHours = manualDurationHours.multiply(range.durationHours)
                        .divide(autoTotalHours, 2, RoundingMode.HALF_UP);
                remainingHours = remainingHours.subtract(currentHours);
            } else {
                currentHours = manualDurationHours.divide(BigDecimal.valueOf(ranges.size()), 2, RoundingMode.HALF_UP);
                remainingHours = remainingHours.subtract(currentHours);
            }
            adjustedRanges.add(copyManualApprovalRangeWithDuration(range, currentHours));
        }
        return adjustedRanges;
    }

    private ManualApprovalRange copyManualApprovalRangeWithDuration(ManualApprovalRange range, BigDecimal durationHours) {
        BigDecimal normalizedDurationHours = durationHours.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return new ManualApprovalRange(
                range.beginTime,
                range.endTime,
                normalizedDurationHours,
                normalizedDurationHours.toPlainString(),
                range.workDate
        );
    }

    private ManualApprovalRange buildManualApprovalRange(Date beginTime, Date endTime, String label) {
        String prefix = label == null || label.isEmpty() ? "" : label;
        if (beginTime == null || endTime == null) {
            throw new IllegalArgumentException(prefix + "请选择开始时间和结束时间");
        }
        if (!endTime.after(beginTime)) {
            throw new IllegalArgumentException(prefix + "结束时间必须晚于开始时间");
        }
        BigDecimal durationHours = calculateDurationHours(beginTime, endTime);
        return new ManualApprovalRange(
                beginTime,
                endTime,
                durationHours,
                durationHours.stripTrailingZeros().toPlainString(),
                toStartOfDay(beginTime)
        );
    }

    private long countFetchMarks(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) {
        return summarizeFetchMarks(month, employeeIds, approvalTypes).count;
    }

    private FetchMarkSummary summarizeFetchMarks(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) {
        if (approvalTypes == null || approvalTypes.isEmpty()) {
            return new FetchMarkSummary(0L, false);
        }
        List<Long> targetEmployeeIds = approvalSyncService.resolveFetchTargetEmployeeIds(employeeIds);
        if (targetEmployeeIds.isEmpty()) {
            return new FetchMarkSummary(0L, false);
        }
        List<String> userIds = resolveUserIds(targetEmployeeIds);
        if (userIds.isEmpty()) {
            return new FetchMarkSummary(0L, false);
        }
        long count = 0L;
        boolean completed = true;
        for (String approvalType : approvalTypes) {
            long currentCount = fetchMarkRepository.countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType(
                    month.format(MONTH_FORMATTER),
                    FETCH_VERSION,
                    userIds,
                    approvalType
            );
            count += currentCount;
            if (currentCount < userIds.size()) {
                completed = false;
            }
        }
        return new FetchMarkSummary(count, completed);
    }

    private boolean isFetchCompleted(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) {
        if (employeeIds == null || employeeIds.isEmpty() || approvalTypes == null || approvalTypes.isEmpty()) {
            return false;
        }
        List<String> userIds = resolveUserIds(employeeIds);
        if (userIds.isEmpty()) {
            return false;
        }
        for (String approvalType : approvalTypes) {
            long count = fetchMarkRepository.countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType(
                    month.format(MONTH_FORMATTER),
                    FETCH_VERSION,
                    userIds,
                    approvalType
            );
            if (count < userIds.size()) {
                return false;
            }
        }
        return true;
    }

    private long countMonthData(YearMonth month, List<Long> employeeIds) {
        Date begin = toDate(month.atDay(1));
        Date end = toDate(month.atEndOfMonth().atTime(23, 59, 59));
        if (employeeIds == null || employeeIds.isEmpty()) {
            return approvalRepository.countByBeginTimeBetween(begin, end);
        }
        List<String> userIds = resolveUserIds(employeeIds);
        if (userIds.isEmpty()) {
            return 0L;
        }
        return approvalRepository.countByBeginTimeBetweenAndUserIdIn(begin, end, userIds);
    }

    private YearMonth parseMonth(AttendanceApprovalMonthBO queryBO) {
        if (queryBO == null || queryBO.getMonth() == null || queryBO.getMonth().trim().isEmpty()) {
            throw new IllegalArgumentException("请选择月份");
        }
        try {
            return YearMonth.parse(queryBO.getMonth().trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("月份格式错误，应为yyyy-MM");
        }
    }

    private List<Long> normalizeEmployeeIds(AttendanceApprovalMonthBO queryBO) {
        if (queryBO == null || queryBO.getEmployeeIds() == null || queryBO.getEmployeeIds().isEmpty()) {
            return new ArrayList<>();
        }
        return normalizeEmployeeIds(queryBO.getEmployeeIds());
    }

    private List<Long> normalizeEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return new ArrayList<>();
        }
        return employeeIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeApprovalTypes(AttendanceApprovalMonthBO queryBO) {
        if (queryBO == null || queryBO.getApprovalTypes() == null || queryBO.getApprovalTypes().isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> types = new LinkedHashSet<>();
        for (String approvalType : queryBO.getApprovalTypes()) {
            if (approvalType == null || approvalType.trim().isEmpty()) {
                continue;
            }
            String value = approvalType.trim().toLowerCase();
            if ("all".equals(value) || "overtime".equals(value) || "misscard".equals(value) || "leave".equals(value) || "travel".equals(value)) {
                types.add(value);
            }
        }
        return new ArrayList<>(types);
    }

    private List<String> resolveUserIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<tbattendanceuser> users = attendanceUserRepository.findAllByEmpIdIn(employeeIds);
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> userIds = new LinkedHashSet<>();
        for (Long employeeId : employeeIds) {
            for (tbattendanceuser user : users) {
                if (user == null || user.getEmpId() == null || !employeeId.equals(user.getEmpId())) {
                    continue;
                }
                if (user.getUserId() != null && !user.getUserId().trim().isEmpty()) {
                    userIds.add(user.getUserId());
                    break;
                }
            }
        }
        return new ArrayList<>(userIds);
    }

    private Date toDate(java.time.LocalDateTime dateTime) {
        return java.sql.Timestamp.valueOf(dateTime);
    }

    private Date toDate(java.time.LocalDate date) {
        return java.sql.Timestamp.valueOf(date.atStartOfDay());
    }

    private BigDecimal calculateDurationHours(Date beginTime, Date endTime) {
        long millis = endTime.getTime() - beginTime.getTime();
        return BigDecimal.valueOf(millis)
                .divide(BigDecimal.valueOf(HOUR_MILLIS), 2, RoundingMode.HALF_UP);
    }

    private Date toStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Map<Long, tbattendanceuser> buildAttendanceUserMap(List<Long> employeeIds, List<tbattendanceuser> attendanceUsers) {
        Map<Long, tbattendanceuser> userMap = new LinkedHashMap<>();
        if (attendanceUsers == null || attendanceUsers.isEmpty()) {
            return userMap;
        }
        Set<Long> requestedEmployeeIds = new LinkedHashSet<>(employeeIds);
        for (tbattendanceuser user : attendanceUsers) {
            if (user == null || user.getEmpId() == null || !requestedEmployeeIds.contains(user.getEmpId())) {
                continue;
            }
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            userMap.merge(user.getEmpId(), user, this::preferAttendanceUserMapping);
        }
        return userMap;
    }

    private tbattendanceuser preferAttendanceUserMapping(tbattendanceuser current, tbattendanceuser candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        Date currentCreateTime = current.getCreateTime();
        Date candidateCreateTime = candidate.getCreateTime();
        if (currentCreateTime == null && candidateCreateTime != null) {
            return candidate;
        }
        if (currentCreateTime != null && candidateCreateTime == null) {
            return current;
        }
        if (currentCreateTime != null && candidateCreateTime != null) {
            int compare = candidateCreateTime.compareTo(currentCreateTime);
            if (compare != 0) {
                return compare > 0 ? candidate : current;
            }
        }
        Integer currentId = current.getId();
        Integer candidateId = candidate.getId();
        if (currentId == null && candidateId != null) {
            return candidate;
        }
        if (currentId != null && candidateId == null) {
            return current;
        }
        if (currentId != null && candidateId != null) {
            return candidateId > currentId ? candidate : current;
        }
        return current;
    }

    private Long resolveBizType(String tagName) {
        String text = tagName == null ? "" : tagName.trim();
        if (text.contains("加班")) {
            return 1L;
        }
        if (text.contains("出差") || text.contains("外出")) {
            return 2L;
        }
        if (text.contains("请假")) {
            return 3L;
        }
        return null;
    }

    private String buildManualApprovalId(Long employeeId, Date createTime, int index) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "MANUAL-" + createTime.getTime() + "-" + employeeId + "-" + index + "-" + random;
    }

    private static class FetchMarkSummary {
        private final long count;
        private final boolean completed;

        private FetchMarkSummary(long count, boolean completed) {
            this.count = count;
            this.completed = completed;
        }
    }

    private static class ManualApprovalRange {
        private final Date beginTime;
        private final Date endTime;
        private final BigDecimal durationHours;
        private final String duration;
        private final Date workDate;

        private ManualApprovalRange(Date beginTime, Date endTime, BigDecimal durationHours, String duration, Date workDate) {
            this.beginTime = beginTime;
            this.endTime = endTime;
            this.durationHours = durationHours;
            this.duration = duration;
            this.workDate = workDate;
        }
    }
}
