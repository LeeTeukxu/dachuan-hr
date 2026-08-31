package com.tianye.hrsystem.entity.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@ApiModel("手工添加审批数据BO")
public class AddAttendanceApprovalBO {

    @ApiModelProperty("员工ID列表")
    private List<Long> employeeIds;

    @ApiModelProperty("审批类型，例如补卡、请假、加班、出差、外出")
    private String tagName;

    @ApiModelProperty("审批子类型，例如事假、调休、病假")
    private String subType;

    @ApiModelProperty("手工合计时长，单位为durationUnit")
    private String duration;

    @ApiModelProperty("手工合计时长单位，当前前端按小时提交")
    private String durationUnit;

    @ApiModelProperty("开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date beginTime;

    @ApiModelProperty("结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    @ApiModelProperty("审批时间段列表，用于一次添加多个非连续日期")
    private List<ApprovalRangeBO> approvalRanges;

    @Data
    @ApiModel("手工添加审批数据时间段BO")
    public static class ApprovalRangeBO {

        @ApiModelProperty("开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date beginTime;

        @ApiModelProperty("结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date endTime;
    }
}
