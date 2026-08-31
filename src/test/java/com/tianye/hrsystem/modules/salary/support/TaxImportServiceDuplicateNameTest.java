package com.tianye.hrsystem.modules.salary.support;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.entity.HrmEmployeeAdditional;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.additional.mapper.HrmEmployeeAdditionalMapper;
import com.tianye.hrsystem.modules.additional.service.HrmAdditionalService;
import com.tianye.hrsystem.modules.additional.service.HrmEmployeeAdditionalService;
import com.tianye.hrsystem.modules.deduction.entity.HrmPersonalIncomeTax;
import com.tianye.hrsystem.modules.deduction.mapper.HrmPersonalIncomeTaxMapper;
import com.tianye.hrsystem.modules.deduction.service.HrmPersonalIncomeTaxService;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TaxImportServiceDuplicateNameTest {

    @Test
    public void personalIncomeTaxImport_shouldMatchDuplicateNamesByMobile() throws Exception {
        TestablePersonalIncomeTaxService service = personalIncomeTaxService(
                employee(1001L, "王芳", "13032750052"),
                employee(1002L, "王芳", "15871989405")
        );

        service.resolvePersonalIncomeTaxData(excelFile(
                row("姓名", "工号", "岗位", "部门", "累计收入", "累计减除费用", "累计专项扣除", "累计已缴税额", "手机"),
                row("说明", "", "", "", "", "", "", "", ""),
                row("王芳", "TYNG-107", "", "", "32031", "25000", "1200", "300", "13032750052"),
                row("王芳", "TYNG-437", "", "", "29461", "25000", "1000", "200", "15871989405")
        ), "2026-05");

        Assert.assertEquals(2, service.savedRecords.size());
        Assert.assertEquals(Long.valueOf(1001L), service.savedRecords.get(0).getEmployeeId());
        Assert.assertEquals(Long.valueOf(1002L), service.savedRecords.get(1).getEmployeeId());
    }

    @Test
    public void personalIncomeTaxImport_shouldRejectDuplicateEmployeePeriod() throws Exception {
        TestablePersonalIncomeTaxService service = personalIncomeTaxService(employee(1001L, "王芳", "13032750052"));

        try {
            service.resolvePersonalIncomeTaxData(excelFile(
                    row("姓名", "工号", "岗位", "部门", "累计收入", "累计减除费用", "累计专项扣除", "累计已缴税额", "手机"),
                    row("说明", "", "", "", "", "", "", "", ""),
                    row("王芳", "TYNG-107", "", "", "32031", "25000", "1200", "300", "13032750052"),
                    row("王芳", "TYNG-107", "", "", "32031", "25000", "1200", "300", "13032750052")
            ), "2026-05");
            Assert.fail("同一员工同一年月重复导入应失败");
        } catch (CrmException exception) {
            Assert.assertTrue(exception.getMsg().contains("导入文件存在重复员工"));
            Assert.assertTrue(exception.getMsg().contains("2026-05"));
        }

        Assert.assertTrue(service.savedRecords.isEmpty());
        Mockito.verify(service.mapper, Mockito.never()).delete(Mockito.any(Wrapper.class));
    }

    @Test
    public void additionalImport_shouldMatchDuplicateNamesByMobile() throws Exception {
        TestableAdditionalService service = additionalService(
                employee(1001L, "王芳", "13032750052"),
                employee(1002L, "王芳", "15871989405")
        );

        service.resolveAdditionalData(excelFile(
                row("姓名", "工号", "岗位", "部门", "子女教育", "住房租金", "住房贷款利息", "赡养老人", "继续教育", "养幼女", "手机"),
                row("说明", "", "", "", "", "", "", "", "", "", ""),
                row("王芳", "TYNG-107", "", "", "1000", "0", "0", "2000", "0", "0", "13032750052"),
                row("王芳", "TYNG-437", "", "", "0", "1500", "0", "0", "0", "0", "15871989405")
        ), "2026", "05");

        Assert.assertEquals(2, service.savedRecords.size());
        Assert.assertEquals(Long.valueOf(1001L), service.savedRecords.get(0).getEmployeeId());
        Assert.assertEquals(Long.valueOf(1002L), service.savedRecords.get(1).getEmployeeId());
    }

    @Test
    public void employeeAdditionalImport_shouldMatchDuplicateNamesByMobile() throws Exception {
        TestableEmployeeAdditionalService service = employeeAdditionalService(
                employee(1001L, "王芳", "13032750052"),
                employee(1002L, "王芳", "15871989405")
        );

        service.resolveEmployeeAdditionalData(excelFile(
                row("姓名", "子女教育", "住房贷款利息", "住房租金", "赡养老人", "继续教育", "养幼女", "年份", "手机"),
                row("说明", "", "", "", "", "", "", "", ""),
                row("王芳", "12000", "0", "0", "24000", "0", "0", "2025", "13032750052"),
                row("王芳", "0", "0", "18000", "0", "0", "0", "2026", "15871989405")
        ));

        Assert.assertEquals(2, service.savedRecords.size());
        Assert.assertEquals(Long.valueOf(1001L), service.savedRecords.get(0).getEmployeeId());
        Assert.assertEquals(Integer.valueOf(2025), service.savedRecords.get(0).getYear());
        Assert.assertEquals(Long.valueOf(1002L), service.savedRecords.get(1).getEmployeeId());
        Assert.assertEquals(Integer.valueOf(2026), service.savedRecords.get(1).getYear());
        Mockito.verify(service.mapper, Mockito.times(2)).delete(Mockito.any(Wrapper.class));
    }

    private TestablePersonalIncomeTaxService personalIncomeTaxService(HrmEmployee... employees) {
        TestablePersonalIncomeTaxService service = new TestablePersonalIncomeTaxService();
        hrmEmployeeRepository repository = Mockito.mock(hrmEmployeeRepository.class);
        Mockito.when(repository.findAll()).thenReturn(Arrays.asList(employees));
        service.mapper = Mockito.mock(HrmPersonalIncomeTaxMapper.class);
        ReflectionTestUtils.setField(service, "employeeRepository", repository);
        ReflectionTestUtils.setField(service, "hrmPersonalIncomeTaxMapper", service.mapper);
        return service;
    }

    private TestableAdditionalService additionalService(HrmEmployee... employees) {
        TestableAdditionalService service = new TestableAdditionalService();
        hrmEmployeeRepository repository = Mockito.mock(hrmEmployeeRepository.class);
        Mockito.when(repository.findAll()).thenReturn(Arrays.asList(employees));
        service.mapper = Mockito.mock(HrmAdditionalMapper.class);
        ReflectionTestUtils.setField(service, "employeeRepository", repository);
        ReflectionTestUtils.setField(service, "hrmAdditionalMapper", service.mapper);
        return service;
    }

    private TestableEmployeeAdditionalService employeeAdditionalService(HrmEmployee... employees) {
        TestableEmployeeAdditionalService service = new TestableEmployeeAdditionalService();
        hrmEmployeeRepository repository = Mockito.mock(hrmEmployeeRepository.class);
        Mockito.when(repository.findAll()).thenReturn(Arrays.asList(employees));
        service.mapper = Mockito.mock(HrmEmployeeAdditionalMapper.class);
        ReflectionTestUtils.setField(service, "employeeRepository", repository);
        ReflectionTestUtils.setField(service, "hrmEmployeeAdditionalMapper", service.mapper);
        return service;
    }

    private HrmEmployee employee(Long employeeId, String employeeName, String mobile) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(employeeName);
        employee.setMobile(mobile);
        employee.setIsDel(0);
        return employee;
    }

    private Object[] row(Object... values) {
        return values;
    }

    private MultipartFile excelFile(Object[]... rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            Object[] values = rows[rowIndex];
            for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
                row.createCell(columnIndex).setCellValue(String.valueOf(values[columnIndex]));
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "tax-import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private static class TestablePersonalIncomeTaxService extends HrmPersonalIncomeTaxService {
        private HrmPersonalIncomeTaxMapper mapper;
        private final List<HrmPersonalIncomeTax> savedRecords = new ArrayList<>();

        @Override
        public boolean saveBatch(Collection<HrmPersonalIncomeTax> entityList) {
            savedRecords.addAll(entityList);
            return true;
        }
    }

    private static class TestableAdditionalService extends HrmAdditionalService {
        private HrmAdditionalMapper mapper;
        private final List<HrmAdditional> savedRecords = new ArrayList<>();

        @Override
        public boolean saveBatch(Collection<HrmAdditional> entityList) {
            savedRecords.addAll(entityList);
            return true;
        }
    }

    private static class TestableEmployeeAdditionalService extends HrmEmployeeAdditionalService {
        private HrmEmployeeAdditionalMapper mapper;
        private final List<HrmEmployeeAdditional> savedRecords = new ArrayList<>();

        @Override
        public boolean saveBatch(Collection<HrmEmployeeAdditional> entityList) {
            savedRecords.addAll(entityList);
            return true;
        }
    }
}
