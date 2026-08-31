package com.tianye.hrsystem.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbrolemenu")
public class tbrolemenu implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "role_menu_id")
  private Long roleMenuId;
  @Column(name = "role_id")
  private Integer roleId;
  @Column(name = "menu_id")
  private Integer menuId;

  public Long getRoleMenuId() {
    return roleMenuId;
  }

  public void setRoleMenuId(Long roleMenuId) {
    this.roleMenuId = roleMenuId;
  }

  public Integer getRoleId() {
    return roleId;
  }

  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
  }

  public Integer getMenuId() {
    return menuId;
  }

  public void setMenuId(Integer menuId) {
    this.menuId = menuId;
  }
}
