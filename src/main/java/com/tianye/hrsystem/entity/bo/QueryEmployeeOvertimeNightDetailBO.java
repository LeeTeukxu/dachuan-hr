package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryEmployeeOvertimeNightDetailBO {

    @ApiModelProperty("员工ID")
    private Long employeeId;

    @ApiModelProperty("统计月份，格式：YYYY-MM")
    private String month;
}
