package com.tianye.hrsystem.imple.employee;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDepartmentDetailExportSupportTest {

    @Test
    public void headers_shouldRemoveScoreColumnsAndKeepFinalDepartmentDetailOrder() {
        Assert.assertEquals(Arrays.asList(
                "序号", "姓名", "岗位", "入职年限", "性别", "年龄", "学历", "专业", "薪资级别", "薪资待遇", "合同到期日"
        ), EmployeeDepartmentDetailExportSupport.headers());
        Assert.assertFalse(EmployeeDepartmentDetailExportSupport.headers().contains("年平均考核分"));
        Assert.assertFalse(EmployeeDepartmentDetailExportSupport.headers().contains("理论考核分"));
    }

    @Test
    public void salaryTreatment_shouldSumBasicPostAndDutySalaryOptionsOnly() {
        Map<Integer, String> salaryOptions = new HashMap<>();
        salaryOptions.put(10101, "2130");
        salaryOptions.put(10102, "1570.50");
        salaryOptions.put(10103, "300");
        salaryOptions.put(40102, "1000");

        Assert.assertEquals(new BigDecimal("4000.50"), EmployeeDepartmentDetailExportSupport.salaryTreatment(salaryOptions));
    }

    @Test
    public void performanceSalaryCost_shouldUsePerformanceSalaryOptionOnly() {
        Map<Integer, String> salaryOptions = new HashMap<>();
        salaryOptions.put(10101, "2130");
        salaryOptions.put(10102, "1570.50");
        salaryOptions.put(10103, "300");
        salaryOptions.put(41001, "600");

        Assert.assertEquals(new BigDecimal("600"), EmployeeDepartmentDetailExportSupport.performanceSalaryCost(salaryOptions));
    }

    @Test
    public void buildFileName_shouldUseTopOrganizationNameAndExportDate() {
        Assert.assertEquals(
                "海南农谷人员明细表20260819.xlsx",
                EmployeeDepartmentDetailExportSupport.buildFileName("海南农谷", LocalDate.of(2026, 8, 19))
        );
        Assert.assertEquals(
                "人员明细表20260819.xlsx",
                EmployeeDepartmentDetailExportSupport.buildFileName(" ", LocalDate.of(2026, 8, 19))
        );
    }

    @Test
    public void buildWorkbook_shouldGroupEmployeesByDepartmentAndWriteSalaryTreatment() throws Exception {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1001L);
        employee.put("employeeName", "张三");
        employee.put("deptName", "生产部");
        employee.put("post", "操作工");
        employee.put("entryTime", "2024-01-02");
        employee.put("sex", 1);
        employee.put("age", 36);
        employee.put("highestEducation", 8);
        employee.put("major", "食品科学");
        employee.put("postLevel", "P3");
        employee.put("lastContractEndTime", "2027-01-01");

        Map<Integer, String> salaryOptions = new HashMap<>();
        salaryOptions.put(10101, "2130");
        salaryOptions.put(10102, "1570");
        salaryOptions.put(10103, "300");

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Collections.singletonList(employee),
                Collections.singletonMap(1001L, Collections.singletonMap("薪资级别", "P4")),
                Collections.singletonMap(1001L, salaryOptions)
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Assert.assertEquals("人员总表", workbook.getSheetAt(0).getSheetName());
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertNotNull(departmentSheet);
            Row header = departmentSheet.getRow(8);
            Assert.assertEquals("序号", header.getCell(0).getStringCellValue());
            Assert.assertEquals("合同到期日", header.getCell(10).getStringCellValue());
            Assert.assertNull("不应保留考核分空列", header.getCell(11));

            Row data = departmentSheet.getRow(9);
            List<String> expected = Arrays.asList(
                    "1", "张三", "操作工", "", "男", "36", "本科", "食品科学", "P4", "4000", "2027-01-01"
            );
            for (int i = 0; i < expected.size(); i++) {
                Assert.assertEquals(expected.get(i), data.getCell(i).getStringCellValue());
            }
        }
    }

    @Test
    public void buildWorkbook_shouldDisplayOpenEndedContractDeadlineText() throws Exception {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1001L);
        employee.put("employeeName", "张三");
        employee.put("deptName", "生产部");
        employee.put("lastContractType", 2);
        employee.put("lastContractEndTime", "");

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Collections.singletonList(employee),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertNotNull(departmentSheet);
            Assert.assertEquals("无固定期限", stringValue(departmentSheet, 9, 10));
        }
    }

    @Test
    public void buildWorkbook_shouldMatchReferenceDepartmentTopSummaryStructure() throws Exception {
        Map<String, Object> firstEmployee = new HashMap<>();
        firstEmployee.put("employeeId", 1001L);
        firstEmployee.put("employeeName", "张三");
        firstEmployee.put("deptName", "生产部");
        firstEmployee.put("sex", 1);
        firstEmployee.put("highestEducation", 8);
        firstEmployee.put("major", "食品科学");
        firstEmployee.put("companyAge", "10年2月");
        firstEmployee.put("status", 1);

        Map<String, Object> secondEmployee = new HashMap<>();
        secondEmployee.put("employeeId", 1002L);
        secondEmployee.put("employeeName", "李四");
        secondEmployee.put("deptName", "生产部");
        secondEmployee.put("sex", 2);
        secondEmployee.put("highestEducation", 7);
        secondEmployee.put("major", "");
        secondEmployee.put("companyAge", "1年6月");
        secondEmployee.put("status", 2);

        Map<String, Object> thirdEmployee = new HashMap<>();
        thirdEmployee.put("employeeId", 1003L);
        thirdEmployee.put("employeeName", "王五");
        thirdEmployee.put("deptName", "生产部");
        thirdEmployee.put("sex", 2);
        thirdEmployee.put("highestEducation", 3);
        thirdEmployee.put("major", "/");
        thirdEmployee.put("companyAge", "7月");
        thirdEmployee.put("status", 3);

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(firstEmployee, secondEmployee, thirdEmployee),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertNotNull(departmentSheet);
            Assert.assertEquals("部门", stringValue(departmentSheet, 0, 0));
            Assert.assertEquals("部门总人数", stringValue(departmentSheet, 0, 1));
            Assert.assertEquals("男女比例", stringValue(departmentSheet, 0, 2));
            Assert.assertEquals("学历层次", stringValue(departmentSheet, 0, 3));
            Assert.assertEquals("工年结构", stringValue(departmentSheet, 0, 4));
            Assert.assertEquals("专业汇总", stringValue(departmentSheet, 0, 5));

            Assert.assertTrue(hasMergedRegion(departmentSheet, 0, 5, 0, 7));
            Assert.assertTrue(hasMergedRegion(departmentSheet, 1, 0, 5, 0));
            Assert.assertTrue(hasMergedRegion(departmentSheet, 1, 1, 5, 1));
            Assert.assertTrue(hasMergedRegion(departmentSheet, 1, 2, 5, 2));
            Assert.assertTrue(hasMergedRegion(departmentSheet, 1, 5, 2, 7));
            Assert.assertTrue(hasMergedRegion(departmentSheet, 3, 5, 5, 7));
            Assert.assertTrue(hasMergedRegion(departmentSheet, 6, 0, 7, 7));

            Assert.assertEquals("生产部", stringValue(departmentSheet, 1, 0));
            Assert.assertEquals("3", stringValue(departmentSheet, 1, 1));
            Assert.assertEquals("1：2\n（男1女2）", stringValue(departmentSheet, 1, 2));
            Assert.assertEquals("本科：1人", stringValue(departmentSheet, 1, 3));
            Assert.assertEquals("大专：1人", stringValue(departmentSheet, 2, 3));
            Assert.assertEquals("中专：1人", stringValue(departmentSheet, 3, 3));
            Assert.assertEquals("10年以上：1人", stringValue(departmentSheet, 1, 4));
            Assert.assertEquals("1-2年：1人", stringValue(departmentSheet, 4, 4));
            Assert.assertEquals("1年以下：1人", stringValue(departmentSheet, 5, 4));
            Assert.assertEquals("", stringValue(departmentSheet, 1, 5));
            Assert.assertEquals("实习期要求：一年以上实习生：0人\n正式老员工：1人\n试用期人员：1人", stringValue(departmentSheet, 3, 5));
            Assert.assertEquals("月固定薪资成本", stringValue(departmentSheet, 1, 8));
            Assert.assertEquals("0", stringValue(departmentSheet, 1, 9));
            Assert.assertEquals("月绩效薪资成本", stringValue(departmentSheet, 2, 8));
            Assert.assertEquals("月社保成本", stringValue(departmentSheet, 3, 8));
            Assert.assertEquals("月公积金成本", stringValue(departmentSheet, 4, 8));
            Assert.assertEquals("", stringValue(departmentSheet, 5, 8));
            Assert.assertEquals("生产部人员明细表", stringValue(departmentSheet, 6, 0));
            Assert.assertEquals("序号", stringValue(departmentSheet, 8, 0));
        }
    }

    @Test
    public void buildWorkbook_shouldRemoveProfessionalRelationTextAndUseEmployeeStatusCounters() throws Exception {
        Map<String, Object> formalEmployee = new HashMap<>();
        formalEmployee.put("employeeId", 1001L);
        formalEmployee.put("employeeName", "正式员工");
        formalEmployee.put("deptName", "生产部");
        formalEmployee.put("status", 1);
        formalEmployee.put("major", "会计");
        formalEmployee.put("companyAge", "3月");

        Map<String, Object> probationEmployee = new HashMap<>();
        probationEmployee.put("employeeId", 1002L);
        probationEmployee.put("employeeName", "试用员工");
        probationEmployee.put("deptName", "生产部");
        probationEmployee.put("status", 2);
        probationEmployee.put("major", "");
        probationEmployee.put("companyAge", "2年");

        Map<String, Object> longIntern = new HashMap<>();
        longIntern.put("employeeId", 1003L);
        longIntern.put("employeeName", "一年实习");
        longIntern.put("deptName", "生产部");
        longIntern.put("status", 3);
        longIntern.put("probation", 12);
        longIntern.put("major", "/");
        longIntern.put("companyAge", "1月");

        Map<String, Object> shortIntern = new HashMap<>();
        shortIntern.put("employeeId", 1004L);
        shortIntern.put("employeeName", "短期实习");
        shortIntern.put("deptName", "生产部");
        shortIntern.put("status", 3);
        shortIntern.put("probation", 6);
        shortIntern.put("major", "无");
        shortIntern.put("companyAge", "5年");

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(formalEmployee, probationEmployee, longIntern, shortIntern),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertEquals("", stringValue(departmentSheet, 1, 5));
            Assert.assertFalse("专业汇总不应再输出专业相关描述",
                    stringValue(departmentSheet, 1, 5).contains("专业相关"));
            Assert.assertFalse("专业汇总不应再输出专业不相关描述",
                    stringValue(departmentSheet, 1, 5).contains("专业不相关"));
            Assert.assertEquals("实习期要求：一年以上实习生：1人\n正式老员工：1人\n试用期人员：1人",
                    stringValue(departmentSheet, 3, 5));
        }
    }

    @Test
    public void summarySheet_shouldMatchReferenceTotalSheetWithoutMealCost() throws Exception {
        Map<String, Object> firstEmployee = new HashMap<>();
        firstEmployee.put("employeeId", 1001L);
        firstEmployee.put("employeeName", "张三");
        firstEmployee.put("deptName", "生产部");
        firstEmployee.put("corporateInsuranceAmount", "466.50");
        firstEmployee.put("corporateProvidentFundAmount", "144");

        Map<String, Object> secondEmployee = new HashMap<>();
        secondEmployee.put("employeeId", 1002L);
        secondEmployee.put("employeeName", "李四");
        secondEmployee.put("deptName", "财务部");
        secondEmployee.put("corporateInsuranceAmount", "700.25");
        secondEmployee.put("corporateProvidentFundAmount", "200");

        Map<Integer, String> firstSalaryOptions = new HashMap<>();
        firstSalaryOptions.put(10101, "2130");
        firstSalaryOptions.put(10102, "1570");
        firstSalaryOptions.put(10103, "300");
        firstSalaryOptions.put(41001, "600");

        Map<Integer, String> secondSalaryOptions = new HashMap<>();
        secondSalaryOptions.put(10101, "3000");
        secondSalaryOptions.put(10102, "1200.50");
        secondSalaryOptions.put(10103, "200");
        secondSalaryOptions.put(41001, "800");

        Map<Long, Map<Integer, String>> salaryOptionsByEmployee = new HashMap<>();
        salaryOptionsByEmployee.put(1001L, firstSalaryOptions);
        salaryOptionsByEmployee.put(1002L, secondSalaryOptions);
        Map<String, String> firstDynamicFields = new HashMap<>();
        firstDynamicFields.put("固定绩效", "100");
        firstDynamicFields.put("职务补助", "50");
        firstDynamicFields.put("其他补助", "80");

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(firstEmployee, secondEmployee),
                Collections.singletonMap(1001L, firstDynamicFields),
                salaryOptionsByEmployee
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet summarySheet = workbook.getSheet("人员总表");
            Assert.assertEquals("   海南农谷   公司人员明细总表", stringValue(summarySheet, 0, 0));
            Assert.assertEquals("企业总人数", stringValue(summarySheet, 1, 0));
            Assert.assertEquals("2", stringValue(summarySheet, 1, 1));
            Assert.assertEquals("月总固定薪资成本", stringValue(summarySheet, 2, 0));
            Assert.assertEquals("8630.5", stringValue(summarySheet, 2, 1));
            Assert.assertEquals("月总绩效薪资成本", stringValue(summarySheet, 3, 0));
            Assert.assertEquals("1400", stringValue(summarySheet, 3, 1));
            Assert.assertEquals("月总社保成本", stringValue(summarySheet, 4, 0));
            Assert.assertEquals("1166.75", stringValue(summarySheet, 4, 1));
            Assert.assertEquals("月总公积金成本", stringValue(summarySheet, 5, 0));
            Assert.assertEquals("344", stringValue(summarySheet, 5, 1));
            Assert.assertEquals("分部门汇总", stringValue(summarySheet, 6, 0));
            Assert.assertEquals("生产部", stringValue(summarySheet, 6, 1));
            Assert.assertEquals("财务部", stringValue(summarySheet, 6, 2));
            Assert.assertEquals("人员总数", stringValue(summarySheet, 7, 0));
            Assert.assertEquals("1", stringValue(summarySheet, 7, 1));
            Assert.assertEquals("1", stringValue(summarySheet, 7, 2));
            Assert.assertEquals("月固定薪资成本", stringValue(summarySheet, 8, 0));
            Assert.assertEquals("4230", stringValue(summarySheet, 8, 1));
            Assert.assertEquals("4400.5", stringValue(summarySheet, 8, 2));
            Assert.assertEquals("合计月人工成本", stringValue(summarySheet, 12, 0));
            Assert.assertEquals("5440.5", stringValue(summarySheet, 12, 1));
            Assert.assertEquals("6100.75", stringValue(summarySheet, 12, 2));
            for (int rowIndex = 0; rowIndex <= summarySheet.getLastRowNum(); rowIndex++) {
                Assert.assertFalse("人员总表不应保留伙食成本", stringValue(summarySheet, rowIndex, 0).contains("伙食"));
            }
        }
    }

    @Test
    public void buildWorkbook_shouldWriteDepartmentCostValuesFromSalaryAndInsuranceSources() throws Exception {
        Map<String, Object> firstEmployee = new HashMap<>();
        firstEmployee.put("employeeId", 1001L);
        firstEmployee.put("employeeName", "张三");
        firstEmployee.put("deptName", "生产部");
        firstEmployee.put("corporateInsuranceAmount", "466.50");
        firstEmployee.put("corporateProvidentFundAmount", "144");

        Map<String, Object> secondEmployee = new HashMap<>();
        secondEmployee.put("employeeId", 1002L);
        secondEmployee.put("employeeName", "李四");
        secondEmployee.put("deptName", "生产部");
        secondEmployee.put("corporateInsuranceAmount", new BigDecimal("700.25"));
        secondEmployee.put("corporateProvidentFundAmount", new BigDecimal("200"));

        Map<Integer, String> firstSalaryOptions = new HashMap<>();
        firstSalaryOptions.put(10101, "2130");
        firstSalaryOptions.put(10102, "1570");
        firstSalaryOptions.put(10103, "300");
        firstSalaryOptions.put(41001, "600");

        Map<Integer, String> secondSalaryOptions = new HashMap<>();
        secondSalaryOptions.put(10101, "3000");
        secondSalaryOptions.put(10102, "1200.50");
        secondSalaryOptions.put(10103, "200");
        secondSalaryOptions.put(41001, "0");

        Map<Long, Map<Integer, String>> salaryOptionsByEmployee = new HashMap<>();
        salaryOptionsByEmployee.put(1001L, firstSalaryOptions);
        salaryOptionsByEmployee.put(1002L, secondSalaryOptions);

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(firstEmployee, secondEmployee),
                Collections.emptyMap(),
                salaryOptionsByEmployee
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertNotNull(departmentSheet);
            Assert.assertEquals("月固定薪资成本", stringValue(departmentSheet, 1, 8));
            Assert.assertEquals("8400.5", stringValue(departmentSheet, 1, 9));
            Assert.assertEquals("月绩效薪资成本", stringValue(departmentSheet, 2, 8));
            Assert.assertEquals("600", stringValue(departmentSheet, 2, 9));
            Assert.assertEquals("月社保成本", stringValue(departmentSheet, 3, 8));
            Assert.assertEquals("1166.75", stringValue(departmentSheet, 3, 9));
            Assert.assertEquals("月公积金成本", stringValue(departmentSheet, 4, 8));
            Assert.assertEquals("344", stringValue(departmentSheet, 4, 9));
            Assert.assertEquals("月伙食成本不再保留", "", stringValue(departmentSheet, 5, 8));
        }
    }

    @Test
    public void buildWorkbook_shouldIncludeFixedPerformanceInSalaryTreatmentAndFixedSalaryCost() throws Exception {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1001L);
        employee.put("employeeName", "王洪平");
        employee.put("deptName", "生产部");

        Map<Integer, String> salaryOptions = new HashMap<>();
        salaryOptions.put(10101, "4150");

        Map<Long, Map<String, String>> dynamicFields = Collections.singletonMap(
                1001L,
                Collections.singletonMap("固定绩效", "4150")
        );

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Collections.singletonList(employee),
                dynamicFields,
                Collections.singletonMap(1001L, salaryOptions)
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertEquals("4150(固定)+4150(绩效)", stringValue(departmentSheet, 9, 9));
            Assert.assertEquals("8300", stringValue(departmentSheet, 1, 9));
        }
    }

    @Test
    public void buildWorkbook_shouldShowFixedPerformanceSectionWhenFixedPerformanceIsExplicitZero() throws Exception {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1001L);
        employee.put("employeeName", "王洪平");
        employee.put("deptName", "生产部");

        Map<Integer, String> salaryOptions = new HashMap<>();
        salaryOptions.put(10101, "4150");

        Map<Long, Map<String, String>> dynamicFields = Collections.singletonMap(
                1001L,
                Collections.singletonMap("固定绩效", "0.00")
        );

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Collections.singletonList(employee),
                dynamicFields,
                Collections.singletonMap(1001L, salaryOptions)
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertEquals("4150(固定)+0(绩效)", stringValue(departmentSheet, 9, 9));
            Assert.assertEquals("4150", stringValue(departmentSheet, 1, 9));
        }
    }

    @Test
    public void buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees() throws Exception {
        Map<String, Object> ordinaryEmployee = new HashMap<>();
        ordinaryEmployee.put("employeeId", 1001L);
        ordinaryEmployee.put("employeeName", "普通员工");
        ordinaryEmployee.put("deptName", "生产部");
        ordinaryEmployee.put("post", "操作工");
        ordinaryEmployee.put("status", 1);
        ordinaryEmployee.put("fullAttendance", 1);

        Map<String, Object> leaderEmployee = new HashMap<>();
        leaderEmployee.put("employeeId", 1002L);
        leaderEmployee.put("employeeName", "领导员工");
        leaderEmployee.put("deptName", "生产部");
        leaderEmployee.put("post", "生产经理");
        leaderEmployee.put("status", 1);
        leaderEmployee.put("fullAttendance", 1);

        Map<String, Object> probationEmployee = new HashMap<>();
        probationEmployee.put("employeeId", 1003L);
        probationEmployee.put("employeeName", "试用员工");
        probationEmployee.put("deptName", "生产部");
        probationEmployee.put("post", "生产经理");
        probationEmployee.put("status", 2);
        probationEmployee.put("fullAttendance", 1);

        Map<Integer, String> fixedSalary = Collections.singletonMap(10101, "4000");
        Map<Long, Map<Integer, String>> salaryOptionsByEmployee = new HashMap<>();
        salaryOptionsByEmployee.put(1001L, fixedSalary);
        salaryOptionsByEmployee.put(1002L, fixedSalary);
        salaryOptionsByEmployee.put(1003L, fixedSalary);

        Map<Long, Map<String, String>> dynamicFields = Collections.singletonMap(
                1002L,
                new HashMap<String, String>() {{
                    put("固定绩效", "300");
                    put("职务补助", "200");
                    put("其他补助", "80");
                }}
        );

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(ordinaryEmployee, leaderEmployee, probationEmployee),
                dynamicFields,
                salaryOptionsByEmployee,
                new BigDecimal("100"),
                new BigDecimal("500")
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertEquals("4000", stringValue(departmentSheet, 9, 9));
            Assert.assertEquals("4000(固定)+300(绩效)+200(职务补助)+80(其他补助)+500(全勤)", stringValue(departmentSheet, 10, 9));
            Assert.assertEquals("4000", stringValue(departmentSheet, 11, 9));
        }
    }

    @Test
    public void buildWorkbook_shouldNotAppendFullAttendanceWhenAmountIsOrdinaryOneHundred() throws Exception {
        Map<String, Object> ordinaryEmployee = new HashMap<>();
        ordinaryEmployee.put("employeeId", 1101L);
        ordinaryEmployee.put("employeeName", "普通员工");
        ordinaryEmployee.put("deptName", "生产部");
        ordinaryEmployee.put("post", "操作工");
        ordinaryEmployee.put("status", 1);
        ordinaryEmployee.put("fullAttendance", 1);

        Map<String, Object> leaderEmployee = new HashMap<>();
        leaderEmployee.put("employeeId", 1102L);
        leaderEmployee.put("employeeName", "领导员工");
        leaderEmployee.put("deptName", "生产部");
        leaderEmployee.put("post", "生产经理");
        leaderEmployee.put("status", 1);
        leaderEmployee.put("fullAttendance", 1);

        Map<Integer, String> fixedSalary = Collections.singletonMap(10101, "4000");
        Map<Long, Map<Integer, String>> salaryOptionsByEmployee = new HashMap<>();
        salaryOptionsByEmployee.put(1101L, fixedSalary);
        salaryOptionsByEmployee.put(1102L, fixedSalary);

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(ordinaryEmployee, leaderEmployee),
                Collections.emptyMap(),
                salaryOptionsByEmployee,
                new BigDecimal("100"),
                new BigDecimal("500")
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertEquals("普通员工全勤金额为100时，薪资待遇不再追加全勤段", "4000", stringValue(departmentSheet, 9, 9));
            Assert.assertEquals("领导全勤金额非100时仍应追加", "4000(固定)+500(全勤)", stringValue(departmentSheet, 10, 9));
        }
    }

    @Test
    public void buildWorkbook_shouldAppendFullAttendanceOnlyWhenEmployeeFullAttendanceIsOne() throws Exception {
        Map<String, Object> eligibleEmployee = new HashMap<>();
        eligibleEmployee.put("employeeId", 1201L);
        eligibleEmployee.put("employeeName", "有全勤");
        eligibleEmployee.put("deptName", "生产部");
        eligibleEmployee.put("post", "操作工");
        eligibleEmployee.put("status", 1);
        eligibleEmployee.put("fullAttendance", 1);

        Map<String, Object> ineligibleEmployee = new HashMap<>();
        ineligibleEmployee.put("employeeId", 1202L);
        ineligibleEmployee.put("employeeName", "无全勤");
        ineligibleEmployee.put("deptName", "生产部");
        ineligibleEmployee.put("post", "操作工");
        ineligibleEmployee.put("status", 1);
        ineligibleEmployee.put("fullAttendance", 2);

        Map<Integer, String> fixedSalary = Collections.singletonMap(10101, "4000");
        Map<Long, Map<Integer, String>> salaryOptionsByEmployee = new HashMap<>();
        salaryOptionsByEmployee.put(1201L, fixedSalary);
        salaryOptionsByEmployee.put(1202L, fixedSalary);

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(eligibleEmployee, ineligibleEmployee),
                Collections.emptyMap(),
                salaryOptionsByEmployee,
                new BigDecimal("500"),
                new BigDecimal("500")
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet departmentSheet = workbook.getSheet("生产部");
            Assert.assertEquals("4000(固定)+500(全勤)", stringValue(departmentSheet, 9, 9));
            Assert.assertEquals("4000", stringValue(departmentSheet, 10, 9));
        }
    }

    @Test
    public void buildWorkbook_shouldAvoidDuplicateSheetNamesAfterExcelSanitizing() throws Exception {
        String longDepartmentPrefix = "一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一";
        Map<String, Object> firstEmployee = new HashMap<>();
        firstEmployee.put("employeeId", 1001L);
        firstEmployee.put("employeeName", "张三");
        firstEmployee.put("deptName", longDepartmentPrefix + "甲");

        Map<String, Object> secondEmployee = new HashMap<>();
        secondEmployee.put("employeeId", 1002L);
        secondEmployee.put("employeeName", "李四");
        secondEmployee.put("deptName", longDepartmentPrefix + "乙");

        byte[] bytes = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                "海南农谷",
                Arrays.asList(firstEmployee, secondEmployee),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Assert.assertEquals(3, workbook.getNumberOfSheets());
            Assert.assertNotEquals(workbook.getSheetAt(1).getSheetName(), workbook.getSheetAt(2).getSheetName());
            Assert.assertTrue(workbook.getSheetAt(1).getSheetName().length() <= 31);
            Assert.assertTrue(workbook.getSheetAt(2).getSheetName().length() <= 31);
        }
    }

    private static String stringValue(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || row.getCell(columnIndex) == null) {
            return "";
        }
        return row.getCell(columnIndex).getStringCellValue();
    }

    private static boolean hasMergedRegion(Sheet sheet, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstRow() == firstRow
                    && region.getFirstColumn() == firstColumn
                    && region.getLastRow() == lastRow
                    && region.getLastColumn() == lastColumn) {
                return true;
            }
        }
        return false;
    }
}
