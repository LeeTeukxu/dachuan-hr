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

    @Override
    public long fetchMonthData(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) throws Exception {
        List<String> normalizedApprovalTypes = normalizeApprovalTypes(approvalTypes);
        if (normalizedApprovalTypes.isEmpty()) {
            throw new IllegalArgumentException("请选择审批类型");
        }
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
        for (int userIndex = 0; userIndex < users.size(); userIndex++) {
            tbattendanceuser user = users.get(userIndex);
            if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
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
                        logger.info("审批实例非同意完成，跳过入库, month={}, employeeId={}, dingTalkUserId={}, processInstanceId={}, status={}, result={}",
                                month,
                                user.getEmpId(),
                                user.getUserId(),
                                processInstanceId,
                                processInstance == null ? null : processInstance.getStatus(),
                                processInstance == null ? null : processInstance.getResult());
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
                    if (!isApprovalInTargetMonth(entity, month)) {
                        logger.info("审批实例业务日期不在目标月份内，跳过入库, month={}, employeeId={}, dingTalkUserId={}, processInstanceId={}, beginTime={}, workDate={}, endTime={}",
                                month,
                                user.getEmpId(),
                                user.getUserId(),
                                processInstanceId,
                                entity.getBeginTime(),
                                entity.getWorkDate(),
                                entity.getEndTime());
                        continue;
                    }
                    Optional<tbattendanceapprove> existing = approvalRepository.findById(processInstanceId);
                    if (existing.isPresent()) {
                        tbattendanceapprove existingEntity = existing.get();
                        entity.setCreateTime(existingEntity.getCreateTime());
                        preserveManuallyEditedSubtype(existingEntity, entity);
                    }
                    addRetainedApprovalId(retainedApprovalIds, entity);
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
        List<tbattendanceuser> completedUsers = users.stream()
                .filter(user -> user != null && user.getUserId() != null && completedUserIds.contains(user.getUserId()))
                .collect(Collectors.toList());
        deleteStaleMonthDataForSelectedEmployees(month, employeeIds, completedUsers, normalizedApprovalTypes, retainedApprovalIds);
        saveFetchMarks(month, completedUsers, completedUserIds, normalizedApprovalTypes);
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
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/listids");
        long cursor = 0L;
        List<String> result = new ArrayList<>();
        while (true) {
            OapiProcessinstanceListidsRequest request = new OapiProcessinstanceListidsRequest();
            request.setProcessCode(processCode);
            request.setUseridList(userId);
            request.setStartTime(month.atDay(1).atStartOfDay(ZONE_ID).toInstant().toEpochMilli());
            request.setEndTime(month.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE_ID).toInstant().toEpochMilli());
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
                result.addAll(response.getResult().getList());
            }
            Long nextCursor = response.getResult().getNextCursor();
            if (nextCursor == null || (response.getResult().getList() == null || response.getResult().getList().isEmpty())) {
                break;
            }
            cursor = nextCursor;
        }
        return result;
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
        return "COMPLETED".equalsIgnoreCase(normalizeText(processInstance.getStatus()))
                && "agree".equalsIgnoreCase(normalizeText(processInstance.getResult()));
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
}
