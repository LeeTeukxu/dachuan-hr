package com.tianye.hrsystem.modules.salary.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.vo.HrmEmployeeVO;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.entity.SalaryBaseTotal;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthEmpRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryTaxRule;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SalaryComputeServiceNewTest {

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void baseComputeSalaryFromMemory_shouldPaySalary_basicCase() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "5000", 1, 1));
        items.add(buildDto(10102, 10, "3000", 1, 1));
        items.add(buildDto(100101, 100, "500", 0, 1));
        items.add(buildDto(100102, 100, "300", 0, 1));
        items.add(buildDto(280, 280, "200", 0, 0));
        items.add(buildDto(282, 282, "100", 0, 0));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("8000")));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(new BigDecimal("800")));
        Assert.assertEquals(0, result.getOtherNoTaxDeductions().compareTo(new BigDecimal("200")));
        Assert.assertEquals(0, result.getTotalloanMoney().compareTo(new BigDecimal("100")));
    }

    @Test
    public void baseComputeSalaryFromMemory_withSubtractItem_shouldDeduct() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "5000", 1, 1));
        items.add(buildDto(180101, 180, "300", 0, 1));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("4700")));
    }

    @Test
    public void baseComputeSalaryFromMemory_emptyList_shouldReturnZeros() {
        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(
            Collections.emptyList(), null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void baseComputeSalaryFromMemory_nullValue_shouldTreatAsZero() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, null, 1, 1));
        items.add(buildDto(10102, 10, "3000", 1, 1));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("3000")));
    }

    @Test
    public void baseComputeSalaryFromMemory_nonNumericValue_shouldTreatAsZero() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "abc", 1, 1));
        items.add(buildDto(10102, 10, "3000", 1, 1));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("3000")));
    }

    @Test
    public void baseComputeSalaryFromMemory_nullList_shouldReturnZeros() {
        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(null, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(BigDecimal.ZERO));
    }


    @Test
    public void calculateCumulativeIncome_shouldIncludeTaxOnlyBonusWithoutAddingPayrollBonusForChengdu() {
        BigDecimal result = SalaryComputeServiceNew.calculateCumulativeIncome(
                new BigDecimal("1000"),
                new BigDecimal("6000"),
                new BigDecimal("800"),
                new BigDecimal("300"),
                new BigDecimal("200"),
                "0002"
        );

        Assert.assertEquals(0, result.compareTo(new BigDecimal("7500")));
    }

    @Test
    public void calculateCumulativeIncome_shouldIncludePayrollAndTaxOnlyBonusForNormalCompany() {
        BigDecimal result = SalaryComputeServiceNew.calculateCumulativeIncome(
                new BigDecimal("1000"),
                new BigDecimal("6000"),
                new BigDecimal("800"),
                new BigDecimal("300"),
                new BigDecimal("200"),
                "0001"
        );

        Assert.assertEquals(0, result.compareTo(new BigDecimal("8300")));
    }

    @Test
    public void resolveCumulativeDeductions_shouldUseMonthTimes5000ForEmployeeWithoutRemark() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250102, "0");

        BigDecimal result = SalaryComputeServiceNew.resolveCumulativeDeductions(lastTaxMap, 6, false);

        Assert.assertEquals(0, result.compareTo(new BigDecimal("30000")));
    }

    @Test
    public void resolveCumulativeDeductions_shouldContinueImportedPriorDeductionBeforeRemarkFallback() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250102, "25000");

        BigDecimal result = SalaryComputeServiceNew.resolveCumulativeDeductions(lastTaxMap, 6, true);

        Assert.assertEquals(0, result.compareTo(new BigDecimal("30000")));
    }

    @Test
    public void resolveCumulativeDeductions_shouldUseAnnual60000ForRemarkWhenPriorDeductionMissing() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250102, "0");

        BigDecimal result = SalaryComputeServiceNew.resolveCumulativeDeductions(lastTaxMap, 6, true);

        Assert.assertEquals(0, result.compareTo(new BigDecimal("60000")));
    }

    @Test
    public void resolveCumulativeDeductions_shouldCapImportedPriorDeductionAtAnnualLimit() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250102, "60000");

        BigDecimal result = SalaryComputeServiceNew.resolveCumulativeDeductions(lastTaxMap, 6, true);

        Assert.assertEquals(0, result.compareTo(new BigDecimal("60000")));
    }

    @Test
    public void resolveCumulativeDeductions_shouldRestartFromJanuaryForNewTaxYear() {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250102, "60000");

        BigDecimal result = SalaryComputeServiceNew.resolveCumulativeDeductions(lastTaxMap, 1, false);

        Assert.assertEquals(0, result.compareTo(new BigDecimal("5000")));
    }

    @Test
    public void computeSalary_disabledEmployee_shouldPayUnionFeeAndSkipTax() {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setCompanyId("0003");
        CompanyContext.set(loginUserInfo);

        SalaryComputeServiceNew service = buildComputeServiceForUnionFeeTest();

        SalaryBaseTotal salaryBaseTotal = new SalaryBaseTotal();
        salaryBaseTotal.setShouldPaySalary(new BigDecimal("4200.00"));
        salaryBaseTotal.setProxyPaySalary(new BigDecimal("600.00"));

        HrmSalaryMonthEmpRecord record = new HrmSalaryMonthEmpRecord()
                .setSEmpRecordId(9001L)
                .setEmployeeId(1001L)
                .setYear(2026)
                .setMonth(7);
        HrmEmployeeVO employeeVO = new HrmEmployeeVO()
                .setEmployeeId(1001L);
        HrmSalaryTaxRule taxRule = new HrmSalaryTaxRule()
                .setMarkingPoint(5000);

        List<HrmSalaryMonthOptionValue> result = service.computeSalary(
                salaryBaseTotal, record, taxRule, null, employeeVO, "1");

        Map<Integer, HrmSalaryMonthOptionValue> valuesByCode = result.stream()
                .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity()));

        Assert.assertEquals("残疾员工也应按应发工资0.5%生成工会费",
                0, new BigDecimal(valuesByCode.get(160102).getValue()).compareTo(new BigDecimal("21.00")));
        Assert.assertEquals("残疾员工仍保持免个税",
                0, new BigDecimal(valuesByCode.get(230101).getValue()).compareTo(BigDecimal.ZERO));
        Assert.assertEquals("代扣小计应包含社保公积金和工会费，不包含个税",
                0, new BigDecimal(valuesByCode.get(1001).getValue()).compareTo(new BigDecimal("621.00")));
        Assert.assertEquals("实发工资应扣除工会费",
                0, new BigDecimal(valuesByCode.get(240101).getValue()).compareTo(new BigDecimal("3579.00")));
    }

    private ComputeSalaryDto buildDto(int code, int parentCode, String value, int isPlus, int isTax) {
        ComputeSalaryDto dto = new ComputeSalaryDto();
        dto.setCode(code);
        dto.setParentCode(parentCode);
        dto.setValue(value);
        dto.setIsPlus(isPlus);
        dto.setIsTax(isTax);
        return dto;
    }

    private SalaryComputeServiceNew buildComputeServiceForUnionFeeTest() {
        SalaryComputeServiceNew service = new SalaryComputeServiceNew();

        HrmSalaryMonthOptionValueService optionValueService = Mockito.mock(HrmSalaryMonthOptionValueService.class);
        HrmSalaryMonthEmpRecordService empRecordService = Mockito.mock(HrmSalaryMonthEmpRecordService.class);
        IHrmEmployeeService employeeService = Mockito.mock(IHrmEmployeeService.class);
        com.tianye.hrsystem.modules.bonus.mapper.HrmBonusMapper bonusMapper =
                Mockito.mock(com.tianye.hrsystem.modules.bonus.mapper.HrmBonusMapper.class);
        com.tianye.hrsystem.modules.bonus.mapper.HrmBonusTaxOnlyMapper taxOnlyBonusMapper =
                Mockito.mock(com.tianye.hrsystem.modules.bonus.mapper.HrmBonusTaxOnlyMapper.class);
        com.tianye.hrsystem.modules.deduction.mapper.HrmPersonalIncomeTaxMapper incomeTaxMapper =
                Mockito.mock(com.tianye.hrsystem.modules.deduction.mapper.HrmPersonalIncomeTaxMapper.class);
        LambdaQueryChainWrapper<HrmSalaryMonthOptionValue> optionQuery = Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<HrmSalaryMonthEmpRecord> empRecordQuery = Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<HrmSalaryMonthOptionValue> optionUpdate = Mockito.mock(LambdaUpdateChainWrapper.class);

        HrmEmployee employee = new HrmEmployee()
                .setEmployeeId(1001L)
                .setStatus(1)
                .setEntryStatus(1)
                .setBecomeTime(LocalDate.of(2026, 6, 1));

        Mockito.when(optionValueService.lambdaQuery()).thenReturn(optionQuery);
        Mockito.when(optionQuery.eq(Mockito.any(), Mockito.any())).thenReturn(optionQuery);
        Mockito.when(optionQuery.in(Mockito.any(), Mockito.anyCollection())).thenReturn(optionQuery);
        Mockito.when(optionQuery.list()).thenReturn(Collections.emptyList());

        Mockito.when(empRecordService.lambdaQuery()).thenReturn(empRecordQuery);
        Mockito.when(empRecordQuery.eq(Mockito.any(), Mockito.any())).thenReturn(empRecordQuery);
        Mockito.when(empRecordQuery.oneOpt()).thenReturn(Optional.empty());

        Mockito.when(optionValueService.lambdaUpdate()).thenReturn(optionUpdate);
        Mockito.when(optionUpdate.in(Mockito.any(), Mockito.anyCollection())).thenReturn(optionUpdate);
        Mockito.when(optionUpdate.eq(Mockito.any(), Mockito.any())).thenReturn(optionUpdate);
        Mockito.when(optionUpdate.remove()).thenReturn(true);

        Mockito.when(employeeService.getById(1001L)).thenReturn(employee);

        ReflectionTestUtils.setField(service, "salaryMonthOptionValueService", optionValueService);
        ReflectionTestUtils.setField(service, "salaryMonthEmpRecordService", empRecordService);
        ReflectionTestUtils.setField(service, "employeeService", employeeService);
        ReflectionTestUtils.setField(service, "hrmBonusMapper", bonusMapper);
        ReflectionTestUtils.setField(service, "hrmBonusTaxOnlyMapper", taxOnlyBonusMapper);
        ReflectionTestUtils.setField(service, "incomeTaxMapper", incomeTaxMapper);

        return service;
    }
}
