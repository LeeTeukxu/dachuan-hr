package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmAttendanceApprovalFetchMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmAttendanceApprovalFetchMarkRepository extends JpaRepository<HrmAttendanceApprovalFetchMark, Long> {

    long countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType(String monthKey, Integer fetchVersion, List<String> userIds, String approvalType);

    boolean existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType(String monthKey, String userId, Integer fetchVersion, String approvalType);
}
