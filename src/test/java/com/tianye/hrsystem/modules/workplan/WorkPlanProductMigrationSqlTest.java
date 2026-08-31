package com.tianye.hrsystem.modules.workplan;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WorkPlanProductMigrationSqlTest {

    @Test
    public void migration_shouldCreateProductPositionAndEmployeeTables() throws Exception {
        String sql = new String(Files.readAllBytes(
                Paths.get("docs/sql/2026-08-22_hrm_workplan_product_position_employee.sql")),
                StandardCharsets.UTF_8);

        Assert.assertTrue(sql.contains("hrm_workplan_product"));
        Assert.assertTrue(sql.contains("hrm_workplan_product_position"));
        Assert.assertTrue(sql.contains("hrm_workplan_position_employee"));
        Assert.assertTrue(sql.contains("product_name"));
        Assert.assertTrue(sql.contains("position_name"));
        Assert.assertTrue(sql.contains("employee_id"));
        Assert.assertTrue(sql.contains("employee_name"));
        Assert.assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS"));
    }

    @Test
    public void menuMigration_shouldCreateWorkplanProductMenuAndGrantRolesWithRecordsPermission() throws Exception {
        String sql = new String(Files.readAllBytes(
                Paths.get("docs/sql/2026-08-22_workplan_product_menu_permission.sql")),
                StandardCharsets.UTF_8);

        Assert.assertTrue(sql.contains("/hrm/attendance/workplanProduct"));
        Assert.assertTrue(sql.contains("生产产品"));
        Assert.assertTrue(sql.contains("/hrm/attendance/records"));
        Assert.assertTrue(sql.contains("INSERT INTO tbmenu"));
        Assert.assertTrue(sql.contains("INSERT INTO tbrolemenu"));
        Assert.assertTrue(sql.contains("NOT EXISTS"));
    }
}
