package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.model.LoginUserInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    LoginUserInfo getByAcountAndCompanyID(@Param("account") String account,@Param("companyId") String companyId,@Param("suffix") String suffix);

    /**
     * SaaS 改造 P2：查询账号可登录的企业列表（含企业名，供前端选择）
     */
    @Select(value = "SELECT DISTINCT a.CompanyID AS companyId, c.companyName AS companyName " +
            "FROM ${database}.tbAllUserList a LEFT JOIN ${database}.tbcompanylist c ON a.CompanyID = c.companyId " +
            "WHERE a.account=#{userName} ORDER BY a.CompanyID")
    List<java.util.Map<String, Object>> getCompaniesByUserName(@Param("userName") String userName, @Param("database") String database);

    /**
     * SaaS 安全改造 P0-2：旧 MD5 密码登录成功后透明升级为 BCrypt
     */
    @Update(value="UPDATE hr_${companyId}${suffix}.tbloginuser SET password=#{newHash}, setPassword=#{newHash} WHERE Account=#{account}")
    int upgradePasswordToBcrypt(@Param("account") String account, @Param("companyId") String companyId,
                                @Param("suffix") String suffix, @Param("newHash") String newHash);

    // ========== 企业权限管理 ==========

    @Select(value = "SELECT a.account, a.companyId, c.companyName " +
            "FROM ${database}.tbAllUserList a " +
            "LEFT JOIN ${database}.tbcompanylist c ON a.CompanyID = c.companyId " +
            "ORDER BY a.account, a.companyId")
    List<java.util.Map<String, Object>> listAccountCompanyPermissions(@Param("database") String database);

    @Select(value = "SELECT companyId FROM ${database}.tbAllUserList WHERE account=#{account}")
    List<String> getAccountCompanyIds(@Param("database") String database, @Param("account") String account);

    @Delete(value = "DELETE FROM ${database}.tbAllUserList WHERE account=#{account}")
    int deleteAccountCompanies(@Param("database") String database, @Param("account") String account);

    @Insert(value = "INSERT INTO ${database}.tbAllUserList (account, companyId) VALUES (#{account}, #{companyId})")
    int insertAccountCompany(@Param("database") String database, @Param("account") String account, @Param("companyId") String companyId);

    @Select(value = "SELECT companyId, companyName FROM ${database}.tbcompanylist ORDER BY companyId")
    List<java.util.Map<String, Object>> getAllCompanies(@Param("database") String database);

    @Insert(value = "INSERT INTO hr_${companyId}${suffix}.tbloginuser " +
            "(name, account, password, depId, roleId, canLogin, setPassword, createtime) " +
            "VALUES (#{name}, #{account}, #{password}, #{depId}, #{roleId}, 1, #{password}, NOW())")
    int insertTenantAccount(@Param("companyId") String companyId, @Param("suffix") String suffix,
                            @Param("name") String name, @Param("account") String account,
                            @Param("password") String password, @Param("depId") Long depId,
                            @Param("roleId") Integer roleId);

    @Select(value = "SELECT COUNT(*) FROM hr_${companyId}${suffix}.tbroletypes WHERE id=#{roleId}")
    int countRoleExists(@Param("companyId") String companyId, @Param("suffix") String suffix,
                        @Param("roleId") Integer roleId);

    @Select(value = "SELECT COUNT(*) FROM hr_${companyId}${suffix}.hrm_dept WHERE dept_id=#{depId}")
    int countDeptExists(@Param("companyId") String companyId, @Param("suffix") String suffix,
                        @Param("depId") Long depId);

    @Select(value = "SELECT dept_id FROM hr_${companyId}${suffix}.hrm_dept " +
            "WHERE parent_id=0 ORDER BY dept_id LIMIT 1")
    Long findTopDeptId(@Param("companyId") String companyId, @Param("suffix") String suffix);

    // ========== 企业权限：角色与菜单权限（按当前登录库角色基准，同步到目标企业）==========

    @Select(value = "SELECT name FROM hr_${companyId}${suffix}.tbroletypes WHERE id=#{roleId}")
    String getRoleName(@Param("companyId") String companyId, @Param("suffix") String suffix,
                       @Param("roleId") Integer roleId);

    @Select(value = "SELECT menu_id FROM hr_${companyId}${suffix}.tbrolemenu WHERE role_id=#{roleId}")
    List<Integer> getRoleMenuIds(@Param("companyId") String companyId, @Param("suffix") String suffix,
                                 @Param("roleId") Integer roleId);

    @Insert(value = "INSERT INTO hr_${companyId}${suffix}.tbroletypes (id, name, canUse) VALUES (#{roleId}, #{roleName}, 1)")
    int insertRole(@Param("companyId") String companyId, @Param("suffix") String suffix,
                   @Param("roleId") Integer roleId, @Param("roleName") String roleName);

    @Delete(value = "DELETE FROM hr_${companyId}${suffix}.tbrolemenu WHERE role_id=#{roleId}")
    int deleteRoleMenu(@Param("companyId") String companyId, @Param("suffix") String suffix,
                       @Param("roleId") Integer roleId);

    @Insert(value = "INSERT INTO hr_${companyId}${suffix}.tbmenu (id, pid, name, path, canuse, component, redirect, icon, showMenu) " +
            "SELECT id, pid, name, path, canuse, component, redirect, icon, showMenu FROM hr_${baseCompanyId}${suffix}.tbmenu " +
            "WHERE id = #{menuId} AND NOT EXISTS (SELECT 1 FROM hr_${companyId}${suffix}.tbmenu WHERE id = #{menuId})")
    int insertMenuIfMissing(@Param("baseCompanyId") String baseCompanyId, @Param("companyId") String companyId,
                            @Param("suffix") String suffix, @Param("menuId") Integer menuId);

    @Insert(value = "INSERT INTO hr_${companyId}${suffix}.tbrolemenu (role_menu_id, role_id, menu_id) VALUES (#{roleMenuId}, #{roleId}, #{menuId})")
    int insertRoleMenu(@Param("companyId") String companyId, @Param("suffix") String suffix,
                       @Param("roleMenuId") long roleMenuId, @Param("roleId") Integer roleId, @Param("menuId") Integer menuId);

    @Update(value = "UPDATE hr_${companyId}${suffix}.tbloginuser SET roleId=#{roleId} WHERE account=#{account}")
    int updateTenantAccountRole(@Param("companyId") String companyId, @Param("suffix") String suffix,
                                @Param("account") String account, @Param("roleId") Integer roleId);

    // ========== 密码策略（A：首次登录强改 / 锁定；B：超管重置）==========

    @Select(value = "SELECT pwd_change_required FROM hr_${companyId}${suffix}.tbloginuser WHERE account=#{account}")
    Integer getPwdChangeRequired(@Param("account") String account, @Param("companyId") String companyId, @Param("suffix") String suffix);

    @Select(value = "SELECT change_pwd_fail FROM hr_${companyId}${suffix}.tbloginuser WHERE account=#{account}")
    Integer getChangePwdFail(@Param("account") String account, @Param("companyId") String companyId, @Param("suffix") String suffix);

    @Select(value = "SELECT admin_reset_count FROM hr_${companyId}${suffix}.tbloginuser WHERE account=#{account}")
    Integer getAdminResetCount(@Param("account") String account, @Param("companyId") String companyId, @Param("suffix") String suffix);

    /** 查询指定租户的系统管理员（roleId=2）账号 */
    @Select(value = "SELECT account FROM hr_${companyId}${suffix}.tbloginuser WHERE roleId=2 ORDER BY id LIMIT 1")
    String getAdminAccountByCompany(@Param("companyId") String companyId, @Param("suffix") String suffix);

    /** A：自助改密成功——更新哈希并清除强改/失败计数 */
    @Update(value = "UPDATE hr_${companyId}${suffix}.tbloginuser " +
            "SET password=#{newHash}, setPassword=#{newHash}, pwd_change_required=0, change_pwd_fail=0 " +
            "WHERE account=#{account}")
    int selfChangePassword(@Param("account") String account, @Param("companyId") String companyId,
                           @Param("suffix") String suffix, @Param("newHash") String newHash);

    /** A：自助改密原密码错误——失败次数+1，满 5 次锁定(canLogin=0) */
    @Update(value = "UPDATE hr_${companyId}${suffix}.tbloginuser " +
            "SET change_pwd_fail = change_pwd_fail + 1, " +
            "canLogin = CASE WHEN change_pwd_fail + 1 >= 5 THEN 0 ELSE canLogin END " +
            "WHERE account=#{account}")
    int incChangePwdFail(@Param("account") String account, @Param("companyId") String companyId,
                         @Param("suffix") String suffix);

    /** B：平台超管重置——写入新哈希、强制下次改密、解锁并清零失败、重置计数+1 */
    @Update(value = "UPDATE hr_${companyId}${suffix}.tbloginuser " +
            "SET password=#{newHash}, setPassword=#{newHash}, pwd_change_required=1, canLogin=1, " +
            "change_pwd_fail=0, admin_reset_count=admin_reset_count+1 " +
            "WHERE account=#{account}")
    int adminResetPassword(@Param("account") String account, @Param("companyId") String companyId,
                          @Param("suffix") String suffix, @Param("newHash") String newHash);

    /** 编辑改密：将新密码同步到账号已授权的指定企业（企业权限管理支持一号多企业） */
    @Update(value = "UPDATE hr_${companyId}${suffix}.tbloginuser " +
            "SET password=#{newHash}, setPassword=#{newHash} " +
            "WHERE account=#{account}")
    int updateAccountPassword(@Param("account") String account, @Param("companyId") String companyId,
                             @Param("suffix") String suffix, @Param("newHash") String newHash);
}
