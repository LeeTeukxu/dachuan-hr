package com.tianye.hrsystem.modules.insurance.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryInsuranceRecordListBO extends MyPageEntity {
    @ApiModelProperty("年份")
    private Integer year;

    @Override
    public String toString() {
        return "QueryInsuranceRecordListBO{" +
                "year=" + year +
                '}';
    }
}
