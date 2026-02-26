package com.tianye.hrsystem.modules.holiday.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.holiday.bo.QueryHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.bo.UpdateHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.service.HrmHolidayDeductionService;
import com.tianye.hrsystem.modules.holiday.service.HrmRemainingVacationService;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import com.tianye.hrsystem.modules.holiday.vo.QueryRemainingVacationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrmHolidayDeduction")
@Api(tags = "假期管理-假期抵扣")
public class HrmHolidayDeductionController {

    @Autowired
    HrmHolidayDeductionService hrmHolidayDeductionService;

    @PostMapping("/queryHolidayDeductionList")
    @ApiOperation("查询假期抵扣列表")
    public Result<Page<QueryHolidayDeductionVO>> queryHolidayDeductionList(@RequestBody QueryHolidayDeductionBO queryHolidayDeductionBO) {
        try {

            Page<QueryHolidayDeductionVO> page = hrmHolidayDeductionService.queryHolidayDeductionList(queryHolidayDeductionBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }

    @PostMapping("/saveHolidayDeduction")
    @ApiOperation("保存假期抵扣")
    public Result saveHolidayDeduction(@RequestBody UpdateHolidayDeductionBO updateHolidayDeductionBO) {
        hrmHolidayDeductionService.saveHolidayDeduction(updateHolidayDeductionBO);
        return Result.OK();
    }

//    @PostMapping("/queryHolidayDeduction")
//    @ApiOperation("获取抵扣")
//    public Result queryHolidayDeduction(@RequestBody QueryHolidayDeductionBO queryHolidayDeductionBO) {
//        QueryHolidayDeductionVO vo = hrmHolidayDeductionService.queryHolidayDeduction(queryHolidayDeductionBO);
//        return Result.OK(vo);
//    }
}
