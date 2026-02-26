package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_abnormal_change_record")
public class HrmEmployeeAbnormalChangeRecord implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "change_record_id")
  private Long changeRecordId;
	/**
	 * 异动类型 1 新入职 2 离职 3 转正 4 调岗
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 异动员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 异动时间
	 */
  @Column(name = "change_time")
  private Date changeTime;
	/**
	 * 创建人id
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

  public Long getChangeRecordId() {
    return changeRecordId;
  }
  public void setChangeRecordId(Long changeRecordId) {
    this.changeRecordId = changeRecordId;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Date getChangeTime() {
    return changeTime;
  }
  public void setChangeTime(Date changeTime) {
    this.changeTime = changeTime;
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
