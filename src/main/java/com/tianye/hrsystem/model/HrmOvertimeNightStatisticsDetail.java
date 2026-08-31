package com.tianye.hrsystem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "hrm_overtime_night_statistics_detail")
public class HrmOvertimeNightStatisticsDetail implements Serializable {

    @Id
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "stat_year")
    private Integer statYear;

    @Column(name = "stat_month")
    private Integer statMonth;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "job_number")
    private String jobNumber;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "dept_name")
    private String deptName;

    @Column(name = "work_date")
    private Date workDate;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "scheduled_off_time")
    private Date scheduledOffTime;

    @Column(name = "actual_off_time")
    private Date actualOffTime;

    @Column(name = "overtime_hours")
    private BigDecimal overtimeHours;

    @Column(name = "night_shift_count")
    private Integer nightShiftCount;

    @Column(name = "expected_attendance_days")
    private Integer expectedAttendanceDays;

    @Column(name = "expected_attendance_hours")
    private BigDecimal expectedAttendanceHours;

    @Column(name = "actual_attendance_days")
    private Integer actualAttendanceDays;

    @Column(name = "actual_attendance_hours")
    private BigDecimal actualAttendanceHours;

    @Column(name = "accrued_attendance_hours")
    private BigDecimal accruedAttendanceHours;

    @Column(name = "attendance_manual_adjusted")
    private Integer attendanceManualAdjusted;

    @Column(name = "create_user_id")
    private Long createUserId;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_user_id")
    private Long updateUserId;

    @Column(name = "update_time")
    private Date updateTime;

    public Long getDetailId() {
        return detailId;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    public Integer getStatYear() {
        return statYear;
    }

    public void setStatYear(Integer statYear) {
        this.statYear = statYear;
    }

    public Integer getStatMonth() {
        return statMonth;
    }

    public void setStatMonth(Integer statMonth) {
        this.statMonth = statMonth;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Date getWorkDate() {
        return workDate;
    }

    public void setWorkDate(Date workDate) {
        this.workDate = workDate;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Date getScheduledOffTime() {
        return scheduledOffTime;
    }

    public void setScheduledOffTime(Date scheduledOffTime) {
        this.scheduledOffTime = scheduledOffTime;
    }

    public Date getActualOffTime() {
        return actualOffTime;
    }

    public void setActualOffTime(Date actualOffTime) {
        this.actualOffTime = actualOffTime;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Integer getNightShiftCount() {
        return nightShiftCount;
    }

    public void setNightShiftCount(Integer nightShiftCount) {
        this.nightShiftCount = nightShiftCount;
    }

    public Integer getExpectedAttendanceDays() {
        return expectedAttendanceDays;
    }

    public void setExpectedAttendanceDays(Integer expectedAttendanceDays) {
        this.expectedAttendanceDays = expectedAttendanceDays;
    }

    public BigDecimal getExpectedAttendanceHours() {
        return expectedAttendanceHours;
    }

    public void setExpectedAttendanceHours(BigDecimal expectedAttendanceHours) {
        this.expectedAttendanceHours = expectedAttendanceHours;
    }

    public Integer getActualAttendanceDays() {
        return actualAttendanceDays;
    }

    public void setActualAttendanceDays(Integer actualAttendanceDays) {
        this.actualAttendanceDays = actualAttendanceDays;
    }

    public BigDecimal getActualAttendanceHours() {
        return actualAttendanceHours;
    }

    public void setActualAttendanceHours(BigDecimal actualAttendanceHours) {
        this.actualAttendanceHours = actualAttendanceHours;
    }

    public BigDecimal getAccruedAttendanceHours() {
        return accruedAttendanceHours;
    }

    public void setAccruedAttendanceHours(BigDecimal accruedAttendanceHours) {
        this.accruedAttendanceHours = accruedAttendanceHours;
    }

    public Integer getAttendanceManualAdjusted() {
        return attendanceManualAdjusted;
    }

    public void setAttendanceManualAdjusted(Integer attendanceManualAdjusted) {
        this.attendanceManualAdjusted = attendanceManualAdjusted;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Long updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
