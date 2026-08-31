package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.po.HrmAttendanceRule;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryDayVo;
import com.tianye.hrsystem.entity.vo.HrmAttendanceSummaryVo;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryExport;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryOption;
import com.tianye.hrsystem.modules.salary.support.SalaryExportCommentWriteHandler;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryPageListVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;

import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Method;
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
    public void isProductionAffiliationSystem_shouldUseEmployeeAffiliationAndIgnoreLegacyEmployeeOverrides() {
        Map<String, Object> legacyForcedProductionEmployee = new HashMap<>();
        legacyForcedProductionEmployee.put("employeeId", 1712718940198L);
        legacyForcedProductionEmployee.put("affiliationSystem", 1);
        legacyForcedProductionEmployee.put("isProduceDept", "1");

        Map<String, Object> legacyForcedAdministrativeEmployee = new HashMap<>();
        legacyForcedAdministrativeEmployee.put("employeeId", 1712718940227L);
        legacyForcedAdministrativeEmployee.put("affiliationSystem", 2);
        legacyForcedAdministrativeEmployee.put("isProduceDept", "0");

        Assert.assertFalse(SalaryMonthRecordServiceNew.isProductionAffiliationSystem(legacyForcedProductionEmployee));
        Assert.assertTrue(SalaryMonthRecordServiceNew.isProductionAffiliationSystem(legacyForcedAdministrativeEmployee));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isProductionAffiliationSystem(1));
        Assert.assertTrue(SalaryMonthRecordServiceNew.isProductionAffiliationSystem(2));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isProductionAffiliationSystem((Integer) null));
    }

    @Test
    public void isFixedRestProductionEmployee_shouldRequireProductionAffiliationAndFixedMonthlyRest() {
        Map<String, Object> fixedRestProductionEmployee = new HashMap<>();
        fixedRestProductionEmployee.put("affiliationSystem", 2);
        fixedRestProductionEmployee.put("restType", 2);

        Map<String, Object> productionDoubleRestEmployee = new HashMap<>();
        productionDoubleRestEmployee.put("affiliationSystem", 2);
        productionDoubleRestEmployee.put("restType", 1);

        Map<String, Object> administrativeFixedRestEmployee = new HashMap<>();
        administrativeFixedRestEmployee.put("affiliationSystem", 1);
        administrativeFixedRestEmployee.put("restType", 2);

        Assert.assertTrue(SalaryMonthRecordServiceNew.isFixedRestProductionEmployee(fixedRestProductionEmployee));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isFixedRestProductionEmployee(productionDoubleRestEmployee));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isFixedRestProductionEmployee(administrativeFixedRestEmployee));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isFixedRestProductionEmployee(2, null));
        Assert.assertFalse(SalaryMonthRecordServiceNew.isFixedRestProductionEmployee(null, 2));
    }

    @Test
    public void salaryMonthRecordServiceSource_shouldNotContainLegacyEmployeeIdDepartmentOverrides() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java")),
                StandardCharsets.UTF_8);

        for (String legacyEmployeeId : Arrays.asList(
                "1712718940198", "1712718940199", "1712718940200",
                "1712718940227", "1831601326890434591", "1831601326890434610", "1831601326890434648",
                "1712718940217", "1712718940220", "1712718940186",
                "1712718940327", "1712718940184")) {
            Assert.assertFalse("薪资核算行政/生产体系逻辑不得再写死员工ID: " + legacyEmployeeId,
                    source.contains(legacyEmployeeId));
        }
    }

    @Test
    public void legacySalaryMonthRecordServiceSource_shouldNotContainDepartmentNameProductionRules() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordService_Bak.java")),
                StandardCharsets.UTF_8);

        for (String forbidden : Arrays.asList(
                "isProduceDept", "质量部", "仓库", "质检部", "设备技术部", "生产部", "仓储部", "研发部", "工程部",
                "daysInMonth -4")) {
            Assert.assertFalse("活动薪资旧服务不得再按部门名称或固定休息天数维护生产体系规则: " + forbidden,
                    source.contains(forbidden));
        }
    }

    @Test
    public void legacySalaryMonthRecordServiceSource_shouldGateOvertimeNightByFixedRestProductionRule() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordService_Bak.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("旧薪资服务加班/夜班也必须按生产体系且固定月休4天判断",
                source.contains("isFixedRestProductionEmployee(map)"));
        Assert.assertTrue("旧薪资服务必须用 canCountOvertimeNight 保护加班费和夜班补贴",
                source.contains("if (!canCountOvertimeNight)"));
    }

    @Test
    public void salaryComputeSources_shouldNotContainHardcodedDefaultAttendanceDays() throws Exception {
        for (String sourcePath : Arrays.asList(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java",
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordService_Bak.java",
                "src/main/java/com/tianye/hrsystem/imple/HrmSalaryMonthRecordServiceImpl.java")) {
            String source = new String(Files.readAllBytes(Paths.get(sourcePath)), StandardCharsets.UTF_8);

            Assert.assertFalse("薪资核算源码不得再硬编码默认出勤天数: " + sourcePath,
                    source.contains("21.75"));
        }
    }

    @Test
    public void salaryMonthRecordServiceSource_shouldUseEmployeeFullMoneyForAllFullAttendanceAwardBranches() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java")),
                StandardCharsets.UTF_8);

        Assert.assertFalse("薪资核算不得在全勤奖分支直接读取基本工资设置的领导全勤金额",
                source.contains("HrmSalaryBasicDefaults.leaderFullAttendanceAmount(batchData.salaryBasic)"));
        Assert.assertFalse("薪资核算不得在全勤奖分支直接读取基本工资设置的普通员工全勤金额",
                source.contains("HrmSalaryBasicDefaults.ordinaryFullAttendanceAmount(batchData.salaryBasic)"));
        Assert.assertTrue("全勤奖写入必须统一使用计薪员工查询得到的 fullMoney",
                source.contains("empAttendanceMap.put(40102, fullMoney.toPlainString())"));
    }

    @Test
    public void legacySalaryMonthRecordServiceSource_shouldParseDecimalFullMoneyWithoutLongCast() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordService_Bak.java")),
                StandardCharsets.UTF_8);

        Assert.assertFalse("旧薪资服务不得把 fullMoney 强转 Long，否则员工级 DECIMAL 金额会 ClassCastException",
                source.contains("(Long)map.get(\"fullMoney\")"));
        Assert.assertTrue("旧薪资服务应复用新服务的 fullMoney 兼容解析",
                source.contains("SalaryMonthRecordServiceNew.resolveFullAttendanceMoney(map.get(\"fullMoney\"))"));
    }

    @Test
    public void salaryMonthRecordServiceSource_shouldNotUseAttendanceInfoOrInternalSystemType() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java")),
                StandardCharsets.UTF_8);

        for (String forbidden : Arrays.asList(
                "HrmAttendance" + "InfoMapper",
                "HrmAttendance" + "Info",
                "hrmAttendance" + "InfoMapper",
                "dept" + "Type",
                "Dept" + "Type",
                "SALARY_DEPT_" + "TYPE",
                "resolveSalaryDept" + "Type",
                "normalDaysByDept" + "Type",
                "queryNormalDaysByDept" + "Type",
                "loadNormalDaysByDept" + "Type",
                "resolveNormalDaysByDept" + "Type",
                "getNormalDaysByDept" + "Type")) {
            Assert.assertFalse("薪资核算不得再通过考勤配置或内部类型判断行政生产体系: " + forbidden,
                    source.contains(forbidden));
        }
    }

    @Test
    public void normalizeComputeEmployeeIds_shouldUseSelectedEmployeeIdsAndDeduplicate() {
        List<Long> employeeIds = SalaryMonthRecordServiceNew.normalizeComputeEmployeeIds(
                999L, Arrays.asList(101L, 102L, 101L, null, 103L));

        Assert.assertEquals(Arrays.asList(101L, 102L, 103L), employeeIds);
    }

    @Test
    public void normalizeComputeEmployeeIds_shouldFallbackToSingleEmployeeId() {
        List<Long> employeeIds = SalaryMonthRecordServiceNew.normalizeComputeEmployeeIds(101L, null);

        Assert.assertEquals(Collections.singletonList(101L), employeeIds);
    }

    @Test
    public void normalizeComputeEmployeeIds_shouldRejectMoreThanFiftyEmployees() {
        List<Long> employeeIds = new ArrayList<>();
        for (long i = 1; i <= 51; i++) {
            employeeIds.add(i);
        }

        try {
            SalaryMonthRecordServiceNew.normalizeComputeEmployeeIds(null, employeeIds);
            Assert.fail("expected HrmException");
        } catch (HrmException ex) {
            Assert.assertTrue(ex.getMsg().contains("不能超过50人"));
        }
    }

    @Test
    public void buildComputeScopeKey_shouldUseBatchEmployeeIds() {
        Assert.assertEquals("BATCH:101,102",
                SalaryMonthRecordServiceNew.buildComputeScopeKey(Arrays.asList(101L, 102L)));
        Assert.assertEquals("ALL", SalaryMonthRecordServiceNew.buildComputeScopeKey(Collections.emptyList()));
    }

    @Test
    public void resolveExportEmployeeIds_shouldUseSelectedEmployeeIdsAndKeepCandidateOrder() {
        List<Long> employeeIds = SalaryMonthRecordServiceNew.resolveExportEmployeeIds(
                Arrays.asList(101L, 102L, 103L, 104L),
                Arrays.asList(103L, 101L, 999L, 103L, null));

        Assert.assertEquals(Arrays.asList(101L, 103L), employeeIds);
    }

    @Test
    public void resolveExportEmployeeIds_shouldFallbackToCandidatesWhenSelectionEmpty() {
        List<Long> employeeIds = SalaryMonthRecordServiceNew.resolveExportEmployeeIds(
                Arrays.asList(101L, 102L, 103L),
                Collections.emptyList());

        Assert.assertEquals(Arrays.asList(101L, 102L, 103L), employeeIds);
    }

    @Test
    public void resolveExportEmployeeIds_shouldReturnEmptyWhenSelectionHasNoCandidateMatch() {
        List<Long> employeeIds = SalaryMonthRecordServiceNew.resolveExportEmployeeIds(
                Arrays.asList(101L, 102L, 103L),
                Arrays.asList(999L));

        Assert.assertTrue(employeeIds.isEmpty());
    }

    @Test
    public void salaryExportSource_shouldApplySelectedEmployeeIdsBeforeQueryingMonthList() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("薪资导出必须在查询月薪资明细前应用用户选择的人员范围",
                source.contains("resolveExportEmployeeIds(employeeIds, querySalaryExportDto.getEmployeeIds())"));
    }

    @Test
    public void salaryExportComments_shouldExplainTargetSalaryCells() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(4);
            for (int column = 0; column < 28; column++) {
                row.createCell(column).setCellValue("");
            }

            HrmSalaryExport export = new HrmSalaryExport();
            export.setEmpname("张三");
            export.setNormaldays("24.00");
            export.setAbsencehours(new BigDecimal("1.00"));
            export.setFullattendancesalary(BigDecimal.ZERO);
            export.setAbsencesalary(new BigDecimal("-229.89"));
            export.setTotalsalary(new BigDecimal("5000.00"));
            export.setTax(new BigDecimal("2.40"));
            export.setUnionfees(BigDecimal.ZERO);

            SalaryExportCommentWriteHandler.applySalaryExportComments(row, export);

            assertCellCommentContains(row.getCell(17), "40102", "未生成", "有效病假", "应计出勤");
            assertCellCommentContains(row.getCell(19), "200101", "应出勤天数", "应计出勤小时", "当前超缺勤天数：1.00");
            assertCellCommentContains(row.getCell(21), "230101", "累计预扣法", "累计已缴税额", "当前个税：2.40");
            assertCellCommentContains(row.getCell(25), "160102", "应发工资 * 0.5%", "实习/离职", "半路转正");
            Assert.assertNull("非目标薪资字段不应添加批注", row.getCell(0).getCellComment());
        }
    }

    @Test
    public void salaryExportComments_shouldSkipSubtotalAndTotalRows() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row subtotalRow = sheet.createRow(4);
            Row totalRow = sheet.createRow(5);
            for (int column : Arrays.asList(17, 19, 21, 25)) {
                subtotalRow.createCell(column).setCellValue("");
                totalRow.createCell(column).setCellValue("");
            }

            HrmSalaryExport subtotal = new HrmSalaryExport();
            subtotal.setEmpname("小计");
            HrmSalaryExport total = new HrmSalaryExport();
            total.setEmpname("合计");

            SalaryExportCommentWriteHandler.applySalaryExportComments(subtotalRow, subtotal);
            SalaryExportCommentWriteHandler.applySalaryExportComments(totalRow, total);

            for (int column : Arrays.asList(17, 19, 21, 25)) {
                Assert.assertNull(subtotalRow.getCell(column).getCellComment());
                Assert.assertNull(totalRow.getCell(column).getCellComment());
            }
        }
    }

    @Test
    public void salaryExportSource_shouldRegisterCommentWriteHandler() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("薪资导出 EasyExcel 写入时必须注册工资字段批注处理器",
                source.contains("new SalaryExportCommentWriteHandler(salaryExportList)"));
    }

    private static void assertCellCommentContains(Cell cell, String... fragments) {
        Assert.assertNotNull("目标单元格必须存在", cell);
        Assert.assertNotNull("目标单元格必须带批注", cell.getCellComment());
        String comment = cell.getCellComment().getString().getString();
        for (String fragment : fragments) {
            Assert.assertTrue("批注应包含：" + fragment + "\n实际批注：" + comment,
                    comment.contains(fragment));
        }
    }

    @Test
    public void getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "getYeBanAndJiaBan",
                HrmProduceAttendance.class,
                Map.class,
                HrmSalaryBasic.class,
                boolean.class
        );
        method.setAccessible(true);

        HrmProduceAttendance attendance = new HrmProduceAttendance()
                .setWorkOverTime(new BigDecimal("10"))
                .setOvertimePay(new BigDecimal("128.50"));
        HrmSalaryBasic salaryBasic = new HrmSalaryBasic().setOvertimePay(new BigDecimal("20"));
        Map<Integer, String> empAttendanceMap = new HashMap<>();

        method.invoke(service, attendance, empAttendanceMap, salaryBasic, true);

        Assert.assertEquals("128.50", empAttendanceMap.get(180101));
    }

    @Test
    public void getYeBanAndJiaBan_shouldCalculateNightSubsidyFromNightShiftWhenUploadedSubsidyMissing() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "getYeBanAndJiaBan",
                HrmProduceAttendance.class,
                Map.class,
                HrmSalaryBasic.class,
                boolean.class
        );
        method.setAccessible(true);

        HrmProduceAttendance attendance = new HrmProduceAttendance()
                .setNightShift(2);
        HrmSalaryBasic salaryBasic = new HrmSalaryBasic().setSubsidy(new BigDecimal("35"));
        Map<Integer, String> empAttendanceMap = new HashMap<>();

        method.invoke(service, attendance, empAttendanceMap, salaryBasic, true);

        Assert.assertEquals("70", empAttendanceMap.get(180102));
    }

    @Test
    public void getYeBanAndJiaBan_shouldNotCreateOvertimePayForAdministrativeEmployee() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "getYeBanAndJiaBan",
                HrmProduceAttendance.class,
                Map.class,
                HrmSalaryBasic.class,
                boolean.class
        );
        method.setAccessible(true);

        HrmProduceAttendance attendance = new HrmProduceAttendance()
                .setWorkOverTime(new BigDecimal("10"))
                .setOvertimePay(new BigDecimal("128.50"));
        HrmSalaryBasic salaryBasic = new HrmSalaryBasic().setOvertimePay(new BigDecimal("20"));
        Map<Integer, String> empAttendanceMap = new HashMap<>();

        method.invoke(service, attendance, empAttendanceMap, salaryBasic, false);

        Assert.assertEquals("0", empAttendanceMap.get(180101));
    }

    @Test
    public void getYeBanAndJiaBan_shouldZeroOvertimePayAndNightSubsidyWhenEmployeeCannotCountOvertimeNight() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "getYeBanAndJiaBan",
                HrmProduceAttendance.class,
                Map.class,
                HrmSalaryBasic.class,
                boolean.class
        );
        method.setAccessible(true);

        HrmProduceAttendance attendance = new HrmProduceAttendance()
                .setWorkOverTime(new BigDecimal("10"))
                .setOvertimePay(new BigDecimal("128.50"))
                .setNightShift(2)
                .setNightSubsidy(new BigDecimal("70.00"));
        HrmSalaryBasic salaryBasic = new HrmSalaryBasic()
                .setOvertimePay(new BigDecimal("20"))
                .setSubsidy(new BigDecimal("35"));
        Map<Integer, String> empAttendanceMap = new HashMap<>();

        method.invoke(service, attendance, empAttendanceMap, salaryBasic, false);

        Assert.assertEquals("0", empAttendanceMap.get(180101));
        Assert.assertEquals("0", empAttendanceMap.get(180102));
    }

    @Test
    public void filterNoFixedSalaryOptions_shouldNotMutateInputAndExcludeCodes() {
        List<HrmSalaryOption> source = Arrays.asList(
                new HrmSalaryOption().setCode(10101),
                new HrmSalaryOption().setCode(100101),
                new HrmSalaryOption().setCode(1001),
                new HrmSalaryOption().setCode(40102),
                new HrmSalaryOption().setCode(281),
                new HrmSalaryOption().setCode(160102),
                new HrmSalaryOption().setCode(20102),
                new HrmSalaryOption().setCode(41001),
                new HrmSalaryOption().setCode(190101)
        );

        List<HrmSalaryOption> filtered = SalaryMonthRecordServiceNew.filterNoFixedSalaryOptions(source);

        List<Integer> filteredCodes = filtered.stream().map(HrmSalaryOption::getCode).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(10101, 190101), filteredCodes);

        List<Integer> sourceCodes = source.stream().map(HrmSalaryOption::getCode).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(10101, 100101, 1001, 40102, 281, 160102, 20102, 41001, 190101), sourceCodes);
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
    public void resolveSalaryAttendanceDays_shouldPreferProduceAttendanceAccruedDays() {
        HrmProduceAttendance produceAttendance = new HrmProduceAttendance()
                .setPositiveAttendance(new BigDecimal("18.00"))
                .setProbationAttendance(new BigDecimal("25.00"));
        Map<Long, BigDecimal> overtimeFallback = new HashMap<>();
        overtimeFallback.put(101L, new BigDecimal("27.00"));

        BigDecimal resolved = SalaryMonthRecordServiceNew.resolveSalaryAttendanceDays(
                produceAttendance, overtimeFallback, 101L);

        Assert.assertEquals(new BigDecimal("25.00"), resolved);
    }

    @Test
    public void resolveSalaryAttendanceDays_shouldFallbackToOvertimeNightAccruedDays() {
        HrmProduceAttendance produceAttendance = new HrmProduceAttendance()
                .setPositiveAttendance(new BigDecimal("18.00"));
        Map<Long, BigDecimal> overtimeFallback = new HashMap<>();
        overtimeFallback.put(101L, new BigDecimal("26.50"));

        BigDecimal resolved = SalaryMonthRecordServiceNew.resolveSalaryAttendanceDays(
                produceAttendance, overtimeFallback, 101L);

        Assert.assertEquals(new BigDecimal("26.50"), resolved);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildExportFullWorkDaysByEmployee_shouldReadAccruedAttendanceFromOvertimeNightStatistics() {
        HrmOvertimeNightStatisticsDetail missingOneDay = new HrmOvertimeNightStatisticsDetail();
        missingOneDay.setEmployeeId(101L);
        missingOneDay.setExpectedAttendanceDays(25);
        missingOneDay.setAccruedAttendanceHours(new BigDecimal("192.00"));

        HrmOvertimeNightStatisticsDetail overAccrued = new HrmOvertimeNightStatisticsDetail();
        overAccrued.setEmployeeId(102L);
        overAccrued.setExpectedAttendanceDays(25);
        overAccrued.setAccruedAttendanceHours(new BigDecimal("224.00"));

        Map<Long, BigDecimal> result = SalaryMonthRecordServiceNew.buildExportFullWorkDaysByEmployee(
                Arrays.asList(missingOneDay, overAccrued));

        Assert.assertEquals(new BigDecimal("24.00"), result.get(101L));
        Assert.assertEquals(new BigDecimal("28.00"), result.get(102L));
    }

    @Test
    public void buildExportAbsenceDaysByEmployee_shouldCalculateExpectedMinusAccruedFromOvertimeNightStatistics() {
        HrmOvertimeNightStatisticsDetail missingOneDay = new HrmOvertimeNightStatisticsDetail();
        missingOneDay.setEmployeeId(101L);
        missingOneDay.setExpectedAttendanceDays(25);
        missingOneDay.setAccruedAttendanceHours(new BigDecimal("192.00"));

        HrmOvertimeNightStatisticsDetail overAccrued = new HrmOvertimeNightStatisticsDetail();
        overAccrued.setEmployeeId(102L);
        overAccrued.setExpectedAttendanceDays(25);
        overAccrued.setAccruedAttendanceHours(new BigDecimal("224.00"));

        Map<Long, BigDecimal> result = SalaryMonthRecordServiceNew.buildExportAbsenceDaysByEmployee(
                Arrays.asList(missingOneDay, overAccrued));

        Assert.assertEquals(new BigDecimal("1.00"), result.get(101L));
        Assert.assertEquals(new BigDecimal("-3.00"), result.get(102L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildSalaryDataRow_shouldUsePreparedOvertimeNightValuesForFullAndAbsenceDays() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "buildSalaryDataRow",
                QuerySalaryPageListVO.class,
                String.class
        );
        method.setAccessible(true);

        QuerySalaryPageListVO vo = new QuerySalaryPageListVO();
        vo.setXh(1);
        vo.setMonth("2026年6月");
        vo.setEmployeeName("张三");
        vo.setSex("男");
        vo.setEntryTime("2026-01-01");
        vo.setDeptName("行政部");
        vo.setPost("专员");
        vo.setNeedWorkDay(new BigDecimal("22.00"));
        vo.setActualWorkDay(new BigDecimal("23.00"));
        vo.setSalary(Arrays.asList(
                new QuerySalaryPageListVO.SalaryValue(0L, 9007, "24.00", 1, "满勤天数"),
                new QuerySalaryPageListVO.SalaryValue(0L, 9008, "1.00", 1, "超缺勤天数")
        ));

        Map<String, String> dataRow = (Map<String, String>) method.invoke(service, vo, "0003");

        Assert.assertEquals("24.00", dataRow.get("满勤天数"));
        Assert.assertEquals("1.00", dataRow.get("超缺勤天数"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildSalaryDataRow_shouldZeroOvertimePayForAdministrativeEmployee() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "buildSalaryDataRow",
                QuerySalaryPageListVO.class,
                String.class
        );
        method.setAccessible(true);

        QuerySalaryPageListVO vo = new QuerySalaryPageListVO();
        vo.setAffiliationSystem(1);
        vo.setSalary(Collections.singletonList(
                new QuerySalaryPageListVO.SalaryValue(0L, 180101, "999.00", 1, "加班费")
        ));

        Map<String, String> dataRow = (Map<String, String>) method.invoke(service, vo, "0003");

        Assert.assertEquals("0", dataRow.get("加班工资"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildSalaryDataRow_shouldZeroOvertimePayAndNightSubsidyForProductionEmployeeWithoutFixedRest() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "buildSalaryDataRow",
                QuerySalaryPageListVO.class,
                String.class
        );
        method.setAccessible(true);

        QuerySalaryPageListVO vo = new QuerySalaryPageListVO();
        vo.setAffiliationSystem(2);
        vo.setRestType(1);
        vo.setSalary(Arrays.asList(
                new QuerySalaryPageListVO.SalaryValue(0L, 180101, "999.00", 1, "加班费"),
                new QuerySalaryPageListVO.SalaryValue(0L, 180102, "70.00", 1, "夜班补贴")
        ));

        Map<String, String> dataRow = (Map<String, String>) method.invoke(service, vo, "0003");

        Assert.assertEquals("0", dataRow.get("加班工资"));
        Assert.assertEquals("0", dataRow.get("夜班补贴"));
    }

    @Test
    public void resolveExportFullWorkDays_shouldRejectMissingOvertimeNightStatistics() throws Exception {
        java.lang.reflect.Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "resolveExportFullWorkDays",
                Map.class,
                Long.class
        );
        method.setAccessible(true);

        try {
            method.invoke(null, Collections.emptyMap(), 101L);
            Assert.fail("expected HrmException");
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof HrmException);
            Assert.assertTrue(((HrmException) ex.getCause()).getMsg().contains("单双休设置"));
        }
    }

    @Test
    public void buildAccruedAttendanceDaysByEmployee_shouldConvertOvertimeNightHoursToDays() {
        HrmOvertimeNightStatisticsDetail first = new HrmOvertimeNightStatisticsDetail();
        first.setEmployeeId(101L);
        first.setAccruedAttendanceHours(new BigDecimal("212.00"));
        HrmOvertimeNightStatisticsDetail second = new HrmOvertimeNightStatisticsDetail();
        second.setEmployeeId(102L);
        second.setAccruedAttendanceHours(new BigDecimal("180.00"));

        Map<Long, BigDecimal> result = SalaryMonthRecordServiceNew.buildAccruedAttendanceDaysByEmployee(
                Arrays.asList(first, second));

        Assert.assertEquals(new BigDecimal("26.50"), result.get(101L));
        Assert.assertEquals(new BigDecimal("22.50"), result.get(102L));
    }

    @Test
    public void buildExpectedAttendanceDaysByEmployee_shouldUseOvertimeNightExpectedDays() {
        HrmOvertimeNightStatisticsDetail first = new HrmOvertimeNightStatisticsDetail();
        first.setEmployeeId(101L);
        first.setExpectedAttendanceDays(23);
        HrmOvertimeNightStatisticsDetail duplicate = new HrmOvertimeNightStatisticsDetail();
        duplicate.setEmployeeId(101L);
        duplicate.setExpectedAttendanceDays(22);
        HrmOvertimeNightStatisticsDetail missingExpected = new HrmOvertimeNightStatisticsDetail();
        missingExpected.setEmployeeId(102L);

        Map<Long, BigDecimal> result = SalaryMonthRecordServiceNew.buildExpectedAttendanceDaysByEmployee(
                Arrays.asList(first, duplicate, missingExpected));

        Assert.assertEquals(new BigDecimal("23.00"), result.get(101L));
        Assert.assertFalse(result.containsKey(102L));
    }

    @Test
    public void resolveFullAttendanceExpectedDays_shouldPreferOvertimeNightExpectedDays() {
        Map<Long, BigDecimal> overtimeExpectedDays = new HashMap<>();
        overtimeExpectedDays.put(101L, new BigDecimal("23.00"));

        Assert.assertEquals(new BigDecimal("23.00"),
                SalaryMonthRecordServiceNew.resolveFullAttendanceExpectedDays(overtimeExpectedDays, 101L));
        Assert.assertNull(SalaryMonthRecordServiceNew.resolveFullAttendanceExpectedDays(
                overtimeExpectedDays, 102L));
    }

    @Test
    public void resolveSalaryExpectedAttendanceDays_shouldRequireOvertimeNightAndNotFallbackToAttendanceInfo() {
        Map<Long, BigDecimal> overtimeExpectedDays = new HashMap<>();
        overtimeExpectedDays.put(101L, new BigDecimal("23.00"));

        Assert.assertEquals(new BigDecimal("23.00"),
                SalaryMonthRecordServiceNew.resolveSalaryExpectedAttendanceDays(overtimeExpectedDays, 101L));
        Assert.assertNull(SalaryMonthRecordServiceNew.resolveSalaryExpectedAttendanceDays(
                overtimeExpectedDays, 102L));
        Assert.assertNull(SalaryMonthRecordServiceNew.resolveSalaryExpectedAttendanceDays(
                overtimeExpectedDays, 103L));
    }

    @Test
    public void requireSalaryExpectedAttendanceDays_shouldPromptSingleDoubleRestSettingWhenOvertimeNightMissing()
            throws Exception {
        Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "requireSalaryExpectedAttendanceDays",
                Map.class,
                Long.class,
                String.class,
                String.class,
                int.class,
                int.class
        );
        method.setAccessible(true);

        try {
            method.invoke(null, Collections.emptyMap(), 102L, "张三", "A001", 2026, 6);
            Assert.fail("expected HrmException");
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof HrmException);
            Assert.assertTrue(((HrmException) ex.getCause()).getMsg().contains("单双休设置"));
        }
    }

    @Test
    public void resolveSalaryNormalDaysForCompute_shouldNotFallbackToExistingNeedWorkDayWhenStatisticsMissing()
            throws Exception {
        SalaryComputeContext ctx = SalaryComputeContext.builder()
                .attendanceDataMap(Collections.emptyMap())
                .expectedAttendanceDaysByEmployee(Collections.emptyMap())
                .build();
        Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "resolveSalaryNormalDaysForCompute",
                SalaryComputeContext.class,
                Long.class);
        method.setAccessible(true);

        Object result = method.invoke(null, ctx, 101L);

        Assert.assertNull("应出勤缺少公式/统计结果时，不得回退历史 needWorkDay", result);
    }

    @Test
    public void resolveSalaryNormalDaysForCompute_shouldUseOvertimeNightExpectedDaysInsteadOfAttendanceMapValue()
            throws Exception {
        Map<Integer, String> attendanceMap = new HashMap<>();
        attendanceMap.put(1, "21.75");
        Map<String, Map<Integer, String>> attendanceDataMap = new HashMap<>();
        attendanceDataMap.put("A001", attendanceMap);
        Map<Long, BigDecimal> expectedAttendanceDaysByEmployee = new HashMap<>();
        expectedAttendanceDaysByEmployee.put(101L, new BigDecimal("23.00"));
        SalaryComputeContext ctx = SalaryComputeContext.builder()
                .attendanceDataMap(attendanceDataMap)
                .expectedAttendanceDaysByEmployee(expectedAttendanceDaysByEmployee)
                .build();
        Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "resolveSalaryNormalDaysForCompute",
                SalaryComputeContext.class,
                Long.class);
        method.setAccessible(true);

        Object result = method.invoke(null, ctx, 101L);

        Assert.assertEquals(new BigDecimal("23.00"), result);
    }

    @Test
    public void shouldFallbackFullAttendanceByAccruedDays_shouldUseAccruedDaysWhenAccruedReachesExpected() {
        Assert.assertTrue(SalaryMonthRecordServiceNew.shouldFallbackFullAttendanceByAccruedDays(
                new BigDecimal("23.00"), new BigDecimal("23.00"), null, BigDecimal.ZERO));
        Assert.assertFalse(SalaryMonthRecordServiceNew.shouldFallbackFullAttendanceByAccruedDays(
                new BigDecimal("22.99"), new BigDecimal("23.00"), null, BigDecimal.ZERO));
        Assert.assertTrue(SalaryMonthRecordServiceNew.shouldFallbackFullAttendanceByAccruedDays(
                new BigDecimal("23.00"), new BigDecimal("23.00"), new HrmAttendanceSummaryVo(), BigDecimal.ZERO));
    }

    @Test
    public void shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected() {
        HrmAttendanceSummaryVo summary = new HrmAttendanceSummaryVo();
        summary.setMisscardCount(16);
        summary.setLateMinute(1);

        Assert.assertTrue(SalaryMonthRecordServiceNew.shouldFallbackFullAttendanceByAccruedDays(
                new BigDecimal("23.00"), new BigDecimal("23.00"), summary, BigDecimal.ZERO));
    }

    @Test
    public void shouldFallbackFullAttendanceByAccruedDays_shouldRejectEffectiveSickLeaveEvenWhenAccruedReachesExpected() {
        Assert.assertFalse(SalaryMonthRecordServiceNew.shouldFallbackFullAttendanceByAccruedDays(
                new BigDecimal("25.00"), new BigDecimal("25.00"), new HrmAttendanceSummaryVo(), new BigDecimal("2.00")));
    }

    @Test
    public void sickDeductDays_shouldConvertAdministrativeSickLeaveHoursToDays() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        HrmAttendanceSummaryDayVo firstSickDay = new HrmAttendanceSummaryDayVo();
        firstSickDay.setWorkDate("2026-06-01");
        firstSickDay.setBingjia(8D);
        firstSickDay.setShijia(0D);
        HrmAttendanceSummaryDayVo secondSickDay = new HrmAttendanceSummaryDayVo();
        secondSickDay.setWorkDate("2026-06-02");
        secondSickDay.setBingjia(8D);
        secondSickDay.setShijia(0D);

        Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "sickDeductDays",
                boolean.class, List.class, HashMap.class, String.class, List.class);
        method.setAccessible(true);

        BigDecimal sickDays = (BigDecimal) method.invoke(service,
                false,
                Arrays.asList(firstSickDay, secondSickDay),
                new HashMap<String, Double>(),
                "2",
                Collections.<QueryHolidayDeductionVO>emptyList());

        Assert.assertEquals(0, sickDays.compareTo(new BigDecimal("2.00")));
    }

    @Test
    public void collectComputePrerequisiteErrors_shouldAggregateMissingJobNumberAndAttendanceData() {
        Map<String, Object> missingJobNumberEmployee = new HashMap<>();
        missingJobNumberEmployee.put("employeeId", 101L);
        missingJobNumberEmployee.put("employeeName", "张三");
        missingJobNumberEmployee.put("jobNumber", "");

        Map<String, Object> missingAttendanceEmployee = new HashMap<>();
        missingAttendanceEmployee.put("employeeId", 102L);
        missingAttendanceEmployee.put("employeeName", "李四");
        missingAttendanceEmployee.put("jobNumber", "B002");

        Map<String, Object> invalidAttendanceEmployee = new HashMap<>();
        invalidAttendanceEmployee.put("employeeId", 103L);
        invalidAttendanceEmployee.put("employeeName", "王五");
        invalidAttendanceEmployee.put("jobNumber", "C003");

        Map<String, Map<Integer, String>> attendanceDataMap = new HashMap<>();
        Map<Integer, String> invalidAttendance = new HashMap<>();
        invalidAttendance.put(1, "22.00");
        invalidAttendance.put(2, "");
        attendanceDataMap.put("C003", invalidAttendance);

        List<String> errors = SalaryMonthRecordServiceNew.collectComputePrerequisiteErrors(
                Arrays.asList(missingJobNumberEmployee, missingAttendanceEmployee, invalidAttendanceEmployee),
                attendanceDataMap,
                false,
                new HrmAttendanceRule(),
                new HrmSalaryBasic().setSalaryBasic(new BigDecimal("1680"))
                        .setOvertimePay(new BigDecimal("12"))
                        .setSubsidy(new BigDecimal("30")));

        Assert.assertEquals(3, errors.size());
        Assert.assertTrue(errors.get(0).contains("张三"));
        Assert.assertTrue(errors.get(0).contains("工号"));
        Assert.assertTrue(errors.get(1).contains("李四"));
        Assert.assertTrue(errors.get(1).contains("缺少考勤数据"));
        Assert.assertTrue(errors.get(2).contains("王五"));
        Assert.assertTrue(errors.get(2).contains("应计出勤天数"));
    }

    @Test
    public void collectComputePrerequisiteErrors_shouldReportMissingAttendanceRuleAndSalaryBasicSettingsTogether() {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 101L);
        employee.put("employeeName", "张三");
        employee.put("jobNumber", "A001");

        List<String> errors = SalaryMonthRecordServiceNew.collectComputePrerequisiteErrors(
                Collections.singletonList(employee),
                null,
                true,
                null,
                new HrmSalaryBasic().setSalaryBasic(null).setOvertimePay(null).setSubsidy(null));

        Assert.assertEquals(4, errors.size());
        Assert.assertTrue(errors.stream().anyMatch(item -> item.contains("考勤扣款规则")));
        Assert.assertTrue(errors.stream().anyMatch(item -> item.contains("最低基本工资")));
        Assert.assertTrue(errors.stream().anyMatch(item -> item.contains("每小时加班费")));
        Assert.assertTrue(errors.stream().anyMatch(item -> item.contains("夜班补贴")));
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
                .setPositiveAttendance(new BigDecimal("12.00"))
                .setWorkOverTime(new BigDecimal("5"))
                .setNightSubsidy(new BigDecimal("100"))
                .setOtherSubsidies(new BigDecimal("50"))
                .setHighTemperature(new BigDecimal("30"))
                .setLowTemperature(new BigDecimal("20"));

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSalaryAmounts(
                probationSalaryMap,
                officialSalaryMap,
                attendance,
                new BigDecimal("22.00")
        );

        BigDecimal probationTotal = new BigDecimal("2000").add(new BigDecimal("1000")).add(new BigDecimal("500"));
        BigDecimal officialTotal = new BigDecimal("3000").add(new BigDecimal("1500")).add(new BigDecimal("700"));

        BigDecimal expectedProbationShouldPay = probationTotal
                .multiply(new BigDecimal("10"))
                .divide(new BigDecimal("22.00"), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal expectedOfficialShouldPay = officialTotal
                .multiply(new BigDecimal("12.00"))
                .divide(new BigDecimal("22.00"), 2, BigDecimal.ROUND_HALF_UP);
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
                BigDecimal.ZERO,
                true,
                true,
                false,
                lastTaxMap,
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(220101)).compareTo(new BigDecimal("500")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5500.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(1001)).compareTo(new BigDecimal("500.00")));
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
                BigDecimal.ZERO,
                false,
                true,
                false,
                lastTaxMap,
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(220101)).compareTo(new BigDecimal("800")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5600.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(1001)).compareTo(new BigDecimal("600.00")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_withRemarkDeductionBelowThreshold_shouldHaveZeroTax() {
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
                BigDecimal.ZERO,
                false,
                true,
                true,
                lastTaxMap,
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5500.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(1001)).compareTo(new BigDecimal("500.00")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_withoutRemark_shouldUseMonthTimes5000Deduction() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "0");
        lastTaxMap.put(250102, "0");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "0");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal("50000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                true,
                false,
                lastTaxMap,
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(270102)).compareTo(new BigDecimal("30000")));
        Assert.assertEquals(0, new BigDecimal(result.get(270105)).compareTo(new BigDecimal("20000")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("600.00")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_withRemark_shouldUse60000DeductionAndStillCalculateTaxWhenExceeded() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "0");
        lastTaxMap.put(250102, "0");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "0");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
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
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(270102)).compareTo(new BigDecimal("60000")));
        Assert.assertEquals(0, new BigDecimal(result.get(270105)).compareTo(new BigDecimal("40000")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("1480.00")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_lowSalaryAfterPriorTax_shouldNotReturnNegativeTax() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "30000");
        lastTaxMap.put(250102, "25000");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "300");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal("3000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                true,
                false,
                lastTaxMap,
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(270102)).compareTo(new BigDecimal("30000")));
        Assert.assertEquals(0, new BigDecimal(result.get(270106)).compareTo(new BigDecimal("90.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
    }

    @Test
    public void calculateMidMonthPromotionSummary_shouldIncludeWelfareTaxableIncomeInCumulativeTax() {
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
                BigDecimal.ZERO,
                false,
                true,
                false,
                lastTaxMap,
                6,
                new BigDecimal("1000")
        );

        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5500.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(270101)).compareTo(new BigDecimal("7000")));
        Assert.assertEquals(0, new BigDecimal(result.get(270105)).compareTo(new BigDecimal("0")));
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
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 6, "2", false, null, BigDecimal.ZERO
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals("6000", resultMap.get(210101).getValue());

        // 6月累计减除费用为 30000，本场景累计应纳税所得额为0
        BigDecimal expectedTax = new BigDecimal("0.00");
        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(expectedTax));

        // 实发 = 6000 - 800(社保公积金) - 200(其他扣款) - 100(借款) = 4900.00
        BigDecimal expectedRealPay = new BigDecimal("4900.00");
        Assert.assertEquals(0, new BigDecimal(resultMap.get(240101).getValue()).compareTo(expectedRealPay));
    }

    @Test
    public void calculateMidMonthPromotionSummary_shouldTaxTaxOnlyBonusWithoutAddingPayAmounts() {
        Map<Integer, String> lastMonthTaxData = new HashMap<>();
        lastMonthTaxData.put(250101, "0");
        lastMonthTaxData.put(250102, "0");
        lastMonthTaxData.put(250103, "0");
        lastMonthTaxData.put(250105, "0");

        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
                new BigDecimal("6000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("3000"),
                false,
                true,
                false,
                lastMonthTaxData,
                6,
                BigDecimal.ZERO
        );

        Assert.assertEquals(0, new BigDecimal(result.get(210101)).compareTo(new BigDecimal("6000")));
        Assert.assertEquals(0, new BigDecimal(result.get(270101)).compareTo(new BigDecimal("9000")));
        Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("0.00")));
        Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("6000.00")));
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
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 6, "1", false, null, BigDecimal.ZERO
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, new BigDecimal(resultMap.get(240101).getValue()).compareTo(new BigDecimal("5200")));
    }

    @Test
    public void processMidMonthPromotion_remarkEmployee_shouldContinueImportedPriorDeduction() {
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
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 3, "2", true, null, BigDecimal.ZERO
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(new BigDecimal("51.00")));
        Assert.assertEquals(0, new BigDecimal(resultMap.get(270102).getValue()).compareTo(new BigDecimal("10000")));
    }

    @Test
    public void processMidMonthPromotion_remarkEmployeeWithoutPriorDeduction_shouldUseAnnual60000Deduction() {
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

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            allOptions, midMonthSalaryMap, null, 2026, 3, "2", true, null, BigDecimal.ZERO
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, new BigDecimal(resultMap.get(270102).getValue()).compareTo(new BigDecimal("60000")));
    }

    @Test
    public void recalculateMidMonthPromotion_emptyMidMonthMap_shouldNotModifyOptions() {
        List<HrmSalaryMonthOptionValue> options = new ArrayList<>();
        options.add(buildOptionValue(210101, "8000"));
        options.add(buildOptionValue(230101, "100"));
        options.add(buildOptionValue(240101, "7000"));

        SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
            options, Collections.emptyMap(), null, 2026, 6, "2", false, null, BigDecimal.ZERO
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = options.stream()
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity()));
        Assert.assertEquals("8000", resultMap.get(210101).getValue());
        Assert.assertEquals("100", resultMap.get(230101).getValue());
        Assert.assertEquals("7000", resultMap.get(240101).getValue());
    }

    @Test
    public void recalculateMidMonthPromotion_december_shouldKeepCurrentYearCumulative() {
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
            allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 12, "2", false, null, BigDecimal.ZERO
        );

        Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

        Assert.assertEquals(0, new BigDecimal(resultMap.get(270101).getValue()).compareTo(new BigDecimal("106000")));
        Assert.assertEquals(0, new BigDecimal(resultMap.get(270102).getValue()).compareTo(new BigDecimal("60000")));
    }

    @Test
    public void calculateMidMonthPromotionSalaryAmounts_nullAttendance_shouldReturnEmptyMap() {
        Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSalaryAmounts(
            buildSalaryMap("3000", "1500", "700"),
            buildSalaryMap("4000", "2000", "900"),
            null,
            new BigDecimal("22.00")
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
