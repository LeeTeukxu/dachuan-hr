package com.tianye.hrsystem.modules.miniapp.entity;

import java.io.Serializable;

/**
 * 微信小程序用户绑定（系统库表 miniapp_user_binding）
 */
public class MiniAppUserBinding implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String openid;
    private String unionid;
    private String phone;
    private Long employeeId;
    private String companyId;
    private java.util.Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }
}