package com.tianye.hrsystem.modules.menu.service;

import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.menu.bo.QueryMenuBO;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TbMenuServiceTest {

    @Test
    public void queryMenuList_shouldAttachChildren_whenParentIdExceedsIntegerCache() {
        TbMenuService service = new TbMenuService() {
            @Override
            public List<tbmenu> queryAllMenus() {
                return Arrays.asList(
                        menu(1010, 0, "系统设置"),
                        menu(1011, 1010, "假期管理")
                );
            }
        };

        List<tbmenu> tree = service.queryMenuList(new QueryMenuBO());

        Assert.assertEquals(1, tree.size());
        Assert.assertEquals(Integer.valueOf(1010), tree.get(0).getId());
        Assert.assertEquals(1, tree.get(0).getChildren().size());
        Assert.assertEquals(Integer.valueOf(1011), tree.get(0).getChildren().get(0).getId());
    }

    @Test
    public void queryMenuList_shouldKeepThirdLevelNode() {
        TbMenuService service = new TbMenuService() {
            @Override
            public List<tbmenu> queryAllMenus() {
                return Arrays.asList(
                        menu(1000, 0, "权限管理"),
                        menu(5000, 1000, "排班小程序"),
                        menu(5020, 5000, "排班数据加载")
                );
            }
        };

        List<tbmenu> tree = service.queryMenuList(new QueryMenuBO());

        Assert.assertEquals(1, tree.size());
        tbmenu parent = tree.get(0);
        Assert.assertEquals(Integer.valueOf(1000), parent.getId());
        Assert.assertEquals(1, parent.getChildren().size());
        tbmenu miniapp = parent.getChildren().get(0);
        Assert.assertEquals(Integer.valueOf(5000), miniapp.getId());
        Assert.assertNotNull(miniapp.getChildren());
        Assert.assertEquals(1, miniapp.getChildren().size());
        Assert.assertEquals(Integer.valueOf(5020), miniapp.getChildren().get(0).getId());
    }

    private static tbmenu menu(Integer id, Integer pid, String name) {
        tbmenu menu = new tbmenu();
        menu.setId(id);
        menu.setPid(pid);
        menu.setName(name);
        menu.setCanUse(1);
        return menu;
    }
}
