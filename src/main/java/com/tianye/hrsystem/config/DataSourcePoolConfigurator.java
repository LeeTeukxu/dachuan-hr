package com.tianye.hrsystem.config;

import lombok.extern.slf4j.Slf4j;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 统一配置动态创建的数据源连接池参数。
 *
 * SaaS 改造（数据源治理 #1）：租户池参数支持环境变量/配置覆盖，默认值收缩为
 * 小池+快回收，防止「租户数 × 池上限」超过 MySQL max_connections：
 * - 每租户最大连接数默认 8（原 20）
 * - 常驻空闲连接默认 0（原 2），配合 idleTimeout 快速回收
 */
@Component
@Slf4j
public class DataSourcePoolConfigurator {

    private static int maximumPoolSize = 8;
    private static int minimumIdle = 0;
    private static long idleTimeout = 180000L;

    @Value("${hrm.tenant.pool.max-size:8}")
    public void setMaximumPoolSize(int value) {
        maximumPoolSize = Math.max(2, value);
    }

    @Value("${hrm.tenant.pool.min-idle:0}")
    public void setMinimumIdle(int value) {
        minimumIdle = Math.max(0, value);
    }

    @Value("${hrm.tenant.pool.idle-timeout:180000}")
    public void setIdleTimeout(long value) {
        idleTimeout = Math.max(30000L, value);
    }

    @PostConstruct
    public void logSettings() {
        HikariDataSource probe = new HikariDataSource();
        try {
            apply(probe, "pool-settings-probe");
        } finally {
            probe.close();
        }
    }

    /** 按统一策略配置租户连接池（供静态调用） */
    public static void apply(HikariDataSource dataSource, String poolName) {
        if (dataSource == null) {
            return;
        }
        dataSource.setPoolName(poolName);
        // 常驻为 0 时，Hikari 仅在借出时建连、归还后按 idleTimeout 回收
        dataSource.setMinimumIdle(Math.min(minimumIdle, maximumPoolSize));
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(15000);
        dataSource.setValidationTimeout(3000);
        dataSource.setConnectionTestQuery("SELECT 1");
        // 低于常见 MySQL wait_timeout（30min），避免借出已被服务端回收的连接
        dataSource.setMaxLifetime(1200000);
    }
}
