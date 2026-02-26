package com.tianye.hrsystem.modules.insurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceScheme;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceSchemeListVO;

import java.util.Map;

/**
 * <p>
 * 社保方案表 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface HrmInsuranceSchemeMapper extends BaseMapper<HrmInsuranceScheme> {


    Page<InsuranceSchemeListVO> queryInsuranceSchemePageList(Page<InsuranceSchemeListVO> parse);

    /**
     * 查询社保方案统计
     *
     * @param schemeId
     * @return
     */
    Map<String, Object> queryInsuranceSchemeCountById(Long schemeId);
}
