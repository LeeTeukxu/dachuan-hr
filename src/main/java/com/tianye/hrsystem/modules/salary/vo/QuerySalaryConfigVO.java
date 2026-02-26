package com.tianye.hrsystem.modules.salary.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuerySalaryConfigVO {
    @ApiModelProperty(value = "主键id")
    @TableId(value = "config_id")
    private Long configId;

    @ApiModelProperty(value = "计薪周期开始日")
    private Integer salaryCycleStartDay;

    @ApiModelProperty(value = "计薪周期结束日")
    private Integer salaryCycleEndDay;

    @ApiModelProperty(value = "发薪日期类型 1当月 2次月")
    private Integer payType;

    @ApiModelProperty(value = "发薪日期")
    private Integer payDay;

    @ApiModelProperty(value = "对应社保自然月 0上月 1当月 2次月")
    private Integer socialSecurityMonthType;

    @ApiModelProperty(value = "薪酬起始月份（例2020.05）")
    private String salaryStartMonth;

    @ApiModelProperty(value = "社保开始月（例2020.05）")
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
