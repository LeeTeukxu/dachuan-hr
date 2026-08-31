package com.tianye.hrsystem.imple.workplan;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import org.apache.commons.lang.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class WorkPlanCustomShiftResolver {

    private static final DateTimeFormatter HH_MM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public NormalizedCustomShift normalize(String startText, String endText) throws Exception {
        LocalTime start = parseTime(startText, "开始时间");
        if (StringUtils.isBlank(endText)) {
            return new NormalizedCustomShift(formatTime(start), null, false);
        }
        LocalTime end = parseTime(endText, "结束时间");
        boolean crossDay = !end.isAfter(start);
        return new NormalizedCustomShift(formatTime(start), formatTime(end), crossDay);
    }

    public Long findExactShiftId(OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group,
                                 NormalizedCustomShift shift) {
        if (group == null || shift == null || group.getSelectedClass() == null) {
            return null;
        }
        for (OapiAttendanceGetsimplegroupsResponse.AtClassVo classVo : group.getSelectedClass()) {
            if (classVo == null || classVo.getClassId() == null || classVo.getSections() == null
                    || classVo.getSections().size() != 1) {
                continue;
            }
            OapiAttendanceGetsimplegroupsResponse.AtSectionVo section = classVo.getSections().get(0);
            if (section == null) {
                continue;
            }
            ShiftSectionTime sectionTime = extractSingleSection(section.getTimes());
            if (sectionTime == null) {
                continue;
            }
            if (shift.getStartText().equals(sectionTime.startText)
                    && shift.getEndText().equals(sectionTime.endText)
                    && shift.getCrossDay().equals(sectionTime.crossDay)) {
                return classVo.getClassId();
            }
        }
        return null;
    }

    private ShiftSectionTime extractSingleSection(List<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> times) {
        if (times == null || times.size() < 2) {
            return null;
        }
        String startText = null;
        String endText = null;
        boolean crossDay = false;
        for (OapiAttendanceGetsimplegroupsResponse.SetionTimeVO time : times) {
            if (time == null || time.getCheckTime() == null) {
                continue;
            }
            if ("OnDuty".equalsIgnoreCase(time.getCheckType())) {
                startText = HH_MM_FORMATTER.format(time.getCheckTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalTime());
            } else if ("OffDuty".equalsIgnoreCase(time.getCheckType())) {
                endText = HH_MM_FORMATTER.format(time.getCheckTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalTime());
                crossDay = time.getAcross() != null && time.getAcross() > 0;
            }
        }
        if (StringUtils.isBlank(startText) || StringUtils.isBlank(endText)) {
            return null;
        }
        return new ShiftSectionTime(startText, endText, crossDay);
    }

    private LocalTime parseTime(String text, String fieldName) throws Exception {
        if (StringUtils.isBlank(text)) {
            throw new Exception(fieldName + "不能为空，格式必须为HH:mm");
        }
        try {
            return LocalTime.parse(text.trim(), HH_MM_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new Exception(fieldName + "格式错误，格式必须为HH:mm");
        }
    }

    private String formatTime(LocalTime time) {
        return HH_MM_FORMATTER.format(time);
    }

    public static class NormalizedCustomShift {
        private final String startText;
        private final String endText;
        private final Boolean crossDay;

        public NormalizedCustomShift(String startText, String endText, Boolean crossDay) {
            this.startText = startText;
            this.endText = endText;
            this.crossDay = crossDay;
        }

        public String getStartText() {
            return startText;
        }

        public String getEndText() {
            return endText;
        }

        public Boolean getCrossDay() {
            return crossDay;
        }

        public String buildDisplayText() {
            if (StringUtils.isBlank(endText)) {
                return startText + "~结束";
            }
            return startText + "~" + (Boolean.TRUE.equals(crossDay) ? "次日" : "") + endText;
        }
    }

    private static class ShiftSectionTime {
        private final String startText;
        private final String endText;
        private final boolean crossDay;

        private ShiftSectionTime(String startText, String endText, boolean crossDay) {
            this.startText = startText;
            this.endText = endText;
            this.crossDay = crossDay;
        }
    }
}
