package com.tianye.hrsystem.modules.deduction.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UpdatePersonalIncomeTaxBO {

    @ApiModelProperty("个税累计项")
    private List<Project> personalIncomeTaxValues;

    @Getter
    @Setter
    public static class Project {
        @ApiModelProperty("主键ID")
        private Long personalIncomeTaxId;

        @ApiModelProperty("员工id")
        private Long employeeId;

        @ApiModelProperty(value = "年")
        private Integer year;

        @ApiModelProperty(value = "截止月份")
        private Integer endMonth;

        @ApiModelProperty(value = "累计收入额(截止上月)")
        private BigDecimal AccumulatedIncome;

        @ApiModelProperty(value = "累计减除费用(截止上月)")
        private BigDecimal AccumulatedDeductionOfExpenses;

        @ApiModelProperty(value = "累计公积金社保扣除(截止上月)")
        private BigDecimal AccumulatedProvidentFund;

        @ApiModelProperty(value = "累计已缴税额")
        private BigDecimal AccumulatedTaxPayment;

    }
}
