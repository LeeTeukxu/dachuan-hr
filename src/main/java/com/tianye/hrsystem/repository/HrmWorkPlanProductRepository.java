package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrmWorkPlanProductRepository extends JpaRepository<HrmWorkPlanProduct, Long> {
    List<HrmWorkPlanProduct> findAllByOrderBySortAscIdAsc();

    HrmWorkPlanProduct findFirstByOrderBySortDescIdDesc();

    HrmWorkPlanProduct findByProductName(String productName);
}
