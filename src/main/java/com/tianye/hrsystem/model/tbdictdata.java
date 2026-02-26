package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbdictdata")
public class tbdictdata implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "pid")
  private Integer pid;
  @Column(name = "dtid")
  private Integer dtid;
  @Column(name = "sn")
  private String sn;
  @Column(name = "name")
  private String name;
  @Column(name = "canUse")
  private Integer canUse;
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


  public Integer getPid() {
    return pid;
  }
  public void setPid(Integer pid) {
    this.pid = pid;
  }


  public Integer getDtid() {
    return dtid;
  }
  public void setDtid(Integer dtid) {
    this.dtid = dtid;
  }


  public String getSn() {
    return sn;
  }
  public void setSn(String sn) {
    this.sn = sn;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public Integer getCanUse() {
    return canUse;
  }
  public void setCanUse(Integer canUse) {
    this.canUse = canUse;
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
