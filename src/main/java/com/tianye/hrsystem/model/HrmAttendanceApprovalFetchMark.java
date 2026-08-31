package com.tianye.hrsystem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "hrm_attendance_approval_fetch_mark")
public class HrmAttendanceApprovalFetchMark implements Serializable {

    @Id
    @Column(name = "fetch_mark_id")
    private Long fetchMarkId;

    @Column(name = "month_key")
    private String monthKey;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "approval_type")
    private String approvalType;

    @Column(name = "fetch_version")
    private Integer fetchVersion;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    public Long getFetchMarkId() {
        return fetchMarkId;
    }

    public void setFetchMarkId(Long fetchMarkId) {
        this.fetchMarkId = fetchMarkId;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public Integer getFetchVersion() {
        return fetchVersion;
    }

    public void setFetchVersion(Integer fetchVersion) {
        this.fetchVersion = fetchVersion;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
