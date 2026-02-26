package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.base.PageEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySalaryListBO extends PageEntity {

    private Long employeeId;

    @Override
    public String toString() {
        return "QuerySalaryListBO{" +
                "employeeId=" + employeeId +
                '}';
    }
}
