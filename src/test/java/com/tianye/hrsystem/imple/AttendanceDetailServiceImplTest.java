package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceClockRepository;
import com.tianye.hrsystem.service.ddTalk.IDetailRecord;
import com.tianye.hrsystem.util.MyDateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttendanceDetailServiceImplTest {

    @InjectMocks
    private AttendanceDetailServiceImpl attendanceDetailService;

    @Mock
    private IDetailRecord detailRecord;

    @Mock
    private MyDateUtils dateUtils;

    @Mock
    private hrmAttendanceClockRepository clockRep;

    private Date begin;
    private Date end;

    @Before
    public void setUp() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        begin = format.parse("2026-02-01 00:00:00");
        end = format.parse("2026-02-28 23:59:59");
        when(dateUtils.getDateRangeByLimit(begin, end, 7))
                .thenReturn(Collections.singletonList(new Date[]{begin, end}));
    }

    @Test
    public void sync_shouldSkipDeleteAndFetch_whenNoMappedUsers() throws Exception {
        attendanceDetailService.setUsers(Collections.emptyList());

        attendanceDetailService.Sync("1001", begin, end);

        verify(clockRep, never()).deleteAllByClockEmployeeIdInAndWorkDateBetween(anyList(), any(Date.class), any(Date.class));
        verify(detailRecord, never()).GetAndSave(any(String.class), any(Date.class), any(Date.class));
    }

    @Test
    public void sync_shouldOnlyProcessMappedEmployees() throws Exception {
        tbattendanceuser mappedUser = new tbattendanceuser();
        mappedUser.setEmpId(1001L);
        mappedUser.setUserId("dingtalk_u1");
        List<tbattendanceuser> users = Collections.singletonList(mappedUser);
        attendanceDetailService.setUsers(users);

        attendanceDetailService.Sync("1001,1002", begin, end);

        verify(clockRep).deleteAllByClockEmployeeIdInAndWorkDateBetween(eq(Collections.singletonList(1001L)), eq(begin), eq(end));
        verify(detailRecord).setUsers(eq(users));
        verify(detailRecord).GetAndSave(eq("1001"), eq(begin), eq(end));
        verify(detailRecord, never()).GetAndSave(eq("1001,1002"), any(Date.class), any(Date.class));
    }
}
