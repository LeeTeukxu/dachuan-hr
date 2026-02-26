package com.tianye.hrsystem.entity.vo;

import lombok.Data;

/**
 * 有夜班补贴的员工以及次数
 */
@Data
public class HrmEmpNightShiftSubsidyVo
{
    private Long employeeId;

    /**
     * 符合条件的夜班次数
     */
    private Integer nightShiftDays;
}
