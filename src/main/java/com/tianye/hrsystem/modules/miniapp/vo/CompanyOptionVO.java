package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;

/**
 * 公司下拉/候选选项
 */
public class CompanyOptionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String companyId;
    private String companyName;

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
}