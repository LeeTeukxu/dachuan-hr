package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryEmployeeOvertimeNightDetailBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightDailyDetailPageBO;
import com.tianye.hrsystem.entity.bo.QueryOvertimeNightStatisticsPageBO;
import com.tianye.hrsystem.entity.bo.UpdateOvertimeNightAttendanceBO;
import com.tianye.hrsystem.entity.vo.EmployeeOvertimeNightMonthlyDetailVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightDailyDetailPageVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;
import com.tianye.hrsystem.model.HrmAttendanceClock;
import com.tianye.hrsystem.model.HrmAttendanceDateShift;
import com.tianye.hrsystem.model.HrmAttendanceGroup;
import com.tianye.hrsystem.model.HrmAttendanceHistoryShift;
import com.tianye.hrsystem.model.HrmAttendancePlan;
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
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryBasicMapper;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.workweek.service.HrmWorkweekSettingService;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekDayCalendarVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekMonthCalendarVO;
import com.tianye.hrsystem.repository.hrmAttendanceClockRepository;
import com.tianye.hrsystem.repository.hrmAttendanceDateShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.hrmAttendanceHistoryShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmEmployeeOverTimeRecordRepository;
import com.tianye.hrsystem.repository.hrmOvertimeNightStatisticsDetailRepository;
import com.tianye.hrsystem.repository.hrmWorkPlanCustomShiftRepository;
import com.tianye.hrsystem.repository.tbattendancedetailRepository;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmOvertimeNightStatisticsServiceImplTest {

    @InjectMocks
    private HrmOvertimeNightStatisticsServiceImpl service;

    @Mock
    private hrmEmployeeRepository employeeRepository;

    @Mock
    private hrmDeptRepository deptRepository;

    @Mock
    private hrmAttendancePlanRepository planRepository;

    @Mock
    private hrmAttendanceShiftRepository shiftRepository;

    @Mock
    private hrmAttendanceGroupRepository attendanceGroupRepository;

    @Mock
    private hrmAttendanceHistoryShiftRepository attendanceHistoryShiftRepository;

    @Mock
    private hrmAttendanceDateShiftRepository attendanceDateShiftRepository;

    @Mock
    private hrmAttendanceClockRepository clockRepository;

    @Mock
    private tbattendancedetailRepository attendanceDetailRepository;

    @Mock
    private hrmAttendanceReportDataRepository attendanceReportDataRepository;

    @Mock
    private hrmEmployeeOverTimeRecordRepository employeeOverTimeRecordRepository;

    @Mock
    private hrmOvertimeNightStatisticsDetailRepository detailRepository;

    @Mock
    private tbattendanceapproveRepository attendanceApproveRepository;

    @Mock
    private tbattendanceuserRepository attendanceUserRepository;

    @Mock
    private tbPlanListRepository localPlanRepository;

    @Mock
    private hrmWorkPlanCustomShiftRepository customShiftRepository;

    @Mock
    private HrmWorkweekSettingService workweekSettingService;

    @Mock
    private HrmSalaryBasicMapper salaryBasicMapper;

    @Test
    public void queryPageList_shouldAggregateFromPersistedDetails() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setJobNumber("E001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        employee.setRestType(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("人事部");

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenReturn(Arrays.asList(
                        buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 1), "2.50", 0),
                        buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 2), "1.25", 1)
                ));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(new BigDecimal("3.75"), row.getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(1), row.getNightShiftCount());
        verify(detailRepository).findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList());
        verify(employeeRepository).findAll();
    }

    @Test
    public void queryPageList_shouldZeroOvertimeAndNightShiftForProductionEmployeeWithoutFixedRest() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setJobNumber("E001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);
        employee.setAffiliationSystem(2);
        employee.setRestType(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        HrmOvertimeNightStatisticsDetail detail = buildDetail(
                1001L, "张三", "E001", 10L, "生产部",
                LocalDate.of(2026, 3, 1), "8.00", 2);
        detail.setExpectedAttendanceDays(25);
        detail.setActualAttendanceDays(25);
        detail.setAccruedAttendanceHours(new BigDecimal("200.00"));

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenReturn(Collections.singletonList(detail));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(0), page.getList().get(0).getNightShiftCount());
    }

    @Test
    public void queryPageList_shouldFillAdministrativeExpectedAttendanceDaysWhenPersistedRowsAreMissingAttendanceDays() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("潘红琼");
        employee.setJobNumber("E009");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenReturn(Collections.singletonList(
                        buildDetail(1001L, "潘红琼", "E009", 10L, "行政部", LocalDate.of(2026, 3, 1), "0.00", 0)
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(23), page.getList().get(0).getExpectedAttendanceDays());
    }

    @Test
    public void queryPageList_shouldReadAdministrativeExpectedAttendanceFromRequestedMonthWorkweekSummary() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("潘红琼");
        employee.setJobNumber("E009");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(24);
        AtomicReference<QueryWorkweekMonthCalendarBO> capturedQuery = new AtomicReference<>();

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(5), anyList()))
                .thenReturn(Collections.singletonList(
                        buildDetail(1001L, "潘红琼", "E009", 10L, "行政部", LocalDate.of(2026, 5, 1), "0.00", 0)
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenAnswer(invocation -> {
            capturedQuery.set(invocation.getArgument(0));
            return workweekCalendar;
        });

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(24), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertNotNull(capturedQuery.get());
        Assert.assertEquals(Integer.valueOf(2026), capturedQuery.get().getYear());
        Assert.assertEquals(Integer.valueOf(5), capturedQuery.get().getMonth());
    }

    @Test
    public void queryPageList_shouldRecalculateActualAttendanceFromExpectedAttendanceWhenPersistedRowsAreStale() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        LocalDate shortWorkDate = LocalDate.of(2026, 3, 25);
        HrmOvertimeNightStatisticsDetail persistedDetail = buildDetail(
                1001L, "李凤皇", "E088", 10L, "行政部", shortWorkDate, "0.00", 0
        );
        persistedDetail.setActualAttendanceDays(25);

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(25);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", shortWorkDate, "4.00", "小时")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("历史明细实际出勤旧值为25时，查询应按应出勤25天扣调休4小时修正为24天",
                Integer.valueOf(24), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("196.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("调休4.00小时"));
    }

    @Test
    public void queryPageList_shouldNotAddMonthlyAutoOvertimeToActualAttendanceForProductionEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政人力资源部");

        LocalDate workDate = LocalDate.of(2026, 6, 17);
        HrmOvertimeNightStatisticsDetail persistedDetail = buildDetail(
                1001L, "李凤皇", "E088", 10L, "行政人力资源部", workDate, "93.48", 0
        );
        persistedDetail.setExpectedAttendanceDays(25);
        persistedDetail.setActualAttendanceDays(36);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", workDate, "4.00", "小时")
                ));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(new BigDecimal("93.48"), row.getOvertimeHours());
        Assert.assertEquals(new BigDecimal("196.00"), row.getActualAttendanceHours());
        Assert.assertEquals(new BigDecimal("200.00"), row.getAccruedAttendanceHours());
        Assert.assertTrue(row.getActualAttendanceRemark().contains("调休4.00小时"));
    }

    @Test
    public void queryPageList_shouldUseFixedMonthlyRestAttendanceWhenRestTypeIsFixedEvenIfAffiliationIsAdministrative() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1831601326890434571L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("TYNG-142");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);
        employee.setRestType(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政人力资源部");

        LocalDate workDate = LocalDate.of(2026, 6, 17);
        HrmOvertimeNightStatisticsDetail persistedDetail = buildDetail(
                employee.getEmployeeId(), "李凤皇", "TYNG-142", 10L, "行政人力资源部", workDate, "93.48", 0
        );
        persistedDetail.setExpectedAttendanceDays(23);
        persistedDetail.setActualAttendanceDays(22);
        persistedDetail.setAccruedAttendanceHours(new BigDecimal("184.00"));

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);
        workweekCalendar.setDays(Arrays.asList(
                buildWorkweekDay("2026-06-19", 5, "legal_rest"),
                buildWorkweekDay("2026-06-20", 6, "legal_rest"),
                buildWorkweekDay("2026-06-21", 7, "legal_rest")
        ));

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);
        when(salaryBasicMapper.selectList(any())).thenReturn(Collections.singletonList(
                new HrmSalaryBasic().setProductionMonthlyRestDays(4)
        ));
        when(attendanceUserRepository.findFirstByEmpId(employee.getEmployeeId()))
                .thenReturn(Optional.of(buildAttendanceUser(employee.getEmployeeId(), "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", workDate, "4.00", "小时")
                ));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(Integer.valueOf(25), row.getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("0.00"), row.getOvertimeHours());
        Assert.assertEquals(new BigDecimal("0.00"), row.getAttendanceOvertimeHours());
        Assert.assertEquals(new BigDecimal("196.00"), row.getActualAttendanceHours());
        Assert.assertEquals(new BigDecimal("200.00"), row.getAccruedAttendanceHours());
        Assert.assertTrue(row.getActualAttendanceRemark().contains("调休4.00小时"));
    }

    @Test
    public void updateAttendanceSummary_shouldPersistManualHoursToEveryDetailInEmployeeMonth() {
        HrmOvertimeNightStatisticsDetail firstDetail = buildDetail(
                1001L, "张三", "E001", 10L, "行政部", LocalDate.of(2026, 6, 1), "0.00", 0);
        firstDetail.setDetailId(900001L);
        firstDetail.setExpectedAttendanceDays(23);
        firstDetail.setExpectedAttendanceHours(new BigDecimal("184.00"));
        firstDetail.setActualAttendanceDays(20);
        firstDetail.setActualAttendanceHours(new BigDecimal("160.00"));
        firstDetail.setAccruedAttendanceHours(new BigDecimal("184.00"));
        HrmOvertimeNightStatisticsDetail secondDetail = buildDetail(
                1001L, "张三", "E001", 10L, "行政部", LocalDate.of(2026, 6, 2), "1.00", 0);
        secondDetail.setDetailId(900002L);
        secondDetail.setExpectedAttendanceDays(23);
        secondDetail.setExpectedAttendanceHours(new BigDecimal("184.00"));
        secondDetail.setActualAttendanceDays(20);
        secondDetail.setActualAttendanceHours(new BigDecimal("160.00"));
        secondDetail.setAccruedAttendanceHours(new BigDecimal("184.00"));

        when(detailRepository.findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(1001L, 2026, 6))
                .thenReturn(Arrays.asList(firstDetail, secondDetail));

        UpdateOvertimeNightAttendanceBO updateBO = new UpdateOvertimeNightAttendanceBO();
        updateBO.setEmployeeId(1001L);
        updateBO.setMonth("2026-06");
        updateBO.setExpectedAttendanceHours(new BigDecimal("200.00"));
        updateBO.setActualAttendanceHours(new BigDecimal("196.50"));
        updateBO.setAccruedAttendanceHours(new BigDecimal("200.00"));

        service.updateAttendanceSummary(updateBO);

        Assert.assertTrue(Arrays.asList(firstDetail, secondDetail).stream().allMatch(detail ->
                Integer.valueOf(25).equals(detail.getExpectedAttendanceDays())
                        && new BigDecimal("200.00").equals(detail.getExpectedAttendanceHours())
                        && Integer.valueOf(24).equals(detail.getActualAttendanceDays())
                        && new BigDecimal("196.50").equals(detail.getActualAttendanceHours())
                        && new BigDecimal("200.00").equals(detail.getAccruedAttendanceHours())
                        && Integer.valueOf(1).equals(detail.getAttendanceManualAdjusted())
                        && detail.getUpdateTime() != null
        ));
        verify(detailRepository).saveAll(Arrays.asList(firstDetail, secondDetail));
        verify(detailRepository, atLeastOnce()).flush();
    }

    @Test
    public void queryPageList_shouldPreferManualAttendanceSummaryOverRefreshRules() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1831601326890434571L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("TYNG-142");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);
        employee.setRestType(2);

        HrmOvertimeNightStatisticsDetail persistedDetail = buildDetail(
                employee.getEmployeeId(), "李凤皇", "TYNG-142", 10L, "行政人力资源部",
                LocalDate.of(2026, 6, 17), "0.00", 0);
        persistedDetail.setExpectedAttendanceDays(24);
        persistedDetail.setExpectedAttendanceHours(new BigDecimal("192.00"));
        persistedDetail.setActualAttendanceDays(22);
        persistedDetail.setActualAttendanceHours(new BigDecimal("180.50"));
        persistedDetail.setAccruedAttendanceHours(new BigDecimal("188.00"));
        persistedDetail.setAttendanceManualAdjusted(1);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(Integer.valueOf(24), row.getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("192.00"), row.getExpectedAttendanceHours());
        Assert.assertEquals(Integer.valueOf(22), row.getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("180.50"), row.getActualAttendanceHours());
        Assert.assertEquals(new BigDecimal("188.00"), row.getAccruedAttendanceHours());
    }

    @Test
    public void queryEmployeeMonthlyDetail_shouldBuildMonthGroupsFromPersistedDetails() {
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Arrays.asList(
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 2), "1.25", 1),
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 1), "2.50", 0),
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 2, 28), "0.50", 0)
        ));

        QueryEmployeeOvertimeNightDetailBO queryBO = new QueryEmployeeOvertimeNightDetailBO();
        queryBO.setEmployeeId(1001L);

        List<EmployeeOvertimeNightMonthlyDetailVO> rows = service.queryEmployeeMonthlyDetail(queryBO);

        Assert.assertEquals(2, rows.size());
        Assert.assertEquals("2026-03", rows.get(0).getMonth());
        Assert.assertEquals(new BigDecimal("3.75"), rows.get(0).getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(1), rows.get(0).getNightShiftCount());
        Assert.assertEquals(2, rows.get(0).getDailyDetails().size());
        Assert.assertEquals("2026-02", rows.get(1).getMonth());
        verify(detailRepository).findAllByEmployeeIdOrderByWorkDateDesc(1001L);
        verify(employeeRepository, never()).findAll();
    }

    @Test
    public void queryEmployeeMonthlyDetail_shouldFillAdministrativeExpectedAttendanceDaysWhenPersistedRowsAreMissingAttendanceDays() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("潘红琼");
        employee.setJobNumber("E009");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(detailRepository.findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(1001L, 2026, 3)).thenReturn(Arrays.asList(
                buildDetail(1001L, "潘红琼", "E009", 10L, "行政部", LocalDate.of(2026, 3, 2), "0.00", 0),
                buildDetail(1001L, "潘红琼", "E009", 10L, "行政部", LocalDate.of(2026, 3, 1), "0.00", 0)
        ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryEmployeeOvertimeNightDetailBO queryBO = new QueryEmployeeOvertimeNightDetailBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> rows = service.queryEmployeeMonthlyDetail(queryBO);

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("2026-03", rows.get(0).getMonth());
        Assert.assertEquals(Integer.valueOf(23), rows.get(0).getExpectedAttendanceDays());
    }

    @Test
    public void queryEmployeeMonthlyDetail_shouldDeductCompensatoryLeaveFromExpectedAttendanceWhenPersistedRowsAreZero() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        LocalDate shortWorkDate = LocalDate.of(2026, 3, 25);
        List<HrmOvertimeNightStatisticsDetail> persistedDetails = Arrays.asList(
                buildDetail(1001L, "李凤皇", "E088", 10L, "行政部", LocalDate.of(2026, 3, 25), "0.00", 0),
                buildDetail(1001L, "李凤皇", "E088", 10L, "行政部", LocalDate.of(2026, 3, 24), "0.00", 0)
        );
        persistedDetails.forEach(detail -> detail.setActualAttendanceDays(0));

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(25);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(detailRepository.findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(1001L, 2026, 3))
                .thenReturn(persistedDetails);
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", shortWorkDate, "4.00", "小时")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryEmployeeOvertimeNightDetailBO queryBO = new QueryEmployeeOvertimeNightDetailBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> rows = service.queryEmployeeMonthlyDetail(queryBO);

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("2026-03", rows.get(0).getMonth());
        Assert.assertEquals("历史明细实际出勤为0时，查看接口应按应出勤25天扣调休4小时返回24天",
                Integer.valueOf(24), rows.get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("196.00"), rows.get(0).getActualAttendanceHours());
        Assert.assertTrue(rows.get(0).getActualAttendanceRemark().contains("调休4.00小时"));
    }

    @Test
    public void queryEmployeeMonthlyDetail_shouldUseApprovalBeginEndWhenCompensatoryLeaveDurationIsBlank() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        LocalDate shortWorkDate = LocalDate.of(2026, 3, 25);
        HrmOvertimeNightStatisticsDetail persistedDetail = buildDetail(
                1001L, "李凤皇", "E088", 10L, "行政部", shortWorkDate, "0.00", 0
        );
        persistedDetail.setActualAttendanceDays(0);

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(25);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(detailRepository.findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(1001L, 2026, 3))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", shortWorkDate, "", "")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryEmployeeOvertimeNightDetailBO queryBO = new QueryEmployeeOvertimeNightDetailBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> rows = service.queryEmployeeMonthlyDetail(queryBO);

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("审批数据未保存duration时，应按调休开始/结束时间13:00-17:00扣减4小时",
                Integer.valueOf(24), rows.get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("196.00"), rows.get(0).getActualAttendanceHours());
        Assert.assertTrue(rows.get(0).getActualAttendanceRemark().contains("调休4.00小时"));
    }

    @Test
    public void queryEmployeeMonthlyDetail_shouldFilterBySelectedMonth() {
        when(detailRepository.findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(1001L, 2026, 3)).thenReturn(Arrays.asList(
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 2), "1.25", 1),
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 1), "2.50", 0)
        ));

        QueryEmployeeOvertimeNightDetailBO queryBO = new QueryEmployeeOvertimeNightDetailBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> rows = service.queryEmployeeMonthlyDetail(queryBO);

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("2026-03", rows.get(0).getMonth());
        Assert.assertEquals(new BigDecimal("3.75"), rows.get(0).getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(1), rows.get(0).getNightShiftCount());
        Assert.assertEquals(2, rows.get(0).getDailyDetails().size());
        verify(detailRepository).findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(1001L, 2026, 3);
        verify(detailRepository, never()).findAllByEmployeeIdOrderByWorkDateDesc(1001L);
    }

    @Test
    public void queryDailyDetailPageList_shouldReturnPagedPersistedRows() {
        HrmOvertimeNightStatisticsDetail firstDetail = buildDetail(1002L, "李四", "E002", 20L, "行政部", LocalDate.of(2026, 3, 3), "0.00", 1);
        firstDetail.setExpectedAttendanceDays(22);
        firstDetail.setActualAttendanceDays(19);
        when(detailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 3)).thenReturn(Arrays.asList(
                firstDetail,
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 2), "1.25", 1),
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 1), "2.50", 0)
        ));
        when(employeeRepository.findAllByEmployeeIdIn(anyList())).thenReturn(Arrays.asList(
                fixedRestProductionEmployee(1002L, "李四", "E002", 20L),
                fixedRestProductionEmployee(1001L, "张三", "E001", 10L)
        ));

        QueryOvertimeNightDailyDetailPageBO queryBO = new QueryOvertimeNightDailyDetailPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(2L);

        BasePage<QueryOvertimeNightDailyDetailPageVO> page = service.queryDailyDetailPageList(queryBO);

        Assert.assertEquals(Long.valueOf(3L), page.getTotalRow());
        Assert.assertEquals(2, page.getList().size());
        Assert.assertEquals("李四", page.getList().get(0).getEmployeeName());
        Assert.assertEquals("2026-03-03", page.getList().get(0).getWorkDate());
        Assert.assertEquals(Integer.valueOf(1), page.getList().get(0).getNightShiftCount());
        Assert.assertEquals(Integer.valueOf(22), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(Integer.valueOf(22), page.getList().get(0).getActualAttendanceDays());
        verify(detailRepository).findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 3);
    }

    @Test
    public void queryDailyDetailPageList_shouldFilterByEmployeeKeyword() {
        when(detailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 3)).thenReturn(Arrays.asList(
                buildDetail(1002L, "李四", "E002", 20L, "行政部", LocalDate.of(2026, 3, 3), "0.00", 1),
                buildDetail(1001L, "张三", "E001", 10L, "人事部", LocalDate.of(2026, 3, 2), "1.25", 1)
        ));
        when(employeeRepository.findAllByEmployeeIdIn(anyList())).thenReturn(Collections.singletonList(
                fixedRestProductionEmployee(1001L, "张三", "E001", 10L)
        ));

        QueryOvertimeNightDailyDetailPageBO queryBO = new QueryOvertimeNightDailyDetailPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setKeyword("张");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightDailyDetailPageVO> page = service.queryDailyDetailPageList(queryBO);

        Assert.assertEquals(Long.valueOf(1L), page.getTotalRow());
        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("张三", page.getList().get(0).getEmployeeName());
        Assert.assertEquals("1.25", page.getList().get(0).getOvertimeHours().toPlainString());
    }

    @Test
    public void queryDailyDetailPageList_shouldFillAdministrativeExpectedAttendanceDaysWhenPersistedRowsAreMissingAttendanceDays() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("潘红琼");
        employee.setJobNumber("E009");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);

        when(detailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 3))
                .thenReturn(Collections.singletonList(
                        buildDetail(1001L, "潘红琼", "E009", 10L, "行政部", LocalDate.of(2026, 3, 1), "0.00", 0)
                ));
        when(employeeRepository.findAllByEmployeeIdIn(anyList())).thenReturn(Collections.singletonList(employee));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryOvertimeNightDailyDetailPageBO queryBO = new QueryOvertimeNightDailyDetailPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightDailyDetailPageVO> page = service.queryDailyDetailPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("潘红琼", page.getList().get(0).getEmployeeName());
        Assert.assertEquals(Integer.valueOf(23), page.getList().get(0).getExpectedAttendanceDays());
    }

    @Test
    public void queryDailyDetailPageList_shouldDeductCompensatoryLeaveFromExpectedAttendanceWhenPersistedRowsAreZero() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        LocalDate shortWorkDate = LocalDate.of(2026, 3, 25);
        HrmOvertimeNightStatisticsDetail persistedDetail = buildDetail(
                1001L, "李凤皇", "E088", 10L, "行政部", shortWorkDate, "0.00", 0
        );
        persistedDetail.setActualAttendanceDays(0);

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(25);

        when(detailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 3))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(employeeRepository.findAllByEmployeeIdIn(anyList())).thenReturn(Collections.singletonList(employee));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", shortWorkDate, "4.00", "小时")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        QueryOvertimeNightDailyDetailPageBO queryBO = new QueryOvertimeNightDailyDetailPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightDailyDetailPageVO> page = service.queryDailyDetailPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("李凤皇", page.getList().get(0).getEmployeeName());
        Assert.assertEquals("历史每日明细实际出勤为0时，显示所有接口应按应出勤25天扣调休4小时返回24天",
                Integer.valueOf(24), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("196.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("调休4.00小时"));
    }

    @Test
    public void startStatistics_shouldIgnoreAttendanceReportDataAndCalculateFromPunches() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        List<HrmAttendancePlan> plans = new ArrayList<>();
        List<HrmAttendanceClock> clocks = new ArrayList<>();
        for (int day = 18; day <= 24; day++) {
            LocalDate workDate = LocalDate.of(2026, 3, day);
            plans.add(buildPlan(3000L + day, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                    LocalDateTime.of(workDate, LocalTime.of(18, 0))));
            clocks.add(buildClock(40000L + day, employee.getEmployeeId(), 1,
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate));
            clocks.add(buildClock(50000L + day, employee.getEmployeeId(), 2,
                    LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(20, 30)), workDate));
        }

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class))).thenReturn(plans);
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(new BigDecimal("24.50"), page.getList().get(0).getOvertimeHours());

        Map<String, BigDecimal> actualOvertimeMap = savedRowsRef.get().stream()
                .collect(Collectors.toMap(
                        detail -> LocalDateTime.ofInstant(detail.getWorkDate().toInstant(), ZoneId.systemDefault()).toLocalDate().toString(),
                        HrmOvertimeNightStatisticsDetail::getOvertimeHours,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-18"));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-19"));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-20"));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-21"));
        verify(attendanceReportDataRepository, never())
                .findAllByEmpIdAndWorkDateBetweenAndFieldNameIn(eq(1001L), any(Date.class), any(Date.class), anyList());
        verify(employeeOverTimeRecordRepository, never()).findAllByEmployeeIdAndAttendanceTimeBetween(eq(1001L), any(Date.class), any(Date.class));
    }

    @Test
    public void startStatistics_shouldNotAddMonthlyAutoOvertimeToActualAttendanceForProductionEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政人力资源部");

        LocalDate workDate = LocalDate.of(2026, 6, 17);
        HrmAttendanceShift shift = buildShift(2001L, "08:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                shift.getShiftId(),
                9001L,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(8, 0)),
                LocalDateTime.of(workDate, LocalTime.of(8, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                workDate
        );

        WorkweekMonthCalendarVO calendar = new WorkweekMonthCalendarVO();
        calendar.setLegalHolidayRestDays(1);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", workDate, "4.00", "小时")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(calendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(new BigDecimal("4.00"), row.getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(25), row.getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("196.00"), row.getActualAttendanceHours());
        Assert.assertEquals(new BigDecimal("200.00"), row.getAccruedAttendanceHours());
    }

    @Test
    public void startStatistics_shouldDeleteExistingRowsByWorkDateRangeBeforeSave() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        List<HrmAttendancePlan> plans = Collections.singletonList(
                buildPlan(3001L, employee.getEmployeeId(), shift.getShiftId(), LocalDate.of(2026, 3, 27))
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class))).thenReturn(plans);
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));

        AtomicReference<Boolean> deletedByWorkDateRef = new AtomicReference<>(false);
        doAnswer(invocation -> {
            deletedByWorkDateRef.set(true);
            return 1;
        }).when(detailRepository).deleteAllByWorkDateBetween(any(Date.class), any(Date.class));
        doThrow(new RuntimeException("duplicate key"))
                .when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        try {
            service.startStatistics(queryBO);
            Assert.fail("expected duplicate key simulation");
        } catch (RuntimeException ex) {
            Assert.assertEquals("duplicate key", ex.getMessage());
        }

        Assert.assertTrue(deletedByWorkDateRef.get());
        InOrder inOrder = inOrder(detailRepository);
        inOrder.verify(detailRepository).deleteAllByWorkDateBetween(any(Date.class), any(Date.class));
        inOrder.verify(detailRepository).flush();
        inOrder.verify(detailRepository).saveAll(anyList());
    }

    @Test
    public void startStatistics_shouldCalculateRowsBeforeDeletingExistingRowsToReduceLockTime() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 27);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(20, 30)), workDate)
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        service.startStatistics(queryBO);

        InOrder inOrder = inOrder(planRepository, clockRepository, detailRepository);
        inOrder.verify(planRepository).findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class));
        inOrder.verify(clockRepository).findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class));
        inOrder.verify(detailRepository).deleteAllByWorkDateBetween(any(Date.class), any(Date.class));
        inOrder.verify(detailRepository).saveAll(anyList());
    }

    @Test
    public void overtimeNightDetailRepositoryDeleteMethods_shouldUseBulkModifyingQueries() throws Exception {
        Method deleteMonthMethod = hrmOvertimeNightStatisticsDetailRepository.class.getMethod(
                "deleteAllByWorkDateBetween",
                Date.class,
                Date.class
        );
        Method deleteEmployeeMonthMethod = hrmOvertimeNightStatisticsDetailRepository.class.getMethod(
                "deleteAllByEmployeeIdAndWorkDateBetween",
                Long.class,
                Date.class,
                Date.class
        );

        Assert.assertEquals(Integer.TYPE, deleteMonthMethod.getReturnType());
        Assert.assertTrue(deleteMonthMethod.isAnnotationPresent(Modifying.class));
        Assert.assertTrue(deleteMonthMethod.isAnnotationPresent(Query.class));
        Assert.assertEquals(Integer.TYPE, deleteEmployeeMonthMethod.getReturnType());
        Assert.assertTrue(deleteEmployeeMonthMethod.isAnnotationPresent(Modifying.class));
        Assert.assertTrue(deleteEmployeeMonthMethod.isAnnotationPresent(Query.class));
    }

    @Test
    public void overtimeNightStatisticsEntryPoints_shouldNotUseLongRunningTransactions() throws Exception {
        Method startStatisticsMethod = HrmOvertimeNightStatisticsServiceImpl.class.getMethod(
                "startStatistics",
                QueryOvertimeNightStatisticsPageBO.class
        );
        Method startEmployeeStatisticsMethod = HrmOvertimeNightStatisticsServiceImpl.class.getMethod(
                "startStatisticsForEmployee",
                QueryOvertimeNightStatisticsPageBO.class
        );

        Assert.assertFalse(startStatisticsMethod.isAnnotationPresent(Transactional.class));
        Assert.assertFalse(startEmployeeStatisticsMethod.isAnnotationPresent(Transactional.class));
    }

    @Test
    public void startStatistics_shouldKeepEmployeesWithoutRawMonthlyDataInResultAsZeroSummary() {
        HrmEmployee employeeWithData = new HrmEmployee();
        employeeWithData.setEmployeeId(1001L);
        employeeWithData.setEmployeeName("张三");
        employeeWithData.setJobNumber("E001");
        employeeWithData.setDeptId(10L);
        employeeWithData.setIsDel(0);
        markFixedRestProduction(employeeWithData);

        HrmEmployee employeeWithoutData = new HrmEmployee();
        employeeWithoutData.setEmployeeId(1002L);
        employeeWithoutData.setEmployeeName("李四");
        employeeWithoutData.setJobNumber("E002");
        employeeWithoutData.setDeptId(10L);
        employeeWithoutData.setIsDel(0);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 4, 18);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        List<HrmAttendancePlan> plans = Collections.singletonList(
                buildPlan(3001L, employeeWithData.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employeeWithData.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employeeWithData.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(20, 0)), workDate)
        );

        when(employeeRepository.findAll()).thenReturn(Arrays.asList(employeeWithData, employeeWithoutData));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class))).thenReturn(plans);
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class))).thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(4), anyList()))
                .thenAnswer(invocation -> {
                    List<Long> employeeIds = invocation.getArgument(2);
                    return savedRowsRef.get().stream()
                            .filter(detail -> employeeIds.contains(detail.getEmployeeId()))
                            .collect(Collectors.toList());
                });

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-04");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(2, page.getList().size());
        Assert.assertEquals(Long.valueOf(2L), page.getTotalRow());

        QueryOvertimeNightStatisticsPageVO firstRow = page.getList().get(0);
        QueryOvertimeNightStatisticsPageVO secondRow = page.getList().get(1);
        Assert.assertEquals("张三", firstRow.getEmployeeName());
        Assert.assertEquals(new BigDecimal("3.00"), firstRow.getOvertimeHours());
        Assert.assertEquals("李四", secondRow.getEmployeeName());
        Assert.assertEquals(new BigDecimal("0.00"), secondRow.getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(0), secondRow.getNightShiftCount());

        Assert.assertEquals(2, savedRowsRef.get().size());
        Assert.assertTrue(savedRowsRef.get().stream().anyMatch(detail ->
                Objects.equals(1002L, detail.getEmployeeId())
                        && new BigDecimal("0.00").compareTo(detail.getOvertimeHours()) == 0
                        && Integer.valueOf(0).equals(detail.getNightShiftCount())
        ));
    }

    @Test
    public void startStatistics_shouldCalculateExpectedAndActualAttendanceDaysForAdministrativeEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setJobNumber("E001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        LocalDate fullDay = LocalDate.of(2026, 3, 2);
        LocalDate shortDay = LocalDate.of(2026, 3, 3);
        LocalDate crossDay = LocalDate.of(2026, 3, 4);
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(fullDay, LocalTime.of(9, 0)),
                        LocalDateTime.of(fullDay, LocalTime.of(9, 0)), fullDay),
                buildClock(4002L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(fullDay, LocalTime.of(17, 0)),
                        LocalDateTime.of(fullDay, LocalTime.of(17, 0)), fullDay),
                buildClock(4003L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(shortDay, LocalTime.of(9, 0)),
                        LocalDateTime.of(shortDay, LocalTime.of(9, 0)), shortDay),
                buildClock(4004L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(shortDay, LocalTime.of(16, 30)),
                        LocalDateTime.of(shortDay, LocalTime.of(16, 30)), shortDay),
                buildClock(4005L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(crossDay, LocalTime.of(20, 0)),
                        LocalDateTime.of(crossDay, LocalTime.of(20, 0)), crossDay),
                buildClock(4006L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(crossDay.plusDays(1), LocalTime.of(4, 10)),
                        LocalDateTime.of(crossDay.plusDays(1), LocalTime.of(4, 10)), crossDay)
        );
        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(22);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(22), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(Integer.valueOf(22), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("176.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(22).equals(detail.getExpectedAttendanceDays())
                        && Integer.valueOf(22).equals(detail.getActualAttendanceDays())
        ));
    }

    @Test
    public void startStatistics_shouldDeductCompensatoryLeaveHoursFromExpectedAttendanceDays() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("E088");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        List<HrmAttendanceClock> clocks = new ArrayList<>();
        for (int day = 1; day <= 24; day++) {
            LocalDate workDate = LocalDate.of(2026, 3, day);
            clocks.add(buildClock(4100L + day, employee.getEmployeeId(), 1,
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate));
            clocks.add(buildClock(4200L + day, employee.getEmployeeId(), 2,
                    LocalDateTime.of(workDate, LocalTime.of(17, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(17, 0)), workDate));
        }
        LocalDate shortWorkDate = LocalDate.of(2026, 3, 25);
        clocks.add(buildClock(4301L, employee.getEmployeeId(), 1,
                LocalDateTime.of(shortWorkDate, LocalTime.of(9, 0)),
                LocalDateTime.of(shortWorkDate, LocalTime.of(9, 0)), shortWorkDate));
        clocks.add(buildClock(4302L, employee.getEmployeeId(), 2,
                LocalDateTime.of(shortWorkDate, LocalTime.of(13, 0)),
                LocalDateTime.of(shortWorkDate, LocalTime.of(13, 0)), shortWorkDate));

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(25);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-lifenghuang", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildCompensatoryLeaveApprove("LEAVE-001", "ding-lifenghuang", shortWorkDate, "4.00", "小时")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("25天应出勤扣调休4小时后应按196小时折算为24天",
                Integer.valueOf(24), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("196.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("调休4.00小时"));
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(24).equals(detail.getActualAttendanceDays())
        ));
    }

    @Test
    public void startStatistics_shouldUseTimeRangeForRoundedFractionalDayLeaveDeduction() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("闫倩");
        employee.setJobNumber("TYNG-089");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);

        LocalDate leaveDate = LocalDate.of(2026, 6, 26);
        tbattendanceapprove roundedLeave = buildLeaveApproveWithTimeRange(
                "PROC-LEAVE-HALF-HOUR",
                "ding-yanqian",
                leaveDate,
                "调休",
                "0.07",
                "天",
                LocalTime.of(17, 30),
                LocalTime.of(18, 0)
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-yanqian", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(roundedLeave));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(23), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("183.50"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("调休0.50小时"));
    }

    @Test
    public void startStatistics_shouldDeductPersonalSickCompensatoryAndAnnualLeaveFromExpectedAttendanceDays() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张明");
        employee.setJobNumber("TYNG-012");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);

        LocalDate workDate = LocalDate.of(2026, 6, 10);
        List<tbattendanceapprove> approvals = Arrays.asList(
                buildLeaveApprove("LEAVE-001", "ding-zhangming", workDate, "事假", "4.00", "小时"),
                buildLeaveApprove("LEAVE-002", "ding-zhangming", workDate.plusDays(1), "病假", "8.00", "小时"),
                buildLeaveApprove("LEAVE-003", "ding-zhangming", workDate.plusDays(2), "调休", "4.00", "小时"),
                buildLeaveApprove("LEAVE-004", "ding-zhangming", workDate.plusDays(3), "年假", "8.00", "小时"),
                buildLeaveApprove("LEAVE-005", "ding-zhangming", workDate.plusDays(4), "外出", "8.00", "小时")
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangming", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(approvals);
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(23), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals("23天应出勤只扣事假4小时、病假8小时、调休4小时、年假8小时，外出不扣",
                Integer.valueOf(20), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("160.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertEquals("应计出勤小时应按实际出勤160小时 + 调休4小时 + 年假8小时 + 病假8小时计算",
                new BigDecimal("180.00"), page.getList().get(0).getAccruedAttendanceHours());
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("事假4.00小时"));
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("病假8.00小时"));
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("调休4.00小时"));
        Assert.assertTrue(page.getList().get(0).getActualAttendanceRemark().contains("年假8.00小时"));
        Assert.assertFalse(page.getList().get(0).getActualAttendanceRemark().contains("外出"));
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                new BigDecimal("180.00").equals(detail.getAccruedAttendanceHours())
        ));
    }

    @Test
    public void startStatistics_shouldCalculateLiMingmingActualAndAccruedAttendanceHours() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1831601326890434563L);
        employee.setEmployeeName("李明明");
        employee.setJobNumber("TYNG-099");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(23);

        List<tbattendanceapprove> approvals = Arrays.asList(
                buildAttendanceApprove("OT-001", "024662561026250638", LocalDate.of(2026, 6, 6), "4.00", "小时"),
                buildAttendanceApprove("OT-002", "024662561026250638", LocalDate.of(2026, 6, 7), "4.00", "小时"),
                buildLeaveApprove("LEAVE-ANNUAL", "024662561026250638", LocalDate.of(2026, 6, 14), "年假", "4", "天")
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1831601326890434563L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1831601326890434563L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1831601326890434563L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1831601326890434563L))
                .thenReturn(Optional.of(buildAttendanceUser(1831601326890434563L, "024662561026250638", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(approvals);
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(Integer.valueOf(23), row.getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("0.00"), row.getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(20), row.getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("160.00"), row.getActualAttendanceHours());
        Assert.assertEquals(new BigDecimal("184.00"), row.getAccruedAttendanceHours());
        Assert.assertTrue(row.getActualAttendanceRemark().contains("年假32.00小时"));
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(20).equals(detail.getActualAttendanceDays())
                        && new BigDecimal("184.00").equals(detail.getAccruedAttendanceHours())
        ));
    }

    @Test
    public void queryPageList_shouldRecalculateLiMingmingAccruedAttendanceWhenPersistedValueIsStale() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1831601326890434563L);
        employee.setEmployeeName("李明明");
        employee.setJobNumber("TYNG-099");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmOvertimeNightStatisticsDetail firstDetail = buildPersistedDetail(
                1831601326890434563L, "李明明", "TYNG-099", LocalDate.of(2026, 6, 6)
        );
        firstDetail.setExpectedAttendanceDays(23);
        firstDetail.setActualAttendanceDays(0);
        firstDetail.setOvertimeHours(new BigDecimal("4.00"));
        firstDetail.setAccruedAttendanceHours(new BigDecimal("207.40"));
        HrmOvertimeNightStatisticsDetail secondDetail = buildPersistedDetail(
                1831601326890434563L, "李明明", "TYNG-099", LocalDate.of(2026, 6, 7)
        );
        secondDetail.setExpectedAttendanceDays(23);
        secondDetail.setActualAttendanceDays(0);
        secondDetail.setOvertimeHours(new BigDecimal("4.00"));
        secondDetail.setAccruedAttendanceHours(new BigDecimal("207.40"));

        List<tbattendanceapprove> approvals = Arrays.asList(
                buildAttendanceApprove("OT-001", "024662561026250638", LocalDate.of(2026, 6, 6), "4.00", "小时"),
                buildAttendanceApprove("OT-002", "024662561026250638", LocalDate.of(2026, 6, 7), "4.00", "小时"),
                buildLeaveApprove("LEAVE-ANNUAL", "024662561026250638", LocalDate.of(2026, 6, 14), "年假", "4", "天")
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Arrays.asList(firstDetail, secondDetail));
        when(attendanceUserRepository.findFirstByEmpId(1831601326890434563L))
                .thenReturn(Optional.of(buildAttendanceUser(1831601326890434563L, "024662561026250638", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(approvals);

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        QueryOvertimeNightStatisticsPageVO row = page.getList().get(0);
        Assert.assertEquals(Integer.valueOf(23), row.getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("0.00"), row.getOvertimeHours());
        Assert.assertEquals(new BigDecimal("160.00"), row.getActualAttendanceHours());
        Assert.assertEquals(new BigDecimal("184.00"), row.getAccruedAttendanceHours());
    }

    @Test
    public void startStatistics_shouldAddOvertimeApprovalToActualAttendance() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("王五");
        employee.setJobNumber("E100");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        LocalDate workDate = LocalDate.of(2026, 6, 10);
        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(1);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-wangwu", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildAttendanceApprove("OT-001", "ding-wangwu", workDate, "8.00", "小时")
                ));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(1), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals("实际出勤应按应出勤8小时 + 加班8小时计算",
                Integer.valueOf(2), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("16.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(2).equals(detail.getActualAttendanceDays())
        ));
    }

    @Test
    public void startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("王五");
        employee.setJobNumber("E100");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("行政部");

        LocalDate workDate = LocalDate.of(2026, 6, 10);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(22, 0)), workDate)
        );
        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setWorkDays(1);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(1), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals("行政体系员工实际出勤只加审批加班，不加日级自动加班",
                new BigDecimal("8.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertEquals(Integer.valueOf(1), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals("行政体系员工应计出勤不扣日级自动加班",
                new BigDecimal("8.00"), page.getList().get(0).getAccruedAttendanceHours());
    }

    @Test
    public void queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张明");
        employee.setJobNumber("TYNG-012");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmOvertimeNightStatisticsDetail persistedDetail = buildPersistedDetail(1001L, "张明", "TYNG-012", LocalDate.of(2026, 6, 1));
        persistedDetail.setExpectedAttendanceDays(23);
        persistedDetail.setActualAttendanceDays(20);
        persistedDetail.setOvertimeHours(new BigDecimal("47.72"));

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangming", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildLeaveApprove("LEAVE-004", "ding-zhangming", LocalDate.of(2026, 6, 30), "外出", "8.00", "小时")
                ));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(23), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals("考勤汇总同步应使用实际/应计出勤模块同口径的加班小时",
                new BigDecimal("0.00"), page.getList().get(0).getAttendanceOvertimeHours());
        Assert.assertEquals("行政体系员工实际出勤不加日级自动加班，只有外出审批时应保持应出勤184小时",
                Integer.valueOf(23), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("184.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertEquals("行政体系员工应计出勤不扣日级自动加班",
                new BigDecimal("184.00"), page.getList().get(0).getAccruedAttendanceHours());
        Assert.assertEquals("", page.getList().get(0).getActualAttendanceRemark());
    }

    @Test
    public void queryPageList_shouldAddOvertimeApprovalToActualAttendance() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("王五");
        employee.setJobNumber("E100");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmOvertimeNightStatisticsDetail persistedDetail = buildPersistedDetail(1001L, "王五", "E100", LocalDate.of(2026, 6, 10));
        persistedDetail.setExpectedAttendanceDays(1);
        persistedDetail.setActualAttendanceDays(2);
        persistedDetail.setOvertimeHours(new BigDecimal("8.00"));
        LocalDate workDate = LocalDate.of(2026, 6, 10);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-wangwu", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildAttendanceApprove("OT-001", "ding-wangwu", workDate, "8.00", "小时")
                ));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(new BigDecimal("8.00"), page.getList().get(0).getAttendanceOvertimeHours());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(Integer.valueOf(2), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("16.00"), page.getList().get(0).getActualAttendanceHours());
    }

    @Test
    public void startStatistics_shouldUseFixedFourDayMonthlyRestForProductionEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("李四");
        employee.setJobNumber("E002");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 6, 2);
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4011L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4012L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(17, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(17, 0)), workDate)
        );
        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setLegalHolidayRestDays(1);

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(25), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertEquals(Integer.valueOf(25), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals(new BigDecimal("200.00"), page.getList().get(0).getActualAttendanceHours());
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(25).equals(detail.getExpectedAttendanceDays())
                        && Integer.valueOf(25).equals(detail.getActualAttendanceDays())
        ));
        verify(workweekSettingService, atLeastOnce()).queryMonthCalendar(any());
    }

    @Test
    public void startStatistics_shouldDeductOnlyWeekdayLegalRestForProductionEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("王芳");
        employee.setJobNumber("E003");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setLegalHolidayRestDays(3);
        workweekCalendar.setDays(Arrays.asList(
                buildWorkweekDay("2026-06-19", 5, "legal_rest"),
                buildWorkweekDay("2026-06-20", 6, "legal_rest"),
                buildWorkweekDay("2026-06-21", 7, "legal_rest")
        ));

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        service.startStatistics(queryBO);

        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(25).equals(detail.getExpectedAttendanceDays())
                        && Integer.valueOf(25).equals(detail.getActualAttendanceDays())
                        && new BigDecimal("200.00").equals(detail.getAccruedAttendanceHours())
        ));
    }

    @Test
    public void startStatistics_shouldUseConfiguredProductionMonthlyRestDaysForProductionEmployee() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("王芳");
        employee.setJobNumber("E003");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(2);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        WorkweekMonthCalendarVO workweekCalendar = new WorkweekMonthCalendarVO();
        workweekCalendar.setDays(Collections.singletonList(
                buildWorkweekDay("2026-06-19", 5, "legal_rest")
        ));

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(workweekSettingService.queryMonthCalendar(any())).thenReturn(workweekCalendar);
        when(salaryBasicMapper.selectList(any())).thenReturn(Collections.singletonList(
                new HrmSalaryBasic().setProductionMonthlyRestDays(6)
        ));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(23), page.getList().get(0).getExpectedAttendanceDays());
        Assert.assertFalse(savedRowsRef.get().isEmpty());
        Assert.assertTrue(savedRowsRef.get().stream().allMatch(detail ->
                Integer.valueOf(23).equals(detail.getExpectedAttendanceDays())
        ));
    }

    @Test
    public void startStatistics_shouldKeepZeroWhenOnlyFallbackPlanCheckTimeExistsWithoutOvertimeRecord() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 18);
        HrmAttendancePlan onDutyPlan = buildPlan(
                3001L, employee.getEmployeeId(), 1516625132L, 9001L, workDate, "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(7, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3002L, employee.getEmployeeId(), 1516625133L, 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(15, 0))
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                LocalDateTime.of(workDate, LocalTime.of(17, 30)),
                LocalDateTime.of(workDate, LocalTime.of(17, 30)),
                workDate.plusDays(1)
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(offDutyClock));
        when(shiftRepository.findById(anyLong())).thenReturn(Optional.empty());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());

        HrmOvertimeNightStatisticsDetail savedDetail = savedRowsRef.get().get(0);
        Assert.assertEquals(Long.valueOf(3002L), savedDetail.getPlanId());
        Assert.assertEquals(Long.valueOf(1516625133L), savedDetail.getClassId());
        Assert.assertEquals(new BigDecimal("0.00"), savedDetail.getOvertimeHours());
        Assert.assertEquals(
                LocalDateTime.of(workDate, LocalTime.of(15, 0)),
                LocalDateTime.ofInstant(savedDetail.getScheduledOffTime().toInstant(), ZoneId.systemDefault())
        );
        Assert.assertEquals(
                LocalDateTime.of(workDate, LocalTime.of(17, 30)),
                LocalDateTime.ofInstant(savedDetail.getActualOffTime().toInstant(), ZoneId.systemDefault())
        );
    }

    @Test
    public void startStatistics_shouldPreferNonNullOffDutyPlanCheckTimeWhenDuplicateOffDutyPlansExist() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 19);
        HrmAttendancePlan onDutyPlan = buildPlan(
                3001L, employee.getEmployeeId(), 1516625132L, 9001L, workDate, "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(7, 0))
        );
        HrmAttendancePlan offDutyPlanWithoutCheckTime = buildPlan(
                3002L, employee.getEmployeeId(), 1516625133L, 9001L, workDate, "OffDuty", null
        );
        HrmAttendancePlan offDutyPlanWithCheckTime = buildPlan(
                3003L, employee.getEmployeeId(), 1516625133L, 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(15, 0))
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                LocalDateTime.of(workDate, LocalTime.of(17, 30)),
                LocalDateTime.of(workDate, LocalTime.of(17, 30)),
                workDate
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlanWithoutCheckTime, offDutyPlanWithCheckTime));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(offDutyClock));
        when(shiftRepository.findById(anyLong())).thenReturn(Optional.empty());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(new BigDecimal("0.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(Long.valueOf(3003L), savedRowsRef.get().get(0).getPlanId());
        Assert.assertEquals(new BigDecimal("0.00"), savedRowsRef.get().get(0).getOvertimeHours());
    }

    @Test
    public void startStatistics_shouldPreferAttendanceApprovalOverAutoCalculatedOvertimeWhenApprovalExists() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setJobNumber("E001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 5, 6);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(22, 0)), workDate)
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangsan", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildAttendanceApprove("APP-001", "ding-zhangsan", workDate, "2.00", "小时")
                ));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(5), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("命中审批后应优先按审批时长统计，而不是继续保留自动计算的4小时",
                new BigDecimal("2.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(new BigDecimal("2.00"), savedRowsRef.get().get(0).getOvertimeHours());
    }

    @Test
    public void startStatistics_shouldIgnoreCancelledAttendanceApprovalWhenCalculatingOvertime() throws Exception {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setJobNumber("E001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 5, 6);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(22, 0)), workDate)
        );
        tbattendanceapprove cancelledApproval = buildAttendanceApprove("APP-CANCELLED", "ding-zhangsan", workDate, "2.00", "小时");
        setApprovalStatisticsStatus(cancelledApproval, "取消至统计");

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangsan", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(cancelledApproval));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(5), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("取消至统计的加班审批不得覆盖自动加班小时",
                new BigDecimal("5.00"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(new BigDecimal("5.00"), savedRowsRef.get().get(0).getOvertimeHours());
    }

    @Test
    public void queryPageList_shouldIgnoreCancelledLeaveApprovalWhenCalculatingActualAttendance() throws Exception {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张明");
        employee.setJobNumber("TYNG-012");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        employee.setAffiliationSystem(1);

        HrmOvertimeNightStatisticsDetail persistedDetail = buildPersistedDetail(1001L, "张明", "TYNG-012", LocalDate.of(2026, 6, 1));
        persistedDetail.setExpectedAttendanceDays(23);
        persistedDetail.setActualAttendanceDays(20);
        tbattendanceapprove cancelledLeave = buildLeaveApprove("LEAVE-CANCELLED", "ding-zhangming", LocalDate.of(2026, 6, 10), "事假", "8.00", "小时");
        setApprovalStatisticsStatus(cancelledLeave, "取消至统计");

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(6), anyList()))
                .thenReturn(Collections.singletonList(persistedDetail));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangming", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(cancelledLeave));

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-06");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.queryPageList(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("取消至统计的请假审批不得扣减实际出勤",
                Integer.valueOf(23), page.getList().get(0).getActualAttendanceDays());
        Assert.assertEquals("", page.getList().get(0).getActualAttendanceRemark());
    }

    @Test
    public void startStatistics_shouldAccumulateMultipleAttendanceApprovalsForSameEmployeeAndDay() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setJobNumber("E001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 5, 7);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, employee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(19, 0)), workDate)
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangsan", 9001L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(
                        buildAttendanceApprove("APP-002", "ding-zhangsan", workDate, "1.50", "小时"),
                        buildAttendanceApprove("APP-003", "ding-zhangsan", workDate, "2.25", "小时")
                ));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(5), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals("同一员工同日多条审批必须按日累加",
                new BigDecimal("3.75"), page.getList().get(0).getOvertimeHours());
        Assert.assertEquals(new BigDecimal("3.75"), savedRowsRef.get().get(0).getOvertimeHours());
    }

    @Test
    public void startStatistics_shouldIgnoreSameNameOtherEmployeesAttendanceApprovals() {
        HrmEmployee employeeA = new HrmEmployee();
        employeeA.setEmployeeId(1001L);
        employeeA.setEmployeeName("张三");
        employeeA.setJobNumber("E001");
        employeeA.setDeptId(10L);
        employeeA.setIsDel(0);
        markFixedRestProduction(employeeA);

        HrmEmployee employeeB = new HrmEmployee();
        employeeB.setEmployeeId(1002L);
        employeeB.setEmployeeName("张三");
        employeeB.setJobNumber("E002");
        employeeB.setDeptId(10L);
        employeeB.setIsDel(0);
        markFixedRestProduction(employeeB);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 5, 8);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan planA = buildPlan(
                3001L, employeeA.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        HrmAttendancePlan planB = buildPlan(
                3002L, employeeB.getEmployeeId(), shift.getShiftId(), 9002L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocksA = Arrays.asList(
                buildClock(4001L, employeeA.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employeeA.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(19, 0)), workDate)
        );
        List<HrmAttendanceClock> clocksB = Arrays.asList(
                buildClock(5001L, employeeB.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(5002L, employeeB.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(19, 0)), workDate)
        );

        when(employeeRepository.findAll()).thenReturn(Arrays.asList(employeeA, employeeB));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(planA));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(planB));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocksA);
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(clocksB);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(attendanceUserRepository.findFirstByEmpId(1001L))
                .thenReturn(Optional.of(buildAttendanceUser(1001L, "ding-zhangsan-a", 9001L)));
        when(attendanceUserRepository.findFirstByEmpId(1002L))
                .thenReturn(Optional.of(buildAttendanceUser(1002L, "ding-zhangsan-b", 9002L)));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(
                        buildAttendanceApprove("APP-004", "ding-zhangsan-b", workDate, "2.50", "小时")
                ));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(5), anyList()))
                .thenAnswer(invocation -> {
                    List<Long> employeeIds = invocation.getArgument(2);
                    return savedRowsRef.get().stream()
                            .filter(detail -> employeeIds.contains(detail.getEmployeeId()))
                            .collect(Collectors.toList());
                });

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(2, page.getList().size());
        Map<Long, QueryOvertimeNightStatisticsPageVO> resultMap = page.getList().stream()
                .collect(Collectors.toMap(QueryOvertimeNightStatisticsPageVO::getEmployeeId, row -> row));
        Assert.assertEquals("同名员工A不能错误吃到员工B的审批时长，应保留自己的自动计算结果",
                new BigDecimal("2.00"), resultMap.get(1001L).getOvertimeHours());
        Assert.assertEquals("员工B应命中自己的审批时长",
                new BigDecimal("2.50"), resultMap.get(1002L).getOvertimeHours());
    }

    @Test
    public void startStatisticsForEmployee_shouldCalculateAllSelectedEmployeesInBatch() {
        HrmEmployee employeeA = new HrmEmployee();
        employeeA.setEmployeeId(1001L);
        employeeA.setEmployeeName("张三");
        employeeA.setJobNumber("E001");
        employeeA.setDeptId(10L);
        employeeA.setIsDel(0);

        HrmEmployee employeeB = new HrmEmployee();
        employeeB.setEmployeeId(1002L);
        employeeB.setEmployeeName("李四");
        employeeB.setJobNumber("E002");
        employeeB.setDeptId(10L);
        employeeB.setIsDel(0);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 5, 8);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan planA = buildPlan(
                3001L, employeeA.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        HrmAttendancePlan planB = buildPlan(
                3002L, employeeB.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocksA = Arrays.asList(
                buildClock(4001L, employeeA.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, employeeA.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(19, 0)), workDate)
        );
        List<HrmAttendanceClock> clocksB = Arrays.asList(
                buildClock(5001L, employeeB.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(5002L, employeeB.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(20, 0)), workDate)
        );

        when(employeeRepository.findAllByEmployeeIdIn(anyList())).thenReturn(Arrays.asList(employeeA, employeeB));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(planA));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(planB));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocksA);
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(clocksB);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1002L)).thenReturn(Collections.emptyList());

        List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>();
        doAnswer(invocation -> {
            savedRows.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setEmployeeIds(Arrays.asList(1001L, 1002L));

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Arrays.asList(1001L, 1002L), savedRows.stream()
                .map(HrmOvertimeNightStatisticsDetail::getEmployeeId)
                .distinct()
                .collect(Collectors.toList()));
        verify(detailRepository).deleteAllByEmployeeIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class));
        verify(detailRepository).deleteAllByEmployeeIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class));
        verify(detailRepository, never()).deleteAllByEmployeeIdAndWorkDateBetween(eq(1003L), any(Date.class), any(Date.class));
        verify(planRepository, never()).findAllByEmpIdAndWorkDateBetween(eq(1003L), any(Date.class), any(Date.class));
    }

    @Test
    public void startStatisticsForEmployee_shouldSkipDeletedSelectedEmployeesLikeFullStatistics() {
        HrmEmployee activeEmployee = new HrmEmployee();
        activeEmployee.setEmployeeId(1001L);
        activeEmployee.setEmployeeName("张三");
        activeEmployee.setJobNumber("E001");
        activeEmployee.setDeptId(10L);
        activeEmployee.setIsDel(0);

        HrmEmployee deletedEmployee = new HrmEmployee();
        deletedEmployee.setEmployeeId(1002L);
        deletedEmployee.setEmployeeName("已删除员工");
        deletedEmployee.setJobNumber("E002");
        deletedEmployee.setDeptId(10L);
        deletedEmployee.setIsDel(1);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 5, 8);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(
                3001L, activeEmployee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0))
        );
        List<HrmAttendanceClock> clocks = Arrays.asList(
                buildClock(4001L, activeEmployee.getEmployeeId(), 1,
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate),
                buildClock(4002L, activeEmployee.getEmployeeId(), 2,
                        LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                        LocalDateTime.of(workDate, LocalTime.of(19, 0)), workDate)
        );

        when(employeeRepository.findAllByEmployeeIdIn(anyList())).thenReturn(Arrays.asList(activeEmployee, deletedEmployee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>();
        doAnswer(invocation -> {
            savedRows.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-05");
        queryBO.setEmployeeIds(Arrays.asList(1001L, 1002L));

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Collections.singletonList(1001L), savedRows.stream()
                .map(HrmOvertimeNightStatisticsDetail::getEmployeeId)
                .distinct()
                .collect(Collectors.toList()));
        verify(detailRepository).deleteAllByEmployeeIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class));
        verify(detailRepository, never()).deleteAllByEmployeeIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class));
        verify(planRepository, never()).findAllByEmpIdAndWorkDateBetween(eq(1002L), any(Date.class), any(Date.class));
    }

    @Test
    public void startStatisticsForEmployee_shouldIgnoreAttendanceReportDataAndCalculateFromPunches() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        List<HrmAttendancePlan> plans = new ArrayList<>();
        List<HrmAttendanceClock> clocks = new ArrayList<>();
        for (int day = 18; day <= 24; day++) {
            LocalDate workDate = LocalDate.of(2026, 3, day);
            plans.add(buildPlan(3000L + day, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                    LocalDateTime.of(workDate, LocalTime.of(18, 0))));
            clocks.add(buildClock(4000L + day, employee.getEmployeeId(), 1,
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate));
            clocks.add(buildClock(5000L + day, employee.getEmployeeId(), 2,
                    LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(20, 30)), workDate));
        }

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class))).thenReturn(plans);
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(clocks);
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(new BigDecimal("24.50"), result.get(0).getOvertimeHours());

        Map<String, BigDecimal> actualOvertimeMap = savedRowsRef.get().stream()
                .collect(Collectors.toMap(
                        detail -> LocalDateTime.ofInstant(detail.getWorkDate().toInstant(), ZoneId.systemDefault()).toLocalDate().toString(),
                        HrmOvertimeNightStatisticsDetail::getOvertimeHours,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-18"));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-20"));
        Assert.assertEquals(new BigDecimal("3.50"), actualOvertimeMap.get("2026-03-24"));
        verify(attendanceReportDataRepository, never())
                .findAllByEmpIdAndWorkDateBetweenAndFieldNameIn(eq(1001L), any(Date.class), any(Date.class), anyList());
        verify(employeeOverTimeRecordRepository, never()).findAllByEmployeeIdAndAttendanceTimeBetween(eq(1001L), any(Date.class), any(Date.class));
    }

    @Test
    public void startStatisticsForEmployee_shouldCalculateOvertimeByPairedWorkMinutesMinusEightHours() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 18);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0)));
        HrmAttendanceClock onDutyClock1 = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock1 = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(12, 0)),
                LocalDateTime.of(workDate, LocalTime.of(12, 0)),
                workDate
        );
        HrmAttendanceClock onDutyClock2 = buildClock(
                4003L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(13, 0)),
                LocalDateTime.of(workDate, LocalTime.of(13, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock2 = buildClock(
                4004L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(18, 30)),
                LocalDateTime.of(workDate, LocalTime.of(18, 30)),
                workDate
        );

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock1, offDutyClock1, onDutyClock2, offDutyClock2));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2026-03", result.get(0).getMonth());
        Assert.assertEquals(new BigDecimal("1.50"), result.get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(new BigDecimal("1.50"), savedRowsRef.get().get(0).getOvertimeHours());
        verify(employeeOverTimeRecordRepository, never()).findAllByEmployeeIdAndAttendanceTimeBetween(eq(1001L), any(Date.class), any(Date.class));
    }

    @Test
    public void startStatisticsForEmployee_shouldUseAttendanceDetailWhenClockRecordsMissing() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0)));
        tbattendancedetail onDutyDetail = buildAttendanceDetailRecord(
                5001L,
                employee.getEmployeeId(),
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                workDate
        );
        tbattendancedetail offDutyDetail1 = buildAttendanceDetailRecord(
                5002L,
                employee.getEmployeeId(),
                "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(12, 0)),
                LocalDateTime.of(workDate, LocalTime.of(12, 0)),
                workDate
        );
        tbattendancedetail onDutyDetail2 = buildAttendanceDetailRecord(
                5003L,
                employee.getEmployeeId(),
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(13, 0)),
                LocalDateTime.of(workDate, LocalTime.of(13, 0)),
                workDate
        );
        tbattendancedetail offDutyDetail2 = buildAttendanceDetailRecord(
                5004L,
                employee.getEmployeeId(),
                "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 30)),
                LocalDateTime.of(workDate, LocalTime.of(18, 30)),
                workDate
        );

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyDetail, offDutyDetail1, onDutyDetail2, offDutyDetail2));
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2026-03", result.get(0).getMonth());
        Assert.assertEquals(new BigDecimal("1.50"), result.get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(new BigDecimal("1.50"), savedRowsRef.get().get(0).getOvertimeHours());
        verify(employeeOverTimeRecordRepository, never()).findAllByEmployeeIdAndAttendanceTimeBetween(eq(1001L), any(Date.class), any(Date.class));
    }

    @Test
    public void startStatisticsForEmployee_shouldUseScheduledStartTimeWhenClockInEarlierThanShiftStart() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        HrmAttendanceShift shift = buildShift(2001L, "08:00", "17:30");
        HrmAttendancePlan onDutyPlan = buildPlan(
                3000L,
                employee.getEmployeeId(),
                shift.getShiftId(),
                1449530106L,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(8, 0))
        );
        HrmAttendancePlan plan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                shift.getShiftId(),
                1449530106L,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(17, 30))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(8, 0)),
                LocalDateTime.of(workDate, LocalTime.of(7, 48, 44)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(17, 30, 12)),
                LocalDateTime.of(workDate, LocalTime.of(17, 30, 12)),
                workDate
        );

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(new BigDecimal("1.50"), result.get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(new BigDecimal("1.50"), savedRowsRef.get().get(0).getOvertimeHours());
    }

    @Test
    public void startStatisticsForEmployee_shouldUseGroupShiftWhenClassShiftMissing() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        Long groupId = 1449530106L;
        Long onDutyClassId = 1516625132L;
        Long offDutyClassId = 1516625133L;
        HrmAttendanceShift groupShift = buildShift(2001L, "08:00", "17:30");
        String[] shiftTokens = {"1001", "1002", "1003", "1004", "1005", "1006", "1007"};
        shiftTokens[workDate.getDayOfWeek().getValue() - 1] = String.valueOf(groupShift.getShiftId());
        HrmAttendanceGroup group = buildGroup(groupId, "白班组", String.join(",", shiftTokens));

        HrmAttendancePlan onDutyPlan = buildPlan(
                3000L,
                employee.getEmployeeId(),
                onDutyClassId,
                groupId,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(7, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                offDutyClassId,
                groupId,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(15, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(7, 0)),
                LocalDateTime.of(workDate, LocalTime.of(7, 48, 44)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(17, 30, 12)),
                LocalDateTime.of(workDate, LocalTime.of(17, 30, 12)),
                workDate
        );

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(offDutyClassId)).thenReturn(Optional.empty());
        when(shiftRepository.findById(groupShift.getShiftId())).thenReturn(Optional.of(groupShift));
        when(attendanceGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(new BigDecimal("1.50"), result.get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(new BigDecimal("1.50"), savedRowsRef.get().get(0).getOvertimeHours());
        Assert.assertEquals(
                LocalDateTime.of(workDate, LocalTime.of(17, 30)),
                LocalDateTime.ofInstant(savedRowsRef.get().get(0).getScheduledOffTime().toInstant(), ZoneId.systemDefault())
        );
    }

    @Test
    public void startStatisticsForEmployee_shouldPreferLocalCustomShiftWhenEmployeeHasCustomPlan() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        Long groupId = 1449530106L;
        Long offDutyClassId = 1516625133L;
        HrmAttendanceShift groupShift = buildShift(2001L, "07:00", "15:00");
        String[] shiftTokens = {"1001", "1002", "1003", "1004", "1005", "1006", "1007"};
        shiftTokens[workDate.getDayOfWeek().getValue() - 1] = String.valueOf(groupShift.getShiftId());
        HrmAttendanceGroup group = buildGroup(groupId, "白班组", String.join(",", shiftTokens));

        HrmAttendancePlan onDutyPlan = buildPlan(
                3000L,
                employee.getEmployeeId(),
                offDutyClassId - 1,
                groupId,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(7, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                offDutyClassId,
                groupId,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(15, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(7, 0)),
                LocalDateTime.of(workDate, LocalTime.of(8, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(15, 0)),
                LocalDateTime.of(workDate, LocalTime.of(19, 30)),
                workDate
        );

        tbattendanceuser attendanceUser = buildAttendanceUser(employee.getEmployeeId(), "user-1001", groupId);
        tbplanlist localCustomPlan = buildLocalCustomPlan(9001, workDate, "user-1001", 501L);
        HrmWorkPlanCustomShift customShift = buildCustomShift(501L, "10:00", "18:00", 0);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(offDutyClassId)).thenReturn(Optional.empty());
        when(shiftRepository.findById(groupShift.getShiftId())).thenReturn(Optional.of(groupShift));
        when(attendanceGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(attendanceUserRepository.findFirstByEmpId(1001L)).thenReturn(Optional.of(attendanceUser));
        when(localPlanRepository.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(localCustomPlan));
        when(customShiftRepository.findById(501L)).thenReturn(Optional.of(customShift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(new BigDecimal("1.50"), result.get(0).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(new BigDecimal("1.50"), savedRowsRef.get().get(0).getOvertimeHours());
        Assert.assertEquals(
                LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                LocalDateTime.ofInstant(savedRowsRef.get().get(0).getScheduledOffTime().toInstant(), ZoneId.systemDefault())
        );
    }

    @Test
    public void startStatistics_shouldCountNightShiftWhenStandardShiftCrossesDayAndOffDutyClockAfter3Am() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        HrmAttendanceShift shift = buildShift(2001L, "20:00", "04:00");
        HrmAttendancePlan onDutyPlan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                shift.getShiftId(),
                9001L,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(20, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3002L,
                employee.getEmployeeId(),
                shift.getShiftId(),
                9001L,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0)),
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 10)),
                workDate
        );

        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(shift.getShiftId())).thenReturn(Optional.of(shift));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());
        when(detailRepository.findAllByStatYearAndStatMonthAndEmployeeIdIn(eq(2026), eq(3), anyList()))
                .thenAnswer(invocation -> savedRowsRef.get());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setMonth("2026-03");
        queryBO.setPage(1L);
        queryBO.setLimit(10L);

        BasePage<QueryOvertimeNightStatisticsPageVO> page = service.startStatistics(queryBO);

        Assert.assertEquals(1, page.getList().size());
        Assert.assertEquals(Integer.valueOf(1), page.getList().get(0).getNightShiftCount());
        Assert.assertTrue(savedRowsRef.get().stream().anyMatch(detail ->
                LocalDate.of(2026, 3, 20).equals(LocalDateTime.ofInstant(detail.getWorkDate().toInstant(), ZoneId.systemDefault()).toLocalDate())
                        && Integer.valueOf(1).equals(detail.getNightShiftCount())
        ));
    }

    @Test
    public void startStatisticsForEmployee_shouldCountNightShiftWhenLocalCustomShiftCrossesDayAndOffDutyClockAfter3Am() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        HrmAttendancePlan onDutyPlan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                7001L,
                9001L,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(20, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3002L,
                employee.getEmployeeId(),
                7001L,
                9001L,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0)),
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 10)),
                workDate
        );

        tbattendanceuser attendanceUser = buildAttendanceUser(employee.getEmployeeId(), "user-1001", 9001L);
        tbplanlist localCustomPlan = buildLocalCustomPlan(9001, workDate, "user-1001", 501L);
        HrmWorkPlanCustomShift customShift = buildCustomShift(501L, "20:00", "04:00", 1);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1001L)).thenReturn(Optional.of(attendanceUser));
        when(localPlanRepository.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(localCustomPlan));
        when(customShiftRepository.findById(501L)).thenReturn(Optional.of(customShift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Integer.valueOf(1), result.get(0).getNightShiftCount());
        Assert.assertTrue(result.get(0).getDailyDetails().stream().anyMatch(detail ->
                "2026-03-20".equals(detail.getWorkDate()) && Integer.valueOf(1).equals(detail.getNightShiftCount())
        ));
        Assert.assertTrue(savedRowsRef.get().stream().anyMatch(detail ->
                LocalDate.of(2026, 3, 20).equals(LocalDateTime.ofInstant(detail.getWorkDate().toInstant(), ZoneId.systemDefault()).toLocalDate())
                        && Integer.valueOf(1).equals(detail.getNightShiftCount())
        ));
    }

    @Test
    public void startStatisticsForEmployee_shouldKeepMonthEndCrossDayGroupShiftOnOriginalWorkDate() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 31);
        Long groupId = 9001L;
        HrmAttendanceShift groupShift = buildShift(2001L, "20:00", "04:00");
        String[] shiftTokens = {"", "", "", "", "", "", ""};
        shiftTokens[workDate.getDayOfWeek().getValue() - 1] = String.valueOf(groupShift.getShiftId());
        HrmAttendanceGroup group = buildGroup(groupId, "夜班组", String.join(",", shiftTokens));

        HrmAttendancePlan onDutyPlan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                7000L,
                groupId,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(20, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3002L,
                employee.getEmployeeId(),
                7001L,
                groupId,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0)),
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 10)),
                workDate
        );

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(7001L)).thenReturn(Optional.empty());
        when(shiftRepository.findById(groupShift.getShiftId())).thenReturn(Optional.of(groupShift));
        when(attendanceGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Integer.valueOf(1), result.get(0).getNightShiftCount());
        Assert.assertEquals(1, result.get(0).getDailyDetails().size());
        Assert.assertEquals("2026-03-31", result.get(0).getDailyDetails().get(0).getWorkDate());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(
                LocalDate.of(2026, 3, 31),
                LocalDateTime.ofInstant(savedRowsRef.get().get(0).getWorkDate().toInstant(), ZoneId.systemDefault()).toLocalDate()
        );
        Assert.assertEquals(Integer.valueOf(1), savedRowsRef.get().get(0).getNightShiftCount());
    }

    @Test
    public void startStatisticsForEmployee_shouldKeepMonthEndCrossDayLocalCustomShiftOnOriginalWorkDate() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 31);
        HrmAttendancePlan onDutyPlan = buildPlan(
                3001L,
                employee.getEmployeeId(),
                7001L,
                9001L,
                workDate,
                "OnDuty",
                LocalDateTime.of(workDate, LocalTime.of(20, 0))
        );
        HrmAttendancePlan offDutyPlan = buildPlan(
                3002L,
                employee.getEmployeeId(),
                7001L,
                9001L,
                workDate,
                "OffDuty",
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0))
        );
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                LocalDateTime.of(workDate, LocalTime.of(20, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 0)),
                LocalDateTime.of(workDate.plusDays(1), LocalTime.of(4, 10)),
                workDate
        );

        tbattendanceuser attendanceUser = buildAttendanceUser(employee.getEmployeeId(), "user-1001", 9001L);
        tbplanlist localCustomPlan = buildLocalCustomPlan(9001, workDate, "user-1001", 501L);
        HrmWorkPlanCustomShift customShift = buildCustomShift(501L, "20:00", "04:00", 1);

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceUserRepository.findFirstByEmpId(1001L)).thenReturn(Optional.of(attendanceUser));
        when(localPlanRepository.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(localCustomPlan));
        when(customShiftRepository.findById(501L)).thenReturn(Optional.of(customShift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Collections.emptyList());

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Integer.valueOf(1), result.get(0).getNightShiftCount());
        Assert.assertEquals(1, result.get(0).getDailyDetails().size());
        Assert.assertEquals("2026-03-31", result.get(0).getDailyDetails().get(0).getWorkDate());
        Assert.assertEquals(1, savedRowsRef.get().size());
        Assert.assertEquals(
                LocalDate.of(2026, 3, 31),
                LocalDateTime.ofInstant(savedRowsRef.get().get(0).getWorkDate().toInstant(), ZoneId.systemDefault()).toLocalDate()
        );
        Assert.assertEquals(Integer.valueOf(1), savedRowsRef.get().get(0).getNightShiftCount());
    }

    @Test
    public void startStatisticsForEmployee_shouldReturnFreshCurrentMonthDetailsInsteadOfStalePersistedRows() {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("许泽刚");
        employee.setJobNumber("ZG001");
        employee.setDeptId(10L);
        employee.setIsDel(0);
        markFixedRestProduction(employee);

        HrmDept dept = new HrmDept();
        dept.setDeptId(10L);
        dept.setName("生产部");

        LocalDate workDate = LocalDate.of(2026, 3, 20);
        HrmAttendanceShift shift = buildShift(2001L, "09:00", "18:00");
        HrmAttendancePlan plan = buildPlan(3001L, employee.getEmployeeId(), shift.getShiftId(), 9001L, workDate, "OffDuty",
                LocalDateTime.of(workDate, LocalTime.of(18, 0)));
        HrmAttendanceClock onDutyClock = buildClock(
                4001L,
                employee.getEmployeeId(),
                1,
                LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                workDate
        );
        HrmAttendanceClock offDutyClock = buildClock(
                4002L,
                employee.getEmployeeId(),
                2,
                LocalDateTime.of(workDate, LocalTime.of(18, 0)),
                LocalDateTime.of(workDate, LocalTime.of(19, 30)),
                workDate
        );

        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(10L))).thenReturn(Collections.singletonList(dept));
        when(planRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyClock, offDutyClock));
        when(attendanceDetailRepository.findAllByEmpIdAndWorkDateBetween(eq(1001L), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(shiftRepository.findById(2001L)).thenReturn(Optional.of(shift));
        when(detailRepository.findAllByEmployeeIdOrderByWorkDateDesc(1001L)).thenReturn(Arrays.asList(
                buildDetail(1001L, "许泽刚", "ZG001", 10L, "生产部", LocalDate.of(2026, 3, 20), "0.00", 0),
                buildDetail(1001L, "许泽刚", "ZG001", 10L, "生产部", LocalDate.of(2026, 2, 28), "1.00", 0)
        ));

        AtomicReference<List<HrmOvertimeNightStatisticsDetail>> savedRowsRef = new AtomicReference<>(Collections.emptyList());
        doAnswer(invocation -> {
            List<HrmOvertimeNightStatisticsDetail> savedRows = new ArrayList<>(invocation.getArgument(0));
            savedRowsRef.set(savedRows);
            return savedRows;
        }).when(detailRepository).saveAll(anyList());

        QueryOvertimeNightStatisticsPageBO queryBO = new QueryOvertimeNightStatisticsPageBO();
        queryBO.setEmployeeId(1001L);
        queryBO.setMonth("2026-03");

        List<EmployeeOvertimeNightMonthlyDetailVO> result = service.startStatisticsForEmployee(queryBO);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("2026-03", result.get(0).getMonth());
        Assert.assertEquals(new BigDecimal("2.50"), result.get(0).getOvertimeHours());
        Assert.assertEquals("2026-02", result.get(1).getMonth());
        Assert.assertEquals(new BigDecimal("1.00"), result.get(1).getOvertimeHours());
        Assert.assertEquals(1, savedRowsRef.get().size());
        verify(detailRepository, atLeastOnce()).flush();
    }

    private HrmOvertimeNightStatisticsDetail buildDetail(Long employeeId,
                                                         String employeeName,
                                                         String jobNumber,
                                                         Long deptId,
                                                         String deptName,
                                                         LocalDate workDate,
                                                         String overtimeHours,
                                                         Integer nightShiftCount) {
        HrmOvertimeNightStatisticsDetail detail = new HrmOvertimeNightStatisticsDetail();
        detail.setEmployeeId(employeeId);
        detail.setEmployeeName(employeeName);
        detail.setJobNumber(jobNumber);
        detail.setDeptId(deptId);
        detail.setDeptName(deptName);
        detail.setStatYear(workDate.getYear());
        detail.setStatMonth(workDate.getMonthValue());
        detail.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        detail.setScheduledOffTime(Date.from(LocalDateTime.of(workDate, java.time.LocalTime.of(18, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        detail.setActualOffTime(Date.from(LocalDateTime.of(workDate, java.time.LocalTime.of(20, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        detail.setOvertimeHours(new BigDecimal(overtimeHours));
        detail.setNightShiftCount(nightShiftCount);
        return detail;
    }

    private void markFixedRestProduction(HrmEmployee employee) {
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
    }

    private HrmEmployee fixedRestProductionEmployee(Long employeeId, String employeeName, String jobNumber, Long deptId) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(employeeName);
        employee.setJobNumber(jobNumber);
        employee.setDeptId(deptId);
        employee.setIsDel(0);
        markFixedRestProduction(employee);
        return employee;
    }

    private HrmAttendancePlan buildPlan(Long planId, Long employeeId, Long classId, LocalDate workDate) {
        return buildPlan(planId, employeeId, classId, null, workDate, null, null);
    }

    private HrmAttendancePlan buildPlan(Long planId,
                                        Long employeeId,
                                        Long classId,
                                        Long groupId,
                                        LocalDate workDate,
                                        String checkType,
                                        LocalDateTime planCheckTime) {
        HrmAttendancePlan plan = new HrmAttendancePlan();
        plan.setPlanId(planId);
        plan.setEmpId(employeeId);
        plan.setClassId(classId);
        plan.setGroupId(groupId);
        plan.setCheckType(checkType);
        plan.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        if (planCheckTime != null) {
            plan.setPlanCheckTime(Date.from(planCheckTime.atZone(ZoneId.systemDefault()).toInstant()));
        }
        return plan;
    }

    private HrmAttendanceClock buildClock(Long clockId,
                                          Long employeeId,
                                          LocalDateTime attendanceTime,
                                          LocalDateTime clockTime,
                                          LocalDate workDate) {
        return buildClock(clockId, employeeId, 2, attendanceTime, clockTime, workDate);
    }

    private HrmAttendanceClock buildClock(Long clockId,
                                          Long employeeId,
                                          Integer clockType,
                                          LocalDateTime attendanceTime,
                                          LocalDateTime clockTime,
                                          LocalDate workDate) {
        HrmAttendanceClock clock = new HrmAttendanceClock();
        clock.setClockId(clockId);
        clock.setClockEmployeeId(employeeId);
        clock.setClockType(clockType);
        clock.setAttendanceTime(Date.from(attendanceTime.atZone(ZoneId.systemDefault()).toInstant()));
        clock.setClockTime(Date.from(clockTime.atZone(ZoneId.systemDefault()).toInstant()));
        clock.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return clock;
    }

    private List<HrmAttendanceClock> buildLifenghuangActualAttendanceClocks(Long employeeId) {
        List<HrmAttendanceClock> clocks = new ArrayList<>();
        for (int day = 1; day <= 24; day++) {
            LocalDate workDate = LocalDate.of(2026, 3, day);
            clocks.add(buildClock(4100L + day, employeeId, 1,
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(9, 0)), workDate));
            clocks.add(buildClock(4200L + day, employeeId, 2,
                    LocalDateTime.of(workDate, LocalTime.of(17, 0)),
                    LocalDateTime.of(workDate, LocalTime.of(17, 0)), workDate));
        }
        LocalDate shortWorkDate = LocalDate.of(2026, 3, 25);
        clocks.add(buildClock(4301L, employeeId, 1,
                LocalDateTime.of(shortWorkDate, LocalTime.of(9, 0)),
                LocalDateTime.of(shortWorkDate, LocalTime.of(9, 0)), shortWorkDate));
        clocks.add(buildClock(4302L, employeeId, 2,
                LocalDateTime.of(shortWorkDate, LocalTime.of(13, 0)),
                LocalDateTime.of(shortWorkDate, LocalTime.of(13, 0)), shortWorkDate));
        return clocks;
    }

    private tbattendancedetail buildAttendanceDetailRecord(Long id,
                                                           Long employeeId,
                                                           String checkType,
                                                           LocalDateTime planCheckTime,
                                                           LocalDateTime userCheckTime,
                                                           LocalDate workDate) {
        tbattendancedetail detail = new tbattendancedetail();
        detail.setId(id);
        detail.setEmpId(employeeId);
        detail.setCheckType(checkType);
        detail.setPlanCheckTime(Date.from(planCheckTime.atZone(ZoneId.systemDefault()).toInstant()));
        detail.setUserCheckTime(Date.from(userCheckTime.atZone(ZoneId.systemDefault()).toInstant()));
        detail.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return detail;
    }

    private tbattendanceuser buildAttendanceUser(Long employeeId, String userId, Long groupId) {
        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(employeeId);
        user.setUserId(userId);
        user.setGroupId(groupId);
        return user;
    }

    private WorkweekDayCalendarVO buildWorkweekDay(String workDate, Integer dayOfWeek, String sourceType) {
        WorkweekDayCalendarVO day = new WorkweekDayCalendarVO();
        day.setWorkDate(workDate);
        day.setDayOfWeek(dayOfWeek);
        day.setSourceType(sourceType);
        return day;
    }

    private tbplanlist buildLocalCustomPlan(Integer id, LocalDate workDate, String userId, Long customShiftId) {
        tbplanlist plan = new tbplanlist();
        plan.setId(id);
        plan.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        plan.setUserId(userId);
        plan.setShiftType("custom");
        plan.setCustomShiftId(customShiftId);
        return plan;
    }

    private HrmWorkPlanCustomShift buildCustomShift(Long id, String start1, String end1, Integer crossDay) {
        HrmWorkPlanCustomShift shift = new HrmWorkPlanCustomShift();
        shift.setId(id);
        shift.setStart1(start1);
        shift.setEnd1(end1);
        shift.setCrossDay(crossDay);
        return shift;
    }

    private HrmAttendanceShift buildShift(Long shiftId, String start1, String end1) {
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(shiftId);
        shift.setStart1(start1);
        shift.setEnd1(end1);
        return shift;
    }

    private HrmAttendanceGroup buildGroup(Long groupId, String name, String shiftSetting) {
        HrmAttendanceGroup group = new HrmAttendanceGroup();
        group.setAttendanceGroupId(groupId);
        group.setName(name);
        group.setShiftSetting(shiftSetting);
        return group;
    }

    private HrmAttendanceHistoryShift buildHistoryShift(Integer shiftId, String start1, String end1) {
        HrmAttendanceHistoryShift shift = new HrmAttendanceHistoryShift();
        shift.setShiftId(shiftId);
        shift.setStart1(start1);
        shift.setEnd1(end1);
        return shift;
    }

    private HrmAttendanceDateShift buildDateShift(Long employeeId, LocalDate workDate, Long shiftId, String start1, String end1) {
        HrmAttendanceDateShift dateShift = new HrmAttendanceDateShift();
        dateShift.setEmployeeId(employeeId);
        dateShift.setUserShiftTime(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dateShift.setShiftId(shiftId);
        dateShift.setStart1(start1);
        dateShift.setEnd1(end1);
        dateShift.setEffectTime(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return dateShift;
    }

    private HrmEmployeeOverTimeRecord buildOvertimeRecord(Long employeeId, LocalDate workDate, String hours) {
        HrmEmployeeOverTimeRecord record = new HrmEmployeeOverTimeRecord();
        record.setEmployeeId(employeeId);
        record.setAttendanceTime(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        record.setOverTimeStartTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(18, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        record.setOverTimeEndTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(18, 0))
                .plusMinutes(new BigDecimal(hours).multiply(new BigDecimal("60")).longValue())
                .atZone(ZoneId.systemDefault()).toInstant()));
        record.setOverTimes(new BigDecimal(hours).doubleValue());
        return record;
    }

    private tbattendanceapprove buildAttendanceApprove(String id,
                                                       String userId,
                                                       LocalDate workDate,
                                                       String duration,
                                                       String durationUnit) {
        tbattendanceapprove approve = new tbattendanceapprove();
        approve.setId(id);
        approve.setUserId(userId);
        approve.setTagName("加班");
        approve.setBizType(1L);
        approve.setDuration(duration);
        approve.setDurationUnit(durationUnit);
        approve.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        approve.setBeginTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(18, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        approve.setEndTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(18, 0))
                .plusMinutes(new BigDecimal(duration).multiply(new BigDecimal("60")).longValue())
                .atZone(ZoneId.systemDefault()).toInstant()));
        return approve;
    }

    private void setApprovalStatisticsStatus(tbattendanceapprove approval, String statisticsStatus) throws Exception {
        Method method = tbattendanceapprove.class.getMethod("setStatisticsStatus", String.class);
        method.invoke(approval, statisticsStatus);
    }

    private tbattendanceapprove buildCompensatoryLeaveApprove(String id,
                                                              String userId,
                                                              LocalDate workDate,
                                                              String duration,
                                                              String durationUnit) {
        tbattendanceapprove approve = new tbattendanceapprove();
        approve.setId(id);
        approve.setUserId(userId);
        approve.setTagName("请假");
        approve.setSubType("调休");
        approve.setBizType(3L);
        approve.setDuration(duration);
        approve.setDurationUnit(durationUnit);
        approve.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        approve.setBeginTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(13, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        approve.setEndTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(17, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        return approve;
    }

    private tbattendanceapprove buildLeaveApprove(String id,
                                                  String userId,
                                                  LocalDate workDate,
                                                  String subType,
                                                  String duration,
                                                  String durationUnit) {
        tbattendanceapprove approve = new tbattendanceapprove();
        approve.setId(id);
        approve.setUserId(userId);
        approve.setTagName(subType);
        approve.setSubType(subType);
        approve.setBizType(3L);
        approve.setDuration(duration);
        approve.setDurationUnit(durationUnit);
        approve.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        approve.setBeginTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(9, 0))
                .atZone(ZoneId.systemDefault()).toInstant()));
        approve.setEndTime(Date.from(LocalDateTime.of(workDate, LocalTime.of(9, 0))
                .plusMinutes(new BigDecimal(duration).multiply(new BigDecimal("60")).longValue())
                .atZone(ZoneId.systemDefault()).toInstant()));
        return approve;
    }

    private tbattendanceapprove buildLeaveApproveWithTimeRange(String id,
                                                               String userId,
                                                               LocalDate workDate,
                                                               String subType,
                                                               String duration,
                                                               String durationUnit,
                                                               LocalTime beginTime,
                                                               LocalTime endTime) {
        tbattendanceapprove approve = buildLeaveApprove(id, userId, workDate, subType, duration, durationUnit);
        approve.setBeginTime(Date.from(LocalDateTime.of(workDate, beginTime)
                .atZone(ZoneId.systemDefault()).toInstant()));
        approve.setEndTime(Date.from(LocalDateTime.of(workDate, endTime)
                .atZone(ZoneId.systemDefault()).toInstant()));
        return approve;
    }

    private HrmOvertimeNightStatisticsDetail buildPersistedDetail(Long employeeId,
                                                                  String employeeName,
                                                                  String jobNumber,
                                                                  LocalDate workDate) {
        HrmOvertimeNightStatisticsDetail detail = new HrmOvertimeNightStatisticsDetail();
        detail.setDetailId(900001L);
        detail.setStatYear(workDate.getYear());
        detail.setStatMonth(workDate.getMonthValue());
        detail.setEmployeeId(employeeId);
        detail.setEmployeeName(employeeName);
        detail.setJobNumber(jobNumber);
        detail.setWorkDate(Date.from(workDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        detail.setOvertimeHours(BigDecimal.ZERO.setScale(2));
        detail.setNightShiftCount(0);
        return detail;
    }

}
