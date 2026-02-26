package com.tianye.hrsystem.task;

import com.tianye.hrsystem.common.RedisImpl;
import com.tianye.hrsystem.imple.ddTalk.AttendanceUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 钉钉考勤缓存刷新任务
 * 定期清理考勤组缓存，确保数据一致性
 */
@Component
public class DingTalkAttendanceCacheRefreshTask {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkAttendanceCacheRefreshTask.class);

    @Autowired
    private RedisImpl redisImpl;

    @Autowired
    private AttendanceUserManager attendanceUserManager;

    /**
     * 每周日凌晨2点执行一次，清除所有考勤组缓存
     * 这样可以确保考勤组变更及时生效
     */
    @Scheduled(cron = "0 0 2 * * 1") // 每周一凌晨2点执行
    public void refreshAttendanceGroupCache() {
        try {
            logger.info("开始执行考勤组缓存刷新任务");

            // 获取所有考勤组缓存的key
            Set<Object> keys = redisImpl.keys("attendance:groupId:*");
            if (keys != null && !keys.isEmpty()) {
                int count = keys.size();
                logger.info("发现 {} 个考勤组缓存，准备删除", count);

                // 删除所有考勤组缓存
                redisImpl.del(keys.toArray());

                logger.info("成功删除 {} 个考勤组缓存", count);
            } else {
                logger.info("没有找到考勤组缓存");
            }

            logger.info("考勤组缓存刷新任务执行完成");
        } catch (Exception e) {
            logger.error("执行考勤组缓存刷新任务失败", e);
        }
    }

    /**
     * 手动触发考勤组缓存刷新
     */
    public void manualRefreshAttendanceGroupCache() {
        try {
            logger.info("手动执行考勤组缓存刷新任务");

            // 获取所有考勤组缓存的key
            Set<Object> keys = redisImpl.keys("attendance:groupId:*");
            if (keys != null && !keys.isEmpty()) {
                int count = keys.size();
                logger.info("发现 {} 个考勤组缓存，准备删除", count);

                // 删除所有考勤组缓存
                redisImpl.del(keys.toArray());

                logger.info("成功删除 {} 个考勤组缓存", count);
            }

            logger.info("手动考勤组缓存刷新任务执行完成");
        } catch (Exception e) {
            logger.error("执行手动考勤组缓存刷新任务失败", e);
        }
    }
}
