package com.tianye.hrsystem.modules.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.salary.dto.QuerySendRecordListDto;
import com.tianye.hrsystem.modules.salary.dto.QuerySlipEmployeePageListDto;
import com.tianye.hrsystem.modules.salary.dto.SendSalarySlipDto;
import com.tianye.hrsystem.modules.salary.dto.SmsUpDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlipOption;
import com.tianye.hrsystem.modules.salary.entity.SlipEmployeeVO;
import com.tianye.hrsystem.modules.salary.service.HrmSalarySlipRecordService;
import com.tianye.hrsystem.modules.salary.vo.QuerySendRecordListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *发工资条记录 前端控制器
 */
@RestController
@RequestMapping("/hrmSalarySlipRecord")
@Api(tags = "工资条-工资条记录接口")
public class HrmSalarySlipRecordController
{
    @Autowired
    private HrmSalarySlipRecordService slipRecordService;

    @PostMapping("/querySlipEmployeePageList")
    @ApiOperation("查询工资条选择发送员工列表")
    public Result<Page<SlipEmployeeVO>> querySlipEmployeePageList(@RequestBody QuerySlipEmployeePageListDto slipEmployeePageListBO) {
        Page<SlipEmployeeVO> page = slipRecordService.querySlipEmployeePageList(slipEmployeePageListBO);
        return Result.ok(page);
    }

    @PostMapping("/sendSalarySlip")
    @ApiOperation("发工资条")
    public Result sendSalarySlip(@RequestBody SendSalarySlipDto sendSalarySlipBO)
    {
        OperationLog operationLog = slipRecordService.sendSalarySlip(sendSalarySlipBO);
        return Result.ok(operationLog);
    }

    @PostMapping("/smsUpModify")
    @ApiOperation("短信上行确认工资")
    public Result SmsUpModify(@RequestBody SmsUpDto smsUpDto){
        OperationLog operationLog = slipRecordService.SmsUpModify(smsUpDto);
        return Result.ok(operationLog);
    }

//    @PostMapping("/querySendRecordList")
//    @ApiOperation("查询发放工资条记录列表")
//    public Result<Page<QuerySendRecordListVO>> querySendRecordList(@RequestBody QuerySendRecordListDto querySendRecordListBO) {
//        Page<QuerySendRecordListVO> page = slipRecordService.querySendRecordList(querySendRecordListBO);
//        return Result.ok(page);
//    }
//
//    @PostMapping("/deleteSendRecord/{id}")
//    @ApiOperation("删除发放记录")
//    @OperateLog(behavior = BehaviorEnum.DELETE, apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_SALARY_SLIP_RECORD)
//    public Result deleteSendRecord(@PathVariable("id") String id) {
//        OperationLog operationLog = slipRecordService.deleteSendRecord(id);
//        return OperationResult.ok(operationLog);
//    }
//
//    @PostMapping("/querySendDetailList")
//    @ApiOperation("查询发放工资条记录详情列表")
//    public Result<BasePage<QuerySendDetailListVO>> querySendDetailList(@RequestBody QuerySendDetailListBO querySendRecordListBO) {
//        BasePage<QuerySendDetailListVO> page = slipRecordService.querySendDetailList(querySendRecordListBO);
//        return Result.ok(page);
//    }

    @PostMapping("/querySlipDetail/{id}")
    @ApiOperation("查询工资条明细")
    public Result<List<HrmSalarySlipOption>> querySlipDetail(@PathVariable("id") Long id) {
        List<HrmSalarySlipOption> list = slipRecordService.querySlipDetail(id);
        return Result.ok(list);
    }

//    @PostMapping("/setSlipRemarks")
//    @ApiOperation("添加修改工资条备注")
//    public Result setSlipRemarks(@RequestBody SetSlipRemarksBO setSlipRemarksBO) {
//        slipRecordService.setSlipRemarks(setSlipRemarksBO);
//        return Result.ok();
//    }
}
