package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.config.CompanyDataSourceProvider;
import com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.service.IHrmAttendanceApprovalService;
import com.tianye.hrsystem.task.AttendanceSyncTaskLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 钉钉审批数据自动获取定时任务
 *
 * 功能：
 * 1. 每天凌晨3点自动触发（比考勤同步晚1小时）
 * 2. 遍历所有租户，为每个租户提交审批数据获取任务
 * 3. 使用有界线程池控制并发（默认最多3个租户同时执行）
 * 4. 复用现有的 AttendanceSyncTaskLauncher 互斥机制
 *
 * 配置开关：hrm.auto-sync.enabled=false（默认关闭）
 */
@Component
@ConditionalOnProperty(name = "hrm.auto-sync.enabled", havingValue = "true", matchIfMissing = false)
public class DingTalkApprovalAutoSyncTask {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkApprovalAutoSyncTask.class);

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 默认审批类型
     */
    private static final List<String> DEFAULT_APPROVAL_TYPES = Arrays.asList("leave", "overtime", "misscard");

    @Autowired
    private DingTalkAutoSyncConfig config;

    @Autowired
    private AttendanceSyncTaskLauncher syncTaskLauncher;

    @Autowired
    private IHrmAttendanceApprovalService attendanceApprovalService;

    /**
     * 业务线程池：控制同时执行的租户数量
     */
    private final ThreadPoolExecutor executor;

    public DingTalkApprovalAutoSyncTask() {
        this.executor = new ThreadPoolExecutor(
                3,  // corePoolSize
                3,  // maxPoolSize
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                r -> {
                    Thread t = new Thread(r, "auto-sync-approval-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用者线程执行，起到限流作用
        );
    }

    /**
     * 定时任务入口
     * Cron表达式：每天凌晨3点执行（比考勤同步晚1小时）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void execute() {
        if (!config.isEnabled()) {
            logger.debug("[自动审批获取] 功能未启用，跳过执行");
            return;
        }

        logger.info("========== [自动审批获取] 任务开始 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 获取所有租户
            List<String> allCompanies = getAllCompanyIds();
            logger.info("[自动审批获取] 共{}个租户需要处理", allCompanies.size());

            // 使用 CountDownLatch 等待所有任务完成
            CountDownLatch latch = new CountDownLatch(allCompanies.size());
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            AtomicInteger skipCount = new AtomicInteger(0);

            for (String companyId : allCompanies) {
                try {
                    executor.submit(() -> {
                        try {
                            boolean submitted = processCompany(companyId);
                            if (submitted) {
                                successCount.incrementAndGet();
                            } else {
                                skipCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            logger.error("[自动审批获取][{}] 处理失败: {}", companyId, e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    failCount.incrementAndGet();
                    skipCount.incrementAndGet();
                    logger.warn("[自动审批获取][{}] 任务队列已满，跳过", companyId);
                    latch.countDown();
                }
            }

            // 等待所有任务完成（最多等待配置的超时时间）
            boolean completed = latch.await(config.getTaskTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!completed) {
                logger.warn("[自动审批获取] 等待超时，可能有任务未完成");
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("========== [自动审批获取] 任务结束，耗时{}ms，成功{}，失败{}，跳过{} ==========",
                    elapsed, successCount.get(), failCount.get(), skipCount.get());

        } catch (Exception e) {
            logger.error("[自动审批获取] 任务执行异常", e);
        } finally {
            // 不关闭 executor，因为它是单例的
        }
    }

    /**
     * 处理单个租户
     *
     * @return true=提交成功，false=跳过（已有任务在运行）
     */
    private boolean processCompany(String companyId) {
        logger.info("[自动审批获取][{}] 开始处理", companyId);

        // 设置租户上下文
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        CompanyContext.set(info);
        CompanyDataSourceProvider.markActive(companyId);

        try {
            // 尝试获取锁（复用现有的互斥机制）
            if (!attendanceApprovalService.tryBeginFetch(companyId)) {
                logger.info("[自动审批获取][{}] 已有获取任务在运行，跳过", companyId);
                return false;
            }

            // 构建审批数据获取参数
            // 获取上个月
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String monthStr = lastMonth.format(MONTH_FORMATTER);

            AttendanceApprovalMonthBO queryBO = new AttendanceApprovalMonthBO();
            queryBO.setMonth(monthStr);

            logger.info("[自动审批获取][{}] 获取月份: {}", companyId, monthStr);

            // 提交后台任务
            attendanceApprovalService.beginFetchProgress();
            boolean submitted = syncTaskLauncher.submit(companyId, info, () -> {
                try {
                    attendanceApprovalService.fetchMonthDataWithAutoRetry(queryBO);
                    logger.info("[自动审批获取][{}] 获取完成", companyId);
                } catch (Exception e) {
                    logger.error("[自动审批获取][{}] 获取失败: {}", companyId, e.getMessage());
                } finally {
                    attendanceApprovalService.finishFetch(companyId);
                    CompanyContext.clear();
                    CompanyDataSourceProvider.markInactive(companyId);
                }
            });

            if (!submitted) {
                logger.warn("[自动审批获取][{}] 任务提交失败（线程池已满）", companyId);
                attendanceApprovalService.finishFetch(companyId);
                return false;
            }

            logger.info("[自动审批获取][{}] 任务已提交", companyId);
            return true;

        } catch (Exception e) {
            logger.error("[自动审批获取][{}] 处理异常", companyId, e);
            attendanceApprovalService.finishFetch(companyId);
            CompanyContext.clear();
            CompanyDataSourceProvider.markInactive(companyId);
            throw e;
        }
    }

    /**
     * 获取所有租户ID
     */
    private List<String> getAllCompanyIds() {
        // 从系统库获取所有租户
        // 这里需要临时切换到系统库上下文
        LoginUserInfo systemContext = new LoginUserInfo();
        systemContext.setCompanyId("Default");
        CompanyContext.set(systemContext);

        try {
            // 从 ddAccount 表获取所有公司ID
            // 这里简化实现，实际应该从 tbcompanylist 获取
            List<String> companies = new java.util.ArrayList<>();

            // 从 Redis 缓存获取（复用现有的 EachCompany 逻辑）
            String cacheKey = "All::Company:List";
            // 这里应该从 Redis 获取，暂时返回空列表
            // 实际实现时需要注入 Redis

            return companies;
        } finally {
            CompanyContext.clear();
        }
    }
}
