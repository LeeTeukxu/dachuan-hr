package com.tianye.hrsystem.modules.salary.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SalaryComputeProgressVO {

    @ApiModelProperty("核算进度百分比")
    private Integer progress;

    @ApiModelProperty("状态：IDLE/RUNNING/SUCCESS/FAILED")
    private String status;

    @ApiModelProperty("阶段：PREPARE/LOAD_DATA/CALCULATE/PERSIST/FINISH/ERROR")
    private String stage;

    @ApiModelProperty("当前提示文案")
    private String message;

    @ApiModelProperty("已处理人数")
    private Integer processedCount;

    @ApiModelProperty("总人数")
    private Integer totalCount;

    @ApiModelProperty("是否已结束（成功或失败）")
    private Boolean done;

    @ApiModelProperty("是否成功")
    private Boolean success;

    @ApiModelProperty("失败问题明细")
    private List<String> errors;
}
