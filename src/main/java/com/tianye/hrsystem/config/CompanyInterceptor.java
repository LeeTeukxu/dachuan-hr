package com.tianye.hrsystem.config;

import lombok.extern.slf4j.Slf4j;


import com.alibaba.fastjson.JSON;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.entity.vo.EmployeeInfo;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class CompanyInterceptor extends HandlerInterceptorAdapter {
    List<String> skipUrls = Arrays.asList(
            "/hrsystem/login",
            "/hrsystem/captcha/generate",
            "/hrsystem/mp/login",
            "/hrsystem/mp/login/bindEmployee",
            "/hrsystem/mp/login/bindCompany",
            "/hrsystem/confirmCompany",
            "/hrsystem/logout");
    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;
@Autowired
ApiPermissionPathSupport apiPermissionPathSupport;
@Autowired
com.tianye.hrsystem.common.TokenRevocationService tokenRevocationService;
@Autowired
com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService miniAppPermissionService;
@Autowired
com.tianye.hrsystem.mapper.LoginUserMapper loginUserMapper;
Logger logger= LoggerFactory.getLogger(CompanyInterceptor.class);
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView
            modelAndView) throws Exception {
        CompanyContext.clear();
    }

    /**
     * 无论 preHandle 返回 true/false 还是抛异常，afterCompletion 都会执行，
     * 保证请求线程的 CompanyContext(ThreadLocal) 一定被清理，避免 LoginUserInfo 在线程上泄漏。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CompanyContext.clear();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String urlPath = request.getRequestURI();
        boolean hasLogin = false;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (skipUrls.contains(urlPath)) return true;
        String token = request.getParameter("token");
        if(org.apache.commons.lang.StringUtils.isEmpty(token))
        {
            token= request.getHeader("token");
        }
        successResult result = new successResult();
        response.setCharacterEncoding("utf-8");
        if (StringUtils.isEmpty(token) == false) {
            try {
                LoginUserInfo Info = JWTTokenUtils.GetByToken(token);

                // 令牌吊销校验：退出登录(jti 黑名单) / 账号被禁用或强制下线(账号封禁)
                String jti = JWTTokenUtils.getJti(token);
                if (tokenRevocationService.isTokenRevoked(jti)) {
                    result.setMessage("登录已失效，请重新登录");
                    result.setTimeOut(true);
                    hasLogin = false;
                    log.info("令牌已被吊销(jti={})，拒绝访问", jti);
                    writeReject(response, result);
                    return false;
                } else if (tokenRevocationService.isAccountBanned(Info.getAccount())) {
                    result.setMessage("账号已被禁用或强制下线，请重新登录");
                    result.setTimeOut(true);
                    hasLogin = false;
                    log.info("账号已被封禁(account={})，拒绝访问", Info.getAccount());
                    writeReject(response, result);
                    return false;
                } else if (tokenRevocationService.getSessionSeed(Info.getAccount()) !=
                        (Info.getSessionSeed() == null ? 0 : Info.getSessionSeed())) {
                    // 凭证（密码/角色/企业等）变更导致的强制下线：旧令牌种子与最新种子不一致即作废，
                    // 但允许凭新密码重新登录（区别于 isAccountBanned 的账号锁定）
                    result.setMessage("账号凭证已变更，请重新登录");
                    result.setTimeOut(true);
                    hasLogin = false;
                    log.info("会话种子已失效(account={})，强制重新登录", Info.getAccount());
                    writeReject(response, result);
                    return false;
                }

                EmployeeInfo Emp=new EmployeeInfo();
                Emp.setDeptId(Info.getDepIdValue());
                Emp.setDeptName(Info.getDepName());
                Emp.setEmployeeName(Info.getUserName());
                CompanyContext.set(Info);
                List<String> requiredMenuPaths = apiPermissionPathSupport.resolveRequiredMenuPaths(urlPath, request.getContextPath());
                String miniappPrefix = request.getContextPath() + "/mp";
                boolean isMiniappPath = urlPath.equals(miniappPrefix) || urlPath.startsWith(miniappPrefix + "/");
                String strippedPath = apiPermissionPathSupport.stripContextPath(urlPath, request.getContextPath());
                // 小程序员工级能力（2026-09-09 起）：/mp/dashboard/* 等接口不再挂菜单，
                // 改为按员工在 mp_schedule_permission 的配置判定，粒度到员工而非公司。
                String requiredAbility = isMiniappPath ? apiPermissionPathSupport.resolveRequiredAbility(strippedPath) : null;
                boolean allowed;
                if (!requiredMenuPaths.isEmpty()) {
                    // 命中菜单权限映射：PC 账号按菜单权限校验；
                    // 小程序员工 token 无菜单树，按租户级判定——任一角色勾选了所需菜单即视为该租户开通此功能
                    if (isMiniappPath
                            && (Info.getMenuTree() == null || Info.getMenuTree().isEmpty())) {
                        allowed = hasTenantMenuPermission(Info, requiredMenuPaths);
                        if (!allowed) result.raiseException(new Exception("当前企业未开通该小程序功能，请联系管理员在角色权限中勾选"));
                    } else {
                        allowed = hasMenuPermission(Info, requiredMenuPaths);
                        if (!allowed) result.raiseException(new Exception("当前账号没有访问该功能的权限"));
                    }
                } else if (requiredAbility != null) {
                    // 小程序员工级能力：按员工在 mp_schedule_permission 的配置判定（严格模式，未配置一律拒绝）
                    Long employeeId = Info.getEmployeeId();
                    boolean abilityOk = employeeId != null
                            && miniAppPermissionService.hasAbility(employeeId, requiredAbility);
                    allowed = abilityOk;
                    if (!allowed) result.raiseException(new Exception("当前员工未开通该小程序功能，请联系管理员在「小程序权限」中配置"));
                } else if (isMiniappPath) {
                    // 小程序端：员工身份无菜单树，仅校验 token；数据权限（本人数据/直属上级）在各接口内校验
                    allowed = true;
                } else {
                    // 默认拒绝：不在权限映射表内的管理端路径仅限操作员 token（PC 登录 token 均含 account），
                    // 防止员工 token 访问 /backup、/companyPermission 等映射表外的敏感端点
                    allowed = StringUtils.isNotEmpty(Info.getAccount());
                    if (!allowed) result.raiseException(new Exception("当前账号没有访问该功能的权限"));
                }
                if (allowed) {
                hasLogin = true;
                logger.info("Hr_"+Info.getCompanyId()+databasesuffix+"的用户:"+Info.getUserName()+"登录成功！");
                }
            } catch (SignatureVerificationException e) {
                log.error("CompanyInterceptor.java 异常", e);
                result.raiseException(new Exception("无效签名！"));
            } catch (TokenExpiredException e) {
                result.setMessage("token过期");
                // SaaS 改造：标记会话超时，前端据此清除本地凭证并跳转登录页
                result.setTimeOut(true);
            } catch (AlgorithmMismatchException e) {
                log.error("CompanyInterceptor.java 异常", e);
                result.raiseException(new Exception("算法不一致"));
            } catch (Exception e) {
                // SaaS 改造：无效 token（含跨环境残留的旧凭证）统一标记为会话失效
                logger.warn("token校验失败: {}", e.getMessage());
                result.setMessage("token无效，请重新登录");
                result.setTimeOut(true);
            }
        } else {
            result.setMessage("请输入token");
            result.setTimeOut(true);
        }
        if (hasLogin == false) {
            String V=JSON.toJSONString(result);
            log.info(V);
            log.info(response.getCharacterEncoding());
            response.getWriter().print(V);
            return false;
        } else return true;
//        return true;
    }

    /** 拒绝访问：写出 JSON 结果（避免 getWriter 抛 IOException 影响主流程） */
    private void writeReject(HttpServletResponse response, successResult result) {
        try {
            String V = JSON.toJSONString(result);
            response.getWriter().print(V);
        } catch (java.io.IOException ex) {
            logger.warn("写出拒绝响应失败: {}", ex.getMessage());
        }
    }

    /**
     * 租户级菜单开关：小程序员工 token 不携带菜单树，查该租户库是否存在任一角色勾选了所需菜单路径。
     * 权限页勾选"排班数据加载"= 给该公司开通此功能。
     */
    private boolean hasTenantMenuPermission(LoginUserInfo info, List<String> requiredMenuPaths) {
        if (requiredMenuPaths == null || requiredMenuPaths.isEmpty()) {
            return true;
        }
        String companyId = info == null ? null : info.getCompanyId();
        if (StringUtils.isEmpty(companyId)) {
            return false;
        }
        String suffix = StringUtils.isNotEmpty(info.getSuffix()) ? info.getSuffix() : databasesuffix;
        try {
            return loginUserMapper.countTenantMenuPermission(companyId, suffix, requiredMenuPaths) > 0;
        } catch (Exception e) {
            logger.warn("租户级菜单权限判定失败(company={}): {}", companyId, e.getMessage());
            return false;
        }
    }

    private boolean hasMenuPermission(LoginUserInfo info, List<String> requiredMenuPaths) {        if (requiredMenuPaths == null || requiredMenuPaths.isEmpty()) {
            return true;
        }
        if (info == null || info.getMenuTree() == null || info.getMenuTree().isEmpty()) {
            return false;
        }
        return requiredMenuPaths.stream().anyMatch(requiredMenuPath -> matchesMenuPath(info.getMenuTree(), requiredMenuPath));
    }

    /**
     * 递归匹配任意层级菜单 path。原实现只匹配「模块 + 一层子菜单」，
     * 三级菜单（如 权限管理>排班小程序>排班数据加载）会匹配不到而被误拒。
     */
    private boolean matchesMenuPath(List<tbmenu> menus, String requiredMenuPath) {
        if (menus == null || menus.isEmpty()) {
            return false;
        }
        for (tbmenu menu : menus) {
            if (menu == null) {
                continue;
            }
            if (requiredMenuPath.equals(menu.getPath())) {
                return true;
            }
            if (matchesMenuPath(menu.getChildren(), requiredMenuPath)) {
                return true;
            }
        }
        return false;
    }
}
