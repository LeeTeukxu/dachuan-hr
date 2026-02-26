package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.modules.salary.dto.DictDataQueryDto;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-13
 */
public interface HrmDictDataMapper extends BaseMapper<tbdictdata>
{
    Page<tbdictdata> selectPageVo(@Param("page") Page<tbdictdata> page, @Param("data") DictDataQueryDto dictDataQueryDto);
}
