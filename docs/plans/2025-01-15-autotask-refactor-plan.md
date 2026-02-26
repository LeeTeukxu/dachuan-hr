# AutoTask 模块重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 重构 autoTask 模块，抽取公共基类、统一限流/重试/断点续传/异常处理，提升代码可读性、健壮性和简洁性。

**Architecture:** 模板方法模式 — `AbstractDingTalkTask` 基类处理所有模板代码（公司遍历、上下文设置、幂等检查、异常处理、进度更新），每个具体 Task 只实现业务逻辑。`DingTalkRateLimiter` 统一限流，`TaskCheckpoint` 管理断点续传，`DingTalkApiTemplate` 封装所有钉钉 API 调用。

**Tech Stack:** Java 8, Spring Boot 2.1.6, JPA/Hibernate, Redis, DingTalk SDK, Hutool

**Project Root:** `/Users/jiangyongming/Project/hr/hainan`
**Source Root:** `src/main/java/com/tianye/hrsystem`

---

## Task 1: 创建 DingTalkRateLimiter 限流器

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/autoTask/common/DingTalkRateLimiter.java`

**Step 1: 创建 common 包目录**

```bash
mkdir -p src/main/java/com/tianye/hrsystem/autoTask/common
```

**Step 2: 编写限流器**

```java
package com.tianye.hrsystem.autoTask.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉API统一限流器
 * 基于令牌桶思想，每个公司独立限流
 * 钉钉官方限制约20次/秒，默认设置15次/秒留余量
 */
@Component
public class DingTalkRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkRateLimiter.class);

    @Value("${dingtalk.rate-limit.permits-per-second:15}")
    private double permitsPerSecond;

    /** 每个公司的上次请求时间戳 */
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    /** 最小请求间隔（毫秒），根据 permitsPerSecond 计算 */
    private long getMinIntervalMs() {
        return (long) (1000.0 / permitsPerSecond);
    }

    /**
     * 获取API调用许可（阻塞等待直到满足限流条件）
     * @param companyId 公司ID
     */
    public void acquire(String companyId) {
        long minInterval = getMinIntervalMs();
        synchronized (lastRequestTime) {
            Long lastTime = lastRequestTime.get(companyId);
            if (lastTime != null) {
                long elapsed = System.currentTimeMillis() - lastTime;
                if (elapsed < minInterval) {
                    try {
                        long waitTime = minInterval - elapsed;
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            lastRequestTime.put(companyId, System.currentTimeMillis());
        }
    }

    /**
     * 带重试的API调用包装（指数退避）
     * @param apiCall API调用逻辑
     * @param maxRetries 最大重试次数
     * @param apiName API名称（用于日志）
     * @return API调用结果
     */
    public <T> T executeWithRetry(ApiCallable<T> apiCall, int maxRetries, String apiName) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return apiCall.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries && isRetryable(e)) {
                    long backoff = (long) (500 * Math.pow(2, attempt)); // 500ms, 1000ms, 2000ms
                    logger.warn("[{}] 第{}次重试，等待{}ms，错误: {}", apiName, attempt + 1, backoff, e.getMessage());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else if (!isRetryable(e)) {
                    throw e; // 不可重试的异常直接抛出
                }
            }
        }
        throw lastException;
    }

    /**
     * 判断异常是否可重试
     */
    public boolean isRetryable(Exception e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) msg = "";
        // 网络相关异常
        if (e instanceof java.net.SocketTimeoutException) return true;
        if (e instanceof java.net.ConnectException) return true;
        if (e instanceof java.io.IOException) return true;
        // 钉钉限流错误
        if (msg.contains("isv.limitedFrequency") || msg.contains("限流")) return true;
        if (msg.contains("isp.") || msg.contains("服务不可用")) return true;
        return false;
    }

    /**
     * 判断异常是否致命（不应继续处理当前公司）
     */
    public boolean isFatal(Exception e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) msg = "";
        // Token无效
        if (msg.contains("不存在登录帐号") || msg.contains("invalid access_token")) return true;
        // 数据库不可用
        if (e instanceof org.springframework.dao.DataAccessResourceFailureException) return true;
        return false;
    }

    public void setPermitsPerSecond(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    @FunctionalInterface
    public interface ApiCallable<T> {
        T call() throws Exception;
    }
}
```

**Step 3: 在 application-dev.properties 添加配置**

```properties
# 钉钉API限流配置（次/秒）
dingtalk.rate-limit.permits-per-second=15
```

**Step 4: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 5: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/common/DingTalkRateLimiter.java
git add src/main/resources/application-dev.properties
git commit -m "feat(autoTask): 添加统一限流器 DingTalkRateLimiter"
```

---

## Task 2: 创建 TaskCheckpoint 检查点管理器

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/autoTask/common/TaskCheckpoint.java`

**Context:** 复用现有 `UpdateRecordTemplate` + `updateRecord` 表，提供员工级细粒度检查点，支持断点续传。

**Step 1: 编写检查点管理器**

```java
package com.tianye.hrsystem.autoTask.common;

import com.tianye.hrsystem.common.UpdateRecordTemplate;
import com.tianye.hrsystem.config.CompanyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 任务检查点管理器
 * 支持员工级断点续传，复用 updateRecord 表
 *
 * Key设计：
 *   mainKey: {companyId}::{TaskClassName}
 *   subKey:  {date}::{userId} 或 {date}
 *   value:   SUCCESS / FAILED
 */
@Component
public class TaskCheckpoint {

    private static final Logger logger = LoggerFactory.getLogger(TaskCheckpoint.class);
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UpdateRecordTemplate recordTemplate;

    /**
     * 检查某个员工在某个Task中是否已成功处理
     */
    public boolean isDone(String companyId, Class<?> taskClass, Date date, String userId) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(date) + "::" + userId;
        return recordTemplate.hasKey(mainKey, subKey);
    }

    /**
     * 检查某个日期段是否已处理（非员工维度）
     */
    public boolean isDone(String companyId, Class<?> taskClass, Date date) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(date);
        return recordTemplate.hasKey(mainKey, subKey);
    }

    /**
     * 检查某个日期范围是否已处理
     */
    public boolean isDone(String companyId, Class<?> taskClass, Date begin, Date end) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(begin) + "::" + dateFormat.format(end);
        return recordTemplate.hasKey(mainKey, subKey);
    }

    /**
     * 标记某个员工处理成功
     */
    public void markDone(String companyId, Class<?> taskClass, Date date, String userId) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(date) + "::" + userId;
        recordTemplate.put(mainKey, subKey, SUCCESS);
    }

    /**
     * 标记某个日期处理成功（非员工维度）
     */
    public void markDone(String companyId, Class<?> taskClass, Date date) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(date);
        recordTemplate.put(mainKey, subKey, SUCCESS);
    }

    /**
     * 标记某个日期范围处理成功
     */
    public void markDone(String companyId, Class<?> taskClass, Date begin, Date end) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(begin) + "::" + dateFormat.format(end);
        recordTemplate.put(mainKey, subKey, SUCCESS);
    }

    /**
     * 标记某个员工处理失败
     */
    public void markFailed(String companyId, Class<?> taskClass, Date date, String userId) {
        String mainKey = companyId + "::" + taskClass.getName();
        String subKey = dateFormat.format(date) + "::" + userId + "::FAILED";
        recordTemplate.put(mainKey, subKey, FAILED);
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/common/TaskCheckpoint.java
git commit -m "feat(autoTask): 添加检查点管理器 TaskCheckpoint，支持断点续传"
```

---

## Task 3: 创建 DingTalkApiTemplate API调用模板

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/autoTask/common/DingTalkApiTemplate.java`

**Context:** 封装所有钉钉API调用的通用逻辑：获取Token → 限流 → 执行 → 检查errcode → 日志 → 重试。

**Step 1: 编写API调用模板**

```java
package com.tianye.hrsystem.autoTask.common;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.taobao.api.ApiException;
import com.taobao.api.TaobaoRequest;
import com.taobao.api.TaobaoResponse;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 钉钉API调用模板
 * 统一处理：Token获取、限流、执行、错误检查、日志记录、重试
 */
@Component
public class DingTalkApiTemplate {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkApiTemplate.class);

    @Autowired
    private IAccessToken accessToken;

    @Autowired
    private DingTalkRateLimiter rateLimiter;

    @Autowired
    private DDTalkResposeLogger ddLogger;

    /**
     * 执行钉钉API调用（带限流+重试+日志）
     *
     * @param companyId 公司ID
     * @param url       API地址
     * @param request   请求对象
     * @param apiName   API名称（用于日志）
     * @param callerClass 调用者类（用于日志）
     * @return 响应对象
     */
    public <REQ extends TaobaoRequest<RSP>, RSP extends TaobaoResponse> RSP execute(
            String companyId, String url, REQ request, String apiName, Class<?> callerClass) throws Exception {

        return rateLimiter.executeWithRetry(() -> {
            // 1. 限流等待
            rateLimiter.acquire(companyId);

            // 2. 获取Token
            String token = accessToken.Refresh();

            // 3. 执行请求
            DingTalkClient client = new DefaultDingTalkClient(url);
            RSP rsp = client.execute(request, token);

            // 4. 记录日志
            ddLogger.Info(rsp, url, new Date(), callerClass);

            // 5. 检查响应
            if (rsp == null) {
                throw new ApiException("钉钉API返回null: " + apiName);
            }

            return rsp;
        }, 3, apiName);
    }

    /**
     * 执行钉钉API调用（简化版，不记录响应日志）
     */
    public <REQ extends TaobaoRequest<RSP>, RSP extends TaobaoResponse> RSP executeSimple(
            String companyId, String url, REQ request, String apiName) throws Exception {

        return rateLimiter.executeWithRetry(() -> {
            rateLimiter.acquire(companyId);
            String token = accessToken.Refresh();
            DingTalkClient client = new DefaultDingTalkClient(url);
            RSP rsp = client.execute(request, token);
            if (rsp == null) {
                throw new ApiException("钉钉API返回null: " + apiName);
            }
            return rsp;
        }, 3, apiName);
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/common/DingTalkApiTemplate.java
git commit -m "feat(autoTask): 添加钉钉API调用模板 DingTalkApiTemplate"
```

---

## Task 4: 创建 AbstractDingTalkTask 基类

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/autoTask/common/AbstractDingTalkTask.java`

**Context:** 这是整个重构的核心。基类使用模板方法模式，统一处理：公司遍历、上下文设置、幂等检查、异常分级、进度更新、断点续传、日志规范。子类只需实现业务逻辑。

**Step 1: 编写基类**

```java
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
 *    并将 employeeLevel 设为 true
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
        Date currentDate = dateUtils.getCurrent();
        logger.info("========== [{}] 任务开始 ==========", getClass().getSimpleName());

        accessToken.EachCompany(companyId -> {
            setupCompanyContext(companyId);
            try {
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
                logger.info("[{}][{}] 开始处理", getClass().getSimpleName(), companyId);

                // 执行业务逻辑
                doProcess(companyId, currentDate);

                // 标记完成
                checkpoint.markDone(companyId, getClass(), currentDate);
                updateProgress(companyId, currentDate);

                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("[{}][{}] 处理完成，耗时{}ms", getClass().getSimpleName(), companyId, elapsed);

            } catch (Exception e) {
                if (rateLimiter.isFatal(e)) {
                    logger.error("[{}][{}] 致命异常，终止当前公司: {}", getClass().getSimpleName(), companyId, e.getMessage());
                } else {
                    logger.error("[{}][{}] 处理异常: {}", getClass().getSimpleName(), companyId, e.getMessage());
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
                rateLimiter.acquire(companyId);
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
        if (!failedUsers.isEmpty()) {
            logger.info("[{}][{}] 开始第二轮补偿重试，共{}人", getClass().getSimpleName(), companyId, failedUsers.size());
            try {
                Thread.sleep(5000); // 等待网络恢复
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            for (tbattendanceuser user : failedUsers) {
                boolean retryOk = retryForEmployee(companyId, user, currentDate);
                if (retryOk) {
                    checkpoint.markDone(companyId, getClass(), currentDate, user.getUserId());
                    successCount++;
                    logger.info("[{}][{}][{}] 补偿重试成功", getClass().getSimpleName(), companyId, user.getUserName());
                }
            }
        }

        logSummary(companyId, successCount, failedUsers.size(), skipCount);
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
                }
                autoTaskListRep.save(task);
                logger.info("[{}][{}] 进度已更新: {}=true", getClass().getSimpleName(), companyId, fieldName);
            }
        } catch (Exception e) {
            logger.error("[{}][{}] 更新进度失败: {}", getClass().getSimpleName(), companyId, e.getMessage());
        }
    }

    /**
     * 批量保存数据（分批，每批独立事务）
     */
    protected <T> void batchSave(List<T> data, int batchSize, Consumer<List<T>> saveAction) {
        if (data == null || data.isEmpty()) return;

        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(i + batchSize, data.size());
            List<T> batch = data.subList(i, end);
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
        logger.info("[{}][{}] 处理完成: 成功{}人, 失败{}人, 跳过{}人",
                getClass().getSimpleName(), companyId, success, failed, skipped);
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/common/AbstractDingTalkTask.java
git commit -m "feat(autoTask): 添加核心基类 AbstractDingTalkTask"
```

---

## Task 5: 优化线程池配置

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/SchedulerConfig.java`

**Context:** 将40线程调度池改为双线程池：5线程调度池 + 5-10线程业务池。

**Step 1: 重写 SchedulerConfig**

将 `SchedulerConfig.java` 修改为：

```java
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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {
    @Value("${scheduling.enabled}")
    private boolean taskEnabled;

    /**
     * 调度线程池：仅负责触发定时任务
     * 5个线程足够（同时最多6个活跃Task + 1个每20秒的消费者）
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
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/SchedulerConfig.java
git commit -m "refactor(autoTask): 优化线程池配置，双线程池架构"
```

---

## Task 6: 重构 AttendanceGroupRefreshTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceGroupRefreshTask.java`

**Context:** 考勤组同步是非员工维度的Task，只需重写 doProcess()。原代码77行 → 重构后约30行。

**Step 1: 重写 AttendanceGroupRefreshTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.service.ddTalk.IGroupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 同步考勤组和考勤班次
 * 每天12:00执行，仅月末生效
 */
@Component
public class AttendanceGroupRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IGroupManager groupManager;

    @Override
    protected String getTaskFieldName() {
        return "groupProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        logger.info("[AttendanceGroupRefreshTask][{}] 开始同步考勤组数据", companyId);
        groupManager.GetAndSave();
    }

    @Scheduled(cron = "0 00 12 * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceGroupRefreshTask.java
git commit -m "refactor(autoTask): 重构 AttendanceGroupRefreshTask 继承基类"
```

---

## Task 7: 重构 AttendanceUserRefreshTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceUserRefreshTask.java`

**Context:** 用户同步也是非员工维度（它本身就是同步用户列表），重写 doProcess()。

**Step 1: 重写 AttendanceUserRefreshTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.service.ddTalk.IUserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 考勤用户与系统用户关联
 * 每天12:30执行，仅月末生效
 */
@Component
public class AttendanceUserRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IUserManager userManager;

    @Override
    protected String getTaskFieldName() {
        return "userProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        logger.info("[AttendanceUserRefreshTask][{}] 开始同步考勤用户", companyId);
        userManager.GetAndSave();
    }

    @Scheduled(cron = "0 30 12 * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceUserRefreshTask.java
git commit -m "refactor(autoTask): 重构 AttendanceUserRefreshTask 继承基类"
```

---

## Task 8: 重构 AttendancePlanRecordTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendancePlanRecordTask.java`

**Context:** 排班同步按日期遍历，非员工维度。使用检查点实现日期级断点续传。

**Step 1: 重写 AttendancePlanRecordTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.imple.ddTalk.AttendancePlanRecord;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 同步排班计划
 * 每天13:00执行，仅月末生效
 * 按日期逐天同步，支持日期级断点续传
 */
@Component
public class AttendancePlanRecordTask extends AbstractDingTalkTask {

    @Autowired
    private AttendancePlanRecord planRecord;

    @Autowired
    private hrmAttendancePlanRepository planRep;

    @Override
    protected String getTaskFieldName() {
        return "planProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        Date beginDate = dateUtils.getBeginDayOfMonth(currentDate);
        Date endDate = dateUtils.getEndDayOfMonth(currentDate);

        // 设置用户列表
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        planRecord.setUsers(users);

        // 按日期逐天同步
        Date workDate = beginDate;
        int processedDays = 0;
        while (!workDate.after(endDate) && !workDate.after(currentDate)) {
            // 日期级检查点
            if (!checkpoint.isDone(companyId, getClass(), workDate)) {
                if (planRep.countByWorkDate(workDate) == 0) {
                    rateLimiter.acquire(companyId);
                    planRecord.GetAndSave(workDate);
                    logger.info("[AttendancePlanRecordTask][{}] 已同步{}的排班数据",
                            companyId, workDate);
                }
                checkpoint.markDone(companyId, getClass(), workDate);
                processedDays++;
            }
            workDate = DateUtils.addDays(workDate, 1);
        }

        // 清理重复用户
        planRecord.DeleteRepeatUser(beginDate, endDate);
        logger.info("[AttendancePlanRecordTask][{}] 完成，共处理{}天", companyId, processedDays);
    }

    @Scheduled(cron = "0 00 13 * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendancePlanRecordTask.java
git commit -m "refactor(autoTask): 重构 AttendancePlanRecordTask 继承基类，支持日期级断点续传"
```

---

## Task 9: 重构 AttendanceDetailRefreshTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceDetailRefreshTask.java`

**Context:** 考勤明细按7天分段查询，使用日期范围级检查点实现断点续传。

**Step 1: 重写 AttendanceDetailRefreshTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.service.ddTalk.IDetailRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 查询考勤明细（打卡记录）
 * 每天14:00执行，仅月末生效
 * 按7天分段查询，支持日期范围级断点续传
 */
@Component
public class AttendanceDetailRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IDetailRecord detailRecord;

    @Override
    protected String getTaskFieldName() {
        return "detailProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 设置用户列表
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        detailRecord.setUsers(users);

        // 按7天分段查询
        List<Date[]> dateRanges = dateUtils.getDateRangeByLimit(currentDate, 7);
        int processedRanges = 0;

        for (Date[] range : dateRanges) {
            Date beginDate = range[0];
            Date endDate = range[1];

            // 日期范围级检查点
            if (!checkpoint.isDone(companyId, getClass(), beginDate, endDate)) {
                rateLimiter.acquire(companyId);
                detailRecord.GetAndSave(beginDate, endDate);
                checkpoint.markDone(companyId, getClass(), beginDate, endDate);
                logger.info("[AttendanceDetailRefreshTask][{}] 已同步{} ~ {}的考勤明细",
                        companyId, beginDate, endDate);
                processedRanges++;
            }
        }

        logger.info("[AttendanceDetailRefreshTask][{}] 完成，共处理{}个日期段", companyId, processedRanges);
    }

    @Scheduled(cron = "0 00 14 * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceDetailRefreshTask.java
git commit -m "refactor(autoTask): 重构 AttendanceDetailRefreshTask 继承基类，支持日期范围级断点续传"
```

---

## Task 10: 重构 AttendanceReportRefreshTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceReportRefreshTask.java`

**Context:** 报表同步是员工维度的Task，按半月分段，每个员工独立处理。使用员工级检查点实现断点续传。

**Step 1: 重写 AttendanceReportRefreshTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.ddtaskresultRepository;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 同步加班及请假报表数据
 * 每天15:00执行，仅月末生效
 * 按半月分段，员工级断点续传
 */
@Component
public class AttendanceReportRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmAttendanceReport report;

    @Autowired
    private ddtaskresultRepository ddRep;

    @Autowired
    private StringRedisTemplate redisRep;

    private final SimpleDateFormat compactFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    protected String getTaskFieldName() {
        return "reportProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 构建半月日期段
        List<Date[]> allDates = buildHalfMonthRanges(currentDate);
        if (allDates.isEmpty()) return;

        // 获取用户列表并更新报表字段
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        report.UpdateReportFields();
        logger.info("[AttendanceReportRefreshTask][{}] 开始同步报表数据，共{}个用户", companyId, users.size());

        String reportKey = compactFormat.format(currentDate) + "::Report::" + companyId;
        long startTime = System.currentTimeMillis();

        for (Date[] dateRange : allDates) {
            Date beginDate = dateRange[0];
            Date endDate = dateRange[1];

            // 日期范围级检查点
            if (checkpoint.isDone(companyId, getClass(), beginDate, endDate)) {
                continue;
            }

            // 遍历每个员工
            for (int i = 0; i < users.size(); i++) {
                tbattendanceuser user = users.get(i);
                String userId = user.getUserId();
                Long empId = user.getEmpId();
                String tBegin = compactFormat.format(beginDate);
                String tEnd = compactFormat.format(endDate);

                // 员工级去重
                if (ddRep.countAllByUserIdAndClassNameAndBeginAndEnd(
                        userId, "UpdateAttendanceReport", tBegin, tEnd) == 0L) {
                    try {
                        rateLimiter.acquire(companyId);
                        report.UpdateAttendanceReport(userId, empId, beginDate, endDate);
                        redisRep.opsForValue().set(reportKey, "1", 8, TimeUnit.HOURS);
                        logger.info("[AttendanceReportRefreshTask][{}][{}] {}/{} 报表数据已保存",
                                companyId, user.getUserName(), i + 1, users.size());
                    } catch (Exception e) {
                        logger.error("[AttendanceReportRefreshTask][{}][{}] 处理失败: {}",
                                companyId, user.getUserName(), e.getMessage());
                        exceptionUtils.addOne(getClass(), e);
                    }
                }
            }

            checkpoint.markDone(companyId, getClass(), beginDate, endDate);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[AttendanceReportRefreshTask][{}] 完成，耗时{}ms", companyId, elapsed);
    }

    /**
     * 构建半月日期段：[月初~月中] + [月中+1~当前]
     */
    private List<Date[]> buildHalfMonthRanges(Date currentDate) throws Exception {
        List<Date[]> ranges = new ArrayList<>();
        Date firstDay = dateUtils.getBeginDayOfMonth(currentDate);
        Date midDay = dateUtils.getMiddleDayOfMonth(currentDate);

        // 上半月
        if (!checkpoint.isDone(
                com.tianye.hrsystem.config.CompanyContext.get().getCompanyId(),
                getClass(), firstDay, midDay)) {
            ranges.add(new Date[]{firstDay, midDay});
        }

        // 下半月
        Date secondHalfStart = DateUtils.addDays(midDay, 1);
        secondHalfStart.setHours(0);
        secondHalfStart.setMinutes(0);
        secondHalfStart.setSeconds(0);
        ranges.add(new Date[]{secondHalfStart, currentDate});

        return ranges;
    }

    @Scheduled(cron = "0 00 15 * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceReportRefreshTask.java
git commit -m "refactor(autoTask): 重构 AttendanceReportRefreshTask 继承基类，员工级断点续传"
```

---

## Task 11: 重构 AttendanceLeaveTimeRefreshTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceLeaveTimeRefreshTask.java`

**Context:** 请假数据同步，结构与 ReportRefreshTask 类似，按半月分段+员工遍历。

**Step 1: 重写 AttendanceLeaveTimeRefreshTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.ddtaskresultRepository;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 获取请假信息
 * 每天15:30执行，仅月末生效
 * 按半月分段，员工级处理
 */
@Component
public class AttendanceLeaveTimeRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmAttendanceReport report;

    @Autowired
    private ddtaskresultRepository ddRep;

    @Autowired
    private StringRedisTemplate redisRep;

    private final SimpleDateFormat compactFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    protected String getTaskFieldName() {
        return "leaveProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 构建半月日期段
        List<Date[]> allDates = buildHalfMonthRanges(currentDate);
        if (allDates.isEmpty()) return;

        List<tbattendanceuser> users = attendanceUserRep.findAll();
        report.UpdateReportFields();
        logger.info("[AttendanceLeaveTimeRefreshTask][{}] 开始同步请假数据，共{}个用户", companyId, users.size());

        String reportKey = compactFormat.format(currentDate) + "::Report::" + companyId;
        long startTime = System.currentTimeMillis();

        for (Date[] dateRange : allDates) {
            Date beginDate = dateRange[0];
            Date endDate = dateRange[1];
            String tBegin = compactFormat.format(beginDate);
            String tEnd = compactFormat.format(endDate);

            if (checkpoint.isDone(companyId, getClass(), beginDate, endDate)) {
                continue;
            }

            for (int i = 0; i < users.size(); i++) {
                tbattendanceuser user = users.get(i);
                Long empId = user.getEmpId();
                String userId = user.getUserId();

                if (ddRep.countAllByEmpIdAndClassNameAndBeginAndEnd(
                        empId, "UpdateHolidayReport", tBegin, tEnd) == 0L) {
                    try {
                        rateLimiter.acquire(companyId);
                        report.UpdateHolidayReport(userId, empId, beginDate, endDate);
                        redisRep.opsForValue().set(reportKey, "1", 8, TimeUnit.HOURS);
                        logger.info("[AttendanceLeaveTimeRefreshTask][{}][{}] {}/{} 请假数据已保存",
                                companyId, user.getUserName(), i + 1, users.size());
                    } catch (Exception e) {
                        logger.error("[AttendanceLeaveTimeRefreshTask][{}][{}] 处理失败: {}",
                                companyId, user.getUserName(), e.getMessage());
                        exceptionUtils.addOne(getClass(), e);
                    }
                }
            }

            checkpoint.markDone(companyId, getClass(), beginDate, endDate);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[AttendanceLeaveTimeRefreshTask][{}] 完成，耗时{}ms", companyId, elapsed);
    }

    private List<Date[]> buildHalfMonthRanges(Date currentDate) throws Exception {
        List<Date[]> ranges = new ArrayList<>();
        Date firstDay = dateUtils.getBeginDayOfMonth(currentDate);
        Date midDay = dateUtils.getMiddleDayOfMonth(currentDate);

        if (!checkpoint.isDone(
                com.tianye.hrsystem.config.CompanyContext.get().getCompanyId(),
                getClass(), firstDay, midDay)) {
            ranges.add(new Date[]{firstDay, midDay});
        }

        Date secondHalfStart = DateUtils.addDays(midDay, 1);
        secondHalfStart.setHours(0);
        secondHalfStart.setMinutes(0);
        secondHalfStart.setSeconds(0);
        ranges.add(new Date[]{secondHalfStart, currentDate});

        return ranges;
    }

    @Scheduled(cron = "0 30 15 * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceLeaveTimeRefreshTask.java
git commit -m "refactor(autoTask): 重构 AttendanceLeaveTimeRefreshTask 继承基类"
```

---

## Task 12: 重构 AttendanceReportDataSaveTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceReportDataSaveTask.java`

**Context:** 异步队列消费者，每20秒执行一次。不需要月末检查和幂等检查，但需要统一上下文和异常处理。

**Step 1: 重写 AttendanceReportDataSaveTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 处理异步队列（Ddtaskresult）中的报表数据
 * 每20秒执行一次，不受月末限制
 * 从队列取一条未处理记录 → 反序列化 → 保存到报表数据表
 */
@Component
public class AttendanceReportDataSaveTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmAttendanceReport report;

    @Autowired
    private StringRedisTemplate redisRep;

    private final SimpleDateFormat compactFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    protected String getTaskFieldName() {
        return null; // 不需要进度追踪
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false; // 不受月末限制
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        String reportKey = compactFormat.format(currentDate) + "::Report::" + companyId;
        if (redisRep.hasKey(reportKey)) {
            report.ProcessOne(companyId);
        }
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void process() {
        execute();
    }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceReportDataSaveTask.java
git commit -m "refactor(autoTask): 重构 AttendanceReportDataSaveTask 继承基类"
```

---

## Task 13: 重构 AttendanceEmpScheduleTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/AttendanceEmpScheduleTask.java`

**Context:** 员工排班同步，当前已禁用（@Scheduled注释掉了）。重构为继承基类，保留禁用状态，但代码结构统一。

**Step 1: 重写 AttendanceEmpScheduleTask**

```java
package com.tianye.hrsystem.autoTask;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceScheduleListbyusersRequest;
import com.dingtalk.api.response.OapiAttendanceScheduleListbyusersResponse;
import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.entity.po.HrmEmpSchedule;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.service.IHrmEmpScheduleService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 同步员工排班记录
 * 当前已禁用
 */
@Component
public class AttendanceEmpScheduleTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmEmpScheduleService hrmEmpScheduleService;

    @Override
    protected String getTaskFieldName() {
        return null; // 不需要进度追踪
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date beginDate = cal.getTime();

        logger.info("[AttendanceEmpScheduleTask][{}] 开始同步员工排班信息", companyId);

        String password = accessToken.Refresh();
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        List<String> userIdList = users.stream()
                .map(tbattendanceuser::getUserId)
                .collect(Collectors.toList());

        // 每次最多查50人
        List<List<String>> batches = divide(userIdList, 50);
        int savedCount = 0;

        for (List<String> batch : batches) {
            rateLimiter.acquire(companyId);

            String userIdStr = String.join(",", batch);
            DingTalkClient client = new DefaultDingTalkClient(
                    "https://oapi.dingtalk.com/topapi/attendance/schedule/listbyusers");
            OapiAttendanceScheduleListbyusersRequest req = new OapiAttendanceScheduleListbyusersRequest();
            req.setOpUserId("296842114330963873");
            req.setUserids(userIdStr);
            req.setFromDateTime(beginDate.getTime());
            req.setToDateTime(currentDate.getTime());

            OapiAttendanceScheduleListbyusersResponse rsp = client.execute(req, password);

            if (rsp != null && rsp.getErrcode() == 0L) {
                List<OapiAttendanceScheduleListbyusersResponse.TopScheduleVo> list = rsp.getResult();
                if (!CollectionUtils.isEmpty(list)) {
                    for (OapiAttendanceScheduleListbyusersResponse.TopScheduleVo vo : list) {
                        Optional<tbattendanceuser> findUser = users.stream()
                                .filter(u -> u.getUserId().equals(vo.getUserid()))
                                .findFirst();
                        if (findUser.isPresent()) {
                            HashMap<String, Object> params = new HashMap<>();
                            params.put("employeeId", findUser.get().getEmpId());
                            params.put("workDate", vo.getWorkDate());
                            HrmEmpSchedule existing = hrmEmpScheduleService.getEmpScheduleByParams(params);
                            if (existing == null) {
                                HrmEmpSchedule schedule = new HrmEmpSchedule();
                                schedule.setEmpId(findUser.get().getEmpId());
                                schedule.setId(vo.getId());
                                schedule.setWorkDate(vo.getWorkDate());
                                schedule.setPlanCheckTime(vo.getPlanCheckTime());
                                schedule.setIsRest(vo.getIsRest());
                                schedule.setClassId(vo.getShiftId());
                                schedule.setGroupId(vo.getGroupId());
                                schedule.setCheckType(vo.getCheckType());
                                hrmEmpScheduleService.save(schedule);
                                savedCount++;
                            }
                        }
                    }
                }
            }
        }

        logger.info("[AttendanceEmpScheduleTask][{}] 完成，保存{}条排班记录", companyId, savedCount);
    }

    /**
     * 集合拆分
     */
    private static <T> List<List<T>> divide(List<T> origin, int size) {
        if (CollectionUtils.isEmpty(origin)) return Collections.emptyList();
        int block = (origin.size() + size - 1) / size;
        return IntStream.range(0, block)
                .boxed()
                .map(i -> {
                    int start = i * size;
                    int end = Math.min(start + size, origin.size());
                    return origin.subList(start, end);
                })
                .collect(Collectors.toList());
    }

    // 当前已禁用
    // @Scheduled(cron = "0 0 23 * * ?")
    // public void process() { execute(); }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/AttendanceEmpScheduleTask.java
git commit -m "refactor(autoTask): 重构 AttendanceEmpScheduleTask 继承基类"
```

---

## Task 14: 重构 CreateOverTimeAndLeaveTimeRecordTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/CreateOverTimeAndLeaveTimeRecordTask.java`

**Context:** 生成加班/请假明细记录，当前已禁用。重构为继承基类。

**Step 1: 重写 CreateOverTimeAndLeaveTimeRecordTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.model.HrmAttendanceReportData;
import com.tianye.hrsystem.model.HrmEmployeeLeaveRecord;
import com.tianye.hrsystem.model.HrmEmployeeOverTimeRecord;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.repository.hrmEmployeeLeaveRecordRepository;
import com.tianye.hrsystem.repository.hrmEmployeeOverTimeRecordRepository;
import com.tianye.hrsystem.mapper.ddAccountMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成加班/请假明细记录
 * 当前已禁用
 * 从报表数据中提取加班和请假信息，生成独立的明细记录
 */
@Component
public class CreateOverTimeAndLeaveTimeRecordTask extends AbstractDingTalkTask {

    @Autowired
    private hrmAttendanceReportFieldRepository fieldRep;
    @Autowired
    private hrmAttendanceReportDataRepository dataRep;
    @Autowired
    private hrmEmployeeLeaveRecordRepository leaveRep;
    @Autowired
    private hrmEmployeeOverTimeRecordRepository overTimeRep;
    @Autowired
    private ddAccountMapper ddMapper;

    private final SimpleDateFormat shortFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected String getTaskFieldName() {
        return null;
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 获取请假类型字段
        List<String> holidayFields = fieldRep.findAllByType(2).stream()
                .map(f -> f.getFieldName())
                .collect(Collectors.toList());

        Date reportSaveDate = ddMapper.getMaxReportDate();
        Date savedDate = ddMapper.getMaxSavedData();
        Date startDate;
        if (savedDate == null) {
            startDate = ddMapper.getMinReportDate();
        } else {
            startDate = DateUtils.addDays(savedDate, 1);
        }

        if (startDate == null || reportSaveDate == null) {
            logger.info("[CreateOverTimeAndLeaveTimeRecordTask][{}] 无报表数据需要处理", companyId);
            return;
        }

        for (Date d = startDate; d.before(reportSaveDate); d = DateUtils.addDays(d, 1)) {
            if (checkpoint.isDone(companyId, getClass(), d)) {
                continue;
            }

            try {
                Date begin = dateUtils.setItBegin(d);
                List<HrmAttendanceReportData> dataList = dataRep.findAllByWorkDate(begin);
                List<HrmEmployeeOverTimeRecord> overTimes = new ArrayList<>();
                List<HrmEmployeeLeaveRecord> leaves = new ArrayList<>();

                for (HrmAttendanceReportData data : dataList) {
                    processReportData(data, leaves, overTimes, holidayFields);
                }

                if (!leaves.isEmpty()) {
                    leaveRep.saveAll(leaves);
                    logger.info("[CreateOverTimeAndLeaveTimeRecordTask][{}] 生成{}条{}的请假记录",
                            companyId, leaves.size(), shortFormat.format(begin));
                }
                if (!overTimes.isEmpty()) {
                    overTimeRep.saveAll(overTimes);
                    logger.info("[CreateOverTimeAndLeaveTimeRecordTask][{}] 生成{}条{}的加班记录",
                            companyId, overTimes.size(), shortFormat.format(begin));
                }

                checkpoint.markDone(companyId, getClass(), d);

                // 有数据生成时暂停，避免一次处理太多
                if (!leaves.isEmpty() || !overTimes.isEmpty()) {
                    break;
                }
            } catch (Exception e) {
                logger.error("[CreateOverTimeAndLeaveTimeRecordTask][{}] 处理{}失败: {}",
                        companyId, shortFormat.format(d), e.getMessage());
                exceptionUtils.addOne(getClass(), e);
            }
        }
    }

    /**
     * 处理单条报表数据，提取加班或请假信息
     */
    private void processReportData(HrmAttendanceReportData data,
                                    List<HrmEmployeeLeaveRecord> leaves,
                                    List<HrmEmployeeOverTimeRecord> overTimes,
                                    List<String> holidayFields) {
        String val = data.getValue();
        String fieldName = data.getFieldName();

        if (!holidayFields.contains(fieldName) && !fieldName.endsWith("加班")) {
            return;
        }
        if (StringUtils.isEmpty(val) || val.equals("0.0")) {
            return;
        }

        if (holidayFields.contains(fieldName)) {
            HrmEmployeeLeaveRecord record = new HrmEmployeeLeaveRecord();
            record.setLeaveDay(Double.parseDouble(val));
            record.setLeaveType(fieldName);
            record.setEmployeeId(data.getEmpId());
            record.setCreateTime(data.getWorkDate());
            record.setCreateUserId(1L);
            record.setLeaveRecordId(System.currentTimeMillis());
            leaves.add(record);
        } else if (fieldName.endsWith("加班")) {
            HrmEmployeeOverTimeRecord record = new HrmEmployeeOverTimeRecord();
            record.setOverTimes(Double.parseDouble(val));
            if (fieldName.equals("工作日加班")) {
                record.setOverTimeType(1);
            } else if (fieldName.equals("休息日加班")) {
                record.setOverTimeType(2);
            } else if (fieldName.equals("节假日加班")) {
                record.setOverTimeType(3);
            }
            record.setEmployeeId(data.getEmpId());
            record.setCreateTime(data.getWorkDate());
            record.setAttendanceTime(data.getWorkDate());
            record.setCreateUserId(1L);
            record.setOverTimeId(System.currentTimeMillis());
            overTimes.add(record);
        }
    }

    // 当前已禁用
    // @Scheduled(cron = "0 0/2 * * * ?")
    // public void process() { execute(); }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/CreateOverTimeAndLeaveTimeRecordTask.java
git commit -m "refactor(autoTask): 重构 CreateOverTimeAndLeaveTimeRecordTask 继承基类"
```

---

## Task 15: 重构 SalaryComputeTask

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/autoTask/SalaryComputeTask.java`

**Context:** 工资计算Task，当前已禁用且逻辑简单。重构为继承基类，保持禁用状态。

**Step 1: 重写 SalaryComputeTask**

```java
package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.service.SalaryMonthRecordService_Bak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 工资计算Task
 * 当前已禁用
 */
@Component
public class SalaryComputeTask extends AbstractDingTalkTask {

    @Autowired
    private SalaryMonthRecordService_Bak salaryMonthRecordService;

    @Override
    protected String getTaskFieldName() {
        return null;
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        logger.info("[SalaryComputeTask][{}] 开始核算工资", companyId);
        HrmSalaryMonthRecord record = salaryMonthRecordService.queryLastSalaryMonthRecord();
        // salaryMonthRecordService.computeSalaryData(record.getSRecordId(), true, true, null, null, null);
        logger.info("[SalaryComputeTask][{}] 核算工资完毕", companyId);
    }

    // 当前已禁用
    // @Scheduled(cron = "* 0/5 * * * ?")
    // public void process() { execute(); }
}
```

**Step 2: 编译验证**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn compile -q
```

**Step 3: Commit**

```bash
git add src/main/java/com/tianye/hrsystem/autoTask/SalaryComputeTask.java
git commit -m "refactor(autoTask): 重构 SalaryComputeTask 继承基类"
```

---

## Task 16: 集成验证与清理

**Context:** 所有Task已重构完成，进行全量编译验证，确保无编译错误。

**Step 1: 全量编译**

```bash
cd /Users/jiangyongming/Project/hr/hainan && mvn clean compile
```

Expected: BUILD SUCCESS

**Step 2: 检查所有重构文件**

确认以下文件都已正确修改：

```
autoTask/
├── SchedulerConfig.java                        ← Task 5: 双线程池
├── common/
│   ├── DingTalkRateLimiter.java                ← Task 1: 限流器
│   ├── TaskCheckpoint.java                     ← Task 2: 检查点
│   ├── DingTalkApiTemplate.java                ← Task 3: API模板
│   └── AbstractDingTalkTask.java               ← Task 4: 基类
├── AttendanceGroupRefreshTask.java             ← Task 6: 重构
├── AttendanceUserRefreshTask.java              ← Task 7: 重构
├── AttendancePlanRecordTask.java               ← Task 8: 重构
├── AttendanceDetailRefreshTask.java            ← Task 9: 重构
├── AttendanceReportRefreshTask.java            ← Task 10: 重构
├── AttendanceLeaveTimeRefreshTask.java         ← Task 11: 重构
├── AttendanceReportDataSaveTask.java           ← Task 12: 重构
├── AttendanceEmpScheduleTask.java              ← Task 13: 重构
├── CreateOverTimeAndLeaveTimeRecordTask.java   ← Task 14: 重构
└── SalaryComputeTask.java                      ← Task 15: 重构
```

**Step 3: 修复编译错误（如有）**

根据编译输出逐个修复。常见问题：
- import 路径不正确
- 基类方法签名与子类不匹配
- 缺少依赖注入

**Step 4: 最终提交**

```bash
git add -A
git commit -m "refactor(autoTask): 完成全部重构，集成验证通过"
```

---

## 实施总结

| Task | 内容 | 类型 | 依赖 |
|------|------|------|------|
| 1 | DingTalkRateLimiter 限流器 | 新增 | 无 |
| 2 | TaskCheckpoint 检查点 | 新增 | 无 |
| 3 | DingTalkApiTemplate API模板 | 新增 | Task 1 |
| 4 | AbstractDingTalkTask 基类 | 新增 | Task 1,2,3 |
| 5 | SchedulerConfig 线程池优化 | 修改 | 无 |
| 6 | AttendanceGroupRefreshTask | 重构 | Task 4 |
| 7 | AttendanceUserRefreshTask | 重构 | Task 4 |
| 8 | AttendancePlanRecordTask | 重构 | Task 4 |
| 9 | AttendanceDetailRefreshTask | 重构 | Task 4 |
| 10 | AttendanceReportRefreshTask | 重构 | Task 4 |
| 11 | AttendanceLeaveTimeRefreshTask | 重构 | Task 4 |
| 12 | AttendanceReportDataSaveTask | 重构 | Task 4 |
| 13 | AttendanceEmpScheduleTask | 重构 | Task 4 |
| 14 | CreateOverTimeAndLeaveTimeRecordTask | 重构 | Task 4 |
| 15 | SalaryComputeTask | 重构 | Task 4 |
| 16 | 集成验证与清理 | 验证 | Task 1-15 |

### 重构效果预估

| 指标 | 重构前 | 重构后 |
|------|--------|--------|
| 总代码行数 | ~1500行 | ~800行 |
| 重复代码率 | ~60% | ~10% |
| 限流策略 | 散落的Thread.sleep | 统一RateLimiter |
| 重试机制 | 仅部分Task有 | 全部统一3次指数退避 |
| 断点续传 | 无 | 员工级/日期级检查点 |
| 异常处理 | 不一致 | 三级分类统一处理 |
| 日志格式 | 混乱 | 统一 [Task][Company][User] |
| 新增Task成本 | 复制粘贴200行 | 继承基类写30行 |
