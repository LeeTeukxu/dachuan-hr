package com.tianye.hrsystem.modules.attendanceinfo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_attendance_info")
@ApiModel(value = "HrmAttendanceInfo", description = "考勤天数信息")
public class HrmAttendanceInfo implements Serializable {

    private static final long serialVersionUID = 813094765220855554L;

    @ApiModelProperty(value = "主键id")
    @TableId(value = "attendance_info_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long attendanceInfoId;

    @ApiModelProperty(value = "年")
    private Integer year;

    @ApiModelProperty(value = "月")
    private Integer month;

    @ApiModelProperty(value = "部门类型 1、行政部 2、生产部")
    private Integer deptType;

    @ApiModelProperty(value = "应出勤天数")
    private String actualWorkDay;
}
