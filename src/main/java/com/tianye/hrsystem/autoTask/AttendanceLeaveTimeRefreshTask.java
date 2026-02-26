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
 * 获取请假信息
 * ⑥ 每月1日 00:50 执行（依赖②用户数据，第二耗时Task）
 * 按半月分段，员工级处理
 */
@Component
public class AttendanceLeaveTimeRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmAttendanceReport report;

    @Autowired
    private ddtaskresultRepository ddRep;

    @Autowired
    private StringRedisTemplate redisRep;

    private static final ThreadLocal<SimpleDateFormat> compactFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    @Override
    protected String getTaskFieldName() {
        return "leaveProcess";
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

        List<tbattendanceuser> users = attendanceUserRep.findAll();
        report.UpdateReportFields();
        logger.info("[AttendanceLeaveTimeRefreshTask][{}] 开始同步请假数据，共{}个用户", companyId, users.size());

        String reportKey = compactFormat.get().format(currentDate) + "::Report::" + companyId;
        long startTime = System.currentTimeMillis();
        int totalFailCount = 0;

        for (Date[] dateRange : allDates) {
            Date beginDate = dateRange[0];
            Date endDate = dateRange[1];
            String tBegin = compactFormat.get().format(beginDate);
            String tEnd = compactFormat.get().format(endDate);

            if (checkpoint.isDone(companyId, getClass(), beginDate, endDate)) {
                continue;
            }

            int failCount = 0;
            for (int i = 0; i < users.size(); i++) {
                tbattendanceuser user = users.get(i);
                Long empId = user.getEmpId();
                String userId = user.getUserId();

                if (ddRep.countAllByEmpIdAndClassNameAndBeginAndEnd(
                        empId, "UpdateHolidayReport", tBegin, tEnd) == 0L) {
                    try {
                        rateLimiter.acquire(companyId);
                        report.UpdateHolidayReport(userId, empId, beginDate, endDate);
                        redisRep.opsForValue().set(reportKey, "1", 8, TimeUnit.HOURS);
                        logger.info("[AttendanceLeaveTimeRefreshTask][{}][{}] {}/{} 请假数据已保存",
                                companyId, user.getUserName(), i + 1, users.size());
                    } catch (Exception e) {
                        failCount++;
                        logger.error("[AttendanceLeaveTimeRefreshTask][{}][{}] 处理失败: {}",
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
                logger.warn("[AttendanceLeaveTimeRefreshTask][{}] 日期段{}-{}有{}个员工失败，不标记完成",
                        companyId, beginDate, endDate, failCount);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[AttendanceLeaveTimeRefreshTask][{}] 完成，耗时{}ms", companyId, elapsed);

        // 有失败时抛异常，阻止基类 markDone
        if (totalFailCount > 0) {
            throw new RuntimeException(String.format(
                    "[AttendanceLeaveTimeRefreshTask][%s] 共有%d个员工处理失败，不标记Task完成", companyId, totalFailCount));
        }
    }

    private List<Date[]> buildHalfMonthRanges(String companyId, Date currentDate) throws Exception {
        List<Date[]> ranges = new ArrayList<>();
        Date firstDay = dateUtils.getBeginDayOfMonth(currentDate);
        Date midDay = dateUtils.getMiddleDayOfMonth(currentDate);

        if (!checkpoint.isDone(companyId, getClass(), firstDay, midDay)) {
            ranges.add(new Date[]{firstDay, midDay});
        }

        Date secondHalfStart = DateUtils.addDays(midDay, 1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(secondHalfStart);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        secondHalfStart = cal.getTime();
        if (!checkpoint.isDone(companyId, getClass(), secondHalfStart, currentDate)) {
            ranges.add(new Date[]{secondHalfStart, currentDate});
        }

        return ranges;
    }

    @Scheduled(cron = "0 50 0 1 * ?")
    public void process() {
        execute();
    }
}
