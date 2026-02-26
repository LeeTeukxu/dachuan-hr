package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuerySalaryBasicDto extends MyPageEntity {

    @ApiModelProperty("主键ID")
    private Long Id;

    private Long deptId;

    private BigDecimal salaryBasic;

    private BigDecimal overtimePay;

    private BigDecimal subsidy;

    @Override
    public String toString() {
        return "QuerySalaryBasicDto{" +
                "deptId=" + deptId +
                ", salaryBasic='" + salaryBasic +
                ", overtimePay=" + overtimePay +
                ", subsidy='" + subsidy +
                '}';
    }
}
