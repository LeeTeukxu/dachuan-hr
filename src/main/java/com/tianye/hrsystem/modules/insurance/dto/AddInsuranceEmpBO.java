package com.tianye.hrsystem.modules.insurance.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddInsuranceEmpBO {

    private List<Long> employeeIds;

    private Long recordId;

    @Override
    public String toString() {
        return "AddInsuranceEmpBO{" +
                "employeeIds=" + employeeIds +
                ", iRecordId=" + recordId +
                '}';
    }
}
