package com.tianye.hrsystem.controller;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.tianye.hrsystem.common.MonthlyFullSyncGuard;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.ComboboxItem;
import com.tianye.hrsystem.model.GroupObject;
import com.tianye.hrsystem.model.HrmAttendanceGroup;
import com.tianye.hrsystem.model.HrmAttendancePlan;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.PlanObject;
import com.tianye.hrsystem.model.ShiftItem;
import com.tianye.hrsystem.model.UserObject;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.task.AttendanceSyncTaskLauncher;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmAttendanceDataService;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.util.MyDateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmAttendanceDataControllerTest {

    @InjectMocks
    private HrmAttendanceDataController controller;

    @Mock
    private IHrmAttendanceDataService dataService;

    @Mock
    private AttendanceSyncTaskLauncher syncTaskLauncher;

    @Mock
    private MonthlyFullSyncGuard monthlyFullSyncGuard;

    @Mock
    private hrmEmployeeRepository empRep;

    @Mock
    private IAccessToken tokener;

    @Mock
    private tbattendanceuserRepository userRep;

    @Mock
    private StringRedisTemplate redisRep;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MyDateUtils dateUtils;

    @Mock
    private IWorkPlanService planService;

    @Mock
    private hrmAttendanceShiftRepository shiftRep;

    @Mock
    private hrmAttendanceGroupRepository groupRep;

    @Mock
    private hrmAttendancePlanRepository attendancePlanRep;

    @Before
    public void setUp() {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("1001");
        CompanyContext.set(info);
        when(redisRep.opsForValue()).thenReturn(valueOperations);
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void getUsersByGroup_shouldUseDisplayUsersCacheAndFilterByGroup() throws Exception {
        UserObject user1 = buildUser("u1", "张三", "10");
        UserObject user2 = buildUser("u2", "李四", "11");
        when(planService.getUsersForDisplay("1001")).thenReturn(Arrays.asList(user1, user2));

        successResult result = controller.getUsersByGroup("10");

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ComboboxItem> data = (List<ComboboxItem>) result.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("u1", data.get(0).getId());
        Assert.assertEquals("张三", data.get(0).getText());
        verify(planService).getUsersForDisplay("1001");
        verifyZeroInteractions(tokener, userRep);
    }

    @Test
    public void getAllUsers_shouldUseDisplayUsersCache() throws Exception {
        UserObject user = buildUser("u1", "张三", "10");
        user.setEmployeeId(101L);
        when(planService.getUsersForDisplay("1001"))
                .thenReturn(Collections.singletonList(user));

        successResult result = controller.getAllUsers();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<UserObject> data = (List<UserObject>) result.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("u1", data.get(0).getId());
        Assert.assertEquals(Long.valueOf(101L), data.get(0).getEmployeeId());
        verify(planService).getUsersForDisplay("1001");
    }

    @Test
    public void getShiftList_shouldReadLocalSyncedShiftSnapshotWithoutDingTalk() throws Exception {
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(30L);
        shift.setShiftName("冬季后勤白班");
        when(shiftRep.findAll()).thenReturn(Collections.singletonList(shift));

        successResult result = controller.getShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<com.dingtalk.api.response.OapiAttendanceShiftListResponse.TopMinimalismShiftVo> data =
                (List<com.dingtalk.api.response.OapiAttendanceShiftListResponse.TopMinimalismShiftVo>) result.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(Long.valueOf(30L), data.get(0).getId());
        Assert.assertEquals("冬季后勤白班", data.get(0).getName());
        verify(tokener, never()).Refresh(eq("1001"));
        verify(tokener, never()).GetAdminUser(eq("1001"));
    }

    @Test
    public void getPlanDataByGroup_shouldReadSyncedLocalAttendancePlanWithoutDingTalk() throws Exception {
        tbattendanceuser user = new tbattendanceuser();
        user.setUserId("10001");
        user.setUserName("张三");
        user.setEmpId(101L);
        user.setGroupId(10L);

        HrmAttendancePlan onDutyPlan = buildAttendancePlan(9001L, "OnDuty", "10001", 101L, 10L, 20L,
                "2026-08-03 08:00:00", "2026-08-03");
        HrmAttendancePlan offDutyPlan = buildAttendancePlan(9002L, "OffDuty", "10001", 101L, 10L, 20L,
                "2026-08-03 17:30:00", "2026-08-03");
        HrmAttendancePlan otherGroupPlan = buildAttendancePlan(9003L, "OffDuty", "10002", 102L, 11L, 21L,
                "2026-08-03 18:00:00", "2026-08-03");

        when(userRep.findAll()).thenReturn(Collections.singletonList(user));
        when(attendancePlanRep.findAllByWorkDateBetween(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(onDutyPlan, offDutyPlan, otherGroupPlan));

        successResult result = controller.getPlanDataByGroup("2026-08-03", "2026-08-03", "10");

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<PlanObject> data = (List<PlanObject>) result.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(Long.valueOf(9002L), data.get(0).getPlanId());
        Assert.assertEquals(Long.valueOf(10L), data.get(0).getGroupId());
        Assert.assertEquals(Long.valueOf(20L), data.get(0).getShiftId());
        Assert.assertEquals(Long.valueOf(10001L), data.get(0).getUserId());
        Assert.assertEquals("张三", data.get(0).getUserName());
        Assert.assertEquals("2026-08-03", data.get(0).getWorkDate());
        verify(attendancePlanRep).findAllByWorkDateBetween(any(Date.class), any(Date.class));
        verify(tokener, never()).Refresh(eq("1001"));
        verify(tokener, never()).GetAdminUser(eq("1001"));
    }

    @Test
    public void getPlanDataByGroup_shouldMatchSyncedPlanOldGroupIdFromLocalSnapshot() throws Exception {
        tbattendanceuser user = new tbattendanceuser();
        user.setUserId("10001");
        user.setUserName("张三");
        user.setEmpId(101L);
        user.setGroupId(99L);

        HrmAttendanceGroup currentGroup = new HrmAttendanceGroup();
        currentGroup.setAttendanceGroupId(10L);
        currentGroup.setOldGroupId(99L);

        HrmAttendancePlan oldGroupPlan = buildAttendancePlan(9001L, "OffDuty", "10001", 101L, 99L, 20L,
                "2026-08-03 17:30:00", "2026-08-03");

        when(userRep.findAll()).thenReturn(Collections.singletonList(user));
        when(groupRep.findById(10L)).thenReturn(java.util.Optional.of(currentGroup));
        when(groupRep.findFirstByOldGroupId(10L)).thenReturn(java.util.Optional.empty());
        when(attendancePlanRep.findAllByWorkDateBetween(any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(oldGroupPlan));

        successResult result = controller.getPlanDataByGroup("2026-08-03", "2026-08-03", "10");

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<PlanObject> data = (List<PlanObject>) result.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(Long.valueOf(99L), data.get(0).getGroupId());
        Assert.assertEquals(Long.valueOf(9001L), data.get(0).getPlanId());
        verify(tokener, never()).Refresh(eq("1001"));
        verify(tokener, never()).GetAdminUser(eq("1001"));
    }

    @Test
    public void getClassListByGroup_shouldFallbackToLocalSnapshotWhenRedisCacheMissing() throws Exception {
        HrmAttendanceGroup group = new HrmAttendanceGroup();
        group.setAttendanceGroupId(10L);
        group.setName("后勤人员");
        group.setShiftSetting("30");

        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(30L);
        shift.setShiftName("冬季后勤白班");
        shift.setGroupId(10L);
        shift.setStart1("08:00:00");
        shift.setEnd1("17:30:00");

        when(redisRep.hasKey("ClassList_10")).thenReturn(false);
        when(groupRep.findById(10L)).thenReturn(java.util.Optional.of(group));
        when(shiftRep.findAll()).thenReturn(Collections.singletonList(shift));

        successResult result = controller.getClassListByGroupId("10");

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ComboboxItem> data = (List<ComboboxItem>) result.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("1", data.get(0).getId());
        Assert.assertEquals("休假", data.get(0).getText());
        Assert.assertEquals("30", data.get(1).getId());
        Assert.assertEquals("冬季后勤白班", data.get(1).getText());
        Assert.assertEquals("08:00", data.get(1).getBegin1());
        Assert.assertEquals("17:30", data.get(1).getEnd1());
        verify(tokener, never()).Refresh(eq("1001"));
        verify(tokener, never()).GetAdminUser(eq("1001"));
    }

    @Test
    public void getData_lockHeld_returns202AndFlagsAlreadyRunning() throws Exception {
        Date end = new Date();
        when(dateUtils.setItEnd(any(Date.class))).thenReturn(end);
        when(syncTaskLauncher.tryBegin(eq("1001"), any(LoginUserInfo.class))).thenReturn(false);

        // fullSync=false：不触达每月全量闸门，直接落到公司锁互斥判断
        successResult result = controller.GetData("101", "2026-08-01", "2026-08-31", "false");

        // 新版：锁被占用 → code=202（前端据此切进度模式），success 仍为 true，data 打 alreadyRunning
        Assert.assertEquals(Integer.valueOf(202), result.getCode());
        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getMessage().contains("正在进行中"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Assert.assertEquals(Boolean.TRUE, data.get("alreadyRunning"));
        Assert.assertEquals(Boolean.TRUE, data.get("queued"));
        verify(dataService, never()).markSyncQueued();
        verify(monthlyFullSyncGuard, never()).isMonthlyFullSynced(any(), any());
    }

    @Test
    public void getData_fullSyncDoneWithinMonth_rejectsWithMonthlyFullDoneCode() throws Exception {
        when(monthlyFullSyncGuard.isMonthlyFullSynced(
                eq(MonthlyFullSyncGuard.BIZ_ATTENDANCE), eq("1001"))).thenReturn(true);

        // fullSync=true 且本月已完成一次全量 → 拒绝，返回 4001 引导定向补拉
        successResult result = controller.GetData("101", "2026-08-01", "2026-08-31", "true");

        Assert.assertEquals(Integer.valueOf(4001), result.getCode());
        Assert.assertTrue(result.getMessage().contains("本月"));
        // 未进到公司锁抢占/后台提交
        verify(syncTaskLauncher, never()).tryBegin(eq("1001"), any(LoginUserInfo.class));
    }

    @Test
    public void getGroupList_shouldUseDisplayGroupsCache() throws Exception {
        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        group.setGroupId(10L);
        group.setGroupName("白班组");
        group.setMemberCount(2L);
        group.setType("TURN");
        group.setSelectedClass(Collections.emptyList());
        when(planService.getAllGroupsForDisplay()).thenReturn(Collections.singletonList(group));

        successResult result = controller.getGroupList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<GroupObject> data = (List<GroupObject>) result.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(Long.valueOf(10L), data.get(0).getGroupId());
        Assert.assertEquals("白班组", data.get(0).getGroupName());
        verify(planService).getAllGroupsForDisplay();
    }

    @Test
    public void getAllShifts_shouldUseDisplayGroupsCacheAndPreserveShiftTimes() throws Exception {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO onDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        onDuty.setCheckType("OnDuty");
        onDuty.setCheckTime(timeFormat.parse("08:30"));

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO offDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        offDuty.setCheckType("OffDuty");
        offDuty.setCheckTime(timeFormat.parse("17:30"));

        OapiAttendanceGetsimplegroupsResponse.AtSectionVo section =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        section.setTimes(Arrays.asList(onDuty, offDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo atClassVo =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        atClassVo.setClassId(20L);
        atClassVo.setClassName("白班");
        atClassVo.setSections(Collections.singletonList(section));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        group.setGroupId(10L);
        group.setGroupName("白班组");
        group.setType("TURN");
        group.setSelectedClass(Collections.singletonList(atClassVo));

        when(planService.getAllGroupsForDisplay()).thenReturn(Collections.singletonList(group));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("20", data.get(1).getShiftId());
        Assert.assertEquals("08:30", data.get(1).getBegin1());
        Assert.assertEquals("17:30", data.get(1).getEnd1());
        verify(planService).getAllGroupsForDisplay();
    }

    @Test
    public void getAllShifts_shouldExtractTimesByCheckType_whenSectionTimeOrderChanges() throws Exception {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO offDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        offDuty.setCheckType("OffDuty");
        offDuty.setCheckTime(timeFormat.parse("17:30"));

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO onDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        onDuty.setCheckType("OnDuty");
        onDuty.setCheckTime(timeFormat.parse("08:30"));

        OapiAttendanceGetsimplegroupsResponse.AtSectionVo section =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        section.setTimes(Arrays.asList(offDuty, onDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo atClassVo =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        atClassVo.setClassId(21L);
        atClassVo.setClassName("白班");
        atClassVo.setSections(Collections.singletonList(section));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        group.setGroupId(10L);
        group.setGroupName("白班组");
        group.setType("TURN");
        group.setSelectedClass(Collections.singletonList(atClassVo));

        when(planService.getAllGroupsForDisplay()).thenReturn(Collections.singletonList(group));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals("08:30", data.get(1).getBegin1());
        Assert.assertEquals("17:30", data.get(1).getEnd1());
    }

    @Test
    public void getAllShifts_shouldPreferDisplayGroups_whenDisplayDataAvailable() throws Exception {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO onDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        onDuty.setCheckType("OnDuty");
        onDuty.setCheckTime(timeFormat.parse("08:00"));

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO offDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        offDuty.setCheckType("OffDuty");
        offDuty.setCheckTime(timeFormat.parse("17:30"));

        OapiAttendanceGetsimplegroupsResponse.AtSectionVo section =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        section.setTimes(Arrays.asList(onDuty, offDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo atClassVo =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        atClassVo.setClassId(30L);
        atClassVo.setClassName("冬季后勤白班");
        atClassVo.setSections(Collections.singletonList(section));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo displayGroup =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        displayGroup.setGroupId(10L);
        displayGroup.setGroupName("后勤人员");
        displayGroup.setType("TURN");
        displayGroup.setSelectedClass(Collections.singletonList(atClassVo));

        when(planService.getAllGroupsForDisplay()).thenReturn(Collections.singletonList(displayGroup));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("30", data.get(1).getShiftId());
        Assert.assertEquals("后勤人员 / 冬季后勤白班", data.get(1).getGroupName());
        Assert.assertEquals("冬季后勤白班", data.get(1).getShiftName());
        Assert.assertEquals("08:00", data.get(1).getBegin1());
        Assert.assertEquals("17:30", data.get(1).getEnd1());
        verify(planService).getAllGroupsForDisplay();
    }

    @Test
    public void getAllShifts_shouldFallbackToLocalSnapshot_whenDisplayDataEmpty() throws Exception {
        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(30L);
        shift.setShiftName("冬季后勤白班");
        shift.setGroupId(10L);
        shift.setStart1("08:00:00");
        shift.setEnd1("17:30:00");

        HrmAttendanceGroup localGroup = new HrmAttendanceGroup();
        localGroup.setAttendanceGroupId(10L);
        localGroup.setName("后勤人员");
        localGroup.setShiftSetting("30");

        when(planService.getAllGroupsForDisplay()).thenReturn(Collections.emptyList());
        when(shiftRep.findAll()).thenReturn(Collections.singletonList(shift));
        when(groupRep.findAll()).thenReturn(Collections.singletonList(localGroup));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("30", data.get(1).getShiftId());
        Assert.assertEquals("后勤人员 / 冬季后勤白班", data.get(1).getGroupName());
        Assert.assertEquals("08:00", data.get(1).getBegin1());
        Assert.assertEquals("17:30", data.get(1).getEnd1());
    }

    @Test
    public void getAllShifts_shouldKeepShiftEntriesSeparatedByGroup() throws Exception {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO groupOneOnDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        groupOneOnDuty.setCheckType("OnDuty");
        groupOneOnDuty.setCheckTime(timeFormat.parse("08:00"));
        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO groupOneOffDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        groupOneOffDuty.setCheckType("OffDuty");
        groupOneOffDuty.setCheckTime(timeFormat.parse("17:30"));

        OapiAttendanceGetsimplegroupsResponse.AtSectionVo groupOneSection =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        groupOneSection.setTimes(Arrays.asList(groupOneOnDuty, groupOneOffDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo shiftOne =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        shiftOne.setClassId(99L);
        shiftOne.setClassName("白班");
        shiftOne.setSections(Collections.singletonList(groupOneSection));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupOne =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        groupOne.setGroupId(10L);
        groupOne.setGroupName("后勤人员");
        groupOne.setType("TURN");
        groupOne.setSelectedClass(Collections.singletonList(shiftOne));

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO groupTwoOnDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        groupTwoOnDuty.setCheckType("OnDuty");
        groupTwoOnDuty.setCheckTime(timeFormat.parse("07:00"));
        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO groupTwoOffDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        groupTwoOffDuty.setCheckType("OffDuty");
        groupTwoOffDuty.setCheckTime(timeFormat.parse("19:00"));

        OapiAttendanceGetsimplegroupsResponse.AtSectionVo groupTwoSection =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        groupTwoSection.setTimes(Arrays.asList(groupTwoOnDuty, groupTwoOffDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo shiftTwo =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        shiftTwo.setClassId(99L);
        shiftTwo.setClassName("白班");
        shiftTwo.setSections(Collections.singletonList(groupTwoSection));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupTwo =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        groupTwo.setGroupId(11L);
        groupTwo.setGroupName("工程两班倒考勤组");
        groupTwo.setType("TURN");
        groupTwo.setSelectedClass(Collections.singletonList(shiftTwo));

        when(planService.getAllGroupsForDisplay()).thenReturn(Arrays.asList(groupOne, groupTwo));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals(3, data.size());
        Assert.assertEquals("后勤人员 / 白班", data.get(1).getGroupName());
        Assert.assertEquals("08:00", data.get(1).getBegin1());
        Assert.assertEquals("工程两班倒考勤组 / 白班", data.get(2).getGroupName());
        Assert.assertEquals("07:00", data.get(2).getBegin1());
    }

    @Test
    public void getAllShifts_shouldKeepOnlyActiveTurnShifts_whenDisplayDataContainsGarbage() throws Exception {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO validOnDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        validOnDuty.setCheckType("OnDuty");
        validOnDuty.setCheckTime(timeFormat.parse("08:00"));
        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO validOffDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        validOffDuty.setCheckType("OffDuty");
        validOffDuty.setCheckTime(timeFormat.parse("17:30"));
        OapiAttendanceGetsimplegroupsResponse.AtSectionVo validSection =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        validSection.setTimes(Arrays.asList(validOnDuty, validOffDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo validShift =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        validShift.setClassId(30L);
        validShift.setClassName("冬季后勤白班");
        validShift.setSections(Collections.singletonList(validSection));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo validGroup =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        validGroup.setGroupId(10L);
        validGroup.setGroupName("后勤人员");
        validGroup.setType("TURN");
        validGroup.setSelectedClass(Collections.singletonList(validShift));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo invalidShiftNotInSnapshot =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        invalidShiftNotInSnapshot.setClassId(31L);
        invalidShiftNotInSnapshot.setClassName("垃圾班次");
        invalidShiftNotInSnapshot.setSections(Collections.singletonList(validSection));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo invalidTurnGroup =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        invalidTurnGroup.setGroupId(11L);
        invalidTurnGroup.setGroupName("默认考勤组");
        invalidTurnGroup.setType("TURN");
        invalidTurnGroup.setSelectedClass(Collections.singletonList(invalidShiftNotInSnapshot));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo invalidFixedShift =
                new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        invalidFixedShift.setClassId(32L);
        invalidFixedShift.setClassName("固定班");
        invalidFixedShift.setSections(Collections.singletonList(validSection));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo invalidFixedGroup =
                new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        invalidFixedGroup.setGroupId(12L);
        invalidFixedGroup.setGroupName("固定考勤组");
        invalidFixedGroup.setType("FIXED");
        invalidFixedGroup.setSelectedClass(Collections.singletonList(invalidFixedShift));

        HrmAttendanceGroup localGroup = new HrmAttendanceGroup();
        localGroup.setAttendanceGroupId(10L);
        localGroup.setName("后勤人员");
        localGroup.setShiftSetting("30");

        HrmAttendanceShift localShift = new HrmAttendanceShift();
        localShift.setShiftId(30L);
        localShift.setGroupId(10L);
        localShift.setShiftName("冬季后勤白班");
        localShift.setStart1("08:00:00");
        localShift.setEnd1("17:30:00");

        when(planService.getAllGroupsForDisplay()).thenReturn(Arrays.asList(validGroup, invalidTurnGroup, invalidFixedGroup));
        when(groupRep.findAll()).thenReturn(Collections.singletonList(localGroup));
        when(shiftRep.findAll()).thenReturn(Collections.singletonList(localShift));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("30", data.get(1).getShiftId());
        Assert.assertEquals("后勤人员 / 冬季后勤白班", data.get(1).getGroupName());
    }

    @Test
    public void getAllShifts_shouldFilterInvalidRows_whenFallbackToLocalSnapshot() throws Exception {
        HrmAttendanceGroup validGroup = new HrmAttendanceGroup();
        validGroup.setAttendanceGroupId(10L);
        validGroup.setName("后勤人员");
        validGroup.setShiftSetting("30");

        HrmAttendanceGroup timedButMissingShiftBindingGroup = new HrmAttendanceGroup();
        timedButMissingShiftBindingGroup.setAttendanceGroupId(11L);
        timedButMissingShiftBindingGroup.setName("默认考勤组");
        timedButMissingShiftBindingGroup.setShiftSetting("32");

        HrmAttendanceShift validShift = new HrmAttendanceShift();
        validShift.setShiftId(30L);
        validShift.setGroupId(10L);
        validShift.setShiftName("冬季后勤白班");
        validShift.setStart1("08:00:00");
        validShift.setEnd1("17:30:00");

        HrmAttendanceShift missingShiftBinding = new HrmAttendanceShift();
        missingShiftBinding.setShiftId(31L);
        missingShiftBinding.setGroupId(10L);
        missingShiftBinding.setShiftName("垃圾班次");
        missingShiftBinding.setStart1("09:00:00");
        missingShiftBinding.setEnd1("18:00:00");

        HrmAttendanceShift missingTimes = new HrmAttendanceShift();
        missingTimes.setShiftId(32L);
        missingTimes.setGroupId(11L);
        missingTimes.setShiftName("无效空班次");

        HrmAttendanceShift missingGroup = new HrmAttendanceShift();
        missingGroup.setShiftId(33L);
        missingGroup.setShiftName("孤儿班次");
        missingGroup.setStart1("07:00:00");
        missingGroup.setEnd1("19:00:00");

        when(planService.getAllGroupsForDisplay()).thenReturn(Collections.emptyList());
        when(groupRep.findAll()).thenReturn(Arrays.asList(validGroup, timedButMissingShiftBindingGroup));
        when(shiftRep.findAll()).thenReturn(Arrays.asList(validShift, missingShiftBinding, missingTimes, missingGroup));

        successResult result = controller.getAllShiftList();

        Assert.assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        List<ShiftItem> data = (List<ShiftItem>) result.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("30", data.get(1).getShiftId());
        Assert.assertEquals("后勤人员 / 冬季后勤白班", data.get(1).getGroupName());
    }

    private UserObject buildUser(String id, String name, String groupId) {
        UserObject user = new UserObject();
        user.setId(id);
        user.setName(name);
        user.setGroupId(groupId);
        return user;
    }

    private HrmAttendancePlan buildAttendancePlan(Long planId, String checkType, String userId, Long empId,
                                                  Long groupId, Long classId, String planCheckTime,
                                                  String workDate) throws Exception {
        HrmAttendancePlan plan = new HrmAttendancePlan();
        plan.setPlanId(planId);
        plan.setCheckType(checkType);
        plan.setUserId(userId);
        plan.setEmpId(empId);
        plan.setGroupId(groupId);
        plan.setClassId(classId);
        plan.setPlanCheckTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(planCheckTime));
        plan.setWorkDate(new SimpleDateFormat("yyyy-MM-dd").parse(workDate));
        return plan;
    }
}
