package com.tianye.hrsystem.imple.employee;

import org.junit.Assert;
import org.junit.Test;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeBasicInfoExportSupportTest {

    @Test
    public void headers_shouldMatchEmployeeBasicInfoTemplate() {
        Assert.assertEquals(Arrays.asList(
                "姓名", "性别", "部门", "职位", "入职日期", "个人电话", "身份证号", "籍贯", "现居地址", "出生日期", "年龄", "工龄", "民族",
                "用工性质", "政治\n面貌", "薪资等级", "学历", "紧急联系人", "与本人关系", "电话号码", "劳动/劳务合同期限", "劳动合同次数", "开始时间", "结束时间"
        ), EmployeeBasicInfoExportSupport.headers());
    }

    @Test
    public void buildRows_shouldMapTemplateFieldsFromDatabaseRowsAndDynamicFields() {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1001L);
        employee.put("employeeName", "张三");
        employee.put("sex", 1);
        employee.put("deptName", "生产部");
        employee.put("post", "操作工");
        employee.put("entryTime", "2024-01-02");
        employee.put("mobile", "13800001111");
        employee.put("idNumber", "420100199001011234");
        employee.put("nativePlace", "湖北武汉");
        employee.put("dateOfBirth", "1990-01-01");
        employee.put("age", 36);
        employee.put("companyAge", 2);
        employee.put("nation", "汉族");
        employee.put("status", 5);
        employee.put("highestEducation", 8);
        employee.put("emergencyContact", "李四");
        employee.put("emergencyRelation", "配偶");
        employee.put("emergencyPhone", "13900002222");
        employee.put("term", 3);
        employee.put("contractSignCount", 2);
        employee.put("startTime", "2024-01-02");
        employee.put("endTime", "2027-01-01");
        employee.put("firstContractStartTime", "2023-01-02");
        employee.put("latestContractStartTime", "2025-01-02");
        employee.put("latestContractEndTime", "2028-01-01");
        employee.put("lastContractEndTime", "2028-01-01");

        Map<String, String> dynamicFields = new HashMap<>();
        dynamicFields.put("现居地址", "海南海口");
        dynamicFields.put("政治面貌", "群众");
        dynamicFields.put("薪资等级", "P3");

        List<List<String>> rows = EmployeeBasicInfoExportSupport.buildRows(
                Collections.singletonList(employee),
                Collections.singletonMap(1001L, dynamicFields)
        );

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(Arrays.asList(
                "张三", "男", "生产部", "操作工", "2024-01-02", "13800001111", "420100199001011234", "湖北武汉", "海南海口", "1990-01-01", "36", "2", "汉族",
                "劳务用工", "群众", "P3", "本科", "李四", "配偶", "13900002222", "2025-01-02-2028-01-01", "2", "2023-01-02", "2028-01-01"
        ), rows.get(0));
    }

    @Test
    public void buildRows_shouldDisplayOpenEndedContractWithoutEndDate() {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1001L);
        employee.put("employeeName", "张三");
        employee.put("latestContractType", 2);
        employee.put("lastContractType", 2);
        employee.put("latestContractStartTime", "2024-01-02");
        employee.put("latestContractEndTime", "");
        employee.put("lastContractEndTime", "");
        employee.put("term", null);

        List<List<String>> rows = EmployeeBasicInfoExportSupport.buildRows(
                Collections.singletonList(employee),
                Collections.emptyMap()
        );

        int contractTermIndex = EmployeeBasicInfoExportSupport.headers().indexOf("劳动/劳务合同期限");
        int endTimeIndex = EmployeeBasicInfoExportSupport.headers().indexOf("结束时间");
        Assert.assertEquals("无固定期限", rows.get(0).get(contractTermIndex));
        Assert.assertEquals("无固定期限", rows.get(0).get(endTimeIndex));
    }

    @Test
    public void buildRows_shouldUseRosterFallbackFieldsForTemplateColumns() {
        Map<String, Object> employee = new HashMap<>();
        employee.put("employeeId", 1002L);
        employee.put("employeeName", "李四");

        Map<String, String> dynamicFields = new HashMap<>();
        dynamicFields.put("现居住地", "湖北武汉");
        dynamicFields.put("politicalStatus", "党员");
        dynamicFields.put("薪资类别", "固定月薪");
        dynamicFields.put("最高学历", "8");

        List<List<String>> rows = EmployeeBasicInfoExportSupport.buildRows(
                Collections.singletonList(employee),
                Collections.singletonMap(1002L, dynamicFields)
        );

        Assert.assertEquals("湖北武汉", rows.get(0).get(EmployeeBasicInfoExportSupport.headers().indexOf("现居地址")));
        Assert.assertEquals("党员", rows.get(0).get(EmployeeBasicInfoExportSupport.headers().indexOf("政治\n面貌")));
        Assert.assertEquals("固定月薪", rows.get(0).get(EmployeeBasicInfoExportSupport.headers().indexOf("薪资等级")));
        Assert.assertEquals("本科", rows.get(0).get(EmployeeBasicInfoExportSupport.headers().indexOf("学历")));
    }

    @Test
    public void buildWorkbook_shouldUseTemplateResourceAndWriteRows() throws Exception {
        byte[] bytes = EmployeeBasicInfoExportSupport.buildWorkbook(Collections.singletonList(Collections.nCopies(EmployeeBasicInfoExportSupport.headers().size(), "测试")));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Assert.assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            Assert.assertEquals("测试", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            Assert.assertEquals("结束时间", workbook.getSheetAt(0).getRow(0).getCell(23).getStringCellValue());
        }
    }

}
