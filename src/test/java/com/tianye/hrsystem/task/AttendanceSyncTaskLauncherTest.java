package com.tianye.hrsystem.task;

import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
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

        Assert.assertTrue(launcher.tryBegin("0001"));
        Assert.assertFalse(launcher.tryBegin("0001"));
        Mockito.verify(redis, Mockito.times(2)).setNx("attendance:sync:running:0001", 7200L, "1");
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
