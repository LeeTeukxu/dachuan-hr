package com.tianye.hrsystem.model;

import java.io.Serializable;

/**
 * @ClassName: tbPlanListVo
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月09日 9:37
 **/
public class tbPlanListVo implements Serializable {
    String ProductName;
    String LinkName;
    String WorkDate;
    String ClassName;
    String UserName;

    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String productName) {
        ProductName = productName;
    }

    public String getLinkName() {
        return LinkName;
    }

    public void setLinkName(String linkName) {
        LinkName = linkName;
    }

    public String getWorkDate() {
        return WorkDate;
    }

    public void setWorkDate(String workDate) {
        WorkDate = workDate;
    }

    public String getClassName() {
        return ClassName;
    }

    public void setClassName(String className) {
        ClassName = className;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }
}
