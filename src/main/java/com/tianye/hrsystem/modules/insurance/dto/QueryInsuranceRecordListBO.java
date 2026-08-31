package com.tianye.hrsystem.modules.insurance.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class QueryInsuranceRecordListBO extends MyPageEntity {
    @ApiModelProperty("年份")
    private Integer year;

    @ApiModelProperty("月份范围，格式 yyyy-MM")
    private List<String> times;

    @ApiModelProperty("部门ID列表")
    private List<Long> deptIds;

    @ApiModelProperty("员工ID列表")
    private List<Long> employeeIds;

    @ApiModelProperty("开始年月，格式 yyyyMM")
    private Integer startPeriod;

    @ApiModelProperty("结束年月，格式 yyyyMM")
    private Integer endPeriod;

    public void normalizeFilters() {
        normalizeTimeRange();
        normalizeScopes();
    }

    private void normalizeTimeRange() {
        if (times != null && times.size() >= 2) {
            startPeriod = parseYearMonth(times.get(0));
            endPeriod = parseYearMonth(times.get(1));
        }
        if (startPeriod == null && endPeriod == null && year != null) {
            startPeriod = year * 100 + 1;
            endPeriod = year * 100 + 12;
        }
        if (startPeriod == null && endPeriod == null) {
            int currentYear = LocalDate.now().getYear();
            startPeriod = currentYear * 100 + 1;
            endPeriod = currentYear * 100 + 12;
        }
        if (startPeriod != null && endPeriod != null && startPeriod > endPeriod) {
            Integer temp = startPeriod;
            startPeriod = endPeriod;
            endPeriod = temp;
        }
    }

    private Integer parseYearMonth(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        if (text.length() >= 7) {
            String[] parts = text.substring(0, 7).split("-");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
            }
        }
        return Integer.parseInt(text.replace("-", ""));
    }

    private void normalizeScopes() {
        deptIds = normalizeIds(deptIds);
        employeeIds = normalizeIds(employeeIds);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "QueryInsuranceRecordListBO{" +
                "year=" + year +
                ", times=" + times +
                ", deptIds=" + deptIds +
                ", employeeIds=" + employeeIds +
                ", startPeriod=" + startPeriod +
                ", endPeriod=" + endPeriod +
                '}';
    }
}
