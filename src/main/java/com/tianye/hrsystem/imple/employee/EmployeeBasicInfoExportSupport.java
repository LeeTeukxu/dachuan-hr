package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianye.hrsystem.enums.EmployeeEducationEnum;
import com.tianye.hrsystem.enums.EmployeeContractType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EmployeeBasicInfoExportSupport {

    private static final String TEMPLATE_RESOURCE = "export/副本人资系统导出员工基础信息模版.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String OPEN_ENDED_CONTRACT_TEXT = "无固定期限";
    private static final List<String> HEADERS = Collections.unmodifiableList(Arrays.asList(
            "姓名", "性别", "部门", "职位", "入职日期", "个人电话", "身份证号", "籍贯", "现居地址", "出生日期", "年龄", "工龄", "民族",
            "用工性质", "政治\n面貌", "薪资等级", "学历", "紧急联系人", "与本人关系", "电话号码", "劳动/劳务合同期限", "劳动合同次数", "开始时间", "结束时间"
    ));

    private EmployeeBasicInfoExportSupport() {
    }

    static List<String> headers() {
        return HEADERS;
    }

    static List<List<String>> buildRows(List<Map<String, Object>> employees, Map<Long, Map<String, String>> dynamicFieldValues) {
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> employee : employees) {
            Long employeeId = Convert.toLong(firstValue(employee, "employeeId", "employee_id"));
            Map<String, String> dynamicFields = dynamicFieldValues == null ? Collections.emptyMap() : dynamicFieldValues.get(employeeId);
            List<String> row = new ArrayList<>();
            for (String header : HEADERS) {
                row.add(valueForHeader(header, employee, dynamicFields));
            }
            rows.add(row);
        }
        return rows;
    }

    static byte[] buildWorkbook(List<List<String>> rows) throws IOException {
        try (InputStream templateStream = EmployeeBasicInfoExportSupport.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (templateStream == null) {
                throw new IOException("导出模板不存在：" + TEMPLATE_RESOURCE);
            }
            try (Workbook workbook = WorkbookFactory.create(templateStream); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.getSheetAt(0);
                writeHeaders(sheet);
                writeRows(sheet, rows == null ? Collections.emptyList() : rows);
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    static String normalizeKey(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private static void writeHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            headerRow = sheet.createRow(0);
        }
        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            Cell cell = headerRow.getCell(columnIndex);
            if (cell == null) {
                cell = headerRow.createCell(columnIndex);
            }
            cell.setCellValue(HEADERS.get(columnIndex));
        }
    }

    private static void writeRows(Sheet sheet, List<List<String>> rows) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.getRow(rowIndex + 1);
            if (row == null) {
                row = sheet.createRow(rowIndex + 1);
            }
            List<String> values = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                String value = columnIndex < values.size() ? values.get(columnIndex) : "";
                cell.setCellValue(value == null ? "" : value);
            }
        }
    }

    private static String valueForHeader(String header, Map<String, Object> employee, Map<String, String> dynamicFields) {
        String normalizedHeader = normalizeKey(header);
        switch (normalizedHeader) {
            case "姓名":
                return text(firstValue(employee, "employeeName", "employee_name"));
            case "性别":
                return sexText(firstValue(employee, "sex"));
            case "部门":
                return text(firstValue(employee, "deptName", "dept_name"));
            case "职位":
                return text(firstValue(employee, "post"));
            case "入职日期":
                return dateText(firstValue(employee, "entryTime", "entry_time"));
            case "个人电话":
                return text(firstValue(employee, "mobile"));
            case "身份证号":
                return text(firstValue(employee, "idNumber", "id_number"));
            case "籍贯":
                return text(firstValue(employee, "nativePlace", "native_place"));
            case "现居地址":
                return dynamicValue(dynamicFields, "现居地址", "现居住地");
            case "出生日期":
                return dateText(firstValue(employee, "dateOfBirth", "date_of_birth"));
            case "年龄":
                return text(firstValue(employee, "age"));
            case "工龄":
                return text(firstValue(employee, "companyAge", "company_age"));
            case "民族":
                return text(firstValue(employee, "nation"));
            case "用工性质":
                return firstNonBlank(dynamicValue(dynamicFields, "用工性质"), employmentNatureText(employee));
            case "政治面貌":
                return dynamicValue(dynamicFields, "政治面貌", "politicsStatus", "politicalStatus");
            case "薪资等级":
                return dynamicValue(dynamicFields, "薪资等级", "薪资类别", "职务级别");
            case "学历":
                return firstNonBlank(educationText(firstValue(employee, "highestEducation", "highest_education")), educationText(dynamicValue(dynamicFields, "学历", "最高学历", "education", "highestEducation")));
            case "紧急联系人":
                return text(firstValue(employee, "emergencyContact", "contactsName", "contacts_name"));
            case "与本人关系":
                return text(firstValue(employee, "emergencyRelation", "relation"));
            case "电话号码":
                return text(firstValue(employee, "emergencyPhone", "contactsPhone", "contacts_phone"));
            case "劳动/劳务合同期限":
                return contractTermText(employee);
            case "劳动合同次数":
                return text(firstValue(employee, "contractSignCount", "contract_sign_count"));
            case "开始时间":
                return dateText(firstValue(employee, "firstContractStartTime", "first_contract_start_time", "startTime", "start_time"));
            case "结束时间":
                return contractEndTimeText(employee);
            default:
                return "";
        }
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
        Map<String, String> normalizedFields = new HashMap<>();
        dynamicFields.forEach((key, value) -> normalizedFields.putIfAbsent(normalizeKey(key), value));
        for (String key : keys) {
            String value = normalizedFields.get(normalizeKey(key));
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String text(Object value) {
        return value == null ? "" : Convert.toStr(value, "").trim();
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

    private static String employmentNatureText(Map<String, Object> employee) {
        Integer status = Convert.toInt(firstValue(employee, "status"), null);
        Integer contractType = Convert.toInt(firstValue(employee, "contractType", "contract_type"), null);
        if (Integer.valueOf(5).equals(status) || Integer.valueOf(5).equals(contractType) || Integer.valueOf(7).equals(contractType)) {
            return "劳务用工";
        }
        return "劳动用工";
    }

    private static String contractTermText(Map<String, Object> employee) {
        if (isOpenEndedContract(employee, "latestContractType", "latest_contract_type", "contractType", "contract_type")) {
            return OPEN_ENDED_CONTRACT_TEXT;
        }
        String latestContractStartTime = dateText(firstValue(employee, "latestContractStartTime", "latest_contract_start_time"));
        String latestContractEndTime = dateText(firstValue(employee, "latestContractEndTime", "latest_contract_end_time"));
        if (StrUtil.isNotBlank(latestContractStartTime) && StrUtil.isNotBlank(latestContractEndTime)) {
            return latestContractStartTime + "-" + latestContractEndTime;
        }
        String startTime = dateText(firstValue(employee, "startTime", "start_time"));
        String endTime = dateText(firstValue(employee, "endTime", "end_time"));
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            return startTime + "-" + endTime;
        }
        String term = text(firstValue(employee, "term"));
        if (StrUtil.isNotBlank(term)) {
            return term.endsWith("年") ? term : term + "年";
        }
        return "";
    }

    private static String contractEndTimeText(Map<String, Object> employee) {
        if (isOpenEndedContract(employee, "lastContractType", "last_contract_type", "latestContractType", "latest_contract_type", "contractType", "contract_type")) {
            return OPEN_ENDED_CONTRACT_TEXT;
        }
        return dateText(firstValue(employee, "lastContractEndTime", "last_contract_end_time", "latestContractEndTime", "latest_contract_end_time", "endTime", "end_time"));
    }

    private static boolean isOpenEndedContract(Map<String, Object> employee, String... keys) {
        Integer contractType = Convert.toInt(firstValue(employee, keys), null);
        return Integer.valueOf(EmployeeContractType.NO_FIXED_TERM_LABOR_CONTRACT.getValue()).equals(contractType);
    }
}
