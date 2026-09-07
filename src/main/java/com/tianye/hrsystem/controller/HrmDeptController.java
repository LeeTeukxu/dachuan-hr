package com.tianye.hrsystem.controller;

import cn.hutool.core.convert.Convert;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddDeptBO;
import com.tianye.hrsystem.entity.bo.GenerateDeptCodeBO;
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
import com.tianye.hrsystem.entity.vo.DeptLeaderVO;
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

    @PostMapping("/generateCode")
    @ApiOperation("生成部门编码")
    public Result<String> generateCode(@RequestBody(required = false) GenerateDeptCodeBO generateDeptCodeBO) {
        Long deptId = generateDeptCodeBO == null ? null : generateDeptCodeBO.getDeptId();
        return Result.ok(deptService.generateCode(deptId));
    }

    @PostMapping("/queryById/{deptId}")
    @ApiOperation("查询部门详情")
    public Result<DeptVO> queryById(@PathVariable("deptId") Long deptId) {
        DeptVO deptVO = deptService.queryById(deptId);
        return Result.ok(deptVO);
    }

    @PostMapping("/queryDeptLeader/{deptId}")
    @ApiOperation("查询部门分管领导")
    public Result<DeptLeaderVO> queryDeptLeader(@PathVariable("deptId") Long deptId) {
        return Result.ok(deptService.queryDeptLeader(deptId));
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

    @Autowired
    private com.tianye.hrsystem.imple.HrmDeptDingTalkSyncService deptDingTalkSyncService;

    @PostMapping("/syncDingTalkDept")
    @ApiOperation("同步钉钉部门数据(不删除本地部门)")
    public Result<java.util.Map<String, Object>> syncDingTalkDept(
            @RequestParam(name = "syncType", defaultValue = "headquarters") String syncType,
            @RequestParam(name = "excludeNames", required = false) String excludeNames) {
        try {
            return Result.ok(deptDingTalkSyncService.syncDingTalkDept(syncType, excludeNames));
        } catch (Exception ex) {
            return Result.error(500, ex.getMessage());
        }
    }

    @GetMapping("/getExcludeNames")
    @ApiOperation("查询钉钉部门同步排除关键字")
    public Result<String> getExcludeNames() {
        return Result.ok(deptDingTalkSyncService.getExcludeNames());
    }

    @PostMapping("/saveExcludeNames")
    @ApiOperation("保存钉钉部门同步排除关键字")
    public Result saveExcludeNames(@RequestParam("excludeNames") String excludeNames) {
        deptDingTalkSyncService.saveExcludeNames(excludeNames);
        return Result.ok();
    }
}
