package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_work_experience")
public class HrmEmployeeWorkExperience implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "work_exp_id")
  private Long workExpId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 工作单位
	 */
  @Column(name = "work_unit")
  private String workUnit;
	/**
	 * 职务
	 */
  @Column(name = "post")
  private String post;
	/**
	 * 工作开始时间
	 */
  @Column(name = "work_start_time")
  private Date workStartTime;
	/**
	 * 工作结束时间
	 */
  @Column(name = "work_end_time")
  private Date workEndTime;
	/**
	 * 离职原因
	 */
  @Column(name = "leaving_reason")
  private String leavingReason;
	/**
	 * 证明人
	 */
  @Column(name = "witness")
  private String witness;
	/**
	 * 证明人手机号
	 */
  @Column(name = "witness_phone")
  private String witnessPhone;
	/**
	 * 工作备注
	 */
  @Column(name = "work_remarks")
  private String workRemarks;
	/**
	 * 排序
	 */
  @Column(name = "sort")
  private Integer sort;
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
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Long createUserId;

  public Long getWorkExpId() {
    return workExpId;
  }
  public void setWorkExpId(Long workExpId) {
    this.workExpId = workExpId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getWorkUnit() {
    return workUnit;
  }
  public void setWorkUnit(String workUnit) {
    this.workUnit = workUnit;
  }


  public String getPost() {
    return post;
  }
  public void setPost(String post) {
    this.post = post;
  }


  public Date getWorkStartTime() {
    return workStartTime;
  }
  public void setWorkStartTime(Date workStartTime) {
    this.workStartTime = workStartTime;
  }


  public Date getWorkEndTime() {
    return workEndTime;
  }
  public void setWorkEndTime(Date workEndTime) {
    this.workEndTime = workEndTime;
  }


  public String getLeavingReason() {
    return leavingReason;
  }
  public void setLeavingReason(String leavingReason) {
    this.leavingReason = leavingReason;
  }


  public String getWitness() {
    return witness;
  }
  public void setWitness(String witness) {
    this.witness = witness;
  }


  public String getWitnessPhone() {
    return witnessPhone;
  }
  public void setWitnessPhone(String witnessPhone) {
    this.witnessPhone = witnessPhone;
  }


  public String getWorkRemarks() {
    return workRemarks;
  }
  public void setWorkRemarks(String workRemarks) {
    this.workRemarks = workRemarks;
  }


  public Integer getSort() {
    return sort;
  }
  public void setSort(Integer sort) {
    this.sort = sort;
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


  public Long getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Long createUserId) {
    this.createUserId = createUserId;
  }

}
