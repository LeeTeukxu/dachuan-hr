package com.tianye.hrsystem.config;


import com.tianye.hrsystem.model.LoginUserInfo;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class MultiCompanyConnectionProviderImpl extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {
    private final Logger logger= LoggerFactory.getLogger(MultiCompanyConnectionProviderImpl.class);
    @Override
    protected DataSource selectAnyDataSource() {
        logger.info("get Connection from HrmSystem.....");
        return CompanyDataSourceProvider.getDataSource("Default");
    }

    @Override
    protected DataSource selectDataSource(String s) {
        LoginUserInfo Info=CompanyContext.get();
        if(Info!=null){
            s=Info.getCompanyId();
        }
        if(s.contains("hrsystem")==false) {
            logger.info("get  Connection from hr_" + s +Info.getSuffix()+".....");
            return CompanyDataSourceProvider.getDataSource(s);
        } else {
            logger.info("get  Connection from hrsystem.....");
            return CompanyDataSourceProvider.getDataSource("Default");
        }
    }
}
