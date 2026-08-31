package com.tianye.hrsystem.imple.workplan;

import com.tianye.hrsystem.model.tbplanlist;

public class ResolvedWorkPlanAssignment {

    private Integer sourceIndex;
    private tbplanlist sourcePlan;
    private String userId;
    private String resolvedGroupId;
    private String resolvedShiftId;

    public Integer getSourceIndex() {
        return sourceIndex;
    }

    public void setSourceIndex(Integer sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public tbplanlist getSourcePlan() {
        return sourcePlan;
    }

    public void setSourcePlan(tbplanlist sourcePlan) {
        this.sourcePlan = sourcePlan;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResolvedGroupId() {
        return resolvedGroupId;
    }

    public void setResolvedGroupId(String resolvedGroupId) {
        this.resolvedGroupId = resolvedGroupId;
    }

    public String getResolvedShiftId() {
        return resolvedShiftId;
    }

    public void setResolvedShiftId(String resolvedShiftId) {
        this.resolvedShiftId = resolvedShiftId;
    }
}
