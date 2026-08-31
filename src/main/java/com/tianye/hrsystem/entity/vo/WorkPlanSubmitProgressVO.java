package com.tianye.hrsystem.entity.vo;

import java.util.ArrayList;
import java.util.List;

public class WorkPlanSubmitProgressVO {

    private String taskId;
    private String status;
    private String stage;
    private String message;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Boolean done;
    private Boolean success;
    private List<WorkPlanSubmitRowErrorVO> errors = new ArrayList<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public List<WorkPlanSubmitRowErrorVO> getErrors() {
        return errors;
    }

    public void setErrors(List<WorkPlanSubmitRowErrorVO> errors) {
        this.errors = errors;
    }
}
