package com.tianye.hrsystem.model.ddTalk;

import java.util.List;

/**
 * @ClassName: RptAttendanceDetail
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月19日 21:15
 **/
public class RptAttendanceDetail {
    String deptName;
    String employeeId;
    String employeeName;
    String jobNumber;
    String post;
    List<singleDate> dateList;
    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public List<singleDate> getDateList() {
        return dateList;
    }

    public void setDateList(List<singleDate> dateList) {
        this.dateList = dateList;
    }
}
