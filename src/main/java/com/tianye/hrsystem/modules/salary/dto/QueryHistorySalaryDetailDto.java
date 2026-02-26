package com.tianye.hrsystem.modules.salary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tianye.hrsystem.common.MyPageEntity;
import com.tianye.hrsystem.base.PageEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryHistorySalaryDetailDto extends PageEntity {

    @JsonProperty("sRecordId")
    private Long sRecordId;

    private String employeeName;

    private String jobNumber;

    private Long deptId;

    @Override
    public String toString() {
        return "QueryHistorySalaryDetailBO{" +
                "sRecordId=" + sRecordId +
                ", employeeName='" + employeeName + '\'' +
                ", jobNumber='" + jobNumber + '\'' +
                ", deptId=" + deptId +
                '}';
    }
}
