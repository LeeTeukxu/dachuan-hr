package com.tianye.hrsystem.entity.bo;

import java.util.ArrayList;
import java.util.List;

public class SaveWorkPlanEmployeeDayAssignmentsBO {

    private Long employeeId;
    private String workDate;
    private String dayStatus;
    private String restShiftType;
    private String workshopName;
    private List<SaveWorkPlanEmployeeDayAssignmentBO> assignments = new ArrayList<>();

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getDayStatus() {
        return dayStatus;
    }

    public void setDayStatus(String dayStatus) {
        this.dayStatus = dayStatus;
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

    public List<SaveWorkPlanEmployeeDayAssignmentBO> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<SaveWorkPlanEmployeeDayAssignmentBO> assignments) {
        this.assignments = assignments;
    }
}
