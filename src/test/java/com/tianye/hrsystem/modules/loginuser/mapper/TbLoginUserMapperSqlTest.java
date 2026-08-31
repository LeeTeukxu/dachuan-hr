package com.tianye.hrsystem.modules.loginuser.mapper;

import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import org.junit.Assert;
import org.junit.Test;
import org.apache.ibatis.annotations.Param;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;

public class TbLoginUserMapperSqlTest {

    @Test
    public void systemAccountInsert_shouldUseConfiguredSystemDatabase() throws Exception {
        String xml = readMapperXml();
        String normalizedXml = xml.toLowerCase();

        Assert.assertTrue("创建登录用户写总库账号索引表时必须使用配置的系统库名", xml.contains("${systemDatabase}.tbAllUserList"));
        Assert.assertFalse("创建登录用户不能写死 hrsystem 总库", normalizedXml.contains("hrsystem.tballuserlist"));
    }

    @Test
    public void mapper_shouldAcceptSystemDatabaseSeparateFromTenantUserData() throws Exception {
        Method method;
        try {
            method = TbLoginUserMapper.class.getMethod("saveSystemLoginAccount", String.class, QueryLoginUserBO.class);
        } catch (NoSuchMethodException ex) {
            Assert.fail("Mapper 保存总库账号索引时应显式接收 systemDatabase 和登录用户数据");
            return;
        }

        Parameter[] parameters = method.getParameters();
        Assert.assertEquals("systemDatabase", parameters[0].getAnnotation(Param.class).value());
        Assert.assertEquals("data", parameters[1].getAnnotation(Param.class).value());
    }

    @Test
    public void mapper_shouldQueryAndDeleteSystemLoginAccountByConfiguredDatabase() throws Exception {
        String xml = readMapperXml();

        Assert.assertTrue(xml.contains("id=\"findSystemCompanyIdsByAccount\""));
        Assert.assertTrue(xml.contains("id=\"deleteSystemLoginAccount\""));
        Assert.assertTrue("系统账号索引查询应按公司去重，兼容历史重复行", xml.contains("SELECT DISTINCT CompanyID FROM ${systemDatabase}.tbAllUserList"));
        Assert.assertTrue(xml.contains("DELETE FROM ${systemDatabase}.tbAllUserList"));

        Method findMethod = TbLoginUserMapper.class.getMethod("findSystemCompanyIdsByAccount", String.class, String.class);
        Assert.assertEquals("systemDatabase", findMethod.getParameters()[0].getAnnotation(Param.class).value());
        Assert.assertEquals("account", findMethod.getParameters()[1].getAnnotation(Param.class).value());

        Method deleteMethod = TbLoginUserMapper.class.getMethod("deleteSystemLoginAccount", String.class, String.class, String.class);
        Assert.assertEquals("systemDatabase", deleteMethod.getParameters()[0].getAnnotation(Param.class).value());
        Assert.assertEquals("companyId", deleteMethod.getParameters()[1].getAnnotation(Param.class).value());
        Assert.assertEquals("account", deleteMethod.getParameters()[2].getAnnotation(Param.class).value());
    }

    private String readMapperXml() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mapper/TbLoginUserMapper.xml")) {
            Assert.assertNotNull("缺少 TbLoginUserMapper.xml", inputStream);
            byte[] bytes = new byte[inputStream.available()];
            int read = inputStream.read(bytes);
            Assert.assertTrue(read > 0);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
