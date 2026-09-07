package com.tianye.hrsystem.modules.miniapp.service;

import com.tianye.hrsystem.model.MpSchedulePermission;

import java.util.List;
import java.util.Map;

/**
 * 排班小程序员工级权限：
 * 1. 添加排班——按员工授权（mp_schedule_permission 有记录即有权限）
 * 2. 审批信息可见范围——按员工三档：1=直属下属(默认) 2=全部员工 3=自定义指定
 */
public interface IMiniAppPermissionService {

    /** 员工的权限配置，未配置返回 null */
    MpSchedulePermission getByEmployeeId(Long employeeId);

    /** 是否被授予"添加排班"权限 */
    boolean canSchedule(Long employeeId);

    /** 是否存在任何小程序权限配置（用于首页入口显隐） */
    boolean hasAnyPermission(Long employeeId);

    /**
     * 审批信息可见的员工 ID 列表。
     * 返回 null 表示不限制（全部员工）；空列表表示看不到任何人的申请。
     */
    List<Long> resolveVisibleEmployeeIds(Long employeeId);

    /** PC 配置页：员工列表（姓名/部门名 + 权限设置），name 支持模糊搜索 */
    Map<String, Object> listEmployeeSettings(String name, int page, int size);

    /** PC 配置页：保存某员工的设置（canSchedule=false 时删除配置记录） */
    void saveSetting(Long employeeId, boolean canSchedule, Integer visibleScope, List<Long> visibleEmployeeIds) throws Exception;
}
