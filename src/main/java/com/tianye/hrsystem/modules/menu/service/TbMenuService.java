package com.tianye.hrsystem.modules.menu.service;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.menu.bo.QueryMenuBO;
import com.tianye.hrsystem.modules.menu.entity.TbMenu;
import com.tianye.hrsystem.modules.menu.mapper.TbMenuMapper;
import com.tianye.hrsystem.modules.menu.vo.QueryMenuVO;
import com.tianye.hrsystem.repository.tbmenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TbMenuService extends BaseServiceImpl<TbMenuMapper, TbMenu> {
    @Autowired
    TbMenuMapper tbMenuMapper;

    @Autowired
    tbmenuRepository menuRepository;

    @Autowired
    MenuPermissionSupport menuPermissionSupport;

    public List<tbmenu> queryAllMenus() {
        return menuRepository.findAll();
    }

    public List<tbmenu> queryMenuList(@RequestBody QueryMenuBO queryMenuBO) {
        List<tbmenu> listMenus = queryAllMenus();
        List<tbmenu> listResult = new ArrayList<>();
         if (listMenus.size() > 0) {
            List<tbmenu> listParents = listMenus.stream().filter(f -> f.getPid() == 0).collect(Collectors.toList());
            if (listParents.size() > 0) {
                Set<Integer> visited = new HashSet<>();
                listParents.forEach(f -> {
                    tbmenu parentMenu = f;
                    // 递归挂载子孙：原实现只挂一层子节点，三级节点（如 权限管理>排班小程序>排班数据加载）会丢失
                    List<tbmenu> listChildren = buildChildren(listMenus, f.getId(), visited);
                    if (listChildren.size() > 0) {
                        parentMenu.setChildren(listChildren);
                    }
                    listResult.add(parentMenu);
                });
            }
        }

//        return tbMenuMapper.queryMenuList(queryMenuBO);
        return listResult;
    }

    /**
     * 递归构建指定父节点的子节点树，支持任意层级。
     * 注意：pid/id 均为 Integer，必须用 Objects.equals 比较（超出 -128~127 缓存时 == 会误判）。
     * visited 防止脏数据 pid 成环导致无限递归。
     */
    private List<tbmenu> buildChildren(List<tbmenu> listMenus, Integer parentId, Set<Integer> visited) {
        List<tbmenu> listChildrenResult = new ArrayList<>();
        for (tbmenu menu : listMenus) {
            if (!Objects.equals(menu.getPid(), parentId)) {
                continue;
            }
            Integer id = menu.getId();
            if (id != null) {
                if (visited.contains(id)) {
                    continue;
                }
                visited.add(id);
            }
            if (id != null) {
                List<tbmenu> grandChildren = buildChildren(listMenus, id, visited);
                if (grandChildren.size() > 0) {
                    menu.setChildren(grandChildren);
                }
            }
            listChildrenResult.add(menu);
        }
        return listChildrenResult;
    }
}
