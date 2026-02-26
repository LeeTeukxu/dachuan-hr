package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_rule")
public class HrmAttendanceRule implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 打卡规则id
	 */
  @Column(name = "attendance_rule_id")
  private Long attendanceRuleId;
	/**
	 * 打卡规则名称
	 */
  @Column(name = "attendance_rule_name")
  private String attendanceRuleName;
	/**
	 * 迟到规则计算方式
	 */
  @Column(name = "late_rule_method")
  private Integer lateRuleMethod;
	/**
	 * 迟到扣款金额
	 */
  @Column(name = "late_deduct_money")
  private Double lateDeductMoney;
	/**
	 * 早退规则计算方式
	 */
  @Column(name = "early_rule_method")
  private Integer earlyRuleMethod;
	/**
	 * 早退扣款金额
	 */
  @Column(name = "early_deduct_money")
  private Double earlyDeductMoney;
	/**
	 * 缺卡规则计算方式
	 */
  @Column(name = "misscard_rule_method")
  private Integer misscardRuleMethod;
	/**
	 * 缺卡扣款金额
	 */
  @Column(name = "misscard_deduct_money")
  private Double misscardDeductMoney;
	/**
	 * 旷工规则计算方式
	 */
  @Column(name = "absenteeism_rule_method")
  private Integer absenteeismRuleMethod;
	/**
	 * 旷工扣款金额
	 */
  @Column(name = "absenteeism_deduct_money")
  private Double absenteeismDeductMoney;
	/**
	 * 是否个性化设置(0 否 1是)
	 */
  @Column(name = "is_personalization")
  private Integer isPersonalization;
	/**
	 * 迟到的总分钟或总次数
	 */
  @Column(name = "late_minutes_or_counts")
  private Integer lateMinutesOrCounts;
	/**
	 * 早退的总分钟或总次数
	 */
  @Column(name = "early_minutes_or_counts")
  private Integer earlyMinutesOrCounts;
	/**
	 * 是否是默认配置（0否 1是）
	 */
  @Column(name = "is_default_setting")
  private Integer isDefaultSetting;
	/**
	 * 创建者
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

  public Long getAttendanceRuleId() {
    return attendanceRuleId;
  }
  public void setAttendanceRuleId(Long attendanceRuleId) {
    this.attendanceRuleId = attendanceRuleId;
  }


  public String getAttendanceRuleName() {
    return attendanceRuleName;
  }
  public void setAttendanceRuleName(String attendanceRuleName) {
    this.attendanceRuleName = attendanceRuleName;
  }


  public Integer getLateRuleMethod() {
    return lateRuleMethod;
  }
  public void setLateRuleMethod(Integer lateRuleMethod) {
    this.lateRuleMethod = lateRuleMethod;
  }


  public Double getLateDeductMoney() {
    return lateDeductMoney;
  }
  public void setLateDeductMoney(Double lateDeductMoney) {
    this.lateDeductMoney = lateDeductMoney;
  }


  public Integer getEarlyRuleMethod() {
    return earlyRuleMethod;
  }
  public void setEarlyRuleMethod(Integer earlyRuleMethod) {
    this.earlyRuleMethod = earlyRuleMethod;
  }


  public Double getEarlyDeductMoney() {
    return earlyDeductMoney;
  }
  public void setEarlyDeductMoney(Double earlyDeductMoney) {
    this.earlyDeductMoney = earlyDeductMoney;
  }


  public Integer getMisscardRuleMethod() {
    return misscardRuleMethod;
  }
  public void setMisscardRuleMethod(Integer misscardRuleMethod) {
    this.misscardRuleMethod = misscardRuleMethod;
  }


  public Double getMisscardDeductMoney() {
    return misscardDeductMoney;
  }
  public void setMisscardDeductMoney(Double misscardDeductMoney) {
    this.misscardDeductMoney = misscardDeductMoney;
  }


  public Integer getAbsenteeismRuleMethod() {
    return absenteeismRuleMethod;
  }
  public void setAbsenteeismRuleMethod(Integer absenteeismRuleMethod) {
    this.absenteeismRuleMethod = absenteeismRuleMethod;
  }


  public Double getAbsenteeismDeductMoney() {
    return absenteeismDeductMoney;
  }
  public void setAbsenteeismDeductMoney(Double absenteeismDeductMoney) {
    this.absenteeismDeductMoney = absenteeismDeductMoney;
  }


  public Integer getIsPersonalization() {
    return isPersonalization;
  }
  public void setIsPersonalization(Integer isPersonalization) {
    this.isPersonalization = isPersonalization;
  }


  public Integer getLateMinutesOrCounts() {
    return lateMinutesOrCounts;
  }
  public void setLateMinutesOrCounts(Integer lateMinutesOrCounts) {
    this.lateMinutesOrCounts = lateMinutesOrCounts;
  }


  public Integer getEarlyMinutesOrCounts() {
    return earlyMinutesOrCounts;
  }
  public void setEarlyMinutesOrCounts(Integer earlyMinutesOrCounts) {
    this.earlyMinutesOrCounts = earlyMinutesOrCounts;
  }


  public Integer getIsDefaultSetting() {
    return isDefaultSetting;
  }
  public void setIsDefaultSetting(Integer isDefaultSetting) {
    this.isDefaultSetting = isDefaultSetting;
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
