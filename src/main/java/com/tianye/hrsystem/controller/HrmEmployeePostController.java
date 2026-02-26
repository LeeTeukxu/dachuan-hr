package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.ApplyEnum;
import com.tianye.hrsystem.common.BehaviorEnum;
import com.tianye.hrsystem.common.OperateLog;
import com.tianye.hrsystem.common.OperateObjectEnum;
import com.tianye.hrsystem.entity.bo.DeleteLeaveInformationBO;
import com.tianye.hrsystem.entity.bo.UpdateInformationBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeQuitInfo;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.entity.vo.PostInformationVO;
import com.tianye.hrsystem.entity.vo.Result;

import com.tianye.hrsystem.service.employee.IHrmEmployeePostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: HrmEmployeePostController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月23日 15:09
 **/
@RestController
@RequestMapping("/hrmEmployeePost")
@Api(tags = "员工管理-员工岗位接口")
public class HrmEmployeePostController {
    @Autowired
    private IHrmEmployeePostService employeePostService;

    @PostMapping("/postInformation/{employeeId}")
    @ApiOperation("岗位信息")
    public Result<PostInformationVO> postInformation(@PathVariable("employeeId") Long employeeId) {
        PostInformationVO postInformationVO = employeePostService.postInformation(employeeId);
        return Result.ok(postInformationVO);
    }

    @PostMapping("/updatePostInformation")
    @ApiOperation("修改岗位信息")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result updatePostInformation(@RequestBody UpdateInformationBO updateInformationBO) {
        OperationLog operationLog = employeePostService.updatePostInformation(updateInformationBO);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/addLeaveInformation")
    @ApiOperation("办理离职")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.ADD_LEAVE)
    public Result addLeaveInformation(@RequestBody HrmEmployeeQuitInfo quitInfo) {
        OperationLog operationLog = employeePostService.addOrUpdateLeaveInformation(quitInfo);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/setLeaveInformation")
    @ApiOperation("修改离职信息")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE_LEAVE)
    public Result setLeaveInformation(@RequestBody HrmEmployeeQuitInfo quitInfo) {
        OperationLog operationLog = employeePostService.addOrUpdateLeaveInformation(quitInfo);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/deleteLeaveInformation")
    @ApiOperation("取消离职")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.DELETE_LEAVE)
    public Result deleteLeaveInformation(@RequestBody DeleteLeaveInformationBO deleteLeaveInformationBO) {
        OperationLog operationLog = employeePostService.deleteLeaveInformation(deleteLeaveInformationBO);
        return OperationResult.ok(operationLog);
    }



}
