package com.tianye.hrsystem.modules.miniapp.service;

import com.tianye.hrsystem.modules.miniapp.vo.MiniAppLoginVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppMonthScheduleVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppDayShiftVO;

import java.util.Date;

public interface IMiniAppService {

    /**
     * code + 手机号code 登录。
     * 已绑定 openid → 直接返回 token；未绑定手机号精确匹配：
     *   唯一 → 绑定并返回 token；多条 → 返回 candidates 让用户选公司。
     */
    MiniAppLoginVO login(String code, String phoneCode) throws Exception;

    /** 手机号跨公司多条时，用户选定公司后绑定 openid 并返回 token */
    MiniAppLoginVO bindCompany(String ticket, String companyId) throws Exception;

    /** 月度排班（月历数据），每员工视角，含待审批标记 */
    MiniAppMonthScheduleVO queryMonthSchedule(Long employeeId, String month) throws Exception;

    /** 某天排班详情 + 该日我的申请（workDate yyyy-MM-dd） */
    MiniAppDayShiftVO queryDayDetail(Long employeeId, Date workDate) throws Exception;

    /** 是否上级角色（存在未删除的直属下级，parent_id 指向自己） */
    boolean isSupervisor(Long employeeId);

    /** 审批权限校验：当前员工必须是该申请单申请人的直属上级，否则抛异常 */
    void checkApprovalPermission(Long approverEmployeeId, Long applicationId) throws Exception;
}