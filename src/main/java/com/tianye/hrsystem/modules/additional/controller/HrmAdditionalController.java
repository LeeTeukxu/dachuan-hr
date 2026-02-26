package com.tianye.hrsystem.modules.additional.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.bo.UpdateAdditionalBO;
import com.tianye.hrsystem.modules.additional.service.HrmAdditionalService;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.deduction.bo.UpdatePersonalIncomeTaxBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrmAdditional")
@Api(tags = "附加累计-个税专项附加")
public class HrmAdditionalController {
    @Autowired
    HrmAdditionalService hrmAdditionalService;

    /**
     * 导入个税专项附加报表数据
     * @param
     * @return
     */
    @PostMapping("/importAdditional")
    @ApiOperation(value = "导入个税专项附加报表数据")
    public Result importAdditional(@ApiParam("个税专项附加") @RequestParam(name = "additionalFile", required = false) MultipartFile additionalFile,
                                   @RequestParam(name = "year") String year, @RequestParam(name = "month") String month) {
        try {
            hrmAdditionalService.resolveAdditionalData(additionalFile, year, month);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    /**
     * 导入专项扣除信息数据
     * @param
     * @return
     */
    @PostMapping("/importAdditionalInfo")
    @ApiOperation(value = "导入专项扣除信息数据")
    public Result importAdditionalInfo(@ApiParam("专项扣除") @RequestParam(name = "additionalInfoFile", required = false) MultipartFile additionalInfoFile,
                                       @RequestParam(name = "year") String year) {
        try {
            hrmAdditionalService.resolveAdditionalInfoData(additionalInfoFile, year);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    @PostMapping("/queryAdditionalList")
    @ApiOperation("查询个税专项附加列表")
    public Result<Page<QueryAdditionalVO>> queryAdditionalList(@RequestBody QueryAdditionalBO queryAdditionalBO) {
        try {
            Page<QueryAdditionalVO> page = hrmAdditionalService.queryAdditionalList(queryAdditionalBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/updateSalary")
    @ApiOperation("在线修改个税项专项扣除")
    public Result updateAdditional(@RequestBody UpdateAdditionalBO updateAdditionalBO)
    {
        OperationResult operationResult = hrmAdditionalService.updateAdditional(updateAdditionalBO);
        return Result.OK();
    }

    @PostMapping("/delete/{additionalId}")
    @ApiOperation("删除专项附加扣除")
    public Result deleteAdditional(@PathVariable("additionalId") Long additionalId) {
        OperationResult operationResult = hrmAdditionalService.deleteAdditional(additionalId);
        return Result.OK();
    }
}
