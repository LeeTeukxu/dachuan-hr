package com.tianye.hrsystem.modules.loginuser.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmDept;
import com.tianye.hrsystem.mapper.HrmDeptMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import com.tianye.hrsystem.modules.loginuser.mapper.TbLoginUserMapper;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import com.tianye.hrsystem.modules.menu.service.MenuPermissionSupport;
import com.tianye.hrsystem.modules.menu.service.TbMenuService;
import com.tianye.hrsystem.modules.menu.service.TbRoleMenuService;
import com.tianye.hrsystem.modules.role.entity.TbRoleTypes;
import com.tianye.hrsystem.modules.role.mapper.TbRoleTypesMapper;
import org.junit.Assert;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbLoginUserServiceValidationTest {

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void validateLoginUser_shouldRejectUnknownDepartmentBecauseLoginViewDependsOnDept() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        HrmDeptMapper hrmDeptMapper = mock(HrmDeptMapper.class);
        when(hrmDeptMapper.selectById(88L)).thenReturn(null);
        setFieldIfPresent(service, "hrmDeptMapper", hrmDeptMapper);
        service.tbRoleTypesMapper = roleMapperWithEnabledRole();
        service.tbRoleMenuService = roleMenuServiceWithEnabledMenu();
        service.tbMenuService = menuServiceWithEnabledMenu();
        service.menuPermissionSupport = new MenuPermissionSupport();

        QueryLoginUserBO loginUser = validLoginUser();

        try {
            invokeValidateLoginUser(service, loginUser);
            Assert.fail("创建登录用户时必须校验 depId 对应部门存在，否则登录视图会过滤账号");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalArgumentException);
            Assert.assertEquals("登录用户部门不存在", ex.getCause().getMessage());
        }
    }

    @Test
    public void validateLoginUser_shouldDefaultMissingDepartmentToTopLevelDepartmentOnCreate() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        HrmDeptMapper hrmDeptMapper = mock(HrmDeptMapper.class);
        HrmDept topDept = new HrmDept();
        topDept.setDeptId(1L);
        topDept.setParentId(0L);
        when(hrmDeptMapper.selectList(any())).thenReturn(Collections.singletonList(topDept));
        setFieldIfPresent(service, "hrmDeptMapper", hrmDeptMapper);
        service.tbRoleTypesMapper = roleMapperWithEnabledRole();
        service.tbRoleMenuService = roleMenuServiceWithEnabledMenu();
        service.tbMenuService = menuServiceWithEnabledMenu();
        service.menuPermissionSupport = new MenuPermissionSupport();

        QueryLoginUserBO loginUser = validLoginUser();
        loginUser.setDepid(null);

        try {
            invokeValidateLoginUser(service, loginUser);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof IllegalArgumentException) {
                Assert.fail("新建登录用户未传部门时不应报错，应默认使用最顶级部门: " + ex.getCause().getMessage());
            }
            throw ex;
        }

        Assert.assertEquals("新建登录用户未传部门时应默认使用最顶级部门", Long.valueOf(1L), loginUser.getDepid());
    }

    @Test
    public void deleteLoginUser_shouldDeleteTenantUserAndSystemAccountIndex() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        TbLoginUserMapper loginUserMapper = mock(TbLoginUserMapper.class);
        com.tianye.hrsystem.modules.loginuser.entity.TbLoginUser loginUser = new com.tianye.hrsystem.modules.loginuser.entity.TbLoginUser();
        loginUser.setId(7);
        loginUser.setAccount("olduser");
        when(loginUserMapper.selectById(7)).thenReturn(loginUser);
        when(loginUserMapper.deleteById(7)).thenReturn(1);
        when(loginUserMapper.deleteSystemLoginAccount("hrsystem_dev", "0003", "olduser")).thenAnswer(invocation -> {
            Assert.assertNull("删除系统库账号索引必须走默认数据源", CompanyContext.get());
            return 1;
        });
        setFieldIfPresent(service, "tbLoginUserMapper", loginUserMapper);
        setFieldIfPresent(service, "systemDatabase", "hrsystem_dev");
        LoginUserInfo tenantInfo = new LoginUserInfo();
        tenantInfo.setCompanyId("0003");
        CompanyContext.set(tenantInfo);

        Integer result = invokeDeleteLoginUser(service, 7);

        Assert.assertEquals(Integer.valueOf(0), result);
        verify(loginUserMapper).deleteById(7);
        Assert.assertSame("删除登录用户后必须恢复原租户上下文", tenantInfo, CompanyContext.get());
    }

    @Test
    public void requireSystemAccountAvailable_shouldRejectAccountMappedToOtherCompany() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        TbLoginUserMapper loginUserMapper = mock(TbLoginUserMapper.class);
        when(loginUserMapper.findSystemCompanyIdsByAccount("hrsystem_dev", "shared")).thenReturn(Collections.singletonList("0002"));
        setFieldIfPresent(service, "tbLoginUserMapper", loginUserMapper);
        setFieldIfPresent(service, "systemDatabase", "hrsystem_dev");

        try {
            invokeRequireSystemAccountAvailable(service, "shared", "0003");
            Assert.fail("总库账号索引已指向其他公司时不能创建同名登录账号");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof IllegalArgumentException);
            Assert.assertEquals("登录账号已在其他公司存在", ex.getCause().getMessage());
        }
    }

    @Test
    public void requireSystemAccountAvailable_shouldAllowDuplicateRowsForSameCompany() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        TbLoginUserMapper loginUserMapper = mock(TbLoginUserMapper.class);
        when(loginUserMapper.findSystemCompanyIdsByAccount("hrsystem_dev", "cfy")).thenReturn(Arrays.asList("0001", "0001"));
        setFieldIfPresent(service, "tbLoginUserMapper", loginUserMapper);
        setFieldIfPresent(service, "systemDatabase", "hrsystem_dev");

        invokeRequireSystemAccountAvailable(service, "cfy", "0001");
    }

    @Test
    public void resolveCompanyId_shouldPreferCurrentTenantContextOverRequestCompanyId() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        QueryLoginUserBO loginUser = validLoginUser();
        loginUser.setCompanyId("0001");
        LoginUserInfo tenantInfo = new LoginUserInfo();
        tenantInfo.setCompanyId("0003");
        CompanyContext.set(tenantInfo);

        String companyId = invokeResolveCompanyId(service, loginUser);

        Assert.assertEquals("登录用户保存系统索引必须使用当前租户，不能信任请求体 companyId", "0003", companyId);
    }

    @Test
    public void systemAccountLookup_shouldUseDefaultCompanyContextAndRestoreTenantContext() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        TbLoginUserMapper loginUserMapper = mock(TbLoginUserMapper.class);
        when(loginUserMapper.findSystemCompanyIdsByAccount("hrsystem_dev", "newuser")).thenAnswer(invocation -> {
            Assert.assertNull("系统库账号索引查询必须走默认数据源", CompanyContext.get());
            return Collections.singletonList("0003");
        });
        setFieldIfPresent(service, "tbLoginUserMapper", loginUserMapper);
        setFieldIfPresent(service, "systemDatabase", "hrsystem_dev");
        LoginUserInfo tenantInfo = new LoginUserInfo();
        tenantInfo.setCompanyId("0003");
        CompanyContext.set(tenantInfo);

        invokeRequireSystemAccountAvailable(service, "newuser", "0003");

        Assert.assertSame("系统库账号索引查询后必须恢复原租户上下文", tenantInfo, CompanyContext.get());
    }

    @Test
    public void syncSystemLoginAccount_shouldUseDefaultCompanyContextForDeleteAndSave() throws Exception {
        TbLoginUserService service = new TbLoginUserService();
        TbLoginUserMapper loginUserMapper = mock(TbLoginUserMapper.class);
        when(loginUserMapper.deleteSystemLoginAccount("hrsystem_dev", "0003", "olduser")).thenAnswer(invocation -> {
            Assert.assertNull("系统库旧账号索引删除必须走默认数据源", CompanyContext.get());
            return 1;
        });
        when(loginUserMapper.deleteSystemLoginAccount("hrsystem_dev", "0003", "newuser")).thenAnswer(invocation -> {
            Assert.assertNull("系统库新账号索引去重必须走默认数据源", CompanyContext.get());
            return 1;
        });
        when(loginUserMapper.saveSystemLoginAccount(eq("hrsystem_dev"), any(QueryLoginUserBO.class))).thenAnswer(invocation -> {
            Assert.assertNull("系统库账号索引保存必须走默认数据源", CompanyContext.get());
            return 1;
        });
        setFieldIfPresent(service, "tbLoginUserMapper", loginUserMapper);
        setFieldIfPresent(service, "systemDatabase", "hrsystem_dev");
        LoginUserInfo tenantInfo = new LoginUserInfo();
        tenantInfo.setCompanyId("0003");
        CompanyContext.set(tenantInfo);

        invokeSyncSystemLoginAccount(service, false, "0003", "olduser", "newuser", validLoginUser());

        verify(loginUserMapper).deleteSystemLoginAccount("hrsystem_dev", "0003", "olduser");
        verify(loginUserMapper).deleteSystemLoginAccount("hrsystem_dev", "0003", "newuser");
        Assert.assertSame("系统库账号索引保存后必须恢复原租户上下文", tenantInfo, CompanyContext.get());
    }

    private QueryLoginUserBO validLoginUser() {
        QueryLoginUserBO loginUser = new QueryLoginUserBO();
        loginUser.setAccount("newuser");
        loginUser.setPassword("123456");
        loginUser.setDepid(88L);
        loginUser.setRoleid(2);
        return loginUser;
    }

    private TbRoleTypesMapper roleMapperWithEnabledRole() {
        TbRoleTypesMapper mapper = mock(TbRoleTypesMapper.class);
        TbRoleTypes role = new TbRoleTypes();
        role.setId(2);
        role.setCanUse(1);
        when(mapper.selectById(2)).thenReturn(role);
        return mapper;
    }

    @SuppressWarnings("unchecked")
    private TbRoleMenuService roleMenuServiceWithEnabledMenu() {
        TbRoleMenuService service = mock(TbRoleMenuService.class);
        LambdaQueryChainWrapper<TbRoleMenu> wrapper = mock(LambdaQueryChainWrapper.class);
        TbRoleMenu roleMenu = new TbRoleMenu();
        roleMenu.setRoleId(2);
        roleMenu.setMenuId(11);
        when(service.lambdaQuery()).thenReturn(wrapper);
        when(wrapper.eq(any(SFunction.class), eq(2))).thenReturn(wrapper);
        when(wrapper.list()).thenReturn(Collections.singletonList(roleMenu));
        return service;
    }

    private TbMenuService menuServiceWithEnabledMenu() {
        TbMenuService service = mock(TbMenuService.class);
        when(service.queryAllMenus()).thenReturn(Arrays.asList(menu(10, 0), menu(11, 10)));
        return service;
    }

    private tbmenu menu(Integer id, Integer pid) {
        tbmenu menu = new tbmenu();
        menu.setId(id);
        menu.setPid(pid);
        menu.setCanUse(1);
        return menu;
    }

    private void invokeValidateLoginUser(TbLoginUserService service, QueryLoginUserBO loginUser) throws Exception {
        Method method = TbLoginUserService.class.getDeclaredMethod("validateLoginUser", QueryLoginUserBO.class);
        method.setAccessible(true);
        method.invoke(service, loginUser);
    }

    private void invokeRequireSystemAccountAvailable(TbLoginUserService service, String account, String companyId) throws Exception {
        Method method = TbLoginUserService.class.getDeclaredMethod("requireSystemAccountAvailable", String.class, String.class);
        method.setAccessible(true);
        method.invoke(service, account, companyId);
    }

    private void invokeSyncSystemLoginAccount(TbLoginUserService service,
                                              boolean isCreate,
                                              String companyId,
                                              String oldAccount,
                                              String account,
                                              QueryLoginUserBO loginUser) throws Exception {
        Method method = TbLoginUserService.class.getDeclaredMethod(
                "syncSystemLoginAccount",
                boolean.class,
                String.class,
                String.class,
                String.class,
                QueryLoginUserBO.class
        );
        method.setAccessible(true);
        method.invoke(service, isCreate, companyId, oldAccount, account, loginUser);
    }

    private Integer invokeDeleteLoginUser(TbLoginUserService service, Integer id) throws Exception {
        Method method;
        try {
            method = TbLoginUserService.class.getDeclaredMethod("Delete", Integer.class);
        } catch (NoSuchMethodException ex) {
            Assert.fail("登录用户服务必须提供 Delete(Integer id) 删除入口");
            return null;
        }
        method.setAccessible(true);
        return (Integer) method.invoke(service, id);
    }

    private String invokeResolveCompanyId(TbLoginUserService service, QueryLoginUserBO loginUser) throws Exception {
        Method method = TbLoginUserService.class.getDeclaredMethod("resolveCompanyId", QueryLoginUserBO.class);
        method.setAccessible(true);
        return (String) method.invoke(service, loginUser);
    }

    private void setFieldIfPresent(Object target, String fieldName, Object value) throws Exception {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException ignored) {
        }
    }
}
