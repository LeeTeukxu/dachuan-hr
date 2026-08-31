package com.tianye.hrsystem.service;

import java.time.YearMonth;
import java.util.List;

public interface IHrmAttendanceApprovalSyncService {

    long fetchMonthData(YearMonth month, List<Long> employeeIds, List<String> approvalTypes) throws Exception;

    List<Long> resolveFetchTargetEmployeeIds(List<Long> employeeIds);
}
