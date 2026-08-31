package com.tianye.hrsystem.model;

import org.junit.Assert;
import org.junit.Test;

public class RoleMenuEntityMappingTest {

    @Test
    public void tbrolemenu_shouldUseLongPrimaryKey_forProductionSnowflakeIds() throws Exception {
        Assert.assertEquals(Long.class, tbrolemenu.class.getDeclaredField("roleMenuId").getType());
    }
}
