package com.tianye.hrsystem.modules.miniapp.vo;

import java.util.List;

/** PC 配置页保存排班小程序员工权限入参 */
public class MpPermissionSaveBO {

    private Long employeeId;
    private Boolean canSchedule;
    private Integer visibleScope;
    private List<Long> visibleEmployeeIds;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Boolean getCanSchedule() { return canSchedule; }
    public void setCanSchedule(Boolean canSchedule) { this.canSchedule = canSchedule; }
    public Integer getVisibleScope() { return visibleScope; }
    public void setVisibleScope(Integer visibleScope) { this.visibleScope = visibleScope; }
    public List<Long> getVisibleEmployeeIds() { return visibleEmployeeIds; }
    public void setVisibleEmployeeIds(List<Long> visibleEmployeeIds) { this.visibleEmployeeIds = visibleEmployeeIds; }
}
