package com.tianye.hrsystem.entity.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdministrativeAttendanceReportMetricVO {
    private Long employeeId;
    private BigDecimal absenteeismDays;
    private BigDecimal lateCount;
    private BigDecimal earlyCount;
    private BigDecimal onDutyMissingCardCount;
    private BigDecimal offDutyMissingCardCount;
}
