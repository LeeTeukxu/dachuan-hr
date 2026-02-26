package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthEmpRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 单员工薪资计算结果，用于先算后存模式。
 * 构造后不可变：内部 List 做了防御性拷贝。
 */
public class EmployeeSalaryResult {
    private final HrmSalaryMonthEmpRecord empRecord;
    private final List<HrmSalaryMonthOptionValue> baseOptions;
    private final List<HrmSalaryMonthOptionValue> finalOptions;
    private final boolean existed;

    public EmployeeSalaryResult(HrmSalaryMonthEmpRecord empRecord,
                                 List<HrmSalaryMonthOptionValue> baseOptions,
                                 List<HrmSalaryMonthOptionValue> finalOptions,
                                 boolean existed) {
        this.empRecord = Objects.requireNonNull(empRecord, "empRecord must not be null");
        this.baseOptions = baseOptions != null
            ? Collections.unmodifiableList(new ArrayList<>(baseOptions))
            : Collections.emptyList();
        this.finalOptions = finalOptions != null
            ? Collections.unmodifiableList(new ArrayList<>(finalOptions))
            : Collections.emptyList();
        this.existed = existed;
    }

    public HrmSalaryMonthEmpRecord getEmpRecord() { return empRecord; }
    public List<HrmSalaryMonthOptionValue> getBaseOptions() { return baseOptions; }
    public List<HrmSalaryMonthOptionValue> getFinalOptions() { return finalOptions; }
    public boolean isExisted() { return existed; }

    @Override
    public String toString() {
        return "EmployeeSalaryResult{empId=" + empRecord.getEmployeeId()
            + ", existed=" + existed
            + ", baseOptions=" + baseOptions.size()
            + ", finalOptions=" + finalOptions.size() + "}";
    }
}
