package com.tianye.hrsystem.autoTask.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉API统一限流器
 * 基于令牌桶思想，每个公司独立限流
 * 钉钉官方限制约20次/秒，默认设置15次/秒留余量
 */
@Component
public class DingTalkRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkRateLimiter.class);
    private static final long MAX_BACKOFF_MS = 10000;
    private static final long CLEANUP_INTERVAL_MS = 60 * 60 * 1000; // 1小时
    private static final long INACTIVE_THRESHOLD_MS = 60 * 60 * 1000; // 1小时未使用

    @Value("${dingtalk.rate-limit.permits-per-second:15}")
    private double permitsPerSecond;

    /** 每个公司的上次请求时间戳 */
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    /** 每个公司的独立锁对象 */
    private final Map<String, Object> companyLocks = new ConcurrentHashMap<>();

    /** 定时清理调度器 */
    private ScheduledExecutorService cleanupScheduler;

    @PostConstruct
    public void init() {
        // 校验配置项
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("dingtalk.rate-limit.permits-per-second must be positive, got: " + permitsPerSecond);
        }
        logger.info("DingTalkRateLimiter initialized with permitsPerSecond={}", permitsPerSecond);

        // 启动定时清理任务
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DingTalkRateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupInactiveCompanies,
            CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        logger.info("DingTalkRateLimiter cleanup scheduler started");
    }

    @PreDestroy
    public void destroy() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
                logger.info("DingTalkRateLimiter cleanup scheduler stopped");
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 清理不活跃的公司数据，防止Map无限增长
     */
    private void cleanupInactiveCompanies() {
        try {
            long now = System.currentTimeMillis();
            int removedCount = 0;
            for (Map.Entry<String, Long> entry : lastRequestTime.entrySet()) {
                if (now - entry.getValue() > INACTIVE_THRESHOLD_MS) {
                    String companyId = entry.getKey();
                    lastRequestTime.remove(companyId);
                    companyLocks.remove(companyId);
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                logger.info("Cleaned up {} inactive companies from rate limiter", removedCount);
            }
        } catch (Exception e) {
            logger.error("Error during cleanup of inactive companies", e);
        }
    }

    /** 最小请求间隔（毫秒），根据 permitsPerSecond 计算 */
    private long getMinIntervalMs() {
        return (long) (1000.0 / permitsPerSecond);
    }

    /**
     * 获取公司的独立锁对象
     */
    private Object getLockForCompany(String companyId) {
        return companyLocks.computeIfAbsent(companyId, k -> new Object());
    }

    /**
     * 获取API调用许可（阻塞等待直到满足限流条件）
     * @param companyId 公司ID
     */
    public void acquire(String companyId) {
        long minInterval = getMinIntervalMs();
        Object lock = getLockForCompany(companyId);
        synchronized (lock) {
            Long lastTime = lastRequestTime.get(companyId);
            if (lastTime != null) {
                long elapsed = System.currentTimeMillis() - lastTime;
                if (elapsed < minInterval) {
                    try {
                        long waitTime = minInterval - elapsed;
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Rate limiter interrupted while waiting", e);
                    }
                }
            }
            lastRequestTime.put(companyId, System.currentTimeMillis());
        }
    }

    /**
     * 带重试的API调用包装（指数退避）
     * @param apiCall API调用逻辑
     * @param maxRetries 最大重试次数
     * @param apiName API名称（用于日志）
     * @return API调用结果
     */
    public <T> T executeWithRetry(ApiCallable<T> apiCall, int maxRetries, String apiName) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return apiCall.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries && isRetryable(e)) {
                    long backoff = Math.min((long) (500 * Math.pow(2, attempt)), MAX_BACKOFF_MS);
                    logger.warn("[{}] 第{}次重试，等待{}ms，错误: {}", apiName, attempt + 1, backoff, e.getMessage());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted for " + apiName, ie);
                    }
                } else if (!isRetryable(e)) {
                    throw e; // 不可重试的异常直接抛出
                }
            }
        }
        throw lastException;
    }

    /**
     * 判断异常是否可重试
     */
    public boolean isRetryable(Exception e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) msg = "";

        // 精确匹配网络异常
        if (e instanceof SocketTimeoutException) return true;
        if (e instanceof ConnectException) return true;
        if (e instanceof SocketException) return true;
        if (e instanceof UnknownHostException) return true;

        // 钉钉限流错误
        if (msg.contains("isv.limitedFrequency") || msg.contains("限流")) return true;
        if (msg.contains("isp.") || msg.contains("服务不可用")) return true;

        return false;
    }

    /**
     * 判断异常是否致命（不应继续处理当前公司）
     */
    public boolean isFatal(Exception e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) msg = "";
        // Token无效
        if (msg.contains("不存在登录帐号") || msg.contains("invalid access_token")) return true;
        // 数据库不可用
        if (e instanceof org.springframework.dao.DataAccessResourceFailureException) return true;
        return false;
    }

    public synchronized void setPermitsPerSecond(double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive, got: " + permitsPerSecond);
        }
        this.permitsPerSecond = permitsPerSecond;
        logger.info("DingTalkRateLimiter permitsPerSecond updated to {}", permitsPerSecond);
    }

    @FunctionalInterface
    public interface ApiCallable<T> {
        T call() throws Exception;
    }
}
