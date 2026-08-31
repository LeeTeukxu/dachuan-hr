package com.tianye.hrsystem.modules.insurance.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class HrmInsuranceSchemeMapperXmlTest {

    @Test
    public void insuranceSchemeCount_shouldOnlySumEnabledProjectRowsAndIgnoreSalaryBasicInsuranceSettings() throws Exception {
        assertSchemeCountUsesEnabledProjectsOnly("src/main/resources/mapper/HrmInsuranceSchemeMapper.xml");
        assertSchemeCountUsesEnabledProjectsOnly("src/main/resources/mapper/HrmInsuranceSechemeMapper.xml");
    }

    @Test
    public void insuranceSchemeIndex_shouldReturnEmployeeNamesForUsageTooltip() throws Exception {
        assertUsageEmployeeNamesIncluded("src/main/resources/mapper/HrmInsuranceSchemeMapper.xml", "queryInsuranceSchemePageList");
        assertUsageEmployeeNamesIncluded("src/main/resources/mapper/HrmInsuranceSechemeMapper.xml", "index");
    }

    @Test
    public void monthEmpProjectCount_shouldOnlySumEnabledProjectRowsAndIgnoreSalaryBasicInsuranceSettings() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmInsuranceMonthEmpProjectRecordMapper.xml")),
                StandardCharsets.UTF_8);
        String select = extractSelect(xml, "queryProjectCount").toLowerCase(Locale.ROOT);

        Assert.assertFalse("员工月度参保项目合计不得读取基本工资金额设置", select.contains("hrm_salary_basic"));
        Assert.assertFalse("员工月度个人社保合计不得自动累加长期护理保险金额设置", select.contains("long_term_care_insurance_amount"));
        Assert.assertFalse("员工月度公司社保合计不得自动累加大额医疗保险金额设置", select.contains("large_medical_insurance_amount"));
        Assert.assertTrue("员工月度项目合计必须按启用状态过滤", countOccurrences(select, "is_enabled") >= 4);
        Assert.assertTrue("员工月度项目合计旧数据 is_enabled 为空时应按启用兼容", select.contains("coalesce("));
        Assert.assertTrue("员工月度社保合计必须继续按项目类型区分社保", select.contains("type not in (10, 11)"));
        Assert.assertTrue("员工月度公积金合计必须继续按项目类型区分公积金", select.contains("type in (10, 11)"));
    }

    @Test
    public void insuranceProjectEnabledMigration_shouldAddSchemeAndMonthProjectColumns() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("docs/sql/2026-08-22_hrm_insurance_project_is_enabled.sql")),
                StandardCharsets.UTF_8);

        Assert.assertTrue(sql.contains("hrm_insurance_project"));
        Assert.assertTrue(sql.contains("hrm_insurance_month_emp_project_record"));
        Assert.assertTrue(sql.contains("is_enabled"));
        Assert.assertTrue(sql.contains("DEFAULT 1"));
    }

    private void assertSchemeCountUsesEnabledProjectsOnly(String mapperPath) throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get(mapperPath)), StandardCharsets.UTF_8);
        String countSql = extractSql(xml, "InsuranceSchemeCount").toLowerCase(Locale.ROOT);

        Assert.assertFalse("社保方案合计不得读取基本工资金额设置", countSql.contains("hrm_salary_basic"));
        Assert.assertFalse("个人社保合计不得自动累加长期护理保险金额设置", countSql.contains("long_term_care_insurance_amount"));
        Assert.assertFalse("公司社保合计不得自动累加大额医疗保险金额设置", countSql.contains("large_medical_insurance_amount"));
        Assert.assertFalse("社保方案合计不需要旧方案长期护理默认金额兜底", countSql.contains("not exists"));
        Assert.assertTrue("社保方案合计必须按启用状态过滤", countOccurrences(countSql, "is_enabled") >= 4);
        Assert.assertTrue("社保方案合计旧数据 is_enabled 为空时应按启用兼容", countSql.contains("coalesce("));
        Assert.assertTrue("社保合计必须继续按项目类型区分社保", countSql.contains("type not in (10, 11)"));
        Assert.assertTrue("公积金合计必须继续按项目类型区分公积金", countSql.contains("type in (10, 11)"));
    }

    private void assertUsageEmployeeNamesIncluded(String mapperPath, String selectId) throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get(mapperPath)), StandardCharsets.UTF_8);
        String select = extractSelect(xml, selectId);

        Assert.assertTrue("社保方案列表必须返回使用人员名单字段", select.contains("useEmployeeNames"));
        Assert.assertTrue("使用人员名单必须从员工姓名聚合", select.contains("e.employee_name"));
        Assert.assertTrue("使用人员名单必须用 GROUP_CONCAT 聚合", select.contains("GROUP_CONCAT"));
        Assert.assertTrue("使用人员名单需要中文顿号分隔", select.contains("SEPARATOR '、'"));
        Assert.assertTrue("使用人员名单应与使用人数使用同一未删除员工口径", select.contains("e.is_del = 0"));
        Assert.assertTrue("使用人员名单聚合前必须支持调高 group_concat_max_len", xml.contains("group_concat_max_len"));
    }

    private String extractSql(String xml, String id) {
        String sqlStart = "<sql id=\"" + id + "\"";
        int startIndex = xml.indexOf(sqlStart);
        Assert.assertTrue(id + " sql must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</sql>", startIndex);
        Assert.assertTrue(id + " sql must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
    }

    private String extractSelect(String xml, String id) {
        String selectStart = "<select id=\"" + id + "\"";
        int startIndex = xml.indexOf(selectStart);
        Assert.assertTrue(id + " select must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</select>", startIndex);
        Assert.assertTrue(id + " select must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = text.indexOf(pattern);
        while (index >= 0) {
            count++;
            index = text.indexOf(pattern, index + pattern.length());
        }
        return count;
    }
}
