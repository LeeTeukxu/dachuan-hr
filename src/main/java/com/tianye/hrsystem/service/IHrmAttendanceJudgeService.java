package com.tianye.hrsystem.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 本地考勤判定引擎（2026-09 弃用钉钉推送后的本地口径）：
 * 应出勤基准 = tbplanlist 班次（夜班即跨天班），打卡来源 = tbattendancedetail + 补卡记录，
 * 阈值/窗口/补卡限额来自考勤规则设置。
 */
public interface IHrmAttendanceJudgeService {

    /**
     * 重算指定日期范围的判定结果（幂等，逐人逐日 upsert）。
     *
     * @param begin       开始日期
     * @param end         结束日期
     * @param employeeIds 指定员工；空=全部在职
     * @return 本次写库行数
     */
    int recompute(Date begin, Date end, List<Long> employeeIds) throws Exception;

    /** 查询判定结果（考勤汇总「本地判定」列数据源） */
    List<Map<String, Object>> queryResults(Date begin, Date end, List<Long> employeeIds) throws Exception;
}
