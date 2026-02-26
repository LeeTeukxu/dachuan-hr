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
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpProjectRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceMonthRecordMapper;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceSechemeMapper;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsuranceRecordListVO;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.service.IHrmSalaryConfigService;
import com.tianye.hrsystem.modules.salary.vo.AdminMessageVO;
import com.tianye.hrsystem.service.IAdminMessageService;
import com.tianye.hrsystem.util.TransferUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HrmInsuranceMonthRecordService extends BaseServiceImpl<HrmInsuranceMonthRecordMapper, HrmInsuranceMonthRecord> {
    @Autowired
    private IHrmSalaryConfigService salaryConfigService;

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
        LoginUserInfo info = CompanyContext.get();
        HrmSalaryConfig salaryConfig = salaryConfigService.getOne(Wrappers.emptyWrapper());
        if (salaryConfig == null) {
            throw new Exception("没有初始化配置");
        }
        String socialSecurityMonth = salaryConfig.getSocialSecurityStartMonth();
        DateTime dateTime = DateUtil.parse(socialSecurityMonth, "yyyy-MM");
        int month = dateTime.month() + 1;
        int year = dateTime.year();
        //查询社保上月记录,如果有就往后推一个月,如果没有就去薪资配置计薪月
        Optional<HrmInsuranceMonthRecord> lastMonthRecord = lambdaQuery().orderByDesc(HrmInsuranceMonthRecord::getCreateTime).last("limit 1").oneOpt();
        if (lastMonthRecord.isPresent()) {
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
        HrmInsuranceMonthRecord hrmInsuranceMonthRecord = new HrmInsuranceMonthRecord();
        hrmInsuranceMonthRecord.setTitle(month + "月社保报表");
        hrmInsuranceMonthRecord.setYear(year);
        hrmInsuranceMonthRecord.setMonth(month);
        hrmInsuranceMonthRecord.setNum(employeeIds.size());
        hrmInsuranceMonthRecord.setCreateTime(LocalDateTime.now());
        hrmInsuranceMonthRecord.setCreateUserId(info.getUserIdValueL());
        //保存每月社保记录
        save(hrmInsuranceMonthRecord);

//        OperationLog operationLog = new OperationLog();
//        operationLog.setOperationObject(hrmInsuranceMonthRecord.getIRecordId(), hrmInsuranceMonthRecord.getTitle());
//        operationLog.setOperationInfo("新建" + hrmInsuranceMonthRecord.getTitle());

//        insuranceActionRecordService.computeInsuranceDataLog(hrmInsuranceMonthRecord);

        int finalYear = year;
        int finalMonth = month;

        employeeIds.forEach(employeeMap -> {
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
        });
        JSONObject data = new JSONObject();
        data.put("year", year);
//        data.put("operationLog", operationLog);
        return data;
    }

    public Page<QueryInsuranceRecordListVO> queryInsuranceRecordList(QueryInsuranceRecordListBO recordListBO) {
        return insuranceMonthRecordMapper.queryInsuranceRecordList(recordListBO.parse(), recordListBO);
    }

    public Page<QueryInsurancePageListVO> queryInsurancePageList(QueryInsurancePageListBO queryInsurancePageListBO) {
        return insuranceMonthRecordMapper.queryInsurancePageList(queryInsurancePageListBO.parse(), queryInsurancePageListBO);
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
