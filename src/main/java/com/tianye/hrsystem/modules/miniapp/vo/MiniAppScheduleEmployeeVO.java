package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;
import java.util.List;

/** 可排班员工（GET /mp/schedule/employees），字段与前端 addschedule.vue 的搜索/展示一致 */
public class MiniAppScheduleEmployeeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long employeeId;

    private String name;

    private String phone;

    private String depName;

    public MiniAppScheduleEmployeeVO() {
    }

    public MiniAppScheduleEmployeeVO(Long employeeId, String name, String phone, String depName) {
        this.employeeId = employeeId;
        this.name = name;
        this.phone = phone;
        this.depName = depName;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }
}
