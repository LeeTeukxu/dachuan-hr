# AutoTask 模块重构设计方案

## 概述

对 `autoTask` 文件夹中的钉钉数据同步代码进行全面优化，目标：
- **高可读性**：抽取公共基类，每个Task只关注业务逻辑
- **健壮性**：统一限流、重试、断点续传、异常分级
- **简洁性**：消除重复代码，预计减少40-50%代码量

### 项目背景

- 规模：3-10家公司，每家100-500人
- 技术栈：Spring Boot + JPA/Hibernate + MyBatis-Plus + Redis + 钉钉旧版SDK
- 多租户：Hibernate DATABASE策略，ThreadLocal切换

---

## 一、统一限流器 DingTalkRateLimiter

### 问题
各处散落 `Thread.sleep(10~1000ms)`，无统一管理，无法根据钉钉实际限流规则动态调整。

### 方案
基于 Guava `RateLimiter` 的令牌桶限流器：

```java
@Component
public class DingTalkRateLimiter {
    // 钉钉限制：每个应用约20次/秒（可通过配置调整）
    private final Map<String, RateLimiter> companyLimiters = new ConcurrentHashMap<>();

    @Value("${dingtalk.rate-limit.permits-per-second:15}")
    private double permitsPerSecond;  // 留余量，设15

    public void acquire(String companyId);

    public <T> T executeWithRetry(Supplier<T> apiCall, int maxRetries, String apiName);
}
```

- 每个公司独立限流（按 companyId 隔离）
- 统一重试：3次 + 指数退避（500ms → 1000ms → 2000ms）
- 遇到钉钉限流错误码时自动降速

---

## 二、Task公共基类 AbstractDingTalkTask

### 问题
每个Task重复写：EachCompany → CompanyContext → 幂等检查 → try/catch → 进度更新，约80%代码是模板代码。

### 方案

```java
public abstract class AbstractDingTalkTask {

    // ===== 子类必须实现 =====
    protected abstract String getTaskFieldName();     // 进度字段名
    protected abstract String getCron();              // cron表达式

    // 子类实现其一：
    protected void doProcess(String companyId, Date date) {}           // 非员工维度
    protected void processOneEmployee(String companyId, String userId, Date date) {} // 员工维度

    // ===== 基类统一提供 =====

    // 1. 任务执行入口（模板方法）
    protected void execute() {
        accessToken.EachCompany(companyId -> {
            setupCompanyContext(companyId);
            try {
                Date current = getCurrentDate();
                if (isAlreadyDone(companyId, current)) return;
                doProcess(companyId, current);
                markTaskDone(companyId, current);
                updateProgress(companyId, current);
            } catch (FatalException e) {
                logFail(null, "任务执行", e);
                exceptionUtils.addOne(e, getClass().getName());
                // 终止当前公司，继续下一家
            } catch (Exception e) {
                logFail(null, "任务执行", e);
                exceptionUtils.addOne(e, getClass().getName());
            } finally {
                CompanyContext.set(null);
            }
        });
    }

    // 2. 员工批量处理（含断点续传 + 补偿重试）
    protected void executeForAllEmployees(String companyId, Date date) {
        List<String> userIds = getAllUserIds(companyId);
        List<String> failedIds = new ArrayList<>();
        int successCount = 0;

        // 第一轮：遍历所有员工
        for (String userId : userIds) {
            if (isCheckpointDone(companyId, date, userId)) {
                successCount++;
                continue;  // 断点续传：已成功的跳过
            }
            try {
                rateLimiter.acquire(companyId);
                processOneEmployee(companyId, userId, date);
                markCheckpointDone(companyId, date, userId);
                successCount++;
            } catch (Exception e) {
                boolean retryOk = retryWithBackoff(
                    () -> { processOneEmployee(companyId, userId, date); return null; }, 3);
                if (retryOk) {
                    markCheckpointDone(companyId, date, userId);
                    successCount++;
                } else {
                    failedIds.add(userId);
                    markCheckpointFailed(companyId, date, userId);
                    exceptionUtils.addOne(e, getClass().getName());
                }
            }
        }

        // 第二轮：补偿重试 FAILED 的
        if (!failedIds.isEmpty()) {
            Thread.sleep(5000);  // 等待网络恢复
            for (String userId : failedIds) {
                boolean retryOk = retryWithBackoff(
                    () -> { processOneEmployee(companyId, userId, date); return null; }, 3);
                if (retryOk) {
                    markCheckpointDone(companyId, date, userId);
                    successCount++;
                }
            }
        }

        logSummary(successCount, failedIds.size(), 0);
    }

    // 3. 限流
    @Autowired
    protected DingTalkRateLimiter rateLimiter;

    // 4. 重试（指数退避）
    protected <T> T retryWithBackoff(Supplier<T> action, int maxRetries);

    // 5. 异常分级
    protected boolean isRetryable(Exception e);  // 网络超时、限流
    protected boolean isFatal(Exception e);       // Token失效、DB不可用

    // 6. 统一日志
    protected void logSuccess(String userId, String action, String detail);
    protected void logRetry(String userId, String action, int attempt);
    protected void logFail(String userId, String action, Exception e);
    protected void logSummary(int success, int failed, int skipped);

    // 7. 检查点（断点续传）
    protected boolean isCheckpointDone(String companyId, Date date, String subKey);
    protected void markCheckpointDone(String companyId, Date date, String subKey);
    protected void markCheckpointFailed(String companyId, Date date, String subKey);

    // 8. 数据库持久化
    protected <T> void batchSave(List<T> data, int batchSize, Consumer<List<T>> saveAction);
    protected <T> void replaceAll(Runnable deleteAction, List<T> data, Consumer<List<T>> saveAction);

    // 9. 日期工具
    protected Date getCurrentDate();
    protected Date getMonthBegin(Date date);
    protected Date getMonthEnd(Date date);
    protected List<Date[]> splitDateRange(Date begin, Date end, int days);
}
```

---

## 三、线程池策略

### 问题
40线程调度池，但大部分Task内部串行执行，资源浪费。

### 方案：双线程池

```java
@Configuration
public class TaskThreadPoolConfig {

    // 调度池：仅负责触发定时任务
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("task-scheduler-");
        return scheduler;
    }

    // 业务池：数据库批量写入专用
    @Bean("dbWriteExecutor")
    public ThreadPoolTaskExecutor dbWriteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("db-write-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        return executor;
    }
}
```

### 线程使用原则

| 场景 | 线程策略 | 原因 |
|------|---------|------|
| 钉钉API调用 | 串行 + 限流器 | 受限流约束，并行触发限流 |
| 数据库批量写入 | 并行（dbWriteExecutor） | I/O密集，多线程提升吞吐 |
| 多公司遍历 | 串行 | 中等规模下串行更稳定 |

### API调用与DB写入配合

```
钉钉API调用（串行，受RateLimiter控制）
    ↓ 数据收集到内存List
    ↓ 达到阈值或全部收集完毕
数据库写入（并行，dbWriteExecutor）
    ↓ 分批提交，每批独立事务
    ↓ 写入完成后继续下一轮API调用
```

---

## 四、断点续传策略

### 问题
网络波动、停电等情况下，Task中断后要么跳过（幂等标记已存在），要么从头开始（浪费已完成的工作）。

### 方案：员工级检查点

复用现有 `updateRecord` 表，细化检查点粒度：

```
mainKey: {companyId}::{TaskClassName}
subKey:  {date}::{userId}
value:   SUCCESS 或 FAILED
```

### 同一Task内中断恢复

```
Task3 执行中：
  赵六 ✅ → 张三 ❌网络断 → 服务宕机

Task3 重新触发：
  赵六 → 检查点 SUCCESS → 跳过
  张三 → 检查点 FAILED → 重新处理
  李四 → 无记录 → 正常处理
  王五 → 无记录 → 正常处理
```

### 不同Task之间

```
Task3 结束：张三 FAILED
Task4 开始：所有员工从头处理（包括赵六、张三、李四、王五）
```

原因：不同Task处理不同类型的数据（排班 vs 打卡 vs 报表），彼此独立，不能跳过。

### 两轮重试机制

```
第一轮：正常遍历
  每个员工失败 → 立即重试3次（指数退避 500ms→1000ms→2000ms）
  仍失败 → 标记 FAILED，继续下一个员工（不阻塞）

第二轮：补偿重试（第一轮结束后）
  等待5秒（网络恢复窗口）
  只处理 FAILED 的员工 → 再重试3次
  仍失败 → 保持 FAILED，等第二天自动补
```

### 不做的事情（YAGNI）
- ❌ 不做员工内部字段级断点——粒度太细
- ❌ 不做自动恢复重试调度——现有cron已覆盖
- ❌ 不做断点过期清理——复用 updateRecord 生命周期

---

## 五、异常处理与日志统一

### 异常三级分类

| 级别 | 类型 | 处理方式 |
|------|------|---------|
| Level 1 | 可重试（网络超时、限流） | 自动重试3次 + 指数退避 |
| Level 2 | 可跳过（单员工数据异常） | 记录tbexception + 标记FAILED + 继续下一个 |
| Level 3 | 致命（Token失效、DB断开） | 记录tbexception + 终止当前公司 + 继续下一家 |

### 异常识别

```java
// Level 1：可重试
SocketTimeoutException, ConnectException, 钉钉限流错误码

// Level 3：致命
钉钉Token无效错误码, DataAccessResourceFailureException
```

### 统一日志格式

```
[TaskName][CompanyId][UserId] 操作描述 - 结果
```

示例：
```
[PlanRecordTask][company3][张三] 拉取排班数据 - 成功，共15条
[PlanRecordTask][company3][李四] 拉取排班数据 - 失败，重试第2次
[PlanRecordTask][company3] 本轮完成：成功98人，失败2人，跳过0人
```

---

## 六、钉钉API调用模板 DingTalkApiTemplate

### 问题
每次调用钉钉API重复：创建请求 → 设置Token → 执行 → 检查errcode → 日志记录。

### 方案

```java
@Component
public class DingTalkApiTemplate {

    public <REQ extends TaobaoRequest<RSP>, RSP extends TaobaoResponse>
        RSP execute(String companyId, REQ request, String apiName) {
        // 1. 获取Token（自动缓存）
        // 2. 限流等待
        // 3. 执行请求
        // 4. 检查 errcode
        // 5. 记录到 postresultlog
        // 6. 失败自动重试
        // 7. 返回响应
    }

    public <T> List<T> fetchAllPages(String companyId,
                                      Function<Long, PageResponse<T>> pageFetcher) {
        // 自动翻页 + 限流控制 + 收集所有结果
    }
}
```

---

## 七、数据库持久化模式统一

### 从5种模式统一为3种

| 模式 | 场景 | 实现 |
|------|------|------|
| batchSave | 大多数场景 | 自动分批（500条/批），每批独立事务，失败单批重试 |
| replaceAll | 全量同步（考勤组等） | 单事务内 delete + save，替代全局synchronized锁 |
| 异步队列 | 报表等大量数据 | 保留Ddtaskresult队列，优化消费端用业务线程池并行处理 |

---

## 八、重构后文件结构

```
autoTask/
├── config/
│   ├── SchedulerConfig.java              ← 优化：调度池5线程
│   └── TaskThreadPoolConfig.java         ← 新增：业务线程池
├── common/
│   ├── AbstractDingTalkTask.java         ← 新增：Task基类
│   ├── DingTalkApiTemplate.java          ← 新增：API调用模板
│   ├── DingTalkRateLimiter.java          ← 新增：统一限流器
│   └── TaskCheckpoint.java              ← 新增：检查点管理
├── task/
│   ├── AttendanceGroupRefreshTask.java        ← 重构
│   ├── AttendanceUserRefreshTask.java         ← 重构
│   ├── AttendancePlanRecordTask.java          ← 重构
│   ├── AttendanceDetailRefreshTask.java       ← 重构
│   ├── AttendanceReportRefreshTask.java       ← 重构
│   ├── AttendanceLeaveTimeRefreshTask.java    ← 重构
│   ├── AttendanceReportDataSaveTask.java      ← 重构
│   ├── AttendanceEmpScheduleTask.java         ← 重构
│   ├── CreateOverTimeAndLeaveTimeRecordTask.java ← 重构
│   └── SalaryComputeTask.java                 ← 重构
```

---

## 九、实施顺序

| 阶段 | 任务 | 依赖 |
|------|------|------|
| P1 | DingTalkRateLimiter 限流器 | 无 |
| P2 | DingTalkApiTemplate API模板 | P1 |
| P3 | TaskCheckpoint 检查点管理 | 无 |
| P4 | AbstractDingTalkTask 基类 | P1, P2, P3 |
| P5 | TaskThreadPoolConfig 线程池 | 无 |
| P6 | 重构 AttendanceGroupRefreshTask | P4 |
| P7 | 重构 AttendanceUserRefreshTask | P4 |
| P8 | 重构 AttendancePlanRecordTask | P4 |
| P9 | 重构 AttendanceDetailRefreshTask | P4 |
| P10 | 重构 AttendanceReportRefreshTask | P4 |
| P11 | 重构 AttendanceLeaveTimeRefreshTask | P4 |
| P12 | 重构 AttendanceReportDataSaveTask | P4, P5 |
| P13 | 重构 AttendanceEmpScheduleTask | P4 |
| P14 | 重构 CreateOverTimeAndLeaveTimeRecordTask | P4 |
| P15 | 重构 SalaryComputeTask | P4 |
| P16 | 优化 SchedulerConfig | P5 |
| P17 | 集成测试 + 清理旧代码 | P6-P16 |
