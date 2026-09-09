package com.tianye.hrsystem.modules.miniapp.controller;

import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppBatchPermissionSaveBO;
import com.tianye.hrsystem.modules.miniapp.vo.MpPermissionSaveBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 排班小程序员工权限配置（PC 端，hr_web 系统管理 → 排班小程序权限）。
 * 映射表挂菜单 /hrm/system/miniappPermission，走操作员/菜单权限校验。
 */
@RestController
@RequestMapping("/mpPermission")
public class MiniAppPermissionController {

    @Autowired
    private IMiniAppPermissionService permissionService;

    /** 员工列表（姓名模糊搜索 + 分页），附带每人的权限设置 */
    @PostMapping("/employeeList")
    public successResult employeeList(String name, Integer page, Integer size) {
        successResult result = new successResult();
        try {
            int p = page == null || page < 1 ? 1 : page;
            int s = size == null || size < 1 ? 20 : Math.min(size, 200);
            Map<String, Object> data = permissionService.listEmployeeSettings(name, p, s);
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 保存某员工的设置：三个能力开关 + 可见范围（1=直属下属 2=全部 3=自定义） */
    @PostMapping("/save")
    public successResult save(@RequestBody MpPermissionSaveBO request) {
        successResult result = new successResult();
        try {
            permissionService.saveSetting(request.getEmployeeId(),
                    request.getCanSchedule() != null && request.getCanSchedule(),
                    request.getCanViewStatistics() != null && request.getCanViewStatistics(),
                    request.getCanLoadSchedule() != null && request.getCanLoadSchedule(),
                    request.getCanSwitchCompany() != null && request.getCanSwitchCompany(),
                    request.getVisibleScope(), request.getVisibleEmployeeIds());
            result.setMessage("保存成功");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 批量保存多名员工的设置：三个能力开关 + 可见范围（不支持自定义档） */
    @PostMapping("/batchSave")
    public successResult batchSave(@RequestBody MiniAppBatchPermissionSaveBO request) {
        successResult result = new successResult();
        try {
            int count = permissionService.batchSaveSettings(request.getEmployeeIds(),
                    request.getCanSchedule() != null && request.getCanSchedule(),
                    request.getCanViewStatistics() != null && request.getCanViewStatistics(),
                    request.getCanLoadSchedule() != null && request.getCanLoadSchedule(),
                    request.getCanSwitchCompany() != null && request.getCanSwitchCompany(),
                    request.getVisibleScope());
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("count", count);
            result.setData(data);
            result.setMessage("已批量设置 " + count + " 名员工");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 保存某员工「公司切换」可见公司授权（B方案，2026-09-10）。
     * body JSON：{employeeId, visibleCompanyIds:[...]}
     */
    @PostMapping("/saveVisibleCompanies")
    public successResult saveVisibleCompanies(@RequestBody MpPermissionSaveBO request) {
        successResult result = new successResult();
        try {
            int count = permissionService.saveVisibleCompanies(request.getEmployeeId(),
                    request.getVisibleCompanyIds());
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("count", count);
            result.setData(data);
            result.setMessage("已保存「公司切换」可见公司" + count + " 条");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }
}
