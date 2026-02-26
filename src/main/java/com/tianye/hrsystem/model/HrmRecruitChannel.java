package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_recruit_channel")
public class HrmRecruitChannel implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "channel_id")
  private Integer channelId;
	/**
	 * 是否系统默认0 否 1 是
	 */
  @Column(name = "is_sys")
  private Integer isSys;
	/**
	 * 状态 0 禁用 1 启用
	 */
  @Column(name = "status")
  private Integer status;
  @Column(name = "value")
  private String value;
	/**
	 * 创建人id
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

  public Integer getChannelId() {
    return channelId;
  }
  public void setChannelId(Integer channelId) {
    this.channelId = channelId;
  }


  public Integer getIsSys() {
    return isSys;
  }
  public void setIsSys(Integer isSys) {
    this.isSys = isSys;
  }


  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }


  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
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
