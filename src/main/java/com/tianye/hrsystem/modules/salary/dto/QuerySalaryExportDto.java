package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySalaryExportDto  {

    @ApiModelProperty("薪资记录id")
    private Long salaryRecordId;

    private Long employeeId;

    private Long deptId;

    private Integer type;

    private String employeeName;

    @Override
    public String toString() {
        return "QuerySalaryPageListBO{" +
                "sRecordId=" + salaryRecordId +
                ", employeeId=" + employeeId +
                ", deptId=" + deptId +
                ", type=" + type +
                ", employeeName='" + employeeName + '\'' +
                '}';
    }
}
