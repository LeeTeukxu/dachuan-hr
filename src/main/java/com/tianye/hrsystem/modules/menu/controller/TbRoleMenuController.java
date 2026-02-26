package com.tianye.hrsystem.modules.menu.controller;

import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.modules.menu.bo.QueryRoleMenuBO;
import com.tianye.hrsystem.modules.menu.service.TbRoleMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tbRoleMenu")
@Api(tags = "菜单")
public class TbRoleMenuController {
    @Autowired
    TbRoleMenuService tbRoleMenuService;

    @PostMapping("/saveRoleMenuList")
    @ApiOperation("保存角色菜单")
    public Result saveRoleMenuList(@RequestBody QueryRoleMenuBO queryRoleMenuBO) {
        try {
            Integer result = tbRoleMenuService.saveRoleMenuList(queryRoleMenuBO);
            return Result.ok(result);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"保存失败");
        }
    }

    @PostMapping("/getRoleMenu")
    @ApiOperation("返回角色菜单")
    public Result getRoleMenu(@RequestBody QueryRoleMenuBO queryRoleMenuBO) {
        try {
            List<Map<String, Object>> findMenuName =  tbRoleMenuService.getRoleMenu(queryRoleMenuBO);
            return Result.ok(findMenuName);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"获取失败");
        }
    }

    @PostMapping("/getLoginRoleMenu")
    @ApiOperation("返回登陆角色的菜单")
    public Result getLoginRoleMenu(@RequestBody QueryRoleMenuBO queryRoleMenuBO) {
        try {
            List<String> findMenuName = tbRoleMenuService.getLoginRoleMenu(queryRoleMenuBO);
            return Result.ok(findMenuName);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"获取失败");
        }
    }
}
