package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_contacts")
public class HrmEmployeeContacts implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "contacts_id")
  private Long contactsId;
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 联系人名称
	 */
  @Column(name = "contacts_name")
  private String contactsName;
	/**
	 * 关系
	 */
  @Column(name = "relation")
  private String relation;
	/**
	 * 联系人电话
	 */
  @Column(name = "contacts_phone")
  private String contactsPhone;
	/**
	 * 联系人工作单位
	 */
  @Column(name = "contacts_work_unit")
  private String contactsWorkUnit;
	/**
	 * 联系儿职务
	 */
  @Column(name = "contacts_post")
  private String contactsPost;
	/**
	 * 联系人地址
	 */
  @Column(name = "contacts_address")
  private String contactsAddress;
  @Column(name = "sort")
  private Integer sort;
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

  public Long getContactsId() {
    return contactsId;
  }
  public void setContactsId(Long contactsId) {
    this.contactsId = contactsId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getContactsName() {
    return contactsName;
  }
  public void setContactsName(String contactsName) {
    this.contactsName = contactsName;
  }


  public String getRelation() {
    return relation;
  }
  public void setRelation(String relation) {
    this.relation = relation;
  }


  public String getContactsPhone() {
    return contactsPhone;
  }
  public void setContactsPhone(String contactsPhone) {
    this.contactsPhone = contactsPhone;
  }


  public String getContactsWorkUnit() {
    return contactsWorkUnit;
  }
  public void setContactsWorkUnit(String contactsWorkUnit) {
    this.contactsWorkUnit = contactsWorkUnit;
  }


  public String getContactsPost() {
    return contactsPost;
  }
  public void setContactsPost(String contactsPost) {
    this.contactsPost = contactsPost;
  }


  public String getContactsAddress() {
    return contactsAddress;
  }
  public void setContactsAddress(String contactsAddress) {
    this.contactsAddress = contactsAddress;
  }


  public Integer getSort() {
    return sort;
  }
  public void setSort(Integer sort) {
    this.sort = sort;
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
