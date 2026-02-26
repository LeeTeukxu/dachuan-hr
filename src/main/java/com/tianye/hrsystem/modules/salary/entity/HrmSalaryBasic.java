package com.tianye.hrsystem.modules.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;

/**
 * 基本工资和加班夜班补贴表(HrmSalaryBasic)实体类
 *
 * @author makejava
 * @since 2024-04-06 12:31:35
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_salary_basic")
@ApiModel(value = "HrmSalaryBasic对象", description = "基本工资和加班夜班补贴表")
public class HrmSalaryBasic implements Serializable {
    private static final long serialVersionUID = 813094765220855554L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long Id;

    @ApiModelProperty(value = "部门ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @ApiModelProperty(value = "基本工资")
    private BigDecimal salaryBasic;

    @ApiModelProperty(value = "加班费")
    private BigDecimal overtimePay;

    @ApiModelProperty(value = "夜班补贴")
    private BigDecimal subsidy;

    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUserId;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}

