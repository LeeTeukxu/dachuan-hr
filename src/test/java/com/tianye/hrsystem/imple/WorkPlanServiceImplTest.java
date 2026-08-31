package com.tianye.hrsystem.imple;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentBO;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentsBO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentsVO;
import com.tianye.hrsystem.entity.vo.WorkPlanCustomShiftOptionVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewRowVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitProgressVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitResultVO;
import com.tianye.hrsystem.imple.workplan.ResolvedWorkPlanAssignment;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmAttendanceGroup;
import com.tianye.hrsystem.model.HrmWorkPlanCustomShift;
import com.tianye.hrsystem.mapper.WorkPlanMapper;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.UserObject;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.hrmWorkPlanCustomShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkPlanServiceImplTest {

    @InjectMocks
    private WorkPlanServiceImpl workPlanService;

    @Mock
    private tbPlanListRepository planRep;

    @Mock
    private IAccessToken tokener;

    @Mock
    private WorkPlanMapper workPlanMapper;

    @Mock
    private hrmAttendanceShiftRepository shiftRep;

    @Mock
    private hrmAttendanceGroupRepository groupRep;

    @Mock
    private hrmWorkPlanCustomShiftRepository customShiftRep;

    @Mock
    private StringRedisTemplate redisRep;

    @Mock
    private tbattendanceuserRepository userRep;

    @Mock
    private hrmEmployeeRepository employeeRep;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Before
    public void setUp() throws Exception {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("1001");
        CompanyContext.set(info);

        when(redisRep.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("[]");
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void persistResolvedAssignments_shouldCreateNewRecord_whenLoadedFromLastDateAndWorkDateChanges() throws Exception {
        Date dec1 = dayFormat.parse("2025-12-01");
        Date dec2 = dayFormat.parse("2025-12-02");

        tbplanlist existing = new tbplanlist();
        existing.setId(11);
        existing.setWorkDate(dec1);
        existing.setClassId("2");
        existing.setGroupId("2001");
        existing.setUserId(",");

        tbplanlist incoming = new tbplanlist();
        incoming.setId(11);
        incoming.setWorkDate(dec2);
        incoming.setClassId("2");
        incoming.setGroupId("2001");
        incoming.setUserId(",");
        incoming.setWorkshopName("新车间");

        when(planRep.findById(11)).thenReturn(Optional.of(existing));

        workPlanService.persistResolvedAssignments(Collections.singletonList(
                buildAssignment(incoming, 0, "u1", "2001", "2")));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertNull("加载上次排班后改新日期应新增记录，不能复用旧ID", saved.getId());
        Assert.assertEquals(dec2, saved.getWorkDate());
        Assert.assertEquals("新车间", saved.getWorkshopName());
        Assert.assertEquals("旧日期排班不能被覆盖", dec1, existing.getWorkDate());
    }

    @Test
    public void persistResolvedAssignments_shouldUpdateExistingRecord_whenWorkDateKeepsSame() throws Exception {
        Date dec2 = dayFormat.parse("2025-12-02");

        tbplanlist existing = new tbplanlist();
        existing.setId(22);
        existing.setWorkDate(dec2);
        existing.setClassId("2");
        existing.setGroupId("2001");
        existing.setUserId(",");
        existing.setProductName("old-product");

        tbplanlist incoming = new tbplanlist();
        incoming.setId(22);
        incoming.setWorkDate(dec2);
        incoming.setClassId("3");
        incoming.setGroupId("3001");
        incoming.setUserId(",");
        incoming.setProductName("new-product");
        incoming.setWorkshopName("新车间");

        when(planRep.findById(22)).thenReturn(Optional.of(existing));

        workPlanService.persistResolvedAssignments(Collections.singletonList(
                buildAssignment(incoming, 0, "u1", "3001", "3")));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertSame("同日期编辑应更新原记录", existing, saved);
        Assert.assertEquals(Integer.valueOf(22), saved.getId());
        Assert.assertEquals("3", saved.getClassId());
        Assert.assertEquals("3001", saved.getGroupId());
        Assert.assertEquals("new-product", saved.getProductName());
        Assert.assertEquals("新车间", saved.getWorkshopName());
    }

    @Test
    public void persistResolvedAssignments_shouldReplaceExistingEmployeeDayRecord_whenImportHasNoId() throws Exception {
        Date may1 = dayFormat.parse("2026-05-01");

        tbplanlist existing = new tbplanlist();
        existing.setId(56);
        existing.setWorkDate(may1);
        existing.setGroupId("old-group");
        existing.setUserId("u-li-dong");
        existing.setCustomShiftId(5L);
        existing.setCustomShiftPeriod("day");
        existing.setShiftType("custom");

        tbplanlist incoming = new tbplanlist();
        incoming.setWorkDate(may1);
        incoming.setGroupId("new-group");
        incoming.setUserId("u-li-dong");
        incoming.setCustomShiftId(6L);
        incoming.setCustomShiftPeriod("night");
        incoming.setShiftType("custom");

        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(existing));

        workPlanService.persistResolvedAssignments(Collections.singletonList(
                buildAssignment(incoming, 0, "u-li-dong", "new-group", "6")));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertSame("重复上传同员工同日期应更新旧记录，不能新增第二条", existing, saved);
        Assert.assertEquals(Integer.valueOf(56), saved.getId());
        Assert.assertEquals("new-group", saved.getGroupId());
        Assert.assertEquals(Long.valueOf(6L), saved.getCustomShiftId());
        Assert.assertEquals("night", saved.getCustomShiftPeriod());
        verify(planRep, never()).deleteAll(anyList());
    }

    @Test
    public void persistResolvedAssignments_shouldDeleteExtraEmployeeDayRecords_whenImportHitsExistingDuplicates() throws Exception {
        Date may1 = dayFormat.parse("2026-05-01");

        tbplanlist latestExisting = new tbplanlist();
        latestExisting.setId(57);
        latestExisting.setWorkDate(may1);
        latestExisting.setGroupId("latest-group");
        latestExisting.setUserId("u-li-dong");
        latestExisting.setCustomShiftId(5L);
        latestExisting.setShiftType("custom");

        tbplanlist olderDuplicate = new tbplanlist();
        olderDuplicate.setId(56);
        olderDuplicate.setWorkDate(may1);
        olderDuplicate.setGroupId("old-group");
        olderDuplicate.setUserId("u-li-dong");
        olderDuplicate.setCustomShiftId(5L);
        olderDuplicate.setShiftType("custom");

        tbplanlist incoming = new tbplanlist();
        incoming.setWorkDate(may1);
        incoming.setGroupId("new-group");
        incoming.setUserId("u-li-dong");
        incoming.setCustomShiftId(6L);
        incoming.setCustomShiftPeriod("night");
        incoming.setShiftType("custom");

        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(java.util.Arrays.asList(latestExisting, olderDuplicate));

        workPlanService.persistResolvedAssignments(Collections.singletonList(
                buildAssignment(incoming, 0, "u-li-dong", "new-group", "6")));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertSame("重复上传应保留最新一条同员工同日期记录", latestExisting, saved);
        Assert.assertEquals(Integer.valueOf(57), saved.getId());
        Assert.assertEquals("new-group", saved.getGroupId());

        ArgumentCaptor<List> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).deleteAll(deleteCaptor.capture());
        Assert.assertEquals(1, deleteCaptor.getValue().size());
        Assert.assertSame("较旧的重复记录应被清理", olderDuplicate, deleteCaptor.getValue().get(0));
    }

    @Test
    public void loadBySelectedDate_shouldLoadNearestPreviousDate_whenLoadLastTrue() throws Exception {
        Date selectedDate = dayFormat.parse("2025-12-05");
        Date nearestDate = dayFormat.parse("2025-12-03");

        tbplanlist nearest = new tbplanlist();
        nearest.setWorkDate(nearestDate);

        tbplanlist row = new tbplanlist();
        row.setId(101);
        row.setWorkDate(nearestDate);

        when(planRep.findTopByWorkDateLessThanOrderByWorkDateDesc(any(Date.class))).thenReturn(nearest);
        when(planRep.findAllByWorkDateBetween(any(Date.class), any(Date.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        workPlanService.loadBySelectedDate(selectedDate, true, 20, 0, "createTime", "asc");

        ArgumentCaptor<Date> lessThanCaptor = ArgumentCaptor.forClass(Date.class);
        verify(planRep).findTopByWorkDateLessThanOrderByWorkDateDesc(lessThanCaptor.capture());
        Assert.assertEquals("2025-12-05 00:00:00", dateTimeFormat.format(lessThanCaptor.getValue()));

        ArgumentCaptor<Date> beginCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> endCaptor = ArgumentCaptor.forClass(Date.class);
        verify(planRep).findAllByWorkDateBetween(beginCaptor.capture(), endCaptor.capture(), any(Pageable.class));
        Assert.assertEquals("2025-12-03 00:00:00", dateTimeFormat.format(beginCaptor.getValue()));
        Assert.assertEquals("2025-12-03 23:59:59", dateTimeFormat.format(endCaptor.getValue()));
    }

    @Test
    public void loadBySelectedDate_shouldLoadSelectedDate_whenLoadLastFalse() throws Exception {
        Date selectedDate = dayFormat.parse("2025-12-05");
        tbplanlist row = new tbplanlist();
        row.setId(102);
        row.setWorkDate(selectedDate);

        when(planRep.findAllByWorkDateBetween(any(Date.class), any(Date.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        workPlanService.loadBySelectedDate(selectedDate, false, 20, 0, "createTime", "asc");

        verify(planRep, never()).findTopByWorkDateLessThanOrderByWorkDateDesc(any(Date.class));
        ArgumentCaptor<Date> beginCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> endCaptor = ArgumentCaptor.forClass(Date.class);
        verify(planRep).findAllByWorkDateBetween(beginCaptor.capture(), endCaptor.capture(), any(Pageable.class));
        Assert.assertEquals("2025-12-05 00:00:00", dateTimeFormat.format(beginCaptor.getValue()));
        Assert.assertEquals("2025-12-05 23:59:59", dateTimeFormat.format(endCaptor.getValue()));
    }

    @Test
    public void addAll_shouldReturnRunningTaskAndSuccessProgressAfterAsyncSave() throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        tbplanlist incoming = new tbplanlist();
        incoming.setWorkDate(dayFormat.parse("2025-12-06"));
        incoming.setClassId("3");
        incoming.setGroupId("3001");
        incoming.setUserId("u1");

        doAnswer(invocation -> {
            List<tbplanlist> plans = invocation.getArgument(0);
            spyService.persistResolvedAssignments(Collections.singletonList(
                    buildAssignment(plans.get(0), 0, "u1", "3001", "3")));
            return null;
        }).when(spyService).submitPlans(anyList());

        WorkPlanSubmitResultVO submitResult = spyService.AddAll(Collections.singletonList(incoming));

        Assert.assertNotNull(submitResult.getTaskId());
        Assert.assertEquals("PENDING", submitResult.getStatus());

        verify(planRep, timeout(3000)).saveAll(anyList());
        WorkPlanSubmitProgressVO progressVO = awaitTaskDone(spyService, submitResult.getTaskId(), 3, TimeUnit.SECONDS);
        Assert.assertTrue(progressVO.getDone());
        Assert.assertTrue(progressVO.getSuccess());
        Assert.assertEquals("SUCCESS", progressVO.getStatus());
    }

    @Test
    public void addAll_shouldRetryTenTimesAndHideEnglishFailureReason() throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        tbplanlist incoming = new tbplanlist();
        incoming.setWorkDate(dayFormat.parse("2025-12-07"));
        incoming.setClassId("3");
        incoming.setGroupId("3001");
        incoming.setUserId(",");
        doThrow(new Exception("permission denied"))
                .when(spyService).submitPlans(anyList());
        // 重试退避 stub 为 0，避免指数退避拖慢单测
        doReturn(0L).when(spyService).submitRetryBackoffMillis(anyInt());

        WorkPlanSubmitResultVO submitResult = spyService.AddAll(Collections.singletonList(incoming));
        WorkPlanSubmitProgressVO progressVO = awaitTaskDone(spyService, submitResult.getTaskId(), 5, TimeUnit.SECONDS);

        Assert.assertTrue(progressVO.getDone());
        Assert.assertFalse(progressVO.getSuccess());
        Assert.assertEquals("FAILED", progressVO.getStatus());
        Assert.assertEquals(Integer.valueOf(10), progressVO.getRetryCount());
        Assert.assertFalse(progressVO.getMessage().toLowerCase().contains("permission"));
        Assert.assertTrue(progressVO.getMessage().contains("权限") || progressVO.getMessage().contains("失败"));
        Assert.assertNotNull(progressVO.getErrors());
        Assert.assertFalse(progressVO.getErrors().isEmpty());
        Assert.assertFalse(progressVO.getErrors().get(0).getReason().toLowerCase().contains("permission"));
    }

    @Test
    public void getAllGroupsForDisplay_shouldUseIndependentDisplayCacheKey() throws Exception {
        when(redisRep.hasKey("1001_getAllGroups_display")).thenReturn(true);
        when(valueOperations.get("1001_getAllGroups_display"))
                .thenReturn("[{\"groupId\":2,\"groupName\":\"展示缓存组\"}]");

        Method method = WorkPlanServiceImpl.class.getMethod("getAllGroupsForDisplay");
        @SuppressWarnings("unchecked")
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> result =
                (List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo>) method.invoke(workPlanService);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(2L), result.get(0).getGroupId());
        Assert.assertEquals("展示缓存组", result.get(0).getGroupName());
    }

    @Test
    public void getUsersForDisplay_shouldUseIndependentDisplayCacheKey() throws Exception {
        when(redisRep.hasKey("1001_getAllUsers_display_v3")).thenReturn(true);
        when(valueOperations.get("1001_getAllUsers_display_v3"))
                .thenReturn("[{\"id\":\"u2\",\"name\":\"展示缓存人员\",\"groupId\":\"2002\"}]");

        Method method = WorkPlanServiceImpl.class.getMethod("getUsersForDisplay", String.class);
        @SuppressWarnings("unchecked")
        List<UserObject> result = (List<UserObject>) method.invoke(workPlanService, "1001");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("u2", result.get(0).getId());
        Assert.assertEquals("展示缓存人员", result.get(0).getName());
        Assert.assertEquals("2002", result.get(0).getGroupId());
    }

    @Test
    public void getUsersForDisplay_shouldPreferLocalSnapshotWhenRefreshingDisplayCache() throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        HrmEmployee localEmployee = buildEmployee(303L, "员工表人员", "13800000303", "u3");
        localEmployee.setIsDel(0);
        localEmployee.setEntryStatus(1);

        when(redisRep.hasKey("1001_getAllUsers_display_v3")).thenReturn(false);
        when(employeeRep.findAllByIsDelAndEntryStatusIn(eq(0), anyList()))
                .thenReturn(Collections.singletonList(localEmployee));
        org.mockito.Mockito.lenient().doThrow(new AssertionError("display cache refresh should not depend on dingtalk"))
                .when(spyService).loadUsersFromDingTalk("1001", true);

        List<UserObject> result = spyService.getUsersForDisplay("1001");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("u3", result.get(0).getId());
        Assert.assertEquals("员工表人员", result.get(0).getName());
        Assert.assertEquals(Long.valueOf(303L), result.get(0).getEmployeeId());
        Assert.assertEquals("", result.get(0).getGroupId());
        verify(spyService, never()).loadUsersFromDingTalk("1001", true);
        verify(userRep, never()).findAll();
        verify(valueOperations).set(eq("1001_getAllUsers_display_v3"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void getUsersForDisplay_shouldUseNonDeletedOnboardingStatusesAndDingTalkIdsFromEmployeeRoster()
            throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        HrmEmployee active = buildEmployee(301L, "陈明成", "13800000301", "ding-chen");
        active.setIsDel(0);
        active.setEntryStatus(1);
        HrmEmployee pendingLeave = buildEmployee(302L, "待离职人员", "13800000302", "ding-pending-leave");
        pendingLeave.setIsDel(0);
        pendingLeave.setEntryStatus(3);
        HrmEmployee departed = buildEmployee(303L, "离职人员", "13800000303", "ding-departed");
        departed.setIsDel(0);
        departed.setEntryStatus(4);
        HrmEmployee pendingEntry = buildEmployee(304L, "待入职人员", "13800000304", "ding-pending-entry");
        pendingEntry.setIsDel(0);
        pendingEntry.setEntryStatus(2);
        HrmEmployee deleted = buildEmployee(305L, "已删除人员", "13800000305", "ding-deleted");
        deleted.setIsDel(1);
        deleted.setEntryStatus(1);
        HrmEmployee withoutDingTalkId = buildEmployee(306L, "无钉钉人员", "13800000306", null);
        withoutDingTalkId.setIsDel(0);
        withoutDingTalkId.setEntryStatus(1);

        when(redisRep.hasKey("1001_getAllUsers_display_v3")).thenReturn(false);
        when(employeeRep.findAllByIsDelAndEntryStatusIn(eq(0), eq(java.util.Arrays.asList(1, 3, 4))))
                .thenReturn(java.util.Arrays.asList(active, pendingLeave, departed));

        List<UserObject> result = spyService.getUsersForDisplay("1001");

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("ding-chen", result.get(0).getId());
        Assert.assertEquals("陈明成", result.get(0).getName());
        Assert.assertEquals(Long.valueOf(301L), result.get(0).getEmployeeId());
        Assert.assertEquals("", result.get(0).getGroupId());
        Assert.assertEquals("ding-pending-leave", result.get(1).getId());
        Assert.assertEquals("ding-departed", result.get(2).getId());
        verify(employeeRep).findAllByIsDelAndEntryStatusIn(eq(0), eq(java.util.Arrays.asList(1, 3, 4)));
        verify(userRep, never()).findAll();
    }

    @Test
    public void getUsersForDisplay_shouldUseAttendanceSnapshotIdOrEmployeeIdWhenRosterDingTalkIdIsMissing()
            throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        HrmEmployee withAttendanceSnapshot = buildEmployee(307L, "快照人员", "13800000307", null);
        withAttendanceSnapshot.setIsDel(0);
        withAttendanceSnapshot.setEntryStatus(1);
        HrmEmployee withoutAttendanceSnapshot = buildEmployee(308L, "仅员工表人员", "13800000308", null);
        withoutAttendanceSnapshot.setIsDel(0);
        withoutAttendanceSnapshot.setEntryStatus(4);

        tbattendanceuser attendanceUser = buildAttendanceUser(307L, "local-user-307", "快照人员");
        attendanceUser.setGroupId(null);

        when(redisRep.hasKey("1001_getAllUsers_display_v3")).thenReturn(false);
        when(employeeRep.findAllByIsDelAndEntryStatusIn(eq(0), eq(java.util.Arrays.asList(1, 3, 4))))
                .thenReturn(java.util.Arrays.asList(withAttendanceSnapshot, withoutAttendanceSnapshot));
        when(userRep.findAll()).thenReturn(Collections.singletonList(attendanceUser));

        List<UserObject> result = spyService.getUsersForDisplay("1001");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("local-user-307", result.get(0).getId());
        Assert.assertEquals("快照人员", result.get(0).getName());
        Assert.assertEquals(Long.valueOf(307L), result.get(0).getEmployeeId());
        Assert.assertEquals("", result.get(0).getGroupId());
        Assert.assertEquals("308", result.get(1).getId());
        Assert.assertEquals("仅员工表人员", result.get(1).getName());
        Assert.assertEquals(Long.valueOf(308L), result.get(1).getEmployeeId());
        Assert.assertEquals("", result.get(1).getGroupId());
        verify(userRep).findAll();
    }

    @Test
    public void getAllGroupsForDisplay_shouldReadLocalSnapshotAndCacheForThirtyMinutesOnRefresh() throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        HrmAttendanceGroup group = new HrmAttendanceGroup();
        group.setAttendanceGroupId(10L);
        group.setName("展示缓存组");
        group.setShiftSetting("30");
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(30L);
        shift.setGroupId(10L);
        shift.setShiftName("白班");
        shift.setStart1("08:00:00");
        shift.setEnd1("17:30:00");
        when(redisRep.hasKey("1001_getAllGroups_display")).thenReturn(false);
        when(groupRep.findAll()).thenReturn(Collections.singletonList(group));
        when(shiftRep.findAll()).thenReturn(Collections.singletonList(shift));

        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> result = spyService.getAllGroupsForDisplay();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(10L), result.get(0).getGroupId());
        Assert.assertEquals("展示缓存组", result.get(0).getGroupName());
        Assert.assertEquals(1, result.get(0).getSelectedClass().size());
        Assert.assertEquals(Long.valueOf(30L), result.get(0).getSelectedClass().get(0).getClassId());
        verify(spyService, never()).loadGroupsFromDingTalk("1001");
        verify(valueOperations).set(eq("1001_getAllGroups_display"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void getUsersForDisplay_shouldReturnEmptyLocalSnapshotWithoutDingTalkFallback() throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        when(redisRep.hasKey("1001_getAllUsers_display_v3")).thenReturn(false);
        when(employeeRep.findAllByIsDelAndEntryStatusIn(eq(0), anyList()))
                .thenReturn(Collections.emptyList());

        List<UserObject> result = spyService.getUsersForDisplay("1001");

        Assert.assertTrue(result.isEmpty());
        verify(spyService, never()).loadUsersFromDingTalk("1001", true);
        verify(userRep, never()).findAll();
        verify(valueOperations).set(eq("1001_getAllUsers_display_v3"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void queryEmployeeDayShift_shouldExposeProductPositionAndWorkshopForEditDialog() throws Exception {
        Date workDate = dayFormat.parse("2026-08-22");
        tbattendanceuser attendanceUser = buildAttendanceUser(108L, "u8", "产品岗位员工");
        attendanceUser.setGroupId(3008L);

        tbplanlist plan = new tbplanlist();
        plan.setId(3001);
        plan.setWorkDate(workDate);
        plan.setUserId("u8");
        plan.setGroupId("3008");
        plan.setProductName("椰子饼");
        plan.setLinkName("装盒");
        plan.setShiftType("custom");
        plan.setCustomStart("08:00");
        plan.setCustomEnd("17:00");
        plan.setCustomShiftPeriod("day");

        HrmAttendanceGroup group = new HrmAttendanceGroup();
        group.setAttendanceGroupId(3008L);
        group.setName("包装车间");

        when(userRep.findFirstByEmpId(108L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));
        when(groupRep.findById(3008L)).thenReturn(Optional.of(group));

        WorkPlanEmployeeDayShiftVO result = workPlanService.queryEmployeeDayShift(108L, workDate);

        Assert.assertEquals("椰子饼", result.getProductName());
        Assert.assertEquals("装盒", result.getPositionName());
        Assert.assertEquals("包装车间", result.getWorkshopName());
    }

    @Test
    public void queryEmployeeDayShift_shouldPreferPlanWorkshopNameOverAttendanceGroupName() throws Exception {
        Date workDate = dayFormat.parse("2026-08-22");
        tbattendanceuser attendanceUser = buildAttendanceUser(118L, "u18", "产品岗位员工");
        attendanceUser.setGroupId(3018L);

        tbplanlist plan = new tbplanlist();
        plan.setId(3011);
        plan.setWorkDate(workDate);
        plan.setUserId("u18");
        plan.setGroupId("3018");
        plan.setProductName("椰子饼");
        plan.setLinkName("装盒");
        plan.setWorkshopName("包装一车间");
        plan.setShiftType("custom");
        plan.setCustomStart("08:00");
        plan.setCustomEnd("17:00");
        plan.setCustomShiftPeriod("day");

        when(userRep.findFirstByEmpId(118L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));

        WorkPlanEmployeeDayShiftVO result = workPlanService.queryEmployeeDayShift(118L, workDate);

        Assert.assertEquals("包装一车间", result.getWorkshopName());
    }

    @Test
    public void queryEmployeeDayAssignments_shouldReturnAllLocalProductPositionAssignments() throws Exception {
        Date workDate = dayFormat.parse("2026-08-24");
        tbattendanceuser attendanceUser = buildAttendanceUser(109L, "u9", "多岗位员工");
        attendanceUser.setGroupId(3009L);

        tbplanlist firstPlan = new tbplanlist();
        firstPlan.setId(3101);
        firstPlan.setWorkDate(workDate);
        firstPlan.setUserId("u9");
        firstPlan.setGroupId("3009");
        firstPlan.setProductName("椰子饼");
        firstPlan.setLinkName("装盒");
        firstPlan.setWorkshopName("包装一车间");
        firstPlan.setShiftType("custom");
        firstPlan.setCustomStart("08:00");
        firstPlan.setCustomEnd("17:00");
        firstPlan.setCustomShiftPeriod("day");
        firstPlan.setCustomContinuousShift(false);

        tbplanlist secondPlan = new tbplanlist();
        secondPlan.setId(3102);
        secondPlan.setWorkDate(workDate);
        secondPlan.setUserId("u9,u-other");
        secondPlan.setGroupId("3009");
        secondPlan.setProductName("椰汁");
        secondPlan.setLinkName("灌装");
        secondPlan.setWorkshopName("包装二车间");
        secondPlan.setShiftType("custom");
        secondPlan.setCustomStart("19:00");
        secondPlan.setCustomEnd("07:00");
        secondPlan.setCustomShiftPeriod("night");
        secondPlan.setCustomContinuousShift(false);
        secondPlan.setCustomCrossDay(true);

        when(userRep.findFirstByEmpId(109L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(java.util.Arrays.asList(secondPlan, firstPlan));

        WorkPlanEmployeeDayAssignmentsVO result = workPlanService.queryEmployeeDayAssignments(109L, workDate);

        Assert.assertEquals(Long.valueOf(109L), result.getEmployeeId());
        Assert.assertEquals("u9", result.getUserId());
        Assert.assertEquals("work", result.getDayStatus());
        Assert.assertEquals("包装二车间", result.getWorkshopName());
        Assert.assertEquals(2, result.getAssignments().size());
        WorkPlanEmployeeDayAssignmentVO nightAssignment = result.getAssignments().get(0);
        Assert.assertEquals(Integer.valueOf(3102), nightAssignment.getPlanId());
        Assert.assertEquals("椰汁", nightAssignment.getProductName());
        Assert.assertEquals("灌装", nightAssignment.getPositionName());
        Assert.assertEquals("包装二车间", nightAssignment.getWorkshopName());
        Assert.assertEquals("custom", nightAssignment.getShiftType());
        Assert.assertEquals("19:00", nightAssignment.getCustomStart());
        Assert.assertEquals("07:00", nightAssignment.getCustomEnd());
        Assert.assertEquals("night", nightAssignment.getCustomShiftPeriod());
        Assert.assertTrue(nightAssignment.getCustomCrossDay());
        WorkPlanEmployeeDayAssignmentVO dayAssignment = result.getAssignments().get(1);
        Assert.assertEquals("椰子饼", dayAssignment.getProductName());
        Assert.assertEquals("装盒", dayAssignment.getPositionName());
        Assert.assertEquals("包装一车间", dayAssignment.getWorkshopName());
    }

    @Test
    public void getUsers_shouldCacheForTenMinutesOnRefresh() throws Exception {
        WorkPlanServiceImpl spyService = spy(workPlanService);
        UserObject user = new UserObject();
        user.setId("u1");
        user.setName("提交缓存人员");
        user.setGroupId("2001");
        when(redisRep.hasKey("1001_getAllUsers")).thenReturn(false);
        doReturn(Collections.singletonList(user)).when(spyService).loadUsersFromDingTalk("1001", false);

        spyService.getUsers("1001");

        verify(valueOperations).set(eq("1001_getAllUsers"), anyString(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void buildLocalCustomAssignments_shouldReuseExistingCustomShift_whenTimeMatches() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-10"));
        plan.setShiftType("custom");
        plan.setCustomStart("08:30");
        plan.setCustomEnd("17:30");
        plan.setUserId("u1,u2");

        HrmWorkPlanCustomShift existingShift = new HrmWorkPlanCustomShift();
        existingShift.setId(9001L);
        existingShift.setStart1("08:30");
        existingShift.setEnd1("17:30");
        existingShift.setCrossDay(0);

        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc("08:30", "17:30", 0, "day"))
                .thenReturn(Optional.of(existingShift));

        Method method = WorkPlanServiceImpl.class.getDeclaredMethod("buildLocalCustomAssignments", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ResolvedWorkPlanAssignment> assignments =
                (List<ResolvedWorkPlanAssignment>) method.invoke(workPlanService, Collections.singletonList(plan));

        Assert.assertEquals(2, assignments.size());
        Assert.assertEquals("9001", assignments.get(0).getResolvedShiftId());
        Assert.assertEquals("9001", assignments.get(1).getResolvedShiftId());
        verify(customShiftRep, never()).save(any(HrmWorkPlanCustomShift.class));
        verify(tokener, never()).Refresh(anyString());
    }

    @Test
    public void buildLocalCustomAssignments_shouldPersistOpenEndedCustomShift_whenEndTimeMissing() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-10"));
        plan.setShiftType("custom");
        plan.setCustomStart("08:00");
        plan.setCustomEnd(null);
        plan.setUserId("u-open");

        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc("08:00", "", 0, "day"))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(9100L);
            return shift;
        });

        Method method = WorkPlanServiceImpl.class.getDeclaredMethod("buildLocalCustomAssignments", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ResolvedWorkPlanAssignment> assignments =
                (List<ResolvedWorkPlanAssignment>) method.invoke(workPlanService, Collections.singletonList(plan));

        Assert.assertEquals(1, assignments.size());
        Assert.assertEquals("9100", assignments.get(0).getResolvedShiftId());
        ArgumentCaptor<HrmWorkPlanCustomShift> saveCaptor = ArgumentCaptor.forClass(HrmWorkPlanCustomShift.class);
        verify(customShiftRep).save(saveCaptor.capture());
        Assert.assertEquals("08:00", saveCaptor.getValue().getStart1());
        Assert.assertEquals("", saveCaptor.getValue().getEnd1());
        Assert.assertEquals(Integer.valueOf(0), saveCaptor.getValue().getCrossDay());
        Assert.assertEquals(Integer.valueOf(0), saveCaptor.getValue().getShiftHours());
    }

    @Test
    public void buildLocalCustomAssignments_shouldApplyEmployeeContinuousShiftPerUser() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-11"));
        plan.setGroupId("2001");
        plan.setShiftType("custom");
        plan.setCustomStart("08:00");
        plan.setCustomEnd("17:00");
        plan.setCustomShiftPeriod("day");
        plan.setCustomContinuousShift(false);
        plan.setUserId("u-continuous,u-normal");

        tbattendanceuser continuousUser = buildAttendanceUser(201L, "u-continuous", "连班员工");
        tbattendanceuser normalUser = buildAttendanceUser(202L, "u-normal", "普通员工");
        HrmEmployee continuousEmployee = buildEmployee(201L, "连班员工", "13800000201", "u-continuous");
        continuousEmployee.setIsContinuousShift(1);
        HrmEmployee normalEmployee = buildEmployee(202L, "普通员工", "13800000202", "u-normal");
        normalEmployee.setIsContinuousShift(2);

        when(userRep.findAllByUserIdIn(java.util.Arrays.asList("u-continuous", "u-normal")))
                .thenReturn(java.util.Arrays.asList(continuousUser, normalUser));
        when(employeeRep.findAllByEmployeeIdIn(java.util.Arrays.asList(201L, 202L)))
                .thenReturn(java.util.Arrays.asList(continuousEmployee, normalEmployee));
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                eq("08:00"), eq("17:00"), eq(0), eq("day"), any(Integer.class)))
                .thenReturn(Optional.empty());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
                eq("08:00"), eq("17:00"), eq(0), eq("day")))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(Integer.valueOf(1).equals(shift.getContinuousShift()) ? 9101L : 9102L);
            return shift;
        });

        Method method = WorkPlanServiceImpl.class.getDeclaredMethod("buildLocalCustomAssignments", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ResolvedWorkPlanAssignment> assignments =
                (List<ResolvedWorkPlanAssignment>) method.invoke(workPlanService, Collections.singletonList(plan));

        Assert.assertEquals(2, assignments.size());
        Assert.assertEquals("u-continuous", assignments.get(0).getUserId());
        Assert.assertEquals("9101", assignments.get(0).getResolvedShiftId());
        Assert.assertEquals(Boolean.TRUE, assignments.get(0).getSourcePlan().getCustomContinuousShift());
        Assert.assertEquals("u-normal", assignments.get(1).getUserId());
        Assert.assertEquals("9102", assignments.get(1).getResolvedShiftId());
        Assert.assertEquals(Boolean.FALSE, assignments.get(1).getSourcePlan().getCustomContinuousShift());
    }

    @Test
    public void buildLocalCustomAssignments_shouldReuseResolvedCustomShiftWhenUsersShareContinuousFlag() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-12"));
        plan.setGroupId("2001");
        plan.setShiftType("custom");
        plan.setCustomStart("08:30");
        plan.setCustomEnd("17:30");
        plan.setCustomShiftPeriod("day");
        plan.setCustomContinuousShift(false);
        plan.setUserId("u-normal-1,u-normal-2");

        tbattendanceuser firstUser = buildAttendanceUser(211L, "u-normal-1", "普通员工1");
        tbattendanceuser secondUser = buildAttendanceUser(212L, "u-normal-2", "普通员工2");
        HrmEmployee firstEmployee = buildEmployee(211L, "普通员工1", "13800000211", "u-normal-1");
        firstEmployee.setIsContinuousShift(2);
        HrmEmployee secondEmployee = buildEmployee(212L, "普通员工2", "13800000212", "u-normal-2");
        secondEmployee.setIsContinuousShift(2);

        when(userRep.findAllByUserIdIn(java.util.Arrays.asList("u-normal-1", "u-normal-2")))
                .thenReturn(java.util.Arrays.asList(firstUser, secondUser));
        when(employeeRep.findAllByEmployeeIdIn(java.util.Arrays.asList(211L, 212L)))
                .thenReturn(java.util.Arrays.asList(firstEmployee, secondEmployee));
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "08:30", "17:30", 0, "day", 0))
                .thenReturn(Optional.empty());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
                "08:30", "17:30", 0, "day"))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(9201L);
            return shift;
        });

        Method method = WorkPlanServiceImpl.class.getDeclaredMethod("buildLocalCustomAssignments", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ResolvedWorkPlanAssignment> assignments =
                (List<ResolvedWorkPlanAssignment>) method.invoke(workPlanService, Collections.singletonList(plan));

        Assert.assertEquals(2, assignments.size());
        Assert.assertEquals("9201", assignments.get(0).getResolvedShiftId());
        Assert.assertEquals("9201", assignments.get(1).getResolvedShiftId());
        verify(customShiftRep, times(1)).save(any(HrmWorkPlanCustomShift.class));
    }

    @Test
    public void buildLocalCustomAssignments_shouldKeepExplicitImportContinuousShiftValue() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-13"));
        plan.setGroupId("2001");
        plan.setShiftType("custom");
        plan.setCustomStart("08:00");
        plan.setCustomEnd("17:00");
        plan.setCustomShiftPeriod("day");
        plan.setCustomContinuousShift(false);
        plan.setCustomContinuousShiftExplicit(true);
        plan.setUserId("u-import");

        tbattendanceuser user = buildAttendanceUser(213L, "u-import", "导入员工");
        HrmEmployee employee = buildEmployee(213L, "导入员工", "13800000213", "u-import");
        employee.setIsContinuousShift(1);

        when(userRep.findAllByUserIdIn(Collections.singletonList("u-import")))
                .thenReturn(Collections.singletonList(user));
        when(employeeRep.findAllByEmployeeIdIn(Collections.singletonList(213L)))
                .thenReturn(Collections.singletonList(employee));
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "08:00", "17:00", 0, "day", 0))
                .thenReturn(Optional.empty());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
                "08:00", "17:00", 0, "day"))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(9301L);
            return shift;
        });

        Method method = WorkPlanServiceImpl.class.getDeclaredMethod("buildLocalCustomAssignments", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ResolvedWorkPlanAssignment> assignments =
                (List<ResolvedWorkPlanAssignment>) method.invoke(workPlanService, Collections.singletonList(plan));

        Assert.assertEquals(1, assignments.size());
        Assert.assertEquals(Boolean.FALSE, assignments.get(0).getSourcePlan().getCustomContinuousShift());
        verify(customShiftRep).findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "08:00", "17:00", 0, "day", 0);
    }

    @Test
    public void previewImportExcel_shouldMatchByNameWhenPhoneBlankEvenWhenSystemHasSameName() throws Exception {
        HrmEmployee unique = buildEmployee(101L, "张三", "13800000001", "u-zhang");
        HrmEmployee sameNameFirst = buildEmployee(102L, "李四", "13800000002", "u-li-1");
        HrmEmployee sameNameSecond = buildEmployee(103L, "李四", "13800000003", "u-li-2");

        when(employeeRep.findAllByEmployeeName("张三"))
                .thenReturn(Collections.singletonList(unique));
        when(employeeRep.findAllByEmployeeName("李四"))
                .thenReturn(java.util.Arrays.asList(sameNameFirst, sameNameSecond));
        when(employeeRep.findAllByEmployeeName("王五"))
                .thenReturn(Collections.singletonList(buildEmployee(104L, "王五", "13800000004", "u-wang")));
        when(userRep.findFirstByEmpId(101L)).thenReturn(Optional.of(buildAttendanceUser(101L, "u-zhang", "张三")));
        when(userRep.findFirstByEmpId(102L)).thenReturn(Optional.of(buildAttendanceUser(102L, "u-li-1", "李四")));
        when(userRep.findFirstByEmpId(104L)).thenReturn(Optional.of(buildAttendanceUser(104L, "u-wang", "王五")));

        MockMultipartFile file = buildImportFile(new String[][]{
                {"2026-06-01", "张三", "", "自定义排班", "8:00-17:30", "白班", "姓名唯一手机号可空"},
                {"2026-06-01", "李四", "13800000002", "自定义排班", "20:00-8:00", "夜班", "同名用手机号"},
                {"2026-06-03", "王五", "", "调休", "", "", "调休无需时间"},
                {"2026-06-02", "李四", "", "自定义排班", "8:00-结束", "白班", "同名未填手机号"}
        });

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(4), preview.getTotalCount());
        Assert.assertEquals(Integer.valueOf(4), preview.getValidCount());
        Assert.assertEquals(Integer.valueOf(0), preview.getErrorCount());

        WorkPlanImportPreviewRowVO uniqueRow = preview.getRows().get(0);
        Assert.assertTrue(uniqueRow.getValid());
        Assert.assertEquals("u-zhang", uniqueRow.getPlan().getUserId());
        Assert.assertEquals("08:00", uniqueRow.getPlan().getCustomStart());
        Assert.assertEquals("17:30", uniqueRow.getPlan().getCustomEnd());
        Assert.assertEquals("day", uniqueRow.getPlan().getCustomShiftPeriod());
        Assert.assertFalse(uniqueRow.getPlan().getCustomCrossDay());

        WorkPlanImportPreviewRowVO sameNameRow = preview.getRows().get(1);
        Assert.assertTrue(sameNameRow.getValid());
        Assert.assertEquals("u-li-1", sameNameRow.getPlan().getUserId());
        Assert.assertEquals("20:00", sameNameRow.getPlan().getCustomStart());
        Assert.assertEquals("08:00", sameNameRow.getPlan().getCustomEnd());
        Assert.assertEquals("night", sameNameRow.getPlan().getCustomShiftPeriod());
        Assert.assertTrue(sameNameRow.getPlan().getCustomCrossDay());

        WorkPlanImportPreviewRowVO restRow = preview.getRows().get(2);
        Assert.assertTrue(restRow.getValid());
        Assert.assertEquals("rest", restRow.getPlan().getShiftType());
        Assert.assertEquals("u-wang", restRow.getPlan().getUserId());
        Assert.assertNull(restRow.getPlan().getCustomStart());
        Assert.assertNull(restRow.getPlan().getCustomEnd());
        Assert.assertNull(restRow.getPlan().getCustomShiftPeriod());

        WorkPlanImportPreviewRowVO sameNameDifferentDateRow = preview.getRows().get(3);
        Assert.assertTrue(sameNameDifferentDateRow.getValid());
        Assert.assertEquals("u-li-1", sameNameDifferentDateRow.getPlan().getUserId());
    }

    @Test
    public void previewImportExcel_shouldMatchSameNameEmployeeByPhoneColumn() throws Exception {
        HrmEmployee first = buildEmployee(201L, "李四", "13800000201", "u-li-first");
        HrmEmployee second = buildEmployee(202L, "李四", "13800000202", "u-li-second");

        when(employeeRep.findAllByEmployeeName("李四")).thenReturn(java.util.Arrays.asList(first, second));
        when(userRep.findFirstByEmpId(202L))
                .thenReturn(Optional.of(buildAttendanceUser(202L, "u-li-second", "李四")));

        MockMultipartFile file = buildImportFileWithHeaders(
                new String[]{"排班日期", "员工", "电话", "排班类型", "排班时间", "白班/夜班", "是否连班", "备注"},
                new String[][]{
                        {"2026-06-04", "李四", "13800000202", "自定义排班", "8:00-17:30", "白班", "否", "姓名电话唯一"}
                });

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(1), preview.getValidCount());
        Assert.assertEquals("u-li-second", preview.getRows().get(0).getPlan().getUserId());
        Assert.assertFalse(preview.getRows().get(0).getPlan().getCustomContinuousShift());
        Assert.assertTrue("导入明确填写是否连班时需保留显式标记",
                preview.getRows().get(0).getPlan().getCustomContinuousShiftExplicit());
    }

    @Test
    public void previewImportExcel_shouldMatchFirstSameNameEmployeeWhenPhoneBlank() throws Exception {
        HrmEmployee first = buildEmployee(211L, "王五", "13800000211", "u-wang-first");
        HrmEmployee second = buildEmployee(212L, "王五", "13800000212", "u-wang-second");

        when(employeeRep.findAllByEmployeeName("王五")).thenReturn(java.util.Arrays.asList(first, second));
        when(userRep.findFirstByEmpId(211L))
                .thenReturn(Optional.of(buildAttendanceUser(211L, "u-wang-first", "王五")));

        MockMultipartFile file = buildImportFileWithHeaders(
                new String[]{"排班日期", "员工", "电话", "排班类型", "排班时间", "白班/夜班", "是否连班", "备注"},
                new String[][]{
                        {"2026-06-05", "王五", "", "自定义排班", "8:00-17:30", "白班", "是", "同名缺电话"}
                });

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(1), preview.getValidCount());
        Assert.assertEquals(Integer.valueOf(0), preview.getErrorCount());
        Assert.assertEquals("u-wang-first", preview.getRows().get(0).getPlan().getUserId());
    }

    @Test
    public void previewImportExcel_shouldApplyEmployeeContinuousShiftWhenContinuousCellBlank() throws Exception {
        HrmEmployee employee = buildEmployee(221L, "赵六", "13800000221", "u-zhao");
        employee.setIsContinuousShift(1);

        when(employeeRep.findAllByEmployeeName("赵六")).thenReturn(Collections.singletonList(employee));
        when(userRep.findFirstByEmpId(221L)).thenReturn(Optional.of(buildAttendanceUser(221L, "u-zhao", "赵六")));

        MockMultipartFile file = buildImportFileWithHeaders(
                new String[]{"排班日期", "员工", "电话", "排班类型", "排班时间", "白班/夜班", "是否连班", "备注"},
                new String[][]{
                        {"2026-06-06", "赵六", "13800000221", "自定义排班", "8:00-17:30", "白班", "", "空连班单元格"}
                });

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(1), preview.getValidCount());
        tbplanlist plan = preview.getRows().get(0).getPlan();
        Assert.assertTrue(plan.getCustomContinuousShift());
        Assert.assertFalse("导入是否连班为空时不应标记为显式值", plan.getCustomContinuousShiftExplicit());
    }

    @Test
    public void previewImportExcel_shouldTreatBlankPhoneSameDateRowsAsDuplicateNameMatches() throws Exception {
        HrmEmployee sameNameFirst = buildEmployee(102L, "李四", "13800000002", "u-li-1");
        HrmEmployee sameNameSecond = buildEmployee(103L, "李四", "13800000003", "u-li-2");

        when(employeeRep.findAllByEmployeeName("李四"))
                .thenReturn(java.util.Arrays.asList(sameNameFirst, sameNameSecond));
        when(userRep.findFirstByEmpId(102L)).thenReturn(Optional.of(buildAttendanceUser(102L, "u-li-1", "李四")));

        MockMultipartFile file = buildImportFile(new String[][]{
                {"2026-06-01", "李四", "", "自定义排班", "8:00-17:30", "白班", "同一天同名未填手机号"},
                {"2026-06-01", "李四", "", "自定义排班", "20:00-8:00", "夜班", "同一天同名未填手机号"},
                {"2026-06-02", "李四", "", "自定义排班", "8:00-结束", "白班", "不同日期单行同名可不填手机号"}
        });

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(3), preview.getTotalCount());
        Assert.assertEquals(Integer.valueOf(1), preview.getValidCount());
        Assert.assertEquals(Integer.valueOf(2), preview.getErrorCount());
        Assert.assertFalse(preview.getRows().get(0).getValid());
        Assert.assertTrue(preview.getRows().get(0).getErrors().get(0).contains("重复"));
        Assert.assertFalse(preview.getRows().get(1).getValid());
        Assert.assertTrue(preview.getRows().get(1).getErrors().get(0).contains("重复"));
        Assert.assertTrue(preview.getRows().get(2).getValid());
        Assert.assertEquals("u-li-1", preview.getRows().get(2).getPlan().getUserId());
    }

    @Test
    public void previewImportExcel_shouldAdaptWorkplanHorizontalDateGroupedXls() throws Exception {
        HrmEmployee employee = buildEmployee(101L, "李小瑞", "13800000001", "u-li-xiao-rui");
        when(employeeRep.findAllByEmployeeName("李小瑞"))
                .thenReturn(Collections.singletonList(employee));
        when(userRep.findFirstByEmpId(101L))
                .thenReturn(Optional.of(buildAttendanceUser(101L, "u-li-xiao-rui", "李小瑞")));

        MockMultipartFile file = buildDesktopWorkplanXls();

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(2), preview.getTotalCount());
        Assert.assertEquals(Integer.valueOf(2), preview.getValidCount());
        Assert.assertEquals(Integer.valueOf(0), preview.getErrorCount());

        WorkPlanImportPreviewRowVO dayRow = preview.getRows().get(0);
        Assert.assertEquals("2026-05-06", dayRow.getWorkDate());
        Assert.assertEquals("李小瑞", dayRow.getEmployeeName());
        Assert.assertEquals("自定义排班", dayRow.getShiftType());
        Assert.assertEquals("15:30-18:00", dayRow.getScheduleText());
        Assert.assertEquals("白班", dayRow.getCustomShiftPeriod());
        Assert.assertEquals(dayFormat.parse("2026-05-06"), dayRow.getPlan().getWorkDate());
        Assert.assertEquals("15:30", dayRow.getPlan().getCustomStart());
        Assert.assertEquals("18:00", dayRow.getPlan().getCustomEnd());
        Assert.assertEquals("day", dayRow.getPlan().getCustomShiftPeriod());
        Assert.assertTrue(dayRow.getPlan().getCustomContinuousShift());

        WorkPlanImportPreviewRowVO restRow = preview.getRows().get(1);
        Assert.assertEquals("2026-05-07", restRow.getWorkDate());
        Assert.assertEquals("休息", restRow.getShiftType());
        Assert.assertEquals("rest", restRow.getPlan().getShiftType());
        Assert.assertEquals("rest", restRow.getPlan().getRestShiftType());
        Assert.assertNull(restRow.getPlan().getCustomStart());
    }

    @Test
    public void previewImportExcel_shouldTreatHorizontalVacationAsRestShift() throws Exception {
        HrmEmployee employee = buildEmployee(105L, "王休假", "13800000105", "u-vacation");
        when(employeeRep.findAllByEmployeeName("王休假"))
                .thenReturn(Collections.singletonList(employee));
        when(userRep.findFirstByEmpId(105L))
                .thenReturn(Optional.of(buildAttendanceUser(105L, "u-vacation", "王休假")));

        MockMultipartFile file = buildDesktopWorkplanXls("王休假", "休假");

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(2), preview.getTotalCount());
        Assert.assertEquals(Integer.valueOf(2), preview.getValidCount());
        Assert.assertEquals(Integer.valueOf(0), preview.getErrorCount());
        WorkPlanImportPreviewRowVO row = preview.getRows().get(1);
        Assert.assertEquals("2026-05-07", row.getWorkDate());
        Assert.assertEquals("休息", row.getShiftType());
        Assert.assertEquals("rest", row.getPlan().getShiftType());
        Assert.assertEquals("rest", row.getPlan().getRestShiftType());
    }

    @Test
    public void createImportTemplateExcel_shouldSaveTemplateWithShiftPeriodColumnToExportFolder() throws Exception {
        File template = workPlanService.createImportTemplateExcel();

        Assert.assertTrue(template.exists());
        Assert.assertTrue(template.getPath().replace(File.separatorChar, '/').endsWith("export/排班导入模板.xlsx"));
        try (Workbook workbook = WorkbookFactory.create(template)) {
            Sheet sheet = workbook.getSheet("排班明细");
            Assert.assertNotNull(sheet);
            Row header = sheet.getRow(0);
            Assert.assertEquals("排班日期", header.getCell(0).getStringCellValue());
            Assert.assertEquals("员工", header.getCell(1).getStringCellValue());
            Assert.assertEquals("电话", header.getCell(2).getStringCellValue());
            Assert.assertEquals("排班类型", header.getCell(3).getStringCellValue());
            Assert.assertEquals("排班时间", header.getCell(4).getStringCellValue());
            Assert.assertEquals("白班/夜班", header.getCell(5).getStringCellValue());
            Assert.assertEquals("是否连班", header.getCell(6).getStringCellValue());
            Assert.assertEquals("备注", header.getCell(7).getStringCellValue());
        }
    }

    @Test
    public void previewImportExcel_shouldRejectDuplicateEmployeeAndDateRows() throws Exception {
        HrmEmployee employee = buildEmployee(101L, "张三", "13800000001", "u-zhang");
        when(employeeRep.findAllByEmployeeName("张三"))
                .thenReturn(Collections.singletonList(employee));
        when(userRep.findFirstByEmpId(101L)).thenReturn(Optional.of(buildAttendanceUser(101L, "u-zhang", "张三")));

        MockMultipartFile file = buildImportFile(new String[][]{
                {"2026-06-01", "张三", "", "自定义排班", "8:00-17:30", "白班", "第一条"},
                {"2026-06-01", "张三", "", "调休", "", "", "重复条"}
        });

        WorkPlanImportPreviewVO preview = workPlanService.previewImportExcel(file);

        Assert.assertEquals(Integer.valueOf(2), preview.getTotalCount());
        Assert.assertEquals(Integer.valueOf(0), preview.getValidCount());
        Assert.assertEquals(Integer.valueOf(2), preview.getErrorCount());
        Assert.assertTrue(preview.getRows().get(0).getErrors().get(0).contains("重复"));
        Assert.assertTrue(preview.getRows().get(1).getErrors().get(0).contains("重复"));
    }

    @Test
    public void fillCustomShiftMeta_shouldReadFromCustomShiftTable_whenRedisMetaMissing() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setId(101);
        plan.setCustomShiftId(11L);
        plan.setShiftType("custom");

        HrmWorkPlanCustomShift localCustomShift = new HrmWorkPlanCustomShift();
        localCustomShift.setId(11L);
        localCustomShift.setShiftName("自定义班次 21:15-05:45");
        localCustomShift.setStart1("21:15");
        localCustomShift.setEnd1("05:45");
        localCustomShift.setCrossDay(1);

        when(customShiftRep.findById(11L)).thenReturn(Optional.of(localCustomShift));

        workPlanService.fillCustomShiftMeta(Collections.singletonList(plan));

        Assert.assertEquals("custom", plan.getShiftType());
        Assert.assertEquals("21:15", plan.getCustomStart());
        Assert.assertEquals("05:45", plan.getCustomEnd());
        Assert.assertTrue(plan.getCustomCrossDay());
    }

    @Test
    public void submitPlans_shouldPersistCustomPlanLocally_withoutCallingDingTalk() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-11"));
        plan.setShiftType("custom");
        plan.setCustomStart("09:00");
        plan.setCustomEnd("18:00");
        plan.setCustomShiftPeriod("night");
        plan.setUserId("u1");

        HrmWorkPlanCustomShift existingShift = new HrmWorkPlanCustomShift();
        existingShift.setId(21L);
        existingShift.setShiftName("自定义班次 09:00-18:00");
        existingShift.setStart1("09:00");
        existingShift.setEnd1("18:00");
        existingShift.setCrossDay(0);
        existingShift.setShiftPeriod("night");

        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc("09:00", "18:00", 0, "night"))
                .thenReturn(Optional.of(existingShift));
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        workPlanService.submitPlans(Collections.singletonList(plan));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals(Long.valueOf(21L), saved.getCustomShiftId());
        Assert.assertEquals("custom", saved.getShiftType());
        Assert.assertEquals("night", saved.getCustomShiftPeriod());
        Assert.assertNull(saved.getClassId());
        verify(planRep).saveAll(anyList());
        verify(tokener, never()).Refresh(anyString());
        verify(tokener, never()).GetAdminUser(anyString());
    }

    @Test
    public void submitPlans_shouldPersistRestPlanLocally_withoutClassOrDingTalk() throws Exception {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(dayFormat.parse("2025-12-15"));
        plan.setShiftType("rest");
        plan.setUserId("u-rest");

        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        workPlanService.submitPlans(Collections.singletonList(plan));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals("rest", saved.getShiftType());
        Assert.assertEquals("u-rest", saved.getUserId());
        Assert.assertNull(saved.getClassId());
        Assert.assertNull(saved.getCustomShiftId());
        Assert.assertNull(saved.getCustomStart());
        Assert.assertNull(saved.getCustomEnd());
        verify(tokener, never()).Refresh(anyString());
        verify(tokener, never()).GetAdminUser(anyString());
    }

    @Test
    public void submitPlans_shouldPersistRestShiftTypeForAdjustAndRestOptions() throws Exception {
        tbplanlist adjustPlan = new tbplanlist();
        adjustPlan.setWorkDate(dayFormat.parse("2025-12-15"));
        adjustPlan.setShiftType("rest");
        adjustPlan.setRestShiftType("adjust");
        adjustPlan.setUserId("u-adjust");

        tbplanlist restPlan = new tbplanlist();
        restPlan.setWorkDate(dayFormat.parse("2025-12-15"));
        restPlan.setShiftType("rest");
        restPlan.setRestShiftType("rest");
        restPlan.setUserId("u-rest");

        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        workPlanService.submitPlans(java.util.Arrays.asList(adjustPlan, restPlan));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        List savedRows = saveCaptor.getValue();
        Assert.assertEquals("adjust", ((tbplanlist) savedRows.get(0)).getRestShiftType());
        Assert.assertEquals("rest", ((tbplanlist) savedRows.get(1)).getRestShiftType());
        Assert.assertNull(((tbplanlist) savedRows.get(0)).getClassId());
        Assert.assertNull(((tbplanlist) savedRows.get(1)).getClassId());
    }

    @Test
    public void queryCustomShiftOptions_shouldReturnDisplayOptions() throws Exception {
        HrmWorkPlanCustomShift first = new HrmWorkPlanCustomShift();
        first.setId(21L);
        first.setShiftName("自定义班次 08:30-17:30");
        first.setStart1("08:30");
        first.setEnd1("17:30");
        first.setCrossDay(0);

        HrmWorkPlanCustomShift second = new HrmWorkPlanCustomShift();
        second.setId(22L);
        second.setShiftName("自定义班次 21:15-05:45");
        second.setStart1("21:15");
        second.setEnd1("05:45");
        second.setCrossDay(1);
        second.setShiftPeriod("night");

        HrmWorkPlanCustomShift openEnded = new HrmWorkPlanCustomShift();
        openEnded.setId(23L);
        openEnded.setShiftName("自定义班次 08:00~结束");
        openEnded.setStart1("08:00");
        openEnded.setEnd1(null);
        openEnded.setCrossDay(0);

        when(customShiftRep.findAll(Sort.by(Sort.Direction.DESC, "updateTime", "id")))
                .thenReturn(java.util.Arrays.asList(second, first, openEnded));

        List<WorkPlanCustomShiftOptionVO> result = workPlanService.queryCustomShiftOptions();

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(Long.valueOf(22L), result.get(0).getId());
        Assert.assertEquals("21:15", result.get(0).getCustomStart());
        Assert.assertEquals("05:45", result.get(0).getCustomEnd());
        Assert.assertEquals("night", result.get(0).getCustomShiftPeriod());
        Assert.assertTrue(result.get(0).getCustomCrossDay());
        Assert.assertTrue(result.get(0).getDisplayName().contains("次日05:45"));
        Assert.assertEquals(Long.valueOf(23L), result.get(2).getId());
        Assert.assertEquals("08:00", result.get(2).getCustomStart());
        Assert.assertNull(result.get(2).getCustomEnd());
        Assert.assertEquals("自定义班次(08:00~结束)", result.get(2).getDisplayName());
    }

    @Test
    public void buildPersistRows_shouldSplitCustomAssignmentsByResolvedGroup() throws Exception {
        Date workDate = dayFormat.parse("2025-12-08");

        tbplanlist source = new tbplanlist();
        source.setProductName("一车间");
        source.setLinkName("一工段");
        source.setWorkDate(workDate);
        source.setShiftType("custom");
        source.setCustomStart("08:30");
        source.setCustomEnd("17:30");
        source.setCustomCrossDay(false);

        ResolvedWorkPlanAssignment first = new ResolvedWorkPlanAssignment();
        first.setSourcePlan(source);
        first.setResolvedGroupId("1001");
        first.setResolvedShiftId("8001");
        first.setUserId("u1");

        ResolvedWorkPlanAssignment second = new ResolvedWorkPlanAssignment();
        second.setSourcePlan(source);
        second.setResolvedGroupId("1002");
        second.setResolvedShiftId("8002");
        second.setUserId("u2");

        List<tbplanlist> rows = workPlanService.buildPersistRows(java.util.Arrays.asList(first, second));

        Assert.assertEquals(2, rows.size());
        Assert.assertEquals("1001", rows.get(0).getGroupId());
        Assert.assertNull(rows.get(0).getClassId());
        Assert.assertEquals(Long.valueOf(8001L), rows.get(0).getCustomShiftId());
        Assert.assertEquals("u1", rows.get(0).getUserId());
        Assert.assertEquals("1002", rows.get(1).getGroupId());
        Assert.assertNull(rows.get(1).getClassId());
        Assert.assertEquals(Long.valueOf(8002L), rows.get(1).getCustomShiftId());
        Assert.assertEquals("u2", rows.get(1).getUserId());
        Assert.assertEquals("custom", rows.get(0).getShiftType());
        Assert.assertEquals("08:30", rows.get(0).getCustomStart());
    }

    @Test
    public void buildPersistRows_shouldMergeUsersWhenResolvedGroupAndShiftAreSame() throws Exception {
        Date workDate = dayFormat.parse("2025-12-09");

        tbplanlist source = new tbplanlist();
        source.setProductName("二车间");
        source.setLinkName("二工段");
        source.setWorkDate(workDate);
        source.setShiftType("custom");
        source.setCustomStart("09:00");
        source.setCustomEnd("18:00");
        source.setCustomCrossDay(false);

        ResolvedWorkPlanAssignment first = new ResolvedWorkPlanAssignment();
        first.setSourcePlan(source);
        first.setResolvedGroupId("2001");
        first.setResolvedShiftId("9001");
        first.setUserId("u1");

        ResolvedWorkPlanAssignment second = new ResolvedWorkPlanAssignment();
        second.setSourcePlan(source);
        second.setResolvedGroupId("2001");
        second.setResolvedShiftId("9001");
        second.setUserId("u2");

        List<tbplanlist> rows = workPlanService.buildPersistRows(java.util.Arrays.asList(first, second));

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals("2001", rows.get(0).getGroupId());
        Assert.assertNull(rows.get(0).getClassId());
        Assert.assertEquals(Long.valueOf(9001L), rows.get(0).getCustomShiftId());
        Assert.assertEquals("u1,u2", rows.get(0).getUserId());
        Assert.assertEquals("09:00", rows.get(0).getCustomStart());
        Assert.assertEquals("18:00", rows.get(0).getCustomEnd());
    }

    @Test
    public void queryEmployeeDayShift_shouldReturnLocalCustomShift_whenLocalPlanExists() throws Exception {
        Date workDate = dayFormat.parse("2025-12-12");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(101L);
        attendanceUser.setUserId("u1");
        attendanceUser.setGroupId(2001L);

        tbplanlist localPlan = new tbplanlist();
        localPlan.setId(31);
        localPlan.setWorkDate(workDate);
        localPlan.setUserId("u1");
        localPlan.setGroupId("2001");
        localPlan.setShiftType("custom");
        localPlan.setCustomShiftId(901L);

        HrmWorkPlanCustomShift customShift = new HrmWorkPlanCustomShift();
        customShift.setId(901L);
        customShift.setStart1("08:30");
        customShift.setEnd1("17:30");
        customShift.setCrossDay(0);
        customShift.setShiftPeriod("night");

        when(userRep.findFirstByEmpId(101L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(localPlan));
        when(customShiftRep.findById(901L)).thenReturn(Optional.of(customShift));

        WorkPlanEmployeeDayShiftVO result = workPlanService.queryEmployeeDayShift(101L, workDate);

        Assert.assertEquals("local", result.getSource());
        Assert.assertEquals("custom", result.getCurrentShiftType());
        Assert.assertEquals("08:30", result.getCustomStart());
        Assert.assertEquals("17:30", result.getCustomEnd());
        Assert.assertEquals("night", result.getCustomShiftPeriod());
        Assert.assertFalse(Boolean.TRUE.equals(result.getCustomCrossDay()));
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldSplitSharedPlanAndKeepOtherUsers() throws Exception {
        Date workDate = dayFormat.parse("2025-12-13");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(102L);
        attendanceUser.setUserId("u1");
        attendanceUser.setUserName("测试员工");
        attendanceUser.setGroupId(2001L);

        tbplanlist sharedPlan = new tbplanlist();
        sharedPlan.setId(41);
        sharedPlan.setProductName("一车间");
        sharedPlan.setLinkName("一工段");
        sharedPlan.setWorkDate(workDate);
        sharedPlan.setGroupId("2001");
        sharedPlan.setClassId("3");
        sharedPlan.setUserId("u1,u2");

        HrmWorkPlanCustomShift customShift = new HrmWorkPlanCustomShift();
        customShift.setId(902L);
        customShift.setStart1("09:00");
        customShift.setEnd1("18:00");
        customShift.setCrossDay(0);

        when(userRep.findFirstByEmpId(102L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(sharedPlan));
        customShift.setShiftPeriod("night");

        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc("09:00", "18:00", 0, "night"))
                .thenReturn(Optional.of(customShift));
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkPlanEmployeeDayShiftVO result =
                workPlanService.saveEmployeeDayCustomShift(102L, workDate, "09:00", "18:00", "night");

        Assert.assertEquals("custom", result.getCurrentShiftType());
        Assert.assertEquals("09:00", result.getCustomStart());
        Assert.assertEquals("18:00", result.getCustomEnd());
        Assert.assertEquals("night", result.getCustomShiftPeriod());

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep, times(2)).saveAll(saveCaptor.capture());
        List<List> saveBatches = saveCaptor.getAllValues();
        tbplanlist savedShared = (tbplanlist) saveBatches.get(0).get(0);
        tbplanlist savedCustom = (tbplanlist) saveBatches.get(1).get(0);

        Assert.assertEquals("u2", savedShared.getUserId());
        Assert.assertEquals(Integer.valueOf(41), savedShared.getId());
        Assert.assertEquals("u1", savedCustom.getUserId());
        Assert.assertNull(savedCustom.getId());
        Assert.assertEquals(Long.valueOf(902L), savedCustom.getCustomShiftId());
        Assert.assertEquals("custom", savedCustom.getShiftType());
        Assert.assertEquals("night", savedCustom.getCustomShiftPeriod());
        Assert.assertEquals("一车间", savedCustom.getProductName());
        Assert.assertEquals("一工段", savedCustom.getLinkName());
    }

    @Test
    public void saveEmployeeDayAssignments_shouldReplaceEmployeeDayWithMultipleProductPositionRows() throws Exception {
        Date workDate = dayFormat.parse("2026-08-24");

        tbattendanceuser attendanceUser = buildAttendanceUser(109L, "u9", "多岗位员工");
        attendanceUser.setGroupId(3009L);

        tbplanlist sharedPlan = new tbplanlist();
        sharedPlan.setId(3201);
        sharedPlan.setWorkDate(workDate);
        sharedPlan.setGroupId("3009");
        sharedPlan.setClassId("6");
        sharedPlan.setUserId("u9,u-other");
        sharedPlan.setProductName("旧产品");
        sharedPlan.setLinkName("旧共享岗位");
        sharedPlan.setWorkshopName("旧共享车间");

        tbplanlist singlePlan = new tbplanlist();
        singlePlan.setId(3202);
        singlePlan.setWorkDate(workDate);
        singlePlan.setGroupId("3009");
        singlePlan.setUserId("u9");
        singlePlan.setProductName("旧产品");
        singlePlan.setLinkName("旧单人岗位");
        singlePlan.setWorkshopName("旧单人车间");
        singlePlan.setShiftType("custom");
        singlePlan.setCustomStart("07:00");
        singlePlan.setCustomEnd("16:00");

        HrmWorkPlanCustomShift dayShift = new HrmWorkPlanCustomShift();
        dayShift.setId(9309L);
        dayShift.setStart1("08:00");
        dayShift.setEnd1("17:00");
        dayShift.setCrossDay(0);
        dayShift.setShiftPeriod("day");
        dayShift.setContinuousShift(0);
        HrmWorkPlanCustomShift nightShift = new HrmWorkPlanCustomShift();
        nightShift.setId(9310L);
        nightShift.setStart1("19:00");
        nightShift.setEnd1("07:00");
        nightShift.setCrossDay(1);
        nightShift.setShiftPeriod("night");
        nightShift.setContinuousShift(0);

        when(userRep.findFirstByEmpId(109L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(java.util.Arrays.asList(singlePlan, sharedPlan));
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "08:00", "17:00", 0, "day", 0))
                .thenReturn(Optional.of(dayShift));
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "19:00", "07:00", 1, "night", 0))
                .thenReturn(Optional.of(nightShift));
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SaveWorkPlanEmployeeDayAssignmentsBO request = new SaveWorkPlanEmployeeDayAssignmentsBO();
        request.setEmployeeId(109L);
        request.setWorkDate("2026-08-24");
        request.setDayStatus("work");
        request.setAssignments(java.util.Arrays.asList(
                buildDayAssignment("椰子饼", "装盒", "包装一车间", "08:00", "17:00", "day", false),
                buildDayAssignment("椰汁", "灌装", "包装二车间", "19:00", "07:00", "night", false)
        ));

        WorkPlanEmployeeDayAssignmentsVO result = workPlanService.saveEmployeeDayAssignments(request);

        Assert.assertEquals("work", result.getDayStatus());
        Assert.assertEquals(2, result.getAssignments().size());

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep, times(2)).saveAll(saveCaptor.capture());
        List<List> saveBatches = saveCaptor.getAllValues();
        tbplanlist savedShared = (tbplanlist) saveBatches.get(0).get(0);
        Assert.assertEquals(Integer.valueOf(3201), savedShared.getId());
        Assert.assertEquals("u-other", savedShared.getUserId());
        Assert.assertEquals("旧共享车间", savedShared.getWorkshopName());

        ArgumentCaptor<List> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).deleteAll(deleteCaptor.capture());
        Assert.assertEquals(1, deleteCaptor.getValue().size());
        Assert.assertSame(singlePlan, deleteCaptor.getValue().get(0));

        List newRows = saveBatches.get(1);
        Assert.assertEquals(2, newRows.size());
        tbplanlist dayRow = (tbplanlist) newRows.get(0);
        Assert.assertNull(dayRow.getId());
        Assert.assertEquals("u9", dayRow.getUserId());
        Assert.assertEquals("椰子饼", dayRow.getProductName());
        Assert.assertEquals("装盒", dayRow.getLinkName());
        Assert.assertEquals("包装一车间", dayRow.getWorkshopName());
        Assert.assertEquals(Long.valueOf(9309L), dayRow.getCustomShiftId());
        Assert.assertEquals("08:00", dayRow.getCustomStart());
        Assert.assertEquals("17:00", dayRow.getCustomEnd());
        Assert.assertEquals("day", dayRow.getCustomShiftPeriod());
        tbplanlist nightRow = (tbplanlist) newRows.get(1);
        Assert.assertEquals("椰汁", nightRow.getProductName());
        Assert.assertEquals("灌装", nightRow.getLinkName());
        Assert.assertEquals("包装二车间", nightRow.getWorkshopName());
        Assert.assertEquals(Long.valueOf(9310L), nightRow.getCustomShiftId());
        Assert.assertEquals("19:00", nightRow.getCustomStart());
        Assert.assertEquals("07:00", nightRow.getCustomEnd());
        Assert.assertEquals("night", nightRow.getCustomShiftPeriod());
    }

    @Test
    public void removeEmployeeDayShift_shouldSplitSharedPlanAndKeepOtherUsers() throws Exception {
        Date workDate = dayFormat.parse("2025-12-14");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(102L);
        attendanceUser.setUserId("u1");
        attendanceUser.setUserName("测试员工");

        tbplanlist sharedPlan = new tbplanlist();
        sharedPlan.setId(51);
        sharedPlan.setWorkDate(workDate);
        sharedPlan.setUserId("u1,u2");

        when(userRep.findFirstByEmpId(102L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(sharedPlan));

        workPlanService.removeEmployeeDayShift(102L, workDate);

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist savedShared = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertEquals(Integer.valueOf(51), savedShared.getId());
        Assert.assertEquals("u2", savedShared.getUserId());
        verify(planRep, never()).deleteAll(anyList());
    }

    @Test
    public void removeEmployeeDayShift_shouldDeleteSingleUserPlan() throws Exception {
        Date workDate = dayFormat.parse("2025-12-15");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(103L);
        attendanceUser.setUserId("u3");
        attendanceUser.setUserName("测试员工");

        tbplanlist plan = new tbplanlist();
        plan.setId(52);
        plan.setWorkDate(workDate);
        plan.setUserId("u3");

        when(userRep.findFirstByEmpId(103L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(plan));

        workPlanService.removeEmployeeDayShift(103L, workDate);

        ArgumentCaptor<List> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).deleteAll(deleteCaptor.capture());
        tbplanlist deleted = (tbplanlist) deleteCaptor.getValue().get(0);

        Assert.assertEquals(Integer.valueOf(52), deleted.getId());
        verify(planRep, never()).saveAll(anyList());
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldUseNonNullFallbackFields_whenNoLocalPlanExists() throws Exception {
        Date workDate = dayFormat.parse("2025-12-14");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(103L);
        attendanceUser.setUserId("u3");
        attendanceUser.setGroupId(2003L);

        HrmWorkPlanCustomShift customShift = new HrmWorkPlanCustomShift();
        customShift.setId(903L);
        customShift.setStart1("08:00");
        customShift.setEnd1("17:00");
        customShift.setCrossDay(0);

        when(userRep.findFirstByEmpId(103L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc("08:00", "17:00", 0, "day"))
                .thenReturn(Optional.of(customShift));
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        workPlanService.saveEmployeeDayCustomShift(103L, workDate, "08:00", "17:00", "day");

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertNotNull(saved.getProductName());
        Assert.assertNotNull(saved.getLinkName());
        Assert.assertEquals("", saved.getProductName());
        Assert.assertEquals("", saved.getLinkName());
        Assert.assertEquals("2003", saved.getGroupId());
    }

    @Test
    public void queryEmployeeDayShift_shouldReturnLocalRestShiftTypeLabel() throws Exception {
        Date workDate = dayFormat.parse("2025-12-16");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(104L);
        attendanceUser.setUserId("u4");
        attendanceUser.setGroupId(2004L);

        tbplanlist localPlan = new tbplanlist();
        localPlan.setId(44);
        localPlan.setWorkDate(workDate);
        localPlan.setUserId("u4");
        localPlan.setGroupId("2004");
        localPlan.setShiftType("rest");
        localPlan.setRestShiftType("adjust");

        when(userRep.findFirstByEmpId(104L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(localPlan));

        WorkPlanEmployeeDayShiftVO result = workPlanService.queryEmployeeDayShift(104L, workDate);

        Assert.assertEquals("rest", result.getCurrentShiftType());
        Assert.assertEquals("adjust", result.getRestShiftType());
        Assert.assertEquals("调休", result.getCurrentShiftLabel());
    }

    @Test
    public void saveEmployeeDayShift_shouldSaveRestWithoutCustomTime() throws Exception {
        Date workDate = dayFormat.parse("2025-12-16");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(104L);
        attendanceUser.setUserId("u4");
        attendanceUser.setGroupId(2004L);

        when(userRep.findFirstByEmpId(104L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkPlanEmployeeDayShiftVO result =
                workPlanService.saveEmployeeDayShift(104L, workDate, "rest", null, null, null, false, "adjust");

        Assert.assertEquals("rest", result.getCurrentShiftType());
        Assert.assertEquals("adjust", result.getRestShiftType());
        Assert.assertEquals("调休", result.getCurrentShiftLabel());
        Assert.assertNull(result.getCustomStart());
        Assert.assertNull(result.getCustomEnd());

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals("rest", saved.getShiftType());
        Assert.assertEquals("adjust", saved.getRestShiftType());
        Assert.assertEquals("u4", saved.getUserId());
        Assert.assertEquals("2004", saved.getGroupId());
        Assert.assertNull(saved.getClassId());
        Assert.assertNull(saved.getCustomShiftId());
    }

    @Test
    public void saveEmployeeDayShift_shouldTreatVacationTextAsRestShift() throws Exception {
        Date workDate = dayFormat.parse("2025-12-20");

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(108L);
        attendanceUser.setUserId("u8");
        attendanceUser.setGroupId(2008L);

        when(userRep.findFirstByEmpId(108L)).thenReturn(Optional.of(attendanceUser));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkPlanEmployeeDayShiftVO result =
                workPlanService.saveEmployeeDayShift(108L, workDate, "休假", null, null, null, false, "休假");

        Assert.assertEquals("rest", result.getCurrentShiftType());
        Assert.assertEquals("rest", result.getRestShiftType());
        Assert.assertEquals("休息", result.getCurrentShiftLabel());

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals("rest", saved.getShiftType());
        Assert.assertEquals("rest", saved.getRestShiftType());
        Assert.assertEquals("u8", saved.getUserId());
    }

    @Test
    public void saveEmployeeDayShift_shouldApplyEmployeeContinuousShiftWhenSavingCustomDayShift() throws Exception {
        Date workDate = dayFormat.parse("2025-12-17");

        tbattendanceuser attendanceUser = buildAttendanceUser(105L, "u5", "连班员工");
        attendanceUser.setGroupId(2005L);
        HrmEmployee employee = buildEmployee(105L, "连班员工", "13800000105", "u5");
        employee.setIsContinuousShift(1);

        when(userRep.findFirstByEmpId(105L)).thenReturn(Optional.of(attendanceUser));
        when(employeeRep.findById(105L)).thenReturn(Optional.of(employee));
        when(userRep.findAllByUserIdIn(Collections.singletonList("u5")))
                .thenReturn(Collections.singletonList(attendanceUser));
        when(employeeRep.findAllByEmployeeIdIn(Collections.singletonList(105L)))
                .thenReturn(Collections.singletonList(employee));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "08:00", "17:00", 0, "day", 1))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(9105L);
            return shift;
        });
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkPlanEmployeeDayShiftVO result =
                workPlanService.saveEmployeeDayShift(105L, workDate, "custom",
                        "08:00", "17:00", "day", false, null);

        Assert.assertEquals(Long.valueOf(9105L), result.getCustomShiftId());
        Assert.assertEquals(Boolean.TRUE, result.getCustomContinuousShift());
        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals(Boolean.TRUE, saved.getCustomContinuousShift());
    }

    @Test
    public void saveEmployeeDayShift_shouldKeepExplicitManualNonContinuousShiftValue() throws Exception {
        Date workDate = dayFormat.parse("2025-12-19");

        tbattendanceuser attendanceUser = buildAttendanceUser(107L, "u7", "手工未选连班员工");
        attendanceUser.setGroupId(2007L);
        HrmEmployee employee = buildEmployee(107L, "手工未选连班员工", "13800000107", "u7");
        employee.setIsContinuousShift(1);

        when(userRep.findFirstByEmpId(107L)).thenReturn(Optional.of(attendanceUser));
        when(userRep.findAllByUserIdIn(Collections.singletonList("u7")))
                .thenReturn(Collections.singletonList(attendanceUser));
        when(employeeRep.findAllByEmployeeIdIn(Collections.singletonList(107L)))
                .thenReturn(Collections.singletonList(employee));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "08:00", "17:00", 0, "day", 0))
                .thenReturn(Optional.empty());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
                "08:00", "17:00", 0, "day"))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(9107L);
            return shift;
        });
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        Method method = WorkPlanServiceImpl.class.getMethod("saveEmployeeDayShift",
                Long.class, Date.class, String.class, String.class, String.class, String.class,
                Boolean.class, Boolean.class, String.class);
        WorkPlanEmployeeDayShiftVO result = (WorkPlanEmployeeDayShiftVO) method.invoke(
                workPlanService, 107L, workDate, "custom", "08:00", "17:00", "day",
                false, true, null);

        Assert.assertEquals(Long.valueOf(9107L), result.getCustomShiftId());
        Assert.assertEquals(Boolean.FALSE, result.getCustomContinuousShift());
        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals(Boolean.FALSE, saved.getCustomContinuousShift());
    }

    @Test
    public void saveEmployeeDayShift_shouldKeepNightShiftNonContinuousEvenWhenEmployeeIsContinuous() throws Exception {
        Date workDate = dayFormat.parse("2025-12-18");

        tbattendanceuser attendanceUser = buildAttendanceUser(106L, "u6", "夜班员工");
        attendanceUser.setGroupId(2006L);
        HrmEmployee employee = buildEmployee(106L, "夜班员工", "13800000106", "u6");
        employee.setIsContinuousShift(1);

        when(userRep.findFirstByEmpId(106L)).thenReturn(Optional.of(attendanceUser));
        when(employeeRep.findById(106L)).thenReturn(Optional.of(employee));
        when(userRep.findAllByUserIdIn(Collections.singletonList("u6")))
                .thenReturn(Collections.singletonList(attendanceUser));
        when(employeeRep.findAllByEmployeeIdIn(Collections.singletonList(106L)))
                .thenReturn(Collections.singletonList(employee));
        when(planRep.findAllByWorkDateBetweenOrderByIdDesc(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                "20:00", "08:00", 1, "night", 0))
                .thenReturn(Optional.empty());
        when(customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
                "20:00", "08:00", 1, "night"))
                .thenReturn(Optional.empty());
        when(customShiftRep.save(any(HrmWorkPlanCustomShift.class))).thenAnswer(invocation -> {
            HrmWorkPlanCustomShift shift = invocation.getArgument(0);
            shift.setId(9106L);
            return shift;
        });
        when(planRep.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkPlanEmployeeDayShiftVO result =
                workPlanService.saveEmployeeDayShift(106L, workDate, "custom",
                        "20:00", "08:00", "night", true, null);

        Assert.assertEquals(Boolean.FALSE, result.getCustomContinuousShift());
        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);
        Assert.assertEquals(Boolean.FALSE, saved.getCustomContinuousShift());
    }

    private WorkPlanSubmitProgressVO awaitTaskDone(WorkPlanServiceImpl service, String taskId,
                                                   long timeoutValue, TimeUnit unit) throws Exception {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeoutValue);
        WorkPlanSubmitProgressVO progressVO = service.querySubmitProgress(taskId);
        while (!Boolean.TRUE.equals(progressVO.getDone()) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
            progressVO = service.querySubmitProgress(taskId);
        }
        return progressVO;
    }

    private ResolvedWorkPlanAssignment buildAssignment(tbplanlist plan, int sourceIndex, String userId,
                                                       String groupId, String shiftId) {
        ResolvedWorkPlanAssignment assignment = new ResolvedWorkPlanAssignment();
        assignment.setSourcePlan(plan);
        assignment.setSourceIndex(sourceIndex);
        assignment.setUserId(userId);
        assignment.setResolvedGroupId(groupId);
        assignment.setResolvedShiftId(shiftId);
        return assignment;
    }

    private SaveWorkPlanEmployeeDayAssignmentBO buildDayAssignment(String productName, String positionName,
                                                                   String workshopName,
                                                                   String customStart, String customEnd,
                                                                   String customShiftPeriod,
                                                                   Boolean customContinuousShift) {
        SaveWorkPlanEmployeeDayAssignmentBO assignment = new SaveWorkPlanEmployeeDayAssignmentBO();
        assignment.setProductName(productName);
        assignment.setPositionName(positionName);
        assignment.setWorkshopName(workshopName);
        assignment.setShiftType("custom");
        assignment.setCustomStart(customStart);
        assignment.setCustomEnd(customEnd);
        assignment.setCustomShiftPeriod(customShiftPeriod);
        assignment.setCustomContinuousShift(customContinuousShift);
        return assignment;
    }

    private HrmEmployee buildEmployee(Long employeeId, String employeeName, String mobile, String dingtalkUserId) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(employeeName);
        employee.setMobile(mobile);
        employee.setDingtalkUserId(dingtalkUserId);
        return employee;
    }

    private tbattendanceuser buildAttendanceUser(Long employeeId, String userId, String userName) {
        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(employeeId);
        user.setUserId(userId);
        user.setUserName(userName);
        return user;
    }

    private MockMultipartFile buildImportFile(String[][] rows) throws Exception {
        return buildImportFileWithHeaders(
                new String[]{"排班日期", "员工", "手机号", "排班类型", "排班时间", "白班/夜班", "备注"},
                rows);
    }

    private MockMultipartFile buildImportFileWithHeaders(String[] headers, String[][] rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("排班明细");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < rows[i].length; j++) {
                row.createCell(j).setCellValue(rows[i][j]);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "排班导入.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
    }

    private MockMultipartFile buildDesktopWorkplanXls() throws Exception {
        return buildDesktopWorkplanXls("李小瑞", "是");
    }

    private MockMultipartFile buildDesktopWorkplanXls(String employeeName, String restValue) throws Exception {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row dateHeader = sheet.createRow(0);
        dateHeader.createCell(0).setCellValue("日期");
        dateHeader.createCell(1).setCellValue("2026-5-6（注意连班信息要收集）");
        dateHeader.createCell(6).setCellValue(46149D);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 5));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 6, 10));

        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("姓名");
        String[] childHeaders = new String[]{"白/夜班", "上班时间", "下班时间", "是否连班", "休假/调休"};
        for (int group = 0; group < 2; group++) {
            for (int i = 0; i < childHeaders.length; i++) {
                header.createCell(1 + group * 5 + i).setCellValue(childHeaders[i]);
            }
        }

        Row employeeRow = sheet.createRow(2);
        employeeRow.createCell(0).setCellValue(employeeName);
        employeeRow.createCell(1).setCellValue("白");
        employeeRow.createCell(2).setCellValue("15:30");
        employeeRow.createCell(3).setCellValue("18:00");
        employeeRow.createCell(4).setCellValue("是");
        employeeRow.createCell(5).setCellValue("否");
        employeeRow.createCell(9).setCellValue("否");
        employeeRow.createCell(10).setCellValue(restValue);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return new MockMultipartFile("file", "workplan.xls",
                "application/vnd.ms-excel", outputStream.toByteArray());
    }
}
