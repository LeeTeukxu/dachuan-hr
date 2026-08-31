package com.tianye.hrsystem.modules.insurance.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HrmInsuranceMonthRecordMapperXmlTest {

    @Test
    public void queryInsuranceRecordListContainsAdvancedFilterSql() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmInsuranceMonthRecordMapper.xml")), StandardCharsets.UTF_8);

        Assert.assertTrue(xml.contains("data.startPeriod"));
        Assert.assertTrue(xml.contains("data.endPeriod"));
        Assert.assertTrue(xml.contains("data.deptIds"));
        Assert.assertTrue(xml.contains("data.employeeIds"));
        Assert.assertTrue(xml.contains("c.dept_id in"));
        Assert.assertTrue(xml.contains("c.employee_id in"));
    }

    @Test
    public void queryInsuranceRecordListShouldReturnMonthlyStatusCountsAndEmployeeNames() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmInsuranceMonthRecordMapper.xml")), StandardCharsets.UTF_8);

        Assert.assertTrue(xml.contains("insuredNum"));
        Assert.assertTrue(xml.contains("insuredEmployeeNames"));
        Assert.assertTrue(xml.contains("stoppedEmployeeNames"));
        Assert.assertTrue(xml.contains("GROUP_CONCAT"));
        Assert.assertTrue(xml.contains("b.status = 1"));
        Assert.assertTrue(xml.contains("b.status = 0"));
        Assert.assertTrue(xml.contains("c.employee_name"));
        Assert.assertTrue(xml.contains("SEPARATOR '、'"));
        Assert.assertTrue(xml.contains("group_concat_max_len"));
    }

    @Test
    public void queryInsurancePageListShouldExposeSalaryBasicInsuranceAmountFlag() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmInsuranceMonthRecordMapper.xml")), StandardCharsets.UTF_8);

        Assert.assertTrue(xml.contains("include_salary_basic_insurance_amount"));
    }
}
