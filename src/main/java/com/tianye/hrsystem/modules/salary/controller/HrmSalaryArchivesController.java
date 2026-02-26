package com.tianye.hrsystem.modules.salary.controller;

import com.github.pagehelper.PageInfo;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.salary.dto.*;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryArchivesService;
import com.tianye.hrsystem.modules.salary.vo.QueryChangeOptionValueVO;
import com.tianye.hrsystem.modules.salary.vo.QueryChangeRecordListVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryArchivesByIdVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryArchivesListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hrmSalaryArchives")
@Api(tags = "薪资档案")
public class HrmSalaryArchivesController
{

    @Autowired
    private HrmSalaryArchivesService hrmSalaryArchivesService;

    @PostMapping("/querySalaryArchivesList")
    @ApiOperation("查询薪资档案列表")
    public Result<BasePage<QuerySalaryArchivesListVO>> querySalaryArchivesList(@RequestBody QuerySalaryArchivesListDto querySalaryArchivesListDto)
    {
        BasePage<QuerySalaryArchivesListVO> page = hrmSalaryArchivesService.querySalaryArchivesList(querySalaryArchivesListDto);
        return Result.ok(page);
    }

    @PostMapping("/querySalaryArchivesById/{employeeId}")
    @ApiOperation("查询薪资档案信息")
    public Result<QuerySalaryArchivesByIdVO> querySalaryArchivesById(@PathVariable("employeeId") Long employeeId) {
        QuerySalaryArchivesByIdVO querySalaryArchivesByIdVO = hrmSalaryArchivesService.querySalaryArchivesById(employeeId);
        return Result.ok(querySalaryArchivesByIdVO);
    }

    @PostMapping("/queryChangeOptionByTemplateId")
    @ApiOperation("查询调薪项的值(单个调薪使用)")
    public Result<QueryChangeOptionValueVO> queryChangeOptionValue(@RequestBody QueryChangeOptionValueDto changeOptionValueBO) {
        QueryChangeOptionValueVO data = hrmSalaryArchivesService.queryChangeOptionValue(changeOptionValueBO);
        return Result.ok(data);
    }

    @PostMapping("/setFixSalaryRecord")
    @ApiOperation("单个定薪")
    public Result setFixSalaryRecord(@RequestBody SetFixSalaryRecordDto setFixSalaryRecordBO)
    {
        try
        {
            List<SetFixSalaryRecordDto> list = new ArrayList<>();
            list.add(setFixSalaryRecordBO);
            Integer result = hrmSalaryArchivesService.setFixSalaryRecord(list);
            return Result.ok(result);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"保存失败");
        }

    }

    @PostMapping("/batchSetFixSalaryRecord")
    @ApiOperation("批量定薪")
    public Result batchSetFixSalaryRecord(@RequestBody List<SetFixSalaryRecordDto> setFixSalaryRecordDtoList)
    {
        try
        {
            Integer result = hrmSalaryArchivesService.setFixSalaryRecord(setFixSalaryRecordDtoList);
            return Result.ok(result);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"保存失败");
        }

    }

    @PostMapping("/setChangeSalaryRecord")
    @ApiOperation("单个调薪")
    public Result setChangeSalaryRecord(@RequestBody SetChangeSalaryRecordDto setChangeSalaryRecordDto) {
        try
        {
            Integer result = hrmSalaryArchivesService.setChangeSalaryRecord(setChangeSalaryRecordDto);
            return Result.ok(result);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"保存失败");
        }
    }

    @PostMapping("/queryChangeRecordList/{employeeId}")
    @ApiOperation("查询员工调薪记录列表")
    public Result<List<QueryChangeRecordListVO>> queryChangeRecordList(@PathVariable("employeeId") Long employeeId) {
        List<QueryChangeRecordListVO> list = hrmSalaryArchivesService.queryChangeRecordList(employeeId);
        return Result.ok(list);
    }


    @PostMapping("/batchChangeSalaryRecord")
    @ApiOperation("批量调薪")
    public OperationResult<Map<String, Object>> batchChangeSalaryRecord(@RequestBody BatchChangeSalaryRecordBO batchChangeSalaryRecordBO) {
        Map<String, Object> result = hrmSalaryArchivesService.batchChangeSalaryRecord(batchChangeSalaryRecordBO);
        List<OperationLog> operationLog = (List<OperationLog>) result.get("operationLog");
        result.remove("operationLog");
        return OperationResult.ok(result, operationLog);
    }
}
