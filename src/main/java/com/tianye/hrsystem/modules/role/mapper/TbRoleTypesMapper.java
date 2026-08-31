package com.tianye.hrsystem.modules.role.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.entity.TbRoleTypes;
import com.tianye.hrsystem.modules.role.vo.QueryRoleTypesVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TbRoleTypesMapper extends BaseMapper<TbRoleTypes> {
    Page<QueryRoleTypesVO> queryRoleTypesList(Page<QueryRoleTypesVO> parse,
                                                          @Param("data") QueryRoleTypesBO queryRoleTypesBO);

    @Select(value = "SELECT COUNT(*) FROM hr_${companyId}${suffix}.tbloginuser WHERE roleId=#{roleId}")
    int countUsersByRole(@Param("companyId") String companyId, @Param("suffix") String suffix,
                         @Param("roleId") Integer roleId);

    @Delete(value = "DELETE FROM hr_${companyId}${suffix}.tbrolemenu WHERE role_id=#{roleId}")
    int deleteRoleMenus(@Param("companyId") String companyId, @Param("suffix") String suffix,
                        @Param("roleId") Integer roleId);
}
