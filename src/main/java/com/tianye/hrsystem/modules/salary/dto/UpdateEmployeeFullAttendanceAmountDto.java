package com.tianye.hrsystem.modules.salary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateEmployeeFullAttendanceAmountDto {

    @ApiModelProperty("金额类型：ordinary=普通员工全勤金额，leader=领导全勤金额")
    private String amountType;

    @ApiModelProperty("全勤金额")
    private BigDecimal amount;

    @ApiModelProperty("员工ID列表；为空表示更新全体未删除员工")
    private List<Long> employeeIds;
}
