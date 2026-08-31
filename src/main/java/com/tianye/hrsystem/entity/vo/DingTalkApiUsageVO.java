package com.tianye.hrsystem.entity.vo;

import java.util.ArrayList;
import java.util.List;

public class DingTalkApiUsageVO {

    private String month;
    private Long totalCount;
    private Long monthlyLimit;
    private Double usagePercent;
    private Integer thresholdPercent;
    private Boolean overThreshold;
    private String description;
    private String limitSource;
    private List<DingTalkApiUsageDistributionVO> distributions = new ArrayList<>();

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Long monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public Double getUsagePercent() {
        return usagePercent;
    }

    public void setUsagePercent(Double usagePercent) {
        this.usagePercent = usagePercent;
    }

    public Integer getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(Integer thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public Boolean getOverThreshold() {
        return overThreshold;
    }

    public void setOverThreshold(Boolean overThreshold) {
        this.overThreshold = overThreshold;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLimitSource() {
        return limitSource;
    }

    public void setLimitSource(String limitSource) {
        this.limitSource = limitSource;
    }

    public List<DingTalkApiUsageDistributionVO> getDistributions() {
        return distributions;
    }

    public void setDistributions(List<DingTalkApiUsageDistributionVO> distributions) {
        this.distributions = distributions;
    }
}
