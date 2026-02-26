package com.tianye.hrsystem.modules.salary.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ApiModel("每月员工薪资项表")
public class SalaryMonthOptionValueDto {

    private Long id;

    @ApiModelProperty("sEmpRecordId")
    private Long sEmpRecordId;

    @ApiModelProperty("code")
    private Integer code;

    @ApiModelProperty("value")
    private BigDecimal value;

    @ApiModelProperty("createUserId")
    private Long createUserId;

    @ApiModelProperty("createTime")
    private LocalDateTime createTime;

    @ApiModelProperty("updateUserId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUserId;

    @ApiModelProperty("updateTime")
    private LocalDateTime updateTime;

}
