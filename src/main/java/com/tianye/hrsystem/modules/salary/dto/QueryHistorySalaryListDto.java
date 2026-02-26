package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryHistorySalaryListDto extends MyPageEntity {

    @ApiModelProperty("年")
    private Integer year;

    @Override
    public String toString() {
        return "QueryHistorySalaryListBO{" +
                "year=" + year +
                '}';
    }
}
