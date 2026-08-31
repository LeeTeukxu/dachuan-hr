package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryOvertimeNightDailyDetailPageBO extends MyPageEntity {

    @ApiModelProperty("统计月份，格式：YYYY-MM")
    private String month;

    @ApiModelProperty("员工姓名/工号关键字")
    private String keyword;
}
