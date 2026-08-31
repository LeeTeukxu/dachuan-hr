package com.tianye.hrsystem.modules.salary.support;

import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.model.HrmEmployee;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TaxImportEmployeeMatcherTest {

    @Test
    public void resolveEmployeeId_shouldMatchDuplicateNamesByMobile() {
        List<HrmEmployee> employees = Arrays.asList(
                employee(1001L, "王芳", "13032750052"),
                employee(1002L, "王芳", "15871989405")
        );

        Long employeeId = TaxImportEmployeeMatcher.resolveEmployeeId(employees, " 王芳 ", "15871989405", 3);

        Assert.assertEquals(Long.valueOf(1002L), employeeId);
    }

    @Test
    public void resolveEmployeeId_shouldAllowNameOnlyWhenNameIsUnique() {
        List<HrmEmployee> employees = Collections.singletonList(employee(1001L, "李四", "13900000000"));

        Long employeeId = TaxImportEmployeeMatcher.resolveEmployeeId(employees, "李四", "", 4);

        Assert.assertEquals(Long.valueOf(1001L), employeeId);
    }

    @Test
    public void resolveEmployeeId_shouldRejectNameOnlyWhenNameIsDuplicated() {
        List<HrmEmployee> employees = Arrays.asList(
                employee(1001L, "王芳", "13032750052"),
                employee(1002L, "王芳", "15871989405")
        );

        try {
            TaxImportEmployeeMatcher.resolveEmployeeId(employees, "王芳", "", 5);
            Assert.fail("重名员工缺少手机号时应拒绝导入");
        } catch (CrmException exception) {
            Assert.assertTrue(exception.getMsg().contains("第5行员工姓名重复"));
            Assert.assertTrue(exception.getMsg().contains("王芳"));
            Assert.assertTrue(exception.getMsg().contains("手机号"));
        }
    }

    @Test
    public void resolveEmployeeId_shouldRejectUnknownNameAndMobile() {
        List<HrmEmployee> employees = Collections.singletonList(employee(1001L, "王芳", "13032750052"));

        try {
            TaxImportEmployeeMatcher.resolveEmployeeId(employees, "王芳", "15871989405", 6);
            Assert.fail("姓名+手机号未匹配到员工时应拒绝导入");
        } catch (CrmException exception) {
            Assert.assertTrue(exception.getMsg().contains("第6行未找到员工"));
            Assert.assertTrue(exception.getMsg().contains("王芳"));
            Assert.assertTrue(exception.getMsg().contains("15871989405"));
        }
    }

    @Test
    public void readCellText_shouldReturnEmptyForMissingColumn() {
        Assert.assertEquals("", TaxImportEmployeeMatcher.readCellText(Collections.singletonList("王芳"), 8));
    }

    private HrmEmployee employee(Long employeeId, String employeeName, String mobile) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(employeeName);
        employee.setMobile(mobile);
        employee.setIsDel(0);
        return employee;
    }
}
