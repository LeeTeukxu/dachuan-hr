package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.bo.AddEmployeeBO;
import com.tianye.hrsystem.entity.bo.AddEmployeeFieldManageBO;
import com.tianye.hrsystem.entity.bo.UpdateInformationBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeData;
import com.tianye.hrsystem.entity.po.HrmEmployeeField;
import com.tianye.hrsystem.entity.po.HrmEmployeeChangeRecord;
import com.tianye.hrsystem.entity.vo.Content;
import com.tianye.hrsystem.enums.BehaviorEnum;
import com.tianye.hrsystem.enums.FieldTypeEnum;
import com.tianye.hrsystem.enums.HrmActionBehaviorEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.service.employee.IHrmEmployeeAbnormalChangeRecordService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeChangeRecordService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeDataService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeFieldService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmEmployeeServiceImplSalaryFieldsTest {

    @Test
    public void addEmployeeBO_shouldAcceptSalaryLevelAndFixedPerformance() {
        AddEmployeeBO request = new AddEmployeeBO();
        request.setSalaryLevel("P3");
        request.setFixedPerformance(new BigDecimal("1234.50"));
        request.setDutySubsidy(new BigDecimal("345.60"));

        Assert.assertEquals("P3", request.getSalaryLevel());
        Assert.assertEquals(new BigDecimal("1234.50"), request.getFixedPerformance());
        Assert.assertEquals(new BigDecimal("345.60"), request.getDutySubsidy());
    }

    @Test
    public void addEmployeeFieldManageBO_shouldAcceptSalaryLevelAndFixedPerformanceForAgainOnboarding() {
        AddEmployeeFieldManageBO request = new AddEmployeeFieldManageBO();
        request.setSalaryLevel("P4");
        request.setFixedPerformance(new BigDecimal("2345.60"));
        request.setDutySubsidy(new BigDecimal("456.70"));

        Assert.assertEquals("P4", request.getSalaryLevel());
        Assert.assertEquals(new BigDecimal("2345.60"), request.getFixedPerformance());
        Assert.assertEquals(new BigDecimal("456.70"), request.getDutySubsidy());
    }

    @Test
    public void hrmEmployeeChangeRecord_shouldAcceptSalaryLevelAndFixedPerformanceForEmployeeEditActions() {
        HrmEmployeeChangeRecord request = new HrmEmployeeChangeRecord();
        request.setSalaryLevel("P5");
        request.setFixedPerformance(new BigDecimal("3456.70"));
        request.setDutySubsidy(new BigDecimal("567.80"));

        Assert.assertEquals("P5", request.getSalaryLevel());
        Assert.assertEquals(new BigDecimal("3456.70"), request.getFixedPerformance());
        Assert.assertEquals(new BigDecimal("567.80"), request.getDutySubsidy());
    }

    @Test
    public void ensureEmployeeSalaryDynamicFields_shouldCreatePersonalTextAndDecimalFields() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Arrays.asList());
        when(employeeFieldService.saveBatch(any(Collection.class))).thenAnswer(invocation -> {
            long id = 8001L;
            for (HrmEmployeeField field : (Collection<HrmEmployeeField>) invocation.getArgument(0)) {
                field.setFieldId(id++);
            }
            return true;
        });
        setField(service, "employeeFieldService", employeeFieldService);

        Map<String, HrmEmployeeField> fields = invokeEnsureSalaryFields(service);

        HrmEmployeeField salaryLevel = fields.get("薪资等级");
        HrmEmployeeField fixedPerformance = fields.get("固定绩效");
        HrmEmployeeField dutySubsidy = fields.get("职务补助");
        HrmEmployeeField otherSubsidy = fields.get("其他补助");
        Assert.assertNotNull(salaryLevel);
        Assert.assertNotNull(fixedPerformance);
        Assert.assertNotNull(dutySubsidy);
        Assert.assertNotNull(otherSubsidy);
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), salaryLevel.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.TEXT.getValue()), salaryLevel.getType());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), fixedPerformance.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fixedPerformance.getType());
        Assert.assertEquals(Integer.valueOf(2), fixedPerformance.getPrecisions());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), dutySubsidy.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), dutySubsidy.getType());
        Assert.assertEquals(Integer.valueOf(2), dutySubsidy.getPrecisions());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), otherSubsidy.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), otherSubsidy.getType());
        Assert.assertEquals(Integer.valueOf(2), otherSubsidy.getPrecisions());
    }

    @Test
    public void ensureEmployeeSalaryDynamicFields_shouldReuseAndCorrectHistoricalFixedPerformanceField() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        HrmEmployeeField salaryLevel = field(7001L, "legacy_salary_level", "薪资等级", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.SOCIAL_SECURITY.getValue(), null);
        HrmEmployeeField fixedPerformance = field(7002L, "legacy_fixed_performance", "固定绩效", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.SOCIAL_SECURITY.getValue(), null);
        HrmEmployeeField dutySubsidy = field(7003L, "legacy_duty_subsidy", "职务补助", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.SOCIAL_SECURITY.getValue(), null);
        HrmEmployeeField otherSubsidy = field(7004L, "legacy_other_subsidy", "其他补助", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.SOCIAL_SECURITY.getValue(), null);
        fixedPerformance.setIsHidden(1);
        fixedPerformance.setIsUpdateValue(0);
        dutySubsidy.setIsHidden(1);
        dutySubsidy.setIsUpdateValue(0);
        otherSubsidy.setIsHidden(1);
        otherSubsidy.setIsUpdateValue(0);

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Arrays.asList(salaryLevel, fixedPerformance, dutySubsidy, otherSubsidy));
        when(employeeFieldService.updateById(any(HrmEmployeeField.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);

        Map<String, HrmEmployeeField> fields = invokeEnsureSalaryFields(service);

        Assert.assertSame(salaryLevel, fields.get("薪资等级"));
        Assert.assertSame(fixedPerformance, fields.get("固定绩效"));
        Assert.assertSame(dutySubsidy, fields.get("职务补助"));
        Assert.assertSame(otherSubsidy, fields.get("其他补助"));
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), salaryLevel.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), fixedPerformance.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fixedPerformance.getType());
        Assert.assertEquals(Integer.valueOf(2), fixedPerformance.getPrecisions());
        Assert.assertEquals(Integer.valueOf(0), fixedPerformance.getIsHidden());
        Assert.assertEquals(Integer.valueOf(1), fixedPerformance.getIsUpdateValue());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), dutySubsidy.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), dutySubsidy.getType());
        Assert.assertEquals(Integer.valueOf(2), dutySubsidy.getPrecisions());
        Assert.assertEquals(Integer.valueOf(0), dutySubsidy.getIsHidden());
        Assert.assertEquals(Integer.valueOf(1), dutySubsidy.getIsUpdateValue());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), otherSubsidy.getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), otherSubsidy.getType());
        Assert.assertEquals(Integer.valueOf(2), otherSubsidy.getPrecisions());
        Assert.assertEquals(Integer.valueOf(0), otherSubsidy.getIsHidden());
        Assert.assertEquals(Integer.valueOf(1), otherSubsidy.getIsUpdateValue());
    }

    @Test
    public void saveEmployeeSalaryDynamicFields_shouldOverwriteOnlySalaryFieldsWithTwoDecimalPerformance() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        HrmEmployeeField salaryLevel = field(7101L, "salary_level", "薪资等级", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.PERSONAL.getValue(), null);
        HrmEmployeeField fixedPerformance = field(7102L, "fixed_performance", "固定绩效", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        HrmEmployeeField dutySubsidy = field(7103L, "duty_subsidy", "职务补助", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        HrmEmployeeField otherSubsidy = field(7104L, "other_subsidy", "其他补助", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Arrays.asList(salaryLevel, fixedPerformance, dutySubsidy, otherSubsidy));
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        invokeSaveSalaryFields(service, 101L, " P3 ", new BigDecimal("1234.5"), new BigDecimal("88.8"), new BigDecimal("66.6"));

        verify(employeeDataService).remove(any());
        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        List<HrmEmployeeData> savedData = (List<HrmEmployeeData>) dataCaptor.getValue();
        Assert.assertEquals(4, savedData.size());
        HrmEmployeeData savedSalaryLevel = findData(savedData, "薪资等级");
        HrmEmployeeData savedFixedPerformance = findData(savedData, "固定绩效");
        HrmEmployeeData savedDutySubsidy = findData(savedData, "职务补助");
        HrmEmployeeData savedOtherSubsidy = findData(savedData, "其他补助");
        Assert.assertEquals(Long.valueOf(101L), savedSalaryLevel.getEmployeeId());
        Assert.assertEquals("P3", savedSalaryLevel.getFieldValue());
        Assert.assertEquals("P3", savedSalaryLevel.getFieldValueDesc());
        Assert.assertEquals("1234.50", savedFixedPerformance.getFieldValue());
        Assert.assertEquals("1234.50", savedFixedPerformance.getFieldValueDesc());
        Assert.assertEquals("88.80", savedDutySubsidy.getFieldValue());
        Assert.assertEquals("88.80", savedDutySubsidy.getFieldValueDesc());
        Assert.assertEquals("66.60", savedOtherSubsidy.getFieldValue());
        Assert.assertEquals("66.60", savedOtherSubsidy.getFieldValueDesc());
    }

    @Test
    public void saveEmployeeSalaryDynamicFields_shouldCreateMissingFieldsWithIdsBeforeSavingValues() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Arrays.asList());
        when(employeeFieldService.save(any(HrmEmployeeField.class))).thenAnswer(invocation -> {
            HrmEmployeeField field = invocation.getArgument(0);
            if ("薪资等级".equals(field.getName())) {
                field.setFieldId(7301L);
            } else if ("固定绩效".equals(field.getName())) {
                field.setFieldId(7302L);
            } else if ("职务补助".equals(field.getName())) {
                field.setFieldId(7303L);
            } else {
                field.setFieldId(7304L);
            }
            return true;
        });
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);

        invokeSaveSalaryFields(service, 102L, "P6", new BigDecimal("111.2"), new BigDecimal("222.3"), new BigDecimal("333.4"));

        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        List<HrmEmployeeData> savedData = (List<HrmEmployeeData>) dataCaptor.getValue();
        Assert.assertEquals(4, savedData.size());
        Assert.assertEquals(Long.valueOf(7301L), findData(savedData, "薪资等级").getFieldId());
        Assert.assertEquals(Long.valueOf(7302L), findData(savedData, "固定绩效").getFieldId());
        Assert.assertEquals(Long.valueOf(7303L), findData(savedData, "职务补助").getFieldId());
        Assert.assertEquals(Long.valueOf(7304L), findData(savedData, "其他补助").getFieldId());
    }

    @Test
    public void convertEmployeeDynamicFieldValue_shouldFormatFixedPerformanceWhenEditingPersonalInformation() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();

        String value = invokeConvertDynamicFieldValue(service, "固定绩效", "legacy_fixed_performance", FieldTypeEnum.DECIMAL.getValue(), "567.8");

        Assert.assertEquals("567.80", value);
    }

    @Test
    public void convertEmployeeDynamicFieldValue_shouldFormatDutySubsidyWhenEditingPersonalInformation() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();

        String value = invokeConvertDynamicFieldValue(service, "职务补助", "duty_subsidy", FieldTypeEnum.DECIMAL.getValue(), "66.6");

        Assert.assertEquals("66.60", value);
    }

    @Test
    public void convertEmployeeDynamicFieldValue_shouldFormatOtherSubsidyWhenEditingPersonalInformation() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();

        String value = invokeConvertDynamicFieldValue(service, "其他补助", "other_subsidy", FieldTypeEnum.DECIMAL.getValue(), "33.3");

        Assert.assertEquals("33.30", value);
    }

    @Test
    public void normalizeEmployeeSalaryInformationFields_shouldAttachDynamicFieldMetadataWhenEditFallbackFieldsHaveNoFieldId() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        HrmEmployeeField salaryLevel = field(7201L, "salary_level", "薪资等级", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.PERSONAL.getValue(), null);
        HrmEmployeeField fixedPerformance = field(7202L, "fixed_performance", "固定绩效", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        HrmEmployeeField dutySubsidy = field(7203L, "duty_subsidy", "职务补助", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        HrmEmployeeField otherSubsidy = field(7204L, "other_subsidy", "其他补助", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);

        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Arrays.asList(salaryLevel, fixedPerformance, dutySubsidy, otherSubsidy));
        setField(service, "employeeFieldService", employeeFieldService);

        List<UpdateInformationBO.InformationFieldBO> fields = Arrays.asList(
                updateField(null, "salary_level", "薪资等级", FieldTypeEnum.TEXT.getValue(), " P5 "),
                updateField(null, "fixed_performance", "固定绩效", FieldTypeEnum.DECIMAL.getValue(), "678.9"),
                updateField(null, "duty_subsidy", "职务补助", FieldTypeEnum.DECIMAL.getValue(), "77.7"),
                updateField(null, "other_subsidy", "其他补助", FieldTypeEnum.DECIMAL.getValue(), "88.8")
        );

        invokeNormalizeSalaryInformationFields(service, fields);

        Assert.assertEquals(Long.valueOf(7201L), fields.get(0).getFieldId());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), fields.get(0).getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.TEXT.getValue()), fields.get(0).getType());
        Assert.assertEquals(Long.valueOf(7202L), fields.get(1).getFieldId());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), fields.get(1).getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fields.get(1).getType());
        Assert.assertEquals(Long.valueOf(7203L), fields.get(2).getFieldId());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), fields.get(2).getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fields.get(2).getType());
        Assert.assertEquals(Long.valueOf(7204L), fields.get(3).getFieldId());
        Assert.assertEquals(Integer.valueOf(LabelGroupEnum.PERSONAL.getValue()), fields.get(3).getLabelGroup());
        Assert.assertEquals(Integer.valueOf(FieldTypeEnum.DECIMAL.getValue()), fields.get(3).getType());
    }

    @Test
    public void change_shouldSaveSalaryFieldsWhenEmployeeEditActionsSubmitSalaryInputs() throws Exception {
        HrmEmployeeServiceImpl service = spy(new HrmEmployeeServiceImpl());
        com.tianye.hrsystem.entity.po.HrmEmployee employee = new com.tianye.hrsystem.entity.po.HrmEmployee();
        employee.setEmployeeId(103L);
        employee.setEmployeeName("张三");
        employee.setDeptId(10L);
        employee.setParentId(20L);
        employee.setPost("专员");
        employee.setPostLevel("P4");
        employee.setWorkAddress("海口");
        doReturn(employee).when(service).getById(103L);

        HrmEmployeeField salaryLevel = field(7401L, "salary_level", "薪资等级", FieldTypeEnum.TEXT.getValue(), LabelGroupEnum.PERSONAL.getValue(), null);
        HrmEmployeeField fixedPerformance = field(7402L, "fixed_performance", "固定绩效", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        HrmEmployeeField dutySubsidy = field(7403L, "duty_subsidy", "职务补助", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        HrmEmployeeField otherSubsidy = field(7404L, "other_subsidy", "其他补助", FieldTypeEnum.DECIMAL.getValue(), LabelGroupEnum.PERSONAL.getValue(), 2);
        IHrmEmployeeFieldService employeeFieldService = mock(IHrmEmployeeFieldService.class);
        when(employeeFieldService.list()).thenReturn(Arrays.asList(salaryLevel, fixedPerformance, dutySubsidy, otherSubsidy));
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        when(employeeDataService.remove(any())).thenReturn(true);
        when(employeeDataService.saveBatch(any(Collection.class))).thenReturn(true);
        IHrmEmployeeChangeRecordService changeRecordService = mock(IHrmEmployeeChangeRecordService.class);
        when(changeRecordService.saveOrUpdate(any(HrmEmployeeChangeRecord.class))).thenReturn(true);
        IHrmEmployeeAbnormalChangeRecordService abnormalChangeRecordService = mock(IHrmEmployeeAbnormalChangeRecordService.class);
        EmployeeActionRecordServiceImpl employeeActionRecordService = mock(EmployeeActionRecordServiceImpl.class);
        when(employeeActionRecordService.changeRecord(any(HrmEmployeeChangeRecord.class))).thenReturn(new Content("张三", "调整部门/岗位", BehaviorEnum.UPDATE));
        setField(service, "employeeFieldService", employeeFieldService);
        setField(service, "employeeDataService", employeeDataService);
        setField(service, "changeRecordService", changeRecordService);
        setField(service, "abnormalChangeRecordService", abnormalChangeRecordService);
        setField(service, "employeeActionRecordService", employeeActionRecordService);

        HrmEmployeeChangeRecord request = new HrmEmployeeChangeRecord();
        request.setEmployeeId(103L);
        request.setChangeType(HrmActionBehaviorEnum.CHANGE_POST.getValue());
        request.setChangeReason(1);
        request.setNewDept(11L);
        request.setNewPost("主管");
        request.setNewPostLevel("P5");
        request.setEffectTime(LocalDate.now().plusDays(1));
        request.setSalaryLevel(" P5 ");
        request.setFixedPerformance(new BigDecimal("888.8"));
        request.setDutySubsidy(new BigDecimal("99.9"));
        request.setOtherSubsidy(new BigDecimal("66.6"));

        service.change(request);

        ArgumentCaptor<Collection> dataCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDataService).saveBatch(dataCaptor.capture());
        List<HrmEmployeeData> savedData = (List<HrmEmployeeData>) dataCaptor.getValue();
        Assert.assertEquals("P5", findData(savedData, "薪资等级").getFieldValue());
        Assert.assertEquals("888.80", findData(savedData, "固定绩效").getFieldValue());
        Assert.assertEquals("99.90", findData(savedData, "职务补助").getFieldValue());
        Assert.assertEquals("66.60", findData(savedData, "其他补助").getFieldValue());
    }

    private Map<String, HrmEmployeeField> invokeEnsureSalaryFields(HrmEmployeeServiceImpl service) throws Exception {
        Method method = HrmEmployeeServiceImpl.class.getDeclaredMethod("ensureEmployeeSalaryDynamicFields");
        method.setAccessible(true);
        return (Map<String, HrmEmployeeField>) method.invoke(service);
    }

    private void invokeSaveSalaryFields(HrmEmployeeServiceImpl service, Long employeeId, String salaryLevel, BigDecimal fixedPerformance, BigDecimal dutySubsidy, BigDecimal otherSubsidy) throws Exception {
        Method method = HrmEmployeeServiceImpl.class.getDeclaredMethod("saveEmployeeSalaryDynamicFields", Long.class, String.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);
        method.setAccessible(true);
        method.invoke(service, employeeId, salaryLevel, fixedPerformance, dutySubsidy, otherSubsidy);
    }

    private String invokeConvertDynamicFieldValue(HrmEmployeeServiceImpl service, String name, String fieldName, Integer type, Object fieldValue) throws Exception {
        Method method = HrmEmployeeServiceImpl.class.getDeclaredMethod("convertEmployeeDynamicFieldValue", String.class, String.class, Integer.class, Object.class);
        method.setAccessible(true);
        return (String) method.invoke(service, name, fieldName, type, fieldValue);
    }

    private void invokeNormalizeSalaryInformationFields(HrmEmployeeServiceImpl service, List<UpdateInformationBO.InformationFieldBO> fields) throws Exception {
        Method method = HrmEmployeeServiceImpl.class.getDeclaredMethod("normalizeEmployeeSalaryInformationFields", List.class);
        method.setAccessible(true);
        method.invoke(service, fields);
    }

    private HrmEmployeeField field(Long fieldId, String fieldName, String name, Integer type, Integer labelGroup, Integer precisions) {
        HrmEmployeeField field = new HrmEmployeeField();
        field.setFieldId(fieldId);
        field.setFieldName(fieldName);
        field.setName(name);
        field.setType(type);
        field.setLabelGroup(labelGroup);
        field.setPrecisions(precisions);
        field.setIsFixed(0);
        field.setIsHidden(0);
        field.setIsUpdateValue(1);
        return field;
    }

    private HrmEmployeeData findData(List<HrmEmployeeData> dataList, String name) {
        return dataList.stream().filter(data -> name.equals(data.getName())).findFirst().orElseThrow(AssertionError::new);
    }

    private UpdateInformationBO.InformationFieldBO updateField(Long fieldId, String fieldName, String name, Integer type, Object fieldValue) {
        UpdateInformationBO.InformationFieldBO field = new UpdateInformationBO.InformationFieldBO();
        field.setFieldId(fieldId);
        field.setFieldName(fieldName);
        field.setName(name);
        field.setType(type);
        field.setFieldValue(fieldValue);
        field.setIsFixed(0);
        return field;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = HrmEmployeeServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
