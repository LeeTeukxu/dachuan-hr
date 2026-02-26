package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_group_relation_dept")
public class HrmAttendanceGroupRelationDept implements Serializable {
  @Id
  @Column(name = "attendance_group_relation_dept_id")
  private Long attendanceGroupRelationDeptId;
	/**
	 * 考勤组id
	 */
  @Column(name = "attendance_group_id")
  private Long attendanceGroupId;
	/**
	 * 部门id
	 */
  @Column(name = "dept_id")
  private Long deptId;
	/**
	 * 生效时间
	 */
  @Column(name = "effect_time")
  private Date effectTime;
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

  public Long getAttendanceGroupRelationDeptId() {
    return attendanceGroupRelationDeptId;
  }
  public void setAttendanceGroupRelationDeptId(Long attendanceGroupRelationDeptId) {
    this.attendanceGroupRelationDeptId = attendanceGroupRelationDeptId;
  }


  public Long getAttendanceGroupId() {
    return attendanceGroupId;
  }
  public void setAttendanceGroupId(Long attendanceGroupId) {
    this.attendanceGroupId = attendanceGroupId;
  }


  public Long getDeptId() {
    return deptId;
  }
  public void setDeptId(Long deptId) {
    this.deptId = deptId;
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
