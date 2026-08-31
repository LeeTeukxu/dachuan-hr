package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.model.HrmWorkplanApplication;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.workplanapplication.service.IWorkPlanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * hr_web 后台「排班申请审批」接口。
 * 走现有菜单权限（ApiPermissionPathSupport 已映射 /workPlanApplication -> 排班管理菜单）。
 */
@RestController
@RequestMapping("/workPlanApplication")
public class WorkPlanApplicationController {

    @Autowired
    private IWorkPlanApplicationService applicationService;

    @PostMapping("/queryPage")
    public successResult queryPage(String status, String begin, String end) {
        successResult result = new successResult();
        try {
            Date beginDate = parseDate(begin);
            Date endDate = parseDate(end);
            List<HrmWorkplanApplication> list = applicationService.listAll(status, beginDate, endDate);
            result.setData(list);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/approve")
    public successResult approve(Long id, String action, String reason) {
        successResult result = new successResult();
        try {
            applicationService.approve(id, action, reason);
            result.setMessage("操作成功");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    private Date parseDate(String date) throws ParseException {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setLenient(false);
        return fmt.parse(date);
    }
}