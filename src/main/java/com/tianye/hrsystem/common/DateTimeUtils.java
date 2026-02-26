package com.tianye.hrsystem.common;

import java.util.Date;

/**
 * @ClassName: DateTimeUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月21日 15:05
 **/
public class DateTimeUtils {
    public static Date CloneToFirst(Date D){
        Date T=D;
        T.setHours(0);
        T.setMinutes(0);
        T.setSeconds(0);
        return T;
    }
    public static Date CloneToEnd(Date D){
        Date T=D;
        T.setHours(23);
        T.setMinutes(59);
        T.setSeconds(59);
        return T;
    }
}
