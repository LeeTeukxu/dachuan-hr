package com.tianye.hrsystem.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 统一API响应结果封装
 */
@ApiModel(description = "返回响应数据")
public class Result<T> {
    @ApiModelProperty(value = "返回code码: \n200: 请求成功\n400: 请求失败\n401: 未认证（签名错误）\n402: token令牌失效\n404: 接口不存在\n500: 服务器内部错误")
    private int code;
    @ApiModelProperty(value = "返回错误信息")
    private String message;
    @ApiModelProperty(value = "返回对象")
    private T data;

    public Result setCode(int code) {
        this.code = code;
        return this;
    }

    public Result setCode(ResultCode resultCode) {
        this.code = resultCode.code();
        return this;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Result setMessage(String message) {
        this.message = message;
        return this;
    }

    public T getData() {
        return data;
    }

    public Result setData(T data) {
        this.data = data;
        return this;
    }
    public static<T>  Result OK(T data){
        Result t=new Result<>();
        t.setCode(ResultCode.SUCCESS);
        t.setData(data);
        return t;
    }
    public static<T> Result OK(){
        Result t=new Result<>();
        t.setCode(ResultCode.SUCCESS);
        return t;
    }
    public static<T> Result Error(Exception ax){
        Result t=new Result();
        t.setCode(ResultCode.INTERNAL_SERVER_ERROR);
        t.setMessage(ax.getMessage());
        return t;
    }
}
