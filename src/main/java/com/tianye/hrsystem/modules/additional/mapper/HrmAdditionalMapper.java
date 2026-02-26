package com.tianye.hrsystem.modules.additional.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.deduction.vo.QueryPersonalIncomeTaxVO;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;

public interface HrmAdditionalMapper extends BaseMapper<HrmAdditional> {

    Page<QueryAdditionalVO> queryAdditionalList(Page<QueryAdditionalVO> parse,
                                                       @Param("data") QueryAdditionalBO queryAdditionalBO);


    QueryAdditionalVO getEmployeeAdditional(HashMap<String,Object> params);
}
