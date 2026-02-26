package com.tianye.hrsystem.modules.deduction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 假期累计
 * </p>
 *
 * @author jiangyongming
 * @since 2024-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_personal_income_tax")
@ApiModel(value = "HrmPersonalIncome对象", description = "个税累计")
public class HrmPersonalIncomeTax implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "personal_income_tax_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long personalIncomeTaxId;

    @ApiModelProperty(value = "员工id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    @ApiModelProperty(value = "累计收入额(截止上月)")
    private BigDecimal AccumulatedIncome;

    @ApiModelProperty(value = "累计减除费用(截止上月)")
    private BigDecimal AccumulatedDeductionOfExpenses;

    @ApiModelProperty(value = "累计公积金社保扣除(截止上月)")
    private BigDecimal AccumulatedProvidentFund;

    @ApiModelProperty(value = "累计已缴税额")
    private BigDecimal AccumulatedTaxPayment;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "截止月份")
    private Integer endMonth;
}
