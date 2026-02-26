package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceClockRepository;
import com.tianye.hrsystem.service.IAttendanceDetailService;
import com.tianye.hrsystem.service.ddTalk.IDetailRecord;
import com.tianye.hrsystem.util.MyDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: AttendanceDetailServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:03
 **/

@Service
public class AttendanceDetailServiceImpl implements IAttendanceDetailService {
    @Autowired
    IDetailRecord detailRecord;
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    hrmAttendanceClockRepository clockRep;
    List<tbattendanceuser> users;
    @Override
    public void setUsers(List<tbattendanceuser> users) {
        this.users=users;
    }
    @Override
    @Transactional
    public void Sync(String EmpIDS, Date Begin, Date End) throws Exception {
        List<Date[]> Dates = dateUtils.getDateRangeByLimit(Begin, End, 7);
        List<Long>EmpIDD= Arrays.stream(EmpIDS.split(",")).map(f->Long.parseLong(f)).collect(Collectors.toList());
        clockRep.deleteAllByClockEmployeeIdInAndWorkDateBetween(EmpIDD,Begin,End);
        for (int i = 0; i < Dates.size(); i++) {
            Date[] Ds = Dates.get(i);
            Date BeginDate = Ds[0];
            Date EndDate = Ds[1];
            detailRecord.setUsers(users);
            detailRecord.GetAndSave(EmpIDS,BeginDate,EndDate);
        }
    }
}
