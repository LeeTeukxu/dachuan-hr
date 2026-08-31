package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import com.tianye.hrsystem.util.MyDateUtils;
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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaveRecordDataServiceImplTest {

    @InjectMocks
    private LeaveRecordDataServiceImpl service;

    @Mock
    private MyDateUtils dateUtils;

    @Mock
    private IHrmAttendanceReport report;

    @Mock
    private tbattendanceuserRepository userRep;

    @Mock
    private hrmAttendanceReportDataRepository dataRep;

    @Mock
    private hrmAttendanceReportFieldRepository fieldRep;

    private Date begin;
    private Date end;

    @Before
    public void setUp() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        begin = format.parse("2026-05-01 00:00:00");
        end = format.parse("2026-05-15 23:59:59");
        when(dateUtils.getDateRangeByLimit(begin, end, 15))
                .thenReturn(Collections.singletonList(new Date[]{begin, end}));
    }

    @Test
    public void sync_shouldReuseInjectedUsersAndSkipDuplicateMappings() throws Exception {
        tbattendanceuser user1 = new tbattendanceuser();
        user1.setEmpId(1001L);
        user1.setUserId("u1");

        tbattendanceuser duplicateUser1 = new tbattendanceuser();
        duplicateUser1.setEmpId(1001L);
        duplicateUser1.setUserId("u1");

        tbattendanceuser user2 = new tbattendanceuser();
        user2.setEmpId(1002L);
        user2.setUserId("u2");

        tbattendanceuser unrelatedUser = new tbattendanceuser();
        unrelatedUser.setEmpId(1003L);
        unrelatedUser.setUserId("u3");


        service.Sync("1001,1002", begin, end, Arrays.asList(user1, duplicateUser1, user2, unrelatedUser));

        verify(report, times(1)).UpdateAttendanceReportQuick(eq("u1"), eq(1001L), eq(begin), eq(end));
        verify(report, times(1)).UpdateAttendanceReportQuick(eq("u2"), eq(1002L), eq(begin), eq(end));
        verify(report, never()).UpdateAttendanceReportQuick(eq("u3"), eq(1003L), eq(begin), eq(end));
        verifyZeroInteractions(userRep, fieldRep, dataRep);
    }
}
