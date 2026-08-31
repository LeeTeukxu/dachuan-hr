package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.entity.bo.UpdateInformationBO;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HrmEmployeeServiceImplUniqueValidationTest {

    @Test
    public void validateEmployeeUniqueFields_shouldRejectDuplicateJobNumberMobileAndIdNumber() {
        InMemoryEmployeeService service = new InMemoryEmployeeService();
        service.employees.add(employee(1L, "老员工", "TYNG-001", "13900000000", "420821198802091515", 0));

        assertDuplicateMessage(service, employee(null, "新员工", "TYNG-001", "13900000001", "420821198802091516", 0), "工号");
        assertDuplicateMessage(service, employee(null, "新员工", "TYNG-002", "139-0000-0000", "420821198802091516", 0), "手机号");
        assertDuplicateMessage(service, employee(null, "新员工", "TYNG-002", "13900000001", "420821198802091515", 0), "身份证号");
    }

    @Test
    public void validateEmployeeUniqueFields_shouldIgnoreCurrentEmployeeAndDeletedEmployees() {
        InMemoryEmployeeService service = new InMemoryEmployeeService();
        service.employees.add(employee(1L, "当前员工", "TYNG-001", "13900000000", "420821198802091515", 0));
        service.employees.add(employee(2L, "已删员工", "TYNG-002", "13900000001", "420821198802091516", 1));

        service.validateEmployeeUniqueFields(employee(1L, "当前员工", "TYNG-001", "13900000000", "420821198802091515", 0), 1L);
        service.validateEmployeeUniqueFields(employee(null, "新员工", "TYNG-002", "13900000001", "420821198802091516", 0), null);
    }

    @Test
    public void updateCommunication_shouldRejectDuplicateMobileBeforeSaving() {
        InMemoryEmployeeService service = new InMemoryEmployeeService();
        service.employees.add(employee(1L, "当前员工", "TYNG-001", "13800000000", "420821198802091515", 0));
        service.employees.add(employee(2L, "其他员工", "TYNG-002", "13900000000", "420821198802091516", 0));
        service.currentEmployee = service.employees.get(0);

        UpdateInformationBO updateInformationBO = new UpdateInformationBO();
        updateInformationBO.setEmployeeId(1L);
        UpdateInformationBO.InformationFieldBO mobileField = new UpdateInformationBO.InformationFieldBO();
        mobileField.setIsFixed(1);
        mobileField.setFieldName("mobile");
        mobileField.setFieldValue("13900000000");
        updateInformationBO.setDataList(Collections.singletonList(mobileField));

        assertDuplicateMessage(service, updateInformationBO, "手机号");
        Assert.assertTrue("重复手机号应在保存前拦截", service.updated.isEmpty());
    }

    private void assertDuplicateMessage(InMemoryEmployeeService service, HrmEmployee employee, String messagePart) {
        try {
            service.validateEmployeeUniqueFields(employee, employee.getEmployeeId());
            Assert.fail("Expected duplicate " + messagePart + " to be rejected");
        } catch (CrmException ex) {
            Assert.assertTrue("错误文案应包含：" + messagePart + "，实际：" + ex.getMsg(), ex.getMsg().contains(messagePart));
        }
    }

    private void assertDuplicateMessage(InMemoryEmployeeService service, UpdateInformationBO updateInformationBO, String messagePart) {
        try {
            service.updateCommunication(updateInformationBO);
            Assert.fail("Expected duplicate " + messagePart + " to be rejected");
        } catch (CrmException ex) {
            Assert.assertTrue("错误文案应包含：" + messagePart + "，实际：" + ex.getMsg(), ex.getMsg().contains(messagePart));
        }
    }

    private HrmEmployee employee(Long employeeId, String name, String jobNumber, String mobile, String idNumber, Integer isDel) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(name);
        employee.setJobNumber(jobNumber);
        employee.setMobile(mobile);
        employee.setIdNumber(idNumber);
        employee.setIsDel(isDel);
        return employee;
    }

    private static class InMemoryEmployeeService extends HrmEmployeeServiceImpl {
        private final List<HrmEmployee> employees = new ArrayList<>();
        private final List<HrmEmployee> updated = new ArrayList<>();
        private HrmEmployee currentEmployee;

        @Override
        public List<HrmEmployee> list() {
            return employees;
        }

        @Override
        public HrmEmployee queryById(Long employeeId) {
            return currentEmployee;
        }

        @Override
        public boolean updateById(HrmEmployee entity) {
            updated.add(entity);
            Assert.fail("重复唯一字段应在 updateById 之前拦截");
            return true;
        }
    }
}
