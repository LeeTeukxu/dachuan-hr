package com.tianye.hrsystem.config;

import com.tianye.hrsystem.HrsystemApplication;
import com.tianye.hrsystem.model.ConnectionInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * @ClassName: connectionParsor
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月05日 22:23
 **/
@Configuration
public class ConnectionParsor {
    private static final String MYSQL_JDBC_QUERY =
            "useUnicode=true&characterEncoding=gbk&autoReconnect=true&serverTimezone=Asia/Shanghai" +
                    "&useSSL=false&autoReconnectForPools=true&allowPublicKeyRetrieval=true";
    private static Logger logger = LoggerFactory.getLogger(ConnectionParsor.class);
    private Map<String, ConnectionInfo> cacheInfo;
    List<String> allKeys = new ArrayList<>();
    Connection Con = null;

    public ConnectionParsor() {
        cacheInfo = new HashMap<>();
    }

    public void setConnection(Connection Con) {
        this.Con = Con;
        Init();
    }

    private void Init() {
        List<String> alls = null;
        try {
            alls = getAllKeys(Con);
        } catch (Exception e) {
            logger.info("Init is a error occur！");
            logger.error("ConnectionParsor.java 异常", e);
        }
        if (alls == null) {
            logger.info("alls is null!");
        }
        for (int i = 0; i < alls.size(); i++) {
            String X = alls.get(i);
            X = X.replace(",", ":");
            ConnectionInfo Y = parseSingle(X);
            if (Y != null) {
                String CompanyCode=Y.getDataBase().trim();
                String[] Cs=CompanyCode.split("_");
                CompanyCode =Cs[Cs.length-1];
                if(CompanyCode.startsWith("000")==false){
                    CompanyCode=Cs[Cs.length-2];
                }
                if (cacheInfo.containsKey(CompanyCode) == false) {
                    cacheInfo.put(CompanyCode, Y);
                    allKeys.add(CompanyCode);
                }
            }
        }
    }

    public ConnectionInfo getByID(String ID) {
        if (cacheInfo == null) cacheInfo = new HashMap<>();
        if (cacheInfo.size() == 0) Init();
        if (cacheInfo.containsKey(ID)) return cacheInfo.get(ID);
        else {
            logger.info("return null as ConnectionInfo");
            return null;
        }
    }

    public static String buildMysqlJdbcUrl(String server, String port, String database) {
        return "jdbc:mysql://" + server + ":" + port + "/" + database + "?" + MYSQL_JDBC_QUERY;
    }

    public List<String> getAllCompanyCodes() {
        return allKeys;
    }

    private ConnectionInfo parseSingle(String conn) {
        String[] SS = conn.split(";");
        if (SS.length > 0) {
            ConnectionInfo Info = new ConnectionInfo();
            for (int i = 0; i < SS.length; i++) {
                String S = SS[i];
                String[] SX = S.split("=");
                if (S.startsWith("Server")) {
                    Info.setServer(SX[1]);
                    if (Info.getServer().indexOf(":") > -1) {
                        String[] X1 = Info.getServer().split(":");
                        Info.setPort(X1[1]);
                        Info.setServer(X1[0]);
                    }
                } else if (S.startsWith("Database")) {
                    Info.setDataBase(SX[1]);
                } else if (S.startsWith("User")) {
                    Info.setUsername(SX[1]);
                } else if (S.startsWith("Password")) {
                    Info.setPassword(SX[1]);
                }
            }
            return Info;
        } else return null;
    }

    public DataSource getDefaultConnection() {
        // SaaS 改造修复：优先使用 Spring 属性源注入的连接信息（正确响应 --spring.profiles.active
        // 与 ${ENV:default} 占位符），仅在 Spring 未初始化时回退到手动读文件的老逻辑
        if (DefaultDataSourceProperties.isAvailable()) {
            logger.info("Using DefaultDataSourceProperties for default pool");
            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
            dataSourceBuilder.url(DefaultDataSourceProperties.getUrl());
            dataSourceBuilder.username(DefaultDataSourceProperties.getUsername());
            dataSourceBuilder.password(DefaultDataSourceProperties.getPassword());
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
            HikariDataSource dd = (HikariDataSource) dataSourceBuilder.build();
            DataSourcePoolConfigurator.apply(dd, "hikari-default");
            return dd;
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = HrsystemApplication.class.getClassLoader().getResourceAsStream("application.properties");
            p.load(in);
            // SaaS 改造修复：优先取命令行/环境变量中的 active profile，文件值仅作兜底
            // （否则 --spring.profiles.active=local 启动时，此处仍会读到文件里的 dev）
            // 注意：Spring Boot 将 --spring.profiles.active=local 写入 spring.profiles.active
            // 系统属性；同时也会设置 ACTIVE_PROFILES 环境变量，需一并检查
            String configName = System.getProperty("spring.profiles.active");
            if (StringUtils.isEmpty(configName)) {
                configName = System.getenv("SPRING_PROFILES_ACTIVE");
            }
            if (StringUtils.isEmpty(configName)) {
                configName = System.getenv("ACTIVE_PROFILES");
            }
            if (StringUtils.isEmpty(configName)) {
                configName = p.getProperty("spring.profiles.active");
            }
            if(StringUtils.isEmpty(configName)==false){
                in=HrsystemApplication.class.getClassLoader().getResourceAsStream("application-"+configName+".properties");
                p.load(in);
            }
            // SaaS 安全改造 P0-3：手动加载的配置需自行解析 ${ENV:default} 占位符
            String url = resolvePlaceholders(p.getProperty("spring.datasource.url"));
            String username = resolvePlaceholders(p.getProperty("spring.datasource.username"));
            String password = resolvePlaceholders(p.getProperty("spring.datasource.password"));
            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
            dataSourceBuilder.url(url);
            dataSourceBuilder.username(username);
            dataSourceBuilder.password(password);
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");

            HikariDataSource dd = (HikariDataSource) dataSourceBuilder.build();
            DataSourcePoolConfigurator.apply(dd, "hikari-default");
            return dd;
        } catch (Exception e) {
            logger.error("ConnectionParsor.java 异常", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                logger.error("ConnectionParsor.java 异常", e);
            }
        }
        return null;
    }

    /**
     * SaaS 安全改造 P0-3：解析 ${ENV_VAR:default} 形式占位符
     * 优先取环境变量，其次 JVM 系统属性，最后用默认值
     */
    public static String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?\\}");
        java.util.regex.Matcher matcher = pattern.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2) == null ? "" : matcher.group(2);
            String resolved = System.getenv(key);
            if (StringUtils.isEmpty(resolved)) {
                resolved = System.getProperty(key);
            }
            if (StringUtils.isEmpty(resolved)) {
                resolved = defaultValue;
            }
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<String> getAllKeys(Connection conn) throws Exception {
        List<String> result = new ArrayList<>();
        String query = "Select url from tbCompanyList";
        // 连接由调用方借出，失败时也必须在 finally 中归还（关闭）到连接池；
        // Statement/ResultSet 用 try-with-resources 确保一定关闭，避免连接/语句泄漏把默认池(仅 8 连)打满。
        try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } finally {
            try {
                conn.close();
            } catch (Exception ignore) {
                // 归还失败不影响解析结果
            }
        }
        return result;
    }
}
