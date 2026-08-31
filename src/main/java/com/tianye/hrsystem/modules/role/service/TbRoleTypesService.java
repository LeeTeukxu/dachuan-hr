package com.tianye.hrsystem.modules.role.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.entity.TbRoleTypes;
import com.tianye.hrsystem.modules.role.mapper.TbRoleTypesMapper;
import com.tianye.hrsystem.modules.role.vo.QueryRoleTypesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class TbRoleTypesService {
    private static final Integer SYSTEM_ADMIN_ROLE_ID = 2;

    @Autowired
    TbRoleTypesMapper tbRoleTypesMapper;

    @Value("${hrm.system.databasesuffix:}")
    String databasesuffix;

    public Page<QueryRoleTypesVO> queryRoleTypesList(@RequestBody QueryRoleTypesBO queryRoleTypesBO) {
        return tbRoleTypesMapper.queryRoleTypesList(queryRoleTypesBO.parse(), queryRoleTypesBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer saveRoleType(@RequestBody QueryRoleTypesBO queryRoleTypesBO) {
        if (queryRoleTypesBO == null || StringUtils.isEmpty(queryRoleTypesBO.getName()) || StringUtils.isEmpty(queryRoleTypesBO.getName().trim())) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        TbRoleTypes role = new TbRoleTypes();
        role.setId(queryRoleTypesBO.getId());
        role.setName(queryRoleTypesBO.getName().trim());
        role.setPid(queryRoleTypesBO.getPid() == null ? 0 : queryRoleTypesBO.getPid());
        role.setCanUse(queryRoleTypesBO.getCanUse() == null ? 1 : queryRoleTypesBO.getCanUse());
        if (role.getId() != null && role.getId().equals(SYSTEM_ADMIN_ROLE_ID)) {
            throw new IllegalArgumentException("系统管理员角色不允许修改");
        }
        if (role.getId() == null) {
            tbRoleTypesMapper.insert(role);
        } else {
            tbRoleTypesMapper.updateById(role);
        }
        return 0;
    }

    /** 删除角色：系统管理员(roleId=2)不允许删除 */
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteRoleType(Integer roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        if (roleId.equals(SYSTEM_ADMIN_ROLE_ID)) {
            throw new IllegalArgumentException("系统管理员角色不允许删除");
        }
        LoginUserInfo info = CompanyContext.get();
        String companyId = info == null ? "" : info.getCompanyId();
        // 若该角色仍有用户引用，不允许删除
        int userCnt = tbRoleTypesMapper.countUsersByRole(companyId, databasesuffix, roleId);
        if (userCnt > 0) {
            throw new IllegalArgumentException("该角色下仍有 " + userCnt + " 个用户，请先移除后再删除");
        }
        // 级联清理角色-菜单绑定
        tbRoleTypesMapper.deleteRoleMenus(companyId, databasesuffix, roleId);
        // 删除角色本身（基于当前公司库）
        tbRoleTypesMapper.deleteById(roleId);
        return 0;
    }
}
