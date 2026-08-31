package com.tianye.hrsystem.modules.miniapp.support;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序 API 客户端：code2session（换 openid）、getuserphonenumber（换手机号）。
 */
@Component
public class MiniAppWxClient {

    private static final Logger logger = LoggerFactory.getLogger(MiniAppWxClient.class);

    @Value("${wx.miniapp.appid:}")
    private String appid;

    @Value("${wx.miniapp.secret:}")
    private String secret;

    /** 简单内存缓存 access_token（单实例足够；多实例部署可换 Redis） */
    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAt = 0;

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    public JSONObject code2Session(String code) throws Exception {
        if (StringUtils.isBlank(appid) || StringUtils.isBlank(secret)) {
            throw new Exception("未配置 wx.miniapp.appid/secret");
        }
        String url = CODE2SESSION_URL + "?appid=" + appid + "&secret=" + secret
                + "&js_code=" + code + "&grant_type=authorization_code";
        String resp = HttpUtil.get(url, 8000);
        JSONObject json = JSON.parseObject(resp);
        if (json == null || json.getInteger("errcode") != 0) {
            throw new Exception("微信登录失败: " + (json == null ? resp : json.getString("errmsg")));
        }
        return json;
    }

    public String getPhoneByCode(String phoneCode) throws Exception {
        String accessToken = getAccessToken();
        String url = PHONE_URL + "?access_token=" + accessToken;
        Map<String, Object> body = new HashMap<>();
        body.put("code", phoneCode);
        String resp = HttpUtil.post(url, JSON.toJSONString(body));
        JSONObject json = JSON.parseObject(resp);
        if (json == null || json.getInteger("errcode") != 0) {
            throw new Exception("获取手机号失败: " + (json == null ? resp : json.getString("errmsg")));
        }
        JSONObject phoneInfo = json.getJSONObject("phone_info");
        if (phoneInfo == null) {
            throw new Exception("手机号信息为空");
        }
        return phoneInfo.getString("purePhoneNumber");
    }

    private String getAccessToken() throws Exception {
        if (StringUtils.isBlank(appid) || StringUtils.isBlank(secret)) {
            throw new Exception("未配置 wx.miniapp.appid/secret");
        }
        if (cachedAccessToken != null && System.currentTimeMillis() < accessTokenExpireAt - 60_000) {
            return cachedAccessToken;
        }
        String url = ACCESS_TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        String resp = HttpUtil.get(url, 8000);
        JSONObject json = JSON.parseObject(resp);
        if (json == null || json.getInteger("errcode") != null) {
            throw new Exception("获取access_token失败: " + (json == null ? resp : json.getString("errmsg")));
        }
        cachedAccessToken = json.getString("access_token");
        accessTokenExpireAt = System.currentTimeMillis() + json.getInteger("expires_in") * 1000L;
        return cachedAccessToken;
    }
}