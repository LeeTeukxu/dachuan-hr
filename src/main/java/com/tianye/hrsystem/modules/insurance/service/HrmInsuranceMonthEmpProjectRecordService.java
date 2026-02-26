package com.tianye.hrsystem.modules.insurance.service;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpProjectRecord;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceMonthEmpProjectRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <p>
 * 员工每月参保项目表 服务实现类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Service
public class HrmInsuranceMonthEmpProjectRecordService extends BaseServiceImpl<HrmInsuranceMonthEmpProjectRecordMapper, HrmInsuranceMonthEmpProjectRecord> {

    public Map<String, Object> queryProjectCount(Long iEmpRecordId) {
        return getBaseMapper().queryProjectCount(iEmpRecordId);
    }
}
