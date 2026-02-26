package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 处理异步队列（Ddtaskresult）中的报表数据
 * ⑦ 每20秒执行一次，不受月末限制
 * 消费⑤ReportRefresh和⑥LeaveTimeRefresh写入的Redis标记和队列数据
 * 从队列取一条未处理记录 → 反序列化 → 保存到报表数据表
 */
@Component
public class AttendanceReportDataSaveTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmAttendanceReport report;

    @Autowired
    private StringRedisTemplate redisRep;

    private static final ThreadLocal<SimpleDateFormat> compactFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    @Override
    protected String getTaskFieldName() {
        return null; // 不需要进度追踪
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false; // 不受月末限制
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        String reportKey = compactFormat.get().format(currentDate) + "::Report::" + companyId;
        if (Boolean.TRUE.equals(redisRep.hasKey(reportKey))) {
            report.ProcessOne(companyId);
        }
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void process() {
        execute();
    }
}
