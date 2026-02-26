package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbloginuser")
public class tbloginuser implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
	/**
	 * 姓名
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 登录帐号
	 */
  @Column(name = "account")
  private String account;
	/**
	 * 登录密码
	 */
  @Column(name = "password")
  private String password;
	/**
	 * 所在部门
	 */
  @Column(name = "depId")
  private Integer depId;
	/**
	 * 角色
	 */
  @Column(name = "roleId")
  private Integer roleId;
	/**
	 * 创建时间
	 */
  @Column(name = "createtime")
  private Date createtime;
	/**
	 * 是否可以登录
	 */
  @Column(name = "canLogin")
  private Integer canLogin;
	/**
	 * 登录次数
	 */
  @Column(name = "loginCount")
  private Integer loginCount;
	/**
	 * 最后登录日期
	 */
  @Column(name = "lastLoginTime")
  private Date lastLoginTime;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getAccount() {
    return account;
  }
  public void setAccount(String account) {
    this.account = account;
  }


  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }


  public Integer getDepId() {
    return depId;
  }
  public void setDepId(Integer depId) {
    this.depId = depId;
  }


  public Integer getRoleId() {
    return roleId;
  }
  public void setRoleId(Integer roleId) {
    this.roleId = roleId;
  }


  public Date getCreatetime() {
    return createtime;
  }
  public void setCreatetime(Date createtime) {
    this.createtime = createtime;
  }


  public Integer getCanLogin() {
    return canLogin;
  }
  public void setCanLogin(Integer canLogin) {
    this.canLogin = canLogin;
  }


  public Integer getLoginCount() {
    return loginCount;
  }
  public void setLoginCount(Integer loginCount) {
    this.loginCount = loginCount;
  }


  public Date getLastLoginTime() {
    return lastLoginTime;
  }
  public void setLastLoginTime(Date lastLoginTime) {
    this.lastLoginTime = lastLoginTime;
  }

}
