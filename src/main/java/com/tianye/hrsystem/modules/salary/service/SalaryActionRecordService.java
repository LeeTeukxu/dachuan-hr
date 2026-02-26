package com.tianye.hrsystem.modules.salary.service;


import com.tianye.hrsystem.entity.vo.Content;
import com.tianye.hrsystem.enums.BehaviorEnum;
import com.tianye.hrsystem.enums.HrmLanguageEnum;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import org.springframework.stereotype.Service;

@Service("salaryActionRecordService")
public class SalaryActionRecordService {

    public Content addNextMonthSalaryLog(HrmSalaryMonthRecord salaryMonthRecord) {
        String detail = "添加" + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle();
        String transDetail = HrmLanguageEnum.ADD.getFieldFormat() + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle();
        return new Content(salaryMonthRecord.getTitle(), detail, transDetail, BehaviorEnum.SAVE);
    }

    public Content computeSalaryDataLog(HrmSalaryMonthRecord salaryMonthRecord) {
        String detail = "核算" + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle();
        String transDetail = HrmLanguageEnum.ACCOUNTING.getFieldFormat() + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle();
        return new Content(salaryMonthRecord.getTitle(), detail, transDetail, BehaviorEnum.SAVE);
    }

    public Content deleteSalaryLog(HrmSalaryMonthRecord salaryMonthRecord) {
        String detail = "删除" + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle();
        String transDetail = HrmLanguageEnum.DELETE.getFieldFormat() + salaryMonthRecord.getYear() + "-" + salaryMonthRecord.getTitle();
        return new Content(salaryMonthRecord.getTitle(), detail, transDetail, BehaviorEnum.SAVE);
    }
}
