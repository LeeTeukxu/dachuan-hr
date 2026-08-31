package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ILeaveRecordDtaService;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import com.tianye.hrsystem.util.MyDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName: LeaveRecordDataServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:20
 **/
@Service
public class LeaveRecordDataServiceImpl implements ILeaveRecordDtaService {
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
    @Override
    public void Sync(String EmpIDS, Date Begin, Date End, List<tbattendanceuser> users) throws Exception {
        List<Date[]> Dates = dateUtils.getDateRangeByLimit(Begin, End, 15);
        List<tbattendanceuser> matchedUsers = resolveTargetUsers(EmpIDS, users);
        for (int a = 0; a < matchedUsers.size(); a++) {
            tbattendanceuser user = matchedUsers.get(a);
            String userId = user.getUserId();
            Long EmpID = user.getEmpId();
            for (int i = 0; i < Dates.size(); i++) {
                Date[] D = Dates.get(i);
                Date BeginDate = D[0];
                Date EndDate = D[1];
                report.UpdateAttendanceReportQuick(userId,EmpID,BeginDate,EndDate);
            }
        }
    }

    private List<tbattendanceuser> resolveTargetUsers(String empIds, List<tbattendanceuser> users) {
        List<Long> ids = parseEmpIds(empIds);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<tbattendanceuser> sourceUsers =
                users == null || users.isEmpty() ? userRep.findAllByEmpIdIn(ids) : users;
        if (sourceUsers == null || sourceUsers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> targetEmpIds = ids.stream().collect(Collectors.toSet());
        Map<String, tbattendanceuser> byUserId = new LinkedHashMap<>();
        for (tbattendanceuser user : sourceUsers) {
            if (user == null || user.getEmpId() == null || user.getUserId() == null) {
                continue;
            }
            String userId = user.getUserId().trim();
            if (userId.isEmpty() || !targetEmpIds.contains(user.getEmpId())) {
                continue;
            }
            byUserId.putIfAbsent(userId, user);
        }
        return byUserId.values().stream().collect(Collectors.toList());
    }

    private List<Long> parseEmpIds(String empIds) {
        if (empIds == null || empIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(empIds.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::parseLong)
                .distinct()
                .collect(Collectors.toList());
    }
}
