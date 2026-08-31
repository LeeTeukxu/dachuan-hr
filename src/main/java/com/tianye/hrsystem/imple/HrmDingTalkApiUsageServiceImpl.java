package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.entity.vo.DingTalkApiUsageDistributionVO;
import com.tianye.hrsystem.entity.vo.DingTalkApiUsageVO;
import com.tianye.hrsystem.model.Postresultlog;
import com.tianye.hrsystem.repository.postresultlogRepository;
import com.tianye.hrsystem.service.IHrmDingTalkApiUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HrmDingTalkApiUsageServiceImpl implements IHrmDingTalkApiUsageService {

    private static final long DEFAULT_MONTHLY_LIMIT = 10000L;
    private static final int DEFAULT_THRESHOLD_PERCENT = 80;

    @Autowired
    private postresultlogRepository logRepository;

    @Value("${dingtalk.api.monthly-limit:10000}")
    private long monthlyLimit = DEFAULT_MONTHLY_LIMIT;

    @Value("${dingtalk.api.usage-threshold-percent:80}")
    private int thresholdPercent = DEFAULT_THRESHOLD_PERCENT;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public DingTalkApiUsageVO queryMonthlyUsage() {
        YearMonth month = YearMonth.from(LocalDate.now(clock));
        ZoneId zoneId = clock.getZone();
        Date begin = Date.from(month.atDay(1).atStartOfDay(zoneId).toInstant());
        Date end = Date.from(month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant());
        List<Postresultlog> logs = logRepository.findAllByCreateTimeGreaterThanEqualAndCreateTimeLessThan(begin, end);
        return buildUsage(month, logs == null ? new ArrayList<Postresultlog>() : logs);
    }

    private DingTalkApiUsageVO buildUsage(YearMonth month, List<Postresultlog> logs) {
        long total = logs.size();
        long limit = monthlyLimit > 0 ? monthlyLimit : DEFAULT_MONTHLY_LIMIT;
        int threshold = thresholdPercent > 0 ? thresholdPercent : DEFAULT_THRESHOLD_PERCENT;
        double usagePercent = percentage(total, limit);
        boolean overThreshold = usagePercent >= threshold;

        DingTalkApiUsageVO result = new DingTalkApiUsageVO();
        result.setMonth(month.toString());
        result.setTotalCount(total);
        result.setMonthlyLimit(limit);
        result.setUsagePercent(round(usagePercent));
        result.setThresholdPercent(threshold);
        result.setOverThreshold(overThreshold);
        result.setLimitSource("application:dingtalk.api.monthly-limit");
        result.setDistributions(buildDistributions(logs, total));
        result.setDescription(buildDescription(total, limit, result.getUsagePercent(), threshold, overThreshold));
        return result;
    }

    private List<DingTalkApiUsageDistributionVO> buildDistributions(List<Postresultlog> logs, long total) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Postresultlog log : logs) {
            String featureName = classifyFeature(log);
            Long count = counts.get(featureName);
            counts.put(featureName, count == null ? 1L : count + 1L);
        }

        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Comparator
                .<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));

        List<DingTalkApiUsageDistributionVO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : entries) {
            result.add(new DingTalkApiUsageDistributionVO(
                    entry.getKey(),
                    entry.getValue(),
                    total > 0 ? round(entry.getValue() * 100D / total) : 0D));
        }
        return result;
    }

    private String classifyFeature(Postresultlog log) {
        String url = normalize(log == null ? null : log.getPostUrl());
        String className = normalize(log == null ? null : log.getClassName());
        String combined = url + " " + className;

        if (combined.contains("processinstance")
                || combined.contains("process/listbyuserid")
                || className.contains("hrmattendanceapprovalsyncserviceimpl")) {
            return "审批数据获取";
        }
        if (combined.contains("attendance/listschedule")
                || combined.contains("schedule/listbyusers")
                || className.contains("attendanceplanrecord")) {
            return "同步考勤-排班";
        }
        if (combined.contains("attendance/listrecord")
                || className.contains("attendancedetailrecord")) {
            return "同步考勤-打卡明细";
        }
        if (combined.contains("attendance/getsimplegroups")
                || combined.contains("attendance/shift")
                || combined.contains("attendance/groups/idtokey")
                || combined.contains("attendance/getusergroup")
                || className.contains("attendancegroupmanager")) {
            return "同步考勤-考勤组/班次";
        }
        if (combined.contains("smartwork/hrm/employee")
                || className.contains("attendanceusermanager")) {
            return "同步考勤-员工花名册";
        }
        if (combined.contains("attendance/getcolumnval")
                || combined.contains("attendance/getattcolumns")
                || combined.contains("attendance/getleavetimebynames")
                || className.contains("hrmattendancereportmanager")) {
            return "同步考勤-报表/请假时长";
        }
        if (combined.contains("attendance/group/schedule/async")
                || combined.contains("attendance/group/users/")) {
            return "排班提交/调组";
        }
        if (combined.contains("gettoken")) {
            return "获取访问令牌";
        }
        return "其他钉钉接口";
    }

    private String buildDescription(long total, long limit, double usagePercent, int threshold, boolean overThreshold) {
        String base = "本月已记录钉钉API调用 " + total + " 次，占月额度 " + usagePercent + "%";
        if (overThreshold) {
            return base + "，已超过" + threshold + "%，请减少非必要同步或缩小同步范围。";
        }
        return base + "，未超过" + threshold + "%提醒线。";
    }

    private double percentage(long total, long limit) {
        if (limit <= 0) {
            return 0D;
        }
        return total * 100D / limit;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
