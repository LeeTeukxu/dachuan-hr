package com.tianye.hrsystem.controller;

import lombok.extern.slf4j.Slf4j;

import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.model.tbrolemenu;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.modules.menu.service.MenuPermissionSupport;
import com.tianye.hrsystem.modules.menu.service.TbMenuService;
import com.tianye.hrsystem.repository.rolemenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @ClassName: LoginController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 14:27
 **/
@RestController
@Slf4j
public class LoginController {
    @Autowired
    LoginUserMapper userMapper;
    @Value("${hrm.system.database}")
    String systemBase;
    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;
    Logger logger= LoggerFactory.getLogger(LoginController.class);

    @Autowired
    rolemenuRepository rolemenuRepository;
    @Autowired
    TbMenuService tbMenuService;
    @Autowired
    MenuPermissionSupport menuPermissionSupport;
    @Autowired
    com.tianye.hrsystem.common.Redis redis;
    @Autowired
    com.tianye.hrsystem.common.PasswordService passwordService;
    @Autowired
    com.tianye.hrsystem.common.TokenRevocationService tokenRevocation;

    private static final String LOGIN_FAIL_KEY_PREFIX = "hr:loginfail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "hr:loginlock:";
    private static final String CONFIRM_TOKEN_KEY_PREFIX = "hr:confirm:";
    private static final int LOGIN_MAX_FAIL_COUNT = 5;
    private static final int LOGIN_LOCK_SECONDS = 900;
    private static final int CONFIRM_TOKEN_TTL_SECONDS = 300;

    @PostMapping("/login")
    public successResult Login(String account, String password, String captchaId, String captchaCode,
                               String companyId) {
        successResult result = new successResult();
        try {
            // SaaS 安全改造 P0-1：验证码校验（一次性）+ 登录失败锁定
            checkAccountNotLocked(account);
            String captchaError = CaptchaController.validate(redis, captchaId, captchaCode);
            if (captchaError != null) {
                throw new Exception(captchaError);
            }

            if (password == null || password.isEmpty()) {
                throw new Exception("密码不能为空!");
            }

            // SaaS 改造 P2（安全修正版）：先验密码再决定是否需要选择企业。
            // 旧实现先返回企业列表，导致①多租户账号绕过失败锁定②未验密即可枚举企业名。
            // SaaS 改造 P3：登录密码与企业选择彻底脱钩——
            // 只验"基准公司"(授权企业第一条)的密码，密码正确即放行，再按授权企业数决定是否弹选择。
            // 不再逐个授权企业验密，避免某企业密码被改/未建号导致的选择消失（bug1/bug2）。
            List<java.util.Map<String, Object>> companies = listCandidateCompanies(account);
            if (companies.isEmpty()) {
                throw new Exception(account + "在系统中不存在!");
            }

            LoginUserInfo matched = null;

            if (companyId != null && !StringUtils.isEmpty(companyId.trim())) {
                // 指定企业：必须在候选列表中
                String target = companyId.trim();
                boolean belongs = companies.stream()
                        .anyMatch(c -> target.equals(String.valueOf(c.get("companyId"))));
                if (!belongs) {
                    throw new Exception("账号 " + account + " 不属于企业 " + target + "，无法登录!");
                }
                LoginUserInfo Info = loadTenantUser(account, target);
                verifyPassword(Info, account, password);
                matched = Info;
                upgradeLegacyHashIfNeeded(Info, account, password);
            } else {
                // 基准公司 = 授权企业第一条（账号所属主公司）
                String baseCid = String.valueOf(companies.get(0).get("companyId"));
                // 若基准公司无账号记录，回退到任一存在账号的授权企业验密
                LoginUserInfo baseInfo = loadTenantUserQuiet(account, baseCid);
                if (baseInfo == null) {
                    for (java.util.Map<String, Object> c : companies) {
                        baseInfo = loadTenantUserQuiet(account, String.valueOf(c.get("companyId")));
                        if (baseInfo != null) break;
                    }
                }
                if (baseInfo == null) {
                    recordLoginFail(account);
                    throw new Exception("账号 " + account + " 未在任何授权企业配置登录!");
                }
                verifyPassword(baseInfo, account, password);
                upgradeLegacyHashIfNeeded(baseInfo, account, password);
                matched = baseInfo;

                // 仅一家授权企业 → 直接登录；多家 → 弹选择（此时已验密）
                if (companies.size() > 1) {
                    clearLoginFail(account); // 密码本身是对的，不计失败
                    throw new MultiCompanyLoginException(companies);
                }
            }

            clearLoginFail(account);
            matched.setSuffix(databasesuffix);
            matched.setAccount(account);
            // 登录令牌携带公司名，异步任务锁提示可明确定位企业。
            fillCompanyName(matched, companies);
            // 校验通过即视为合法用户：清除可能因连续输错密码产生的封禁，避免“登录成功却被强制下线”的死循环
            tokenRevocation.unbanAccount(account);
            Integer pcr = userMapper.getPwdChangeRequired(matched.getAccount(), matched.getCompanyId(), databasesuffix);
            matched.setMustChangePassword(pcr != null && pcr == 1);
            matched.setPassword(null);
            matched.setSessionSeed(tokenRevocation.getSessionSeed(account));
            fillPermissionMenus(matched);
            String Token= JWTTokenUtils.getToken(matched);
            matched.setToken(Token);
            result.setData(matched);
        }
        catch(MultiCompanyLoginException mc){
            // SaaS 改造 P2：密码已验过，生成一次性确认令牌，避免企业选择时重复输入验证码
            String confirmToken = UUID.randomUUID().toString().replace("-", "");
            try {
                java.util.Map<String, Object> tokenData = new java.util.HashMap<>();
                tokenData.put("account", account);
                tokenData.put("password", password);
                redis.setex(CONFIRM_TOKEN_KEY_PREFIX + confirmToken, CONFIRM_TOKEN_TTL_SECONDS,
                        com.alibaba.fastjson.JSON.toJSONString(tokenData));
            } catch (Exception ex) {
                logger.warn("生成确认令牌异常: {}", ex.getMessage());
            }
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("needSelectCompany", true);
            data.put("companies", mc.getCompanies());
            data.put("confirmToken", confirmToken);
            result.setData(data);
            result.setMessage(mc.getMessage());
        }
        catch(Exception ax){
            result.raiseException(ax);
            log.error("LoginController.java 异常", ax);
        }
        return result;
    }

    /**
     * SaaS 改造 P2：企业选择确认端点。密码已在首次 /login 时验证，此处不再校验验证码。
     */
    @PostMapping("/confirmCompany")
    public successResult confirmCompany(String confirmToken, String companyId) {
        successResult result = new successResult();
        String key = null;
        try {
            if (StringUtils.isEmpty(confirmToken) || StringUtils.isEmpty(companyId)) {
                throw new Exception("缺少确认令牌或企业ID");
            }
            key = CONFIRM_TOKEN_KEY_PREFIX + confirmToken;
            String json = redis.get(key);
            if (StringUtils.isEmpty(json)) {
                throw new Exception("确认令牌已过期或无效，请重新登录");
            }
            // 注意：确认令牌不在数据库操作前消费。若租户数据库连接失败，
            // 若不保留令牌用户将被迫重新登录；此处改为“全部成功后才一次性消费”，
            // 使瞬时故障可在令牌 TTL 内重试，且错误信息可直接暴露真实原因。

            java.util.Map<String, Object> tokenData = com.alibaba.fastjson.JSON.parseObject(json);
            String account = (String) tokenData.get("account");

            LoginUserInfo matched = loadTenantUser(account, companyId.trim());
            // 密码已在首次 /login 时于基准公司验证，此处不再逐企业验密（密码与授权脱钩）
            clearLoginFail(account);
            matched.setSuffix(databasesuffix);
            matched.setAccount(account);
            fillCompanyName(matched, listCandidateCompanies(account));
            // 校验通过即视为合法用户：清除可能因连续输错密码产生的封禁，避免“登录成功却被强制下线”的死循环
            tokenRevocation.unbanAccount(account);
            Integer pcr2 = userMapper.getPwdChangeRequired(matched.getAccount(), matched.getCompanyId(), databasesuffix);
            matched.setMustChangePassword(pcr2 != null && pcr2 == 1);
            matched.setPassword(null);
            matched.setSessionSeed(tokenRevocation.getSessionSeed(account));
            fillPermissionMenus(matched);
            String Token = JWTTokenUtils.getToken(matched);
            matched.setToken(Token);
            result.setData(matched);
            // 仅在整条登录链路成功后才消费一次性确认令牌
            redis.del(key);
        } catch (Exception ax) {
            String msg = ax.getMessage();
            boolean jdbcFailure = msg != null && (msg.contains("Unable to acquire JDBC Connection")
                    || msg.contains("租户") && msg.contains("无法连接其数据库"));
            if (jdbcFailure) {
                // 租户数据库连接失败：保留令牌以便前端在 TTL 内重试；返回可定位的提示
                String hint = companyId == null ? "" : "企业(" + companyId.trim() + ")的";
                result.setSuccess(false);
                result.setCode(500);
                result.setMessage("登录失败：" + hint + "数据库连接异常，请检查该企业数据库是否可访问或联系系统管理员");
                log.error("confirmCompany 租户数据库连接失败 companyId={}", companyId, ax);
            } else {
                result.raiseException(ax);
                log.error("confirmCompany 异常", ax);
            }
        }
        return result;
    }

    /**
     * 个人信息界面「切换企业」：登录态内把当前账号换发为绑定目标企业的新令牌。
     * 多租户路由以 token 内嵌 companyId 为唯一来源，切换=签发绑新企业的新 JWT + 重载该企业菜单。
     * 安全：目标企业必须属于当前账号候选（实时白名单），越权直接拒绝；不引入明文密码/验证码。
     * 会话种子沿用当前值(不 bump)，原企业 token 保留至自然过期，支持切回与多标签页不被误杀。
     * 失败优雅降级：目标企业数据库不可达等异常返回失败，不消费/不影响当前登录态，用户可留在原企业。
     */
    @PostMapping("/switchCompany")
    public successResult switchCompany(String companyId) {
        successResult result = new successResult();
        try {
            LoginUserInfo current = CompanyContext.get();
            if (current == null || current.getAccount() == null || current.getAccount().trim().isEmpty()) {
                result.raiseException(new Exception("未登录或登录已失效"));
                return result;
            }
            String account = current.getAccount();
            if (companyId == null || StringUtils.isEmpty(companyId.trim())) {
                throw new Exception("缺少目标企业ID");
            }
            String target = companyId.trim();
            if (current.getCompanyId() != null && target.equals(current.getCompanyId())) {
                throw new Exception("当前已在该企业，无需切换");
            }
            List<java.util.Map<String, Object>> companies = listCandidateCompanies(account);
            if (companies.isEmpty()) {
                throw new Exception(account + "在系统中不存在!");
            }
            boolean belongs = companies.stream()
                    .anyMatch(c -> target.equals(String.valueOf(c.get("companyId"))));
            if (!belongs) {
                throw new Exception("当前账号不可访问企业 " + target + "，无法切换");
            }
            LoginUserInfo matched = loadTenantUser(account, target);
            matched.setSuffix(databasesuffix);
            matched.setAccount(account);
            fillCompanyName(matched, companies);
            // 切换企业不动会话种子：沿用当前值，原企业令牌保留至自然过期（可切回/多标签不误杀）
            matched.setSessionSeed(tokenRevocation.getSessionSeed(account));
            matched.setPassword(null);
            Integer pcr = userMapper.getPwdChangeRequired(matched.getAccount(), matched.getCompanyId(), databasesuffix);
            matched.setMustChangePassword(pcr != null && pcr == 1);
            // 装载目标企业 menuTree（fillPermissionMenus 内部 set 目标企业上下文，会顺带触发目标租户池建池预热）
            fillPermissionMenus(matched);
            String token = JWTTokenUtils.getToken(matched);
            matched.setToken(token);
            result.setData(matched);
        } catch (Exception ax) {
            String msg = ax == null ? "" : (ax.getMessage() == null ? "" : ax.getMessage());
            boolean jdbcFailure = msg.contains("Unable to acquire JDBC Connection")
                    || (msg.contains("租户") && msg.contains("无法连接其数据库"));
            if (jdbcFailure) {
                // 目标企业库不可达：保持当前登录态，返回可定位的提示（前端可留在原企业）
                result.setSuccess(false);
                result.setCode(500);
                result.setMessage("切换失败：企业(" + (companyId == null ? "" : companyId.trim())
                        + ")的数据库连接异常，请检查该企业数据库是否可访问或联系系统管理员");
                log.error("switchCompany 目标租户数据库连接失败 companyId={}", companyId, ax);
            } else {
                result.raiseException(ax);
                log.error("switchCompany 异常", ax);
            }
        }
        return result;
    }

    /**
     * 个人信息界面「切换企业」候选列表：返回当前账号可访问的企业(实时白名单)与当前所在企业。
     */
    @PostMapping("/switchCompany/candidates")
    public successResult switchCompanyCandidates() {
        successResult result = new successResult();
        try {
            LoginUserInfo current = CompanyContext.get();
            if (current == null || current.getAccount() == null || current.getAccount().trim().isEmpty()) {
                result.raiseException(new Exception("未登录或登录已失效"));
                return result;
            }
            String account = current.getAccount();
            List<java.util.Map<String, Object>> companies = listCandidateCompanies(account);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("currentCompanyId", current.getCompanyId());
            data.put("companies", companies);
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
            log.error("switchCompanyCandidates 异常", ax);
        }
        return result;
    }

    /** 去重后的候选企业列表 */
    private List<java.util.Map<String, Object>> listCandidateCompanies(String account) throws Exception {
        List<java.util.Map<String, Object>> companies =
                userMapper.getCompaniesByUserName(account, systemBase);
        List<java.util.Map<String, Object>> distinct = new java.util.ArrayList<>();
        if (companies != null) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (java.util.Map<String, Object> c : companies) {
                String id = c.get("companyId") == null ? null : c.get("companyId").toString().trim();
                if (!StringUtils.isEmpty(id) && seen.add(id)) {
                    distinct.add(c);
                }
            }
        }
        if (distinct.isEmpty()) {
            throw new Exception(account + "在系统中不存在!");
        }
        return distinct;
    }

    /** 加载租户用户并做存在性/禁用检查 */
    private LoginUserInfo loadTenantUser(String account, String companyId) throws Exception {
        LoginUserInfo info = userMapper.getByAcountAndCompanyID(account, companyId, databasesuffix);
        if (info == null) {
            recordLoginFail(account);
            throw new Exception(account + " 在企业 " + companyId + " 中不存在或未配置登录!");
        }
        Boolean canLogin = info.getCanLogin();
        if (canLogin == null || !canLogin) {
            throw new Exception(account + " 已被禁止登录系统!");
        }
        return info;
    }

    /** 静默加载租户用户：无账号返回 null（不抛异常、不计失败），供基准公司缺账号时回退 */
    private LoginUserInfo loadTenantUserQuiet(String account, String companyId) {
        try {
            return userMapper.getByAcountAndCompanyID(account, companyId, databasesuffix);
        } catch (Exception ex) {
            return null;
        }
    }

    private void fillCompanyName(LoginUserInfo info, List<java.util.Map<String, Object>> companies) {
        if (info == null || info.getCompanyId() == null || companies == null) {
            return;
        }
        for (java.util.Map<String, Object> company : companies) {
            if (info.getCompanyId().equals(String.valueOf(company.get("companyId")))) {
                Object companyName = company.get("companyName");
                if (companyName != null && !StringUtils.isEmpty(String.valueOf(companyName).trim())) {
                    info.setCompanyName(String.valueOf(companyName).trim());
                }
                return;
            }
        }
    }

    /** 密码比对（BCrypt/旧MD5 自适应），失败计入锁定 */
    private void verifyPassword(LoginUserInfo info, String account, String rawPassword) {
        if (!passwordService.matches(rawPassword, info.getPassword())) {
            recordLoginFail(account);
            throw new IllegalArgumentException("登录密码不正确!");
        }
    }

    /**
     * SaaS 安全改造 P0-2：旧双重 MD5 密码在登录成功后透明升级为 BCrypt。
     * 升级失败仅记录日志，不影响本次登录。
     */
    private void upgradeLegacyHashIfNeeded(LoginUserInfo info, String account, String rawPassword) {
        try {
            if (!passwordService.needsUpgrade(info.getPassword())) return;
            String newHash = passwordService.upgradeStoredHash(rawPassword);
            int rows = userMapper.upgradePasswordToBcrypt(account, info.getCompanyId(), databasesuffix, newHash);
            if (rows > 0) {
                logger.info("【密码升级】账号 {} 在租户 {} 的密码已从 MD5 升级为 BCrypt", account, info.getCompanyId());
            }
        } catch (Exception ex) {
            logger.warn("【密码升级】透明升级失败（不影响登录）: {}", ex.getMessage());
        }
    }

    /**
     * 登录失败锁定：连续失败 LOGIN_MAX_FAIL_COUNT 次后，锁定 LOGIN_LOCK_SECONDS 秒
     */
    private void checkAccountNotLocked(String account) {
        if (account == null || account.trim().isEmpty()) return;
        if (redis.exists(LOGIN_LOCK_KEY_PREFIX + account)) {
            Long ttl = redis.ttl(LOGIN_LOCK_KEY_PREFIX + account);
            long minutes = Math.max(1, ttl == null ? LOGIN_LOCK_SECONDS : ttl) / 60;
            throw new IllegalArgumentException("登录失败次数过多，账号已锁定，请约 " + minutes + " 分钟后再试");
        }
    }

    private void recordLoginFail(String account) {
        try {
            String failKey = LOGIN_FAIL_KEY_PREFIX + account;
            Long count = redis.incr(failKey);
            if (count != null && count == 1) {
                redis.expire(failKey, 900);
            }
            if (count != null && count >= LOGIN_MAX_FAIL_COUNT) {
                redis.setex(LOGIN_LOCK_KEY_PREFIX + account, LOGIN_LOCK_SECONDS, 1);
                redis.del(failKey);
            }
        } catch (Exception ex) {
            logger.warn("记录登录失败次数异常(不影响登录主流程): {}", ex.getMessage());
        }
    }

    private void clearLoginFail(String account) {
        try {
            redis.del(LOGIN_FAIL_KEY_PREFIX + account);
        } catch (Exception ex) {
            logger.warn("清除登录失败计数异常: {}", ex.getMessage());
        }
    }

    /** 一号多租异常：携带可选企业列表 */
    public static class MultiCompanyLoginException extends Exception {
        private final List<java.util.Map<String, Object>> companies;

        public MultiCompanyLoginException(List<java.util.Map<String, Object>> companies) {
            super("该账号可登录多个企业，请选择");
            this.companies = companies;
        }

        public List<java.util.Map<String, Object>> getCompanies() {
            return companies;
        }
    }

    private void fillPermissionMenus(LoginUserInfo info) {
        LoginUserInfo previousContext = CompanyContext.get();
        CompanyContext.set(info);
        try {
            Integer loginRoleId = parseRoleId(info.getRoleId());
            List<tbrolemenu> roleMenus = rolemenuRepository.getAllByRoleId(loginRoleId);
            List<Integer> menuIds = roleMenus.stream().map(tbrolemenu::getMenuId).collect(Collectors.toList());
            List<tbmenu> allMenus = tbMenuService.queryAllMenus();
            menuPermissionSupport.requireRoleHasEnabledSubMenuIds(loginRoleId, menuIds, allMenus);
            info.setRolemenu(menuPermissionSupport.buildAuthorizedMenuNames(allMenus, menuIds));
            info.setMenuTree(menuPermissionSupport.buildAuthorizedMenuTree(allMenus, menuIds));
        } finally {
            CompanyContext.set(previousContext);
        }
    }

    private Integer parseRoleId(String roleId) {
        if (StringUtils.isEmpty(roleId)) {
            throw new IllegalArgumentException("登录用户必须绑定角色");
        }
        try {
            return Integer.parseInt(roleId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("登录用户角色配置不正确");
        }
    }

    /**
     * A：自助改密（登录态下）。原密码连续错 5 次 → 锁定账号(canLogin=0)，需平台超管重置解锁。
     * 新密码统一走 BCrypt（与开通/登录一致，不影响既有账号）。
     */
    /**
     * 退出登录：把当前令牌的 jti 写入 Redis 黑名单，使其立即失效（前端应清除本地令牌）。
     * 令牌无效/已失效时也幂等返回成功。
     */
    @PostMapping("/logout")
    public successResult logout(String token) {
        successResult result = new successResult();
        try {
            if (StringUtils.isEmpty(token)) {
                token = "";
            }
            if (!StringUtils.isEmpty(token)) {
                // 仅当令牌本身有效时才登记吊销，避免对非法串写无效黑名单
                JWTTokenUtils.GetByToken(token);
                String jti = JWTTokenUtils.getJti(token);
                if (jti != null) {
                    tokenRevocation.revokeToken(jti);
                }
            }
            result.setMessage("已退出登录");
        } catch (Exception e) {
            // 令牌本就无效，视为已退出
            result.setMessage("已退出登录");
        }
        return result;
    }

    @PostMapping("/changePassword")
    public successResult changePassword(String oldPassword, String newPassword) {
        successResult result = new successResult();
        try {
            LoginUserInfo me = CompanyContext.get();
            if (me == null) {
                throw new Exception("未登录或登录已失效");
            }
            LoginUserInfo info = userMapper.getByAcountAndCompanyID(me.getAccount(), me.getCompanyId(), databasesuffix);
            if (info == null) {
                throw new Exception("当前账号不存在");
            }
            if (oldPassword == null || oldPassword.isEmpty()) {
                throw new Exception("请输入原密码");
            }
            if (newPassword == null || newPassword.length() < 6) {
                throw new Exception("新密码至少 6 位");
            }
            if (!passwordService.matches(oldPassword, info.getPassword())) {
                userMapper.incChangePwdFail(me.getAccount(), me.getCompanyId(), databasesuffix);
                Integer fail = userMapper.getChangePwdFail(me.getAccount(), me.getCompanyId(), databasesuffix);
                if (fail != null && fail >= 5) {
                    // 账号锁定：封禁其所有在途令牌，使其立即失效（需平台超管重置解锁）
                    tokenRevocation.banAccount(me.getAccount());
                    throw new Exception("原密码连续错误 5 次，账号已锁定，请联系平台超管重置");
                }
                throw new Exception("原密码错误");
            }
            // 兼容历史双重 MD5：此处统一升级为 BCrypt
            String hash = passwordService.encode(newPassword);
            userMapper.selfChangePassword(me.getAccount(), me.getCompanyId(), databasesuffix, hash);
            // 改密后使该账号所有在途会话失效（含当前会话），需重新登录；但不锁定账号，可凭新密码登录
            tokenRevocation.bumpSessionSeed(me.getAccount());
            result.setMessage("密码修改成功，请妥善保管");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }
}
