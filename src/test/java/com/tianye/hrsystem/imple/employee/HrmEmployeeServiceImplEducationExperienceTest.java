package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.po.HrmEmployeeEducationExperience;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;

public class HrmEmployeeServiceImplEducationExperienceTest {

    @Test
    public void addOrUpdateEduExperience_shouldRejectMissingEmployee() {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl() {
            @Override
            public HrmEmployee getById(Serializable id) {
                return null;
            }
        };
        HrmEmployeeEducationExperience educationExperience = new HrmEmployeeEducationExperience();
        educationExperience.setEmployeeId(123456789L);
        educationExperience.setEducation(8);

        CrmException exception = catchCrmException(() -> service.addOrUpdateEduExperience(educationExperience));

        Assert.assertEquals("员工不存在，无法保存学历信息", exception.getMsg());
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
