package com.tianye.hrsystem.imple.ddTalk;

import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetattcolumnsRequest;
import com.dingtalk.api.request.OapiAttendanceGetcolumnvalRequest;
import com.dingtalk.api.request.OapiAttendanceGetleavetimebynamesRequest;
import com.dingtalk.api.response.OapiAttendanceGetattcolumnsResponse;
import com.dingtalk.api.response.OapiAttendanceGetcolumnvalResponse;
import com.dingtalk.api.response.OapiAttendanceGetleavetimebynamesResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.model.ddTalk.Ddtaskresult;
import com.tianye.hrsystem.repository.*;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmAttendanceReportManager
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月20日 9:23
 **/
@Service
public class HrmAttendanceReportManager implements IHrmAttendanceReport {

    @Autowired
    IAccessToken tokener;
    @Autowired
    hrmAttendanceReportFieldRepository fieldRep;
    @Autowired
    hrmAttendanceReportDataRepository dataRep;
    @Autowired
    hrmAttendanceJudgeResultRepository judgeResultRepository;
    @Autowired
    hrmEmployeeRepository employeeRepository;
    @Autowired
    tbattendanceapproveRepository approveRepository;
    @Autowired
    hrmAttendanceClockRepository clockRepository;
    @Autowired
    com.tianye.hrsystem.modules.workweek.service.HrmWorkweekSettingService workweekService;
    @Autowired
    hrmEmployeeLeaveRecordRepository leaveRep;
    @Autowired
    hrmEmployeeOverTimeRecordRepository overTimeRep;
    Long PreNum = 19000000L;
    private static final ThreadLocal<SimpleDateFormat> SIMPLE =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private static final ThreadLocal<SimpleDateFormat> SHORT_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    List<String> overTimes = Arrays.asList("工作日加班", "休息日加班", "节假日加班");
    /** 打卡类型：1 上班打卡 2 下班打卡 */
    private static final int PUNCH_TYPE_ON = 1;
    private static final int PUNCH_TYPE_OFF = 2;

    Logger logger = LoggerFactory.getLogger(HrmAttendanceReportManager.class);
    @Autowired
    DDTalkResposeLogger ddLogger;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Override
    public List<HrmAttendanceReportField> UpdateReportFields() throws ApiException {
        // 先调用API获取数据（在事务外执行，避免长事务）
        String password = tokener.Refresh();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getattcolumns");
        OapiAttendanceGetattcolumnsRequest req = new OapiAttendanceGetattcolumnsRequest();
        OapiAttendanceGetattcolumnsResponse rsp = client.execute(req, password);
        
        if (rsp.isSuccess()) {
            // 先在内存中准备好数据
            List<HrmAttendanceReportField> fields = new ArrayList<>();
            List<OapiAttendanceGetattcolumnsResponse.ColumnForTopVo> columns = rsp.getResult().getColumns();
            for (int i = 0; i < columns.size(); i++) {
                OapiAttendanceGetattcolumnsResponse.ColumnForTopVo vo = columns.get(i);
                Long Id = vo.getId();
                Integer Type = 1;//
                if (Id == null) {
                    Type = 2;
                    Id = PreNum;
                    PreNum++;
                }
                String name = vo.getName();
                HrmAttendanceReportField newOne = new HrmAttendanceReportField();
                newOne.setFieldId(Id);
                newOne.setFieldName(name);
                newOne.setCreateTime(new Date());
                newOne.setType(Type);
                fields.add(newOne);
            }
            
            // 在短事务中执行数据库操作
            final List<HrmAttendanceReportField> fieldsToSave = fields;
            transactionTemplate.execute(status -> {
                fieldRep.deleteAll();
                if (!fieldsToSave.isEmpty()) {
                    fieldRep.saveAll(fieldsToSave);
                }
                return null;
            });
            logger.info("更新考勤报表字段完成，共 {} 个字段", fields.size());
        }
        return null;
    }
    @Override
    @Transactional
    public void UpdateAttendanceReport(String userId, Long empId, Date begin, Date end) throws ApiException {
        // 本地计算，不再调用钉钉 getcolumnval（降配额；原逻辑存 ddtaskresult 待解析，现已直接落 report_data）
        generateLocalReportData(empId, begin, end);
    }

    @Override
    @Transactional
    public void UpdateAttendanceReportQuick(String userId, Long empId, Date begin, Date end) throws ApiException {
        // 本地计算，不再调用钉钉 getcolumnval（降配额）
        generateLocalReportData(empId, begin, end);
    }

    @Autowired
    ddtaskresultRepository ddRep;
    private static final ThreadLocal<SimpleDateFormat> CIMPLE =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));
    @Override
    @Transactional
    public void UpdateHolidayReport(String userId, Long empId, Date begin, Date end) throws ApiException {
        // 本地计算，不再调用钉钉 getleavetimebynames（降配额）
        generateLocalHolidayData(empId, begin, end);
    }

    @Override
    @Transactional
    public void UpdateHolidayReportQuick(String userId, Long empId, Date begin, Date end) throws ApiException {
        // 本地计算，不再调用钉钉 getleavetimebynames（降配额）
        generateLocalHolidayData(empId, begin, end);
    }

    /**
     * 本地生成 type=2 请假报表数据（替代钉钉 getleavetimebynames）。
     * 数据源：本地审批 tbattendanceapprove（tagName=请假，subType=假别）。
     * 单位复刻钉钉：按天请的假记"天"（durationDay），按小时请的记"小时"（duration）。
     * 跨天假按区间内"工作日"均摊到每一天，保证月报按天累加的总量不重复、不放大。
     */
    private void generateLocalHolidayData(Long empId, Date begin, Date end) {
        List<HrmAttendanceReportField> allFields = fieldRep.findAll();
        List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 2).collect(Collectors.toList());
        if (fields.isEmpty()) {
            return;
        }
        List<Long> fieldIds = fields.stream().map(HrmAttendanceReportField::getFieldId).collect(Collectors.toList());
        dataRep.deleteAllByEmpIdAndWorkDateBetweenAndFieldIdIn(empId, begin, end, fieldIds);

        Map<Date, Map<String, Double>> leaveByDay = loadLeaveByDay(empId, begin, end);
        List<HrmAttendanceReportData> datas = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfDay(begin));
        Date endDay = endOfDay(end);
        while (!cal.getTime().after(endDay)) {
            Date day = cal.getTime();
            Map<String, Double> dayValues = leaveByDay.getOrDefault(day, Collections.emptyMap());
            for (HrmAttendanceReportField field : fields) {
                HrmAttendanceReportData d = new HrmAttendanceReportData();
                d.setEmpId(empId);
                Double v = dayValues.get(field.getFieldName());
                d.setValue(v == null ? "0" : formatHours(v));
                d.setFieldId(field.getFieldId());
                d.setFieldName(field.getFieldName());
                d.setWorkDate(day);
                d.setCreateTime(new Date());
                d.setCreateUser(1L);
                datas.add(d);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (!datas.isEmpty()) {
            dataRep.saveAll(datas);
        }
        logger.info("[本地报表] 员工{} {}-{} 生成 type=2 请假数据 {} 条（已去钉钉 getleavetimebynames）",
                empId, SHORT_FORMAT.get().format(begin), SHORT_FORMAT.get().format(end), datas.size());
    }

    /** 请假数据按日分组：假别(subType) → 当日值（天或小时，按钉钉单位复刻） */
    private Map<Date, Map<String, Double>> loadLeaveByDay(Long empId, Date begin, Date end) {
        Map<Date, Map<String, Double>> map = new HashMap<>();
        if (empId == null) {
            return map;
        }
        Optional<HrmEmployee> emp = employeeRepository.findById(empId);
        String dingUserId = emp.isPresent() ? emp.get().getDingtalkUserId() : null;
        if (StringUtils.isBlank(dingUserId)) {
            logger.warn("[本地报表] 员工{} 未绑定钉钉 userId，请假报表列置 0", empId);
            return map;
        }
        List<tbattendanceapprove> list = approveRepository.findAllByUserIdInAndWorkDateBetween(
                Collections.singletonList(dingUserId), startOfDay(begin), endOfDay(end));
        if (list == null) {
            return map;
        }
        for (tbattendanceapprove a : list) {
            if (!"请假".equals(a.getTagName()) || StringUtils.isBlank(a.getSubType())) {
                continue;
            }
            Date from = a.getBeginTime() != null ? a.getBeginTime() : a.getWorkDate();
            if (from == null) {
                continue;
            }
            Date to = a.getEndTime() != null ? a.getEndTime() : from;
            if (to.before(from)) {
                to = from;
            }
            // 单位复刻钉钉：按天请的假 → 天数；按小时请的 → 小时数
            boolean byDay = isWholeDayLeave(a);
            double total = byDay ? parseNum(a.getDurationDay()) : parseNum(a.getDuration());
            if (total <= 0d) {
                continue;
            }
            List<Date> days = workdaysBetween(from, to);
            if (days.isEmpty()) {
                days = allDaysBetween(from, to);
            }
            if (days.isEmpty()) {
                continue;
            }
            double per = total / days.size();
            for (Date d : days) {
                map.computeIfAbsent(startOfDay(d), k -> new HashMap<>())
                        .merge(a.getSubType(), per, Double::sum);
            }
        }
        return map;
    }

    /**
     * 是否"按天请假"：durationDay × 2 为整数（半天/全天/1.5天…）即为按天填写，
     * 与审批解析口径 HrmAttendanceApprovalProcessInstanceParser 一致。
     */
    private static boolean isWholeDayLeave(tbattendanceapprove a) {
        BigDecimal dd = toBigDecimal(a.getDurationDay());
        if (dd == null) {
            return false;
        }
        return dd.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() <= 0;
    }

    /** 区间内的工作日（无工作日时返回空列表，由调用方回落到全部日历日） */
    private List<Date> workdaysBetween(Date from, Date to) {
        List<Date> days = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(from));
        Date last = startOfDay(to);
        while (!c.getTime().after(last)) {
            Date d = c.getTime();
            if (workweekService.countWorkDays(toLocalDate(d), toLocalDate(d)) > 0) {
                days.add(d);
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private static List<Date> allDaysBetween(Date from, Date to) {
        List<Date> days = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTime(startOfDay(from));
        Date last = startOfDay(to);
        while (!c.getTime().after(last)) {
            days.add(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private static LocalDate toLocalDate(Date d) {
        return new java.sql.Date(d.getTime()).toLocalDate();
    }

    private static double parseNum(String v) {
        BigDecimal bd = toBigDecimal(v);
        return bd == null ? 0d : bd.doubleValue();
    }

    private static BigDecimal toBigDecimal(String v) {
        if (StringUtils.isBlank(v)) {
            return null;
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 本地生成 type=1 考勤报表数据（替代钉钉 getcolumnval）。
     * 数据源：hrm_attendance_judge_result（每人每日一行，含出勤工时/迟到/早退/缺卡/旷工/休息日出勤等）。
     * 按员工+区间遍历每日，按字段名(fieldName)映射本地值落 hrm_attendance_report_data；
     * 写入前先删除该员工区间旧的 type=1 数据做幂等。事务由 public 调用方保证。
     */
    private void generateLocalReportData(Long empId, Date begin, Date end) {
        List<HrmAttendanceReportField> allFields = fieldRep.findAll();
        List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 1).collect(Collectors.toList());
        if (fields.isEmpty()) {
            return;
        }
        List<HrmAttendanceJudgeResult> judgeRows =
                judgeResultRepository.findByEmpIdAndWorkDateBetweenOrderByWorkDateAsc(empId, begin, end);
        Map<Date, HrmAttendanceJudgeResult> judgeByDay = new LinkedHashMap<>();
        for (HrmAttendanceJudgeResult j : judgeRows) {
            if (j != null && j.getWorkDate() != null) {
                judgeByDay.put(startOfDay(j.getWorkDate()), j);
            }
        }
        List<Long> fieldIds = fields.stream().map(HrmAttendanceReportField::getFieldId).collect(Collectors.toList());
        dataRep.deleteAllByEmpIdAndWorkDateBetweenAndFieldIdIn(empId, begin, end, fieldIds);

        // 预加载本地数据源：打卡流水（多段打卡）、钉钉审批（补卡/出差/外出/关联审批单）、加班记录
        Map<Date, List<HrmAttendanceClock>> clockByDay = loadClockByDay(empId, begin, end);
        Map<Date, List<tbattendanceapprove>> approveByDay = loadApproveByDay(empId, begin, end);
        Map<Date, List<HrmEmployeeOverTimeRecord>> overtimeByDay = loadOvertimeByDay(empId, begin, end);

        List<HrmAttendanceReportData> datas = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfDay(begin));
        Date endDay = endOfDay(end);
        while (!cal.getTime().after(endDay)) {
            Date day = cal.getTime();
            HrmAttendanceJudgeResult j = judgeByDay.get(day);
            DayContext ctx = new DayContext(day,
                    clockByDay.getOrDefault(day, Collections.emptyList()),
                    approveByDay.getOrDefault(day, Collections.emptyList()),
                    overtimeByDay.getOrDefault(day, Collections.emptyList()));
            for (HrmAttendanceReportField field : fields) {
                HrmAttendanceReportData d = new HrmAttendanceReportData();
                d.setEmpId(empId);
                d.setValue(resolveLocalColumnValue(field.getFieldName(), j, ctx));
                d.setFieldId(field.getFieldId());
                d.setFieldName(field.getFieldName());
                d.setWorkDate(day);
                d.setCreateTime(new Date());
                d.setCreateUser(1L);
                datas.add(d);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (!datas.isEmpty()) {
            dataRep.saveAll(datas);
        }
        logger.info("[本地报表] 员工{} {}-{} 生成 type=1 数据 {} 条（已去钉钉 getcolumnval）",
                empId, SHORT_FORMAT.get().format(begin), SHORT_FORMAT.get().format(end), datas.size());
    }

    /** 按字段名映射本地值（日级，对齐钉钉列）。返回空串表示该列本地暂未覆盖。 */
    private String resolveLocalColumnValue(String fieldName, HrmAttendanceJudgeResult j, DayContext ctx) {
        // 打卡流水 / 审批 / 加班类字段不依赖本地判定结果，即使当天无判定行也能算
        switch (fieldName) {
            case "上班1打卡时间":
            case "上班2打卡时间":
            case "上班3打卡时间":
                return ctx.punchTimeText(punchStage(fieldName), PUNCH_TYPE_ON, true);
            case "下班1打卡时间":
            case "下班2打卡时间":
            case "下班3打卡时间":
                return ctx.punchTimeText(punchStage(fieldName), PUNCH_TYPE_OFF, false);
            case "上班1打卡结果":
            case "上班2打卡结果":
            case "上班3打卡结果":
                return ctx.punchStatusText(punchStage(fieldName), PUNCH_TYPE_ON, true);
            case "下班1打卡结果":
            case "下班2打卡结果":
            case "下班3打卡结果":
                return ctx.punchStatusText(punchStage(fieldName), PUNCH_TYPE_OFF, false);
            case "补卡次数":
                return String.valueOf(ctx.countApprove("补卡"));
            case "出差时长":
                return ctx.approveHoursText("出差");
            case "外出时长":
                return ctx.approveHoursText("外出");
            case "加班-审批单统计":
                return String.valueOf(ctx.countApprove("加班"));
            case "工作日加班":
                return ctx.overtimeHoursText(1);
            case "休息日加班":
                return ctx.overtimeHoursText(2);
            case "节假日加班":
                return ctx.overtimeHoursText(3);
            case "加班总时长":
                return ctx.overtimeHoursText(null);
            case "关联的审批单":
                return ctx.relatedApproveText();
            case "工作日（转加班费）":
            case "休息日（转加班费）":
            case "节假日（转加班费）":
            case "工作日（转调休）":
            case "休息日（转调休）":
            case "节假日（转调休）":
                return ""; // 本地无"转加班费/转调休"来源，保持留空
            default:
                break;
        }
        if (j == null) {
            return "";
        }
        boolean shouldAttend = Boolean.TRUE.equals(j.getShouldAttend());
        switch (fieldName) {
            case "工作时长":
                return j.getWorkHours() == null ? "" : j.getWorkHours().toPlainString();
            case "出勤天数":
                return (shouldAttend && j.getFirstPunchTime() != null) ? "1" : "0";
            case "应出勤天数":
                return shouldAttend ? "1" : "0";
            case "休息天数":
                return "rest".equals(j.getShiftType()) ? "1" : "0";
            case "迟到时长":
                return String.valueOf(j.getLateMinutes() == null ? 0 : j.getLateMinutes());
            case "迟到次数":
                return (j.getLateMinutes() != null && j.getLateMinutes() > 0) ? "1" : "0";
            case "早退时长":
                return String.valueOf(j.getEarlyMinutes() == null ? 0 : j.getEarlyMinutes());
            case "早退次数":
                return (j.getEarlyMinutes() != null && j.getEarlyMinutes() > 0) ? "1" : "0";
            case "上班缺卡次数":
                return String.valueOf(j.getMissCardOnCount() == null
                        ? (j.getMissCardCount() == null ? 0 : j.getMissCardCount()) : j.getMissCardOnCount());
            case "下班缺卡次数":
                return String.valueOf(j.getMissCardOffCount() == null ? 0 : j.getMissCardOffCount());
            case "旷工天数":
                return Boolean.TRUE.equals(j.getAbsenteeism()) ? "1" : "0";
            case "严重迟到次数":
            case "严重迟到时长":
            case "旷工迟到次数":
            case "旷工迟到天数":
                return "0";
            case "休息日出勤":
                return Boolean.TRUE.equals(j.getRestDayWork()) ? "1" : "0";
            case "出勤班次":
            case "班次":
                return j.getShiftType() == null ? "" : j.getShiftType();
            case "考勤结果":
                return buildAttendanceResultText(j);
            default:
                return "";
        }
    }

    private String buildAttendanceResultText(HrmAttendanceJudgeResult j) {
        if (Boolean.TRUE.equals(j.getAbsenteeism())) {
            return "旷工";
        }
        List<String> parts = new ArrayList<>();
        if (j.getLateMinutes() != null && j.getLateMinutes() > 0) {
            parts.add("迟到" + j.getLateMinutes() + "分");
        }
        if (j.getEarlyMinutes() != null && j.getEarlyMinutes() > 0) {
            parts.add("早退" + j.getEarlyMinutes() + "分");
        }
        if (j.getMissCardCount() != null && j.getMissCardCount() > 0) {
            parts.add("缺卡" + j.getMissCardCount() + "次");
        }
        if (parts.isEmpty()) {
            return Boolean.TRUE.equals(j.getShouldAttend()) ? "正常" : "休息";
        }
        return String.join("、", parts);
    }

    /** 从"上班1打卡时间"这类字段名里取段号（1/2/3） */
    private static int punchStage(String fieldName) {
        for (char ch : fieldName.toCharArray()) {
            if (ch >= '1' && ch <= '3') {
                return ch - '0';
            }
        }
        return 1;
    }

    /** 打卡流水按日分组（多段打卡来源：clock_stage + clock_type） */
    private Map<Date, List<HrmAttendanceClock>> loadClockByDay(Long empId, Date begin, Date end) {
        Map<Date, List<HrmAttendanceClock>> map = new HashMap<>();
        List<HrmAttendanceClock> clocks =
                clockRepository.findAllByClockEmployeeIdAndClockTimeBetween(empId, startOfDay(begin), endOfDay(end));
        if (clocks == null) {
            return map;
        }
        for (HrmAttendanceClock c : clocks) {
            Date base = c.getWorkDate() != null ? c.getWorkDate() : c.getClockTime();
            if (base == null) {
                continue;
            }
            map.computeIfAbsent(startOfDay(base), k -> new ArrayList<>()).add(c);
        }
        return map;
    }

    /** 钉钉审批按日分组（补卡/出差/外出/关联审批单）。审批存的是钉钉 userId，需先从员工档案取。 */
    private Map<Date, List<tbattendanceapprove>> loadApproveByDay(Long empId, Date begin, Date end) {
        Map<Date, List<tbattendanceapprove>> map = new HashMap<>();
        if (empId == null) {
            return map;
        }
        Optional<HrmEmployee> emp = employeeRepository.findById(empId);
        String dingUserId = emp.isPresent() ? emp.get().getDingtalkUserId() : null;
        if (StringUtils.isBlank(dingUserId)) {
            logger.warn("[本地报表] 员工{} 未绑定钉钉 userId，审批类报表列留空", empId);
            return map;
        }
        List<tbattendanceapprove> list = approveRepository.findAllByUserIdInAndWorkDateBetween(
                Collections.singletonList(dingUserId), startOfDay(begin), endOfDay(end));
        if (list == null) {
            return map;
        }
        for (tbattendanceapprove a : list) {
            Date from = a.getBeginTime() != null ? a.getBeginTime() : a.getWorkDate();
            if (from == null) {
                continue;
            }
            Date to = a.getEndTime() != null ? a.getEndTime() : from;
            if (to.before(from)) {
                to = from;
            }
            // 跨天审批（如多日出差）在其覆盖的每一天都登记，便于按日折算时长
            Calendar c = Calendar.getInstance();
            c.setTime(startOfDay(from));
            Date last = startOfDay(to);
            while (!c.getTime().after(last)) {
                map.computeIfAbsent(c.getTime(), k -> new ArrayList<>()).add(a);
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        return map;
    }

    /** 加班记录按日分组（over_time_type：1工作日 2休息日 3节假日） */
    private Map<Date, List<HrmEmployeeOverTimeRecord>> loadOvertimeByDay(Long empId, Date begin, Date end) {
        Map<Date, List<HrmEmployeeOverTimeRecord>> map = new HashMap<>();
        List<HrmEmployeeOverTimeRecord> list =
                overTimeRep.findAllByEmployeeIdAndAttendanceTimeBetween(empId, startOfDay(begin), endOfDay(end));
        if (list == null) {
            return map;
        }
        for (HrmEmployeeOverTimeRecord r : list) {
            Date base = r.getAttendanceTime() != null ? r.getAttendanceTime() : r.getOverTimeStartTime();
            if (base == null) {
                continue;
            }
            map.computeIfAbsent(startOfDay(base), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    /** 某一天的本地数据上下文：打卡流水 + 审批 + 加班记录 */
    private static class DayContext {
        private final Date day;
        private final List<HrmAttendanceClock> clocks;
        private final List<tbattendanceapprove> approvals;
        private final List<HrmEmployeeOverTimeRecord> overtimes;

        DayContext(Date day, List<HrmAttendanceClock> clocks,
                   List<tbattendanceapprove> approvals, List<HrmEmployeeOverTimeRecord> overtimes) {
            this.day = day;
            this.clocks = clocks == null ? Collections.emptyList() : clocks;
            this.approvals = approvals == null ? Collections.emptyList() : approvals;
            this.overtimes = overtimes == null ? Collections.emptyList() : overtimes;
        }

        /** 取指定段/类型的一条打卡记录：上班取最早，下班取最晚 */
        private HrmAttendanceClock punch(int stage, int type, boolean earliest) {
            HrmAttendanceClock picked = null;
            for (HrmAttendanceClock c : clocks) {
                if (c.getClockTime() == null) {
                    continue;
                }
                int st = c.getClockStage() == null ? 1 : c.getClockStage();
                Integer ct = c.getClockType();
                if (st != stage || ct == null || ct.intValue() != type) {
                    continue;
                }
                if (picked == null
                        || (earliest ? c.getClockTime().before(picked.getClockTime())
                                     : c.getClockTime().after(picked.getClockTime()))) {
                    picked = c;
                }
            }
            return picked;
        }

        String punchTimeText(int stage, int type, boolean earliest) {
            HrmAttendanceClock c = punch(stage, type, earliest);
            return c == null ? "" : SIMPLE.get().format(c.getClockTime());
        }

        /** 打卡状态：0正常 1迟到 2早退 3旷工迟到 4加班 5未打卡 */
        String punchStatusText(int stage, int type, boolean earliest) {
            HrmAttendanceClock c = punch(stage, type, earliest);
            if (c == null || c.getClockStatus() == null) {
                return "";
            }
            switch (c.getClockStatus()) {
                case 0: return "正常";
                case 1: return "迟到";
                case 2: return "早退";
                case 3: return "旷工迟到";
                case 4: return "加班";
                case 5: return "未打卡";
                default: return "";
            }
        }

        /** 指定 tagName 的审批单条数（如补卡、加班） */
        long countApprove(String tagName) {
            long n = 0;
            for (tbattendanceapprove a : approvals) {
                if (tagName.equals(a.getTagName())) {
                    n++;
                }
            }
            return n;
        }

        /** 指定 tagName 的审批在该日折算的时长（小时）：取审批区间与当天的交集 */
        String approveHoursText(String tagName) {
            double hours = 0d;
            Date dayStart = startOfDay(day);
            Date dayEnd = endOfDay(day);
            for (tbattendanceapprove a : approvals) {
                if (!tagName.equals(a.getTagName())) {
                    continue;
                }
                hours += overlapHours(a.getBeginTime(), a.getEndTime(), dayStart, dayEnd);
            }
            return formatHours(hours);
        }

        /** 加班时长（小时）。type 为 null 表示全部类型合计 */
        String overtimeHoursText(Integer type) {
            double hours = 0d;
            for (HrmEmployeeOverTimeRecord r : overtimes) {
                if (r.getOverTimes() == null) {
                    continue;
                }
                if (type != null && (r.getOverTimeType() == null || r.getOverTimeType().intValue() != type.intValue())) {
                    continue;
                }
                hours += r.getOverTimes();
            }
            return formatHours(hours);
        }

        /** 当天关联的审批单名称，如"请假-调休,补卡" */
        String relatedApproveText() {
            List<String> names = new ArrayList<>();
            for (tbattendanceapprove a : approvals) {
                if (a.getTagName() == null) {
                    continue;
                }
                String name = StringUtils.isBlank(a.getSubType()) ? a.getTagName() : a.getTagName() + "-" + a.getSubType();
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
            return String.join(",", names);
        }
    }

    /** 两段时间的交集小时数 */
    private static double overlapHours(Date begin, Date end, Date rangeStart, Date rangeEnd) {
        if (begin == null) {
            return 0d;
        }
        Date to = end == null ? begin : end;
        long from = Math.max(begin.getTime(), rangeStart.getTime());
        long until = Math.min(to.getTime(), rangeEnd.getTime());
        if (until <= from) {
            return 0d;
        }
        return (until - from) / 3600000d;
    }

    private static String formatHours(double hours) {
        if (hours <= 0d) {
            return "0";
        }
        BigDecimal bd = BigDecimal.valueOf(hours).setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.stripTrailingZeros().toPlainString();
    }

    private static Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date endOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    void SaveHolidayData(Long empId,OapiAttendanceGetleavetimebynamesResponse  rsp) throws  ApiException{
        if(rsp.isSuccess()){
            List<HrmAttendanceReportField> allFields = fieldRep.findAll();
            List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 2).collect(Collectors.toList());
            List<Long> idd=fields.stream().map(f->f.getFieldId()).collect(Collectors.toList());
            OapiAttendanceGetleavetimebynamesResponse.ColumnValListForTopVo V = rsp.getResult();
            List<OapiAttendanceGetleavetimebynamesResponse.ColumnValForTopVo> values = V.getColumns();
            List<HrmAttendanceReportData> Datas = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                OapiAttendanceGetleavetimebynamesResponse.ColumnValForTopVo value = values.get(i);
                List<OapiAttendanceGetleavetimebynamesResponse.ColumnDayAndVal> vs = value.getColumnvals();
                if (vs.size() > 0) {
                    OapiAttendanceGetleavetimebynamesResponse.ColumnForTopVo col = value.getColumnvo();
                    String fieldName = col.getName();
                    Optional<HrmAttendanceReportField> findFields =
                            fields.stream().filter(f -> f.getFieldName().equals(fieldName)).findFirst();
                    if (findFields.isPresent()) {
                        HrmAttendanceReportField ss = findFields.get();
                        for (int n = 0; n < vs.size(); n++) {
                            OapiAttendanceGetleavetimebynamesResponse.ColumnDayAndVal cVal = vs.get(n);

                            HrmAttendanceReportField field = findFields.get();
                            String fieldValue = cVal.getValue();
                            HrmAttendanceReportData sData = new HrmAttendanceReportData();
                            sData.setEmpId(empId);
                            sData.setValue(fieldValue);
                            sData.setFieldId(ss.getFieldId());
                            sData.setFieldName(field.getFieldName());
                            sData.setWorkDate(cVal.getDate());
                            sData.setCreateTime(new Date());
                            sData.setCreateUser(1L);
                            Datas.add(sData);
                        }
                    } else {
                        logger.info(fieldName + "没有找到对应的ID");
                        break;
                    }
                }
            }
            if (Datas.size() > 0) {
                dataRep.saveAll(Datas);
                logger.info("一共保存了" + Integer.toString(Datas.size()) + "条请假报表数据！");
            }
        }
    }
    void SaveReportData(Long empId, OapiAttendanceGetcolumnvalResponse rsp) throws ApiException{
        if (rsp.isSuccess()) {
            List<HrmAttendanceReportField> allFields = fieldRep.findAll();
            List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 1).collect(Collectors.toList());


            OapiAttendanceGetcolumnvalResponse.ColumnValListForTopVo V = rsp.getResult();
            List<OapiAttendanceGetcolumnvalResponse.ColumnValForTopVo> values = V.getColumnVals();
            List<HrmAttendanceReportData> Datas = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                OapiAttendanceGetcolumnvalResponse.ColumnValForTopVo value = values.get(i);
                List<OapiAttendanceGetcolumnvalResponse.ColumnDayAndVal> vs = value.getColumnVals();
                if (vs.size() > 0) {
                    OapiAttendanceGetcolumnvalResponse.ColumnForTopVo col = value.getColumnVo();
                    Long fieldId = col.getId();
                    Optional<HrmAttendanceReportField> findFields =
                            fields.stream().filter(f -> f.getFieldId().equals(fieldId)).findFirst();
                    if (findFields.isPresent()) {
                        for (int n = 0; n < vs.size(); n++) {
                            OapiAttendanceGetcolumnvalResponse.ColumnDayAndVal cVal = vs.get(n);
                            HrmAttendanceReportField field = findFields.get();
                            String fieldValue = cVal.getValue();
                            HrmAttendanceReportData sData = new HrmAttendanceReportData();
                            sData.setEmpId(empId);
                            sData.setValue(fieldValue);
                            sData.setFieldId(fieldId);
                            sData.setFieldName(field.getFieldName());
                            sData.setWorkDate(cVal.getDate());
                            sData.setCreateTime(new Date());
                            sData.setCreateUser(1L);
                            Datas.add(sData);
                        }
                    }
                }
            }
            if (Datas.size() > 0) {
                dataRep.saveAll(Datas);
                logger.info("一共保存了" + Long.toString(empId) + "的" + Integer.toString(Datas.size())+"条考勤及加班报表数据！");
            }
        }
    }

    @Transactional
    public void ProcessOne(String CompanyID) {
        Optional<Ddtaskresult> findOnes=ddRep.findFirstByProcessedAndCompanyIdOrderByCreatetime(0,CompanyID);
        if(findOnes.isPresent()){
            Ddtaskresult one=findOnes.get();
            Long T1=System.currentTimeMillis();
            try {
                String className=one.getClassName();
                String Content=one.getContent();
                Long empId=one.getEmpId();
                if(StringUtils.isEmpty(Content)==false){
                    if(className.equals("UpdateHolidayReport")){
                        OapiAttendanceGetleavetimebynamesResponse rsp=JSON.parseObject(Content,OapiAttendanceGetleavetimebynamesResponse.class);
                        SaveHolidayData(empId,rsp);
                    }else if(className.equals("UpdateAttendanceReport") || className.equals("UpdateAttendanceRepot")){
                        OapiAttendanceGetcolumnvalResponse rsp=JSON.parseObject(Content,OapiAttendanceGetcolumnvalResponse.class);
                        SaveReportData(empId,rsp);
                    }
                    one.setResult("处理完成!");
                    one.setSuccess(1);
                    one.setProcessed(200);
                } else throw new Exception("Content is empty!");
            }
            catch(Exception ax){
                one.setProcessed(500);
                one.setSuccess(0);
                one.setResult("处理出错:"+ax.getMessage());
            }
            finally {
                one.setProcesstime(new Date());
            }
            try {
                ddRep.save(one);
            }
            catch(Exception ax){
                ax.printStackTrace();
            }
            finally {
                Long T2=System.currentTimeMillis();
                logger.info("数据处理完成，用时:"+Long.toString(T2-T1));
            }
        } else {
            logger.info("CompanyID:"+CompanyID+"在DDTaskResult中未找到待处理的记录!");
        }
    }
}
