package com.tianye.hrsystem.model;

import lombok.Data;

import java.util.Date;

@Data
public class HrmScheduled
{
    private Long id;
    private String corn;//定时器表达式
    private String cornDesc;//定时器表达式内容
    private String cornName;//定时器名称
    private String status;//定时器状态 1=开启  2=关闭
    private Date startTime;//上一次开始时间
    private Date endTime;//上一次结束时间
    private String remark;//备注说明
    private String cornLink;//定时器访问接口链接 eg:http://ali-assist/deal/test?id=123456
    private String method;//定时器访问接口方式 GET POST
    private String code;//定时器编号

    private String startTimeStr;//上一次开始时间
    private String endTimeStr;//上一次结束时间
}
