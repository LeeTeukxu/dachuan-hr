package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface hrmAttendancePlanRepository  extends JpaRepository<HrmAttendancePlan,Integer>  {
    int countByWorkDate(Date workDate);
    List<HrmAttendancePlan> findAllByWorkDate(Date WorkDate);
    List<HrmAttendancePlan> findAllByWorkDateAndEmpId(Date WorkDate,Long EmpID);
    int deleteAllByEmpIdAndWorkDate(Long EmpID,Date workDate);
    
    @Modifying
    @Query("DELETE FROM HrmAttendancePlan p WHERE p.empId IN :empIds AND p.workDate = :workDate")
    int deleteAllByEmpIdInAndWorkDate(@Param("empIds") List<Long> empIds, @Param("workDate") Date workDate);

    @Modifying
    @Query("DELETE FROM HrmAttendancePlan p WHERE p.empId IN :empIds AND p.workDate BETWEEN :beginDate AND :endDate")
    int deleteAllByEmpIdInAndWorkDateBetween(@Param("empIds") List<Long> empIds, @Param("beginDate") Date beginDate, @Param("endDate") Date endDate);

    int countByUserIdAndWorkDateBetween(String UserID,Date Begin,Date End);
}
