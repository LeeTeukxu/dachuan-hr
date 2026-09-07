package com.tianye.hrsystem.modules.miniapp.service.impl;

import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.MpSchedulePermission;
import com.tianye.hrsystem.model.MpScheduleVisibleEmployee;
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

    @Override
    public MpSchedulePermission getByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return permissionRepository.findByEmployeeId(employeeId);
    }

    @Override
    public boolean canSchedule(Long employeeId) {
        return getByEmployeeId(employeeId) != null;
    }

    @Override
    public boolean hasAnyPermission(Long employeeId) {
        return getByEmployeeId(employeeId) != null;
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
            row.put("canSchedule", setting != null);
            row.put("visibleScope", setting == null || setting.getVisibleScope() == null
                    ? MpSchedulePermission.SCOPE_SUBORDINATES : setting.getVisibleScope());
            row.put("visibleEmployees", setting == null ? Collections.emptyList()
                    : customDetails.getOrDefault(employee.getEmployeeId(), Collections.emptyList()));
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
    public void saveSetting(Long employeeId, boolean canSchedule, Integer visibleScope,
                            List<Long> visibleEmployeeIds) throws Exception {
        if (employeeId == null) {
            throw new Exception("员工ID不能为空");
        }
        HrmEmployee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            throw new Exception("员工不存在");
        }
        MpSchedulePermission existing = permissionRepository.findByEmployeeId(employeeId);
        if (!canSchedule) {
            if (existing != null) {
                visibleEmployeeRepository.deleteByPermissionEmployeeId(employeeId);
                permissionRepository.delete(existing);
            }
            return;
        }
        int scope = visibleScope == null ? MpSchedulePermission.SCOPE_SUBORDINATES : visibleScope;
        if (scope != MpSchedulePermission.SCOPE_SUBORDINATES && scope != MpSchedulePermission.SCOPE_ALL
                && scope != MpSchedulePermission.SCOPE_CUSTOM) {
            throw new Exception("可见范围取值无效");
        }
        if (scope == MpSchedulePermission.SCOPE_CUSTOM
                && (visibleEmployeeIds == null || visibleEmployeeIds.isEmpty())) {
            throw new Exception("自定义可见范围请至少选择一名员工");
        }
        if (existing == null) {
            existing = new MpSchedulePermission();
            existing.setEmployeeId(employeeId);
            existing.setCreateTime(new Date());
        }
        existing.setVisibleScope(scope);
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
}
