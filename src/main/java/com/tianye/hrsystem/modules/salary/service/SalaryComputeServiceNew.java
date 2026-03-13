package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.vo.HrmEmployeeVO;
import com.tianye.hrsystem.enums.EmployeeStatusEnum;
import com.tianye.hrsystem.enums.IsEnum;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonus;
import com.tianye.hrsystem.modules.bonus.mapper.HrmBonusMapper;
import com.tianye.hrsystem.modules.deduction.entity.HrmPersonalIncomeTax;
import com.tianye.hrsystem.modules.deduction.mapper.HrmPersonalIncomeTaxMapper;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.dto.SalaryMonthOptionValueDto;
import com.tianye.hrsystem.modules.salary.entity.*;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthOptionValueMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryComputeServiceNew
{

    /** @deprecated 已由 {@link TaxCalculator} 替代，保留仅供旧代码交叉验证 */
    @Deprecated
    private static RangeMap<BigDecimal, TaxEntity> taxRateRangeMap = TreeRangeMap.create();

    /** @deprecated 同 taxRateRangeMap，已由 TaxCalculator 替代 */
    static {
        taxRateRangeMap.put(Range.lessThan(new BigDecimal(0)), new TaxEntity(0, 0));
        taxRateRangeMap.put(Range.closed(new BigDecimal(0), new BigDecimal(36000)), new TaxEntity(3, 0));
        taxRateRangeMap.put(Range.openClosed(new BigDecimal(36000), new BigDecimal(144000)), new TaxEntity(10, 2520));
        taxRateRangeMap.put(Range.openClosed(new BigDecimal(144000), new BigDecimal(300000)), new TaxEntity(20, 16920));
        taxRateRangeMap.put(Range.openClosed(new BigDecimal(300000), new BigDecimal(420000)), new TaxEntity(25, 31920));
        taxRateRangeMap.put(Range.openClosed(new BigDecimal(420000), new BigDecimal(660000)), new TaxEntity(30, 52920));
        taxRateRangeMap.put(Range.openClosed(new BigDecimal(660000), new BigDecimal(960000)), new TaxEntity(35, 85920));
        taxRateRangeMap.put(Range.atLeast(new BigDecimal(960000)), new TaxEntity(45, 181920));
    }


    @Autowired
    HrmSalaryMonthEmpRecordService salaryMonthEmpRecordService;

    @Autowired
    HrmSalaryMonthOptionValueService salaryMonthOptionValueService;

    @Autowired
    HrmPersonalIncomeTaxMapper incomeTaxMapper;

    @Autowired
    HrmAdditionalMapper additionalMapper;

    @Autowired
    HrmBonusMapper hrmBonusMapper;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    HrmSalaryMonthOptionValueMapper hrmSalaryMonthOptionValueMapper;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public SalaryBaseTotal baseComputeSalary(HrmSalaryMonthEmpRecord salaryMonthEmpRecord)
    {
        LoginUserInfo Info = CompanyContext.get();
        SalaryBaseTotal salaryBaseTotal = new SalaryBaseTotal();
        //所有工资项
        List<ComputeSalaryDto> computeSalaryDTOS = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(salaryMonthEmpRecord.getSEmpRecordId());
        List<Integer> listSalaryMonthOptionValueCode = new ArrayList<>();
        listSalaryMonthOptionValueCode.add(41001);
        //应发工资金额 code = 210101
        //应发工资金额（基本工资+津补贴+浮动工资+奖金+提成工资+计件工资+计时工资+工龄/司龄工资+职称工资+税前补发 - 税前补扣）- 考勤扣款合计+加班工资+满勤奖
        List<Integer> shouldPayCodeList = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 130, 140, 180, 200);
        BigDecimal shouldPaySalary = new BigDecimal(0);
        //加减税总额 (特殊计税项 - 扣代缴项总金额 )
        //扣代缴项总金额(个人社保+个人公积金等)
        BigDecimal proxyPaySalary = new BigDecimal(0);
        //特殊计税项
        BigDecimal specialTaxSalary = new BigDecimal(0);
        //税后补发-税后补扣   parentCode=150 - parentCode=160
        BigDecimal taxAfterPaySalary = new BigDecimal(0);
        //个税专项附加扣除累计 parentCode=260
        BigDecimal taxSpecialGrandTotal = new BigDecimal(0);
        //其他扣款
        BigDecimal otherNoTaxDeductions = new BigDecimal(0);
        //借款
        BigDecimal loanMoney = new BigDecimal(0);
        for (ComputeSalaryDto computeSalaryDTO : computeSalaryDTOS) {
            //计算代扣代缴项总金额
            if (computeSalaryDTO.getParentCode().equals(100) && !computeSalaryDTO.getCode().equals(1001)) {
                proxyPaySalary = proxyPaySalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getParentCode().equals(170)) {
                specialTaxSalary = specialTaxSalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getParentCode().equals(150)) {
                taxAfterPaySalary = taxAfterPaySalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
//            if (computeSalaryDTO.getParentCode().equals(160)) {
//                taxAfterPaySalary = taxAfterPaySalary.subtract(new BigDecimal(computeSalaryDTO.getValue()));
//            }
            if (computeSalaryDTO.getParentCode().equals(260)) {
                taxSpecialGrandTotal = taxSpecialGrandTotal.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getCode().equals(280)) {
                //其他扣款
                otherNoTaxDeductions = otherNoTaxDeductions.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getCode().equals(282)) {
                //借款
                loanMoney = loanMoney.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getCode().equals(281)) {
                //其他补贴
                shouldPaySalary = shouldPaySalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }

            if (Info.getCompanyId().equals("0002")) {
                if (computeSalaryDTO.getCode().equals(41001)) {
                    String bouns = salaryMonthOptionValueService.getBounsBySEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId(),41001);
                    if (bouns != null) {
                        shouldPaySalary = shouldPaySalary.add(new BigDecimal(bouns));
                    }
                }
            }

            //计算应发工资(应发工资=员工工资总额-请假扣款-考勤扣款;代扣代缴不需要计算:parentCode=90;企业社保不需要计算:parentCode=110;企业公积金不需要计算:parentCode=110)
            if (shouldPayCodeList.contains(computeSalaryDTO.getParentCode())) {
                if (computeSalaryDTO.getIsPlus() == IsEnum.YES.getValue()) {
                    shouldPaySalary = shouldPaySalary.add(new BigDecimal(computeSalaryDTO.getValue().toString()));
                } else if (computeSalaryDTO.getIsPlus() == IsEnum.NO.getValue()) {
                    shouldPaySalary = shouldPaySalary.subtract(new BigDecimal(computeSalaryDTO.getValue().toString()));
                }
            }
//            if (computeSalaryDTO.getIsCompute() == 1 && computeSalaryDTO.getIsPlus() == 1 && computeSalaryDTO.getIsTax() == 1){
//                cumulativeTaxFreeIncome = cumulativeTaxFreeIncome.add(new BigDecimal(computeSalaryDTO.getValue()));
//            }
        }
        salaryBaseTotal.setShouldPaySalary(shouldPaySalary);
        salaryBaseTotal.setProxyPaySalary(proxyPaySalary);
        salaryBaseTotal.setTaxAfterPaySalary(taxAfterPaySalary);
        salaryBaseTotal.setTaxSpecialGrandTotal(taxSpecialGrandTotal);
        salaryBaseTotal.setSpecialTaxSalary(specialTaxSalary);
        salaryBaseTotal.setOtherNoTaxDeductions(otherNoTaxDeductions);
        salaryBaseTotal.setTotalloanMoney(loanMoney);

        return salaryBaseTotal;
    }



    /**
     * 计算工资，与扣税
     * @param salaryMonthEmpRecord
     * @param taxRule
     * @param cumulativeTaxOfLastMonthData
     * @return
     */
    public List<HrmSalaryMonthOptionValue> computeSalary(SalaryBaseTotal salaryBaseTotal, HrmSalaryMonthEmpRecord salaryMonthEmpRecord, HrmSalaryTaxRule taxRule,
                                                         Map<Integer, String> cumulativeTaxOfLastMonthData, HrmEmployeeVO hrmEmployeeVO, String isDisabled) {
        return computeSalary(salaryBaseTotal, salaryMonthEmpRecord, taxRule,
                cumulativeTaxOfLastMonthData, hrmEmployeeVO, isDisabled, BigDecimal.ZERO);
    }

    public List<HrmSalaryMonthOptionValue> computeSalary(SalaryBaseTotal salaryBaseTotal, HrmSalaryMonthEmpRecord salaryMonthEmpRecord, HrmSalaryTaxRule taxRule,
                                                         Map<Integer, String> cumulativeTaxOfLastMonthData, HrmEmployeeVO hrmEmployeeVO, String isDisabled,
                                                         BigDecimal welfareTaxableIncome) {
        LoginUserInfo Info = CompanyContext.get();
        List<HrmSalaryMonthOptionValue> salaryMonthOptionValueList = new ArrayList<>();
        BigDecimal shouldTaxSalary = new BigDecimal(0);
        //基础应税工资-不包含每月减除费用(应发工资 + 特殊计税项 - 扣代缴项总金额 ) code = 210101
        BigDecimal baseShouldTaxSalary = salaryBaseTotal.getShouldPaySalary().add(salaryBaseTotal.getSpecialTaxSalary()).subtract(salaryBaseTotal.getProxyPaySalary());
        if (baseShouldTaxSalary.compareTo(new BigDecimal(taxRule.getMarkingPoint())) > 0) {
            shouldTaxSalary = baseShouldTaxSalary.subtract(new BigDecimal(taxRule.getMarkingPoint()));
        }
        //本月上月个税累计信息对应上月个税累计信息 code
        Map<Integer, Integer> lastTaxOptionCodeMap = new HashMap<>();
        lastTaxOptionCodeMap.put(270101, 250101);
        lastTaxOptionCodeMap.put(270102, 250102);
        lastTaxOptionCodeMap.put(270103, 250103);
        lastTaxOptionCodeMap.put(270106, 250105);
        Map<Integer, String> lastTaxOptionValueMap;
        List<HrmSalaryMonthOptionValue> lastTaxOptionValueList = salaryMonthOptionValueService.lambdaQuery().eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId())
                    .in(HrmSalaryMonthOptionValue::getCode, lastTaxOptionCodeMap.values()).list();
        String strYearMonth = String.format("%d-%02d", salaryMonthEmpRecord.getYear(), salaryMonthEmpRecord.getMonth());
        int[] yearMonthFormatter = getLastMonthYearAndMonth(strYearMonth);
        if (CollUtil.isNotEmpty(lastTaxOptionValueList) && cumulativeTaxOfLastMonthData == null) {
            //如果是更新薪资(有个税累计数据),则查询本月生成的上月个税累计信息
            lastTaxOptionValueMap = lastTaxOptionValueList.stream().collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, HrmSalaryMonthOptionValue::getValue));
        } else {
            List<HrmSalaryMonthOptionValue> lastTaxOptionValueList1;
            //(没有个税累计数据，查询员工上个月薪资数据)，本月个税累计对应上月个税累计code
            Optional<HrmSalaryMonthEmpRecord> lastSalaryMonthEmpRecordOpt = salaryMonthEmpRecordService.lambdaQuery().eq(HrmSalaryMonthEmpRecord::getYear, yearMonthFormatter[0])
                    .eq(HrmSalaryMonthEmpRecord::getMonth, yearMonthFormatter[1]).eq(HrmSalaryMonthEmpRecord::getEmployeeId, salaryMonthEmpRecord.getEmployeeId()).oneOpt();
            //算出 上月个税累计信息 12月份开始重新累计
            if (lastSalaryMonthEmpRecordOpt.isPresent() && salaryMonthEmpRecord.getMonth() != 12) {
                HrmSalaryMonthEmpRecord lastSalaryMonthEmpRecord = lastSalaryMonthEmpRecordOpt.get();
                lastTaxOptionValueList1 = salaryMonthOptionValueService.lambdaQuery().eq(HrmSalaryMonthOptionValue::getSEmpRecordId, lastSalaryMonthEmpRecord.getSEmpRecordId())
                        .in(HrmSalaryMonthOptionValue::getCode, lastTaxOptionCodeMap.keySet()).list();
                if (CollUtil.isEmpty(lastTaxOptionValueList1)) {
                    lastTaxOptionCodeMap.keySet().forEach(code -> {
                        HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                        salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
                        salaryMonthOptionValue.setCode(code);
                        salaryMonthOptionValue.setValue("0");
                        lastTaxOptionValueList1.add(salaryMonthOptionValue);
                    });
                }
            } else {
                //查询不到上月,默认填充0
                lastTaxOptionValueList1 = new ArrayList<>();
                lastTaxOptionCodeMap.keySet().forEach(code -> {
                    HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                    salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
                    salaryMonthOptionValue.setCode(code);
                    salaryMonthOptionValue.setValue("0");
                    lastTaxOptionValueList1.add(salaryMonthOptionValue);
                });
            }
            lastTaxOptionValueMap = lastTaxOptionValueList1.stream().peek(option -> option.setCode(lastTaxOptionCodeMap.get(option.getCode())))
                    .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, HrmSalaryMonthOptionValue::getValue));
            //12月份不需要从上个月开始累计，重新开始累计
            if (salaryMonthEmpRecord.getMonth() == 12) {
                // 12月重置所有累计个税数据
                lastTaxOptionValueMap.put(250101, "0"); // 累计收入
                lastTaxOptionValueMap.put(250102, "0"); // 累计减除费用
                lastTaxOptionValueMap.put(250103, "0"); // 累计专项扣除
                lastTaxOptionValueMap.put(250105, "0"); // 累计已缴税额
            } else if (CollUtil.isNotEmpty(cumulativeTaxOfLastMonthData)) {
                //是否导入上月个税累计,导入需覆盖
                cumulativeTaxOfLastMonthData.forEach(lastTaxOptionValueMap::put);
            }
        }
        //当员工为残疾人时(状态为1),不计算工会费
        BigDecimal labourunionPay = new BigDecimal(0);
        if (isDisabled.equals("2")) {
            // 计算工会费
            labourunionPay = calculateUnionFee(hrmEmployeeVO, salaryMonthEmpRecord, salaryBaseTotal);
        }
        // 计算当月奖金
        BigDecimal bonusSalary = getBonusSalary(hrmEmployeeVO.getEmployeeId(), salaryMonthEmpRecord);
        // 计算个税累计信息
        TaxAccumulation taxAccumulation = calculateTaxAccumulation(
            salaryBaseTotal, lastTaxOptionValueMap, bonusSalary, salaryMonthEmpRecord, Info,
            welfareTaxableIncome == null ? BigDecimal.ZERO : welfareTaxableIncome
        );
        
        // 当 hrm_employee.is_remark=2 且 上年度累计收入+本年度累计收入<6万 则不计算个税
        boolean skipTaxForRemark = false;
        HrmEmployee employee = employeeService.getById(hrmEmployeeVO.getEmployeeId());
        if (employee != null && employee.getIsRemark() != null && employee.getIsRemark() == 2) {
            BigDecimal lastYearAccumulated = incomeTaxMapper.getAccumulatedIncomeByEmployeeAndYear(
                hrmEmployeeVO.getEmployeeId(), salaryMonthEmpRecord.getYear() - 1);
            if (lastYearAccumulated == null) {
                lastYearAccumulated = BigDecimal.ZERO;
            }
            BigDecimal totalAccumulated = lastYearAccumulated.add(taxAccumulation.cumulativeIncome);
            if (totalAccumulated.compareTo(new BigDecimal("60000")) < 0) {
                skipTaxForRemark = true;
            }
        }
        
        // 计算当月个税
        //当员工为残疾人时(状态为1),不计算个税；当 is_remark=2 且累计收入<6万时也不计算个税
        BigDecimal payTaxSalary = new BigDecimal(0);
        if (isDisabled.equals("2") && !skipTaxForRemark) {
            payTaxSalary = calculateMonthTax(taxAccumulation, lastTaxOptionValueMap);
        }
        // 计算实发工资
        BigDecimal realPaySalary = calculateRealPaySalary(
            salaryBaseTotal, payTaxSalary, labourunionPay
        );
        // 构建工资项数据
        Map<Integer, String> codeValueMap = buildSalaryCodeValueMap(
            salaryBaseTotal, shouldTaxSalary, payTaxSalary, realPaySalary, 
            labourunionPay, bonusSalary, lastTaxOptionValueMap, taxAccumulation
        );



        //删除表hrm_salary_month_option_value里面之前指定工资项的数据
        salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(
                210101, 220101, 230101, 240101,
                250101, 250102, 250103, 250105,
                270101, 270102, 270103, 270104, 270105, 270106,160102,1001,41001))
                .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
        //构建新的工资项数据
        codeValueMap.forEach((code, value) -> {
            HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
            salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
            salaryMonthOptionValue.setCode(code);
            salaryMonthOptionValue.setValue(value);
            salaryMonthOptionValueList.add(salaryMonthOptionValue);
        });

        // 保存个税累计数据
        saveTaxAccumulationData(hrmEmployeeVO, salaryMonthEmpRecord, taxAccumulation, lastTaxOptionValueMap, payTaxSalary);

        return salaryMonthOptionValueList;
    }

    /** @deprecated 已由 {@link TaxCalculator} 替代，保留仅用于交叉验证 */
    @Deprecated
    private static final BigDecimal[] TAX_RATES = {
            new BigDecimal("0.03"),  // 3%
            new BigDecimal("0.10"),  // 10%
            new BigDecimal("0.20"),  // 20%
            new BigDecimal("0.25"),  // 25%
            new BigDecimal("0.30"),  // 30%
            new BigDecimal("0.35"),  // 35%
            new BigDecimal("0.45")   // 45%
    };

    /** @deprecated 已由 {@link TaxCalculator} 替代，保留仅用于交叉验证 */
    @Deprecated
    private static final BigDecimal[] DEDUCTIONS = {
            new BigDecimal("0"),
            new BigDecimal("2520"),
            new BigDecimal("16920"),
            new BigDecimal("31920"),
            new BigDecimal("52920"),
            new BigDecimal("85920"),
            new BigDecimal("181920")
    };

    /**
     * @deprecated 已由 {@link TaxCalculator#calculateCumulativeTax} 替代，保留仅用于交叉验证。
     */
    @Deprecated
    public static BigDecimal calculateTax(BigDecimal taxableIncome) {
        // 1. 处理应纳税所得额为负数的情况（直接返回0）
        if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal maxTax = BigDecimal.ZERO;

        // 2. 遍历所有税率档次，计算每种档次下的税额（模拟Excel数组运算）
        for (int i = 0; i < TAX_RATES.length; i++) {
            // 税额 = 应纳税所得额 × 税率 - 速算扣除数
            BigDecimal tax = taxableIncome.multiply(TAX_RATES[i]).subtract(DEDUCTIONS[i]);
            // 保留当前最大税额（对应Excel的MAX函数，自动匹配最高适用税率）
            if (tax.compareTo(maxTax) > 0) {
                maxTax = tax;
            }
        }

        // 3. 确保税额不小于0（避免极端情况出现负数），并保留2位小数（对应Excel的ROUND(...,2)）
        return maxTax.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * @deprecated 已由 {@link TaxCalculator#calculateTaxableIncome} 替代
     */
    @Deprecated
    public static BigDecimal calculateTaxIncome(BigDecimal cumulativeIncome,BigDecimal cumulativeSpecialDeduction,BigDecimal cumulativeDeductions,BigDecimal cumulativeSpecialAdditionalDeduction) {
        //累计工资
        //累计本月公积金社保
        //累计减除费用
        //累计附加扣除项目小计
        BigDecimal txtIncome = cumulativeIncome.subtract(cumulativeSpecialDeduction).subtract(cumulativeDeductions).subtract(cumulativeSpecialAdditionalDeduction);
        return txtIncome;
    }

    /** @deprecated 测试方法，使用已废弃的 calculateTax，应移除 */
    @Deprecated
    public void test() {
        // 示例1：M3 = 30000（第1级税率）
        BigDecimal tax1 = calculateTax(new BigDecimal("14072.3"));
        System.out.println(tax1 + " 元");
    }

    public static int[] getLastMonthYearAndMonth(String yearMonthStr) {
        // 1. 解析字符串为 YearMonth 对象
        YearMonth currentYearMonth = YearMonth.parse(yearMonthStr, MONTH_FORMATTER);

        // 2. 计算上一个月（自动处理跨年）
        YearMonth lastYearMonth = currentYearMonth.minusMonths(1);

        // 3. 提取年和月
        int lastYear = lastYearMonth.getYear();
        int lastMonth = lastYearMonth.getMonthValue();

        return new int[]{lastYear, lastMonth};
    }

    /**
     * 计算工会费
     * 规则：正式员工按应发工资的0.5%收取，实习生、离职员工、半路转正员工不收取
     */
    private BigDecimal calculateUnionFee(HrmEmployeeVO hrmEmployeeVO, HrmSalaryMonthEmpRecord salaryMonthEmpRecord, 
                                         SalaryBaseTotal salaryBaseTotal) {
        LoginUserInfo info = CompanyContext.get();
        // 成都、0005攀枝花公司不收工会费
        if (info.getCompanyId().equals("0002") || info.getCompanyId().equals("0005")) {
            return BigDecimal.ZERO;
        }
        
        // 应发工资小于等于0不收工会费
        if (salaryBaseTotal.getShouldPaySalary().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        HrmEmployee employee = employeeService.getById(hrmEmployeeVO.getEmployeeId());
        // 实习生、离职员工不收工会费
        if (employee.getStatus() == 3 || employee.getEntryStatus() == 4) {
            return BigDecimal.ZERO;
        }
        
        // 半路转正员工不收工会费（转正日期在月份中间）
        if (employee.getBecomeTime() != null) {
            YearMonth becomeMonth = YearMonth.from(employee.getBecomeTime());
            YearMonth salaryMonth = YearMonth.of(salaryMonthEmpRecord.getYear(), salaryMonthEmpRecord.getMonth());
            // 如果转正月份等于计薪月份，且转正日不是1号，则为半路转正
            if (becomeMonth.equals(salaryMonth) && employee.getBecomeTime().getDayOfMonth() != 1) {
                return BigDecimal.ZERO;
            }
        }
        
        // 判断是否已转正
        boolean isOfficialEmployee = isOfficialEmployee(employee, salaryMonthEmpRecord);
        if (isOfficialEmployee) {
            return salaryBaseTotal.getShouldPaySalary()
                .multiply(new BigDecimal("0.005"))
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 判断是否为正式员工
     */
    private boolean isOfficialEmployee(HrmEmployee employee, HrmSalaryMonthEmpRecord salaryMonthEmpRecord) {
        // 没有转正日期，但状态为正式员工
        if (employee.getBecomeTime() == null) {
            return employee.getStatus() != null && employee.getStatus() == 1;
        }
        
        // 有转正日期，判断转正月份是否小于等于计薪月份
        YearMonth becomeMonth = YearMonth.from(employee.getBecomeTime());
        YearMonth salaryMonth = YearMonth.of(salaryMonthEmpRecord.getYear(), salaryMonthEmpRecord.getMonth());
        return becomeMonth.equals(salaryMonth) || becomeMonth.isBefore(salaryMonth);
    }
    
    /**
     * 获取当月奖金
     */
    private BigDecimal getBonusSalary(Long employeeId, HrmSalaryMonthEmpRecord salaryMonthEmpRecord) {
        HrmBonus bonus = hrmBonusMapper.getEmpBonus(
            employeeId, salaryMonthEmpRecord.getYear(), salaryMonthEmpRecord.getMonth()
        );
        if (bonus != null && bonus.getBonus().compareTo(BigDecimal.ZERO) > 0) {
            return bonus.getBonus();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * 计算个税累计信息
     */
    private TaxAccumulation calculateTaxAccumulation(SalaryBaseTotal salaryBaseTotal,
                                                     Map<Integer, String> lastTaxOptionValueMap,
                                                     BigDecimal bonusSalary,
                                                     HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                     LoginUserInfo info,
                                                     BigDecimal welfareTaxableIncome) {
        TaxAccumulation accumulation = new TaxAccumulation();
        BigDecimal safeWelfareTaxableIncome = welfareTaxableIncome == null ? BigDecimal.ZERO : welfareTaxableIncome;
        
        // 累计收入额 = 上月累计 + 本月应发工资 + 奖金(成都公司除外)
        BigDecimal lastIncome = new BigDecimal(lastTaxOptionValueMap.get(250101));
        accumulation.cumulativeIncome = lastIncome.add(salaryBaseTotal.getShouldPaySalary());
        if (!info.getCompanyId().equals("0002")) {
            accumulation.cumulativeIncome = accumulation.cumulativeIncome.add(bonusSalary);
        }
        accumulation.cumulativeIncome = accumulation.cumulativeIncome.add(safeWelfareTaxableIncome);
        
        // 累计减除费用 = 上月累计 + 5000
        accumulation.cumulativeDeductions = new BigDecimal(lastTaxOptionValueMap.get(250102))
            .add(new BigDecimal(5000));
        
        // 累计专项扣除(社保公积金) = 上月累计 + 本月代扣代缴
        accumulation.cumulativeSpecialDeduction = new BigDecimal(lastTaxOptionValueMap.get(250103))
            .add(salaryBaseTotal.getProxyPaySalary());
        
        // 累计专项附加扣除
        accumulation.cumulativeSpecialAdditionalDeduction = salaryBaseTotal.getTaxSpecialGrandTotal();
        
        // 累计应纳税所得额
        accumulation.cumulativeTaxableIncome = TaxCalculator.calculateTaxableIncome(
            accumulation.cumulativeIncome,
            accumulation.cumulativeDeductions,
            accumulation.cumulativeSpecialDeduction,
            accumulation.cumulativeSpecialAdditionalDeduction);

        // 累计应纳税额
        accumulation.cumulativeTaxPayable = TaxCalculator.calculateCumulativeTax(accumulation.cumulativeTaxableIncome);
        
        return accumulation;
    }
    
    /**
     * 计算当月个税
     */
    private BigDecimal calculateMonthTax(TaxAccumulation accumulation, Map<Integer, String> lastTaxOptionValueMap) {
        // 当月个税 = 累计应纳税额 - 累计已缴税额
        BigDecimal monthTax = accumulation.cumulativeTaxPayable
            .subtract(new BigDecimal(lastTaxOptionValueMap.get(250105)));
        // 个税不能为负数
        return monthTax.max(BigDecimal.ZERO);
    }
    
    /**
     * 计算实发工资
     */
    private BigDecimal calculateRealPaySalary(SalaryBaseTotal salaryBaseTotal, 
                                              BigDecimal payTaxSalary, 
                                              BigDecimal labourunionPay) {
        if (salaryBaseTotal.getShouldPaySalary().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 实发工资 = 应发工资 - 代扣代缴 - 个税 + 税后补发 - 工会费 - 其他扣款 - 借款
        return salaryBaseTotal.getShouldPaySalary()
            .subtract(salaryBaseTotal.getProxyPaySalary())
            .subtract(payTaxSalary)
            .add(salaryBaseTotal.getTaxAfterPaySalary())
            .subtract(labourunionPay)
            .subtract(salaryBaseTotal.getOtherNoTaxDeductions())
            .subtract(salaryBaseTotal.getTotalloanMoney());
    }
    
    /**
     * 构建工资项数据集合
     */
    private Map<Integer, String> buildSalaryCodeValueMap(SalaryBaseTotal salaryBaseTotal,
                                                         BigDecimal shouldTaxSalary,
                                                         BigDecimal payTaxSalary,
                                                         BigDecimal realPaySalary,
                                                         BigDecimal labourunionPay,
                                                         BigDecimal bonusSalary,
                                                         Map<Integer, String> lastTaxOptionValueMap,
                                                         TaxAccumulation taxAccumulation) {
        Map<Integer, String> codeValueMap = new HashMap<>();
        
        // 基础工资项
        codeValueMap.put(210101, salaryBaseTotal.getShouldPaySalary().toString()); // 应发工资
        codeValueMap.put(220101, shouldTaxSalary.toString()); // 应税工资
        codeValueMap.put(230101, payTaxSalary.toString()); // 个人所得税
        codeValueMap.put(240101, realPaySalary.toString()); // 实发工资
        codeValueMap.put(160102, labourunionPay.toString()); // 工会费
        codeValueMap.put(41001, bonusSalary.toString()); // 本月奖金
        
        // 上月个税累计信息
        lastTaxOptionValueMap.forEach(codeValueMap::put);
        
        // 本月个税累计信息
        codeValueMap.put(270101, taxAccumulation.cumulativeIncome.toString());
        codeValueMap.put(270102, taxAccumulation.cumulativeDeductions.toString());
        codeValueMap.put(270103, taxAccumulation.cumulativeSpecialDeduction.toString());
        codeValueMap.put(270104, taxAccumulation.cumulativeSpecialAdditionalDeduction.toString());
        codeValueMap.put(270105, taxAccumulation.cumulativeTaxableIncome.toString());
        codeValueMap.put(270106, taxAccumulation.cumulativeTaxPayable.toString());
        
        // 代扣小计 = 公积金 + 社保 + 工会费 + 借款 + 其他 + 个税
        BigDecimal totalDeduction = salaryBaseTotal.getProxyPaySalary()
            .add(labourunionPay)
            .add(salaryBaseTotal.getTotalloanMoney())
            .add(salaryBaseTotal.getOtherNoTaxDeductions())
            .add(payTaxSalary);
        codeValueMap.put(1001, totalDeduction.toString());
        
        return codeValueMap;
    }
    
    /**
     * 保存个税累计数据
     */
    private void saveTaxAccumulationData(HrmEmployeeVO hrmEmployeeVO,
                                        HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                        TaxAccumulation taxAccumulation,
                                        Map<Integer, String> lastTaxOptionValueMap,
                                        BigDecimal payTaxSalary) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("employeeId", hrmEmployeeVO.getEmployeeId());
        params.put("year", salaryMonthEmpRecord.getYear());
        params.put("month", salaryMonthEmpRecord.getMonth());
        
        // 删除旧数据
        incomeTaxMapper.deleteByParams(params);
        
        // 保存新数据
        HrmPersonalIncomeTax incomeTax = new HrmPersonalIncomeTax();
        incomeTax.setEmployeeId(hrmEmployeeVO.getEmployeeId());
        incomeTax.setAccumulatedIncome(taxAccumulation.cumulativeIncome);
        incomeTax.setAccumulatedDeductionOfExpenses(taxAccumulation.cumulativeDeductions);
        incomeTax.setAccumulatedProvidentFund(taxAccumulation.cumulativeSpecialDeduction);
        incomeTax.setAccumulatedTaxPayment(
            new BigDecimal(lastTaxOptionValueMap.get(250105)).add(payTaxSalary)
        );
        incomeTax.setEndMonth(salaryMonthEmpRecord.getMonth());
        incomeTax.setYear(salaryMonthEmpRecord.getYear());
        incomeTaxMapper.insert(incomeTax);
    }
    
    /**
     * 个税累计信息实体
     */
    private static class TaxAccumulation {
        BigDecimal cumulativeIncome; // 累计收入
        BigDecimal cumulativeDeductions; // 累计减除费用
        BigDecimal cumulativeSpecialDeduction; // 累计专项扣除
        BigDecimal cumulativeSpecialAdditionalDeduction; // 累计专项附加扣除
        BigDecimal cumulativeTaxableIncome; // 累计应纳税所得额
        BigDecimal cumulativeTaxPayable; // 累计应纳税额
    }

    /**
     * 从内存中的工资项列表计算 SalaryBaseTotal，不依赖DB查询。
     * 逻辑与 baseComputeSalary 完全一致。
     * <p>注意：code=41001（成都奖金）在内存模式下直接使用传入的 value，
     * 而原 baseComputeSalary 从DB查询获取。调用方需确保 items 中已包含正确的41001值。</p>
     * @param items 工资项列表（等价于 queryEmpSalaryOptionValueList 的返回值）
     * @param companyId 公司ID（用于成都奖金特殊处理），可为null
     */
    public static SalaryBaseTotal baseComputeSalaryFromMemory(List<ComputeSalaryDto> items, String companyId) {
        SalaryBaseTotal total = new SalaryBaseTotal();
        if (items == null || items.isEmpty()) {
            return total;
        }
        List<Integer> shouldPayCodeList = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 130, 140, 180, 200);
        BigDecimal shouldPaySalary = BigDecimal.ZERO;
        BigDecimal proxyPaySalary = BigDecimal.ZERO;
        BigDecimal specialTaxSalary = BigDecimal.ZERO;
        BigDecimal taxAfterPaySalary = BigDecimal.ZERO;
        BigDecimal taxSpecialGrandTotal = BigDecimal.ZERO;
        BigDecimal otherNoTaxDeductions = BigDecimal.ZERO;
        BigDecimal loanMoney = BigDecimal.ZERO;

        for (ComputeSalaryDto dto : items) {
            BigDecimal val = safeParseDecimal(dto.getValue());
            if (dto.getParentCode().equals(100) && !dto.getCode().equals(1001)) {
                proxyPaySalary = proxyPaySalary.add(val);
            }
            if (dto.getParentCode().equals(170)) {
                specialTaxSalary = specialTaxSalary.add(val);
            }
            if (dto.getParentCode().equals(150)) {
                taxAfterPaySalary = taxAfterPaySalary.add(val);
            }
            if (dto.getParentCode().equals(260)) {
                taxSpecialGrandTotal = taxSpecialGrandTotal.add(val);
            }
            if (dto.getCode().equals(280)) {
                otherNoTaxDeductions = otherNoTaxDeductions.add(val);
            }
            if (dto.getCode().equals(282)) {
                loanMoney = loanMoney.add(val);
            }
            if (dto.getCode().equals(281)) {
                shouldPaySalary = shouldPaySalary.add(val);
            }
            if ("0002".equals(companyId) && dto.getCode().equals(41001)) {
                shouldPaySalary = shouldPaySalary.add(val);
            }
            if (shouldPayCodeList.contains(dto.getParentCode())) {
                if (dto.getIsPlus() == IsEnum.YES.getValue()) {
                    shouldPaySalary = shouldPaySalary.add(val);
                } else if (dto.getIsPlus() == IsEnum.NO.getValue()) {
                    shouldPaySalary = shouldPaySalary.subtract(val);
                }
            }
        }
        total.setShouldPaySalary(shouldPaySalary);
        total.setProxyPaySalary(proxyPaySalary);
        total.setSpecialTaxSalary(specialTaxSalary);
        total.setTaxAfterPaySalary(taxAfterPaySalary);
        total.setTaxSpecialGrandTotal(taxSpecialGrandTotal);
        total.setOtherNoTaxDeductions(otherNoTaxDeductions);
        total.setTotalloanMoney(loanMoney);
        return total;
    }

    private static BigDecimal safeParseDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
