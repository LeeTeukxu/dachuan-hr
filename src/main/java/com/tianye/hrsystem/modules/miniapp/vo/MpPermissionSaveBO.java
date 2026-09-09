package com.tianye.hrsystem.modules.miniapp.vo;

import java.util.List;

/** PC 配置页保存排班小程序员工权限入参 */
public class MpPermissionSaveBO {

    private Long employeeId;
    private Boolean canSchedule;
    private Boolean canViewStatistics;
    private Boolean canLoadSchedule;
    /**
     * 可切换公司（2026-09-10，2a）：
     * 与 canViewStatistics 组合控制员工是否可切到非本登录公司；与可见公司清单(hrsystem.mp_employee_visible_company) 独立——
     * 清单控"可看哪些公司"，本字段控"是否允许切"。
     */
    private Boolean canSwitchCompany;
    private Integer visibleScope;
    private List<Long> visibleEmployeeIds;
    /** 公司切换可见公司 id 列表（B方案，2026-09-10，仅 saveVisibleCompanies 使用） */
    private List<String> visibleCompanyIds;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
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
    public List<Long> getVisibleEmployeeIds() { return visibleEmployeeIds; }
    public void setVisibleEmployeeIds(List<Long> visibleEmployeeIds) { this.visibleEmployeeIds = visibleEmployeeIds; }
    public List<String> getVisibleCompanyIds() { return visibleCompanyIds; }
    public void setVisibleCompanyIds(List<String> visibleCompanyIds) { this.visibleCompanyIds = visibleCompanyIds; }
}
