package com.tianye.hrsystem.mapper;


import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmEmployeeContract;

import java.util.List;

/**
 * <p>
 * 员工合同 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface HrmEmployeeContractMapper extends BaseMapper<HrmEmployeeContract> {

    List<Long> queryToExpireContractCount();


}
