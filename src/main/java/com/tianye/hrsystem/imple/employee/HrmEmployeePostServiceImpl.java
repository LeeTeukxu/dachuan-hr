package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.tianye.hrsystem.enums.FieldTypeEnum;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.common.EmployeeHolder;
import com.tianye.hrsystem.common.LanguageFieldUtil;
import com.tianye.hrsystem.config.ApplicationContextHolder;
import com.tianye.hrsystem.entity.bo.DeleteLeaveInformationBO;
import com.tianye.hrsystem.entity.bo.UpdateInformationBO;
import com.tianye.hrsystem.entity.po.*;
import com.tianye.hrsystem.entity.vo.*;
import com.tianye.hrsystem.enums.*;
import com.tianye.hrsystem.mapper.HrmEmployeePostMapper;
import com.tianye.hrsystem.service.IHrmActionRecordService;
import com.tianye.hrsystem.service.employee.*;
import com.tianye.hrsystem.util.EmployeeUtil;
import com.tianye.hrsystem.util.FieldUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 员工证书 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeePostServiceImpl extends BaseServiceImpl<HrmEmployeePostMapper, HrmEmployeeCertificate> implements IHrmEmployeePostService {

    @Autowired
    private IHrmEmployeeQuitInfoService quitInfoService;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    private IHrmEmployeeDataService employeeDataService;

    @Autowired
    private IHrmEmployeeFieldService employeeFieldService;
    @Resource
    private EmployeeActionRecordServiceImpl employeeActionRecordService;

    @Autowired
    private IHrmEmployeeAbnormalChangeRecordService abnormalChangeRecordService;
    @Autowired
    private IHrmFieldExtendService hrmFieldExtendService;

    @Autowired
    private IHrmEmployeeEmploymentRecordService employmentRecordService;

    private static final String COMPANY_AGE = "company_age";

    @Override
    public PostInformationVO postInformation(Long employeeId) {
        HrmEmployeeQuitInfo employeeQuitInfo = quitInfoService.lambdaQuery()
                .eq(HrmEmployeeQuitInfo::getEmployeeId, employeeId)
                .eq(HrmEmployeeQuitInfo::getIsArchived, 0)
                .last("limit 1")
                .one();
        HrmEmployee employee = employeeService.getById(employeeId);
        refreshCompanyAge(employee);
        List<HrmEmployeeData> fieldValueList = employeeDataService.queryListByEmployeeId(employeeId);
        JSONObject employeeModel = BeanUtil.copyProperties(employee, JSONObject.class);
        List<InformationFieldVO> informationFieldVOList = employeeService.transferInformation(employeeModel, LabelGroupEnum.POST, fieldValueList);
        String companyAgeDesc = EmployeeUtil.computeCompanyAge(employee.getCompanyAge());
        informationFieldVOList.forEach(fieldValue -> {
            if (COMPANY_AGE.equals(fieldValue.getFieldName())) {
                fieldValue.setFieldValueDesc(companyAgeDesc);
            }
            //添加语言包key
            Map<String, String> keyMap = LanguageFieldUtil.getFieldNameKeyMap("name_resourceKey", "customField.hrmField.", fieldValue.getFieldName(), fieldValue.getSetting());
            if ("channel_id".equals(fieldValue.getFieldName())) {
                keyMap.put("fieldValueDesc", "admin.recruitChannel." + fieldValue.getFieldValueDesc());
            }
            fieldValue.setLanguageKeyMap(keyMap);
        });
        if(employee.getEmploymentForms()!=null){
            if (employee.getEmploymentForms().equals(EmploymentFormsEnum.NO_OFFICIAL.getValue())) {
                informationFieldVOList.removeIf(fieldValue -> "probation".equals(fieldValue.getFieldName()));
            }
        }
        return new PostInformationVO(informationFieldVOList, employeeQuitInfo);
    }

    private void refreshCompanyAge(HrmEmployee employee) {
        if (employee == null || ObjectUtil.equal(employee.getEntryStatus(), EmployeeEntryStatus.ALREADY_LEAVE.getValue())) {
            return;
        }
        boolean needUpdate = false;
        if (employee.getCompanyAgeStartTime() == null && employee.getEntryTime() != null) {
            employee.setCompanyAgeStartTime(employee.getEntryTime());
            needUpdate = true;
        }
        if (employee.getCompanyAgeStartTime() == null) {
            if (employee.getCompanyAge() == null) {
                employee.setCompanyAge(0);
            }
            return;
        }
        long nowCompanyAge = LocalDateTimeUtil.between(employee.getCompanyAgeStartTime().atStartOfDay(), LocalDateTime.now()).toDays() + 1;
        if (LocalDateTimeUtil.toEpochMilli(employee.getCompanyAgeStartTime()) > System.currentTimeMillis()) {
            nowCompanyAge = 0;
        }
        int companyAge = (int) nowCompanyAge;
        if (!ObjectUtil.equal(employee.getCompanyAge(), companyAge)) {
            employee.setCompanyAge(companyAge);
            needUpdate = true;
        }
        if (needUpdate) {
            employeeService.updateById(employee);
        }
    }

    @Override
    public OperationLog updatePostInformation(UpdateInformationBO updateInformationBO) {
        Long employeeId = updateInformationBO.getEmployeeId();
        HrmEmployee oldHrmEmployee = employeeService.getById(employeeId);

        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(oldHrmEmployee.getEmployeeId(), oldHrmEmployee.getEmployeeName());

        List<UpdateInformationBO.InformationFieldBO> dataList = updateInformationBO.getDataList();
        Map<FiledIsFixedEnum, List<UpdateInformationBO.InformationFieldBO>> isFixedMap = dataList.stream().collect(Collectors.groupingBy(employeeData -> FiledIsFixedEnum.parse(employeeData.getIsFixed())));
        List<UpdateInformationBO.InformationFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(employeeData -> {
            Object converted = FieldUtil.convertFieldValue(employeeData.getType(), employeeData.getFieldValue(), IsEnum.YES.getValue());
            // 日期类字段被清空时前端提交空串/空值:空串转 LocalDate 会抛异常导致保存失败,统一置 null
            if (ObjectUtil.isEmpty(converted)
                    && (Integer.valueOf(FieldTypeEnum.DATE.getValue()).equals(employeeData.getType())
                    || Integer.valueOf(FieldTypeEnum.DATETIME.getValue()).equals(employeeData.getType()))) {
                converted = null;
            }
            jsonObject.put(employeeData.getFieldName(), converted);
        });
        // 司龄开始日期是否被本次提交清空(用于下方显式置 NULL)
        boolean companyAgeStartCleared = fixedEmployeeData.stream().anyMatch(field ->
                "company_age_start_time".equals(field.getFieldName()) && ObjectUtil.isEmpty(field.getFieldValue()));
        HrmEmployee employee = jsonObject.toJavaObject(HrmEmployee.class);
        if (employee.getDeptId() == null) {
            employeeService.lambdaUpdate().set(HrmEmployee::getDeptId, null).eq(HrmEmployee::getEmployeeId, employeeId).update();
        }
        if (employee.getParentId() == null) {
            employeeService.lambdaUpdate().set(HrmEmployee::getParentId, null).eq(HrmEmployee::getEmployeeId, employeeId).update();
        }
        // 司龄开始日期允许清空:updateById 默认忽略 null 字段,清空必须显式置 NULL 才能真正保存,
        // 否则旧日期残留,员工司龄仍按旧日期计算(田野农谷反馈)
        if (employee.getCompanyAgeStartTime() == null && companyAgeStartCleared) {
            employeeService.lambdaUpdate().set(HrmEmployee::getCompanyAgeStartTime, null)
                    .eq(HrmEmployee::getEmployeeId, employeeId).update();
        }
        employee.setEmployeeId(employeeId);
        Integer probation = employee.getProbation();
        if (probation != null) {
            if (probation == 0) {
                employee.setStatus(EmployeeStatusEnum.OFFICIAL.getValue());
                employee.setBecomeTime(employee.getEntryTime());
            } else {
                LocalDateTime dateTime = LocalDateTimeUtil.offset(employee.getEntryTime().atStartOfDay(), probation, ChronoUnit.MONTHS);
                if (dateTime.isAfter(LocalDateTimeUtil.beginOfDay(LocalDateTime.now()))) {
                    employee.setStatus(EmployeeStatusEnum.TRY_OUT.getValue());
                } else {
                    employee.setStatus(EmployeeStatusEnum.OFFICIAL.getValue());
                }
                employee.setBecomeTime(dateTime.toLocalDate());
            }
        }
        List<UpdateInformationBO.InformationFieldBO> informationFieldBOS = isFixedMap.get(FiledIsFixedEnum.NO_FIXED);
        if (informationFieldBOS == null) {
            informationFieldBOS = new ArrayList<>();
        }
        List<HrmEmployeeData> hrmEmployeeData = informationFieldBOS.stream()
                .map(field -> {
                    Object value = field.getFieldValue();
                    if (value == null) {
                        value = "";
                    }
                    field.setFieldValue(employeeFieldService.convertObjectValueToString(field.getType(), field.getFieldValue(), value.toString()));
                    return BeanUtil.copyProperties(field, HrmEmployeeData.class);
                }).collect(Collectors.toList());
        Dict kv = Dict.create().set("key", "employee_id").set("param", "label_group").set("labelGroup", LabelGroupEnum.POST.getValue()).set("value", employeeId).set("dataTableName", "hrm_employee_data");
        List<HrmModelFiledVO> oldFieldList = ApplicationContextHolder.getBean(IHrmActionRecordService.class).queryFieldValue(kv);
        employeeFieldService.saveEmployeeField(hrmEmployeeData, LabelGroupEnum.POST, employeeId);
        if (null != oldHrmEmployee.getEntryTime() && !oldHrmEmployee.getEntryTime().equals(employee.getEntryTime())) {
            if (null == employee.getCompanyAgeStartTime() && null == oldHrmEmployee.getCompanyAgeStartTime()) {
                employee.setCompanyAgeStartTime(employee.getEntryTime());
            }
        }
        employeeService.saveOrUpdate(employee);
        //固定字段操作记录保存
        Content content = employeeActionRecordService.employeeFixedFieldRecord(BeanUtil.beanToMap(oldHrmEmployee), BeanUtil.beanToMap(employee), LabelGroupEnum.POST, employeeId);

        //非固定字段操作记录保存
        Content content1 = employeeActionRecordService.employeeNOFixedFieldRecord(informationFieldBOS, oldFieldList, employeeId);

        String[] split = content.getDetail().split(",");
        String[] split1 = content1.getDetail().split(",");
        operationLog.setOperationInfo(JSONUtil.toJsonStr(ArrayUtil.addAll(split1, split)));

        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog addOrUpdateLeaveInformation(HrmEmployeeQuitInfo quitInfo) {
        HrmEmployee employee;

        OperationLog operationLog = new OperationLog();
        if (quitInfo.getQuitInfoId() == null) {
            boolean exists = quitInfoService.lambdaQuery()
                    .eq(HrmEmployeeQuitInfo::getEmployeeId, quitInfo.getEmployeeId())
                    .eq(HrmEmployeeQuitInfo::getIsArchived, 0)
                    .exists();
            if (exists) {
                throw new CrmException(HrmCodeEnum.THE_EMPLOYEE_HAS_ALREADY_HANDLED_THE_RESIGNATION);
            }
            Content content = employeeActionRecordService.quitRecord(quitInfo);
            employee = employeeService.getById(quitInfo.getEmployeeId());
            quitInfo.setOldStatus(employee.getStatus());

            operationLog.setOperationObject(quitInfo.getEmployeeId(), employee.getEmployeeName());
            operationLog.setOperationInfo(content.getDetail());
        } else {
            employee = new HrmEmployee();
            employee.setEmployeeId(quitInfo.getEmployeeId());
            HrmEmployeeQuitInfo old = quitInfoService.getById(quitInfo.getQuitInfoId());

            HrmEmployee hrmEmployee = employeeService.getById(quitInfo.getEmployeeId());
            operationLog.setOperationObject(hrmEmployee.getEmployeeId(), hrmEmployee.getEmployeeName());
            Content content = employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.QUIT, BeanUtil.beanToMap(old), BeanUtil.beanToMap(quitInfo), quitInfo.getEmployeeId());
            operationLog.setOperationInfo(content.getDetail());
        }
        LocalDateTime planQuitTime = quitInfo.getPlanQuitTime().atStartOfDay();
        EmployeeEntryStatus entryStatus;
        if (LocalDateTimeUtil.toEpochMilli(planQuitTime) > System.currentTimeMillis()) {
            entryStatus = EmployeeEntryStatus.TO_LEAVE;
        } else {
            entryStatus = EmployeeEntryStatus.ALREADY_LEAVE;
            if (ObjectUtil.isNotNull(employee.getCompanyAgeStartTime())) {
                long nowCompanyAge = LocalDateTimeUtil.between(employee.getCompanyAgeStartTime().atStartOfDay(), LocalDateTime.now()).toDays() + 1;
                employee.setCompanyAge(Convert.toInt(nowCompanyAge));
            }
            abnormalChangeRecordService.addAbnormalChangeRecord(quitInfo.getEmployeeId(), AbnormalChangeType.RESIGNATION, quitInfo.getPlanQuitTime().atStartOfDay());
        }
        employee.setEntryStatus(entryStatus.getValue());
        employeeService.updateById(employee);
        quitInfoService.saveOrUpdate(quitInfo);
        // 记录离职时间节点（幂等，修改离职信息时会同步更新节点日期）
        employmentRecordService.recordLeave(quitInfo.getEmployeeId(), quitInfo.getPlanQuitTime(), null);
        return operationLog;
    }

    @Override
    public OperationLog deleteLeaveInformation(DeleteLeaveInformationBO deleteLeaveInformationBO) {
        Long employeeId = deleteLeaveInformationBO.getEmployeeId();
        HrmEmployee hrmEmployee = employeeService.getById(employeeId);
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(hrmEmployee.getEmployeeId(), hrmEmployee.getEmployeeName());

        HrmEmployeeQuitInfo quitInfo = quitInfoService.lambdaQuery()
                .eq(HrmEmployeeQuitInfo::getEmployeeId, employeeId)
                .eq(HrmEmployeeQuitInfo::getIsArchived, 0)
                .last("limit 1")
                .one();
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(quitInfo.getEmployeeId());
        employee.setEntryStatus(EmployeeEntryStatus.IN.getValue());
        employeeService.updateById(employee);
        quitInfoService.removeById(quitInfo.getQuitInfoId());
        employmentRecordService.cancelLeave(employeeId);
        Content content = employeeActionRecordService.cancelLeave(deleteLeaveInformationBO);
        operationLog.setOperationInfo(content.getDetail());
        return operationLog;
    }

    @Override
    public PostInformationVO postArchives() {
        Long employeeId = EmployeeHolder.getEmployeeId();
        HrmEmployeeQuitInfo employeeQuitInfo = quitInfoService.lambdaQuery()
                .eq(HrmEmployeeQuitInfo::getEmployeeId, employeeId)
                .eq(HrmEmployeeQuitInfo::getIsArchived, 0)
                .last("limit 1")
                .one();
        HrmEmployee employee = employeeService.getById(employeeId);
        List<HrmEmployeeData> fieldValueList = employeeDataService.queryListByEmployeeId(employeeId);
        JSONObject employeeModel = BeanUtil.copyProperties(employee, JSONObject.class);
        List<HrmEmployeeField> list = employeeFieldService.lambdaQuery().eq(HrmEmployeeField::getIsHidden, 0)
                .eq(HrmEmployeeField::getLabelGroup, LabelGroupEnum.POST.getValue())
                .eq(HrmEmployeeField::getIsEmployeeVisible, 1)
                .orderByAsc(HrmEmployeeField::getSorting).list();
        List<InformationFieldVO> informationFieldVOList = employeeService.transferInformation(employeeModel, list, fieldValueList);
        //计算司龄,修改描述
        String companyAgeDesc = EmployeeUtil.computeCompanyAge(employee.getCompanyAge());
        informationFieldVOList.forEach(fieldValue -> {
            if (COMPANY_AGE.equals(fieldValue.getFieldName())) {
                fieldValue.setFieldValueDesc(companyAgeDesc);
            }
            //添加语言包key
            fieldValue.setLanguageKeyMap(LanguageFieldUtil.getFieldNameKeyMap("name_resourceKey", "customField.hrmField.", fieldValue.getFieldName(), fieldValue.getSetting()));
        });
        if (employee.getEmploymentForms().equals(EmploymentFormsEnum.NO_OFFICIAL.getValue())) {
            informationFieldVOList.removeIf(fieldValue -> "probation".equals(fieldValue.getFieldName()));
        }

        return new PostInformationVO(informationFieldVOList, employeeQuitInfo);
    }

}
