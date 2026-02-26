package com.tianye.hrsystem.modules.salary.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySalaryMonthRecrodButtonStatusVO {

    @ApiModelProperty(value = "行政审核")
    private Boolean administrativeReview;

    @ApiModelProperty(value = "发送工资条")
    private Boolean sendPaySlip;

    @ApiModelProperty(value = "核算工资")
    private Boolean calculateSalary;

    @ApiModelProperty(value = "在线编辑")
    private Boolean onlineEditing;

    @ApiModelProperty(value = "财务审核")
    private Boolean financialRevie;

    @ApiModelProperty(value = "总经理审核")
    private Boolean generalManagerReview;


    @ApiModelProperty(value = "导入专项扣除累计数据")
    private Boolean importAdditionalDeduction;
}
