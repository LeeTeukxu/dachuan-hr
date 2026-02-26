package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_point")
public class HrmAttendancePoint implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 打卡地点id
	 */
  @Column(name = "attendance_point_id")
  private Long attendancePointId;
	/**
	 * 考勤组id
	 */
  @Column(name = "attendance_group_id")
  private Long attendanceGroupId;
	/**
	 * 地点名称
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 定位名称
	 */
  @Column(name = "address")
  private String address;
	/**
	 * 纬度
	 */
  @Column(name = "lat")
  private String lat;
	/**
	 * 经度
	 */
  @Column(name = "lng")
  private String lng;
	/**
	 * 范围（米）
	 */
  @Column(name = "point_range")
  private Integer pointRange;
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

  public Long getAttendancePointId() {
    return attendancePointId;
  }
  public void setAttendancePointId(Long attendancePointId) {
    this.attendancePointId = attendancePointId;
  }


  public Long getAttendanceGroupId() {
    return attendanceGroupId;
  }
  public void setAttendanceGroupId(Long attendanceGroupId) {
    this.attendanceGroupId = attendanceGroupId;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }


  public String getLat() {
    return lat;
  }
  public void setLat(String lat) {
    this.lat = lat;
  }


  public String getLng() {
    return lng;
  }
  public void setLng(String lng) {
    this.lng = lng;
  }


  public Integer getPointRange() {
    return pointRange;
  }
  public void setPointRange(Integer pointRange) {
    this.pointRange = pointRange;
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
