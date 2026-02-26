package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_over_time_record")
public class HrmEmployeeOverTimeRecord implements Serializable {
  @Id
  @Column(name = "over_time_id")
  private Long overTimeId;
	/**
	 * 员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 加班类型(1.工作日加班, 2.休息日加班)
	 */
  @Column(name = "over_time_type")
  private Integer overTimeType;
	/**
	 * 加班开始时间
	 */
  @Column(name = "over_time_start_time")
  private Date overTimeStartTime;
	/**
	 * 加班结束时间
	 */
  @Column(name = "over_time_end_time")
  private Date overTimeEndTime;
	/**
	 * 上班时间
	 */
  @Column(name = "attendance_time")
  private Date attendanceTime;
	/**
	 * 加班时长
	 */
  @Column(name = "over_times")
  private Double overTimes;
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

    /**
     * 审批id
     */
    @Column(name = "examine_id")
    private String  examineId;

  public Long getOverTimeId() {
    return overTimeId;
  }
  public void setOverTimeId(Long overTimeId) {
    this.overTimeId = overTimeId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Integer getOverTimeType() {
    return overTimeType;
  }
  public void setOverTimeType(Integer overTimeType) {
    this.overTimeType = overTimeType;
  }


  public Date getOverTimeStartTime() {
    return overTimeStartTime;
  }
  public void setOverTimeStartTime(Date overTimeStartTime) {
    this.overTimeStartTime = overTimeStartTime;
  }


  public Date getOverTimeEndTime() {
    return overTimeEndTime;
  }
  public void setOverTimeEndTime(Date overTimeEndTime) {
    this.overTimeEndTime = overTimeEndTime;
  }


  public Date getAttendanceTime() {
    return attendanceTime;
  }
  public void setAttendanceTime(Date attendanceTime) {
    this.attendanceTime = attendanceTime;
  }


  public Double getOverTimes() {
    return overTimes;
  }
  public void setOverTimes(Double overTimes) {
    this.overTimes = overTimes;
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

    public String getExamineId() {
        return examineId;
    }

    public void setExamineId(String examineId) {
        this.examineId = examineId;
    }
}
