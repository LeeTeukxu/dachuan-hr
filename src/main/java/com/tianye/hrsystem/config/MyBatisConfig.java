package com.tianye.hrsystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.tianye.hrsystem.model.ConnectionInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mapstruct.Qualifier;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class MyBatisConfig {

    @Autowired
    private Environment env;

//    @Bean
//    public SqlSessionFactoryBean getSqlSessionFactoryBean() throws Exception {
//        SqlSessionFactoryBean sqlSessionFactoryBean=new SqlSessionFactoryBean();
//        sqlSessionFactoryBean.setDataSource(dynamicDataSource());
//        sqlSessionFactoryBean.setTypeAliasesPackage("com.tianye.hrsystem.model");
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath*:/mapper/*.xml"));
//        return sqlSessionFactoryBean;
//    }

    @Bean(name = "defSqlSessionFactory")
    @Primary
    public SqlSessionFactory defSqlSessionFactory() throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        //SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dynamicDataSource());
        //设置mybatis的xml所在位置
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        bean.setMapperLocations(resolver.getResources("classpath*:/mapper/*.xml"));
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        bean.setPlugins(mybatisPlusInterceptor);
        SqlSessionFactory factory = bean.getObject();
        return factory;
    }


    @Bean
    public  DynamicDataSource dynamicDataSource(){
        // SaaS 改造：确保 DefaultDataSourceProperties 在早期初始化时就有值，
        // 避免 Bean 顺序导致的空值回退到旧文件读取逻辑
        if (!DefaultDataSourceProperties.isAvailable()) {
            DefaultDataSourceProperties.initialize(
                    env.getProperty("spring.datasource.url", ""),
                    env.getProperty("spring.datasource.username", ""),
                    env.getProperty("spring.datasource.password", ""));
        }
        ConnectionParsor connectionParsor=new ConnectionParsor();
        Map<Object,Object> dataSources=new HashMap<>();
        DynamicDataSource d=new DynamicDataSource();
        DataSource defaultDataSource=connectionParsor.getDefaultConnection();
        dataSources.put("Default", defaultDataSource);
        d.setDefaultTargetDataSource(defaultDataSource);

        // 注意：租户数据源不再在此处按 tbcompanylist.url 预建（旧逻辑会把“过期凭据/错误主机”
        // 直接写死进 DynamicDataSource.OX，导致请求时直接命中陈旧池、绕过了
        // CompanyDataSourceProvider 的“主库同源回退”逻辑）。租户数据源统一交由
        // CompanyDataSourceProvider 懒加载 + 缓存 + 回退，保证连接信息始终以运行期校验为准。
        d.setDataSource(dataSources);
        return d;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 向Mybatis过滤器链中添加分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
