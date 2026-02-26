package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "admin_message")
public class AdminMessage implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 消息ID
	 */
  @Column(name = "message_id")
  private Long messageId;
	/**
	 * 消息标题
	 */
  @Column(name = "title")
  private String title;
	/**
	 * 内容
	 */
  @Column(name = "content")
  private String content;
	/**
	 * 消息大类 1 任务 2 日志 3 oa审批 4公告 5 日程 6 crm消息 7 知识库 8 人资
	 */
  @Column(name = "label")
  private Integer label;
	/**
	 * 消息类型 详见AdminMessageEnum
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 关联ID
	 */
  @Column(name = "type_id")
  private Long typeId;
	/**
	 * 消息创建者 0为系统
	 */
  @Column(name = "create_user")
  private Long createUser;
	/**
	 * 接收人
	 */
  @Column(name = "recipient_user")
  private Long recipientUser;
	/**
	 * 创建时间
	 */
  @Column(name = "create_time")
  private Date createTime;
	/**
	 * 是否已读 0 未读 1 已读
	 */
  @Column(name = "is_read")
  private Integer isRead;
	/**
	 * 已读时间
	 */
  @Column(name = "read_time")
  private Date readTime;
	/**
	 * 创建人ID
	 */
  @Column(name = "create_user_id")
  private Long createUserId;
	/**
	 * 更新时间
	 */
  @Column(name = "update_time")
  private Date updateTime;
	/**
	 * 修改人ID
	 */
  @Column(name = "update_user_id")
  private Long updateUserId;
	/**
	 * 审批类型
	 */
  @Column(name = "create_user_email")
  private String createUserEmail;

  public Long getMessageId() {
    return messageId;
  }
  public void setMessageId(Long messageId) {
    this.messageId = messageId;
  }


  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }


  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }


  public Integer getLabel() {
    return label;
  }
  public void setLabel(Integer label) {
    this.label = label;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }


  public Long getTypeId() {
    return typeId;
  }
  public void setTypeId(Long typeId) {
    this.typeId = typeId;
  }


  public Long getCreateUser() {
    return createUser;
  }
  public void setCreateUser(Long createUser) {
    this.createUser = createUser;
  }


  public Long getRecipientUser() {
    return recipientUser;
  }
  public void setRecipientUser(Long recipientUser) {
    this.recipientUser = recipientUser;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Integer getIsRead() {
    return isRead;
  }
  public void setIsRead(Integer isRead) {
    this.isRead = isRead;
  }


  public Date getReadTime() {
    return readTime;
  }
  public void setReadTime(Date readTime) {
    this.readTime = readTime;
  }


  public Long getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Long createUserId) {
    this.createUserId = createUserId;
  }


  public Date getUpdateTime() {
    return updateTime;
  }
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }


  public Long getUpdateUserId() {
    return updateUserId;
  }
  public void setUpdateUserId(Long updateUserId) {
    this.updateUserId = updateUserId;
  }


  public String getCreateUserEmail() {
    return createUserEmail;
  }
  public void setCreateUserEmail(String createUserEmail) {
    this.createUserEmail = createUserEmail;
  }

}
