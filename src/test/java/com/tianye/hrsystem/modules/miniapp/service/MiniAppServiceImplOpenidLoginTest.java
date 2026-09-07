package com.tianye.hrsystem.modules.miniapp.service;

import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.miniapp.mapper.MiniAppSystemMapper;
import com.tianye.hrsystem.modules.miniapp.mapper.MiniAppEmployeeMapper;
import com.tianye.hrsystem.modules.miniapp.service.impl.MiniAppServiceImpl;
import com.tianye.hrsystem.modules.miniapp.support.MiniAppWxClient;
import com.tianye.hrsystem.modules.miniapp.vo.CompanyOptionVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppLoginVO;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MiniAppServiceImplOpenidLoginTest {

    @InjectMocks
    private MiniAppServiceImpl service;

    @Mock
    private MiniAppWxClient wxClient;

    @Mock
    private MiniAppSystemMapper systemMapper;

    @Mock
    private MiniAppEmployeeMapper employeeBindingMapper;

    @Mock
    private hrmEmployeeRepository employeeRepository;

    @Mock
    private Redis redis;

    @After
    public void tearDown() {
        CompanyContext.clear();
    }

    @Test
    public void login_shouldLoadEmployeeByOpenidWithoutPhoneCode() throws Exception {
        when(wxClient.code2Session("wx-code")).thenReturn(session("openid-1"));
        when(systemMapper.listAllCompanies()).thenReturn(Arrays.asList(
                company("0001", "公司一"), company("0002", "公司二")));
        HrmEmployee employee = employee(22L, "测试员工", "openid-1");
        when(employeeRepository.findFirstByOpenid("openid-1")).thenAnswer(invocation ->
                "0002".equals(CompanyContext.get().getCompanyId())
                        ? Optional.of(employee) : Optional.empty());

        MiniAppLoginVO result = service.login("wx-code");

        Assert.assertEquals(Long.valueOf(22L), result.getEmployeeId());
        Assert.assertEquals("0002", result.getCompanyId());
        Assert.assertEquals("公司二", result.getCompanyName());
        Assert.assertNotNull(result.getToken());
        Assert.assertNull("跨租户查询后必须恢复上下文", CompanyContext.get());
    }

    @Test
    public void login_shouldReturnSelfBindingTicketWhenOpenidIsUnknown() throws Exception {
        when(wxClient.code2Session("wx-code")).thenReturn(session("openid-new"));
        when(systemMapper.listAllCompanies()).thenReturn(Arrays.asList(
                company("0001", "公司一"), company("0002", "公司二")));
        when(employeeRepository.findFirstByOpenid(eq("openid-new"))).thenReturn(Optional.empty());

        MiniAppLoginVO result = service.login("wx-code");

        Assert.assertNotNull("首次绑定必须返回服务端ticket", result.getToken());
        Assert.assertEquals(2, result.getCandidates().size());
        Assert.assertNull(result.getEmployeeId());
        verify(redis).setex(
                eq("mp:login:ticket:" + result.getToken()),
                eq(600),
                eq("bind:openid-new"));
    }

    @Test
    public void login_shouldPreserveBindingFailuresWhenIssuingNewTicket() throws Exception {
        when(wxClient.code2Session("wx-code")).thenReturn(session("openid-new"));
        when(systemMapper.listAllCompanies()).thenReturn(
                Collections.singletonList(company("0001", "公司一")));
        when(employeeRepository.findFirstByOpenid("openid-new")).thenReturn(Optional.empty());

        MiniAppLoginVO result = service.login("wx-code");

        Assert.assertEquals(Boolean.TRUE, result.getBindRequired());
        verify(redis, never()).del(anyString());
    }

    @Test
    public void bindEmployee_shouldBindVerifiedEmployeeAndReturnLogin() throws Exception {
        String ticket = createPendingTicket("bind-code", "openid-bind");
        HrmEmployee employee = employee(33L, "张三", null);
        employee.setIdNumber("460100199001011234");
        when(employeeRepository.findAllByEmployeeName("张三"))
                .thenReturn(Collections.singletonList(employee));
        when(employeeBindingMapper.bindOpenidIfEmpty(33L, "openid-bind")).thenAnswer(invocation -> {
            Assert.assertNotNull("写入OpenID时必须设置租户上下文", CompanyContext.get());
            Assert.assertEquals("0001", CompanyContext.get().getCompanyId());
            return 1;
        });

        MiniAppLoginVO result = service.bindEmployee(
                ticket, "0001", " 张三 ", "460100199001011234");

        Assert.assertEquals(Long.valueOf(33L), result.getEmployeeId());
        Assert.assertEquals("0001", result.getCompanyId());
        Assert.assertNotNull(result.getToken());
        verify(employeeBindingMapper).bindOpenidIfEmpty(33L, "openid-bind");
    }

    @Test
    public void bindEmployee_shouldRejectInvalidIdentityWithoutWritingOpenid() throws Exception {
        String ticket = createPendingTicket("invalid-code", "openid-invalid");
        when(employeeRepository.findAllByEmployeeName("不存在"))
                .thenReturn(Collections.emptyList());

        try {
            service.bindEmployee(ticket, "0001", "不存在", "000000000000000000");
            Assert.fail("错误员工信息不应绑定成功");
        } catch (Exception ex) {
            Assert.assertEquals("员工信息核验失败，请检查后重试", ex.getMessage());
        }
        verify(employeeBindingMapper, never()).bindOpenidIfEmpty(any(), any());
    }

    @Test
    public void bindEmployee_shouldRejectAmbiguousEmployeeMatches() throws Exception {
        String ticket = createPendingTicket("duplicate-code", "openid-duplicate");
        HrmEmployee first = employee(33L, "张三", null);
        first.setIdNumber("P1234567");
        HrmEmployee second = employee(44L, "张三", null);
        second.setIdNumber("P1234567");
        when(employeeRepository.findAllByEmployeeName("张三"))
                .thenReturn(Arrays.asList(first, second));

        try {
            service.bindEmployee(ticket, "0001", "张三", "P1234567");
            Assert.fail("重复员工记录不应默认绑定第一条");
        } catch (Exception ex) {
            Assert.assertEquals("员工信息核验失败，请检查后重试", ex.getMessage());
        }
        verify(employeeBindingMapper, never()).bindOpenidIfEmpty(any(), any());
    }

    @Test
    public void bindEmployee_shouldHideDatabaseConstraintDetails() throws Exception {
        String ticket = createPendingTicket("conflict-code", "openid-conflict");
        HrmEmployee employee = employee(55L, "李四", null);
        employee.setIdNumber("P7654321");
        when(employeeRepository.findAllByEmployeeName("李四"))
                .thenReturn(Collections.singletonList(employee));
        when(employeeBindingMapper.bindOpenidIfEmpty(55L, "openid-conflict"))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry 'openid-conflict' for key 'uk_hrm_employee_openid'"));
        when(employeeRepository.findById(55L)).thenReturn(Optional.of(employee));

        try {
            service.bindEmployee(ticket, "0001", "李四", "P7654321");
            Assert.fail("数据库约束冲突不应绑定成功");
        } catch (Exception ex) {
            Assert.assertEquals("员工信息核验失败，请检查后重试", ex.getMessage());
            Assert.assertFalse(ex.getMessage().contains("Duplicate"));
        }
    }

    @Test
    public void switchCompany_shouldFindCompaniesByCurrentEmployeeOpenid() throws Exception {
        LoginUserInfo current = new LoginUserInfo();
        current.setCompanyId("0001");
        CompanyContext.set(current);
        HrmEmployee companyOneEmployee = employee(33L, "张三", "openid-switch");
        HrmEmployee companyTwoEmployee = employee(44L, "张三", "openid-switch");
        when(systemMapper.listAllCompanies()).thenReturn(Arrays.asList(
                company("0001", "公司一"), company("0002", "公司二")));
        when(employeeRepository.findById(33L)).thenReturn(Optional.of(companyOneEmployee));
        when(employeeRepository.findFirstByOpenid("openid-switch")).thenAnswer(invocation ->
                "0002".equals(CompanyContext.get().getCompanyId())
                        ? Optional.of(companyTwoEmployee) : Optional.of(companyOneEmployee));

        List<CompanyOptionVO> result = service.switchCompany(33L);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("0001", CompanyContext.get().getCompanyId());
    }

    @Test
    public void confirmSwitch_shouldIssueTokenForEmployeeMatchedByOpenid() throws Exception {
        LoginUserInfo current = new LoginUserInfo();
        current.setCompanyId("0001");
        CompanyContext.set(current);
        HrmEmployee companyOneEmployee = employee(33L, "张三", "openid-switch");
        HrmEmployee companyTwoEmployee = employee(44L, "张三", "openid-switch");
        when(systemMapper.listAllCompanies()).thenReturn(Arrays.asList(
                company("0001", "公司一"), company("0002", "公司二")));
        when(employeeRepository.findById(33L)).thenReturn(Optional.of(companyOneEmployee));
        when(employeeRepository.findFirstByOpenid("openid-switch")).thenAnswer(invocation ->
                "0002".equals(CompanyContext.get().getCompanyId())
                        ? Optional.of(companyTwoEmployee) : Optional.of(companyOneEmployee));

        MiniAppLoginVO result = service.confirmSwitch(33L, "0002");

        Assert.assertEquals(Long.valueOf(44L), result.getEmployeeId());
        Assert.assertEquals("0002", result.getCompanyId());
        Assert.assertNotNull(result.getToken());
        Assert.assertEquals("0001", CompanyContext.get().getCompanyId());
    }

    private String createPendingTicket(String code, String openid) throws Exception {
        when(wxClient.code2Session(code)).thenReturn(session(openid));
        when(systemMapper.listAllCompanies()).thenReturn(Arrays.asList(
                company("0001", "公司一"), company("0002", "公司二")));
        when(employeeRepository.findFirstByOpenid(openid)).thenReturn(Optional.empty());
        String ticket = service.login(code).getToken();
        when(redis.get("mp:login:ticket:" + ticket)).thenReturn("bind:" + openid);
        return ticket;
    }

    private JSONObject session(String openid) {
        JSONObject session = new JSONObject();
        session.put("openid", openid);
        return session;
    }

    private CompanyOptionVO company(String id, String name) {
        CompanyOptionVO company = new CompanyOptionVO();
        company.setCompanyId(id);
        company.setCompanyName(name);
        return company;
    }

    private HrmEmployee employee(Long id, String name, String openid) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(id);
        employee.setEmployeeName(name);
        employee.setOpenid(openid);
        employee.setDeptId(1L);
        employee.setIsDel(0);
        return employee;
    }
}
