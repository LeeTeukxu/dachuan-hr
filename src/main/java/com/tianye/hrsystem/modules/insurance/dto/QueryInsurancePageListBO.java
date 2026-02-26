package com.tianye.hrsystem.modules.insurance.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryInsurancePageListBO extends MyPageEntity {

    @ApiModelProperty("每月社保记录id")
    private Long recordId;

    @ApiModelProperty("员工名称")
    private String employeeName;

    @ApiModelProperty("参保方案id")
    private Long schemeId;

    @ApiModelProperty("参保城市")
    private String city;

    @ApiModelProperty("0 停保 1 参保")
    private Integer status;

    @Override
    public String toString() {
        return "QueryInsurancePageListBO{" +
                "recordId=" + recordId +
                ", employeeName='" + employeeName + '\'' +
                ", schemeId=" + schemeId +
                ", city='" + city + '\'' +
                ", status=" + status +
                '}';
    }
}
