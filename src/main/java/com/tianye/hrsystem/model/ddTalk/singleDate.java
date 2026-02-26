package com.tianye.hrsystem.model.ddTalk;

import java.util.List;

/**
 * @ClassName: singleDate
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月19日 21:36
 **/
public class singleDate {

    String date;
    List<String> time;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getTime() {
        return time;
    }

    public void setTime(List<String> time) {
        this.time = time;
    }
}
