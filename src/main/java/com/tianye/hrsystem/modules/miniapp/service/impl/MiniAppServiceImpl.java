package com.tianye.hrsystem.modules.miniapp.service.impl;

import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmWorkplanApplication;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.miniapp.entity.MiniAppUserBinding;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小程序登录绑定与排班查询。
 *
 * <p>重名处理：openid 首次登录按手机号遍历各公司库精确匹配（matching by mobile，从不按姓名），
 * 手机号匹配结果与公司ID一起记录；openid 与员工唯一绑定后，后续一律按绑定表定位员工，与姓名无关。</p>
 *
 * <p>微信手机号 code（phoneCode）为一次性凭证：跨公司多条匹配时，将 openid+手机号暂存于服务端
 * 短时 ticket，用户选择公司后携带 ticket 完成绑定，避免重复消耗 phoneCode。</p>
 */
@Service
public class MiniAppServiceImpl implements IMiniAppService {

    private static final Logger logger = LoggerFactory.getLogger(MiniAppServiceImpl.class);

    private static final String STATUS_PENDING = "pending";
    private static final long PENDING_TICKET_TTL_MILLIS = 10 * 60 * 1000L;

    /** 登录限流：同一身份连续失败 5 次锁定 15 分钟（与 PC 端登录锁定策略一致） */
    private static final String LOGIN_FAIL_KEY_PREFIX = "mp:login:fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "mp:login:lock:";
    private static final int LOGIN_MAX_FAIL_COUNT = 5;
    private static final int LOGIN_LOCK_SECONDS = 900;

    /** 短时暂存：ticket -> 待绑定信息 */
    private static final Map<String, PendingLogin> PENDING_LOGINS = new ConcurrentHashMap<>();

    /** 清理过期 ticket */
    static {
        Timer timer = new Timer("miniapp-pending-cleaner", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                PENDING_LOGINS.entrySet().removeIf(e ->
                        now - e.getValue().createAt > PENDING_TICKET_TTL_MILLIS);
            }
        }, 60_000L, 60_000L);
    }

    @Autowired
    private MiniAppWxClient wxClient;

    @Autowired
    private MiniAppSystemMapper systemMapper;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private hrmWorkplanApplicationRepository applicationRepository;

    @Autowired
    private IWorkPlanApplicationService applicationService;

    @Autowired
    private Redis redis;

    @Value("${hrm.system.databasesuffix:}")
    private String databaseSuffix;

    @Override
    public MiniAppLoginVO login(String code, String phoneCode) throws Exception {
        if (StringUtils.isBlank(code)) {
            throw new Exception("微信登录code为空");
        }
        // code2session 失败时拿不到 openid，退化为按 code 指纹限流，避免免鉴权接口被刷
        String identity;
        com.alibaba.fastjson.JSONObject session = null;
        try {
            session = wxClient.code2Session(code);
            identity = "openid:" + maskText(session.getString("openid"), 4);
        } catch (Exception e) {
            identity = "code:" + Integer.toHexString(StringUtils.trimToEmpty(code).hashCode());
        }
        checkLoginNotLocked(identity);
        try {
            MiniAppLoginVO vo = doLogin(session, phoneCode);
            redis.del(LOGIN_FAIL_KEY_PREFIX + identity);
            return vo;
        } catch (Exception e) {
            recordLoginFail(identity);
            logger.warn("小程序登录失败({}): {}", identity, e.getMessage());
            throw e;
        }
    }

    private MiniAppLoginVO doLogin(com.alibaba.fastjson.JSONObject session, String phoneCode) throws Exception {
        if (session == null || StringUtils.isBlank(session.getString("openid"))) {
            throw new Exception("微信登录返回openid为空");
        }
        String openid = session.getString("openid");

        MiniAppLoginVO existing = buildLoginByBinding(openid);
        if (existing != null) {
            return existing;
        }

        String phone = wxClient.getPhoneByCode(phoneCode);
        List<EmployeeMatch> matches = findEmployeesByPhone(phone);
        if (matches.isEmpty()) {
            throw new Exception("未找到手机号对应的员工，请联系人事");
        }

        if (matches.size() == 1) {
            EmployeeMatch single = matches.get(0);
            bindAndExempt(openid, session.getString("unionid"), phone, single);
            return buildLoginVO(single);
        }

        String ticket = UUID.randomUUID().toString().replace("-", "");
        PENDING_LOGINS.put(ticket, new PendingLogin(openid, phone));
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
        if (applicant == null || applicant.getParentId() == null
                || !approverEmployeeId.equals(applicant.getParentId())) {
            throw new Exception("仅申请人的直属上级可审批");
        }
    }

    @Override
    public MiniAppLoginVO bindCompany(String ticket, String companyId) throws Exception {
        PendingLogin pending = PENDING_LOGINS.remove(ticket);
        if (pending == null) {
            throw new Exception("绑定凭证已过期，请重新登录");
        }
        List<EmployeeMatch> matches = findEmployeesByPhone(pending.phone);
        EmployeeMatch target = null;
        for (EmployeeMatch match : matches) {
            if (companyId != null && companyId.equals(match.companyId)) {
                target = match;
                break;
            }
        }
        if (target == null) {
            throw new Exception("所选公司下未找到该手机号的员工");
        }
        bindAndExempt(pending.openid, null, pending.phone, target);
        return buildLoginVO(target);
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

    private MiniAppLoginVO buildLoginByBinding(String openid) {
        MiniAppUserBinding binding = systemMapper.findByOpenid(openid);
        if (binding == null) {
            return null;
        }
        HrmEmployee emp = resolveEmployee(binding.getCompanyId(), binding.getEmployeeId());
        if (emp == null) {
            return null;
        }
        EmployeeMatch match = new EmployeeMatch(binding.getCompanyId(), emp);
        return buildLoginVO(match);
    }

    /** 按手机号遍历各公司库精确匹配在岗员工，结果携带公司ID */
    private List<EmployeeMatch> findEmployeesByPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (StringUtils.isBlank(normalized)) {
            return new ArrayList<>();
        }
        List<EmployeeMatch> result = new ArrayList<>();
        List<CompanyOptionVO> companies;
        try {
            companies = systemMapper.listAllCompanies();
        } catch (Exception ex) {
            logger.warn("枚举公司失败: {}", ex.getMessage());
            return result;
        }
        if (companies == null || companies.isEmpty()) {
            return result;
        }
        for (CompanyOptionVO company : companies) {
            if (company == null || StringUtils.isBlank(company.getCompanyId())) {
                continue;
            }
            try {
                HrmEmployee found = findByMobileInCompany(company.getCompanyId(), normalized);
                if (found != null) {
                    result.add(new EmployeeMatch(company.getCompanyId(), found));
                }
            } catch (Exception ex) {
                logger.warn("公司{}按手机号匹配异常: {}", company.getCompanyId(), ex.getMessage());
            }
        }
        return result;
    }

    /** 切换 CompanyContext 到指定公司后，用 JPA 按手机号查员工 */
    private HrmEmployee findByMobileInCompany(String companyId, String phone) {
        LoginUserInfo previous = CompanyContext.get();
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        info.setSuffix(databaseSuffix);
        CompanyContext.set(info);
        try {
            Optional<HrmEmployee> byMobile = employeeRepository.findFirstByMobile(phone);
            if (byMobile.isPresent()) {
                HrmEmployee e = byMobile.get();
                if (e.getIsDel() == null || e.getIsDel() != 1) {
                    return e;
                }
            }
            return null;
        } finally {
            CompanyContext.set(previous);
        }
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

    private void bindAndExempt(String openid, String unionid, String phone, EmployeeMatch match) {
        MiniAppUserBinding binding = new MiniAppUserBinding();
        binding.setOpenid(openid);
        binding.setUnionid(unionid);
        binding.setPhone(phone);
        binding.setEmployeeId(match.emp.getEmployeeId());
        binding.setCompanyId(match.companyId);
        try {
            systemMapper.insertBinding(binding);
        } catch (Exception ex) {
            logger.warn("绑定写入失败(可能已存在): {}", ex.getMessage());
        }
    }

    private MiniAppLoginVO buildLoginVO(EmployeeMatch match) {
        HrmEmployee emp = match.emp;
        MiniAppLoginVO vo = new MiniAppLoginVO();
        vo.setEmployeeId(emp.getEmployeeId());
        vo.setEmployeeName(emp.getEmployeeName());
        vo.setCompanyId(match.companyId);
        vo.setDepId(emp.getDeptId());

        LoginUserInfo info = new LoginUserInfo();
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
                o.setCompanyName(match.companyId);
                candidates.add(o);
            }
        }
        vo.setCandidates(candidates);
        vo.setToken(ticket);
        return vo;
    }

    private String normalizePhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }
        String normalized = phone.trim().replaceAll("[\\s\\-\\(\\)\\+]", "");
        if (normalized.startsWith("86") && normalized.length() == 13) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    /** 员工匹配结果：公司ID + 员工 */
    private static class EmployeeMatch {
        final String companyId;
        final HrmEmployee emp;

        EmployeeMatch(String companyId, HrmEmployee emp) {
            this.companyId = companyId;
            this.emp = emp;
        }
    }

    /** 待绑定暂存信息 */
    private static class PendingLogin {
        final String openid;
        final String phone;
        final long createAt;

        PendingLogin(String openid, String phone) {
            this.openid = openid;
            this.phone = phone;
            this.createAt = System.currentTimeMillis();
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