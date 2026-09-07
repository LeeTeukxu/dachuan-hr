package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmAttendanceJudgeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface hrmAttendanceJudgeResultRepository extends JpaRepository<HrmAttendanceJudgeResult, Long> {

    Optional<HrmAttendanceJudgeResult> findByEmpIdAndWorkDate(Long empId, Date workDate);

    List<HrmAttendanceJudgeResult> findByEmpIdAndWorkDateBetweenOrderByWorkDateAsc(Long empId, Date begin, Date end);

    List<HrmAttendanceJudgeResult> findByWorkDateBetweenOrderByWorkDateAscEmpIdAsc(Date begin, Date end);

    void deleteByEmpIdAndWorkDate(Long empId, Date workDate);
}
