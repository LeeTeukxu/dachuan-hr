package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("删除审批数据BO")
public class DeleteAttendanceApprovalBO {

    @ApiModelProperty("审批实例ID")
    private String approvalId;
}
