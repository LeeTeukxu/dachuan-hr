package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HrmEmployeeControllerTest {

    @Test
    public void downloadEmployeeRosterTemplate_shouldWriteValidRosterWorkbook() throws Exception {
        HrmEmployeeController controller = new HrmEmployeeController();
        ReflectionTestUtils.setField(controller, "employeeService", Mockito.mock(IHrmEmployeeService.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadEmployeeRosterTemplate(response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains("employee_module.xlsx"));
        Assert.assertTrue(response.getContentAsByteArray().length > 0);
        double originalMinInflateRatio = ZipSecureFile.getMinInflateRatio();
        ZipSecureFile.setMinInflateRatio(0.001);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Assert.assertNotNull(workbook.getSheet("田野农谷"));
        } finally {
            ZipSecureFile.setMinInflateRatio(originalMinInflateRatio);
        }
    }

    @Test
    public void downloadEmployeeRosterTemplate_shouldPutSalaryAmountColumnsUnderMergedSalaryBenefitGroup() throws Exception {
        HrmEmployeeController controller = new HrmEmployeeController();
        ReflectionTestUtils.setField(controller, "employeeService", Mockito.mock(IHrmEmployeeService.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadEmployeeRosterTemplate(response);

        double originalMinInflateRatio = ZipSecureFile.getMinInflateRatio();
        ZipSecureFile.setMinInflateRatio(0.001);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheet("田野农谷");
            Assert.assertNotNull(sheet);
            Row groupRow = sheet.getRow(0);
            Row headerRow = sheet.getRow(1);
            Assert.assertNotNull(groupRow);
            Assert.assertNotNull(headerRow);

            Map<String, Integer> headerIndex = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
                String header = formatter.formatCellValue(headerRow.getCell(columnIndex));
                if (header != null && !header.trim().isEmpty()) {
                    headerIndex.put(header.replace("\n", "").trim(), columnIndex);
                }
            }

            Assert.assertFalse("花名册模板不应再新增薪资级别列，应使用已有薪资等级列", headerIndex.containsKey("薪资级别"));
            Assert.assertTrue("花名册模板应包含已有薪资等级列", headerIndex.containsKey("薪资等级"));
            Assert.assertTrue("花名册模板应包含固定绩效列", headerIndex.containsKey("固定绩效"));
            Assert.assertTrue("花名册模板应包含职务补助列", headerIndex.containsKey("职务补助"));
            Assert.assertTrue("花名册模板应包含其他补助列", headerIndex.containsKey("其他补助"));
            Assert.assertEquals("固定绩效应放在薪酬福利分组", "薪酬福利", formatter.formatCellValue(groupRow.getCell(headerIndex.get("固定绩效"))));
            Assert.assertEquals("固定绩效应放在薪资等级旁边", headerIndex.get("薪资等级") + 1, headerIndex.get("固定绩效").intValue());
            Assert.assertEquals("职务补助应放在固定绩效旁边", headerIndex.get("固定绩效") + 1, headerIndex.get("职务补助").intValue());
            Assert.assertEquals("其他补助应放在职务补助旁边", headerIndex.get("职务补助") + 1, headerIndex.get("其他补助").intValue());
            Assert.assertTrue("固定绩效列应设置两位小数数字格式",
                    sheet.getColumnStyle(headerIndex.get("固定绩效")).getDataFormatString().contains("0.00"));
            Assert.assertTrue("职务补助列应设置两位小数数字格式",
                    sheet.getColumnStyle(headerIndex.get("职务补助")).getDataFormatString().contains("0.00"));
            Assert.assertTrue("其他补助列应设置两位小数数字格式",
                    sheet.getColumnStyle(headerIndex.get("其他补助")).getDataFormatString().contains("0.00"));
            CellRangeAddress salaryBenefitGroup = mergedRegionContaining(sheet, 0, headerIndex.get("固定绩效"));
            Assert.assertNotNull("固定绩效必须落在薪酬福利合并父表头范围内，不能生成第二个孤立父表头", salaryBenefitGroup);
            Assert.assertTrue("薪酬福利父表头应覆盖薪资等级、固定绩效、职务补助、其他补助",
                    salaryBenefitGroup.getFirstColumn() <= headerIndex.get("薪资等级")
                            && salaryBenefitGroup.getLastColumn() >= headerIndex.get("其他补助"));
            Assert.assertEquals("薪酬福利", formatter.formatCellValue(groupRow.getCell(salaryBenefitGroup.getFirstColumn())));
        } finally {
            ZipSecureFile.setMinInflateRatio(originalMinInflateRatio);
        }
    }

    private CellRangeAddress mergedRegionContaining(Sheet sheet, int rowIndex, int columnIndex) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(rowIndex, columnIndex)) {
                return region;
            }
        }
        return null;
    }

    @Test
    public void exportDepartmentDetail_shouldDelegateToEmployeeService() throws Exception {
        IHrmEmployeeService employeeService = Mockito.mock(IHrmEmployeeService.class);
        HrmEmployeeController controller = new HrmEmployeeController();
        ReflectionTestUtils.setField(controller, "employeeService", employeeService);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDepartmentDetail(response);

        Mockito.verify(employeeService).exportDepartmentDetail(response);
    }
}
