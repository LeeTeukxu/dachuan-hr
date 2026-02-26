package com.tianye.hrsystem.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_USER = "createBy";
    @Override
    public void insertFill(MetaObject metaObject) {
        System.out.println("start insert fill...");
        LoginUserInfo info = CompanyContext.get();
        this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("createUserId", info.getUserIdValue(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        System.out.println("start update fill...");
        LoginUserInfo info = CompanyContext.get();
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("createUserId", info.getUserIdValue());
    }
}
