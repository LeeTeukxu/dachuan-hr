package com.tianye.hrsystem.modules.insurance.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.insurance.dto.InsuranceSchemeDto;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceScheme;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceSchemeListVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface HrmInsuranceSechemeMapper extends BaseMapper<HrmInsuranceScheme> {
    BasePage<InsuranceSchemeListVO> index(BasePage<InsuranceSchemeListVO> parse);

    @Delete("DELETE FROM hrm_insurance_scheme WHERE scheme_id = #{schemeId}")
    int deleteBySchemeId(@Param("schemeId") Long schemeId);

    /**
     * 查询社保方案统计
     *
     * @param schemeId
     * @return
     */
    Map<String, Object> queryInsuranceSchemeCountById(Long schemeId);
}
