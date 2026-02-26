package com.tianye.hrsystem.modules.insurance.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsurancePageListBO;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsuranceRecordListVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthRecordService;

@RestController
@RequestMapping("/hrmInsuranceMonthRecord")
public class HrmInsuranceMonthRecordController {
    @Autowired
    private HrmInsuranceMonthRecordService insuranceMonthRecordService;

    @PostMapping("/computeInsuranceData")
    @ApiOperation("核算社保数据")
    public Result computeInsuranceData() {
        try {
            JSONObject data = insuranceMonthRecordService.computeInsuranceData();
            return Result.OK(data.getString("year"));
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
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
