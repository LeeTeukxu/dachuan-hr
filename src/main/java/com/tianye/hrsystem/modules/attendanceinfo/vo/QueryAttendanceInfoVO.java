package com.tianye.hrsystem.modules.attendanceinfo.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryAttendanceInfoVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "attendance_info_id")
    private Long attendanceInfoId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "部门类型 0、行政部 1、生产部")
    private Integer deptType;

    @ApiModelProperty(value = "应出勤天数")
    private Integer actualWorkDay;
}
