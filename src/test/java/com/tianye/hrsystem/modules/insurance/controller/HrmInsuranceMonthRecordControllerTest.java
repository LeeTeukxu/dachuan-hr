package com.tianye.hrsystem.modules.insurance.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceSalaryBasicAmountBO;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthRecordService;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceComputeProgressVO;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmInsuranceMonthRecordControllerTest {

    @Test
    public void queryComputeInsuranceProgressShouldExposePostMappingAndReturnProgress() throws Exception {
        HrmInsuranceMonthRecordService service = mock(HrmInsuranceMonthRecordService.class);
        InsuranceComputeProgressVO progressVO = new InsuranceComputeProgressVO();
        progressVO.setProgress(68);
        progressVO.setStatus("RUNNING");
        progressVO.setStage("GENERATE_EMP");
        progressVO.setMessage("正在生成员工社保 8/12");
        when(service.queryComputeInsuranceProgress()).thenReturn(progressVO);

        HrmInsuranceMonthRecordController controller = new HrmInsuranceMonthRecordController();
        ReflectionTestUtils.setField(controller, "insuranceMonthRecordService", service);

        PostMapping postMapping = HrmInsuranceMonthRecordController.class
                .getMethod("queryComputeInsuranceProgress")
                .getAnnotation(PostMapping.class);
        Assert.assertArrayEquals(new String[]{"/queryComputeInsuranceProgress"}, postMapping.value());

        Result<InsuranceComputeProgressVO> result = controller.queryComputeInsuranceProgress();

        Assert.assertEquals(200, result.getCode());
        Assert.assertEquals("RUNNING", result.getData().getStatus());
        Assert.assertEquals(Integer.valueOf(68), result.getData().getProgress());
        verify(service).queryComputeInsuranceProgress();
    }

    @Test
    public void updateSalaryBasicInsuranceAmountShouldExposePostMappingAndDelegateService() throws Exception {
        HrmInsuranceMonthRecordService service = mock(HrmInsuranceMonthRecordService.class);
        UpdateInsuranceSalaryBasicAmountBO bo = new UpdateInsuranceSalaryBasicAmountBO();
        bo.setIRecordId(1001L);
        bo.setIncludeSalaryBasicInsuranceAmount(1);
        when(service.updateSalaryBasicInsuranceAmount(bo)).thenReturn(3);

        HrmInsuranceMonthRecordController controller = new HrmInsuranceMonthRecordController();
        ReflectionTestUtils.setField(controller, "insuranceMonthRecordService", service);

        PostMapping postMapping = HrmInsuranceMonthRecordController.class
                .getMethod("updateSalaryBasicInsuranceAmount", UpdateInsuranceSalaryBasicAmountBO.class)
                .getAnnotation(PostMapping.class);
        Assert.assertArrayEquals(new String[]{"/updateSalaryBasicInsuranceAmount"}, postMapping.value());

        Result<Integer> result = controller.updateSalaryBasicInsuranceAmount(bo);

        Assert.assertEquals(200, result.getCode());
        Assert.assertEquals(Integer.valueOf(3), result.getData());
        verify(service).updateSalaryBasicInsuranceAmount(bo);
    }

    @Test
    public void updateSalaryBasicInsuranceAmountRequestShouldAcceptFrontendIRecordFieldNames() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        UpdateInsuranceSalaryBasicAmountBO bo = objectMapper.readValue(
                "{\"iRecordId\":\"1001\",\"iEmpRecordIds\":[\"2001\",\"2002\"],\"includeSalaryBasicInsuranceAmount\":1}",
                UpdateInsuranceSalaryBasicAmountBO.class);

        Assert.assertEquals(Long.valueOf(1001L), bo.getIRecordId());
        Assert.assertEquals(Arrays.asList(2001L, 2002L), bo.getIEmpRecordIds());
        Assert.assertEquals(Integer.valueOf(1), bo.getIncludeSalaryBasicInsuranceAmount());
    }
}
