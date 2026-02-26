package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_social_security_info")
public class HrmEmployeeSocialSecurityInfo implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "social_security_info_id")
  private Long socialSecurityInfoId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 是否首次缴纳社保 0 否 1 是
	 */
  @Column(name = "is_first_social_security")
  private Integer isFirstSocialSecurity;
	/**
	 * 是否首次缴纳公积金 0 否 1 是
	 */
  @Column(name = "is_first_accumulation_fund")
  private Integer isFirstAccumulationFund;
	/**
	 * 社保号
	 */
  @Column(name = "social_security_num")
  private String socialSecurityNum;
	/**
	 * 公积金账号
	 */
  @Column(name = "accumulation_fund_num")
  private String accumulationFundNum;
	/**
	 * 参保起始月份（2020.05）
	 */
  @Column(name = "social_security_start_month")
  private String socialSecurityStartMonth;
	/**
	 * 参保方案
	 */
  @Column(name = "scheme_id")
  private Long schemeId;
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

  public Long getSocialSecurityInfoId() {
    return socialSecurityInfoId;
  }
  public void setSocialSecurityInfoId(Long socialSecurityInfoId) {
    this.socialSecurityInfoId = socialSecurityInfoId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Integer getIsFirstSocialSecurity() {
    return isFirstSocialSecurity;
  }
  public void setIsFirstSocialSecurity(Integer isFirstSocialSecurity) {
    this.isFirstSocialSecurity = isFirstSocialSecurity;
  }


  public Integer getIsFirstAccumulationFund() {
    return isFirstAccumulationFund;
  }
  public void setIsFirstAccumulationFund(Integer isFirstAccumulationFund) {
    this.isFirstAccumulationFund = isFirstAccumulationFund;
  }


  public String getSocialSecurityNum() {
    return socialSecurityNum;
  }
  public void setSocialSecurityNum(String socialSecurityNum) {
    this.socialSecurityNum = socialSecurityNum;
  }


  public String getAccumulationFundNum() {
    return accumulationFundNum;
  }
  public void setAccumulationFundNum(String accumulationFundNum) {
    this.accumulationFundNum = accumulationFundNum;
  }


  public String getSocialSecurityStartMonth() {
    return socialSecurityStartMonth;
  }
  public void setSocialSecurityStartMonth(String socialSecurityStartMonth) {
    this.socialSecurityStartMonth = socialSecurityStartMonth;
  }


  public Long getSchemeId() {
    return schemeId;
  }
  public void setSchemeId(Long schemeId) {
    this.schemeId = schemeId;
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
