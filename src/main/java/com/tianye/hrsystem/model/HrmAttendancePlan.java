package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_plan")
public class HrmAttendancePlan implements Serializable {
  @Id
  @Column(name = "plan_id")
  private Long planId;
  @Column(name = "check_type")
  private String checkType;
  @Column(name = "user_id")
  private String userId;
  @Column(name = "plan_check_time")
  private Date planCheckTime;
  @Column(name = "group_id")
  private Long groupId;
  @Column(name = "emp_id")
  private Long empId;
  @Column(name = "create_time")
  private Date createTime;
  @Column(name = "create_user")
  private Long createUser;
  @Column(name="work_date")
  private Date workDate;

  @Column(name="class_id")
  private Long classId;

  @Column(name="class_setting_id")
  private Long classSettingId;


  public Long getPlanId() {
    return planId;
  }
  public void setPlanId(Long planId) {
    this.planId = planId;
  }



  public String getCheckType() {
    return checkType;
  }
  public void setCheckType(String checkType) {
    this.checkType = checkType;
  }


  public String getUserId() {
    return userId;
  }
  public void setUserId(String userId) {
    this.userId = userId;
  }


  public Date getPlanCheckTime() {
    return planCheckTime;
  }
  public void setPlanCheckTime(Date planCheckTime) {
    this.planCheckTime = planCheckTime;
  }


  public Long getGroupId() {
    return groupId;
  }
  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }


  public Long getEmpId() {
    return empId;
  }
  public void setEmpId(Long empId) {
    this.empId = empId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Long getCreateUser() {
    return createUser;
  }
  public void setCreateUser(Long createUser) {
    this.createUser = createUser;
  }

  public Date getWorkDate() {
    return workDate;
  }

  public void setWorkDate(Date workDate) {
    this.workDate = workDate;
  }

  public Long getClassId() {
    return classId;
  }

  public void setClassId(Long classId) {
    this.classId = classId;
  }

  public Long getClassSettingId() {
    return classSettingId;
  }

  public void setClassSettingId(Long classSettingId) {
    this.classSettingId = classSettingId;
  }
}
