package com.tianye.hrsystem.modules.miniapp.service.impl;

import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.MpSchedulePermission;
import com.tianye.hrsystem.model.MpScheduleVisibleEmployee;
import com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.mpSchedulePermissionRepository;
import com.tianye.hrsystem.repository.mpScheduleVisibleEmployeeRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排班小程序员工级权限实现。权限数据在各租户库：
 * mp_schedule_permission（配置主表，有记录=可添加排班）+ mp_schedule_visible_employee（自定义可见明细）
 */
@Service
public class MiniAppPermissionServiceImpl implements IMiniAppPermissionService {

    @Autowired
    private mpSchedulePermissionRepository permissionRepository;

    @Autowired
    private mpScheduleVisibleEmployeeRepository visibleEmployeeRepository;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private hrmDeptRepository deptRepository;

    @Autowired
    private com.tianye.hrsystem.modules.miniapp.support.MiniAppVisibleCompanySupport visibleCompanySupport;

    @Override
    public MpSchedulePermission getByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return permissionRepository.findByEmployeeId(employeeId);
    }

    @Override
    public boolean canSchedule(Long employeeId) {
        MpSchedulePermission permission = getByEmployeeId(employeeId);
        return permission != null && isOn(permission.getCanSchedule());
    }

    @Override
    public boolean hasAnyPermission(Long employeeId) {
        MpSchedulePermission permission = getByEmployeeId(employeeId);
        return permission != null
                && (isOn(permission.getCanSchedule())
                || isOn(permission.getCanViewStatistics())
                || isOn(permission.getCanLoadSchedule())
                || isOn(permission.getCanSwitchCompany()));
    }

    @Override
    public boolean hasAbility(Long employeeId, String ability) {
        if (employeeId == null || org.apache.commons.lang.StringUtils.isEmpty(ability)) {
            return false;
        }
        MpSchedulePermission permission = getByEmployeeId(employeeId);
        if (permission == null) {
            // 严格模式：未配置任何权限的员工一律无小程序能力
            return false;
        }
        if (ApiPermissionPathSupport.ABILITY_STATISTICS.equals(ability)) {
            return isOn(permission.getCanViewStatistics());
        }
        if (ApiPermissionPathSupport.ABILITY_SCHEDULE_VIEW.equals(ability)) {
            return isOn(permission.getCanLoadSchedule());
        }
        if (ApiPermissionPathSupport.ABILITY_SCHEDULE_CREATE.equals(ability)) {
            return isOn(permission.getCanSchedule());
        }
        if (ApiPermissionPathSupport.ABILITY_SWITCH_COMPANY.equals(ability)) {
            return isOn(permission.getCanViewStatistics()) && isOn(permission.getCanSwitchCompany());
        }
        return false;
    }

    private static boolean isOn(Integer value) {
        return value != null && value == 1;
    }

    @Override
    public List<Long> resolveVisibleEmployeeIds(Long employeeId) {
        MpSchedulePermission permission = getByEmployeeId(employeeId);
        if (permission != null && permission.getVisibleScope() != null
                && permission.getVisibleScope() == MpSchedulePermission.SCOPE_ALL) {
            // 全部员工档：null=不限制
            return null;
        }
        List<Long> visibleIds = new ArrayList<>();
        for (MpScheduleVisibleEmployee item : visibleEmployeeRepository.findByPermissionEmployeeId(employeeId)) {
            visibleIds.add(item.getVisibleEmployeeId());
        }
        if (permission != null && permission.getVisibleScope() != null
                && permission.getVisibleScope() == MpSchedulePermission.SCOPE_CUSTOM) {
            // 自定义档：仅追加指定员工
            return visibleIds;
        }
        // 默认档（无记录/未配置/直属下属档）：直属下属（员工管理 parent_id）∪ 追加指定员工
        List<Long> ids = new ArrayList<>();
        for (HrmEmployee subordinate : employeeRepository.findAllByParentIdAndIsDel(employeeId, 0)) {
            ids.add(subordinate.getEmployeeId());
        }
        for (Long visibleId : visibleIds) {
            if (!ids.contains(visibleId)) {
                ids.add(visibleId);
            }
        }
        return ids;
    }

    @Override
    public Map<String, Object> listEmployeeSettings(String name, int page, int size) {
        List<HrmEmployee> employees =
                employeeRepository.findAllByIsDelAndEntryStatusIn(0, Arrays.asList(1, 3, 4));
        String keyword = StringUtils.trimToNull(name);
        List<HrmEmployee> filtered = new ArrayList<>();
        Set<Long> deptIds = new HashSet<>();
        for (HrmEmployee employee : employees) {
            if (keyword != null && (employee.getEmployeeName() == null
                    || !employee.getEmployeeName().contains(keyword))) {
                continue;
            }
            filtered.add(employee);
            if (employee.getDeptId() != null) {
                deptIds.add(employee.getDeptId());
            }
        }
        filtered.sort((a, b) -> a.getEmployeeId() == null || b.getEmployeeId() == null ? 0
                : Long.compare(b.getEmployeeId(), a.getEmployeeId()));

        Map<Long, String> deptNames = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (HrmDept dept : deptRepository.findAllByDeptIdIn(new ArrayList<>(deptIds))) {
                deptNames.put(dept.getDeptId(), dept.getName());
            }
        }
        List<Long> employeeIds = new ArrayList<>();
        for (HrmEmployee employee : filtered) {
            employeeIds.add(employee.getEmployeeId());
        }
        Map<Long, MpSchedulePermission> settings = new LinkedHashMap<>();
        if (!employeeIds.isEmpty()) {
            for (MpSchedulePermission setting : permissionRepository.findByEmployeeIdIn(employeeIds)) {
                settings.put(setting.getEmployeeId(), setting);
            }
        }
        Set<Long> customDetailOwners = new HashSet<>();
        for (Long employeeId : settings.keySet()) {
            MpSchedulePermission setting = settings.get(employeeId);
            // 自定义档=全部可见明细；直属下属档=追加指定员工明细
            if (setting.getVisibleScope() != null && (setting.getVisibleScope() == MpSchedulePermission.SCOPE_CUSTOM
                    || setting.getVisibleScope() == MpSchedulePermission.SCOPE_SUBORDINATES)) {
                customDetailOwners.add(employeeId);
            }
        }
        Map<Long, List<Map<String, Object>>> customDetails = new HashMap<>();
        for (Long owner : customDetailOwners) {
            List<Map<String, Object>> items = new ArrayList<>();
            List<Long> visibleIds = new ArrayList<>();
            for (MpScheduleVisibleEmployee item : visibleEmployeeRepository.findByPermissionEmployeeId(owner)) {
                visibleIds.add(item.getVisibleEmployeeId());
            }
            Map<Long, HrmEmployee> visibleEmployees = new HashMap<>();
            if (!visibleIds.isEmpty()) {
                for (HrmEmployee employee : employeeRepository.findAllByEmployeeIdIn(visibleIds)) {
                    visibleEmployees.put(employee.getEmployeeId(), employee);
                }
            }
            for (Long visibleId : visibleIds) {
                HrmEmployee employee = visibleEmployees.get(visibleId);
                Map<String, Object> item = new HashMap<>();
                item.put("employeeId", visibleId);
                item.put("employeeName", employee == null ? "未知员工" : employee.getEmployeeName());
                item.put("deptName", employee == null || employee.getDeptId() == null ? ""
                        : StringUtils.trimToEmpty(deptNames.get(employee.getDeptId())));
                items.add(item);
            }
            customDetails.put(owner, items);
        }

        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + Math.max(1, size));
        List<Map<String, Object>> records = new ArrayList<>();
        for (HrmEmployee employee : filtered.subList(from, to)) {
            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", employee.getEmployeeId());
            row.put("employeeName", employee.getEmployeeName());
            row.put("mobile", employee.getMobile());
            row.put("deptName", employee.getDeptId() == null ? ""
                    : StringUtils.trimToEmpty(deptNames.get(employee.getDeptId())));
            MpSchedulePermission setting = settings.get(employee.getEmployeeId());
            row.put("canSchedule", setting != null && isOn(setting.getCanSchedule()));
            row.put("canViewStatistics", setting != null && isOn(setting.getCanViewStatistics()));
            row.put("canLoadSchedule", setting != null && isOn(setting.getCanLoadSchedule()));
            row.put("canSwitchCompany", setting != null && isOn(setting.getCanSwitchCompany()));
            row.put("visibleScope", setting == null || setting.getVisibleScope() == null
                    ? MpSchedulePermission.SCOPE_SUBORDINATES : setting.getVisibleScope());
            row.put("visibleEmployees", setting == null ? Collections.emptyList()
                    : customDetails.getOrDefault(employee.getEmployeeId(), Collections.emptyList()));
            // 公司切换可见公司（B方案，2026-09-10）：openid + 已授权可见公司 id 列表
            String openid = StringUtils.trimToNull(employee.getOpenid());
            row.put("openid", openid == null ? "" : openid);
            row.put("visibleCompanies", openid == null ? Collections.emptyList()
                    : visibleCompanySupport.queryVisibleByOpenid(openid));
            records.add(row);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSetting(Long employeeId, boolean canSchedule, boolean canViewStatistics,
                            boolean canLoadSchedule, boolean canSwitchCompany,
                            Integer visibleScope,
                            List<Long> visibleEmployeeIds) throws Exception {
        if (employeeId == null) {
            throw new Exception("员工ID不能为空");
        }
        HrmEmployee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            throw new Exception("员工不存在");
        }
        MpSchedulePermission existing = permissionRepository.findByEmployeeId(employeeId);
        // 三个主要能力全关 = 未授权，直接删除记录（含可见范围明细）；
        // canSwitchCompany 是辅助开关，不参与"全关=删除"的判定（不开开关 ≠ 未授权）。
        if (!canSchedule && !canViewStatistics && !canLoadSchedule) {
            if (existing != null) {
                visibleEmployeeRepository.deleteByPermissionEmployeeId(employeeId);
                permissionRepository.delete(existing);
            }
            return;
        }
        int scope = visibleScope == null ? MpSchedulePermission.SCOPE_SUBORDINATES : visibleScope;
        // 可见范围只对"可添加排班"有意义；未授予该能力时不做校验，统一落到直属下属档
        if (!canSchedule) {
            scope = MpSchedulePermission.SCOPE_SUBORDINATES;
            visibleEmployeeIds = null;
        } else {
            if (scope != MpSchedulePermission.SCOPE_SUBORDINATES && scope != MpSchedulePermission.SCOPE_ALL
                    && scope != MpSchedulePermission.SCOPE_CUSTOM) {
                throw new Exception("可见范围取值无效");
            }
            if (scope == MpSchedulePermission.SCOPE_CUSTOM
                    && (visibleEmployeeIds == null || visibleEmployeeIds.isEmpty())) {
                throw new Exception("自定义可见范围请至少选择一名员工");
            }
        }
        if (existing == null) {
            existing = new MpSchedulePermission();
            existing.setEmployeeId(employeeId);
            existing.setCreateTime(new Date());
        }
        existing.setVisibleScope(scope);
        existing.setCanSchedule(canSchedule ? 1 : 0);
        existing.setCanViewStatistics(canViewStatistics ? 1 : 0);
        existing.setCanLoadSchedule(canLoadSchedule ? 1 : 0);
        existing.setCanSwitchCompany(canSwitchCompany ? 1 : 0);
        existing.setUpdateTime(new Date());
        permissionRepository.save(existing);
        visibleEmployeeRepository.deleteByPermissionEmployeeId(employeeId);
        // 自定义档=全部可见明细；直属下属档=追加指定员工（与直属下属并集生效）
        if ((scope == MpSchedulePermission.SCOPE_CUSTOM || scope == MpSchedulePermission.SCOPE_SUBORDINATES)
                && visibleEmployeeIds != null) {
            Set<Long> seen = new HashSet<>();
            for (Long visibleId : visibleEmployeeIds) {
                if (visibleId == null || !seen.add(visibleId) || visibleId.equals(employeeId)) {
                    continue;
                }
                MpScheduleVisibleEmployee item = new MpScheduleVisibleEmployee();
                item.setPermissionEmployeeId(employeeId);
                item.setVisibleEmployeeId(visibleId);
                item.setCreateTime(new Date());
                visibleEmployeeRepository.save(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveSettings(List<Long> employeeIds, boolean canSchedule, boolean canViewStatistics,
                                 boolean canLoadSchedule, boolean canSwitchCompany,
                                 Integer visibleScope) throws Exception {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new Exception("请至少选择一名员工");
        }
        if (visibleScope != null && visibleScope == MpSchedulePermission.SCOPE_CUSTOM) {
            throw new Exception("批量设置不支持自定义可见范围，请逐个员工配置");
        }
        if (visibleScope != null && visibleScope != MpSchedulePermission.SCOPE_SUBORDINATES
                && visibleScope != MpSchedulePermission.SCOPE_ALL) {
            throw new Exception("可见范围取值无效");
        }
        int count = 0;
        Set<Long> seen = new HashSet<>();
        for (Long employeeId : employeeIds) {
            if (employeeId == null || !seen.add(employeeId)) {
                continue;
            }
            HrmEmployee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                continue;
            }
            MpSchedulePermission existing = permissionRepository.findByEmployeeId(employeeId);
            if (!canSchedule && !canViewStatistics && !canLoadSchedule) {
                if (existing != null) {
                    visibleEmployeeRepository.deleteByPermissionEmployeeId(employeeId);
                    permissionRepository.delete(existing);
                }
                count++;
                continue;
            }
            int scope;
            if (!canSchedule) {
                // 未授予添加排班：可见范围无意义，统一落直属下属档
                scope = MpSchedulePermission.SCOPE_SUBORDINATES;
            } else if (visibleScope != null) {
                scope = visibleScope;
            } else if (existing != null && existing.getVisibleScope() != null) {
                scope = existing.getVisibleScope();
            } else {
                scope = MpSchedulePermission.SCOPE_SUBORDINATES;
            }
            if (existing == null) {
                existing = new MpSchedulePermission();
                existing.setEmployeeId(employeeId);
                existing.setCreateTime(new Date());
            }
            existing.setVisibleScope(scope);
            existing.setCanSchedule(canSchedule ? 1 : 0);
            existing.setCanViewStatistics(canViewStatistics ? 1 : 0);
            existing.setCanLoadSchedule(canLoadSchedule ? 1 : 0);
            existing.setCanSwitchCompany(canSwitchCompany ? 1 : 0);
            existing.setUpdateTime(new Date());
            permissionRepository.save(existing);
            if (scope != MpSchedulePermission.SCOPE_CUSTOM) {
                visibleEmployeeRepository.deleteByPermissionEmployeeId(employeeId);
            }
            count++;
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveVisibleCompanies(Long employeeId, List<String> visibleCompanyIds) throws Exception {
        if (employeeId == null) {
            throw new Exception("员工ID不能为空");
        }
        HrmEmployee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            throw new Exception("员工不存在");
        }
        String openid = StringUtils.trimToNull(employee.getOpenid());
        if (openid == null) {
            throw new Exception("该员工尚未在小程序绑定登录（无 openid），无法授予跨公司可见；请先让该员工在微信小程序登录一次");
        }
        java.util.List<String> companyIds = new java.util.ArrayList<>();
        if (visibleCompanyIds != null) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String cid : visibleCompanyIds) {
                String c = StringUtils.trimToNull(cid);
                if (c != null && seen.add(c)) {
                    companyIds.add(c);
                }
            }
        }
        return visibleCompanySupport.saveVisibleCompanies(openid, employee.getMobile(), companyIds);
    }
}
