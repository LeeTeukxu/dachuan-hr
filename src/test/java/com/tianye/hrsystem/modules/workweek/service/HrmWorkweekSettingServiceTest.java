package com.tianye.hrsystem.modules.workweek.service;

import com.tianye.hrsystem.modules.workweek.bo.InitWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.QueryWorkweekMonthCalendarBO;
import com.tianye.hrsystem.modules.workweek.bo.UpdateWorkweekSettingBO;
import com.tianye.hrsystem.modules.workweek.bo.SaveWorkweekMonthCalendarBO;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmWorkweekSettingServiceTest {

    @InjectMocks
    private HrmWorkweekSettingService service;

    @Mock
    private hrmWorkweekSettingRepository repository;

    @Mock
    private hrmAttendanceLegalHolidaysRepository legalHolidaysRepository;

    @Mock
    private hrmWorkweekDaySettingRepository daySettingRepository;

    @Test
    public void initYearSettings_shouldGenerateNaturalWeeks_whenYearHasNoExistingRecords() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(Collections.emptyList());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        InitWorkweekSettingBO initBO = new InitWorkweekSettingBO();
        initBO.setYear(2026);
        initBO.setFirstWeekType(1);

        WorkweekYearSettingVO result = service.initYearSettings(initBO);

        Assert.assertTrue(result.getInitialized());
        Assert.assertEquals(Integer.valueOf(2026), result.getYear());
        Assert.assertEquals(Integer.valueOf(53), result.getTotalWeeks());
        Assert.assertEquals(53, result.getWeekSettings().size());

        WorkweekSettingVO week1 = result.getWeekSettings().get(0);
        WorkweekSettingVO week2 = result.getWeekSettings().get(1);
        WorkweekSettingVO week53 = result.getWeekSettings().get(52);

        Assert.assertEquals(Integer.valueOf(1), week1.getWeekNo());
        Assert.assertEquals(Integer.valueOf(1), week1.getWeekType());
        Assert.assertEquals("单休", week1.getWeekTypeName());
        Assert.assertEquals("周日", week1.getRestDayText());
        Assert.assertEquals("2025-12-29", week1.getWeekStartDate());
        Assert.assertEquals("2026-01-04", week1.getWeekEndDate());
        Assert.assertEquals(Integer.valueOf(2), week2.getWeekType());
        Assert.assertEquals(Integer.valueOf(1), week53.getWeekType());
        verify(repository).saveAll(anyList());
    }

    @Test
    public void queryYearSettings_shouldReadPersistedRecords_whenYearAlreadyInitialized() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(Arrays.asList(
                buildSetting(1L, 2026, 1, 1, 0, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
                buildSetting(2L, 2026, 2, 2, 0, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11))
        ));

        QueryWorkweekSettingBO queryBO = new QueryWorkweekSettingBO();
        queryBO.setYear(2026);

        WorkweekYearSettingVO result = service.queryYearSettings(queryBO);

        Assert.assertTrue(result.getInitialized());
        Assert.assertEquals(Integer.valueOf(2), result.getTotalWeeks());
        Assert.assertEquals(2, result.getWeekSettings().size());
        Assert.assertEquals("双休", result.getWeekSettings().get(1).getWeekTypeName());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    public void updateWeekType_shouldRecalculateFromEditedWeekToYearEnd() {
        List<HrmWorkweekSetting> existingRows = Arrays.asList(
                buildSetting(1L, 2026, 1, 1, 0, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
                buildSetting(2L, 2026, 2, 2, 0, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)),
                buildSetting(3L, 2026, 3, 1, 0, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)),
                buildSetting(4L, 2026, 4, 2, 0, LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 25))
        );
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(existingRows);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWorkweekSettingBO updateBO = new UpdateWorkweekSettingBO();
        updateBO.setYear(2026);
        updateBO.setWeekNo(2);
        updateBO.setWeekType(1);

        WorkweekYearSettingVO result = service.updateWeekType(updateBO);

        Assert.assertEquals(4, result.getWeekSettings().size());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(0).getWeekType());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(1).getWeekType());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(1).getManualOverride());
        Assert.assertEquals(Integer.valueOf(2), result.getWeekSettings().get(2).getWeekType());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(3).getWeekType());
        verify(repository).saveAll(anyList());
    }

    @Test
    public void updateWeekType_shouldOnlyChangeSelectedWeek_whenRecalculateFollowingIsFalse() {
        List<HrmWorkweekSetting> existingRows = Arrays.asList(
                buildSetting(1L, 2026, 1, 1, 0, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
                buildSetting(2L, 2026, 2, 2, 0, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)),
                buildSetting(3L, 2026, 3, 1, 0, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)),
                buildSetting(4L, 2026, 4, 2, 0, LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 25))
        );
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(existingRows);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWorkweekSettingBO updateBO = new UpdateWorkweekSettingBO();
        updateBO.setYear(2026);
        updateBO.setWeekNo(2);
        updateBO.setWeekType(1);
        updateBO.setRecalculateFollowing(false);

        WorkweekYearSettingVO result = service.updateWeekType(updateBO);

        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(0).getWeekType());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(1).getWeekType());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(1).getManualOverride());
        Assert.assertEquals(Integer.valueOf(1), result.getWeekSettings().get(2).getWeekType());
        Assert.assertEquals(Integer.valueOf(2), result.getWeekSettings().get(3).getWeekType());
        verify(repository).saveAll(anyList());
    }

    @Test
    public void queryYearSettings_shouldCalculateMonthlyWorkAndRestDaysWithLegalHolidayOverrides() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(Arrays.asList(
                buildSetting(1L, 2026, 1, 1, 0, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
                buildSetting(2L, 2026, 2, 2, 0, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)),
                buildSetting(3L, 2026, 3, 1, 0, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)),
                buildSetting(4L, 2026, 4, 2, 0, LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 25)),
                buildSetting(5L, 2026, 5, 1, 0, LocalDate.of(2026, 1, 26), LocalDate.of(2026, 2, 1))
        ));
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(
                        buildLegalHoliday(LocalDate.of(2026, 1, 1), 2),
                        buildLegalHoliday(LocalDate.of(2026, 1, 10), 1)
                ));

        QueryWorkweekSettingBO queryBO = new QueryWorkweekSettingBO();
        queryBO.setYear(2026);

        WorkweekYearSettingVO result = service.queryYearSettings(queryBO);

        WorkweekMonthSummaryVO january = result.getMonthSummaries().get(0);
        Assert.assertEquals("2026-01", january.getMonth());
        Assert.assertEquals(Integer.valueOf(31), january.getTotalDays());
        Assert.assertEquals(Integer.valueOf(25), january.getWorkDays());
        Assert.assertEquals(Integer.valueOf(6), january.getRestDays());
        Assert.assertEquals(Integer.valueOf(1), january.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(1), january.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(25), result.getTotalWorkDays());
        Assert.assertEquals(Integer.valueOf(7), result.getTotalRestDays());
    }

    @Test
    public void queryYearSettings_shouldUseBuiltIn2026HolidayScheduleWhenHolidayTableIsEmpty() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(buildFullYearRows(2026, 1));
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        QueryWorkweekSettingBO queryBO = new QueryWorkweekSettingBO();
        queryBO.setYear(2026);

        WorkweekYearSettingVO result = service.queryYearSettings(queryBO);

        WorkweekMonthSummaryVO january = result.getMonthSummaries().get(0);
        WorkweekMonthSummaryVO february = result.getMonthSummaries().get(1);
        WorkweekMonthSummaryVO may = result.getMonthSummaries().get(4);
        WorkweekMonthSummaryVO september = result.getMonthSummaries().get(8);
        WorkweekMonthSummaryVO october = result.getMonthSummaries().get(9);
        Assert.assertEquals(Integer.valueOf(33), result.getTotalLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(6), result.getTotalAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(3), january.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(1), january.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(9), february.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(2), february.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(5), may.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(1), may.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(1), september.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(7), october.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(1), october.getAdjustedWorkDays());
    }

    @Test
    public void queryYearSettings_shouldUseSavedDaySettingsWhenCountingMonthSummary() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(buildJanuaryRows());
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(daySettingRepository.findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(buildDaySetting(LocalDate.of(2026, 1, 3), 2)));

        QueryWorkweekSettingBO queryBO = new QueryWorkweekSettingBO();
        queryBO.setYear(2026);

        WorkweekYearSettingVO result = service.queryYearSettings(queryBO);

        WorkweekMonthSummaryVO january = result.getMonthSummaries().get(0);
        Assert.assertEquals(Integer.valueOf(23), january.getWorkDays());
        Assert.assertEquals(Integer.valueOf(8), january.getRestDays());
        Assert.assertEquals(Integer.valueOf(2), january.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(1), january.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(23), result.getTotalWorkDays());
        Assert.assertEquals(Integer.valueOf(9), result.getTotalRestDays());
    }

    @Test
    public void queryMonthCalendar_shouldUseLegalHolidayDefaultsAndSavedManualOverrides() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(buildJanuaryRows());
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(
                        buildLegalHoliday(LocalDate.of(2026, 1, 1), 2),
                        buildLegalHoliday(LocalDate.of(2026, 1, 10), 1)
                ));
        when(daySettingRepository.findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(buildDaySetting(LocalDate.of(2026, 1, 2), 2)));

        QueryWorkweekMonthCalendarBO queryBO = new QueryWorkweekMonthCalendarBO();
        queryBO.setYear(2026);
        queryBO.setMonth(1);

        WorkweekMonthCalendarVO result = service.queryMonthCalendar(queryBO);

        Assert.assertEquals(Integer.valueOf(2026), result.getYear());
        Assert.assertEquals(Integer.valueOf(1), result.getMonth());
        Assert.assertEquals("2026-01", result.getMonthText());
        Assert.assertEquals(31, result.getDays().size());
        Assert.assertEquals(Integer.valueOf(24), result.getWorkDays());
        Assert.assertEquals(Integer.valueOf(7), result.getRestDays());
        Assert.assertEquals(Integer.valueOf(1), result.getLegalHolidayRestDays());
        Assert.assertEquals(Integer.valueOf(1), result.getAdjustedWorkDays());
        Assert.assertEquals(Integer.valueOf(1), result.getManualDays());

        WorkweekDayCalendarVO januaryFirst = result.getDays().get(0);
        Assert.assertEquals("2026-01-01", januaryFirst.getWorkDate());
        Assert.assertEquals(Integer.valueOf(2), januaryFirst.getDayType());
        Assert.assertEquals("法定休息", januaryFirst.getSourceName());

        WorkweekDayCalendarVO januarySecond = result.getDays().get(1);
        Assert.assertEquals("2026-01-02", januarySecond.getWorkDate());
        Assert.assertEquals(Integer.valueOf(2), januarySecond.getDayType());
        Assert.assertEquals("手动设置", januarySecond.getSourceName());

        WorkweekDayCalendarVO januaryTenth = result.getDays().get(9);
        Assert.assertEquals("2026-01-10", januaryTenth.getWorkDate());
        Assert.assertEquals(Integer.valueOf(1), januaryTenth.getDayType());
        Assert.assertEquals("调休上班", januaryTenth.getSourceName());
    }

    @Test
    public void queryMonthCalendar_shouldExposeHolidayNameForLegalHolidayDates() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(Arrays.asList(
                buildSetting(18L, 2026, 18, 2, 0, LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 3)),
                buildSetting(19L, 2026, 19, 1, 0, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10))
        ));
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(
                        buildLegalHoliday(LocalDate.of(2026, 5, 1), 2),
                        buildLegalHoliday(LocalDate.of(2026, 5, 9), 1)
                ));
        when(daySettingRepository.findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        QueryWorkweekMonthCalendarBO queryBO = new QueryWorkweekMonthCalendarBO();
        queryBO.setYear(2026);
        queryBO.setMonth(5);

        WorkweekMonthCalendarVO result = service.queryMonthCalendar(queryBO);

        WorkweekDayCalendarVO laborDay = result.getDays().get(0);
        Assert.assertEquals("2026-05-01", laborDay.getWorkDate());
        Assert.assertEquals("劳动节", laborDay.getHolidayName());
        Assert.assertEquals("法定休息", laborDay.getSourceName());

        WorkweekDayCalendarVO adjustedWorkDay = result.getDays().get(8);
        Assert.assertEquals("2026-05-09", adjustedWorkDay.getWorkDate());
        Assert.assertEquals("劳动节", adjustedWorkDay.getHolidayName());
        Assert.assertEquals("调休上班", adjustedWorkDay.getSourceName());
    }

    @Test
    public void saveMonthCalendar_shouldUpsertOnlySubmittedDaysWithoutDeletingWholeMonth() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(buildJanuaryRows());
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        HrmWorkweekDaySetting existing = buildDaySetting(LocalDate.of(2026, 1, 31), 1);
        existing.setDaySettingId(99L);
        when(daySettingRepository.findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(existing));
        when(daySettingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SaveWorkweekMonthCalendarBO saveBO = new SaveWorkweekMonthCalendarBO();
        saveBO.setYear(2026);
        saveBO.setMonth(1);
        WorkweekDaySettingBO daySettingBO = new WorkweekDaySettingBO();
        daySettingBO.setWorkDate("2026-01-31");
        daySettingBO.setDayType(2);
        saveBO.getDays().add(daySettingBO);

        WorkweekMonthCalendarVO result = service.saveMonthCalendar(saveBO);

        ArgumentCaptor<List<HrmWorkweekDaySetting>> captor = ArgumentCaptor.forClass(List.class);
        verify(daySettingRepository, never()).deleteBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThan(any(), any(Date.class), any(Date.class));
        verify(daySettingRepository).saveAll(captor.capture());
        Assert.assertEquals(1, captor.getValue().size());
        Assert.assertEquals(Long.valueOf(99L), captor.getValue().get(0).getDaySettingId());
        Assert.assertEquals(Integer.valueOf(2), captor.getValue().get(0).getDayType());
        Assert.assertEquals(Integer.valueOf(1), result.getManualDays());
        Assert.assertEquals(Integer.valueOf(2), result.getDays().get(30).getDayType());
    }

    @Test
    public void saveMonthCalendar_shouldUpsertSubmittedMonthDaysAndReturnSavedCalendar() {
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(buildJanuaryRows());
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(daySettingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SaveWorkweekMonthCalendarBO saveBO = new SaveWorkweekMonthCalendarBO();
        saveBO.setYear(2026);
        saveBO.setMonth(1);
        for (int day = 1; day <= 31; day++) {
            WorkweekDaySettingBO daySettingBO = new WorkweekDaySettingBO();
            daySettingBO.setWorkDate("2026-01-" + String.format("%02d", day));
            daySettingBO.setDayType(day == 1 ? 2 : 1);
            saveBO.getDays().add(daySettingBO);
        }

        WorkweekMonthCalendarVO result = service.saveMonthCalendar(saveBO);

        ArgumentCaptor<List<HrmWorkweekDaySetting>> captor = ArgumentCaptor.forClass(List.class);
        verify(daySettingRepository, never()).deleteBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThan(any(), any(Date.class), any(Date.class));
        verify(daySettingRepository).saveAll(captor.capture());
        Assert.assertEquals(31, captor.getValue().size());
        Assert.assertEquals(Integer.valueOf(2026), captor.getValue().get(0).getSettingYear());
        Assert.assertEquals(Integer.valueOf(1), captor.getValue().get(0).getSettingMonth());
        Assert.assertEquals(Integer.valueOf(2), captor.getValue().get(0).getDayType());
        Assert.assertEquals(Integer.valueOf(30), result.getWorkDays());
        Assert.assertEquals(Integer.valueOf(1), result.getRestDays());
        Assert.assertEquals(Integer.valueOf(31), result.getManualDays());
    }

    private List<HrmWorkweekSetting> buildJanuaryRows() {
        return Arrays.asList(
                buildSetting(1L, 2026, 1, 1, 0, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
                buildSetting(2L, 2026, 2, 2, 0, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)),
                buildSetting(3L, 2026, 3, 1, 0, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)),
                buildSetting(4L, 2026, 4, 2, 0, LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 25)),
                buildSetting(5L, 2026, 5, 1, 0, LocalDate.of(2026, 1, 26), LocalDate.of(2026, 2, 1))
        );
    }

    private List<HrmWorkweekSetting> buildFullYearRows(Integer year, Integer firstWeekType) {
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);
        LocalDate weekStartDate = firstDay.minusDays(firstDay.getDayOfWeek().getValue() - 1);
        LocalDate weekEndLimit = lastDay.plusDays(7 - lastDay.getDayOfWeek().getValue());
        List<HrmWorkweekSetting> rows = new java.util.ArrayList<>();
        Integer weekType = firstWeekType;
        Integer weekNo = 1;
        for (LocalDate cursor = weekStartDate; !cursor.isAfter(weekEndLimit); cursor = cursor.plusDays(7)) {
            rows.add(buildSetting(Long.valueOf(weekNo), year, weekNo, weekType, 0, cursor, cursor.plusDays(6)));
            weekType = weekType == 1 ? 2 : 1;
            weekNo++;
        }
        return rows;
    }

    @Test
    public void countWorkDays_shouldCountWorkdaysAcrossMonthBoundary() {
        // 复现跨月调休 7/24(周五)~8/4(周二)：第30周单休(7/20-7/26，周六上班)、第31周双休(7/27-8/2)、第32周单休(8/3-8/9)。
        // 全区间工作日 = 7/24 + 7/25(单休周六) + 7/27~7/31 + 8/3 + 8/4 = 9 天；7 月部分 7 天、8 月部分 2 天。
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(Arrays.asList(
                buildSetting(30L, 2026, 30, 1, 0, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 26)),
                buildSetting(31L, 2026, 31, 2, 0, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2)),
                buildSetting(32L, 2026, 32, 1, 0, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9))
        ));
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        Assert.assertEquals(9, service.countWorkDays(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 8, 4)));
        Assert.assertEquals(7, service.countWorkDays(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 31)));
        Assert.assertEquals(2, service.countWorkDays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4)));
    }

    @Test
    public void countWorkDays_shouldDefaultToMondayFridayWithoutWeekSettings_andHonorManualOverride() {
        // 年度单双休未初始化：默认周一至周五上班，但已保存日级设置仍生效（7/25 手动设为上班）。
        when(repository.findAllBySettingYearOrderByWeekNoAsc(2026)).thenReturn(Collections.emptyList());
        when(legalHolidaysRepository.findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(daySettingRepository.findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(any(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(buildDaySetting(LocalDate.of(2026, 7, 25), 1)));

        // 7/24(五)~7/31(五)：默认 6 个工作日 + 手动上班的周六 7/25 = 7
        Assert.assertEquals(7, service.countWorkDays(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 31)));
    }

    private HrmWorkweekSetting buildSetting(Long settingId,
                                            Integer year,
                                            Integer weekNo,
                                            Integer weekType,
                                            Integer manualOverride,
                                            LocalDate weekStartDate,
                                            LocalDate weekEndDate) {
        HrmWorkweekSetting row = new HrmWorkweekSetting();
        row.setSettingId(settingId);
        row.setSettingYear(year);
        row.setWeekNo(weekNo);
        row.setWeekType(weekType);
        row.setManualOverride(manualOverride);
        row.setRestDayText(weekType == 1 ? "周日" : "周六、周日");
        row.setWeekStartDate(toDate(weekStartDate));
        row.setWeekEndDate(toDate(weekEndDate));
        return row;
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private HrmAttendanceLegalHolidays buildLegalHoliday(LocalDate date, Integer type) {
        HrmAttendanceLegalHolidays holiday = new HrmAttendanceLegalHolidays();
        holiday.setHolidayTime(toDate(date));
        holiday.setType(type);
        return holiday;
    }

    private HrmWorkweekDaySetting buildDaySetting(LocalDate date, Integer dayType) {
        HrmWorkweekDaySetting daySetting = new HrmWorkweekDaySetting();
        daySetting.setSettingYear(date.getYear());
        daySetting.setSettingMonth(date.getMonthValue());
        daySetting.setWorkDate(toDate(date));
        daySetting.setDayType(dayType);
        return daySetting;
    }
}
