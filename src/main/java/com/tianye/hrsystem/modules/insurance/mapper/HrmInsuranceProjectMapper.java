package com.tianye.hrsystem.modules.insurance.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface HrmInsuranceProjectMapper extends BaseMapper<HrmInsuranceProject> {

    @Delete("DELETE FROM hrm_insurance_project WHERE scheme_id = #{schemeId}")
    int deleteBySchemeId(@Param("schemeId") Long schemeId);
}
