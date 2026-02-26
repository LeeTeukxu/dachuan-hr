package com.tianye.hrsystem.entity.vo;

import lombok.Data;

/**
 * 员工出勤天数
 */
@Data
public class EmployeeAttendanceDaysStaticVo
{
    private Long employeeId;

    /**
     * 应出勤天数
     */
    private Integer normalDays;

    /**
     * 实际出勤天数
     */
    private Integer actualityDays;
}
