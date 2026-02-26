package com.tianye.hrsystem.modules.attendanceinfo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import com.tianye.hrsystem.modules.attendanceinfo.bo.QueryAttendanceInfoBO;
import com.tianye.hrsystem.modules.attendanceinfo.service.HrmAttendanceInfoService;
import com.tianye.hrsystem.modules.attendanceinfo.vo.QueryAttendanceInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hrmAttendanceInfo")
@Api(tags = "考勤管理-考勤天数")
public class HrmAttendanceInfoController {

    @Autowired
    HrmAttendanceInfoService attendanceInfoService;

    @PostMapping("/queryAttendanceInfoByParam")
    @ApiOperation("根据年月条件查询出勤天数")
    public Result<List<QueryAttendanceInfoVO>> queryAttendanceInfoByParam(@RequestBody QueryAttendanceInfoBO queryAttendanceInfoBO) {
        try {
            List<QueryAttendanceInfoVO> listAttendanceInfo = attendanceInfoService.queryAttendanceInfoByParam(queryAttendanceInfoBO);
            return Result.OK(listAttendanceInfo);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.Error(ax);
        }
    }

    @PostMapping("/saveAttendanceInfo")
    @ApiOperation("保存出勤天数")
    public Result saveAttendanceInfo(@RequestBody QueryAttendanceInfoBO queryAttendanceInfoBO) {
        try {
            attendanceInfoService.saveAttendanceInfo(queryAttendanceInfoBO);
            return Result.OK();
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.Error(ax);
        }
    }
}
