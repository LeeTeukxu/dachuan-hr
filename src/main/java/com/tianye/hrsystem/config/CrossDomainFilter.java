package com.tianye.hrsystem.config;

import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 配置跨域过滤器
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CrossDomainFilter implements Filter {
    private static final String DEFAULT_ALLOWED_METHODS = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
    private static final String DEFAULT_ALLOWED_HEADERS = "Origin,Content-Type,Accept,token,Authorization,X-Requested-With";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        log.debug("======【过滤器】: 进入到跨域过滤器 ======");
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        HttpServletResponse httpResponse = (HttpServletResponse) res;
        String origin = httpRequest.getHeader("Origin");
        if (StringUtils.isNotBlank(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Vary", "Origin");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Allow-Methods", DEFAULT_ALLOWED_METHODS);
            String requestedHeaders = httpRequest.getHeader("Access-Control-Request-Headers");
            httpResponse.setHeader("Access-Control-Allow-Headers",
                    StringUtils.isNotBlank(requestedHeaders) ? requestedHeaders : DEFAULT_ALLOWED_HEADERS);
            httpResponse.setHeader("Access-Control-Expose-Headers", "Content-Disposition, X-Archive-Password");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
        }
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod()) && StringUtils.isNotBlank(origin)) {
            httpResponse.setStatus(200);
            return;
        }
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {

    }
}
