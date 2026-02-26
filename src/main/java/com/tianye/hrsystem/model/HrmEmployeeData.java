package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_data")
public class HrmEmployeeData implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;
  @Column(name = "field_id")
  private Long fieldId;
  @Column(name = "label_group")
  private Integer labelGroup;
	/**
	 * 字段名称
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 字段值
	 */
  @Column(name = "field_value")
  private String fieldValue;
	/**
	 * 字段值描述
	 */
  @Column(name = "field_value_desc")
  private String fieldValueDesc;
	/**
	 * employee_id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
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

  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }


  public Long getFieldId() {
    return fieldId;
  }
  public void setFieldId(Long fieldId) {
    this.fieldId = fieldId;
  }


  public Integer getLabelGroup() {
    return labelGroup;
  }
  public void setLabelGroup(Integer labelGroup) {
    this.labelGroup = labelGroup;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getFieldValue() {
    return fieldValue;
  }
  public void setFieldValue(String fieldValue) {
    this.fieldValue = fieldValue;
  }


  public String getFieldValueDesc() {
    return fieldValueDesc;
  }
  public void setFieldValueDesc(String fieldValueDesc) {
    this.fieldValueDesc = fieldValueDesc;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
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
