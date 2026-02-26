package com.tianye.hrsystem.controller;

import cn.hutool.core.convert.Convert;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddDeptBO;
import com.tianye.hrsystem.entity.bo.QueryDeptListBO;
import com.tianye.hrsystem.entity.bo.QueryEmployeeByDeptIdBO;
import com.tianye.hrsystem.entity.vo.QueryEmployeeListByDeptIdVO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.IHrmDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.tianye.hrsystem.entity.vo.DeptVO;
/**
 * @ClassName: DepController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 23:11
 **/

@RestController
@RequestMapping("/hrmDept")
@Api(tags = "组织管理-部门")
public class HrmDeptController {
    @Autowired
    private IHrmDeptService deptService;

    @PostMapping("/addDept")
    @ApiOperation("添加部门")
    public Result addDept(@Valid @RequestBody AddDeptBO addDeptBO) {
        deptService.addOrUpdate(addDeptBO);
        return Result.ok();
    }

    @PostMapping("/setDept")
    @ApiOperation("修改部门")
    public Result setDept(@Valid @RequestBody AddDeptBO addDeptBO) {
        deptService.addOrUpdate(addDeptBO);
        return Result.ok();
    }

    @PostMapping("/queryById/{deptId}")
    @ApiOperation("查询部门详情")
    public Result<DeptVO> queryById(@PathVariable("deptId") Long deptId) {
        DeptVO deptVO = deptService.queryById(deptId);
        return Result.ok(deptVO);
    }

    @PostMapping("/queryTreeList")
    @ApiOperation("查询部门列表")
    public Result<List<DeptVO>> queryTreeList(@RequestBody QueryDeptListBO queryDeptListBO) {
        List<DeptVO> treeNode = deptService.queryTreeList(queryDeptListBO);
        return Result.ok(treeNode);
    }

    @PostMapping("/queryEmployeeByDeptId")
    @ApiOperation("通过部门id查询员工列表")
    public Result<BasePage<QueryEmployeeListByDeptIdVO>> queryEmployeeByDeptId(@RequestBody QueryEmployeeByDeptIdBO employeeByDeptIdBO) {
        BasePage<QueryEmployeeListByDeptIdVO> page = deptService.queryEmployeeByDeptId(employeeByDeptIdBO);
        return Result.ok(page);
    }

    @PostMapping("/deleteDeptById/{deptId}")
    @ApiOperation("删除部门")
    public Result deleteDeptById(@PathVariable("deptId") String deptId) {
        deptService.deleteDeptById(deptId);
        return Result.ok();
    }
}
