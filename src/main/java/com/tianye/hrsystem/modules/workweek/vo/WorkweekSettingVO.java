package com.tianye.hrsystem.modules.workweek.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkweekSettingVO {

    private Long settingId;

    private Integer year;

    private Integer weekNo;

    private Integer weekType;

    private String weekTypeName;

    private String restDayText;

    private String weekStartDate;

    private String weekEndDate;

    private Integer manualOverride;
}
