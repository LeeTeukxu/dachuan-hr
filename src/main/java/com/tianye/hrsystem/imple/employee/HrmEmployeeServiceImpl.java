package com.tianye.hrsystem.imple.employee;

import ch.qos.logback.core.util.DatePatternToRegexUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.*;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.*;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.*;
import com.tianye.hrsystem.config.ApplicationContextHolder;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.*;
import com.tianye.hrsystem.entity.po.*;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.entity.vo.*;
import com.tianye.hrsystem.enums.*;
import com.tianye.hrsystem.imple.CandidateActionRecordServiceImpl;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.entity.po.HrmRecruitChannel;
import com.tianye.hrsystem.model.HrmFieldExtend;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceScheme;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthEmpRecordService;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceSchemeService;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryArchivesOption;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryArchivesOptionService;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryBasicService;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeEducationExperienceRepository;
import com.tianye.hrsystem.mapper.HrmFieldExtendMapper;
import com.tianye.hrsystem.repository.hrmEmployeeQuitInfoRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.service.AdminFileService;
import com.tianye.hrsystem.service.IHrmActionRecordService;
import com.tianye.hrsystem.service.IHrmDeptService;
import com.tianye.hrsystem.service.IHrmRecruitCandidateService;
import com.tianye.hrsystem.service.employee.*;
import com.tianye.hrsystem.util.EmployeeUtil;
import com.tianye.hrsystem.util.RecursionUtil;
import com.tianye.hrsystem.util.FieldUtil;
import com.tianye.hrsystem.util.TransferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 员工表 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeeServiceImpl extends BaseServiceImpl<HrmEmployeeMapper, HrmEmployee> implements IHrmEmployeeService {

    /** 钉钉映射前置保证：在职员工保存时必须能按姓名+手机号映射到钉钉 userId（查无此人拒绝保存） */
    @Autowired
    private com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService approvalSyncService;

    @Autowired
    private HrmEmployeeMapper employeeMapper;

    @Autowired
    private IHrmEmployeeDataService employeeDataService;

    @Autowired
    private IHrmEmployeeFieldService employeeFieldService;

    @Autowired
    private IHrmDeptService hrmDeptService;

    @Autowired
    private IHrmEmployeeEducationExperienceService educationExperienceService;

    @Autowired
    private IHrmEmployeeWorkExperienceService workExperienceService;

    @Autowired
    private IHrmEmployeePostService certificateService;

    @Autowired
    private IHrmEmployeeContactsService contactsService;

    @Autowired
    private IHrmEmployeeContactsDataService contactsDataService;

    @Autowired
    private IHrmEmployeeTrainingExperienceService trainingExperienceService;

    @Autowired
    private IHrmEmployeeChangeRecordService changeRecordService;

    @Autowired
    private HrmInsuranceSchemeService insuranceSchemeService;

    @Autowired
    private HrmSalaryArchivesOptionService salaryArchivesOptionService;

    @Autowired
    private HrmSalaryBasicService salaryBasicService;

    @Autowired
    private HrmInsuranceMonthEmpRecordService insuranceMonthEmpRecordService;

    @Autowired
    private IHrmEmployeeSocialSecurityService securityInfoService;

    @Autowired
    private IHrmEmployeeContractService contractService;

    @Resource
    private EmployeeActionRecordServiceImpl employeeActionRecordService;

    @Autowired
    private IHrmEmployeeQuitInfoService quitInfoService;

    @Autowired
    private IHrmRecruitChannelService recruitChannelService;

    @Autowired
    private IHrmEmployeeAbnormalChangeRecordService abnormalChangeRecordService;

    @Autowired
    private IHrmEmployeeEmploymentRecordService employmentRecordService;

    @Autowired
    private IHrmRecruitCandidateService candidateService;

    @Resource
    private CandidateActionRecordServiceImpl candidateActionRecordService;

    @Resource
    private com.tianye.hrsystem.modules.dashboard.service.DashboardPermissionSupport dashboardPermissionSupport;

    @Autowired
    private EmployeeUtil employeeUtil;
//    @Autowired
//    private IAdminMessageService messageService;
    @Autowired
    private IHrmFieldExtendService hrmFieldExtendService;

    @Autowired
    private HrmFieldExtendMapper fieldExtendMapper;

    @Autowired
    private AdminFileService adminFileService;

    private static final int TWO = 2;
    private static final String ROSTER_IMPORT_SHEET_NAME = "田野农谷";
    private static final int ROSTER_GROUP_ROW_INDEX = 0;
    private static final int ROSTER_HEADER_ROW_INDEX = 1;
    private static final int ROSTER_DATA_START_ROW_INDEX = 2;
    private static final String SALARY_LEVEL_DISPLAY_NAME = "薪资等级";
    private static final String FIXED_PERFORMANCE_DISPLAY_NAME = "固定绩效";
    private static final String DUTY_SUBSIDY_DISPLAY_NAME = "职务补助";
    private static final String OTHER_SUBSIDY_DISPLAY_NAME = "其他补助";
    private static final String SALARY_LEVEL_FIELD_NAME = "salary_level";
    private static final String FIXED_PERFORMANCE_FIELD_NAME = "fixed_performance";
    private static final String DUTY_SUBSIDY_FIELD_NAME = "duty_subsidy";
    private static final String OTHER_SUBSIDY_FIELD_NAME = "other_subsidy";

    @Override
    public List<HrmEmployee> listByIds(List<Long> employeeIds) {
         return  lambdaQuery().in(HrmEmployee::getEmployeeId,employeeIds).list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OperationLog> add(AddEmployeeBO employeeVO) {
        List<OperationLog> operationLogList = new ArrayList<>();

        if (employeeVO.getCandidateId() != null) {
            if (employeeVO.getEntryStatus() == 1) {
                candidateService.lambdaUpdate().set(HrmRecruitCandidate::getStatus, CandidateStatusEnum.HAVE_JOINED.getValue())
                        .set(HrmRecruitCandidate::getStatusUpdateTime, new Date())
                        .set(HrmRecruitCandidate::getEntryTime, new Date())
                        .eq(HrmRecruitCandidate::getCandidateId, employeeVO.getCandidateId()).update();
                UpdateCandidateStatusBO updateCandidateStatusBO = new UpdateCandidateStatusBO();
                updateCandidateStatusBO.setCandidateIds(Collections.singletonList(employeeVO.getCandidateId()));
                updateCandidateStatusBO.setStatus(CandidateStatusEnum.HAVE_JOINED.getValue());
                operationLogList.addAll(candidateActionRecordService.updateCandidateStatusRecord(updateCandidateStatusBO));

            } else {
                candidateService.lambdaUpdate().set(HrmRecruitCandidate::getStatus, CandidateStatusEnum.PENDING_ENTRY.getValue())
                        .set(HrmRecruitCandidate::getStatusUpdateTime, new Date())
                        .eq(HrmRecruitCandidate::getCandidateId, employeeVO.getCandidateId()).update();
                UpdateCandidateStatusBO updateCandidateStatusBO = new UpdateCandidateStatusBO();
                updateCandidateStatusBO.setCandidateIds(Collections.singletonList(employeeVO.getCandidateId()));
                updateCandidateStatusBO.setStatus(CandidateStatusEnum.PENDING_ENTRY.getValue());
                operationLogList.addAll(candidateActionRecordService.updateCandidateStatusRecord(updateCandidateStatusBO));
            }
        }
        HrmEmployee employee = BeanUtil.copyProperties(employeeVO, HrmEmployee.class);
        if (employee.getCandidateId() != null) {
            HrmRecruitCandidate candidate = candidateService.getById(employee.getCandidateId());
            employee.setChannelId(candidate.getChannelId());
        }
        if(employee.getEntryStatus()==null){
            employee.setEntryStatus(1);
        }
        employee.setIsDel(0);
        prepareEmployeeForAdd(employee);
        // 未手动选择直属上级时，默认取部门的分管领导
        fillParentIdFromDeptLeader(employee);
        transferEmployee(employee);
        if(employee.getCreateUserId()==null){
            LoginUserInfo Info= CompanyContext.get();
            employee.setCreateUserId(Info.getUserIdValueL());
        }
        if(employee.getCreateTime()==null){
            employee.setCreateTime(LocalDateTime.now());
        }

        // 钉钉映射前置保证：在职员工必须能在钉钉中按姓名+手机号匹配（查无此人拒绝保存；钉钉服务异常放行为待映射）
        if (employee.getEntryStatus() != null && employee.getEntryStatus() == EmployeeEntryStatus.IN.getValue()) {
            try {
                employee.setDingtalkUserId(approvalSyncService.ensureDingTalkUserId(toMappingProbe(employee.getEmployeeId(),
                        employee.getEmployeeName(), employee.getMobile(), employee.getDingtalkUserId())));
            } catch (com.tianye.hrsystem.common.EmployeeNotInDingTalkException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            } catch (Exception mappingEx) {
                // 钉钉服务暂不可用：放行保存，dingtalk_user_id 留空，由每日重试任务补齐
            }
        }

        save(employee);
        // 直接新建在职员工时记录入职时间节点
        if (employee.getEntryStatus() != null && employee.getEntryStatus() == EmployeeEntryStatus.IN.getValue()) {
            employmentRecordService.recordEntry(employee.getEmployeeId(), employee.getEntryTime(), false);
        }
        if (hasExplicitEmployeeSalaryInput(employeeVO.getSalaryLevel(), employeeVO.getFixedPerformance(), employeeVO.getDutySubsidy(), employeeVO.getOtherSubsidy())) {
            saveEmployeeSalaryDynamicFields(employee.getEmployeeId(), employeeVO.getSalaryLevel(), employeeVO.getFixedPerformance(), employeeVO.getDutySubsidy(), employeeVO.getOtherSubsidy());
        }
        boolean mobileFieldCount = employeeFieldService.query().eq("field_name", "flied_kwbova").eq("label_group", LabelGroupEnum.COMMUNICATION.getValue()).eq("type", FieldTypeEnum.MOBILE.getValue()).exists();
        if (mobileFieldCount) {
            //若是存在自定义手机号码字段，则进行更新
            HrmEmployee hrmEmployee = employeeMapper.selectById(employee.getEmployeeId());
            JSONObject employeeModel = BeanUtil.copyProperties(hrmEmployee, JSONObject.class);
            List<HrmEmployeeData> fieldValueList = employeeDataService.queryListByEmployeeId(employee.getEmployeeId());
            List<InformationFieldVO> communicationInformation = transferInformation(employeeModel,LabelGroupEnum.COMMUNICATION, fieldValueList);
            List<InformationFieldVO> informationFieldVOS = communicationInformation.stream().collect(Collectors.groupingBy(employeeData -> FiledIsFixedEnum.parse(employeeData.getIsFixed()))).get(FiledIsFixedEnum.NO_FIXED);
            String fliedKwbova = "flied_kwbova";
            List<HrmEmployeeData> hrmEmployeeInformationData = informationFieldVOS.stream()
                    .map(field -> {
                        if (fliedKwbova.equals(field.getFieldName()) && field.getType().equals(FieldTypeEnum.MOBILE.getValue())) {
                            field.setFieldValue(employee.getMobile());
                            field.setFieldValueDesc(employee.getMobile());
                        }
                        Object value = field.getFieldValue();
                        if (value == null) {
                            value = "";
                        }
                        field.setFieldValue(employeeFieldService.convertObjectValueToString(field.getType(), field.getFieldValue(), value.toString()));
                        return BeanUtil.copyProperties(field, HrmEmployeeData.class);
                    }).collect(Collectors.toList());
            employeeFieldService.saveEmployeeField(hrmEmployeeInformationData, LabelGroupEnum.COMMUNICATION, employee.getEmployeeId());
        }
        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.PERSONAL, employee.getEmployeeId());


        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());
        operationLog.setOperationInfo(content.getDetail());

        if (employeeVO.getEntryStatus()!=null && employeeVO.getEntryStatus() == 1) {
            abnormalChangeRecordService.addAbnormalChangeRecord(employee.getEmployeeId(), AbnormalChangeType.NEW_ENTRY, LocalDateTime.now());
        }
        //发送通知
//        AdminMessage adminMessage = new AdminMessage();
//        adminMessage.setCreateUser(UserUtil.getUserId());
//        adminMessage.setCreateTime(LocalDateTime.now());
//        adminMessage.setRecipientUser(EmployeeCacheUtil.getUserId(employee.getEmployeeId()));
//        adminMessage.setLabel(8);
//        adminMessage.setType(AdminMessageEnum.HRM_EMPLOYEE_OPEN.getType());
//        messageService.save(adminMessage);

        if (operationLogList.isEmpty()) {
            operationLogList.add(operationLog);
        }
        return operationLogList;
    }

    private void prepareEmployeeForAdd(HrmEmployee employee) {
        if (employee.getCompanyAgeStartTime() == null) {
            employee.setCompanyAgeStartTime(employee.getEntryTime());
        }
    }

    @Override
    public List<SimpleHrmEmployeeVO> queryInspectionAllEmployeeList(String employeeName) {
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.EMPLOYEE_MENU_ID);
        LambdaQueryWrapper<HrmEmployee> wrapper = new QueryWrapper<HrmEmployee>().lambda().select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName, HrmEmployee::getMobile, HrmEmployee::getPost,
                HrmEmployee::getEntryStatus, HrmEmployee::getIsDel, HrmEmployee::getDeptId).eq(HrmEmployee::getIsDel, 0).like(StrUtil.isNotEmpty(employeeName), HrmEmployee::getEmployeeName, employeeName);
        List<HrmEmployee> hrmEmployeeList = this.list(wrapper);
        List<SimpleHrmEmployeeVO> simpleHrmEmployeeVOList = new ArrayList<>();
        for (HrmEmployee employee : hrmEmployeeList) {
            if (employeeIds.contains(employee.getEmployeeId())) {
                simpleHrmEmployeeVOList.add(transferSimpleEmp(employee));
            }
        }
        return simpleHrmEmployeeVOList;
    }

    /**
     * 转换入职员工信息
     *
     * @return
     */
    private HrmEmployee transferEmployee(HrmEmployee employee) {
        if (employee.getIdType() != null && employee.getIdType() == IdTypeEnum.ID_CARD.getValue() && StrUtil.isNotEmpty(employee.getIdNumber())) {
            String idNumber = employee.getIdNumber();
            if (!IdcardUtil.isValidCard(idNumber)) {
                throw new CrmException(HrmCodeEnum.IDENTITY_INFORMATION_IS_ILLEGAL, idNumber);
            }
            employee.setDateOfBirth(IdcardUtil.getBirthDate(idNumber).toLocalDateTime().toLocalDate());
        }
        if (ObjectUtil.isNotNull(employee.getDateOfBirth())) {
            if (employee.getDateOfBirth().isBefore(LocalDate.now())) {
                employee.setAge(DateUtil.ageOfNow(LocalDateTimeUtil.format(employee.getDateOfBirth(), DatePattern.NORM_DATE_PATTERN)));
            }
        }
        if (employee.getEmploymentForms() != null && employee.getEmploymentForms() == EmploymentFormsEnum.OFFICIAL.getValue()) {
            if (ObjectUtil.isEmpty(employee.getProbation())) {
                employee.setProbation(0);
            }
            Integer probation = employee.getProbation();
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
        normalizeEmployeeUniqueFields(employee);
        validateEmployeeUniqueFields(employee, employee.getEmployeeId());
        if (StrUtil.isNotEmpty(employee.getWorkCity()) && BaseUtil.isJSON(employee.getWorkCity())) {
            JSONObject jsonObject = JSONObject.parseObject(employee.getWorkCity());
            StringBuilder workCity = new StringBuilder();
            workCity.append(jsonObject.getString("province"));
            if (StrUtil.isNotEmpty(jsonObject.getString("city"))) {
                workCity.append(jsonObject.getString("city"));
            }
            if (StrUtil.isNotEmpty(jsonObject.getString("area"))) {
                workCity.append(jsonObject.getString("area"));
            }
            employee.setWorkCity(workCity.toString());
        }
        return employee;
    }

    void validateEmployeeUniqueFields(HrmEmployee employee, Long excludeEmployeeId) {
        validateEmployeeUniqueFields(employee, excludeEmployeeId, Collections.emptyList());
    }

    private void validateEmployeeUniqueFields(HrmEmployee employee, Long excludeEmployeeId, Collection<HrmEmployee> additionalEmployees) {
        if (employee == null) {
            return;
        }
        Long currentEmployeeId = excludeEmployeeId != null ? excludeEmployeeId : employee.getEmployeeId();
        String jobNumber = normalizeUniqueText(employee.getJobNumber());
        String mobile = normalizePhone(employee.getMobile());
        String idNumber = normalizeIdNumber(employee.getIdNumber());
        if (StrUtil.isEmpty(jobNumber) && StrUtil.isEmpty(mobile) && StrUtil.isEmpty(idNumber)) {
            return;
        }
        List<HrmEmployee> employees = new ArrayList<>();
        List<HrmEmployee> savedEmployees = list();
        if (CollectionUtil.isNotEmpty(savedEmployees)) {
            employees.addAll(savedEmployees);
        }
        if (CollectionUtil.isNotEmpty(additionalEmployees)) {
            employees.addAll(additionalEmployees);
        }
        if (CollectionUtil.isEmpty(employees)) {
            return;
        }
        for (HrmEmployee existing : employees) {
            if (existing == null || Objects.equals(existing.getIsDel(), 1)) {
                continue;
            }
            Long existingEmployeeId = existing.getEmployeeId();
            if (currentEmployeeId != null && Objects.equals(currentEmployeeId, existingEmployeeId)) {
                continue;
            }
            if (StrUtil.isNotEmpty(jobNumber) && jobNumber.equals(normalizeUniqueText(existing.getJobNumber()))) {
                throw new CrmException(HrmCodeEnum.JOB_NUMBER_EXISTED, jobNumber);
            }
            if (StrUtil.isNotEmpty(mobile) && mobile.equals(normalizePhone(existing.getMobile()))) {
                throw new CrmException(HrmCodeEnum.PHONE_NUMBER_ALREADY_EXISTS, mobile);
            }
            if (StrUtil.isNotEmpty(idNumber) && idNumber.equals(normalizeIdNumber(existing.getIdNumber()))) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "身份证号已存在：" + idNumber);
            }
        }
    }

    private void normalizeEmployeeUniqueFields(HrmEmployee employee) {
        if (employee == null) {
            return;
        }
        if (employee.getJobNumber() != null) {
            employee.setJobNumber(normalizeUniqueText(employee.getJobNumber()));
        }
        if (employee.getMobile() != null) {
            employee.setMobile(normalizePhone(employee.getMobile()));
        }
        if (employee.getIdNumber() != null) {
            employee.setIdNumber(normalizeIdNumber(employee.getIdNumber()));
        }
    }

    private String normalizeUniqueText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeIdNumber(String idNumber) {
        String value = normalizeUniqueText(idNumber);
        return StrUtil.isEmpty(value) ? "" : value.toUpperCase(Locale.ROOT);
    }

    @Override
    public List<SimpleHrmEmployeeVO> queryAllEmployeeList(String employeeName, String month) {
        // 使用Mapper方法进行关联查询
        HrmEmployeeMapper mapper = (HrmEmployeeMapper) getBaseMapper();
        List<Map<String, Object>> resultMapList = mapper.queryEmployeeListForTransfer(employeeName, month);
        
        List<SimpleHrmEmployeeVO> simpleHrmEmployeeVOList = new ArrayList<>();
        for (Map<String, Object> resultMap : resultMapList) {
            SimpleHrmEmployeeVO vo = new SimpleHrmEmployeeVO();
            vo.setEmployeeId(Convert.toLong(resultMap.get("employeeId")));
            vo.setEmployeeName(Convert.toStr(resultMap.get("employeeName"), ""));
            vo.setMobile(Convert.toStr(resultMap.get("mobile"), ""));
            vo.setDeptId(Convert.toLong(resultMap.get("deptId")));
            vo.setDeptName(Convert.toStr(resultMap.get("deptName"), ""));
            vo.setPost(Convert.toStr(resultMap.get("post"), ""));
            
            // 设置员工状态
            Integer entryStatus = resultMap.get("entryStatus") != null ? Convert.toInt(resultMap.get("entryStatus")) : null;
            int status = 1;
            if (entryStatus != null) {
                if (entryStatus.equals(EmployeeEntryStatus.IN.getValue())) {
                    status = 1;  // 在职
                } else if (entryStatus.equals(EmployeeEntryStatus.ALREADY_LEAVE.getValue()) || 
                           entryStatus.equals(EmployeeEntryStatus.TO_IN.getValue())) {
                    status = 2;  // 离职
                }
            }
            vo.setStatus(status);
            
            // 设置计划离职时间
            Object planQuitTimeObj = resultMap.get("planQuitTime");
            if (planQuitTimeObj != null) {
                if (planQuitTimeObj instanceof java.sql.Date) {
                    vo.setPlanQuitTime(((java.sql.Date) planQuitTimeObj).toLocalDate());
                } else if (planQuitTimeObj instanceof java.util.Date) {
                    vo.setPlanQuitTime(new java.sql.Date(((java.util.Date) planQuitTimeObj).getTime()).toLocalDate());
                }
            }
            
            simpleHrmEmployeeVOList.add(vo);
        }
        return simpleHrmEmployeeVOList;
    }

    @Override
    public SimpleHrmEmployeeVO transferSimpleEmp(HrmEmployee employee) {
        SimpleHrmEmployeeVO simpleHrmEmployeeVO = new SimpleHrmEmployeeVO();
        simpleHrmEmployeeVO.setEmployeeId(employee.getEmployeeId());
        simpleHrmEmployeeVO.setEmployeeName(employee.getEmployeeName());
        simpleHrmEmployeeVO.setMobile(employee.getMobile());
        simpleHrmEmployeeVO.setDeptId(employee.getDeptId());
        int status = 1;
        if (employee.getIsDel() == 1) {
            status = 3;
        }
        if (employee.getIsDel() == 0 && employee.getEntryStatus().equals(EmployeeEntryStatus.IN.getValue())) {
            status = 1;
        }
        if (employee.getIsDel() == 0 &&
                (employee.getEntryStatus().equals(EmployeeEntryStatus.ALREADY_LEAVE.getValue()) ||
                        employee.getEntryStatus().equals(EmployeeEntryStatus.TO_IN.getValue()))) {
            status = 2;
        }
        simpleHrmEmployeeVO.setStatus(status);
        simpleHrmEmployeeVO.setPost(employee.getPost());
        if (null != employee.getDeptId()) {
            DeptVO deptVO = hrmDeptService.queryById(employee.getDeptId());
            if (deptVO != null) {
                simpleHrmEmployeeVO.setDeptName(deptVO.getName());
            }
        }
        return simpleHrmEmployeeVO;
    }

    @Override
    public List<SimpleHrmEmployeeVO> queryInEmployeeList() {
        LambdaQueryWrapper<HrmEmployee> wrapper = new QueryWrapper<HrmEmployee>().lambda().select(HrmEmployee::getEmployeeId, HrmEmployee::getDeptId, HrmEmployee::getEmployeeName, HrmEmployee::getPost)
                .eq(HrmEmployee::getEntryStatus, EmployeeEntryStatus.IN.getValue()).eq(HrmEmployee::getIsDel, 0);
        List<HrmEmployee> hrmEmployeeList = this.list(wrapper);
        hrmEmployeeList.forEach(employee -> {
            if (employee.getDeptId() != null) {
                DeptVO deptVO = hrmDeptService.queryById(employee.getDeptId());
                if (deptVO != null) {
                    employee.setDeptName(deptVO.getName());
                }
            }
        });
        return TransferUtil.transferList(hrmEmployeeList, SimpleHrmEmployeeVO.class);
    }

    @Override
    public PersonalInformationVO personalInformation(Long employeeId) {
        ensureChildrenInfoField();
        ensureEmployeeSalaryDynamicFields();
        HrmEmployee hrmEmployee = employeeMapper.selectById(employeeId);
        List<HrmEmployeeData> fieldValueList = employeeDataService.queryListByEmployeeId(employeeId);
        JSONObject employeeModel = BeanUtil.copyProperties(hrmEmployee, JSONObject.class);
        //基本信息
        List<InformationFieldVO> information = transferInformation(employeeModel, LabelGroupEnum.PERSONAL, fieldValueList);
        //通讯信息
        List<InformationFieldVO> communicationInformation = transferInformation(employeeModel, LabelGroupEnum.COMMUNICATION, fieldValueList);
        //教育经历
        List<HrmEmployeeEducationExperience> educationExperienceList = educationExperienceService.lambdaQuery().eq(HrmEmployeeEducationExperience::getEmployeeId, employeeId).list();
        //工作经历
        List<HrmEmployeeWorkExperience> workExperienceList = workExperienceService.lambdaQuery().eq(HrmEmployeeWorkExperience::getEmployeeId, employeeId).list();
        //证书
        List<HrmEmployeeCertificate> certificateList = certificateService.lambdaQuery().eq(HrmEmployeeCertificate::getEmployeeId, employeeId).list();
        //培训经历
        List<HrmEmployeeTrainingExperience> trainingExperienceList = trainingExperienceService.lambdaQuery().eq(HrmEmployeeTrainingExperience::getEmployeeId, employeeId).list();
        //联系人信息

        List<Map<String, Object>> hrmEmployeeContacts = new ArrayList<>();
        List<HrmEmployeeContacts> contactsList = contactsService.lambdaQuery().eq(HrmEmployeeContacts::getEmployeeId, employeeId).list();
        contactsList.forEach(contacts -> {
            QueryWrapper<HrmEmployeeContactsData> eq = Wrappers.<HrmEmployeeContactsData>query().select("field_id", "field_value", "field_value_desc").eq("contacts_id", contacts.getContactsId());
            List<HrmEmployeeContactsData> list = contactsDataService.list(eq);
            //联系人自定义字段值
            List<HrmEmployeeData> contactsFieldValueList = TransferUtil.transferList(list, HrmEmployeeData.class);
            Map<String, Object> hrmEmployeeContact = new HashMap<>();
            hrmEmployeeContact.put("contactsId", contacts.getContactsId());
            hrmEmployeeContact.put("information", transferInformation(BeanUtil.copyProperties(contacts, JSONObject.class), LabelGroupEnum.CONTACT_PERSON, contactsFieldValueList));
            hrmEmployeeContacts.add(hrmEmployeeContact);
        });
        return new PersonalInformationVO(information, communicationInformation, educationExperienceList, workExperienceList, certificateList, hrmEmployeeContacts, trainingExperienceList);
    }


    @Override
    public PersonalInformationVO personalArchives() {
        ensureChildrenInfoField();
        ensureEmployeeSalaryDynamicFields();
        Long employeeId = EmployeeHolder.getEmployeeId();
        HrmEmployee hrmEmployee = employeeMapper.selectById(employeeId);
        List<HrmEmployeeData> fieldValueList = employeeDataService.queryListByEmployeeId(employeeId);
        JSONObject employeeModel = BeanUtil.copyProperties(hrmEmployee, JSONObject.class);
        List<HrmEmployeeField> personalFieldList = employeeFieldService.lambdaQuery().eq(HrmEmployeeField::getIsHidden, 0)
                .eq(HrmEmployeeField::getLabelGroup, LabelGroupEnum.PERSONAL.getValue())
                .eq(HrmEmployeeField::getIsEmployeeVisible, 1)
                .orderByAsc(HrmEmployeeField::getSorting).list();
        //基本信息
        List<InformationFieldVO> information = transferInformation(employeeModel, personalFieldList, fieldValueList);
        List<HrmEmployeeField> communicationFieldList = employeeFieldService.lambdaQuery().eq(HrmEmployeeField::getIsHidden, 0)
                .eq(HrmEmployeeField::getLabelGroup, LabelGroupEnum.COMMUNICATION.getValue())
                .eq(HrmEmployeeField::getIsEmployeeVisible, 1)
                .orderByAsc(HrmEmployeeField::getSorting).list();
        //通讯信息
        List<InformationFieldVO> communicationInformation = transferInformation(employeeModel, communicationFieldList, fieldValueList);
        //教育经历
        List<HrmEmployeeEducationExperience> educationExperienceList = educationExperienceService.lambdaQuery().eq(HrmEmployeeEducationExperience::getEmployeeId, employeeId).list();
        //工作经历
        List<HrmEmployeeWorkExperience> workExperienceList = workExperienceService.lambdaQuery().eq(HrmEmployeeWorkExperience::getEmployeeId, employeeId).list();
        //证书
        List<HrmEmployeeCertificate> certificateList = certificateService.lambdaQuery().eq(HrmEmployeeCertificate::getEmployeeId, employeeId).list();
        //培训经历
        List<HrmEmployeeTrainingExperience> trainingExperienceList = trainingExperienceService.lambdaQuery().eq(HrmEmployeeTrainingExperience::getEmployeeId, employeeId).list();
        //联系人信息
        List<Map<String, Object>> hrmEmployeeContacts = new ArrayList<>();
        List<HrmEmployeeContacts> contactsList = contactsService.lambdaQuery().eq(HrmEmployeeContacts::getEmployeeId, employeeId).list();
        contactsList.forEach(contacts -> {
            QueryWrapper<HrmEmployeeContactsData> eq = Wrappers.<HrmEmployeeContactsData>query().select("field_id", "field_value", "field_value_desc").eq("contacts_id", contacts.getContactsId());
            List<HrmEmployeeContactsData> list = contactsDataService.list(eq);
            //联系人自定义字段值
            List<HrmEmployeeData> contactsFieldValueList = TransferUtil.transferList(list, HrmEmployeeData.class);
            Map<String, Object> hrmEmployeeContact = new HashMap<>();
            hrmEmployeeContact.put("contactsId", contacts.getContactsId());
            hrmEmployeeContact.put("information", transferInformation(BeanUtil.copyProperties(contacts, JSONObject.class), LabelGroupEnum.CONTACT_PERSON, contactsFieldValueList));
            hrmEmployeeContacts.add(hrmEmployeeContact);
        });
        return new PersonalInformationVO(information, communicationInformation, educationExperienceList, workExperienceList, certificateList, hrmEmployeeContacts, trainingExperienceList);
    }


    private void ensureChildrenInfoField() {
        List<HrmEmployeeField> fields = employeeFieldService.lambdaQuery()
                .eq(HrmEmployeeField::getFieldName, EmployeeChildrenInfoFieldFactory.FIELD_NAME)
                .eq(HrmEmployeeField::getLabelGroup, LabelGroupEnum.PERSONAL.getValue())
                .orderByAsc(HrmEmployeeField::getSorting)
                .list();
        HrmEmployeeField field;
        if (CollectionUtil.isEmpty(fields)) {
            field = EmployeeChildrenInfoFieldFactory.createField(EmployeeChildrenInfoFieldFactory.DEFAULT_SORTING);
            field.setFieldId(nextChildrenInfoFieldId());
            employeeFieldService.save(field);
        } else {
            field = fields.get(0);
        }
        ensureChildrenInfoFieldExtends(field.getFieldId());
    }

    private Long nextChildrenInfoFieldId() {
        long fieldId = EmployeeChildrenInfoFieldFactory.DEFAULT_FIELD_ID;
        while (employeeFieldService.getById(fieldId) != null) {
            fieldId++;
        }
        return fieldId;
    }

    private void ensureChildrenInfoFieldExtends(Long fieldId) {
        if (fieldId == null || fieldId > Integer.MAX_VALUE) {
            return;
        }
        List<HrmFieldExtend> existingExtends = fieldExtendMapper.findAllByParentFieldId(Math.toIntExact(fieldId));
        Set<String> existingFieldNames = existingExtends.stream()
                .map(HrmFieldExtend::getFieldName)
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toSet());
        List<HrmFieldExtend> missingExtends = EmployeeChildrenInfoFieldFactory.createFieldExtends(fieldId).stream()
                .filter(fieldExtend -> !existingFieldNames.contains(fieldExtend.getFieldName()))
                .collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(missingExtends)) {
            missingExtends.forEach(fieldExtendMapper::insert);
        }
    }

    private Map<String, HrmEmployeeField> ensureEmployeeSalaryDynamicFields() {
        List<HrmEmployeeField> fields = Optional.ofNullable(employeeFieldService.list()).orElse(Collections.emptyList());
        Map<String, HrmEmployeeField> displayNameFieldMap = buildEmployeeSalaryDisplayNameMap(fields);
        Set<String> fieldNames = fields.stream().map(HrmEmployeeField::getFieldName).filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        int nextSorting = nextEmployeeSalaryFieldSorting(fields);
        List<HrmEmployeeField> missingFields = new ArrayList<>();

        HrmEmployeeField salaryLevelField = displayNameFieldMap.get(SALARY_LEVEL_DISPLAY_NAME);
        if (salaryLevelField == null) {
            salaryLevelField = createEmployeeSalaryField(
                    uniqueEmployeeSalaryFieldName(SALARY_LEVEL_FIELD_NAME, fieldNames),
                    SALARY_LEVEL_DISPLAY_NAME,
                    FieldTypeEnum.TEXT.getValue(),
                    null,
                    nextSorting + missingFields.size()
            );
            missingFields.add(salaryLevelField);
            displayNameFieldMap.put(SALARY_LEVEL_DISPLAY_NAME, salaryLevelField);
        } else {
            ensureEmployeeSalaryFieldDefinition(salaryLevelField, FieldTypeEnum.TEXT.getValue(), null, nextSorting);
        }

        HrmEmployeeField fixedPerformanceField = displayNameFieldMap.get(FIXED_PERFORMANCE_DISPLAY_NAME);
        if (fixedPerformanceField == null) {
            fixedPerformanceField = createEmployeeSalaryField(
                    uniqueEmployeeSalaryFieldName(FIXED_PERFORMANCE_FIELD_NAME, fieldNames),
                    FIXED_PERFORMANCE_DISPLAY_NAME,
                    FieldTypeEnum.DECIMAL.getValue(),
                    2,
                    nextSorting + missingFields.size()
            );
            missingFields.add(fixedPerformanceField);
            displayNameFieldMap.put(FIXED_PERFORMANCE_DISPLAY_NAME, fixedPerformanceField);
        } else {
            ensureEmployeeSalaryFieldDefinition(fixedPerformanceField, FieldTypeEnum.DECIMAL.getValue(), 2, nextSorting);
        }

        HrmEmployeeField dutySubsidyField = displayNameFieldMap.get(DUTY_SUBSIDY_DISPLAY_NAME);
        if (dutySubsidyField == null) {
            dutySubsidyField = createEmployeeSalaryField(
                    uniqueEmployeeSalaryFieldName(DUTY_SUBSIDY_FIELD_NAME, fieldNames),
                    DUTY_SUBSIDY_DISPLAY_NAME,
                    FieldTypeEnum.DECIMAL.getValue(),
                    2,
                    nextSorting + missingFields.size()
            );
            missingFields.add(dutySubsidyField);
            displayNameFieldMap.put(DUTY_SUBSIDY_DISPLAY_NAME, dutySubsidyField);
        } else {
            ensureEmployeeSalaryFieldDefinition(dutySubsidyField, FieldTypeEnum.DECIMAL.getValue(), 2, nextSorting);
        }

        HrmEmployeeField otherSubsidyField = displayNameFieldMap.get(OTHER_SUBSIDY_DISPLAY_NAME);
        if (otherSubsidyField == null) {
            otherSubsidyField = createEmployeeSalaryField(
                    uniqueEmployeeSalaryFieldName(OTHER_SUBSIDY_FIELD_NAME, fieldNames),
                    OTHER_SUBSIDY_DISPLAY_NAME,
                    FieldTypeEnum.DECIMAL.getValue(),
                    2,
                    nextSorting + missingFields.size()
            );
            missingFields.add(otherSubsidyField);
            displayNameFieldMap.put(OTHER_SUBSIDY_DISPLAY_NAME, otherSubsidyField);
        } else {
            ensureEmployeeSalaryFieldDefinition(otherSubsidyField, FieldTypeEnum.DECIMAL.getValue(), 2, nextSorting);
        }

        if (CollectionUtil.isNotEmpty(missingFields)) {
            missingFields.forEach(employeeFieldService::save);
        }
        return displayNameFieldMap;
    }

    private Map<String, HrmEmployeeField> buildEmployeeSalaryDisplayNameMap(List<HrmEmployeeField> fields) {
        Map<String, HrmEmployeeField> displayNameFieldMap = new HashMap<>();
        for (HrmEmployeeField field : fields) {
            if (field == null || StrUtil.isEmpty(field.getName())) {
                continue;
            }
            String displayName = normalizeHeader(field.getName());
            if (!SALARY_LEVEL_DISPLAY_NAME.equals(displayName)
                    && !FIXED_PERFORMANCE_DISPLAY_NAME.equals(displayName)
                    && !DUTY_SUBSIDY_DISPLAY_NAME.equals(displayName)
                    && !OTHER_SUBSIDY_DISPLAY_NAME.equals(displayName)) {
                continue;
            }
            HrmEmployeeField existing = displayNameFieldMap.get(displayName);
            if (existing == null || Objects.equals(field.getLabelGroup(), LabelGroupEnum.PERSONAL.getValue())) {
                displayNameFieldMap.put(displayName, field);
            }
        }
        return displayNameFieldMap;
    }

    private HrmEmployeeField createEmployeeSalaryField(String fieldName, String displayName, Integer type, Integer precision, Integer sorting) {
        HrmEmployeeField field = new HrmEmployeeField();
        field.setFieldName(fieldName);
        field.setName(displayName);
        field.setType(type);
        field.setPrecisions(precision);
        field.setSorting(sorting);
        applyEmployeeSalaryFieldCommonDefinition(field);
        return field;
    }

    private void ensureEmployeeSalaryFieldDefinition(HrmEmployeeField field, Integer expectedType, Integer expectedPrecision, Integer defaultSorting) {
        boolean changed = applyEmployeeSalaryFieldCommonDefinition(field);
        if (!Objects.equals(field.getType(), expectedType)) {
            field.setType(expectedType);
            changed = true;
        }
        if (!Objects.equals(field.getPrecisions(), expectedPrecision)) {
            field.setPrecisions(expectedPrecision);
            changed = true;
        }
        if (field.getSorting() == null) {
            field.setSorting(defaultSorting);
            changed = true;
        }
        if (changed && field.getFieldId() != null) {
            employeeFieldService.updateById(field);
        }
    }

    private boolean applyEmployeeSalaryFieldCommonDefinition(HrmEmployeeField field) {
        boolean changed = false;
        changed |= setFieldValueIfDifferent(field::getComponentType, field::setComponentType, 0);
        changed |= setFieldValueIfDifferent(field::getLabel, field::setLabel, 1);
        changed |= setFieldValueIfDifferent(field::getLabelGroup, field::setLabelGroup, LabelGroupEnum.PERSONAL.getValue());
        changed |= setFieldValueIfDifferent(field::getMaxLength, field::setMaxLength, 255);
        changed |= setFieldValueIfDifferent(field::getIsUnique, field::setIsUnique, 0);
        changed |= setFieldValueIfDifferent(field::getIsNull, field::setIsNull, 0);
        changed |= setFieldValueIfDifferent(field::getIsFixed, field::setIsFixed, 0);
        changed |= setFieldValueIfDifferent(field::getOperating, field::setOperating, 0);
        changed |= setFieldValueIfDifferent(field::getIsHidden, field::setIsHidden, 0);
        changed |= setFieldValueIfDifferent(field::getIsUpdateValue, field::setIsUpdateValue, 1);
        changed |= setFieldValueIfDifferent(field::getIsHeadField, field::setIsHeadField, 0);
        changed |= setFieldValueIfDifferent(field::getIsImportField, field::setIsImportField, 1);
        changed |= setFieldValueIfDifferent(field::getIsEmployeeVisible, field::setIsEmployeeVisible, 1);
        changed |= setFieldValueIfDifferent(field::getIsEmployeeUpdate, field::setIsEmployeeUpdate, 1);
        changed |= setFieldValueIfDifferent(field::getStylePercent, field::setStylePercent, 1);
        return changed;
    }

    private boolean setFieldValueIfDifferent(Supplier<Integer> getter, java.util.function.Consumer<Integer> setter, Integer expectedValue) {
        if (Objects.equals(getter.get(), expectedValue)) {
            return false;
        }
        setter.accept(expectedValue);
        return true;
    }

    private int nextEmployeeSalaryFieldSorting(List<HrmEmployeeField> fields) {
        return fields.stream()
                .filter(field -> field != null && Objects.equals(field.getLabelGroup(), LabelGroupEnum.PERSONAL.getValue()))
                .map(HrmEmployeeField::getSorting)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(999) + 1;
    }

    private String uniqueEmployeeSalaryFieldName(String baseFieldName, Set<String> fieldNames) {
        String fieldName = baseFieldName;
        int index = 1;
        while (fieldNames.contains(fieldName)) {
            fieldName = baseFieldName + "_" + index++;
        }
        fieldNames.add(fieldName);
        return fieldName;
    }

    private void saveEmployeeSalaryDynamicFields(Long employeeId, String salaryLevel, BigDecimal fixedPerformance, BigDecimal dutySubsidy, BigDecimal otherSubsidy) {
        if (employeeId == null) {
            return;
        }
        Map<String, HrmEmployeeField> salaryFields = ensureEmployeeSalaryDynamicFields();
        HrmEmployeeField salaryLevelField = salaryFields.get(SALARY_LEVEL_DISPLAY_NAME);
        HrmEmployeeField fixedPerformanceField = salaryFields.get(FIXED_PERFORMANCE_DISPLAY_NAME);
        HrmEmployeeField dutySubsidyField = salaryFields.get(DUTY_SUBSIDY_DISPLAY_NAME);
        HrmEmployeeField otherSubsidyField = salaryFields.get(OTHER_SUBSIDY_DISPLAY_NAME);
        List<Long> fieldIds = Stream.of(salaryLevelField, fixedPerformanceField, dutySubsidyField, otherSubsidyField)
                .filter(Objects::nonNull)
                .map(HrmEmployeeField::getFieldId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(fieldIds)) {
            employeeDataService.remove(new QueryWrapper<HrmEmployeeData>()
                    .eq("employee_id", employeeId)
                    .in("field_id", fieldIds));
        }

        List<HrmEmployeeData> dataList = new ArrayList<>();
        String salaryLevelValue = StrUtil.trimToEmpty(salaryLevel);
        if (StrUtil.isNotEmpty(salaryLevelValue) && salaryLevelField != null) {
            dataList.add(createEmployeeSalaryData(employeeId, salaryLevelField, salaryLevelValue));
        }
        String fixedPerformanceValue = normalizeEmployeeSalaryDecimalValue(fixedPerformance, FIXED_PERFORMANCE_DISPLAY_NAME);
        if (StrUtil.isNotEmpty(fixedPerformanceValue) && fixedPerformanceField != null) {
            dataList.add(createEmployeeSalaryData(employeeId, fixedPerformanceField, fixedPerformanceValue));
        }
        String dutySubsidyValue = normalizeEmployeeSalaryDecimalValue(dutySubsidy, DUTY_SUBSIDY_DISPLAY_NAME);
        if (StrUtil.isNotEmpty(dutySubsidyValue) && dutySubsidyField != null) {
            dataList.add(createEmployeeSalaryData(employeeId, dutySubsidyField, dutySubsidyValue));
        }
        String otherSubsidyValue = normalizeEmployeeSalaryDecimalValue(otherSubsidy, OTHER_SUBSIDY_DISPLAY_NAME);
        if (StrUtil.isNotEmpty(otherSubsidyValue) && otherSubsidyField != null) {
            dataList.add(createEmployeeSalaryData(employeeId, otherSubsidyField, otherSubsidyValue));
        }
        if (CollectionUtil.isNotEmpty(dataList)) {
            employeeDataService.saveBatch(dataList);
        }
    }

    private boolean hasExplicitEmployeeSalaryInput(String salaryLevel, BigDecimal fixedPerformance, BigDecimal dutySubsidy, BigDecimal otherSubsidy) {
        return StrUtil.isNotBlank(salaryLevel) || fixedPerformance != null || dutySubsidy != null || otherSubsidy != null;
    }

    private HrmEmployeeData createEmployeeSalaryData(Long employeeId, HrmEmployeeField field, String value) {
        HrmEmployeeData data = new HrmEmployeeData();
        data.setEmployeeId(employeeId);
        data.setFieldId(field.getFieldId());
        data.setLabelGroup(field.getLabelGroup());
        data.setName(field.getName());
        data.setType(field.getType());
        data.setFieldValue(value);
        data.setFieldValueDesc(value);
        return data;
    }

    private String convertEmployeeDynamicFieldValue(String name, String fieldName, Integer type, Object fieldValue) {
        if (isFixedPerformanceField(name, fieldName)) {
            return normalizeEmployeeSalaryDecimalValue(fieldValue, FIXED_PERFORMANCE_DISPLAY_NAME);
        }
        if (isDutySubsidyField(name, fieldName)) {
            return normalizeEmployeeSalaryDecimalValue(fieldValue, DUTY_SUBSIDY_DISPLAY_NAME);
        }
        if (isOtherSubsidyField(name, fieldName)) {
            return normalizeEmployeeSalaryDecimalValue(fieldValue, OTHER_SUBSIDY_DISPLAY_NAME);
        }
        Object value = fieldValue == null ? "" : fieldValue;
        return employeeFieldService.convertObjectValueToString(type, fieldValue, value.toString());
    }

    private boolean isFixedPerformanceField(String name, String fieldName) {
        if (FIXED_PERFORMANCE_DISPLAY_NAME.equals(normalizeHeader(name))) {
            return true;
        }
        if (FIXED_PERFORMANCE_FIELD_NAME.equals(fieldName)) {
            return true;
        }
        return "fixedPerformance".equals(fieldName);
    }

    private boolean isDutySubsidyField(String name, String fieldName) {
        if (DUTY_SUBSIDY_DISPLAY_NAME.equals(normalizeHeader(name))) {
            return true;
        }
        if (DUTY_SUBSIDY_FIELD_NAME.equals(fieldName)) {
            return true;
        }
        return "dutySubsidy".equals(fieldName);
    }

    private boolean isOtherSubsidyField(String name, String fieldName) {
        if (OTHER_SUBSIDY_DISPLAY_NAME.equals(normalizeHeader(name))) {
            return true;
        }
        if (OTHER_SUBSIDY_FIELD_NAME.equals(fieldName)) {
            return true;
        }
        return "otherSubsidy".equals(fieldName);
    }

    private void normalizeEmployeeSalaryInformationFields(List<UpdateInformationBO.InformationFieldBO> dataList) {
        if (CollectionUtil.isEmpty(dataList)) {
            return;
        }
        Map<String, HrmEmployeeField> salaryFields = ensureEmployeeSalaryDynamicFields();
        for (UpdateInformationBO.InformationFieldBO field : dataList) {
            HrmEmployeeField definition = resolveEmployeeSalaryInformationField(field, salaryFields);
            if (definition != null) {
                applyEmployeeSalaryInformationFieldDefinition(field, definition);
            }
        }
    }

    private HrmEmployeeField resolveEmployeeSalaryInformationField(UpdateInformationBO.InformationFieldBO field, Map<String, HrmEmployeeField> salaryFields) {
        if (field == null || salaryFields == null) {
            return null;
        }
        if (isSalaryLevelField(field.getName(), field.getFieldName())) {
            return salaryFields.get(SALARY_LEVEL_DISPLAY_NAME);
        }
        if (isFixedPerformanceField(field.getName(), field.getFieldName())) {
            return salaryFields.get(FIXED_PERFORMANCE_DISPLAY_NAME);
        }
        if (isDutySubsidyField(field.getName(), field.getFieldName())) {
            return salaryFields.get(DUTY_SUBSIDY_DISPLAY_NAME);
        }
        if (isOtherSubsidyField(field.getName(), field.getFieldName())) {
            return salaryFields.get(OTHER_SUBSIDY_DISPLAY_NAME);
        }
        return null;
    }

    private boolean isSalaryLevelField(String name, String fieldName) {
        if (SALARY_LEVEL_DISPLAY_NAME.equals(normalizeHeader(name))) {
            return true;
        }
        if (SALARY_LEVEL_FIELD_NAME.equals(fieldName)) {
            return true;
        }
        return "salaryLevel".equals(fieldName);
    }

    private void applyEmployeeSalaryInformationFieldDefinition(UpdateInformationBO.InformationFieldBO field, HrmEmployeeField definition) {
        field.setFieldId(definition.getFieldId());
        field.setLabelGroup(LabelGroupEnum.PERSONAL.getValue());
        field.setFieldName(definition.getFieldName());
        field.setName(definition.getName());
        field.setType(definition.getType());
        field.setIsFixed(0);
    }

    private String normalizeEmployeeSalaryDecimalValue(Object value, String displayName) {
        if (value == null) {
            return "";
        }
        String text = value.toString().trim();
        if (StrUtil.isEmpty(text)) {
            return "";
        }
        try {
            return new BigDecimal(text.replace(",", "")).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ex) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, displayName + "必须为数字，且最多保留两位小数");
        }
    }

    /**
     * 自定义字段基本信息转换
     *
     * @param model
     * @param labelGroupEnum
     * @param fieldValueList
     * @return
     */
    @Override
    public List<InformationFieldVO> transferInformation(JSONObject model, LabelGroupEnum labelGroupEnum, List<HrmEmployeeData> fieldValueList) {
        List<HrmEmployeeField> hrmEmployeeFieldList = employeeFieldService.queryInformationFieldByLabelGroup(labelGroupEnum);
        return transferInformation(model, hrmEmployeeFieldList, fieldValueList);
    }

    @Override
    public List<InformationFieldVO> transferInformation(JSONObject model, List<HrmEmployeeField> hrmEmployeeFieldList
            , List<HrmEmployeeData> fieldValueList) {
        List<InformationFieldVO> InformationFieldVoList = new ArrayList<>();
        Map<Long, HrmEmployeeData> fieldValueMap = new HashMap<>();
        fieldValueList.forEach(field -> fieldValueMap.put(field.getFieldId(), field));
        hrmEmployeeFieldList.forEach(field -> {
            InformationFieldVO fieldVO = BeanUtil.copyProperties(field, InformationFieldVO.class);
            Integer isFixed = fieldVO.getIsFixed();
            Integer type = fieldVO.getType();
            //固定字段
            if (isFixed == 1) {
                String fieldName = StrUtil.toCamelCase(fieldVO.getFieldName());
                Integer componentType = fieldVO.getComponentType();
                fieldVO.setFieldValue(model.get(fieldName) != null ? model.get(fieldName) : "");
                if (componentType == ComponentType.HRM_EMPLOYEE.getValue()) {
                    fieldVO.setFieldValueDesc(model.getLong(fieldName) != null && model.getLong(fieldName) != 0L ?
                            Optional.ofNullable(employeeMapper.selectById(model.getLong(fieldName))).map(HrmEmployee::getEmployeeName).orElse("") : "");
                } else if (componentType == ComponentType.HRM_DEPT.getValue()) {
                    fieldVO.setFieldValueDesc(model.getLong(fieldName) != null && model.getLong(fieldName) != 0L ?
                            Optional.ofNullable(hrmDeptService.getById(model.getLong(fieldName))).map(HrmDept::getName).orElse("") : "");
                } else if (componentType == ComponentType.RECRUIT_CHANNEL.getValue()) {
                    fieldVO.setFieldValueDesc(model.getLong(fieldName) != null && model.getLong(fieldName) != 0 ?
                            Optional.ofNullable(recruitChannelService.getById(model.getLong(fieldName))).map(HrmRecruitChannel::getValue).orElse("") : "");
                } else if (componentType == ComponentType.NO.getValue() && type.equals(FieldTypeEnum.SELECT.getValue())) {
                    Object value = model.get(fieldName);
                    if (value != null) {
                        List<Map<String, Object>> list = JSON.parseObject(fieldVO.getOptions(), List.class);
                        if (ObjectUtil.isNotEmpty(list)) {
                            String keyName = "value";
                            list.forEach(map -> {
                                if (map.get(keyName).equals(value)) {
                                    fieldVO.setFieldValueDesc(map.get("name"));
                                }
                            });
                        } else {
                            fieldVO.setFieldValueDesc(value);
                        }
                        fieldVO.setFieldValue(value);
                    } else {
                        fieldVO.setFieldValueDesc("");
                    }
                } else {
                    if (fieldVO.getType().equals(FieldTypeEnum.DATE.getValue())) {
                        if (ObjectUtil.isNotEmpty(model.get(fieldName))) {
                            if (model.get(fieldName) instanceof LocalDateTime) {
                                fieldVO.setFieldValueDesc(LocalDateTimeUtil.format((LocalDateTime) model.get(fieldName), DatePattern.NORM_DATE_PATTERN));
                                fieldVO.setFieldValue(LocalDateTimeUtil.format((LocalDateTime) model.get(fieldName), DatePattern.NORM_DATE_PATTERN));
                            }
                            if (model.get(fieldName) instanceof LocalDate) {
                                fieldVO.setFieldValueDesc(LocalDateTimeUtil.format((LocalDate) model.get(fieldName), DatePattern.NORM_DATE_PATTERN));
                                fieldVO.setFieldValue(LocalDateTimeUtil.format((LocalDate) model.get(fieldName), DatePattern.NORM_DATE_PATTERN));
                            }
                        } else {
                            fieldVO.setFieldValueDesc("");
                            fieldVO.setFieldValue("");
                        }
                    } else {
                        fieldVO.setFieldValueDesc(model.get(fieldName) != null ? model.get(fieldName) : "");
                    }
                }
            } else {
                if (fieldValueMap.get(fieldVO.getFieldId()) != null) {
                    if (fieldVO.getType().equals(FieldEnum.AREA_POSITION.getType())) {
                        String value = fieldValueMap.get(fieldVO.getFieldId()).getFieldValue();
                        if (StrUtil.isNotEmpty(value)) {
                            String contains = "=";
                            if (value.contains(contains)) {
                                System.out.println(value);

                            } else {
                                fieldVO.setFieldValue(JSON.parseArray(value));
                                fieldVO.setFieldValueDesc(JSON.parseArray(value));
                            }
                        } else {
                            fieldVO.setFieldValue(new ArrayList<>());
                            fieldVO.setFieldValueDesc(new ArrayList<>());
                        }
                    } else if (fieldVO.getType().equals(FieldEnum.CURRENT_POSITION.getType())) {
                        fieldVO.setFieldValue(JSON.parseObject(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue()));
                        fieldVO.setFieldValueDesc(JSON.parseObject(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc()));
                    } else if (fieldVO.getType().equals(FieldEnum.FILE.getType())) {
                        if (StrUtil.isNotEmpty(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue())) {
                            fieldVO.setFieldValue(adminFileService.queryFileList(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue()));
                        } else {
                            fieldVO.setFieldValue(new ArrayList<>());
                        }
                        if (StrUtil.isNotEmpty(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc())) {
                            fieldVO.setFieldValueDesc(adminFileService.queryFileList(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc()));
                        } else {
                            fieldVO.setFieldValueDesc(new ArrayList<>());
                        }
                    } else if (fieldVO.getType().equals(FieldEnum.STRUCTURE.getType())) {
                        if (StrUtil.isNotEmpty(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue())) {
                            fieldVO.setFieldValue(hrmDeptService.querySimpleDeptList(TagUtil.toLongSet(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue())));
                        } else {
                            fieldVO.setFieldValue(new ArrayList<>());
                        }
                        if (StrUtil.isNotEmpty(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc())) {
                            fieldVO.setFieldValueDesc(hrmDeptService.querySimpleDeptList(TagUtil.toLongSet(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc())));
                        } else {
                            fieldVO.setFieldValueDesc(new ArrayList<>());
                        }
                    } else if (fieldVO.getType().equals(FieldEnum.USER.getType())) {
                        if (StrUtil.isNotEmpty(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue())) {
                            fieldVO.setFieldValue(querySimpleEmployeeList(TagUtil.toLongSet(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue())));
                        } else {
                            fieldVO.setFieldValue(new ArrayList<>());
                        }
                        if (StrUtil.isNotEmpty(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc())) {
                            fieldVO.setFieldValueDesc(querySimpleEmployeeList(TagUtil.toLongSet(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc())));
                        } else {
                            fieldVO.setFieldValueDesc(new ArrayList<>());
                        }
                    } else {
                        fieldVO.setFieldValue(fieldValueMap.get(fieldVO.getFieldId()).getFieldValue());
                        fieldVO.setFieldValueDesc(fieldValueMap.get(fieldVO.getFieldId()).getFieldValueDesc());
                    }
                } else {
                    fieldVO.setFieldValue("");
                    fieldVO.setFieldValueDesc("");
                }
            }
            FieldEnum typeEnum = FieldEnum.parse(fieldVO.getType());
            recordToFormType(fieldVO, typeEnum);


            //添加语言包key
            Map<String, String> keyMap = LanguageFieldUtil.getFieldNameKeyMap("name_resourceKey", "customField.hrmField.", fieldVO.getFieldName(), fieldVO.getSetting());
            keyMap.put("fieldValueDesc_resourceKey", "customField.hrmField." + LanguageFieldUtil.getFieldName(fieldVO.getFieldName()) + "Options." + fieldVO.getFieldValueDesc());
            if (CollectionUtil.isNotEmpty(fieldVO.getSetting())) {
                Object first = CollectionUtil.getFirst(fieldVO.getSetting());
                if (NumberUtil.isNumber(LanguageFieldUtil.getSettingValue(first))) {

                } else {
                    keyMap.put("fieldValue_resourceKey", "customField.hrmField." + LanguageFieldUtil.getFieldName(fieldVO.getFieldName()) + "Options." + fieldVO.getFieldValueDesc());
                }
            }

            fieldVO.setLanguageKeyMap(keyMap);
            InformationFieldVoList.add(fieldVO);

        });
        return InformationFieldVoList;
    }

    @Override
    public HrmEmployee queryById(Long employeeId) {
        HrmEmployee employee = getById(employeeId);
        if (ObjectUtil.isNotNull(employee)) {
            hrmDeptService.lambdaQuery().select(HrmDept::getName)
                    .eq(HrmDept::getDeptId, employee.getDeptId()).oneOpt().ifPresent(dept -> employee.setDeptName(dept.getName()));
        } else {
            log.info("当前员工查询为空,Id:{}", employeeId);
        }
        return employee;
    }

    /** 转换为考勤域映射所需的 model.HrmEmployee 探针（仅含 employeeId/姓名/手机号/钉钉userId） */
    private com.tianye.hrsystem.model.HrmEmployee toMappingProbe(Long employeeId, String employeeName,
                                                                 String mobile, String dingTalkUserId) {
        com.tianye.hrsystem.model.HrmEmployee probe = new com.tianye.hrsystem.model.HrmEmployee();
        probe.setEmployeeId(employeeId);
        probe.setEmployeeName(employeeName);
        probe.setMobile(mobile);
        probe.setDingtalkUserId(dingTalkUserId);
        return probe;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog updateInformation(UpdateInformationBO updateInformationBO) {
        Long employeeId = updateInformationBO.getEmployeeId();
        if (employeeId == null) {
            employeeId = EmployeeHolder.getEmployeeId();
        }
        HrmEmployee oldEmployee = queryById(employeeId);
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(oldEmployee.getEmployeeId(), oldEmployee.getEmployeeName());

        List<UpdateInformationBO.InformationFieldBO> dataList = updateInformationBO.getDataList();
        normalizeEmployeeSalaryInformationFields(dataList);
        Map<FiledIsFixedEnum, List<UpdateInformationBO.InformationFieldBO>> isFixedMap = getIsFixedMap(dataList);
        List<UpdateInformationBO.InformationFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(employeeData -> jsonObject.put(employeeData.getFieldName(), employeeData.getFieldValue()));
        HrmEmployee employee = jsonObject.toJavaObject(HrmEmployee.class);
        employee.setEmployeeId(employeeId);
        if (employee.getIdType() != null && employee.getIdType() == IdTypeEnum.ID_CARD.getValue() && StrUtil.isNotEmpty(employee.getIdNumber())) {
            String idNumber = employee.getIdNumber();
            if (!IdcardUtil.isValidCard(idNumber)) {
                throw new CrmException(HrmCodeEnum.IDENTITY_INFORMATION_IS_ILLEGAL, idNumber);
            }
            employee.setDateOfBirth(IdcardUtil.getBirthDate(idNumber).toLocalDateTime().toLocalDate());
        }
        if (ObjectUtil.isNotNull(employee.getDateOfBirth())) {
            if (employee.getDateOfBirth().isBefore(LocalDate.now())) {
                employee.setAge(DateUtil.ageOfNow(LocalDateTimeUtil.format(employee.getDateOfBirth(), DatePattern.NORM_DATE_PATTERN)));
            }
        }
        normalizeEmployeeUniqueFields(employee);
        validateEmployeeUniqueFields(employee, employeeId);
        // 钉钉映射前置保证：姓名变更时重新校验（查无此人拒绝保存；钉钉服务异常放行）
        if (oldEmployee != null && oldEmployee.getEntryStatus() != null
                && oldEmployee.getEntryStatus() == EmployeeEntryStatus.IN.getValue()
                && StrUtil.isNotBlank(employee.getEmployeeName())
                && !employee.getEmployeeName().equals(oldEmployee.getEmployeeName())) {
            try {
                approvalSyncService.ensureDingTalkUserId(toMappingProbe(employeeId,
                        employee.getEmployeeName(), oldEmployee.getMobile(), oldEmployee.getDingtalkUserId()));
            } catch (com.tianye.hrsystem.common.EmployeeNotInDingTalkException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            } catch (Exception mappingEx) {
                // 钉钉服务暂不可用：放行本次修改，待映射状态由每日重试任务处理
            }
        }
        List<UpdateInformationBO.InformationFieldBO> informationFieldBOS = isFixedMap.get(FiledIsFixedEnum.NO_FIXED);
        List<HrmEmployeeData> hrmEmployeeData = informationFieldBOS.stream()
                .map(field -> {
                    Object value = field.getFieldValue();
                    if (value == null) {
                        value = "";
                    }
                    field.setFieldValue(convertEmployeeDynamicFieldValue(field.getName(), field.getFieldName(), field.getType(), field.getFieldValue()));
                    return BeanUtil.copyProperties(field, HrmEmployeeData.class);
                }).collect(Collectors.toList());
        Dict kv = Dict.create().set("key", "employee_id").set("param", "label_group").set("labelGroup", LabelGroupEnum.PERSONAL.getValue()).set("value", employeeId).set("dataTableName", "hrm_employee_data");
        List<HrmModelFiledVO> oldFieldList = ApplicationContextHolder.getBean(IHrmActionRecordService.class).queryFieldValue(kv);
        employeeFieldService.saveEmployeeField(hrmEmployeeData, LabelGroupEnum.PERSONAL, employeeId);
        if (employee.getDateOfBirth() == null) {
            lambdaUpdate().eq(HrmEmployee::getEmployeeId, employeeId).set(HrmEmployee::getDateOfBirth, null).update();
        }
        updateById(employee);
        if (ObjectUtil.isNotNull(oldEmployee)) {
            //固定字段操作记录保存
            Content content = employeeActionRecordService.employeeFixedFieldRecord(BeanUtil.beanToMap(oldEmployee), BeanUtil.beanToMap(employee), LabelGroupEnum.PERSONAL, employeeId);
            //非固定字段操作记录保存

            String[] recprdInfo = content.getDetail().split(",");

            Content content1 = employeeActionRecordService.employeeNOFixedFieldRecord(informationFieldBOS, oldFieldList, employeeId);

            String[] recprdInfo1 = content1.getDetail().split(",");

            String[] updateInfo = ArrayUtil.addAll(recprdInfo, recprdInfo1);
            operationLog.setOperationInfo(JSONUtil.toJsonStr(updateInfo));
        }
        return operationLog;
    }

    private <E> Map<FiledIsFixedEnum, List<E>> getIsFixedMap(List<E> dataList) {
        Map<FiledIsFixedEnum, List<E>> listMap =
                dataList.stream().collect(Collectors.groupingBy(employeeData -> {
                    if (employeeData instanceof UpdateInformationBO.InformationFieldBO) {
                        return FiledIsFixedEnum.parse(((UpdateInformationBO.InformationFieldBO) employeeData).getIsFixed());
                    } else if (employeeData instanceof AddEmployeeFieldManageBO.EmployeeFieldBO) {
                        return FiledIsFixedEnum.parse(((AddEmployeeFieldManageBO.EmployeeFieldBO) employeeData).getIsFixed());
                    }
                    return FiledIsFixedEnum.FIXED;
                }));
        if (!listMap.containsKey(FiledIsFixedEnum.FIXED)) {
            listMap.put(FiledIsFixedEnum.FIXED, new ArrayList<>());
        }
        if (!listMap.containsKey(FiledIsFixedEnum.NO_FIXED)) {
            listMap.put(FiledIsFixedEnum.NO_FIXED, new ArrayList<>());
        }
        return listMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog updateCommunication(UpdateInformationBO updateInformationBO) {
        Long employeeId = updateInformationBO.getEmployeeId();
        if (employeeId == null) {
            employeeId = EmployeeHolder.getEmployeeId();
        }
        HrmEmployee oldEmployee = queryById(employeeId);
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(oldEmployee.getEmployeeId(), oldEmployee.getEmployeeName());


        List<UpdateInformationBO.InformationFieldBO> dataList = updateInformationBO.getDataList();
        Map<FiledIsFixedEnum, List<UpdateInformationBO.InformationFieldBO>> isFixedMap = getIsFixedMap(dataList);
        List<UpdateInformationBO.InformationFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(employeeData -> jsonObject.put(employeeData.getFieldName(), employeeData.getFieldValue()));
        HrmEmployee employee = jsonObject.toJavaObject(HrmEmployee.class);
        employee.setEmployeeId(employeeId);
        normalizeEmployeeUniqueFields(employee);
        validateEmployeeUniqueFields(employee, employeeId);
        // 钉钉映射前置保证：手机号变更时重新校验（查无此人拒绝保存；钉钉服务异常放行）
        if (oldEmployee != null && oldEmployee.getEntryStatus() != null
                && oldEmployee.getEntryStatus() == EmployeeEntryStatus.IN.getValue()
                && StrUtil.isNotBlank(employee.getMobile())
                && !employee.getMobile().equals(oldEmployee.getMobile())) {
            try {
                approvalSyncService.ensureDingTalkUserId(toMappingProbe(employeeId,
                        oldEmployee.getEmployeeName(), employee.getMobile(), oldEmployee.getDingtalkUserId()));
            } catch (com.tianye.hrsystem.common.EmployeeNotInDingTalkException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            } catch (Exception mappingEx) {
                // 钉钉服务暂不可用：放行本次修改，待映射状态由每日重试任务处理
            }
        }
        updateById(employee);
        List<UpdateInformationBO.InformationFieldBO> informationFieldBOS = isFixedMap.get(FiledIsFixedEnum.NO_FIXED);
        List<HrmEmployeeData> hrmEmployeeData = informationFieldBOS.stream()
                .map(field -> {
                    Object value = field.getFieldValue();
                    if (value == null) {
                        value = "";
                    }
                    field.setFieldValue(employeeFieldService.convertObjectValueToString(field.getType(), field.getFieldValue(), value.toString()));
                    return BeanUtil.copyProperties(field, HrmEmployeeData.class);
                }).collect(Collectors.toList());
        Dict set = Dict.create().set("key", "employee_id").set("value", employeeId).set("param", "label_group").set("labelGroup", LabelGroupEnum.CONTACT_PERSON.getValue()).set("dataTableName", "hrm_employee_data");
        List<HrmModelFiledVO> oldFieldList = ApplicationContextHolder.getBean(IHrmActionRecordService.class).queryFieldValue(set);
        employeeFieldService.saveEmployeeField(hrmEmployeeData, LabelGroupEnum.CONTACT_PERSON, employeeId);
        //固定字段操作记录保存
        Content content = employeeActionRecordService.employeeFixedFieldRecord(BeanUtil.beanToMap(oldEmployee), BeanUtil.beanToMap(employee), LabelGroupEnum.CONTACT_PERSON, employeeId);
        //非固定字段操作记录保存
        Content content1 = employeeActionRecordService.employeeNOFixedFieldRecord(informationFieldBOS, oldFieldList, employeeId);
        String[] split = content.getDetail().split(",");
        String[] split1 = content1.getDetail().split(",");
        operationLog.setOperationInfo(JSONUtil.toJsonStr(ArrayUtil.addAll(split, split1)));

        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog addOrUpdateEduExperience(HrmEmployeeEducationExperience educationExperience) {
        OperationLog operationLog = new OperationLog();
        HrmEmployee employee = getById(educationExperience.getEmployeeId());
        if (employee == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "员工不存在，无法保存学历信息");
        }
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        if (educationExperience.getEducationId() == null) {
            Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.EDUCATIONAL_EXPERIENCE, educationExperience.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));
        } else {
            HrmEmployeeEducationExperience old = educationExperienceService.getById(educationExperience.getEducationId());
            Content content = employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.EDUCATIONAL_EXPERIENCE, BeanUtil.beanToMap(old), BeanUtil.beanToMap(educationExperience), educationExperience.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(content.getDetail().split(",")));
        }
        educationExperienceService.saveOrUpdate(educationExperience);
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog deleteEduExperience(Long educationId) {
        HrmEmployeeEducationExperience educationExperience = educationExperienceService.getById(educationId);
        OperationLog operationLog = new OperationLog();
        HrmEmployee employee = getById(educationExperience.getEmployeeId());
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.EDUCATIONAL_EXPERIENCE, educationExperience.getEmployeeId());

        operationLog.setOperationInfo(content.getDetail());
        educationExperienceService.removeById(educationId);
        return operationLog;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog addOrUpdateWorkExperience(HrmEmployeeWorkExperience workExperience) {
        HrmEmployee employee = getById(workExperience.getEmployeeId());

        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        if (workExperience.getWorkExpId() == null) {
            Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.WORK_EXPERIENCE, workExperience.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));
        } else {
            HrmEmployeeWorkExperience old = workExperienceService.getById(workExperience.getWorkExpId());
            Content content = employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.WORK_EXPERIENCE, BeanUtil.beanToMap(old), BeanUtil.beanToMap(workExperience), workExperience.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(content.getDetail().split(",")));
        }
        //TODO 操作记录
        workExperienceService.saveOrUpdate(workExperience);
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog deleteWorkExperience(Long workExpId) {
        HrmEmployeeWorkExperience workExperience = workExperienceService.getById(workExpId);

        HrmEmployee employee = getById(workExperience.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.WORK_EXPERIENCE, workExperience.getEmployeeId());
        operationLog.setOperationInfo(content.getDetail());
        workExperienceService.removeById(workExpId);
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog addOrUpdateCertificate(HrmEmployeeCertificate certificate) {
        HrmEmployee employee = getById(certificate.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        if (certificate.getCertificateId() == null) {
            Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.CERTIFICATE, certificate.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));

        } else {
            HrmEmployeeCertificate old = certificateService.getById(certificate.getCertificateId());
            Content content = employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.CERTIFICATE, BeanUtil.beanToMap(old), BeanUtil.beanToMap(certificate), certificate.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(content.getDetail().split(",")));
        }
        certificateService.saveOrUpdate(certificate);
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog deleteCertificate(Long certificateId) {

        HrmEmployeeCertificate certificate = certificateService.getById(certificateId);
        HrmEmployee employee = getById(certificate.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());
        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.CERTIFICATE, certificate.getEmployeeId());
        operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));
        certificateService.removeById(certificateId);
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog addOrUpdateTrainingExperience(HrmEmployeeTrainingExperience trainingExperience) {

        HrmEmployee employee = getById(trainingExperience.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());
        if (trainingExperience.getTrainingId() == null) {
            Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.TRAINING_EXPERIENCE, trainingExperience.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));
        } else {
            HrmEmployeeTrainingExperience old = trainingExperienceService.getById(trainingExperience.getTrainingId());
            Content content = employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.TRAINING_EXPERIENCE, BeanUtil.beanToMap(old), BeanUtil.beanToMap(trainingExperience), trainingExperience.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(content.getDetail().split(",")));

        }
        trainingExperienceService.saveOrUpdate(trainingExperience);
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog deleteTrainingExperience(Long trainingId) {
        HrmEmployeeTrainingExperience trainingExperience = trainingExperienceService.getById(trainingId);
        HrmEmployee employee = getById(trainingExperience.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());
        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.TRAINING_EXPERIENCE, trainingExperience.getEmployeeId());
        operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));
        trainingExperienceService.removeById(trainingId);
        return operationLog;
    }

    @Override
    public List<HrmEmployeeField> queryContactsAddField() {
        return employeeFieldService.queryInformationFieldByLabelGroup(LabelGroupEnum.CONTACT_PERSON);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog addOrUpdateContacts(UpdateInformationBO updateInformationBO) {
        Long contactsId = updateInformationBO.getContactsId();
        HrmEmployeeContacts oldEmployeeContacts = contactsService.getById(contactsId);
        Long employeeId = updateInformationBO.getEmployeeId();
        List<UpdateInformationBO.InformationFieldBO> fieldList = updateInformationBO.getDataList();
        Map<FiledIsFixedEnum, List<UpdateInformationBO.InformationFieldBO>> isFixedMap = getIsFixedMap(fieldList);
        List<UpdateInformationBO.InformationFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(contactsData -> jsonObject.put(contactsData.getFieldName(), FieldUtil.convertFieldValue(contactsData.getType(), contactsData.getFieldValue(), IsEnum.YES.getValue())));
        HrmEmployeeContacts employeeContacts = jsonObject.toJavaObject(HrmEmployeeContacts.class);
        employeeContacts.setEmployeeId(employeeId);
        employeeContacts.setContactsId(contactsId);

        HrmEmployee employee = getById(employeeContacts.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        if (employeeContacts.getContactsId() == null) {
            Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.CONTACT_PERSON, employeeContacts.getEmployeeId());
            operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));

        } else {
            //固定字段操作记录保存
            Content content = employeeActionRecordService.employeeFixedFieldRecord(BeanUtil.beanToMap(oldEmployeeContacts), BeanUtil.beanToMap(employeeContacts), LabelGroupEnum.CONTACT_PERSON, employeeId);
            operationLog.setOperationInfo(JSONUtil.toJsonStr(content.getDetail().split(",")));
        }
        contactsService.saveOrUpdate(employeeContacts);
        List<UpdateInformationBO.InformationFieldBO> informationFieldBOS = isFixedMap.get(FiledIsFixedEnum.NO_FIXED);
        if (null != informationFieldBOS && informationFieldBOS.size() > 0) {
            List<HrmEmployeeContactsData> hrmEmployeeContactsData = informationFieldBOS.stream()
                    .map(field -> {
                        Object value = field.getFieldValue();
                        if (value == null) {
                            value = "";
                        }
                        field.setFieldValue(employeeFieldService.convertObjectValueToString(field.getType(), field.getFieldValue(), value.toString()));
                        return BeanUtil.copyProperties(field, HrmEmployeeContactsData.class);
                    }).collect(Collectors.toList());
            Dict set = Dict.create().set("key", "contacts_id").set("value", contactsId).set("param", "label_group").set("labelGroup", LabelGroupEnum.CONTACT_PERSON.getValue()).set("dataTableName", "hrm_employee_contacts_data");
            List<HrmModelFiledVO> oldFieldList = ApplicationContextHolder.getBean(IHrmActionRecordService.class).queryFieldValue(set);
            employeeFieldService.saveEmployeeContactsField(hrmEmployeeContactsData, LabelGroupEnum.CONTACT_PERSON, employeeContacts.getContactsId());
            //非固定字段操作记录保存
            employeeActionRecordService.employeeNOFixedFieldRecord(informationFieldBOS, oldFieldList, employeeId);
        }
        return operationLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog deleteContacts(Long contractsId) {
        HrmEmployeeContacts employeeContacts = contactsService.getById(contractsId);

        HrmEmployee employee = getById(employeeContacts.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.CONTACT_PERSON, employeeContacts.getEmployeeId());
        operationLog.setOperationInfo(JSONUtil.toJsonStr(ListUtil.toList(content.getDetail())));
        contactsService.removeById(contractsId);
        contactsDataService.lambdaUpdate().eq(HrmEmployeeContactsData::getContactsId, contractsId).remove();
        return operationLog;
    }

    @Override
    public List<OperationLog> deleteByIds(List<Long> employeeIds) {
        List<HrmEmployee> hrmEmployees = listByIds(employeeIds);

        List<OperationLog> operationLogList = new ArrayList<>();
        for (HrmEmployee hrmEmployee : hrmEmployees) {
            OperationLog operationLog = new OperationLog();
            operationLog.setOperationObject(hrmEmployee.getEmployeeId(), hrmEmployee.getEmployeeName());
            operationLog.setOperationInfo("删除了员工" + hrmEmployee.getEmployeeName());
            operationLogList.add(operationLog);
        }

        lambdaUpdate().set(HrmEmployee::getIsDel, 1).in(HrmEmployee::getEmployeeId, employeeIds).update();
        abnormalChangeRecordService.lambdaUpdate().in(HrmEmployeeAbnormalChangeRecord::getEmployeeId, employeeIds).remove();
        return operationLogList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog change(HrmEmployeeChangeRecord hrmEmployeeChangeRecord) {

        HrmEmployee employee = getById(hrmEmployeeChangeRecord.getEmployeeId());

        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());
        if (hrmEmployeeChangeRecord.getChangeType() != HrmActionBehaviorEnum.BECOME.getValue()) {
            hrmEmployeeChangeRecord.setOldDept(employee.getDeptId());
            hrmEmployeeChangeRecord.setOldParentId(employee.getParentId());
            hrmEmployeeChangeRecord.setOldPost(employee.getPost());
            hrmEmployeeChangeRecord.setOldPostLevel(employee.getPostLevel());
            hrmEmployeeChangeRecord.setOldWorkAddress(employee.getWorkAddress());
        }
        if (LocalDateTimeUtil.toEpochMilli(hrmEmployeeChangeRecord.getEffectTime()) <= System.currentTimeMillis()) {
            //生效时间是当前或者之前 直接修改员工状态
            HrmEmployee hrmEmployee = EmployeeChangeCron.employeeChangeRecord(hrmEmployeeChangeRecord);
            saveOrUpdate(hrmEmployee);
        } else {
            changeRecordService.saveOrUpdate(hrmEmployeeChangeRecord);
        }
        if (hasExplicitEmployeeSalaryInput(hrmEmployeeChangeRecord.getSalaryLevel(), hrmEmployeeChangeRecord.getFixedPerformance(), hrmEmployeeChangeRecord.getDutySubsidy(), hrmEmployeeChangeRecord.getOtherSubsidy())) {
            saveEmployeeSalaryDynamicFields(hrmEmployeeChangeRecord.getEmployeeId(), hrmEmployeeChangeRecord.getSalaryLevel(), hrmEmployeeChangeRecord.getFixedPerformance(), hrmEmployeeChangeRecord.getDutySubsidy(), hrmEmployeeChangeRecord.getOtherSubsidy());
        }
        //添加异动记录
        if (hrmEmployeeChangeRecord.getChangeType() == HrmActionBehaviorEnum.BECOME.getValue()) {
            lambdaUpdate().set(HrmEmployee::getBecomeTime, hrmEmployeeChangeRecord.getEffectTime()).eq(HrmEmployee::getEmployeeId, hrmEmployeeChangeRecord.getEmployeeId())
                    .update();
            abnormalChangeRecordService.addAbnormalChangeRecord(hrmEmployeeChangeRecord.getEmployeeId(), AbnormalChangeType.BECOME, hrmEmployeeChangeRecord.getEffectTime().atStartOfDay());
        } else if (hrmEmployeeChangeRecord.getChangeType() == HrmActionBehaviorEnum.CHANGE_POST.getValue()) {
            abnormalChangeRecordService.addAbnormalChangeRecord(hrmEmployeeChangeRecord.getEmployeeId(), AbnormalChangeType.CHANGE_POST, hrmEmployeeChangeRecord.getEffectTime().atStartOfDay());
        }
        Content content = employeeActionRecordService.changeRecord(hrmEmployeeChangeRecord);
        operationLog.setOperationInfo(content.getDetail());
        return operationLog;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OperationLog> updateInsuranceScheme(UpdateInsuranceSchemeBO updateInsuranceSchemeBO) {
        if (updateInsuranceSchemeBO == null || CollectionUtil.isEmpty(updateInsuranceSchemeBO.getEmployeeIds())) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "请先选择要设置参保方案的员工");
        }
        Long schemeId = updateInsuranceSchemeBO.getSchemeId();
        if (schemeId == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "请选择参保方案");
        }
        HrmInsuranceScheme insuranceScheme = insuranceSchemeService.getById(schemeId);
        if (insuranceScheme == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "参保方案不存在或已删除");
        }
        List<OperationLog> operationLogList = new ArrayList<>();

        updateInsuranceSchemeBO.getEmployeeIds().forEach(employeeId -> {
            HrmEmployee employee = getById(employeeId);
            if (employee == null) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "员工不存在，无法设置参保方案");
            }
            OperationLog operationLog = new OperationLog();
            operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

            Optional<HrmEmployeeSocialSecurityInfo> socialSecurityInfoOpt = securityInfoService.lambdaQuery()
                    .eq(HrmEmployeeSocialSecurityInfo::getEmployeeId, employeeId).oneOpt();
            Long oldSchemeId = null;
            if (socialSecurityInfoOpt.isPresent()) {
                HrmEmployeeSocialSecurityInfo socialSecurityInfo = socialSecurityInfoOpt.get();
                oldSchemeId = socialSecurityInfo.getSchemeId();
                securityInfoService.lambdaUpdate().set(HrmEmployeeSocialSecurityInfo::getSchemeId, schemeId).eq(HrmEmployeeSocialSecurityInfo::getEmployeeId, employeeId).update();
            } else {
                HrmEmployeeSocialSecurityInfo socialSecurityInfo = new HrmEmployeeSocialSecurityInfo();
                socialSecurityInfo.setSchemeId(schemeId);
                socialSecurityInfo.setEmployeeId(employeeId);
                securityInfoService.save(socialSecurityInfo);
            }
            if (oldSchemeId == null || !oldSchemeId.equals(schemeId)) {
                HrmInsuranceScheme oldInsuranceScheme = insuranceSchemeService.getById(oldSchemeId);
                Content content = employeeActionRecordService.updateSchemeRecord(oldInsuranceScheme, insuranceScheme, employee);
                operationLog.setOperationInfo(content.getDetail());
                operationLogList.add(operationLog);
            }
        });
        return operationLogList;
    }

    @Override
    public BasePage<Map<String, Object>> queryPageList(QueryEmployeePageListBO employeePageListBO) {
        // 递归查找选中部门的所有子部门
        if (employeePageListBO.getDeptId() != null) {
            List<Long> allDeptIds = new ArrayList<>();
            allDeptIds.add(employeePageListBO.getDeptId());
            List<HrmDept> allDepts = hrmDeptService.list();
            List<Long> childIds = RecursionUtil.getChildList(allDepts, "parentId", employeePageListBO.getDeptId(), "deptId", "deptId");
            allDeptIds.addAll(childIds);
            employeePageListBO.setDeptIds(allDeptIds);
        }

        List<Long> birthdayEmpList = null;
        int six = 6;
        if (employeePageListBO.getToDoRemind() != null && employeePageListBO.getToDoRemind() == six) {
            birthdayEmpList = queryBirthdayEmp();
        }
        String sortField = StrUtil.isNotEmpty(employeePageListBO.getSortField()) ? StrUtil.toUnderlineCase(employeePageListBO.getSortField()) : null;
        employeePageListBO.setSortField(sortField);
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.EMPLOYEE_MENU_ID);
        BasePage<Map<String, Object>> page = employeeMapper.queryPageList(employeePageListBO.parse(), employeePageListBO, birthdayEmpList, employeeIds);
        page.getList().forEach(map -> {
            fillIdentityBirthdayFields(map);
            Long employeeId = Convert.toLong(map.get("employeeId"));
            List<JSONObject> fieldDatalist = employeeDataService.queryFiledListByEmployeeId(employeeId);
            fieldDatalist.forEach(fieldData -> {
                String fieldName = employeeFieldDataValue(fieldData, "fieldName", "field_name");
                String fieldType = employeeFieldDataValue(fieldData, "type", "type");
                if (StrUtil.isBlank(fieldName) || StrUtil.isBlank(fieldType)) {
                    return;
                }
                Object fieldValue = employeeFieldService.convertValueByFormType(
                        employeeFieldDataValue(fieldData, "fieldValueDesc", "field_value_desc", "fieldValue", "field_value"), FieldEnum.parse(Integer.valueOf(fieldType)));
                map.put(StrUtil.toCamelCase(fieldName), fieldValue);
            });
            fillCompanyAgeFields(map);
        });
        return page;
    }

    private void fillCompanyAgeFields(Map<String, Object> map) {
        String companyAgeStartDate = Convert.toStr(map.get("companyAgeStartTime"));
        if (StrUtil.isBlank(companyAgeStartDate)) {
            companyAgeStartDate = Convert.toStr(map.get("entryTime"));
            if (StrUtil.isNotBlank(companyAgeStartDate)) {
                map.put("companyAgeStartTime", companyAgeStartDate);
            }
        }
        if (StrUtil.isBlank(companyAgeStartDate)) {
            return;
        }
        LocalDate start = LocalDate.parse(companyAgeStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 已离职员工司龄冻结在离职日期,其余算到今天
        LocalDate end = LocalDate.now();
        String quitTime = Convert.toStr(map.get("quitTime"));
        if (StrUtil.isNotBlank(quitTime)) {
            try {
                LocalDate quitDate = LocalDate.parse(quitTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (quitDate.isBefore(end)) {
                    end = quitDate;
                }
            } catch (Exception ignore) {
            }
        }
        if (start.isAfter(end)) {
            map.put("companyAge", "");
            return;
        }
        Period period = Period.between(start, end);
        StringBuilder companyAge = new StringBuilder();
        if (period.getYears() > 0) {
            companyAge.append(period.getYears()).append("年");
        }
        if (period.getMonths() > 0) {
            companyAge.append(period.getMonths()).append("月");
        }
        if (period.getDays() > 0 || companyAge.length() == 0) {
            companyAge.append(period.getDays()).append("天");
        }
        map.put("companyAge", companyAge.toString());
    }

    private void fillIdentityBirthdayFields(Map<String, Object> map) {
        Integer idType = Convert.toInt(map.get("idType"));
        String idNumber = Convert.toStr(map.get("idNumber"));
        if (!Objects.equals(idType, IdTypeEnum.ID_CARD.getValue()) || StrUtil.isBlank(idNumber) || !IdcardUtil.isValidCard(idNumber)) {
            return;
        }
        String dateOfBirth = DateUtil.format(IdcardUtil.getBirthDate(idNumber), DatePattern.NORM_DATE_PATTERN);
        if (ObjectUtil.isEmpty(map.get("dateOfBirth"))) {
            map.put("dateOfBirth", dateOfBirth);
        }
        if (ObjectUtil.isEmpty(map.get("birthday"))) {
            map.put("birthday", dateOfBirth.substring(5, 7) + dateOfBirth.substring(8, 10));
        }
        if (ObjectUtil.isEmpty(map.get("age"))) {
            map.put("age", DateUtil.ageOfNow(dateOfBirth));
        }
    }


    public Map<String, HrmEmployeeField> getEmployeeFieldMap() {
        List<HrmEmployeeField> hrmEmployeeFields = employeeFieldService.lambdaQuery().select(HrmEmployeeField::getName, HrmEmployeeField::getFieldName,
                        HrmEmployeeField::getType, HrmEmployeeField::getComponentType)
                .eq(HrmEmployeeField::getIsHeadField, 1).eq(HrmEmployeeField::getIsFixed, 0).eq(HrmEmployeeField::getIsHidden, 0)
                .list();
        Map<String, HrmEmployeeField> employeeFieldMap = new HashMap<>();
        for (HrmEmployeeField employeeField : hrmEmployeeFields) {
            employeeFieldMap.put(employeeField.getFieldName(), employeeField);
            if (employeeField.getComponentType().equals(ComponentType.ADMIN_USER.getValue()) && !employeeFieldMap.containsKey("dept")) {
                employeeFieldMap.put("dept", null);
            } else if (employeeField.getType().equals(ComponentType.ADMIN_DEPT.getValue()) && !employeeFieldMap.containsKey("user")) {
                employeeFieldMap.put("user", null);
            }
        }
        return employeeFieldMap;
    }

    @Override
    public List<SimpleHrmEmployeeVO> querySimpleEmployeeList(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return new ArrayList<>();
        }
        return lambdaQuery().select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName)
                .in(HrmEmployee::getEmployeeId, employeeIds).eq(HrmEmployee::getIsDel, 0).list()
                .stream().map(employee -> {
                    SimpleHrmEmployeeVO simpleHrmEmployeeVO = new SimpleHrmEmployeeVO();
                    simpleHrmEmployeeVO.setEmployeeId(employee.getEmployeeId());
                    simpleHrmEmployeeVO.setEmployeeName(employee.getEmployeeName());
                    return simpleHrmEmployeeVO;
                }).collect(Collectors.toList());
    }


    @Override
    public Map<Integer, Long> queryEmployeeStatusNum() {
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.EMPLOYEE_MENU_ID);

        List<HrmEmployee> list;
        if (employeeIds != null && employeeIds.size() == 0) {
            list = new ArrayList<>();
        } else {
            list = lambdaQuery().select(HrmEmployee::getStatus)
                    .in(HrmEmployee::getEntryStatus, EmployeeEntryStatus.IN.getValue(), EmployeeEntryStatus.TO_LEAVE.getValue())
                    .in(employeeIds != null, HrmEmployee::getEmployeeId, employeeIds)
                    .eq(HrmEmployee::getIsDel, 0)
                    .list();
        }
        //查询在职状态人数
        TreeMap<Integer, Long> collect = list
                .stream().collect(Collectors.groupingBy(HrmEmployee::getStatus, TreeMap::new, Collectors.counting()));
        for (EmployeeStatusEnum value : EmployeeStatusEnum.values()) {
            if (!collect.containsKey(value.getValue())) {
                collect.put(value.getValue(), 0L);
            }
        }
        //在职
        collect.put(11, (long) list.size());
        if (employeeIds != null && employeeIds.size() == 0) {
            //全职(正式+试用)
            collect.put(12, 0L);
            //待入职
            collect.put(13, 0L);
            //待离职
            collect.put(14, 0L);
            //已离职
            collect.put(15, 0L);
        } else {
            //全职(正式+试用)
            Long fullTimeCount = lambdaQuery().in(HrmEmployee::getStatus, EmployeeStatusEnum.OFFICIAL.getValue(), EmployeeStatusEnum.TRY_OUT.getValue())
                    .in(employeeIds != null, HrmEmployee::getEmployeeId, employeeIds)
                    .eq(HrmEmployee::getIsDel, 0)
                    .in(HrmEmployee::getEntryStatus, EmployeeEntryStatus.IN.getValue(), EmployeeEntryStatus.TO_LEAVE.getValue()).count();
            collect.put(12, fullTimeCount);
            //待入职
            collect.put(13, lambdaQuery().in(employeeIds != null, HrmEmployee::getEmployeeId, employeeIds).in(HrmEmployee::getEntryStatus, EmployeeEntryStatus.TO_IN.getValue()).eq(HrmEmployee::getIsDel, 0).count());
            //待离职
            collect.put(14, lambdaQuery().in(employeeIds != null, HrmEmployee::getEmployeeId, employeeIds).in(HrmEmployee::getEntryStatus, EmployeeEntryStatus.TO_LEAVE.getValue()).eq(HrmEmployee::getIsDel, 0).count());
            //已离职
            collect.put(15, lambdaQuery().in(employeeIds != null, HrmEmployee::getEmployeeId, employeeIds).in(HrmEmployee::getEntryStatus, EmployeeEntryStatus.ALREADY_LEAVE.getValue()).eq(HrmEmployee::getIsDel, 0).count());

        }
        return collect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog againOnboarding(AddEmployeeFieldManageBO addEmployeeFieldManageBO) {
        // 归档离职信息（保留历史，不再物理删除），供入离职履历追溯
        quitInfoService.lambdaUpdate().eq(HrmEmployeeQuitInfo::getEmployeeId, addEmployeeFieldManageBO.getEmployeeId())
                .set(HrmEmployeeQuitInfo::getIsArchived, 1)
                .update();
        // 员工个人信息自定义字段列表
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> employeeFieldList = addEmployeeFieldManageBO.getEmployeeFieldList();
        // 员工岗位自定义字段列表
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> postFieldList = addEmployeeFieldManageBO.getPostFieldList();

        if(employeeFieldList==null)employeeFieldList=new ArrayList<>();
        if(postFieldList==null) postFieldList=new ArrayList<>();
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> employeeDataList = Stream.concat(employeeFieldList.stream(), postFieldList.stream()).collect(Collectors.toList());
        Map<FiledIsFixedEnum, List<AddEmployeeFieldManageBO.EmployeeFieldBO>> isFixedMap = getIsFixedMap(employeeDataList);
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(employeeData -> jsonObject.put(employeeData.getFieldName(), employeeData.getFieldValue()));


        HrmEmployee hrmEmployee = getById(addEmployeeFieldManageBO.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(hrmEmployee.getEmployeeId(), hrmEmployee.getEmployeeName());
        operationLog.setOperationInfo("为" + hrmEmployee.getEmployeeName() + "办理再入职");
        HrmEmployee employee = jsonObject.toJavaObject(HrmEmployee.class);
        employee.setEmployeeId(addEmployeeFieldManageBO.getEmployeeId());
        employee.setEntryStatus(EmployeeEntryStatus.IN.getValue());
        transferEmployee(employee);
        updateById(employee);
        if (hasExplicitEmployeeSalaryInput(addEmployeeFieldManageBO.getSalaryLevel(), addEmployeeFieldManageBO.getFixedPerformance(), addEmployeeFieldManageBO.getDutySubsidy(), addEmployeeFieldManageBO.getOtherSubsidy())) {
            saveEmployeeSalaryDynamicFields(employee.getEmployeeId(), addEmployeeFieldManageBO.getSalaryLevel(), addEmployeeFieldManageBO.getFixedPerformance(), addEmployeeFieldManageBO.getDutySubsidy(), addEmployeeFieldManageBO.getOtherSubsidy());
        }
        abnormalChangeRecordService.addAbnormalChangeRecord(employee.getEmployeeId(), AbnormalChangeType.NEW_ENTRY, LocalDateTime.now());
        // 记录再入职时间节点
        employmentRecordService.recordEntry(employee.getEmployeeId(), employee.getEntryTime(), true);
        return operationLog;
    }

    @Override
    public EmployeeInfo queryEmployeeInfoByMobile(String mobile) {
        return employeeMapper.queryEmployeeInfoByMobile(mobile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLog confirmEntry(AddEmployeeFieldManageBO addEmployeeFieldManageBO) {
        // 员工个人信息自定义字段列表
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> employeeFieldList = addEmployeeFieldManageBO.getEmployeeFieldList();
        // 员工岗位自定义字段列表
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> postFieldList = addEmployeeFieldManageBO.getPostFieldList();
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> employeeDataList = Stream.concat(employeeFieldList.stream(), postFieldList.stream()).collect(Collectors.toList());
        Map<FiledIsFixedEnum, List<AddEmployeeFieldManageBO.EmployeeFieldBO>> isFixedMap = getIsFixedMap(employeeDataList);
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(employeeData -> jsonObject.put(employeeData.getFieldName(), employeeData.getFieldValue()));
        HrmEmployee employee = jsonObject.toJavaObject(HrmEmployee.class);
        employee.setEmployeeId(addEmployeeFieldManageBO.getEmployeeId());
        normalizeEmployeeUniqueFields(employee);
        validateEmployeeUniqueFields(employee, employee.getEmployeeId());
        employee.setEntryStatus(EmployeeEntryStatus.IN.getValue());
        employee.setIsDel(0);
        if (employee.getEmploymentForms() == EmploymentFormsEnum.OFFICIAL.getValue()) {
            Integer probation = employee.getProbation();
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
        updateById(employee);
        abnormalChangeRecordService.addAbnormalChangeRecord(employee.getEmployeeId(), AbnormalChangeType.NEW_ENTRY, LocalDateTime.now());
        // 记录入职（待入职确认）时间节点
        employmentRecordService.recordEntry(employee.getEmployeeId(), employee.getEntryTime(), false);
        HrmEmployee hrmEmployee = getById(addEmployeeFieldManageBO.getEmployeeId());
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(hrmEmployee.getEmployeeId(), hrmEmployee.getEmployeeName());
        operationLog.setOperationInfo("为" + hrmEmployee.getEmployeeName() + "办理入职");

        if (hrmEmployee.getCandidateId() != null) {
            candidateService.lambdaUpdate().set(HrmRecruitCandidate::getStatus, CandidateStatusEnum.HAVE_JOINED.getValue()).set(HrmRecruitCandidate::getEntryTime, new Date())
                    .eq(HrmRecruitCandidate::getCandidateId, hrmEmployee.getCandidateId()).update();
        }
        return operationLog;
    }

    @Override
    public List<HrmEmployeeField> downloadExcelFiled() {
        List<HrmEmployeeField> list = employeeFieldService.lambdaQuery().eq(HrmEmployeeField::getIsImportField, 1).eq(HrmEmployeeField::getIsHidden, 0)
                .list();
        for (HrmEmployeeField hrmEmployeeField : list) {
            if ("channel_id".equals(hrmEmployeeField.getFieldName())) {
                List<HrmRecruitChannel> channelList = recruitChannelService.lambdaQuery().eq(HrmRecruitChannel::getStatus, 1).list();
                List<Map<String, Object>> mapList = new ArrayList<>();
                for (HrmRecruitChannel channel : channelList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", channel.getValue());
                    mapList.add(map);
                }
                hrmEmployeeField.setOptions(JSON.toJSONString(mapList));
            }
        }
        return list.stream().sorted(Comparator.comparingInt(HrmEmployeeField::getSorting)).sorted(Comparator.comparingInt(HrmEmployeeField::getLabel)).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> export(QueryEmployeePageListBO employeePageListBO) {
        List<Map<String, Object>> list = employeeMapper.export(employeePageListBO);
        list.forEach(map -> {
            Long deptId = (Long) map.remove("deptId");
            Integer employmentForms = (Integer) map.remove("employmentForms");
            Integer status = (Integer) map.remove("status");
            Integer idType = (Integer) map.remove("idType");
            HrmDept hrmDept = hrmDeptService.getById(deptId);
            if (hrmDept != null) {
                map.put("deptName", hrmDept.getName());
            } else {
                map.put("deptName", "");
            }
            map.put("employmentForms", EmploymentFormsEnum.parseName(employmentForms));
            map.put("status", EmployeeStatusEnum.parseName(status));
            if (idType == null) {
                map.put("idType", "");
            } else {
                map.put("idType", IdTypeEnum.parseName(idType));
            }
        });
        return list;
    }

    @Override
    public void exportBasicInfoTemplate(QueryEmployeePageListBO employeePageListBO, HttpServletResponse response) throws IOException {
        QueryEmployeePageListBO exportQuery = employeePageListBO == null ? new QueryEmployeePageListBO() : employeePageListBO;
        exportQuery.setPage(1L);
        exportQuery.setPageType(0);
        exportQuery.setLimit(10000L);
        BasePage<Map<String, Object>> page = queryPageList(exportQuery);
        List<Map<String, Object>> employees = page.getList() == null ? Collections.emptyList() : page.getList();
        Map<Long, Map<String, String>> dynamicFields = buildEmployeeBasicInfoDynamicFields(employees);
        List<List<String>> rows = EmployeeBasicInfoExportSupport.buildRows(employees, dynamicFields);
        WebFileUtils.download("员工基础信息.xlsx", EmployeeBasicInfoExportSupport.buildWorkbook(rows), response);
    }

    @Override
    public void exportDepartmentDetail(HttpServletResponse response) throws IOException {
        LoginUserInfo loginUserInfo = CompanyContext.get();
        if (EmployeeDepartmentDetailCrossCompanyExportSupport.isAdministrativeManager(loginUserInfo)) {
            exportCrossCompanyDepartmentDetail(response, loginUserInfo);
            return;
        }
        DepartmentDetailExportData exportData = buildDepartmentDetailExportData();
        String fileName = EmployeeDepartmentDetailExportSupport.buildFileName(exportData.companyName, LocalDate.now());
        WebFileUtils.download(fileName, exportData.workbook, response);
    }

    @Override
    public Map<String, Object> departmentDetailGroupSummary() {
        LoginUserInfo loginUserInfo = CompanyContext.get();
        if (!EmployeeDepartmentDetailCrossCompanyExportSupport.isAdministrativeManager(loginUserInfo)) {
            throw new CrmException(403, "无权访问集团数据");
        }
        dashboardPermissionSupport.assertAnyPermitted(
                "groupSummary:kpi:集团总人数", "groupSummary:kpi:月总固定薪资成本", "groupSummary:kpi:月总绩效薪资成本",
                "groupSummary:kpi:月总社保成本", "groupSummary:kpi:月总公积金成本", "groupSummary:kpi:总成本",
                "groupSummary:chart:companyCost", "groupSummary:chart:headcount",
                "groupSummary:chart:costPie", "groupSummary:chart:deptTop",
                "groupSummary:table:total", "groupSummary:table:dept");
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> companies = new ArrayList<>();
        List<Map<String, Object>> departments = new ArrayList<>();
        BigDecimal totalFixed = BigDecimal.ZERO;
        BigDecimal totalPerformance = BigDecimal.ZERO;
        BigDecimal totalInsurance = BigDecimal.ZERO;
        BigDecimal totalFund = BigDecimal.ZERO;
        int totalCount = 0;
        for (String companyId : EmployeeDepartmentDetailCrossCompanyExportSupport.companyIds()) {
            CompanyDepartmentGroupData data;
            try {
                data = EmployeeDepartmentDetailCrossCompanyExportSupport.withCompanyContext(
                        loginUserInfo, companyId, this::computeCompanyDepartmentGroupData);
            } catch (IOException exception) {
                throw new CrmException(500, "集团数据汇总失败：" + companyId);
            }
            Map<String, Object> company = new LinkedHashMap<>();
            company.put("companyId", companyId);
            company.put("companyName", data.companyName);
            company.put("employeeCount", data.employeeCount);
            company.put("fixedSalaryCost", data.fixedSalaryCost);
            company.put("performanceSalaryCost", data.performanceSalaryCost);
            company.put("corporateInsuranceCost", data.corporateInsuranceCost);
            company.put("corporateProvidentFundCost", data.corporateProvidentFundCost);
            company.put("totalCost", data.totalCost());
            companies.add(company);
            totalCount += data.employeeCount;
            totalFixed = totalFixed.add(data.fixedSalaryCost);
            totalPerformance = totalPerformance.add(data.performanceSalaryCost);
            totalInsurance = totalInsurance.add(data.corporateInsuranceCost);
            totalFund = totalFund.add(data.corporateProvidentFundCost);
            for (Map<String, Object> department : data.departments) {
                Map<String, Object> row = new LinkedHashMap<>(department);
                row.put("companyId", companyId);
                row.put("companyName", data.companyName);
                departments.add(row);
            }
        }
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("companyName", "集团总数");
        total.put("employeeCount", totalCount);
        total.put("fixedSalaryCost", totalFixed);
        total.put("performanceSalaryCost", totalPerformance);
        total.put("corporateInsuranceCost", totalInsurance);
        total.put("corporateProvidentFundCost", totalFund);
        total.put("totalCost", totalFixed.add(totalPerformance).add(totalInsurance).add(totalFund));
        result.put("total", total);
        result.put("companies", companies);
        result.put("departmentDetails", departments);
        Set<String> groupHiddenKpis = dashboardPermissionSupport.hiddenKpis("groupSummary");
        if (groupHiddenKpis != null && !groupHiddenKpis.isEmpty()) {
            Map<String, String> kpiFields = new HashMap<>();
            kpiFields.put("集团总人数", "employeeCount");
            kpiFields.put("月总固定薪资成本", "fixedSalaryCost");
            kpiFields.put("月总绩效薪资成本", "performanceSalaryCost");
            kpiFields.put("月总社保成本", "corporateInsuranceCost");
            kpiFields.put("月总公积金成本", "corporateProvidentFundCost");
            kpiFields.put("总成本", "totalCost");
            stripGroupSummaryKpis(total, groupHiddenKpis, kpiFields);
            for (Map<String, Object> company : companies) {
                stripGroupSummaryKpis(company, groupHiddenKpis, kpiFields);
            }
            for (Map<String, Object> department : departments) {
                stripGroupSummaryKpis(department, groupHiddenKpis, kpiFields);
            }
        }
        return result;
    }

    private void stripGroupSummaryKpis(Map<String, Object> row, Set<String> hidden, Map<String, String> kpiFields) {
        if (row == null) {
            return;
        }
        for (Map.Entry<String, String> entry : kpiFields.entrySet()) {
            if (hidden.contains(entry.getKey())) {
                row.remove(entry.getValue());
            }
        }
    }

    private CompanyDepartmentGroupData computeCompanyDepartmentGroupData() {
        List<Map<String, Object>> employees = employeeMapper.queryDepartmentDetailExportList();
        if (employees == null) {
            employees = Collections.emptyList();
        }
        employees.forEach(this::fillCompanyAgeFields);
        Map<Long, Map<String, String>> dynamicFields = buildEmployeeBasicInfoDynamicFields(employees);
        Map<Long, Map<Integer, String>> salaryOptions = buildDepartmentDetailSalaryOptions(employees);
        fillDepartmentDetailInsuranceCostFields(employees);
        String companyName = departmentDetailCompanyName(resolveTopOrganizationName());

        int employeeCount = employees.size();
        BigDecimal fixedSalaryCost = BigDecimal.ZERO;
        BigDecimal performanceSalaryCost = BigDecimal.ZERO;
        BigDecimal corporateInsuranceCost = BigDecimal.ZERO;
        BigDecimal corporateProvidentFundCost = BigDecimal.ZERO;

        Map<String, Integer> deptCount = new LinkedHashMap<>();
        Map<String, BigDecimal> deptFixed = new LinkedHashMap<>();
        Map<String, BigDecimal> deptPerformance = new LinkedHashMap<>();
        Map<String, BigDecimal> deptInsurance = new LinkedHashMap<>();
        Map<String, BigDecimal> deptFund = new LinkedHashMap<>();

        for (Map<String, Object> employee : employees) {
            Long employeeId = Convert.toLong(employee.get("employeeId"));
            Map<Integer, String> employeeSalaryOptions = salaryOptions == null || employeeId == null
                    ? Collections.emptyMap() : salaryOptions.get(employeeId);
            Map<String, String> employeeDynamicFields = dynamicFields == null || employeeId == null
                    ? Collections.emptyMap() : dynamicFields.get(employeeId);
            BigDecimal fixed = EmployeeDepartmentDetailExportSupport.salaryTreatment(employeeSalaryOptions)
                    .add(departmentDetailDynamicMoney(employeeDynamicFields, "固定绩效", "fixedPerformance", "fixed_performance"))
                    .add(departmentDetailDynamicMoney(employeeDynamicFields, "职务补助", "dutySubsidy", "duty_subsidy"))
                    .add(departmentDetailDynamicMoney(employeeDynamicFields, "其他补助", "otherSubsidy", "other_subsidy"));
            BigDecimal performance = EmployeeDepartmentDetailExportSupport.performanceSalaryCost(employeeSalaryOptions);
            BigDecimal insurance = departmentDetailMoney(employee.get("corporateInsuranceAmount"));
            BigDecimal fund = departmentDetailMoney(employee.get("corporateProvidentFundAmount"));

            fixedSalaryCost = fixedSalaryCost.add(fixed);
            performanceSalaryCost = performanceSalaryCost.add(performance);
            corporateInsuranceCost = corporateInsuranceCost.add(insurance);
            corporateProvidentFundCost = corporateProvidentFundCost.add(fund);

            String departmentName = StrUtil.blankToDefault(
                    Convert.toStr(employee.get("deptName"), "").trim(), "未分配部门");
            deptCount.merge(departmentName, 1, Integer::sum);
            deptFixed.merge(departmentName, fixed, BigDecimal::add);
            deptPerformance.merge(departmentName, performance, BigDecimal::add);
            deptInsurance.merge(departmentName, insurance, BigDecimal::add);
            deptFund.merge(departmentName, fund, BigDecimal::add);
        }

        List<Map<String, Object>> departments = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : deptCount.entrySet()) {
            String departmentName = entry.getKey();
            BigDecimal fixed = deptFixed.get(departmentName);
            BigDecimal performance = deptPerformance.get(departmentName);
            BigDecimal insurance = deptInsurance.get(departmentName);
            BigDecimal fund = deptFund.get(departmentName);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", departmentName);
            row.put("employeeCount", entry.getValue());
            row.put("fixedSalaryCost", fixed);
            row.put("performanceSalaryCost", performance);
            row.put("corporateInsuranceCost", insurance);
            row.put("corporateProvidentFundCost", fund);
            row.put("totalCost", fixed.add(performance).add(insurance).add(fund));
            departments.add(row);
        }

        return new CompanyDepartmentGroupData(
                companyName, employeeCount, fixedSalaryCost, performanceSalaryCost,
                corporateInsuranceCost, corporateProvidentFundCost, departments);
    }

    private static final class CompanyDepartmentGroupData {
        private final String companyName;
        private final int employeeCount;
        private final BigDecimal fixedSalaryCost;
        private final BigDecimal performanceSalaryCost;
        private final BigDecimal corporateInsuranceCost;
        private final BigDecimal corporateProvidentFundCost;
        private final List<Map<String, Object>> departments;

        private CompanyDepartmentGroupData(String companyName,
                                           int employeeCount,
                                           BigDecimal fixedSalaryCost,
                                           BigDecimal performanceSalaryCost,
                                           BigDecimal corporateInsuranceCost,
                                           BigDecimal corporateProvidentFundCost,
                                           List<Map<String, Object>> departments) {
            this.companyName = companyName;
            this.employeeCount = employeeCount;
            this.fixedSalaryCost = safe(fixedSalaryCost);
            this.performanceSalaryCost = safe(performanceSalaryCost);
            this.corporateInsuranceCost = safe(corporateInsuranceCost);
            this.corporateProvidentFundCost = safe(corporateProvidentFundCost);
            this.departments = departments == null ? Collections.emptyList() : departments;
        }

        private BigDecimal totalCost() {
            return fixedSalaryCost.add(performanceSalaryCost)
                    .add(corporateInsuranceCost).add(corporateProvidentFundCost);
        }

        private static BigDecimal safe(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private void exportCrossCompanyDepartmentDetail(HttpServletResponse response,
                                                    LoginUserInfo loginUserInfo) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        List<EmployeeDepartmentDetailGroupSummarySupport.CompanySummary> companySummaries = new ArrayList<>();
        for (String companyId : EmployeeDepartmentDetailCrossCompanyExportSupport.companyIds()) {
            DepartmentDetailExportData exportData = EmployeeDepartmentDetailCrossCompanyExportSupport.withCompanyContext(
                    loginUserInfo, companyId, this::buildDepartmentDetailExportData);
            String companyName = StrUtil.blankToDefault(exportData.companyName, "hr_" + companyId);
            String fileName = EmployeeDepartmentDetailCrossCompanyExportSupport.companyDepartmentFileName(companyName);
            files.put(uniqueArchiveEntryName(files, fileName, companyId), exportData.workbook);
            companySummaries.add(exportData.companySummary);
        }
        files.put(uniqueArchiveEntryName(files, "集团总表.xlsx", "group"),
                EmployeeDepartmentDetailGroupSummarySupport.buildWorkbook("集团总表", companySummaries));
        String password = EmployeeDepartmentDetailCrossCompanyExportSupport.generateArchivePassword();
        byte[] archive = EmployeeDepartmentDetailCrossCompanyExportSupport.buildEncryptedZip(files, password);
        response.setHeader("X-Archive-Password", password);
        WebFileUtils.download("部门明细.zip", archive, response);
    }

    private DepartmentDetailExportData buildDepartmentDetailExportData() throws IOException {
        List<Map<String, Object>> employees = employeeMapper.queryDepartmentDetailExportList();
        if (employees == null) {
            employees = Collections.emptyList();
        }
        employees.forEach(this::fillCompanyAgeFields);
        Map<Long, Map<String, String>> dynamicFields = buildEmployeeBasicInfoDynamicFields(employees);
        Map<Long, Map<Integer, String>> salaryOptions = buildDepartmentDetailSalaryOptions(employees);
        fillDepartmentDetailInsuranceCostFields(employees);
        QuerySalaryBasicVO salaryBasic = salaryBasicService == null ? null : salaryBasicService.findAll();
        if (salaryBasic == null) {
            salaryBasic = new QuerySalaryBasicVO();
        }
        String topOrganizationName = resolveTopOrganizationName();
        String companyName = departmentDetailCompanyName(topOrganizationName);
        byte[] workbook = EmployeeDepartmentDetailExportSupport.buildWorkbook(
                companyName,
                employees,
                dynamicFields,
                salaryOptions,
                salaryBasic.getOrdinaryFullAttendanceAmount(),
                salaryBasic.getLeaderFullAttendanceAmount());
        return new DepartmentDetailExportData(
                companyName,
                workbook,
                buildDepartmentDetailCompanySummary(companyName, employees, dynamicFields, salaryOptions));
    }

    private String departmentDetailCompanyName(String topOrganizationName) {
        if (StrUtil.isNotBlank(topOrganizationName)) {
            return topOrganizationName;
        }
        LoginUserInfo context = CompanyContext.get();
        if (context != null && StrUtil.isNotBlank(context.getCompanyId())) {
            return "hr_" + context.getCompanyId();
        }
        return "";
    }

    private EmployeeDepartmentDetailGroupSummarySupport.CompanySummary buildDepartmentDetailCompanySummary(
            String companyName,
            List<Map<String, Object>> employees,
            Map<Long, Map<String, String>> dynamicFields,
            Map<Long, Map<Integer, String>> salaryOptionsByEmployee) {
        int employeeCount = CollectionUtil.isEmpty(employees) ? 0 : employees.size();
        BigDecimal fixedSalaryCost = BigDecimal.ZERO;
        BigDecimal performanceSalaryCost = BigDecimal.ZERO;
        BigDecimal corporateInsuranceCost = BigDecimal.ZERO;
        BigDecimal corporateProvidentFundCost = BigDecimal.ZERO;
        List<Map<String, Object>> rows = employees == null ? Collections.emptyList() : employees;
        for (Map<String, Object> employee : rows) {
            Long employeeId = Convert.toLong(employee.get("employeeId"));
            Map<Integer, String> salaryOptions = salaryOptionsByEmployee == null || employeeId == null
                    ? Collections.emptyMap()
                    : salaryOptionsByEmployee.get(employeeId);
            Map<String, String> employeeDynamicFields = dynamicFields == null || employeeId == null
                    ? Collections.emptyMap()
                    : dynamicFields.get(employeeId);
            fixedSalaryCost = fixedSalaryCost
                    .add(EmployeeDepartmentDetailExportSupport.salaryTreatment(salaryOptions))
                    .add(departmentDetailDynamicMoney(employeeDynamicFields, "固定绩效", "fixedPerformance", "fixed_performance"))
                    .add(departmentDetailDynamicMoney(employeeDynamicFields, "职务补助", "dutySubsidy", "duty_subsidy"))
                    .add(departmentDetailDynamicMoney(employeeDynamicFields, "其他补助", "otherSubsidy", "other_subsidy"));
            performanceSalaryCost = performanceSalaryCost
                    .add(EmployeeDepartmentDetailExportSupport.performanceSalaryCost(salaryOptions));
            corporateInsuranceCost = corporateInsuranceCost
                    .add(departmentDetailMoney(employee.get("corporateInsuranceAmount")));
            corporateProvidentFundCost = corporateProvidentFundCost
                    .add(departmentDetailMoney(employee.get("corporateProvidentFundAmount")));
        }
        return new EmployeeDepartmentDetailGroupSummarySupport.CompanySummary(
                companyName,
                employeeCount,
                fixedSalaryCost,
                performanceSalaryCost,
                corporateInsuranceCost,
                corporateProvidentFundCost);
    }

    private BigDecimal departmentDetailDynamicMoney(Map<String, String> dynamicFields, String... keys) {
        return departmentDetailMoney(departmentDetailDynamicValue(dynamicFields, keys));
    }

    private String departmentDetailDynamicValue(Map<String, String> dynamicFields, String... keys) {
        if (CollectionUtil.isEmpty(dynamicFields) || keys == null) {
            return "";
        }
        Map<String, String> normalizedFields = new HashMap<>();
        dynamicFields.forEach((key, value) -> normalizedFields.putIfAbsent(normalizeDepartmentDetailKey(key), value));
        for (String key : keys) {
            String value = normalizedFields.get(normalizeDepartmentDetailKey(key));
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String normalizeDepartmentDetailKey(String key) {
        return key == null ? "" : key.replaceAll("\\s+", "").trim();
    }

    private BigDecimal departmentDetailMoney(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        String valueText = Convert.toStr(value, "").trim();
        if (StrUtil.isBlank(valueText)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valueText);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String uniqueArchiveEntryName(Map<String, byte[]> files, String fileName, String suffixToken) {
        String safeName = StrUtil.blankToDefault(fileName, "部门明细.xlsx");
        if (files == null || !files.containsKey(safeName)) {
            return safeName;
        }
        int dotIndex = safeName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? safeName.substring(0, dotIndex) : safeName;
        String extension = dotIndex > 0 ? safeName.substring(dotIndex) : "";
        String suffix = StrUtil.blankToDefault(suffixToken, "重复");
        String candidate = baseName + "-" + suffix + extension;
        for (int index = 2; files.containsKey(candidate); index++) {
            candidate = baseName + "-" + suffix + "-" + index + extension;
        }
        return candidate;
    }

    private static final class DepartmentDetailExportData {
        private final String companyName;
        private final byte[] workbook;
        private final EmployeeDepartmentDetailGroupSummarySupport.CompanySummary companySummary;

        private DepartmentDetailExportData(String companyName,
                                           byte[] workbook,
                                           EmployeeDepartmentDetailGroupSummarySupport.CompanySummary companySummary) {
            this.companyName = companyName;
            this.workbook = workbook;
            this.companySummary = companySummary;
        }
    }

    private Map<Long, Map<Integer, String>> buildDepartmentDetailSalaryOptions(List<Map<String, Object>> employees) {
        Map<Long, Map<Integer, String>> salaryOptionsByEmployee = new HashMap<>();
        if (CollectionUtil.isEmpty(employees)) {
            return salaryOptionsByEmployee;
        }
        List<Long> employeeIds = departmentDetailEmployeeIds(employees);
        if (CollectionUtil.isEmpty(employeeIds)) {
            return salaryOptionsByEmployee;
        }
        Map<Long, Integer> currentIsProByEmployee = new HashMap<>();
        for (Map<String, Object> employee : employees) {
            Long employeeId = Convert.toLong(employee.get("employeeId"));
            if (employeeId != null) {
                currentIsProByEmployee.put(employeeId, resolveCurrentSalaryArchivesIsPro(employee));
            }
        }
        List<HrmSalaryArchivesOption> salaryOptions = salaryArchivesOptionService.lambdaQuery()
                .in(HrmSalaryArchivesOption::getEmployeeId, employeeIds)
                .in(HrmSalaryArchivesOption::getCode, 10101, 10102, 10103, 41001)
                .list();
        for (HrmSalaryArchivesOption salaryOption : salaryOptions) {
            Long employeeId = salaryOption.getEmployeeId();
            Integer expectedIsPro = currentIsProByEmployee.get(employeeId);
            if (expectedIsPro == null || !expectedIsPro.equals(salaryOption.getIsPro())) {
                continue;
            }
            salaryOptionsByEmployee
                    .computeIfAbsent(employeeId, key -> new HashMap<>())
                    .putIfAbsent(salaryOption.getCode(), salaryOption.getValue());
        }
        return salaryOptionsByEmployee;
    }

    private void fillDepartmentDetailInsuranceCostFields(List<Map<String, Object>> employees) {
        Map<Long, HrmInsuranceMonthEmpRecord> insuranceCostMap = buildLatestDepartmentDetailInsuranceCostMap(employees);
        if (CollectionUtil.isEmpty(employees) || CollectionUtil.isEmpty(insuranceCostMap)) {
            return;
        }
        for (Map<String, Object> employee : employees) {
            Long employeeId = Convert.toLong(employee.get("employeeId"));
            HrmInsuranceMonthEmpRecord insuranceRecord = insuranceCostMap.get(employeeId);
            if (insuranceRecord == null) {
                continue;
            }
            employee.put("corporateInsuranceAmount", insuranceRecord.getCorporateInsuranceAmount());
            employee.put("corporateProvidentFundAmount", insuranceRecord.getCorporateProvidentFundAmount());
        }
    }

    private Map<Long, HrmInsuranceMonthEmpRecord> buildLatestDepartmentDetailInsuranceCostMap(List<Map<String, Object>> employees) {
        if (CollectionUtil.isEmpty(employees) || insuranceMonthEmpRecordService == null) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = departmentDetailEmployeeIds(employees);
        if (CollectionUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        Optional<HrmInsuranceMonthEmpRecord> latestRecordOpt = insuranceMonthEmpRecordService.lambdaQuery()
                .select(HrmInsuranceMonthEmpRecord::getYear, HrmInsuranceMonthEmpRecord::getMonth)
                .eq(HrmInsuranceMonthEmpRecord::getStatus, IsEnum.YES.getValue())
                .orderByDesc(HrmInsuranceMonthEmpRecord::getYear)
                .orderByDesc(HrmInsuranceMonthEmpRecord::getMonth)
                .last("limit 1")
                .oneOpt();
        if (!latestRecordOpt.isPresent()) {
            return Collections.emptyMap();
        }
        HrmInsuranceMonthEmpRecord latestRecord = latestRecordOpt.get();
        List<HrmInsuranceMonthEmpRecord> insuranceRecords = insuranceMonthEmpRecordService.lambdaQuery()
                .eq(HrmInsuranceMonthEmpRecord::getYear, latestRecord.getYear())
                .eq(HrmInsuranceMonthEmpRecord::getMonth, latestRecord.getMonth())
                .eq(HrmInsuranceMonthEmpRecord::getStatus, IsEnum.YES.getValue())
                .in(HrmInsuranceMonthEmpRecord::getEmployeeId, employeeIds)
                .list();
        if (CollectionUtil.isEmpty(insuranceRecords)) {
            return Collections.emptyMap();
        }
        return insuranceRecords.stream()
                .filter(record -> record != null && record.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmInsuranceMonthEmpRecord::getEmployeeId, record -> record, (first, second) -> first));
    }

    private List<Long> departmentDetailEmployeeIds(List<Map<String, Object>> employees) {
        if (CollectionUtil.isEmpty(employees)) {
            return Collections.emptyList();
        }
        return employees.stream()
                .map(employee -> Convert.toLong(employee.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Integer resolveCurrentSalaryArchivesIsPro(Map<String, Object> employee) {
        Integer status = Convert.toInt(employee.get("status"), null);
        if (EmployeeStatusEnum.TRY_OUT.getValue() == Convert.toInt(status, -1)) {
            return 1;
        }
        LocalDate becomeTime = parseLocalDate(employee.get("becomeTime"));
        if (becomeTime == null || LocalDate.now().isBefore(becomeTime)) {
            return 1;
        }
        return 0;
    }

    private LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate();
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        String valueText = Convert.toStr(value, "").trim();
        if (StrUtil.isBlank(valueText)) {
            return null;
        }
        if (valueText.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return LocalDate.parse(valueText.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return null;
    }

    private String resolveTopOrganizationName() {
        HrmDept topDept = hrmDeptService.lambdaQuery()
                .select(HrmDept::getName)
                .eq(HrmDept::getParentId, 0L)
                .orderByAsc(HrmDept::getDeptId)
                .last("limit 1")
                .one();
        if (topDept != null && StrUtil.isNotBlank(topDept.getName())) {
            return topDept.getName();
        }
        LoginUserInfo userInfo = CompanyContext.get();
        return userInfo == null ? "" : StrUtil.blankToDefault(userInfo.getCompanyName(), "");
    }

    private Map<Long, Map<String, String>> buildEmployeeBasicInfoDynamicFields(List<Map<String, Object>> employees) {
        Map<Long, Map<String, String>> dynamicFieldValues = new HashMap<>();
        if (CollectionUtil.isEmpty(employees)) {
            return dynamicFieldValues;
        }
        for (Map<String, Object> employee : employees) {
            Long employeeId = Convert.toLong(employee.get("employeeId"));
            if (employeeId == null) {
                employeeId = Convert.toLong(employee.get("employee_id"));
            }
            if (employeeId == null) {
                continue;
            }
            List<JSONObject> fieldDataList = employeeDataService.queryFiledListByEmployeeId(employeeId);
            Map<String, String> employeeDynamicFields = new HashMap<>();
            if (CollectionUtil.isNotEmpty(fieldDataList)) {
                for (JSONObject fieldData : fieldDataList) {
                    String fieldValue = convertEmployeeBasicInfoFieldValue(fieldData);
                    String fieldName = employeeFieldDataValue(fieldData, "fieldName", "field_name");
                    putEmployeeBasicInfoDynamicValue(employeeDynamicFields, fieldName, fieldValue);
                    if (StrUtil.isNotBlank(fieldName)) {
                        putEmployeeBasicInfoDynamicValue(employeeDynamicFields, StrUtil.toCamelCase(fieldName), fieldValue);
                    }
                    putEmployeeBasicInfoDynamicValue(employeeDynamicFields, employeeFieldDataValue(fieldData, "name", "name"), fieldValue);
                }
            }
            dynamicFieldValues.put(employeeId, employeeDynamicFields);
        }
        return dynamicFieldValues;
    }

    private String convertEmployeeBasicInfoFieldValue(JSONObject fieldData) {
        if (fieldData == null) {
            return "";
        }
        String fieldValue = employeeFieldDataValue(fieldData, "fieldValueDesc", "field_value_desc", "fieldValue", "field_value");
        String fieldType = employeeFieldDataValue(fieldData, "type", "type");
        if (StrUtil.isBlank(fieldType)) {
            return Convert.toStr(fieldValue, "");
        }
        Object convertedValue = employeeFieldService.convertValueByFormType(fieldValue, FieldEnum.parse(Integer.valueOf(fieldType)));
        return Convert.toStr(convertedValue, "");
    }

    private String employeeFieldDataValue(JSONObject fieldData, String... keys) {
        if (fieldData == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = fieldData.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private void putEmployeeBasicInfoDynamicValue(Map<String, String> dynamicFields, String key, String value) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(value)) {
            return;
        }
        dynamicFields.putIfAbsent(key, value);
        dynamicFields.putIfAbsent(EmployeeBasicInfoExportSupport.normalizeKey(key), value);
    }

    @Override
    public Integer queryFieldValueNoDelete(List<HrmEmployeeField> uniqueList) {
        return employeeMapper.queryFieldValueNoDelete(uniqueList);
    }

    @Override
    public List<Long> queryToInByMonth(int year, int month) {
        return employeeMapper.queryToInByMonth(year, month);
    }

    @Override
    public List<Long> queryToLeaveByMonth(int year, int month) {
        return employeeMapper.queryToLeaveByMonth(year, month);
    }

    @Override
    public List<Long> queryToCorrectCount() {
        return employeeMapper.queryToCorrectCount();
    }


    @Override
    public List<Long> queryBirthdayEmp() {
        DateTime start = DateUtil.beginOfMonth(new Date());
        DateTime end = DateUtil.endOfMonth(new Date());
        DateRange dateTimes = new DateRange(start, end, DateField.DAY_OF_YEAR);
        List<String> lunarList = new ArrayList<>();
        List<String> solarList = new ArrayList<>();
        dateTimes.forEach(dateTime -> {
            String[] strings = queryBirthdayTime(dateTime);
            lunarList.add(strings[0]);
            solarList.add(strings[1]);
        });
        return employeeMapper.queryBirthdayEmp(lunarList, solarList);
    }

    @Override
    public List<HrmEmployee> queryBirthdayListByTime(LocalDate time, Collection<Long> employeeIds) {
        String[] strings = queryBirthdayTime(Timestamp.valueOf(time.atStartOfDay()));
        return employeeMapper.queryBirthdayListByTime(strings[0], strings[1], employeeIds);
    }

    @Override
    public List<HrmEmployee> queryEntryEmpListByTime(LocalDate time, Collection<Long> employeeIds) {
        return employeeMapper.queryEntryEmpListByTime(time, employeeIds);
    }

    private String[] queryBirthdayTime(Date date) {
        ChineseDate chineseDate = new ChineseDate(date);
        String lunarMonth = String.valueOf(chineseDate.getMonth());
        String lunarDay = String.valueOf(chineseDate.getDay());
        if (lunarMonth.length() == 1) {
            lunarMonth = "0" + lunarMonth;
        }
        if (lunarDay.length() == 1) {
            lunarDay = "0" + lunarDay;
        }
        LocalDate localDate = LocalDate.parse(DateUtil.format(date, "yyyy-MM-dd"));
        String solarMonth = String.valueOf(localDate.getMonthValue());
        String solarDay = String.valueOf(localDate.getDayOfMonth());
        if (solarMonth.length() == 1) {
            solarMonth = "0" + solarMonth;
        }
        if (solarDay.length() == 1) {
            solarDay = "0" + solarDay;
        }
        String lunarBirthday = lunarMonth + lunarDay;
        String solarBirthday = solarMonth + solarDay;
        return new String[]{lunarBirthday, solarBirthday};
    }

    @Override
    public List<HrmEmployee> queryBecomeEmpListByTime(LocalDate time, Collection<Long> employeeIds) {
        return employeeMapper.queryBecomeEmpListByTime(time, employeeIds);
    }

    @Override
    public List<HrmEmployee> queryLeaveEmpListByTime(LocalDate time, Collection<Long> employeeIds) {
        return employeeMapper.queryLeaveEmpListByTime(time, employeeIds);
    }

    @Override
    public DeptEmployeeListVO queryDeptEmployeeList(Long deptId) {
        DeptEmployeeListVO deptEmployeeListVO = new DeptEmployeeListVO();
        List<DeptEmployeeVO> deptList = hrmDeptService.queryDeptEmployeeList();
        createTree(deptId, deptList);
        List<SimpleHrmEmployeeVO> employeeList = employeeMapper.querySimpleEmpByDeptId(deptId);
        List<DeptEmployeeVO> collect = deptList.stream().filter(dept -> dept.getParentId().equals(deptId)).collect(Collectors.toList());
        deptEmployeeListVO.setDeptList(collect);
        deptEmployeeListVO.setEmployeeList(employeeList);
        return deptEmployeeListVO;
    }


    @Override
    public DeptEmployeeListVO queryInspectionDeptEmployeeList(Long deptId) {
        Integer dataAuthType = DataAuthEnum.ALL.getValue();
        Collection<Long> deptIds = null;
        if (!EmployeeHolder.isHrmAdmin() && !DataAuthEnum.ALL.getValue().equals(dataAuthType)) {
            deptIds = employeeUtil.queryDataAuthDeptId(dataAuthType);
        } else {
            List<HrmDept> hrmDeptList = hrmDeptService.list();
            if (CollectionUtil.isNotEmpty(hrmDeptList)) {
                deptIds = hrmDeptList.stream().map(HrmDept::getDeptId).collect(Collectors.toList());
            }
        }
        DeptEmployeeListVO deptEmployeeListVO = new DeptEmployeeListVO();
        //查询所有部门下的所有员工列表
        List<DeptEmployeeVO> deptList = hrmDeptService.queryDeptEmployeeList();
        //查询当前部门下的员工列表
        List<SimpleHrmEmployeeVO> employeeList = employeeMapper.querySimpleEmpByDeptId(deptId);


        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.CREATE_ACHIEVEMENT_SETTING_MENU_ID);
        List<SimpleHrmEmployeeVO> empFilterList = employeeList.stream().filter(employeeVO -> {
            return employeeIds.contains(employeeVO.getEmployeeId());
        }).collect(Collectors.toList());

        //查询指定部门的下级部门并返回树结构
        List<DeptEmployeeVO> treeDept = createTreeInspection(deptId, deptList);
        DeptEmployeeVO root = new DeptEmployeeVO();

        DeptEmployeeVO node = new DeptEmployeeVO();
        node.setChildren(treeDept);
        //上一步得到的部门树结构是没有经过权限过滤的，这一步对权限进行过滤
        if (CollUtil.isNotEmpty(treeDept)) {
            filterNode(root, node, deptIds);
        }
//        createTree(deptId, treeDept);
        deptEmployeeListVO.setDeptList(root.getChildren());
        deptEmployeeListVO.setEmployeeList(empFilterList);

        return deptEmployeeListVO;
    }

    private List<DeptEmployeeVO> createTree(Long pid, List<DeptEmployeeVO> deptList) {
        List<DeptEmployeeVO> treeDept = new ArrayList<>();
        for (DeptEmployeeVO dept : deptList) {
            if (pid.equals(dept.getParentId())) {
                treeDept.add(dept);
                List<DeptEmployeeVO> children = createTree(dept.getDeptId(), deptList);
                if (CollUtil.isNotEmpty(children)) {
                    for (DeptEmployeeVO child : children) {
                        dept.setAllNum(dept.getAllNum() + child.getAllNum());
                    }
                    dept.setHasChildren(1);
                } else {
                    dept.setHasChildren(0);
                }
            }
        }
        return treeDept;
    }


    private List<DeptEmployeeVO> createTreeInspection(Long pid, List<DeptEmployeeVO> deptList) {
        List<DeptEmployeeVO> treeDept = new ArrayList<>();
        for (DeptEmployeeVO dept : deptList) {
            if (pid.equals(dept.getParentId())) {
                treeDept.add(dept);
                List<DeptEmployeeVO> children = createTreeInspection(dept.getDeptId(), deptList);
                dept.setChildren(children);
            }
        }
        return treeDept;
    }


    /**
     * 树结构根据指定部门重组
     *
     * @param result      返回接过
     * @param node        全部树结构
     * @param findDeptIds 需要筛选的部门id
     */
    private static void filterNode(DeptEmployeeVO result, DeptEmployeeVO node, Collection<Long> findDeptIds) {
        List<DeptEmployeeVO> childList = node.getChildren();
        if (findDeptIds.contains(node.getDeptId())) {
            DeptEmployeeVO newNode = BeanUtil.copyProperties(node, DeptEmployeeVO.class);
            newNode.setChildren(null);
            if (CollUtil.isEmpty(result.getChildren())) {
                result.setChildren(ListUtil.toList(newNode));
            } else {
                List<DeptEmployeeVO> childList1 = result.getChildren();
                childList1.add(newNode);
            }
            findDeptIds.remove(node.getDeptId());
            if (CollUtil.isEmpty(childList)) {
                return;
            }
            for (DeptEmployeeVO deptVO : childList) {
                filterNode(newNode, deptVO, findDeptIds);
            }
        } else {
            if (CollUtil.isEmpty(childList)) {
                return;
            }
            for (DeptEmployeeVO deptVO : childList) {
                filterNode(result, deptVO, findDeptIds);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OperationLog> adminAddEmployee(List<AddEmployeeBO> employeeBOS) {
        List<OperationLog> operationLogList = new ArrayList<>();
        for (AddEmployeeBO employeeBO : employeeBOS) {
            employeeBO.setEntryStatus(EmployeeEntryStatus.IN.getValue());
            operationLogList.addAll(add(employeeBO));
        }
        return operationLogList;
    }

    @Override
    public Set<SimpleHrmEmployeeVO> queryDeptUserListByUser(DeptUserListByUserBO deptUserListByUserBO) {
        Set<SimpleHrmEmployeeVO> employeeVOS = new HashSet<>();
        if (CollUtil.isNotEmpty(deptUserListByUserBO.getDeptIdList())) {
            List<HrmEmployee> userList = findChildUserList(deptUserListByUserBO.getDeptIdList());
            List<SimpleHrmEmployeeVO> hrmSimpleEmpVOS = TransferUtil.transferList(userList, SimpleHrmEmployeeVO.class);
            employeeVOS.addAll(hrmSimpleEmpVOS);
        }
        if (CollUtil.isNotEmpty(deptUserListByUserBO.getEmployeeIdList())) {
            List<HrmEmployee> userList = query().select("employee_id", "employee_name", "post", "sex", "mobile", "email").in("employee_id", deptUserListByUserBO.getEmployeeIdList())
                    .eq("is_del", 0).in("entry_status", 1, 3).list();
            List<SimpleHrmEmployeeVO> hrmSimpleUserVOS = TransferUtil.transferList(userList, SimpleHrmEmployeeVO.class);
            employeeVOS.addAll(hrmSimpleUserVOS);
        }
        return employeeVOS;
    }

    private List<HrmEmployee> findChildUserList(List<Long> deptIds) {
        List<HrmEmployee> empList = new ArrayList<>();
        for (Long deptId : deptIds) {
            List<HrmEmployee> list = query().select("employee_id", "employee_name", "post", "sex", "mobile", "email").eq("dept_id", deptId)
                    .in("entry_status", 1, 3).eq("is_del", 0).list();
            empList.addAll(list);
            List<HrmDept> childList = hrmDeptService.lambdaQuery().select(HrmDept::getDeptId).eq(HrmDept::getParentId, deptId).list();
            if (CollUtil.isNotEmpty(childList)) {
                List<Long> childDeptIds = childList.stream().map(HrmDept::getDeptId).collect(Collectors.toList());
                empList.addAll(findChildUserList(childDeptIds));
            }
        }
        return empList;
    }

    @Override
    public Set<Long> filterDeleteEmployeeIds(Set<Long> employeeIds) {
        return employeeMapper.filterDeleteEmployeeIds(employeeIds);
    }

    @Override
    public List<SimpleHrmEmployeeVO> queryAllSimpleEmployeeList(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return new ArrayList<>();
        }
        List<HrmEmployee> list = lambdaQuery().select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName, HrmEmployee::getEntryStatus, HrmEmployee::getIsDel)
                .in(HrmEmployee::getEmployeeId, employeeIds).list();
        return list.stream().map(this::transferSimpleEmp).collect(Collectors.toList());
    }

    @Override
    public Set<Long> queryChildEmployeeId(List<Long> employeeIds) {
        Set<Long> result = new HashSet<>(employeeIds);
        if (CollUtil.isNotEmpty(employeeIds)) {
            employeeIds.forEach(employeeId -> {
                List<Long> collect = lambdaQuery().select(HrmEmployee::getEmployeeId).eq(HrmEmployee::getParentId, employeeId).list()
                        .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
                result.addAll(queryChildEmployeeId(collect));
            });
        }
        return result;
    }

    @Override
    public Set<String> queryEntryStatusList(QueryNotesStatusBO queryNotesStatusBO, Collection<Long> employeeIds) {
        return employeeMapper.queryEntryStatusList(queryNotesStatusBO, employeeIds);
    }

    @Override
    public Set<String> queryBecomeStatusList(QueryNotesStatusBO queryNotesStatusBO, Collection<Long> employeeIds) {
        return employeeMapper.queryBecomeStatusList(queryNotesStatusBO, employeeIds);
    }

    @Override
    public Set<String> queryLeaveStatusList(QueryNotesStatusBO queryNotesStatusBO, Collection<Long> employeeIds) {
        return employeeMapper.queryLeaveStatusList(queryNotesStatusBO, employeeIds);
    }

    @Override
    public List<HrmModelFiledVO> queryEmployeeField(Integer entryStatus) {
        List<HrmModelFiledVO> filedVOS = employeeFieldService.queryField(entryStatus);
        return filedVOS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OperationLog> addEmployeeField(AddEmployeeFieldManageBO addEmployeeFieldManageBO) {
        String jobNumber = addEmployeeFieldManageBO.getJobNumber();
        if (StrUtil.isNotEmpty(jobNumber)) {
            boolean exists = lambdaQuery().eq(HrmEmployee::getIsDel, 0).eq(HrmEmployee::getJobNumber, jobNumber).exists();
            if (exists) {
                throw new CrmException(HrmCodeEnum.JOB_NUMBER_EXISTED, jobNumber);
            }
        }


        List<OperationLog> operationLogList = new ArrayList<>();

        if (addEmployeeFieldManageBO.getCandidateId() != null) {
            if (addEmployeeFieldManageBO.getEntryStatus() == 1) {
                UpdateCandidateStatusBO updateCandidateStatusBO = new UpdateCandidateStatusBO();
                updateCandidateStatusBO.setCandidateIds(Collections.singletonList(addEmployeeFieldManageBO.getCandidateId()));
                updateCandidateStatusBO.setStatus(CandidateStatusEnum.HAVE_JOINED.getValue());
                operationLogList.addAll(candidateActionRecordService.updateCandidateStatusRecord(updateCandidateStatusBO));
                candidateService.lambdaUpdate().set(HrmRecruitCandidate::getStatus, CandidateStatusEnum.HAVE_JOINED.getValue())
                        .set(HrmRecruitCandidate::getStatusUpdateTime, new Date())
                        .set(HrmRecruitCandidate::getEntryTime, new Date())
                        .eq(HrmRecruitCandidate::getCandidateId, addEmployeeFieldManageBO.getCandidateId()).update();

            } else {
                UpdateCandidateStatusBO updateCandidateStatusBO = new UpdateCandidateStatusBO();
                updateCandidateStatusBO.setCandidateIds(Collections.singletonList(addEmployeeFieldManageBO.getCandidateId()));
                updateCandidateStatusBO.setStatus(CandidateStatusEnum.PENDING_ENTRY.getValue());
                operationLogList.addAll(candidateActionRecordService.updateCandidateStatusRecord(updateCandidateStatusBO));
                candidateService.lambdaUpdate().set(HrmRecruitCandidate::getStatus, CandidateStatusEnum.PENDING_ENTRY.getValue())
                        .set(HrmRecruitCandidate::getStatusUpdateTime, new Date())
                        .eq(HrmRecruitCandidate::getCandidateId, addEmployeeFieldManageBO.getCandidateId()).update();
            }

        }
        // 员工个人信息自定义字段列表
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> employeeFieldList = addEmployeeFieldManageBO.getEmployeeFieldList();
        // 员工岗位自定义字段列表
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> postFieldList = addEmployeeFieldManageBO.getPostFieldList();
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> employeeDataList = Stream.concat(employeeFieldList.stream(), postFieldList.stream()).collect(Collectors.toList());
        Map<FiledIsFixedEnum, List<AddEmployeeFieldManageBO.EmployeeFieldBO>> isFixedMap = getIsFixedMap(employeeDataList);
        List<AddEmployeeFieldManageBO.EmployeeFieldBO> fixedEmployeeData = isFixedMap.get(FiledIsFixedEnum.FIXED);
        JSONObject jsonObject = new JSONObject();
        fixedEmployeeData.forEach(employeeData -> jsonObject.put(employeeData.getFieldName(), employeeData.getFieldValue()));
        HrmEmployee employee = jsonObject.toJavaObject(HrmEmployee.class);
        if (addEmployeeFieldManageBO.getCandidateId() != null) {
            HrmRecruitCandidate candidate = candidateService.getById(addEmployeeFieldManageBO.getCandidateId());
            employee.setChannelId(candidate.getChannelId());
            employee.setCandidateId(addEmployeeFieldManageBO.getCandidateId());
        }
        employee.setEntryStatus(addEmployeeFieldManageBO.getEntryStatus());
        if (null == employee.getCompanyAgeStartTime()) {
            employee.setCompanyAgeStartTime(employee.getEntryTime());
        }
        transferEmployee(employee);
        save(employee);
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

        List<HrmEmployeeData> hrmEmployeeData = isFixedMap.get(FiledIsFixedEnum.NO_FIXED).stream()
                .map(field -> {
                    Object value = field.getFieldValue();
                    if (value == null) {
                        value = "";
                    }
                    field.setFieldValue(convertEmployeeDynamicFieldValue(field.getName(), field.getFieldName(), field.getType(), field.getFieldValue()));
                    return BeanUtil.copyProperties(field, HrmEmployeeData.class);
                }).collect(Collectors.toList());
        if (!hrmEmployeeData.isEmpty()) {
            List<HrmEmployeeData> personalData = hrmEmployeeData.stream().filter(employeeData -> employeeData.getLabelGroup().equals(LabelGroupEnum.PERSONAL.getValue())).collect(Collectors.toList());
            employeeFieldService.saveEmployeeField(personalData, LabelGroupEnum.PERSONAL, employee.getEmployeeId());
            List<HrmEmployeeData> postData = hrmEmployeeData.stream().filter(employeeData -> employeeData.getLabelGroup().equals(LabelGroupEnum.POST.getValue())).collect(Collectors.toList());
            employeeFieldService.saveEmployeeField(postData, LabelGroupEnum.POST, employee.getEmployeeId());
        }
        boolean exists = employeeFieldService.query().eq("field_name", "flied_kwbova").eq("label_group", LabelGroupEnum.COMMUNICATION.getValue()).eq("type", FieldTypeEnum.MOBILE.getValue()).exists();
        if (exists) {
            //若是存在自定义手机号码字段，则进行更新
            HrmEmployee hrmEmployee = employeeMapper.selectById(employee.getEmployeeId());
            JSONObject employeeModel = BeanUtil.copyProperties(hrmEmployee, JSONObject.class);
            List<HrmEmployeeData> fieldValueList = employeeDataService.queryListByEmployeeId(employee.getEmployeeId());
            List<InformationFieldVO> communicationInformation = transferInformation(employeeModel, LabelGroupEnum.COMMUNICATION, fieldValueList);
            List<InformationFieldVO> informationFieldVOS = communicationInformation.stream().collect(Collectors.groupingBy(employeeData -> FiledIsFixedEnum.parse(employeeData.getIsFixed()))).get(FiledIsFixedEnum.NO_FIXED);
            String fliedKwbova = "flied_kwbova";
            List<HrmEmployeeData> hrmEmployeeInformationData = informationFieldVOS.stream()
                    .map(field -> {
                        if (fliedKwbova.equals(field.getFieldName()) && field.getType().equals(FieldTypeEnum.MOBILE.getValue())) {
                            field.setFieldValue(employee.getMobile());
                            field.setFieldValueDesc(employee.getMobile());
                        }
                        Object value = field.getFieldValue();
                        if (value == null) {
                            value = "";
                        }
                        field.setFieldValue(employeeFieldService.convertObjectValueToString(field.getType(), field.getFieldValue(), value.toString()));
                        return BeanUtil.copyProperties(field, HrmEmployeeData.class);
                    }).collect(Collectors.toList());
            employeeFieldService.saveEmployeeField(hrmEmployeeInformationData, LabelGroupEnum.COMMUNICATION, employee.getEmployeeId());
        }
        Content content = employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.PERSONAL, employee.getEmployeeId());
        operationLog.setOperationInfo(content.getDetail());
        if (addEmployeeFieldManageBO.getEntryStatus() == 1) {
            abnormalChangeRecordService.addAbnormalChangeRecord(employee.getEmployeeId(), AbnormalChangeType.NEW_ENTRY, LocalDateTime.now());
        }
        //发送通知
//        AdminMessage adminMessage = new AdminMessage();
//        adminMessage.setCreateUser(UserUtil.getUserId());
//        adminMessage.setCreateTime(LocalDateTime.now());
//        adminMessage.setRecipientUser(EmployeeCacheUtil.getUserId(employee.getEmployeeId()));
//        adminMessage.setLabel(8);
//        adminMessage.setType(AdminMessageEnum.HRM_EMPLOYEE_OPEN.getType());
//        messageService.save(adminMessage);

        if (operationLogList.isEmpty()) {
            operationLogList.add(operationLog);
        }
        return operationLogList;
    }

    @Override
    public void recordToFormType(InformationFieldVO record, FieldEnum typeEnum) {
        record.setFormType(typeEnum.getFormType());
        switch (typeEnum) {
            case CHECKBOX:
                String contains = "[]";
                if (record.getDefaultValue().toString().contains(contains)) {
                    record.setDefaultValue("");
                }
                record.setDefaultValue(StrUtil.splitTrim((CharSequence) record.getDefaultValue().toString(), Const.SEPARATOR));
                record.setFieldValue(StrUtil.splitTrim((CharSequence) record.getFieldValue(), Const.SEPARATOR));
            case SELECT:
                if (Objects.equals(record.getRemark(), FieldEnum.OPTIONS_TYPE.getFormType())) {
                    if (CollUtil.isEmpty(record.getOptionsData())) {
                        JSONObject optionsData = JSON.parseObject(record.getOptions());
                        record.setOptionsData(optionsData);
                        record.setSetting(new ArrayList<>(optionsData.keySet()));
                    }
                } else {
                    if (CollUtil.isEmpty(record.getSetting())) {
                        try {
                            String dtStr = Optional.ofNullable(record.getOptions()).orElse("").toString();
                            List<Object> jsonArrayList = JSON.parseObject(dtStr, List.class);
                            record.setSetting(jsonArrayList);
                        } catch (Exception e) {
                            record.setSetting(new ArrayList<>(StrUtil.splitTrim(record.getOptions(), Const.SEPARATOR)));
                        }
                    }
                }
                break;
            case DATE_INTERVAL:
                String dataValueStr = Optional.ofNullable(record.getDefaultValue()).orElse("").toString();
                if (StrUtil.isNotEmpty(dataValueStr)) {
                    record.setDefaultValue(StrUtil.split(dataValueStr, Const.SEPARATOR));
                }
                if (record.getFieldValue() instanceof String) {
                    record.setFieldValue(StrUtil.split((String) record.getFieldValue(), Const.SEPARATOR));
                }
                break;
            case USER:
            case STRUCTURE:
                record.setDefaultValue(new ArrayList<>(0));
                break;
            case AREA:
                String defaultValue = Optional.ofNullable(record.getDefaultValue()).orElse("").toString();
                record.setDefaultValue(JSON.parse(defaultValue));
                if (record.getFieldValue() instanceof String) {
                    String value = Optional.ofNullable(record.getFieldValue()).orElse("").toString();
                    record.setFieldValue(value);
                }
                break;
            case DETAIL_TABLE:
                if (CollUtil.isEmpty(record.getFieldExtendList())) {
                    record.setFieldExtendList(hrmFieldExtendService.queryHrmFieldExtend(record.getFieldId()));
                }
                record.setFieldValue(employeeFieldService.convertValueByFormType(record.getFieldValue(), typeEnum));
                break;
            case DESC_TEXT:
                record.setFieldValue(record.getDefaultValue());
                break;
            default:
                record.setSetting(new ArrayList<>());
                break;
        }
    }

    @Override
    public SimpleHrmEmployeeVO querySimpleEmployee(Long employeeId) {
        if (employeeId == null) {
            return new SimpleHrmEmployeeVO();
        }
        HrmEmployee hrmEmployee = lambdaQuery().select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName)
                .eq(HrmEmployee::getEmployeeId, employeeId).eq(HrmEmployee::getIsDel, 0).one();
        return BeanUtil.copyProperties(hrmEmployee, SimpleHrmEmployeeVO.class);
    }

    /**
     * 查询部门员工列表(考勤打卡调用)
     *
     * @param deptId
     * @return
     */
    @Override
    public DeptEmployeeListVO queryAttendDeptEmployeeList(Long deptId) {
        Integer dataAuthType = DataAuthEnum.ALL.getValue();
        Collection<Long> deptIds = null;
        if (!EmployeeHolder.isHrmAdmin() && !DataAuthEnum.ALL.getValue().equals(dataAuthType)) {
            deptIds = employeeUtil.queryDataAuthDeptId(dataAuthType);
        } else {
            List<HrmDept> hrmDeptList = hrmDeptService.list();
            if (CollectionUtil.isNotEmpty(hrmDeptList)) {
                deptIds = hrmDeptList.stream().map(HrmDept::getDeptId).collect(Collectors.toList());
            }
        }
        DeptEmployeeListVO deptEmployeeListVO = new DeptEmployeeListVO();
        //查询所有部门下的所有员工列表
        List<DeptEmployeeVO> deptList = hrmDeptService.queryDeptEmployeeList();
        //查询当前部门下的员工列表
        List<SimpleHrmEmployeeVO> employeeList = employeeMapper.querySimpleEmpByDeptId(deptId);
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.ATTENDANCE_MENU_ID);
        List<SimpleHrmEmployeeVO> empFilterList = employeeList.stream().filter(employeeVO -> {
            return employeeIds.contains(employeeVO.getEmployeeId());
        }).collect(Collectors.toList());
        //查询指定部门的下级部门并返回树结构
        List<DeptEmployeeVO> treeDept = createTreeInspection(deptId, deptList);
        DeptEmployeeVO root = new DeptEmployeeVO();
        DeptEmployeeVO node = new DeptEmployeeVO();
        node.setChildren(treeDept);
        if (CollUtil.isNotEmpty(treeDept)) {
            filterNode(root, node, deptIds);
        }
        deptEmployeeListVO.setDeptList(root.getChildren());
        deptEmployeeListVO.setEmployeeList(empFilterList);
        return deptEmployeeListVO;
    }

    /**
     * 查询考勤范围可查询的所有员工
     *
     * @param employeeName
     * @return
     */
    @Override
    public List<SimpleHrmEmployeeVO> queryAttendanceAllEmployeeList(String employeeName) {
        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.EMPLOYEE_MENU_ID);
        LambdaQueryWrapper<HrmEmployee> wrapper = new QueryWrapper<HrmEmployee>().lambda().select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName, HrmEmployee::getPost,
                HrmEmployee::getEntryStatus, HrmEmployee::getIsDel, HrmEmployee::getDeptId).eq(HrmEmployee::getIsDel, 0).like(StrUtil.isNotEmpty(employeeName), HrmEmployee::getEmployeeName, employeeName);
        List<HrmEmployee> hrmEmployeeList = this.list(wrapper);
        List<SimpleHrmEmployeeVO> simpleHrmEmployeeVOList = new ArrayList<>();
        for (HrmEmployee employee : hrmEmployeeList) {
            if (employeeIds.contains(employee.getEmployeeId())) {
                simpleHrmEmployeeVOList.add(transferSimpleEmp(employee));
            }
        }
        return simpleHrmEmployeeVOList;
    }

    @Override
    public HrmEmployee queryByJobNumber(String jobNumber)
    {
        LambdaQueryWrapper<HrmEmployee> wrapper = new QueryWrapper<HrmEmployee>().lambda().select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName, HrmEmployee::getPost,
                HrmEmployee::getEntryStatus, HrmEmployee::getIsDel, HrmEmployee::getDeptId).eq(HrmEmployee::getIsDel, 0).eq(StrUtil.isNotEmpty(jobNumber), HrmEmployee::getJobNumber, jobNumber);
        List<HrmEmployee> hrmEmployeeList = this.list(wrapper);
        return hrmEmployeeList.get(0);
    }

    @Autowired
    hrmEmployeeRepository employeeRep;
    @Autowired
    hrmDeptRepository deptRep;
    @Autowired
    hrmEmployeeQuitInfoRepository quitRep;
    @Autowired
    hrmEmployeeEducationExperienceRepository eduExpRep;
    Logger logger= LoggerFactory.getLogger(HrmEmployeeServiceImpl.class);
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ImportDatas(List<EmployeeImportVO> rows)throws Exception {
        List<com.tianye.hrsystem.model.HrmEmployee> datas=new ArrayList<>();
        List<com.tianye.hrsystem.model.HrmEmployeeQuitInfo> qs=new ArrayList<>();
        for(int i=0;i<rows.size();i++){
            EmployeeImportVO vo=rows.get(i);
            String Name=vo.getEmployeeName();
            List<com.tianye.hrsystem.model.HrmEmployee> findEmps=employeeRep.findAllByEmployeeName(Name);
            if(findEmps.size()>1){
                throw new Exception(Name+"在系统中存在重名!");
            }
            com.tianye.hrsystem.model.HrmEmployee emp=null;
            if(findEmps.size()==1){
                emp=findEmps.get(0);
                logger.info("即将更新:"+Name);
            } else {
                emp=new com.tianye.hrsystem.model.HrmEmployee();
                emp.setEmployeeName(Name);
                emp.setCreateTime(new Date());
                emp.setCreateUserId(1L);
                logger.info("新增了用户:"+Name);
            }

            String status=vo.getStatus();
            if(StringUtils.isEmpty(status)==false){
                if(status.equals("正式工")){
                    emp.setStatus(1);
                }
                else if(status.equals("劳务工")){
                    emp.setStatus(5);
                }
                else if(status.equals("试用工")){
                    emp.setStatus(2);
                }
                else if(status.equals("实习工")){
                    emp.setStatus(3);
                }
            }
            String entryStatus=vo.getEntryStatus();
            String sex=vo.getSex();
            if(StringUtils.isEmpty(sex)==false){
                if(sex.equals("男")){
                    emp.setSex(1);
                }
                else emp.setSex(2);
            }
            String idNumber=vo.getIdNumber();
            if(StringUtils.isEmpty(idNumber)==false){
                emp.setIdType(1);
                emp.setIdNumber(idNumber);
            }
            String mobile=vo.getMobile();
            if(StringUtils.isEmpty(mobile)==false){
                emp.setMobile(mobile);
            }
            String deptId=vo.getDeptId();
            if(StringUtils.isEmpty(deptId)==false){
                deptId=deptId.trim();
                List<com.tianye.hrsystem.model.HrmDept> findDepts=new ArrayList<>();
                if(deptId.startsWith("集团")){
                    findDepts=deptRep.findAllByParentIdAndName(1481534121629855749L,deptId.replace("集团",""));
                }
                else {
                    findDepts=deptRep.findAllByNameOrderByDeptId(deptId);
                }
                if(findDepts.size()>0){
                    if(findDepts.size()==1){
                        emp.setDeptId(findDepts.get(0).getDeptId());
                    } else {
                        com.tianye.hrsystem.model.HrmDept dd=findDepts.get(findDepts.size()-1);
                        emp.setDeptId(dd.getDeptId());
                    }
                } else throw new Exception(deptId+"未找到对应的部门信息!");
            }
            emp.setAge(vo.getAge());

            SimpleDateFormat simple=new SimpleDateFormat("yyyy/MM/dd");
            String entryTime=vo.getEntryTime();
            if(StringUtils.isEmpty(entryTime)==false){
                if(entryTime.indexOf("/")==-1 && entryTime.indexOf("-")>=0){
                    simple=new SimpleDateFormat("yyyy-MM-dd");
                } else simple=new SimpleDateFormat("yyyy/MM/dd");
                try {
                    LocalDate dEntry=LocalDate.parse(entryTime);
                    emp.setEntryTime(dEntry);
                }
                catch(Exception p){

                }
            }
            String beComeTime=vo.getBecomeTime();
            if(StringUtils.isEmpty(beComeTime)==false){
                if(beComeTime.indexOf("/")==-1 && beComeTime.indexOf("-")>=0){
                    simple=new SimpleDateFormat("yyyy-MM-dd");
                } else simple=new SimpleDateFormat("yyyy/MM/dd");
                try {
                    LocalDateTime dBeCome=LocalDateTime.parse(beComeTime);
                    emp.setBecomeTime(dBeCome);
                    if(emp.getCompanyAgeStartTime()==null){
                        // 司龄开始日期兜底必须用入职日期:历史版本误用转正日期,导致司龄整体偏小
                        LocalDate entryDate = emp.getEntryTime();
                        emp.setCompanyAgeStartTime(entryDate != null ? entryDate.atStartOfDay() : dBeCome);
                    }
                }
                catch(Exception z){
                   // z.printStackTrace();
                }
            }
            String dateOfBirth=vo.getDateOfBirth();
            if(StringUtils.isEmpty(dateOfBirth)){
                if(StringUtils.isEmpty(idNumber)==false){
                    dateOfBirth=idNumber.substring(6,14);
                }
            }
            if(StringUtils.isEmpty(dateOfBirth)==false){
                if(dateOfBirth.indexOf("/")==-1 && dateOfBirth.indexOf("-")>=0){
                    simple=new SimpleDateFormat("yyyy-MM-dd");
                }
                //从身份证中取。
                if(dateOfBirth.indexOf("/")==-1 && dateOfBirth.indexOf("/")==-1 && dateOfBirth.indexOf("-")==-1){
                    simple=new SimpleDateFormat("yyyyMMdd");
                }
                try {
                    if(dateOfBirth.equals("会计师")){
                        int a=11111;

                    }
                    Date dBirth=simple.parse(dateOfBirth);
                    emp.setDateOfBirth(dBirth);
                    emp.setBirthdayType(1);
                    SimpleDateFormat s=new SimpleDateFormat("MMdd");
                    emp.setBirthday(s.format(dBirth));
                }
                catch(Exception z){
                    z.printStackTrace();
                }
            }
            String quitTime=vo.getQuitTime();
            Date dQuit=null;
            if(StringUtils.isEmpty(quitTime)==false){
                if(quitTime.indexOf("/")==-1 && quitTime.indexOf("-")>=0){
                    simple=new SimpleDateFormat("yyyy-MM-dd");
                } else simple=new SimpleDateFormat("yyyy/MM/dd");
                try {
                    dQuit=simple.parse(quitTime);
                }
                catch(Exception z){

                }
            }
            String jobNumber=vo.getJobNumber();
            if(StringUtils.isEmpty(jobNumber)){
                jobNumber= Long.toString(System.currentTimeMillis());
            }
            emp.setJobNumber(jobNumber);
            emp.setIsDel(0);

            //人员在职状态
            if(StringUtils.isEmpty(entryStatus)==false){
                if(entryStatus.equals("离职") && dQuit!=null){
                    emp.setEntryStatus(4);
                    List<com.tianye.hrsystem.model.HrmEmployeeQuitInfo> qqs= quitRep.findAllByEmployeeId(emp.getEmployeeId());
                    if(qqs.size()==0){
                        com.tianye.hrsystem.model.HrmEmployeeQuitInfo qInfo=new com.tianye.hrsystem.model.HrmEmployeeQuitInfo();
                        qInfo.setEmployeeId(emp.getEmployeeId());
                        qInfo.setApplyQuitTime(dQuit);
                        qInfo.setPlanQuitTime(dQuit);
                        qInfo.setCreateTime(new Date());
                        qInfo.setQuitType(1);
                        qInfo.setRemarks("导入人员时设置");
                        qs.add(qInfo);
                    }
                }
                else if(entryStatus.equals("在职")){
                    emp.setEntryStatus(1);
                }
            }
            //民族
            if(StringUtils.isEmpty(vo.getNation())==false){
                emp.setNation(vo.getNation());
            }
            //籍贯
            if(StringUtils.isEmpty(vo.getNativePlace())==false){
                emp.setNativePlace(vo.getNativePlace());
            }
            //地址
            if(StringUtils.isEmpty(vo.getAddress())==false){
                emp.setAddress(vo.getAddress());
            }
            //教育程度
            if(StringUtils.isEmpty(vo.getHighestEducation())==false){
                emp.setHighestEducation(getEducation(vo.getHighestEducation()));
                Optional<com.tianye.hrsystem.model.HrmEmployeeEducationExperience> findExps=
                        eduExpRep.findFirstByEducationAndEmployeeId(emp.getHighestEducation(),emp.getEmployeeId());

                //增加一条教育记录
                if(findExps.isPresent()==false){
                    com.tianye.hrsystem.model.HrmEmployeeEducationExperience exp=new com.tianye.hrsystem.model.HrmEmployeeEducationExperience();
                    exp.setEmployeeId(emp.getEmployeeId());
                    exp.setEducation(emp.getHighestEducation());
                    exp.setGraduateSchool(vo.getSchool());
                    exp.setIsFirstDegree(1);
                    exp.setMajor(vo.getMajor());
                    exp.setCreateTime(new Date());
                    exp.setCreateUserId(1L);
                    eduExpRep.save(exp);
                }
            }
            emp.setFullAttendance(1);
            emp.setExpandProduction(1);
            datas.add(emp);
        }
        if(datas.size()>0){
            logger.info("即将更新:"+Integer.toString(datas.size())+"条数据!");
            employeeRep.saveAll(datas);
        }
        if(qs.size()>0){
            logger.info("即将更新:"+Integer.toString(datas.size())+"条离职数据!");
            quitRep.saveAll(qs);
        }
    }
    private  Integer getEducation(String eduName){
        //学历 1小学、2初中、3中专、4中职、5中技、6高中、7大专、8本科、9硕士、10博士、11博士后、12其他
        if(eduName.equals("小学")){
            return 1;
        }
        else if(eduName.equals("初中")){
            return 2;
        }
        else if(eduName.contains("中专")){
            return 3;
        }
        else if(eduName.contains("中职")){
            return 4;
        }
        else if(eduName.contains("中技")){
            return 5;
        }
        else if(eduName.equals("高中")){
            return 6;
        }
        else if(eduName.equals("大专")){
            return 7;
        }
        else if(eduName.equals("本科")){
            return 8;
        }
        else if(eduName.equals("硕士")){
            return 9;
        }
        else if(eduName.equals("博士")){
            return 10;
        }
        else if(eduName.equals("博士后")){
            return 11;
        }
        else return 12;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importEmployee(MultipartFile multipartFile) throws Exception {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "请选择员工花名册文件");
        }
        try (InputStream inputStream = multipartFile.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = findRosterImportSheet(workbook);
            if (sheet == null) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "导入文件未找到可读取的工作表");
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            List<RosterImportColumn> columns = readRosterImportColumns(sheet, formatter, evaluator);
            if (columns.isEmpty()) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "导入文件第2行未读取到员工字段");
            }
            Map<String, Integer> headerRepeatCount = countHeaderRepeat(columns);
            List<HrmEmployee> existingEmployees = list();
            Map<String, HrmEmployee> employeeMap = buildEmployeeImportUniqueMap(existingEmployees);
            Map<String, HrmEmployee> employeeIdentityMap = buildEmployeeImportIdentityMap(existingEmployees);
            Map<Integer, Map<String, HrmEmployeeField>> fieldMap = buildRosterFieldMap(employeeFieldService.list());
            ensureRosterDynamicFields(columns, headerRepeatCount, fieldMap);
            Set<String> rowUniqueKeys = new HashSet<>();
            List<HrmEmployee> checkedEmployees = new ArrayList<>();
            List<RosterImportRecord> importRecords = new ArrayList<>();
            for (int rowIndex = ROSTER_DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                Map<RosterImportColumn, String> rowValues = readRosterRow(row, columns, formatter, evaluator);
                if (isRosterRowEmpty(rowValues)) {
                    continue;
                }
                String employeeName = valueOf(rowValues, "基本信息", "姓名");
                String mobile = normalizePhone(valueOf(rowValues, "基本信息", "个人电话"));
                if (StrUtil.isEmpty(mobile)) {
                    mobile = normalizePhone(valueOf(rowValues, "", "手机"));
                }
                if (StrUtil.isEmpty(employeeName) || StrUtil.isEmpty(mobile)) {
                    throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + (rowIndex + 1) + "行姓名和个人电话不能为空");
                }
                String uniqueKey = employeeUniqueKey(employeeName, mobile);
                if (!rowUniqueKeys.add(uniqueKey)) {
                    throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "导入文件中存在重复员工：" + employeeName + " + " + mobile);
                }
                HrmEmployee employee = employeeMap.get(uniqueKey);
                if (employee == null) {
                    employee = employeeIdentityMap.get(employeeIdentityKey(employeeName, valueOf(rowValues, "基本信息", "身份证号")));
                }
                boolean isNewEmployee = employee == null;
                if (isNewEmployee) {
                    employee = new HrmEmployee();
                    employee.setIsDel(0);
                    employee.setEntryStatus(EmployeeEntryStatus.IN.getValue());
                    employee.setStatus(EmployeeStatusEnum.OFFICIAL.getValue());
                    employee.setEmploymentForms(EmploymentFormsEnum.OFFICIAL.getValue());
                    employee.setCreateTime(LocalDateTime.now());
                    employee.setCreateUserId(currentImportUserId());
                } else {
                    employee = BeanUtil.copyProperties(employee, HrmEmployee.class);
                }
                applyRosterEmployeeFields(employee, rowValues);
                normalizeEmployeeUniqueFields(employee);
                validateEmployeeUniqueFields(employee, employee.getEmployeeId(), checkedEmployees);
                checkedEmployees.add(employee);
                importRecords.add(new RosterImportRecord(employee, isNewEmployee, rowValues));
                employeeMap.put(uniqueKey, employee);
            }
            for (RosterImportRecord importRecord : importRecords) {
                HrmEmployee employee = importRecord.getEmployee();
                Map<RosterImportColumn, String> rowValues = importRecord.getRowValues();
                if (importRecord.isNewEmployee()) {
                    if (employee.getCompanyAgeStartTime() == null) {
                        employee.setCompanyAgeStartTime(employee.getEntryTime());
                    }
                    // 导入未填直属上级时，默认取部门的分管领导
                    fillParentIdFromDeptLeader(employee);
                    save(employee);
                } else {
                    employee.setUpdateTime(LocalDateTime.now());
                    employee.setUpdateUserId(currentImportUserId());
                    updateById(employee);
                }
                saveRosterRelatedInformation(employee.getEmployeeId(), rowValues);
                saveRosterDynamicData(employee.getEmployeeId(), rowValues, fieldMap, headerRepeatCount);
            }
        }
    }

    /**
     * 员工未手动指定直属上级时，默认取其部门的分管领导；分管领导是本人时不设置（自己不能是自己的上级）
     */
    private void fillParentIdFromDeptLeader(HrmEmployee employee) {
        if (employee == null || employee.getParentId() != null || employee.getDeptId() == null) {
            return;
        }
        HrmDept dept = hrmDeptService.getById(employee.getDeptId());
        if (dept == null || dept.getLeaderEmployeeId() == null) {
            return;
        }
        if (employee.getEmployeeId() != null && employee.getEmployeeId().equals(dept.getLeaderEmployeeId())) {
            return;
        }
        employee.setParentId(dept.getLeaderEmployeeId());
    }

    private Sheet findRosterImportSheet(Workbook workbook) {
        int activeSheetIndex = workbook.getActiveSheetIndex();
        if (activeSheetIndex >= 0 && activeSheetIndex < workbook.getNumberOfSheets()
                && !workbook.isSheetHidden(activeSheetIndex) && !workbook.isSheetVeryHidden(activeSheetIndex)) {
            return workbook.getSheetAt(activeSheetIndex);
        }
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            if (workbook.isSheetHidden(index) || workbook.isSheetVeryHidden(index)) {
                continue;
            }
            return workbook.getSheetAt(index);
        }
        return null;
    }

    private List<RosterImportColumn> readRosterImportColumns(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        Row groupRow = sheet.getRow(ROSTER_GROUP_ROW_INDEX);
        Row headerRow = sheet.getRow(ROSTER_HEADER_ROW_INDEX);
        if (headerRow == null) {
            return Collections.emptyList();
        }
        List<RosterImportColumn> columns = new ArrayList<>();
        String currentGroup = "";
        short lastCellNum = headerRow.getLastCellNum();
        for (int columnIndex = 0; columnIndex < lastCellNum; columnIndex++) {
            String group = getCellText(groupRow == null ? null : groupRow.getCell(columnIndex), formatter, evaluator);
            if (StrUtil.isNotEmpty(group)) {
                currentGroup = group;
            }
            String header = getCellText(headerRow.getCell(columnIndex), formatter, evaluator);
            if (StrUtil.isEmpty(header)) {
                continue;
            }
            columns.add(new RosterImportColumn(columnIndex, currentGroup, header));
        }
        return columns;
    }

    private Map<RosterImportColumn, String> readRosterRow(Row row, List<RosterImportColumn> columns, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<RosterImportColumn, String> rowValues = new LinkedHashMap<>();
        if (row == null) {
            return rowValues;
        }
        for (RosterImportColumn column : columns) {
            rowValues.put(column, getCellText(row.getCell(column.getIndex()), formatter, evaluator));
        }
        return rowValues;
    }

    private String getCellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
            return LocalDateTimeUtil.format(cell.getDateCellValue().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate(), DatePattern.NORM_DATE_PATTERN);
        }
        String text;
        try {
            text = formatter.formatCellValue(cell, evaluator);
        } catch (RuntimeException ex) {
            text = formatCachedFormulaCellValue(cell, formatter);
        }
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ').trim();
    }

    private String formatCachedFormulaCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() != CellType.FORMULA) {
            return "";
        }
        try {
            CellType cachedType = cell.getCachedFormulaResultType();
            if (cachedType == CellType.NUMERIC) {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return LocalDateTimeUtil.format(cell.getDateCellValue().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate(), DatePattern.NORM_DATE_PATTERN);
                }
                return formatter.formatRawCellContents(cell.getNumericCellValue(), cell.getCellStyle().getDataFormat(), cell.getCellStyle().getDataFormatString());
            }
            if (cachedType == CellType.STRING) {
                return cell.getStringCellValue();
            }
            if (cachedType == CellType.BOOLEAN) {
                return String.valueOf(cell.getBooleanCellValue());
            }
        } catch (RuntimeException ignored) {
            return "";
        }
        return "";
    }

    private Map<String, HrmEmployee> buildEmployeeImportUniqueMap(List<HrmEmployee> employees) {
        Map<String, HrmEmployee> employeeMap = new HashMap<>();
        if (CollectionUtil.isEmpty(employees)) {
            return employeeMap;
        }
        for (HrmEmployee employee : employees) {
            if (employee == null || Objects.equals(employee.getIsDel(), 1)) {
                continue;
            }
            String name = employee.getEmployeeName();
            String mobile = normalizePhone(employee.getMobile());
            if (StrUtil.isEmpty(name) || StrUtil.isEmpty(mobile)) {
                continue;
            }
            String uniqueKey = employeeUniqueKey(name, mobile);
            if (employeeMap.containsKey(uniqueKey)) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "系统中存在重复员工：" + name + " + " + mobile);
            }
            employeeMap.put(uniqueKey, employee);
        }
        return employeeMap;
    }

    private Map<String, HrmEmployee> buildEmployeeImportIdentityMap(List<HrmEmployee> employees) {
        Map<String, HrmEmployee> employeeIdentityMap = new HashMap<>();
        if (CollectionUtil.isEmpty(employees)) {
            return employeeIdentityMap;
        }
        for (HrmEmployee employee : employees) {
            if (employee == null || Objects.equals(employee.getIsDel(), 1)) {
                continue;
            }
            String uniqueKey = employeeIdentityKey(employee.getEmployeeName(), employee.getIdNumber());
            if (StrUtil.isEmpty(uniqueKey)) {
                continue;
            }
            if (employeeIdentityMap.containsKey(uniqueKey)) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "系统中存在重复员工身份证：" + employee.getEmployeeName() + " + " + normalizeIdNumber(employee.getIdNumber()));
            }
            employeeIdentityMap.put(uniqueKey, employee);
        }
        return employeeIdentityMap;
    }

    private String employeeUniqueKey(String employeeName, String mobile) {
        return normalizeHeader(employeeName) + "|" + normalizePhone(mobile);
    }

    private String employeeIdentityKey(String employeeName, String idNumber) {
        String name = normalizeHeader(employeeName);
        String identity = normalizeIdNumber(idNumber);
        if (StrUtil.isEmpty(name) || StrUtil.isEmpty(identity)) {
            return "";
        }
        return name + "|" + identity;
    }

    private void applyRosterEmployeeFields(HrmEmployee employee, Map<RosterImportColumn, String> rowValues) {
        setStringIfPresent(valueOf(rowValues, "基本信息", "姓名"), employee::setEmployeeName);
        setStringIfPresent(normalizePhone(valueOf(rowValues, "基本信息", "个人电话")), employee::setMobile);
        setStringIfPresent(valueOf(rowValues, "基本信息", "工号"), employee::setJobNumber);
        setStringIfPresent(valueOf(rowValues, "基本信息", "身份证号"), value -> {
            employee.setIdType(IdTypeEnum.ID_CARD.getValue());
            employee.setIdNumber(value);
        });
        setStringIfPresent(valueOf(rowValues, "基本信息", "籍贯"), employee::setNativePlace);
        setStringIfPresent(valueOf(rowValues, "基本信息", "户籍地址"), employee::setAddress);
        setStringIfPresent(valueOf(rowValues, "基本信息", "民族"), employee::setNation);
        setStringIfPresent(valueOf(rowValues, "基本信息", "职位"), employee::setPost);
        setStringIfPresent(valueOf(rowValues, "基本信息", "职务级别"), employee::setPostLevel);
        setStringIfPresent(valueOf(rowValues, "基本信息", "工作地点"), employee::setWorkAddress);
        setStringIfPresent(valueOf(rowValues, "基本信息", "性别"), value -> employee.setSex(parseSex(value)));
        setStringIfPresent(valueOf(rowValues, "基本信息", "入职日期"), value -> employee.setEntryTime(parseLocalDate(value)));
        setStringIfPresent(valueOf(rowValues, "基本信息", "出生日期"), value -> employee.setDateOfBirth(parseLocalDate(value)));
        setStringIfPresent(valueOf(rowValues, "基本信息", "年龄"), value -> employee.setAge(parseInteger(value)));
        setStringIfPresent(valueOf(rowValues, "基本信息", "工龄"), value -> employee.setCompanyAge(parseInteger(value)));
        Long deptId = findDeptIdByName(valueOf(rowValues, "基本信息", "部门"));
        if (deptId != null) {
            employee.setDeptId(deptId);
        }
    }

    private void saveRosterRelatedInformation(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        if (employeeId == null) {
            return;
        }
        saveRosterWorkExperience(employeeId, rowValues);
        saveRosterEducationExperience(employeeId, rowValues);
        saveRosterContact(employeeId, rowValues);
        saveRosterCertificate(employeeId, rowValues);
        saveRosterTraining(employeeId, rowValues);
        saveRosterContract(employeeId, rowValues);
        saveRosterSalarySocialSecurity(employeeId, rowValues);
        saveRosterQuitInfo(employeeId, rowValues);
    }

    private void saveRosterWorkExperience(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        HrmEmployeeWorkExperience workExperience = new HrmEmployeeWorkExperience();
        workExperience.setEmployeeId(employeeId);
        workExperience.setWorkUnit(valueOf(rowValues, "工作经历", "工作单位"));
        workExperience.setWorkStartTime(parseLocalDate(valueOf(rowValues, "工作经历", "开始日期")));
        workExperience.setWorkEndTime(parseLocalDate(valueOf(rowValues, "工作经历", "结束日期")));
        workExperience.setPost(valueOf(rowValues, "工作经历", "工作岗位"));
        workExperience.setWitness(valueOf(rowValues, "工作经历", "证明人"));
        workExperience.setWitnessPhone(normalizePhone(valueOf(rowValues, "工作经历", "联系电话")));
        if (workExperienceService != null && hasAnyValue(workExperience.getWorkUnit(), workExperience.getPost(), workExperience.getWitness(), workExperience.getWitnessPhone(), workExperience.getWorkStartTime(), workExperience.getWorkEndTime())) {
            workExperience.setSort(1);
            workExperienceService.save(workExperience);
        }
    }

    private void saveRosterEducationExperience(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        HrmEmployeeEducationExperience educationExperience = new HrmEmployeeEducationExperience();
        educationExperience.setEmployeeId(employeeId);
        String educationName = valueOf(rowValues, "教育经历", "学历");
        if (StrUtil.isNotEmpty(educationName)) {
            educationExperience.setEducation(getEducation(educationName));
        }
        educationExperience.setGraduateSchool(valueOf(rowValues, "教育经历", "毕业学校"));
        educationExperience.setMajor(valueOf(rowValues, "教育经历", "专业"));
        educationExperience.setAdmissionTime(parseLocalDate(valueOf(rowValues, "教育经历", "开始日期")));
        educationExperience.setGraduationTime(parseLocalDate(valueOf(rowValues, "教育经历", "结束日期")));
        if (educationExperienceService != null && hasAnyValue(educationExperience.getEducation(), educationExperience.getGraduateSchool(), educationExperience.getMajor(), educationExperience.getAdmissionTime(), educationExperience.getGraduationTime())) {
            educationExperience.setSort(1);
            educationExperienceService.save(educationExperience);
        }
    }

    private void saveRosterContact(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        HrmEmployeeContacts contacts = new HrmEmployeeContacts();
        contacts.setEmployeeId(employeeId);
        contacts.setContactsAddress(valueOf(rowValues, "家庭状况", "家庭住址"));
        contacts.setRelation(firstNonBlank(valueOf(rowValues, "家庭状况", "与本人关系"), valueOf(rowValues, "家庭状况", "家庭关系/称呼")));
        contacts.setContactsName(firstNonBlank(valueOf(rowValues, "家庭状况", "紧急联系人"), valueOf(rowValues, "家庭状况", "姓名")));
        contacts.setContactsPhone(normalizePhone(valueOf(rowValues, "家庭状况", "电话号码")));
        contacts.setContactsPost(valueOf(rowValues, "家庭状况", "职业"));
        if (contactsService != null && hasAnyValue(contacts.getContactsName(), contacts.getRelation(), contacts.getContactsPhone(), contacts.getContactsAddress(), contacts.getContactsPost())) {
            contacts.setSort(1);
            contactsService.save(contacts);
        }
    }

    private void saveRosterCertificate(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        String certificateName = valueOf(rowValues, "教育经历", "职业资格/水平证书");
        if (StrUtil.isEmpty(certificateName) || certificateService == null) {
            return;
        }
        HrmEmployeeCertificate certificate = new HrmEmployeeCertificate();
        certificate.setEmployeeId(employeeId);
        certificate.setCertificateName(certificateName);
        certificate.setSort(1);
        certificateService.save(certificate);
    }

    private void saveRosterTraining(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        String trainingRecord = valueOf(rowValues, "教育经历", "培训记录");
        if (StrUtil.isEmpty(trainingRecord) || trainingExperienceService == null) {
            return;
        }
        HrmEmployeeTrainingExperience trainingExperience = new HrmEmployeeTrainingExperience();
        trainingExperience.setEmployeeId(employeeId);
        trainingExperience.setTrainingCourse(trainingRecord);
        trainingExperience.setSort(1);
        trainingExperienceService.save(trainingExperience);
    }

    private void saveRosterContract(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        LocalDate startTime = parseLocalDate(valueOf(rowValues, "员工关系及其他", "开始时间"));
        LocalDate endTime = parseLocalDate(valueOf(rowValues, "员工关系及其他", "结束时间"));
        String contractPeriod = valueOf(rowValues, "员工关系及其他", "劳动/劳务合同期限");
        if (startTime == null && endTime == null && StrUtil.isNotEmpty(contractPeriod) && contractPeriod.contains("-")) {
            String[] range = contractPeriod.split("-", 2);
            startTime = parseLocalDate(range[0]);
            endTime = parseLocalDate(range[1]);
        }
        if (startTime == null || endTime == null || contractService == null) {
            return;
        }
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setEmployeeId(employeeId);
        contract.setStartTime(startTime);
        contract.setEndTime(endTime);
        contract.setContractType(1);
        contract.setStatus(1);
        contract.setSort(1);
        contractService.addOrUpdateContract(contract);
    }

    private void saveRosterSalarySocialSecurity(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        String salaryCardNum = valueOf(rowValues, "修订/更新", "银行卡号");
        if (StrUtil.isNotEmpty(salaryCardNum) && securityInfoService != null) {
            com.tianye.hrsystem.modules.salary.entity.HrmEmployeeSalaryCard salaryCard = new com.tianye.hrsystem.modules.salary.entity.HrmEmployeeSalaryCard();
            salaryCard.setEmployeeId(employeeId);
            salaryCard.setSalaryCardNum(salaryCardNum);
            securityInfoService.addOrUpdateSalaryCard(salaryCard);
        }
        HrmEmployeeSocialSecurityInfo socialSecurityInfo = new HrmEmployeeSocialSecurityInfo();
        socialSecurityInfo.setEmployeeId(employeeId);
        socialSecurityInfo.setSocialSecurityStartMonth(valueOf(rowValues, "修订/更新", "社保开始月份"));
        socialSecurityInfo.setSocialSecurityNum(valueOf(rowValues, "修订/更新", "社保号"));
        if (hasAnyValue(socialSecurityInfo.getSocialSecurityStartMonth(), socialSecurityInfo.getSocialSecurityNum()) && securityInfoService != null) {
            securityInfoService.addOrUpdateSocialSecurity(socialSecurityInfo);
        }
    }

    private void saveRosterQuitInfo(Long employeeId, Map<RosterImportColumn, String> rowValues) {
        LocalDate leaveDate = parseLocalDate(valueOf(rowValues, "员工关系及其他", "离职日期"));
        String leaveReason = valueOf(rowValues, "员工关系及其他", "离职原因（类别）");
        String leaveType = valueOf(rowValues, "员工关系及其他", "离职类别");
        if (quitInfoService == null || !hasAnyValue(leaveDate, leaveReason, leaveType)) {
            return;
        }
        HrmEmployeeQuitInfo quitInfo = new HrmEmployeeQuitInfo();
        quitInfo.setEmployeeId(employeeId);
        quitInfo.setPlanQuitTime(leaveDate);
        quitInfo.setApplyQuitTime(leaveDate);
        quitInfo.setRemarks(firstNonBlank(leaveReason, leaveType));
        quitInfoService.save(quitInfo);
        if (employmentRecordService != null && leaveDate != null) {
            employmentRecordService.recordLeave(employeeId, leaveDate, "花名册导入");
        }
    }

    private void saveRosterDynamicData(Long employeeId, Map<RosterImportColumn, String> rowValues, Map<Integer, Map<String, HrmEmployeeField>> fieldMap, Map<String, Integer> headerRepeatCount) {
        List<HrmEmployeeData> employeeDataList = new ArrayList<>();
        List<Long> fieldIds = new ArrayList<>();
        for (Map.Entry<RosterImportColumn, String> entry : rowValues.entrySet()) {
            RosterImportColumn column = entry.getKey();
            String value = entry.getValue();
            if (StrUtil.isEmpty(value) || isMappedRosterColumn(column)) {
                continue;
            }
            value = normalizeRosterDynamicValue(column, value);
            HrmEmployeeField field = findRosterField(column, fieldMap, headerRepeatCount);
            if (field == null || field.getFieldId() == null) {
                continue;
            }
            HrmEmployeeData employeeData = new HrmEmployeeData();
            employeeData.setEmployeeId(employeeId);
            employeeData.setFieldId(field.getFieldId());
            employeeData.setLabelGroup(field.getLabelGroup());
            employeeData.setName(field.getName());
            employeeData.setFieldValue(value);
            employeeData.setFieldValueDesc(value);
            employeeDataList.add(employeeData);
            fieldIds.add(field.getFieldId());
        }
        if (CollectionUtil.isEmpty(employeeDataList)) {
            return;
        }
        employeeDataService.remove(new QueryWrapper<HrmEmployeeData>().eq("employee_id", employeeId).in("field_id", fieldIds));
        employeeDataService.saveBatch(employeeDataList);
    }

    private Map<Integer, Map<String, HrmEmployeeField>> buildRosterFieldMap(List<HrmEmployeeField> fields) {
        Map<Integer, Map<String, HrmEmployeeField>> fieldMap = new HashMap<>();
        if (CollectionUtil.isEmpty(fields)) {
            return fieldMap;
        }
        Map<String, HrmEmployeeField> allFieldMap = fieldMap.computeIfAbsent(0, key -> new HashMap<>());
        for (HrmEmployeeField field : fields) {
            if (field == null || StrUtil.isEmpty(field.getName()) || field.getLabelGroup() == null) {
                continue;
            }
            String normalizedName = normalizeHeader(field.getName());
            fieldMap.computeIfAbsent(field.getLabelGroup(), key -> new HashMap<>()).put(normalizedName, field);
            allFieldMap.putIfAbsent(normalizedName, field);
        }
        return fieldMap;
    }

    private void ensureRosterDynamicFields(List<RosterImportColumn> columns, Map<String, Integer> headerRepeatCount, Map<Integer, Map<String, HrmEmployeeField>> fieldMap) {
        List<HrmEmployeeField> missingFields = new ArrayList<>();
        Set<String> fieldNames = fieldMap.values().stream().flatMap(map -> map.values().stream()).map(HrmEmployeeField::getFieldName).filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        for (RosterImportColumn column : columns) {
            if (isMappedRosterColumn(column)) {
                continue;
            }
            Integer labelGroup = rosterFieldLabelGroup(column);
            String displayName = rosterDynamicDisplayName(column, headerRepeatCount);
            String normalizedDisplayName = normalizeHeader(displayName);
            Map<String, HrmEmployeeField> groupFieldMap = fieldMap.computeIfAbsent(labelGroup, key -> new HashMap<>());
            Map<String, HrmEmployeeField> allFieldMap = fieldMap.computeIfAbsent(0, key -> new HashMap<>());
            HrmEmployeeField existingField = allFieldMap.get(normalizedDisplayName);
            if (existingField != null) {
                ensureRosterDynamicFieldDefinition(existingField, column);
                groupFieldMap.putIfAbsent(normalizedDisplayName, existingField);
                continue;
            }
            if (groupFieldMap.containsKey(normalizedDisplayName)) {
                continue;
            }
            HrmEmployeeField field = new HrmEmployeeField();
            field.setFieldName(generateRosterFieldName(column, fieldNames));
            field.setName(displayName);
            field.setType(rosterDynamicFieldType(column));
            field.setPrecisions(rosterDynamicFieldPrecision(column));
            field.setComponentType(0);
            field.setLabel(rosterFieldLabel(labelGroup));
            field.setLabelGroup(labelGroup);
            field.setMaxLength(255);
            field.setIsUnique(0);
            field.setIsNull(0);
            field.setSorting(1000 + missingFields.size());
            field.setIsFixed(0);
            field.setOperating(0);
            field.setIsHidden(0);
            field.setIsUpdateValue(1);
            field.setIsHeadField(0);
            field.setIsImportField(1);
            field.setIsEmployeeVisible(1);
            field.setIsEmployeeUpdate(1);
            field.setStylePercent(1);
            missingFields.add(field);
            groupFieldMap.put(normalizedDisplayName, field);
            allFieldMap.put(normalizedDisplayName, field);
        }
        if (CollectionUtil.isNotEmpty(missingFields)) {
            employeeFieldService.saveBatch(missingFields);
        }
    }

    private void ensureRosterDynamicFieldDefinition(HrmEmployeeField field, RosterImportColumn column) {
        if (field == null || !isRosterSalaryDecimalColumn(column)) {
            return;
        }
        boolean changed = false;
        Integer expectedType = FieldTypeEnum.DECIMAL.getValue();
        Integer expectedPrecision = 2;
        if (!Objects.equals(field.getType(), expectedType)) {
            field.setType(expectedType);
            changed = true;
        }
        if (!Objects.equals(field.getPrecisions(), expectedPrecision)) {
            field.setPrecisions(expectedPrecision);
            changed = true;
        }
        if (changed && field.getFieldId() != null) {
            employeeFieldService.updateById(field);
        }
    }

    private Integer rosterDynamicFieldType(RosterImportColumn column) {
        if (isRosterSalaryDecimalColumn(column)) {
            return FieldTypeEnum.DECIMAL.getValue();
        }
        return FieldTypeEnum.TEXT.getValue();
    }

    private Integer rosterDynamicFieldPrecision(RosterImportColumn column) {
        if (isRosterSalaryDecimalColumn(column)) {
            return 2;
        }
        return null;
    }

    private String normalizeRosterDynamicValue(RosterImportColumn column, String value) {
        if (!isRosterSalaryDecimalColumn(column) || StrUtil.isEmpty(value)) {
            return value;
        }
        String normalizedValue = cleanRosterText(value).replace(",", "");
        try {
            return new BigDecimal(normalizedValue).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ex) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, column.getNormalizedHeader() + "必须为数字，且最多保留两位小数");
        }
    }

    private boolean isRosterSalaryDecimalColumn(RosterImportColumn column) {
        if (column == null) {
            return false;
        }
        String header = column.getNormalizedHeader();
        return "固定绩效".equals(header) || "职务补助".equals(header) || "其他补助".equals(header);
    }

    private HrmEmployeeField findRosterField(RosterImportColumn column, Map<Integer, Map<String, HrmEmployeeField>> fieldMap, Map<String, Integer> headerRepeatCount) {
        Integer labelGroup = rosterFieldLabelGroup(column);
        Map<String, HrmEmployeeField> groupFieldMap = fieldMap.get(labelGroup);
        String displayName = rosterDynamicDisplayName(column, headerRepeatCount);
        String normalizedDisplayName = normalizeHeader(displayName);
        if (groupFieldMap != null) {
            HrmEmployeeField field = groupFieldMap.get(normalizedDisplayName);
            if (field != null) {
                return field;
            }
        }
        Map<String, HrmEmployeeField> allFieldMap = fieldMap.get(0);
        return allFieldMap == null ? null : allFieldMap.get(normalizedDisplayName);
    }

    private String generateRosterFieldName(RosterImportColumn column, Set<String> fieldNames) {
        String base = "roster_" + Integer.toHexString((column.getNormalizedGroup() + "_" + column.getNormalizedHeader()).hashCode()).replace("-", "n");
        String fieldName = base;
        int index = 1;
        while (fieldNames.contains(fieldName)) {
            fieldName = base + "_" + index++;
        }
        fieldNames.add(fieldName);
        return fieldName;
    }

    private String rosterDynamicDisplayName(RosterImportColumn column, Map<String, Integer> headerRepeatCount) {
        Integer repeatCount = headerRepeatCount.get(column.getNormalizedHeader());
        if (repeatCount != null && repeatCount > 1 && StrUtil.isNotEmpty(column.getGroup())) {
            return cleanRosterText(column.getGroup()) + "-" + cleanRosterText(column.getHeader());
        }
        return cleanRosterText(column.getHeader());
    }

    private Map<String, Integer> countHeaderRepeat(List<RosterImportColumn> columns) {
        Map<String, Integer> repeatCount = new HashMap<>();
        for (RosterImportColumn column : columns) {
            String key = column.getNormalizedHeader();
            repeatCount.put(key, repeatCount.getOrDefault(key, 0) + 1);
        }
        return repeatCount;
    }

    private Integer rosterFieldLabelGroup(RosterImportColumn column) {
        String group = column.getNormalizedGroup();
        if (group.contains("家庭")) {
            return LabelGroupEnum.CONTACT_PERSON.getValue();
        }
        if (group.contains("工作经历")) {
            return LabelGroupEnum.WORK_EXPERIENCE.getValue();
        }
        if (group.contains("教育")) {
            return LabelGroupEnum.EDUCATIONAL_EXPERIENCE.getValue();
        }
        if (group.contains("员工关系") || group.contains("合同")) {
            return LabelGroupEnum.CONTRACT.getValue();
        }
        if (group.contains("薪酬") || group.contains("社保") || group.contains("公积金")) {
            return LabelGroupEnum.SOCIAL_SECURITY.getValue();
        }
        if (group.contains("基本")) {
            return LabelGroupEnum.PERSONAL.getValue();
        }
        return LabelGroupEnum.PERSONAL.getValue();
    }

    private Integer rosterFieldLabel(Integer labelGroup) {
        if (Arrays.asList(LabelGroupEnum.POST.getValue(), LabelGroupEnum.QUIT.getValue()).contains(labelGroup)) {
            return 2;
        }
        if (LabelGroupEnum.CONTRACT.getValue() == labelGroup) {
            return 3;
        }
        if (Arrays.asList(LabelGroupEnum.SALARY_CARD.getValue(), LabelGroupEnum.SOCIAL_SECURITY.getValue()).contains(labelGroup)) {
            return 4;
        }
        return 1;
    }

    private boolean isMappedRosterColumn(RosterImportColumn column) {
        String group = column.getNormalizedGroup();
        String header = column.getNormalizedHeader();
        if (group.contains("基本信息")) {
            return Arrays.asList("工号", "姓名", "性别", "部门", "职位", "入职日期", "个人电话", "身份证号", "籍贯", "户籍地址", "出生日期", "年龄", "工龄", "民族", "职务级别", "工作地点").contains(header);
        }
        if (group.contains("工作经历")) {
            return Arrays.asList("工作单位", "开始日期", "结束日期", "工作岗位", "证明人", "联系电话").contains(header);
        }
        if (group.contains("教育经历")) {
            return Arrays.asList("学历", "毕业学校", "专业", "开始日期", "结束日期", "职业资格/水平证书", "培训记录").contains(header);
        }
        if (group.contains("家庭状况")) {
            return Arrays.asList("家庭住址", "家庭关系/称呼", "姓名", "职业", "紧急联系人", "与本人关系", "电话号码").contains(header);
        }
        if (group.contains("员工关系及其他")) {
            return Arrays.asList("劳动/劳务合同期限", "开始时间", "结束时间", "离职类别", "离职日期", "离职原因（类别）").contains(header);
        }
        if (group.contains("修订/更新")) {
            return Arrays.asList("银行卡号", "社保开始月份", "社保号").contains(header);
        }
        return false;
    }

    private String valueOf(Map<RosterImportColumn, String> rowValues, String groupKeyword, String header) {
        String normalizedGroupKeyword = normalizeHeader(groupKeyword);
        String normalizedHeader = normalizeHeader(header);
        for (Map.Entry<RosterImportColumn, String> entry : rowValues.entrySet()) {
            RosterImportColumn column = entry.getKey();
            if (!column.getNormalizedHeader().equals(normalizedHeader)) {
                continue;
            }
            if (StrUtil.isNotEmpty(normalizedGroupKeyword) && !column.getNormalizedGroup().contains(normalizedGroupKeyword)) {
                continue;
            }
            return entry.getValue();
        }
        return "";
    }

    private boolean isRosterRowEmpty(Map<RosterImportColumn, String> rowValues) {
        if (rowValues == null || rowValues.isEmpty()) {
            return true;
        }
        for (String value : rowValues.values()) {
            if (StrUtil.isNotEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    private Long currentImportUserId() {
        try {
            LoginUserInfo info = CompanyContext.get();
            return info == null ? null : info.getUserIdValueL();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String generateImportJobNumber() {
        return "HR" + System.currentTimeMillis();
    }

    private Long findDeptIdByName(String deptName) {
        if (StrUtil.isEmpty(deptName) || hrmDeptService == null) {
            return null;
        }
        List<HrmDept> deptList = hrmDeptService.list();
        if (CollectionUtil.isEmpty(deptList)) {
            return null;
        }
        String normalizedDeptName = normalizeHeader(deptName);
        for (HrmDept dept : deptList) {
            if (dept != null && normalizedDeptName.equals(normalizeHeader(dept.getName()))) {
                return dept.getDeptId();
            }
        }
        return null;
    }

    private Integer parseSex(String sex) {
        if ("男".equals(cleanRosterText(sex))) {
            return 1;
        }
        if ("女".equals(cleanRosterText(sex))) {
            return 2;
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        String cleanValue = value.trim();
        try {
            return new java.math.BigDecimal(cleanValue).setScale(0, java.math.BigDecimal.ROUND_DOWN).intValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        String cleanValue = cleanRosterText(value).replace("年", "-").replace("月", "-").replace("日", "");
        if (cleanValue.matches("\\d{4}\\.\\d{1,2}")) {
            String[] parts = cleanValue.split("\\.");
            return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
        }
        cleanValue = cleanValue.replace('/', '-').replace('.', '-');
        String[] parts = cleanValue.split("-");
        if (parts.length >= 3) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                if (year < 100) {
                    year += 2000;
                }
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private void setStringIfPresent(String value, java.util.function.Consumer<String> consumer) {
        if (StrUtil.isNotEmpty(value)) {
            consumer.accept(value);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StrUtil.isNotEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private boolean hasAnyValue(Object... values) {
        if (values == null) {
            return false;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String && StrUtil.isEmpty((String) value)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[\\s\\-]", "").trim();
    }

    private String cleanRosterText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", "").replace("\n", "").trim();
    }

    private String normalizeHeader(String text) {
        return cleanRosterText(text).replace(" ", "").replace("　", "");
    }

    private static class RosterImportRecord {
        private final HrmEmployee employee;
        private final boolean newEmployee;
        private final Map<RosterImportColumn, String> rowValues;

        private RosterImportRecord(HrmEmployee employee, boolean newEmployee, Map<RosterImportColumn, String> rowValues) {
            this.employee = employee;
            this.newEmployee = newEmployee;
            this.rowValues = rowValues;
        }

        private HrmEmployee getEmployee() {
            return employee;
        }

        private boolean isNewEmployee() {
            return newEmployee;
        }

        private Map<RosterImportColumn, String> getRowValues() {
            return rowValues;
        }
    }

    private static class RosterImportColumn {
        private final int index;
        private final String group;
        private final String header;
        private final String normalizedGroup;
        private final String normalizedHeader;

        private RosterImportColumn(int index, String group, String header) {
            this.index = index;
            this.group = group == null ? "" : group;
            this.header = header == null ? "" : header;
            this.normalizedGroup = normalizeStatic(this.group);
            this.normalizedHeader = normalizeStatic(this.header);
        }

        private int getIndex() {
            return index;
        }

        private String getGroup() {
            return group;
        }

        private String getHeader() {
            return header;
        }

        private String getNormalizedGroup() {
            return normalizedGroup;
        }

        private String getNormalizedHeader() {
            return normalizedHeader;
        }

        private static String normalizeStatic(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("\r", "").replace("\n", "").replace(" ", "").replace("　", "").trim();
        }
    }

    private Integer getidType(String idType){
        if(idType.equals("身份证")){
            return 1;
        }
        else if(idType.equals("港澳通行证")){
            return 2;
        }
        else if(idType.contains("台湾通行证")){
            return 3;
        }
        else if(idType.contains("护照")){
            return 4;
        }
        else return 5;
    }

    private Integer getSex(String sex){
        if(sex.equals("男")){
            return 1;
        }
        else return 2;
    }

    private Integer getFullAttendance(String fullAttendance) {
        if (fullAttendance.equals("有")) {
            return 1;
        }else return 2;
    }

    private Integer getExpandProduction(String expandProduction) {
        if (expandProduction.equals("未加入")) {
            return 0;
        }else return 1;
    }

    private Integer getEmploymentForms(String employementForms) {
        if (employementForms.equals("正式")) {
            return 1;
        }else return 2;
    }

    private Integer getProbation(String probation) {
        Integer pro = 0;
        switch (probation) {
            case "无试用期":
                pro = 0;
                break;
            case "1个月":
                pro = 1;
                break;
            case "2个月":
                pro = 2;
                break;
            case "3个月":
                pro = 3;
                break;
            case "4个月":
                pro = 4;
            case "5个月":
                pro = 5;
                break;
            case "6个月":
                pro = 6;
                break;
            default:
                break;
        }
        return pro;
    }

    @Override
    public List<SimpleHrmEmployeeVO> listForBatchSetting(List<Long> deptIds) {
        // 递归查找所有选中部门的子部门
        List<Long> allDeptIds = new ArrayList<>(deptIds);
        List<HrmDept> allDepts = hrmDeptService.list();
        for (Long deptId : deptIds) {
            List<Long> childIds = RecursionUtil.getChildList(allDepts, "parentId", deptId, "deptId", "deptId");
            allDeptIds.addAll(childIds);
        }

        List<HrmEmployee> employees = lambdaQuery()
                .select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName, HrmEmployee::getMobile,
                        HrmEmployee::getDeptId, HrmEmployee::getPost, HrmEmployee::getJobNumber)
                .eq(HrmEmployee::getIsDel, 0)
                .in(CollectionUtil.isNotEmpty(allDeptIds), HrmEmployee::getDeptId, allDeptIds)
                .list();
        return employees.stream().map(e -> {
            SimpleHrmEmployeeVO vo = new SimpleHrmEmployeeVO();
            vo.setEmployeeId(e.getEmployeeId());
            vo.setEmployeeName(e.getEmployeeName());
            vo.setMobile(e.getMobile());
            vo.setDeptId(e.getDeptId());
            vo.setPost(e.getPost());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量设置字段白名单(与前端批量设置弹窗配置保持一致)
     */
    private static final Set<String> BATCH_SETTING_FIELDS = new HashSet<>(Arrays.asList(
            "fullAttendance", "expandProduction", "isDisabled", "isContinuousShift", "isRetiredSoldier",
            "isPartyMember", "personnelCategory", "isRemark", "affiliationSystem", "restType"));

    @Override
    public Integer batchUpdateEmployeeField(String fieldName, Integer fieldValue, List<Long> employeeIds) {
        if (!BATCH_SETTING_FIELDS.contains(fieldName)) {
            throw new HrmException(500, "不支持批量设置的字段: " + fieldName);
        }
        if (CollectionUtil.isEmpty(employeeIds) || fieldValue == null) {
            throw new HrmException(500, "参数不完整");
        }
        List<Long> distinctIds = employeeIds.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return 0;
        }
        return lambdaUpdate()
                .set(getFieldSetter(fieldName), fieldValue)
                .in(HrmEmployee::getEmployeeId, distinctIds)
                .update() ? distinctIds.size() : 0;
    }

    private SFunction<HrmEmployee, ?> getFieldSetter(String fieldName) {
        switch (fieldName) {
            case "fullAttendance": return HrmEmployee::getFullAttendance;
            case "expandProduction": return HrmEmployee::getExpandProduction;
            case "isDisabled": return HrmEmployee::getIsDisabled;
            case "isContinuousShift": return HrmEmployee::getIsContinuousShift;
            case "isRetiredSoldier": return HrmEmployee::getIsRetiredSoldier;
            case "isPartyMember": return HrmEmployee::getIsPartyMember;
            case "personnelCategory": return HrmEmployee::getPersonnelCategory;
            case "isRemark": return HrmEmployee::getIsRemark;
            case "affiliationSystem": return HrmEmployee::getAffiliationSystem;
            case "restType": return HrmEmployee::getRestType;
            default: throw new HrmException(500, "不支持批量设置的字段: " + fieldName);
        }
    }
}