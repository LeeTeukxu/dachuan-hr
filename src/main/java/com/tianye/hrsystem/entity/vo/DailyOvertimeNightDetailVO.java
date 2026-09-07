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

    /** 加班/夜班计算过程说明（悬浮展示） */
    private String calcProcess;

    public String getCalcProcess() {
        return calcProcess;
    }

    public void setCalcProcess(String calcProcess) {
        this.calcProcess = calcProcess;
    }
}
