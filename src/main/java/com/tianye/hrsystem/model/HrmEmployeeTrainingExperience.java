package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_training_experience")
public class HrmEmployeeTrainingExperience implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "training_id")
  private Long trainingId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 培训课程
	 */
  @Column(name = "training_course")
  private String trainingCourse;
	/**
	 * 培训机构名称
	 */
  @Column(name = "training_organ_name")
  private String trainingOrganName;
	/**
	 * 培训开始时间
	 */
  @Column(name = "start_time")
  private Date startTime;
	/**
	 * 培训结束时间
	 */
  @Column(name = "end_time")
  private Date endTime;
	/**
	 * 培训时长
	 */
  @Column(name = "training_duration")
  private String trainingDuration;
	/**
	 * 培训成绩
	 */
  @Column(name = "training_results")
  private String trainingResults;
	/**
	 * 培训课程名称
	 */
  @Column(name = "training_certificate_name")
  private String trainingCertificateName;
	/**
	 * 备注
	 */
  @Column(name = "remarks")
  private String remarks;
	/**
	 * 排序
	 */
  @Column(name = "sort")
  private Integer sort;
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
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Long createUserId;

  public Long getTrainingId() {
    return trainingId;
  }
  public void setTrainingId(Long trainingId) {
    this.trainingId = trainingId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getTrainingCourse() {
    return trainingCourse;
  }
  public void setTrainingCourse(String trainingCourse) {
    this.trainingCourse = trainingCourse;
  }


  public String getTrainingOrganName() {
    return trainingOrganName;
  }
  public void setTrainingOrganName(String trainingOrganName) {
    this.trainingOrganName = trainingOrganName;
  }


  public Date getStartTime() {
    return startTime;
  }
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }


  public Date getEndTime() {
    return endTime;
  }
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }


  public String getTrainingDuration() {
    return trainingDuration;
  }
  public void setTrainingDuration(String trainingDuration) {
    this.trainingDuration = trainingDuration;
  }


  public String getTrainingResults() {
    return trainingResults;
  }
  public void setTrainingResults(String trainingResults) {
    this.trainingResults = trainingResults;
  }


  public String getTrainingCertificateName() {
    return trainingCertificateName;
  }
  public void setTrainingCertificateName(String trainingCertificateName) {
    this.trainingCertificateName = trainingCertificateName;
  }


  public String getRemarks() {
    return remarks;
  }
  public void setRemarks(String remarks) {
    this.remarks = remarks;
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
