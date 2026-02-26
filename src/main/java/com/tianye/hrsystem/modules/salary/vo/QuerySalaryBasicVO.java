package com.tianye.hrsystem.modules.salary.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuerySalaryBasicVO {
    @ApiModelProperty(value = "主键id")
    @TableId(value = "id")
    private Long Id;

    @ApiModelProperty(value = "部门ID")
    private Long deptId;

    @ApiModelProperty(value = "基本工资")
    private BigDecimal salaryBasic;

    @ApiModelProperty(value = "加班费")
    private BigDecimal overtimePay;

    @ApiModelProperty(value = "夜班补贴")
    private BigDecimal subsidy;

    @Override
    public String toString() {
        return "QuerySalaryBasicVO{" +
                "deptId=" + deptId +
                ", salaryBasic='" + salaryBasic +
                ", overtimePay=" + overtimePay +
                ", subsidy='" + subsidy +
                '}';
    }
}
