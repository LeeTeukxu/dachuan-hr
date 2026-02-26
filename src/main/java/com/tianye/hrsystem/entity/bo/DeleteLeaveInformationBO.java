package com.tianye.hrsystem.entity.bo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteLeaveInformationBO {

    private Long employeeId;

    private String remarks;

    @Override
    public String toString() {
        return "DeleteLeaveInformationBO{" +
                "employeeId=" + employeeId +
                ", remarks='" + remarks + '\'' +
                '}';
    }
}
