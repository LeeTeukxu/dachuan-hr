package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.imple.ddTalk.AttendancePlanRecord;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 同步排班计划
 * ③ 每月1日 00:10 执行（依赖②用户数据）
 * 按日期逐天同步，支持日期级断点续传
 */
@Component
public class AttendancePlanRecordTask extends AbstractDingTalkTask {

    @Autowired
    private AttendancePlanRecord planRecord;

    @Autowired
    private hrmAttendancePlanRepository planRep;

    @Override
    protected String getTaskFieldName() {
        return "planProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        Date beginDate = dateUtils.getBeginDayOfMonth(currentDate);
        Date endDate = dateUtils.getEndDayOfMonth(currentDate);

        // 设置用户列表
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        planRecord.setUsers(users);

        // 按日期逐天同步
        Date workDate = beginDate;
        int processedDays = 0;
        int failedDays = 0;
        while (!workDate.after(endDate) && !workDate.after(currentDate)) {
            // 日期级检查点
            if (!checkpoint.isDone(companyId, getClass(), workDate)) {
                try {
                    rateLimiter.acquire(companyId);
                    planRecord.GetAndSave(workDate);
                    checkpoint.markDone(companyId, getClass(), workDate);
                    processedDays++;
                    logger.info("[AttendancePlanRecordTask][{}] 已同步{}的排班数据",
                            companyId, workDate);
                } catch (Exception e) {
                    failedDays++;
                    logger.error("[AttendancePlanRecordTask][{}] 同步{}失败: {}", companyId, workDate, e.getMessage());
                    exceptionUtils.addOne(getClass(), e);
                }
            }
            workDate = DateUtils.addDays(workDate, 1);
        }

        // 仅在所有天数都成功时才清理重复用户
        if (failedDays == 0) {
            planRecord.DeleteRepeatUser(beginDate, endDate);
        } else {
            logger.warn("[AttendancePlanRecordTask][{}] 有{}天同步失败，跳过清理重复用户", companyId, failedDays);
        }
        logger.info("[AttendancePlanRecordTask][{}] 完成，共处理{}天，失败{}天", companyId, processedDays, failedDays);

        if (failedDays > 0) {
            throw new RuntimeException(String.format(
                    "[AttendancePlanRecordTask][%s] 有%d天同步失败，不标记Task完成", companyId, failedDays));
        }
    }

    @Scheduled(cron = "0 10 0 1 * ?")
    public void process() {
        execute();
    }
}
