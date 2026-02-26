package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@ApiModel("查询考勤分组")
@EqualsAndHashCode(callSuper = false)
public class QueryHrmAttendanceGroupBO extends PageEntity {

    @ApiModelProperty(value = "考勤组id")
    private Long attendanceGroupId;

    @ApiModelProperty(value = "名称")
    private String name;

}
