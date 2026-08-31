package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import net.lingala.zip4j.ZipFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDepartmentDetailCrossCompanyExportSupportTest {

    @Test
    public void administrativeManager_shouldExportAllFiveCompaniesIncludingChengdu() {
        Assert.assertTrue(EmployeeDepartmentDetailCrossCompanyExportSupport.isAdministrativeManager(
                loginUser("行政经理")));
        Assert.assertFalse(EmployeeDepartmentDetailCrossCompanyExportSupport.isAdministrativeManager(
                loginUser("行政经理 ")));
        Assert.assertEquals(Arrays.asList("0001", "0002", "0003", "0004", "0005"),
                EmployeeDepartmentDetailCrossCompanyExportSupport.companyIds());
    }

    @Test
    public void companyDepartmentFileName_shouldUseCompanyNameWithoutDateSuffix() {
        Assert.assertEquals("达川公司部门明细.xlsx",
                EmployeeDepartmentDetailCrossCompanyExportSupport.companyDepartmentFileName("达川公司"));
    }

    @Test
    public void withCompanyContext_shouldRestoreOriginalContextAfterWork() throws Exception {
        LoginUserInfo original = loginUser("行政经理");
        original.setCompanyId("0001");
        CompanyContext.set(original);
        try {
            String result = EmployeeDepartmentDetailCrossCompanyExportSupport.withCompanyContext(
                    original,
                    "0003",
                    () -> {
                        Assert.assertEquals("0003", CompanyContext.get().getCompanyId());
                        Assert.assertEquals("行政经理", CompanyContext.get().getRoleName());
                        return "done";
                    });
            Assert.assertEquals("done", result);
            Assert.assertSame(original, CompanyContext.get());
        } finally {
            CompanyContext.clear();
        }
    }

    @Test
    public void withCompanyContext_shouldNotReuseOriginalCompanyNameForOtherCompany() throws Exception {
        LoginUserInfo original = loginUser("行政经理");
        original.setCompanyId("0001");
        original.setCompanyName("当前登录公司");
        CompanyContext.set(original);
        try {
            EmployeeDepartmentDetailCrossCompanyExportSupport.withCompanyContext(
                    original,
                    "0003",
                    () -> {
                        Assert.assertEquals("0003", CompanyContext.get().getCompanyId());
                        Assert.assertNull("跨公司导出公司名必须从目标库顶层组织读取，不能复用当前登录公司名",
                                CompanyContext.get().getCompanyName());
                        return null;
                    });
        } finally {
            CompanyContext.clear();
        }
    }

    @Test
    public void groupSummary_shouldMatchReferenceLayoutWithoutMealColumn() throws Exception {
        List<EmployeeDepartmentDetailGroupSummarySupport.CompanySummary> companies = Arrays.asList(
                new EmployeeDepartmentDetailGroupSummarySupport.CompanySummary(
                        "北海田野", 2, "100", "20", "30", "40"),
                new EmployeeDepartmentDetailGroupSummarySupport.CompanySummary(
                        "达川", 1, "200", "30", "40", "50")
        );

        byte[] bytes = EmployeeDepartmentDetailGroupSummarySupport.buildWorkbook(
                "田野股份集团总表", companies);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("集团总表");
            Assert.assertNotNull(sheet);
            Assert.assertEquals("田野股份集团总表", value(sheet, 0, 0));
            Assert.assertEquals("企业人数", value(sheet, 1, 1));
            Assert.assertEquals("月总固定薪资成本", value(sheet, 1, 2));
            Assert.assertEquals("月总绩效薪资成本", value(sheet, 1, 3));
            Assert.assertEquals("月总社保成本", value(sheet, 1, 4));
            Assert.assertEquals("月总公积金成本", value(sheet, 1, 5));
            Assert.assertEquals("总成本", value(sheet, 1, 6));
            Assert.assertEquals("集团总数", value(sheet, 2, 0));
            Assert.assertEquals("3", value(sheet, 2, 1));
            Assert.assertEquals("300", value(sheet, 2, 2));
            Assert.assertEquals("50", value(sheet, 2, 3));
            Assert.assertEquals("70", value(sheet, 2, 4));
            Assert.assertEquals("90", value(sheet, 2, 5));
            Assert.assertEquals("510", value(sheet, 2, 6));
            Assert.assertEquals("北海田野", value(sheet, 3, 0));
            Assert.assertEquals("达川", value(sheet, 4, 0));
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Assert.assertFalse(value(sheet, rowIndex, 0).contains("伙食"));
                Assert.assertFalse(value(sheet, rowIndex, 1).contains("伙食"));
                Assert.assertFalse(value(sheet, rowIndex, 2).contains("伙食"));
                Assert.assertFalse(value(sheet, rowIndex, 3).contains("伙食"));
                Assert.assertFalse(value(sheet, rowIndex, 4).contains("伙食"));
                Assert.assertFalse(value(sheet, rowIndex, 5).contains("伙食"));
                Assert.assertFalse(value(sheet, rowIndex, 6).contains("伙食"));
            }
        }
    }

    @Test
    public void encryptedZip_shouldRequirePasswordAndContainAllWorkbooks() throws Exception {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("北海田野部门明细.xlsx", new byte[]{1, 2, 3});
        files.put("达川部门明细.xlsx", new byte[]{4, 5, 6});
        files.put("集团总表.xlsx", new byte[]{7, 8, 9});

        byte[] archive = EmployeeDepartmentDetailCrossCompanyExportSupport.buildEncryptedZip(
                files, "Archive-20260824");
        Path archivePath = Files.createTempFile("employee-department-detail-", ".zip");
        Files.write(archivePath, archive);
        try {
            ZipFile zipFile = new ZipFile(archivePath.toFile(), "Archive-20260824".toCharArray());
            Assert.assertEquals(3, zipFile.getFileHeaders().size());
            Assert.assertTrue(zipFile.isEncrypted());
            Assert.assertArrayEquals(new byte[]{1, 2, 3},
                    readAll(zipFile.getInputStream(zipFile.getFileHeader("北海田野部门明细.xlsx"))));
        } finally {
            Files.deleteIfExists(archivePath);
        }
    }

    private static LoginUserInfo loginUser(String roleName) {
        LoginUserInfo info = new LoginUserInfo();
        info.setRoleName(roleName);
        return info;
    }

    private static String value(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null || row.getCell(columnIndex) == null ? "" : row.getCell(columnIndex).toString();
    }

    private static byte[] readAll(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        }
    }
}
