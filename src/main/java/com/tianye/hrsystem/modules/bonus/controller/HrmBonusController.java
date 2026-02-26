package com.tianye.hrsystem.modules.bonus.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.service.HrmBonusService;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrmBonus")
@Api(tags = "奖金")
public class HrmBonusController {

    @Autowired
    HrmBonusService hrmBonusService;

    /**
     * 导入个税专项附加报表数据
     * @param
     * @return
     */
    @PostMapping("/importBonus")
    @ApiOperation(value = "导入奖金数据")
    public Result importBonus(@ApiParam("奖金") @RequestParam(name = "bonusFile", required = false) MultipartFile bonusFile,
                                   @RequestParam(name = "year") String year,
                                   @RequestParam(name = "month") String month) {
        try {
            hrmBonusService.resolveBonusData(bonusFile, year, month);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    @PostMapping("/queryBounsList")
    @ApiOperation("查询奖金列表")
    public Result<Page<QueryBounsVO>> queryBounsList(@RequestBody QueryBonusBO queryBonusBO) {
        try {
            Page<QueryBounsVO> page = hrmBonusService.queryHrmBonusList(queryBonusBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }
}
