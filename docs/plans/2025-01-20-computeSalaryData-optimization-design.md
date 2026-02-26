# computeSalaryData 薪资核算核心方法优化设计

## 1. 背景

`SalaryMonthRecordServiceNew.java`（约4700行）是薪资核算的核心文件，其中 `computeSalaryData` → `doComputeSalaryData` → `computeAndSaveEmployeeSalary` 是薪资计算的主流程。

当前存在以下问题：
- 两套独立的个税计算实现（`SalaryComputeServiceNew` 用 TreeRangeMap，`calculateMidMonthPromotionSummary` 用数组），未来税率调整需改两处，边界值可能不一致
- `computeAndSaveEmployeeSalary` 有20个参数，可读性差
- 计算和持久化耦合：每个员工计算完立即 saveBatch，导致半路转正需要两次 `applyMidMonthPromotionAndUnionFee` 调用（第一次保存基础项到DB → computeSalary从DB读取 → 第二次计算转正薪资）
- 半路转正和正式员工的薪资核算路径差异大，兼容性风险高

## 2. 优化目标

1. 高可读性、健壮性、简洁性
2. 计算与持久化分离：先串行计算所有员工结果放内存，最后统一批量持久化
3. 合理抽取公共方法
4. 半路转正员工薪资核算与已转正员工薪资核算兼容
5. 在已实现算法基础上减少代码量
6. 使用TDD流程

## 3. 关键讨论记录

### 3.1 两套个税计算统一

**现状：**
- 正式员工：`SalaryComputeServiceNew.computeSalary` → `calculateTax` → `TreeRangeMap` 查找税率
- 半路转正：`calculateMidMonthPromotionSummary` → `calculateCumulativeTaxPayable` → 数组遍历查找税率
- 两者税率表相同（3%-45%七级累进），但实现不同

**决定：** 统一为一套实现，保持结果准确性。尚未在生产环境发现不一致，但存在潜在风险。

### 3.2 硬编码员工ID（全勤豁免）

**现状：** `fillAttendanceDataForEmployee` 中硬编码了3个员工ID（董事长、总经理、张宏海）直接给全勤。

**决定：** 本次保留不动，后续优化为数据库字段配置。

### 3.3 参数封装

**现状：** `computeAndSaveEmployeeSalary` 有20个参数，包含大量批量预加载的 Map 数据。

**决定：** 封装为 `SalaryComputeContext` 上下文对象，参数从20个降到3-4个。新增批量数据时只需改 context 类。

### 3.4 线程与持久化策略

**现状：** 每个员工串行执行：计算 → saveBatch → updateById，中间有DB交互。

**决定：** 采用方案B — 先串行计算所有员工结果放内存，最后统一批量持久化。减少DB交互次数，风险低。

**排除的方案：**
- 员工之间并行（事务隔离风险高）
- 单员工内部异步保存（复杂度中等，收益不明显）

### 3.5 baseComputeSalary 重构

**现状：** `SalaryComputeServiceNew.baseComputeSalary` 从DB查询已保存的工资项来计算应发工资，导致必须先 saveBatch 再计算个税。

**决定：** 增加重载方法，支持从内存中的工资项列表计算，消除中间 saveBatch 依赖。这样半路转正的两次 `applyMidMonthPromotionAndUnionFee` 可以合并为一次流程。

## 4. 方案对比

### 方案A：渐进式重构 ✅ 已选定

在现有方法结构上做精准手术，不改变整体类的组织方式。

**具体改动：**
1. 新增 `SalaryComputeContext` — 封装所有批量预加载数据
2. 统一个税计算 — 提取公共 `TaxCalculator`，`calculateMidMonthPromotionSummary` 和 `SalaryComputeServiceNew` 共用
3. `baseComputeSalary` 增加内存重载 — 支持从 List 计算，不依赖DB
4. 计算与持久化分离 — `computeAndSaveEmployeeSalary` 拆成纯计算方法 + 外层统一 saveBatch
5. 半路转正流程合并 — 两次调用合并为一次
6. TDD — 先写测试覆盖现有行为，再重构

**优点：** 改动范围可控，风险低，逐步验证
**缺点：** 类文件仍然较大（但核心方法会大幅缩短）

### 方案B：大规模拆分重构

将 `SalaryMonthRecordServiceNew` 拆成多个独立 Service：
- `SalaryOrchestrationService`：编排流程
- `SalaryOptionResolver`：工资项解析
- `TaxCalculationService`：统一个税计算
- `MidMonthPromotionService`：半路转正专用逻辑

**优点：** 架构更清晰，职责分离彻底
**缺点：** 改动面巨大，风险高，需要大量集成测试

### 方案C：仅做代码整理

只做参数封装和方法提取，不动计算逻辑和持久化时机。

**优点：** 最安全
**缺点：** 不解决核心问题

## 5. 技术要点

### 5.1 薪资项编码体系

| 编码 | 含义 |
|------|------|
| 10101 | 基本工资 |
| 10102 | 岗位工资 |
| 10103 | 职务工资 |
| 40102 | 满勤奖 |
| 100101 | 个人社保 |
| 100102 | 个人公积金 |
| 160102 | 工会费 |
| 210101 | 应发工资 |
| 220101 | 应税工资 |
| 230101 | 个税 |
| 240101 | 实发工资 |
| 250101-250105 | 上月累计（收入/费用/公积金/已缴税） |
| 260101-260106 | 专项附加扣除（子女教育/住房租金等） |
| 270101-270106 | 本月累计 |
| 280 | 其他扣款 |
| 282 | 借款 |
| 41001 | 奖金 |

### 5.2 个税七级累进税率表

| 级数 | 累计应纳税所得额 | 税率 | 速算扣除数 |
|------|-----------------|------|-----------|
| 1 | ≤36,000 | 3% | 0 |
| 2 | 36,000-144,000 | 10% | 2,520 |
| 3 | 144,000-300,000 | 20% | 16,920 |
| 4 | 300,000-420,000 | 25% | 31,920 |
| 5 | 420,000-660,000 | 30% | 52,920 |
| 6 | 660,000-960,000 | 35% | 85,920 |
| 7 | >960,000 | 45% | 181,920 |

### 5.3 特殊规则

- 12月累计税重置（新年度从零开始）
- 残疾人免税（isDisabled="1"）
- 备注员工免税（is_remark=2 且年收入<6万）
- 公司特殊规则（0002=成都不含奖金累计收入，0005=攀枝花）
- 工会费：正式员工应发工资 × 0.5%
- 社保同步口径：当月/上月/下月可配置

### 5.4 半路转正核心逻辑

半路转正员工（转正日期在计薪月内）的薪资按天数比例拆分：
- 试用期天数的薪资 = 试用期定薪 × (试用期出勤天数 / 应出勤天数)
- 正式期天数的薪资 = 正式定薪 × (正式出勤天数 / 应出勤天数)
- 个税按合并后的应发工资统一计算

## 6. 涉及文件

| 文件 | 改动类型 |
|------|---------|
| `SalaryMonthRecordServiceNew.java` | 主要重构目标 |
| `SalaryComputeServiceNew.java` | baseComputeSalary 增加内存重载，提取公共税率计算 |
| `SalaryMonthRecordServiceNewTest.java` | TDD：扩展测试覆盖 |
| `SalaryComputeContext.java`（新增） | 上下文对象 |
| `TaxCalculator.java`（新增） | 公共个税计算工具 |

## 7. TDD流程

1. 先为现有 `computeAndSaveEmployeeSalary` 的关键路径补充单元测试（正式员工、半路转正员工）
2. 提取 `TaxCalculator`，写测试验证与两套现有实现结果一致
3. 逐步重构，每步确保测试通过
4. 新增 `SalaryComputeContext`，重构参数传递
5. 重构 `baseComputeSalary` 支持内存计算
6. 合并半路转正流程
7. 分离计算与持久化
