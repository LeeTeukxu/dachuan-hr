package com.tianye.hrsystem.exception;


import com.tianye.hrsystem.enums.ResultCode;
import com.tianye.hrsystem.enums.SystemCodeEnum;

/**
 * crm默认的异常处理
 *
 * @author Administrator
 */
public class HrmException extends RuntimeException implements ResultCode {

    private final String msg;
    private final int code;


    public HrmException() {
        super("参数验证错误", null, false, false);
        this.code = SystemCodeEnum.SYSTEM_NO_VALID.getCode();
        this.msg = SystemCodeEnum.SYSTEM_NO_VALID.getMsg();
    }

    public HrmException(int code, String msg) {
        super(code + ":" + msg, null, true, true);
        this.code = code;
        this.msg = msg;
    }

    public HrmException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMsg());
    }

    public HrmException(ResultCode resultCode, Object... args) {
        this(resultCode.getCode(), String.format(resultCode.getMsg(), args));
    }

    public HrmException(ResultCode resultCode, String str, Boolean flag) {
        this(resultCode.getCode(), resultCode.getMsg() + ":" + str);
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
