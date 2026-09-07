package com.tianye.hrsystem.imple;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryMonthAttendanceBO;
import com.tianye.hrsystem.entity.bo.SyncProduceAttendanceBO;
import com.tianye.hrsystem.entity.bo.UpdateProduceAttendanceCellBO;
import com.tianye.hrsystem.entity.vo.AdministrativeAttendanceReportMetricVO;
import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;
import com.tianye.hrsystem.mapper.HrmProduceAttendanceMapper;
import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryBasicMapper;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryConfigService;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmOvertimeNightStatisticsDetailRepository;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmOvertimeNightStatisticsService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmProduceAttendanceServiceImplTest {

    @InjectMocks
    private HrmProduceAttendanceServiceImpl service;

    @Mock
    private HrmProduceAttendanceMapper produceAttendanceMapper;

    @Mock
    private IHrmOvertimeNightStatisticsService overtimeNightStatisticsService;

    @Mock
    private hrmOvertimeNightStatisticsDetailRepository overtimeNightStatisticsDetailRepository;

    @Mock
    private HrmSalaryBasicMapper salaryBasicMapper;

    @Mock
    private hrmEmployeeRepository employeeRepository;

    @Mock
    private hrmDeptRepository deptRepository;

    @Mock
    private tbattendanceuserRepository attendanceUserRepository;

    @Mock
    private tbattendanceapproveRepository attendanceApproveRepository;

    @Mock
    private HrmSalaryConfigService salaryConfigService;

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void downloadAdministrativeAttendance_shouldExportSelectedMonthTemplateThroughColumnS() throws Exception {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setCompanyName("湖北田野农谷生物科技有限公司");
        CompanyContext.set(loginUserInfo);

        QueryMonthAttendanceBO bo = new QueryMonthAttendanceBO();
        bo.setYear(2026);
        bo.setMonth(6);

        HrmEmployee administrativeEmployee = new HrmEmployee();
        administrativeEmployee.setEmployeeId(1001L);
        administrativeEmployee.setEmployeeName("张三");
        administrativeEmployee.setDeptId(2001L);
        administrativeEmployee.setAffiliationSystem(1);
        administrativeEmployee.setIsDel(0);

        HrmEmployee productionEmployee = new HrmEmployee();
        productionEmployee.setEmployeeId(1002L);
        productionEmployee.setEmployeeName("李四");
        productionEmployee.setDeptId(2002L);
        productionEmployee.setAffiliationSystem(2);
        productionEmployee.setIsDel(0);
        when(employeeRepository.findAll()).thenReturn(Arrays.asList(administrativeEmployee, productionEmployee));

        HrmDept dept = new HrmDept();
        dept.setDeptId(2001L);
        dept.setName("行政人力资源部");
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(2001L))).thenReturn(Collections.singletonList(dept));

        HrmOvertimeNightStatisticsDetail adminDetail = statisticsDetail(1001L, "张三", "行政人力资源部");
        adminDetail.setExpectedAttendanceDays(23);
        adminDetail.setActualAttendanceDays(22);
        adminDetail.setAccruedAttendanceHours(new BigDecimal("184.00"));
        HrmOvertimeNightStatisticsDetail productionDetail = statisticsDetail(1002L, "李四", "生产部");
        productionDetail.setExpectedAttendanceDays(25);
        productionDetail.setActualAttendanceDays(25);
        productionDetail.setAccruedAttendanceHours(new BigDecimal("200.00"));
        when(overtimeNightStatisticsDetailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 6))
                .thenReturn(Arrays.asList(adminDetail, productionDetail));

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(1001L);
        attendanceUser.setUserId("ding-1001");
        when(attendanceUserRepository.findAllByEmpIdIn(Collections.singletonList(1001L)))
                .thenReturn(Collections.singletonList(attendanceUser));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(any(), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(
                        approval("a1", "ding-1001", "请假", "事假", "8", "小时"),
                        approval("a2", "ding-1001", "请假", "病假", "270", "分钟"),
                        approval("a3", "ding-1001", "请假", "调休", "2", "小时"),
                        approval("a4", "ding-1001", "请假", "年假", "1", "天"),
                        approval("a5", "ding-1001", "出差", "出差", "8", "小时"),
                        approval("a8", "ding-1001", "外出", "外出", "4", "小时"),
                        approval("a6", "ding-1001", "加班", "加班", "5.5", "小时"),
                        canceledApproval("a7", "ding-1001", "请假", "事假", "99", "小时")
                ));

        AdministrativeAttendanceReportMetricVO metric = new AdministrativeAttendanceReportMetricVO();
        metric.setEmployeeId(1001L);
        metric.setAbsenteeismDays(new BigDecimal("0.50"));
        metric.setLateCount(new BigDecimal("2"));
        metric.setEarlyCount(new BigDecimal("1"));
        metric.setOnDutyMissingCardCount(new BigDecimal("3"));
        metric.setOffDutyMissingCardCount(new BigDecimal("4"));
        when(produceAttendanceMapper.queryAdministrativeAttendanceReportMetrics(any(Date.class), any(Date.class), any()))
                .thenReturn(Collections.singletonList(metric));

        MockHttpServletResponse response = new MockHttpServletResponse();
        stubAdministrativePayDay();
        service.downloadAdministrativeAttendance(bo, response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains("行政体系考勤"));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Assert.assertEquals(1, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(0);
            Assert.assertEquals("2026年6月份行政后勤考勤统计表", cellText(sheet.getRow(0), 0));
            Assert.assertEquals("编制单位:湖北田野农谷生物科技有限公司", cellText(sheet.getRow(1), 0));
            Assert.assertTrue(cellText(sheet.getRow(1), 12).matches("制表时间：\\d{4}年\\d{1,2}月\\d{1,2}日"));
            Assert.assertEquals("部门", cellText(sheet.getRow(3), 2));
            Assert.assertEquals("月天数（天）", cellText(sheet.getRow(3), 3));
            Assert.assertEquals("加班（小时）", cellText(sheet.getRow(3), 18));
            assertNoContentAfterColumnS(sheet);

            Row dataRow = sheet.getRow(4);
            Assert.assertEquals("1", cellText(dataRow, 0));
            Assert.assertEquals("张三", cellText(dataRow, 1));
            Assert.assertEquals("行政人力资源部", cellText(dataRow, 2));
            Assert.assertEquals(30.0, cellNumber(dataRow, 3), 0.001);
            Assert.assertEquals(184.0, cellNumber(dataRow, 4), 0.001);
            Assert.assertEquals(167.0, cellNumber(dataRow, 5), 0.001);
            Assert.assertEquals(184.0, cellNumber(dataRow, 6), 0.001);
            Assert.assertEquals(8.0, cellNumber(dataRow, 7), 0.001);
            Assert.assertEquals(4.5, cellNumber(dataRow, 8), 0.001);
            Assert.assertEquals(2.0, cellNumber(dataRow, 9), 0.001);
            Assert.assertEquals(8.0, cellNumber(dataRow, 10), 0.001);
            Assert.assertEquals(1.0, cellNumber(dataRow, 11), 0.001);
            Assert.assertEquals(0.5, cellNumber(dataRow, 12), 0.001);
            Assert.assertEquals(2.0, cellNumber(dataRow, 13), 0.001);
            Assert.assertEquals(1.0, cellNumber(dataRow, 14), 0.001);
            Assert.assertEquals(3.0, cellNumber(dataRow, 15), 0.001);
            Assert.assertEquals(4.0, cellNumber(dataRow, 16), 0.001);
            Assert.assertEquals(7.0, cellNumber(dataRow, 17), 0.001);
            Assert.assertEquals(5.5, cellNumber(dataRow, 18), 0.001);
            assertCommentContains(dataRow.getCell(7), "事假", "发生时间");
            assertCommentContains(dataRow.getCell(8), "病假", "分钟÷60");
            assertCommentContains(dataRow.getCell(9), "调休");
            assertCommentContains(dataRow.getCell(10), "年假");
            assertCommentContains(dataRow.getCell(11), "出差", "本月合计：1.00 天");
            assertCommentContains(dataRow.getCell(12), "旷工");
            assertCommentContains(dataRow.getCell(13), "迟到", "每天打卡迟到记 1 次");
            assertCommentContains(dataRow.getCell(14), "早退");
            assertCommentContains(dataRow.getCell(15), "上班缺卡");
            assertCommentContains(dataRow.getCell(16), "下班缺卡");
            assertCommentContains(dataRow.getCell(17), "上班缺卡次数 + 下班缺卡次数", "7.00 次");
            assertCommentContains(dataRow.getCell(18), "加班", "本月合计：5.50 小时");
            Assert.assertNull("非批注列不应添加批注", dataRow.getCell(0).getCellComment());
            Assert.assertNull("production employee should not be exported", sheet.getRow(5));
        }
    }

    @Test
    public void downloadAdministrativeAttendance_shouldUseStatisticsActualHoursWithApprovalOvertimeAndDeductions() throws Exception {
        QueryMonthAttendanceBO bo = new QueryMonthAttendanceBO();
        bo.setYear(2026);
        bo.setMonth(6);

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1831601326890434565L);
        employee.setEmployeeName("吴晓霞");
        employee.setDeptId(2001L);
        employee.setAffiliationSystem(1);
        employee.setIsDel(0);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        HrmDept dept = new HrmDept();
        dept.setDeptId(2001L);
        dept.setName("行政人力资源部");
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(2001L))).thenReturn(Collections.singletonList(dept));

        HrmOvertimeNightStatisticsDetail detail = statisticsDetail(1831601326890434565L, "吴晓霞", "行政人力资源部");
        detail.setExpectedAttendanceDays(23);
        detail.setActualAttendanceDays(23);
        detail.setAccruedAttendanceHours(new BigDecimal("184.00"));
        when(overtimeNightStatisticsDetailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 6))
                .thenReturn(Collections.singletonList(detail));

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(1831601326890434565L);
        attendanceUser.setUserId("ding-wxx");
        when(attendanceUserRepository.findAllByEmpIdIn(Collections.singletonList(1831601326890434565L)))
                .thenReturn(Collections.singletonList(attendanceUser));

        tbattendanceapprove compensatoryLeave = approval("leave-1", "ding-wxx", "请假", "调休", "", "");
        compensatoryLeave.setWorkDate(date(2026, 6, 8));
        compensatoryLeave.setBeginTime(date(2026, 6, 8, 8, 0));
        compensatoryLeave.setEndTime(date(2026, 6, 8, 8, 30));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(any(), any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(
                        approval("ot-1", "ding-wxx", "加班", "加班", "1", "小时"),
                        approval("ot-2", "ding-wxx", "加班", "加班", "1", "小时"),
                        compensatoryLeave
                ));

        MockHttpServletResponse response = new MockHttpServletResponse();
        stubAdministrativePayDay();
        service.downloadAdministrativeAttendance(bo, response);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row dataRow = workbook.getSheetAt(0).getRow(4);
            Assert.assertEquals(184.0, cellNumber(dataRow, 4), 0.001);
            Assert.assertEquals(185.5, cellNumber(dataRow, 5), 0.001);
            Assert.assertEquals(184.0, cellNumber(dataRow, 6), 0.001);
            Assert.assertEquals(0.5, cellNumber(dataRow, 9), 0.001);
            Assert.assertEquals(2.0, cellNumber(dataRow, 18), 0.001);
        }
    }

    @Test
    public void downloadAdministrativeAttendance_shouldUseRefreshedStatisticsAttendanceHoursForFixedRestAdministrativeEmployee() throws Exception {
        QueryMonthAttendanceBO bo = new QueryMonthAttendanceBO();
        bo.setYear(2026);
        bo.setMonth(6);

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1831601326890434571L);
        employee.setEmployeeName("李凤皇");
        employee.setJobNumber("TYNG-142");
        employee.setDeptId(2001L);
        employee.setAffiliationSystem(1);
        employee.setRestType(2);
        employee.setIsDel(0);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        HrmDept dept = new HrmDept();
        dept.setDeptId(2001L);
        dept.setName("行政人力资源部");
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(2001L))).thenReturn(Collections.singletonList(dept));

        HrmOvertimeNightStatisticsDetail staleDetail = statisticsDetail(1831601326890434571L, "李凤皇", "行政人力资源部");
        staleDetail.setExpectedAttendanceDays(23);
        staleDetail.setActualAttendanceDays(22);
        staleDetail.setAccruedAttendanceHours(new BigDecimal("184.00"));
        when(overtimeNightStatisticsDetailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 6))
                .thenReturn(Collections.singletonList(staleDetail));

        QueryOvertimeNightStatisticsPageVO refreshedStatistics = new QueryOvertimeNightStatisticsPageVO();
        refreshedStatistics.setEmployeeId(1831601326890434571L);
        refreshedStatistics.setEmployeeName("李凤皇");
        refreshedStatistics.setExpectedAttendanceDays(25);
        refreshedStatistics.setActualAttendanceHours(new BigDecimal("196.00"));
        refreshedStatistics.setAccruedAttendanceHours(new BigDecimal("200.00"));
        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(refreshedStatistics)));

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(1831601326890434571L);
        attendanceUser.setUserId("ding-lfh");
        when(attendanceUserRepository.findAllByEmpIdIn(Collections.singletonList(1831601326890434571L)))
                .thenReturn(Collections.singletonList(attendanceUser));

        tbattendanceapprove compensatoryLeave = approval("leave-lfh", "ding-lfh", "请假", "调休", "4", "小时");
        compensatoryLeave.setWorkDate(date(2026, 6, 17));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(compensatoryLeave));

        MockHttpServletResponse response = new MockHttpServletResponse();
        stubAdministrativePayDay();
        service.downloadAdministrativeAttendance(bo, response);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row dataRow = workbook.getSheetAt(0).getRow(4);
            Assert.assertEquals(200.0, cellNumber(dataRow, 4), 0.001);
            Assert.assertEquals(196.0, cellNumber(dataRow, 5), 0.001);
            Assert.assertEquals(200.0, cellNumber(dataRow, 6), 0.001);
            Assert.assertEquals(4.0, cellNumber(dataRow, 9), 0.001);
        }
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldUpdateExistingAttendanceByEmployeeAndMonth() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        QueryOvertimeNightStatisticsPageVO statistics = statisticsRow();
        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statistics)));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(new HrmProduceAttendance()
                        .setSummaryId(9L)
                        .setEmployeeId(1001L)
                        .setYear(2026)
                        .setMonth(6)
                        .setLoan(new BigDecimal("88"))));
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(overtimeNightStatisticsService).queryPageList(argThat(query ->
                "2026-06".equals(query.getMonth())
                        && Integer.valueOf(0).equals(query.getPageType())));
        verify(produceAttendanceMapper).updateById(argThat(row ->
                Long.valueOf(9L).equals(row.getSummaryId())
                        && decimalEquals("23.00", row.getPositiveAttendance())
                        && decimalEquals("25.00", row.getProbationAttendance())
                        && decimalEquals("8.50", row.getWorkOverTime())
                        && decimalEquals("170.00", row.getOvertimePay())
                        && Integer.valueOf(2).equals(row.getNightShift())
                        && decimalEquals("70.00", row.getNightSubsidy())
                        && decimalEquals("88", row.getLoan())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldInsertMissingAttendanceRows() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statisticsRow())));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setDeptId(2001L);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).insert(argThat(row ->
                Long.valueOf(1001L).equals(row.getEmployeeId())
                        && "张三".equals(row.getEmployeeName())
                        && Integer.valueOf(2).equals(row.getDepartment())
                        && decimalEquals("23.00", row.getPositiveAttendance())
                        && decimalEquals("25.00", row.getProbationAttendance())
                        && decimalEquals("170.00", row.getOvertimePay())
                        && decimalEquals("70.00", row.getNightSubsidy())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldRefreshExistingDepartmentFromEmployeeAffiliationSystem() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        QueryOvertimeNightStatisticsPageVO statistics = statisticsRow();
        statistics.setEmployeeName("潘红琼");
        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statistics)));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(new HrmProduceAttendance()
                        .setSummaryId(9L)
                        .setEmployeeId(1001L)
                        .setEmployeeName("潘红琼")
                        .setDepartment(1)
                        .setYear(2026)
                        .setMonth(6)));

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("潘红琼");
        employee.setDeptId(2001L);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).updateById(argThat(row ->
                Long.valueOf(9L).equals(row.getSummaryId())
                        && "潘红琼".equals(row.getEmployeeName())
                        && Integer.valueOf(2).equals(row.getDepartment())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldZeroOvertimeAndNightAmountsForProductionEmployeeWithoutFixedRest() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statisticsRow())));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setDeptId(2001L);
        employee.setAffiliationSystem(2);
        employee.setRestType(1);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).insert(argThat(row ->
                Long.valueOf(1001L).equals(row.getEmployeeId())
                        && decimalEquals("0.00", row.getWorkOverTime())
                        && decimalEquals("0.00", row.getOvertimePay())
                        && Integer.valueOf(0).equals(row.getNightShift())
                        && decimalEquals("0.00", row.getNightSubsidy())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldDefaultAdministrativeWhenEmployeeAffiliationMissing() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        QueryOvertimeNightStatisticsPageVO statistics = statisticsRow();
        statistics.setDeptName("生产部");
        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statistics)));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setDeptId(2001L);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).insert(argThat(row ->
                Long.valueOf(1001L).equals(row.getEmployeeId())
                        && Integer.valueOf(1).equals(row.getDepartment())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldFallbackDefaultAllowanceRatesWhenSalaryBasicMissing() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statisticsRow())));
        when(salaryBasicMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(produceAttendanceMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(new HrmProduceAttendance()
                        .setSummaryId(9L)
                        .setEmployeeId(1001L)
                        .setYear(2026)
                        .setMonth(6)));
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).updateById(argThat(row ->
                decimalEquals("102.00", row.getOvertimePay())
                        && decimalEquals("60.00", row.getNightSubsidy())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldUseAttendanceOvertimeHoursForAdministrativeAttendanceSummary() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        QueryOvertimeNightStatisticsPageVO statistics = statisticsRow();
        statistics.setEmployeeName("张明");
        statistics.setOvertimeHours(new BigDecimal("47.72"));
        statistics.setAttendanceOvertimeHours(BigDecimal.ZERO);
        statistics.setActualAttendanceHours(new BigDecimal("184.00"));
        statistics.setAccruedAttendanceHours(new BigDecimal("184.00"));
        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statistics)));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(new HrmProduceAttendance()
                        .setSummaryId(9L)
                        .setEmployeeId(1001L)
                        .setYear(2026)
                        .setMonth(6)));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).updateById(argThat(row ->
                Long.valueOf(9L).equals(row.getSummaryId())
                        && decimalEquals("23.00", row.getPositiveAttendance())
                        && decimalEquals("23.00", row.getProbationAttendance())
                        && decimalEquals("0.00", row.getWorkOverTime())
                        && decimalEquals("0.00", row.getOvertimePay())));
    }

    @Test
    public void syncFromOvertimeNightStatistics_shouldUsePersistedStatisticsDetailsForAttendanceDays() {
        SyncProduceAttendanceBO bo = new SyncProduceAttendanceBO();
        bo.setMonth("2026-06");

        HrmOvertimeNightStatisticsDetail firstDetail = statisticsDetail(1001L, "张三", "生产部");
        firstDetail.setActualAttendanceDays(26);
        firstDetail.setAccruedAttendanceHours(new BigDecimal("224.00"));
        firstDetail.setOvertimeHours(new BigDecimal("4.00"));
        firstDetail.setNightShiftCount(1);
        HrmOvertimeNightStatisticsDetail secondDetail = statisticsDetail(1001L, "张三", "生产部");
        secondDetail.setActualAttendanceDays(26);
        secondDetail.setAccruedAttendanceHours(new BigDecimal("224.00"));
        secondDetail.setOvertimeHours(new BigDecimal("6.00"));
        secondDetail.setNightShiftCount(2);
        when(overtimeNightStatisticsDetailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 6))
                .thenReturn(Arrays.asList(firstDetail, secondDetail));
        when(overtimeNightStatisticsService.queryPageList(any()))
                .thenReturn(new BasePage<>(1L, 10000L, 1L, Collections.singletonList(statisticsRow())));
        when(salaryBasicMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new HrmSalaryBasic()
                        .setOvertimePay(new BigDecimal("20"))
                        .setSubsidy(new BigDecimal("35"))));
        when(produceAttendanceMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(new HrmProduceAttendance()
                        .setSummaryId(9L)
                        .setEmployeeId(1001L)
                        .setYear(2026)
                        .setMonth(6)
                        .setLoan(new BigDecimal("88"))));

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setDeptId(2001L);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(employee));

        int synced = service.syncFromOvertimeNightStatistics(bo);

        Assert.assertEquals(1, synced);
        verify(produceAttendanceMapper).updateById(argThat(row ->
                Long.valueOf(9L).equals(row.getSummaryId())
                        && decimalEquals("26.00", row.getPositiveAttendance())
                        && decimalEquals("28.00", row.getProbationAttendance())
                        && decimalEquals("8.50", row.getWorkOverTime())
                        && decimalEquals("170.00", row.getOvertimePay())
                        && Integer.valueOf(2).equals(row.getNightShift())
                        && decimalEquals("70.00", row.getNightSubsidy())
                        && decimalEquals("88", row.getLoan())));
    }

    @Test
    public void updateProduceAttendanceCell_shouldUpdateWhitelistedBusinessFieldOnly() {
        when(produceAttendanceMapper.selectById(9L))
                .thenReturn(new HrmProduceAttendance().setSummaryId(9L).setEmployeeId(1001L));
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setAffiliationSystem(2);
        employee.setRestType(2);
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));

        UpdateProduceAttendanceCellBO bo = new UpdateProduceAttendanceCellBO();
        bo.setSummaryId(9L);
        bo.setField("overtimePay");
        bo.setValue("128.50");

        service.updateProduceAttendanceCell(bo);

        verify(produceAttendanceMapper).updateById(argThat(row ->
                Long.valueOf(9L).equals(row.getSummaryId())
                        && new BigDecimal("128.50").compareTo(row.getOvertimePay()) == 0));
    }

    @Test
    public void updateProduceAttendanceCell_shouldRejectOvertimeNightFieldsForUnqualifiedEmployee() {
        when(produceAttendanceMapper.selectById(9L))
                .thenReturn(new HrmProduceAttendance().setSummaryId(9L).setEmployeeId(1001L));
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(1001L);
        employee.setAffiliationSystem(2);
        employee.setRestType(1);
        when(employeeRepository.findById(1001L)).thenReturn(Optional.of(employee));

        UpdateProduceAttendanceCellBO bo = new UpdateProduceAttendanceCellBO();
        bo.setSummaryId(9L);
        bo.setField("overtimePay");
        bo.setValue("128.50");

        try {
            service.updateProduceAttendanceCell(bo);
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("生产体系且固定月休4天"));
        }
    }

    @Test
    public void applyOvertimeNightEligibility_shouldZeroImportedFieldsForUnqualifiedEmployee() {
        HrmProduceAttendance attendance = new HrmProduceAttendance()
                .setWorkOverTime(new BigDecimal("8.50"))
                .setOvertimePay(new BigDecimal("170.00"))
                .setNightShift(2)
                .setNightSubsidy(new BigDecimal("70.00"));
        HrmEmployee employee = new HrmEmployee();
        employee.setAffiliationSystem(2);
        employee.setRestType(1);

        HrmProduceAttendanceServiceImpl.applyOvertimeNightEligibility(attendance, employee);

        Assert.assertTrue(decimalEquals("0.00", attendance.getWorkOverTime()));
        Assert.assertTrue(decimalEquals("0.00", attendance.getOvertimePay()));
        Assert.assertEquals(Integer.valueOf(0), attendance.getNightShift());
        Assert.assertTrue(decimalEquals("0.00", attendance.getNightSubsidy()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateProduceAttendanceCell_shouldRejectIdentityFields() {
        UpdateProduceAttendanceCellBO bo = new UpdateProduceAttendanceCellBO();
        bo.setSummaryId(9L);
        bo.setField("employeeName");
        bo.setValue("李四");

        service.updateProduceAttendanceCell(bo);
    }

    private static QueryOvertimeNightStatisticsPageVO statisticsRow() {
        QueryOvertimeNightStatisticsPageVO statistics = new QueryOvertimeNightStatisticsPageVO();
        statistics.setEmployeeId(1001L);
        statistics.setEmployeeName("张三");
        statistics.setActualAttendanceHours(new BigDecimal("184.00"));
        statistics.setAccruedAttendanceHours(new BigDecimal("200.00"));
        statistics.setOvertimeHours(new BigDecimal("8.50"));
        statistics.setNightShiftCount(2);
        return statistics;
    }

    private static HrmOvertimeNightStatisticsDetail statisticsDetail(Long employeeId, String employeeName, String deptName) {
        HrmOvertimeNightStatisticsDetail detail = new HrmOvertimeNightStatisticsDetail();
        detail.setStatYear(2026);
        detail.setStatMonth(6);
        detail.setEmployeeId(employeeId);
        detail.setEmployeeName(employeeName);
        detail.setDeptName(deptName);
        return detail;
    }

    private static tbattendanceapprove approval(String id,
                                                String userId,
                                                String tagName,
                                                String subType,
                                                String duration,
                                                String durationUnit) {
        tbattendanceapprove approval = new tbattendanceapprove();
        approval.setId(id);
        approval.setUserId(userId);
        approval.setTagName(tagName);
        approval.setSubType(subType);
        approval.setDuration(duration);
        approval.setDurationUnit(durationUnit);
        approval.setWorkDate(date(2026, 6, 10));
        approval.setBeginTime(date(2026, 6, 10, 9, 0));
        approval.setEndTime(date(2026, 6, 10, 18, 0));
        return approval;
    }

    private static tbattendanceapprove canceledApproval(String id,
                                                        String userId,
                                                        String tagName,
                                                        String subType,
                                                        String duration,
                                                        String durationUnit) {
        tbattendanceapprove approval = approval(id, userId, tagName, subType, duration, durationUnit);
        approval.setStatisticsStatus("取消至统计");
        return approval;
    }

    private static Date date(int year, int month, int day) {
        return date(year, month, day, 0, 0);
    }

    private static Date date(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, 0);
        return calendar.getTime();
    }

    private void stubAdministrativePayDay() {
        HrmSalaryConfig salaryConfig = new HrmSalaryConfig();
        salaryConfig.setPayDay(10);
        when(salaryConfigService.getOne(any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(salaryConfig);
    }

    private static void assertCommentContains(Cell cell, String... fragments) {
        Assert.assertNotNull("目标单元格必须存在", cell);
        Assert.assertNotNull("目标单元格必须带批注", cell.getCellComment());
        String comment = cell.getCellComment().getString().getString();
        for (String fragment : fragments) {
            Assert.assertTrue("批注应包含：" + fragment + "\n实际批注：" + comment,
                    comment.contains(fragment));
        }
    }

    private static String cellText(Row row, int columnIndex) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        return new DataFormatter().formatCellValue(cell);
    }

    private static double cellNumber(Row row, int columnIndex) {
        String value = cellText(row, columnIndex);
        return value == null || value.trim().isEmpty() ? 0.0 : Double.parseDouble(value.trim());
    }

    private static void assertNoContentAfterColumnS(Sheet sheet) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Assert.assertTrue("row " + (rowIndex + 1) + " should not contain cells after S",
                    row.getLastCellNum() <= 19);
        }
    }

    @Test
    public void downloadAdministrativeAttendance_shouldFallbackToProduceAttendanceWhenStatisticsDetailEmpty() throws Exception {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setCompanyName("测试公司");
        CompanyContext.set(loginUserInfo);

        QueryMonthAttendanceBO bo = new QueryMonthAttendanceBO();
        bo.setYear(2026);
        bo.setMonth(7);

        HrmEmployee administrativeEmployee = new HrmEmployee();
        administrativeEmployee.setEmployeeId(2001L);
        administrativeEmployee.setEmployeeName("王五");
        administrativeEmployee.setDeptId(3001L);
        administrativeEmployee.setAffiliationSystem(1);
        administrativeEmployee.setIsDel(0);
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(administrativeEmployee));

        HrmDept dept = new HrmDept();
        dept.setDeptId(3001L);
        dept.setName("行政部");
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(3001L))).thenReturn(Collections.singletonList(dept));

        when(overtimeNightStatisticsDetailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 7))
                .thenReturn(Collections.emptyList());

        HrmProduceAttendance attendance = new HrmProduceAttendance();
        attendance.setEmployeeId(2001L);
        attendance.setEmployeeName("王五");
        attendance.setYear(2026);
        attendance.setMonth(7);
        attendance.setDepartment(1);
        attendance.setPositiveAttendance(new BigDecimal("176.00"));
        attendance.setProbationAttendance(new BigDecimal("0.00"));
        when(produceAttendanceMapper.selectList(any())).thenReturn(Collections.singletonList(attendance));

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(2001L);
        attendanceUser.setUserId("ding-2001");
        when(attendanceUserRepository.findAllByEmpIdIn(Collections.singletonList(2001L)))
                .thenReturn(Collections.singletonList(attendanceUser));
        when(attendanceApproveRepository.findAllByUserIdInAndWorkDateBetween(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        when(produceAttendanceMapper.queryAdministrativeAttendanceReportMetrics(any(Date.class), any(Date.class), any()))
                .thenReturn(Collections.emptyList());

        MockHttpServletResponse response = new MockHttpServletResponse();
        stubAdministrativePayDay();
        service.downloadAdministrativeAttendance(bo, response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Assert.assertEquals("2026年7月份行政后勤考勤统计表", cellText(sheet.getRow(0), 0));
            Row dataRow = sheet.getRow(4);
            Assert.assertNotNull("should have data row when statistics detail is empty but produce attendance exists", dataRow);
            Assert.assertEquals("1", cellText(dataRow, 0));
            Assert.assertEquals("王五", cellText(dataRow, 1));
            Assert.assertEquals("行政部", cellText(dataRow, 2));
        }
    }

    @Test
    public void downloadAdministrativeAttendance_shouldIncludeAllAdministrativeEmployeesWhenStatisticsDetailPartial() throws Exception {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setCompanyName("测试公司");
        CompanyContext.set(loginUserInfo);

        QueryMonthAttendanceBO bo = new QueryMonthAttendanceBO();
        bo.setYear(2026);
        bo.setMonth(8);

        HrmEmployee statEmployee = new HrmEmployee();
        statEmployee.setEmployeeId(3001L);
        statEmployee.setEmployeeName("聂小玲");
        statEmployee.setDeptId(3001L);
        statEmployee.setAffiliationSystem(1);
        statEmployee.setIsDel(0);

        HrmEmployee otherEmployee = new HrmEmployee();
        otherEmployee.setEmployeeId(3002L);
        otherEmployee.setEmployeeName("王五");
        otherEmployee.setDeptId(3001L);
        otherEmployee.setAffiliationSystem(1);
        otherEmployee.setIsDel(0);

        HrmEmployee productionEmployee = new HrmEmployee();
        productionEmployee.setEmployeeId(3003L);
        productionEmployee.setEmployeeName("李四");
        productionEmployee.setDeptId(3002L);
        productionEmployee.setAffiliationSystem(2);
        productionEmployee.setIsDel(0);
        when(employeeRepository.findAll()).thenReturn(Arrays.asList(statEmployee, otherEmployee, productionEmployee));

        HrmDept dept = new HrmDept();
        dept.setDeptId(3001L);
        dept.setName("行政部");
        when(deptRepository.findAllByDeptIdIn(Collections.singletonList(3001L))).thenReturn(Collections.singletonList(dept));

        // 统计明细只覆盖一名员工（单人统计残留），考勤汇总当月无数据
        HrmOvertimeNightStatisticsDetail partialDetail = statisticsDetail(3001L, "聂小玲", "行政部");
        when(overtimeNightStatisticsDetailRepository.findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(2026, 8))
                .thenReturn(Collections.singletonList(partialDetail));
        when(produceAttendanceMapper.selectList(any())).thenReturn(Collections.emptyList());

        when(attendanceUserRepository.findAllByEmpIdIn(any())).thenReturn(Collections.emptyList());
        when(produceAttendanceMapper.queryAdministrativeAttendanceReportMetrics(any(Date.class), any(Date.class), any()))
                .thenReturn(Collections.emptyList());

        MockHttpServletResponse response = new MockHttpServletResponse();
        stubAdministrativePayDay();
        service.downloadAdministrativeAttendance(bo, response);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row firstRow = sheet.getRow(4);
            Row secondRow = sheet.getRow(5);
            Assert.assertNotNull("有统计明细的员工应导出", secondRow);
            Assert.assertNotNull("无统计明细的行政体系员工也应导出", firstRow);
            Assert.assertEquals("王五", cellText(firstRow, 1));
            Assert.assertEquals("聂小玲", cellText(secondRow, 1));
            Assert.assertNull("生产体系员工不应导出", sheet.getRow(6));
        }
    }

    private static boolean decimalEquals(String expected, BigDecimal actual) {
        return actual != null && new BigDecimal(expected).compareTo(actual) == 0;
    }
}
