package com.tianye.hrsystem.entity.po;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 生产部员工考勤表
 * </p>
 *
 * @author jiangyongming
 * @since 2024-05-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_produce_attendance")
@ApiModel(value = "HrmProduceAttendance", description = "生产部员工考勤表")
public class HrmProduceAttendance implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键id")
    @TableId(value = "summary_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
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

    @ApiModelProperty(value = "试用期出勤天数")
    private BigDecimal probationAttendance;

    @ApiModelProperty(value = "转正后出勤天数")
    private BigDecimal positiveAttendance;

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
    private BigDecimal currentMonthVacation;

    @ApiModelProperty(value = "高温津贴")
    private BigDecimal highTemperature;

    @ApiModelProperty(value = "低温津贴")
    private BigDecimal lowTemperature;

    @ApiModelProperty(value = "借款")
    private BigDecimal loan;

    @ApiModelProperty(value = "其它补贴")
    private BigDecimal otherSubsidies;

    @ApiModelProperty(value = "其它扣款")
    private BigDecimal otherDeductions;

    @ApiModelProperty(value = "福利")
    private String welfare;

    @ApiModelProperty(value = "备注")
    private String remark;
}
