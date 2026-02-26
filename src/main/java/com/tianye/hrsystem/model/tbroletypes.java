package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbroletypes")
public class tbroletypes implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "pid")
  private Integer pid;
  @Column(name = "name")
  private String name;
  @Column(name = "canUse")
  private Integer canUse;

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

}
