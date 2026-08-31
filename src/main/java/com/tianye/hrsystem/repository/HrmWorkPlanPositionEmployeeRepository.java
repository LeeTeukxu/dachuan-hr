package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanPositionEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrmWorkPlanPositionEmployeeRepository extends JpaRepository<HrmWorkPlanPositionEmployee, Long> {
    List<HrmWorkPlanPositionEmployee> findAllByPositionIdInOrderBySortAscIdAsc(List<Long> positionIds);

    void deleteAllByPositionId(Long positionId);

    void deleteAllByPositionIdIn(List<Long> positionIds);
}
