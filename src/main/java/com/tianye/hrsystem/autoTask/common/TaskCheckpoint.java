package com.tianye.hrsystem.autoTask.common;

import com.tianye.hrsystem.common.UpdateRecordTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 任务检查点管理器
 * 支持员工级断点续传，复用 updateRecord 表
 *
 * Key设计：
 *   mainKey: {companyId}::{TaskClassName}
 *   subKey:  {date}::{userId} 或 {date}
 *   value:   SUCCESS / FAILED
 *
 * isDone 仅当 value=SUCCESS 时返回 true（FAILED 的记录会被重新处理）
 * markDone 使用 putOrUpdate，支持 FAILED → SUCCESS 覆盖
 */
@Component
public class TaskCheckpoint {

    private static final Logger logger = LoggerFactory.getLogger(TaskCheckpoint.class);
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    // 使用ThreadLocal确保SimpleDateFormat线程安全
    private static final ThreadLocal<SimpleDateFormat> dateFormat =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    @Autowired
    private UpdateRecordTemplate recordTemplate;

    /**
     * 检查某个员工在某个Task中是否已成功处理
     * 仅 value=SUCCESS 时返回 true，FAILED 的记录返回 false（允许重试）
     */
    public boolean isDone(String companyId, Class<?> taskClass, Date date, String userId) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(date) + "::" + userId;
        return SUCCESS.equals(recordTemplate.getValue(mainKey, subKey));
    }

    /**
     * 检查某个日期段是否已处理（非员工维度）
     */
    public boolean isDone(String companyId, Class<?> taskClass, Date date) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(date);
        return SUCCESS.equals(recordTemplate.getValue(mainKey, subKey));
    }

    /**
     * 检查某个日期范围是否已处理
     */
    public boolean isDone(String companyId, Class<?> taskClass, Date begin, Date end) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(begin) + "::" + dateFormat.get().format(end);
        return SUCCESS.equals(recordTemplate.getValue(mainKey, subKey));
    }

    /**
     * 标记某个员工处理成功（支持覆盖 FAILED → SUCCESS）
     */
    public void markDone(String companyId, Class<?> taskClass, Date date, String userId) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(date) + "::" + userId;
        recordTemplate.putOrUpdate(mainKey, subKey, SUCCESS);
    }

    /**
     * 标记某个日期处理成功（非员工维度）
     */
    public void markDone(String companyId, Class<?> taskClass, Date date) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(date);
        recordTemplate.putOrUpdate(mainKey, subKey, SUCCESS);
    }

    /**
     * 标记某个日期范围处理成功
     */
    public void markDone(String companyId, Class<?> taskClass, Date begin, Date end) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(begin) + "::" + dateFormat.get().format(end);
        recordTemplate.putOrUpdate(mainKey, subKey, SUCCESS);
    }

    /**
     * 标记某个员工处理失败
     */
    public void markFailed(String companyId, Class<?> taskClass, Date date, String userId) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.get().format(date) + "::" + userId;
        recordTemplate.putOrUpdate(mainKey, subKey, FAILED);
    }
}
