package com.tianye.hrsystem.config;

import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.model.successResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SaaS 改造 P1-5：全局异常处理器
 * 未被 Controller 捕获的异常统一返回 JSON（替代 Tomcat/Spring 的 HTML 500 页面），
 * 前端 axios 拦截器可统一解析；同时避免堆栈信息泄露。
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.contains("/hrsystem/") || uri.endsWith(".html") == false);
    }

    private void writeJson(HttpServletResponse response, successResult result) throws Exception {
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().print(JSON.toJSONString(result));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object handleException(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error("【全局异常】{} {}", request.getMethod(), request.getRequestURI(), e);

        // 非接口请求（静态资源等）保留原有跳转行为
        if (!isApiRequest(request)) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return null;
        }

        successResult result = new successResult();
        if (e instanceof MaxUploadSizeExceededException) {
            result.setMessage("上传文件过大，请压缩后重试");
        } else if (e instanceof NoHandlerFoundException) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            result.setMessage("请求的接口不存在: " + request.getRequestURI());
        } else {
            result.setMessage("系统繁忙，请稍后重试: " + rootMessage(e));
        }
        try {
            writeJson(response, result);
        } catch (Exception ex) {
            log.error("【全局异常】写出 JSON 失败", ex);
        }
        return null;
    }

    private String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg == null ? cur.getClass().getSimpleName() : msg;
    }
}
