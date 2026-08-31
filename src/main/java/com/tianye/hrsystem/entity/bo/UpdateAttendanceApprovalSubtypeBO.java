package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修改审批子类型BO")
public class UpdateAttendanceApprovalSubtypeBO {

    @ApiModelProperty("审批实例ID")
    private String approvalId;

    @ApiModelProperty("审批子类型")
    private String subType;
}
