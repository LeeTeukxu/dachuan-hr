package com.tianye.hrsystem.config;

import com.zaxxer.hikari.HikariDataSource;
import com.tianye.hrsystem.model.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
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
            if (defaultSource == null) {
                logger.error("主数据源未初始化，无法解析租户 {} 的连接信息", companyId);
                return null;
            }
            ConnectionParsor connectionParsor=new ConnectionParsor();
            ConnectionInfo targetInfo=null;
            try {
                connectionParsor.setConnection(defaultSource.getConnection());
                targetInfo=connectionParsor.getByID(companyId);
            } catch (Exception e) {
                logger.error("解析租户连接信息失败: {}", e.getMessage());
            }
            if(targetInfo==null){
                String reason = String.format(
                        "主库 tbcompanylist 中未找到企业 %s 的数据库连接配置（companyId 与 url 中库名解析出的企业编码不匹配）。"
                                + "请核对 tbAllUserList.CompanyID 与 tbcompanylist.url 的库名是否一致。",
                        companyId);
                logger.error(reason);
                throw new IllegalStateException(reason);
            }
            // 1) 优先使用 tbcompanylist.url 中解析出的连接信息
            String url = ConnectionParsor.buildMysqlJdbcUrl(targetInfo.getServer(), targetInfo.getPort(), targetInfo.getDataBase());
            logger.info("为租户 {} 建立数据源(URL方式) -> Server={}:{}, Database={}, User={}",
                    companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getDataBase(), targetInfo.getUsername());
            HikariDataSource dd = buildPool(url, targetInfo.getUsername(), targetInfo.getPassword(), "hikari-" + companyId);
            try (Connection testConn = dd.getConnection()) {
                logger.info("租户 {} 数据源连接成功(URL方式) -> Server={}:{}, Database={}",
                        companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getDataBase());
                dataSourceMap.put(companyId, dd);
                return dd;
            } catch (Exception e) {
                // 2) 回退：租户库与主库在同一 MySQL 实例时，复用主库的主机/端口/账号密码，
                //    仅替换数据库名为 hr_<企业ID>，规避 tbcompanylist.url 中凭据过期/主机填错导致的连不上。
                //    （url 正确时不会走到这里；真正的异地多租户仍由 url 优先保证）
                logger.warn("租户 {} 使用 tbcompanylist.url 建连失败（Server={}:{}, User={}），回退到主库同源连接: {}",
                        companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getUsername(), e.getMessage());
                try {
                    dd.close();
                } catch (Exception ignore) {
                }
                HikariDataSource master = (HikariDataSource) defaultSource;
                String masterUrl = master.getJdbcUrl();
                String hostPort = masterUrl.substring("jdbc:mysql://".length(), masterUrl.indexOf('/', "jdbc:mysql://".length()));
                String[] hp = hostPort.split(":");
                String host = hp[0];
                String port = hp.length > 1 ? hp[1] : "3306";
                String fallbackUrl = ConnectionParsor.buildMysqlJdbcUrl(host, port, targetInfo.getDataBase());
                HikariDataSource fb = buildPool(fallbackUrl, master.getUsername(), master.getPassword(), "hikari-" + companyId + "-fb");
                try (Connection testConn = fb.getConnection()) {
                    logger.info("租户 {} 数据源连接成功(主库同源回退) -> {}", companyId, fallbackUrl);
                    dataSourceMap.put(companyId, fb);
                    return fb;
                } catch (Exception e2) {
                    String reason = String.format(
                            "租户 %s 无法连接其数据库。已尝试：①tbcompanylist.url(Server=%s:%s,User=%s) ②主库同源(Server=%s)。"
                                    + "请检查该企业的数据库是否可访问、tbcompanylist.url 凭据是否正确。",
                            companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getUsername(), hostPort);
                    logger.error(reason, e2);
                    try {
                        fb.close();
                    } catch (Exception ignore) {
                    }
                    throw new IllegalStateException(reason);
                }
            }
        }
    }
    private static HikariDataSource buildPool(String url, String username, String password, String poolName) {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(url);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
        HikariDataSource dd = (HikariDataSource) dataSourceBuilder.build();
        DataSourcePoolConfigurator.apply(dd, poolName);
        return dd;
    }

    public static DataSource getDataSource(String companyId){
        DataSource dx=null;
        if(dataSourceMap.containsKey(companyId)){
            dx= dataSourceMap.get(companyId);
        } else {
            if(!"Default".equals(companyId)) {
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
