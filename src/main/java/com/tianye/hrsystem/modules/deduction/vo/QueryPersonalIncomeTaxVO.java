package com.tianye.hrsystem.modules.deduction.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryPersonalIncomeTaxVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "personal_income_tax_id")
    private Long personalIncomeTaxId;

    @ApiModelProperty(value = "员工id")
    private Long employeeId;

    @ApiModelProperty(value = "员工")
    private String employeeName;

    @ApiModelProperty(value = "累计收入额(截止上月)")
    private BigDecimal AccumulatedIncome = new BigDecimal(0);

    @ApiModelProperty(value = "累计减除费用(截止上月)")
    private BigDecimal AccumulatedDeductionOfExpenses = new BigDecimal(0);

    @ApiModelProperty(value = "累计公积金社保扣除(截止上月)")
    private BigDecimal AccumulatedProvidentFund = new BigDecimal(0);

    @ApiModelProperty(value = "累计已缴税额")
    private BigDecimal AccumulatedTaxPayment = new BigDecimal(0);

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "截止月份")
    private Integer endMonth;
}
