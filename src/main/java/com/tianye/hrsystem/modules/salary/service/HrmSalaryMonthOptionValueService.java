package com.tianye.hrsystem.modules.salary.service;


import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.dto.SalaryMonthOptionValueDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthOptionValueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 每月员工薪资项表 服务实现类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Service
public class HrmSalaryMonthOptionValueService extends BaseServiceImpl<HrmSalaryMonthOptionValueMapper, HrmSalaryMonthOptionValue> {

    @Autowired
    private HrmSalaryMonthOptionValueMapper optionValueMapper;

    public List<ComputeSalaryDto> queryEmpSalaryOptionValueList(Long sEmpRecordId) {
        return optionValueMapper.queryEmpSalaryOptionValueList(sEmpRecordId);
    }
    public String getBounsBySEmpRecordId(Long sEmpRecordId, Integer code) {
        return optionValueMapper.getBounsBySEmpRecordId(sEmpRecordId,code);
    }

    public List<ComputeSalaryDto> querySalaryOptionValue(Long sEmpRecordId) {
        return optionValueMapper.querySalaryOptionValue(sEmpRecordId);
    }


    /**
     * 按条件查询员工月工资项
     * @param params
     * @return
     */
    public List<ComputeSalaryDto> queryEmpSalaryOptionList(HashMap<String,Object> params)
    {
        return optionValueMapper.queryEmpSalaryOptionList(params);
    }
}
