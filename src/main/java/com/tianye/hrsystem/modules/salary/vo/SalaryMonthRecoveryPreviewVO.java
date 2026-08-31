package com.tianye.hrsystem.modules.salary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SalaryMonthRecoveryPreviewVO {

    @ApiModelProperty("目标恢复年份")
    private Integer year;

    @ApiModelProperty("目标恢复月份")
    private Integer month;

    @ApiModelProperty("目标年月，YYYY-MM")
    private String yearMonth;

    @ApiModelProperty("目标薪资月记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetRecordId;

    @ApiModelProperty("目标薪资月记录状态")
    private Integer targetCheckStatus;

    @ApiModelProperty("目标薪资月记录状态名称")
    private String targetCheckStatusName;

    @ApiModelProperty("恢复后目标薪资月记录状态")
    private Integer restoredCheckStatus;

    @ApiModelProperty("恢复后目标薪资月记录状态名称")
    private String restoredCheckStatusName;

    @ApiModelProperty("是否可自动恢复")
    private Boolean recoverable = false;

    @ApiModelProperty("预览说明")
    private String message;

    @ApiModelProperty("阻断原因")
    private List<String> blockingReasons = new ArrayList<>();

    @ApiModelProperty("目标月份之后的薪资月记录")
    private List<SalaryMonthRecoveryRecordVO> laterRecords = new ArrayList<>();
}
