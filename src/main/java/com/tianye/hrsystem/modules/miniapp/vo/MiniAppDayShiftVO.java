package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;

/**
 * 某天排班的展示信息
 */
public class MiniAppDayShiftVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String workDate;
    private String shiftType;   // standard / custom / rest / empty
    private String shiftLabel;
    private String start;
    private String end;
    private boolean hasPending; // 是否有待审批申请

    public MiniAppDayShiftVO() {
    }

    public MiniAppDayShiftVO(String workDate) {
        this.workDate = workDate;
        this.shiftType = "empty";
        this.shiftLabel = "";
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public void setShiftLabel(String shiftLabel) {
        this.shiftLabel = shiftLabel;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public boolean getHasPending() {
        return hasPending;
    }

    public void setHasPending(boolean hasPending) {
        this.hasPending = hasPending;
    }
}