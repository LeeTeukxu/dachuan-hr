package com.tianye.hrsystem.imple.workplan;

import org.apache.commons.lang.StringUtils;

public class WorkPlanDingTalkErrorTranslator {

    public String translate(Throwable throwable) {
        if (throwable == null) {
            return "排班提交失败，请稍后重试";
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return translateMessage(root.getMessage());
    }

    public String translateMessage(String rawMessage) {
        if (StringUtils.isBlank(rawMessage)) {
            return "排班提交失败，请检查员工、考勤组、班次和日期配置";
        }
        String message = rawMessage.trim();
        if (containsChinese(message) && !containsEnglish(message)) {
            return message;
        }
        String lower = message.toLowerCase();
        if (lower.contains("permission") || lower.contains("access")) {
            return "钉钉接口权限不足，请联系管理员检查考勤排班权限";
        }
        if (lower.contains("flow control") || lower.contains("rate") || lower.contains("frequency")) {
            return "钉钉接口调用过于频繁，请稍后重试";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "钉钉接口响应超时，请稍后重试";
        }
        if (lower.contains("network") || lower.contains("connect") || lower.contains("connection")) {
            return "网络连接异常，请稍后重试";
        }
        if (lower.contains("group") && (lower.contains("not") || lower.contains("exist") || lower.contains("invalid"))) {
            return "考勤组不存在或已失效";
        }
        if (lower.contains("shift") && (lower.contains("not") || lower.contains("exist") || lower.contains("invalid"))) {
            return "班次不存在或已失效";
        }
        if (lower.contains("user") && (lower.contains("not") || lower.contains("exist") || lower.contains("invalid"))) {
            return "员工不存在或未加入考勤组";
        }
        if (lower.contains("date") || lower.contains("work_date")) {
            return "排班日期不合法，请检查后重新提交";
        }
        if (containsChinese(message)) {
            return message.replaceAll("[A-Za-z0-9_./:-]+", "").trim();
        }
        return "钉钉排班提交失败，请检查员工、考勤组、班次和日期配置";
    }

    private boolean containsChinese(String text) {
        return text != null && text.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private boolean containsEnglish(String text) {
        return text != null && text.matches(".*[A-Za-z].*");
    }
}
