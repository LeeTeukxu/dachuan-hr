# 社保管理（社保方案 / 月度参保 / 险种 / 医疗长期护理 / 一键设置 / 停保名单）

菜单前缀：`/hrmInsurance*`（MonthRecord/MonthEmpRecord/Scheme/Project）、`/hrmRemainingVaction`（假期余额，未单列）。

## 需求要点
- 社保方案项目新增 `医疗长期护理保险`（type=12，在生育保险后）：支持启用/禁用（`is_enabled`，默认启用）并随方案保存；旧方案详情自动补默认启用行；月度参保编辑旧方案虚拟行先落真实 `hrm_insurance_project` 再写月度项目记录。
- 合计口径：个人/公司社保合计只统计已启用项目行（`coalesce(is_enabled,1)=1`）；不再从基本工资设置自动累加大额医疗 15 / 长期护理 3（该旧口径已废弃，`hrm_salary_basic` 两字段仅留作他用）。
- 一键设置/取消保险金额（`POST /hrmInsuranceMonthRecord/updateSalaryBasicInsuranceAmount`）：先按员工月度参保项目重算基础金额，再在 `include_salary_basic_insurance_amount=1` 时追加固定金额——个人加长期护理 3、公司加大额医疗 15；追加不受 type=12 是否启用限制，只看员工级开关；取消回到基础金额，幂等不重复累加。支持全员（`irecordId`）与选中员工（`iempRecordIds`）。
- 本月参保/停保名单：主列表返回 `insuredNum`（status=1）/`stopNum`（status=0）+ `insuredEmployeeNames/stoppedEmployeeNames`（GROUP_CONCAT，查询前调高 `group_concat_max_len=4194304`）；前端只显示数字、悬浮展示名单。
- 社保方案列表返回 `useEmployeeNames`（使用人数悬浮名单）。
- 高级筛选：默认当前年 1-12 月；月份时间段 `times=[YYYY-MM,YYYY-MM]`、部门 `deptIds`（金额与人数按命中员工聚合）；人员筛选项已删除（BO 字段保留兼容）。
- 生成次月报表提供真实进度：`/queryComputeInsuranceProgress`（IDLE/RUNNING/SUCCESS/FAILED × PREPARE/LOAD_EMPLOYEE/GENERATE_EMP/PERSIST/FINISH/ERROR，内存态 1h 过期）。

## 设计与契约
- 关键表：`hrm_insurance_scheme`（方案）、`hrm_insurance_project`（项目行，含 `is_enabled`）、`hrm_insurance_month_record`（月报主表）、`hrm_insurance_month_emp_record`（员工月记录，含 `include_salary_basic_insurance_amount`）、`hrm_insurance_month_emp_project_record`（员工月项目，含 `is_enabled`）。
- 员工参保关系：`hrm_employee_social_security_info.scheme_id`；批量设置参保走 `/hrmEmployee/updateInsuranceScheme`（见 employee.md）。
- 并发互斥：`computeInsuranceData` 按公司 `tryLock`（`INSURANCE_COMPUTE_LOCK_MAP`），重复点击立即报错。
- 字段名陷阱：Lombok `getIRecordId/getIEmpRecordIds` 被 Jackson 识别为 `irecordId/iempRecordIds`——对外 BO 用 `@JsonProperty` 固定 + `@JsonAlias` 兼容，前端提交小写 `irecordId/iempRecordIds`。
- DDL（发布前必须执行）：`2026-08-22_hrm_insurance_project_is_enabled.sql`、`2026-08-22_hrm_insurance_month_emp_include_salary_basic_insurance_amount.sql`（均已在 dev 5 租户库执行并复核）。

## 近期变更
- 2026-08-22 一键设置保险金额：接口/员工级开关/前端按钮上线；修复"社保记录不能为空"（Jackson 字段名）；修复 hr_0003 2026-06 88 条"金额已含 3/15 但开关=0"脏数据（SQL 对齐后验证设置/取消闭环）；确认追加不受 type=12 启用限制。
- 2026-08-22 医疗长期护理保险：type=12 项目行 + `is_enabled` 全链路（方案保存/月度编辑落库/合计过滤/自动补行），合计删除基本工资固定金额自动累加。
- 2026-08-21 参保/停保名单：`insuredNum/insuredEmployeeNames/stoppedEmployeeNames` + 方案 `useEmployeeNames`，前端 tooltip 化。

## 历史摘要
- 2026-07-16/17：基本工资设置新增大额医疗/长期护理固定金额并进入社保合计（该口径已于 2026-08-22 废弃）；社保报表生成真实进度。
- 2026-06-18/19：社保详情与加班总览隐藏页 `checkPath` 权限 + 多租户补权 SQL；主列表高级筛选（times/deptIds、默认当前年）；删除人员筛选项。
- 2026-07-17/18：农谷 hr_0003 社保方案与参保关系数据整理（7 月表/6 月工资表两轮，含回滚 SQL，见 changelog）；薪资"个人社保多 3 元"根因（历史固定金额叠加 + 月记录未随方案重整更新）。
- 更早细节见 `../archive/changelog.md`。
