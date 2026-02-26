package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_contract")
public class HrmEmployeeContract implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "contract_id")
  private Long contractId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 合同编号
	 */
  @Column(name = "contract_num")
  private String contractNum;
	/**
	 * 1、固定期限劳动合同 2、无固定期限劳动合同 3、已完成一定工作任务为期限的劳动合同 4、实习协议 5、劳务合同 6、返聘协议 7、劳务派遣合同 8、借调合同 9、其他
	 */
  @Column(name = "contract_type")
  private Integer contractType;
  @Column(name = "start_time")
  private Date startTime;
  @Column(name = "end_time")
  private Date endTime;
	/**
	 * 期限
	 */
  @Column(name = "term")
  private Integer term;
	/**
	 * 合同状态  0未执行 1 执行中、 2已到期、 
	 */
  @Column(name = "status")
  private Integer status;
	/**
	 * 签约公司
	 */
  @Column(name = "sign_company")
  private String signCompany;
	/**
	 * 合同签订日期
	 */
  @Column(name = "sign_time")
  private Date signTime;
	/**
	 * 备注
	 */
  @Column(name = "remarks")
  private String remarks;
	/**
	 * 是否到期提醒 0 否 1 是
	 */
  @Column(name = "is_expire_remind")
  private Integer isExpireRemind;
  @Column(name = "sort")
  private Integer sort;
  @Column(name = "batch_id")
  private String batchId;
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

  public Long getContractId() {
    return contractId;
  }
  public void setContractId(Long contractId) {
    this.contractId = contractId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getContractNum() {
    return contractNum;
  }
  public void setContractNum(String contractNum) {
    this.contractNum = contractNum;
  }


  public Integer getContractType() {
    return contractType;
  }
  public void setContractType(Integer contractType) {
    this.contractType = contractType;
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


  public Integer getTerm() {
    return term;
  }
  public void setTerm(Integer term) {
    this.term = term;
  }


  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }


  public String getSignCompany() {
    return signCompany;
  }
  public void setSignCompany(String signCompany) {
    this.signCompany = signCompany;
  }


  public Date getSignTime() {
    return signTime;
  }
  public void setSignTime(Date signTime) {
    this.signTime = signTime;
  }


  public String getRemarks() {
    return remarks;
  }
  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }


  public Integer getIsExpireRemind() {
    return isExpireRemind;
  }
  public void setIsExpireRemind(Integer isExpireRemind) {
    this.isExpireRemind = isExpireRemind;
  }


  public Integer getSort() {
    return sort;
  }
  public void setSort(Integer sort) {
    this.sort = sort;
  }


  public String getBatchId() {
    return batchId;
  }
  public void setBatchId(String batchId) {
    this.batchId = batchId;
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
