package com.tianye.hrsystem.modules.miniapp.mapper;

import com.tianye.hrsystem.modules.miniapp.entity.MiniAppUserBinding;
import com.tianye.hrsystem.modules.miniapp.vo.CompanyOptionVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 小程序登录用系统库查询（不使用多租户动态数据源，直连系统库 hr_system）
 */
@Mapper
public interface MiniAppSystemMapper {

    @Select("SELECT openid, unionid, phone, employee_id, company_id, create_time " +
            "FROM miniapp_user_binding WHERE openid = #{openid} LIMIT 1")
    MiniAppUserBinding findByOpenid(@Param("openid") String openid);

    @Select("SELECT company_id, company_name FROM tbCompanyList")
    List<CompanyOptionVO> listAllCompanies();

    @Insert("INSERT INTO miniapp_user_binding(openid, unionid, phone, employee_id, company_id, create_time) " +
            "VALUES(#{openid}, #{unionid}, #{phone}, #{employeeId}, #{companyId}, NOW())")
    int insertBinding(MiniAppUserBinding binding);
}