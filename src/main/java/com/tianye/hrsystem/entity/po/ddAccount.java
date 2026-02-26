package com.tianye.hrsystem.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @ClassName: ddAccount
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年05月29日 22:14
 **/

@TableName(value = "ddAccount")
@Entity
public class ddAccount {
    @TableField("ID")
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;
    @TableField("CompanyID")
    private String companyId;
    @TableField("AppKey")
    private String appKey;
    @TableField("AppSecret")
    private String appsecret;
    @TableField("AgentID")
    private String agentId;
    @TableField("CreateTime")
    private LocalDateTime createTime;

    @TableField("AdminID")
    private String adminId;

    @TableField("MessageToken")
    private String messageToken;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppsecret() {
        return appsecret;
    }

    public void setAppsecret(String appsecret) {
        this.appsecret = appsecret;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getMessageToken() {
        return messageToken;
    }

    public void setMessageToken(String messageToken) {
        this.messageToken = messageToken;
    }
}
