package com.tianye.hrsystem.modules.salary.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryBasicDto;
import com.tianye.hrsystem.modules.salary.dto.UpdateEmployeeFullAttendanceAmountDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HrmSalaryBasicServiceTest {

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void findAll_shouldReturnDefaultFullAttendanceAndProductionRestSettingsWhenNoRecordExists() {
        TestableHrmSalaryBasicService service = new TestableHrmSalaryBasicService();

        QuerySalaryBasicVO result = service.findAll();

        Assert.assertEquals(new BigDecimal("100"), result.getOrdinaryFullAttendanceAmount());
        Assert.assertEquals(new BigDecimal("500"), result.getLeaderFullAttendanceAmount());
        Assert.assertEquals(Integer.valueOf(4), result.getProductionMonthlyRestDays());
        Assert.assertEquals(new BigDecimal("15"), result.getLargeMedicalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("3"), result.getLongTermCareInsuranceAmount());
    }

    @Test
    public void findAll_shouldFillDefaultSettingsForLegacyRecord() {
        TestableHrmSalaryBasicService service = new TestableHrmSalaryBasicService();
        service.records.add(new HrmSalaryBasic()
                .setSalaryBasic(new BigDecimal("2000"))
                .setOvertimePay(new BigDecimal("12"))
                .setSubsidy(new BigDecimal("30")));

        QuerySalaryBasicVO result = service.findAll();

        Assert.assertEquals(new BigDecimal("2000"), result.getSalaryBasic());
        Assert.assertEquals(new BigDecimal("100"), result.getOrdinaryFullAttendanceAmount());
        Assert.assertEquals(new BigDecimal("500"), result.getLeaderFullAttendanceAmount());
        Assert.assertEquals(Integer.valueOf(4), result.getProductionMonthlyRestDays());
        Assert.assertEquals(new BigDecimal("15"), result.getLargeMedicalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("3"), result.getLongTermCareInsuranceAmount());
    }

    @Test
    public void findAll_shouldReturnLatestSalaryBasicRecordByCreateTimeAndId() {
        TestableHrmSalaryBasicService service = new TestableHrmSalaryBasicService();
        service.records.add(new HrmSalaryBasic()
                .setId(20L)
                .setCreateTime(java.time.LocalDateTime.of(2026, 7, 1, 10, 0))
                .setOrdinaryFullAttendanceAmount(new BigDecimal("130"))
                .setLeaderFullAttendanceAmount(new BigDecimal("650"))
                .setProductionMonthlyRestDays(5)
                .setLargeMedicalInsuranceAmount(new BigDecimal("18"))
                .setLongTermCareInsuranceAmount(new BigDecimal("5")));
        service.records.add(new HrmSalaryBasic()
                .setId(10L)
                .setCreateTime(java.time.LocalDateTime.of(2026, 6, 1, 10, 0))
                .setOrdinaryFullAttendanceAmount(new BigDecimal("90"))
                .setLeaderFullAttendanceAmount(new BigDecimal("450"))
                .setProductionMonthlyRestDays(3)
                .setLargeMedicalInsuranceAmount(new BigDecimal("12"))
                .setLongTermCareInsuranceAmount(new BigDecimal("2")));

        QuerySalaryBasicVO result = service.findAll();

        Assert.assertEquals(Long.valueOf(20L), result.getId());
        Assert.assertEquals(new BigDecimal("130"), result.getOrdinaryFullAttendanceAmount());
        Assert.assertEquals(new BigDecimal("650"), result.getLeaderFullAttendanceAmount());
        Assert.assertEquals(Integer.valueOf(5), result.getProductionMonthlyRestDays());
        Assert.assertEquals(new BigDecimal("18"), result.getLargeMedicalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("5"), result.getLongTermCareInsuranceAmount());
    }

    @Test
    public void saveSalaryBasic_shouldPersistConfiguredSettings() {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId("1001");
        CompanyContext.set(loginUserInfo);

        TestableHrmSalaryBasicService service = new TestableHrmSalaryBasicService();
        QuerySalaryBasicDto dto = new QuerySalaryBasicDto();
        dto.setSalaryBasic(new BigDecimal("2000"));
        dto.setOvertimePay(new BigDecimal("12"));
        dto.setSubsidy(new BigDecimal("30"));
        dto.setOrdinaryFullAttendanceAmount(new BigDecimal("120"));
        dto.setLeaderFullAttendanceAmount(new BigDecimal("600"));
        dto.setProductionMonthlyRestDays(6);
        dto.setLargeMedicalInsuranceAmount(new BigDecimal("20"));
        dto.setLongTermCareInsuranceAmount(new BigDecimal("4"));

        service.saveSalaryBasic(dto);

        Assert.assertNotNull(service.saved);
        Assert.assertEquals(new BigDecimal("120"), service.saved.getOrdinaryFullAttendanceAmount());
        Assert.assertEquals(new BigDecimal("600"), service.saved.getLeaderFullAttendanceAmount());
        Assert.assertEquals(Integer.valueOf(6), service.saved.getProductionMonthlyRestDays());
        Assert.assertEquals(new BigDecimal("20"), service.saved.getLargeMedicalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("4"), service.saved.getLongTermCareInsuranceAmount());
    }

    @Test
    public void saveSalaryBasic_shouldSyncBasicSalaryAmountToEmployeeArchives() {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId("1001");
        CompanyContext.set(loginUserInfo);

        TestableHrmSalaryBasicService service = new TestableHrmSalaryBasicService();
        QuerySalaryBasicDto dto = new QuerySalaryBasicDto();
        dto.setSalaryBasic(new BigDecimal("2300.00"));

        service.saveSalaryBasic(dto);

        Assert.assertEquals(new BigDecimal("2300.00"), service.syncedBasicSalaryAmount);
        Assert.assertEquals("1001", service.syncedByUserId);
    }

    @Test
    public void saveSalaryBasic_shouldNotSyncBasicSalaryArchivesWhenAmountMissing() {
        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId("1001");
        CompanyContext.set(loginUserInfo);

        TestableHrmSalaryBasicService service = new TestableHrmSalaryBasicService();
        service.saveSalaryBasic(new QuerySalaryBasicDto());

        Assert.assertEquals(0, service.syncBasicSalaryCallCount);
    }

    @Test
    public void salaryBasicServiceSource_shouldSyncBasicSalaryArchivesForNotDeletedEmployees() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/HrmSalaryBasicService.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("保存基本工资设置后必须同步员工薪资档案基本工资",
                source.contains("syncBasicSalaryToEmployeeArchives"));
        Assert.assertTrue("同步薪资档案必须写入薪资档案明细表",
                source.contains("HrmSalaryArchivesOptionService"));
        Assert.assertTrue("同步范围必须由数据库按未删除员工过滤，避免全量员工 ID 拼 IN",
                source.contains("EXISTS_NOT_DELETED_EMPLOYEE_SQL"));
        Assert.assertTrue("同步范围必须关联员工表未删除条件",
                source.contains("e.is_del = 0"));
        Assert.assertTrue("同步 SQL 必须用薪资档案明细员工 ID 关联员工表",
                source.contains("hrm_salary_archives_option.employee_id"));
        Assert.assertTrue("同步工资项必须限定为 10101 / 基本工资",
                source.contains("BASIC_SALARY_ARCHIVES_OPTION_CODE"));
        Assert.assertTrue("同步应覆盖薪资档案的试用期和正式两套基本工资项",
                source.contains("BASIC_SALARY_ARCHIVES_IS_PRO_TYPES"));
        Assert.assertFalse("同步基本工资不得先全量查询未删除员工 ID 再拼 IN 更新",
                source.contains("queryNotDeletedEmployeeIdsForBasicSalarySync"));
        Assert.assertFalse("同步基本工资不得使用 selectList 全量加载员工 ID",
                source.contains("selectList(new LambdaQueryWrapper"));
    }

    @Test
    public void salaryBasicServiceSource_shouldBatchUpdateEmployeeFullAttendanceAmount() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/service/HrmSalaryBasicService.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("基本工资服务必须提供员工级全勤金额批量设置方法",
                source.contains("updateEmployeeFullAttendanceAmount"));
        Assert.assertTrue("员工级全勤金额必须写入员工表",
                source.contains("HrmEmployeeMapper"));
        Assert.assertTrue("普通员工全勤金额设置必须写入 ordinaryFullAttendanceAmount",
                source.contains("setOrdinaryFullAttendanceAmount"));
        Assert.assertTrue("领导全勤金额设置必须写入 leaderFullAttendanceAmount",
                source.contains("setLeaderFullAttendanceAmount"));
        Assert.assertTrue("空范围必须按全员更新，更新范围需要过滤未删除员工",
                source.contains("HrmEmployee::getIsDel"));
        Assert.assertTrue("非空范围必须按 employeeIds 限定员工",
                source.contains("HrmEmployee::getEmployeeId"));
    }

    @Test(expected = HrmException.class)
    public void updateEmployeeFullAttendanceAmount_shouldRejectExplicitSelectionWhenNoEmployeeUpdated() {
        HrmSalaryBasicService service = new HrmSalaryBasicService();
        HrmEmployeeMapper employeeMapper = mock(HrmEmployeeMapper.class);
        when(employeeMapper.update(any(), any())).thenReturn(0);
        ReflectionTestUtils.setField(service, "hrmEmployeeMapper", employeeMapper);

        UpdateEmployeeFullAttendanceAmountDto dto = new UpdateEmployeeFullAttendanceAmountDto();
        dto.setAmountType("ordinary");
        dto.setAmount(new BigDecimal("120"));
        dto.setEmployeeIds(Collections.singletonList(999L));

        service.updateEmployeeFullAttendanceAmount(dto);
    }

    @Test
    public void salaryBasicControllerSource_shouldExposeEmployeeFullAttendanceAmountEndpoint() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryBasicController.java")),
                StandardCharsets.UTF_8);

        Assert.assertTrue("基本工资控制器必须提供员工级全勤金额设置接口",
                source.contains("/updateEmployeeFullAttendanceAmount"));
        Assert.assertTrue("接口必须调用服务层批量设置方法",
                source.contains("updateEmployeeFullAttendanceAmount"));
    }

    private static class TestableHrmSalaryBasicService extends HrmSalaryBasicService {
        private final List<HrmSalaryBasic> records = new ArrayList<>();
        private HrmSalaryBasic saved;
        private BigDecimal syncedBasicSalaryAmount;
        private String syncedByUserId;
        private int syncBasicSalaryCallCount;

        @Override
        public List<HrmSalaryBasic> list() {
            return records;
        }

        @Override
        public boolean save(HrmSalaryBasic entity) {
            this.saved = entity;
            return true;
        }

        @Override
        public boolean update(HrmSalaryBasic entity, Wrapper<HrmSalaryBasic> updateWrapper) {
            this.saved = entity;
            return true;
        }

        protected int syncBasicSalaryToEmployeeArchives(BigDecimal salaryBasicAmount, LoginUserInfo info,
                                                        java.time.LocalDateTime now) {
            syncBasicSalaryCallCount++;
            this.syncedBasicSalaryAmount = salaryBasicAmount;
            this.syncedByUserId = info == null ? null : info.getUserId();
            return 0;
        }
    }
}
