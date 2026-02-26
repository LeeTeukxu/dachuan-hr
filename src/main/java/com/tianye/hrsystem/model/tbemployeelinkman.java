package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeelinkman")
public class tbemployeelinkman implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 联系人姓名
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 关系
	 */
  @Column(name = "relation")
  private String relation;
	/**
	 * 联系电话
	 */
  @Column(name = "phone")
  private String phone;
	/**
	 * 工作单位
	 */
  @Column(name = "orgName")
  private String orgName;
	/**
	 * 职务
	 */
  @Column(name = "duty")
  private String duty;
	/**
	 * 地址
	 */
  @Column(name = "address")
  private String address;
  @Column(name = "createMan")
  private Integer createMan;
  @Column(name = "createTime")
  private Date createTime;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public Integer getEmpId() {
    return empId;
  }
  public void setEmpId(Integer empId) {
    this.empId = empId;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getRelation() {
    return relation;
  }
  public void setRelation(String relation) {
    this.relation = relation;
  }


  public String getPhone() {
    return phone;
  }
  public void setPhone(String phone) {
    this.phone = phone;
  }


  public String getOrgName() {
    return orgName;
  }
  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }


  public String getDuty() {
    return duty;
  }
  public void setDuty(String duty) {
    this.duty = duty;
  }


  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }


  public Integer getCreateMan() {
    return createMan;
  }
  public void setCreateMan(Integer createMan) {
    this.createMan = createMan;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

}
