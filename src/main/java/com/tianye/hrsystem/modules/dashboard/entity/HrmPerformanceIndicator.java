package com.tianye.hrsystem.modules.dashboard.entity;

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
import java.time.LocalDateTime;

/**
 * 绩效考核月度指标
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_performance_indicator")
@ApiModel(value = "HrmPerformanceIndicator对象", description = "绩效考核月度指标")
public class HrmPerformanceIndicator implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("部门id，NULL表示全公司")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @ApiModelProperty("统计年份")
    private Integer statYear;

    @ApiModelProperty("统计月份1-12")
    private Integer statMonth;

    @ApiModelProperty("指标类型 1安全 2质量 3营收 4成本 5产能")
    private Integer indicatorType;

    @ApiModelProperty("目标值")
    private BigDecimal targetValue;

    @ApiModelProperty("实际值")
    private BigDecimal actualValue;

    @ApiModelProperty("备注")
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
