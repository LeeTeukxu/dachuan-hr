package com.tianye.hrsystem.imple;

import com.dingtalk.api.response.OapiProcessinstanceGetResponse;
import com.tianye.hrsystem.model.HrmAttendanceApprovalFetchMark;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HrmAttendanceApprovalSyncServiceImplWorkflowTest {

    @Test
    public void approvalRepositoryModifyingDeleteWithRetainedIds_shouldUseSpringSupportedReturnType() throws Exception {
        Method method = tbattendanceapproveRepository.class.getMethod(
                "deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn",
                Date.class,
                Date.class,
                List.class,
                List.class,
                List.class
        );

        Assert.assertEquals(Integer.TYPE, method.getReturnType());
    }

    @Test
    public void fetchMonthData_shouldPersistWorkflowApprovalsForSelectedTypes() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicReference<tbattendanceapprove> savedApproval = new AtomicReference<>();
        AtomicReference<HrmAttendanceApprovalFetchMark> savedMark = new AtomicReference<>();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    if ("findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        savedApproval.set((tbattendanceapprove) args[0]);
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        savedMark.set((HrmAttendanceApprovalFetchMark) args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processIds = Collections.singletonList("PROC-LEAVE");
        service.processDetail = process("请假审批", "ding-101",
                component("请假类型", "调休"),
                component("开始时间", "2026-05-02 09:00"),
                component("结束时间", "2026-05-02 18:00"),
                component("时长", "1天"));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 5), Collections.singletonList(101L), Collections.singletonList("leave"));

        Assert.assertEquals("应写入一条审批快照", 1L, insertedCount);
        Assert.assertEquals("审批快照应保存一次", 1, saveCount.get());
        Assert.assertNotNull("应保存审批快照", savedApproval.get());
        Assert.assertEquals("请假", savedApproval.get().getTagName());
        Assert.assertEquals("调休", savedApproval.get().getSubType());
        Assert.assertNotNull("应写入完成标记", savedMark.get());
        Assert.assertEquals("leave", savedMark.get().getApprovalType());
    }

    @Test
    public void fetchMonthData_shouldTranslateWorkflowPermissionErrorToReadableMessage() throws Exception {
        PermissionDeniedWorkflowSyncService service = new PermissionDeniedWorkflowSyncService();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    if ("findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        try {
            service.fetchMonthData(YearMonth.of(2026, 5), Collections.singletonList(101L), Collections.singletonList("leave"));
            Assert.fail("审批权限不足时应抛出明确异常");
        } catch (IllegalStateException ex) {
            Assert.assertEquals("当前钉钉应用未开通审批读取权限，请联系管理员开通后重试", ex.getMessage());
        }
    }

    @Test
    public void fetchMonthData_shouldTranslateMisleadingTemplateScopeErrorToReadableMessage() throws Exception {
        MisleadingTemplatePermissionWorkflowSyncService service = new MisleadingTemplatePermissionWorkflowSyncService();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    if ("findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        try {
            service.fetchMonthData(YearMonth.of(2026, 5), Collections.singletonList(101L), Collections.singletonList("leave"));
            Assert.fail("审批模板权限异常时应抛出明确异常");
        } catch (IllegalStateException ex) {
            Assert.assertEquals("当前钉钉应用未开通审批读取权限，请联系管理员开通后重试", ex.getMessage());
        }
    }

    @Test
    public void fetchMonthData_shouldQueryProcessInstancesByResolvedProcessCodes() throws Exception {
        ProcessCodeAwareWorkflowSyncService service = new ProcessCodeAwareWorkflowSyncService();
        AtomicReference<tbattendanceapprove> savedApproval = new AtomicReference<>();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        savedApproval.set((tbattendanceapprove) args[0]);
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Arrays.asList("PROC-CODE-LEAVE-1", "PROC-CODE-LEAVE-2");
        service.processIdsByCode.put("PROC-CODE-LEAVE-1", Collections.singletonList("PROC-LEAVE-1"));
        service.processIdsByCode.put("PROC-CODE-LEAVE-2", Collections.emptyList());
        service.processDetail = process("请假审批", "ding-101",
                component("请假类型", "调休"),
                component("开始时间", "2026-05-02 09:00"),
                component("结束时间", "2026-05-02 18:00"),
                component("时长", "1天"));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 5), Collections.singletonList(101L), Collections.singletonList("leave"));

        Assert.assertEquals("应按命中的流程编码写入审批快照", 1L, insertedCount);
        Assert.assertEquals("审批快照应保存一次", 1, saveCount.get());
        Assert.assertEquals("应按解析出的流程编码依次查询", Arrays.asList("PROC-CODE-LEAVE-1", "PROC-CODE-LEAVE-2"), service.requestedProcessCodes);
        Assert.assertNotNull("应保存审批快照", savedApproval.get());
        Assert.assertEquals("请假", savedApproval.get().getTagName());
    }

    @Test
    public void fetchMonthData_shouldPersistOnlyApprovedWorkflowInstances() throws Exception {
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));
        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-AGREE", "PROC-REFUSE", "PROC-TERMINATED");
        service.processDetails.put("PROC-AGREE", processWithState("加班审批", "ding-101", "COMPLETED", "agree",
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        service.processDetails.put("PROC-REFUSE", processWithState("加班审批", "ding-101", "COMPLETED", "refuse",
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 07:05"),
                component("预计加班时长", "1")));
        service.processDetails.put("PROC-TERMINATED", processWithState("加班审批", "ding-101", "TERMINATED", "",
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("只有同意完成的审批实例应写入本地快照", 1L, insertedCount);
        Assert.assertEquals("只应保存一条审批快照", 1, saveCount.get());
        Assert.assertEquals(Collections.singletonList("PROC-AGREE"), savedApprovalIds);
    }

    @Test
    public void fetchMonthData_shouldDeleteRevokedStaleApprovalWhenReFetched() throws Exception {
        // 回归：某审批此前以"通过(COMPLETED+agree)"入库，钉钉侧随后被撤销(status=TERMINATED)。
        // 重新抓取复核到该实例非同意完成时，必须删除其本地旧快照，保证"只同步通过未撤销的审批"。
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();
        AtomicReference<String> deletedApprovalId = new AtomicReference<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));

        // 模拟库里已存在该审批的旧"通过"快照
        tbattendanceapprove staleApproved = new tbattendanceapprove();
        staleApproved.setId("PROC-REVOKED");
        staleApproved.setUserId("ding-101");
        staleApproved.setTagName("加班");

        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        if ("PROC-REVOKED".equals(args[0])) {
                            return Optional.of(staleApproved);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedApprovalId.set((String) args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-VALID", "PROC-REVOKED");
        service.processDetails.put("PROC-VALID", processWithState("加班审批", "ding-101", "COMPLETED", "agree",
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        service.processDetails.put("PROC-REVOKED", processWithState("加班审批", "ding-101", "TERMINATED", "",
                component("加班日期", "2026-06-16 18:00"),
                component("结束日期", "2026-06-16 19:00"),
                component("预计加班时长", "1")));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("只有同意完成的审批实例应写入本地快照", 1L, insertedCount);
        Assert.assertEquals("有效审批应保存一次", 1, saveCount.get());
        Assert.assertEquals(Collections.singletonList("PROC-VALID"), savedApprovalIds);
        Assert.assertEquals("已撤销的审批旧快照应被删除", "PROC-REVOKED", deletedApprovalId.get());
    }

    @Test
    public void fetchMonthData_windowMode_shouldDeleteRevokedStaleApproval() throws Exception {
        // 手工"获取审批数据"走窗口模式（发起时间窗口，不做整段 stale 清理）。
        // 已入库的审批被撤销后，重抓复核为非同意完成时仍须按 id 删除其旧快照，不能残留在列表。
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();
        AtomicReference<String> deletedApprovalId = new AtomicReference<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));

        tbattendanceapprove staleApproved = new tbattendanceapprove();
        staleApproved.setId("PROC-REVOKED");
        staleApproved.setUserId("ding-101");
        staleApproved.setTagName("加班");

        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        if ("PROC-REVOKED".equals(args[0])) {
                            return Optional.of(staleApproved);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedApprovalId.set((String) args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-VALID", "PROC-REVOKED");
        service.processDetails.put("PROC-VALID", processWithState("加班审批", "ding-101", "COMPLETED", "agree",
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        service.processDetails.put("PROC-REVOKED", processWithState("加班审批", "ding-101", "TERMINATED", "",
                component("加班日期", "2026-06-16 18:00"),
                component("结束日期", "2026-06-16 19:00"),
                component("预计加班时长", "1")));

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        long start = java.time.LocalDate.of(2026, 5, 1).atStartOfDay(zone).toInstant().toEpochMilli();
        long end = java.time.LocalDate.of(2026, 7, 31).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli();

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), start, end,
                Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("窗口模式只应写入同意完成的审批实例", 1L, insertedCount);
        Assert.assertEquals("有效审批应保存一次", 1, saveCount.get());
        Assert.assertEquals(Collections.singletonList("PROC-VALID"), savedApprovalIds);
        Assert.assertEquals("窗口模式下已撤销的审批旧快照也应被删除", "PROC-REVOKED", deletedApprovalId.get());
    }

    @Test
    public void fetchMonthData_shouldSkipRevokedApprovalWhenReportedCompletedAgreeWithTerminateOperation() throws Exception {
        // 关键回归：钉钉对"通过后又被撤销"的审批，processinstance/get 仍返回 status=COMPLETED + result=agree，
        // 单看状态/结果无法识别。其操作记录必含 TERMINATE_PROCESS_INSTANCE(=终止(撤销)流程实例)。
        // 该实例必须视为已撤销：不得新入库；若此前已有本地"通过"快照，重抓时必须删除。
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();
        AtomicReference<String> deletedApprovalId = new AtomicReference<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));

        tbattendanceapprove staleApproved = new tbattendanceapprove();
        staleApproved.setId("PROC-REVOKED-BY-OP");
        staleApproved.setUserId("ding-101");
        staleApproved.setTagName("加班");

        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        if ("PROC-REVOKED-BY-OP".equals(args[0])) {
                            return Optional.of(staleApproved);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedApprovalId.set((String) args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-VALID", "PROC-REVOKED-BY-OP");
        service.processDetails.put("PROC-VALID", processWithStateAndOperations("加班审批", "ding-101", "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE")),
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        // status=COMPLETED、result=agree 表面通过，但操作记录含 TERMINATE_PROCESS_INSTANCE，即已被撤销
        service.processDetails.put("PROC-REVOKED-BY-OP", processWithStateAndOperations("加班审批", "ding-101", "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE"),
                        operationRecord("TERMINATE_PROCESS_INSTANCE")),
                component("加班日期", "2026-06-16 18:00"),
                component("结束日期", "2026-06-16 19:00"),
                component("预计加班时长", "1")));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("仅无撤销操作记录的真实通过实例应写入", 1L, insertedCount);
        Assert.assertEquals(Collections.singletonList("PROC-VALID"), savedApprovalIds);
        Assert.assertEquals("表面 COMPLETED+agree 但被撤销的实例其旧快照应被删除", "PROC-REVOKED-BY-OP", deletedApprovalId.get());
    }

    @Test
    public void fetchMonthData_windowMode_shouldSkipRevokedApprovalWhenReportedCompletedAgreeWithTerminateOperation() throws Exception {
        // 手工"获取审批数据"窗口模式：撤销单被钉钉报成 COMPLETED+agree、但操作记录含 TERMINATE_PROCESS_INSTANCE，
        // 必须同样视为非通过：不写入、并删除其既有本地快照，保证列表/统计不再出现撤销审批。
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();
        AtomicReference<String> deletedApprovalId = new AtomicReference<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));

        tbattendanceapprove staleApproved = new tbattendanceapprove();
        staleApproved.setId("PROC-REVOKED-BY-OP");
        staleApproved.setUserId("ding-101");
        staleApproved.setTagName("加班");

        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        if ("PROC-REVOKED-BY-OP".equals(args[0])) {
                            return Optional.of(staleApproved);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedApprovalId.set((String) args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-VALID", "PROC-REVOKED-BY-OP");
        service.processDetails.put("PROC-VALID", processWithStateAndOperations("加班审批", "ding-101", "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE")),
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        service.processDetails.put("PROC-REVOKED-BY-OP", processWithStateAndOperations("加班审批", "ding-101", "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE"),
                        operationRecord("TERMINATE_PROCESS_INSTANCE")),
                component("加班日期", "2026-06-16 18:00"),
                component("结束日期", "2026-06-16 19:00"),
                component("预计加班时长", "1")));

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        long start = java.time.LocalDate.of(2026, 5, 1).atStartOfDay(zone).toInstant().toEpochMilli();
        long end = java.time.LocalDate.of(2026, 7, 31).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli();

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), start, end,
                Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("窗口模式仅应写入无撤销操作记录的真实通过实例", 1L, insertedCount);
        Assert.assertEquals(Collections.singletonList("PROC-VALID"), savedApprovalIds);
        Assert.assertEquals("窗口模式下被撤销(表面 COMPLETED+agree)实例旧快照也应删除", "PROC-REVOKED-BY-OP", deletedApprovalId.get());
    }

    @Test
    public void fetchMonthData_shouldDropBothRevokedOriginalAndRevokeReissuedDerivative() throws Exception {
        // 决定性回归（2026-09-05 用户定稿规则）：本地只保留"右上角(业务级)与审批结果都为通过/同意"的最终有效单。
        // 钉钉对"通过后被撤销替代的作废原单"(Do4xK4 型)与"撤销后重发的派生单"(CfyM0j 型)都仍返回 COMPLETED+agree 且无 TERMINATE：
        //  - 作废原单通过 attachedProcessInstanceIds 非空暴露被顶替；
        //  - 撤销后重发替身通过 bizAction=REVOKE / mainProcessInstanceId 非空暴露（即使 attached 为空、单条接口报 agree）。
        // 二者在钉钉 App 端业务维度均属"已撤销"（右上角标已撤销、收进"撤销流程"），一律视为非通过：不写入并删除既有本地快照。
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();
        java.util.List<String> deletedApprovalIds = new java.util.ArrayList<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));

        tbattendanceapprove staleOriginal = new tbattendanceapprove();
        staleOriginal.setId("PROC-REVOKED-ORIGINAL");
        staleOriginal.setUserId("ding-101");
        staleOriginal.setTagName("加班");
        tbattendanceapprove staleReissued = new tbattendanceapprove();
        staleReissued.setId("PROC-REISSUED");
        staleReissued.setUserId("ding-101");
        staleReissued.setTagName("加班");

        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        if ("PROC-REVOKED-ORIGINAL".equals(args[0])) {
                            return Optional.of(staleOriginal);
                        }
                        if ("PROC-REISSUED".equals(args[0])) {
                            return Optional.of(staleReissued);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedApprovalIds.add((String) args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-VALID", "PROC-REVOKED-ORIGINAL", "PROC-REISSUED");
        service.processDetails.put("PROC-VALID", processWithStateAndOperations("加班审批", "ding-101", "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE")),
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        // 作废原单：COMPLETED+agree、无 TERMINATE，但 attached 含撤销动作单与重发新单 → 被撤销替代，作废
        service.processDetails.put("PROC-REVOKED-ORIGINAL",
                revokedOriginalWithAttached("加班审批", "ding-101", "COMPLETED", "agree",
                        java.util.Arrays.asList(
                                operationRecord("EXECUTE_TASK_NORMAL"),
                                operationRecord("FINISH_PROCESS_INSTANCE")),
                        java.util.Arrays.asList("PROC-REVOKE-ACTION", "PROC-REISSUED"),
                        component("加班日期", "2026-06-16 18:00"),
                        component("结束日期", "2026-06-16 19:00"),
                        component("预计加班时长", "1")));
        // 撤销后重发的派生替身：attached 为空、单条接口报 agree，但 bizAction=REVOKE+mainProcessInstanceId 指回原单
        // → 在钉钉 App 业务维度属"已撤销/撤销流程"，即便单条审核结果同意，也不构成最终有效通过单 → 作废
        service.processDetails.put("PROC-REISSUED",
                reissuedAfterRevoke("加班审批", "ding-101", "PROC-REVOKED-ORIGINAL",
                        component("加班日期", "2026-06-16 18:00"),
                        component("结束日期", "2026-06-16 19:00"),
                        component("预计加班时长", "1")));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("仅正常单写入，被撤销作废原单与 REVOKE 重发派生单均不写入", 1L, insertedCount);
        Assert.assertTrue("应写入正常单", savedApprovalIds.contains("PROC-VALID"));
        Assert.assertFalse("被撤销替代的作废原单不应写入", savedApprovalIds.contains("PROC-REVOKED-ORIGINAL"));
        Assert.assertFalse("撤销后重发的 REVOKE 派生单不应写入", savedApprovalIds.contains("PROC-REISSUED"));
        Assert.assertTrue("作废原单(attached非空)既有本地快照应被删除", deletedApprovalIds.contains("PROC-REVOKED-ORIGINAL"));
        Assert.assertTrue("REVOKE 重发派生单(main非空)既有本地快照也应被删除", deletedApprovalIds.contains("PROC-REISSUED"));
    }

    @Test
    public void fetchMonthData_windowMode_shouldDropBothRevokedOriginalAndRevokeReissuedDerivative() throws Exception {
        // 手工窗口模式同样适用（用户定稿规则）：被撤销替代的作废原单(attached 非空)与撤销后重发的派生替身
        // (bizAction=REVOKE / mainProcessInstanceId 非空，即使单条接口报 COMPLETED+agree)均为业务级"已撤销"，一律删除且不写入。
        InstanceStateWorkflowSyncService service = new InstanceStateWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        java.util.List<String> savedApprovalIds = new java.util.ArrayList<>();
        java.util.List<String> deletedApprovalIds = new java.util.ArrayList<>();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(user), new AtomicReference<>()));

        tbattendanceapprove staleOriginal = new tbattendanceapprove();
        staleOriginal.setId("PROC-REVOKED-ORIGINAL");
        staleOriginal.setUserId("ding-101");
        staleOriginal.setTagName("加班");
        tbattendanceapprove staleReissued = new tbattendanceapprove();
        staleReissued.setId("PROC-REISSUED");
        staleReissued.setUserId("ding-101");
        staleReissued.setTagName("加班");

        setField(service, "approvalRepository", Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        if ("PROC-REVOKED-ORIGINAL".equals(args[0])) {
                            return Optional.of(staleOriginal);
                        }
                        if ("PROC-REISSUED".equals(args[0])) {
                            return Optional.of(staleReissued);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        tbattendanceapprove approval = (tbattendanceapprove) args[0];
                        savedApprovalIds.add(approval.getId());
                        saveCount.incrementAndGet();
                        return approval;
                    }
                    if ("deleteById".equals(method.getName())) {
                        deletedApprovalIds.add((String) args[0]);
                        return defaultValue(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                }));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        service.processIds = Arrays.asList("PROC-VALID", "PROC-REVOKED-ORIGINAL", "PROC-REISSUED");
        service.processDetails.put("PROC-VALID", processWithStateAndOperations("加班审批", "ding-101", "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE")),
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1")));
        service.processDetails.put("PROC-REVOKED-ORIGINAL",
                revokedOriginalWithAttached("加班审批", "ding-101", "COMPLETED", "agree",
                        java.util.Arrays.asList(
                                operationRecord("EXECUTE_TASK_NORMAL"),
                                operationRecord("FINISH_PROCESS_INSTANCE")),
                        java.util.Arrays.asList("PROC-REVOKE-ACTION", "PROC-REISSUED"),
                        component("加班日期", "2026-06-16 18:00"),
                        component("结束日期", "2026-06-16 19:00"),
                        component("预计加班时长", "1")));
        service.processDetails.put("PROC-REISSUED",
                reissuedAfterRevoke("加班审批", "ding-101", "PROC-REVOKED-ORIGINAL",
                        component("加班日期", "2026-06-16 18:00"),
                        component("结束日期", "2026-06-16 19:00"),
                        component("预计加班时长", "1")));

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        long start = java.time.LocalDate.of(2026, 5, 1).atStartOfDay(zone).toInstant().toEpochMilli();
        long end = java.time.LocalDate.of(2026, 7, 31).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli();

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), start, end,
                Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("窗口模式：仅正常单写入", 1L, insertedCount);
        Assert.assertTrue("应写入正常单", savedApprovalIds.contains("PROC-VALID"));
        Assert.assertFalse("被撤销替代的作废原单不应写入", savedApprovalIds.contains("PROC-REVOKED-ORIGINAL"));
        Assert.assertFalse("撤销后重发的 REVOKE 派生单不应写入", savedApprovalIds.contains("PROC-REISSUED"));
        Assert.assertTrue("窗口模式下作废原单既有快照应删除", deletedApprovalIds.contains("PROC-REVOKED-ORIGINAL"));
        Assert.assertTrue("窗口模式下 REVOKE 重发派生单既有快照也应删除", deletedApprovalIds.contains("PROC-REISSUED"));
    }

    @Test
    public void fetchMonthData_shouldUpdateExistingApprovalSnapshotWhenReFetchSameProcessInstance() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicReference<tbattendanceapprove> savedApproval = new AtomicReference<>();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        tbattendanceapprove existing = new tbattendanceapprove();
        existing.setId("PROC-OT-RANGE");
        existing.setUserId("ding-101");
        existing.setTagName("加班");
        existing.setBeginTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-04-24 10:46"));
        existing.setEndTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-04-17 18:00"));

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(existing);
                    }
                    if ("save".equals(method.getName())) {
                        savedApproval.set((tbattendanceapprove) args[0]);
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-OT");
        service.processIds = Collections.singletonList("PROC-OT-RANGE");
        service.processDetail = process("加班申请", "ding-101",
                component("开始时间", "[\"2026-04-17 17:30\",\"2026-04-17 18:00\"]"),
                component("时长", "0.5"));

        long changedCount = service.fetchMonthData(YearMonth.of(2026, 4), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("重抓同一审批实例应计入更新数", 1L, changedCount);
        Assert.assertEquals("应覆盖保存一次", 1, saveCount.get());
        Assert.assertNotNull("应保存更新后的审批快照", savedApproval.get());
        Assert.assertEquals(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-04-17 17:30"), savedApproval.get().getBeginTime());
        Assert.assertEquals(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-04-17 18:00"), savedApproval.get().getEndTime());
    }

    @Test
    public void fetchMonthData_shouldPreserveManuallyEditedSubtypeWhenReFetchSameProcessInstance() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicReference<tbattendanceapprove> savedApproval = new AtomicReference<>();
        AtomicInteger oldDeleteInvokeCount = new AtomicInteger();
        AtomicInteger staleDeleteInvokeCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        tbattendanceapprove existing = new tbattendanceapprove();
        existing.setId("PROC-LEAVE-0604");
        existing.setUserId("ding-101");
        existing.setTagName("请假");
        existing.setSubType("事假");
        existing.setBeginTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-06-04 16:02"));
        existing.setEndTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-06-04 20:19"));

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameIn".equals(method.getName())) {
                        oldDeleteInvokeCount.incrementAndGet();
                        return 1L;
                    }
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn".equals(method.getName())) {
                        staleDeleteInvokeCount.incrementAndGet();
                        Assert.assertEquals(Collections.singletonList("PROC-LEAVE-0604"), args[4]);
                        return 0;
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.of(existing);
                    }
                    if ("save".equals(method.getName())) {
                        savedApproval.set((tbattendanceapprove) args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-LEAVE");
        service.processIds = Collections.singletonList("PROC-LEAVE-0604");
        service.processDetail = process("请假审批", "ding-101",
                component("请假类型", "调休"),
                component("开始时间", "2026-06-04 16:02"),
                component("结束时间", "2026-06-04 20:19"),
                component("时长", "4小时"));

        long changedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("leave"));

        Assert.assertEquals("重抓同一审批实例应计入更新数", 1L, changedCount);
        Assert.assertEquals("重抓前不应再全量删除，否则人工子类型会丢失", 0, oldDeleteInvokeCount.get());
        Assert.assertEquals("重抓后应按本次返回实例清理陈旧快照", 1, staleDeleteInvokeCount.get());
        Assert.assertNotNull("应保存审批快照", savedApproval.get());
        Assert.assertEquals("本地人工修改后的子类型应跨同步保留", "事假", savedApproval.get().getSubType());
    }

    @Test
    public void fetchMonthData_shouldSkipApprovalWhenBusinessDateFallsOutsideSelectedMonth() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        AtomicInteger fetchMarkSaveCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        fetchMarkSaveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-OT");
        service.processIds = Collections.singletonList("PROC-OT-OUT-OF-MONTH");
        service.processDetail = process("加班申请", "ding-101",
                component("加班日期", "2026-03-31 13:30"),
                component("结束日期", "2026-03-31 17:30"),
                component("预计加班时长", "4"));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 4), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("业务日期不在所选月份内的审批不应入库", 0L, insertedCount);
        Assert.assertEquals("业务日期不在所选月份内的审批不应保存快照", 0, saveCount.get());
        Assert.assertEquals("抓取流程成功但审批业务日期全部越界时，仍应保留完成标记", 1, fetchMarkSaveCount.get());
    }

    /**
     * 核心 bug 回归：用户选目标月 4 月获取审批数据，但某张加班审批是 3 月底发起的、业务日期落在 3 月。
     * 发起窗口入口（带 fetchStartTime/fetchEndTime，模拟前端"选开始日期→点确定"）必须把它幂等落库，
     * 而不是像老业务月抓取那样按"业务日期≠目标月"丢弃——否则这类跨月单会永久丢失。
     */
    @Test
    public void fetchMonthData_windowMode_shouldPersistCrossMonthBusinessApproval() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        AtomicInteger fetchMarkSaveCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        fetchMarkSaveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-OT");
        // 审批单在发起窗口内被拉回，但其业务日期落在 3 月（不在目标月 4 月）
        service.processIds = Collections.singletonList("PROC-OT-CROSS-MONTH");
        service.processDetail = process("加班申请", "ding-101",
                component("加班日期", "2026-03-31 13:30"),
                component("结束日期", "2026-03-31 17:30"),
                component("预计加班时长", "4"));

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        long start = java.time.LocalDate.of(2026, 3, 1).atStartOfDay(zone).toInstant().toEpochMilli();
        long end = java.time.LocalDate.of(2026, 4, 30).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli();

        // 窗口入口：目标展示月 4 月，发起窗口覆盖 3/1~4/30
        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 4), start, end,
                Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("跨月发起窗口内的审批(业务日期落3月)必须落库，不得被目标月过滤丢弃", 1L, insertedCount);
        Assert.assertEquals("跨月审批快照应保存一次", 1, saveCount.get());
        Assert.assertEquals("跨月审批抓取完成仍应保留完成标记", 1, fetchMarkSaveCount.get());
    }

    @Test
    public void fetchMonthData_shouldDeleteSelectedEmployeeMonthDataBeforeReFetch() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicReference<java.util.Date> deletedBegin = new AtomicReference<>();
        AtomicReference<java.util.Date> deletedEnd = new AtomicReference<>();
        AtomicReference<List<String>> deletedUserIds = new AtomicReference<>();
        AtomicReference<List<String>> deletedTagNames = new AtomicReference<>();
        AtomicReference<List<String>> retainedApprovalIds = new AtomicReference<>();
        AtomicInteger staleDeleteInvokeCount = new AtomicInteger();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(101L);
        user.setUserId("ding-101");
        user.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(user);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn".equals(method.getName())) {
                        staleDeleteInvokeCount.incrementAndGet();
                        deletedBegin.set((java.util.Date) args[0]);
                        deletedEnd.set((java.util.Date) args[1]);
                        deletedUserIds.set((List<String>) args[2]);
                        deletedTagNames.set((List<String>) args[3]);
                        retainedApprovalIds.set((List<String>) args[4]);
                        return 2;
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-OT");
        service.processIds = Collections.singletonList("PROC-OT-APR");
        service.processDetail = process("加班申请", "ding-101",
                component("加班日期", "2026-04-17 17:30"),
                component("结束日期", "2026-04-17 18:30"),
                component("预计加班时长", "1"));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 4), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("重抓后应按员工+月份+审批类型清理陈旧数据", 1, staleDeleteInvokeCount.get());
        Assert.assertEquals(Collections.singletonList("ding-101"), deletedUserIds.get());
        Assert.assertEquals(Collections.singletonList("加班"), deletedTagNames.get());
        Assert.assertEquals(Collections.singletonList("PROC-OT-APR"), retainedApprovalIds.get());
        Assert.assertNotNull("删除开始时间不能为空", deletedBegin.get());
        Assert.assertNotNull("删除结束时间不能为空", deletedEnd.get());
        Assert.assertEquals("清理旧数据前应写入新审批快照", 1L, insertedCount);
        Assert.assertEquals("应保存新审批快照", 1, saveCount.get());
    }

    @Test
    public void fetchMonthData_shouldDeleteOnlyMatchedUserIdWhenEmployeesShareSameName() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicReference<List<String>> deletedUserIds = new AtomicReference<>();
        AtomicInteger staleDeleteInvokeCount = new AtomicInteger();

        tbattendanceuser targetUser = new tbattendanceuser();
        targetUser.setEmpId(101L);
        targetUser.setUserId("ding-101");
        targetUser.setUserName("张三");

        tbattendanceuser sameNameOtherUser = new tbattendanceuser();
        sameNameOtherUser.setEmpId(202L);
        sameNameOtherUser.setUserId("ding-202");
        sameNameOtherUser.setUserName("张三");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(targetUser, sameNameOtherUser);
                    }
                    if ("findAll".equals(method.getName())) {
                        return Arrays.asList(targetUser, sameNameOtherUser);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn".equals(method.getName())) {
                        staleDeleteInvokeCount.incrementAndGet();
                        deletedUserIds.set((List<String>) args[2]);
                        return 1;
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-LEAVE");
        service.processIds = Collections.singletonList("PROC-LEAVE-APR");
        service.processDetail = process("请假审批", "ding-101",
                component("请假类型", "事假"),
                component("开始时间", "2026-04-10 09:00"),
                component("结束时间", "2026-04-10 18:00"),
                component("时长", "1天"));

        service.fetchMonthData(YearMonth.of(2026, 4), Collections.singletonList(101L), Collections.singletonList("leave"));

        Assert.assertEquals("同名员工场景也应只清理一次目标员工旧数据", 1, staleDeleteInvokeCount.get());
        Assert.assertEquals("清理口径必须按 employeeId 映射出的 userId，而不能按同名 userName 扩散", Collections.singletonList("ding-101"), deletedUserIds.get());
    }

    @Test
    public void fetchMonthData_shouldDeleteStaleMonthDataWhenFetchingAllEmployees() throws Exception {
        TestableWorkflowSyncService service = new TestableWorkflowSyncService();
        AtomicReference<List<String>> deletedUserIds = new AtomicReference<>();
        AtomicReference<List<String>> deletedTagNames = new AtomicReference<>();
        AtomicReference<List<String>> retainedApprovalIds = new AtomicReference<>();
        AtomicInteger staleDeleteInvokeCount = new AtomicInteger();

        tbattendanceuser user1 = new tbattendanceuser();
        user1.setEmpId(101L);
        user1.setUserId("ding-101");
        user1.setUserName("张三");

        tbattendanceuser user2 = new tbattendanceuser();
        user2.setEmpId(102L);
        user2.setUserId("ding-102");
        user2.setUserName("李四");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return Arrays.asList(user1, user2);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn".equals(method.getName())) {
                        staleDeleteInvokeCount.incrementAndGet();
                        deletedUserIds.set((List<String>) args[2]);
                        deletedTagNames.set((List<String>) args[3]);
                        retainedApprovalIds.set((List<String>) args[4]);
                        return 1;
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processCodes = Collections.singletonList("PROC-CODE-OT");
        service.processIds = Collections.singletonList("PROC-OT-APR");
        service.processDetail = process("加班申请", "ding-101",
                component("加班日期", "2026-06-15 18:00"),
                component("结束日期", "2026-06-15 19:00"),
                component("预计加班时长", "1"));

        service.fetchMonthData(YearMonth.of(2026, 6), Collections.emptyList(), Collections.singletonList("overtime"));

        Assert.assertEquals("全员重抓也应清理本次未返回的陈旧审批快照", 1, staleDeleteInvokeCount.get());
        Assert.assertEquals(Arrays.asList("ding-101", "ding-102"), deletedUserIds.get());
        Assert.assertEquals(Collections.singletonList("加班"), deletedTagNames.get());
        Assert.assertEquals(Collections.singletonList("PROC-OT-APR"), retainedApprovalIds.get());
    }

    @Test
    public void fetchMonthData_shouldResolveProcessCodesPerEmployeeWhenVisibleTemplatesDiffer() throws Exception {
        PerEmployeeProcessCodeWorkflowSyncService service = new PerEmployeeProcessCodeWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user1 = new tbattendanceuser();
        user1.setEmpId(101L);
        user1.setUserId("ding-101");
        user1.setUserName("张三");

        tbattendanceuser user2 = new tbattendanceuser();
        user2.setEmpId(102L);
        user2.setUserId("ding-102");
        user2.setUserName("李四");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return Arrays.asList(user1, user2);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.emptyList(), Collections.singletonList("overtime"));

        Assert.assertEquals("每个员工应使用自己的流程编码抓取审批实例", 2L, insertedCount);
        Assert.assertEquals("应保存两个员工各自的审批快照", 2, saveCount.get());
        Assert.assertEquals(Arrays.asList("ding-101/PROC-OT-ding-101", "ding-102/PROC-OT-ding-102"), service.requestedUserAndProcessCodes);
    }

    @Test
    public void fetchMonthData_shouldResolveProcessCodesForEachEmployeeWhenFetchingAllEmployees() throws Exception {
        CountingProcessCodeWorkflowSyncService service = new CountingProcessCodeWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user1 = new tbattendanceuser();
        user1.setEmpId(101L);
        user1.setUserId("ding-101");
        user1.setUserName("张三");

        tbattendanceuser user2 = new tbattendanceuser();
        user2.setEmpId(102L);
        user2.setUserId("ding-102");
        user2.setUserName("李四");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return Arrays.asList(user1, user2);
                    }
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(user1, user2);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processDetail = process("请假审批", "ding-101",
                component("请假类型", "调休"),
                component("开始时间", "2026-05-02 09:00"),
                component("结束时间", "2026-05-02 18:00"),
                component("时长", "1天"));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 5), Collections.emptyList(), Collections.singletonList("leave"));

        Assert.assertEquals("两名员工各一条审批快照", 2L, insertedCount);
        Assert.assertEquals("应保存两条审批快照", 2, saveCount.get());
        Assert.assertEquals("全员抓取时流程编码应按员工解析，避免用第一个员工的可见模板漏抓其他员工审批", 2, service.resolveProcessCodesInvokeCount.get());
    }

    @Test
    public void fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees() throws Exception {
        MissingDingTalkUserWorkflowSyncService service = new MissingDingTalkUserWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();
        AtomicReference<List<String>> cleanupUserIds = new AtomicReference<>();
        AtomicReference<HrmAttendanceApprovalFetchMark> savedMark = new AtomicReference<>();

        tbattendanceuser invalidUser = new tbattendanceuser();
        invalidUser.setEmpId(101L);
        invalidUser.setUserId("missing-user");
        invalidUser.setUserName("已离职员工");

        tbattendanceuser validUser = new tbattendanceuser();
        validUser.setEmpId(102L);
        validUser.setUserId("ding-102");
        validUser.setUserName("李四");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return Arrays.asList(invalidUser, validUser);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn".equals(method.getName())
                            || "deleteByBeginTimeBetweenAndUserIdInAndTagNameIn".equals(method.getName())) {
                        cleanupUserIds.set((List<String>) args[2]);
                        return 0;
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        savedMark.set((HrmAttendanceApprovalFetchMark) args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.emptyList(), Collections.singletonList("overtime"));

        Assert.assertEquals("有效员工审批仍应继续抓取", 1L, insertedCount);
        Assert.assertEquals("只应保存有效员工审批快照", 1, saveCount.get());
        Assert.assertEquals(Arrays.asList("missing-user", "ding-102"), service.resolveProcessCodeUserIds);
        Assert.assertEquals("陈旧快照清理不能包含钉钉已不存在的员工", Collections.singletonList("ding-102"), cleanupUserIds.get());
        Assert.assertNotNull("只应为成功抓取员工写完成标记", savedMark.get());
        Assert.assertEquals("ding-102", savedMark.get().getUserId());
    }

    @Test
    public void fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched() throws Exception {
        MissingDingTalkUserWorkflowSyncService service = new MissingDingTalkUserWorkflowSyncService();

        tbattendanceuser invalidUser = new tbattendanceuser();
        invalidUser.setEmpId(101L);
        invalidUser.setUserId("missing-user");
        invalidUser.setUserName("已离职员工");

        setField(service, "tokenCreator", new StubAccessToken());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return Collections.singletonList(invalidUser);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        try {
            service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));
            Assert.fail("全部员工钉钉账号不存在时应给出明确提示");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("钉钉用户不存在"));
            Assert.assertTrue(ex.getMessage().contains("已离职员工/missing-user"));
        }
    }

    @Test
    public void fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee() throws Exception {
        MissingDingTalkUserWorkflowSyncService service = new MissingDingTalkUserWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser oldUser = new tbattendanceuser();
        oldUser.setId(1);
        oldUser.setEmpId(101L);
        oldUser.setUserId("missing-user");
        oldUser.setUserName("已离职员工");
        oldUser.setCreateTime(new Date(1000));

        tbattendanceuser newUser = new tbattendanceuser();
        newUser.setId(2);
        newUser.setEmpId(101L);
        newUser.setUserId("ding-101");
        newUser.setUserName("张三");
        newUser.setCreateTime(new Date(2000));

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(oldUser, newUser);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("单选员工应优先取最新有效映射", 1L, insertedCount);
        Assert.assertEquals("单选员工应仅保存一条审批快照", 1, saveCount.get());
        Assert.assertEquals(Collections.singletonList("ding-101"), service.resolveProcessCodeUserIds);
    }

    @Test
    public void fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing() throws Exception {
        EmployeeDingTalkMappingWorkflowSyncService service = new EmployeeDingTalkMappingWorkflowSyncService();
        AtomicReference<tbattendanceuser> savedMapping = new AtomicReference<>();
        AtomicInteger approvalSaveCount = new AtomicInteger();

        HrmEmployee employee = employee(101L, "张三", "13800000001", "ding-101");
        service.matchingUserIds.add("ding-101");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setFieldIfPresent(service, "hrmEmployeeRepository", employeeRepository(employee));
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.emptyList(), savedMapping));
        setField(service, "approvalRepository", approvalRepository(approvalSaveCount));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("员工表已有钉钉ID时应直接抓取", 1L, insertedCount);
        Assert.assertEquals("不应再调用钉钉花名册反查", 0, service.lookupCount.get());
        Assert.assertNotNull("应补齐tbattendanceuser映射", savedMapping.get());
        Assert.assertEquals("ding-101", savedMapping.get().getUserId());
        Assert.assertEquals(Long.valueOf(101L), savedMapping.get().getEmpId());
        Assert.assertEquals(1, approvalSaveCount.get());
    }

    @Test
    public void fetchMonthData_shouldRefreshExistingDingTalkUserIdWhenNameAndMobileMatchDifferentUser() throws Exception {
        EmployeeDingTalkMappingWorkflowSyncService service = new EmployeeDingTalkMappingWorkflowSyncService();
        AtomicReference<HrmEmployee> savedEmployee = new AtomicReference<>();
        AtomicReference<tbattendanceuser> savedMapping = new AtomicReference<>();
        AtomicInteger approvalSaveCount = new AtomicInteger();

        HrmEmployee employee = employee(101L, "王芳", "15871989405", "wrong-ding");
        service.lookupResult = Optional.of("ding-101");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setFieldIfPresent(service, "hrmEmployeeRepository", employeeRepository(employee, savedEmployee));
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.emptyList(), savedMapping));
        setField(service, "approvalRepository", approvalRepository(approvalSaveCount));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("刷新到正确钉钉ID后应继续抓取审批", 1L, insertedCount);
        Assert.assertEquals("已有钉钉ID也应校验姓名手机号", 1, service.lookupCount.get());
        Assert.assertNotNull("应写回员工表正确钉钉ID", savedEmployee.get());
        Assert.assertEquals("ding-101", savedEmployee.get().getDingtalkUserId());
        Assert.assertNotNull("应同步保存正确tbattendanceuser映射", savedMapping.get());
        Assert.assertEquals("ding-101", savedMapping.get().getUserId());
        Assert.assertEquals(Collections.singletonList("ding-101"), service.fetchInstanceUserIds);
        Assert.assertEquals(1, approvalSaveCount.get());
    }

    @Test
    public void fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt() throws Exception {
        EmployeeDingTalkMappingWorkflowSyncService service = new EmployeeDingTalkMappingWorkflowSyncService();
        AtomicReference<HrmEmployee> savedEmployee = new AtomicReference<>();
        AtomicReference<tbattendanceuser> savedMapping = new AtomicReference<>();
        AtomicInteger approvalSaveCount = new AtomicInteger();

        HrmEmployee employee = employee(101L, "张三", "13800000001", null);
        service.lookupResult = Optional.of("ding-101");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setFieldIfPresent(service, "hrmEmployeeRepository", employeeRepository(employee, savedEmployee));
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.emptyList(), savedMapping));
        setField(service, "approvalRepository", approvalRepository(approvalSaveCount));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("反查到钉钉ID后应继续抓取审批", 1L, insertedCount);
        Assert.assertEquals(1, service.lookupCount.get());
        Assert.assertNotNull("应写回员工表钉钉ID", savedEmployee.get());
        Assert.assertEquals("ding-101", savedEmployee.get().getDingtalkUserId());
        Assert.assertNotNull("应同步补齐tbattendanceuser映射", savedMapping.get());
        Assert.assertEquals("ding-101", savedMapping.get().getUserId());
        Assert.assertEquals(1, approvalSaveCount.get());
    }

    @Test
    public void fetchMonthData_shouldUseVerifiedAttendanceMappingWhenEmployeeDingTalkUserIdMissing() throws Exception {
        EmployeeDingTalkMappingWorkflowSyncService service = new EmployeeDingTalkMappingWorkflowSyncService();
        AtomicReference<HrmEmployee> savedEmployee = new AtomicReference<>();
        AtomicReference<tbattendanceuser> savedMapping = new AtomicReference<>();
        AtomicInteger approvalSaveCount = new AtomicInteger();

        HrmEmployee employee = employee(101L, "张三", "13800000001", null);
        tbattendanceuser existingMapping = new tbattendanceuser();
        existingMapping.setId(1);
        existingMapping.setEmpId(101L);
        existingMapping.setUserId("ding-101");
        existingMapping.setUserName("张三");
        existingMapping.setCreateTime(new Date(1000));
        service.matchingUserIds.add("ding-101");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setFieldIfPresent(service, "hrmEmployeeRepository", employeeRepository(employee, savedEmployee));
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.singletonList(existingMapping), savedMapping));
        setField(service, "approvalRepository", approvalRepository(approvalSaveCount));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 6), Collections.singletonList(101L), Collections.singletonList("overtime"));

        Assert.assertEquals("员工表缺钉钉ID时，应验证并复用已有考勤映射继续抓取", 1L, insertedCount);
        Assert.assertEquals("姓名手机号反查未命中后应回退校验本地映射", 1, service.lookupCount.get());
        Assert.assertNotNull("应把验证通过的钉钉ID回填员工表", savedEmployee.get());
        Assert.assertEquals("ding-101", savedEmployee.get().getDingtalkUserId());
        Assert.assertNotNull("应保存并刷新本地考勤映射", savedMapping.get());
        Assert.assertEquals("ding-101", savedMapping.get().getUserId());
        Assert.assertEquals(1, approvalSaveCount.get());
    }

    @Test
    public void fetchMonthData_shouldSkipEmployeeResolutionFailureWhenFetchingMultipleEmployees() throws Exception {
        EmployeeResolutionFailureWorkflowSyncService service = new EmployeeResolutionFailureWorkflowSyncService();
        AtomicInteger approvalSaveCount = new AtomicInteger();

        HrmEmployee failedEmployee = employee(101L, "张三", "13800000001", "ding-101");
        HrmEmployee validEmployee = employee(102L, "李四", "13800000002", "ding-102");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());
        setFieldIfPresent(service, "hrmEmployeeRepository", employeeRepository(Arrays.asList(failedEmployee, validEmployee)));
        setField(service, "attendanceUserRepository", attendanceUserRepository(Collections.emptyList(), new AtomicReference<>()));
        setField(service, "approvalRepository", approvalRepository(approvalSaveCount));
        setField(service, "fetchMarkRepository", fetchMarkRepository());

        long insertedCount = service.fetchMonthData(
                YearMonth.of(2026, 7),
                Arrays.asList(101L, 102L),
                Collections.singletonList("overtime")
        );

        Assert.assertEquals("员工预解析阶段单人异常不应中断整批", 1L, insertedCount);
        Assert.assertEquals("仍应继续抓取可解析员工审批", Collections.singletonList("ding-102"), service.fetchInstanceUserIds);
        Assert.assertEquals(1, approvalSaveCount.get());
    }

    @Test
    public void fetchMonthData_shouldDeduplicateAttendanceUsersWhenFetchingAllEmployees() throws Exception {
        UserDedupWorkflowSyncService service = new UserDedupWorkflowSyncService();
        AtomicInteger saveCount = new AtomicInteger();

        tbattendanceuser user1 = new tbattendanceuser();
        user1.setEmpId(101L);
        user1.setUserId("ding-101");
        user1.setUserName("张三");

        tbattendanceuser duplicateUser1 = new tbattendanceuser();
        duplicateUser1.setEmpId(101L);
        duplicateUser1.setUserId("ding-101");
        duplicateUser1.setUserName("张三");

        tbattendanceuser user2 = new tbattendanceuser();
        user2.setEmpId(102L);
        user2.setUserId("ding-102");
        user2.setUserName("李四");

        setField(service, "tokenCreator", new StubAccessToken());
        setField(service, "processInstanceParser", new HrmAttendanceApprovalProcessInstanceParser());

        Object userRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return Arrays.asList(user1, duplicateUser1, user2);
                    }
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(user1, duplicateUser1, user2);
                    }
                    return defaultValue(method.getReturnType());
                });

        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);

        service.processDetail = process("请假审批", "ding-101",
                component("请假类型", "调休"),
                component("开始时间", "2026-05-02 09:00"),
                component("结束时间", "2026-05-02 18:00"),
                component("时长", "1天"));

        long insertedCount = service.fetchMonthData(YearMonth.of(2026, 5), Collections.emptyList(), Collections.singletonList("leave"));

        Assert.assertEquals("重复的本地用户映射不应放大审批实例抓取", 2L, insertedCount);
        Assert.assertEquals("重复映射去重后只应保存两条审批快照", 2, saveCount.get());
        Assert.assertEquals("全员抓取应只按去重后的 userId 调用流程实例接口",
                Arrays.asList("ding-101", "ding-102"), service.requestedUserIds);
    }

    private static OapiProcessinstanceGetResponse.ProcessInstanceTopVo process(String title,
                                                                               String userId,
                                                                               OapiProcessinstanceGetResponse.FormComponentValueVo... components) {
        return processWithState(title, userId, "COMPLETED", "agree", components);
    }

    private static OapiProcessinstanceGetResponse.ProcessInstanceTopVo processWithState(String title,
                                                                                       String userId,
                                                                                       String status,
                                                                                       String result,
                                                                                       OapiProcessinstanceGetResponse.FormComponentValueVo... components) {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle(title);
        process.setOriginatorUserid(userId);
        process.setStatus(status);
        process.setResult(result);
        process.setCreateTime(new java.util.Date());
        process.setFinishTime(new java.util.Date());
        process.setFormComponentValues(Arrays.asList(components));
        return process;
    }

    private static OapiProcessinstanceGetResponse.ProcessInstanceTopVo processWithStateAndOperations(
            String title,
            String userId,
            String status,
            String result,
            java.util.List<OapiProcessinstanceGetResponse.OperationRecordsVo> operationRecords,
            OapiProcessinstanceGetResponse.FormComponentValueVo... components) {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = processWithState(title, userId, status, result, components);
        process.setOperationRecords(operationRecords);
        return process;
    }

    /** 构造"审批通过后被撤销替代的作废原单"：COMPLETED+agree 且无 TERMINATE 操作，但带 attachedProcessInstanceIds。 */
    private static OapiProcessinstanceGetResponse.ProcessInstanceTopVo revokedOriginalWithAttached(
            String title,
            String userId,
            String status,
            String result,
            java.util.List<OapiProcessinstanceGetResponse.OperationRecordsVo> operationRecords,
            java.util.List<String> attachedProcessInstanceIds,
            OapiProcessinstanceGetResponse.FormComponentValueVo... components) {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = processWithStateAndOperations(title, userId, status, result, operationRecords, components);
        process.setAttachedProcessInstanceIds(attachedProcessInstanceIds);
        return process;
    }

    /** 构造"撤销后重发的派生替身"(CfyM0j 型)：COMPLETED+agree、无 TERMINATE、attached 为空，仅 bizAction=REVOKE + mainProcessInstanceId 指回原单。
     *  用户定稿规则：此类单在钉钉 App 业务维度属"已撤销/撤销流程"（右上角标已撤销），即便单条接口报 agree 也不构成最终有效通过单 → 作废删。 */
    private static OapiProcessinstanceGetResponse.ProcessInstanceTopVo reissuedAfterRevoke(
            String title,
            String userId,
            String mainProcessInstanceId,
            OapiProcessinstanceGetResponse.FormComponentValueVo... components) {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = processWithStateAndOperations(title, userId, "COMPLETED", "agree",
                java.util.Arrays.asList(
                        operationRecord("EXECUTE_TASK_NORMAL"),
                        operationRecord("FINISH_PROCESS_INSTANCE")),
                components);
        process.setBizAction("REVOKE");
        process.setMainProcessInstanceId(mainProcessInstanceId);
        return process;
    }

    private static OapiProcessinstanceGetResponse.OperationRecordsVo operationRecord(String operationType) {
        OapiProcessinstanceGetResponse.OperationRecordsVo record = new OapiProcessinstanceGetResponse.OperationRecordsVo();
        record.setOperationType(operationType);
        return record;
    }

    private static OapiProcessinstanceGetResponse.FormComponentValueVo component(String name, String value) {
        OapiProcessinstanceGetResponse.FormComponentValueVo component = new OapiProcessinstanceGetResponse.FormComponentValueVo();
        component.setName(name);
        component.setValue(value);
        return component;
    }

    private static HrmEmployee employee(Long employeeId, String employeeName, String mobile, String dingTalkUserId) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(employeeName);
        employee.setMobile(mobile);
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setDingtalkUserId(dingTalkUserId);
        return employee;
    }

    private Object employeeRepository(HrmEmployee employee) {
        return employeeRepository(employee, new AtomicReference<>());
    }

    private Object employeeRepository(HrmEmployee employee, AtomicReference<HrmEmployee> savedEmployee) {
        return Proxy.newProxyInstance(
                hrmEmployeeRepository.class.getClassLoader(),
                new Class<?>[]{hrmEmployeeRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmployeeIdIn".equals(method.getName())) {
                        return Collections.singletonList(employee);
                    }
                    if ("findAll".equals(method.getName())) {
                        return Collections.singletonList(employee);
                    }
                    if ("save".equals(method.getName())) {
                        savedEmployee.set((HrmEmployee) args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object employeeRepository(List<HrmEmployee> employees) {
        return Proxy.newProxyInstance(
                hrmEmployeeRepository.class.getClassLoader(),
                new Class<?>[]{hrmEmployeeRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmployeeIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return employees;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object attendanceUserRepository(List<tbattendanceuser> mappings, AtomicReference<tbattendanceuser> savedMapping) {
        return Proxy.newProxyInstance(
                tbattendanceuserRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceuserRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName()) || "findAll".equals(method.getName())) {
                        return mappings;
                    }
                    if ("findAllByEmpId".equals(method.getName())) {
                        Long employeeId = (Long) args[0];
                        return mappings.stream()
                                .filter(mapping -> Objects.equals(mapping.getEmpId(), employeeId))
                                .collect(Collectors.toList());
                    }
                    if ("findFirstByUserId".equals(method.getName())) {
                        String userId = (String) args[0];
                        return mappings.stream()
                                .filter(mapping -> Objects.equals(mapping.getUserId(), userId))
                                .findFirst();
                    }
                    if ("save".equals(method.getName())) {
                        savedMapping.set((tbattendanceuser) args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object approvalRepository(AtomicInteger saveCount) {
        return Proxy.newProxyInstance(
                tbattendanceapproveRepository.class.getClassLoader(),
                new Class<?>[]{tbattendanceapproveRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    if ("deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn".equals(method.getName())
                            || "deleteByBeginTimeBetweenAndUserIdInAndTagNameIn".equals(method.getName())) {
                        return 0;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object fetchMarkRepository() {
        return Proxy.newProxyInstance(
                hrmAttendanceApprovalFetchMarkRepository.class.getClassLoader(),
                new Class<?>[]{hrmAttendanceApprovalFetchMarkRepository.class},
                (proxy, method, args) -> {
                    if ("existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType".equals(method.getName())) {
                        return false;
                    }
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = HrmAttendanceApprovalSyncServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setFieldIfPresent(Object target, String name, Object value) throws Exception {
        try {
            setField(target, name, value);
        } catch (NoSuchFieldException ignored) {
        }
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return null;
    }

    private static class TestableWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private List<String> processIds = Collections.emptyList();
        private List<String> processCodes = Collections.singletonList("PROC-CODE-LEAVE");
        private OapiProcessinstanceGetResponse.ProcessInstanceTopVo processDetail;

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return processCodes;
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            return processIds;
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            return processDetail;
        }
    }

    private static class ProcessCodeAwareWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private List<String> processCodes = Collections.emptyList();
        private java.util.Map<String, List<String>> processIdsByCode = new java.util.LinkedHashMap<>();
        private java.util.List<String> requestedProcessCodes = new java.util.ArrayList<>();
        private OapiProcessinstanceGetResponse.ProcessInstanceTopVo processDetail;

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return processCodes;
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            requestedProcessCodes.add(processCode);
            return processIdsByCode.getOrDefault(processCode, Collections.emptyList());
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            return processDetail;
        }
    }

    private static class InstanceStateWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private List<String> processIds = Collections.emptyList();
        private java.util.Map<String, OapiProcessinstanceGetResponse.ProcessInstanceTopVo> processDetails = new java.util.LinkedHashMap<>();

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return Collections.singletonList("PROC-CODE-OT");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            return processIds;
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            return processDetails.get(processInstanceId);
        }
    }

    private static class PermissionDeniedWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return Collections.singletonList("PROC-CODE-LEAVE");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            throw new IllegalStateException("Forbidden.AccessDenied.PermissionDenied: qyapi_aflow");
        }
    }

    private static class MisleadingTemplatePermissionWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            throw new IllegalStateException("ding talk error[subcode=60011,submsg=应用尚未开通所需的权限：[qyapi_dingpay_alipay], {requiredScopes=[qyapi_dingpay_alipay]}]");
        }
    }

    private static class CountingProcessCodeWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private final AtomicInteger resolveProcessCodesInvokeCount = new AtomicInteger();
        private OapiProcessinstanceGetResponse.ProcessInstanceTopVo processDetail;

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            resolveProcessCodesInvokeCount.incrementAndGet();
            return Collections.singletonList("PROC-CODE-LEAVE");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            return Collections.singletonList("PROC-" + userId);
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            OapiProcessinstanceGetResponse.ProcessInstanceTopVo copy = process(
                    processDetail.getTitle(),
                    processInstanceId.replace("PROC-", ""),
                    processDetail.getFormComponentValues().toArray(new OapiProcessinstanceGetResponse.FormComponentValueVo[0])
            );
            return copy;
        }
    }

    private static class PerEmployeeProcessCodeWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private final java.util.List<String> requestedUserAndProcessCodes = new java.util.ArrayList<>();

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return Collections.singletonList("PROC-OT-" + userId);
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            requestedUserAndProcessCodes.add(userId + "/" + processCode);
            if (processCode.equals("PROC-OT-" + userId)) {
                return Collections.singletonList("PROC-" + userId);
            }
            return Collections.emptyList();
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            String userId = processInstanceId.replace("PROC-", "");
            return process("加班申请", userId,
                    component("加班日期", "2026-06-15 18:00"),
                    component("结束日期", "2026-06-15 19:00"),
                    component("预计加班时长", "1"));
        }
    }

    private static class UserDedupWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private final java.util.List<String> requestedUserIds = new java.util.ArrayList<>();
        private OapiProcessinstanceGetResponse.ProcessInstanceTopVo processDetail;

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return Collections.singletonList("PROC-CODE-LEAVE");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            requestedUserIds.add(userId);
            return Collections.singletonList("PROC-" + userId);
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            return process(
                    processDetail.getTitle(),
                    processInstanceId.replace("PROC-", ""),
                    processDetail.getFormComponentValues().toArray(new OapiProcessinstanceGetResponse.FormComponentValueVo[0])
            );
        }
    }

    private static class EmployeeDingTalkMappingWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private final AtomicInteger lookupCount = new AtomicInteger();
        private final java.util.List<String> fetchInstanceUserIds = new java.util.ArrayList<>();
        private final java.util.Set<String> matchingUserIds = new java.util.HashSet<>();
        private Optional<String> lookupResult = Optional.empty();

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return Collections.singletonList("PROC-CODE-OT");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            fetchInstanceUserIds.add(userId);
            return Collections.singletonList("PROC-" + userId);
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            String userId = processInstanceId.replace("PROC-", "");
            return process("加班申请", userId,
                    component("加班日期", "2026-06-15 18:00"),
                    component("结束日期", "2026-06-15 19:00"),
                    component("预计加班时长", "1"));
        }

        @Override
        protected boolean isDingTalkUserIdMatchingEmployee(String token, String dingTalkUserId, HrmEmployee employee) {
            return matchingUserIds.contains(dingTalkUserId);
        }

        protected Optional<String> fetchDingTalkUserIdByNameAndMobile(String token, String employeeName, String mobile) {
            lookupCount.incrementAndGet();
            return lookupResult;
        }
    }

    private static class EmployeeResolutionFailureWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private final java.util.List<String> fetchInstanceUserIds = new java.util.ArrayList<>();

        @Override
        protected boolean isDingTalkUserIdMatchingEmployee(String token, String dingTalkUserId, HrmEmployee employee) {
            if ("ding-101".equals(dingTalkUserId)) {
                throw new IllegalStateException("topapi/smartwork/hrm/employee/v2/list调用失败, errmsg=isv.limitedFrequency");
            }
            return true;
        }

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            return Collections.singletonList("PROC-CODE-OT");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            fetchInstanceUserIds.add(userId);
            return Collections.singletonList("PROC-" + userId);
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            String userId = processInstanceId.replace("PROC-", "");
            return process("加班申请", userId,
                    component("加班日期", "2026-07-15 18:00"),
                    component("结束日期", "2026-07-15 19:00"),
                    component("预计加班时长", "1"));
        }
    }

    private static class MissingDingTalkUserWorkflowSyncService extends HrmAttendanceApprovalSyncServiceImpl {
        private final java.util.List<String> resolveProcessCodeUserIds = new java.util.ArrayList<>();

        @Override
        protected List<String> resolveProcessCodes(String token, String userId, List<String> approvalTypes) {
            resolveProcessCodeUserIds.add(userId);
            if ("missing-user".equals(userId)) {
                throw new IllegalStateException("topapi/process.listbyuserid调用失败, errcode=400023, errmsg=用户不存在");
            }
            return Collections.singletonList("PROC-CODE-OT");
        }

        @Override
        protected List<String> fetchProcessInstanceIds(String token, String userId, YearMonth month, String processCode) {
            return Collections.singletonList("PROC-" + userId);
        }

        @Override
        protected OapiProcessinstanceGetResponse.ProcessInstanceTopVo fetchProcessInstanceDetail(String token,
                                                                                                 String processInstanceId) {
            String userId = processInstanceId.replace("PROC-", "");
            return process("加班申请", userId,
                    component("加班日期", "2026-06-15 18:00"),
                    component("结束日期", "2026-06-15 19:00"),
                    component("预计加班时长", "1"));
        }
    }

    private static class StubAccessToken implements IAccessToken {
        @Override
        public String Refresh() {
            return "mock-token";
        }

        @Override
        public String Refresh(String CompanyID) {
            return "mock-token";
        }

        @Override
        public void EachCompany(java.util.function.Consumer<String> EachFun) {
        }

        @Override
        public String GetAdminUser(String CompanyID) {
            return null;
        }

        @Override
        public String GetMessageToken(String CompanyID) {
            return null;
        }
    }
}
