package com.tianye.hrsystem.modules.holiday.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryHolidayDeductionBO extends MyPageEntity {

    @ApiModelProperty(value = "累计抵扣假期ID")
    private Long holidayId;

    @ApiModelProperty(value = "员工id")
    private Long employeeId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "抵扣类型 1、迟到 2、早退 3、事假 4、病假 5、调休 6、缺卡补卡 7、年假 8旷工")
    private Integer type;
}
