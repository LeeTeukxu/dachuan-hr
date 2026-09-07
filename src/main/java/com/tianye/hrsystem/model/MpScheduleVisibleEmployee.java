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
 * 排班小程序审批可见范围-自定义明细（租户库表 mp_schedule_visible_employee）。
 * visible_scope=3 时生效：permission_employee_id 可见 visible_employee_id 提交的申请
 */
@Entity
@Table(name = "mp_schedule_visible_employee")
public class MpScheduleVisibleEmployee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_employee_id", nullable = false)
    private Long permissionEmployeeId;

    @Column(name = "visible_employee_id", nullable = false)
    private Long visibleEmployeeId;

    @Column(name = "create_time")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPermissionEmployeeId() { return permissionEmployeeId; }
    public void setPermissionEmployeeId(Long permissionEmployeeId) { this.permissionEmployeeId = permissionEmployeeId; }
    public Long getVisibleEmployeeId() { return visibleEmployeeId; }
    public void setVisibleEmployeeId(Long visibleEmployeeId) { this.visibleEmployeeId = visibleEmployeeId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
