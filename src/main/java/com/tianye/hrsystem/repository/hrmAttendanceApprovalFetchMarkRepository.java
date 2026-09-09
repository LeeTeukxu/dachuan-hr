package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmAttendanceApprovalFetchMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface hrmAttendanceApprovalFetchMarkRepository extends JpaRepository<HrmAttendanceApprovalFetchMark, Long> {

    long countByMonthKeyAndFetchVersionAndUserIdInAndApprovalType(String monthKey, Integer fetchVersion, List<String> userIds, String approvalType);

    boolean existsByMonthKeyAndUserIdAndFetchVersionAndApprovalType(String monthKey, String userId, Integer fetchVersion, String approvalType);

    /** 该员工该月最近一次成功抓取时间（跨审批类型取最大），用于冷却期跳过判断 */
    @Query("select max(m.updateTime) from HrmAttendanceApprovalFetchMark m "
            + "where m.monthKey = :monthKey and m.userId = :userId and m.fetchVersion = :fetchVersion")
    Date findLastFetchTime(@Param("monthKey") String monthKey, @Param("userId") String userId,
                           @Param("fetchVersion") Integer fetchVersion);
}
