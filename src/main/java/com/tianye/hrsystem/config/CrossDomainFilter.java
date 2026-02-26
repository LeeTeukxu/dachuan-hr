package com.tianye.hrsystem.config;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 配置跨域过滤器
 */
@Slf4j
public class CrossDomainFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        log.debug("======【过滤器】: 进入到跨域过滤器 ======");
        HttpServletResponse httpResponse = (HttpServletResponse) res;
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");  // 允许跨域的地址为所有
        //httpResponse.addHeader("Access-Control-Allow-Headers", "Content-Type,X-Requested-With,accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,token");
        httpResponse.addHeader("Access-Control-Allow-Headers", "*");
        httpResponse.addHeader("Access-Control-Max-Age", "3600");  // 非简单请求，只要第一次通过OPTIONS检查 在1小时之内不会在调用OPTIONS进行检测
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");  // 带有Cookie的跨域请求，此值必须设置为true。
        //httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type,token");
        if (((HttpServletRequest) req).getMethod().equals("OPTIONS")) {
            httpResponse.setStatus(200);
            return;
        }
        chain.doFilter(req, httpResponse);
    }

    @Override
    public void destroy() {

    }
}
