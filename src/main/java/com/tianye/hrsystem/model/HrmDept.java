package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 注释
 */
@Entity
@Table(name = "hrm_dept")
public class HrmDept implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "dept_id")
  private Long deptId;
	/**
	 * 父级ID 顶级部门为0
	 */
  @Column(name = "parent_id")
  private Long parentId;
	/**
	 * 1 公司 2 部门
	 */
  @Column(name = "dept_type")
  private Integer deptType;
	/**
	 * 部门名称
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 部门编码
	 */
  @Column(name = "code")
  private String code;
	/**
	 * 部门负责人ID
	 */
  @Column(name = "main_employee_id")
  private Long mainEmployeeId;
	/**
	 * 分管领导
	 */
  @Column(name = "leader_employee_id")
  private Long leaderEmployeeId;
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

  public Long getDeptId() {
    return deptId;
  }
  public void setDeptId(Long deptId) {
    this.deptId = deptId;
  }


  public Long getParentId() {
    return parentId;
  }
  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }


  public Integer getDeptType() {
    return deptType;
  }
  public void setDeptType(Integer deptType) {
    this.deptType = deptType;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getCode() {
    return code;
  }
  public void setCode(String code) {
    this.code = code;
  }


  public Long getMainEmployeeId() {
    return mainEmployeeId;
  }
  public void setMainEmployeeId(Long mainEmployeeId) {
    this.mainEmployeeId = mainEmployeeId;
  }


  public Long getLeaderEmployeeId() {
    return leaderEmployeeId;
  }
  public void setLeaderEmployeeId(Long leaderEmployeeId) {
    this.leaderEmployeeId = leaderEmployeeId;
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
