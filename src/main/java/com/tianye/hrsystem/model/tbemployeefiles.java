package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeefiles")
public class tbemployeefiles implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 证书名称
	 */
  @Column(name = "fileNam")
  private String fileNam;
	/**
	 * 证书级别
	 */
  @Column(name = "fileType")
  private String fileType;
	/**
	 * 证书编号
	 */
  @Column(name = "fileCode")
  private String fileCode;
	/**
	 * 有效期起始日期
	 */
  @Column(name = "beginDate")
  private Date beginDate;
	/**
	 * 有效期到期日期
	 */
  @Column(name = "endDate")
  private Date endDate;
	/**
	 * 发证机构
	 */
  @Column(name = "sendOrgName")
  private String sendOrgName;
	/**
	 * 发证日期
	 */
  @Column(name = "sendDate")
  private Date sendDate;
	/**
	 * 证书备注
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


  public String getFileNam() {
    return fileNam;
  }
  public void setFileNam(String fileNam) {
    this.fileNam = fileNam;
  }


  public String getFileType() {
    return fileType;
  }
  public void setFileType(String fileType) {
    this.fileType = fileType;
  }


  public String getFileCode() {
    return fileCode;
  }
  public void setFileCode(String fileCode) {
    this.fileCode = fileCode;
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


  public String getSendOrgName() {
    return sendOrgName;
  }
  public void setSendOrgName(String sendOrgName) {
    this.sendOrgName = sendOrgName;
  }


  public Date getSendDate() {
    return sendDate;
  }
  public void setSendDate(Date sendDate) {
    this.sendDate = sendDate;
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
