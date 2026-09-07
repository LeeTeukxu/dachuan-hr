package com.tianye.hrsystem.task;

import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AttendanceSyncTaskLauncherTest {

    private Redis redis;
    private AttendanceSyncTaskLauncher launcher;

    @Before
    public void setUp() {
        redis = Mockito.mock(Redis.class);
        launcher = new AttendanceSyncTaskLauncher(1, 1, 1);
        ReflectionTestUtils.setField(launcher, "redis", redis);
    }

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void tryBegin_shouldRejectSecondAcquireForSameCompany() {
        Mockito.doReturn(true).doReturn(false).when(redis).setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any());

        LoginUserInfo info = new LoginUserInfo();
        info.setAccount("alice");
        info.setUserName("Alice");
        info.setCompanyName("示例公司");
        Assert.assertTrue(launcher.tryBegin("0001", info));
        Assert.assertFalse(launcher.tryBegin("0001", info));
        Mockito.verify(redis, Mockito.times(2)).setNx(Mockito.eq("attendance:sync:running:0001"), Mockito.eq(7200L), Mockito.argThat(value -> {
            String payload = String.valueOf(value);
            return payload.contains("alice") && payload.contains("示例公司");
        }));
    }

    @Test
    public void getRunningOwner_shouldExposeAccountAndCompanyFromLockPayload() {
        Mockito.when(redis.get("attendance:sync:running:0001"))
                .thenReturn("{\"account\":\"alice\",\"userName\":\"Alice\",\"companyName\":\"示例公司\"}");

        Map<String, Object> owner = launcher.getRunningOwner("0001");

        Assert.assertEquals("alice", owner.get("account"));
        Assert.assertEquals("Alice", owner.get("userName"));
        Assert.assertEquals("示例公司", owner.get("companyName"));
    }

    @Test
    public void tryBegin_shouldRecoverFinishedStaleLock() {
        long oldAcquiredAt = System.currentTimeMillis() - 60_000L;
        Mockito.when(redis.setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(false).thenReturn(true);
        Mockito.when(redis.get("attendance:sync:running:0001"))
                .thenReturn("{\"account\":\"alice\",\"acquiredAt\":" + oldAcquiredAt + "}");
        Mockito.when(redis.get("attendance:sync:status:0001")).thenReturn("SUCCESS");
        Mockito.when(redis.get("attendance:sync:update_time:0001")).thenReturn(String.valueOf(oldAcquiredAt + 1));

        Assert.assertTrue(launcher.tryBegin("0001", new LoginUserInfo()));

        Mockito.verify(redis).del("attendance:sync:running:0001");
    }

    @Test
    public void tryBegin_shouldRecoverTerminalLockImmediatelyAfterProgressIsFinal() {
        long acquiredAt = System.currentTimeMillis();
        Mockito.when(redis.setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(false).thenReturn(true);
        Mockito.when(redis.get("attendance:sync:running:0001"))
                .thenReturn("{\"account\":\"alice\",\"acquiredAt\":" + acquiredAt + "}");
        Mockito.when(redis.get("attendance:sync:status:0001")).thenReturn("SUCCESS");
        Mockito.when(redis.get("attendance:sync:update_time:0001")).thenReturn(String.valueOf(acquiredAt + 1));

        Assert.assertTrue(launcher.tryBegin("0001", new LoginUserInfo()));

        Mockito.verify(redis).del("attendance:sync:running:0001");
    }

    @Test
    public void tryBegin_shouldRecoverRunningLockCreatedBeforeThisProcessStarted() {
        long acquiredAt = System.currentTimeMillis() - 60_000L;
        Mockito.when(redis.setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(false).thenReturn(true);
        Mockito.when(redis.get("attendance:sync:running:0001"))
                .thenReturn("{\"account\":\"alice\",\"acquiredAt\":" + acquiredAt + "}");
        Mockito.when(redis.get("attendance:sync:status:0001")).thenReturn("RUNNING");

        Assert.assertTrue(launcher.tryBegin("0001", new LoginUserInfo()));

        Mockito.verify(redis).del("attendance:sync:running:0001");
    }

    @Test
    public void tryBegin_shouldKeepRunningLockCreatedByThisProcess() {
        long processStartedAt = (Long) ReflectionTestUtils.getField(launcher, "processStartedAt");
        long acquiredAt = processStartedAt + 1;
        Mockito.when(redis.setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(false);
        Mockito.when(redis.get("attendance:sync:running:0001"))
                .thenReturn("{\"account\":\"alice\",\"acquiredAt\":" + acquiredAt + "}");
        Mockito.when(redis.get("attendance:sync:status:0001")).thenReturn("RUNNING");

        Assert.assertFalse(launcher.tryBegin("0001", new LoginUserInfo()));

        Mockito.verify(redis, Mockito.never()).del("attendance:sync:running:0001");
        Mockito.verify(redis, Mockito.times(1))
                .setNx(Mockito.eq("attendance:sync:running:0001"), Mockito.eq(7200L), Mockito.any());
    }

    @Test
    public void submit_shouldRunTaskWithCompanyContextAndReleaseLock() throws Exception {
        Mockito.when(redis.setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> contextCompanyId = new AtomicReference<>(null);
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId("0002");

        boolean submitted = launcher.submit("0002", info, () -> {
            LoginUserInfo context = CompanyContext.get();
            contextCompanyId.set(context == null ? null : context.getCompanyId());
            done.countDown();
        });
        Assert.assertTrue(submitted);
        Assert.assertTrue(done.await(5, TimeUnit.SECONDS));
        Assert.assertEquals("0002", contextCompanyId.get());
        // 后台任务结束后归还公司锁
        Mockito.verify(redis, Mockito.timeout(1000)).del("attendance:sync:running:0002");
        Assert.assertNull(CompanyContext.get());
    }

    @Test
    public void submit_shouldReturnFalseWhenQueueRejected() {
        Mockito.when(redis.setNx(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
        CountDownLatch block = new CountDownLatch(1);
        // 线程池 1 线程 + 队列 1：占住线程、占住队列后第三次提交应被拒绝
        launcher.submit("0001", new LoginUserInfo(), () -> awaitQuietly(block));
        launcher.submit("0001", new LoginUserInfo(), () -> awaitQuietly(block));
        boolean submitted = launcher.submit("0001", new LoginUserInfo(), () -> { });
        Assert.assertFalse(submitted);
        block.countDown();
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
