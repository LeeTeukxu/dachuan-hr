package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 员工批量设置
 */
@Getter
@Setter
public class EmployeeBatchSettingBO {

    @ApiModelProperty("字段名(白名单: fullAttendance/expandProduction/isDisabled/isContinuousShift/isRetiredSoldier/isPartyMember/personnelCategory/isRemark/affiliationSystem/restType)")
    private String fieldName;

    @ApiModelProperty("字段值")
    private Integer fieldValue;

    @ApiModelProperty("员工id列表")
    private List<Long> employeeIds;
}
