package com.tianye.hrsystem.modules.salary.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HrmSalaryArchivesMapperSqlTest {

    @Test
    public void querySalaryArchivesList_shouldSupportActiveAndDepartedEmployeeStatusFilters() throws Exception {
        String query = normalizeSql(extractSelectSql(readMapperXml(), "querySalaryArchivesList"));

        Assert.assertTrue("薪资档案在职筛选必须使用 status=11",
                query.contains("data.status == '11'"));
        Assert.assertTrue("薪资档案在职筛选必须按员工入职状态过滤",
                query.contains("and a.entry_status in (1,3)"));
        Assert.assertTrue("薪资档案离职筛选必须使用 status=15",
                query.contains("data.status == '15'"));
        Assert.assertTrue("薪资档案离职筛选必须按员工离职状态过滤",
                query.contains("and a.entry_status = 4"));
    }

    private String readMapperXml() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mapper/HrmSalaryArchivesMapper.xml")) {
            Assert.assertNotNull("HrmSalaryArchivesMapper.xml must exist on test classpath", inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String extractSelectSql(String xml, String id) {
        String selectStart = "<select id=\"" + id + "\"";
        int startIndex = xml.indexOf(selectStart);
        Assert.assertTrue(id + " select must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</select>", startIndex);
        Assert.assertTrue(id + " select must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
