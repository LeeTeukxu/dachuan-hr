package com.tianye.hrsystem.common;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 考勤数据库操作锁（按公司隔离）
 * 用于序列化同一公司的考勤数据库写操作，避免InnoDB索引锁冲突
 *
 * 注意：API调用仍然是并行的，只有数据库写入需要串行化；
 * 锁粒度为公司，不同租户之间的考勤写入不再互相阻塞
 */
public class AttendanceDbLock {

    /** 公司上下文缺失时的兜底全局锁 */
    private static final Object GLOBAL_FALLBACK_LOCK = new Object();

    private static final ConcurrentHashMap<String, Object> COMPANY_LOCKS = new ConcurrentHashMap<>();

    /**
     * 获取指定公司的考勤写库锁（键为 companyId，未知公司退化为全局锁）
     */
    public static Object lockFor(String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return GLOBAL_FALLBACK_LOCK;
        }
        return COMPANY_LOCKS.computeIfAbsent(companyId, key -> new Object());
    }
}
