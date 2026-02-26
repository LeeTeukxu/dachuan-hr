package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_salary_card")
public class HrmEmployeeSalaryCard implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "salary_card_id")
  private Long salaryCardId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 工资卡卡号
	 */
  @Column(name = "salary_card_num")
  private String salaryCardNum;
	/**
	 * 开户城市
	 */
  @Column(name = "account_opening_city")
  private String accountOpeningCity;
	/**
	 * 银行名称
	 */
  @Column(name = "bank_name")
  private String bankName;
	/**
	 * 工资卡开户行
	 */
  @Column(name = "opening_bank")
  private String openingBank;
	/**
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Long createUserId;
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

  public Long getSalaryCardId() {
    return salaryCardId;
  }
  public void setSalaryCardId(Long salaryCardId) {
    this.salaryCardId = salaryCardId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getSalaryCardNum() {
    return salaryCardNum;
  }
  public void setSalaryCardNum(String salaryCardNum) {
    this.salaryCardNum = salaryCardNum;
  }


  public String getAccountOpeningCity() {
    return accountOpeningCity;
  }
  public void setAccountOpeningCity(String accountOpeningCity) {
    this.accountOpeningCity = accountOpeningCity;
  }


  public String getBankName() {
    return bankName;
  }
  public void setBankName(String bankName) {
    this.bankName = bankName;
  }


  public String getOpeningBank() {
    return openingBank;
  }
  public void setOpeningBank(String openingBank) {
    this.openingBank = openingBank;
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
