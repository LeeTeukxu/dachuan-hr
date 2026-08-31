package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("审批数据月份操作BO")
public class AttendanceApprovalMonthBO {

    @ApiModelProperty("月份，格式yyyy-MM")
    private String month;

    @ApiModelProperty("员工ID列表，空表示全部员工")
    private List<Long> employeeIds;

    @ApiModelProperty("审批类型列表，可选值：all/overtime/misscard/leave/travel")
    private List<String> approvalTypes;
}
