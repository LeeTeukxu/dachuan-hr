package com.tianye.hrsystem.entity.vo;

public class WorkPlanCustomShiftOptionVO {

    private Long id;
    private String shiftName;
    private String customStart;
    private String customEnd;
    private String customShiftPeriod;
    private Boolean customCrossDay;
    private Boolean customContinuousShift;
    private String displayName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
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

    public Boolean getCustomCrossDay() {
        return customCrossDay;
    }

    public void setCustomCrossDay(Boolean customCrossDay) {
        this.customCrossDay = customCrossDay;
    }

    public Boolean getCustomContinuousShift() {
        return customContinuousShift;
    }

    public void setCustomContinuousShift(Boolean customContinuousShift) {
        this.customContinuousShift = customContinuousShift;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
