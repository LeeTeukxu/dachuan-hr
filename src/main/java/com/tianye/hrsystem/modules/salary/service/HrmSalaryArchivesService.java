package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.vo.HrmEmployeeVO;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.enums.*;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.dto.*;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryArchives;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryArchivesOption;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryChangeRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryChangeTemplate;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryArchivesMapper;
import com.tianye.hrsystem.modules.salary.vo.*;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import  com.tianye.hrsystem.entity.po.HrmEmployee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * //薪资档案表 服务实现类
 */
@Service
public class HrmSalaryArchivesService  extends BaseServiceImpl<HrmSalaryArchivesMapper, HrmSalaryArchives>
{

    @Autowired
    private HrmEmployeeMapper hrmEmployeeMapper;

    @Autowired
    private HrmSalaryArchivesMapper hrmSalaryArchivesMapper;

    @Autowired
    private HrmSalaryChangeRecordService salaryChangeRecordService;

    @Autowired
    private HrmSalaryArchivesOptionService hrmSalaryArchivesOptionService;

    @Autowired
    private HrmSalaryChangeTemplateService hrmSalaryChangeTemplateService;

    @Autowired
    private IHrmEmployeeService employeeService;


    /**
     * 薪资档案列表
     * @param querySalaryArchivesListDto
     * @return
     */
    public BasePage<QuerySalaryArchivesListVO> querySalaryArchivesList(QuerySalaryArchivesListDto querySalaryArchivesListDto)
    {
        List<Long> employeeIds =null;
        if(!CollectionUtil.isEmpty(querySalaryArchivesListDto.getEmployeeIds()))
        {
            employeeIds = querySalaryArchivesListDto.getEmployeeIds();
        }
        else
        {
            employeeIds = new ArrayList<>();
            employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).ne(HrmEmployee::getIsDel, 1).list()
                    .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));
        }
        BasePage<QuerySalaryArchivesListVO> page  = hrmSalaryArchivesMapper.querySalaryArchivesList(querySalaryArchivesListDto.parse(),querySalaryArchivesListDto,employeeIds, querySalaryArchivesListDto.getYear(), querySalaryArchivesListDto.getMonth());
        return page;
    }


    /**
     * 员工薪资档案列表
     * @param querySalaryArchivesListDto
     * @return
     */
    public List<QuerySalaryArchivesListVO> queryEmpSalaryArchivesList(QuerySalaryArchivesListDto querySalaryArchivesListDto)
    {
        List<Long> employeeIds =null;
        if(!CollectionUtil.isEmpty(querySalaryArchivesListDto.getEmployeeIds()))
        {
            employeeIds = querySalaryArchivesListDto.getEmployeeIds();
        }
        else
        {
            employeeIds = new ArrayList<>();
            employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).ne(HrmEmployee::getIsDel, 1).list()
                    .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));
        }
        List<QuerySalaryArchivesListVO> list  = hrmSalaryArchivesMapper.queryEmpSalaryArchivesList(querySalaryArchivesListDto,employeeIds, querySalaryArchivesListDto.getYear(), querySalaryArchivesListDto.getMonth());
        return list;
    }




    /**
     *定薪
     * @param setFixSalaryRecordBOList
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer setFixSalaryRecord(List<SetFixSalaryRecordDto> setFixSalaryRecordBOList)
    {
        LoginUserInfo userInfo = CompanyContext.get();
        for(SetFixSalaryRecordDto setFixSalaryRecordBO : setFixSalaryRecordBOList)
        {
            Long employeeId = setFixSalaryRecordBO.getEmployeeId();
            HrmEmployeeVO employee = hrmEmployeeMapper.getEmployeeEntryByEmpId(employeeId);
            Optional<HrmSalaryChangeRecord> fixSalaryRecordOpt = salaryChangeRecordService.lambdaQuery().eq(HrmSalaryChangeRecord::getEmployeeId, employeeId).eq(HrmSalaryChangeRecord::getRecordType, SalaryChangeRecordTypeEnum.FIX_SALARY.getValue())
                    .oneOpt();
            if (fixSalaryRecordOpt.isPresent()) {
                boolean isUpdate = salaryChangeRecordService.lambdaQuery().eq(HrmSalaryChangeRecord::getEmployeeId, employeeId).eq(HrmSalaryChangeRecord::getRecordType, SalaryChangeRecordTypeEnum.CHANGE_SALARY.getValue())
                        .ne(HrmSalaryChangeRecord::getStatus, SalaryChangeRecordStatusEnum.CANCEL.getValue()).count() > 0;
                if (isUpdate) {
                    throw new HrmException(HrmCodeEnum.CHANGE_SALARY_NOT_FIX_SALARY);
                }
                //如果是更新,需要删除之前的数据重新生成
                lambdaUpdate().eq(HrmSalaryArchives::getEmployeeId, employeeId).remove();
                hrmSalaryArchivesOptionService.lambdaUpdate().eq(HrmSalaryArchivesOption::getEmployeeId, employeeId).remove();
            }
            HrmSalaryArchives hrmSalaryArchives = new HrmSalaryArchives();
            hrmSalaryArchives.setEmployeeId(employeeId);
            hrmSalaryArchives.setChangeData(LocalDate.now());
            hrmSalaryArchives.setChangeReason(SalaryChangeReasonEnum.ENTRY_FIX_SALARY.getValue());
            hrmSalaryArchives.setRemarks(setFixSalaryRecordBO.getRemarks());
            hrmSalaryArchives.setChangeType(SalaryChangeTypeEnum.HAS_FIX_SALARY.getValue());
            //保存薪资项 试用期
            List<HrmSalaryArchivesOption> proArchivesOptionList = setFixSalaryRecordBO.getProSalary().stream().map(option -> {
                HrmSalaryArchivesOption hrmSalaryArchivesOption = new HrmSalaryArchivesOption();
                hrmSalaryArchivesOption.setEmployeeId(employeeId);
                hrmSalaryArchivesOption.setCode(option.getCode());
                hrmSalaryArchivesOption.setName(option.getName());
                hrmSalaryArchivesOption.setValue(option.getValue());
                hrmSalaryArchivesOption.setIsPro(1);
                return hrmSalaryArchivesOption;
            }).collect(Collectors.toList());
            hrmSalaryArchivesOptionService.saveBatch(proArchivesOptionList);
            //正式
            List<HrmSalaryArchivesOption> archivesOptionList = setFixSalaryRecordBO.getSalary().stream().map(option -> {
                HrmSalaryArchivesOption hrmSalaryArchivesOption = new HrmSalaryArchivesOption();
                hrmSalaryArchivesOption.setEmployeeId(employeeId);
                hrmSalaryArchivesOption.setCode(option.getCode());
                hrmSalaryArchivesOption.setName(option.getName());
                hrmSalaryArchivesOption.setValue(option.getValue());
                hrmSalaryArchivesOption.setIsPro(0);
                return hrmSalaryArchivesOption;
            }).collect(Collectors.toList());
            hrmSalaryArchivesOptionService.saveBatch(archivesOptionList);
            save(hrmSalaryArchives);
            HrmSalaryChangeRecord salaryChangeRecord = new HrmSalaryChangeRecord();
            salaryChangeRecord.setId(fixSalaryRecordOpt.map(HrmSalaryChangeRecord::getId).orElse(null));
            salaryChangeRecord.setEmployeeId(employeeId);
            salaryChangeRecord.setCreateTime(LocalDateTime.now());
            salaryChangeRecord.setCreateUserId(Integer.parseInt(userInfo.getUserId()));
            salaryChangeRecord.setEmployeeStatus(employee.getStatus());
            salaryChangeRecord.setRecordType(SalaryChangeRecordTypeEnum.FIX_SALARY.getValue());
            salaryChangeRecord.setChangeReason(hrmSalaryArchives.getChangeReason());
            salaryChangeRecord.setEnableDate(LocalDate.now());
            salaryChangeRecord.setProAfterSum(setFixSalaryRecordBO.getProSum());
            salaryChangeRecord.setProSalary(JSON.toJSONString(setFixSalaryRecordBO.getProSalary()));
            salaryChangeRecord.setAfterSum(setFixSalaryRecordBO.getSum());
            salaryChangeRecord.setSalary(JSON.toJSONString(setFixSalaryRecordBO.getSalary()));
            salaryChangeRecord.setStatus(SalaryChangeRecordStatusEnum.HAS_TAKE_EFFECT.getValue());
            salaryChangeRecord.setRemarks(setFixSalaryRecordBO.getRemarks());
            if (employee.getStatus().equals(EmployeeStatusEnum.TRY_OUT.getValue())) {
                salaryChangeRecord.setAfterTotal(setFixSalaryRecordBO.getProSum());
            } else {
                salaryChangeRecord.setAfterTotal(setFixSalaryRecordBO.getSum());
            }
            salaryChangeRecordService.saveOrUpdate(salaryChangeRecord);
        }
        return 0;
    }


    /**
     * 查询员工薪资数据(包含试用期和转正数据，以薪资项形式呈现)
     * @param changeOptionValueDto
     * @return
     */
    public QueryChangeOptionValueVO queryChangeOptionValue(QueryChangeOptionValueDto changeOptionValueDto) {
        //查询出定薪/调薪模板
        HrmSalaryChangeTemplate changeTemplate = hrmSalaryChangeTemplateService.getById(changeOptionValueDto.getTemplateId());
        //根据模板查询出对应的薪资项
        List<ChangeSalaryOptionVO> changeSalaryOptions = JSON.parseArray(changeTemplate.getValue(), ChangeSalaryOptionVO.class);
        //查询出员工的薪资项与对应的金额(员工与薪资项关联数据)
        List<HrmSalaryArchivesOption> archivesOptions = hrmSalaryArchivesOptionService.lambdaQuery().eq(HrmSalaryArchivesOption::getEmployeeId, changeOptionValueDto.getEmployeeId()).list();
        Map<Integer, Map<Integer, String>> isProMap = archivesOptions.stream().collect(Collectors.groupingBy(HrmSalaryArchivesOption::getIsPro, Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue)));
        Map<Integer, String> proCodeValueMap = isProMap.get(1);
        Map<Integer, String> codeValueMap = isProMap.get(0);
        //转正后工资
        List<ChangeSalaryOptionVO> proSalary = changeSalaryOptions.stream().map(option -> {
            String value = Objects.isNull(proCodeValueMap.get(option.getCode())) ? "" : proCodeValueMap.get(option.getCode());
            return new ChangeSalaryOptionVO(option.getName(), option.getCode(), value);
        }).collect(Collectors.toList());
        //试用期工资
        List<ChangeSalaryOptionVO> salary = changeSalaryOptions.stream().map(option -> {
            String value = Objects.isNull(codeValueMap.get(option.getCode())) ? "" : codeValueMap.get(option.getCode());
            return new ChangeSalaryOptionVO(option.getName(), option.getCode(), value);
        }).collect(Collectors.toList());
        QueryChangeOptionValueVO data = new QueryChangeOptionValueVO();
        data.setProSalary(proSalary);
        data.setSalary(salary);
        return data;
    }

    /**
     * 查询员工的薪资档案详情
     * @param employeeId
     * @return
     */
    public QuerySalaryArchivesByIdVO querySalaryArchivesById(Long employeeId) {
        QuerySalaryArchivesByIdVO querySalaryArchivesByIdVO = new QuerySalaryArchivesByIdVO();
        HrmSalaryArchives archives = lambdaQuery().eq(HrmSalaryArchives::getEmployeeId, employeeId).one();
        if (archives == null) {
            return querySalaryArchivesByIdVO;
        }
        querySalaryArchivesByIdVO.setEmployeeId(employeeId);
        List<ChangeSalaryOptionVO> changeSalaryOptions = hrmSalaryChangeTemplateService.queryChangeSalaryOption();
        HrmEmployeeVO employee = hrmEmployeeMapper.getEmployeeEntryByEmpId(employeeId);
        Map<Integer, String> map;
        // 根据员工状态和转正日期来确定使用哪套薪资标准
        boolean isStillInProbation = employee.getStatus().equals(EmployeeStatusEnum.TRY_OUT.getValue());
        
        // 如果员工状态是试用期，则取is_pro=1的薪资项
        if (isStillInProbation) {
            map = hrmSalaryArchivesOptionService.lambdaQuery()
                .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                .eq(HrmSalaryArchivesOption::getIsPro, 1).list()
                .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
        } else {
            // 如果员工状态已转正，还需检查转正日期
            LocalDate becomeTime = employee.getBecomeTime();
            
            // 如果转正日期为空或当前日期早于转正日期，则仍取is_pro=1
            if (becomeTime == null || LocalDate.now().isBefore(becomeTime)) {
                map = hrmSalaryArchivesOptionService.lambdaQuery()
                    .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                    .eq(HrmSalaryArchivesOption::getIsPro, 1).list()
                    .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
            } else {
                // 否则取is_pro=0的薪资项
                map = hrmSalaryArchivesOptionService.lambdaQuery()
                    .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                    .eq(HrmSalaryArchivesOption::getIsPro, 0).list()
                    .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
            }
        }
        BigDecimal total = new BigDecimal(0);
        for (ChangeSalaryOptionVO option : changeSalaryOptions) {
            String value = Objects.isNull(map.get(option.getCode())) ? "0" : map.get(option.getCode());
            option.setValue(value);
            total = total.add(new BigDecimal(value));
        }
        querySalaryArchivesByIdVO.setTotal(total.toString());
        querySalaryArchivesByIdVO.setSalaryOptions(changeSalaryOptions);
        return querySalaryArchivesByIdVO;
    }


    /**
     * 单个调薪
     * @param setChangeSalaryRecordDto
     * @return
     */
    public int setChangeSalaryRecord(SetChangeSalaryRecordDto setChangeSalaryRecordDto) {
        //员工当前存在正在进行中的调薪任务，无法再次新增调薪任务
        LoginUserInfo info = CompanyContext.get();
        boolean isChange = salaryChangeRecordService.lambdaQuery().eq(HrmSalaryChangeRecord::getEmployeeId, setChangeSalaryRecordDto.getEmployeeId()).eq(HrmSalaryChangeRecord::getRecordType, SalaryChangeRecordTypeEnum.CHANGE_SALARY.getValue())
                .eq(HrmSalaryChangeRecord::getStatus, SalaryChangeRecordStatusEnum.NOT_TAKE_EFFECT.getValue()).ne(setChangeSalaryRecordDto.getId() != null, HrmSalaryChangeRecord::getId, setChangeSalaryRecordDto.getId())
                .count() > 0;
        if (isChange) {
            throw new HrmException(HrmCodeEnum.EXIST_CHANGE_RECORD);
        }
        HrmEmployeeVO employee = hrmEmployeeMapper.getEmployeeEntryByEmpId(setChangeSalaryRecordDto.getEmployeeId());
        HrmSalaryChangeRecord salaryChangeRecord = BeanUtil.copyProperties(setChangeSalaryRecordDto, HrmSalaryChangeRecord.class);
        salaryChangeRecord.setProSalary(JSON.toJSONString(setChangeSalaryRecordDto.getProSalary()));
        salaryChangeRecord.setSalary(JSON.toJSONString(setChangeSalaryRecordDto.getSalary()));
//        salaryChangeRecord.setStatus(SalaryChangeRecordStatusEnum.NOT_TAKE_EFFECT.getValue());
        //直接设置为已生效
        salaryChangeRecord.setStatus(SalaryChangeRecordStatusEnum.HAS_TAKE_EFFECT.getValue());
        salaryChangeRecord.setEmployeeStatus(employee.getStatus());
        salaryChangeRecord.setRecordType(SalaryChangeRecordTypeEnum.CHANGE_SALARY.getValue());
        salaryChangeRecord.setChangeReason(setChangeSalaryRecordDto.getChangeReason());
        BigDecimal beforeTotal = new BigDecimal(0);
        BigDecimal afterTotal = new BigDecimal(0);
        List<HrmSalaryArchivesOption> salaryArchivesOptions;
        List<ChangeSalaryOptionVO> newSalaryOptions;
        // 根据员工状态和转正日期来确定使用哪套薪资标准
        boolean isStillInProbation = employee.getStatus().equals(EmployeeStatusEnum.TRY_OUT.getValue());
        
        // 如果员工状态是试用期，则取is_pro=1的薪资项
        if (isStillInProbation) {
            salaryArchivesOptions = hrmSalaryArchivesOptionService.lambdaQuery()
                .eq(HrmSalaryArchivesOption::getEmployeeId, employee.getEmployeeId())
                .eq(HrmSalaryArchivesOption::getIsPro, 1).list();
            newSalaryOptions = setChangeSalaryRecordDto.getProSalary().getNewSalary();
        } else {
            // 如果员工状态已转正，还需检查转正日期
            LocalDate becomeTime = employee.getBecomeTime();
            
            // 如果转正日期为空或当前日期早于转正日期，则仍取is_pro=1
            if (becomeTime == null || LocalDate.now().isBefore(becomeTime)) {
                salaryArchivesOptions = hrmSalaryArchivesOptionService.lambdaQuery()
                    .eq(HrmSalaryArchivesOption::getEmployeeId, employee.getEmployeeId())
                    .eq(HrmSalaryArchivesOption::getIsPro, 1).list();
                newSalaryOptions = setChangeSalaryRecordDto.getProSalary().getNewSalary();
            } else {
                // 否则取is_pro=0的薪资项
                salaryArchivesOptions = hrmSalaryArchivesOptionService.lambdaQuery()
                    .eq(HrmSalaryArchivesOption::getEmployeeId, employee.getEmployeeId())
                    .eq(HrmSalaryArchivesOption::getIsPro, 0).list();
                newSalaryOptions = setChangeSalaryRecordDto.getSalary().getNewSalary();
            }
        }
        List<Integer> newCodeList = newSalaryOptions.stream().map(ChangeSalaryOptionVO::getCode).collect(Collectors.toList());
        for (HrmSalaryArchivesOption option : salaryArchivesOptions) {
            beforeTotal = beforeTotal.add(new BigDecimal(option.getValue()));
        }
        List<HrmSalaryArchivesOption> oldOptions = salaryArchivesOptions.stream().filter(option -> !newCodeList.contains(option.getCode())).collect(Collectors.toList());
        for (HrmSalaryArchivesOption option : oldOptions) {
            afterTotal = afterTotal.add(new BigDecimal(option.getValue()));
        }
        for (ChangeSalaryOptionVO newOption : newSalaryOptions) {
            afterTotal = afterTotal.add(new BigDecimal(newOption.getValue()));
        }
        salaryChangeRecord.setAfterTotal(afterTotal.toString());
        salaryChangeRecord.setBeforeTotal(beforeTotal.toString());
        salaryChangeRecord.setCreateUserId(Integer.parseInt(info.getUserId()));
        salaryChangeRecord.setCreateTime(LocalDateTime.now());
        salaryChangeRecordService.saveOrUpdate(salaryChangeRecord);
        return 0;
    }

    public List<QueryChangeRecordListVO> queryChangeRecordList(Long employeeId) {
        return hrmSalaryArchivesMapper.queryChangeRecordList(employeeId);
    }

    /**
     * 获取员工指定年月的工资项金额清单
     * @param employeeId
     * @param year
     * @param month
     * @return
     */
    public List<HrmSalaryArchivesOption> querySalaryArchivesOption(Long employeeId, int year, int month)
    {
        HrmEmployeeVO employee = hrmEmployeeMapper.getEmployeeById(employeeId);
        HrmSalaryArchives salaryArchives = lambdaQuery().eq(HrmSalaryArchives::getEmployeeId, employeeId).one();
        if (Objects.isNull(salaryArchives))
        {
            return new ArrayList<>();
        }
        Optional<HrmSalaryChangeRecord> salaryChangeRecordOpt = salaryChangeRecordService.lambdaQuery().eq(HrmSalaryChangeRecord::getEmployeeId, employeeId)
                .eq(HrmSalaryChangeRecord::getRecordType, SalaryChangeRecordTypeEnum.CHANGE_SALARY.getValue())
                .eq(HrmSalaryChangeRecord::getStatus, SalaryChangeRecordStatusEnum.NOT_TAKE_EFFECT.getValue())
                .apply("year(enable_date) = {0} and month(enable_date) = {1}", year, month).oneOpt();
        if (salaryChangeRecordOpt.isPresent())
        {
            //存在未生效的调薪记录
            HrmSalaryChangeRecord salaryChangeRecord = salaryChangeRecordOpt.get();
            String equalsKey = "null";
            if (StrUtil.isNotEmpty(salaryChangeRecord.getProSalary()) && !equalsKey.equals(salaryChangeRecord.getProSalary()))
            {
                ChangeSalaryRecordVO proSalary = JSON.parseObject(salaryChangeRecord.getProSalary(), ChangeSalaryRecordVO.class);
                List<ChangeSalaryOptionVO> newSalary = proSalary.getNewSalary();
                List<Integer> removeCode = newSalary.stream().map(ChangeSalaryOptionVO::getCode).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(removeCode))
                {
                    hrmSalaryArchivesOptionService.lambdaUpdate().eq(HrmSalaryArchivesOption::getIsPro, 1).eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                            .in(HrmSalaryArchivesOption::getCode, removeCode)
                            .remove();
                }
                List<HrmSalaryArchivesOption> archivesOptionList = newSalary.stream().map(option -> {
                    HrmSalaryArchivesOption archivesOption = new HrmSalaryArchivesOption();
                    archivesOption.setEmployeeId(employeeId);
                    archivesOption.setIsPro(1);
                    archivesOption.setCode(option.getCode());
                    archivesOption.setName(option.getName());
                    archivesOption.setValue(option.getValue());
                    return archivesOption;
                }).collect(Collectors.toList());
                hrmSalaryArchivesOptionService.saveBatch(archivesOptionList);
            }
            if (StrUtil.isNotEmpty(salaryChangeRecord.getSalary())) {
                ChangeSalaryRecordVO salary = JSON.parseObject(salaryChangeRecord.getSalary(), ChangeSalaryRecordVO.class);
                List<ChangeSalaryOptionVO> newSalary = salary.getNewSalary();
                List<Integer> removeCode = newSalary.stream().map(ChangeSalaryOptionVO::getCode).collect(Collectors.toList());
                hrmSalaryArchivesOptionService.lambdaUpdate().eq(HrmSalaryArchivesOption::getIsPro, 0).eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                        .in(HrmSalaryArchivesOption::getCode, removeCode)
                        .remove();
                List<HrmSalaryArchivesOption> archivesOptionList = newSalary.stream().map(option -> {
                    HrmSalaryArchivesOption archivesOption = new HrmSalaryArchivesOption();
                    archivesOption.setEmployeeId(employeeId);
                    archivesOption.setIsPro(0);
                    archivesOption.setCode(option.getCode());
                    archivesOption.setName(option.getName());
                    archivesOption.setValue(option.getValue());
                    return archivesOption;
                }).collect(Collectors.toList());
                hrmSalaryArchivesOptionService.saveBatch(archivesOptionList);
                salaryChangeRecord.setStatus(SalaryChangeRecordStatusEnum.HAS_TAKE_EFFECT.getValue());
                salaryChangeRecordService.updateById(salaryChangeRecord);
            }
            salaryArchives.setChangeType(2);
            salaryArchives.setChangeReason(salaryChangeRecord.getChangeReason());
            salaryArchives.setRemarks(salaryChangeRecord.getRemarks());
            salaryArchives.setChangeData(salaryChangeRecord.getEnableDate());
            updateById(salaryArchives);
        }
        List<HrmSalaryArchivesOption> salaryArchivesOptions;
        // 根据员工状态和转正日期来确定使用哪套薪资标准
        boolean isStillInProbation = employee.getStatus().equals(EmployeeStatusEnum.TRY_OUT.getValue());
        
        // 如果员工状态是试用期，则取is_pro=1的薪资项
        if (isStillInProbation) {
            salaryArchivesOptions = hrmSalaryArchivesOptionService.lambdaQuery()
                .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                .eq(HrmSalaryArchivesOption::getIsPro, 1).list();
        } else {
            // 如果员工状态已转正，还需检查转正日期是否已到
            LocalDate becomeTime = employee.getBecomeTime();
            YearMonth targetYearMonth = YearMonth.of(year, month);
            
            // 如果转正日期为空或转正日期的年月大于当前薪资计算年月，则仍取is_pro=1
            if (becomeTime == null || YearMonth.from(becomeTime).isAfter(targetYearMonth)) {
                salaryArchivesOptions = hrmSalaryArchivesOptionService.lambdaQuery()
                    .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                    .eq(HrmSalaryArchivesOption::getIsPro, 1).list();
            } else {
                // 否则取is_pro=0的薪资项
                salaryArchivesOptions = hrmSalaryArchivesOptionService.lambdaQuery()
                    .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                    .eq(HrmSalaryArchivesOption::getIsPro, 0).list();
            }
        }
        return salaryArchivesOptions;
    }


    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchChangeSalaryRecord(BatchChangeSalaryRecordBO batchChangeSalaryRecordBO) {
        List<Long> employeeIds = new ArrayList<>(batchChangeSalaryRecordBO.getEmployeeIds());
        if (CollUtil.isNotEmpty(batchChangeSalaryRecordBO.getDeptIds())) {
            employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).in(HrmEmployee::getDeptId, batchChangeSalaryRecordBO.getDeptIds()).list()
                    .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));
        }
        int errorNum = 0;
        List<ChangeSalaryOptionVO> changeSalaryOptions = batchChangeSalaryRecordBO.getSalaryOptions();
        List<Integer> changeCodes = changeSalaryOptions.stream().map(ChangeSalaryOptionVO::getCode).collect(Collectors.toList());

        List<OperationLog> operationLogs = new ArrayList<>();
        for (Long employeeId : employeeIds) {
            boolean exists = lambdaQuery().eq(HrmSalaryArchives::getEmployeeId, employeeId).exists();
            if (!exists) {
                errorNum++;
                continue;
            }
            HrmEmployee employee = employeeService.getById(employeeId);

            OperationLog operationLog = new OperationLog();
            operationLog.setOperationObject(employee.getEmployeeId(), employee.getEmployeeName());

            SetChangeSalaryRecordDto setChangeSalaryRecordBO = new SetChangeSalaryRecordDto();
            // 根据员工状态和转正日期来确定使用哪套薪资标准
            boolean isStillInProbation = employee.getStatus().equals(EmployeeStatusEnum.TRY_OUT.getValue());
                        
            // 如果员工状态是试用期，则使用is_pro=1的薪资项
            if (isStillInProbation) {
                Map<Integer, String> codeValueMap = hrmSalaryArchivesOptionService.lambdaQuery().eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                        .eq(HrmSalaryArchivesOption::getIsPro, 1).in(HrmSalaryArchivesOption::getCode, changeCodes).list()
                        .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
                ChangeSalaryRecordVO proSalary = new ChangeSalaryRecordVO();
                BigDecimal proBeforeSum = new BigDecimal(0);
                BigDecimal proAfterSum = new BigDecimal(0);
                List<ChangeSalaryOptionVO> proOldSalary = new ArrayList<>();
                List<ChangeSalaryOptionVO> proNewSalary = new ArrayList<>();
                for (ChangeSalaryOptionVO option : changeSalaryOptions)
                {
                    ChangeSalaryOptionVO oldOption = new ChangeSalaryOptionVO();
                    oldOption.setName(option.getName());
                    oldOption.setCode(option.getCode());
                    String oldValueStr = Optional.ofNullable(codeValueMap.get(option.getCode())).orElse("0");
                    oldOption.setValue(oldValueStr);
                    proOldSalary.add(oldOption);
                    BigDecimal oldValue = new BigDecimal(oldValueStr);
                    proBeforeSum = proBeforeSum.add(oldValue);
                    ChangeSalaryOptionVO newOption = new ChangeSalaryOptionVO();
                    newOption.setName(option.getName());
                    newOption.setCode(option.getCode());
                    BigDecimal newValue;
                    if (batchChangeSalaryRecordBO.getType() == 1) {
                        newValue = oldValue.add(oldValue.multiply(new BigDecimal(option.getValue()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_UP)));
                    } else {
                        newValue = oldValue.add(new BigDecimal(option.getValue()));
                    }
                    newOption.setValue(newValue.toString());
                    proNewSalary.add(newOption);
                    proAfterSum = proAfterSum.add(newValue);
                }
                proSalary.setOldSalary(proOldSalary);
                proSalary.setNewSalary(proNewSalary);
                setChangeSalaryRecordBO.setProSalary(proSalary);
                setChangeSalaryRecordBO.setProBeforeSum(proBeforeSum.toString());
                setChangeSalaryRecordBO.setProAfterSum(proAfterSum.toString());
            } else {
                // 如果员工状态已转正，还需检查转正日期
                LocalDate becomeTime = employee.getBecomeTime();
                            
                // 如果转正日期为空或当前日期早于转正日期，则使用is_pro=1的薪资项
                if (becomeTime == null || LocalDate.now().isBefore(becomeTime)) {
                    Map<Integer, String> codeValueMap = hrmSalaryArchivesOptionService.lambdaQuery().eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                            .eq(HrmSalaryArchivesOption::getIsPro, 1).in(HrmSalaryArchivesOption::getCode, changeCodes).list()
                            .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
                    ChangeSalaryRecordVO proSalary = new ChangeSalaryRecordVO();
                    BigDecimal proBeforeSum = new BigDecimal(0);
                    BigDecimal proAfterSum = new BigDecimal(0);
                    List<ChangeSalaryOptionVO> proOldSalary = new ArrayList<>();
                    List<ChangeSalaryOptionVO> proNewSalary = new ArrayList<>();
                    for (ChangeSalaryOptionVO option : changeSalaryOptions)
                    {
                        ChangeSalaryOptionVO oldOption = new ChangeSalaryOptionVO();
                        oldOption.setName(option.getName());
                        oldOption.setCode(option.getCode());
                        String oldValueStr = Optional.ofNullable(codeValueMap.get(option.getCode())).orElse("0");
                        oldOption.setValue(oldValueStr);
                        proOldSalary.add(oldOption);
                        BigDecimal oldValue = new BigDecimal(oldValueStr);
                        proBeforeSum = proBeforeSum.add(oldValue);
                        ChangeSalaryOptionVO newOption = new ChangeSalaryOptionVO();
                        newOption.setName(option.getName());
                        newOption.setCode(option.getCode());
                        BigDecimal newValue;
                        if (batchChangeSalaryRecordBO.getType() == 1) {
                            newValue = oldValue.add(oldValue.multiply(new BigDecimal(option.getValue()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_UP)));
                        } else {
                            newValue = oldValue.add(new BigDecimal(option.getValue()));
                        }
                        newOption.setValue(newValue.toString());
                        proNewSalary.add(newOption);
                        proAfterSum = proAfterSum.add(newValue);
                    }
                    proSalary.setOldSalary(proOldSalary);
                    proSalary.setNewSalary(proNewSalary);
                    setChangeSalaryRecordBO.setProSalary(proSalary);
                    setChangeSalaryRecordBO.setProBeforeSum(proBeforeSum.toString());
                    setChangeSalaryRecordBO.setProAfterSum(proAfterSum.toString());
                }
            }
            // 根据员工状态和转正日期来确定使用哪套薪资标准
            Map<Integer, String> codeValueMap;
            if (isStillInProbation) {
                codeValueMap = hrmSalaryArchivesOptionService.lambdaQuery().eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                        .eq(HrmSalaryArchivesOption::getIsPro, 1).in(HrmSalaryArchivesOption::getCode, changeCodes).list()
                        .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
            } else {
                LocalDate becomeTime = employee.getBecomeTime();
                // 如果转正日期为空或当前日期早于转正日期，则仍使用is_pro=1的数据
                if (becomeTime == null || LocalDate.now().isBefore(becomeTime)) {
                    codeValueMap = hrmSalaryArchivesOptionService.lambdaQuery().eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                            .eq(HrmSalaryArchivesOption::getIsPro, 1).in(HrmSalaryArchivesOption::getCode, changeCodes).list()
                            .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
                } else {
                    codeValueMap = hrmSalaryArchivesOptionService.lambdaQuery().eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                            .eq(HrmSalaryArchivesOption::getIsPro, 0).in(HrmSalaryArchivesOption::getCode, changeCodes).list()
                            .stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
                }
            }
            ChangeSalaryRecordVO salary = new ChangeSalaryRecordVO();
            BigDecimal beforeSum = new BigDecimal(0);
            BigDecimal afterSum = new BigDecimal(0);
            List<ChangeSalaryOptionVO> oldSalary = new ArrayList<>();
            List<ChangeSalaryOptionVO> newSalary = new ArrayList<>();
            for (ChangeSalaryOptionVO option : changeSalaryOptions) {
                ChangeSalaryOptionVO oldOption = new ChangeSalaryOptionVO();
                oldOption.setName(option.getName());
                oldOption.setCode(option.getCode());
                String oldValueStr = Optional.ofNullable(codeValueMap.get(option.getCode())).orElse("0");
                oldOption.setValue(oldValueStr);
                oldSalary.add(oldOption);
                BigDecimal oldValue = new BigDecimal(oldValueStr);
                beforeSum = beforeSum.add(oldValue);
                ChangeSalaryOptionVO newOption = new ChangeSalaryOptionVO();
                newOption.setName(option.getName());
                newOption.setCode(option.getCode());
                BigDecimal newValue;
                if (batchChangeSalaryRecordBO.getType() == 1) {
                    newValue = oldValue.add(oldValue.multiply(new BigDecimal(option.getValue()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_UP)));
                } else {
                    newValue = oldValue.add(new BigDecimal(option.getValue()));
                }
                newOption.setValue(newValue.toString());
                newSalary.add(newOption);
                afterSum = afterSum.add(newValue);
            }
            salary.setOldSalary(oldSalary);
            salary.setNewSalary(newSalary);
            setChangeSalaryRecordBO.setEmployeeId(employeeId);
            setChangeSalaryRecordBO.setSalary(salary);
            setChangeSalaryRecordBO.setBeforeSum(beforeSum.toString());
            setChangeSalaryRecordBO.setAfterSum(afterSum.toString());
            setChangeSalaryRecordBO.setRemarks(batchChangeSalaryRecordBO.getRemarks());
            setChangeSalaryRecordBO.setChangeReason(batchChangeSalaryRecordBO.getChangeReason());
            setChangeSalaryRecordBO.setEnableDate(batchChangeSalaryRecordBO.getEnableDate());
            try {
                setChangeSalaryRecord(setChangeSalaryRecordBO);

                OperationLog log = new OperationLog();
                log.setOperationInfo("员工:"+employee.getEmployeeName()+"调薪成功");
                operationLogs.add(log);
            } catch (HrmException e) {
                e.printStackTrace();
                errorNum++;
            }
        }
        Map<String, Object> result = new HashMap<>(2);
        result.put("errorNum", errorNum);
        result.put("successNum", employeeIds.size() - errorNum);
        result.put("operationLog", operationLogs);
        return result;
    }

}
