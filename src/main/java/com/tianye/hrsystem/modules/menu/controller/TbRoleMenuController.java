package com.tianye.hrsystem.modules.menu.controller;

import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.modules.menu.bo.QueryRoleMenuBO;
import com.tianye.hrsystem.modules.menu.service.TbRoleMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tbRoleMenu")
@Api(tags = "菜单")
public class TbRoleMenuController {
    @Autowired
    TbRoleMenuService tbRoleMenuService;
    @Autowired
    LoginUserMapper loginUserMapper;
    @Value("${hrm.system.database}")
    String systemBase;

    @PostMapping("/saveRoleMenuList")
    @ApiOperation("保存角色菜单")
    public Result saveRoleMenuList(@RequestBody QueryRoleMenuBO queryRoleMenuBO) {
        try {
            Integer result = tbRoleMenuService.saveRoleMenuList(queryRoleMenuBO);
            return Result.ok(result);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),ax.getMessage());
        }
    }

    @GetMapping("/companies")
    @ApiOperation("获取所有企业列表")
    public Result companies() {
        try {
            List<Map<String, Object>> list = loginUserMapper.getAllCompanies(systemBase);
            return Result.ok(list);
        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(), "查询企业列表失败: " + e.getMessage());
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
