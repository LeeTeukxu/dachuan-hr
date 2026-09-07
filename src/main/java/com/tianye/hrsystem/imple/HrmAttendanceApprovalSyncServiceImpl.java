package com.tianye.hrsystem.imple;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiProcessListbyuseridRequest;
import com.dingtalk.api.request.OapiProcessinstanceGetRequest;
import com.dingtalk.api.request.OapiProcessinstanceListidsRequest;
import com.dingtalk.api.request.OapiSmartworkHrmEmployeeQueryonjobRequest;
import com.dingtalk.api.request.OapiSmartworkHrmEmployeeV2ListRequest;
import com.dingtalk.api.response.OapiProcessListbyuseridResponse;
import com.dingtalk.api.response.OapiProcessinstanceGetResponse;
import com.dingtalk.api.response.OapiProcessinstanceListidsResponse;
import com.dingtalk.api.response.OapiAttendanceGetupdatedataResponse;
import com.dingtalk.api.response.OapiSmartworkHrmEmployeeQueryonjobResponse;
import com.dingtalk.api.response.OapiSmartworkHrmEmployeeV2ListResponse;
import com.tianye.hrsystem.common.BaseUtil;
import com.tianye.hrsystem.common.EmployeeNotInDingTalkException;
import com.tianye.hrsystem.common.ProgressTracker;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.HrmAttendanceApprovalFetchMark;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.taobao.api.ApiException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HrmAttendanceApprovalSyncServiceImpl implements IHrmAttendanceApprovalSyncService {

    private static final Logger logger = LoggerFactory.getLogger(HrmAttendanceApprovalSyncServiceImpl.class);
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final String API_URL = "https://oapi.dingtalk.com/topapi/attendance/getupdatedata";
    private static final int FETCH_VERSION = 1;
    private static final String TYPE_ALL = "all";
    private static final String TYPE_OVERTIME = "overtime";
    private static final String TYPE_MISSCARD = "misscard";
    private static final String TYPE_LEAVE = "leave";
    private static final String TYPE_TRAVEL = "travel";

    @Override
    public String ensureDingTalkUserId(HrmEmployee employee) throws Exception {
        if (employee == null) {
            throw new EmployeeNotInDingTalkException("员工信息为空，无法映射钉钉用户");
        }
        String existing = normalizeText(employee.getDingtalkUserId());
        if (!existing.isEmpty()) {
            return existing;
        }
        if (normalizeEmployeeName(employee.getEmployeeName()).isEmpty()
                || normalizePhoneNumber(employee.getMobile()).isEmpty()) {
            throw new EmployeeNotInDingTalkException("员工「"
                    + StringUtils.defaultString(employee.getEmployeeName())
                    + "」缺少姓名或手机号，无法在钉钉中匹配，请先补全员工资料");
        }
        // 已入库员工：加载完整实体，避免部分字段被 JPA save 覆盖为空
        HrmEmployee mappingTarget = employee;
        if (employee.getEmployeeId() != null) {
            HrmEmployee full = hrmEmployeeRepository.findById(employee.getEmployeeId()).orElse(null);
            if (full == null) {
                throw new EmployeeNotInDingTalkException("员工不存在，无法映射钉钉用户");
            }
            if (StringUtils.isNotBlank(employee.getEmployeeName())) {
                full.setEmployeeName(employee.getEmployeeName());
            }
            if (StringUtils.isNotBlank(employee.getMobile())) {
                full.setMobile(employee.getMobile());
            }
            mappingTarget = full;
        }
        String token;
        try {
            token = tokenCreator.Refresh();
            Optional<String> lookedUp = fetchDingTalkUserIdByNameAndMobile(token, mappingTarget.getEmployeeName(), mappingTarget.getMobile());
            if (lookedUp.isPresent()) {
                // 新员工（未入库）只解析返回，由保存流程随实体写入；已入库员工立即回写
                if (mappingTarget.getEmployeeId() != null) {
                    saveEmployeeDingTalkUserId(mappingTarget, lookedUp.get());
                }
                return lookedUp.get();
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            if (EmployeeNotInDingTalkException.isDingTalkPermissionError(String.valueOf(ex.getMessage()))) {
                // 权限缺失是配置问题，必须让用户看到并找管理员，不能静默放行
                logger.warn("钉钉通讯录权限缺失, employeeId={}, employeeName={}", employee.getEmployeeId(), employee.getEmployeeName(), ex);
                throw new EmployeeNotInDingTalkException(EmployeeNotInDingTalkException.DINGTALK_PERMISSION_GUIDANCE, ex);
            }
            logger.warn("钉钉服务调用失败，员工映射暂缓, employeeId={}, employeeName={}", employee.getEmployeeId(), employee.getEmployeeName(), ex);
            throw new Exception("钉钉服务暂不可用，员工「" + employee.getEmployeeName() + "」将以待映射状态保存，稍后自动重试", ex);
        }
        throw new EmployeeNotInDingTalkException("员工「" + employee.getEmployeeName() + "」未能在钉钉中匹配（姓名+手机号），请先在钉钉创建该员工并完善手机号");
    }

    private static final String GENERIC_FETCH_ERROR_MESSAGE = "获取审批数据失败，请稍后重试";
    private static final String WORKFLOW_PERMISSION_DENIED_MESSAGE = "当前钉钉应用未开通审批读取权限，请联系管理员开通后重试";
    private static final String PROCESS_CODE_MISSING_MESSAGE = "当前应用未匹配到所选审批类型对应的审批流程，请先确认钉钉审批模板名称和权限配置";
    private static final String DINGTALK_FREQUENCY_LIMIT_MESSAGE = "钉钉审批接口触发限流，请稍后重试或缩小员工范围";
    private static final List<String> LEAVE_KEYWORDS = Arrays.asList(
            "请假", "事假", "调休", "病假", "婚假", "丧假", "产假", "陪产假", "年假", "补休"
    );

    @Autowired
    private IAccessToken tokenCreator;

    @Value("${ddTalk.agentId:}")
    private String agentId;

    @Autowired
    private hrmEmployeeRepository hrmEmployeeRepository;

    @Autowired
    private tbattendanceuserRepository attendanceUserRepository;

    @Autowired
    private tbattendanceapproveRepository approvalRepository;

    @Autowired
    private hrmAttendanceApprovalFetchMarkRepository fetchMarkRepository;

    @Autowired
    private HrmAttendanceApprovalProcessInstanceParser processInstanceParser;

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private com.tianye.hrsystem.common.Redis redis;

    @Autowired
    private AdminMessageServiceImpl adminMessageService;

    // ProgressTracker 前缀（与 HrmAttendanceApprovalServiceImpl 保持一致）
    private static final String FETCH_KEY_PREFIX = "attendance:fetch";

    /** 发起窗口最多往前回溯月数（护栏，防用户乱点把调用量/范围拉爆），默认 3，可配置 */
    @Value("${hrm.approval-fetch.max-months-before:3}")
    private int maxMonthsBefore = 3;

    /** 钉钉 processinstance/listids 单次发起时间跨度上限（毫秒）：官方限 120 天，超出需分片 */
    private static final long LISTIDS_MAX_SPAN_MILLIS = 120L * 24L * 60L * 60L * 1000L;

    /** 当前抓取请求的发起时间窗口（由窗口入口设置，循环抓取结束后复位）。
     *  因 fetch 按公司互斥 + 单实例任务线程串行执行，此处实例字段安全。
     *  仅用于让基类 fetchProcessInstanceIds 按窗口分片拉取，同时保持被测试子类 override 的调用点不变。 */
    private Long requestFetchStartTime;
    private Long requestFetchEndTime;

    @Override
    public long fetchMonthData(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) throws Exception {
        // 老入口/定时任务：业务月抓取。不设发起窗口（requestFetch 字段保持 null）→
        // 基类 listids 按 month 首日~末日、主循环按业务月过滤、收尾按 month 边界清理，行为与改造前一致。
        if (month == null) {
            throw new IllegalArgumentException("请选择月份");
        }
        try {
            return doFetchMonthData(month, employeeIds, approvalTypes);
        } finally {
            requestFetchStartTime = null;
            requestFetchEndTime = null;
        }
    }

    @Override
    public long fetchMonthData(YearMonth month, Long fetchStartTime, Long fetchEndTime,
                               List<Long> employeeIds, List<String> approvalTypes) throws Exception {
        // 未传窗口：等同老入口（业务月抓取）
        if (fetchStartTime == null && fetchEndTime == null) {
            return fetchMonthData(month, employeeIds, approvalTypes);
        }
        // 发起窗口入口（前端手动）：写入实例字段驱动分片拉取与全量落库
        resolveAndValidateFetchWindow(month, fetchStartTime, fetchEndTime);
        logger.info("审批抓取窗口模式, month={}, fetchStartTime={}, fetchEndTime={}, employeeCount={}, approvalTypes={}",
                month,
                requestFetchStartTime,
                requestFetchEndTime,
                employeeIds == null ? 0 : employeeIds.size(),
                summarizeStringList(normalizeApprovalTypes(approvalTypes), 10));
        try {
            return doFetchMonthData(month, employeeIds, approvalTypes);
        } finally {
            requestFetchStartTime = null;
            requestFetchEndTime = null;
        }
    }

    /**
     * 解析并校验本次抓取窗口；同时把合法窗口写入实例字段（供基类分片拉取）。
     * 兼容：未传窗口（老调用/定时任务）→ 按 month 首日~末日；只传了部分 → 缺失端补足。
     * 护栏：fetchStartTime 不得早于 fetchEndTime − maxMonthsBefore 个月。
     */
    private void resolveAndValidateFetchWindow(YearMonth month, Long fetchStartTime, Long fetchEndTime) {
        long start;
        long end;
        if (fetchStartTime == null && fetchEndTime == null) {
            if (month == null) {
                throw new IllegalArgumentException("请选择月份或指定发起时间范围");
            }
            start = month.atDay(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
            end = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE_ID).toInstant().toEpochMilli();
        } else {
            end = (fetchEndTime != null) ? fetchEndTime : System.currentTimeMillis();
            long earliestStart = earliestAllowedFetchStartTime(end);
            start = (fetchStartTime != null) ? fetchStartTime : (end - LISTIDS_MAX_SPAN_MILLIS < earliestStart ? earliestStart : end - LISTIDS_MAX_SPAN_MILLIS);
            if (start >= end) {
                throw new IllegalArgumentException("发起时间范围不合法：开始时间需早于结束时间");
            }
            if (start < earliestStart) {
                throw new IllegalArgumentException("开始日期最早只能选择到今天前 " + maxMonthsBefore + " 个月内，请放宽开始日期后重试");
            }
        }
        requestFetchStartTime = start;
        requestFetchEndTime = end;
    }

    /** 护栏下限：点确定时刻(end)往前 maxMonthsBefore 个月的那天 00:00 */
    private long earliestAllowedFetchStartTime(long endMillis) {
        int months = Math.max(1, maxMonthsBefore);
        java.time.ZonedDateTime endAt = java.time.ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(endMillis), ZONE_ID);
        java.time.ZonedDateTime lower = endAt.minusMonths(months);
        return lower.toLocalDate().atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * 主抓取流程：按当前窗口（requestFetchStartTime/requestFetchEndTime，由 fetchMonthData 先解析写入）
     * 拉取每个员工每个审批模板的实例 id 列表，逐单 get 详情、已通过审批按业务日幂等落库。
     * 窗口起止决定"拉取哪些发起时间段的审批"，业务日(month)只用于列表/统计展示口径与清理口径。
     */
    private long doFetchMonthData(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) throws Exception {
        List<String> normalizedApprovalTypes = normalizeApprovalTypes(approvalTypes);
        if (normalizedApprovalTypes.isEmpty()) {
            throw new IllegalArgumentException("请选择审批类型");
        }
        // 进度：准备阶段
        updateFetchProgress(15, "正在获取员工信息");
        String token = tokenCreator.Refresh();
        List<tbattendanceuser> users = resolveTargetUsers(employeeIds, token);
        if (users == null || users.isEmpty()) {
            logger.warn("审批抓取未命中任何有效员工钉钉映射, month={}, requestedEmployeeIds={}",
                    month, summarizeLongList(employeeIds, 20));
            return 0L;
        }
        Set<String> retainedApprovalIds = new LinkedHashSet<>();
        logger.info("开始手工抓取审批数据, month={}, requestedEmployeeIds={}, resolvedUserCount={}, resolvedUsers={}, approvalTypes={}",
                month,
                summarizeLongList(employeeIds, 20),
                users.size(),
                summarizeResolvedUsers(users, 20),
                summarizeStringList(normalizedApprovalTypes, 10));
        long insertedCount = 0L;
        Set<String> completedUserIds = new HashSet<>();
        List<tbattendanceuser> missingDingTalkUsers = new ArrayList<>();
        // 进度：员工处理阶段
        int totalUsers = users.size();
        int processedUsers = 0;
        int baseProgress = 15;
        int userProgressRange = 75; // 15% - 90%
        for (int userIndex = 0; userIndex < users.size(); userIndex++) {
            tbattendanceuser user = users.get(userIndex);
            if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            // 更新员工处理进度
            processedUsers++;
            int currentProgress = baseProgress + (processedUsers * userProgressRange / totalUsers);
            String progressMessage = String.format("正在处理员工 %d/%d - %s",
                    processedUsers, totalUsers, user.getUserName() != null ? user.getUserName() : "");
            updateFetchProgress(currentProgress, progressMessage);
            logger.info("审批抓取员工映射, month={}, employeeId={}, dingTalkUserId={}, userName={}",
                    month, user.getEmpId(), user.getUserId(), user.getUserName());
            List<String> employeeProcessCodes;
            try {
                employeeProcessCodes = resolveProcessCodes(token, user.getUserId(), normalizedApprovalTypes);
            } catch (Exception ex) {
                if (isDingTalkUserNotFoundException(ex)) {
                    Optional<tbattendanceuser> refreshedUser = refreshDingTalkUserAfterNotFound(token, user);
                    if (refreshedUser.isPresent() && !Objects.equals(refreshedUser.get().getUserId(), user.getUserId())) {
                        user = refreshedUser.get();
                        users.set(userIndex, user);
                        logger.info("审批抓取钉钉用户不存在后已刷新员工映射, month={}, employeeId={}, refreshedDingTalkUserId={}",
                                month, user.getEmpId(), user.getUserId());
                        try {
                            employeeProcessCodes = resolveProcessCodes(token, user.getUserId(), normalizedApprovalTypes);
                        } catch (Exception retryEx) {
                            if (isDingTalkUserNotFoundException(retryEx)) {
                                missingDingTalkUsers.add(user);
                                logger.warn("审批抓取刷新后仍跳过钉钉用户不存在的员工, month={}, employeeId={}, dingTalkUserId={}, userName={}, reason={}",
                                        month, user.getEmpId(), user.getUserId(), user.getUserName(), extractErrorMessage(retryEx));
                                continue;
                            }
                            throw translateWorkflowFetchException("审批模板流程编码解析", retryEx);
                        }
                    } else {
                        missingDingTalkUsers.add(user);
                        logger.warn("审批抓取跳过钉钉用户不存在的员工, month={}, employeeId={}, dingTalkUserId={}, userName={}, reason={}",
                                month, user.getEmpId(), user.getUserId(), user.getUserName(), extractErrorMessage(ex));
                        continue;
                    }
                } else {
                    throw translateWorkflowFetchException("审批模板流程编码解析", ex);
                }
            }
            logger.info("审批抓取流程编码解析结果, month={}, employeeId={}, dingTalkUserId={}, processCodeCount={}, processCodes={}",
                    month, user.getEmpId(), user.getUserId(),
                    employeeProcessCodes == null ? 0 : employeeProcessCodes.size(), summarizeStringList(employeeProcessCodes, 20));
            if (employeeProcessCodes == null || employeeProcessCodes.isEmpty()) {
                throw new IllegalStateException(PROCESS_CODE_MISSING_MESSAGE);
            }
            boolean userFetchCompleted = true;
            for (String processCode : employeeProcessCodes) {
                if (processCode == null || processCode.trim().isEmpty()) {
                    continue;
                }
                List<String> processInstanceIds;
                try {
                    processInstanceIds = fetchProcessInstanceIds(token, user.getUserId(), month, processCode);
                } catch (Exception ex) {
                    if (isDingTalkUserNotFoundException(ex)) {
                        Optional<tbattendanceuser> refreshedUser = refreshDingTalkUserAfterNotFound(token, user);
                        if (refreshedUser.isPresent() && !Objects.equals(refreshedUser.get().getUserId(), user.getUserId())) {
                            user = refreshedUser.get();
                            users.set(userIndex, user);
                            logger.info("审批实例列表拉取时已刷新员工映射, month={}, employeeId={}, refreshedDingTalkUserId={}, processCode={}",
                                    month, user.getEmpId(), user.getUserId(), processCode);
                            try {
                                processInstanceIds = fetchProcessInstanceIds(token, user.getUserId(), month, processCode);
                            } catch (Exception retryEx) {
                                if (isDingTalkUserNotFoundException(retryEx)) {
                                    missingDingTalkUsers.add(user);
                                    userFetchCompleted = false;
                                    logger.warn("审批抓取刷新后仍跳过钉钉用户不存在的员工, month={}, employeeId={}, dingTalkUserId={}, userName={}, processCode={}, reason={}",
                                            month, user.getEmpId(), user.getUserId(), user.getUserName(), processCode, extractErrorMessage(retryEx));
                                    break;
                                }
                                throw translateWorkflowFetchException("topapi/processinstance/listids", retryEx);
                            }
                        } else {
                            missingDingTalkUsers.add(user);
                            userFetchCompleted = false;
                            logger.warn("审批抓取跳过钉钉用户不存在的员工, month={}, employeeId={}, dingTalkUserId={}, userName={}, processCode={}, reason={}",
                                    month, user.getEmpId(), user.getUserId(), user.getUserName(), processCode, extractErrorMessage(ex));
                            break;
                        }
                    } else {
                        throw translateWorkflowFetchException("topapi/processinstance/listids", ex);
                    }
                }
                logger.info("审批抓取实例列表结果, month={}, employeeId={}, dingTalkUserId={}, processCode={}, instanceCount={}, instanceIds={}",
                        month, user.getEmpId(), user.getUserId(), processCode,
                        processInstanceIds == null ? 0 : processInstanceIds.size(), summarizeStringList(processInstanceIds, 20));
                for (String processInstanceId : processInstanceIds) {
                    if (processInstanceId == null || processInstanceId.trim().isEmpty()) {
                        continue;
                    }
                    OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance;
                    try {
                        processInstance = fetchProcessInstanceDetail(token, processInstanceId);
                    } catch (Exception ex) {
                        throw translateWorkflowFetchException("topapi/processinstance/get", ex);
                    }
                    if (!isApprovedProcessInstance(processInstance)) {
                        logger.info("【诊断标记LINKAGE】↓↓↓审批实例非同意完成，跳过入库↓↓↓ month={}, employeeId={}, dingTalkUserId={}, processInstanceId={}, status={}, result={}, operationTypes={}, linkage={}",
                                month,
                                user.getEmpId(),
                                user.getUserId(),
                                processInstanceId,
                                processInstance == null ? null : processInstance.getStatus(),
                                processInstance == null ? null : processInstance.getResult(),
                                summarizeOperationTypes(processInstance),
                                summarizeLinkage(processInstance));
                        // 该实例曾以"通过(COMPLETED+agree)"状态入库，如今钉钉侧已撤销/终止/拒绝（status 非 COMPLETED 等，
                        // 或操作记录含 TERMINATE_PROCESS_INSTANCE 表示已被撤销）。
                        // 为满足"只同步通过未撤销的审批"，凡本次复核为非同意完成的既有本地快照一律删除，
                        // 避免窗口抓取模式（不做整段 stale 清理）把已撤销审批继续保留在列表/统计中。
                        removeRevokedStaleApproval(processInstanceId);
                        continue;
                    }
                    Optional<tbattendanceapprove> parsed = processInstanceParser.parse(processInstanceId, processInstance);
                    if (!parsed.isPresent()) {
                        continue;
                    }
                    tbattendanceapprove entity = parsed.get();
                    if (!matchesApprovalRecord(entity, normalizedApprovalTypes)) {
                        continue;
                    }
                    // 发起窗口入口（前端手动，治跨月）：已通过审批一律幂等落库，不做业务月过滤。
                    // 发起在别月、业务落本次抓取月的跨月单（如7月申请8月年假、9月补卡8月缺卡）也能被拉到并持久化，
                    // 列表/统计页天然按业务 beginTime 归类展示。
                    if (requestFetchStartTime == null && requestFetchEndTime == null) {
                        // 老入口/定时任务（未设发起窗口）：仍按业务日期过滤到目标月，保持老语义
                        if (!isApprovalInTargetMonth(entity, month)) {
                            logger.info("审批实例业务日期不在目标月份内，跳过入库(业务月抓取模式), month={}, employeeId={}, dingTalkUserId={}, processInstanceId={}",
                                    month, user.getEmpId(), user.getUserId(), processInstanceId);
                            continue;
                        }
                    }
                    Optional<tbattendanceapprove> existing = approvalRepository.findById(processInstanceId);
                    if (existing.isPresent()) {
                        tbattendanceapprove existingEntity = existing.get();
                        entity.setCreateTime(existingEntity.getCreateTime());
                        preserveManuallyEditedSubtype(existingEntity, entity);
                    }
                    addRetainedApprovalId(retainedApprovalIds, entity);
                    logger.info("【诊断标记LINKAGE】↓↓↓审批实例同意完成，入库↓↓↓ month={}, employeeId={}, dingTalkUserId={}, processInstanceId={}, status={}, result={}, operationTypes={}, linkage={}, beginTime={}, endTime={}",
                            month, user.getEmpId(), user.getUserId(), processInstanceId,
                            processInstance == null ? null : processInstance.getStatus(),
                            processInstance == null ? null : processInstance.getResult(),
                            summarizeOperationTypes(processInstance),
                            summarizeLinkage(processInstance),
                            entity.getBeginTime(), entity.getEndTime());
                    approvalRepository.save(entity);
                    insertedCount++;
                }
            }
            if (userFetchCompleted) {
                completedUserIds.add(user.getUserId());
            }
        }
        if (completedUserIds.isEmpty() && !missingDingTalkUsers.isEmpty()) {
            throw new IllegalStateException(buildMissingDingTalkUsersMessage(missingDingTalkUsers));
        }
        // 进度：保存数据阶段
        updateFetchProgress(90, "正在保存审批数据");
        List<tbattendanceuser> completedUsers = users.stream()
                .filter(user -> user != null && user.getUserId() != null && completedUserIds.contains(user.getUserId()))
                .collect(Collectors.toList());
        deleteStaleMonthDataForSelectedEmployees(month, employeeIds, completedUsers, normalizedApprovalTypes, retainedApprovalIds);
        saveFetchMarks(month, completedUsers, completedUserIds, normalizedApprovalTypes);
        // 进度：保存完成，等待外层设置最终100%
        updateFetchProgress(95, "审批数据已保存，正在完成收尾");
        return insertedCount;
    }

    protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) throws ApiException {
        List<String> normalizedApprovalTypes = normalizeApprovalTypes(approvalTypes);
        if (normalizedApprovalTypes.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProcessTemplateMeta> templates = resolveProcessTemplates(token, userId);
        List<String> processCodes = new ArrayList<>();
        for (ProcessTemplateMeta template : templates) {
            if (template == null || template.processCode == null || template.processCode.trim().isEmpty()) {
                continue;
            }
            if (matchesApprovalTemplateName(template.templateName, normalizedApprovalTypes)
                    && !processCodes.contains(template.processCode)) {
                processCodes.add(template.processCode);
            }
        }
        return processCodes;
    }

    protected List<ProcessTemplateMeta> resolveProcessTemplates(String token, String userId) throws ApiException {
        return fetchUserVisibleProcessTemplates(token, userId);
    }

    protected List<ProcessTemplateMeta> fetchUserVisibleProcessTemplates(String token, String userId) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/process/listbyuserid");
        long offset = 0L;
        List<ProcessTemplateMeta> templates = new ArrayList<>();
        while (true) {
            OapiProcessListbyuseridRequest request = new OapiProcessListbyuseridRequest();
            request.setUserid(userId);
            request.setOffset(offset);
            request.setSize(100L);
            OapiProcessListbyuseridResponse response = client.execute(request, token);
            // 串行外呼间隔 100ms，降低 1 号集中抓取触发钉钉频控的概率
            try {
                Thread.sleep(100L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            if (!response.isSuccess() || response.getResult() == null) {
                throw new IllegalStateException(buildWorkflowApiErrorDetail(
                        "topapi/process.listbyuserid",
                        response.getErrcode(),
                        response.getErrmsg()
                ));
            }
            OapiProcessListbyuseridResponse.HomePageProcessTemplateVo result = response.getResult();
            if (result.getProcessList() != null) {
                for (OapiProcessListbyuseridResponse.ProcessTopVo template : result.getProcessList()) {
                    if (template == null || template.getProcessCode() == null || template.getProcessCode().trim().isEmpty()) {
                        continue;
                    }
                    templates.add(new ProcessTemplateMeta(template.getName(), template.getProcessCode()));
                }
            }
            Long nextCursor = result.getNextCursor();
            if (nextCursor == null || result.getProcessList() == null || result.getProcessList().isEmpty()) {
                break;
            }
            offset = nextCursor;
        }
        return templates;
    }

    @Override
    public List<Long> resolveFetchTargetEmployeeIds(List<Long> employeeIds) {
        List<tbattendanceuser> users = resolveTargetUsers(employeeIds);
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        return users.stream()
                .map(tbattendanceuser::getEmpId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private void saveFetchMarks(YearMonth month, List<tbattendanceuser> users, Set<String> completedUserIds, List<String> approvalTypes) {
        if (users == null || users.isEmpty() || completedUserIds == null || completedUserIds.isEmpty() || approvalTypes == null || approvalTypes.isEmpty()) {
            return;
        }
        Date now = new Date();
        String monthKey = month.toString();
        for (tbattendanceuser user : users) {
            if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            if (!completedUserIds.contains(user.getUserId())) {
                continue;
            }
            for (String approvalType : approvalTypes) {
                if (fetchMarkRepository.existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType(monthKey, user.getUserId(), FETCH_VERSION, approvalType)) {
                    continue;
                }
                HrmAttendanceApprovalFetchMark mark = new HrmAttendanceApprovalFetchMark();
                mark.setFetchMarkId(BaseUtil.getNextId());
                mark.setMonthKey(monthKey);
                mark.setUserId(user.getUserId());
                mark.setEmployeeId(user.getEmpId());
                mark.setApprovalType(approvalType);
                mark.setFetchVersion(FETCH_VERSION);
                mark.setCreateTime(now);
                mark.setUpdateTime(now);
                fetchMarkRepository.save(mark);
            }
        }
    }

    private List<tbattendanceuser> resolveTargetUsers(List<Long> employeeIds) {
        return resolveTargetUsersFromAttendanceMappings(employeeIds);
    }

    private List<tbattendanceuser> resolveTargetUsers(List<Long> employeeIds, String token) throws ApiException {
        if (hrmEmployeeRepository == null) {
            return resolveTargetUsersFromAttendanceMappings(employeeIds);
        }
        List<HrmEmployee> employees = resolveTargetEmployees(employeeIds);
        if (employees.isEmpty()) {
            return resolveTargetUsersFromAttendanceMappings(employeeIds);
        }
        List<tbattendanceuser> result = new ArrayList<>();
        List<EmployeeResolutionFailure> resolutionFailures = new ArrayList<>();
        for (HrmEmployee employee : employees) {
            try {
                Optional<tbattendanceuser> resolvedUser = resolveAttendanceUserForEmployee(token, employee, false);
                if (resolvedUser.isPresent()) {
                    result.add(resolvedUser.get());
                }
            } catch (Exception ex) {
                resolutionFailures.add(new EmployeeResolutionFailure(employee, ex));
                logger.warn("审批抓取员工钉钉userId预解析失败，跳过该员工, employeeId={}, employeeName={}, mobile={}, dingTalkUserId={}, reason={}",
                        employee.getEmployeeId(),
                        employee.getEmployeeName(),
                        employee.getMobile(),
                        employee.getDingtalkUserId(),
                        extractErrorMessage(ex));
            }
        }
        if (result.isEmpty() && !resolutionFailures.isEmpty()) {
            throw new IllegalStateException(buildEmployeeResolutionFailureMessage(resolutionFailures));
        }
        return result;
    }

    private List<tbattendanceuser> resolveTargetUsersFromAttendanceMappings(List<Long> employeeIds) {
        List<Long> normalizedEmployeeIds = normalizeEmployeeIds(employeeIds);
        if (normalizedEmployeeIds.isEmpty()) {
            return deduplicateUsersByEmployeeId(attendanceUserRepository.findAll());
        }
        List<tbattendanceuser> users = attendanceUserRepository.findAllByEmpIdIn(normalizedEmployeeIds);
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, tbattendanceuser> userMap = new LinkedHashMap<>();
        for (tbattendanceuser user : users) {
            if (user == null || user.getEmpId() == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            userMap.merge(user.getEmpId(), user, this::preferAttendanceUser);
        }
        List<tbattendanceuser> result = new ArrayList<>();
        for (Long employeeId : normalizedEmployeeIds) {
            tbattendanceuser user = userMap.get(employeeId);
            if (user != null) {
                result.add(user);
            }
        }
        if (result.size() < normalizedEmployeeIds.size()) {
            Set<Long> resolvedEmployeeIds = result.stream()
                    .map(tbattendanceuser::getEmpId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            List<Long> missingEmployeeIds = normalizedEmployeeIds.stream()
                    .filter(id -> !resolvedEmployeeIds.contains(id))
                    .collect(Collectors.toList());
            logger.warn("审批抓取员工映射缺失, requestedEmployeeIds={}, resolvedEmployeeIds={}, missingEmployeeIds={}",
                    summarizeLongList(normalizedEmployeeIds, 20),
                    summarizeLongList(new ArrayList<>(resolvedEmployeeIds), 20),
                    summarizeLongList(missingEmployeeIds, 20));
        }
        return result;
    }

    private List<HrmEmployee> resolveTargetEmployees(List<Long> employeeIds) {
        List<Long> normalizedEmployeeIds = normalizeEmployeeIds(employeeIds);
        List<HrmEmployee> employees;
        if (normalizedEmployeeIds.isEmpty()) {
            employees = hrmEmployeeRepository.findAll();
            if (employees == null) {
                return new ArrayList<>();
            }
            return employees.stream()
                    .filter(this::isActiveEmployee)
                    .collect(Collectors.toList());
        }
        employees = hrmEmployeeRepository.findAllByEmployeeIdIn(normalizedEmployeeIds);
        if (employees == null || employees.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, HrmEmployee> employeeMap = employees.stream()
                .filter(Objects::nonNull)
                .filter(employee -> employee.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmEmployee::getEmployeeId, employee -> employee, (left, right) -> left, LinkedHashMap::new));
        List<HrmEmployee> result = new ArrayList<>();
        for (Long employeeId : normalizedEmployeeIds) {
            HrmEmployee employee = employeeMap.get(employeeId);
            if (employee != null && isActiveEmployee(employee)) {
                result.add(employee);
            }
        }
        return result;
    }

    private boolean isActiveEmployee(HrmEmployee employee) {
        return employee != null && (employee.getIsDel() == null || employee.getIsDel() != 1);
    }

    private Optional<tbattendanceuser> resolveAttendanceUserForEmployee(String token,
                                                                        HrmEmployee employee,
                                                                        boolean forceRefresh) throws ApiException {
        if (employee == null || employee.getEmployeeId() == null) {
            return Optional.empty();
        }
        String dingTalkUserId = normalizeText(employee.getDingtalkUserId());
        boolean existingUserIdVerified = false;
        if (!forceRefresh && !dingTalkUserId.isEmpty()) {
            existingUserIdVerified = isDingTalkUserIdMatchingEmployee(token, dingTalkUserId, employee);
            if (!existingUserIdVerified) {
                logger.warn("审批抓取员工表钉钉userId与员工姓名手机号不匹配，准备按姓名手机号刷新, employeeId={}, employeeName={}, mobile={}, oldDingTalkUserId={}",
                        employee.getEmployeeId(), employee.getEmployeeName(), employee.getMobile(), dingTalkUserId);
            }
        }
        boolean mustRefresh = forceRefresh || dingTalkUserId.isEmpty() || !existingUserIdVerified;
        boolean refreshedByLookup = false;
        if (mustRefresh) {
            Optional<String> lookupUserId = fetchDingTalkUserIdByNameAndMobile(token, employee.getEmployeeName(), employee.getMobile());
            if (lookupUserId.isPresent()) {
                dingTalkUserId = lookupUserId.get();
                refreshedByLookup = true;
                saveEmployeeDingTalkUserId(employee, dingTalkUserId);
            }
            if (!refreshedByLookup) {
                Optional<String> verifiedMappingUserId = resolveVerifiedAttendanceMappingUserId(token, employee);
                if (verifiedMappingUserId.isPresent()) {
                    dingTalkUserId = verifiedMappingUserId.get();
                    refreshedByLookup = true;
                    saveEmployeeDingTalkUserId(employee, dingTalkUserId);
                    logger.info("审批抓取复用已校验的本地考勤映射作为员工钉钉userId, employeeId={}, employeeName={}, dingTalkUserId={}",
                            employee.getEmployeeId(), employee.getEmployeeName(), dingTalkUserId);
                }
            }
        }
        if (dingTalkUserId.isEmpty() || (mustRefresh && !existingUserIdVerified && !refreshedByLookup)) {
            logger.warn("审批抓取员工缺少有效钉钉userId且未能通过姓名手机号匹配, employeeId={}, employeeName={}, mobile={}",
                    employee.getEmployeeId(), employee.getEmployeeName(), employee.getMobile());
            return Optional.empty();
        }
        return Optional.of(saveAttendanceUserMapping(employee, dingTalkUserId));
    }

    private Optional<String> resolveVerifiedAttendanceMappingUserId(String token, HrmEmployee employee) throws ApiException {
        if (attendanceUserRepository == null || employee == null || employee.getEmployeeId() == null) {
            return Optional.empty();
        }
        List<tbattendanceuser> mappings = attendanceUserRepository.findAllByEmpId(employee.getEmployeeId());
        if (mappings == null || mappings.isEmpty()) {
            return Optional.empty();
        }
        List<tbattendanceuser> candidates = mappings.stream()
                .filter(Objects::nonNull)
                .filter(mapping -> mapping.getUserId() != null && !mapping.getUserId().trim().isEmpty())
                .sorted((left, right) -> compareAttendanceUserPreference(right, left))
                .collect(Collectors.toList());
        for (tbattendanceuser mapping : candidates) {
            String candidateUserId = normalizeText(mapping.getUserId());
            if (!candidateUserId.isEmpty() && isDingTalkUserIdMatchingEmployee(token, candidateUserId, employee)) {
                return Optional.of(candidateUserId);
            }
        }
        return Optional.empty();
    }

    protected boolean isDingTalkUserIdMatchingEmployee(String token,
                                                       String dingTalkUserId,
                                                       HrmEmployee employee) throws ApiException {
        String normalizedUserId = normalizeText(dingTalkUserId);
        if (normalizedUserId.isEmpty() || employee == null) {
            return false;
        }
        List<DingTalkEmployeeProfile> profiles = fetchDingTalkEmployeeProfiles(token, Collections.singletonList(normalizedUserId));
        if (profiles == null || profiles.isEmpty()) {
            return false;
        }
        for (DingTalkEmployeeProfile profile : profiles) {
            if (profile == null || !normalizedUserId.equals(normalizeText(profile.userId))) {
                continue;
            }
            String employeeName = normalizeEmployeeName(employee.getEmployeeName());
            String profileName = normalizeEmployeeName(profile.name);
            if (!employeeName.isEmpty() && !employeeName.equals(profileName)) {
                return false;
            }
            String employeeMobile = normalizePhoneNumber(employee.getMobile());
            String profileMobile = normalizePhoneNumber(profile.mobile);
            if (!employeeMobile.isEmpty() && !employeeMobile.equals(profileMobile)) {
                return false;
            }
            return true;
        }
        return false;
    }

    private Optional<tbattendanceuser> refreshDingTalkUserAfterNotFound(String token, tbattendanceuser user) {
        if (user == null || user.getEmpId() == null || hrmEmployeeRepository == null) {
            return Optional.empty();
        }
        try {
            List<HrmEmployee> employees = hrmEmployeeRepository.findAllByEmployeeIdIn(Collections.singletonList(user.getEmpId()));
            if (employees == null || employees.isEmpty()) {
                return Optional.empty();
            }
            return resolveAttendanceUserForEmployee(token, employees.get(0), true);
        } catch (Exception ex) {
            logger.warn("审批抓取刷新员工钉钉userId失败, employeeId={}, oldDingTalkUserId={}, reason={}",
                    user.getEmpId(), user.getUserId(), extractErrorMessage(ex));
            return Optional.empty();
        }
    }

    private void saveEmployeeDingTalkUserId(HrmEmployee employee, String dingTalkUserId) {
        if (employee == null || dingTalkUserId == null || dingTalkUserId.trim().isEmpty()) {
            return;
        }
        String normalizedUserId = dingTalkUserId.trim();
        if (Objects.equals(normalizeText(employee.getDingtalkUserId()), normalizedUserId)) {
            return;
        }
        employee.setDingtalkUserId(normalizedUserId);
        employee.setUpdateTime(new Date());
        hrmEmployeeRepository.save(employee);
    }

    private tbattendanceuser saveAttendanceUserMapping(HrmEmployee employee, String dingTalkUserId) {
        Optional<tbattendanceuser> existing = attendanceUserRepository.findFirstByUserId(dingTalkUserId);
        tbattendanceuser mapping = existing.orElseGet(tbattendanceuser::new);
        mapping.setUserId(dingTalkUserId);
        mapping.setUserName(employee.getEmployeeName());
        mapping.setEmpId(employee.getEmployeeId());
        mapping.setDepId(employee.getDeptId());
        if (mapping.getCreateMan() == null) {
            mapping.setCreateMan(1);
        }
        if (mapping.getCreateTime() == null) {
            mapping.setCreateTime(new Date());
        }
        return attendanceUserRepository.save(mapping);
    }

    private List<tbattendanceuser> deduplicateUsersByEmployeeId(List<tbattendanceuser> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, tbattendanceuser> userMap = new LinkedHashMap<>();
        for (tbattendanceuser user : users) {
            if (user == null || user.getEmpId() == null || user.getEmpId() <= 0) {
                continue;
            }
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            userMap.merge(user.getEmpId(), user, this::preferAttendanceUser);
        }
        return new ArrayList<>(userMap.values());
    }

    private tbattendanceuser preferAttendanceUser(tbattendanceuser current, tbattendanceuser candidate) {
        return compareAttendanceUserPreference(current, candidate) >= 0 ? current : candidate;
    }

    private int compareAttendanceUserPreference(tbattendanceuser current, tbattendanceuser candidate) {
        if (current == null) {
            return candidate == null ? 0 : -1;
        }
        if (candidate == null) {
            return 1;
        }
        Date currentCreateTime = current.getCreateTime();
        Date candidateCreateTime = candidate.getCreateTime();
        if (currentCreateTime == null && candidateCreateTime != null) {
            return -1;
        }
        if (currentCreateTime != null && candidateCreateTime == null) {
            return 1;
        }
        if (currentCreateTime != null && candidateCreateTime != null) {
            int compare = candidateCreateTime.compareTo(currentCreateTime);
            if (compare != 0) {
                return compare > 0 ? -1 : 1;
            }
        }
        Integer currentId = current.getId();
        Integer candidateId = candidate.getId();
        if (currentId == null && candidateId != null) {
            return -1;
        }
        if (currentId != null && candidateId == null) {
            return 1;
        }
        if (currentId != null && candidateId != null) {
            int compare = candidateId.compareTo(currentId);
            if (compare != 0) {
                return compare > 0 ? -1 : 1;
            }
        }
        return 0;
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

    private void deleteExistingMonthDataForSelectedEmployees(YearMonth month,
                                                             List<Long> requestedEmployeeIds,
                                                             List<tbattendanceuser> users,
                                                             List<String> approvalTypes) {
        List<Long> normalizedEmployeeIds = normalizeEmployeeIds(requestedEmployeeIds);
        if (month == null || normalizedEmployeeIds.isEmpty() || users == null || users.isEmpty() || approvalTypes == null || approvalTypes.isEmpty()) {
            return;
        }
        List<String> userIds = collectApprovalCleanupUserIds(users);
        List<String> tagNames = resolveTagNamesByApprovalTypes(approvalTypes);
        if (userIds.isEmpty() || tagNames.isEmpty()) {
            return;
        }
        Date begin = Date.from(month.atDay(1).atStartOfDay(ZONE_ID).toInstant());
        Date end = Date.from(month.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE_ID).toInstant());
        long deletedCount = approvalRepository.deleteByBeginTimeBetweenAndUserIdInAndTagNameIn(begin, end, userIds, tagNames);
        logger.info("审批重抓前删除旧数据, month={}, requestedEmployeeIds={}, userIds={}, approvalTypes={}, tagNames={}, deletedCount={}",
                month,
                summarizeLongList(normalizedEmployeeIds, 20),
                summarizeStringList(userIds, 20),
                summarizeStringList(approvalTypes, 10),
                summarizeStringList(tagNames, 10),
                deletedCount);
    }

    private void deleteStaleMonthDataForSelectedEmployees(YearMonth month,
                                                          List<Long> requestedEmployeeIds,
                                                          List<tbattendanceuser> users,
                                                          List<String> approvalTypes,
                                                          Set<String> retainedApprovalIds) {
        // 发起窗口入口（用户自选开始日期→点确定当下）：本表无"审批发起时间"列，
        // 无法按发起窗口安全地表达"只删窗口内未回库的单"；按业务月整段删非 retained
        // 会误删"发起早于窗口起点、但业务落目标月"的既有跨月单。故窗口入口不做整段 stale 清理，
        // 仅靠幂等 upsert 拉回，缺失/撤销单走"同步考勤/人工核对"路径兜底。
        if (requestFetchStartTime != null && requestFetchEndTime != null) {
            logger.info("审批窗口抓取跳过整段陈旧清理(表内无发起时间列、避免误删跨月单), month={}, requestedEmployeeIds={}",
                    month, summarizeLongList(requestedEmployeeIds, 20));
            return;
        }
        List<Long> normalizedEmployeeIds = normalizeEmployeeIds(requestedEmployeeIds);
        if (month == null || users == null || users.isEmpty() || approvalTypes == null || approvalTypes.isEmpty()) {
            return;
        }
        List<Long> cleanupEmployeeIds = normalizedEmployeeIds.isEmpty()
                ? users.stream()
                        .map(tbattendanceuser::getEmpId)
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .collect(Collectors.toList())
                : normalizedEmployeeIds;
        if (cleanupEmployeeIds.isEmpty()) {
            return;
        }
        List<String> userIds = collectApprovalCleanupUserIds(users);
        List<String> tagNames = resolveTagNamesByApprovalTypes(approvalTypes);
        if (userIds.isEmpty() || tagNames.isEmpty()) {
            return;
        }
        Date begin = Date.from(month.atDay(1).atStartOfDay(ZONE_ID).toInstant());
        Date end = Date.from(month.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE_ID).toInstant());
        long deletedCount;
        if (retainedApprovalIds == null || retainedApprovalIds.isEmpty()) {
            deletedCount = approvalRepository.deleteByBeginTimeBetweenAndUserIdInAndTagNameIn(begin, end, userIds, tagNames);
        } else {
            deletedCount = approvalRepository.deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(
                    begin,
                    end,
                    userIds,
                    tagNames,
                    new ArrayList<>(retainedApprovalIds)
            );
        }
        logger.info("审批重抓后清理陈旧数据, month={}, requestedEmployeeIds={}, userIds={}, approvalTypes={}, tagNames={}, retainedApprovalIds={}, deletedCount={}",
                month,
                summarizeLongList(cleanupEmployeeIds, 20),
                summarizeStringList(userIds, 20),
                summarizeStringList(approvalTypes, 10),
                summarizeStringList(tagNames, 10),
                summarizeStringList(retainedApprovalIds == null ? new ArrayList<>() : new ArrayList<>(retainedApprovalIds), 20),
                deletedCount);
    }

    private void addRetainedApprovalId(Set<String> retainedApprovalIds, tbattendanceapprove approval) {
        if (retainedApprovalIds == null || approval == null || approval.getId() == null || approval.getId().trim().isEmpty()) {
            return;
        }
        retainedApprovalIds.add(approval.getId().trim());
    }

    /**
     * 钉钉侧某审批实例本次复核为非同意完成（已被撤销/终止/拒绝）时，删除其此前以"通过"状态入库的本地快照。
     * tbattendanceapprove.id 即钉钉 proc_inst_id，按 id 精确定位，不会误删其他审批；重复删除（无记录）无害。
     * 用于保证"只同步通过未撤销的审批"，同时弥补窗口抓取模式不做整段 stale 清理的缺口。
     */
    private void removeRevokedStaleApproval(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.trim().isEmpty()) {
            return;
        }
        String id = processInstanceId.trim();
        try {
            if (!approvalRepository.findById(id).isPresent()) {
                return;
            }
            approvalRepository.deleteById(id);
            logger.info("审批实例已撤销/非通过，删除本地快照, processInstanceId={}", id);
        } catch (Exception ex) {
            logger.warn("删除已撤销审批本地快照失败, processInstanceId={}, reason={}",
                    id, extractErrorMessage(ex));
        }
    }

    private List<String> collectApprovalCleanupUserIds(List<tbattendanceuser> users) {
        Set<String> userIds = new LinkedHashSet<>();
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        for (tbattendanceuser user : users) {
            if (user == null) {
                continue;
            }
            addUserId(userIds, user.getUserId());
            if (user.getEmpId() == null || attendanceUserRepository == null) {
                continue;
            }
            try {
                List<tbattendanceuser> allMappings = attendanceUserRepository.findAllByEmpId(user.getEmpId());
                if (allMappings == null) {
                    continue;
                }
                for (tbattendanceuser mapping : allMappings) {
                    if (mapping != null) {
                        addUserId(userIds, mapping.getUserId());
                    }
                }
            } catch (Exception ex) {
                logger.warn("审批旧数据清理收集员工历史钉钉userId失败, employeeId={}, reason={}",
                        user.getEmpId(), extractErrorMessage(ex));
            }
        }
        return new ArrayList<>(userIds);
    }

    private void addUserId(Set<String> userIds, String userId) {
        if (userIds == null || userId == null || userId.trim().isEmpty()) {
            return;
        }
        userIds.add(userId.trim());
    }

    private void preserveManuallyEditedSubtype(tbattendanceapprove existingEntity, tbattendanceapprove parsedEntity) {
        if (existingEntity == null || parsedEntity == null) {
            return;
        }
        String existingSubtype = normalizeText(existingEntity.getSubType());
        String parsedSubtype = normalizeText(parsedEntity.getSubType());
        if (!existingSubtype.isEmpty() && !Objects.equals(existingSubtype, parsedSubtype)) {
            parsedEntity.setSubType(existingEntity.getSubType());
        }
    }

    private List<String> resolveTagNamesByApprovalTypes(List<String> approvalTypes) {
        if (approvalTypes == null || approvalTypes.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tagNames = new ArrayList<>();
        for (String approvalType : approvalTypes) {
            if (TYPE_ALL.equals(approvalType)) {
                addTagName(tagNames, "补卡");
                addTagName(tagNames, "请假");
                addTagName(tagNames, "加班");
                addTagName(tagNames, "出差");
                addTagName(tagNames, "外出");
                continue;
            }
            if (TYPE_MISSCARD.equals(approvalType)) {
                addTagName(tagNames, "补卡");
                continue;
            }
            if (TYPE_LEAVE.equals(approvalType)) {
                addTagName(tagNames, "请假");
                continue;
            }
            if (TYPE_OVERTIME.equals(approvalType)) {
                addTagName(tagNames, "加班");
                continue;
            }
            if (TYPE_TRAVEL.equals(approvalType)) {
                addTagName(tagNames, "出差");
                addTagName(tagNames, "外出");
            }
        }
        return tagNames;
    }

    private void addTagName(List<String> tagNames, String tagName) {
        if (tagName == null || tagName.trim().isEmpty() || tagNames.contains(tagName)) {
            return;
        }
        tagNames.add(tagName);
    }

    private List<String> normalizeApprovalTypes(List<String> approvalTypes) {
        if (approvalTypes == null || approvalTypes.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> normalized = new HashSet<>();
        for (String approvalType : approvalTypes) {
            if (approvalType == null || approvalType.trim().isEmpty()) {
                continue;
            }
            String value = approvalType.trim().toLowerCase();
            if (Arrays.asList(TYPE_ALL, TYPE_OVERTIME, TYPE_MISSCARD, TYPE_LEAVE, TYPE_TRAVEL).contains(value)) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private boolean matchesApprovalTypes(OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo approval, List<String> approvalTypes) {
        if (approvalTypes == null || approvalTypes.isEmpty()) {
            return false;
        }
        if (approvalTypes.contains(TYPE_ALL)) {
            return true;
        }
        String tagName = normalizeText(approval.getTagName());
        String subType = normalizeText(approval.getSubType());
        Long bizType = approval.getBizType();
        if (approvalTypes.contains(TYPE_OVERTIME)
                && (Long.valueOf(1L).equals(bizType) || containsAnyKeyword(tagName, subType, "加班"))) {
            return true;
        }
        if (approvalTypes.contains(TYPE_MISSCARD) && containsAnyKeyword(tagName, subType, "补卡")) {
            return true;
        }
        if (approvalTypes.contains(TYPE_LEAVE)
                && (Long.valueOf(3L).equals(bizType) || containsAnyKeyword(tagName, subType, LEAVE_KEYWORDS))) {
            return true;
        }
        if (approvalTypes.contains(TYPE_TRAVEL)
                && (Long.valueOf(2L).equals(bizType) || containsAnyKeyword(tagName, subType, "出差", "外出"))) {
            return true;
        }
        return false;
    }

    private boolean matchesApprovalRecord(tbattendanceapprove approval, List<String> approvalTypes) {
        if (approval == null || approvalTypes == null || approvalTypes.isEmpty()) {
            return false;
        }
        if (approvalTypes.contains(TYPE_ALL)) {
            return true;
        }
        String tagName = normalizeText(approval.getTagName());
        String subType = normalizeText(approval.getSubType());
        Long bizType = approval.getBizType();
        if (approvalTypes.contains(TYPE_OVERTIME)
                && (Long.valueOf(1L).equals(bizType) || containsAnyKeyword(tagName, subType, "加班"))) {
            return true;
        }
        if (approvalTypes.contains(TYPE_MISSCARD) && containsAnyKeyword(tagName, subType, "补卡")) {
            return true;
        }
        if (approvalTypes.contains(TYPE_LEAVE)
                && (Long.valueOf(3L).equals(bizType) || containsAnyKeyword(tagName, subType, LEAVE_KEYWORDS))) {
            return true;
        }
        if (approvalTypes.contains(TYPE_TRAVEL)
                && (Long.valueOf(2L).equals(bizType) || containsAnyKeyword(tagName, subType, "出差", "外出"))) {
            return true;
        }
        return false;
    }

    protected Optional<String> fetchDingTalkUserIdByNameAndMobile(String token,
                                                                  String employeeName,
                                                                  String mobile) throws ApiException {
        String normalizedName = normalizeEmployeeName(employeeName);
        String normalizedMobile = normalizePhoneNumber(mobile);
        if (normalizedName.isEmpty() || normalizedMobile.isEmpty()) {
            return Optional.empty();
        }
        List<String> userIds = fetchOnJobDingTalkUserIds(token);
        if (userIds.isEmpty()) {
            return Optional.empty();
        }
        Set<String> matchedUserIds = new LinkedHashSet<>();
        for (List<String> batch : partition(userIds, 50)) {
            for (DingTalkEmployeeProfile profile : fetchDingTalkEmployeeProfiles(token, batch)) {
                if (profile == null) {
                    continue;
                }
                if (normalizedName.equals(normalizeEmployeeName(profile.name))
                        && normalizedMobile.equals(normalizePhoneNumber(profile.mobile))) {
                    matchedUserIds.add(profile.userId);
                }
            }
        }
        if (matchedUserIds.size() > 1) {
            throw new IllegalStateException("钉钉中存在重复员工，无法按姓名和手机号唯一匹配：" + employeeName + "/" + mobile);
        }
        if (matchedUserIds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matchedUserIds.iterator().next());
    }

    private List<String> fetchOnJobDingTalkUserIds(String token) throws ApiException {
        List<String> result = new ArrayList<>();
        long offset = 0L;
        while (true) {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/queryonjob");
            OapiSmartworkHrmEmployeeQueryonjobRequest request = new OapiSmartworkHrmEmployeeQueryonjobRequest();
            request.setStatusList("2,3,5");
            request.setOffset(offset);
            request.setSize(50L);
            OapiSmartworkHrmEmployeeQueryonjobResponse response = client.execute(request, token);
            if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getResult() == null) {
                throw new IllegalStateException(buildWorkflowApiErrorDetail(
                        "topapi/smartwork/hrm/employee/queryonjob",
                        response == null ? null : response.getErrcode(),
                        response == null ? "response is null" : response.getErrmsg()
                ));
            }
            OapiSmartworkHrmEmployeeQueryonjobResponse.PageResult page = response.getResult();
            if (page.getDataList() != null) {
                for (String userId : page.getDataList()) {
                    if (userId != null && !userId.trim().isEmpty()) {
                        result.add(userId.trim());
                    }
                }
            }
            Long nextCursor = page.getNextCursor();
            if (nextCursor == null) {
                break;
            }
            offset = nextCursor;
        }
        return result;
    }

    private List<DingTalkEmployeeProfile> fetchDingTalkEmployeeProfiles(String token, List<String> userIds) throws ApiException {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/v2/list");
        OapiSmartworkHrmEmployeeV2ListRequest request = new OapiSmartworkHrmEmployeeV2ListRequest();
        request.setUseridList(String.join(",", userIds));
        if (agentId != null && !agentId.trim().isEmpty()) {
            request.setAgentid(Long.parseLong(agentId.trim()));
        }
        OapiSmartworkHrmEmployeeV2ListResponse response = client.execute(request, token);
        if (response == null || response.getResult() == null) {
            throw new IllegalStateException(buildWorkflowApiErrorDetail(
                    "topapi/smartwork/hrm/employee/v2/list",
                    response == null ? null : response.getErrcode(),
                    response == null ? "response is null" : response.getErrmsg()
            ));
        }
        List<DingTalkEmployeeProfile> result = new ArrayList<>();
        for (OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo row : response.getResult()) {
            if (row == null || row.getUserid() == null || row.getUserid().trim().isEmpty()) {
                continue;
            }
            String name = firstRosterFieldValue(row, "姓名", "name", "sys01-name");
            String mobile = firstRosterFieldValue(row, "手机号码", "mobile", "sys02-mobile", "手机号");
            result.add(new DingTalkEmployeeProfile(row.getUserid().trim(), name, mobile));
        }
        return result;
    }

    private String firstRosterFieldValue(OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo row, String... fieldNames) {
        if (row == null || row.getFieldDataList() == null || fieldNames == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null) {
                continue;
            }
            for (OapiSmartworkHrmEmployeeV2ListResponse.EmpFieldDataVo fieldData : row.getFieldDataList()) {
                if (fieldData == null || !fieldName.equals(fieldData.getFieldName())) {
                    continue;
                }
                if (fieldData.getFieldValueList() == null || fieldData.getFieldValueList().isEmpty()) {
                    continue;
                }
                String value = fieldData.getFieldValueList().get(0).getValue();
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private List<List<String>> partition(List<String> values, int size) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        int batchSize = Math.max(size, 1);
        List<List<String>> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index += batchSize) {
            result.add(values.subList(index, Math.min(index + batchSize, values.size())));
        }
        return result;
    }

    protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) throws ApiException {
        // 若本次请求设置了发起时间窗口（用户点确定当下为终点），按 [start, end] 拉取；
        // 否则回退到老行为：按目标业务月的 1 日~末日拉取（老调用/定时任务/测试子类）。
        if (requestFetchStartTime != null && requestFetchEndTime != null) {
            return fetchProcessInstanceIdsInWindow(token, userId, processCode, requestFetchStartTime, requestFetchEndTime);
        }
        return fetchProcessInstanceIdsInWindow(token, userId, processCode,
                month.atDay(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli(),
                month.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE_ID).toInstant().toEpochMilli());
    }

    /**
     * 拉取某审批模板在发起时间窗 [startMillis, endMillis] 内、指定钉钉用户的全部审批实例 id。
     * 钉钉 topapi/processinstance/listids 单次发起时间跨度上限约 120 天，窗口超限则按 120 天分片依次枚举并去重。
     */
    private List<String> fetchProcessInstanceIdsInWindow(String token, String userId, String processCode,
                                                         long startMillis, long endMillis) throws ApiException {
        List<String> allIds = new ArrayList<>();
        long cursor = 0L;
        long sliceStart = startMillis;
        while (sliceStart <= endMillis) {
            long sliceEnd = Math.min(sliceStart + LISTIDS_MAX_SPAN_MILLIS, endMillis);
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/listids");
            cursor = 0L;
            while (true) {
                OapiProcessinstanceListidsRequest request = new OapiProcessinstanceListidsRequest();
                request.setProcessCode(processCode);
                request.setUseridList(userId);
                request.setStartTime(sliceStart);
                request.setEndTime(sliceEnd);
                request.setCursor(cursor);
                request.setSize(20L);
                OapiProcessinstanceListidsResponse response = client.execute(request, token);
                // 串行外呼间隔 100ms，降低 1 号集中抓取触发钉钉频控的概率
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (!response.isSuccess() || response.getResult() == null) {
                    throw new IllegalStateException(buildWorkflowApiErrorDetail(
                            "topapi/processinstance/listids",
                            response.getErrcode(),
                            response.getErrmsg()
                    ));
                }
                if (response.getResult().getList() != null) {
                    allIds.addAll(response.getResult().getList());
                }
                Long nextCursor = response.getResult().getNextCursor();
                if (nextCursor == null || (response.getResult().getList() == null || response.getResult().getList().isEmpty())) {
                    break;
                }
                cursor = nextCursor;
            }
            if (sliceEnd >= endMillis) {
                break;
            }
            sliceStart = sliceEnd + 1L;
        }
        // 分片边界理论上不会重叠，去重兜底
        Set<String> deduplicated = new LinkedHashSet<>(allIds);
        return new ArrayList<>(deduplicated);
    }

    private boolean matchesApprovalTemplateName(String templateName, List<String> approvalTypes) {
        if (approvalTypes == null || approvalTypes.isEmpty()) {
            return false;
        }
        if (approvalTypes.contains(TYPE_ALL)) {
            return true;
        }
        String normalizedTemplateName = normalizeText(templateName);
        if (approvalTypes.contains(TYPE_MISSCARD) && containsAnyKeyword(normalizedTemplateName, "", "补卡")) {
            return true;
        }
        if (approvalTypes.contains(TYPE_OVERTIME) && containsAnyKeyword(normalizedTemplateName, "", "加班")) {
            return true;
        }
        if (approvalTypes.contains(TYPE_TRAVEL) && containsAnyKeyword(normalizedTemplateName, "", "出差", "外出")) {
            return true;
        }
        if (approvalTypes.contains(TYPE_LEAVE) && containsAnyKeyword(normalizedTemplateName, "", LEAVE_KEYWORDS)) {
            return true;
        }
        return false;
    }

    protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                             String processInstanceId) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/get");
        OapiProcessinstanceGetRequest request = new OapiProcessinstanceGetRequest();
        request.setProcessInstanceId(processInstanceId);
        OapiProcessinstanceGetResponse response = client.execute(request, token);
            // 串行外呼间隔 100ms，降低 1 号集中抓取触发钉钉频控的概率
            try {
                Thread.sleep(100L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        if (!response.isSuccess() || response.getProcessInstance() == null) {
            throw new IllegalStateException(buildWorkflowApiErrorDetail(
                    "topapi/processinstance/get",
                    response.getErrcode(),
                    response.getErrmsg()
            ));
        }
        return response.getProcessInstance();
    }

    /** 仅业务月抓取（老入口/定时任务，未设发起窗口）时按业务日期过滤到目标月，保持老语义；
     *  发起窗口入口（前端手动，治跨月）不做此过滤，凡已通过审批一律幂等落库，列表/统计按业务月归类展示。 */
    private boolean isApprovalInTargetMonth(tbattendanceapprove approval, YearMonth month) {
        if (approval == null || month == null) {
            return false;
        }
        Date businessDate = resolveApprovalBusinessDate(approval);
        if (businessDate == null) {
            return false;
        }
        LocalDate localDate = businessDate.toInstant().atZone(ZONE_ID).toLocalDate();
        return YearMonth.from(localDate).equals(month);
    }

    private Date resolveApprovalBusinessDate(tbattendanceapprove approval) {
        if (approval == null) {
            return null;
        }
        if (approval.getBeginTime() != null) {
            return approval.getBeginTime();
        }
        if (approval.getWorkDate() != null) {
            return approval.getWorkDate();
        }
        return approval.getEndTime();
    }

    private IllegalStateException translateWorkflowFetchException(String apiName, Exception ex) {
        String rawMessage = extractErrorMessage(ex);
        logger.error("审批数据抓取调用{}失败: {}", apiName, rawMessage, ex);
        return new IllegalStateException(translateWorkflowErrorMessage(rawMessage), ex);
    }

    private String buildWorkflowApiErrorDetail(String apiName, Long errcode, String errmsg) {
        StringBuilder builder = new StringBuilder(apiName).append("调用失败");
        if (errcode != null) {
            builder.append(", errcode=").append(errcode);
        }
        if (errmsg != null && !errmsg.trim().isEmpty()) {
            builder.append(", errmsg=").append(errmsg.trim());
        }
        return builder.toString();
    }

    private String extractErrorMessage(Exception ex) {
        if (ex == null) {
            return "";
        }
        String message = ex.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message.trim();
        }
        return ex.toString();
    }

    private String translateWorkflowErrorMessage(String rawMessage) {
        String normalized = normalizeText(rawMessage).toLowerCase(Locale.ROOT);
        if (normalized.contains("qyapi_aflow")
                || normalized.contains("qyapi_dingpay_alipay")
                || normalized.contains("permissiondenied")
                || normalized.contains("accessdenied")
                || normalized.contains("没有权限")
                || normalized.contains("无权限")) {
            return WORKFLOW_PERMISSION_DENIED_MESSAGE;
        }
        if (normalized.contains("missingrequiredarguments:processcode")
                || normalized.contains("processcode")
                || normalized.contains("审批流程")) {
            return PROCESS_CODE_MISSING_MESSAGE;
        }
        if (normalized.contains("limitedfrequency")
                || normalized.contains("rate limit")
                || normalized.contains("too many request")
                || normalized.contains("too many call")
                || normalized.contains("限流")
                || normalized.contains("频控")
                || normalized.contains("调用频率")) {
            return DINGTALK_FREQUENCY_LIMIT_MESSAGE;
        }
        return GENERIC_FETCH_ERROR_MESSAGE;
    }

    private String buildEmployeeResolutionFailureMessage(List<EmployeeResolutionFailure> failures) {
        if (failures == null || failures.isEmpty()) {
            return GENERIC_FETCH_ERROR_MESSAGE;
        }
        String reason = translateWorkflowErrorMessage(extractErrorMessage(failures.get(0).exception));
        String employees = failures.stream()
                .filter(Objects::nonNull)
                .limit(10)
                .map(failure -> {
                    HrmEmployee employee = failure.employee;
                    if (employee == null) {
                        return "";
                    }
                    StringBuilder builder = new StringBuilder();
                    if (employee.getEmployeeName() != null && !employee.getEmployeeName().trim().isEmpty()) {
                        builder.append(employee.getEmployeeName().trim());
                    }
                    if (employee.getMobile() != null && !employee.getMobile().trim().isEmpty()) {
                        if (builder.length() > 0) {
                            builder.append("/");
                        }
                        builder.append(employee.getMobile().trim());
                    }
                    if (employee.getEmployeeId() != null) {
                        if (builder.length() > 0) {
                            builder.append("/");
                        }
                        builder.append(employee.getEmployeeId());
                    }
                    return builder.toString();
                })
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(Collectors.joining("，"));
        if (failures.size() > 10) {
            employees = employees + " 等" + failures.size() + "人";
        }
        if (employees.isEmpty()) {
            return reason;
        }
        return reason + "；员工钉钉用户预解析失败：" + employees;
    }

    private boolean isDingTalkUserNotFoundException(Exception ex) {
        String normalized = normalizeText(extractErrorMessage(ex)).toLowerCase(Locale.ROOT);
        return normalized.contains("errcode=400023")
                || normalized.contains("用户不存在")
                || normalized.contains("invaliduser")
                || normalized.contains("useridnotexist");
    }

    private static class EmployeeResolutionFailure {
        private final HrmEmployee employee;
        private final Exception exception;

        private EmployeeResolutionFailure(HrmEmployee employee, Exception exception) {
            this.employee = employee;
            this.exception = exception;
        }
    }

    private String buildMissingDingTalkUsersMessage(List<tbattendanceuser> users) {
        return "钉钉用户不存在或已离职，请先同步员工钉钉用户后重试：" + summarizeResolvedUsers(users, 20);
    }

    private boolean isApprovedProcessInstance(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstance == null) {
            return false;
        }
        // 钉钉对"通过后又被撤销/终止"的审批，processinstance/get 仍可能返回 status=COMPLETED + result=agree，
        // 单看状态/结果无法识别。终止/撤销必定在操作记录里留下 TERMINATE_PROCESS_INSTANCE（=终止(撤销)流程实例）。
        // 凡含此操作记录的实例一律视为已撤销，不再当作"通过"审批入库，以达成"只同步通过未撤销的审批"。
        if (containsTerminateOperation(processInstance)) {
            return false;
        }
        // 关键补充判据（2026-09-05 用户定稿规则）：本地只保留"右上角(业务级)与审批结果(单条审核)都为通过/同意"的最终有效单。
        // 钉钉 App 端按 business_id 归并判断：当某笔业务整体被判"已撤销"，其内部的"撤销后重发"派生单在 App 上右上角显示"已撤销"、
        // 标题带"（撤销）"并收进"撤销流程"折叠项——即使 processinstance/get 对这条派生单仍返回 COMPLETED+agree。
        // 判别它属于"撤销后重发替身"的唯一 API 信号 = bizAction=REVOKE 或 mainProcessInstanceId 非空（指向被顶替/被撤销的原单）。
        // 凡命中即视为业务级已作废，不入库。原单被替代作废另由 attachedProcessInstanceIds 非空暴露（见 hasAttachedReplacementInstances）。
        // 例：CfyM0j(bizAction=REVOKE, mainProcessInstanceId=Do4xK4, attached=空) → 撤销重发替身 → 作废删；
        // hgNowj(bizAction=NONE, mainProcessInstanceId=空, attached=空, COMPLETED+agree) → 独立有效单 → 保留。
        if (isRevokeDerivative(processInstance)) {
            return false;
        }
        if (hasAttachedReplacementInstances(processInstance)) {
            return false;
        }
        return "COMPLETED".equalsIgnoreCase(normalizeText(processInstance.getStatus()))
                && "agree".equalsIgnoreCase(normalizeText(processInstance.getResult()));
    }

    /**
     * 判断钉钉审批实例是否已被"撤销后重发"流程替代作废。钉钉会把后续撤销动作单及撤销后重发的新单 id
     * 挂在被替代的作废原单的 attachedProcessInstanceIds 上（而重发新单自身不携带 attached，仅带
     * mainProcessInstanceId / bizAction 指回原单）。因此 attachedProcessInstanceIds 非空即代表本单已被撤销作废。
     */
    private boolean hasAttachedReplacementInstances(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstance == null) {
            return false;
        }
        List<String> attached = processInstance.getAttachedProcessInstanceIds();
        return attached != null && !attached.isEmpty();
    }

    /**
     * 判断钉钉审批实例是否为"撤销后重发的派生替身"（2026-09-05 用户定稿规则新增）。
     * 钉钉 App 端把这类单在业务维度标为"已撤销/撤销流程"，即便单条流程 processinstance/get 返回 COMPLETED+agree
     * （= 单条"审核结果"通过），也不构成"最终有效通过单"。判别信号 = bizAction=REVOKE（表示该实例是由原实例撤销后
     * 重新发起的）或 mainProcessInstanceId 非空（指回被它顶替/撤销的原单）。
     */
    private boolean isRevokeDerivative(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstance == null) {
            return false;
        }
        if ("REVOKE".equalsIgnoreCase(normalizeText(processInstance.getBizAction()))) {
            return true;
        }
        return !normalizeText(processInstance.getMainProcessInstanceId()).isEmpty();
    }

    /**
     * 判断钉钉审批实例的操作记录中是否包含"终止(撤销)流程实例"操作（TERMINATE_PROCESS_INSTANCE）。
     * 官方操作类型：TERMINATE_PROCESS_INSTANCE = 终止(撤销)流程实例；正常审批通过仅有
     * EXECUTE_TASK_NORMAL / FINISH_PROCESS_INSTANCE 等，因此该信号可精确识别被撤销的审批单。
     */
    private boolean containsTerminateOperation(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstance == null || processInstance.getOperationRecords() == null) {
            return false;
        }
        for (OapiProcessinstanceGetResponse.OperationRecordsVo record : processInstance.getOperationRecords()) {
            if (record == null) {
                continue;
            }
            if ("TERMINATE_PROCESS_INSTANCE".equalsIgnoreCase(normalizeText(record.getOperationType()))) {
                return true;
            }
        }
        return false;
    }

    /** 汇总实例操作记录的操作类型，供诊断日志观察被撤销/终止信号。 */
    private String summarizeOperationTypes(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstance == null || processInstance.getOperationRecords() == null
                || processInstance.getOperationRecords().isEmpty()) {
            return "[]";
        }
        List<String> types = new ArrayList<>();
        for (OapiProcessinstanceGetResponse.OperationRecordsVo record : processInstance.getOperationRecords()) {
            if (record != null && record.getOperationType() != null && !record.getOperationType().trim().isEmpty()) {
                types.add(record.getOperationType().trim());
            }
        }
        return String.join(",", types);
    }

    /**
     * 汇总实例的"撤销/重发"关联字段（诊断用）：bizAction / mainProcessInstanceId /
     * attachedProcessInstanceIds / finishTime。钉钉在"已通过审批被撤销后重新发起"时会在新实例上
     * 打 bizAction、并以 attached 关联原单；这些字段可能提供"通过后撤销"的可识别信号。
     */
    private String summarizeLinkage(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstance == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("bizAction=").append(processInstance.getBizAction());
        sb.append(",mainProcessInstanceId=").append(processInstance.getMainProcessInstanceId());
        List<String> attached = processInstance.getAttachedProcessInstanceIds();
        sb.append(",attachedProcessInstanceIds=").append(attached == null || attached.isEmpty() ? "[]" : String.join(",", attached));
        sb.append(",finishTime=").append(processInstance.getFinishTime());
        return sb.toString();
    }

    private boolean containsAnyKeyword(String tagName, String subType, List<String> keywords) {
        return containsAnyKeyword(tagName, subType, keywords.toArray(new String[0]));
    }

    private boolean containsAnyKeyword(String tagName, String subType, String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return false;
        }
        String normalizedTagName = normalizeText(tagName);
        String normalizedSubType = normalizeText(subType);
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeText(keyword);
            if (normalizedKeyword.isEmpty()) {
                continue;
            }
            if (normalizedTagName.contains(normalizedKeyword) || normalizedSubType.contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "").trim();
    }

    private String normalizeEmployeeName(String value) {
        return normalizeText(value);
    }

    private String normalizePhoneNumber(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("[\\s\\-\\(\\)\\+]", "");
        if (normalized.startsWith("86") && normalized.length() == 13) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private String summarizeLongList(List<Long> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "all";
        }
        List<String> items = values.stream()
                .filter(value -> value != null)
                .map(String::valueOf)
                .collect(Collectors.toList());
        return summarizeItems(items, limit);
    }

    private String summarizeStringList(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        List<String> items = values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .collect(Collectors.toList());
        if (items.isEmpty()) {
            return "[]";
        }
        return summarizeItems(items, limit);
    }

    private String summarizeResolvedUsers(List<tbattendanceuser> users, int limit) {
        if (users == null || users.isEmpty()) {
            return "[]";
        }
        List<String> items = new ArrayList<>();
        for (tbattendanceuser user : users) {
            if (user == null) {
                continue;
            }
            String employeeId = user.getEmpId() == null ? "null" : String.valueOf(user.getEmpId());
            String userName = normalizeText(user.getUserName()).isEmpty() ? "-" : user.getUserName().trim();
            String dingTalkUserId = normalizeText(user.getUserId()).isEmpty() ? "-" : user.getUserId().trim();
            items.add(employeeId + "/" + userName + "/" + dingTalkUserId);
        }
        if (items.isEmpty()) {
            return "[]";
        }
        return summarizeItems(items, limit);
    }

    private String summarizeItems(List<String> items, int limit) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        int safeLimit = Math.max(limit, 1);
        List<String> sample = items.stream().limit(safeLimit).collect(Collectors.toList());
        if (items.size() <= safeLimit) {
            return sample.toString();
        }
        return sample + "...(total=" + items.size() + ")";
    }

    protected static class ProcessTemplateMeta {
        private final String templateName;
        private final String processCode;

        protected ProcessTemplateMeta(String templateName, String processCode) {
            this.templateName = templateName;
            this.processCode = processCode;
        }
    }

    private static class DingTalkEmployeeProfile {
        private final String userId;
        private final String name;
        private final String mobile;

        private DingTalkEmployeeProfile(String userId, String name, String mobile) {
            this.userId = userId;
            this.name = name;
            this.mobile = mobile;
        }
    }

    /**
     * 更新审批获取进度
     */
    private void updateFetchProgress(int percent, String message) {
        String companyId = getCompanyId();
        ProgressTracker.ProgressState state = new ProgressTracker.ProgressState();
        state.progress = Math.max(0, Math.min(100, percent));
        state.status = ProgressTracker.STATUS_RUNNING;
        state.message = message;
        state.done = false;
        state.success = false;
        if (progressTracker != null) {
            progressTracker.saveProgress(FETCH_KEY_PREFIX, companyId, state);
        }
        // 同步更新通知中心的进度内容
        updateNotificationProgress(companyId, percent);
    }

    /**
     * 更新通知中心的进度内容（实时百分比）
     */
    private void updateNotificationProgress(String companyId, int percent) {
        try {
            String messageIdStr = redis.get("attendance:notification:fetch:" + companyId);
            if (messageIdStr == null || messageIdStr.isEmpty()) {
                return;
            }
            Long messageId = Long.parseLong(messageIdStr);
            adminMessageService.updateContent(messageId, "进度 " + percent + "%");
        } catch (Exception e) {
            logger.warn("[审批获取进度] 更新通知内容失败", e);
        }
    }

    /**
     * 获取当前公司 ID
     */
    private String getCompanyId() {
        if (CompanyContext.get() != null && CompanyContext.get().getCompanyId() != null) {
            return CompanyContext.get().getCompanyId();
        }
        return "default";
    }
}
