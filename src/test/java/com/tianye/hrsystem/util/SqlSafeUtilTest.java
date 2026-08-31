package com.tianye.hrsystem.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SaaS 改造 P1-4：SqlSafeUtil 单元测试
 */
public class SqlSafeUtilTest {

    @Test
    public void safeIdentifier_合法列名() {
        assertEquals("createTime", SqlSafeUtil.safeIdentifier("createTime"));
        assertEquals("a.update_time", SqlSafeUtil.safeIdentifier("a.update_time"));
        assertEquals("user_id", SqlSafeUtil.safeIdentifier("user_id"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeIdentifier_SQL注入片段_拒绝() {
        SqlSafeUtil.safeIdentifier("1;DROP TABLE hrm_employee");
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeIdentifier_sleep注入_拒绝() {
        SqlSafeUtil.safeIdentifier("sleep(5)");
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeIdentifier_引号逃逸_拒绝() {
        SqlSafeUtil.safeIdentifier("name'--");
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeIdentifier_null_拒绝() {
        SqlSafeUtil.safeIdentifier(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeIdentifier_超长标识符_拒绝() {
        SqlSafeUtil.safeIdentifier(new String(new char[100]).replace('\0', 'a'));
    }

    @Test
    public void safeIdentifierOrEmpty_空值返回空串不抛异常() {
        // 覆盖 MyBatis <bind> 场景：sortField 缺失时不能抛异常
        assertEquals("", SqlSafeUtil.safeIdentifierOrEmpty(null));
        assertEquals("", SqlSafeUtil.safeIdentifierOrEmpty(""));
        assertEquals("", SqlSafeUtil.safeIdentifierOrEmpty("  "));
    }

    @Test
    public void safeIdentifierOrEmpty_合法值原样返回() {
        assertEquals("employee_name", SqlSafeUtil.safeIdentifierOrEmpty("employee_name"));
    }

    @Test
    public void safeSortOrder_仅接受asc_desc() {
        assertEquals("asc", SqlSafeUtil.safeSortOrder("ASC"));
        assertEquals("desc", SqlSafeUtil.safeSortOrder("desc"));
        assertEquals("asc", SqlSafeUtil.safeSortOrder("; DROP TABLE x"));
        assertEquals("asc", SqlSafeUtil.safeSortOrder(""));
        assertEquals("asc", SqlSafeUtil.safeSortOrder(null));
    }

    @Test
    public void safePageParams_边界与非法值() {
        assertEquals(0, SqlSafeUtil.safePageNum(-5).intValue());
        assertEquals(0, SqlSafeUtil.safePageNum(null).intValue());
        assertEquals(50, SqlSafeUtil.safePageNum(50).intValue());
        assertEquals(10, SqlSafeUtil.safePageSize(null).intValue());
        assertEquals(10, SqlSafeUtil.safePageSize(0).intValue());
        assertEquals(5000, SqlSafeUtil.safePageSize(999999).intValue());
    }
}
