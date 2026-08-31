package com.tianye.hrsystem.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class EmployeeOvertimeNightMonthlyDetailVO {

    @ApiModelProperty("统计月份")
    private String month;

    @ApiModelProperty("加班小时")
    private BigDecimal overtimeHours;

    @ApiModelProperty("夜班次数")
    private Integer nightShiftCount;

    @ApiModelProperty("应出勤天数")
    private Integer expectedAttendanceDays;

    @ApiModelProperty("应出勤小时")
    private BigDecimal expectedAttendanceHours;

    @ApiModelProperty("实际出勤天数")
    private Integer actualAttendanceDays;

    @ApiModelProperty("实际出勤小时")
    private BigDecimal actualAttendanceHours;

    @ApiModelProperty("应计出勤小时")
    private BigDecimal accruedAttendanceHours;

    @ApiModelProperty("实际出勤备注")
    private String actualAttendanceRemark;

    @ApiModelProperty("每日明细")
    private List<DailyOvertimeNightDetailVO> dailyDetails;
}
