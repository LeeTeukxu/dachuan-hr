package com.tianye.hrsystem.repository;

import cn.hutool.core.date.DateTime;
import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface hrmAttendanceClockRepository  extends JpaRepository<HrmAttendanceClock,Long>  {
    List<HrmAttendanceClock> findAllByClockTimeBetween(Date begin,Date end);
    List<HrmAttendanceClock> findAllByClockEmployeeIdAndClockTimeBetween(Long empId,Date begin,Date end);
    int deleteAllByClockEmployeeIdInAndWorkDateBetween(List<Long> empId,Date begin,Date end);
}
