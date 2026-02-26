package com.tianye.hrsystem.model;

import com.tianye.hrsystem.modules.menu.entity.TbMenu;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: LoginUserInfo
 * @Author: 肖新民
 * @*TODO:登录用户信息
 * @CreateTime: 2024年03月05日 22:40
 **/
public class LoginUserInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String companyId;
    private String depId;
    private String userId;
    private String userName;
    private String companyName;
    private String account;
    private String depName;
    private String password;
    private String roleName;
    private String token;
    private String suffix;
    private List<String> rolemenu;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    private String roleId;
    private boolean canLogin;

    public List<Integer> getMyManager() {
        return myManager;
    }

    public Long getUserIdValueL() {
        return Long.parseLong(userId);
    }
    public Integer getUserIdValue(){
        return Integer.parseInt(userId);
    }

    public Long getDepIdValue() {
        return Long.parseLong(depId);
    }

    public void setMyManager(List<Integer> myManager) {
        this.myManager = myManager;
    }

    private List<Integer> myManager;

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getDepId() {
        return depId;
    }

    public void setDepId(String depId) {
        this.depId = depId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean getCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public List<String> getRolemenu() {
        return rolemenu;
    }

    public void setRolemenu(List<String> rolemenu) {
        this.rolemenu = rolemenu;
    }
}
