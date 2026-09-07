package com.tianye.hrsystem.modules.insurance.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.AdminMessageEnum;
import com.tianye.hrsystem.config.ApplicationContextHolder;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.enums.IsEnum;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsurancePageListBO;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceSalaryBasicAmountBO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpProjectRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceMonthRecordMapper;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceSechemeMapper;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceComputeProgressVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsuranceRecordListVO;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.service.IHrmSalaryConfigService;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryBasicService;
import com.tianye.hrsystem.modules.salary.support.HrmSalaryBasicDefaults;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.salary.vo.AdminMessageVO;
import com.tianye.hrsystem.service.IAdminMessageService;
import com.tianye.hrsystem.util.TransferUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class HrmInsuranceMonthRecordService extends BaseServiceImpl<HrmInsuranceMonthRecordMapper, HrmInsuranceMonthRecord> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HrmInsuranceMonthRecordService.class);
    private static final ConcurrentHashMap<String, InsuranceComputeProgressState> INSURANCE_COMPUTE_PROGRESS_MAP = new ConcurrentHashMap<>();
    // 按公司互斥：生成社保报表含“结转上月/删除旧明细”等破坏性步骤，并发双击会生成双月报、重复明细
    private static final ConcurrentHashMap<String, ReentrantLock> INSURANCE_COMPUTE_LOCK_MAP = new ConcurrentHashMap<>();
    private static final long INSURANCE_COMPUTE_PROGRESS_TTL_MS = 60 * 60 * 1000L;
    private static final String COMPUTE_STATUS_IDLE = "IDLE";
    private static final String COMPUTE_STATUS_RUNNING = "RUNNING";
    private static final String COMPUTE_STATUS_SUCCESS = "SUCCESS";
    private static final String COMPUTE_STATUS_FAILED = "FAILED";

    private static class InsuranceComputeProgressState {
        // 生成线程写、HTTP 轮询线程读，volatile 保证可见性
        volatile int progress;
        volatile String status;
        volatile String stage;
        volatile String message;
        volatile int processedCount;
        volatile int totalCount;
        volatile long updateTime;
    }

    @Autowired
    private IHrmSalaryConfigService salaryConfigService;

    @Autowired
    private HrmSalaryBasicService salaryBasicService;

    @Autowired
    private HrmInsuranceMonthEmpRecordService monthEmpRecordService;

    @Autowired
    private HrmInsuranceMonthEmpProjectRecordService monthEmpProjectRecordService;

    @Autowired
    private HrmInsuranceMonthRecordMapper insuranceMonthRecordMapper;

    @Autowired
    private HrmInsuranceSechemeMapper insuranceSchemeMapper;

    @Autowired
    private HrmInsuranceProjectService insuranceProjectService;

    @Autowired
    private IAdminMessageService adminMessageService;

    public JSONObject computeInsuranceData(Integer reqYear, Integer reqMonth) throws Exception {
        LoginUserInfo lockOwner = CompanyContext.get();
        String lockCompanyId = lockOwner != null && lockOwner.getCompanyId() != null ? lockOwner.getCompanyId() : "unknown";
        ReentrantLock computeLock = INSURANCE_COMPUTE_LOCK_MAP.computeIfAbsent(lockCompanyId, key -> new ReentrantLock());
        if (!computeLock.tryLock()) {
            logger.warn("拒绝重复生成社保报表：本公司已有生成任务在运行");
            throw new Exception("社保报表正在生成中，请勿重复点击，可稍后刷新查看进度");
        }
        int processedCount = 0;
        int totalCount = 0;
        try {
            clearExpiredComputeProgress();
            updateComputeProgress(1, COMPUTE_STATUS_RUNNING, "PREPARE", "正在准备生成社保报表", 0, 0);
            
            // 写入通知：社保计算开始
            try {
                AdminMessage startMsg = new AdminMessage();
                startMsg.setTitle("社保报表生成中");
                startMsg.setContent("点击查看");
                startMsg.setLabel(8);
                startMsg.setType(205); // HRM_INSURANCE_COMPUTE_RUNNING
                startMsg.setLinkUrl("/hrm/insurance-scheme");
                startMsg.setCreateUser(0L);
                startMsg.setRecipientUser(0L);
                startMsg.setCreateTime(LocalDateTime.now());
                startMsg.setIsRead(0);
                adminMessageService.save(startMsg);
                logger.info("[社保报表生成] 已写入开始通知");
            } catch (Exception e) {
                logger.error("[社保报表生成] 写入开始通知失败", e);
            }
            
            LoginUserInfo info = CompanyContext.get();
            HrmSalaryConfig salaryConfig = salaryConfigService.getOne(Wrappers.emptyWrapper());
            if (salaryConfig == null) {
                throw new Exception("尚未初始化计薪设置，请先到【系统设置-计薪设置】完成配置后再生成社保报表");
            }
            updateComputeProgress(8, COMPUTE_STATUS_RUNNING, "PREPARE", "已读取计薪设置", 0, 0);
            // 社保开始月是社保报表的首期锚点；历史数据存在 2020.05 / 2026-09-01 等多种写法，解析需容错
            int[] startYearMonth = parseYearMonth(salaryConfig.getSocialSecurityStartMonth(), "社保开始月");
            // 上一期按 (年*12+月) 取最大，补录历史月份时不会因创建时间倒挂而取错
            HrmInsuranceMonthRecord lastMonthRecord = queryLatestMonthRecord();
            int year;
            int month;
            if (reqYear != null && reqMonth != null) {
                // 前端在弹窗里手动指定了目标月份
                year = reqYear;
                month = reqMonth;
            } else if (lastMonthRecord == null) {
                year = startYearMonth[0];
                month = startYearMonth[1];
            } else {
                int[] nextYearMonth = nextMonth(lastMonthRecord.getYear(), lastMonthRecord.getMonth());
                // 社保开始月晚于"上一期+1"时以社保开始月为准，保证在计薪设置中调整后立即生效
                if (toMonthKey(startYearMonth[0], startYearMonth[1]) > toMonthKey(nextYearMonth[0], nextYearMonth[1])) {
                    year = startYearMonth[0];
                    month = startYearMonth[1];
                } else {
                    year = nextYearMonth[0];
                    month = nextYearMonth[1];
                }
            }
            logger.info("[社保报表生成] 社保开始月={}-{}，上一期={}，本期生成={}-{}",
                    startYearMonth[0], startYearMonth[1],
                    lastMonthRecord == null ? "无" : (lastMonthRecord.getYear() + "-" + lastMonthRecord.getMonth()),
                    year, month);
            if (year < 2000 || year > 2999 || month < 1 || month > 12) {
                throw new Exception("生成的社保报表月份不合法：" + year + "年" + month + "月");
            }
            boolean duplicated = lambdaQuery().eq(HrmInsuranceMonthRecord::getYear, year)
                    .eq(HrmInsuranceMonthRecord::getMonth, month).exists();
            if (duplicated) {
                throw new Exception(year + "年" + month + "月社保报表已存在，无需重复生成");
            }
            // 仅在“生成上一期的下一个月”时才结转上一月（标记已结算、清理其员工明细）；
            // 手动指定历史/其他月份时不触动已有报表，避免误删或误结算。
            if (lastMonthRecord != null) {
                int[] nextYearMonth = nextMonth(lastMonthRecord.getYear(), lastMonthRecord.getMonth());
                boolean isImmediateNext = (nextYearMonth[0] == year && nextYearMonth[1] == month);
                if (isImmediateNext) {
                    updateComputeProgress(12, COMPUTE_STATUS_RUNNING, "PREPARE", "正在结转上一月社保记录", 0, 0);
                    List<Long> empRecordIds = insuranceMonthRecordMapper.queryDeleteEmpRecordIds(lastMonthRecord.getIRecordId());
                    if (CollUtil.isNotEmpty(empRecordIds)) {
                        monthEmpProjectRecordService.lambdaUpdate().in(HrmInsuranceMonthEmpProjectRecord::getIEmpRecordId, empRecordIds).remove();
                        monthEmpRecordService.lambdaUpdate().in(HrmInsuranceMonthEmpRecord::getIEmpRecordId, empRecordIds).remove();
                    }
                    lastMonthRecord.setStatus(IsEnum.YES.getValue());
                    updateById(lastMonthRecord);
                }
            }
            List<Map<String, Long>> employeeIds = insuranceMonthRecordMapper.queryInsuranceEmployee();
            totalCount = employeeIds.size();
            updateComputeProgress(18, COMPUTE_STATUS_RUNNING, "LOAD_EMPLOYEE",
                    "已加载参保员工，共" + totalCount + "人", 0, totalCount);
            HrmInsuranceMonthRecord hrmInsuranceMonthRecord = new HrmInsuranceMonthRecord();
            hrmInsuranceMonthRecord.setTitle(month + "月社保报表");
            hrmInsuranceMonthRecord.setYear(year);
            hrmInsuranceMonthRecord.setMonth(month);
            hrmInsuranceMonthRecord.setNum(employeeIds.size());
            hrmInsuranceMonthRecord.setCreateTime(LocalDateTime.now());
            hrmInsuranceMonthRecord.setCreateUserId(info.getUserIdValueL());
            //保存每月社保记录
            save(hrmInsuranceMonthRecord);
            updateComputeProgress(25, COMPUTE_STATUS_RUNNING, "PERSIST",
                    "已创建" + month + "月社保报表", 0, totalCount);

            int finalYear = year;
            int finalMonth = month;

            for (Map<String, Long> employeeMap : employeeIds) {
                Long employeeId = employeeMap.get("employee_id");
                Long schemeId = employeeMap.get("scheme_id");
                Map<String, Object> stringObjectMap = insuranceSchemeMapper.queryInsuranceSchemeCountById(schemeId);
                HrmInsuranceMonthEmpRecord insuranceMonthEmpRecord = new HrmInsuranceMonthEmpRecord();
                BeanUtil.fillBeanWithMap(stringObjectMap, insuranceMonthEmpRecord, true);
                insuranceMonthEmpRecord.setIRecordId(hrmInsuranceMonthRecord.getIRecordId());
                insuranceMonthEmpRecord.setEmployeeId(employeeId);
                insuranceMonthEmpRecord.setSchemeId(schemeId);
                insuranceMonthEmpRecord.setYear(finalYear);
                insuranceMonthEmpRecord.setMonth(finalMonth);
                monthEmpRecordService.save(insuranceMonthEmpRecord);

                List<HrmInsuranceProject> insuranceProjectList = insuranceProjectService.lambdaQuery().eq(HrmInsuranceProject::getSchemeId, schemeId).list();
                List<HrmInsuranceMonthEmpProjectRecord> monthEmpProjectRecordList = TransferUtil.transferList(insuranceProjectList, HrmInsuranceMonthEmpProjectRecord.class);
                monthEmpProjectRecordList.forEach(monthEmpProjectRecord -> {
                    monthEmpProjectRecord.setIEmpRecordId(insuranceMonthEmpRecord.getIEmpRecordId());
                });
                monthEmpProjectRecordService.saveBatch(monthEmpProjectRecordList);
                processedCount++;
                int progress = totalCount <= 0 ? 95 : 25 + (int) Math.floor((processedCount * 70.0d) / totalCount);
                updateComputeProgress(Math.min(progress, 95), COMPUTE_STATUS_RUNNING, "GENERATE_EMP",
                        "正在生成员工社保 " + processedCount + "/" + totalCount, processedCount, totalCount);
            }
            if (totalCount == 0) {
                updateComputeProgress(95, COMPUTE_STATUS_RUNNING, "GENERATE_EMP",
                        "暂无可生成参保员工", 0, 0);
            }
            updateComputeProgress(98, COMPUTE_STATUS_RUNNING, "PERSIST",
                    "正在完成社保报表生成", processedCount, totalCount);
            JSONObject data = new JSONObject();
            data.put("year", year);
            data.put("month", month);
            data.put("title", year + "年" + month + "月社保报表");
            updateComputeProgress(100, COMPUTE_STATUS_SUCCESS, "FINISH",
                    "社保报表生成完成", processedCount, totalCount);
            
            // 写入通知：社保报表生成完成
            try {
                AdminMessage completeMsg = new AdminMessage();
                completeMsg.setTitle("社保报表生成完成");
                completeMsg.setContent("点击查看");
                completeMsg.setLabel(8); // 人资
                completeMsg.setType(206); // HRM_INSURANCE_COMPUTE_COMPLETE
                completeMsg.setLinkUrl("/hrm/insurance-scheme");
                completeMsg.setCreateUser(0L); // 系统
                completeMsg.setRecipientUser(0L); // 系统级通知
                completeMsg.setCreateTime(LocalDateTime.now());
                completeMsg.setIsRead(0);
                adminMessageService.save(completeMsg);
                logger.info("[社保报表生成] 已写入完成通知");
            } catch (Exception e) {
                logger.error("[社保报表生成] 写入完成通知失败", e);
            }
            
            return data;
        } catch (Exception ex) {
            updateComputeProgress(null, COMPUTE_STATUS_FAILED, "ERROR",
                    resolveComputeErrorMessage(ex), processedCount, totalCount);
            throw ex;
        } finally {
            computeLock.unlock();
        }
    }

    /**
     * 返回建议生成的社保报表年月：有历史则上一期+1，首期用社保开始月；
     * 社保开始月晚于"上一期+1"时以社保开始月为准。供前端弹窗预填默认月份。
     */
    public JSONObject getSuggestMonth() throws Exception {
        HrmSalaryConfig salaryConfig = salaryConfigService.getOne(Wrappers.emptyWrapper());
        if (salaryConfig == null) {
            throw new Exception("尚未初始化计薪设置，请先到【系统设置-计薪设置】完成配置");
        }
        int[] startYearMonth = parseYearMonth(salaryConfig.getSocialSecurityStartMonth(), "社保开始月");
        HrmInsuranceMonthRecord lastMonthRecord = queryLatestMonthRecord();
        int year;
        int month;
        if (lastMonthRecord == null) {
            year = startYearMonth[0];
            month = startYearMonth[1];
        } else {
            int[] nextYearMonth = nextMonth(lastMonthRecord.getYear(), lastMonthRecord.getMonth());
            if (toMonthKey(startYearMonth[0], startYearMonth[1]) > toMonthKey(nextYearMonth[0], nextYearMonth[1])) {
                year = startYearMonth[0];
                month = startYearMonth[1];
            } else {
                year = nextYearMonth[0];
                month = nextYearMonth[1];
            }
        }
        JSONObject data = new JSONObject();
        data.put("year", year);
        data.put("month", month);
        data.put("socialSecurityStartMonth", salaryConfig.getSocialSecurityStartMonth());
        data.put("hasRecord", lastMonthRecord != null);
        if (lastMonthRecord != null) {
            data.put("lastYear", lastMonthRecord.getYear());
            data.put("lastMonth", lastMonthRecord.getMonth());
        }
        return data;
    }

    /**
     * 按 (年*12+月) 取最新一期社保报表。
     * 不用 create_time 排序：补录/回补历史月份时创建时间会倒挂，导致"上一期"取错、生成出重复或跳跃的月份。
     */
    private HrmInsuranceMonthRecord queryLatestMonthRecord() {
        List<HrmInsuranceMonthRecord> records = lambdaQuery().list();
        if (CollUtil.isEmpty(records)) {
            return null;
        }
        HrmInsuranceMonthRecord latest = null;
        int latestKey = Integer.MIN_VALUE;
        for (HrmInsuranceMonthRecord record : records) {
            if (record.getYear() == null || record.getMonth() == null) {
                continue;
            }
            int key = toMonthKey(record.getYear(), record.getMonth());
            if (key > latestKey) {
                latestKey = key;
                latest = record;
            }
        }
        return latest;
    }

    private static int toMonthKey(int year, int month) {
        return year * 12 + month;
    }

    private static int[] nextMonth(int year, int month) {
        int nextMonth = month + 1;
        int nextYear = year;
        if (nextMonth > 12) {
            nextMonth = 1;
            nextYear = year + 1;
        }
        return new int[]{nextYear, nextMonth};
    }

    /**
     * 解析"年月"配置值。历史数据存在 2020.05 / 2026-09-01 / 2026/09 等写法，逐格式尝试；
     * 全部失败或值为空时给出中文提示（旧实现直接抛 hutool 的 "Date String must be not blank !" 英文异常）。
     *
     * @return {年, 月(1-12)}
     */
    private int[] parseYearMonth(String value, String fieldName) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            throw new Exception("未配置" + fieldName + "，请先到【系统设置-计薪设置】设置后再生成社保报表");
        }
        String text = value.trim();
        String[] patterns = new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy.MM", "yyyy/MM", "yyyy年MM月"};
        for (String pattern : patterns) {
            try {
                DateTime dateTime = DateUtil.parse(text, pattern);
                return new int[]{dateTime.year(), dateTime.month() + 1};
            } catch (Exception ignored) {
                // 继续尝试下一种格式
            }
        }
        throw new Exception(fieldName + "格式不正确（当前值：" + text + "），请到【系统设置-计薪设置】重新选择月份");
    }

    public InsuranceComputeProgressVO queryComputeInsuranceProgress() {
        clearExpiredComputeProgress();
        InsuranceComputeProgressState state = INSURANCE_COMPUTE_PROGRESS_MAP.get(buildComputeProgressKey());
        if (state == null) {
            InsuranceComputeProgressVO vo = new InsuranceComputeProgressVO();
            vo.setProgress(0);
            vo.setStatus(COMPUTE_STATUS_IDLE);
            vo.setStage("PREPARE");
            vo.setMessage("等待生成社保报表");
            vo.setProcessedCount(0);
            vo.setTotalCount(0);
            vo.setDone(false);
            vo.setSuccess(false);
            return vo;
        }
        InsuranceComputeProgressVO vo = new InsuranceComputeProgressVO();
        vo.setProgress(state.progress);
        vo.setStatus(state.status);
        vo.setStage(state.stage);
        vo.setMessage(state.message);
        vo.setProcessedCount(state.processedCount);
        vo.setTotalCount(state.totalCount);
        boolean done = COMPUTE_STATUS_SUCCESS.equals(state.status) || COMPUTE_STATUS_FAILED.equals(state.status);
        vo.setDone(done);
        vo.setSuccess(COMPUTE_STATUS_SUCCESS.equals(state.status));
        return vo;
    }

    private void updateComputeProgress(Integer progress,
                                       String status,
                                       String stage,
                                       String message,
                                       Integer processedCount,
                                       Integer totalCount) {
        InsuranceComputeProgressState state = INSURANCE_COMPUTE_PROGRESS_MAP
                .computeIfAbsent(buildComputeProgressKey(), key -> new InsuranceComputeProgressState());
        if (progress != null) {
            state.progress = Math.max(0, Math.min(100, progress));
        }
        if (status != null && !status.trim().isEmpty()) {
            state.status = status;
        }
        if (stage != null && !stage.trim().isEmpty()) {
            state.stage = stage;
        }
        if (message != null && !message.trim().isEmpty()) {
            state.message = message;
        }
        if (processedCount != null) {
            state.processedCount = Math.max(0, processedCount);
        }
        if (totalCount != null) {
            state.totalCount = Math.max(0, totalCount);
        }
        state.updateTime = System.currentTimeMillis();
    }

    private String buildComputeProgressKey() {
        LoginUserInfo info = CompanyContext.get();
        String companyId = info != null && info.getCompanyId() != null && !info.getCompanyId().trim().isEmpty()
                ? info.getCompanyId() : "default";
        String userId = info != null && info.getUserId() != null && !info.getUserId().trim().isEmpty()
                ? info.getUserId() : "default";
        return companyId + ":" + userId + ":insurance";
    }

    private void clearExpiredComputeProgress() {
        long now = System.currentTimeMillis();
        INSURANCE_COMPUTE_PROGRESS_MAP.entrySet().removeIf(entry -> {
            InsuranceComputeProgressState state = entry.getValue();
            if (state == null) {
                return true;
            }
            return now - state.updateTime > INSURANCE_COMPUTE_PROGRESS_TTL_MS;
        });
    }

    private String resolveComputeErrorMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root != null ? root.getMessage() : null;
        return message == null || message.trim().isEmpty() ? "社保报表生成失败" : message;
    }

    @Transactional(rollbackFor = Exception.class)
    public Page<QueryInsuranceRecordListVO> queryInsuranceRecordList(QueryInsuranceRecordListBO recordListBO) {
        if (recordListBO == null) {
            recordListBO = new QueryInsuranceRecordListBO();
        }
        recordListBO.normalizeFilters();
        insuranceMonthRecordMapper.setGroupConcatMaxLen();
        return insuranceMonthRecordMapper.queryInsuranceRecordList(recordListBO.parse(), recordListBO);
    }

    public Page<QueryInsurancePageListVO> queryInsurancePageList(QueryInsurancePageListBO queryInsurancePageListBO) {
        return insuranceMonthRecordMapper.queryInsurancePageList(queryInsurancePageListBO.parse(), queryInsurancePageListBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateSalaryBasicInsuranceAmount(UpdateInsuranceSalaryBasicAmountBO updateBO) {
        if (updateBO == null) {
            throw new IllegalArgumentException("社保保险金额设置参数不能为空");
        }
        List<Long> iEmpRecordIds = normalizeIEmpRecordIds(updateBO.getIEmpRecordIds());
        if (updateBO.getIRecordId() == null && iEmpRecordIds.isEmpty()) {
            throw new IllegalArgumentException("社保记录不能为空");
        }
        boolean includeSalaryBasicAmount = Integer.valueOf(1).equals(updateBO.getIncludeSalaryBasicInsuranceAmount());

        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<HrmInsuranceMonthEmpRecord> query =
                monthEmpRecordService.lambdaQuery()
                        .eq(HrmInsuranceMonthEmpRecord::getStatus, IsEnum.YES.getValue());
        if (updateBO.getIRecordId() != null) {
            query.eq(HrmInsuranceMonthEmpRecord::getIRecordId, updateBO.getIRecordId());
        }
        if (!iEmpRecordIds.isEmpty()) {
            query.in(HrmInsuranceMonthEmpRecord::getIEmpRecordId, iEmpRecordIds);
        }
        List<HrmInsuranceMonthEmpRecord> records = query.list();
        if (CollUtil.isEmpty(records)) {
            return 0;
        }
        records.forEach(record -> refreshSalaryBasicInsuranceAmount(record, includeSalaryBasicAmount));
        monthEmpRecordService.updateBatchById(records);
        return records.size();
    }

    public void refreshSalaryBasicInsuranceAmount(HrmInsuranceMonthEmpRecord record, boolean includeSalaryBasicAmount) {
        if (record == null) {
            return;
        }
        Map<String, Object> projectCount = monthEmpProjectRecordService.queryProjectCount(record.getIEmpRecordId());
        record.setPersonalInsuranceAmount(defaultZero(readAmount(projectCount,
                "personalInsuranceAmount", "personal_insurance_amount")));
        record.setCorporateInsuranceAmount(defaultZero(readAmount(projectCount,
                "corporateInsuranceAmount", "corporate_insurance_amount")));
        record.setPersonalProvidentFundAmount(readAmount(projectCount,
                "personalProvidentFundAmount", "personal_provident_fund_amount"));
        record.setCorporateProvidentFundAmount(readAmount(projectCount,
                "corporateProvidentFundAmount", "corporate_provident_fund_amount"));
        record.setIncludeSalaryBasicInsuranceAmount(includeSalaryBasicAmount ? 1 : 0);

        if (!includeSalaryBasicAmount) {
            return;
        }
        QuerySalaryBasicVO salaryBasic = salaryBasicService == null ? new QuerySalaryBasicVO() : salaryBasicService.findAll();
        HrmSalaryBasicDefaults.applyTo(salaryBasic);
        record.setPersonalInsuranceAmount(defaultZero(record.getPersonalInsuranceAmount())
                .add(defaultZero(salaryBasic.getLongTermCareInsuranceAmount())));
        record.setCorporateInsuranceAmount(defaultZero(record.getCorporateInsuranceAmount())
                .add(defaultZero(salaryBasic.getLargeMedicalInsuranceAmount())));
    }

    private List<Long> normalizeIEmpRecordIds(List<Long> iEmpRecordIds) {
        if (CollUtil.isEmpty(iEmpRecordIds)) {
            return new ArrayList<>();
        }
        return iEmpRecordIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private BigDecimal readAmount(Map<String, Object> map, String camelKey, String underscoreKey) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Object value = map.containsKey(camelKey) ? map.get(camelKey) : map.get(underscoreKey);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : new BigDecimal(text);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public QueryInsuranceRecordListVO queryInsuranceRecord(String iRecordId) {
//        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.INSURANCE_MENU_ID);
//        boolean exists = false;
//        if (CollUtil.isNotEmpty(employeeIds)) {
//            exists = monthEmpRecordService.lambdaQuery().eq(HrmInsuranceMonthEmpRecord::getIRecordId, iRecordId)
//                    .in(HrmInsuranceMonthEmpRecord::getEmployeeId, employeeIds).exists();
//        }
//        if (exists) {
//            return insuranceMonthRecordMapper.queryInsuranceRecord(iRecordId, employeeIds);
//        }
//        return insuranceMonthRecordMapper.queryNoEmpInsuranceRecord(iRecordId);
        return insuranceMonthRecordMapper.queryInsuranceRecord(iRecordId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public OperationLog deleteInsurance(Long iRecordId) throws Exception{
        Integer count = lambdaQuery().count().intValue();
//        if (count == 1) {
//            throw new Exception("只有一个月社保记录,不能删除");
//        }

        HrmInsuranceMonthRecord insuranceMonthRecord = getById(iRecordId);

        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(insuranceMonthRecord.getIRecordId(), insuranceMonthRecord.getTitle());
        operationLog.setOperationInfo("删除" + insuranceMonthRecord.getTitle());

        List<Long> iEmpRecordIds = monthEmpRecordService.lambdaQuery().select(HrmInsuranceMonthEmpRecord::getIEmpRecordId).eq(HrmInsuranceMonthEmpRecord::getIRecordId, iRecordId).list()
                .stream().map(HrmInsuranceMonthEmpRecord::getIEmpRecordId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(iEmpRecordIds)) {
            monthEmpProjectRecordService.lambdaUpdate().in(HrmInsuranceMonthEmpProjectRecord::getIEmpRecordId, iEmpRecordIds).remove();
            monthEmpRecordService.lambdaUpdate().in(HrmInsuranceMonthEmpRecord::getIEmpRecordId, iEmpRecordIds).remove();
        }
        removeById(iRecordId);
//        HrmInsuranceMonthRecord monthRecord = lambdaQuery().orderByDesc(HrmInsuranceMonthRecord::getCreateTime).one();
//        monthRecord.setStatus(0);
//        updateById(monthRecord);
//        insuranceActionRecordService.deleteInsurance(monthRecord);
        return operationLog;
    }
}
