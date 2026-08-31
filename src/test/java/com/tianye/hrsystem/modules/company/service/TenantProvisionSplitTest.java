package com.tianye.hrsystem.modules.company.service;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/**
 * SaaS 改造 P1-4：租户开通流水线的 SQL 拆分逻辑测试
 * （防止 mysqldump 条件注释/多行语句被错误切分）
 */
public class TenantProvisionSplitTest {

    private List<String> split(String script) throws Exception {
        Method m = TenantProvisionService.class.getDeclaredMethod("splitStatements", String.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, script);
    }

    @Test
    public void 单条语句_正确拆分() throws Exception {
        List<String> r = split("CREATE TABLE a (id INT);\n");
        assertEquals(1, r.size());
        assertTrue(r.get(0).contains("CREATE TABLE"));
    }

    @Test
    public void 多行语句_保持完整() throws Exception {
        List<String> r = split("CREATE TABLE a (\n  id INT,\n  name VARCHAR(10)\n);\n");
        assertEquals(1, r.size());
        assertTrue(r.get(0).contains("name VARCHAR(10)"));
    }

    @Test
    public void 条件注释行_被过滤() throws Exception {
        List<String> r = split("/*!40101 SET @saved_cs_client = @@character_set_client */;\nCREATE TABLE a (id INT);\n");
        assertEquals(1, r.size());
        assertFalse(r.get(0).contains("40101"));
    }

    @Test
    public void 占位视图多行块_整体过滤不产生碎片() throws Exception {
        // mysqldump 视图占位块：首行 /*!50001 开头，末行 */; 结尾，中间是裸列定义
        String script = "CREATE TABLE real_t (id INT);\n"
                + "/*!50001 CREATE VIEW `v` AS SELECT \n"
                + " 1 AS `col1`,\n"
                + " 1 AS `col2`*/;\n"
                + "CREATE TABLE real_t2 (id INT);\n";
        List<String> r = split(script);
        assertEquals(2, r.size());
        for (String s : r) {
            assertTrue(s.contains("real_t"));
            assertFalse(s.contains("50001"));
        }
    }

    @Test
    public void 普通注释与空行_跳过() throws Exception {
        List<String> r = split("-- 注释\n\n#井号注释\nINSERT INTO t VALUES (1);\n");
        assertEquals(1, r.size());
    }
}
