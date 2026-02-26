package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbattendanceapprove")
public class tbattendanceapprove implements Serializable {
  @Id
  @Column(name = "id")
  private String id;

  /**
   请假类型
   **/
  @Column(name = "subType")
  private String subType;

  /**
   *请假,出差,外出,加班
   **/
  @Column(name = "tagName")
  private String tagName;

  /**
   1：加班
   2：出差/外出
   3：请假
   **/
  @Column(name = "bizType")
  private Long bizType;
  /**
  开始时间
   **/
  @Column(name = "beginTime")
  private Date beginTime;

  /**
    结束时间
   **/
  @Column(name = "endTime")
  private Date endTime;

  /**
    请假单位
   **/
  @Column(name = "durationUnit")
  private String durationUnit;
  @Column(name = "userId")
  private String  userId;
  @Column(name = "groupId")
  private Long groupId;
  @Column(name = "createTime")
  private Date createTime;
  /**
   时长
   */
  @Column(name="duration")
  private String duration;

  /**
  申请日期
   **/
  @Column(name="workDate")
  private Date workDate;

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }


  public String getSubType() {
    return subType;
  }
  public void setSubType(String subType) {
    this.subType = subType;
  }


  public String getTagName() {
    return tagName;
  }
  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  public Long getBizType() {
    return bizType;
  }

  public void setBizType(Long bizType) {
    this.bizType = bizType;
  }

  public Date getBeginTime() {
    return beginTime;
  }
  public void setBeginTime(Date beginTime) {
    this.beginTime = beginTime;
  }


  public Date getEndTime() {
    return endTime;
  }
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }


  public String getDurationUnit() {
    return durationUnit;
  }
  public void setDurationUnit(String durationUnit) {
    this.durationUnit = durationUnit;
  }

  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public Date getWorkDate() {
    return workDate;
  }

  public void setWorkDate(Date workDate) {
    this.workDate = workDate;
  }
}
