package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncProduceAttendanceBO {

    @ApiModelProperty("同步月份，格式：YYYY-MM")
    private String month;

    @ApiModelProperty("年份，兼容旧调用")
    private Integer year;

    @ApiModelProperty("月份，兼容旧调用")
    private Integer monthNumber;
}
