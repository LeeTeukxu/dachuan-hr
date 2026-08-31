package com.tianye.hrsystem.modules.deduction.controller;

import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.modules.deduction.service.HrmPersonalIncomeTaxService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.net.URLEncoder;

public class HrmPersonalIncomeTaxControllerTest {

    @Test
    public void downloadPersonalIncomeTaxTemplate_shouldWriteFixedWorkbook() throws Exception {
        byte[] expectedBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/个税累计.xls")) {
            Assert.assertNotNull(inputStream);
            expectedBytes = IOUtils.toByteArray(inputStream);
        }

        HrmPersonalIncomeTaxController controller = new HrmPersonalIncomeTaxController();
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadPersonalIncomeTaxTemplate(response);

        Assert.assertEquals("application/vnd.ms-excel", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition")
                .contains(URLEncoder.encode("个税累计.xls", "UTF-8")));
        Assert.assertArrayEquals(expectedBytes, response.getContentAsByteArray());
    }

    @Test
    public void importPersonalIncomeTax_shouldRejectBlankMonthBeforeServiceCall() throws Exception {
        HrmPersonalIncomeTaxController controller = new HrmPersonalIncomeTaxController();
        HrmPersonalIncomeTaxService service = Mockito.mock(HrmPersonalIncomeTaxService.class);
        ReflectionTestUtils.setField(controller, "hrmPersonalIncomeTaxService", service);

        Result result = controller.importPersonalIncomeTax(null, " ");

        Assert.assertNotEquals(ResultCode.SUCCESS.code(), result.getCode());
        Assert.assertNotNull(result.getMessage());
        Assert.assertTrue(result.getMessage().contains("请先选择月份"));
        Mockito.verify(service, Mockito.never())
                .resolvePersonalIncomeTaxData(Mockito.any(), Mockito.anyString());
    }
}
