package com.tianye.hrsystem.modules.menu.service;

import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MenuPermissionSupportTest {

    private final MenuPermissionSupport support = new MenuPermissionSupport();

    @Test
    public void normalizeRoleMenus_shouldAutoAttachParentModule_whenOnlySubMenuSelected() {
        List<tbmenu> menus = Arrays.asList(
                menu(10, 0, "系统管理", 1),
                menu(11, 10, "登录用户管理", 1),
                menu(12, 10, "角色管理", 1)
        );
        TbRoleMenu selectedSubMenu = roleMenu(3, 11);

        List<TbRoleMenu> normalized = support.normalizeRoleMenus(3, Collections.singletonList(selectedSubMenu), menus);

        List<Integer> menuIds = normalized.stream().map(TbRoleMenu::getMenuId).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(10, 11), menuIds);
        Assert.assertTrue(normalized.stream().allMatch(item -> Integer.valueOf(3).equals(item.getRoleId())));
    }

    @Test
    public void normalizeRoleMenus_shouldRejectRoleWithoutEnabledSubMenuPermission() {
        List<tbmenu> menus = Arrays.asList(
                menu(10, 0, "系统管理", 1),
                menu(11, 10, "登录用户管理", 2)
        );

        try {
            support.normalizeRoleMenus(3, Collections.singletonList(roleMenu(3, 10)), menus);
            Assert.fail("只选择父模块时应阻止保存角色权限");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("角色必须至少配置一个启用的子菜单权限", ex.getMessage());
        }
    }

    @Test
    public void normalizeRoleMenus_shouldAllowRootLeafMenu_whenTopLevelMenuIsPage() {
        List<tbmenu> menus = Collections.singletonList(menu(20, 0, "员工管理", 1));

        List<TbRoleMenu> normalized = support.normalizeRoleMenus(3, Collections.singletonList(roleMenu(3, 20)), menus);

        Assert.assertEquals(1, normalized.size());
        Assert.assertEquals(Integer.valueOf(20), normalized.get(0).getMenuId());
    }

    @Test
    public void normalizeRoleMenus_shouldRejectParentOnlyMenuEvenWhenOtherPageSelected() {
        List<tbmenu> menus = Arrays.asList(
                menu(10, 0, "组织管理", 1),
                menu(20, 0, "系统设置", 1),
                menu(21, 20, "假期管理", 1)
        );

        try {
            support.normalizeRoleMenus(3, Arrays.asList(roleMenu(3, 10), roleMenu(3, 20)), menus);
            Assert.fail("父级菜单没有子菜单时不应保存成功");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("父级菜单不能单独授权，请至少勾选一个子菜单权限", ex.getMessage());
        }
    }

    @Test
    public void requireRoleHasEnabledSubMenu_shouldRejectLoginUserRoleWithoutPermission() {
        List<tbmenu> menus = Arrays.asList(
                menu(10, 0, "系统管理", 1),
                menu(11, 10, "登录用户管理", 1)
        );

        try {
            support.requireRoleHasEnabledSubMenu(5, Collections.emptyList(), menus);
            Assert.fail("创建登录用户时角色无权限应被阻止");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("该账号绑定的角色未配置菜单权限", ex.getMessage());
        }
    }

    @Test
    public void buildAuthorizedMenuTree_shouldOnlyReturnAllowedEnabledModulesAndSubMenus() {
        List<tbmenu> menus = Arrays.asList(
                menu(10, 0, "系统管理", 1),
                menu(11, 10, "登录用户管理", 1),
                menu(12, 10, "角色管理", 2),
                menu(20, 0, "薪资管理", 1),
                menu(21, 20, "薪资核算", 1)
        );

        List<tbmenu> tree = support.buildAuthorizedMenuTree(menus, Arrays.asList(10, 11, 12));

        Assert.assertEquals(1, tree.size());
        Assert.assertEquals("系统管理", tree.get(0).getName());
        Assert.assertEquals(1, tree.get(0).getChildren().size());
        Assert.assertEquals("登录用户管理", tree.get(0).getChildren().get(0).getName());
    }

    @Test
    public void buildAuthorizedMenuTree_shouldReturnRootLeafMenu_whenTopLevelMenuIsPage() {
        List<tbmenu> menus = Collections.singletonList(menu(20, 0, "员工管理", 1));

        List<tbmenu> tree = support.buildAuthorizedMenuTree(menus, Collections.singletonList(20));

        Assert.assertEquals(1, tree.size());
        Assert.assertEquals("员工管理", tree.get(0).getName());
    }

    @Test
    public void buildAuthorizedMenuTree_shouldSupportCurrentProductionMenuShape() {
        List<tbmenu> menus = Arrays.asList(
                menu(10, 0, "组织管理", 1),
                menu(20, 0, "员工管理", 1),
                menu(30, 0, "考勤管理", 1),
                menu(330, 30, "排班管理", 1),
                menu(350, 30, "添加排班", 1),
                menu(50, 0, "薪资管理", 1),
                menu(500, 50, "薪资管理", 1),
                menu(510, 50, "薪资档案", 1),
                menu(60, 0, "奖金中心", 1),
                menu(600, 60, "上传奖金(累加至工资计税)", 1),
                menu(601, 60, "上传奖金(只计税)", 1),
                menu(70, 0, "数据配置", 1),
                menu(700, 70, "数据配置", 1)
        );

        List<tbmenu> tree = support.buildAuthorizedMenuTree(menus, Arrays.asList(20, 30, 330, 60, 600, 601));

        Assert.assertEquals(3, tree.size());
        Assert.assertEquals("员工管理", tree.get(0).getName());
        Assert.assertEquals("考勤管理", tree.get(1).getName());
        Assert.assertEquals(1, tree.get(1).getChildren().size());
        Assert.assertEquals("排班管理", tree.get(1).getChildren().get(0).getName());
        Assert.assertEquals("奖金中心", tree.get(2).getName());
        Assert.assertEquals(2, tree.get(2).getChildren().size());
        Assert.assertEquals("上传奖金(累加至工资计税)", tree.get(2).getChildren().get(0).getName());
        Assert.assertEquals("上传奖金(只计税)", tree.get(2).getChildren().get(1).getName());
    }

    private static TbRoleMenu roleMenu(Integer roleId, Integer menuId) {
        TbRoleMenu roleMenu = new TbRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private static tbmenu menu(Integer id, Integer pid, String name, Integer canUse) {
        tbmenu menu = new tbmenu();
        menu.setId(id);
        menu.setPid(pid);
        menu.setName(name);
        menu.setCanUse(canUse);
        return menu;
    }
}
