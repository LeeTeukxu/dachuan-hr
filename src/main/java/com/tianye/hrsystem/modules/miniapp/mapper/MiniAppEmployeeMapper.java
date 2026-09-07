package com.tianye.hrsystem.modules.miniapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MiniAppEmployeeMapper {

    @Update("UPDATE hrm_employee SET openid = #{openid}, update_time = NOW() " +
            "WHERE employee_id = #{employeeId} AND (openid IS NULL OR openid = '')")
    int bindOpenidIfEmpty(@Param("employeeId") Long employeeId,
                          @Param("openid") String openid);
}
