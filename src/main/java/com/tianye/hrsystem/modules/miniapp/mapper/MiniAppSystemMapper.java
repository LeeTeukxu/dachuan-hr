package com.tianye.hrsystem.modules.miniapp.mapper;

import com.tianye.hrsystem.modules.miniapp.vo.CompanyOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 小程序登录用系统库查询（不使用多租户动态数据源，直连系统库 hrsystem）。
 */
@Mapper
public interface MiniAppSystemMapper {

    @Select("SELECT companyId, companyName FROM tbCompanyList")
    List<CompanyOptionVO> listAllCompanies();
}
