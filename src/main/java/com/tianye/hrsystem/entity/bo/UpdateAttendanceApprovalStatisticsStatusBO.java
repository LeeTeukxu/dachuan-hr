package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修改审批统计状态BO")
public class UpdateAttendanceApprovalStatisticsStatusBO {

    @ApiModelProperty("审批实例ID")
    private String approvalId;

    @ApiModelProperty("统计状态：空=参与统计，取消至统计=不参与统计")
    private String statisticsStatus;
}
