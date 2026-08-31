package com.tianye.hrsystem.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CompanyInterceptorTest {

    @InjectMocks
    private CompanyInterceptor companyInterceptor;

    @Mock
    private com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport apiPermissionPathSupport;

    @Test
    public void optionsPreflightShouldBypassTokenValidation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/hrsystem/hrmEmployee/queryPageList");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(companyInterceptor.preHandle(request, response, new Object()));
        Assert.assertEquals(200, response.getStatus());
    }
}
