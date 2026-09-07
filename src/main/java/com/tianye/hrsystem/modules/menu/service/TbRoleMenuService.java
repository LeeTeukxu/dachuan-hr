package com.tianye.hrsystem.modules.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.menu.bo.QueryRoleMenuBO;
import com.tianye.hrsystem.modules.menu.entity.TbMenu;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import com.tianye.hrsystem.modules.menu.mapper.TbRoleMenuMapper;
import com.tianye.hrsystem.model.tbmenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TbRoleMenuService extends BaseServiceImpl<TbRoleMenuMapper, TbRoleMenu> {
    private static final Logger logger = LoggerFactory.getLogger(TbRoleMenuService.class);

    @Autowired
    TbRoleMenuMapper tbRoleMenuMapper;
    @Autowired
    TbMenuService tbMenuService;
    @Autowired
    MenuPermissionSupport menuPermissionSupport;
    @Autowired
    LoginUserMapper loginUserMapper;

    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;

    @Value("${hrm.system.database}")
    String systemBase;

    @Transactional(rollbackFor = Exception.class)
    public Integer saveRoleMenuList(QueryRoleMenuBO queryRoleMenuBO)
    {
        // 1. 保存当前企业的角色权限（原有逻辑）
        LambdaQueryWrapper<TbRoleMenu> wrappers = new LambdaQueryWrapper<>();
        wrappers.eq(TbRoleMenu::getRoleId, queryRoleMenuBO.getRoleId());
        tbRoleMenuMapper.delete(wrappers);

        List<tbmenu> allMenus = tbMenuService.queryAllMenus();
        List<TbRoleMenu> listRoleMenu = menuPermissionSupport.normalizeRoleMenus(queryRoleMenuBO.getRoleId(), queryRoleMenuBO.getListRoleMenu(), allMenus);
        saveBatch(listRoleMenu);

        // 2. 如果用户选择同步到其他企业
        if (Boolean.TRUE.equals(queryRoleMenuBO.getSyncToOtherCompanies())) {
            syncRoleToOtherCompanies(queryRoleMenuBO);
        }

        return 0;
    }

    private void syncRoleToOtherCompanies(QueryRoleMenuBO queryRoleMenuBO) {
        LoginUserInfo currentInfo = CompanyContext.get();
        if (currentInfo == null || StringUtils.isEmpty(currentInfo.getCompanyId())) {
            throw new IllegalStateException("无法获取当前登录企业");
        }
        String baseCompanyId = currentInfo.getCompanyId();

        List<String> accounts = loginUserMapper.getAccountsByRoleId(
            baseCompanyId, databasesuffix, queryRoleMenuBO.getRoleId());

        Set<String> allTargetCompanies = new HashSet<>();
        if (queryRoleMenuBO.getTargetCompanyIds() != null && !queryRoleMenuBO.getTargetCompanyIds().isEmpty()) {
            allTargetCompanies.addAll(queryRoleMenuBO.getTargetCompanyIds());
        } else {
            for (String account : accounts) {
                List<Map<String, Object>> companies = loginUserMapper.getCompaniesByUserName(account, systemBase);
                for (Map<String, Object> company : companies) {
                    allTargetCompanies.add((String) company.get("companyId"));
                }
            }
        }

        String roleName = loginUserMapper.getRoleName(baseCompanyId, databasesuffix, queryRoleMenuBO.getRoleId());
        if (StringUtils.isEmpty(roleName)) {
            roleName = "权限角色" + queryRoleMenuBO.getRoleId();
        }
        List<Integer> menuIds = queryRoleMenuBO.getListRoleMenu().stream()
            .map(TbRoleMenu::getMenuId)
            .collect(Collectors.toList());

        for (String targetCompanyId : allTargetCompanies) {
            if (targetCompanyId.equals(baseCompanyId)) {
                continue;
            }
            try {
                syncRoleToSingleCompany(baseCompanyId, targetCompanyId, queryRoleMenuBO.getRoleId(), roleName, menuIds);
                logger.info("【批量角色同步】已同步角色 {} 到企业 {}", queryRoleMenuBO.getRoleId(), targetCompanyId);
            } catch (Exception e) {
                logger.error("【批量角色同步】同步角色 {} 到企业 {} 失败: {}", queryRoleMenuBO.getRoleId(), targetCompanyId, e.getMessage());
            }
        }
    }

    private void syncRoleToSingleCompany(String baseCompanyId, String targetCompanyId,
                                          Integer roleId, String roleName, List<Integer> menuIds) {
        if (loginUserMapper.countRoleExists(targetCompanyId, databasesuffix, roleId) == 0) {
            loginUserMapper.insertRole(targetCompanyId, databasesuffix, roleId, roleName);
        }

        loginUserMapper.deleteRoleMenu(targetCompanyId, databasesuffix, roleId);
        for (Integer menuId : menuIds) {
            loginUserMapper.insertMenuIfMissing(baseCompanyId, targetCompanyId, databasesuffix, menuId);
            loginUserMapper.insertRoleMenu(targetCompanyId, databasesuffix,
                com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), roleId, menuId);
        }

        for (String account : loginUserMapper.getAccountsByRoleId(baseCompanyId, databasesuffix, roleId)) {
            LoginUserInfo tenantInfo = loginUserMapper.getByAcountAndCompanyID(account, targetCompanyId, databasesuffix);
            if (tenantInfo == null) {
                LoginUserInfo baseInfo = loginUserMapper.getByAcountAndCompanyID(account, baseCompanyId, databasesuffix);
                if (baseInfo != null) {
                    Long depId = baseInfo.getDepIdValue();
                    if (depId == null || loginUserMapper.countDeptExists(targetCompanyId, databasesuffix, depId) == 0) {
                        depId = loginUserMapper.findTopDeptId(targetCompanyId, databasesuffix);
                    }
                    loginUserMapper.insertTenantAccount(targetCompanyId, databasesuffix,
                        baseInfo.getUserName(), account, baseInfo.getPassword(), depId, roleId);
                }
            } else if (!roleId.toString().equals(tenantInfo.getRoleId())) {
                loginUserMapper.updateTenantAccountRole(targetCompanyId, databasesuffix, account, roleId);
            }
        }
    }

    public List<Map<String, Object>> getRoleMenu(QueryRoleMenuBO queryRoleMenuBO) {
        List<TbRoleMenu> findRoleMenu = lambdaQuery().in(TbRoleMenu::getRoleId, queryRoleMenuBO.getRoleId()).list();
        List<Integer> findMenuId = new ArrayList<>();
        List<Map<String, Object>> findMenuName = new ArrayList<>();
        if (findRoleMenu.size() > 0) {
            for (TbRoleMenu roleMenu : findRoleMenu) {
                findMenuId.add(roleMenu.getMenuId());
            }
        }
        if (findMenuId.size() > 0) {
            List<TbMenu> findMenus = tbMenuService.lambdaQuery().in(TbMenu::getId, findMenuId).list();
            if (findMenus.size() > 0) {
                for (TbMenu menu : findMenus) {
                    Map<String, Object> maps = new HashMap<>();
                    maps.put("id", menu.getId());
                    maps.put("name", menu.getName());
                    findMenuName.add(maps);
                }
            }
        }
        return findMenuName;
    }

    public List<String> getLoginRoleMenu(QueryRoleMenuBO queryRoleMenuBO) {
        List<TbRoleMenu> findRoleMenu = lambdaQuery().in(TbRoleMenu::getRoleId, queryRoleMenuBO.getRoleId()).list();
        List<Integer> findMenuId = new ArrayList<>();
        List<String> findMenuName = new ArrayList<>();
        if (findRoleMenu.size() > 0) {
            for (TbRoleMenu roleMenu : findRoleMenu) {
                findMenuId.add(roleMenu.getMenuId());
            }
        }
        if (findMenuId.size() > 0) {
            List<TbMenu> findMenus = tbMenuService.lambdaQuery().in(TbMenu::getId, findMenuId).list();
            if (findMenus.size() > 0) {
                for (TbMenu menu : findMenus) {
                    findMenuName.add(menu.getName());
                }
            }
        }
        return findMenuName;
    }
}
