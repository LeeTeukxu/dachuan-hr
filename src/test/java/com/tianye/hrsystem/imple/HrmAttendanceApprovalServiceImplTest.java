package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.BasePage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class HrmAttendanceApprovalServiceImplTest {

    @Test
    public void queryPageList_shouldDelegateToMapperWithoutDingTalkDependency() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> mapperClass = loadClass("com.tianye.hrsystem.mapper.HrmAttendanceApprovalMapper");
        Assert.assertNotNull("审批数据Mapper未创建", mapperClass);

        Class<?> queryBOClass = loadClass("com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO");
        Assert.assertNotNull("审批数据分页查询BO未创建", queryBOClass);

        for (Field field : serviceClass.getDeclaredFields()) {
            String typeName = field.getType().getName().toLowerCase();
            Assert.assertFalse("审批数据查询服务严禁依赖钉钉类型", typeName.contains("dingtalk"));
            Assert.assertFalse("审批数据查询服务严禁依赖钉钉访问令牌", typeName.contains("accesstoken"));
        }

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        BasePage<Object> expectedPage = new BasePage<>(1L, 15L, 1L, Collections.emptyList());
        AtomicInteger invokeCount = new AtomicInteger();

        Object mapperProxy = Proxy.newProxyInstance(
                mapperClass.getClassLoader(),
                new Class<?>[]{mapperClass},
                (proxy, method, args) -> {
                    if ("queryPageList".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Assert.assertNotNull("分页对象不能为空", args[0]);
                        Assert.assertNotNull("查询条件不能为空", args[1]);
                        return expectedPage;
                    }
                    return null;
                });

        setField(service, "attendanceApprovalMapper", mapperProxy);

        Object queryBO = queryBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(queryBO, "setPage", Long.class, 1L);
        invokeSetter(queryBO, "setLimit", Long.class, 15L);
        invokeSetter(queryBO, "setTagNames", List.class, Arrays.asList("补卡", "请假"));
        invokeSetter(queryBO, "setSubTypes", List.class, Arrays.asList("事假", "调休"));

        Method queryMethod = serviceClass.getMethod("queryPageList", queryBOClass);
        Object actualPage = queryMethod.invoke(service, queryBO);

        Assert.assertEquals("服务必须只委托一次Mapper", 1, invokeCount.get());
        Assert.assertSame("服务必须直接返回Mapper分页结果", expectedPage, actualPage);
        Assert.assertEquals(Arrays.asList("补卡", "请假"), queryBOClass.getMethod("getTagNames").invoke(queryBO));
        Assert.assertEquals(Arrays.asList("事假", "调休"), queryBOClass.getMethod("getSubTypes").invoke(queryBO));
    }

    @Test
    public void checkMonthData_shouldReturnExistsFlagByRepositoryCount() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);

        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Assert.assertEquals("leave", args[3]);
                        return 5L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(101L, 102L, 103L, 104L, 105L);
                    }
                    return defaultValue(method.getReturnType());
                });
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-101"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-102"),
                                createAttendanceUser(attendanceUserClass, 103L, "ding-103"),
                                createAttendanceUser(attendanceUserClass, 104L, "ding-104"),
                                createAttendanceUser(attendanceUserClass, 105L, "ding-105")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);
        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("leave"));

        Method method = serviceClass.getMethod("checkMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("月份检查必须调用一次仓库计数", 1, invokeCount.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(Boolean.TRUE, ((Map<?, ?>) result).get("exists"));
        Assert.assertEquals(5L, ((Map<?, ?>) result).get("count"));
    }

    @Test
    public void checkMonthData_shouldUseSelectedEmployeesWhenEmployeeIdsProvided() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);

        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger employeeCountInvoke = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        employeeCountInvoke.incrementAndGet();
                        Assert.assertEquals(Arrays.asList("ding-1", "ding-2"), args[2]);
                        Assert.assertEquals("travel", args[3]);
                        return 2L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        Assert.assertEquals(Arrays.asList(101L, 102L), args[0]);
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-1"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-2")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        Assert.assertEquals(Arrays.asList(101L, 102L), args[0]);
                        return Arrays.asList(101L, 102L);
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);
        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setEmployeeIds", List.class, Arrays.asList(101L, 102L));
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("travel"));

        Method method = serviceClass.getMethod("checkMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("按员工判重时必须走员工范围计数", 1, employeeCountInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(Boolean.TRUE, ((Map<?, ?>) result).get("exists"));
        Assert.assertEquals(2L, ((Map<?, ?>) result).get("count"));
    }

    @Test
    public void checkMonthData_shouldReturnFalseWhenNotAllSelectedApprovalTypesCompleted() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);

        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger countInvoke = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        countInvoke.incrementAndGet();
                        if ("leave".equals(args[3])) {
                            return 2L;
                        }
                        if ("misscard".equals(args[3])) {
                            return 1L;
                        }
                        return 0L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-1"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-2")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(101L, 102L);
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);

        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("leave", "misscard"));

        Method method = serviceClass.getMethod("checkMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("月份检查必须按每个审批类型分别校验完成标记", 2, countInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(Boolean.FALSE, ((Map<?, ?>) result).get("exists"));
        Assert.assertEquals(3L, ((Map<?, ?>) result).get("count"));
    }

    @Test
    public void fetchMonthData_shouldCallSyncServiceWhenMonthNotFetched() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger countInvoke = new AtomicInteger();
        AtomicInteger syncInvoke = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        countInvoke.incrementAndGet();
                        return 0L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(101L, 102L);
                    }
                    if ("fetchMonthData".equals(method.getName())) {
                        syncInvoke.incrementAndGet();
                        Assert.assertEquals(Arrays.asList("overtime"), args[2]);
                        return 12L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-101"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-102")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);

        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("overtime"));

        Method method = serviceClass.getMethod("fetchMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("手工获取不再检查月份是否已存在", 0, countInvoke.get());
        Assert.assertEquals("月份未获取过时必须调用同步服务", 1, syncInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(Boolean.TRUE, ((Map<?, ?>) result).get("success"));
        Assert.assertEquals(12L, ((Map<?, ?>) result).get("insertedCount"));
    }

    @Test
    public void fetchMonthData_shouldPassSelectedEmployeeIdsToSyncService() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);

        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger syncInvoke = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        return 0L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 301L, "ding-301"),
                                createAttendanceUser(attendanceUserClass, 302L, "ding-302")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(301L, 302L);
                    }
                    if ("fetchMonthData".equals(method.getName())) {
                        syncInvoke.incrementAndGet();
                        Assert.assertEquals("2026-05", String.valueOf(args[0]));
                        Assert.assertEquals(Arrays.asList(301L, 302L), args[1]);
                        Assert.assertEquals(Arrays.asList("leave", "travel"), args[2]);
                        return 6L;
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);

        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setEmployeeIds", List.class, Arrays.asList(301L, 302L));
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("leave", "travel"));

        Method method = serviceClass.getMethod("fetchMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("按员工获取时必须调用一次同步服务", 1, syncInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(6L, ((Map<?, ?>) result).get("insertedCount"));
    }

    @Test
    public void fetchMonthData_shouldAllowRefetchWhenMonthAlreadyFetched_withoutRejecting() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger syncInvoke = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(101L, 102L);
                    }
                    if ("fetchMonthData".equals(method.getName())) {
                        syncInvoke.incrementAndGet();
                        return 9L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-101"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-102")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);

        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("overtime"));

        Method method = serviceClass.getMethod("fetchMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("月份已获取过时也应允许再次调用同步服务", 1, syncInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(9L, ((Map<?, ?>) result).get("insertedCount"));
    }

    @Test
    public void fetchMonthData_shouldAllowRefetchWhenMonthAlreadyFetched() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger syncInvoke = new AtomicInteger();

        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        return 2L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(101L, 102L);
                    }
                    if ("fetchMonthData".equals(method.getName())) {
                        syncInvoke.incrementAndGet();
                        return 9L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-101"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-102")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);

        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("overtime"));

        Method method = serviceClass.getMethod("fetchMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("即使月份已抓取过也应允许再次调用同步服务", 1, syncInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(9L, ((Map<?, ?>) result).get("insertedCount"));
    }

    @Test
    public void fetchMonthData_shouldAllowRefetchWhenLegacyDataExistsWithoutFetchMark() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> repositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", repositoryClass);

        Class<?> syncServiceClass = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService");
        Assert.assertNotNull("审批数据同步服务接口未创建", syncServiceClass);

        Class<?> fetchMarkRepositoryClass = loadClass("com.tianye.hrsystem.repository.hrmAttendanceApprovalFetchMarkRepository");
        Assert.assertNotNull("审批获取标记仓库未创建", fetchMarkRepositoryClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger syncInvoke = new AtomicInteger();

        Object repositoryProxy = Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if ("countByBeginTimeBetween".equals(method.getName())) {
                        return 1L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object fetchMarkRepositoryProxy = Proxy.newProxyInstance(
                fetchMarkRepositoryClass.getClassLoader(),
                new Class<?>[]{fetchMarkRepositoryClass},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    if ("countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType".equals(method.getName())) {
                        return 0L;
                    }
                    return defaultValue(method.getReturnType());
                });
        Object syncProxy = Proxy.newProxyInstance(
                syncServiceClass.getClassLoader(),
                new Class<?>[]{syncServiceClass},
                (proxy, method, args) -> {
                    if ("fetchMonthData".equals(method.getName())) {
                        syncInvoke.incrementAndGet();
                        Assert.assertEquals(Arrays.asList("misscard"), args[2]);
                        return 4L;
                    }
                    if ("resolveFetchTargetEmployeeIds".equals(method.getName())) {
                        return Arrays.asList(101L, 102L);
                    }
                    return defaultValue(method.getReturnType());
                });
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(
                                createAttendanceUser(attendanceUserClass, 101L, "ding-101"),
                                createAttendanceUser(attendanceUserClass, 102L, "ding-102")
                        );
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "approvalRepository", repositoryProxy);
        setField(service, "fetchMarkRepository", fetchMarkRepositoryProxy);
        setField(service, "approvalSyncService", syncProxy);
        setField(service, "attendanceUserRepository", userRepositoryProxy);

        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");
        invokeSetter(monthBO, "setApprovalTypes", List.class, Arrays.asList("misscard"));

        Method method = serviceClass.getMethod("fetchMonthData", monthBOClass);
        Object result = method.invoke(service, monthBO);

        Assert.assertEquals("存在旧数据但没有新版获取标记时应允许补抓", 1, syncInvoke.get());
        Assert.assertTrue("返回结果应为Map", result instanceof Map);
        Assert.assertEquals(4L, ((Map<?, ?>) result).get("insertedCount"));
    }

    @Test
    public void fetchMonthData_shouldRequireApprovalTypes() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        Object monthBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(monthBO, "setMonth", String.class, "2026-05");

        Method method = serviceClass.getMethod("fetchMonthData", monthBOClass);
        try {
            method.invoke(service, monthBO);
            Assert.fail("未选择审批类型时应抛出异常");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            Assert.assertTrue("应返回请选择审批类型的中文提示", String.valueOf(cause.getMessage()).contains("请选择审批类型"));
        }
    }

    @Test
    public void updateStatisticsStatus_shouldMarkApprovalAsCancelledOrIncludedInStatistics() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> updateBOClass = loadClass("com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalStatisticsStatusBO");
        Assert.assertNotNull("审批统计状态更新BO未创建", updateBOClass);

        Class<?> approvalRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", approvalRepositoryClass);

        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据模型未创建", approvalClass);
        Assert.assertNotNull("审批数据模型必须提供统计状态getter", approvalClass.getMethod("getStatisticsStatus"));
        Assert.assertNotNull("审批数据模型必须提供统计状态setter", approvalClass.getMethod("setStatisticsStatus", String.class));

        Object approval = approvalClass.getDeclaredConstructor().newInstance();
        approvalClass.getMethod("setId", String.class).invoke(approval, "PROC-OT-001");

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger saveCount = new AtomicInteger();
        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                approvalRepositoryClass.getClassLoader(),
                new Class<?>[]{approvalRepositoryClass},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return java.util.Optional.of(approval);
                    }
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });
        setField(service, "approvalRepository", approvalRepositoryProxy);

        Object cancelBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(cancelBO, "setApprovalId", String.class, "PROC-OT-001");
        invokeSetter(cancelBO, "setStatisticsStatus", String.class, "取消至统计");

        Method method = serviceClass.getMethod("updateStatisticsStatus", updateBOClass);
        Object cancelResult = method.invoke(service, cancelBO);

        Assert.assertTrue(cancelResult instanceof Map);
        Assert.assertEquals("取消至统计", ((Map<?, ?>) cancelResult).get("statisticsStatus"));
        Assert.assertEquals("取消至统计", approvalClass.getMethod("getStatisticsStatus").invoke(approval));

        Object includeBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(includeBO, "setApprovalId", String.class, "PROC-OT-001");
        invokeSetter(includeBO, "setStatisticsStatus", String.class, "");

        Object includeResult = method.invoke(service, includeBO);

        Assert.assertTrue(includeResult instanceof Map);
        Assert.assertEquals("", ((Map<?, ?>) includeResult).get("statisticsStatus"));
        Assert.assertEquals("", approvalClass.getMethod("getStatisticsStatus").invoke(approval));
        Assert.assertEquals("两次状态更新都应保存本地审批快照", 2, saveCount.get());
    }

    @Test
    public void queryPageList_shouldExposeStatisticsStatusForOperationColumn() throws Exception {
        Class<?> voClass = loadClass("com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO");
        Assert.assertNotNull("审批数据分页VO未创建", voClass);
        Assert.assertNotNull("审批数据分页VO必须返回统计状态", voClass.getMethod("getStatisticsStatus"));
        Assert.assertNotNull("审批数据分页VO必须支持设置统计状态", voClass.getMethod("setStatisticsStatus", String.class));

        String mapperXml = new String(
                java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/mapper/HrmAttendanceApprovalMapper.xml")),
                java.nio.charset.StandardCharsets.UTF_8
        );
        Assert.assertTrue("审批数据列表SQL必须查询 statisticsStatus 供操作列判断",
                mapperXml.contains("a.statisticsStatus as statisticsStatus"));
    }

    @Test
    public void queryPageList_shouldExposeEmployeeIdAndMobileForRowManualAdd() throws Exception {
        Class<?> voClass = loadClass("com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO");
        Assert.assertNotNull("审批数据分页VO未创建", voClass);
        Assert.assertNotNull("审批数据分页VO必须返回员工ID，供行内添加审批数据精确入库", voClass.getMethod("getEmployeeId"));
        Assert.assertNotNull("审批数据分页VO必须支持设置员工ID", voClass.getMethod("setEmployeeId", Long.class));
        Assert.assertNotNull("审批数据分页VO必须返回手机号，供同名员工人工识别", voClass.getMethod("getMobile"));
        Assert.assertNotNull("审批数据分页VO必须支持设置手机号", voClass.getMethod("setMobile", String.class));

        String mapperXml = new String(
                java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/mapper/HrmAttendanceApprovalMapper.xml")),
                java.nio.charset.StandardCharsets.UTF_8
        );
        Assert.assertTrue("审批数据列表SQL必须查询员工ID，避免行内添加按姓名猜测员工",
                mapperXml.contains("e.employee_id as employeeId"));
        Assert.assertTrue("审批数据列表SQL必须查询手机号，支持同名员工姓名+手机号识别",
                mapperXml.contains("e.mobile as mobile"));
    }

    @Test
    public void querySubtypeOptions_shouldMergeDefaultOptionsAndExistingSubTypes() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> repositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", repositoryClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        Object repositoryProxy = Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if ("findDistinctSubTypes".equals(method.getName())) {
                        return Arrays.asList("调休", "哺乳假", "", null, "病假");
                    }
                    return defaultValue(method.getReturnType());
                });
        setField(service, "approvalRepository", repositoryProxy);

        Method method = serviceClass.getMethod("querySubtypeOptions");
        Object result = method.invoke(service);

        Assert.assertTrue("子类型选项应返回List", result instanceof List);
        List<?> options = (List<?>) result;
        Assert.assertTrue("必须包含默认调休选项", options.contains("调休"));
        Assert.assertTrue("必须包含默认事假选项", options.contains("事假"));
        Assert.assertTrue("必须合并数据库已有特殊子类型", options.contains("哺乳假"));
        Assert.assertFalse("空子类型不应进入下拉选项", options.contains(""));
    }

    @Test
    public void updateSubtype_shouldPersistLocalApprovalSubtype() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> repositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", repositoryClass);

        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        Class<?> updateBOClass = loadClass("com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalSubtypeBO");
        Assert.assertNotNull("审批子类型更新BO未创建", updateBOClass);

        Object approval = approvalClass.getDeclaredConstructor().newInstance();
        invokeSetter(approval, "setId", String.class, "PROC-LEAVE-0604");
        invokeSetter(approval, "setSubType", String.class, "调休");

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger saveInvoke = new AtomicInteger();
        Object repositoryProxy = Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Assert.assertEquals("PROC-LEAVE-0604", args[0]);
                        return java.util.Optional.of(approval);
                    }
                    if ("save".equals(method.getName())) {
                        saveInvoke.incrementAndGet();
                        Assert.assertEquals("事假", approvalClass.getMethod("getSubType").invoke(args[0]));
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });
        setField(service, "approvalRepository", repositoryProxy);

        Object updateBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(updateBO, "setApprovalId", String.class, "PROC-LEAVE-0604");
        invokeSetter(updateBO, "setSubType", String.class, "事假");

        Method method = serviceClass.getMethod("updateSubtype", updateBOClass);
        Object result = method.invoke(service, updateBO);

        Assert.assertEquals("审批子类型应保存一次", 1, saveInvoke.get());
        Assert.assertTrue("更新结果应为Map", result instanceof Map);
        Assert.assertEquals("PROC-LEAVE-0604", ((Map<?, ?>) result).get("approvalId"));
        Assert.assertEquals("事假", ((Map<?, ?>) result).get("subType"));
    }

    @Test
    public void updateDuration_shouldPersistLocalApprovalDurationAndUnit() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> repositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", repositoryClass);

        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        Class<?> updateBOClass = loadClass("com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalDurationBO");
        Assert.assertNotNull("审批时长更新BO未创建", updateBOClass);

        Object approval = approvalClass.getDeclaredConstructor().newInstance();
        invokeSetter(approval, "setId", String.class, "PROC-OT-001");
        invokeSetter(approval, "setDuration", String.class, "51");
        invokeSetter(approval, "setDurationUnit", String.class, "小时");

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger saveInvoke = new AtomicInteger();
        Object repositoryProxy = Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Assert.assertEquals("PROC-OT-001", args[0]);
                        return java.util.Optional.of(approval);
                    }
                    if ("save".equals(method.getName())) {
                        saveInvoke.incrementAndGet();
                        Assert.assertEquals("51", approvalClass.getMethod("getDuration").invoke(args[0]));
                        Assert.assertEquals("小时", approvalClass.getMethod("getDurationUnit").invoke(args[0]));
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });
        setField(service, "approvalRepository", repositoryProxy);

        Object updateBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(updateBO, "setApprovalId", String.class, "PROC-OT-001");
        invokeSetter(updateBO, "setDuration", String.class, "51");
        invokeSetter(updateBO, "setDurationUnit", String.class, "小时");

        Method method = serviceClass.getMethod("updateDuration", updateBOClass);
        Object result = method.invoke(service, updateBO);

        Assert.assertEquals("审批时长应保存一次", 1, saveInvoke.get());
        Assert.assertTrue("更新结果应为Map", result instanceof Map);
        Assert.assertEquals("PROC-OT-001", ((Map<?, ?>) result).get("approvalId"));
        Assert.assertEquals("51", ((Map<?, ?>) result).get("duration"));
        Assert.assertEquals("小时", ((Map<?, ?>) result).get("durationUnit"));
    }

    @Test
    public void addManualApproval_shouldPersistLocalApprovalForEachSelectedEmployee() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> addBOClass = loadClass("com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO");
        Assert.assertNotNull("手工添加审批数据BO未创建", addBOClass);

        Class<?> approvalRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", approvalRepositoryClass);

        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);

        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);

        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date beginTime = format.parse("2026-07-15 09:00:00");
        java.util.Date endTime = format.parse("2026-07-15 13:00:00");
        java.util.Date beforeSave = new java.util.Date();
        AtomicInteger saveCount = new AtomicInteger();

        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        Assert.assertEquals(Arrays.asList(101L, 102L), args[0]);
                        Object user1 = createAttendanceUser(attendanceUserClass, 101L, "ding-101");
                        Object user2 = createAttendanceUser(attendanceUserClass, 102L, "ding-102");
                        invokeSetter(user1, "setGroupId", Long.class, 9001L);
                        invokeSetter(user2, "setGroupId", Long.class, 9002L);
                        return Arrays.asList(user1, user2);
                    }
                    return defaultValue(method.getReturnType());
                });
        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                approvalRepositoryClass.getClassLoader(),
                new Class<?>[]{approvalRepositoryClass},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        saveCount.incrementAndGet();
                        Object approval = args[0];
                        Assert.assertTrue("手工审批ID必须使用本地前缀避免和钉钉流程ID冲突",
                                String.valueOf(approvalClass.getMethod("getId").invoke(approval)).startsWith("MANUAL-"));
                        Assert.assertEquals("请假", approvalClass.getMethod("getTagName").invoke(approval));
                        Assert.assertEquals("调休", approvalClass.getMethod("getSubType").invoke(approval));
                        Assert.assertEquals(3L, approvalClass.getMethod("getBizType").invoke(approval));
                        Assert.assertEquals(beginTime, approvalClass.getMethod("getBeginTime").invoke(approval));
                        Assert.assertEquals(endTime, approvalClass.getMethod("getEndTime").invoke(approval));
                        Assert.assertEquals("4", approvalClass.getMethod("getDuration").invoke(approval));
                        Assert.assertEquals("小时", approvalClass.getMethod("getDurationUnit").invoke(approval));
                        Assert.assertEquals("2026-07-15", new java.text.SimpleDateFormat("yyyy-MM-dd")
                                .format((java.util.Date) approvalClass.getMethod("getWorkDate").invoke(approval)));
                        java.util.Date createTime = (java.util.Date) approvalClass.getMethod("getCreateTime").invoke(approval);
                        Assert.assertNotNull("保存时间必须由服务器生成", createTime);
                        Assert.assertFalse("保存时间不能早于调用前时间", createTime.before(beforeSave));
                        return approval;
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        Object addBO = addBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(addBO, "setEmployeeIds", List.class, Arrays.asList(101L, 102L));
        invokeSetter(addBO, "setTagName", String.class, "请假");
        invokeSetter(addBO, "setSubType", String.class, "调休");
        invokeSetter(addBO, "setBeginTime", java.util.Date.class, beginTime);
        invokeSetter(addBO, "setEndTime", java.util.Date.class, endTime);

        Method method = serviceClass.getMethod("addManualApproval", addBOClass);
        Object result = method.invoke(service, addBO);

        Assert.assertEquals("每个选中员工都应保存一条审批快照", 2, saveCount.get());
        Assert.assertTrue("添加结果应为Map", result instanceof Map);
        Assert.assertEquals(2, ((Map<?, ?>) result).get("insertedCount"));
        Assert.assertEquals("4", ((Map<?, ?>) result).get("duration"));
        Assert.assertEquals("小时", ((Map<?, ?>) result).get("durationUnit"));
        Assert.assertNotNull("添加结果应返回服务器保存时间", ((Map<?, ?>) result).get("createTime"));
    }

    @Test
    public void addManualApproval_shouldPersistEachSelectedApprovalRangeForEachEmployee() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> addBOClass = loadClass("com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO");
        Assert.assertNotNull("手工添加审批数据BO未创建", addBOClass);
        Assert.assertNotNull("手工添加审批数据BO必须支持多日期审批时段", findMethod(addBOClass, "getApprovalRanges"));
        Assert.assertNotNull("手工添加审批数据BO必须支持设置多日期审批时段",
                findSetter(addBOClass, "setApprovalRanges", List.class));

        Class<?> rangeBOClass = loadClass("com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO$ApprovalRangeBO");
        Assert.assertNotNull("多日期审批时段BO未创建", rangeBOClass);
        Assert.assertNotNull("多日期审批时段BO必须提供开始时间", findMethod(rangeBOClass, "getBeginTime"));
        Assert.assertNotNull("多日期审批时段BO必须提供结束时间", findMethod(rangeBOClass, "getEndTime"));

        Class<?> approvalRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据仓库未创建", approvalRepositoryClass);
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Object range13 = rangeBOClass.getDeclaredConstructor().newInstance();
        Object range22 = rangeBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(range13, "setBeginTime", java.util.Date.class, format.parse("2026-07-13 08:00:00"));
        invokeSetter(range13, "setEndTime", java.util.Date.class, format.parse("2026-07-13 16:00:00"));
        invokeSetter(range22, "setBeginTime", java.util.Date.class, format.parse("2026-07-22 08:00:00"));
        invokeSetter(range22, "setEndTime", java.util.Date.class, format.parse("2026-07-22 16:00:00"));

        java.util.List<Object> savedApprovals = new java.util.ArrayList<>();
        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        Assert.assertEquals(Arrays.asList(101L), args[0]);
                        Object user = createAttendanceUser(attendanceUserClass, 101L, "ding-101");
                        invokeSetter(user, "setGroupId", Long.class, 9001L);
                        return Arrays.asList(user);
                    }
                    return defaultValue(method.getReturnType());
                });
        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                approvalRepositoryClass.getClassLoader(),
                new Class<?>[]{approvalRepositoryClass},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        Object approval = args[0];
                        savedApprovals.add(approval);
                        Assert.assertEquals("请假", approvalClass.getMethod("getTagName").invoke(approval));
                        Assert.assertEquals("调休", approvalClass.getMethod("getSubType").invoke(approval));
                        Assert.assertEquals("8", approvalClass.getMethod("getDuration").invoke(approval));
                        Assert.assertEquals("小时", approvalClass.getMethod("getDurationUnit").invoke(approval));
                        Assert.assertEquals("ding-101", approvalClass.getMethod("getUserId").invoke(approval));
                        return approval;
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        Object addBO = addBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(addBO, "setEmployeeIds", List.class, Arrays.asList(101L));
        invokeSetter(addBO, "setTagName", String.class, "请假");
        invokeSetter(addBO, "setSubType", String.class, "调休");
        invokeSetter(addBO, "setApprovalRanges", List.class, Arrays.asList(range13, range22));

        Method method = serviceClass.getMethod("addManualApproval", addBOClass);
        Object result = method.invoke(service, addBO);

        Assert.assertEquals("一个员工两段日期应保存两条审批快照", 2, savedApprovals.size());
        Assert.assertEquals("2026-07-13", dayFormat.format((java.util.Date) approvalClass.getMethod("getBeginTime").invoke(savedApprovals.get(0))));
        Assert.assertEquals("2026-07-13", dayFormat.format((java.util.Date) approvalClass.getMethod("getWorkDate").invoke(savedApprovals.get(0))));
        Assert.assertEquals("2026-07-22", dayFormat.format((java.util.Date) approvalClass.getMethod("getBeginTime").invoke(savedApprovals.get(1))));
        Assert.assertEquals("2026-07-22", dayFormat.format((java.util.Date) approvalClass.getMethod("getWorkDate").invoke(savedApprovals.get(1))));
        Assert.assertTrue("添加结果应为Map", result instanceof Map);
        Assert.assertEquals(2, ((Map<?, ?>) result).get("insertedCount"));
        Assert.assertEquals(2, ((Map<?, ?>) result).get("rangeCount"));
    }

    @Test
    public void addManualApproval_shouldPersistManualDurationWhenProvided() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> addBOClass = loadClass("com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO");
        Assert.assertNotNull("手工添加审批数据BO未创建", addBOClass);
        Assert.assertNotNull("手工添加审批数据BO必须支持手工合计时长", findMethod(addBOClass, "getDuration"));
        Assert.assertNotNull("手工添加审批数据BO必须支持设置手工合计时长",
                findSetter(addBOClass, "setDuration", String.class));
        Assert.assertNotNull("手工添加审批数据BO必须支持手工时长单位", findMethod(addBOClass, "getDurationUnit"));
        Assert.assertNotNull("手工添加审批数据BO必须支持设置手工时长单位",
                findSetter(addBOClass, "setDurationUnit", String.class));

        Class<?> approvalRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据仓库未创建", approvalRepositoryClass);
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        AtomicReference<Object> savedApproval = new AtomicReference<>();

        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        Object user = createAttendanceUser(attendanceUserClass, 101L, "ding-101");
                        invokeSetter(user, "setGroupId", Long.class, 9001L);
                        return Arrays.asList(user);
                    }
                    return defaultValue(method.getReturnType());
                });
        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                approvalRepositoryClass.getClassLoader(),
                new Class<?>[]{approvalRepositoryClass},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        savedApproval.set(args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        Object addBO = addBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(addBO, "setEmployeeIds", List.class, Arrays.asList(101L));
        invokeSetter(addBO, "setTagName", String.class, "请假");
        invokeSetter(addBO, "setSubType", String.class, "调休");
        invokeSetter(addBO, "setBeginTime", java.util.Date.class, format.parse("2026-07-15 09:00:00"));
        invokeSetter(addBO, "setEndTime", java.util.Date.class, format.parse("2026-07-15 13:00:00"));
        invokeSetter(addBO, "setDuration", String.class, "3.5");
        invokeSetter(addBO, "setDurationUnit", String.class, "小时");

        Method method = serviceClass.getMethod("addManualApproval", addBOClass);
        Object result = method.invoke(service, addBO);

        Assert.assertNotNull("应保存一条手工审批快照", savedApproval.get());
        Assert.assertEquals("手工添加必须优先使用前端传入的合计时长",
                "3.5", approvalClass.getMethod("getDuration").invoke(savedApproval.get()));
        Assert.assertEquals("小时", approvalClass.getMethod("getDurationUnit").invoke(savedApproval.get()));
        Assert.assertTrue("添加结果应为Map", result instanceof Map);
        Assert.assertEquals("3.5", ((Map<?, ?>) result).get("duration"));
        Assert.assertEquals("小时", ((Map<?, ?>) result).get("durationUnit"));
    }

    @Test
    public void addManualApproval_shouldUseLatestAttendanceUserMappingWhenEmployeeHasHistory() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> addBOClass = loadClass("com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO");
        Class<?> approvalRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Class<?> userRepositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceuserRepository");
        Class<?> attendanceUserClass = loadClass("com.tianye.hrsystem.model.tbattendanceuser");
        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("手工添加审批数据BO未创建", addBOClass);
        Assert.assertNotNull("审批数据仓库未创建", approvalRepositoryClass);
        Assert.assertNotNull("考勤用户仓库未创建", userRepositoryClass);
        Assert.assertNotNull("考勤用户模型未创建", attendanceUserClass);
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Object oldUser = createAttendanceUser(attendanceUserClass, 101L, "ding-old");
        invokeSetter(oldUser, "setId", Integer.class, 1);
        invokeSetter(oldUser, "setGroupId", Long.class, 8001L);
        invokeSetter(oldUser, "setCreateTime", java.util.Date.class, format.parse("2026-07-01 09:00:00"));
        Object latestUser = createAttendanceUser(attendanceUserClass, 101L, "ding-new");
        invokeSetter(latestUser, "setId", Integer.class, 2);
        invokeSetter(latestUser, "setGroupId", Long.class, 9001L);
        invokeSetter(latestUser, "setCreateTime", java.util.Date.class, format.parse("2026-07-10 09:00:00"));
        AtomicReference<Object> savedApproval = new AtomicReference<>();

        Object userRepositoryProxy = Proxy.newProxyInstance(
                userRepositoryClass.getClassLoader(),
                new Class<?>[]{userRepositoryClass},
                (proxy, method, args) -> {
                    if ("findAllByEmpIdIn".equals(method.getName())) {
                        return Arrays.asList(oldUser, latestUser);
                    }
                    return defaultValue(method.getReturnType());
                });
        Object approvalRepositoryProxy = Proxy.newProxyInstance(
                approvalRepositoryClass.getClassLoader(),
                new Class<?>[]{approvalRepositoryClass},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        savedApproval.set(args[0]);
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                });

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        setField(service, "attendanceUserRepository", userRepositoryProxy);
        setField(service, "approvalRepository", approvalRepositoryProxy);

        Object addBO = addBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(addBO, "setEmployeeIds", List.class, Arrays.asList(101L));
        invokeSetter(addBO, "setTagName", String.class, "请假");
        invokeSetter(addBO, "setSubType", String.class, "调休");
        invokeSetter(addBO, "setBeginTime", java.util.Date.class, format.parse("2026-07-15 09:00:00"));
        invokeSetter(addBO, "setEndTime", java.util.Date.class, format.parse("2026-07-15 13:00:00"));

        Method method = serviceClass.getMethod("addManualApproval", addBOClass);
        method.invoke(service, addBO);

        Assert.assertNotNull("应保存一条手工审批快照", savedApproval.get());
        Assert.assertEquals("手工添加必须使用该员工最新考勤映射，避免历史userId参与统计",
                "ding-new", approvalClass.getMethod("getUserId").invoke(savedApproval.get()));
        Assert.assertEquals(9001L, approvalClass.getMethod("getGroupId").invoke(savedApproval.get()));
    }

    @Test
    public void deleteApproval_shouldDeleteOnlyTheApprovalIdToAvoidSameNameEmployeeMisdelete() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> deleteBOClass = loadClass("com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO");
        Assert.assertNotNull("删除审批数据BO未创建", deleteBOClass);
        Assert.assertNotNull("删除审批数据BO必须提供审批ID", deleteBOClass.getMethod("getApprovalId"));
        Assert.assertNotNull("删除审批数据BO必须支持设置审批ID", deleteBOClass.getMethod("setApprovalId", String.class));
        Assert.assertNull("删除审批数据请求不应包含员工姓名，避免同名员工误删", findMethod(deleteBOClass, "getEmployeeName"));
        Assert.assertNull("删除审批数据请求不应包含部门名称，避免同名员工误删", findMethod(deleteBOClass, "getDeptName"));

        Class<?> repositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", repositoryClass);

        Class<?> approvalClass = loadClass("com.tianye.hrsystem.model.tbattendanceapprove");
        Assert.assertNotNull("审批数据模型未创建", approvalClass);

        Object approval = approvalClass.getDeclaredConstructor().newInstance();
        invokeSetter(approval, "setId", String.class, "PROC-SAME-NAME-001");
        invokeSetter(approval, "setUserId", String.class, "ding-same-name-a");

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        AtomicInteger findByIdCount = new AtomicInteger();
        AtomicInteger deleteCount = new AtomicInteger();
        Object repositoryProxy = Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        findByIdCount.incrementAndGet();
                        Assert.assertEquals("必须只按审批主键查询待删除记录", "PROC-SAME-NAME-001", args[0]);
                        return java.util.Optional.of(approval);
                    }
                    if ("delete".equals(method.getName())) {
                        deleteCount.incrementAndGet();
                        Assert.assertSame("必须删除findById查到的同一条审批实体", approval, args[0]);
                        return null;
                    }
                    if (method.getName().startsWith("deleteBy") || method.getName().startsWith("deleteAllBy")) {
                        Assert.fail("删除审批数据不得按员工、部门、时间或类型批量删除: " + method.getName());
                    }
                    return defaultValue(method.getReturnType());
                });
        setField(service, "approvalRepository", repositoryProxy);

        Object deleteBO = deleteBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(deleteBO, "setApprovalId", String.class, "PROC-SAME-NAME-001");

        Method method = serviceClass.getMethod("deleteApproval", deleteBOClass);
        Object result = method.invoke(service, deleteBO);

        Assert.assertEquals("必须只按审批ID查询一次", 1, findByIdCount.get());
        Assert.assertEquals("必须只删除一条审批数据", 1, deleteCount.get());
        Assert.assertTrue("删除结果应为Map", result instanceof Map);
        Assert.assertEquals("PROC-SAME-NAME-001", ((Map<?, ?>) result).get("approvalId"));
        Assert.assertEquals(Boolean.TRUE, ((Map<?, ?>) result).get("deleted"));
    }

    @Test
    public void deleteApproval_shouldRequireApprovalId() throws Exception {
        Class<?> serviceClass = loadClass("com.tianye.hrsystem.imple.HrmAttendanceApprovalServiceImpl");
        Assert.assertNotNull("审批数据服务实现未创建", serviceClass);

        Class<?> deleteBOClass = loadClass("com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO");
        Assert.assertNotNull("删除审批数据BO未创建", deleteBOClass);

        Object service = serviceClass.getDeclaredConstructor().newInstance();
        Object deleteBO = deleteBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(deleteBO, "setApprovalId", String.class, "  ");

        Method method = serviceClass.getMethod("deleteApproval", deleteBOClass);
        try {
            method.invoke(service, deleteBO);
            Assert.fail("删除审批数据缺少审批ID时应抛出异常");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            Assert.assertTrue("应返回审批数据ID不能为空的中文提示",
                    String.valueOf(cause.getMessage()).contains("审批数据ID不能为空"));
        }
    }

    @Test
    public void deleteSelectedMonthDataRepositoryMethod_shouldDeclareTransactionalBoundary() throws Exception {
        Class<?> repositoryClass = loadClass("com.tianye.hrsystem.repository.tbattendanceapproveRepository");
        Assert.assertNotNull("审批数据仓库未创建", repositoryClass);

        Method deleteMethod = repositoryClass.getMethod(
                "deleteByBeginTimeBetweenAndUserIdInAndTagNameIn",
                java.util.Date.class,
                java.util.Date.class,
                List.class,
                List.class
        );

        Assert.assertTrue(
                "审批重抓前删旧数据的方法必须声明事务边界，否则JPA delete会在无事务线程下报错",
                deleteMethod.isAnnotationPresent(Transactional.class)
        );
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void invokeSetter(Object target, String methodName, Class<?> argType, Object value) throws Exception {
        Method method = target.getClass().getMethod(methodName, argType);
        method.invoke(target, value);
    }

    private Method findMethod(Class<?> target, String methodName) {
        try {
            return target.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private Method findSetter(Class<?> target, String methodName, Class<?> argType) {
        try {
            return target.getMethod(methodName, argType);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private Object createAttendanceUser(Class<?> attendanceUserClass, Long empId, String userId) throws Exception {
        Object user = attendanceUserClass.getDeclaredConstructor().newInstance();
        invokeSetter(user, "setEmpId", Long.class, empId);
        invokeSetter(user, "setUserId", String.class, userId);
        return user;
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
}
