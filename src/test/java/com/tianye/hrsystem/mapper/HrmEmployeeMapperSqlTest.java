package com.tianye.hrsystem.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HrmEmployeeMapperSqlTest {

    @Test
    public void queryPageList_shouldExposeEmployeeIdentityAndBirthdayFieldsAsCamelCaseKeys() throws Exception {
        String xml = readMapperXml();
        String queryPageListSql = extractQueryPageListSql(xml);

        Assert.assertTrue("员工列表必须返回证件类型驼峰字段 idType", queryPageListSql.contains("a.id_type as idType"));
        Assert.assertTrue("员工列表必须返回证件号码驼峰字段 idNumber", queryPageListSql.contains("a.id_number as idNumber"));
        Assert.assertTrue("员工列表必须返回出生日期驼峰字段 dateOfBirth", queryPageListSql.contains("as dateOfBirth"));
        Assert.assertTrue("员工列表必须返回生日字段 birthday", queryPageListSql.contains("a.birthday"));
        Assert.assertTrue("员工列表必须返回年龄字段 age", queryPageListSql.contains("a.age"));
        Assert.assertTrue("员工列表必须返回劳动合同签订次数 contractSignCount", queryPageListSql.contains("as contractSignCount"));
        Assert.assertTrue("劳动合同签订次数必须按员工合同表总记录数统计", queryPageListSql.contains("count(1) from hrm_employee_contract"));
        Assert.assertTrue("员工列表必须关联离职信息表", queryPageListSql.contains("hrm_employee_quit_info"));
        Assert.assertTrue("员工列表必须返回离职时间 quitTime", queryPageListSql.contains("as quitTime"));
        Assert.assertTrue("员工列表必须返回离职原因 quitReason", queryPageListSql.contains("as quitReason"));
        Assert.assertTrue("员工列表离职原因必须来自 quit_reason", queryPageListSql.contains("quit_reason"));
        Assert.assertTrue("员工基础信息导出学历必须从教育经历兜底", queryPageListSql.contains("hrm_employee_education_experience"));
        Assert.assertTrue("员工基础信息导出学历必须优先主表最高学历再兜底教育经历", queryPageListSql.contains("coalesce(a.highest_education"));
        Assert.assertTrue("员工基础信息导出必须能读取紧急联系人", queryPageListSql.contains("as emergencyContact"));
        Assert.assertTrue("员工基础信息导出必须能读取与本人关系", queryPageListSql.contains("as emergencyRelation"));
        Assert.assertTrue("员工基础信息导出必须能读取联系人电话", queryPageListSql.contains("as emergencyPhone"));
        Assert.assertTrue("紧急联系人必须来自员工联系人表", queryPageListSql.contains("hrm_employee_contacts"));
        Assert.assertTrue("员工基础信息导出必须返回第一次合同开始时间", queryPageListSql.contains("as firstContractStartTime"));
        Assert.assertTrue("员工基础信息导出必须返回最近一次合同开始时间", queryPageListSql.contains("as latestContractStartTime"));
        Assert.assertTrue("员工基础信息导出必须返回最近一次合同结束时间", queryPageListSql.contains("as latestContractEndTime"));
        Assert.assertTrue("员工基础信息导出必须返回最后一次合同结束时间", queryPageListSql.contains("as lastContractEndTime"));
        Assert.assertTrue("员工基础信息导出必须返回最近一次合同类型用于无固定期限展示", queryPageListSql.contains("as latestContractType"));
        Assert.assertTrue("员工基础信息导出必须返回最后一次合同类型用于结束时间展示", queryPageListSql.contains("as lastContractType"));
        Assert.assertTrue("最近一次合同必须按开始时间倒序取数", queryPageListSql.contains("order by fc.start_time desc"));
        Assert.assertTrue("员工列表必须返回所属体系 affiliationSystem", queryPageListSql.contains("a.affiliation_system as affiliationSystem"));
        Assert.assertTrue("员工列表必须返回休息制度 restType", queryPageListSql.contains("a.rest_type as restType"));
        Assert.assertTrue("员工列表必须返回是否连班 isContinuousShift", queryPageListSql.contains("a.is_continuous_shift as isContinuousShift"));
        Assert.assertTrue("员工列表必须返回员工级普通全勤金额 ordinaryFullAttendanceAmount",
                queryPageListSql.contains("a.ordinary_full_attendance_amount as ordinaryFullAttendanceAmount"));
        Assert.assertTrue("员工列表必须返回员工级领导全勤金额 leaderFullAttendanceAmount",
                queryPageListSql.contains("a.leader_full_attendance_amount as leaderFullAttendanceAmount"));
    }

    @Test
    public void queryPageList_shouldSupportAdvancedEmployeeFilters() throws Exception {
        String xml = readMapperXml();
        String employeeListCondition = extractEmployeeListConditionSql(xml);

        Assert.assertTrue("高级查询必须支持年龄下限 ageMin", employeeListCondition.contains("data.ageMin"));
        Assert.assertTrue("高级查询必须支持年龄上限 ageMax", employeeListCondition.contains("data.ageMax"));
        Assert.assertTrue("高级查询必须支持出生月份 birthMonth", employeeListCondition.contains("data.birthMonth"));
        Assert.assertTrue("高级查询必须支持司龄下限 companyAgeMin", employeeListCondition.contains("data.companyAgeMin"));
        Assert.assertTrue("高级查询必须支持司龄上限 companyAgeMax", employeeListCondition.contains("data.companyAgeMax"));
        Assert.assertTrue("高级查询必须支持劳动合同结束时间 contractEndTime", employeeListCondition.contains("data.contractEndTime"));
        Assert.assertTrue("高级查询必须支持劳动合同签订次数 contractSignCount", employeeListCondition.contains("data.contractSignCount"));
        Assert.assertTrue("高级查询必须支持用工性质 employmentNature", employeeListCondition.contains("data.employmentNature"));
        Assert.assertTrue("高级查询必须支持政治面貌 politicalStatus", employeeListCondition.contains("data.politicalStatus"));
        Assert.assertTrue("高级查询必须支持离职时间 quitTime", employeeListCondition.contains("data.quitTime"));
        Assert.assertTrue("离职时间筛选必须使用 plan_quit_time", employeeListCondition.contains("plan_quit_time"));
        Assert.assertTrue("转正日期范围必须只在首尾日期完整时生效", employeeListCondition.contains("data.becomeTime != null and data.becomeTime.size == 2"));
    }


    @Test
    public void employeeDataFieldList_shouldExposeCamelCaseAliasesForDynamicExportFields() throws Exception {
        String xml = readMapperXml("mapper/HrmEmployeeDataMapper.xml");

        Assert.assertTrue("动态字段必须返回 fieldName 驼峰别名", xml.contains("as fieldName"));
        Assert.assertTrue("动态字段必须返回 fieldValue 驼峰别名", xml.contains("as fieldValue"));
        Assert.assertTrue("动态字段必须返回 fieldValueDesc 驼峰别名", xml.contains("as fieldValueDesc"));
        Assert.assertTrue("动态字段必须返回中文字段名 name", xml.contains("b.name as name"));
    }

    @Test
    public void queryHasOverTimePayEmpList_shouldRequireProductionAffiliationAndFixedRestType() throws Exception {
        String xml = readMapperXml();
        String query = extractSelectSql(xml, "queryHasOverTimePayEmpList");

        Assert.assertTrue("有加班费员工筛选必须使用员工所属体系字段", query.contains("affiliation_system = 2"));
        Assert.assertTrue("有加班费员工筛选必须同时要求固定月休4天休息制度", query.contains("rest_type = 2"));
        Assert.assertFalse("有加班费员工筛选不得再按部门名称判断生产体系", query.contains("质量部"));
        Assert.assertFalse("有加班费员工筛选不得再按岗位名称维护生产体系例外", query.contains("仓库"));
    }

    @Test
    public void queryDepartmentDetailExportList_shouldExportUndeletedOnJobEmployeesWithoutPageFilters() throws Exception {
        String xml = readMapperXml();
        String query = normalizeSql(extractSelectSql(xml, "queryDepartmentDetailExportList"));

        Assert.assertTrue("部门明细导出必须只限制未删除且在职员工",
                query.contains("where a.is_del = 0 and a.entry_status in (1,3)"));
        Assert.assertTrue("部门明细导出必须返回教育经历专业 major", query.contains("as major"));
        Assert.assertTrue("部门明细导出必须返回员工表试用期字段用于实习期一年以上统计", query.contains("a.probation"));
        Assert.assertTrue("专业必须来自员工教育经历", query.contains("hrm_employee_education_experience"));
        Assert.assertTrue("部门明细导出必须返回最新合同到期日", query.contains("as lastContractEndTime"));
        Assert.assertTrue("部门明细导出必须返回最新合同类型用于无固定期限展示", query.contains("as lastContractType"));
        Assert.assertFalse("部门明细导出不得复用页面高级筛选条件", query.contains("employeeListCondition"));
        Assert.assertFalse("部门明细导出不得只导出页面指定员工列表", query.contains("employeeIds"));
        Assert.assertTrue("部门明细导出必须返回员工是否有全勤字段 fullAttendance",
                query.contains("a.full_attendance as fullAttendance"));
    }

    @Test
    public void queryDepartmentDetailExportList_shouldUseLatestEmployeeContractEndTimeFromContractList() throws Exception {
        String xml = readMapperXml();
        String query = normalizeSql(extractSelectSql(xml, "queryDepartmentDetailExportList"));

        Assert.assertTrue("合同到期日必须来自员工合同表的合同结束日期", query.contains("select date_format(fc.end_time, '%Y-%m-%d') from hrm_employee_contract fc"));
        Assert.assertTrue("合同到期日必须按当前员工关联合同", query.contains("where fc.employee_id = a.employee_id"));
        Assert.assertTrue("合同到期日必须按员工合同列表排序取最新一条",
                query.contains("order by coalesce(fc.sort, -1) desc, fc.start_time desc, fc.contract_id desc limit 1"));
        Assert.assertFalse("合同到期日不能因为合同开始日期为空而跳过员工合同记录", query.contains("fc.start_time is not null"));
    }

    @Test
    public void employeeFullAttendanceAmountMigrationScript_shouldAddEmployeeAmountColumns() {
        String scriptPath = "docs/sql/2026-07-22_hrm_employee_full_attendance_amount.sql";

        Assert.assertTrue("员工级全勤金额字段迁移脚本必须存在", Files.exists(Paths.get(scriptPath)));
    }

    @Test
    public void employeeContinuousShiftMigrationScript_shouldAddEmployeeContinuousShiftColumn() throws Exception {
        String scriptPath = "docs/sql/2026-07-25_hrm_employee_is_continuous_shift.sql";

        Assert.assertTrue("员工是否连班字段迁移脚本必须存在", Files.exists(Paths.get(scriptPath)));
        String script = new String(Files.readAllBytes(Paths.get(scriptPath)), StandardCharsets.UTF_8);
        Assert.assertTrue("迁移脚本必须检测 is_continuous_shift 字段", script.contains("is_continuous_shift"));
        Assert.assertTrue("迁移脚本必须写明是否连班业务含义", script.contains("是否连班"));
        Assert.assertTrue("是否连班脚本不应依赖 rest_type 字段先存在", script.contains("AFTER `full_attendance`"));
        Assert.assertFalse("是否连班脚本不应依赖 rest_type 字段先存在", script.contains("AFTER `rest_type`"));
    }

    private String readMapperXml() throws Exception {
        return readMapperXml("mapper/HrmEmployeeMapper.xml");
    }

    private String readMapperXml(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            Assert.assertNotNull(resourcePath + " must exist on test classpath", inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String extractQueryPageListSql(String xml) {
        String selectStart = "<select id=\"queryPageList\"";
        int startIndex = xml.indexOf(selectStart);
        Assert.assertTrue("queryPageList select must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</select>", startIndex);
        Assert.assertTrue("queryPageList select must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
    }

    private String extractEmployeeListConditionSql(String xml) {
        String sqlStart = "<sql id=\"employeeListCondition\"";
        int startIndex = xml.indexOf(sqlStart);
        Assert.assertTrue("employeeListCondition sql must exist", startIndex >= 0);
        int endIndex = xml.indexOf("</sql>", startIndex);
        Assert.assertTrue("employeeListCondition sql must be closed", endIndex > startIndex);
        return xml.substring(startIndex, endIndex);
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
