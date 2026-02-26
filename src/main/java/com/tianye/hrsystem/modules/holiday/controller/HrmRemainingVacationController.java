package com.tianye.hrsystem.modules.holiday.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import com.tianye.hrsystem.modules.holiday.bo.QueryRemainingVacationBO;
import com.tianye.hrsystem.modules.holiday.service.HrmRemainingVacationService;
import com.tianye.hrsystem.modules.holiday.vo.QueryRemainingVacationVO;
import com.tianye.hrsystem.service.IHrmProduceAttendanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hrmRemainingVaction")
@Api(tags = "假期管理-假期累计")
public class HrmRemainingVacationController {

    @Autowired
    HrmRemainingVacationService hrmRemainingVacationService;

    /**
     * 导入生产部考勤报表数据
     * @param
     * @return
     */
    @PostMapping("/importRemainingVacation")
    @ApiOperation(value = "导入假期累计报表数据")
    public Result importRemainingVacation(@ApiParam("假期累计") @RequestParam(name = "remainingVacationFile", required = false) MultipartFile remainingVacationFile) {
        try {
            hrmRemainingVacationService.resolveRemainingVacationData(remainingVacationFile);
        }catch (Exception ax) {
            ax.printStackTrace();
        }
        return Result.OK();
    }

    @PostMapping("/queryRemainingVacationList")
    @ApiOperation("查询假期累计列表")
    public Result<Page<QueryRemainingVacationVO>> queryRemainingVacationList(@RequestBody QueryRemainingVacationBO queryRemainingVacationBO) {
        try {
            Page<QueryRemainingVacationVO> page = hrmRemainingVacationService.queryRemainingVacationList(queryRemainingVacationBO);
            return Result.OK(page);
        }catch (Exception ax) {
            return Result.Error(ax);
        }
    }
}
