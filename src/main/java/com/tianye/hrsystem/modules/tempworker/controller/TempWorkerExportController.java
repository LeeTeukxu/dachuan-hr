package com.tianye.hrsystem.modules.tempworker.controller;

import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryConfigDto;
import com.tianye.hrsystem.modules.tempworker.service.TempWorkerExportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/tempworker")
public class TempWorkerExportController
{
    @Autowired
    private TempWorkerExportService exportService;

    @GetMapping("/export")
    public Result export(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            exportService.export(response);
            return Result.ok();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return Result.error(500,"失败");
        }


    }
}
