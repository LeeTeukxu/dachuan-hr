package com.tianye.hrsystem.modules.attendanceinfo.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QueryAttendanceInfoBO extends MyPageEntity {

    @ApiModelProperty(value = "主键ID")
    private Long attendanceInfoId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "部门类型 1、行政部 2、生产部")
    private Integer deptType;

    @ApiModelProperty(value = "应出勤天数")
    private String actualWorkDay;

    private List<QueryAttendanceInfoBO> queryAttendanceInfoBOS;
}
