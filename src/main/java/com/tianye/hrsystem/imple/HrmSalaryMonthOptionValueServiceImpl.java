package com.tianye.hrsystem.imple;


import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthOptionValueMapper;
import com.tianye.hrsystem.service.IHrmSalaryMonthOptionValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 每月员工薪资项表 服务实现类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Service
public class HrmSalaryMonthOptionValueServiceImpl extends BaseServiceImpl<HrmSalaryMonthOptionValueMapper, HrmSalaryMonthOptionValue> implements IHrmSalaryMonthOptionValueService {
//
//    @Autowired
//    private HrmSalaryMonthOptionValueMapper optionValueMapper;
//
//    @Override
//    public List<ComputeSalaryDto> querySalaryOptionValue(Long sEmpRecordId) {
//        return optionValueMapper.querySalaryOptionValue(sEmpRecordId);
//    }
}
