package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_leave_record")
public class HrmEmployeeLeaveRecord implements Serializable {
  @Id
	/**
	 * 请假记录id
	 */
  @Column(name = "leave_record_id")
  private Long leaveRecordId;
	/**
	 * 审批id
	 */
  @Column(name = "examine_id")
  private String  examineId;
	/**
	 * 员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 请假类型
	 */
  @Column(name = "leave_type")
  private String leaveType;
	/**
	 * 请假开始时间
	 */
  @Column(name = "leave_start_time")
  private Date leaveStartTime;
	/**
	 * 请假结束时间
	 */
  @Column(name = "leave_end_time")
  private Date leaveEndTime;
	/**
	 * 请假时长
	 */
  @Column(name = "leave_day")
  private Double leaveDay;
	/**
	 * 请假理由
	 */
  @Column(name = "leave_reason")
  private String leaveReason;
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

  public Long getLeaveRecordId() {
    return leaveRecordId;
  }
  public void setLeaveRecordId(Long leaveRecordId) {
    this.leaveRecordId = leaveRecordId;
  }


  public String  getExamineId() {
    return examineId;
  }
  public void setExamineId(String  examineId) {
    this.examineId = examineId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getLeaveType() {
    return leaveType;
  }
  public void setLeaveType(String leaveType) {
    this.leaveType = leaveType;
  }


  public Date getLeaveStartTime() {
    return leaveStartTime;
  }
  public void setLeaveStartTime(Date leaveStartTime) {
    this.leaveStartTime = leaveStartTime;
  }


  public Date getLeaveEndTime() {
    return leaveEndTime;
  }
  public void setLeaveEndTime(Date leaveEndTime) {
    this.leaveEndTime = leaveEndTime;
  }


  public Double getLeaveDay() {
    return leaveDay;
  }
  public void setLeaveDay(Double leaveDay) {
    this.leaveDay = leaveDay;
  }


  public String getLeaveReason() {
    return leaveReason;
  }
  public void setLeaveReason(String leaveReason) {
    this.leaveReason = leaveReason;
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

}
