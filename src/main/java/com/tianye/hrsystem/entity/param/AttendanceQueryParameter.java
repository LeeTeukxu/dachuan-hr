package com.tianye.hrsystem.entity.param;

import java.util.List;

/**
 * @ClassName: AttendanceQueryParameter
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月20日 10:07
 **/
public class AttendanceQueryParameter extends  RequestParameterBase{
    List<String> times;

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }
}
