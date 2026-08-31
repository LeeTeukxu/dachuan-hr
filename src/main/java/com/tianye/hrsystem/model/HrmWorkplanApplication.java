package com.tianye.hrsystem.model;

import javax.persistence.*;
import java.util.Date;

/**
 * 排班修改申请单（每个租户库 hrm_workplan_application）
 */
@Entity
@Table(name = "hrm_workplan_application")
public class HrmWorkplanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "work_date")
    @Temporal(TemporalType.DATE)
    private Date workDate;

    @Column(name = "shift_type")
    private String shiftType;

    @Column(name = "class_id")
    private String classId;

    @Column(name = "custom_start")
    private String customStart;

    @Column(name = "custom_end")
    private String customEnd;

    @Column(name = "custom_shift_period")
    private String customShiftPeriod;

    @Column(name = "custom_continuous_shift")
    private Boolean customContinuousShift;

    @Column(name = "rest_shift_type")
    private String restShiftType;

    @Column(name = "status")
    private String status;

    @Column(name = "is_current_effective")
    private Boolean isCurrentEffective;

    @Column(name = "approver_employee_id")
    private Long approverEmployeeId;

    @Column(name = "approve_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date approveTime;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Date getWorkDate() {
        return workDate;
    }

    public void setWorkDate(Date workDate) {
        this.workDate = workDate;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsCurrentEffective() {
        return isCurrentEffective;
    }

    public void setIsCurrentEffective(Boolean isCurrentEffective) {
        this.isCurrentEffective = isCurrentEffective;
    }

    public Long getApproverEmployeeId() {
        return approverEmployeeId;
    }

    public void setApproverEmployeeId(Long approverEmployeeId) {
        this.approverEmployeeId = approverEmployeeId;
    }

    public Date getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(Date approveTime) {
        this.approveTime = approveTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}