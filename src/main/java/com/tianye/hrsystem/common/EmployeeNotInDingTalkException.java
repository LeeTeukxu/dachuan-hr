package com.tianye.hrsystem.common;

/**
 * 员工在钉钉中无法匹配（姓名+手机号查无此人），保存/操作应被拒绝并提示先在钉钉创建员工。
 * 与"钉钉服务暂时不可用"区分：后者不应拒绝保存，员工以"待映射"状态放行。
 */
public class EmployeeNotInDingTalkException extends Exception {

    /** 钉钉权限缺失时的用户指引 */
    public static final String DINGTALK_PERMISSION_GUIDANCE =
            "钉钉应用未开通通讯录权限，请联系管理员登录钉钉开放平台，在「权限管理」中开通「通讯录部门成员读信息」权限";

    /** 识别钉钉权限类错误（errcode 88 / sub_code 60011 等） */
    public static boolean isDingTalkPermissionError(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("尚未开通所需的权限")
                || message.contains("60011")
                || message.contains("qyapi_get_department")
                || message.contains("ip is not in whitelist") ;
    }

    public EmployeeNotInDingTalkException(String message) {
        super(message);
    }

    public EmployeeNotInDingTalkException(String message, Throwable cause) {
        super(message, cause);
    }
}
