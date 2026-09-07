package com.tianye.hrsystem.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 公共进度追踪工具类
 * 用于考勤同步和审批获取的进度管理，统一 Redis 读写逻辑
 *
 * 使用方式：
 * - saveProgress() 保存进度
 * - queryProgress() 查询进度
 * - markQueued() 标记排队
 * - detectStaleProgress() 心跳检测超时任务
 * - clearProgress() 清理进度
 */
@Component
public class ProgressTracker {

    private static final Logger logger = LoggerFactory.getLogger(ProgressTracker.class);

    @Autowired
    private Redis redis;

    // 通用状态常量
    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // 默认过期时间：24小时
    private static final int DEFAULT_TTL_SECONDS = 86400;

    /**
     * 进度数据结构
     */
    public static class ProgressState {
        /** 进度百分比 0-100 */
        public int progress;
        /** 状态：IDLE/RUNNING/SUCCESS/FAILED */
        public String status = STATUS_IDLE;
        /** 显示消息 */
        public String message = "";
        /** 是否完成 */
        public boolean done;
        /** 是否成功 */
        public boolean success;
        /** 最后更新时间（毫秒时间戳） */
        public long updateTime;
        /** 排队时间（毫秒时间戳） */
        public long queuedAt;
        /** 错误信息列表 */
        public List<String> errors = Collections.emptyList();
    }

    /**
     * 保存进度到 Redis
     *
     * @param keyPrefix  前缀，如 "attendance:sync" 或 "attendance:fetch"
     * @param companyId  公司 ID
     * @param state      进度状态
     */
    public void saveProgress(String keyPrefix, String companyId, ProgressState state) {
        String base = keyPrefix + ":" + companyId;
        long now = System.currentTimeMillis();
        redis.setex(base + ":progress", DEFAULT_TTL_SECONDS, String.valueOf(state.progress));
        redis.setex(base + ":status", DEFAULT_TTL_SECONDS, state.status);
        redis.setex(base + ":message", DEFAULT_TTL_SECONDS, state.message);
        redis.setex(base + ":done", DEFAULT_TTL_SECONDS, String.valueOf(state.done));
        redis.setex(base + ":success", DEFAULT_TTL_SECONDS, String.valueOf(state.success));
        redis.setex(base + ":update_time", DEFAULT_TTL_SECONDS, String.valueOf(now));
        redis.setex(base + ":queued", DEFAULT_TTL_SECONDS, String.valueOf(state.queuedAt));
        if (state.errors != null && !state.errors.isEmpty()) {
            redis.setex(base + ":errors", DEFAULT_TTL_SECONDS, String.join("\n", state.errors));
        } else {
            redis.setex(base + ":errors", DEFAULT_TTL_SECONDS, "");
        }
    }

    /**
     * 从 Redis 查询进度
     *
     * @param keyPrefix  前缀
     * @param companyId  公司 ID
     * @return 进度状态
     */
    public ProgressState queryProgress(String keyPrefix, String companyId) {
        String base = keyPrefix + ":" + companyId;
        ProgressState state = new ProgressState();
        state.progress = parseInt(redis.get(base + ":progress"));
        state.status = getStringOrDefault(redis.get(base + ":status"), STATUS_IDLE);
        state.message = getStringOrDefault(redis.get(base + ":message"), "");
        state.done = parseBoolean(redis.get(base + ":done"));
        state.success = parseBoolean(redis.get(base + ":success"));
        state.updateTime = parseLong(redis.get(base + ":update_time"));
        state.queuedAt = parseLong(redis.get(base + ":queued"));
        String errors = redis.get(base + ":errors");
        state.errors = (errors != null && !errors.isEmpty())
                ? Arrays.asList(errors.split("\n"))
                : Collections.emptyList();
        return state;
    }

    /**
     * 标记排队（提交时调用）
     *
     * @param keyPrefix  前缀
     * @param companyId  公司 ID
     * @return 排队时间戳
     */
    public long markQueued(String keyPrefix, String companyId) {
        long queuedAt = System.currentTimeMillis();
        String base = keyPrefix + ":" + companyId;
        redis.setex(base + ":queued", DEFAULT_TTL_SECONDS, String.valueOf(queuedAt));
        return queuedAt;
    }

    /**
     * 心跳检测：检测超时任务
     * 如果 status=RUNNING 但 updateTime 超过阈值未更新，标记为 FAILED
     *
     * @param keyPrefix    前缀
     * @param companyId    公司 ID
     * @param timeoutMs    超时阈值（毫秒）
     * @return true 表示已标记为 FAILED
     */
    public boolean detectStaleProgress(String keyPrefix, String companyId, long timeoutMs) {
        ProgressState state = queryProgress(keyPrefix, companyId);
        if (!STATUS_RUNNING.equals(state.status)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (state.updateTime > 0 && (now - state.updateTime) > timeoutMs) {
            logger.warn("检测到任务超时：key={}, companyId={}, updateTime={}ms 前，标记为 FAILED",
                    keyPrefix, companyId, now - state.updateTime);
            state.status = STATUS_FAILED;
            state.done = true;
            state.success = false;
            state.message = "任务超时，可能因服务器重启或网络中断导致";
            state.errors = Collections.singletonList(state.message);
            saveProgress(keyPrefix, companyId, state);
            return true;
        }
        return false;
    }

    /**
     * 清理进度
     *
     * @param keyPrefix  前缀
     * @param companyId  公司 ID
     */
    public void clearProgress(String keyPrefix, String companyId) {
        String base = keyPrefix + ":" + companyId;
        redis.del(base + ":progress", base + ":status", base + ":message",
                base + ":done", base + ":success", base + ":update_time",
                base + ":queued", base + ":errors");
    }

    // ==================== 辅助方法 ====================

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0L : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }

    private String getStringOrDefault(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
