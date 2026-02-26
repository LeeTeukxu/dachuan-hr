package com.tianye.hrsystem.config;

import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.ConnectionInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

public class DynamicDataSource extends AbstractRoutingDataSource{
    Map<Object,Object> OX=null;
    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
    }
    @Override
    protected Object determineCurrentLookupKey() {
        LoginUserInfo info= CompanyContext.get();
        String Key="";
        if(info!=null) Key= info.getCompanyId();else Key= "Default";
        if(OX.containsKey(Key)==false){
            ConnectionParsor connectionParsor=new ConnectionParsor();
            DataSource defaultDataSource=(DataSource) OX.get("Default");
            try {
                connectionParsor.setConnection(defaultDataSource.getConnection());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            ConnectionInfo Info = connectionParsor.getByID(Key);
            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
            String url="jdbc:mysql://"+Info.getServer()+ ":"+Info.getPort()+"/"+Info.getDataBase()+"?useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=false&autoReconnectForPools=true";
            dataSourceBuilder.url(url);

            dataSourceBuilder.username(Info.getUsername());
            dataSourceBuilder.password(Info.getPassword());
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");

            HikariDataSource tinySource = (HikariDataSource) dataSourceBuilder.build();
            OX.put(Key,tinySource);
            super.setTargetDataSources(OX);
            super.afterPropertiesSet();
        }
        return Key;
    }
    public  void setDataSource(Map<Object,Object> dataSources){
        OX=dataSources;
        super.setTargetDataSources(dataSources);
    }
}
