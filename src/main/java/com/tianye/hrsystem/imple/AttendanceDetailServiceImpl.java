package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceClockRepository;
import com.tianye.hrsystem.service.IAttendanceDetailService;
import com.tianye.hrsystem.service.ddTalk.IDetailRecord;
import com.tianye.hrsystem.util.MyDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName: AttendanceDetailServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:03
 **/

@Service
public class AttendanceDetailServiceImpl implements IAttendanceDetailService {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceDetailServiceImpl.class);
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
        List<Long> targetEmpIds = Arrays.stream(EmpIDS.split(",")).map(Long::parseLong).collect(Collectors.toList());
        Set<Long> mappedEmpIds = users == null ? java.util.Collections.emptySet() :
                users.stream().map(tbattendanceuser::getEmpId).collect(Collectors.toSet());
        List<Long> availableEmpIds = targetEmpIds.stream().filter(mappedEmpIds::contains).collect(Collectors.toList());

        if (availableEmpIds.isEmpty()) {
            logger.warn("本次同步未匹配到钉钉用户映射，跳过删除与拉取。empIds={}", EmpIDS);
            return;
        }

        String availableEmpIdString = availableEmpIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        clockRep.deleteAllByClockEmployeeIdInAndWorkDateBetween(availableEmpIds, Begin, End);
        for (int i = 0; i < Dates.size(); i++) {
            Date[] Ds = Dates.get(i);
            Date BeginDate = Ds[0];
            Date EndDate = Ds[1];
            detailRecord.setUsers(users);
            detailRecord.GetAndSave(availableEmpIdString,BeginDate,EndDate);
        }
    }
}
