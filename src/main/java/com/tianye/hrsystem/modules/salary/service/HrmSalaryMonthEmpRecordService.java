package com.tianye.hrsystem.modules.salary.service;


import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthEmpRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthEmpRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 员工每月薪资记录 服务实现类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Service
public class HrmSalaryMonthEmpRecordService extends BaseServiceImpl<HrmSalaryMonthEmpRecordMapper, HrmSalaryMonthEmpRecord> {

    public List<Long> queryEmployeeIds(Long sRecordId, Collection<Long> dataAuthEmployeeIds) {
        return getBaseMapper().queryEmployeeIds(sRecordId, dataAuthEmployeeIds);
    }


}
