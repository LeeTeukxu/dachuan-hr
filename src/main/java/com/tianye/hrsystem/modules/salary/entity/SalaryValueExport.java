package com.tianye.hrsystem.modules.salary.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public  class SalaryValueExport {
    @ApiModelProperty("薪资项id")
    private Long id;
    @ApiModelProperty("薪资项code")
    private Integer code;
    @ApiModelProperty("值")
    private String value;
    @ApiModelProperty("是否固定")
    private Integer isFixed;
    @ApiModelProperty("薪资项名称")
    private String name;
}
