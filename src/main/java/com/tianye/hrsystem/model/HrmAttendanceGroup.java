package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_group")
public class HrmAttendanceGroup implements Serializable {
  @Id
	/**
	 * 考勤组id
	 */
  @Column(name = "attendance_group_id")
  private Long attendanceGroupId;
	/**
	 * 考勤组初始化id
	 */
  @Column(name = "old_group_id")
  private Long oldGroupId;
	/**
	 * 名称
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 工作时长
	 */
  @Column(name = "daily_time")
  private Double dailyTime;
	/**
	 * 扣款规则id
	 */
  @Column(name = "attendance_rule_id")
  private Long attendanceRuleId;
	/**
	 * 是否开启wifi打卡
	 */
  @Column(name = "is_open_wifi_card")
  private Integer isOpenWifiCard;
	/**
	 * 是否开启定位打卡
	 */
  @Column(name = "is_open_point_card")
  private Integer isOpenPointCard;
	/**
	 * 是否自动打卡（0否 1是）
	 */
  @Column(name = "is_auto_card")
  private Integer isAutoCard;
	/**
	 * 考勤班组设置
	 */
  @Column(name = "shift_setting")
  private String shiftSetting;
	/**
	 * 是否法定节假日休息（0否 1是）
	 */
  @Column(name = "is_rest")
  private Integer isRest;
	/**
	 * 特殊日期设置
	 */
  @Column(name = "special_date_setting")
  private String specialDateSetting;
	/**
	 * 是否是默认配置（0否 1是）
	 */
  @Column(name = "is_default_setting")
  private Integer isDefaultSetting;
	/**
	 * 是否历史配置 (0否 1是)
	 */
  @Column(name = "old_setting")
  private Integer oldSetting;
	/**
	 * 生效时间
	 */
  @Column(name = "effect_time")
  private Date effectTime;
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

  public Long getAttendanceGroupId() {
    return attendanceGroupId;
  }
  public void setAttendanceGroupId(Long attendanceGroupId) {
    this.attendanceGroupId = attendanceGroupId;
  }


  public Long getOldGroupId() {
    return oldGroupId;
  }
  public void setOldGroupId(Long oldGroupId) {
    this.oldGroupId = oldGroupId;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public Double getDailyTime() {
    return dailyTime;
  }
  public void setDailyTime(Double dailyTime) {
    this.dailyTime = dailyTime;
  }


  public Long getAttendanceRuleId() {
    return attendanceRuleId;
  }
  public void setAttendanceRuleId(Long attendanceRuleId) {
    this.attendanceRuleId = attendanceRuleId;
  }


  public Integer getIsOpenWifiCard() {
    return isOpenWifiCard;
  }
  public void setIsOpenWifiCard(Integer isOpenWifiCard) {
    this.isOpenWifiCard = isOpenWifiCard;
  }


  public Integer getIsOpenPointCard() {
    return isOpenPointCard;
  }
  public void setIsOpenPointCard(Integer isOpenPointCard) {
    this.isOpenPointCard = isOpenPointCard;
  }


  public Integer getIsAutoCard() {
    return isAutoCard;
  }
  public void setIsAutoCard(Integer isAutoCard) {
    this.isAutoCard = isAutoCard;
  }


  public String getShiftSetting() {
    return shiftSetting;
  }
  public void setShiftSetting(String shiftSetting) {
    this.shiftSetting = shiftSetting;
  }


  public Integer getIsRest() {
    return isRest;
  }
  public void setIsRest(Integer isRest) {
    this.isRest = isRest;
  }


  public String getSpecialDateSetting() {
    return specialDateSetting;
  }
  public void setSpecialDateSetting(String specialDateSetting) {
    this.specialDateSetting = specialDateSetting;
  }


  public Integer getIsDefaultSetting() {
    return isDefaultSetting;
  }
  public void setIsDefaultSetting(Integer isDefaultSetting) {
    this.isDefaultSetting = isDefaultSetting;
  }


  public Integer getOldSetting() {
    return oldSetting;
  }
  public void setOldSetting(Integer oldSetting) {
    this.oldSetting = oldSetting;
  }


  public Date getEffectTime() {
    return effectTime;
  }
  public void setEffectTime(Date effectTime) {
    this.effectTime = effectTime;
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
