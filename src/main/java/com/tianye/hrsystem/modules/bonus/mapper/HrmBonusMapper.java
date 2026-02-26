package com.tianye.hrsystem.modules.bonus.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonus;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
import org.apache.ibatis.annotations.Param;

public interface HrmBonusMapper extends BaseMapper<HrmBonus>
{
    HrmBonus getEmpBonus(@Param("employeeId") Long employeeId,@Param("year") Integer year,@Param("month") Integer month);

    Page<QueryBounsVO> queryHrmBonusList(Page<QueryBounsVO> parse,
                                                @Param("data") QueryBonusBO queryBonusBO);
}
