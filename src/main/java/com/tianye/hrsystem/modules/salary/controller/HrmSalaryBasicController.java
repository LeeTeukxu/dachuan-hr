package com.tianye.hrsystem.modules.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryBasicDto;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryBasicService;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrmSalaryBasic")
@Api(tags = "基本工资")
public class HrmSalaryBasicController {

    @Autowired
    HrmSalaryBasicService hrmSalaryBasicService;

//    @PostMapping("/querySalaryBasic")
//    @ApiOperation("查询基本工资列表")
//    public Result<Page<QuerySalaryBasicVO>> querySalaryBasic(@RequestBody QuerySalaryBasicDto querySalaryBasicDto) {
//        Page<QuerySalaryBasicVO> page = hrmSalaryBasicService.querySalaryBasic(querySalaryBasicDto);
//        return Result.ok(page);
//    }

    @PostMapping("/deleteSalaryBasic/{id}")
    @ApiOperation("删除基本工资")
    public Result deleteSalaryBasic(@PathVariable Long id) {
        hrmSalaryBasicService.deleteSalaryBasic(id);
        return Result.ok();
    }

    @PostMapping("/saveSalaryBasic")
    @ApiOperation("保存基本工资")
    public Result saveSalaryBasic(@RequestBody QuerySalaryBasicDto querySalaryBasicDto){
        hrmSalaryBasicService.saveSalaryBasic(querySalaryBasicDto);
        return Result.ok();
    }

    @PostMapping("/queryById/{id}")
    @ApiOperation("根据id查询基本工资详情")
    public Result<QuerySalaryBasicVO> queryById(@PathVariable("id") String id) {
        QuerySalaryBasicVO querySalaryBasicVO = hrmSalaryBasicService.queryById(id);
        return Result.ok(querySalaryBasicVO);
    }

    @PostMapping("/findAll")
    @ApiOperation("查询基本工资")
    public Result<QuerySalaryBasicVO> findAll() {
        QuerySalaryBasicVO querySalaryBasicVO = hrmSalaryBasicService.findAll();
        return Result.ok(querySalaryBasicVO);
    }
}
