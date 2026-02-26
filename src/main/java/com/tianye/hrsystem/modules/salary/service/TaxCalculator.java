package com.tianye.hrsystem.modules.salary.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 统一个税计算工具：七级超额累进税率。
 * 替代 SalaryComputeServiceNew.calculateTax 和 SalaryMonthRecordServiceNew.calculateCumulativeTaxPayable。
 */
public final class TaxCalculator {

    private TaxCalculator() {}

    private static final BigDecimal[] THRESHOLDS = {
        new BigDecimal("36000"), new BigDecimal("144000"), new BigDecimal("300000"),
        new BigDecimal("420000"), new BigDecimal("660000"), new BigDecimal("960000")
    };
    private static final BigDecimal[] RATES = {
        new BigDecimal("0.03"), new BigDecimal("0.10"), new BigDecimal("0.20"),
        new BigDecimal("0.25"), new BigDecimal("0.30"), new BigDecimal("0.35"), new BigDecimal("0.45")
    };
    private static final BigDecimal[] QUICK_DEDUCTIONS = {
        new BigDecimal("0"), new BigDecimal("2520"), new BigDecimal("16920"),
        new BigDecimal("31920"), new BigDecimal("52920"), new BigDecimal("85920"), new BigDecimal("181920")
    };

    public static final BigDecimal MONTHLY_DEDUCTION = new BigDecimal("5000");

    static {
        if (RATES.length != QUICK_DEDUCTIONS.length || THRESHOLDS.length != RATES.length - 1) {
            throw new ExceptionInInitializerError("税率表数组长度不一致: THRESHOLDS=" + THRESHOLDS.length
                + ", RATES=" + RATES.length + ", QUICK_DEDUCTIONS=" + QUICK_DEDUCTIONS.length);
        }
    }

    /**
     * 计算累计应纳税额。
     * 公式：累计应纳税所得额 × 适用税率 - 速算扣除数，结果 ≥ 0，保留2位小数。
     */
    public static BigDecimal calculateCumulativeTax(BigDecimal cumulativeTaxableIncome) {
        if (cumulativeTaxableIncome == null || cumulativeTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int bracket = RATES.length - 1;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (cumulativeTaxableIncome.compareTo(THRESHOLDS[i]) <= 0) {
                bracket = i;
                break;
            }
        }
        BigDecimal tax = cumulativeTaxableIncome.multiply(RATES[bracket]).subtract(QUICK_DEDUCTIONS[bracket]);
        return tax.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算累计应纳税所得额 = 累计收入 - 累计减除费用 - 累计专项扣除 - 累计专项附加扣除，结果 ≥ 0。
     */
    public static BigDecimal calculateTaxableIncome(BigDecimal cumulativeIncome,
                                                     BigDecimal cumulativeDeductions,
                                                     BigDecimal cumulativeSpecialDeduction,
                                                     BigDecimal cumulativeSpecialAdditionalDeduction) {
        BigDecimal result = safe(cumulativeIncome)
            .subtract(safe(cumulativeDeductions))
            .subtract(safe(cumulativeSpecialDeduction))
            .subtract(safe(cumulativeSpecialAdditionalDeduction));
        return (result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
