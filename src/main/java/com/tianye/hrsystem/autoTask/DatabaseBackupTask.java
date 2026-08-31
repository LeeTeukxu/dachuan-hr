package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.modules.backup.service.DatabaseBackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据库自动备份任务（独立于全局 scheduling.enabled 调度）
 * 按配置的小时列表（默认 1,13 即凌晨1点与午间13点）每天自动备份，
 * 并对同一小时做去重，避免重复执行。备份完成后清理过期备份。
 */
@Slf4j
@Component
public class DatabaseBackupTask {

    @Autowired
    private DatabaseBackupService backupService;

    @Value("${hr.backup.enabled:false}")
    private boolean backupEnabled;

    @Value("${hr.backup.auto.hours:1,13}")
    private String autoHours;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "db-backup-scheduler");
        t.setDaemon(true);
        return t;
    });

    private Set<Integer> backupHours;
    private volatile int lastBackupHour = -1;

    @PostConstruct
    public void init() {
        backupHours = new HashSet<>();
        for (String h : autoHours.split(",")) {
            try {
                backupHours.add(Integer.parseInt(h.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        log.info("数据库自动备份任务初始化: enabled={}, 小时={}", backupEnabled, autoHours);
        if (!backupEnabled) {
            return;
        }
        scheduler.scheduleWithFixedDelay(this::tick, 1, 30, TimeUnit.MINUTES);
    }

    private void tick() {
        try {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (!backupHours.contains(hour)) {
                lastBackupHour = -1;
                return;
            }
            if (lastBackupHour == hour) {
                return; // 本小时已备份过，去重
            }
            log.info("触发数据库自动备份（hour={}）", hour);
            backupService.createBackup();
            backupService.cleanupExpired();
            lastBackupHour = hour;
        } catch (Exception e) {
            log.error("数据库自动备份失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }
}
