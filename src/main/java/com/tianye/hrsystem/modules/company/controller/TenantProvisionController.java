package com.tianye.hrsystem.modules.company.controller;

import lombok.extern.slf4j.Slf4j;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.modules.company.service.TenantProvisionService;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SaaS 改造 P1：租户管理
 */
@RestController
@RequestMapping("/tenant")
@Api(tags = "租户管理")
@Slf4j
public class TenantProvisionController {

    @Autowired
    TenantProvisionService tenantProvisionService;

    @Autowired
    com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport apiPermissionPathSupport;

    /**
     * 列出全部租户库及信息，并返回下一可用公司编码（基于真实库自动编号）
     */
    @GetMapping("/list")
    @ApiOperation("租户库列表")
    public successResult list() {
        successResult result = new successResult();
        try {
            result.setData(tenantProvisionService.listTenants());
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * SaaS 改造 P3：热刷新 API-菜单权限映射（修改 tb_api_permission 后调用）
     */
    @PostMapping("/reloadApiPermission")
    @ApiOperation("刷新权限映射")
    public successResult reloadApiPermission() {
        successResult result = new successResult();
        try {
            apiPermissionPathSupport.refresh();
            result.setMessage("权限映射已刷新");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 一键开通新租户
     */
    @PostMapping("/provision")
    @ApiOperation("开通新租户")
    public successResult provision(String companyId, String companyName,
                                   String adminAccount, String adminPassword, String adminName,
                                   String ddAppKey, String ddAppsecret, String ddAgentId) {
        successResult result = new successResult();
        try {
            String message = tenantProvisionService.provision(companyId, companyName,
                    adminAccount, adminPassword, adminName,
                    ddAppKey, ddAppsecret, ddAgentId);
            result.setMessage(message);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * B：平台超管重置某租户管理员密码（累计最多 5 次）。
     * 不传 newPassword 则由系统生成简单临时密码并返回。
     */
    @PostMapping("/resetAdminPassword")
    @ApiOperation("平台超管重置租户管理员密码")
    public successResult resetAdminPassword(String companyId, String newPassword) {
        successResult result = new successResult();
        try {
            LoginUserInfo operator = CompanyContext.get();
            String temp = tenantProvisionService.resetTenantAdminPassword(operator, companyId, newPassword);
            result.setData(java.util.Collections.singletonMap("tempPassword", temp));
            result.setMessage("已重置；临时密码：" + temp + "（请转告租户管理员，其下次登录需改密）");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 删除“仅注册、无数据表”的租户库（清理孤儿租户）。
     * 仅当租户库内 0 张表时允许删除，有表则拒绝。
     */
    @DeleteMapping("/{companyId}")
    @ApiOperation("删除仅注册无表的租户库")
    public successResult deleteEmpty(@PathVariable String companyId) {
        successResult result = new successResult();
        try {
            tenantProvisionService.deleteEmptyTenant(CompanyContext.get(), companyId);
            result.setMessage("租户 " + companyId + " 已删除");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 查看指定租户库的数据表/视图清单（平台排查用）。
     */
    @GetMapping("/tables")
    @ApiOperation("查看租户库的表清单")
    public successResult tables(String companyId) {
        successResult result = new successResult();
        try {
            result.setData(tenantProvisionService.listTenantTables(companyId));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 获取已有租户的钉钉配置列表（用于开户时绑定）
     */
    @GetMapping("/ddAccountList")
    @ApiOperation("获取已有租户的钉钉配置")
    public successResult ddAccountList() {
        successResult result = new successResult();
        try {
            result.setData(tenantProvisionService.listDdAccounts());
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 对比两个租户库的表结构差异（表名、行数、字段数量）
     */
    @GetMapping("/compare")
    @ApiOperation("对比两个租户库的表结构差异")
    public successResult compare(String sourceCompanyId, String targetCompanyId) {
        successResult result = new successResult();
        try {
            result.setData(tenantProvisionService.compareTenantTables(sourceCompanyId, targetCompanyId));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }
}
