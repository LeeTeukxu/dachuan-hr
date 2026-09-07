package com.tianye.hrsystem.modules.miniapp.service.impl;

import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmWorkplanApplication;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.miniapp.mapper.MiniAppEmployeeMapper;
import com.tianye.hrsystem.modules.miniapp.mapper.MiniAppSystemMapper;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppService;
import com.tianye.hrsystem.modules.miniapp.support.MiniAppWxClient;
import com.tianye.hrsystem.modules.miniapp.vo.CompanyOptionVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppDayShiftVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppLoginVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppMonthScheduleVO;
import com.tianye.hrsystem.modules.workplanapplication.service.IWorkPlanApplicationService;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmWorkplanApplicationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 小程序登录绑定与排班查询。
 *
 * <p>登录使用微信 code2Session 返回的 openid 遍历租户员工表。首次未绑定时返回短期 ticket，
 * 员工完成公司、姓名和证件号码核验后将 openid 写回 hrm_employee。</p>
 */
@Service
public class MiniAppServiceImpl implements IMiniAppService {

    private static final Logger logger = LoggerFactory.getLogger(MiniAppServiceImpl.class);

    private static final String STATUS_PENDING = "pending";
    private static final String BIND_VERIFY_ERROR = "员工信息核验失败，请检查后重试";
    private static final String PENDING_TICKET_KEY_PREFIX = "mp:login:ticket:";
    private static final String PENDING_BIND_PREFIX = "bind:";
    private static final String PENDING_COMPANY_PREFIX = "company:";
    private static final int PENDING_TICKET_TTL_SECONDS = 10 * 60;

    /** 登录限流：同一身份连续失败 5 次锁定 15 分钟（与 PC 端登录锁定策略一致） */
    private static final String LOGIN_FAIL_KEY_PREFIX = "mp:login:fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "mp:login:lock:";
    private static final int LOGIN_MAX_FAIL_COUNT = 5;
    private static final int LOGIN_LOCK_SECONDS = 900;

    @Autowired
    private MiniAppWxClient wxClient;

    @Autowired
    private MiniAppSystemMapper systemMapper;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private MiniAppEmployeeMapper employeeBindingMapper;

    @Autowired
    private hrmWorkplanApplicationRepository applicationRepository;

    @Autowired
    private IWorkPlanApplicationService applicationService;

    @Autowired
    private Redis redis;

    @Autowired
    private com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService miniAppPermissionService;

    @Value("${hrm.system.databasesuffix:}")
    private String databaseSuffix;

    @Override
    public MiniAppLoginVO login(String code) throws Exception {
        if (StringUtils.isBlank(code)) {
            throw new Exception("微信登录code为空");
        }
        // code2session 失败时拿不到 openid，退化为按 code 指纹限流，避免免鉴权接口被刷
        String identityKey;
        String identityLabel;
        com.alibaba.fastjson.JSONObject session = null;
        try {
            session = wxClient.code2Session(code);
            String openid = session.getString("openid");
            identityKey = "openid:" + fingerprint(openid);
            identityLabel = "openid:" + maskText(openid, 4);
        } catch (Exception e) {
            identityKey = "code:" + fingerprint(code);
            identityLabel = "code:" + fingerprint(code).substring(0, 8);
        }
        checkLoginNotLocked(identityKey);
        try {
            MiniAppLoginVO vo = doLogin(session);
            if (!Boolean.TRUE.equals(vo.getBindRequired())) {
                redis.del(LOGIN_FAIL_KEY_PREFIX + identityKey);
            }
            return vo;
        } catch (Exception e) {
            recordLoginFail(identityKey);
            logger.warn("小程序登录失败({}): {}", identityLabel, e.getMessage());
            throw e;
        }
    }

    @Override
    public MiniAppLoginVO bindEmployee(String ticket, String companyId,
                                       String employeeName, String idNumber) throws Exception {
        PendingLogin pending = loadPendingLogin(ticket);
        if (pending == null || !pending.employeeBindingAllowed) {
            throw new Exception("绑定凭证已过期，请重新登录");
        }

        String identityKey = "openid:" + fingerprint(pending.openid);
        String identityLabel = "openid:" + maskText(pending.openid, 4);
        checkLoginNotLocked(identityKey);
        try {
            CompanyOptionVO company = findCompany(companyId);
            if (company == null || StringUtils.isBlank(employeeName) || StringUtils.isBlank(idNumber)) {
                throw bindingVerificationException();
            }
            HrmEmployee employee = findEmployeeForBinding(
                    companyId, employeeName.trim(), idNumber.trim());
            if (employee == null) {
                throw bindingVerificationException();
            }
            if (StringUtils.isBlank(employee.getOpenid())) {
                int updated;
                try {
                    updated = bindOpenidInCompany(
                            companyId, employee.getEmployeeId(), pending.openid);
                } catch (Exception updateException) {
                    updated = 0;
                }
                if (updated != 1) {
                    HrmEmployee current = resolveEmployee(companyId, employee.getEmployeeId());
                    if (current == null || !pending.openid.equals(current.getOpenid())) {
                        throw bindingVerificationException();
                    }
                    employee = current;
                } else {
                    employee.setOpenid(pending.openid);
                }
            } else if (!pending.openid.equals(employee.getOpenid())) {
                throw bindingVerificationException();
            }

            deletePendingLogin(ticket);
            redis.del(LOGIN_FAIL_KEY_PREFIX + identityKey);
            return buildLoginVO(new EmployeeMatch(companyId, company.getCompanyName(), employee));
        } catch (Exception ex) {
            recordLoginFail(identityKey);
            logger.warn("小程序首次绑定失败({}): {}", identityLabel,
                    ex.getClass().getSimpleName());
            if (BIND_VERIFY_ERROR.equals(ex.getMessage())) {
                throw ex;
            }
            throw bindingVerificationException();
        }
    }

    private MiniAppLoginVO doLogin(com.alibaba.fastjson.JSONObject session) throws Exception {
        if (session == null || StringUtils.isBlank(session.getString("openid"))) {
            throw new Exception("微信登录返回openid为空");
        }
        String openid = session.getString("openid");

        List<EmployeeMatch> matches = findEmployeesByOpenid(openid);

        if (matches.size() == 1) {
            return buildLoginVO(matches.get(0));
        }

        String ticket = UUID.randomUUID().toString().replace("-", "");
        if (matches.isEmpty()) {
            storePendingLogin(ticket, openid, true);
            return buildBindingRequired(ticket);
        }
        storePendingLogin(ticket, openid, false);
        return buildCandidates(ticket, matches);
    }

    /** 登录失败锁定检查（Redis 异常不阻断登录主流程） */
    private void checkLoginNotLocked(String identity) throws Exception {
        boolean locked = false;
        long minutes = LOGIN_LOCK_SECONDS / 60;
        try {
            if (redis.exists(LOGIN_LOCK_KEY_PREFIX + identity)) {
                locked = true;
                Long ttl = redis.ttl(LOGIN_LOCK_KEY_PREFIX + identity);
                minutes = Math.max(1, ttl == null ? LOGIN_LOCK_SECONDS : ttl) / 60;
            }
        } catch (Exception ex) {
            logger.warn("查询小程序登录限流状态异常(不影响登录主流程): {}", ex.getMessage());
        }
        if (locked) {
            throw new Exception("登录失败次数过多，请约 " + minutes + " 分钟后再试");
        }
    }

    private void recordLoginFail(String identity) {
        try {
            String failKey = LOGIN_FAIL_KEY_PREFIX + identity;
            Long count = redis.incr(failKey);
            if (count != null && count == 1) {
                redis.expire(failKey, LOGIN_LOCK_SECONDS);
            }
            if (count != null && count >= LOGIN_MAX_FAIL_COUNT) {
                redis.setex(LOGIN_LOCK_KEY_PREFIX + identity, LOGIN_LOCK_SECONDS, "1");
                redis.del(failKey);
                logger.warn("小程序登录连续失败{}次，已临时锁定: {}", count, identity);
            }
        } catch (Exception ex) {
            logger.warn("记录小程序登录失败次数异常(不影响登录主流程): {}", ex.getMessage());
        }
    }

    private String maskText(String value, int keep) {
        if (StringUtils.isBlank(value)) {
            return "empty";
        }
        String v = value.trim();
        if (v.length() <= keep * 2) {
            return v.substring(0, 1) + "****";
        }
        return v.substring(0, keep) + "****" + v.substring(v.length() - keep);
    }

    private String fingerprint(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(StringUtils.trimToEmpty(value)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256不可用", ex);
        }
    }

    @Override
    public void checkApprovalPermission(Long approverEmployeeId, Long applicationId) throws Exception {
        if (approverEmployeeId == null) {
            throw new Exception("当前登录身份缺少员工信息");
        }
        HrmWorkplanApplication application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            throw new Exception("申请单不存在");
        }
        Long applicantId = application.getEmployeeId();
        if (applicantId == null || approverEmployeeId.equals(applicantId)) {
            throw new Exception("仅申请人的直属上级可审批");
        }
        HrmEmployee applicant = employeeRepository.findById(applicantId).orElse(null);
        if (applicant == null) {
            throw new Exception("仅申请人的直属上级可审批");
        }
        boolean directSupervisor = applicant.getParentId() != null && approverEmployeeId.equals(applicant.getParentId());
        if (directSupervisor) {
            return;
        }
        // 直属下属档追加指定员工/自定义档：在可见员工名单内也可审批（跨部门）
        List<Long> visibleIds = miniAppPermissionService.resolveVisibleEmployeeIds(approverEmployeeId);
        if (visibleIds != null && visibleIds.contains(applicantId)) {
            return;
        }
        throw new Exception("仅申请人的直属上级或已授权可见的员工可审批");
    }

    @Override
    public MiniAppLoginVO bindCompany(String ticket, String companyId) throws Exception {
        PendingLogin pending = loadPendingLogin(ticket);
        if (pending == null) {
            throw new Exception("绑定凭证已过期，请重新登录");
        }
        List<EmployeeMatch> matches = findEmployeesByOpenid(pending.openid);
        EmployeeMatch target = null;
        for (EmployeeMatch match : matches) {
            if (companyId != null && companyId.equals(match.companyId)) {
                target = match;
                break;
            }
        }
        if (target == null) {
            throw new Exception("所选公司下未找到已绑定的员工，请选择其他公司");
        }
        deletePendingLogin(ticket);
        return buildLoginVO(target);
    }

    @Override
    public List<CompanyOptionVO> switchCompany(Long employeeId) throws Exception {
        if (employeeId == null) {
            throw new Exception("员工ID为空");
        }
        String openid = currentEmployeeOpenid(employeeId);
        List<EmployeeMatch> matches = findEmployeesByOpenid(openid);
        if (matches.isEmpty()) {
            throw new Exception("未找到可切换的公司");
        }
        List<CompanyOptionVO> candidates = new ArrayList<>();
        for (EmployeeMatch match : matches) {
            boolean skip = false;
            for (CompanyOptionVO candidate : candidates) {
                if (candidate.getCompanyId().equals(match.companyId)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                CompanyOptionVO option = new CompanyOptionVO();
                option.setCompanyId(match.companyId);
                option.setCompanyName(match.companyName);
                candidates.add(option);
            }
        }
        return candidates;
    }

    @Override
    public MiniAppLoginVO confirmSwitch(Long employeeId, String companyId) throws Exception {
        if (employeeId == null || StringUtils.isBlank(companyId)) {
            throw new Exception("参数不完整");
        }
        String openid = currentEmployeeOpenid(employeeId);
        List<EmployeeMatch> matches = findEmployeesByOpenid(openid);
        for (EmployeeMatch match : matches) {
            if (companyId.equals(match.companyId)) {
                return buildLoginVO(match);
            }
        }
        throw new Exception("您不在该公司名下，无法切换");
    }

    @Override
    public MiniAppMonthScheduleVO queryMonthSchedule(Long employeeId, String month) throws Exception {
        if (employeeId == null) {
            throw new Exception("employeeId不能为空");
        }
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        Date monthDate = monthFmt.parse(StringUtils.isBlank(month) ? monthFmt.format(new Date()) : month);

        Calendar cal = Calendar.getInstance();
        cal.setTime(monthDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date begin = cal.getTime();
        cal.add(Calendar.MONTH, 1);
        Date end = cal.getTime();

        List<HrmWorkplanApplication> pendings = new ArrayList<>();
        try {
            List<HrmWorkplanApplication> found =
                    applicationRepository.findByEmployeeIdAndStatusAndWorkDateBetween(employeeId, STATUS_PENDING, begin, end);
            if (found != null) {
                pendings.addAll(found);
            }
        } catch (Exception ex) {
            logger.warn("查询待审批申请标记失败: {}", ex.getMessage());
        }

        Map<String, MiniAppDayShiftVO> days = new LinkedHashMap<>();
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(begin);
        while (cursor.getTime().before(end)) {
            Date d = cursor.getTime();
            String key = dateFmt.format(d);
            MiniAppDayShiftVO item = new MiniAppDayShiftVO(key);
            try {
                WorkPlanEmployeeDayShiftVO vo = applicationService.queryEmployeeDayShift(employeeId, d);
                if (vo != null) {
                    item.setShiftType(vo.getCurrentShiftType() == null ? "empty" : vo.getCurrentShiftType());
                    item.setShiftLabel(vo.getCurrentShiftLabel());
                    item.setStart(vo.getCurrentStart());
                    item.setEnd(vo.getCurrentEnd());
                }
            } catch (Exception ex) {
                logger.debug("查询{}排班失败: {}", key, ex.getMessage());
            }
            final String dateKey = key;
            boolean hasPending = pendings.stream().anyMatch(a ->
                    a.getWorkDate() != null && dateFmt.format(a.getWorkDate()).equals(dateKey));
            item.setHasPending(hasPending);
            days.put(key, item);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        MiniAppMonthScheduleVO result = new MiniAppMonthScheduleVO();
        result.setMonth(monthFmt.format(begin));
        result.setDays(days);
        return result;
    }

    @Override
    public MiniAppDayShiftVO queryDayDetail(Long employeeId, Date workDate) throws Exception {
        WorkPlanEmployeeDayShiftVO vo = applicationService.queryEmployeeDayShift(employeeId, workDate);
        MiniAppDayShiftVO item = new MiniAppDayShiftVO(new SimpleDateFormat("yyyy-MM-dd").format(workDate));
        if (vo != null) {
            item.setShiftType(vo.getCurrentShiftType() == null ? "empty" : vo.getCurrentShiftType());
            item.setShiftLabel(vo.getCurrentShiftLabel());
            item.setStart(vo.getCurrentStart());
            item.setEnd(vo.getCurrentEnd());
        }
        return item;
    }

    // ---------------- 私有方法 ----------------

    /** 按 OpenID 遍历各公司库精确匹配未删除员工，结果携带公司ID */
    private List<EmployeeMatch> findEmployeesByOpenid(String openid) {
        List<EmployeeMatch> result = new ArrayList<>();
        if (StringUtils.isBlank(openid)) {
            return result;
        }
        List<CompanyOptionVO> companies;
        try {
            companies = systemMapper.listAllCompanies();
        } catch (Exception ex) {
            logger.warn("枚举公司失败: {}", ex.getMessage());
            return result;
        }
        if (companies == null) {
            return result;
        }
        for (CompanyOptionVO company : companies) {
            if (company == null || StringUtils.isBlank(company.getCompanyId())) {
                continue;
            }
            try {
                HrmEmployee found = findByOpenidInCompany(company.getCompanyId(), openid);
                if (found != null) {
                    result.add(new EmployeeMatch(company.getCompanyId(), company.getCompanyName(), found));
                }
            } catch (Exception ex) {
                logger.warn("公司{}按openid匹配异常: {}", company.getCompanyId(), ex.getMessage());
            }
        }
        return result;
    }

    private HrmEmployee findByOpenidInCompany(String companyId, String openid) {
        LoginUserInfo previous = CompanyContext.get();
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setSuffix(databaseSuffix);
        CompanyContext.set(info);
        try {
            Optional<HrmEmployee> employee = employeeRepository.findFirstByOpenid(openid);
            if (employee.isPresent()) {
                HrmEmployee found = employee.get();
                if (found.getIsDel() == null || found.getIsDel() != 1) {
                    return found;
                }
            }
            return null;
        } finally {
            CompanyContext.set(previous);
        }
    }

    private HrmEmployee findEmployeeForBinding(String companyId, String employeeName, String idNumber) {
        LoginUserInfo previous = CompanyContext.get();
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setSuffix(databaseSuffix);
        CompanyContext.set(info);
        try {
            List<HrmEmployee> employees = employeeRepository.findAllByEmployeeName(employeeName);
            if (employees == null) {
                return null;
            }
            HrmEmployee matched = null;
            for (HrmEmployee employee : employees) {
                if (employee != null
                        && (employee.getIsDel() == null || employee.getIsDel() != 1)
                        && StringUtils.isNotBlank(employee.getIdNumber())
                        && idNumber.equalsIgnoreCase(employee.getIdNumber().trim())) {
                    if (matched != null) {
                        return null;
                    }
                    matched = employee;
                }
            }
            return matched;
        } finally {
            CompanyContext.set(previous);
        }
    }

    private int bindOpenidInCompany(String companyId, Long employeeId, String openid) {
        LoginUserInfo previous = CompanyContext.get();
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setSuffix(databaseSuffix);
        CompanyContext.set(info);
        try {
            return employeeBindingMapper.bindOpenidIfEmpty(employeeId, openid);
        } finally {
            CompanyContext.set(previous);
        }
    }

    private CompanyOptionVO findCompany(String companyId) {
        if (StringUtils.isBlank(companyId)) {
            return null;
        }
        List<CompanyOptionVO> companies = systemMapper.listAllCompanies();
        if (companies != null) {
            for (CompanyOptionVO company : companies) {
                if (company != null && companyId.equals(company.getCompanyId())) {
                    return company;
                }
            }
        }
        return null;
    }

    private HrmEmployee resolveEmployee(String companyId, Long employeeId) {
        LoginUserInfo previous = CompanyContext.get();
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setSuffix(databaseSuffix);
        CompanyContext.set(info);
        try {
            Optional<HrmEmployee> byId = employeeRepository.findById(employeeId);
            return byId.orElse(null);
        } finally {
            CompanyContext.set(previous);
        }
    }

    private String currentEmployeeOpenid(Long employeeId) throws Exception {
        LoginUserInfo current = CompanyContext.get();
        if (current == null || StringUtils.isBlank(current.getCompanyId())) {
            throw new Exception("当前登录身份缺少公司信息");
        }
        HrmEmployee employee = resolveEmployee(current.getCompanyId(), employeeId);
        if (employee == null || StringUtils.isBlank(employee.getOpenid())) {
            throw new Exception("当前员工未绑定微信，请重新登录");
        }
        return employee.getOpenid();
    }

    private MiniAppLoginVO buildLoginVO(EmployeeMatch match) {
        HrmEmployee emp = match.emp;
        MiniAppLoginVO vo = new MiniAppLoginVO();
        vo.setEmployeeId(emp.getEmployeeId());
        vo.setEmployeeName(emp.getEmployeeName());
        vo.setCompanyId(match.companyId);
        vo.setCompanyName(match.companyName);
        vo.setDepId(emp.getDeptId());

        LoginUserInfo info = new LoginUserInfo();
        info.setUserId(String.valueOf(emp.getEmployeeId()));
        info.setEmployeeId(emp.getEmployeeId());
        info.setCompanyId(match.companyId);
        info.setUserName(emp.getEmployeeName());
        info.setSuffix(databaseSuffix);
        info.setCanLogin(true);
        if (emp.getDeptId() != null) {
            info.setDepId(String.valueOf(emp.getDeptId()));
        }
        vo.setToken(JWTTokenUtils.getToken(info));
        return vo;
    }

    private MiniAppLoginVO buildCandidates(String ticket, List<EmployeeMatch> matches) {
        MiniAppLoginVO vo = new MiniAppLoginVO();
        List<CompanyOptionVO> candidates = new ArrayList<>();
        for (EmployeeMatch match : matches) {
            boolean skip = false;
            for (CompanyOptionVO c : candidates) {
                if (c.getCompanyId().equals(match.companyId)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                CompanyOptionVO o = new CompanyOptionVO();
                o.setCompanyId(match.companyId);
                o.setCompanyName(match.companyName);
                candidates.add(o);
            }
        }
        vo.setCandidates(candidates);
        vo.setToken(ticket);
        return vo;
    }

    private MiniAppLoginVO buildBindingRequired(String ticket) {
        MiniAppLoginVO vo = new MiniAppLoginVO();
        List<CompanyOptionVO> companies = systemMapper.listAllCompanies();
        vo.setBindRequired(true);
        vo.setCandidates(companies == null ? new ArrayList<>() : companies);
        vo.setToken(ticket);
        return vo;
    }

    private void storePendingLogin(String ticket, String openid, boolean employeeBindingAllowed) {
        String prefix = employeeBindingAllowed ? PENDING_BIND_PREFIX : PENDING_COMPANY_PREFIX;
        redis.setex(PENDING_TICKET_KEY_PREFIX + ticket, PENDING_TICKET_TTL_SECONDS, prefix + openid);
    }

    private PendingLogin loadPendingLogin(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        String value = redis.get(PENDING_TICKET_KEY_PREFIX + ticket);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (value.startsWith(PENDING_BIND_PREFIX)) {
            return new PendingLogin(value.substring(PENDING_BIND_PREFIX.length()), true);
        }
        if (value.startsWith(PENDING_COMPANY_PREFIX)) {
            return new PendingLogin(value.substring(PENDING_COMPANY_PREFIX.length()), false);
        }
        return null;
    }

    private void deletePendingLogin(String ticket) {
        if (StringUtils.isNotBlank(ticket)) {
            redis.del(PENDING_TICKET_KEY_PREFIX + ticket);
        }
    }

    private Exception bindingVerificationException() {
        return new Exception(BIND_VERIFY_ERROR);
    }

    /** 员工匹配结果：公司ID + 公司名称 + 员工 */
    private static class EmployeeMatch {
        final String companyId;
        final String companyName;
        final HrmEmployee emp;

        EmployeeMatch(String companyId, String companyName, HrmEmployee emp) {
            this.companyId = companyId;
            this.companyName = companyName;
            this.emp = emp;
        }
    }

    /** 待绑定暂存信息 */
    private static class PendingLogin {
        final String openid;
        final boolean employeeBindingAllowed;

        PendingLogin(String openid, boolean employeeBindingAllowed) {
            this.openid = openid;
            this.employeeBindingAllowed = employeeBindingAllowed;
        }
    }

    @Override
    public boolean isSupervisor(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        return employeeRepository.countByParentIdAndIsDel(employeeId, 0) > 0;
    }
}
