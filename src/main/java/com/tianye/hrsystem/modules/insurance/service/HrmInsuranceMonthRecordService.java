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
import java.util.Optional;
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

    public JSONObject computeInsuranceData() throws Exception {
        LoginUserInfo lockOwner = CompanyContext.get();
        String lockCompanyId = lockOwner != null && lockOwner.getCompanyId() != null ? lockOwner.getCompanyId() : "unknown";
        ReentrantLock computeLock = INSURANCE_COMPUTE_LOCK_MAP.computeIfAbsent(lockCompanyId, key -> new ReentrantLock());
        if (!computeLock.tryLock()) {
            logger.warn("拒绝重复生成社保报表：本公司已有生成任务在运行");
            throw new Exception("社保报表正在生成中，请勿重复点击，可稍后刷新查看进度");
        }
        clearExpiredComputeProgress();
        updateComputeProgress(1, COMPUTE_STATUS_RUNNING, "PREPARE", "正在准备生成社保报表", 0, 0);
        int processedCount = 0;
        int totalCount = 0;
        try {
        LoginUserInfo info = CompanyContext.get();
        HrmSalaryConfig salaryConfig = salaryConfigService.getOne(Wrappers.emptyWrapper());
        if (salaryConfig == null) {
            throw new Exception("没有初始化配置");
        }
        updateComputeProgress(8, COMPUTE_STATUS_RUNNING, "PREPARE", "已读取社保薪资配置", 0, 0);
        String socialSecurityMonth = salaryConfig.getSocialSecurityStartMonth();
        DateTime dateTime = DateUtil.parse(socialSecurityMonth, "yyyy-MM");
        int month = dateTime.month() + 1;
        int year = dateTime.year();
        //查询社保上月记录,如果有就往后推一个月,如果没有就去薪资配置计薪月
        Optional<HrmInsuranceMonthRecord> lastMonthRecord = lambdaQuery().orderByDesc(HrmInsuranceMonthRecord::getCreateTime).last("limit 1").oneOpt();
        if (lastMonthRecord.isPresent()) {
            updateComputeProgress(12, COMPUTE_STATUS_RUNNING, "PREPARE", "正在结转上一月社保记录", 0, 0);
            HrmInsuranceMonthRecord insuranceMonthRecord = lastMonthRecord.get();
            DateTime date = DateUtil.offsetMonth(DateUtil.parse(insuranceMonthRecord.getYear() + "-" + insuranceMonthRecord.getMonth(), "yy-MM"), 1);
            month = date.month() + 1;
            year = date.year();
            List<Long> empRecordIds = insuranceMonthRecordMapper.queryDeleteEmpRecordIds(insuranceMonthRecord.getIRecordId());
            if (CollUtil.isNotEmpty(empRecordIds)) {
                monthEmpProjectRecordService.lambdaUpdate().in(HrmInsuranceMonthEmpProjectRecord::getIEmpRecordId, empRecordIds).remove();
                monthEmpRecordService.lambdaUpdate().in(HrmInsuranceMonthEmpRecord::getIEmpRecordId, empRecordIds).remove();
            }
            insuranceMonthRecord.setStatus(IsEnum.YES.getValue());
            updateById(insuranceMonthRecord);
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

//        OperationLog operationLog = new OperationLog();
//        operationLog.setOperationObject(hrmInsuranceMonthRecord.getIRecordId(), hrmInsuranceMonthRecord.getTitle());
//        operationLog.setOperationInfo("新建" + hrmInsuranceMonthRecord.getTitle());

//        insuranceActionRecordService.computeInsuranceDataLog(hrmInsuranceMonthRecord);

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

            //发送通知
            AdminMessage adminMessage = new AdminMessage();
            adminMessage.setCreateUser(Long.valueOf(info.getUserId()));
            adminMessage.setCreateTime(LocalDateTime.now());
            adminMessage.setRecipientUser(Long.valueOf(info.getUserId()));
            adminMessage.setLabel(8);
            adminMessage.setType(AdminMessageEnum.HRM_EMPLOYEE_INSURANCE.getType());
            adminMessage.setTitle(finalYear + "-" + finalMonth + "{admin.hrm.9e06b58abc0ca454d6f1463aa168010c}");
//            ApplicationContextHolder.getBean(IAdminMessageService.class).save(adminMessage);
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
//        data.put("operationLog", operationLog);
        updateComputeProgress(100, COMPUTE_STATUS_SUCCESS, "FINISH",
                "社保报表生成完成", processedCount, totalCount);
        return data;
        } catch (Exception ex) {
            updateComputeProgress(null, COMPUTE_STATUS_FAILED, "ERROR",
                    resolveComputeErrorMessage(ex), processedCount, totalCount);
            throw ex;
        } finally {
            computeLock.unlock();
        }
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
