package com.tianye.hrsystem.modules.miniapp;

import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MiniAppOpenidSchemaContractTest {

    @Test
    public void employeeModelAndRepository_shouldExposeOpenid() throws Exception {
        Assert.assertEquals(String.class, HrmEmployee.class.getMethod("getOpenid").getReturnType());
        Assert.assertNotNull(HrmEmployee.class.getMethod("setOpenid", String.class));
        Assert.assertNotNull(hrmEmployeeRepository.class.getMethod("findFirstByOpenid", String.class));
    }

    @Test
    public void migration_shouldAddNullableUniqueOpenidToEveryCurrentTenant() throws Exception {
        Path migration = Paths.get("docs/sql/2026-09-02_hrm_employee_openid.sql");
        Assert.assertTrue("缺少 hrm_employee openid 迁移脚本", Files.isRegularFile(migration));
        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8).toLowerCase();

        for (int i = 1; i <= 5; i++) {
            Assert.assertTrue("迁移未覆盖 hr_000" + i, sql.contains("hr_000" + i));
        }
        Assert.assertTrue(sql.contains("`openid` varchar(64)"));
        Assert.assertTrue("OpenID 必须使用大小写敏感排序规则", sql.contains("collate ascii_bin"));
        Assert.assertTrue("迁移必须修正已存在的非 binary 字段", sql.contains("modify column `openid`"));
        Assert.assertTrue(sql.contains("unique"));
    }

    @Test
    public void tenantBaseline_shouldIncludeBinaryUniqueOpenid() throws Exception {
        Path baseline = Paths.get("src/main/resources/sql/tenant/schema-baseline.sql");
        String sql = new String(Files.readAllBytes(baseline), StandardCharsets.UTF_8).toLowerCase();
        int employeeTable = sql.indexOf("create table `hrm_employee`");
        int nextTable = sql.indexOf("create table", employeeTable + 1);
        String employeeDdl = sql.substring(employeeTable, nextTable);

        Assert.assertTrue(employeeDdl.contains("`openid` varchar(64)"));
        Assert.assertTrue(employeeDdl.contains("collate ascii_bin"));
        Assert.assertTrue(employeeDdl.contains("unique key `uk_hrm_employee_openid` (`openid`)"));
    }
}
