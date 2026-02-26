package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface hrmAttendanceReportDataRepository  extends JpaRepository<HrmAttendanceReportData,Integer>  {
        int countAllByEmpIdAndWorkDate(Long empId, Date workDate);
        int countAllByEmpIdAndWorkDateBetween(Long empId,Date begin,Date End);
        List<HrmAttendanceReportData> findAllByWorkDateBetweenOrderByWorkDate(Date Begin,Date End);
        List<HrmAttendanceReportData> findAllByEmpIdInAndFieldIdInAndWorkDateIn(List<Long> empIds,List<Long> fieldIds,List<Date> dates);
        Integer countByWorkDateBetween(Date Begin,Date End);
        List<HrmAttendanceReportData> findAllByWorkDate(Date workDate);
        int countAllByEmpIdAndWorkDateBetweenAndFieldIdIn(Long empId,Date Begin,Date End,List<Long> fieldIds);
        int deleteAllByEmpIdAndWorkDateBetweenAndFieldIdIn(Long empId,Date Begin,Date End,List<Long> fieldIds);
        int deleteAllByEmpIdInAndWorkDateBetween(List<Long> empIds,Date Begin,Date End);
}
