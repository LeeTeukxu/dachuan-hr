package com.tianye.hrsystem.modules.workweek.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitWorkweekSettingBO {

    @ApiModelProperty(value = "年份", required = true)
    private Integer year;

    @ApiModelProperty(value = "第一周类型 1单休 2双休", required = true)
    private Integer firstWeekType;
}
