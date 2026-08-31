package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanProductPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrmWorkPlanProductPositionRepository extends JpaRepository<HrmWorkPlanProductPosition, Long> {
    List<HrmWorkPlanProductPosition> findAllByProductIdInOrderBySortAscIdAsc(List<Long> productIds);

    List<HrmWorkPlanProductPosition> findAllByProductIdOrderBySortAscIdAsc(Long productId);

    HrmWorkPlanProductPosition findFirstByProductIdOrderBySortDescIdDesc(Long productId);

    List<HrmWorkPlanProductPosition> findAllByProductId(Long productId);

    void deleteAllByProductId(Long productId);
}
