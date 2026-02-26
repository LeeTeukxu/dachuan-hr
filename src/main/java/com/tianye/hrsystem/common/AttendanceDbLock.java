package com.tianye.hrsystem.common;

/**
 * 考勤数据库操作全局锁
 * 用于序列化所有考勤相关的数据库写操作，避免InnoDB索引锁冲突
 * 
 * 注意：API调用仍然是并行的，只有数据库写入需要串行化
 * 数据库写操作执行很快（毫秒级），不会影响整体性能
 */
public class AttendanceDbLock {
    
    /**
     * 全局锁对象
     * 所有考勤相关的数据库写操作都应该使用这个锁
     */
    public static final Object LOCK = new Object();
}
