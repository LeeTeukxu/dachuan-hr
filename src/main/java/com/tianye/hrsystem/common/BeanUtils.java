package com.tianye.hrsystem.common;

import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 * @ClassName: BeanUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月21日 13:50
 **/
public class BeanUtils {
    public static <T> T Clone(Object Obj,Class<T> tClass){
        String O= JSON.toJSONString(Obj);
        return JSON.parseObject(O,tClass);
    }
    public static <T> List<T> CloneArray(Object obj,Class<T> tClass){
        String O= JSON.toJSONString(obj);
        return JSON.parseArray(O,tClass);
    }
}
