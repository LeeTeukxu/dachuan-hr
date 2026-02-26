package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuerySalaryConfigDto extends MyPageEntity {

    @ApiModelProperty("主键ID")
    private Long configId;

    private Integer salaryCycleStartDay;

    private Integer salaryCycleEndDay;

    private Integer payType;

    private Integer payDay;

    private Integer socialSecurityMonthType;

    private String salaryStartMonth;

    private String socialSecurityStartMonth;

    @Override
    public String toString() {
        return "QuerySalaryConfigVO{" +
                "configId=" + configId +
                ", salaryCycleStartDay='" + salaryCycleStartDay +
                ", salaryCycleEndDay=" + salaryCycleEndDay +
                ", payType='" + payType +
                ", payDay='" + payDay +
                ", socialSecurityMonthType=" + socialSecurityMonthType +
                ", salaryStartMonth='" + salaryStartMonth +
                ", socialSecurityStartMonth='" + socialSecurityStartMonth +
                '}';
    }
}
