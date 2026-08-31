package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.Redis;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * SaaS 改造 P1-4：验证码校验逻辑单元测试
 * （一次性消费、大小写不敏感、过期/缺失场景）
 */
public class CaptchaValidateTest {

    private String validate(Redis redis, String captchaId, String code) throws Exception {
        Method m = CaptchaController.class.getDeclaredMethod("validate", Redis.class, String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, redis, captchaId, code);
    }

    @Test
    public void 正确验证码_通过且立即失效() throws Exception {
        Redis redis = mock(Redis.class);
        when(redis.get("hr:captcha:c1")).thenReturn((Object) "ABCD");

        assertNull(validate(redis, "c1", "abcd")); // 大小写不敏感
        verify(redis).del("hr:captcha:c1");        // 一次性消费
    }

    @Test
    public void 错误验证码_拒绝且同样销毁() throws Exception {
        Redis redis = mock(Redis.class);
        when(redis.get("hr:captcha:c1")).thenReturn((Object) "ABCD");

        assertEquals("验证码不正确", validate(redis, "c1", "WRONG"));
        verify(redis).del("hr:captcha:c1"); // 防重试爆破
    }

    @Test
    public void 过期或不存在_提示刷新() throws Exception {
        Redis redis = mock(Redis.class);
        when(redis.get(anyString())).thenReturn(null);

        assertEquals("验证码已过期，请刷新后重试", validate(redis, "gone", "AAAA"));
    }

    @Test
    public void 空参数_拒绝() throws Exception {
        Redis redis = mock(Redis.class);
        assertEquals("验证码已失效，请刷新后重试", validate(redis, "", "AAAA"));
        assertEquals("请输入验证码", validate(redis, "c1", ""));
    }
}
