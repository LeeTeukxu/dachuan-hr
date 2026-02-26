package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_examine")
public class HrmAttendanceExamine implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 审批id
	 */
  @Column(name = "attendance_examine_id")
  private Long attendanceExamineId;
	/**
	 * 关联审批字段id
	 */
  @Column(name = "examine_field_id")
  private Long examineFieldId;
	/**
	 * 类型字段id
	 */
  @Column(name = "type_field_id")
  private Long typeFieldId;
	/**
	 * 开始时间字段id
	 */
  @Column(name = "start_time_field_id")
  private Long startTimeFieldId;
	/**
	 * 结束时间字段id
	 */
  @Column(name = "end_time_field_id")
  private Long endTimeFieldId;
	/**
	 * 天数字段id
	 */
  @Column(name = "duration_field_id")
  private Long durationFieldId;
	/**
	 * 备注字段id
	 */
  @Column(name = "remark_field_id")
  private Long remarkFieldId;
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

  public Long getAttendanceExamineId() {
    return attendanceExamineId;
  }
  public void setAttendanceExamineId(Long attendanceExamineId) {
    this.attendanceExamineId = attendanceExamineId;
  }


  public Long getExamineFieldId() {
    return examineFieldId;
  }
  public void setExamineFieldId(Long examineFieldId) {
    this.examineFieldId = examineFieldId;
  }


  public Long getTypeFieldId() {
    return typeFieldId;
  }
  public void setTypeFieldId(Long typeFieldId) {
    this.typeFieldId = typeFieldId;
  }


  public Long getStartTimeFieldId() {
    return startTimeFieldId;
  }
  public void setStartTimeFieldId(Long startTimeFieldId) {
    this.startTimeFieldId = startTimeFieldId;
  }


  public Long getEndTimeFieldId() {
    return endTimeFieldId;
  }
  public void setEndTimeFieldId(Long endTimeFieldId) {
    this.endTimeFieldId = endTimeFieldId;
  }


  public Long getDurationFieldId() {
    return durationFieldId;
  }
  public void setDurationFieldId(Long durationFieldId) {
    this.durationFieldId = durationFieldId;
  }


  public Long getRemarkFieldId() {
    return remarkFieldId;
  }
  public void setRemarkFieldId(Long remarkFieldId) {
    this.remarkFieldId = remarkFieldId;
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
