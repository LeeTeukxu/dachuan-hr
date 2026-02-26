package com.tianye.hrsystem.entity.bo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryInsuranceTypeBO {
    private String cityId;

    @Override
    public String toString() {
        return "QueryInsuranceTypeBO{" +
                "cityId='" + cityId + '\'' +
                '}';
    }
}
