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
import com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.entity.HrmEmployeeAdditional;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.additional.service.HrmAdditionalService;
import com.tianye.hrsystem.modules.additional.service.HrmEmployeeAdditionalService;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonusTaxOnly;
import com.tianye.hrsystem.modules.bonus.mapper.HrmBonusTaxOnlyMapper;
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
import com.tianye.hrsystem.modules.salary.mapper.HrmSalarySlipMapper;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalarySlipRecordMapper;
import com.tianye.hrsystem.modules.salary.support.HrmSalaryBasicDefaults;
import com.tianye.hrsystem.modules.salary.support.SalaryExportCommentWriteHandler;
import com.tianye.hrsystem.modules.salary.vo.SalaryOptionHeadVO;
import com.tianye.hrsystem.modules.salary.vo.*;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmOvertimeNightStatisticsDetailRepository;
import com.tianye.hrsystem.service.IHrmAttendanceClockService;
import com.tianye.hrsystem.service.IHrmAttendanceGroupService;
import com.tianye.hrsystem.service.IHrmAttendanceReportDataService;
import com.tianye.hrsystem.service.IHrmAttendanceRuleService;
import com.tianye.hrsystem.service.IHrmDeptService;
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
import org.springframework.transaction.support.TransactionTemplate;
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
    private TransactionTemplate transactionTemplate;


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
    private IHrmDeptService hrmDeptService;

    /**
     * 部门筛选递归填充子部门ID(不影响原 deptId 语义)
     */
    private void fillDeptIdsWithChildren(QuerySalaryPageListDto dto) {
        if (dto != null && dto.getDeptId() != null && CollUtil.isEmpty(dto.getDeptIds())) {
            List<Long> allDeptIds = new ArrayList<>();
            allDeptIds.add(dto.getDeptId());
            allDeptIds.addAll(RecursionUtil.getChildList(hrmDeptService.list(), "parentId", dto.getDeptId(), "deptId", "deptId"));
            dto.setDeptIds(allDeptIds);
        }
    }

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
    HrmBonusTaxOnlyMapper hrmBonusTaxOnlyMapper;

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
    private hrmOvertimeNightStatisticsDetailRepository overtimeNightStatisticsDetailRepository;

    @Autowired
    HrmSalaryExportMapper exportMapper;

    @Autowired
    private HrmSalarySlipRecordMapper salarySlipRecordMapper;

    @Autowired
    private HrmSalarySlipMapper salarySlipMapper;

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
    /** 员工所属体系：生产 */
    private static final int AFFILIATION_SYSTEM_PRODUCTION = 2;
    /** 员工休息制度：固定月休4天 */
    private static final int REST_TYPE_FIXED_MONTHLY_REST = 2;
    private static final BigDecimal MID_MONTH_OVERTIME_UNIT_PRICE = new BigDecimal("12");
    private static final BigDecimal MONTHLY_TAX_FREE_DEDUCTION = new BigDecimal("5000");
    public static final int MAX_COMPUTE_EMPLOYEE_COUNT = 50;
    private static final Set<Integer> EXCLUDED_NO_FIXED_CODES = new HashSet<>(Arrays.asList(
            100101, 100102, 110101, 120101,
            1001, 160102, 210101, 220101, 230101, 240101,
            250101, 250102, 250103, 250105,
            270101, 270102, 270103, 270104, 270105, 270106,
            40102, 41001, 280, 281, 282, 20105, 20102
    ));
    // 锁对象常驻缓存（键为薪资月记录ID，数量有限）：不做事后 remove，
    // 避免“unlock 后 hasQueuedThreads 判断再 remove”竞态导致两个线程各持不同锁对象并行进入临界区
    private static final ConcurrentHashMap<Long, ReentrantLock> COMPUTE_RECORD_LOCK_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SalaryComputeProgressState> SALARY_COMPUTE_PROGRESS_MAP = new ConcurrentHashMap<>();
    private static final long SALARY_COMPUTE_PROGRESS_TTL_MS = 60 * 60 * 1000L;
    private static final String COMPUTE_STATUS_IDLE = "IDLE";
    private static final String COMPUTE_STATUS_RUNNING = "RUNNING";
    private static final String COMPUTE_STATUS_SUCCESS = "SUCCESS";
    private static final String COMPUTE_STATUS_FAILED = "FAILED";

    private static class SalaryComputeProgressState {
        // 核算线程写、HTTP 轮询线程读，volatile 保证可见性
        volatile int progress;
        volatile String status;
        volatile String stage;
        volatile String message;
        volatile int processedCount;
        volatile int totalCount;
        List<String> errors = Collections.emptyList();
        volatile long updateTime;
    }

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
        final Map<Long, BigDecimal> accruedAttendanceDaysByEmployee;
        final Map<Long, BigDecimal> expectedAttendanceDaysByEmployee;

        AttendanceSyncBatchData(List<Long> employeeIds, List<HrmProduceAttendance> hasOverTimePayEmpList,
                                HrmSalaryBasic salaryBasic, HrmAttendanceRule attendanceRule,
                                Date beginDate, Date endDate,
                                List<HrmAttendanceSummaryDayVo> attendanceSummaryVoDayList,
                                Map<Long, List<HrmAttendanceSummaryDayVo>> attendanceSummaryDayMap,
                                List<HrmAttendanceSummaryVo> attendanceSummaryVoList,
                                Map<Long, HrmAttendanceSummaryVo> attendanceSummaryMap,
                                List<QuerySalaryArchivesListVO> empSalaryArchivesList,
                                Map<Long, QuerySalaryArchivesListVO> empSalaryArchivesMap,
                                LocalDate dateStartTime, LocalDate dateEndTime, int daysInMonth, List<String> dates,
                                Map<Long, List<QueryHolidayDeductionVO>> holidayDeductionMap,
                                Map<Long, Map<String, Double>> workHoursMap,
                                Map<Long, String> deptNameMap,
                                Map<Long, BigDecimal> accruedAttendanceDaysByEmployee,
                                Map<Long, BigDecimal> expectedAttendanceDaysByEmployee) {
            this.employeeIds = employeeIds;
            this.hasOverTimePayEmpList = hasOverTimePayEmpList != null ? hasOverTimePayEmpList : Collections.emptyList();
            this.overTimePayEmpMap = toProduceAttendanceMap(this.hasOverTimePayEmpList);
            this.salaryBasic = salaryBasic;
            this.attendanceRule = attendanceRule;
            this.beginDate = beginDate;
            this.endDate = endDate;
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
            this.accruedAttendanceDaysByEmployee = accruedAttendanceDaysByEmployee != null
                    ? accruedAttendanceDaysByEmployee : Collections.emptyMap();
            this.expectedAttendanceDaysByEmployee = expectedAttendanceDaysByEmployee != null
                    ? expectedAttendanceDaysByEmployee : Collections.emptyMap();
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
        final Map<Long, BigDecimal> expectedAttendanceDaysByEmployee;
        final Map<Long, Map<Integer, String>> lastMonthTaxDataMap;
        final Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap;
        final Map<Long, HrmAdditional> additionalDeductionMap;
        final Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap;

        SalaryComputeBatchData(List<Map<String, Object>> employeeMapList,
                               Map<String, Map<Integer, String>> attendanceDataMap,
                               Map<Long, Boolean> hasAttendanceGroupMap,
                               SalaryOptionBatchData salaryOptionBatchData,
                               Map<Long, HrmProduceAttendance> produceAttendanceMap,
                               Map<Long, BigDecimal> expectedAttendanceDaysByEmployee,
                               Map<Long, Map<Integer, String>> lastMonthTaxDataMap,
                               Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap,
                               Map<Long, HrmAdditional> additionalDeductionMap,
                               Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap) {
            this.employeeMapList = employeeMapList != null ? employeeMapList : Collections.emptyList();
            this.attendanceDataMap = attendanceDataMap != null ? attendanceDataMap : Collections.emptyMap();
            this.hasAttendanceGroupMap = hasAttendanceGroupMap != null ? hasAttendanceGroupMap : Collections.emptyMap();
            this.salaryOptionBatchData = salaryOptionBatchData != null ? salaryOptionBatchData
                    : new SalaryOptionBatchData(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
            this.produceAttendanceMap = produceAttendanceMap != null ? produceAttendanceMap : Collections.emptyMap();
            this.expectedAttendanceDaysByEmployee = expectedAttendanceDaysByEmployee != null
                    ? expectedAttendanceDaysByEmployee : Collections.emptyMap();
            this.lastMonthTaxDataMap = lastMonthTaxDataMap != null ? lastMonthTaxDataMap : Collections.emptyMap();
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
        final Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap;
        final Map<Long, HrmAdditional> additionalDeductionMap;
        final Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap;

        HistoryComputeData(Map<Long, Map<Integer, String>> lastMonthTaxDataMap,
                           Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap,
                           Map<Long, HrmAdditional> additionalDeductionMap,
                           Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap) {
            this.lastMonthTaxDataMap = lastMonthTaxDataMap != null ? lastMonthTaxDataMap : Collections.emptyMap();
            this.socialSecurityEmpRecordMap = socialSecurityEmpRecordMap != null ? socialSecurityEmpRecordMap : Collections.emptyMap();
            this.additionalDeductionMap = additionalDeductionMap != null ? additionalDeductionMap : Collections.emptyMap();
            this.midMonthArchivesOptionMap = midMonthArchivesOptionMap != null ? midMonthArchivesOptionMap : Collections.emptyMap();
        }
    }

    static boolean isProductionAffiliationSystem(Map<String, Object> map) {
        if (map == null) {
            return false;
        }
        return isProductionAffiliationSystem(parseIntegerValue(map.get("affiliationSystem")));
    }

    static boolean isProductionAffiliationSystem(Integer affiliationSystem) {
        return Integer.valueOf(AFFILIATION_SYSTEM_PRODUCTION).equals(affiliationSystem);
    }

    static boolean isFixedRestProductionEmployee(Map<String, Object> map) {
        if (map == null) {
            return false;
        }
        return isFixedRestProductionEmployee(
                parseIntegerValue(map.get("affiliationSystem")),
                parseIntegerValue(map.get("restType"))
        );
    }

    static boolean isFixedRestProductionEmployee(Integer affiliationSystem, Integer restType) {
        return isProductionAffiliationSystem(affiliationSystem)
                && Integer.valueOf(REST_TYPE_FIXED_MONTHLY_REST).equals(restType);
    }

    private static Integer parseIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
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
        if (hasOverTimePayEmpList == null) {
            hasOverTimePayEmpList = Collections.emptyList();
        }
        HrmSalaryBasic salaryBasic = hrmSalaryBasicService.lambdaQuery().orderByDesc(HrmSalaryBasic::getCreateTime).last("limit 1").one();
        HrmAttendanceRule attendanceRule = selectEffectiveAttendanceRule();
        LocalDateTime startDateTime = dateStartTime.atStartOfDay();
        LocalDateTime endDateTime = LocalDateTimeUtil.endOfDay(dateEndTime.atStartOfDay());
        Date beginDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
        HashMap<String, Object> params = new HashMap<>();
        params.put("beginDate", beginDate);
        params.put("endDate", endDate);
        List<HrmAttendanceSummaryDayVo> attendanceSummaryVoDayList = attendanceReportDataService.getEmpAttendanceSummaryDayList(params);
        if (attendanceSummaryVoDayList == null) {
            attendanceSummaryVoDayList = Collections.emptyList();
        }
        Map<Long, List<HrmAttendanceSummaryDayVo>> attendanceSummaryDayMap = attendanceSummaryVoDayList.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.groupingBy(HrmAttendanceSummaryDayVo::getEmployeeId));
        List<HrmAttendanceSummaryVo> attendanceSummaryVoList = attendanceReportDataService.getEmpAttendanceSummaryList(params);
        if (attendanceSummaryVoList == null) {
            attendanceSummaryVoList = Collections.emptyList();
        }
        Map<Long, HrmAttendanceSummaryVo> attendanceSummaryMap = attendanceSummaryVoList.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmAttendanceSummaryVo::getEmployeeId, Function.identity(), (v1, v2) -> v1));
        QuerySalaryArchivesListDto querySalaryArchivesListDto = new QuerySalaryArchivesListDto();
        querySalaryArchivesListDto.setEmployeeIds(employeeIds);
        querySalaryArchivesListDto.setYear(year);
        querySalaryArchivesListDto.setMonth(month);
        List<QuerySalaryArchivesListVO> empSalaryArchivesList = salaryArchivesService.queryEmpSalaryArchivesList(querySalaryArchivesListDto);
        if (empSalaryArchivesList == null) {
            empSalaryArchivesList = Collections.emptyList();
        }
        Map<Long, QuerySalaryArchivesListVO> empSalaryArchivesMap = empSalaryArchivesList.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.toMap(QuerySalaryArchivesListVO::getEmployeeId, Function.identity(), (v1, v2) -> v1));

        // 批量预加载假期抵扣数据（消除 fillAttendanceDataForEmployee 中的 N+1 查询）
        List<QueryHolidayDeductionVO> allDeductions = holidayDeductionService.queryHolidayDeductionBatch(year, month);
        if (allDeductions == null) {
            allDeductions = Collections.emptyList();
        }
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
            List<com.tianye.hrsystem.model.HrmDept> deptList = hrmDeptRepository.findAllByDeptIdIn(deptIds);
            deptNameMap = deptList.stream()
                    .filter(d -> d.getDeptId() != null && StrUtil.isNotBlank(d.getName()))
                    .collect(Collectors.toMap(com.tianye.hrsystem.model.HrmDept::getDeptId,
                            com.tianye.hrsystem.model.HrmDept::getName, (v1, v2) -> v1));
        }

        List<HrmOvertimeNightStatisticsDetail> overtimeNightDetails =
                loadOvertimeNightStatisticsDetails(year, month, employeeIds);
        Map<Long, BigDecimal> accruedAttendanceDaysByEmployee =
                buildAccruedAttendanceDaysByEmployee(overtimeNightDetails);
        Map<Long, BigDecimal> expectedAttendanceDaysByEmployee =
                buildExpectedAttendanceDaysByEmployee(overtimeNightDetails);

        return new AttendanceSyncBatchData(employeeIds, hasOverTimePayEmpList, salaryBasic, attendanceRule,
                beginDate, endDate, attendanceSummaryVoDayList, attendanceSummaryDayMap,
                attendanceSummaryVoList, attendanceSummaryMap, empSalaryArchivesList, empSalaryArchivesMap,
                dateStartTime, dateEndTime, daysInMonth, dates,
                holidayDeductionMap, workHoursMap, deptNameMap,
                accruedAttendanceDaysByEmployee, expectedAttendanceDaysByEmployee);
    }

    private static Map<Long, HrmProduceAttendance> toProduceAttendanceMap(List<HrmProduceAttendance> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
                .filter(v -> v.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmProduceAttendance::getEmployeeId, Function.identity(), (v1, v2) -> v1));
    }

    private List<HrmOvertimeNightStatisticsDetail> loadOvertimeNightStatisticsDetails(int year,
                                                                                      int month,
                                                                                      List<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds) || overtimeNightStatisticsDetailRepository == null) {
            return Collections.emptyList();
        }
        List<Long> distinctEmployeeIds = employeeIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(distinctEmployeeIds)) {
            return Collections.emptyList();
        }
        return overtimeNightStatisticsDetailRepository
                .findAllByStatYearAndStatMonthAndEmployeeIdIn(year, month, distinctEmployeeIds);
    }

    static Map<Long, BigDecimal> buildAccruedAttendanceDaysByEmployee(List<HrmOvertimeNightStatisticsDetail> details) {
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details) {
            if (detail == null || detail.getEmployeeId() == null || detail.getAccruedAttendanceHours() == null) {
                continue;
            }
            result.putIfAbsent(detail.getEmployeeId(), toAttendanceDays(detail.getAccruedAttendanceHours()));
        }
        return result;
    }

    static Map<Long, BigDecimal> buildExpectedAttendanceDaysByEmployee(List<HrmOvertimeNightStatisticsDetail> details) {
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details) {
            if (detail == null || detail.getEmployeeId() == null || detail.getExpectedAttendanceDays() == null) {
                continue;
            }
            result.putIfAbsent(detail.getEmployeeId(),
                    BigDecimal.valueOf(detail.getExpectedAttendanceDays()).setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    static Map<Long, BigDecimal> buildExportFullWorkDaysByEmployee(List<HrmOvertimeNightStatisticsDetail> details) {
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details) {
            if (detail == null
                    || detail.getEmployeeId() == null
                    || detail.getAccruedAttendanceHours() == null) {
                continue;
            }
            result.putIfAbsent(detail.getEmployeeId(), toAttendanceDays(detail.getAccruedAttendanceHours()));
        }
        return result;
    }

    static Map<Long, BigDecimal> buildExportAbsenceDaysByEmployee(List<HrmOvertimeNightStatisticsDetail> details) {
        if (CollUtil.isEmpty(details)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details) {
            if (detail == null
                    || detail.getEmployeeId() == null
                    || detail.getExpectedAttendanceDays() == null
                    || detail.getAccruedAttendanceHours() == null) {
                continue;
            }
            BigDecimal expectedHours = BigDecimal.valueOf(detail.getExpectedAttendanceDays())
                    .multiply(BigDecimal.valueOf(8));
            BigDecimal differenceDays = expectedHours.subtract(detail.getAccruedAttendanceHours())
                    .divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
            result.putIfAbsent(detail.getEmployeeId(), differenceDays);
        }
        return result;
    }

    private static BigDecimal resolveExportFullWorkDays(Map<Long, BigDecimal> fullWorkDaysByEmployee,
                                                        Long employeeId) {
        return resolveExportOvertimeNightDays(fullWorkDaysByEmployee, employeeId, "满勤天数");
    }

    private static BigDecimal resolveExportAbsenceDays(Map<Long, BigDecimal> absenceDaysByEmployee,
                                                       Long employeeId) {
        return resolveExportOvertimeNightDays(absenceDaysByEmployee, employeeId, "超缺勤天数");
    }

    private static BigDecimal resolveExportOvertimeNightDays(Map<Long, BigDecimal> daysByEmployee,
                                                             Long employeeId,
                                                             String columnName) {
        if (employeeId != null && daysByEmployee != null && daysByEmployee.containsKey(employeeId)) {
            return daysByEmployee.get(employeeId);
        }
        throw new HrmException(7001, "薪资导出" + columnName
                + "缺少加班/夜班统计明细，请先到单双休设置中设置数据后再导出薪资");
    }

    static BigDecimal resolveSalaryAttendanceDays(HrmProduceAttendance attendance,
                                                  Map<Long, BigDecimal> overtimeNightAccruedAttendanceDaysByEmployee,
                                                  Long employeeId) {
        if (attendance != null && attendance.getProbationAttendance() != null) {
            return normalizeAttendanceDays(attendance.getProbationAttendance());
        }
        if (employeeId == null || overtimeNightAccruedAttendanceDaysByEmployee == null) {
            return null;
        }
        return normalizeAttendanceDays(overtimeNightAccruedAttendanceDaysByEmployee.get(employeeId));
    }

    static BigDecimal resolveFullAttendanceExpectedDays(Map<Long, BigDecimal> overtimeNightExpectedAttendanceDaysByEmployee,
                                                        Long employeeId) {
        return resolveSalaryExpectedAttendanceDays(
                overtimeNightExpectedAttendanceDaysByEmployee, employeeId);
    }

    static BigDecimal resolveSalaryExpectedAttendanceDays(Map<Long, BigDecimal> overtimeNightExpectedAttendanceDaysByEmployee,
                                                          Long employeeId) {
        if (employeeId != null && overtimeNightExpectedAttendanceDaysByEmployee != null) {
            BigDecimal overtimeNightExpectedDays = overtimeNightExpectedAttendanceDaysByEmployee.get(employeeId);
            if (isPositive(overtimeNightExpectedDays)) {
                return normalizeAttendanceDays(overtimeNightExpectedDays);
            }
        }
        return null;
    }

    private static BigDecimal requireSalaryExpectedAttendanceDays(
            Map<Long, BigDecimal> overtimeNightExpectedAttendanceDaysByEmployee,
            Long employeeId,
            String employeeName,
            String jobNumber,
            int year,
            int month) {
        BigDecimal attendanceDays = resolveSalaryExpectedAttendanceDays(
                overtimeNightExpectedAttendanceDaysByEmployee, employeeId);
        if (attendanceDays != null) {
            return attendanceDays;
        }
        throw missingSalaryExpectedAttendanceDaysException(employeeId, employeeName, jobNumber, year, month);
    }

    private static BigDecimal resolveSalaryNormalDaysForCompute(SalaryComputeContext ctx,
                                                                Long employeeId) {
        if (ctx == null) {
            return null;
        }
        return resolveSalaryExpectedAttendanceDays(ctx.getExpectedAttendanceDaysByEmployee(), employeeId);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static HrmException missingSalaryExpectedAttendanceDaysException(Long employeeId,
                                                                            String employeeName,
                                                                            String jobNumber,
                                                                            int year,
                                                                            int month) {
        StringBuilder label = new StringBuilder();
        if (StrUtil.isNotBlank(employeeName)) {
            label.append(employeeName);
        }
        if (employeeId != null) {
            if (label.length() > 0) {
                label.append(" ");
            }
            label.append("员工ID ").append(employeeId);
        }
        if (StrUtil.isNotBlank(jobNumber)) {
            if (label.length() > 0) {
                label.append(" ");
            }
            label.append("工号").append(jobNumber);
        }
        if (label.length() == 0) {
            label.append("当前员工");
        }
        return new HrmException(6001, "员工" + label + "缺少" + year + "-" + month
                + "应出勤天数，请先到单双休设置中设置数据后再生成薪资或导出薪资。");
    }

    static boolean shouldFallbackFullAttendanceByAccruedDays(BigDecimal salaryAttendanceDays,
                                                             BigDecimal expectedAttendanceDays,
                                                             HrmAttendanceSummaryVo empAttendanceSummary,
                                                             BigDecimal sickLeaveDays) {
        if (salaryAttendanceDays == null || expectedAttendanceDays == null) {
            return false;
        }
        if (expectedAttendanceDays.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (isPositive(sickLeaveDays)) {
            return false;
        }
        return salaryAttendanceDays.compareTo(expectedAttendanceDays) >= 0;
    }

    private static BigDecimal toAttendanceDays(BigDecimal attendanceHours) {
        if (attendanceHours == null) {
            return null;
        }
        return attendanceHours.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
    }

    /**
     * 选取生效的考勤扣款规则：优先取"默认设置"标记的规则（有多条时取其中最新创建的），
     * 没有任何默认标记时回退为最新创建的一条（兼容历史数据）。
     */
    private HrmAttendanceRule selectEffectiveAttendanceRule() {
        HrmAttendanceRule defaultRule = hrmAttendanceRuleService.lambdaQuery()
                .eq(HrmAttendanceRule::getIsDefaultSetting, IsEnum.YES.getValue())
                .orderByDesc(HrmAttendanceRule::getCreateTime)
                .last("limit 1")
                .one();
        if (defaultRule != null) {
            return defaultRule;
        }
        return hrmAttendanceRuleService.lambdaQuery()
                .orderByDesc(HrmAttendanceRule::getCreateTime)
                .last("limit 1")
                .one();
    }

    private static BigDecimal normalizeAttendanceDays(BigDecimal attendanceDays) {
        if (attendanceDays == null) {
            return null;
        }
        return attendanceDays.setScale(2, RoundingMode.HALF_UP);
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
        return calculateMidMonthPromotionSalaryAmounts(probationSalaryMap, officialSalaryMap, attendance, normalDays, true);
    }

    static Map<Integer, String> calculateMidMonthPromotionSalaryAmounts(Map<Integer, String> probationSalaryMap,
                                                                         Map<Integer, String> officialSalaryMap,
                                                                         HrmProduceAttendance attendance,
                                                                         BigDecimal normalDays,
                                                                         boolean canCountOvertimeNight) {
        if (attendance == null) {
            return Collections.emptyMap();
        }
        Map<Integer, String> resultMap = new HashMap<>();
        if (normalDays == null || normalDays.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyMap();
        }
        BigDecimal safeNormalDays = normalDays;

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
        BigDecimal nightSubsidy = canCountOvertimeNight ? nullSafe(attendance.getNightSubsidy()) : BigDecimal.ZERO;
        totalSubsidies = nightSubsidy
                .add(nullSafe(attendance.getOtherSubsidies()))
                .add(nullSafe(attendance.getHighTemperature()))
                .add(nullSafe(attendance.getLowTemperature()));
        overtimePay = canCountOvertimeNight
                ? nullSafe(attendance.getWorkOverTime()).multiply(MID_MONTH_OVERTIME_UNIT_PRICE)
                : BigDecimal.ZERO;

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

    private BigDecimal getTaxOnlyBonusSalary(Long employeeId, Integer year, Integer month) {
        if (employeeId == null || year == null || month == null || hrmBonusTaxOnlyMapper == null) {
            return BigDecimal.ZERO;
        }
        HrmBonusTaxOnly bonus = hrmBonusTaxOnlyMapper.getEmpTaxOnlyBonus(employeeId, year, month);
        if (bonus == null || bonus.getBonus() == null || bonus.getBonus().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return bonus.getBonus();
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
     * @param hasAnnualDeductionRemark is_remark=2 时为 true，累计减除费用按全年60000
     * @param taxSpecialAdditionalDeduction 专项附加扣除合计，null 时按 0 处理
     */
    static void recalculateMidMonthPromotionTaxAndPay(
            List<HrmSalaryMonthOptionValue> allOptions,
            Map<Integer, String> midMonthSalaryMap,
            Map<Integer, String> lastMonthTaxData,
            int year, int month,
            String isDisabled,
            boolean hasAnnualDeductionRemark,
            BigDecimal taxSpecialAdditionalDeduction,
            BigDecimal welfareTaxableIncome) {
        recalculateMidMonthPromotionTaxAndPay(allOptions, midMonthSalaryMap, lastMonthTaxData,
                year, month, isDisabled, hasAnnualDeductionRemark, taxSpecialAdditionalDeduction,
                welfareTaxableIncome, BigDecimal.ZERO);
    }

    static void recalculateMidMonthPromotionTaxAndPay(
            List<HrmSalaryMonthOptionValue> allOptions,
            Map<Integer, String> midMonthSalaryMap,
            Map<Integer, String> lastMonthTaxData,
            int year, int month,
            String isDisabled,
            boolean hasAnnualDeductionRemark,
            BigDecimal taxSpecialAdditionalDeduction,
            BigDecimal welfareTaxableIncome,
            BigDecimal taxOnlyBonusSalary) {

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
                taxOnlyBonusSalary,
                false,                     // includeBonusInCumulativeIncome
                isDisabledNo,
                hasAnnualDeductionRemark,
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
                                                                  BigDecimal taxOnlyBonusSalary,
                                                                  boolean includeBonusInCumulativeIncome,
                                                                  boolean isDisabledNo,
                                                                  boolean hasAnnualDeductionRemark,
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
        BigDecimal safeTaxOnlyBonusSalary = taxOnlyBonusSalary == null ? BigDecimal.ZERO : taxOnlyBonusSalary;
        BigDecimal safeWelfareTaxableIncome = welfareTaxableIncome == null ? BigDecimal.ZERO : welfareTaxableIncome;

        BigDecimal baseShouldTaxSalary = safeShouldPaySalary.add(safeSpecialTaxSalary).subtract(safeProxyPaySalary);
        BigDecimal shouldTaxSalary = baseShouldTaxSalary.compareTo(MONTHLY_TAX_FREE_DEDUCTION) > 0
                ? baseShouldTaxSalary.subtract(MONTHLY_TAX_FREE_DEDUCTION)
                : BigDecimal.ZERO;

        BigDecimal cumulativeIncome = parseAmount(lastTaxMap.get(250101)).add(safeShouldPaySalary);
        if (includeBonusInCumulativeIncome) {
            cumulativeIncome = cumulativeIncome.add(safeBonusSalary);
        }
        cumulativeIncome = cumulativeIncome.add(safeTaxOnlyBonusSalary).add(safeWelfareTaxableIncome);
        BigDecimal cumulativeDeductions = SalaryComputeServiceNew.resolveCumulativeDeductions(
                lastTaxMap, month, hasAnnualDeductionRemark);
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
        if (isDisabledNo) {
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
        if (month == 1) {
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
        fillDeptIdsWithChildren(querySalaryPageListDto);
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
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, querySalaryPageListVO.getActualWorkDay().toString(), 1, "应计出勤天数"));
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
        computeSalaryData(sRecordId, isSyncInsuranceData, isSyncAttendanceData, employeeId, null);
    }

    // 注意：不再用 @Transactional 包住整个方法——核算等待锁期间若持有事务会占死租户连接池；
    // 改为：先取锁，锁内再用 TransactionTemplate 开启事务执行核算
    public void computeSalaryData(Long sRecordId, Boolean isSyncInsuranceData, Boolean isSyncAttendanceData,
                                  Long employeeId, List<Long> employeeIds) {
        List<Long> computeEmployeeIds = normalizeComputeEmployeeIds(employeeId, employeeIds);
        String progressKey = buildComputeProgressKey(sRecordId, computeEmployeeIds);
        clearExpiredComputeProgress();
        updateComputeProgress(progressKey, 1, COMPUTE_STATUS_RUNNING, "PREPARE",
                "正在准备核算任务", 0, 0);
        try {
            withSalaryRecordLock(sRecordId, () -> {
                runInTransaction(() -> {
                    doComputeSalaryData(sRecordId, isSyncInsuranceData, isSyncAttendanceData, computeEmployeeIds, progressKey);
                    return null;
                });
                return null;
            });
            updateComputeProgress(progressKey, 100, COMPUTE_STATUS_SUCCESS, "FINISH",
                    "核算完成", null, null);
        } catch (Exception ex) {
            updateComputeProgress(progressKey, null, COMPUTE_STATUS_FAILED, "ERROR",
                    resolveComputeErrorMessage(ex), null, null);
            throw ex;
        }
    }

    private void doComputeSalaryData(Long sRecordId, Boolean isSyncInsuranceData, Boolean isSyncAttendanceData,
                                     List<Long> employeeIds,
                                     String progressKey) {
        updateComputeProgress(progressKey, 8, COMPUTE_STATUS_RUNNING, "PREPARE",
                "正在校验薪资配置与社保数据", null, null);
        HrmSalaryMonthRecord salaryMonthRecord = getSalaryMonthRecordOrThrow(sRecordId);
        HrmSalaryConfig salaryConfig = getSalaryConfigOrThrow();

        int year = salaryMonthRecord.getYear();
        int month = salaryMonthRecord.getMonth();
        boolean syncInsuranceData = Boolean.TRUE.equals(isSyncInsuranceData);
        boolean syncAttendanceData = Boolean.TRUE.equals(isSyncAttendanceData);
        updateAdditionForComputeScope(employeeIds, year, month);
        List<String> initialPrecheckErrors = collectInsuranceDataErrors(isSyncInsuranceData, salaryConfig, year, month);
        SalaryComputeBatchData batchData = prepareSalaryComputeBatchData(salaryMonthRecord, employeeIds,
                year, month, syncAttendanceData, isSyncInsuranceData, salaryConfig, progressKey, initialPrecheckErrors);
        int totalCount = batchData != null && batchData.employeeMapList != null ? batchData.employeeMapList.size() : 0;
        updateComputeProgress(progressKey, 18, COMPUTE_STATUS_RUNNING, "LOAD_DATA",
                "已加载核算范围，共" + totalCount + "人", 0, totalCount);

        Set<String> midMonthPromotionReviewSet = new LinkedHashSet<>();

        SalaryComputeContext ctx = buildSalaryComputeContext(year, month, syncInsuranceData, syncAttendanceData,
                salaryConfig, batchData);
        List<EmployeeSalaryResult> results = computeEmployeeSalaryResults(batchData.employeeMapList, sRecordId,
                salaryMonthRecord, ctx, batchData.hasAttendanceGroupMap, midMonthPromotionReviewSet,
                progressKey, totalCount);

        updateComputeProgress(progressKey, 96, COMPUTE_STATUS_RUNNING, "PERSIST",
                "正在写入核算结果", totalCount, totalCount);
        batchSaveResults(results);
        finishSalaryCompute(year, month, midMonthPromotionReviewSet, salaryMonthRecord);
    }

    public static List<Long> normalizeComputeEmployeeIds(Long employeeId, List<Long> employeeIds) {
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(employeeIds)) {
            for (Long selectedEmployeeId : employeeIds) {
                if (selectedEmployeeId != null) {
                    normalizedIds.add(selectedEmployeeId);
                }
            }
        } else if (employeeId != null) {
            normalizedIds.add(employeeId);
        }
        if (normalizedIds.size() > MAX_COMPUTE_EMPLOYEE_COUNT) {
            throw new HrmException(6001, "穿梭框右侧人员不能超过50人");
        }
        return new ArrayList<>(normalizedIds);
    }

    public static String buildComputeScopeKey(List<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return "ALL";
        }
        return employeeIds.size() == 1
                ? String.valueOf(employeeIds.get(0))
                : "BATCH:" + employeeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static List<Long> resolveExportEmployeeIds(Collection<Long> candidateEmployeeIds,
                                                      Collection<Long> selectedEmployeeIds) {
        if (CollUtil.isEmpty(candidateEmployeeIds)) {
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(selectedEmployeeIds)) {
            return candidateEmployeeIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        Set<Long> selectedIdSet = selectedEmployeeIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return candidateEmployeeIds.stream()
                .filter(Objects::nonNull)
                .filter(selectedIdSet::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private void updateAdditionForComputeScope(List<Long> employeeIds, int year, int month) {
        if (CollUtil.isEmpty(employeeIds)) {
            updateAddition(null, year, month);
            return;
        }
        for (Long employeeId : employeeIds) {
            updateAddition(employeeId, year, month);
        }
    }

    private SalaryComputeBatchData prepareSalaryComputeBatchData(HrmSalaryMonthRecord salaryMonthRecord,
                                                                 List<Long> employeeIds,
                                                                 int year,
                                                                 int month,
                                                                 boolean syncAttendanceData,
                                                                 Boolean isSyncInsuranceData,
                                                                 HrmSalaryConfig salaryConfig,
                                                                 String progressKey,
                                                                 List<String> inheritedPrecheckErrors) {
        EmployeeComputeScopeData employeeScopeData = loadEmployeeComputeScopeData(
                salaryMonthRecord, employeeIds, year, month, syncAttendanceData, progressKey, inheritedPrecheckErrors);
        SalaryOptionBatchData salaryOptionBatchData = loadSalaryOptionBatchData();
        Map<Long, HrmProduceAttendance> produceAttendanceMap = resolveProduceAttendanceMap(
                employeeScopeData.attendanceSyncBatchData, year, month);
        Map<Long, BigDecimal> expectedAttendanceDaysByEmployee = resolveExpectedAttendanceDaysByEmployeeForCompute(
                employeeScopeData, year, month);
        HistoryComputeData historyData = loadHistoryComputeData(employeeScopeData.employeeMapList, year, month,
                isSyncInsuranceData, salaryConfig);
        return new SalaryComputeBatchData(employeeScopeData.employeeMapList, employeeScopeData.attendanceDataMap,
                employeeScopeData.hasAttendanceGroupMap,
                salaryOptionBatchData, produceAttendanceMap, expectedAttendanceDaysByEmployee,
                historyData.lastMonthTaxDataMap, historyData.socialSecurityEmpRecordMap, historyData.additionalDeductionMap,
                historyData.midMonthArchivesOptionMap);
    }

    private EmployeeComputeScopeData loadEmployeeComputeScopeData(HrmSalaryMonthRecord salaryMonthRecord,
                                                                  List<Long> employeeIds,
                                                                  int year,
                                                                  int month,
                                                                  boolean syncAttendanceData,
                                                                  String progressKey,
                                                                  List<String> inheritedPrecheckErrors) {
        List<Map<String, Object>> employeeMapList = queryHasSalaryArchivesEmployeeList(salaryMonthRecord, employeeIds);
        markSalaryRecordAsCreated(salaryMonthRecord, employeeMapList.size());
        Map<String, Map<Integer, String>> attendanceDataMap = resolveAttendanceData(employeeMapList);
        List<String> employeePrecheckErrors = mergePrecheckErrors(inheritedPrecheckErrors,
                collectComputePrerequisiteErrors(employeeMapList, attendanceDataMap, false, null, null));
        if (!syncAttendanceData) {
            failComputePrecheck(progressKey, employeePrecheckErrors);
        }
        Map<Long, Boolean> hasAttendanceGroupMap = loadHasAttendanceGroupMap(employeeMapList);
        AttendanceSyncBatchData attendanceSyncBatchData = prepareAttendanceDataForCompute(syncAttendanceData, year, month,
                employeeMapList, attendanceDataMap, hasAttendanceGroupMap, progressKey, employeePrecheckErrors);
        failComputePrecheck(progressKey, mergePrecheckErrors(employeePrecheckErrors,
                collectComputePrerequisiteErrors(employeeMapList, attendanceDataMap, false, null, null)));
        return new EmployeeComputeScopeData(employeeMapList, attendanceDataMap, hasAttendanceGroupMap, attendanceSyncBatchData);
    }

    private HistoryComputeData loadHistoryComputeData(List<Map<String, Object>> employeeMapList,
                                                      int year,
                                                      int month,
                                                      Boolean isSyncInsuranceData,
                                                      HrmSalaryConfig salaryConfig) {
        Map<Long, Map<Integer, String>> lastMonthTaxDataMap = loadLastMonthTaxDataMap(employeeMapList, year, month);
        Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap = loadSocialSecurityEmpRecordMap(
                employeeMapList, year, month, isSyncInsuranceData, salaryConfig);
        Map<Long, HrmAdditional> additionalDeductionMap = loadAdditionalDeductionMap(employeeMapList, year, month);
        Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap = loadMidMonthArchivesOptionMap(employeeMapList);
        return new HistoryComputeData(lastMonthTaxDataMap, socialSecurityEmpRecordMap,
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

    private Map<Long, BigDecimal> resolveExpectedAttendanceDaysByEmployeeForCompute(EmployeeComputeScopeData employeeScopeData,
                                                                                   int year,
                                                                                   int month) {
        if (employeeScopeData == null) {
            return Collections.emptyMap();
        }
        if (employeeScopeData.attendanceSyncBatchData != null) {
            return employeeScopeData.attendanceSyncBatchData.expectedAttendanceDaysByEmployee;
        }
        List<Long> employeeIds = employeeScopeData.employeeMapList.stream()
                .filter(Objects::nonNull)
                .map(map -> Convert.toLong(map.get("employeeId")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        return buildExpectedAttendanceDaysByEmployee(
                loadOvertimeNightStatisticsDetails(year, month, employeeIds));
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
                .expectedAttendanceDaysByEmployee(batchData.expectedAttendanceDaysByEmployee)
                .optionParentCodeMap(batchData.salaryOptionBatchData.optionParentCodeMap)
                .salaryOptionConfigMap(batchData.salaryOptionBatchData.salaryOptionConfigMap)
                .lastMonthTaxDataMap(batchData.lastMonthTaxDataMap)
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
                                                                    Set<String> midMonthPromotionReviewSet,
                                                                    String progressKey,
                                                                    int totalCount) {
        List<EmployeeSalaryResult> results = new ArrayList<>(employeeMapList.size());
        if (totalCount <= 0) {
            updateComputeProgress(progressKey, 95, COMPUTE_STATUS_RUNNING, "CALCULATE",
                    "未查询到可核算员工，正在收尾", 0, 0);
            return results;
        }
        int processedCount = 0;
        for (Map<String, Object> map : employeeMapList) {
            EmployeeSalaryResult result = computeSingleEmployeeSalaryResult(
                    map, sRecordId, salaryMonthRecord, ctx, hasAttendanceGroupMap, midMonthPromotionReviewSet);
            if (result != null) {
                results.add(result);
            }
            processedCount++;
            int progress = 20 + (int) Math.floor((processedCount * 75.0d) / totalCount);
            updateComputeProgress(progressKey, Math.min(progress, 95), COMPUTE_STATUS_RUNNING, "CALCULATE",
                    "正在核算员工 " + processedCount + "/" + totalCount,
                    processedCount, totalCount);
        }
        updateComputeProgress(progressKey, 95, COMPUTE_STATUS_RUNNING, "CALCULATE",
                "员工核算完成，正在汇总", totalCount, totalCount);
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
                                                                    Map<Long, Boolean> hasAttendanceGroupMap,
                                                                    String progressKey,
                                                                    List<String> inheritedPrecheckErrors) {
        if (!syncAttendanceData) {
            return null;
        }
        attendanceDataMap.clear();
        AttendanceSyncBatchData batchData = loadAttendanceSyncBatchData(year, month, employeeMapList);
        if (batchData == null) {
            return null;
        }
        failComputePrecheck(progressKey, mergePrecheckErrors(inheritedPrecheckErrors,
                collectComputePrerequisiteErrors(employeeMapList, null, true, batchData.attendanceRule, batchData.salaryBasic)));
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

    /** 编程式事务入口：生产走 TransactionTemplate；单测子类可覆写为直接执行（无事务） */
    protected <T> T runInTransaction(java.util.concurrent.Callable<T> callable) {
        return transactionTemplate.execute(status -> {
            try {
                return callable.call();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
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
            // 锁对象常驻 COMPUTE_RECORD_LOCK_MAP（键数量有限，内存可忽略）；
            // 严禁在此按 hasQueuedThreads 判断后 remove：B 线程已取到旧锁未入队时，
            // A 线程 remove 后 C 线程会创建新锁对象，B/C 并行进入临界区破坏互斥
            lock.unlock();
        }
    }

    private <T> T withSalaryRecordLocks(Collection<Long> sRecordIds, java.util.concurrent.Callable<T> callable) {
        if (CollUtil.isEmpty(sRecordIds)) {
            try {
                return callable.call();
            } catch (HrmException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        List<Long> normalizedIds = sRecordIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        List<Long> lockedIds = new ArrayList<>();
        try {
            for (Long sRecordId : normalizedIds) {
                ReentrantLock lock = COMPUTE_RECORD_LOCK_MAP.computeIfAbsent(sRecordId, key -> new ReentrantLock());
                lock.lock();
                lockedIds.add(sRecordId);
            }
            return callable.call();
        } catch (HrmException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            for (int i = lockedIds.size() - 1; i >= 0; i--) {
                Long sRecordId = lockedIds.get(i);
                ReentrantLock lock = COMPUTE_RECORD_LOCK_MAP.get(sRecordId);
                if (lock == null) {
                    continue;
                }
                // 与 withSalaryRecordLock 相同：只解锁不 remove，保证同一 sRecordId 永远同一把锁
                lock.unlock();
            }
        }
    }

    public SalaryComputeProgressVO queryComputeProgress(Long sRecordId, Long employeeId) {
        return queryComputeProgress(sRecordId, employeeId, null);
    }

    public SalaryComputeProgressVO queryComputeProgress(Long sRecordId, Long employeeId, List<Long> employeeIds) {
        clearExpiredComputeProgress();
        String progressKey = buildComputeProgressKey(sRecordId, normalizeComputeEmployeeIds(employeeId, employeeIds));
        SalaryComputeProgressState state = SALARY_COMPUTE_PROGRESS_MAP.get(progressKey);
        if (state == null) {
            SalaryComputeProgressVO vo = new SalaryComputeProgressVO();
            vo.setProgress(0);
            vo.setStatus(COMPUTE_STATUS_IDLE);
            vo.setStage("PREPARE");
            vo.setMessage("等待开始");
            vo.setProcessedCount(0);
            vo.setTotalCount(0);
            vo.setDone(false);
            vo.setSuccess(false);
            vo.setErrors(Collections.emptyList());
            return vo;
        }
        SalaryComputeProgressVO vo = new SalaryComputeProgressVO();
        vo.setProgress(state.progress);
        vo.setStatus(state.status);
        vo.setStage(state.stage);
        vo.setMessage(state.message);
        vo.setProcessedCount(state.processedCount);
        vo.setTotalCount(state.totalCount);
        boolean done = COMPUTE_STATUS_SUCCESS.equals(state.status) || COMPUTE_STATUS_FAILED.equals(state.status);
        vo.setDone(done);
        vo.setSuccess(COMPUTE_STATUS_SUCCESS.equals(state.status));
        vo.setErrors(state.errors == null ? Collections.emptyList() : new ArrayList<>(state.errors));
        return vo;
    }

    private String buildComputeProgressKey(Long sRecordId, List<Long> employeeIds) {
        LoginUserInfo info = CompanyContext.get();
        String companyId = info != null && StrUtil.isNotBlank(info.getCompanyId())
                ? info.getCompanyId() : "default";
        String scope = buildComputeScopeKey(employeeIds);
        return companyId + ":" + sRecordId + ":" + scope;
    }

    private void updateComputeProgress(String progressKey,
                                       Integer progress,
                                       String status,
                                       String stage,
                                       String message,
                                       Integer processedCount,
                                       Integer totalCount) {
        updateComputeProgress(progressKey, progress, status, stage, message, processedCount, totalCount, null);
    }

    private void updateComputeProgress(String progressKey,
                                       Integer progress,
                                       String status,
                                       String stage,
                                       String message,
                                       Integer processedCount,
                                       Integer totalCount,
                                       List<String> errors) {
        if (StrUtil.isBlank(progressKey)) {
            return;
        }
        SalaryComputeProgressState state = SALARY_COMPUTE_PROGRESS_MAP
                .computeIfAbsent(progressKey, key -> new SalaryComputeProgressState());
        if (progress != null) {
            state.progress = Math.max(0, Math.min(100, progress));
        }
        if (StrUtil.isNotBlank(status)) {
            state.status = status;
        }
        if (StrUtil.isNotBlank(stage)) {
            state.stage = stage;
        }
        if (StrUtil.isNotBlank(message)) {
            state.message = message;
        }
        if (processedCount != null) {
            state.processedCount = Math.max(0, processedCount);
        }
        if (totalCount != null) {
            state.totalCount = Math.max(0, totalCount);
        }
        if (errors != null) {
            state.errors = errors.stream()
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
        } else if (COMPUTE_STATUS_SUCCESS.equals(status)) {
            state.errors = Collections.emptyList();
        }
        state.updateTime = System.currentTimeMillis();
    }

    private void clearExpiredComputeProgress() {
        long now = System.currentTimeMillis();
        SALARY_COMPUTE_PROGRESS_MAP.entrySet().removeIf(entry -> {
            SalaryComputeProgressState state = entry.getValue();
            if (state == null) {
                return true;
            }
            return now - state.updateTime > SALARY_COMPUTE_PROGRESS_TTL_MS;
        });
    }

    private String resolveComputeErrorMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof HrmException) {
            return ((HrmException) root).getMsg();
        }
        String message = root != null ? root.getMessage() : null;
        return StrUtil.isNotBlank(message) ? message : "核算失败";
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
        HrmProduceAttendance midMonthAttendance = ctx.getProduceAttendanceMap().get(employeeId);
        BigDecimal welfareTaxableIncome = extractWelfareTaxableIncome(midMonthAttendance);
        collectMidMonthPromotionAttendanceReview(map, year, month, becomeDate, midMonthAttendance, midMonthPromotionReviewSet);
        boolean canCountOvertimeNight = isFixedRestProductionEmployee(map);
        boolean isJoinAttendance = hasAttendanceGroup
                || ENTRY_STATUS_QUIT.equals(String.valueOf(map.get("entryStatus") != null ? map.get("entryStatus") : ""));

        EmpRecordWithOptions ro = getOrCreateRecordAndApplyAttendance(employeeId, jobNumber, sRecordId, salaryMonthRecord,
                year, month, ctx.getAttendanceDataMap(), ctx.getNoFixedSalaryOptionList(), isJoinAttendance,
                ctx.getIsSyncAttendanceData(), becomeDate);
        BigDecimal normalDays = resolveSalaryNormalDaysForCompute(ctx, employeeId);

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

        BigDecimal taxOnlyBonusSalaryForRemark = getTaxOnlyBonusSalary(employeeId, year, month);
        boolean hasAnnualDeductionRemark = hasAnnualDeductionRemark(map);

        // 半路转正：按日比例拆分薪资 + 移除全勤奖和工会费 + 一致性校验
        if (isMidMonthPromotion(becomeDate, year, month)) {
            if (normalDays == null) {
                throw missingSalaryExpectedAttendanceDaysException(employeeId, employeeVO.getEmployeeName(),
                        jobNumber, year, month);
            }
            processMidMonthPromotionSalary(employeeId, year, month, finalOptions,
                    becomeDate, midMonthAttendance, normalDays, lastMonthTaxData,
                    baseOptionMap, isDisabled, hasAnnualDeductionRemark, taxSpecialAdditionalDeduction,
                    welfareTaxableIncome, taxOnlyBonusSalaryForRemark, canCountOvertimeNight,
                    ctx.getMidMonthArchivesOptionMap());
            // 第二次移除：processMidMonthPromotionSalary 可能重新写入全勤奖/工会费到 finalOptions，需再次清理
            removeFullAttendanceAndUnionFeeForMidMonthPromotion(year, month, finalOptions, becomeDate);
            applyMidMonthPromotionSummaryConsistency(map, ro.record, finalOptions, baseOptionMap, lastMonthTaxData,
                    isDisabled, year, month, becomeDate, welfareTaxableIncome, ctx.getOptionParentCodeMap());
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
                                                          Map<Integer, Integer> optionParentCodeMap) {
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
        BigDecimal taxOnlyBonusSalary = getTaxOnlyBonusSalary(salaryMonthEmpRecord.getEmployeeId(), year, month);
        boolean hasAnnualDeductionRemark = hasAnnualDeductionRemark(employeeMap);

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
                taxOnlyBonusSalary,
                includeBonusInCumulativeIncome,
                IS_DISABLED_NO.equals(isDisabled),
                hasAnnualDeductionRemark,
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

    private static boolean hasAnnualDeductionRemark(Map<String, Object> employeeMap) {
        // isRemark 已在 queryHasSalaryArchivesEmployeeList 中批量预加载到 employeeMap，无需逐条查询DB
        Integer isRemark = null;
        if (employeeMap != null && employeeMap.get("isRemark") != null) {
            isRemark = Convert.toInt(employeeMap.get("isRemark"), null);
        }
        return SalaryComputeServiceNew.hasAnnualDeductionRemark(isRemark);
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
            Map<Integer, String> cv = requireAttendanceCodeValueMap(attendanceDataMap, employeeId, jobNumber);
            record = new HrmSalaryMonthEmpRecord();
            record.setSRecordId(salaryMonthRecord.getSRecordId());
            record.setEmployeeId(employeeId);
            record.setActualWorkDay(readAttendanceDecimal(cv, 2, "应计出勤天数", employeeId, jobNumber));
            record.setNeedWorkDay(readAttendanceDecimal(cv, 1, "应出勤天数", employeeId, jobNumber));
            record.setYear(year);
            record.setMonth(month);
            salaryMonthEmpRecordService.save(record);
            options = new ArrayList<>(getNoFixedOptionValue(record, noFixedSalaryOptionList, true, becomeDate));
            existed = false;
        }
        try {
            Map<Integer, String> cv = requireAttendanceCodeValueMap(attendanceDataMap, employeeId, jobNumber);
            if (Boolean.TRUE.equals(isSyncAttendanceData)) {
                record.setActualWorkDay(readAttendanceDecimal(cv, 2, "应计出勤天数", employeeId, jobNumber));
                record.setNeedWorkDay(readAttendanceDecimal(cv, 1, "应出勤天数", employeeId, jobNumber));
                // updateById 延迟到 batchSaveResults 统一批量执行
                cv.remove(1);
                cv.remove(2);
                options.addAll(getFixedOptionValue(record, cv, isJoinAttendance));
            } else if (!existed) {
                record.setNeedWorkDay(readAttendanceDecimal(cv, 1, "应出勤天数", employeeId, jobNumber));
                cv.remove(1);
                cv.remove(2);
                options.addAll(getFixedOptionValue(record, cv, isJoinAttendance));
            } else {
                options.addAll(cloneOptionValuesForRecord(oldFixedOptionValueList, record.getSEmpRecordId()));
            }
        } catch (HrmException e) {
            throw e;
        } catch (Exception e) {
            throw new HrmException(HrmCodeEnum.ATTENDANCE_DATA_ERROR);
        }
        return new EmpRecordWithOptions(record, options, existed);
    }

    private Map<Integer, String> requireAttendanceCodeValueMap(Map<String, Map<Integer, String>> attendanceDataMap,
                                                               Long employeeId,
                                                               String jobNumber) {
        if (StrUtil.isBlank(jobNumber)) {
            throw new HrmException(6001, "员工ID " + employeeId + " 缺少工号，请先在员工档案补充工号。");
        }
        Map<Integer, String> codeValueMap = attendanceDataMap != null ? attendanceDataMap.get(jobNumber) : null;
        if (codeValueMap == null) {
            throw new HrmException(6001, "员工ID " + employeeId + "（工号" + jobNumber + "）缺少考勤数据，请先同步考勤或补齐考勤汇总。");
        }
        return codeValueMap;
    }

    private BigDecimal readAttendanceDecimal(Map<Integer, String> codeValueMap,
                                             Integer code,
                                             String fieldName,
                                             Long employeeId,
                                             String jobNumber) {
        String value = codeValueMap != null ? codeValueMap.get(code) : null;
        if (StrUtil.isBlank(value)) {
            throw new HrmException(6001, "员工ID " + employeeId + "（工号" + jobNumber + "）" + fieldName + "为空，请检查考勤汇总数据。");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new HrmException(6001, "员工ID " + employeeId + "（工号" + jobNumber + "）" + fieldName + "不是数字，请检查考勤汇总数据。");
        }
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
    Double normalDays;
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
    BigDecimal fullMoney = resolveFullAttendanceMoney(map.get("fullMoney"));//员工对应的全勤奖金额
    String fullAttendanceFlag = String.valueOf((Integer)map.get("isFullAttendance"));//员工是否享有全勤
    String entryStatus = String.valueOf(map.get("entryStatus"));//员工是否离职
    Date entryDate = (Date) map.get("entryDate");//入职日期
        boolean isProduce = isProductionAffiliationSystem(map);
        boolean canCountOvertimeNight = isFixedRestProductionEmployee(map);
    BigDecimal expectedAttendanceDays = requireSalaryExpectedAttendanceDays(
            batchData.expectedAttendanceDaysByEmployee,
            employeeId,
            employeeName,
            jobNumber,
            year,
            month);
    normalDays = expectedAttendanceDays.doubleValue();
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
    BigDecimal salaryAttendanceDays = resolveSalaryAttendanceDays(
            hrmProduceAttendance, batchData.accruedAttendanceDaysByEmployee, employeeId);
    BigDecimal fullAttendanceExpectedDays = expectedAttendanceDays;
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
    //员工当月排班时长（从批量预加载数据获取，避免循环内逐条查询）
    HashMap<String,Double> workTimsMap = getWorkHoursFromBatch(employeeId, isProduce, batchData.dates, batchData.workHoursMap);
    //病假天数。病假 2 天内不扣工资，但仍需作为扣全勤依据，必须早于全勤兜底计算。
    leaveOfsickDays = sickDeductDays(isProduce, employeeSummaryDayList, workTimsMap, "2", deductionVOList);
    //判断是否为全勤（使用批量考勤数据）- 不区分残疾员工，所有员工都可以获得满勤奖
    isFullAttendance = checkIsFullAttendance(employeeSummaryDayList, empAttendanceSummary, deductionVOList, expandProduction);
    if (!isFullAttendance && shouldFallbackFullAttendanceByAccruedDays(
            salaryAttendanceDays, fullAttendanceExpectedDays, empAttendanceSummary, leaveOfsickDays)) {
        isFullAttendance = true;
    }

    //没有考勤组的员工存在非满勤的情况
    if(!hasAttendanceGroup && !"4".equals(entryStatus))
    {
        //如果员工没有放在考勤组，则工资计算排除考勤数据
        empAttendanceMap.put(1, expectedAttendanceDays.toPlainString());
        empAttendanceMap.put(2, salaryAttendanceDays != null ? salaryAttendanceDays.toPlainString()
                : expectedAttendanceDays.toPlainString());
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
                    empAttendanceMap.put(40102, fullMoney.toPlainString());
                }else if (employeeId.toString().equals("1712718940181") || employeeId.toString().equals("1712718940179") || employeeId.toString().equals("1789114659308232706")) {
                    //董事长和总经理、张宏海直接全勤
                    empAttendanceMap.put(40102, fullMoney.toPlainString());
                }
            }
        }


                        attendanceDataMap.put(jobNumber, empAttendanceMap);

                        //存在可能出现离职员工没有考勤记录但是需要取夜班和加班数据（使用批量加载的 salaryBasic）
                        getYeBanAndJiaBan(hrmProduceAttendance, empAttendanceMap, batchData.salaryBasic, canCountOvertimeNight);

        return;
    }
    //根据员工的工号查询员工定薪/调薪数据（来自批量加载）
    QuerySalaryArchivesListVO salaryArchivesOption = batchData.empSalaryArchivesMap.get(employeeId);
    if(salaryArchivesOption != null)
    {
        empSalary = new BigDecimal(Double.parseDouble(salaryArchivesOption.getTotal()));
    }
    //事假天数
    leaveOfAbsenceDays = sickDeductDays(isProduce, employeeSummaryDayList, workTimsMap, "1", deductionVOList);

    //获取生效的扣款规则（默认标记优先，见 selectEffectiveAttendanceRule）
    HrmAttendanceRule hrmAttendanceRule = batchData.attendanceRule;
    if (hrmAttendanceRule == null) {
        hrmAttendanceRule = selectEffectiveAttendanceRule();
    }


    //加班天数
//                    overTimeCount = attendanceClockService.queryEmpAttendanceOverTimeCountDays(Arrays.asList(LocalDateTimeUtil.of(dateStartTime).toLocalDate(), LocalDateTimeUtil.of(dateEndTime).toLocalDate()), employeeId);
    //这里直接用钉钉的考勤统计数据（来自批量加载）
    if(empAttendanceSummary == null)
    {
        attendanceEmpRecordMap.put("normalDays", normalDays);
        attendanceEmpRecordMap.put("actualWorkDay",
                salaryAttendanceDays != null ? salaryAttendanceDays.doubleValue() : normalDays);
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
        if(salaryAttendanceDays != null)
        {
            // 薪资核算实际计薪天数使用“应计出勤天数”口径。
            empAttendanceSummary.setActualityDays(salaryAttendanceDays.doubleValue());

        }
        // 如果应计出勤天数小于应出勤天数，则表示该员工没有满勤。
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
                // 入职/离职场景使用应出勤天数 normalDays - 应计出勤天数，得到未上班天数并赋值给 AbsenteeismDays。
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
            // 如果钉钉打卡数据没有异常，仍可能存在手动维护的应计出勤天数和应出勤天数不一样。
            empAttendanceSummary.setAbsenteeismDays(AbsenteeismDays);
        }else if (empAttendanceSummary.getBingjia() > 0) {
            if (info != null && "0001".equals(info.getCompanyId())) {
                //病假存在先补贴再扣钱（旷工扣款显示）的情况
                empAttendanceSummary.setAbsenteeismDays(empAttendanceSummary.getBingjia());
            }
        }
        attendanceEmpRecordMap.put("normalDays", normalDays);//应出勤天数
        attendanceEmpRecordMap.put("actualWorkDay", empAttendanceSummary.getActualityDays());//应计出勤天数
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
    //计算早退（2026-09-06 确认：早退扣款上不封顶，原封顶分支误用迟到单价，已整体移除）
    if (hrmAttendanceRule.getEarlyRuleMethod() == ONE) {
        earlyMoney = hrmAttendanceRule.getEarlyDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("earlyMinute").toString()));
    } else if (hrmAttendanceRule.getEarlyRuleMethod() == TWO) {
        earlyMoney = hrmAttendanceRule.getEarlyDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("earlyCount").toString()));
    } else if (hrmAttendanceRule.getEarlyRuleMethod() == THREE) {
        earlyMoney = hrmAttendanceRule.getEarlyDeductMoney();
    }

    //缺卡扣款
    BigDecimal misscardMoney = hrmAttendanceRule.getMisscardDeductMoney().multiply(new BigDecimal(attendanceEmpRecordMap.get("misscardCount").toString())).setScale(2, RoundingMode.HALF_UP);
    //旷工扣款 员工每天的工资 * 旷工天数
    //取整算法
    BigDecimal absenteeismMoney = new BigDecimal(ZERO);
    BigDecimal normalDaysDecimal = BigDecimal.valueOf(normalDays);
    if (info != null && ("0001".equals(info.getCompanyId()) || "0005".equals(info.getCompanyId()) || "0003".equals(info.getCompanyId()))) {
        //精确小数算法
        absenteeismMoney = empSalary.divide(normalDaysDecimal,4, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("absenteeismDays").toString())).setScale(2, RoundingMode.HALF_UP);
    }else {
        //取整算法
        absenteeismMoney = empSalary.divide(normalDaysDecimal, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("absenteeismDays").toString())).setScale(0, RoundingMode.HALF_UP);
    }

    //事假扣款 事假天数*每天的工资
    BigDecimal leaveOfAbsenceMoney = new BigDecimal(ZERO);
    if (info != null && "0001".equals(info.getCompanyId())) {
        //精确小数算法
        leaveOfAbsenceMoney =empSalary.divide(normalDaysDecimal,4, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("leaveOfAbsence").toString())).setScale(2, RoundingMode.HALF_UP);
    }else {
        //取整算法
        leaveOfAbsenceMoney = empSalary.divide(normalDaysDecimal, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(attendanceEmpRecordMap.get("leaveOfAbsence").toString())).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 获取夜班补贴（使用批量加载的 salaryBasic）
     */
    getYeBanAndJiaBan(hrmProduceAttendance, empAttendanceMap, batchData.salaryBasic, canCountOvertimeNight);
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
            empAttendanceMap.put(40102, fullMoney.toPlainString());//全勤奖
        }
    }
    else
    {
        if(isFullAttendance  && status!=null && status==1 && "1".equals(fullAttendanceFlag))
        {
            empAttendanceMap.put(40102, fullMoney.toPlainString());//全勤奖
        }
    }
//                    成都公司试用期也有全勤
    if (info != null && "0002".equals(info.getCompanyId())) {
        if(isFullAttendance  && status!=null && status==2 && "1".equals(fullAttendanceFlag))
        {
            empAttendanceMap.put(40102, fullMoney.toPlainString());//全勤奖
        }
    }
    //市场入职默认有全勤（从 map 获取 deptId，从批量预加载的部门数据获取部门名称，避免循环内逐条查询）
    Long empDeptId = Convert.toLong(map.get("deptId"));
    if (empDeptId != null && empDeptId.toString().equals("1481534121629855751")) {
        String empDeptName = batchData.deptNameMap.get(empDeptId);
        if (empDeptName != null && empDeptName.contains("总监")) {
            empAttendanceMap.put(40102, fullMoney.toPlainString());//全勤奖
        } else {
            empAttendanceMap.put(40102, fullMoney.toPlainString());
        }
    }
    empAttendanceMap.put(1, attendanceEmpRecordMap.get("normalDays").toString());//应出勤天数
    empAttendanceMap.put(2, attendanceEmpRecordMap.get("actualWorkDay").toString());//应计出勤天数
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
        BigDecimal oneDayMoney = baseSalary.divide(normalDaysDecimal, 2, RoundingMode.HALF_UP);
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
        return queryHasSalaryArchivesEmployeeList(salaryMonthRecord,
                employeeId == null ? Collections.emptyList() : Collections.singletonList(employeeId));
    }

    /**
     * 查询有薪资档案的人员
     * @return
     */
    public List<Map<String, Object>> queryHasSalaryArchivesEmployeeList(HrmSalaryMonthRecord salaryMonthRecord, List<Long> selectedEmployeeIds)
    {
        List<Map<String, Object>> employeeList = new ArrayList<>();
        if (salaryMonthRecord == null) {
            return employeeList;
        }
        Collection<Long> dataAuthEmployeeIds = new ArrayList<>();
        if(CollUtil.isNotEmpty(selectedEmployeeIds))
        {
            dataAuthEmployeeIds.addAll(selectedEmployeeIds.stream().filter(Objects::nonNull).collect(Collectors.toList()));
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

    /**
     * 查询“核算薪资”同口径的计薪员工列表（与 computeSalaryData 使用同一逻辑）
     * @param sRecordId 薪资月记录ID，为空时取最新一条
     * @return 计薪员工列表
     */
    public List<Map<String, Object>> queryComputeSalaryEmployeeList(Long sRecordId) {
        HrmSalaryMonthRecord salaryMonthRecord;
        if (sRecordId != null) {
            salaryMonthRecord = getById(sRecordId);
        } else {
            salaryMonthRecord = queryLastSalaryMonthRecord();
        }
        if (salaryMonthRecord == null) {
            return new ArrayList<>();
        }
        return queryHasSalaryArchivesEmployeeList(salaryMonthRecord, Collections.emptyList());
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
            // 新租户可能未配置薪资信息，直接返回 null 避免 NPE
            if (salaryConfig == null || salaryConfig.getSalaryStartMonth() == null) {
                return null;
            }
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

    HrmSalaryMonthRecord findLatestSalaryMonthRecord() {
        return lambdaQuery().orderByDesc(HrmSalaryMonthRecord::getCreateTime).last("limit 1").one();
    }

    HrmSalaryMonthRecord findSalaryMonthRecordByYearAndMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        return lambdaQuery()
                .eq(HrmSalaryMonthRecord::getYear, year)
                .eq(HrmSalaryMonthRecord::getMonth, month)
                .orderByDesc(HrmSalaryMonthRecord::getCreateTime)
                .last("limit 1")
                .one();
    }

    HrmSalaryMonthRecord createNextSalaryMonthRecord(HrmSalaryMonthRecord sourceSalaryMonthRecord) {
        if (sourceSalaryMonthRecord == null || sourceSalaryMonthRecord.getYear() == null || sourceSalaryMonthRecord.getMonth() == null) {
            throw new HrmException(6001, "薪资月记录不存在");
        }
        int[] nextYearMonth = getNextMonthYearAndMonth(sourceSalaryMonthRecord.getYear(), sourceSalaryMonthRecord.getMonth());
        HrmSalaryMonthRecord nextSalaryMonthRecord = findSalaryMonthRecordByYearAndMonth(nextYearMonth[0], nextYearMonth[1]);
        if (nextSalaryMonthRecord != null) {
            return nextSalaryMonthRecord;
        }
        HrmSalaryConfig salaryConfig = hrmSalaryConfigService.getOne(Wrappers.emptyWrapper());
        LocalDate startTime = LocalDate.of(nextYearMonth[0], nextYearMonth[1], salaryConfig.getSalaryCycleStartDay());
        LocalDate endTime;
        if (salaryConfig.getSalaryCycleStartDay() > 1) {
            LocalDate dateTime = LocalDateTimeUtil.offset(startTime.atStartOfDay(), 1, ChronoUnit.MONTHS).toLocalDate();
            endTime = LocalDate.of(dateTime.getYear(), dateTime.getMonthValue(), salaryConfig.getSalaryCycleEndDay());
        } else {
            endTime = startTime.with(TemporalAdjusters.lastDayOfMonth());
        }
        HrmSalaryMonthRecord salaryMonthRecord = new HrmSalaryMonthRecord();
        salaryMonthRecord.setTitle(HrmLanguageEnum.parseName(nextYearMonth[1]) + HrmLanguageEnum.SALARY_REPORT.getName());
        salaryMonthRecord.setYear(nextYearMonth[0]);
        salaryMonthRecord.setMonth(nextYearMonth[1]);
        salaryMonthRecord.setStartTime(startTime);
        salaryMonthRecord.setEndTime(endTime);
        salaryMonthRecord.setCreateTime(LocalDateTime.now());
        salaryMonthRecord.setNum(queryPaySalaryEmployeeListByType(1, null).size());
        save(salaryMonthRecord);
        salaryActionRecordService.addNextMonthSalaryLog(salaryMonthRecord);
        return salaryMonthRecord;
    }

    public SalaryMonthRecoveryPreviewVO previewSalaryMonthRecovery(Integer year, Integer month) {
        validateSalaryMonthRecoveryRequest(year, month);
        HrmSalaryMonthRecord targetRecord = findSalaryMonthRecordByYearAndMonth(year, month);
        if (targetRecord == null) {
            throw new HrmException(6001, "目标薪资月份不存在");
        }

        List<HrmSalaryMonthRecord> laterMonthRecords = new ArrayList<>(findSalaryMonthRecordsAfter(year, month));
        laterMonthRecords.sort(this::compareSalaryMonthRecordForRecovery);

        List<SalaryMonthRecoveryRecordVO> laterRecordVOList = laterMonthRecords.stream()
                .map(this::buildSalaryMonthRecoveryRecordVO)
                .collect(Collectors.toList());
        List<String> blockingReasons = laterRecordVOList.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> item.getBlockingReasons().stream())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        long targetEmployeeRecordCount = countRecoveryEmployeeRecords(targetRecord.getSRecordId());
        boolean targetHasSentSlipData = isSalaryMonthRecordSent(targetRecord)
                || countRecoverySalarySlipRecords(targetRecord.getSRecordId()) > 0
                || countRecoverySalarySlips(targetRecord.getYear(), targetRecord.getMonth()) > 0;
        Integer restoredCheckStatus = resolveRecoveredTargetCheckStatus(targetRecord, targetEmployeeRecordCount, targetHasSentSlipData);

        SalaryMonthRecoveryPreviewVO preview = new SalaryMonthRecoveryPreviewVO();
        preview.setYear(targetRecord.getYear());
        preview.setMonth(targetRecord.getMonth());
        preview.setYearMonth(formatRecoveryYearMonth(targetRecord.getYear(), targetRecord.getMonth()));
        preview.setTargetRecordId(targetRecord.getSRecordId());
        preview.setTargetCheckStatus(targetRecord.getCheckStatus());
        preview.setTargetCheckStatusName(resolveSalaryRecordStatusName(targetRecord.getCheckStatus()));
        preview.setRestoredCheckStatus(restoredCheckStatus);
        preview.setRestoredCheckStatusName(resolveSalaryRecordStatusName(restoredCheckStatus));
        preview.setLaterRecords(laterRecordVOList);
        preview.setBlockingReasons(blockingReasons);
        preview.setRecoverable(CollUtil.isNotEmpty(laterRecordVOList) && CollUtil.isEmpty(blockingReasons));
        preview.setMessage(buildSalaryMonthRecoveryPreviewMessage(preview));
        return preview;
    }

    // 锁在事务外获取（同 computeSalaryData），锁内用 TransactionTemplate 开事务
    public SalaryMonthRecoveryResultVO recoverSalaryMonth(Integer year, Integer month) {
        validateSalaryMonthRecoveryRequest(year, month);
        HrmSalaryMonthRecord targetRecord = findSalaryMonthRecordByYearAndMonth(year, month);
        if (targetRecord == null) {
            throw new HrmException(6001, "目标薪资月份不存在");
        }

        return withSalaryRecordLock(targetRecord.getSRecordId(), () ->
            runInTransaction(() -> {
            SalaryMonthRecoveryPreviewVO firstPreview = previewSalaryMonthRecovery(year, month);
            List<Long> candidateRecordIds = firstPreview.getLaterRecords().stream()
                    .map(SalaryMonthRecoveryRecordVO::getSRecordId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return withSalaryRecordLocks(candidateRecordIds, () -> {
                SalaryMonthRecoveryPreviewVO lockedPreview = previewSalaryMonthRecovery(year, month);
                if (CollUtil.isEmpty(lockedPreview.getLaterRecords())) {
                    return buildSalaryMonthRecoveryResult(lockedPreview, Collections.emptyList());
                }
                if (!Boolean.TRUE.equals(lockedPreview.getRecoverable())) {
                    throw new HrmException(6001, buildSalaryMonthRecoveryBlockedMessage(lockedPreview));
                }

                List<Long> recordIdsToDelete = lockedPreview.getLaterRecords().stream()
                        .map(SalaryMonthRecoveryRecordVO::getSRecordId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                removeRecoverySalaryMonthRecords(recordIdsToDelete);

                HrmSalaryMonthRecord refreshedTargetRecord = findSalaryMonthRecordByYearAndMonth(year, month);
                if (refreshedTargetRecord != null
                        && lockedPreview.getRestoredCheckStatus() != null
                        && !Objects.equals(refreshedTargetRecord.getCheckStatus(), lockedPreview.getRestoredCheckStatus())) {
                    refreshedTargetRecord.setCheckStatus(lockedPreview.getRestoredCheckStatus());
                    updateRecoveredTargetMonthRecord(refreshedTargetRecord);
                }

                return buildSalaryMonthRecoveryResult(lockedPreview, recordIdsToDelete);
            });
            })
        );
    }

    List<HrmSalaryMonthRecord> findSalaryMonthRecordsAfter(Integer year, Integer month) {
        if (year == null || month == null) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .and(wrapper -> wrapper.gt(HrmSalaryMonthRecord::getYear, year)
                        .or()
                        .eq(HrmSalaryMonthRecord::getYear, year)
                        .gt(HrmSalaryMonthRecord::getMonth, month))
                .orderByAsc(HrmSalaryMonthRecord::getYear)
                .orderByAsc(HrmSalaryMonthRecord::getMonth)
                .orderByAsc(HrmSalaryMonthRecord::getCreateTime)
                .list();
    }

    long countRecoveryEmployeeRecords(Long sRecordId) {
        if (sRecordId == null) {
            return 0L;
        }
        return salaryMonthEmpRecordService.lambdaQuery()
                .eq(HrmSalaryMonthEmpRecord::getSRecordId, sRecordId)
                .count();
    }

    long countRecoverySalarySlipRecords(Long sRecordId) {
        if (sRecordId == null) {
            return 0L;
        }
        return salarySlipRecordMapper.selectCount(Wrappers.<HrmSalarySlipRecord>lambdaQuery()
                .eq(HrmSalarySlipRecord::getSRecordId, sRecordId));
    }

    long countRecoverySalarySlips(Integer year, Integer month) {
        if (year == null || month == null) {
            return 0L;
        }
        return salarySlipMapper.selectCount(Wrappers.<HrmSalarySlip>lambdaQuery()
                .eq(HrmSalarySlip::getYear, year)
                .eq(HrmSalarySlip::getMonth, month));
    }

    void removeRecoverySalaryMonthRecords(List<Long> recordIds) {
        if (CollUtil.isEmpty(recordIds)) {
            return;
        }
        removeByIds(recordIds);
    }

    void updateRecoveredTargetMonthRecord(HrmSalaryMonthRecord targetRecord) {
        updateById(targetRecord);
    }

    private void validateSalaryMonthRecoveryRequest(Integer year, Integer month) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new HrmException(6001, "请选择正确的目标薪资月份");
        }
    }

    private SalaryMonthRecoveryRecordVO buildSalaryMonthRecoveryRecordVO(HrmSalaryMonthRecord record) {
        SalaryMonthRecoveryRecordVO vo = new SalaryMonthRecoveryRecordVO();
        if (record == null) {
            vo.setMessage("薪资月记录不存在");
            vo.setBlockingReasons(Collections.singletonList("薪资月记录不存在"));
            vo.setRecoverable(false);
            return vo;
        }
        long employeeRecordCount = countRecoveryEmployeeRecords(record.getSRecordId());
        long salarySlipRecordCount = countRecoverySalarySlipRecords(record.getSRecordId());
        long salarySlipCount = countRecoverySalarySlips(record.getYear(), record.getMonth());

        List<String> blockingReasons = collectSalaryMonthRecoveryBlockingReasons(
                record, employeeRecordCount, salarySlipRecordCount, salarySlipCount);
        vo.setSRecordId(record.getSRecordId());
        vo.setYear(record.getYear());
        vo.setMonth(record.getMonth());
        vo.setYearMonth(formatRecoveryYearMonth(record.getYear(), record.getMonth()));
        vo.setTitle(record.getTitle());
        vo.setCheckStatus(record.getCheckStatus());
        vo.setCheckStatusName(resolveSalaryRecordStatusName(record.getCheckStatus()));
        vo.setIsSend(record.getIsSend());
        vo.setEmployeeRecordCount(employeeRecordCount);
        vo.setSalarySlipRecordCount(salarySlipRecordCount);
        vo.setSalarySlipCount(salarySlipCount);
        vo.setBlockingReasons(blockingReasons);
        vo.setRecoverable(CollUtil.isEmpty(blockingReasons));
        vo.setMessage(Boolean.TRUE.equals(vo.getRecoverable())
                ? "可恢复：该月份未生成员工薪资明细和工资条，恢复时会删除该薪资月记录。"
                : String.join("；", blockingReasons));
        return vo;
    }

    private List<String> collectSalaryMonthRecoveryBlockingReasons(HrmSalaryMonthRecord record,
                                                                    long employeeRecordCount,
                                                                    long salarySlipRecordCount,
                                                                    long salarySlipCount) {
        if (record == null) {
            return Collections.singletonList("薪资月记录不存在");
        }
        List<String> reasons = new ArrayList<>();
        String monthText = formatRecoveryChineseMonth(record.getYear(), record.getMonth());
        if (employeeRecordCount > 0) {
            reasons.add(monthText + "已有员工薪资明细，不能自动恢复。");
        }
        if (salarySlipRecordCount > 0) {
            reasons.add(monthText + "已有工资条发放记录，不能自动恢复。");
        }
        if (salarySlipCount > 0) {
            reasons.add(monthText + "已有工资条明细，不能自动恢复。");
        }
        if (isSalaryMonthRecordSent(record)) {
            reasons.add(monthText + "月记录已标记发送工资条，不能自动恢复。");
        }
        return reasons;
    }

    private String buildSalaryMonthRecoveryPreviewMessage(SalaryMonthRecoveryPreviewVO preview) {
        if (preview == null) {
            return "恢复预览失败";
        }
        if (CollUtil.isEmpty(preview.getLaterRecords())) {
            return "未发现目标月份之后的薪资月记录，无需恢复。";
        }
        if (!Boolean.TRUE.equals(preview.getRecoverable())) {
            return "存在不能自动恢复的后续薪资月份，请先查看预览原因。";
        }
        String restoreStatusText = "";
        if (!Objects.equals(preview.getTargetCheckStatus(), preview.getRestoredCheckStatus())) {
            restoreStatusText = "，目标月份状态将恢复为" + preview.getRestoredCheckStatusName();
        }
        return "可恢复，恢复时将删除目标月份之后的" + preview.getLaterRecords().size()
                + "条空薪资月记录" + restoreStatusText + "。";
    }

    private String buildSalaryMonthRecoveryBlockedMessage(SalaryMonthRecoveryPreviewVO preview) {
        if (preview == null || CollUtil.isEmpty(preview.getBlockingReasons())) {
            return "存在不能自动恢复的薪资月份，请先查看预览原因。";
        }
        return "存在不能自动恢复的薪资月份：" + preview.getBlockingReasons().get(0);
    }

    private SalaryMonthRecoveryResultVO buildSalaryMonthRecoveryResult(SalaryMonthRecoveryPreviewVO preview,
                                                                       List<Long> deletedRecordIds) {
        SalaryMonthRecoveryResultVO result = new SalaryMonthRecoveryResultVO();
        result.setYear(preview.getYear());
        result.setMonth(preview.getMonth());
        result.setYearMonth(preview.getYearMonth());
        result.setTargetRecordId(preview.getTargetRecordId());
        result.setRestoredCheckStatus(preview.getRestoredCheckStatus());
        result.setRestoredCheckStatusName(preview.getRestoredCheckStatusName());
        result.setDeletedRecordIds(deletedRecordIds == null ? Collections.emptyList() : new ArrayList<>(deletedRecordIds));
        List<String> deletedMonths = preview.getLaterRecords().stream()
                .map(SalaryMonthRecoveryRecordVO::getYearMonth)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        result.setDeletedMonths(deletedMonths);
        if (CollUtil.isEmpty(deletedMonths)) {
            result.setMessage("未发现需要恢复的后续薪资月份。");
        } else {
            result.setMessage("已恢复到" + preview.getYearMonth() + "，删除后续空薪资月份：" + String.join("、", deletedMonths) + "。");
        }
        return result;
    }

    private Integer resolveRecoveredTargetCheckStatus(HrmSalaryMonthRecord targetRecord,
                                                       long targetEmployeeRecordCount,
                                                       boolean targetHasSentSlipData) {
        if (targetRecord == null) {
            return null;
        }
        Integer currentStatus = targetRecord.getCheckStatus();
        if (!Objects.equals(currentStatus, SalaryRecordStatus.HISTORY.getValue()) || targetHasSentSlipData) {
            return currentStatus;
        }
        return targetEmployeeRecordCount > 0
                ? SalaryRecordStatus.COMPUTE.getValue()
                : SalaryRecordStatus.CREATED.getValue();
    }

    private boolean isSalaryMonthRecordSent(HrmSalaryMonthRecord record) {
        return record != null && Objects.equals(record.getIsSend(), ONE);
    }

    private String resolveSalaryRecordStatusName(Integer status) {
        if (status == null) {
            return "未设置";
        }
        for (SalaryRecordStatus recordStatus : SalaryRecordStatus.values()) {
            if (recordStatus.getValue() == status) {
                return recordStatus.getName();
            }
        }
        return "未知状态(" + status + ")";
    }

    private int compareSalaryMonthRecordForRecovery(HrmSalaryMonthRecord left, HrmSalaryMonthRecord right) {
        int leftIndex = toRecoveryMonthIndex(left);
        int rightIndex = toRecoveryMonthIndex(right);
        if (leftIndex != rightIndex) {
            return Integer.compare(leftIndex, rightIndex);
        }
        Long leftId = left == null || left.getSRecordId() == null ? 0L : left.getSRecordId();
        Long rightId = right == null || right.getSRecordId() == null ? 0L : right.getSRecordId();
        return Long.compare(leftId, rightId);
    }

    private int toRecoveryMonthIndex(HrmSalaryMonthRecord record) {
        if (record == null) {
            return Integer.MIN_VALUE;
        }
        return toRecoveryMonthIndex(record.getYear(), record.getMonth());
    }

    private int toRecoveryMonthIndex(Integer year, Integer month) {
        if (year == null || month == null) {
            return Integer.MIN_VALUE;
        }
        return year * 12 + month;
    }

    private String formatRecoveryYearMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return "";
        }
        return year + "-" + String.format("%02d", month);
    }

    private String formatRecoveryChineseMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return "该月份";
        }
        return year + "年" + month + "月";
    }

    /**
     * 创建下月薪资表
     * @return
     */
    // 锁在事务外获取（同 computeSalaryData），锁内用 TransactionTemplate 开事务
    public OperationLog addNextMonthSalary() {
        HrmSalaryMonthRecord lastSalaryMonthRecord = findLatestSalaryMonthRecord();
        if (lastSalaryMonthRecord == null) {
            throw new HrmException(6001, "薪资月记录不存在");
        }
        return withSalaryRecordLock(lastSalaryMonthRecord.getSRecordId(), () ->
            runInTransaction(() -> {
            HrmSalaryMonthRecord lockedLastSalaryMonthRecord = getById(lastSalaryMonthRecord.getSRecordId());
            if (lockedLastSalaryMonthRecord == null) {
                throw new HrmException(6001, "薪资月记录不存在");
            }
            lockedLastSalaryMonthRecord.setCheckStatus(SalaryRecordStatus.HISTORY.getValue());
            computeSalaryCount(lockedLastSalaryMonthRecord);
            updateById(lockedLastSalaryMonthRecord);
            HrmSalaryMonthRecord salaryMonthRecord = createNextSalaryMonthRecord(lockedLastSalaryMonthRecord);
            OperationLog operationLog = new OperationLog();
            operationLog.setOperationObject(salaryMonthRecord.getSRecordId(), salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle());
            operationLog.setOperationInfo("新建薪资报表：" + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle());
            return operationLog;
            })
        );

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
        optionHeadVOList.add(new SalaryOptionHeadVO(2, "应计出勤天数", 1));
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
        optionHeadVOList.add(new SalaryOptionHeadVO(2, "应计出勤天数", 1));
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

    public static List<String> collectComputePrerequisiteErrors(List<Map<String, Object>> employeeMapList,
                                                                Map<String, Map<Integer, String>> attendanceDataMap,
                                                                boolean syncAttendanceData,
                                                                HrmAttendanceRule attendanceRule,
                                                                HrmSalaryBasic salaryBasic) {
        List<String> errors = new ArrayList<>();
        if (CollUtil.isEmpty(employeeMapList)) {
            errors.add("未查询到可核算员工，请确认本次选择范围内有已入职且有薪资档案的员工。");
        } else {
            int index = 0;
            for (Map<String, Object> employeeMap : employeeMapList) {
                index++;
                String employeeLabel = formatComputeEmployeeLabel(employeeMap, index);
                Long employeeId = employeeMap == null ? null : Convert.toLong(employeeMap.get("employeeId"));
                String employeeName = employeeMap == null ? "" : Convert.toStr(employeeMap.get("employeeName"), "");
                String jobNumber = employeeMap == null ? "" : Convert.toStr(employeeMap.get("jobNumber"), "");
                if (employeeId == null) {
                    errors.add(employeeLabel + "缺少员工ID，请检查员工档案。");
                }
                if (StrUtil.isBlank(employeeName)) {
                    errors.add(employeeLabel + "缺少员工姓名，请检查员工档案。");
                }
                if (StrUtil.isBlank(jobNumber)) {
                    errors.add(employeeLabel + "缺少工号，请先在员工档案补充工号。");
                    continue;
                }
                if (attendanceDataMap != null) {
                    Map<Integer, String> attendanceMap = attendanceDataMap.get(jobNumber);
                    if (attendanceMap == null) {
                        errors.add(employeeLabel + "缺少考勤数据，请先同步考勤或补齐考勤汇总。");
                        continue;
                    }
                    addAttendanceNumberError(errors, employeeLabel, attendanceMap, 1, "应出勤天数");
                    addAttendanceNumberError(errors, employeeLabel, attendanceMap, 2, "应计出勤天数");
                }
            }
        }
        if (syncAttendanceData) {
            if (attendanceRule == null) {
                errors.add("未配置考勤扣款规则，请先在考勤规则中维护迟到、早退、缺卡等扣款规则。");
            }
            if (salaryBasic == null) {
                errors.add("未配置基本工资金额设置，请先维护最低基本工资、每小时加班费和夜班补贴。");
            } else {
                if (salaryBasic.getSalaryBasic() == null) {
                    errors.add("基本工资金额设置缺少最低基本工资，请先补充后再核算。");
                }
                if (salaryBasic.getOvertimePay() == null) {
                    errors.add("基本工资金额设置缺少每小时加班费，请先补充后再核算。");
                }
                if (salaryBasic.getSubsidy() == null) {
                    errors.add("基本工资金额设置缺少夜班补贴，请先补充后再核算。");
                }
            }
        }
        return errors.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private static void addAttendanceNumberError(List<String> errors,
                                                 String employeeLabel,
                                                 Map<Integer, String> attendanceMap,
                                                 Integer code,
                                                 String fieldName) {
        String value = attendanceMap.get(code);
        if (StrUtil.isBlank(value) || !isValidDecimal(value)) {
            errors.add(employeeLabel + fieldName + "为空或不是数字，请检查考勤汇总数据。");
        }
    }

    private static boolean isValidDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String formatComputeEmployeeLabel(Map<String, Object> employeeMap, int index) {
        if (employeeMap == null) {
            return "第" + index + "名计薪员工";
        }
        String employeeName = Convert.toStr(employeeMap.get("employeeName"), "");
        String jobNumber = Convert.toStr(employeeMap.get("jobNumber"), "");
        Long employeeId = Convert.toLong(employeeMap.get("employeeId"));
        String base = StrUtil.isNotBlank(employeeName) ? employeeName : "第" + index + "名计薪员工";
        if (StrUtil.isNotBlank(jobNumber)) {
            return base + "（工号" + jobNumber + "）";
        }
        if (employeeId != null) {
            return base + "（员工ID " + employeeId + "）";
        }
        return base;
    }

    private void failComputePrecheck(String progressKey, List<String> errors) {
        if (CollUtil.isEmpty(errors)) {
            return;
        }
        String message = buildComputePrecheckMessage(errors);
        updateComputeProgress(progressKey, null, COMPUTE_STATUS_FAILED, "ERROR",
                message, null, null, errors);
        throw new HrmException(6001, message);
    }

    private static List<String> mergePrecheckErrors(List<String> first, List<String> second) {
        List<String> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(first)) {
            result.addAll(first);
        }
        if (CollUtil.isNotEmpty(second)) {
            result.addAll(second);
        }
        return result.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private static String buildComputePrecheckMessage(List<String> errors) {
        int count = errors == null ? 0 : errors.size();
        return "核算前发现" + count + "个问题，请在进度窗口查看明细并一次性处理后重新核算。";
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

    // 锁在事务外获取（同 computeSalaryData），锁内用 TransactionTemplate 开事务
    public OperationResult updateCheckStatus(Integer checkStatus, Integer year, Integer month) {
        HrmSalaryMonthRecord sourceSalaryMonthRecord = findSalaryMonthRecordByYearAndMonth(year, month);
        if (sourceSalaryMonthRecord == null) {
            throw new HrmException(6001, "薪资月记录不存在");
        }
        return withSalaryRecordLock(sourceSalaryMonthRecord.getSRecordId(), () ->
            runInTransaction(() -> {
            LambdaUpdateWrapper<HrmSalaryMonthRecord> wrapper = new LambdaUpdateWrapper<HrmSalaryMonthRecord>()
                    .set(HrmSalaryMonthRecord::getCheckStatus, checkStatus)
                    .eq(HrmSalaryMonthRecord::getYear, year)
                    .and(i -> i.eq(HrmSalaryMonthRecord::getMonth, month));
            update(null, wrapper);

            HrmSalaryMonthRecord lockedSourceSalaryMonthRecord = getById(sourceSalaryMonthRecord.getSRecordId());
            if (lockedSourceSalaryMonthRecord == null) {
                throw new HrmException(6001, "薪资月记录不存在");
            }
            lockedSourceSalaryMonthRecord.setCheckStatus(SalaryRecordStatus.HISTORY.getValue());
            computeSalaryCount(lockedSourceSalaryMonthRecord);
            updateById(lockedSourceSalaryMonthRecord);
            createNextSalaryMonthRecord(lockedSourceSalaryMonthRecord);
            return null;
            })
        );
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

                HrmSalaryMonthRecord salaryMonthRecordNext = createNextSalaryMonthRecord(salaryMonthRecord);
                operationLog.setOperationObject(salaryMonthRecordNext.getSRecordId(), salaryMonthRecordNext.getYear() + "-" + salaryMonthRecordNext.getTitle());
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


    public static BigDecimal resolveFullAttendanceMoney(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text);
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
            //行政体系按固定 8 小时工作日计算。
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
                leavelDays = new BigDecimal(sickHours).divide(new BigDecimal(8)).setScale(2, RoundingMode.HALF_UP);
            }
            else if("1".equals(type))
            {
                //获取员工对应的事假时间
                Double shiJiaHours = employeeDayAttendanceList.stream().mapToDouble(HrmAttendanceSummaryDayVo::getShijia).sum();
                //行政体系按一天 8 小时折算事假天数。
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
        //按员工保存工资项明细值（键 = 部门|姓名），用于生成导出批注的具体说明
        Map<String, Map<Integer, String>> exportCommentDetailByEmp = new HashMap<>();

        List<Long> employeeIds = new ArrayList<>();
        //查询出已经定薪了的人员列表
        LocalDate nows = LocalDate.now().withDayOfMonth(1);
        employeeIds.addAll(employeeService.lambdaQuery().select(HrmEmployee::getEmployeeId).lt(HrmEmployee::getEntryTime, nows).ne(HrmEmployee::getIsDel, 1).orderByDesc(HrmEmployee::getDeptId).list()
                .stream().map(HrmEmployee::getEmployeeId).collect(Collectors.toList()));
        employeeIds = resolveExportEmployeeIds(employeeIds, querySalaryExportDto.getEmployeeIds());
        //填充数据
        QuerySalaryPageListDto querySalaryPageListDto = new QuerySalaryPageListDto();
        querySalaryPageListDto.setSRecordId(querySalaryExportDto.getSalaryRecordId());
        BeanUtils.copyProperties(querySalaryExportDto,querySalaryPageListDto);
        fillDeptIdsWithChildren(querySalaryPageListDto);
        List<QuerySalaryPageListVO> salaryPageListVOS = salaryMonthEmpRecordMapper.querySalaryMonthList(querySalaryPageListDto,employeeIds);
        if (!CollectionUtil.isEmpty(salaryPageListVOS)) {
            List<Long> exportEmployeeIds = salaryPageListVOS.stream()
                    .map(QuerySalaryPageListVO::getEmployeeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            List<HrmOvertimeNightStatisticsDetail> exportOvertimeNightDetails =
                    loadOvertimeNightStatisticsDetails(year, month, exportEmployeeIds);
            Map<Long, BigDecimal> exportFullWorkDaysByEmployee = buildExportFullWorkDaysByEmployee(exportOvertimeNightDetails);
            Map<Long, BigDecimal> exportAbsenceDaysByEmployee = buildExportAbsenceDaysByEmployee(exportOvertimeNightDetails);
            for(QuerySalaryPageListVO vo : salaryPageListVOS) {
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
                //记录该员工全部工资项明细值，供导出批注使用
                Map<Integer, String> empOptionValueMap = new HashMap<>();
                for (ComputeSalaryDto optionValue : list) {
                    if (optionValue != null && optionValue.getCode() != null && optionValue.getValue() != null) {
                        empOptionValueMap.put(optionValue.getCode(), optionValue.getValue());
                    }
                }
                exportCommentDetailByEmp.put(vo.getDeptName() + "|" + vo.getEmployeeName(), empOptionValueMap);
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
                BigDecimal fullWorkDays = resolveExportFullWorkDays(
                        exportFullWorkDaysByEmployee, vo.getEmployeeId());
                BigDecimal absenceDays = resolveExportAbsenceDays(
                        exportAbsenceDaysByEmployee, vo.getEmployeeId());
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9007, fullWorkDays.toString(), 1, "满勤天数"));
                salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 9008, absenceDays.toString(), 1, "超缺勤天数"));
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

        //为每名员工生成五类批注的具体说明文本（全勤奖、超缺勤、个税、工会费、其他补贴）
        if (!CollectionUtil.isEmpty(salaryExportList)) {
            for (HrmSalaryExport export : salaryExportList) {
                if ("小计".equals(export.getEmpname()) || "合计".equals(export.getEmpname())) {
                    continue;
                }
                Map<Integer, String> empOptionValueMap = exportCommentDetailByEmp.get(
                        export.getDeptname() + "|" + export.getEmpname());
                if (empOptionValueMap == null) {
                    empOptionValueMap = exportCommentDetailByEmp.get(export.getEmpname());
                }
                if (empOptionValueMap == null) {
                    continue;
                }
                export.setFullAttendanceComment(buildFullAttendanceCommentText(export, empOptionValueMap));
                export.setAbsenceComment(buildAbsenceCommentText(export, empOptionValueMap));
                export.setTaxComment(buildTaxCommentText(empOptionValueMap));
                export.setUnionFeesComment(buildUnionFeesCommentText(export, empOptionValueMap));
                export.setOtherSubsidyComment(buildOtherSubsidyCommentText(empOptionValueMap));
            }
        }

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
        try {
            WriteSheet writeSheet = EasyExcel.
                    writerSheet("Sheet1")
//                    .registerWriteHandler(new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle))
//                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(new CustomCellWriteHandler(xjIndexList))
                    .registerWriteHandler(new SalaryExportCommentWriteHandler(salaryExportList))
//                      .registerWriteHandler(new CustomCellWriteHeightConfig())
                    .build();
            //writeSheet.setRelativeHeadRowIndex(1);

            FillConfig fillConfig = FillConfig.builder()
                    // 开启填充换行
                    .forceNewRow(true)
                    .build();

//            writeSheet.setHead(headTitles);
            //excelWriter.write(salaryExportList, writeSheet);
            SalaryExportHeader headInfoVo = new SalaryExportHeader();
            companyName = info.getDepName();
            headInfoVo.setTitle(companyName+""+year+"年"+month+"月工资表");
            headInfoVo.setSalaryMonth(year+"年"+month+"月");
            headInfoVo.setCompanyName(companyName);
            excelWriter.fill(new FillWrapper("header", Collections.singletonList(headInfoVo)), writeSheet);
            excelWriter.fill(salaryExportList, fillConfig, writeSheet);
        } finally {
            // 异常时也必须 finish，否则 POI 临时资源与模板流不释放
            excelWriter.finish();
            if (is != null) {
                try {
                    is.close();
                } catch (java.io.IOException ignore) {
                }
            }
        }

    }


    /**考勤扣款各项工资项编号：迟到*/
    private static final int CODE_LATE_DEDUCTION = 190101;
    /**早退*/
    private static final int CODE_EARLY_DEDUCTION = 190102;
    /**旷工*/
    private static final int CODE_ABSENTEEISM_DEDUCTION = 190103;
    /**病假*/
    private static final int CODE_SICK_LEAVE_DEDUCTION = 19010401;
    /**事假*/
    private static final int CODE_PERSONAL_LEAVE_DEDUCTION = 19010402;
    /**缺卡*/
    private static final int CODE_MISS_CARD_DEDUCTION = 190105;
    /**其他补贴*/
    private static final int CODE_OTHER_SUBSIDY = 281;
    /**个税累计：收入/减除费用/专项扣除/专项附加扣除/应纳税所得额/应纳税额/已缴税额*/
    private static final int CODE_TAX_CUMULATIVE_INCOME = 270101;
    private static final int CODE_TAX_CUMULATIVE_DEDUCTION = 270102;
    private static final int CODE_TAX_CUMULATIVE_SPECIAL = 270103;
    private static final int CODE_TAX_CUMULATIVE_ADDITIONAL = 270104;
    private static final int CODE_TAX_CUMULATIVE_TAXABLE = 270105;
    private static final int CODE_TAX_CUMULATIVE_PAYABLE = 270106;
    private static final int CODE_TAX_CUMULATIVE_PAID = 250105;

    private static String commentAmount(Map<Integer, String> empOptionValueMap, int code) {
        String value = empOptionValueMap.get(code);
        if (value == null || value.trim().isEmpty()) {
            return "0";
        }
        try {
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private static boolean commentAmountPositive(Map<Integer, String> empOptionValueMap, int code) {
        return new BigDecimal(commentAmount(empOptionValueMap, code)).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 全勤奖批注：没有全勤奖时给出该员工的具体原因线索
     */
    private static String buildFullAttendanceCommentText(HrmSalaryExport export, Map<Integer, String> empOptionValueMap) {
        BigDecimal fullAttendance = export.getFullattendancesalary() == null ? BigDecimal.ZERO : export.getFullattendancesalary();
        StringBuilder text = new StringBuilder();
        text.append("全勤奖是员工当月满勤发放的奖励，金额在【薪资管理-基本工资设置】中按员工设定。");
        if (fullAttendance.compareTo(BigDecimal.ZERO) > 0) {
            text.append("该员工本月满足满勤条件（已转正、无有效病假、出勤达标），获得全勤奖 ")
                .append(fullAttendance.toPlainString()).append(" 元。");
        } else {
            text.append("本月没有全勤奖，原因：");
            boolean hasReason = false;
            if (commentAmountPositive(empOptionValueMap, CODE_SICK_LEAVE_DEDUCTION)) {
                text.append("本月有病假扣款（").append(commentAmount(empOptionValueMap, CODE_SICK_LEAVE_DEDUCTION))
                    .append(" 元），按规则取消全勤；");
                hasReason = true;
            }
            if (export.getAbsencehours() != null && export.getAbsencehours().compareTo(BigDecimal.ZERO) != 0) {
                text.append("本月存在超缺勤（").append(export.getAbsencehours().toPlainString())
                    .append(" 天），出勤不满；");
                hasReason = true;
            }
            text.append("其他可能：入职后尚未转正或当月才转正、基本工资设置中未启用全勤奖");
            if (!hasReason) {
                text.append("——请结合该员工的转正状态和基本工资设置核对");
            }
            text.append("。当前满勤天数：");
            text.append(export.getNormaldays() == null || export.getNormaldays().trim().isEmpty()
                    ? "0" : export.getNormaldays());
            text.append(" 天。");
        }
        return text.toString();
    }

    /**
     * 超缺勤工资批注：列出各项考勤扣款明细及合计
     */
    private static String buildAbsenceCommentText(HrmSalaryExport export, Map<Integer, String> empOptionValueMap) {
        StringBuilder text = new StringBuilder();
        text.append("超缺勤工资是因迟到、早退、旷工、请事假、请病假、缺卡等从工资中扣除的部分，")
            .append("超缺勤天数 =（应出勤天数 × 8 − 应计出勤小时）÷ 8，出勤数据来自考勤统计结果。");
        String late = commentAmount(empOptionValueMap, CODE_LATE_DEDUCTION);
        String early = commentAmount(empOptionValueMap, CODE_EARLY_DEDUCTION);
        String absenteeism = commentAmount(empOptionValueMap, CODE_ABSENTEEISM_DEDUCTION);
        String personalLeave = commentAmount(empOptionValueMap, CODE_PERSONAL_LEAVE_DEDUCTION);
        String sickLeave = commentAmount(empOptionValueMap, CODE_SICK_LEAVE_DEDUCTION);
        String missCard = commentAmount(empOptionValueMap, CODE_MISS_CARD_DEDUCTION);
        if (commentAmountPositive(empOptionValueMap, CODE_LATE_DEDUCTION)
                || commentAmountPositive(empOptionValueMap, CODE_EARLY_DEDUCTION)
                || commentAmountPositive(empOptionValueMap, CODE_ABSENTEEISM_DEDUCTION)
                || commentAmountPositive(empOptionValueMap, CODE_PERSONAL_LEAVE_DEDUCTION)
                || commentAmountPositive(empOptionValueMap, CODE_SICK_LEAVE_DEDUCTION)
                || commentAmountPositive(empOptionValueMap, CODE_MISS_CARD_DEDUCTION)) {
            text.append("本月扣款明细：迟到扣 ").append(late).append(" 元，早退扣 ").append(early)
                .append(" 元，旷工扣 ").append(absenteeism).append(" 元，事假扣 ").append(personalLeave)
                .append(" 元，病假扣 ").append(sickLeave).append(" 元（每月前2天病假不扣钱，超过部分按当地最低工资折算扣除），缺卡扣 ")
                .append(missCard).append(" 元。");
        } else {
            text.append("本月没有考勤扣款项");
            if (export.getAbsencesalary() != null && export.getAbsencesalary().compareTo(BigDecimal.ZERO) == 0) {
                text.append("，超缺勤扣款为0");
            }
            text.append("。");
        }
        text.append("当前超缺勤天数：")
            .append(export.getAbsencehours() == null ? "0" : export.getAbsencehours().toPlainString())
            .append(" 天，超缺勤扣款合计：")
            .append(export.getAbsencesalary() == null ? "0" : export.getAbsencesalary().toPlainString())
            .append(" 元。");
        return text.toString();
    }

    /**
     * 个人所得税批注：给出公式与该员工本月的具体计算过程
     */
    private static String buildTaxCommentText(Map<Integer, String> empOptionValueMap) {
        String income = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_INCOME);
        String deduction = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_DEDUCTION);
        String special = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_SPECIAL);
        String additional = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_ADDITIONAL);
        String taxable = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_TAXABLE);
        String payable = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_PAYABLE);
        String paid = commentAmount(empOptionValueMap, CODE_TAX_CUMULATIVE_PAID);

        StringBuilder text = new StringBuilder();
        text.append("个人所得税按“累计预扣”方法计算：把今年1月至本月的收入累加，")
            .append("减去每月5000元的固定减除费用、个人承担的社保和公积金、专项附加扣除，得到累计应纳税所得额；")
            .append("再按税率表算出累计应缴税额，减去之前月份已缴的税，就是本月要扣的个税。本月计算过程：")
            .append("累计收入 ").append(income).append(" 元 − 累计固定减除费用 ").append(deduction)
            .append(" 元 − 累计社保公积金 ").append(special).append(" 元 − 累计专项附加扣除 ").append(additional)
            .append(" 元 = 累计应纳税所得额 ").append(taxable).append(" 元。");
        if (new BigDecimal(taxable).compareTo(BigDecimal.ZERO) > 0) {
            int bracket = 6;
            BigDecimal[] thresholds = {new BigDecimal("36000"), new BigDecimal("144000"), new BigDecimal("300000"),
                    new BigDecimal("420000"), new BigDecimal("660000"), new BigDecimal("960000")};
            String[] rates = {"3%", "10%", "20%", "25%", "30%", "35%", "45%"};
            String[] quick = {"0", "2520", "16920", "31920", "52920", "85920", "181920"};
            for (int i = 0; i < thresholds.length; i++) {
                if (new BigDecimal(taxable).compareTo(thresholds[i]) <= 0) {
                    bracket = i;
                    break;
                }
            }
            text.append("适用税率 ").append(rates[bracket]).append("%，速算扣除数 ").append(quick[bracket])
                .append(" 元，累计应纳税额 ").append(payable).append(" 元 − 累计已缴税额 ").append(paid)
                .append(" 元 = 本月个税。");
        } else {
            text.append("累计应纳税所得额不超过0，本月无需缴纳个税。");
        }
        return text.toString();
    }

    /**
     * 工会费批注：有则给出计算式，没有则说明原因
     */
    private static String buildUnionFeesCommentText(HrmSalaryExport export, Map<Integer, String> empOptionValueMap) {
        BigDecimal unionFees = export.getUnionfees() == null ? BigDecimal.ZERO : export.getUnionfees();
        StringBuilder text = new StringBuilder();
        text.append("工会费按应发工资的0.5%收取，并受员工状态、转正时间等规则限制。");
        if (unionFees.compareTo(BigDecimal.ZERO) > 0) {
            text.append("本月计算：应发工资 ")
                .append(export.getTotalsalary() == null ? "0" : export.getTotalsalary().toPlainString())
                .append(" 元 × 0.5% = ").append(unionFees.toPlainString()).append(" 元。");
        } else {
            text.append("本月没有工会费，原因：应发工资为0或员工为实习、已离职、当月才转正，或所在公司免收工会费。")
                .append("当前应发工资：")
                .append(export.getTotalsalary() == null ? "0" : export.getTotalsalary().toPlainString())
                .append(" 元。");
        }
        return text.toString();
    }

    /**
     * 其他补贴批注：说明数据来自考勤管理的上传考勤
     */
    private static String buildOtherSubsidyCommentText(Map<Integer, String> empOptionValueMap) {
        String amount = commentAmount(empOptionValueMap, CODE_OTHER_SUBSIDY);
        StringBuilder text = new StringBuilder();
        text.append("其他补贴的数据来自【考勤管理】中的“每月考勤统计”（上传考勤）：")
            .append("由考勤报表文件导入该员工当月的“其他补贴”列，也可以在考勤统计页面上直接修改单元格保存，")
            .append("薪资核算时按该列金额发放。");
        if (new BigDecimal(amount).compareTo(BigDecimal.ZERO) > 0) {
            text.append("本月金额：").append(amount).append(" 元。");
        } else {
            text.append("本月该员工的其他补贴未填写或为0。");
        }
        return text.toString();
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
        fillDeptIdsWithChildren(querySalaryPageListDto);
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
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, vo.getActualWorkDay().toString(), 1, "应计出勤天数"));

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
                                   HrmSalaryBasic salaryBasic,boolean canCountOvertimeNight) {
        // 加班/夜班只对生产体系且固定月休4天员工生效；金额优先取考勤汇总已落库值，其次取基本工资设置。
        if (!canCountOvertimeNight) {
            empAttendanceMap.put(180101, "0");
            empAttendanceMap.put(180102, "0");
            return;
        }
        if(hrmProduceAttendance!=null)
        {
            //有加班费
            if (hrmProduceAttendance.getOvertimePay() != null)
            {
                empAttendanceMap.put(180101, hrmProduceAttendance.getOvertimePay().toString());//180101 加班费
            }
            else if (hrmProduceAttendance.getWorkOverTime()!=null)
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
            if(hrmProduceAttendance.getNightSubsidy()!=null)
            {
                empAttendanceMap.put(180102, hrmProduceAttendance.getNightSubsidy().toString());//180102为夜班补贴
            }
            else if(hrmProduceAttendance.getNightShift()!=null)
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
        List<String> errors = collectInsuranceDataErrors(isSyncInsuranceData, salaryConfig, year, month);
        if (CollUtil.isNotEmpty(errors)) {
            throw new HrmException(HrmCodeEnum.SOCIAL_SECURITY_DATA_IS_NOT_GENERATED_THIS_MONTH.getCode(), errors.get(0));
        }
    }

    private List<String> collectInsuranceDataErrors(Boolean isSyncInsuranceData, HrmSalaryConfig salaryConfig,
                                                    int year, int month) {
        List<String> errors = new ArrayList<>();
        if (!Boolean.TRUE.equals(isSyncInsuranceData)) {
            return errors;
        }
        //如果是同步社保数据，需要验证社保数据是否已生成
        Integer socialSecurityMonthType = salaryConfig != null ? salaryConfig.getSocialSecurityMonthType() : null;
        if (socialSecurityMonthType == null) {
            socialSecurityMonthType = ONE;
            logger.warn("薪资配置 socialSecurityMonthType 为空，按当月口径处理：{}-{}", year, month);
        }
        YearMonth socialSecurityYearMonth = resolveSocialSecurityReferenceYearMonth(
                socialSecurityMonthType, year, month);

        //查询社保数据是否生成
        Optional<HrmInsuranceMonthRecord> insuranceMonthRecordOpt = insuranceMonthRecordService.lambdaQuery()
                .eq(HrmInsuranceMonthRecord::getYear, socialSecurityYearMonth.getYear())
                .eq(HrmInsuranceMonthRecord::getMonth, socialSecurityYearMonth.getMonthValue())
                .oneOpt();

        if (!insuranceMonthRecordOpt.isPresent()) {
            errors.add("社保数据未生成：" + socialSecurityYearMonth.getYear() + "-"
                    + socialSecurityYearMonth.getMonthValue()
                    + " 的社保月记录不存在，请先生成社保报表或关闭“同步社保数据”。");
            return errors;
        }

        HrmInsuranceMonthRecord insuranceMonthRecord = insuranceMonthRecordOpt.get();
        if (!Integer.valueOf(IsEnum.YES.getValue()).equals(insuranceMonthRecord.getStatus())) {
            boolean hasActiveInsuranceEmpRecord = insuranceMonthEmpRecordService.lambdaQuery()
                    .eq(HrmInsuranceMonthEmpRecord::getYear, socialSecurityYearMonth.getYear())
                    .eq(HrmInsuranceMonthEmpRecord::getMonth, socialSecurityYearMonth.getMonthValue())
                    .eq(HrmInsuranceMonthEmpRecord::getStatus, IsEnum.YES.getValue())
                    .exists();
            if (!hasActiveInsuranceEmpRecord) {
                errors.add("社保数据未生成：" + insuranceMonthRecord.getYear() + "-"
                        + insuranceMonthRecord.getMonth()
                        + " 的社保月记录未完成，且没有有效员工社保明细，请先完成社保报表生成。");
                return errors;
            }
            logger.warn("社保月记录状态未完成但存在有效员工社保明细，放行薪资核算: {}-{}",
                    socialSecurityYearMonth.getYear(), socialSecurityYearMonth.getMonthValue());
        }
        return errors;
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
                                                                      boolean canCountOvertimeNight,
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
        return calculateMidMonthPromotionSalaryAmounts(
                probationSalaryMap,
                officialSalaryMap,
                hrmProduceAttendance,
                normalDays,
                canCountOvertimeNight);
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
     * @param becomeDate 转正日期
     * @param attendance 考勤数据
     * @param normalDays 应出勤天数
     * @param lastMonthTaxData 上月个税累计数据
     * @param baseOptionMap 首次落库前的完整基础项（含 100101/100102/280/282 等扣款项）
     * @param isDisabled 是否残疾员工（"1"=残疾免税）
     * @param hasAnnualDeductionRemark is_remark=2 时为 true，累计减除费用按全年60000
     * @param taxSpecialAdditionalDeduction 专项附加扣除合计
     * @param taxOnlyBonusSalary 只计税奖金，只参与个税累计收入
     */
    private void processMidMonthPromotionSalary(Long employeeId, int year, int month,
                                                List<HrmSalaryMonthOptionValue> optionValueList,
                                                LocalDate becomeDate, HrmProduceAttendance attendance,
                                                BigDecimal normalDays, Map<Integer, String> lastMonthTaxData,
                                                Map<Integer, String> baseOptionMap,
                                                String isDisabled, boolean hasAnnualDeductionRemark,
                                                BigDecimal taxSpecialAdditionalDeduction,
                                                BigDecimal welfareTaxableIncome,
                                                BigDecimal taxOnlyBonusSalary,
                                                boolean canCountOvertimeNight,
                                                Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap) {
        if (!isMidMonthPromotion(becomeDate, year, month) || CollUtil.isEmpty(optionValueList)) {
            return;
        }

        List<HrmSalaryArchivesOption> preloadedArchivesOptionList = midMonthArchivesOptionMap != null
                ? midMonthArchivesOptionMap.get(employeeId)
                : Collections.emptyList();
        Map<Integer, String> salaryMap = calculateMidMonthPromotionFullSalary(employeeId, year, month,
                becomeDate, attendance, normalDays, canCountOvertimeNight, preloadedArchivesOptionList);
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
                year, month, isDisabled, hasAnnualDeductionRemark, taxSpecialAdditionalDeduction,
                welfareTaxableIncome, taxOnlyBonusSalary);
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
        dataRow.put("满勤天数", getValueByCode(vo.getSalary(), 9007));
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
            dataRow.put("夜班补贴", resolveExportNightSubsidy(vo));
            dataRow.put("其他补贴", getValueByCode(vo.getSalary(), 281));
            dataRow.put("全勤奖", getValueByCode(vo.getSalary(), 40102));

            // 加班工资只允许生产体系且固定月休4天员工导出；历史薪资项中若已有不合格员工加班费，导出时强制归零。
            dataRow.put("加班工资", resolveExportOvertimePay(vo));
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

    private String resolveExportOvertimePay(QuerySalaryPageListVO vo) {
        if (vo == null || !isFixedRestProductionEmployee(vo.getAffiliationSystem(), vo.getRestType())) {
            return "0";
        }
        return getValueByCode(vo.getSalary(), 180101);
    }

    private String resolveExportNightSubsidy(QuerySalaryPageListVO vo) {
        if (vo == null || !isFixedRestProductionEmployee(vo.getAffiliationSystem(), vo.getRestType())) {
            return "0";
        }
        return getValueByCode(vo.getSalary(), 180102);
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
