package com.tianye.hrsystem.modules.workweek.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "hrm_workweek_day_setting")
public class HrmWorkweekDaySetting implements Serializable {

    @Id
    @Column(name = "day_setting_id")
    private Long daySettingId;

    @Column(name = "setting_year")
    private Integer settingYear;

    @Column(name = "setting_month")
    private Integer settingMonth;

    @Column(name = "work_date")
    private Date workDate;

    @Column(name = "day_type")
    private Integer dayType;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;
}
