package com.tianye.hrsystem.entity.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryMonthAttendanceVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "summary_id")
    private Long summaryId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "员工ID")
    private Long employeeId;

    @ApiModelProperty(value = "员工名称")
    private String employeeName;

    @ApiModelProperty(value = "部门类型 1、行政部门 2、生产部门")
    private Integer department;

    @ApiModelProperty(value = "实际出勤天数")
    private BigDecimal actualAttendance;

    @ApiModelProperty(value = "加班/小时")
    private BigDecimal workOverTime;

    @ApiModelProperty(value = "空班/次")
    private Integer emptyClass;

    @ApiModelProperty(value = "中班/次")
    private Integer middleClass;

    @ApiModelProperty(value = "夜班/次")
    private Integer nightShift;

    @ApiModelProperty(value = "夜班补贴")
    private BigDecimal nightSubsidy;

    @ApiModelProperty(value = "当月休假/天")
    private Integer currentMonthVacation;

    @ApiModelProperty(value = "借款")
    private BigDecimal loan;

    @ApiModelProperty(value = "其它补贴")
    private Integer otherSubsidies;

    @ApiModelProperty(value = "其它扣款")
    private BigDecimal otherDeductions;

    @ApiModelProperty(value = "备注")
    private String remark;
}
