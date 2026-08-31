package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.BasePage;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class HrmAttendanceApprovalControllerTest {

    @Test
    public void queryPageList_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> queryBOClass = loadClass("com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO");
        Assert.assertNotNull("审批数据分页查询BO未创建", queryBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        BasePage<Object> expectedPage = new BasePage<>(1L, 15L, 1L, Collections.emptyList());
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("queryPageList".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        return expectedPage;
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object queryBO = queryBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(queryBO, "setPage", Long.class, 1L);
        invokeSetter(queryBO, "setLimit", Long.class, 15L);

        Method queryMethod = controllerClass.getMethod("queryPageList", queryBOClass);
        Object result = queryMethod.invoke(controller, queryBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertSame("控制器必须返回服务结果", expectedPage, data);
    }

    @Test
    public void checkMonthData_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("checkMonthData".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        return Collections.singletonMap("exists", Boolean.TRUE);
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object queryBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(queryBO, "setMonth", String.class, "2026-05");

        Method queryMethod = controllerClass.getMethod("checkMonthData", monthBOClass);
        Object result = queryMethod.invoke(controller, queryBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals(Boolean.TRUE, ((java.util.Map<?, ?>) data).get("exists"));
    }

    @Test
    public void fetchMonthData_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("tryBeginFetch".equals(method.getName())) {
                        return Boolean.TRUE;
                    }
                    if ("fetchMonthDataWithAutoRetry".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        return Collections.singletonMap("insertedCount", 8L);
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);
        setField(controller, "syncTaskLauncher", new com.tianye.hrsystem.task.AttendanceSyncTaskLauncher());

        Object queryBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(queryBO, "setMonth", String.class, "2026-05");
        invokeSetter(queryBO, "setEmployeeIds", java.util.List.class, Arrays.asList(11L, 12L));
        invokeSetter(queryBO, "setApprovalTypes", java.util.List.class, Arrays.asList("overtime", "leave"));

        Method queryMethod = controllerClass.getMethod("fetchMonthData", monthBOClass);
        Object result = queryMethod.invoke(controller, queryBO);

        // 后端已异步化：委托发生在提交线程池的任务里，等待其执行完成
        awaitDelegation(invokeCount, 1);
        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals(Boolean.TRUE, ((java.util.Map<?, ?>) data).get("queued"));
    }

    @Test
    public void fetchMonthData_shouldPassApprovalTypesToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Object> capturedTypes = new java.util.concurrent.atomic.AtomicReference<>(null);

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("tryBeginFetch".equals(method.getName())) {
                        return Boolean.TRUE;
                    }
                    if ("fetchMonthDataWithAutoRetry".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        capturedTypes.set(request.getClass().getMethod("getApprovalTypes").invoke(request));
                        return Collections.singletonMap("insertedCount", 3L);
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);
        setField(controller, "syncTaskLauncher", new com.tianye.hrsystem.task.AttendanceSyncTaskLauncher());

        Object queryBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(queryBO, "setMonth", String.class, "2026-05");
        invokeSetter(queryBO, "setApprovalTypes", java.util.List.class, Arrays.asList("misscard", "leave"));

        Method queryMethod = controllerClass.getMethod("fetchMonthData", monthBOClass);
        queryMethod.invoke(controller, queryBO);

        // 后端已异步化：等待后台任务执行后再断言透传参数
        awaitDelegation(invokeCount, 1);
        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertEquals("控制器必须原样透传approvalTypes给服务层", Arrays.asList("misscard", "leave"), capturedTypes.get());
    }

    @Test
    public void queryFetchProgress_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();
        java.util.Map<String, Object> progress = new java.util.LinkedHashMap<>();
        progress.put("status", "RUNNING");
        progress.put("message", "正在获取审批数据");

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("queryFetchProgress".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        return progress;
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Method queryMethod = controllerClass.getMethod("queryFetchProgress");
        Object result = queryMethod.invoke(controller);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertSame("控制器必须返回服务进度", progress, data);
    }

    @Test
    public void querySubtypeOptions_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("querySubtypeOptions".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        return Arrays.asList("事假", "调休", "哺乳假");
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Method queryMethod = controllerClass.getMethod("querySubtypeOptions");
        Object result = queryMethod.invoke(controller);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertEquals(Arrays.asList("事假", "调休", "哺乳假"), data);
    }

    @Test
    public void updateSubtype_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> updateBOClass = loadClass("com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalSubtypeBO");
        Assert.assertNotNull("审批子类型更新BO未创建", updateBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("updateSubtype".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        Assert.assertEquals("PROC-LEAVE-0604", request.getClass().getMethod("getApprovalId").invoke(request));
                        Assert.assertEquals("事假", request.getClass().getMethod("getSubType").invoke(request));
                        return Collections.singletonMap("subType", "事假");
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object updateBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(updateBO, "setApprovalId", String.class, "PROC-LEAVE-0604");
        invokeSetter(updateBO, "setSubType", String.class, "事假");

        Method updateMethod = controllerClass.getMethod("updateSubtype", updateBOClass);
        Object result = updateMethod.invoke(controller, updateBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals("事假", ((java.util.Map<?, ?>) data).get("subType"));
    }

    @Test
    public void updateDuration_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> updateBOClass = loadClass("com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalDurationBO");
        Assert.assertNotNull("审批时长更新BO未创建", updateBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("updateDuration".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        Assert.assertEquals("PROC-OT-001", request.getClass().getMethod("getApprovalId").invoke(request));
                        Assert.assertEquals("51", request.getClass().getMethod("getDuration").invoke(request));
                        Assert.assertEquals("小时", request.getClass().getMethod("getDurationUnit").invoke(request));
                        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                        result.put("duration", "51");
                        result.put("durationUnit", "小时");
                        return result;
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object updateBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(updateBO, "setApprovalId", String.class, "PROC-OT-001");
        invokeSetter(updateBO, "setDuration", String.class, "51");
        invokeSetter(updateBO, "setDurationUnit", String.class, "小时");

        Method updateMethod = controllerClass.getMethod("updateDuration", updateBOClass);
        Object result = updateMethod.invoke(controller, updateBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals("51", ((java.util.Map<?, ?>) data).get("duration"));
        Assert.assertEquals("小时", ((java.util.Map<?, ?>) data).get("durationUnit"));
    }

    @Test
    public void updateStatisticsStatus_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> updateBOClass = loadClass("com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalStatisticsStatusBO");
        Assert.assertNotNull("审批统计状态更新BO未创建", updateBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("updateStatisticsStatus".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        Assert.assertEquals("PROC-OT-001", request.getClass().getMethod("getApprovalId").invoke(request));
                        Assert.assertEquals("取消至统计", request.getClass().getMethod("getStatisticsStatus").invoke(request));
                        return Collections.singletonMap("statisticsStatus", "取消至统计");
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object updateBO = updateBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(updateBO, "setApprovalId", String.class, "PROC-OT-001");
        invokeSetter(updateBO, "setStatisticsStatus", String.class, "取消至统计");

        Method updateMethod = controllerClass.getMethod("updateStatisticsStatus", updateBOClass);
        Object result = updateMethod.invoke(controller, updateBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals("取消至统计", ((java.util.Map<?, ?>) data).get("statisticsStatus"));
    }

    @Test
    public void addManualApproval_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> addBOClass = loadClass("com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO");
        Assert.assertNotNull("手工添加审批数据BO未创建", addBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("addManualApproval".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        Assert.assertEquals(Arrays.asList(101L, 102L), request.getClass().getMethod("getEmployeeIds").invoke(request));
                        Assert.assertEquals("请假", request.getClass().getMethod("getTagName").invoke(request));
                        Assert.assertEquals("调休", request.getClass().getMethod("getSubType").invoke(request));
                        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                        result.put("insertedCount", 2);
                        result.put("duration", "4");
                        result.put("durationUnit", "小时");
                        return result;
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object addBO = addBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(addBO, "setEmployeeIds", java.util.List.class, Arrays.asList(101L, 102L));
        invokeSetter(addBO, "setTagName", String.class, "请假");
        invokeSetter(addBO, "setSubType", String.class, "调休");

        Method updateMethod = controllerClass.getMethod("addManualApproval", addBOClass);
        Object result = updateMethod.invoke(controller, addBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals(2, ((java.util.Map<?, ?>) data).get("insertedCount"));
        Assert.assertEquals("4", ((java.util.Map<?, ?>) data).get("duration"));
        Assert.assertEquals("小时", ((java.util.Map<?, ?>) data).get("durationUnit"));
    }

    @Test
    public void deleteApproval_shouldDelegateToServiceByApprovalId() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> deleteBOClass = loadClass("com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO");
        Assert.assertNotNull("删除审批数据BO未创建", deleteBOClass);

        Class<?> resultClass = loadClass("com.tianye.hrsystem.entity.vo.Result");
        Assert.assertNotNull("统一返回结果类型不存在", resultClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("deleteApproval".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        Assert.assertEquals("PROC-SAME-NAME-001", request.getClass().getMethod("getApprovalId").invoke(request));
                        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                        result.put("approvalId", "PROC-SAME-NAME-001");
                        result.put("deleted", Boolean.TRUE);
                        return result;
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object deleteBO = deleteBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(deleteBO, "setApprovalId", String.class, "PROC-SAME-NAME-001");

        Method deleteMethod = controllerClass.getMethod("deleteApproval", deleteBOClass);
        Object result = deleteMethod.invoke(controller, deleteBO);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Object data = resultClass.getMethod("getData").invoke(result);
        Assert.assertTrue(data instanceof java.util.Map);
        Assert.assertEquals("PROC-SAME-NAME-001", ((java.util.Map<?, ?>) data).get("approvalId"));
        Assert.assertEquals(Boolean.TRUE, ((java.util.Map<?, ?>) data).get("deleted"));
    }

    @Test
    public void checkMonthData_shouldPassEmployeeIdsToService() throws Exception {
        Class<?> controllerClass = loadClass("com.tianye.hrsystem.controller.HrmAttendanceApprovalController");
        Assert.assertNotNull("审批数据控制器未创建", controllerClass);

        Class<?> serviceInterface = loadClass("com.tianye.hrsystem.service.IHrmAttendanceApprovalService");
        Assert.assertNotNull("审批数据服务接口未创建", serviceInterface);

        Class<?> monthBOClass = loadClass("com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO");
        Assert.assertNotNull("审批数据月份BO未创建", monthBOClass);

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("checkMonthData".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        Object request = args[0];
                        Object employeeIds = request.getClass().getMethod("getEmployeeIds").invoke(request);
                        Assert.assertEquals(Arrays.asList(21L, 22L), employeeIds);
                        return Collections.singletonMap("exists", Boolean.FALSE);
                    }
                    return null;
                });

        setField(controller, "attendanceApprovalService", serviceProxy);

        Object queryBO = monthBOClass.getDeclaredConstructor().newInstance();
        invokeSetter(queryBO, "setMonth", String.class, "2026-05");
        invokeSetter(queryBO, "setEmployeeIds", java.util.List.class, Arrays.asList(21L, 22L));

        Method queryMethod = controllerClass.getMethod("checkMonthData", monthBOClass);
        queryMethod.invoke(controller, queryBO);

        Assert.assertEquals("控制器必须原样透传employeeIds给服务层", 1, invokeCount.get());
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

    /** 异步委托等待：后台任务在线程池执行，轮询至委托计数达标 */
    private static void awaitDelegation(AtomicInteger counter, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000L;
        while (counter.get() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20L);
        }
    }
}
