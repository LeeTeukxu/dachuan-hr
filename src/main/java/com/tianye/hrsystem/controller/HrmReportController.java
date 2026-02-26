package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.model.HrmAttendanceReportData;
import com.tianye.hrsystem.model.HrmAttendanceReportField;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import lombok.Data;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmReportController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月18日 23:11
 **/
@RestController
@RequestMapping("/report")
public class HrmReportController {


    @Autowired
    hrmAttendanceReportFieldRepository fieldRep;
    @Autowired
    hrmAttendanceReportDataRepository dataRep;
    @Autowired
    hrmEmployeeRepository empRep;
    List<HrmEmployee> Emps = new ArrayList<>();
    List<HrmAttendanceReportField> fields = new ArrayList<>();

    SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd");

    @RequestMapping("/getData")
    @ResponseBody
    public PageObject GetReportData(String Begin, String End) {
        PageObject res = new PageObject();
        try {
            Date begin = simple.parse(Begin);
            Emps = empRep.findAll();
            Date end = simple.parse(End);
            List<Map<String, Object>> Res = new ArrayList<>();
            fields = fieldRep.findAll().stream().filter(f -> f.getFieldId() != null).collect(Collectors.toList());
            List<HrmAttendanceReportData> Datas = dataRep.findAllByWorkDateBetweenOrderByWorkDate(begin, end);
            for (Date D = begin; D.before(end); DateUtils.addDays(D, 1)) {
                String DText=simple.format(D);
                List<HrmAttendanceReportData> Ds =
                        Datas.stream().filter(f ->  simple.format(f.getWorkDate()).equals(DText)).collect(Collectors.toList());
                List<Map<String, Object>> KK = getSingleDay(D, Ds);
                Res.addAll(KK);
            }

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


        return res;
    }

    private List<Map<String, Object>> getSingleDay(Date workDate, List<HrmAttendanceReportData> Datas) {
        List<Long> empIds = Datas.stream().map(f -> f.getEmpId()).distinct().collect(Collectors.toList());
        List<Map<String, Object>> OO = new ArrayList<>();
        for (int i = 0; i < empIds.size(); i++) {
            Long empId = empIds.get(i);
            Optional<HrmEmployee> findEmps = Emps.stream().filter(f -> f.getEmployeeId().equals(empId)).findFirst();
            if (findEmps.isPresent()) {
                Map<String, Object> Res = new HashMap<>();
                HrmEmployee employee = findEmps.get();
                Res.put("empName", employee.getEmployeeName());
                Res.put("workDate", simple.format(workDate));
                List<HrmAttendanceReportData> DD =
                        Datas.stream().filter(f -> f.getEmpId().equals(empId)).collect(Collectors.toList());
                for (int n = 0; n < fields.size(); n++) {
                    HrmAttendanceReportField field = fields.get(n);
                    Long fieldId = field.getFieldId();
                    Optional<HrmAttendanceReportData> FindValues =
                            DD.stream().filter(f -> f.getFieldId().equals(fieldId)).findFirst();
                    if (FindValues.isPresent()) {
                        HrmAttendanceReportData val = FindValues.get();
                        Res.put(field.getFieldName(), val.getValue());
                    } else Res.put(field.getFieldName(), "");
                }
                OO.add(Res);
            }
        }
        return OO;
    }

}
