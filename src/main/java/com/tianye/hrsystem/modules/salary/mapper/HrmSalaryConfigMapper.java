package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryBasicDto;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryConfigDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryConfigVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 薪资初始配置 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmSalaryConfigMapper extends BaseMapper<HrmSalaryConfig> {

    List<HrmSalaryConfig> queryList(HashMap<String,Object> params);

    Page<QuerySalaryConfigVO> querySalaryConfig(@Param("page") Page<HrmSalaryConfig> page, @Param("data") QuerySalaryConfigDto querySalaryConfigDto);

    @Delete("DELETE FROM hrm_salary_config WHERE config_id = #{id}")
    int deleteSalaryConfig(@Param("id") Long id);

}
