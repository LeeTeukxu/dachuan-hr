package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.ddtaskresultRepository;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 同步加班及请假报表数据
 * ⑤ 每月1日 00:20 执行（依赖②用户数据，最耗时Task）
 * 按半月分段，员工级断点续传
 */
@Component
public class AttendanceReportRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmAttendanceReport report;

    @Autowired
    private ddtaskresultRepository ddRep;

    @Autowired
    private StringRedisTemplate redisRep;

    private static final ThreadLocal<SimpleDateFormat> compactFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    @Override
    protected String getTaskFieldName() {
        return "reportProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 构建半月日期段
        List<Date[]> allDates = buildHalfMonthRanges(companyId, currentDate);
        if (allDates.isEmpty()) return;

        // 获取用户列表并更新报表字段
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        report.UpdateReportFields();
        logger.info("[AttendanceReportRefreshTask][{}] 开始同步报表数据，共{}个用户", companyId, users.size());

        String reportKey = compactFormat.get().format(currentDate) + "::Report::" + companyId;
        long startTime = System.currentTimeMillis();
        int totalFailCount = 0;

        for (Date[] dateRange : allDates) {
            Date beginDate = dateRange[0];
            Date endDate = dateRange[1];

            // 日期范围级检查点
            if (checkpoint.isDone(companyId, getClass(), beginDate, endDate)) {
                continue;
            }

            // 遍历每个员工
            int failCount = 0;
            for (int i = 0; i < users.size(); i++) {
                tbattendanceuser user = users.get(i);
                String userId = user.getUserId();
                Long empId = user.getEmpId();
                String tBegin = compactFormat.get().format(beginDate);
                String tEnd = compactFormat.get().format(endDate);

                // 员工级去重
                if (ddRep.countAllByUserIdAndClassNameAndBeginAndEnd(
                        userId, "UpdateAttendanceReport", tBegin, tEnd) == 0L) {
                    try {
                        rateLimiter.acquire(companyId);
                        report.UpdateAttendanceReport(userId, empId, beginDate, endDate);
                        redisRep.opsForValue().set(reportKey, "1", 8, TimeUnit.HOURS);
                        logger.info("[AttendanceReportRefreshTask][{}][{}] {}/{} 报表数据已保存",
                                companyId, user.getUserName(), i + 1, users.size());
                    } catch (Exception e) {
                        failCount++;
                        logger.error("[AttendanceReportRefreshTask][{}][{}] 处理失败: {}",
                                companyId, user.getUserName(), e.getMessage());
                        exceptionUtils.addOne(getClass(), e);
                    }
                }
            }

            // 仅当所有员工都成功时才标记日期段完成
            if (failCount == 0) {
                checkpoint.markDone(companyId, getClass(), beginDate, endDate);
            } else {
                totalFailCount += failCount;
                logger.warn("[AttendanceReportRefreshTask][{}] 日期段{}-{}有{}个员工失败，不标记完成",
                        companyId, beginDate, endDate, failCount);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[AttendanceReportRefreshTask][{}] 完成，耗时{}ms", companyId, elapsed);

        // 有失败时抛异常，阻止基类 markDone
        if (totalFailCount > 0) {
            throw new RuntimeException(String.format(
                    "[AttendanceReportRefreshTask][%s] 共有%d个员工处理失败，不标记Task完成", companyId, totalFailCount));
        }
    }

    /**
     * 构建半月日期段：[月初~月中] + [月中+1~当前]
     */
    private List<Date[]> buildHalfMonthRanges(String companyId, Date currentDate) throws Exception {
        List<Date[]> ranges = new ArrayList<>();
        Date firstDay = dateUtils.getBeginDayOfMonth(currentDate);
        Date midDay = dateUtils.getMiddleDayOfMonth(currentDate);

        // 上半月
        if (!checkpoint.isDone(companyId, getClass(), firstDay, midDay)) {
            ranges.add(new Date[]{firstDay, midDay});
        }

        // 下半月
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUtils.addDays(midDay, 1));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date secondHalfStart = cal.getTime();
        if (!checkpoint.isDone(companyId, getClass(), secondHalfStart, currentDate)) {
            ranges.add(new Date[]{secondHalfStart, currentDate});
        }

        return ranges;
    }

    @Scheduled(cron = "0 20 0 1 * ?")
    public void process() {
        execute();
    }
}
