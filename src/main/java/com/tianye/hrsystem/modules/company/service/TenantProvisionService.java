package com.tianye.hrsystem.modules.company.service;

import com.tianye.hrsystem.common.MD5Utils;
import com.tianye.hrsystem.model.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SaaS 改造 P1-2：租户一键开通流水线
 * 步骤：校验 → 建库 → 表结构基线 → 视图（库名替换）→ 种子数据 → 默认部门+管理员 → 主库注册
 * 数据源为懒加载（CompanyDataSourceProvider），开通完成后无需重启即可访问新租户
 */
@Slf4j
@Service
public class TenantProvisionService {

    private static final Pattern COMPANY_ID_PATTERN = Pattern.compile("^\\d{4}$");
    private static final String SCHEMA_SCRIPT = "sql/tenant/schema-baseline.sql";
    private static final String EXAM_SCRIPT = "sql/tenant/exam-schema.sql";
    private static final String VIEWS_SCRIPT = "sql/tenant/views-template.sql";
    private static final String SEED_SCRIPT = "sql/tenant/seed-data.sql";
    /** 视图模板中硬编码的源库名，执行前替换为目标库名 */
    private static final String VIEW_SOURCE_DB = "hr_0003";

    @Value("${hrm.system.database}")
    private String systemDatabaseName;

    @org.springframework.beans.factory.annotation.Autowired
    private com.tianye.hrsystem.common.PasswordService passwordService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.tianye.hrsystem.mapper.LoginUserMapper loginUserMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.tianye.hrsystem.common.TokenRevocationService tokenRevocation;

    @Value("${hrm.system.databasesuffix:}")
    private String databasesuffix;

    @Value("${spring.datasource.username:root}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password:}")
    private String dataSourcePassword;

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/hrsystem}")
    private String springDatasourceUrl;

    /**
     * 开通新租户
     * @param companyId     4 位公司编码，如 0006
     * @param companyName   公司名
     * @param adminAccount  管理员登录账号
     * @param adminPassword 管理员明文密码（入库前 MD5）
     * @return 结果消息
     */
    public String provision(String companyId, String companyName,
                            String adminAccount, String adminPassword, String adminName,
                            String ddAppKey, String ddAppsecret, String ddAgentId) throws Exception {
        // 1. 校验
        if (companyId == null || !COMPANY_ID_PATTERN.matcher(companyId).matches()) {
            throw new IllegalArgumentException("公司编码必须是 4 位数字，如 0006");
        }
        if (StringUtils.isEmpty(companyName)) {
            throw new IllegalArgumentException("公司名称不能为空");
        }
        if (StringUtils.isEmpty(adminAccount) || StringUtils.isEmpty(adminPassword)) {
            throw new IllegalArgumentException("管理员账号和密码不能为空");
        }

        String tenantDb = "hr_" + companyId;

        try (Connection conn = openMainConnection()) {
            try {
                if (companyExists(conn, companyId)) {
                    throw new IllegalArgumentException("公司编码 " + companyId + " 已存在，不能重复开通");
                }

                // 2. 建库（gbk 与现有租户库保持一致）
                executeUpdate(conn, "CREATE DATABASE IF NOT EXISTS `" + tenantDb
                        + "` DEFAULT CHARACTER SET gbk COLLATE gbk_chinese_ci");

                // 3. 表结构基线
                use(conn, tenantDb);
                executeScript(conn, readClasspathScript(SCHEMA_SCRIPT));

                // 3.1 考试系统表结构
                executeScript(conn, readClasspathScript(EXAM_SCRIPT));

                // 4. 视图（模板内硬编码 hr_0003，替换为目标库名）
                String viewsSql = readClasspathScript(VIEWS_SCRIPT)
                        .replace("`" + VIEW_SOURCE_DB + "`", "`" + tenantDb + "`")
                        .replace(VIEW_SOURCE_DB, tenantDb);
                executeScript(conn, viewsSql);

                // 5. 种子数据（菜单/角色/角色菜单/自定义字段）
                executeScript(conn, readClasspathScript(SEED_SCRIPT));

                // 5.1 插入 tbsettingmenu 子菜单（依赖根菜单 f_id，需动态获取）
                insertSettingMenuChildren(conn, tenantDb);

                // 6. 默认部门 + 管理员账号
                long deptId = insertDefaultDept(conn, tenantDb, companyName);
                insertAdminUser(conn, tenantDb, adminAccount, adminPassword,
                        StringUtils.isEmpty(adminName) ? "管理员" : adminName, deptId);

                // 7. 钉钉配置（必填）
                insertDdAccount(conn, systemDatabaseName, companyId, ddAppKey, ddAppsecret, ddAgentId);

                // 8. 切回主库并注册账号与公司（url 格式与 ConnectionParsor 解析规则一致）
                use(conn, systemDatabaseName);
                registerAccountInMainDb(conn, companyId, adminAccount);
                insertCompanyRow(conn, companyId, companyName, tenantDb);

                log.info("【租户开通】成功 companyId={} db={} company={}", companyId, tenantDb, companyName);
                return "租户开通成功：" + companyName + "(" + companyId + ")，库名 " + tenantDb
                        + "，管理员账号 " + adminAccount;
            } catch (Exception e) {
                use(conn, systemDatabaseName);
                log.error("【租户开通】失败 companyId={}", companyId, e);
                throw new Exception("租户开通失败: " + e.getMessage(), e);
            }
        }
    }

    private static final Pattern REAL_DB_PATTERN = Pattern.compile("^hr_\\d+$");

    /**
     * 列出全部租户库及信息，并计算下一可用公司编码。
     * 数据来源：真实库（information_schema）+ 主库注册表（tbcompanylist）。
     */
    public Map<String, Object> listTenants() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> tenants = new ArrayList<>();
        try (Connection conn = openMainConnection()) {
            // 1. 真实库
            Map<String, String> realDbs = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'hr\\_%'")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String db = rs.getString(1);
                    if (REAL_DB_PATTERN.matcher(db).matches()) {
                        realDbs.put(db, db);
                    }
                }
            }
            // 2. 注册表（按 database 索引）
            Map<String, Map<String, Object>> registered = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT companyId, companyName, `database`, createTime FROM tbcompanylist")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("companyId", rs.getString("companyId"));
                    row.put("companyName", rs.getString("companyName"));
                    row.put("database", rs.getString("database"));
                    row.put("createTime", rs.getTimestamp("createTime"));
                    String db = rs.getString("database");
                    if (db != null) registered.put(db, row);
                }
            }
            // 3. 真实库条目
            int maxCode = 0;
            for (String db : realDbs.keySet()) {
                int code = Integer.parseInt(db.substring("hr_".length()));
                maxCode = Math.max(maxCode, code);
                Map<String, Object> reg = registered.get(db);
                boolean isReg = reg != null;
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("database", db);
                e.put("companyId", String.format("%04d", code));
                e.put("companyName", isReg ? reg.get("companyName") : null);
                e.put("createTime", isReg ? reg.get("createTime") : null);
                e.put("status", isReg ? "正常" : "仅库未注册");
                fillStats(conn, e, db);
                tenants.add(e);
            }
            // 4. 已注册但真实库缺失
            for (Map<String, Object> reg : registered.values()) {
                String db = (String) reg.get("database");
                if (db == null || !realDbs.containsKey(db)) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("database", db);
                    e.put("companyId", reg.get("companyId"));
                    e.put("companyName", reg.get("companyName"));
                    e.put("createTime", reg.get("createTime"));
                    e.put("status", "仅注册无库");
                    e.put("tableCount", -1);
                    e.put("roleCount", -1);
                    e.put("userCount", -1);
                    tenants.add(e);
                }
            }
            result.put("tenants", tenants);
            result.put("nextCompanyId", String.format("%04d", maxCode + 1));
            return result;
        }
    }

    private void fillStats(Connection conn, Map<String, Object> e, String db) {
        // 表数量
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?")) {
            ps.setString(1, db);
            ResultSet rs = ps.executeQuery();
            rs.next();
            e.put("tableCount", rs.getInt(1));
        } catch (Exception ex) {
            e.put("tableCount", -1);
        }
        // 角色数 / 用户数（按已校验库名拼接）
        for (String stat : new String[]{"roleCount:tbroletypes", "userCount:tbloginuser"}) {
            String[] kv = stat.split(":");
            String key = kv[0], table = kv[1];
            String sql = "SELECT COUNT(*) FROM `" + db + "`." + table;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                e.put(key, rs.getInt(1));
            } catch (Exception ex) {
                e.put(key, -1);
            }
        }
    }

    /**
     * 获取已有租户的钉钉配置列表（用于开户时绑定）
     */
    public List<Map<String, Object>> listDdAccounts() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = openMainConnection()) {
            // ddAccount 与 tbcompanylist 的 companyId 列 collation 可能不一致（历史建表遗留），
            // 显式 COLLATE 避免跨表比较抛 "Illegal mix of collations"
            String sql = "SELECT d.companyId, c.companyName, d.appKey, d.appsecret, d.agentId " +
                    "FROM `ddAccount` d LEFT JOIN tbcompanylist c ON d.companyId = c.companyId COLLATE utf8mb4_0900_ai_ci " +
                    "ORDER BY d.companyId";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("companyId", rs.getString("companyId"));
                    row.put("companyName", rs.getString("companyName"));
                    row.put("appKey", rs.getString("appKey"));
                    row.put("appsecret", rs.getString("appsecret"));
                    row.put("agentId", rs.getString("agentId"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    private Connection openMainConnection() throws Exception {
        DataSourceHolder holder = DataSourceHolder.INSTANCE;
        if (holder.dataSource == null) {
            synchronized (DataSourceHolder.class) {
                if (holder.dataSource == null) {
                    holder.dataSource = com.tianye.hrsystem.config.CompanyDataSourceProvider.getDataSource("Default");
                }
            }
        }
        return holder.dataSource.getConnection();
    }

    private static class DataSourceHolder {
        private static final DataSourceHolder INSTANCE = new DataSourceHolder();
        private javax.sql.DataSource dataSource;
    }

    private boolean companyExists(Connection conn, String companyId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(1) FROM tbcompanylist WHERE companyId=?")) {
            ps.setString(1, companyId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private void executeUpdate(Connection conn, String sql) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /** 查询单值整数（用于 information_schema 计数） */
    private Integer queryInt(Connection conn, String sql, String param) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    private void use(Connection conn, String dbName) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("USE `" + dbName + "`");
        }
    }

    /** 在当前连接（已 USE 到目标库）上执行多语句脚本。HikariCP 归还连接时会自动重置 catalog */
    private void executeScript(Connection conn, String script) throws Exception {
        try (Statement st = conn.createStatement()) {
            for (String statement : splitStatements(script)) {
                if (!StringUtils.isEmpty(statement.trim())) {
                    st.execute(statement);
                }
            }
        }
    }

    private long insertDefaultDept(Connection conn, String tenantDb, String companyName) throws Exception {
        String deptName = companyName + "总部";
        long deptId = System.currentTimeMillis();
        String sql = "INSERT INTO `" + tenantDb + "`.hrm_dept " +
                "(dept_id, parent_id, dept_type, name, create_time, sortno) VALUES (?,0,0,?,NOW(),1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, deptId);
            ps.setString(2, deptName);
            ps.executeUpdate();
        }
        return deptId;
    }

    private void insertAdminUser(Connection conn, String tenantDb, String account,
                                 String password, String adminName, long deptId) throws Exception {
        // SaaS 安全改造 P0-2：新租户管理员密码直接使用 BCrypt
        String hash = passwordService.encode(password);
        String sql = "INSERT INTO `" + tenantDb + "`.tbloginuser " +
                "(name, account, password, depId, roleId, createtime, canLogin, loginCount, setPassword, pwd_change_required) " +
                "VALUES (?,?,?,?,?,NOW(),1,0,?,1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, adminName);
            ps.setString(2, account);
            ps.setString(3, hash);
            ps.setLong(4, deptId);
            ps.setInt(5, 2); // 角色 2 = 系统管理员（种子数据中 role_id=2 已绑定全部菜单）
            ps.setString(6, hash);
            ps.executeUpdate();
        }
    }

    /**
     * 插入钉钉账号配置到 hrsystem.ddAccount 表
     */
    private void insertDdAccount(Connection conn, String systemDb, String companyId,
                                 String appKey, String appsecret, String agentId) throws Exception {
        String sql = "INSERT INTO `" + systemDb + "`.`ddAccount` " +
                "(companyId, appKey, appsecret, agentId, createTime) " +
                "VALUES (?,?,?,?,NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, companyId);
            ps.setString(2, appKey);
            ps.setString(3, appsecret);
            ps.setString(4, agentId);
            ps.executeUpdate();
        }
    }

    /**
     * B：平台超管重置指定租户管理员密码。
     * - 同一租户管理员累计最多重置 5 次；
     * - 不指定新密码时生成简单临时密码（易转告），并强制其下次登录改密。
     * @return 实际生效的明文密码（临时密码时返回，便于超管转告）
     */
    public String resetTenantAdminPassword(LoginUserInfo operator, String companyId, String newPassword) throws Exception {
        if (companyId == null || !COMPANY_ID_PATTERN.matcher(companyId).matches()) {
            throw new IllegalArgumentException("公司编码必须是 4 位数字");
        }
        String suffix = databasesuffix == null ? "" : databasesuffix;
        String adminAccount = loginUserMapper.getAdminAccountByCompany(companyId, suffix);
        if (adminAccount == null) {
            throw new Exception("租户 " + companyId + " 无系统管理员账号");
        }
        Integer resetCount = loginUserMapper.getAdminResetCount(adminAccount, companyId, suffix);
        if (resetCount != null && resetCount >= 5) {
            throw new Exception("该租户管理员密码重置已达上限(5次)，如需继续请联系平台");
        }
        String pwd = (newPassword != null && !newPassword.isEmpty()) ? newPassword : generateSimpleTempPassword();
        String hash = passwordService.encode(pwd);
        loginUserMapper.adminResetPassword(adminAccount, companyId, suffix, hash);
        // 重置成功后解禁该账号，使其令牌可重新登录（此前若被锁定/封禁则清除封禁）
        tokenRevocation.unbanAccount(adminAccount);
        return pwd;
    }

    /**
     * 删除“仅注册、无数据表”的租户库（清理孤儿租户）。
     * 仅当租户库内 0 张表时才允许删除；有表则拒绝，避免误删业务数据。
     */
    public void deleteEmptyTenant(LoginUserInfo operator, String companyId) throws Exception {
        if (companyId == null || !COMPANY_ID_PATTERN.matcher(companyId).matches()) {
            throw new IllegalArgumentException("公司编码必须是 4 位数字");
        }
        String tenantDb = "hr_" + companyId;
        try (Connection conn = openMainConnection()) {
            Integer tableCount = queryInt(conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?", tenantDb);
            if (tableCount != null && tableCount > 0) {
                throw new Exception("租户库 " + tenantDb + " 仍存在 " + tableCount + " 张表，禁止删除（请先清空数据）");
            }
            Integer dbExists = queryInt(conn,
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?", tenantDb);
            if (dbExists != null && dbExists > 0) {
                executeUpdate(conn, "DROP DATABASE `" + tenantDb + "`");
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tbAllUserList WHERE companyId=?")) {
                ps.setString(1, companyId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tbcompanylist WHERE companyId=?")) {
                ps.setString(1, companyId);
                ps.executeUpdate();
            }
        }
    }

    /**
     * 查看指定租户库的表清单（含视图），用于平台排查/审计。
     */
    public List<java.util.Map<String, Object>> listTenantTables(String companyId) throws Exception {
        if (companyId == null || !COMPANY_ID_PATTERN.matcher(companyId).matches()) {
            throw new IllegalArgumentException("公司编码必须是 4 位数字");
        }
        String tenantDb = "hr_" + companyId;
        List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        try (Connection conn = openMainConnection()) {
            Integer dbExists = queryInt(conn,
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?", tenantDb);
            if (dbExists == null || dbExists == 0) {
                throw new Exception("租户库 " + tenantDb + " 不存在");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT table_name, table_type, engine, table_rows FROM information_schema.tables " +
                    "WHERE table_schema = ? ORDER BY table_type, table_name")) {
                ps.setString(1, tenantDb);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.util.Map<String, Object> m = new java.util.HashMap<>();
                        m.put("tableName", rs.getString("table_name"));
                        m.put("tableType", rs.getString("table_type"));
                        m.put("engine", rs.getString("engine"));
                        m.put("tableRows", rs.getLong("table_rows"));
                        rows.add(m);
                    }
                }
            }
        }
        return rows;
    }

    /** 生成简单、易转告的临时密码（8 位，排除易混字符 0/O/1/l/I） */
    private String generateSimpleTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.util.Random r = new java.util.Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 对比两个租户库的表结构差异（表名、行数、字段数量）
     * @param sourceCompanyId 源租户编码（点击查看表的租户）
     * @param targetCompanyId 目标租户编码（选择对比的租户）
     * @return 对比结果摘要和详细表对比
     */
    public Map<String, Object> compareTenantTables(String sourceCompanyId, String targetCompanyId) throws Exception {
        if (sourceCompanyId == null || !COMPANY_ID_PATTERN.matcher(sourceCompanyId).matches()) {
            throw new IllegalArgumentException("源租户编码必须是 4 位数字");
        }
        if (targetCompanyId == null || !COMPANY_ID_PATTERN.matcher(targetCompanyId).matches()) {
            throw new IllegalArgumentException("目标租户编码必须是 4 位数字");
        }
        if (sourceCompanyId.equals(targetCompanyId)) {
            throw new IllegalArgumentException("不能与自身对比");
        }

        String sourceDb = "hr_" + sourceCompanyId;
        String targetDb = "hr_" + targetCompanyId;
        Map<String, Object> result = new LinkedHashMap<>();

        try (Connection conn = openMainConnection()) {
            // 校验两个库都存在
            Integer sourceExists = queryInt(conn,
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?", sourceDb);
            if (sourceExists == null || sourceExists == 0) {
                throw new Exception("源租户库 " + sourceDb + " 不存在");
            }
            Integer targetExists = queryInt(conn,
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?", targetDb);
            if (targetExists == null || targetExists == 0) {
                throw new Exception("目标租户库 " + targetDb + " 不存在");
            }

            // 获取源租户表信息
            Map<String, Map<String, Object>> sourceTables = getTableInfo(conn, sourceDb);
            // 获取目标租户表信息
            Map<String, Map<String, Object>> targetTables = getTableInfo(conn, targetDb);

            // 计算差异
            Set<String> allTableNames = new java.util.TreeSet<>();
            allTableNames.addAll(sourceTables.keySet());
            allTableNames.addAll(targetTables.keySet());

            List<Map<String, Object>> tableComparison = new ArrayList<>();
            List<String> missingInTarget = new ArrayList<>();
            List<String> extraInTarget = new ArrayList<>();
            int diffCount = 0;

            for (String tableName : allTableNames) {
                Map<String, Object> sourceInfo = sourceTables.get(tableName);
                Map<String, Object> targetInfo = targetTables.get(tableName);
                Map<String, Object> comparison = new LinkedHashMap<>();
                comparison.put("tableName", tableName);

                if (sourceInfo != null && targetInfo != null) {
                    // 两边都有表，对比详情
                    comparison.put("sourceExists", true);
                    comparison.put("targetExists", true);
                    comparison.put("sourceRows", sourceInfo.get("tableRows"));
                    comparison.put("targetRows", targetInfo.get("tableRows"));
                    comparison.put("sourceColumns", sourceInfo.get("columnCount"));
                    comparison.put("targetColumns", targetInfo.get("columnCount"));

                    // 计算行数差异
                    long sourceRows = (Long) sourceInfo.get("tableRows");
                    long targetRows = (Long) targetInfo.get("tableRows");
                    if (sourceRows != targetRows) {
                        long diff = targetRows - sourceRows;
                        double percent = sourceRows > 0 ? (double) diff / sourceRows * 100 : 0;
                        comparison.put("rowDiff", diff + " (" + String.format("%.1f", percent) + "%)");
                        diffCount++;
                    } else {
                        comparison.put("rowDiff", "相同");
                    }

                    // 计算列数差异
                    int sourceCols = (Integer) sourceInfo.get("columnCount");
                    int targetCols = (Integer) targetInfo.get("columnCount");
                    if (sourceCols != targetCols) {
                        comparison.put("columnDiff", Math.abs(targetCols - sourceCols) + "列差异");
                        diffCount++;
                    } else {
                        comparison.put("columnDiff", "相同");
                    }

                    comparison.put("status", "normal");
                } else if (sourceInfo != null) {
                    // 只在源租户存在
                    comparison.put("sourceExists", true);
                    comparison.put("targetExists", false);
                    missingInTarget.add(tableName);
                    comparison.put("sourceRows", sourceInfo.get("tableRows"));
                    comparison.put("targetRows", "-");
                    comparison.put("sourceColumns", sourceInfo.get("columnCount"));
                    comparison.put("targetColumns", "-");
                    comparison.put("rowDiff", "目标缺失");
                    comparison.put("columnDiff", "目标缺失");
                    comparison.put("status", "missing");
                    diffCount++;
                } else {
                    // 只在目标租户存在
                    comparison.put("sourceExists", false);
                    comparison.put("targetExists", true);
                    extraInTarget.add(tableName);
                    comparison.put("sourceRows", "-");
                    comparison.put("targetRows", targetInfo.get("tableRows"));
                    comparison.put("sourceColumns", "-");
                    comparison.put("targetColumns", targetInfo.get("columnCount"));
                    comparison.put("rowDiff", "源缺失");
                    comparison.put("columnDiff", "源缺失");
                    comparison.put("status", "extra");
                    diffCount++;
                }

                tableComparison.add(comparison);
            }

            // 构建摘要
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("sourceTableCount", sourceTables.size());
            summary.put("targetTableCount", targetTables.size());
            summary.put("totalTableCount", allTableNames.size());
            summary.put("missingInTarget", missingInTarget);
            summary.put("extraInTarget", extraInTarget);
            summary.put("diffCount", diffCount);

            result.put("summary", summary);
            result.put("tableComparison", tableComparison);
        }

        return result;
    }

    /**
     * 获取指定租户库的表信息（表名、行数、字段数量）
     */
    private Map<String, Map<String, Object>> getTableInfo(Connection conn, String db) throws Exception {
        Map<String, Map<String, Object>> tables = new LinkedHashMap<>();

        // 获取表的基本信息
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT table_name, table_type, engine, table_rows FROM information_schema.tables " +
                "WHERE table_schema = ? ORDER BY table_type, table_name")) {
            ps.setString(1, db);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> info = new LinkedHashMap<>();
                    String tableName = rs.getString("table_name");
                    info.put("tableType", rs.getString("table_type"));
                    info.put("engine", rs.getString("engine"));
                    info.put("tableRows", rs.getLong("table_rows"));
                    tables.put(tableName, info);
                }
            }
        }

        // 获取每张表的字段数量
        for (Map.Entry<String, Map<String, Object>> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            Map<String, Object> info = entry.getValue();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ?")) {
                ps.setString(1, db);
                ps.setString(2, tableName);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        info.put("columnCount", rs.getInt(1));
                    }
                }
            }
        }

        return tables;
    }

    private void registerAccountInMainDb(Connection conn, String companyId, String account) throws Exception {
        // tbAllUserList 用于登录时由账号反查租户；同账号同租户不重复写
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(1) FROM tballuserlist WHERE companyId=? AND account=?")) {
            check.setString(1, companyId);
            check.setString(2, account);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String sql = "INSERT INTO tballuserlist (companyId, account, createTime) VALUES (?,?,NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, companyId);
            ps.setString(2, account);
            ps.executeUpdate();
        }
    }

    private void insertCompanyRow(Connection conn, String companyId, String companyName, String tenantDb) throws Exception {
        // url 格式须符合 ConnectionParsor.parseSingle 的解析规则：
        // Server=host:port;Database=dbname;User=user;Password=pwd
        String jdbcPart = springDatasourceUrl.substring("jdbc:mysql://".length());
        String hostPort = jdbcPart.substring(0, jdbcPart.indexOf('/'));
        if (!hostPort.contains(":")) {
            hostPort = hostPort + ":3306";
        }
        String url = "Server=" + hostPort + ";Database=" + tenantDb
                + ";User=" + dataSourceUsername + ";Password=" + dataSourcePassword;
        String sql = "INSERT INTO tbcompanylist (companyId, companyName, `database`, url, createTime) VALUES (?,?,?,?,NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, companyId);
            ps.setString(2, companyName);
            ps.setString(3, tenantDb);
            ps.setString(4, url);
            ps.executeUpdate();
        }
    }

    /** 按行拆分 SQL 语句：跳过注释，以 ; 结尾为一个语句（视图定义不含存储过程体，可安全拆分） */
    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        // mysqldump 条件注释块（/*!50001 CREATE VIEW ... */;）首行 /*! 开头、末行 */ 结尾，
        // 中间是裸列定义，必须带状态整体跳过，否则碎片会污染后续语句
        boolean inConditionalBlock = false;
        try (Scanner scanner = new Scanner(script)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String trimmed = line.trim();
                if (inConditionalBlock) {
                    if (trimmed.endsWith("*/") || trimmed.endsWith("*/;")) {
                        inConditionalBlock = false;
                    }
                    continue;
                }
                if (trimmed.startsWith("/*")) {
                    if (!(trimmed.endsWith("*/") || trimmed.endsWith("*/;"))) {
                        inConditionalBlock = true;
                    }
                    continue;
                }
                if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(current.toString());
                    current.setLength(0);
                }
            }
        }
        if (current.length() > 0) {
            statements.add(current.toString());
        }
        return statements;
    }

    static String readClasspathScript(String location) throws Exception {
        ClassPathResource resource = new ClassPathResource(location);
        try (InputStream in = resource.getInputStream()) {
            Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * 插入 tbsettingmenu 子菜单（依赖根菜单 f_id，需动态获取）
     * 根菜单已在 seed-data.sql 中插入，这里只插入子菜单
     */
    private void insertSettingMenuChildren(Connection conn, String tenantDb) throws Exception {
        // 查询根菜单 f_id，按 sn 排序（考勤设置=1, 薪资设置=3, 钉钉数据=6, 个税相关=8, 奖金=12, 社保设置=14, 员工/部门=16）
        Map<String, Integer> rootMenuIds = new HashMap<>();
        String queryRoot = "SELECT f_id, sn FROM `" + tenantDb + "`.tbsettingmenu WHERE p_id = 0";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(queryRoot)) {
            while (rs.next()) {
                rootMenuIds.put(rs.getString("sn"), rs.getInt("f_id"));
            }
        }

        if (rootMenuIds.isEmpty()) {
            log.warn("【租户开通】tbsettingmenu 根菜单为空，跳过子菜单插入");
            return;
        }

        // 定义子菜单：parentSn -> (sn, title, url, pageName)
        List<Object[]> children = new ArrayList<>();
        // 考勤设置 子菜单
        if (rootMenuIds.containsKey("1")) {
            int pid = rootMenuIds.get("1");
            children.add(new Object[]{pid, "2", "打卡异常", "/attendance/index", "attendance"});
            children.add(new Object[]{pid, "5", "上传考勤", "/produceAttendance/index", "produceAttendance"});
        }
        // 薪资设置 子菜单
        if (rootMenuIds.containsKey("3")) {
            int pid = rootMenuIds.get("3");
            children.add(new Object[]{pid, "4", "上传定薪/调薪", "/salaryFixing/index", "salaryFixing"});
        }
        // 钉钉数据 子菜单
        if (rootMenuIds.containsKey("6")) {
            int pid = rootMenuIds.get("6");
            children.add(new Object[]{pid, "7", "钉钉数据差异", "/salaryMonthRecord/index", "salaryMonthRecord"});
        }
        // 个税相关 子菜单
        if (rootMenuIds.containsKey("8")) {
            int pid = rootMenuIds.get("8");
            children.add(new Object[]{pid, "9", "个税累计", "/personalIncomeTax/index", "personalIncomeTax"});
            children.add(new Object[]{pid, "10", "附加扣除累计", "/additional/index", "additional"});
            children.add(new Object[]{pid, "11", "附加扣除值", "/employeeAdditional/index", "additional"});
        }
        // 奖金 子菜单
        if (rootMenuIds.containsKey("12")) {
            int pid = rootMenuIds.get("12");
            children.add(new Object[]{pid, "13", "上传奖金", "/bonus/index", "bonus"});
        }
        // 社保设置 子菜单
        if (rootMenuIds.containsKey("14")) {
            int pid = rootMenuIds.get("14");
            children.add(new Object[]{pid, "15", "上传社保方案", "", "insuranceScheme"});
        }
        // 员工/部门 子菜单
        if (rootMenuIds.containsKey("16")) {
            int pid = rootMenuIds.get("16");
            children.add(new Object[]{pid, "17", "员工设置", "/employee/index", "employee"});
            children.add(new Object[]{pid, "18", "部门配置", "/dept/index", "dept"});
        }

        // 批量插入子菜单
        String insertSql = "INSERT INTO `" + tenantDb + "`.tbsettingmenu (p_id, sn, title, url, icon, short_cut, can_use, page_name) VALUES (?,?,?,?,NULL,0,1,?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Object[] row : children) {
                ps.setInt(1, (int) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.setString(5, (String) row[4]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        log.info("【租户开通】插入 tbsettingmenu 子菜单 {} 条", children.size());
    }
}
