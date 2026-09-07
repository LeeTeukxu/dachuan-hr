package com.tianye.hrsystem.modules.miniapp.support;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信小程序 API 客户端：code2session（换 openid）。
 */
@Component
public class MiniAppWxClient {

    @Value("${wx.miniapp.appid:}")
    private String appid;

    @Value("${wx.miniapp.secret:}")
    private String secret;

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    public JSONObject code2Session(String code) throws Exception {
        if (StringUtils.isBlank(appid) || StringUtils.isBlank(secret)) {
            throw new Exception("未配置 wx.miniapp.appid/secret");
        }
        String url = CODE2SESSION_URL + "?appid=" + appid + "&secret=" + secret
                + "&js_code=" + code + "&grant_type=authorization_code";
        String resp = HttpUtil.get(url, 8000);
        JSONObject json = JSON.parseObject(resp);
        if (json == null || (json.containsKey("errcode") && json.getInteger("errcode") != 0)) {
            throw new Exception("微信登录失败: " + (json == null ? resp : json.getString("errmsg")));
        }
        return json;
    }
}
