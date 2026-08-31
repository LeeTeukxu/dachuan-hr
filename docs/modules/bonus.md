# 奖金中心（奖金导入 / 只计税奖金）

菜单前缀：`/hrmBonus`；菜单权限 `/hrm/bonus/payroll`（发放）、`/hrm/bonus/taxOnly`（只计税），旧 `/hrm/bonus/index` 兼容重定向。

## 需求要点
- 奖金中心为父菜单 + 两个子菜单（不是一个页面两个按钮）：`上传奖金(累加至工资计税)` 调 `POST /hrmBonus/importBonus` 入 `hrm_bonus`；`上传奖金(只计税)` 调 `POST /hrmBonus/importTaxOnlyBonus` 入 `hrm_bonus_tax_only`。
- Excel 解析规则两类共用：第 1 列员工姓名、第 2 列奖金金额、第 3 行起读明细；上传某一类只清理并覆盖该类型对应年月数据，不互相覆盖。
- 只计税奖金只进入个税累计收入/累计应纳税所得额/个人所得税，不写工资项 `41001`、不加应发 `210101`；实发 `240101` 仅因个税增加而减少。
- 奖金列表按员工年月 union 两表展示，只有只计税记录时也可见；成都公司累计收入口径继续排除发放奖金但包含只计税奖金。
- 员工"其他补助"为员工管理薪资动态字段（见 employee.md），不属于奖金模块。

## 设计与契约
- 表：`hrm_bonus`（发放）、`hrm_bonus_tax_only`（只计税，含员工年月索引；DDL `docs/sql/2026-06-18_hrm_bonus_tax_only.sql`）。
- 服务：`HrmBonusService#resolveBonusData / resolveTaxOnlyBonusData`（复用同一 Excel 解析）；计税：`SalaryComputeServiceNew#getTaxOnlyBonusSalary / calculateCumulativeIncome`；半路转正复算 `calculateMidMonthPromotionSummary` 显式加入 `taxOnlyBonusSalary` 参数，两条链路口径一致。
- 菜单/权限：`ApiPermissionPathSupport` 将 `importBonus→/hrm/bonus/payroll`、`importTaxOnlyBonus→/hrm/bonus/taxOnly`、列表查询两路径均可；`tbmenu` 600=发放、601=只计税，原拥有 600 的角色已补授 601。

## 近期变更
- 2026-06-18 双表只计税上传：新增 `HrmBonusTaxOnly` 实体/Mapper 与 `importTaxOnlyBonus` 接口；`queryHrmBonusList` 改 union 查询。
- 2026-06-18 菜单拆分：权限路径拆分 + `tbmenu` 600 改名与 601 新增，`hr_0001~hr_0005` 已同步（60 重定向 payroll）。
- 2026-06-18 薪资核算复算口径：`computeEmployeeSalary`/半路转正按员工年月读取 `getEmpTaxOnlyBonus` 进入个税累计收入；回归锁定"增个税不增应发"。

## 历史摘要
- 本模块自 2026-06-18 起才有专项轮次，无更早历史；早期仅有 `/hrm/bonus/index` 单上传页面（前端见 `hr_web/docs/modules/bonus.md`）。
