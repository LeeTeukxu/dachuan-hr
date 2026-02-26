package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.service.ddTalk.IDetailRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 查询考勤明细（打卡记录）
 * ④ 每月1日 00:15 执行（依赖②用户数据）
 * 按7天分段查询，支持日期范围级断点续传
 */
@Component
public class AttendanceDetailRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IDetailRecord detailRecord;

    @Override
    protected String getTaskFieldName() {
        return "detailProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 设置用户列表
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        detailRecord.setUsers(users);

        // 按7天分段查询
        List<Date[]> dateRanges = dateUtils.getDateRangeByLimit(currentDate, 7);
        int processedRanges = 0;

        for (Date[] range : dateRanges) {
            Date beginDate = range[0];
            Date endDate = range[1];

            // 日期范围级检查点
            if (!checkpoint.isDone(companyId, getClass(), beginDate, endDate)) {
                try {
                    rateLimiter.acquire(companyId);
                    detailRecord.GetAndSave(beginDate, endDate);
                    checkpoint.markDone(companyId, getClass(), beginDate, endDate);
                    logger.info("[AttendanceDetailRefreshTask][{}] 已同步{} ~ {}的考勤明细",
                            companyId, beginDate, endDate);
                    processedRanges++;
                } catch (Exception e) {
                    logger.error("[AttendanceDetailRefreshTask][{}] 同步{} ~ {}失败: {}", companyId, beginDate, endDate, e.getMessage());
                    exceptionUtils.addOne(getClass(), e);
                }
            }
        }

        logger.info("[AttendanceDetailRefreshTask][{}] 完成，共处理{}个日期段", companyId, processedRanges);
    }

    @Scheduled(cron = "0 15 0 1 * ?")
    public void process() {
        execute();
    }
}
