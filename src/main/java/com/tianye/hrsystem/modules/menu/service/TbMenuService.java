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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TbMenuService extends BaseServiceImpl<TbMenuMapper, TbMenu> {
    @Autowired
    TbMenuMapper tbMenuMapper;

    @Autowired
    tbmenuRepository menuRepository;

    public List<tbmenu> queryMenuList(@RequestBody QueryMenuBO queryMenuBO) {
        List<tbmenu> listMenus = menuRepository.findAll();
        List<tbmenu> listResult = new ArrayList<>();
         if (listMenus.size() > 0) {
            List<tbmenu> listParents = listMenus.stream().filter(f -> f.getPid() == 0).collect(Collectors.toList());
            if (listParents.size() > 0) {
                listParents.forEach(f -> {
                    tbmenu parentMenu = new tbmenu();
                    parentMenu = f;
                    List<tbmenu> listChildren = listMenus.stream().filter(x -> x.getPid() == f.getId()).collect(Collectors.toList());
                    if (listChildren.size() > 0) {
                        List<tbmenu> listChildrenResult = new ArrayList<>();
                        listChildren.forEach(x -> {
                            tbmenu childrenMenu = new tbmenu();
                            childrenMenu = x;
                            listChildrenResult.add(childrenMenu);
                        });
                        parentMenu.setChildren(listChildrenResult);
                    }
                    listResult.add(parentMenu);
                });
            }
        }

//        return tbMenuMapper.queryMenuList(queryMenuBO);
        return listResult;
    }
}
