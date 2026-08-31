package com.tianye.hrsystem.modules.workweek.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryWorkweekMonthCalendarBO {

    @ApiModelProperty(value = "年份")
    private Integer year;

    @ApiModelProperty(value = "月份 1-12")
    private Integer month;
}
