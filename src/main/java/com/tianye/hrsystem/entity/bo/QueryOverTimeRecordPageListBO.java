package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@ApiModel("查询加班")
@EqualsAndHashCode(callSuper = false)
public class QueryOverTimeRecordPageListBO extends PageEntity {

    @ApiModelProperty(value = "员工id")
    private Long employeeId;

    @ApiModelProperty(value = "加班类型(1.工作日加班, 2.休息日加班)")
    private Integer overTimeType;

}
