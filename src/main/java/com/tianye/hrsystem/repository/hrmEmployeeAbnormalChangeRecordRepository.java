package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmEmployeeAbnormalChangeRecordRepository  extends JpaRepository<HrmEmployeeAbnormalChangeRecord, Long>  {
    int deleteAllByEmployeeIdAndType(Long empId,Integer type);
    int deleteAllByEmployeeIdIn(List<Long> ids);
}
