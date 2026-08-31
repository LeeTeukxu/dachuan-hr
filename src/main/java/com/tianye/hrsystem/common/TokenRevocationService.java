package com.tianye.hrsystem.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 登录令牌吊销服务：基于 Redis 实现「退出即失效 / 禁用立即生效」。
 * - JTI 黑名单：退出登录时把令牌唯一标识 jti 写入，令牌在其剩余生命周期内作废。
 * - 账号封禁：管理员禁用/锁定账号时封禁该账号，其所有在途令牌立即失效。
 */
@Component
public class TokenRevocationService {

    private static final String BL_PREFIX = "hr:jwt:bl:";
    private static final String BAN_PREFIX = "hr:jwt:ban:";
    private static final String SEED_PREFIX = "hr:jwt:seed:";

    @Value("${hrm.jwt.expire-hours:8}")
    private int expireHours;

    @Resource
    private Redis redis;

    /** 吊销指定 jti 的令牌（退出登录时调用） */
    public void revokeToken(String jti) {
        if (jti == null) {
            return;
        }
        redis.setex(BL_PREFIX + jti, expireHours * 3600, "1");
    }

    /** 该 jti 是否已被吊销 */
    public boolean isTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return redis.exists(BL_PREFIX + jti);
    }

    /** 封禁账号：使其所有在途令牌失效，直至解禁 */
    public void banAccount(String account) {
        if (account == null) {
            return;
        }
        // 封禁时长取较长值，确保禁用态下令牌不会因 TTL 过期而“复活”
        redis.setex(BAN_PREFIX + account, 60 * 24 * 3600, "1");
    }

    /** 解禁账号 */
    public void unbanAccount(String account) {
        if (account == null) {
            return;
        }
        redis.del(BAN_PREFIX + account);
    }

    /** 账号是否处于封禁态 */
    public boolean isAccountBanned(String account) {
        if (account == null) {
            return false;
        }
        return redis.exists(BAN_PREFIX + account);
    }

    /**
     * 使账号所有在途令牌失效（强制下线），但允许凭新密码重新登录。
     * 与 banAccount（账号锁定，需超管解禁）区分：种子自增后，旧令牌种子与最新种子不一致即作废，
     * 新登录会签发携带最新种子的令牌，因此不会被“自己踢自己”挡在门外。
     * @return 自增后的新种子值
     */
    public long bumpSessionSeed(String account) {
        if (account == null) {
            return 0;
        }
        String key = SEED_PREFIX + account;
        Long next = redis.incr(key);
        if (next != null && next == 1) {
            redis.expire(key, expireHours * 3600);
        }
        return next == null ? 0 : next;
    }

    /** 获取账号当前会话种子（令牌签发时写入，校验时比对；无则返回 0） */
    public long getSessionSeed(String account) {
        if (account == null) {
            return 0;
        }
        String val = redis.get(SEED_PREFIX + account);
        if (val == null) {
            return 0;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
