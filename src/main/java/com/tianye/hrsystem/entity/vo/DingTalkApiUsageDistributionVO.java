package com.tianye.hrsystem.entity.vo;

public class DingTalkApiUsageDistributionVO {

    private String featureName;
    private Long count;
    private Double percentage;

    public DingTalkApiUsageDistributionVO() {
    }

    public DingTalkApiUsageDistributionVO(String featureName, Long count, Double percentage) {
        this.featureName = featureName;
        this.count = count;
        this.percentage = percentage;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}
