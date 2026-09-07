package com.tianye.hrsystem.task;

import com.tianye.hrsystem.common.ProgressTracker;
import com.tianye.hrsystem.common.Redis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 进度心跳检测定时任务
 * 定期扫描考勤同步和审批获取的进度，检测超时任务并标记为 FAILED
 */
@Component
public class ProgressHeartbeatTask {

    private static final Logger logger = LoggerFactory.getLogger(ProgressHeartbeatTask.class);

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private Redis redis;

    @Value("${hrm.sync.heartbeat-timeout-ms:600000}")
    private long heartbeatTimeoutMs = 600000; // 默认10分钟

    @Value("${hrm.sync.heartbeat-enabled:true}")
    private boolean heartbeatEnabled = true;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "progress-heartbeat-checker");
        t.setDaemon(true);
        return t;
    });

    // 考勤同步前缀
    private static final String SYNC_KEY_PREFIX = "attendance:sync";
    // 审批获取前缀
    private static final String FETCH_KEY_PREFIX = "attendance:fetch";

    @PostConstruct
    public void init() {
        if (!heartbeatEnabled) {
            logger.info("进度心跳检测已禁用（hrm.sync.heartbeat-enabled=false）");
            return;
        }
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkStaleTasks(SYNC_KEY_PREFIX, "考勤同步");
                checkStaleTasks(FETCH_KEY_PREFIX, "审批获取");
            } catch (Exception e) {
                logger.warn("心跳检测异常", e);
            }
        }, 60, 60, TimeUnit.SECONDS);
        logger.info("进度心跳检测已启动，间隔60秒，超时阈值{}毫秒", heartbeatTimeoutMs);
    }

    /**
     * 检测指定前缀的超时任务
     * 扫描 Redis 中所有 {keyPrefix}:status:* 的键，检测是否超时
     */
    private void checkStaleTasks(String keyPrefix, String taskName) {
        try {
            String pattern = keyPrefix + ":status:*";
            java.util.Set<Object> keys = redis.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (Object keyObj : keys) {
                String key = String.valueOf(keyObj);
                // 从 key 中提取 companyId
                // key 格式: {keyPrefix}:status:{companyId}
                String companyId = key.substring(key.lastIndexOf(':') + 1);
                if (companyId.isEmpty()) {
                    continue;
                }

                // 检测超时
                progressTracker.detectStaleProgress(keyPrefix, companyId, heartbeatTimeoutMs);
            }
        } catch (Exception e) {
            logger.debug("扫描{}超时任务时发生异常: {}", taskName, e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
        logger.info("进度心跳检测已停止");
    }
}
