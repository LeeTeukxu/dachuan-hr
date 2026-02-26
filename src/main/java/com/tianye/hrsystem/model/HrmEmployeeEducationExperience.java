package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_education_experience")
public class HrmEmployeeEducationExperience implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "education_id")
  private Long educationId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 学历 1小学、2初中、3中专、4中职、5中技、6高中、7大专、8本科、9硕士、10博士、11博士后、12其他
	 */
  @Column(name = "education")
  private Integer education;
	/**
	 * 毕业院校
	 */
  @Column(name = "graduate_school")
  private String graduateSchool;
	/**
	 * 专业
	 */
  @Column(name = "major")
  private String major;
	/**
	 * 入学时间
	 */
  @Column(name = "admission_time")
  private Date admissionTime;
	/**
	 * 毕业时间
	 */
  @Column(name = "graduation_time")
  private Date graduationTime;
	/**
	 * 教学方式 1 全日制、2成人教育、3远程教育、4自学考试、5其他
	 */
  @Column(name = "teaching_methods")
  private Integer teachingMethods;
	/**
	 * 是否第一学历 0 否 1 是
	 */
  @Column(name = "is_first_degree")
  private Integer isFirstDegree;
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

  public Long getEducationId() {
    return educationId;
  }
  public void setEducationId(Long educationId) {
    this.educationId = educationId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Integer getEducation() {
    return education;
  }
  public void setEducation(Integer education) {
    this.education = education;
  }


  public String getGraduateSchool() {
    return graduateSchool;
  }
  public void setGraduateSchool(String graduateSchool) {
    this.graduateSchool = graduateSchool;
  }


  public String getMajor() {
    return major;
  }
  public void setMajor(String major) {
    this.major = major;
  }


  public Date getAdmissionTime() {
    return admissionTime;
  }
  public void setAdmissionTime(Date admissionTime) {
    this.admissionTime = admissionTime;
  }


  public Date getGraduationTime() {
    return graduationTime;
  }
  public void setGraduationTime(Date graduationTime) {
    this.graduationTime = graduationTime;
  }


  public Integer getTeachingMethods() {
    return teachingMethods;
  }
  public void setTeachingMethods(Integer teachingMethods) {
    this.teachingMethods = teachingMethods;
  }


  public Integer getIsFirstDegree() {
    return isFirstDegree;
  }
  public void setIsFirstDegree(Integer isFirstDegree) {
    this.isFirstDegree = isFirstDegree;
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
