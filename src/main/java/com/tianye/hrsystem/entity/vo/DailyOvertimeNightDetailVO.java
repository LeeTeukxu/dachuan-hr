package com.tianye.hrsystem.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DailyOvertimeNightDetailVO {

    @ApiModelProperty("日期")
    private String workDate;

    @ApiModelProperty("加班小时")
    private BigDecimal overtimeHours;

    @ApiModelProperty("夜班次数")
    private Integer nightShiftCount;
}
