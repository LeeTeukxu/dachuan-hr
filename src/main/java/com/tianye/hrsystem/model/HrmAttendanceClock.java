package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_clock")
public class HrmAttendanceClock implements Serializable {
  @Id
	/**
	 * 打卡记录id
	 */
  @Column(name = "clock_id")
  private Long clockId;
  @Column(name = "clock_employee_id")
  private Long clockEmployeeId;
	/**
	 * 打卡时间
	 */
  @Column(name = "clock_time")
  private Date clockTime;
	/**
	 * 打卡类型 1 上班打卡 2 下班打卡
	 */
  @Column(name = "clock_type")
  private Integer clockType;
	/**
	 * 上班时间（正常打卡时间）
	 */
  @Column(name = "attendance_time")
  private Date attendanceTime;
	/**
	 * 打卡来源类型 1手机端打卡 2手工录入3.自动打卡
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 打卡状态 0 正常 1 迟到 2 早退 3 旷工迟到  4 加班 5 未打卡
	 */
  @Column(name = "clock_status")
  private Integer clockStatus;
	/**
	 * 打卡阶段
	 */
  @Column(name = "clock_stage")
  private Integer clockStage;


  @Column(name="class_id")
  private Long classId;
  @Column(name="plan_id")
  private Long planId;
  @Column(name="work_date")
  private Date workDate;
	/**
	 * 考勤地址
	 */
  @Column(name = "address")
  private String address;
	/**
	 * 经度
	 */
  @Column(name = "lng")
  private String lng;
	/**
	 * 维度
	 */
  @Column(name = "lat")
  private String lat;
	/**
	 * wifi名称
	 */
  @Column(name = "ssid")
  private String ssid;
	/**
	 * mac地址
	 */
  @Column(name = "mac")
  private String mac;
	/**
	 * 是否外勤（0否 1是）
	 */
  @Column(name = "is_out_work")
  private Integer isOutWork;
	/**
	 * 备注
	 */
  @Column(name = "remark")
  private String remark;
	/**
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Long createUserId;
	/**
	 * 创建时间
	 */
  @Column(name = "create_time")
  private Date createTime;
	/**
	 * 更新人id
	 */
  @Column(name = "update_user_id")
  private Long updateUserId;
	/**
	 * 更新时间
	 */
  @Column(name = "update_time")
  private Date updateTime;

  public Long getClockId() {
    return clockId;
  }
  public void setClockId(Long clockId) {
    this.clockId = clockId;
  }


  public Long getClockEmployeeId() {
    return clockEmployeeId;
  }
  public void setClockEmployeeId(Long clockEmployeeId) {
    this.clockEmployeeId = clockEmployeeId;
  }


  public Date getClockTime() {
    return clockTime;
  }
  public void setClockTime(Date clockTime) {
    this.clockTime = clockTime;
  }


  public Integer getClockType() {
    return clockType;
  }
  public void setClockType(Integer clockType) {
    this.clockType = clockType;
  }


  public Date getAttendanceTime() {
    return attendanceTime;
  }
  public void setAttendanceTime(Date attendanceTime) {
    this.attendanceTime = attendanceTime;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }


  public Integer getClockStatus() {
    return clockStatus;
  }
  public void setClockStatus(Integer clockStatus) {
    this.clockStatus = clockStatus;
  }


  public Integer getClockStage() {
    return clockStage;
  }
  public void setClockStage(Integer clockStage) {
    this.clockStage = clockStage;
  }


  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }


  public String getLng() {
    return lng;
  }
  public void setLng(String lng) {
    this.lng = lng;
  }


  public String getLat() {
    return lat;
  }
  public void setLat(String lat) {
    this.lat = lat;
  }


  public String getSsid() {
    return ssid;
  }
  public void setSsid(String ssid) {
    this.ssid = ssid;
  }


  public String getMac() {
    return mac;
  }
  public void setMac(String mac) {
    this.mac = mac;
  }


  public Integer getIsOutWork() {
    return isOutWork;
  }
  public void setIsOutWork(Integer isOutWork) {
    this.isOutWork = isOutWork;
  }


  public String getRemark() {
    return remark;
  }
  public void setRemark(String remark) {
    this.remark = remark;
  }


  public Long getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Long createUserId) {
    this.createUserId = createUserId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Long getUpdateUserId() {
    return updateUserId;
  }
  public void setUpdateUserId(Long updateUserId) {
    this.updateUserId = updateUserId;
  }


  public Date getUpdateTime() {
    return updateTime;
  }
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Date getWorkDate() {
        return workDate;
    }

    public void setWorkDate(Date workDate) {
        this.workDate = workDate;
    }
}
