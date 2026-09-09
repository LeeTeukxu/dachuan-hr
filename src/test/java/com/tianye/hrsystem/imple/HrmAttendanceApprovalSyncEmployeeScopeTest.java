package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmEmployeeQuitInfo;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmEmployeeQuitInfoRepository;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批"获取审批数据"全量名单过滤测试。
 *
 * 覆盖 2026-09-09 改造：全量名单从"所有未删除(is_del≠1)"收窄为
 *   "未删除 + (在职 entry_status≠4 或 近两月离职)"；定向指定人不套该过滤(勾谁抓谁)。
 * 测试直接反射调用私有 resolveTargetEmployees，用 JDK Proxy mock 两个 repository，
 * 避免启动 Spring 上下文。
 */
public class HrmAttendanceApprovalSyncEmployeeScopeTest {

    /** 离职窗口锚点（近两月离职判定下界）：2026-07-01 12:00 */
    private static final long ANCHOR_MILLIS = LocalDateTime.of(2026, 7, 1, 12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    @Test
    public void fullScope_keepsActive_andRecentlyQuit_dropsOldQuitAndUnmapped() throws Exception {
        long inWindowQuitMillis = LocalDateTime.of(2026, 7, 15, 9, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); // ∈ [7/1, 8/1)
        long oldQuitMillis = LocalDateTime.of(2023, 12, 31, 9, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();   // 早年离职

        HrmEmployee active = employee(1001L, "在职甲", "18600000001", 1, "ding-1001");
        HrmEmployee recentlyQuit = employee(1002L, "近两月离职乙", "18600000002", 4, "ding-1002");
        HrmEmployee oldQuitEmp = employee(1003L, "早年离职丙", "18600000003", 4, "ding-1003");
        HrmEmployee quitNoRecord = employee(1004L, "离职无记录丁", "18600000004", 4, "ding-1004");
        HrmEmployee quitDeleted = employee(1005L, "离职已删戊", "18600000005", 4, "ding-1005");
        quitDeleted.setIsDel(1);

        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();
        setField(service, "hrmEmployeeRepository", employeeRepoProxy(
                Arrays.asList(active, recentlyQuit, oldQuitEmp, quitNoRecord, quitDeleted)));
        setField(service, "quitInfoRepository", quitInfoRepoProxy(
                Arrays.asList(quitRecord(1002L, inWindowQuitMillis), quitRecord(1003L, oldQuitMillis))));
        setField(service, "quitWindowAnchorMillis", ANCHOR_MILLIS);

        List<HrmEmployee> result = invokeResolveTargetEmployees(service, null);

        List<Long> ids = result.stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
        // 在职保留；近两月离职(7/15)保留；早年离职、离职无quit记录、离职但 is_del=1 一律排除
        Assert.assertEquals("全量名单应只含在职 + 近两月离职", Arrays.asList(1001L, 1002L), ids);
    }

    @Test
    public void fullScope_anchorUnset_dropsAllLeftEmployees() throws Exception {
        HrmEmployee active = employee(1001L, "在职甲", "18600000001", 1, "ding-1001");
        HrmEmployee recentlyQuit = employee(1002L, "近两月离职乙", "18600000002", 4, "ding-1002");

        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();
        setField(service, "hrmEmployeeRepository", employeeRepoProxy(
                Arrays.asList(active, recentlyQuit)));
        setField(service, "quitInfoRepository", quitInfoRepoProxy(
                Arrays.asList(quitRecord(1002L,
                        LocalDateTime.of(2026, 7, 15, 9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))));
        // quitWindowAnchorMillis 保持默认 0：异常/未设置路径 → 不纳入任何离职者

        List<HrmEmployee> result = invokeResolveTargetEmployees(service, null);

        List<Long> ids = result.stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
        Assert.assertEquals("锚点未设置时离职者应全部排除，仅留在职", Arrays.asList(1001L), ids);
    }

    @Test
    public void targetedScope_ignoresQuitWindowFilter_picksAnySelectedNonDeleted() throws Exception {
        // 定向指定人(穿梭框勾选具体员工/部门)：不套"近两月离职"过滤，勾谁抓谁——即便勾早年离职者也保留(仅按 is_del≠1)
        HrmEmployee oldQuit = employee(1003L, "早年离职丙", "18600000003", 4, "ding-1003");

        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();
        setField(service, "hrmEmployeeRepository", targetedRepoProxy(
                Arrays.asList(oldQuit)));
        setField(service, "quitInfoRepository", quitInfoRepoProxy(new ArrayList<>()));
        setField(service, "quitWindowAnchorMillis", ANCHOR_MILLIS);

        List<HrmEmployee> result = invokeResolveTargetEmployees(service, Arrays.asList(1003L));

        List<Long> ids = result.stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
        Assert.assertEquals("定向模式应勾谁抓谁，不受近两月离职限制", Arrays.asList(1003L), ids);
    }

    // ---- helpers ----

    private static HrmEmployee employee(Long id, String name, String mobile, Integer entryStatus, String dingId) {
        HrmEmployee e = new HrmEmployee();
        e.setEmployeeId(id);
        e.setEmployeeName(name);
        e.setMobile(mobile);
        e.setEntryStatus(entryStatus);
        e.setDingtalkUserId(dingId);
        e.setIsDel(0);
        return e;
    }

    private static HrmEmployeeQuitInfo quitRecord(Long employeeId, long planQuitMillis) {
        HrmEmployeeQuitInfo qi = new HrmEmployeeQuitInfo();
        qi.setEmployeeId(employeeId);
        qi.setPlanQuitTime(new java.util.Date(planQuitMillis));
        return qi;
    }

    private static List<HrmEmployee> invokeResolveTargetEmployees(HrmAttendanceApprovalSyncServiceImpl service,
                                                                  List<Long> employeeIds) throws Exception {
        Method method = HrmAttendanceApprovalSyncServiceImpl.class.getDeclaredMethod(
                "resolveTargetEmployees", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<HrmEmployee> result = (List<HrmEmployee>) method.invoke(service, employeeIds);
        return result == null ? new ArrayList<>() : result;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = HrmAttendanceApprovalSyncServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** 全量用：findAll() 返回全部员工 */
    private static Object employeeRepoProxy(List<HrmEmployee> all) {
        return Proxy.newProxyInstance(hrmEmployeeRepository.class.getClassLoader(),
                new Class<?>[]{hrmEmployeeRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return all;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    /** 定向用：findAllByEmployeeIdIn 返回与请求匹配的员工 */
    private static Object targetedRepoProxy(List<HrmEmployee> all) {
        return Proxy.newProxyInstance(hrmEmployeeRepository.class.getClassLoader(),
                new Class<?>[]{hrmEmployeeRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmployeeIdIn".equals(method.getName())) {
                        Collection<Long> requested = (Collection<Long>) args[0];
                        return all.stream()
                                .filter(e -> e.getEmployeeId() != null && requested.contains(e.getEmployeeId()))
                                .collect(Collectors.toList());
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object quitInfoRepoProxy(List<HrmEmployeeQuitInfo> quitInfos) {
        return Proxy.newProxyInstance(hrmEmployeeQuitInfoRepository.class.getClassLoader(),
                new Class<?>[]{hrmEmployeeQuitInfoRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByEmployeeIdIn".equals(method.getName())) {
                        Collection<Long> requested = (Collection<Long>) args[0];
                        return quitInfos.stream()
                                .filter(q -> q.getEmployeeId() != null && requested.contains(q.getEmployeeId()))
                                .collect(Collectors.toList());
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) return false;
        if (byte.class.equals(returnType)) return (byte) 0;
        if (short.class.equals(returnType)) return (short) 0;
        if (int.class.equals(returnType)) return 0;
        if (long.class.equals(returnType)) return 0L;
        if (float.class.equals(returnType)) return 0F;
        if (double.class.equals(returnType)) return 0D;
        if (char.class.equals(returnType)) return '\0';
        return null;
    }
}
