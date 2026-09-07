package com.tianye.hrsystem.modules.insurance.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsurancePageListBO;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceSalaryBasicAmountBO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsuranceRecordListVO;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceComputeProgressVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthRecordService;

@RestController
@RequestMapping("/hrmInsuranceMonthRecord")
public class HrmInsuranceMonthRecordController {
    @Autowired
    private HrmInsuranceMonthRecordService insuranceMonthRecordService;

    @PostMapping("/getSuggestMonth")
    @ApiOperation("查询建议生成的社保报表年月")
    public Result getSuggestMonth() {
        try {
            JSONObject data = insuranceMonthRecordService.getSuggestMonth();
            return Result.OK(data);
        } catch (Exception e) {
            return Result.Error(e);
        }
    }

    @PostMapping("/computeInsuranceData")
    @ApiOperation("核算社保数据")
    public Result computeInsuranceData(@RequestBody(required = false) JSONObject body) {
        try {
            Integer year = null;
            Integer month = null;
            if (body != null) {
                year = body.getInteger("year");
                month = body.getInteger("month");
            }
            JSONObject data = insuranceMonthRecordService.computeInsuranceData(year, month);
            return Result.OK(data.getString("year"));
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.Error(ax);
        }
    }

    @PostMapping("/queryComputeInsuranceProgress")
    @ApiOperation("查询社保报表生成进度")
    public Result<InsuranceComputeProgressVO> queryComputeInsuranceProgress() {
        InsuranceComputeProgressVO progress = insuranceMonthRecordService.queryComputeInsuranceProgress();
        return Result.OK(progress);
    }

    @PostMapping("/queryInsuranceRecordList")
    @ApiOperation("查询社保统计数据列表")
    public Result<Page<QueryInsuranceRecordListVO>> queryInsuranceRecordList(@RequestBody QueryInsuranceRecordListBO recordListBO) {
        Page<QueryInsuranceRecordListVO> page = insuranceMonthRecordService.queryInsuranceRecordList(recordListBO);
        return Result.OK(page);
    }

    @PostMapping("/queryInsuranceRecordList/{iRecordId}")
    @ApiOperation("查询社保详情统计数据(详情统计)")
        public Result<QueryInsuranceRecordListVO> queryInsuranceRecord(@PathVariable("iRecordId") String iRecordId) {
        QueryInsuranceRecordListVO data = insuranceMonthRecordService.queryInsuranceRecord(iRecordId);
        return Result.OK(data);
    }

    @PostMapping("/queryInsurancePageList")
    @ApiOperation("查询社保数据列表")
    public Result<Page<QueryInsurancePageListVO>> queryInsurancePageList(@RequestBody QueryInsurancePageListBO queryInsurancePageListBO) {
        Page<QueryInsurancePageListVO> page = insuranceMonthRecordService.queryInsurancePageList(queryInsurancePageListBO);
        return Result.OK(page);
    }

    @PostMapping("/updateSalaryBasicInsuranceAmount")
    @ApiOperation("设置或取消累计基本工资保险金额")
    public Result<Integer> updateSalaryBasicInsuranceAmount(@RequestBody UpdateInsuranceSalaryBasicAmountBO updateBO) {
        int updatedCount = insuranceMonthRecordService.updateSalaryBasicInsuranceAmount(updateBO);
        return Result.OK(updatedCount);
    }

    @PostMapping("/deleteInsurance/{iRecordId}")
    @ApiOperation("删除社保记录")
//    @OperateLog(apply = ApplyEnum.HRM, behavior = BehaviorEnum.DELETE, object = OperateObjectEnum.HRM_INSURANCE_SCHEME)
    public Result deleteInsurance(@PathVariable("iRecordId") Long iRecordId) {
        try {
            insuranceMonthRecordService.deleteInsurance(iRecordId);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }
}
