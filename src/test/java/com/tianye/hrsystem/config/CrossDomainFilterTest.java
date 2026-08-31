package com.tianye.hrsystem.config;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CrossDomainFilterTest {

    private final CrossDomainFilter crossDomainFilter = new CrossDomainFilter();

    @Test
    public void optionsPreflightShouldReturnCorsHeadersWithoutInvokingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/hrsystem/hrmEmployee/queryPageList");
        request.addHeader("Origin", "http://153.0.237.99:8081");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "token,content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        crossDomainFilter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("http://153.0.237.99:8081", response.getHeader("Access-Control-Allow-Origin"));
        Assert.assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        Assert.assertEquals("GET,POST,PUT,DELETE,PATCH,OPTIONS", response.getHeader("Access-Control-Allow-Methods"));
        Assert.assertEquals("token,content-type", response.getHeader("Access-Control-Allow-Headers"));
        Assert.assertEquals("Origin", response.getHeader("Vary"));
    }

    @Test
    public void nonOptionsRequestShouldContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/hrsystem/hrmEmployee/queryPageList");
        request.addHeader("Origin", "http://153.0.237.99:8081");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        crossDomainFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        Assert.assertEquals("http://153.0.237.99:8081", response.getHeader("Access-Control-Allow-Origin"));
        Assert.assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void corsShouldExposeArchivePasswordForEncryptedDownload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/hrsystem/hrmEmployee/exportDepartmentDetail");
        request.addHeader("Origin", "http://153.0.237.99:8081");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        crossDomainFilter.doFilter(request, response, chain);

        Assert.assertEquals("Content-Disposition, X-Archive-Password",
                response.getHeader("Access-Control-Expose-Headers"));
    }
}
