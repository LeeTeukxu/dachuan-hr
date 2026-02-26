package com.tianye.hrsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @ClassName: CustomMvcConfig
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 22:30
 **/

@Component
public class CustomMvcConfig implements WebMvcConfigurer {
    @Autowired
    private CompanyInterceptor companyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(companyInterceptor);
    }
}
