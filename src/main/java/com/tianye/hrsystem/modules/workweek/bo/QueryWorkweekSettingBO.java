package com.tianye.hrsystem.modules.workweek.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryWorkweekSettingBO {

    @ApiModelProperty(value = "年份", required = true)
    private Integer year;
}
