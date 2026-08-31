package com.tianye.hrsystem.entity.vo;

import com.tianye.hrsystem.model.tbplanlist;

import java.util.ArrayList;
import java.util.List;

public class WorkPlanImportPreviewRowVO {
    private Integer rowIndex;
    private String workDate;
    private String employeeName;
    private String mobile;
    private String productName;
    private String positionName;
    private String workshopName;
    private String shiftType;
    private String scheduleText;
    private String customShiftPeriod;
    private String customContinuousShift;
    private String remark;
    private Boolean valid;
    private List<String> errors = new ArrayList<>();
    private tbplanlist plan;

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getScheduleText() {
        return scheduleText;
    }

    public void setScheduleText(String scheduleText) {
        this.scheduleText = scheduleText;
    }

    public String getCustomShiftPeriod() {
        return customShiftPeriod;
    }

    public void setCustomShiftPeriod(String customShiftPeriod) {
        this.customShiftPeriod = customShiftPeriod;
    }

    public String getCustomContinuousShift() {
        return customContinuousShift;
    }

    public void setCustomContinuousShift(String customContinuousShift) {
        this.customContinuousShift = customContinuousShift;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public tbplanlist getPlan() {
        return plan;
    }

    public void setPlan(tbplanlist plan) {
        this.plan = plan;
    }
}
