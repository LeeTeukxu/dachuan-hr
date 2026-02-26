package com.tianye.hrsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "updaterecord")
public class updateRecord implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 主键，主键+后缀保存文件
	 */
  @Column(name = "id")
  private String id;

  @Column(name="mainKey")
  private String mainKey;
  @Column(name="subKey")
  private String subKey;

  @Column(name="value")
  private String value;
  @Column(name = "createTime")
  private Date createTime;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMainKey() {
    return mainKey;
  }

  public void setMainKey(String mainKey) {
    this.mainKey = mainKey;
  }

  public String getSubKey() {
    return subKey;
  }

  public void setSubKey(String subKey) {
    this.subKey = subKey;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }
}
