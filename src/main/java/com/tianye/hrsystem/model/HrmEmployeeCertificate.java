package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_certificate")
public class HrmEmployeeCertificate implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "certificate_id")
  private Long certificateId;
	/**
	 * 员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 证书名称
	 */
  @Column(name = "certificate_name")
  private String certificateName;
	/**
	 * 证书级别
	 */
  @Column(name = "certificate_level")
  private String certificateLevel;
	/**
	 * 证书编号
	 */
  @Column(name = "certificate_num")
  private String certificateNum;
	/**
	 * 有效起始日期
	 */
  @Column(name = "start_time")
  private Date startTime;
	/**
	 * 有效结束日期
	 */
  @Column(name = "end_time")
  private Date endTime;
	/**
	 * 发证机构
	 */
  @Column(name = "issuing_authority")
  private String issuingAuthority;
	/**
	 * 发证日期
	 */
  @Column(name = "issuing_time")
  private Date issuingTime;
	/**
	 * 备注
	 */
  @Column(name = "remarks")
  private String remarks;
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

  public Long getCertificateId() {
    return certificateId;
  }
  public void setCertificateId(Long certificateId) {
    this.certificateId = certificateId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getCertificateName() {
    return certificateName;
  }
  public void setCertificateName(String certificateName) {
    this.certificateName = certificateName;
  }


  public String getCertificateLevel() {
    return certificateLevel;
  }
  public void setCertificateLevel(String certificateLevel) {
    this.certificateLevel = certificateLevel;
  }


  public String getCertificateNum() {
    return certificateNum;
  }
  public void setCertificateNum(String certificateNum) {
    this.certificateNum = certificateNum;
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


  public String getIssuingAuthority() {
    return issuingAuthority;
  }
  public void setIssuingAuthority(String issuingAuthority) {
    this.issuingAuthority = issuingAuthority;
  }


  public Date getIssuingTime() {
    return issuingTime;
  }
  public void setIssuingTime(Date issuingTime) {
    this.issuingTime = issuingTime;
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
