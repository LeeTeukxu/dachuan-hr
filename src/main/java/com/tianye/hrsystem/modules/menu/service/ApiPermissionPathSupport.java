package com.tianye.hrsystem.modules.menu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ApiPermissionPathSupport {

    private final Map<String, List<String>> exactApiMenuPathMap = new LinkedHashMap<>();
    private final Map<String, List<String>> apiMenuPathMap = new LinkedHashMap<>();

    @Value("${hrm.system.database}")
    private String systemDatabase;

    /** SaaS 改造 P3：true=从主库 tb_api_permission 表加载映射（可热刷新）；false=使用内置默认 */
    @Value("${hrm.permission.api-mapping-from-db:false}")
    private boolean loadFromDb;

    public ApiPermissionPathSupport() {
        put("/tbLoginUser", "/hrm/system/loginUser");
        put("/tbRoleMenu", "/hrm/system/rolePermission");
        put("/tbRoleTypes", "/hrm/system/rolePermission");
        put("/tbMenu", "/hrm/system/menu");
        put("/menu", "/hrm/system/menu");

        putExact("/dashboard/permission/all", "/hrm/system/dashboardPermission");
        putExact("/dashboard/permission/save", "/hrm/system/dashboardPermission");

        put("/hrmDept", "/hrm/dept");
        put("/hrmEmployeeContract", "/hrm/employee");
        put("/hrmEmployeeFile", "/hrm/employee");
        put("/hrmEmployeePost", "/hrm/employee");
        put("/hrmEmployee/SocialSecurity", "/hrm/employee");
        put("/hrmEmployee", "/hrm/employee");
        put("/hrmRecruitChannel", "/hrm/employee");

        put("/workPlan", "/hrm/attendance/records");
        put("/workPlanProduct", "/hrm/attendance/workplanProduct");
        put("/attendanceData", "/hrm/attendance/scheduling");
        put("/hrmAttendanceClock", "/hrm/attendance/index");
        put("/hrmAttendanceEmpMonthRecord", "/hrm/attendance/index");
        put("/hrmAttendanceApproval", "/hrm/attendance/approval");
        put("/hrmOvertimeNightStatistics", "/hrm/attendance/overtimeNight");
        put("/hrmWorkweekSetting", "/hrm/attendance/workweek");
        put("/hrmAttendanceGroupRelationDept", "/hrm/attendance/scheduling");
        put("/hrmAttendanceGroupRelationEmployee", "/hrm/attendance/scheduling");
        put("/hrmAttendanceGroup", "/hrm/attendance/scheduling");
        put("/hrmAttendancePoint", "/hrm/attendance/scheduling");
        putExact("/hrmAttendanceRule/queryAttendanceRulePageList", "/hrm/attendance/scheduling", "/manage/attendance");
        putExact("/hrmAttendanceRule/addAttendanceRule", "/manage/attendance");
        putExact("/hrmAttendanceRule/delete", "/manage/attendance");
        put("/hrmAttendanceRule", "/hrm/attendance/scheduling");
        put("/hrmAttendanceShift", "/hrm/attendance/scheduling");
        put("/hrmAttendanceInfo", "/hrm/attendance/scheduling");
        put("/hrmProduceAttendance", "/hrm/attendance/index");
        put("/hrmEmployeeLeaveRecord", "/hrm/attendance/index");
        put("/hrmEmployeeOverTimeRecord", "/hrm/attendance/overtimeNight");

        put("/hrmSalaryArchives", "/hrm/salary/archives");
        put("/hrmSalaryMonthRecord", "/hrm/salary/index");
        put("/hrmSalaryHistoryRecord", "/hrm/salary/history");
        put("/hrmSalarySlipRecord", "/hrm/salary/record");
        put("/hrmSalarySlipTemplateOption", "/hrm/salary/record");
        put("/hrmSalarySlipTemplate", "/hrm/salary/record");
        putExact("/hrmSalaryBasic/findAll", "/hrm/salary/index", "/manage/salary");
        putExact("/hrmSalaryBasic/saveSalaryBasic", "/hrm/salary/index", "/manage/salary");
        putExact("/hrmSalaryBasic/queryById", "/hrm/salary/index", "/manage/salary");
        putExact("/hrmSalaryBasic/deleteSalaryBasic", "/hrm/salary/index", "/manage/salary");
        put("/hrmSalaryConfig", "/manage/salary");
        put("/hrmSalaryBasic", "/manage/salary");
        put("/hrmSalaryChangeTemplate", "/manage/salary");
        putExact("/hrmBonus/importBonus", "/hrm/bonus/payroll");
        putExact("/hrmBonus/importTaxOnlyBonus", "/hrm/bonus/taxOnly");
        putExact("/hrmBonus/queryBounsList", "/hrm/bonus/payroll", "/hrm/bonus/taxOnly");
        put("/hrmBonus", "/hrm/bonus/payroll");
        put("/hrmAdditional", "/hrm/salary/addition");
        put("/hrmEmployeeAdditional", "/hrm/salary/addition");
        put("/hrmPersonalIncomeTax", "/hrm/salary/tax");
        putExact("/hrmHolidayDeduction/queryHolidayDeductionList", "/hrm/salary/addition", "/manage/vacation");
        putExact("/hrmHolidayDeduction/saveHolidayDeduction", "/manage/vacation");
        put("/hrmHolidayDeduction", "/hrm/salary/addition");
        put("/hrmRemainingVaction", "/manage/vacation");

        put("/hrmInsuranceMonthEmpRecord", "/hrm/insurance-scheme/index");
        put("/hrmInsuranceMonthRecord", "/hrm/insurance-scheme/index");
        put("/hrmInsuranceProject", "/manage/insurance-scheme");
        putExact("/hrmInsuranceScheme/index", "/hrm/insurance-scheme/index", "/manage/insurance-scheme");
        putExact("/hrmInsuranceScheme/queryInsuranceSchemeById", "/hrm/insurance-scheme/index", "/manage/insurance-scheme");
        putExact("/hrmInsuranceScheme/deleteInsuranceScheme", "/manage/insurance-scheme");
        put("/hrmInsuranceScheme", "/hrm/insurance-scheme/index");

        put("/dict", "/hrm/dataConfig/index");
        put("/tbCompanyList", "/hrm/dataConfig/index");
        put("/report", "/hrm/dataConfig/index");
        put("/tempworker", "/hrm/dataConfig/index");

        put("/workPlanApplication", "/hrm/attendance/scheduling");
    }

    /**
     * SaaS 改造 P3：启动时尝试从主库 tb_api_permission 加载映射（DB 优先，失败回退内置默认）
     */
    @PostConstruct
    public void initFromDb() {
        if (!loadFromDb) {
            log.info("【权限映射】使用内置默认映射（hrm.permission.api-mapping-from-db=false）");
            return;
        }
        try {
            refresh();
            log.info("【权限映射】已从数据库加载 API-菜单映射");
        } catch (Exception e) {
            log.warn("【权限映射】从数据库加载失败，保留内置默认: {}", e.getMessage());
        }
    }

    /**
     * SaaS 改造 P3：热刷新——管理端修改 tb_api_permission 后调用，无需重启
     */
    public synchronized void refresh() throws Exception {
        javax.sql.DataSource ds = com.tianye.hrsystem.config.CompanyDataSourceProvider.getDataSource("Default");
        Map<String, List<String>> exactMap = new LinkedHashMap<>();
        Map<String, List<String>> prefixMap = new LinkedHashMap<>();
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("USE `" + systemDatabase + "`");
            try (ResultSet rs = st.executeQuery(
                    "SELECT api_prefix, menu_paths, match_type FROM tb_api_permission ORDER BY sortno, id")) {
                while (rs.next()) {
                    String apiPrefix = rs.getString("api_prefix");
                    List<String> menuPaths = Arrays.asList(rs.getString("menu_paths").split(","));
                    if ("exact".equalsIgnoreCase(rs.getString("match_type"))) {
                        exactMap.put(apiPrefix, new ArrayList<>(menuPaths));
                    } else {
                        prefixMap.put(apiPrefix, new ArrayList<>(menuPaths));
                    }
                }
            }
        }
        if (exactMap.isEmpty() && prefixMap.isEmpty()) {
            throw new IllegalStateException("tb_api_permission 表为空");
        }
        synchronized (this) {
            exactApiMenuPathMap.clear();
            exactApiMenuPathMap.putAll(exactMap);
            apiMenuPathMap.clear();
            apiMenuPathMap.putAll(prefixMap);
        }
    }

    public String resolveRequiredMenuPath(String requestUri, String contextPath) {
        List<String> menuPaths = resolveRequiredMenuPaths(requestUri, contextPath);
        return menuPaths.isEmpty() ? null : menuPaths.get(0);
    }

    public List<String> resolveRequiredMenuPaths(String requestUri, String contextPath) {
        String path = stripContextPath(requestUri, contextPath);
        if (isPublicPath(path)) {
            return Collections.emptyList();
        }
        List<String> exactMenuPaths = resolveMenuPaths(path, exactApiMenuPathMap);
        if (!exactMenuPaths.isEmpty()) {
            return exactMenuPaths;
        }
        return resolveMenuPaths(path, apiMenuPathMap);
    }

    private List<String> resolveMenuPaths(String path, Map<String, List<String>> menuPathMap) {
        for (Map.Entry<String, List<String>> entry : menuPathMap.entrySet()) {
            if (path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }

    private void put(String apiPrefix, String menuPath) {
        apiMenuPathMap.put(apiPrefix, Collections.singletonList(menuPath));
    }

    private void putExact(String apiPrefix, String... menuPaths) {
        exactApiMenuPathMap.put(apiPrefix, new ArrayList<>(Arrays.asList(menuPaths)));
    }

    private String stripContextPath(String requestUri, String contextPath) {
        if (StringUtils.isEmpty(requestUri)) {
            return "";
        }
        if (!StringUtils.isEmpty(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/login")
                || path.startsWith("/swagger")
                || path.startsWith("/v2/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/doc.html")
                || path.startsWith("/favicon");
    }
}
