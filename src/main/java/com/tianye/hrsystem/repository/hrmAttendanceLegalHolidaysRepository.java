package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface hrmAttendanceLegalHolidaysRepository  extends JpaRepository<HrmAttendanceLegalHolidays,Long>  {

    List<HrmAttendanceLegalHolidays> findAllByHolidayTimeGreaterThanEqualAndHolidayTimeLessThan(Date beginTime, Date endTime);
}
