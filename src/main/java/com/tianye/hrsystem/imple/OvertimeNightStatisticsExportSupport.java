package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.entity.vo.DailyOvertimeNightDetailVO;
import com.tianye.hrsystem.entity.vo.EmployeeOvertimeNightMonthlyDetailVO;
import com.tianye.hrsystem.entity.vo.QueryOvertimeNightStatisticsPageVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class OvertimeNightStatisticsExportSupport {

    private OvertimeNightStatisticsExportSupport() {
    }

    public static String buildFileName(String month) {
        String normalizedMonth = normalizeMonth(month);
        return "加班夜班统计_" + normalizedMonth + ".xlsx";
    }

    public static List<List<String>> buildSummaryHead() {
        return Arrays.asList(
                Collections.singletonList("姓名"),
                Collections.singletonList("工号"),
                Collections.singletonList("部门"),
                Collections.singletonList("统计月份"),
                Collections.singletonList("应出勤天数"),
                Collections.singletonList("实际出勤天数"),
                Collections.singletonList("加班小时"),
                Collections.singletonList("夜班次数")
        );
    }

    public static List<List<String>> buildDailyDetailHead() {
        return Arrays.asList(
                Collections.singletonList("姓名"),
                Collections.singletonList("工号"),
                Collections.singletonList("部门"),
                Collections.singletonList("统计月份"),
                Collections.singletonList("日期"),
                Collections.singletonList("加班小时"),
                Collections.singletonList("夜班次数")
        );
    }

    public static List<List<String>> buildSummaryRows(List<QueryOvertimeNightStatisticsPageVO> rows) {
        List<List<String>> exportRows = new ArrayList<>();
        for (QueryOvertimeNightStatisticsPageVO row : rows != null ? rows : Collections.<QueryOvertimeNightStatisticsPageVO>emptyList()) {
            exportRows.add(Arrays.asList(
                    safe(row.getEmployeeName()),
                    safe(row.getJobNumber()),
                    safe(row.getDeptName()),
                    normalizeMonth(row.getMonth()),
                    String.valueOf(row.getExpectedAttendanceDays() == null ? 0 : row.getExpectedAttendanceDays()),
                    String.valueOf(row.getActualAttendanceDays() == null ? 0 : row.getActualAttendanceDays()),
                    formatDecimal(row.getOvertimeHours()),
                    String.valueOf(row.getNightShiftCount() == null ? 0 : row.getNightShiftCount())
            ));
        }
        return exportRows;
    }

    public static List<List<String>> buildDailyDetailRows(QueryOvertimeNightStatisticsPageVO summaryRow, EmployeeOvertimeNightMonthlyDetailVO detail) {
        List<List<String>> exportRows = new ArrayList<>();
        if (summaryRow == null || detail == null) {
            return exportRows;
        }
        for (DailyOvertimeNightDetailVO dailyDetail : detail.getDailyDetails() != null ? detail.getDailyDetails() : Collections.<DailyOvertimeNightDetailVO>emptyList()) {
            exportRows.add(Arrays.asList(
                    safe(summaryRow.getEmployeeName()),
                    safe(summaryRow.getJobNumber()),
                    safe(summaryRow.getDeptName()),
                    normalizeMonth(detail.getMonth()),
                    safe(dailyDetail.getWorkDate()),
                    formatDecimal(dailyDetail.getOvertimeHours()),
                    String.valueOf(dailyDetail.getNightShiftCount() == null ? 0 : dailyDetail.getNightShiftCount())
            ));
        }
        return exportRows;
    }

    private static String formatDecimal(BigDecimal value) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return normalized.toPlainString();
    }

    private static String normalizeMonth(String month) {
        if (month == null) {
            return "";
        }
        String text = month.trim();
        if (text.matches("\\d{4}-\\d{1,2}")) {
            String[] parts = text.split("-");
            return parts[0] + "-" + String.format("%02d", Integer.parseInt(parts[1]));
        }
        return text;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
