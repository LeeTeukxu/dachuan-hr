package com.tianye.hrsystem.modules.salary.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthRecord;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthEmpRecordService;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthRecordService;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SalaryMonthRecordInsuranceValidationTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void validateInsuranceData_shouldAllowIncompleteMonthRecord_whenEmployeeInsuranceDataExists() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        HrmInsuranceMonthRecordService monthRecordService = Mockito.mock(HrmInsuranceMonthRecordService.class);
        HrmInsuranceMonthEmpRecordService empRecordService = Mockito.mock(HrmInsuranceMonthEmpRecordService.class);

        LambdaQueryChainWrapper monthQuery = Mockito.mock(LambdaQueryChainWrapper.class);
        when(monthRecordService.lambdaQuery()).thenReturn(monthQuery);
        when(monthQuery.eq(any(), any())).thenReturn(monthQuery);
        when(monthQuery.oneOpt()).thenReturn(Optional.of(
                new HrmInsuranceMonthRecord().setYear(2026).setMonth(2).setStatus(0)
        ));

        LambdaQueryChainWrapper empQuery = Mockito.mock(LambdaQueryChainWrapper.class);
        when(empRecordService.lambdaQuery()).thenReturn(empQuery);
        when(empQuery.eq(any(), any())).thenReturn(empQuery);
        when(empQuery.exists()).thenReturn(true);

        setField(service, "insuranceMonthRecordService", monthRecordService);
        setField(service, "insuranceMonthEmpRecordService", empRecordService);

        HrmSalaryConfig salaryConfig = new HrmSalaryConfig();
        salaryConfig.setSocialSecurityMonthType(1);

        invokeValidateInsuranceData(service, true, salaryConfig, 2026, 2);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void validateInsuranceData_shouldThrow6012_whenMonthRecordMissing() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        HrmInsuranceMonthRecordService monthRecordService = Mockito.mock(HrmInsuranceMonthRecordService.class);

        LambdaQueryChainWrapper monthQuery = Mockito.mock(LambdaQueryChainWrapper.class);
        when(monthRecordService.lambdaQuery()).thenReturn(monthQuery);
        when(monthQuery.eq(any(), any())).thenReturn(monthQuery);
        when(monthQuery.oneOpt()).thenReturn(Optional.empty());

        setField(service, "insuranceMonthRecordService", monthRecordService);

        HrmSalaryConfig salaryConfig = new HrmSalaryConfig();
        salaryConfig.setSocialSecurityMonthType(1);

        try {
            invokeValidateInsuranceData(service, true, salaryConfig, 2026, 2);
            Assert.fail("expected HrmException");
        } catch (HrmException ex) {
            Assert.assertEquals(6012, ex.getCode());
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void collectInsuranceDataErrors_shouldReturnUserReadablePrecheckMessage_whenMonthRecordMissing() throws Exception {
        SalaryMonthRecordServiceNew service = new SalaryMonthRecordServiceNew();
        HrmInsuranceMonthRecordService monthRecordService = Mockito.mock(HrmInsuranceMonthRecordService.class);

        LambdaQueryChainWrapper monthQuery = Mockito.mock(LambdaQueryChainWrapper.class);
        when(monthRecordService.lambdaQuery()).thenReturn(monthQuery);
        when(monthQuery.eq(any(), any())).thenReturn(monthQuery);
        when(monthQuery.oneOpt()).thenReturn(Optional.empty());

        setField(service, "insuranceMonthRecordService", monthRecordService);

        HrmSalaryConfig salaryConfig = new HrmSalaryConfig();
        salaryConfig.setSocialSecurityMonthType(1);

        java.util.List<String> errors = invokeCollectInsuranceDataErrors(service, true, salaryConfig, 2026, 2);

        Assert.assertEquals(1, errors.size());
        Assert.assertTrue(errors.get(0).contains("2026-2"));
        Assert.assertTrue(errors.get(0).contains("社保月记录不存在"));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SalaryMonthRecordServiceNew.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void invokeValidateInsuranceData(SalaryMonthRecordServiceNew service,
                                                    Boolean isSyncInsuranceData,
                                                    HrmSalaryConfig salaryConfig,
                                                    int year,
                                                    int month) throws Exception {
        Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "validateInsuranceData", Boolean.class, HrmSalaryConfig.class, int.class, int.class);
        method.setAccessible(true);
        try {
            method.invoke(service, isSyncInsuranceData, salaryConfig, year, month);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<String> invokeCollectInsuranceDataErrors(SalaryMonthRecordServiceNew service,
                                                                           Boolean isSyncInsuranceData,
                                                                           HrmSalaryConfig salaryConfig,
                                                                           int year,
                                                                           int month) throws Exception {
        Method method = SalaryMonthRecordServiceNew.class.getDeclaredMethod(
                "collectInsuranceDataErrors", Boolean.class, HrmSalaryConfig.class, int.class, int.class);
        method.setAccessible(true);
        return (java.util.List<String>) method.invoke(service, isSyncInsuranceData, salaryConfig, year, month);
    }
}
