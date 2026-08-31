package com.tianye.hrsystem.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HrmAdditionalMapperXmlTest {

    @Test
    public void queryAdditionalList_shouldFilterBySelectedYearAndMonth() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmAdditionalMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "queryAdditionalList");

        Assert.assertTrue("附加累计列表查询必须识别年份筛选字段", query.contains("data.year"));
        Assert.assertTrue("附加累计列表查询必须按年份过滤", query.contains("a.`year` = #{data.year}"));
        Assert.assertTrue("附加累计列表查询必须识别月份筛选字段", query.contains("data.month"));
        Assert.assertTrue("附加累计列表查询必须按月份过滤", query.contains("a.`month` = #{data.month}"));
    }

    @Test
    public void queryAdditionalBO_shouldAcceptYearAndMonthFilters() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/tianye/hrsystem/modules/additional/bo/QueryAdditionalBO.java")), StandardCharsets.UTF_8);

        Assert.assertTrue("附加累计查询 BO 必须接收 year", source.contains("private Integer year"));
        Assert.assertTrue("附加累计查询 BO 必须接收 month", source.contains("private Integer month"));
    }

    private String extractSelect(String xml, String id) {
        String selectStart = "<select id=\"" + id + "\"";
        int startIndex = xml.indexOf(selectStart);
        Assert.assertTrue(id + " select must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</select>", startIndex);
        Assert.assertTrue(id + " select must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
    }
}
