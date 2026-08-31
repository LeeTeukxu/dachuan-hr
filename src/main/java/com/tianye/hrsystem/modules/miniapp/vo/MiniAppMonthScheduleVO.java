package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 月度排班返回：days 为 {yyyy-MM-dd: MiniAppDayShiftVO} 的映射
 */
public class MiniAppMonthScheduleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String month;
    private Map<String, MiniAppDayShiftVO> days;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Map<String, MiniAppDayShiftVO> getDays() {
        return days;
    }

    public void setDays(Map<String, MiniAppDayShiftVO> days) {
        this.days = days;
    }

    public static class MonthFull implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<MiniAppDayShiftVO> dayList;

        public List<MiniAppDayShiftVO> getDayList() {
            return dayList;
        }

        public void setDayList(List<MiniAppDayShiftVO> dayList) {
            this.dayList = dayList;
        }
    }
}