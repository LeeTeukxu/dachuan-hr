package com.tianye.hrsystem.entity.po;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 员工排班信息(HrmEmpSchedule)表实体类
 *
 * @author makejava
 * @since 2024-04-21 10:44:31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_emp_schedule")
@ApiModel(value = "HrmEmpSchedule对象", description = "员工排班信息")
public class HrmEmpSchedule {
    //id
    private Long id;
    //该员工打卡时间
    private Date checkDateTime;
    //打卡状态：Init：未打 Checked：已打卡 Timeout：缺卡
    private String checkStatus;
    //考勤类型：Onduty：上班打卡 OffDuty：下班打卡
    private String checkType;
    //班次ID
    private Long classId;
    //班次名称
    private String className;
    
    private String classSettingId;
    //考勤组id
    private Long groupId;
    //是否休息：Y：当天排休 N：不休息
    private String isRest;
    //开启弹性工时卡点调整后用户应打卡时间
    private Date realPlanTime;
    //工作日
    private Date workDate;
    //开始打卡时间
    private Date checkBeginTime;
    //结束打卡时间
    private Date checkEndTime;

    //计划打卡时间
    private Date planCheckTime;
    //扩展字段。

/**dataSource：该字段暂无实际意义

flexMinutes：所在班次设置的允许晚到晚走，早到早走的时间，单位分钟

punchId：对应卡点

idovertimeSettingId：加班规则ID
 **/
    private String features;
    //员工id
    private Long empId;



}

