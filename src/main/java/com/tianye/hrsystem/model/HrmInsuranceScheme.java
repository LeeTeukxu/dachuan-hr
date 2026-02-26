package com.tianye.hrsystem.model;

import java.util.Date;
import java.io.Serializable;

/**
 * 社保方案表(HrmInsuranceScheme)实体类
 *
 * @author makejava
 * @since 2024-03-22 16:27:58
 */
public class HrmInsuranceScheme implements Serializable {
    private static final long serialVersionUID = -90209638420543551L;
    /**
     * 社保方案ID
     */
    private Long schemeId;
    /**
     * 方案名称
     */
    private String schemeName;
    /**
     * 参保城市ID
     */
    private Long city;
    /**
     * 创建人ID
     */
    private Long createUserId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新人ID
     */
    private Long updateUserId;
    /**
     * 更新时间
     */
    private Date updateTime;


    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public Long getCity() {
        return city;
    }

    public void setCity(Long city) {
        this.city = city;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Long updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}

