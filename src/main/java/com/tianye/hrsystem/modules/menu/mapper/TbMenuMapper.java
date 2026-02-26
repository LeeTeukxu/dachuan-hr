package com.tianye.hrsystem.modules.menu.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.menu.bo.QueryMenuBO;
import com.tianye.hrsystem.modules.menu.entity.TbMenu;
import com.tianye.hrsystem.modules.menu.vo.QueryMenuVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TbMenuMapper extends BaseMapper<TbMenu> {
    List<QueryMenuVO> queryMenuList(@Param("data") QueryMenuBO queryMenuBO);
}
