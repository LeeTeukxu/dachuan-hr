package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.HrmAttendanceReportField;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHolidayDataService;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import com.tianye.hrsystem.util.MyDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: HolidayDataServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:21
 **/

@Service
public class HolidayDataServiceImpl implements IHolidayDataService {


    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    IHrmAttendanceReport report;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    hrmAttendanceReportDataRepository dataRep;
    @Autowired
    hrmAttendanceReportFieldRepository fieldRep;
    List<tbattendanceuser> users;
    @Override
    public void setUsers(List<tbattendanceuser> users) {
        this.users=users;
    }
    @Override
    public void Sync(String EmpIDS, Date Begin, Date End) throws Exception{
        List<Date[]> Dates = dateUtils.getDateRangeByLimit(Begin, End, 15);
        List<Long> IDS = Arrays.stream(EmpIDS.split(",")).map(f -> Long.parseLong(f)).collect(Collectors.toList());
        List<tbattendanceuser> users = userRep.findAllByEmpIdIn(IDS);
        List<Long> Fields=getFieldByType(2);
        for (int a = 0; a < users.size(); a++) {
            tbattendanceuser user = users.get(a);
            String userId = user.getUserId();
            Long EmpID = user.getEmpId();
            //dataRep.deleteAllByEmpIdAndWorkDateBetweenAndFieldIdIn(EmpID,Begin,End,Fields);
            for (int i = 0; i < Dates.size(); i++) {
                Date[] D = Dates.get(i);
                Date BeginDate = D[0];
                Date EndDate = D[1];
                report.UpdateHolidayReportQuick(userId,EmpID,BeginDate,EndDate);
            }
        }
    }
    private List<Long> getFieldByType(Integer Type){
        List<HrmAttendanceReportField> fields=fieldRep.findAllByType(Type);
        return fields.stream().map(f->f.getFieldId()).collect(Collectors.toList());
    }
}
