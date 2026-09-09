package com.tianye.hrsystem.modules.miniapp.vo;

import java.util.List;

/**
 * PC 配置页批量保存排班小程序员工权限入参。
 * 三个能力开关全关时，批量删除这些员工的配置记录；可见范围仅对「可添加排班」有意义。
 */
public class MiniAppBatchPermissionSaveBO {

    private List<Long> employeeIds;
    private Boolean canSchedule;
    private Boolean canViewStatistics;
    private Boolean canLoadSchedule;
    /** 可切换公司（2026-09-10，2a）：批量场景下 true=开启、false=关闭 */
    private Boolean canSwitchCompany;
    private Integer visibleScope;

    public List<Long> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
    public Boolean getCanSchedule() { return canSchedule; }
    public void setCanSchedule(Boolean canSchedule) { this.canSchedule = canSchedule; }
    public Boolean getCanViewStatistics() { return canViewStatistics; }
    public void setCanViewStatistics(Boolean canViewStatistics) { this.canViewStatistics = canViewStatistics; }
    public Boolean getCanLoadSchedule() { return canLoadSchedule; }
    public void setCanLoadSchedule(Boolean canLoadSchedule) { this.canLoadSchedule = canLoadSchedule; }
    public Boolean getCanSwitchCompany() { return canSwitchCompany; }
    public void setCanSwitchCompany(Boolean canSwitchCompany) { this.canSwitchCompany = canSwitchCompany; }
    public Integer getVisibleScope() { return visibleScope; }
    public void setVisibleScope(Integer visibleScope) { this.visibleScope = visibleScope; }
}
