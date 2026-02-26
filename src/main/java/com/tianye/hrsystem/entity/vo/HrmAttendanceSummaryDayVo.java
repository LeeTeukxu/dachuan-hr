package com.tianye.hrsystem.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 员工考勤情况 每日统计数据
 */
@Data
public class HrmAttendanceSummaryDayVo
{
    /**
     * 员工id
     */
    private Long employeeId;

    /**
     * 员工工号
     */
    private String jobNumber;

    /**
     * 早退次数
     */
    private Integer earlyCount;

    /**
     * 早退分钟
     */
    private Integer earlyMinute;

    /**
     * 迟到次数
     */
    private Integer lateCount;

    /**
     * 迟到分钟
     */
    private Integer lateMinute;

    /**
     * 缺卡次数
     */
    private Integer misscardCount;

    /**
     * 加班次数
     */
    private Integer overTimeCount;


    /**
     * 旷工天数
     */
    private double absenteeismDays;

    /**
     * 工作日加班(小时)
     */
    private Double workDateOverTime;

    /**
     * 休息日加班(小时)
     */
    private Double restOverTime;

    /**
     * 节假日加班(小时)
     */
    private Double holidayOverTime;

    /**
     * 应出勤天数
     */
    private Integer normalDays;

    /**
     * 加班时长
     */
    private Double overTimeHours;

    /**
     * 实际出勤天数
     */
    private Double actualityDays;

    /**
     * 产假
     */
    private Integer chanjia;

    /**
     * 婚假
     */
    private Integer hunjia;

    /**
     * 陪产假
     */
    private Integer peichanjia;

    /**
     * 丧假
     */
    private Integer sangjia;

    /**
     * 事假
     */
    private Double shijia;

    /**
     * 调休
     */
    private Double tiaoxiu;

    /**
     * 年假
     */
    private Double nianjia;

    /**
     * 哺乳假
     */
    private Double burujia;

    /**
     * 病假
     */
    private Double bingjia;

    private String workDate;


}
