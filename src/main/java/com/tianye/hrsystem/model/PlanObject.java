package com.tianye.hrsystem.model;

import java.io.Serializable;

/**
 * @ClassName: PlanObject
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年07月18日 21:54
 **/
public class PlanObject implements Serializable {
    private Long planId;
    private Long shiftId;
    private Long groupId;
    private Long userId;
    private String userName;
    private String workDate;
    private String rest;
    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getRest() {
        return rest;
    }

    public void setRest(String rest) {
        this.rest = rest;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
