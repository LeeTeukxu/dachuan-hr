package com.tianye.hrsystem.util;


import com.tianye.hrsystem.common.Redis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author zhangzhiwei
 * 用户缓存相关方法
 */
@Component
@ConditionalOnClass(CacheManager.class)
public class UserCacheUtil {

    static UserCacheUtil ME;

    public static String getUserName(Long createUserId) {
        return "";

    }

    @PostConstruct
    public void init() {
        ME = this;
    }
    @Autowired
    Redis redis;
}
