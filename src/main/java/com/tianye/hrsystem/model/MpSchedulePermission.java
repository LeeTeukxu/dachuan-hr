package com.tianye.hrsystem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * 排班小程序员工级权限（租户库表 mp_schedule_permission）。
 * 有记录即授予"添加排班"能力；visible_scope 控制审批信息可见范围：
 * 1=直属下属(默认) 2=全部员工 3=自定义（明细见 mp_schedule_visible_employee）
 */
@Entity
@Table(name = "mp_schedule_permission")
public class MpSchedulePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SCOPE_SUBORDINATES = 1;
    public static final int SCOPE_ALL = 2;
    public static final int SCOPE_CUSTOM = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;

    @Column(name = "visible_scope", nullable = false)
    private Integer visibleScope = SCOPE_SUBORDINATES;

    /** 可添加排班（/mp/schedule/save 等）。2026-09-09 起由显式字段控制，不再用“有记录”表达 */
    @Column(name = "can_schedule", nullable = false)
    private Integer canSchedule = 0;

    /** 小程序数据统计（/mp/dashboard/*） */
    @Column(name = "can_view_statistics", nullable = false)
    private Integer canViewStatistics = 0;

    /** 小程序排班数据加载（/mp/mySchedule、/mp/mySchedule/day、/mp/schedule/query） */
    @Column(name = "can_load_schedule", nullable = false)
    private Integer canLoadSchedule = 0;

    /**
     * 可切换公司（2026-09-10，2a 闭环）：
     * 与 can_view_statistics 组合控制"切到非本登录公司"的细粒度权限——
     * can_view_statistics=1 且 can_switch_company=1 才允许带非本司 companyId 访问 dashboard 端点；
     * 仅 can_view_statistics=1 时只能看本公司（默认安全）。
     */
    @Column(name = "can_switch_company", nullable = false)
    private Integer canSwitchCompany = 0;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getVisibleScope() { return visibleScope; }
    public void setVisibleScope(Integer visibleScope) { this.visibleScope = visibleScope; }
    public Integer getCanSchedule() { return canSchedule; }
    public void setCanSchedule(Integer canSchedule) { this.canSchedule = canSchedule; }
    public Integer getCanViewStatistics() { return canViewStatistics; }
    public void setCanViewStatistics(Integer canViewStatistics) { this.canViewStatistics = canViewStatistics; }
    public Integer getCanLoadSchedule() { return canLoadSchedule; }
    public void setCanLoadSchedule(Integer canLoadSchedule) { this.canLoadSchedule = canLoadSchedule; }
    public Integer getCanSwitchCompany() { return canSwitchCompany; }
    public void setCanSwitchCompany(Integer canSwitchCompany) { this.canSwitchCompany = canSwitchCompany; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
