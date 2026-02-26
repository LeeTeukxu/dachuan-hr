package com.tianye.hrsystem.modules.menu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.menu.bo.QueryMenuBO;
import com.tianye.hrsystem.modules.menu.service.TbMenuService;
import com.tianye.hrsystem.modules.menu.vo.QueryMenuVO;
import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.service.TbRoleTypesService;
import com.tianye.hrsystem.modules.role.vo.QueryRoleTypesVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tbMenu")
@Api(tags = "菜单")
public class TbMenuController {
    @Autowired
    TbMenuService tbMenuService;

    @PostMapping("/queryMenuList")
    @ApiOperation("查询菜单列表")
    public Result queryMenuList(@RequestBody QueryMenuBO queryMenuBO) {
        try {
//            List<QueryMenuVO> page = tbMenuService.queryMenuList(queryMenuBO);
            List<tbmenu> page = tbMenuService.queryMenuList(queryMenuBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }
}
