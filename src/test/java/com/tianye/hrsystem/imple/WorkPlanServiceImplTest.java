package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.mapper.WorkPlanMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private StringRedisTemplate redisRep;

    @Mock
    private tbattendanceuserRepository userRep;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Before
    public void setUp() throws Exception {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("1001");
        CompanyContext.set(info);

        when(redisRep.hasKey(anyString())).thenReturn(true);
        when(redisRep.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("[]");
        when(tokener.Refresh(eq("1001"))).thenReturn("mock-token");
        when(tokener.GetAdminUser(eq("1001"))).thenReturn("mock-admin");
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void addAll_shouldCreateNewRecord_whenLoadedFromLastDateAndWorkDateChanges() throws Exception {
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

        when(planRep.findById(11)).thenReturn(Optional.of(existing));

        workPlanService.AddAll(Collections.singletonList(incoming));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertNull("加载上次排班后改新日期应新增记录，不能复用旧ID", saved.getId());
        Assert.assertEquals(dec2, saved.getWorkDate());
        Assert.assertEquals("旧日期排班不能被覆盖", dec1, existing.getWorkDate());
    }

    @Test
    public void addAll_shouldUpdateExistingRecord_whenWorkDateKeepsSame() throws Exception {
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

        when(planRep.findById(22)).thenReturn(Optional.of(existing));

        workPlanService.AddAll(Collections.singletonList(incoming));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRep).saveAll(saveCaptor.capture());
        tbplanlist saved = (tbplanlist) saveCaptor.getValue().get(0);

        Assert.assertSame("同日期编辑应更新原记录", existing, saved);
        Assert.assertEquals(Integer.valueOf(22), saved.getId());
        Assert.assertEquals("3", saved.getClassId());
        Assert.assertEquals("3001", saved.getGroupId());
        Assert.assertEquals("new-product", saved.getProductName());
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
}
