package com.tianye.hrsystem.modules.salary.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class QuerySalaryMonthRecordVO {
    @ApiModelProperty(value = "主键id")
    @TableId(value = "s_record_id")
    private Long sRecordId;

    @ApiModelProperty(value = "报表标题")
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

    @ApiModelProperty("状态  0待审核、1通过、2拒绝、3审核中(财务已审核) 4:撤回 5 新创建,薪资未生成  10 历史薪资 11核算完成 12 员工确认完成")
    private Integer checkStatus;
}
