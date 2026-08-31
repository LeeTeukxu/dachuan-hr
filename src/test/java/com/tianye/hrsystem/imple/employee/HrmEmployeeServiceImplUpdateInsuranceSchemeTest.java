package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.entity.bo.UpdateInsuranceSchemeBO;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class HrmEmployeeServiceImplUpdateInsuranceSchemeTest {

    @Test
    public void updateInsuranceScheme_shouldRejectEmptyEmployeeSelection() {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        UpdateInsuranceSchemeBO request = new UpdateInsuranceSchemeBO();
        request.setSchemeId(1001L);
        request.setEmployeeIds(Collections.emptyList());

        CrmException exception = catchCrmException(() -> service.updateInsuranceScheme(request));

        Assert.assertEquals("请先选择要设置参保方案的员工", exception.getMsg());
    }

    @Test
    public void updateInsuranceScheme_shouldRejectMissingScheme() {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        UpdateInsuranceSchemeBO request = new UpdateInsuranceSchemeBO();
        request.setEmployeeIds(Collections.singletonList(101L));

        CrmException exception = catchCrmException(() -> service.updateInsuranceScheme(request));

        Assert.assertEquals("请选择参保方案", exception.getMsg());
    }

    private CrmException catchCrmException(Runnable runnable) {
        try {
            runnable.run();
            Assert.fail("expected CrmException");
            return null;
        } catch (CrmException exception) {
            return exception;
        }
    }
}
