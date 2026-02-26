package com.tianye.hrsystem.modules.salary.entity;

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
 * 每月薪资记录
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_salary_month_record")
@ApiModel(value = "HrmSalaryMonthRecord对象", description = "每月薪资记录")
public class HrmSalaryMonthRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "s_record_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sRecordId;

    @ApiModelProperty("报表标题")
    private String title;

    @ApiModelProperty(value = "年份")
    private Integer year;

    @ApiModelProperty(value = "月份")
    private Integer month;

    @ApiModelProperty(value = "计薪人数")
    private Integer num;

    @ApiModelProperty("计薪开始时间")
    private LocalDate startTime;

    @ApiModelProperty("计薪结束日期")
    private LocalDate endTime;

    @ApiModelProperty(value = "个人社保")
    private BigDecimal personalInsuranceAmount;

    @ApiModelProperty(value = "个人公积金")
    private BigDecimal personalProvidentFundAmount;

    @ApiModelProperty(value = "企业社保")
    private BigDecimal corporateInsuranceAmount;

    @ApiModelProperty(value = "企业公积金")
    private BigDecimal corporateProvidentFundAmount;

    @ApiModelProperty(value = "预计应发工资")
    private BigDecimal expectedPaySalary;

    @ApiModelProperty(value = "个人所得税")
    private BigDecimal personalTax;

    @ApiModelProperty(value = "预计实发工资")
    private BigDecimal realPaySalary;

    @ApiModelProperty("薪资项表头")
    private String optionHead;

    @ApiModelProperty("审批记录id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examineRecordId;
    @ApiModelProperty("状态  0待审核、1通过、2拒绝、3审核中(财务已审核) 4:撤回 5 新创建,薪资未生成  10 历史薪资 11核算完成 15 财务拒绝 12 员工全部同意 12 员工确认完成")
    private Integer checkStatus;

    @ApiModelProperty("是否发送短信 0、未发送 1、已发送")
    private Integer isSend;

    @ApiModelProperty("创建人Id")
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
