package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbattendancerecord")
public class tbattendancerecord implements Serializable {
    @Id
    /**
     * 接口主键
     */
    @Column(name = "id")
    private Long id;
    /**
     * 考勤类型(OnDuty：上班,OffDuty：下班)
     */
    @Column(name = "checkType")
    private String checkType;
    /**
     * 位置结果（Normal：范围内,Outside：范围外,NotSigned：未打卡）
     */
    @Column(name = "locationResult")
    private String locationResult;
    /**
     * 考勤组
     */
    @Column(name = "groupId")
    private Long groupId;
    /**
     * 打卡结果(Normal：正常,Early：早退,Late：迟到,SeriousLate：严重迟到,Absenteeism：旷工迟到,NotSigned：未打卡)
     */
    @Column(name = "timeResult")
    private String timeResult;
    /**
     * 打卡用户ID
     */
    @Column(name = "userId")
    private String userId;
    /**
     * 工作日
     */
    @Column(name = "workDate")
    private Date workDate;
    /**
     * 打卡来源(ATM：考勤机打卡（指纹/人脸打卡）,BEACON：IBeacon,DING_ATM：钉钉考勤机（考勤机蓝牙打卡）,USER：用户打卡,BOSS：老板改签,APPROVE：审批系统,SYSTEM：考勤系统,
     * AUTO_CHECK：自动打卡)
     */
    @Column(name = "sourceType")
    private String sourceType;
    /**
     * 实际打卡时间
     */
    @Column(name = "userCheckTime")
    private Date userCheckTime;


    /**
     * 计划打卡时间
     **/
    @Column(name = "planCheckTime")
    private Date planCheckTime;
    /**
     * 打卡地点
     */
    @Column(name = "userAddress")
    private String userAddress;
    /**
     * 打卡备注
     */
    @Column(name = "outsideRemark")
    private String outsideRemark;
    @Column(name = "createTime")
    private Date createTime;

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }


    public String getLocationResult() {
        return locationResult;
    }

    public void setLocationResult(String locationResult) {
        this.locationResult = locationResult;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getTimeResult() {
        return timeResult;
    }

    public void setTimeResult(String timeResult) {
        this.timeResult = timeResult;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public Date getWorkDate() {
        return workDate;
    }

    public void setWorkDate(Date workDate) {
        this.workDate = workDate;
    }


    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }


    public Date getUserCheckTime() {
        return userCheckTime;
    }

    public void setUserCheckTime(Date userCheckTime) {
        this.userCheckTime = userCheckTime;
    }


    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }


    public String getOutsideRemark() {
        return outsideRemark;
    }

    public void setOutsideRemark(String outsideRemark) {
        this.outsideRemark = outsideRemark;
    }


    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getPlanCheckTime() {
        return planCheckTime;
    }

    public void setPlanCheckTime(Date planCheckTime) {
        this.planCheckTime = planCheckTime;
    }
}
