package com.tianye.hrsystem.autoTask.common;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.HrmAutoTaskList;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAutoTaskListRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.util.ExceptionUtils;
import com.tianye.hrsystem.util.MyDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;

/**
 * 钉钉定时任务基类
 *
 * 模板方法模式：统一处理公司遍历、上下文、幂等、异常、进度、日志
 * 子类只需实现 doProcess() 或 processOneEmployee()
 *
 * 使用方式：
 * 1. 非员工维度的Task（如考勤组同步）：重写 doProcess(companyId, date)
 * 2. 员工维度的Task（如报表同步）：重写 processOneEmployee(companyId, user, date)
 *    并将 isEmployeeLevel() 返回 true
 */
public abstract class AbstractDingTalkTask {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected IAccessToken accessToken;
    @Autowired
    protected MyDateUtils dateUtils;
    @Autowired
    protected ExceptionUtils exceptionUtils;
    @Autowired
    protected TaskCheckpoint checkpoint;
    @Autowired
    protected DingTalkRateLimiter rateLimiter;
    @Autowired
    protected DingTalkApiTemplate apiTemplate;
    @Autowired
    protected hrmAutoTaskListRepository autoTaskListRep;
    @Autowired
    protected tbattendanceuserRepository attendanceUserRep;
    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Value("${hrm.system.databasesuffix:}")
    protected String databaseSuffix;

    /**
     * 测试模式：只处理指定公司（为空则处理所有公司）
     * 配置示例：dingtalk.test.company-id=0003
     */
    @Value("${dingtalk.test.company-id:}")
    protected String testCompanyId;

    // ==================== 子类需要实现的方法 ====================

    /**
     * 返回进度追踪字段名（如 "groupProcess", "userProcess" 等）
     * 返回 null 表示不需要进度追踪
     */
    protected abstract String getTaskFieldName();

    /**
     * 非员工维度的处理逻辑（子类按需重写）
     */
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 默认走员工维度处理
        executeForAllEmployees(companyId, currentDate);
    }

    /**
     * 员工维度的处理逻辑（子类按需重写）
     */
    protected void processOneEmployee(String companyId, tbattendanceuser user, Date currentDate) throws Exception {
        // 子类实现
    }

    /**
     * 是否需要月末才执行（默认true，与现有逻辑一致）
     */
    protected boolean requireEndOfMonth() {
        return true;
    }

    /**
     * 是否是员工维度的Task（默认false）
     */
    protected boolean isEmployeeLevel() {
        return false;
    }

    // ==================== 核心执行流程 ====================

    /**
     * 任务执行入口（由 @Scheduled 调用）
     */
    protected void execute() {
        logger.info("========== [{}] 任务开始 ==========", getClass().getSimpleName());

        // 致命异常标志：Token无效/数据库不可用时终止后续公司处理
        final boolean[] fatalOccurred = {false};

        accessToken.EachCompany(companyId -> {
            // 致命异常已发生，跳过后续所有公司
            if (fatalOccurred[0]) {
                logger.warn("[{}][{}] 因致命异常跳过", getClass().getSimpleName(), companyId);
                return;
            }

            // 公司过滤：如果配置了测试公司ID，只处理该公司
            if (testCompanyId != null && !testCompanyId.isEmpty() && !testCompanyId.equals(companyId)) {
                logger.debug("[{}][{}] 非测试公司，跳过（当前测试公司: {}）", getClass().getSimpleName(), companyId, testCompanyId);
                return;
            }

            try {
                setupCompanyContext(companyId);

                // 在公司上下文中获取当前处理日期（查询公司库的hrm_auto_taskList表）
                Date currentDate = dateUtils.getCurrent();

                // 空值检查
                if (currentDate == null) {
                    logger.error("[{}][{}] 获取当前日期失败，跳过", getClass().getSimpleName(), companyId);
                    return;
                }

                // 月末检查
                if (requireEndOfMonth() && !dateUtils.isEndOfMonth(currentDate)) {
                    logger.debug("[{}][{}] 非月末，跳过", getClass().getSimpleName(), companyId);
                    return;
                }

                // 幂等检查（Task级别）
                if (checkpoint.isDone(companyId, getClass(), currentDate)) {
                    logger.info("[{}][{}] 已执行过，跳过", getClass().getSimpleName(), companyId);
                    return;
                }

                long startTime = System.currentTimeMillis();
                logger.info("[{}][{}] 开始处理，目标日期: {}", getClass().getSimpleName(), companyId, currentDate);

                // 执行业务逻辑
                doProcess(companyId, currentDate);

                // 标记完成
                checkpoint.markDone(companyId, getClass(), currentDate);
                updateProgress(companyId, currentDate);

                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("[{}][{}] 处理完成，耗时{}ms", getClass().getSimpleName(), companyId, elapsed);

            } catch (Exception e) {
                if (rateLimiter.isFatal(e)) {
                    logger.error("[{}][{}] 致命异常，终止后续所有公司处理", getClass().getSimpleName(), companyId, e);
                    fatalOccurred[0] = true;
                } else {
                    logger.error("[{}][{}] 处理异常", getClass().getSimpleName(), companyId, e);
                }
                exceptionUtils.addOne(getClass(), e);
            } finally {
                CompanyContext.set(null);
            }
        });

        logger.info("========== [{}] 任务结束 ==========", getClass().getSimpleName());
    }

    // ==================== 员工批量处理（含断点续传） ====================

    /**
     * 遍历所有员工执行处理（含断点续传 + 两轮重试）
     */
    protected void executeForAllEmployees(String companyId, Date currentDate) throws Exception {
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        if (users.isEmpty()) {
            logger.warn("[{}][{}] 无考勤用户数据", getClass().getSimpleName(), companyId);
            return;
        }

        List<tbattendanceuser> failedUsers = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;

        // 第一轮：遍历所有员工
        for (tbattendanceuser user : users) {
            String userId = user.getUserId();

            // 断点续传：已成功的跳过
            if (checkpoint.isDone(companyId, getClass(), currentDate, userId)) {
                skipCount++;
                continue;
            }

            try {
                // 注意：不在此处 acquire 限流，由子类 processOneEmployee 内部
                // 通过 apiTemplate.execute() 统一限流，避免双重 acquire 导致吞吐量减半
                processOneEmployee(companyId, user, currentDate);
                checkpoint.markDone(companyId, getClass(), currentDate, userId);
                successCount++;
            } catch (Exception e) {
                // 立即重试3次（指数退避）
                boolean retryOk = retryForEmployee(companyId, user, currentDate);
                if (retryOk) {
                    checkpoint.markDone(companyId, getClass(), currentDate, userId);
                    successCount++;
                } else {
                    failedUsers.add(user);
                    checkpoint.markFailed(companyId, getClass(), currentDate, userId);
                    logger.warn("[{}][{}][{}] 第一轮处理失败", getClass().getSimpleName(), companyId, user.getUserName());
                    exceptionUtils.addOne(getClass(), e);
                }
            }
        }

        // 第二轮：补偿重试 FAILED 的员工
        int retrySuccessCount = 0;
        if (!failedUsers.isEmpty()) {
            logger.info("[{}][{}] 开始第二轮补偿重试，共{}人", getClass().getSimpleName(), companyId, failedUsers.size());
            try {
                Thread.sleep(5000); // 等待网络恢复
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("[{}][{}] 补偿重试等待被中断，终止流程", getClass().getSimpleName(), companyId);
                return;
            }

            for (tbattendanceuser user : failedUsers) {
                boolean retryOk = retryForEmployee(companyId, user, currentDate);
                if (retryOk) {
                    checkpoint.markDone(companyId, getClass(), currentDate, user.getUserId());
                    successCount++;
                    retrySuccessCount++;
                    logger.info("[{}][{}][{}] 补偿重试成功", getClass().getSimpleName(), companyId, user.getUserName());
                }
            }
        }

        logSummary(companyId, successCount, failedUsers.size() - retrySuccessCount, skipCount);
    }

    /**
     * 对单个员工进行重试（3次，指数退避）
     */
    private boolean retryForEmployee(String companyId, tbattendanceuser user, Date currentDate) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                long backoff = (long) (500 * Math.pow(2, attempt - 1));
                Thread.sleep(backoff);
                rateLimiter.acquire(companyId);
                processOneEmployee(companyId, user, currentDate);
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("[{}][{}][{}] 重试第{}次被中断，终止重试",
                        getClass().getSimpleName(), companyId, user.getUserName(), attempt);
                return false;
            } catch (Exception e) {
                logger.warn("[{}][{}][{}] 重试第{}次失败: {}",
                        getClass().getSimpleName(), companyId, user.getUserName(), attempt, e.getMessage());
            }
        }
        return false;
    }

    // ==================== 辅助方法 ====================

    /**
     * 设置公司上下文
     */
    protected void setupCompanyContext(String companyId) {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setSuffix(databaseSuffix);
        CompanyContext.set(info);
    }

    /**
     * 更新任务进度
     */
    protected void updateProgress(String companyId, Date currentDate) {
        String fieldName = getTaskFieldName();
        if (fieldName == null) return;

        try {
            Optional<HrmAutoTaskList> taskOpt = autoTaskListRep.findFirstByEndTime(currentDate);
            if (taskOpt.isPresent()) {
                HrmAutoTaskList task = taskOpt.get();
                switch (fieldName) {
                    case "groupProcess": task.setGroupProcess(true); break;
                    case "userProcess": task.setUserProcess(true); break;
                    case "planProcess": task.setPlanProcess(true); break;
                    case "detailProcess": task.setDetailProcess(true); break;
                    case "reportProcess": task.setReportProcess(true); break;
                    case "leaveProcess": task.setLeaveProcess(true); break;
                    default:
                        logger.warn("[{}][{}] 未知的进度字段: {}", getClass().getSimpleName(), companyId, fieldName);
                        return;
                }
                autoTaskListRep.save(task);
                logger.info("[{}][{}] 进度已更新: {}=true", getClass().getSimpleName(), companyId, fieldName);
            }
        } catch (Exception e) {
            logger.error("[{}][{}] 更新进度失败", getClass().getSimpleName(), companyId, e);
        }
    }

    /**
     * 批量保存数据（分批，每批独立事务）
     */
    protected <T> void batchSave(List<T> data, int batchSize, Consumer<List<T>> saveAction) {
        if (data == null || data.isEmpty()) return;

        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(i + batchSize, data.size());
            List<T> batch = new ArrayList<>(data.subList(i, end));
            try {
                transactionTemplate.execute(status -> {
                    saveAction.accept(batch);
                    return null;
                });
            } catch (Exception e) {
                logger.error("批量保存第{}-{}条失败: {}", i, end, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 全量替换（单事务内 delete + save）
     */
    protected <T> void replaceAll(Runnable deleteAction, List<T> data, Consumer<List<T>> saveAction) {
        transactionTemplate.execute(status -> {
            deleteAction.run();
            if (data != null && !data.isEmpty()) {
                saveAction.accept(data);
            }
            return null;
        });
    }

    // ==================== 日志方法 ====================

    protected void logSummary(String companyId, int success, int failed, int skipped) {
        int total = success + failed + skipped;
        logger.info("[{}][{}] 处理完成: 总计{}人, 成功{}人, 失败{}人, 跳过{}人",
                getClass().getSimpleName(), companyId, total, success, failed, skipped);
    }
}
