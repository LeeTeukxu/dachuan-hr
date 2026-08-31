package com.tianye.hrsystem.modules.loginuser.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.MD5Utils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmDept;
import com.tianye.hrsystem.mapper.HrmDeptMapper;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbmenu;
import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import com.tianye.hrsystem.modules.loginuser.entity.TbLoginUser;
import com.tianye.hrsystem.modules.loginuser.mapper.TbLoginUserMapper;
import com.tianye.hrsystem.modules.loginuser.vo.QueryLoginUserVO;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import com.tianye.hrsystem.modules.menu.service.MenuPermissionSupport;
import com.tianye.hrsystem.modules.menu.service.TbMenuService;
import com.tianye.hrsystem.modules.menu.service.TbRoleMenuService;
import com.tianye.hrsystem.modules.role.entity.TbRoleTypes;
import com.tianye.hrsystem.modules.role.mapper.TbRoleTypesMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class TbLoginUserService extends BaseServiceImpl<TbLoginUserMapper, TbLoginUser> {
    private static final Logger logger = LoggerFactory.getLogger(TbLoginUserService.class);

    @Autowired
    TbLoginUserMapper tbLoginUserMapper;
    @Autowired
    TbRoleTypesMapper tbRoleTypesMapper;
    @Autowired
    HrmDeptMapper hrmDeptMapper;
    @Autowired
    TbRoleMenuService tbRoleMenuService;
    @Autowired
    TbMenuService tbMenuService;
    @Autowired
    MenuPermissionSupport menuPermissionSupport;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    com.tianye.hrsystem.common.TokenRevocationService tokenRevocation;
    @Autowired
    LoginUserMapper sysLoginUserMapper;
    @Value("${hrm.system.database}")
    String systemDatabase;
    @Value("${hrm.system.databasesuffix}")
    String databaseSuffix;

    public Page<QueryLoginUserVO> queryLoginUserList(@RequestBody QueryLoginUserBO queryLoginUserBO) {
        return tbLoginUserMapper.queryLoginUserList(queryLoginUserBO.parse(), queryLoginUserBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer Add(@RequestBody QueryLoginUserBO queryLoginUserBO) {
        validateLoginUser(queryLoginUserBO);
        String account = queryLoginUserBO.getAccount().trim();
        queryLoginUserBO.setAccount(account);
        Integer userId = queryLoginUserBO.getId();
        boolean isCreate = userId == null;
        Optional<TbLoginUser> existedUser = isCreate ? Optional.empty() : lambdaQuery().eq(TbLoginUser::getId, userId).oneOpt();
        if (!isCreate && !existedUser.isPresent()) {
            throw new IllegalArgumentException("登录用户不存在");
        }
        String companyId = resolveCompanyId(queryLoginUserBO);
        String oldAccount = existedUser.map(TbLoginUser::getAccount).orElse(null);
        java.time.LocalDateTime oldCreateTime = existedUser.map(TbLoginUser::getCreatetime).orElse(null);
        boolean duplicateAccount = lambdaQuery()
                .eq(TbLoginUser::getAccount, account)
                .ne(!isCreate, TbLoginUser::getId, userId)
                .count() > 0;
        if (duplicateAccount) {
            throw new IllegalArgumentException("登录账号已存在");
        }
        // 账号已授权其他企业的校验仅在「新增」时拦截；
        // 编辑（含改密）时允许，因为企业权限管理可设置账号登录多个企业
        if (isCreate) {
            requireSystemAccountAvailable(account, companyId);
        }
        // 取被修改用户「修改前」已绑定的企业，用于判断是否变更了企业权限
        List<String> oldCompanyIds = (!isCreate && oldAccount != null)
                ? runWithDefaultCompanyContext(() -> tbLoginUserMapper.findSystemCompanyIdsByAccount(systemDatabase, oldAccount))
                : java.util.Collections.emptyList();

        TbLoginUser tbLoginUser = existedUser.orElseGet(TbLoginUser::new);
        String oldPassword = tbLoginUser.getPassword();
        BeanUtils.copyProperties(queryLoginUserBO, tbLoginUser);
        tbLoginUser.setAccount(account);
        if (!isCreate && queryLoginUserBO.getCreatetime() == null) {
            tbLoginUser.setCreatetime(oldCreateTime);
            queryLoginUserBO.setCreatetime(oldCreateTime);
        }
        if (!StringUtils.isEmpty(queryLoginUserBO.getPassword())) {
            tbLoginUser.setPassword(MD5Utils.enCode(queryLoginUserBO.getPassword()));
        } else if (!isCreate) {
            tbLoginUser.setPassword(oldPassword);
        }
        if (tbLoginUser.getCanlogin() == null) {
            tbLoginUser.setCanlogin(1);
        }
        queryLoginUserBO.setCompanyId(companyId);
        if (queryLoginUserBO.getCreatetime() == null) {
            queryLoginUserBO.setCreatetime(java.time.LocalDateTime.now());
            tbLoginUser.setCreatetime(queryLoginUserBO.getCreatetime());
        }
        saveOrUpdate(tbLoginUser);
        syncSystemLoginAccount(isCreate, companyId, oldAccount, account, queryLoginUserBO);
        // 编辑场景下修改密码：同步更新账号已授权的其他企业密码，保证可凭新密码登录各企业
        if (!isCreate && !StringUtils.isEmpty(queryLoginUserBO.getPassword())) {
            syncPasswordToAuthorizedCompanies(account, tbLoginUser.getPassword());
        }
        // 修改（非新增）成功后，若密码/角色/企业/账号发生变更，强制该用户下线（已有令牌立即失效）
        if (!isCreate) {
            boolean passwordChanged = !StringUtils.isEmpty(queryLoginUserBO.getPassword());
            boolean roleChanged = existedUser.isPresent()
                    && !java.util.Objects.equals(existedUser.get().getRoleid(), queryLoginUserBO.getRoleid());
            boolean accountChanged = oldAccount != null && !account.equals(oldAccount);
            boolean companyChanged = oldCompanyIds == null || !oldCompanyIds.contains(companyId);
            if (passwordChanged || roleChanged || accountChanged || companyChanged) {
                final String targetAccount = account;
                org.springframework.transaction.support.TransactionSynchronizationManager
                        .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                tokenRevocation.bumpSessionSeed(targetAccount);
                            }
                        });
            }
        }
        return 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer Delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("登录用户ID不能为空");
        }
        TbLoginUser loginUser = tbLoginUserMapper.selectById(id);
        if (loginUser == null) {
            throw new IllegalArgumentException("登录用户不存在");
        }
        String companyId = resolveCompanyId(null);
        tbLoginUserMapper.deleteById(id);
        runWithDefaultCompanyContext(() -> {
            tbLoginUserMapper.deleteSystemLoginAccount(systemDatabase, companyId, loginUser.getAccount());
            return 0;
        });
        return 0;
    }

    private void syncSystemLoginAccount(boolean isCreate, String companyId, String oldAccount, String account, QueryLoginUserBO queryLoginUserBO) {
        runWithDefaultCompanyContext(() -> {
            if (!isCreate && !account.equals(oldAccount)) {
                tbLoginUserMapper.deleteSystemLoginAccount(systemDatabase, companyId, oldAccount);
            }
            tbLoginUserMapper.deleteSystemLoginAccount(systemDatabase, companyId, account);
            tbLoginUserMapper.saveSystemLoginAccount(systemDatabase, queryLoginUserBO);
            return 0;
        });
    }

    private void requireSystemAccountAvailable(String account, String companyId) {
        List<String> mappedCompanyIds = runWithDefaultCompanyContext(() -> tbLoginUserMapper.findSystemCompanyIdsByAccount(systemDatabase, account));
        boolean mappedToOtherCompany = mappedCompanyIds != null && mappedCompanyIds.stream()
                .filter(mappedCompanyId -> !StringUtils.isEmpty(mappedCompanyId))
                .distinct()
                .anyMatch(mappedCompanyId -> !companyId.equals(mappedCompanyId));
        if (mappedToOtherCompany) {
            throw new IllegalArgumentException("登录账号已在其他公司存在");
        }
    }

    /**
     * 编辑改密时，将新密码同步到账号已授权的所有企业（含当前企业和其他企业），
     * 使账号在「企业权限管理」中授权的每个企业都能用新密码登录。
     */
    private void syncPasswordToAuthorizedCompanies(String account, String encodedPassword) {
        if (StringUtils.isEmpty(account) || StringUtils.isEmpty(encodedPassword)) {
            return;
        }
        List<String> companyIds = runWithDefaultCompanyContext(() -> tbLoginUserMapper.findSystemCompanyIdsByAccount(systemDatabase, account));
        if (companyIds == null || companyIds.isEmpty()) {
            return;
        }
        for (String companyId : companyIds) {
            if (StringUtils.isEmpty(companyId)) {
                continue;
            }
            try {
                sysLoginUserMapper.updateAccountPassword(account, companyId, databaseSuffix, encodedPassword);
            } catch (Exception ex) {
                logger.warn("【登录用户改密】同步企业 {} 的密码失败: {}", companyId, ex.getMessage());
            }
        }
    }


    private <T> T runWithDefaultCompanyContext(Supplier<T> supplier) {
        LoginUserInfo previousContext = CompanyContext.get();
        CompanyContext.clear();
        try {
            if (transactionManager == null) {
                return supplier.get();
            }
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transactionTemplate.execute(status -> supplier.get());
        } finally {
            if (previousContext == null) {
                CompanyContext.clear();
            } else {
                CompanyContext.set(previousContext);
            }
        }
    }

    private void validateLoginUser(QueryLoginUserBO queryLoginUserBO) {
        if (queryLoginUserBO == null) {
            throw new IllegalArgumentException("登录用户信息不能为空");
        }
        if (StringUtils.isEmpty(queryLoginUserBO.getAccount()) || StringUtils.isEmpty(queryLoginUserBO.getAccount().trim())) {
            throw new IllegalArgumentException("登录账号不能为空");
        }
        if (queryLoginUserBO.getId() == null && StringUtils.isEmpty(queryLoginUserBO.getPassword())) {
            throw new IllegalArgumentException("登录密码不能为空");
        }
        ensureLoginUserDepartment(queryLoginUserBO);
        if (queryLoginUserBO.getRoleid() == null) {
            throw new IllegalArgumentException("登录用户必须绑定角色");
        }
        TbRoleTypes role = tbRoleTypesMapper.selectById(queryLoginUserBO.getRoleid());
        if (role == null || Integer.valueOf(2).equals(role.getCanUse())) {
            throw new IllegalArgumentException("角色不存在或已停用");
        }
        List<TbRoleMenu> roleMenus = tbRoleMenuService.lambdaQuery()
                .eq(TbRoleMenu::getRoleId, queryLoginUserBO.getRoleid())
                .list();
        List<tbmenu> allMenus = tbMenuService.queryAllMenus();
        menuPermissionSupport.requireRoleHasEnabledSubMenu(queryLoginUserBO.getRoleid(), roleMenus, allMenus);
    }

    private String resolveCompanyId(QueryLoginUserBO queryLoginUserBO) {
        LoginUserInfo info = CompanyContext.get();
        if (info != null && !StringUtils.isEmpty(info.getCompanyId())) {
            return info.getCompanyId();
        }
        if (queryLoginUserBO != null && !StringUtils.isEmpty(queryLoginUserBO.getCompanyId())) {
            return queryLoginUserBO.getCompanyId();
        }
        throw new IllegalArgumentException("公司编号不能为空");
    }

    private void ensureLoginUserDepartment(QueryLoginUserBO queryLoginUserBO) {
        if (queryLoginUserBO.getDepid() == null) {
            if (queryLoginUserBO.getId() != null) {
                throw new IllegalArgumentException("登录用户必须选择部门");
            }
            HrmDept topLevelDept = queryTopLevelDept();
            if (topLevelDept == null || topLevelDept.getDeptId() == null) {
                throw new IllegalArgumentException("未找到顶级部门，无法默认所属部门");
            }
            queryLoginUserBO.setDepid(topLevelDept.getDeptId());
            return;
        }
        if (hrmDeptMapper.selectById(queryLoginUserBO.getDepid()) == null) {
            throw new IllegalArgumentException("登录用户部门不存在");
        }
    }

    private HrmDept queryTopLevelDept() {
        List<HrmDept> topLevelDeptList = hrmDeptMapper.selectList(new LambdaQueryWrapper<HrmDept>()
                .eq(HrmDept::getParentId, 0L)
                .orderByAsc(HrmDept::getDeptId)
                .last("limit 1"));
        if (topLevelDeptList == null || topLevelDeptList.isEmpty()) {
            return null;
        }
        return topLevelDeptList.get(0);
    }

    /** 强制某角色下所有登录用户下线（修改看板/角色权限后调用，受当前租户上下文约束） */
    public void forceLogoutByRole(Integer roleId) {
        if (roleId == null) {
            return;
        }
        List<TbLoginUser> users = lambdaQuery().eq(TbLoginUser::getRoleid, roleId).list();
        if (users == null) {
            return;
        }
        for (TbLoginUser u : users) {
            if (u.getAccount() != null && !u.getAccount().isEmpty()) {
                tokenRevocation.bumpSessionSeed(u.getAccount());
            }
        }
    }
}
