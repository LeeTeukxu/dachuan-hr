package com.tianye.hrsystem.modules.bonus.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonusTaxOnly;
import org.apache.ibatis.annotations.Param;

public interface HrmBonusTaxOnlyMapper extends BaseMapper<HrmBonusTaxOnly> {

    HrmBonusTaxOnly getEmpTaxOnlyBonus(@Param("employeeId") Long employeeId,
                                       @Param("year") Integer year,
                                       @Param("month") Integer month);
}
