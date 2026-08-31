package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface hrmAttendanceDateShiftRepository  extends JpaRepository<HrmAttendanceDateShift,Long>  {
    Optional<HrmAttendanceDateShift> findFirstByEmployeeIdAndUserShiftTimeBetween(Long employeeId, Date begin, Date end);
    Optional<HrmAttendanceDateShift> findFirstByEmployeeIdAndUserShiftTimeLessThanEqualOrderByUserShiftTimeDesc(Long employeeId, Date end);
}
