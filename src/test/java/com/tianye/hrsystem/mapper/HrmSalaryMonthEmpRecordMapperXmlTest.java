package com.tianye.hrsystem.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HrmSalaryMonthEmpRecordMapperXmlTest {

    @Test
    public void queryPaySalaryEmployeeList_shouldPreferEmployeeFullAttendanceAmountsAndFallbackToSalaryBasicSettings() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmSalaryMonthEmpRecordMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "queryPaySalaryEmployeeList");

        Assert.assertTrue("计薪员工查询必须读取最新基本工资金额设置", query.contains("hrm_salary_basic"));
        Assert.assertTrue("普通员工全勤金额必须优先来自员工表 ordinary_full_attendance_amount",
                query.contains("a.ordinary_full_attendance_amount"));
        Assert.assertTrue("领导全勤金额必须优先来自员工表 leader_full_attendance_amount",
                query.contains("a.leader_full_attendance_amount"));
        Assert.assertTrue("普通员工全勤金额缺失时才回退基本工资设置",
                query.contains("coalesce(a.ordinary_full_attendance_amount, salary_basic_setting.ordinary_full_attendance_amount, 100)"));
        Assert.assertTrue("领导全勤金额缺失时才回退基本工资设置",
                query.contains("coalesce(a.leader_full_attendance_amount, salary_basic_setting.leader_full_attendance_amount, 500)"));
        Assert.assertTrue("领导全勤金额缺失时应回退 500", query.contains("500"));
        Assert.assertTrue("普通员工全勤金额缺失时应回退 100", query.contains("100"));
        Assert.assertTrue("计薪员工查询必须返回手机号，供同名员工按姓名+手机号识别", query.contains("a.mobile as mobile"));
        Assert.assertTrue("计薪员工查询必须返回员工所属体系", query.contains("a.affiliation_system as affiliationSystem"));
        Assert.assertTrue("计薪员工查询必须返回员工休息制度", query.contains("a.rest_type as restType"));
        Assert.assertTrue("计薪员工查询的加班夜班资格必须同时来自所属体系和休息制度",
                query.contains("a.affiliation_system = 2") && query.contains("a.rest_type = 2"));
        Assert.assertFalse("计薪员工查询不得再按部门名称判断生产体系", query.contains("质量部"));
    }

    @Test
    public void querySalaryMonthList_shouldUseEmployeeAffiliationSystemForProductionFlag() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmSalaryMonthEmpRecordMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "querySalaryMonthList");

        Assert.assertTrue("薪资列表必须返回员工所属体系", query.contains("b.affiliation_system as affiliationSystem"));
        Assert.assertTrue("薪资列表必须返回员工休息制度", query.contains("b.rest_type as restType"));
        Assert.assertTrue("薪资列表的加班夜班资格必须同时来自所属体系和休息制度",
                query.contains("b.affiliation_system = 2") && query.contains("b.rest_type = 2"));
        Assert.assertFalse("薪资列表不得再按部门名称判断生产体系", query.contains("质量部"));
    }

    private String extractSelect(String xml, String id) {
        String selectStart = "<select id=\"" + id + "\"";
        int startIndex = xml.indexOf(selectStart);
        Assert.assertTrue(id + " select must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</select>", startIndex);
        Assert.assertTrue(id + " select must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
    }
}
