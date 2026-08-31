package com.tianye.hrsystem.modules.workweek.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkweekDayCalendarVO {

    private String workDate;

    private Integer year;

    private Integer month;

    private Integer dayOfMonth;

    private Integer dayOfWeek;

    private String dayOfWeekName;

    private Integer dayType;

    private String dayTypeName;

    private String sourceType;

    private String sourceName;

    private String holidayName;
}

