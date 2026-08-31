# computeSalaryData 薪资核算核心方法重构实施方案

## 1. 项目概述

### 1.1 背景

`computeSalaryData` 是月薪计算主入口，覆盖：员工筛选、考勤同步、薪资项装配、个税计算、实发计算、结果落库。原实现存在以下核心问题：

- 两套独立的个税计算逻辑（`SalaryComputeServiceNew.calculateTax` 与 `SalaryMonthRecordServiceNew.calculateCumulativeTaxPayable`），维护成本高且易分叉
- 核心方法 `computeAndSaveEmployeeSalary` 参数多达 20 个，可读性差
- 计算与持久化耦合，每个员工计算过程中多次 `saveBatch`/`updateById`
- 半路转正流程分两次调用，数据来源不完整导致扣款项丢失
- 多处 N+1 查询热点，大规模员工场景下性能线性恶化

### 1.2 目标

1. 只做优化，不改薪资算法口径
2. 重点审查并修正半路转正路径
3. 兼顾简洁性、健壮性、可复用性、性能治理
4. 采用 TDD 流程，每步独立提交，保证可回滚

### 1.3 技术栈

Spring Boot 2.1.6 + MyBatis-Plus 3.5.3.2 + MySQL 8.0.11 + JUnit 4 + Java 1.8

---

## 2. 整体架构设计

### 2.1 设计原则

| 原则 | 说明 |
|------|------|
| 不改算法口径 | 保留现有税率、扣减、rounding 与公司特例 |
| 不改业务结果定义 | 半路转正仍按既有业务定义计算 |
| 先修正确性再做性能 | 先消除错误和分叉，再优化查询 |
| 渐进式重构 | 每步独立提交，保证可回滚 |

### 2.2 方案选型

经评审选定方案 A（低风险渐进式重构），核心改造：

1. 提取公共 `TaxCalculator` 统一两套个税实现
2. 新增 `SalaryComputeContext` 封装 20 个参数
3. `baseComputeSalary` 增加内存重载消除中间 saveBatch
4. 计算与持久化分离为先算后存模式
5. 半路转正流程合并为单次内联流程

---

## 3. 实施步骤

### 3.1 步骤总览

| 步骤 | 内容 | 提交 | 依赖 | 风险 |
|------|------|------|------|------|
| 1 | 提取公共 TaxCalculator 工具类 | `4ad279e` | 无 | 低 |
| 2 | 新增 SalaryComputeContext 上下文对象 | `d4c2ef3` | 无 | 低 |
| 3 | baseComputeSalary 增加内存重载 | `73a1a46` | 无 | 低 |
| 4 | 新增 EmployeeSalaryResult 封装类 | `26be9f4` | 无 | 低 |
| 5 | 替换两套个税计算为 TaxCalculator | `bb6d732` | 步骤1 | 中 |
| 6 | doComputeSalaryData 使用 Context + 先算后存 | `fd626d1` | 步骤2,4 | 中 |
| 7 | computeEmployeeSalary 使用内存计算 | `041f4c8` | 步骤3,6 | 高 |
| 8 | 合并半路转正两次调用为内联流程 | `01329ec` | 步骤7 | 高 |
| 9 | 清理冗余代码 | `a9c17c5` | 步骤1-8 | 低 |
| 10 | 交叉审查 P0/P1 修复 | `86756a6` | 步骤9 | 中 |
| 11 | 交叉审查 P2/P3 修复 | `36cfdc1` | 步骤10 | 低 |

步骤 1/2/3/4 互相独立可并行，步骤 6 依赖 2+4，步骤 7 依赖 3+6，步骤 8 依赖 7，步骤 9 最后执行。

---

### 3.2 步骤 1：提取公共 TaxCalculator 工具类

**目标：** 统一七级超额累进税率计算，替代两套独立实现。

**新增文件：**
- `src/main/java/.../salary/service/TaxCalculator.java`
- `src/test/java/.../salary/service/TaxCalculatorTest.java`

**核心实现：**

```java
public final class TaxCalculator {
    private static final BigDecimal[] THRESHOLDS = {
        new BigDecimal("36000"), new BigDecimal("144000"), new BigDecimal("300000"),
        new BigDecimal("420000"), new BigDecimal("660000"), new BigDecimal("960000")
    };
    private static final BigDecimal[] RATES = {
        new BigDecimal("0.03"), new BigDecimal("0.10"), new BigDecimal("0.20"),
        new BigDecimal("0.25"), new BigDecimal("0.30"), new BigDecimal("0.35"),
        new BigDecimal("0.45")
    };
    private static final BigDecimal[] QUICK_DEDUCTIONS = { ... };

    // 静态校验数组长度一致性
    static {
        if (RATES.length != QUICK_DEDUCTIONS.length
                || THRESHOLDS.length != RATES.length - 1) {
            throw new ExceptionInInitializerError("税率表数组长度不一致");
        }
    }

    // 计算累计应纳税额
    public static BigDecimal calculateCumulativeTax(BigDecimal cumulativeTaxableIncome);

    // 计算累计应纳税所得额
    public static BigDecimal calculateTaxableIncome(
        BigDecimal cumulativeIncome, BigDecimal cumulativeDeductions,
        BigDecimal cumulativeSpecialDeduction,
        BigDecimal cumulativeSpecialAdditionalDeduction);
}
```

**测试覆盖（22 个用例）：**
- 零值、负值、null 输入 → 返回 0
- 七级税率每档代表值验证
- 全部 6 个边界精确值（36000/144000/300000/420000/660000/960000）
- 小数精度验证（36000.01 跨档）
- 与旧 `SalaryComputeServiceNew.calculateTax` 交叉验证
- `calculateTaxableIncome` null 参数、全 null、正常扣减、负值归零

---

### 3.3 步骤 2：新增 SalaryComputeContext 上下文对象

**目标：** 封装 `computeAndSaveEmployeeSalary` 的 20 个参数为 Builder 模式上下文。

**新增文件：**
- `src/main/java/.../salary/service/SalaryComputeContext.java`
- `src/test/java/.../salary/service/SalaryComputeContextTest.java`

**核心设计：**

```java
public class SalaryComputeContext {
    private int year;
    private int month;
    private boolean isSyncInsuranceData;   // primitive boolean，避免 NPE
    private boolean isSyncAttendanceData;
    private HrmSalaryConfig salaryConfig;
    private Map<String, Map<Integer, String>> attendanceDataMap;
    private Map<Long, Map<Integer, String>> lastMonthTaxDataMap;
    private Map<Long, BigDecimal> lastYearAccumulatedIncomeMap;
    // ... 共 16 个字段

    private SalaryComputeContext() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        // Builder 使用独立字段，build() 时拷贝到新 Context
        // null Map/List 默认为 Collections.emptyMap()/emptyList()
        public SalaryComputeContext build() { ... }
    }
}
```

**测试覆盖（3 个用例）：**
- 全字段构建验证
- null Map/List 默认空集合（覆盖全部 12 个集合字段）
- Builder 复用独立性验证

---

### 3.4 步骤 3：baseComputeSalary 增加内存重载

**目标：** 新增 `baseComputeSalaryFromMemory` 静态方法，从内存 List 计算应发/代扣/扣款等汇总，不依赖 DB 查询。

**修改文件：**
- `src/main/java/.../salary/service/SalaryComputeServiceNew.java`
- `src/test/java/.../salary/service/SalaryComputeServiceNewTest.java`

**核心实现：**

```java
public static SalaryBaseTotal baseComputeSalaryFromMemory(
        List<ComputeSalaryDto> items, String companyId) {
    // 逻辑与 baseComputeSalary 完全一致
    // 使用 safeParseDecimal 防御非数字输入
    for (ComputeSalaryDto dto : items) {
        BigDecimal val = safeParseDecimal(dto.getValue());
        // ... 按 parentCode/code 分类累加
    }
}

private static BigDecimal safeParseDecimal(String value) {
    if (StrUtil.isBlank(value)) return BigDecimal.ZERO;
    try {
        return new BigDecimal(value.trim());
    } catch (NumberFormatException e) {
        return BigDecimal.ZERO;
    }
}
```

**测试覆盖（6 个用例）：**
- 基本应发工资计算
- 减项扣除验证
- 空列表 / null 列表 → 返回零值
- null 值 / 非数字值 → 安全降级为 0

---

### 3.5 步骤 4：新增 EmployeeSalaryResult 封装类

**目标：** 封装单员工计算结果，用于先算后存模式。

**新增文件：**
- `src/main/java/.../salary/service/EmployeeSalaryResult.java`

**核心设计：**

```java
public class EmployeeSalaryResult {
    private final HrmSalaryMonthEmpRecord empRecord;       // 非空，构造时校验
    private final List<HrmSalaryMonthOptionValue> baseOptions;   // 防御性拷贝，不可变
    private final List<HrmSalaryMonthOptionValue> finalOptions;  // 防御性拷贝，不可变
    private final boolean existed;  // 区分新增 vs 已有记录

    public EmployeeSalaryResult(...) {
        this.empRecord = Objects.requireNonNull(empRecord);
        this.baseOptions = baseOptions != null
            ? Collections.unmodifiableList(new ArrayList<>(baseOptions))
            : Collections.emptyList();
        // ...
    }
}
```

---

### 3.6 步骤 5：替换两套个税计算为 TaxCalculator

**目标：** 消除个税计算分叉，统一使用 `TaxCalculator`。

**修改文件：**
- `SalaryMonthRecordServiceNew.java` — `calculateCumulativeTaxPayable` 委托到 `TaxCalculator`
- `SalaryComputeServiceNew.java` — `calculateTaxAccumulation` 使用 `TaxCalculator`

**改动要点：**
1. `calculateCumulativeTaxPayable` → 委托 `TaxCalculator.calculateCumulativeTax`
2. `calculateTaxAccumulation` → 替换手工税率查表为 `TaxCalculator.calculateCumulativeTax`
3. 旧 `calculateTax`、`taxRateRangeMap` 等标记 `@Deprecated`，保留用于交叉验证
4. 删除 `SalaryMonthRecordServiceNew` 中的 `TAXABLE_THRESHOLDS`/`TAX_RATES`/`TAX_QUICK_DEDUCTIONS` 数组

---

### 3.7 步骤 6：doComputeSalaryData 使用 Context + 先算后存

**目标：** 主流程改造为构建 Context → 循环计算 → 统一批量持久化。

**修改文件：**
- `SalaryMonthRecordServiceNew.java`

**改造前：**
```java
for (Map<String, Object> map : mapList) {
    computeAndSaveEmployeeSalary(map, sRecordId, salaryMonthRecord,
        ...20个参数...);  // 每个员工内部多次 saveBatch/updateById
}
```

**改造后：**
```java
SalaryComputeContext ctx = SalaryComputeContext.builder()
    .year(year).month(month)
    .salaryConfig(salaryConfig)
    .attendanceDataMap(attendanceDataMap)
    // ... 批量预加载数据
    .build();

List<EmployeeSalaryResult> results = new ArrayList<>();
for (Map<String, Object> map : mapList) {
    EmployeeSalaryResult result = computeEmployeeSalary(map, sRecordId,
        salaryMonthRecord, ctx, hasAttendanceGroup);
    if (result != null) results.add(result);
}
batchSaveResults(results);  // 统一批量持久化
```

**batchSaveResults 实现：**
```java
private void batchSaveResults(List<EmployeeSalaryResult> results) {
    List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
    List<HrmSalaryMonthEmpRecord> toUpdate = new ArrayList<>();
    List<HrmSalaryMonthEmpRecord> toInsert = new ArrayList<>();
    for (EmployeeSalaryResult r : results) {
        allOptions.addAll(r.getBaseOptions());
        allOptions.addAll(r.getFinalOptions());
        if (r.isExisted()) toUpdate.add(r.getEmpRecord());
        else toInsert.add(r.getEmpRecord());
    }
    if (!allOptions.isEmpty()) salaryMonthOptionValueService.saveBatch(allOptions);
    if (!toUpdate.isEmpty()) salaryMonthEmpRecordService.updateBatchById(toUpdate);
    if (!toInsert.isEmpty()) salaryMonthEmpRecordService.saveBatch(toInsert);
}
```

---

### 3.8 步骤 7：computeEmployeeSalary 使用内存计算

**目标：** 单员工计算过程不再依赖 DB 中间状态。

**核心改动：**
1. 移除两处 `salaryMonthOptionValueService.saveBatch()`
2. 移除 `salaryMonthEmpRecordService.updateById()`
3. 新增 `convertToComputeSalaryDtoList` 将内存 OptionValue 转为 ComputeSalaryDto
4. 使用 `baseComputeSalaryFromMemory` 替代从 DB 读取的 `baseComputeSalary`
5. 返回 `EmployeeSalaryResult` 而非直接持久化

**convertToComputeSalaryDtoList 实现：**
```java
private static List<ComputeSalaryDto> convertToComputeSalaryDtoList(
        List<HrmSalaryMonthOptionValue> optionValueList,
        Map<Integer, HrmSalaryOption> optionConfigMap) {
    // 使用预加载的 salaryOptionConfigMap 获取 parentCode/isPlus/isTax
    // 未找到配置时输出 warn 日志，默认加项、不参与计税
    for (HrmSalaryMonthOptionValue ov : optionValueList) {
        HrmSalaryOption config = optionConfigMap.get(ov.getCode());
        if (config != null) {
            dto.setParentCode(config.getParentCode());
            // ...
        } else {
            logger.warn("工资项配置缺失, code={}", ov.getCode());
            // 默认值兜底
        }
    }
}
```

---

### 3.9 步骤 8：合并半路转正流程

**目标：** 将半路转正的两次分散调用合并为单次内联流程。

**改造前（两次分散调用）：**
```
第一次 removeFullAttendanceAndUnionFee → saveBatch → computeSalary
第二次 processMidMonthPromotionSalary → removeFullAttendanceAndUnionFee → applyConsistency
```

**改造后（单次内联流程）：**
```java
// 1. 半路转正：移除全勤奖和工会费（在计算个税前）
removeFullAttendanceAndUnionFeeForMidMonthPromotion(year, month, options, becomeDate);

// 2. 从内存计算个税（基于已清理后的 options）
List<HrmSalaryMonthOptionValue> finalOptions = computeSalaryFromMemory(...);

// 3. 半路转正：按日比例拆分薪资 + 再次移除 + 一致性校验
if (isMidMonthPromotion(becomeDate, year, month)) {
    processMidMonthPromotionSalary(...);
    // 第二次移除：processMidMonthPromotionSalary 可能重新写入全勤奖/工会费
    removeFullAttendanceAndUnionFeeForMidMonthPromotion(year, month, finalOptions, becomeDate);
    applyMidMonthPromotionSummaryConsistency(...);
}
```

---

### 3.10 步骤 9：清理冗余代码

- 删除旧 `computeAndSaveEmployeeSalary` 方法
- 删除 `applyMidMonthPromotionAndUnionFee` 方法（已内联）
- 删除 `TAXABLE_THRESHOLDS`/`TAX_RATES`/`TAX_QUICK_DEDUCTIONS` 数组常量
- 旧 `calculateTax`/`taxRateRangeMap` 等标记 `@Deprecated`

---

### 3.11 步骤 10-11：交叉审查修复

由三个独立审查子任务分别审查：
1. 新工具类（TaxCalculator、SalaryComputeContext、EmployeeSalaryResult）
2. SalaryMonthRecordServiceNew 核心重构
3. SalaryComputeServiceNew 变更

**P0/P1 修复（步骤 10）：**

| 编号 | 问题 | 修复 |
|------|------|------|
| P0-1 | SalaryComputeContext Builder 共享实例导致复用不安全 | Builder 改为独立字段，build() 时拷贝到新 Context |
| P0-2 | batchSaveResults 未区分新增/更新记录 | 按 `r.isExisted()` 分流 saveBatch/updateBatchById |
| P0-3 | Boolean 拆箱 NPE 风险 | SalaryComputeContext 改为 primitive boolean |
| P1-1 | parseAmount 缺少 NumberFormatException 防御 | 添加 try-catch 返回 ZERO |
| P1-2 | getOrCreateRecordAndApplyAttendance 中 updateById 未延迟 | 添加注释说明延迟到 batchSaveResults |

**P2/P3 修复（步骤 11）：**

| 编号 | 问题 | 修复 |
|------|------|------|
| P2-1 | Logger 指向错误类 | 改为 `static final Logger` 指向 `SalaryMonthRecordServiceNew.class` |
| P2-2 | convertToComputeSalaryDtoList 缺失配置无日志 | 添加 `logger.warn("工资项配置缺失, code={}")` |
| P2-3 | amountByCode 直接 `new BigDecimal` 无防御 | 委托到已有的 `parseAmount` 方法 |
| P2-4 | baseComputeSalaryFromMemory 非数字输入 NFE | 新增 `safeParseDecimal` 方法 |
| P2-5 | TaxCalculator 数组长度无校验 | 添加 static 初始化块校验 |
| P2-6 | calculateTaxableIncome 返回值无 setScale | 添加 `.setScale(2, HALF_UP)` |
| P3-1 | 半路转正第二次 remove 缺少注释 | 添加说明注释 |
| P3-2 | EmployeeSalaryResult 无防御性拷贝 | 添加 `Objects.requireNonNull` + `unmodifiableList` + `toString` |
| P3-3 | 测试覆盖不足 | 补充 TaxCalculator/Context/ComputeService 共 12 个新用例 |

---

## 4. 涉及文件清单

### 4.1 新增文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../service/TaxCalculator.java` | 统一个税计算工具类 |
| `src/main/java/.../service/SalaryComputeContext.java` | 薪资计算上下文（Builder 模式） |
| `src/main/java/.../service/EmployeeSalaryResult.java` | 单员工计算结果封装 |
| `src/test/java/.../service/TaxCalculatorTest.java` | TaxCalculator 单元测试（22 用例） |
| `src/test/java/.../service/SalaryComputeContextTest.java` | Context 单元测试（3 用例） |
| `src/test/java/.../service/SalaryComputeServiceNewTest.java` | 内存计算单元测试（6 用例） |

### 4.2 修改文件

| 文件 | 改动要点 |
|------|----------|
| `SalaryMonthRecordServiceNew.java` | 主流程重构（Context+先算后存）、半路转正内联、logger 修正、warn 日志 |
| `SalaryComputeServiceNew.java` | 新增 `baseComputeSalaryFromMemory`、`safeParseDecimal`、旧方法标记 `@Deprecated` |

---

## 5. 测试验证

### 5.1 单元测试

| 测试类 | 用例数 | 状态 |
|--------|--------|------|
| TaxCalculatorTest | 22 | ✅ 全部通过 |
| SalaryComputeContextTest | 3 | ✅ 全部通过 |
| SalaryComputeServiceNewTest | 6 | ✅ 全部通过 |
| **合计** | **31** | **✅ 全部通过** |

### 5.2 编译验证

```
mvn -q -DskipTests compile ✅ 通过
```

### 5.3 建议的集成验证

选 3 组真实员工做前后对比：
- A：普通员工（验证结果不变）
- B：半路转正且扣款复杂（验证 `230101/240101` 正确性）
- C：半路转正但无考勤分段（验证兜底行为）

重点核对字段：`210101/220101/230101/240101/1001/270101~270106`

---

## 6. 提交记录

| 序号 | 提交 | 说明 |
|------|------|------|
| 1 | `03bb618` | docs: 优化设计文档和实施计划 |
| 2 | `26be9f4` | feat: 新增 EmployeeSalaryResult 封装单员工计算结果 |
| 3 | `4ad279e` | feat: 提取公共 TaxCalculator 统一个税计算 |
| 4 | `d4c2ef3` | feat: 新增 SalaryComputeContext 封装薪资计算上下文 |
| 5 | `73a1a46` | feat: baseComputeSalary 增加内存重载 |
| 6 | `bb6d732` | refactor: 替换两套个税计算为统一 TaxCalculator |
| 7 | `fd626d1` | refactor: doComputeSalaryData 使用 Context + 先算后存模式 |
| 8 | `041f4c8` | refactor: computeEmployeeSalary 使用内存计算替代中间 saveBatch |
| 9 | `01329ec` | refactor: 合并半路转正两次调用为直接内联流程 |
| 10 | `a9c17c5` | refactor: 清理冗余代码完成 computeSalaryData 重构 |
| 11 | `86756a6` | fix: 交叉审查 P0/P1 问题修复 |
| 12 | `36cfdc1` | fix: 交叉审查 P2/P3 修复 |

---

## 7. 风险与注意事项

1. **半路转正两次 remove**：`processMidMonthPromotionSalary` 可能重新写入全勤奖/工会费到 finalOptions，因此需要第二次清理。如果后续修改该方法，需同步评估是否仍需二次清理。
2. **旧方法 @Deprecated**：`SalaryComputeServiceNew.calculateTax`、`taxRateRangeMap` 等已标记废弃，后续版本可安全删除（当前保留用于测试交叉验证）。
3. **batchSaveResults 事务**：统一批量持久化在主事务内执行，失败即整批回滚，保证批次级原子性。
4. **safeParseDecimal**：非数字值静默降级为 0，不抛异常。如需审计，可在调用处添加日志。
