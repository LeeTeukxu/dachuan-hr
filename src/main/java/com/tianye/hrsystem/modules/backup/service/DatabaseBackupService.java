package com.tianye.hrsystem.modules.backup.service;

import com.tianye.hrsystem.config.ConnectionParsor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 数据库自动备份 + 还原
 * 通过 mysqldump 备份主库(hrsystem)与所有 hr_ 前缀租户库，
 * 备份记录写入主库 tb_backup_record，支持手动/定时备份、按日期还原、超期清理。
 */
@Slf4j
@Service
public class DatabaseBackupService {

    @Value("${hr.backup.db.host:127.0.0.1}")
    private String dbHost;

    @Value("${hr.backup.db.port:3306}")
    private String dbPort;

    @Value("${hr.backup.db.username:root}")
    private String dbUsername;

    @Value("${hr.backup.db.password:}")
    private String dbPassword;

    @Value("${hr.backup.dir:./backup}")
    private String backupDir;

    /** 还原快照保留期：24 小时 */
    private static final long RESTORE_SNAPSHOT_RETENTION_MILLIS = 24L * 60L * 60L * 1000L;

    @Value("${hr.backup.retention-count:15}")
    private int retentionCount;

    @Value("${hr.backup.mysqldump.path:mysqldump}")
    private String mysqldumpPath;

    @Value("${hrm.system.database:hrsystem}")
    private String systemDatabase;

    @Autowired
    private ConnectionParsor connectionParsor;

    /** mysql 客户端路径（还原用），与 mysqldump 同目录；Windows 下自动补 .exe 后缀 */
    private String getMysqlPath() {
        File f = new File(mysqldumpPath);
        String dir = f.getParent();
        if (dir == null) {
            return resolveExePath("mysql");
        }
        return new File(dir, resolveExePath("mysql")).getPath();
    }

    /** 解析带 .exe 后缀的可执行文件名（仅 Windows 且文件名无扩展名时补全） */
    private String resolveExePath(String cmd) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (windows && !cmd.contains(".")) {
            return cmd + ".exe";
        }
        return cmd;
    }

    /** 解析 mysqldump 可执行文件路径（Windows 下自动补 .exe 后缀） */
    private String resolveMysqldumpPath() {
        String p = mysqldumpPath;
        if (p == null) return "mysqldump";
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (windows && !p.contains(".") && !p.contains(File.separator) && !p.contains("/")) {
            return p + ".exe";
        }
        return p;
    }

    private String jdbcUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + systemDatabase
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(jdbcUrl(), dbUsername, dbPassword);
    }

    /**
     * 发现所有需要备份的库：主库 + 所有 hr_ 前缀租户库
     */
    public List<String> discoverDatabases() {
        List<String> dbs = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (name.equalsIgnoreCase(systemDatabase) || name.toLowerCase().startsWith("hr_")) {
                    dbs.add(name);
                }
            }
        } catch (Exception e) {
            log.error("发现数据库列表失败", e);
        }
        return dbs;
    }

    /**
     * 执行一次完整备份：主库 + 所有租户库，逐个 mysqldump
     * @return 成功备份的库数量
     */
    public int createBackup() {
        List<String> dbs = discoverDatabases();
        if (dbs.isEmpty()) {
            throw new RuntimeException("未发现任何可备份的数据库");
        }
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File dir = new File(backupDir, ts);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建备份目录失败: " + dir.getAbsolutePath());
        }
        log.info("开始数据库备份，共 {} 个库，目录: {}", dbs.size(), dir.getAbsolutePath());
        int success = 0;
        int fail = 0;
        for (String db : dbs) {
            String filePath = new File(dir, db + "_" + ts + ".sql").getAbsolutePath();
            String msg = "";
            String status = "SUCCESS";
            try {
                long size = dumpDatabase(db, filePath);
                log.info("备份库 {} 成功, 大小 {} 字节", db, size);
                success++;
            } catch (Exception e) {
                fail++;
                status = "FAILED";
                msg = e.getMessage() == null ? "备份失败" : e.getMessage();
                log.error("备份库 {} 失败", db, e);
            }
            try {
                insertRecord(db, filePath, 0, status, msg);
            } catch (Exception e) {
                log.error("写入备份记录失败, db={}", db, e);
            }
        }
        log.info("备份完成: 成功={}, 失败={}", success, fail);
        if (success == 0 && fail > 0) {
            throw new RuntimeException("所有数据库备份失败");
        }
        return success;
    }

    /**
     * mysqldump 单个库，返回文件字节数
     * 若 mysqldump 不支持 --column-statistics 参数，自动去掉该参数重试一次
     */
    private long dumpDatabase(String db, String filePath) throws Exception {
        try {
            return dumpDatabase0(db, filePath, true);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("column-statistics") || msg.contains("column statistics")) {
                log.warn("mysqldump 不支持 --column-statistics，去掉该参数重试: {}", db);
                return dumpDatabase0(db, filePath, false);
            }
            throw e;
        }
    }

    private long dumpDatabase0(String db, String filePath, boolean columnStatistics) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(resolveMysqldumpPath());
        cmd.add("-h"); cmd.add(dbHost);
        cmd.add("-P"); cmd.add(dbPort);
        cmd.add("-u"); cmd.add(dbUsername);
        cmd.add("-p" + dbPassword);
        cmd.add("--routines"); cmd.add("--triggers"); cmd.add("--events");
        cmd.add("--single-transaction"); cmd.add("--set-gtid-purged=OFF");
        cmd.add("--default-character-set=utf8mb4");
        if (columnStatistics) {
            cmd.add("--column-statistics=0");
        }
        cmd.add(db);
        cmd.add("-r"); cmd.add(filePath);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder err = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                err.append(line).append("\n");
            }
        }
        if (!process.waitFor(10, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("备份超时: " + db);
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("mysqldump 失败: " + err.toString().trim());
        }
        File f = new File(filePath);
        if (!f.exists() || f.length() == 0) {
            throw new RuntimeException("备份文件为空: " + filePath);
        }
        return f.length();
    }

    private void insertRecord(String db, String filePath, long size, String status, String message) throws Exception {
        String sql = "INSERT INTO tb_backup_record (id, backup_time, file_path, file_size, database_name, status, message) " +
                "VALUES (?, NOW(), ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis() + (long) (Math.random() * 10000));
            ps.setString(2, filePath);
            ps.setLong(3, size);
            ps.setString(4, db);
            ps.setString(5, status);
            ps.setString(6, message);
            ps.executeUpdate();
        }
    }

    /**
     * 查询备份记录（按时间倒序）
     */
    public List<Map<String, Object>> listRecords() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, date_format(backup_time,'%Y-%m-%d %H:%i:%s') AS backupTime, " +
                "file_path AS filePath, file_size AS fileSize, database_name AS dbName, status, message " +
                "FROM tb_backup_record ORDER BY backup_time DESC, id DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("backupTime", rs.getString("backupTime"));
                row.put("filePath", rs.getString("filePath"));
                row.put("fileSize", rs.getLong("fileSize"));
                row.put("dbName", rs.getString("dbName"));
                row.put("status", rs.getString("status"));
                row.put("message", rs.getString("message"));
                list.add(row);
            }
        } catch (Exception e) {
            log.error("查询备份记录失败", e);
        }
        return list;
    }

    /**
     * 还原指定备份记录：先将该库的 .sql 文件导入到对应数据库
     * 还原前自动做一次当前状态快照，保证可回滚
     */
    public String restoreBackup(long recordId) throws Exception {
        Map<String, Object> record = getRecord(recordId);
        if (record == null) {
            throw new RuntimeException("备份记录不存在: " + recordId);
        }
        String filePath = (String) record.get("filePath");
        String db = (String) record.get("dbName");
        File f = new File(filePath);
        if (!f.exists()) {
            throw new RuntimeException("备份文件不存在: " + filePath);
        }
        // 还原前快照，防止误还原丢失当前数据
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String snapshotDir = new File(backupDir, "restore-snapshot_" + ts).getAbsolutePath();
        try {
            dumpDatabase(db, new File(snapshotDir, db + ".sql").getAbsolutePath());
            log.info("还原前快照完成: {}", snapshotDir);
        } catch (Exception e) {
            log.warn("还原前快照失败（继续还原）: {}", e.getMessage());
        }
        // 导入
        ProcessBuilder pb = new ProcessBuilder(
                getMysqlPath(),
                "-h", dbHost, "-P", dbPort, "-u", dbUsername,
                "-p" + dbPassword,
                "--default-character-set=utf8mb4",
                db
        );
        pb.redirectInput(ProcessBuilder.Redirect.from(f));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
            }
        }
        if (!process.waitFor(15, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("还原超时: " + db);
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("还原失败: " + out.toString().trim());
        }
        log.info("库 {} 从 {} 还原成功", db, filePath);
        return "数据库 " + db + " 还原成功";
    }

    private Map<String, Object> getRecord(long recordId) {
        String sql = "SELECT id, file_path AS filePath, database_name AS dbName FROM tb_backup_record WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("filePath", rs.getString("filePath"));
                    row.put("dbName", rs.getString("dbName"));
                    return row;
                }
            }
        } catch (Exception e) {
            log.error("查询备份记录失败", e);
        }
        return null;
    }

     /**
      * 清理备份：备份记录与文件始终只保留最新 N 条（默认15条），
      * 删除最旧的超出部分对应的文件+记录。
      * @return 清理的条数
      */
    public int cleanupExpired() {
        int snapshotRemoved = cleanupRestoreSnapshots();
        int removed = 0;
        List<Long> ids = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 找出所有需删除的（id 不属于最新 N 条）
            String sql = "SELECT t.id, t.file_path FROM tb_backup_record t " +
                    "WHERE t.id NOT IN (SELECT id FROM (SELECT id FROM tb_backup_record " +
                    "ORDER BY backup_time DESC, id DESC LIMIT " + retentionCount + ") keep)";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String fp = rs.getString("file_path");
                    ids.add(id);
                    File f = new File(fp);
                    if (f.exists()) {
                        boolean del = f.delete();
                        log.info("删除超量备份文件: {} => {}", fp, del);
                    }
                }
            }
            if (!ids.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tb_backup_record WHERE id=?")) {
                    for (long id : ids) {
                        ps.setLong(1, id);
                        ps.addBatch();
                        removed++;
                    }
                    ps.executeBatch();
                }
            }
        } catch (Exception e) {
            log.error("清理超量备份失败", e);
        }
        log.info("清理备份完成，共清理 {} 条，保留最近 {} 条，过期还原快照 {} 个", removed, retentionCount, snapshotRemoved);
        return removed;
    }

    /** 还原前快照目录只保留 24 小时，超期自动清理，避免备份目录无限膨胀 */
    private int cleanupRestoreSnapshots() {
        File dir = new File(backupDir);
        File[] snapshots = dir.listFiles((d, name) -> name.startsWith("restore-snapshot_"));
        if (snapshots == null) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - RESTORE_SNAPSHOT_RETENTION_MILLIS;
        int removed = 0;
        for (File snapshot : snapshots) {
            if (snapshot.lastModified() >= cutoff) {
                continue;
            }
            if (deleteRecursively(snapshot)) {
                removed++;
                log.info("已清理过期还原快照: {}", snapshot.getName());
            }
        }
        return removed;
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }
}
