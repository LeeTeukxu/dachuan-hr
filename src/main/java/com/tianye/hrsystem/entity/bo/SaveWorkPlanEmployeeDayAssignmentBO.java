package com.tianye.hrsystem.entity.bo;

public class SaveWorkPlanEmployeeDayAssignmentBO {

    private Integer planId;
    private String productMode;
    private Long productId;
    private String productName;
    private Long positionId;
    private String positionName;
    private String shiftType;
    private String classId;
    private Long customShiftId;
    private String customStart;
    private String customEnd;
    private String customShiftPeriod;
    private Boolean customContinuousShift;
    private String restShiftType;
    private String workshopName;

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public String getProductMode() {
        return productMode;
    }

    public void setProductMode(String productMode) {
        this.productMode = productMode;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public Long getCustomShiftId() {
        return customShiftId;
    }

    public void setCustomShiftId(Long customShiftId) {
        this.customShiftId = customShiftId;
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

    public Boolean getCustomContinuousShift() {
        return customContinuousShift;
    }

    public void setCustomContinuousShift(Boolean customContinuousShift) {
        this.customContinuousShift = customContinuousShift;
    }

    public String getRestShiftType() {
        return restShiftType;
    }

    public void setRestShiftType(String restShiftType) {
        this.restShiftType = restShiftType;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }
}
