package com.tianye.hrsystem.imple.employee;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HrmEmployeeServiceImplDepartmentDetailCostTest {

    @Test
    public void exportDepartmentDetail_shouldLoadPerformanceAndLatestInsuranceCostSources() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("部门明细薪资选项必须同时加载 41001 / 绩效工资",
                source.contains(".in(HrmSalaryArchivesOption::getCode, 10101, 10102, 10103, 41001)"));
        Assert.assertTrue("部门明细导出必须加载最近有效社保月员工记录",
                source.contains("buildLatestDepartmentDetailInsuranceCostMap"));
        Assert.assertTrue("最近有效社保月必须按年月倒序取",
                source.contains(".orderByDesc(HrmInsuranceMonthEmpRecord::getYear)")
                        && source.contains(".orderByDesc(HrmInsuranceMonthEmpRecord::getMonth)"));
        Assert.assertTrue("部门明细导出必须按本次未删除员工范围加载社保成本",
                source.contains(".in(HrmInsuranceMonthEmpRecord::getEmployeeId, employeeIds)"));
        Assert.assertTrue("企业社保成本必须写回员工导出行供部门 sheet 汇总",
                source.contains("employee.put(\"corporateInsuranceAmount\""));
        Assert.assertTrue("企业公积金成本必须写回员工导出行供部门 sheet 汇总",
                source.contains("employee.put(\"corporateProvidentFundAmount\""));
        Assert.assertTrue("部门明细导出必须读取最新基本工资设置中的普通/领导全勤金额",
                source.contains("salaryBasicService.findAll()"));
        Assert.assertTrue("部门明细 workbook 必须传入普通员工全勤金额",
                source.contains("salaryBasic.getOrdinaryFullAttendanceAmount()"));
        Assert.assertTrue("部门明细 workbook 必须传入领导全勤金额",
                source.contains("salaryBasic.getLeaderFullAttendanceAmount()"));
    }

    @Test
    public void exportDepartmentDetail_shouldBuildEncryptedArchiveForAdministrativeManager() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("行政经理下载部门明细必须进入跨公司加密压缩包分支",
                source.contains("EmployeeDepartmentDetailCrossCompanyExportSupport.isAdministrativeManager"));
        Assert.assertTrue("行政经理下载必须遍历非成都公司白名单",
                source.contains("EmployeeDepartmentDetailCrossCompanyExportSupport.companyIds()"));
        Assert.assertTrue("每家公司明细必须在切换公司上下文后独立构建",
                source.contains("EmployeeDepartmentDetailCrossCompanyExportSupport.withCompanyContext"));
        Assert.assertTrue("跨公司下载必须生成公司名+部门明细的 Excel 文件名",
                source.contains("EmployeeDepartmentDetailCrossCompanyExportSupport.companyDepartmentFileName"));
        Assert.assertTrue("跨公司下载必须额外生成集团总表",
                source.contains("EmployeeDepartmentDetailGroupSummarySupport.buildWorkbook"));
        Assert.assertTrue("跨公司下载必须用随机密码生成 AES 加密 ZIP",
                source.contains("EmployeeDepartmentDetailCrossCompanyExportSupport.generateArchivePassword()")
                        && source.contains("EmployeeDepartmentDetailCrossCompanyExportSupport.buildEncryptedZip"));
        Assert.assertTrue("跨公司下载必须通过响应头返回压缩包密码",
                source.contains("X-Archive-Password"));
        Assert.assertTrue("行政经理下载文件必须是 zip",
                source.contains("部门明细.zip"));
    }
}
