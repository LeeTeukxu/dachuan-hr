package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.model.ddTalk.RptAttendanceDetail;
import com.tianye.hrsystem.service.ddTalk.IAttendanceReport;
import com.tianye.hrsystem.entity.param.AttendanceQueryParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: AttendanceController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月19日 20:58
 **/
@RestController
public class AttendanceController {

    @Autowired
    IAttendanceReport attReportService;

//    @RequestMapping("/getDetail")
//    public Result<PageObject<RptAttendanceDetail>> GetDetail(AttendanceQueryParameter parameter) {
//        Result<PageObject<RptAttendanceDetail>> result = new Result<>();
//        SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        try {
//
//            List<String> times = parameter.getTimes();
//
//            Date Begin = simple.parse(times.get(0) + " 00:00:00");
//            Date End = simple.parse(times.get(1) + " 23:59:59");
//            List<RptAttendanceDetail> Ks = attReportService.Create(Begin, End);
//            PageObject<RptAttendanceDetail> RR = new PageObject<>();
//            RR.setList(Ks);
//            result.setData(RR);
//        } catch (Exception ax) {
//            result.setCode(ResultCode.FAIL);
//        }
//        return result;
//    }

}
