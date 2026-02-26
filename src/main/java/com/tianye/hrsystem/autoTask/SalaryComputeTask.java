package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.service.SalaryMonthRecordService_Bak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 工资计算Task
 * 当前已禁用
 */
@Component
public class SalaryComputeTask extends AbstractDingTalkTask {

    @Autowired
    private SalaryMonthRecordService_Bak salaryMonthRecordService;

    @Override
    protected String getTaskFieldName() {
        return null;
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        logger.info("[SalaryComputeTask][{}] 开始核算工资", companyId);
        HrmSalaryMonthRecord record = salaryMonthRecordService.queryLastSalaryMonthRecord();
        // salaryMonthRecordService.computeSalaryData(record.getSRecordId(), true, true, null, null, null);
        logger.info("[SalaryComputeTask][{}] 核算工资完毕", companyId);
    }

    // 当前已禁用
    // @Scheduled(cron = "* 0/5 * * * ?")
    // public void process() { execute(); }
}
