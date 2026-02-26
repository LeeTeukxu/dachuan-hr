package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.service.ddTalk.IUserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 考勤用户与系统用户关联
 * ② 每月1日 00:05 执行（依赖①完成，后续所有Task都依赖本Task的用户数据）
 * 仅月末生效
 */
@Component
public class AttendanceUserRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IUserManager userManager;

    @Override
    protected String getTaskFieldName() {
        return "userProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        logger.info("[AttendanceUserRefreshTask][{}] 开始同步考勤用户", companyId);
        userManager.GetAndSave();
    }

    @Scheduled(cron = "0 5 0 1 * ?")
    public void process() {
        execute();
    }
}
