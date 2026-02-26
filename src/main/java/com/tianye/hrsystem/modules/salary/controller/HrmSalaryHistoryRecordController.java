package com.tianye.hrsystem.modules.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.salary.dto.QueryHistorySalaryDetailDto;
import com.tianye.hrsystem.modules.salary.dto.QueryHistorySalaryListDto;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryMonthRecordService;
import com.tianye.hrsystem.modules.salary.vo.QueryHistorySalaryDetailVO;
import com.tianye.hrsystem.modules.salary.vo.QueryHistorySalaryListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hrmSalaryHistoryRecord")
@Api(tags = "薪资管理-历史薪资")
public class HrmSalaryHistoryRecordController {

    @Autowired
    private HrmSalaryMonthRecordService salaryMonthRecordService;

    @PostMapping("/queryHistorySalaryList")
    @ApiOperation("查询历史薪资列表")
    public Result<Page<QueryHistorySalaryListVO>> queryHistorySalaryList(@RequestBody QueryHistorySalaryListDto queryHistorySalaryListDto) {
        Page<QueryHistorySalaryListVO> page = salaryMonthRecordService.queryHistorySalaryList(queryHistorySalaryListDto);
        return Result.ok(page);
    }

    @PostMapping("/queryHistorySalaryDetail")
    @ApiOperation("查询历史薪资详情")
    public Result<QueryHistorySalaryDetailVO> queryHistorySalaryDetail(@RequestBody QueryHistorySalaryDetailDto queryHistorySalaryDetailDto) {
        QueryHistorySalaryDetailVO historySalaryDetail = salaryMonthRecordService.queryHistorySalaryDetail(queryHistorySalaryDetailDto);
        return Result.ok(historySalaryDetail);
    }
}
