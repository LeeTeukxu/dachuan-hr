package com.tianye.hrsystem.imple.employee;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.vo.InformationFieldVO;
import com.tianye.hrsystem.entity.vo.PostInformationVO;
import com.tianye.hrsystem.enums.EmployeeEntryStatus;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.service.employee.IHrmEmployeeDataService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeQuitInfoService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmEmployeePostServiceImplCompanyAgeTest {

    @Test
    public void postInformation_shouldFallbackCompanyAgeStartTimeToEntryTime() throws Exception {
        HrmEmployeePostServiceImpl service = new HrmEmployeePostServiceImpl();
        IHrmEmployeeQuitInfoService quitInfoService = mock(IHrmEmployeeQuitInfoService.class);
        IHrmEmployeeService employeeService = mock(IHrmEmployeeService.class);
        IHrmEmployeeDataService employeeDataService = mock(IHrmEmployeeDataService.class);
        LambdaQueryChainWrapper quitInfoQuery = mock(LambdaQueryChainWrapper.class);

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(101L);
        employee.setEmployeeName("老员工");
        employee.setEntryTime(LocalDate.of(2021, 9, 14));
        employee.setEntryStatus(EmployeeEntryStatus.IN.getValue());

        InformationFieldVO companyAgeField = new InformationFieldVO();
        companyAgeField.setFieldName("company_age");
        companyAgeField.setFieldValueDesc("");

        when(quitInfoService.lambdaQuery()).thenReturn(quitInfoQuery);
        when(quitInfoQuery.eq(any(), eq(101L))).thenReturn(quitInfoQuery);
        when(quitInfoQuery.one()).thenReturn(null);
        when(employeeService.getById(101L)).thenReturn(employee);
        when(employeeDataService.queryListByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(employeeService.transferInformation(any(), eq(LabelGroupEnum.POST), any())).thenReturn(Collections.singletonList(companyAgeField));

        setField(service, "quitInfoService", quitInfoService);
        setField(service, "employeeService", employeeService);
        setField(service, "employeeDataService", employeeDataService);

        PostInformationVO result = service.postInformation(101L);

        InformationFieldVO resultCompanyAge = result.getInformation().get(0);
        Assert.assertNotNull("详情接口应回退写入司龄开始日期", employee.getCompanyAgeStartTime());
        Assert.assertNotNull("详情接口应动态计算司龄天数，不能因旧数据 companyAge 为空而 NPE", employee.getCompanyAge());
        Assert.assertTrue("详情接口应返回司龄描述", String.valueOf(resultCompanyAge.getFieldValueDesc()).length() > 0);
        verify(employeeService).updateById(employee);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
