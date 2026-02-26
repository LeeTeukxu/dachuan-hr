package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeecontract")
public class tbemployeecontract implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 合同编号
	 */
  @Column(name = "no")
  private String no;
	/**
	 * 合同类型
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 合同开始日期
	 */
  @Column(name = "beginDate")
  private Date beginDate;
	/**
	 * 合同结束日期
	 */
  @Column(name = "endDate")
  private Date endDate;
	/**
	 * 合同期限
	 */
  @Column(name = "limitTime")
  private String limitTime;
	/**
	 * 合同状态
	 */
  @Column(name = "status")
  private Integer status;
	/**
	 * 签约公司
	 */
  @Column(name = "signOrg")
  private String signOrg;
	/**
	 * 签约日期
	 */
  @Column(name = "signDate")
  private Date signDate;
	/**
	 * 备注
	 */
  @Column(name = "memo")
  private String memo;

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


  public String getNo() {
    return no;
  }
  public void setNo(String no) {
    this.no = no;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
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


  public String getLimitTime() {
    return limitTime;
  }
  public void setLimitTime(String limitTime) {
    this.limitTime = limitTime;
  }


  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }


  public String getSignOrg() {
    return signOrg;
  }
  public void setSignOrg(String signOrg) {
    this.signOrg = signOrg;
  }


  public Date getSignDate() {
    return signDate;
  }
  public void setSignDate(Date signDate) {
    this.signDate = signDate;
  }


  public String getMemo() {
    return memo;
  }
  public void setMemo(String memo) {
    this.memo = memo;
  }

}
