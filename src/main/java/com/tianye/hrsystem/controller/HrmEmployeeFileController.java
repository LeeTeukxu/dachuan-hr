package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.entity.bo.AddFileBO;
import com.tianye.hrsystem.entity.bo.QueryFileBySubTypeBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeFile;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.employee.IHrmEmployeeFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工附件表 前端控制器
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@RestController
@RequestMapping("/hrmEmployeeFile")
@Api(tags = "员工管理-员工附件")
public class HrmEmployeeFileController {

    @Autowired
    private IHrmEmployeeFileService employeeFileService;

    @PostMapping("/queryFileNum/{employeeId}")
    @ApiOperation("查询员工总体附件")
    public Result<Map<String, Object>> queryFileNum(@PathVariable("employeeId") Long employeeId) {
        Map<String, Object> map = employeeFileService.queryFileNum(employeeId);
        return Result.ok(map);
    }

    @PostMapping("/queryFileBySubType")
    @ApiOperation("根据附件类型查询附件详情")
    public Result<List<HrmEmployeeFile>> queryFileBySubType(@RequestBody QueryFileBySubTypeBO QueryFileBySubTypeBo) {
        List<HrmEmployeeFile> list = employeeFileService.queryFileBySubType(QueryFileBySubTypeBo);
        return Result.ok(list);
    }

    @PostMapping("/addFile")
    @ApiOperation("添加附件")
    public Result addFile(@RequestBody AddFileBO addFileBO) {
        employeeFileService.addFile(addFileBO);
        return Result.ok();
    }


    @PostMapping("/deleteFile/{employeeFileId}")
    @ApiOperation("删除附件")
    public Result deleteFile(@PathVariable("employeeFileId") Long employeeFileId) {
        employeeFileService.deleteFile(employeeFileId);
        return Result.ok();
    }
}

