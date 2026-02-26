package com.tianye.hrsystem.entity.vo;

import lombok.Data;

/**
 * 员工请假,调休统计数据
 */
@Data
public class EmployeeLeaveStaticVo
{
    private Long employeeId;

    /**
     * 请假时长
     */
    private Integer holidayTime;

    /**
     * 时长单位 1小时  2天
     */
    private String timeUnit;

    /**
     * 请假类型
     */
    private String holidayType;
}
