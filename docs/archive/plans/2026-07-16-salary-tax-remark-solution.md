# 薪资核算个税备注场景解决方案

## 背景

用户反馈薪资核算个税出现新场景：税务局系统中员工是否生成备注，会影响累计减除费用口径；同时存在某月工资低于 5000、但前后月份已有或将有个税的内部特殊情况。

本方案最初用于说明计算口径和代码改造方向；2026-07-16 已按“方案 A：复用 `hrm_employee.is_remark`”落地实现。

## 实施前代码口径

- 薪资核算入口：`HrmSalaryMonthRecordController#computeSalaryData` 调用 `SalaryMonthRecordServiceNew#computeSalaryData(...)`。
- 主链路在 `SalaryMonthRecordServiceNew#doComputeSalaryData(...)` 中：
  - 先调用 `updateAddition(employeeId, year, month)` 生成下月专项附加累计；
  - 通过 `loadLastMonthTaxDataMap(...)` 读取 `hrm_personal_income_tax` 上月累计个税；
  - 通过 `loadAdditionalDeductionMap(...)` 读取当月 `hrm_additional` 专项附加累计；
  - 通过 `computeSalaryFromMemory(...)` 调用 `SalaryComputeServiceNew#computeSalary(...)` 计算个税、实发和累计项。
- 现有个税累计项：
  - `250101`：上月累计收入；
  - `250102`：上月累计减除费用；
  - `250103`：上月累计专项扣除；
  - `250105`：上月累计已缴税额；
  - `270101`：本月累计收入；
  - `270102`：本月累计减除费用；
  - `270103`：本月累计专项扣除；
  - `270104`：本月累计专项附加扣除；
  - `270105`：本月累计应纳税所得额；
  - `270106`：本月累计应纳税额；
  - `230101`：本月个人所得税。
- 实施前备注逻辑是 `hrm_employee.is_remark=2` 且收入条件满足时，本月个税直接为 0；但 `270102` 仍按 `上月累计减除费用 + 5000`，没有切到全年 60000 口径。
- 复核后确认主链路通过 `computeSalaryFromMemory(...) -> SalaryComputeServiceNew#computeSalary(...) -> saveTaxAccumulationData(...)` 同步保存 `hrm_personal_income_tax`，因此本次实现重点是让保存数据使用新口径。

## 目标计算口径

个税应统一按累计预扣预缴计算，不按单月工资是否超过 5000 单独切断。

### 1. 无税务局备注

以 6 月薪资为例：

```text
累计减除费用 = 6 * 5000 = 30000
累计专项扣除 = 基本养老保险 + 基本医疗费用 + 失业保险 + 住房公积金
累计应纳税所得额 = max(截止6月累计收入 - 累计专项扣除 - 累计专项附加扣除 - 30000, 0)
累计应纳税额 = 七级超额累进税率(累计应纳税所得额)
6月个税 = max(累计应纳税额 - 1-5月累计已缴税额, 0)
```

说明：用户公式中的括号应按四项合计理解，即 `养老 + 医疗 + 失业 + 公积金`，不是四项互相相减。

### 2. 有税务局备注

以 6 月薪资为例：

```text
累计减除费用 = 60000
累计专项扣除 = 基本养老保险 + 基本医疗费用 + 失业保险 + 住房公积金
累计应纳税所得额 = max(截止6月累计收入 - 累计专项扣除 - 累计专项附加扣除 - 60000, 0)
累计应纳税额 = 七级超额累进税率(累计应纳税所得额)
6月个税 = max(累计应纳税额 - 1-5月累计已缴税额, 0)
```

这条规则不应实现为“备注员工一律不算税”。当员工累计收入扣除 60000、专项扣除、专项附加扣除后仍有正数应纳税所得额时，应继续按七级税率计算。

### 3. 某月工资低于 5000 的特殊情况

以 6 月工资只发 3000、7 月恢复满工资为例：

1. 6 月不按单月工资 3000 单独判断是否退税或清零全年税额。
2. 先按 1-6 月累计数据计算 `累计应纳税额6`。
3. `6月个税 = max(累计应纳税额6 - 1-5月累计已缴税额, 0)`。
4. 如果 1-5 月已缴税额大于 1-6 月重新计算出的累计应纳税额，6 月工资表扣税为 0，不在工资中生成负数退税。
5. 7 月恢复满工资时，重新按 1-7 月累计数据计算：

```text
无备注：累计减除费用 = 7 * 5000 = 35000
有备注：累计减除费用 = 60000
7月个税 = max(累计应纳税额7 - 1-6月累计已缴税额, 0)
```

6. 因 6 月低工资导致的前期多扣税，会通过后续月份 `累计应纳税额 - 累计已缴税额` 自然抵减；若年末仍多缴，走年度汇算清缴退税，不建议在薪资表里发负个税。

## 改造方案

### 方案 A：最小改造，复用 `is_remark`

- 将 `hrm_employee.is_remark=2` 解释为“税务局系统已生成 6 万减除费用备注”。
- 移除或废弃备注员工“本月个税直接跳过”的旧逻辑。
- 新增统一方法解析累计减除费用：

```text
if is_remark == 2:
    cumulativeDeduction = 60000
else:
    cumulativeDeduction = 5000 * 当前纳税年度截至计薪月的本单位任职受雇月份数
```

- 现有 `SalaryComputeServiceNew#calculateTaxAccumulation(...)` 和 `SalaryMonthRecordServiceNew#calculateMidMonthPromotionSummary(...)` 都应改为使用该方法，保证普通员工和半路转正复算一致。
- `270102` 必须写入最终采用的累计减除费用：无备注 6 月为 `30000`，有备注 6 月为 `60000`。

优点：改动小，能快速对齐用户反馈。

风险：`is_remark` 是员工维度字段，税务局备注实际是年度口径；跨年度变化时需要人工维护。

### 方案 B：推荐改造，新增员工年度个税口径

新增或引入员工年度个税政策数据，例如：

```text
employee_id
tax_year
deduction_mode: MONTHLY_5000 / ANNUAL_60000
source: TAX_SYSTEM / MANUAL
remark_text
updated_time
```

薪资核算按 `employee_id + year` 查询该表：

- `ANNUAL_60000`：累计减除费用固定 `60000`；
- `MONTHLY_5000` 或无配置：按任职受雇月份数乘 `5000`；
- 页面可展示“税务局备注口径”，并支持按税务局导出或人工核对结果导入。

优点：符合年度政策属性，可审计、可追踪来源，避免 2026 年备注影响 2027 年。

风险：需要补表、导入/维护入口和数据迁移。

## 个税累计表处理

现有 `SalaryComputeServiceNew#saveTaxAccumulationData(...)` 已承担同步步骤；批量核算通过 `computeSalaryFromMemory(...)` 调用同一入口：

1. 从最终工资项读取：
   - `270101` 累计收入；
   - `270102` 累计减除费用；
   - `270103` 累计专项扣除；
   - `250105 + 230101` 累计实际已缴税额。
2. 按 `employee_id + year + end_month` 删除旧 `hrm_personal_income_tax`。
3. 插入本次核算后的累计记录。

这样 6 月核算完成后，7 月可直接读取 6 月累计，不再要求业务先手工导入“截止上月个税累计”。

## 现有数据核对结论

按前端菜单和接口反查，现有三个模块已经覆盖方案需要的数据来源：

- 个税累计菜单：`个税累计`，路径 `/hrm/salary/tax`，接口前缀 `/hrmPersonalIncomeTax`，表 `hrm_personal_income_tax`；
- 附加累计菜单：`附加累计`，路径 `/hrm/salary/addition`，接口前缀 `/hrmAdditional`，表 `hrm_additional`；
- 年度附加扣除菜单：`年度附加扣除`，路径 `/hrm/salary/additionDeduction`，接口前缀 `/hrmEmployeeAdditional`，表 `hrm_employee_additional`；
- 税务局备注字段在员工表 `hrm_employee.is_remark`，当前代码中 `is_remark=2` 已作为特殊个税标识读取。

因此落地本方案不需要新增“6月低工资”“7月满工资”“特殊情况”等新业务数据表或字段。需要保证现有数据按月份连续维护：

- 计算某月薪资时，必须能取到上月 `hrm_personal_income_tax`；
- 计算某月薪资时，必须能取到当月 `hrm_additional`；
- 生成下月附加累计时，必须能取到对应年度 `hrm_employee_additional`；
- 员工 `is_remark=2` 视为已生成税务局 60000 减除费用备注，`NULL/其他值` 视为未生成备注。

`hr_0003` 初次核对结果显示：

- `hrm_employee.is_remark`：在职/未删除员工中 `2` 有 96 人，`NULL` 有 17 人；
- `hrm_personal_income_tax`：`2026-05` 有 100 行、98 人，存在 1 个员工重复 3 行；`2026-06` 目前无记录；
- `hrm_additional`：`2026-06` 有 41 行、40 人，存在 1 个重复员工；`2026-07` 目前无记录；
- `hrm_employee_additional`：`2026` 有 39 行、38 人，存在 1 个重复员工。

`2026-05` 个税累计重复不是同名员工天然重复，而是同一员工 ID 重复：`employee_id=1831601326890434614`，员工王芳，工号 `TYNG-107`，手机号 `13032750052`，身份证号 `420801197602274069`，同一年月存在 3 条个税累计记录，累计收入分别为 `29461.00/19281.00/32031.00`。该员工本身与另外两名王芳是不同员工，但重复问题应按 `employee_id + year + end_month` 判断，而不能按姓名判断。

这些重复/缺月问题属于现有数据质量和自动延续问题，不是新增数据模型问题。已先完成重名员工导入匹配修复和既有重复数据清理，最终 `hrm_personal_income_tax(employee_id,year,end_month)`、`hrm_additional(employee_id,year,month)`、`hrm_employee_additional(employee_id,year)` 三类重复计数均为 0。计算 6 月薪资时，应读取 `2026-05` 个税累计作为上月基础，叠加 6 月本次核算出的累计收入、累计减除费用、累计专项扣除和本月个税后，自动保存为 `2026-06` 的 `hrm_personal_income_tax` 记录，供 7 月继续读取。

## 已实施内容

- `SalaryComputeServiceNew#resolveCumulativeDeductions(...)`：
  - 无备注员工按 `5000 * 计薪月份`；
  - `is_remark=2` 员工固定 `60000`。
- `SalaryComputeServiceNew#computeSalary(...)`、`SalaryMonthRecordServiceNew#calculateMidMonthPromotionSummary(...)` 和半路转正一致性复算均使用同一方法。
- 备注员工不再直接免扣个税；本月个税统一为 `max(累计应纳税额 - 上月累计已缴税额, 0)`。
- 1 月重置本年度累计，12 月继续累计全年。
- 批量核算上下文已移除旧备注免税规则依赖的上一年累计收入数据。
- 验证命令：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test`，37 个测试通过。

## 测试场景

- 无备注员工，6 月：`270102 = 30000`，按累计收入、专项扣除、专项附加扣除计算 `230101`。
- 有备注员工，6 月累计收入未超 60000：`270102 = 60000`，`270105 = 0`，`230101 = 0`。
- 有备注员工，累计收入超过 60000 且扣除后为正：不免扣个税，按七级税率计算。
- 6 月工资 3000 且 1-5 月已缴税：6 月本期个税不为负，写 `0`。
- 7 月恢复满工资：按 1-7 月累计重算，再减 1-6 月累计已缴税额。
- 半路转正员工：主计算和一致性复算的 `270102/230101` 结果一致。
- 1 月跨年：累计口径清零后重新开始；建议同时复查现有 `month == 12` 清零逻辑是否应改为 `month == 1`。

## 待确认问题

- 税务局备注数据是否已经可靠落到 `hrm_employee.is_remark`，还是需要从税务局导出文件新增导入？
- 税务局备注是否只在年初确定，还是业务上可能中途变更？如果中途变更，需按税局系统当前属期提示决定是否允许切换。
- 工资表是否允许显示负个税/退税？本方案建议不允许，保持 `max(..., 0)`，多缴部分由后续月份抵减或年度汇算处理。

## 政策依据

- [国家税务总局公告 2018 年第 56 号](https://beijing.chinatax.gov.cn/bjswj/sszc/zxwj/201906/86e9726f01b74b8e99a99f8381c93b19.shtml)：居民个人工资薪金按累计预扣法计算，公式为累计收入减累计减除费用、累计专项扣除、累计专项附加扣除等后计算本期应预扣预缴税额。
- [国家税务总局公告 2020 年第 19 号相关解读](https://guangxi.chinatax.gov.cn/beihai/tzgg_15227/tzgg_15228/202012/t20201214_327989.html)：符合条件人员本年度预扣预缴时，累计减除费用自 1 月起直接按全年 60000 计算；累计收入未超过 60000 的月份暂不预扣，超过后再预扣。
