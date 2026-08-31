package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.service.IHrmOvertimeNightStatisticsService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.InputStream;

@RunWith(MockitoJUnitRunner.class)
public class HrmOvertimeNightStatisticsControllerTest {

    @InjectMocks
    private HrmOvertimeNightStatisticsController controller;

    @Mock
    private IHrmOvertimeNightStatisticsService overtimeNightStatisticsService;

    @Test
    public void downloadStatisticsTemplate_shouldWriteFixedJbtjWorkbook() throws Exception {
        byte[] expectedBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/jbtj.xlsx")) {
            Assert.assertNotNull(inputStream);
            expectedBytes = IOUtils.toByteArray(inputStream);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadStatisticsTemplate(response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains("jbtj.xlsx"));
        Assert.assertArrayEquals(expectedBytes, response.getContentAsByteArray());
    }
}
