package com.tianye.hrsystem.autoTask.common;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.taobao.api.ApiException;
import com.taobao.api.TaobaoRequest;
import com.taobao.api.TaobaoResponse;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 钉钉API调用模板
 * 统一处理：Token获取、限流、执行、错误检查、日志记录、重试
 */
@Component
public class DingTalkApiTemplate {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkApiTemplate.class);

    @Autowired
    private IAccessToken accessToken;

    @Autowired
    private DingTalkRateLimiter rateLimiter;

    @Autowired
    private DDTalkResposeLogger ddLogger;

    /**
     * 执行钉钉API调用（带限流+重试+日志）
     *
     * @param companyId 公司ID
     * @param url       API地址
     * @param request   请求对象
     * @param apiName   API名称（用于日志）
     * @param callerClass 调用者类（用于日志）
     * @return 响应对象
     */
    public <REQ extends TaobaoRequest<RSP>, RSP extends TaobaoResponse> RSP execute(
            String companyId, String url, REQ request, String apiName, Class<?> callerClass) throws Exception {

        return rateLimiter.executeWithRetry(() -> {
            // 1. 限流等待
            rateLimiter.acquire(companyId);

            // 2. 获取Token
            String token = accessToken.Refresh();

            // 3. 执行请求
            DingTalkClient client = new DefaultDingTalkClient(url);
            RSP rsp = client.execute(request, token);

            // 4. 检查响应（先于日志，避免 NPE）
            if (rsp == null) {
                throw new ApiException("钉钉API返回null: " + apiName);
            }

            // 5. 记录日志
            ddLogger.Info(rsp, url, new Date(), callerClass);

            // 6. 检查钉钉业务错误码
            if (!rsp.isSuccess()) {
                String errMsg = String.format("钉钉API业务错误[%s]: errorCode=%s, msg=%s, subCode=%s",
                        apiName, rsp.getErrorCode(), rsp.getMsg(), rsp.getSubCode());
                // 限流错误可重试
                if ("isv.limitedFrequency".equals(rsp.getSubCode())) {
                    logger.warn(errMsg);
                    throw new ApiException(errMsg);
                }
                // 其他业务错误直接抛出，不可重试
                throw new ApiException(errMsg);
            }

            return rsp;
        }, 3, apiName);
    }

    /**
     * 执行钉钉API调用（简化版，不记录响应日志）
     */
    public <REQ extends TaobaoRequest<RSP>, RSP extends TaobaoResponse> RSP executeSimple(
            String companyId, String url, REQ request, String apiName) throws Exception {

        return rateLimiter.executeWithRetry(() -> {
            rateLimiter.acquire(companyId);
            String token = accessToken.Refresh();
            DingTalkClient client = new DefaultDingTalkClient(url);
            RSP rsp = client.execute(request, token);
            if (rsp == null) {
                throw new ApiException("钉钉API返回null: " + apiName);
            }
            if (!rsp.isSuccess()) {
                String errMsg = String.format("钉钉API业务错误[%s]: errorCode=%s, msg=%s, subCode=%s",
                        apiName, rsp.getErrorCode(), rsp.getMsg(), rsp.getSubCode());
                if ("isv.limitedFrequency".equals(rsp.getSubCode())) {
                    logger.warn(errMsg);
                    throw new ApiException(errMsg);
                }
                throw new ApiException(errMsg);
            }
            return rsp;
        }, 3, apiName);
    }
}
