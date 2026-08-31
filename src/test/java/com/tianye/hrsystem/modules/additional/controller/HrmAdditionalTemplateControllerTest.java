package com.tianye.hrsystem.modules.additional.controller;

import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.modules.additional.service.HrmAdditionalService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.net.URLEncoder;

public class HrmAdditionalTemplateControllerTest {

    @Test
    public void downloadAdditionalTemplate_shouldWriteFixedWorkbook() throws Exception {
        byte[] expectedBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/附加扣除累计.xls")) {
            Assert.assertNotNull(inputStream);
            expectedBytes = IOUtils.toByteArray(inputStream);
        }

        HrmAdditionalController controller = new HrmAdditionalController();
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadAdditionalTemplate(response);

        Assert.assertEquals("application/vnd.ms-excel", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition")
                .contains(URLEncoder.encode("附加扣除累计.xls", "UTF-8")));
        Assert.assertArrayEquals(expectedBytes, response.getContentAsByteArray());
    }

    @Test
    public void downloadEmployeeAdditionalTemplate_shouldWriteFixedWorkbook() throws Exception {
        byte[] expectedBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/专项扣除累加表.xlsx")) {
            Assert.assertNotNull(inputStream);
            expectedBytes = IOUtils.toByteArray(inputStream);
        }

        HrmEmployeeAdditionalController controller = new HrmEmployeeAdditionalController();
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadEmployeeAdditionalTemplate(response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition")
                .contains(URLEncoder.encode("专项扣除累加表.xlsx", "UTF-8")));
        Assert.assertArrayEquals(expectedBytes, response.getContentAsByteArray());
    }

    @Test
    public void importAdditional_shouldRejectBlankYearMonthBeforeServiceCall() throws Exception {
        HrmAdditionalController controller = new HrmAdditionalController();
        HrmAdditionalService service = Mockito.mock(HrmAdditionalService.class);
        ReflectionTestUtils.setField(controller, "hrmAdditionalService", service);

        Result result = controller.importAdditional(null, "", " ");

        Assert.assertNotEquals(ResultCode.SUCCESS.code(), result.getCode());
        Assert.assertNotNull(result.getMessage());
        Assert.assertTrue(result.getMessage().contains("请先选择年-月"));
        Mockito.verify(service, Mockito.never())
                .resolveAdditionalData(Mockito.any(), Mockito.anyString(), Mockito.anyString());
    }
}
