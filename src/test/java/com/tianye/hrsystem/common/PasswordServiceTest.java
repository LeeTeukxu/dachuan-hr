package com.tianye.hrsystem.common;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * SaaS 改造 P1-4：密码服务单元测试
 * 覆盖 BCrypt 新哈希、历史双重 MD5 兼容、透明升级判断
 */
public class PasswordServiceTest {

    private PasswordService passwordService;

    @Before
    public void setUp() {
        passwordService = new PasswordService();
    }

    @Test
    public void encode_生成BCrypt格式() {
        String hash = passwordService.encode("admin123");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
        // 加盐：同一明文两次编码结果不同
        assertNotEquals(hash, passwordService.encode("admin123"));
    }

    @Test
    public void matches_BCrypt哈希校验() {
        String hash = passwordService.encode("admin123");
        assertTrue(passwordService.matches("admin123", hash));
        assertFalse(passwordService.matches("wrong", hash));
    }

    @Test
    public void matches_兼容历史双重MD5() {
        // 与 MD5Utils.enCode 一致的双重 MD5
        String legacy = MD5Utils.enCode("legacy123");
        assertTrue(passwordService.matches("legacy123", legacy));
        assertFalse(passwordService.matches("other", legacy));
    }

    @Test
    public void matches_空值安全() {
        assertFalse(passwordService.matches(null, "$2a$10$x"));
        assertFalse(passwordService.matches("abc", null));
        assertFalse(passwordService.matches("", ""));
    }

    @Test
    public void needsUpgrade_旧MD5需升级_BCrypt不需() {
        assertTrue(passwordService.needsUpgrade(MD5Utils.enCode("old")));
        assertFalse(passwordService.needsUpgrade(passwordService.encode("new")));
        assertFalse(passwordService.needsUpgrade(null));
    }

    @Test
    public void 透明升级闭环_升级后新哈希可验证() {
        String legacyHash = MD5Utils.enCode("mypassword");
        assertTrue(passwordService.matches("mypassword", legacyHash)); // 旧哈希可登录
        String newHash = passwordService.upgradeStoredHash("mypassword");   // 登录后升级
        assertTrue(newHash.startsWith("$2"));
        assertTrue(passwordService.matches("mypassword", newHash));    // 新哈希仍可登录
        assertFalse(passwordService.needsUpgrade(newHash));            // 不再需要升级
    }
}
