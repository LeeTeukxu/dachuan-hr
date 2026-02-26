package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_legal_holidays")
public class HrmAttendanceLegalHolidays implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "holiday_id")
  private Long holidayId;
	/**
	 * 假期时间
	 */
  @Column(name = "holiday_time")
  private Date holidayTime;
	/**
	 * 类型 1.上班 2.休息
	 */
  @Column(name = "type")
  private Integer type;
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

  public Long getHolidayId() {
    return holidayId;
  }
  public void setHolidayId(Long holidayId) {
    this.holidayId = holidayId;
  }


  public Date getHolidayTime() {
    return holidayTime;
  }
  public void setHolidayTime(Date holidayTime) {
    this.holidayTime = holidayTime;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
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
