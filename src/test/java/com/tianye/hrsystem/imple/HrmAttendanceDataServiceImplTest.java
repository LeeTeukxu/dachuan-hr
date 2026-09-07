package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IAttendanceDetailService;
import com.tianye.hrsystem.service.IAttendancePlanService;
import com.tianye.hrsystem.service.IHolidayDataService;
import com.tianye.hrsystem.service.ILeaveRecordDtaService;
import com.tianye.hrsystem.service.ddTalk.IGroupManager;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import com.tianye.hrsystem.service.ddTalk.IUserManager;
import com.tianye.hrsystem.util.MyDateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmAttendanceDataServiceImplTest {

    @InjectMocks
    private HrmAttendanceDataServiceImpl service;

    @Mock
    private IAttendancePlanService planService;

    @Mock
    private IAttendanceDetailService detailService;

    @Mock
    private ILeaveRecordDtaService leaveService;

    @Mock
    private IHolidayDataService holidayService;

    @Mock
    private tbattendanceuserRepository userRep;

    @Mock
    private IHrmAttendanceReport report;

    @Mock
    private MyDateUtils dateUtils;

    @Mock
    private IUserManager userManager;

    @Mock
    private IGroupManager groupManager;

    @Mock
    private hrmAttendanceReportDataRepository dataRep;

    @Mock
    private hrmEmployeeRepository empRep;

    @Mock
    private Redis redis;

    private Date begin;
    private Date end;
    private List<tbattendanceuser> users;

    @Before
    public void setUp() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        begin = format.parse("2026-05-01 00:00:00");
        end = format.parse("2026-05-31 23:59:59");

        tbattendanceuser user1 = new tbattendanceuser();
        user1.setEmpId(1001L);
        user1.setUserId("u1");
        tbattendanceuser user2 = new tbattendanceuser();
        user2.setEmpId(1002L);
        user2.setUserId("u2");
        users = Arrays.asList(user1, user2);

        when(userRep.findAll()).thenReturn(users);
        doNothing().when(redis).setex(anyString(), anyInt(), any());
        doNothing().when(redis).del(any());

        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("0003");
        CompanyContext.set(info);
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void syncDataWithResume_shouldCallPlanSyncOnceForWholeEmployeeBatch() throws Exception {
        doThrow(new RuntimeException("stop-after-step4")).when(report).UpdateReportFields();

        try {
            service.SyncDataWithResume("1001,1002", begin, end, false);
        } catch (RuntimeException ex) {
            org.junit.Assert.assertEquals("stop-after-step4", ex.getMessage());
        }

        verify(planService).Sync(eq("1001,1002"), eq(begin), eq(end), eq(users));
        verify(planService, never()).Sync(eq("1001"), eq(begin), eq(end), eq(users));
        verify(planService, never()).Sync(eq("1002"), eq(begin), eq(end), eq(users));
    }

    @Test
    public void syncDataWithResume_shouldCallDetailSyncOnceForFiftyEmployeeBatch() throws Exception {
        tbattendanceuser user3 = new tbattendanceuser();
        user3.setEmpId(1003L);
        user3.setUserId("u3");
        when(userRep.findAll()).thenReturn(Arrays.asList(users.get(0), users.get(1), user3));
        doThrow(new RuntimeException("stop-after-step4")).when(report).UpdateReportFields();

        try {
            service.SyncDataWithResume("1001,1002,1003", begin, end, false);
        } catch (RuntimeException ex) {
            org.junit.Assert.assertEquals("stop-after-step4", ex.getMessage());
        }

        verify(detailService).Sync(eq("1001,1002,1003"), eq(begin), eq(end), any(List.class));
        verify(detailService, never()).Sync(eq("1001"), eq(begin), eq(end), any(List.class));
        verify(detailService, never()).Sync(eq("1002"), eq(begin), eq(end), any(List.class));
        verify(detailService, never()).Sync(eq("1003"), eq(begin), eq(end), any(List.class));
    }

    @Test
    public void syncDataWithResume_shouldDeduplicateRequestedEmployeeIdsBeforeSyncing() throws Exception {
        doThrow(new RuntimeException("stop-after-step4")).when(report).UpdateReportFields();

        try {
            service.SyncDataWithResume("1001,1001,1002,,1002", begin, end, false);
        } catch (RuntimeException ex) {
            org.junit.Assert.assertEquals("stop-after-step4", ex.getMessage());
        }

        verify(planService).Sync(eq("1001,1002"), eq(begin), eq(end), eq(users));
        verify(detailService).Sync(eq("1001,1002"), eq(begin), eq(end), any(List.class));
        verify(planService, never()).Sync(eq("1001,1001,1002,,1002"), eq(begin), eq(end), any(List.class));
        verify(detailService, never()).Sync(eq("1001,1001,1002,,1002"), eq(begin), eq(end), any(List.class));
    }

    @Test
    public void getSyncProgress_shouldExposeSuccessUntilOperatorAcknowledges() {
        when(redis.get("attendance:sync:step:0003")).thenReturn("7");
        when(redis.get("attendance:sync:status:0003")).thenReturn("SUCCESS");
        when(redis.get("attendance:sync:message:0003")).thenReturn("同步完成，请确认后关闭进度条");

        Map<String, Object> progress = service.getSyncProgress();

        org.junit.Assert.assertEquals("SUCCESS", progress.get("status"));
        org.junit.Assert.assertEquals(Boolean.TRUE, progress.get("done"));
        org.junit.Assert.assertEquals(Boolean.TRUE, progress.get("success"));
        org.junit.Assert.assertEquals(100, progress.get("progress"));
        org.junit.Assert.assertEquals("同步完成，请确认后关闭进度条", progress.get("message"));
    }

    @Test
    public void markSyncRetrying_shouldStoreUpdateTimeAsStringForStringRedisSerializer() {
        service.markSyncRetrying(1, 1000L);

        org.mockito.ArgumentCaptor<Object> updateTime = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(redis).setex(eq("attendance:sync:update_time:0003"), anyInt(), updateTime.capture());
        org.junit.Assert.assertTrue("进度更新时间必须以字符串写入 Redis", updateTime.getValue() instanceof String);
    }

    @Test
    public void getSyncProgress_shouldParseStringStageFromRedis() {
        org.mockito.Mockito.doReturn("3").when(redis).get("attendance:sync:step:0003");

        Map<String, Object> progress = service.getSyncProgress();

        org.junit.Assert.assertEquals(3, progress.get("currentStep"));
        org.junit.Assert.assertEquals(20, progress.get("progress"));
    }

    @Test
    public void syncDataWithResume_shouldPersistFriendlyFailureProgressForResume() throws Exception {
        doThrow(new RuntimeException("java.sql.SQLSyntaxErrorException: Unknown column 'foo'"))
                .when(groupManager).GetAndSave();

        try {
            service.SyncDataWithResume("1001,1002", begin, end, false);
            org.junit.Assert.fail("同步失败时应抛出异常");
        } catch (RuntimeException ex) {
            org.junit.Assert.assertTrue(ex.getMessage().contains("SQLSyntaxErrorException"));
        }

        verify(redis).setex(eq("attendance:sync:status:0003"), anyInt(), eq("FAILED"));
        verify(redis).setex(eq("attendance:sync:message:0003"), anyInt(), contains("同步考勤失败"));
        verify(redis, never()).setex(eq("attendance:sync:message:0003"), anyInt(), contains("SQLSyntaxErrorException"));
        verify(redis, never()).setex(eq("attendance:sync:message:0003"), anyInt(), contains("Unknown column"));
    }


    @Test
    public void syncDataWithAutoRetry_shouldResumeAfterTransientFailure() throws Exception {
        HrmAttendanceDataServiceImpl spyService = org.mockito.Mockito.spy(service);
        org.mockito.Mockito.doThrow(new RuntimeException("dingtalk down"))
                .when(spyService).SyncData(anyString(), any(Date.class), any(Date.class));
        org.mockito.Mockito.doReturn(true).when(spyService)
                .SyncDataWithResume(anyString(), any(Date.class), any(Date.class), org.mockito.Mockito.eq(true));
        org.springframework.test.util.ReflectionTestUtils.setField(spyService, "syncAutoRetryBackoffMs", 1L);

        boolean ok = spyService.SyncDataWithAutoRetry("1001,1002", begin, end);

        org.junit.Assert.assertTrue(ok);
        // 第二次运行必须以断点续传方式执行
        org.mockito.Mockito.verify(spyService).SyncDataWithResume("1001,1002", begin, end, true);
        // 重试等待期保持 RUNNING,避免前端把中间失败当成最终结果
        org.mockito.Mockito.verify(redis, org.mockito.Mockito.atLeastOnce())
                .setex(org.mockito.Mockito.eq("attendance:sync:status:0003"), org.mockito.Mockito.anyInt(), org.mockito.Mockito.eq("RUNNING"));
    }
}
