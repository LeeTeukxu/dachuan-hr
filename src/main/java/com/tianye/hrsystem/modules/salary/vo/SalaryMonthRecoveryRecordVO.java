package com.tianye.hrsystem.modules.salary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SalaryMonthRecoveryRecordVO {

    @ApiModelProperty("薪资月记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sRecordId;

    @ApiModelProperty("年份")
    private Integer year;

    @ApiModelProperty("月份")
    private Integer month;

    @ApiModelProperty("年月，YYYY-MM")
    private String yearMonth;

    @ApiModelProperty("报表标题")
    private String title;

    @ApiModelProperty("薪资月记录状态")
    private Integer checkStatus;

    @ApiModelProperty("薪资月记录状态名称")
    private String checkStatusName;

    @ApiModelProperty("是否已发送工资条")
    private Integer isSend;

    @ApiModelProperty("员工月薪资明细数量")
    private Long employeeRecordCount = 0L;

    @ApiModelProperty("工资条发放记录数量")
    private Long salarySlipRecordCount = 0L;

    @ApiModelProperty("工资条明细数量")
    private Long salarySlipCount = 0L;

    @ApiModelProperty("是否可自动恢复")
    private Boolean recoverable = false;

    @ApiModelProperty("预览说明")
    private String message;

    @ApiModelProperty("阻断原因")
    private List<String> blockingReasons = new ArrayList<>();
}
