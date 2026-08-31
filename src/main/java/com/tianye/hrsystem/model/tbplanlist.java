package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbplanlist")
public class tbplanlist implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "ID")
  private Integer id;
  @Column(name = "ProductName")
  private String productName;
  @Column(name = "LinkName")
  private String linkName;
  @Column(name = "workshop_name")
  private String workshopName;
  @Column(name = "WorkDate")
  private Date workDate;
  @Column(name = "GroupID")
  private String groupId;
  @Column(name = "ClassID")
  private String classId;
  @Column(name = "custom_shift_id")
  private Long customShiftId;
  @Column(name = "custom_shift_period")
  private String customShiftPeriod;
  @Column(name = "custom_continuous_shift")
  private Boolean customContinuousShift;
  @Column(name = "rest_shift_type")
  private String restShiftType;
  @Column(name = "UserID")
  private String userId;
  @Column(name = "CreateTime")
  private Date createTime;
  @Column(name = "shift_source")
  private String shiftType;
  @Transient
  private String customStart;
  @Transient
  private String customEnd;
  @Transient
  private Boolean customCrossDay;
  @Transient
  private Boolean customContinuousShiftExplicit;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public String getProductName() {
    return productName;
  }
  public void setProductName(String productName) {
    this.productName = productName;
  }


  public String getLinkName() {
    return linkName;
  }
  public void setLinkName(String linkName) {
    this.linkName = linkName;
  }


  public String getWorkshopName() {
    return workshopName;
  }
  public void setWorkshopName(String workshopName) {
    this.workshopName = workshopName;
  }


  public Date getWorkDate() {
    return workDate;
  }
  public void setWorkDate(Date workDate) {
    this.workDate = workDate;
  }


  public String getGroupId() {
    return groupId;
  }
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }


  public String getClassId() {
    return classId;
  }
  public void setClassId(String classId) {
    this.classId = classId;
  }


  public String getUserId() {
    return userId;
  }
  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Long getCustomShiftId() {
    return customShiftId;
  }

  public void setCustomShiftId(Long customShiftId) {
    this.customShiftId = customShiftId;
  }

  public String getCustomShiftPeriod() {
    return customShiftPeriod;
  }

  public void setCustomShiftPeriod(String customShiftPeriod) {
    this.customShiftPeriod = customShiftPeriod;
  }

  public Boolean getCustomContinuousShift() {
    return customContinuousShift;
  }

  public void setCustomContinuousShift(Boolean customContinuousShift) {
    this.customContinuousShift = customContinuousShift;
  }

  public String getRestShiftType() {
    return restShiftType;
  }

  public void setRestShiftType(String restShiftType) {
    this.restShiftType = restShiftType;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public String getShiftType() {
    return shiftType;
  }

  public void setShiftType(String shiftType) {
    this.shiftType = shiftType;
  }

  public String getCustomStart() {
    return customStart;
  }

  public void setCustomStart(String customStart) {
    this.customStart = customStart;
  }

  public String getCustomEnd() {
    return customEnd;
  }

  public void setCustomEnd(String customEnd) {
    this.customEnd = customEnd;
  }

  public Boolean getCustomCrossDay() {
    return customCrossDay;
  }

  public void setCustomCrossDay(Boolean customCrossDay) {
    this.customCrossDay = customCrossDay;
  }

  public Boolean getCustomContinuousShiftExplicit() {
    return customContinuousShiftExplicit;
  }

  public void setCustomContinuousShiftExplicit(Boolean customContinuousShiftExplicit) {
    this.customContinuousShiftExplicit = customContinuousShiftExplicit;
  }

}
