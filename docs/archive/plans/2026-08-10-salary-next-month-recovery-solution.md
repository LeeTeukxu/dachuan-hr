# 2026-07 薪资新建次月误推进解决方案

## 1. 问题

用户在 2026-07 薪资页面重复点击“新建次月薪资”：

- 第一次请求按 2026-07 创建 2026-08；
- 后续请求没有继续使用请求中的源月份，而是按最新 `create_time` 取到 2026-08，再创建 2026-09；
- 前端按钮没有提交中状态，且月份为空时会回退到系统当前月份。

## 2. 代码修复

- 前端 `SalaryManage.vue`：
  - “新建次月薪资”增加独立 `loading/disabled`；
  - 按当前选择的 `YYYY-MM` 提交源 `year/month`；
  - 月份为空时不再使用系统当前月份；
  - 新建成功后保留当前源月份，避免刷新后立即切到新建月份。
- 后端 `SalaryMonthRecordServiceNew` 和 `SalaryMonthRecordService_Bak`：
  - `updateCheckStatus(year, month)` 按请求源年月定位源记录；
  - 创建次月前先按目标年月查询，已存在时直接复用；
  - `addNextMonthSalary`、总经理审核通过分支统一复用该幂等逻辑。

## 3. 系统内恢复能力

当前系统已提供“恢复薪资月份”功能，不再依赖 DBA 手工删除。页面流程如下：

1. 在薪资管理页点击“恢复薪资月份”。
2. 选择目标月份并点击“预览”，系统会列出该月之后的所有薪资月记录及其依赖情况。
3. 只有当后续月份没有员工薪资明细、工资条记录、工资条明细，且未标记 `is_send=1` 时，恢复按钮才可点击。
4. 点击恢复后，系统只删除目标月份之后的空薪资月记录，并把目标月份恢复到可继续处理的状态。

## 4. 历史备份说明

本方案最初没有直接执行删除或更新数据库操作。若遇到极端故障且系统恢复接口无法使用，可仍按下面只读核查 SQL 辅助人工处理：

1. 暂停薪资页面的“新建次月薪资”操作，先备份目标租户库。
2. 只读核查 2026-07、2026-08、2026-09 月记录：

```sql
SELECT
    s_record_id,
    year,
    month,
    check_status,
    num,
    create_time,
    start_time,
    end_time
FROM hrm_salary_month_record
WHERE (year = 2026 AND month IN (7, 8, 9))
ORDER BY create_time, s_record_id;
```

3. 记录各月是否已有员工工资明细、工资项、工资条或已对外发送数据：

```sql
SELECT
    r.s_record_id,
    r.year,
    r.month,
    COUNT(e.s_emp_record_id) AS employee_record_count
FROM hrm_salary_month_record r
LEFT JOIN hrm_salary_month_emp_record e
       ON e.s_record_id = r.s_record_id
WHERE (r.year = 2026 AND r.month IN (7, 8, 9))
GROUP BY r.s_record_id, r.year, r.month;
```

4. 确认 2026-07 是业务源月份后：
   - 若 2026-08/2026-09 只是误创建的空记录，备份后由 DBA 按实际依赖关系清理；
   - 若 2026-08/2026-09 已有真实核算数据或已发送工资条，不直接删除，先保留并由业务确认是否需要回滚工资明细、工资项、工资条和发送记录；
   - 仅修改 `check_status` 不足以改变“最新薪资记录”查询，因为该查询按 `create_time` 取最新记录。
5. 数据处理完成后，进入薪资管理页选择 `2026-07`，确认页面的 `srecordId` 对应 7 月记录，再点击“新建次月薪资”一次。新代码会复用已有 2026-08，不会再次创建 2026-09。

## 5. 验证

- 后端：`mvn -Dtest=SalaryMonthRecordRecoveryTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordNextMonthTest test`
- 前端：`node tests/salary-month-recovery.test.mjs && node tests/salary-create-next-month.test.mjs`

上述定向测试当前均通过。系统内恢复功能已落地，数据库恢复 SQL 仅作为历史兜底参考。
