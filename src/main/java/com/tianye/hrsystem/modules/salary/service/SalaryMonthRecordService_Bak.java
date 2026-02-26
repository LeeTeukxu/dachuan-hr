package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.tianye.hrsystem.autoTask.AttendanceReportRefreshTask;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.po.HrmAttendanceGroup;
import com.tianye.hrsystem.entity.po.HrmAttendanceRule;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.vo.*;
import com.tianye.hrsystem.enums.*;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.mapper.HrmAttendancePlanMapper;
import com.tianye.hrsystem.mapper.HrmAttendanceShiftMapper;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.mapper.HrmProduceAttendanceMapper;
import com.tianye.hrsystem.modules.holiday.bo.QueryHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.service.HrmHolidayDeductionService;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthRecord;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthEmpRecordService;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthRecordService;
import com.tianye.hrsystem.modules.salary.dto.*;
import com.tianye.hrsystem.modules.salary.entity.*;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthEmpRecordMapper;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthRecordMapper;
import com.tianye.hrsystem.modules.salary.vo.SalaryOptionHeadVO;
import com.tianye.hrsystem.modules.salary.vo.*;
import com.tianye.hrsystem.service.IHrmAttendanceClockService;
import com.tianye.hrsystem.service.IHrmAttendanceGroupService;
import com.tianye.hrsystem.service.IHrmAttendanceReportDataService;
import com.tianye.hrsystem.service.IHrmAttendanceRuleService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import com.tianye.hrsystem.util.TransferUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 每月薪资记录 服务实现类
 */
@Service
public class SalaryMonthRecordService_Bak extends BaseServiceImpl<HrmSalaryMonthRecordMapper, HrmSalaryMonthRecord> {

    private Logger logger = LoggerFactory.getLogger(AttendanceReportRefreshTask.class);


    @Autowired
    private HrmSalaryMonthEmpRecordMapper salaryMonthEmpRecordMapper;

    @Autowired
    private HrmSalaryMonthRecordMapper salaryMonthRecordMapper;

    @Autowired
    private HrmEmployeeMapper hrmEmployeeMapper;
    @Autowired
    private HrmSalaryMonthOptionValueService salaryMonthOptionValueService;

    @Autowired
    private HrmSalaryArchivesService salaryArchivesService;

    @Autowired
    private HrmSalaryMonthEmpRecordService salaryMonthEmpRecordService;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    private HrmSalaryGroupService hrmSalaryGroupService;

    @Autowired
    private IHrmSalaryConfigService hrmSalaryConfigService;

    @Autowired
    private HrmSalaryOptionService hrmSalaryOptionService;

    @Autowired
    private SalaryComputeService_Bak salaryComputeService;

    @Autowired
    private SalaryActionRecordService salaryActionRecordService;

    @Autowired
    private HrmInsuranceMonthRecordService insuranceMonthRecordService;

    @Autowired
    private IHrmAttendanceClockService attendanceClockService;

    @Autowired
    private IHrmAttendanceRuleService attendanceRuleService;

    @Autowired
    private IHrmAttendanceGroupService attendanceGroupService;

    @Autowired
    private HrmInsuranceMonthEmpRecordService insuranceMonthEmpRecordService;

    @Autowired
    private IHrmAttendanceRuleService hrmAttendanceRuleService;

    @Autowired
    private IHrmAttendanceReportDataService attendanceReportDataService;

    @Autowired
    private HrmSalaryBasicService hrmSalaryBasicService;

    @Autowired
    private HrmAttendanceShiftMapper attendanceShiftMapper;

    @Autowired
    private HrmProduceAttendanceMapper produceAttendanceMapper;

    @Autowired
    private HrmAttendancePlanMapper planMapper;

    @Autowired
    private HrmHolidayDeductionService holidayDeductionService;



    private static final int TWO = 2;

    private static final int FIVE = 5;

    private static final int FOUR = 4;

    private static final int STATUS = 11;

    private static final int ZERO = 0;

    private static final int THREE = 3;

    private static final int ONE = 1;

    /**
     * （薪资管理）查询薪资列表
     *
     * @param querySalaryPageListDto
     * @return
     */
    public BasePage<QuerySalaryPageListVO> querySalaryPageList(QuerySalaryPageListDto querySalaryPageListDto) {
        List<Long> employeeIds = new ArrayList<>();

        //查询出已经定薪了的人员列表

        employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).ne(HrmEmployee::getIsDel, 1).list()
                .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));

        BasePage<QuerySalaryPageListVO> page = salaryMonthEmpRecordMapper.querySalaryPageList(querySalaryPageListDto.parse(), querySalaryPageListDto,employeeIds);
        if (CollectionUtil.isEmpty(page.getList())) {
            return null;
        }
        page.getList().forEach(querySalaryPageListVO -> {
            List<ComputeSalaryDto> list = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(querySalaryPageListVO.getSEmpRecordId());
            List<QuerySalaryPageListVO.SalaryValue> salaryValues = TransferUtil.transferList(list, QuerySalaryPageListVO.SalaryValue.class);
//            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 1, querySalaryPageListVO.getNeedWorkDay().toString(), 1, "计薪天数"));
//            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, querySalaryPageListVO.getActualWorkDay().toString(), 1, "实际计薪天数"));

            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 1, querySalaryPageListVO.getNeedWorkDay().toString(), 1, "应出勤天数"));
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, querySalaryPageListVO.getActualWorkDay().toString(), 1, "实际出勤天数"));

            querySalaryPageListVO.setSalary(salaryValues);
        });

        List<Long> sEmpRecordIds = salaryMonthEmpRecordMapper.querysEmpRecordIds(querySalaryPageListDto, employeeIds);
        if (sEmpRecordIds.size() == 0) {
            sEmpRecordIds.add(0L);
        }
        List<Map<String, Object>> salaryOption = querySalaryByIds(sEmpRecordIds, querySalaryPageListDto.getSRecordId());
        JSONObject json = new JSONObject();
        json.put("salaryOption", salaryOption);
        page.setExtraData(json);
        return page;


    }


    private List<Map<String, Object>> querySalaryByIds(List<Long> sEmpRecordIds, Long sRecordId) {
        return salaryMonthRecordMapper.querySalaryByIds(sEmpRecordIds, sRecordId);
    }

    /**
     * 查询每月薪资记录列表
     * @param querySalaryMonthRecordDto
     * @return
     */
    public Page<QuerySalaryMonthRecordVO> querySalaryMonthRecordList(QuerySalaryMonthRecordDto querySalaryMonthRecordDto)
    {
        Page<QuerySalaryMonthRecordVO> page = salaryMonthRecordMapper.querySlipEmployeePageList(querySalaryMonthRecordDto.parse(), querySalaryMonthRecordDto);
        return page;
    }

    /**
     * 获取非固定性value(除了社保项)
     * 90101 个人社保
     * 90102  个人公积金
     * 100101  企业社保
     * 110101  企业公积金
     *
     * @return
     */
    private List<HrmSalaryMonthOptionValue> getNoFixedOptionValue(HrmSalaryMonthEmpRecord salaryMonthEmpRecord, List<HrmSalaryOption> noFixedSalaryOptionList, boolean isNew) {
        //移除社保公积金项
        noFixedSalaryOptionList.removeIf(salaryOption ->
                salaryOption.getCode().equals(100101) || salaryOption.getCode().equals(100102)
                        || salaryOption.getCode().equals(110101) || salaryOption.getCode().equals(120101)
        );
        //移除全勤奖 项,其他扣款，其他补贴
        noFixedSalaryOptionList.removeIf(salaryOption ->
                salaryOption.getCode().equals(40102) || salaryOption.getCode().equals(280)
                        || salaryOption.getCode().equals(281) || salaryOption.getCode().equals(282)
        );
        List<HrmSalaryMonthOptionValue> noFixedOptionValueList = new ArrayList<>();
        if (isNew) {
            //从薪资档案中获取员工对应的工资项和对应的值
            List<HrmSalaryArchivesOption> archivesOptionList = salaryArchivesService.querySalaryArchivesOption(salaryMonthEmpRecord.getEmployeeId(), salaryMonthEmpRecord.getYear(), salaryMonthEmpRecord.getMonth());
            Map<Integer, String> optionValueCodeMap = archivesOptionList.stream().collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue));
            BigDecimal all = new BigDecimal(0);
            if (CollUtil.isNotEmpty(noFixedSalaryOptionList)) {
                for (HrmSalaryOption salaryOption : noFixedSalaryOptionList) {
                    String value = "0";
                    if (StrUtil.isNotEmpty(optionValueCodeMap.get(salaryOption.getCode()))) {
                        value = optionValueCodeMap.get(salaryOption.getCode());
                    }
                    HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                    salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
                    salaryMonthOptionValue.setCode(salaryOption.getCode());
                    salaryMonthOptionValue.setValue(value);
                    all = all.add(new BigDecimal(value));
                    noFixedOptionValueList.add(salaryMonthOptionValue);
                }
            }
            // 未设定薪资档案
            if (all.compareTo(BigDecimal.ZERO) == 0) {
                DateTime date = DateUtil.offsetMonth(DateUtil.parse(salaryMonthEmpRecord.getYear() + "-" + salaryMonthEmpRecord.getMonth(), "yy-MM"), -1);
                Optional<HrmSalaryMonthEmpRecord> salaryMonthEmpRecordOpt = salaryMonthEmpRecordService.lambdaQuery()
                        .eq(HrmSalaryMonthEmpRecord::getYear, date.year())
                        .eq(HrmSalaryMonthEmpRecord::getMonth, date.month() + 1).eq(HrmSalaryMonthEmpRecord::getEmployeeId, salaryMonthEmpRecord.getEmployeeId()).oneOpt();
                if (salaryMonthEmpRecordOpt.isPresent()) {
                    noFixedOptionValueList.clear();
                    List<HrmSalaryMonthOptionValue> oldOptionValueList = salaryMonthOptionValueService.lambdaQuery()
                            .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecordOpt.get().getSEmpRecordId()).list();
                    Map<Integer, String> oldValueCodeMap = oldOptionValueList.stream().collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, HrmSalaryMonthOptionValue::getValue, (k1, k2) -> k1));
                    noFixedSalaryOptionList.forEach(salaryOption -> {
                        String value = "0";
                        if (StrUtil.isNotEmpty(oldValueCodeMap.get(salaryOption.getCode()))) {
                            value = oldValueCodeMap.get(salaryOption.getCode());
                        }
                        HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                        salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
                        salaryMonthOptionValue.setCode(salaryOption.getCode());
                        salaryMonthOptionValue.setValue(value);
                        noFixedOptionValueList.add(salaryMonthOptionValue);
                    });
                }
            }
        } else {
            List<HrmSalaryMonthOptionValue> oldOptionValueList = salaryMonthOptionValueService.lambdaQuery()
                    .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).list();
            Set<Integer> oldCodeSet = new HashSet<>();
            oldOptionValueList.forEach(oldOptionValue -> {
                oldCodeSet.add(oldOptionValue.getCode());
            });
            noFixedSalaryOptionList.forEach(salaryOption -> {
                if (!oldCodeSet.contains(salaryOption.getCode())) {
                    HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                    salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
                    salaryMonthOptionValue.setCode(salaryOption.getCode());
                    salaryMonthOptionValue.setValue("0");
                    noFixedOptionValueList.add(salaryMonthOptionValue);
                }
            });
        }
        return noFixedOptionValueList;
    }



    /**
     * 核算薪资数据
     * @param sRecordId
     * @param isSyncInsuranceData
     * @param isSyncAttendanceData
     * @param attendanceFile
     * @param additionalDeductionFile
     * @param cumulativeTaxOfLastMonthFile
     */
    @Transactional
    public void computeSalaryData(Long sRecordId, Boolean isSyncInsuranceData, Boolean isSyncAttendanceData,
                                  MultipartFile attendanceFile, MultipartFile additionalDeductionFile,
                                  MultipartFile cumulativeTaxOfLastMonthFile)
    {
        HrmSalaryMonthRecord salaryMonthRecord = getById(sRecordId);
        HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
        int year = salaryMonthRecord.getYear();
        int month = salaryMonthRecord.getMonth();
        if (isSyncInsuranceData)
        {
            //如果是同步社保数据
            Integer socialSecurityMonthType = salaryConfig.getSocialSecurityMonthType();
            DateTime date = DateUtil.parse(year + "-" + month, "yy-MM");
            if (socialSecurityMonthType == 0) {
                date = DateUtil.offsetMonth(DateUtil.parse(year + "-" + month, "yy-MM"), -1);
            } else if (socialSecurityMonthType == 2) {
                date = DateUtil.offsetMonth(DateUtil.parse(year + "-" + month, "yy-MM"), 1);
            }
            //查询社保数据是否生成
            Optional<HrmInsuranceMonthRecord> insuranceMonthRecordOpt = insuranceMonthRecordService.lambdaQuery()
                    .eq(HrmInsuranceMonthRecord::getYear, date.year())
                    .eq(HrmInsuranceMonthRecord::getMonth, date.month() + 1).oneOpt();
            if (!insuranceMonthRecordOpt.isPresent())
            {
                throw new HrmException(HrmCodeEnum.SOCIAL_SECURITY_DATA_IS_NOT_GENERATED_THIS_MONTH);
            }
        }
        //查询计薪员工列表
        List<Map<String, Object>> mapList = queryHasSalaryArchivesEmployeeList();
        salaryMonthRecord.setNum(mapList.size());
        salaryMonthRecord.setCheckStatus(SalaryRecordStatus.CREATED.getValue());

        //考勤数据map
        Map<String, Map<Integer, String>> attendanceDataMap;
        try {
            attendanceDataMap = resolveAttendanceData(attendanceFile);
        } catch (Exception e) {
            throw new HrmException(HrmCodeEnum.ATTENDANCE_DATA_ERROR);
        }
        if (isSyncAttendanceData) {
            attendanceDataMap.clear();
            LocalDate dateStartTime = DateUtil.beginOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
            LocalDate dateEndTime = DateUtil.endOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
            List<String> dates = attendanceClockService.findDates(dateStartTime, dateEndTime);
            LocalDate date = LocalDate.of(year, month, 1);
            //算出每月的天数
            int daysInMonth = date.lengthOfMonth();
            List<Long> employeeIds = new ArrayList<>();
            for (Map<String, Object> map : mapList)
            {
                //查询出员工ID列表
                Long employeeId = Convert.toLong(map.get("employeeId"));
                employeeIds.add(employeeId);
            }
            if (CollUtil.isNotEmpty(employeeIds))
            {
                HashMap<String,Object> queryOvertimeParams = new HashMap<>();
                queryOvertimeParams.put("year",year);
                queryOvertimeParams.put("month",month);
                //获取 员工 加班费,夜班补贴数据 以及其他扣款，补贴数据(从导入的数据里面取)
                List<HrmProduceAttendance> hasOverTimePayEmpList = produceAttendanceMapper.getOvertimeAllowanceStatistics(queryOvertimeParams);

                //查询基本工资设置，比如夜班补贴费用，小时加班费，基本工资等
                HrmSalaryBasic salaryBasic = hrmSalaryBasicService.lambdaQuery().orderByDesc(HrmSalaryBasic::getCreateTime).last("limit 1").one();

                //考勤规则
                Map<Long, HrmAttendanceRule> hAttendanceRuleMap =
                        attendanceRuleService.list().stream().collect(Collectors.toMap(HrmAttendanceRule::getAttendanceRuleId, Function.identity()));
                LocalDateTime startDateTime = dateStartTime.atStartOfDay();
                LocalDateTime endDateTime = LocalDateTimeUtil.endOfDay(dateEndTime.atStartOfDay());

                Date beginDate = Date.from(startDateTime.atZone( ZoneId.systemDefault()).toInstant());
                Date endDate = Date.from(endDateTime.atZone( ZoneId.systemDefault()).toInstant());
                //这是查询时间范围内的 考勤情况参数
                HashMap<String,Object> params = new HashMap<>();
                params.put("beginDate",beginDate);
                params.put("endDate",endDate);
                //查询哪些人 能拿夜间补贴
//                params.put("checkHour",3);//3表示下班时间 为凌晨三点

                //获取行政考勤天数(未设置考勤组人员使用该值)
                Integer daysOfAdminDept = planMapper.getAdministrationAttendanceDays(params);

                //每日 员工考勤统计数据
                List<HrmAttendanceSummaryDayVo> attendanceSummaryVoDayList = attendanceReportDataService.getEmpAttendanceSummaryDayList(params);

                //每月 员工考勤统计数据
                List<HrmAttendanceSummaryVo> attendanceSummaryVoList = attendanceReportDataService.getEmpAttendanceSummaryList(params);

                //查出员工的薪资档案
                QuerySalaryArchivesListDto querySalaryArchivesListDto = new QuerySalaryArchivesListDto();
                querySalaryArchivesListDto.setEmployeeIds(employeeIds);
                List<QuerySalaryArchivesListVO> empSalaryArchivesList = salaryArchivesService.queryEmpSalaryArchivesList(querySalaryArchivesListDto);

                for (Map<String, Object> map : mapList)
                {
                    // 休息天数
                    Integer restDays = ZERO;
                    // 应出勤天数
                    Double normalDays = daysOfAdminDept!=null?daysOfAdminDept:21.75;
                    // 迟到分钟
                    Integer lateMinute = ZERO;
                    // 迟到次数
                    Integer lateCount = ZERO;
                    // 早退分钟
                    Integer earlyMinute = ZERO;
                    // 早退次数
                    Integer earlyCount = ZERO;
                    // 缺卡次数
                    Integer misscardCount = ZERO;
                    // 加班次数
                    Integer overTimeCount=ZERO;
                    //加班时长
                    Integer overTimeHours=ZERO;
                    // 请假天数
                    BigDecimal leaveDays = new BigDecimal(ZERO);
                    // 旷工天数
                    BigDecimal absenteeismDays = new BigDecimal(0);
                    //事假天数
                    BigDecimal leaveOfAbsenceDays =new BigDecimal(ZERO);;
                    //病假天数
                    BigDecimal leaveOfsickDays = new BigDecimal(ZERO);
                    //是否为全勤
                    boolean isFullAttendance = false;
                    //员工的定薪
                    BigDecimal empSalary =new BigDecimal(ZERO);
                    //员工每天的工资额
                    BigDecimal empDaySalary = new BigDecimal(ZERO);

                    Map<String, Object> attendanceEmpRecordMap = new HashMap<>();
                    Map<Integer, String> empAttendanceMap = new HashMap<>();
                    Long employeeId = Convert.toLong(map.get("employeeId"));
                    String jobNumber = (String) map.get("jobNumber");
                    Integer status = (Integer)map.get("status");//员工状态 1正式 2试用
                    String post = (String) map.get("post");//职位
                    Long fullMoney = (Long)map.get("fullMoney");//员工对应的全勤奖 金额
                    String isProduceDept = (String) map.get("isProduceDept");//员工部门是否为生产类型部门
                    boolean isProduce = isProduceDept.equals("0")?false:true;
                    //其他扣款
                    BigDecimal otherDeduction = new BigDecimal(ZERO);
                    //其他补贴
                    BigDecimal otherSubsidy = new BigDecimal(ZERO);
                    //借款
                    BigDecimal loanMoney = new BigDecimal(ZERO);
                    HrmProduceAttendance hrmProduceAttendance = hasOverTimePayEmpList.stream().filter(f -> f.getEmployeeId()!=null && f.getEmployeeId().toString().equals(employeeId.toString())).findAny().orElse(null);
                    if(hrmProduceAttendance!=null)
                    {
                        otherDeduction = hrmProduceAttendance.getOtherDeductions();
                        otherSubsidy = hrmProduceAttendance.getOtherSubsidies();
                        loanMoney = hrmProduceAttendance.getLoan();
                    }
                    //查询员工所属考勤组,比如领导 是不用打卡的
                    HrmAttendanceGroup hrmAttendanceGroup = attendanceGroupService.queryAttendanceGroupDingDing(employeeId);
                    if(hrmAttendanceGroup==null)
                    {
                        //如果员工没有放在考勤组，则工资计算排除考勤数据
                        empAttendanceMap.put(1, hrmProduceAttendance!=null?String.valueOf(hrmProduceAttendance.getPositiveAttendance()):"21.75");
                        empAttendanceMap.put(2, hrmProduceAttendance!=null?String.valueOf(hrmProduceAttendance.getPositiveAttendance()):"21.75");
                        empAttendanceMap.put(280, otherDeduction!=null?otherDeduction.toString():"0");//其他扣款
                        empAttendanceMap.put(281, otherSubsidy!=null ? otherSubsidy.toString():"0");//其他补贴
                        empAttendanceMap.put(282, otherSubsidy!=null ? loanMoney.toString():"0");//借款
                        empAttendanceMap.put(40102, fullMoney==null?"0":String.valueOf(fullMoney));//全勤奖
                        attendanceDataMap.put(jobNumber, empAttendanceMap);
                        continue;
                    }
                    //员工的定薪
                    Optional<QuerySalaryArchivesListVO> salaryArchivesOption  = empSalaryArchivesList.stream().filter(f -> f.getEmployeeId().equals(employeeId)).findFirst();
                    if(salaryArchivesOption.isPresent())
                    {
                        empSalary = new BigDecimal(Double.parseDouble(salaryArchivesOption.get().getTotal()));
                    }
                    //员工当月排班时长
                    HashMap<String,Double> workTimsMap = getWorkHours(employeeId,isProduce,dates);

                    //病假天数
                    leaveOfsickDays = sickDeductDays(employeeId,isProduce,attendanceSummaryVoDayList,workTimsMap,"2");

                    //事假天数
                    leaveOfAbsenceDays = sickDeductDays(employeeId,isProduce,attendanceSummaryVoDayList,workTimsMap,"1");

                    //获取默认的扣款规则
                    HrmAttendanceRule hrmAttendanceRule =hrmAttendanceRuleService. lambdaQuery().orderByDesc(HrmAttendanceRule::getCreateTime).one();

                    //考勤缺勤抵扣数据
                    QueryHolidayDeductionBO queryHolidayDeductionBO = new QueryHolidayDeductionBO();
                    queryHolidayDeductionBO.setMonth(month);
                    queryHolidayDeductionBO.setYear(year);
                    queryHolidayDeductionBO.setEmployeeId(employeeId);
                    //查询是否添加了抵扣记录 可以抵扣本月的迟到，请假等
                    List<QueryHolidayDeductionVO> deductionVOList = holidayDeductionService.queryHolidayDeduction(queryHolidayDeductionBO);

                    //加班天数
//                    overTimeCount = attendanceClockService.queryEmpAttendanceOverTimeCountDays(Arrays.asList(LocalDateTimeUtil.of(dateStartTime).toLocalDate(), LocalDateTimeUtil.of(dateEndTime).toLocalDate()), employeeId);
                    //这里直接用钉钉的考勤统计数据，来获取缺卡，迟到，早退,请假等数据
                    Optional<HrmAttendanceSummaryVo> empAttendanceSummaryOption = attendanceSummaryVoList.stream().filter(f -> f.getEmployeeId().equals(employeeId)).findFirst();
                    if(empAttendanceSummaryOption==null)
                    {
                        attendanceEmpRecordMap.put("normalDays", normalDays);
                        attendanceEmpRecordMap.put("actualWorkDay", normalDays);
                        attendanceEmpRecordMap.put("absenteeismDays", 0);
                        attendanceEmpRecordMap.put("earlyCount", 0);
                        attendanceEmpRecordMap.put("earlyMinute", 0);
                        attendanceEmpRecordMap.put("lateCount", 0);
                        attendanceEmpRecordMap.put("lateMinute", 0);
                        attendanceEmpRecordMap.put("misscardCount", 0);
                        attendanceEmpRecordMap.put("overTimeHours", 0);
                        attendanceEmpRecordMap.put("leaveOfAbsence", 0);
                    }
                    else
                    {
                        HrmAttendanceSummaryVo empAttendanceSummary = empAttendanceSummaryOption.get();
                        //应出勤天数=实际出勤天数+事假+病假+婚嫁+陪产假+丧假+哺乳假+产假 +旷工天数

                        if(hrmProduceAttendance!=null && hrmProduceAttendance.getPositiveAttendance()!=null)
                        {
                            //如果手动导入的实际出勤天数不为空，则取手动导入的数据
                            attendanceEmpRecordMap.put("actualWorkDay", hrmProduceAttendance.getPositiveAttendance());//实际出勤天数
                        }
                        else
                        {
                            attendanceEmpRecordMap.put("actualWorkDay", empAttendanceSummary.getActualityDays());//实际出勤天数
                        }
                        normalDays = getNormalDays(empAttendanceSummary,isProduce,daysInMonth);
                        if(normalDays==0d)
                        {
                            normalDays = Double.parseDouble(attendanceEmpRecordMap.get("actualWorkDay").toString());
                        }
                        //计算出员工每天的薪资 全额工资/全勤天数
                        empDaySalary = empSalary.divide(new BigDecimal(normalDays),BigDecimal.ROUND_CEILING).setScale(2, RoundingMode.HALF_UP);

                        attendanceEmpRecordMap.put("normalDays", normalDays);//应出勤天数
                        attendanceEmpRecordMap.put("absenteeismDays", empAttendanceSummary.getAbsenteeismDays());
                        attendanceEmpRecordMap.put("earlyCount", empAttendanceSummary.getEarlyCount());
                        attendanceEmpRecordMap.put("earlyMinute", empAttendanceSummary.getEarlyMinute());
                        attendanceEmpRecordMap.put("lateCount", empAttendanceSummary.getLateCount());
                        attendanceEmpRecordMap.put("lateMinute", empAttendanceSummary.getLateMinute());
                        attendanceEmpRecordMap.put("misscardCount", empAttendanceSummary.getMisscardCount());
                        attendanceEmpRecordMap.put("overTimeHours", empAttendanceSummary.getOverTimeHours() != null ?  empAttendanceSummary.getOverTimeHours() : ZERO);
                        attendanceEmpRecordMap.put("leaveOfAbsence", leaveOfAbsenceDays);//事假
                        attendanceEmpRecordMap.put("leaveOfSick", leaveOfsickDays);//病假天数

                    }
                    BigDecimal lateMoney = new BigDecimal(0);
                    BigDecimal earlyMoney = new BigDecimal(0);
                    Integer isPersonal = hrmAttendanceRule.getIsPersonalization();
                    //计算迟到扣款
                    if (hrmAttendanceRule.getLateRuleMethod() == ONE) {
                        lateMoney = hrmAttendanceRule.getLateDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("lateMinute").toString()));
                        if (isPersonal == IsEnum.YES.getValue()) {
                            if (attendanceEmpRecordMap.get("lateMinute")!=null && (int) attendanceEmpRecordMap.get("lateMinute") > hrmAttendanceRule.getLateMinutesOrCounts() && hrmAttendanceRule.getLateMinutesOrCounts() > 0) {
                                lateMoney = hrmAttendanceRule.getLateDeductMoney().multiply(new BigDecimal(hrmAttendanceRule.getLateMinutesOrCounts().toString())).setScale(2, RoundingMode.HALF_UP);
                            }
                        }
                    } else if (hrmAttendanceRule.getLateRuleMethod() == TWO) {
                        lateMoney = hrmAttendanceRule.getLateDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("lateCount").toString()));
                        if (isPersonal == IsEnum.YES.getValue()) {
                            if (attendanceEmpRecordMap.get("lateCount")!=null && (int) attendanceEmpRecordMap.get("lateCount") > hrmAttendanceRule.getLateMinutesOrCounts() && hrmAttendanceRule.getLateMinutesOrCounts() > 0) {
                                lateMoney = hrmAttendanceRule.getLateDeductMoney().multiply(new BigDecimal(hrmAttendanceRule.getLateMinutesOrCounts().toString())).setScale(2, RoundingMode.HALF_UP);
                            }
                        }
                    } else if (hrmAttendanceRule.getLateRuleMethod() == THREE) {
                        lateMoney = hrmAttendanceRule.getLateDeductMoney();
                    }
                    //计算早退
                    if (hrmAttendanceRule.getEarlyRuleMethod() == ONE) {
                        earlyMoney = hrmAttendanceRule.getEarlyDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("earlyMinute").toString()));
                        if (isPersonal == IsEnum.YES.getValue()) {
                            if (attendanceEmpRecordMap.get("earlyMinute")!=null && (int) attendanceEmpRecordMap.get("earlyMinute") > hrmAttendanceRule.getEarlyMinutesOrCounts() && hrmAttendanceRule.getEarlyMinutesOrCounts() > 0) {
                                earlyMoney = hrmAttendanceRule.getLateDeductMoney().multiply(new BigDecimal(hrmAttendanceRule.getEarlyMinutesOrCounts().toString())).setScale(2, RoundingMode.HALF_UP);
                            }
                        }
                    } else if (hrmAttendanceRule.getEarlyRuleMethod() == TWO) {
                        earlyMoney = hrmAttendanceRule.getEarlyDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("earlyCount").toString()));
                        if (isPersonal == IsEnum.YES.getValue()) {
                            if (attendanceEmpRecordMap.get("earlyCount")!=null && (int) attendanceEmpRecordMap.get("earlyCount") > hrmAttendanceRule.getEarlyMinutesOrCounts() && hrmAttendanceRule.getEarlyMinutesOrCounts() > 0) {
                                earlyMoney = hrmAttendanceRule.getLateDeductMoney().multiply(new BigDecimal(hrmAttendanceRule.getEarlyMinutesOrCounts().toString())).setScale(2, RoundingMode.HALF_UP);
                            }
                        }
                    } else if (hrmAttendanceRule.getEarlyRuleMethod() == THREE) {
                        earlyMoney = hrmAttendanceRule.getEarlyDeductMoney();
                    }

                    //缺卡扣款
                    BigDecimal misscardMoney = hrmAttendanceRule.getMisscardDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("misscardCount").toString())).setScale(2, RoundingMode.HALF_UP);

                    //旷工扣款 员工每天的工资 * 旷工天数
                    BigDecimal absenteeismMoney =empDaySalary.multiply(new BigDecimal(attendanceEmpRecordMap.get("absenteeismDays").toString()));

                    //事假扣款 事假天数*每天的工资
                    BigDecimal leaveOfAbsenceMoney =empDaySalary.multiply(new BigDecimal(attendanceEmpRecordMap.get("leaveOfAbsence").toString()));


                    //抵扣 缺卡，迟到，早退这些情况
                    if(CollectionUtil.isNotEmpty(deductionVOList))
                    {
                        //添加的 早退 抵扣时长
                        QueryHolidayDeductionVO deductionVoZaotui= deductionVOList.stream().filter(f -> f.getType()==2).findAny().orElse(null);
                        if(deductionVoZaotui!=null)
                        {
                            BigDecimal deductionTimeZaotui = deductionVoZaotui.getDeductionTime();//分钟
                            //请假抵扣时长 早退
                            BigDecimal needDiKouZaotui = new BigDecimal(attendanceEmpRecordMap.get("earlyMinute").toString());
                            if(deductionTimeZaotui.compareTo(needDiKouZaotui)>=0)
                            {
                                earlyMoney = new BigDecimal(0);
                            }
                        }

                        //缺卡补卡抵扣
                        QueryHolidayDeductionVO deductionVoBuKa= deductionVOList.stream().filter(f -> f.getType()==6).findAny().orElse(null);
                        if(deductionVoBuKa!=null)
                        {
                            BigDecimal deductionCountBuKa = deductionVoBuKa.getDeductionTime();//次数
                            //缺卡补卡 抵扣
                            if(deductionCountBuKa.compareTo(new BigDecimal(attendanceEmpRecordMap.get("misscardCount").toString()))>=0)
                            {
                                //缺卡补卡 抵扣
                                misscardMoney = new BigDecimal(0);
                            }
                        }

                        //添加的 迟到 抵扣时长
                        QueryHolidayDeductionVO deductionVoChidao= deductionVOList.stream().filter(f -> f.getType()==1).findAny().orElse(null);
                        if(deductionVoChidao!=null)
                        {
                            BigDecimal deductionTimeChidao = deductionVoChidao.getDeductionTime();//分钟
                            //请假抵扣时长 早退
                            BigDecimal needDiKouChidao = new BigDecimal(attendanceEmpRecordMap.get("lateMinute").toString());
                            if(deductionTimeChidao.compareTo(needDiKouChidao)>=0)
                            {
                                lateMoney = new BigDecimal(0);
                            }
                        }

                        //添加的 事假 抵扣时长
                        QueryHolidayDeductionVO deductionVoShiJia= deductionVOList.stream().filter(f -> f.getType()==3).findAny().orElse(null);
                        if(deductionVoShiJia!=null)
                        {
                            BigDecimal deductionTimeQingJia = deductionVoShiJia.getDeductionTime();//分钟
                            //请假抵扣时长 事假
                            BigDecimal needDiKouShiJia = new BigDecimal(attendanceEmpRecordMap.get("leaveOfAbsence").toString());
                            needDiKouShiJia = needDiKouShiJia.multiply(new BigDecimal(60));
                            if(deductionTimeQingJia.compareTo(needDiKouShiJia)>=0)
                            {
                                //如果添加的 请假时长大于 本月发生的事假时长，则修改本月事假时长为0
                                leaveOfAbsenceMoney = new BigDecimal(0);
                            }
                        }
                    }

                    /**
                     * 加班费: 加班工资：12元/小时。（生产、工程、质检、仓库有加班工资，副经
                     * 理及以上岗位加班没有加班工资）
                     */
                    if(hrmProduceAttendance!=null)
                    {
                        //有加班费
                        if (hrmProduceAttendance.getWorkOverTime()!=null)
                        {
                            empAttendanceMap.put(180101, salaryBasic==null?String.valueOf(hrmProduceAttendance.getWorkOverTime().multiply(new BigDecimal(12))):String.valueOf(salaryBasic.getOvertimePay().multiply(hrmProduceAttendance.getWorkOverTime())));//180101 加班费
                        }
                        else
                        {
                            //没有加班费
                            empAttendanceMap.put(180101, "0");
                        }
                    }
                    else
                    {
                        empAttendanceMap.put(180101, "0");
                    }

                    /**
                     * 夜班补贴，30元/夜班 （三班倒没有夜班补贴，夜班必须连续上满8
                     * 小时并且超过凌晨3点）
                     */
                    if(hrmProduceAttendance!=null)
                    {
                        if(hrmProduceAttendance.getNightShift()!=null)
                        {
                            empAttendanceMap.put(180102, salaryBasic==null?String.valueOf(30*hrmProduceAttendance.getNightShift()):String.valueOf(salaryBasic.getSubsidy().multiply(new BigDecimal(hrmProduceAttendance.getNightShift()))));//180102为夜班补贴
                        }
                        else
                        {
                            empAttendanceMap.put(180102, "0");
                        }
                    }
                    else
                    {
                        empAttendanceMap.put(180102, "0");
                    }
                    /**
                     * 满勤奖金，计算
                     */
                    //判断是否为全勤,不参与考勤的默认全勤
                    isFullAttendance = checkIsFullAttendance(employeeId,attendanceSummaryVoDayList,attendanceSummaryVoList,year,month,deductionVOList);
                    if(isFullAttendance)
                    {
                        empAttendanceMap.put(40102, fullMoney==null?"0":String.valueOf(fullMoney));//全勤奖
                    }
                    empAttendanceMap.put(1, attendanceEmpRecordMap.get("normalDays").toString());//应出勤天数
                    empAttendanceMap.put(2, attendanceEmpRecordMap.get("actualWorkDay").toString());//实际出勤天数
                    empAttendanceMap.put(190101, lateMoney.toString());//迟到扣款
                    empAttendanceMap.put(190102, earlyMoney.toString());//早退扣款
                    empAttendanceMap.put(190103, absenteeismMoney.toString());//旷工扣款
                    empAttendanceMap.put(19010402, leaveOfAbsenceMoney.toString());//假期扣款(事假)
                    empAttendanceMap.put(190105, misscardMoney.toString());//缺卡扣款
                    empAttendanceMap.put(190106, "0");//综合扣款
                    empAttendanceMap.put(280, otherDeduction!=null?otherDeduction.toString():"0");//其他扣款
                    empAttendanceMap.put(281, otherSubsidy!=null ? otherSubsidy.toString():"0");//其他补贴
                    empAttendanceMap.put(282, loanMoney!=null ? loanMoney.toString():"0");//借款

                    //病假扣款规则
                    /**
                     * 2天内全额发放2天的工资，假如3天病假，就按照最低工资标准扣钱
                     * 每天扣的钱是1680/25（天）=67.2元/天
                     * 3天病假总计67.2*3 = 201.6元
                     */
                    if(leaveOfsickDays.compareTo(new BigDecimal(2))==-1 || leaveOfsickDays.compareTo(new BigDecimal(2))==0)
                    {
                        empAttendanceMap.put(19010401, "0");
                    }
                    else
                    {
                        //当地最低基本工资
                        BigDecimal baseSalary = salaryBasic.getSalaryBasic();
                        //每天需要扣的钱
                        BigDecimal oneDayMoney = baseSalary.divide(new BigDecimal(normalDays),BigDecimal.ROUND_CEILING).setScale(2, BigDecimal.ROUND_UP);
                        //计算病假需要扣的钱
                        BigDecimal sickMoney = oneDayMoney.multiply(leaveOfsickDays).setScale(2, BigDecimal.ROUND_UP);
                        empAttendanceMap.put(19010401, sickMoney.toString());//假期扣款(病假)
                    }
                    attendanceDataMap.put(jobNumber, empAttendanceMap);
                }
            }

        }

        //附加扣除项map
        Map<String, Map<Integer, String>> additionalDeductionDataMap;
        try {
            additionalDeductionDataMap = resolveAdditionalDeductionData(additionalDeductionFile);
        } catch (Exception e) {
            throw new HrmException(HrmCodeEnum.ADDITIONAL_DEDUCTION_DATA_ERROR);
        }
        //截止上月个税累计map
        Map<String, Map<Integer, String>> cumulativeTaxOfLastMonthDataMap;
        try {
            cumulativeTaxOfLastMonthDataMap = resolveCumulativeTaxOfLastMonthData(cumulativeTaxOfLastMonthFile);
        } catch (Exception e) {
            throw new HrmException(HrmCodeEnum.CUMULATIVE_TAX_OF_LAST_MONTH_DATA_ERROR);
        }

        for (Map<String, Object> map : mapList)
        {
            //是否参与考勤
            boolean isJoinAttendance = true;
          //计算每个员工的薪资项与对应的值
            //薪资项
            List<HrmSalaryOption> salaryOptionList = hrmSalaryOptionService.lambdaQuery().ne(HrmSalaryOption::getParentCode, 0).list();
            Map<Integer, List<HrmSalaryOption>> salaryOptionListMap = salaryOptionList.stream().collect(Collectors.groupingBy(HrmSalaryOption::getIsFixed));
            List<HrmSalaryOption> noFixedSalaryOptionList = salaryOptionListMap.get(0);
            Long employeeId = Convert.toLong(map.get("employeeId"));
            String jobNumber = (String) map.get("jobNumber");
            Integer status = (Integer) map.get("status");//员工状态
            HrmEmployeeVO hrmEmployeeVO = new HrmEmployeeVO();
            hrmEmployeeVO.setEmployeeId(employeeId);
            hrmEmployeeVO.setStatus(status);
            hrmEmployeeVO.setJobNumber(jobNumber);

            HrmAttendanceGroup hrmAttendanceGroup = attendanceGroupService.queryAttendanceGroupDingDing(employeeId);
            if(hrmAttendanceGroup==null)
            {
                isJoinAttendance = false;
            }
            //获取员工本月的薪资记录
            Optional<HrmSalaryMonthEmpRecord> salaryMonthEmpRecordOpt = salaryMonthEmpRecordService.lambdaQuery().eq(HrmSalaryMonthEmpRecord::getSRecordId, sRecordId)
                    .eq(HrmSalaryMonthEmpRecord::getEmployeeId, employeeId).oneOpt();
            HrmSalaryMonthEmpRecord salaryMonthEmpRecord;
            List<HrmSalaryMonthOptionValue> optionValueList;
            if (salaryMonthEmpRecordOpt.isPresent()) {
                salaryMonthEmpRecord = salaryMonthEmpRecordOpt.get();
                //删除原有的员工薪资项记录
                salaryMonthOptionValueService.lambdaUpdate().eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
                //获取非固定项 及金额
                List<HrmSalaryMonthOptionValue> noFixedOptionValueList = getNoFixedOptionValue(salaryMonthEmpRecord, noFixedSalaryOptionList, true);
                optionValueList = new ArrayList<>(noFixedOptionValueList);
            } else {
                Map<Integer, String> codeValueMap = attendanceDataMap.get(jobNumber);
                salaryMonthEmpRecord = new HrmSalaryMonthEmpRecord();
                salaryMonthEmpRecord.setSRecordId(salaryMonthRecord.getSRecordId());
                salaryMonthEmpRecord.setEmployeeId(employeeId);
                salaryMonthEmpRecord.setActualWorkDay(new BigDecimal(codeValueMap.get(2)));
                salaryMonthEmpRecord.setNeedWorkDay(new BigDecimal(codeValueMap.get(1)));
                salaryMonthEmpRecord.setYear(year);
                salaryMonthEmpRecord.setMonth(month);
                salaryMonthEmpRecordService.save(salaryMonthEmpRecord);
                //获取非固定项
                List<HrmSalaryMonthOptionValue> noFixedOptionValueList = getNoFixedOptionValue(salaryMonthEmpRecord, noFixedSalaryOptionList, true);
                optionValueList = new ArrayList<>(noFixedOptionValueList);
            }

            try {
                Map<Integer, String> codeValueMap = attendanceDataMap.get(jobNumber);
                if (attendanceFile != null || isSyncAttendanceData) {
                    //获取实际计薪时长,codeValueMap key=1 对应 normalDays
                    salaryMonthEmpRecord.setActualWorkDay(new BigDecimal(codeValueMap.get(2)));
                    salaryMonthEmpRecord.setNeedWorkDay(new BigDecimal(codeValueMap.get(1)));
                    salaryMonthEmpRecordService.updateById(salaryMonthEmpRecord);
                    codeValueMap.remove(1);
                    codeValueMap.remove(2);
                    //获取固定项
                    List<HrmSalaryMonthOptionValue> fixedOptionValueList = getFixedOptionValue(salaryMonthEmpRecord, codeValueMap,isJoinAttendance);
                    optionValueList.addAll(fixedOptionValueList);
                } else {
                    if (!salaryMonthEmpRecordOpt.isPresent()) {
                        salaryMonthEmpRecord.setNeedWorkDay(new BigDecimal(codeValueMap.get(1)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new HrmException(HrmCodeEnum.ATTENDANCE_DATA_ERROR);
            }
            //获取社保项
            List<HrmSalaryMonthOptionValue> socialSecurityOption = getSocialSecurityOption(salaryMonthEmpRecord, isSyncInsuranceData);
            optionValueList.addAll(socialSecurityOption);
//            //初始个税专项附加扣除累计数据
            Map<Integer, String> additionalDeductionData = additionalDeductionDataMap.get(jobNumber);
            if (additionalDeductionData != null) {
                salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(
                        260101, 260102, 260103, 260104, 260105,260106))
                        .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
                additionalDeductionData.forEach((code, value) -> {
                    HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                    salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
                    salaryMonthOptionValue.setCode(code);
                    salaryMonthOptionValue.setValue(value);
                    optionValueList.add(salaryMonthOptionValue);
                });
            }
            //保存员工工资项及其值
            salaryMonthOptionValueService.saveBatch(optionValueList);
            //取出 截止上月个税累计
            Map<Integer, String> cumulativeTaxOfLastMonthData = cumulativeTaxOfLastMonthDataMap.get(jobNumber);
            List<HrmSalaryMonthOptionValue> salaryMonthOptionValues = computeSalary(salaryMonthEmpRecord, cumulativeTaxOfLastMonthData,hrmEmployeeVO);

            salaryMonthOptionValueService.saveBatch(salaryMonthOptionValues);
            salaryMonthEmpRecordService.updateById(salaryMonthEmpRecord);

        }

        Map<String, Object> countMap = salaryMonthRecordMapper.queryMonthSalaryCount(salaryMonthRecord.getSRecordId());
        BeanUtil.fillBeanWithMap(countMap, salaryMonthRecord, true);
        salaryMonthRecord.setCheckStatus(SalaryRecordStatus.COMPUTE.getValue());
        updateById(salaryMonthRecord);
        //salaryActionRecordService.computeSalaryDataLog(salaryMonthRecord);




    }


    /**
     * 通过类型查询计薪/未计薪人员
     *
     * @param type 0 未计薪 1 计薪
     * @return
     */
    public List<Map<String, Object>> queryPaySalaryEmployeeListByType(Integer type, TaxType taxType)
    {
        List<Map<String, Object>> employeeList = new ArrayList<>();
        HrmSalaryMonthRecord salaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("LIMIT 1").one();
        Collection<Long> dataAuthEmployeeIds = null;
        dataAuthEmployeeIds = employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).eq(HrmEmployee::getIsDel, 0).list()
                    .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
        List<Map<String, Object>> list = salaryMonthEmpRecordMapper.queryPaySalaryEmployeeList(salaryMonthRecord.getEndTime(), dataAuthEmployeeIds);
        list.forEach(map -> {
            Long employeeId = Convert.toLong(map.get("employeeId"));
            Long deptId = Convert.toLong(map.get("deptId"));

//            if(employeeId==1712718940268L || employeeId==1712718940204L || employeeId==1712718940300L)
//            {
//                employeeList.add(map);
//            }
//            if(employeeId==1712718940268L)
//            {
//                employeeList.add(map);
//            }

            employeeList.add(map);


        });
        return employeeList;

    }


    /**
     * 查询有薪资档案的人员
     * @return
     */
    public List<Map<String, Object>> queryHasSalaryArchivesEmployeeList()
    {
        QuerySalaryArchivesListDto querySalaryArchivesListDto = new QuerySalaryArchivesListDto();
        List<Map<String, Object>> employeeList = new ArrayList<>();
        HrmSalaryMonthRecord salaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("LIMIT 1").one();
        Collection<Long> dataAuthEmployeeIds = null;
        dataAuthEmployeeIds = employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).eq(HrmEmployee::getIsDel, 0).list()
                .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
        List<Map<String, Object>> list = salaryMonthEmpRecordMapper.queryPaySalaryEmployeeList(salaryMonthRecord.getEndTime(), dataAuthEmployeeIds);
        list.forEach(map -> {
            Long employeeId = Convert.toLong(map.get("employeeId"));

            Long deptId = Convert.toLong(map.get("deptId"));
            querySalaryArchivesListDto.setEmployeeId(employeeId);
            List<QuerySalaryArchivesListVO> empSalaryArchivesList = salaryArchivesService.queryEmpSalaryArchivesList(querySalaryArchivesListDto);
            if(CollectionUtil.isNotEmpty(empSalaryArchivesList))
            {
                QuerySalaryArchivesListVO salaryArchivesListVO = empSalaryArchivesList.get(0);
                if(StringUtils.isNotBlank(salaryArchivesListVO.getTotal()) && Double.parseDouble(salaryArchivesListVO.getTotal())>0)
                {
//                    if(employeeId==1712718940191L)
//                     {
//                        employeeList.add(map);
//                     }
                 employeeList.add(map);
                }
            }
        });
        return employeeList;

    }

    /**
     * 计算薪资
     *
     * @param salaryMonthEmpRecord
     * @param cumulativeTaxOfLastMonthData 上个月的累计税数据
     * @return
     */
    public List<HrmSalaryMonthOptionValue> computeSalary(HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                         Map<Integer, String> cumulativeTaxOfLastMonthData,HrmEmployeeVO hrmEmployeeVO) {
        //员工计税规则
        HrmSalaryTaxRule hrmSalaryTaxRule = new HrmSalaryTaxRule();
        hrmSalaryTaxRule.setIsTax(1);
        hrmSalaryTaxRule.setCycleType(1);
        hrmSalaryTaxRule.setMarkingPoint(5000);
        hrmSalaryTaxRule.setTaxType(1);
        SalaryBaseTotal salaryBaseTotal = salaryComputeService.baseComputeSalary(salaryMonthEmpRecord);
        return salaryComputeService.computeSalary(salaryBaseTotal,salaryMonthEmpRecord,hrmSalaryTaxRule,cumulativeTaxOfLastMonthData,hrmEmployeeVO);

    }


    /**
     * 获取最新的每月薪资记录
     * @return
     */
    public HrmSalaryMonthRecord queryLastSalaryMonthRecord()
    {
        HrmSalaryMonthRecord salaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("limit 1").one();
        if(salaryMonthRecord==null)
        {
            //如果还没有每月薪资记录，则根据薪资配置生成一条
            HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
            String salaryStartMonth = salaryConfig.getSalaryStartMonth();
            DateTime date = DateUtil.parse(salaryStartMonth, "yyyy-MM");
            int month = date.month() + 1;
            int year = date.year();
            DateTime startTime = DateUtil.parse(year + "-" + month + "-" + salaryConfig.getSalaryCycleStartDay(), "yyyy-MM-dd");
            DateTime endTime;
            if (salaryConfig.getSalaryCycleStartDay() > 1) {
                DateTime dateTime = DateUtil.offsetMonth(startTime, 1);
                int nextMonth = dateTime.month() + 1;
                endTime = DateUtil.parse(year + "-" + nextMonth + "-" + salaryConfig.getSalaryCycleEndDay(), "yyyy-MM-dd");
            } else {
                endTime = DateUtil.parseDate(DateUtil.formatDate(DateUtil.endOfMonth(startTime)));
            }
            salaryMonthRecord = new HrmSalaryMonthRecord();
            salaryMonthRecord.setTitle(HrmLanguageEnum.parseName(month) + HrmLanguageEnum.SALARY_REPORT.getName());
            salaryMonthRecord.setYear(year);
            salaryMonthRecord.setMonth(month);
            salaryMonthRecord.setCreateTime(LocalDateTime.now());
            salaryMonthRecord.setStartTime(startTime.toLocalDateTime().toLocalDate());
            salaryMonthRecord.setEndTime(endTime.toLocalDateTime().toLocalDate());
            //保存
            save(salaryMonthRecord);
        }
        return lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("limit 1").one();
    }


    /**
     * 创建下月薪资表
     * @return
     */
    @Transactional
    public OperationLog addNextMonthSalary() {
        //查询薪资上月记录,如果有就往后推一个月,如果没有就去薪资配置计薪月
        HrmSalaryMonthRecord lastSalaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).one();
        HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
        LocalDate date = LocalDateTimeUtil.offset(LocalDateTimeUtil.of(DateUtil.parse(lastSalaryMonthRecord.getYear() + "-" + lastSalaryMonthRecord.getMonth(), "yy-MM")), 1, ChronoUnit.MONTHS).toLocalDate();
        int month = date.getMonthValue();
        int year = date.getYear();
        LocalDate startTime = LocalDate.of(year, month, salaryConfig.getSalaryCycleStartDay());
        LocalDate endTime;
        if (salaryConfig.getSalaryCycleStartDay() > 1) {
            LocalDate dateTime = LocalDateTimeUtil.offset(startTime.atStartOfDay(), 1, ChronoUnit.MONTHS).toLocalDate();
            int nextMonth = dateTime.getMonthValue();
            endTime = LocalDate.of(year, nextMonth, salaryConfig.getSalaryCycleEndDay());
        } else {
            endTime = startTime.with(TemporalAdjusters.lastDayOfMonth());
        }
        //当前月薪资数据将变为归档状态
        lastSalaryMonthRecord.setCheckStatus(SalaryRecordStatus.HISTORY.getValue());
        computeSalaryCount(lastSalaryMonthRecord);
        updateById(lastSalaryMonthRecord);
        HrmSalaryMonthRecord salaryMonthRecord = new HrmSalaryMonthRecord();
        salaryMonthRecord.setTitle(HrmLanguageEnum.parseName(month) + HrmLanguageEnum.SALARY_REPORT.getName());
        salaryMonthRecord.setYear(year);
        salaryMonthRecord.setMonth(month);
        salaryMonthRecord.setStartTime(startTime);
        salaryMonthRecord.setEndTime(endTime);
        salaryMonthRecord.setCreateTime(LocalDateTime.now());
        salaryMonthRecord.setNum(queryPaySalaryEmployeeListByType(1, null).size());
        save(salaryMonthRecord);
        salaryActionRecordService.addNextMonthSalaryLog(salaryMonthRecord);
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(salaryMonthRecord.getSRecordId(), year + "-" + salaryMonthRecord.getTitle());
        operationLog.setOperationInfo("新建薪资报表：" + year + "-" + salaryMonthRecord.getTitle());
        return operationLog;

    }


    public HrmSalaryMonthRecord computeSalaryCount(HrmSalaryMonthRecord salaryMonthRecord) {
        //保存薪资项表头
        List<Long> empRecordIds = salaryMonthRecordMapper.queryDeleteEmpRecordIds(salaryMonthRecord.getSRecordId());
        if (CollUtil.isNotEmpty(empRecordIds)) {
            //过滤掉被删除员工薪资数据
            salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getSEmpRecordId, empRecordIds).remove();
            salaryMonthEmpRecordService.lambdaUpdate().in(HrmSalaryMonthEmpRecord::getSEmpRecordId, empRecordIds).remove();
        }
        Integer num = salaryMonthEmpRecordService.lambdaQuery().eq(HrmSalaryMonthEmpRecord::getSRecordId, salaryMonthRecord.getSRecordId()).count().intValue();
        salaryMonthRecord.setNum(num);
        List<SalaryOptionHeadVO> salaryOptionHeadVOList = querySalaryOptionHead();
        salaryMonthRecord.setOptionHead(JSON.toJSONString(salaryOptionHeadVOList));
        Map<String, Object> countMap = salaryMonthRecordMapper.queryMonthSalaryCount(salaryMonthRecord.getSRecordId());
        return BeanUtil.fillBeanWithMap(countMap, salaryMonthRecord, true);
    }

    /**
     * 薪资表头
     * @return
     */
    public List<SalaryOptionHeadVO> querySalaryOptionHead() {
        List<HrmSalaryOption> list = hrmSalaryOptionService.lambdaQuery()
                .select(HrmSalaryOption::getCode, HrmSalaryOption::getName, HrmSalaryOption::getIsFixed)
                .eq(HrmSalaryOption::getIsShow, IsEnum.YES.getValue())
                .ne(HrmSalaryOption::getParentCode, 0)
                .eq(HrmSalaryOption::getIsOpen, 1)
                .orderByAsc(HrmSalaryOption::getCode).list();
        List<SalaryOptionHeadVO> optionHeadVOList = new LinkedList<>();
//        optionHeadVOList.add(new SalaryOptionHeadVO(1, "计薪天数", 1));
//        optionHeadVOList.add(new SalaryOptionHeadVO(2, "实际计薪天数", 1));
        optionHeadVOList.add(new SalaryOptionHeadVO(1, "应出勤天数", 1));
        optionHeadVOList.add(new SalaryOptionHeadVO(2, "实际出勤天数", 1));
        List<SalaryOptionHeadVO> salaryOptionHeadVOList = TransferUtil.transferList(list, SalaryOptionHeadVO.class);
        optionHeadVOList.addAll(salaryOptionHeadVOList);
        List<Integer> attendList = Arrays.asList(180101, 190101, 190102, 190103, 19010401,19010402, 190105, 190106, 200101,40102,281,280,282);
        for (SalaryOptionHeadVO headVO : optionHeadVOList) {
            if (attendList.contains(headVO.getCode())) {
                //允许考勤相关的字段编辑
                headVO.setIsFixed(0);
            }
            //添加语言包key
            //headVO.setLanguageKeyMap(LanguageFieldUtil.getFieldNameKeyMap("name_resourceKey", "hrm.", StrUtil.toString(headVO.getCode())));
        }

        return optionHeadVOList;
    }


    /**
     * 解析考勤数据
     */
    public Map<String, Map<Integer, String>> resolveAttendanceData(MultipartFile multipartFile) throws Exception {
        Map<Integer, Integer> indexCodeMap = new HashMap<>();
        indexCodeMap.put(4, 180101);
        indexCodeMap.put(5, 190101);
        indexCodeMap.put(6, 190102);
        indexCodeMap.put(7, 190103);
        indexCodeMap.put(8, 190104);
        indexCodeMap.put(9, 190105);
        indexCodeMap.put(10, 190106);
        indexCodeMap.put(11, 1);
        Map<String, Map<Integer, String>> jobNumberMap = new HashMap<>();
        if (multipartFile != null) {
            ExcelReader reader = null;
            reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<List<Object>> read = reader.read();
            for (int i = TWO; i < read.size(); i++) {
                List<Object> row = read.get(i);
                String jobNumber = row.get(2).toString();
                Map<Integer, String> codeValueMap = new HashMap<>();
                indexCodeMap.forEach((k, v) -> {
                    if (ObjectUtil.isNotEmpty(row.get(k))) {
                        codeValueMap.put(v, row.get(k).toString());
                    } else {
                        codeValueMap.put(v, "0");
                    }
                });
                jobNumberMap.put(jobNumber, codeValueMap);
            }
        } else {
            List<Map<String, Object>> mapList = queryPaySalaryEmployeeListByType(1, null);
            for (Map<String, Object> map : mapList) {
                Map<Integer, String> codeValueMap = new HashMap<>();
                indexCodeMap.forEach((k, v) -> {
                    codeValueMap.put(v, "0");
                });
                jobNumberMap.put((String) map.get("jobNumber"), codeValueMap);
            }
        }
        return jobNumberMap;
    }

    /**
     * 获取固定薪资项
     *
     * @param salaryMonthEmpRecord
     * @param codeValueMap
     * @param  isJoinAttendance 是否参与考勤
     * @return
     */
    private List<HrmSalaryMonthOptionValue> getFixedOptionValue(HrmSalaryMonthEmpRecord salaryMonthEmpRecord, Map<Integer, String> codeValueMap,boolean isJoinAttendance) {
        salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(180101, 180102,190101, 190102, 190103, 19010401, 19010402,190105, 190106, 200101,40102,280,281,282))
                .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
        List<HrmSalaryMonthOptionValue> fixedOptionValueList = new ArrayList<>();
        //考勤扣款合计
        BigDecimal attendanceDeductionTotal = new BigDecimal(0);
        for (Integer code : codeValueMap.keySet()) {
            String value = codeValueMap.get(code);
            if (code != 180101 && code != 180102 && code!=40102 && code != 282) {
                //除去加班工资项,夜班补贴，其他补贴，和满勤
                ////借款不放入考勤扣款合计
                attendanceDeductionTotal = attendanceDeductionTotal.add(new BigDecimal(value));
            }
            HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
            salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
            salaryMonthOptionValue.setCode(code);
            salaryMonthOptionValue.setValue(value);
            fixedOptionValueList.add(salaryMonthOptionValue);
        }
        if(isJoinAttendance)
        {
            //考勤扣款合计
            HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
            salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
            salaryMonthOptionValue.setCode(200101);//200101是考勤扣款合计
            salaryMonthOptionValue.setValue(attendanceDeductionTotal.toString());
            fixedOptionValueList.add(salaryMonthOptionValue);
        }


        return fixedOptionValueList;
    }


    /**
     * 获取社保薪资项
     *
     * @param salaryMonthEmpRecord 100101 个人社保
     *                             100102  个人公积金
     *                             110101  企业社保
     *                             120101  企业公积金
     */
    public List<HrmSalaryMonthOptionValue> getSocialSecurityOption(HrmSalaryMonthEmpRecord salaryMonthEmpRecord, Boolean isSyncInsuranceData) {
        HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
        Map<Integer, String> socialSecurityOptionMap = new HashMap<>();
        List<HrmSalaryMonthOptionValue> salaryMonthOptionValueList = new ArrayList<>();
        if (!isSyncInsuranceData) {
            List<HrmSalaryMonthOptionValue> socialSecurityOptions = salaryMonthOptionValueService.lambdaQuery().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(100101, 100102, 110101, 120101))
                    .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).list();
            if (CollUtil.isNotEmpty(socialSecurityOptions)) {
                return salaryMonthOptionValueList;
            } else {
                socialSecurityOptionMap.put(100101, "0");
                socialSecurityOptionMap.put(100102, "0");
                socialSecurityOptionMap.put(110101, "0");
                socialSecurityOptionMap.put(120101, "0");
            }
        } else {
            salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(100101, 100102, 110101, 120101))
                    .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
            Integer socialSecurityMonthType = salaryConfig.getSocialSecurityMonthType();
            DateTime date = DateUtil.parse(salaryMonthEmpRecord.getYear() + "-" + salaryMonthEmpRecord.getMonth(), "yy-MM");
            if (socialSecurityMonthType == 0) {
                date = DateUtil.offsetMonth(DateUtil.parse(salaryMonthEmpRecord.getYear() + "-" + salaryMonthEmpRecord.getMonth(), "yy-MM"), -1);
            } else if (socialSecurityMonthType == TWO) {
                date = DateUtil.offsetMonth(DateUtil.parse(salaryMonthEmpRecord.getYear() + "-" + salaryMonthEmpRecord.getMonth(), "yy-MM"), 1);
            }
            Optional<HrmInsuranceMonthEmpRecord> salaryMonthEmpRecordOpt = insuranceMonthEmpRecordService.lambdaQuery()
                    .eq(HrmInsuranceMonthEmpRecord::getYear, date.year())
                    .eq(HrmInsuranceMonthEmpRecord::getMonth, date.month() + 1)
                    .eq(HrmInsuranceMonthEmpRecord::getEmployeeId, salaryMonthEmpRecord.getEmployeeId())
                    .eq(HrmInsuranceMonthEmpRecord::getStatus, 1)
                    .oneOpt();
            if (salaryMonthEmpRecordOpt.isPresent()) {
                HrmInsuranceMonthEmpRecord insuranceMonthEmpRecord = salaryMonthEmpRecordOpt.get();
                socialSecurityOptionMap.put(100101, insuranceMonthEmpRecord.getPersonalInsuranceAmount() == null ? "0" : insuranceMonthEmpRecord.getPersonalInsuranceAmount().toString());
                socialSecurityOptionMap.put(100102, insuranceMonthEmpRecord.getPersonalProvidentFundAmount() == null ? "0" : insuranceMonthEmpRecord.getPersonalProvidentFundAmount().setScale(0, BigDecimal.ROUND_HALF_DOWN).toString());
                socialSecurityOptionMap.put(110101, insuranceMonthEmpRecord.getCorporateInsuranceAmount() == null ? "0" : insuranceMonthEmpRecord.getCorporateInsuranceAmount().toString());
                socialSecurityOptionMap.put(120101, insuranceMonthEmpRecord.getCorporateProvidentFundAmount() == null ? "0" : insuranceMonthEmpRecord.getCorporateProvidentFundAmount().toString());
            } else {
                socialSecurityOptionMap.put(100101, "0");
                socialSecurityOptionMap.put(100102, "0");
                socialSecurityOptionMap.put(110101, "0");
                socialSecurityOptionMap.put(120101, "0");
            }
        }
        socialSecurityOptionMap.forEach((code, value) -> {
            HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
            salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
            salaryMonthOptionValue.setCode(code);
            salaryMonthOptionValue.setValue(value);
            salaryMonthOptionValueList.add(salaryMonthOptionValue);
        });
        return salaryMonthOptionValueList;
    }

    /**
     * 解析excel附加扣除项
     *
     * @param additionalDeductionFile
     * @return
     */
    public Map<String, Map<Integer, String>> resolveAdditionalDeductionData(MultipartFile additionalDeductionFile) throws Exception {
        Map<String, Map<Integer, String>> jobNumberMap = new HashMap<>();
        if (additionalDeductionFile != null) {
            Map<Integer, Integer> indexCodeMap = new HashMap<>();
            indexCodeMap.put(4, 260101);
            indexCodeMap.put(5, 260102);
            indexCodeMap.put(6, 260103);
            indexCodeMap.put(7, 260104);
            indexCodeMap.put(8, 260105);
            indexCodeMap.put(9, 260106);

            ExcelReader reader = ExcelUtil.getReader(additionalDeductionFile.getInputStream());
            List<List<Object>> read = reader.read();
            for (int i = TWO; i < read.size(); i++) {
                List<Object> row = read.get(i);
                String jobNumber = row.get(2).toString();
                Map<Integer, String> codeValueMap = new HashMap<>();
                indexCodeMap.forEach((k, v) -> {
                    if (ObjectUtil.isNotEmpty(row.get(k))) {
                        codeValueMap.put(v, row.get(k).toString());
                    } else {
                        codeValueMap.put(v, "0");
                    }
                });
                logger.info("260101:"+codeValueMap.get(260101) +";260102:"+codeValueMap.get(260102)+";260103:"+codeValueMap.get(260103)+";260104:"+codeValueMap.get(260104)+";260105:"+codeValueMap.get(260105)+";260106:"+codeValueMap.get(260106));

                jobNumberMap.put(jobNumber, codeValueMap);
            }
        }
        return jobNumberMap;
    }

    private Map<String, Map<Integer, String>> resolveCumulativeTaxOfLastMonthData(MultipartFile cumulativeTaxOfLastMonthFile) throws Exception {
        Map<String, Map<Integer, String>> jobNumberMap = new HashMap<>();
        if (cumulativeTaxOfLastMonthFile != null) {
            Map<Integer, Integer> indexCodeMap = new HashMap<>();
            indexCodeMap.put(4, 250101);
            indexCodeMap.put(5, 250102);
            indexCodeMap.put(6, 250103);
            indexCodeMap.put(7, 250105);
            ExcelReader reader = ExcelUtil.getReader(cumulativeTaxOfLastMonthFile.getInputStream());
            List<List<Object>> read = reader.read();
            for (int i = TWO; i < read.size(); i++) {
                List<Object> row = read.get(i);
                String jobNumber = row.get(2).toString();
                Map<Integer, String> codeValueMap = new HashMap<>();
                indexCodeMap.forEach((k, v) -> {
                    if (ObjectUtil.isNotEmpty(row.get(k))) {
                        codeValueMap.put(v, row.get(k).toString());
                    } else {
                        codeValueMap.put(v, "0");
                    }
                });
                logger.info("250101:"+codeValueMap.get(250101) +";250102:"+codeValueMap.get(250102)+";250103:"+codeValueMap.get(250103)+";260105:"+codeValueMap.get(260105));

                jobNumberMap.put(jobNumber, codeValueMap);
            }

        }
        return jobNumberMap;
    }

    @Transactional(rollbackFor = Exception.class)
    public OperationResult updateSalary(List<UpdateSalaryBO> updateSalaryBOList) {
        List<HrmSalaryMonthOptionValue> salaryMonthOptionValueList = new ArrayList<>();
        updateSalaryBOList.forEach(updateSalaryBO -> {
            Long sEmpRecordId = updateSalaryBO.getSEmpRecordId();
            HrmSalaryMonthEmpRecord salaryMonthEmpRecord = null;
            if(updateSalaryBO.getMonth()!=null && updateSalaryBO.getEmployeeId()!=null)
            {
                //如果员工id和月份不为空，则按员工和月份查询
                salaryMonthEmpRecord = salaryMonthEmpRecordService.lambdaQuery()
                        .eq(HrmSalaryMonthEmpRecord::getMonth, updateSalaryBO.getMonth()).eq(HrmSalaryMonthEmpRecord::getEmployeeId, updateSalaryBO.getEmployeeId()).one();
                sEmpRecordId = salaryMonthEmpRecord.getSEmpRecordId();
            }
            else
            {
                 salaryMonthEmpRecord = salaryMonthEmpRecordService.getById(sEmpRecordId);
            }

            Map<Integer, String> map = updateSalaryBO.getSalaryValues();
            Long finalSEmpRecordId = sEmpRecordId;
            map.forEach((code, value) -> {
                salaryMonthOptionValueService.lambdaUpdate().set(HrmSalaryMonthOptionValue::getValue, value)
                        .eq(HrmSalaryMonthOptionValue::getCode, code)
                        .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, finalSEmpRecordId)
                        .update();
            });
            //重新计算薪资
            if(salaryMonthEmpRecord!=null)
            {
                List<HrmSalaryMonthOptionValue> salaryMonthOptionValues = computeSalary(salaryMonthEmpRecord, null,null);
                salaryMonthOptionValueList.addAll(salaryMonthOptionValues);
            }
        });
        salaryMonthOptionValueService.saveBatch(salaryMonthOptionValueList);
        HrmSalaryMonthRecord salaryMonthRecord = queryLastSalaryMonthRecord();
        Map<String, Object> countMap = salaryMonthRecordMapper.queryMonthSalaryCount(salaryMonthRecord.getSRecordId());
        BeanUtil.fillBeanWithMap(countMap, salaryMonthRecord, true);
        updateById(salaryMonthRecord);

        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(salaryMonthRecord.getSRecordId(), salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle());
        operationLog.setOperationInfo("编辑薪资报表：" + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle());
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public OperationResult updateCheckStatus(Integer checkStatus, Integer year, Integer month) {
        LambdaUpdateWrapper<HrmSalaryMonthRecord> wrapper = new LambdaUpdateWrapper<HrmSalaryMonthRecord>()
                .set(HrmSalaryMonthRecord::getCheckStatus, checkStatus)
                .eq(HrmSalaryMonthRecord::getYear, year)
                .and(i -> i.eq(HrmSalaryMonthRecord::getMonth, month));
        update(null, wrapper);

        //查询薪资上月记录,如果有就往后推一个月,如果没有就去薪资配置计薪月
        HrmSalaryMonthRecord lastSalaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).one();
        HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
        LocalDate date = LocalDateTimeUtil.offset(LocalDateTimeUtil.of(DateUtil.parse(lastSalaryMonthRecord.getYear() + "-" + lastSalaryMonthRecord.getMonth(), "yy-MM")), 1, ChronoUnit.MONTHS).toLocalDate();
        int months = date.getMonthValue();
        int years = date.getYear();
        LocalDate startTime = LocalDate.of(years, months, salaryConfig.getSalaryCycleStartDay());
        LocalDate endTime;
        if (salaryConfig.getSalaryCycleStartDay() > 1) {
            LocalDate dateTime = LocalDateTimeUtil.offset(startTime.atStartOfDay(), 1, ChronoUnit.MONTHS).toLocalDate();
            int nextMonth = dateTime.getMonthValue();
            endTime = LocalDate.of(years, nextMonth, salaryConfig.getSalaryCycleEndDay());
        } else {
            endTime = startTime.with(TemporalAdjusters.lastDayOfMonth());
        }
        //当前月薪资数据将变为归档状态
        lastSalaryMonthRecord.setCheckStatus(SalaryRecordStatus.HISTORY.getValue());
        computeSalaryCount(lastSalaryMonthRecord);
        updateById(lastSalaryMonthRecord);
        HrmSalaryMonthRecord salaryMonthRecord = new HrmSalaryMonthRecord();
        salaryMonthRecord.setTitle(HrmLanguageEnum.parseName(months) + HrmLanguageEnum.SALARY_REPORT.getName());
        salaryMonthRecord.setYear(years);
        salaryMonthRecord.setMonth(months);
        salaryMonthRecord.setStartTime(startTime);
        salaryMonthRecord.setEndTime(endTime);
        salaryMonthRecord.setCreateTime(LocalDateTime.now());
        salaryMonthRecord.setNum(queryPaySalaryEmployeeListByType(1, null).size());
        save(salaryMonthRecord);
        return null;
    }


    /**
     薪资审核
     */
    @Transactional(rollbackFor = Exception.class)
    public OperationResult salaryAudit(SalaryAuditDto salaryAuditDto)
    {
        HrmSalaryMonthRecord salaryMonthRecord = getById(salaryAuditDto.getSrecordId());
        OperationLog operationLog = new OperationLog();

        if("1".equals(salaryAuditDto.getOperType()))
        {
            //财务审核
            if("1".equals(salaryAuditDto.getAuditStatus()))
            {
                //财务已审核
                salaryMonthRecord.setCheckStatus(SalaryRecordStatus.UNDER_EXAMINE.getValue());
                operationLog.setOperationObject(salaryMonthRecord.getSRecordId());
                operationLog.setOperationInfo("财务审核已通过");
            }
            else
            {
                //财务审核未通过
                salaryMonthRecord.setCheckStatus(SalaryRecordStatus.FINANCIAL.getValue());
                operationLog.setOperationObject(salaryMonthRecord.getSRecordId());
                operationLog.setOperationInfo("财务审核未通过");
            }

        }
        if("2".equals(salaryAuditDto.getOperType()))
        {
            //总经理审核
            if("1".equals(salaryAuditDto.getAuditStatus()))
            {
                //总经理审核已审核
                salaryMonthRecord.setCheckStatus(SalaryRecordStatus.PASS.getValue());
                //当前月薪资数据将变为归档状态
                salaryMonthRecord.setCheckStatus(SalaryRecordStatus.HISTORY.getValue());
                computeSalaryCount(salaryMonthRecord);
                updateById(salaryMonthRecord);

                //生成下月工资
                HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
                LocalDate date = LocalDateTimeUtil.offset(LocalDateTimeUtil.of(DateUtil.parse(salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getMonth(), "yy-MM")), 1, ChronoUnit.MONTHS).toLocalDate();
                int month = date.getMonthValue();
                int year = date.getYear();
                LocalDate startTime = LocalDate.of(year, month, salaryConfig.getSalaryCycleStartDay());
                LocalDate endTime;
                if (salaryConfig.getSalaryCycleStartDay() > 1) {
                    LocalDate dateTime = LocalDateTimeUtil.offset(startTime.atStartOfDay(), 1, ChronoUnit.MONTHS).toLocalDate();
                    int nextMonth = dateTime.getMonthValue();
                    endTime = LocalDate.of(year, nextMonth, salaryConfig.getSalaryCycleEndDay());
                } else {
                    endTime = startTime.with(TemporalAdjusters.lastDayOfMonth());
                }
                HrmSalaryMonthRecord salaryMonthRecordNext = new HrmSalaryMonthRecord();
                salaryMonthRecordNext.setTitle(HrmLanguageEnum.parseName(month) + HrmLanguageEnum.SALARY_REPORT.getName());
                salaryMonthRecordNext.setYear(year);
                salaryMonthRecordNext.setMonth(month);
                salaryMonthRecordNext.setStartTime(startTime);
                salaryMonthRecordNext.setEndTime(endTime);
                salaryMonthRecordNext.setCreateTime(LocalDateTime.now());
                salaryMonthRecordNext.setNum(queryPaySalaryEmployeeListByType(1, null).size());
                save(salaryMonthRecordNext);
                salaryActionRecordService.addNextMonthSalaryLog(salaryMonthRecordNext);
                operationLog.setOperationObject(salaryMonthRecordNext.getSRecordId(), year + "-" + salaryMonthRecordNext.getTitle());
                operationLog.setOperationInfo("总经理审核通过");

            }
            else
            {
                //总经理审核未通过
                salaryMonthRecord.setCheckStatus(SalaryRecordStatus.REFUSE.getValue());
                operationLog.setOperationObject(salaryMonthRecord.getSRecordId());
                operationLog.setOperationInfo("总经理审核未通过");
            }

        }
        if("3".equals(salaryAuditDto.getOperType()))
        {
            //行政提交
            salaryMonthRecord.setCheckStatus(SalaryRecordStatus.WAIT_EXAMINE.getValue());
            operationLog.setOperationObject(salaryMonthRecord.getSRecordId());
            operationLog.setOperationInfo("行政提交");
        }
        updateById(salaryMonthRecord);
        return null;
    }

    public QuerySalaryMonthRecrodButtonStatusVO showButtonStatus(SalaryAuditDto salaryAuditDto) {
        HrmSalaryMonthRecord hrmSalaryMonthRecord = getById(salaryAuditDto.getSrecordId());
        QuerySalaryMonthRecrodButtonStatusVO vo = new QuerySalaryMonthRecrodButtonStatusVO();
        if (hrmSalaryMonthRecord != null) {
            if (hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.CREATED.getValue()) {
                //新创建,薪资未生成
                vo.setCalculateSalary(true);
            }else if (hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.COMPUTE.getValue()) {
                //核算完成
                vo.setSendPaySlip(true);
                vo.setOnlineEditing(true);
                vo.setImportAdditionalDeduction(true);
                vo.setCalculateSalary(true);
            }else if (hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.XZ_CONFIRM.getValue()) {
                //员工确认完毕
                vo.setFinancialRevie(true);
            }else if (hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.UNDER_EXAMINE.getValue()) {
                //财务已审核
                vo.setGeneralManagerReview(true);
            }else if (hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.FINANCIAL.getValue() || hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.REFUSE.getValue()) {
                //财务或总经理审核未通过
                vo.setAdministrativeReview(true);
            }else if (hrmSalaryMonthRecord.getCheckStatus() == SalaryRecordStatus.WAIT_EXAMINE.getValue()) {
                //财务或总经理审核驳回后再次提交审核
                vo.setFinancialRevie(true);
            }
        }
        return vo;
    }


    /**
     * 判断是否是否全勤
     */
    private boolean checkIsFullAttendance(Long employeeId,List<HrmAttendanceSummaryDayVo> summaryDayVoList,List<HrmAttendanceSummaryVo> summaryVoList,int year,int month,List<QueryHolidayDeductionVO> deductionVOList)
    {
        //除了调休 和 年假，以及每日迟到15分钟以内的，都算缺勤
        boolean isFull = false;
        HrmAttendanceSummaryVo summaryVo= summaryVoList.stream().filter(f -> f.getEmployeeId().equals(employeeId)).findAny().orElse(null);;
        if(summaryVo==null)
        {
            //找不到考勤统计数据，则不为全勤
            return false;
        }
        //过滤出来员工的月度考勤统计数据
        //标识是否有迟到30分钟以上的考勤
        boolean isOverLate30Minute = false;
        //过滤出员工每日的考勤
        List<HrmAttendanceSummaryDayVo> employeeDayAttendanceList = summaryDayVoList.stream().filter(f -> f.getEmployeeId().equals(employeeId)).collect(Collectors.toList());


        //找出是否有迟到超过30分钟的考勤记录
        List<HrmAttendanceSummaryDayVo> late30MinuteList = employeeDayAttendanceList.stream().filter(f -> f.getLateMinute()>30).collect(Collectors.toList());
        if(CollectionUtil.isNotEmpty(late30MinuteList))
        {
            isOverLate30Minute =true;
            int totalLateMinute = late30MinuteList.stream().mapToInt(HrmAttendanceSummaryDayVo::getLateMinute).sum();
            if(CollectionUtil.isNotEmpty(deductionVOList))
            {
                //查询是否有抵扣 值
                QueryHolidayDeductionVO deductionVoChidao= deductionVOList.stream().filter(f -> f.getType()==1).findAny().orElse(null);
                if(deductionVoChidao!=null && deductionVoChidao.getDeductionTime().compareTo(new BigDecimal(totalLateMinute))>=0)
                {
                    isOverLate30Minute =false;
                }
            }
        }

        if(CollectionUtil.isNotEmpty(deductionVOList))
        {
            //添加的 早退 抵扣时长
            QueryHolidayDeductionVO deductionVoZaotui= deductionVOList.stream().filter(f -> f.getType()==2).findAny().orElse(null);
            if(deductionVoZaotui!=null)
            {
                BigDecimal deductionTimeZaotui = deductionVoZaotui.getDeductionTime();//分钟
                //请假抵扣时长 早退
                BigDecimal needDiKouZaotui = new BigDecimal(summaryVo.getEarlyMinute());
                if(deductionTimeZaotui.compareTo(needDiKouZaotui)>=0)
                {
                    summaryVo.setEarlyMinute(0);
                }
            }
            //添加的 事假 抵扣时长
            QueryHolidayDeductionVO deductionVoShiJia= deductionVOList.stream().filter(f -> f.getType()==3).findAny().orElse(null);
            if(deductionVoShiJia!=null)
            {
                BigDecimal deductionTimeQingJia = deductionVoShiJia.getDeductionTime();//分钟
                //请假抵扣时长 事假
                BigDecimal needDiKouShiJia = new BigDecimal(summaryVo.getShijia());
                needDiKouShiJia = needDiKouShiJia.multiply(new BigDecimal(60));
                if(deductionTimeQingJia.compareTo(needDiKouShiJia)>=0)
                {
                    //如果添加的 请假时长大于 本月发生的事假时长，则修改本月事假时长为0
                    summaryVo.setShijia(0d);
                }
            }

            //病假抵扣
            QueryHolidayDeductionVO deductionVoBingJia= deductionVOList.stream().filter(f -> f.getType()==4).findAny().orElse(null);
            if(deductionVoBingJia!=null)
            {
                BigDecimal deductionTimeBingJia = deductionVoBingJia.getDeductionTime();//分钟
                //请假抵扣时长 病假
                BigDecimal needDiKouBingJia = new BigDecimal(summaryVo.getBingjia());
                needDiKouBingJia = needDiKouBingJia.multiply(new BigDecimal(60));
                if(deductionTimeBingJia.compareTo(needDiKouBingJia)>=0)
                {
                    //如果添加的 病假时长大于 本月发生的病假时长，则修改本月病假时长为0
                    summaryVo.setBingjia(0d);
                }
            }


            //调休抵扣
            QueryHolidayDeductionVO deductionVoTiaoxiu= deductionVOList.stream().filter(f -> f.getType()==5).findAny().orElse(null);
            if(deductionVoTiaoxiu!=null)
            {
                BigDecimal deductionTimeTiaoxiu = deductionVoTiaoxiu.getDeductionTime();//分钟
                //调休抵扣
                BigDecimal needDiKouTiaoxiu = new BigDecimal(summaryVo.getTiaoxiu());
                needDiKouTiaoxiu = needDiKouTiaoxiu.multiply(new BigDecimal(60));
                if(deductionTimeTiaoxiu.compareTo(needDiKouTiaoxiu)>=0)
                {
                    //调休抵扣
                    summaryVo.setTiaoxiu(0d);
                }
            }


            //缺卡补卡抵扣
            QueryHolidayDeductionVO deductionVoBuKa= deductionVOList.stream().filter(f -> f.getType()==6).findAny().orElse(null);
            if(deductionVoBuKa!=null)
            {
                BigDecimal deductionCountBuKa = deductionVoBuKa.getDeductionTime();//次数
                //缺卡补卡 抵扣
                if(deductionCountBuKa.compareTo(new BigDecimal(summaryVo.getMisscardCount()))>=0)
                {
                    //缺卡补卡 抵扣
                    summaryVo.setMisscardCount(0);
                    summaryVo.setEarlyCount(0);
                }
            }


        }
        //如果缺卡次数=0 && 旷工天数=0 &产假=0 & 婚假=0 & 陪产假=0 &丧假=0 &事假=0 & 哺乳假=0 & 病假=0 &早退=0 &调休=0 & 早退=0 并且没有迟到超过30分钟以上的考勤记录，则判断为全勤
        if((0==summaryVo.getMisscardCount() && 0==summaryVo.getAbsenteeismDays() &&  0==summaryVo.getAbsenteeismDays() && 0==summaryVo.getTiaoxiu()
                && 0==summaryVo.getChanjia() && 0==summaryVo.getHunjia() && 0==summaryVo.getPeichanjia() && 0==summaryVo.getSangjia() && 0==summaryVo.getEarlyMinute()
                 && 0==summaryVo.getShijia() && 0==summaryVo.getBurujia() && 0==summaryVo.getBingjia() && 0==summaryVo.getEarlyCount()) && !isOverLate30Minute)
        {
            isFull = true;
        }
        return isFull;

    }

    /**
     * 获取员工每天的排班时长(小时)
     * @param employeeId 员工id
     * @param isProduce 是否生产线员工
     * @param dates 考勤日期区间
     * @return
     */
    private HashMap<String,Double> getWorkHours(Long employeeId,boolean isProduce,List<String> dates)
    {
        HashMap<String,Double> workTimesMap = new HashMap<>();
        if(!isProduce)
        {
            //非生产部的，工作时长为8小时
            for(String date : dates)
            {
                workTimesMap.put(date,8d);
            }
            return workTimesMap;
        }
        HashMap<String,Object> params = new HashMap<>();
        params.put("employeeId",employeeId);
        for(String date : dates)
        {
            //获取生产线 各部门员工的每日排班时长
            params.put("shiftDate",date);
            HrmAttendanceShiftVO hrmAttendanceShiftVO = attendanceShiftMapper.getEmpHrmAttendanceShift(params);
            if(hrmAttendanceShiftVO!=null)
            {
                workTimesMap.put(date,(double)hrmAttendanceShiftVO.getShiftHours());
            }
            else
            {
                //休息日,工作时长设置为0
                workTimesMap.put(date,0d);
            }

        }
        return workTimesMap;
    }

    /**
     * 员工病假,事假天数计算
     * @param employeeId
     * @param isProduce
     * @param summaryDayVoList
     * type  1事假 2病假
     * @return
     */
    private BigDecimal sickDeductDays(Long employeeId,boolean isProduce,List<HrmAttendanceSummaryDayVo> summaryDayVoList,HashMap<String,Double> workTimesMap,String type)
    {
        List<HrmAttendanceSummaryDayVo> employeeDayAttendanceList = summaryDayVoList.stream()
                .filter(f -> f.getEmployeeId().equals(employeeId)).collect(Collectors.toList());
        //请假天数
        BigDecimal leavelDays = new BigDecimal(0);

        if(isProduce)
        {
            //生产相关部门计算病假天数，依赖排班工时
            //遍历每日的排班工时
            for (Map.Entry<String, Double> entry : workTimesMap.entrySet())
            {
                String date = entry.getKey();
                Double workHours = entry.getValue();
                //判断当天是否有病假
                Optional<HrmAttendanceSummaryDayVo> optional= employeeDayAttendanceList.stream().filter(f -> f.getWorkDate().equals(date)).findFirst();
                if(optional.isPresent())
                {
                    HrmAttendanceSummaryDayVo summaryDayVo = optional.get();
                    if("2".equals(type))
                    {
                        if(workHours!=0d && summaryDayVo.getBingjia()!=0d)
                        {
                            if(summaryDayVo.getBingjia()!=null && summaryDayVo.getBingjia()<=workHours/2)
                            {
                                //如果请假时长小于等于当日排班时长的一半，则按半天计算
                                leavelDays = leavelDays.add(new BigDecimal(0.5));
                            }
                            else if(summaryDayVo.getBingjia()!=null && summaryDayVo.getBingjia()>workHours/2)
                            {
                                //如果请假时大于当日排班时长的一半，则按一天计算
                                leavelDays = leavelDays.add(new BigDecimal(1));
                            }
                        }

                    }
                    else if("1".equals(type))
                    {
                        if(workHours!=0d && summaryDayVo.getShijia()!=0d)
                        {
                            if(summaryDayVo.getShijia()!=null && summaryDayVo.getShijia()<=workHours/2)
                            {
                                //如果请假时长小于等于当日排班时长的一半，则按半天计算
                                leavelDays = leavelDays.add(new BigDecimal(0.5));
                            }
                            else if(summaryDayVo.getShijia()!=null && summaryDayVo.getShijia()>workHours/2)
                            {
                                //如果请假时大于当日排班时长的一半，则按一天计算
                                leavelDays = leavelDays.add(new BigDecimal(1));
                            }
                        }

                    }

                }
            }
        }
        else
        {
            if("2".equals(type))
            {
                //获取员工对应的病假时间
                Double sickHours = employeeDayAttendanceList.stream().mapToDouble(HrmAttendanceSummaryDayVo::getBingjia).sum();
                //非生产部门，算出对应的天数,按一天8小时计算
                leavelDays = new BigDecimal(sickHours).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
            }
            else if("1".equals(type))
            {
                //获取员工对应的事假时间
                Double shiJiaHours = employeeDayAttendanceList.stream().mapToDouble(HrmAttendanceSummaryDayVo::getShijia).sum();
                //非生产部门，算出对应的天数,按一天8小时计算
                leavelDays = new BigDecimal(shiJiaHours).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
            }

        }
        return leavelDays;
    }

    /**
     * 获取员工事假天数
     * @param employeeId
     * @param isProduce
     * @param workTimesMap
     * @return
     */
    private double shiJiaDays(Long employeeId,boolean isProduce,HrmAttendanceSummaryVo empAttendanceSummary,HashMap<String,Double> workTimesMap)
    {

        //事假请假天数
        double shiJiaDays = 0;
//        if(!isProduce)
//        {
//            if(empAttendanceSummary.getShijia()!=null && empAttendanceSummary.getShijia()!=0)
//            {
//                //事假
//                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getShijia());
//                shijia = b1.divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP).doubleValue();
//                empAttendanceSummary.setShijia(shijia);
//            }
//        }
//        else
//        {
//
//        }
        return shiJiaDays;
    }


//    private Double getActualityDays(Long employeeId,boolean isProduce,List<HrmAttendanceSummaryDayVo> summaryDayVoList,Double actualityDays)
//    {
//        Double days = actualityDays;
//        List<HrmAttendanceSummaryDayVo> empDaySummaryVolist = summaryDayVoList.stream()
//                .filter(f -> f.getEmployeeId().equals(employeeId)).collect(Collectors.toList());
//        if(!isProduce)
//        {
//            for(HrmAttendanceSummaryDayVo summaryDayVo : empDaySummaryVolist)
//            {
//                if(summaryDayVo.getActualityDays()>0 && summaryDayVo.getShijia()>)
//            }
//        }
//
//
//        return days;
//
//    }


    /**
     * 获取员工全勤天数
     * @param empAttendanceSummary
     * @param isProduce
     * @param daysInMonth
     * @return
     */

    private Double getNormalDays(HrmAttendanceSummaryVo empAttendanceSummary,boolean isProduce,int daysInMonth)
    {
        Double normalDays =1d;
        if(!isProduce)
        {
            //非生产类型部门，请假小时换算为天数时，一天按8小时计算
            Double shijia = 0d;
            if(empAttendanceSummary.getShijia()!=null && empAttendanceSummary.getShijia()!=0)
            {
                //事假
                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getShijia());
                shijia = b1.divide(new BigDecimal(8),BigDecimal.ROUND_CEILING).setScale(2, RoundingMode.HALF_UP).doubleValue();
                empAttendanceSummary.setShijia(shijia);
            }
            Double bingjia = 0d;
            if(empAttendanceSummary.getBingjia()!=null && empAttendanceSummary.getBingjia()!=0)
            {
                //病假
                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getBingjia());
                bingjia = b1.divide(new BigDecimal(8),BigDecimal.ROUND_CEILING).setScale(2, RoundingMode.HALF_UP).doubleValue();
                empAttendanceSummary.setBingjia(bingjia);
            }
            Double burujia = 0d;
            if(empAttendanceSummary.getBurujia()!=null && empAttendanceSummary.getBurujia()!=0)
            {
                //哺乳假
                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getBurujia());
                burujia = b1.divide(new BigDecimal(8),BigDecimal.ROUND_CEILING).setScale(2, RoundingMode.HALF_UP).doubleValue();
                empAttendanceSummary.setBingjia(burujia);
            }
            //应出勤天数=实际出勤天数+事假+病假+婚嫁+陪产假+丧假+哺乳假+产假 +旷工天数
            normalDays = empAttendanceSummary.getActualityDays()
                    +empAttendanceSummary.getAbsenteeismDays()
                    +empAttendanceSummary.getChanjia()
                    +empAttendanceSummary.getHunjia()
                    +empAttendanceSummary.getPeichanjia()
                    +empAttendanceSummary.getSangjia()
                    +empAttendanceSummary.getShijia()
                    +empAttendanceSummary.getBingjia()
                    +empAttendanceSummary.getBurujia();
        }
        else
        {
            //生产部门，应出勤天数=当月总天数-4（4为休息天数）
            normalDays = (double)(daysInMonth -4);
        }
        return normalDays;
    }


    /**
     * 薪资导出
     */
    public void exportSalary(QuerySalaryExportDto querySalaryExportDto, HttpServletResponse response) throws IOException {
        List<SalaryOptionHeadVO> headVOList = querySalaryOptionHead();

        SalaryOptionHeadVO headDept = new SalaryOptionHeadVO(-1,"部门",1);
        SalaryOptionHeadVO headName = new SalaryOptionHeadVO(-2,"姓名",1);
        headVOList.add(0,headDept);
        headVOList.add(1,headName);
        List<String> exportHead = headVOList.stream().filter(x -> !x.getName().equals("")).map(SalaryOptionHeadVO::getName).collect(Collectors.toList());

        List<Long> employeeIds = new ArrayList<>();

        //查询出已经定薪了的人员列表
        employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).ne(HrmEmployee::getIsDel, 1).list()
                .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));

        QuerySalaryPageListDto querySalaryPageListDto = new QuerySalaryPageListDto();
        querySalaryPageListDto.setSRecordId(querySalaryExportDto.getSalaryRecordId());
        BeanUtils.copyProperties(querySalaryExportDto,querySalaryPageListDto);
        List<QuerySalaryPageListVO> salaryPageListVOS = salaryMonthEmpRecordMapper.querySalaryMonthList(querySalaryPageListDto,employeeIds);
        if (CollectionUtil.isEmpty(salaryPageListVOS))
        {
            return ;
        }
        List<List<Object>> dataList = ListUtils.newArrayList();
        for(QuerySalaryPageListVO vo : salaryPageListVOS)
        {
            List<ComputeSalaryDto> list = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(vo.getSEmpRecordId());
            List<QuerySalaryPageListVO.SalaryValue> salaryValues = TransferUtil.transferList(list, QuerySalaryPageListVO.SalaryValue.class);
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 1, vo.getNeedWorkDay().toString(), 1, "应出勤天数"));
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, vo.getActualWorkDay().toString(), 1, "实际出勤天数"));

            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, -1, vo.getDeptName().toString(), 1, "部门"));
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, -2, vo.getEmployeeName().toString(), 1, "姓名"));

            vo.setSalary(salaryValues);

            List<Object> data = ListUtils.newArrayList();
            for(SalaryOptionHeadVO headVO : headVOList)
            {
                QuerySalaryPageListVO.SalaryValue salaryValue = vo.getSalary().stream().filter(f -> f.getCode().toString().equals(headVO.getCode().toString())).findAny().orElse(null);
                if(salaryValue!=null)
                {
                    data.add(salaryValue.getValue());
                }
                else
                {
                    data.add("");
                }

            }
            dataList.add(data);


        }
        List<List<String>> headLists = exportHead.stream().map(x -> Lists.newArrayList(x)).collect(Collectors.toList());

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDateTime = sdf.format(now);

        //内容样式策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        //垂直居中,水平居中
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        //设置 自动换行
        contentWriteCellStyle.setWrapped(true);
        // 字体策略
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 12);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        //头策略使用默认 设置字体大小
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 12);
        headWriteCellStyle.setWriteFont(headWriteFont);

        String fileName = "工资表导出 "+ formattedDateTime+".xlsx";
        fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);
        // 需要写入文件夹就是用 DileOutPutStram
        // 如果是接口请求直接浏览器下载，就使用 response.getOutputStream()
        EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle))
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(headLists)
                .sheet()
                .doWrite(dataList);

    }
}
