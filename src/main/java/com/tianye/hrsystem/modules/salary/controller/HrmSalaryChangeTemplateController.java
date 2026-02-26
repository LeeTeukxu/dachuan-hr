package com.tianye.hrsystem.modules.salary.controller;

import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.salary.dto.SetChangeTemplateDto;
import com.tianye.hrsystem.modules.salary.service.HrmSalaryChangeTemplateService;
import com.tianye.hrsystem.modules.salary.vo.ChangeSalaryOptionVO;
import com.tianye.hrsystem.modules.salary.vo.QueryChangeTemplateListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hrmSalaryChangeTemplate")
@Api(tags = "薪资档案-调薪模板")
public class HrmSalaryChangeTemplateController
{
    @Autowired
    private HrmSalaryChangeTemplateService salaryChangeTemplateService;
    @PostMapping("/queryChangeTemplateList")
    @ApiOperation("查询模板列表")
    public Result<List<QueryChangeTemplateListVO>> queryChangeTemplateList()
    {
        List<QueryChangeTemplateListVO> list = salaryChangeTemplateService.queryChangeTemplateList();
        return Result.ok(list);
    }

    @PostMapping("/queryChangeSalaryOption")
    @ApiOperation("查询调薪默认项")
    public Result<List<ChangeSalaryOptionVO>> queryChangeSalaryOption() {
        List<ChangeSalaryOptionVO> list = salaryChangeTemplateService.queryChangeSalaryOption();
        return Result.ok(list);
    }


    @PostMapping("/setChangeTemplate")
    @ApiOperation("设置定薪/调薪模板")
    public Result setChangeTemplate(@RequestBody SetChangeTemplateDto setChangeTemplateDto)
    {
        salaryChangeTemplateService.setChangeTemplate(setChangeTemplateDto);
        return Result.ok();
    }

    @PostMapping("/deleteChangeTemplate/{id}")
    @ApiOperation("删除模板")
    public Result deleteChangeTemplate(@PathVariable("id") Long id) {
        salaryChangeTemplateService.deleteChangeTemplate(id);
        return Result.ok();
    }

}
