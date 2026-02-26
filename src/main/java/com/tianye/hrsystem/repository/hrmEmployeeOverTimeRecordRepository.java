package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeOverTimeRecordRepository  extends JpaRepository<HrmEmployeeOverTimeRecord,Long>  {
    Optional<HrmEmployeeOverTimeRecord> findFirstByExamineId(String approveId);
}
