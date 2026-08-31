package com.tianye.hrsystem.config;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionParsorTest {

    @Test
    public void buildMysqlJdbcUrl_shouldIncludeAllowPublicKeyRetrievalForMysql8Authentication() {
        String url = ConnectionParsor.buildMysqlJdbcUrl("153.0.237.98", "3306", "hr_0001");

        Assert.assertTrue("动态数据源 JDBC URL 必须包含 allowPublicKeyRetrieval=true",
                url.contains("allowPublicKeyRetrieval=true"));
        Assert.assertTrue("动态数据源 JDBC URL 必须保留 useSSL=false 兼容当前部署",
                url.contains("useSSL=false"));
        Assert.assertEquals(
                "jdbc:mysql://153.0.237.98:3306/hr_0001?useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=false&autoReconnectForPools=true&allowPublicKeyRetrieval=true",
                url
        );
    }
}
