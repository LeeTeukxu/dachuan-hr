package com.tianye.hrsystem.model.ddTalk;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ddtaskresult")
public class Ddtaskresult implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "className")
  private String className;
  @Column(name = "content")
  private String content;
  @Column(name = "processed")
  private Integer processed;
  @Column(name = "createtime")
  private Date createtime;
  @Column(name = "processtime")
  private Date processtime;
  @Column(name = "success")
  private Integer success;
  @Column(name = "result")
  private String result;
  @Column(name="empId")
  private Long empId;
  @Column(name="userId")
  private String userId;  // 【修复重名员工问题】：添加钉钉userId字段，用于区分重名员工
  @Column(name="companyId")
  private String companyId;
  @Column(name="begin")
  private String begin;
  @Column(name="end")
  private String end;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public String getClassName() {
    return className;
  }
  public void setClassName(String className) {
    this.className = className;
  }


  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }


  public Integer getProcessed() {
    return processed;
  }
  public void setProcessed(Integer processed) {
    this.processed = processed;
  }


  public Date getCreatetime() {
    return createtime;
  }
  public void setCreatetime(Date createtime) {
    this.createtime = createtime;
  }


  public Date getProcesstime() {
    return processtime;
  }
  public void setProcesstime(Date processtime) {
    this.processtime = processtime;
  }


  public Integer getSuccess() {
    return success;
  }
  public void setSuccess(Integer success) {
    this.success = success;
  }


  public String getResult() {
    return result;
  }
  public void setResult(String result) {
    this.result = result;
  }

  public Long getEmpId() {
    return empId;
  }

  public void setEmpId(Long empId) {
    this.empId = empId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getBegin() {
    return begin;
  }

  public void setBegin(String begin) {
    this.begin = begin;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }
}
