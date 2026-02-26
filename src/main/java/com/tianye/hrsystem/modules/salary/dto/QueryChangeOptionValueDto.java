package com.tianye.hrsystem.modules.salary.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryChangeOptionValueDto {

    private Long templateId;

    private Long employeeId;

    @Override
    public String toString() {
        return "QueryChangeOptionValueBO{" +
                "templateId=" + templateId +
                ", employeeId=" + employeeId +
                '}';
    }
}
