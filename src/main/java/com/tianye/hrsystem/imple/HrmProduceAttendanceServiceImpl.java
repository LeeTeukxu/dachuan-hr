package com.tianye.hrsystem.imple;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.QueryMonthAttendanceBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightStatisticsPageBO;
import com.tianye.hrsystem.entity.bo.SyncProduceAttendanceBO;
import com.tianye.hrsystem.entity.bo.UpdateProduceAttendanceCellBO;
import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.vo.AdministrativeAttendanceReportDetailVO;
import com.tianye.hrsystem.entity.vo.AdministrativeAttendanceReportMetricVO;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;
import com.tianye.hrsystem.mapper.HrmProduceAttendanceMapper;
import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmEmployeeQuitInfo;
import com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryBasicMapper;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryConfigService;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeQuitInfoRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmOvertimeNightStatisticsDetailRepository;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmOvertimeNightStatisticsService;
import com.tianye.hrsystem.service.IHrmProduceAttendanceService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HrmProduceAttendanceServiceImpl extends BaseServiceImpl<HrmProduceAttendanceMapper, HrmProduceAttendance> implements IHrmProduceAttendanceService {

    @Autowired
    IHrmEmployeeService hrmEmployeeService;

    @Autowired
    hrmEmployeeRepository employeeRepository;

    @Autowired
    HrmProduceAttendanceMapper produceAttendanceMapper;

    @Autowired
    IHrmOvertimeNightStatisticsService overtimeNightStatisticsService;

    @Autowired
    hrmOvertimeNightStatisticsDetailRepository overtimeNightStatisticsDetailRepository;

    @Autowired
    hrmDeptRepository deptRepository;

    @Autowired
    tbattendanceuserRepository attendanceUserRepository;

    @Autowired
    tbattendanceapproveRepository attendanceApproveRepository;

    private static final int TWO = 2;
    private static final int DEPARTMENT_ADMINISTRATIVE = 1;
    private static final int DEPARTMENT_PRODUCTION = 2;
    private static final int REST_TYPE_FIXED_MONTHLY_REST = 2;
    private static final BigDecimal DEFAULT_OVERTIME_UNIT_PRICE = BigDecimal.valueOf(12);
    private static final BigDecimal DEFAULT_NIGHT_SUBSIDY_UNIT_PRICE = BigDecimal.valueOf(30);
    private static final BigDecimal APPROVAL_HOURS_PER_DAY = BigDecimal.valueOf(8);
    private static final BigDecimal FRACTIONAL_DAY_RANGE_TOLERANCE_HOURS = BigDecimal.valueOf(0.30);
    private static final String STATISTICS_STATUS_CANCELED = "取消至统计";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    HrmSalaryBasicMapper salaryBasicMapper;

    @Autowired
    HrmSalaryConfigService salaryConfigService;

    @Autowired
    hrmEmployeeQuitInfoRepository quitInfoRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveProduceAttendanceData(MultipartFile multipartFile, String dates) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmProduceAttendance> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            Integer year = 0;
            Integer month = 0;
            for (int i = TWO; i < read.size(); i++) {
                HrmProduceAttendance hrmProduceAttendance = new HrmProduceAttendance();
                List<Object> row = read.get(i);
                String EmployeeName = row.get(1).toString().trim();

//                LambdaQueryWrapper<HrmEmployee> wrapper = new QueryWrapper<HrmEmployee>().lambda().select(HrmEmployee::getEmployeeId).eq(HrmEmployee::getEmployeeName, EmployeeName);
//                HrmEmployee employee = hrmEmployeeService.getOne(wrapper);
//                hrmProduceAttendance.setEmployeeId(employee.getEmployeeId());
                HrmEmployee matchedEmployee = null;
                for (HrmEmployee f : listHrmEmployees) {
                    if (f.getEmployeeName().equals(EmployeeName)) {
                        hrmProduceAttendance.setEmployeeId(f.getEmployeeId());
                        hrmProduceAttendance.setDepartment(resolveDepartmentType(f));
                        matchedEmployee = f;
                    }
                }

                String[] date = dates.split("-");
                year = Integer.parseInt(date[0]);
                month = Integer.parseInt(date[1]);
                hrmProduceAttendance.setYear(year);
                hrmProduceAttendance.setMonth(month);
                hrmProduceAttendance.setEmployeeName(EmployeeName);
                if (!row.get(2).toString().trim().equals("")) {
                    hrmProduceAttendance.setPositiveAttendance(new BigDecimal(row.get(2).toString().trim()));
                }else hrmProduceAttendance.setPositiveAttendance(new BigDecimal(0));
                if (!row.get(3).toString().trim().equals("")) {
                    hrmProduceAttendance.setProbationAttendance(new BigDecimal(row.get(3).toString().trim()));
                }else hrmProduceAttendance.setPositiveAttendance(new BigDecimal(0));
                if (!row.get(4).toString().trim().equals("")) {
                    hrmProduceAttendance.setWorkOverTime(new BigDecimal(row.get(4).toString().trim().trim()));
                }
////                if (!row.get(4).toString().equals("")) {
////                    hrmProduceAttendance.setEmptyClass(Integer.parseInt(row.get(4).toString()));
////                }else hrmProduceAttendance.setEmptyClass(0);
////                if (!row.get(5).toString().equals("")) {
////                    hrmProduceAttendance.setMiddleClass(Integer.parseInt(row.get(5).toString()));
//                }else hrmProduceAttendance.setMiddleClass(0);
                if (!row.get(5).toString().trim().equals("")) {
                    hrmProduceAttendance.setNightShift(Integer.parseInt(row.get(5).toString().trim()));
                }else hrmProduceAttendance.setNightShift(0);
                if (!row.get(6).toString().trim().equals("")) {
                    hrmProduceAttendance.setNightSubsidy(new BigDecimal(row.get(6).toString().trim()));
                }else hrmProduceAttendance.setNightSubsidy(new BigDecimal(0));
//                if (!row.get(8).toString().equals("")) {
//                    hrmProduceAttendance.setCurrentMonthVacation(new BigDecimal(row.get(8).toString()));
//                }
                if (!row.get(7).toString().trim().equals("")) {
                    hrmProduceAttendance.setHighTemperature(new BigDecimal(row.get(7).toString().trim()));
                }else hrmProduceAttendance.setHighTemperature(new BigDecimal(0));
                if (!row.get(8).toString().trim().equals("")) {
                    hrmProduceAttendance.setLowTemperature(new BigDecimal(row.get(8).toString().trim()));
                }else hrmProduceAttendance.setLowTemperature(new BigDecimal(0));
                if (!row.get(9).toString().trim().equals("")) {
                    hrmProduceAttendance.setOtherSubsidies(new BigDecimal(row.get(9).toString().trim()));
                }else hrmProduceAttendance.setOtherSubsidies(new BigDecimal(0));
                if (!row.get(10).toString().trim().equals("")) {
                    hrmProduceAttendance.setOtherDeductions(new BigDecimal(row.get(10).toString().trim()));
                }else hrmProduceAttendance.setOtherDeductions(new BigDecimal(0));
                if (!row.get(11).toString().trim().equals("")) {
                    hrmProduceAttendance.setLoan(new BigDecimal(row.get(11).toString().trim()));
                }else hrmProduceAttendance.setLoan(new BigDecimal(0));
                //有BUG
                if (!row.get(12).toString().trim().equals("")) {
                    hrmProduceAttendance.setRemark(row.get(12).toString().trim());
                }
                applyOvertimeNightEligibility(hrmProduceAttendance, matchedEmployee);
                list.add(hrmProduceAttendance);
            }
            LambdaQueryWrapper<HrmProduceAttendance> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmProduceAttendance::getYear, year).eq(HrmProduceAttendance::getMonth, month);
            produceAttendanceMapper.delete(wrappers);
            saveBatch(list);
        }
    }

    @Override
    public Page<QueryMonthAttendanceVO> queryProduceAttendanceList(QueryMonthAttendanceBO queryMonthAttendanceBO) {
        return produceAttendanceMapper.queryProduceAttendanceList(queryMonthAttendanceBO.parse(), queryMonthAttendanceBO);
    }

    @Override
    public void downloadAdministrativeAttendance(QueryMonthAttendanceBO queryMonthAttendanceBO,
                                                 HttpServletResponse response) throws Exception {
        YearMonth targetMonth = resolveAdministrativeAttendanceExportMonth(queryMonthAttendanceBO);
        int payDay = resolveAdministrativePayDay();
        List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> exportRows =
                buildAdministrativeAttendanceExportRows(queryMonthAttendanceBO, targetMonth, payDay);
        byte[] bytes = AdministrativeAttendanceExportSupport.buildWorkbook(
                targetMonth,
                resolveCompanyName(),
                LocalDate.now(),
                exportRows
        );
        writeAdministrativeAttendanceResponse(response, targetMonth, bytes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromOvertimeNightStatistics(SyncProduceAttendanceBO syncProduceAttendanceBO) {
        YearMonth targetMonth = resolveSyncMonth(syncProduceAttendanceBO);
        Map<Long, QueryOvertimeNightStatisticsPageVO> persistedStatisticsByEmployee =
                queryPersistedStatisticsRowsByEmployee(targetMonth);
        List<QueryOvertimeNightStatisticsPageVO> statisticsRows = queryOvertimeNightStatisticsRows(targetMonth);
        statisticsRows = mergePersistedOnlyRows(statisticsRows, persistedStatisticsByEmployee);
        if (statisticsRows.isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<HrmProduceAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrmProduceAttendance::getYear, targetMonth.getYear())
                .eq(HrmProduceAttendance::getMonth, targetMonth.getMonthValue());
        List<HrmProduceAttendance> existingRows = produceAttendanceMapper.selectList(wrapper);
        Map<Long, HrmProduceAttendance> existingByEmployee = (existingRows != null ? existingRows : Collections.<HrmProduceAttendance>emptyList())
                .stream()
                .filter(row -> row.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmProduceAttendance::getEmployeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<Long, HrmEmployee> employeeMap = employeeRepository.findAll().stream()
                .filter(employee -> employee.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmEmployee::getEmployeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        HrmSalaryBasic salaryBasic = resolveLatestSalaryBasic();

        int synced = 0;
        for (QueryOvertimeNightStatisticsPageVO statistics : statisticsRows) {
            if (statistics.getEmployeeId() == null) {
                continue;
            }
            HrmEmployee employee = employeeMap.get(statistics.getEmployeeId());
            HrmProduceAttendance attendance = existingByEmployee.get(statistics.getEmployeeId());
            boolean insert = false;
            if (attendance == null) {
                insert = true;
                attendance = new HrmProduceAttendance();
                attendance.setYear(targetMonth.getYear());
                attendance.setMonth(targetMonth.getMonthValue());
                attendance.setEmployeeId(statistics.getEmployeeId());
            }

            attendance.setEmployeeName(employee != null ? employee.getEmployeeName() : statistics.getEmployeeName());
            attendance.setDepartment(resolveDepartmentType(employee));
            QueryOvertimeNightStatisticsPageVO persistedStatistics = persistedStatisticsByEmployee.get(statistics.getEmployeeId());
            attendance.setPositiveAttendance(resolveActualAttendanceDaysForSync(statistics, persistedStatistics));
            attendance.setProbationAttendance(resolveAccruedAttendanceDaysForSync(statistics, persistedStatistics));
            boolean canCountOvertimeNight = canCountOvertimeNight(employee);
            attendance.setWorkOverTime(canCountOvertimeNight
                    ? resolveAttendanceOvertimeHours(statistics)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            attendance.setNightShift(canCountOvertimeNight && statistics.getNightShiftCount() != null
                    ? statistics.getNightShiftCount()
                    : 0);
            attendance.setOvertimePay(calculateOvertimePay(attendance.getWorkOverTime(), salaryBasic));
            attendance.setNightSubsidy(calculateNightSubsidy(attendance.getNightShift(), salaryBasic));

            if (insert) {
                produceAttendanceMapper.insert(attendance);
            } else {
                produceAttendanceMapper.updateById(attendance);
            }
            synced++;
        }
        return synced;
    }

    private List<QueryOvertimeNightStatisticsPageVO> queryOvertimeNightStatisticsRows(YearMonth targetMonth) {
        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth(targetMonth.format(MONTH_FORMATTER));
        queryBO.setPage(1L);
        queryBO.setLimit(10000L);
        queryBO.setPageType(0);

        BasePage<QueryOvertimeNightStatisticsPageVO> statisticsPage = overtimeNightStatisticsService.queryPageList(queryBO);
        return statisticsPage != null && statisticsPage.getList() != null
                ? statisticsPage.getList()
                : Collections.emptyList();
    }

    private List<QueryOvertimeNightStatisticsPageVO> mergePersistedOnlyRows(
            List<QueryOvertimeNightStatisticsPageVO> statisticsRows,
            Map<Long, QueryOvertimeNightStatisticsPageVO> persistedStatisticsByEmployee) {
        List<QueryOvertimeNightStatisticsPageVO> mergedRows = new ArrayList<>(
                statisticsRows != null ? statisticsRows : Collections.<QueryOvertimeNightStatisticsPageVO>emptyList()
        );
        Map<Long, QueryOvertimeNightStatisticsPageVO> existing = mergedRows.stream()
                .filter(row -> row != null && row.getEmployeeId() != null)
                .collect(Collectors.toMap(QueryOvertimeNightStatisticsPageVO::getEmployeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        for (Map.Entry<Long, QueryOvertimeNightStatisticsPageVO> entry : persistedStatisticsByEmployee.entrySet()) {
            if (!existing.containsKey(entry.getKey())) {
                mergedRows.add(entry.getValue());
            }
        }
        return mergedRows;
    }

    private Map<Long, QueryOvertimeNightStatisticsPageVO> queryPersistedStatisticsRowsByEmployee(YearMonth targetMonth) {
        List<HrmOvertimeNightStatisticsDetail> details = overtimeNightStatisticsDetailRepository
                .findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                );
        Map<Long, PersistedStatisticsAccumulator> accumulators = new LinkedHashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details != null ? details : Collections.<HrmOvertimeNightStatisticsDetail>emptyList()) {
            if (detail == null || detail.getEmployeeId() == null) {
                continue;
            }
            accumulators
                    .computeIfAbsent(detail.getEmployeeId(), PersistedStatisticsAccumulator::new)
                    .accept(detail, targetMonth);
        }
        Map<Long, QueryOvertimeNightStatisticsPageVO> result = new LinkedHashMap<>();
        for (PersistedStatisticsAccumulator accumulator : accumulators.values()) {
            result.put(accumulator.employeeId, accumulator.toRow());
        }
        return result;
    }

    private YearMonth resolveAdministrativeAttendanceExportMonth(QueryMonthAttendanceBO queryMonthAttendanceBO) {
        if (queryMonthAttendanceBO != null
                && queryMonthAttendanceBO.getYear() != null
                && queryMonthAttendanceBO.getMonth() != null
                && queryMonthAttendanceBO.getYear() > 0
                && queryMonthAttendanceBO.getMonth() > 0) {
            return YearMonth.of(queryMonthAttendanceBO.getYear(), queryMonthAttendanceBO.getMonth());
        }
        return YearMonth.now();
    }

    private List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> buildAdministrativeAttendanceExportRows(
            QueryMonthAttendanceBO queryMonthAttendanceBO,
            YearMonth targetMonth,
            int payDay) {
        List<HrmOvertimeNightStatisticsDetail> details = overtimeNightStatisticsDetailRepository
                .findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                );

        Map<Long, HrmEmployee> employeeMap = employeeRepository.findAll().stream()
                .filter(employee -> employee != null && employee.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmEmployee::getEmployeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        // 发薪日(payDay)是"当月是否算在职"的分界：payDay 当天(含)及之后才离职的员工，当月仍按在职导出。
        LocalDate employmentCutoverDate = targetMonth.atDay(Math.min(payDay, targetMonth.lengthOfMonth()));
        Map<Long, LocalDate> quitDateByEmployeeId = resolveAdministrativeQuitDateMap(employeeMap);

        // 三源并集：统计明细优先，考勤汇总（department=1）与行政体系员工表补齐缺失员工，
        // 避免“统计明细只覆盖部分员工”时导出遗漏其余行政体系员工。
        List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows = new ArrayList<>();
        Set<Long> exportedEmployeeIds = new LinkedHashSet<>();
        if (details != null && !details.isEmpty()) {
            appendMissingAdministrativeAttendanceRows(rows, exportedEmployeeIds,
                    buildRowsFromStatisticsDetails(details, employeeMap, queryMonthAttendanceBO, targetMonth,
                            quitDateByEmployeeId, employmentCutoverDate));
        }
        appendMissingAdministrativeAttendanceRows(rows, exportedEmployeeIds,
                buildRowsFromProduceAttendance(employeeMap, queryMonthAttendanceBO, targetMonth,
                        quitDateByEmployeeId, employmentCutoverDate));
        appendMissingAdministrativeAttendanceRows(rows, exportedEmployeeIds,
                buildRowsFromEmployeeList(employeeMap, queryMonthAttendanceBO, targetMonth,
                        quitDateByEmployeeId, employmentCutoverDate));

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        sortAdministrativeAttendanceRows(rows);

        enrichAdministrativeAttendanceApprovalMetrics(rows, targetMonth);
        refreshAdministrativeAttendanceActualHours(rows);
        refreshAdministrativeAttendanceHoursFromStatisticsRows(rows, targetMonth);
        enrichAdministrativeAttendanceReportMetrics(rows, targetMonth);
        attachAdministrativeAttendanceCommentTexts(rows, targetMonth);
        return rows;
    }

    /**
     * 为导出行生成批注：标注每类考勤记录的发生时间与计算过程（全中文说明）
     */
    private void attachAdministrativeAttendanceCommentTexts(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows,
            YearMonth targetMonth) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> employeeIds = rows.stream()
                .map(row -> row.employeeId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return;
        }
        //审批类（事假/病假/调休/年假/出差/加班）发生日期
        Map<Long, Map<String, Set<LocalDate>>> approvalDatesByEmployee =
                collectAdministrativeApprovalDates(targetMonth, employeeIds);
        //考勤报告类（旷工/迟到/早退/缺卡）发生日期
        Map<Long, Map<String, Set<LocalDate>>> reportDatesByEmployee =
                collectAdministrativeReportDates(targetMonth, employeeIds);

        for (AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row : rows) {
            if (row == null) {
                continue;
            }
            Map<String, Set<LocalDate>> approvalDates = approvalDatesByEmployee.getOrDefault(
                    row.employeeId, Collections.emptyMap());
            Map<String, Set<LocalDate>> reportDates = reportDatesByEmployee.getOrDefault(
                    row.employeeId, Collections.emptyMap());
            row.columnComments.put(7, AdministrativeAttendanceExportSupport.buildLeaveCommentText(
                    "事假", joinAdministrativeCommentDates(approvalDates.get("事假")), row.personalLeaveHours));
            row.columnComments.put(8, AdministrativeAttendanceExportSupport.buildLeaveCommentText(
                    "病假", joinAdministrativeCommentDates(approvalDates.get("病假")), row.sickLeaveHours));
            row.columnComments.put(9, AdministrativeAttendanceExportSupport.buildLeaveCommentText(
                    "调休", joinAdministrativeCommentDates(approvalDates.get("调休")), row.compensatoryLeaveHours));
            row.columnComments.put(10, AdministrativeAttendanceExportSupport.buildLeaveCommentText(
                    "年假", joinAdministrativeCommentDates(approvalDates.get("年假")), row.annualLeaveHours));
            row.columnComments.put(11, AdministrativeAttendanceExportSupport.buildTravelCommentText(
                    joinAdministrativeCommentDates(approvalDates.get("出差")), row.travelDays));
            Set<LocalDate> absenteeismDates = new TreeSet<>();
            if (reportDates.get("旷工天数") != null) {
                absenteeismDates.addAll(reportDates.get("旷工天数"));
            }
            if (reportDates.get("旷工迟到天数") != null) {
                absenteeismDates.addAll(reportDates.get("旷工迟到天数"));
            }
            row.columnComments.put(12, AdministrativeAttendanceExportSupport.buildAbsenteeismCommentText(
                    joinAdministrativeCommentDates(absenteeismDates), row.absenteeismDays));
            row.columnComments.put(13, AdministrativeAttendanceExportSupport.buildCountCommentText(
                    "迟到", joinAdministrativeCommentDates(reportDates.get("迟到次数")), row.lateCount,
                    "每天打卡迟到记 1 次，全月累计。"));
            row.columnComments.put(14, AdministrativeAttendanceExportSupport.buildCountCommentText(
                    "早退", joinAdministrativeCommentDates(reportDates.get("早退次数")), row.earlyCount,
                    "每天打卡早退记 1 次，全月累计。"));
            row.columnComments.put(15, AdministrativeAttendanceExportSupport.buildCountCommentText(
                    "上班缺卡", joinAdministrativeCommentDates(reportDates.get("上班缺卡次数")), row.onDutyMissingCardCount,
                    "上班时段没有打卡记录的，每次记 1 次，全月累计。"));
            row.columnComments.put(16, AdministrativeAttendanceExportSupport.buildCountCommentText(
                    "下班缺卡", joinAdministrativeCommentDates(reportDates.get("下班缺卡次数")), row.offDutyMissingCardCount,
                    "下班时段没有打卡记录的，每次记 1 次，全月累计。"));
            row.columnComments.put(17, AdministrativeAttendanceExportSupport.buildTotalMissingCardCommentText(
                    row.onDutyMissingCardCount, row.offDutyMissingCardCount));
            row.columnComments.put(18, AdministrativeAttendanceExportSupport.buildOvertimeCommentText(
                    joinAdministrativeCommentDates(approvalDates.get("加班")), row.overtimeHours));
        }
    }

    private Map<Long, Map<String, Set<LocalDate>>> collectAdministrativeApprovalDates(YearMonth targetMonth,
                                                                                      List<Long> employeeIds) {
        Map<Long, Map<String, Set<LocalDate>>> result = new LinkedHashMap<>();
        List<tbattendanceuser> attendanceUsers = attendanceUserRepository.findAllByEmpIdIn(employeeIds);
        if (attendanceUsers == null || attendanceUsers.isEmpty()) {
            return result;
        }
        Map<String, Long> employeeIdByDingTalkUserId = new LinkedHashMap<>();
        for (tbattendanceuser attendanceUser : attendanceUsers) {
            if (attendanceUser == null || attendanceUser.getEmpId() == null || trimToEmpty(attendanceUser.getUserId()).isEmpty()) {
                continue;
            }
            employeeIdByDingTalkUserId.putIfAbsent(attendanceUser.getUserId().trim(), attendanceUser.getEmpId());
        }
        if (employeeIdByDingTalkUserId.isEmpty()) {
            return result;
        }
        Date begin = toDate(targetMonth.atDay(1).atStartOfDay());
        Date end = toDate(targetMonth.atEndOfMonth().atTime(LocalTime.MAX));
        List<tbattendanceapprove> approvals = attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(
                new ArrayList<>(employeeIdByDingTalkUserId.keySet()), begin, end);
        if (approvals == null || approvals.isEmpty()) {
            return result;
        }
        for (tbattendanceapprove approval : approvals) {
            if (approval == null || !isApprovalIncludedInAdministrativeAttendanceExport(approval)) {
                continue;
            }
            Long employeeId = employeeIdByDingTalkUserId.get(trimToEmpty(approval.getUserId()));
            if (employeeId == null) {
                continue;
            }
            String type = resolveAdministrativeAttendanceApprovalType(approval);
            if (type == null) {
                continue;
            }
            BigDecimal hours = resolveAdministrativeAttendanceApprovalHours(approval);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LocalDate occurredDate = resolveAdministrativeApprovalDate(approval);
            if (occurredDate == null) {
                continue;
            }
            result.computeIfAbsent(employeeId, key -> new LinkedHashMap<>())
                    .computeIfAbsent(type, key -> new TreeSet<>())
                    .add(occurredDate);
        }
        return result;
    }

    private Map<Long, Map<String, Set<LocalDate>>> collectAdministrativeReportDates(YearMonth targetMonth,
                                                                                    List<Long> employeeIds) {
        Map<Long, Map<String, Set<LocalDate>>> result = new LinkedHashMap<>();
        Date begin = toDate(targetMonth.atDay(1).atStartOfDay());
        Date end = toDate(targetMonth.atEndOfMonth().atTime(LocalTime.MAX));
        List<AdministrativeAttendanceReportDetailVO> details = produceAttendanceMapper
                .queryAdministrativeAttendanceReportDetail(begin, end, employeeIds);
        if (details == null || details.isEmpty()) {
            return result;
        }
        for (AdministrativeAttendanceReportDetailVO detail : details) {
            if (detail == null || detail.getEmployeeId() == null
                    || detail.getWorkDate() == null || trimToEmpty(detail.getFieldName()).isEmpty()) {
                continue;
            }
            LocalDate occurredDate = toLocalDateTime(detail.getWorkDate()).toLocalDate();
            result.computeIfAbsent(detail.getEmployeeId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(trimToEmpty(detail.getFieldName()), key -> new TreeSet<>())
                    .add(occurredDate);
        }
        return result;
    }

    private LocalDate resolveAdministrativeApprovalDate(tbattendanceapprove approval) {
        if (approval.getWorkDate() != null) {
            return toLocalDateTime(approval.getWorkDate()).toLocalDate();
        }
        if (approval.getBeginTime() != null) {
            return toLocalDateTime(approval.getBeginTime()).toLocalDate();
        }
        return null;
    }

    private String joinAdministrativeCommentDates(Set<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return null;
        }
        return dates.stream()
                .map(date -> date.getMonthValue() + "月" + date.getDayOfMonth() + "日")
                .collect(Collectors.joining("、"));
    }

    private List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> buildRowsFromStatisticsDetails(
            List<HrmOvertimeNightStatisticsDetail> details,
            Map<Long, HrmEmployee> employeeMap,
            QueryMonthAttendanceBO queryMonthAttendanceBO,
            YearMonth targetMonth,
            Map<Long, LocalDate> quitDateByEmployeeId,
            LocalDate employmentCutoverDate) {
        Map<Long, AdministrativeAttendanceAccumulator> accumulators = new LinkedHashMap<>();
        for (HrmOvertimeNightStatisticsDetail detail : details) {
            if (detail == null || detail.getEmployeeId() == null) {
                continue;
            }
            HrmEmployee employee = employeeMap.get(detail.getEmployeeId());
            if (!shouldExportAdministrativeAttendanceEmployee(detail, employee, queryMonthAttendanceBO,
                    quitDateByEmployeeId, employmentCutoverDate)) {
                continue;
            }
            accumulators
                    .computeIfAbsent(detail.getEmployeeId(), employeeId -> new AdministrativeAttendanceAccumulator(employeeId, employee))
                    .accept(detail);
        }
        if (accumulators.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> deptNames = resolveAdministrativeAttendanceDeptNames(accumulators.values());
        List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> exportRows = accumulators.values()
                .stream()
                .map(accumulator -> accumulator.toRow(targetMonth, deptNames))
                .collect(Collectors.toList());
        sortAdministrativeAttendanceRows(exportRows);
        return exportRows;
    }

    private void appendMissingAdministrativeAttendanceRows(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> target,
            Set<Long> exportedEmployeeIds,
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> additional) {
        if (additional == null || additional.isEmpty()) {
            return;
        }
        for (AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row : additional) {
            if (row == null) {
                continue;
            }
            if (row.employeeId != null && !exportedEmployeeIds.add(row.employeeId)) {
                continue;
            }
            target.add(row);
        }
    }

    private static void sortAdministrativeAttendanceRows(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows) {
        rows.sort(Comparator
                .comparing((AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row) -> nullToEmpty(row.deptName))
                .thenComparing(row -> nullToEmpty(row.employeeName))
                .thenComparing(row -> row.employeeId, Comparator.nullsLast(Long::compareTo)));
    }

    private List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> buildRowsFromProduceAttendance(
            Map<Long, HrmEmployee> employeeMap,
            QueryMonthAttendanceBO queryMonthAttendanceBO,
            YearMonth targetMonth,
            Map<Long, LocalDate> quitDateByEmployeeId,
            LocalDate employmentCutoverDate) {
        LambdaQueryWrapper<HrmProduceAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrmProduceAttendance::getYear, targetMonth.getYear())
                .eq(HrmProduceAttendance::getMonth, targetMonth.getMonthValue())
                .eq(HrmProduceAttendance::getDepartment, DEPARTMENT_ADMINISTRATIVE);
        List<HrmProduceAttendance> attendanceList = produceAttendanceMapper.selectList(wrapper);
        if (attendanceList == null || attendanceList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> deptNames = resolveDeptNamesFromEmployees(attendanceList, employeeMap);
        List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows = new ArrayList<>();
        for (HrmProduceAttendance attendance : attendanceList) {
            if (attendance == null || attendance.getEmployeeId() == null) {
                continue;
            }
            HrmEmployee employee = employeeMap.get(attendance.getEmployeeId());
            if (employee != null) {
                if (Integer.valueOf(1).equals(employee.getIsDel())) {
                    continue;
                }
                if (!isEmployeeActiveForExport(employee, quitDateByEmployeeId.get(employee.getEmployeeId()), employmentCutoverDate)) {
                    continue;
                }
                if (!matchesAdministrativeAttendanceEmployeeFilter(employee, queryMonthAttendanceBO)) {
                    continue;
                }
                if (!matchesAdministrativeAttendanceDeptFilter(employee, queryMonthAttendanceBO)) {
                    continue;
                }
            } else {
                String keyword = queryMonthAttendanceBO != null ? trimToEmpty(queryMonthAttendanceBO.getEmployeeName()) : "";
                if (!keyword.isEmpty() && !keyword.equals(trimToEmpty(attendance.getEmployeeName()))) {
                    continue;
                }
                if (queryMonthAttendanceBO != null && queryMonthAttendanceBO.getDeptIds() != null
                        && !queryMonthAttendanceBO.getDeptIds().isEmpty()) {
                    continue;
                }
            }
            String deptName = deptNames.getOrDefault(attendance.getEmployeeId(), "");
            rows.add(new AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow(
                    attendance.getEmployeeId(),
                    attendance.getEmployeeName(),
                    deptName,
                    BigDecimal.valueOf(targetMonth.lengthOfMonth()),
                    BigDecimal.ZERO,
                    attendance.getPositiveAttendance() != null ? attendance.getPositiveAttendance() : BigDecimal.ZERO,
                    attendance.getProbationAttendance() != null ? attendance.getProbationAttendance() : BigDecimal.ZERO
            ));
        }
        return rows;
    }

    private Map<Long, String> resolveDeptNamesFromEmployees(
            List<HrmProduceAttendance> attendanceList,
            Map<Long, HrmEmployee> employeeMap) {
        Set<Long> deptIds = new LinkedHashSet<>();
        for (HrmProduceAttendance attendance : attendanceList) {
            if (attendance != null && attendance.getEmployeeId() != null) {
                HrmEmployee employee = employeeMap.get(attendance.getEmployeeId());
                if (employee != null && employee.getDeptId() != null) {
                    deptIds.add(employee.getDeptId());
                }
            }
        }
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrmDept> depts = deptRepository.findAllByDeptIdIn(new ArrayList<>(deptIds));
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> employeeToDept = new LinkedHashMap<>();
        for (HrmProduceAttendance attendance : attendanceList) {
            if (attendance != null && attendance.getEmployeeId() != null) {
                HrmEmployee employee = employeeMap.get(attendance.getEmployeeId());
                if (employee != null && employee.getDeptId() != null) {
                    employeeToDept.putIfAbsent(attendance.getEmployeeId(), employee.getDeptId());
                }
            }
        }
        Map<Long, String> deptNameById = depts.stream()
                .filter(dept -> dept != null && dept.getDeptId() != null)
                .collect(Collectors.toMap(HrmDept::getDeptId, dept -> nullToEmpty(dept.getName()), (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : employeeToDept.entrySet()) {
            result.put(entry.getKey(), deptNameById.getOrDefault(entry.getValue(), ""));
        }
        return result;
    }

    private List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> buildRowsFromEmployeeList(
            Map<Long, HrmEmployee> employeeMap,
            QueryMonthAttendanceBO queryMonthAttendanceBO,
            YearMonth targetMonth,
            Map<Long, LocalDate> quitDateByEmployeeId,
            LocalDate employmentCutoverDate) {
        LambdaQueryWrapper<HrmProduceAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrmProduceAttendance::getYear, targetMonth.getYear())
                .eq(HrmProduceAttendance::getMonth, targetMonth.getMonthValue());
        List<HrmProduceAttendance> allAttendance = produceAttendanceMapper.selectList(wrapper);
        Map<Long, HrmProduceAttendance> attendanceByEmployee = (allAttendance != null ? allAttendance : Collections.<HrmProduceAttendance>emptyList())
                .stream()
                .filter(a -> a != null && a.getEmployeeId() != null)
                .collect(Collectors.toMap(HrmProduceAttendance::getEmployeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<HrmEmployee> adminEmployees = employeeMap.values().stream()
                .filter(e -> e != null && e.getEmployeeId() != null)
                .filter(e -> !Integer.valueOf(1).equals(e.getIsDel()))
                .filter(e -> Integer.valueOf(DEPARTMENT_ADMINISTRATIVE).equals(resolveDepartmentType(e)))
                .filter(e -> isEmployeeActiveForExport(e, quitDateByEmployeeId.get(e.getEmployeeId()), employmentCutoverDate))
                .filter(e -> matchesAdministrativeAttendanceEmployeeFilter(e, queryMonthAttendanceBO))
                .filter(e -> matchesAdministrativeAttendanceDeptFilter(e, queryMonthAttendanceBO))
                .sorted(Comparator.comparing(HrmEmployee::getEmployeeId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());

        if (adminEmployees.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> deptIds = adminEmployees.stream()
                .filter(e -> e.getDeptId() != null)
                .map(HrmEmployee::getDeptId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> deptNameMap = resolveDeptNamesByIds(deptIds);

        List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows = new ArrayList<>();
        for (HrmEmployee employee : adminEmployees) {
            HrmProduceAttendance attendance = attendanceByEmployee.get(employee.getEmployeeId());
            String deptName = employee.getDeptId() != null ? deptNameMap.getOrDefault(employee.getDeptId(), "") : "";
            rows.add(new AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow(
                    employee.getEmployeeId(),
                    employee.getEmployeeName(),
                    deptName,
                    BigDecimal.valueOf(targetMonth.lengthOfMonth()),
                    BigDecimal.ZERO,
                    attendance != null && attendance.getPositiveAttendance() != null ? attendance.getPositiveAttendance() : BigDecimal.ZERO,
                    attendance != null && attendance.getProbationAttendance() != null ? attendance.getProbationAttendance() : BigDecimal.ZERO
            ));
        }
        return rows;
    }

    private Map<Long, String> resolveDeptNamesByIds(Set<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrmDept> depts = deptRepository.findAllByDeptIdIn(new ArrayList<>(deptIds));
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyMap();
        }
        return depts.stream()
                .filter(dept -> dept != null && dept.getDeptId() != null)
                .collect(Collectors.toMap(HrmDept::getDeptId, dept -> nullToEmpty(dept.getName()), (left, right) -> left, LinkedHashMap::new));
    }

    private boolean shouldExportAdministrativeAttendanceEmployee(HrmOvertimeNightStatisticsDetail detail,
                                                                 HrmEmployee employee,
                                                                 QueryMonthAttendanceBO queryMonthAttendanceBO,
                                                                 Map<Long, LocalDate> quitDateByEmployeeId,
                                                                 LocalDate employmentCutoverDate) {
        if (employee != null) {
            if (Integer.valueOf(1).equals(employee.getIsDel())) {
                return false;
            }
            if (!isEmployeeActiveForExport(employee, quitDateByEmployeeId.get(employee.getEmployeeId()), employmentCutoverDate)) {
                return false;
            }
            if (!Integer.valueOf(DEPARTMENT_ADMINISTRATIVE).equals(resolveDepartmentType(employee))) {
                return false;
            }
            if (!matchesAdministrativeAttendanceEmployeeFilter(employee, queryMonthAttendanceBO)) {
                return false;
            }
            return matchesAdministrativeAttendanceDeptFilter(employee, queryMonthAttendanceBO);
        }
        return matchesAdministrativeAttendanceFallbackNameFilter(detail, queryMonthAttendanceBO)
                && (queryMonthAttendanceBO == null
                || queryMonthAttendanceBO.getDeptIds() == null
                || queryMonthAttendanceBO.getDeptIds().isEmpty());
    }

    private boolean matchesAdministrativeAttendanceEmployeeFilter(HrmEmployee employee,
                                                                  QueryMonthAttendanceBO queryMonthAttendanceBO) {
        String keyword = queryMonthAttendanceBO != null ? trimToEmpty(queryMonthAttendanceBO.getEmployeeName()) : "";
        if (keyword.isEmpty()) {
            return true;
        }
        return keyword.equals(trimToEmpty(employee.getEmployeeName()))
                || keyword.equals(trimToEmpty(employee.getJobNumber()));
    }

    private boolean matchesAdministrativeAttendanceFallbackNameFilter(HrmOvertimeNightStatisticsDetail detail,
                                                                      QueryMonthAttendanceBO queryMonthAttendanceBO) {
        String keyword = queryMonthAttendanceBO != null ? trimToEmpty(queryMonthAttendanceBO.getEmployeeName()) : "";
        return keyword.isEmpty() || keyword.equals(trimToEmpty(detail.getEmployeeName()));
    }

    private boolean matchesAdministrativeAttendanceDeptFilter(HrmEmployee employee,
                                                              QueryMonthAttendanceBO queryMonthAttendanceBO) {
        if (queryMonthAttendanceBO == null
                || queryMonthAttendanceBO.getDeptIds() == null
                || queryMonthAttendanceBO.getDeptIds().isEmpty()) {
            return true;
        }
        return employee != null
                && employee.getDeptId() != null
                && queryMonthAttendanceBO.getDeptIds().contains(employee.getDeptId());
    }

    /**
     * 解析行政考勤导出的发薪日（计薪设置 payDay）。
     * 发薪日是"当月是否算在职员工"的分界日；未配置时提示先到系统设置-计薪设置维护发薪日期。
     */
    private int resolveAdministrativePayDay() {
        HrmSalaryConfig salaryConfig = salaryConfigService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(), false);
        if (salaryConfig == null || salaryConfig.getPayDay() == null || salaryConfig.getPayDay() <= 0) {
            throw new IllegalArgumentException("未配置发薪日期，请先在【系统设置-计薪设置】中维护发薪日期后再下载行政体系考勤");
        }
        return salaryConfig.getPayDay();
    }

    /**
     * 批量取已离职员工(entry_status=4)的离职日期(plan_quit_time)，多个离职记录取最近一条，只取到日。
     */
    private Map<Long, LocalDate> resolveAdministrativeQuitDateMap(Map<Long, HrmEmployee> employeeMap) {
        if (employeeMap == null || employeeMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> quitEmployeeIds = employeeMap.values().stream()
                .filter(e -> e != null && e.getEmployeeId() != null && Integer.valueOf(4).equals(e.getEntryStatus()))
                .map(HrmEmployee::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());
        if (quitEmployeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrmEmployeeQuitInfo> quitInfos = quitInfoRepository.findAllByEmployeeIdIn(quitEmployeeIds);
        if (quitInfos == null || quitInfos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, LocalDate> result = new LinkedHashMap<>();
        for (HrmEmployeeQuitInfo quitInfo : quitInfos) {
            if (quitInfo == null || quitInfo.getEmployeeId() == null || quitInfo.getPlanQuitTime() == null) {
                continue;
            }
            LocalDate quitDate = toLocalDateOnly(quitInfo.getPlanQuitTime());
            result.merge(quitInfo.getEmployeeId(), quitDate,
                    (oldDate, newDate) -> newDate.isAfter(oldDate) ? newDate : oldDate);
        }
        return result;
    }

    private LocalDate toLocalDateOnly(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 员工在目标月(M)是否算"在职应导出"：
     * - entry_status=2(待入职) 不导出；
     * - entry_status=4(已离职)：离职日期缺失一律不导出；离职日期在发薪日(cutoverDate，含当天)及之后才离职的，
     *   该员工到发薪日仍在职，M 月仍按在职导出（发薪日当天离职保留）；
     * - 其余(1 在职 / 3 待离职 / 未知) 视为在职导出。
     */
    private boolean isEmployeeActiveForExport(HrmEmployee employee,
                                              LocalDate quitDate,
                                              LocalDate employmentCutoverDate) {
        Integer entryStatus = employee.getEntryStatus();
        if (entryStatus == null) {
            return true;
        }
        if (Integer.valueOf(4).equals(entryStatus)) {
            if (quitDate == null) {
                return false;
            }
            return !quitDate.isBefore(employmentCutoverDate);
        }
        if (Integer.valueOf(2).equals(entryStatus)) {
            return false;
        }
        return true;
    }

    private Map<Long, String> resolveAdministrativeAttendanceDeptNames(
            Iterable<AdministrativeAttendanceAccumulator> accumulators) {
        Set<Long> deptIds = new LinkedHashSet<>();
        for (AdministrativeAttendanceAccumulator accumulator : accumulators) {
            if (accumulator != null && accumulator.deptId != null) {
                deptIds.add(accumulator.deptId);
            }
        }
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrmDept> depts = deptRepository.findAllByDeptIdIn(new ArrayList<>(deptIds));
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyMap();
        }
        return depts.stream()
                .filter(dept -> dept != null && dept.getDeptId() != null)
                .collect(Collectors.toMap(HrmDept::getDeptId, dept -> nullToEmpty(dept.getName()), (left, right) -> left, LinkedHashMap::new));
    }

    private void enrichAdministrativeAttendanceApprovalMetrics(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows,
            YearMonth targetMonth) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> employeeIds = rows.stream()
                .map(row -> row.employeeId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return;
        }
        List<tbattendanceuser> attendanceUsers = attendanceUserRepository.findAllByEmpIdIn(employeeIds);
        if (attendanceUsers == null || attendanceUsers.isEmpty()) {
            return;
        }
        Map<String, Long> employeeIdByDingTalkUserId = new LinkedHashMap<>();
        for (tbattendanceuser attendanceUser : attendanceUsers) {
            if (attendanceUser == null || attendanceUser.getEmpId() == null || trimToEmpty(attendanceUser.getUserId()).isEmpty()) {
                continue;
            }
            employeeIdByDingTalkUserId.putIfAbsent(attendanceUser.getUserId().trim(), attendanceUser.getEmpId());
        }
        if (employeeIdByDingTalkUserId.isEmpty()) {
            return;
        }
        Date begin = toDate(targetMonth.atDay(1).atStartOfDay());
        Date end = toDate(targetMonth.atEndOfMonth().atTime(LocalTime.MAX));
        List<tbattendanceapprove> approvals = attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(
                new ArrayList<>(employeeIdByDingTalkUserId.keySet()),
                begin,
                end
        );
        if (approvals == null || approvals.isEmpty()) {
            return;
        }
        Map<Long, AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rowByEmployee = rows.stream()
                .filter(row -> row.employeeId != null)
                .collect(Collectors.toMap(row -> row.employeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        for (tbattendanceapprove approval : approvals) {
            if (approval == null || !isApprovalIncludedInAdministrativeAttendanceExport(approval)) {
                continue;
            }
            Long employeeId = employeeIdByDingTalkUserId.get(trimToEmpty(approval.getUserId()));
            AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row = rowByEmployee.get(employeeId);
            if (row == null) {
                continue;
            }
            String type = resolveAdministrativeAttendanceApprovalType(approval);
            if (type == null) {
                continue;
            }
            BigDecimal hours = resolveAdministrativeAttendanceApprovalHours(approval);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            mergeAdministrativeAttendanceApprovalHours(row, type, hours);
        }
    }

    private void mergeAdministrativeAttendanceApprovalHours(
            AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row,
            String type,
            BigDecimal hours) {
        if ("事假".equals(type)) {
            row.personalLeaveHours = row.personalLeaveHours.add(hours).setScale(2, RoundingMode.HALF_UP);
        } else if ("病假".equals(type)) {
            row.sickLeaveHours = row.sickLeaveHours.add(hours).setScale(2, RoundingMode.HALF_UP);
        } else if ("调休".equals(type)) {
            row.compensatoryLeaveHours = row.compensatoryLeaveHours.add(hours).setScale(2, RoundingMode.HALF_UP);
        } else if ("年假".equals(type)) {
            row.annualLeaveHours = row.annualLeaveHours.add(hours).setScale(2, RoundingMode.HALF_UP);
        } else if ("出差".equals(type)) {
            row.travelDays = row.travelDays
                    .add(hours.divide(APPROVAL_HOURS_PER_DAY, 2, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
        } else if ("加班".equals(type)) {
            row.overtimeHours = row.overtimeHours.add(hours).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String resolveAdministrativeAttendanceApprovalType(tbattendanceapprove approval) {
        String tagName = trimToEmpty(approval.getTagName());
        String subType = trimToEmpty(approval.getSubType());
        String combined = tagName + " " + subType;
        if (Long.valueOf(1L).equals(approval.getBizType()) || combined.contains("加班")) {
            return "加班";
        }
        if (combined.contains("出差")) {
            return "出差";
        }
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

    private boolean isApprovalIncludedInAdministrativeAttendanceExport(tbattendanceapprove approval) {
        return approval != null && !STATISTICS_STATUS_CANCELED.equals(trimToEmpty(approval.getStatisticsStatus()));
    }

    private BigDecimal resolveAdministrativeAttendanceApprovalHours(tbattendanceapprove approval) {
        if (approval == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String rawDuration = trimToEmpty(approval.getDuration());
        if (rawDuration.isEmpty()) {
            return resolveAdministrativeAttendanceApprovalHoursFromTimeRange(approval);
        }
        String normalizedDuration = rawDuration
                .replace("小时", "")
                .replace("分钟", "")
                .replace("天", "")
                .trim();
        if (normalizedDuration.isEmpty()) {
            return resolveAdministrativeAttendanceApprovalHoursFromTimeRange(approval);
        }
        BigDecimal durationValue;
        try {
            durationValue = new BigDecimal(normalizedDuration);
        } catch (NumberFormatException ex) {
            return resolveAdministrativeAttendanceApprovalHoursFromTimeRange(approval);
        }
        String unitText = trimToEmpty(approval.getDurationUnit()) + rawDuration;
        BigDecimal hours;
        if (unitText.contains("分钟")) {
            hours = durationValue.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        } else if (unitText.contains("天")) {
            hours = resolveRoundedAdministrativeAttendanceDayHours(approval, durationValue);
        } else {
            hours = durationValue;
        }
        return hours.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveRoundedAdministrativeAttendanceDayHours(tbattendanceapprove approval, BigDecimal durationDays) {
        BigDecimal dayHours = durationDays.multiply(APPROVAL_HOURS_PER_DAY).setScale(2, RoundingMode.HALF_UP);
        if (durationDays.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return dayHours;
        }
        BigDecimal timeRangeHours = resolveAdministrativeAttendanceApprovalHoursFromTimeRange(approval);
        if (timeRangeHours.compareTo(BigDecimal.ZERO) <= 0) {
            return dayHours;
        }
        BigDecimal difference = dayHours.subtract(timeRangeHours).abs();
        return difference.compareTo(FRACTIONAL_DAY_RANGE_TOLERANCE_HOURS) <= 0
                ? timeRangeHours
                : dayHours;
    }

    private BigDecimal resolveAdministrativeAttendanceApprovalHoursFromTimeRange(tbattendanceapprove approval) {
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

    private void refreshAdministrativeAttendanceActualHours(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row : rows) {
            if (row == null
                    || row.expectedAttendanceHours == null
                    || row.expectedAttendanceHours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal deductionHours = defaultDecimal(row.personalLeaveHours)
                    .add(defaultDecimal(row.sickLeaveHours))
                    .add(defaultDecimal(row.compensatoryLeaveHours))
                    .add(defaultDecimal(row.annualLeaveHours));
            BigDecimal actualHours = row.expectedAttendanceHours
                    .add(defaultDecimal(row.overtimeHours))
                    .subtract(deductionHours);
            if (actualHours.compareTo(BigDecimal.ZERO) < 0) {
                actualHours = BigDecimal.ZERO;
            }
            row.actualAttendanceHours = actualHours.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private void refreshAdministrativeAttendanceHoursFromStatisticsRows(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows,
            YearMonth targetMonth) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, QueryOvertimeNightStatisticsPageVO> statisticsByEmployee = queryOvertimeNightStatisticsRows(targetMonth)
                .stream()
                .filter(row -> row != null && row.getEmployeeId() != null)
                .collect(Collectors.toMap(
                        QueryOvertimeNightStatisticsPageVO::getEmployeeId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (statisticsByEmployee.isEmpty()) {
            return;
        }
        for (AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row : rows) {
            if (row == null || row.employeeId == null) {
                continue;
            }
            QueryOvertimeNightStatisticsPageVO statistics = statisticsByEmployee.get(row.employeeId);
            if (statistics == null) {
                continue;
            }
            if (statistics.getExpectedAttendanceHours() != null) {
                row.expectedAttendanceHours = statistics.getExpectedAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            } else if (statistics.getExpectedAttendanceDays() != null) {
                row.expectedAttendanceHours = BigDecimal.valueOf(statistics.getExpectedAttendanceDays())
                        .multiply(APPROVAL_HOURS_PER_DAY)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (statistics.getActualAttendanceHours() != null) {
                row.actualAttendanceHours = statistics.getActualAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
            if (statistics.getAccruedAttendanceHours() != null) {
                row.accruedAttendanceHours = statistics.getAccruedAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
        }
    }

    private void enrichAdministrativeAttendanceReportMetrics(
            List<AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rows,
            YearMonth targetMonth) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> employeeIds = rows.stream()
                .map(row -> row.employeeId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return;
        }
        Date begin = toDate(targetMonth.atDay(1).atStartOfDay());
        Date end = toDate(targetMonth.atEndOfMonth().atTime(LocalTime.MAX));
        List<AdministrativeAttendanceReportMetricVO> metrics = produceAttendanceMapper
                .queryAdministrativeAttendanceReportMetrics(begin, end, employeeIds);
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        Map<Long, AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow> rowByEmployee = rows.stream()
                .filter(row -> row.employeeId != null)
                .collect(Collectors.toMap(row -> row.employeeId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        for (AdministrativeAttendanceReportMetricVO metric : metrics) {
            if (metric == null || metric.getEmployeeId() == null) {
                continue;
            }
            AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow row = rowByEmployee.get(metric.getEmployeeId());
            if (row == null) {
                continue;
            }
            row.absenteeismDays = defaultDecimal(metric.getAbsenteeismDays());
            row.lateCount = defaultDecimal(metric.getLateCount());
            row.earlyCount = defaultDecimal(metric.getEarlyCount());
            row.onDutyMissingCardCount = defaultDecimal(metric.getOnDutyMissingCardCount());
            row.offDutyMissingCardCount = defaultDecimal(metric.getOffDutyMissingCardCount());
            row.totalMissingCardCount = row.onDutyMissingCardCount.add(row.offDutyMissingCardCount).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String resolveCompanyName() {
        LoginUserInfo loginUserInfo = CompanyContext.get();
        if (loginUserInfo == null) {
            return "";
        }
        if (!trimToEmpty(loginUserInfo.getCompanyName()).isEmpty()) {
            return loginUserInfo.getCompanyName().trim();
        }
        return trimToEmpty(loginUserInfo.getDepName());
    }

    private void writeAdministrativeAttendanceResponse(HttpServletResponse response,
                                                       YearMonth targetMonth,
                                                       byte[] bytes) throws Exception {
        String fileName = "行政体系考勤_" + targetMonth.format(MONTH_FORMATTER) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\";filename*=UTF-8''" + encodedFileName);
        response.setHeader("Set-Cookie", "fileDownload=true; path=/");
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduceAttendanceCell(UpdateProduceAttendanceCellBO updateProduceAttendanceCellBO) {
        if (updateProduceAttendanceCellBO == null || updateProduceAttendanceCellBO.getSummaryId() == null) {
            throw new IllegalArgumentException("上传考勤ID不能为空");
        }
        String field = updateProduceAttendanceCellBO.getField();
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("保存字段不能为空");
        }
        HrmProduceAttendance attendance = new HrmProduceAttendance();
        attendance.setSummaryId(updateProduceAttendanceCellBO.getSummaryId());
        String value = updateProduceAttendanceCellBO.getValue();
        if (isOvertimeNightField(field)) {
            validateCanEditOvertimeNightField(updateProduceAttendanceCellBO.getSummaryId());
        }
        switch (field) {
            case "actualAttendance":
            case "positiveAttendance":
                attendance.setPositiveAttendance(parseDecimal(value));
                break;
            case "accruedAttendance":
            case "probationAttendance":
                attendance.setProbationAttendance(parseDecimal(value));
                break;
            case "workOverTime":
                attendance.setWorkOverTime(parseDecimal(value));
                break;
            case "overtimePay":
                attendance.setOvertimePay(parseDecimal(value));
                break;
            case "nightShift":
                attendance.setNightShift(parseInteger(value));
                break;
            case "nightSubsidy":
                attendance.setNightSubsidy(parseDecimal(value));
                break;
            case "loan":
                attendance.setLoan(parseDecimal(value));
                break;
            case "otherSubsidies":
                attendance.setOtherSubsidies(parseDecimal(value));
                break;
            case "otherDeductions":
                attendance.setOtherDeductions(parseDecimal(value));
                break;
            case "remark":
                attendance.setRemark(value != null ? value : "");
                break;
            default:
                throw new IllegalArgumentException("不支持保存该字段：" + field);
        }
        produceAttendanceMapper.updateById(attendance);
    }

    private void validateCanEditOvertimeNightField(Long summaryId) {
        HrmProduceAttendance existingAttendance = produceAttendanceMapper.selectById(summaryId);
        if (existingAttendance == null || existingAttendance.getEmployeeId() == null) {
            throw new IllegalArgumentException("上传考勤记录不存在，不能保存加班/夜班字段");
        }
        HrmEmployee employee = employeeRepository.findById(existingAttendance.getEmployeeId()).orElse(null);
        if (!canCountOvertimeNight(employee)) {
            throw new IllegalArgumentException("只有生产体系且固定月休4天员工才能统计加班/夜班字段");
        }
    }

    private boolean isOvertimeNightField(String field) {
        return "workOverTime".equals(field)
                || "overtimePay".equals(field)
                || "nightShift".equals(field)
                || "nightSubsidy".equals(field);
    }

    private YearMonth resolveSyncMonth(SyncProduceAttendanceBO syncProduceAttendanceBO) {
        if (syncProduceAttendanceBO == null) {
            throw new IllegalArgumentException("同步月份不能为空");
        }
        if (syncProduceAttendanceBO.getMonth() != null && !syncProduceAttendanceBO.getMonth().trim().isEmpty()) {
            return YearMonth.parse(syncProduceAttendanceBO.getMonth().trim(), MONTH_FORMATTER);
        }
        if (syncProduceAttendanceBO.getYear() != null && syncProduceAttendanceBO.getMonthNumber() != null) {
            return YearMonth.of(syncProduceAttendanceBO.getYear(), syncProduceAttendanceBO.getMonthNumber());
        }
        throw new IllegalArgumentException("同步月份不能为空");
    }

    private BigDecimal resolveAttendanceDays(BigDecimal hours, Integer days) {
        if (hours != null) {
            return hours.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
        }
        if (days != null) {
            return BigDecimal.valueOf(days).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveActualAttendanceDaysForSync(QueryOvertimeNightStatisticsPageVO statistics,
                                                          QueryOvertimeNightStatisticsPageVO persistedStatistics) {
        BigDecimal hours = persistedStatistics != null && persistedStatistics.getActualAttendanceHours() != null
                ? persistedStatistics.getActualAttendanceHours()
                : statistics.getActualAttendanceHours();
        Integer days = persistedStatistics != null && persistedStatistics.getActualAttendanceDays() != null
                ? persistedStatistics.getActualAttendanceDays()
                : statistics.getActualAttendanceDays();
        return resolveAttendanceDays(hours, days);
    }

    private BigDecimal resolveAccruedAttendanceDaysForSync(QueryOvertimeNightStatisticsPageVO statistics,
                                                           QueryOvertimeNightStatisticsPageVO persistedStatistics) {
        BigDecimal hours = persistedStatistics != null && persistedStatistics.getAccruedAttendanceHours() != null
                ? persistedStatistics.getAccruedAttendanceHours()
                : statistics.getAccruedAttendanceHours();
        return resolveAttendanceDays(hours, null);
    }

    private BigDecimal resolveAttendanceOvertimeHours(QueryOvertimeNightStatisticsPageVO statistics) {
        if (statistics == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (statistics.getAttendanceOvertimeHours() != null) {
            return defaultDecimal(statistics.getAttendanceOvertimeHours());
        }
        return defaultDecimal(statistics.getOvertimeHours());
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private HrmSalaryBasic resolveLatestSalaryBasic() {
        List<HrmSalaryBasic> salaryBasics = salaryBasicMapper.selectList(new LambdaQueryWrapper<HrmSalaryBasic>()
                .orderByDesc(HrmSalaryBasic::getCreateTime)
                .last("limit 1"));
        if (salaryBasics == null || salaryBasics.isEmpty()) {
            return null;
        }
        return salaryBasics.get(0);
    }

    private BigDecimal calculateOvertimePay(BigDecimal workOverTime, HrmSalaryBasic salaryBasic) {
        BigDecimal unitPrice = salaryBasic != null && salaryBasic.getOvertimePay() != null
                ? salaryBasic.getOvertimePay()
                : DEFAULT_OVERTIME_UNIT_PRICE;
        return defaultDecimal(workOverTime).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateNightSubsidy(Integer nightShift, HrmSalaryBasic salaryBasic) {
        BigDecimal unitPrice = salaryBasic != null && salaryBasic.getSubsidy() != null
                ? salaryBasic.getSubsidy()
                : DEFAULT_NIGHT_SUBSIDY_UNIT_PRICE;
        BigDecimal count = nightShift != null ? BigDecimal.valueOf(nightShift) : BigDecimal.ZERO;
        return count.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date value) {
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private Integer resolveDepartmentType(HrmEmployee employee) {
        if (employee != null && (Integer.valueOf(DEPARTMENT_ADMINISTRATIVE).equals(employee.getAffiliationSystem())
                || Integer.valueOf(DEPARTMENT_PRODUCTION).equals(employee.getAffiliationSystem()))) {
            return employee.getAffiliationSystem();
        }
        return DEPARTMENT_ADMINISTRATIVE;
    }

    static void applyOvertimeNightEligibility(HrmProduceAttendance attendance, HrmEmployee employee) {
        if (attendance == null || canCountOvertimeNight(employee)) {
            return;
        }
        attendance.setWorkOverTime(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        attendance.setOvertimePay(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        attendance.setNightShift(0);
        attendance.setNightSubsidy(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private static boolean canCountOvertimeNight(HrmEmployee employee) {
        return employee != null
                && Integer.valueOf(DEPARTMENT_PRODUCTION).equals(employee.getAffiliationSystem())
                && Integer.valueOf(REST_TYPE_FIXED_MONTHLY_REST).equals(employee.getRestType());
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class AdministrativeAttendanceAccumulator {
        private final Long employeeId;
        private String employeeName;
        private Long deptId;
        private String detailDeptName;
        private Integer expectedAttendanceDays;
        private BigDecimal expectedAttendanceHours;
        private Integer actualAttendanceDays;
        private BigDecimal actualAttendanceHours;
        private BigDecimal accruedAttendanceHours;

        private AdministrativeAttendanceAccumulator(Long employeeId, HrmEmployee employee) {
            this.employeeId = employeeId;
            if (employee != null) {
                employeeName = employee.getEmployeeName();
                deptId = employee.getDeptId();
            }
        }

        private void accept(HrmOvertimeNightStatisticsDetail detail) {
            if (employeeName == null) {
                employeeName = detail.getEmployeeName();
            }
            if (deptId == null) {
                deptId = detail.getDeptId();
            }
            if (detailDeptName == null) {
                detailDeptName = detail.getDeptName();
            }
            if (detail.getExpectedAttendanceDays() != null) {
                expectedAttendanceDays = expectedAttendanceDays == null
                        ? detail.getExpectedAttendanceDays()
                        : Math.max(expectedAttendanceDays, detail.getExpectedAttendanceDays());
            }
            if (expectedAttendanceHours == null && detail.getExpectedAttendanceHours() != null) {
                expectedAttendanceHours = detail.getExpectedAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
            if (detail.getActualAttendanceDays() != null) {
                actualAttendanceDays = actualAttendanceDays == null
                        ? detail.getActualAttendanceDays()
                        : Math.max(actualAttendanceDays, detail.getActualAttendanceDays());
            }
            if (actualAttendanceHours == null && detail.getActualAttendanceHours() != null) {
                actualAttendanceHours = detail.getActualAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
            if (accruedAttendanceHours == null && detail.getAccruedAttendanceHours() != null) {
                accruedAttendanceHours = detail.getAccruedAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
        }

        private AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow toRow(
                YearMonth targetMonth,
                Map<Long, String> deptNames) {
            String deptName = deptId != null && deptNames != null && deptNames.containsKey(deptId)
                    ? deptNames.get(deptId)
                    : detailDeptName;
            return new AdministrativeAttendanceExportSupport.AdministrativeAttendanceExportRow(
                    employeeId,
                    employeeName,
                    deptName,
                    BigDecimal.valueOf(targetMonth.lengthOfMonth()),
                    expectedAttendanceHours != null
                            ? expectedAttendanceHours
                            : BigDecimal.valueOf(expectedAttendanceDays != null ? expectedAttendanceDays : 0)
                                    .multiply(APPROVAL_HOURS_PER_DAY)
                                    .setScale(2, RoundingMode.HALF_UP),
                    actualAttendanceHours != null
                            ? actualAttendanceHours
                            : BigDecimal.valueOf(actualAttendanceDays != null ? actualAttendanceDays : 0)
                                    .multiply(APPROVAL_HOURS_PER_DAY)
                                    .setScale(2, RoundingMode.HALF_UP),
                    defaultDecimal(accruedAttendanceHours)
            );
        }
    }

    private static class PersistedStatisticsAccumulator {
        private final Long employeeId;
        private String employeeName;
        private String deptName;
        private Integer actualAttendanceDays;
        private BigDecimal actualAttendanceHours;
        private BigDecimal accruedAttendanceHours;
        private BigDecimal overtimeHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private int nightShiftCount;
        private String month;

        private PersistedStatisticsAccumulator(Long employeeId) {
            this.employeeId = employeeId;
        }

        private void accept(HrmOvertimeNightStatisticsDetail detail, YearMonth targetMonth) {
            if (employeeName == null) {
                employeeName = detail.getEmployeeName();
            }
            if (deptName == null) {
                deptName = detail.getDeptName();
            }
            month = targetMonth.format(MONTH_FORMATTER);
            if (detail.getActualAttendanceDays() != null) {
                actualAttendanceDays = actualAttendanceDays == null
                        ? detail.getActualAttendanceDays()
                        : Math.max(actualAttendanceDays, detail.getActualAttendanceDays());
            }
            if (actualAttendanceHours == null && detail.getActualAttendanceHours() != null) {
                actualAttendanceHours = detail.getActualAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
            if (accruedAttendanceHours == null && detail.getAccruedAttendanceHours() != null) {
                accruedAttendanceHours = detail.getAccruedAttendanceHours().setScale(2, RoundingMode.HALF_UP);
            }
            if (detail.getOvertimeHours() != null) {
                overtimeHours = overtimeHours.add(detail.getOvertimeHours()).setScale(2, RoundingMode.HALF_UP);
            }
            if (detail.getNightShiftCount() != null) {
                nightShiftCount += detail.getNightShiftCount();
            }
        }

        private QueryOvertimeNightStatisticsPageVO toRow() {
            QueryOvertimeNightStatisticsPageVO row = new QueryOvertimeNightStatisticsPageVO();
            row.setEmployeeId(employeeId);
            row.setEmployeeName(employeeName);
            row.setDeptName(deptName);
            row.setMonth(month);
            row.setActualAttendanceDays(actualAttendanceDays);
            if (actualAttendanceHours != null) {
                row.setActualAttendanceHours(actualAttendanceHours);
            } else if (actualAttendanceDays != null) {
                row.setActualAttendanceHours(BigDecimal.valueOf(actualAttendanceDays)
                        .multiply(BigDecimal.valueOf(8))
                        .setScale(2, RoundingMode.HALF_UP));
            }
            row.setAccruedAttendanceHours(accruedAttendanceHours);
            row.setOvertimeHours(overtimeHours);
            row.setNightShiftCount(nightShiftCount);
            return row;
        }
    }
}
