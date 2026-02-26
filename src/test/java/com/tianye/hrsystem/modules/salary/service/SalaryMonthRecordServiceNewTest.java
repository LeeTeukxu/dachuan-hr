package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryOption;
import com.tianye.hrsystem.modules.salary.service.SalaryMonthRecordServiceNew;
import org.junit.Assert;
import org.junit.Test;

import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SalaryMonthRecordServiceNewTest {

    @Test
    public void filterNoFixedSalaryOptions_shouldNotMutateInputAndExcludeCodes() {
        List<HrmSalaryOption> source = Arrays.asList(
                new HrmSalaryOption().setCode(10101),
                new HrmSalaryOption().setCode(100101),
                new HrmSalaryOption().setCode(40102),
                new HrmSalaryOption().setCode(281),
                new HrmSalaryOption().setCode(20102),
                new HrmSalaryOption().setCode(190101)
        );

        List<HrmSalaryOption> filtered = SalaryMonthRecordServiceNew.filterNoFixedSalaryOptions(source);

        List<Integer> filteredCodes = filtered.stream().map(HrmSalaryOption::getCode).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(10101, 190101), filteredCodes);

        List<Integer> sourceCodes = source.stream().map(HrmSalaryOption::getCode).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(10101, 100101, 40102, 281, 20102, 190101), sourceCodes);
    }

    @Test
    public void resolveAttendanceData_shouldContainNeedAndActualWorkDayDefaults() {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        Map<String, Object> employee = new HashMap<>();
        employee.put("jobNumber", "A001");
        List<Map<String, Object>> employeeList = Collections.singletonList(employee);

        Map<String, Map<Integer, String>> result = service.resolveAttendanceData(employeeList);

        Assert.assertTrue(result.containsKey("A001"));
        Map<Integer, String> attendanceMap = result.get("A001");
        Assert.assertNotNull(attendanceMap);
        Assert.assertEquals("0", attendanceMap.get(1));
        Assert.assertEquals("0", attendanceMap.get(2));
    }

    @Test
    public void resolveAttendanceData_shouldContainRequiredFixedCodes() {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        Map<String, Object> employee = new HashMap<>();
        employee.put("jobNumber", "A001");

        Map<Integer, String> attendanceMap = service.resolveAttendanceData(Collections.singletonList(employee)).get("A001");

        List<Integer> requiredCodes = Arrays.asList(180101, 190101, 190102, 190103, 19010401, 19010402,
                190105, 190106, 280, 281, 282, 20102, 20105, 40102);
        for (Integer requiredCode : requiredCodes) {
            Assert.assertEquals("0", attendanceMap.get(requiredCode));
        }
    }

    @Test
    public void collectAdditionalIdsToDelete_shouldDeleteUnconfiguredAndDuplicateRows() {
        List<HrmAdditional> existingList = Arrays.asList(
                new HrmAdditional().setAdditionalId(1L).setEmployeeId(100L),
                new HrmAdditional().setAdditionalId(2L).setEmployeeId(100L),
                new HrmAdditional().setAdditionalId(3L).setEmployeeId(200L),
                new HrmAdditional().setAdditionalId(4L).setEmployeeId(300L)
        );

        List<Long> deleteIds = SalaryMonthRecordServiceNew.collectAdditionalIdsToDelete(existingList,
                new java.util.HashSet<>(Arrays.asList(100L, 300L)));

        Assert.assertEquals(Arrays.asList(2L, 3L), deleteIds);
    }

    @Test
    public void isMidMonthPromotion_shouldFollowBoundaryRules() {
        Assert.assertFalse(SalaryMonthRecordServiceNew.isMidMonthPromotion(null, 2026, 2));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isMidMonthPromotion(LocalDate.of(2026, 2, 1), 2026, 2));
        Assert.assertTrue(SalaryMonthRecordServiceNew.isMidMonthPromotion(LocalDate.of(2026, 2, 10), 2026, 2));
        Assert.assertTrue(SalaryMonthRecordServiceNew.isMidMonthPromotion(LocalDate.of(2026, 2, 28), 2026, 2));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isMidMonthPromotion(LocalDate.of(2026, 3, 1), 2026, 2));
    }

    @Test
    public void resolveSocialSecurityReferenceYearMonth_shouldFollowConfig() {
        Assert.assertEquals(YearMonth.of(2026, 2),
                SalaryMonthRecordServiceNew.resolveSocialSecurityReferenceYearMonth(null, 2026, 2));
        Assert.assertEquals(YearMonth.of(2026, 1),
                SalaryMonthRecordServiceNew.resolveSocialSecurityReferenceYearMonth(0, 2026, 2));
        Assert.assertEquals(YearMonth.of(2026, 2),
                SalaryMonthRecordServiceNew.resolveSocialSecurityReferenceYearMonth(1, 2026, 2));
        Assert.assertEquals(YearMonth.of(2026, 3),
                SalaryMonthRecordServiceNew.resolveSocialSecurityReferenceYearMonth(2, 2026, 2));
    }

    @Test
    public void calculateMidMonthPromotionSalaryAmounts_shouldComputeExpectedTotals() {
        Map<Integer, String> probationSalaryMap = buildSalaryMap("2000", "1000", "500");
        Map<Integer, String> officialSalaryMap = buildSalaryMap("3000", "1500", "700");

        HrmProduceAttendance attendance = new HrmProduceAttendance()
                .setProbationAttendance(new BigDecimal("10"))
                .setPositiveAttendance(new BigDecimal("11.75"))
                .setWorkOverTime(new BigDecimal("5"))
                .setNightSubsidy(new BigDecimal("100"))
                .setOtherSubsidies(new BigDecimal("50"))
                .setHighTemperature(new BigDecimal("30"))
                .setLowTemperature(new BigDecimal("20"));

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSalaryAmounts(
                probationSalaryMap,
                officialSalaryMap,
                attendance,
                new BigDecimal("21.75")
        );

        BigDecimal probationTotal = new BigDecimal("2000").add(new BigDecimal("1000")).add(new BigDecimal("500"));
        BigDecimal officialTotal = new BigDecimal("3000").add(new BigDecimal("1500")).add(new BigDecimal("700"));

        BigDecimal expectedProbationShouldPay = probationTotal
                .multiply(new BigDecimal("10"))
                .divide(new BigDecimal("21.75"), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal expectedOfficialShouldPay = officialTotal
                .multiply(new BigDecimal("11.75"))
                .divide(new BigDecimal("21.75"), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal expectedSubsidies = new BigDecimal("200");
        BigDecimal expectedOvertime = new BigDecimal("60");
        BigDecimal expectedShouldPay = expectedProbationShouldPay
                .add(expectedOfficialShouldPay)
                .add(expectedSubsidies)
                .add(expectedOvertime);

        Assert.assertEquals(0, new BigDecimal(result.get(999001)).compareTo(expectedProbationShouldPay));
        Assert.assertEquals(0, new BigDecimal(result.get(999002)).compareTo(expectedOfficialShouldPay));
        Assert.assertEquals(0, new BigDecimal(result.get(999003)).compareTo(expectedSubsidies));
        Assert.assertEquals(0, new BigDecimal(result.get(999004)).compareTo(expectedOvertime));
        Assert.assertEquals(0, new BigDecimal(result.get(210101)).compareTo(expectedShouldPay));
        Assert.assertEquals("3000", result.get(10101));
        Assert.assertEquals("1500", result.get(10102));
        Assert.assertEquals("700", result.get(10103));
    }

    @Test
    public void calculateMidMonthPromotionSummary_shouldIncludeBonusWhenConfigured() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "0");
        lastTaxMap.put(250102, "0");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "0");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal(6000),
                new BigDecimal(500),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(1000),
                true,
                true,
                false,
                lastTaxMap,
                6
        );

        Assert.assertEquals(0, new BigDecimal(result.get(220101)).compareTo(new BigDecimal("500")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("45.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5455.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(1001)).compareTo(new BigDecimal("545.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(270101)).compareTo(new BigDecimal("7000")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_shouldApplySpecialTaxAndTaxAfterPay() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "0");
        lastTaxMap.put(250102, "0");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "0");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal(6000),
                new BigDecimal(500),
                new BigDecimal(100),
                BigDecimal.ZERO,
                new BigDecimal(200),
                new BigDecimal(300),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                true,
                false,
                lastTaxMap,
                6
        );

        Assert.assertEquals(0, new BigDecimal(result.get(220101)).compareTo(new BigDecimal("800")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("15.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5585.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(1001)).compareTo(new BigDecimal("615.00")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_shouldSkipTaxForRemarkRule() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "0");
        lastTaxMap.put(250102, "0");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "0");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal(6000),
                new BigDecimal(500),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                true,
                true,
                lastTaxMap,
                6
        );

        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5500.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(1001)).compareTo(new BigDecimal("500.00")));
    }

    private Map<Integer, String> buildSalaryMap(String basic, String post, String duty) {
        return Arrays.asList(
                new Integer[]{10101, Integer.parseInt(basic)},
                new Integer[]{10102, Integer.parseInt(post)},
                new Integer[]{10103, Integer.parseInt(duty)}
        ).stream().collect(Collectors.toMap(v -> v[0], v -> String.valueOf(v[1])));
    }

    private HrmSalaryMonthOptionValue buildOptionValue(int code, String value) {
        HrmSalaryMonthOptionValue ov = new HrmSalaryMonthOptionValue();
        ov.setCode(code);
        ov.setValue(value);
        return ov;
    }

    @Test
    public void processMidMonthPromotion_shouldUseCompleteDeductionContext() {
        List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
        allOptions.add(buildOptionValue(100101, "500"));   // 社保
        allOptions.add(buildOptionValue(100102, "300"));   // 公积金
        allOptions.add(buildOptionValue(280, "200"));      // 其他扣款
        allOptions.add(buildOptionValue(282, "100"));      // 借款
        allOptions.add(buildOptionValue(210101, "8000"));   // 应发（将被覆盖）
        allOptions.add(buildOptionValue(230101, "0"));      // 个税
        allOptions.add(buildOptionValue(240101, "0"));      // 实发

        Map<Integer, String> midMonthSalaryMap = new HashMap<>();
        midMonthSalaryMap.put(210101, "6000");
        midMonthSalaryMap.put(10101, "3000");
        midMonthSalaryMap.put(10102, "1500");
        midMonthSalaryMap.put(10103, "700");
        midMonthSalaryMap.put(999004, "60");

        Map<Integer, String> lastMonthTaxData = new HashMap<>();
        lastMonthTaxData.put(250101, "0");
        lastMonthTaxData.put(250102, "0");
        lastMonthTaxData.put(250103, "0");
        lastMonthTaxData.put(250105, "0");

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 6, "2", false, null
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals("6000", resultMap.get(210101).getValue());

        // 应税所得 = 6000 - 5000 - 800 = 200, 税 = 200 * 3% = 6.00
        BigDecimal expectedTax = new BigDecimal("6.00");
        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(expectedTax));

        // 实发 = 6000 - 800(社保公积金) - 6(税) - 200(其他扣款) - 100(借款) = 4894.00
        BigDecimal expectedRealPay = new BigDecimal("4894.00");
        Assert.assertEquals(0, new BigDecimal(resultMap.get(240101).getValue()).compareTo(expectedRealPay));
    }

    @Test
    public void processMidMonthPromotion_disabledEmployee_shouldSkipTax() {
        List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
        allOptions.add(buildOptionValue(100101, "500"));
        allOptions.add(buildOptionValue(100102, "300"));
        allOptions.add(buildOptionValue(280, "0"));
        allOptions.add(buildOptionValue(282, "0"));
        allOptions.add(buildOptionValue(210101, "8000"));
        allOptions.add(buildOptionValue(230101, "0"));
        allOptions.add(buildOptionValue(240101, "0"));

        Map<Integer, String> midMonthSalaryMap = new HashMap<>();
        midMonthSalaryMap.put(210101, "6000");
        midMonthSalaryMap.put(10101, "3000");
        midMonthSalaryMap.put(10102, "1500");
        midMonthSalaryMap.put(10103, "700");
        midMonthSalaryMap.put(999004, "60");

        Map<Integer, String> lastMonthTaxData = new HashMap<>();
        lastMonthTaxData.put(250101, "0");
        lastMonthTaxData.put(250102, "0");
        lastMonthTaxData.put(250103, "0");
        lastMonthTaxData.put(250105, "0");

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 6, "1", false, null
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, new BigDecimal(resultMap.get(240101).getValue()).compareTo(new BigDecimal("5200")));
    }

    @Test
    public void processMidMonthPromotion_remarkEmployee_under60k_shouldSkipTax() {
        List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
        allOptions.add(buildOptionValue(100101, "200"));
        allOptions.add(buildOptionValue(100102, "100"));
        allOptions.add(buildOptionValue(280, "0"));
        allOptions.add(buildOptionValue(282, "0"));
        allOptions.add(buildOptionValue(210101, "4000"));
        allOptions.add(buildOptionValue(230101, "0"));
        allOptions.add(buildOptionValue(240101, "0"));

        Map<Integer, String> midMonthSalaryMap = new HashMap<>();
        midMonthSalaryMap.put(210101, "3000");
        midMonthSalaryMap.put(10101, "2000");
        midMonthSalaryMap.put(10102, "500");
        midMonthSalaryMap.put(10103, "300");
        midMonthSalaryMap.put(999004, "0");

        Map<Integer, String> lastMonthTaxData = new HashMap<>();
        lastMonthTaxData.put(250101, "10000");
        lastMonthTaxData.put(250102, "5000");
        lastMonthTaxData.put(250103, "1000");
        lastMonthTaxData.put(250105, "0");

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 3, "2", true, null
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void recalculateMidMonthPromotion_emptyMidMonthMap_shouldNotModifyOptions() {
        List<HrmSalaryMonthOptionValue> options = new ArrayList<>();
        options.add(buildOptionValue(210101, "8000"));
        options.add(buildOptionValue(230101, "100"));
        options.add(buildOptionValue(240101, "7000"));

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            options, Collections.emptyMap(), null, 2026, 6, "2", false, null
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = options.stream()
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity()));
        Assert.assertEquals("8000", resultMap.get(210101).getValue());
        Assert.assertEquals("100", resultMap.get(230101).getValue());
        Assert.assertEquals("7000", resultMap.get(240101).getValue());
    }

    @Test
    public void recalculateMidMonthPromotion_december_shouldResetCumulative() {
        List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
        allOptions.add(buildOptionValue(100101, "500"));
        allOptions.add(buildOptionValue(100102, "300"));
        allOptions.add(buildOptionValue(280, "0"));
        allOptions.add(buildOptionValue(282, "0"));
        allOptions.add(buildOptionValue(210101, "8000"));
        allOptions.add(buildOptionValue(230101, "0"));
        allOptions.add(buildOptionValue(240101, "0"));

        Map<Integer, String> midMonthSalaryMap = new HashMap<>();
        midMonthSalaryMap.put(210101, "6000");
        midMonthSalaryMap.put(10101, "3000");
        midMonthSalaryMap.put(10102, "1500");
        midMonthSalaryMap.put(10103, "700");
        midMonthSalaryMap.put(999004, "60");

        Map<Integer, String> lastMonthTaxData = new HashMap<>();
        lastMonthTaxData.put(250101, "100000");
        lastMonthTaxData.put(250102, "55000");
        lastMonthTaxData.put(250103, "10000");
        lastMonthTaxData.put(250105, "5000");

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 12, "2", false, null
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(270101).getValue()).compareTo(new BigDecimal("6000")));
        Assert.assertEquals(0, new BigDecimal(resultMap.get(270102).getValue()).compareTo(new BigDecimal("5000")));
    }

    @Test
    public void calculateMidMonthPromotionSalaryAmounts_nullAttendance_shouldReturnEmptyMap() {
        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSalaryAmounts(
            buildSalaryMap("3000", "1500", "700"),
            buildSalaryMap("4000", "2000", "900"),
            null,
            new BigDecimal("21.75")
        );
        Assert.assertTrue("缺考勤分段应返回空Map", result.isEmpty());
    }

    @Test
    public void collectMidMonthPromotionAttendanceReview_shouldCollectWhenSplitAttendanceMissing() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "collectMidMonthPromotionAttendanceReview",
                Map.class, int.class, int.class, LocalDate.class,
                HrmProduceAttendance.class, java.util.Set.class
        );
        method.setAccessible(true);

        Map<String, Object> employeeMap = new HashMap<>();
        employeeMap.put("employeeId", 1001L);
        employeeMap.put("employeeName", "张三");
        employeeMap.put("jobNumber", "A001");

        LinkedHashSet<String> reviewSet = new LinkedHashSet<>();
        method.invoke(service, employeeMap, 2026, 2, LocalDate.of(2026, 2, 10), null, reviewSet);

        Assert.assertEquals(1, reviewSet.size());
        String first = reviewSet.iterator().next();
        Assert.assertTrue(first.contains("employeeId=1001"));
        Assert.assertTrue(first.contains("name=张三"));
        Assert.assertTrue(first.contains("jobNumber=A001"));
    }
}
