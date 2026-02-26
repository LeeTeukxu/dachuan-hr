package com.tianye.hrsystem.imple;

import cn.hutool.core.collection.ListUtil;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.ApplicationContextHolder;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.controller.HrmAttendanceDataController;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.*;
import com.tianye.hrsystem.service.ddTalk.IGroupManager;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import com.tianye.hrsystem.service.ddTalk.IUserManager;
import com.tianye.hrsystem.util.MyDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmAttendanceDataServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:16
 **/
@Service
public class HrmAttendanceDataServiceImpl implements IHrmAttendanceDataService {

    // Redis Key 前缀
    private static final String SYNC_PROGRESS_KEY = "attendance:sync:progress";
    private static final String SYNC_STEP_KEY = "attendance:sync:step";
    private static final String SYNC_PROCESSED_EMPS_KEY = "attendance:sync:processed_emps";
    private static final String SYNC_PARAMS_KEY = "attendance:sync:params";
    // 步骤7子步骤进度Key
    private static final String SYNC_STEP7A_PROCESSED_KEY = "attendance:sync:step7a_processed"; // 请假数据
    private static final String SYNC_STEP7B_PROCESSED_KEY = "attendance:sync:step7b_processed"; // 假期数据
    // 进度过期时间：24小时
    private static final int PROGRESS_EXPIRE_SECONDS = 86400;
    // 并行处理线程数（步骤3-4调用钉钉API，保守设置）
    private static final int PARALLEL_THREADS = 2;
    // 步骤7并行线程数（降低以避免钉钉限流，实测5线程失败率75%）
    private static final int STEP7_PARALLEL_THREADS = 2;
    // 每批次处理的员工数
    private static final int BATCH_SIZE = 1;
    // 步骤7每批次处理的员工数（降低批次大小以减少单请求处理时间）
    private static final int STEP7_BATCH_SIZE = 1;

    @Autowired
    IAttendancePlanService planService;
    @Autowired
    IAttendanceDetailService detailService;
    @Autowired
    ILeaveRecordDtaService leaveService;
    @Autowired
    IHolidayDataService holidayService;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    IHrmAttendanceReport report;
    Logger logger= LoggerFactory.getLogger(HrmAttendanceDataServiceImpl.class);
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    IUserManager userManager;
    @Autowired
    IGroupManager groupManager;
    @Autowired
    hrmAttendanceReportDataRepository dataRep;
    @Autowired
    hrmEmployeeRepository empRep;
    @Autowired
    Redis redis;

    /**
     * 获取当前同步进度信息
     * @return 进度信息Map，包含step, processedEmps, params等
     */
    public Map<String, Object> getSyncProgress() {
        Map<String, Object> progress = new HashMap<>();
        
        Integer step = redis.get(SYNC_STEP_KEY);
        String processedEmps = redis.get(SYNC_PROCESSED_EMPS_KEY);
        String params = redis.get(SYNC_PARAMS_KEY);
        
        progress.put("hasProgress", step != null && step > 0);
        progress.put("currentStep", step != null ? step : 0);
        progress.put("processedEmps", processedEmps != null ? processedEmps : "");
        progress.put("params", params != null ? params : "");
        
        return progress;
    }

    /**
     * 清除同步进度（同步成功或需要重新开始时调用）
     */
    public void clearSyncProgress() {
        redis.del(SYNC_STEP_KEY, SYNC_PROCESSED_EMPS_KEY, SYNC_PARAMS_KEY, 
                SYNC_STEP7A_PROCESSED_KEY, SYNC_STEP7B_PROCESSED_KEY);
        logger.info("已清除同步进度缓存");
    }

    /**
     * 保存同步进度
     */
    private void saveSyncProgress(int step, String processedEmps, String params) {
        redis.setex(SYNC_STEP_KEY, PROGRESS_EXPIRE_SECONDS, step);
        redis.setex(SYNC_PROCESSED_EMPS_KEY, PROGRESS_EXPIRE_SECONDS, processedEmps);
        redis.setex(SYNC_PARAMS_KEY, PROGRESS_EXPIRE_SECONDS, params);
    }

    /**
     * 同步考勤数据（主入口）
     * 自动根据Redis中的进度记录判断是否从断点恢复
     * 注意：不使用@Transactional，让每个步骤独立提交，支持断点续传
     */
    @Override
    public boolean SyncData(String EmpIDS, Date Begin, Date End) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentParams = EmpIDS + "|" + sdf.format(Begin) + "|" + sdf.format(End);
        
        // 根据Redis进度记录自动判断是否从断点恢复
        Map<String, Object> progress = getSyncProgress();
        boolean hasValidProgress = (Boolean) progress.get("hasProgress") 
                && currentParams.equals(progress.get("params"));
        
        if (hasValidProgress) {
            logger.info("检测到有效的断点进度，将从步骤{}恢复", progress.get("currentStep"));
        } else {
            logger.info("无有效断点进度，将从头开始同步");
        }
        
        return SyncDataWithResume(EmpIDS, Begin, End, hasValidProgress);
    }

    /**
     * 支持断点续传的同步方法
     * 注意：不使用@Transactional，让每个步骤独立提交，避免异常时回滚已处理的数据
     */
    public boolean SyncDataWithResume(String EmpIDS, Date Begin, Date End, boolean resumeFromBreakpoint) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentParams = EmpIDS + "|" + sdf.format(Begin) + "|" + sdf.format(End);
        
        List<String> allEmpIds = Arrays.asList(EmpIDS.split(","));
        List<String> remainingEmpIds = new ArrayList<>(allEmpIds);
        int startStep = 7; // TODO: 临时调试 - 强制从步骤7开始
        Set<String> processedEmpSet = new HashSet<>();
        
        // 全量员工相关变量，用于步骤6-7
        String allEmpIDS = EmpIDS;
        List<Long> allEmpIDD = allEmpIds.stream().map(Long::parseLong).collect(Collectors.toList());

        // 检查是否需要从断点恢复
        if (resumeFromBreakpoint) {
            Map<String, Object> progress = getSyncProgress();
            if ((Boolean) progress.get("hasProgress")) {
                String savedParams = (String) progress.get("params");
                // 验证参数是否匹配
                if (currentParams.equals(savedParams)) {
                    startStep = (Integer) progress.get("currentStep");
                    String processedEmps = (String) progress.get("processedEmps");
                    if (processedEmps != null && !processedEmps.isEmpty()) {
                        processedEmpSet = new HashSet<>(Arrays.asList(processedEmps.split(",")));
                        remainingEmpIds.removeAll(processedEmpSet);
                    }
                    logger.info("========== 从断点恢复同步，起始步骤: {}, 剩余员工数: {} ==========", startStep, remainingEmpIds.size());
                } else {
                    logger.warn("参数不匹配，将从头开始同步。缓存参数: {}, 当前参数: {}", savedParams, currentParams);
                    clearSyncProgress();
                }
            } else {
                logger.info("无可恢复的进度，将从头开始同步");
            }
        } else {
            // 非恢复模式，清除旧进度
            clearSyncProgress();
        }

        // 只有当员工级步骤（3-4）已完成且所有步骤都已完成时才跳过
        if (remainingEmpIds.isEmpty() && startStep > 7) {
            logger.info("所有步骤已完成，无需同步");
            clearSyncProgress();
            return true;
        }
        
        // 如果员工级步骤已完成但还有后续步骤（5-7），继续执行
        if (remainingEmpIds.isEmpty() && startStep <= 7) {
            logger.info("员工级步骤（3-4）已完成，继续执行步骤{}-7", startStep);
        }
                
        // 待处理员工，用于步骤3-4的断点续传
        String remainingEmpIDS = String.join(",", remainingEmpIds);
                
        logger.info("========== 开始同步考勤数据 (步骤{}/7起) ==========", startStep);
        logger.info("总员工数: {}，待处理员工数: {}", allEmpIds.size(), remainingEmpIds.size());

        try {
            // 步骤1: 同步组织架构
            if (startStep <= 1) {
                logger.info("[步骤1/7] 同步钉钉组织架构...");
                groupManager.GetAndSave();
                saveSyncProgress(2, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤2: 同步用户信息
            if (startStep <= 2) {
                logger.info("[步骤2/7] 同步钉钉用户信息...");
                userManager.GetAndSave();
                saveSyncProgress(3, String.join(",", processedEmpSet), currentParams);
            }

            List<tbattendanceuser> users = userRep.findAll();
            // 创建线程安全的users副本，避免并行处理时的状态共享问题
            final List<tbattendanceuser> threadSafeUsers = Collections.unmodifiableList(new ArrayList<>(users));

            // 步骤3: 同步考勤计划（并行处理，支持断点）
            if (startStep <= 3) {
                logger.info("[步骤3/7] 同步考勤计划（并行模式，线程数: {}）", PARALLEL_THREADS);
                // 每个员工的删除和保存在同一个短事务中完成，避免锁冲突
                syncByEmployeeBatchParallelSafe(remainingEmpIds, processedEmpSet, Begin, End, currentParams, 3,
                        threadSafeUsers, planService);
                saveSyncProgress(4, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤4: 同步考勤明细（并行处理，支持断点）
            if (startStep <= 4) {
                logger.info("[步骤4/7] 同步考勤明细（并行模式，线程数: {}）", PARALLEL_THREADS);
                // 重置已处理集合用于此步骤
                Set<String> step4Processed = ConcurrentHashMap.newKeySet();
                syncByEmployeeBatchParallelSafe(remainingEmpIds, step4Processed, Begin, End, currentParams, 4,
                        threadSafeUsers, detailService);
                saveSyncProgress(5, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤5: 更新考勤报表字段
            if (startStep <= 5) {
                logger.info("[步骤5/7] 更新考勤报表字段");
                report.UpdateReportFields();
                saveSyncProgress(6, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤6: 删除历史数据（只删除当前批次要处理的员工，保留已处理员工的数据）
            if (startStep <= 6) {
                logger.info("[步骤6/7] 删除历史数据，处理所有员工: {} 人", allEmpIDD.size());
                // 使用事务模板执行删除操作
                TransactionTemplate transactionTemplate = ApplicationContextHolder.getBean(TransactionTemplate.class);
                Integer Num = transactionTemplate.execute(status -> {
                    return dataRep.deleteAllByEmpIdInAndWorkDateBetween(allEmpIDD, Begin, End);
                });
                logger.info("[步骤6/7] 已删除 {} 条历史记录", Num);
                saveSyncProgress(7, String.join(",", processedEmpSet), currentParams);
            }

            // 步骤7: 同步请假和假期数据（并行处理，支持断点续传）
            if (startStep <= 7) {
                logger.info("[步骤7/7] 同步请假和假期数据（并行模式，处理所有员工: {}，线程数: {}）", allEmpIds.size(), STEP7_PARALLEL_THREADS);
                
                // 步骤7a: 请假数据同步（支持断点续传）
                Set<String> step7aProcessed = ConcurrentHashMap.newKeySet();
                if (resumeFromBreakpoint) {
                    String saved7a = redis.get(SYNC_STEP7A_PROCESSED_KEY);
                    if (saved7a != null && !saved7a.isEmpty()) {
                        step7aProcessed.addAll(Arrays.asList(saved7a.split(",")));
                        logger.info("[步骤7a/7] 从断点恢复，已处理: {} 人", step7aProcessed.size());
                    }
                }
                logger.info("[步顂7a/7] 开始同步请假数据");
                try {
                    syncByEmployeeBatchParallelSafe(allEmpIds, step7aProcessed, Begin, End, 
                            currentParams, 7, threadSafeUsers, leaveService);
                    logger.info("[步顂7a/7] 请假数据同步完成");
                } catch (Exception e) {
                    logger.error("[步顂7a/7] 请假数据同步失败，已保存进度，可从断点恢复", e);
                    throw e; // 重新抛出异常，不执行后续步骤
                }
                            
                // 步骤7b: 假期数据同步（支持断点续传）
                Set<String> step7bProcessed = ConcurrentHashMap.newKeySet();
                if (resumeFromBreakpoint) {
                    String saved7b = redis.get(SYNC_STEP7B_PROCESSED_KEY);
                    if (saved7b != null && !saved7b.isEmpty()) {
                        step7bProcessed.addAll(Arrays.asList(saved7b.split(",")));
                        logger.info("[步骤7b/7] 从断点恢复，已处理: {} 人", step7bProcessed.size());
                    }
                }
                logger.info("[步顂7b/7] 开始同步假期数据");
                try {
                    syncByEmployeeBatchParallelSafe(allEmpIds, step7bProcessed, Begin, End, 
                            currentParams, 7, threadSafeUsers, holidayService);
                    logger.info("[步顂7b/7] 假期数据同步完成");
                } catch (Exception e) {
                    logger.error("[步顂7b/7] 假期数据同步失败，已保存进度，可从断点恢复", e);
                    throw e; // 重新抛出异常
                }
            }

            // 同步成功，清除进度缓存
            clearSyncProgress();
            logger.info("========== 考勤数据同步完成 ==========");
            return true;

        } catch (Exception e) {
            logger.error("同步过程中发生异常，进度已保存，可调用 SyncDataWithResume(..., true) 从断点恢复", e);
            throw e;
        }
    }

    /**
     * 按员工批量同步（串行版本，保留作为备用）
     */
    private void syncByEmployeeBatch(List<String> empIds, Set<String> processedSet, 
                                      Date begin, Date end, String params, int currentStep,
                                      EmployeeSyncAction action) throws Exception {
        int total = empIds.size();
        int processed = 0;
        
        for (String empId : empIds) {
            if (processedSet.contains(empId)) {
                processed++;
                continue;
            }
            
            try {
                action.sync(empId);
                processedSet.add(empId);
                processed++;
                
                // 每处理10个员工保存一次进度
                if (processed % 10 == 0) {
                    saveSyncProgress(currentStep, String.join(",", processedSet), params);
                    logger.info("[步骤{}/7] 进度: {}/{}", currentStep, processed, total);
                }
            } catch (Exception e) {
                // 发生异常时保存当前进度
                saveSyncProgress(currentStep, String.join(",", processedSet), params);
                logger.error("处理员工 {} 时发生异常，已保存进度，可从断点恢复", empId);
                throw e;
            }
        }
    }

    /**
     * 按员工并行同步（线程安全版本）
     * 使用同步块保证 setUsers 和 Sync 的原子性
     */
    private void syncByEmployeeBatchParallelSafe(List<String> empIds, Set<String> processedSet,
                                                  Date begin, Date end, String params, int currentStep,
                                                  List<tbattendanceuser> users, Object service) throws Exception {
        // 过滤掉已处理的员工
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());
        
        if (toProcess.isEmpty()) {
            logger.info("[步骤{}/7] 所有员工已处理完成", currentStep);
            return;
        }

        int total = toProcess.size();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedEmpIds = Collections.synchronizedList(new ArrayList<>());

        // 使用线程安全的Set
        Set<String> threadSafeProcessedSet = ConcurrentHashMap.newKeySet();
        threadSafeProcessedSet.addAll(processedSet);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        // 分批提交任务
        List<List<String>> batches = partitionList(toProcess, BATCH_SIZE);

        logger.info("[步骤{}/7] 开始并行处理，总员工数: {}，分 {} 批，每批 {} 人", 
                currentStep, total, batches.size(), BATCH_SIZE);

        long startTime = System.currentTimeMillis();

        // 保存主线程的 CompanyContext，供子线程使用
        final LoginUserInfo mainThreadContext = CompanyContext.get();
        if (mainThreadContext == null) {
            logger.error("主线程 CompanyContext 为 null，无法进行并行处理");
            throw new Exception("缺少租户上下文（CompanyContext），请确保通过正常 API 调用");
        }
        logger.info("[步骤{}/7] 主线程上下文: companyId={}", currentStep, mainThreadContext.getCompanyId());

        for (List<String> batch : batches) {
            Future<?> future = executor.submit(() -> {
                // 在子线程中恢复 CompanyContext
                CompanyContext.set(mainThreadContext);
                try {
                    for (String empId : batch) {
                        try {
                            // 移除同步锁，实现真正的并行处理
                            if (service instanceof IAttendancePlanService) {
                                IAttendancePlanService planSvc = (IAttendancePlanService) service;
                                planSvc.setUsers(users);
                                planSvc.Sync(empId, begin, end);
                            } else if (service instanceof IAttendanceDetailService) {
                                IAttendanceDetailService detailSvc = (IAttendanceDetailService) service;
                                detailSvc.setUsers(users);
                                detailSvc.Sync(empId, begin, end);
                            }
                            
                            threadSafeProcessedSet.add(empId);
                            int count = processed.incrementAndGet();
                            
                            // 每处理20个员工保存一次进度并打印日志
                            if (count % 20 == 0) {
                                synchronized (this) {
                                    saveSyncProgress(currentStep, String.join(",", threadSafeProcessedSet), params);
                                }
                                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                                double speed = count / (double) Math.max(elapsed, 1);
                                int remaining = total - count;
                                long eta = (long) (remaining / Math.max(speed, 0.1));
                                logger.info("[步骤{}/7] 进度: {}/{} ({}员工/秒)，预计剩余: {}秒", 
                                        currentStep, count, total, String.format("%.1f", speed), eta);
                            }
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            failedEmpIds.add(empId);
                            logger.error("处理员工 {} 失败，异常类型: {}，错误信息: {}", empId, e.getClass().getName(), e.getMessage());
                            logger.error("员工 {} 失败详细堆栈:", empId, e);
                        }
                    }
                } finally {
                    // 清理子线程的 CompanyContext，避免内存泄漏
                    CompanyContext.clear();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                logger.error("批次处理超时");
            } catch (Exception e) {
                logger.error("批次处理异常: {}", e.getMessage());
            }
        }

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 合并结果到原始集合
        processedSet.addAll(threadSafeProcessedSet);

        // 保存最终进度
        saveSyncProgress(currentStep, String.join(",", processedSet), params);

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("[步骤{}/7] 完成！成功: {}，失败: {}，总耗时: {}秒", 
                currentStep, processed.get(), failed.get(), totalTime);

        // 如果有失败，抛出异常以便断点续传
        if (failed.get() > 0) {
            logger.error("步骤{}有{}个员工处理失败，失败员工ID: {}，已保存进度，可重试", 
                    currentStep, failed.get(), String.join(",", failedEmpIds));
            throw new Exception(String.format("步骤%d有%d个员工处理失败，已保存进度，可重试", currentStep, failed.get()));
        }
    }

    /**
     * 按员工并行同步（多线程版本，大幅提升性能）
     * 使用线程池并行处理多个员工，同时保持断点续传能力
     */
    private void syncByEmployeeBatchParallel(List<String> empIds, Set<String> processedSet,
                                              Date begin, Date end, String params, int currentStep,
                                              EmployeeSyncAction action) throws Exception {
        // 过滤掉已处理的员工
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());
        
        if (toProcess.isEmpty()) {
            logger.info("[步骤{}/7] 所有员工已处理完成", currentStep);
            return;
        }

        int total = toProcess.size();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> failedEmpIds = Collections.synchronizedList(new ArrayList<>());

        // 使用线程安全的Set
        Set<String> threadSafeProcessedSet = ConcurrentHashMap.newKeySet();
        threadSafeProcessedSet.addAll(processedSet);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        // 分批提交任务
        List<List<String>> batches = partitionList(toProcess, BATCH_SIZE);

        logger.info("[步骤{}/7] 开始并行处理，总员工数: {}，分 {} 批，每批 {} 人", 
                currentStep, total, batches.size(), BATCH_SIZE);

        long startTime = System.currentTimeMillis();

        // 保存主线程的 CompanyContext，供子线程使用
        final LoginUserInfo mainThreadContext = CompanyContext.get();
        if (mainThreadContext == null) {
            logger.error("主线程 CompanyContext 为 null，无法进行并行处理");
            throw new Exception("缺少租户上下文（CompanyContext），请确保通过正常 API 调用");
        }

        for (List<String> batch : batches) {
            Future<?> future = executor.submit(() -> {
                // 在子线程中恢复 CompanyContext
                CompanyContext.set(mainThreadContext);
                try {
                    for (String empId : batch) {
                        try {
                            action.sync(empId);
                            threadSafeProcessedSet.add(empId);
                            int count = processed.incrementAndGet();
                            
                            // 每处理20个员工保存一次进度并打印日志
                            if (count % 20 == 0) {
                                synchronized (this) {
                                    saveSyncProgress(currentStep, String.join(",", threadSafeProcessedSet), params);
                                }
                                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                                double speed = count / (double) Math.max(elapsed, 1);
                                int remaining = total - count;
                                long eta = (long) (remaining / Math.max(speed, 0.1));
                                logger.info("[步骤{}/7] 进度: {}/{} ({}员工/秒)，预计剩余: {}秒", 
                                        currentStep, count, total, String.format("%.1f", speed), eta);
                            }
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            failedEmpIds.add(empId);
                            logger.error("处理员工 {} 失败: {}", empId, e.getMessage());
                            // 记录失败但继续处理其他员工
                        }
                    }
                } finally {
                    // 清理子线程的 CompanyContext，避免内存泄漏
                    CompanyContext.clear();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES); // 每批最多等待30分钟
            } catch (TimeoutException e) {
                logger.error("批次处理超时");
            } catch (Exception e) {
                logger.error("批次处理异常: {}", e.getMessage());
            }
        }

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 合并结果到原始集合
        processedSet.addAll(threadSafeProcessedSet);

        // 保存最终进度
        saveSyncProgress(currentStep, String.join(",", processedSet), params);

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("[步骤{}/7] 完成！成功: {}，失败: {}，总耗时: {}秒", 
                currentStep, processed.get(), failed.get(), totalTime);

        // 如果有失败，抛出异常以便断点续传
        if (failed.get() > 0) {
            logger.error("步骤{}有{}个员工处理失败，失败员工ID: {}，已保存进度，可重试", 
                    currentStep, failed.get(), String.join(",", failedEmpIds));
            throw new Exception(String.format("步骤%d有%d个员工处理失败，已保存进度，可重试", currentStep, failed.get()));
        }
    }
    
    /**
     * 将列表分割成指定大小的批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
    
    /**
     * 步骤7批次并行同步（支持断点续传）
     * 将员工分成多个批次，每个批次在独立线程中调用服务的Sync方法
     * @param processedSet 已处理的员工ID集合，用于断点续传
     * @param progressKey Redis进度Key，用于保存此子步骤的进度
     */
    private void syncStep7BatchParallelWithResume(List<String> empIds, Set<String> processedSet,
                                                    Date begin, Date end, String params,
                                                    List<tbattendanceuser> users, Object service, 
                                                    String serviceName, String progressKey) throws Exception {
        // 过滤已处理的员工
        List<String> toProcess = empIds.stream()
                .filter(id -> !processedSet.contains(id))
                .collect(Collectors.toList());
        
        if (toProcess.isEmpty()) {
            logger.info("[步骤7/7] {}数据所有员工已处理完成", serviceName);
            return;
        }

        int total = toProcess.size();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        List<String> failedEmpIds = Collections.synchronizedList(new ArrayList<>());
        
        // 线程安全的已处理集合
        Set<String> threadSafeProcessedSet = ConcurrentHashMap.newKeySet();
        threadSafeProcessedSet.addAll(processedSet);

        ExecutorService executor = Executors.newFixedThreadPool(STEP7_PARALLEL_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        List<List<String>> batches = partitionList(toProcess, STEP7_BATCH_SIZE);

        logger.info("[步骤7/7] {}数据开始批次并行处理，待处理: {}人，分 {} 批，每批 {} 人，线程数: {}", 
                serviceName, total, batches.size(), STEP7_BATCH_SIZE, STEP7_PARALLEL_THREADS);

        long startTime = System.currentTimeMillis();

        final LoginUserInfo mainThreadContext = CompanyContext.get();
        if (mainThreadContext == null) {
            logger.error("主线程 CompanyContext 为 null，无法进行并行处理");
            throw new Exception("缺少租户上下文（CompanyContext）");
        }

        for (List<String> batch : batches) {
            final String batchEmpIds = String.join(",", batch);
            
            Future<?> future = executor.submit(() -> {
                CompanyContext.set(mainThreadContext);
                try {
                    // 调用服务的Sync方法
                    if (service instanceof ILeaveRecordDtaService) {
                        ILeaveRecordDtaService svc = (ILeaveRecordDtaService) service;
                        svc.setUsers(users);
                        svc.Sync(batchEmpIds, begin, end);
                    } else if (service instanceof IHolidayDataService) {
                        IHolidayDataService svc = (IHolidayDataService) service;
                        svc.setUsers(users);
                        svc.Sync(batchEmpIds, begin, end);
                    }
                    
                    // 批次成功，记录已处理的员工
                    threadSafeProcessedSet.addAll(batch);
                    int count = processedCount.addAndGet(batch.size());
                    
                    // 每处理20人或每5批保存一次进度并打印日志
                    if (count % 20 == 0 || count == total) {
                        synchronized (this) {
                            redis.setex(progressKey, PROGRESS_EXPIRE_SECONDS, String.join(",", threadSafeProcessedSet));
                        }
                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                        double speed = count / (double) Math.max(elapsed, 1);
                        int remaining = total - count;
                        long eta = (long) (remaining / Math.max(speed, 0.1));
                        logger.info("[步骤7/7] {}进度: {}/{} ({}%)，{}员工/秒，预计剩余: {}秒", 
                                serviceName, count, total, String.format("%.1f", count * 100.0 / total),
                                String.format("%.1f", speed), eta);
                    }
                } catch (Exception e) {
                    failedCount.addAndGet(batch.size());
                    failedEmpIds.addAll(batch);
                    logger.error("[步骤7/7] {}批次处理失败，员工数: {}，异常: {}", 
                            serviceName, batch.size(), e.getMessage(), e);
                } finally {
                    CompanyContext.clear();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                logger.error("[步骤7/7] {}批次处理超时", serviceName);
            } catch (Exception e) {
                logger.error("[步骤7/7] {}批次处理异常: {}", serviceName, e.getMessage());
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 合并结果到原始集合
        processedSet.addAll(threadSafeProcessedSet);

        // 保存最终进度
        redis.setex(progressKey, PROGRESS_EXPIRE_SECONDS, String.join(",", processedSet));

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("[步骤7/7] {}完成！成功: {}，失败: {}，总耗时: {}秒", 
                serviceName, processedCount.get(), failedCount.get(), totalTime);

        // 如果有失败，抛出异常以便断点续传
        if (failedCount.get() > 0) {
            logger.error("步骤7{}有{}个员工处理失败，失败员工ID: {}，已保存进度，可重试", 
                    serviceName, failedCount.get(), String.join(",", failedEmpIds));
            throw new Exception(String.format("步骤7%s有%d个员工处理失败，已保存进度，可重试", serviceName, failedCount.get()));
        }
    }

    /**
     * 员工同步动作接口
     */
    @FunctionalInterface
    private interface EmployeeSyncAction {
        void sync(String empId) throws Exception;
    }
}
