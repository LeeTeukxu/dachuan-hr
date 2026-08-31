package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QueryOvertimeNightStatisticsPageBO extends MyPageEntity {

    @ApiModelProperty("统计月份，格式：YYYY-MM")
    private String month;

    @ApiModelProperty("姓名/工号关键字")
    private String keyword;

    @ApiModelProperty("指定员工ID（单人统计时使用）")
    private Long employeeId;

    @ApiModelProperty("指定员工ID列表（单人统计批量选择时使用）")
    private List<Long> employeeIds;
}
