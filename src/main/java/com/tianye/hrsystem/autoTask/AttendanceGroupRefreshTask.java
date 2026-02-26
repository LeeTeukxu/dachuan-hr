package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.service.ddTalk.IGroupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 同步考勤组和考勤班次
 * ① 每月1日 00:00 执行（第一个Task，无依赖）
 * 仅月末生效（getCurrent()返回未完成月份的月末日期）
 */
@Component
public class AttendanceGroupRefreshTask extends AbstractDingTalkTask {

    @Autowired
    private IGroupManager groupManager;

    @Override
    protected String getTaskFieldName() {
        return "groupProcess";
    }

    @Override
    protected boolean requireEndOfMonth() {
        return true;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        logger.info("[AttendanceGroupRefreshTask][{}] 开始同步考勤组数据", companyId);
        groupManager.GetAndSave();
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void process() {
        execute();
    }
}
