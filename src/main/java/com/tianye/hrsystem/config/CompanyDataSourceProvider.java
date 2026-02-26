package com.tianye.hrsystem.config;

import com.zaxxer.hikari.HikariDataSource;
import com.tianye.hrsystem.model.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CompanyDataSourceProvider {
    private static Map<String,DataSource> dataSourceMap=new HashMap<>();
    private static Logger logger= LoggerFactory.getLogger(CompanyDataSourceProvider.class);
    private static DataSource getByCompanyID(String companyId){
        logger.info("get DataSource by Id:"+companyId);
        if(dataSourceMap.containsKey(companyId)){
            return dataSourceMap.get(companyId);
        } else {
            DataSource defaultSource=dataSourceMap.get("Default");
            ConnectionParsor connectionParsor=new ConnectionParsor();
            ConnectionInfo targetInfo=null;
            try {
                connectionParsor.setConnection(defaultSource.getConnection());
                targetInfo=connectionParsor.getByID(companyId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if(targetInfo==null){
                logger.info("targetInfo is null");
                return null;
            }
            //jdbc:mysql://39.100.81.9:3306/hrsystem?useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=false&autoReconnectForPools=true
            String url="jdbc:mysql://"+targetInfo.getServer()+ ":"+targetInfo.getPort()+"/"+targetInfo.getDataBase()+"?useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=false&autoReconnectForPools=true";
            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
            dataSourceBuilder.url(url);
            dataSourceBuilder.username(targetInfo.getUsername());
            dataSourceBuilder.password(targetInfo.getPassword());
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
            HikariDataSource dd=(HikariDataSource) dataSourceBuilder.build();
            dd.setMaximumPoolSize(20);
            dd.setIdleTimeout(40000);
            dd.setConnectionTimeout(60000);
            dd.setMaximumPoolSize(100);
            dd.setConnectionTestQuery("Select 1");
            try {
                dd.setLoginTimeout(5);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dd.setMaxLifetime(60000);
            dataSourceMap.put(companyId,dd);
            return dd;
        }
    }
    public static DataSource getDataSource(String companyId){
        DataSource dx=null;
        if(dataSourceMap.containsKey(companyId)){
            dx= dataSourceMap.get(companyId);
        } else {
            if(companyId!="Default") {
                dx = getByCompanyID(companyId);
            } else {
                ConnectionParsor connectionParsor=new ConnectionParsor();
                dx=connectionParsor.getDefaultConnection();
                dataSourceMap.put("Default",dx);
            }
        }
        return dx;
    }
}
