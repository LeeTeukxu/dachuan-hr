package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.model.LoginUserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @ClassName: LoginUserMapper
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 16:04
 **/
@Mapper
public interface LoginUserMapper {
    @Select(value = "Select CompanyID from ${database}.tbAllUserList Where account=#{userName}")
    String   getCompanyIdByUserName(String userName,String database);
    @Select(value="Select UserID,UserName,DepName,RoleName,DepID,RoleID,'${companyId}' as CompanyID,Password,CanLogin from hr_${companyId}${suffix}.view_LoginUser Where Account=#{account}")
    LoginUserInfo getByAcountAndCompanyID(String account,String companyId,String suffix);
}
