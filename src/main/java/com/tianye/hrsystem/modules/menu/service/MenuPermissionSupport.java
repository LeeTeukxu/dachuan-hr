package com.tianye.hrsystem.modules.menu.service;

import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MenuPermissionSupport {

    public List<TbRoleMenu> normalizeRoleMenus(Integer roleId, List<TbRoleMenu> selectedRoleMenus, List<tbmenu> allMenus) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        Map<Integer, tbmenu> menuMap = toMenuMap(allMenus);
        LinkedHashSet<Integer> selectedIds = new LinkedHashSet<>();
        if (selectedRoleMenus != null) {
            for (TbRoleMenu roleMenu : selectedRoleMenus) {
                if (roleMenu != null && roleMenu.getMenuId() != null) {
                    selectedIds.add(roleMenu.getMenuId());
                }
            }
        }
        requireHasPageMenuIds(selectedIds, menuMap, "角色必须至少配置一个启用的子菜单权限");
        requireNoParentOnlyMenus(selectedIds, menuMap);

        LinkedHashSet<Integer> normalizedIds = new LinkedHashSet<>();
        for (Integer menuId : selectedIds) {
            tbmenu menu = menuMap.get(menuId);
            if (!isEnabled(menu)) {
                continue;
            }
            addParentMenus(menu, menuMap, normalizedIds);
            normalizedIds.add(menuId);
        }

        return sortByMenuOrder(normalizedIds, allMenus).stream().map(menuId -> {
            TbRoleMenu roleMenu = new TbRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            return roleMenu;
        }).collect(Collectors.toList());
    }

    public void requireRoleHasEnabledSubMenu(Integer roleId, List<TbRoleMenu> roleMenus, List<tbmenu> allMenus) {
        Collection<Integer> menuIds = roleMenus == null ? Collections.emptyList() : roleMenus.stream()
                .filter(Objects::nonNull)
                .map(TbRoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        requireRoleHasEnabledSubMenuIds(roleId, menuIds, allMenus);
    }

    public void requireRoleHasEnabledSubMenuIds(Integer roleId, Collection<Integer> menuIds, List<tbmenu> allMenus) {
        if (roleId == null) {
            throw new IllegalArgumentException("登录用户必须绑定角色");
        }
        requireHasPageMenuIds(menuIds, toMenuMap(allMenus), "该账号绑定的角色未配置菜单权限");
    }

    public List<String> buildAuthorizedMenuNames(List<tbmenu> allMenus, Collection<Integer> allowedMenuIds) {
        Set<Integer> allowedIds = allowedMenuIds == null ? Collections.emptySet() : new HashSet<>(allowedMenuIds);
        return allMenus == null ? Collections.emptyList() : allMenus.stream()
                .filter(menu -> allowedIds.contains(menu.getId()))
                .filter(this::isEnabled)
                .map(tbmenu::getName)
                .collect(Collectors.toList());
    }

    public List<tbmenu> buildAuthorizedMenuTree(List<tbmenu> allMenus, Collection<Integer> allowedMenuIds) {
        if (allMenus == null || allMenus.isEmpty() || allowedMenuIds == null || allowedMenuIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> allowedIds = new HashSet<>(allowedMenuIds);
        List<tbmenu> result = new ArrayList<>();
        for (tbmenu module : allMenus) {
            if (!isRoot(module) || !isEnabled(module) || !allowedIds.contains(module.getId())) {
                continue;
            }
            List<tbmenu> children = buildAuthorizedChildren(allMenus, module.getId(), allowedIds);
            if (!children.isEmpty()) {
                tbmenu copiedModule = copyMenu(module);
                copiedModule.setChildren(children);
                result.add(copiedModule);
            } else if (isRootLeafPage(module, allMenus)) {
                result.add(copyMenu(module));
            }
        }
        return result;
    }

    /**
     * 递归构建「已授权」的子菜单，支持任意层级。
     * 原实现只挂一层子节点，三级节点（如 权限管理>排班小程序>排班数据加载）会丢失，
     * 导致 CompanyInterceptor 按 path 鉴权时匹配不到而被拒绝。
     * 规则：只保留 allowedIds 内且启用的节点；自身没有已授权子孙时，若是叶子页面则保留，否则整支剪掉（避免空壳父模块）。
     */
    private List<tbmenu> buildAuthorizedChildren(List<tbmenu> allMenus, Integer parentId, Set<Integer> allowedIds) {
        List<tbmenu> children = new ArrayList<>();
        if (allMenus == null) {
            return children;
        }
        for (tbmenu menu : allMenus) {
            if (!Objects.equals(menu.getPid(), parentId) || !isEnabled(menu) || !allowedIds.contains(menu.getId())) {
                continue;
            }
            List<tbmenu> grandChildren = buildAuthorizedChildren(allMenus, menu.getId(), allowedIds);
            tbmenu copied = copyMenu(menu);
            if (!grandChildren.isEmpty()) {
                copied.setChildren(grandChildren);
            }
            children.add(copied);
        }
        return children;
    }

    private void requireHasPageMenuIds(Collection<Integer> menuIds, Map<Integer, tbmenu> menuMap, String message) {
        boolean hasPageMenu = menuIds != null && menuIds.stream()
                .map(menuMap::get)
                .anyMatch(menu -> menu != null && isEnabled(menu) && (!isRoot(menu) || isRootLeafPage(menu, new ArrayList<>(menuMap.values()))));
        if (!hasPageMenu) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireNoParentOnlyMenus(Collection<Integer> selectedIds, Map<Integer, tbmenu> menuMap) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return;
        }
        Collection<tbmenu> allMenus = menuMap.values();
        for (Integer menuId : selectedIds) {
            tbmenu menu = menuMap.get(menuId);
            if (!isEnabled(menu) || !hasChild(menu, allMenus)) {
                continue;
            }
            if (!hasSelectedPageDescendant(menu, selectedIds, allMenus)) {
                throw new IllegalArgumentException("父级菜单不能单独授权，请至少勾选一个子菜单权限");
            }
        }
    }

    private boolean hasSelectedPageDescendant(tbmenu parent, Collection<Integer> selectedIds, Collection<tbmenu> allMenus) {
        for (tbmenu menu : allMenus) {
            if (menu == null || !Objects.equals(menu.getPid(), parent.getId()) || !isEnabled(menu)) {
                continue;
            }
            if (!hasChild(menu, allMenus) && selectedIds.contains(menu.getId())) {
                return true;
            }
            if (hasSelectedPageDescendant(menu, selectedIds, allMenus)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasChild(tbmenu parent, Collection<tbmenu> allMenus) {
        return parent != null && allMenus != null && allMenus.stream()
                .anyMatch(item -> item != null && Objects.equals(item.getPid(), parent.getId()));
    }

    private void addParentMenus(tbmenu menu, Map<Integer, tbmenu> menuMap, LinkedHashSet<Integer> normalizedIds) {
        Integer pid = menu.getPid();
        if (pid == null || pid == 0) {
            return;
        }
        tbmenu parent = menuMap.get(pid);
        if (isEnabled(parent)) {
            addParentMenus(parent, menuMap, normalizedIds);
            normalizedIds.add(parent.getId());
        }
    }

    private List<Integer> sortByMenuOrder(Set<Integer> menuIds, List<tbmenu> allMenus) {
        if (allMenus == null) {
            return new ArrayList<>(menuIds);
        }
        List<Integer> ordered = allMenus.stream()
                .map(tbmenu::getId)
                .filter(menuIds::contains)
                .collect(Collectors.toList());
        for (Integer menuId : menuIds) {
            if (!ordered.contains(menuId)) {
                ordered.add(menuId);
            }
        }
        return ordered;
    }

    private Map<Integer, tbmenu> toMenuMap(List<tbmenu> allMenus) {
        Map<Integer, tbmenu> menuMap = new HashMap<>();
        if (allMenus != null) {
            for (tbmenu menu : allMenus) {
                if (menu != null && menu.getId() != null) {
                    menuMap.put(menu.getId(), menu);
                }
            }
        }
        return menuMap;
    }

    private boolean isRoot(tbmenu menu) {
        return menu != null && (menu.getPid() == null || menu.getPid() == 0);
    }

    private boolean isRootLeafPage(tbmenu menu, List<tbmenu> allMenus) {
        if (!isRoot(menu) || allMenus == null) {
            return false;
        }
        return allMenus.stream().noneMatch(item -> item != null && Objects.equals(item.getPid(), menu.getId()));
    }

    private boolean isEnabled(tbmenu menu) {
        return menu != null && !Integer.valueOf(2).equals(menu.getCanUse());
    }

    private tbmenu copyMenu(tbmenu source) {
        tbmenu target = new tbmenu();
        target.setId(source.getId());
        target.setPid(source.getPid());
        target.setName(source.getName());
        target.setPath(source.getPath());
        target.setCanUse(source.getCanUse());
        target.setComponent(source.getComponent());
        target.setRedirect(source.getRedirect());
        target.setIcon(source.getIcon());
        target.setShowMenu(source.getShowMenu());
        return target;
    }
}
