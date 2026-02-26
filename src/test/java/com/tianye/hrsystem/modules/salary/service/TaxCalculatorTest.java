package com.tianye.hrsystem.modules.salary.service;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class TaxCalculatorTest {

    @Test
    public void calculateCumulativeTax_zeroIncome_shouldReturnZero() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(BigDecimal.ZERO);
        Assert.assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void calculateCumulativeTax_negativeIncome_shouldReturnZero() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("-1000"));
        Assert.assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void calculateCumulativeTax_firstBracket_36000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("36000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("1080.00")));
    }

    @Test
    public void calculateCumulativeTax_secondBracket_100000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("100000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("7480.00")));
    }

    @Test
    public void calculateCumulativeTax_thirdBracket_200000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("200000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("23080.00")));
    }

    @Test
    public void calculateCumulativeTax_fourthBracket_350000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("350000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("55580.00")));
    }

    @Test
    public void calculateCumulativeTax_fifthBracket_500000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("500000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("97080.00")));
    }

    @Test
    public void calculateCumulativeTax_sixthBracket_800000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("800000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("194080.00")));
    }

    @Test
    public void calculateCumulativeTax_seventhBracket_1000000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("1000000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("268080.00")));
    }

    @Test
    public void calculateCumulativeTax_boundaryExact_144000() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("144000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("11880.00")));
    }

    @Test
    public void calculateCumulativeTax_smallAmount_500() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("500"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("15.00")));
    }

    @Test
    public void calculateCumulativeTax_shouldMatchSalaryComputeServiceResult() {
        BigDecimal[] testCases = {
            new BigDecimal("0"), new BigDecimal("14072.3"), new BigDecimal("36000"),
            new BigDecimal("36001"), new BigDecimal("144000"), new BigDecimal("144001"),
            new BigDecimal("300000"), new BigDecimal("420000"), new BigDecimal("660000"),
            new BigDecimal("960000"), new BigDecimal("1000000")
        };
        for (BigDecimal income : testCases) {
            BigDecimal expected = SalaryComputeServiceNew.calculateTax(income);
            BigDecimal actual = TaxCalculator.calculateCumulativeTax(income);
            Assert.assertEquals("Mismatch for income=" + income, 0, expected.compareTo(actual));
        }
    }

    @Test
    public void calculateCumulativeTax_nullIncome_shouldReturnZero() {
        BigDecimal result = TaxCalculator.calculateCumulativeTax(null);
        Assert.assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void calculateCumulativeTax_boundaryExact_300000() {
        // 300000 × 20% - 16920 = 43080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("300000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("43080.00")));
    }

    @Test
    public void calculateCumulativeTax_boundaryExact_420000() {
        // 420000 × 25% - 31920 = 73080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("420000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("73080.00")));
    }

    @Test
    public void calculateCumulativeTax_boundaryExact_660000() {
        // 660000 × 30% - 52920 = 145080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("660000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("145080.00")));
    }

    @Test
    public void calculateCumulativeTax_boundaryExact_960000() {
        // 960000 × 35% - 85920 = 250080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("960000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("250080.00")));
    }

    @Test
    public void calculateCumulativeTax_decimalPrecision_36000_01() {
        // 36000.01 falls into 2nd bracket: 36000.01 × 10% - 2520 = 1080.001 → 1080.00
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("36000.01"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("1080.00")));
    }

    @Test
    public void calculateTaxableIncome_nullParams_shouldTreatAsZero() {
        BigDecimal result = TaxCalculator.calculateTaxableIncome(
            new BigDecimal("50000"), null, null, null);
        Assert.assertEquals(0, result.compareTo(new BigDecimal("50000.00")));
    }

    @Test
    public void calculateTaxableIncome_allNull_shouldReturnZero() {
        BigDecimal result = TaxCalculator.calculateTaxableIncome(null, null, null, null);
        Assert.assertEquals(0, result.compareTo(new BigDecimal("0.00")));
    }

    @Test
    public void calculateTaxableIncome_shouldSubtractAllDeductions() {
        BigDecimal result = TaxCalculator.calculateTaxableIncome(
            new BigDecimal("50000"),
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            new BigDecimal("3000")
        );
        Assert.assertEquals(0, result.compareTo(new BigDecimal("32000")));
    }

    @Test
    public void calculateTaxableIncome_negative_shouldReturnZero() {
        BigDecimal result = TaxCalculator.calculateTaxableIncome(
            new BigDecimal("5000"),
            new BigDecimal("10000"),
            new BigDecimal("5000"),
            new BigDecimal("3000")
        );
        Assert.assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }
}
