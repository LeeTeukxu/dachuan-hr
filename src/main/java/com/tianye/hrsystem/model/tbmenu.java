package com.tianye.hrsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "tbmenu")
public class tbmenu implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "pid")
  private Integer pid;
  @Column(name = "name")
  private String name;
  @Column(name = "path")
  private String path;
  @Column(name = "canUse")
  private Integer canUse;
  @Column(name = "component")
  private String component;
  @Column(name = "redirect")
  private String redirect;
  @Column(name = "icon")
  private String icon;
  @Column(name = "showMenu")
  private String showMenu;
  @Transient
  private List<tbmenu> children;

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

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getCanUse() {
    return canUse;
  }

  public void setCanUse(Integer canUse) {
    this.canUse = canUse;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public String getRedirect() {
    return redirect;
  }

  public void setRedirect(String redirect) {
    this.redirect = redirect;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getShowMenu() {
    return showMenu;
  }

  public void setShowMenu(String showMenu) {
    this.showMenu = showMenu;
  }

  public List<tbmenu> getChildren() {
    return children;
  }

  public void setChildren(List<tbmenu> children) {
    this.children = children;
  }
}
