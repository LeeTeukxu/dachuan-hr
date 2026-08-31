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

    @ApiModelProperty(value = "普通员工全勤金额")
    private BigDecimal ordinaryFullAttendanceAmount;

    @ApiModelProperty(value = "领导全勤金额")
    private BigDecimal leaderFullAttendanceAmount;

    @ApiModelProperty(value = "生产体系员工月度休息天数")
    private Integer productionMonthlyRestDays;

    @ApiModelProperty(value = "大额医疗保险金额")
    private BigDecimal largeMedicalInsuranceAmount;

    @ApiModelProperty(value = "长期护理保险金额")
    private BigDecimal longTermCareInsuranceAmount;

    @Override
    public String toString() {
        return "QuerySalaryBasicVO{" +
                "deptId=" + deptId +
                ", salaryBasic='" + salaryBasic +
                ", overtimePay=" + overtimePay +
                ", subsidy='" + subsidy +
                ", ordinaryFullAttendanceAmount=" + ordinaryFullAttendanceAmount +
                ", leaderFullAttendanceAmount=" + leaderFullAttendanceAmount +
                ", productionMonthlyRestDays=" + productionMonthlyRestDays +
                ", largeMedicalInsuranceAmount=" + largeMedicalInsuranceAmount +
                ", longTermCareInsuranceAmount=" + longTermCareInsuranceAmount +
                '}';
    }
}
