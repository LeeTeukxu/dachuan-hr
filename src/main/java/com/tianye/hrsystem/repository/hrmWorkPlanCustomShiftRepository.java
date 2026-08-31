package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmWorkPlanCustomShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface hrmWorkPlanCustomShiftRepository extends JpaRepository<HrmWorkPlanCustomShift, Long> {

    Optional<HrmWorkPlanCustomShift> findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
            String start1, String end1, Integer crossDay, String shiftPeriod);

    Optional<HrmWorkPlanCustomShift> findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
            String start1, String end1, Integer crossDay, String shiftPeriod, Integer continuousShift);
}
