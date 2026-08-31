package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import io.swagger.annotations.Api;
import javax.servlet.http.HttpServletRequest;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/companyPermission")
@Api(tags = "企业权限管理")
public class CompanyPermissionController {

    private static final Logger logger = LoggerFactory.getLogger(CompanyPermissionController.class);

    @Autowired
    LoginUserMapper userMapper;

    @Value("${hrm.system.database}")
    String systemBase;

    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;

    @GetMapping("/list")
    @ApiOperation("查询所有账号的企业权限")
    public Result list() {
        try {
            List<Map<String, Object>> list = userMapper.listAccountCompanyPermissions(systemBase);
            Map<String, Map<String, Object>> grouped = new HashMap<>();
            for (Map<String, Object> row : list) {
                String account = (String) row.get("account");
                grouped.computeIfAbsent(account, k -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("account", account);
                    item.put("companies", new java.util.ArrayList<>());
                    return item;
                });
                Map<String, Object> company = new HashMap<>();
                company.put("companyId", row.get("companyId"));
                company.put("companyName", row.get("companyName"));
                ((List<Map<String, Object>>) grouped.get(account).get("companies")).add(company);
            }
            return Result.ok(grouped.values());
        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(), "查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/companies")
    @ApiOperation("获取所有可选企业列表")
    public Result companies() {
        try {
            List<Map<String, Object>> list = userMapper.getAllCompanies(systemBase);
            return Result.ok(list);
        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(), "查询企业列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/accountCompanies")
    @ApiOperation("获取指定账号的企业列表")
    public Result accountCompanies(@RequestParam String account) {
        try {
            List<String> companyIds = userMapper.getAccountCompanyIds(systemBase, account);
            return Result.ok(companyIds);
        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(), "查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    @ApiOperation("保存账号的企业权限（全量替换）。以当前登录库中被分配账号自身的角色与菜单权限为基准，"
            + "为缺失账号的企业自动创建同名账号并统一角色；目标企业缺该角色时按基准角色新建并复制相同菜单权限")
    @Transactional
    public Result save(@RequestParam String account, @RequestBody List<String> companyIds, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(account)) {
                throw new IllegalArgumentException("登录账号不能为空");
            }

            // 当前登录者所在库 = “当前登录库”（基准库）
            LoginUserInfo adminInfo = resolveCurrentUser(request);
            if (adminInfo == null || StringUtils.isEmpty(adminInfo.getCompanyId())) {
                throw new IllegalArgumentException("无法从登录令牌解析当前登录企业，请重新登录");
            }
            String baseCompanyId = adminInfo.getCompanyId();

            // 基准角色/菜单权限 = 被分配账号在当前登录库自身的角色及其菜单权限（需求：以当前登录库该账号的角色为基准）
            LoginUserInfo targetBase = userMapper.getByAcountAndCompanyID(account, baseCompanyId, databasesuffix);
            if (targetBase == null || StringUtils.isEmpty(targetBase.getRoleId())) {
                throw new IllegalArgumentException("账号 " + account + " 在当前登录企业 " + baseCompanyId + " 中不存在或尚未绑定角色，无法作为权限基准");
            }
            Integer baselineRoleId = Integer.parseInt(targetBase.getRoleId());
            String baselineRoleName = userMapper.getRoleName(baseCompanyId, databasesuffix, baselineRoleId);
            if (StringUtils.isEmpty(baselineRoleName)) {
                baselineRoleName = "权限角色" + baselineRoleId;
            }
            List<Integer> baselineMenuIds = userMapper.getRoleMenuIds(baseCompanyId, databasesuffix, baselineRoleId);
            if (baselineMenuIds == null) {
                baselineMenuIds = new java.util.ArrayList<>();
            }
            // 基准角色没有任何菜单权限时禁止保存:登录校验要求角色必须有可启用页面菜单,
            // 静默跳过会留下"角色已绑定但菜单为空"的账号(登录报"该账号绑定的角色未配置菜单权限")。
            // 该状态通常是历史失败保存"先删后插"留下的残留,需先在角色权限分配中补齐菜单。
            if (baselineMenuIds.isEmpty()) {
                throw new IllegalArgumentException("基准账号 " + account + " 在当前登录企业 " + baseCompanyId
                        + " 的角色【" + baselineRoleName + "】未配置任何菜单权限，无法为企业同步菜单；"
                        + "请先在【角色权限分配】中为该角色勾选菜单并保存后，再重新保存企业权限");
            }

            // 全量替换授权映射
            userMapper.deleteAccountCompanies(systemBase, account);
            if (companyIds != null) {
                for (String companyId : companyIds) {
                    if (companyId == null || companyId.trim().isEmpty()) continue;
                    companyId = companyId.trim();
                    userMapper.insertAccountCompany(systemBase, account, companyId);

                    // 目标企业缺基准角色 → 按当前登录库该账号的角色新建（同名同 id）
                    if (userMapper.countRoleExists(companyId, databasesuffix, baselineRoleId) == 0) {
                        userMapper.insertRole(companyId, databasesuffix, baselineRoleId, baselineRoleName);
                        logger.info("【企业权限】目标企业 {} 缺角色 {}，已按当前登录库角色新建", companyId, baselineRoleId);
                    }
                    // 同步菜单权限：以当前登录库该角色的菜单权限为准（先清后插，保证一致）；
                    // 基准菜单为空已在保存前拦截，这里必然有菜单可同步
                    userMapper.deleteRoleMenu(companyId, databasesuffix, baselineRoleId);
                    for (Integer menuId : baselineMenuIds) {
                        // 目标库 tbmenu 缺失的菜单定义先补齐,避免角色-菜单映射引用不存在的菜单,
                        // 登录时报"该账号绑定的角色未配置菜单权限"
                        userMapper.insertMenuIfMissing(baseCompanyId, companyId, databasesuffix, menuId);
                        // tbrolemenu.role_menu_id 为非自增主键:显式生成雪花 ID(与角色权限分配页 ASSIGN_ID 同策略)
                        userMapper.insertRoleMenu(companyId, databasesuffix,
                                com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), baselineRoleId, menuId);
                    }

                    // 该企业库若没有此账号 → 自动创建同名账号并统一角色
                    LoginUserInfo tenantInfo = userMapper.getByAcountAndCompanyID(account, companyId, databasesuffix);
                    if (tenantInfo == null) {
                        try {
                            Long depId = targetBase.getDepIdValue();
                            if (depId == null || userMapper.countDeptExists(companyId, databasesuffix, depId) == 0) {
                                depId = userMapper.findTopDeptId(companyId, databasesuffix);
                            }
                            userMapper.insertTenantAccount(companyId, databasesuffix, targetBase.getUserName(), account, targetBase.getPassword(), depId, baselineRoleId);
                            logger.info("【企业权限】已为企业 {} 自动创建账号 {} (roleId={}, depId={})",
                                    companyId, account, baselineRoleId, depId);
                        } catch (Exception ex) {
                            logger.warn("【企业权限】为企业 {} 自动创建账号 {} 失败: {}", companyId, account, ex.getMessage());
                        }
                    } else if (!baselineRoleId.toString().equals(tenantInfo.getRoleId())) {
                        // 已存在则统一角色，保证多企业账号角色一致
                        userMapper.updateTenantAccountRole(companyId, databasesuffix, account, baselineRoleId);
                        logger.info("【企业权限】已统一企业 {} 账号 {} 的角色为 {}", companyId, account, baselineRoleId);
                    }
                }
            }
            return Result.ok("保存成功");
        } catch (Exception e) {
            // 全量替换语义:任一公司失败必须整体回滚(包括循环前已删除的企业映射),
            // 否则会留下"映射存在但角色/菜单未同步"的中间状态,登录报"该账号绑定的角色未配置菜单权限"。
            // 异常在此被捕获不会触发事务回滚,必须显式标记 rollback-only。
            org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus().setRollbackOnly();
            logger.error("【企业权限】保存失败,已整体回滚: account={}, {}", account, e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(), "保存失败: " + e.getMessage());
        }
    }

    private LoginUserInfo resolveCurrentUser(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (StringUtils.isEmpty(token)) {
            token = request.getHeader("token");
        }
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        try {
            return JWTTokenUtils.GetByToken(token);
        } catch (Exception e) {
            logger.warn("解析登录令牌失败: {}", e.getMessage());
            return null;
        }
    }
}
