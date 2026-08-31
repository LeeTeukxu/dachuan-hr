package com.tianye.hrsystem.modules.salary.support;

import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;

import java.math.BigDecimal;

public final class HrmSalaryBasicDefaults {

    public static final BigDecimal ORDINARY_FULL_ATTENDANCE_AMOUNT = new BigDecimal("100");
    public static final BigDecimal LEADER_FULL_ATTENDANCE_AMOUNT = new BigDecimal("500");
    public static final BigDecimal LARGE_MEDICAL_INSURANCE_AMOUNT = new BigDecimal("15");
    public static final BigDecimal LONG_TERM_CARE_INSURANCE_AMOUNT = new BigDecimal("3");
    public static final int PRODUCTION_MONTHLY_REST_DAYS = 4;

    private HrmSalaryBasicDefaults() {
    }

    public static void applyTo(HrmSalaryBasic salaryBasic) {
        if (salaryBasic == null) {
            return;
        }
        if (salaryBasic.getOrdinaryFullAttendanceAmount() == null) {
            salaryBasic.setOrdinaryFullAttendanceAmount(ORDINARY_FULL_ATTENDANCE_AMOUNT);
        }
        if (salaryBasic.getLeaderFullAttendanceAmount() == null) {
            salaryBasic.setLeaderFullAttendanceAmount(LEADER_FULL_ATTENDANCE_AMOUNT);
        }
        if (salaryBasic.getProductionMonthlyRestDays() == null) {
            salaryBasic.setProductionMonthlyRestDays(PRODUCTION_MONTHLY_REST_DAYS);
        }
        if (salaryBasic.getLargeMedicalInsuranceAmount() == null) {
            salaryBasic.setLargeMedicalInsuranceAmount(LARGE_MEDICAL_INSURANCE_AMOUNT);
        }
        if (salaryBasic.getLongTermCareInsuranceAmount() == null) {
            salaryBasic.setLongTermCareInsuranceAmount(LONG_TERM_CARE_INSURANCE_AMOUNT);
        }
    }

    public static void applyTo(QuerySalaryBasicVO salaryBasic) {
        if (salaryBasic == null) {
            return;
        }
        if (salaryBasic.getOrdinaryFullAttendanceAmount() == null) {
            salaryBasic.setOrdinaryFullAttendanceAmount(ORDINARY_FULL_ATTENDANCE_AMOUNT);
        }
        if (salaryBasic.getLeaderFullAttendanceAmount() == null) {
            salaryBasic.setLeaderFullAttendanceAmount(LEADER_FULL_ATTENDANCE_AMOUNT);
        }
        if (salaryBasic.getProductionMonthlyRestDays() == null) {
            salaryBasic.setProductionMonthlyRestDays(PRODUCTION_MONTHLY_REST_DAYS);
        }
        if (salaryBasic.getLargeMedicalInsuranceAmount() == null) {
            salaryBasic.setLargeMedicalInsuranceAmount(LARGE_MEDICAL_INSURANCE_AMOUNT);
        }
        if (salaryBasic.getLongTermCareInsuranceAmount() == null) {
            salaryBasic.setLongTermCareInsuranceAmount(LONG_TERM_CARE_INSURANCE_AMOUNT);
        }
    }

    public static int productionMonthlyRestDays(HrmSalaryBasic salaryBasic) {
        if (salaryBasic == null || salaryBasic.getProductionMonthlyRestDays() == null) {
            return PRODUCTION_MONTHLY_REST_DAYS;
        }
        return Math.max(salaryBasic.getProductionMonthlyRestDays(), 0);
    }

    public static BigDecimal ordinaryFullAttendanceAmount(HrmSalaryBasic salaryBasic) {
        if (salaryBasic == null || salaryBasic.getOrdinaryFullAttendanceAmount() == null) {
            return ORDINARY_FULL_ATTENDANCE_AMOUNT;
        }
        return salaryBasic.getOrdinaryFullAttendanceAmount();
    }

    public static BigDecimal leaderFullAttendanceAmount(HrmSalaryBasic salaryBasic) {
        if (salaryBasic == null || salaryBasic.getLeaderFullAttendanceAmount() == null) {
            return LEADER_FULL_ATTENDANCE_AMOUNT;
        }
        return salaryBasic.getLeaderFullAttendanceAmount();
    }

    public static BigDecimal largeMedicalInsuranceAmount(HrmSalaryBasic salaryBasic) {
        if (salaryBasic == null || salaryBasic.getLargeMedicalInsuranceAmount() == null) {
            return LARGE_MEDICAL_INSURANCE_AMOUNT;
        }
        return salaryBasic.getLargeMedicalInsuranceAmount();
    }

    public static BigDecimal longTermCareInsuranceAmount(HrmSalaryBasic salaryBasic) {
        if (salaryBasic == null || salaryBasic.getLongTermCareInsuranceAmount() == null) {
            return LONG_TERM_CARE_INSURANCE_AMOUNT;
        }
        return salaryBasic.getLongTermCareInsuranceAmount();
    }
}
