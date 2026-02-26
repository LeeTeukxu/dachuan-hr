package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeework")
public class tbemployeework implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 工作单位
	 */
  @Column(name = "companyName")
  private String companyName;
	/**
	 * 职务
	 */
  @Column(name = "duty")
  private String duty;
	/**
	 * 工作开始日期
	 */
  @Column(name = "beginDate")
  private Date beginDate;
	/**
	 * 工作结束日期
	 */
  @Column(name = "endDate")
  private Date endDate;
	/**
	 * 离职日期
	 */
  @Column(name = "leaveReason")
  private String leaveReason;
	/**
	 * 证明人
	 */
  @Column(name = "proveManName")
  private String proveManName;
	/**
	 * 证明人电话
	 */
  @Column(name = "proveManPhone")
  private String proveManPhone;
	/**
	 * 工作备注
	 */
  @Column(name = "memo")
  private String memo;
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


  public String getCompanyName() {
    return companyName;
  }
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }


  public String getDuty() {
    return duty;
  }
  public void setDuty(String duty) {
    this.duty = duty;
  }


  public Date getBeginDate() {
    return beginDate;
  }
  public void setBeginDate(Date beginDate) {
    this.beginDate = beginDate;
  }


  public Date getEndDate() {
    return endDate;
  }
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }


  public String getLeaveReason() {
    return leaveReason;
  }
  public void setLeaveReason(String leaveReason) {
    this.leaveReason = leaveReason;
  }


  public String getProveManName() {
    return proveManName;
  }
  public void setProveManName(String proveManName) {
    this.proveManName = proveManName;
  }


  public String getProveManPhone() {
    return proveManPhone;
  }
  public void setProveManPhone(String proveManPhone) {
    this.proveManPhone = proveManPhone;
  }


  public String getMemo() {
    return memo;
  }
  public void setMemo(String memo) {
    this.memo = memo;
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
