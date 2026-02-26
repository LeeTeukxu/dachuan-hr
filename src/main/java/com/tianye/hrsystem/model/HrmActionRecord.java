package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_action_record")
public class HrmActionRecord implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
	/**
	 * ip地址
	 */
  @Column(name = "ip_address")
  private String ipAddress;
	/**
	 * 操作类型 1 员工 2 招聘管理 3 候选人 4 绩效管理
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 操作对象id
	 */
  @Column(name = "type_id")
  private Integer typeId;
	/**
	 * 操作行为 1 新建 2 编辑 3 删除 4 转正 5 调岗 6 晋升 7 降级 8 转全职员工 9 离职 10 参保方案
	 */
  @Column(name = "behavior")
  private Integer behavior;
	/**
	 * 内容
	 */
  @Column(name = "content")
  private String content;
	/**
	 * 翻译内容
	 */
  @Column(name = "trans_content")
  private String transContent;
	/**
	 * 操作人ID
	 */
  @Column(name = "create_user_id")
  private Integer createUserId;
	/**
	 * 创建时间
	 */
  @Column(name = "create_time")
  private Date createTime;
	/**
	 * 更新人id
	 */
  @Column(name = "update_user_id")
  private Integer updateUserId;
	/**
	 * 更新时间
	 */
  @Column(name = "update_time")
  private Date updateTime;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public String getIpAddress() {
    return ipAddress;
  }
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }


  public Integer getTypeId() {
    return typeId;
  }
  public void setTypeId(Integer typeId) {
    this.typeId = typeId;
  }


  public Integer getBehavior() {
    return behavior;
  }
  public void setBehavior(Integer behavior) {
    this.behavior = behavior;
  }


  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }


  public String getTransContent() {
    return transContent;
  }
  public void setTransContent(String transContent) {
    this.transContent = transContent;
  }


  public Integer getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Integer getUpdateUserId() {
    return updateUserId;
  }
  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }


  public Date getUpdateTime() {
    return updateTime;
  }
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

}
