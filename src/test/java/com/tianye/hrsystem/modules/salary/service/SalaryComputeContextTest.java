package com.tianye.hrsystem.modules.salary.service;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;

public class SalaryComputeContextTest {

    @Test
    public void builder_shouldCreateContextWithAllFields() {
        SalaryComputeContext ctx = SalaryComputeContext.builder()
            .year(2025).month(6)
            .isSyncInsuranceData(true)
            .isSyncAttendanceData(false)
            .attendanceDataMap(Collections.emptyMap())
            .noFixedSalaryOptionList(Collections.emptyList())
            .produceAttendanceMap(Collections.emptyMap())
            .normalDaysByDeptType(Collections.emptyMap())
            .optionParentCodeMap(Collections.emptyMap())
            .lastMonthTaxDataMap(Collections.emptyMap())
            .lastYearAccumulatedIncomeMap(Collections.emptyMap())
            .socialSecurityEmpRecordMap(Collections.emptyMap())
            .additionalDeductionMap(Collections.emptyMap())
            .midMonthArchivesOptionMap(Collections.emptyMap())
            .build();

        Assert.assertEquals(2025, ctx.getYear());
        Assert.assertEquals(6, ctx.getMonth());
        Assert.assertTrue(ctx.getIsSyncInsuranceData());
        Assert.assertFalse(ctx.getIsSyncAttendanceData());
        Assert.assertNotNull(ctx.getAttendanceDataMap());
    }

    @Test
    public void builder_nullMaps_shouldDefaultToEmpty() {
        SalaryComputeContext ctx = SalaryComputeContext.builder()
            .year(2025).month(1)
            .build();

        Assert.assertNotNull(ctx.getAttendanceDataMap());
        Assert.assertTrue(ctx.getAttendanceDataMap().isEmpty());
        Assert.assertNotNull(ctx.getLastMonthTaxDataMap());
        Assert.assertTrue(ctx.getLastMonthTaxDataMap().isEmpty());
        Assert.assertNotNull(ctx.getNoFixedSalaryOptionList());
        Assert.assertTrue(ctx.getNoFixedSalaryOptionList().isEmpty());
        Assert.assertNotNull(ctx.getProduceAttendanceMap());
        Assert.assertTrue(ctx.getProduceAttendanceMap().isEmpty());
        Assert.assertNotNull(ctx.getNormalDaysByDeptType());
        Assert.assertTrue(ctx.getNormalDaysByDeptType().isEmpty());
        Assert.assertNotNull(ctx.getOptionParentCodeMap());
        Assert.assertTrue(ctx.getOptionParentCodeMap().isEmpty());
        Assert.assertNotNull(ctx.getLastYearAccumulatedIncomeMap());
        Assert.assertTrue(ctx.getLastYearAccumulatedIncomeMap().isEmpty());
        Assert.assertNotNull(ctx.getSocialSecurityEmpRecordMap());
        Assert.assertTrue(ctx.getSocialSecurityEmpRecordMap().isEmpty());
        Assert.assertNotNull(ctx.getAdditionalDeductionMap());
        Assert.assertTrue(ctx.getAdditionalDeductionMap().isEmpty());
        Assert.assertNotNull(ctx.getMidMonthArchivesOptionMap());
        Assert.assertTrue(ctx.getMidMonthArchivesOptionMap().isEmpty());
        Assert.assertNotNull(ctx.getHasAttendanceGroupMap());
        Assert.assertTrue(ctx.getHasAttendanceGroupMap().isEmpty());
        Assert.assertNotNull(ctx.getSalaryOptionConfigMap());
        Assert.assertTrue(ctx.getSalaryOptionConfigMap().isEmpty());
    }

    @Test
    public void builder_reuse_shouldProduceIndependentContexts() {
        SalaryComputeContext.Builder builder = SalaryComputeContext.builder()
            .year(2025).month(1)
            .isSyncInsuranceData(true);

        SalaryComputeContext ctx1 = builder.build();
        SalaryComputeContext ctx2 = builder.month(6).isSyncInsuranceData(false).build();

        Assert.assertEquals(1, ctx1.getMonth());
        Assert.assertTrue(ctx1.getIsSyncInsuranceData());
        Assert.assertEquals(6, ctx2.getMonth());
        Assert.assertFalse(ctx2.getIsSyncInsuranceData());
    }
}
