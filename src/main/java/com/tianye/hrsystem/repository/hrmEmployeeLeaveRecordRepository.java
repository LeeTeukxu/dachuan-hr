package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeLeaveRecordRepository  extends JpaRepository<HrmEmployeeLeaveRecord,Long>  {
    Optional<HrmEmployeeLeaveRecord> findFirstByExamineId(String approveId);
    List<HrmEmployeeLeaveRecord> findAllByLeaveTypeAndLeaveDay(String leaveType,String leaveDay);
}
