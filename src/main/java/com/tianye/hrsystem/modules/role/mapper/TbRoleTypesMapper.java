package com.tianye.hrsystem.modules.role.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.entity.TbRoleTypes;
import com.tianye.hrsystem.modules.role.vo.QueryRoleTypesVO;
import org.apache.ibatis.annotations.Param;

public interface TbRoleTypesMapper extends BaseMapper<TbRoleTypes> {
    Page<QueryRoleTypesVO> queryRoleTypesList(Page<QueryRoleTypesVO> parse,
                                                          @Param("data") QueryRoleTypesBO queryRoleTypesBO);
}
