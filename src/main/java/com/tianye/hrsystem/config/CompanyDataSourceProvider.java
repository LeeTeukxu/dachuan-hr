package com.tianye.hrsystem.config;

import com.zaxxer.hikari.HikariDataSource;
import com.tianye.hrsystem.model.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 多租户数据源提供器。
 *
 * <p>风险修复（内存溢出 / 数据库连接未释放）：早期实现用静态 {@code HashMap<String, DataSource>}
 * 永久缓存每个租户的连接池，既不设上限、也不驱逐、也不关闭，并发 put 还可能损坏 Map。
 * 随租户（或请求）增多，连接池数量与 MySQL 连接数、堆内存无上限增长，最终打满
 * {@code max_connections} 或 OOM。</p>
 *
 * <p>现改为有界、可驱逐的缓存：
 * <ul>
 *   <li>每租户池上限 {@link #MAX_TENANT_POOLS}，超过后按最近访问时间(LRU)驱逐并关闭底层 Hikari 池；</li>
 *   <li>空闲超过 {@link #IDLE_EVICT_MILLIS} 的租户池定时驱逐并关闭；</li>
 *   <li>默认数据源只构建一次，永不过期、不参与驱逐；</li>
 *   <li>进程关闭时({@link #shutdown()})统一关闭所有池，归还连接。</li>
 * </ul>
 */
@Component
public class CompanyDataSourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(CompanyDataSourceProvider.class);

    private static final int MAX_TENANT_POOLS = 500;
    private static final long IDLE_EVICT_MILLIS = 30 * 60 * 1000L;
    private static final long EVICTOR_PERIOD_MILLIS = 5 * 60 * 1000L;

    private static final ConcurrentHashMap<String, CacheEntry> tenantPoolCache = new ConcurrentHashMap<>();
    /** 正在执行后台任务的租户集合，evictor 跳过这些租户不驱逐 */
    private static final Set<String> activeTenants = ConcurrentHashMap.newKeySet();
    private static volatile DataSource defaultDataSource;

    private static final ScheduledExecutorService evictor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "tenant-datasource-evictor");
                t.setDaemon(true);
                return t;
            });

    private static final class CacheEntry {
        final HikariDataSource dataSource;
        volatile long lastAccessMillis;

        CacheEntry(HikariDataSource dataSource) {
            this.dataSource = dataSource;
            this.lastAccessMillis = System.currentTimeMillis();
        }
    }

    @PostConstruct
    public void startEvictor() {
        evictor.scheduleWithFixedDelay(CompanyDataSourceProvider::evictIdlePools,
                EVICTOR_PERIOD_MILLIS, EVICTOR_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        evictor.shutdownNow();
        for (CacheEntry entry : tenantPoolCache.values()) {
            closeQuietly(entry.dataSource);
        }
        tenantPoolCache.clear();
        closeQuietly(defaultDataSource);
        defaultDataSource = null;
    }

    public static DataSource getDataSource(String companyId) {
        if (companyId == null || "Default".equals(companyId)) {
            return getDefaultDataSource();
        }
        return getTenantDataSource(companyId);
    }

    private static DataSource getDefaultDataSource() {
        DataSource ds = defaultDataSource;
        if (ds != null) {
            return ds;
        }
        synchronized (CompanyDataSourceProvider.class) {
            if (defaultDataSource != null) {
                return defaultDataSource;
            }
            DataSource built = new ConnectionParsor().getDefaultConnection();
            if (built == null) {
                throw new IllegalStateException("主数据源初始化失败，无法解析租户连接信息");
            }
            defaultDataSource = built;
            return defaultDataSource;
        }
    }

    private static DataSource getTenantDataSource(String companyId) {
        CacheEntry entry = tenantPoolCache.get(companyId);
        if (entry != null) {
            entry.lastAccessMillis = System.currentTimeMillis();
            return entry.dataSource;
        }
        synchronized (CompanyDataSourceProvider.class) {
            entry = tenantPoolCache.get(companyId);
            if (entry != null) {
                entry.lastAccessMillis = System.currentTimeMillis();
                return entry.dataSource;
            }
            HikariDataSource built = buildTenantPool(companyId);
            tenantPoolCache.put(companyId, new CacheEntry(built));
            return built;
        }
    }

    private static HikariDataSource buildTenantPool(String companyId) {
        DataSource defaultSource = getDefaultDataSource();
        ConnectionInfo targetInfo;
        try (Connection conn = defaultSource.getConnection()) {
            ConnectionParsor connectionParsor = new ConnectionParsor();
            connectionParsor.setConnection(conn);
            targetInfo = connectionParsor.getByID(companyId);
        } catch (Exception e) {
            logger.error("解析租户连接信息失败: {}", e.getMessage());
            throw new IllegalStateException("解析租户 " + companyId + " 连接信息失败", e);
        }
        if (targetInfo == null) {
            String reason = String.format(
                    "主库 tbcompanylist 中未找到企业 %s 的数据库连接配置（companyId 与 url 中库名解析出的企业编码不匹配）。"
                            + "请核对 tbAllUserList.CompanyID 与 tbcompanylist.url 的库名是否一致。",
                    companyId);
            logger.error(reason);
            throw new IllegalStateException(reason);
        }
        String url = ConnectionParsor.buildMysqlJdbcUrl(targetInfo.getServer(), targetInfo.getPort(), targetInfo.getDataBase());
        logger.info("为租户 {} 建立数据源(URL方式) -> Server={}:{}, Database={}, User={}",
                companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getDataBase(), targetInfo.getUsername());
        HikariDataSource dd = buildPool(url, targetInfo.getUsername(), targetInfo.getPassword(), "hikari-" + companyId);
        try (Connection testConn = dd.getConnection()) {
            logger.info("租户 {} 数据源连接成功(URL方式) -> Server={}:{}, Database={}",
                    companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getDataBase());
            return dd;
        } catch (Exception e) {
            logger.warn("租户 {} 使用 tbcompanylist.url 建连失败（Server={}:{}, User={}），回退到主库同源连接: {}",
                    companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getUsername(), e.getMessage());
            closeQuietly(dd);
            HikariDataSource master = (HikariDataSource) defaultSource;
            String masterUrl = master.getJdbcUrl();
            String hostPort = masterUrl.substring("jdbc:mysql://".length(), masterUrl.indexOf('/', "jdbc:mysql://".length()));
            String[] hp = hostPort.split(":");
            String host = hp[0];
            String port = hp.length > 1 ? hp[1] : "3306";
            String fallbackUrl = ConnectionParsor.buildMysqlJdbcUrl(host, port, targetInfo.getDataBase());
            HikariDataSource fb = buildPool(fallbackUrl, master.getUsername(), master.getPassword(), "hikari-" + companyId + "-fb");
            try (Connection testConn = fb.getConnection()) {
                logger.info("租户 {} 数据源连接成功(主库同源回退) -> {}", companyId, fallbackUrl);
                return fb;
            } catch (Exception e2) {
                String reason = String.format(
                        "租户 %s 无法连接其数据库。已尝试：①tbcompanylist.url(Server=%s:%s,User=%s) ②主库同源(Server=%s)。"
                                + "请检查该企业的数据库是否可访问、tbcompanylist.url 凭据是否正确。",
                        companyId, targetInfo.getServer(), targetInfo.getPort(), targetInfo.getUsername(), hostPort);
                logger.error(reason, e2);
                closeQuietly(fb);
                throw new IllegalStateException(reason);
            }
        }
    }

    private static HikariDataSource buildPool(String url, String username, String password, String poolName) {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(url);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
        HikariDataSource dd = (HikariDataSource) dataSourceBuilder.build();
        DataSourcePoolConfigurator.apply(dd, poolName);
        return dd;
    }

    private static void evictIdlePools() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, CacheEntry> e : tenantPoolCache.entrySet()) {
                // 跳过正在执行后台任务的活跃租户
                if (activeTenants.contains(e.getKey())) {
                    continue;
                }
                if (now - e.getValue().lastAccessMillis > IDLE_EVICT_MILLIS) {
                    CacheEntry removed = tenantPoolCache.remove(e.getKey());
                    if (removed != null) {
                        closeQuietly(removed.dataSource);
                        logger.info("驱逐空闲租户数据源(已关闭连接池): {}", e.getKey());
                    }
                }
            }
            while (tenantPoolCache.size() > MAX_TENANT_POOLS) {
                String oldestKey = null;
                long oldestAccess = Long.MAX_VALUE;
                for (Map.Entry<String, CacheEntry> e : tenantPoolCache.entrySet()) {
                    if (e.getValue().lastAccessMillis < oldestAccess) {
                        oldestAccess = e.getValue().lastAccessMillis;
                        oldestKey = e.getKey();
                    }
                }
                if (oldestKey == null) {
                    break;
                }
                CacheEntry removed = tenantPoolCache.remove(oldestKey);
                if (removed != null) {
                    closeQuietly(removed.dataSource);
                    logger.info("租户数据源超过上限({})，驱逐最久未用: {}", MAX_TENANT_POOLS, oldestKey);
                }
            }
        } catch (Exception ex) {
            logger.warn("租户数据源驱逐任务异常: {}", ex.getMessage());
        }
    }

    private static void closeQuietly(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            try {
                ((HikariDataSource) dataSource).close();
            } catch (Exception ignore) {
                logger.warn("关闭数据源失败: {}", ignore.getMessage());
            }
        }
    }

    /** 标记租户为活跃（后台任务运行期间），evictor 将跳过该租户 */
    public static void markActive(String companyId) {
        if (companyId != null) {
            activeTenants.add(companyId);
        }
    }

    /** 取消租户活跃标记（后台任务完成），允许 evictor 正常驱逐 */
    public static void markInactive(String companyId) {
        if (companyId != null) {
            activeTenants.remove(companyId);
        }
    }
}
