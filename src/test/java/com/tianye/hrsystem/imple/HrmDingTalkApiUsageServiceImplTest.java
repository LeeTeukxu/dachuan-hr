package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.Postresultlog;
import com.tianye.hrsystem.repository.postresultlogRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmDingTalkApiUsageServiceImplTest {

    @Mock
    private postresultlogRepository logRepository;

    private HrmDingTalkApiUsageServiceImpl service;

    @Before
    public void setUp() {
        service = new HrmDingTalkApiUsageServiceImpl();
        ReflectionTestUtils.setField(service, "logRepository", logRepository);
        ReflectionTestUtils.setField(service, "monthlyLimit", 100L);
        ReflectionTestUtils.setField(service, "thresholdPercent", 80);
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-08-16T03:20:00Z"), ZoneId.of("Asia/Shanghai")));
    }

    @Test
    public void queryMonthlyUsage_shouldSummarizeCurrentMonthRecordedDingTalkCalls() throws Exception {
        List<Postresultlog> logs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            logs.add(log("https://oapi.dingtalk.com/topapi/attendance/listschedule",
                    "com.tianye.hrsystem.imple.ddTalk.AttendancePlanRecord"));
        }
        for (int i = 0; i < 30; i++) {
            logs.add(log("https://oapi.dingtalk.com/attendance/listRecord",
                    "com.tianye.hrsystem.imple.ddTalk.AttendanceDetailRecord"));
        }
        for (int i = 0; i < 2; i++) {
            logs.add(log("https://oapi.dingtalk.com/topapi/processinstance/get",
                    "com.tianye.hrsystem.imple.HrmAttendanceApprovalSyncServiceImpl"));
        }
        when(logRepository.findAllByCreateTimeGreaterThanEqualAndCreateTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(logs);

        Object result = service.queryMonthlyUsage();

        Assert.assertEquals("2026-08", invoke(result, "getMonth"));
        Assert.assertEquals(82L, invoke(result, "getTotalCount"));
        Assert.assertEquals(100L, invoke(result, "getMonthlyLimit"));
        Assert.assertEquals(82.0D, (Double) invoke(result, "getUsagePercent"), 0.001D);
        Assert.assertEquals(Boolean.TRUE, invoke(result, "getOverThreshold"));
        Assert.assertTrue(((String) invoke(result, "getDescription")).contains("已超过80%"));

        List<?> distributions = (List<?>) invoke(result, "getDistributions");
        Assert.assertEquals("应按功能聚合为3类", 3, distributions.size());
        Assert.assertEquals("同步考勤-排班", invoke(distributions.get(0), "getFeatureName"));
        Assert.assertEquals(50L, invoke(distributions.get(0), "getCount"));
        Assert.assertEquals(60.98D, (Double) invoke(distributions.get(0), "getPercentage"), 0.001D);
        Assert.assertEquals("同步考勤-打卡明细", invoke(distributions.get(1), "getFeatureName"));
        Assert.assertEquals("审批数据获取", invoke(distributions.get(2), "getFeatureName"));

        verify(logRepository).findAllByCreateTimeGreaterThanEqualAndCreateTimeLessThan(any(Date.class), any(Date.class));
    }

    @Test
    public void queryMonthlyUsage_shouldReturnSafeEmptyResultWhenNoRecordedCalls() throws Exception {
        when(logRepository.findAllByCreateTimeGreaterThanEqualAndCreateTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(new ArrayList<Postresultlog>());

        Object result = service.queryMonthlyUsage();

        Assert.assertEquals("2026-08", invoke(result, "getMonth"));
        Assert.assertEquals(0L, invoke(result, "getTotalCount"));
        Assert.assertEquals(0.0D, (Double) invoke(result, "getUsagePercent"), 0.001D);
        Assert.assertEquals(Boolean.FALSE, invoke(result, "getOverThreshold"));
        Assert.assertTrue(((List<?>) invoke(result, "getDistributions")).isEmpty());
    }

    private Postresultlog log(String url, String className) {
        Postresultlog log = new Postresultlog();
        log.setPostUrl(url);
        log.setClassName(className);
        log.setCreateTime(new Date());
        return log;
    }

    private Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }
}
