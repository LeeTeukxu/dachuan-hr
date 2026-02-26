package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbattachment")
public class tbattachment implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 主键，主键+后缀保存文件
	 */
  @Column(name = "id")
  private String id;
	/**
	 * 原始名称
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 大小
	 */
  @Column(name = "size")
  private Integer size;
	/**
	 * 不带.后缀
	 */
  @Column(name = "ext")
  private String ext;
	/**
	 * 附件分类
	 */
  @Column(name = "type")
  private String type;
	/**
	 * 保存路径
	 */
  @Column(name = "savePath")
  private String savePath;
  @Column(name = "createTime")
  private Date createTime;
  @Column(name = "createMan")
  private Integer createMan;
  @Column(name = "createManName")
  private String createManName;

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public Integer getSize() {
    return size;
  }
  public void setSize(Integer size) {
    this.size = size;
  }


  public String getExt() {
    return ext;
  }
  public void setExt(String ext) {
    this.ext = ext;
  }


  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }


  public String getSavePath() {
    return savePath;
  }
  public void setSavePath(String savePath) {
    this.savePath = savePath;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Integer getCreateMan() {
    return createMan;
  }
  public void setCreateMan(Integer createMan) {
    this.createMan = createMan;
  }


  public String getCreateManName() {
    return createManName;
  }
  public void setCreateManName(String createManName) {
    this.createManName = createManName;
  }

}
