# computeSalaryData 渐进式重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 重构薪资核算核心方法 `computeSalaryData`，统一个税计算、封装上下文参数、分离计算与持久化、合并半路转正流程。

**Architecture:** 在现有 `SalaryMonthRecordServiceNew` 上做渐进式手术。提取公共 `TaxCalculator` 统一两套个税实现；新增 `SalaryComputeContext` 封装20个参数；`baseComputeSalary` 增加内存重载消除中间 saveBatch；计算与持久化分离为先算后存模式。

**Tech Stack:** Spring Boot 2.1.6 + MyBatis-Plus 3.5.3.2 + JUnit 4 + Java 8

---

## Task 1: 提取公共 TaxCalculator 工具类

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/modules/salary/service/TaxCalculator.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/TaxCalculatorTest.java`

### Step 1: 写失败测试 — 验证 TaxCalculator 与两套现有实现结果一致

```java
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
        // 36000 * 3% - 0 = 1080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("36000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("1080.00")));
    }

    @Test
    public void calculateCumulativeTax_secondBracket_100000() {
        // 100000 * 10% - 2520 = 7480
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("100000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("7480.00")));
    }

    @Test
    public void calculateCumulativeTax_thirdBracket_200000() {
        // 200000 * 20% - 16920 = 23080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("200000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("23080.00")));
    }

    @Test
    public void calculateCumulativeTax_fourthBracket_350000() {
        // 350000 * 25% - 31920 = 55580
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("350000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("55580.00")));
    }

    @Test
    public void calculateCumulativeTax_fifthBracket_500000() {
        // 500000 * 30% - 52920 = 97080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("500000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("97080.00")));
    }

    @Test
    public void calculateCumulativeTax_sixthBracket_800000() {
        // 800000 * 35% - 85920 = 194080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("800000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("194080.00")));
    }

    @Test
    public void calculateCumulativeTax_seventhBracket_1000000() {
        // 1000000 * 45% - 181920 = 268080
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("1000000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("268080.00")));
    }

    @Test
    public void calculateCumulativeTax_boundaryExact_144000() {
        // 144000 * 10% - 2520 = 11880
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("144000"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("11880.00")));
    }

    @Test
    public void calculateCumulativeTax_smallAmount_500() {
        // 500 * 3% - 0 = 15
        BigDecimal result = TaxCalculator.calculateCumulativeTax(new BigDecimal("500"));
        Assert.assertEquals(0, result.compareTo(new BigDecimal("15.00")));
    }

    /** 验证与 SalaryComputeServiceNew.calculateTax 结果一致 */
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
    public void calculateTaxableIncome_shouldSubtractAllDeductions() {
        BigDecimal result = TaxCalculator.calculateTaxableIncome(
            new BigDecimal("50000"),  // cumulativeIncome
            new BigDecimal("10000"),  // cumulativeDeductions
            new BigDecimal("5000"),   // cumulativeSpecialDeduction
            new BigDecimal("3000")    // cumulativeSpecialAdditionalDeduction
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
```

### Step 2: 运行测试确认失败

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=TaxCalculatorTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: 编译失败，TaxCalculator 类不存在

### Step 3: 实现 TaxCalculator

```java
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
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
```

### Step 4: 运行测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=TaxCalculatorTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: 全部 PASS

### Step 5: 提交

```bash
git add src/main/java/com/tianye/hrsystem/modules/salary/service/TaxCalculator.java \
        src/test/java/com/tianye/hrsystem/modules/salary/service/TaxCalculatorTest.java
git commit -m "feat: 提取公共TaxCalculator统一个税计算"
```

---

## Task 2: 替换两套现有个税计算为 TaxCalculator

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNew.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`（现有测试）

### Step 1: 运行现有测试确认基线通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: 全部 PASS（17个测试）

### Step 2: 替换 SalaryMonthRecordServiceNew.calculateCumulativeTaxPayable

将 `calculateCumulativeTaxPayable` 方法体改为委托 `TaxCalculator`：

```java
private static BigDecimal calculateCumulativeTaxPayable(BigDecimal cumulativeTaxableIncome) {
    return TaxCalculator.calculateCumulativeTax(cumulativeTaxableIncome);
}
```

同时可删除 `SalaryMonthRecordServiceNew` 中的 `TAXABLE_THRESHOLDS`、`TAX_RATES`、`TAX_QUICK_DEDUCTIONS` 三个数组常量（第261-286行），因为已由 `TaxCalculator` 内部维护。

### Step 3: 替换 SalaryComputeServiceNew.calculateTax 和 calculateTaxAccumulation

在 `SalaryComputeServiceNew.calculateTaxAccumulation`（第501-506行）中，将：
```java
TaxEntity taxEntity = taxRateRangeMap.get(accumulation.cumulativeTaxableIncome);
accumulation.cumulativeTaxPayable = accumulation.cumulativeTaxableIncome
    .multiply(new BigDecimal(taxEntity.getTaxRate()))
    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
    .subtract(new BigDecimal(taxEntity.getQuickDeduction()))
    .setScale(2, RoundingMode.UP);
```
替换为：
```java
accumulation.cumulativeTaxPayable = TaxCalculator.calculateCumulativeTax(accumulation.cumulativeTaxableIncome);
```

注意：`SalaryComputeServiceNew.calculateTax` 静态方法保留（因为 `TaxCalculatorTest` 中用它做交叉验证），但标记 `@Deprecated`。

### Step 4: 运行全部相关测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest,TaxCalculatorTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: 全部 PASS

### Step 5: 提交

```bash
git add -u
git commit -m "refactor: 替换两套个税计算为统一TaxCalculator"
```

---

## Task 3: 新增 SalaryComputeContext 上下文对象

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeContext.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeContextTest.java`

### Step 1: 写失败测试

```java
package com.tianye.hrsystem.modules.salary.service;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;

public class SalaryComputeContextTest {

    @Test
    public void builder_shouldCreateContextWithAllFields() {
        SalaryComputeContext ctx = SalaryComputeContext.builder()
            .year(2025).month(6)
            .isSyncInsuranceData(true)
            .isSyncAttendanceData(false)
            .attendanceDataMap(Collections.emptyMap())
            .noFixedSalaryOptionList(Collections.emptyList())
            .produceAttendanceMap(Collections.emptyMap())
            .normalDaysByDeptType(Collections.emptyMap())
            .optionParentCodeMap(Collections.emptyMap())
            .lastMonthTaxDataMap(Collections.emptyMap())
            .lastYearAccumulatedIncomeMap(Collections.emptyMap())
            .socialSecurityEmpRecordMap(Collections.emptyMap())
            .additionalDeductionMap(Collections.emptyMap())
            .midMonthArchivesOptionMap(Collections.emptyMap())
            .build();

        Assert.assertEquals(2025, ctx.getYear());
        Assert.assertEquals(6, ctx.getMonth());
        Assert.assertTrue(ctx.getIsSyncInsuranceData());
        Assert.assertFalse(ctx.getIsSyncAttendanceData());
        Assert.assertNotNull(ctx.getAttendanceDataMap());
    }

    @Test
    public void builder_nullMaps_shouldDefaultToEmpty() {
        SalaryComputeContext ctx = SalaryComputeContext.builder()
            .year(2025).month(1)
            .build();

        Assert.assertNotNull(ctx.getAttendanceDataMap());
        Assert.assertTrue(ctx.getAttendanceDataMap().isEmpty());
        Assert.assertNotNull(ctx.getLastMonthTaxDataMap());
        Assert.assertTrue(ctx.getLastMonthTaxDataMap().isEmpty());
    }
}
```

### Step 2: 运行测试确认失败

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryComputeContextTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: 编译失败

### Step 3: 实现 SalaryComputeContext

```java
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
    private Boolean isSyncInsuranceData;
    private Boolean isSyncAttendanceData;
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

    private SalaryComputeContext() {}

    // --- getters ---
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public Boolean getIsSyncInsuranceData() { return isSyncInsuranceData; }
    public Boolean getIsSyncAttendanceData() { return isSyncAttendanceData; }
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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SalaryComputeContext ctx = new SalaryComputeContext();
        public Builder year(int v) { ctx.year = v; return this; }
        public Builder month(int v) { ctx.month = v; return this; }
        public Builder isSyncInsuranceData(Boolean v) { ctx.isSyncInsuranceData = v; return this; }
        public Builder isSyncAttendanceData(Boolean v) { ctx.isSyncAttendanceData = v; return this; }
        public Builder salaryConfig(HrmSalaryConfig v) { ctx.salaryConfig = v; return this; }
        public Builder attendanceDataMap(Map<String, Map<Integer, String>> v) { ctx.attendanceDataMap = v; return this; }
        public Builder noFixedSalaryOptionList(List<HrmSalaryOption> v) { ctx.noFixedSalaryOptionList = v; return this; }
        public Builder produceAttendanceMap(Map<Long, HrmProduceAttendance> v) { ctx.produceAttendanceMap = v; return this; }
        public Builder normalDaysByDeptType(Map<Integer, Double> v) { ctx.normalDaysByDeptType = v; return this; }
        public Builder optionParentCodeMap(Map<Integer, Integer> v) { ctx.optionParentCodeMap = v; return this; }
        public Builder lastMonthTaxDataMap(Map<Long, Map<Integer, String>> v) { ctx.lastMonthTaxDataMap = v; return this; }
        public Builder lastYearAccumulatedIncomeMap(Map<Long, BigDecimal> v) { ctx.lastYearAccumulatedIncomeMap = v; return this; }
        public Builder socialSecurityEmpRecordMap(Map<Long, HrmInsuranceMonthEmpRecord> v) { ctx.socialSecurityEmpRecordMap = v; return this; }
        public Builder additionalDeductionMap(Map<Long, HrmAdditional> v) { ctx.additionalDeductionMap = v; return this; }
        public Builder midMonthArchivesOptionMap(Map<Long, List<HrmSalaryArchivesOption>> v) { ctx.midMonthArchivesOptionMap = v; return this; }
        public Builder hasAttendanceGroupMap(Map<Long, Boolean> v) { ctx.hasAttendanceGroupMap = v; return this; }

        public SalaryComputeContext build() {
            if (ctx.attendanceDataMap == null) ctx.attendanceDataMap = Collections.emptyMap();
            if (ctx.noFixedSalaryOptionList == null) ctx.noFixedSalaryOptionList = Collections.emptyList();
            if (ctx.produceAttendanceMap == null) ctx.produceAttendanceMap = Collections.emptyMap();
            if (ctx.normalDaysByDeptType == null) ctx.normalDaysByDeptType = Collections.emptyMap();
            if (ctx.optionParentCodeMap == null) ctx.optionParentCodeMap = Collections.emptyMap();
            if (ctx.lastMonthTaxDataMap == null) ctx.lastMonthTaxDataMap = Collections.emptyMap();
            if (ctx.lastYearAccumulatedIncomeMap == null) ctx.lastYearAccumulatedIncomeMap = Collections.emptyMap();
            if (ctx.socialSecurityEmpRecordMap == null) ctx.socialSecurityEmpRecordMap = Collections.emptyMap();
            if (ctx.additionalDeductionMap == null) ctx.additionalDeductionMap = Collections.emptyMap();
            if (ctx.midMonthArchivesOptionMap == null) ctx.midMonthArchivesOptionMap = Collections.emptyMap();
            if (ctx.hasAttendanceGroupMap == null) ctx.hasAttendanceGroupMap = Collections.emptyMap();
            return ctx;
        }
    }
}
```

### Step 4: 运行测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryComputeContextTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: 全部 PASS

### Step 5: 提交

```bash
git add src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeContext.java \
        src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeContextTest.java
git commit -m "feat: 新增SalaryComputeContext封装薪资计算上下文"
```

---

## Task 4: baseComputeSalary 增加内存重载

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNew.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNewTest.java`（新建）

### Step 1: 写失败测试

```java
package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import com.tianye.hrsystem.modules.salary.entity.SalaryBaseTotal;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;

public class SalaryComputeServiceNewTest {

    @Test
    public void baseComputeSalaryFromMemory_shouldPaySalary_basicCase() {
        // 模拟工资项：基本工资10101(parentCode=10,isPlus=1) + 社保100101(parentCode=100)
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "5000", 1, 1));   // 基本工资 +5000
        items.add(buildDto(10102, 10, "3000", 1, 1));   // 岗位工资 +3000
        items.add(buildDto(100101, 100, "500", 0, 1));   // 个人社保（代扣代缴）
        items.add(buildDto(100102, 100, "300", 0, 1));   // 个人公积金（代扣代缴）
        items.add(buildDto(280, 280, "200", 0, 0));      // 其他扣款
        items.add(buildDto(282, 282, "100", 0, 0));      // 借款

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("8000")));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(new BigDecimal("800")));
        Assert.assertEquals(0, result.getOtherNoTaxDeductions().compareTo(new BigDecimal("200")));
        Assert.assertEquals(0, result.getTotalloanMoney().compareTo(new BigDecimal("100")));
    }

    @Test
    public void baseComputeSalaryFromMemory_withSubtractItem_shouldDeduct() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "5000", 1, 1));
        items.add(buildDto(180101, 180, "300", 0, 1));   // 考勤扣款(isPlus=0)

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("4700")));
    }

    @Test
    public void baseComputeSalaryFromMemory_emptyList_shouldReturnZeros() {
        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(
            Collections.emptyList(), null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(BigDecimal.ZERO));
    }

    private ComputeSalaryDto buildDto(int code, int parentCode, String value, int isPlus, int isTax) {
        ComputeSalaryDto dto = new ComputeSalaryDto();
        dto.setCode(code);
        dto.setParentCode(parentCode);
        dto.setValue(value);
        dto.setIsPlus(isPlus);
        dto.setIsTax(isTax);
        return dto;
    }
}
```

### Step 2: 运行测试确认失败

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryComputeServiceNewTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: 编译失败，`baseComputeSalaryFromMemory` 不存在

### Step 3: 实现 baseComputeSalaryFromMemory

在 `SalaryComputeServiceNew` 中新增静态方法（与现有 `baseComputeSalary` 逻辑一致，但从内存 List 读取）：

```java
/**
 * 从内存中的工资项列表计算 SalaryBaseTotal，不依赖DB查询。
 * 逻辑与 baseComputeSalary 完全一致。
 * @param items 工资项列表（等价于 queryEmpSalaryOptionValueList 的返回值）
 * @param companyId 公司ID（用于成都奖金特殊处理），可为null
 */
public static SalaryBaseTotal baseComputeSalaryFromMemory(List<ComputeSalaryDto> items, String companyId) {
    SalaryBaseTotal total = new SalaryBaseTotal();
    if (items == null || items.isEmpty()) {
        return total;
    }
    List<Integer> shouldPayCodeList = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 130, 140, 180, 200);
    BigDecimal shouldPaySalary = BigDecimal.ZERO;
    BigDecimal proxyPaySalary = BigDecimal.ZERO;
    BigDecimal specialTaxSalary = BigDecimal.ZERO;
    BigDecimal taxAfterPaySalary = BigDecimal.ZERO;
    BigDecimal taxSpecialGrandTotal = BigDecimal.ZERO;
    BigDecimal otherNoTaxDeductions = BigDecimal.ZERO;
    BigDecimal loanMoney = BigDecimal.ZERO;

    for (ComputeSalaryDto dto : items) {
        BigDecimal val = new BigDecimal(dto.getValue());
        if (dto.getParentCode().equals(100) && !dto.getCode().equals(1001)) {
            proxyPaySalary = proxyPaySalary.add(val);
        }
        if (dto.getParentCode().equals(170)) {
            specialTaxSalary = specialTaxSalary.add(val);
        }
        if (dto.getParentCode().equals(150)) {
            taxAfterPaySalary = taxAfterPaySalary.add(val);
        }
        if (dto.getParentCode().equals(260)) {
            taxSpecialGrandTotal = taxSpecialGrandTotal.add(val);
        }
        if (dto.getCode().equals(280)) {
            otherNoTaxDeductions = otherNoTaxDeductions.add(val);
        }
        if (dto.getCode().equals(282)) {
            loanMoney = loanMoney.add(val);
        }
        if (dto.getCode().equals(281)) {
            shouldPaySalary = shouldPaySalary.add(val);
        }
        if ("0002".equals(companyId) && dto.getCode().equals(41001)) {
            shouldPaySalary = shouldPaySalary.add(val);
        }
        if (shouldPayCodeList.contains(dto.getParentCode())) {
            if (dto.getIsPlus() == 1) {
                shouldPaySalary = shouldPaySalary.add(val);
            } else if (dto.getIsPlus() == 0) {
                shouldPaySalary = shouldPaySalary.subtract(val);
            }
        }
    }
    total.setShouldPaySalary(shouldPaySalary);
    total.setProxyPaySalary(proxyPaySalary);
    total.setSpecialTaxSalary(specialTaxSalary);
    total.setTaxAfterPaySalary(taxAfterPaySalary);
    total.setTaxSpecialGrandTotal(taxSpecialGrandTotal);
    total.setOtherNoTaxDeductions(otherNoTaxDeductions);
    total.setTotalloanMoney(loanMoney);
    return total;
}
```

### Step 4: 运行测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryComputeServiceNewTest,TaxCalculatorTest,SalaryMonthRecordServiceNewTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: 全部 PASS

### Step 5: 提交

```bash
git add -A
git commit -m "feat: baseComputeSalary增加内存重载baseComputeSalaryFromMemory"
```

---

## Task 5: 新增 EmployeeSalaryResult 封装单员工计算结果

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/modules/salary/service/EmployeeSalaryResult.java`

### Step 1: 实现结果封装类

用于"先算后存"模式，每个员工计算完后返回此对象，最后统一持久化。

```java
package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthEmpRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthOptionValue;
import java.util.List;

/**
 * 单员工薪资计算结果，用于先算后存模式。
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
        this.empRecord = empRecord;
        this.baseOptions = baseOptions;
        this.finalOptions = finalOptions;
        this.existed = existed;
    }

    public HrmSalaryMonthEmpRecord getEmpRecord() { return empRecord; }
    public List<HrmSalaryMonthOptionValue> getBaseOptions() { return baseOptions; }
    public List<HrmSalaryMonthOptionValue> getFinalOptions() { return finalOptions; }
    public boolean isExisted() { return existed; }
}
```

### Step 2: 提交

```bash
git add src/main/java/com/tianye/hrsystem/modules/salary/service/EmployeeSalaryResult.java
git commit -m "feat: 新增EmployeeSalaryResult封装单员工计算结果"
```

---

## Task 6: 重构 doComputeSalaryData — 使用 Context + 先算后存

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`

### Step 1: 在 doComputeSalaryData 中构建 SalaryComputeContext

将第1050-1068行的批量加载数据封装到 context：

```java
SalaryComputeContext ctx = SalaryComputeContext.builder()
    .year(year).month(month)
    .isSyncInsuranceData(isSyncInsuranceData)
    .isSyncAttendanceData(isSyncAttendanceData)
    .salaryConfig(salaryConfig)
    .attendanceDataMap(attendanceDataMap)
    .noFixedSalaryOptionList(noFixedSalaryOptionList)
    .produceAttendanceMap(produceAttendanceMap)
    .normalDaysByDeptType(normalDaysByDeptType)
    .optionParentCodeMap(optionParentCodeMap)
    .lastMonthTaxDataMap(lastMonthTaxDataMap)
    .lastYearAccumulatedIncomeMap(lastYearAccumulatedIncomeMap)
    .socialSecurityEmpRecordMap(socialSecurityEmpRecordMap)
    .additionalDeductionMap(additionalDeductionMap)
    .midMonthArchivesOptionMap(midMonthArchivesOptionMap)
    .hasAttendanceGroupMap(hasAttendanceGroupMap)
    .build();
```

### Step 2: 改造循环为先算后存

将原来的：
```java
for (Map<String, Object> map : mapList) {
    computeAndSaveEmployeeSalary(map, sRecordId, salaryMonthRecord, ...20个参数...);
}
```

改为：
```java
List<EmployeeSalaryResult> results = new ArrayList<>(mapList.size());
Set<String> midMonthPromotionReviewSet = new LinkedHashSet<>();

for (Map<String, Object> map : mapList) {
    Long currentEmployeeId = Convert.toLong(map.get("employeeId"));
    boolean hasAttendanceGroup = currentEmployeeId != null
        && Boolean.TRUE.equals(ctx.getHasAttendanceGroupMap().get(currentEmployeeId));
    EmployeeSalaryResult result = computeEmployeeSalary(
        map, sRecordId, salaryMonthRecord, ctx, hasAttendanceGroup, midMonthPromotionReviewSet);
    if (result != null) {
        results.add(result);
    }
}

// 统一批量持久化
batchSaveResults(results);
```

### Step 3: 新增 computeEmployeeSalary（纯计算，不持久化）

将现有 `computeAndSaveEmployeeSalary` 复制为 `computeEmployeeSalary`，做以下改动：
1. 参数从20个改为 `(Map<String, Object> map, Long sRecordId, HrmSalaryMonthRecord salaryMonthRecord, SalaryComputeContext ctx, boolean hasAttendanceGroup, Set<String> midMonthPromotionReviewSet)`
2. 从 ctx 中读取所有批量数据
3. 移除两处 `salaryMonthOptionValueService.saveBatch()`
4. 移除 `salaryMonthEmpRecordService.updateById()`
5. 将 `computeSalary` 调用改为使用内存重载（见 Task 7）
6. 返回 `EmployeeSalaryResult`

### Step 4: 新增 batchSaveResults 方法

```java
private void batchSaveResults(List<EmployeeSalaryResult> results) {
    if (CollUtil.isEmpty(results)) return;
    List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
    List<HrmSalaryMonthEmpRecord> recordsToUpdate = new ArrayList<>();
    for (EmployeeSalaryResult r : results) {
        if (r.getBaseOptions() != null) allOptions.addAll(r.getBaseOptions());
        if (r.getFinalOptions() != null) allOptions.addAll(r.getFinalOptions());
        recordsToUpdate.add(r.getEmpRecord());
    }
    if (CollUtil.isNotEmpty(allOptions)) {
        salaryMonthOptionValueService.saveBatch(allOptions);
    }
    if (CollUtil.isNotEmpty(recordsToUpdate)) {
        salaryMonthEmpRecordService.updateBatchById(recordsToUpdate);
    }
}
```

### Step 5: 运行全部测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest,TaxCalculatorTest,SalaryComputeContextTest,SalaryComputeServiceNewTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: 全部 PASS

### Step 6: 提交

```bash
git add -u
git commit -m "refactor: doComputeSalaryData使用Context+先算后存模式"
```

---

## Task 7: 重构 computeEmployeeSalary — 使用内存计算替代中间 saveBatch

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNew.java`

### Step 1: 新增 computeSalaryFromMemory 方法

在 `SalaryMonthRecordServiceNew` 中新增方法，替代原来的 `computeSalary`（原方法先 saveBatch 到DB，再由 `SalaryComputeServiceNew.baseComputeSalary` 从DB读取）：

```java
/**
 * 基于内存中的工资项列表计算个税和实发工资，不依赖DB中间状态。
 * 替代原来的 computeSalary → baseComputeSalary(从DB读) 流程。
 */
private List<HrmSalaryMonthOptionValue> computeSalaryFromMemory(
        HrmSalaryMonthEmpRecord salaryMonthEmpRecord,
        List<HrmSalaryMonthOptionValue> optionValueList,
        Map<Integer, String> cumulativeTaxOfLastMonthData,
        HrmEmployeeVO hrmEmployeeVO,
        String isDisabled) {
    // 将 optionValueList 转换为 ComputeSalaryDto 列表
    List<ComputeSalaryDto> dtoList = convertToComputeSalaryDtoList(optionValueList);
    LoginUserInfo info = CompanyContext.get();
    String companyId = info != null ? info.getCompanyId() : null;
    SalaryBaseTotal salaryBaseTotal = SalaryComputeServiceNew.baseComputeSalaryFromMemory(dtoList, companyId);

    HrmSalaryTaxRule hrmSalaryTaxRule = new HrmSalaryTaxRule();
    hrmSalaryTaxRule.setIsTax(1);
    hrmSalaryTaxRule.setCycleType(1);
    hrmSalaryTaxRule.setMarkingPoint(5000);
    hrmSalaryTaxRule.setTaxType(1);

    return salaryComputeService.computeSalary(
        salaryBaseTotal, salaryMonthEmpRecord, hrmSalaryTaxRule,
        cumulativeTaxOfLastMonthData, hrmEmployeeVO, isDisabled);
}
```

### Step 2: 新增 convertToComputeSalaryDtoList 辅助方法

```java
/**
 * 将内存中的 HrmSalaryMonthOptionValue 列表转换为 ComputeSalaryDto 列表。
 * 需要查询 parentCode 和 isPlus 信息（从 optionParentCodeMap 获取）。
 */
private List<ComputeSalaryDto> convertToComputeSalaryDtoList(
        List<HrmSalaryMonthOptionValue> optionValueList) {
    if (CollUtil.isEmpty(optionValueList)) {
        return Collections.emptyList();
    }
    // 查询所有工资项配置（含 parentCode, isPlus, isTax 等）
    Map<Integer, HrmSalaryOption> optionConfigMap = hrmSalaryOptionService.lambdaQuery()
        .ne(HrmSalaryOption::getParentCode, 0).list().stream()
        .filter(o -> o.getCode() != null)
        .collect(Collectors.toMap(HrmSalaryOption::getCode, Function.identity(), (a, b) -> a));

    List<ComputeSalaryDto> result = new ArrayList<>(optionValueList.size());
    for (HrmSalaryMonthOptionValue ov : optionValueList) {
        if (ov == null || ov.getCode() == null) continue;
        HrmSalaryOption config = optionConfigMap.get(ov.getCode());
        ComputeSalaryDto dto = new ComputeSalaryDto();
        dto.setCode(ov.getCode());
        dto.setValue(ov.getValue() != null ? ov.getValue() : "0");
        if (config != null) {
            dto.setParentCode(config.getParentCode());
            dto.setIsPlus(config.getIsPlus());
            dto.setIsTax(config.getIsTax());
        } else {
            dto.setParentCode(0);
            dto.setIsPlus(1);
            dto.setIsTax(0);
        }
        result.add(dto);
    }
    return result;
}
```

### Step 3: 在 computeEmployeeSalary 中使用 computeSalaryFromMemory

将原来的：
```java
salaryMonthOptionValueService.saveBatch(options);  // 先保存到DB
// ...
List<HrmSalaryMonthOptionValue> finalOptions = computeSalary(ro.record, lastMonthTaxData, employeeVO, isDisabled);
```

改为：
```java
// 不再 saveBatch，直接从内存计算
List<HrmSalaryMonthOptionValue> finalOptions = computeSalaryFromMemory(
    ro.record, options, lastMonthTaxData, employeeVO, isDisabled);
```

### Step 4: 运行全部测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest,TaxCalculatorTest,SalaryComputeServiceNewTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: 全部 PASS

### Step 5: 提交

```bash
git add -u
git commit -m "refactor: computeEmployeeSalary使用内存计算替代中间saveBatch"
```

---

## Task 8: 合并半路转正两次调用为一次流程

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`

### Step 1: 写测试 — 验证合并后半路转正结果不变

```java
@Test
public void computeEmployeeSalary_midMonthPromotion_shouldMatchOriginalResult() {
    // 构造半路转正员工数据：转正日期在月中（如6月15日）
    // 基本工资试用期3000，正式5000；岗位工资试用期1500，正式2000
    // 社保500，公积金300，无其他扣款
    // 验证：应发工资、个税、实发工资与原流程一致

    Map<Integer, String> lastTaxMap = new HashMap<>();
    lastTaxMap.put(250101, "0");
    lastTaxMap.put(250102, "0");
    lastTaxMap.put(250103, "0");
    lastTaxMap.put(250105, "0");

    // 模拟半路转正计算结果：应发=6000
    Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSummary(
        new BigDecimal("6000"),   // shouldPaySalary
        new BigDecimal("800"),    // proxyPaySalary (500+300)
        BigDecimal.ZERO,          // otherDeductions
        BigDecimal.ZERO,          // loanMoney
        BigDecimal.ZERO,          // taxAfterPaySalary
        BigDecimal.ZERO,          // specialTaxSalary
        BigDecimal.ZERO,          // taxSpecialAdditionalDeduction
        BigDecimal.ZERO,          // labourUnionPay
        BigDecimal.ZERO,          // bonusSalary
        false,                    // includeBonusInCumulativeIncome
        true,                     // isDisabledNo (正常计税)
        false,                    // skipTaxForRemark
        lastTaxMap,
        6                         // month
    );

    // 应税 = 6000 + 0 - 800 - 5000 = 200, 税 = 200 * 3% = 6.00
    Assert.assertEquals(0, new BigDecimal(result.get(220101)).compareTo(new BigDecimal("200")));
    Assert.assertEquals(0, new BigDecimal(result.get(230101)).compareTo(new BigDecimal("6.00")));
    // 实发 = 6000 - 800 - 6 = 5194.00
    Assert.assertEquals(0, new BigDecimal(result.get(240101)).compareTo(new BigDecimal("5194.00")));
}
```

### Step 2: 运行测试确认通过（基线）

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -Dsurefire.useFile=false 2>&1 | tail -20`
Expected: PASS

### Step 3: 合并 applyMidMonthPromotionAndUnionFee 的两次调用

在 `computeEmployeeSalary` 中，原来的流程是：

```java
// 第一次：removeThenPromotion=true → 只移除全勤奖和工会费
applyMidMonthPromotionAndUnionFee(..., true, ...);
// saveBatch(options) — 已移除
// computeSalary — 现在从内存计算
// 第二次：removeThenPromotion=false → 执行半路转正薪资计算
applyMidMonthPromotionAndUnionFee(..., false, ...);
// applyMidMonthPromotionSummaryConsistency
```

合并为单次流程：

```java
// 1. 移除全勤奖和工会费（半路转正员工不享受）
if (isMidMonthPromotion(becomeDate, year, month)) {
    removeFullAttendanceAndUnionFeeForMidMonthPromotion(year, month, options, becomeDate);
}

// 2. 从内存计算个税（基于已移除全勤/工会费后的 options）
List<HrmSalaryMonthOptionValue> finalOptions = computeSalaryFromMemory(
    ro.record, options, lastMonthTaxData, employeeVO, isDisabled);

// 3. 半路转正薪资计算 + 一致性校验（一次完成）
if (isMidMonthPromotion(becomeDate, year, month)) {
    processMidMonthPromotionSalary(employeeId, year, month, finalOptions, deptType,
        becomeDate, midMonthAttendance, normalDays, lastMonthTaxData,
        baseOptionMap, isDisabled, skipTaxForRemark, taxSpecialAdditionalDeduction,
        ctx.getMidMonthArchivesOptionMap());
    applyMidMonthPromotionSummaryConsistency(map, ro.record, finalOptions, baseOptionMap,
        lastMonthTaxData, isDisabled, year, month, becomeDate,
        ctx.getOptionParentCodeMap(), ctx.getLastYearAccumulatedIncomeMap());
}
```

### Step 4: 删除 applyMidMonthPromotionAndUnionFee 方法

该方法（第1542-1561行）不再需要，其逻辑已内联到 `computeEmployeeSalary` 中。

### Step 5: 运行全部测试确认通过

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest,TaxCalculatorTest,SalaryComputeServiceNewTest,SalaryComputeContextTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: 全部 PASS

### Step 6: 提交

```bash
git add -u
git commit -m "refactor: 合并半路转正两次调用为一次流程"
```

---

## Task 9: 清理冗余代码 + 最终验证

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNew.java`

### Step 1: 清理 SalaryMonthRecordServiceNew

1. 删除不再使用的 `TAXABLE_THRESHOLDS`、`TAX_RATES`、`TAX_QUICK_DEDUCTIONS` 数组常量（如果 Task 2 中未删除）
2. 删除旧的 `computeAndSaveEmployeeSalary` 方法（已被 `computeEmployeeSalary` 替代）
3. 删除 `applyMidMonthPromotionAndUnionFee` 方法（已内联）
4. 确认 `calculateCumulativeTaxPayable` 委托到 `TaxCalculator`

### Step 2: 清理 SalaryComputeServiceNew

1. 将 `calculateTax` 标记为 `@Deprecated`，注释指向 `TaxCalculator.calculateCumulativeTax`
2. `taxRateRangeMap` 静态块保留（`calculateTax` 仍被引用做交叉验证），但添加注释说明已废弃

### Step 3: 运行全部测试做最终验证

Run: `cd /Users/jiangyongming/Project/hr/hainan && mvn test -pl . -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: 全部 PASS

### Step 4: 提交

```bash
git add -u
git commit -m "refactor: 清理冗余代码完成computeSalaryData重构"
```

---

## 实施顺序总结

| Task | 内容 | 依赖 | 风险 |
|------|------|------|------|
| 1 | 提取 TaxCalculator | 无 | 低 |
| 2 | 替换两套个税为 TaxCalculator | Task 1 | 中（需验证结果一致） |
| 3 | 新增 SalaryComputeContext | 无 | 低 |
| 4 | baseComputeSalary 内存重载 | 无 | 低 |
| 5 | 新增 EmployeeSalaryResult | 无 | 低 |
| 6 | doComputeSalaryData 使用 Context + 先算后存 | Task 3, 5 | 中 |
| 7 | computeEmployeeSalary 内存计算 | Task 4, 6 | 高（核心改动） |
| 8 | 合并半路转正流程 | Task 7 | 高（需仔细验证） |
| 9 | 清理冗余代码 | Task 1-8 | 低 |

Task 1/3/4/5 互相独立，可并行执行。Task 6 依赖 3+5，Task 7 依赖 4+6，Task 8 依赖 7，Task 9 最后执行。
