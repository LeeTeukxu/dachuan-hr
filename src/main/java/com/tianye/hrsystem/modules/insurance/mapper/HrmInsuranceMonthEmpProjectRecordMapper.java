package com.tianye.hrsystem.modules.insurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpProjectRecord;

import java.util.Map;

/**
 * <p>
 * 员工每月参保项目表 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmInsuranceMonthEmpProjectRecordMapper extends BaseMapper<HrmInsuranceMonthEmpProjectRecord> {

    Map<String, Object> queryProjectCount(Long iEmpRecordId);
}
