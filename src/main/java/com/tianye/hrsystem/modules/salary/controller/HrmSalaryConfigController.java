package com.tianye.hrsystem.modules.salary.controller;

import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryConfigDto;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryConfigService;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrmSalaryConfig")
@Api(tags = "计薪设置")
public class HrmSalaryConfigController {

    @Autowired
    HrmSalaryConfigService hrmSalaryConfigService;

//    @PostMapping("/querySalaryBasic")
//    @ApiOperation("查询基本工资列表")
//    public Result<Page<QuerySalaryBasicVO>> querySalaryBasic(@RequestBody QuerySalaryBasicDto querySalaryBasicDto) {
//        Page<QuerySalaryBasicVO> page = hrmSalaryBasicService.querySalaryBasic(querySalaryBasicDto);
//        return Result.ok(page);
//    }

    @PostMapping("/deleteSalaryConfig/{id}")
    @ApiOperation("删除计薪设置")
    public Result deleteSalaryConfig(@PathVariable Long id) {
        hrmSalaryConfigService.deleteSalaryConfig(id);
        return Result.ok();
    }

    @PostMapping("/saveSalaryConfig")
    @ApiOperation("保存计薪设置")
    public Result saveSalaryConfig(@RequestBody QuerySalaryConfigDto querySalaryConfigDto){
        hrmSalaryConfigService.saveSalaryConfig(querySalaryConfigDto);
        return Result.ok();
    }

    @PostMapping("/queryById/{id}")
    @ApiOperation("根据id查询计薪设置详情")
    public Result<QuerySalaryConfigVO> queryById(@PathVariable("id") String id) {
        QuerySalaryConfigVO querySalaryBasicVO = hrmSalaryConfigService.queryById(id);
        return Result.ok(querySalaryBasicVO);
    }

    @PostMapping("/findAll")
    @ApiOperation("查询计薪设置详情")
    public Result<QuerySalaryConfigVO> findAll() {
        QuerySalaryConfigVO querySalaryBasicVO = hrmSalaryConfigService.findAll();
        return Result.ok(querySalaryBasicVO);
    }
}
