# computeSalaryData 半路转正优化 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复半路转正薪资计算中的数据来源错误和税算分叉问题，确保半路转正员工的个税、实发等关键金额正确。

**Architecture:** 保留现有两阶段落库结构不变。核心改造：(1) processMidMonthPromotionSalary 复算时合并完整工资上下文（基础项+汇总项），解决扣款项丢失；(2) 将手工税算替换为复用 SalaryComputeServiceNew 的统一税算方法，消除公式分叉；(3) 同步更新全链路依赖字段；(4) 缺考勤分段兜底策略。

**Tech Stack:** Java 8, Spring Boot 2.1.6, JUnit 4, MyBatis-Plus, BigDecimal

---

## Task 1: 为 processMidMonthPromotionSalary 的扣款项读取编写失败测试

**Files:**
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`

**Step 1: 编写测试用例 — 验证半路转正复算能正确读取 100101/100102/280/282**

当前 `processMidMonthPromotionSalary` 从 `finalOptions`（computeSalary 返回值）构建 optionMap，但 finalOptions 不包含 100101/100102/280/282，导致这些扣款项被视为 0。

新增测试：构造一个包含基础项（100101=500, 100102=300, 280=200, 282=100）和汇总项（210101, 230101, 240101）的 optionValueList，调用 processMidMonthPromotionSalary 后验证个税和实发计算中正确使用了这些扣款值。

由于 processMidMonthPromotionSalary 是 private 方法且依赖数据库查询（calculateMidMonthPromotionFullSalary 内部查 salary archives），我们需要：
- 提取一个可测试的纯计算方法（Task 3 实现）
- 此处先写测试骨架，标记为预期的方法签名

```java
@Test
public void processMidMonthPromotion_shouldUseCompleteDeductionContext() {
    // 构造完整的 optionValueList（包含基础项 + 汇总项）
    List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
    allOptions.add(buildOptionValue(100101, "500"));   // 社保
    allOptions.add(buildOptionValue(100102, "300"));   // 公积金
    allOptions.add(buildOptionValue(280, "200"));      // 其他扣款
    allOptions.add(buildOptionValue(282, "100"));      // 借款
    allOptions.add(buildOptionValue(210101, "8000"));   // 应发（将被覆盖）
    allOptions.add(buildOptionValue(230101, "0"));      // 个税（将被重算）
    allOptions.add(buildOptionValue(240101, "0"));      // 实发（将被重算）

    // 半路转正重算后的应发
    Map<Integer, String> midMonthSalaryMap = new HashMap<>();
    midMonthSalaryMap.put(210101, "6000");
    midMonthSalaryMap.put(10101, "3000");
    midMonthSalaryMap.put(10102, "1500");
    midMonthSalaryMap.put(10103, "700");
    midMonthSalaryMap.put(999004, "60");

    // 上月税累计
    Map<Integer, String> lastMonthTaxData = new HashMap<>();
    lastMonthTaxData.put(250101, "0");
    lastMonthTaxData.put(250102, "0");
    lastMonthTaxData.put(250103, "0");
    lastMonthTaxData.put(250105, "0");

    // 调用待提取的纯计算方法（Task 3 创建）
    SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
        allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 6, "2", false, null
    );

    Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
        .filter(o -> o.getCode() != null)
        .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

    // 验证应发被覆盖为半路转正值
    Assert.assertEquals("6000", resultMap.get(210101).getValue());

    // 验证个税计算使用了完整扣款（proxyPay=800, 不是0）
    // 应税所得 = 6000 - 5000 - 800 = 200, 税 = 200 * 3% = 6
    BigDecimal expectedTax = new BigDecimal("6.00");
    Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(expectedTax));

    // 验证实发 = 6000 - 800(社保公积金) - 6(税) - 200(其他扣款) - 100(借款) = 4894
    BigDecimal expectedRealPay = new BigDecimal("4894.00");
    Assert.assertEquals(0, new BigDecimal(resultMap.get(240101).getValue()).compareTo(expectedRealPay));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest#processMidMonthPromotion_shouldUseCompleteDeductionContext -DfailIfNoTests=false`
Expected: 编译失败（recalculateMidMonthPromotionTaxAndPay 方法不存在）

**Step 3: 提交测试骨架**

```bash
svn commit -m "test: add failing test for mid-month promotion deduction context (P0-1)"
```

---

## Task 2: 为半路转正统一税算编写失败测试

**Files:**
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`

**Step 1: 编写测试 — 验证残疾员工半路转正免税**

```java
@Test
public void processMidMonthPromotion_disabledEmployee_shouldSkipTax() {
    List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
    allOptions.add(buildOptionValue(100101, "500"));
    allOptions.add(buildOptionValue(100102, "300"));
    allOptions.add(buildOptionValue(280, "0"));
    allOptions.add(buildOptionValue(282, "0"));
    allOptions.add(buildOptionValue(210101, "8000"));
    allOptions.add(buildOptionValue(230101, "0"));
    allOptions.add(buildOptionValue(240101, "0"));

    Map<Integer, String> midMonthSalaryMap = new HashMap<>();
    midMonthSalaryMap.put(210101, "6000");
    midMonthSalaryMap.put(10101, "3000");
    midMonthSalaryMap.put(10102, "1500");
    midMonthSalaryMap.put(10103, "700");
    midMonthSalaryMap.put(999004, "60");

    Map<Integer, String> lastMonthTaxData = new HashMap<>();
    lastMonthTaxData.put(250101, "0");
    lastMonthTaxData.put(250102, "0");
    lastMonthTaxData.put(250103, "0");
    lastMonthTaxData.put(250105, "0");

    // isDisabled="1" 表示残疾人，应免税
    SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
        allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 6, "1", false, null
    );

    Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
        .filter(o -> o.getCode() != null)
        .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

    // 残疾员工个税应为0
    Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(BigDecimal.ZERO));
    // 实发 = 6000 - 800 - 0(税) - 0 - 0 = 5200
    Assert.assertEquals(0, new BigDecimal(resultMap.get(240101).getValue()).compareTo(new BigDecimal("5200")));
}
```

**Step 2: 编写测试 — 验证 is_remark=2 且累计<6w 免税**

```java
@Test
public void processMidMonthPromotion_remarkEmployee_under60k_shouldSkipTax() {
    List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
    allOptions.add(buildOptionValue(100101, "200"));
    allOptions.add(buildOptionValue(100102, "100"));
    allOptions.add(buildOptionValue(280, "0"));
    allOptions.add(buildOptionValue(282, "0"));
    allOptions.add(buildOptionValue(210101, "4000"));
    allOptions.add(buildOptionValue(230101, "0"));
    allOptions.add(buildOptionValue(240101, "0"));

    Map<Integer, String> midMonthSalaryMap = new HashMap<>();
    midMonthSalaryMap.put(210101, "3000");
    midMonthSalaryMap.put(10101, "2000");
    midMonthSalaryMap.put(10102, "500");
    midMonthSalaryMap.put(10103, "300");
    midMonthSalaryMap.put(999004, "0");

    Map<Integer, String> lastMonthTaxData = new HashMap<>();
    lastMonthTaxData.put(250101, "10000"); // 累计收入1万
    lastMonthTaxData.put(250102, "5000");
    lastMonthTaxData.put(250103, "1000");
    lastMonthTaxData.put(250105, "0");

    // skipTaxForRemark=true 表示 is_remark=2 且累计<6w
    SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
        allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 3, "2", true, null
    );

    Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
        .filter(o -> o.getCode() != null)
        .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

    Assert.assertEquals(0, new BigDecimal(resultMap.get(230101).getValue()).compareTo(BigDecimal.ZERO));
}
```

**Step 3: 运行测试确认失败**

Run: `mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -DfailIfNoTests=false`
Expected: 编译失败

**Step 4: 提交**

```bash
svn commit -m "test: add failing tests for mid-month promotion unified tax (P0-2)"
```

---

## Task 3: 提取 recalculateMidMonthPromotionTaxAndPay 纯计算方法（P0-1 + P0-2 核心修复）

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`

**Step 1: 创建静态纯计算方法**

从 `processMidMonthPromotionSalary` 中提取税算和实发重算逻辑为独立的 static 方法，复用 `SalaryComputeServiceNew.calculateTax` 和 `SalaryComputeServiceNew.calculateTaxIncome`。

关键改造点：
1. 入参 `allOptions` 包含完整工资上下文（基础项 + 汇总项合并后的列表），确保 100101/100102/280/282 可被正确读取
2. 复用统一税算公式，不再手写 7 级税率
3. 支持残疾员工免税（isDisabled="1"）
4. 支持 is_remark=2 免税（skipTaxForRemark 参数）
5. 同步更新全链路依赖字段：220101, 1001, 270101~270106

```java
/**
 * 半路转正纯计算方法：覆盖应发 + 统一税算 + 实发 + 全链路依赖字段
 *
 * @param allOptions       完整工资项列表（基础项 + 汇总项合并），会被原地修改
 * @param midMonthSalaryMap 半路转正分段计算结果（来自 calculateMidMonthPromotionFullSalary）
 * @param lastMonthTaxData  上月税累计数据（250101/250102/250103/250105）
 * @param year             计薪年
 * @param month            计薪月
 * @param isDisabled       是否残疾人（"1"=是，"2"=否）
 * @param skipTaxForRemark 是否 is_remark=2 且累计<6w 免税
 * @param taxSpecialGrandTotal 专项附加扣除累计（可为 null，默认 0）
 */
static void recalculateMidMonthPromotionTaxAndPay(
        List<HrmSalaryMonthOptionValue> allOptions,
        Map<Integer, String> midMonthSalaryMap,
        Map<Integer, String> lastMonthTaxData,
        int year, int month,
        String isDisabled,
        boolean skipTaxForRemark,
        BigDecimal taxSpecialGrandTotal) {

    if (midMonthSalaryMap == null || midMonthSalaryMap.isEmpty()) return;

    Map<Integer, HrmSalaryMonthOptionValue> optionMap = allOptions.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

    // 1. 覆盖基础薪资项
    upsertOptionValue(allOptions, optionMap, 10101, parseAmount(midMonthSalaryMap.get(10101)));
    upsertOptionValue(allOptions, optionMap, 10102, parseAmount(midMonthSalaryMap.get(10102)));
    upsertOptionValue(allOptions, optionMap, 10103, parseAmount(midMonthSalaryMap.get(10103)));
    BigDecimal newShouldPay = parseAmount(midMonthSalaryMap.get(210101));
    upsertOptionValue(allOptions, optionMap, 210101, newShouldPay);
    upsertOptionValue(allOptions, optionMap, 180101, parseAmount(midMonthSalaryMap.get(999004)));

    // 2. 从完整上下文读取扣款项（修复 P0-1）
    BigDecimal proxyPaySalary = amountByCode(optionMap, 100101).add(amountByCode(optionMap, 100102));
    BigDecimal otherDeductions = amountByCode(optionMap, 280);
    BigDecimal loanMoney = amountByCode(optionMap, 282);
    BigDecimal taxAfterPaySalary = amountByCode(optionMap, 150); // 税后补发（如有）
    if (taxSpecialGrandTotal == null) taxSpecialGrandTotal = BigDecimal.ZERO;

    // 3. 构建上月税累计（复用统一逻辑）
    Map<Integer, BigDecimal> lastTaxMap = new HashMap<>();
    lastTaxMap.put(250101, BigDecimal.ZERO);
    lastTaxMap.put(250102, BigDecimal.ZERO);
    lastTaxMap.put(250103, BigDecimal.ZERO);
    lastTaxMap.put(250105, BigDecimal.ZERO);
    if (lastMonthTaxData != null && month != 12) {
        lastTaxMap.put(250101, parseAmount(lastMonthTaxData.get(250101)));
        lastTaxMap.put(250102, parseAmount(lastMonthTaxData.get(250102)));
        lastTaxMap.put(250103, parseAmount(lastMonthTaxData.get(250103)));
        lastTaxMap.put(250105, parseAmount(lastMonthTaxData.get(250105)));
    }

    // 4. 计算累计值
    BigDecimal cumulativeIncome = lastTaxMap.get(250101).add(newShouldPay);
    BigDecimal cumulativeDeductions = lastTaxMap.get(250102).add(new BigDecimal(5000));
    BigDecimal cumulativeSpecialDeduction = lastTaxMap.get(250103).add(proxyPaySalary);

    // 5. 计算应纳税所得额（复用统一公式，修复 P0-2）
    BigDecimal cumulativeTaxableIncome = SalaryComputeServiceNew.calculateTaxIncome(
            cumulativeIncome, cumulativeSpecialDeduction, cumulativeDeductions, taxSpecialGrandTotal);
    if (cumulativeTaxableIncome.compareTo(BigDecimal.ZERO) < 0) {
        cumulativeTaxableIncome = BigDecimal.ZERO;
    }

    // 6. 计算累计税额（复用统一公式）
    BigDecimal cumulativeTaxPayable = SalaryComputeServiceNew.calculateTax(cumulativeTaxableIncome);

    // 7. 当月个税（残疾人或 is_remark 免税时为 0）
    BigDecimal payTaxSalary = BigDecimal.ZERO;
    if ("2".equals(isDisabled) && !skipTaxForRemark) {
        payTaxSalary = cumulativeTaxPayable.subtract(lastTaxMap.get(250105)).max(BigDecimal.ZERO);
    }

    // 8. 应税工资（P1-1 同步）
    BigDecimal shouldTaxSalary = newShouldPay.add(BigDecimal.ZERO) // specialTaxSalary 半路转正暂为0
            .subtract(proxyPaySalary);
    if (shouldTaxSalary.compareTo(BigDecimal.ZERO) < 0) shouldTaxSalary = BigDecimal.ZERO;

    // 9. 实发工资
    BigDecimal realPaySalary = newShouldPay.subtract(proxyPaySalary).subtract(payTaxSalary)
            .add(taxAfterPaySalary).subtract(otherDeductions).subtract(loanMoney);

    // 10. 代扣小计（P1-1 同步）
    BigDecimal totalDeduction = proxyPaySalary.add(payTaxSalary)
            .add(otherDeductions).add(loanMoney);

    // 11. 写入所有字段
    upsertOptionValue(allOptions, optionMap, 220101, shouldTaxSalary);
    upsertOptionValue(allOptions, optionMap, 230101, payTaxSalary);
    upsertOptionValue(allOptions, optionMap, 240101, realPaySalary);
    upsertOptionValue(allOptions, optionMap, 1001, totalDeduction);

    // 12. 本月累计字段（P1-1 同步 270101~270106）
    upsertOptionValue(allOptions, optionMap, 270101, cumulativeIncome);
    upsertOptionValue(allOptions, optionMap, 270102, cumulativeDeductions);
    upsertOptionValue(allOptions, optionMap, 270103, cumulativeSpecialDeduction);
    upsertOptionValue(allOptions, optionMap, 270104, taxSpecialGrandTotal);
    upsertOptionValue(allOptions, optionMap, 270105, cumulativeTaxableIncome);
    upsertOptionValue(allOptions, optionMap, 270106, cumulativeTaxPayable);
}
```

**Step 2: 运行 Task 1 和 Task 2 的测试确认通过**

Run: `mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -DfailIfNoTests=false`
Expected: 全部 PASS

**Step 3: 提交**

```bash
svn commit -m "feat: extract recalculateMidMonthPromotionTaxAndPay with unified tax engine (P0-1, P0-2)"
```

---

## Task 4: 修改 processMidMonthPromotionSalary 调用新方法 + 合并完整上下文

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java` (lines 3747-3828)
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java` (lines 866-906, computeAndSaveEmployeeSalary)

**Step 1: 修改 computeAndSaveEmployeeSalary — 合并完整上下文**

当前问题：第二次 `applyMidMonthPromotionAndUnionFee` 传入的 `finalOptions` 仅包含 computeSalary 返回的汇总项，不含基础项（100101/100102/280/282）。

修改方案：在调用第二次 applyMidMonthPromotionAndUnionFee 前，将 `options`（基础项）和 `finalOptions`（汇总项）合并为 `allOptions`。

```java
// 在 computeAndSaveEmployeeSalary 中，约 line 901 处
List<HrmSalaryMonthOptionValue> finalOptions = computeSalary(ro.record, lastMonthTaxData, employeeVO, isDisabled);

// ===== 修改开始：合并完整上下文 =====
List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>(options);
allOptions.addAll(finalOptions);
// ===== 修改结束 =====

applyMidMonthPromotionAndUnionFee(employeeId, year, month, allOptions, deptType, false,
        becomeDate, midMonthAttendance, normalDays, lastMonthTaxData);
salaryMonthOptionValueService.saveBatch(finalOptions);
```

注意：`allOptions` 仅用于半路转正复算时读取完整上下文，最终落库仍然是 `finalOptions`（汇总项），因为基础项已在前面 saveBatch(options) 落库。但 `recalculateMidMonthPromotionTaxAndPay` 会修改 allOptions 中的汇总项对象（通过 upsertOptionValue），这些对象引用与 finalOptions 中的是同一批对象，所以 saveBatch(finalOptions) 会保存正确的值。

**Step 2: 修改 processMidMonthPromotionSalary — 委托给新方法**

将 `processMidMonthPromotionSalary` 的税算部分替换为调用 `recalculateMidMonthPromotionTaxAndPay`：

```java
private void processMidMonthPromotionSalary(Long employeeId, int year, int month,
                                             List<HrmSalaryMonthOptionValue> optionValueList, Integer deptType,
                                             LocalDate becomeDate, HrmProduceAttendance attendance,
                                             BigDecimal normalDays, Map<Integer, String> lastMonthTaxData) {
    if (!isMidMonthPromotion(becomeDate, year, month) || CollUtil.isEmpty(optionValueList)) {
        return;
    }

    Map<Integer, String> salaryMap = calculateMidMonthPromotionFullSalary(employeeId, year, month,
            becomeDate, attendance, normalDays);
    if (salaryMap == null || salaryMap.isEmpty()) {
        return;
    }

    // 获取残疾人状态和 is_remark 免税状态
    String isDisabled = getEmployeeDisabledStatus(employeeId);
    boolean skipTaxForRemark = checkSkipTaxForRemark(employeeId, year, month, lastMonthTaxData);
    BigDecimal taxSpecialGrandTotal = getEmployeeTaxSpecialGrandTotal(employeeId, year, month);

    // 委托给统一纯计算方法
    recalculateMidMonthPromotionTaxAndPay(
        optionValueList, salaryMap, lastMonthTaxData, year, month,
        isDisabled, skipTaxForRemark, taxSpecialGrandTotal
    );
}
```

**Step 3: 添加辅助方法**

```java
/** 获取员工残疾状态 */
private String getEmployeeDisabledStatus(Long employeeId) {
    HrmEmployee emp = employeeService.getById(employeeId);
    if (emp != null && emp.getIsDisabled() != null) {
        return String.valueOf(emp.getIsDisabled());
    }
    return "2"; // 默认非残疾
}

/** 检查是否 is_remark=2 且累计收入<6w 免税 */
private boolean checkSkipTaxForRemark(Long employeeId, int year, int month,
                                       Map<Integer, String> lastMonthTaxData) {
    HrmEmployee emp = employeeService.getById(employeeId);
    if (emp == null || emp.getIsRemark() == null || emp.getIsRemark() != 2) {
        return false;
    }
    BigDecimal lastYearAccumulated = incomeTaxMapper.getAccumulatedIncomeByEmployeeAndYear(employeeId, year - 1);
    if (lastYearAccumulated == null) lastYearAccumulated = BigDecimal.ZERO;
    BigDecimal currentCumulativeIncome = BigDecimal.ZERO;
    if (lastMonthTaxData != null && lastMonthTaxData.get(250101) != null) {
        currentCumulativeIncome = new BigDecimal(lastMonthTaxData.get(250101));
    }
    return lastYearAccumulated.add(currentCumulativeIncome).compareTo(new BigDecimal("60000")) < 0;
}

/** 获取员工专项附加扣除累计 */
private BigDecimal getEmployeeTaxSpecialGrandTotal(Long employeeId, int year, int month) {
    // 从当月已保存的工资项中读取 parentCode=260 的合计
    // 此值在 baseComputeSalary 中已计算并存入 SalaryBaseTotal
    // 这里从已落库的 options 中读取
    List<ComputeSalaryDto> dtos = salaryMonthOptionValueService.queryEmpSalaryOptionValueListByEmployee(employeeId, year, month);
    BigDecimal total = BigDecimal.ZERO;
    if (dtos != null) {
        for (ComputeSalaryDto dto : dtos) {
            if (dto.getParentCode() != null && dto.getParentCode().equals(260)) {
                total = total.add(new BigDecimal(dto.getValue()));
            }
        }
    }
    return total;
}
```

**Step 4: 运行全部测试**

Run: `mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -DfailIfNoTests=false`
Expected: PASS

**Step 5: 编译检查**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

**Step 6: 提交**

```bash
svn commit -m "feat: processMidMonthPromotionSalary delegates to unified recalculation (P0-1, P0-2, P1-1)"
```

---

## Task 5: 缺失考勤分段数据兜底策略（P1-2）

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java` (calculateMidMonthPromotionFullSalary, ~line 3713)
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`

**Step 1: 编写测试 — 缺考勤分段时应记录警告并返回空 Map**

```java
@Test
public void calculateMidMonthPromotionFullSalary_nullAttendance_shouldReturnEmptyMap() {
    // 当 attendance 为 null 时，应返回空 Map（不静默走普通流程）
    // 由于 calculateMidMonthPromotionFullSalary 是 private 且依赖 DB，
    // 我们测试 calculateMidMonthPromotionSalaryAmounts 的 null attendance 行为
    Map<Integer, String> result = SalaryMonthRecordServiceNew.calculateMidMonthPromotionSalaryAmounts(
        buildSalaryMap("3000", "1500", "700"),
        buildSalaryMap("4000", "2000", "900"),
        null,  // attendance 为 null
        new BigDecimal("21.75")
    );
    Assert.assertTrue("缺考勤分段应返回空Map", result.isEmpty());
}
```

**Step 2: 修改 calculateMidMonthPromotionFullSalary 添加日志警告**

在 `calculateMidMonthPromotionFullSalary` 中，当 `hrmProduceAttendance == null` 时：

```java
if (hrmProduceAttendance == null) {
    logger.warn("[半路转正] 员工{}({}/{})缺少考勤分段数据，跳过半路转正重算，保留普通计算结果",
            employeeId, year, month);
    return Collections.emptyMap();
}
```

**Step 3: 修改 calculateMidMonthPromotionSalaryAmounts 添加 null 检查**

```java
static Map<Integer, String> calculateMidMonthPromotionSalaryAmounts(...) {
    if (attendance == null) {
        return Collections.emptyMap();
    }
    // ... 原有逻辑
}
```

**Step 4: 运行测试**

Run: `mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -DfailIfNoTests=false`
Expected: PASS

**Step 5: 提交**

```bash
svn commit -m "fix: add fallback for missing attendance segment data in mid-month promotion (P1-2)"
```

---

## Task 6: Boolean 入参空值防护（P2-1）

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java` (computeSalaryData 方法入口)

**Step 1: 在 computeSalaryData 入口处添加 Boolean 空值防护**

```java
// 在 computeSalaryData 方法开头（约 line 770）
if (isSyncInsuranceData == null) isSyncInsuranceData = Boolean.FALSE;
if (isSyncAttendanceData == null) isSyncAttendanceData = Boolean.FALSE;
```

**Step 2: 编译检查**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
svn commit -m "fix: null-safe Boolean params in computeSalaryData (P2-1)"
```

---

## Task 7: 测试辅助方法补充 + 回归测试

**Files:**
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`

**Step 1: 添加 buildOptionValue 辅助方法**

```java
private HrmSalaryMonthOptionValue buildOptionValue(int code, String value) {
    HrmSalaryMonthOptionValue ov = new HrmSalaryMonthOptionValue();
    ov.setCode(code);
    ov.setValue(value);
    return ov;
}
```

**Step 2: 添加回归测试 — 非半路转正员工不受影响**

```java
@Test
public void recalculateMidMonthPromotion_emptyMidMonthMap_shouldNotModifyOptions() {
    List<HrmSalaryMonthOptionValue> options = new ArrayList<>();
    options.add(buildOptionValue(210101, "8000"));
    options.add(buildOptionValue(230101, "100"));
    options.add(buildOptionValue(240101, "7000"));

    // midMonthSalaryMap 为空，不应修改任何值
    SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
        options, Collections.emptyMap(), null, 2026, 6, "2", false, null
    );

    Map<Integer, HrmSalaryMonthOptionValue> resultMap = options.stream()
        .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity()));
    Assert.assertEquals("8000", resultMap.get(210101).getValue());
    Assert.assertEquals("100", resultMap.get(230101).getValue());
    Assert.assertEquals("7000", resultMap.get(240101).getValue());
}
```

**Step 3: 添加回归测试 — 12月累计重置**

```java
@Test
public void recalculateMidMonthPromotion_december_shouldResetCumulative() {
    List<HrmSalaryMonthOptionValue> allOptions = new ArrayList<>();
    allOptions.add(buildOptionValue(100101, "500"));
    allOptions.add(buildOptionValue(100102, "300"));
    allOptions.add(buildOptionValue(280, "0"));
    allOptions.add(buildOptionValue(282, "0"));
    allOptions.add(buildOptionValue(210101, "8000"));
    allOptions.add(buildOptionValue(230101, "0"));
    allOptions.add(buildOptionValue(240101, "0"));

    Map<Integer, String> midMonthSalaryMap = new HashMap<>();
    midMonthSalaryMap.put(210101, "6000");
    midMonthSalaryMap.put(10101, "3000");
    midMonthSalaryMap.put(10102, "1500");
    midMonthSalaryMap.put(10103, "700");
    midMonthSalaryMap.put(999004, "60");

    // 上月有大量累计（但12月应重置）
    Map<Integer, String> lastMonthTaxData = new HashMap<>();
    lastMonthTaxData.put(250101, "100000");
    lastMonthTaxData.put(250102, "55000");
    lastMonthTaxData.put(250103, "10000");
    lastMonthTaxData.put(250105, "5000");

    SalaryMonthRecordServiceNew.recalculateMidMonthPromotionTaxAndPay(
        allOptions, midMonthSalaryMap, lastMonthTaxData, 2026, 12, "2", false, null
    );

    Map<Integer, HrmSalaryMonthOptionValue> resultMap = allOptions.stream()
        .filter(o -> o.getCode() != null)
        .collect(Collectors.toMap(HrmSalaryMonthOptionValue::getCode, Function.identity(), (a, b) -> b));

    // 12月累计收入应仅为本月：6000（不含上月累计）
    Assert.assertEquals(0, new BigDecimal(resultMap.get(270101).getValue()).compareTo(new BigDecimal("6000")));
    // 累计减除费用应仅为 5000
    Assert.assertEquals(0, new BigDecimal(resultMap.get(270102).getValue()).compareTo(new BigDecimal("5000")));
}
```

**Step 4: 运行全部测试**

Run: `mvn test -pl . -Dtest=SalaryMonthRecordServiceNewTest -DfailIfNoTests=false`
Expected: 全部 PASS

**Step 5: 提交**

```bash
svn commit -m "test: add regression tests for mid-month promotion recalculation"
```

---

## Task 8: 全量编译 + 最终验证

**Step 1: 全量编译**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

**Step 2: 运行全部单元测试**

Run: `mvn test -DfailIfNoTests=false`
Expected: 全部 PASS

**Step 3: 代码审查清单**

验证以下关键点：
- [ ] processMidMonthPromotionSalary 不再包含手写 7 级税率代码
- [ ] recalculateMidMonthPromotionTaxAndPay 调用 SalaryComputeServiceNew.calculateTax
- [ ] optionMap 构建来源包含 100101/100102/280/282
- [ ] 220101/1001/270101~270106 在半路转正时同步更新
- [ ] 残疾员工免税逻辑与 computeSalary 一致
- [ ] is_remark=2 免税逻辑与 computeSalary 一致
- [ ] 12月累计重置逻辑与 computeSalary 一致
- [ ] 缺考勤分段时有 warn 日志且返回空 Map
- [ ] Boolean 参数 null 安全

**Step 4: 提交最终版本**

```bash
svn commit -m "chore: final verification for computeSalaryData mid-month promotion optimization"
```

---

## 实施顺序总结

| 顺序 | Task | 风险级别 | 内容 |
|------|------|----------|------|
| 1 | Task 1 | P0-1 | 编写扣款项读取失败测试 |
| 2 | Task 2 | P0-2 | 编写统一税算失败测试 |
| 3 | Task 3 | P0-1+P0-2 | 提取 recalculateMidMonthPromotionTaxAndPay |
| 4 | Task 4 | P0+P1-1 | 修改调用链 + 合并完整上下文 |
| 5 | Task 5 | P1-2 | 缺考勤分段兜底 |
| 6 | Task 6 | P2-1 | Boolean 空值防护 |
| 7 | Task 7 | 回归 | 补充回归测试 |
| 8 | Task 8 | 验证 | 全量编译 + 最终验证 |

每个 Task 独立提交，保证可回滚。
