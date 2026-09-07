package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.model.HrmFieldExtend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 扩展自定义字段表 Mapper 接口
 * </p>
 *
 * @author opencode
 * @since 2026-09-03
 */
@Mapper
public interface HrmFieldExtendMapper extends BaseMapper<HrmFieldExtend> {
    @Select("SELECT * FROM hrm_field_extend WHERE parent_field_id = #{parentFieldId}")
    List<HrmFieldExtend> findAllByParentFieldId(Integer parentFieldId);
}