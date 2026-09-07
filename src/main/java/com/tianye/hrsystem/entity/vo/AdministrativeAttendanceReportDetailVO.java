package com.tianye.hrsystem.entity.vo;

import lombok.Data;

import java.util.Date;

/**
 * 行政体系考勤导出批注用：按天的考勤异常明细（旷工/迟到/早退/缺卡）
 */
@Data
public class AdministrativeAttendanceReportDetailVO {
    private Long employeeId;
    private Date workDate;
    private String fieldName;
}
