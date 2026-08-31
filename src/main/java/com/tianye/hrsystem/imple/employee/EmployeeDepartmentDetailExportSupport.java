package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianye.hrsystem.enums.EmployeeContractType;
import com.tianye.hrsystem.enums.EmployeeEducationEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EmployeeDepartmentDetailExportSupport {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String OPEN_ENDED_CONTRACT_TEXT = "无固定期限";
    private static final String SUMMARY_SHEET_NAME = "人员总表";
    private static final String UNASSIGNED_DEPARTMENT = "未分配部门";
    private static final List<Integer> SALARY_TREATMENT_CODES = Arrays.asList(10101, 10102, 10103);
    private static final int PERFORMANCE_SALARY_CODE = 41001;
    private static final Pattern COMPANY_AGE_YEAR_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*年");
    private static final Pattern COMPANY_AGE_MONTH_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:个)?月");
    private static final List<String> EDUCATION_SUMMARY_LABELS = Collections.unmodifiableList(Arrays.asList(
            "本科", "大专", "中专", "高中", "初中"
    ));
    private static final List<String> HEADERS = Collections.unmodifiableList(Arrays.asList(
            "序号", "姓名", "岗位", "入职年限", "性别", "年龄", "学历", "专业", "薪资级别", "薪资待遇", "合同到期日"
    ));

    private EmployeeDepartmentDetailExportSupport() {
    }

    static List<String> headers() {
        return HEADERS;
    }

    static String buildFileName(String topOrganizationName, LocalDate exportDate) {
        LocalDate date = exportDate == null ? LocalDate.now() : exportDate;
        String companyName = StrUtil.blankToDefault(topOrganizationName, "");
        return companyName.trim() + "人员明细表" + FILE_DATE_FORMATTER.format(date) + ".xlsx";
    }

    static BigDecimal salaryTreatment(Map<Integer, String> salaryOptions) {
        BigDecimal total = BigDecimal.ZERO;
        if (salaryOptions == null || salaryOptions.isEmpty()) {
            return total;
        }
        for (Integer code : SALARY_TREATMENT_CODES) {
            total = total.add(parseMoney(salaryOptions.get(code)));
        }
        return total;
    }

    static BigDecimal performanceSalaryCost(Map<Integer, String> salaryOptions) {
        if (salaryOptions == null || salaryOptions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return parseMoney(salaryOptions.get(PERFORMANCE_SALARY_CODE));
    }

    static byte[] buildWorkbook(String topOrganizationName,
                                List<Map<String, Object>> employees,
                                Map<Long, Map<String, String>> dynamicFieldValues,
                                Map<Long, Map<Integer, String>> salaryOptionsByEmployee) throws IOException {
        return buildWorkbook(topOrganizationName, employees, dynamicFieldValues, salaryOptionsByEmployee,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    static byte[] buildWorkbook(String topOrganizationName,
                                List<Map<String, Object>> employees,
                                Map<Long, Map<String, String>> dynamicFieldValues,
                                Map<Long, Map<Integer, String>> salaryOptionsByEmployee,
                                BigDecimal ordinaryFullAttendanceAmount,
                                BigDecimal leaderFullAttendanceAmount) throws IOException {
        FullAttendanceAmounts fullAttendanceAmounts = new FullAttendanceAmounts(
                ordinaryFullAttendanceAmount, leaderFullAttendanceAmount);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ExportStyles styles = new ExportStyles(workbook);
            List<Map<String, Object>> rows = employees == null ? Collections.emptyList() : employees;
            Map<String, List<Map<String, Object>>> employeesByDepartment = groupByDepartment(rows);
            Map<String, DepartmentSummary> departmentSummaries = buildDepartmentSummaries(
                    employeesByDepartment, dynamicFieldValues, salaryOptionsByEmployee);
            writeSummarySheet(workbook, styles, topOrganizationName, departmentSummaries);
            for (Map.Entry<String, List<Map<String, Object>>> entry : employeesByDepartment.entrySet()) {
                writeDepartmentSheet(workbook, styles, entry.getKey(), entry.getValue(),
                        dynamicFieldValues, salaryOptionsByEmployee, departmentSummaries.get(entry.getKey()),
                        fullAttendanceAmounts);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void writeSummarySheet(Workbook workbook,
                                          ExportStyles styles,
                                          String topOrganizationName,
                                          Map<String, DepartmentSummary> departmentSummaries) {
        Sheet sheet = workbook.createSheet(SUMMARY_SHEET_NAME);
        setSummarySheetWidths(sheet, departmentSummaries == null ? 0 : departmentSummaries.size());
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(summaryTitle(topOrganizationName));
        titleCell.setCellStyle(styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        SummaryTotals totals = SummaryTotals.of(departmentSummaries);
        writeSummaryValueRow(sheet, styles, 1, "企业总人数", String.valueOf(totals.totalCount));
        writeSummaryValueRow(sheet, styles, 2, "月总固定薪资成本", costMoneyText(totals.fixedSalaryCost));
        writeSummaryValueRow(sheet, styles, 3, "月总绩效薪资成本", costMoneyText(totals.performanceSalaryCost));
        writeSummaryValueRow(sheet, styles, 4, "月总社保成本", costMoneyText(totals.corporateInsuranceCost));
        writeSummaryValueRow(sheet, styles, 5, "月总公积金成本", costMoneyText(totals.corporateProvidentFundCost));

        Row departmentHeaderRow = getOrCreateRow(sheet, 6);
        writeCell(departmentHeaderRow, 0, "分部门汇总", styles.headerStyle);
        int departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (String departmentName : departmentSummaries.keySet()) {
                writeCell(departmentHeaderRow, departmentColumnIndex++, departmentName, styles.headerStyle);
            }
        }

        Row countRow = getOrCreateRow(sheet, 7);
        writeCell(countRow, 0, "人员总数", styles.headerStyle);
        departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (DepartmentSummary summary : departmentSummaries.values()) {
                writeCell(countRow, departmentColumnIndex++, String.valueOf(summary.totalCount), styles.bodyStyle);
            }
        }

        Row fixedSalaryRow = getOrCreateRow(sheet, 8);
        writeCell(fixedSalaryRow, 0, "月固定薪资成本", styles.headerStyle);
        departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (DepartmentSummary summary : departmentSummaries.values()) {
                writeCell(fixedSalaryRow, departmentColumnIndex++, costMoneyText(summary.fixedSalaryCost), styles.bodyStyle);
            }
        }

        Row performanceSalaryRow = getOrCreateRow(sheet, 9);
        writeCell(performanceSalaryRow, 0, "月绩效薪资成本", styles.headerStyle);
        departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (DepartmentSummary summary : departmentSummaries.values()) {
                writeCell(performanceSalaryRow, departmentColumnIndex++, costMoneyText(summary.performanceSalaryCost), styles.bodyStyle);
            }
        }

        Row insuranceCostRow = getOrCreateRow(sheet, 10);
        writeCell(insuranceCostRow, 0, "月社保成本", styles.headerStyle);
        departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (DepartmentSummary summary : departmentSummaries.values()) {
                writeCell(insuranceCostRow, departmentColumnIndex++, costMoneyText(summary.corporateInsuranceCost), styles.bodyStyle);
            }
        }

        Row providentFundRow = getOrCreateRow(sheet, 11);
        writeCell(providentFundRow, 0, "月公积金成本", styles.headerStyle);
        departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (DepartmentSummary summary : departmentSummaries.values()) {
                writeCell(providentFundRow, departmentColumnIndex++, costMoneyText(summary.corporateProvidentFundCost), styles.bodyStyle);
            }
        }

        Row totalCostRow = getOrCreateRow(sheet, 12);
        writeCell(totalCostRow, 0, "合计月人工成本", styles.headerStyle);
        departmentColumnIndex = 1;
        if (departmentSummaries != null) {
            for (DepartmentSummary summary : departmentSummaries.values()) {
                BigDecimal totalCost = summary.fixedSalaryCost
                        .add(summary.performanceSalaryCost)
                        .add(summary.corporateInsuranceCost)
                        .add(summary.corporateProvidentFundCost);
                writeCell(totalCostRow, departmentColumnIndex++, costMoneyText(totalCost), styles.bodyStyle);
            }
        }
    }

    private static void writeDepartmentSheet(Workbook workbook,
                                             ExportStyles styles,
                                             String departmentName,
                                             List<Map<String, Object>> employees,
                                             Map<Long, Map<String, String>> dynamicFieldValues,
                                             Map<Long, Map<Integer, String>> salaryOptionsByEmployee,
                                             DepartmentSummary summary,
                                             FullAttendanceAmounts fullAttendanceAmounts) {
        Sheet sheet = workbook.createSheet(uniqueSheetName(workbook, departmentName));
        setDepartmentSheetWidths(sheet);
        writeDepartmentSummary(sheet, styles, departmentName, summary);
        Row titleRow = getOrCreateRow(sheet, 6);
        titleRow.setHeightInPoints(22);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(departmentName + "人员明细表");
        titleCell.setCellStyle(styles.titleStyle);
        getOrCreateRow(sheet, 7).setHeightInPoints(22);
        styleMergedRegion(sheet, 6, 0, 7, 7, styles.titleStyle);

        Row headerRow = getOrCreateRow(sheet, 8);
        headerRow.setHeightInPoints(22);
        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            writeCell(headerRow, columnIndex, HEADERS.get(columnIndex), styles.headerStyle);
        }

        for (int rowIndex = 0; rowIndex < employees.size(); rowIndex++) {
            Map<String, Object> employee = employees.get(rowIndex);
            Long employeeId = Convert.toLong(firstValue(employee, "employeeId", "employee_id"));
            Map<String, String> dynamicFields = dynamicFieldValues == null || employeeId == null
                    ? Collections.emptyMap()
                    : dynamicFieldValues.get(employeeId);
            Map<Integer, String> salaryOptions = salaryOptionsByEmployee == null || employeeId == null
                    ? Collections.emptyMap()
                    : salaryOptionsByEmployee.get(employeeId);
            Row row = sheet.createRow(9 + rowIndex);
            row.setHeightInPoints(22);
            List<String> values = buildDepartmentRow(rowIndex + 1, employee, dynamicFields, salaryOptions,
                    fullAttendanceAmounts);
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                writeCell(row, columnIndex, values.get(columnIndex), styles.bodyStyle);
            }
        }
    }

    private static void writeDepartmentSummary(Sheet sheet,
                                               ExportStyles styles,
                                               String departmentName,
                                               DepartmentSummary summary) {
        for (int rowIndex = 0; rowIndex <= 5; rowIndex++) {
            getOrCreateRow(sheet, rowIndex).setHeightInPoints(22);
        }

        Row headerRow = getOrCreateRow(sheet, 0);
        writeCell(headerRow, 0, "部门", styles.headerStyle);
        writeCell(headerRow, 1, "部门总人数", styles.headerStyle);
        writeCell(headerRow, 2, "男女比例", styles.headerStyle);
        writeCell(headerRow, 3, "学历层次", styles.headerStyle);
        writeCell(headerRow, 4, "工年结构", styles.headerStyle);
        writeCell(headerRow, 5, "专业汇总", styles.headerStyle);
        styleMergedRegion(sheet, 0, 5, 0, 7, styles.headerStyle);

        writeCell(getOrCreateRow(sheet, 1), 0, departmentName, styles.bodyStyle);
        writeCell(getOrCreateRow(sheet, 1), 1, String.valueOf(summary.totalCount), styles.bodyStyle);
        writeCell(getOrCreateRow(sheet, 1), 2, summary.genderRatioText(), styles.bodyStyle);
        styleMergedRegion(sheet, 1, 0, 5, 0, styles.bodyStyle);
        styleMergedRegion(sheet, 1, 1, 5, 1, styles.bodyStyle);
        styleMergedRegion(sheet, 1, 2, 5, 2, styles.bodyStyle);

        List<String> educationLines = summary.educationLines();
        List<String> companyAgeLines = summary.companyAgeLines();
        for (int index = 0; index < 5; index++) {
            Row row = getOrCreateRow(sheet, index + 1);
            writeCell(row, 3, educationLines.get(index), styles.bodyStyle);
            writeCell(row, 4, companyAgeLines.get(index), styles.bodyStyle);
        }

        writeCell(getOrCreateRow(sheet, 1), 5, summary.majorSummaryText(), styles.bodyStyle);
        styleMergedRegion(sheet, 1, 5, 2, 7, styles.bodyStyle);
        writeCell(getOrCreateRow(sheet, 3), 5, summary.probationSummaryText(), styles.bodyStyle);
        styleMergedRegion(sheet, 3, 5, 5, 7, styles.bodyStyle);

        writeCostRow(sheet, styles, 1, "月固定薪资成本", summary.fixedSalaryCost);
        writeCostRow(sheet, styles, 2, "月绩效薪资成本", summary.performanceSalaryCost);
        writeCostRow(sheet, styles, 3, "月社保成本", summary.corporateInsuranceCost);
        writeCostRow(sheet, styles, 4, "月公积金成本", summary.corporateProvidentFundCost);
    }

    private static List<String> buildDepartmentRow(int sequence,
                                                   Map<String, Object> employee,
                                                   Map<String, String> dynamicFields,
                                                   Map<Integer, String> salaryOptions,
                                                   FullAttendanceAmounts fullAttendanceAmounts) {
        List<String> row = new ArrayList<>(HEADERS.size());
        row.add(String.valueOf(sequence));
        row.add(text(firstValue(employee, "employeeName", "employee_name")));
        row.add(text(firstValue(employee, "post")));
        row.add(text(firstValue(employee, "companyAge", "company_age")));
        row.add(sexText(firstValue(employee, "sex")));
        row.add(text(firstValue(employee, "age")));
        row.add(educationText(firstValue(employee, "highestEducation", "highest_education")));
        row.add(text(firstValue(employee, "major")));
        row.add(salaryLevelText(employee, dynamicFields));
        row.add(salaryTreatmentText(employee, salaryOptions, dynamicFields, fullAttendanceAmounts));
        row.add(contractDeadlineText(employee));
        return row;
    }

    private static Map<String, List<Map<String, Object>>> groupByDepartment(List<Map<String, Object>> employees) {
        Map<String, List<Map<String, Object>>> employeesByDepartment = new LinkedHashMap<>();
        for (Map<String, Object> employee : employees) {
            String departmentName = StrUtil.blankToDefault(text(firstValue(employee, "deptName", "dept_name")), UNASSIGNED_DEPARTMENT);
            employeesByDepartment.computeIfAbsent(departmentName, key -> new ArrayList<>()).add(employee);
        }
        return employeesByDepartment;
    }

    private static String salaryLevelText(Map<String, Object> employee, Map<String, String> dynamicFields) {
        String dynamicValue = dynamicValue(dynamicFields, "薪资级别", "薪资等级", "薪资类别", "职务级别", "salaryLevel");
        return StrUtil.isNotBlank(dynamicValue) ? dynamicValue : text(firstValue(employee, "postLevel", "post_level"));
    }

    private static String salaryTreatmentText(Map<String, Object> employee,
                                              Map<Integer, String> salaryOptions,
                                              Map<String, String> dynamicFields,
                                              FullAttendanceAmounts fullAttendanceAmounts) {
        BigDecimal fixedSalary = salaryTreatment(salaryOptions);
        String fixedPerformance = dynamicValue(dynamicFields, "固定绩效", "fixedPerformance", "fixed_performance");
        String dutySubsidy = dynamicValue(dynamicFields, "职务补助", "dutySubsidy", "duty_subsidy");
        String otherSubsidy = dynamicValue(dynamicFields, "其他补助", "otherSubsidy", "other_subsidy");
        BigDecimal fullAttendanceAmount = fullAttendanceAmount(employee, fullAttendanceAmounts);
        if (StrUtil.isBlank(fixedPerformance) && StrUtil.isBlank(dutySubsidy) && StrUtil.isBlank(otherSubsidy) && BigDecimal.ZERO.compareTo(fullAttendanceAmount) == 0) {
            return moneyText(fixedSalary);
        }
        List<String> parts = new ArrayList<>();
        if (BigDecimal.ZERO.compareTo(fixedSalary) != 0) {
            parts.add(costMoneyText(fixedSalary) + "(固定)");
        }
        if (StrUtil.isNotBlank(fixedPerformance)) {
            parts.add(costMoneyText(parseMoney(fixedPerformance)) + "(绩效)");
        }
        if (StrUtil.isNotBlank(dutySubsidy)) {
            parts.add(costMoneyText(parseMoney(dutySubsidy)) + "(职务补助)");
        }
        if (StrUtil.isNotBlank(otherSubsidy)) {
            parts.add(costMoneyText(parseMoney(otherSubsidy)) + "(其他补助)");
        }
        if (BigDecimal.ZERO.compareTo(fullAttendanceAmount) != 0) {
            parts.add(costMoneyText(fullAttendanceAmount) + "(全勤)");
        }
        return String.join("+", parts);
    }

    private static BigDecimal fullAttendanceAmount(Map<String, Object> employee,
                                                   FullAttendanceAmounts fullAttendanceAmounts) {
        if (employee == null || fullAttendanceAmounts == null
                || !hasFullAttendance(employee)
                || isProbationStatus(firstValue(employee, "status"))) {
            return BigDecimal.ZERO;
        }
        String post = text(firstValue(employee, "post"));
        BigDecimal amount = isLeaderPost(post) ? fullAttendanceAmounts.leaderAmount : fullAttendanceAmounts.ordinaryAmount;
        return new BigDecimal("100").compareTo(amount) == 0 ? BigDecimal.ZERO : amount;
    }

    private static boolean hasFullAttendance(Map<String, Object> employee) {
        Integer fullAttendance = Convert.toInt(firstValue(employee, "fullAttendance", "full_attendance"), null);
        return Integer.valueOf(1).equals(fullAttendance);
    }

    private static boolean isLeaderPost(String post) {
        if (StrUtil.isBlank(post)) {
            return false;
        }
        return post.contains("经理")
                || post.contains("总监")
                || post.contains("副总")
                || post.contains("董事长")
                || post.contains("高级技师")
                || post.contains("厂长");
    }

    private static BigDecimal fixedSalaryCost(Map<Integer, String> salaryOptions,
                                              Map<String, String> dynamicFields) {
        return salaryTreatment(salaryOptions)
                .add(parseMoney(dynamicValue(dynamicFields, "固定绩效", "fixedPerformance", "fixed_performance")))
                .add(parseMoney(dynamicValue(dynamicFields, "职务补助", "dutySubsidy", "duty_subsidy")))
                .add(parseMoney(dynamicValue(dynamicFields, "其他补助", "otherSubsidy", "other_subsidy")));
    }

    private static Object firstValue(Map<String, Object> employee, String... keys) {
        if (employee == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (employee.containsKey(key)) {
                Object value = employee.get(key);
                if (value != null && StrUtil.isNotBlank(Convert.toStr(value))) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String dynamicValue(Map<String, String> dynamicFields, String... keys) {
        if (dynamicFields == null || dynamicFields.isEmpty() || keys == null) {
            return "";
        }
        Map<String, String> normalizedFields = new LinkedHashMap<>();
        dynamicFields.forEach((key, value) -> normalizedFields.putIfAbsent(normalizeKey(key), value));
        for (String key : keys) {
            String value = normalizedFields.get(normalizeKey(key));
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private static String text(Object value) {
        return value == null ? "" : Convert.toStr(value, "").trim();
    }

    private static String sexText(Object sex) {
        String value = text(sex);
        if ("1".equals(value)) {
            return "男";
        }
        if ("2".equals(value)) {
            return "女";
        }
        return value;
    }

    private static String educationText(Object highestEducation) {
        Integer education = Convert.toInt(highestEducation, null);
        if (education == null) {
            return text(highestEducation);
        }
        return EmployeeEducationEnum.parseName(education);
    }

    private static String dateText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DATE_FORMATTER);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate().format(DATE_FORMATTER);
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) value);
        }
        String valueText = text(value);
        if (valueText.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return valueText.substring(0, 10);
        }
        return valueText;
    }

    private static String contractDeadlineText(Map<String, Object> employee) {
        if (isOpenEndedContract(employee)) {
            return OPEN_ENDED_CONTRACT_TEXT;
        }
        return dateText(firstValue(employee, "lastContractEndTime", "last_contract_end_time", "latestContractEndTime", "latest_contract_end_time", "endTime", "end_time"));
    }

    private static boolean isOpenEndedContract(Map<String, Object> employee) {
        Integer contractType = Convert.toInt(firstValue(employee, "lastContractType", "last_contract_type", "latestContractType", "latest_contract_type", "contractType", "contract_type"), null);
        return Integer.valueOf(EmployeeContractType.NO_FIXED_TERM_LABOR_CONTRACT.getValue()).equals(contractType);
    }

    private static BigDecimal parseMoney(String value) {
        if (StrUtil.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parseMoney(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return parseMoney(text(value));
    }

    private static String moneyText(BigDecimal money) {
        if (money == null || BigDecimal.ZERO.compareTo(money) == 0) {
            return "";
        }
        return money.stripTrailingZeros().toPlainString();
    }

    private static String costMoneyText(BigDecimal money) {
        BigDecimal safeMoney = money == null ? BigDecimal.ZERO : money;
        return safeMoney.stripTrailingZeros().toPlainString();
    }

    private static String uniqueSheetName(Workbook workbook, String departmentName) {
        String baseName = StrUtil.blankToDefault(departmentName, UNASSIGNED_DEPARTMENT);
        String safeBaseName = WorkbookUtil.createSafeSheetName(baseName);
        if (safeBaseName.length() > 31) {
            safeBaseName = safeBaseName.substring(0, 31);
        }
        String candidate = safeBaseName;
        if (!sheetNameExists(workbook, candidate)) {
            return candidate;
        }
        for (int index = 2; ; index++) {
            String suffix = "(" + index + ")";
            int maxBaseLength = Math.max(1, 31 - suffix.length());
            String truncatedBase = safeBaseName.length() > maxBaseLength
                    ? safeBaseName.substring(0, maxBaseLength)
                    : safeBaseName;
            candidate = truncatedBase + suffix;
            if (!sheetNameExists(workbook, candidate)) {
                return candidate;
            }
        }
    }

    private static boolean sheetNameExists(Workbook workbook, String candidate) {
        if (workbook == null || StrUtil.isBlank(candidate)) {
            return false;
        }
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            String sheetName = workbook.getSheetAt(index).getSheetName();
            if (candidate.equalsIgnoreCase(sheetName)) {
                return true;
            }
        }
        return false;
    }

    private static void setDefaultWidths(Sheet sheet) {
        for (int i = 0; i < HEADERS.size(); i++) {
            sheet.setColumnWidth(i, 16 * 256);
        }
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.setColumnWidth(10, 16 * 256);
    }

    private static void setSummarySheetWidths(Sheet sheet, int departmentCount) {
        int totalColumns = Math.max(8, departmentCount + 1);
        for (int index = 0; index < totalColumns; index++) {
            sheet.setColumnWidth(index, 14 * 256);
        }
        sheet.setColumnWidth(0, 18 * 256);
    }

    private static void setDepartmentSheetWidths(Sheet sheet) {
        int[] widths = {8, 12, 13, 12, 18, 12, 16, 18, 18, 28, 18};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
    }

    private static String summaryTitle(String topOrganizationName) {
        String companyName = StrUtil.blankToDefault(topOrganizationName, "").trim();
        if (StrUtil.isBlank(companyName)) {
            return "公司人员明细总表";
        }
        return "   " + companyName + "   公司人员明细总表";
    }

    private static void writeSummaryValueRow(Sheet sheet, ExportStyles styles, int rowIndex, String label, String value) {
        Row row = getOrCreateRow(sheet, rowIndex);
        writeCell(row, 0, label, styles.headerStyle);
        writeCell(row, 1, value, styles.bodyStyle);
        styleMergedRegion(sheet, rowIndex, 1, rowIndex, 7, styles.bodyStyle);
    }

    private static void writeCostRow(Sheet sheet, ExportStyles styles, int rowIndex, String label, BigDecimal amount) {
        Row row = getOrCreateRow(sheet, rowIndex);
        writeCell(row, 8, label, styles.headerStyle);
        writeCell(row, 9, costMoneyText(amount), styles.bodyStyle);
        writeCell(row, 10, "", styles.bodyStyle);
    }

    private static void styleMergedRegion(Sheet sheet,
                                          int firstRow,
                                          int firstColumn,
                                          int lastRow,
                                          int lastColumn,
                                          CellStyle style) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = getOrCreateRow(sheet, rowIndex);
            for (int columnIndex = firstColumn; columnIndex <= lastColumn; columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                cell.setCellStyle(style);
            }
        }
    }

    private static Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private static void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static final class DepartmentSummary {
        private final int totalCount;
        private final int maleCount;
        private final int femaleCount;
        private final Map<String, Integer> educationCounts;
        private final int moreThanTenYearsCount;
        private final int fiveToTenYearsCount;
        private final int twoToFiveYearsCount;
        private final int oneToTwoYearsCount;
        private final int lessThanOneYearCount;
        private final int majorRelatedCount;
        private final int majorUnrelatedCount;
        private final int moreThanOneYearInternCount;
        private final int formalOldEmployeeCount;
        private final int probationEmployeeCount;
        private final BigDecimal fixedSalaryCost;
        private final BigDecimal performanceSalaryCost;
        private final BigDecimal corporateInsuranceCost;
        private final BigDecimal corporateProvidentFundCost;

        private DepartmentSummary(int totalCount,
                                  int maleCount,
                                  int femaleCount,
                                  Map<String, Integer> educationCounts,
                                  int moreThanTenYearsCount,
                                  int fiveToTenYearsCount,
                                  int twoToFiveYearsCount,
                                  int oneToTwoYearsCount,
                                  int lessThanOneYearCount,
                                  int majorRelatedCount,
                                  int majorUnrelatedCount,
                                  int moreThanOneYearInternCount,
                                  int formalOldEmployeeCount,
                                  int probationEmployeeCount,
                                  BigDecimal fixedSalaryCost,
                                  BigDecimal performanceSalaryCost,
                                  BigDecimal corporateInsuranceCost,
                                  BigDecimal corporateProvidentFundCost) {
            this.totalCount = totalCount;
            this.maleCount = maleCount;
            this.femaleCount = femaleCount;
            this.educationCounts = educationCounts;
            this.moreThanTenYearsCount = moreThanTenYearsCount;
            this.fiveToTenYearsCount = fiveToTenYearsCount;
            this.twoToFiveYearsCount = twoToFiveYearsCount;
            this.oneToTwoYearsCount = oneToTwoYearsCount;
            this.lessThanOneYearCount = lessThanOneYearCount;
            this.majorRelatedCount = majorRelatedCount;
            this.majorUnrelatedCount = majorUnrelatedCount;
            this.moreThanOneYearInternCount = moreThanOneYearInternCount;
            this.formalOldEmployeeCount = formalOldEmployeeCount;
            this.probationEmployeeCount = probationEmployeeCount;
            this.fixedSalaryCost = fixedSalaryCost;
            this.performanceSalaryCost = performanceSalaryCost;
            this.corporateInsuranceCost = corporateInsuranceCost;
            this.corporateProvidentFundCost = corporateProvidentFundCost;
        }

        private static DepartmentSummary of(List<Map<String, Object>> employees,
                                            Map<Long, Map<String, String>> dynamicFieldValues,
                                            Map<Long, Map<Integer, String>> salaryOptionsByEmployee) {
            int totalCount = employees == null ? 0 : employees.size();
            int maleCount = 0;
            int femaleCount = 0;
            Map<String, Integer> educationCounts = new LinkedHashMap<>();
            for (String label : EDUCATION_SUMMARY_LABELS) {
                educationCounts.put(label, 0);
            }
            int moreThanTenYearsCount = 0;
            int fiveToTenYearsCount = 0;
            int twoToFiveYearsCount = 0;
            int oneToTwoYearsCount = 0;
            int lessThanOneYearCount = 0;
            int majorRelatedCount = 0;
            int majorUnrelatedCount = 0;
            int moreThanOneYearInternCount = 0;
            int formalOldEmployeeCount = 0;
            int probationEmployeeCount = 0;
            BigDecimal fixedSalaryCost = BigDecimal.ZERO;
            BigDecimal performanceSalaryCost = BigDecimal.ZERO;
            BigDecimal corporateInsuranceCost = BigDecimal.ZERO;
            BigDecimal corporateProvidentFundCost = BigDecimal.ZERO;

            List<Map<String, Object>> rows = employees == null ? Collections.emptyList() : employees;
            for (Map<String, Object> employee : rows) {
                Long employeeId = Convert.toLong(firstValue(employee, "employeeId", "employee_id"));
                Map<Integer, String> salaryOptions = salaryOptionsByEmployee == null || employeeId == null
                        ? Collections.emptyMap()
                        : salaryOptionsByEmployee.get(employeeId);
                Map<String, String> dynamicFields = dynamicFieldValues == null || employeeId == null
                        ? Collections.emptyMap()
                        : dynamicFieldValues.get(employeeId);
                fixedSalaryCost = fixedSalaryCost.add(fixedSalaryCost(salaryOptions, dynamicFields));
                performanceSalaryCost = performanceSalaryCost.add(performanceSalaryCost(salaryOptions));
                corporateInsuranceCost = corporateInsuranceCost.add(parseMoney(firstValue(
                        employee, "corporateInsuranceAmount", "corporate_insurance_amount")));
                corporateProvidentFundCost = corporateProvidentFundCost.add(parseMoney(firstValue(
                        employee, "corporateProvidentFundAmount", "corporate_provident_fund_amount")));

                String sex = sexText(firstValue(employee, "sex"));
                if ("男".equals(sex)) {
                    maleCount++;
                } else if ("女".equals(sex)) {
                    femaleCount++;
                }

                String education = educationSummaryLabel(firstValue(employee, "highestEducation", "highest_education"));
                if (educationCounts.containsKey(education)) {
                    educationCounts.put(education, educationCounts.get(education) + 1);
                }

                double companyAgeYears = companyAgeYears(employee);
                if (companyAgeYears >= 10D) {
                    moreThanTenYearsCount++;
                } else if (companyAgeYears >= 5D) {
                    fiveToTenYearsCount++;
                } else if (companyAgeYears >= 2D) {
                    twoToFiveYearsCount++;
                } else if (companyAgeYears >= 1D) {
                    oneToTwoYearsCount++;
                } else {
                    lessThanOneYearCount++;
                }

                if (hasMajor(employee)) {
                    majorRelatedCount++;
                } else {
                    majorUnrelatedCount++;
                }

                Object status = firstValue(employee, "status");
                Integer probation = Convert.toInt(firstValue(employee, "probation"), null);
                if (isInternStatus(status) && probation != null && probation >= 12) {
                    moreThanOneYearInternCount++;
                }
                if (isFormalStatus(status)) {
                    formalOldEmployeeCount++;
                }
                if (isProbationStatus(status)) {
                    probationEmployeeCount++;
                }
            }

            return new DepartmentSummary(
                    totalCount,
                    maleCount,
                    femaleCount,
                    educationCounts,
                    moreThanTenYearsCount,
                    fiveToTenYearsCount,
                    twoToFiveYearsCount,
                    oneToTwoYearsCount,
                    lessThanOneYearCount,
                    majorRelatedCount,
                    majorUnrelatedCount,
                    moreThanOneYearInternCount,
                    formalOldEmployeeCount,
                    probationEmployeeCount,
                    fixedSalaryCost,
                    performanceSalaryCost,
                    corporateInsuranceCost,
                    corporateProvidentFundCost
            );
        }

        private String genderRatioText() {
            return maleCount + "：" + femaleCount + "\n（男" + maleCount + "女" + femaleCount + "）";
        }

        private List<String> educationLines() {
            List<String> lines = new ArrayList<>(EDUCATION_SUMMARY_LABELS.size());
            for (String label : EDUCATION_SUMMARY_LABELS) {
                lines.add(label + "：" + educationCounts.get(label) + "人");
            }
            return lines;
        }

        private List<String> companyAgeLines() {
            return Arrays.asList(
                    "10年以上：" + moreThanTenYearsCount + "人",
                    "5-10年：" + fiveToTenYearsCount + "人",
                    "2-5年：" + twoToFiveYearsCount + "人",
                    "1-2年：" + oneToTwoYearsCount + "人",
                    "1年以下：" + lessThanOneYearCount + "人"
            );
        }

        private String majorSummaryText() {
            return "";
        }

        private String probationSummaryText() {
            return "实习期要求：一年以上实习生：" + moreThanOneYearInternCount
                    + "人\n正式老员工：" + formalOldEmployeeCount
                    + "人\n试用期人员：" + probationEmployeeCount + "人";
        }
    }

    private static final class FullAttendanceAmounts {
        private final BigDecimal ordinaryAmount;
        private final BigDecimal leaderAmount;

        private FullAttendanceAmounts(BigDecimal ordinaryAmount, BigDecimal leaderAmount) {
            this.ordinaryAmount = ordinaryAmount == null ? BigDecimal.ZERO : ordinaryAmount;
            this.leaderAmount = leaderAmount == null ? BigDecimal.ZERO : leaderAmount;
        }
    }

    private static String educationSummaryLabel(Object highestEducation) {
        String education = educationText(highestEducation);
        if ("硕士".equals(education) || "博士".equals(education) || "博士后".equals(education)) {
            return "本科";
        }
        if ("中职".equals(education) || "中技".equals(education)) {
            return "中专";
        }
        if ("小学".equals(education)) {
            return "初中";
        }
        return education;
    }

    private static boolean hasMajor(Map<String, Object> employee) {
        String major = text(firstValue(employee, "major"));
        return StrUtil.isNotBlank(major) && !"/".equals(major) && !"无".equals(major);
    }

    private static Map<String, DepartmentSummary> buildDepartmentSummaries(Map<String, List<Map<String, Object>>> employeesByDepartment,
                                                                           Map<Long, Map<String, String>> dynamicFieldValues,
                                                                           Map<Long, Map<Integer, String>> salaryOptionsByEmployee) {
        Map<String, DepartmentSummary> summaries = new LinkedHashMap<>();
        if (employeesByDepartment == null || employeesByDepartment.isEmpty()) {
            return summaries;
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : employeesByDepartment.entrySet()) {
            summaries.put(entry.getKey(), DepartmentSummary.of(entry.getValue(), dynamicFieldValues, salaryOptionsByEmployee));
        }
        return summaries;
    }

    static CompanyCostSummary companyCostSummary(List<Map<String, Object>> employees,
                                                  Map<Long, Map<String, String>> dynamicFieldValues,
                                                  Map<Long, Map<Integer, String>> salaryOptionsByEmployee) {
        Map<String, List<Map<String, Object>>> employeesByDepartment = groupByDepartment(
                employees == null ? Collections.emptyList() : employees);
        SummaryTotals totals = SummaryTotals.of(
                buildDepartmentSummaries(employeesByDepartment, dynamicFieldValues, salaryOptionsByEmployee));
        return new CompanyCostSummary(totals.totalCount, totals.fixedSalaryCost, totals.performanceSalaryCost,
                totals.corporateInsuranceCost, totals.corporateProvidentFundCost);
    }

    static final class CompanyCostSummary {
        final int employeeCount;
        final BigDecimal fixedSalaryCost;
        final BigDecimal performanceSalaryCost;
        final BigDecimal corporateInsuranceCost;
        final BigDecimal corporateProvidentFundCost;

        private CompanyCostSummary(int employeeCount,
                                   BigDecimal fixedSalaryCost,
                                   BigDecimal performanceSalaryCost,
                                   BigDecimal corporateInsuranceCost,
                                   BigDecimal corporateProvidentFundCost) {
            this.employeeCount = employeeCount;
            this.fixedSalaryCost = fixedSalaryCost;
            this.performanceSalaryCost = performanceSalaryCost;
            this.corporateInsuranceCost = corporateInsuranceCost;
            this.corporateProvidentFundCost = corporateProvidentFundCost;
        }
    }

    private static final class SummaryTotals {
        private final int totalCount;
        private final BigDecimal fixedSalaryCost;
        private final BigDecimal performanceSalaryCost;
        private final BigDecimal corporateInsuranceCost;
        private final BigDecimal corporateProvidentFundCost;

        private SummaryTotals(int totalCount,
                              BigDecimal fixedSalaryCost,
                              BigDecimal performanceSalaryCost,
                              BigDecimal corporateInsuranceCost,
                              BigDecimal corporateProvidentFundCost) {
            this.totalCount = totalCount;
            this.fixedSalaryCost = fixedSalaryCost;
            this.performanceSalaryCost = performanceSalaryCost;
            this.corporateInsuranceCost = corporateInsuranceCost;
            this.corporateProvidentFundCost = corporateProvidentFundCost;
        }

        private static SummaryTotals of(Map<String, DepartmentSummary> departmentSummaries) {
            int totalCount = 0;
            BigDecimal fixedSalaryCost = BigDecimal.ZERO;
            BigDecimal performanceSalaryCost = BigDecimal.ZERO;
            BigDecimal corporateInsuranceCost = BigDecimal.ZERO;
            BigDecimal corporateProvidentFundCost = BigDecimal.ZERO;
            if (departmentSummaries != null) {
                for (DepartmentSummary summary : departmentSummaries.values()) {
                    if (summary == null) {
                        continue;
                    }
                    totalCount += summary.totalCount;
                    fixedSalaryCost = fixedSalaryCost.add(summary.fixedSalaryCost);
                    performanceSalaryCost = performanceSalaryCost.add(summary.performanceSalaryCost);
                    corporateInsuranceCost = corporateInsuranceCost.add(summary.corporateInsuranceCost);
                    corporateProvidentFundCost = corporateProvidentFundCost.add(summary.corporateProvidentFundCost);
                }
            }
            return new SummaryTotals(totalCount, fixedSalaryCost, performanceSalaryCost, corporateInsuranceCost, corporateProvidentFundCost);
        }
    }

    private static boolean isFormalStatus(Object status) {
        String value = text(status);
        return "1".equals(value) || "正式".equals(value);
    }

    private static boolean isProbationStatus(Object status) {
        String value = text(status);
        return "2".equals(value) || "试用".equals(value);
    }

    private static boolean isInternStatus(Object status) {
        String value = text(status);
        return "3".equals(value) || "实习".equals(value);
    }

    private static double companyAgeYears(Map<String, Object> employee) {
        Object companyAge = firstValue(employee, "companyAge", "company_age");
        double parsedAge = parseCompanyAgeYears(text(companyAge));
        if (parsedAge >= 0D) {
            return parsedAge;
        }
        LocalDate startDate = parseDate(firstValue(employee, "companyAgeStartTime", "company_age_start_time", "entryTime", "entry_time"));
        if (startDate == null || startDate.isAfter(LocalDate.now())) {
            return 0D;
        }
        Period period = Period.between(startDate, LocalDate.now());
        return period.getYears() + period.getMonths() / 12D;
    }

    private static double parseCompanyAgeYears(String companyAgeText) {
        if (StrUtil.isBlank(companyAgeText)) {
            return -1D;
        }
        String value = companyAgeText.trim();
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            return Convert.toDouble(value, -1D);
        }
        Matcher yearMatcher = COMPANY_AGE_YEAR_PATTERN.matcher(value);
        Matcher monthMatcher = COMPANY_AGE_MONTH_PATTERN.matcher(value);
        double years = 0D;
        boolean matched = false;
        if (yearMatcher.find()) {
            years += Convert.toDouble(yearMatcher.group(1), 0D);
            matched = true;
        }
        if (monthMatcher.find()) {
            years += Convert.toDouble(monthMatcher.group(1), 0D) / 12D;
            matched = true;
        }
        return matched ? years : -1D;
    }

    private static LocalDate parseDate(Object value) {
        String valueText = dateText(value);
        if (StrUtil.isBlank(valueText) || !valueText.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.parse(valueText, DATE_FORMATTER);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static final class ExportStyles {
        private final CellStyle titleStyle;
        private final CellStyle headerStyle;
        private final CellStyle bodyStyle;

        private ExportStyles(Workbook workbook) {
            this.titleStyle = workbook.createCellStyle();
            this.titleStyle.setAlignment(HorizontalAlignment.CENTER);
            this.titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            this.titleStyle.setFont(titleFont);

            this.headerStyle = workbook.createCellStyle();
            this.headerStyle.setAlignment(HorizontalAlignment.CENTER);
            this.headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            this.headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            this.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            this.headerStyle.setWrapText(true);
            setThinBorder(this.headerStyle);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            this.headerStyle.setFont(headerFont);

            this.bodyStyle = workbook.createCellStyle();
            this.bodyStyle.setAlignment(HorizontalAlignment.CENTER);
            this.bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            this.bodyStyle.setWrapText(true);
            setThinBorder(this.bodyStyle);
        }

        private static void setThinBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
        }
    }
}
