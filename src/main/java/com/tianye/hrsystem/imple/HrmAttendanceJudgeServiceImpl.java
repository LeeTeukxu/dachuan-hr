package com.tianye.hrsystem.imple;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianye.hrsystem.common.EmployeeNotInDingTalkException;
import com.tianye.hrsystem.entity.po.HrmAttendanceRule;
import com.tianye.hrsystem.mapper.HrmAttendanceRuleMapper;
import com.tianye.hrsystem.model.HrmAttendanceJudgeResult;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendancedetail;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.model.tbattendanceapprove;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.hrmAttendanceJudgeResultRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendancedetailRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.service.IHrmAttendanceJudgeService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本地考勤判定引擎实现。算法（与业务方确认的口径）：
 * 1. 应出勤基准 = tbplanlist 当天生效班次；rest=不应出勤；夜班（customShiftPeriod=night 或 customCrossDay）下班时刻在次日；
 *    标准班按 hrm_attendance_shift 的 1~3 段起止逐段判定；自定义班单段。
 * 2. 有效打卡集 = tbattendancedetail（empId+workDate）落在 [A-窗口提前, B+窗口延后] 的记录；
 *    补卡记录视为对应时段有效卡；外勤卡视为有效卡。
 * 3. 迟到 = 最早上班卡 > A+豁免分钟（规则 lateMinutesOrCounts）；早退 = 最晚下班卡 < B-豁免分钟（earlyMinutesOrCounts）；
 *    上/下班窗口内无有效卡 = 缺卡（迟到与缺卡互斥）；旷工 = 应出勤日全天无有效打卡且无补卡。
 * 4. 宽松取卡口径：上班取窗口内最早、下班取窗口内最晚。
 */
@Service
public class HrmAttendanceJudgeServiceImpl implements IHrmAttendanceJudgeService {

    private static final Logger logger = LoggerFactory.getLogger(HrmAttendanceJudgeServiceImpl.class);
    private static final String SHIFT_TYPE_STANDARD = "standard";
    private static final String SHIFT_TYPE_CUSTOM = "custom";
    private static final String SHIFT_TYPE_REST = "rest";
    private static final int DEFAULT_WINDOW_BEFORE_MINUTES = 120;
    private static final int DEFAULT_WINDOW_AFTER_MINUTES = 240;
    private static final int DEFAULT_MAX_MONTHLY_CARD_REPAIR = 3;
    private static final int SECTION_ON_DUTY = 1;
    private static final int SECTION_OFF_DUTY = 2;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private tbPlanListRepository planListRepository;

    @Autowired
    private tbattendancedetailRepository detailRepository;

    @Autowired
    private hrmAttendanceShiftRepository shiftRepository;

    @Autowired
    private hrmAttendanceJudgeResultRepository judgeResultRepository;

    @Autowired
    private tbattendanceapproveRepository approveRepository;

    @Autowired
    private HrmAttendanceRuleMapper attendanceRuleMapper;

    @Override
    public int recompute(Date begin, Date end, List<Long> employeeIds) throws Exception {
        Date startDay = startOfDay(begin);
        Date endDay = endOfDay(end);
        List<HrmEmployee> employees = employeeRepository.findAllByIsDelAndEntryStatusIn(0, Arrays.asList(1, 3, 4));
        if (employees == null || employees.isEmpty()) {
            return 0;
        }
        List<tbplanlist> plans = planListRepository.findAllByWorkDateBetweenOrderByIdDesc(startDay, endDay);
        List<tbattendancedetail> punches = detailRepository.findAllByWorkDateBetween(startDay, endDay);

        Map<Long, HrmEmployee> employeeById = new HashMap<>();
        for (HrmEmployee employee : employees) {
            if (employee != null && employee.getEmployeeId() != null) {
                employeeById.put(employee.getEmployeeId(), employee);
            }
        }
        // 双键 → 员工ID：行内 userId 可能是 dingtalk_user_id 或历史 employeeId 字符串
        Map<String, Long> employeeIdByPlanKey = new HashMap<>();
        for (HrmEmployee employee : employeeById.values()) {
            String dingTalkKey = StringUtils.trimToEmpty(employee.getDingtalkUserId());
            if (!dingTalkKey.isEmpty()) {
                employeeIdByPlanKey.putIfAbsent(dingTalkKey, employee.getEmployeeId());
            }
            employeeIdByPlanKey.putIfAbsent(String.valueOf(employee.getEmployeeId()), employee.getEmployeeId());
        }

        Map<String, List<tbplanlist>> plansByEmployeeDay = new HashMap<>();
        if (plans != null) {
            for (tbplanlist plan : plans) {
                if (plan == null || plan.getWorkDate() == null || StringUtils.isBlank(plan.getUserId())) {
                    continue;
                }
                for (String key : splitUserIds(plan.getUserId())) {
                    Long employeeId = employeeIdByPlanKey.get(key);
                    if (employeeId == null) {
                        continue;
                    }
                    plansByEmployeeDay.computeIfAbsent(employeeId + "|" + dayKey(plan.getWorkDate()),
                            k -> new ArrayList<>()).add(plan);
                }
            }
        }
        Map<String, List<tbattendancedetail>> punchesByEmployeeDay = new HashMap<>();
        if (punches != null) {
            for (tbattendancedetail detail : punches) {
                if (detail == null || detail.getEmpId() == null || detail.getWorkDate() == null) {
                    continue;
                }
                punchesByEmployeeDay.computeIfAbsent(detail.getEmpId() + "|" + dayKey(detail.getWorkDate()),
                        k -> new ArrayList<>()).add(detail);
            }
        }

        HrmAttendanceRule rule = loadDefaultRule();
        int beforeMinutes = rule == null || rule.getJudgeWindowBeforeMinutes() == null
                ? DEFAULT_WINDOW_BEFORE_MINUTES : rule.getJudgeWindowBeforeMinutes();
        int afterMinutes = rule == null || rule.getJudgeWindowAfterMinutes() == null
                ? DEFAULT_WINDOW_AFTER_MINUTES : rule.getJudgeWindowAfterMinutes();
        int lateThresholdMinutes = rule == null || rule.getLateMinutesOrCounts() == null
                ? 0 : rule.getLateMinutesOrCounts();
        int earlyThresholdMinutes = rule == null || rule.getEarlyMinutesOrCounts() == null
                ? 0 : rule.getEarlyMinutesOrCounts();

        // 补卡来自审批数据（tagName=补卡）：按月统计每人补卡单，超过考勤规则限额（max_monthly_card_repair）的
        // 补卡单（按 createTime 最早的优先）不参与判定，即其对应日期的缺卡不消除
        int monthlyRepairLimit = rule == null || rule.getMaxMonthlyCardRepair() == null
                ? DEFAULT_MAX_MONTHLY_CARD_REPAIR : rule.getMaxMonthlyCardRepair();
        Set<String> effectiveRepairDays = loadEffectiveRepairDays(employeeById.values(),
                startDay, endDay, monthlyRepairLimit);

        Calendar day = Calendar.getInstance();
        day.setTime(startDay);
        int written = 0;
        while (!day.getTime().after(endDay)) {
            Date workDate = day.getTime();
            for (HrmEmployee employee : employeeById.values()) {
                String bucket = employee.getEmployeeId() + "|" + dayKey(workDate);
                List<tbplanlist> dayPlans = plansByEmployeeDay.get(bucket);
                List<tbattendancedetail> dayPunches = punchesByEmployeeDay.get(bucket);
                if ((dayPlans == null || dayPlans.isEmpty())
                        && (dayPunches == null || dayPunches.isEmpty())) {
                    continue;
                }
                try {
                    boolean repaired = effectiveRepairDays.contains(
                            employee.getEmployeeId() + "|" + dayKey(workDate));
                    judgeOneDay(employee, workDate, dayPlans, dayPunches == null ? new ArrayList<>() : dayPunches,
                            rule, beforeMinutes, afterMinutes, lateThresholdMinutes, earlyThresholdMinutes, repaired);
                    written++;
                } catch (Exception ex) {
                    logger.warn("[本地考勤判定] 员工{} {} 判定失败", employee.getEmployeeId(), dayKey(workDate), ex);
                }
            }
            day.add(Calendar.DATE, 1);
        }
        logger.info("[本地考勤判定] 区间 {}~{} 判定完成，写库 {} 行", dayKey(startDay), dayKey(endDay), written);
        return written;
    }

    @Override
    public List<Map<String, Object>> queryResults(Date begin, Date end, List<Long> employeeIds) throws Exception {
        List<HrmAttendanceJudgeResult> rows =
                judgeResultRepository.findByWorkDateBetweenOrderByWorkDateAscEmpIdAsc(startOfDay(begin), endOfDay(end));
        Set<Long> wanted = employeeIds == null || employeeIds.isEmpty() ? null : new HashSet<>(employeeIds);
        List<Map<String, Object>> result = new ArrayList<>();
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (HrmAttendanceJudgeResult row : rows) {
            if (wanted != null && !wanted.contains(row.getEmpId())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("empId", row.getEmpId());
            item.put("workDate", dateFormat.format(row.getWorkDate()));
            item.put("shouldAttend", row.getShouldAttend());
            item.put("shiftType", row.getShiftType());
            item.put("shiftStart", row.getShiftStart());
            item.put("shiftEnd", row.getShiftEnd());
            item.put("crossDay", row.getCrossDay());
            item.put("firstPunchTime", row.getFirstPunchTime() == null ? "" : datetimeFormat.format(row.getFirstPunchTime()));
            item.put("lastPunchTime", row.getLastPunchTime() == null ? "" : datetimeFormat.format(row.getLastPunchTime()));
            item.put("lateMinutes", row.getLateMinutes());
            item.put("earlyMinutes", row.getEarlyMinutes());
            item.put("missCardCount", row.getMissCardCount());
            item.put("absenteeism", row.getAbsenteeism());
            item.put("restDayWork", row.getRestDayWork());
            result.add(item);
        }
        return result;
    }

    private void judgeOneDay(HrmEmployee employee, Date workDate, List<tbplanlist> dayPlans,
                             List<tbattendancedetail> dayPunches, HrmAttendanceRule rule,
                             int beforeMinutes, int afterMinutes,
                             int lateThresholdMinutes, int earlyThresholdMinutes, boolean dayRepaired) throws Exception {
        HrmAttendanceJudgeResult result = judgeResultRepository
                .findByEmpIdAndWorkDate(employee.getEmployeeId(), startOfDay(workDate))
                .orElseGet(HrmAttendanceJudgeResult::new);
        result.setEmpId(employee.getEmployeeId());
        result.setWorkDate(startOfDay(workDate));
        result.setRuleId(rule == null ? null : rule.getAttendanceRuleId());
        result.setJudgeTime(new Date());

        if (dayPlans == null || dayPlans.isEmpty()) {
            // 无排班：不判定出勤问题，仅当有打卡时记录休息日出勤
            result.setShouldAttend(false);
            result.setShiftType(SHIFT_TYPE_REST);
            result.setShiftStart("");
            result.setShiftEnd("");
            result.setCrossDay(false);
            result.setFirstPunchTime(earliestPunch(dayPunches));
            result.setLastPunchTime(latestPunch(dayPunches));
            result.setLateMinutes(0);
            result.setEarlyMinutes(0);
            result.setMissCardCount(0);
            result.setAbsenteeism(false);
            result.setRestDayWork(!dayPunches.isEmpty());
            judgeResultRepository.save(result);
            return;
        }

        tbplanlist primaryPlan = dayPlans.get(0);
        String shiftType = StringUtils.trimToEmpty(primaryPlan.getShiftType()).toLowerCase();
        result.setShiftType(shiftType);
        result.setRestDayWork(false);

        if (SHIFT_TYPE_REST.equals(shiftType)) {
            result.setShouldAttend(false);
            result.setShiftStart("");
            result.setShiftEnd("");
            result.setCrossDay(false);
            result.setFirstPunchTime(earliestPunch(dayPunches));
            result.setLastPunchTime(latestPunch(dayPunches));
            result.setLateMinutes(0);
            result.setEarlyMinutes(0);
            result.setMissCardCount(0);
            result.setAbsenteeism(false);
            result.setRestDayWork(!dayPunches.isEmpty());
            judgeResultRepository.save(result);
            return;
        }

        result.setShouldAttend(true);
        List<long[]> sections = resolveShiftSections(primaryPlan, workDate); // epoch millis {A, B} per section
        int missCardCount = 0;
        boolean repairConsumed = false;
        int lateMinutes = 0;
        int earlyMinutes = 0;
        Date firstOnDuty = null;
        Date lastOffDuty = null;
        boolean anyValidPunch = !dayPunches.isEmpty() || dayRepaired;

        for (int index = 0; index < sections.size(); index++) {
            long sectionStart = sections.get(index)[0];
            long sectionEnd = sections.get(index)[1];
            long windowStart = sectionStart - beforeMinutes * 60_000L;
            long windowEnd = sectionEnd + afterMinutes * 60_000L;

            List<Date> onDutyTimes = new ArrayList<>();
            List<Date> offDutyTimes = new ArrayList<>();
            for (tbattendancedetail detail : dayPunches) {
                Date punchTime = detail.getUserCheckTime() != null ? detail.getUserCheckTime() : detail.getBaseCheckTime();
                if (punchTime == null) {
                    continue;
                }
                long time = punchTime.getTime();
                if (time < windowStart || time > windowEnd) {
                    continue;
                }
                String checkType = StringUtils.trimToEmpty(detail.getCheckType()).toUpperCase();
                if (checkType.contains("OFF")) {
                    offDutyTimes.add(punchTime);
                } else if (checkType.contains("ON") || checkType.isEmpty()) {
                    onDutyTimes.add(punchTime);
                }
            }
            // 该日有生效补卡单：视为补上一次缺卡；同日第二段缺卡无第二张单抵扣仍记录
            if (!onDutyTimes.isEmpty()) {
                Date earliest = earliest(onDutyTimes);
                if (firstOnDuty == null || earliest.before(firstOnDuty)) {
                    firstOnDuty = earliest;
                }
                long late = earliest.getTime() - (sectionStart + lateThresholdMinutes * 60_000L);
                if (late > 0) {
                    lateMinutes += (int) Math.ceil(late / 60_000.0);
                }
            } else if (dayRepaired && !repairConsumed) {
                repairConsumed = true;
            } else {
                missCardCount++;
            }

            if (!offDutyTimes.isEmpty()) {
                Date latest = latest(offDutyTimes);
                if (lastOffDuty == null || latest.after(lastOffDuty)) {
                    lastOffDuty = latest;
                }
                long early = (sectionEnd - earlyThresholdMinutes * 60_000L) - latest.getTime();
                if (early > 0) {
                    earlyMinutes += (int) Math.ceil(early / 60_000.0);
                }
            } else if (dayRepaired && !repairConsumed) {
                repairConsumed = true;
            } else {
                missCardCount++;
            }
        }

        result.setFirstPunchTime(firstOnDuty);
        result.setLastPunchTime(lastOffDuty);
        result.setLateMinutes(lateMinutes);
        result.setEarlyMinutes(earlyMinutes);
        result.setMissCardCount(missCardCount);
        result.setAbsenteeism(!anyValidPunch);
        judgeResultRepository.save(result);
    }

    /** 解析班次的上下班时段（毫秒时间戳 {A,B}），夜班/跨天班 B 落次日；标准班支持 1~3 段 */
    private List<long[]> resolveShiftSections(tbplanlist plan, Date workDate) throws Exception {
        List<long[]> sections = new ArrayList<>();
        String shiftType = StringUtils.trimToEmpty(plan.getShiftType()).toLowerCase();
        boolean night = "night".equals(StringUtils.trimToEmpty(plan.getCustomShiftPeriod()));
        if (SHIFT_TYPE_CUSTOM.equals(shiftType)) {
            Date start = timeOnDay(workDate, plan.getCustomStart());
            Date end = timeOnDay(workDate, plan.getCustomEnd());
            boolean crossDay = Boolean.TRUE.equals(plan.getCustomCrossDay()) || night
                    || (start != null && end != null && !end.after(start));
            if (start != null && end != null) {
                if (crossDay) {
                    end = addDays(end, 1);
                }
                sections.add(new long[]{start.getTime(), end.getTime()});
            }
            return sections;
        }
        // 标准班：classId 查班次字典，1~3 段
        HrmAttendanceShift shift = null;
        if (StringUtils.isNotBlank(plan.getClassId())) {
            try {
                shift = shiftRepository.findById(Long.parseLong(plan.getClassId().trim())).orElse(null);
            } catch (NumberFormatException ignored) {
                // classId 非数字（如自定义班遗留值），按无班次处理
            }
        }
        if (shift == null) {
            // 找不到班次字典：退化为全天窗口，仅判旷工与缺卡
            Date start = startOfDay(workDate);
            sections.add(new long[]{start.getTime(), addDays(start, 1).getTime() - 1});
            return sections;
        }
        addSection(sections, workDate, shift.getStart1(), shift.getEnd1());
        addSection(sections, workDate, shift.getStart2(), shift.getEnd2());
        addSection(sections, workDate, shift.getStart3(), shift.getEnd3());
        if (sections.isEmpty()) {
            Date start = startOfDay(workDate);
            sections.add(new long[]{start.getTime(), addDays(start, 1).getTime() - 1});
        }
        return sections;
    }

    private void addSection(List<long[]> sections, Date workDate, String startText, String endText) {
        if (StringUtils.isBlank(startText) || StringUtils.isBlank(endText)) {
            return;
        }
        try {
            Date start = timeOnDay(workDate, trimToHHmm(startText));
            Date end = timeOnDay(workDate, trimToHHmm(endText));
            if (start == null || end == null) {
                return;
            }
            if (!end.after(start)) {
                end = addDays(end, 1);
            }
            sections.add(new long[]{start.getTime(), end.getTime()});
        } catch (Exception ex) {
            logger.warn("[本地考勤判定] 班次时段解析失败: {}~{}", startText, endText, ex);
        }
    }

    private String trimToHHmm(String text) {
        String value = StringUtils.trimToEmpty(text);
        String[] parts = value.split(":");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1];
        }
        return value;
    }

    private HrmAttendanceRule loadDefaultRule() {
        List<HrmAttendanceRule> rules = attendanceRuleMapper.selectList(
                new QueryWrapper<HrmAttendanceRule>().eq("is_default_setting", 1).last("limit 1"));
        return rules == null || rules.isEmpty() ? null : rules.get(0);
    }

    /**
     * 从审批数据统计生效补卡：每人每月按 createTime 最早的前 N 张（N=考勤规则 max_monthly_card_repair），
     * 超限的补卡单不参与判定。返回 "empId|yyyy-MM-dd" 集合。
     */
    private Set<String> loadEffectiveRepairDays(java.util.Collection<HrmEmployee> employees,
                                                Date begin, Date end, int monthlyLimit) {
        Set<String> result = new HashSet<>();
        if (employees == null || employees.isEmpty() || monthlyLimit <= 0) {
            return result;
        }
        Map<String, Long> employeeIdByDingTalkId = new HashMap<>();
        List<String> dingTalkUserIds = new ArrayList<>();
        for (HrmEmployee employee : employees) {
            String dingTalkId = StringUtils.trimToEmpty(employee.getDingtalkUserId());
            if (!dingTalkId.isEmpty()) {
                employeeIdByDingTalkId.put(dingTalkId, employee.getEmployeeId());
                dingTalkUserIds.add(dingTalkId);
            }
        }
        if (dingTalkUserIds.isEmpty()) {
            return result;
        }
        Calendar monthStartCal = Calendar.getInstance();
        monthStartCal.setTime(startOfDay(begin));
        monthStartCal.set(Calendar.DAY_OF_MONTH, 1);
        Calendar monthEndCal = Calendar.getInstance();
        monthEndCal.setTime(endOfDay(end));
        monthEndCal.set(Calendar.DAY_OF_MONTH, 1);
        monthEndCal.add(Calendar.MONTH, 1);
        monthEndCal.add(Calendar.DATE, -1);
        List<tbattendanceapprove> approvals = approveRepository.findAllByUserIdInAndWorkDateBetween(
                dingTalkUserIds, monthStartCal.getTime(), monthEndCal.getTime());
        if (approvals == null || approvals.isEmpty()) {
            return result;
        }
        Map<Long, List<tbattendanceapprove>> byEmployee = new HashMap<>();
        for (tbattendanceapprove approval : approvals) {
            if (approval == null || !StringUtils.trimToEmpty(approval.getTagName()).contains("补卡")) {
                continue;
            }
            Long employeeId = employeeIdByDingTalkId.get(StringUtils.trimToEmpty(approval.getUserId()));
            if (employeeId == null || approval.getWorkDate() == null) {
                continue;
            }
            byEmployee.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(approval);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
        for (Map.Entry<Long, List<tbattendanceapprove>> entry : byEmployee.entrySet()) {
            List<tbattendanceapprove> list = entry.getValue();
            list.sort((a, b) -> Long.compare(
                    a.getCreateTime() == null ? 0L : a.getCreateTime().getTime(),
                    b.getCreateTime() == null ? 0L : b.getCreateTime().getTime()));
            Map<String, Integer> monthlyCount = new HashMap<>();
            for (tbattendanceapprove approval : list) {
                String monthKey = monthFormat.format(approval.getWorkDate());
                int used = monthlyCount.getOrDefault(monthKey, 0);
                if (used >= monthlyLimit) {
                    continue;
                }
                monthlyCount.put(monthKey, used + 1);
                result.add(entry.getKey() + "|" + dateFormat.format(approval.getWorkDate()));
            }
        }
        return result;
    }

    private Date earliestPunch(List<tbattendancedetail> punches) {
        List<Date> times = new ArrayList<>();
        for (tbattendancedetail detail : punches) {
            Date time = detail.getUserCheckTime() != null ? detail.getUserCheckTime() : detail.getBaseCheckTime();
            if (time != null) {
                times.add(time);
            }
        }
        return earliest(times);
    }

    private Date latestPunch(List<tbattendancedetail> punches) {
        List<Date> times = new ArrayList<>();
        for (tbattendancedetail detail : punches) {
            Date time = detail.getUserCheckTime() != null ? detail.getUserCheckTime() : detail.getBaseCheckTime();
            if (time != null) {
                times.add(time);
            }
        }
        return latest(times);
    }

    private Date earliest(List<Date> times) {
        Date result = null;
        for (Date time : times) {
            if (time != null && (result == null || time.before(result))) {
                result = time;
            }
        }
        return result;
    }

    private Date latest(List<Date> times) {
        Date result = null;
        for (Date time : times) {
            if (time != null && (result == null || time.after(result))) {
                result = time;
            }
        }
        return result;
    }

    private Date timeOnDay(Date workDate, String hhmm) throws Exception {
        if (StringUtils.isBlank(hhmm)) {
            return null;
        }
        String[] parts = trimToHHmm(hhmm).split(":");
        if (parts.length < 2) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay(workDate));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0].trim()));
        calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1].trim()));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date endOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay(date));
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTime();
    }

    private Date startOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay(date));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private String dayKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private List<String> splitUserIds(String userId) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(userId)) {
            return result;
        }
        for (String part : userId.split(",")) {
            if (StringUtils.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
