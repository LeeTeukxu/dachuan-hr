package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;

/**
 * 登录返回：token 与当前员工信息
 */
public class MiniAppLoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token;
    private Long employeeId;
    private String employeeName;
    private String companyId;
    private String companyName;
    private String depName;
    private Long depId;
    /** 首次自助绑定时为 true */
    private Boolean bindRequired;
    /** OpenID 匹配到多公司或首次绑定时返回公司候选 */
    private java.util.List<CompanyOptionVO> candidates;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }

    public Long getDepId() {
        return depId;
    }

    public void setDepId(Long depId) {
        this.depId = depId;
    }

    public Boolean getBindRequired() {
        return bindRequired;
    }

    public void setBindRequired(Boolean bindRequired) {
        this.bindRequired = bindRequired;
    }

    public java.util.List<CompanyOptionVO> getCandidates() {
        return candidates;
    }

    public void setCandidates(java.util.List<CompanyOptionVO> candidates) {
        this.candidates = candidates;
    }
}
