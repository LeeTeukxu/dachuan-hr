package com.tianye.hrsystem.modules.miniapp.support;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.config.CompanyDataSourceProvider;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 小程序统计「公司切换」可见公司支持（B方案，2026-09-10）。
 *
 * <p>授权数据存系统库 hrsystem.mp_employee_visible_company（键=openid+mobile，公司级）。
 * 请求员工可见集合 = {本登录公司} ∪ (系统表 openid 匹配的 company_id)；
 * 无任何授权行 = 只自家公司（严格模式）。</p>
 *
 * <p>所有 /mp/dashboard 取数（含集团总表聚合）都只允许在可见集合内，
 * 由 {@link com.tianye.hrsystem.controller.MiniAppDashboardController} 在入口统一校验。</p>
 */
@Component
public class MiniAppVisibleCompanySupport {

    private static final Logger log = LoggerFactory.getLogger(MiniAppVisibleCompanySupport.class);

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Value("${hrm.system.database}")
    private String systemDatabase;

    /**
     * 解析当前登录员工在本登录公司的 openid。openid 为空则无法跨公司授权，仅自家可见。
     */
    public String resolveCurrentEmployeeOpenid() {
        LoginUserInfo info = CompanyContext.get();
        if (info == null || info.getEmployeeId() == null) {
            return null;
        }
        try {
            HrmEmployee employee = employeeRepository.findById(info.getEmployeeId()).orElse(null);
            return employee == null ? null : StringUtils.trimToNull(employee.getOpenid());
        } catch (Exception e) {
            log.warn("解析员工openid失败 employeeId={}: {}", info.getEmployeeId(), e.getMessage());
            return null;
        }
    }

    /**
     * 当前登录公司 id（CompanyContext.companyId）。
     */
    public String currentCompanyId() {
        LoginUserInfo info = CompanyContext.get();
        return info == null ? null : info.getCompanyId();
    }

    /**
     * 员工可见公司 id 集合（含本登录公司）。无授权 = 只自家公司。
     */
    public Set<String> resolveVisibleCompanyIds() {
        String home = currentCompanyId();
        if (home == null) {
            return Collections.emptySet();
        }
        Set<String> visible = new LinkedHashSet<>();
        visible.add(home);
        String openid = resolveCurrentEmployeeOpenid();
        if (openid != null) {
            for (String cid : queryVisibleByOpenid(openid)) {
                if (StringUtils.isNotBlank(cid)) {
                    visible.add(cid);
                }
            }
        }
        return visible;
    }

    /**
     * 目标公司是否在当前员工可见集合内。
     */
    public boolean canAccess(String targetCompanyId) {
        if (StringUtils.isBlank(targetCompanyId)) {
            return false;
        }
        return resolveVisibleCompanyIds().contains(targetCompanyId);
    }

    /**
     * 从系统库按 openid 查询被授权的可见公司 id。
     */
    public List<String> queryVisibleByOpenid(String openid) {
        if (StringUtils.isBlank(openid)) {
            return Collections.emptyList();
        }
        DataSource ds = null;
        try {
            ds = CompanyDataSourceProvider.getDataSource("Default");
        } catch (Exception e) {
            log.warn("获取系统数据源失败: {}", e.getMessage());
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        String sql = "SELECT company_id FROM " + systemDatabase
                + ".mp_employee_visible_company WHERE openid = ? ORDER BY company_id";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, openid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("company_id"));
                }
            }
        } catch (Exception e) {
            log.warn("查询可见公司失败 openid={}: {}", openid, e.getMessage());
        }
        return ids;
    }

    /**
     * PC 端保存某员工的可见公司授权：全量替换（先删该 openid 的全部行，再按列表插入）。
     * @return 实际写入行数
     */
    public int saveVisibleCompanies(String openid, String mobile, List<String> visibleCompanyIds) {
        if (StringUtils.isBlank(openid)) {
            return 0;
        }
        DataSource ds;
        try {
            ds = CompanyDataSourceProvider.getDataSource("Default");
        } catch (Exception e) {
            log.warn("获取系统数据源失败: {}", e.getMessage());
            return 0;
        }
        String table = systemDatabase + ".mp_employee_visible_company";
        int inserted = 0;
        try (Connection conn = ds.getConnection()) {
            // 先清空该 openid 旧授权
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM " + table + " WHERE openid = ?")) {
                del.setString(1, openid);
                del.executeUpdate();
            }
            if (visibleCompanyIds != null) {
                for (String companyId : visibleCompanyIds) {
                    if (StringUtils.isBlank(companyId)) {
                        continue;
                    }
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO " + table
                                    + " (openid, mobile, company_id) VALUES (?, ?, ?)")) {
                        ins.setString(1, openid);
                        ins.setString(2, mobile == null ? "" : mobile);
                        ins.setString(3, companyId.trim());
                        inserted += ins.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("保存可见公司授权失败 openid={}: {}", openid, e.getMessage());
        }
        return inserted;
    }
}
