package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.service.employee.IHrmEmployeeContractService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

public class HrmEmployeeContractControllerTest {

    @Test
    public void addContract_shouldIgnoreSubmittedContractId() {
        HrmEmployeeContractController controller = new HrmEmployeeContractController();
        IHrmEmployeeContractService employeeContractService = Mockito.mock(IHrmEmployeeContractService.class);
        ReflectionTestUtils.setField(controller, "employeeContractService", employeeContractService);
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setContractId(90001L);
        contract.setEmployeeId(10001L);

        controller.addContract(contract);

        ArgumentCaptor<HrmEmployeeContract> captor = ArgumentCaptor.forClass(HrmEmployeeContract.class);
        Mockito.verify(employeeContractService).addOrUpdateContract(captor.capture());
        Assert.assertNull("添加合同接口必须按新增处理，不能使用前端残留的旧合同 ID 覆盖原合同", captor.getValue().getContractId());
    }

    @Test
    public void downloadContractTemplate_shouldWriteFixedContractWorkbook() throws Exception {
        byte[] expectedBytes;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/hetong_module.xlsx")) {
            Assert.assertNotNull(inputStream);
            expectedBytes = IOUtils.toByteArray(inputStream);
        }

        HrmEmployeeContractController controller = new HrmEmployeeContractController();
        IHrmEmployeeContractService employeeContractService = Mockito.mock(IHrmEmployeeContractService.class);
        ReflectionTestUtils.setField(controller, "employeeContractService", employeeContractService);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadContractTemplate(response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains("hetong_module.xlsx"));
        Assert.assertArrayEquals(expectedBytes, response.getContentAsByteArray());
    }
}
