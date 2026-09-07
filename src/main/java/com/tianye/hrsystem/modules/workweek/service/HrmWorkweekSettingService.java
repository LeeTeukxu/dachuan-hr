package com.tianye.hrsystem.modules.workweek.service;

import com.tianye.hrsystem.common.BaseUtil;
import com.tianye.hrsystem.modules.workweek.bo.InitWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.workweek.bo.SaveWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.workweek.bo.UpdateWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.WorkweekDaySettingBO;
import com.tianye.hrsystem.modules.workweek.entity.HrmWorkweekDaySetting;
import com.tianye.hrsystem.modules.workweek.entity.HrmWorkweekSetting;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekDayCalendarVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekMonthCalendarVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekMonthSummaryVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekSettingVO;
import com.tianye.hrsystem.modules.workweek.vo.WorkweekYearSettingVO;
import com.tianye.hrsystem.model.HrmAttendanceLegalHolidays;
import com.tianye.hrsystem.repository.hrmAttendanceLegalHolidaysRepository;
import com.tianye.hrsystem.repository.hrmWorkweekDaySettingRepository;
import com.tianye.hrsystem.repository.hrmWorkweekSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HrmWorkweekSettingService {

    private static final int SINGLE_REST = 1;
    private static final int DOUBLE_REST = 2;
    private static final int WORK_DAY = 1;
    private static final int REST_DAY = 2;
    private static final String SOURCE_WEEKLY_WORK = "weekly_work";
    private static final String SOURCE_WEEKLY_REST = "weekly_rest";
    private static final String SOURCE_LEGAL_REST = "legal_rest";
    private static final String SOURCE_ADJUSTED_WORK = "adjusted_work";
    private static final String SOURCE_MANUAL = "manual";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private hrmWorkweekSettingRepository repository;

    @Autowired
    private hrmAttendanceLegalHolidaysRepository legalHolidaysRepository;

    @Autowired
    private hrmWorkweekDaySettingRepository daySettingRepository;

    public WorkweekYearSettingVO queryYearSettings(QueryWorkweekSettingBO queryBO) {
        Integer year = requireYear(queryBO != null ? queryBO.getYear() : null);
        return toYearVO(year, repository.findAllBySettingYearOrderByWeekNoAsc(year));
    }

    /**
     * 统计闭区间 [start, end] 内的工作日天数，判定优先级与月度日历一致：
     * 已保存日级设置 > 调休上班 > 法定休息 > 周休。单双休年度未初始化时按周一至周五默认上班，
     * 但调休上班/法定休息/已保存日级设置仍然生效。
     */
    public int countWorkDays(LocalDate start, LocalDate end) {
        return countWorkDays(start, end, null);
    }

    /**
     * 带员工排班覆盖的工作日统计：scheduledDayStatus 中显式 TRUE=排班上班（即使日历是休息也计为工作日）、
     * FALSE=排班休息（优先级最高，直接剔除）；未覆盖的日期按单双休/节假日日历判定。
     */
    public int countWorkDays(LocalDate start, LocalDate end, Map<LocalDate, Boolean> scheduledDayStatus) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        Map<LocalDate, HrmWorkweekSetting> settingByDate = new HashMap<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            List<HrmWorkweekSetting> rows = repository.findAllBySettingYearOrderByWeekNoAsc(year);
            if (rows != null && !rows.isEmpty()) {
                settingByDate.putAll(buildSettingByDate(year, rows));
            }
        }
        Map<LocalDate, HolidayOverride> holidayOverrides = new HashMap<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            holidayOverrides.putAll(queryHolidayOverrides(year));
        }
        Map<LocalDate, HrmWorkweekDaySetting> savedSettingByDate = querySavedDaySettingsByDate(start, end);
        int workDays = 0;
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            Boolean scheduled = scheduledDayStatus != null ? scheduledDayStatus.get(cursor) : null;
            if (scheduled != null) {
                if (scheduled) {
                    workDays++;
                }
                continue;
            }
            if (isWorkDay(cursor, settingByDate.get(cursor), holidayOverrides.get(cursor), savedSettingByDate.get(cursor))) {
                workDays++;
            }
        }
        return workDays;
    }

    private Map<LocalDate, HrmWorkweekDaySetting> querySavedDaySettingsByDate(LocalDate start, LocalDate end) {
        Map<LocalDate, HrmWorkweekDaySetting> savedSettingByDate = new HashMap<>();
        if (daySettingRepository == null) {
            return savedSettingByDate;
        }
        LocalDate monthCursor = start.withDayOfMonth(1);
        while (!monthCursor.isAfter(end)) {
            for (HrmWorkweekDaySetting row : querySavedDaySettings(monthCursor.getYear(), monthCursor.getMonthValue())) {
                LocalDate date = toLocalDate(row.getWorkDate());
                if (date != null) {
                    savedSettingByDate.put(date, row);
                }
            }
            monthCursor = monthCursor.plusMonths(1);
        }
        return savedSettingByDate;
    }

    private boolean isWorkDay(LocalDate date,
                              HrmWorkweekSetting weekSetting,
                              HolidayOverride holidayOverride,
                              HrmWorkweekDaySetting savedDaySetting) {
        if (savedDaySetting != null && savedDaySetting.getDayType() != null) {
            return savedDaySetting.getDayType() == WORK_DAY;
        }
        if (holidayOverride != null && holidayOverride.isAdjustedWorkDay()) {
            return true;
        }
        if (holidayOverride != null && holidayOverride.isLegalRestDay()) {
            return false;
        }
        if (weekSetting == null) {
            // 年度单双休未初始化：按周一至周五默认上班
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        }
        return !isWeeklyRestDay(date, weekSetting);
    }

    public WorkweekMonthCalendarVO queryMonthCalendar(QueryWorkweekMonthCalendarBO queryBO) {
        Integer year = requireYear(queryBO != null ? queryBO.getYear() : null);
        Integer month = requireMonth(queryBO != null ? queryBO.getMonth() : null);
        List<HrmWorkweekSetting> rows = repository.findAllBySettingYearOrderByWeekNoAsc(year);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("该年度单双休尚未初始化");
        }
        return buildMonthCalendar(year, month, rows, querySavedDaySettings(year, month));
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkweekYearSettingVO initYearSettings(InitWorkweekSettingBO initBO) {
        Integer year = requireYear(initBO != null ? initBO.getYear() : null);
        List<HrmWorkweekSetting> existing = repository.findAllBySettingYearOrderByWeekNoAsc(year);
        if (!existing.isEmpty()) {
            return toYearVO(year, existing);
        }

        Integer firstWeekType = requireWeekType(initBO.getFirstWeekType());
        List<HrmWorkweekSetting> rows = buildYearSettings(year, firstWeekType);
        repository.saveAll(rows);
        return toYearVO(year, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkweekYearSettingVO updateWeekType(UpdateWorkweekSettingBO updateBO) {
        Integer year = requireYear(updateBO != null ? updateBO.getYear() : null);
        Integer weekNo = updateBO != null ? updateBO.getWeekNo() : null;
        if (weekNo == null || weekNo < 1) {
            throw new IllegalArgumentException("周次不能为空");
        }
        Integer weekType = requireWeekType(updateBO.getWeekType());

        List<HrmWorkweekSetting> rows = repository.findAllBySettingYearOrderByWeekNoAsc(year);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("该年度单双休尚未初始化");
        }
        if (weekNo > rows.size()) {
            throw new IllegalArgumentException("周次不存在, weekNo=" + weekNo);
        }

        boolean recalculateFollowing = updateBO.getRecalculateFollowing() == null || updateBO.getRecalculateFollowing();
        Date now = new Date();
        if (recalculateFollowing) {
            for (int i = weekNo - 1; i < rows.size(); i++) {
                HrmWorkweekSetting row = rows.get(i);
                int currentType = (i == weekNo - 1) ? weekType : toggleWeekType(rows.get(i - 1).getWeekType());
                row.setWeekType(currentType);
                row.setRestDayText(resolveRestDayText(currentType));
                row.setManualOverride(i == weekNo - 1 ? 1 : 0);
                row.setUpdateTime(now);
            }
        } else {
            HrmWorkweekSetting row = rows.get(weekNo - 1);
            row.setWeekType(weekType);
            row.setRestDayText(resolveRestDayText(weekType));
            row.setManualOverride(1);
            row.setUpdateTime(now);
        }
        repository.saveAll(rows);
        return toYearVO(year, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkweekMonthCalendarVO saveMonthCalendar(SaveWorkweekMonthCalendarBO saveBO) {
        Integer year = requireYear(saveBO != null ? saveBO.getYear() : null);
        Integer month = requireMonth(saveBO != null ? saveBO.getMonth() : null);
        List<HrmWorkweekSetting> weekRows = repository.findAllBySettingYearOrderByWeekNoAsc(year);
        if (weekRows == null || weekRows.isEmpty()) {
            throw new IllegalArgumentException("该年度单双休尚未初始化");
        }

        List<WorkweekDaySettingBO> daySettings = saveBO != null ? saveBO.getDays() : null;
        List<HrmWorkweekDaySetting> existingRows = querySavedDaySettings(year, month);
        Map<LocalDate, HrmWorkweekDaySetting> existingByDate = buildSavedDaySettingByDate(existingRows);
        List<HrmWorkweekDaySetting> rows = buildDaySettingRows(year, month, daySettings, existingByDate);
        List<HrmWorkweekDaySetting> savedRows = daySettingRepository.saveAll(rows);
        Map<LocalDate, HrmWorkweekDaySetting> mergedByDate = buildSavedDaySettingByDate(existingRows);
        for (HrmWorkweekDaySetting savedRow : savedRows) {
            if (savedRow.getWorkDate() != null) {
                mergedByDate.put(toLocalDate(savedRow.getWorkDate()), savedRow);
            }
        }
        return buildMonthCalendar(year, month, weekRows, new ArrayList<>(mergedByDate.values()));
    }

    private List<HrmWorkweekSetting> buildYearSettings(Integer year, Integer firstWeekType) {
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);
        LocalDate start = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        Date now = new Date();

        List<HrmWorkweekSetting> rows = new ArrayList<>();
        int weekNo = 1;
        int weekType = firstWeekType;
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(7)) {
            HrmWorkweekSetting row = new HrmWorkweekSetting();
            row.setSettingId(BaseUtil.getNextId());
            row.setSettingYear(year);
            row.setWeekNo(weekNo++);
            row.setWeekType(weekType);
            row.setWeekStartDate(toDate(cursor));
            row.setWeekEndDate(toDate(cursor.plusDays(6)));
            row.setRestDayText(resolveRestDayText(weekType));
            row.setManualOverride(0);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rows.add(row);
            weekType = toggleWeekType(weekType);
        }
        return rows;
    }

    private WorkweekYearSettingVO toYearVO(Integer year, List<HrmWorkweekSetting> rows) {
        List<HrmWorkweekSetting> safeRows = rows != null ? rows : Collections.<HrmWorkweekSetting>emptyList();
        WorkweekYearSettingVO result = new WorkweekYearSettingVO();
        result.setYear(year);
        result.setInitialized(!safeRows.isEmpty());
        result.setTotalWeeks(safeRows.size());
        result.setTotalWorkDays(0);
        result.setTotalRestDays(0);
        result.setTotalLegalHolidayRestDays(0);
        result.setTotalAdjustedWorkDays(0);

        List<WorkweekSettingVO> weekSettings = new ArrayList<>();
        for (HrmWorkweekSetting row : safeRows) {
            WorkweekSettingVO item = new WorkweekSettingVO();
            item.setSettingId(row.getSettingId());
            item.setYear(row.getSettingYear());
            item.setWeekNo(row.getWeekNo());
            item.setWeekType(row.getWeekType());
            item.setWeekTypeName(resolveWeekTypeName(row.getWeekType()));
            item.setRestDayText(row.getRestDayText());
            item.setWeekStartDate(formatDate(row.getWeekStartDate()));
            item.setWeekEndDate(formatDate(row.getWeekEndDate()));
            item.setManualOverride(row.getManualOverride() != null ? row.getManualOverride() : 0);
            weekSettings.add(item);
        }
        result.setWeekSettings(weekSettings);
        List<WorkweekMonthSummaryVO> monthSummaries = buildMonthSummaries(year, safeRows);
        result.setMonthSummaries(monthSummaries);
        for (WorkweekMonthSummaryVO monthSummary : monthSummaries) {
            result.setTotalWorkDays(result.getTotalWorkDays() + monthSummary.getWorkDays());
            result.setTotalRestDays(result.getTotalRestDays() + monthSummary.getRestDays());
            result.setTotalLegalHolidayRestDays(result.getTotalLegalHolidayRestDays() + monthSummary.getLegalHolidayRestDays());
            result.setTotalAdjustedWorkDays(result.getTotalAdjustedWorkDays() + monthSummary.getAdjustedWorkDays());
        }
        return result;
    }

    private List<WorkweekMonthSummaryVO> buildMonthSummaries(Integer year, List<HrmWorkweekSetting> rows) {
        List<WorkweekMonthSummaryVO> monthSummaries = initMonthSummaries(year);
        if (rows == null || rows.isEmpty()) {
            return monthSummaries;
        }

        countWeeksByAnchorMonth(year, rows, monthSummaries);
        Map<LocalDate, HrmWorkweekSetting> settingByDate = buildSettingByDate(year, rows);
        Map<LocalDate, HolidayOverride> holidayOverrides = queryHolidayOverrides(year);
        LocalDate cursor = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        Map<Integer, Map<LocalDate, HrmWorkweekDaySetting>> savedSettingsByMonth = new HashMap<>();
        while (!cursor.isAfter(end)) {
            HrmWorkweekSetting workweekSetting = settingByDate.get(cursor);
            if (workweekSetting == null) {
                cursor = cursor.plusDays(1);
                continue;
            }
            Integer month = cursor.getMonthValue();
            Map<LocalDate, HrmWorkweekDaySetting> savedSettingByDate = savedSettingsByMonth.get(month);
            if (savedSettingByDate == null) {
                savedSettingByDate = buildSavedDaySettingByDate(querySavedDaySettings(year, month));
                savedSettingsByMonth.put(month, savedSettingByDate);
            }
            WorkweekDayCalendarVO dayCalendar = buildDayCalendarItem(
                    cursor, workweekSetting, holidayOverrides.get(cursor), savedSettingByDate.get(cursor));
            WorkweekMonthSummaryVO monthSummary = monthSummaries.get(month - 1);
            monthSummary.setTotalDays(monthSummary.getTotalDays() + 1);
            if (dayCalendar.getDayType() != null && dayCalendar.getDayType() == WORK_DAY) {
                monthSummary.setWorkDays(monthSummary.getWorkDays() + 1);
            } else {
                monthSummary.setRestDays(monthSummary.getRestDays() + 1);
            }
            if (SOURCE_WEEKLY_REST.equals(dayCalendar.getSourceType())) {
                monthSummary.setWeeklyRestDays(monthSummary.getWeeklyRestDays() + 1);
            } else if (SOURCE_LEGAL_REST.equals(dayCalendar.getSourceType())) {
                monthSummary.setLegalHolidayRestDays(monthSummary.getLegalHolidayRestDays() + 1);
            } else if (SOURCE_ADJUSTED_WORK.equals(dayCalendar.getSourceType())) {
                monthSummary.setAdjustedWorkDays(monthSummary.getAdjustedWorkDays() + 1);
            }
            cursor = cursor.plusDays(1);
        }
        return monthSummaries;
    }

    private List<WorkweekMonthSummaryVO> initMonthSummaries(Integer year) {
        List<WorkweekMonthSummaryVO> monthSummaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            WorkweekMonthSummaryVO item = new WorkweekMonthSummaryVO();
            item.setMonth(year + "-" + String.format("%02d", month));
            item.setLabel(month + "月");
            item.setTotalDays(0);
            item.setWorkDays(0);
            item.setRestDays(0);
            item.setWeeklyRestDays(0);
            item.setLegalHolidayRestDays(0);
            item.setAdjustedWorkDays(0);
            item.setTotalWeeks(0);
            item.setSingleRestWeeks(0);
            item.setDoubleRestWeeks(0);
            item.setManualOverrideWeeks(0);
            item.setWeekRangeText("--");
            monthSummaries.add(item);
        }
        return monthSummaries;
    }

    private void countWeeksByAnchorMonth(Integer year, List<HrmWorkweekSetting> rows, List<WorkweekMonthSummaryVO> monthSummaries) {
        for (HrmWorkweekSetting row : rows) {
            LocalDate anchorDate = resolveAnchorDate(year, row.getWeekStartDate());
            if (anchorDate == null) {
                continue;
            }
            WorkweekMonthSummaryVO monthSummary = monthSummaries.get(anchorDate.getMonthValue() - 1);
            monthSummary.setTotalWeeks(monthSummary.getTotalWeeks() + 1);
            if (row.getWeekType() != null && row.getWeekType() == DOUBLE_REST) {
                monthSummary.setDoubleRestWeeks(monthSummary.getDoubleRestWeeks() + 1);
            } else {
                monthSummary.setSingleRestWeeks(monthSummary.getSingleRestWeeks() + 1);
            }
            if (row.getManualOverride() != null && row.getManualOverride() == 1) {
                monthSummary.setManualOverrideWeeks(monthSummary.getManualOverrideWeeks() + 1);
            }
            Integer weekNo = row.getWeekNo();
            if (weekNo != null) {
                if (monthSummary.getFirstWeekNo() == null || weekNo < monthSummary.getFirstWeekNo()) {
                    monthSummary.setFirstWeekNo(weekNo);
                }
                if (monthSummary.getLastWeekNo() == null || weekNo > monthSummary.getLastWeekNo()) {
                    monthSummary.setLastWeekNo(weekNo);
                }
                monthSummary.setWeekRangeText(buildWeekRangeText(monthSummary.getFirstWeekNo(), monthSummary.getLastWeekNo()));
            }
        }
    }

    private WorkweekMonthCalendarVO buildMonthCalendar(Integer year,
                                                       Integer month,
                                                       List<HrmWorkweekSetting> weekRows,
                                                       List<HrmWorkweekDaySetting> savedDaySettings) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        Map<LocalDate, HrmWorkweekSetting> settingByDate = buildSettingByDate(year, weekRows);
        Map<LocalDate, HolidayOverride> holidayOverrides = queryHolidayOverrides(year);
        Map<LocalDate, HrmWorkweekDaySetting> savedSettingByDate = buildSavedDaySettingByDate(savedDaySettings);

        WorkweekMonthCalendarVO result = new WorkweekMonthCalendarVO();
        result.setYear(year);
        result.setMonth(month);
        result.setMonthText(year + "-" + String.format("%02d", month));
        result.setTotalDays(monthStart.lengthOfMonth());
        result.setWorkDays(0);
        result.setRestDays(0);
        result.setWeeklyRestDays(0);
        result.setLegalHolidayRestDays(0);
        result.setAdjustedWorkDays(0);
        result.setManualDays(0);

        for (LocalDate cursor = monthStart; cursor.isBefore(nextMonthStart); cursor = cursor.plusDays(1)) {
            HrmWorkweekSetting weekSetting = settingByDate.get(cursor);
            HolidayOverride holidayOverride = holidayOverrides.get(cursor);
            HrmWorkweekDaySetting savedDaySetting = savedSettingByDate.get(cursor);
            WorkweekDayCalendarVO item = buildDayCalendarItem(cursor, weekSetting, holidayOverride, savedDaySetting);
            result.getDays().add(item);

            if (item.getDayType() != null && item.getDayType() == WORK_DAY) {
                result.setWorkDays(result.getWorkDays() + 1);
            } else {
                result.setRestDays(result.getRestDays() + 1);
            }
            if (SOURCE_WEEKLY_REST.equals(item.getSourceType())) {
                result.setWeeklyRestDays(result.getWeeklyRestDays() + 1);
            } else if (SOURCE_LEGAL_REST.equals(item.getSourceType())) {
                result.setLegalHolidayRestDays(result.getLegalHolidayRestDays() + 1);
            } else if (SOURCE_ADJUSTED_WORK.equals(item.getSourceType())) {
                result.setAdjustedWorkDays(result.getAdjustedWorkDays() + 1);
            } else if (SOURCE_MANUAL.equals(item.getSourceType())) {
                result.setManualDays(result.getManualDays() + 1);
            }
        }
        return result;
    }

    private WorkweekDayCalendarVO buildDayCalendarItem(LocalDate date,
                                                       HrmWorkweekSetting weekSetting,
                                                       HolidayOverride holidayOverride,
                                                       HrmWorkweekDaySetting savedDaySetting) {
        WorkweekDayCalendarVO item = new WorkweekDayCalendarVO();
        item.setWorkDate(DAY_FORMATTER.format(date));
        item.setYear(date.getYear());
        item.setMonth(date.getMonthValue());
        item.setDayOfMonth(date.getDayOfMonth());
        item.setDayOfWeek(date.getDayOfWeek().getValue());
        item.setDayOfWeekName(resolveDayOfWeekName(date.getDayOfWeek()));
        item.setHolidayName(resolveHolidayName(date, holidayOverride));

        if (savedDaySetting != null && savedDaySetting.getDayType() != null) {
            item.setDayType(savedDaySetting.getDayType());
            item.setSourceType(SOURCE_MANUAL);
            item.setSourceName("手动设置");
        } else if (holidayOverride != null && holidayOverride.isAdjustedWorkDay()) {
            item.setDayType(WORK_DAY);
            item.setSourceType(SOURCE_ADJUSTED_WORK);
            item.setSourceName("调休上班");
        } else if (holidayOverride != null && holidayOverride.isLegalRestDay()) {
            item.setDayType(REST_DAY);
            item.setSourceType(SOURCE_LEGAL_REST);
            item.setSourceName("法定休息");
        } else if (isWeeklyRestDay(date, weekSetting)) {
            item.setDayType(REST_DAY);
            item.setSourceType(SOURCE_WEEKLY_REST);
            item.setSourceName("周休休息");
        } else {
            item.setDayType(WORK_DAY);
            item.setSourceType(SOURCE_WEEKLY_WORK);
            item.setSourceName("默认上班");
        }
        item.setDayTypeName(resolveDayTypeName(item.getDayType()));
        return item;
    }

    private List<HrmWorkweekDaySetting> querySavedDaySettings(Integer year, Integer month) {
        if (daySettingRepository == null) {
            return Collections.emptyList();
        }
        LocalDate monthStart = LocalDate.of(year, month, 1);
        return daySettingRepository.findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(
                year, toDate(monthStart), toDate(monthStart.plusMonths(1)));
    }

    private Map<LocalDate, HrmWorkweekDaySetting> buildSavedDaySettingByDate(List<HrmWorkweekDaySetting> savedDaySettings) {
        Map<LocalDate, HrmWorkweekDaySetting> savedSettingByDate = new HashMap<>();
        if (savedDaySettings == null || savedDaySettings.isEmpty()) {
            return savedSettingByDate;
        }
        for (HrmWorkweekDaySetting savedDaySetting : savedDaySettings) {
            if (savedDaySetting.getWorkDate() == null) {
                continue;
            }
            savedSettingByDate.put(toLocalDate(savedDaySetting.getWorkDate()), savedDaySetting);
        }
        return savedSettingByDate;
    }

    private List<HrmWorkweekDaySetting> buildDaySettingRows(Integer year,
                                                             Integer month,
                                                             List<WorkweekDaySettingBO> daySettings,
                                                             Map<LocalDate, HrmWorkweekDaySetting> existingByDate) {
        if (daySettings == null || daySettings.isEmpty()) {
            throw new IllegalArgumentException("月度日历不能为空");
        }

        Date now = new Date();
        Set<LocalDate> seenDates = new HashSet<>();
        List<HrmWorkweekDaySetting> rows = new ArrayList<>();
        for (WorkweekDaySettingBO daySetting : daySettings) {
            LocalDate workDate = parseWorkDate(daySetting != null ? daySetting.getWorkDate() : null);
            if (workDate.getYear() != year || workDate.getMonthValue() != month) {
                throw new IllegalArgumentException("日期不属于当前月份: " + DAY_FORMATTER.format(workDate));
            }
            if (!seenDates.add(workDate)) {
                throw new IllegalArgumentException("日期重复: " + DAY_FORMATTER.format(workDate));
            }
            Integer dayType = requireDayType(daySetting.getDayType());
            HrmWorkweekDaySetting row = existingByDate != null ? existingByDate.get(workDate) : null;
            if (row == null) {
                row = new HrmWorkweekDaySetting();
                row.setDaySettingId(BaseUtil.getNextId());
                row.setSettingYear(year);
                row.setSettingMonth(month);
                row.setWorkDate(toDate(workDate));
                row.setCreateTime(now);
            }
            row.setDayType(dayType);
            row.setUpdateTime(now);
            rows.add(row);
        }
        return rows;
    }

    private Map<LocalDate, HrmWorkweekSetting> buildSettingByDate(Integer year, List<HrmWorkweekSetting> rows) {
        Map<LocalDate, HrmWorkweekSetting> settingByDate = new HashMap<>();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        for (HrmWorkweekSetting row : rows) {
            if (row.getWeekStartDate() == null || row.getWeekEndDate() == null) {
                continue;
            }
            LocalDate start = toLocalDate(row.getWeekStartDate());
            LocalDate end = toLocalDate(row.getWeekEndDate());
            LocalDate cursor = start.isBefore(yearStart) ? yearStart : start;
            LocalDate safeEnd = end.isAfter(yearEnd) ? yearEnd : end;
            while (!cursor.isAfter(safeEnd)) {
                settingByDate.put(cursor, row);
                cursor = cursor.plusDays(1);
            }
        }
        return settingByDate;
    }

    private Map<LocalDate, HolidayOverride> queryHolidayOverrides(Integer year) {
        Map<LocalDate, HolidayOverride> holidayOverrides = new HashMap<>();
        if (legalHolidaysRepository == null) {
            return buildBuiltinHolidayOverrides(year);
        }
        List<HrmAttendanceLegalHolidays> holidays = legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(
                toDate(LocalDate.of(year, 1, 1)), toDate(LocalDate.of(year + 1, 1, 1)));
        if (holidays == null || holidays.isEmpty()) {
            return buildBuiltinHolidayOverrides(year);
        }
        for (HrmAttendanceLegalHolidays holiday : holidays) {
            if (holiday.getHolidayTime() == null || holiday.getType() == null) {
                continue;
            }
            LocalDate holidayDate = toLocalDate(holiday.getHolidayTime());
            HolidayOverride override = holidayOverrides.get(holidayDate);
            if (override == null) {
                override = new HolidayOverride();
                holidayOverrides.put(holidayDate, override);
            }
            if (holiday.getType() == 1) {
                override.setLegalRestDay(false);
                override.setAdjustedWorkDay(true);
            } else if (holiday.getType() == 2) {
                override.setAdjustedWorkDay(false);
                override.setLegalRestDay(true);
            }
        }
        return holidayOverrides;
    }

    private Map<LocalDate, HolidayOverride> buildBuiltinHolidayOverrides(Integer year) {
        Map<LocalDate, HolidayOverride> holidayOverrides = new HashMap<>();
        if (year == null || year != 2026) {
            return holidayOverrides;
        }
        addLegalRestDays(holidayOverrides,
                "2026-01-01", "2026-01-02", "2026-01-03",
                "2026-02-15", "2026-02-16", "2026-02-17", "2026-02-18", "2026-02-19",
                "2026-02-20", "2026-02-21", "2026-02-22", "2026-02-23",
                "2026-04-04", "2026-04-05", "2026-04-06",
                "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05",
                "2026-06-19", "2026-06-20", "2026-06-21",
                "2026-09-25", "2026-09-26", "2026-09-27",
                "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07");
        addAdjustedWorkDays(holidayOverrides,
                "2026-01-04",
                "2026-02-14", "2026-02-28",
                "2026-05-09",
                "2026-09-20", "2026-10-10");
        return holidayOverrides;
    }

    private void addLegalRestDays(Map<LocalDate, HolidayOverride> holidayOverrides, String... days) {
        for (String day : days) {
            HolidayOverride holidayOverride = getOrCreateHolidayOverride(holidayOverrides, day);
            holidayOverride.setAdjustedWorkDay(false);
            holidayOverride.setLegalRestDay(true);
        }
    }

    private void addAdjustedWorkDays(Map<LocalDate, HolidayOverride> holidayOverrides, String... days) {
        for (String day : days) {
            HolidayOverride holidayOverride = getOrCreateHolidayOverride(holidayOverrides, day);
            holidayOverride.setLegalRestDay(false);
            holidayOverride.setAdjustedWorkDay(true);
        }
    }

    private HolidayOverride getOrCreateHolidayOverride(Map<LocalDate, HolidayOverride> holidayOverrides, String day) {
        LocalDate date = LocalDate.parse(day, DAY_FORMATTER);
        HolidayOverride holidayOverride = holidayOverrides.get(date);
        if (holidayOverride == null) {
            holidayOverride = new HolidayOverride();
            holidayOverrides.put(date, holidayOverride);
        }
        return holidayOverride;
    }

    private boolean isWeeklyRestDay(LocalDate date, HrmWorkweekSetting setting) {
        if (setting == null) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (setting.getWeekType() != null && setting.getWeekType() == DOUBLE_REST) {
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }
        return dayOfWeek == DayOfWeek.SUNDAY;
    }

    private LocalDate resolveAnchorDate(Integer year, Date weekStartDate) {
        if (weekStartDate == null) {
            return null;
        }
        LocalDate anchorDate = toLocalDate(weekStartDate);
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        if (anchorDate.isBefore(yearStart)) {
            return yearStart;
        }
        if (anchorDate.isAfter(yearEnd)) {
            return yearEnd;
        }
        return anchorDate;
    }

    private String buildWeekRangeText(Integer firstWeekNo, Integer lastWeekNo) {
        if (firstWeekNo == null || lastWeekNo == null) {
            return "--";
        }
        return firstWeekNo.equals(lastWeekNo) ? "第" + firstWeekNo + "周" : "第" + firstWeekNo + "-" + lastWeekNo + "周";
    }

    private Integer requireYear(Integer year) {
        if (year == null || year < 1900) {
            throw new IllegalArgumentException("年份不能为空");
        }
        return year;
    }

    private Integer requireMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("月份不能为空");
        }
        return month;
    }

    private Integer requireWeekType(Integer weekType) {
        if (weekType == null || (weekType != SINGLE_REST && weekType != DOUBLE_REST)) {
            throw new IllegalArgumentException("单双休类型不合法");
        }
        return weekType;
    }

    private Integer requireDayType(Integer dayType) {
        if (dayType == null || (dayType != WORK_DAY && dayType != REST_DAY)) {
            throw new IllegalArgumentException("日期类型不合法");
        }
        return dayType;
    }

    private int toggleWeekType(Integer weekType) {
        return weekType != null && weekType == SINGLE_REST ? DOUBLE_REST : SINGLE_REST;
    }

    private String resolveWeekTypeName(Integer weekType) {
        return weekType != null && weekType == DOUBLE_REST ? "双休" : "单休";
    }

    private String resolveDayTypeName(Integer dayType) {
        return dayType != null && dayType == REST_DAY ? "休息" : "上班";
    }

    private String resolveDayOfWeekName(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
        switch (dayOfWeek) {
            case MONDAY:
                return "周一";
            case TUESDAY:
                return "周二";
            case WEDNESDAY:
                return "周三";
            case THURSDAY:
                return "周四";
            case FRIDAY:
                return "周五";
            case SATURDAY:
                return "周六";
            case SUNDAY:
                return "周日";
            default:
                return null;
        }
    }

    private String resolveHolidayName(LocalDate date, HolidayOverride holidayOverride) {
        String officialName = resolveOfficialHolidayName(date);
        if (officialName != null) {
            return officialName;
        }
        if (holidayOverride == null || (!holidayOverride.isAdjustedWorkDay() && !holidayOverride.isLegalRestDay())) {
            return null;
        }
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        if (month == 1 && day <= 4) {
            return "元旦";
        }
        if ((month == 1 && day >= 15) || month == 2) {
            return "春节";
        }
        if (month == 4 && day >= 3 && day <= 7) {
            return "清明节";
        }
        if (month == 5) {
            return "劳动节";
        }
        if (month == 6) {
            return "端午节";
        }
        if (month == 9) {
            return "中秋节";
        }
        if (month == 10) {
            return "国庆节";
        }
        return null;
    }

    private String resolveOfficialHolidayName(LocalDate date) {
        if (date.getYear() != 2026) {
            return null;
        }
        String day = DAY_FORMATTER.format(date);
        if (isDateIn(day, "2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04")) {
            return "元旦";
        }
        if (isDateIn(day, "2026-02-14", "2026-02-15", "2026-02-16", "2026-02-17", "2026-02-18",
                "2026-02-19", "2026-02-20", "2026-02-21", "2026-02-22", "2026-02-23", "2026-02-28")) {
            return "春节";
        }
        if (isDateIn(day, "2026-04-04", "2026-04-05", "2026-04-06")) {
            return "清明节";
        }
        if (isDateIn(day, "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05", "2026-05-09")) {
            return "劳动节";
        }
        if (isDateIn(day, "2026-06-19", "2026-06-20", "2026-06-21")) {
            return "端午节";
        }
        if (isDateIn(day, "2026-09-25", "2026-09-26", "2026-09-27")) {
            return "中秋节";
        }
        if (isDateIn(day, "2026-09-20", "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04",
                "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-10")) {
            return "国庆节";
        }
        return null;
    }

    private boolean isDateIn(String day, String... holidays) {
        if (day == null || holidays == null) {
            return false;
        }
        for (String holiday : holidays) {
            if (day.equals(holiday)) {
                return true;
            }
        }
        return false;
    }

    private String resolveRestDayText(Integer weekType) {
        return weekType != null && weekType == DOUBLE_REST ? "周六、周日" : "周日";
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DAY_FORMATTER.format(toLocalDate(date));
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate parseWorkDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("日期不能为空");
        }
        try {
            return LocalDate.parse(value.trim(), DAY_FORMATTER);
        } catch (Exception ax) {
            throw new IllegalArgumentException("日期格式不合法: " + value);
        }
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static class HolidayOverride {

        private boolean adjustedWorkDay;

        private boolean legalRestDay;

        boolean isAdjustedWorkDay() {
            return adjustedWorkDay;
        }

        void setAdjustedWorkDay(boolean adjustedWorkDay) {
            this.adjustedWorkDay = adjustedWorkDay;
        }

        boolean isLegalRestDay() {
            return legalRestDay;
        }

        void setLegalRestDay(boolean legalRestDay) {
            this.legalRestDay = legalRestDay;
        }
    }
}
