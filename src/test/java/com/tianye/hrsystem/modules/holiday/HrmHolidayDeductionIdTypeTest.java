package com.tianye.hrsystem.modules.holiday;

import com.tianye.hrsystem.modules.holiday.bo.UpdateHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.entity.HrmHolidayDeduction;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import org.junit.Assert;
import org.junit.Test;

public class HrmHolidayDeductionIdTypeTest {

    @Test
    public void deductionId_shouldUseStringTypeBecauseDatabaseStoresUuidValues() throws Exception {
        Assert.assertEquals(String.class, QueryHolidayDeductionVO.class.getDeclaredField("deductionId").getType());
        Assert.assertEquals(String.class, HrmHolidayDeduction.class.getDeclaredField("deductionId").getType());
        Assert.assertEquals(String.class, UpdateHolidayDeductionBO.class.getDeclaredField("deductionId").getType());
    }
}
