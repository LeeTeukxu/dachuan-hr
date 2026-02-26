package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_wifi")
public class HrmAttendanceWifi implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 打卡wifiid
	 */
  @Column(name = "attendance_wifi_id")
  private Long attendanceWifiId;
	/**
	 * 考勤组id
	 */
  @Column(name = "attendance_group_id")
  private Long attendanceGroupId;
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
	 * 创建者
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

  public Long getAttendanceWifiId() {
    return attendanceWifiId;
  }
  public void setAttendanceWifiId(Long attendanceWifiId) {
    this.attendanceWifiId = attendanceWifiId;
  }


  public Long getAttendanceGroupId() {
    return attendanceGroupId;
  }
  public void setAttendanceGroupId(Long attendanceGroupId) {
    this.attendanceGroupId = attendanceGroupId;
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

}
