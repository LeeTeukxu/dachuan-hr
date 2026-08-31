package com.tianye.hrsystem.controller;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

public class HrmDingTalkApiUsageControllerTest {

    @Test
    public void queryMonthlyUsage_shouldDelegateToService() throws Exception {
        Class<?> controllerClass = Class.forName("com.tianye.hrsystem.controller.HrmDingTalkApiUsageController");
        Class<?> serviceInterface = Class.forName("com.tianye.hrsystem.service.IHrmDingTalkApiUsageService");
        Class<?> resultClass = Class.forName("com.tianye.hrsystem.entity.vo.Result");

        Object controller = controllerClass.getDeclaredConstructor().newInstance();
        AtomicInteger invokeCount = new AtomicInteger();
        Class<?> usageVOClass = Class.forName("com.tianye.hrsystem.entity.vo.DingTalkApiUsageVO");
        Object expectedVO = usageVOClass.getDeclaredConstructor().newInstance();
        usageVOClass.getMethod("setMonth", String.class).invoke(expectedVO, "2026-08");

        Object serviceProxy = Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    if ("queryMonthlyUsage".equals(method.getName())) {
                        invokeCount.incrementAndGet();
                        return expectedVO;
                    }
                    return null;
                });
        setField(controller, "dingTalkApiUsageService", serviceProxy);

        Method method = controllerClass.getMethod("queryMonthlyUsage");
        Object result = method.invoke(controller);

        Assert.assertEquals("控制器必须只委托一次服务", 1, invokeCount.get());
        Assert.assertTrue("返回值必须是统一Result类型", resultClass.isInstance(result));
        Assert.assertSame(expectedVO, resultClass.getMethod("getData").invoke(result));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
