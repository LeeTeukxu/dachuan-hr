package com.tianye.hrsystem.modules.workweek.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WorkweekMonthCalendarVO {

    private Integer year;

    private Integer month;

    private String monthText;

    private Integer totalDays;

    private Integer workDays;

    private Integer restDays;

    private Integer weeklyRestDays;

    private Integer legalHolidayRestDays;

    private Integer adjustedWorkDays;

    private Integer manualDays;

    private List<WorkweekDayCalendarVO> days = new ArrayList<>();
}
