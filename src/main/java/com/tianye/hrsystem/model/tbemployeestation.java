package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeestation")
public class tbemployeestation implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 入职日期
	 */
  @Column(name = "entryDate")
  private Date entryDate;
	/**
	 * 是否有试用期
	 */
  @Column(name = "hasPeriod")
  private Integer hasPeriod;
	/**
	 * 转正日期
	 */
  @Column(name = "regularDate")
  private Date regularDate;
	/**
	 * 工号
	 */
  @Column(name = "workNo")
  private String workNo;
	/**
	 * 部门
	 */
  @Column(name = "depId")
  private Integer depId;
	/**
	 * 直属上级
	 */
  @Column(name = "leader")
  private String leader;
	/**
	 * 岗位
	 */
  @Column(name = "postion")
  private String postion;
	/**
	 * 职务
	 */
  @Column(name = "duty")
  private String duty;
	/**
	 * 工作地点
	 */
  @Column(name = "workPlace")
  private String workPlace;
	/**
	 * 具体地点
	 */
  @Column(name = "detailPlace")
  private String detailPlace;
	/**
	 * 工作城市
	 */
  @Column(name = "workCity")
  private String workCity;
	/**
	 * 招聘渠道
	 */
  @Column(name = "inviteType")
  private String inviteType;
	/**
	 * 聘用形式
	 */
  @Column(name = "enageType")
  private Integer enageType;
	/**
	 * 员工状态
	 */
  @Column(name = "status")
  private Integer status;
	/**
	 * 司龄开始日期
	 */
  @Column(name = "workPeriodBegin")
  private Date workPeriodBegin;
	/**
	 * 司龄
	 */
  @Column(name = "workPeriod")
  private String workPeriod;
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


  public Date getEntryDate() {
    return entryDate;
  }
  public void setEntryDate(Date entryDate) {
    this.entryDate = entryDate;
  }


  public Integer getHasPeriod() {
    return hasPeriod;
  }
  public void setHasPeriod(Integer hasPeriod) {
    this.hasPeriod = hasPeriod;
  }


  public Date getRegularDate() {
    return regularDate;
  }
  public void setRegularDate(Date regularDate) {
    this.regularDate = regularDate;
  }


  public String getWorkNo() {
    return workNo;
  }
  public void setWorkNo(String workNo) {
    this.workNo = workNo;
  }


  public Integer getDepId() {
    return depId;
  }
  public void setDepId(Integer depId) {
    this.depId = depId;
  }


  public String getLeader() {
    return leader;
  }
  public void setLeader(String leader) {
    this.leader = leader;
  }


  public String getPostion() {
    return postion;
  }
  public void setPostion(String postion) {
    this.postion = postion;
  }


  public String getDuty() {
    return duty;
  }
  public void setDuty(String duty) {
    this.duty = duty;
  }


  public String getWorkPlace() {
    return workPlace;
  }
  public void setWorkPlace(String workPlace) {
    this.workPlace = workPlace;
  }


  public String getDetailPlace() {
    return detailPlace;
  }
  public void setDetailPlace(String detailPlace) {
    this.detailPlace = detailPlace;
  }


  public String getWorkCity() {
    return workCity;
  }
  public void setWorkCity(String workCity) {
    this.workCity = workCity;
  }


  public String getInviteType() {
    return inviteType;
  }
  public void setInviteType(String inviteType) {
    this.inviteType = inviteType;
  }


  public Integer getEnageType() {
    return enageType;
  }
  public void setEnageType(Integer enageType) {
    this.enageType = enageType;
  }


  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }


  public Date getWorkPeriodBegin() {
    return workPeriodBegin;
  }
  public void setWorkPeriodBegin(Date workPeriodBegin) {
    this.workPeriodBegin = workPeriodBegin;
  }


  public String getWorkPeriod() {
    return workPeriod;
  }
  public void setWorkPeriod(String workPeriod) {
    this.workPeriod = workPeriod;
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
