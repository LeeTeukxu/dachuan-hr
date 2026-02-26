package com.tianye.hrsystem.modules.additional.entity;

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
 * 假期累计
 * </p>
 *
 * @author jiangyongming
 * @since 2024-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_additional")
@ApiModel(value = "HrmAdditional对象", description = "专项附加扣除")
public class HrmAdditional implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "additional_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long additionalId;

    @ApiModelProperty(value = "员工id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    @ApiModelProperty(value = "子女教育")
    private BigDecimal childrenEducation;

    @ApiModelProperty(value = "住房租金")
    private BigDecimal housingRent;

    @ApiModelProperty(value = "住房贷款利息")
    private BigDecimal housingLoanInterest;

    @ApiModelProperty(value = "赡养老人")
    private BigDecimal supportingTheElderly;

    @ApiModelProperty(value = "继续教育")
    private BigDecimal continuingEducation;

    @ApiModelProperty(value = "养幼女")
    private BigDecimal raisingGirls;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;
}
