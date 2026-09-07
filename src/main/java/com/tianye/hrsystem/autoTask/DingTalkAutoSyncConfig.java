package com.tianye.hrsystem.autoTask;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 钉钉自动同步配置
 *
 * 配置示例（application.properties）：
 * hrm.auto-sync.enabled=false
 * hrm.auto-sync.cron=0 0 2 * * ?
 * hrm.auto-sync.max-concurrent=3
 * hrm.auto-sync.queue-capacity=10
 * hrm.auto-sync.test-company-id=
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hrm.auto-sync")
public class DingTalkAutoSyncConfig {

    /**
     * 是否启用自动同步（默认关闭，等服务器升级后再开放）
     */
    private boolean enabled = false;

    /**
     * Cron表达式：默认每天凌晨2点执行
     */
    private String cron = "0 0 2 * * ?";

    /**
     * 同时最多执行的租户数量（控制并发）
     */
    private int maxConcurrent = 3;

    /**
     * 等待队列容量
     */
    private int queueCapacity = 10;

    /**
     * 测试模式：只处理指定公司（留空则处理所有公司）
     */
    private String testCompanyId = "";

    /**
     * 任务超时时间（毫秒）：默认2小时
     */
    private long taskTimeoutMs = 2 * 60 * 60 * 1000L;

    /**
     * 重试次数
     */
    private int retryAttempts = 2;

    /**
     * 重试退避时间（毫秒）
     */
    private long retryBackoffMs = 60000L;
}
