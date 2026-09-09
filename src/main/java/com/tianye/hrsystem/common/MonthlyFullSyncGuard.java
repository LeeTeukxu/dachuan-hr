package com.tianye.hrsystem.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 「每月一次」全量获取/同步的 Redis 月锁。
 *
 * <p>业务规则（用户定）：每个租户按自然月，审批全量获取与考勤全量同步各自最多成功 1 次。
 * 某自然月已经成功跑过一次覆盖全部员工的获取/同步后，本月内用户再发起覆盖全部员工的请求，
 * 一律拦截，只能改为定向勾选少数员工补拉，以控制钉钉 API 月配额消耗。</p>
 *
 * <p>实现：写一个带 TTL 的 Redis key（value=成功时间），TTL 到下一个自然月 1 号 0 点自动过期，
 * 因此无需每月清理逻辑，跨月自动恢复可再次全量。</p>
 *
 * <p>key 规则：fullsync:{biz}:{companyId}:{yyyyMM}。业务类型 biz：approval=审批全量获取、attendance=考勤全量同步。</p>
 */
@Component
public class MonthlyFullSyncGuard {

    /** 审批数据全量获取 */
    public static final String BIZ_APPROVAL = "approval";
    /** 考勤全量同步 */
    public static final String BIZ_ATTENDANCE = "attendance";

    private static final String KEY_PREFIX = "fullsync";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private Redis redis;

    /** 构造 key：fullsync:{biz}:{companyId}:{yyyyMM} */
    public String key(String biz, String companyId) {
        String cid = companyId == null ? "unknown" : companyId;
        return KEY_PREFIX + ":" + biz + ":" + cid + ":" + YearMonth.now().format(MONTH);
    }

    /** 该公司（自然月内）是否已完成一次该业务的全量获取/同步 */
    public boolean isMonthlyFullSynced(String biz, String companyId) {
        try {
            return redis.exists(key(biz, companyId));
        } catch (Exception e) {
            return false; // Redis 异常不阻断正常获取
        }
    }

    /** 标记该公司本自然月已完成一次该业务的全量获取/同步，TTL 到下一自然月 1 号 0 点自动过期 */
    public void markFullSynced(String biz, String companyId) {
        try {
            redis.setex(key(biz, companyId), secondsUntilNextMonth(), String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            // 打标失败不阻断业务，仅影响防重全量
        }
    }

    /** 距下一自然月 1 号 0 点的秒数（TTL）；保证为正（若恰为月初则兜底 1 天） */
    private int secondsUntilNextMonth() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth current = YearMonth.from(now);
        YearMonth next = current.plusMonths(1);
        LocalDateTime nextMonthStart = next.atDay(1).atStartOfDay();
        long seconds = Math.max(Duration.between(now, nextMonthStart).getSeconds(), 86400L);
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }
}
