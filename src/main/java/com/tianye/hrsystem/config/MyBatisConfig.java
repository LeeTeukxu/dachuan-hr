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

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class MyBatisConfig {

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
        ConnectionParsor connectionParsor=new ConnectionParsor();
        Map<Object,Object> dataSources=new HashMap<>();
        DynamicDataSource d=new DynamicDataSource();
        DataSource defaultDataSource=connectionParsor.getDefaultConnection();
        dataSources.put("Default", defaultDataSource);
        d.setDefaultTargetDataSource(defaultDataSource);

        //jdbc:mysql://39.100.81.9:3306/hrsystem?useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=false&autoReconnectForPools=true
        try {
            connectionParsor.setConnection(defaultDataSource.getConnection());
            List<String> allKeys = connectionParsor.getAllCompanyCodes();
            for (int i = 0; i < allKeys.size(); i++) {
                String Key = allKeys.get(i);
                ConnectionInfo Info = connectionParsor.getByID(Key);
                DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
                dataSourceBuilder.url("jdbc:mysql://" + Info.getServer() +
                        ":"+Info.getPort()+"/" +
                        Info.getDataBase()+"?useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=false&autoReconnectForPools=true");

                dataSourceBuilder.username(Info.getUsername());
                dataSourceBuilder.password(Info.getPassword());
                dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
                HikariDataSource tinySource = (HikariDataSource) dataSourceBuilder.build();
                tinySource.setMaximumPoolSize(20);
                dataSources.put(Key, tinySource);
            }
        }catch (Exception ax)
        {
            System.out.println(ax.getMessage());
        }
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
