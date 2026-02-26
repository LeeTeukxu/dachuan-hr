package com.tianye.hrsystem.modules.holiday.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 假期累计
 * </p>
 *
 * @author jiangyongming
 * @since 2024-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_remaining_vacation")
@ApiModel(value = "HrmRemainingVacation对象", description = "假期累计")
public class HrmRemainingVacation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "holiday_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long holidayId;

    @ApiModelProperty(value = "累计剩余假期")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal remainingVacation;

    @ApiModelProperty(value = "员工id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    @ApiModelProperty(value = "员工名称")
    @JsonSerialize(using = ToStringSerializer.class)
    private String employeeName;

}
