package com.tianye.hrsystem.modules.insurance.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryEmpInsuranceMonthBO extends MyPageEntity {

    @ApiModelProperty("年")
    private Integer year;

    @ApiModelProperty("月")
    private Integer month;

    @Override
    public String toString() {
        return "QueryEmpInsuranceMonthBO{" +
                "year=" + year +
                ", month=" + month +
                '}';
    }
}
