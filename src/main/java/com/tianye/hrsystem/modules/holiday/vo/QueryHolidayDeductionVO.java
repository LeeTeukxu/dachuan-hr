package com.tianye.hrsystem.modules.holiday.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryHolidayDeductionVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "deduction_id")
    private Long deductionId;

    @ApiModelProperty(value = "假期余额ID")
    private Long holidayId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "员工")
    private String employeeName;

    @ApiModelProperty(value = "抵扣类型 1、迟到 2、早退 3、事假 4、病假 5、调休 6、缺卡补卡 7、年假 8旷工")
    private Integer type;

    @ApiModelProperty(value = "抵扣时间")
    private BigDecimal deductionTime;
}
