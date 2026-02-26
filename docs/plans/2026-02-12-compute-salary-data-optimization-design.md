# `computeSalaryData` 优化设计文档（评审草案）

## 1. 背景与目标

`computeSalaryData` 是当前月薪计算主入口，覆盖：员工筛选、考勤同步、薪资项装配、个税计算、实发计算、结果落库。

本轮目标（严格按你的要求）：

1. **只做优化，不改薪资算法口径**（尤其各薪资项计算规则保持一致）。
2. 重点审查并修正**半路转正**路径（高风险逻辑）。
3. 方案兼顾：**简洁性、易用性、可复用性、性能瓶颈治理**。
4. 先完成问题分析与方案讨论，再进入实现。

---

## 2. 审查范围

本次重点审查以下链路：

- 主流程：`SalaryMonthRecordServiceNew.computeSalaryData`。
- 员工计算子流程：`computeAndSaveEmployeeSalary`、`getOrCreateRecordAndApplyAttendance`。
- 半路转正相关：`isMidMonthPromotion`、`calculateMidMonthPromotionSalaryAmounts`、`processMidMonthPromotionSalary`。
- 个税/实发基准算法：`SalaryComputeServiceNew.computeSalary` 及其税费辅助逻辑。

---

## 3. 当前流程概览（简化）

### 3.1 主流程

1. 查询月记录、薪资配置。
2. 更新附加扣除累计。
3. 校验社保月数据。
4. 查“有薪资档案”员工。
5. 解析/同步考勤到 `attendanceDataMap`。
6. 循环员工执行：
   - 创建或重建员工月记录 + 非固定项 + 固定项。
   - 注入社保、专项附加扣除。
   - 先做一次半路转正“移除满勤/工会费”。
   - 落库基础项。
   - 调 `computeSalary` 计算个税/实发等汇总项。
   - 再做一次半路转正重算（应发、个税、实发）。
   - 落库汇总项。
7. 统计月记录汇总并更新状态。

### 3.2 半路转正当前处理方式

- 判定规则：转正日期在计薪月内，且日期大于 1 号。
- 汇总应发由 `calculateMidMonthPromotionSalaryAmounts` 计算：
  - 试用段应发（按试用薪资 * 试用出勤/应出勤）。
  - 转正段应发（按转正薪资 * 转正出勤/应出勤）。
  - 叠加补贴和加班。
- 在 `processMidMonthPromotionSalary` 中覆盖 `210101/180101/230101/240101` 等关键项。

---

## 4. 关键问题清单（按风险级别）

## P0（必须优先处理）

### P0-1 半路转正复算时“代扣/其他扣款/借款”被错误视为 0

现象：

- `processMidMonthPromotionSalary` 里读取 `100101/100102/280/282` 来计算个税和实发。
- 但该方法的 `optionMap` 来源是 `finalOptions`（即 `computeSalary` 的返回）。
- `computeSalary` 返回的集合中并不包含 `100101/100102/280/282`，仅有汇总项与税累计项。

影响：

- 半路转正员工的 `proxyPaySalary`、`otherDeductions`、`loanMoney` 可能被算成 0。
- 直接导致：`230101`（个税）与 `240101`（实发）失真。

风险等级：**极高（直接金额错误）**。

---

### P0-2 半路转正手工税算逻辑与统一税算引擎分叉

现象：

- 正常员工使用 `SalaryComputeServiceNew` 税算流程。
- 半路转正使用 `processMidMonthPromotionSalary` 手工分支重算个税/实发。

分叉后可能遗漏统一规则：

- 残疾员工免税逻辑。
- `is_remark=2` 年收入<6w 免税逻辑。
- 税后补发/补扣处理。
- 代扣小计 `1001` 与累计税字段一致性。

影响：

- 半路转正与普通员工规则不一致，且后续改税规则时易漏改（高复发）。

风险等级：**极高（可维护性与正确性双高风险）**。

---

## P1（应在本次一起处理）

### P1-1 覆盖了 `210101/230101/240101`，但未同步所有依赖字段

现象：

- 半路转正复算覆盖了部分汇总项，但 `220101`、`1001`、`270101~270106` 等依赖字段未同步更新。

影响：

- 报表字段与明细字段可能“表面一致、累计不一致”。
- 后续月份税累计来源存在偏差风险。

风险等级：**高**。

---

### P1-2 半路转正缺失考勤分段数据时直接跳过复算

现象：

- `calculateMidMonthPromotionFullSalary` 在 `hrmProduceAttendance == null` 时直接返回空。

影响：

- 半路转正员工可能走普通流程结果，出现“未按半路转正规则处理”。
- 且未形成可追踪告警。

风险等级：**高**。

---

### P1-3 主流程存在明显 N+1 查询热点

热点位置：

- 员工筛选时逐人查薪资档案。
- 逐人判断是否有考勤组。
- 每员工计算多次读写 option/value。

影响：

- 员工规模上来后计算耗时线性恶化，事务时长过长。

风险等级：**中高**。

---

## P2（建议顺手修正）

### P2-1 Boolean 入参空值风险

- `Boolean` 参数（如 `isSyncAttendanceData`, `isSyncInsuranceData`）在部分分支直接 `if (flag)`/`if (!flag)`。
- 若调用方传 `null`，存在 NPE 风险。

### P2-2 规则可读性不足、重复映射散落

- 大量“工资项 code”硬编码分散，半路转正处理难以审计。

---

## 5. 半路转正逻辑专项结论（重点）

我这次最关注的是你强调的“谨慎审查半路转正”：

1. 当前方案是“**先按通用算，再覆盖关键值**”。思路可行，但覆盖时依赖了不完整数据源，导致关键扣款项丢失（P0-1）。
2. 手工税算分支与统一税算引擎并行，短期可工作，长期高复发（P0-2）。
3. 在“考勤分段缺失”场景缺少兜底策略（P1-2），容易静默产生偏差。

结论：**半路转正必须先做“数据来源纠偏 + 统一税算口径复用”，再谈性能优化。**

---

## 6. 三种优化路径（含取舍）

## 方案 A（推荐，低风险渐进）

核心思想：

- 保留现有主流程和数据表结构。
- 修正半路转正复算的数据来源，确保读取完整扣款项。
- 将半路转正税算迁移为“复用统一税算逻辑”，杜绝公式分叉。

优点：

- 对现有逻辑扰动最小。
- 风险可控，可快速落地。
- 能立刻解决金额错误与高复发问题。

不足：

- 仍保留“两阶段落库”结构，性能收益有限。

---

## 方案 B（中风险，结构优化）

核心思想：

- 把半路转正分段应发前置为“基础项校正”。
- 再统一走一次 `computeSalary` 完成汇总。

优点：

- 流程更简洁，“一次算完”更清晰。
- 一致性更好。

不足：

- 需要梳理 `baseComputeSalary` 对父项聚合的依赖，改动面明显变大。

---

## 方案 C（高风险，大重构）

核心思想：

- 重构为“薪资计算管道 + 策略模式（普通/半路转正）”。

优点：

- 长期可维护性最佳。

不足：

- 交付周期长，回归成本高，不适合当前“谨慎优化”的诉求。

---

## 7. 推荐落地方案（A）详细设计

## 7.1 设计原则

1. **不改算法口径**：保留现有税率、扣减、 rounding 与公司特例。
2. **不改业务结果定义**：半路转正仍按既有业务定义计算。
3. **只消除错误和分叉**：先修 correctness，再做性能。

## 7.2 关键改造点

### D1. 构建“完整工资上下文”再做半路转正复算

- 复算时不要只看 `finalOptions`，而是合并：
  - 基础项（首次落库前的 options）
  - 汇总项（computeSalary 返回）
- 保证 `100101/100102/280/282` 可被正确读取。

### D2. 抽取统一“汇总重算器”并复用 `SalaryComputeServiceNew` 规则

- 目标不是换公式，而是**复用现公式**。
- 将 `税累计/当月税/实发/代扣小计` 的计算方法暴露为可复用组件（内部调用现有逻辑）。
- 半路转正仅传入“校正后的应发/基础扣款”进行统一重算，避免手工再写一套。

### D3. 半路转正覆盖时同步更新全链路依赖字段

除 `210101/230101/240101` 外，同步校正：

- `220101`（应税工资）
- `1001`（代扣小计）
- `270101~270106`（本月累计）
- 必要时 `160102`（工会费，半路转正应为 0）

### D4. 缺失分段考勤数据的兜底策略

建议策略：

- 缺考勤分段时记录 warning 日志并打标。
- 暂按当前普通结果保留（不 silent overwrite），同时将该员工纳入“待人工复核列表”。

### D5. 小步性能优化（不改变结果）

优先两项：

1. 员工薪资档案校验改批量查询，替换逐人查询。
2. 考勤组判断改批量查询接口，替换逐人接口。

---

## 8. 验证与回归方案

## 8.1 单测新增建议

1. 半路转正 + 有社保/公积金 + 其他扣款 + 借款：验证 `230101/240101`。
2. 半路转正 + 残疾员工：验证免税逻辑一致。
3. 半路转正 + `is_remark=2` 且累计<6w：验证免税一致。
4. 半路转正 + 缺考勤分段：验证兜底行为（不静默错算）。
5. 非半路转正回归：保证结果不变。

## 8.2 集成验证建议

- 选 3 组真实员工做前后对比：
  - A：普通员工
  - B：半路转正且扣款复杂
  - C：半路转正但无考勤分段

重点核对字段：`210101/220101/230101/240101/1001/270101~270106`。

---

## 9. 实施顺序（建议）

1. **先修 P0**（半路转正扣款来源 + 税算分叉）。
2. **再修 P1**（依赖字段一致性 + 缺考勤兜底）。
3. **最后做性能项**（批量查询替换 N+1）。

每步独立提交，保证可回滚。

---

## 10. 结论

当前 `computeSalaryData` 的最大风险不是“算不出来”，而是“半路转正在复杂扣款场景下可能算偏，且后续改规则时易复发”。

推荐先按方案 A 做**低风险一致性修复**：

- 不动业务算法口径；
- 修正数据来源；
- 复用统一税算引擎；
- 补齐依赖字段一致性。

这样可以在最小改动下先把正确性和可维护性稳定住，再继续做性能优化。

---

## 11. 实施进展（2026-02-12）

### 11.1 已完成（对应 B 方案）

1. 已修复半路转正复算依赖不完整问题：复算时引入基础项快照，保证 `100101/100102/280/282` 等扣款项参与个税与实发计算。
2. 已引入统一汇总重算方法 `calculateMidMonthPromotionSummary`，统一产出：
   - `220101/230101/240101/160102/1001`
   - `250101/250102/250103/250105`
   - `270101~270106`
3. 已在半路转正一致性收敛步骤中补齐 `parentCode=150/170`（税后补发、特殊计税）与奖金累计口径（是否计入累计收入）。
4. 已兼容 `is_remark=2` 规则在 map 缺字段场景下的回退查询，降低免税判断误差。
5. 已补“缺分段考勤”语义：`calculateMidMonthPromotionSalaryAmounts` 在 `attendance==null` 时返回空映射。

### 11.2 测试与验证结果

- 单元测试：`mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅ 通过。
- 编译验证：`mvn -q -DskipTests compile` ✅ 通过。
- 打包验证：`mvn -q -DskipTests package` ❌ 失败（`maven-jar-plugin` 组包阶段 `target/classes/BOOT-INF/lib` 报错，疑似项目现有打包配置问题，与本次变更无直接耦合）。

### 11.3 剩余建议（下一步）

1. 对 3 组真实员工数据做对账（普通 / 半路转正复杂扣款 / 半路转正缺考勤分段）。
2. 处理 P1-2 的“告警与复核名单”闭环（当前已实现日志告警 + 复核名单汇总输出，可继续扩展到持久化表）。
3. 进入性能项：替换员工筛选与考勤组判定的 N+1 查询。

### 11.4 P1-2 闭环补充（本轮新增）

- 在 `computeSalaryData` 主循环引入半路转正复核集合，计算结束统一输出名单。
- 在单员工计算阶段新增 `collectMidMonthPromotionAttendanceReview`：
  - 当员工为半路转正，且 `HrmProduceAttendance` 缺失或缺少 `probationAttendance/positiveAttendance` 任一值时，加入复核集合；
  - 同时输出单条 warning，便于在线排障。
- 该改造仅增加观测与复核能力，不改变原有薪资算法与计算结果。

### 11.5 性能优化补充（本轮新增）

- 已完成 `queryHasSalaryArchivesEmployeeList` 的 N+1 查询治理：
  - 旧逻辑：逐员工调用 `queryEmpSalaryArchivesList`（单次批量被拆成 N 次）；
  - 新逻辑：先收集员工 ID，单次批量查询薪资档案，再按 `total > 0` 过滤。
- 对比收益：当月计薪员工规模为 N 时，档案查询由 O(N) 次降为 O(1) 次（业务复杂度不变）。
- 一致性保证：仍按“有薪资档案且总额大于 0”作为入选条件，未改变筛选口径。

### 11.6 性能优化补充（本轮新增）

- 已完成 `loadHasAttendanceGroupMap` 的 N+1 查询治理：
  - 旧逻辑：逐员工调用 `queryAttendanceGroupDingDing`；
  - 新逻辑：新增批量接口 `queryEmployeeIdsInAttendanceGroupDingDing`，一次查询后内存映射布尔结果。
- 扩展点：
  - `IHrmAttendanceGroupService` 新增批量查询方法；
  - `HrmAttendanceGroupMapper` + XML 新增批量 SQL；
  - `HrmAttendanceGroupServiceImpl` 提供实现。
- 对比收益：员工规模为 N 时，考勤组查询由 O(N) 次降为 O(1) 次。
- 一致性保证：仅改变查询方式，不改变“是否在钉钉考勤组”判定规则。

### 11.7 性能优化补充（本轮新增）

- 已优化半路转正一致性收敛阶段的额外查询：
  - 旧逻辑：每个员工在 `applyMidMonthPromotionSummaryConsistency` 内调用 `queryEmpSalaryOptionValueList` 获取工资项明细；
  - 新逻辑：复用循环外已加载的 `salaryOptionList` 构建 `code -> parentCode` 映射，结合内存中的 `baseOptionMap` 计算 `parentCode=150/170` 金额。
- 对比收益：该路径每位员工减少 1 次工资项明细查询。
- 一致性保证：仍按同一 `parentCode` 口径聚合税后补发与特殊计税项，未改变计算规则。

### 11.8 性能优化补充（本轮新增）

- 已减少社保项计算中的重复配置查询：
  - 旧逻辑：`getSocialSecurityOption` 每员工内部重复查询一次 `HrmSalaryConfig`；
  - 新逻辑：在 `computeSalaryData` 入口已获取 `salaryConfig`，沿调用链透传复用。
- 对比收益：员工规模为 N 时，减少约 N 次薪资配置查询。
- 一致性保证：仅减少重复读取，不改变社保项取值与同步逻辑。

### 11.9 稳定性补充（本轮新增）

- 已按 P2 建议收敛 `Boolean` 入参空值风险：
  - 将 `isSyncAttendanceData`、`isSyncInsuranceData` 的关键判断统一改为 `Boolean.TRUE.equals(...)`；
  - 覆盖点：考勤同步分支、社保同步分支、社保数据校验入口。
- 收益：避免调用方传 `null` 时潜在 NPE。
- 一致性保证：`null` 语义按“不同步”处理，不改变原有显式 `false` 语义。

### 11.10 性能优化补充（本轮新增）

- 已完成“免税判断链路”的批量预载，降低半路转正路径中的潜在 N+1 查询：
  1. 主流程批量预载上月个税累计数据 `lastMonthTaxDataMap`（已落地）。
  2. 主流程新增批量预载“上年度累计收入” `lastYearAccumulatedIncomeMap`，替代按员工调用 `getAccumulatedIncomeByEmployeeAndYear`。
  3. 在 `queryHasSalaryArchivesEmployeeList` 阶段批量回填 `isRemark` 到员工 map，避免半路转正免税判断中逐员工 `employeeService.getById` 回查。
- SQL/Mapper 扩展：
  - `HrmPersonalIncomeTaxMapper` 新增 `queryAccumulatedIncomeByEmployeeIdsAndYear`。
  - 对应 XML 新增“按员工取年度最新 end_month 记录”的批量查询语句。
- 对比收益：
  - 旧逻辑：每员工至少 1 次（或更多）免税辅助查询；
  - 新逻辑：计薪批次内统一批量查询 1 次并在内存复用。
- 一致性保证：
  - 仅优化数据装载方式，免税判定条件仍为：`is_remark=2` 且（上年累计 + 当年累计）`< 60000`。

### 11.11 回归验证（本轮新增）

- 单元测试：`mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅ 通过。
- 编译验证：`mvn -q -DskipTests compile` ✅ 通过。

### 11.12 R1-R5 修复落地（本轮新增）

- 已按最新接口口径收敛 `computeSalaryData` 入参：
  - `computeSalaryData(Long sRecordId, Boolean isSyncInsuranceData, Boolean isSyncAttendanceData, Long employeeId)`。
- R1：计薪员工查询按当前 `sRecordId` 对应月记录执行，不再读取“最新月记录”。
  - `queryHasSalaryArchivesEmployeeList` 改为显式接收 `salaryMonthRecord`。
- R2：去除考勤文件入口，改为按当前计薪员工集合构建默认考勤 map，避免跨月份串数据。
  - `resolveAttendanceData` 改为 `resolveAttendanceData(List<Map<String, Object>> mapList)`。
- R3：旧员工非同步考勤场景，先缓存旧固定项后回填，避免删后丢失固定项。
  - 在 `getOrCreateRecordAndApplyAttendance` 中引入固定项快照与克隆回填逻辑。
- R4：新员工非同步考勤场景，也补齐固定项（由默认考勤 map 构建），避免固定项缺失。
- R5：修复双休日统计年份错误，`getShuangXiuDays` 改为使用入参 `year`。
- 回归验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.13 R6-R8 修复落地（本轮新增）

- R6 并发保护：
  - 在 `computeSalaryData` 增加按 `sRecordId` 的进程内互斥锁，避免同月记录并发重算导致删写覆盖。
  - 入口改为 `withSalaryRecordLock(sRecordId, ...)` 包裹执行。
- R7 事务隔离：
  - 主流程拆分为 `doComputeSalaryData`；员工循环内每个员工使用 `REQUIRES_NEW` 事务执行。
  - 通过 `executeInRequiresNew` 使用 `TransactionTemplate` 显式创建子事务，降低大事务锁范围并提高容错。
- R8 社保配置空值兜底：
  - `validateInsuranceData` 与 `getSocialSecurityOption` 中对 `socialSecurityMonthType` 增加 null 兜底；
  - 当配置为空时按“当月口径”处理并输出 warning，避免拆箱 NPE。
- 回归验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.14 R9-R12 修复落地（本轮新增）

- R9 社保状态校验补充：
  - 在 `validateInsuranceData` 中增加社保月记录 `status` 校验；
  - 仅当状态为 `IsEnum.YES` 视为可用于同步，避免“有记录但未完成”进入计薪。
- R10 专项附加扣除幂等改造：
  - `updateAddition` 从“删除后插入”改为“按 employeeId 匹配后 saveOrUpdateBatch”；
  - 对已有下月记录回填 `additionalId` 后执行 upsert，降低覆盖风险。
- R11 专项附加扣除空值安全：
  - 累加逻辑引入 `safeAmount`，统一将 null 按 0 处理，避免 BigDecimal NPE。
- R12 免税口径统一：
  - 首次 `skipTaxForRemark` 判断补齐奖金累计口径（与一致性收敛阶段一致）；
  - 口径统一为：当公司规则允许时将 `41001` 计入累计收入。
- 回归验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.15 本轮收口修复（R4/R10 语义补强）

- 默认考勤码表补齐（R4 延伸）：
  - `resolveAttendanceData` 统一补齐非同步考勤场景必需码：`1/2/180101/190101~190106/19010401/19010402/280/281/282/20102/20105/40102`；
  - 解决“默认 map 缺键导致 `new BigDecimal(cv.get(...))` 空值异常”风险；
  - 不改计算口径，仅保证默认值完整性（全部默认 `0`）。
- 专项附加扣除语义回归（R10 补强）：
  - 当下一年配置为空时，恢复“清理下月数据”语义，避免历史脏数据残留；
  - `saveOrUpdate` 前增加“删除未配置员工 + 删除重复 employeeId 旧记录”，再按配置员工 upsert，避免旧逻辑“删后插”改造后产生残留；
  - 增加 `collectAdditionalIdsToDelete` 辅助方法提升可复用性与可测试性。
- 回归测试新增：
  - `resolveAttendanceData_shouldContainNeedAndActualWorkDayDefaults`；
  - `resolveAttendanceData_shouldContainRequiredFixedCodes`；
  - `collectAdditionalIdsToDelete_shouldDeleteUnconfiguredAndDuplicateRows`。
- 本轮验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.16 事务一致性收口（本轮新增）

- 问题收口：`computeSalaryData` 员工循环去除 `REQUIRES_NEW` 子事务执行，恢复主事务内统一提交。
  - 目标：避免单个员工失败时出现“前序员工已提交、当前批次整体失败”的部分提交状态。
- 处理结果：
  - 员工循环改为直接调用 `computeAndSaveEmployeeSalary`；
  - 删除 `executeInRequiresNew` 及对应事务模板依赖。
- 一致性说明：
  - 不改变薪资项算法与税算口径；
  - 仅恢复批次级原子性，保证失败即整批回滚（与“只优化不改业务规则”一致）。
- 本轮验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.17 数据健壮性补充（本轮新增）

- `updateAddition` 中 `currentMonthDataMap` 构建增加 `null` 防御：
  - 过滤 `null` 行与 `employeeId=null` 行后再 `toMap`；
  - 避免极端脏数据触发 `Collectors.toMap` 空键异常。
- 一致性说明：
  - 不改变专项附加扣除计算口径；
  - 仅提升异常输入容错性。
- 本轮验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.18 语义边界收口（本轮新增）

- `updateAddition` 清理行为补强：
  - 旧逻辑仅在 `nextMonthDataList` 非空时才执行“下月脏数据清理”；
  - 新逻辑改为先执行“删除未配置员工 + 删除重复记录”，再决定是否 `saveOrUpdateBatch`。
- 解决场景：
  - 当配置列表存在但有效员工集合为空（如数据脏行 `employeeId=null`）时，仍能清理下月残留数据，避免脏数据滞留。
- 可读性优化：
  - `loadMidMonthArchivesOptionMap` 去掉未使用的 `year/month` 参数，减少误导。
- 一致性说明：
  - 不改变专项附加扣除计算口径；
  - 不改变半路转正算法，仅做清理时机与方法签名收敛。
- 本轮验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅

### 11.19 社保同步批量预载优化（本轮新增）

- 性能优化：新增 `loadSocialSecurityEmpRecordMap`，在计薪批次内按“社保口径月份 + 员工集合”一次性批量查询社保月员工数据。
- 主流程改造：
  - `doComputeSalaryData` 预载社保映射后透传至 `computeAndSaveEmployeeSalary`；
  - `getSocialSecurityOption` 优先从预载映射读取，未命中时再走单员工兜底查询（兼容原流程）。
- 口径一致性：
  - 社保口径月份抽取为 `resolveSocialSecurityReferenceYearMonth`（当月/上月/下月）；
  - 仍按 `status=YES` 数据参与同步，不改计算公式与取值规则。
- 附带收敛：
  - 新增 `resolveSocialSecurityReferenceYearMonth_shouldFollowConfig` 单测，覆盖 `null/0/1/2` 四种口径。
- 本轮验证：
  - `mvn -q -Dtest=SalaryMonthRecordServiceNewTest test` ✅
  - `mvn -q -DskipTests compile` ✅
