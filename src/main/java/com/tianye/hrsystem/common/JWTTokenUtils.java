package com.tianye.hrsystem.common;

import com.alibaba.fastjson.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Calendar;

/**
 * @ClassName: TokenUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 14:42
 **/
@Component
public class JWTTokenUtils {
    private static  String SIGN="!TIANYE_HR_20240306$$";
    public static  String getToken(LoginUserInfo Info){
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, 60);
        String X= JSON.toJSONString(Info);
        return JWT.create().withExpiresAt(instance.getTime()).withClaim("Content",X).sign(Algorithm.HMAC256(SIGN));
    }
    public static  LoginUserInfo GetByToken(String Token) throws Exception{
       DecodedJWT result= JWT.require(Algorithm.HMAC256(SIGN)).build().verify(Token);
       String X=result.getClaim("Content").asString();
       if(StringUtils.isEmpty(X)) return null;
       else return JSON.parseObject(X,LoginUserInfo.class);
    }
}
