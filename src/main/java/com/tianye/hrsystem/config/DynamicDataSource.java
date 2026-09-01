package com.tianye.hrsystem.config;

import com.tianye.hrsystem.model.LoginUserInfo;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * SaaS 改造（数据源治理 #2）：租户数据源创建逻辑统一收敛到
 * CompanyDataSourceProvider（懒加载 + 静态缓存），本类不再自带一套
 * ConnectionParsor 解析/建池代码，消除双路径不一致的隐患。
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {
    Map<Object, Object> OX = null;

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        LoginUserInfo info = CompanyContext.get();
        String Key = "";
        if (info != null) Key = info.getCompanyId();
        else Key = "Default";

        // 检查已有数据源是否已被 evictor 关闭，如果是则移除并重建
        Object existing = OX.get(Key);
        if (existing instanceof HikariDataSource && ((HikariDataSource) existing).isClosed()) {
            log.info("【数据源路由】租户 {} 的连接池已被驱逐关闭，重新获取", Key);
            OX.remove(Key);
        }

        if (OX.containsKey(Key) == false) {
            if ("Default".equals(Key)) {
                log.warn("【数据源路由】Default 数据源缺失");
                return Key;
            }
            // 统一走 Provider 懒加载（内部含缓存与连接池配置）
            DataSource tenantSource = CompanyDataSourceProvider.getDataSource(Key);
            if (tenantSource == null) {
                log.error("【数据源路由】租户 {} 的数据源不存在（未开通或已停用）", Key);
                throw new IllegalStateException("租户数据源不存在: hr_" + Key);
            }
            OX.put(Key, tenantSource);
            super.setTargetDataSources(OX);
            super.afterPropertiesSet();
        }
        return Key;
    }

    public void setDataSource(Map<Object, Object> dataSources) {
        OX = dataSources;
        super.setTargetDataSources(dataSources);
    }
}
