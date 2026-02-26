package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.tianye.hrsystem.entity.vo.HrmEmployeeVO;
import com.tianye.hrsystem.enums.EmployeeStatusEnum;
import com.tianye.hrsystem.enums.IsEnum;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryComputeService_Bak
{

    private static RangeMap<BigDecimal, TaxEntity> taxRateRangeMap = TreeRangeMap.create();

    //税率与速算
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


    public SalaryBaseTotal baseComputeSalary(HrmSalaryMonthEmpRecord salaryMonthEmpRecord)
    {
        SalaryBaseTotal salaryBaseTotal = new SalaryBaseTotal();
        //所有工资项
        List<ComputeSalaryDto> computeSalaryDTOS = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(salaryMonthEmpRecord.getSEmpRecordId());
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
            if (computeSalaryDTO.getParentCode().equals(100)) {
                proxyPaySalary = proxyPaySalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getParentCode().equals(170)) {
                specialTaxSalary = specialTaxSalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getParentCode().equals(150)) {
                taxAfterPaySalary = taxAfterPaySalary.add(new BigDecimal(computeSalaryDTO.getValue()));
            }
            if (computeSalaryDTO.getParentCode().equals(160)) {
                taxAfterPaySalary = taxAfterPaySalary.subtract(new BigDecimal(computeSalaryDTO.getValue()));
            }
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

            //计算应发工资(应发工资=员工工资总额-请假扣款-考勤扣款;代扣代缴不需要计算:parentCode=90;企业社保不需要计算:parentCode=110;企业公积金不需要计算:parentCode=110)
            if (shouldPayCodeList.contains(computeSalaryDTO.getParentCode())) {
                if (computeSalaryDTO.getIsPlus() == IsEnum.YES.getValue()) {
                    shouldPaySalary = shouldPaySalary.add(new BigDecimal(computeSalaryDTO.getValue()));
                } else if (computeSalaryDTO.getIsPlus() == IsEnum.NO.getValue()) {
                    shouldPaySalary = shouldPaySalary.subtract(new BigDecimal(computeSalaryDTO.getValue()));
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
                                                         Map<Integer, String> cumulativeTaxOfLastMonthData, HrmEmployeeVO hrmEmployeeVO) {
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
        if (CollUtil.isNotEmpty(lastTaxOptionValueList) && cumulativeTaxOfLastMonthData == null) {
            //如果是更新薪资(有个税累计数据),则查询本月生成的上月个税累计信息
            lastTaxOptionValueMap = lastTaxOptionValueList.stream().collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, HrmSalaryMonthOptionValue::getValue));
        } else {
            List<HrmSalaryMonthOptionValue> lastTaxOptionValueList1;
            //(没有个税累计数据，查询员工上个月薪资数据)，本月个税累计对应上月个税累计code
            Optional<HrmSalaryMonthEmpRecord> lastSalaryMonthEmpRecordOpt = salaryMonthEmpRecordService.lambdaQuery().eq(HrmSalaryMonthEmpRecord::getYear, salaryMonthEmpRecord.getYear())
                    .eq(HrmSalaryMonthEmpRecord::getMonth, salaryMonthEmpRecord.getMonth() - 1).eq(HrmSalaryMonthEmpRecord::getEmployeeId, salaryMonthEmpRecord.getEmployeeId()).oneOpt();
            //算出 上月个税累计信息
            if (lastSalaryMonthEmpRecordOpt.isPresent()) {
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
            if (CollUtil.isNotEmpty(cumulativeTaxOfLastMonthData)) {
                //是否导入上月个税累计,导入需覆盖
                cumulativeTaxOfLastMonthData.forEach(lastTaxOptionValueMap::put);
            }
        }
        //工会费
        BigDecimal labourunionPay = new BigDecimal(0);
        //工会费 正式工工会费按应发工资的千分之五收取
        if(hrmEmployeeVO!=null && EmployeeStatusEnum.OFFICIAL.getValue()==hrmEmployeeVO.getStatus() && salaryBaseTotal.getShouldPaySalary().compareTo(new BigDecimal(0))>0)
        {
            labourunionPay = salaryBaseTotal.getShouldPaySalary().multiply(new BigDecimal(0.005)).setScale(0, BigDecimal.ROUND_HALF_UP);
        }
        //个税累计信息
        //累计收入额 同一个纳税年度内，员工在该企业累计至当前月份的收入额（收入额=应发工资-税后补发+其它）code=270101
        //下面在做累计操作
        BigDecimal cumulativeIncome = new BigDecimal(lastTaxOptionValueMap.get(250101)).add(salaryBaseTotal.getShouldPaySalary());
        //累计减除费用 同一个纳税年度内，员工截至当前月（计薪月）在本单位的任职受雇从业月份数*5000  code=270102
        BigDecimal cumulativeDeductions = new BigDecimal(lastTaxOptionValueMap.get(250102)).add(new BigDecimal(5000));
        //累计专项扣除 同一个纳税年度内，员工在该企业累计至当前月份的个人社保、个人公积金等费用 code=270103
        BigDecimal cumulativeSpecialDeduction = new BigDecimal(lastTaxOptionValueMap.get(250103)).add(salaryBaseTotal.getProxyPaySalary());
        //累计专项附加扣除 同一个纳税年度内，员工在该企业累计至当前月份的五项专项附加扣除合计 code=270104
        BigDecimal cumulativeSpecialAdditionalDeduction = salaryBaseTotal.getTaxSpecialGrandTotal();

        //累计应纳税所得额 同一个纳税年度内，员工在该企业累计至当前月份的应税工资 code=270105
        //累计应纳税所得额 = 累计收入-累计免税收入（工资项为加项，且不参与计税的工资）-累计基本减除费用-累计专项扣除-累计专项附加扣除-累计依法确定的其他扣除（标红的先不用考虑），
        BigDecimal cumulativeTaxableIncome = cumulativeIncome.subtract(cumulativeDeductions).subtract(cumulativeSpecialDeduction).subtract(cumulativeSpecialAdditionalDeduction);
        //累计应纳税额 同一个纳税年度内，员工在该企业累计至当前月份的累计应缴个税 code=270106
        TaxEntity taxEntity = taxRateRangeMap.get(cumulativeTaxableIncome);
        BigDecimal cumulativeTaxPayable = cumulativeTaxableIncome.multiply(new BigDecimal(taxEntity.getTaxRate())).divide(new BigDecimal(100), 2, BigDecimal.ROUND_UP)
                .subtract(new BigDecimal(taxEntity.getQuickDeduction())).setScale(2, BigDecimal.ROUND_HALF_UP);
        ;
        //当月个人所得税 = 累计应纳税额 - 累计已预交税额
        BigDecimal payTaxSalary = cumulativeTaxPayable.subtract(new BigDecimal(lastTaxOptionValueMap.get(250105)));
        if(payTaxSalary.compareTo(new BigDecimal(0))<0)
        {
            payTaxSalary = new BigDecimal(0);
        }
        BigDecimal realPaySalary = new BigDecimal(0);
        //实发工资 = 应发工资 - 扣代缴项总金额 - 个人所得税 + 税后补发 - 税后补扣 -工会费 -其他扣款 -借款  code=240101
        if(salaryBaseTotal.getShouldPaySalary().compareTo(new BigDecimal(0))>0)
        {
            realPaySalary = salaryBaseTotal.getShouldPaySalary().subtract(salaryBaseTotal.getProxyPaySalary()).
                    subtract(payTaxSalary).add(salaryBaseTotal.getTaxAfterPaySalary()).subtract(labourunionPay).
                    subtract(salaryBaseTotal.getOtherNoTaxDeductions()).subtract(salaryBaseTotal.getTotalloanMoney());
        }
        //210101	210	应发工资
        //220101	220	应税工资
        //230101	230	个人所得税
        //240101	240	实发工资
        Map<Integer, String> codeValueMap = new HashMap<>();
        codeValueMap.put(210101, salaryBaseTotal.getShouldPaySalary().toString());
        codeValueMap.put(220101, shouldTaxSalary.toString());
        codeValueMap.put(230101, payTaxSalary.toString());
        codeValueMap.put(240101, realPaySalary.toString());
        //本月对应上月个税累计信息(parentCode=250)
        lastTaxOptionValueMap.forEach(codeValueMap::put);
        //个税累计信息
        codeValueMap.put(270101, cumulativeIncome.toString());
        codeValueMap.put(270102, cumulativeDeductions.toString());
        codeValueMap.put(270103, cumulativeSpecialDeduction.toString());
        codeValueMap.put(270104, cumulativeSpecialAdditionalDeduction.toString());
        codeValueMap.put(270105, cumulativeTaxableIncome.toString());
        codeValueMap.put(270106, cumulativeTaxPayable.toString());
        //工会费用
        codeValueMap.put(160102, labourunionPay.toString());

        //代扣小计  公积金+社保+工会费+个人借款+其他+个税
        BigDecimal daiKouXiaoJi = salaryBaseTotal.getProxyPaySalary().add(labourunionPay).add(salaryBaseTotal.getTotalloanMoney()).add(salaryBaseTotal.getOtherNoTaxDeductions()).add(payTaxSalary);
        codeValueMap.put(1001, daiKouXiaoJi.toString());




        //删除表hrm_salary_month_option_value里面之前指定工资项的数据
        salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(
                210101, 220101, 230101, 240101,
                250101, 250102, 250103, 250105,
                270101, 270102, 270103, 270104, 270105, 270106,160102,1001))
                .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
        //构建新的工资项数据
        codeValueMap.forEach((code, value) -> {
            HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
            salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
            salaryMonthOptionValue.setCode(code);
            salaryMonthOptionValue.setValue(value);
            salaryMonthOptionValueList.add(salaryMonthOptionValue);
        });
        return salaryMonthOptionValueList;
    }
}
