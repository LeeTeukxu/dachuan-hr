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
@Table(name = "hrm_workweek_setting")
public class HrmWorkweekSetting implements Serializable {

    @Id
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "setting_year")
    private Integer settingYear;

    @Column(name = "week_no")
    private Integer weekNo;

    @Column(name = "week_type")
    private Integer weekType;

    @Column(name = "week_start_date")
    private Date weekStartDate;

    @Column(name = "week_end_date")
    private Date weekEndDate;

    @Column(name = "rest_day_text")
    private String restDayText;

    @Column(name = "manual_override")
    private Integer manualOverride;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;
}
