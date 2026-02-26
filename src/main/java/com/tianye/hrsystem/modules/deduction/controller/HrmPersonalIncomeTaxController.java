package com.tianye.hrsystem.modules.deduction.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.*;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.modules.deduction.bo.QueryPersonalIncomeTaxBO;
import com.tianye.hrsystem.modules.deduction.bo.UpdatePersonalIncomeTaxBO;
import com.tianye.hrsystem.modules.deduction.service.HrmPersonalIncomeTaxService;
import com.tianye.hrsystem.modules.deduction.vo.QueryPersonalIncomeTaxVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/hrmPersonalIncomeTax")
@Api(tags = "个税累计-扣税")
public class HrmPersonalIncomeTaxController {
    @Autowired
    HrmPersonalIncomeTaxService hrmPersonalIncomeTaxService;

    /**
     * 导入个税累计报表数据
     * @param
     * @return
     */
    @PostMapping("/importPersonalIncomeTax")
    @ApiOperation(value = "导入个税累计报表数据")
    public Result importPersonalIncomeTax(@ApiParam("累计个税") @RequestParam(name = "personalIncomeTaxFile", required = false) MultipartFile personalIncomeTaxFile,
                                          @RequestParam(name = "dates") String dates) {
        try {
            hrmPersonalIncomeTaxService.resolvePersonalIncomeTaxData(personalIncomeTaxFile, dates);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    @PostMapping("/queryPersonalIncomeTaxList")
    @ApiOperation("查询个税累计列表")
    public Result<Page<QueryPersonalIncomeTaxVO>> queryPersonalIncomeTaxList(@RequestBody QueryPersonalIncomeTaxBO queryRemainingVacationBO) {
        try {
            Page<QueryPersonalIncomeTaxVO> page = hrmPersonalIncomeTaxService.queryPersonalIncomeTaxList(queryRemainingVacationBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/updateSalary")
    @ApiOperation("在线修改个税累计项")
    public Result updatePersonalIncomeTax(@RequestBody UpdatePersonalIncomeTaxBO updatePersonalIncomeTaxBOList)
    {
        OperationResult operationResult = hrmPersonalIncomeTaxService.updatePersonalIncomeTax(updatePersonalIncomeTaxBOList);
        return Result.OK();
    }

    @PostMapping("/delete/{personalIncomeTaxId}")
    @ApiOperation("删除个税累计")
    public Result deletePersonalIncomeTax(@PathVariable("personalIncomeTaxId") Long personalIncomeTaxId) {
        OperationResult operationResult = hrmPersonalIncomeTaxService.deletePersonalIncomeTax(personalIncomeTaxId);
        return Result.OK();
    }
}
