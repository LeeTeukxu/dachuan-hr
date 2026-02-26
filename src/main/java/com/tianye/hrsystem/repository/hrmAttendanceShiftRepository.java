package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface hrmAttendanceShiftRepository  extends JpaRepository<HrmAttendanceShift,Long>  {
        Page<HrmAttendanceShift> getAllByShiftNameLike(String shiftName,Pageable page);
        int deleteAllByGroupId(Long groupId);

}
