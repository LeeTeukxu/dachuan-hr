package com.tianye.hrsystem.modules.miniapp.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class MiniAppEmployeeMapperSqlTest {

    @Test
    public void bindOpenidIfEmpty_shouldUseAtomicConditionalUpdate() throws Exception {
        Class<?> mapper = Class.forName(
                "com.tianye.hrsystem.modules.miniapp.mapper.MiniAppEmployeeMapper");
        Method method = mapper.getMethod("bindOpenidIfEmpty", Long.class, String.class);
        Update update = method.getAnnotation(Update.class);

        Assert.assertNotNull("OpenID绑定必须声明 @Update", update);
        String sql = String.join(" ", update.value()).replaceAll("\\s+", " ").toLowerCase();
        Assert.assertTrue(sql.contains("update hrm_employee"));
        Assert.assertTrue(sql.contains("employee_id = #{employeeid}"));
        Assert.assertTrue(sql.contains("openid = #{openid}"));
        Assert.assertTrue(sql.contains("openid is null"));
        Assert.assertTrue(sql.contains("openid = ''"));
    }
}
