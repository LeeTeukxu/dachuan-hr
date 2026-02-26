package com.tianye.hrsystem.entity.bo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddInsuranceEmpBO {

    private List<Long> employeeIds;

    private Long iRecordId;

    @Override
    public String toString() {
        return "AddInsuranceEmpBO{" +
                "employeeIds=" + employeeIds +
                ", iRecordId=" + iRecordId +
                '}';
    }
}
