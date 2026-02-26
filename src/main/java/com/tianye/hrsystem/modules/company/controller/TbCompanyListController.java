package com.tianye.hrsystem.modules.company.controller;

import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.company.service.TbCompanyListService;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import com.tianye.hrsystem.modules.menu.service.TbRoleMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tbCompanyList")
@Api(tags = "公司列表")
public class TbCompanyListController {
    @Autowired
    TbCompanyListService tbCompanyListService;

    @PostMapping("/getCompanyList")
    @ApiOperation("获取所有公司")
    public Result getCompanyList() {
        try {
            List<QueryCompanyListVO> result = tbCompanyListService.getCompanyList();
            return Result.ok(result);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"获取失败");
        }
    }
}
