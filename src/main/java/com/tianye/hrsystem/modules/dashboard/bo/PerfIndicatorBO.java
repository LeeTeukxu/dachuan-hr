package com.tianye.hrsystem.modules.dashboard.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 绩效指标录入行
 */
@Getter
@Setter
public class PerfIndicatorBO {

    @ApiModelProperty("主键，编辑时传")
    private Long id;

    @ApiModelProperty("部门id，空表示全公司")
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
}
