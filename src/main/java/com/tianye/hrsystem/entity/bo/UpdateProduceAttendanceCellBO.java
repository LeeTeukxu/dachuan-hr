package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProduceAttendanceCellBO {

    @ApiModelProperty("上传考勤主键ID")
    private Long summaryId;

    @ApiModelProperty("字段名")
    private String field;

    @ApiModelProperty("字段值")
    private String value;
}
