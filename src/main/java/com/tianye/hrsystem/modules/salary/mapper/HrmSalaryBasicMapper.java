package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryBasicDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface HrmSalaryBasicMapper extends BaseMapper<HrmSalaryBasic> {
    Page<QuerySalaryBasicVO> querySalaryBasic(@Param("page") Page<HrmSalaryBasic> page, @Param("data") QuerySalaryBasicDto querySalaryBasicDto);

    @Delete("DELETE FROM hrm_salary_basic WHERE id = #{id}")
    int deleteSalaryBasic(@Param("id") Long id);
}
