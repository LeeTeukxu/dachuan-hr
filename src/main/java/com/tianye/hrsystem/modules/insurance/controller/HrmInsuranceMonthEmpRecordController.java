package com.tianye.hrsystem.modules.insurance.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.modules.insurance.dto.AddInsuranceEmpBO;
import com.tianye.hrsystem.modules.insurance.dto.QueryEmpInsuranceMonthBO;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceProjectBO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceMonthEmpRecordService;
import com.tianye.hrsystem.modules.insurance.vo.EmpInsuranceByIdVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryEmpInsuranceMonthVO;
import com.tianye.hrsystem.modules.insurance.vo.SimpleHrmEmployeeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 员工每月社保记录 前端控制器
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@RestController
@RequestMapping("/hrmInsuranceMonthEmpRecord")
@Api(tags = "社保管理-员工社保")
public class HrmInsuranceMonthEmpRecordController {


    @Autowired
    private HrmInsuranceMonthEmpRecordService monthEmpRecordService;

    @PostMapping("/queryEmpInsuranceMonth")
    @ApiOperation("查询每月员工社保列表")
    public Result<Page<QueryEmpInsuranceMonthVO>> queryEmpInsuranceMonth(@RequestBody QueryEmpInsuranceMonthBO queryEmpInsuranceMonthBO) {
        Page<QueryEmpInsuranceMonthVO> page = monthEmpRecordService.queryEmpInsuranceMonth(queryEmpInsuranceMonthBO);
        return Result.OK(page);
    }

    @PostMapping("/queryById/{iempRecordId}")
    @ApiOperation("查询员工社保详情")
    public Result<EmpInsuranceByIdVO> queryById(@PathVariable("iempRecordId") String iempRecordId) {
        EmpInsuranceByIdVO empInsuranceByIdVO = monthEmpRecordService.queryById(iempRecordId);
        return Result.OK(empInsuranceByIdVO);
    }


    @PostMapping("/stop")
    @ApiOperation("停止参保")
//    @OperateLog(apply = ApplyEnum.HRM, behavior = BehaviorEnum.STOP_INSURANCE, object = OperateObjectEnum.HRM_INSURANCE_SCHEME)
    public Result stop(@RequestBody List<Long> ids) {
        List<OperationLog> operationLogList = monthEmpRecordService.stop(ids);
        return Result.OK(operationLogList);
    }

    @PostMapping("/updateInsuranceProject")
    @ApiOperation("修改参保方案项目")
//    @OperateLog(apply = ApplyEnum.HRM, behavior = BehaviorEnum.UPDATE_INSURANCE_SCHEME, object = OperateObjectEnum.HRM_INSURANCE_SCHEME)
    public Result updateInsuranceProject(@RequestBody UpdateInsuranceProjectBO updateInsuranceProjectBO) {
        OperationLog operationLog = monthEmpRecordService.updateInsuranceProject(updateInsuranceProjectBO);
        return Result.OK(operationLog);
    }

    @PostMapping("/batchUpdateInsuranceProject")
    @ApiOperation("批量修改参保方案项目")
//    @OperateLog(apply = ApplyEnum.HRM, behavior = BehaviorEnum.UPDATE_INSURANCE_SCHEME, object = OperateObjectEnum.HRM_INSURANCE_SCHEME)
    public Result batchUpdateInsuranceProject(@RequestBody UpdateInsuranceProjectBO updateInsuranceProjectBO) {
        List<OperationLog> operationLogList = monthEmpRecordService.batchUpdateInsuranceProject(updateInsuranceProjectBO);
        return Result.OK(operationLogList);
    }

    @PostMapping("/addInsuranceEmp")
    @ApiOperation("添加参保人员")
//    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_INSURANCE_SCHEME, behavior = BehaviorEnum.ADD_INSURANCE_EMP)
    public Result addInsuranceEmp(@RequestBody AddInsuranceEmpBO addInsuranceEmpBO) {
        List<OperationLog> operationLogList = monthEmpRecordService.addInsuranceEmp(addInsuranceEmpBO);
        return Result.OK(operationLogList);
    }

    @PostMapping("/queryNoInsuranceEmp/{iRecordId}")
    @ApiOperation("查询本月没有参保人员")
    public Result<List<SimpleHrmEmployeeVO>> queryNoInsuranceEmp(@PathVariable("iRecordId") Long iRecordId) {
        List<SimpleHrmEmployeeVO> employeeVOS = monthEmpRecordService.queryNoInsuranceEmp(iRecordId);
        return Result.OK(employeeVOS);
    }

    @PostMapping("/myInsurance")
    @ApiOperation("我的社保")
    public Result<Page<HrmInsuranceMonthEmpRecord>> myInsurancePageList(@RequestBody QueryInsuranceRecordListBO recordListBO) {
        Page<HrmInsuranceMonthEmpRecord> page = monthEmpRecordService.myInsurancePageList(recordListBO);
        return Result.OK(page);
    }

    @PostMapping("/myInsuranceDetail/{iempRecordId}")
    @ApiOperation("我的社保详情")
    public Result<EmpInsuranceByIdVO> myInsuranceDetail(@PathVariable("iempRecordId") String iempRecordId) {
        EmpInsuranceByIdVO empInsuranceByIdVO = monthEmpRecordService.queryById(iempRecordId);
        return Result.OK(empInsuranceByIdVO);
    }

}

