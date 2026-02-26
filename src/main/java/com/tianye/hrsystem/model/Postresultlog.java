package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "postresultlog")
public class Postresultlog implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "postUrl")
  private String postUrl;
  @Column(name = "begin")
  private Date begin;
  @Column(name = "end")
  private Date end;
  @Column(name = "content")
  private String content;
  @Column(name = "createTime")
  private Date createTime;
  @Column(name = "className")
  private String className;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public String getPostUrl() {
    return postUrl;
  }
  public void setPostUrl(String postUrl) {
    this.postUrl = postUrl;
  }


  public Date getBegin() {
    return begin;
  }
  public void setBegin(Date begin) {
    this.begin = begin;
  }


  public Date getEnd() {
    return end;
  }
  public void setEnd(Date end) {
    this.end = end;
  }


  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public String getClassName() {
    return className;
  }
  public void setClassName(String className) {
    this.className = className;
  }

}
