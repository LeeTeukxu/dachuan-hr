package com.tianye.hrsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 本地考勤判定结果（2026-09 本地考勤判定引擎，弃用钉钉推送后以 tbplanlist 为应出勤基准本地算）。
 * 每人每日一行（跨天班按班表日期记，下班卡可落次日）。
 */
@Entity
@Table(name = "hrm_attendance_judge_result")
public class HrmAttendanceJudgeResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 员工ID（hrm_employee.employeeId） */
    @Column(name = "emp_id")
    private Long empId;

    /** 班表日期（跨天班记上班日） */
    @Column(name = "work_date")
    private Date workDate;

    /** 判定采用的考勤规则ID */
    @Column(name = "rule_id")
    private Long ruleId;

    /** 是否应出勤（false=休息班） */
    @Column(name = "should_attend")
    private Boolean shouldAttend;

    /** 班次类型（standard/custom/rest） */
    @Column(name = "shift_type")
    private String shiftType;

    /** 班次上班时刻（HH:mm，多段班取首段） */
    @Column(name = "shift_start")
    private String shiftStart;

    /** 班次下班时刻（HH:mm，跨天班为次日时刻） */
    @Column(name = "shift_end")
    private String shiftEnd;

    /** 是否跨天班 */
    @Column(name = "cross_day")
    private Boolean crossDay;

    /** 首次有效上班卡时间 */
    @Column(name = "first_punch_time")
    private Date firstPunchTime;

    /** 最后有效下班卡时间 */
    @Column(name = "last_punch_time")
    private Date lastPunchTime;

    /** 迟到分钟数 */
    @Column(name = "late_minutes")
    private Integer lateMinutes;

    /** 早退分钟数 */
    @Column(name = "early_minutes")
    private Integer earlyMinutes;

    /** 缺卡次数（上班/下班/多段累计） */
    @Column(name = "miss_card_count")
    private Integer missCardCount;

    /** 旷工（应出勤日全天无有效打卡且无补卡） */
    @Column(name = "absenteeism")
    private Boolean absenteeism;

    /** 休息日出勤（不计缺勤，供加班统计复核） */
    @Column(name = "rest_day_work")
    private Boolean restDayWork;

    /** 判定时间 */
    @Column(name = "judge_time")
    private Date judgeTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }
    public Date getWorkDate() { return workDate; }
    public void setWorkDate(Date workDate) { this.workDate = workDate; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Boolean getShouldAttend() { return shouldAttend; }
    public void setShouldAttend(Boolean shouldAttend) { this.shouldAttend = shouldAttend; }
    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
    public String getShiftStart() { return shiftStart; }
    public void setShiftStart(String shiftStart) { this.shiftStart = shiftStart; }
    public String getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(String shiftEnd) { this.shiftEnd = shiftEnd; }
    public Boolean getCrossDay() { return crossDay; }
    public void setCrossDay(Boolean crossDay) { this.crossDay = crossDay; }
    public Date getFirstPunchTime() { return firstPunchTime; }
    public void setFirstPunchTime(Date firstPunchTime) { this.firstPunchTime = firstPunchTime; }
    public Date getLastPunchTime() { return lastPunchTime; }
    public void setLastPunchTime(Date lastPunchTime) { this.lastPunchTime = lastPunchTime; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public Integer getEarlyMinutes() { return earlyMinutes; }
    public void setEarlyMinutes(Integer earlyMinutes) { this.earlyMinutes = earlyMinutes; }
    public Integer getMissCardCount() { return missCardCount; }
    public void setMissCardCount(Integer missCardCount) { this.missCardCount = missCardCount; }
    public Boolean getAbsenteeism() { return absenteeism; }
    public void setAbsenteeism(Boolean absenteeism) { this.absenteeism = absenteeism; }
    public Boolean getRestDayWork() { return restDayWork; }
    public void setRestDayWork(Boolean restDayWork) { this.restDayWork = restDayWork; }
    public Date getJudgeTime() { return judgeTime; }
    public void setJudgeTime(Date judgeTime) { this.judgeTime = judgeTime; }
}
