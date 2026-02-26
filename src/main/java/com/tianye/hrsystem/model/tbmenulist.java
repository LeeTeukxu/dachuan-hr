package com.tianye.hrsystem.model;

import java.util.ArrayList;
import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "tbmenulist")
public class tbmenulist implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "pid")
  private Integer pid;
  @Column(name = "sn")
  private Integer sn;
  @Column(name = "name")
  private String name;
  @Column(name = "url")
  private String url;
  @Column(name = "icon")
  private String icon;
  @Column(name = "canUse")
  private Integer canUse;
  @Column(name = "createTime")
  private Date createTime;

  @Transient
  private List<tbmenulist> children;

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


  public Integer getSn() {
    return sn;
  }
  public void setSn(Integer sn) {
    this.sn = sn;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }


  public String getIcon() {
    return icon;
  }
  public void setIcon(String icon) {
    this.icon = icon;
  }


  public Integer getCanUse() {
    return canUse;
  }
  public void setCanUse(Integer canUse) {
    this.canUse = canUse;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public List<tbmenulist> getChildren() {
    return children;
  }

  public void setChildren(List<tbmenulist> children) {
    this.children = children;
  }
}
