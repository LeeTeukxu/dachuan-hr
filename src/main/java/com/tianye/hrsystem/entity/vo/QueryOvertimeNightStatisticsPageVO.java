package com.tianye.hrsystem.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryOvertimeNightStatisticsPageVO {

    @ApiModelProperty("员工ID")
    private Long employeeId;

    @ApiModelProperty("姓名")
    private String employeeName;

    @ApiModelProperty("工号")
    private String jobNumber;

    @ApiModelProperty("部门")
    private String deptName;

    @ApiModelProperty("统计月份")
    private String month;

    @ApiModelProperty("加班小时")
    private BigDecimal overtimeHours;

    @ApiModelProperty("可计入实际/应计出勤的加班小时")
    private BigDecimal attendanceOvertimeHours;

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
}
