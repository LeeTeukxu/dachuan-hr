package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeetrain")
public class tbemployeetrain implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 培训课程
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 培训机构名称
	 */
  @Column(name = "orgName")
  private String orgName;
	/**
	 * 开始日期
	 */
  @Column(name = "beginDate")
  private Date beginDate;
	/**
	 * 结束日期
	 */
  @Column(name = "endDate")
  private Date endDate;
	/**
	 * 培训时长
	 */
  @Column(name = "duration")
  private String duration;
	/**
	 * 培训成绩
	 */
  @Column(name = "result")
  private String result;
	/**
	 * 培训证书名称
	 */
  @Column(name = "reportName")
  private String reportName;
	/**
	 * 备注
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


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getOrgName() {
    return orgName;
  }
  public void setOrgName(String orgName) {
    this.orgName = orgName;
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


  public String getDuration() {
    return duration;
  }
  public void setDuration(String duration) {
    this.duration = duration;
  }


  public String getResult() {
    return result;
  }
  public void setResult(String result) {
    this.result = result;
  }


  public String getReportName() {
    return reportName;
  }
  public void setReportName(String reportName) {
    this.reportName = reportName;
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
