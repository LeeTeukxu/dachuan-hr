package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_change_record")
public class HrmEmployeeChangeRecord implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "record_id")
  private Long recordId;
	/**
	 * 员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 变动类型 4 转正 5调岗 6晋升 7降级 8转为全职员工
	 */
  @Column(name = "change_type")
  private Integer changeType;
	/**
	 * 异动原因 1 组织架构调整 2个人申请 3 工作安排 4 违规违纪 5 绩效不达标 6 个人身体原因 7 不适应当前岗位
	 */
  @Column(name = "change_reason")
  private Integer changeReason;
	/**
	 * 原部门
	 */
  @Column(name = "old_dept")
  private Long oldDept;
	/**
	 * 新部门
	 */
  @Column(name = "new_dept")
  private Long newDept;
	/**
	 * 原岗位
	 */
  @Column(name = "old_post")
  private String oldPost;
	/**
	 * 新岗位
	 */
  @Column(name = "new_post")
  private String newPost;
	/**
	 * 新职级
	 */
  @Column(name = "old_post_level")
  private String oldPostLevel;
	/**
	 * 新职级
	 */
  @Column(name = "new_post_level")
  private String newPostLevel;
	/**
	 * 原工作地点
	 */
  @Column(name = "old_work_address")
  private String oldWorkAddress;
	/**
	 * 新工作地点
	 */
  @Column(name = "new_work_address")
  private String newWorkAddress;
	/**
	 * 原直属上级
	 */
  @Column(name = "old_parent_id")
  private Long oldParentId;
	/**
	 * 新直属上级
	 */
  @Column(name = "new_parent_id")
  private Long newParentId;
	/**
	 * 试用期
	 */
  @Column(name = "probation")
  private Integer probation;
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

  public Long getRecordId() {
    return recordId;
  }
  public void setRecordId(Long recordId) {
    this.recordId = recordId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Integer getChangeType() {
    return changeType;
  }
  public void setChangeType(Integer changeType) {
    this.changeType = changeType;
  }


  public Integer getChangeReason() {
    return changeReason;
  }
  public void setChangeReason(Integer changeReason) {
    this.changeReason = changeReason;
  }


  public Long getOldDept() {
    return oldDept;
  }
  public void setOldDept(Long oldDept) {
    this.oldDept = oldDept;
  }


  public Long getNewDept() {
    return newDept;
  }
  public void setNewDept(Long newDept) {
    this.newDept = newDept;
  }


  public String getOldPost() {
    return oldPost;
  }
  public void setOldPost(String oldPost) {
    this.oldPost = oldPost;
  }


  public String getNewPost() {
    return newPost;
  }
  public void setNewPost(String newPost) {
    this.newPost = newPost;
  }


  public String getOldPostLevel() {
    return oldPostLevel;
  }
  public void setOldPostLevel(String oldPostLevel) {
    this.oldPostLevel = oldPostLevel;
  }


  public String getNewPostLevel() {
    return newPostLevel;
  }
  public void setNewPostLevel(String newPostLevel) {
    this.newPostLevel = newPostLevel;
  }


  public String getOldWorkAddress() {
    return oldWorkAddress;
  }
  public void setOldWorkAddress(String oldWorkAddress) {
    this.oldWorkAddress = oldWorkAddress;
  }


  public String getNewWorkAddress() {
    return newWorkAddress;
  }
  public void setNewWorkAddress(String newWorkAddress) {
    this.newWorkAddress = newWorkAddress;
  }


  public Long getOldParentId() {
    return oldParentId;
  }
  public void setOldParentId(Long oldParentId) {
    this.oldParentId = oldParentId;
  }


  public Long getNewParentId() {
    return newParentId;
  }
  public void setNewParentId(Long newParentId) {
    this.newParentId = newParentId;
  }


  public Integer getProbation() {
    return probation;
  }
  public void setProbation(Integer probation) {
    this.probation = probation;
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
