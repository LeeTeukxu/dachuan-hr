package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateOvertimeNightAttendanceBO {

    @ApiModelProperty("员工ID")
    private Long employeeId;

    @ApiModelProperty("统计月份，格式：YYYY-MM")
    private String month;

    @ApiModelProperty("应出勤小时")
    private BigDecimal expectedAttendanceHours;

    @ApiModelProperty("实际出勤小时")
    private BigDecimal actualAttendanceHours;

    @ApiModelProperty("应计出勤小时")
    private BigDecimal accruedAttendanceHours;
}
