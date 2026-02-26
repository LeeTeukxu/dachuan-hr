package com.tianye.hrsystem.autoTask;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {
    @Value("${scheduling.enabled}")
    private boolean taskEnabled;

    /**
     * 调度线程池：仅负责触发定时任务
     * 8个线程足够（同时最多6个活跃Task + 1个每20秒的消费者）
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("task-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 业务线程池：数据库批量写入专用
     * core=5, max=10, 队列200
     * 拒绝策略：CallerRunsPolicy（调用者线程执行，起到限流作用）
     */
    @Bean("dbWriteExecutor")
    public ThreadPoolTaskExecutor dbWriteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("db-write-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!taskEnabled) {
            taskRegistrar.setTriggerTasks(Maps.newHashMap());
            taskRegistrar.setCronTasks(Maps.newHashMap());
            taskRegistrar.setFixedRateTasks(Maps.newHashMap());
            taskRegistrar.setFixedDelayTasks(Maps.newHashMap());
        }
        taskRegistrar.setScheduler(taskScheduler());
    }
}
