package com.tianye.hrsystem.modules.additional.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryEmployeeAdditionalVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "employee_additional_id")
    private Long employeeEdditionalId;

    @ApiModelProperty(value = "员工id")
    private Long employeeId;

    @ApiModelProperty(value = "员工名称")
    private String employeeName;

    @ApiModelProperty(value = "子女教育")
    private BigDecimal childrenEducation;

    @ApiModelProperty(value = "住房租金")
    private BigDecimal housingRent;

    @ApiModelProperty(value = "住房贷款利息")
    private BigDecimal housingLoanInterest;

    @ApiModelProperty(value = "赡养老人")
    private BigDecimal supportingTheElderly;

    @ApiModelProperty(value = "继续教育")
    private BigDecimal continuingEducation;

    @ApiModelProperty(value = "养幼女")
    private BigDecimal raisingGirls;

    @ApiModelProperty(value = "年")
    private Integer year;
}
