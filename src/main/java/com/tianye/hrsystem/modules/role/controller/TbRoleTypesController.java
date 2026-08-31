package com.tianye.hrsystem.modules.role.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
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

@RestController
@RequestMapping("/tbRoleTypes")
@Api(tags = "角色")
public class TbRoleTypesController {
    @Autowired
    TbRoleTypesService tbRoleTypesService;

    @PostMapping("/queryRoleTypesList")
    @ApiOperation("查询角色列表")
    public Result<Page<QueryRoleTypesVO>> queryRoleTypesList(@RequestBody QueryRoleTypesBO queryRoleTypesBO) {
        try {
            Page<QueryRoleTypesVO> page = tbRoleTypesService.queryRoleTypesList(queryRoleTypesBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/saveRoleType")
    @ApiOperation("保存角色")
    public Result saveRoleType(@RequestBody QueryRoleTypesBO queryRoleTypesBO) {
        try {
            Integer result = tbRoleTypesService.saveRoleType(queryRoleTypesBO);
            return Result.OK(result);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/deleteRoleType")
    @ApiOperation("删除角色")
    public Result deleteRoleType(@RequestBody QueryRoleTypesBO queryRoleTypesBO) {
        try {
            Integer result = tbRoleTypesService.deleteRoleType(queryRoleTypesBO.getId());
            return Result.OK(result);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }
}
