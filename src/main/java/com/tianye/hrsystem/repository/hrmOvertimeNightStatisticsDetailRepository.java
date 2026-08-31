package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface hrmOvertimeNightStatisticsDetailRepository extends JpaRepository<HrmOvertimeNightStatisticsDetail, Long> {

    @Transactional
    @Modifying
    @Query("delete from HrmOvertimeNightStatisticsDetail d where d.statYear = :statYear and d.statMonth = :statMonth")
    int deleteAllByStatYearAndStatMonth(@Param("statYear") Integer statYear, @Param("statMonth") Integer statMonth);

    @Transactional
    @Modifying
    @Query("delete from HrmOvertimeNightStatisticsDetail d where d.workDate between :beginDate and :endDate")
    int deleteAllByWorkDateBetween(@Param("beginDate") Date beginDate, @Param("endDate") Date endDate);

    @Transactional
    @Modifying
    @Query("delete from HrmOvertimeNightStatisticsDetail d where d.employeeId = :employeeId and d.workDate between :beginDate and :endDate")
    int deleteAllByEmployeeIdAndWorkDateBetween(@Param("employeeId") Long employeeId,
                                                @Param("beginDate") Date beginDate,
                                                @Param("endDate") Date endDate);

    List<HrmOvertimeNightStatisticsDetail> findAllByStatYearAndStatMonthAndEmployeeIdIn(Integer statYear, Integer statMonth, List<Long> employeeIds);

    List<HrmOvertimeNightStatisticsDetail> findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(Integer statYear, Integer statMonth);

    List<HrmOvertimeNightStatisticsDetail> findAllByEmployeeIdOrderByWorkDateDesc(Long employeeId);

    List<HrmOvertimeNightStatisticsDetail> findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(Long employeeId, Integer statYear, Integer statMonth);
}
