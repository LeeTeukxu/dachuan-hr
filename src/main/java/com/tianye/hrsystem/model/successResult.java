package com.tianye.hrsystem.model;

/**
 * @ClassName: successResult
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 15:54
 **/
public class successResult {
    public successResult() {
        success = true;
        message = "";
        code = 200;
        timeOut = false;
    }
    private boolean success;
    private String message;
    private Object data;
    private Integer code;

    private boolean timeOut;

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public boolean getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(boolean timeOut) {
        this.timeOut = timeOut;
    }

    public void raiseException(Exception ax){
        this.setSuccess(false);
        this.code = 500;
        this.setMessage(ax.getMessage());
    }
}
