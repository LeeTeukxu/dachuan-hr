package com.tianye.hrsystem.imple.employee;

import com.alibaba.excel.EasyExcel;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.common.AllEmployeeListener;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.po.HrmEmployeeData;
import com.tianye.hrsystem.entity.po.HrmEmployeeField;
import com.tianye.hrsystem.entity.vo.EmployeeImportVO;
import com.tianye.hrsystem.enums.FieldTypeEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.service.employee.IHrmEmployeeDataService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeFieldService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmEmployeeServiceImplImportEmployeeTest {

    @Test
    public void employeeImportVO_shouldReadJobNumberColumn() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("员工导入");
        sheet.createRow(0).createCell(0).setCellValue("员工信息");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("入职状态");
        header.createCell(1).setCellValue("员工类别");
        header.createCell(2).setCellValue("员工姓名");
        header.createCell(23).setCellValue("工号");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("在职");
        row.createCell(1).setCellValue("正式工");
        row.createCell(2).setCellValue("张三");
        row.createCell(23).setCellValue("NG-0001");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        AllEmployeeListener listener = new AllEmployeeListener();
        EasyExcel.read(new ByteArrayInputStream(outputStream.toByteArray()), EmployeeImportVO.class, listener)
                .headRowNumber(2)
                .sheet()
                .doRead();

        Assert.assertEquals("NG-0001", listener.getItems().get(0).getJobNumber());
    }

    @Test
    public void importEmployee_shouldUseRosterJobNumberForNewEmployee() throws Exception {
        ImportTestService service = new ImportTestService();

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.emptyList());
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFileWithJobNumber("孙七", "NG-0001", "13955556666"));

        Assert.assertEquals(1, service.saved.size());
        Assert.assertEquals("孙七", service.saved.get(0).getEmployeeName());
        Assert.assertEquals("NG-0001", service.saved.get(0).getJobNumber());
    }

    @Test
    public void importEmployee_shouldUseOnlyVisibleSheetWhenRosterSheetNameMissing() throws Exception {
        ImportTestService service = new ImportTestService();

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.emptyList());
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(singleVisibleSheetRosterFile("未知员工", "TYNG-452", "17389819211"));

        Assert.assertEquals("仅有一个可见工作表时，即使名称不是田野农谷也应导入", 1, service.saved.size());
        Assert.assertEquals("未知员工", service.saved.get(0).getEmployeeName());
        Assert.assertEquals("TYNG-452", service.saved.get(0).getJobNumber());
        Assert.assertEquals("17389819211", service.saved.get(0).getMobile());
    }

    @Test
    public void importEmployee_shouldUseActiveVisibleSheetInsteadOfNamedRosterSheet() throws Exception {
        ImportTestService service = new ImportTestService();

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.emptyList());
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(activeSheetRosterFile());

        Assert.assertEquals("应按当前激活工作表导入，而不是固定读取田野农谷", 1, service.saved.size());
        Assert.assertEquals("当前员工", service.saved.get(0).getEmployeeName());
        Assert.assertEquals("CUR-001", service.saved.get(0).getJobNumber());
        Assert.assertEquals("17700000000", service.saved.get(0).getMobile());
    }

    @Test
    public void importEmployee_shouldCreateMissingRosterFieldAndMatchByNameAndPersonalPhone() throws Exception {
        ImportTestService service = new ImportTestService();
        HrmEmployee sameNameDifferentPhone = employee(1L, "张三", "13800000000");
        service.employees.add(sameNameDifferentPhone);

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(new ArrayList<>());
        when(employeeFieldService.saveBatch(any(Collection.class))).thenAnswer(invocation -> {
            Collection<HrmEmployeeField> fields = invocation.getArgument(0);
            long nextId = 9001L;
            for (HrmEmployeeField field : fields) {
                field.setFieldId(nextId++);
            }
            return true;
        });
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFile(row("张三", "13900000000", "湖北田野农谷生物科技有限公司")));

        Assert.assertEquals("同名但手机号不同应新建员工，不能只按姓名覆盖", 1, service.saved.size());
        Assert.assertEquals("张三", service.saved.get(0).getEmployeeName());
        Assert.assertEquals("13900000000", service.saved.get(0).getMobile());
        Assert.assertTrue("同名不同手机号不能更新原员工", service.updated.isEmpty());

        ArgumentCaptor<Collection> fieldCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeFieldService).saveBatch(fieldCaptor.capture());
        HrmEmployeeField createdField = (HrmEmployeeField) fieldCaptor.getValue().iterator().next();
        Assert.assertEquals("法人", createdField.getName());
        Assert.assertEquals("新增花名册字段应为可导入字段", Integer.valueOf(1), createdField.getIsImportField());

        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        HrmEmployeeData savedData = (HrmEmployeeData) dataCaptor.getValue().iterator().next();
        Assert.assertEquals(service.saved.get(0).getEmployeeId(), savedData.getEmployeeId());
        Assert.assertEquals("法人", savedData.getName());
        Assert.assertEquals("湖北田野农谷生物科技有限公司", savedData.getFieldValue());
    }

    @Test
    public void importEmployee_shouldUpdateOnlyWhenNameAndPersonalPhoneBothMatch() throws Exception {
        ImportTestService service = new ImportTestService();
        HrmEmployee existing = employee(8L, "李四", "13911112222");
        existing.setJobNumber("LS-001");
        existing.setIdNumber("420821198802091515");
        service.employees.add(existing);

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.emptyList());
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFile(row("李四", "13911112222", "湖北田野农谷生物科技有限公司")));

        Assert.assertTrue("姓名+手机号匹配时不应重复新建员工", service.saved.isEmpty());
        Assert.assertEquals(1, service.updated.size());
        Assert.assertEquals(Long.valueOf(8L), service.updated.get(0).getEmployeeId());
        Assert.assertEquals("13911112222", service.updated.get(0).getMobile());
    }

    @Test
    public void importEmployee_shouldUpdateSameNameEmployeeWhenIdNumberMatchesAndPhoneChanged() throws Exception {
        ImportTestService service = new ImportTestService();
        HrmEmployee existing = employee(18L, "杨磊", "13911112222");
        existing.setJobNumber("YL-001");
        existing.setIdNumber("420821198802091515");
        service.employees.add(existing);

        configureRosterImportDependencies(service);

        service.importEmployee(rosterFileWithUniqueRows(new String[][]{
                {"杨磊", "YL-001", "13999998888", "420821198802091515"}
        }));

        Assert.assertTrue("姓名+身份证号匹配时应更新原员工，不能因手机号变化新建", service.saved.isEmpty());
        Assert.assertEquals(1, service.updated.size());
        Assert.assertEquals(Long.valueOf(18L), service.updated.get(0).getEmployeeId());
        Assert.assertEquals("13999998888", service.updated.get(0).getMobile());
    }

    @Test
    public void importEmployee_shouldRejectDuplicateJobNumberMobileAndIdNumberInFile() throws Exception {
        ImportTestService service = new ImportTestService();
        configureRosterImportDependencies(service);

        assertImportDuplicate(service, rosterFileWithUniqueRows(new String[][]{
                {"张三", "DUP-001", "13900000000", "420821198802091515"},
                {"李四", "DUP-001", "13900000001", "420821198802091516"}
        }), "工号");

        service = new ImportTestService();
        configureRosterImportDependencies(service);
        assertImportDuplicate(service, rosterFileWithUniqueRows(new String[][]{
                {"张三", "DUP-001", "13900000000", "420821198802091515"},
                {"李四", "DUP-002", "13900000000", "420821198802091516"}
        }), "手机号");

        service = new ImportTestService();
        configureRosterImportDependencies(service);
        assertImportDuplicate(service, rosterFileWithUniqueRows(new String[][]{
                {"张三", "DUP-001", "13900000000", "420821198802091515"},
                {"李四", "DUP-002", "13900000001", "420821198802091515"}
        }), "身份证号");
    }

    @Test
    public void importEmployee_shouldRejectDuplicateUniqueFieldsFromExistingEmployees() throws Exception {
        ImportTestService service = new ImportTestService();
        configureRosterImportDependencies(service);
        HrmEmployee existing = employee(9L, "已有员工", "13900000000");
        existing.setJobNumber("EXIST-001");
        existing.setIdNumber("420821198802091515");
        service.employees.add(existing);

        assertImportDuplicate(service, rosterFileWithUniqueRows(new String[][]{
                {"新员工", "EXIST-001", "13900000001", "420821198802091516"}
        }), "工号");

        service = new ImportTestService();
        configureRosterImportDependencies(service);
        existing = employee(9L, "已有员工", "13900000000");
        existing.setJobNumber("EXIST-001");
        existing.setIdNumber("420821198802091515");
        service.employees.add(existing);
        assertImportDuplicate(service, rosterFileWithUniqueRows(new String[][]{
                {"新员工", "EXIST-002", "13900000000", "420821198802091516"}
        }), "手机号");

        service = new ImportTestService();
        configureRosterImportDependencies(service);
        existing = employee(9L, "已有员工", "13900000000");
        existing.setJobNumber("EXIST-001");
        existing.setIdNumber("420821198802091515");
        service.employees.add(existing);
        assertImportDuplicate(service, rosterFileWithUniqueRows(new String[][]{
                {"新员工", "EXIST-002", "13900000001", "420821198802091515"}
        }), "身份证号");
    }

    @Test
    public void importEmployee_shouldReuseSameNameFieldFromAnyEmployeeTab() throws Exception {
        ImportTestService service = new ImportTestService();

        HrmEmployeeField existingLegalPersonField = new HrmEmployeeField();
        existingLegalPersonField.setFieldId(7001L);
        existingLegalPersonField.setName("法人");
        existingLegalPersonField.setFieldName("existing_legal_person");
        existingLegalPersonField.setLabelGroup(LabelGroupEnum.POST.getValue());

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.singletonList(existingLegalPersonField));
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFile(row("王五", "13922223333", "湖北田野农谷生物科技有限公司")));

        verify(employeeFieldService, org.mockito.Mockito.never()).saveBatch(any(Collection.class));
        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        HrmEmployeeData savedData = (HrmEmployeeData) dataCaptor.getValue().iterator().next();
        Assert.assertEquals("已有任意选项卡同名字段时，应复用原字段", Long.valueOf(7001L), savedData.getFieldId());
    }

    @Test
    public void importEmployee_shouldKeepRepeatedHeadersByGroup() throws Exception {
        ImportTestService service = new ImportTestService();

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(new ArrayList<>());
        when(employeeFieldService.saveBatch(any(Collection.class))).thenAnswer(invocation -> {
            Collection<HrmEmployeeField> fields = invocation.getArgument(0);
            long nextId = 8001L;
            for (HrmEmployeeField field : fields) {
                field.setFieldId(nextId++);
            }
            return true;
        });
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(repeatedHeaderRosterFile());

        Assert.assertEquals("赵六", service.saved.get(0).getEmployeeName());
        ArgumentCaptor<Collection> fieldCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeFieldService).saveBatch(fieldCaptor.capture());
        List<String> createdNames = new ArrayList<>();
        for (Object field : fieldCaptor.getValue()) {
            createdNames.add(((HrmEmployeeField) field).getName());
        }
        Assert.assertTrue("重复表头字段名应带分组避免歧义", createdNames.contains("家庭状况-年龄"));
    }

    @Test
    public void importEmployee_shouldImportFixedPerformanceAsTwoDecimalNumber() throws Exception {
        ImportTestService service = new ImportTestService();

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(new ArrayList<>());
        when(employeeFieldService.saveBatch(any(Collection.class))).thenAnswer(invocation -> {
            Collection<HrmEmployeeField> fields = invocation.getArgument(0);
            long nextId = 8101L;
            for (HrmEmployeeField field : fields) {
                field.setFieldId(nextId++);
            }
            return true;
        });
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFileWithFixedPerformance());

        ArgumentCaptor<Collection> fieldCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeFieldService).saveBatch(fieldCaptor.capture());
        HrmEmployeeField fixedPerformanceField = null;
        for (Object item : fieldCaptor.getValue()) {
            HrmEmployeeField field = (HrmEmployeeField) item;
            if ("固定绩效".equals(field.getName())) {
                fixedPerformanceField = field;
            }
        }
        Assert.assertNotNull(fixedPerformanceField);
        Assert.assertEquals("固定绩效应创建为小数字段", Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fixedPerformanceField.getType());
        Assert.assertEquals("固定绩效应限制为两位小数", Integer.valueOf(2), fixedPerformanceField.getPrecisions());

        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        HrmEmployeeData fixedPerformanceData = null;
        for (Object item : dataCaptor.getValue()) {
            HrmEmployeeData data = (HrmEmployeeData) item;
            if ("固定绩效".equals(data.getName())) {
                fixedPerformanceData = data;
            }
        }
        Assert.assertNotNull(fixedPerformanceData);
        Assert.assertEquals("1234.50", fixedPerformanceData.getFieldValue());
        Assert.assertEquals("1234.50", fixedPerformanceData.getFieldValueDesc());
    }

    @Test
    public void importEmployee_shouldImportDutySubsidyAsTwoDecimalNumber() throws Exception {
        ImportTestService service = new ImportTestService();

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(new ArrayList<>());
        when(employeeFieldService.saveBatch(any(Collection.class))).thenAnswer(invocation -> {
            Collection<HrmEmployeeField> fields = invocation.getArgument(0);
            long nextId = 8201L;
            for (HrmEmployeeField field : fields) {
                field.setFieldId(nextId++);
            }
            return true;
        });
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFileWithSalaryAmountColumns());

        ArgumentCaptor<Collection> fieldCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeFieldService).saveBatch(fieldCaptor.capture());
        HrmEmployeeField dutySubsidyField = null;
        HrmEmployeeField otherSubsidyField = null;
        for (Object item : fieldCaptor.getValue()) {
            HrmEmployeeField field = (HrmEmployeeField) item;
            if ("职务补助".equals(field.getName())) {
                dutySubsidyField = field;
            } else if ("其他补助".equals(field.getName())) {
                otherSubsidyField = field;
            }
        }
        Assert.assertNotNull(dutySubsidyField);
        Assert.assertEquals("职务补助应创建为小数字段", Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), dutySubsidyField.getType());
        Assert.assertEquals("职务补助应限制为两位小数", Integer.valueOf(2), dutySubsidyField.getPrecisions());
        Assert.assertNotNull(otherSubsidyField);
        Assert.assertEquals("其他补助应创建为小数字段", Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), otherSubsidyField.getType());
        Assert.assertEquals("其他补助应限制为两位小数", Integer.valueOf(2), otherSubsidyField.getPrecisions());

        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        HrmEmployeeData dutySubsidyData = null;
        HrmEmployeeData otherSubsidyData = null;
        for (Object item : dataCaptor.getValue()) {
            HrmEmployeeData data = (HrmEmployeeData) item;
            if ("职务补助".equals(data.getName())) {
                dutySubsidyData = data;
            } else if ("其他补助".equals(data.getName())) {
                otherSubsidyData = data;
            }
        }
        Assert.assertNotNull(dutySubsidyData);
        Assert.assertEquals("234.60", dutySubsidyData.getFieldValue());
        Assert.assertEquals("234.60", dutySubsidyData.getFieldValueDesc());
        Assert.assertNotNull(otherSubsidyData);
        Assert.assertEquals("345.70", otherSubsidyData.getFieldValue());
        Assert.assertEquals("345.70", otherSubsidyData.getFieldValueDesc());
    }

    @Test
    public void importEmployee_shouldCorrectExistingFixedPerformanceFieldToTwoDecimalNumber() throws Exception {
        ImportTestService service = new ImportTestService();

        HrmEmployeeField existingFixedPerformanceField = new HrmEmployeeField();
        existingFixedPerformanceField.setFieldId(9101L);
        existingFixedPerformanceField.setName("固定绩效");
        existingFixedPerformanceField.setFieldName("existing_fixed_performance");
        existingFixedPerformanceField.setLabelGroup(LabelGroupEnum.SOCIAL_SECURITY.getValue());
        existingFixedPerformanceField.setType(FieldTypeEnum.TEXT.getValue());

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.singletonList(existingFixedPerformanceField));
        when(employeeFieldService.updateById(any(HrmEmployeeField.class))).thenReturn(true);
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        service.importEmployee(rosterFileWithFixedPerformance());

        ArgumentCaptor<HrmEmployeeField> fieldCaptor = ArgumentCaptor.forClass(HrmEmployeeField.class);
        verify(employeeFieldService).updateById(fieldCaptor.capture());
        Assert.assertEquals(Long.valueOf(9101L), fieldCaptor.getValue().getFieldId());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fieldCaptor.getValue().getType());
        Assert.assertEquals(Integer.valueOf(2), fieldCaptor.getValue().getPrecisions());
    }

    private HrmEmployee employee(Long employeeId, String name, String mobile) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(name);
        employee.setMobile(mobile);
        employee.setIsDel(0);
        return employee;
    }

    private String[] row(String name, String phone, String legalPerson) {
        return new String[]{name, phone, legalPerson};
    }

    private MultipartFile rosterFile(String[] dataRow) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet ignored = workbook.createSheet("田野源味");
        ignored.createRow(1).createCell(0).setCellValue("姓名");
        Sheet sheet = workbook.createSheet("田野农谷");
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        group.createCell(3).setCellValue("基本信息");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("个人电话");
        header.createCell(2).setCellValue("法人");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue(dataRow[0]);
        row.createCell(1).setCellValue(dataRow[1]);
        row.createCell(2).setCellValue(dataRow[2]);
        workbook.setActiveSheet(1);
        workbook.setSelectedTab(1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MultipartFile rosterFileWithJobNumber(String name, String jobNumber, String phone) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("田野农谷");
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("工号");
        header.createCell(2).setCellValue("个人电话");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(jobNumber);
        row.createCell(2).setCellValue(phone);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MultipartFile rosterFileWithUniqueRows(String[][] rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("田野农谷");
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("工号");
        header.createCell(2).setCellValue("个人电话");
        header.createCell(3).setCellValue("身份证号");
        for (int index = 0; index < rows.length; index++) {
            Row row = sheet.createRow(index + 2);
            row.createCell(0).setCellValue(rows[index][0]);
            row.createCell(1).setCellValue(rows[index][1]);
            row.createCell(2).setCellValue(rows[index][2]);
            row.createCell(3).setCellValue(rows[index][3]);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private void assertImportDuplicate(ImportTestService service, MultipartFile file, String messagePart) throws Exception {
        try {
            service.importEmployee(file);
            Assert.fail("Expected duplicate " + messagePart + " to be rejected");
        } catch (CrmException ex) {
            Assert.assertTrue("错误文案应包含：" + messagePart + "，实际：" + ex.getMsg(), ex.getMsg().contains(messagePart));
        }
        Assert.assertTrue("导入唯一性失败时不应保存前序员工", service.saved.isEmpty());
        Assert.assertTrue("导入唯一性失败时不应更新前序员工", service.updated.isEmpty());
    }

    private MultipartFile singleVisibleSheetRosterFile(String name, String jobNumber, String phone) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row group = sheet.createRow(0);
        group.createCell(1).setCellValue("基本信息");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("序号");
        header.createCell(1).setCellValue("工号");
        header.createCell(2).setCellValue("姓名");
        header.createCell(7).setCellValue("入职日期");
        header.createCell(8).setCellValue("个人电话");
        header.createCell(9).setCellValue("身份证号");
        header.createCell(14).setCellValue("出生日期");
        header.createCell(15).setCellValue("年龄");
        header.createCell(16).setCellValue("工龄");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("1");
        row.createCell(1).setCellValue(jobNumber);
        row.createCell(2).setCellValue(name);
        row.createCell(7).setCellValue("2025-04-01 00:00:00");
        row.createCell(8).setCellValue(phone);
        row.createCell(9).setCellValue("420821198802091515");
        row.createCell(14).setCellFormula("TEXT(MID(J3,7,8),\"0-00-00\")");
        row.createCell(15).setCellFormula("YEAR(TODAY())-MID(J3,7,4)");
        row.createCell(16).setCellFormula("IF(H3=\"\",\"\",DATEDIF(H3,TODAY(),\"Y\"))");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MultipartFile activeSheetRosterFile() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet namedSheet = workbook.createSheet("田野农谷");
        fillSimpleRosterSheet(namedSheet, "旧员工", "OLD-001", "16600000000");
        Sheet activeSheet = workbook.createSheet("Sheet1");
        fillSimpleRosterSheet(activeSheet, "当前员工", "CUR-001", "17700000000");
        workbook.setActiveSheet(1);
        workbook.setSelectedTab(1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private void fillSimpleRosterSheet(Sheet sheet, String name, String jobNumber, String phone) {
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("工号");
        header.createCell(2).setCellValue("个人电话");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(jobNumber);
        row.createCell(2).setCellValue(phone);
    }

    private MultipartFile repeatedHeaderRosterFile() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("田野农谷");
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        group.createCell(3).setCellValue("家庭状况");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("个人电话");
        header.createCell(2).setCellValue("年龄");
        header.createCell(3).setCellValue("法人");
        header.createCell(4).setCellValue("年龄");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("赵六");
        row.createCell(1).setCellValue("13933334444");
        row.createCell(2).setCellValue("30");
        row.createCell(3).setCellValue("湖北田野农谷生物科技有限公司");
        row.createCell(4).setCellValue("60");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MultipartFile rosterFileWithFixedPerformance() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("田野农谷");
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        group.createCell(3).setCellValue("薪酬福利");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("个人电话");
        header.createCell(2).setCellValue("职位");
        header.createCell(3).setCellValue("固定绩效");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("钱七");
        row.createCell(1).setCellValue("13944445555");
        row.createCell(2).setCellValue("主管");
        row.createCell(3).setCellValue(1234.5);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MultipartFile rosterFileWithSalaryAmountColumns() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("田野农谷");
        Row group = sheet.createRow(0);
        group.createCell(0).setCellValue("基本信息");
        group.createCell(3).setCellValue("薪酬福利");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        header.createCell(1).setCellValue("个人电话");
        header.createCell(2).setCellValue("职位");
        header.createCell(3).setCellValue("固定绩效");
        header.createCell(4).setCellValue("职务补助");
        header.createCell(5).setCellValue("其他补助");
        Row row = sheet.createRow(2);
        row.createCell(0).setCellValue("孙八");
        row.createCell(1).setCellValue("13955556666");
        row.createCell(2).setCellValue("主管");
        row.createCell(3).setCellValue(1234.5);
        row.createCell(4).setCellValue(234.6);
        row.createCell(5).setCellValue(345.7);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "roster.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = HrmEmployeeServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void configureRosterImportDependencies(ImportTestService service) throws Exception {
        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Collections.emptyList());
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);
    }

    private static class ImportTestService extends HrmEmployeeServiceImpl {
        private final List<HrmEmployee> employees = new ArrayList<>();
        private final List<HrmEmployee> saved = new ArrayList<>();
        private final List<HrmEmployee> updated = new ArrayList<>();
        private long nextEmployeeId = 1000L;

        @Override
        public List<HrmEmployee> list() {
            return employees;
        }

        @Override
        public boolean save(HrmEmployee entity) {
            if (entity.getEmployeeId() == null) {
                entity.setEmployeeId(nextEmployeeId++);
            }
            saved.add(entity);
            employees.add(entity);
            return true;
        }

        @Override
        public boolean updateById(HrmEmployee entity) {
            updated.add(entity);
            return true;
        }
    }
}
