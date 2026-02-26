package com.tianye.hrsystem.modules.salary.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryBaseTotal
{
    /**
     * 扣代缴项总金额(个人社保+个人公积金等)
     */
    BigDecimal proxyPaySalary = new BigDecimal(0);
    /**
     * 应发工资金额
     */
    BigDecimal shouldPaySalary =new BigDecimal(0);
    /**
     * 税后补发-税后补扣
     */
    BigDecimal taxAfterPaySalary =new BigDecimal(0);

    /**
     * 个税专项附加扣除累计
     */
    BigDecimal taxSpecialGrandTotal = new BigDecimal(0);

    /**
     * 特殊计税项
     */
    BigDecimal specialTaxSalary = new BigDecimal(0);

    /**
     * 其他扣款
     */
    BigDecimal otherNoTaxDeductions = new BigDecimal(0);

    /**
     * 借款
     */
    BigDecimal totalloanMoney = new BigDecimal(0);

    /**
     * 奖金
     */
    BigDecimal bonusSalary = new BigDecimal(0);

}
