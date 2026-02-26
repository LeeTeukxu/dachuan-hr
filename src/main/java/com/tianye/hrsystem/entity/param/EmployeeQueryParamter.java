package com.tianye.hrsystem.entity.param;

/**
 * @ClassName: EmployeeQueryParamter
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月20日 10:04
 **/
public class EmployeeQueryParamter extends   RequestParameterBase {
    String employeeName;
    String status;

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
