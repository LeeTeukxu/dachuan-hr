package com.tianye.hrsystem.modules.holiday.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryRemainingVacationVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "holiday_id")
    private Long holidayId;

    @ApiModelProperty(value = "累计剩余假期")
    private BigDecimal remainingVacation;

    @ApiModelProperty(value = "员工id")
    private Long employeeId;

    @ApiModelProperty(value = "员工名称")
    private String employeeName;
}
