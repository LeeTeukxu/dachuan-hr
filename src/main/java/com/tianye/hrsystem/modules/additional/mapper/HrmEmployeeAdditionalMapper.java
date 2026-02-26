package com.tianye.hrsystem.modules.additional.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.entity.HrmEmployeeAdditional;
import com.tianye.hrsystem.modules.additional.vo.QueryEmployeeAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;

public interface HrmEmployeeAdditionalMapper extends BaseMapper<HrmEmployeeAdditional> {

    Page<QueryEmployeeAdditionalVO> queryEmployeeAdditionalList(Page<QueryEmployeeAdditionalVO> parse,
                                                      @Param("data") QueryAdditionalBO queryAdditionalBO);
}
