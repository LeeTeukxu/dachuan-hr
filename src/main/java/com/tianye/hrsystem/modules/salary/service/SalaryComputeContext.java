package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryArchivesOption;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryOption;

import java.math.BigDecimal;
import java.util.*;

/**
 * 薪资计算上下文：封装 doComputeSalaryData 中批量预加载的所有数据，
 * 替代 computeAndSaveEmployeeSalary 的20个参数。
 */
public class SalaryComputeContext {
    private int year;
    private int month;
    private boolean isSyncInsuranceData;
    private boolean isSyncAttendanceData;
    private HrmSalaryConfig salaryConfig;
    private Map<String, Map<Integer, String>> attendanceDataMap;
    private List<HrmSalaryOption> noFixedSalaryOptionList;
    private Map<Long, HrmProduceAttendance> produceAttendanceMap;
    private Map<Integer, Double> normalDaysByDeptType;
    private Map<Integer, Integer> optionParentCodeMap;
    private Map<Long, Map<Integer, String>> lastMonthTaxDataMap;
    private Map<Long, BigDecimal> lastYearAccumulatedIncomeMap;
    private Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap;
    private Map<Long, HrmAdditional> additionalDeductionMap;
    private Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap;
    private Map<Long, Boolean> hasAttendanceGroupMap;
    private Map<Integer, HrmSalaryOption> salaryOptionConfigMap;

    private SalaryComputeContext() {}

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public boolean getIsSyncInsuranceData() { return isSyncInsuranceData; }
    public boolean getIsSyncAttendanceData() { return isSyncAttendanceData; }
    public HrmSalaryConfig getSalaryConfig() { return salaryConfig; }
    public Map<String, Map<Integer, String>> getAttendanceDataMap() { return attendanceDataMap; }
    public List<HrmSalaryOption> getNoFixedSalaryOptionList() { return noFixedSalaryOptionList; }
    public Map<Long, HrmProduceAttendance> getProduceAttendanceMap() { return produceAttendanceMap; }
    public Map<Integer, Double> getNormalDaysByDeptType() { return normalDaysByDeptType; }
    public Map<Integer, Integer> getOptionParentCodeMap() { return optionParentCodeMap; }
    public Map<Long, Map<Integer, String>> getLastMonthTaxDataMap() { return lastMonthTaxDataMap; }
    public Map<Long, BigDecimal> getLastYearAccumulatedIncomeMap() { return lastYearAccumulatedIncomeMap; }
    public Map<Long, HrmInsuranceMonthEmpRecord> getSocialSecurityEmpRecordMap() { return socialSecurityEmpRecordMap; }
    public Map<Long, HrmAdditional> getAdditionalDeductionMap() { return additionalDeductionMap; }
    public Map<Long, List<HrmSalaryArchivesOption>> getMidMonthArchivesOptionMap() { return midMonthArchivesOptionMap; }
    public Map<Long, Boolean> getHasAttendanceGroupMap() { return hasAttendanceGroupMap; }
    public Map<Integer, HrmSalaryOption> getSalaryOptionConfigMap() { return salaryOptionConfigMap; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int year;
        private int month;
        private boolean isSyncInsuranceData;
        private boolean isSyncAttendanceData;
        private HrmSalaryConfig salaryConfig;
        private Map<String, Map<Integer, String>> attendanceDataMap;
        private List<HrmSalaryOption> noFixedSalaryOptionList;
        private Map<Long, HrmProduceAttendance> produceAttendanceMap;
        private Map<Integer, Double> normalDaysByDeptType;
        private Map<Integer, Integer> optionParentCodeMap;
        private Map<Long, Map<Integer, String>> lastMonthTaxDataMap;
        private Map<Long, BigDecimal> lastYearAccumulatedIncomeMap;
        private Map<Long, HrmInsuranceMonthEmpRecord> socialSecurityEmpRecordMap;
        private Map<Long, HrmAdditional> additionalDeductionMap;
        private Map<Long, List<HrmSalaryArchivesOption>> midMonthArchivesOptionMap;
        private Map<Long, Boolean> hasAttendanceGroupMap;
        private Map<Integer, HrmSalaryOption> salaryOptionConfigMap;

        public Builder year(int v) { this.year = v; return this; }
        public Builder month(int v) { this.month = v; return this; }
        public Builder isSyncInsuranceData(boolean v) { this.isSyncInsuranceData = v; return this; }
        public Builder isSyncAttendanceData(boolean v) { this.isSyncAttendanceData = v; return this; }
        public Builder salaryConfig(HrmSalaryConfig v) { this.salaryConfig = v; return this; }
        public Builder attendanceDataMap(Map<String, Map<Integer, String>> v) { this.attendanceDataMap = v; return this; }
        public Builder noFixedSalaryOptionList(List<HrmSalaryOption> v) { this.noFixedSalaryOptionList = v; return this; }
        public Builder produceAttendanceMap(Map<Long, HrmProduceAttendance> v) { this.produceAttendanceMap = v; return this; }
        public Builder normalDaysByDeptType(Map<Integer, Double> v) { this.normalDaysByDeptType = v; return this; }
        public Builder optionParentCodeMap(Map<Integer, Integer> v) { this.optionParentCodeMap = v; return this; }
        public Builder lastMonthTaxDataMap(Map<Long, Map<Integer, String>> v) { this.lastMonthTaxDataMap = v; return this; }
        public Builder lastYearAccumulatedIncomeMap(Map<Long, BigDecimal> v) { this.lastYearAccumulatedIncomeMap = v; return this; }
        public Builder socialSecurityEmpRecordMap(Map<Long, HrmInsuranceMonthEmpRecord> v) { this.socialSecurityEmpRecordMap = v; return this; }
        public Builder additionalDeductionMap(Map<Long, HrmAdditional> v) { this.additionalDeductionMap = v; return this; }
        public Builder midMonthArchivesOptionMap(Map<Long, List<HrmSalaryArchivesOption>> v) { this.midMonthArchivesOptionMap = v; return this; }
        public Builder hasAttendanceGroupMap(Map<Long, Boolean> v) { this.hasAttendanceGroupMap = v; return this; }
        public Builder salaryOptionConfigMap(Map<Integer, HrmSalaryOption> v) { this.salaryOptionConfigMap = v; return this; }

        public SalaryComputeContext build() {
            SalaryComputeContext ctx = new SalaryComputeContext();
            ctx.year = this.year;
            ctx.month = this.month;
            ctx.isSyncInsuranceData = this.isSyncInsuranceData;
            ctx.isSyncAttendanceData = this.isSyncAttendanceData;
            ctx.salaryConfig = this.salaryConfig;
            ctx.attendanceDataMap = this.attendanceDataMap != null ? this.attendanceDataMap : Collections.emptyMap();
            ctx.noFixedSalaryOptionList = this.noFixedSalaryOptionList != null ? this.noFixedSalaryOptionList : Collections.emptyList();
            ctx.produceAttendanceMap = this.produceAttendanceMap != null ? this.produceAttendanceMap : Collections.emptyMap();
            ctx.normalDaysByDeptType = this.normalDaysByDeptType != null ? this.normalDaysByDeptType : Collections.emptyMap();
            ctx.optionParentCodeMap = this.optionParentCodeMap != null ? this.optionParentCodeMap : Collections.emptyMap();
            ctx.lastMonthTaxDataMap = this.lastMonthTaxDataMap != null ? this.lastMonthTaxDataMap : Collections.emptyMap();
            ctx.lastYearAccumulatedIncomeMap = this.lastYearAccumulatedIncomeMap != null ? this.lastYearAccumulatedIncomeMap : Collections.emptyMap();
            ctx.socialSecurityEmpRecordMap = this.socialSecurityEmpRecordMap != null ? this.socialSecurityEmpRecordMap : Collections.emptyMap();
            ctx.additionalDeductionMap = this.additionalDeductionMap != null ? this.additionalDeductionMap : Collections.emptyMap();
            ctx.midMonthArchivesOptionMap = this.midMonthArchivesOptionMap != null ? this.midMonthArchivesOptionMap : Collections.emptyMap();
            ctx.hasAttendanceGroupMap = this.hasAttendanceGroupMap != null ? this.hasAttendanceGroupMap : Collections.emptyMap();
            ctx.salaryOptionConfigMap = this.salaryOptionConfigMap != null ? this.salaryOptionConfigMap : Collections.emptyMap();
            return ctx;
        }
    }
}
