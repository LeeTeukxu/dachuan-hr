package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_quit_info")
public class HrmEmployeeQuitInfo implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "quit_info_id")
  private Long quitInfoId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 计划离职日期
	 */
  @Column(name = "plan_quit_time")
  private Date planQuitTime;
	/**
	 * 申请离职日期
	 */
  @Column(name = "apply_quit_time")
  private Date applyQuitTime;
	/**
	 * 薪资结算日期
	 */
  @Column(name = "salary_settlement_time")
  private Date salarySettlementTime;
	/**
	 * 离职类型 1 主动离职 2 被动离职 3 退休
	 */
  @Column(name = "quit_type")
  private Integer quitType;
	/**
	 * 离职原因 1家庭原因 2身体原因 3薪资原因 4交通不便 5工作压力 6管理问题 7无晋升机会 8职业规划 9合同到期放弃续签 10其他个人原因  11试用期内辞退 12违反公司条例 13组织调整/裁员 14绩效不达标辞退 15合同到期不续签 16 其他原因被动离职
	 */
  @Column(name = "quit_reason")
  private Integer quitReason;
	/**
	 * 备注
	 */
  @Column(name = "remarks")
  private String remarks;
	/**
	 * 离职前状态
	 */
  @Column(name = "old_status")
  private Integer oldStatus;
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

  public Long getQuitInfoId() {
    return quitInfoId;
  }
  public void setQuitInfoId(Long quitInfoId) {
    this.quitInfoId = quitInfoId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Date getPlanQuitTime() {
    return planQuitTime;
  }
  public void setPlanQuitTime(Date planQuitTime) {
    this.planQuitTime = planQuitTime;
  }


  public Date getApplyQuitTime() {
    return applyQuitTime;
  }
  public void setApplyQuitTime(Date applyQuitTime) {
    this.applyQuitTime = applyQuitTime;
  }


  public Date getSalarySettlementTime() {
    return salarySettlementTime;
  }
  public void setSalarySettlementTime(Date salarySettlementTime) {
    this.salarySettlementTime = salarySettlementTime;
  }


  public Integer getQuitType() {
    return quitType;
  }
  public void setQuitType(Integer quitType) {
    this.quitType = quitType;
  }


  public Integer getQuitReason() {
    return quitReason;
  }
  public void setQuitReason(Integer quitReason) {
    this.quitReason = quitReason;
  }


  public String getRemarks() {
    return remarks;
  }
  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }


  public Integer getOldStatus() {
    return oldStatus;
  }
  public void setOldStatus(Integer oldStatus) {
    this.oldStatus = oldStatus;
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
