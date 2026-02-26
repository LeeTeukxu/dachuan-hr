package com.tianye.hrsystem.modules.insurance.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryInsuranceSchemeVO {
    @ApiModelProperty(value = "主键id")
    @TableId(value = "id")
    private Long id;

    @ApiModelProperty(value = "部门ID")
    private String deptId;

    @ApiModelProperty(value = "基本工资")
    private BigDecimal salaryBasic;

    @ApiModelProperty(value = "加班费")
    private BigDecimal overtimePay;

    @ApiModelProperty(value = "夜班补贴")
    private BigDecimal subsidy;

    @Override
    public String toString() {
        return "QueryEmpInsuranceMonthVO{" +
                "id=" + id +
                ", deptId='" + deptId + '\'' +
                ", salaryBasic='" + salaryBasic + '\'' +
                ", overtimePay=" + overtimePay +
                ", subsidy='" + subsidy + '\'' +
                '}';
    }
}
