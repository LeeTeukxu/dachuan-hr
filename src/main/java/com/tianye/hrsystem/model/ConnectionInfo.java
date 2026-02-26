package com.tianye.hrsystem.model;

/**
 * @ClassName: connectionInfo
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月05日 21:25
 **/
public class ConnectionInfo {
    private String server;
    private String dataBase;
    private String username;
    private String password;
    private String port;

    public ConnectionInfo() {
        port = "3066";
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getDataBase() {
        return dataBase;
    }

    public void setDataBase(String dataBase) {
        this.dataBase = dataBase;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}