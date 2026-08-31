package com.tianye.hrsystem.modules.menu.service;

import org.junit.Assert;
import org.junit.Test;

public class ApiPermissionPathSupportTest {

    private final ApiPermissionPathSupport support = new ApiPermissionPathSupport();

    @Test
    public void resolveRequiredMenuPath_shouldMapSystemManagementApisToSystemSubMenus() {
        Assert.assertEquals("/hrm/system/loginUser", support.resolveRequiredMenuPath("/hrsystem/tbLoginUser/Add", "/hrsystem"));
        Assert.assertEquals("/hrm/system/rolePermission", support.resolveRequiredMenuPath("/hrsystem/tbRoleMenu/saveRoleMenuList", "/hrsystem"));
        Assert.assertEquals("/hrm/system/rolePermission", support.resolveRequiredMenuPath("/hrsystem/tbRoleTypes/queryRoleTypesList", "/hrsystem"));
        Assert.assertEquals("/hrm/system/menu", support.resolveRequiredMenuPath("/hrsystem/tbMenu/queryMenuList", "/hrsystem"));
    }

    @Test
    public void resolveRequiredMenuPath_shouldMapBusinessApisToSubMenuRoutes() {
        Assert.assertEquals("/hrm/dept", support.resolveRequiredMenuPath("/hrsystem/hrmDept/queryTreeList", "/hrsystem"));
        Assert.assertEquals("/hrm/employee", support.resolveRequiredMenuPath("/hrsystem/hrmEmployee/queryPageList", "/hrsystem"));
        Assert.assertEquals("/hrm/attendance/records", support.resolveRequiredMenuPath("/hrsystem/workPlan/saveAll", "/hrsystem"));
        Assert.assertEquals("/hrm/attendance/workplanProduct",
                support.resolveRequiredMenuPath("/hrsystem/workPlanProduct/queryTree", "/hrsystem"));
        Assert.assertEquals("/hrm/attendance/overtimeNight", support.resolveRequiredMenuPath("/hrsystem/hrmOvertimeNightStatistics/queryPageList", "/hrsystem"));
        Assert.assertEquals("/hrm/salary/archives", support.resolveRequiredMenuPath("/hrsystem/hrmSalaryArchives/queryPageList", "/hrsystem"));
    }

    @Test
    public void resolveRequiredMenuPaths_shouldAllowSystemSettingGearPermissions() {
        Assert.assertTrue(support.resolveRequiredMenuPaths("/hrsystem/hrmAttendanceRule/queryAttendanceRulePageList", "/hrsystem")
                .contains("/manage/attendance"));
        Assert.assertTrue(support.resolveRequiredMenuPaths("/hrsystem/hrmInsuranceScheme/index", "/hrsystem")
                .contains("/manage/insurance-scheme"));
        Assert.assertEquals("/manage/vacation", support.resolveRequiredMenuPath("/hrsystem/hrmRemainingVaction/queryRemainingVacationList", "/hrsystem"));
        Assert.assertTrue(support.resolveRequiredMenuPaths("/hrsystem/hrmSalaryBasic/findAll", "/hrsystem")
                .contains("/manage/salary"));
    }

    @Test
    public void resolveRequiredMenuPaths_shouldMapBonusApisToSeparateUploadMenus() {
        Assert.assertEquals("/hrm/bonus/payroll",
                support.resolveRequiredMenuPath("/hrsystem/hrmBonus/importBonus", "/hrsystem"));
        Assert.assertEquals("/hrm/bonus/taxOnly",
                support.resolveRequiredMenuPath("/hrsystem/hrmBonus/importTaxOnlyBonus", "/hrsystem"));
        Assert.assertTrue(support.resolveRequiredMenuPaths("/hrsystem/hrmBonus/queryBounsList", "/hrsystem")
                .contains("/hrm/bonus/payroll"));
        Assert.assertTrue(support.resolveRequiredMenuPaths("/hrsystem/hrmBonus/queryBounsList", "/hrsystem")
                .contains("/hrm/bonus/taxOnly"));
    }

    @Test
    public void resolveRequiredMenuPath_shouldSkipPublicAndUnmappedPaths() {
        Assert.assertNull(support.resolveRequiredMenuPath("/hrsystem/login", "/hrsystem"));
        Assert.assertNull(support.resolveRequiredMenuPath("/hrsystem/swagger-ui.html", "/hrsystem"));
        Assert.assertNull(support.resolveRequiredMenuPath("/hrsystem/unknown/ping", "/hrsystem"));
    }
}
