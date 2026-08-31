package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeOverTimeRecordRepository  extends JpaRepository<HrmEmployeeOverTimeRecord,Long>  {
    Optional<HrmEmployeeOverTimeRecord> findFirstByExamineId(String approveId);

    @Query("select r from HrmEmployeeOverTimeRecord r " +
            "where r.employeeId = :employeeId and (" +
            "  (r.attendanceTime is not null and r.attendanceTime between :beginDate and :endDate) or " +
            "  (r.attendanceTime is null and (" +
            "      (r.overTimeStartTime is not null and r.overTimeStartTime between :beginDate and :endDate) or " +
            "      (r.overTimeEndTime is not null and r.overTimeEndTime between :beginDate and :endDate) or " +
            "      (r.overTimeStartTime is not null and r.overTimeEndTime is not null and r.overTimeStartTime <= :endDate and r.overTimeEndTime >= :beginDate)" +
            "  ))" +
            ")")
    List<HrmEmployeeOverTimeRecord> findAllByEmployeeIdAndAttendanceTimeBetween(@Param("employeeId") Long employeeId,
                                                                                 @Param("beginDate") Date beginDate,
                                                                                 @Param("endDate") Date endDate);
}
