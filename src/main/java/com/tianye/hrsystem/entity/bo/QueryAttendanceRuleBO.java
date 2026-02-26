package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("查询打卡规则")
public class QueryAttendanceRuleBO {

    @ApiModelProperty(value = "打卡规则id")
    private Long attendanceRuleId;

    @ApiModelProperty(value = "打卡规则名称")
    private String attendanceRuleName;
}
