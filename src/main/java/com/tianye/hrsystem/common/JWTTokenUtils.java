package com.tianye.hrsystem.common;

import com.alibaba.fastjson.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.UUID;

/**
 * @ClassName: TokenUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 14:42
 **/
@Component
public class JWTTokenUtils {
    private static  String SIGN="!TIANYE_HR_20240306$$";
    /** 登录令牌有效期（小时），可通过 hrm.jwt.expire-hours 配置，默认 8 小时 */
    private static int expireHours = 8;

    @Value("${hrm.jwt.expire-hours:8}")
    public void setExpireHours(int value) {
        if (value >= 1) {
            expireHours = value;
        }
    }

    public static int getExpireSeconds() {
        return expireHours * 3600;
    }

    public static  String getToken(LoginUserInfo Info){
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.HOUR, expireHours);
        String X= JSON.toJSONString(Info);
        // 为每个令牌分配唯一 jti，支持退出登录/禁用时的服务端吊销（Redis 黑名单）
        return JWT.create().withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(instance.getTime()).withClaim("Content",X)
                .sign(Algorithm.HMAC256(SIGN));
    }

    public static  LoginUserInfo GetByToken(String Token) throws Exception{
       DecodedJWT result= JWT.require(Algorithm.HMAC256(SIGN)).build().verify(Token);
       String X=result.getClaim("Content").asString();
       if(StringUtils.isEmpty(X)) return null;
       else return JSON.parseObject(X,LoginUserInfo.class);
    }

    /** 从已签名的令牌中提取 jti（用于黑名单校验），校验失败返回 null */
    public static String getJti(String Token){
        try {
            DecodedJWT result = JWT.require(Algorithm.HMAC256(SIGN)).build().verify(Token);
            return result.getClaim("jti").asString();
        } catch (Exception e) {
            return null;
        }
    }
}
