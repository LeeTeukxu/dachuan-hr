package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryOption;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryOptionTemplate;

import java.util.List;

/**
 * <p>
 * 系统薪资项 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmSalaryOptionMapper extends BaseMapper<HrmSalaryOption> {

    List<HrmSalaryOptionTemplate> querySalaryOptionTemplateList();

}
