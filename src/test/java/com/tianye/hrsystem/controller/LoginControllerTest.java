package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.MD5Utils;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.model.tbrolemenu;
import com.tianye.hrsystem.modules.menu.service.MenuPermissionSupport;
import com.tianye.hrsystem.modules.menu.service.TbMenuService;
import com.tianye.hrsystem.repository.rolemenuRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoginControllerTest {

    private static final String CAPTCHA_ID = "captcha-test-id";
    private static final String CAPTCHA_CODE = "ABCD";

    @InjectMocks
    private LoginController controller;

    @Mock
    private LoginUserMapper userMapper;

    @Mock
    private rolemenuRepository rolemenuRepository;

    @Mock
    private TbMenuService tbMenuService;

    @Mock
    private Redis redis;

    @Mock
    private com.tianye.hrsystem.common.TokenRevocationService tokenRevocation;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        controller.systemBase = "hrsystem";
        controller.databasesuffix = "";
        controller.menuPermissionSupport = new MenuPermissionSupport();
        // SaaS P0-2：注入真实密码服务（BCrypt/MD5 自适应）
        controller.passwordService = new com.tianye.hrsystem.common.PasswordService();
        CompanyContext.clear();
    }

    @After
    public void tearDown() throws Exception {
        CompanyContext.clear();
    }

    /** 模拟通过校验的验证码 */
    private void stubValidCaptcha() {
        when(redis.exists(anyString())).thenReturn(false);
        when(redis.get("hr:captcha:" + CAPTCHA_ID)).thenReturn((Object) CAPTCHA_CODE);
    }

    private Map<String, Object> company(String companyId, String companyName) {
        Map<String, Object> map = new HashMap<>();
        map.put("companyId", companyId);
        map.put("companyName", companyName);
        return map;
    }

    @Test
    public void Login_shouldSetCompanyContext_beforeLoadingPermissionMenus() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("hbadmin", "hrsystem"))
                .thenReturn(Arrays.asList(company("0003", "湖北公司")));
        when(userMapper.getByAcountAndCompanyID("hbadmin", "0003", "")).thenReturn(loginUser());
        when(rolemenuRepository.getAllByRoleId(2)).thenAnswer(invocation -> {
            Assert.assertNotNull("登录加载角色菜单前应设置租户上下文", CompanyContext.get());
            Assert.assertEquals("0003", CompanyContext.get().getCompanyId());
            return Arrays.asList(roleMenu(2, 10), roleMenu(2, 11));
        });
        when(tbMenuService.queryAllMenus()).thenAnswer(invocation -> {
            Assert.assertNotNull("登录加载菜单前应设置租户上下文", CompanyContext.get());
            Assert.assertEquals("0003", CompanyContext.get().getCompanyId());
            return Arrays.asList(menu(10, 0, "系统管理"), menu(11, 10, "登录用户管理"));
        });

        successResult result = controller.Login("hbadmin", "123", CAPTCHA_ID, CAPTCHA_CODE, null);

        Assert.assertTrue(result.getMessage(), result.getSuccess());
        LoginUserInfo data = (LoginUserInfo) result.getData();
        Assert.assertEquals(Arrays.asList("系统管理", "登录用户管理"), data.getRolemenu());
        Assert.assertEquals(1, data.getMenuTree().size());
        // 登录成功应清除失败计数
        org.mockito.Mockito.verify(redis).del(org.mockito.ArgumentMatchers.contains("loginfail"));
    }

    @Test
    public void Login_shouldDeduplicateSystemAccountRowsForSameCompany() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0001", "达川公司")));
        when(userMapper.getByAcountAndCompanyID("cfy", "0001", "")).thenReturn(loginUser("0001"));
        stubPermissionMenus();

        successResult result = controller.Login("cfy", "123", CAPTCHA_ID, CAPTCHA_CODE, null);

        Assert.assertTrue(result.getMessage(), result.getSuccess());
        LoginUserInfo data = (LoginUserInfo) result.getData();
        Assert.assertEquals("0001", data.getCompanyId());
    }

    @Test
    public void Login_shouldRequireCompanySelectionWhenAccountMapsToMultipleCompanies() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0002", "成都公司")));
        when(userMapper.getByAcountAndCompanyID("cfy", "0001", "")).thenReturn(loginUser("0001"));
        stubPermissionMenus();

        successResult result = controller.Login("cfy", "123", CAPTCHA_ID, CAPTCHA_CODE, null);

        // SaaS P2：密码在两个企业均匹配时返回 needSelectCompany + 企业列表
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Assert.assertNotNull(data);
        Assert.assertEquals(Boolean.TRUE, data.get("needSelectCompany"));
        Assert.assertEquals(2, ((java.util.List) data.get("companies")).size());
    }

    @Test
    public void Login_shouldLoginTargetCompany_whenCompanyIdProvided() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0002", "成都公司")));
        when(userMapper.getByAcountAndCompanyID("cfy", "0002", "")).thenReturn(loginUser("0002"));
        stubPermissionMenus();

        successResult result = controller.Login("cfy", "123", CAPTCHA_ID, CAPTCHA_CODE, "0002");

        Assert.assertTrue(result.getMessage(), result.getSuccess());
        LoginUserInfo data = (LoginUserInfo) result.getData();
        Assert.assertEquals("0002", data.getCompanyId());
    }

    @Test
    public void Login_shouldIncludeCompanyNameInIssuedLoginInfo() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0002", "成都公司")));
        when(userMapper.getByAcountAndCompanyID("cfy", "0002", "")).thenReturn(loginUser("0002"));
        stubPermissionMenus();

        successResult result = controller.Login("cfy", "123", CAPTCHA_ID, CAPTCHA_CODE, null);

        Assert.assertTrue(result.getMessage(), result.getSuccess());
        Assert.assertEquals("成都公司", ((LoginUserInfo) result.getData()).getCompanyName());
    }

    @Test
    public void Login_shouldRejectWrongCaptcha() {
        when(redis.exists(anyString())).thenReturn(false);
        when(redis.get("hr:captcha:" + CAPTCHA_ID)).thenReturn((Object) "XXXX");

        successResult result = controller.Login("hbadmin", "123", CAPTCHA_ID, CAPTCHA_CODE, null);

        Assert.assertFalse(result.getSuccess());
        Assert.assertEquals("验证码不正确", result.getMessage());
    }

    @Test
    public void Login_shouldCountFail_whenPasswordWrongOnMultiCompanyAccount() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0002", "成都公司")));
        when(userMapper.getByAcountAndCompanyID(org.mockito.ArgumentMatchers.eq("cfy"), anyString(), org.mockito.ArgumentMatchers.eq("")))
                .thenReturn(loginUser("0001"));

        successResult result = controller.Login("cfy", "wrongpass", CAPTCHA_ID, CAPTCHA_CODE, null);

        Assert.assertFalse(result.getSuccess());
        Assert.assertEquals("登录密码不正确!", result.getMessage());
        org.mockito.Mockito.verify(redis).incr(org.mockito.ArgumentMatchers.contains("loginfail"));
    }

    @Test
    public void Login_shouldRejectCompanyListLeak_whenPasswordWrong() {
        stubValidCaptcha();
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0002", "成都公司")));
        when(userMapper.getByAcountAndCompanyID(org.mockito.ArgumentMatchers.eq("cfy"), anyString(), org.mockito.ArgumentMatchers.eq("")))
                .thenReturn(loginUser("0001"));

        successResult result = controller.Login("cfy", "wrongpass", CAPTCHA_ID, CAPTCHA_CODE, null);

        // 安全修正：未验密不得返回企业列表
        Assert.assertFalse(result.getSuccess());
        Assert.assertNull(result.getData());
    }

    // ============ 切换企业（switchCompany / candidates）============

    /** 模拟当前已登录操作员：其账号 cfy 可访问 0001/0002，当前在 0001 */
    private LoginUserInfo currentOperatorIn0001() {
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0002", "成都公司")));
        when(userMapper.getByAcountAndCompanyID("cfy", "0002", "")).thenReturn(loginUser("0002"));
        when(tokenRevocation.getSessionSeed("cfy")).thenReturn(99L);
        when(userMapper.getPwdChangeRequired("cfy", "0002", "")).thenReturn(null);
        stubPermissionMenus();
        LoginUserInfo current = loginUser("0001");
        current.setAccount("cfy");
        CompanyContext.set(current);
        return current;
    }

    @Test
    public void switchCompany_shouldIssueTokenForTargetInCandidates() {
        currentOperatorIn0001();
        successResult result = controller.switchCompany("0002");
        Assert.assertTrue(result.getMessage(), result.getSuccess());
        LoginUserInfo data = (LoginUserInfo) result.getData();
        Assert.assertEquals("0002", data.getCompanyId());
        Assert.assertEquals("cfy", data.getAccount());
        Assert.assertNotNull(data.getToken());
        // 沿用当前会话种子（不 bump），原企业 token 不被作废
        Assert.assertEquals(Long.valueOf(99L), data.getSessionSeed());
        Assert.assertEquals("成都公司", data.getCompanyName());
        // 目标企业菜单已装载
        Assert.assertEquals(1, data.getMenuTree().size());
    }

    @Test
    public void switchCompany_shouldReject_whenTargetNotInCandidates() {
        LoginUserInfo current = loginUser("0001");
        current.setAccount("cfy");
        CompanyContext.set(current);
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司")));
        successResult result = controller.switchCompany("0009");
        Assert.assertFalse(result.getSuccess());
        Assert.assertTrue(result.getMessage().contains("不可访问"));
    }

    @Test
    public void switchCompany_shouldReject_whenSwitchingToCurrentCompany() {
        currentOperatorIn0001();
        successResult result = controller.switchCompany("0001");
        Assert.assertFalse(result.getSuccess());
        Assert.assertTrue(result.getMessage().contains("无需切换"));
    }

    @Test
    public void switchCompanyCandidates_shouldReturnCompaniesAndCurrentCompanyId() {
        LoginUserInfo current = loginUser("0001");
        current.setAccount("cfy");
        CompanyContext.set(current);
        when(userMapper.getCompaniesByUserName("cfy", "hrsystem"))
                .thenReturn(Arrays.asList(company("0001", "达川公司"), company("0002", "成都公司")));
        successResult result = controller.switchCompanyCandidates();
        Assert.assertTrue(result.getMessage(), result.getSuccess());
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Assert.assertEquals("0001", data.get("currentCompanyId"));
        Assert.assertEquals(2, ((java.util.List) data.get("companies")).size());
    }

    private void stubPermissionMenus() {
        when(rolemenuRepository.getAllByRoleId(2)).thenReturn(Arrays.asList(roleMenu(2, 10), roleMenu(2, 11)));
        when(tbMenuService.queryAllMenus()).thenReturn(Arrays.asList(menu(10, 0, "系统管理"), menu(11, 10, "登录用户管理")));
    }

    private static LoginUserInfo loginUser() {
        return loginUser("0003");
    }

    private static LoginUserInfo loginUser(String companyId) {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setUserId("1");
        info.setUserName("管理员");
        info.setDepId("1");
        info.setDepName("总部");
        info.setRoleId("2");
        info.setRoleName("系统管理员");
        info.setCanLogin(true);
        // 使用旧版双重 MD5 存储，顺带覆盖“登录后透明升级”路径（mapper 为 mock，升级写库仅记日志）
        info.setPassword(MD5Utils.enCode("123"));
        return info;
    }

    private static tbrolemenu roleMenu(Integer roleId, Integer menuId) {
        tbrolemenu roleMenu = new tbrolemenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private static tbmenu menu(Integer id, Integer pid, String name) {
        tbmenu menu = new tbmenu();
        menu.setId(id);
        menu.setPid(pid);
        menu.setName(name);
        menu.setCanUse(1);
        return menu;
    }
}
