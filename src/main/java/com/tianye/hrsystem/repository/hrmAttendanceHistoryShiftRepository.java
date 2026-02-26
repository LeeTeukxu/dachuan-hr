package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface hrmAttendanceHistoryShiftRepository extends JpaRepository<HrmAttendanceHistoryShift,Integer>  {
    Optional<HrmAttendanceHistoryShift> findFirstByUpdateTimeBetween(Date Begin, Date End);
}
