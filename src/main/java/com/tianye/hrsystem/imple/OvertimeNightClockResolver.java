package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.HrmAttendanceClock;
import com.tianye.hrsystem.model.HrmAttendanceShift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class OvertimeNightClockResolver {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm", Locale.CHINA);
    private static final LocalTime NIGHT_SHIFT_MIN_END_TIME = LocalTime.of(3, 0);

    private OvertimeNightClockResolver() {
    }

    public static ScheduledEndTimeResolution resolveScheduledEndTimeResolution(LocalDate workDate,
                                                                               List<HrmAttendanceClock> offClocks,
                                                                               HrmAttendanceShift shift,
                                                                               LocalDateTime planEndTime,
                                                                               ZoneId zoneId) {
        LocalDateTime shiftEnd = resolveShiftEndTime(workDate, shift);
        if (shiftEnd != null) {
            return new ScheduledEndTimeResolution(shiftEnd, ScheduledEndTimeSource.SHIFT);
        }
        if (planEndTime != null) {
            return new ScheduledEndTimeResolution(planEndTime, ScheduledEndTimeSource.PLAN_CHECK_TIME);
        }
        LocalDateTime attendanceEndTime = (offClocks != null ? offClocks : Collections.<HrmAttendanceClock>emptyList()).stream()
                .map(HrmAttendanceClock::getAttendanceTime)
                .filter(Objects::nonNull)
                .map(date -> LocalDateTime.ofInstant(date.toInstant(), zoneId))
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (attendanceEndTime != null) {
            return new ScheduledEndTimeResolution(attendanceEndTime, ScheduledEndTimeSource.CLOCK_ATTENDANCE);
        }
        return new ScheduledEndTimeResolution(null, ScheduledEndTimeSource.NONE);
    }

    public static LocalDateTime resolveScheduledEndTime(LocalDate workDate,
                                                        List<HrmAttendanceClock> offClocks,
                                                        HrmAttendanceShift shift,
                                                        LocalDateTime planEndTime,
                                                        ZoneId zoneId) {
        return resolveScheduledEndTimeResolution(workDate, offClocks, shift, planEndTime, zoneId).getTime();
    }

    public static LocalDateTime resolveActualOffTime(List<HrmAttendanceClock> offClocks, ZoneId zoneId) {
        return (offClocks != null ? offClocks : Collections.<HrmAttendanceClock>emptyList()).stream()
                .map(HrmAttendanceClock::getClockTime)
                .filter(Objects::nonNull)
                .map(date -> LocalDateTime.ofInstant(date.toInstant(), zoneId))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public static boolean isNightShift(LocalDate workDate, LocalDateTime scheduledEndTime, LocalDateTime actualOffTime) {
        if (workDate == null || scheduledEndTime == null || actualOffTime == null) {
            return false;
        }
        return scheduledEndTime.toLocalDate().isAfter(workDate)
                && actualOffTime.toLocalDate().isAfter(workDate)
                && !actualOffTime.toLocalTime().isBefore(NIGHT_SHIFT_MIN_END_TIME);
    }

    /**
     * 夜班判定（2026-09-06 口径）：排班为夜班别或排班结束越过次日凌晨3点，
     * 且实际下班时间（缺卡回退排班结束时间）达到次日凌晨3点及之后，计1个夜班。
     */
    public static boolean isNightShift(LocalDate workDate, boolean scheduledNight, LocalDateTime effectiveOffTime) {
        if (workDate == null || !scheduledNight || effectiveOffTime == null) {
            return false;
        }
        return !effectiveOffTime.isBefore(workDate.plusDays(1).atTime(NIGHT_SHIFT_MIN_END_TIME));
    }

    public static boolean isScheduledNight(LocalDate workDate, String shiftPeriod, LocalDateTime scheduledEndTime) {
        if (shiftPeriod != null && "night".equalsIgnoreCase(shiftPeriod.trim())) {
            return true;
        }
        if (workDate == null || scheduledEndTime == null) {
            return false;
        }
        return !scheduledEndTime.isBefore(workDate.plusDays(1).atTime(NIGHT_SHIFT_MIN_END_TIME));
    }

    private static LocalDateTime resolveShiftEndTime(LocalDate workDate, HrmAttendanceShift shift) {
        if (workDate == null || shift == null) {
            return null;
        }
        String start = firstNotBlank(shift.getStart3(), shift.getStart2(), shift.getStart1());
        String end = firstNotBlank(shift.getEnd3(), shift.getEnd2(), shift.getEnd1());
        if (start == null || end == null) {
            return null;
        }
        try {
            LocalTime startTime = LocalTime.parse(start, TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(end, TIME_FORMATTER);
            LocalDateTime endDateTime = LocalDateTime.of(workDate, endTime);
            if (endTime.isBefore(startTime)) {
                endDateTime = endDateTime.plusDays(1);
            }
            return endDateTime;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public enum ScheduledEndTimeSource {
        SHIFT,
        PLAN_CHECK_TIME,
        CLOCK_ATTENDANCE,
        NONE
    }

    public static final class ScheduledEndTimeResolution {
        private final LocalDateTime time;
        private final ScheduledEndTimeSource source;

        public ScheduledEndTimeResolution(LocalDateTime time, ScheduledEndTimeSource source) {
            this.time = time;
            this.source = source != null ? source : ScheduledEndTimeSource.NONE;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public ScheduledEndTimeSource getSource() {
            return source;
        }
    }
}
