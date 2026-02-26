package com.tianye.hrsystem.modules.salary.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryChangeTemplate;
import com.tianye.hrsystem.modules.salary.vo.ChangeSalaryOptionVO;

import java.util.List;

/**
 * <p>
 * 调薪模板 Mapper 接口
 * </p>
 *
 * @author hmb
 * @since 2020-11-05
 */
public interface HrmSalaryChangeTemplateMapper extends BaseMapper<HrmSalaryChangeTemplate>
{

    List<ChangeSalaryOptionVO> queryChangeSalaryOption();

}
