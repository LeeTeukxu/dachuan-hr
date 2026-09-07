package com.tianye.hrsystem.task;

import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.config.CompanyDataSourceProvider;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    /** 当前单机同步服务的启动时间；早于它的锁属于重启前已中断的任务。 */
    private final long processStartedAt = System.currentTimeMillis();

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
        return tryBegin(companyId, null);
    }

    /**
     * 尝试占用指定公司的同步运行锁，并记录发起账号，便于重复提交时定位实际操作者。
     * 旧版本锁值为 "1"，读取端仍兼容该值。
     */
    public boolean tryBegin(String companyId, LoginUserInfo owner) {
        String key = RUNNING_LOCK_PREFIX + (companyId == null ? "unknown" : companyId);
        Map<String, Object> lockInfo = new HashMap<>();
        lockInfo.put("account", owner == null ? null : owner.getAccount());
        lockInfo.put("userName", owner == null ? null : owner.getUserName());
        lockInfo.put("companyName", owner == null ? null : owner.getCompanyName());
        lockInfo.put("acquiredAt", System.currentTimeMillis());
        lockInfo.put("token", UUID.randomUUID().toString());
        String payload = JSON.toJSONString(lockInfo);
        
        // 检查锁是否存在
        Object existingLock = redis.get(key);
        if (existingLock != null) {
            logger.info("尝试获取锁: 锁已存在, companyId={}, existingLock={}", companyId, existingLock);
        }
        
        boolean acquired = redis.setNx(key, (long) RUNNING_LOCK_TTL_SECONDS, payload);
        if (!acquired && isRecoverableStaleLock(companyId)) {
            // 已终态或由上一个进程遗留的锁可自愈；当前进程建立的 RUNNING 锁仍保持互斥。
            logger.info("尝试获取锁: 检测到可恢复的残留锁, 正在回收, companyId={}", companyId);
            finish(companyId);
            acquired = redis.setNx(key, (long) RUNNING_LOCK_TTL_SECONDS, payload);
            if (acquired) {
                logger.info("尝试获取锁: 成功回收残留锁并重新获取, companyId={}", companyId);
            }
        }
        if (!acquired) {
            logger.warn("拒绝重复发起同步：该公司已有同步任务在运行, companyId={}, owner={}", companyId, owner);
        } else {
            logger.info("成功获取同步锁: companyId={}, owner={}", companyId, owner);
        }
        return acquired;
    }

    /** 返回当前锁记录的账号/公司信息；旧版锁或异常内容返回空 map。 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRunningOwner(String companyId) {
        String key = RUNNING_LOCK_PREFIX + (companyId == null ? "unknown" : companyId);
        Object raw = redis.get(key);
        if (raw == null) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> parsed = JSON.parseObject(String.valueOf(raw), Map.class);
            return parsed == null ? new HashMap<>() : parsed;
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private boolean isRecoverableStaleLock(String companyId) {
        Map<String, Object> owner = getRunningOwner(companyId);
        long acquiredAt = parseLong(owner.get("acquiredAt"));
        if (acquiredAt <= 0) {
            logger.warn("残留锁检查: 无法获取锁创建时间, companyId={}, owner={}", companyId, owner);
            return false;
        }
        String normalizedId = normalizeCompanyId(companyId);
        // 兼容新旧两种 key 格式：旧版 status:{companyId}，新版 attendance:sync:{companyId}:status
        String statusNew = String.valueOf((Object) redis.get("attendance:sync:" + normalizedId + ":status"));
        String statusOld = String.valueOf((Object) redis.get("attendance:sync:status:" + normalizedId));
        String status = (statusNew != null && !statusNew.isEmpty() && !"null".equals(statusNew)) ? statusNew : statusOld;
        
        // 兼容新旧两种 key 格式
        String updateTimeNew = String.valueOf((Object) redis.get("attendance:sync:" + normalizedId + ":update_time"));
        String updateTimeOld = String.valueOf((Object) redis.get("attendance:sync:update_time:" + normalizedId));
        String updateTimeStr = (updateTimeNew != null && !updateTimeNew.isEmpty() && !"null".equals(updateTimeNew)) ? updateTimeNew : updateTimeOld;
        long updateTime = parseLong(updateTimeStr);
        
        // 详细日志：记录锁检查的完整信息
        long timeDiff = updateTime - acquiredAt;
        long lockAge = System.currentTimeMillis() - acquiredAt;
        logger.info("残留锁检查: companyId={}, status={}, acquiredAt={}, updateTime={}, processStartedAt={}, timeDiff={}ms, lockAge={}ms", 
                    companyId, status, acquiredAt, updateTime, processStartedAt, timeDiff, lockAge);
        
        if (!("SUCCESS".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status))) {
            // 单机 systemd 服务重启会中断后台线程；重启前的 RUNNING 锁绝不能继续阻塞 2 小时。
            boolean recoverable = acquiredAt < processStartedAt;
            logger.info("非终态锁检查: acquiredAt < processStartedAt = {} (diff={}ms)", 
                        recoverable, processStartedAt - acquiredAt);
            return recoverable;
        }
        
        // 终态写入必须晚于本锁建立，才能确认不是上一轮任务遗留的进度。
        // 使用 >= 而不是 >，避免同一毫秒内完成的任务无法自愈
        boolean recoverable = updateTime >= acquiredAt;
        
        // 兜底条件：如果进度状态是SUCCESS/FAILED，且锁存在时间超过5分钟，允许回收
        if (!recoverable && lockAge > 5 * 60 * 1000) {
            logger.info("兜底自愈: 锁存在超过5分钟且进度已终态, companyId={}, lockAge={}ms", companyId, lockAge);
            recoverable = true;
        }
        
        logger.info("终态锁检查: updateTime >= acquiredAt = {} (diff={}ms), recoverable={}", 
                    recoverable, timeDiff, recoverable);
        return recoverable;
    }

    private long parseLong(Object value) {
        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String normalizeCompanyId(String companyId) {
        return companyId == null ? "unknown" : companyId;
    }

    /** 归还指定公司的同步运行锁（带重试机制） */
    public void finish(String companyId) {
        String key = RUNNING_LOCK_PREFIX + (companyId == null ? "unknown" : companyId);
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                redis.del(key);
                logger.info("成功释放同步运行锁: companyId={}, key={}, attempt={}", companyId, key, i + 1);
                return;
            } catch (Exception e) {
                logger.warn("释放同步运行锁失败(第{}次): companyId={}, key={}", i + 1, companyId, key, e);
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(100 * (i + 1)); // 递增延迟：100ms, 200ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        logger.error("释放同步运行锁最终失败（已重试{}次），锁将等待TTL过期: companyId={}, key={}", maxRetries, companyId, key);
    }

    /**
     * 提交后台同步任务：恢复租户上下文、执行任务、最终归还公司锁。
     *
     * @return false = 线程池与队列已满，调用方必须 finish(companyId) 并返回"系统繁忙"
     */
    public boolean submit(String companyId, LoginUserInfo requestContext, Runnable task) {
        logger.info("提交后台同步任务: companyId={}, queueSize={}, activeCount={}", 
                    companyId, executor.getQueue().size(), executor.getActiveCount());
        try {
            executor.submit(() -> {
                logger.info("后台同步任务开始执行: companyId={}", companyId);
                CompanyDataSourceProvider.markActive(companyId);
                CompanyContext.set(requestContext);
                try {
                    task.run();
                    logger.info("后台同步任务执行完成: companyId={}", companyId);
                } catch (Exception e) {
                    logger.error("公司{}后台同步任务执行异常", companyId, e);
                } finally {
                    CompanyContext.clear();
                    CompanyDataSourceProvider.markInactive(companyId);
                    logger.info("后台同步任务释放锁: companyId={}", companyId);
                    finish(companyId);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            logger.warn("同步任务队列已满，公司{}提交被拒绝, queueSize={}, activeCount={}", 
                        companyId, executor.getQueue().size(), executor.getActiveCount());
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }
}
