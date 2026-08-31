package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.bo.AddEmployeeBO;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.util.EmployeeUtil;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

public class HrmEmployeeServiceImplCompanyAgeTest {

    @Test
    public void prepareEmployeeForAdd_shouldKeepExplicitCompanyAgeStartTime() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        HrmEmployee employee = new HrmEmployee();
        employee.setEntryTime(LocalDate.of(2024, 1, 10));
        employee.setCompanyAgeStartTime(LocalDate.of(2023, 5, 1));

        invokePrepareEmployeeForAdd(service, employee);

        Assert.assertEquals("新增员工显式选择司龄开始日期时不能被入职日期覆盖", LocalDate.of(2023, 5, 1), employee.getCompanyAgeStartTime());
    }

    @Test
    public void prepareEmployeeForAdd_shouldFallbackCompanyAgeStartTimeToEntryTime() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        HrmEmployee employee = new HrmEmployee();
        employee.setEntryTime(LocalDate.of(2024, 1, 10));

        invokePrepareEmployeeForAdd(service, employee);

        Assert.assertEquals("新增员工未选择司龄开始日期时默认使用入职日期", LocalDate.of(2024, 1, 10), employee.getCompanyAgeStartTime());
    }

    @Test
    public void addEmployeeBO_shouldAcceptCompanyAgeStartTime() {
        AddEmployeeBO request = new AddEmployeeBO();
        request.setCompanyAgeStartTime(LocalDate.of(2023, 5, 1));

        Assert.assertEquals(LocalDate.of(2023, 5, 1), request.getCompanyAgeStartTime());
    }

    @Test
    public void addEmployeeBO_shouldAcceptAffiliationSystemAndRestType() {
        AddEmployeeBO request = new AddEmployeeBO();
        request.setAffiliationSystem(2);
        request.setRestType(2);

        Assert.assertEquals(Integer.valueOf(2), request.getAffiliationSystem());
        Assert.assertEquals(Integer.valueOf(2), request.getRestType());
    }

    @Test
    public void updatePostInformation_shouldMapCompanyAgeStartTimeField() {
        JSONObject fixedFieldPayload = new JSONObject();
        fixedFieldPayload.put("company_age_start_time", "2023-05-01");

        HrmEmployee employee = fixedFieldPayload.toJavaObject(HrmEmployee.class);

        Assert.assertEquals("编辑员工岗位信息时，固定字段下划线名称应能映射为司龄开始日期", LocalDate.of(2023, 5, 1), employee.getCompanyAgeStartTime());
    }

    @Test
    public void computeCompanyAge_shouldTreatNullAsBlank() {
        Assert.assertEquals("历史员工司龄天数为空时，司龄描述应为空字符串而不是报错", "", EmployeeUtil.computeCompanyAge(null));
    }

    private void invokePrepareEmployeeForAdd(HrmEmployeeServiceImpl service, HrmEmployee employee) throws Exception {
        Method method = HrmEmployeeServiceImpl.class.getDeclaredMethod("prepareEmployeeForAdd", HrmEmployee.class);
        method.setAccessible(true);
        method.invoke(service, employee);
    }
}
