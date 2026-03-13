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
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.tianye.hrsystem.autoTask.AttendanceReportRefreshTask;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.*;
import com.tianye.hrsystem.entity.vo.*;
import com.tianye.hrsystem.enums.*;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.imple.HrmProduceAttendanceServiceImpl;
import com.tianye.hrsystem.mapper.*;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.entity.HrmEmployeeAdditional;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.additional.service.HrmAdditionalService;
import com.tianye.hrsystem.modules.additional.service.HrmEmployeeAdditionalService;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.attendanceinfo.entity.HrmAttendanceInfo;
import com.tianye.hrsystem.modules.attendanceinfo.mapper.HrmAttendanceInfoMapper;
import com.tianye.hrsystem.modules.deduction.mapper.HrmPersonalIncomeTaxMapper;
import com.tianye.hrsystem.modules.deduction.vo.QueryPersonalIncomeTaxVO;
import com.tianye.hrsystem.modules.holiday.service.HrmHolidayDeductionService;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthRecord;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthEmpRecordService;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthRecordService;
import com.tianye.hrsystem.modules.salary.dto.*;
import com.tianye.hrsystem.modules.salary.entity.*;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryExportMapper;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthEmpRecordMapper;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthOptionValueMapper;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthRecordMapper;
import com.tianye.hrsystem.modules.salary.vo.SalaryOptionHeadVO;
import com.tianye.hrsystem.modules.salary.vo.*;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.service.IHrmAttendanceClockService;
import com.tianye.hrsystem.service.IHrmAttendanceGroupService;
import com.tianye.hrsystem.service.IHrmAttendanceReportDataService;
import com.tianye.hrsystem.service.IHrmAttendanceRuleService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeQuitInfoService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import com.tianye.hrsystem.util.*;
import net.sf.cglib.core.Local;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.Option;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 每月薪资记录 服务实现类
 */
@Service
public class SalaryMonthRecordServiceNew extends BaseServiceImpl<HrmSalaryMonthRecordMapper, HrmSalaryMonthRecord> {

    private static final Logger logger = LoggerFactory.getLogger(SalaryMonthRecordServiceNew.class);


    @Autowired
    private HrmSalaryMonthEmpRecordMapper salaryMonthEmpRecordMapper;

    @Autowired
    private HrmSalaryMonthRecordMapper salaryMonthRecordMapper;

    @Autowired
    private HrmEmployeeMapper hrmEmployeeMapper;
    @Autowired
    private hrmDeptRepository hrmDeptRepository;
    @Autowired
    private HrmSalaryMonthOptionValueService salaryMonthOptionValueService;
    @Autowired
    private HrmSalaryMonthOptionValueMapper salaryMonthOptionValueMapper;

    @Autowired
    private HrmSalaryArchivesService salaryArchivesService;

    @Autowired
    private HrmSalaryArchivesOptionService salaryArchivesOptionService;

    @Autowired
    private HrmSalaryMonthEmpRecordService salaryMonthEmpRecordService;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    private IHrmEmployeeQuitInfoService hrmEmployeeQuitInfoService;

    @Autowired
    private HrmSalaryGroupService hrmSalaryGroupService;

    @Autowired
    private IHrmSalaryConfigService hrmSalaryConfigService;

    @Autowired
    private HrmSalaryOptionService hrmSalaryOptionService;

    @Autowired
    private SalaryComputeServiceNew salaryComputeService;

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
    private HrmProduceAttendanceServiceImpl produceAttendanceService;

    @Autowired
    private HrmAttendancePlanMapper planMapper;

    @Autowired
    private HrmHolidayDeductionService holidayDeductionService;

    @Autowired
    HrmPersonalIncomeTaxMapper incomeTaxMapper;

    @Autowired
    HrmAdditionalMapper additionalMapper;

    @Autowired
    HrmSalaryMonthEmpRecordService hrmSalaryMonthEmpRecordService;

    @Autowired
    HrmSalaryMonthEmpRecordMapper hrmSalaryMonthEmpRecordMapper;

    @Autowired
    HrmAdditionalService hrmAdditionalService;

    @Autowired
    HrmAdditionalMapper hrmAdditionalMapper;

    @Autowired
    HrmEmployeeAdditionalService hrmEmployeeAdditionalService;

    @Autowired
    HrmAttendanceInfoMapper hrmAttendanceInfoMapper;

    @Autowired
    HrmSalaryExportMapper exportMapper;

    @Value("${hrm.system.companyname}")
    private String companyName;
    private static final int TWO = 2;

    private static final int FIVE = 5;

    private static final int FOUR = 4;

    private static final int STATUS = 11;

    private static final int ZERO = 0;

    private static final int THREE = 3;

    private static final int ONE = 1;

    /** 入职状态：离职 */
    private static final String ENTRY_STATUS_QUIT = "4";
    /** 是否残疾：否（参与全勤/个税计算） */
    private static final String IS_DISABLED_NO = "2";
    /** 是否残疾：是 */
    private static final String IS_DISABLED_YES = "1";
    /** 全勤标识：有全勤 */
    private static final String FULL_ATTENDANCE_FLAG_YES = "1";
    /** 部门类型：生产 */
    private static final String IS_PRODUCE_DEPT_YES = "1";
    /** 默认应出勤天数 */
    private static final double DEFAULT_NORMAL_DAYS = 21.75;
    private static final BigDecimal DEFAULT_NORMAL_DAYS_DECIMAL = new BigDecimal("21.75");
    private static final BigDecimal MID_MONTH_OVERTIME_UNIT_PRICE = new BigDecimal("12");
    private static final BigDecimal MONTHLY_TAX_FREE_DEDUCTION = new BigDecimal("5000");
    private static final Set<Integer> EXCLUDED_NO_FIXED_CODES = new HashSet<>(Arrays.asList(
            100101, 100102, 110101, 120101,
            40102, 280, 281, 282, 20105, 20102
    ));
    private static final ConcurrentHashMap<Long, ReentrantLock> COMPUTE_RECORD_LOCK_MAP = new ConcurrentHashMap<>();

    /**
     * 考勤同步时一次性加载的批量数据，避免在员工循环内重复查询，提升性能与可读性。
     */
    private static class AttendanceSyncBatchData {
        final List<Long> employeeIds;
        final List<HrmProduceAttendance> hasOverTimePayEmpList;
        final Map<Long, HrmProduceAttendance> overTimePayEmpMap;
        final HrmSalaryBasic salaryBasic;
        final HrmAttendanceRule attendanceRule;
        final Date beginDate;
        final Date endDate;
        final Map<Integer, Double> normalDaysByDeptType;
        final List<HrmAttendanceSummaryDayVo> attendanceSummaryVoDayList;
        final Map<Long, List<HrmAttendanceSummaryDayVo>> attendanceSummaryDayMap;
        final List<HrmAttendanceSummaryVo> attendanceSummaryVoList;
        final Map<Long, HrmAttendanceSummaryVo> attendanceSummaryMap;
        final List<QuerySalaryArchivesListVO> empSalaryArchivesList;
        final Map<Long, QuerySalaryArchivesListVO> empSalaryArchivesMap;
        final LocalDate dateStartTime;
        final LocalDate dateEndTime;
        final int daysInMonth;
        final List<String> dates;
        final Map<Long, List<QueryHolidayDeductionVO>> holidayDeductionMap;
        final Map<Long, Map<String, Double>> workHoursMap;
        final Map<Long, String> deptNameMap;

        AttendanceSyncBatchData(List<Long> employeeIds, List<HrmProduceAttendance> hasOverTimePayEmpList,
                                HrmSalaryBasic salaryBasic, HrmAttendanceRule attendanceRule,
                                Date beginDate, Date endDate, Map<Integer, Double> normalDaysByDeptType,
                                List<HrmAttendanceSummaryDayVo> attendanceSummaryVoDayList,
                                Map<Long, List<HrmAttendanceSummaryDayVo>> attendanceSummaryDayMap,
                                List<HrmAttendanceSummaryVo> attendanceSummaryVoList,
                                Map<Long, HrmAttendanceSummaryVo> attendanceSummaryMap,
                                List<QuerySalaryArchivesListVO> empSalaryArchivesList,
                                Map<Long, QuerySalaryArchivesListVO> empSalaryArchivesMap,
                                LocalDate dateStartTime, LocalDate dateEndTime, int daysInMonth, List<String> dates,
                                Map<Long, List<QueryHolidayDeductionVO>> holidayDeductionMap,
                                Map<Long, Map<String, Double>> workHoursMap,
                                Map<Long, String> deptNameMap) {
            this.employeeIds = employeeIds;
            this.hasOverTimePayEmpList = hasOverTimePayEmpList != null ? hasOverTimePayEmpList : Collections.emptyList();
            this.overTimePayEmpMap = toProduceAttendanceMap(this.hasOverTimePayEmpList);
            this.salaryBasic = salaryBasic;
            this.attendanceRule = attendanceRule;
            this.beginDate = beginDate;
            this.endDate = endDate;
            this.normalDaysByDeptType = normalDaysByDeptType != null ? normalDaysByDeptType : Collections.emptyMap();
            this.attendanceSummaryVoDayList = attendanceSummaryVoDayList != null ? attendanceSummaryVoDayList : Collections.emptyList();
            this.attendanceSummaryDayMap = attendanceSummaryDayMap != null ? attendanceSummaryDayMap : Collections.emptyMap();
            this.attendanceSummaryVoList = attendanceSummaryVoList != null ? attendanceSummaryVoList : Collections.emptyList();
            this.attendanceSummaryMap = attendanceSummaryMap != null ? attendanceSummaryMap : Collections.emptyMap();
            this.empSalaryArchivesList = empSalaryArchivesList != null ? empSalaryArchivesList : Collections.emptyList();
            this.empSalaryArchivesMap = empSalaryArchivesMap != null ? empSalaryArchivesMap : Collections.emptyMap();
            this.dateStartTime = dateStartTime;
            this.dateEndTime = dateEndTime;
            this.daysInMonth = daysInMonth;
            this.dates = dates != null ? dates : Collections.emptyList();
            this.holidayDeductionMap = holidayDeductionMap != null ? holidayDeductionMap : Collections.emptyMap();
            this.workHoursMap = workHoursMap != null ? workHoursMap : Collections.emptyMap();
            this.deptNameMap = deptNameMap != null ? deptNameMap : Collections.emptyMap();
        }
    }

    private static class SalaryOptionBatchData {
        final Map<Integer, Integer> optionParentCodeMap;
        final Map<Integer, HrmSalaryOption> salaryOptionConfigMap;
        final List<HrmSalaryOption> noFixedSalaryOptionList;

        SalaryOptionBatchData(Map<Integer, Integer> optionParentCodeMap,
                              Map<Integer, HrmSalaryOption> salaryOptionConfigMap,
                              List<HrmSalaryOption> noFixedSalaryOptionList) {
            this.optionParentCodeMap = optionParentCodeMap != null ? optionParentCodeMap : Collections.emptyMap();
            this.salaryOptionConfigMap = salaryOptionConfigMap != null ? salaryOptionConfigMap : Collections.emptyMap();
            this.noFixedSalaryOptionList = noFixedSalaryOptionList != null ? noFixedSalaryOptionList : Collections.emptyList();
        }
    }

    private static class SalaryComputeBatchData {
        final List<Map<String, Object>> employeeMapList;
        final Map<String, Map<Integer, String>> attendanceDataMap;
        final Map<Long, Boolean> hasAttendanceGroupMap;
        final SalaryOptionBatchData salaryOptionBatchData;
        final Map<Long, HrmProduceAttendance> produceAttendanceMap;
        final Map<Integer, Double> normalDaysByDeptType;
        final Map<Long, Map<Integer, String>> lastMonthTaxDataMap;
        final Map<Long, BigDecimal> lastYearAccumulatedIncomeMap;
        final Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap;
        final Map<Long, HrmAdditional> additionalDeductionMap;
        final Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap;

        SalaryComputeBatchData(List<Map<String, Object>> employeeMapList,
                               Map<String, Map<Integer, String>> attendanceDataMap,
                               Map<Long, Boolean> hasAttendanceGroupMap,
                               SalaryOptionBatchData salaryOptionBatchData,
                               Map<Long, HrmProduceAttendance> produceAttendanceMap,
                               Map<Integer, Double> normalDaysByDeptType,
                               Map<Long, Map<Integer, String>> lastMonthTaxDataMap,
                               Map<Long, BigDecimal> lastYearAccumulatedIncomeMap,
                               Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap,
                               Map<Long, HrmAdditional> additionalDeductionMap,
                               Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap) {
            this.employeeMapList = employeeMapList != null ? employeeMapList : Collections.emptyList();
            this.attendanceDataMap = attendanceDataMap != null ? attendanceDataMap : Collections.emptyMap();
            this.hasAttendanceGroupMap = hasAttendanceGroupMap != null ? hasAttendanceGroupMap : Collections.emptyMap();
            this.salaryOptionBatchData = salaryOptionBatchData != null ? salaryOptionBatchData
                    : new SalaryOptionBatchData(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
            this.produceAttendanceMap = produceAttendanceMap != null ? produceAttendanceMap : Collections.emptyMap();
            this.normalDaysByDeptType = normalDaysByDeptType != null ? normalDaysByDeptType : Collections.emptyMap();
            this.lastMonthTaxDataMap = lastMonthTaxDataMap != null ? lastMonthTaxDataMap : Collections.emptyMap();
            this.lastYearAccumulatedIncomeMap = lastYearAccumulatedIncomeMap != null ? lastYearAccumulatedIncomeMap : Collections.emptyMap();
            this.socialSecurityEmpRecordMap = socialSecurityEmpRecordMap != null ? socialSecurityEmpRecordMap : Collections.emptyMap();
            this.additionalDeductionMap = additionalDeductionMap != null ? additionalDeductionMap : Collections.emptyMap();
            this.midMonthArchivesOptionMap = midMonthArchivesOptionMap != null ? midMonthArchivesOptionMap : Collections.emptyMap();
        }
    }

    private static class EmployeeComputeScopeData {
        final List<Map<String, Object>> employeeMapList;
        final Map<String, Map<Integer, String>> attendanceDataMap;
        final Map<Long, Boolean> hasAttendanceGroupMap;
        final AttendanceSyncBatchData attendanceSyncBatchData;

        EmployeeComputeScopeData(List<Map<String, Object>> employeeMapList,
                                 Map<String, Map<Integer, String>> attendanceDataMap,
                                 Map<Long, Boolean> hasAttendanceGroupMap,
                                 AttendanceSyncBatchData attendanceSyncBatchData) {
            this.employeeMapList = employeeMapList != null ? employeeMapList : Collections.emptyList();
            this.attendanceDataMap = attendanceDataMap != null ? attendanceDataMap : Collections.emptyMap();
            this.hasAttendanceGroupMap = hasAttendanceGroupMap != null ? hasAttendanceGroupMap : Collections.emptyMap();
            this.attendanceSyncBatchData = attendanceSyncBatchData;
        }
    }

    private static class HistoryComputeData {
        final Map<Long, Map<Integer, String>> lastMonthTaxDataMap;
        final Map<Long, BigDecimal> lastYearAccumulatedIncomeMap;
        final Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap;
        final Map<Long, HrmAdditional> additionalDeductionMap;
        final Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap;

        HistoryComputeData(Map<Long, Map<Integer, String>> lastMonthTaxDataMap,
                           Map<Long, BigDecimal> lastYearAccumulatedIncomeMap,
                           Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap,
                           Map<Long, HrmAdditional> additionalDeductionMap,
                           Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap) {
            this.lastMonthTaxDataMap = lastMonthTaxDataMap != null ? lastMonthTaxDataMap : Collections.emptyMap();
            this.lastYearAccumulatedIncomeMap = lastYearAccumulatedIncomeMap != null ? lastYearAccumulatedIncomeMap : Collections.emptyMap();
            this.socialSecurityEmpRecordMap = socialSecurityEmpRecordMap != null ? socialSecurityEmpRecordMap : Collections.emptyMap();
            this.additionalDeductionMap = additionalDeductionMap != null ? additionalDeductionMap : Collections.emptyMap();
            this.midMonthArchivesOptionMap = midMonthArchivesOptionMap != null ? midMonthArchivesOptionMap : Collections.emptyMap();
        }
    }

    /**
     * 根据员工 map 计算部门类型（0 行政 1 生产），与考勤应出勤天数逻辑一致。
     */
    private int getDeptTypeForEmployee(Map<String, Object> map) {
        if (map == null) return 0;
        Long employeeId = Convert.toLong(map.get("employeeId"));
        String isProduceDept = map.get("isProduceDept") != null ? String.valueOf(map.get("isProduceDept")) : "0";
        boolean isProduce = IS_PRODUCE_DEPT_YES.equals(isProduceDept);
        if (!isProduce) {
            if (employeeId != null && ("1712718940198".equals(employeeId.toString()) || "1712718940199".equals(employeeId.toString()) || "1712718940200".equals(employeeId.toString()))) {
                return 1;
            }
            return 0;
        }
        if (employeeId != null && ("1712718940227".equals(employeeId.toString()) || "1831601326890434591".equals(employeeId.toString())
                || "1831601326890434610".equals(employeeId.toString()) || "1831601326890434648".equals(employeeId.toString()))) {
            return 0;
        }
        return 1;
    }

    /**
     * 考勤同步时一次性加载当月批量数据（考勤统计、薪资档案、规则等），避免在员工循环内重复查询。
     * @return 无计薪员工时返回 null
     */
    private AttendanceSyncBatchData loadAttendanceSyncBatchData(int year, int month, List<Map<String, Object>> mapList) {
        if (CollUtil.isEmpty(mapList)) {
            return null;
        }
        List<Long> employeeIds = mapList.stream()
                .map(m -> Convert.toLong(m.get("employeeId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return null;
        }
        LocalDate dateStartTime = DateUtil.beginOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
        LocalDate dateEndTime = DateUtil.endOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
        List<String> dates = attendanceClockService.findDates(dateStartTime, dateEndTime);
        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();

        HashMap<String, Object> queryOvertimeParams = new HashMap<>();
        queryOvertimeParams.put("year", year);
        queryOvertimeParams.put("month", month);
        List<HrmProduceAttendance> hasOverTimePayEmpList = produceAttendanceMapper.getOvertimeAllowanceStatistics(queryOvertimeParams);
        HrmSalaryBasic salaryBasic = hrmSalaryBasicService.lambdaQuery().orderByDesc(HrmSalaryBasic::getCreateTime).last("limit 1").one();
        HrmAttendanceRule attendanceRule = hrmAttendanceRuleService.lambdaQuery().orderByDesc(HrmAttendanceRule::getCreateTime).one();
        LocalDateTime startDateTime = dateStartTime.atStartOfDay();
        LocalDateTime endDateTime = LocalDateTimeUtil.endOfDay(dateEndTime.atStartOfDay());
        Date beginDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
        HashMap<String, Object> params = new HashMap<>();
        params.put("beginDate", beginDate);
        params.put("endDate", endDate);
        List<HrmAttendanceSummaryDayVo> attendanceSummaryVoDayList = attendanceReportDataService.getEmpAttendanceSummaryDayList(params);
        Map<Long, List<HrmAttendanceSummaryDayVo>> attendanceSummaryDayMap = attendanceSummaryVoDayList.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.groupingBy(HrmAttendanceSummaryDayVo::getEmployeeId));
        List<HrmAttendanceSummaryVo> attendanceSummaryVoList = attendanceReportDataService.getEmpAttendanceSummaryList(params);
        Map<Long, HrmAttendanceSummaryVo> attendanceSummaryMap = attendanceSummaryVoList.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmAttendanceSummaryVo::getEmployeeId, Function.identity(), (v1, v2) -> v1));
        QuerySalaryArchivesListDto querySalaryArchivesListDto = new QuerySalaryArchivesListDto();
        querySalaryArchivesListDto.setEmployeeIds(employeeIds);
        querySalaryArchivesListDto.setYear(year);
        querySalaryArchivesListDto.setMonth(month);
        List<QuerySalaryArchivesListVO> empSalaryArchivesList = salaryArchivesService.queryEmpSalaryArchivesList(querySalaryArchivesListDto);
        Map<Long, QuerySalaryArchivesListVO> empSalaryArchivesMap = empSalaryArchivesList.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.toMap(QuerySalaryArchivesListVO::getEmployeeId, Function.identity(), (v1, v2) -> v1));
        Map<Integer, Double> normalDaysByDeptType = loadNormalDaysByDeptType(year, month);

        // 批量预加载假期抵扣数据（消除 fillAttendanceDataForEmployee 中的 N+1 查询）
        List<QueryHolidayDeductionVO> allDeductions = holidayDeductionService.queryHolidayDeductionBatch(year, month);
        Map<Long, List<QueryHolidayDeductionVO>> holidayDeductionMap = allDeductions.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.groupingBy(QueryHolidayDeductionVO::getEmployeeId));

        // 批量预加载排班时长数据（消除 getWorkHours 中的 N*days 次查询）
        HashMap<String, Object> shiftBatchParams = new HashMap<>();
        shiftBatchParams.put("beginDate", dateStartTime.toString());
        shiftBatchParams.put("endDate", dateEndTime.toString());
        List<HrmAttendanceShiftVO> allShifts = attendanceShiftMapper.getEmpHrmAttendanceShiftBatch(shiftBatchParams);
        Map<Long, Map<String, Double>> workHoursMap = new HashMap<>();
        if (CollUtil.isNotEmpty(allShifts)) {
            for (HrmAttendanceShiftVO shift : allShifts) {
                if (shift.getEmpId() == null || shift.getAttendanceShiftDate() == null) continue;
                workHoursMap
                        .computeIfAbsent(shift.getEmpId(), k -> new HashMap<>())
                        .put(shift.getAttendanceShiftDate(), (double) shift.getShiftHours());
            }
        }

        // 批量预加载部门名称（消除 fillAttendanceDataForEmployee 末尾的逐条查询）
        List<Long> deptIds = mapList.stream()
                .map(m -> Convert.toLong(m.get("deptId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> deptNameMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(deptIds)) {
            List<SimpleHrmDeptVO> deptList = hrmDeptRepository.findAllByDeptIdIn(deptIds);
            deptNameMap = deptList.stream()
                    .filter(d -> d.getDeptId() != null && d.getDeptName() != null)
                    .collect(Collectors.toMap(SimpleHrmDeptVO::getDeptId, SimpleHrmDeptVO::getDeptName, (v1, v2) -> v1));
        }

        return new AttendanceSyncBatchData(employeeIds, hasOverTimePayEmpList, salaryBasic, attendanceRule,
                beginDate, endDate, normalDaysByDeptType, attendanceSummaryVoDayList, attendanceSummaryDayMap,
                attendanceSummaryVoList, attendanceSummaryMap, empSalaryArchivesList, empSalaryArchivesMap,
                dateStartTime, dateEndTime, daysInMonth, dates,
                holidayDeductionMap, workHoursMap, deptNameMap);
    }

    private Map<Integer, Double> loadNormalDaysByDeptType(int year, int month) {
        Map<Integer, Double> result = new HashMap<>(2);
        result.put(0, queryNormalDaysByDeptType(year, month, 0));
        result.put(1, queryNormalDaysByDeptType(year, month, 1));
        return result;
    }

    private Double queryNormalDaysByDeptType(int year, int month, int deptType) {
        HashMap<String, Object> infoParams = new HashMap<>();
        infoParams.put("year", year);
        infoParams.put("month", month);
        infoParams.put("deptType", deptType);
        List<HrmAttendanceInfo> attendanceInfoList = hrmAttendanceInfoMapper.queryInfo(infoParams);
        HrmAttendanceInfo attendanceInfo = attendanceInfoList.stream().findAny().orElse(null);
        if (attendanceInfo == null || StrUtil.isBlank(attendanceInfo.getActualWorkDay())) {
            return DEFAULT_NORMAL_DAYS;
        }
        return Convert.toDouble(attendanceInfo.getActualWorkDay(), DEFAULT_NORMAL_DAYS);
    }

    private static Map<Long, HrmProduceAttendance> toProduceAttendanceMap(List<HrmProduceAttendance> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmProduceAttendance::getEmployeeId, Function.identity(), (v1, v2) -> v1));
    }

    static List<HrmSalaryOption> filterNoFixedSalaryOptions(List<HrmSalaryOption> source) {
        if (CollUtil.isEmpty(source)) {
            return Collections.emptyList();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getCode() != null && !EXCLUDED_NO_FIXED_CODES.contains(option.getCode()))
                .collect(Collectors.toList());
    }

    static boolean isMidMonthPromotion(LocalDate becomeDate, int year, int month) {
        if (becomeDate == null) {
            return false;
        }
        YearMonth targetMonth = YearMonth.of(year, month);
        if (!targetMonth.equals(YearMonth.from(becomeDate))) {
            return false;
        }
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        return becomeDate.isAfter(monthStart) && (becomeDate.isBefore(monthEnd) || becomeDate.isEqual(monthEnd));
    }

    static Map<Integer, String> calculateMidMonthPromotionSalaryAmounts(Map<Integer, String> probationSalaryMap,
                                                                         Map<Integer, String> officialSalaryMap,
                                                                         HrmProduceAttendance attendance,
                                                                         BigDecimal normalDays) {
        if (attendance == null) {
            return Collections.emptyMap();
        }
        Map<Integer, String> resultMap = new HashMap<>();
        BigDecimal safeNormalDays = normalDays == null || normalDays.compareTo(BigDecimal.ZERO) <= 0
                ? DEFAULT_NORMAL_DAYS_DECIMAL : normalDays;

        BigDecimal probationBaseSalary = parseAmount(probationSalaryMap.get(10101));
        BigDecimal probationPostSalary = parseAmount(probationSalaryMap.get(10102));
        BigDecimal probationDutySalary = parseAmount(probationSalaryMap.get(10103));
        BigDecimal probationTotalSalary = probationBaseSalary.add(probationPostSalary).add(probationDutySalary);

        BigDecimal officialBaseSalary = parseAmount(officialSalaryMap.get(10101));
        BigDecimal officialPostSalary = parseAmount(officialSalaryMap.get(10102));
        BigDecimal officialDutySalary = parseAmount(officialSalaryMap.get(10103));
        BigDecimal officialTotalSalary = officialBaseSalary.add(officialPostSalary).add(officialDutySalary);

        BigDecimal probationShouldPay = BigDecimal.ZERO;
        BigDecimal officialShouldPay = BigDecimal.ZERO;
        BigDecimal totalSubsidies = BigDecimal.ZERO;
        BigDecimal overtimePay = BigDecimal.ZERO;

        if (attendance.getProbationAttendance() != null) {
            probationShouldPay = probationTotalSalary.multiply(attendance.getProbationAttendance())
                    .divide(safeNormalDays, 2, RoundingMode.HALF_UP);
        }
        if (attendance.getPositiveAttendance() != null) {
            officialShouldPay = officialTotalSalary.multiply(attendance.getPositiveAttendance())
                    .divide(safeNormalDays, 2, RoundingMode.HALF_UP);
        }
        totalSubsidies = nullSafe(attendance.getNightSubsidy())
                .add(nullSafe(attendance.getOtherSubsidies()))
                .add(nullSafe(attendance.getHighTemperature()))
                .add(nullSafe(attendance.getLowTemperature()));
        overtimePay = nullSafe(attendance.getWorkOverTime()).multiply(MID_MONTH_OVERTIME_UNIT_PRICE);

        BigDecimal totalShouldPay = probationShouldPay.add(officialShouldPay).add(totalSubsidies).add(overtimePay);
        resultMap.put(10101, officialBaseSalary.stripTrailingZeros().toPlainString());
        resultMap.put(10102, officialPostSalary.stripTrailingZeros().toPlainString());
        resultMap.put(10103, officialDutySalary.stripTrailingZeros().toPlainString());
        resultMap.put(999001, probationShouldPay.toPlainString());
        resultMap.put(999002, officialShouldPay.toPlainString());
        resultMap.put(999003, totalSubsidies.toPlainString());
        resultMap.put(999004, overtimePay.toPlainString());
        resultMap.put(210101, totalShouldPay.toPlainString());
        return resultMap;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal parseAmount(String value) {
        if (StrUtil.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal extractWelfareTaxableIncome(HrmProduceAttendance attendance) {
        if (attendance == null || StrUtil.isBlank(attendance.getWelfare())) {
            return BigDecimal.ZERO;
        }
        return parseAmount(attendance.getWelfare());
    }

    private static BigDecimal amountByCode(Map<Integer, HrmSalaryMonthOptionValue> optionMap, int code) {
        HrmSalaryMonthOptionValue optionValue = optionMap.get(code);
        if (optionValue == null || StrUtil.isBlank(optionValue.getValue())) {
            return BigDecimal.ZERO;
        }
        return parseAmount(optionValue.getValue());
    }

    private static BigDecimal amountFromFinalOptions(List<HrmSalaryMonthOptionValue> options, int code) {
        if (options == null) return BigDecimal.ZERO;
        return options.stream()
                .filter(o -> o.getCode() != null && o.getCode() == code)
                .findFirst()
                .map(o -> parseAmount(o.getValue()))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * 半路转正复算：基于完整工资上下文（含基础项+汇总项）重新计算个税与实发。
     * <p>
     * 解决原 processMidMonthPromotionSalary 中 optionMap 缺少 100101/100102/280/282 导致
     * 扣款项被视为 0 的问题（P0-1），同时复用 calculateMidMonthPromotionSummary 统一税算口径（P0-2），
     * 并同步更新全链路依赖字段 220101/1001/270101~270106（P1-1）。
     *
     * @param allOptions          完整薪资项列表（基础项 + computeSalary 汇总项合并后）
     * @param midMonthSalaryMap   calculateMidMonthPromotionFullSalary 返回的半路转正薪资数据
     * @param lastMonthTaxData    上月个税累计数据
     * @param year                年份
     * @param month               月份
     * @param isDisabled          是否残疾员工（"1"=残疾免税，其他=正常计税）
     * @param skipTaxForRemark    is_remark=2 且年收入<6w 时为 true，跳过个税
     * @param taxSpecialAdditionalDeduction 专项附加扣除合计，null 时按 0 处理
     */
    static void recalculateMidMonthPromotionTaxAndPay(
            List<HrmSalaryMonthOptionValue> allOptions,
            Map<Integer, String> midMonthSalaryMap,
            Map<Integer, String> lastMonthTaxData,
            int year, int month,
            String isDisabled,
            boolean skipTaxForRemark,
            BigDecimal taxSpecialAdditionalDeduction,
            BigDecimal welfareTaxableIncome) {

        if (midMonthSalaryMap == null || midMonthSalaryMap.isEmpty()) {
            return;
        }

        Map<Integer, HrmSalaryMonthOptionValue> optionMap = allOptions.stream()
                .filter(o -> o.getCode() != null)
                .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> a));

        // 1. 覆盖基本工资、岗位工资、职务工资、应发工资、加班费
        upsertOptionValue(allOptions, optionMap, 10101, parseAmount(midMonthSalaryMap.get(10101)));
        upsertOptionValue(allOptions, optionMap, 10102, parseAmount(midMonthSalaryMap.get(10102)));
        upsertOptionValue(allOptions, optionMap, 10103, parseAmount(midMonthSalaryMap.get(10103)));
        upsertOptionValue(allOptions, optionMap, 210101, parseAmount(midMonthSalaryMap.get(210101)));
        upsertOptionValue(allOptions, optionMap, 180101, parseAmount(midMonthSalaryMap.get(999004)));

        // 2. 从完整 optionMap 中读取扣款项（P0-1 修复核心）
        BigDecimal proxyPaySalary = amountByCode(optionMap, 100101).add(amountByCode(optionMap, 100102));
        BigDecimal otherDeductions = amountByCode(optionMap, 280);
        BigDecimal loanMoney = amountByCode(optionMap, 282);
        BigDecimal shouldPaySalary = parseAmount(midMonthSalaryMap.get(210101));

        // 3. 复用统一税算口径（P0-2 修复核心）
        boolean isDisabledNo = !"1".equals(isDisabled);
        Map<Integer, String> summaryMap = calculateMidMonthPromotionSummary(
                shouldPaySalary,
                proxyPaySalary,
                otherDeductions,
                loanMoney,
                BigDecimal.ZERO,           // taxAfterPaySalary
                BigDecimal.ZERO,           // specialTaxSalary
                taxSpecialAdditionalDeduction == null ? BigDecimal.ZERO : taxSpecialAdditionalDeduction,
                BigDecimal.ZERO,           // labourUnionPay（半路转正不计工会费）
                BigDecimal.ZERO,           // bonusSalary
                false,                     // includeBonusInCumulativeIncome
                isDisabledNo,
                skipTaxForRemark,
                lastMonthTaxData,
                month,
                welfareTaxableIncome
        );

        // 4. 同步更新全链路依赖字段（P1-1 修复）
        for (Integer code : Arrays.asList(220101, 230101, 240101, 160102, 1001,
                250101, 250102, 250103, 250105,
                270101, 270102, 270103, 270104, 270105, 270106)) {
            upsertOptionValue(allOptions, optionMap, code, parseAmount(summaryMap.get(code)));
        }
    }

    static Map<Integer, String> calculateMidMonthPromotionSummary(BigDecimal shouldPaySalary,
                                                                  BigDecimal proxyPaySalary,
                                                                  BigDecimal otherDeductions,
                                                                  BigDecimal loanMoney,
                                                                  BigDecimal taxAfterPaySalary,
                                                                  BigDecimal specialTaxSalary,
                                                                  BigDecimal taxSpecialAdditionalDeduction,
                                                                  BigDecimal labourUnionPay,
                                                                  BigDecimal bonusSalary,
                                                                  boolean includeBonusInCumulativeIncome,
                                                                  boolean isDisabledNo,
                                                                  boolean skipTaxForRemark,
                                                                  Map<Integer, String> lastMonthTaxData,
                                                                  int month,
                                                                  BigDecimal welfareTaxableIncome) {
        Map<Integer, String> lastTaxMap = initLastMonthTaxMap(lastMonthTaxData, month);
        BigDecimal safeShouldPaySalary = shouldPaySalary == null ? BigDecimal.ZERO : shouldPaySalary;
        BigDecimal safeProxyPaySalary = proxyPaySalary == null ? BigDecimal.ZERO : proxyPaySalary;
        BigDecimal safeOtherDeductions = otherDeductions == null ? BigDecimal.ZERO : otherDeductions;
        BigDecimal safeLoanMoney = loanMoney == null ? BigDecimal.ZERO : loanMoney;
        BigDecimal safeTaxAfterPaySalary = taxAfterPaySalary == null ? BigDecimal.ZERO : taxAfterPaySalary;
        BigDecimal safeSpecialTaxSalary = specialTaxSalary == null ? BigDecimal.ZERO : specialTaxSalary;
        BigDecimal safeTaxSpecialAdditionalDeduction = taxSpecialAdditionalDeduction == null ? BigDecimal.ZERO : taxSpecialAdditionalDeduction;
        BigDecimal safeLabourUnionPay = labourUnionPay == null ? BigDecimal.ZERO : labourUnionPay;
        BigDecimal safeBonusSalary = bonusSalary == null ? BigDecimal.ZERO : bonusSalary;
        BigDecimal safeWelfareTaxableIncome = welfareTaxableIncome == null ? BigDecimal.ZERO : welfareTaxableIncome;

        BigDecimal baseShouldTaxSalary = safeShouldPaySalary.add(safeSpecialTaxSalary).subtract(safeProxyPaySalary);
        BigDecimal shouldTaxSalary = baseShouldTaxSalary.compareTo(MONTHLY_TAX_FREE_DEDUCTION) > 0
                ? baseShouldTaxSalary.subtract(MONTHLY_TAX_FREE_DEDUCTION)
                : BigDecimal.ZERO;

        BigDecimal cumulativeIncome = parseAmount(lastTaxMap.get(250101)).add(safeShouldPaySalary);
        if (includeBonusInCumulativeIncome) {
            cumulativeIncome = cumulativeIncome.add(safeBonusSalary);
        }
        cumulativeIncome = cumulativeIncome.add(safeWelfareTaxableIncome);
        BigDecimal cumulativeDeductions = parseAmount(lastTaxMap.get(250102)).add(MONTHLY_TAX_FREE_DEDUCTION);
        BigDecimal cumulativeSpecialDeduction = parseAmount(lastTaxMap.get(250103)).add(safeProxyPaySalary);
        BigDecimal cumulativeSpecialAdditionalDeduction = safeTaxSpecialAdditionalDeduction;
        BigDecimal cumulativeTaxableIncome = cumulativeIncome
                .subtract(cumulativeDeductions)
                .subtract(cumulativeSpecialDeduction)
                .subtract(cumulativeSpecialAdditionalDeduction);
        if (cumulativeTaxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            cumulativeTaxableIncome = BigDecimal.ZERO;
        }

        BigDecimal cumulativeTaxPayable = calculateCumulativeTaxPayable(cumulativeTaxableIncome);
        BigDecimal payTaxSalary = BigDecimal.ZERO;
        if (isDisabledNo && !skipTaxForRemark) {
            payTaxSalary = cumulativeTaxPayable.subtract(parseAmount(lastTaxMap.get(250105))).max(BigDecimal.ZERO);
        }

        BigDecimal realPaySalary = safeShouldPaySalary
                .subtract(safeProxyPaySalary)
                .subtract(payTaxSalary)
                .add(safeTaxAfterPaySalary)
                .subtract(safeLabourUnionPay)
                .subtract(safeOtherDeductions)
                .subtract(safeLoanMoney);

        BigDecimal totalDeduction = safeProxyPaySalary
                .add(safeLabourUnionPay)
                .add(safeLoanMoney)
                .add(safeOtherDeductions)
                .add(payTaxSalary);

        Map<Integer, String> result = new HashMap<>();
        result.put(210101, safeShouldPaySalary.toPlainString());
        result.put(220101, shouldTaxSalary.toPlainString());
        result.put(230101, payTaxSalary.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.put(240101, realPaySalary.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.put(160102, safeLabourUnionPay.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.put(1001, totalDeduction.setScale(2, RoundingMode.HALF_UP).toPlainString());

        result.put(250101, lastTaxMap.get(250101));
        result.put(250102, lastTaxMap.get(250102));
        result.put(250103, lastTaxMap.get(250103));
        result.put(250105, lastTaxMap.get(250105));

        result.put(270101, cumulativeIncome.toPlainString());
        result.put(270102, cumulativeDeductions.toPlainString());
        result.put(270103, cumulativeSpecialDeduction.toPlainString());
        result.put(270104, cumulativeSpecialAdditionalDeduction.toPlainString());
        result.put(270105, cumulativeTaxableIncome.toPlainString());
        result.put(270106, cumulativeTaxPayable.setScale(2, RoundingMode.HALF_UP).toPlainString());

        return result;
    }

    private static Map<Integer, String> initLastMonthTaxMap(Map<Integer, String> lastMonthTaxData, int month) {
        Map<Integer, String> lastTaxMap = new HashMap<>();
        lastTaxMap.put(250101, "0");
        lastTaxMap.put(250102, "0");
        lastTaxMap.put(250103, "0");
        lastTaxMap.put(250105, "0");
        if (lastMonthTaxData != null) {
            lastTaxMap.put(250101, String.valueOf(lastMonthTaxData.getOrDefault(250101, "0")));
            lastTaxMap.put(250102, String.valueOf(lastMonthTaxData.getOrDefault(250102, "0")));
            lastTaxMap.put(250103, String.valueOf(lastMonthTaxData.getOrDefault(250103, "0")));
            lastTaxMap.put(250105, String.valueOf(lastMonthTaxData.getOrDefault(250105, "0")));
        }
        if (month == 12) {
            lastTaxMap.put(250101, "0");
            lastTaxMap.put(250102, "0");
            lastTaxMap.put(250103, "0");
            lastTaxMap.put(250105, "0");
        }
        return lastTaxMap;
    }

    private static BigDecimal calculateCumulativeTaxPayable(BigDecimal cumulativeTaxableIncome) {
        return TaxCalculator.calculateCumulativeTax(cumulativeTaxableIncome);
    }

    private static void upsertOptionValue(List<HrmSalaryMonthOptionValue> optionValueList,
                                          Map<Integer, HrmSalaryMonthOptionValue> optionMap,
                                          Integer code,
                                          BigDecimal value) {
        HrmSalaryMonthOptionValue target = optionMap.get(code);
        if (target == null) {
            target = new HrmSalaryMonthOptionValue();
            target.setCode(code);
            optionValueList.add(target);
            optionMap.put(code, target);
        }
        target.setValue(value.toPlainString());
    }

    private static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * （薪资管理）查询薪资列表
     *
     * @param querySalaryPageListDto
     * @return
     */
    public BasePage<QuerySalaryPageListVO> querySalaryPageList(QuerySalaryPageListDto querySalaryPageListDto) {
        List<Long> employeeIds = new ArrayList<>();

        //查询薪资月记录
        HrmSalaryMonthRecord salaryMonthRecord = salaryMonthRecordMapper.querySalaryRecordById(querySalaryPageListDto.getSRecordId());
        //查询出已经定薪了的人员列表
        employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).ne(HrmEmployee::getIsDel, 1).list().stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));

        //排除掉离职等不需要计薪的人员
        List<Long> salaryEmployeeIds = salaryMonthEmpRecordMapper.queryPaySalaryEmployeeIdList(salaryMonthRecord.getEndTime(),employeeIds);

        BasePage<QuerySalaryPageListVO> page = salaryMonthEmpRecordMapper.querySalaryPageList(querySalaryPageListDto.parse(), querySalaryPageListDto,salaryEmployeeIds);
        if (CollectionUtil.isEmpty(page.getList())) {
            return null;
        }
        page.getList().forEach(querySalaryPageListVO -> {
            List<ComputeSalaryDto> list = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(querySalaryPageListVO.getSEmpRecordId());
            List<QuerySalaryPageListVO.SalaryValue> salaryValues = TransferUtil.transferList(list, QuerySalaryPageListVO.SalaryValue.class);
//            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 1, querySalaryPageListVO.getNeedWorkDay().toString(), 1, "计薪天数"));
//            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, querySalaryPageListVO.getActualWorkDay().toString(), 1, "实际计薪天数"));

            //个税显示保留两位小数
            QuerySalaryPageListVO.SalaryValue taxSalaryValue = salaryValues.stream().filter(f -> f.getCode()!=null && f.getCode()==230101).findAny().orElse(null);
            if(taxSalaryValue!=null && StringUtils.isNotBlank(taxSalaryValue.getValue()))
            {
                taxSalaryValue.setValue(new BigDecimal(taxSalaryValue.getValue()).setScale(2,BigDecimal.ROUND_HALF_UP).toString());
            }

            if (querySalaryPageListVO.getNeedWorkDay() != null && querySalaryPageListVO.getActualWorkDay() != null) {
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 1, querySalaryPageListVO.getNeedWorkDay().toString(), 1, "应出勤天数"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, querySalaryPageListVO.getActualWorkDay().toString(), 1, "实际出勤天数"));
            }
            querySalaryPageListVO.setSalary(salaryValues);
        });

        List<Long> sEmpRecordIds = salaryMonthEmpRecordMapper.querysEmpRecordIds(querySalaryPageListDto, salaryEmployeeIds);
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
    /**
     * 获取非固定薪资项（支持半路转正员工分段计算）
     * @param salaryMonthEmpRecord 员工薪资记录
     * @param noFixedSalaryOptionList 非固定薪资项列表
     * @param isNew 是否新记录
     * @return 薪资项值列表
     */
    private List<HrmSalaryMonthOptionValue> getNoFixedOptionValue(HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                                   List<HrmSalaryOption> noFixedSalaryOptionList,
                                                                   boolean isNew,
                                                                   LocalDate becomeDate) {
        List<HrmSalaryOption> filteredNoFixedSalaryOptionList = filterNoFixedSalaryOptions(noFixedSalaryOptionList);
        List<HrmSalaryMonthOptionValue> noFixedOptionValueList = new ArrayList<>();
        if (isNew) {
            //从薪资档案中获取员工对应的工资项和对应的值
            //检查是否为半路转正员工
            Map<Integer, String> optionValueCodeMap = calculateMidMonthPromotionSalary(
                    salaryMonthEmpRecord.getEmployeeId(), 
                    salaryMonthEmpRecord.getYear(), 
                    salaryMonthEmpRecord.getMonth(),
                    filteredNoFixedSalaryOptionList,
                    becomeDate
            );
            
            BigDecimal all = new BigDecimal(0);
            if (CollUtil.isNotEmpty(filteredNoFixedSalaryOptionList)) {
                for (HrmSalaryOption salaryOption : filteredNoFixedSalaryOptionList) {
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
                    filteredNoFixedSalaryOptionList.forEach(salaryOption -> {
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
            filteredNoFixedSalaryOptionList.forEach(salaryOption -> {
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
     * 计算半路转正员工的薪资（不按比例折算）
     * 半路转正：转正日期在计薪月份中间的员工
     * 
     * 业务逻辑：
     * 1. 试用期应发工资 = 基本工资(is_pro=1) + 岗位工资(is_pro=1) + 职务工资(is_pro=1) - 超缺勤扣款
     * 2. 转正后应发工资 = 基本工资(is_pro=0) + 岗位工资(is_pro=0) + 职务工资(is_pro=0)
     *                     + 高温津贴 + 低温津贴 + 夜班补贴 + 其他补贴 + 满勤奖(=0) + 加班工资 - 超缺勤扣款
     * 3. 整月应发工资 = 试用期应发 + 转正后应发
     * 4. 整月实发工资 = 整月应发 - 个税 - 社保 - 公积金 - 其他扣款 - 工会费(=0)
     * 
     * 注意：
     * - 基本工资、岗位工资、职务工资不按比例折算，直接使用月标准
     * - 半路转正员工没有满勤奖和工会费
     * - 个税基于整月应发工资计算
     * 
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @param salaryOptionList 薪资项列表
     * @return 薪资项code与计算后值的映射（返回试用期和转正后的薪资标准，不进行超缺勤计算）
     */
    private Map<Integer, String> calculateMidMonthPromotionSalary(Long employeeId, int year, int month,
                                                                   List<HrmSalaryOption> salaryOptionList,
                                                                   LocalDate becomeDate) {
        if (!isMidMonthPromotion(becomeDate, year, month)) {
            return querySalaryArchivesOptionMap(employeeId, year, month);
        }

        List<HrmSalaryArchivesOption> archivesOptionList = salaryArchivesOptionService.lambdaQuery()
                .eq(HrmSalaryArchivesOption::getEmployeeId, employeeId)
                .in(HrmSalaryArchivesOption::getIsPro, Arrays.asList(0, 1))
                .list();
        Map<Integer, String> probationSalaryMap = archivesOptionList.stream()
                .filter(option -> option.getIsPro() != null && option.getIsPro() == 1)
                .collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue, (v1, v2) -> v1));
        Map<Integer, String> officialSalaryMap = archivesOptionList.stream()
                .filter(option -> option.getIsPro() != null && option.getIsPro() == 0)
                .collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue, (v1, v2) -> v1));

        Map<Integer, String> resultMap = new HashMap<>();
        for (HrmSalaryOption salaryOption : salaryOptionList) {
            Integer code = salaryOption.getCode();
            String value = officialSalaryMap.getOrDefault(code, probationSalaryMap.getOrDefault(code, "0"));
            resultMap.put(code, value);
        }
        return resultMap;
    }

    private Map<Integer, String> querySalaryArchivesOptionMap(Long employeeId, int year, int month) {
        List<HrmSalaryArchivesOption> archivesOptionList = salaryArchivesService.querySalaryArchivesOption(employeeId, year, month);
        if (CollUtil.isEmpty(archivesOptionList)) {
            return Collections.emptyMap();
        }
        return archivesOptionList.stream()
                .collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue, (v1, v2) -> v1));
    }



    /**
     * 核算薪资数据
     * @param sRecordId
     * @param isSyncInsuranceData
     * @param isSyncAttendanceData
     * @param employeeId 员工ID（null 表示全量）
     */
    @Transactional
    public void computeSalaryData(Long sRecordId, Boolean isSyncInsuranceData, Boolean isSyncAttendanceData,
                                  Long employeeId) {
        withSalaryRecordLock(sRecordId, () -> {
            doComputeSalaryData(sRecordId, isSyncInsuranceData, isSyncAttendanceData, employeeId);
            return null;
        });
    }

    private void doComputeSalaryData(Long sRecordId, Boolean isSyncInsuranceData, Boolean isSyncAttendanceData,
                                     Long employeeId) {
        HrmSalaryMonthRecord salaryMonthRecord = getSalaryMonthRecordOrThrow(sRecordId);
        HrmSalaryConfig salaryConfig = getSalaryConfigOrThrow();

        int year = salaryMonthRecord.getYear();
        int month = salaryMonthRecord.getMonth();
        boolean syncInsuranceData = Boolean.TRUE.equals(isSyncInsuranceData);
        boolean syncAttendanceData = Boolean.TRUE.equals(isSyncAttendanceData);
        updateAddition(employeeId, year, month);
        validateInsuranceData(isSyncInsuranceData, salaryConfig, year, month);
        SalaryComputeBatchData batchData = prepareSalaryComputeBatchData(salaryMonthRecord, employeeId,
                year, month, syncAttendanceData, isSyncInsuranceData, salaryConfig);

        Set<String> midMonthPromotionReviewSet = new LinkedHashSet<>();

        SalaryComputeContext ctx = buildSalaryComputeContext(year, month, syncInsuranceData, syncAttendanceData,
                salaryConfig, batchData);
        List<EmployeeSalaryResult> results = computeEmployeeSalaryResults(batchData.employeeMapList, sRecordId,
                salaryMonthRecord, ctx, batchData.hasAttendanceGroupMap, midMonthPromotionReviewSet);

        batchSaveResults(results);
        finishSalaryCompute(year, month, midMonthPromotionReviewSet, salaryMonthRecord);
    }

    private SalaryComputeBatchData prepareSalaryComputeBatchData(HrmSalaryMonthRecord salaryMonthRecord,
                                                                 Long employeeId,
                                                                 int year,
                                                                 int month,
                                                                 boolean syncAttendanceData,
                                                                 Boolean isSyncInsuranceData,
                                                                 HrmSalaryConfig salaryConfig) {
        EmployeeComputeScopeData employeeScopeData = loadEmployeeComputeScopeData(
                salaryMonthRecord, employeeId, year, month, syncAttendanceData);
        SalaryOptionBatchData salaryOptionBatchData = loadSalaryOptionBatchData();
        Map<Long, HrmProduceAttendance> produceAttendanceMap = resolveProduceAttendanceMap(
                employeeScopeData.attendanceSyncBatchData, year, month);
        Map<Integer, Double> normalDaysByDeptType = resolveNormalDaysByDeptType(
                employeeScopeData.attendanceSyncBatchData, year, month);
        HistoryComputeData historyData = loadHistoryComputeData(employeeScopeData.employeeMapList, year, month,
                isSyncInsuranceData, salaryConfig);
        return new SalaryComputeBatchData(employeeScopeData.employeeMapList, employeeScopeData.attendanceDataMap,
                employeeScopeData.hasAttendanceGroupMap,
                salaryOptionBatchData, produceAttendanceMap, normalDaysByDeptType,
                historyData.lastMonthTaxDataMap, historyData.lastYearAccumulatedIncomeMap,
                historyData.socialSecurityEmpRecordMap, historyData.additionalDeductionMap,
                historyData.midMonthArchivesOptionMap);
    }

    private EmployeeComputeScopeData loadEmployeeComputeScopeData(HrmSalaryMonthRecord salaryMonthRecord,
                                                                  Long employeeId,
                                                                  int year,
                                                                  int month,
                                                                  boolean syncAttendanceData) {
        List<Map<String, Object>> employeeMapList = queryHasSalaryArchivesEmployeeList(salaryMonthRecord, employeeId);
        markSalaryRecordAsCreated(salaryMonthRecord, employeeMapList.size());
        Map<String, Map<Integer, String>> attendanceDataMap = resolveAttendanceData(employeeMapList);
        Map<Long, Boolean> hasAttendanceGroupMap = loadHasAttendanceGroupMap(employeeMapList);
        AttendanceSyncBatchData attendanceSyncBatchData = prepareAttendanceDataForCompute(syncAttendanceData, year, month,
                employeeMapList, attendanceDataMap, hasAttendanceGroupMap);
        return new EmployeeComputeScopeData(employeeMapList, attendanceDataMap, hasAttendanceGroupMap, attendanceSyncBatchData);
    }

    private HistoryComputeData loadHistoryComputeData(List<Map<String, Object>> employeeMapList,
                                                      int year,
                                                      int month,
                                                      Boolean isSyncInsuranceData,
                                                      HrmSalaryConfig salaryConfig) {
        Map<Long, Map<Integer, String>> lastMonthTaxDataMap = loadLastMonthTaxDataMap(employeeMapList, year, month);
        Map<Long, BigDecimal> lastYearAccumulatedIncomeMap = loadLastYearAccumulatedIncomeMap(employeeMapList, year);
        Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap = loadSocialSecurityEmpRecordMap(
                employeeMapList, year, month, isSyncInsuranceData, salaryConfig);
        Map<Long, HrmAdditional> additionalDeductionMap = loadAdditionalDeductionMap(employeeMapList, year, month);
        Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap = loadMidMonthArchivesOptionMap(employeeMapList);
        return new HistoryComputeData(lastMonthTaxDataMap, lastYearAccumulatedIncomeMap, socialSecurityEmpRecordMap,
                additionalDeductionMap, midMonthArchivesOptionMap);
    }

    private HrmSalaryMonthRecord getSalaryMonthRecordOrThrow(Long sRecordId) {
        HrmSalaryMonthRecord salaryMonthRecord = getById(sRecordId);
        if (salaryMonthRecord == null) {
            throw new HrmException(6001, "薪资月记录不存在: " + sRecordId);
        }
        return salaryMonthRecord;
    }

    private HrmSalaryConfig getSalaryConfigOrThrow() {
        HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
        if (salaryConfig == null) {
            throw new HrmException(HrmCodeEnum.NO_INITIAL_CONFIGURATION.getCode(), "薪资配置不存在");
        }
        return salaryConfig;
    }

    private void markSalaryRecordAsCreated(HrmSalaryMonthRecord salaryMonthRecord, int employeeCount) {
        salaryMonthRecord.setNum(employeeCount);
        salaryMonthRecord.setCheckStatus(SalaryRecordStatus.CREATED.getValue());
    }

    private Map<Long, HrmProduceAttendance> resolveProduceAttendanceMap(AttendanceSyncBatchData attendanceSyncBatchData,
                                                                        int year,
                                                                        int month) {
        if (attendanceSyncBatchData != null) {
            return attendanceSyncBatchData.overTimePayEmpMap;
        }
        return loadProduceAttendanceMap(year, month);
    }

    private Map<Integer, Double> resolveNormalDaysByDeptType(AttendanceSyncBatchData attendanceSyncBatchData,
                                                              int year,
                                                              int month) {
        if (attendanceSyncBatchData != null) {
            return attendanceSyncBatchData.normalDaysByDeptType;
        }
        return loadNormalDaysByDeptType(year, month);
    }

    private SalaryComputeContext buildSalaryComputeContext(int year,
                                                           int month,
                                                           boolean syncInsuranceData,
                                                           boolean syncAttendanceData,
                                                           HrmSalaryConfig salaryConfig,
                                                           SalaryComputeBatchData batchData) {
        return SalaryComputeContext.builder()
                .year(year).month(month)
                .isSyncInsuranceData(syncInsuranceData)
                .isSyncAttendanceData(syncAttendanceData)
                .salaryConfig(salaryConfig)
                .attendanceDataMap(batchData.attendanceDataMap)
                .noFixedSalaryOptionList(batchData.salaryOptionBatchData.noFixedSalaryOptionList)
                .produceAttendanceMap(batchData.produceAttendanceMap)
                .normalDaysByDeptType(batchData.normalDaysByDeptType)
                .optionParentCodeMap(batchData.salaryOptionBatchData.optionParentCodeMap)
                .salaryOptionConfigMap(batchData.salaryOptionBatchData.salaryOptionConfigMap)
                .lastMonthTaxDataMap(batchData.lastMonthTaxDataMap)
                .lastYearAccumulatedIncomeMap(batchData.lastYearAccumulatedIncomeMap)
                .socialSecurityEmpRecordMap(batchData.socialSecurityEmpRecordMap)
                .additionalDeductionMap(batchData.additionalDeductionMap)
                .midMonthArchivesOptionMap(batchData.midMonthArchivesOptionMap)
                .hasAttendanceGroupMap(batchData.hasAttendanceGroupMap)
                .build();
    }

    private List<EmployeeSalaryResult> computeEmployeeSalaryResults(List<Map<String, Object>> employeeMapList,
                                                                    Long sRecordId,
                                                                    HrmSalaryMonthRecord salaryMonthRecord,
                                                                    SalaryComputeContext ctx,
                                                                    Map<Long, Boolean> hasAttendanceGroupMap,
                                                                    Set<String> midMonthPromotionReviewSet) {
        List<EmployeeSalaryResult> results = new ArrayList<>(employeeMapList.size());
        for (Map<String, Object> map : employeeMapList) {
            EmployeeSalaryResult result = computeSingleEmployeeSalaryResult(
                    map, sRecordId, salaryMonthRecord, ctx, hasAttendanceGroupMap, midMonthPromotionReviewSet);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    private EmployeeSalaryResult computeSingleEmployeeSalaryResult(Map<String, Object> employeeMap,
                                                                   Long sRecordId,
                                                                   HrmSalaryMonthRecord salaryMonthRecord,
                                                                   SalaryComputeContext ctx,
                                                                   Map<Long, Boolean> hasAttendanceGroupMap,
                                                                   Set<String> midMonthPromotionReviewSet) {
        Long currentEmployeeId = Convert.toLong(employeeMap.get("employeeId"));
        boolean hasAttendanceGroup = isEmployeeInAttendanceGroup(currentEmployeeId, hasAttendanceGroupMap);
        return computeEmployeeSalary(employeeMap, sRecordId, salaryMonthRecord, ctx,
                hasAttendanceGroup, midMonthPromotionReviewSet);
    }

    private void finishSalaryCompute(int year,
                                     int month,
                                     Set<String> midMonthPromotionReviewSet,
                                     HrmSalaryMonthRecord salaryMonthRecord) {
        logMidMonthPromotionReviewWarning(year, month, midMonthPromotionReviewSet);
        updateSalaryMonthRecordAfterCompute(salaryMonthRecord);
    }

    private void logMidMonthPromotionReviewWarning(int year, int month, Set<String> midMonthPromotionReviewSet) {
        if (CollUtil.isEmpty(midMonthPromotionReviewSet)) {
            return;
        }
        logger.warn("计薪月{}-{}发现{}名半路转正员工缺少分段考勤数据，请人工复核：{}",
                year, month, midMonthPromotionReviewSet.size(), String.join(" | ", midMonthPromotionReviewSet));
    }

    private void updateSalaryMonthRecordAfterCompute(HrmSalaryMonthRecord salaryMonthRecord) {
        Map<String, Object> countMap = salaryMonthRecordMapper.queryMonthSalaryCount(salaryMonthRecord.getSRecordId());
        BeanUtil.fillBeanWithMap(countMap, salaryMonthRecord, true);
        salaryMonthRecord.setCheckStatus(SalaryRecordStatus.COMPUTE.getValue());
        updateById(salaryMonthRecord);
    }

    private AttendanceSyncBatchData prepareAttendanceDataForCompute(boolean syncAttendanceData,
                                                                    int year,
                                                                    int month,
                                                                    List<Map<String, Object>> employeeMapList,
                                                                    Map<String, Map<Integer, String>> attendanceDataMap,
                                                                    Map<Long, Boolean> hasAttendanceGroupMap) {
        if (!syncAttendanceData) {
            return null;
        }
        attendanceDataMap.clear();
        AttendanceSyncBatchData batchData = loadAttendanceSyncBatchData(year, month, employeeMapList);
        if (batchData == null) {
            return null;
        }
        for (Map<String, Object> map : employeeMapList) {
            Long currentEmployeeId = Convert.toLong(map.get("employeeId"));
            boolean hasAttendanceGroup = isEmployeeInAttendanceGroup(currentEmployeeId, hasAttendanceGroupMap);
            fillAttendanceDataForEmployee(map, batchData, attendanceDataMap, year, month, hasAttendanceGroup);
        }
        return batchData;
    }

    private SalaryOptionBatchData loadSalaryOptionBatchData() {
        List<HrmSalaryOption> salaryOptionList = hrmSalaryOptionService.lambdaQuery()
                .ne(HrmSalaryOption::getParentCode, 0)
                .list();
        Map<Integer, Integer> optionParentCodeMap = salaryOptionList.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getCode() != null && option.getParentCode() != null)
                .collect(Collectors.toMap(HrmSalaryOption::getCode, HrmSalaryOption::getParentCode, (v1, v2) -> v1));
        Map<Integer, HrmSalaryOption> salaryOptionConfigMap = salaryOptionList.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getCode() != null)
                .collect(Collectors.toMap(HrmSalaryOption::getCode, Function.identity(), (a, b) -> a));
        List<HrmSalaryOption> noFixedSalaryOptionList = filterNoFixedSalaryOptions(
                salaryOptionList.stream()
                        .filter(Objects::nonNull)
                        .filter(option -> Objects.equals(option.getIsFixed(), ZERO))
                        .collect(Collectors.toList()));
        return new SalaryOptionBatchData(optionParentCodeMap, salaryOptionConfigMap, noFixedSalaryOptionList);
    }

    private boolean isEmployeeInAttendanceGroup(Long employeeId, Map<Long, Boolean> hasAttendanceGroupMap) {
        return employeeId != null && Boolean.TRUE.equals(hasAttendanceGroupMap.get(employeeId));
    }

    private <T> T withSalaryRecordLock(Long sRecordId, java.util.concurrent.Callable<T> callable) {
        if (sRecordId == null) {
            throw new HrmException(6001, "薪资月记录不存在");
        }
        ReentrantLock lock = COMPUTE_RECORD_LOCK_MAP.computeIfAbsent(sRecordId, key -> new ReentrantLock());
        lock.lock();
        try {
            return callable.call();
        } catch (HrmException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                COMPUTE_RECORD_LOCK_MAP.remove(sRecordId, lock);
            }
        }
    }


    private Map<Long, Boolean> loadHasAttendanceGroupMap(List<Map<String, Object>> mapList) {
        if (CollUtil.isEmpty(mapList)) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = mapList.stream()
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        Set<Long> employeeIdsInAttendanceGroup = attendanceGroupService
                .queryEmployeeIdsInAttendanceGroupDingDing(employeeIds);
        Map<Long, Boolean> result = new HashMap<>(employeeIds.size());
        for (Long employeeId : employeeIds) {
            result.put(employeeId, employeeIdsInAttendanceGroup.contains(employeeId));
        }
        return result;
    }

    private Map<Long, HrmInsuranceMonthEmpRecord> loadSocialSecurityEmpRecordMap(List<Map<String, Object>> employeeMapList,
                                                                                   int year,
                                                                                   int month,
                                                                                   Boolean isSyncInsuranceData,
                                                                                   HrmSalaryConfig salaryConfig) {
        if (!Boolean.TRUE.equals(isSyncInsuranceData) || CollUtil.isEmpty(employeeMapList)) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = employeeMapList.stream()
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        Integer socialSecurityMonthType = salaryConfig != null ? salaryConfig.getSocialSecurityMonthType() : null;
        if (socialSecurityMonthType == null) {
            socialSecurityMonthType = ONE;
            logger.warn("薪资配置 socialSecurityMonthType 为空，社保按当月口径批量加载");
        }
        YearMonth socialSecurityYearMonth = resolveSocialSecurityReferenceYearMonth(socialSecurityMonthType, year, month);
        List<HrmInsuranceMonthEmpRecord> insuranceMonthEmpRecordList = insuranceMonthEmpRecordService.lambdaQuery()
                .eq(HrmInsuranceMonthEmpRecord::getYear, socialSecurityYearMonth.getYear())
                .eq(HrmInsuranceMonthEmpRecord::getMonth, socialSecurityYearMonth.getMonthValue())
                .eq(HrmInsuranceMonthEmpRecord::getStatus, IsEnum.YES.getValue())
                .in(HrmInsuranceMonthEmpRecord::getEmployeeId, employeeIds)
                .list();
        if (CollUtil.isEmpty(insuranceMonthEmpRecordList)) {
            return Collections.emptyMap();
        }
        return insuranceMonthEmpRecordList.stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmInsuranceMonthEmpRecord::getEmployeeId, Function.identity(), (v1, v2) -> v1));
    }

    static YearMonth resolveSocialSecurityReferenceYearMonth(Integer socialSecurityMonthType, int year, int month) {
        YearMonth currentYearMonth = YearMonth.of(year, month);
        if (socialSecurityMonthType == null) {
            return currentYearMonth;
        }
        if (socialSecurityMonthType == ZERO) {
            return currentYearMonth.minusMonths(1);
        }
        if (socialSecurityMonthType == TWO) {
            return currentYearMonth.plusMonths(1);
        }
        return currentYearMonth;
    }

    private Map<Long, HrmProduceAttendance> loadProduceAttendanceMap(int year, int month) {
        HashMap<String, Object> queryOvertimeParams = new HashMap<>();
        queryOvertimeParams.put("year", year);
        queryOvertimeParams.put("month", month);
        List<HrmProduceAttendance> attendanceList = produceAttendanceMapper.getOvertimeAllowanceStatistics(queryOvertimeParams);
        return toProduceAttendanceMap(attendanceList);
    }
    /**
     * 纯计算单员工薪资，不持久化。从 SalaryComputeContext 读取批量数据。
     * 返回 EmployeeSalaryResult 供外层统一批量保存。
     */
    private EmployeeSalaryResult computeEmployeeSalary(Map<String, Object> map, Long sRecordId,
                                                        HrmSalaryMonthRecord salaryMonthRecord,
                                                        SalaryComputeContext ctx,
                                                        boolean hasAttendanceGroup,
                                                        Set<String> midMonthPromotionReviewSet) {
        Long employeeId = Convert.toLong(map.get("employeeId"));
        if (employeeId == null) return null;
        int year = ctx.getYear();
        int month = ctx.getMonth();
        LocalDate becomeDate = toLocalDate((Date) map.get("becomeTime"));
        HrmEmployeeVO employeeVO = new HrmEmployeeVO();
        employeeVO.setEmployeeId(employeeId);
        employeeVO.setStatus((Integer) map.get("status"));
        employeeVO.setJobNumber((String) map.get("jobNumber"));
        employeeVO.setEmployeeName(map.get("employeeName") != null ? map.get("employeeName").toString() : "");
        String jobNumber = employeeVO.getJobNumber();
        int deptType = getDeptTypeForEmployee(map);
        BigDecimal normalDays = BigDecimal.valueOf(ctx.getNormalDaysByDeptType().getOrDefault(deptType, DEFAULT_NORMAL_DAYS));
        HrmProduceAttendance midMonthAttendance = ctx.getProduceAttendanceMap().get(employeeId);
        BigDecimal welfareTaxableIncome = extractWelfareTaxableIncome(midMonthAttendance);
        collectMidMonthPromotionAttendanceReview(map, year, month, becomeDate, midMonthAttendance, midMonthPromotionReviewSet);
        boolean isJoinAttendance = hasAttendanceGroup
                || ENTRY_STATUS_QUIT.equals(String.valueOf(map.get("entryStatus") != null ? map.get("entryStatus") : ""));

        EmpRecordWithOptions ro = getOrCreateRecordAndApplyAttendance(employeeId, jobNumber, sRecordId, salaryMonthRecord,
                year, month, ctx.getAttendanceDataMap(), ctx.getNoFixedSalaryOptionList(), isJoinAttendance,
                ctx.getIsSyncAttendanceData(), becomeDate);

        List<HrmSalaryMonthOptionValue> options = ro.optionValueList;
        options.addAll(getSocialSecurityOption(ro.record, ctx.getIsSyncInsuranceData(), ctx.getSalaryConfig(),
                ctx.getSocialSecurityEmpRecordMap()));
        addAdditionalDeductionOptions(employeeId, ro.record, options, ctx.getAdditionalDeductionMap());
        // 半路转正员工：移除全勤奖和工会费（在计算个税前）
        removeFullAttendanceAndUnionFeeForMidMonthPromotion(year, month, options, becomeDate);

        Map<Integer, String> baseOptionMap = options.stream()
                .filter(option -> option.getCode() != null)
                .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode,
                        option -> StrUtil.blankToDefault(option.getValue(), "0"),
                        (v1, v2) -> v1));

        Map<Integer, String> lastMonthTaxData = ctx.getLastMonthTaxDataMap() != null
                ? ctx.getLastMonthTaxDataMap().get(employeeId) : null;
        String isDisabled = map.get("isDisabled") != null ? String.valueOf(map.get("isDisabled")) : IS_DISABLED_NO;
        List<HrmSalaryMonthOptionValue> finalOptions = computeSalaryFromMemory(
                ro.record, options, lastMonthTaxData, employeeVO, isDisabled, welfareTaxableIncome, ctx);

        BigDecimal taxSpecialAdditionalDeduction = parseAmount(baseOptionMap.get(260101))
                .add(parseAmount(baseOptionMap.get(260102)))
                .add(parseAmount(baseOptionMap.get(260103)))
                .add(parseAmount(baseOptionMap.get(260104)))
                .add(parseAmount(baseOptionMap.get(260105)))
                .add(parseAmount(baseOptionMap.get(260106)));

        BigDecimal shouldPayForRemark = amountFromFinalOptions(finalOptions, 210101);
        BigDecimal bonusSalaryForRemark = amountFromFinalOptions(finalOptions, 41001);
        LoginUserInfo loginUserInfo = CompanyContext.get();
        boolean includeBonusInCumulativeIncome = loginUserInfo != null && !"0002".equals(loginUserInfo.getCompanyId());
        BigDecimal cumulativeIncomeForRemark = parseAmount(lastMonthTaxData != null ? lastMonthTaxData.get(250101) : "0")
                .add(shouldPayForRemark)
                .add(includeBonusInCumulativeIncome ? bonusSalaryForRemark : BigDecimal.ZERO)
                .add(welfareTaxableIncome);
        boolean skipTaxForRemark = shouldSkipTaxForRemark(map, employeeId, year, cumulativeIncomeForRemark,
                ctx.getLastYearAccumulatedIncomeMap());

        // 半路转正：按日比例拆分薪资 + 移除全勤奖和工会费 + 一致性校验
        if (isMidMonthPromotion(becomeDate, year, month)) {
            processMidMonthPromotionSalary(employeeId, year, month, finalOptions, deptType,
                    becomeDate, midMonthAttendance, normalDays, lastMonthTaxData,
                    baseOptionMap, isDisabled, skipTaxForRemark, taxSpecialAdditionalDeduction,
                    welfareTaxableIncome,
                    ctx.getMidMonthArchivesOptionMap());
            // 第二次移除：processMidMonthPromotionSalary 可能重新写入全勤奖/工会费到 finalOptions，需再次清理
            removeFullAttendanceAndUnionFeeForMidMonthPromotion(year, month, finalOptions, becomeDate);
            applyMidMonthPromotionSummaryConsistency(map, ro.record, finalOptions, baseOptionMap, lastMonthTaxData,
                    isDisabled, year, month, becomeDate, welfareTaxableIncome, ctx.getOptionParentCodeMap(),
                    ctx.getLastYearAccumulatedIncomeMap());
        }

        return new EmployeeSalaryResult(ro.record, options, finalOptions, ro.existed);
    }

    /**
     * 统一批量持久化所有员工的计算结果。
     */
    private void batchSaveResults(List<EmployeeSalaryResult> results) {
        if (CollUtil.isEmpty(results)) return;
        List<HrmSalaryMonthOptionValue> allBaseOptions = new ArrayList<>();
        List<HrmSalaryMonthOptionValue> allFinalOptions = new ArrayList<>();
        List<HrmSalaryMonthEmpRecord> recordsToUpdate = new ArrayList<>();
        for (EmployeeSalaryResult r : results) {
            if (r.getBaseOptions() != null) allBaseOptions.addAll(r.getBaseOptions());
            if (r.getFinalOptions() != null) allFinalOptions.addAll(r.getFinalOptions());
            // 新记录已在 getOrCreateRecordAndApplyAttendance 中 save（需要ID），
            // 已存在的记录需要 updateById 更新考勤等字段
            if (r.isExisted()) {
                recordsToUpdate.add(r.getEmpRecord());
            }
        }
        if (CollUtil.isNotEmpty(allBaseOptions)) {
            salaryMonthOptionValueService.saveBatch(allBaseOptions);
        }
        if (CollUtil.isNotEmpty(allFinalOptions)) {
            salaryMonthOptionValueService.saveBatch(allFinalOptions);
        }
        if (CollUtil.isNotEmpty(recordsToUpdate)) {
            salaryMonthEmpRecordService.updateBatchById(recordsToUpdate);
        }
    }

    /**
     * 基于内存中的工资项列表计算个税和实发工资，不依赖DB中间状态。
     * 替代原来的 saveBatch(options) → computeSalary → baseComputeSalary(从DB读) 流程。
     */
    private List<HrmSalaryMonthOptionValue> computeSalaryFromMemory(
            HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
            List<HrmSalaryMonthOptionValue> optionValueList,
            Map<Integer, String> cumulativeTaxOfLastMonthData,
            HrmEmployeeVO hrmEmployeeVO,
            String isDisabled,
            BigDecimal welfareTaxableIncome,
            SalaryComputeContext ctx) {
        List<ComputeSalaryDto> dtoList = convertToComputeSalaryDtoList(optionValueList, ctx.getSalaryOptionConfigMap());
        LoginUserInfo info = CompanyContext.get();
        String companyId = info != null ? info.getCompanyId() : null;
        SalaryBaseTotal salaryBaseTotal = SalaryComputeServiceNew.baseComputeSalaryFromMemory(dtoList, companyId);

        HrmSalaryTaxRule hrmSalaryTaxRule = new HrmSalaryTaxRule();
        hrmSalaryTaxRule.setIsTax(1);
        hrmSalaryTaxRule.setCycleType(1);
        hrmSalaryTaxRule.setMarkingPoint(5000);
        hrmSalaryTaxRule.setTaxType(1);

        return salaryComputeService.computeSalary(
                salaryBaseTotal, salaryMonthEmpRecord, hrmSalaryTaxRule,
                cumulativeTaxOfLastMonthData, hrmEmployeeVO, isDisabled,
                welfareTaxableIncome == null ? BigDecimal.ZERO : welfareTaxableIncome);
    }

    /**
     * 将内存中的 HrmSalaryMonthOptionValue 列表转换为 ComputeSalaryDto 列表，
     * 使用预加载的 salaryOptionConfigMap 获取 parentCode/isPlus/isTax，避免额外DB查询。
     */
    private static List<ComputeSalaryDto> convertToComputeSalaryDtoList(
            List<HrmSalaryMonthOptionValue> optionValueList,
            Map<Integer, HrmSalaryOption> optionConfigMap) {
        if (CollUtil.isEmpty(optionValueList)) {
            return Collections.emptyList();
        }
        List<ComputeSalaryDto> result = new ArrayList<>(optionValueList.size());
        for (HrmSalaryMonthOptionValue ov : optionValueList) {
            if (ov == null || ov.getCode() == null) continue;
            HrmSalaryOption config = optionConfigMap.get(ov.getCode());
            ComputeSalaryDto dto = new ComputeSalaryDto();
            dto.setCode(ov.getCode());
            dto.setValue(ov.getValue() != null ? ov.getValue() : "0");
            if (config != null) {
                dto.setParentCode(config.getParentCode());
                dto.setIsPlus(config.getIsPlus());
                dto.setIsTax(config.getIsTax());
            } else {
                // 未找到配置时，默认加项、不参与计税（parentCode=0不在shouldPayCodeList中，不影响应发工资）
                logger.warn("工资项配置缺失, code={}", ov.getCode());
                dto.setParentCode(0);
                dto.setIsPlus(IsEnum.YES.getValue());
                dto.setIsTax(IsEnum.NO.getValue());
            }
            result.add(dto);
        }
        return result;
    }

    private Map<Long, Map<Integer, String>> loadLastMonthTaxDataMap(List<Map<String, Object>> employeeMapList,
                                                                     int year,
                                                                     int month) {
        if (CollUtil.isEmpty(employeeMapList)) {
            return Collections.emptyMap();
        }
        int targetYear = year;
        int targetMonth = month - 1;
        if (targetMonth <= 0) {
            // 跨年：1月份需要查上一年12月的累计个税数据
            targetMonth = 12;
            targetYear = year - 1;
        }
        List<Long> employeeIds = employeeMapList.stream()
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        List<QueryPersonalIncomeTaxVO> taxDataList = incomeTaxMapper
                .queryPersonalIncomeTaxByEmployeeIds(employeeIds, targetYear, targetMonth);
        if (CollUtil.isEmpty(taxDataList)) {
            return Collections.emptyMap();
        }
        Map<Long, Map<Integer, String>> result = new HashMap<>();
        for (QueryPersonalIncomeTaxVO taxData : taxDataList) {
            if (taxData == null || taxData.getEmployeeId() == null) {
                continue;
            }
            Map<Integer, String> dataMap = new HashMap<>();
            dataMap.put(250101, taxData.getAccumulatedIncome() == null ? "0" : taxData.getAccumulatedIncome().toString());
            dataMap.put(250102, taxData.getAccumulatedDeductionOfExpenses() == null ? "0" : taxData.getAccumulatedDeductionOfExpenses().toString());
            dataMap.put(250103, taxData.getAccumulatedProvidentFund() == null ? "0" : taxData.getAccumulatedProvidentFund().toString());
            dataMap.put(250105, taxData.getAccumulatedTaxPayment() == null ? "0" : taxData.getAccumulatedTaxPayment().toString());
            result.put(taxData.getEmployeeId(), dataMap);
        }
        return result;
    }

    private Map<Long, BigDecimal> loadLastYearAccumulatedIncomeMap(List<Map<String, Object>> employeeMapList,
                                                                    int year) {
        if (CollUtil.isEmpty(employeeMapList)) {
            return Collections.emptyMap();
        }
        int targetYear = year - 1;
        if (targetYear <= 0) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = employeeMapList.stream()
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        List<QueryPersonalIncomeTaxVO> taxDataList = incomeTaxMapper
                .queryAccumulatedIncomeByEmployeeIdsAndYear(employeeIds, targetYear);
        if (CollUtil.isEmpty(taxDataList)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (QueryPersonalIncomeTaxVO taxData : taxDataList) {
            if (taxData == null || taxData.getEmployeeId() == null) {
                continue;
            }
            result.put(taxData.getEmployeeId(),
                    taxData.getAccumulatedIncome() == null ? BigDecimal.ZERO : taxData.getAccumulatedIncome());
        }
        return result;
    }

    private Map<Long, HrmAdditional> loadAdditionalDeductionMap(List<Map<String, Object>> employeeMapList,
                                                                 int year,
                                                                 int month) {
        if (CollUtil.isEmpty(employeeMapList)) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = employeeMapList.stream()
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        List<HrmAdditional> additionalList = hrmAdditionalService.lambdaQuery()
                .eq(HrmAdditional::getYear, year)
                .eq(HrmAdditional::getMonth, month)
                .in(HrmAdditional::getEmployeeId, employeeIds)
                .list();
        if (CollUtil.isEmpty(additionalList)) {
            return Collections.emptyMap();
        }
        return additionalList.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmAdditional::getEmployeeId, Function.identity(), (v1, v2) -> v1));
    }

    private Map<Long, List<HrmSalaryArchivesOption>> loadMidMonthArchivesOptionMap(List<Map<String, Object>> employeeMapList) {
        if (CollUtil.isEmpty(employeeMapList)) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = employeeMapList.stream()
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        List<HrmSalaryArchivesOption> archivesOptionList = salaryArchivesOptionService.lambdaQuery()
                .in(HrmSalaryArchivesOption::getEmployeeId, employeeIds)
                .in(HrmSalaryArchivesOption::getCode, Arrays.asList(10101, 10102, 10103))
                .in(HrmSalaryArchivesOption::getIsPro, Arrays.asList(0, 1))
                .list();
        if (CollUtil.isEmpty(archivesOptionList)) {
            return Collections.emptyMap();
        }
        return archivesOptionList.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getEmployeeId() != null)
                .collect(Collectors.groupingBy(HrmSalaryArchivesOption::getEmployeeId));
    }

    private void collectMidMonthPromotionAttendanceReview(Map<String, Object> employeeMap,
                                                          int year,
                                                          int month,
                                                          LocalDate becomeDate,
                                                          HrmProduceAttendance midMonthAttendance,
                                                          Set<String> midMonthPromotionReviewSet) {
        if (midMonthPromotionReviewSet == null || !isMidMonthPromotion(becomeDate, year, month)) {
            return;
        }
        boolean missingSplitAttendance = midMonthAttendance == null
                || midMonthAttendance.getProbationAttendance() == null
                || midMonthAttendance.getPositiveAttendance() == null;
        if (!missingSplitAttendance) {
            return;
        }
        Long employeeId = employeeMap != null ? Convert.toLong(employeeMap.get("employeeId")) : null;
        String employeeName = employeeMap != null && employeeMap.get("employeeName") != null
                ? String.valueOf(employeeMap.get("employeeName")) : "";
        String jobNumber = employeeMap != null && employeeMap.get("jobNumber") != null
                ? String.valueOf(employeeMap.get("jobNumber")) : "";
        String reviewItem = "employeeId=" + (employeeId == null ? "" : employeeId)
                + ",name=" + employeeName
                + ",jobNumber=" + jobNumber;
        if (midMonthPromotionReviewSet.add(reviewItem)) {
            logger.warn("半路转正员工缺少分段考勤数据，已加入复核名单：{}，月份={}-{}",
                    reviewItem, year, month);
        }
    }

    private void applyMidMonthPromotionSummaryConsistency(Map<String, Object> employeeMap,
                                                          HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                          List<HrmSalaryMonthOptionValue> finalOptions,
                                                          Map<Integer, String> baseOptionMap,
                                                          Map<Integer, String> lastMonthTaxData,
                                                          String isDisabled,
                                                          int year,
                                                          int month,
                                                          LocalDate becomeDate,
                                                          BigDecimal welfareTaxableIncome,
                                                          Map<Integer, Integer> optionParentCodeMap,
                                                          Map<Long, BigDecimal> lastYearAccumulatedIncomeMap) {
        if (!isMidMonthPromotion(becomeDate, year, month) || CollUtil.isEmpty(finalOptions)) {
            return;
        }
        if (baseOptionMap == null) {
            baseOptionMap = Collections.emptyMap();
        }
        Map<Integer, HrmSalaryMonthOptionValue> finalOptionMap = finalOptions.stream()
                .filter(option -> option.getCode() != null)
                .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (v1, v2) -> v1));

        BigDecimal shouldPaySalary = amountByCode(finalOptionMap, 210101);
        BigDecimal proxyPaySalary = parseAmount(baseOptionMap.get(100101)).add(parseAmount(baseOptionMap.get(100102)));
        BigDecimal otherDeductions = parseAmount(baseOptionMap.get(280));
        BigDecimal loanMoney = parseAmount(baseOptionMap.get(282));
        BigDecimal taxAfterPaySalary = sumAmountsByParentCode(baseOptionMap, optionParentCodeMap, 150);
        BigDecimal specialTaxSalary = sumAmountsByParentCode(baseOptionMap, optionParentCodeMap, 170);
        BigDecimal additionalDeduction = parseAmount(baseOptionMap.get(260101))
                .add(parseAmount(baseOptionMap.get(260102)))
                .add(parseAmount(baseOptionMap.get(260103)))
                .add(parseAmount(baseOptionMap.get(260104)))
                .add(parseAmount(baseOptionMap.get(260105)))
                .add(parseAmount(baseOptionMap.get(260106)));
        BigDecimal bonusSalary = amountByCode(finalOptionMap, 41001);
        LoginUserInfo info = CompanyContext.get();
        boolean includeBonusInCumulativeIncome = info != null && !"0002".equals(info.getCompanyId());
        BigDecimal cumulativeIncomeForRemark = parseAmount(lastMonthTaxData != null ? lastMonthTaxData.get(250101) : "0")
                .add(shouldPaySalary)
                .add(includeBonusInCumulativeIncome ? bonusSalary : BigDecimal.ZERO)
                .add(welfareTaxableIncome == null ? BigDecimal.ZERO : welfareTaxableIncome);

        boolean skipTaxForRemark = shouldSkipTaxForRemark(employeeMap, salaryMonthEmpRecord.getEmployeeId(),
                salaryMonthEmpRecord.getYear(), cumulativeIncomeForRemark, lastYearAccumulatedIncomeMap);

        Map<Integer, String> summaryMap = calculateMidMonthPromotionSummary(
                shouldPaySalary,
                proxyPaySalary,
                otherDeductions,
                loanMoney,
                taxAfterPaySalary,
                specialTaxSalary,
                additionalDeduction,
                BigDecimal.ZERO,
                bonusSalary,
                includeBonusInCumulativeIncome,
                IS_DISABLED_NO.equals(isDisabled),
                skipTaxForRemark,
                lastMonthTaxData,
                month,
                welfareTaxableIncome
        );

        for (Integer code : Arrays.asList(220101, 230101, 240101, 160102, 1001,
                250101, 250102, 250103, 250105,
                270101, 270102, 270103, 270104, 270105, 270106)) {
            upsertOptionValue(finalOptions, finalOptionMap, code, parseAmount(summaryMap.get(code)));
        }
    }

    private BigDecimal sumAmountsByParentCode(Map<Integer, String> optionValueMap,
                                              Map<Integer, Integer> optionParentCodeMap,
                                              int parentCode) {
        if (CollUtil.isEmpty(optionValueMap) || CollUtil.isEmpty(optionParentCodeMap)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<Integer, String> entry : optionValueMap.entrySet()) {
            Integer code = entry.getKey();
            Integer currentParentCode = optionParentCodeMap.get(code);
            if (currentParentCode == null || currentParentCode != parentCode) {
                continue;
            }
            sum = sum.add(parseAmount(entry.getValue()));
        }
        return sum;
    }

    private boolean shouldSkipTaxForRemark(Map<String, Object> employeeMap,
                                           Long employeeId,
                                           int year,
                                           BigDecimal currentCumulativeIncome,
                                           Map<Long, BigDecimal> lastYearAccumulatedIncomeMap) {
        if (employeeId == null) {
            return false;
        }
        // isRemark 已在 queryHasSalaryArchivesEmployeeList 中批量预加载到 employeeMap，无需逐条查询DB
        Integer isRemark = null;
        if (employeeMap != null && employeeMap.get("isRemark") != null) {
            isRemark = Convert.toInt(employeeMap.get("isRemark"), null);
        }
        if (isRemark == null || isRemark != 2) {
            return false;
        }
        BigDecimal lastYearAccumulated = lastYearAccumulatedIncomeMap != null
                ? lastYearAccumulatedIncomeMap.getOrDefault(employeeId, BigDecimal.ZERO)
                : BigDecimal.ZERO;
        BigDecimal currentYearAccumulated = currentCumulativeIncome == null ? BigDecimal.ZERO : currentCumulativeIncome;
        return lastYearAccumulated.add(currentYearAccumulated).compareTo(new BigDecimal("60000")) < 0;
    }

    /** 获取或创建当月员工记录并合并考勤数据到工资项 */
    private EmpRecordWithOptions getOrCreateRecordAndApplyAttendance(Long employeeId, String jobNumber, Long sRecordId,
                                                                      HrmSalaryMonthRecord salaryMonthRecord, int year, int month,
                                                                      Map<String, Map<Integer, String>> attendanceDataMap,
                                                                      List<HrmSalaryOption> noFixedSalaryOptionList,
                                                                      boolean isJoinAttendance,
                                                                      Boolean isSyncAttendanceData, LocalDate becomeDate) {
        Optional<HrmSalaryMonthEmpRecord> opt = salaryMonthEmpRecordService.lambdaQuery()
                .eq(HrmSalaryMonthEmpRecord::getSRecordId, sRecordId)
                .eq(HrmSalaryMonthEmpRecord::getEmployeeId, employeeId).oneOpt();
        HrmSalaryMonthEmpRecord record;
        List<HrmSalaryMonthOptionValue> options;
        List<HrmSalaryMonthOptionValue> oldFixedOptionValueList = Collections.emptyList();
        boolean existed;
        if (opt.isPresent()) {
            record = opt.get();
            oldFixedOptionValueList = salaryMonthOptionValueService.lambdaQuery()
                    .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, record.getSEmpRecordId())
                    .in(HrmSalaryMonthOptionValue::getCode,
                            Arrays.asList(180101, 180102, 190101, 190102, 190103, 19010401, 19010402,
                                    190105, 190106, 200101, 40102, 280, 281, 282, 20102, 20105, 1))
                    .list();
            salaryMonthOptionValueService.lambdaUpdate().eq(HrmSalaryMonthOptionValue::getSEmpRecordId, record.getSEmpRecordId()).remove();
            options = new ArrayList<>(getNoFixedOptionValue(record, noFixedSalaryOptionList, true, becomeDate));
            existed = true;
        } else {
            Map<Integer, String> cv = attendanceDataMap.get(jobNumber);
            record = new HrmSalaryMonthEmpRecord();
            record.setSRecordId(salaryMonthRecord.getSRecordId());
            record.setEmployeeId(employeeId);
            record.setActualWorkDay(new BigDecimal(cv.get(2)));
            record.setNeedWorkDay(new BigDecimal(cv.get(1)));
            record.setYear(year);
            record.setMonth(month);
            salaryMonthEmpRecordService.save(record);
            options = new ArrayList<>(getNoFixedOptionValue(record, noFixedSalaryOptionList, true, becomeDate));
            existed = false;
        }
        try {
            Map<Integer, String> cv = attendanceDataMap.get(jobNumber);
            if (Boolean.TRUE.equals(isSyncAttendanceData)) {
                record.setActualWorkDay(new BigDecimal(cv.get(2)));
                record.setNeedWorkDay(new BigDecimal(cv.get(1)));
                // updateById 延迟到 batchSaveResults 统一批量执行
                cv.remove(1);
                cv.remove(2);
                options.addAll(getFixedOptionValue(record, cv, isJoinAttendance));
            } else if (!existed) {
                record.setNeedWorkDay(new BigDecimal(cv.get(1)));
                cv.remove(1);
                cv.remove(2);
                options.addAll(getFixedOptionValue(record, cv, isJoinAttendance));
            } else {
                options.addAll(cloneOptionValuesForRecord(oldFixedOptionValueList, record.getSEmpRecordId()));
            }
        } catch (Exception e) {
            throw new HrmException(HrmCodeEnum.ATTENDANCE_DATA_ERROR);
        }
        return new EmpRecordWithOptions(record, options, existed);
    }

    private List<HrmSalaryMonthOptionValue> cloneOptionValuesForRecord(List<HrmSalaryMonthOptionValue> source,
                                                                        Long sEmpRecordId) {
        if (CollUtil.isEmpty(source)) {
            return Collections.emptyList();
        }
        List<HrmSalaryMonthOptionValue> result = new ArrayList<>(source.size());
        for (HrmSalaryMonthOptionValue optionValue : source) {
            if (optionValue == null || optionValue.getCode() == null) {
                continue;
            }
            HrmSalaryMonthOptionValue cloned = new HrmSalaryMonthOptionValue();
            cloned.setSEmpRecordId(sEmpRecordId);
            cloned.setCode(optionValue.getCode());
            cloned.setValue(StrUtil.blankToDefault(optionValue.getValue(), "0"));
            result.add(cloned);
        }
        return result;
    }

    private static class EmpRecordWithOptions {
        final HrmSalaryMonthEmpRecord record;
        final List<HrmSalaryMonthOptionValue> optionValueList;
        final boolean existed;

        EmpRecordWithOptions(HrmSalaryMonthEmpRecord record, List<HrmSalaryMonthOptionValue> optionValueList, boolean existed) {
            this.record = record;
            this.optionValueList = optionValueList;
            this.existed = existed;
        }
    }

    /** 单员工考勤数据填充（考勤同步时调用） */
    private void fillAttendanceDataForEmployee(Map<String, Object> map, AttendanceSyncBatchData batchData,
                                                Map<String, Map<Integer, String>> attendanceDataMap, int year, int month,
                                                boolean hasAttendanceGroup) {
    // 休息天数
    Integer restDays = ZERO;
    // 应出勤天数（从批量数据取，避免重复查询）
    Double normalDays = DEFAULT_NORMAL_DAYS;
    int deptType = getDeptTypeForEmployee(map);
    LoginUserInfo info = CompanyContext.get();
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
    Long employeeId = Convert.toLong(map.get("employeeId"));//员工ID
    String employeeName = map.get("employeeName").toString();//员工名称
    String jobNumber = (String) map.get("jobNumber");//工号
    Integer status = (Integer)map.get("status");//员工状态 1正式 2试用
    Date becomeTime = (Date) map.get("becomeTime");//转正日期
    String post = (String) map.get("post");//职位
    String deptName = (String) map.get("deptName"); //部门
    Integer expandProduction = (Integer)map.get("expand_production"); //是否加入钉钉，未加入钉钉的算满勤
    Long fullMoneyInteger = (Long) map.get("fullMoney");//员工对应的全勤奖 金额
    Long fullMoney =0L;//员工对应的全勤奖 金额
    if(fullMoneyInteger!=null)
    {
        fullMoney = fullMoneyInteger.longValue();
    }
    String fullAttendanceFlag = String.valueOf((Integer)map.get("isFullAttendance"));//员工是否享有全勤
    String isProduceDept = (String) map.get("isProduceDept");//员工部门是否为生产类型部门
    String entryStatus = String.valueOf(map.get("entryStatus"));//员工是否离职
    Date entryDate = (Date) map.get("entryDate");//入职日期
    boolean isProduce = isProduceDept.equals("0")?false:true;
    normalDays = batchData.normalDaysByDeptType.getOrDefault(deptType, DEFAULT_NORMAL_DAYS);
    List<HrmAttendanceSummaryDayVo> employeeSummaryDayList = batchData.attendanceSummaryDayMap.getOrDefault(employeeId, Collections.emptyList());
    HrmAttendanceSummaryVo empAttendanceSummary = batchData.attendanceSummaryMap.get(employeeId);

    //其他扣款
    BigDecimal otherDeduction = new BigDecimal(ZERO);
    //其他补贴
    BigDecimal otherSubsidy = new BigDecimal(ZERO);
    //高温津贴
    BigDecimal highTemperature = new BigDecimal(ZERO);
    //低温津贴
    BigDecimal lowTemperature = new BigDecimal(ZERO);
    //借款
    BigDecimal loanMoney = new BigDecimal(ZERO);
    //获取根据ID数据库中员工考勤表数据（来自批量加载）
    HrmProduceAttendance hrmProduceAttendance = batchData.overTimePayEmpMap.get(employeeId);
    if(hrmProduceAttendance!=null)
    {
        otherDeduction = hrmProduceAttendance.getOtherDeductions();
        otherSubsidy = hrmProduceAttendance.getOtherSubsidies();
        loanMoney = hrmProduceAttendance.getLoan();
        highTemperature = hrmProduceAttendance.getHighTemperature();
        lowTemperature = hrmProduceAttendance.getLowTemperature();
    }
    //考勤缺勤抵扣数据（从批量预加载数据获取，避免循环内逐条查询）
    List<QueryHolidayDeductionVO> deductionVOList = batchData.holidayDeductionMap.getOrDefault(employeeId, Collections.emptyList());
    //判断是否为全勤（使用批量考勤数据）
    if (IS_DISABLED_NO.equals(String.valueOf(map.get("isDisabled")))) {
        isFullAttendance = checkIsFullAttendance(employeeSummaryDayList, empAttendanceSummary, deductionVOList, expandProduction);
    }

    //没有考勤组的员工存在非满勤的情况
    if(!hasAttendanceGroup && !"4".equals(entryStatus))
    {
        //如果员工没有放在考勤组，则工资计算排除考勤数据
        empAttendanceMap.put(1, hrmProduceAttendance!=null?String.valueOf(hrmProduceAttendance.getPositiveAttendance()):"21.75");
        empAttendanceMap.put(2, hrmProduceAttendance!=null?String.valueOf(hrmProduceAttendance.getPositiveAttendance()):"21.75");
        empAttendanceMap.put(280, otherDeduction!=null?otherDeduction.toString():"0");//其他扣款
        empAttendanceMap.put(281, otherSubsidy!=null ? otherSubsidy.toString():"0");//其他补贴
        empAttendanceMap.put(282, loanMoney!=null ? loanMoney.toString():"0");//借款//全勤奖
        empAttendanceMap.put(20102, highTemperature!=null ? highTemperature.toString():"0");//高温补贴
        empAttendanceMap.put(20105, lowTemperature!=null ? lowTemperature.toString():"0");//低温补贴

        //没有考勤组的员工也要转正才能有全勤奖
        if(becomeTime!=null)
        {
            //判断转正日期是否为空，并且转正日期是否在计薪月之前
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(becomeTime);
            YearMonth yearMonth1 = YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
            YearMonth yearMonth2 = YearMonth.of(year, month);
            if(((yearMonth1.equals(yearMonth2) ||yearMonth1.isBefore(yearMonth2))) && "1".equals(fullAttendanceFlag))
            {
                if(isFullAttendance)
                {
                    empAttendanceMap.put(40102, fullMoney==null?"0":String.valueOf(fullMoney));
                }else if (employeeId.toString().equals("1712718940181") || employeeId.toString().equals("1712718940179") || employeeId.toString().equals("1789114659308232706")) {
                    //董事长和总经理、张宏海直接全勤
                    empAttendanceMap.put(40102, String.valueOf(fullMoney));
                }
            }
        }


                        attendanceDataMap.put(jobNumber, empAttendanceMap);

                        //存在可能出现离职员工没有考勤记录但是需要取夜班和加班数据（使用批量加载的 salaryBasic）
                        getYeBanAndJiaBan(hrmProduceAttendance, empAttendanceMap, batchData.salaryBasic, isProduce);

        return;
    }
    //根据员工的工号查询员工定薪/调薪数据（来自批量加载）
    QuerySalaryArchivesListVO salaryArchivesOption = batchData.empSalaryArchivesMap.get(employeeId);
    if(salaryArchivesOption != null)
    {
        empSalary = new BigDecimal(Double.parseDouble(salaryArchivesOption.getTotal()));
    }
    //员工当月排班时长（从批量预加载数据获取，避免循环内逐条查询）
    HashMap<String,Double> workTimsMap = getWorkHoursFromBatch(employeeId, isProduce, batchData.dates, batchData.workHoursMap);

    //病假天数
    leaveOfsickDays = sickDeductDays(isProduce, employeeSummaryDayList, workTimsMap, "2", deductionVOList);

    //事假天数
    leaveOfAbsenceDays = sickDeductDays(isProduce, employeeSummaryDayList, workTimsMap, "1", deductionVOList);

    //获取默认的扣款规则
    HrmAttendanceRule hrmAttendanceRule = batchData.attendanceRule;
    if (hrmAttendanceRule == null) {
        hrmAttendanceRule = hrmAttendanceRuleService.lambdaQuery().orderByDesc(HrmAttendanceRule::getCreateTime).one();
    }


    //加班天数
//                    overTimeCount = attendanceClockService.queryEmpAttendanceOverTimeCountDays(Arrays.asList(LocalDateTimeUtil.of(dateStartTime).toLocalDate(), LocalDateTimeUtil.of(dateEndTime).toLocalDate()), employeeId);
    //这里直接用钉钉的考勤统计数据（来自批量加载）
    if(empAttendanceSummary == null)
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
        if(hrmProduceAttendance!=null && hrmProduceAttendance.getPositiveAttendance()!=null)
        {
            //如果手动导入的实际出勤天数不为空，则取手动导入的数据
            empAttendanceSummary.setActualityDays(hrmProduceAttendance.getPositiveAttendance().doubleValue());

        }
//                        normalDays = getNormalDays(empAttendanceSummary,isProduce,daysInMonth);
        //应出勤天数直接从批量缓存取
        normalDays = batchData.normalDaysByDeptType.getOrDefault(deptType, DEFAULT_NORMAL_DAYS);
        //由于存在生产体系有些员工为双休 需要按照双休的天数来计算每个月应出勤天数
        if (employeeId.toString().equals("1712718940217") || employeeId.toString().equals("1712718940220") || employeeId.toString().equals("1712718940186")) {
            //获取指定年-月的双休天数
            int ShuangXiuDays = getShuangXiuDays(year,month);
            //将指定年-月的整个月份的天数减去双休天数得到该月应出勤天数并赋值给normalDays
            normalDays = Double.parseDouble(new BigDecimal(batchData.daysInMonth).subtract(new BigDecimal(ShuangXiuDays)).toString());
        }
        //如果实际出勤天数小于应出勤天数，则表示该员工没有满勤
        if(empAttendanceSummary.getActualityDays()<normalDays)
        {
            isFullAttendance = false;
        }
        //判断员工是否离职
        if("4".equals(entryStatus))
        {
            //计算离职员工的应出勤天数
            if(entryStatus!=null && "4".equals(entryStatus))
            {
                //离职后，没上班的天数，设置为旷工天数，以便后面超缺勤的计算
                empAttendanceSummary.setAbsenteeismDays(normalDays-empAttendanceSummary.getActualityDays());
            }
        }
        //如果入职日期不为空
        if(entryDate!=null)
        {
            //判断入职时间是否在当前月
            LocalDate startTime = DateUtil.beginOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
            LocalDate endTime = DateUtil.endOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
            Instant instant = entryDate.toInstant();
            ZoneId zoneId = ZoneId.systemDefault();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
            LocalDate dateToCheck = localDateTime.toLocalDate();
            boolean isWithinRange = MyDateUtils.isWithinRange(dateToCheck, startTime, endTime);
            if(isWithinRange)
            {
                HrmProduceAttendance defaultHrmProduceAttendance = new HrmProduceAttendance();
//                                if (info.getCompanyId().equals("0001")) { //海南达川
//                                    defaultHrmProduceAttendance = hasOverTimePayEmpList.stream().filter(f -> f.getEmployeeId()!=null && f.getEmployeeId().toString().equals("1712718940181")).findAny().orElse(null);
//                                }else if (info.getCompanyId().equals("0002")){ //成都分公司
//                                    defaultHrmProduceAttendance = hasOverTimePayEmpList.stream().filter(f -> f.getEmployeeId()!=null && f.getEmployeeId().toString().equals("1796721967383121945")).findAny().orElse(null);
//                                }
                //这里由于hrmAttendanceInfo有每个员工的应出勤天数，所以直接用应出勤天数normalDays-该员工考勤统计表hrmProduceAttendance的实际出勤天数得到离职后没有上班的天数并赋值给AbsenteeismDays
                //离职后，没上班的天数，设置为旷工天数，以便后面超缺勤的计算
                empAttendanceSummary.setAbsenteeismDays(normalDays-empAttendanceSummary.getActualityDays());
            }

        }
        Double AbsenteeismDays = normalDays-empAttendanceSummary.getActualityDays();
        if (String.valueOf(map.get("isDisabled")).equals("1")) {
            //如过是残疾人的话,发满基本工资。发满基本工资。
            empAttendanceSummary.setAbsenteeismDays(0.0);
        }
        //再次判断如果缺卡次数=0 && 旷工天数=0 &产假=0 & 婚假=0 & 陪产假=0 &丧假=0 &事假=0 & 哺乳假=0 & 病假=0 &早退=0 &调休=0 & 早退=0 并且isFullAttendance为false，则判断为全勤
        else if((0==empAttendanceSummary.getMisscardCount() && 0==empAttendanceSummary.getAbsenteeismDays()
                && 0==empAttendanceSummary.getChanjia() && 0==empAttendanceSummary.getHunjia() && 0==empAttendanceSummary.getPeichanjia() && 0==empAttendanceSummary.getSangjia() && 0==empAttendanceSummary.getEarlyMinute()
                && 0==empAttendanceSummary.getShijia() && 0==empAttendanceSummary.getBurujia() && 0==empAttendanceSummary.getBingjia() && 0==empAttendanceSummary.getEarlyCount()) && !isFullAttendance)
        {
            //如果没有请假，迟到，并且又不是全勤，则判定为旷工
            empAttendanceSummary.setAbsenteeismDays(AbsenteeismDays);
        }else if (AbsenteeismDays <= 0) {
            //如果钉钉打卡数据没有异常，可能存在手动上传的实际出勤天数和应出勤天数不一样
            empAttendanceSummary.setAbsenteeismDays(AbsenteeismDays);
        }else if (empAttendanceSummary.getBingjia() > 0) {
            if (info != null && "0001".equals(info.getCompanyId())) {
                //病假存在先补贴再扣钱（旷工扣款显示）的情况
                empAttendanceSummary.setAbsenteeismDays(empAttendanceSummary.getBingjia());
            }
        }
        attendanceEmpRecordMap.put("normalDays", normalDays);//应出勤天数
        attendanceEmpRecordMap.put("actualWorkDay", empAttendanceSummary.getActualityDays());//实际出勤天数
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
    //取整算法
    BigDecimal absenteeismMoney = new BigDecimal(ZERO);
    if (info != null && ("0001".equals(info.getCompanyId()) || "0005".equals(info.getCompanyId()) || "0003".equals(info.getCompanyId()))) {
        //精确小数算法
        absenteeismMoney = empSalary.divide(new BigDecimal(normalDays),4, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("absenteeismDays").toString())).setScale(2, RoundingMode.HALF_UP);
    }else {
        //取整算法
        absenteeismMoney = empSalary.divide(new BigDecimal(normalDays), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("absenteeismDays").toString())).setScale(0, RoundingMode.HALF_UP);
    }

    //事假扣款 事假天数*每天的工资
    BigDecimal leaveOfAbsenceMoney = new BigDecimal(ZERO);
    if (info != null && "0001".equals(info.getCompanyId())) {
        //精确小数算法
        leaveOfAbsenceMoney =empSalary.divide(new BigDecimal(normalDays),4, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("leaveOfAbsence").toString())).setScale(2, RoundingMode.HALF_UP);
    }else {
        //取整算法
        leaveOfAbsenceMoney = empSalary.divide(new BigDecimal(normalDays), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("leaveOfAbsence").toString())).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 获取夜班补贴（使用批量加载的 salaryBasic）
     */
    getYeBanAndJiaBan(hrmProduceAttendance, empAttendanceMap, batchData.salaryBasic, isProduce);
    /**
     * 满勤奖金，计算
     */
    //正式员工才有全勤奖
    if(becomeTime!=null)
    {
        //判断转正日期是否为空，并且转正日期是否在计薪月之前
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(becomeTime);
        YearMonth yearMonth1 = YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
        YearMonth yearMonth2 = YearMonth.of(year, month);
        if(isFullAttendance && (yearMonth1.equals(yearMonth2) || yearMonth1.isBefore(yearMonth2)) && "1".equals(fullAttendanceFlag))
        {
            empAttendanceMap.put(40102, fullMoney==null?"0":String.valueOf(fullMoney));//全勤奖
        }
    }
    else
    {
        if(isFullAttendance  && status!=null && status==1 && "1".equals(fullAttendanceFlag))
        {
            empAttendanceMap.put(40102, fullMoney==null?"0":String.valueOf(fullMoney));//全勤奖
        }
    }
//                    成都公司试用期也有全勤
    if (info != null && "0002".equals(info.getCompanyId())) {
        if(isFullAttendance  && status!=null && status==2 && "1".equals(fullAttendanceFlag))
        {
            empAttendanceMap.put(40102, fullMoney==null?"0":String.valueOf(fullMoney));//全勤奖
        }
    }
    //市场入职默认有全勤（从 map 获取 deptId，从批量预加载的部门数据获取部门名称，避免循环内逐条查询）
    Long empDeptId = Convert.toLong(map.get("deptId"));
    if (empDeptId != null && empDeptId.toString().equals("1481534121629855751")) {
        String empDeptName = batchData.deptNameMap.get(empDeptId);
        if (empDeptName != null && empDeptName.contains("总监")) {
            empAttendanceMap.put(40102, "500");//全勤奖
        } else {
            empAttendanceMap.put(40102, "100");
        }
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
    empAttendanceMap.put(20102, highTemperature!=null ? highTemperature.toString():"0");//高温津贴
    empAttendanceMap.put(20105, lowTemperature!=null ? lowTemperature.toString():"0");//低温津贴

    //病假扣款规则
    /**
     * 2天内全额发放2天的工资，假如3天病假，则两天算全额工资，剩余的1天就按照最低工资标准扣钱
     * 每天扣的钱是1680/25（天）=67.2元/天
     * 3天病假总计67.2*1 = 67.2元
     */
    if(leaveOfsickDays.compareTo(new BigDecimal(2))==-1 || leaveOfsickDays.compareTo(new BigDecimal(2))==0)
    {
        empAttendanceMap.put(19010401, "0");
    }
    else
    {
        //两天算全额工资(不扣钱)，剩余的算基本工资
        BigDecimal needKoukuanDays = leaveOfsickDays.subtract(new BigDecimal(2));
        //当地最低基本工资（来自批量加载）
        BigDecimal baseSalary = batchData.salaryBasic.getSalaryBasic();
        //每天需要扣的钱((基本工资)/应出勤天数 *缺的天数)
        BigDecimal oneDayMoney = baseSalary.divide(new BigDecimal(normalDays), 2, RoundingMode.HALF_UP);
        //计算病假需要扣的钱
        BigDecimal sickMoney = oneDayMoney.multiply(needKoukuanDays).setScale(0, RoundingMode.HALF_UP);
        empAttendanceMap.put(19010401, sickMoney.toString());//假期扣款(病假)
    }
    attendanceDataMap.put(jobNumber, empAttendanceMap);
    }

    private void getEmloyeeQuitSalary(int year, int month, Long empId, Long sRecordId){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate starLocalDate = LocalDate.of(year, month, 01);
        LocalDate starLastMonthDate = starLocalDate.minusMonths(1); // 获取上个月的日期

        String lastMonthName = starLastMonthDate.format(formatter);
        String[] lastMonthNames = lastMonthName.split("-");
        LocalDate endLocalDate = LocalDate.of(Integer.parseInt(lastMonthNames[0]),
                Integer.parseInt(lastMonthNames[1]),
                getMonthDays(Integer.parseInt(lastMonthNames[1]), Integer.parseInt(lastMonthNames[0])));


        if (empId == null) {
            //获取上个月所有离职人员
            List<HrmEmployeeQuitInfo> lists = hrmEmployeeQuitInfoService.lambdaQuery().ge(HrmEmployeeQuitInfo::getApplyQuitTime, lastMonthName).
                    le(HrmEmployeeQuitInfo::getApplyQuitTime, endLocalDate.format(formatter)).list();
            List<HrmSalaryMonthEmpRecord> hrmSalaryMonthEmpRecords = new ArrayList<>();
            List<Long> employeeIds = new ArrayList<>();
            for (HrmEmployeeQuitInfo hrmEmployeeQuitInfo : lists) {
                HrmProduceAttendance hrmProduceAttendance = produceAttendanceService.lambdaQuery().eq(HrmProduceAttendance::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId()).one();
                if (hrmProduceAttendance != null) {
                    HrmSalaryMonthEmpRecord hrmSalaryMonthEmpRecord = new HrmSalaryMonthEmpRecord();
                    hrmSalaryMonthEmpRecord.setSRecordId(sRecordId);
                    hrmSalaryMonthEmpRecord.setEmployeeId(hrmEmployeeQuitInfo.getEmployeeId());
                    hrmSalaryMonthEmpRecords.add(hrmSalaryMonthEmpRecord);
                    employeeIds.add(hrmEmployeeQuitInfo.getEmployeeId());
                }
                LambdaQueryWrapper<HrmSalaryMonthEmpRecord> salaryMonthEmpRecordLambdaQueryWrapper = new LambdaQueryWrapper<>();
                salaryMonthEmpRecordLambdaQueryWrapper.eq(HrmSalaryMonthEmpRecord::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId());
                salaryMonthEmpRecordMapper.delete(salaryMonthEmpRecordLambdaQueryWrapper);
                hrmSalaryMonthEmpRecordService.saveBatch(hrmSalaryMonthEmpRecords);
            }
            hrmSalaryMonthEmpRecordService.saveBatch(hrmSalaryMonthEmpRecords);

            //有BUG
            List<String> codes = Arrays.asList("180102", "210101", "240101");
            for (HrmEmployeeQuitInfo hrmEmployeeQuitInfo : lists) {
                List<HrmSalaryMonthOptionValue> saves = new ArrayList<>();
                List<Long> sEmpRecordIds = new ArrayList<>();
                for (String code : codes) {
                    HrmSalaryMonthEmpRecord hrmSalaryMonthEmpRecord = hrmSalaryMonthEmpRecordService.lambdaQuery().eq(HrmSalaryMonthEmpRecord::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId()).one();
                    HrmSalaryMonthOptionValue hrmSalaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                    hrmSalaryMonthOptionValue.setSEmpRecordId(hrmSalaryMonthEmpRecord.getSEmpRecordId());
                    HrmProduceAttendance hrmProduceAttendance = produceAttendanceService.lambdaQuery().eq(HrmProduceAttendance::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId()).one();
                    hrmSalaryMonthOptionValue.setCode(Integer.parseInt(code));
                    if (Strings.isEmpty(hrmProduceAttendance.getNightSubsidy().toString()) || hrmProduceAttendance.getNightSubsidy() == null) {
                        hrmSalaryMonthOptionValue.setValue("0");
                    } else {
                        hrmSalaryMonthOptionValue.setValue(hrmProduceAttendance.getNightSubsidy().toString());
                    }
                    sEmpRecordIds.add(hrmSalaryMonthEmpRecord.getSEmpRecordId());
                    saves.add(hrmSalaryMonthOptionValue);
                    LambdaQueryWrapper<HrmSalaryMonthOptionValue> salaryMonthOptionValueQueryWrapper = new LambdaQueryWrapper<>();
                    salaryMonthOptionValueQueryWrapper.eq(HrmSalaryMonthOptionValue::getSEmpRecordId, hrmSalaryMonthEmpRecord.getSEmpRecordId());
                    salaryMonthOptionValueMapper.delete(salaryMonthOptionValueQueryWrapper);
                }
                salaryMonthOptionValueService.saveBatch(saves);
            }
        }else {
            HrmProduceAttendance hrmProduceAttendance = produceAttendanceService.lambdaQuery().eq(HrmProduceAttendance::getEmployeeId, empId).one();
            if (hrmProduceAttendance != null) {
                List<HrmSalaryMonthEmpRecord> hrmSalaryMonthEmpRecords = new ArrayList<>();
                HrmEmployeeQuitInfo hrmEmployeeQuitInfo = hrmEmployeeQuitInfoService.lambdaQuery().eq(HrmEmployeeQuitInfo::getEmployeeId, empId).ge(HrmEmployeeQuitInfo::getApplyQuitTime, lastMonthName).
                        le(HrmEmployeeQuitInfo::getApplyQuitTime, endLocalDate.format(formatter)).one();
                if (hrmEmployeeQuitInfo != null) {
                    HrmSalaryMonthEmpRecord hrmSalaryMonthEmpRecord = new HrmSalaryMonthEmpRecord();
                    hrmSalaryMonthEmpRecord.setSRecordId(sRecordId);
                    hrmSalaryMonthEmpRecord.setEmployeeId(empId);
                    hrmSalaryMonthEmpRecords.add(hrmSalaryMonthEmpRecord);

                    LambdaQueryWrapper<HrmSalaryMonthEmpRecord> salaryMonthEmpRecordLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    salaryMonthEmpRecordLambdaQueryWrapper.eq(HrmSalaryMonthEmpRecord::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId());
                    salaryMonthEmpRecordMapper.delete(salaryMonthEmpRecordLambdaQueryWrapper);
                    hrmSalaryMonthEmpRecordService.saveBatch(hrmSalaryMonthEmpRecords);

                    List<String> codes = Arrays.asList("180102", "210101", "240101");
                    List<HrmSalaryMonthOptionValue> saves = new ArrayList<>();
                    for (String code : codes) {
                        HrmSalaryMonthEmpRecord findOne = hrmSalaryMonthEmpRecordService.lambdaQuery().eq(HrmSalaryMonthEmpRecord::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId()).one();
                        HrmSalaryMonthOptionValue hrmSalaryMonthOptionValue = new HrmSalaryMonthOptionValue();
                        hrmSalaryMonthOptionValue.setSEmpRecordId(findOne.getSEmpRecordId());
//                        HrmProduceAttendance hrmProduceAttendance = produceAttendanceService.lambdaQuery().eq(HrmProduceAttendance::getEmployeeId, hrmEmployeeQuitInfo.getEmployeeId()).one();
                        hrmSalaryMonthOptionValue.setCode(Integer.parseInt(code));
                        if (Strings.isEmpty(hrmProduceAttendance.getNightSubsidy().toString()) || hrmProduceAttendance.getNightSubsidy() == null) {
                            hrmSalaryMonthOptionValue.setValue("0");
                        } else {
                            hrmSalaryMonthOptionValue.setValue(hrmProduceAttendance.getNightSubsidy().toString());
                        }
                        saves.add(hrmSalaryMonthOptionValue);
                        LambdaQueryWrapper<HrmSalaryMonthOptionValue> salaryMonthOptionValueQueryWrapper = new LambdaQueryWrapper<>();
                        salaryMonthOptionValueQueryWrapper.eq(HrmSalaryMonthOptionValue::getSEmpRecordId, findOne.getSEmpRecordId());
                        salaryMonthOptionValueMapper.delete(salaryMonthOptionValueQueryWrapper);
                    }
                    salaryMonthOptionValueService.saveBatch(saves);
                }
            }
        }
    }

    public static int getMonthDays(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }

    //获取双休天数
    public static int getShuangXiuDays(int year, int month) {
        int weekendCount = 0;
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            int dayOfWeek = LocalDate.of(year, month, day).getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                weekendCount ++;
            }
        }
        return weekendCount;
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
    public List<Map<String, Object>> queryHasSalaryArchivesEmployeeList(HrmSalaryMonthRecord salaryMonthRecord, Long employeeId)
    {
        List<Map<String, Object>> employeeList = new ArrayList<>();
        if (salaryMonthRecord == null) {
            return employeeList;
        }
        Collection<Long> dataAuthEmployeeIds = new ArrayList<>();
        if(employeeId!=null)
        {
            dataAuthEmployeeIds.add(employeeId);
        }
        else
        {
            dataAuthEmployeeIds = employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).eq(HrmEmployee::getIsDel, 0).list()
                    .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList());
        }
        List<Map<String, Object>> list = salaryMonthEmpRecordMapper.queryPaySalaryEmployeeList(salaryMonthRecord.getEndTime(), dataAuthEmployeeIds);
        if (CollUtil.isEmpty(list)) {
            return employeeList;
        }

        List<Long> employeeIds = list.stream()
                .map(m -> Convert.toLong(m.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return employeeList;
        }

        QuerySalaryArchivesListDto querySalaryArchivesListDto = new QuerySalaryArchivesListDto();
        querySalaryArchivesListDto.setEmployeeIds(employeeIds);
        querySalaryArchivesListDto.setYear(salaryMonthRecord.getYear());
        querySalaryArchivesListDto.setMonth(salaryMonthRecord.getMonth());
        List<QuerySalaryArchivesListVO> empSalaryArchivesList = salaryArchivesService.queryEmpSalaryArchivesList(querySalaryArchivesListDto);

        Set<Long> validEmployeeIds = empSalaryArchivesList.stream()
                .filter(this::hasPositiveSalaryArchive)
                .map(QuerySalaryArchivesListVO::getEmployeeId)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(validEmployeeIds)) {
            return employeeList;
        }

        list.forEach(map -> {
            Long currentEmployeeId = Convert.toLong(map.get("employeeId"));
            if (currentEmployeeId != null && validEmployeeIds.contains(currentEmployeeId)) {
                employeeList.add(map);
            }
        });

        if (CollUtil.isNotEmpty(employeeList)) {
            Set<Long> matchedEmployeeIds = employeeList.stream()
                    .map(m -> Convert.toLong(m.get("employeeId")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, Integer> isRemarkMap = employeeService.lambdaQuery()
                    .select(HrmEmployee::getEmployeeId, HrmEmployee::getIsRemark)
                    .in(HrmEmployee::getEmployeeId, matchedEmployeeIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(HrmEmployee::getEmployeeId,
                            employee -> employee.getIsRemark() == null ? 0 : employee.getIsRemark(),
                            (v1, v2) -> v1));
            for (Map<String, Object> employeeMap : employeeList) {
                Long currentEmployeeId = Convert.toLong(employeeMap.get("employeeId"));
                if (currentEmployeeId == null) {
                    continue;
                }
                employeeMap.put("isRemark", isRemarkMap.getOrDefault(currentEmployeeId, 0));
            }
        }
        return employeeList;

    }

    private boolean hasPositiveSalaryArchive(QuerySalaryArchivesListVO salaryArchivesListVO) {
        if (salaryArchivesListVO == null || salaryArchivesListVO.getEmployeeId() == null) {
            return false;
        }
        if (StrUtil.isBlank(salaryArchivesListVO.getTotal())) {
            return false;
        }
        try {
            return new BigDecimal(salaryArchivesListVO.getTotal()).compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 计算薪资
     *
     * @param salaryMonthEmpRecord
     * @param cumulativeTaxOfLastMonthData 上个月的累计税数据
     * @return
     */
    public List<HrmSalaryMonthOptionValue> computeSalary(HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                         Map<Integer, String> cumulativeTaxOfLastMonthData,HrmEmployeeVO hrmEmployeeVO, String isDisabled) {
        try {
            //员工计税规则
            HrmSalaryTaxRule hrmSalaryTaxRule = new HrmSalaryTaxRule();
            hrmSalaryTaxRule.setIsTax(1);
            hrmSalaryTaxRule.setCycleType(1);
            hrmSalaryTaxRule.setMarkingPoint(5000);
            hrmSalaryTaxRule.setTaxType(1);
            SalaryBaseTotal salaryBaseTotal = salaryComputeService.baseComputeSalary(salaryMonthEmpRecord);
            return salaryComputeService.computeSalary(salaryBaseTotal, salaryMonthEmpRecord, hrmSalaryTaxRule, cumulativeTaxOfLastMonthData, hrmEmployeeVO, isDisabled);
        }catch (Exception ax) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw ax;
        }
    }


    /**
     * 获取最新的每月薪资记录
     * @return
     */
    public HrmSalaryMonthRecord queryLastSalaryMonthRecord()
    {
        HrmSalaryMonthRecord salaryMonthRecord =salaryMonthRecordMapper.getLastSalaryRecord();
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
        return salaryMonthRecord;
    }


    /**
     * 创建下月薪资表
     * @return
     */
    @Transactional
    public OperationLog addNextMonthSalary() {
        //查询薪资上月记录,如果有就往后推一个月,如果没有就去薪资配置计薪月
        HrmSalaryMonthRecord lastSalaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("limit 1").one();
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

    /**
     * 更新专项附加扣除
     * @return
     */
    /**
     * 计算并更新下个月的专项附加扣除累计数据
     * 
     * 业务规则：
     * 1. 从 hrm_additional 表获取当前月员工的实际专项附加扣除数据
     * 2. 从 hrm_employee_additional 表获取下一年的员工配置数据
     * 3. 非12月：下月累计 = 当月实际扣除 + 员工配置
     * 4. 12月：跨年清零，下月累计 = 员工配置（不累加）
     * 
     * @param empId 员工ID（null表示处理全部员工）
     * @param year 当前计薪年份
     * @param month 当前计薪月份
     * @return OperationLog
     */
    @Transactional
    public OperationLog updateAddition(Long empId, int year, int month) {
        // 1. 计算下个月的年月
        int[] nextYearMonth = getNextMonthYearAndMonth(year, month);
        int nextYear = nextYearMonth[0];
        int nextMonth = nextYearMonth[1];
        boolean isCrossYear = (month == 12); // 是否跨年
        
        // 2. 查询下一年的员工专项附加扣除配置（hrm_employee_additional）
        List<HrmEmployeeAdditional> employeeConfigList = queryEmployeeAdditionalConfig(empId, nextYear);
        if (CollUtil.isEmpty(employeeConfigList)) {
            // 没有配置时保持原语义：清理下月数据
            deleteNextMonthAdditional(empId, nextYear, nextMonth);
            return null;
        }
        Set<Long> configuredEmployeeIds = employeeConfigList.stream()
                .map(HrmEmployeeAdditional::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 3. 查询当前月的实际专项附加扣除数据（hrm_additional）
        List<HrmAdditional> currentMonthDataList = queryCurrentMonthAdditional(empId, year, month);
        
        // 4. 构建员工ID -> 当前月数据的映射，提升查询效率
        Map<Long, HrmAdditional> currentMonthDataMap = currentMonthDataList.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmAdditional::getEmployeeId, Function.identity(), (v1, v2) -> v1));
        
        // 5. 生成下个月的累计数据
        List<HrmAdditional> nextMonthDataList = new ArrayList<>();
        for (HrmEmployeeAdditional employeeConfig : employeeConfigList) {
            Long employeeId = employeeConfig.getEmployeeId();
            if (employeeId == null) {
                continue;
            }
            HrmAdditional currentMonthData = currentMonthDataMap.get(employeeId);
            
            // 创建下个月的累计数据
            HrmAdditional nextMonthData = new HrmAdditional();
            nextMonthData.setEmployeeId(employeeId);
            nextMonthData.setYear(nextYear);
            nextMonthData.setMonth(nextMonth);
            
            // 根据是否跨年和是否有当月数据决定累计方式
            if (currentMonthData == null) {
                // 情况1：新入职员工，当前月没有附加扣除数据
                // 直接使用配置值作为下月累计值（从0开始累计）
                nextMonthData.setChildrenEducation(safeAmount(employeeConfig.getChildrenEducation()));
                nextMonthData.setHousingLoanInterest(safeAmount(employeeConfig.getHousingLoanInterest()));
                nextMonthData.setHousingRent(safeAmount(employeeConfig.getHousingRent()));
                nextMonthData.setSupportingTheElderly(safeAmount(employeeConfig.getSupportingTheElderly()));
                nextMonthData.setContinuingEducation(safeAmount(employeeConfig.getContinuingEducation()));
                nextMonthData.setRaisingGirls(safeAmount(employeeConfig.getRaisingGirls()));
            } else if (isCrossYear) {
                // 情况2：12月到1月跨年，累计清零
                // 直接使用配置值作为下月累计值
                nextMonthData.setChildrenEducation(safeAmount(employeeConfig.getChildrenEducation()));
                nextMonthData.setHousingLoanInterest(safeAmount(employeeConfig.getHousingLoanInterest()));
                nextMonthData.setHousingRent(safeAmount(employeeConfig.getHousingRent()));
                nextMonthData.setSupportingTheElderly(safeAmount(employeeConfig.getSupportingTheElderly()));
                nextMonthData.setContinuingEducation(safeAmount(employeeConfig.getContinuingEducation()));
                nextMonthData.setRaisingGirls(safeAmount(employeeConfig.getRaisingGirls()));
            } else {
                // 情况3：非跨年且有当月数据，累加计算
                // 下月累计 = 当月累计 + 员工配置
                nextMonthData.setChildrenEducation(
                        safeAmount(currentMonthData.getChildrenEducation()).add(safeAmount(employeeConfig.getChildrenEducation())));
                nextMonthData.setHousingLoanInterest(
                        safeAmount(currentMonthData.getHousingLoanInterest()).add(safeAmount(employeeConfig.getHousingLoanInterest())));
                nextMonthData.setHousingRent(
                        safeAmount(currentMonthData.getHousingRent()).add(safeAmount(employeeConfig.getHousingRent())));
                nextMonthData.setSupportingTheElderly(
                        safeAmount(currentMonthData.getSupportingTheElderly()).add(safeAmount(employeeConfig.getSupportingTheElderly())));
                nextMonthData.setContinuingEducation(
                        safeAmount(currentMonthData.getContinuingEducation()).add(safeAmount(employeeConfig.getContinuingEducation())));
                nextMonthData.setRaisingGirls(
                        safeAmount(currentMonthData.getRaisingGirls()).add(safeAmount(employeeConfig.getRaisingGirls())));
            }
            
            nextMonthDataList.add(nextMonthData);
        }
        
        // 6. 先清理下月未配置员工数据与重复数据，避免残留脏数据
        List<HrmAdditional> existingNextMonthDataList = queryCurrentMonthAdditional(empId, nextYear, nextMonth);
        List<Long> deleteIds = collectAdditionalIdsToDelete(existingNextMonthDataList, configuredEmployeeIds);
        Set<Long> deleteIdSet = new HashSet<>(deleteIds);
        if (CollUtil.isNotEmpty(deleteIds)) {
            hrmAdditionalService.removeByIds(deleteIds);
        }

        // 7. 批量保存下个月的累计数据
        if (CollUtil.isEmpty(nextMonthDataList)) {
            return null;
        }
        Map<Long, HrmAdditional> existingNextMonthDataMap = existingNextMonthDataList.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getEmployeeId() != null && !deleteIdSet.contains(item.getAdditionalId()))
                .collect(Collectors.toMap(HrmAdditional::getEmployeeId, Function.identity(), (v1, v2) -> v1));
        for (HrmAdditional nextMonthData : nextMonthDataList) {
            if (nextMonthData == null || nextMonthData.getEmployeeId() == null) {
                continue;
            }
            HrmAdditional existing = existingNextMonthDataMap.get(nextMonthData.getEmployeeId());
            if (existing != null) {
                nextMonthData.setAdditionalId(existing.getAdditionalId());
            }
        }
        hrmAdditionalService.saveOrUpdateBatch(nextMonthDataList);

        return null;
    }

    static List<Long> collectAdditionalIdsToDelete(List<HrmAdditional> existingNextMonthDataList,
                                                    Set<Long> configuredEmployeeIds) {
        if (CollUtil.isEmpty(existingNextMonthDataList)) {
            return Collections.emptyList();
        }
        Set<Long> configuredIds = configuredEmployeeIds == null ? Collections.emptySet() : configuredEmployeeIds;
        List<Long> deleteIds = new ArrayList<>();
        Set<Long> seenConfiguredEmployeeIds = new HashSet<>();
        for (HrmAdditional existing : existingNextMonthDataList) {
            if (existing == null || existing.getAdditionalId() == null) {
                continue;
            }
            Long existingEmployeeId = existing.getEmployeeId();
            if (existingEmployeeId == null || !configuredIds.contains(existingEmployeeId)) {
                deleteIds.add(existing.getAdditionalId());
                continue;
            }
            if (!seenConfiguredEmployeeIds.add(existingEmployeeId)) {
                deleteIds.add(existing.getAdditionalId());
            }
        }
        return deleteIds;
    }
    
    /**
     * 查询员工专项附加扣除配置（hrm_employee_additional表）
     * 
     * @param empId 员工ID（null表示查询全部）
     * @param year 年份
     * @return 配置列表
     */
    private List<HrmEmployeeAdditional> queryEmployeeAdditionalConfig(Long empId, int year) {
        LambdaQueryWrapper<HrmEmployeeAdditional> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HrmEmployeeAdditional::getYear, year);
        if (empId != null) {
            queryWrapper.eq(HrmEmployeeAdditional::getEmployeeId, empId);
        }
        return hrmEmployeeAdditionalService.list(queryWrapper);
    }
    
    /**
     * 查询当前月的专项附加扣除实际数据（hrm_additional表）
     * 
     * @param empId 员工ID（null表示查询全部）
     * @param year 年份
     * @param month 月份
     * @return 当前月数据列表
     */
    private List<HrmAdditional> queryCurrentMonthAdditional(Long empId, int year, int month) {
        LambdaQueryWrapper<HrmAdditional> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HrmAdditional::getYear, year)
                    .eq(HrmAdditional::getMonth, month);
        if (empId != null) {
            queryWrapper.eq(HrmAdditional::getEmployeeId, empId);
        }
        return hrmAdditionalService.list(queryWrapper);
    }
    
    /**
     * 删除下个月的专项附加扣除数据
     * 
     * @param empId 员工ID（null表示删除全部）
     * @param year 年份
     * @param month 月份
     */
    private void deleteNextMonthAdditional(Long empId, int year, int month) {
        LambdaQueryWrapper<HrmAdditional> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(HrmAdditional::getYear, year)
                     .eq(HrmAdditional::getMonth, month);
        if (empId != null) {
            deleteWrapper.eq(HrmAdditional::getEmployeeId, empId);
        }
        hrmAdditionalMapper.delete(deleteWrapper);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
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
     * 导出表头可选列表
     */

    public List<SalaryOptionHeadVO> querySalaryOptionHeadExport() {
        List<HrmSalaryOption> list = hrmSalaryOptionService.lambdaQuery()
                .select(HrmSalaryOption::getCode, HrmSalaryOption::getName, HrmSalaryOption::getIsFixed)
                .ne(HrmSalaryOption::getParentCode, 0)
                .orderByAsc(HrmSalaryOption::getCode).list();
        List<SalaryOptionHeadVO> optionHeadVOList = new LinkedList<>();
//        optionHeadVOList.add(new SalaryOptionHeadVO(1, "计薪天数", 1));
//        optionHeadVOList.add(new SalaryOptionHeadVO(2, "实际计薪天数", 1));
//        optionHeadVOList.add(new SalaryOptionHeadVO(1, "应出勤天数", 1));
        optionHeadVOList.add(new SalaryOptionHeadVO(2, "实际出勤天数", 1));
        List<SalaryOptionHeadVO> salaryOptionHeadVOList = TransferUtil.transferList(list, SalaryOptionHeadVO.class);
        optionHeadVOList.addAll(salaryOptionHeadVOList);

        return optionHeadVOList;
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
                .orderByAsc(HrmSalaryOption::getDisplayOrder).list();
        List<SalaryOptionHeadVO> optionHeadVOList = new LinkedList<>();
//        optionHeadVOList.add(new SalaryOptionHeadVO(1, "计薪天数", 1));
//        optionHeadVOList.add(new SalaryOptionHeadVO(2, "实际计薪天数", 1));
//        optionHeadVOList.add(new SalaryOptionHeadVO(1, "应出勤天数", 1));
        optionHeadVOList.add(new SalaryOptionHeadVO(2, "实际出勤天数", 1));
        List<SalaryOptionHeadVO> salaryOptionHeadVOList = TransferUtil.transferList(list, SalaryOptionHeadVO.class);
        optionHeadVOList.addAll(salaryOptionHeadVOList);
        List<Integer> attendList = Arrays.asList(180101, 190101, 190102, 190103, 19010401,19010402, 190105, 190106, 200101,40102,281,280,282,41001);
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
    public Map<String, Map<Integer, String>> resolveAttendanceData(List<Map<String, Object>> mapList) {
        List<Integer> defaultAttendanceCodes = Arrays.asList(
                1, 2,
                180101,
                190101, 190102, 190103, 19010401, 19010402, 190105, 190106,
                280, 281, 282,
                20102, 20105,
                40102
        );
        Map<String, Map<Integer, String>> jobNumberMap = new HashMap<>();
        if (CollUtil.isEmpty(mapList)) {
            return jobNumberMap;
        }
        for (Map<String, Object> map : mapList) {
            String jobNumber = map != null ? Convert.toStr(map.get("jobNumber"), "") : "";
            if (StrUtil.isBlank(jobNumber)) {
                continue;
            }
            Map<Integer, String> codeValueMap = new HashMap<>();
            for (Integer code : defaultAttendanceCodes) {
                codeValueMap.put(code, "0");
            }
            jobNumberMap.put(jobNumber, codeValueMap);
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
            if (code != 180101 && code != 180102 && code!=40102 && code != 281 && code != 280 && code != 282 && code != 20105 && code != 20102) {

                //除去加班工资项,夜班补贴，其他补贴，和满勤
                ////借款不放入考勤扣款合计
                //其他扣款(280)不放入扣款合计，有单独的工资项
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
        return getSocialSecurityOption(salaryMonthEmpRecord, isSyncInsuranceData, salaryConfig, null);
    }

    private List<HrmSalaryMonthOptionValue> getSocialSecurityOption(HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                                     Boolean isSyncInsuranceData,
                                                                     HrmSalaryConfig salaryConfig) {
        return getSocialSecurityOption(salaryMonthEmpRecord, isSyncInsuranceData, salaryConfig, null);
    }

    private List<HrmSalaryMonthOptionValue> getSocialSecurityOption(HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                                                     Boolean isSyncInsuranceData,
                                                                     HrmSalaryConfig salaryConfig,
                                                                     Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap) {
        Map<Integer, String> socialSecurityOptionMap = new HashMap<>();
        List<HrmSalaryMonthOptionValue> salaryMonthOptionValueList = new ArrayList<>();
        if (!Boolean.TRUE.equals(isSyncInsuranceData)) {
            List<HrmSalaryMonthOptionValue> socialSecurityOptions = salaryMonthOptionValueService.lambdaQuery().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(100101, 100102, 110101, 120101))
                    .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).list();
            if (CollUtil.isNotEmpty(socialSecurityOptions)) {
                return socialSecurityOptions;
            }
            socialSecurityOptionMap.put(100101, "0");
            socialSecurityOptionMap.put(100102, "0");
            socialSecurityOptionMap.put(110101, "0");
            socialSecurityOptionMap.put(120101, "0");
        } else {
            salaryMonthOptionValueService.lambdaUpdate().in(HrmSalaryMonthOptionValue::getCode, Arrays.asList(100101, 100102, 110101, 120101))
                    .eq(HrmSalaryMonthOptionValue::getSEmpRecordId, salaryMonthEmpRecord.getSEmpRecordId()).remove();
            Integer socialSecurityMonthType = salaryConfig != null ? salaryConfig.getSocialSecurityMonthType() : null;
            if (socialSecurityMonthType == null) {
                socialSecurityMonthType = ONE;
                logger.warn("薪资配置 socialSecurityMonthType 为空，员工{}社保按当月口径处理",
                        salaryMonthEmpRecord.getEmployeeId());
            }
            HrmInsuranceMonthEmpRecord insuranceMonthEmpRecord = socialSecurityEmpRecordMap != null
                    ? socialSecurityEmpRecordMap.get(salaryMonthEmpRecord.getEmployeeId())
                    : null;
            if (insuranceMonthEmpRecord == null) {
                YearMonth socialSecurityYearMonth = resolveSocialSecurityReferenceYearMonth(
                        socialSecurityMonthType,
                        salaryMonthEmpRecord.getYear(),
                        salaryMonthEmpRecord.getMonth());
                Optional<HrmInsuranceMonthEmpRecord> salaryMonthEmpRecordOpt = insuranceMonthEmpRecordService.lambdaQuery()
                        .eq(HrmInsuranceMonthEmpRecord::getYear, socialSecurityYearMonth.getYear())
                        .eq(HrmInsuranceMonthEmpRecord::getMonth, socialSecurityYearMonth.getMonthValue())
                        .eq(HrmInsuranceMonthEmpRecord::getEmployeeId, salaryMonthEmpRecord.getEmployeeId())
                        .eq(HrmInsuranceMonthEmpRecord::getStatus, IsEnum.YES.getValue())
                        .oneOpt();
                if (salaryMonthEmpRecordOpt.isPresent()) {
                    insuranceMonthEmpRecord = salaryMonthEmpRecordOpt.get();
                }
            }
            if (insuranceMonthEmpRecord != null) {
                String personalInsuranceAmountStr = insuranceMonthEmpRecord.getPersonalInsuranceAmount() == null
                        ? "0"
                        : insuranceMonthEmpRecord.getPersonalInsuranceAmount().toString();
                BigDecimal personalInsuranceAmount = new BigDecimal(personalInsuranceAmountStr);

                socialSecurityOptionMap.put(100101, personalInsuranceAmount.toString());
                socialSecurityOptionMap.put(100102, insuranceMonthEmpRecord.getPersonalProvidentFundAmount() == null
                        ? "0"
                        : insuranceMonthEmpRecord.getPersonalProvidentFundAmount().setScale(0, BigDecimal.ROUND_HALF_DOWN).toString());
                socialSecurityOptionMap.put(110101, insuranceMonthEmpRecord.getCorporateInsuranceAmount() == null
                        ? "0"
                        : insuranceMonthEmpRecord.getCorporateInsuranceAmount().toString());
                socialSecurityOptionMap.put(120101, insuranceMonthEmpRecord.getCorporateProvidentFundAmount() == null
                        ? "0"
                        : insuranceMonthEmpRecord.getCorporateProvidentFundAmount().toString());
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
                HrmEmployeeVO employee = hrmEmployeeMapper.getEmployeeById(salaryMonthEmpRecord.getEmployeeId());
                HrmEmployeeVO hrmEmployeeVO = new HrmEmployeeVO();
                hrmEmployeeVO.setEmployeeId(employee.getEmployeeId());
                hrmEmployeeVO.setStatus(employee.getStatus());
                hrmEmployeeVO.setJobNumber(employee.getJobNumber());
                //用到的时候要传入是否残疾人参数(最后一个)
                List<HrmSalaryMonthOptionValue> salaryMonthOptionValues = computeSalary(salaryMonthEmpRecord, null,employee,"2");
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
        HrmSalaryMonthRecord lastSalaryMonthRecord = lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("limit 1").one();
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
    private boolean checkIsFullAttendance(List<HrmAttendanceSummaryDayVo> summaryDayVoList,
                                          HrmAttendanceSummaryVo summaryVo,
                                          List<QueryHolidayDeductionVO> deductionVOList,
                                          Integer expandProduction)
    {
        //除了调休 和 年假，以及每日迟到15分钟以内的，都算缺勤
        boolean isFull = false;
        if(expandProduction == null || expandProduction.toString().equals("0"))
        {
            return true;
        }
        if(summaryVo==null)
        {
            //找不到考勤统计数据，则不为全勤
            return false;
        }
        //过滤出来员工的月度考勤统计数据
        //标识是否有迟到30分钟以上的考勤
        boolean isOverLate30Minute = false;
        //过滤出员工每日的考勤
        List<HrmAttendanceSummaryDayVo> employeeDayAttendanceList = summaryDayVoList != null ? summaryDayVoList : Collections.emptyList();


        //找出是否有迟到超过30分钟的考勤记录
        List<HrmAttendanceSummaryDayVo> late30MinuteList = employeeDayAttendanceList.stream().filter(f -> f.getLateMinute()>30).collect(Collectors.toList());
        if(CollectionUtil.isNotEmpty(late30MinuteList))
        {
            isOverLate30Minute =true;
            int totalLateMinute = late30MinuteList.stream().mapToInt(HrmAttendanceSummaryDayVo::getLateMinute).sum();
            if(CollectionUtil.isNotEmpty(deductionVOList))
            {
                //查询是否有抵扣 值
                QueryHolidayDeductionVO deductionVoChidao= deductionVOList.stream().filter(f -> f.getType()==2).findAny().orElse(null);
                if(deductionVoChidao!=null && deductionVoChidao.getDeductionTime().compareTo(new BigDecimal(totalLateMinute))>=0)
                {
                    if(deductionVoChidao.getDeductionTime().compareTo(new BigDecimal(totalLateMinute))>=0)
                    {
                        isOverLate30Minute =false;
                        summaryVo.setLateMinute(0);
                        summaryVo.setLateCount(0);
                    }
                    else
                    {
                        //不能全部抵扣的场景，则抵扣一部分
                        summaryVo.setLateMinute(totalLateMinute-deductionVoChidao.getDeductionTime().intValue());
                    }
                }
            }
        }

        if(CollectionUtil.isNotEmpty(deductionVOList))
        {
            //添加的 早退 抵扣时长
            QueryHolidayDeductionVO deductionVoZaotui= deductionVOList.stream().filter(f -> f.getType()==1).findAny().orElse(null);
            if(deductionVoZaotui!=null)
            {
                BigDecimal deductionTimeZaotui = deductionVoZaotui.getDeductionTime();//分钟
                //请假抵扣时长 早退
                BigDecimal needDiKouZaotui = new BigDecimal(summaryVo.getEarlyMinute());
                if(deductionTimeZaotui.compareTo(needDiKouZaotui)>=0)
                {
                    summaryVo.setEarlyMinute(0);
                    summaryVo.setEarlyCount(0);
                }
                else
                {
                    //只能抵扣一部分的场景
                    summaryVo.setEarlyMinute(needDiKouZaotui.intValue()-deductionTimeZaotui.intValue());
                }
            }
            //添加的 事假 抵扣时长
            QueryHolidayDeductionVO deductionVoShiJia= deductionVOList.stream().filter(f -> f.getType()==3).findAny().orElse(null);
            if(deductionVoShiJia!=null)
            {
                BigDecimal deductionTimeQingJia = deductionVoShiJia.getDeductionTime();//分钟
                //请假抵扣时长 事假
                BigDecimal needDiKouShiJia = new BigDecimal(summaryVo.getShijia()).setScale(2, RoundingMode.HALF_UP);
//                needDiKouShiJia = needDiKouShiJia.multiply(new BigDecimal(60));
                if(deductionTimeQingJia.compareTo(needDiKouShiJia)>=0)
                {
                    //如果添加的 请假时长大于 本月发生的事假时长，则修改本月事假时长为0
                    summaryVo.setShijia(0d);
                }
                else
                {
                    //只能抵扣一部分的场景  (换回小时)
                    double shijiaHour = (needDiKouShiJia.subtract(deductionTimeQingJia)).divide(new BigDecimal(60)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    summaryVo.setShijia(shijiaHour);
                }
            }

            //病假抵扣
            QueryHolidayDeductionVO deductionVoBingJia= deductionVOList.stream().filter(f -> f.getType()==4).findAny().orElse(null);
            if(deductionVoBingJia!=null)
            {
                BigDecimal deductionTimeBingJia = deductionVoBingJia.getDeductionTime();//分钟
                //请假抵扣时长 病假
                BigDecimal needDiKouBingJia = new BigDecimal(summaryVo.getBingjia()).setScale(2, RoundingMode.HALF_UP);
//                needDiKouBingJia = needDiKouBingJia.multiply(new BigDecimal(60));
                if(deductionTimeBingJia.compareTo(needDiKouBingJia)>=0)
                {
                    //如果添加的 病假时长大于 本月发生的病假时长，则修改本月病假时长为0
                    summaryVo.setBingjia(0d);
                }
                else
                {
                    //只能抵扣一部分的场景 (换回小时)
                    double bingJiaHour = (needDiKouBingJia.subtract(deductionTimeBingJia)).divide(new BigDecimal(60)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    summaryVo.setBingjia(bingJiaHour);
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
                else
                {
                    //只能抵扣一部分的场景 (换回小时)
                    double tiaoXiuHour = (needDiKouTiaoxiu.subtract(deductionTimeTiaoxiu)).divide(new BigDecimal(60)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    summaryVo.setTiaoxiu(tiaoXiuHour);
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
                else
                {
                    //只能抵扣一部分的场景
                    summaryVo.setMisscardCount(summaryVo.getMisscardCount().intValue()-deductionCountBuKa.intValue());
                }
            }


            //旷工抵扣
            QueryHolidayDeductionVO deductionVoKuanggong= deductionVOList.stream().filter(f -> f.getType()==8).findAny().orElse(null);
            if(deductionVoKuanggong!=null)
            {
                BigDecimal deductionCountKuangong = deductionVoKuanggong.getDeductionTime();
                //旷工抵扣 抵扣
                summaryVo.setAbsenteeismDays(summaryVo.getAbsenteeismDays()-deductionCountKuangong.doubleValue());
            }

            //婚假抵扣
            QueryHolidayDeductionVO deductionVoHunjia= deductionVOList.stream().filter(f -> f.getType()==9).findAny().orElse(null);
            if(deductionVoHunjia!=null)
            {
                BigDecimal deductionCountHunjia = deductionVoHunjia.getDeductionTime();
                //婚假抵扣 抵扣
                summaryVo.setAbsenteeismDays(summaryVo.getHunjia()-deductionCountHunjia.doubleValue());
                summaryVo.setHunjia(0);
            }

            //丧假抵扣
            QueryHolidayDeductionVO deductionVoSangjia= deductionVOList.stream().filter(f -> f.getType()==10).findAny().orElse(null);
            if(deductionVoSangjia!=null)
            {
                BigDecimal deductionCountSangjia = deductionVoSangjia.getDeductionTime();
                //丧假抵扣 抵扣
                summaryVo.setAbsenteeismDays(summaryVo.getSangjia()-deductionCountSangjia.doubleValue());
                summaryVo.setSangjia(0);
            }

            //产假抵扣
            QueryHolidayDeductionVO deductionVoChanjia= deductionVOList.stream().filter(f -> f.getType()==11).findAny().orElse(null);
            if(deductionVoChanjia!=null)
            {
                BigDecimal deductionCountChanjia = deductionVoChanjia.getDeductionTime();
                //产假抵扣 抵扣
                summaryVo.setAbsenteeismDays(summaryVo.getChanjia()-deductionCountChanjia.doubleValue());
                summaryVo.setChanjia(0);
            }

            //陪产假抵扣
            QueryHolidayDeductionVO deductionVoPeiChanjia= deductionVOList.stream().filter(f -> f.getType()==12).findAny().orElse(null);
            if(deductionVoPeiChanjia!=null)
            {
                BigDecimal deductionCountPeiChanjia = deductionVoPeiChanjia.getDeductionTime();
                //陪产假抵扣 抵扣
                summaryVo.setAbsenteeismDays(summaryVo.getPeichanjia()-deductionCountPeiChanjia.doubleValue());
                summaryVo.setPeichanjia(0);
            }
        }
        //如果缺卡次数=0 && 旷工天数=0 &产假=0 & 婚假=0 & 陪产假=0 &丧假=0 &事假=0 & 哺乳假=0 & 病假=0 &早退=0 &调休=0 & 早退=0 并且没有迟到超过30分钟以上的考勤记录，则判断为全勤
        if((0==summaryVo.getMisscardCount() && 0==summaryVo.getAbsenteeismDays() &&  0==summaryVo.getAbsenteeismDays()
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
     * 从批量预加载的排班数据中获取员工当月排班时长，避免循环内逐条查询DB
     */
    private HashMap<String,Double> getWorkHoursFromBatch(Long employeeId, boolean isProduce, List<String> dates,
                                                          Map<Long, Map<String, Double>> batchWorkHoursMap) {
        HashMap<String,Double> workTimesMap = new HashMap<>();
        if (!isProduce) {
            for (String date : dates) {
                workTimesMap.put(date, 8d);
            }
            return workTimesMap;
        }
        Map<String, Double> empShiftMap = batchWorkHoursMap.getOrDefault(employeeId, Collections.emptyMap());
        for (String date : dates) {
            Double hours = empShiftMap.get(date);
            workTimesMap.put(date, hours != null ? hours : 0d);
        }
        return workTimesMap;
    }

    /**
     * 员工病假,事假天数计算
     * @param isProduce
     * @param summaryDayVoList
     * type  1事假 2病假
     * @return
     */
    private BigDecimal sickDeductDays(boolean isProduce, List<HrmAttendanceSummaryDayVo> summaryDayVoList,
                                      HashMap<String,Double> workTimesMap, String type,
                                      List<QueryHolidayDeductionVO> deductionVOList)
    {
        List<HrmAttendanceSummaryDayVo> employeeDayAttendanceList = summaryDayVoList != null ? summaryDayVoList : Collections.emptyList();
        //请假天数
        BigDecimal leavelHours = new BigDecimal(0);
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
//                        if(workHours!=0d && summaryDayVo.getBingjia()!=0d)
//                        {
//                            if(summaryDayVo.getBingjia()!=null && summaryDayVo.getBingjia()<=workHours/2)
//                            {
//                                //如果请假时长小于等于当日排班时长的一半，则按半天计算
//                                leavelDays = leavelDays.add(new BigDecimal(0.5));
//                            }
//                            else if(summaryDayVo.getBingjia()!=null && summaryDayVo.getBingjia()>workHours/2)
//                            {
//                                //如果请假时大于当日排班时长的一半，则按一天计算
//                                leavelDays = leavelDays.add(new BigDecimal(1));
//                            }
//                        }
                        if (summaryDayVo.getBingjia() != 0d) {
                            leavelHours = leavelHours.add(new BigDecimal(summaryDayVo.getBingjia())).setScale(0, RoundingMode.HALF_UP);
                            //换算成天,钉钉里面是小时
                            leavelDays = leavelHours.divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
                        }
                    }
                    else if("1".equals(type))
                    {
//                        if(workHours!=0d && summaryDayVo.getShijia()!=0d)
//                        {
//                            if(summaryDayVo.getShijia()!=null && summaryDayVo.getShijia()<=workHours/2)
//                            {
//                                //如果请假时长小于等于当日排班时长的一半，则按半天计算
//                                leavelDays = leavelDays.add(new BigDecimal(0.5));
//                            }
//                            else if(summaryDayVo.getShijia()!=null && summaryDayVo.getShijia()>workHours/2)
//                            {
//                                //如果请假时大于当日排班时长的一半，则按一天计算
//                                leavelDays = leavelDays.add(new BigDecimal(1));
//                            }
//                        }
                        if (summaryDayVo.getShijia() != 0d) {
                            leavelHours = leavelHours.add(new BigDecimal(summaryDayVo.getShijia())).setScale(0, RoundingMode.HALF_UP);
                            //换算成天,钉钉里面是小时
                            leavelDays = leavelHours.divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
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
//                leavelDays = new BigDecimal(sickHours).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
                leavelDays = new BigDecimal(sickHours).setScale(2, RoundingMode.HALF_UP);
            }
            else if("1".equals(type))
            {
                //获取员工对应的事假时间
                Double shiJiaHours = employeeDayAttendanceList.stream().mapToDouble(HrmAttendanceSummaryDayVo::getShijia).sum();
                //非生产部门，算出对应的天数,按一天8小时计算
                leavelDays = new BigDecimal(shiJiaHours).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
//                leavelDays = new BigDecimal(shiJiaHours).setScale(2, RoundingMode.HALF_UP);
            }

        }
        if(CollectionUtil.isNotEmpty(deductionVOList))
        {
            if("1".equals(type))
            {
                //添加的 事假 抵扣时长
                QueryHolidayDeductionVO deductionVoShiJia= deductionVOList.stream().filter(f -> f.getType()==3).findAny().orElse(null);
                if(deductionVoShiJia!=null)
                {
                    BigDecimal deductionTimeQingJia = deductionVoShiJia.getDeductionTime();//分钟
                    //请假抵扣时长 事假
//                    BigDecimal needDiKouShiJia = leavelDays.multiply(new BigDecimal(8));
//                    needDiKouShiJia = needDiKouShiJia.multiply(new BigDecimal(60));
                    BigDecimal needDiKouShiJia = leavelDays;
                    if(deductionTimeQingJia.compareTo(needDiKouShiJia)>=0)
                    {
                        //如果添加的 请假时长大于 本月发生的事假时长，则修改本月事假时长为0
                        leavelDays=new BigDecimal(0);
                    }
                    else
                    {
                        //只能抵扣一部分的场景  (换回小时)
                        double shijiaHour = (needDiKouShiJia.subtract(deductionTimeQingJia)).divide(new BigDecimal(60)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                        leavelDays =new BigDecimal(shijiaHour).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
            if("2".equals(type))
            {
                //病假抵扣
                QueryHolidayDeductionVO deductionVoBingJia= deductionVOList.stream().filter(f -> f.getType()==4).findAny().orElse(null);
                if(deductionVoBingJia!=null)
                {
                    BigDecimal deductionTimeBingJia = deductionVoBingJia.getDeductionTime();//分钟
                    //请假抵扣时长 病假
//                    BigDecimal needDiKouBingJia = leavelDays.multiply(new BigDecimal(8));
//                    needDiKouBingJia = needDiKouBingJia.multiply(new BigDecimal(60));
                    BigDecimal needDiKouBingJia = leavelDays;
                    if(deductionTimeBingJia.compareTo(needDiKouBingJia)>=0)
                    {
                        //如果添加的 病假时长大于 本月发生的病假时长，则修改本月病假时长为0
                        leavelDays=new BigDecimal(0);
                    }
                    else
                    {
                        //只能抵扣一部分的场景 (换回小时)
                        double bingJiaHour = (needDiKouBingJia.subtract(deductionTimeBingJia)).divide(new BigDecimal(60)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                        leavelDays =new BigDecimal(bingJiaHour).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
                    }
                }
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
        Double normalDays =0d;
        if(empAttendanceSummary.getEmployeeId().toString().equals("1712718940327") || empAttendanceSummary.getEmployeeId().toString().equals("1712718940184") || !isProduce)
        {
            //非生产类型部门，请假小时换算为天数时，一天按8小时计算
            Double shijia = 0d;
            if(empAttendanceSummary.getShijia()!=null && empAttendanceSummary.getShijia()!=0)
            {
                //事假 小时->天
                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getShijia());
                shijia = b1.divide(new BigDecimal(8)).setScale(3, RoundingMode.HALF_UP).doubleValue();
                if(shijia>0.5d && shijia<=1d)
                {
                    shijia=1d;
                }
                empAttendanceSummary.setShijia(shijia);
            }
            Double bingjia = 0d;
            if(empAttendanceSummary.getBingjia()!=null && empAttendanceSummary.getBingjia()!=0)
            {
                //病假 小时->天
                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getBingjia());
                bingjia =b1.divide(new BigDecimal(8)).setScale(3, RoundingMode.HALF_UP).doubleValue();
                if(bingjia>0.5d && bingjia<=1d)
                {
                    bingjia=1d;
                }
                if(bingjia<=2)
                {
                    //如果病假<=2天，则不计算
                    empAttendanceSummary.setBingjia(0d);
                }
                else
                {
                    //病假有两天不用扣工资
                    empAttendanceSummary.setBingjia(bingjia);
                }
            }
            Double burujia = 0d;
            if(empAttendanceSummary.getBurujia()!=null && empAttendanceSummary.getBurujia()!=0)
            {
                //哺乳假 小时->天
                BigDecimal b1 = new BigDecimal(empAttendanceSummary.getBurujia());
                burujia = b1.divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                if(burujia>0.5d && burujia<=1d)
                {
                    burujia=1d;
                }
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

    public void exportSalaryNew(QuerySalaryExportDto querySalaryExportDto, HttpServletResponse response) throws IOException
    {
        LoginUserInfo Info = CompanyContext.get();
        HrmSalaryMonthRecord salaryMonthRecord = getById(querySalaryExportDto.getSalaryRecordId());
        int month = salaryMonthRecord.getMonth();
        int year = salaryMonthRecord.getYear();

        LocalDate dateStartTime = DateUtil.beginOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();
        LocalDate dateEndTime = DateUtil.endOfMonth(DateUtil.parse(year + "-" + month, "yy-MM")).toLocalDateTime().toLocalDate();

        LocalDateTime startDateTime = dateStartTime.atStartOfDay();
        LocalDateTime endDateTime = LocalDateTimeUtil.endOfDay(dateEndTime.atStartOfDay());

        Date beginDate = Date.from(startDateTime.atZone( ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone( ZoneId.systemDefault()).toInstant());
        //这是查询时间范围内的 考勤情况参数
        HashMap<String,Object> params = new HashMap<>();
        params.put("beginDate",beginDate);
        params.put("endDate",endDate);

        //每月 员工考勤统计数据
        List<HrmAttendanceSummaryVo> attendanceSummaryVoList = attendanceReportDataService.getEmpAttendanceSummaryList(params);

        params.put("year",year);
        params.put("month",month);
        //获取 员工 加班费,夜班补贴数据 以及其他扣款，补贴数据(从导入的数据里面取)
        List<HrmProduceAttendance> hasOverTimePayEmpList = produceAttendanceMapper.getOvertimeAllowanceStatistics(params);

        List<SalaryOptionHeadVO> headVOList = querySalaryOptionHeadExport();
        headVOList.add(0,new SalaryOptionHeadVO(9000,"序号",1));
        headVOList.add(1,new SalaryOptionHeadVO(9001,"月份",1));
        headVOList.add(2,new SalaryOptionHeadVO(9002,"姓名",1));
        headVOList.add(3,new SalaryOptionHeadVO(9003,"性别",1));
        headVOList.add(4,new SalaryOptionHeadVO(9004,"入职时间",1));
        headVOList.add(5,new SalaryOptionHeadVO(9005,"部门",1));
        headVOList.add(6,new SalaryOptionHeadVO(9006,"岗位",1));
        headVOList.add(7,new SalaryOptionHeadVO(9007,"满勤天数",1));
        headVOList.add(8,new SalaryOptionHeadVO(9008,"超缺勤天数",1));
        headVOList.add(9,new SalaryOptionHeadVO(9009,"加班工时",1));

        List<List<String>> headTitles = Lists.newArrayList();


        for(int i=0;i<=9;i++)
        {
            //Lists.newArrayList("", "序号") 这里表示表头为两行
            headTitles.add(Lists.newArrayList(headVOList.get(i).getName()));
        }
        // 应发工资部分 - 按固定顺序添加
        // 基本工资
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 10101==f.getCode()).findAny().orElse(null).getName()));
        // 岗位工资
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 10102==f.getCode()).findAny().orElse(null).getName()));

        // 根据公司ID动态判断第12列显示内容
        // companyId=0002时显示绩效工资(code=41001)，其他公司显示职务补助(code=10103)
        if ("0002".equals(Info.getCompanyId())) {
            // 0002公司：第12列显示绩效工资
            headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 41001 == f.getCode()).findAny().orElse(new SalaryOptionHeadVO(41001, "绩效工资", 1)).getName()));
        } else {
            // 其他公司：第12列显示职务补助
            headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 10103 == f.getCode()).findAny().orElse(new SalaryOptionHeadVO(10103, "职务补助", 1)).getName()));
        }

        // 高温补贴
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 20102==f.getCode()).findAny().orElse(null).getName()));
        // 低温补贴
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 20105==f.getCode()).findAny().orElse(null).getName()));
        // 夜班津贴
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 180102==f.getCode()).findAny().orElse(null).getName()));
        // 其他补贴
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 281==f.getCode()).findAny().orElse(null).getName()));
        // 满勤补贴
        headTitles.add(Lists.newArrayList("应发工资", headVOList.stream().filter(f -> 40102==f.getCode()).findAny().orElse(null).getName()));

        // 加班工资部分
        // 加班工资
        headTitles.add(Lists.newArrayList("加班工资", headVOList.stream().filter(f -> 180101==f.getCode()).findAny().orElse(null).getName()));
        // 超缺勤工资
        headTitles.add(Lists.newArrayList("加班工资", headVOList.stream().filter(f -> 200101==f.getCode()).findAny().orElse(null).getName()));

        // 合计
        headTitles.add(Lists.newArrayList("合计"));

        // 代扣部分
        // 个人所得税
        headTitles.add(Lists.newArrayList("代扣", headVOList.stream().filter(f -> 230101==f.getCode()).findAny().orElse(null).getName()));
        // 个人社保
        headTitles.add(Lists.newArrayList("代扣", headVOList.stream().filter(f -> 100101==f.getCode()).findAny().orElse(null).getName()));
        // 个人公积金
        headTitles.add(Lists.newArrayList("代扣", headVOList.stream().filter(f -> 100102==f.getCode()).findAny().orElse(null).getName()));
        // 其他扣款
        headTitles.add(Lists.newArrayList("代扣", headVOList.stream().filter(f -> 280==f.getCode()).findAny().orElse(null).getName()));
        // 工会费
        headTitles.add(Lists.newArrayList("代扣", headVOList.stream().filter(f -> 160102==f.getCode()).findAny().orElse(null).getName()));

        // 实发工资
        headTitles.add(Lists.newArrayList("实发工资"));

        // 签领人
        headTitles.add(Lists.newArrayList("签领人"));



        // 使用 Map 结构存储每行数据，以字段名为键，消除硬编码索引
        List<Map<String, String>> dataMapList = new ArrayList<>();
        List<List<String>> dataList = new ArrayList<>();

        List<Long> employeeIds = new ArrayList<>();
        //查询出已经定薪了的人员列表
        LocalDate nows = LocalDate.now().withDayOfMonth(1);
        employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).lt(HrmEmployee::getEntryTime, nows).ne(HrmEmployee::getIsDel, 1).orderByDesc(HrmEmployee::getDeptId).list()
                .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));
        //填充数据
        QuerySalaryPageListDto querySalaryPageListDto = new QuerySalaryPageListDto();
        querySalaryPageListDto.setSRecordId(querySalaryExportDto.getSalaryRecordId());
        BeanUtils.copyProperties(querySalaryExportDto,querySalaryPageListDto);
        List<QuerySalaryPageListVO> salaryPageListVOS = salaryMonthEmpRecordMapper.querySalaryMonthList(querySalaryPageListDto,employeeIds);
        if (!CollectionUtil.isEmpty(salaryPageListVOS)) {
            for(QuerySalaryPageListVO vo : salaryPageListVOS) {
                boolean isProduce = vo.getIsProduceDept().equals("0")?false:true;
                //应出勤天数    获取每月的考勤信息(应出勤天数等)
                HashMap<String,Object> infoParams = new HashMap<>();
                infoParams.put("year",year);
                infoParams.put("month",month);

                //符兴凯等两个员工按行政部门应出勤天数来算
                if(!isProduce) {
                    if (vo.getEmployeeId().toString().equals("1712718940198") || vo.getEmployeeId().toString().equals("1712718940199") ||
                            vo.getEmployeeId().toString().equals("1712718940200")) {
                        //吴雪芳等人的出勤天数由行政体系改为生产体系
                        infoParams.put("deptType", 1);
                    }else {
                        //行政体系
                        infoParams.put("deptType", 0);
                    }
                }else if (isProduce){
                    if (vo.getEmployeeId().toString().equals("1712718940227") || vo.getEmployeeId().toString().equals("1831601326890434591") ||
                            vo.getEmployeeId().toString().equals("1831601326890434610") || vo.getEmployeeId().toString().equals("1831601326890434648")) {
                        //李彩平等人的出勤天数由生产部改为行政部出勤天数
                        infoParams.put("deptType", 0);
                    }else {
                        //生产体系
                        infoParams.put("deptType", 1);
                    }
                }
                List<HrmAttendanceInfo> attendanceInfoList = hrmAttendanceInfoMapper.queryInfo(infoParams);
                //应出勤天数设置对象
                HrmAttendanceInfo hrmAllDayInfo = attendanceInfoList.stream().findAny().orElse(null);
                //获取应出勤天数
                Double normalDays = Double.parseDouble(hrmAllDayInfo.getActualWorkDay());

                // 加班工时
                BigDecimal overTimeHours =new BigDecimal(0);
                //获取到钉钉上的数据
                Optional<HrmAttendanceSummaryVo> empAttendanceSummaryOption = attendanceSummaryVoList.stream().filter(f -> f.getEmployeeId().toString().equals(vo.getEmployeeId().toString())).findFirst();
                if(empAttendanceSummaryOption.isPresent()) {
                    HrmAttendanceSummaryVo empAttendanceSummary = empAttendanceSummaryOption.get();
                    overTimeHours = new BigDecimal(empAttendanceSummary.getOverTimeHours());
                }
                HrmProduceAttendance hrmProduceAttendance = hasOverTimePayEmpList.stream().filter(f -> f.getEmployeeId()!=null && f.getEmployeeId().toString().equals(vo.getEmployeeId().toString())).findAny().orElse(null);
                if(hrmProduceAttendance!=null) {
                    if(hrmProduceAttendance.getWorkOverTime()!=null && hrmProduceAttendance.getWorkOverTime().compareTo(new BigDecimal(0))>0) {
                        overTimeHours = new BigDecimal(hrmProduceAttendance.getWorkOverTime().doubleValue());
                    }
                }

                vo.setMonth(year+"年"+month+"月");
                List<ComputeSalaryDto> list = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(vo.getSEmpRecordId());
                List<QuerySalaryPageListVO.SalaryValue> salaryValues = TransferUtil.transferList(list, QuerySalaryPageListVO.SalaryValue.class);

                // 如果是0002公司，从 hrm_bonus 表查询奖金数据
                if ("0002".equals(Info.getCompanyId())) {
                    // 先移除可能存在的旧的绩效工资数据（code=41001）
                    salaryValues.removeIf(sv -> sv.getCode() != null && sv.getCode() == 41001);

                    // 从 hrm_bonus 表查询绩效工资
                    String bonusValue = salaryMonthOptionValueService.getBounsBySEmpRecordId(vo.getSEmpRecordId(), 41001);

                    // 确保绩效工资数据存在，即使为空也添加默认值"0"
                    if (bonusValue != null && !bonusValue.trim().isEmpty()) {
                        // 添加从 hrm_bonus 表查询到的绩效工资
                        salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 41001, bonusValue, 1, "绩效工资"));
                        logger.info("员工 {} (ID:{}) 绩效工资: {}", vo.getEmployeeName(), vo.getEmployeeId(), bonusValue);
                    } else {
                        // 没有数据时添加默认值"0"
                        salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 41001, "0", 1, "绩效工资"));
                        logger.warn("员工 {} (ID:{}) 未找到绩效工资数据，使用默认值0", vo.getEmployeeName(), vo.getEmployeeId());
                    }
                }

                // 添加基础字段到 salaryValues
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9000, vo.getXh().toString(), 1, "序号"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9001, vo.getMonth().toString(), 1, "月份"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9002, vo.getEmployeeName().toString(), 1, "姓名"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9003, vo.getSex().toString(), 1, "性别"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9004, vo.getEntryTime().toString(), 1, "入职时间"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9005, vo.getDeptName().toString(), 1, "部门"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9006, vo.getPost()!=null?vo.getPost():"", 1, "岗位"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9007, vo.getNeedWorkDay().toString(), 1, "满勤天数"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9008, (new BigDecimal(normalDays).subtract(vo.getActualWorkDay())).toString(), 1, "超缺勤天数"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9009, overTimeHours.toString(), 1, "加班工时"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9010, "", 1, "签领人"));
                // 个税显示保留两位小数
                QuerySalaryPageListVO.SalaryValue taxSalaryValue = salaryValues.stream().filter(f -> f.getCode()!=null && f.getCode()==230101).findAny().orElse(null);
                if(taxSalaryValue!=null && StringUtils.isNotBlank(taxSalaryValue.getValue())) {
                    taxSalaryValue.setValue(new BigDecimal(taxSalaryValue.getValue()).setScale(2,BigDecimal.ROUND_HALF_UP).toString());
                }
                vo.setSalary(salaryValues);
                // 构建 Map 结构的数据行，以字段名为键
                Map<String, String> dataRow = buildSalaryDataRow(vo, Info.getCompanyId());
                dataMapList.add(dataRow);
            }
        }

        // 严格按表头顺序组装每一行数据，避免列错乱
        if (!CollectionUtil.isEmpty(dataMapList)) {
            // 构造字段顺序列表，顺序与headTitles一致
            List<String> fieldOrder = new ArrayList<>();

            // 前10列固定字段
            fieldOrder.add("序号");
            fieldOrder.add("月份");
            fieldOrder.add("姓名");
            fieldOrder.add("性别");
            fieldOrder.add("入职时间");
            fieldOrder.add("部门");
            fieldOrder.add("岗位");
            fieldOrder.add("满勤天数");
            fieldOrder.add("超缺勤天数");
            fieldOrder.add("加班工时");

            // 工资项字段（索引10-19）- 必须与 buildSalaryDataRow 中的键名完全一致
            fieldOrder.add("基本工资");
            fieldOrder.add("岗位工资");
            fieldOrder.add("绩效工资");  // 注意：这里统一使用"绩效工资"作为键名
            fieldOrder.add("高温津贴");
            fieldOrder.add("低温津贴");
            fieldOrder.add("夜班补贴");
            fieldOrder.add("其他补贴");
            fieldOrder.add("全勤奖");
            fieldOrder.add("加班工资");
            fieldOrder.add("超缺勤");

            // 合计及代扣字段（索引20-26）
            fieldOrder.add("合计");
            fieldOrder.add("个人所得税");
            fieldOrder.add("个人社保");
            fieldOrder.add("个人公积金");
            fieldOrder.add("其他扣款");
            fieldOrder.add("工会费");
            fieldOrder.add("实发工资");
            fieldOrder.add("签领人");

            // 调试日志：输出第一行数据的所有键
            if (!dataMapList.isEmpty()) {
                Map<String, String> firstRow = dataMapList.get(0);

                // 检查绩效工资数据
                String performanceValue = firstRow.get("绩效工资");
            }

            // 按 fieldOrder 顺序从 dataMapList 提取数据
            for (Map<String, String> row : dataMapList) {
                List<String> rowList = new ArrayList<>();
                for (String key : fieldOrder) {
                    String value = row.getOrDefault(key, "");
                    rowList.add(value);
                }
                dataList.add(rowList);
            }
        }

        // 合计列已经在上面直接使用 code=210101（应发工资）的值，不需要再手动计算

        //将数据先填充到 导出表
        initExportData(dataList,year,month);


        List<HrmSalaryExport> salaryExportList = exportMapper.queryExportData(year,month);

        if(!CollectionUtil.isEmpty(salaryExportList))
        {
            //增加合计行s
            HrmSalaryExport total = new HrmSalaryExport();
            total.setEmpname("合计");
            total.setAbsencehours(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getAbsencehours).reduce(BigDecimal.ZERO, BigDecimal::add));
            total.setOvertime(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getOvertime).reduce(BigDecimal.ZERO, BigDecimal::add));
            total.setBasicsalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getBasicsalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //岗位工资
            total.setPostsalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getPostsalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            total.setPerformance(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getPerformance).reduce(BigDecimal.ZERO, BigDecimal::add));
            //职务工资
            total.setDutiessalary(salaryExportList.stream().filter(v -> v.getEmpname().equals("小计")).map(HrmSalaryExport::getDutiessalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //高温津贴
            total.setHightempsalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getHightempsalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //低温津贴
            total.setLowtempsalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getLowtempsalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //夜班补贴
            total.setNightshiftsalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getNightshiftsalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //其他补贴
            total.setOthersalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getOthersalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //全勤奖
            total.setFullattendancesalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getFullattendancesalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //加班工资
            total.setOvertimesalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getOvertimesalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //超缺勤工资
            total.setAbsencesalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getAbsencesalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //合计工资
            total.setTotalsalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getTotalsalary).reduce(BigDecimal.ZERO, BigDecimal::add));
            //个人所得税
            total.setTax(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getTax).reduce(BigDecimal.ZERO, BigDecimal::add));
            //个人社保
            total.setSocial(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getSocial).reduce(BigDecimal.ZERO, BigDecimal::add));
            //个人公积金
            total.setAccumulation(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getAccumulation).reduce(BigDecimal.ZERO, BigDecimal::add));
            //其他扣款
            total.setOtherdeduction(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getOtherdeduction).reduce(BigDecimal.ZERO, BigDecimal::add));
            //工会费
            total.setUnionfees(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getUnionfees).reduce(BigDecimal.ZERO, BigDecimal::add));
            //实发工资
            total.setActualitysalary(salaryExportList.stream().filter(v->v.getEmpname().equals("小计")).map(HrmSalaryExport::getActualitysalary).reduce(BigDecimal.ZERO, BigDecimal::add));

            salaryExportList.add(salaryExportList.size(),total);
        }

        List<Integer> xjIndexList = new ArrayList<>();
        //找出salaryExportList 中小计行
        for(int index=0;index<salaryExportList.size();index++)
        {
            HrmSalaryExport export = salaryExportList.get(index);
            if("小计".equals(export.getEmpname()) || "合计".equals(export.getEmpname()))
            {
                xjIndexList.add(index);
            }
        }

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = sdf.format(now);

        //内容样式策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        //垂直居中,水平居中
//        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THICK);
        contentWriteCellStyle.setBorderTop(BorderStyle.THICK);
        contentWriteCellStyle.setBorderRight(BorderStyle.THICK);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THICK);
        //设置 自动换行
        contentWriteCellStyle.setWrapped(false);

        //contentWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
//        contentWriteCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 字体策略
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 10);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        //头策略使用默认 设置字体大小
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 10);
        headWriteCellStyle.setWriteFont(headWriteFont);


        LoginUserInfo info = CompanyContext.get();
        String fileName = info.getDepName() + month + "月份工资.xlsx";
        fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);
        // 需要写入文件夹就是用 DileOutPutStram
        // 如果是接口请求直接浏览器下载，就使用 response.getOutputStream()
        InputStream is;
        if (Info.getCompanyId().equals("0002")) {
            is = SalaryComputeServiceNew.class.getResourceAsStream("/export/salary_export_cd.xlsx");
        }else {
            is = SalaryComputeServiceNew.class.getResourceAsStream("/export/salary_export.xlsx");
        }
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).withTemplate(is).build();
        WriteSheet writeSheet = EasyExcel.
                writerSheet("Sheet1")
//                .registerWriteHandler(new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle))
//                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .registerWriteHandler(new CustomCellWriteHandler(xjIndexList))
//                  .registerWriteHandler(new CustomCellWriteHeightConfig())
                .build();
        //writeSheet.setRelativeHeadRowIndex(1);

        FillConfig fillConfig = FillConfig.builder()
                // 开启填充换行
                .forceNewRow(true)
                .build();

//        writeSheet.setHead(headTitles);
        //excelWriter.write(salaryExportList, writeSheet);
        SalaryExportHeader headInfoVo = new SalaryExportHeader();
        companyName = info.getDepName();
        headInfoVo.setTitle(companyName+""+year+"年"+month+"月工资表");
        headInfoVo.setSalaryMonth(year+"年"+month+"月");
        headInfoVo.setCompanyName(companyName);
        excelWriter.fill(new FillWrapper("header", Collections.singletonList(headInfoVo)), writeSheet);
        excelWriter.fill(salaryExportList, fillConfig, writeSheet);
        excelWriter.finish();

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
        employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).ne(HrmEmployee::getIsDel, 1).orderByDesc(HrmEmployee::getDeptId).list()
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

            //个税显示保留两位小数
            QuerySalaryPageListVO.SalaryValue taxSalaryValue = salaryValues.stream().filter(f -> f.getCode()!=null && f.getCode()==230101).findAny().orElse(null);
            if(taxSalaryValue!=null && StringUtils.isNotBlank(taxSalaryValue.getValue()))
            {
                taxSalaryValue.setValue(new BigDecimal(taxSalaryValue.getValue()).setScale(2,BigDecimal.ROUND_HALF_UP).toString());
            }
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
                .registerWriteHandler(new RwhzCustemhandler())
                .head(headLists)
                .sheet()
                .doWrite(dataList);

    }


    /**
     * 初始化导出数据
     * @param dataList 数据列表
     * @param year 年份
     * @param month 月份
     */
    private void initExportData(List<List<String>> dataList, int year, int month) {
        LoginUserInfo Info = CompanyContext.get();
        // 先删除原有的数据
        exportMapper.deleteByYears(year, month);
        List<HrmSalaryExport> saveList = new ArrayList<>();

        for (List<String> dataRow : dataList) {
            HrmSalaryExport export = new HrmSalaryExport();

            // 基础信息字段（索引 0-9）
            export.setXh(dataRow.get(0));
            export.setYear(year);
            export.setMonth(month);
            export.setYears(dataRow.get(1));
            export.setEmpname(dataRow.get(2));
            export.setSex(dataRow.get(3));
            export.setEntrytime(dataRow.get(4));
            export.setDept(dataRow.get(5));
            export.setPost(dataRow.get(6));
            export.setNormaldays(dataRow.get(7));
            export.setAbsencehours(parseBigDecimal(dataRow.get(8)));
            export.setOvertime(parseBigDecimal(dataRow.get(9)));

            // 工资项字段（索引 10-19）
            export.setBasicsalary(parseBigDecimal(dataRow.get(10)));      // 基本工资
            export.setPostsalary(parseBigDecimal(dataRow.get(11)));       // 岗位工资

            // 第12列：根据公司ID，0002是绩效工资，其他是职务补助
            if ("0002".equals(Info.getCompanyId())) {
                export.setPerformance(parseBigDecimal(dataRow.get(12)));  // 绩效工资
                export.setDutiessalary(BigDecimal.ZERO);                  // 职务补助设为0
            } else {
                export.setDutiessalary(parseBigDecimal(dataRow.get(12))); // 职务补助
                export.setPerformance(BigDecimal.ZERO);                   // 绩效工资设为0
            }

            export.setHightempsalary(parseBigDecimal(dataRow.get(13)));   // 高温津贴
            export.setLowtempsalary(parseBigDecimal(dataRow.get(14)));    // 低温津贴
            export.setNightshiftsalary(parseBigDecimal(dataRow.get(15))); // 夜班补贴
            export.setOthersalary(parseBigDecimal(dataRow.get(16)));      // 其他补贴
            export.setFullattendancesalary(parseBigDecimal(dataRow.get(17))); // 全勤奖
            export.setOvertimesalary(parseBigDecimal(dataRow.get(18)));   // 加班工资
            export.setAbsencesalary(parseBigDecimal(dataRow.get(19)));    // 超缺勤工资

            // 合计及代扣字段（索引 20-26）
            export.setTotalsalary(parseBigDecimal(dataRow.get(20)));      // 合计
            export.setTax(parseBigDecimal(dataRow.get(21)));              // 个人所得税
            export.setSocial(parseBigDecimal(dataRow.get(22)));           // 个人社保
            export.setAccumulation(parseBigDecimal(dataRow.get(23)));     // 个人公积金
            export.setOtherdeduction(parseBigDecimal(dataRow.get(24)));   // 其他扣款
            export.setUnionfees(parseBigDecimal(dataRow.get(25)));        // 工会费
            export.setActualitysalary(parseBigDecimal(dataRow.get(26)));  // 实发工资

            saveList.add(export);
        }

        // 保存数据到导出表
        exportMapper.insertBatch(saveList);
    }

    /**
     * 安全解析BigDecimal，避免空值异常
     * @param value 字符串值
     * @return BigDecimal对象，如果为空则返回0
     */
    private BigDecimal parseBigDecimal(String value) {
        if (StringUtils.isEmpty(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void getYeBanAndJiaBan(HrmProduceAttendance hrmProduceAttendance,Map<Integer, String> empAttendanceMap,
                                   HrmSalaryBasic salaryBasic,boolean isProduce) {
        /**
         * 加班费: 加班工资：12元/小时。（生产、工程、质检、仓库有加班工资，副经
         * 理及以上岗位加班没有加班工资）
         */
        if(hrmProduceAttendance!=null)
        {
            //有加班费
            if (hrmProduceAttendance.getWorkOverTime()!=null && isProduce)
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
            if(hrmProduceAttendance.getNightSubsidy()!=null)
            {
                empAttendanceMap.put(180102, hrmProduceAttendance.getNightSubsidy().toString());//180102为夜班补贴
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
    }

    /**
     * 获取离职,本月入职的员工应出勤天数
     * @param empId
     * @param isProduce
     * @param actualityDays
     * @param daysInMonth
     * @return
     */
    private Double getDepartEmpNormalDays(Long empId,boolean isProduce,Double actualityDays,int daysInMonth,Double defaultDays)
    {
        Double normalDays =0d;
        if(isProduce)
        {
            //生产部门，应出勤天数=当月总天数-4（4为休息天数）
            normalDays = (double)(daysInMonth -4);
        }
        else
        {
            //非生产部门
            //找出一个不用打开领导的实际出勤天数 = 应出勤天数，来代表这个离职员工的应出勤天数
            normalDays = defaultDays;
        }
        return normalDays;
    }

    public static int[] getNextMonthYearAndMonth(int year, int month) {
        YearMonth currentYearMonth = YearMonth.of(year, month);
        YearMonth nextYearMonth = currentYearMonth.plusMonths(1);
        return new int[]{nextYearMonth.getYear(), nextYearMonth.getMonthValue()};
    }

    /**
     * 验证社保数据是否已生成
     * @param isSyncInsuranceData 是否同步社保数据
     * @param salaryConfig 薪资配置
     * @param year 年份
     * @param month 月份
     */
    private void validateInsuranceData(Boolean isSyncInsuranceData, HrmSalaryConfig salaryConfig, 
                                      int year, int month) {
        if (!Boolean.TRUE.equals(isSyncInsuranceData)) {
            return;
        }

        //如果是同步社保数据，需要验证社保数据是否已生成
        Integer socialSecurityMonthType = salaryConfig != null ? salaryConfig.getSocialSecurityMonthType() : null;
        if (socialSecurityMonthType == null) {
            socialSecurityMonthType = ONE;
            logger.warn("薪资配置 socialSecurityMonthType 为空，按当月口径处理：{}-{}", year, month);
        }
        DateTime date = DateUtil.parse(year + "-" + month, "yy-MM");
        if (socialSecurityMonthType == 0) {
            date = DateUtil.offsetMonth(date, -1);
        } else if (socialSecurityMonthType == 2) {
            date = DateUtil.offsetMonth(date, 1);
        }

        //查询社保数据是否生成
        Optional<HrmInsuranceMonthRecord> insuranceMonthRecordOpt = insuranceMonthRecordService.lambdaQuery()
                .eq(HrmInsuranceMonthRecord::getYear, date.year())
                .eq(HrmInsuranceMonthRecord::getMonth, date.month() + 1)
                .oneOpt();

        if (!insuranceMonthRecordOpt.isPresent()) {
            throw new HrmException(HrmCodeEnum.SOCIAL_SECURITY_DATA_IS_NOT_GENERATED_THIS_MONTH);
        }

        HrmInsuranceMonthRecord insuranceMonthRecord = insuranceMonthRecordOpt.get();
        if (!Integer.valueOf(IsEnum.YES.getValue()).equals(insuranceMonthRecord.getStatus())) {
            throw new HrmException(HrmCodeEnum.SOCIAL_SECURITY_DATA_IS_NOT_GENERATED_THIS_MONTH,
                    "社保月记录状态未完成: " + insuranceMonthRecord.getYear() + "-" + insuranceMonthRecord.getMonth(), true);
        }
    }

    /**
     * 添加专项附加扣除项
     * 包括：子女教育(260101)、住房租金(260102)、住房贷款利息(260103)、
     *      赡养老人(260104)、继续教育(260105)、3岁以下婴幼儿照护(260106)
     * 
     * @param employeeId 员工ID
     * @param salaryMonthEmpRecord 员工薪资记录
     * @param optionValueList 薪资项列表
     */
    private void addAdditionalDeductionOptions(Long employeeId, HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
                                              List<HrmSalaryMonthOptionValue> optionValueList,
                                              Map<Long, HrmAdditional> additionalDeductionMap) {
        if (employeeId == null) {
            return;
        }
        HrmAdditional additionalVO = additionalDeductionMap != null ? additionalDeductionMap.get(employeeId) : null;
        if (additionalVO == null) {
            return;
        }

        //构建专项附加扣除数据
        Map<Integer, String> additionalDeductionData = new HashMap<>();
        additionalDeductionData.put(260101, safeAmount(additionalVO.getChildrenEducation()).toString());
        additionalDeductionData.put(260102, safeAmount(additionalVO.getHousingRent()).toString());
        additionalDeductionData.put(260103, safeAmount(additionalVO.getHousingLoanInterest()).toString());
        additionalDeductionData.put(260104, safeAmount(additionalVO.getSupportingTheElderly()).toString());
        additionalDeductionData.put(260105, safeAmount(additionalVO.getContinuingEducation()).toString());
        additionalDeductionData.put(260106, safeAmount(additionalVO.getRaisingGirls()).toString());

        //添加新的专项附加扣除项
        // 注：原有记录已在 getOrCreateRecordAndApplyAttendance 中按 sEmpRecordId 全量删除，无需再按 code 逐条删除
        additionalDeductionData.forEach((code, value) -> {
            HrmSalaryMonthOptionValue salaryMonthOptionValue = new HrmSalaryMonthOptionValue();
            salaryMonthOptionValue.setSEmpRecordId(salaryMonthEmpRecord.getSEmpRecordId());
            salaryMonthOptionValue.setCode(code);
            salaryMonthOptionValue.setValue(value);
            optionValueList.add(salaryMonthOptionValue);
        });
    }

    /**
     * 获取上月个税累计数据
     * 包括：累计收入(250101)、累计减除费用(250102)、累计专项扣除(250103)、累计已缴税额(250105)
     * 
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @return 上月个税累计数据Map，如果没有则返回null
     */
    private Map<Integer, String> getLastMonthTaxData(Long employeeId, int year, int month) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("employeeId", employeeId);
        params.put("year", year);
        params.put("month", month - 1);

        //根据员工ID查询对应的个税累计数据
        QueryPersonalIncomeTaxVO personalIncomeTaxVO = incomeTaxMapper.getQueryPersonalIncomeTax(params);
        if (personalIncomeTaxVO == null) {
            return null;
        }

        Map<Integer, String> cumulativeTaxOfLastMonthData = new HashMap<>();
        cumulativeTaxOfLastMonthData.put(250101, personalIncomeTaxVO.getAccumulatedIncome().toString());
        cumulativeTaxOfLastMonthData.put(250102, personalIncomeTaxVO.getAccumulatedDeductionOfExpenses().toString());
        cumulativeTaxOfLastMonthData.put(250103, personalIncomeTaxVO.getAccumulatedProvidentFund().toString());
        cumulativeTaxOfLastMonthData.put(250105, personalIncomeTaxVO.getAccumulatedTaxPayment().toString());
        return cumulativeTaxOfLastMonthData;
    }

    /**
     * 处理半路转正员工的全勤奖和工会费
     * 半路转正的员工不享受满勤奖和工会费，薪资计算时需排除这两项
     *
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @param optionValueList 薪资选项值列表
     */
    private void removeFullAttendanceAndUnionFeeForMidMonthPromotion(int year, int month,
                                                                      List<HrmSalaryMonthOptionValue> optionValueList,
                                                                      LocalDate becomeDate) {
        if (!isMidMonthPromotion(becomeDate, year, month)) {
            return;
        }
        optionValueList.removeIf(option -> option.getCode() != null && option.getCode().equals(40102));
        optionValueList.removeIf(option -> option.getCode() != null && option.getCode().equals(160102));
    }

    /**
     * 计算半路转正员工的完整薪资（试用期+转正后）
     * 
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @param hrmProduceAttendance 考勤数据
     * @return 薪资项映射
     */
    private Map<Integer, String> calculateMidMonthPromotionFullSalary(Long employeeId, int year, int month,
                                                                      LocalDate becomeDate,
                                                                      HrmProduceAttendance hrmProduceAttendance,
                                                                      BigDecimal normalDays,
                                                                      List<HrmSalaryArchivesOption> preloadedArchivesOptionList) {
        if (!isMidMonthPromotion(becomeDate, year, month) || hrmProduceAttendance == null) {
            return Collections.emptyMap();
        }
        List<HrmSalaryArchivesOption> archivesOptionList = preloadedArchivesOptionList != null
                ? preloadedArchivesOptionList
                : Collections.emptyList();

        Map<Integer, String> probationSalaryMap = archivesOptionList.stream()
                .filter(option -> option.getIsPro() != null && option.getIsPro() == 1)
                .collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue, (v1, v2) -> v1));
        Map<Integer, String> officialSalaryMap = archivesOptionList.stream()
                .filter(option -> option.getIsPro() != null && option.getIsPro() == 0)
                .collect(Collectors.toMap(HrmSalaryArchivesOption::getCode, HrmSalaryArchivesOption::getValue, (v1, v2) -> v1));
        return calculateMidMonthPromotionSalaryAmounts(probationSalaryMap, officialSalaryMap, hrmProduceAttendance, normalDays);
    }
    
    /**
     * 处理半路转正员工的薪资计算（集成到主流程）
     * <p>
     * 改造后委托 {@link #recalculateMidMonthPromotionTaxAndPay} 完成税算与实发重算，
     * 确保扣款项从完整基础项上下文中读取（P0-1），并复用统一税算口径（P0-2）。
     *
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @param optionValueList 薪资项列表（来自 computeSalary 返回的汇总项）
     * @param deptType 部门类型
     * @param becomeDate 转正日期
     * @param attendance 考勤数据
     * @param normalDays 应出勤天数
     * @param lastMonthTaxData 上月个税累计数据
     * @param baseOptionMap 首次落库前的完整基础项（含 100101/100102/280/282 等扣款项）
     * @param isDisabled 是否残疾员工（"1"=残疾免税）
     * @param skipTaxForRemark is_remark=2 且年收入<6w 时为 true
     * @param taxSpecialAdditionalDeduction 专项附加扣除合计
     */
    private void processMidMonthPromotionSalary(Long employeeId, int year, int month,
                                                List<HrmSalaryMonthOptionValue> optionValueList, Integer deptType,
                                                LocalDate becomeDate, HrmProduceAttendance attendance,
                                                BigDecimal normalDays, Map<Integer, String> lastMonthTaxData,
                                                Map<Integer, String> baseOptionMap,
                                                String isDisabled, boolean skipTaxForRemark,
                                                BigDecimal taxSpecialAdditionalDeduction,
                                                BigDecimal welfareTaxableIncome,
                                                Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap) {
        if (!isMidMonthPromotion(becomeDate, year, month) || CollUtil.isEmpty(optionValueList)) {
            return;
        }

        List<HrmSalaryArchivesOption> preloadedArchivesOptionList = midMonthArchivesOptionMap != null
                ? midMonthArchivesOptionMap.get(employeeId)
                : Collections.emptyList();
        Map<Integer, String> salaryMap = calculateMidMonthPromotionFullSalary(employeeId, year, month,
                becomeDate, attendance, normalDays, preloadedArchivesOptionList);
        if (salaryMap.isEmpty()) {
            return;
        }

        // 将基础项中的扣款数据注入到 optionValueList，确保 recalculate 能读到完整扣款（P0-1 修复核心）
        if (baseOptionMap != null) {
            Map<Integer, HrmSalaryMonthOptionValue> existingMap = optionValueList.stream()
                    .filter(o -> o.getCode() != null)
                    .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> a));
            for (int code : new int[]{100101, 100102, 280, 282}) {
                if (!existingMap.containsKey(code) && baseOptionMap.containsKey(code)) {
                    HrmSalaryMonthOptionValue injected = new HrmSalaryMonthOptionValue();
                    injected.setCode(code);
                    injected.setValue(baseOptionMap.get(code));
                    optionValueList.add(injected);
                }
            }
        }

        recalculateMidMonthPromotionTaxAndPay(optionValueList, salaryMap, lastMonthTaxData,
                year, month, isDisabled, skipTaxForRemark, taxSpecialAdditionalDeduction,
                welfareTaxableIncome);
    }
    
    /**
     * 构建工资数据行 - 将QuerySalaryPageListVO转换为Map结构
     * @param vo 工资页面列表VO
     * @param companyId 公司ID
     * @return Map<String, String> 数据行映射
     */
    /**
     * 构建工资数据行，将VO对象转换为Map结构
     * @param vo 工资数据VO
     * @param companyId 公司ID
     * @return 以字段名为键的数据Map
     */
    private Map<String, String> buildSalaryDataRow(QuerySalaryPageListVO vo, String companyId) {
        Map<String, String> dataRow = new LinkedHashMap<>();

        // 基础信息字段（索引 0-9）- 严格按照Excel模板列顺序
        dataRow.put("序号", vo.getXh() != null ? vo.getXh().toString() : "");
        dataRow.put("月份", vo.getMonth() != null ? vo.getMonth().toString() : "");
        dataRow.put("姓名", vo.getEmployeeName() != null ? vo.getEmployeeName().toString() : "");
        dataRow.put("性别", vo.getSex() != null ? vo.getSex().toString() : "");
        dataRow.put("入职时间", vo.getEntryTime() != null ? vo.getEntryTime().toString() : "");
        dataRow.put("部门", vo.getDeptName() != null ? vo.getDeptName().toString() : "");
        dataRow.put("岗位", vo.getPost() != null ? vo.getPost() : "");
        dataRow.put("满勤天数", vo.getNeedWorkDay() != null ? vo.getNeedWorkDay().toString() : "0");
        dataRow.put("超缺勤天数", getValueByCode(vo.getSalary(), 9008));
        dataRow.put("加班工时", getValueByCode(vo.getSalary(), 9009));

        // 工资项字段（索引 10-19）- 严格按照Excel模板列顺序
        if (vo.getSalary() != null) {
            // 应发工资部分
            dataRow.put("基本工资", getValueByCode(vo.getSalary(), 10101));
            dataRow.put("岗位工资", getValueByCode(vo.getSalary(), 10102));

            // 第12列：根据公司ID决定显示绩效工资(41001)还是职务补助(10103)
            if ("0002".equals(companyId)) {
                dataRow.put("绩效工资", getValueByCode(vo.getSalary(), 41001));
            } else {
                dataRow.put("绩效工资", getValueByCode(vo.getSalary(), 10103));
            }

            dataRow.put("高温津贴", getValueByCode(vo.getSalary(), 20102));
            dataRow.put("低温津贴", getValueByCode(vo.getSalary(), 20105));
            dataRow.put("夜班补贴", getValueByCode(vo.getSalary(), 180102));
            dataRow.put("其他补贴", getValueByCode(vo.getSalary(), 281));
            dataRow.put("全勤奖", getValueByCode(vo.getSalary(), 40102));

            // 加班工资部分
            dataRow.put("加班工资", getValueByCode(vo.getSalary(), 180101));
            dataRow.put("超缺勤", getValueByCode(vo.getSalary(), 200101));

            // 合计列（应发工资总额，code=210101）
            dataRow.put("合计", getValueByCode(vo.getSalary(), 210101));

            // 代扣部分
            dataRow.put("个人所得税", getValueByCode(vo.getSalary(), 230101));
            dataRow.put("个人社保", getValueByCode(vo.getSalary(), 100101));
            dataRow.put("个人公积金", getValueByCode(vo.getSalary(), 100102));
            dataRow.put("其他扣款", getValueByCode(vo.getSalary(), 280));
            dataRow.put("工会费", getValueByCode(vo.getSalary(), 160102));

            // 实发工资（code=240101）
            dataRow.put("实发工资", getValueByCode(vo.getSalary(), 240101));
        } else {
            // 如果没有工资数据，初始化为0
            dataRow.put("基本工资", "0");
            dataRow.put("岗位工资", "0");
            dataRow.put("绩效工资", "0");
            dataRow.put("高温津贴", "0");
            dataRow.put("低温津贴", "0");
            dataRow.put("夜班补贴", "0");
            dataRow.put("其他补贴", "0");
            dataRow.put("全勤奖", "0");
            dataRow.put("加班工资", "0");
            dataRow.put("超缺勤", "0");
            dataRow.put("合计", "0");
            dataRow.put("个人所得税", "0");
            dataRow.put("个人社保", "0");
            dataRow.put("个人公积金", "0");
            dataRow.put("其他扣款", "0");
            dataRow.put("工会费", "0");
            dataRow.put("实发工资", "0");
        }

        // 签领人字段
        dataRow.put("签领人", getValueByCode(vo.getSalary(), 9010));

        return dataRow;
    }
    
    /**
     * 根据工资代码获取对应的值
     * @param salaryValues 工资值列表
     * @param code 工资代码
     * @return 对应的值字符串，如果找不到则返回"0"
     */
    private String getValueByCode(List<QuerySalaryPageListVO.SalaryValue> salaryValues, Integer code) {
        if (salaryValues == null || code == null) {
            return "0";
        }
        
        QuerySalaryPageListVO.SalaryValue salaryValue = salaryValues.stream()
            .filter(sv -> sv.getCode() != null && sv.getCode().equals(code))
            .findFirst().orElse(null);
            
        if (salaryValue != null && salaryValue.getValue() != null && !salaryValue.getValue().isEmpty()) {
            return salaryValue.getValue();
        }
        
        return "0";
    }
}
