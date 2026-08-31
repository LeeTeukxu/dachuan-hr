package com.tianye.hrsystem.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HrmProduceAttendanceMapperXmlTest {

    @Test
    public void queryProduceAttendanceList_shouldAliasDatabaseColumnsToFrontendFields() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmProduceAttendanceMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "queryProduceAttendanceList");

        Assert.assertFalse("考勤汇总列表不能使用 select *，否则 positive_attendance 无法映射到 actualAttendance",
                query.contains("select *"));
        Assert.assertTrue("实际出勤天数必须映射为 actualAttendance",
                query.contains("positive_attendance as actualAttendance"));
        Assert.assertTrue("应计出勤天数必须映射为 accruedAttendance",
                query.contains("probation_attendance as accruedAttendance"));
        Assert.assertTrue("加班小时必须显式映射为 workOverTime",
                query.contains("end as workOverTime"));
        Assert.assertTrue("加班工资必须显式映射为 overtimePay",
                query.contains("end as overtimePay"));
        Assert.assertTrue("夜班次数必须显式映射为 nightShift",
                query.contains("end as nightShift"));
        Assert.assertTrue("夜班补贴必须显式映射为 nightSubsidy",
                query.contains("end as nightSubsidy"));
        Assert.assertTrue("员工 ID 必须显式映射为 employeeId",
                query.contains("employee_id as employeeId"));
        Assert.assertTrue("员工姓名必须显式映射为 employeeName",
                query.contains("employee_name as employeeName"));
        Assert.assertTrue("主键必须显式映射为 summaryId",
                query.contains("summary_id as summaryId"));
    }

    @Test
    public void queryProduceAttendanceList_shouldZeroOvertimeNightFieldsForUnqualifiedEmployees() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmProduceAttendanceMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "queryProduceAttendanceList");

        Assert.assertTrue("考勤汇总列表必须关联员工档案判断加班/夜班资格",
                query.contains("left join hrm_employee"));
        Assert.assertTrue("考勤汇总列表加班/夜班资格必须同时要求生产体系和固定月休4天",
                query.contains("affiliation_system = 2") && query.contains("rest_type = 2"));
        Assert.assertTrue("非目标员工列表加班小时必须展示为 0",
                query.contains("else 0 end as workOverTime"));
        Assert.assertTrue("非目标员工列表加班工资必须展示为 0",
                query.contains("else 0 end as overtimePay"));
        Assert.assertTrue("非目标员工列表夜班次数必须展示为 0",
                query.contains("else 0 end as nightShift"));
        Assert.assertTrue("非目标员工列表夜班补贴必须展示为 0",
                query.contains("else 0 end as nightSubsidy"));
    }

    @Test
    public void queryProduceAttendanceList_shouldFilterByEmployeeDeptIds() throws Exception {
        String boSource = new String(Files.readAllBytes(Paths.get("src/main/java/com/tianye/hrsystem/entity/bo/QueryMonthAttendanceBO.java")), StandardCharsets.UTF_8);
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmProduceAttendanceMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "queryProduceAttendanceList");

        Assert.assertTrue("考勤汇总查询入参必须支持真实部门 ID 集合",
                boSource.contains("List<Long> deptIds"));
        Assert.assertTrue("部门筛选必须使用员工档案部门字段，不能复用 department 所属体系字段",
                query.contains("data.deptIds") && query.contains("e.dept_id in"));
        Assert.assertTrue("部门筛选必须按 deptIds 集合展开，支持多部门查询",
                query.contains("collection=\"data.deptIds\""));
    }

    @Test
    public void queryAdministrativeAttendanceReportMetrics_shouldAggregateDingTalkReportFieldsThroughMissingCardSplit() throws Exception {
        String mapperSource = new String(Files.readAllBytes(Paths.get("src/main/java/com/tianye/hrsystem/mapper/HrmProduceAttendanceMapper.java")), StandardCharsets.UTF_8);
        String xml = new String(Files.readAllBytes(Paths.get("src/main/resources/mapper/HrmProduceAttendanceMapper.xml")), StandardCharsets.UTF_8);
        String query = extractSelect(xml, "queryAdministrativeAttendanceReportMetrics");

        Assert.assertTrue("Mapper 必须暴露行政体系考勤导出报表聚合方法",
                mapperSource.contains("queryAdministrativeAttendanceReportMetrics"));
        Assert.assertTrue("导出异常字段必须读取已同步钉钉报表数据",
                query.contains("from hrm_attendance_report_data"));
        Assert.assertTrue("导出需按员工集合限制范围",
                query.contains("collection=\"employeeIds\""));
        Assert.assertTrue("上班缺卡次数必须单独聚合",
                query.contains("field_name = '上班缺卡次数'") && query.contains("onDutyMissingCardCount"));
        Assert.assertTrue("下班缺卡次数必须单独聚合",
                query.contains("field_name = '下班缺卡次数'") && query.contains("offDutyMissingCardCount"));
        Assert.assertTrue("旷工、迟到、早退也必须随同导出聚合",
                query.contains("旷工天数") && query.contains("迟到次数") && query.contains("早退次数"));
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
