package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.HrmEmployee;

import java.time.YearMonth;
import java.util.List;

public interface IHrmAttendanceApprovalSyncService {

    long fetchMonthData(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) throws Exception;

    /**
     * 按“发起时间窗口”抓取审批数据（V4 主入口）。
     *
     * @param month          展示/兼容用业务月（可为空，为空时仅作日志/展示）
     * @param fetchStartTime 发起时间窗口起点毫秒（用户所选开始日期）；null 时按 month 首日兜底
     * @param fetchEndTime   发起时间窗口终点毫秒（点“确定”的当下）；null 时按 month 末日兜底
     * @param employeeIds    员工ID列表，空表示全部
     * @param approvalTypes  审批类型
     * @return 本次新增/更新落库条数
     */
    long fetchMonthData(YearMonth month, Long fetchStartTime, Long fetchEndTime,
                        List<Long> employeeIds, List<String> approvalTypes) throws Exception;

    List<Long> resolveFetchTargetEmployeeIds(List<Long> employeeIds);

    /**
     * 确保员工已映射钉钉 userId（映射前置保证）。
     * 已有 dingtalk_user_id 直接返回；缺失时按「姓名+手机号」在钉钉匹配并回写员工表。
     *
     * @return 员工的钉钉 userId
     * @throws com.tianye.hrsystem.common.EmployeeNotInDingTalkException 钉钉查无此人/资料缺失（调用方应拒绝保存）
     * @throws Exception                                                 钉钉服务异常（调用方可按"待映射"放行）
     */
    String ensureDingTalkUserId(HrmEmployee employee) throws Exception;
}
