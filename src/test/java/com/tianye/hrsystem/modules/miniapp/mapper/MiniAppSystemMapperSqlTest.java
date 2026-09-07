package com.tianye.hrsystem.modules.miniapp.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class MiniAppSystemMapperSqlTest {

    @Test
    public void listAllCompanies_shouldUseLegacyCamelCaseColumns() throws Exception {
        Method method = MiniAppSystemMapper.class.getMethod("listAllCompanies");
        Select select = method.getAnnotation(Select.class);

        Assert.assertNotNull("公司列表查询必须声明 @Select", select);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ").trim();
        Assert.assertTrue("tbCompanyList 企业编码列名必须是 companyId",
                sql.contains("companyId"));
        Assert.assertTrue("tbCompanyList 企业名称列名必须是 companyName",
                sql.contains("companyName"));
        Assert.assertFalse("tbCompanyList 不存在 company_id 列",
                sql.contains("company_id"));
        Assert.assertFalse("tbCompanyList 不存在 company_name 列",
                sql.contains("company_name"));
    }
}
