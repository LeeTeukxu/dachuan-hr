package com.tianye.hrsystem.modules.insurance.vo;

import org.junit.Assert;
import org.junit.Test;

public class InsuranceSchemeListVOTest {

    @Test
    public void insuranceSchemeListVo_shouldExposeUsageEmployeeNames() {
        InsuranceSchemeListVO vo = new InsuranceSchemeListVO();

        vo.setUseEmployeeNames("张三、李四");

        Assert.assertEquals("张三、李四", vo.getUseEmployeeNames());
    }
}
