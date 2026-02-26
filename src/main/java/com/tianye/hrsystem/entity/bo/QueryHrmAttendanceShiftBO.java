package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@ApiModel("查询班次")
@EqualsAndHashCode(callSuper = false)
public class QueryHrmAttendanceShiftBO extends PageEntity {

    @ApiModelProperty(value = "班次id")
    private Long shiftId;

    @ApiModelProperty(value = "班次名称")
    private String shiftName;

}
