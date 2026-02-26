package com.tianye.hrsystem.modules.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.menu.bo.QueryRoleMenuBO;
import com.tianye.hrsystem.modules.menu.entity.TbMenu;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import com.tianye.hrsystem.modules.menu.mapper.TbRoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TbRoleMenuService extends BaseServiceImpl<TbRoleMenuMapper, TbRoleMenu> {
    @Autowired
    TbRoleMenuMapper tbRoleMenuMapper;
    @Autowired
    TbMenuService tbMenuService;

    @Transactional(rollbackFor = Exception.class)
    public Integer saveRoleMenuList(QueryRoleMenuBO queryRoleMenuBO)
    {
        LambdaQueryWrapper<TbRoleMenu> wrappers = new LambdaQueryWrapper<>();
        wrappers.eq(TbRoleMenu::getRoleId, queryRoleMenuBO.getRoleId());
        tbRoleMenuMapper.delete(wrappers);

        List<TbRoleMenu> listRoleMenu = queryRoleMenuBO.getListRoleMenu();
        saveBatch(listRoleMenu);
        return 0;
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
