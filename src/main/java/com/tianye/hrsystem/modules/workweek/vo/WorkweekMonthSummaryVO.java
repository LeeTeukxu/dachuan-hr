package com.tianye.hrsystem.modules.workweek.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkweekMonthSummaryVO {

    private String month;

    private String label;

    private Integer totalDays;

    private Integer workDays;

    private Integer restDays;

    private Integer weeklyRestDays;

    private Integer legalHolidayRestDays;

    private Integer adjustedWorkDays;

    private Integer totalWeeks;

    private Integer singleRestWeeks;

    private Integer doubleRestWeeks;

    private Integer manualOverrideWeeks;

    private Integer firstWeekNo;

    private Integer lastWeekNo;

    private String weekRangeText;
}
