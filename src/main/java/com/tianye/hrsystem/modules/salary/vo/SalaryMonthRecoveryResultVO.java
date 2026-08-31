package com.tianye.hrsystem.modules.salary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SalaryMonthRecoveryResultVO {

    @ApiModelProperty("目标恢复年份")
    private Integer year;

    @ApiModelProperty("目标恢复月份")
    private Integer month;

    @ApiModelProperty("目标年月，YYYY-MM")
    private String yearMonth;

    @ApiModelProperty("目标薪资月记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetRecordId;

    @ApiModelProperty("已删除的薪资月记录ID")
    private List<Long> deletedRecordIds = new ArrayList<>();

    @ApiModelProperty("已删除的薪资月份")
    private List<String> deletedMonths = new ArrayList<>();

    @ApiModelProperty("恢复后目标薪资月记录状态")
    private Integer restoredCheckStatus;

    @ApiModelProperty("恢复后目标薪资月记录状态名称")
    private String restoredCheckStatusName;

    @ApiModelProperty("处理说明")
    private String message;
}
