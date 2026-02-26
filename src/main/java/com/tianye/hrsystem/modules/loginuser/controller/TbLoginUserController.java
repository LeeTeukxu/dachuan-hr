package com.tianye.hrsystem.modules.loginuser.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import com.tianye.hrsystem.modules.loginuser.service.TbLoginUserService;
import com.tianye.hrsystem.modules.loginuser.vo.QueryLoginUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/tbLoginUser")
@Api(tags = "登陆用户")
public class TbLoginUserController {
    @Autowired
    TbLoginUserService tbLoginUserService;

    @PostMapping("/queryLoginUserList")
    @ApiOperation("查询登陆用户列表")
    public Result queryLoginUserList(@RequestBody QueryLoginUserBO queryLoginUserBO) {
        try {
            Page<QueryLoginUserVO> page = tbLoginUserService.queryLoginUserList(queryLoginUserBO);
            return Result.ok(page);
        }catch (Exception ax) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"获取失败");
        }
    }

    @PostMapping("/Add")
    @ApiOperation("添加登陆用户")
    public Result Add(@RequestBody QueryLoginUserBO queryLoginUserBO) {
        try {
            Integer result = tbLoginUserService.Add(queryLoginUserBO);
            return Result.ok(result);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"保存失败");
        }
    }
}
