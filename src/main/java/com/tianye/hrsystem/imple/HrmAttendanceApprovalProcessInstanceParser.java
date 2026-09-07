package com.tianye.hrsystem.imple;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.response.OapiProcessinstanceGetResponse;
import com.tianye.hrsystem.model.tbattendanceapprove;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HrmAttendanceApprovalProcessInstanceParser {

    private static final Long BIZ_TYPE_OVERTIME = 1L;
    private static final Long BIZ_TYPE_TRAVEL = 2L;
    private static final Long BIZ_TYPE_LEAVE = 3L;

    /** 请假时长口径：1 个工作日 = 8 工作小时（与 PC 前端/考勤统计换算一致） */
    private static final java.math.BigDecimal LEAVE_HOURS_PER_DAY = java.math.BigDecimal.valueOf(8);

    private static final List<String> LEAVE_KEYWORDS = Arrays.asList(
            "请假", "事假", "调休", "病假", "婚假", "丧假", "产假", "陪产假", "年假", "补休"
    );
    private static final List<String> DATE_PATTERNS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
    );
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(
            "\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\s+\\d{2}:\\d{2}(?::\\d{2})?"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}"
    );

    public Optional<tbattendanceapprove> parse(String processInstanceId,
                                               OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (processInstanceId == null || processInstanceId.trim().isEmpty() || processInstance == null) {
            return Optional.empty();
        }
        String title = normalizeText(processInstance.getTitle());
        String typeLabel = resolveTypeLabel(title, processInstance.getFormComponentValues());
        if (typeLabel == null) {
            return Optional.empty();
        }

        DurationValue durationValue = resolveDuration(typeLabel, processInstance.getFormComponentValues());
        DateRange dateRange = resolveDateRange(processInstance, typeLabel, durationValue);
        tbattendanceapprove entity = new tbattendanceapprove();
        entity.setId(processInstanceId);
        entity.setUserId(processInstance.getOriginatorUserid());
        entity.setTagName(typeLabel);
        entity.setBizType(resolveBizType(typeLabel));
        entity.setSubType(resolveSubType(typeLabel, processInstance.getFormComponentValues()));
        entity.setBeginTime(dateRange.beginTime);
        entity.setEndTime(dateRange.endTime);
        entity.setWorkDate(dateRange.beginTime != null ? dateRange.beginTime : processInstance.getCreateTime());
        entity.setCreateTime(new Date());

        entity.setDuration(durationValue.value);
        entity.setDurationUnit(durationValue.unit);
        entity.setDurationDay(toDayText(durationValue.value, durationValue.unit));
        return Optional.of(entity);
    }

    private String resolveTypeLabel(String title,
                                    List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        if (containsKeyword(title, "补卡")) {
            return "补卡";
        }
        if (containsKeyword(title, "加班")) {
            return "加班";
        }
        if (containsKeyword(title, "出差", "外出")) {
            return containsKeyword(title, "外出") ? "外出" : "出差";
        }
        if (containsAnyKeyword(title, LEAVE_KEYWORDS)) {
            return "请假";
        }
        String leaveSubType = resolveLeaveSubType(components);
        if (!leaveSubType.isEmpty()) {
            return "请假";
        }
        String componentNames = joinComponentNames(components);
        if (containsKeyword(componentNames, "补卡")) {
            return "补卡";
        }
        if (containsKeyword(componentNames, "加班")) {
            return "加班";
        }
        if (containsKeyword(componentNames, "出差", "外出")) {
            return containsKeyword(componentNames, "外出") ? "外出" : "出差";
        }
        if (containsAnyKeyword(componentNames, LEAVE_KEYWORDS)) {
            return "请假";
        }
        return null;
    }

    private Long resolveBizType(String typeLabel) {
        if ("加班".equals(typeLabel)) {
            return BIZ_TYPE_OVERTIME;
        }
        if ("请假".equals(typeLabel)) {
            return BIZ_TYPE_LEAVE;
        }
        if ("出差".equals(typeLabel) || "外出".equals(typeLabel)) {
            return BIZ_TYPE_TRAVEL;
        }
        return null;
    }

    private String resolveSubType(String typeLabel,
                                  List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        if ("请假".equals(typeLabel)) {
            return resolveLeaveSubType(components);
        }
        if ("加班".equals(typeLabel)) {
            return findComponentValue(components, "加班类型", "加班类别", "类型");
        }
        return "";
    }

    private String resolveLeaveSubType(List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        String value = findComponentValue(components, "请假类型", "假期类型", "请假类别", "假别");
        if (!normalizeText(value).isEmpty()) {
            return value;
        }
        String structuredValue = resolveStructuredLeaveSubType(components);
        if (!structuredValue.isEmpty()) {
            return structuredValue;
        }
        return "";
    }

    private String resolveStructuredLeaveSubType(List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        if (components == null || components.isEmpty()) {
            return "";
        }
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            if (component == null) {
                continue;
            }
            String extValueSubtype = resolveLeaveSubTypeFromExtValue(component);
            if (!extValueSubtype.isEmpty()) {
                return extValueSubtype;
            }
            String componentName = normalizeText(component.getName());
            String componentValue = normalizeText(component.getValue());
            if (containsKeyword(componentName, "请假", "假期", "假别")
                    || containsKeyword(componentValue, "请假类型", "假期类型", "请假类别", "假别")) {
                String subtype = findLeaveKeyword(componentValue);
                if (!subtype.isEmpty()) {
                    return subtype;
                }
            }
        }
        return "";
    }

    private String resolveLeaveSubTypeFromExtValue(OapiProcessinstanceGetResponse.FormComponentValueVo component) {
        JSONObject extValue = parseComponentExtValue(component);
        if (extValue == null) {
            return "";
        }
        String subtype = findLeaveKeyword(extValue.getString("tag"));
        if (!subtype.isEmpty()) {
            return subtype;
        }
        String extension = extValue.getString("extension");
        if (extension != null && !extension.trim().isEmpty()) {
            try {
                JSONObject extensionObject = JSON.parseObject(extension);
                subtype = findLeaveKeyword(extensionObject.getString("tag"));
                if (!subtype.isEmpty()) {
                    return subtype;
                }
            } catch (RuntimeException ignored) {
                subtype = findLeaveKeyword(extension);
                if (!subtype.isEmpty()) {
                    return subtype;
                }
            }
        }
        return "";
    }

    private String findLeaveKeyword(String value) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue.isEmpty()) {
            return "";
        }
        for (String keyword : LEAVE_KEYWORDS) {
            if ("请假".equals(keyword)) {
                continue;
            }
            if (containsKeyword(normalizedValue, keyword)) {
                return keyword;
            }
        }
        return "";
    }

    private static final List<String> LEAVE_BEGIN_COMPONENT_NAMES = Arrays.asList(
            "请假开始时间", "请假开始日期", "请假时间", "假期开始时间", "假期开始日期", "假期时间"
    );
    private static final List<String> LEAVE_END_COMPONENT_NAMES = Arrays.asList(
            "请假结束时间", "请假结束日期", "请假时间", "假期结束时间", "假期结束日期", "假期时间"
    );

    private DateRange resolveDateRange(OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance,
                                       String typeLabel,
                                       DurationValue durationValue) {
        List<OapiProcessinstanceGetResponse.FormComponentValueVo> components = processInstance.getFormComponentValues();
        if ("请假".equals(typeLabel)) {
            DateRange leaveRange = resolveLeaveDateRange(components, processInstance);
            if (leaveRange != null) {
                return leaveRange;
            }
        }
        OapiProcessinstanceGetResponse.FormComponentValueVo beginComponent = findComponent(
                components,
                "开始时间", "开始日期", "起始时间", "加班日期", "加班开始时间", "加班开始日期",
                "出差开始时间", "外出开始时间", "补卡时间"
        );
        OapiProcessinstanceGetResponse.FormComponentValueVo endComponent = findComponent(
                components,
                "结束时间", "结束日期", "截止时间", "加班结束时间", "加班结束日期",
                "出差结束时间", "外出结束时间", "补卡时间"
        );
        List<Date> beginCandidates = extractComponentDates(beginComponent);
        List<Date> endCandidates = extractComponentDates(endComponent);
        Date beginTime = firstDate(beginCandidates);
        Date endTime = lastDate(endCandidates);
        if (endTime == null && beginCandidates.size() >= 2) {
            endTime = lastDate(beginCandidates);
        }
        if (beginTime == null && endCandidates.size() >= 2) {
            beginTime = firstDate(endCandidates);
        }
        if (beginTime == null) {
            beginTime = processInstance.getCreateTime();
        }
        if (endTime == null) {
            endTime = processInstance.getFinishTime() != null ? processInstance.getFinishTime() : beginTime;
        }
        DateRange dateRange = new DateRange(beginTime, endTime);
        if ("加班".equals(typeLabel)) {
            return repairOvertimeDateRange(dateRange, durationValue);
        }
        return dateRange;
    }

    private DurationValue resolveDuration(String typeLabel,
                                          List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        DurationValue complexDuration = resolveComplexLeaveDuration(typeLabel, components);
        if (complexDuration != null) {
            return complexDuration;
        }
        String raw = findComponentValue(components, "时长", "请假时长", "加班时长", "预计加班时长", "外出时长", "出差时长");
        String normalized = normalizeText(raw);
        if (normalized.isEmpty()) {
            return new DurationValue("", "");
        }
        String unit = "";
        if (normalized.contains("小时")) {
            unit = "小时";
            normalized = normalized.replace("小时", "");
        } else if (normalized.contains("天")) {
            unit = "天";
            normalized = normalized.replace("天", "");
        } else if (normalized.contains("分钟")) {
            unit = "分钟";
            normalized = normalized.replace("分钟", "");
        }
        if (unit.isEmpty() && "加班".equals(typeLabel) && isNumeric(normalized)) {
            unit = "小时";
        }
        return new DurationValue(normalized, unit);
    }

    private DurationValue resolveComplexLeaveDuration(String typeLabel,
                                                      List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        if (!"请假".equals(typeLabel) || components == null || components.isEmpty()) {
            return null;
        }
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            JSONObject extValue = parseComponentExtValue(component);
            if (extValue == null) {
                continue;
            }
            String durationInDay = normalizeNumericText(extValue.getString("durationInDay"));
            String durationInHour = normalizeNumericText(extValue.getString("durationInHour"));
            java.math.BigDecimal dayValue = parsePositiveDecimal(durationInDay);
            java.math.BigDecimal hourValue = parsePositiveDecimal(durationInHour);

            // 钉钉请假控件口径：按"天"填写的请假 durationInDay 恒为 0.5 的整数倍（半天=0.5、全天=1、1.5 天等），
            // 是权威的"时长(天)"。而 durationInHour 对这类请假可能返回日历小时伪值
            // （如半天=下午固定 12 小时，而非工作小时 4h），与 durationInDay*8 不一致，不能直接信任。
            // 判定：durationInDay 是 0.5 的整数倍 ⇒ 按天填写 ⇒ 小时 = durationInDay * 8 落库；
            // 否则为按小时填写（如 30 分钟→durationInDay=0.07），以 durationInHour 为准。
            if (dayValue != null && isHalfDayGranular(dayValue)) {
                return new DurationValue(
                        dayValue.multiply(LEAVE_HOURS_PER_DAY).stripTrailingZeros().toPlainString(), "小时");
            }
            if (hourValue != null) {
                return new DurationValue(hourValue.stripTrailingZeros().toPlainString(), "小时");
            }
            if (dayValue != null) {
                return new DurationValue(
                        dayValue.multiply(LEAVE_HOURS_PER_DAY).stripTrailingZeros().toPlainString(), "小时");
            }
        }
        return null;
    }

    /**
     * 请假类（调休/年假等）业务日期区间解析。
     * 钉钉请假控件组件名通常是"请假时间/假期"等，不在原通用候选名单内，导致 beginTime/endTime
     * 兜底成发起时间/审批完成时间，跨月单（如 7/24~8/4）整段落发起月，次月列表查不到。
     * 解析优先级：①请假控件 ext_value 权威 start_time/end_time（毫秒）②请假类命名组件 value 内的日期
     * ③全部组件 value 日期扫描取 min/max（仅当扫到日期时）。
     */
    private DateRange resolveLeaveDateRange(List<OapiProcessinstanceGetResponse.FormComponentValueVo> components,
                                            OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstance) {
        if (components == null || components.isEmpty()) {
            return null;
        }
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            JSONObject extValue = parseComponentExtValue(component);
            if (extValue == null) {
                continue;
            }
            Date startTime = parseMillis(extValue.getString("start_time"));
            Date endTime = parseMillis(extValue.getString("end_time"));
            if (startTime != null && endTime != null) {
                return new DateRange(startTime, endTime);
            }
        }
        List<Date> beginDates = extractComponentDates(findComponent(components, LEAVE_BEGIN_COMPONENT_NAMES.toArray(new String[0])));
        List<Date> endDates = extractComponentDates(findComponent(components, LEAVE_END_COMPONENT_NAMES.toArray(new String[0])));
        Date beginTime = firstDate(beginDates);
        Date endTime = lastDate(endDates);
        if (endTime == null && beginDates.size() >= 2) {
            endTime = lastDate(beginDates);
        }
        if (beginTime == null && endDates.size() >= 2) {
            beginTime = firstDate(endDates);
        }
        if (beginTime != null && endTime != null) {
            return new DateRange(beginTime, endTime);
        }
        List<Date> allDates = new ArrayList<>();
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            allDates.addAll(extractComponentDates(component));
        }
        if (!allDates.isEmpty()) {
            return new DateRange(firstDate(allDates), lastDate(allDates));
        }
        return null;
    }

    private Date parseMillis(String value) {
        String normalized = normalizeNumericText(value);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            long millis = Long.parseLong(normalized);
            return millis > 0 ? new Date(millis) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isHalfDayGranular(java.math.BigDecimal dayValue) {
        return dayValue.multiply(java.math.BigDecimal.valueOf(2))
                .remainder(java.math.BigDecimal.ONE)
                .compareTo(java.math.BigDecimal.ZERO) == 0;
    }

    private java.math.BigDecimal parsePositiveDecimal(String value) {
        String normalized = normalizeNumericText(value);
        if (normalized.isEmpty()) {
            return null;
        }
        java.math.BigDecimal decimal = new java.math.BigDecimal(normalized);
        return decimal.compareTo(java.math.BigDecimal.ZERO) > 0 ? decimal : null;
    }

    private JSONObject parseComponentExtValue(OapiProcessinstanceGetResponse.FormComponentValueVo component) {
        if (component == null || component.getExtValue() == null || component.getExtValue().trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(component.getExtValue());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalizeNumericText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        try {
            new java.math.BigDecimal(normalized);
            return normalized;
        } catch (NumberFormatException ex) {
            return "";
        }
    }

    private OapiProcessinstanceGetResponse.FormComponentValueVo findComponent(
            List<OapiProcessinstanceGetResponse.FormComponentValueVo> components,
            String... names
    ) {
        if (components == null || components.isEmpty() || names == null || names.length == 0) {
            return null;
        }
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            String componentName = normalizeText(component == null ? null : component.getName());
            if (componentName.isEmpty()) {
                continue;
            }
            for (String name : names) {
                if (componentName.contains(normalizeText(name))) {
                    return component;
                }
            }
        }
        return null;
    }

    private String findComponentValue(List<OapiProcessinstanceGetResponse.FormComponentValueVo> components, String... names) {
        OapiProcessinstanceGetResponse.FormComponentValueVo component = findComponent(components, names);
        return extractComponentText(component);
    }

    private String joinComponentNames(List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        if (components == null || components.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            if (component != null && component.getName() != null) {
                builder.append(component.getName());
            }
        }
        return normalizeText(builder.toString());
    }

    private String joinComponentValues(List<OapiProcessinstanceGetResponse.FormComponentValueVo> components) {
        if (components == null || components.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (OapiProcessinstanceGetResponse.FormComponentValueVo component : components) {
            if (component != null && component.getValue() != null) {
                builder.append(component.getValue());
            }
        }
        return normalizeText(builder.toString());
    }

    private boolean containsAnyKeyword(String value, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (containsKeyword(value, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKeyword(String value, String... keywords) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue.isEmpty() || keywords == null || keywords.length == 0) {
            return false;
        }
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeText(keyword);
            if (!normalizedKeyword.isEmpty() && normalizedValue.contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private Date parseDate(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        for (String pattern : DATE_PATTERNS) {
            try {
                return new SimpleDateFormat(pattern, Locale.CHINA).parse(text);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private List<Date> extractComponentDates(OapiProcessinstanceGetResponse.FormComponentValueVo component) {
        List<Date> dates = new ArrayList<>();
        if (component == null) {
            return dates;
        }
        collectDates(component.getValue(), dates);
        collectDates(component.getExtValue(), dates);
        return dates;
    }

    private void collectDates(String raw, List<Date> dates) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return;
        }
        boolean matched = false;
        Matcher matcher = DATE_TIME_PATTERN.matcher(text);
        while (matcher.find()) {
            Date date = parseDate(matcher.group());
            if (date != null && !containsDate(dates, date)) {
                dates.add(date);
            }
            matched = true;
        }
        if (!matched) {
            Matcher dateMatcher = DATE_PATTERN.matcher(text);
            while (dateMatcher.find()) {
                Date date = parseDate(dateMatcher.group());
                if (date != null && !containsDate(dates, date)) {
                    dates.add(date);
                }
                matched = true;
            }
        }
        if (!matched) {
            Date date = parseDate(text);
            if (date != null && !containsDate(dates, date)) {
                dates.add(date);
            }
        }
    }

    private boolean containsDate(List<Date> dates, Date target) {
        if (dates == null || dates.isEmpty() || target == null) {
            return false;
        }
        for (Date date : dates) {
            if (date != null && date.getTime() == target.getTime()) {
                return true;
            }
        }
        return false;
    }

    private Date firstDate(List<Date> dates) {
        return dates == null || dates.isEmpty() ? null : dates.get(0);
    }

    private Date lastDate(List<Date> dates) {
        return dates == null || dates.isEmpty() ? null : dates.get(dates.size() - 1);
    }

    private DateRange repairOvertimeDateRange(DateRange dateRange, DurationValue durationValue) {
        if (dateRange == null || dateRange.endTime == null) {
            return dateRange;
        }
        Long durationMillis = toDurationMillis(durationValue);
        if (durationMillis == null || durationMillis <= 0L) {
            return dateRange;
        }
        Date beginTime = dateRange.beginTime;
        if (beginTime == null || beginTime.after(dateRange.endTime) || isOvertimeSpanObviouslyInconsistent(beginTime, dateRange.endTime, durationMillis)) {
            return new DateRange(new Date(dateRange.endTime.getTime() - durationMillis), dateRange.endTime);
        }
        return dateRange;
    }

    private boolean isOvertimeSpanObviouslyInconsistent(Date beginTime, Date endTime, long durationMillis) {
        if (beginTime == null || endTime == null) {
            return false;
        }
        long spanMillis = endTime.getTime() - beginTime.getTime();
        if (spanMillis < 0L) {
            return true;
        }
        long thresholdMillis = Math.max(durationMillis * 2L, durationMillis + 60L * 60L * 1000L);
        return spanMillis > thresholdMillis;
    }

    private Long toDurationMillis(DurationValue durationValue) {
        if (durationValue == null || !isNumeric(durationValue.value)) {
            return null;
        }
        double numericValue = Double.parseDouble(durationValue.value);
        if (numericValue <= 0D) {
            return null;
        }
        if ("天".equals(durationValue.unit)) {
            return Math.round(numericValue * 24D * 60D * 60D * 1000D);
        }
        if ("分钟".equals(durationValue.unit)) {
            return Math.round(numericValue * 60D * 1000D);
        }
        if ("小时".equals(durationValue.unit)) {
            return Math.round(numericValue * 60D * 60D * 1000D);
        }
        return null;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String extractComponentText(OapiProcessinstanceGetResponse.FormComponentValueVo component) {
        if (component == null) {
            return "";
        }
        String value = component.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        String extValue = component.getExtValue();
        return extValue == null ? "" : extValue.trim();
    }

    private String toDayText(String value, String unit) {
        if (value == null || !isNumeric(value)) {
            return "";
        }
        java.math.BigDecimal numeric;
        try {
            numeric = new java.math.BigDecimal(value);
        } catch (NumberFormatException ex) {
            return "";
        }
        if (numeric.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return "";
        }
        java.math.BigDecimal days;
        if ("天".equals(unit)) {
            days = numeric;
        } else if ("分钟".equals(unit)) {
            days = numeric.divide(java.math.BigDecimal.valueOf(60), 6, java.math.RoundingMode.HALF_UP)
                    .divide(LEAVE_HOURS_PER_DAY, 6, java.math.RoundingMode.HALF_UP);
        } else {
            // 默认按小时换算：天 = 小时 / 8
            days = numeric.divide(LEAVE_HOURS_PER_DAY, 6, java.math.RoundingMode.HALF_UP);
        }
        return days.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(" ", "").trim();
    }

    private static class DateRange {
        private final Date beginTime;
        private final Date endTime;

        private DateRange(Date beginTime, Date endTime) {
            this.beginTime = beginTime;
            this.endTime = endTime;
        }
    }

    private static class DurationValue {
        private final String value;
        private final String unit;

        private DurationValue(String value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }
}
