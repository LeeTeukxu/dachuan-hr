package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.QueryAttendanceDailyDetailBO;
import com.tianye.hrsystem.entity.bo.QueryAttendanceEmpMonthRecordBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceClock;
import com.tianye.hrsystem.entity.vo.AttendEmpOverViewVO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceEmpMonthDetailVO;
import com.tianye.hrsystem.entity.vo.QueryEmployeeAttendanceVO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceEmpDetailVO;
import com.tianye.hrsystem.entity.po.HrmAttendanceShift;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.service.IHrmAttendanceGroupService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeLeaveRecordService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.tianye.hrsystem.enums.ClockType.GET_OFF;
import static com.tianye.hrsystem.enums.ClockType.GO_TO;
import static com.tianye.hrsystem.enums.ShiftTypeEnum.COMMON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmAttendanceClockServiceImplTest {

    @InjectMocks
    private HrmAttendanceClockServiceImpl attendanceClockService;

    @Mock
    private IHrmEmployeeService employeeService;

    @Mock
    private IHrmEmployeeLeaveRecordService leaveRecordService;

    @Mock
    private IHrmAttendanceGroupService attendanceGroupService;

    @Before
    public void setUp() {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("1001");
        CompanyContext.set(info);
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void queryAttendanceEmpMonthDetailPageList_shouldReturnAllClockTimesForSingleDay_whenEmployeeHasMultipleClocks() {
        HrmAttendanceClockServiceImpl spyService = spy(attendanceClockService);
        QueryAttendanceEmpMonthRecordBO query = new QueryAttendanceEmpMonthRecordBO();
        query.setTimes(Arrays.asList(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 15)));

        QueryEmployeeAttendanceVO employeeVO = new QueryEmployeeAttendanceVO();
        employeeVO.setEmployeeId(1001L);
        employeeVO.setEmployeeName("张三");
        BasePage<QueryEmployeeAttendanceVO> employeePage =
                new BasePage<>(1L, 20L, 1L, Collections.singletonList(employeeVO));

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEntryTime(LocalDate.of(2025, 1, 1));
        when(employeeService.getById(1001L)).thenReturn(employee);

        List<HrmAttendanceClock> startClocks = Arrays.asList(
                buildClock(1L, 1001L, GO_TO.getValue(), LocalDateTime.of(2026, 4, 15, 8, 0), 0),
                buildClock(2L, 1001L, GO_TO.getValue(), LocalDateTime.of(2026, 4, 15, 13, 0), 0)
        );
        List<HrmAttendanceClock> endClocks = Arrays.asList(
                buildClock(3L, 1001L, GET_OFF.getValue(), LocalDateTime.of(2026, 4, 15, 12, 0), 0),
                buildClock(4L, 1001L, GET_OFF.getValue(), LocalDateTime.of(2026, 4, 15, 18, 0), 0)
        );

        doReturn(employeePage).when(spyService).queryAttendanceEmpList(eq(query));
        doReturn(startClocks).when(spyService)
                .queryAttendanceClockTimelineList(eq(GO_TO.getValue()), any(), any(), anyList(), isNull());
        doReturn(endClocks).when(spyService)
                .queryAttendanceClockTimelineList(eq(GET_OFF.getValue()), any(), any(), anyList(), isNull());

        BasePage<QueryAttendanceEmpMonthDetailVO> result = spyService.queryAttendanceEmpMonthDetailPageList(query);

        QueryAttendanceEmpMonthDetailVO detailVO = result.getList().get(0);
        Map<String, Object> dateDetail = detailVO.getDateList().get(0);
        String[] times = (String[]) dateDetail.get("time");

        Assert.assertArrayEquals(
                new String[]{"08:00-0", "12:00-0", "13:00-0", "18:00-0"},
                times
        );
    }

    @Test
    public void queryAttendanceEmpMonthDetail_shouldNotHideHistoricalMonthWhenCreateTimeIsBeforeQueryMonth() {
        assertMonthDetailShowsClockWhenEmployeeCreateTime(LocalDateTime.of(2025, 1, 1, 15, 19, 20));
    }

    @Test
    public void queryAttendanceEmpMonthDetail_shouldNotHideHistoricalMonthWhenCreateTimeIsAfterQueryMonth() {
        assertMonthDetailShowsClockWhenEmployeeCreateTime(LocalDateTime.of(2025, 11, 11, 15, 19, 20));
    }

    private void assertMonthDetailShowsClockWhenEmployeeCreateTime(LocalDateTime createTime) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEntryTime(LocalDate.of(2025, 1, 1));
        employee.setCreateTime(createTime);
        when(employeeService.getById(1001L)).thenReturn(employee);
        when(leaveRecordService.queryStartOrEndLeaveRecord(any(), anyLong())).thenReturn(null);

        HrmAttendanceShift shift = buildCommonShift();

        HrmAttendanceClockServiceImpl spyService = spy(attendanceClockService);
        doReturn(Collections.singletonList(buildClock(1L, 1001L, GO_TO.getValue(), LocalDateTime.of(2025, 10, 15, 8, 0), 0)))
                .when(spyService).queryAttendanceClockList(eq(GO_TO.getValue()), any(), any(), anyList(), isNull());
        doReturn(Collections.singletonList(buildClock(2L, 1001L, GET_OFF.getValue(), LocalDateTime.of(2025, 10, 15, 17, 0), 0)))
                .when(spyService).queryAttendanceClockList(eq(GET_OFF.getValue()), any(), any(), anyList(), isNull());
        doReturn(Collections.singletonMap("2025-10-15", shift)).when(spyService).getHrmAttendanceShiftMap(anyList(), any(), eq(1001L));
        doReturn(shift).when(spyService).getHrmAttendanceShift(any(QueryAttendanceDailyDetailBO.class));

        QueryAttendanceEmpDetailVO result = spyService.queryAttendanceEmpMonthDetail(buildMonthDetailQuery());

        Assert.assertFalse("历史月份不应因 createTime 而空白", result.getDateList().isEmpty());
        AttendEmpOverViewVO dayDetail = (AttendEmpOverViewVO) result.getDateList().get(0).get("2025-10-15");
        Assert.assertNotNull(dayDetail);
        Assert.assertEquals("08:00", dayDetail.getStart1());
        Assert.assertEquals("17:00", dayDetail.getEnd1());
    }

    private HrmAttendanceClock buildClock(Long clockId, Long employeeId, Integer clockType,
                                          LocalDateTime clockTime, Integer status) {
        HrmAttendanceClock clock = new HrmAttendanceClock();
        clock.setClockId(clockId);
        clock.setClockEmployeeId(employeeId);
        clock.setClockType(clockType);
        clock.setClockTime(clockTime);
        clock.setAttendanceTime(clockTime);
        clock.setClockStatus(status);
        clock.setIsOutWork(0);
        return clock;
    }

    private com.tianye.hrsystem.entity.bo.QueryAttendanceEmpMonthDetailBO buildMonthDetailQuery() {
        com.tianye.hrsystem.entity.bo.QueryAttendanceEmpMonthDetailBO query = new com.tianye.hrsystem.entity.bo.QueryAttendanceEmpMonthDetailBO();
        query.setEmployeeId(1001L);
        query.setTimes(Arrays.asList(LocalDate.of(2025, 10, 15), LocalDate.of(2025, 10, 15)));
        query.setStatus(-1);
        query.setIncludeToday(1);
        return query;
    }

    private HrmAttendanceShift buildCommonShift() {
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftType(COMMON.getValue());
        shift.setStart1("08:00");
        shift.setEnd1("17:00");
        shift.setAdvanceCard1("07:00");
        shift.setLateCard1("09:00");
        shift.setEarlyCard1("16:00");
        shift.setPostponeCard1("18:00");
        shift.setShiftHours(480);
        return shift;
    }
}
