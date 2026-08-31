package com.tianye.hrsystem.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * SaaS 安全改造 P0-2：统一密码服务
 * - 新密码一律 BCrypt 加盐哈希
 * - 存量双重 MD5 密码在登录验证通过后透明升级为 BCrypt（无需用户重置密码）
 */
@Slf4j
@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    /** BCrypt 编码 */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文密码与库中哈希是否匹配，支持两种格式：
     * 1. BCrypt（$2a$/$2b$/$2y$ 开头）—— 直接校验
     * 2. 历史遗留的双重 MD5 —— 兼容校验
     * @return 匹配返回 true；不匹配或哈希为空返回 false
     */
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || rawPassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            try {
                return encoder.matches(rawPassword, storedHash);
            } catch (IllegalArgumentException e) {
                log.warn("BCrypt 校验异常（哈希格式损坏）: {}", e.getMessage());
                return false;
            }
        }
        // 历史遗留：双重 MD5
        return MD5Utils.enCode(rawPassword).equals(storedHash);
    }

    /**
     * 判断库中哈希是否需要升级为 BCrypt
     * @return true = 仍是旧版双重 MD5，登录成功后应调用 upgradeStoredHash
     */
    public boolean needsUpgrade(String storedHash) {
        return storedHash != null && !isBcrypt(storedHash);
    }

    /**
     * 登录成功后的透明升级：将旧 MD5 哈希替换为 BCrypt
     * @return BCrypt 新哈希
     */
    public String upgradeStoredHash(String rawPassword) {
        return encode(rawPassword);
    }

    private boolean isBcrypt(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
