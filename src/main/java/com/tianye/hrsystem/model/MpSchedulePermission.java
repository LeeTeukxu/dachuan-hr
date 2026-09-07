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
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
