package com.tianye.hrsystem.controller;


import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryOverTimeRecordPageListBO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.employee.IHrmEmployeeOverTimeRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <p>
 * 员工加班表 前端控制器
 * </p>
 *
 * @author guomenghao
 * @since 2021-09-07
 */
@RestController
@RequestMapping("/hrmEmployeeOverTimeRecord")
@Api(tags = "考勤管理-加班记录")
public class HrmEmployeeOverTimeRecordController {
    @Autowired
    private IHrmEmployeeOverTimeRecordService employeeOverTimeRecordService;

    @PostMapping("/queryOverTimeRecordPageList")
    @ApiOperation("查询加班记录列表")
    public Result<BasePage<Map<String, Object>>> queryPageList(@RequestBody QueryOverTimeRecordPageListBO queryOverTimeRecordPageListBO) {
        BasePage<Map<String, Object>> map = employeeOverTimeRecordService.queryOverTimeRecordPageList(queryOverTimeRecordPageListBO);
        return Result.ok(map);
    }


}

