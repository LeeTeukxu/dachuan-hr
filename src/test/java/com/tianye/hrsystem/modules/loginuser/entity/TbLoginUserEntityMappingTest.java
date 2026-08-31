package com.tianye.hrsystem.modules.loginuser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.junit.Assert;
import org.junit.Test;

public class TbLoginUserEntityMappingTest {

    @Test
    public void id_shouldUseDatabaseAutoIncrementForLoginUserTable() throws Exception {
        TableId tableId = TbLoginUser.class.getDeclaredField("id").getAnnotation(TableId.class);

        Assert.assertEquals("tbloginuser.id 是数据库自增主键，新建登录用户时不能生成雪花 ID", IdType.AUTO, tableId.type());
    }
}
