package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.common.CrmException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HrmEmployeeContractServiceImplTest {

    @Test
    public void addOrUpdateContract_shouldCalculateTermFromStartAndEndDates() {
        TestableContractService service = new TestableContractService();
        ReflectionTestUtils.setField(service, "employeeActionRecordService", Mockito.mock(EmployeeActionRecordServiceImpl.class));
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setEmployeeId(10001L);
        contract.setStartTime(LocalDate.of(2024, 1, 1));
        contract.setEndTime(LocalDate.of(2026, 12, 31));
        contract.setTerm(1);

        service.addOrUpdateContract(contract);

        Assert.assertEquals("合同期限应按开始日期和结束日期自动计算并覆盖前端传值", Integer.valueOf(3), service.savedContract.getTerm());
    }

    @Test
    public void addOrUpdateContract_shouldAllowOpenEndedContractWithoutEndDate() {
        TestableContractService service = new TestableContractService();
        ReflectionTestUtils.setField(service, "employeeActionRecordService", Mockito.mock(EmployeeActionRecordServiceImpl.class));
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setEmployeeId(10001L);
        contract.setContractType(2);
        contract.setStartTime(LocalDate.of(2024, 1, 1));
        contract.setEndTime(null);
        contract.setTerm(10);

        service.addOrUpdateContract(contract);

        Assert.assertNull("无固定期限劳动合同不应要求合同结束日期，也不应保存前端传入的固定年限", service.savedContract.getTerm());
        Assert.assertNull("无固定期限劳动合同应以空合同结束日期表达无到期日", service.savedContract.getEndTime());
    }

    @Test
    public void addOrUpdateContract_shouldClearSubmittedEndDateForOpenEndedContract() {
        TestableContractService service = new TestableContractService();
        ReflectionTestUtils.setField(service, "employeeActionRecordService", Mockito.mock(EmployeeActionRecordServiceImpl.class));
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setEmployeeId(10001L);
        contract.setContractType(2);
        contract.setStartTime(LocalDate.of(2024, 1, 1));
        contract.setEndTime(LocalDate.of(2099, 12, 31));
        contract.setTerm(10);

        service.addOrUpdateContract(contract);

        Assert.assertNull("无固定期限劳动合同不应保留前端残留或导入文件中的占位结束日期", service.savedContract.getEndTime());
        Assert.assertNull("无固定期限劳动合同不应保留前端残留期限", service.savedContract.getTerm());
    }

    @Test
    public void addOrUpdateContract_shouldStillRequireStartDateForOpenEndedContract() {
        TestableContractService service = new TestableContractService();
        ReflectionTestUtils.setField(service, "employeeActionRecordService", Mockito.mock(EmployeeActionRecordServiceImpl.class));
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setEmployeeId(10001L);
        contract.setContractType(2);
        contract.setStartTime(null);
        contract.setEndTime(null);

        try {
            service.addOrUpdateContract(contract);
            Assert.fail("无固定期限劳动合同仍应要求合同开始日期");
        } catch (CrmException exception) {
            Assert.assertEquals("合同开始日期不能为空", exception.getMsg());
        }
    }

    @Test
    public void importContracts_shouldAddOneContractPerRowAndMatchEmployeeByNameAndPhone() throws Exception {
        TestableContractService service = contractImportService(
                employee(10001L, "张三", "13900000000"),
                employee(10002L, "张三", "13911112222")
        );

        service.importContracts(contractImportFile(
                contractRow("张三", "13900000000", "HT-001"),
                contractRow("张三", "13900000000", "HT-002")
        ));

        Assert.assertEquals("每一行合同都应新增一条合同记录", 2, service.savedContracts.size());
        Assert.assertEquals(Long.valueOf(10001L), service.savedContracts.get(0).getEmployeeId());
        Assert.assertEquals(Long.valueOf(10001L), service.savedContracts.get(1).getEmployeeId());
        Assert.assertEquals("HT-001", service.savedContracts.get(0).getContractNum());
        Assert.assertEquals("HT-002", service.savedContracts.get(1).getContractNum());
        Assert.assertEquals("固定期限劳动合同应按中文枚举导入", Integer.valueOf(1), service.savedContracts.get(0).getContractType());
        Assert.assertEquals("执行中应按中文枚举导入", Integer.valueOf(1), service.savedContracts.get(0).getStatus());
        Assert.assertNull("导入合同必须走新增路径，不能使用旧主键覆盖", service.savedContracts.get(0).getContractId());
    }

    @Test
    public void importContracts_shouldAllowOpenEndedContractWithBlankEndDate() throws Exception {
        TestableContractService service = contractImportService(employee(10001L, "黎冬霜", "18872438626"));

        service.importContracts(contractImportFile(
                new Object[]{"黎冬霜", "18872438626", "HT-002", "无固定期限劳动合同", "2026-05-01", "", "执行中", "田野农谷", "2026-04-27", "备注"}
        ));

        Assert.assertEquals(1, service.savedContracts.size());
        HrmEmployeeContract savedContract = service.savedContracts.get(0);
        Assert.assertEquals("无固定期限劳动合同应按中文枚举导入", Integer.valueOf(2), savedContract.getContractType());
        Assert.assertNull("无固定期限劳动合同导入时允许合同结束日期为空", savedContract.getEndTime());
        Assert.assertNull("无固定期限劳动合同导入时不应保存固定期限年限", savedContract.getTerm());
    }

    @Test
    public void importContracts_shouldClearSubmittedEndDateForOpenEndedContract() throws Exception {
        TestableContractService service = contractImportService(employee(10001L, "黎冬霜", "18872438626"));

        service.importContracts(contractImportFile(
                new Object[]{"黎冬霜", "18872438626", "HT-002", "无固定期限劳动合同", "2026-05-01", "2099-12-31", "执行中", "田野农谷", "2026-04-27", "备注"}
        ));

        Assert.assertEquals(1, service.savedContracts.size());
        Assert.assertNull("无固定期限劳动合同导入时不应保留占位结束日期", service.savedContracts.get(0).getEndTime());
        Assert.assertNull("无固定期限劳动合同导入时不应保存固定期限年限", service.savedContracts.get(0).getTerm());
    }

    @Test
    public void importContracts_shouldStillRequireEndDateForFixedTermContract() throws Exception {
        TestableContractService service = contractImportService(employee(10001L, "黎冬霜", "18872438626"));

        try {
            service.importContracts(contractImportFile(
                    new Object[]{"黎冬霜", "18872438626", "HT-003", "固定期限劳动合同", "2026-05-01", "", "执行中", "田野农谷", "2026-04-27", "备注"}
            ));
            Assert.fail("固定期限劳动合同合同结束日期仍应必填");
        } catch (CrmException exception) {
            Assert.assertEquals("第2行合同结束日期不能为空", exception.getMsg());
        }
    }

    @Test
    public void importContracts_shouldReportInvalidRequiredDateWithColumnNameAndOriginalValue() throws Exception {
        TestableContractService service = contractImportService(employee(10001L, "黎冬霜", "18872438626"));

        try {
            service.importContracts(contractImportFileAtDisplayRow(40,
                    new Object[]{"黎冬霜", "18872438626", "HT-040", "固定期限劳动合同", "2026-05-01", "2027-4-31", "执行中", "田野农谷", "2026-04-27", "备注"}
            ));
            Assert.fail("无效合同结束日期应提示具体列名和原始值");
        } catch (CrmException exception) {
            Assert.assertEquals("第40行合同结束日期无效：2027-4-31", exception.getMsg());
        }
    }

    @Test
    public void importContracts_shouldReportInvalidEightDigitRequiredDateWithColumnNameAndOriginalValue() throws Exception {
        TestableContractService service = contractImportService(employee(10001L, "黎冬霜", "18872438626"));

        try {
            service.importContracts(contractImportFileAtDisplayRow(40,
                    new Object[]{"黎冬霜", "18872438626", "HT-040", "固定期限劳动合同", "2026-05-01", "20270431", "执行中", "田野农谷", "2026-04-27", "备注"}
            ));
            Assert.fail("8位无效合同结束日期应提示具体列名和原始值");
        } catch (CrmException exception) {
            Assert.assertEquals("第40行合同结束日期无效：20270431", exception.getMsg());
        }
    }

    private static class TestableContractService extends HrmEmployeeContractServiceImpl {

        private HrmEmployeeContract savedContract;
        private final List<HrmEmployee> employees = new ArrayList<>();
        private final List<HrmEmployeeContract> savedContracts = new ArrayList<>();

        @Override
        public boolean saveOrUpdate(HrmEmployeeContract entity) {
            this.savedContract = entity;
            this.savedContracts.add(entity);
            return true;
        }

        @Override
        protected List<HrmEmployee> listContractImportCandidates() {
            return employees;
        }
    }

    private TestableContractService contractImportService(HrmEmployee... employees) {
        TestableContractService service = new TestableContractService();
        ReflectionTestUtils.setField(service, "employeeActionRecordService", Mockito.mock(EmployeeActionRecordServiceImpl.class));
        service.employees.addAll(Arrays.asList(employees));
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

    private Object[] contractRow(String employeeName, String phone, String contractNum) {
        return new Object[]{employeeName, phone, contractNum, "固定期限劳动合同", "2025-06-16 00:00:00", "2026-06-16 00:00:00", "执行中", "田野农谷", "2025-06-16 00:00:00", "备注"};
    }

    private MultipartFile contractImportFile(Object[]... rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row header = sheet.createRow(0);
        String[] headers = {"姓名", "电话", "合同编号", "合同类型", "合同开始日期", "合同结束日期", "合同状态", "签约公司", "合同签订日期", "合同备注"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            Object[] values = rows[rowIndex];
            for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
                row.createCell(columnIndex).setCellValue(String.valueOf(values[columnIndex]));
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "contracts.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MultipartFile contractImportFileAtDisplayRow(int displayRow, Object[] values) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row header = sheet.createRow(0);
        String[] headers = {"姓名", "电话", "合同编号", "合同类型", "合同开始日期", "合同结束日期", "合同状态", "签约公司", "合同签订日期", "合同备注"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        Row row = sheet.createRow(displayRow - 1);
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            row.createCell(columnIndex).setCellValue(String.valueOf(values[columnIndex]));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "contracts.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }
}
