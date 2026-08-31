package com.tianye.hrsystem.modules.workweek.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkweekSettingBO {

    @ApiModelProperty(value = "年份", required = true)
    private Integer year;

    @ApiModelProperty(value = "周次", required = true)
    private Integer weekNo;

    @ApiModelProperty(value = "周类型 1单休 2双休", required = true)
    private Integer weekType;

    @ApiModelProperty(value = "是否从本周开始影响后续单双休，默认 true")
    private Boolean recalculateFollowing;
}
