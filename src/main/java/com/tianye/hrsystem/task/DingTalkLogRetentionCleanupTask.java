package com.tianye.hrsystem.task;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.tianye.hrsystem.repository.ddtaskresultRepository;
import com.tianye.hrsystem.repository.postresultlogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉链路日志表保留期清理任务。
 *
 * 背景：postresultlog（每次钉钉调用的完整响应 JSON）与 ddtaskresult（报表落库队列，处理后仅置 processed=200）
 * 只写不删，表随调用量无界膨胀；用量统计/队列消费查询会把整月大 JSON 行载入堆。
 *
 * 说明：项目 @Scheduled 调度被 hrm.scheduling.enabled=false 统一关闭（SchedulerConfig 会清空注册），
 * 因此本任务使用独立 daemon ScheduledExecutorService，保证任何 profile 下都能运行。
 * 删除按 2000 条一批、单批独立事务执行，避免大事务长时间占用租户连接。
 */
@Component
public class DingTalkLogRetentionCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkLogRetentionCleanupTask.class);

    /** 每批删除行数：控制单事务大小与主从延迟 */
    private static final int BATCH_SIZE = 2000;
    /** 每公司每表单次运行最多批数，超出部分留给下次运行，避免首轮清理长期占库 */
    private static final int MAX_BATCHES_PER_RUN = 50;
    /** ddtaskresult 已处理标记 */
    private static final int TASK_RESULT_PROCESSED = 200;
    /** 每天固定在 5 点附近执行一次 */
    private static final int RUN_HOUR = 5;

    @Autowired
    private LoginUserMapper loginUserMapper;
    @Autowired
    private postresultlogRepository postResultLogRepository;
    @Autowired
    private ddtaskresultRepository ddtaskResultRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Value("${hrm.system.database}")
    private String systemDatabase;

    @Value("${hrm.dingtalk.log-cleanup.enabled:true}")
    private boolean enabled;

    @Value("${hrm.dingtalk.log-cleanup.postresultlog-retention-days:90}")
    private int postResultLogRetentionDays;

    @Value("${hrm.dingtalk.log-cleanup.ddtaskresult-retention-days:7}")
    private int ddtaskResultRetentionDays;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "dingtalk-log-cleanup");
        t.setDaemon(true);
        return t;
    });

    private volatile int lastRunDay = -1;

    @PostConstruct
    public void init() {
        if (!enabled) {
            logger.info("钉钉日志表清理任务未启用（hrm.dingtalk.log-cleanup.enabled=false）");
            return;
        }
        // 启动后 5 分钟做首轮，之后每 30 分钟检查一次是否到达执行时间
        scheduler.scheduleWithFixedDelay(this::tick, 5, 30, TimeUnit.MINUTES);
        logger.info("钉钉日志表清理任务已启动: postresultlog保留{}天, ddtaskresult保留{}天",
                postResultLogRetentionDays, ddtaskResultRetentionDays);
    }

    private void tick() {
        try {
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != RUN_HOUR || lastRunDay == day) {
                return;
            }
            lastRunDay = day;
            cleanupAllCompanies();
        } catch (Exception e) {
            logger.error("钉钉日志表清理任务执行失败", e);
        }
    }

    void cleanupAllCompanies() {
        Date postLogCutoff = cutoffDate(postResultLogRetentionDays);
        Date taskResultCutoff = cutoffDate(ddtaskResultRetentionDays);
        List<Map<String, Object>> companies = loginUserMapper.getAllCompanies(systemDatabase);
        if (companies == null || companies.isEmpty()) {
            return;
        }
        for (Map<String, Object> company : companies) {
            Object companyIdObj = company.get("companyId");
            if (companyIdObj == null) {
                continue;
            }
            String companyId = String.valueOf(companyIdObj);
            try {
                cleanupOneCompany(companyId, postLogCutoff, taskResultCutoff);
            } catch (Exception e) {
                logger.error("清理公司{}的钉钉日志表失败，跳过该公司继续", companyId, e);
            }
        }
    }

    private void cleanupOneCompany(String companyId, Date postLogCutoff, Date taskResultCutoff) {
        LoginUserInfo previousContext = CompanyContext.get();
        try {
            switchCompanyContext(previousContext, companyId);
            int postLogDeleted = deletePostResultLogs(postLogCutoff);
            int taskResultDeleted = deleteTaskResults(taskResultCutoff);
            if (postLogDeleted > 0 || taskResultDeleted > 0) {
                logger.info("公司{}钉钉日志清理完成: postresultlog删除{}条, ddtaskresult删除{}条",
                        companyId, postLogDeleted, taskResultDeleted);
            }
        } finally {
            CompanyContext.set(previousContext);
        }
    }

    /** 批量删除：单批独立事务，超出 MAX_BATCHES_PER_RUN 留待下次运行 */
    private int deletePostResultLogs(Date cutoff) {
        Pageable limit = PageRequest.of(0, BATCH_SIZE);
        int total = 0;
        for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
            List<Integer> ids = postResultLogRepository.findIdsByCreateTimeLessThan(cutoff, limit);
            if (ids == null || ids.isEmpty()) {
                break;
            }
            Integer deleted = transactionTemplate.execute(status ->
                    postResultLogRepository.deleteByIdIn(ids));
            total += deleted == null ? 0 : deleted;
            if (ids.size() < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    private int deleteTaskResults(Date cutoff) {
        Pageable limit = PageRequest.of(0, BATCH_SIZE);
        int total = 0;
        for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
            List<Integer> ids = ddtaskResultRepository
                    .findIdsByProcessedAndCreatetimeLessThan(TASK_RESULT_PROCESSED, cutoff, limit);
            if (ids == null || ids.isEmpty()) {
                break;
            }
            Integer deleted = transactionTemplate.execute(status ->
                    ddtaskResultRepository.deleteByIdIn(ids));
            total += deleted == null ? 0 : deleted;
            if (ids.size() < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    private void switchCompanyContext(LoginUserInfo previousContext, String companyId) {
        LoginUserInfo target = previousContext != null ? previousContext : new LoginUserInfo();
        target.setCompanyId(companyId);
        CompanyContext.set(target);
    }

    private Date cutoffDate(int retentionDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -Math.max(retentionDays, 1));
        return calendar.getTime();
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }
}
