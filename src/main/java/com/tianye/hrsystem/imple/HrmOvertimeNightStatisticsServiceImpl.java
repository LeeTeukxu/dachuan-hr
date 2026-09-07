package com.tianye.hrsystem.imple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.BaseUtil;
import com.tianye.hrsystem.entity.bo.QueryEmployeeOvertimeNightDetailBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightDailyDetailPageBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightStatisticsPageBO;
import com.tianye.hrsystem.entity.bo.UpdateOvertimeNightAttendanceBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryBasicMapper;
import com.tianye.hrsystem.modules.salary.support.HrmSalaryBasicDefaults;
import com.tianye.hrsystem.modules.workweek.service.HrmWorkweekSettingService;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekDayCalendarVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekMonthCalendarVO;
import com.tianye.hrsystem.entity.vo.DailyOvertimeNightDetailVO;
import com.tianye.hrsystem.entity.vo.EmployeeOvertimeNightMonthlyDetailVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightDailyDetailPageVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;
import com.tianye.hrsystem.model.HrmAttendanceClock;
import com.tianye.hrsystem.model.HrmAttendanceDateShift;
import com.tianye.hrsystem.model.HrmAttendancePlan;
import com.tianye.hrsystem.model.HrmAttendanceHistoryShift;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmEmployeeOverTimeRecord;
import com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail;
import com.tianye.hrsystem.model.HrmWorkPlanCustomShift;
import com.tianye.hrsystem.model.tbattendancedetail;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.hrmAttendanceClockRepository;
import com.tianye.hrsystem.repository.hrmAttendanceDateShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.hrmAttendanceHistoryShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmEmployeeOverTimeRecordRepository;
import com.tianye.hrsystem.repository.hrmOvertimeNightStatisticsDetailRepository;
import com.tianye.hrsystem.repository.hrmWorkPlanCustomShiftRepository;
import com.tianye.hrsystem.repository.tbattendancedetailRepository;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.model.HrmAttendanceGroup;
import com.tianye.hrsystem.service.IHrmOvertimeNightStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class HrmOvertimeNightStatisticsServiceImpl implements IHrmOvertimeNightStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(HrmOvertimeNightStatisticsServiceImpl.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DEBUG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEBUG_EMPLOYEE_NAME = "许泽刚";
    private static final LocalDate DEBUG_FOCUS_DATE = LocalDate.of(2026, 3, 20);
    private static final Path DEBUG_TRACE_PATH = Paths.get("logs", "hainan-overtime-debug.log").toAbsolutePath().normalize();
    private static final int AFFILIATION_SYSTEM_ADMINISTRATIVE = 1;
    private static final int AFFILIATION_SYSTEM_PRODUCTION = 2;
    private static final int REST_TYPE_WORKWEEK = 1;
    private static final int REST_TYPE_FIXED_MONTHLY_REST = 2;
    private static final String WORKWEEK_SOURCE_LEGAL_REST = "legal_rest";
    private static final int ACTUAL_ATTENDANCE_FULL_DAY_MINUTES = 8 * 60;
    private static final long STANDARD_WORK_MINUTES = 8 * 60L;
    private static final long LUNCH_BREAK_MINUTES = 2 * 60L;
    private static final BigDecimal APPROVAL_HOURS_PER_DAY = BigDecimal.valueOf(8);
    private static final BigDecimal FRACTIONAL_DAY_RANGE_TOLERANCE_HOURS = BigDecimal.valueOf(0.30);

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private hrmDeptRepository deptRepository;

    @Autowired
    private hrmAttendancePlanRepository planRepository;

    @Autowired
    private hrmAttendanceShiftRepository shiftRepository;

    @Autowired
    private hrmAttendanceClockRepository clockRepository;

    @Autowired
    private tbattendancedetailRepository attendanceDetailRepository;

    @Autowired
    private hrmEmployeeOverTimeRecordRepository employeeOverTimeRecordRepository;

    @Autowired
    private hrmOvertimeNightStatisticsDetailRepository detailRepository;

    @Autowired
    private tbattendanceapproveRepository attendanceApproveRepository;

    @Autowired
    private hrmAttendanceGroupRepository attendanceGroupRepository;

    @Autowired
    private hrmAttendanceHistoryShiftRepository attendanceHistoryShiftRepository;

    @Autowired
    private hrmAttendanceDateShiftRepository attendanceDateShiftRepository;

    @Autowired
    private tbattendanceuserRepository attendanceUserRepository;

    @Autowired
    private tbPlanListRepository localPlanRepository;

    @Autowired
    private hrmWorkPlanCustomShiftRepository customShiftRepository;

    @Autowired
    private HrmWorkweekSettingService workweekSettingService;

    @Autowired
    private HrmSalaryBasicMapper salaryBasicMapper;

    @Autowired(required = false)
    private TransactionOperations transactionOperations;

    @Override
    public BasePage<QueryOvertimeNightStatisticsPageVO> queryPageList(QueryOvertimeNightStatisticsPageBO queryBO) {
        QueryOvertimeNightStatisticsPageBO safeQuery = queryBO != null ? queryBO : new QueryOvertimeNightStatisticsPageBO();
        YearMonth targetMonth = resolveMonth(safeQuery.getMonth());
        List<HrmEmployee> employees = filterEmployees(employeeRepository.findAll(), safeQuery.getKeyword());

        long current = safeQuery.getPage();
        long size = safeQuery.getLimit();
        int fromIndex = (int) Math.min((current - 1) * size, employees.size());
        int toIndex = (int) Math.min(fromIndex + size, employees.size());
        List<HrmEmployee> pageEmployees = employees.subList(fromIndex, toIndex);

        Map<Long, String> deptNameMap = buildDeptNameMap(pageEmployees);
        Map<Long, OvertimeNightSummary> summaryMap = loadMonthlySummaryMap(pageEmployees, targetMonth);
        Map<YearMonth, Integer> expectedAttendanceDaysCache = new LinkedHashMap<>();
        Map<String, ActualAttendanceResolution> actualAttendanceDaysCache = new LinkedHashMap<>();

        List<QueryOvertimeNightStatisticsPageVO> rows = pageEmployees.stream()
                .map(employee -> toPageVO(
                        employee,
                        deptNameMap.get(employee.getDeptId()),
                        targetMonth,
                        summaryMap.getOrDefault(employee.getEmployeeId(), OvertimeNightSummary.ZERO),
                        expectedAttendanceDaysCache,
                        actualAttendanceDaysCache
                ))
                .collect(Collectors.toList());
        return new BasePage<>(current, size, (long) employees.size(), rows);
    }

    @Override
    public BasePage<QueryOvertimeNightStatisticsPageVO> startStatistics(QueryOvertimeNightStatisticsPageBO queryBO) {
        QueryOvertimeNightStatisticsPageBO safeQuery = queryBO != null ? queryBO : new QueryOvertimeNightStatisticsPageBO();
        YearMonth targetMonth = resolveMonth(safeQuery.getMonth());
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        List<HrmEmployee> employees = filterEmployees(employeeRepository.findAll(), null);
        Map<Long, String> deptNameMap = buildDeptNameMap(employees);
        Map<Long, HrmAttendanceShift> shiftCache = new LinkedHashMap<>();
        Map<Long, HrmAttendanceGroup> groupCache = new LinkedHashMap<>();
        Map<YearMonth, Integer> expectedAttendanceDaysCache = new LinkedHashMap<>();
        List<HrmOvertimeNightStatisticsDetail> detailRows = new ArrayList<>();

        for (HrmEmployee employee : employees) {
            detailRows.addAll(calculateMonthlyDetails(
                    employee,
                    deptNameMap.get(employee.getDeptId()),
                    targetMonth,
                    shiftCache,
                    groupCache,
                    expectedAttendanceDaysCache,
                    OvertimeHoursSourceMode.DEFAULT
            ));
        }
        replaceMonthlyDetails(monthStart, monthEnd, detailRows);

        QueryOvertimeNightStatisticsPageBO refreshQuery = new QueryOvertimeNightStatisticsPageBO();
        refreshQuery.setPage(safeQuery.getPage());
        refreshQuery.setLimit(safeQuery.getLimit());
        refreshQuery.setKeyword(safeQuery.getKeyword());
        refreshQuery.setMonth(targetMonth.format(MONTH_FORMATTER));
        return queryPageList(refreshQuery);
    }

    @Override
    public List<EmployeeOvertimeNightMonthlyDetailVO> startStatisticsForEmployee(QueryOvertimeNightStatisticsPageBO queryBO) {
        QueryOvertimeNightStatisticsPageBO safeQuery = queryBO != null ? queryBO : new QueryOvertimeNightStatisticsPageBO();
        List<Long> selectedEmployeeIds = resolveSelectedEmployeeIds(safeQuery);
        if (selectedEmployeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        YearMonth targetMonth = resolveMonth(safeQuery.getMonth());
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<HrmEmployee> employees = loadSelectedEmployees(selectedEmployeeIds);
        if (employees.isEmpty()) {
            debugTrace(String.format(
                    Locale.ROOT,
                    "[single-stat] employeesNotFound employeeIds=%s rawMonth=%s resolvedMonth=%s",
                    selectedEmployeeIds,
                    safeQuery.getMonth(),
                    targetMonth.format(MONTH_FORMATTER)
            ));
            return Collections.emptyList();
        }
        Map<Long, String> deptNameMap = buildDeptNameMap(employees);
        Map<Long, HrmAttendanceShift> shiftCache = new LinkedHashMap<>();
        Map<Long, HrmAttendanceGroup> groupCache = new LinkedHashMap<>();
        Map<YearMonth, Integer> expectedAttendanceDaysCache = new LinkedHashMap<>();
        List<EmployeeOvertimeNightMonthlyDetailVO> responseRows = new ArrayList<>();

        for (HrmEmployee employee : employees) {
            debugTrace(String.format(
                    Locale.ROOT,
                    "[single-stat] start employeeId=%s employeeName=%s rawMonth=%s resolvedMonth=%s",
                    employee.getEmployeeId(),
                    employee.getEmployeeName(),
                    safeQuery.getMonth(),
                    targetMonth.format(MONTH_FORMATTER)
            ));

            List<HrmOvertimeNightStatisticsDetail> detailRows = calculateMonthlyDetails(
                    employee,
                    deptNameMap.get(employee.getDeptId()),
                    targetMonth,
                    shiftCache,
                    groupCache,
                    expectedAttendanceDaysCache,
                    OvertimeHoursSourceMode.DEFAULT
            );
            replaceEmployeeMonthlyDetails(employee.getEmployeeId(), monthStart, monthEnd, detailRows);
            debugTrace(String.format(
                    Locale.ROOT,
                    "[single-stat] finish employeeId=%s employeeName=%s resolvedMonth=%s detailRows=%d",
                    employee.getEmployeeId(),
                    employee.getEmployeeName(),
                    targetMonth.format(MONTH_FORMATTER),
                    detailRows.size()
            ));
            responseRows.addAll(buildEmployeeMonthlyDetailsForResponse(employee, targetMonth, detailRows));
        }
        return responseRows;
    }

    private List<Long> resolveSelectedEmployeeIds(QueryOvertimeNightStatisticsPageBO queryBO) {
        if (queryBO == null) {
            return Collections.emptyList();
        }
        List<Long> selectedEmployeeIds = new ArrayList<>();
        if (queryBO.getEmployeeIds() != null) {
            for (Long employeeId : queryBO.getEmployeeIds()) {
                if (employeeId != null && !selectedEmployeeIds.contains(employeeId)) {
                    selectedEmployeeIds.add(employeeId);
                }
            }
        }
        Long singleEmployeeId = queryBO.getEmployeeId();
        if (selectedEmployeeIds.isEmpty() && singleEmployeeId != null) {
            selectedEmployeeIds.add(singleEmployeeId);
        }
        return selectedEmployeeIds;
    }

    private List<HrmEmployee> loadSelectedEmployees(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (employeeIds.size() == 1) {
            return employeeRepository.findById(employeeIds.get(0))
                    .filter(this::isActiveEmployee)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
        List<HrmEmployee> employees = employeeRepository.findAllByEmployeeIdIn(employeeIds);
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, HrmEmployee> employeeMap = employees.stream()
                .filter(employee -> employee != null && employee.getEmployeeId() != null)
                .filter(this::isActiveEmployee)
                .collect(Collectors.toMap(
                        HrmEmployee::getEmployeeId,
                        employee -> employee,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        return employeeIds.stream()
                .map(employeeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isActiveEmployee(HrmEmployee employee) {
        return employee != null && !Objects.equals(employee.getIsDel(), 1);
    }

    private void replaceMonthlyDetails(LocalDate monthStart,
                                       LocalDate monthEnd,
                                       List<HrmOvertimeNightStatisticsDetail> detailRows) {
        executeStatisticsWriteTransaction(() -> {
            detailRepository.deleteAllByWorkDateBetween(
                    toDate(monthStart.atStartOfDay()),
                    toDate(monthEnd.atTime(LocalTime.MAX))
            );
            detailRepository.flush();
            if (detailRows != null && !detailRows.isEmpty()) {
                detailRepository.saveAll(detailRows);
                detailRepository.flush();
            }
            return null;
        });
    }

    private void replaceEmployeeMonthlyDetails(Long employeeId,
                                               LocalDate monthStart,
                                               LocalDate monthEnd,
                                               List<HrmOvertimeNightStatisticsDetail> detailRows) {
        executeStatisticsWriteTransaction(() -> {
            detailRepository.deleteAllByEmployeeIdAndWorkDateBetween(
                    employeeId,
                    toDate(monthStart.atStartOfDay()),
                    toDate(monthEnd.atTime(LocalTime.MAX))
            );
            detailRepository.flush();
            if (detailRows != null && !detailRows.isEmpty()) {
                detailRepository.saveAll(detailRows);
                detailRepository.flush();
            }
            return null;
        });
    }

    private <T> T executeStatisticsWriteTransaction(Supplier<T> action) {
        if (transactionOperations == null) {
            return action.get();
        }
        return transactionOperations.execute(status -> action.get());
    }

    @Override
    public List<EmployeeOvertimeNightMonthlyDetailVO> queryEmployeeMonthlyDetail(QueryEmployeeOvertimeNightDetailBO queryBO) {
        if (queryBO == null || queryBO.getEmployeeId() == null) {
            return Collections.emptyList();
        }
        QueryEmployeeOvertimeNightDetailBO safeQuery = queryBO;
        List<HrmOvertimeNightStatisticsDetail> details;
        if (safeQuery.getMonth() != null && !safeQuery.getMonth().trim().isEmpty()) {
            YearMonth targetMonth = resolveMonth(safeQuery.getMonth());
            details = detailRepository.findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(
                    safeQuery.getEmployeeId(),
                    targetMonth.getYear(),
                    targetMonth.getMonthValue()
            );
        } else {
            details = detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(safeQuery.getEmployeeId());
        }
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }
        HrmEmployee employee = employeeRepository.findById(safeQuery.getEmployeeId()).orElse(null);
        return toMonthlyDetailVOList(details, employee, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    @Override
    public BasePage<QueryOvertimeNightDailyDetailPageVO> queryDailyDetailPageList(QueryOvertimeNightDailyDetailPageBO queryBO) {
        QueryOvertimeNightDailyDetailPageBO safeQuery = queryBO != null ? queryBO : new QueryOvertimeNightDailyDetailPageBO();
        YearMonth targetMonth = resolveMonth(safeQuery.getMonth());
        List<HrmOvertimeNightStatisticsDetail> details = detailRepository
                .findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(targetMonth.getYear(), targetMonth.getMonthValue());

        List<HrmOvertimeNightStatisticsDetail> filteredDetails = (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .filter(detail -> matchesKeyword(detail, safeQuery.getKeyword()))
                .collect(Collectors.toList());
        Map<Long, HrmEmployee> employeeMap = buildEmployeeMapByIds(filteredDetails.stream()
                .map(HrmOvertimeNightStatisticsDetail::getEmployeeId)
                .collect(Collectors.toList()));
        Map<YearMonth, Integer> expectedAttendanceDaysCache = new LinkedHashMap<>();
        Map<String, ActualAttendanceResolution> actualAttendanceDaysCache = new LinkedHashMap<>();
        Map<Long, BigDecimal> monthlyOvertimeHoursByEmployee = buildMonthlyOvertimeHoursByEmployee(filteredDetails);

        List<QueryOvertimeNightDailyDetailPageVO> rows = filteredDetails.stream()
                .map(detail -> toDailyPageVO(
                        detail,
                        employeeMap.get(detail.getEmployeeId()),
                        targetMonth,
                        monthlyOvertimeHoursByEmployee.get(detail.getEmployeeId()),
                        expectedAttendanceDaysCache,
                        actualAttendanceDaysCache
                ))
                .collect(Collectors.toList());

        long current = safeQuery.getPage();
        long size = safeQuery.getLimit();
        int fromIndex = (int) Math.min((current - 1) * size, rows.size());
        int toIndex = (int) Math.min(fromIndex + size, rows.size());
        List<QueryOvertimeNightDailyDetailPageVO> pageRows = rows.subList(fromIndex, toIndex);
        return new BasePage<>(current, size, (long) rows.size(), pageRows);
    }

    @Override
    public void updateAttendanceSummary(UpdateOvertimeNightAttendanceBO updateBO) {
        if (updateBO == null || updateBO.getEmployeeId() == null) {
            throw new IllegalArgumentException("员工不能为空");
        }
        YearMonth targetMonth = resolveMonth(updateBO.getMonth());
        BigDecimal expectedAttendanceHours = parseAttendanceSummaryHours(
                updateBO.getExpectedAttendanceHours(), "应出勤时间");
        BigDecimal actualAttendanceHours = parseAttendanceSummaryHours(
                updateBO.getActualAttendanceHours(), "实际出勤时间");
        BigDecimal accruedAttendanceHours = parseAttendanceSummaryHours(
                updateBO.getAccruedAttendanceHours(), "应计出勤");
        List<HrmOvertimeNightStatisticsDetail> details = detailRepository
                .findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(
                        updateBO.getEmployeeId(),
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                );
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("请先开始统计生成加班/夜班统计明细");
        }
        Integer expectedAttendanceDays = toAttendanceDaysFloor(expectedAttendanceHours);
        Integer actualAttendanceDays = toAttendanceDaysFloor(actualAttendanceHours);
        Date now = new Date();
        executeStatisticsWriteTransaction(() -> {
            for (HrmOvertimeNightStatisticsDetail detail : details) {
                if (detail == null) {
                    continue;
                }
                detail.setExpectedAttendanceDays(expectedAttendanceDays);
                detail.setExpectedAttendanceHours(expectedAttendanceHours);
                detail.setActualAttendanceDays(actualAttendanceDays);
                detail.setActualAttendanceHours(actualAttendanceHours);
                detail.setAccruedAttendanceHours(accruedAttendanceHours);
                detail.setAttendanceManualAdjusted(1);
                detail.setUpdateTime(now);
            }
            detailRepository.saveAll(details);
            detailRepository.flush();
            return null;
        });
    }

    @Override
    public void exportStatistics(QueryOvertimeNightStatisticsPageBO queryBO, HttpServletResponse response) throws IOException {
        QueryOvertimeNightStatisticsPageBO exportQuery = queryBO != null ? queryBO : new QueryOvertimeNightStatisticsPageBO();
        exportQuery.setPage(1L);
        exportQuery.setLimit(10000L);

        YearMonth targetMonth = resolveMonth(exportQuery.getMonth());
        BasePage<QueryOvertimeNightStatisticsPageVO> page = queryPageList(exportQuery);
        List<QueryOvertimeNightStatisticsPageVO> summaryRows = page.getList() != null
                ? page.getList() : Collections.<QueryOvertimeNightStatisticsPageVO>emptyList();
        List<HrmOvertimeNightStatisticsDetail> details = loadDetailsByMonthAndEmployees(
                targetMonth,
                summaryRows.stream()
                        .map(QueryOvertimeNightStatisticsPageVO::getEmployeeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
        Map<Long, List<HrmOvertimeNightStatisticsDetail>> detailMap = details.stream()
                .collect(Collectors.groupingBy(HrmOvertimeNightStatisticsDetail::getEmployeeId));

        List<List<String>> detailRows = new ArrayList<>();
        for (QueryOvertimeNightStatisticsPageVO summaryRow : summaryRows) {
            detailRows.addAll(OvertimeNightStatisticsExportSupport.buildDailyDetailRows(
                    summaryRow,
                    toMonthlyDetailVO(targetMonth, detailMap.getOrDefault(summaryRow.getEmployeeId(), Collections.emptyList()))
            ));
        }

        String fileName = URLEncoder.encode(
                OvertimeNightStatisticsExportSupport.buildFileName(targetMonth.format(MONTH_FORMATTER)),
                "UTF-8"
        ).replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);

        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
        try {
            WriteSheet summarySheet = EasyExcel.writerSheet("汇总")
                    .head(OvertimeNightStatisticsExportSupport.buildSummaryHead())
                    .build();
            excelWriter.write(OvertimeNightStatisticsExportSupport.buildSummaryRows(summaryRows), summarySheet);

            WriteSheet detailSheet = EasyExcel.writerSheet("每日明细")
                    .head(OvertimeNightStatisticsExportSupport.buildDailyDetailHead())
                    .build();
            excelWriter.write(detailRows, detailSheet);
        } finally {
            excelWriter.finish();
        }
    }

    private Map<Long, OvertimeNightSummary> loadMonthlySummaryMap(List<HrmEmployee> employees, YearMonth month) {
        List<Long> employeeIds = employees.stream()
                .map(HrmEmployee::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return loadDetailsByMonthAndEmployees(month, employeeIds).stream()
                .collect(Collectors.groupingBy(HrmOvertimeNightStatisticsDetail::getEmployeeId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> buildSummary(entry.getValue()), (left, right) -> left, LinkedHashMap::new));
    }

    private List<HrmOvertimeNightStatisticsDetail> loadDetailsByMonthAndEmployees(YearMonth month, List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<HrmOvertimeNightStatisticsDetail> details = detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(
                month.getYear(),
                month.getMonthValue(),
                employeeIds
        );
        return details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList();
    }

    private Map<Long, BigDecimal> buildMonthlyOvertimeHoursByEmployee(List<HrmOvertimeNightStatisticsDetail> details) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()) {
            if (detail == null || detail.getEmployeeId() == null) {
                continue;
            }
            result.merge(
                    detail.getEmployeeId(),
                    defaultOvertime(detail.getOvertimeHours()),
                    (left, right) -> left.add(right).setScale(2, RoundingMode.HALF_UP)
            );
        }
        return result;
    }

    private Map<Long, HrmEmployee> buildEmployeeMapByIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinctEmployeeIds = employeeIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctEmployeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrmEmployee> employees = employeeRepository.findAllByEmployeeIdIn(distinctEmployeeIds);
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyMap();
        }
        return employees.stream()
                .filter(Objects::nonNull)
                .filter(employee -> employee.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmEmployee::getEmployeeId, employee -> employee, (left, right) -> left, LinkedHashMap::new));
    }

    private QueryOvertimeNightStatisticsPageVO toPageVO(HrmEmployee employee,
                                                        String deptName,
                                                        YearMonth month,
                                                        OvertimeNightSummary summary,
                                                        Map<YearMonth, Integer> expectedAttendanceDaysCache,
                                                        Map<String, ActualAttendanceResolution> actualAttendanceDaysCache) {
        QueryOvertimeNightStatisticsPageVO row = new QueryOvertimeNightStatisticsPageVO();
        row.setEmployeeId(employee.getEmployeeId());
        row.setEmployeeName(employee.getEmployeeName());
        row.setJobNumber(employee.getJobNumber());
        row.setDeptName(deptName);
        row.setMonth(month.format(MONTH_FORMATTER));
        BigDecimal overtimeHours = resolveCountableOvertimeHours(employee, summary.getOvertimeHours());
        int nightShiftCount = resolveCountableNightShiftCount(employee, summary.getNightShiftCount());
        BigDecimal attendanceOvertimeHours = resolveAccruedAttendanceOvertimeHours(employee, month, overtimeHours);
        if (summary.isManualAttendanceAdjusted()) {
            row.setExpectedAttendanceDays(summary.getExpectedAttendanceDays());
            row.setExpectedAttendanceHours(defaultExpectedAttendanceHours(
                    summary.getExpectedAttendanceHours(),
                    summary.getExpectedAttendanceDays()
            ));
            row.setActualAttendanceDays(summary.getActualAttendanceDays());
            row.setActualAttendanceHours(summary.getActualAttendanceHours());
            row.setAttendanceOvertimeHours(attendanceOvertimeHours);
            row.setAccruedAttendanceHours(defaultAccruedAttendanceHours(summary.getAccruedAttendanceHours()));
            row.setActualAttendanceRemark(summary.getActualAttendanceRemark());
            row.setOvertimeHours(overtimeHours);
            row.setNightShiftCount(nightShiftCount);
            row.setCalcProcess(summary.getCalcProcess());
            return row;
        }
        row.setExpectedAttendanceDays(resolveExpectedAttendanceDaysForQuery(
                employee,
                month,
                summary.getExpectedAttendanceDays(),
                expectedAttendanceDaysCache
        ));
        row.setExpectedAttendanceHours(toAttendanceHours(row.getExpectedAttendanceDays()));
        ActualAttendanceResolution actualAttendance = resolveActualAttendanceForQuery(
                employee,
                month,
                summary.getActualAttendanceDays(),
                row.getExpectedAttendanceDays(),
                summary.getOvertimeHours(),
                actualAttendanceDaysCache
        );
        row.setActualAttendanceDays(actualAttendance.getDays());
        row.setActualAttendanceHours(actualAttendance.getHours());
        row.setAttendanceOvertimeHours(attendanceOvertimeHours);
        row.setAccruedAttendanceHours(calculateAccruedAttendanceHours(actualAttendance, attendanceOvertimeHours));
        row.setActualAttendanceRemark(actualAttendance.getRemark());
        row.setOvertimeHours(overtimeHours);
        row.setNightShiftCount(nightShiftCount);
        row.setCalcProcess(summary.getCalcProcess());
        return row;
    }

    private EmployeeOvertimeNightMonthlyDetailVO toMonthlyDetailVO(YearMonth month, List<HrmOvertimeNightStatisticsDetail> details) {
        return toMonthlyDetailVO(month, details, null, null);
    }

    private EmployeeOvertimeNightMonthlyDetailVO toMonthlyDetailVO(YearMonth month,
                                                                   List<HrmOvertimeNightStatisticsDetail> details,
                                                                   HrmEmployee employee,
                                                                   Map<YearMonth, Integer> expectedAttendanceDaysCache) {
        return toMonthlyDetailVO(month, details, employee, expectedAttendanceDaysCache, null);
    }

    private EmployeeOvertimeNightMonthlyDetailVO toMonthlyDetailVO(YearMonth month,
                                                                   List<HrmOvertimeNightStatisticsDetail> details,
                                                                   HrmEmployee employee,
                                                                   Map<YearMonth, Integer> expectedAttendanceDaysCache,
                                                                   Map<String, ActualAttendanceResolution> actualAttendanceDaysCache) {
        OvertimeNightSummary summary = buildSummary(details, employee, month, expectedAttendanceDaysCache, actualAttendanceDaysCache);
        EmployeeOvertimeNightMonthlyDetailVO row = new EmployeeOvertimeNightMonthlyDetailVO();
        row.setMonth(month.format(MONTH_FORMATTER));
        row.setExpectedAttendanceDays(summary.getExpectedAttendanceDays());
        row.setExpectedAttendanceHours(summary.getExpectedAttendanceHours());
        row.setActualAttendanceDays(summary.getActualAttendanceDays());
        row.setActualAttendanceHours(summary.getActualAttendanceHours());
        row.setAccruedAttendanceHours(summary.getAccruedAttendanceHours() != null
                ? summary.getAccruedAttendanceHours().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        row.setActualAttendanceRemark(summary.getActualAttendanceRemark());
        row.setOvertimeHours(summary.getOvertimeHours());
        row.setNightShiftCount(summary.getNightShiftCount());
        row.setDailyDetails(summary.getDailyDetails());
        return row;
    }

    private QueryOvertimeNightDailyDetailPageVO toDailyPageVO(HrmOvertimeNightStatisticsDetail detail) {
        return toDailyPageVO(detail, null, resolveDetailMonth(detail), defaultOvertime(detail.getOvertimeHours()), null, null);
    }

    private QueryOvertimeNightDailyDetailPageVO toDailyPageVO(HrmOvertimeNightStatisticsDetail detail,
                                                              HrmEmployee employee,
                                                              YearMonth month,
                                                              BigDecimal monthlyOvertimeHours,
                                                              Map<YearMonth, Integer> expectedAttendanceDaysCache,
                                                              Map<String, ActualAttendanceResolution> actualAttendanceDaysCache) {
        QueryOvertimeNightDailyDetailPageVO row = new QueryOvertimeNightDailyDetailPageVO();
        row.setEmployeeId(detail.getEmployeeId());
        row.setEmployeeName(detail.getEmployeeName());
        row.setJobNumber(detail.getJobNumber());
        row.setDeptName(detail.getDeptName());
        row.setWorkDate(detail.getWorkDate() != null ? DAY_FORMATTER.format(toLocalDateTime(detail.getWorkDate()).toLocalDate()) : null);
        boolean manualAttendanceAdjusted = isManualAttendanceAdjusted(detail);
        BigDecimal overtimeHours = resolveCountableOvertimeHours(employee, detail.getOvertimeHours());
        BigDecimal effectiveMonthlyOvertimeHours = canCountOvertimeNight(employee)
                ? defaultOvertime(monthlyOvertimeHours)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        row.setOvertimeHours(overtimeHours);
        row.setNightShiftCount(resolveCountableNightShiftCount(employee, detail.getNightShiftCount()));
        row.setCalcProcess(detail.getCalcProcess());
        int expectedAttendanceDays = manualAttendanceAdjusted
                ? defaultExpectedAttendanceDays(detail)
                : resolveExpectedAttendanceDaysForQuery(
                        employee,
                        month,
                        detail.getExpectedAttendanceDays(),
                        expectedAttendanceDaysCache
                );
        row.setExpectedAttendanceDays(expectedAttendanceDays);
        row.setExpectedAttendanceHours(manualAttendanceAdjusted
                ? defaultExpectedAttendanceHours(detail, expectedAttendanceDays)
                : toAttendanceHours(expectedAttendanceDays));
        ActualAttendanceResolution actualAttendance = manualAttendanceAdjusted
                ? new ActualAttendanceResolution(
                        defaultActualAttendanceDays(detail),
                        defaultActualAttendanceHours(detail, detail.getActualAttendanceDays()),
                        resolveActualAttendanceRemark(detail),
                        ActualAttendanceDeduction.empty()
                )
                : resolveActualAttendanceForQuery(
                        employee,
                        month,
                        detail.getActualAttendanceDays(),
                        row.getExpectedAttendanceDays(),
                        effectiveMonthlyOvertimeHours,
                        actualAttendanceDaysCache
                );
        row.setActualAttendanceDays(actualAttendance.getDays());
        row.setActualAttendanceHours(actualAttendance.getHours());
        row.setAccruedAttendanceHours(manualAttendanceAdjusted
                ? defaultAccruedAttendanceHours(detail.getAccruedAttendanceHours())
                : resolveAccruedAttendanceHoursForQuery(
                        detail.getAccruedAttendanceHours(),
                        actualAttendance,
                        employee,
                        month,
                        effectiveMonthlyOvertimeHours
                ));
        row.setActualAttendanceRemark(manualAttendanceAdjusted
                ? resolveActualAttendanceRemark(detail)
                : actualAttendance.getRemark());
        return row;
    }

    private List<EmployeeOvertimeNightMonthlyDetailVO> buildEmployeeMonthlyDetailsForResponse(HrmEmployee employee,
                                                                                              YearMonth currentMonth,
                                                                                              List<HrmOvertimeNightStatisticsDetail> currentMonthDetails) {
        Long employeeId = employee != null ? employee.getEmployeeId() : null;
        List<HrmOvertimeNightStatisticsDetail> persistedDetails = employeeId != null
                ? detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(employeeId)
                : Collections.emptyList();
        List<HrmOvertimeNightStatisticsDetail> mergedDetails = new ArrayList<>();
        if (persistedDetails != null) {
            for (HrmOvertimeNightStatisticsDetail persistedDetail : persistedDetails) {
                YearMonth persistedMonth = resolveDetailMonth(persistedDetail);
                if (persistedMonth == null || Objects.equals(persistedMonth, currentMonth)) {
                    continue;
                }
                mergedDetails.add(persistedDetail);
            }
        }
        if (currentMonthDetails != null && !currentMonthDetails.isEmpty()) {
            mergedDetails.addAll(currentMonthDetails);
        }
        return toMonthlyDetailVOList(mergedDetails, employee, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private List<EmployeeOvertimeNightMonthlyDetailVO> toMonthlyDetailVOList(List<HrmOvertimeNightStatisticsDetail> details) {
        return toMonthlyDetailVOList(details, null, null);
    }

    private List<EmployeeOvertimeNightMonthlyDetailVO> toMonthlyDetailVOList(List<HrmOvertimeNightStatisticsDetail> details,
                                                                             HrmEmployee employee,
                                                                             Map<YearMonth, Integer> expectedAttendanceDaysCache) {
        return toMonthlyDetailVOList(details, employee, expectedAttendanceDaysCache, null);
    }

    private List<EmployeeOvertimeNightMonthlyDetailVO> toMonthlyDetailVOList(List<HrmOvertimeNightStatisticsDetail> details,
                                                                             HrmEmployee employee,
                                                                             Map<YearMonth, Integer> expectedAttendanceDaysCache,
                                                                             Map<String, ActualAttendanceResolution> actualAttendanceDaysCache) {
        Map<YearMonth, List<HrmOvertimeNightStatisticsDetail>> detailMap = new LinkedHashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details) {
            YearMonth month = resolveDetailMonth(detail);
            if (month == null) {
                continue;
            }
            detailMap.computeIfAbsent(month, key -> new ArrayList<>()).add(detail);
        }
        return detailMap.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, List<HrmOvertimeNightStatisticsDetail>>comparingByKey().reversed())
                .map(entry -> toMonthlyDetailVO(entry.getKey(), entry.getValue(), employee, expectedAttendanceDaysCache, actualAttendanceDaysCache))
                .collect(Collectors.toList());
    }

    private OvertimeNightSummary buildSummary(List<HrmOvertimeNightStatisticsDetail> details) {
        return buildSummary(details, null, null, null, null);
    }

    private OvertimeNightSummary buildSummary(List<HrmOvertimeNightStatisticsDetail> details,
                                              HrmEmployee employee,
                                              YearMonth month,
                                              Map<YearMonth, Integer> expectedAttendanceDaysCache,
                                              Map<String, ActualAttendanceResolution> actualAttendanceDaysCache) {
        boolean manualAttendanceAdjusted = hasManualAttendanceAdjusted(details);
        boolean canCountOvertimeNight = canCountOvertimeNight(employee);
        List<DailyOvertimeNightDetailVO> dailyDetails = (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .sorted(Comparator.comparing(HrmOvertimeNightStatisticsDetail::getWorkDate, Comparator.nullsLast(Date::compareTo)).reversed())
                .map(this::toDailyDetailVO)
                .collect(Collectors.toList());
        if (employee != null && !canCountOvertimeNight) {
            for (DailyOvertimeNightDetailVO dailyDetail : dailyDetails) {
                dailyDetail.setOvertimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                dailyDetail.setNightShiftCount(0);
            }
        }
        BigDecimal overtimeHours = dailyDetails.stream()
                .map(DailyOvertimeNightDetailVO::getOvertimeHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int nightShiftCount = dailyDetails.stream()
                .map(DailyOvertimeNightDetailVO::getNightShiftCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int expectedAttendanceDays = (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .map(HrmOvertimeNightStatisticsDetail::getExpectedAttendanceDays)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        BigDecimal expectedAttendanceHours = resolvePersistedExpectedAttendanceHours(details, expectedAttendanceDays);
        if (!manualAttendanceAdjusted) {
            expectedAttendanceDays = resolveExpectedAttendanceDaysForQuery(
                    employee,
                    month,
                    expectedAttendanceDays,
                    expectedAttendanceDaysCache
            );
            expectedAttendanceHours = toAttendanceHours(expectedAttendanceDays);
        }
        int persistedActualAttendanceDays = (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .map(HrmOvertimeNightStatisticsDetail::getActualAttendanceDays)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        BigDecimal persistedActualAttendanceHours = resolvePersistedActualAttendanceHours(details, persistedActualAttendanceDays);
        ActualAttendanceResolution actualAttendance = manualAttendanceAdjusted
                ? new ActualAttendanceResolution(
                        persistedActualAttendanceDays,
                        persistedActualAttendanceHours,
                        resolvePersistedActualAttendanceRemark(details),
                        ActualAttendanceDeduction.empty()
                )
                : resolveActualAttendanceForQuery(
                        employee,
                        month,
                        persistedActualAttendanceDays,
                        expectedAttendanceDays,
                        overtimeHours,
                        actualAttendanceDaysCache
                );
        BigDecimal persistedAccruedAttendanceHours = resolvePersistedAccruedAttendanceHours(details);
        BigDecimal accruedAttendanceHours = manualAttendanceAdjusted
                ? persistedAccruedAttendanceHours
                : employee != null
                        ? calculateAccruedAttendanceHours(
                                actualAttendance,
                                resolveAccruedAttendanceOvertimeHours(employee, month, overtimeHours)
                        )
                        : persistedAccruedAttendanceHours;
        String calcProcess = (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .filter(detail -> detail != null && detail.getCalcProcess() != null && !detail.getCalcProcess().trim().isEmpty())
                .map(HrmOvertimeNightStatisticsDetail::getCalcProcess)
                .collect(Collectors.joining("\n"));
        return new OvertimeNightSummary(
                overtimeHours,
                nightShiftCount,
                expectedAttendanceDays,
                expectedAttendanceHours,
                actualAttendance.getDays(),
                actualAttendance.getHours(),
                actualAttendance.getRemark(),
                accruedAttendanceHours,
                dailyDetails,
                manualAttendanceAdjusted,
                calcProcess
        );
    }

    private DailyOvertimeNightDetailVO toDailyDetailVO(HrmOvertimeNightStatisticsDetail detail) {
        DailyOvertimeNightDetailVO row = new DailyOvertimeNightDetailVO();
        row.setWorkDate(detail.getWorkDate() != null ? DAY_FORMATTER.format(toLocalDateTime(detail.getWorkDate()).toLocalDate()) : null);
        row.setOvertimeHours(defaultOvertime(detail.getOvertimeHours()));
        row.setNightShiftCount(detail.getNightShiftCount() != null ? detail.getNightShiftCount() : 0);
        row.setCalcProcess(detail.getCalcProcess());
        return row;
    }

    private List<HrmOvertimeNightStatisticsDetail> calculateMonthlyDetails(HrmEmployee employee,
                                                                           String deptName,
                                                                           YearMonth month,
                                                                           Map<Long, HrmAttendanceShift> shiftCache,
                                                                           Map<Long, HrmAttendanceGroup> groupCache,
                                                                           Map<YearMonth, Integer> expectedAttendanceDaysCache,
                                                                           OvertimeHoursSourceMode overtimeHoursSourceMode) {
        if (employee == null || employee.getEmployeeId() == null) {
            return Collections.emptyList();
        }
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        Date begin = toDate(monthStart.atStartOfDay());
        Date end = toDate(monthEnd.atTime(LocalTime.MAX));
        Date clockEnd = toDate(monthEnd.plusDays(1).atTime(LocalTime.MAX));

        String planUserId = resolveStatisticsPlanUserId(employee);
        List<tbplanlist> localPlans = planUserId == null
                ? Collections.<tbplanlist>emptyList()
                : localPlanRepository.findAllByWorkDateBetweenOrderByIdDesc(begin, end);
        Map<String, List<tbplanlist>> localPlanDayMap = new LinkedHashMap<>();
        Set<Long> monthlyGroupIds = new LinkedHashSet<>();
        for (tbplanlist localPlan : localPlans != null ? localPlans : Collections.<tbplanlist>emptyList()) {
            if (localPlan == null || localPlan.getWorkDate() == null) {
                continue;
            }
            if (!parsePlanUserIds(localPlan.getUserId()).contains(planUserId)) {
                continue;
            }
            String key = resolveDayKey(localPlan.getWorkDate());
            localPlanDayMap.computeIfAbsent(key, ignored -> new ArrayList<>()).add(localPlan);
            Long planGroupId = parseLongValue(localPlan.getGroupId());
            if (planGroupId != null) {
                monthlyGroupIds.add(planGroupId);
            }
        }

        List<HrmAttendanceClock> clockList = clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(employee.getEmployeeId(), begin, clockEnd);
        int expectedAttendanceDays = resolveExpectedAttendanceDays(employee, month, expectedAttendanceDaysCache);
        ActualAttendanceDeduction deduction = resolveActualAttendanceDeduction(employee.getEmployeeId(), begin, end);
        MonthlyAttendanceDays attendanceDays = calculateMonthlyAttendanceDays(
                month,
                clockList,
                expectedAttendanceDays,
                deduction,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        Map<String, List<HrmAttendanceClock>> clockMap = (clockList != null ? clockList : Collections.<HrmAttendanceClock>emptyList()).stream()
                .map(clock -> new java.util.AbstractMap.SimpleEntry<>(resolveClockDayKey(clock), clock))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        List<tbattendancedetail> attendanceDetails = attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(employee.getEmployeeId(), begin, clockEnd);
        Map<String, List<tbattendancedetail>> attendanceDetailMap = (attendanceDetails != null ? attendanceDetails : Collections.<tbattendancedetail>emptyList()).stream()
                .map(detail -> new java.util.AbstractMap.SimpleEntry<>(resolveAttendanceDetailDayKey(detail), detail))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        Map<String, HrmWorkPlanCustomShift> customShiftCache = new LinkedHashMap<>();
        Map<String, OvertimeApprovalAggregate> attendanceApprovalMap = buildAttendanceApprovalMap(
                employee.getEmployeeId(),
                begin,
                end
        );
        if (shouldDebugEmployee(employee)) {
            debugTrace(String.format(
                    Locale.ROOT,
                    "[single-stat-data] employeeId=%s employeeName=%s month=%s localPlanCount=%d localPlanDays=%s clockCount=%d clockDayKeys=%s detailCount=%d detailDayKeys=%s approvalDays=%s",
                    employee.getEmployeeId(),
                    employee.getEmployeeName(),
                    month.format(MONTH_FORMATTER),
                    localPlans != null ? localPlans.size() : 0,
                    localPlanDayMap.keySet(),
                    clockList != null ? clockList.size() : 0,
                    clockMap.keySet(),
                    attendanceDetails != null ? attendanceDetails.size() : 0,
                    attendanceDetailMap.keySet(),
                    attendanceApprovalMap.keySet()
            ));
        }

        Set<String> candidateDayKeys = new LinkedHashSet<>();
        candidateDayKeys.addAll(localPlanDayMap.keySet());
        candidateDayKeys.addAll(clockMap.keySet());
        candidateDayKeys.addAll(attendanceDetailMap.keySet());
        candidateDayKeys.addAll(attendanceApprovalMap.keySet());
        if (candidateDayKeys.isEmpty()) {
            return Collections.singletonList(buildZeroDetailRow(employee, deptName, month, monthStart, attendanceDays, new Date()));
        }

        List<LocalDate> orderedWorkDates = candidateDayKeys.stream()
                .map(this::parseDayKey)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        Date now = new Date();
        List<HrmOvertimeNightStatisticsDetail> rows = new ArrayList<>();
        Map<String, Set<String>> consumedCarryoverPunchKeys = new LinkedHashMap<>();
        for (LocalDate workDate : orderedWorkDates) {
            String dayKey = DAY_FORMATTER.format(workDate);
            List<tbplanlist> dayPlans = localPlanDayMap.getOrDefault(dayKey, Collections.emptyList());
            tbplanlist plan = selectPrimaryLocalPlan(dayPlans);
            boolean restDayPlan = isRestLocalPlan(plan);
            HrmWorkPlanCustomShift matchedCustomShift = plan != null && plan.getCustomShiftId() != null
                    ? resolveCustomShiftCached(plan.getCustomShiftId(), customShiftCache)
                    : null;
            List<HrmAttendanceClock> dayClocks = filterConsumedClockRecords(
                    dayKey,
                    clockMap.getOrDefault(dayKey, Collections.emptyList()),
                    consumedCarryoverPunchKeys
            );
            List<tbattendancedetail> dayDetails = filterConsumedAttendanceDetailRecords(
                    dayKey,
                    attendanceDetailMap.getOrDefault(dayKey, Collections.emptyList()),
                    consumedCarryoverPunchKeys
            );
            OvertimeApprovalAggregate approvalAggregate = attendanceApprovalMap.get(dayKey);
            if (dayPlans.isEmpty() && dayClocks.isEmpty() && dayDetails.isEmpty() && approvalAggregate == null) {
                continue;
            }
            List<HrmAttendanceClock> offDutyClocks = dayClocks.stream()
                    .filter(clock -> Objects.equals(clock.getClockType(), 2))
                    .collect(Collectors.toList());
            List<Long> fallbackGroupIds = !dayPlans.isEmpty() ? new ArrayList<>(monthlyGroupIds) : Collections.<Long>emptyList();
            HrmAttendanceShift resolvedDateShift = resolveDateShift(employee.getEmployeeId(), workDate);
            HrmAttendanceShift shift = null;
            if (!restDayPlan && plan != null) {
                if (isCustomLocalPlan(plan)) {
                    shift = resolveLocalCustomShift(plan, shiftCache);
                }
                if (shift == null) {
                    shift = resolveShiftByGroupAndDate(
                            shiftCache,
                            parseLongValue(plan.getClassId()),
                            parseLongValue(plan.getGroupId()),
                            fallbackGroupIds,
                            workDate,
                            groupCache
                    );
                }
                if (shift == null) {
                    shift = resolvedDateShift;
                }
            }
            List<PunchRecord> punchRecords = buildPunchRecords(dayClocks, dayDetails);
            OvertimeNightClockResolver.ScheduledEndTimeResolution scheduledEndResolution =
                    OvertimeNightClockResolver.resolveScheduledEndTimeResolution(
                    workDate,
                    offDutyClocks,
                    shift,
                    null,
                    java.time.ZoneId.systemDefault()
            );
            LocalDateTime scheduledEndTime = scheduledEndResolution.getTime();
            ScheduledStartTimeResolution scheduledStartResolution = resolveScheduledStartTimeResolution(workDate, Collections.<HrmAttendancePlan>emptyList(), shift);
            LocalDateTime scheduledStartTime = scheduledStartResolution.getTime();
            String nextDayKey = resolveDayKey(toDate(workDate.plusDays(1).atStartOfDay()));
            CrossDayPunchMergeResult crossDayPunchMergeResult = mergeCrossDayOffDutyPunchRecords(
                    workDate,
                    scheduledEndTime,
                    punchRecords,
                    filterConsumedClockRecords(
                            nextDayKey,
                            clockMap.getOrDefault(nextDayKey, Collections.emptyList()),
                            consumedCarryoverPunchKeys
                    ),
                    filterConsumedAttendanceDetailRecords(
                            nextDayKey,
                            attendanceDetailMap.getOrDefault(nextDayKey, Collections.emptyList()),
                            consumedCarryoverPunchKeys
                    )
            );
            rememberConsumedCarryoverPunchKeys(consumedCarryoverPunchKeys, nextDayKey, crossDayPunchMergeResult.getConsumedPunchKeys());
            punchRecords = crossDayPunchMergeResult.getMergedPunchRecords();
            LocalDateTime firstOnDutyTime = resolveFirstOnDutyTime(punchRecords);
            LocalDateTime actualOffTime = resolveActualOffTime(punchRecords);
            LocalDateTime effectiveStartTime = resolveEffectiveStartTime(firstOnDutyTime, scheduledStartTime);
            boolean hasScheduledShift = plan != null && !restDayPlan;
            boolean continuousShift = resolveContinuousShift(employee, plan, matchedCustomShift);
            PunchWorkResult punchWork = computePunchWork(punchRecords);
            long workedMinutes = punchWork.getWorkedMinutes();
            if (hasScheduledShift && scheduledStartTime != null && firstOnDutyTime != null
                    && firstOnDutyTime.isBefore(scheduledStartTime)) {
                workedMinutes -= Duration.between(firstOnDutyTime, scheduledStartTime).toMinutes();
            }
            long lunchDeductMinutes = 0L;
            if (hasScheduledShift && !continuousShift && punchWork.getSegmentCount() <= 1) {
                lunchDeductMinutes = LUNCH_BREAK_MINUTES;
                workedMinutes -= LUNCH_BREAK_MINUTES;
            }
            workedMinutes = Math.max(workedMinutes, 0L);
            BigDecimal overtimeHours = hasScheduledShift && workedMinutes > STANDARD_WORK_MINUTES
                    ? BigDecimal.valueOf(workedMinutes - STANDARD_WORK_MINUTES)
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (approvalAggregate != null && approvalAggregate.hasOvertimeHours()) {
                overtimeHours = approvalAggregate.getOvertimeHours();
            }
            String shiftPeriod = matchedCustomShift != null && matchedCustomShift.getShiftPeriod() != null
                    ? matchedCustomShift.getShiftPeriod()
                    : (plan != null ? plan.getCustomShiftPeriod() : null);
            boolean scheduledNight = hasScheduledShift
                    && scheduledEndResolution.getSource() == OvertimeNightClockResolver.ScheduledEndTimeSource.SHIFT
                    && OvertimeNightClockResolver.isScheduledNight(workDate, shiftPeriod, scheduledEndTime);
            LocalDateTime effectiveOffTime = actualOffTime != null ? actualOffTime : scheduledEndTime;
            int nightShiftCount = OvertimeNightClockResolver.isNightShift(workDate, scheduledNight, effectiveOffTime) ? 1 : 0;
            if (!canCountOvertimeNight(employee)) {
                overtimeHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                nightShiftCount = 0;
            }
            String calcProcess = buildDailyCalcProcess(
                    workDate,
                    plan,
                    restDayPlan,
                    continuousShift,
                    scheduledStartTime,
                    scheduledEndTime,
                    firstOnDutyTime,
                    actualOffTime,
                    workedMinutes,
                    lunchDeductMinutes,
                    overtimeHours,
                    nightShiftCount,
                    approvalAggregate != null && approvalAggregate.hasOvertimeHours(),
                    !canCountOvertimeNight(employee)
            );

            if (shouldDebugEmployee(employee)) {
                String debugLine = String.format(
                        Locale.ROOT,
                        "[daily] workDate=%s classId=%s groupId=%s selectedPlanId=%s restDay=%s shift=%s shiftTimes=[%s-%s] scheduledStartTime=%s scheduledEndTime=%s scheduledEndSource=%s firstOnDutyTime=%s actualOffTime=%s punchSegments=%d workedMinutes=%s lunchDeductMinutes=%s continuousShift=%s overtimeHours=%s nightShiftCount=%s offDutyClocksCount=%d",
                        workDate,
                        plan != null ? plan.getClassId() : null,
                        plan != null ? plan.getGroupId() : null,
                        plan != null ? plan.getId() : null,
                        restDayPlan,
                        shift != null ? shift.getShiftId() : "null",
                        shift != null ? firstNotBlank(shift.getStart3(), shift.getStart2(), shift.getStart1()) : "null",
                        shift != null ? firstNotBlank(shift.getEnd3(), shift.getEnd2(), shift.getEnd1()) : "null",
                        scheduledStartTime,
                        scheduledEndTime,
                        scheduledEndResolution.getSource(),
                        firstOnDutyTime,
                        actualOffTime,
                        punchWork.getSegmentCount(),
                        workedMinutes,
                        lunchDeductMinutes,
                        continuousShift,
                        overtimeHours,
                        nightShiftCount,
                        offDutyClocks.size()
                );
                debugTrace(debugLine);
            }

            HrmOvertimeNightStatisticsDetail detail = new HrmOvertimeNightStatisticsDetail();
            detail.setDetailId(BaseUtil.getNextId());
            detail.setStatYear(month.getYear());
            detail.setStatMonth(month.getMonthValue());
            detail.setEmployeeId(employee.getEmployeeId());
            detail.setEmployeeName(employee.getEmployeeName());
            detail.setJobNumber(employee.getJobNumber());
            detail.setDeptId(employee.getDeptId());
            detail.setDeptName(deptName);
            detail.setWorkDate(plan != null && plan.getWorkDate() != null ? plan.getWorkDate() : toDate(workDate.atStartOfDay()));
            detail.setPlanId(plan != null && plan.getId() != null ? plan.getId().longValue() : null);
            detail.setClassId(plan != null ? parseLongValue(plan.getClassId()) : null);
            detail.setScheduledOffTime(scheduledEndTime != null ? toDate(scheduledEndTime) : null);
            detail.setActualOffTime(actualOffTime != null ? toDate(actualOffTime) : null);
            detail.setOvertimeHours(overtimeHours);
            detail.setNightShiftCount(nightShiftCount);
            detail.setCalcProcess(calcProcess);
            detail.setExpectedAttendanceDays(attendanceDays.getExpectedAttendanceDays());
            detail.setActualAttendanceDays(attendanceDays.getActualAttendanceDays());
            detail.setAccruedAttendanceHours(attendanceDays.getAccruedAttendanceHours());
            detail.setCreateTime(now);
            detail.setUpdateTime(now);
            rows.add(detail);
        }
        BigDecimal monthlyOvertimeHours = sumOvertimeHours(rows);
        BigDecimal monthlyApprovalOvertimeHours = sumOvertimeApprovalHours(attendanceApprovalMap);
        BigDecimal actualAttendanceOvertimeHours = resolveActualAttendanceOvertimeHours(
                employee,
                monthlyOvertimeHours,
                monthlyApprovalOvertimeHours
        );
        MonthlyAttendanceDays finalAttendanceDays = calculateMonthlyAttendanceDays(
                month,
                clockList,
                expectedAttendanceDays,
                deduction,
                actualAttendanceOvertimeHours,
                actualAttendanceOvertimeHours
        );
        applyMonthlyAttendanceDays(rows, finalAttendanceDays);
        return rows;
    }

    private void applyMonthlyAttendanceDays(List<HrmOvertimeNightStatisticsDetail> rows,
                                            MonthlyAttendanceDays attendanceDays) {
        if (rows == null || rows.isEmpty() || attendanceDays == null) {
            return;
        }
        for (HrmOvertimeNightStatisticsDetail row : rows) {
            row.setExpectedAttendanceDays(attendanceDays.getExpectedAttendanceDays());
            row.setExpectedAttendanceHours(attendanceDays.getExpectedAttendanceHours());
            row.setActualAttendanceDays(attendanceDays.getActualAttendanceDays());
            row.setActualAttendanceHours(attendanceDays.getActualAttendanceHours());
            row.setAccruedAttendanceHours(attendanceDays.getAccruedAttendanceHours());
            row.setAttendanceManualAdjusted(0);
        }
    }

    private BigDecimal sumOvertimeHours(List<HrmOvertimeNightStatisticsDetail> rows) {
        return (rows != null ? rows : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .map(HrmOvertimeNightStatisticsDetail::getOvertimeHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumOvertimeApprovalHours(Map<String, OvertimeApprovalAggregate> approvalMap) {
        return (approvalMap != null ? approvalMap.values() : Collections.<OvertimeApprovalAggregate>emptyList()).stream()
                .filter(Objects::nonNull)
                .map(OvertimeApprovalAggregate::getOvertimeHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveActualAttendanceOvertimeHours(HrmEmployee employee,
                                                            BigDecimal monthlyOvertimeHours,
                                                            BigDecimal monthlyApprovalOvertimeHours) {
        if (!usesApprovalOvertimeForActualAttendance(employee)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (usesApprovalOvertimeForActualAttendance(employee)) {
            return defaultOvertime(monthlyApprovalOvertimeHours);
        }
        return defaultOvertime(monthlyOvertimeHours);
    }

    private boolean usesApprovalOvertimeForActualAttendance(HrmEmployee employee) {
        if (employee == null) {
            return false;
        }
        return Objects.equals(employee.getAffiliationSystem(), AFFILIATION_SYSTEM_ADMINISTRATIVE)
                || Objects.equals(employee.getAffiliationSystem(), AFFILIATION_SYSTEM_PRODUCTION);
    }

    private boolean canCountOvertimeNight(HrmEmployee employee) {
        return employee != null
                && (Objects.equals(employee.getAffiliationSystem(), AFFILIATION_SYSTEM_PRODUCTION)
                || Objects.equals(employee.getRestType(), REST_TYPE_FIXED_MONTHLY_REST));
    }

    private BigDecimal resolveCountableOvertimeHours(HrmEmployee employee, BigDecimal overtimeHours) {
        return canCountOvertimeNight(employee)
                ? defaultOvertime(overtimeHours)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private int resolveCountableNightShiftCount(HrmEmployee employee, Integer nightShiftCount) {
        return canCountOvertimeNight(employee) && nightShiftCount != null ? nightShiftCount : 0;
    }

    /** 统计用排班匹配用户ID：优先员工钉钉ID，缺失时回退考勤用户快照 UserID。 */
    private String resolveStatisticsPlanUserId(HrmEmployee employee) {
        if (employee == null) {
            return null;
        }
        String dingUserId = employee.getDingtalkUserId();
        if (dingUserId != null && !dingUserId.trim().isEmpty()) {
            return dingUserId.trim();
        }
        return attendanceUserRepository.findFirstByEmpId(employee.getEmployeeId())
                .map(tbattendanceuser::getUserId)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }

    /** 同日多条排班（多产品/岗位共享行）择一：非休息优先，其余取最新一条。 */
    private tbplanlist selectPrimaryLocalPlan(List<tbplanlist> dayPlans) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return null;
        }
        return dayPlans.stream()
                .filter(Objects::nonNull)
                .filter(plan -> !isRestLocalPlan(plan))
                .findFirst()
                .orElse(dayPlans.get(0));
    }

    private boolean isRestLocalPlan(tbplanlist plan) {
        if (plan == null) {
            return false;
        }
        if (plan.getRestShiftType() != null && !plan.getRestShiftType().trim().isEmpty()) {
            return true;
        }
        String shiftType = plan.getShiftType();
        return shiftType != null && "rest".equalsIgnoreCase(shiftType.trim());
    }

    private HrmWorkPlanCustomShift resolveCustomShiftCached(Long customShiftId, Map<String, HrmWorkPlanCustomShift> cache) {
        if (customShiftId == null) {
            return null;
        }
        String key = String.valueOf(customShiftId);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        HrmWorkPlanCustomShift customShift = customShiftRepository.findById(customShiftId).orElse(null);
        cache.put(key, customShift);
        return customShift;
    }

    /** 连班判定：自定义班次设置 > 排班行显式标记 > 员工档案 is_continuous_shift。 */
    private boolean resolveContinuousShift(HrmEmployee employee, tbplanlist plan, HrmWorkPlanCustomShift customShift) {
        if (customShift != null && customShift.getContinuousShift() != null) {
            return customShift.getContinuousShift() == 1;
        }
        if (plan != null && plan.getCustomContinuousShift() != null) {
            return Boolean.TRUE.equals(plan.getCustomContinuousShift());
        }
        return employee != null && Objects.equals(employee.getIsContinuousShift(), 1);
    }

    /** 有效工时按打卡分段累计（上午上班-上午下班-下午上班-下午下班），午休缺口天然排除。 */
    private PunchWorkResult computePunchWork(List<PunchRecord> punchRecords) {
        long workedMinutes = 0L;
        int segmentCount = 0;
        LocalDateTime openOnDutyTime = null;
        for (PunchRecord record : sortPunchRecords(punchRecords)) {
            if (record == null || record.getActualTime() == null) {
                continue;
            }
            if (record.getType() == PunchRecordType.ON_DUTY) {
                if (openOnDutyTime == null) {
                    openOnDutyTime = record.getActualTime();
                }
            } else if (openOnDutyTime != null) {
                if (!record.getActualTime().isBefore(openOnDutyTime)) {
                    workedMinutes += Duration.between(openOnDutyTime, record.getActualTime()).toMinutes();
                    segmentCount++;
                }
                openOnDutyTime = null;
            }
        }
        return new PunchWorkResult(workedMinutes, segmentCount);
    }

    private String buildDailyCalcProcess(LocalDate workDate,
                                         tbplanlist plan,
                                         boolean restDayPlan,
                                         boolean continuousShift,
                                         LocalDateTime scheduledStartTime,
                                         LocalDateTime scheduledEndTime,
                                         LocalDateTime firstOnDutyTime,
                                         LocalDateTime actualOffTime,
                                         long workedMinutes,
                                         long lunchDeductMinutes,
                                         BigDecimal overtimeHours,
                                         int nightShiftCount,
                                         boolean approvalOverride,
                                         boolean notCountable) {
        StringBuilder text = new StringBuilder();
        text.append(DAY_FORMATTER.format(workDate)).append("：");
        if (plan == null) {
            text.append("无排班，不计加班");
        } else if (restDayPlan) {
            text.append("排班为休息/调休，不计加班");
        } else {
            text.append("排班 ")
                    .append(formatCalcTime(scheduledStartTime)).append('-').append(formatCalcTime(scheduledEndTime))
                    .append(continuousShift ? "(连班)" : "(不连班)");
            if (firstOnDutyTime != null) {
                text.append("，上班卡 ").append(formatCalcTime(firstOnDutyTime));
            }
            if (actualOffTime != null) {
                text.append("，下班卡 ").append(formatCalcTime(actualOffTime));
            }
            text.append("，有效工时 ").append(formatCalcMinutes(workedMinutes)).append("h");
            if (lunchDeductMinutes > 0) {
                text.append("（未分段计午休，扣除2h）");
            }
            text.append("，超8h基准部分计加班 ").append(overtimeHours).append("h");
        }
        if (nightShiftCount > 0) {
            text.append("；下班超过次日凌晨3点，计夜班1次");
        }
        if (approvalOverride) {
            text.append("；存在本地加班审批，按审批时长计");
        }
        if (notCountable) {
            text.append("；非生产体系且非固定月休，加班/夜班记0");
        }
        return text.toString();
    }

    private String formatCalcTime(LocalDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
    }

    private String formatCalcMinutes(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private HrmOvertimeNightStatisticsDetail buildZeroDetailRow(HrmEmployee employee,
                                                                String deptName,
                                                                YearMonth month,
                                                                LocalDate workDate,
                                                                MonthlyAttendanceDays attendanceDays,
                                                                Date now) {
        HrmOvertimeNightStatisticsDetail detail = new HrmOvertimeNightStatisticsDetail();
        detail.setDetailId(BaseUtil.getNextId());
        detail.setStatYear(month.getYear());
        detail.setStatMonth(month.getMonthValue());
        detail.setEmployeeId(employee.getEmployeeId());
        detail.setEmployeeName(employee.getEmployeeName());
        detail.setJobNumber(employee.getJobNumber());
        detail.setDeptId(employee.getDeptId());
        detail.setDeptName(deptName);
        detail.setWorkDate(toDate(workDate.atStartOfDay()));
        detail.setOvertimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        detail.setNightShiftCount(0);
        detail.setExpectedAttendanceDays(attendanceDays != null ? attendanceDays.getExpectedAttendanceDays() : 0);
        detail.setExpectedAttendanceHours(attendanceDays != null
                ? attendanceDays.getExpectedAttendanceHours()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        detail.setActualAttendanceDays(attendanceDays != null ? attendanceDays.getActualAttendanceDays() : 0);
        detail.setActualAttendanceHours(attendanceDays != null
                ? attendanceDays.getActualAttendanceHours()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        detail.setAccruedAttendanceHours(attendanceDays != null
                ? attendanceDays.getAccruedAttendanceHours()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        detail.setAttendanceManualAdjusted(0);
        detail.setCalcProcess("当月无排班/打卡/审批数据，零值占位行");
        detail.setCreateTime(now);
        detail.setUpdateTime(now);
        return detail;
    }

    private MonthlyAttendanceDays calculateMonthlyAttendanceDays(YearMonth month,
                                                                  List<HrmAttendanceClock> clockList,
                                                                  int expectedAttendanceDays,
                                                                  ActualAttendanceDeduction deduction,
                                                                  BigDecimal actualAttendanceOvertimeHours,
                                                                  BigDecimal accruedAttendanceOvertimeHours) {
        ActualAttendanceResolution actualAttendance;
        if (expectedAttendanceDays > 0) {
            actualAttendance = calculateActualAttendanceFromExpected(expectedAttendanceDays, actualAttendanceOvertimeHours, deduction);
        } else {
            int actualAttendanceDays = calculateActualAttendanceDays(month, clockList, 0L);
            actualAttendance = ActualAttendanceResolution.withoutRemark(
                    actualAttendanceDays,
                    BigDecimal.valueOf(actualAttendanceDays).multiply(BigDecimal.valueOf(8)),
                    deduction
            );
        }
        return new MonthlyAttendanceDays(
                expectedAttendanceDays,
                actualAttendance.getDays(),
                actualAttendance.getHours(),
                calculateAccruedAttendanceHours(actualAttendance, accruedAttendanceOvertimeHours)
        );
    }

    private BigDecimal parseAttendanceSummaryHours(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        BigDecimal normalizedValue = value.setScale(2, RoundingMode.HALF_UP);
        if (normalizedValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + "不能小于0");
        }
        return normalizedValue;
    }

    private boolean hasManualAttendanceAdjusted(List<HrmOvertimeNightStatisticsDetail> details) {
        return (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .anyMatch(this::isManualAttendanceAdjusted);
    }

    private boolean isManualAttendanceAdjusted(HrmOvertimeNightStatisticsDetail detail) {
        return detail != null && Integer.valueOf(1).equals(detail.getAttendanceManualAdjusted());
    }

    private int defaultExpectedAttendanceDays(HrmOvertimeNightStatisticsDetail detail) {
        if (detail == null) {
            return 0;
        }
        if (detail.getExpectedAttendanceDays() != null) {
            return detail.getExpectedAttendanceDays();
        }
        return toAttendanceDaysFloor(detail.getExpectedAttendanceHours());
    }

    private BigDecimal defaultExpectedAttendanceHours(BigDecimal hours, Integer days) {
        return hours != null
                ? hours.setScale(2, RoundingMode.HALF_UP)
                : toAttendanceHours(days);
    }

    private BigDecimal defaultExpectedAttendanceHours(HrmOvertimeNightStatisticsDetail detail, Integer days) {
        return detail != null
                ? defaultExpectedAttendanceHours(detail.getExpectedAttendanceHours(), days)
                : toAttendanceHours(days);
    }

    private int defaultActualAttendanceDays(HrmOvertimeNightStatisticsDetail detail) {
        if (detail == null) {
            return 0;
        }
        if (detail.getActualAttendanceDays() != null) {
            return detail.getActualAttendanceDays();
        }
        return toAttendanceDaysFloor(detail.getActualAttendanceHours());
    }

    private BigDecimal defaultActualAttendanceHours(HrmOvertimeNightStatisticsDetail detail, Integer days) {
        return detail != null && detail.getActualAttendanceHours() != null
                ? detail.getActualAttendanceHours().setScale(2, RoundingMode.HALF_UP)
                : toAttendanceHours(days);
    }

    private BigDecimal defaultAccruedAttendanceHours(BigDecimal hours) {
        return hours != null
                ? hours.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePersistedExpectedAttendanceHours(List<HrmOvertimeNightStatisticsDetail> details,
                                                               Integer expectedAttendanceDays) {
        return (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .map(HrmOvertimeNightStatisticsDetail::getExpectedAttendanceHours)
                .filter(Objects::nonNull)
                .findFirst()
                .map(value -> value.setScale(2, RoundingMode.HALF_UP))
                .orElseGet(() -> toAttendanceHours(expectedAttendanceDays));
    }

    private BigDecimal resolvePersistedActualAttendanceHours(List<HrmOvertimeNightStatisticsDetail> details,
                                                             Integer actualAttendanceDays) {
        return (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .map(HrmOvertimeNightStatisticsDetail::getActualAttendanceHours)
                .filter(Objects::nonNull)
                .findFirst()
                .map(value -> value.setScale(2, RoundingMode.HALF_UP))
                .orElseGet(() -> toAttendanceHours(actualAttendanceDays));
    }

    private String resolvePersistedActualAttendanceRemark(List<HrmOvertimeNightStatisticsDetail> details) {
        return "";
    }

    private String resolveActualAttendanceRemark(HrmOvertimeNightStatisticsDetail detail) {
        return "";
    }

    private Integer toAttendanceDaysFloor(BigDecimal hours) {
        if (hours == null) {
            return 0;
        }
        return hours.divide(APPROVAL_HOURS_PER_DAY, 0, RoundingMode.DOWN).intValue();
    }

    private BigDecimal toAttendanceHours(Integer days) {
        return BigDecimal.valueOf(days != null ? days : 0)
                .multiply(APPROVAL_HOURS_PER_DAY)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int resolveExpectedAttendanceDaysForQuery(HrmEmployee employee,
                                                       YearMonth month,
                                                       Integer persistedExpectedAttendanceDays,
                                                       Map<YearMonth, Integer> expectedAttendanceDaysCache) {
        int currentValue = persistedExpectedAttendanceDays != null ? persistedExpectedAttendanceDays : 0;
        if (currentValue > 0 && !shouldRefreshExpectedAttendanceDaysForQuery(employee)) {
            return currentValue;
        }
        if (employee == null) {
            return currentValue;
        }
        int resolvedValue = resolveExpectedAttendanceDays(employee, month, expectedAttendanceDaysCache);
        return resolvedValue > 0 ? resolvedValue : currentValue;
    }

    private boolean shouldRefreshExpectedAttendanceDaysForQuery(HrmEmployee employee) {
        if (employee == null || employee.getRestType() == null) {
            return false;
        }
        if (Objects.equals(employee.getRestType(), REST_TYPE_FIXED_MONTHLY_REST)) {
            return !Objects.equals(employee.getAffiliationSystem(), AFFILIATION_SYSTEM_PRODUCTION);
        }
        if (Objects.equals(employee.getRestType(), REST_TYPE_WORKWEEK)) {
            return !Objects.equals(employee.getAffiliationSystem(), AFFILIATION_SYSTEM_ADMINISTRATIVE);
        }
        return false;
    }

    private int resolveExpectedAttendanceDays(HrmEmployee employee,
                                              YearMonth month,
                                              Map<YearMonth, Integer> expectedAttendanceDaysCache) {
        if (employee == null) {
            return 0;
        }
        // 月休4天：总天数 - 基本工资设置的 productionMonthlyRestDays（不再扣除法定节假日）
        if (Objects.equals(employee.getRestType(), REST_TYPE_FIXED_MONTHLY_REST)) {
            int productionMonthlyRestDays = resolveProductionMonthlyRestDays();
            return Math.max(month.lengthOfMonth() - productionMonthlyRestDays, 0);
        }
        // 行政单双休：从单双休设置获取
        if (Objects.equals(employee.getRestType(), REST_TYPE_WORKWEEK)
                || Objects.equals(employee.getAffiliationSystem(), AFFILIATION_SYSTEM_ADMINISTRATIVE)) {
            return queryAdministrativeExpectedAttendanceDays(month, expectedAttendanceDaysCache);
        }
        // 默认：总天数 - productionMonthlyRestDays
        int productionMonthlyRestDays = resolveProductionMonthlyRestDays();
        return Math.max(month.lengthOfMonth() - productionMonthlyRestDays, 0);
    }

    private int queryAdministrativeExpectedAttendanceDays(YearMonth month,
                                                          Map<YearMonth, Integer> expectedAttendanceDaysCache) {
        if (month == null) {
            return 0;
        }
        if (expectedAttendanceDaysCache != null && expectedAttendanceDaysCache.containsKey(month)) {
            return expectedAttendanceDaysCache.get(month);
        }
        int expectedAttendanceDays = queryAdministrativeExpectedAttendanceDays(month);
        if (expectedAttendanceDaysCache != null) {
            expectedAttendanceDaysCache.put(month, expectedAttendanceDays);
        }
        return expectedAttendanceDays;
    }

    private int queryProductionExpectedAttendanceDays(YearMonth month) {
        if (month == null) {
            return 0;
        }
        int productionMonthlyRestDays = resolveProductionMonthlyRestDays();
        int legalHolidayRestDays = queryProductionLegalHolidayRestDays(month);
        return Math.max(month.lengthOfMonth() - productionMonthlyRestDays - legalHolidayRestDays, 0);
    }

    private int resolveProductionMonthlyRestDays() {
        if (salaryBasicMapper == null) {
            return HrmSalaryBasicDefaults.PRODUCTION_MONTHLY_REST_DAYS;
        }
        List<HrmSalaryBasic> salaryBasics = salaryBasicMapper.selectList(new LambdaQueryWrapper<HrmSalaryBasic>()
                .orderByDesc(HrmSalaryBasic::getCreateTime)
                .orderByDesc(HrmSalaryBasic::getId)
                .last("limit 1"));
        HrmSalaryBasic salaryBasic = salaryBasics == null || salaryBasics.isEmpty() ? null : salaryBasics.get(0);
        return HrmSalaryBasicDefaults.productionMonthlyRestDays(salaryBasic);
    }

    private int queryProductionLegalHolidayRestDays(YearMonth month) {
        if (month == null || workweekSettingService == null) {
            return 0;
        }
        QueryWorkweekMonthCalendarBO queryBO = new QueryWorkweekMonthCalendarBO();
        queryBO.setYear(month.getYear());
        queryBO.setMonth(month.getMonthValue());
        try {
            WorkweekMonthCalendarVO calendar = workweekSettingService.queryMonthCalendar(queryBO);
            if (calendar != null && calendar.getDays() != null && !calendar.getDays().isEmpty()) {
                return (int) calendar.getDays().stream()
                        .filter(day -> day != null && WORKWEEK_SOURCE_LEGAL_REST.equals(day.getSourceType()))
                        .filter(day -> !isWeekendLegalRestDay(day))
                        .count();
            }
            return calendar != null && calendar.getLegalHolidayRestDays() != null
                    ? Math.max(calendar.getLegalHolidayRestDays(), 0)
                    : 0;
        } catch (RuntimeException ex) {
            log.warn("query production legal holiday rest days failed, month={}, reason={}",
                    month.format(MONTH_FORMATTER), ex.getMessage());
            return 0;
        }
    }

    private boolean isWeekendLegalRestDay(WorkweekDayCalendarVO day) {
        Integer dayOfWeek = day.getDayOfWeek();
        if (dayOfWeek == null && day.getWorkDate() != null) {
            try {
                dayOfWeek = LocalDate.parse(day.getWorkDate(), DAY_FORMATTER).getDayOfWeek().getValue();
            } catch (DateTimeParseException ignored) {
                return false;
            }
        }
        return dayOfWeek != null && (dayOfWeek == 6 || dayOfWeek == 7);
    }

    private int queryAdministrativeExpectedAttendanceDays(YearMonth month) {
        if (month == null || workweekSettingService == null) {
            return 0;
        }
        QueryWorkweekMonthCalendarBO queryBO = new QueryWorkweekMonthCalendarBO();
        queryBO.setYear(month.getYear());
        queryBO.setMonth(month.getMonthValue());
        try {
            WorkweekMonthCalendarVO calendar = workweekSettingService.queryMonthCalendar(queryBO);
            return calendar != null && calendar.getWorkDays() != null ? calendar.getWorkDays() : 0;
        } catch (RuntimeException ex) {
            log.warn("query administrative expected attendance days failed, month={}, reason={}",
                    month.format(MONTH_FORMATTER), ex.getMessage());
            return 0;
        }
    }

    private ActualAttendanceResolution resolveActualAttendanceForQuery(HrmEmployee employee,
                                                                       YearMonth month,
                                                                       Integer persistedActualAttendanceDays,
                                                                       Integer expectedAttendanceDays,
                                                                       BigDecimal overtimeHours,
                                                                       Map<String, ActualAttendanceResolution> actualAttendanceDaysCache) {
        int currentValue = persistedActualAttendanceDays != null ? persistedActualAttendanceDays : 0;
        if (employee == null || employee.getEmployeeId() == null || month == null) {
            return ActualAttendanceResolution.withoutRemark(
                    currentValue,
                    BigDecimal.valueOf(currentValue).multiply(BigDecimal.valueOf(8))
            );
        }
        BigDecimal normalizedOvertimeHours = defaultOvertime(overtimeHours);
        String cacheKey = employee.getEmployeeId() + "#" + month.format(MONTH_FORMATTER)
                + "#" + Math.max(expectedAttendanceDays != null ? expectedAttendanceDays : 0, 0)
                + "#" + normalizedOvertimeHours.toPlainString();
        if (actualAttendanceDaysCache != null && actualAttendanceDaysCache.containsKey(cacheKey)) {
            return actualAttendanceDaysCache.get(cacheKey);
        }
        ActualAttendanceResolution resolvedValue = calculateActualAttendanceForQuery(
                employee,
                month,
                expectedAttendanceDays != null ? expectedAttendanceDays : 0,
                normalizedOvertimeHours,
                currentValue
        );
        if (actualAttendanceDaysCache != null) {
            actualAttendanceDaysCache.put(cacheKey, resolvedValue);
        }
        return resolvedValue;
    }

    private ActualAttendanceResolution calculateActualAttendanceForQuery(HrmEmployee employee,
                                                                         YearMonth month,
                                                                         int expectedAttendanceDays,
                                                                         BigDecimal overtimeHours,
                                                                         int persistedActualAttendanceDays) {
        Long employeeId = employee != null ? employee.getEmployeeId() : null;
        if (employeeId == null || month == null) {
            return ActualAttendanceResolution.withoutRemark(
                    persistedActualAttendanceDays,
                    BigDecimal.valueOf(persistedActualAttendanceDays).multiply(BigDecimal.valueOf(8))
            );
        }
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        Date begin = toDate(monthStart.atStartOfDay());
        Date end = toDate(monthEnd.atTime(LocalTime.MAX));
        Date clockEnd = toDate(monthEnd.plusDays(1).atTime(LocalTime.MAX));
        try {
            BigDecimal actualAttendanceOvertimeHours = resolveActualAttendanceOvertimeHours(
                    employee,
                    overtimeHours,
                    resolveMonthlyOvertimeApprovalHours(employeeId, begin, end)
            );
            ActualAttendanceDeduction deduction = resolveActualAttendanceDeduction(employeeId, begin, end);
            if (expectedAttendanceDays > 0) {
                return calculateActualAttendanceFromExpected(expectedAttendanceDays, actualAttendanceOvertimeHours, deduction);
            }
            List<HrmAttendanceClock> clockList = clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(
                    employeeId,
                    begin,
                    clockEnd
            );
            int actualAttendanceDays = calculateActualAttendanceDays(
                    month,
                    clockList,
                    0L
            );
            int resolvedDays = Math.max(persistedActualAttendanceDays, actualAttendanceDays);
            return ActualAttendanceResolution.withoutRemark(
                    resolvedDays,
                    BigDecimal.valueOf(resolvedDays).multiply(BigDecimal.valueOf(8)),
                    deduction
            );
        } catch (RuntimeException ex) {
            log.warn("query actual attendance days fallback failed, employeeId={}, month={}, reason={}",
                    employeeId, month.format(MONTH_FORMATTER), ex.getMessage());
            return ActualAttendanceResolution.withoutRemark(
                    persistedActualAttendanceDays,
                    BigDecimal.valueOf(persistedActualAttendanceDays).multiply(BigDecimal.valueOf(8))
            );
        }
    }

    private BigDecimal resolveAccruedAttendanceHoursForQuery(BigDecimal persistedAccruedAttendanceHours,
                                                             ActualAttendanceResolution actualAttendance,
                                                             HrmEmployee employee,
                                                             YearMonth month,
                                                             BigDecimal overtimeHours) {
        if (actualAttendance != null) {
            return calculateAccruedAttendanceHours(
                    actualAttendance,
                    resolveAccruedAttendanceOvertimeHours(employee, month, overtimeHours)
            );
        }
        return persistedAccruedAttendanceHours != null
                ? persistedAccruedAttendanceHours.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveAccruedAttendanceOvertimeHours(HrmEmployee employee,
                                                             YearMonth month,
                                                             BigDecimal monthlyOvertimeHours) {
        BigDecimal defaultHours = defaultOvertime(monthlyOvertimeHours);
        if (!usesApprovalOvertimeForActualAttendance(employee)) {
            return defaultHours;
        }
        if (employee.getEmployeeId() == null || month == null) {
            return defaultHours;
        }
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        Date begin = toDate(monthStart.atStartOfDay());
        Date end = toDate(monthEnd.atTime(LocalTime.MAX));
        try {
            return resolveActualAttendanceOvertimeHours(
                    employee,
                    defaultHours,
                    resolveMonthlyOvertimeApprovalHours(employee.getEmployeeId(), begin, end)
            );
        } catch (RuntimeException ex) {
            log.warn("query accrued attendance overtime fallback failed, employeeId={}, month={}, reason={}",
                    employee.getEmployeeId(), month.format(MONTH_FORMATTER), ex.getMessage());
            return defaultHours;
        }
    }

    private BigDecimal resolvePersistedAccruedAttendanceHours(List<HrmOvertimeNightStatisticsDetail> details) {
        return (details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()).stream()
                .map(HrmOvertimeNightStatisticsDetail::getAccruedAttendanceHours)
                .filter(Objects::nonNull)
                .findFirst()
                .map(value -> value.setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private BigDecimal calculateAccruedAttendanceHours(ActualAttendanceResolution actualAttendance,
                                                       BigDecimal overtimeHours) {
        ActualAttendanceDeduction deduction = actualAttendance != null
                ? actualAttendance.getDeduction()
                : ActualAttendanceDeduction.empty();
        BigDecimal actualHours = actualAttendance != null
                ? actualAttendance.getHours()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return actualHours
                .add(deduction.getHoursByType("调休"))
                .add(deduction.getHoursByType("年假"))
                .subtract(defaultOvertime(overtimeHours))
                .add(deduction.getHoursByType("病假"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ActualAttendanceResolution calculateActualAttendanceFromExpected(int expectedAttendanceDays,
                                                                            BigDecimal overtimeHours,
                                                                            ActualAttendanceDeduction deduction) {
        BigDecimal expectedHours = BigDecimal.valueOf(Math.max(expectedAttendanceDays, 0))
                .multiply(BigDecimal.valueOf(8));
        BigDecimal actualHours = expectedHours
                .add(defaultOvertime(overtimeHours))
                .subtract(deduction != null ? deduction.getHours() : BigDecimal.ZERO);
        if (actualHours.compareTo(BigDecimal.ZERO) < 0) {
            actualHours = BigDecimal.ZERO;
        }
        int actualDays = actualHours.divide(BigDecimal.valueOf(8), 0, RoundingMode.DOWN).intValue();
        return new ActualAttendanceResolution(
                actualDays,
                actualHours.setScale(2, RoundingMode.HALF_UP),
                deduction != null ? deduction.getRemark() : "",
                deduction
        );
    }

    private int calculateActualAttendanceDays(YearMonth month, List<HrmAttendanceClock> clockList, long compensatoryLeaveMinutes) {
        if (month == null) {
            return 0;
        }
        Map<LocalDate, List<LocalDateTime>> clockTimesByDate = new LinkedHashMap<>();
        for (HrmAttendanceClock clock : clockList != null ? clockList : Collections.<HrmAttendanceClock>emptyList()) {
            LocalDate workDate = resolveActualAttendanceWorkDate(clock);
            LocalDateTime clockTime = resolveActualAttendanceClockTime(clock);
            if (workDate == null || clockTime == null || !YearMonth.from(workDate).equals(month)) {
                continue;
            }
            clockTimesByDate.computeIfAbsent(workDate, ignored -> new ArrayList<>()).add(clockTime);
        }
        long actualAttendanceMinutes = 0;
        for (List<LocalDateTime> dayClockTimes : clockTimesByDate.values()) {
            if (dayClockTimes == null || dayClockTimes.size() < 2) {
                continue;
            }
            LocalDateTime firstClockTime = dayClockTimes.stream().min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime lastClockTime = dayClockTimes.stream().max(LocalDateTime::compareTo).orElse(null);
            if (firstClockTime == null || lastClockTime == null) {
                continue;
            }
            long minutes = Duration.between(firstClockTime, lastClockTime).toMinutes();
            if (minutes > 0) {
                actualAttendanceMinutes += Math.min(minutes, ACTUAL_ATTENDANCE_FULL_DAY_MINUTES);
            }
        }
        actualAttendanceMinutes += Math.max(compensatoryLeaveMinutes, 0L);
        return (int) (actualAttendanceMinutes / ACTUAL_ATTENDANCE_FULL_DAY_MINUTES);
    }

    private LocalDate resolveActualAttendanceWorkDate(HrmAttendanceClock clock) {
        if (clock == null) {
            return null;
        }
        Date workDate = clock.getWorkDate() != null ? clock.getWorkDate()
                : (clock.getAttendanceTime() != null ? clock.getAttendanceTime() : clock.getClockTime());
        return workDate != null ? toLocalDateTime(workDate).toLocalDate() : null;
    }

    private LocalDateTime resolveActualAttendanceClockTime(HrmAttendanceClock clock) {
        if (clock == null) {
            return null;
        }
        Date clockTime = clock.getClockTime() != null ? clock.getClockTime() : clock.getAttendanceTime();
        return clockTime != null ? toLocalDateTime(clockTime) : null;
    }

    private ActualAttendanceDeduction resolveActualAttendanceDeduction(Long employeeId, Date begin, Date end) {
        if (employeeId == null || begin == null || end == null) {
            return ActualAttendanceDeduction.empty();
        }
        List<String> userIds = resolveAttendanceUserIds(employeeId);
        if (userIds.isEmpty()) {
            return ActualAttendanceDeduction.empty();
        }
        List<tbattendanceapprove> approvals = attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(userIds, begin, end);
        if (approvals == null || approvals.isEmpty()) {
            return ActualAttendanceDeduction.empty();
        }
        Set<String> allowedUserIds = new LinkedHashSet<>(userIds);
        Map<String, BigDecimal> hoursByType = new LinkedHashMap<>();
        for (tbattendanceapprove approval : approvals) {
            if (approval == null || approval.getUserId() == null || !allowedUserIds.contains(approval.getUserId().trim())) {
                continue;
            }
            if (!isApprovalIncludedInStatistics(approval)) {
                continue;
            }
            String leaveType = resolveActualAttendanceDeductibleType(approval);
            if (leaveType == null) {
                continue;
            }
            BigDecimal hours = resolveAttendanceApprovalHours(approval);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            hoursByType.merge(leaveType, hours, BigDecimal::add);
        }
        return ActualAttendanceDeduction.of(hoursByType);
    }

    private BigDecimal resolveMonthlyOvertimeApprovalHours(Long employeeId, Date begin, Date end) {
        if (employeeId == null || begin == null || end == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sumOvertimeApprovalHours(buildAttendanceApprovalMap(employeeId, begin, end));
    }

    private String resolveActualAttendanceDeductibleType(tbattendanceapprove approval) {
        if (approval == null) {
            return null;
        }
        String tagName = approval.getTagName() != null ? approval.getTagName().trim() : "";
        String subType = approval.getSubType() != null ? approval.getSubType().trim() : "";
        String combined = tagName + " " + subType;
        if (combined.contains("事假")) {
            return "事假";
        }
        if (combined.contains("病假")) {
            return "病假";
        }
        if (combined.contains("调休")) {
            return "调休";
        }
        if (combined.contains("年假")) {
            return "年假";
        }
        return null;
    }

    private Map<String, OvertimeApprovalAggregate> buildAttendanceApprovalMap(Long employeeId, Date begin, Date end) {
        if (employeeId == null || begin == null || end == null) {
            return Collections.emptyMap();
        }
        List<String> userIds = resolveAttendanceUserIds(employeeId);
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<tbattendanceapprove> approvals = attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(userIds, begin, end);
        if (approvals == null || approvals.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> allowedUserIds = new LinkedHashSet<>(userIds);
        Map<String, OvertimeApprovalAggregate> approvalMap = new LinkedHashMap<>();
        for (tbattendanceapprove approval : approvals) {
            if (approval == null || approval.getUserId() == null || !allowedUserIds.contains(approval.getUserId().trim())) {
                continue;
            }
            if (!isApprovalIncludedInStatistics(approval)) {
                continue;
            }
            if (!isOvertimeApproval(approval)) {
                continue;
            }
            String dayKey = resolveAttendanceApprovalDayKey(approval);
            if (dayKey == null) {
                continue;
            }
            approvalMap.computeIfAbsent(dayKey, key -> new OvertimeApprovalAggregate())
                    .merge(approval, this::toLocalDateTime);
        }
        return approvalMap;
    }

    private boolean isApprovalIncludedInStatistics(tbattendanceapprove approval) {
        if (approval == null) {
            return false;
        }
        String statisticsStatus = approval.getStatisticsStatus() != null ? approval.getStatisticsStatus().trim() : "";
        return !"取消至统计".equals(statisticsStatus);
    }

    private List<String> resolveAttendanceUserIds(Long employeeId) {
        if (employeeId == null) {
            return Collections.emptyList();
        }
        Set<String> userIds = new LinkedHashSet<>();
        List<tbattendanceuser> attendanceUsers = attendanceUserRepository.findAllByEmpId(employeeId);
        for (tbattendanceuser attendanceUser : attendanceUsers != null ? attendanceUsers : Collections.<tbattendanceuser>emptyList()) {
            if (attendanceUser != null && attendanceUser.getUserId() != null && !attendanceUser.getUserId().trim().isEmpty()) {
                userIds.add(attendanceUser.getUserId().trim());
            }
        }
        if (userIds.isEmpty()) {
            Optional<tbattendanceuser> attendanceUser = attendanceUserRepository.findFirstByEmpId(employeeId);
            if (attendanceUser.isPresent()
                    && attendanceUser.get().getUserId() != null
                    && !attendanceUser.get().getUserId().trim().isEmpty()) {
                userIds.add(attendanceUser.get().getUserId().trim());
            }
        }
        return new ArrayList<>(userIds);
    }

    private boolean isOvertimeApproval(tbattendanceapprove approval) {
        if (approval == null) {
            return false;
        }
        if (Objects.equals(Long.valueOf(1L), approval.getBizType())) {
            return true;
        }
        String tagName = approval.getTagName() != null ? approval.getTagName().trim() : "";
        String subType = approval.getSubType() != null ? approval.getSubType().trim() : "";
        return tagName.contains("加班") || subType.contains("加班");
    }

    private String resolveAttendanceApprovalDayKey(tbattendanceapprove approval) {
        if (approval == null) {
            return null;
        }
        Date businessDate = firstNonNull(
                approval.getWorkDate(),
                approval.getBeginTime(),
                approval.getEndTime()
        );
        return resolveDayKey(businessDate);
    }

    private BigDecimal resolveAttendanceApprovalHours(tbattendanceapprove approval) {
        if (approval == null || approval.getDuration() == null || approval.getDuration().trim().isEmpty()) {
            return resolveAttendanceApprovalHoursFromTimeRange(approval);
        }
        String rawDuration = approval.getDuration().trim();
        String normalizedDuration = rawDuration
                .replace("小时", "")
                .replace("分钟", "")
                .replace("天", "")
                .trim();
        if (normalizedDuration.isEmpty()) {
            return resolveAttendanceApprovalHoursFromTimeRange(approval);
        }
        BigDecimal durationValue;
        try {
            durationValue = new BigDecimal(normalizedDuration);
        } catch (NumberFormatException ex) {
            return resolveAttendanceApprovalHoursFromTimeRange(approval);
        }
        String durationUnit = approval.getDurationUnit() != null ? approval.getDurationUnit().trim() : "";
        String mergedUnitText = durationUnit + rawDuration;
        BigDecimal hours;
        if (mergedUnitText.contains("分钟")) {
            hours = durationValue.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        } else if (mergedUnitText.contains("天")) {
            hours = resolveRoundedFractionalDayApprovalHours(approval, durationValue);
        } else {
            hours = durationValue;
        }
        return hours.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveRoundedFractionalDayApprovalHours(tbattendanceapprove approval, BigDecimal durationDays) {
        BigDecimal dayHours = durationDays.multiply(APPROVAL_HOURS_PER_DAY).setScale(2, RoundingMode.HALF_UP);
        if (durationDays.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return dayHours;
        }
        BigDecimal timeRangeHours = resolveAttendanceApprovalHoursFromTimeRange(approval);
        if (timeRangeHours.compareTo(BigDecimal.ZERO) <= 0) {
            return dayHours;
        }
        BigDecimal difference = dayHours.subtract(timeRangeHours).abs();
        if (difference.compareTo(FRACTIONAL_DAY_RANGE_TOLERANCE_HOURS) <= 0) {
            return timeRangeHours;
        }
        return dayHours;
    }

    private BigDecimal resolveAttendanceApprovalHoursFromTimeRange(tbattendanceapprove approval) {
        if (approval == null || approval.getBeginTime() == null || approval.getEndTime() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        LocalDateTime beginTime = toLocalDateTime(approval.getBeginTime());
        LocalDateTime endTime = toLocalDateTime(approval.getEndTime());
        long minutes = Duration.between(beginTime, endTime).toMinutes();
        if (minutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private Map<String, OvertimeRecordAggregate> buildOvertimeRecordMap(List<HrmEmployeeOverTimeRecord> overtimeRecords) {
        if (overtimeRecords == null || overtimeRecords.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, OvertimeRecordAggregate> recordMap = new LinkedHashMap<>();
        for (HrmEmployeeOverTimeRecord overtimeRecord : overtimeRecords) {
            String dayKey = resolveOvertimeRecordDayKey(overtimeRecord);
            if (dayKey == null) {
                continue;
            }
            recordMap.computeIfAbsent(dayKey, key -> new OvertimeRecordAggregate()).merge(overtimeRecord, this::toLocalDateTime);
        }
        return recordMap;
    }

    private String resolveOvertimeRecordDayKey(HrmEmployeeOverTimeRecord overtimeRecord) {
        if (overtimeRecord == null) {
            return null;
        }
        Date businessDate = firstNonNull(
                overtimeRecord.getAttendanceTime(),
                overtimeRecord.getOverTimeStartTime(),
                overtimeRecord.getOverTimeEndTime()
        );
        return resolveDayKey(businessDate);
    }

    private HrmAttendancePlan selectPrimaryPlan(List<HrmAttendancePlan> dailyPlans) {
        if (dailyPlans == null || dailyPlans.isEmpty()) {
            return null;
        }
        Comparator<HrmAttendancePlan> comparator = Comparator
                .comparing((HrmAttendancePlan plan) -> plan.getPlanCheckTime() != null)
                .thenComparing(HrmAttendancePlan::getPlanCheckTime, Comparator.nullsFirst(Date::compareTo))
                .thenComparing((HrmAttendancePlan plan) -> plan.getCreateTime() != null)
                .thenComparing(HrmAttendancePlan::getCreateTime, Comparator.nullsFirst(Date::compareTo))
                .thenComparing(HrmAttendancePlan::getPlanId, Comparator.nullsFirst(Long::compareTo));
        Optional<HrmAttendancePlan> offDutyPlan = dailyPlans.stream()
                .filter(Objects::nonNull)
                .filter(this::isOffDutyPlan)
                .max(comparator);
        if (offDutyPlan.isPresent()) {
            return offDutyPlan.get();
        }
        return dailyPlans.stream()
                .filter(Objects::nonNull)
                .max(comparator)
                .orElse(null);
    }

    private boolean isOffDutyPlan(HrmAttendancePlan plan) {
        return plan != null && "OffDuty".equalsIgnoreCase(plan.getCheckType());
    }

    private LocalDateTime resolvePlanCheckTime(HrmAttendancePlan plan) {
        if (plan == null || plan.getPlanCheckTime() == null) {
            return null;
        }
        return toLocalDateTime(plan.getPlanCheckTime());
    }

    private HrmAttendanceShift resolveShift(Map<Long, HrmAttendanceShift> shiftCache, Long classId) {
        if (classId == null) {
            return null;
        }
        if (shiftCache.containsKey(classId)) {
            return shiftCache.get(classId);
        }
        HrmAttendanceShift shift = shiftRepository.findById(classId).orElse(null);
        shiftCache.put(classId, shift);
        return shift;
    }

    private HrmAttendanceShift resolveHistoryShift(Long shiftId) {
        if (shiftId == null || shiftId > Integer.MAX_VALUE || shiftId < Integer.MIN_VALUE) {
            return null;
        }
        HrmAttendanceHistoryShift historyShift = attendanceHistoryShiftRepository
                .findFirstByShiftIdOrderByUpdateTimeDesc(shiftId.intValue())
                .orElse(null);
        if (historyShift == null) {
            return null;
        }
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(Long.valueOf(historyShift.getShiftId()));
        shift.setShiftType(historyShift.getShiftType());
        shift.setShiftName(historyShift.getShiftName());
        shift.setShiftHours(historyShift.getShiftHours());
        shift.setStart1(historyShift.getStart1());
        shift.setEnd1(historyShift.getEnd1());
        shift.setStart2(historyShift.getStart2());
        shift.setEnd2(historyShift.getEnd2());
        shift.setStart3(historyShift.getStart3());
        shift.setEnd3(historyShift.getEnd3());
        shift.setAdvanceCard1(historyShift.getAdvanceCard1());
        shift.setLateCard1(historyShift.getLateCard1());
        shift.setAdvanceCard2(historyShift.getAdvanceCard2());
        shift.setLateCard2(historyShift.getLateCard2());
        shift.setAdvanceCard3(historyShift.getAdvanceCard3());
        shift.setLateCard3(historyShift.getLateCard3());
        shift.setEarlyCard1(historyShift.getEarlyCard1());
        shift.setPostponeCard1(historyShift.getPostponeCard1());
        shift.setEarlyCard2(historyShift.getEarlyCard2());
        shift.setPostponeCard2(historyShift.getPostponeCard2());
        shift.setEarlyCard3(historyShift.getEarlyCard3());
        shift.setPostponeCard3(historyShift.getPostponeCard3());
        shift.setRestTimeStatus(historyShift.getRestTimeStatus());
        shift.setRestStartTime(historyShift.getRestStartTime());
        shift.setRestEndTime(historyShift.getRestEndTime());
        shift.setIsDefaultSetting(historyShift.getIsDefaultSetting());
        shift.setEffectTime(historyShift.getEffectTime());
        shift.setCreateUserId(historyShift.getCreateUserId() != null ? Long.valueOf(historyShift.getCreateUserId()) : null);
        shift.setCreateTime(historyShift.getCreateTime());
        shift.setUpdateUserId(historyShift.getUpdateUserId() != null ? Long.valueOf(historyShift.getUpdateUserId()) : null);
        shift.setUpdateTime(historyShift.getUpdateTime());
        return shift;
    }

    /**
     * 优先按 classId 解析班次；若 classId 无效，则继续按当前排班组/月内可用考勤组的 shiftSetting 兜底。
     */
    private HrmAttendanceShift resolveShiftByGroupAndDate(Map<Long, HrmAttendanceShift> shiftCache,
                                                           Long classId, Long groupId, List<Long> fallbackGroupIds,
                                                           LocalDate workDate,
                                                           Map<Long, HrmAttendanceGroup> groupCache) {
        if (classId != null) {
            HrmAttendanceShift shift = resolveShiftOrHistory(shiftCache, classId);
            if (shift != null) {
                return shift;
            }
        }
        if (workDate == null) {
            return null;
        }
        Set<Long> candidateGroupIds = new LinkedHashSet<>();
        if (groupId != null) {
            candidateGroupIds.add(groupId);
        }
        if (fallbackGroupIds != null) {
            candidateGroupIds.addAll(fallbackGroupIds);
        }
        for (Long candidateGroupId : candidateGroupIds) {
            HrmAttendanceShift shift = resolveShiftFromGroupAndDate(shiftCache, candidateGroupId, workDate, groupCache);
            if (shift != null) {
                return shift;
            }
        }
        return null;
    }

    private HrmAttendanceShift resolveShiftFromGroupAndDate(Map<Long, HrmAttendanceShift> shiftCache,
                                                             Long groupId,
                                                             LocalDate workDate,
                                                             Map<Long, HrmAttendanceGroup> groupCache) {
        if (groupId == null || workDate == null) {
            return null;
        }
        HrmAttendanceGroup group = resolveAttendanceGroup(groupId, groupCache);
        if (group == null || group.getShiftSetting() == null || group.getShiftSetting().trim().isEmpty()) {
            return null;
        }
        String[] shiftIds = group.getShiftSetting().split(",");
        int index = workDate.getDayOfWeek().getValue() - 1; // shiftSetting: Monday ... Sunday
        if (index < 0 || index >= shiftIds.length) {
            return null;
        }
        String shiftIdStr = shiftIds[index].trim();
        if (shiftIdStr.isEmpty()) {
            return null;
        }
        try {
            Long shiftId = Long.parseLong(shiftIdStr);
            return resolveShiftOrHistory(shiftCache, shiftId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private HrmAttendanceShift resolveShiftOrHistory(Map<Long, HrmAttendanceShift> shiftCache, Long shiftId) {
        HrmAttendanceShift shift = resolveShift(shiftCache, shiftId);
        if (shift != null) {
            return shift;
        }
        return resolveHistoryShift(shiftId);
    }

    private HrmAttendanceGroup resolveAttendanceGroup(Long groupId, Map<Long, HrmAttendanceGroup> groupCache) {
        if (groupId == null) {
            return null;
        }
        if (groupCache.containsKey(groupId)) {
            return groupCache.get(groupId);
        }
        HrmAttendanceGroup group = attendanceGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            group = attendanceGroupRepository.findFirstByOldGroupId(groupId).orElse(null);
        }
        groupCache.put(groupId, group);
        return group;
    }

    private HrmAttendanceShift resolveDateShift(Long employeeId, LocalDate workDate) {
        if (employeeId == null || workDate == null) {
            return null;
        }
        Date end = toDate(workDate.atTime(LocalTime.MAX));
        HrmAttendanceDateShift dateShift = attendanceDateShiftRepository
                .findFirstByEmployeeIdAndUserShiftTimeLessThanEqualOrderByUserShiftTimeDesc(employeeId, end)
                .orElse(null);
        if (dateShift == null) {
            return null;
        }
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(dateShift.getShiftId());
        shift.setShiftType(dateShift.getShiftType());
        shift.setShiftName(dateShift.getShiftName());
        shift.setShiftHours(dateShift.getShiftHours());
        shift.setStart1(dateShift.getStart1());
        shift.setEnd1(dateShift.getEnd1());
        shift.setStart2(dateShift.getStart2());
        shift.setEnd2(dateShift.getEnd2());
        shift.setStart3(dateShift.getStart3());
        shift.setEnd3(dateShift.getEnd3());
        shift.setAdvanceCard1(dateShift.getAdvanceCard1());
        shift.setLateCard1(dateShift.getLateCard1());
        shift.setAdvanceCard2(dateShift.getAdvanceCard2());
        shift.setLateCard2(dateShift.getLateCard2());
        shift.setAdvanceCard3(dateShift.getAdvanceCard3());
        shift.setLateCard3(dateShift.getLateCard3());
        shift.setEarlyCard1(dateShift.getEarlyCard1());
        shift.setPostponeCard1(dateShift.getPostponeCard1());
        shift.setEarlyCard2(dateShift.getEarlyCard2());
        shift.setPostponeCard2(dateShift.getPostponeCard2());
        shift.setEarlyCard3(dateShift.getEarlyCard3());
        shift.setPostponeCard3(dateShift.getPostponeCard3());
        shift.setRestTimeStatus(dateShift.getRestTimeStatus());
        shift.setRestStartTime(dateShift.getRestStartTime());
        shift.setRestEndTime(dateShift.getRestEndTime());
        shift.setIsDefaultSetting(dateShift.getIsDefaultSetting());
        shift.setEffectTime(dateShift.getEffectTime());
        shift.setCreateUserId(dateShift.getCreateUserId());
        shift.setCreateTime(dateShift.getCreateTime());
        shift.setUpdateUserId(dateShift.getUpdateUserId());
        shift.setUpdateTime(dateShift.getUpdateTime());
        return shift;
    }

    private Map<String, HrmAttendanceShift> buildLocalCustomShiftMap(Long employeeId,
                                                                     Date begin,
                                                                     Date end,
                                                                     Map<Long, HrmAttendanceShift> shiftCache) {
        if (employeeId == null || begin == null || end == null) {
            return Collections.emptyMap();
        }
        Optional<tbattendanceuser> attendanceUser = attendanceUserRepository.findFirstByEmpId(employeeId);
        if (!attendanceUser.isPresent() || attendanceUser.get().getUserId() == null
                || attendanceUser.get().getUserId().trim().isEmpty()) {
            return Collections.emptyMap();
        }
        List<tbplanlist> localPlans = localPlanRepository.findAllByWorkDateBetweenOrderByIdDesc(begin, end);
        if (localPlans == null || localPlans.isEmpty()) {
            return Collections.emptyMap();
        }
        String userId = attendanceUser.get().getUserId().trim();
        Map<String, HrmAttendanceShift> localCustomShiftMap = new LinkedHashMap<>();
        for (tbplanlist localPlan : localPlans) {
            if (localPlan == null || localPlan.getWorkDate() == null) {
                continue;
            }
            if (!isCustomLocalPlan(localPlan) || !parsePlanUserIds(localPlan.getUserId()).contains(userId)) {
                continue;
            }
            String dayKey = resolveDayKey(localPlan.getWorkDate());
            if (dayKey == null || localCustomShiftMap.containsKey(dayKey)) {
                continue;
            }
            HrmAttendanceShift shift = resolveLocalCustomShift(localPlan, shiftCache);
            if (shift != null) {
                localCustomShiftMap.put(dayKey, shift);
            }
        }
        return localCustomShiftMap;
    }

    private HrmAttendanceShift resolveLocalCustomShift(tbplanlist localPlan,
                                                       Map<Long, HrmAttendanceShift> shiftCache) {
        if (localPlan == null) {
            return null;
        }
        if (localPlan.getCustomShiftId() != null) {
            HrmWorkPlanCustomShift customShift = customShiftRepository.findById(localPlan.getCustomShiftId()).orElse(null);
            if (customShift != null) {
                return toAttendanceShift(customShift);
            }
        }
        Long legacyShiftId = parseLongValue(localPlan.getClassId());
        if (legacyShiftId == null || legacyShiftId >= 0) {
            return null;
        }
        HrmAttendanceShift legacyShift = resolveShift(shiftCache, legacyShiftId);
        if (legacyShift == null || !isLegacyLocalCustomShift(legacyShift)) {
            return null;
        }
        return legacyShift;
    }

    private HrmAttendanceShift toAttendanceShift(HrmWorkPlanCustomShift customShift) {
        if (customShift == null) {
            return null;
        }
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(customShift.getId());
        shift.setShiftType(1);
        shift.setShiftName(customShift.getShiftName());
        shift.setShiftHours(customShift.getShiftHours());
        shift.setStart1(customShift.getStart1());
        shift.setEnd1(customShift.getEnd1());
        shift.setCreateTime(customShift.getCreateTime());
        shift.setUpdateTime(customShift.getUpdateTime());
        return shift;
    }

    private boolean isCustomLocalPlan(tbplanlist localPlan) {
        if (localPlan == null) {
            return false;
        }
        if (localPlan.getCustomShiftId() != null) {
            return true;
        }
        String shiftType = localPlan.getShiftType();
        return shiftType != null
                && ("custom".equalsIgnoreCase(shiftType.trim())
                || "2".equals(shiftType.trim())
                || "自定义班次".equals(shiftType.trim()));
    }

    private List<String> parsePlanUserIds(String userIdText) {
        if (userIdText == null || userIdText.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = new LinkedHashSet<>();
        for (String item : userIdText.split(",")) {
            if (item != null && !item.trim().isEmpty()) {
                userIds.add(item.trim());
            }
        }
        return new ArrayList<>(userIds);
    }

    private boolean isLegacyLocalCustomShift(HrmAttendanceShift shift) {
        return shift != null
                && shift.getShiftId() != null
                && shift.getShiftId() < 0
                && shift.getShiftName() != null
                && shift.getShiftName().startsWith("自定义班次 ")
                && hasSingleSectionShiftTime(shift);
    }

    private boolean hasSingleSectionShiftTime(HrmAttendanceShift shift) {
        return shift != null
                && shift.getStart1() != null && !shift.getStart1().trim().isEmpty()
                && shift.getEnd1() != null && !shift.getEnd1().trim().isEmpty()
                && (shift.getStart2() == null || shift.getStart2().trim().isEmpty())
                && (shift.getEnd2() == null || shift.getEnd2().trim().isEmpty())
                && (shift.getStart3() == null || shift.getStart3().trim().isEmpty())
                && (shift.getEnd3() == null || shift.getEnd3().trim().isEmpty());
    }

    private Long parseLongValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildShiftResolutionTrace(LocalDate workDate,
                                             List<HrmAttendancePlan> dailyPlans,
                                             HrmAttendancePlan selectedPlan,
                                             List<Long> fallbackGroupIds,
                                             Map<Long, HrmAttendanceShift> shiftCache,
                                             Map<Long, HrmAttendanceGroup> groupCache) {
        Long employeeId = resolveTraceEmployeeId(selectedPlan, dailyPlans);
        return buildShiftResolutionTrace(
                workDate,
                dailyPlans,
                selectedPlan,
                fallbackGroupIds,
                shiftCache,
                groupCache,
                resolveDateShift(employeeId, workDate)
        );
    }

    private String buildShiftResolutionTrace(LocalDate workDate,
                                             List<HrmAttendancePlan> dailyPlans,
                                             HrmAttendancePlan selectedPlan,
                                             List<Long> fallbackGroupIds,
                                             Map<Long, HrmAttendanceShift> shiftCache,
                                             Map<Long, HrmAttendanceGroup> groupCache,
                                             HrmAttendanceShift resolvedDateShift) {
        Set<Long> candidateGroupIds = new LinkedHashSet<>();
        if (selectedPlan != null && selectedPlan.getGroupId() != null) {
            candidateGroupIds.add(selectedPlan.getGroupId());
        }
        if (fallbackGroupIds != null) {
            candidateGroupIds.addAll(fallbackGroupIds);
        }
        Long classId = selectedPlan != null ? selectedPlan.getClassId() : null;
        Long employeeId = resolveTraceEmployeeId(selectedPlan, dailyPlans);
        HrmAttendanceShift resolvedClassShift = classId != null ? resolveShiftOrHistory(shiftCache, classId) : null;
        String groupDiagnostics = candidateGroupIds.stream()
                .map(groupId -> buildGroupResolutionTrace(groupId, workDate, shiftCache, groupCache))
                .collect(Collectors.joining(" || "));
        return String.format(
                Locale.ROOT,
                "[daily-resolution] workDate=%s selectedPlanId=%s selectedEmployeeId=%s selectedClassId=%s selectedGroupId=%s allPlans=%s selectedClassResolvedShift=%s dateShiftResolved=%s candidateGroups=%s",
                workDate,
                selectedPlan != null ? selectedPlan.getPlanId() : null,
                employeeId,
                classId,
                selectedPlan != null ? selectedPlan.getGroupId() : null,
                formatPlanSummary(dailyPlans),
                formatShiftSummary(resolvedClassShift),
                formatShiftSummary(resolvedDateShift),
                groupDiagnostics
        );
    }

    private Long resolveTraceEmployeeId(HrmAttendancePlan selectedPlan, List<HrmAttendancePlan> dailyPlans) {
        if (selectedPlan != null && selectedPlan.getEmpId() != null) {
            return selectedPlan.getEmpId();
        }
        if (dailyPlans == null || dailyPlans.isEmpty()) {
            return null;
        }
        return dailyPlans.stream()
                .filter(Objects::nonNull)
                .map(HrmAttendancePlan::getEmpId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String buildGroupResolutionTrace(Long candidateGroupId,
                                             LocalDate workDate,
                                             Map<Long, HrmAttendanceShift> shiftCache,
                                             Map<Long, HrmAttendanceGroup> groupCache) {
        if (candidateGroupId == null) {
            return "groupId=null";
        }
        HrmAttendanceGroup resolvedGroup = resolveAttendanceGroup(candidateGroupId, groupCache);
        if (resolvedGroup == null) {
            return String.format(Locale.ROOT, "groupId=%s route=MISS", candidateGroupId);
        }
        int index = workDate != null ? workDate.getDayOfWeek().getValue() - 1 : -1;
        String shiftSetting = resolvedGroup.getShiftSetting();
        String shiftIdToken = null;
        HrmAttendanceShift resolvedShift = null;
        if (shiftSetting != null && !shiftSetting.trim().isEmpty() && index >= 0) {
            String[] shiftIds = shiftSetting.split(",");
            if (index < shiftIds.length) {
                shiftIdToken = shiftIds[index].trim();
                if (!shiftIdToken.isEmpty()) {
                    try {
                        resolvedShift = resolveShiftOrHistory(shiftCache, Long.parseLong(shiftIdToken));
                    } catch (NumberFormatException ignored) {
                        resolvedShift = null;
                    }
                }
            }
        }
        return String.format(
                Locale.ROOT,
                "groupId=%s route=%s resolvedGroupId=%s oldGroupId=%s oldSetting=%s effectTime=%s shiftSetting=%s index=%s shiftToken=%s resolvedShift=%s",
                candidateGroupId,
                Objects.equals(resolvedGroup.getAttendanceGroupId(), candidateGroupId) ? "CURRENT_ID"
                        : (Objects.equals(resolvedGroup.getOldGroupId(), candidateGroupId) ? "OLD_GROUP_ID" : "CACHE_OR_UNKNOWN"),
                resolvedGroup.getAttendanceGroupId(),
                resolvedGroup.getOldGroupId(),
                resolvedGroup.getOldSetting(),
                resolvedGroup.getEffectTime(),
                shiftSetting,
                index,
                shiftIdToken,
                formatShiftSummary(resolvedShift)
        );
    }

    private String formatPlanSummary(List<HrmAttendancePlan> dailyPlans) {
        if (dailyPlans == null || dailyPlans.isEmpty()) {
            return "[]";
        }
        return dailyPlans.stream()
                .filter(Objects::nonNull)
                .map(plan -> String.format(
                        Locale.ROOT,
                        "{id=%s,type=%s,classId=%s,groupId=%s,planCheckTime=%s}",
                        plan.getPlanId(),
                        plan.getCheckType(),
                        plan.getClassId(),
                        plan.getGroupId(),
                        resolvePlanCheckTime(plan)
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatShiftSummary(HrmAttendanceShift shift) {
        if (shift == null) {
            return "null";
        }
        return String.format(
                Locale.ROOT,
                "{shiftId=%s,start=%s,end=%s,effectTime=%s}",
                shift.getShiftId(),
                firstNotBlank(shift.getStart3(), shift.getStart2(), shift.getStart1()),
                firstNotBlank(shift.getEnd3(), shift.getEnd2(), shift.getEnd1()),
                shift.getEffectTime()
        );
    }

    private Map<Long, String> buildDeptNameMap(List<HrmEmployee> employees) {
        Set<Long> deptIds = (employees != null ? employees : Collections.<HrmEmployee>emptyList()).stream()
                .map(HrmEmployee::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrmDept> deptList = deptRepository.findAllByDeptIdIn(new ArrayList<>(deptIds));
        return (deptList != null ? deptList : Collections.<HrmDept>emptyList()).stream()
                .filter(dept -> dept.getDeptId() != null)
                .collect(Collectors.toMap(HrmDept::getDeptId, HrmDept::getName, (left, right) -> left));
    }

    private String resolveClockDayKey(HrmAttendanceClock clock) {
        if (clock == null) {
            return null;
        }
        Date businessDate = clock.getAttendanceTime() != null ? clock.getAttendanceTime() : clock.getWorkDate();
        return resolveDayKey(businessDate);
    }

    private String resolveDayKey(Date date) {
        if (date == null) {
            return null;
        }
        return DAY_FORMATTER.format(toLocalDateTime(date).toLocalDate());
    }

    private String resolveAttendanceDetailDayKey(tbattendancedetail detail) {
        if (detail == null) {
            return null;
        }
        Date businessDate = firstNonNull(
                detail.getPlanCheckTime(),
                detail.getWorkDate(),
                detail.getUserCheckTime()
        );
        return resolveDayKey(businessDate);
    }

    private LocalDate parseDayKey(String dayKey) {
        if (dayKey == null || dayKey.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dayKey.trim(), DAY_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private List<PunchRecord> buildPunchRecords(List<HrmAttendanceClock> dayClocks,
                                                List<tbattendancedetail> dayDetails) {
        Map<String, PunchRecord> recordMap = new LinkedHashMap<>();
        for (HrmAttendanceClock clock : dayClocks != null ? dayClocks : Collections.<HrmAttendanceClock>emptyList()) {
            PunchRecord record = toPunchRecord(clock);
            if (record != null) {
                recordMap.putIfAbsent(record.uniqueKey(), record);
            }
        }
        for (tbattendancedetail detail : dayDetails != null ? dayDetails : Collections.<tbattendancedetail>emptyList()) {
            PunchRecord record = toPunchRecord(detail);
            if (record != null) {
                recordMap.putIfAbsent(record.uniqueKey(), record);
            }
        }
        return sortPunchRecords(recordMap.values());
    }

    private CrossDayPunchMergeResult mergeCrossDayOffDutyPunchRecords(LocalDate workDate,
                                                                      LocalDateTime scheduledEndTime,
                                                                      List<PunchRecord> dayPunchRecords,
                                                                      List<HrmAttendanceClock> nextDayClocks,
                                                                      List<tbattendancedetail> nextDayDetails) {
        if (workDate == null || scheduledEndTime == null || !scheduledEndTime.toLocalDate().isAfter(workDate)) {
            return new CrossDayPunchMergeResult(
                    dayPunchRecords != null ? dayPunchRecords : Collections.<PunchRecord>emptyList(),
                    Collections.emptySet()
            );
        }
        List<PunchRecord> nextDayPunchRecords = buildPunchRecords(nextDayClocks, nextDayDetails);
        if (nextDayPunchRecords.isEmpty()) {
            return new CrossDayPunchMergeResult(
                    dayPunchRecords != null ? dayPunchRecords : Collections.<PunchRecord>emptyList(),
                    Collections.emptySet()
            );
        }
        LocalDateTime nextDayFirstOnDutyTime = nextDayPunchRecords.stream()
                .filter(record -> record.getType() == PunchRecordType.ON_DUTY)
                .map(PunchRecord::getActualTime)
                .filter(Objects::nonNull)
                .filter(actualTime -> actualTime.toLocalDate().isAfter(workDate))
                .min(LocalDateTime::compareTo)
                .orElse(null);

        Map<String, PunchRecord> mergedRecordMap = new LinkedHashMap<>();
        Set<String> consumedPunchKeys = new LinkedHashSet<>();
        for (PunchRecord record : dayPunchRecords != null ? dayPunchRecords : Collections.<PunchRecord>emptyList()) {
            if (record != null) {
                mergedRecordMap.putIfAbsent(record.uniqueKey(), record);
            }
        }
        for (PunchRecord record : nextDayPunchRecords) {
            if (record == null || record.getType() != PunchRecordType.OFF_DUTY || record.getActualTime() == null) {
                continue;
            }
            if (!record.getActualTime().toLocalDate().isAfter(workDate)) {
                continue;
            }
            if (nextDayFirstOnDutyTime != null && record.getActualTime().isAfter(nextDayFirstOnDutyTime)) {
                continue;
            }
            mergedRecordMap.putIfAbsent(record.uniqueKey(), record);
            consumedPunchKeys.add(record.uniqueKey());
        }
        return new CrossDayPunchMergeResult(sortPunchRecords(mergedRecordMap.values()), consumedPunchKeys);
    }

    private List<PunchRecord> sortPunchRecords(java.util.Collection<PunchRecord> records) {
        return (records != null ? records : Collections.<PunchRecord>emptyList()).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(PunchRecord::getActualTime)
                        .thenComparing(record -> record.getType() == PunchRecordType.ON_DUTY ? 0 : 1)
                        .thenComparing(PunchRecord::getSource)
                        .thenComparing(PunchRecord::getSourceId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private PunchRecord toPunchRecord(HrmAttendanceClock clock) {
        if (clock == null) {
            return null;
        }
        PunchRecordType type = PunchRecordType.fromClockType(clock.getClockType());
        LocalDateTime actualTime = firstNonNull(
                clock.getClockTime() != null ? toLocalDateTime(clock.getClockTime()) : null,
                clock.getAttendanceTime() != null ? toLocalDateTime(clock.getAttendanceTime()) : null
        );
        if (type == null || actualTime == null) {
            return null;
        }
        LocalDateTime scheduledTime = clock.getAttendanceTime() != null ? toLocalDateTime(clock.getAttendanceTime()) : null;
        return new PunchRecord(
                PunchSource.CLOCK,
                clock.getClockId(),
                type,
                scheduledTime,
                actualTime
        );
    }

    private PunchRecord toPunchRecord(tbattendancedetail detail) {
        if (detail == null) {
            return null;
        }
        PunchRecordType type = PunchRecordType.fromCheckType(detail.getCheckType());
        LocalDateTime actualTime = firstNonNull(
                detail.getUserCheckTime() != null ? toLocalDateTime(detail.getUserCheckTime()) : null,
                detail.getPlanCheckTime() != null ? toLocalDateTime(detail.getPlanCheckTime()) : null
        );
        if (type == null || actualTime == null) {
            return null;
        }
        LocalDateTime scheduledTime = detail.getPlanCheckTime() != null ? toLocalDateTime(detail.getPlanCheckTime()) : null;
        return new PunchRecord(
                PunchSource.ATTENDANCE_DETAIL,
                detail.getId(),
                type,
                scheduledTime,
                actualTime
        );
    }

    private List<HrmAttendanceClock> filterConsumedClockRecords(String dayKey,
                                                                List<HrmAttendanceClock> clocks,
                                                                Map<String, Set<String>> consumedCarryoverPunchKeys) {
        Set<String> consumedPunchKeys = consumedCarryoverPunchKeys.getOrDefault(dayKey, Collections.emptySet());
        if (consumedPunchKeys.isEmpty()) {
            return clocks != null ? clocks : Collections.<HrmAttendanceClock>emptyList();
        }
        return (clocks != null ? clocks : Collections.<HrmAttendanceClock>emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(clock -> {
                    PunchRecord record = toPunchRecord(clock);
                    return record == null || !consumedPunchKeys.contains(record.uniqueKey());
                })
                .collect(Collectors.toList());
    }

    private List<tbattendancedetail> filterConsumedAttendanceDetailRecords(String dayKey,
                                                                           List<tbattendancedetail> details,
                                                                           Map<String, Set<String>> consumedCarryoverPunchKeys) {
        Set<String> consumedPunchKeys = consumedCarryoverPunchKeys.getOrDefault(dayKey, Collections.emptySet());
        if (consumedPunchKeys.isEmpty()) {
            return details != null ? details : Collections.<tbattendancedetail>emptyList();
        }
        return (details != null ? details : Collections.<tbattendancedetail>emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(detail -> {
                    PunchRecord record = toPunchRecord(detail);
                    return record == null || !consumedPunchKeys.contains(record.uniqueKey());
                })
                .collect(Collectors.toList());
    }

    private void rememberConsumedCarryoverPunchKeys(Map<String, Set<String>> consumedCarryoverPunchKeys,
                                                    String dayKey,
                                                    Set<String> consumedPunchKeys) {
        if (dayKey == null || consumedPunchKeys == null || consumedPunchKeys.isEmpty()) {
            return;
        }
        consumedCarryoverPunchKeys
                .computeIfAbsent(dayKey, ignored -> new LinkedHashSet<>())
                .addAll(consumedPunchKeys);
    }

    private String formatPunchRecords(List<PunchRecord> punchRecords) {
        if (punchRecords == null || punchRecords.isEmpty()) {
            return "[]";
        }
        return punchRecords.stream()
                .map(record -> String.format(
                        Locale.ROOT,
                        "{source=%s,sourceId=%s,type=%s,scheduled=%s,actual=%s}",
                        record.getSource(),
                        record.getSourceId(),
                        record.getType(),
                        record.getScheduledTime(),
                        record.getActualTime()
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatClockRecords(List<HrmAttendanceClock> clocks) {
        if (clocks == null || clocks.isEmpty()) {
            return "[]";
        }
        return clocks.stream()
                .filter(Objects::nonNull)
                .map(clock -> String.format(
                        Locale.ROOT,
                        "{clockId=%s,type=%s,attendance=%s,clock=%s,workDate=%s}",
                        clock.getClockId(),
                        clock.getClockType(),
                        clock.getAttendanceTime() != null ? toLocalDateTime(clock.getAttendanceTime()) : null,
                        clock.getClockTime() != null ? toLocalDateTime(clock.getClockTime()) : null,
                        clock.getWorkDate() != null ? toLocalDateTime(clock.getWorkDate()).toLocalDate() : null
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatClockRecordsZh(List<HrmAttendanceClock> clocks) {
        if (clocks == null || clocks.isEmpty()) {
            return "[]";
        }
        return clocks.stream()
                .filter(Objects::nonNull)
                .map(clock -> String.format(
                        Locale.ROOT,
                        "{记录ID=%s, 打卡类型=%s, 计划打卡时间=%s, 实际打卡时间=%s, 工作日=%s}",
                        clock.getClockId(),
                        Objects.equals(clock.getClockType(), 1) ? "上班" : (Objects.equals(clock.getClockType(), 2) ? "下班" : "未知"),
                        clock.getAttendanceTime() != null ? toLocalDateTime(clock.getAttendanceTime()) : null,
                        clock.getClockTime() != null ? toLocalDateTime(clock.getClockTime()) : null,
                        clock.getWorkDate() != null ? toLocalDateTime(clock.getWorkDate()).toLocalDate() : null
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatAttendanceDetailRecords(List<tbattendancedetail> details) {
        if (details == null || details.isEmpty()) {
            return "[]";
        }
        return details.stream()
                .filter(Objects::nonNull)
                .map(detail -> String.format(
                        Locale.ROOT,
                        "{id=%s,type=%s,plan=%s,actual=%s,workDate=%s}",
                        detail.getId(),
                        detail.getCheckType(),
                        detail.getPlanCheckTime() != null ? toLocalDateTime(detail.getPlanCheckTime()) : null,
                        detail.getUserCheckTime() != null ? toLocalDateTime(detail.getUserCheckTime()) : null,
                        detail.getWorkDate() != null ? toLocalDateTime(detail.getWorkDate()).toLocalDate() : null
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatAttendanceDetailRecordsZh(List<tbattendancedetail> details) {
        if (details == null || details.isEmpty()) {
            return "[]";
        }
        return details.stream()
                .filter(Objects::nonNull)
                .map(detail -> String.format(
                        Locale.ROOT,
                        "{记录ID=%s, 打卡类型=%s, 计划打卡时间=%s, 实际打卡时间=%s, 工作日=%s}",
                        detail.getId(),
                        "OnDuty".equalsIgnoreCase(detail.getCheckType()) ? "上班"
                                : ("OffDuty".equalsIgnoreCase(detail.getCheckType()) ? "下班" : detail.getCheckType()),
                        detail.getPlanCheckTime() != null ? toLocalDateTime(detail.getPlanCheckTime()) : null,
                        detail.getUserCheckTime() != null ? toLocalDateTime(detail.getUserCheckTime()) : null,
                        detail.getWorkDate() != null ? toLocalDateTime(detail.getWorkDate()).toLocalDate() : null
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatPunchRecordsZh(List<PunchRecord> punchRecords) {
        if (punchRecords == null || punchRecords.isEmpty()) {
            return "[]";
        }
        return punchRecords.stream()
                .map(record -> String.format(
                        Locale.ROOT,
                        "{来源=%s, 来源ID=%s, 打卡类型=%s, 计划时间=%s, 实际时间=%s}",
                        record.getSource() == PunchSource.CLOCK ? "打卡表" : "打卡明细表",
                        record.getSourceId(),
                        record.getType() == PunchRecordType.ON_DUTY ? "上班" : "下班",
                        record.getScheduledTime(),
                        record.getActualTime()
                ))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private ScheduledStartTimeResolution resolveScheduledStartTimeResolution(LocalDate workDate,
                                                                            List<HrmAttendancePlan> dayPlans,
                                                                            HrmAttendanceShift shift) {
        LocalDateTime shiftStartTime = resolveShiftStartTime(workDate, shift);
        if (shiftStartTime != null) {
            return new ScheduledStartTimeResolution(shiftStartTime, ScheduledStartTimeSource.SHIFT);
        }
        LocalDateTime onDutyPlanStartTime = (dayPlans != null ? dayPlans : Collections.<HrmAttendancePlan>emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(plan -> "OnDuty".equalsIgnoreCase(plan.getCheckType()))
                .map(this::resolvePlanCheckTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return new ScheduledStartTimeResolution(
                onDutyPlanStartTime,
                onDutyPlanStartTime != null ? ScheduledStartTimeSource.PLAN_CHECK_TIME : ScheduledStartTimeSource.NONE
        );
    }

    private LocalDateTime resolveShiftStartTime(LocalDate workDate, HrmAttendanceShift shift) {
        if (workDate == null || shift == null) {
            return null;
        }
        String shiftStart = firstNotBlank(shift.getStart1(), shift.getStart2(), shift.getStart3());
        if (shiftStart == null) {
            return null;
        }
        try {
            return LocalDateTime.of(workDate, LocalTime.parse(shiftStart, DateTimeFormatter.ofPattern("H:mm", Locale.CHINA)));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime resolveFirstOnDutyTime(List<PunchRecord> punchRecords) {
        return (punchRecords != null ? punchRecords : Collections.<PunchRecord>emptyList()).stream()
                .filter(record -> record.getType() == PunchRecordType.ON_DUTY)
                .map(PunchRecord::getActualTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime resolveEffectiveStartTime(LocalDateTime firstOnDutyTime, LocalDateTime scheduledStartTime) {
        if (firstOnDutyTime == null) {
            return null;
        }
        if (scheduledStartTime != null && firstOnDutyTime.isBefore(scheduledStartTime)) {
            return scheduledStartTime;
        }
        return firstOnDutyTime;
    }

    private OvertimeComputation buildOvertimeComputation(LocalDateTime firstOnDutyTime,
                                                         LocalDateTime scheduledStartTime,
                                                         LocalDateTime effectiveStartTime,
                                                         LocalDateTime lastOffDutyTime) {
        Duration actualWorkDuration = calculateActualWorkDuration(effectiveStartTime, lastOffDutyTime);
        long actualWorkMinutes = actualWorkDuration.toMinutes();
        Duration rawOvertimeDuration = actualWorkDuration.compareTo(Duration.ofHours(8)) > 0
                ? actualWorkDuration.minusHours(8)
                : Duration.ZERO;
        long rawOvertimeMinutes = Math.max(rawOvertimeDuration.toMinutes(), 0L);
        BigDecimal rawOvertimeHours = BigDecimal.valueOf(rawOvertimeMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal overtimeHours = rawOvertimeHours;
        return new OvertimeComputation(
                firstOnDutyTime,
                scheduledStartTime,
                effectiveStartTime,
                actualWorkDuration,
                actualWorkMinutes,
                rawOvertimeDuration,
                rawOvertimeMinutes,
                rawOvertimeHours,
                overtimeHours
        );
    }

    private Duration calculateActualWorkDuration(LocalDateTime firstOnDutyTime, LocalDateTime lastOffDutyTime) {
        if (firstOnDutyTime == null || lastOffDutyTime == null || lastOffDutyTime.isBefore(firstOnDutyTime)) {
            return Duration.ZERO;
        }
        return Duration.between(firstOnDutyTime, lastOffDutyTime);
    }

    private boolean shouldLogFocusConsole(HrmEmployee employee, LocalDate workDate) {
        return shouldDebugEmployee(employee) && Objects.equals(workDate, DEBUG_FOCUS_DATE);
    }

    private String buildFocusRawConsoleMessage(HrmEmployee employee,
                                               LocalDate workDate,
                                               HrmAttendancePlan plan,
                                               HrmAttendanceShift shift,
                                               ScheduledStartTimeResolution scheduledStartResolution,
                                               OvertimeNightClockResolver.ScheduledEndTimeResolution scheduledEndResolution,
                                               LocalDateTime scheduledStartTime,
                                               LocalDateTime scheduledEndTime,
                                               List<HrmAttendanceClock> dayClocks,
                                               List<tbattendancedetail> dayDetails,
                                               List<PunchRecord> punchRecords) {
        return String.format(
                Locale.ROOT,
                "3月20日加班原始数据：员工姓名=%s，统计日期=%s，班次ID=%s，排班组ID=%s，排班开始时间=%s，排班开始来源=%s，排班结束时间=%s，排班结束来源=%s，原始打卡记录=%s，原始考勤明细=%s，归一后打卡记录=%s",
                employee != null ? employee.getEmployeeName() : null,
                workDate,
                shift != null ? shift.getShiftId() : (plan != null ? plan.getClassId() : null),
                plan != null ? plan.getGroupId() : null,
                scheduledStartTime,
                scheduledStartResolution != null ? scheduledStartResolution.getSource().getLabel() : ScheduledStartTimeSource.NONE.getLabel(),
                scheduledEndTime,
                formatScheduledEndSourceZh(scheduledEndResolution != null ? scheduledEndResolution.getSource() : OvertimeNightClockResolver.ScheduledEndTimeSource.NONE),
                formatClockRecordsZh(dayClocks),
                formatAttendanceDetailRecordsZh(dayDetails),
                formatPunchRecordsZh(punchRecords)
        );
    }

    private String buildFocusCalculationConsoleMessage(HrmEmployee employee,
                                                       LocalDate workDate,
                                                       LocalDateTime firstOnDutyTime,
                                                       LocalDateTime scheduledStartTime,
                                                       LocalDateTime effectiveStartTime,
                                                       LocalDateTime lastOffDutyTime,
                                                       OvertimeComputation overtimeComputation) {
        boolean clampedBySchedule = firstOnDutyTime != null
                && scheduledStartTime != null
                && firstOnDutyTime.isBefore(scheduledStartTime);
        String startReason = clampedBySchedule
                ? String.format(Locale.ROOT, "第2步，因首次上班打卡早于排班开始时间，本次由%s调整为%s作为计入工时的开始时间；", firstOnDutyTime, effectiveStartTime)
                : "第2步，首次上班打卡未早于排班开始时间，计入工时的开始时间直接取首次上班打卡；";
        return String.format(
                Locale.ROOT,
                "3月20日加班计算过程：员工姓名=%s，统计日期=%s，首次上班打卡=%s，排班开始时间=%s，计入工时的开始时间=%s，最后下班打卡=%s，实际工作跨度=%s，扣除8小时后剩余=%s，最终加班小时=%s。计算步骤：第1步，读取当天首次上班打卡和最后下班打卡；%s第3步，按计入工时的开始时间到最后下班打卡计算实际工作跨度；第4步，扣除8小时基准工时；第5步，将剩余时长折算成小时并保留两位小数。因此本次输出%s小时。",
                employee != null ? employee.getEmployeeName() : null,
                workDate,
                firstOnDutyTime,
                scheduledStartTime,
                effectiveStartTime,
                lastOffDutyTime,
                formatDurationZh(overtimeComputation.getActualWorkDuration()),
                formatDurationZh(overtimeComputation.getRawOvertimeDuration()),
                overtimeComputation.getOvertimeHours().toPlainString(),
                startReason,
                overtimeComputation.getOvertimeHours().toPlainString()
        );
    }

    private String formatDurationZh(Duration duration) {
        Duration safeDuration = duration != null && !duration.isNegative() ? duration : Duration.ZERO;
        long totalSeconds = safeDuration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d小时%d分%d秒", hours, minutes, seconds);
    }

    private String formatScheduledEndSourceZh(OvertimeNightClockResolver.ScheduledEndTimeSource source) {
        if (source == null) {
            return "无";
        }
        switch (source) {
            case SHIFT:
                return "班次";
            case PLAN_CHECK_TIME:
                return "计划打卡时间";
            case CLOCK_ATTENDANCE:
                return "打卡记录";
            default:
                return "无";
        }
    }

    private LocalDateTime resolveActualOffTime(List<PunchRecord> punchRecords) {
        return (punchRecords != null ? punchRecords : Collections.<PunchRecord>emptyList()).stream()
                .filter(record -> record.getType() == PunchRecordType.OFF_DUTY)
                .map(PunchRecord::getActualTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private long calculateActualWorkMinutes(List<PunchRecord> punchRecords) {
        if (punchRecords == null || punchRecords.isEmpty()) {
            return 0L;
        }
        LocalDateTime firstOnDutyTime = resolveFirstOnDutyTime(punchRecords);
        LocalDateTime lastOffDutyTime = resolveActualOffTime(punchRecords);
        if (firstOnDutyTime == null || lastOffDutyTime == null || lastOffDutyTime.isBefore(firstOnDutyTime)) {
            return 0L;
        }
        return Math.max(java.time.Duration.between(firstOnDutyTime, lastOffDutyTime).toMinutes(), 0L);
    }

    private boolean matchesKeyword(HrmOvertimeNightStatisticsDetail detail, String keyword) {
        String normalizedKeyword = keyword != null ? keyword.trim() : "";
        if (normalizedKeyword.isEmpty()) {
            return true;
        }
        String employeeName = detail.getEmployeeName() != null ? detail.getEmployeeName() : "";
        String jobNumber = detail.getJobNumber() != null ? detail.getJobNumber() : "";
        return employeeName.contains(normalizedKeyword) || jobNumber.contains(normalizedKeyword);
    }

    private List<HrmEmployee> filterEmployees(List<HrmEmployee> employees, String keyword) {
        String normalizedKeyword = keyword != null ? keyword.trim() : "";
        return (employees != null ? employees : Collections.<HrmEmployee>emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(employee -> !Objects.equals(employee.getIsDel(), 1))
                .filter(employee -> {
                    if (normalizedKeyword.isEmpty()) {
                        return true;
                    }
                    String employeeName = employee.getEmployeeName() != null ? employee.getEmployeeName() : "";
                    String jobNumber = employee.getJobNumber() != null ? employee.getJobNumber() : "";
                    return employeeName.contains(normalizedKeyword) || jobNumber.contains(normalizedKeyword);
                })
                .sorted(Comparator.comparing(HrmEmployee::getEmployeeId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private YearMonth resolveMonth(String monthText) {
        if (monthText != null && !monthText.trim().isEmpty()) {
            try {
                return YearMonth.parse(monthText.trim(), MONTH_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }
        return YearMonth.now();
    }

    private YearMonth resolveDetailMonth(HrmOvertimeNightStatisticsDetail detail) {
        if (detail == null) {
            return null;
        }
        if (detail.getStatYear() != null && detail.getStatMonth() != null) {
            return YearMonth.of(detail.getStatYear(), detail.getStatMonth());
        }
        if (detail.getWorkDate() != null) {
            return YearMonth.from(toLocalDateTime(detail.getWorkDate()));
        }
        return null;
    }

    private BigDecimal defaultOvertime(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean shouldDebugEmployee(HrmEmployee employee) {
        return employee != null && DEBUG_EMPLOYEE_NAME.equals(employee.getEmployeeName());
    }

    private void debugTrace(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        try {
            Path parent = DEBUG_TRACE_PATH.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            String line = LocalDateTime.now().format(DEBUG_TIME_FORMATTER) + " " + message + System.lineSeparator();
            Files.write(
                    DEBUG_TRACE_PATH,
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            log.warn("write overtime debug trace failed: {}", ex.getMessage());
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), java.time.ZoneId.systemDefault());
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    private enum OvertimeHoursSourceMode {
        DEFAULT,
        RAW_SYNC_DATA_ONLY
    }

    private enum PunchSource {
        CLOCK,
        ATTENDANCE_DETAIL
    }

    private enum PunchRecordType {
        ON_DUTY,
        OFF_DUTY;

        private static PunchRecordType fromClockType(Integer clockType) {
            if (Objects.equals(clockType, 1)) {
                return ON_DUTY;
            }
            if (Objects.equals(clockType, 2)) {
                return OFF_DUTY;
            }
            return null;
        }

        private static PunchRecordType fromCheckType(String checkType) {
            if ("OnDuty".equalsIgnoreCase(checkType)) {
                return ON_DUTY;
            }
            if ("OffDuty".equalsIgnoreCase(checkType)) {
                return OFF_DUTY;
            }
            return null;
        }
    }

    private static class PunchRecord {
        private final PunchSource source;
        private final Long sourceId;
        private final PunchRecordType type;
        private final LocalDateTime scheduledTime;
        private final LocalDateTime actualTime;

        private PunchRecord(PunchSource source,
                            Long sourceId,
                            PunchRecordType type,
                            LocalDateTime scheduledTime,
                            LocalDateTime actualTime) {
            this.source = source;
            this.sourceId = sourceId;
            this.type = type;
            this.scheduledTime = scheduledTime;
            this.actualTime = actualTime;
        }

        private PunchSource getSource() {
            return source;
        }

        private Long getSourceId() {
            return sourceId;
        }

        private PunchRecordType getType() {
            return type;
        }

        private LocalDateTime getScheduledTime() {
            return scheduledTime;
        }

        private LocalDateTime getActualTime() {
            return actualTime;
        }

        private String uniqueKey() {
            return String.format(
                    Locale.ROOT,
                    "%s|%s|%s",
                    type,
                    actualTime,
                    scheduledTime
            );
        }
    }

    private static class CrossDayPunchMergeResult {
        private final List<PunchRecord> mergedPunchRecords;
        private final Set<String> consumedPunchKeys;

        private CrossDayPunchMergeResult(List<PunchRecord> mergedPunchRecords, Set<String> consumedPunchKeys) {
            this.mergedPunchRecords = mergedPunchRecords != null ? mergedPunchRecords : Collections.<PunchRecord>emptyList();
            this.consumedPunchKeys = consumedPunchKeys != null ? consumedPunchKeys : Collections.<String>emptySet();
        }

        private List<PunchRecord> getMergedPunchRecords() {
            return mergedPunchRecords;
        }

        private Set<String> getConsumedPunchKeys() {
            return consumedPunchKeys;
        }
    }

    private enum ScheduledStartTimeSource {
        SHIFT("班次"),
        PLAN_CHECK_TIME("计划打卡时间"),
        NONE("无");

        private final String label;

        ScheduledStartTimeSource(String label) {
            this.label = label;
        }

        private String getLabel() {
            return label;
        }
    }

    private static final class ScheduledStartTimeResolution {
        private final LocalDateTime time;
        private final ScheduledStartTimeSource source;

        private ScheduledStartTimeResolution(LocalDateTime time, ScheduledStartTimeSource source) {
            this.time = time;
            this.source = source != null ? source : ScheduledStartTimeSource.NONE;
        }

        private LocalDateTime getTime() {
            return time;
        }

        private ScheduledStartTimeSource getSource() {
            return source;
        }
    }

    private static class PunchWorkResult {
        private final long workedMinutes;
        private final int segmentCount;

        private PunchWorkResult(long workedMinutes, int segmentCount) {
            this.workedMinutes = workedMinutes;
            this.segmentCount = segmentCount;
        }

        private long getWorkedMinutes() {
            return workedMinutes;
        }

        private int getSegmentCount() {
            return segmentCount;
        }
    }

    private static class OvertimeNightSummary {
        private static final OvertimeNightSummary ZERO = new OvertimeNightSummary(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                0,
                0,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                0,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "",
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                Collections.emptyList(),
                false,
                ""
        );

        private final BigDecimal overtimeHours;
        private final Integer nightShiftCount;
        private final Integer expectedAttendanceDays;
        private final BigDecimal expectedAttendanceHours;
        private final Integer actualAttendanceDays;
        private final BigDecimal actualAttendanceHours;
        private final String actualAttendanceRemark;
        private final BigDecimal accruedAttendanceHours;
        private final List<DailyOvertimeNightDetailVO> dailyDetails;
        private final boolean manualAttendanceAdjusted;
        private final String calcProcess;

        private OvertimeNightSummary(BigDecimal overtimeHours,
                                     Integer nightShiftCount,
                                     Integer expectedAttendanceDays,
                                     BigDecimal expectedAttendanceHours,
                                     Integer actualAttendanceDays,
                                     BigDecimal actualAttendanceHours,
                                     String actualAttendanceRemark,
                                     BigDecimal accruedAttendanceHours,
                                     List<DailyOvertimeNightDetailVO> dailyDetails,
                                     boolean manualAttendanceAdjusted,
                                     String calcProcess) {
            this.overtimeHours = overtimeHours;
            this.nightShiftCount = nightShiftCount;
            this.expectedAttendanceDays = expectedAttendanceDays;
            this.expectedAttendanceHours = expectedAttendanceHours != null
                    ? expectedAttendanceHours.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.actualAttendanceDays = actualAttendanceDays;
            this.actualAttendanceHours = actualAttendanceHours != null
                    ? actualAttendanceHours.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.actualAttendanceRemark = actualAttendanceRemark != null ? actualAttendanceRemark : "";
            this.accruedAttendanceHours = accruedAttendanceHours != null
                    ? accruedAttendanceHours.setScale(2, RoundingMode.HALF_UP)
                    : null;
            this.dailyDetails = dailyDetails;
            this.manualAttendanceAdjusted = manualAttendanceAdjusted;
            this.calcProcess = calcProcess != null ? calcProcess : "";
        }

        public BigDecimal getOvertimeHours() {
            return overtimeHours;
        }

        public Integer getNightShiftCount() {
            return nightShiftCount;
        }

        public Integer getExpectedAttendanceDays() {
            return expectedAttendanceDays;
        }

        public BigDecimal getExpectedAttendanceHours() {
            return expectedAttendanceHours;
        }

        public Integer getActualAttendanceDays() {
            return actualAttendanceDays;
        }

        public BigDecimal getActualAttendanceHours() {
            return actualAttendanceHours;
        }

        public String getActualAttendanceRemark() {
            return actualAttendanceRemark;
        }

        public BigDecimal getAccruedAttendanceHours() {
            return accruedAttendanceHours;
        }

        public List<DailyOvertimeNightDetailVO> getDailyDetails() {
            return dailyDetails;
        }

        public boolean isManualAttendanceAdjusted() {
            return manualAttendanceAdjusted;
        }

        public String getCalcProcess() {
            return calcProcess;
        }
    }

    private static class ActualAttendanceResolution {
        private final Integer days;
        private final BigDecimal hours;
        private final String remark;
        private final ActualAttendanceDeduction deduction;

        private ActualAttendanceResolution(Integer days, BigDecimal hours, String remark) {
            this(days, hours, remark, ActualAttendanceDeduction.empty());
        }

        private ActualAttendanceResolution(Integer days, BigDecimal hours, String remark, ActualAttendanceDeduction deduction) {
            this.days = days != null ? days : 0;
            this.hours = hours != null
                    ? hours.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.remark = remark != null ? remark : "";
            this.deduction = deduction != null ? deduction : ActualAttendanceDeduction.empty();
        }

        private static ActualAttendanceResolution withoutRemark(Integer days, BigDecimal hours) {
            return new ActualAttendanceResolution(days, hours, "");
        }

        private static ActualAttendanceResolution withoutRemark(Integer days, BigDecimal hours, ActualAttendanceDeduction deduction) {
            return new ActualAttendanceResolution(days, hours, "", deduction);
        }

        private Integer getDays() {
            return days;
        }

        private BigDecimal getHours() {
            return hours;
        }

        private String getRemark() {
            return remark;
        }

        private ActualAttendanceDeduction getDeduction() {
            return deduction;
        }
    }

    private static class ActualAttendanceDeduction {
        private final BigDecimal hours;
        private final String remark;
        private final Map<String, BigDecimal> hoursByType;

        private ActualAttendanceDeduction(BigDecimal hours, String remark) {
            this(hours, remark, Collections.emptyMap());
        }

        private ActualAttendanceDeduction(BigDecimal hours, String remark, Map<String, BigDecimal> hoursByType) {
            this.hours = hours != null
                    ? hours.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.remark = remark != null ? remark : "";
            Map<String, BigDecimal> normalizedHoursByType = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> entry : hoursByType != null ? hoursByType.entrySet() : Collections.<Map.Entry<String, BigDecimal>>emptySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                normalizedHoursByType.put(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP));
            }
            this.hoursByType = Collections.unmodifiableMap(normalizedHoursByType);
        }

        private static ActualAttendanceDeduction empty() {
            return new ActualAttendanceDeduction(BigDecimal.ZERO, "");
        }

        private static ActualAttendanceDeduction of(Map<String, BigDecimal> hoursByType) {
            if (hoursByType == null || hoursByType.isEmpty()) {
                return empty();
            }
            BigDecimal totalHours = hoursByType.values().stream()
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            if (totalHours.compareTo(BigDecimal.ZERO) <= 0) {
                return empty();
            }
            String remark = "扣除：" + hoursByType.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .map(entry -> entry.getKey() + entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString() + "小时")
                    .collect(Collectors.joining("、"));
            return new ActualAttendanceDeduction(totalHours, remark, hoursByType);
        }

        private BigDecimal getHours() {
            return hours;
        }

        private String getRemark() {
            return remark;
        }

        private BigDecimal getHoursByType(String type) {
            return hoursByType.getOrDefault(type, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static class MonthlyAttendanceDays {
        private final Integer expectedAttendanceDays;
        private final BigDecimal expectedAttendanceHours;
        private final Integer actualAttendanceDays;
        private final BigDecimal actualAttendanceHours;
        private final BigDecimal accruedAttendanceHours;

        private MonthlyAttendanceDays(Integer expectedAttendanceDays,
                                      Integer actualAttendanceDays,
                                      BigDecimal actualAttendanceHours,
                                      BigDecimal accruedAttendanceHours) {
            this.expectedAttendanceDays = expectedAttendanceDays != null ? expectedAttendanceDays : 0;
            this.expectedAttendanceHours = BigDecimal.valueOf(this.expectedAttendanceDays)
                    .multiply(APPROVAL_HOURS_PER_DAY)
                    .setScale(2, RoundingMode.HALF_UP);
            this.actualAttendanceDays = actualAttendanceDays != null ? actualAttendanceDays : 0;
            this.actualAttendanceHours = actualAttendanceHours != null
                    ? actualAttendanceHours.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(this.actualAttendanceDays)
                            .multiply(APPROVAL_HOURS_PER_DAY)
                            .setScale(2, RoundingMode.HALF_UP);
            this.accruedAttendanceHours = accruedAttendanceHours != null
                    ? accruedAttendanceHours.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        private Integer getExpectedAttendanceDays() {
            return expectedAttendanceDays;
        }

        private BigDecimal getExpectedAttendanceHours() {
            return expectedAttendanceHours;
        }

        private Integer getActualAttendanceDays() {
            return actualAttendanceDays;
        }

        private BigDecimal getActualAttendanceHours() {
            return actualAttendanceHours;
        }

        private BigDecimal getAccruedAttendanceHours() {
            return accruedAttendanceHours;
        }
    }

    private static class OvertimeComputation {
        private final LocalDateTime firstOnDutyTime;
        private final LocalDateTime scheduledStartTime;
        private final LocalDateTime effectiveStartTime;
        private final Duration actualWorkDuration;
        private final long actualWorkMinutes;
        private final Duration rawOvertimeDuration;
        private final long rawOvertimeMinutes;
        private final BigDecimal rawOvertimeHours;
        private final BigDecimal overtimeHours;

        private OvertimeComputation(LocalDateTime firstOnDutyTime,
                                    LocalDateTime scheduledStartTime,
                                    LocalDateTime effectiveStartTime,
                                    Duration actualWorkDuration,
                                    long actualWorkMinutes,
                                    Duration rawOvertimeDuration,
                                    long rawOvertimeMinutes,
                                    BigDecimal rawOvertimeHours,
                                    BigDecimal overtimeHours) {
            this.firstOnDutyTime = firstOnDutyTime;
            this.scheduledStartTime = scheduledStartTime;
            this.effectiveStartTime = effectiveStartTime;
            this.actualWorkDuration = actualWorkDuration;
            this.actualWorkMinutes = actualWorkMinutes;
            this.rawOvertimeDuration = rawOvertimeDuration;
            this.rawOvertimeMinutes = rawOvertimeMinutes;
            this.rawOvertimeHours = rawOvertimeHours;
            this.overtimeHours = overtimeHours;
        }

        private LocalDateTime getFirstOnDutyTime() {
            return firstOnDutyTime;
        }

        private LocalDateTime getScheduledStartTime() {
            return scheduledStartTime;
        }

        private LocalDateTime getEffectiveStartTime() {
            return effectiveStartTime;
        }

        private Duration getActualWorkDuration() {
            return actualWorkDuration;
        }

        private long getActualWorkMinutes() {
            return actualWorkMinutes;
        }

        private Duration getRawOvertimeDuration() {
            return rawOvertimeDuration;
        }

        private long getRawOvertimeMinutes() {
            return rawOvertimeMinutes;
        }

        private BigDecimal getRawOvertimeHours() {
            return rawOvertimeHours;
        }

        private BigDecimal getOvertimeHours() {
            return overtimeHours;
        }
    }

    private static class OvertimeRecordAggregate {
        private BigDecimal overtimeHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private LocalDateTime scheduledOffTime;
        private LocalDateTime actualOffTime;

        private void merge(HrmEmployeeOverTimeRecord overtimeRecord, java.util.function.Function<Date, LocalDateTime> toLocalDateTime) {
            if (overtimeRecord.getOverTimes() != null) {
                overtimeHours = overtimeHours.add(
                        BigDecimal.valueOf(overtimeRecord.getOverTimes()).setScale(2, RoundingMode.HALF_UP)
                ).setScale(2, RoundingMode.HALF_UP);
            }
            LocalDateTime recordScheduledOffTime = overtimeRecord.getOverTimeStartTime() != null
                    ? toLocalDateTime.apply(overtimeRecord.getOverTimeStartTime()) : null;
            if (recordScheduledOffTime != null
                    && (scheduledOffTime == null || recordScheduledOffTime.isBefore(scheduledOffTime))) {
                scheduledOffTime = recordScheduledOffTime;
            }
            LocalDateTime recordActualOffTime = overtimeRecord.getOverTimeEndTime() != null
                    ? toLocalDateTime.apply(overtimeRecord.getOverTimeEndTime()) : null;
            if (recordActualOffTime != null
                    && (actualOffTime == null || recordActualOffTime.isAfter(actualOffTime))) {
                actualOffTime = recordActualOffTime;
            }
        }

        private boolean hasOvertimeHours() {
            return overtimeHours.compareTo(BigDecimal.ZERO) > 0;
        }

        private BigDecimal getOvertimeHours() {
            return overtimeHours;
        }

        private LocalDateTime getScheduledOffTime() {
            return scheduledOffTime;
        }

        private LocalDateTime getActualOffTime() {
            return actualOffTime;
        }
    }

    private class OvertimeApprovalAggregate {
        private BigDecimal overtimeHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        private void merge(tbattendanceapprove approval, java.util.function.Function<Date, LocalDateTime> toLocalDateTime) {
            overtimeHours = overtimeHours.add(resolveAttendanceApprovalHours(approval)).setScale(2, RoundingMode.HALF_UP);
            LocalDateTime recordStartTime = approval.getBeginTime() != null ? toLocalDateTime.apply(approval.getBeginTime()) : null;
            if (recordStartTime != null && (startTime == null || recordStartTime.isBefore(startTime))) {
                startTime = recordStartTime;
            }
            LocalDateTime recordEndTime = approval.getEndTime() != null ? toLocalDateTime.apply(approval.getEndTime()) : null;
            if (recordEndTime != null && (endTime == null || recordEndTime.isAfter(endTime))) {
                endTime = recordEndTime;
            }
        }

        private boolean hasOvertimeHours() {
            return overtimeHours.compareTo(BigDecimal.ZERO) > 0;
        }

        private BigDecimal getOvertimeHours() {
            return overtimeHours;
        }
    }
}
