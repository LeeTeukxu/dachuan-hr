package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修改审批时长BO")
public class UpdateAttendanceApprovalDurationBO {

    @ApiModelProperty("审批实例ID")
    private String approvalId;

    @ApiModelProperty("时长")
    private String duration;

    @ApiModelProperty("时长单位")
    private String durationUnit;
}
