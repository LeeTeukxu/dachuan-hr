package com.tianye.hrsystem.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class QueryAttendanceApprovalPageVO {

    @ApiModelProperty("审批实例ID")
    private String approvalId;

    @ApiModelProperty("员工ID")
    private Long employeeId;

    @ApiModelProperty("员工姓名")
    private String employeeName;

    @ApiModelProperty("手机号")
    private String mobile;

    @ApiModelProperty("工号")
    private String jobNumber;

    @ApiModelProperty("部门")
    private String deptName;

    @ApiModelProperty("岗位")
    private String post;

    @ApiModelProperty("审批类型")
    private String tagName;

    @ApiModelProperty("审批子类型")
    private String subType;

    @ApiModelProperty("审批业务类型")
    private Long bizType;

    @ApiModelProperty("开始时间")
    private Date beginTime;

    @ApiModelProperty("结束时间")
    private Date endTime;

    @ApiModelProperty("时长")
    private String duration;

    @ApiModelProperty("时长单位")
    private String durationUnit;

    @ApiModelProperty("时长(天)：与 duration(小时) 一致派生，恒 = duration(小时)/8")
    private String durationDay;

    @ApiModelProperty("申请日期")
    private Date workDate;

    @ApiModelProperty("同步入库时间")
    private Date createTime;

    @ApiModelProperty("统计状态：空=参与统计，取消至统计=不参与统计")
    private String statisticsStatus;
}
