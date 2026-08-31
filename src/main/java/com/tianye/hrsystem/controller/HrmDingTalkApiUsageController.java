package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.entity.vo.DingTalkApiUsageVO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.IHrmDingTalkApiUsageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dingTalkApiUsage")
@Api(tags = "钉钉API调用量")
public class HrmDingTalkApiUsageController {

    @Autowired
    private IHrmDingTalkApiUsageService dingTalkApiUsageService;

    @PostMapping("/monthly")
    @ApiOperation("查询本月钉钉API调用量")
    public Result<DingTalkApiUsageVO> queryMonthlyUsage() {
        return Result.ok(dingTalkApiUsageService.queryMonthlyUsage());
    }
}
