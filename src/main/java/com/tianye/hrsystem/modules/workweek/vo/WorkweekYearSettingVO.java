package com.tianye.hrsystem.modules.workweek.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WorkweekYearSettingVO {

    private Integer year;

    private Boolean initialized;

    private Integer totalWeeks;

    private Integer totalWorkDays;

    private Integer totalRestDays;

    private Integer totalLegalHolidayRestDays;

    private Integer totalAdjustedWorkDays;

    private List<WorkweekSettingVO> weekSettings = new ArrayList<>();

    private List<WorkweekMonthSummaryVO> monthSummaries = new ArrayList<>();
}
