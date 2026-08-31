package com.tianye.hrsystem.modules.insurance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceSalaryBasicAmountBO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthEmpProjectRecordService;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceMonthRecordMapper;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryBasicService;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceComputeProgressVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsuranceRecordListVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmInsuranceMonthRecordServiceTest {

    @Before
    public void clearProgressState() {
        Map progressMap = (Map) ReflectionTestUtils.getField(HrmInsuranceMonthRecordService.class,
                "INSURANCE_COMPUTE_PROGRESS_MAP");
        if (progressMap != null) {
            progressMap.clear();
        }
    }

    @Test
    public void queryComputeInsuranceProgressReturnsIdleWhenNoTaskStarted() {
        HrmInsuranceMonthRecordService service = new HrmInsuranceMonthRecordService();

        InsuranceComputeProgressVO progress = service.queryComputeInsuranceProgress();

        Assert.assertEquals(Integer.valueOf(0), progress.getProgress());
        Assert.assertEquals("IDLE", progress.getStatus());
        Assert.assertEquals("PREPARE", progress.getStage());
        Assert.assertEquals("等待生成社保报表", progress.getMessage());
        Assert.assertEquals(Integer.valueOf(0), progress.getProcessedCount());
        Assert.assertEquals(Integer.valueOf(0), progress.getTotalCount());
        Assert.assertFalse(progress.getDone());
        Assert.assertFalse(progress.getSuccess());
    }

    @Test
    public void queryComputeInsuranceProgressReflectsRunningAndFinishedState() {
        HrmInsuranceMonthRecordService service = new HrmInsuranceMonthRecordService();

        ReflectionTestUtils.invokeMethod(service, "updateComputeProgress",
                45, "RUNNING", "GENERATE_EMP", "正在生成员工社保 2/4", 2, 4);

        InsuranceComputeProgressVO running = service.queryComputeInsuranceProgress();
        Assert.assertEquals(Integer.valueOf(45), running.getProgress());
        Assert.assertEquals("RUNNING", running.getStatus());
        Assert.assertEquals("GENERATE_EMP", running.getStage());
        Assert.assertEquals("正在生成员工社保 2/4", running.getMessage());
        Assert.assertEquals(Integer.valueOf(2), running.getProcessedCount());
        Assert.assertEquals(Integer.valueOf(4), running.getTotalCount());
        Assert.assertFalse(running.getDone());
        Assert.assertFalse(running.getSuccess());

        ReflectionTestUtils.invokeMethod(service, "updateComputeProgress",
                100, "SUCCESS", "FINISH", "社保报表生成完成", 4, 4);

        InsuranceComputeProgressVO finished = service.queryComputeInsuranceProgress();
        Assert.assertEquals(Integer.valueOf(100), finished.getProgress());
        Assert.assertEquals("SUCCESS", finished.getStatus());
        Assert.assertEquals("FINISH", finished.getStage());
        Assert.assertTrue(finished.getDone());
        Assert.assertTrue(finished.getSuccess());
    }

    @Test
    public void queryInsuranceRecordListDefaultsToCurrentYearRangeWhenNoTimeFilter() {
        HrmInsuranceMonthRecordMapper mapper = mock(HrmInsuranceMonthRecordMapper.class);
        when(mapper.queryInsuranceRecordList(any(Page.class), any(QueryInsuranceRecordListBO.class))).thenReturn(new Page<QueryInsuranceRecordListVO>());
        HrmInsuranceMonthRecordService service = new HrmInsuranceMonthRecordService();
        ReflectionTestUtils.setField(service, "insuranceMonthRecordMapper", mapper);

        QueryInsuranceRecordListBO bo = new QueryInsuranceRecordListBO();
        bo.setPageType(0);
        service.queryInsuranceRecordList(bo);

        ArgumentCaptor<QueryInsuranceRecordListBO> captor = ArgumentCaptor.forClass(QueryInsuranceRecordListBO.class);
        verify(mapper).queryInsuranceRecordList(any(Page.class), captor.capture());
        int currentYear = LocalDate.now().getYear();
        Assert.assertEquals(Integer.valueOf(currentYear * 100 + 1), captor.getValue().getStartPeriod());
        Assert.assertEquals(Integer.valueOf(currentYear * 100 + 12), captor.getValue().getEndPeriod());
    }

    @Test
    public void queryInsuranceRecordListKeepsAdvancedFilterRangeAndScopes() {
        HrmInsuranceMonthRecordMapper mapper = mock(HrmInsuranceMonthRecordMapper.class);
        when(mapper.queryInsuranceRecordList(any(Page.class), any(QueryInsuranceRecordListBO.class))).thenReturn(new Page<QueryInsuranceRecordListVO>());
        HrmInsuranceMonthRecordService service = new HrmInsuranceMonthRecordService();
        ReflectionTestUtils.setField(service, "insuranceMonthRecordMapper", mapper);

        QueryInsuranceRecordListBO bo = new QueryInsuranceRecordListBO();
        bo.setTimes(Arrays.asList("2026-03", "2026-05"));
        bo.setDeptIds(Arrays.asList(10L, 20L));
        bo.setEmployeeIds(Arrays.asList(1001L, 1002L));
        service.queryInsuranceRecordList(bo);

        ArgumentCaptor<QueryInsuranceRecordListBO> captor = ArgumentCaptor.forClass(QueryInsuranceRecordListBO.class);
        verify(mapper).queryInsuranceRecordList(any(Page.class), captor.capture());
        QueryInsuranceRecordListBO normalized = captor.getValue();
        Assert.assertEquals(Integer.valueOf(202603), normalized.getStartPeriod());
        Assert.assertEquals(Integer.valueOf(202605), normalized.getEndPeriod());
        Assert.assertEquals(Arrays.asList(10L, 20L), normalized.getDeptIds());
        Assert.assertEquals(Arrays.asList(1001L, 1002L), normalized.getEmployeeIds());
    }

    @Test
    public void queryInsuranceRecordListRaisesGroupConcatLimitBeforeQueryingNames() {
        HrmInsuranceMonthRecordMapper mapper = mock(HrmInsuranceMonthRecordMapper.class);
        when(mapper.queryInsuranceRecordList(any(Page.class), any(QueryInsuranceRecordListBO.class))).thenReturn(new Page<QueryInsuranceRecordListVO>());
        HrmInsuranceMonthRecordService service = new HrmInsuranceMonthRecordService();
        ReflectionTestUtils.setField(service, "insuranceMonthRecordMapper", mapper);

        service.queryInsuranceRecordList(new QueryInsuranceRecordListBO());

        InOrder inOrder = inOrder(mapper);
        inOrder.verify(mapper).setGroupConcatMaxLen();
        inOrder.verify(mapper).queryInsuranceRecordList(any(Page.class), any(QueryInsuranceRecordListBO.class));
    }

    @Test
    public void refreshSalaryBasicInsuranceAmountShouldAddAmountsWheneverSettingIsEnabled() {
        HrmInsuranceMonthEmpProjectRecordService projectRecordService = mock(HrmInsuranceMonthEmpProjectRecordService.class);
        HrmSalaryBasicService salaryBasicService = mock(HrmSalaryBasicService.class);
        HrmInsuranceMonthRecordService service = spy(new HrmInsuranceMonthRecordService());
        ReflectionTestUtils.setField(service, "monthEmpProjectRecordService", projectRecordService);
        ReflectionTestUtils.setField(service, "salaryBasicService", salaryBasicService);

        HrmInsuranceMonthEmpRecord record = new HrmInsuranceMonthEmpRecord();
        record.setIEmpRecordId(9001L);
        record.setSchemeId(3001L);

        Map<String, Object> projectCount = new HashMap<String, Object>();
        projectCount.put("personalInsuranceAmount", new BigDecimal("100.00"));
        projectCount.put("corporateInsuranceAmount", new BigDecimal("200.00"));
        projectCount.put("personalProvidentFundAmount", BigDecimal.ZERO);
        projectCount.put("corporateProvidentFundAmount", BigDecimal.ZERO);
        when(projectRecordService.queryProjectCount(9001L)).thenReturn(projectCount);

        QuerySalaryBasicVO salaryBasic = new QuerySalaryBasicVO();
        salaryBasic.setLongTermCareInsuranceAmount(new BigDecimal("3.00"));
        salaryBasic.setLargeMedicalInsuranceAmount(new BigDecimal("15.00"));
        when(salaryBasicService.findAll()).thenReturn(salaryBasic);
        service.refreshSalaryBasicInsuranceAmount(record, true);

        Assert.assertEquals(Integer.valueOf(1), record.getIncludeSalaryBasicInsuranceAmount());
        Assert.assertEquals(new BigDecimal("103.00"), record.getPersonalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("215.00"), record.getCorporateInsuranceAmount());

        service.refreshSalaryBasicInsuranceAmount(record, true);

        Assert.assertEquals(new BigDecimal("103.00"), record.getPersonalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("215.00"), record.getCorporateInsuranceAmount());

        service.refreshSalaryBasicInsuranceAmount(record, false);

        Assert.assertEquals(Integer.valueOf(0), record.getIncludeSalaryBasicInsuranceAmount());
        Assert.assertEquals(new BigDecimal("100.00"), record.getPersonalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("200.00"), record.getCorporateInsuranceAmount());
    }

    @Test
    public void updateSalaryBasicInsuranceAmountShouldPersistRecomputedEmployeeAmounts() {
        HrmInsuranceMonthEmpRecordService empRecordService = mock(HrmInsuranceMonthEmpRecordService.class);
        HrmInsuranceMonthEmpProjectRecordService projectRecordService = mock(HrmInsuranceMonthEmpProjectRecordService.class);
        HrmSalaryBasicService salaryBasicService = mock(HrmSalaryBasicService.class);
        HrmInsuranceMonthRecordService service = spy(new HrmInsuranceMonthRecordService());
        ReflectionTestUtils.setField(service, "monthEmpRecordService", empRecordService);
        ReflectionTestUtils.setField(service, "monthEmpProjectRecordService", projectRecordService);
        ReflectionTestUtils.setField(service, "salaryBasicService", salaryBasicService);

        HrmInsuranceMonthEmpRecord record = new HrmInsuranceMonthEmpRecord();
        record.setIEmpRecordId(9001L);
        record.setIRecordId(8001L);
        record.setSchemeId(3001L);

        LambdaQueryChainWrapper<HrmInsuranceMonthEmpRecord> query = mock(LambdaQueryChainWrapper.class);
        when(empRecordService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(record));

        Map<String, Object> projectCount = new HashMap<String, Object>();
        projectCount.put("personalInsuranceAmount", new BigDecimal("100.00"));
        projectCount.put("corporateInsuranceAmount", new BigDecimal("200.00"));
        when(projectRecordService.queryProjectCount(9001L)).thenReturn(projectCount);

        QuerySalaryBasicVO salaryBasic = new QuerySalaryBasicVO();
        salaryBasic.setLongTermCareInsuranceAmount(new BigDecimal("3.00"));
        salaryBasic.setLargeMedicalInsuranceAmount(new BigDecimal("15.00"));
        when(salaryBasicService.findAll()).thenReturn(salaryBasic);
        UpdateInsuranceSalaryBasicAmountBO updateBO = new UpdateInsuranceSalaryBasicAmountBO();
        updateBO.setIRecordId(8001L);
        updateBO.setIncludeSalaryBasicInsuranceAmount(1);

        int updated = service.updateSalaryBasicInsuranceAmount(updateBO);

        Assert.assertEquals(1, updated);
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(empRecordService).updateBatchById(captor.capture());
        HrmInsuranceMonthEmpRecord updatedRecord = (HrmInsuranceMonthEmpRecord) captor.getValue().iterator().next();
        Assert.assertEquals(Integer.valueOf(1), updatedRecord.getIncludeSalaryBasicInsuranceAmount());
        Assert.assertEquals(new BigDecimal("103.00"), updatedRecord.getPersonalInsuranceAmount());
        Assert.assertEquals(new BigDecimal("215.00"), updatedRecord.getCorporateInsuranceAmount());
    }
}
