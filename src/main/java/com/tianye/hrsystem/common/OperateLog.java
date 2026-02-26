package com.tianye.hrsystem.common;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author: admin
 * @version: v1.0
 * @date:2023/8/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface OperateLog {

    //操作行为
    BehaviorEnum behavior() default BehaviorEnum.NULL;

    //操作对象
    OperateObjectEnum object() default OperateObjectEnum.NULL;

    //操作类型
    OperateTypeEnum type() default OperateTypeEnum.GENERAL;

    //应用名称
    ApplyEnum apply() default ApplyEnum.NULL;


}
