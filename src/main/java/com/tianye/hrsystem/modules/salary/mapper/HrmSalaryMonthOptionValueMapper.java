package com.tianye.hrsystem.modules.salary.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.dto.SalaryMonthOptionValueDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 每月员工薪资项表 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmSalaryMonthOptionValueMapper extends BaseMapper<HrmSalaryMonthOptionValue> {

    List<ComputeSalaryDto> queryEmpSalaryOptionValueList(Long sEmpRecordId);

    @Select(value="SELECT b.bonus FROM hrm_salary_month_emp_record AS a LEFT JOIN hrm_bonus AS b ON a.employee_id=b.employee_id AND a.year=b.year AND a.month=b.month where a.s_emp_record_id='${sEmpRecordId}'")
    String getBounsBySEmpRecordId(Long sEmpRecordId, Integer code);

    List<ComputeSalaryDto> querySalaryOptionValue(Long sEmpRecordId);


    /**
     * 按条件查询员工月工资项
     * @param params
     * @return
     */
    List<ComputeSalaryDto> queryEmpSalaryOptionList(HashMap<String,Object> params);
}
