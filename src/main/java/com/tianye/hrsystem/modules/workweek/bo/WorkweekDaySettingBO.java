package com.tianye.hrsystem.modules.workweek.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkweekDaySettingBO {

    @ApiModelProperty(value = "日期 yyyy-MM-dd")
    private String workDate;

    @ApiModelProperty(value = "日期类型 1上班 2休息")
    private Integer dayType;
}
