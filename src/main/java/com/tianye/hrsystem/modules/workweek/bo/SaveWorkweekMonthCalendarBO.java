package com.tianye.hrsystem.modules.workweek.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaveWorkweekMonthCalendarBO {

    @ApiModelProperty(value = "年份")
    private Integer year;

    @ApiModelProperty(value = "月份 1-12")
    private Integer month;

    @ApiModelProperty(value = "当月每日上班/休息设置")
    private List<WorkweekDaySettingBO> days = new ArrayList<>();
}
