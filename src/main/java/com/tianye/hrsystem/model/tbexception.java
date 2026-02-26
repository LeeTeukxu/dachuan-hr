package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbexception")
public class tbexception implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "className")
  private String className;
  @Column(name = "exception")
  private String exception;
  @Column(name = "message")
  private String message;
  @Column(name = "detail")
  private String detail;
  @Column(name = "createtime")
  private Date createtime;

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


  public String getException() {
    return exception;
  }
  public void setException(String exception) {
    this.exception = exception;
  }


  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }


  public String getDetail() {
    return detail;
  }
  public void setDetail(String detail) {
    this.detail = detail;
  }


  public Date getCreatetime() {
    return createtime;
  }
  public void setCreatetime(Date createtime) {
    this.createtime = createtime;
  }

}
