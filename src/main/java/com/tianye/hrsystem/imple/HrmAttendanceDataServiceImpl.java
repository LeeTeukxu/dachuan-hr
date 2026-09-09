package com.tianye.hrsystem.imple;

import cn.hutool.core.collection.ListUtil;
import com.tianye.hrsystem.common.MonthlyFullSyncGuard;
import com.tianye.hrsystem.common.ProgressTracker;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.ApplicationContextHolder;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.controller.HrmAttendanceDataController;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.*;
import com.tianye.hrsystem.service.ddTalk.IGroupManager;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import com.tianye.hrsystem.service.ddTalk.IUserManager;
import com.tianye.hrsystem.util.MyDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmAttendanceDataServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:16
 **/
@Service
public class HrmAttendanceDataServiceImpl implements IHrmAttendanceDataService {

    // Redis Key 前缀
    private static final String SYNC_PROGRESS_KEY = "attendance:sync:progress";
    private static final String SYNC_STEP_KEY = "attendance:sync:step";
    private static final String SYNC_PROCESSED_EMPS_KEY = "attendance:sync:processed_emps";
    private static final String SYNC_PARAMS_KEY = "attendance:sync:params";
    private static final String SYNC_STATUS_KEY = "attendance:sync:status";
    private static final String SYNC_MESSAGE_KEY = "attendance:sync:message";
    private static final String SYNC_ERRORS_KEY = "attendance:sync:errors";
    // 步骤7子步骤进度Key
    private static final String SYNC_STEP7A_PROCESSED_KEY = "attendance:sync:step7a_processed"; // 请假数据
    private static final String SYNC_STEP7B_PROCESSED_KEY = "attendance:sync:step7b_processed"; // 假期数据
    // 进度写入的服务端时间戳与最近一次提交的排队标记：
    // 前端用 updateTime >= queuedAt 判定完成信号属于本次运行，防止读到上一次运行的旧完成状态
    private static final String SYNC_UPDATE_TIME_KEY = "attendance:sync:update_time";
    private static final String SYNC_QUEUED_KEY = "attendance:sync:queued";
    // 「本次运行为全量同步」待标记：Controller 校验通过后写入，后台同步成功时据此写自然月月锁、失败则清除
    private static final String SYNC_FULLSYNC_PENDING_KEY = "attendance:sync:fullsync_pending";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_IDLE = "IDLE";
    // ProgressTracker 前缀
    private static final String SYNC_KEY_PREFIX = "attendance:sync";
    // 进度过期时间：24小时
    private static final int PROGRESS_EXPIRE_SECONDS = 86400;
    // 并行处理线程数（步骤3-4调用钉钉API，保守设置）
    private static final int PARALLEL_THREADS = 2;
    // 步骤7并行线程数（降低以避免钉钉限流，实测5线程失败率75%）
    private static final int STEP7_PARALLEL_THREADS = 2;
    // 每批次处理的员工数
    private static final int BATCH_SIZE = 1;
    // 步骤7每批次处理的员工数（降低批次大小以减少单请求处理时间）
    private static final int STEP7_BATCH_SIZE = 1;
    // 数据库连接异常重试次数（用于大批量同步时的瞬时断连）
    private static final int DB_RETRY_MAX_ATTEMPTS = 3;
    // 明细接口天然支持最多 50 人批量查询，按接口上限分批可显著降低调用量
    private static final int DETAIL_BATCH_SIZE = 50;

    @Autowired
    IAttendancePlanService planService;
    @Autowired
    IAttendanceDetailService detailService;
    @Autowired
    ILeaveRecordDtaService leaveService;
    @Autowired
    IHolidayDataService holidayService;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    IHrmAttendanceReport report;
    Logger logger= LoggerFactory.getLogger(HrmAttendanceDataServiceImpl.class);
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    IUserManager userManager;
    @Autowired
    IGroupManager groupManager;
    @Autowired
    hrmAttendanceReportDataRepository dataRep;
    @Autowired
    hrmEmployeeRepository empRep;
    @Autowired
    Redis redis;
    @Autowired
    ProgressTracker progressTracker;
    @Autowired
    AdminMessageServiceImpl adminMessageService;

    @Autowired
    private MonthlyFullSyncGuard monthlyFullSyncGuard;

    // 同步失败自动重试：次数与退避基数可配（默认 2 次自动重试、60s 起步指数退避、封顶 5 分钟）
    @Value("${hrm.attendance-sync.auto-retry.attempts:2}")
    private int syncAutoRetryAttempts = 2;
    @Value("${hrm.attendance-sync.auto-retry.backoff-ms:60000}")
    private long syncAutoRetryBackoffMs = 60000L;

    /**
     * 获取当前同步进度信息
     * @return 进度信息Map，包含step, processedEmps, params等
     */
    /** 同步进度键按公司隔离：Redis 前缀为恒等实现，键名必须显式带 companyId，否则跨公司并发互相覆盖进度 */
    private String syncKey(String baseKey) {
        LoginUserInfo info = CompanyContext.get();
        String companyId = info != null && info.getCompanyId() != null ? info.getCompanyId() : "unknown";
        return baseKey + ":" + companyId;
    }

    /** 获取当前公司 ID */
    private String getCompanyId() {
        LoginUserInfo info = CompanyContext.get();
        return info != null && info.getCompanyId() != null ? info.getCompanyId() : "unknown";
    }

    private long parseRedisLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** 同步进度使用 StringRedisSerializer，所有状态值统一以字符串持久化。 */
    private void saveSyncValue(String key, Object value) {
        redis.setex(syncKey(key), PROGRESS_EXPIRE_SECONDS, String.valueOf(value));
    }

    /** 提交同步时写入排队标记（服务端时间戳）：前端据此区分本次运行与上一次运行的旧进度 */
    public long markSyncQueued() {
        String companyId = getCompanyId();
        long queuedAt = progressTracker.markQueued(SYNC_KEY_PREFIX, companyId);
        logger.info("[同步进度] 公司{} 已标记排队 queuedAt={}", companyId, queuedAt);
        return queuedAt;
    }

    /** 自动重试等待期：保持 RUNNING 状态并刷新文案，避免前端把中间失败当成最终结果 */
    public void markSyncRetrying(int attempt, long backoffMs) {
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = progressTracker.queryProgress(SYNC_KEY_PREFIX, companyId);
        state.status = ProgressTracker.STATUS_RUNNING;
        state.message = "同步中断，正在自动重试（第" + (attempt + 1) + "次），约" + Math.max(1, Math.round(backoffMs / 1000.0)) + "秒后开始";
        progressTracker.saveProgress(SYNC_KEY_PREFIX, companyId, state);
    }

    /**
     * 带自动重试的同步入口（/sync、/syncAll 使用）：
     * 首次运行失败后按指数退避自动从断点续传（默认 2 次自动重试、60s 起步、封顶 5 分钟）；
     * 重试等待期进度保持 RUNNING 并显示重试文案，仅在最终失败时置为 FAILED。
     */
    @Override
    public boolean SyncDataWithAutoRetry(String EmpIDS, Date Begin, Date End) throws Exception {
        int maxAttempts = 1 + Math.max(0, syncAutoRetryAttempts);
        long backoffMs = Math.max(1000L, syncAutoRetryBackoffMs);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean ok = attempt == 1
                        ? SyncData(EmpIDS, Begin, End)
                        : SyncDataWithResume(EmpIDS, Begin, End, true);
                if (attempt > 1) {
                    logger.info("考勤同步第{}次自动重试成功", attempt);
                }
                return ok;
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    logger.error("考勤同步自动重试{}次后仍失败", maxAttempts - 1, ex);
                    throw ex; // SyncData/SyncDataWithResume 内部已把进度标为 FAILED
                }
                logger.warn("考勤同步第{}次运行失败，{}ms 后自动从断点重试: {}", attempt, backoffMs, ex.getMessage());
                markSyncRetrying(attempt, backoffMs);
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

    @Override
    public Map<String, Object> getSyncProgress() {
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = progressTracker.queryProgress(SYNC_KEY_PREFIX, companyId);

        // 读取断点续传所需的字段
        String processedEmps = redis.get(syncKey(SYNC_PROCESSED_EMPS_KEY));
        String params = redis.get(syncKey(SYNC_PARAMS_KEY));
        String step7aProcessed = redis.get(syncKey(SYNC_STEP7A_PROCESSED_KEY));
        String step7bProcessed = redis.get(syncKey(SYNC_STEP7B_PROCESSED_KEY));

        // 从状态中提取当前步骤（如果有的话，否则从 processedEmps 推断）
        int currentStep = state.progress > 0 ? inferStepFromProgress(state.progress) : 0;
        boolean hasProgress = currentStep > 0;
        int totalCount = parseTotalCountFromParams(params);
        int processedCount = countCsvItems(processedEmps);
        int step7aCount = countCsvItems(step7aProcessed);
        int step7bCount = countCsvItems(step7bProcessed);

        // 计算实际进度百分比
        int percent = state.success
                ? 100
                : calculateProgressPercent(currentStep, hasProgress, totalCount, processedCount, step7aCount, step7bCount);

        Map<String, Object> progress = new HashMap<>();
        progress.put("hasProgress", hasProgress && !state.success);
        progress.put("currentStep", currentStep);
        progress.put("processedEmps", processedEmps != null ? processedEmps : "");
        progress.put("params", params != null ? params : "");
        progress.put("totalCount", totalCount);
        progress.put("processedCount", processedCount);
        progress.put("progress", percent);
        progress.put("percent", percent);
        progress.put("status", state.status);
        progress.put("done", state.done);
        progress.put("success", state.success);
        progress.put("error", ProgressTracker.STATUS_FAILED.equals(state.status));
        progress.put("errors", state.errors);
        progress.put("message", state.message);
        progress.put("updateTime", state.updateTime);
        progress.put("queuedAt", state.queuedAt);

        return progress;
    }

    /** 根据进度百分比推断当前步骤 */
    private int inferStepFromProgress(int progress) {
        if (progress <= 5) return 1;
        if (progress <= 15) return 2;
        if (progress <= 38) return 3;
        if (progress <= 60) return 4;
        if (progress <= 70) return 5;
        if (progress <= 82) return 6;
        return 7;
    }

    private String normalizeSyncStatus(String status, boolean hasProgress) {
        if (status == null || status.trim().isEmpty()) {
            return hasProgress ? STATUS_RUNNING : STATUS_IDLE;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_SUCCESS.equals(normalized) || STATUS_FAILED.equals(normalized) || STATUS_RUNNING.equals(normalized)) {
            return normalized;
        }
        return hasProgress ? STATUS_RUNNING : STATUS_IDLE;
    }

    private List<String> parseProgressErrors(String savedErrors) {
        if (savedErrors == null || savedErrors.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(savedErrors.split("\\n"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private String resolveProgressMessage(String savedMessage, String status, int step, boolean hasProgress) {
        if (savedMessage != null && !savedMessage.trim().isEmpty()) {
            return savedMessage;
        }
        if (STATUS_SUCCESS.equals(status)) {
            return "同步完成，请确认后关闭进度条";
        }
        if (STATUS_FAILED.equals(status)) {
            return "同步考勤失败，请处理提示中的数据问题后重试";
        }
        return hasProgress ? getStepLabel(step) : "暂无同步任务";
    }

    private int parseTotalCountFromParams(String params) {
        if (params == null || params.trim().isEmpty()) {
            return 0;
        }
        String[] segments = params.split("\\|", 3);
        if (segments.length == 0) {
            return 0;
        }
        return countCsvItems(segments[0]);
    }

    private int countCsvItems(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return 0;
        }
        return (int) Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .count();
    }

    private int calculateProgressPercent(int step, boolean hasProgress, int totalCount, int processedCount,
                                         int step7aCount, int step7bCount) {
        if (!hasProgress || step <= 0) {
            return 0;
        }
        double base;
        switch (step) {
            case 1:
                base = 5;
                break;
            case 2:
                base = 15;
                break;
            case 3:
                base = 20 + getRatio(processedCount, totalCount) * 18;
                break;
            case 4:
                base = 40 + getRatio(processedCount, totalCount) * 20;
                break;
            case 5:
                base = 70;
                break;
            case 6:
                base = 82;
                break;
            case 7:
                double leaveRatio = getRatio(step7aCount, totalCount);
                double holidayRatio = getRatio(step7bCount, totalCount);
                base = 85 + ((leaveRatio + holidayRatio) / 2.0) * 14;
                break;
            default:
                base = 96;
                break;
        }
        return clampPercent(base);
    }

    private double getRatio(int done, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(1.0, Math.max(0.0, done * 1.0 / total));
    }

    private int clampPercent(double value) {
        int rounded = (int) Math.round(value);
        if (rounded < 0) {
            return 0;
        }
        if (rounded > 99) {
            return 99;
        }
        return rounded;
    }

    private String getStepLabel(int step) {
        switch (step) {
            case 1:
                return "同步组织架构";
            case 2:
                return "同步用户信息";
            case 3:
                return "同步考勤计划";
            case 4:
                return "同步考勤明细";
            case 5:
                return "更新考勤报表";
            case 6:
                return "清理历史数据";
            case 7:
                return "同步请假与假期数据";
            default:
                return "准备同步";
        }
    }

    /**
     * 归一化用户映射，避免同一 userId 多条历史脏数据导致错配。
     */
    private List<tbattendanceuser> normalizeUsers(List<tbattendanceuser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, tbattendanceuser> byUserId = new LinkedHashMap<>();
        for (tbattendanceuser user : users) {
            if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            tbattendanceuser current = byUserId.get(user.getUserId());
            if (current == null) {
                byUserId.put(user.getUserId(), user);
            } else {
                byUserId.put(user.getUserId(), selectPreferredUser(current, user));
            }
        }
        return new ArrayList<>(byUserId.values());
    }

    private tbattendanceuser selectPreferredUser(tbattendanceuser left, tbattendanceuser right) {
        boolean leftValidEmp = left.getEmpId() != null && left.getEmpId() > 0;
        boolean rightValidEmp = right.getEmpId() != null && right.getEmpId() > 0;
        if (leftValidEmp != rightValidEmp) {
            return leftValidEmp ? left : right;
        }

        Date leftCreate = left.getCreateTime();
        Date rightCreate = right.getCreateTime();
        if (leftCreate != null && rightCreate != null && !leftCreate.equals(rightCreate)) {
            return leftCreate.after(rightCreate) ? left : right;
        }
        if (leftCreate == null && rightCreate != null) {
            return right;
        }
        if (leftCreate != null && rightCreate == null) {
            return left;
        }

        Integer leftId = left.getId();
        Integer rightId = right.getId();
        if (leftId != null && rightId != null && !leftId.equals(rightId)) {
            return leftId > rightId ? left : right;
        }
        if (leftId == null && rightId != null) {
            return right;
        }
        return left;
    }

    /**
     * 清除同步进度（同步成功或需要重新开始时调用）
     */
    public void clearSyncProgress() {
        String companyId = getCompanyId();
        progressTracker.clearProgress(SYNC_KEY_PREFIX, companyId);
        // 清除断点续传所需的字段
        redis.del(syncKey(SYNC_PROCESSED_EMPS_KEY), syncKey(SYNC_PARAMS_KEY),
                syncKey(SYNC_STEP7A_PROCESSED_KEY), syncKey(SYNC_STEP7B_PROCESSED_KEY));
        logger.info("已清除同步进度缓存");
    }

    /**
     * 保存同步进度
     */
    private void saveSyncProgress(int step, String processedEmps, String params) {
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = new ProgressTracker.ProgressState();
        state.progress = calculateProgressPercent(step, true, parseTotalCountFromParams(params),
                countCsvItems(processedEmps), 0, 0);
        state.status = ProgressTracker.STATUS_RUNNING;
        state.message = getStepLabel(step);
        state.done = false;
        state.queuedAt = parseRedisLong(redis.get(syncKey(SYNC_QUEUED_KEY)));
        progressTracker.saveProgress(SYNC_KEY_PREFIX, companyId, state);
        // 保留断点续传所需的字段
        redis.setex(syncKey(SYNC_PROCESSED_EMPS_KEY), PROGRESS_EXPIRE_SECONDS, processedEmps);
        redis.setex(syncKey(SYNC_PARAMS_KEY), PROGRESS_EXPIRE_SECONDS, params);
        logger.info("[同步进度] 公司{} 步骤{}/7 {} 进度{}%", companyId, step, getStepLabel(step), state.progress);
        // 同步更新通知中心的进度内容（带已处理/总数明细，避免操作员干等百分比）
        int totalCount = parseTotalCountFromParams(params);
        int processedCount = countCsvItems(processedEmps);
        String detail = buildSyncDetail(step, processedCount, totalCount);
        updateSyncNotificationProgress(companyId, state.progress, detail);
    }

    /** 拼接考勤同步通知内容里百分号之后的明细段（步骤名 + 已处理员工计数）。 */
    private String buildSyncDetail(int step, int processedCount, int totalCount) {
        String label = getStepLabel(step);
        if (processedCount > 0 && totalCount > 0) {
            return label + " · 已处理 " + processedCount + " / 共 " + totalCount + " 员工";
        }
        return label;
    }

    /**
     * 更新同步考勤通知的进度内容（实时百分比 + 明细）
     */
    private void updateSyncNotificationProgress(String companyId, int percent, String detailMessage) {
        try {
            String messageIdStr = redis.get("attendance:notification:sync:" + companyId);
            if (messageIdStr == null || messageIdStr.isEmpty()) {
                return;
            }
            Long messageId = Long.parseLong(messageIdStr);
            String content = "进度 " + percent + "%";
            String detail = detailMessage == null ? "" : detailMessage.trim();
            if (!detail.isEmpty()) {
                content = content + " · " + detail;
            }
            adminMessageService.updateContent(messageId, content);
        } catch (Exception e) {
            logger.warn("[同步进度] 更新通知内容失败", e);
        }
    }

    private void saveFinalSyncProgress(String params) {
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = new ProgressTracker.ProgressState();
        state.progress = 100;
        state.status = ProgressTracker.STATUS_SUCCESS;
        state.message = "同步完成，请确认后关闭进度条";
        state.done = true;
        state.success = true;
        state.queuedAt = parseRedisLong(redis.get(syncKey(SYNC_QUEUED_KEY)));
        progressTracker.saveProgress(SYNC_KEY_PREFIX, companyId, state);
        // 保留断点续传所需的字段
        redis.setex(syncKey(SYNC_PARAMS_KEY), PROGRESS_EXPIRE_SECONDS, params);
        logger.info("[同步进度] 公司{} 同步完成", companyId);

        // 本次运行为全量同步且已成功 → 打自然月标记：本月该范围已完成一次全量同步；随后清除待标记
        try {
            if (redis.exists(syncKey(SYNC_FULLSYNC_PENDING_KEY))) {
                monthlyFullSyncGuard.markFullSynced(MonthlyFullSyncGuard.BIZ_ATTENDANCE, companyId);
                redis.del(syncKey(SYNC_FULLSYNC_PENDING_KEY));
                logger.info("[同步进度] 公司{} 全量同步成功，已写本月标记", companyId);
            }
        } catch (Exception e) {
            logger.warn("[同步进度] 公司{} 全量同步打本月标记失败", companyId, e);
        }
        
        // 更新运行中通知的内容为"已完成"，并清理Redis标记
        try {
            String runningMsgIdStr = redis.get("attendance:notification:sync:" + companyId);
            if (runningMsgIdStr != null && !runningMsgIdStr.isEmpty()) {
                adminMessageService.updateContent(Long.parseLong(runningMsgIdStr), "已完成");
                redis.del("attendance:notification:sync:" + companyId);
            }
        } catch (Exception e) {
            logger.warn("[同步进度] 更新运行中通知失败", e);
        }
        
        // 写入通知：同步考勤完成
        try {
            AdminMessage completeMsg = new AdminMessage();
            completeMsg.setTitle("同步考勤完成");
            completeMsg.setContent("点击查看");
            completeMsg.setLabel(8); // 人资
            completeMsg.setType(202); // HRM_ATTENDANCE_SYNC_COMPLETE
            completeMsg.setLinkUrl("/hrm/attendance/scheduling");
            completeMsg.setCreateUser(0L); // 系统
            completeMsg.setRecipientUser(0L); // 系统级通知
            completeMsg.setCreateTime(LocalDateTime.now());
            completeMsg.setIsRead(0);
            adminMessageService.save(completeMsg);
            logger.info("[同步进度] 公司{} 已写入完成通知", companyId);
        } catch (Exception e) {
            logger.error("[同步进度] 公司{} 写入完成通知失败", companyId, e);
        }
    }

    private void saveFailedSyncProgress(int step, String params, Throwable throwable) {
        int currentStep = Math.max(1, Math.min(7, step));
        String message = toFriendlySyncErrorMessage(throwable);
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = new ProgressTracker.ProgressState();
        state.progress = calculateProgressPercent(currentStep, true, parseTotalCountFromParams(params), 0, 0, 0);
        state.status = ProgressTracker.STATUS_FAILED;
        state.message = message;
        state.done = true;
        state.success = false;
        state.errors = Collections.singletonList(message);
        state.queuedAt = parseRedisLong(redis.get(syncKey(SYNC_QUEUED_KEY)));
        progressTracker.saveProgress(SYNC_KEY_PREFIX, companyId, state);
        // 保留断点续传所需的字段
        redis.setex(syncKey(SYNC_PARAMS_KEY), PROGRESS_EXPIRE_SECONDS, params);
        // 全量同步失败：清除「全量待标记」，使用户当月仍可重试
        try {
            if (redis.exists(syncKey(SYNC_FULLSYNC_PENDING_KEY))) {
                redis.del(syncKey(SYNC_FULLSYNC_PENDING_KEY));
                logger.info("[同步进度] 公司{} 全量同步失败，已清除本月全量待标记，允许当月重试", companyId);
            }
        } catch (Exception e) {
            logger.warn("[同步进度] 公司{} 清除全量待标记失败", companyId, e);
        }
        // 更新运行中通知的内容为稳定的失败标记，并清理Redis标记（对齐审批"获取失败"语义，
        // 供通知中心识别"该条是失败任务"并展示「继续」按钮；同时修复失败后误判仍在轮询的旧问题）
        try {
            String runningMsgIdStr = redis.get("attendance:notification:sync:" + companyId);
            if (runningMsgIdStr != null && !runningMsgIdStr.isEmpty()) {
                adminMessageService.updateContent(Long.parseLong(runningMsgIdStr), "同步失败");
                redis.del("attendance:notification:sync:" + companyId);
            }
        } catch (Exception e) {
            logger.warn("[同步进度] 更新运行中通知(失败)失败", e);
        }
        logger.error("[同步进度] 公司{} 步骤{}/7 同步失败: {}", companyId, currentStep, message);
    }

    public static String toFriendlySyncErrorMessage(Throwable throwable) {
        String message = collectThrowableMessage(throwable);
        String lower = message.toLowerCase(Locale.ROOT);
        if (message.contains("请选择") || message.contains("不能为空")) {
            return "请选择需要同步的员工和同步月份后重试";
        }
        if (message.contains("CompanyContext") || message.contains("租户上下文") || message.contains("登录")) {
            return "当前登录信息已失效，请重新登录后再同步考勤";
        }
        if (lower.contains("permission") || message.contains("权限") || lower.contains("forbidden")) {
            return "当前账号或钉钉应用没有同步考勤权限，请联系管理员处理后重试";
        }
        if (message.contains("钉钉") || lower.contains("dingtalk") || lower.contains("oapi")
                || lower.contains("api") || message.contains("限流") || message.contains("频控")) {
            return "钉钉考勤接口繁忙或权限不足，请稍后重试或联系管理员检查钉钉配置";
        }
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("database")
                || lower.contains("unknown column") || lower.contains("communications link")
                || lower.contains("connection") || message.contains("数据库")) {
            return "同步考勤失败，数据库连接或表结构需要管理员检查，当前进度已保留，可处理后重试";
        }
        if (message.contains("员工") || message.contains("步骤")) {
            return "部分员工考勤同步失败，请检查员工钉钉信息或缩小同步范围后重试，当前进度已保留";
        }
        return "同步考勤失败，请处理提示中的数据问题后重试";
    }

    private static String collectThrowableMessage(Throwable throwable) {
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

    /**
     * 同步考勤数据（主入口）
     * 自动根据Redis中的进度记录判断是否从断点恢复
     * 注意：不使用@Transactional，让每个步骤独立提交，支持断点续传
     */
    @Override
    public boolean SyncData(String EmpIDS, Date Begin, Date End) throws Exception {
        // 防重入互斥已上移到 AttendanceSyncTaskLauncher（按公司粒度，提交时同步抢占、后台任务 finally 释放）。
        // 本方法只负责断点续传判定与执行，由后台线程调用。
        String normalizedEmpIds = normalizeEmpIdCsv(EmpIDS);
        if (normalizedEmpIds.isEmpty()) {
            throw new IllegalArgumentException("EmpID不能为空");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentParams = normalizedEmpIds + "|" + sdf.format(Begin) + "|" + sdf.format(End);

        // 根据Redis进度记录自动判断是否从断点恢复
        Map<String, Object> progress = getSyncProgress();
        boolean hasValidProgress = (Boolean) progress.get("hasProgress")
                && STATUS_FAILED.equals(progress.get("status"))
                && currentParams.equals(progress.get("params"));

        if (hasValidProgress) {
            logger.info("检测到有效的断点进度，将从步骤{}恢复", progress.get("currentStep"));
        } else {
            logger.info("无有效断点进度，将从头开始同步");
        }

        return SyncDataWithResume(normalizedEmpIds, Begin, End, hasValidProgress);
    }

    /**
     * 支持断点续传的同步方法
     * 注意：不使用@Transactional，让每个步骤独立提交，避免异常时回滚已处理的数据
     */
    public boolean SyncDataWithResume(String EmpIDS, Date Begin, Date End, boolean resumeFromBreakpoint) throws Exception {
        String normalizedEmpIds = normalizeEmpIdCsv(EmpIDS);
        if (normalizedEmpIds.isEmpty()) {
            throw new IllegalArgumentException("EmpID不能为空");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentParams = normalizedEmpIds + "|" + sdf.format(Begin) + "|" + sdf.format(End);

        List<String> allEmpIds = normalizeEmpIdList(normalizedEmpIds);
        List<String> remainingEmpIds = new ArrayList<>(allEmpIds);
        int startStep = 1;
        Set<String> processedEmpSet = new HashSet<>();

        List<Long> allEmpIDD = allEmpIds.stream().map(Long::parseLong).collect(Collectors.toList());

        // 检查是否需要从断点恢复
        if (resumeFromBreakpoint) {
            Map<String, Object> progress = getSyncProgress();
            if ((Boolean) progress.get("hasProgress")) {
                String savedParams = (String) progress.get("params");
                // 验证参数是否匹配
                if (currentParams.equals(savedParams)) {
                    startStep = (Integer) progress.get("currentStep");
                    String processedEmps = (String) progress.get("processedEmps");
                    if (processedEmps != null && !processedEmps.isEmpty()) {
                        processedEmpSet = new HashSet<>(Arrays.asList(processedEmps.split(",")));
                        remainingEmpIds.removeAll(processedEmpSet);
                    }
                    logger.info("========== 从断点恢复同步，起始步骤: {}, 剩余员工数: {} ==========", startStep, remainingEmpIds.size());
                } else {
                    logger.warn("参数不匹配，将从头开始同步。缓存参数: {}, 当前参数: {}", savedParams, currentParams);
                    clearSyncProgress();
                }
            } else {
                logger.info("无可恢复的进度，将从头开始同步");
            }
        } else {
            // 非恢复模式，清除旧进度
            clearSyncProgress();
        }

        // 只有当员工级步骤（3-4）已完成且所有步骤都已完成时才跳过
        if (remainingEmpIds.isEmpty() && startStep > 7) {
            logger.info("所有步骤已完成，无需同步");
            clearSyncProgress();
            return true;
        }

        // 如果员工级步骤已完成但还有后续步骤（5-7），继续执行
        if (remainingEmpIds.isEmpty() && startStep <= 7) {
            logger.info("员工级步骤（3-4）已完成，继续执行步骤{}-7", startStep);
        }

        // 待处理员工，用于步骤3-4的断点续传
        logger.info("========== 开始同步考勤数据 (步骤{}/7起) ==========", startStep);
        logger.info("总员工数: {}，待处理员工数: {}", allEmpIds.size(), remainingEmpIds.size());

        // 写入通知：同步考勤开始
        try {
            AdminMessage startMsg = new AdminMessage();
            startMsg.setTitle("同步考勤数据中");
            startMsg.setContent("准备中... 0%");
            startMsg.setLabel(8);
            startMsg.setType(201); // HRM_ATTENDANCE_SYNC_RUNNING
            startMsg.setLinkUrl("/hrm/attendance/scheduling");
            startMsg.setCreateUser(0L);
            startMsg.setRecipientUser(0L);
            startMsg.setCreateTime(LocalDateTime.now());
            startMsg.setIsRead(0);
            adminMessageService.save(startMsg);
            // 保存通知ID到Redis，用于进度更新时同步通知内容
            String companyId = getCompanyId();
            redis.setex("attendance:notification:sync:" + companyId, 7200, String.valueOf(startMsg.getMessageId()));
            logger.info("[同步进度] 已写入开始通知, messageId={}", startMsg.getMessageId());
        } catch (Exception e) {
            logger.error("[同步进度] 写入开始通知失败", e);
        }

        int activeStep = startStep;
        try {
            // 步骤1: 同步组织架构
            if (startStep <= 1) {
                activeStep = 1;
                logger.info("[步骤1/7] 同步钉钉组织架构...");
                groupManager.GetAndSave();
                saveSyncProgress(2, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤2: 同步用户信息
            if (startStep <= 2) {
                activeStep = 2;
                logger.info("[步骤2/7] 同步钉钉用户信息...");
                userManager.GetAndSave();
                saveSyncProgress(3, String.join(",", processedEmpSet), currentParams);
            }

            List<tbattendanceuser> users = userRep.findAll();
            List<tbattendanceuser> normalizedUsers = normalizeUsers(users);
            logger.info("同步用户映射加载完成: 原始{}条，去重后{}条", users.size(), normalizedUsers.size());
            // 创建线程安全的users副本，避免并行处理时的状态共享问题
            final List<tbattendanceuser> threadSafeUsers = Collections.unmodifiableList(new ArrayList<>(normalizedUsers));

            // 步骤3: 同步考勤计划（并行处理，支持断点）
            if (startStep <= 3) {
                activeStep = 3;
                logger.info("[步骤3/7] 同步考勤计划（并行模式，线程数: {}）", PARALLEL_THREADS);
                // 每个员工的删除和保存在同一个短事务中完成，避免锁冲突
                syncByEmployeeBatchParallelSafe(remainingEmpIds, processedEmpSet, Begin, End, currentParams, 3,
                        threadSafeUsers, planService);
                saveSyncProgress(4, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤4: 同步考勤明细（并行处理，支持断点）
            if (startStep <= 4) {
                activeStep = 4;
                logger.info("[步骤4/7] 同步考勤明细（并行模式，线程数: {}）", PARALLEL_THREADS);
                // 重置已处理集合用于此步骤
                Set<String> step4Processed = ConcurrentHashMap.newKeySet();
                syncByEmployeeBatchParallelSafe(remainingEmpIds, step4Processed, Begin, End, currentParams, 4,
                        threadSafeUsers, detailService);
                saveSyncProgress(5, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤5: 更新考勤报表字段
            if (startStep <= 5) {
                activeStep = 5;
                logger.info("[步骤5/7] 更新考勤报表字段");
                report.UpdateReportFields();
                saveSyncProgress(6, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤6: 删除历史数据（只删除当前批次要处理的员工，保留已处理员工的数据）
            if (startStep <= 6) {
                activeStep = 6;
                logger.info("[步骤6/7] 删除历史数据，处理所有员工: {} 人", allEmpIDD.size());
                // 使用事务模板执行删除操作
                TransactionTemplate transactionTemplate = ApplicationContextHolder.getBean(TransactionTemplate.class);
                Integer Num = transactionTemplate.execute(status -> {
                    return dataRep.deleteAllByEmpIdInAndWorkDateBetween(allEmpIDD, Begin, End);
                });
                logger.info("[步骤6/7] 已删除 {} 条历史记录", Num);
                saveSyncProgress(7, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤7: 同步请假和假期数据（并行处理，支持断点续传）
            if (startStep <= 7) {
                activeStep = 7;
                logger.info("[步骤7/7] 同步请假和假期数据（并行模式，处理所有员工: {}，线程数: {}）", allEmpIds.size(), STEP7_PARALLEL_THREADS);

                // 步骤7a: 请假数据同步（支持断点续传）
                Set<String> step7aProcessed = ConcurrentHashMap.newKeySet();
                if (resumeFromBreakpoint) {
                    String saved7a = redis.get(syncKey(SYNC_STEP7A_PROCESSED_KEY));
                    if (saved7a != null && !saved7a.isEmpty()) {
                        step7aProcessed.addAll(Arrays.asList(saved7a.split(",")));
                        logger.info("[步骤7a/7] 从断点恢复，已处理: {} 人", step7aProcessed.size());
                    }
                }
                logger.info("[步顂7a/7] 开始同步请假数据");
                try {
                    syncByEmployeeBatchParallelSafe(allEmpIds, step7aProcessed, Begin, End,
                            currentParams, 7, threadSafeUsers, leaveService);
                    logger.info("[步顂7a/7] 请假数据同步完成");
                    // 7a完成后更新进度，防止后续步骤失败导致进度卡在RUNNING
                    saveSyncProgress(7, String.join(",", step7aProcessed), currentParams);
                } catch (Exception e) {
                    logger.error("[步顂7a/7] 请假数据同步失败，已保存进度，可从断点恢复", e);
                    throw e; // 重新抛出异常，不执行后续步骤
                }

                // 步骤7b: 假期数据同步（支持断点续传）
                Set<String> step7bProcessed = ConcurrentHashMap.newKeySet();
                if (resumeFromBreakpoint) {
                    String saved7b = redis.get(syncKey(SYNC_STEP7B_PROCESSED_KEY));
                    if (saved7b != null && !saved7b.isEmpty()) {
                        step7bProcessed.addAll(Arrays.asList(saved7b.split(",")));
                        logger.info("[步骤7b/7] 从断点恢复，已处理: {} 人", step7bProcessed.size());
                    }
                }
                logger.info("[步顂7b/7] 开始同步假期数据");
                try {
                    syncByEmployeeBatchParallelSafe(allEmpIds, step7bProcessed, Begin, End,
                            currentParams, 7, threadSafeUsers, holidayService);
                    logger.info("[步顂7b/7] 假期数据同步完成");
                    // 7b完成后更新进度，确保状态反映最新进展
                    Set<String> allStep7Processed = new HashSet<>(step7aProcessed);
                    allStep7Processed.addAll(step7bProcessed);
                    saveSyncProgress(7, String.join(",", allStep7Processed), currentParams);
                } catch (Exception e) {
                    logger.error("[步顂7b/7] 假期数据同步失败，已保存进度，可从断点恢复", e);
                    throw e; // 重新抛出异常
                }
            }

            // 同步成功后保留最终进度，等待操作人员在前端确认后关闭进度条。
            saveFinalSyncProgress(currentParams);
            logger.info("========== 考勤数据同步完成 ==========");

            // 同步完成后触发本地考勤判定重算（2026-09 弃用钉钉推送后的本地口径；失败不影响同步结果）
            try {
                ApplicationContextHolder.getBean(com.tianye.hrsystem.service.IHrmAttendanceJudgeService.class)
                        .recompute(Begin, End, null);
            } catch (Exception judgeEx) {
                logger.warn("[本地考勤判定] 同步后重算失败，可用考勤汇总的手动重算接口补算", judgeEx);
            }
            return true;

        } catch (Exception e) {
            saveFailedSyncProgress(activeStep, currentParams, e);
            logger.error("同步过程中发生异常，进度已保存，可调用 SyncDataWithResume(..., true) 从断点恢复", e);
            throw e;
        }
    }

    /**
     * 按员工批量同步（串行版本，保留作为备用）
     */
    private void syncByEmployeeBatch(List<String> empIds, Set<String> processedSet,
                                      Date begin, Date end, String params, int currentStep,
                                      EmployeeSyncAction action) throws Exception {
        int total = empIds.size();
        int processed = 0;

        for (String empId : empIds) {
            if (processedSet.contains(empId)) {
                processed++;
                continue;
            }

            try {
                action.sync(empId);
                processedSet.add(empId);
                processed++;

                // 每处理10个员工保存一次进度
                if (processed % 10 == 0) {
                    saveSyncProgress(currentStep, String.join(",", processedSet), params);
                    logger.info("[步骤{}/7] 进度: {}/{}", currentStep, processed, total);
                }
            } catch (Exception e) {
                // 发生异常时保存当前进度
                saveSyncProgress(currentStep, String.join(",", processedSet), params);
                logger.error("处理员工 {} 时发生异常，已保存进度，可从断点恢复", empId);
                throw e;
            }
        }
    }

    /**
     * 按员工并行同步（线程安全版本）
     * users 通过方法参数传递，服务单例不持有共享可变状态
     */
    private void syncByEmployeeBatchParallelSafe(List<String> empIds, Set<String> processedSet,
                                                  Date begin, Date end, String params, int currentStep,
                                                  List<tbattendanceuser> users, Object service) throws Exception {
        if (service instanceof IAttendancePlanService) {
            syncPlanByDateForAllEmployees(empIds, processedSet, begin, end, params, currentStep, users,
                    (IAttendancePlanService) service);
            return;
        }
        if (service instanceof IAttendanceDetailService) {
            syncDetailByBatch(empIds, processedSet, begin, end, params, currentStep, users,
                    (IAttendanceDetailService) service);
            return;
        }
        // 过滤掉已处理的员工
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());

        if (toProcess.isEmpty()) {
            logger.info("[步骤{}/7] 所有员工已处理完成", currentStep);
            return;
        }

        int total = toProcess.size();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedEmpIds = Collections.synchronizedList(new ArrayList<>());

        // 使用线程安全的Set
        Set<String> threadSafeProcessedSet = ConcurrentHashMap.newKeySet();
        threadSafeProcessedSet.addAll(processedSet);

        // 先校验租户上下文再创建线程池：避免校验失败抛出时，线程池（非 daemon 线程）泄漏
        final LoginUserInfo mainThreadContext = CompanyContext.get();
        if (mainThreadContext == null) {
            logger.error("主线程 CompanyContext 为 null，无法进行并行处理");
            throw new Exception("缺少租户上下文（CompanyContext），请确保通过正常 API 调用");
        }

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        // 分批提交任务
        List<List<String>> batches = partitionList(toProcess, BATCH_SIZE);

        logger.info("[步骤{}/7] 开始并行处理，总员工数: {}，分 {} 批，每批 {} 人",
                currentStep, total, batches.size(), BATCH_SIZE);

        long startTime = System.currentTimeMillis();
        logger.info("[步骤{}/7] 主线程上下文: companyId={}", currentStep, mainThreadContext.getCompanyId());

        for (List<String> batch : batches) {
            Future<?> future = executor.submit(() -> {
                // 在子线程中恢复 CompanyContext
                CompanyContext.set(mainThreadContext);
                try {
                    for (String empId : batch) {
                        try {
                            // users 以参数逐层传入，单例服务不再持有共享可变状态
                            if (service instanceof IAttendancePlanService) {
                                IAttendancePlanService planSvc = (IAttendancePlanService) service;
                                runWithDbRetry(currentStep, empId, () -> planSvc.Sync(empId, begin, end, users));
                            } else if (service instanceof IAttendanceDetailService) {
                                IAttendanceDetailService detailSvc = (IAttendanceDetailService) service;
                                runWithDbRetry(currentStep, empId, () -> detailSvc.Sync(empId, begin, end, users));
                            } else if (service instanceof ILeaveRecordDtaService) {
                                ILeaveRecordDtaService leaveSvc = (ILeaveRecordDtaService) service;
                                runWithDbRetry(currentStep, empId, () -> leaveSvc.Sync(empId, begin, end, users));
                            } else if (service instanceof IHolidayDataService) {
                                IHolidayDataService holidaySvc = (IHolidayDataService) service;
                                runWithDbRetry(currentStep, empId, () -> holidaySvc.Sync(empId, begin, end, users));
                            }

                            threadSafeProcessedSet.add(empId);
                            int count = processed.incrementAndGet();

                            // 每处理20个员工保存一次进度并打印日志
                            if (count % 20 == 0) {
                                synchronized (this) {
                                    saveSyncProgress(currentStep, String.join(",", threadSafeProcessedSet), params);
                                }
                                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                                double speed = count / (double) Math.max(elapsed, 1);
                                int remaining = total - count;
                                long eta = (long) (remaining / Math.max(speed, 0.1));
                                logger.info("[步骤{}/7] 进度: {}/{} ({}员工/秒)，预计剩余: {}秒",
                                        currentStep, count, total, String.format("%.1f", speed), eta);
                            }
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            failedEmpIds.add(empId);
                            logger.error("处理员工 {} 失败，异常类型: {}，错误信息: {}", empId, e.getClass().getName(), e.getMessage());
                            logger.error("员工 {} 失败详细堆栈:", empId, e);
                        }
                    }
                } finally {
                    // 清理子线程的 CompanyContext，避免内存泄漏
                    CompanyContext.clear();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                logger.error("批次处理超时");
            } catch (Exception e) {
                logger.error("批次处理异常: {}", e.getMessage());
            }
        }

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 合并结果到原始集合
        processedSet.addAll(threadSafeProcessedSet);

        // 保存最终进度
        saveSyncProgress(currentStep, String.join(",", processedSet), params);

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("[步骤{}/7] 完成！成功: {}，失败: {}，总耗时: {}秒",
                currentStep, processed.get(), failed.get(), totalTime);

        // 如果有失败，抛出异常以便断点续传
        if (failed.get() > 0) {
            logger.error("步骤{}有{}个员工处理失败，失败员工ID: {}，已保存进度，可重试",
                    currentStep, failed.get(), String.join(",", failedEmpIds));
            throw new Exception(String.format("步骤%d有%d个员工处理失败，已保存进度，可重试", currentStep, failed.get()));
        }
    }

    private void syncPlanByDateForAllEmployees(List<String> empIds, Set<String> processedSet,
                                               Date begin, Date end, String params, int currentStep,
                                               List<tbattendanceuser> users,
                                               IAttendancePlanService planSvc) throws Exception {
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());
        if (toProcess.isEmpty()) {
            logger.info("[步骤{}/7] 所有员工已处理完成", currentStep);
            return;
        }

        String batchEmpIds = String.join(",", toProcess);
        logger.info("[步骤{}/7] 使用日期级批量模式同步考勤计划，员工数: {}", currentStep, toProcess.size());
        runWithDbRetry(currentStep, batchEmpIds, () -> planSvc.Sync(batchEmpIds, begin, end, users));
        processedSet.addAll(toProcess);
        saveSyncProgress(currentStep, String.join(",", processedSet), params);
        logger.info("[步骤{}/7] 考勤计划批量同步完成，处理员工数: {}", currentStep, toProcess.size());
    }

    private void syncDetailByBatch(List<String> empIds, Set<String> processedSet,
                                   Date begin, Date end, String params, int currentStep,
                                   List<tbattendanceuser> users,
                                   IAttendanceDetailService detailSvc) throws Exception {
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());
        if (toProcess.isEmpty()) {
            logger.info("[步骤{}/7] 所有员工已处理完成", currentStep);
            return;
        }

        List<List<String>> batches = partitionList(toProcess, DETAIL_BATCH_SIZE);
        int processedCount = 0;
        for (List<String> batch : batches) {
            String batchEmpIds = String.join(",", batch);
            runWithDbRetry(currentStep, batchEmpIds, () -> detailSvc.Sync(batchEmpIds, begin, end, users));
            processedSet.addAll(batch);
            processedCount += batch.size();
            saveSyncProgress(currentStep, String.join(",", processedSet), params);
            logger.info("[步骤{}/7] 考勤明细批量同步进度: {}/{}", currentStep, processedCount, toProcess.size());
        }
    }

    /**
     * 按员工并行同步（多线程版本，大幅提升性能）
     * 使用线程池并行处理多个员工，同时保持断点续传能力
     */
    private void syncByEmployeeBatchParallel(List<String> empIds, Set<String> processedSet,
                                              Date begin, Date end, String params, int currentStep,
                                              EmployeeSyncAction action) throws Exception {
        // 过滤掉已处理的员工
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());

        if (toProcess.isEmpty()) {
            logger.info("[步骤{}/7] 所有员工已处理完成", currentStep);
            return;
        }

        int total = toProcess.size();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedEmpIds = Collections.synchronizedList(new ArrayList<>());

        // 使用线程安全的Set
        Set<String> threadSafeProcessedSet = ConcurrentHashMap.newKeySet();
        threadSafeProcessedSet.addAll(processedSet);

        // 先校验租户上下文再创建线程池：避免校验失败抛出时，线程池（非 daemon 线程）泄漏
        final LoginUserInfo mainThreadContext = CompanyContext.get();
        if (mainThreadContext == null) {
            logger.error("主线程 CompanyContext 为 null，无法进行并行处理");
            throw new Exception("缺少租户上下文（CompanyContext），请确保通过正常 API 调用");
        }

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        // 分批提交任务
        List<List<String>> batches = partitionList(toProcess, BATCH_SIZE);

        logger.info("[步骤{}/7] 开始并行处理，总员工数: {}，分 {} 批，每批 {} 人",
                currentStep, total, batches.size(), BATCH_SIZE);

        long startTime = System.currentTimeMillis();

        for (List<String> batch : batches) {
            Future<?> future = executor.submit(() -> {
                // 在子线程中恢复 CompanyContext
                CompanyContext.set(mainThreadContext);
                try {
                    for (String empId : batch) {
                        try {
                            action.sync(empId);
                            threadSafeProcessedSet.add(empId);
                            int count = processed.incrementAndGet();

                            // 每处理20个员工保存一次进度并打印日志
                            if (count % 20 == 0) {
                                synchronized (this) {
                                    saveSyncProgress(currentStep, String.join(",", threadSafeProcessedSet), params);
                                }
                                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                                double speed = count / (double) Math.max(elapsed, 1);
                                int remaining = total - count;
                                long eta = (long) (remaining / Math.max(speed, 0.1));
                                logger.info("[步骤{}/7] 进度: {}/{} ({}员工/秒)，预计剩余: {}秒",
                                        currentStep, count, total, String.format("%.1f", speed), eta);
                            }
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            failedEmpIds.add(empId);
                            logger.error("处理员工 {} 失败: {}", empId, e.getMessage());
                            // 记录失败但继续处理其他员工
                        }
                    }
                } finally {
                    // 清理子线程的 CompanyContext，避免内存泄漏
                    CompanyContext.clear();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES); // 每批最多等待30分钟
            } catch (TimeoutException e) {
                logger.error("批次处理超时");
            } catch (Exception e) {
                logger.error("批次处理异常: {}", e.getMessage());
            }
        }

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 合并结果到原始集合
        processedSet.addAll(threadSafeProcessedSet);

        // 保存最终进度
        saveSyncProgress(currentStep, String.join(",", processedSet), params);

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("[步骤{}/7] 完成！成功: {}，失败: {}，总耗时: {}秒",
                currentStep, processed.get(), failed.get(), totalTime);

        // 如果有失败，抛出异常以便断点续传
        if (failed.get() > 0) {
            logger.error("步骤{}有{}个员工处理失败，失败员工ID: {}，已保存进度，可重试",
                    currentStep, failed.get(), String.join(",", failedEmpIds));
            throw new Exception(String.format("步骤%d有%d个员工处理失败，已保存进度，可重试", currentStep, failed.get()));
        }
    }

    /**
     * 将列表分割成指定大小的批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    @FunctionalInterface
    private interface SyncAction {
        void run() throws Exception;
    }

    private void runWithDbRetry(int step, String targetId, SyncAction action) throws Exception {
        int attempt = 1;
        while (true) {
            try {
                action.run();
                return;
            } catch (Exception ex) {
                if (!isRetryableDbConnectionError(ex) || attempt >= DB_RETRY_MAX_ATTEMPTS) {
                    throw ex;
                }
                logger.warn("[步骤{}/7] 处理目标 {} 出现数据库连接异常，重试 {}/{}：{}",
                        step, targetId, attempt, DB_RETRY_MAX_ATTEMPTS, ex.getMessage());
                sleepQuietly(1000L * attempt);
                attempt++;
            }
        }
    }

    private boolean isRetryableDbConnectionError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (className.contains("CommunicationsException")
                    || className.contains("JDBCConnectionException")
                    || current instanceof java.sql.SQLTransientConnectionException
                    || (message != null && message.toLowerCase().contains("communications link failure"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizeEmpIdCsv(String empIdsCsv) {
        return String.join(",", normalizeEmpIdList(empIdsCsv));
    }

    private List<String> normalizeEmpIdList(String empIdsCsv) {
        if (empIdsCsv == null || empIdsCsv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(empIdsCsv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 步骤7批次并行同步（支持断点续传）
     * 将员工分成多个批次，每个批次在独立线程中调用服务的Sync方法
     * @param processedSet 已处理的员工ID集合，用于断点续传
     * @param progressKey Redis进度Key，用于保存此子步骤的进度
     */
    private void syncStep7BatchParallelWithResume(List<String> empIds, Set<String> processedSet,
                                                    Date begin, Date end, String params,
                                                    List<tbattendanceuser> users, Object service,
                                                    String serviceName, String progressKey) throws Exception {
        // 过滤已处理的员工
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());

        if (toProcess.isEmpty()) {
            logger.info("[步骤7/7] {}数据所有员工已处理完成", serviceName);
            return;
        }

        int total = toProcess.size();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        List<String> failedEmpIds = Collections.synchronizedList(new ArrayList<>());

        // 线程安全的已处理集合
        Set<String> threadSafeProcessedSet = ConcurrentHashMap.newKeySet();
        threadSafeProcessedSet.addAll(processedSet);

        final LoginUserInfo mainThreadContext = CompanyContext.get();
        if (mainThreadContext == null) {
            logger.error("主线程 CompanyContext 为 null，无法进行并行处理");
            throw new Exception("缺少租户上下文（CompanyContext）");
        }

        ExecutorService executor = Executors.newFixedThreadPool(STEP7_PARALLEL_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        List<List<String>> batches = partitionList(toProcess, STEP7_BATCH_SIZE);

        logger.info("[步骤7/7] {}数据开始批次并行处理，待处理: {}人，分 {} 批，每批 {} 人，线程数: {}",
                serviceName, total, batches.size(), STEP7_BATCH_SIZE, STEP7_PARALLEL_THREADS);

        long startTime = System.currentTimeMillis();

        for (List<String> batch : batches) {
            final String batchEmpIds = String.join(",", batch);

            Future<?> future = executor.submit(() -> {
                CompanyContext.set(mainThreadContext);
                try {
                    // 调用服务的Sync方法
                    if (service instanceof ILeaveRecordDtaService) {
                        ILeaveRecordDtaService svc = (ILeaveRecordDtaService) service;
                        runWithDbRetry(7, batchEmpIds, () -> svc.Sync(batchEmpIds, begin, end, users));
                    } else if (service instanceof IHolidayDataService) {
                        IHolidayDataService svc = (IHolidayDataService) service;
                        runWithDbRetry(7, batchEmpIds, () -> svc.Sync(batchEmpIds, begin, end, users));
                    }

                    // 批次成功，记录已处理的员工
                    threadSafeProcessedSet.addAll(batch);
                    int count = processedCount.addAndGet(batch.size());

                    // 每处理20人或每5批保存一次进度并打印日志
                    if (count % 20 == 0 || count == total) {
                        synchronized (this) {
                            redis.setex(progressKey, PROGRESS_EXPIRE_SECONDS, String.join(",", threadSafeProcessedSet));
                        }
                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                        double speed = count / (double) Math.max(elapsed, 1);
                        int remaining = total - count;
                        long eta = (long) (remaining / Math.max(speed, 0.1));
                        logger.info("[步骤7/7] {}进度: {}/{} ({}%)，{}员工/秒，预计剩余: {}秒",
                                serviceName, count, total, String.format("%.1f", count * 100.0 / total),
                                String.format("%.1f", speed), eta);
                    }
                } catch (Exception e) {
                    failedCount.addAndGet(batch.size());
                    failedEmpIds.addAll(batch);
                    logger.error("[步骤7/7] {}批次处理失败，员工数: {}，异常: {}",
                            serviceName, batch.size(), e.getMessage(), e);
                } finally {
                    CompanyContext.clear();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                logger.error("[步骤7/7] {}批次处理超时", serviceName);
            } catch (Exception e) {
                logger.error("[步骤7/7] {}批次处理异常: {}", serviceName, e.getMessage());
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 合并结果到原始集合
        processedSet.addAll(threadSafeProcessedSet);

        // 保存最终进度
        redis.setex(progressKey, PROGRESS_EXPIRE_SECONDS, String.join(",", processedSet));

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("[步骤7/7] {}完成！成功: {}，失败: {}，总耗时: {}秒",
                serviceName, processedCount.get(), failedCount.get(), totalTime);

        // 如果有失败，抛出异常以便断点续传
        if (failedCount.get() > 0) {
            logger.error("步骤7{}有{}个员工处理失败，失败员工ID: {}，已保存进度，可重试",
                    serviceName, failedCount.get(), String.join(",", failedEmpIds));
            throw new Exception(String.format("步骤7%s有%d个员工处理失败，已保存进度，可重试", serviceName, failedCount.get()));
        }
    }

    /**
     * 员工同步动作接口
     */
    @FunctionalInterface
    private interface EmployeeSyncAction {
        void sync(String empId) throws Exception;
    }
}
