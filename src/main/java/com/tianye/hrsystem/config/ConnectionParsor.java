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
import java.sql.SQLException;
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
            e.printStackTrace();
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
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = HrsystemApplication.class.getClassLoader().getResourceAsStream("application.properties");
            p.load(in);
            String configName=p.getProperty("spring.profiles.active");
            if(StringUtils.isEmpty(configName)==false){
                in=HrsystemApplication.class.getClassLoader().getResourceAsStream("application-"+configName+".properties");
                p.load(in);
            }
            String url = p.getProperty("spring.datasource.url");
            String username = p.getProperty("spring.datasource.username");
            String password = p.getProperty("spring.datasource.password");
            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
            dataSourceBuilder.url(url);
            dataSourceBuilder.username(username);
            dataSourceBuilder.password(password);
            dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");

            HikariDataSource dd = (HikariDataSource) dataSourceBuilder.build();
            dd.setMaximumPoolSize(20);
            dd.setIdleTimeout(60000);
            dd.setConnectionTimeout(10000);
            dd.setValidationTimeout(3000);
            dd.setConnectionTestQuery("Select 1");
            try {
                dd.setLoginTimeout(5);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dd.setMaxLifetime(60000);
            return dd;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private List<String> getAllKeys(Connection Conn) throws Exception {
        List<String> result = new ArrayList<>();
        Statement stmt = Conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        //查询语句
        String query = "Select url from tbCompanyList";
        Conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        //执行查询
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            String value = rs.getString(1);
            //logger.info("get :"+value);
            result.add(value);
        }
        rs.close();
        stmt.close();
        Conn.close();
        //logger.info("alls:"+Integer.toString(result.size())+"个元素!");
        return result;
    }
}