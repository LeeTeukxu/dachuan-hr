package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO;
import com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalDurationBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalStatisticsStatusBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalSubtypeBO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO;
import com.tianye.hrsystem.mapper.HrmAttendanceApprovalMapper;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository;
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
    private static final Map<String, ApprovalFetchProgressState> FETCH_PROGRESS_MAP = new ConcurrentHashMap<>();
    // 按公司运行标记：同一公司同时只允许一个“获取审批数据”任务；超过 2 小时视为异常残留自动放行
    private static final Map<String, java.util.concurrent.atomic.AtomicReference<Long>> FETCH_RUNNING_MAP = new ConcurrentHashMap<>();
    private static final long STALE_FETCH_RUNNING_MILLIS = 2L * HOUR_MILLIS;
    // 审批获取失败自动重试：次数与退避基数可配（默认 2 次自动重试、60s 起步指数退避、封顶 5 分钟）
    @Value("${hrm.approval-fetch.auto-retry.attempts:2}")
    private int fetchAutoRetryAttempts = 2;
    @Value("${hrm.approval-fetch.auto-retry.backoff-ms:60000}")
    private long fetchAutoRetryBackoffMs = 60000L;
    // 进度条状态保留期：终态 2 小时后清理，运行态 24 小时（视为异常残留）后清理
    private static final long FETCH_PROGRESS_TTL_MILLIS = 2L * HOUR_MILLIS;
    private static final long FETCH_RUNNING_PROGRESS_TTL_MILLIS = 24L * HOUR_MILLIS;
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

    @Override
    public BasePage<QueryAttendanceApprovalPageVO> queryPageList(QueryAttendanceApprovalPageBO queryBO) {
        return attendanceApprovalMapper.queryPageList(queryBO.parse(), queryBO);
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
        clearExpiredFetchProgress();
        String progressKey = buildFetchProgressKey();
        updateFetchProgress(progressKey, 5, FETCH_STATUS_RUNNING, "正在准备获取审批数据", false, false, 0L, null);
        try {
            updateFetchProgress(progressKey, 35, FETCH_STATUS_RUNNING, "正在从钉钉获取审批数据", false, false, 0L, null);
            long insertedCount = approvalSyncService.fetchMonthData(month, employeeIds, approvalTypes);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("month", month.format(MONTH_FORMATTER));
            result.put("insertedCount", insertedCount);
            updateFetchProgress(progressKey, 100, FETCH_STATUS_SUCCESS,
                    "审批数据获取完成，请确认后关闭进度条", true, true, insertedCount, null);
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
        clearExpiredFetchProgress();
        ApprovalFetchProgressState state = FETCH_PROGRESS_MAP.get(buildFetchProgressKey());
        Map<String, Object> result = new LinkedHashMap<>();
        if (state == null) {
            result.put("progress", 0);
            result.put("status", FETCH_STATUS_IDLE);
            result.put("message", "等待获取审批数据");
            result.put("done", Boolean.FALSE);
            result.put("success", Boolean.FALSE);
            result.put("error", Boolean.FALSE);
            result.put("insertedCount", 0L);
            result.put("errors", Collections.emptyList());
            return result;
        }
        result.put("progress", state.progress);
        result.put("status", state.status);
        result.put("message", state.message);
        result.put("done", state.done);
        result.put("success", state.success);
        result.put("error", FETCH_STATUS_FAILED.equals(state.status));
        result.put("insertedCount", state.insertedCount);
        result.put("errors", state.errors == null ? Collections.emptyList() : state.errors);
        return result;
    }

    private String buildFetchProgressKey() {
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
        ApprovalFetchProgressState state = FETCH_PROGRESS_MAP.computeIfAbsent(key, ignored -> new ApprovalFetchProgressState());
        if (progress != null) {
            state.progress = Math.max(0, Math.min(100, progress));
        }
        state.lastUpdatedMillis = System.currentTimeMillis();
        state.status = status;
        state.message = message;
        state.done = done;
        state.success = success;
        state.insertedCount = insertedCount;
        state.errors = errors == null ? Collections.emptyList() : new ArrayList<>(errors);
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
        java.util.concurrent.atomic.AtomicReference<Long> flag = FETCH_RUNNING_MAP.get(companyId);
        if (flag != null) {
            flag.set(null);
        }
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
                    throw ex; // fetchMonthData 内部已把进度标为 FAILED
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
        clearExpiredFetchProgress();
        updateFetchProgress(buildFetchProgressKey(), 2, FETCH_STATUS_RUNNING,
                "已提交获取审批数据请求，正在排队执行", false, false, 0L, null);
    }

    private void clearExpiredFetchProgress() {
        long now = System.currentTimeMillis();
        FETCH_PROGRESS_MAP.entrySet().removeIf(entry -> {
            ApprovalFetchProgressState state = entry.getValue();
            if (state == null) {
                return true;
            }
            boolean terminal = FETCH_STATUS_SUCCESS.equals(state.status) || FETCH_STATUS_FAILED.equals(state.status)
                    || FETCH_STATUS_IDLE.equals(state.status);
            long age = now - state.lastUpdatedMillis;
            return terminal ? age > FETCH_PROGRESS_TTL_MILLIS : age > FETCH_RUNNING_PROGRESS_TTL_MILLIS;
        });
    }

    private static class ApprovalFetchProgressState {
        private volatile long lastUpdatedMillis;
        private volatile int progress;
        private volatile String status = FETCH_STATUS_IDLE;
        private volatile String message = "等待获取审批数据";
        private volatile boolean done;
        private volatile boolean success;
        private long insertedCount;
        private List<String> errors = Collections.emptyList();
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
        approvalRepository.save(approval);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalId", approval.getId());
        result.put("duration", approval.getDuration());
        result.put("durationUnit", approval.getDurationUnit());
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
