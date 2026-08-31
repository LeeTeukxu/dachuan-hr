package com.tianye.hrsystem.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SaaS 改造修复：以 Spring 完整属性源链（含命令行参数、环境变量、profile 文件、${ENV:default} 占位符）
 * 保存主数据源连接信息，供无法注入 Environment 的静态场景（如 ConnectionParsor）使用。
 */
@Component
@Slf4j
public class DefaultDataSourceProperties {

    private static volatile String url;
    private static volatile String username;
    private static volatile String password;

    @Value("${spring.datasource.url:}")
    public void setUrl(String value) {
        url = value;
    }

    @Value("${spring.datasource.username:}")
    public void setUsername(String value) {
        username = value;
    }

    @Value("${spring.datasource.password:}")
    public void setPassword(String value) {
        password = value;
    }

    public static boolean isAvailable() {
        return url != null && !url.isEmpty();
    }

    /**
     * 供早期初始化的 @Bean（如 MyBatisConfig）在 Environment 可用、组件尚未注入时主动填充，
     * 避免 Bean 顺序导致的空值回退到旧文件读取逻辑
     */
    public static void initialize(String urlValue, String usernameValue, String passwordValue) {
        url = urlValue;
        username = usernameValue;
        password = passwordValue;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }
}
