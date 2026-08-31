package com.tianye.hrsystem.modules.salary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SalaryMonthRecoveryDto {

    @ApiModelProperty("目标恢复年份")
    private Integer year;

    @ApiModelProperty("目标恢复月份")
    private Integer month;
}
