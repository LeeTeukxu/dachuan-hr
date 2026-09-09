package com.tianye.hrsystem.modules.miniapp.service;

import com.tianye.hrsystem.model.MpSchedulePermission;

import java.util.List;
import java.util.Map;

/**
 * 小程序员工级权限（2026-09-09 起三个能力统一按员工配置）：
 * 1. 添加排班（can_schedule）
 * 2. 小程序数据统计（can_view_statistics）——/mp/dashboard/*
 * 3. 排班数据加载（can_load_schedule）——/mp/mySchedule、/mp/mySchedule/day、/mp/schedule/query
 * 4. 审批信息可见范围——按员工三档：1=直属下属(默认) 2=全部员工 3=自定义指定
 * 未配置任何能力的员工，上述接口一律拒绝（严格模式）。
 */
public interface IMiniAppPermissionService {

    /** 员工的权限配置，未配置返回 null */
    MpSchedulePermission getByEmployeeId(Long employeeId);

    /** 是否被授予"添加排班"权限 */
    boolean canSchedule(Long employeeId);

    /** 是否存在任何小程序权限配置（用于首页入口显隐） */
    boolean hasAnyPermission(Long employeeId);

    /**
     * 员工级能力判定：ability 取 {@link com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport#ABILITY_STATISTICS}
     * 或 {@link com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport#ABILITY_SCHEDULE_VIEW}、
     * 或 {@link com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport#ABILITY_SWITCH_COMPANY}。
     * SWITCH_COMPANY 隐含要求 can_view_statistics=1。
     */
    boolean hasAbility(Long employeeId, String ability);

    /**
     * 审批信息可见的员工 ID 列表。
     * 返回 null 表示不限制（全部员工）；空列表表示看不到任何人的申请。
     */
    List<Long> resolveVisibleEmployeeIds(Long employeeId);

    /** PC 配置页：员工列表（姓名/部门名 + 权限设置），name 支持模糊搜索 */
    Map<String, Object> listEmployeeSettings(String name, int page, int size);

    /**
     * PC 配置页：保存某员工的设置（三个能力全关时删除配置记录）。
     * canSwitchCompany（2026-09-10，2a）：与 canViewStatistics 组合决定员工是否可切到非本司；
     * 与可见公司清单(hrsystem.mp_employee_visible_company) 独立——清单控"可看哪些公司"，本字段控"是否允许切"。
     */
    void saveSetting(Long employeeId, boolean canSchedule, boolean canViewStatistics,
                     boolean canLoadSchedule, boolean canSwitchCompany,
                     Integer visibleScope, List<Long> visibleEmployeeIds) throws Exception;

    /**
     * PC 配置页：批量设置能力开关（按部门/全员开通用）。
     * canSwitchCompany 在批量场景下：true=开启（与 canViewStatistics 组合生效），
     * false=关闭（员工若有可切公司授权也被冻结）。
     */
    int batchSaveSettings(List<Long> employeeIds, boolean canSchedule, boolean canViewStatistics,
                          boolean canLoadSchedule, boolean canSwitchCompany,
                          Integer visibleScope) throws Exception;

    /**
     * 保存某员工「公司切换」可见公司授权（全量替换，系统库 mp_employee_visible_company）。
     * 员工需已在本登录公司绑定 openid（先在小程序登录过），否则无法跨公司授权。
     * @return 实际写入行数
     */
    int saveVisibleCompanies(Long employeeId, List<String> visibleCompanyIds) throws Exception;
}
