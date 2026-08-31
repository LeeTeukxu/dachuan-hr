package com.tianye.hrsystem.task;

import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉同步任务托管启动器（考勤同步 + 审批获取共用）。
 *
 * 职责：
 * 1. 有界线程池：核心=上限=8（≈分公司数上限，保证 8 家同时提交全部立即并行执行，
 *    不因超出核心数而排队），队列 8；队列满时提交失败，由调用方返回"系统繁忙"，
 *    绝不在 HTTP 请求线程上回退执行（避免异步化失效）。
 * 2. 按公司互斥锁（Redis setNx，TTL 2 小时，后台任务 finally 释放；进程崩溃最多 2 小时自动放行）：
 *    同一家公司同时只允许一个同步任务，跨公司互不影响——各公司打各自钉钉应用、写各自租户库。
 *
 * 提交协议（调用方必须遵守）：
 *   tryBegin(companyId) 成功 → markQueued(companyId) → submit(...)；
 *   submit 返回 false 或期间抛异常 → 调用方必须 finish(companyId) 归还锁。
 */
@Component
public class AttendanceSyncTaskLauncher {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceSyncTaskLauncher.class);

    /** 按公司运行锁键前缀（键 = 前缀 + companyId） */
    public static final String RUNNING_LOCK_PREFIX = "attendance:sync:running:";
    /** 锁 TTL：正常结束/失败在后台任务 finally 主动释放；进程崩溃时最多 2 小时自动放行 */
    public static final int RUNNING_LOCK_TTL_SECONDS = 7200;

    @Autowired
    private Redis redis;

    private final ThreadPoolExecutor executor;

    public AttendanceSyncTaskLauncher() {
        this(8, 8, 8);
    }

    AttendanceSyncTaskLauncher(int corePoolSize, int maxPoolSize, int queueCapacity) {
        executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r, "dingtalk-sync-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** 尝试占用指定公司的同步运行锁；false = 该公司已有同步在运行 */
    public boolean tryBegin(String companyId) {
        String key = RUNNING_LOCK_PREFIX + (companyId == null ? "unknown" : companyId);
        boolean acquired = redis.setNx(key, (long) RUNNING_LOCK_TTL_SECONDS, "1");
        if (!acquired) {
            logger.warn("拒绝重复发起同步：该公司已有同步任务在运行");
        }
        return acquired;
    }

    /** 归还指定公司的同步运行锁 */
    public void finish(String companyId) {
        try {
            redis.del(RUNNING_LOCK_PREFIX + (companyId == null ? "unknown" : companyId));
        } catch (Exception e) {
            logger.warn("释放同步运行锁失败（等待 TTL 自动过期）: {}", companyId, e);
        }
    }

    /**
     * 提交后台同步任务：恢复租户上下文、执行任务、最终归还公司锁。
     *
     * @return false = 线程池与队列已满，调用方必须 finish(companyId) 并返回"系统繁忙"
     */
    public boolean submit(String companyId, LoginUserInfo requestContext, Runnable task) {
        try {
            executor.submit(() -> {
                CompanyContext.set(requestContext);
                try {
                    task.run();
                } catch (Exception e) {
                    logger.error("公司{}后台同步任务执行异常", companyId, e);
                } finally {
                    CompanyContext.clear();
                    finish(companyId);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            logger.warn("同步任务队列已满，公司{}提交被拒绝", companyId);
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }
}
