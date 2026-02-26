package com.tianye.hrsystem.modules.holiday.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 假期抵扣
 * </p>
 *
 * @author jiangyongming
 * @since 2024-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_holiday_deduction")
@ApiModel(value = "HrmHolidayDeduction", description = "假期抵扣")
public class HrmHolidayDeduction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "deduction_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deductionId;

    @ApiModelProperty(value = "假期余额ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long holidayId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "抵扣类型 1、迟到 2、早退 3、事假 4、病假 5、调休 6、缺卡补卡 7、年假")
    private Integer type;

    @ApiModelProperty(value = "是否更新假期余额 1、不更新 2、更新")
    private Integer update_status;


    @ApiModelProperty(value = "抵扣时间")
    private BigDecimal deductionTime;
}
