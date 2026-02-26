package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbplanlist")
public class tbplanlist implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "ID")
  private Integer id;
  @Column(name = "ProductName")
  private String productName;
  @Column(name = "LinkName")
  private String linkName;
  @Column(name = "WorkDate")
  private Date workDate;
  @Column(name = "GroupID")
  private String groupId;
  @Column(name = "ClassID")
  private String classId;
  @Column(name = "UserID")
  private String userId;
  @Column(name = "CreateTime")
  private Date createTime;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public String getProductName() {
    return productName;
  }
  public void setProductName(String productName) {
    this.productName = productName;
  }


  public String getLinkName() {
    return linkName;
  }
  public void setLinkName(String linkName) {
    this.linkName = linkName;
  }


  public Date getWorkDate() {
    return workDate;
  }
  public void setWorkDate(Date workDate) {
    this.workDate = workDate;
  }


  public String getGroupId() {
    return groupId;
  }
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }


  public String getClassId() {
    return classId;
  }
  public void setClassId(String classId) {
    this.classId = classId;
  }


  public String getUserId() {
    return userId;
  }
  public void setUserId(String userId) {
    this.userId = userId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

}
