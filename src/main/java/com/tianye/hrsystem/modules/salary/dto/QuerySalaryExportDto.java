package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuerySalaryExportDto  {

    @ApiModelProperty("薪资记录id")
    private Long salaryRecordId;

    private Long employeeId;

    private Long deptId;

    private Integer type;

    private String employeeName;

    @ApiModelProperty("员工id集合")
    private List<Long> employeeIds;

    @Override
    public String toString() {
        return "QuerySalaryPageListBO{" +
                "sRecordId=" + salaryRecordId +
                ", employeeId=" + employeeId +
                ", deptId=" + deptId +
                ", type=" + type +
                ", employeeName='" + employeeName + '\'' +
                ", employeeIds=" + employeeIds +
                '}';
    }
}
