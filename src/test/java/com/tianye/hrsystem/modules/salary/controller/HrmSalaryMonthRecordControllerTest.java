package com.tianye.hrsystem.modules.salary.controller;

import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryExportDto;
import com.tianye.hrsystem.modules.salary.dto.SalaryMonthRecoveryDto;
import com.tianye.hrsystem.modules.salary.service.SalaryMonthRecordServiceNew;
import com.tianye.hrsystem.modules.salary.vo.SalaryMonthRecoveryPreviewVO;
import com.tianye.hrsystem.modules.salary.vo.SalaryMonthRecoveryResultVO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

public class HrmSalaryMonthRecordControllerTest {

    @Test
    public void exportSalary_shouldPropagateServiceExceptionInsteadOfReturningEmptyResponse() throws Exception {
        SalaryMonthRecordServiceNew service = Mockito.mock(SalaryMonthRecordServiceNew.class);
        HrmSalaryMonthRecordController controller = new HrmSalaryMonthRecordController();
        ReflectionTestUtils.setField(controller, "salaryMonthRecordService", service);

        QuerySalaryExportDto dto = new QuerySalaryExportDto();
        dto.setSalaryRecordId(123L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("导出失败");
        Mockito.doThrow(failure).when(service).exportSalaryNew(dto, response);

        try {
            controller.exportSalary(dto, response);
            Assert.fail("导出异常不能被吞掉，否则前端会下载空白响应");
        } catch (RuntimeException ex) {
            Assert.assertSame(failure, ex);
        }
    }

    @Test
    public void previewSalaryMonthRecovery_shouldDelegateToService() {
        SalaryMonthRecordServiceNew service = Mockito.mock(SalaryMonthRecordServiceNew.class);
        HrmSalaryMonthRecordController controller = new HrmSalaryMonthRecordController();
        ReflectionTestUtils.setField(controller, "salaryMonthRecordService", service);

        SalaryMonthRecoveryDto dto = new SalaryMonthRecoveryDto();
        dto.setYear(2026);
        dto.setMonth(7);
        SalaryMonthRecoveryPreviewVO previewVO = new SalaryMonthRecoveryPreviewVO();
        previewVO.setYear(2026);
        previewVO.setMonth(7);
        Mockito.when(service.previewSalaryMonthRecovery(2026, 7)).thenReturn(previewVO);

        Result<SalaryMonthRecoveryPreviewVO> result = controller.previewSalaryMonthRecovery(dto);

        Assert.assertSame(previewVO, result.getData());
        Mockito.verify(service).previewSalaryMonthRecovery(2026, 7);
    }

    @Test
    public void recoverSalaryMonth_shouldDelegateToService() {
        SalaryMonthRecordServiceNew service = Mockito.mock(SalaryMonthRecordServiceNew.class);
        HrmSalaryMonthRecordController controller = new HrmSalaryMonthRecordController();
        ReflectionTestUtils.setField(controller, "salaryMonthRecordService", service);

        SalaryMonthRecoveryDto dto = new SalaryMonthRecoveryDto();
        dto.setYear(2026);
        dto.setMonth(7);
        SalaryMonthRecoveryResultVO resultVO = new SalaryMonthRecoveryResultVO();
        resultVO.setYear(2026);
        resultVO.setMonth(7);
        Mockito.when(service.recoverSalaryMonth(2026, 7)).thenReturn(resultVO);

        Result<SalaryMonthRecoveryResultVO> result = controller.recoverSalaryMonth(dto);

        Assert.assertSame(resultVO, result.getData());
        Mockito.verify(service).recoverSalaryMonth(2026, 7);
    }
}
