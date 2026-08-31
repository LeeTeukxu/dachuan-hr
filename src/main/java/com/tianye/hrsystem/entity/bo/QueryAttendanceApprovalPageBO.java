package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@Data
@ApiModel("查询审批数据BO")
@EqualsAndHashCode(callSuper = false)
public class QueryAttendanceApprovalPageBO extends PageEntity {

    @ApiModelProperty("姓名/工号关键字")
    private String search;

    @ApiModelProperty("员工ID")
    private Long employeeId;

    @ApiModelProperty("部门ID列表")
    private List<Long> deptIds;

    @ApiModelProperty("审批业务类型列表，1=加班，2=出差/外出，3=请假")
    private List<Long> bizTypes;

    @ApiModelProperty("审批类型列表，例如补卡、请假、出差、外出、加班")
    private List<String> tagNames;

    @ApiModelProperty("审批子类型列表，例如事假、调休、病假、婚假、丧假")
    private List<String> subTypes;

    @ApiModelProperty("查询日期范围")
    private List<LocalDate> times;
}
