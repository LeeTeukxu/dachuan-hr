package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.vo.DeptVO;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.service.IHrmDeptService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HrmEmployeeServiceImplTransferSimpleEmpTest {

    @Test
    public void transferSimpleEmp_shouldPreserveDeptIdForFrontendDepartmentSelection() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        setField(service, "hrmDeptService", createDeptServiceProxy());

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(101L);
        employee.setEmployeeName("张三");
        employee.setDeptId(88L);
        employee.setPost("装配工");
        employee.setIsDel(0);
        employee.setEntryStatus(1);

        Object simpleEmployee = service.transferSimpleEmp(employee);
        Method getDeptId = simpleEmployee.getClass().getMethod("getDeptId");
        Object deptId = getDeptId.invoke(simpleEmployee);

        Assert.assertEquals("员工简表必须返回deptId，供审批数据按部门反向映射员工", 88L, deptId);
    }

    @Test
    public void transferSimpleEmp_shouldPreserveMobileForDuplicateNameSelection() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        setField(service, "hrmDeptService", createDeptServiceProxy());

        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(102L);
        employee.setEmployeeName("王芳");
        employee.setMobile("13800000001");
        employee.setDeptId(88L);
        employee.setIsDel(0);
        employee.setEntryStatus(1);

        Object simpleEmployee = service.transferSimpleEmp(employee);
        Method getMobile = simpleEmployee.getClass().getMethod("getMobile");
        Object mobile = getMobile.invoke(simpleEmployee);

        Assert.assertEquals("员工简表必须返回手机号，供全勤金额设置穿梭框区分同名员工", "13800000001", mobile);
    }

    private Object createDeptServiceProxy() {
        return Proxy.newProxyInstance(
                IHrmDeptService.class.getClassLoader(),
                new Class<?>[]{IHrmDeptService.class},
                (proxy, method, args) -> {
                    if ("queryById".equals(method.getName())) {
                        DeptVO deptVO = new DeptVO();
                        deptVO.setDeptId(88L);
                        deptVO.setName("装配部");
                        return deptVO;
                    }
                    return null;
                }
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
