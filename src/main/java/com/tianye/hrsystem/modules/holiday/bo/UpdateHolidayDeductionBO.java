package com.tianye.hrsystem.modules.holiday.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateHolidayDeductionBO extends MyPageEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long deductionId;

    @ApiModelProperty(value = "假期余额ID")
    private Long holidayId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "抵扣类型 1、迟到 2、早退 3、事假 4、病假 5、调休 6、缺卡补卡 7、年假")
    private Integer type;

    @ApiModelProperty(value = "是否更新假期余额 1、不更新 2、更新")
    private Integer update_status;

    @ApiModelProperty(value = "抵扣备注")
    private String remark;

    @ApiModelProperty(value = "抵扣时间")
    private BigDecimal deductionTime;
}
