package com.tianye.hrsystem.modules.insurance.controller;


import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.vo.InsuranceSchemeVO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceSchemeService;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceSchemeListVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/hrmInsuranceScheme")
@CrossOrigin
public class HrmInsuranceSchemeController {

    @Autowired
    private HrmInsuranceSchemeService insuranceSchemeService;

    @PostMapping("/index")
    @ApiOperation("查询参保方案列表")
    public Result<BasePage<InsuranceSchemeListVO>> index(@RequestBody PageEntity pageEntity) {
        BasePage<InsuranceSchemeListVO> page = insuranceSchemeService.index(pageEntity);
        return Result.ok(page);
    }

    @PostMapping("/queryInsuranceSchemeById/{schemeId}")
    @ApiOperation("查询参保方案详情")
    public Result<InsuranceSchemeVO> queryInsuranceSchemeById(@PathVariable("schemeId") Long schemeId) {
        InsuranceSchemeVO insuranceSchemeVO = insuranceSchemeService.queryInsuranceSchemeById(schemeId);
        return Result.ok(insuranceSchemeVO);
    }

    @PostMapping("/deleteInsuranceScheme/{schemeId}")
    public Result deleteInsuranceScheme(@PathVariable Long schemeId) {
        insuranceSchemeService.deleteInsuranceScheme(schemeId);
        return Result.ok();
    }
}

