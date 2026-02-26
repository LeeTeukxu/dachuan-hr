package com.tianye.hrsystem.modules.additional.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.service.HrmEmployeeAdditionalService;
import com.tianye.hrsystem.modules.additional.vo.QueryEmployeeAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrmEmployeeAdditional")
@Api(tags = "员工专项附加扣除值")
public class HrmEmployeeAdditionalController {

    @Autowired
    HrmEmployeeAdditionalService hrmEmployeeAdditionalService;

    @PostMapping("/importEmployeeAdditional")
    @ApiOperation(value = "更新员工附加扣除值")
    public Result importEmployeeAdditional(@ApiParam("员工专项扣除值") @RequestParam(name = "employeeAdditionalFile", required = false) MultipartFile multipartFile) {
        try {
            hrmEmployeeAdditionalService.resolveEmployeeAdditionalData(multipartFile);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    @PostMapping("/queryEmployeeAdditionalList")
    @ApiOperation("查询员工年度附加扣除值列表")
    public Result<Page<QueryEmployeeAdditionalVO>> queryEmployeeAdditionalList(@RequestBody QueryAdditionalBO queryAdditionalBO) {
        try {
            Page<QueryEmployeeAdditionalVO> page = hrmEmployeeAdditionalService.queryEmployeeAdditionalList(queryAdditionalBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }
}
