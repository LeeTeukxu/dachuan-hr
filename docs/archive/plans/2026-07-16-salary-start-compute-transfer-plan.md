# 薪资管理开始核算穿梭框实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将薪资管理页核算入口收敛为“开始核算”，在弹窗内支持按人员或按部门选择核算范围。

**Architecture:** 前端弹窗负责展示人员/部门穿梭框，并把按部门选择展开为最终员工 ID 列表。后端 `computeSalaryData` 统一接收可选 `employeeIds`，为空时沿用全员核算，非空时只核算指定员工并限制最多 50 人。

**Tech Stack:** Spring Boot、MyBatis/MyBatis-Plus、Vue3、Element Plus、Node 测试、JUnit。

---

### Task 1: 前端范围工具函数

**Files:**
- Create: `hr_web/src/views/hrm/salary/salary/components/salary-compute-scope-utils.js`
- Create: `hr_web/tests/salary-compute-scope-utils.test.mjs`

**Steps:**
1. 写 RED 测试覆盖员工选项归一化、部门树扁平化、按部门展开员工、核算 payload 构建、50 人限制。
2. 运行 `node tests/salary-compute-scope-utils.test.mjs`，确认因文件/函数缺失失败。
3. 实现最小工具函数并重新运行测试。

### Task 2: 后端批量员工范围

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryMonthRecordController.java`
- Modify: `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java`

**Steps:**
1. 写 RED 测试覆盖 `normalizeComputeEmployeeIds` 去重、兼容旧 `employeeId`、超过 50 人抛业务异常、进度 key 按批量范围区分。
2. 运行 `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test`，确认失败。
3. 扩展服务方法签名和内部查询范围，保留旧单人入参兼容。
4. 控制器接收 `employeeIds` 请求参数并传入服务。

### Task 3: 薪资管理页弹窗

**Files:**
- Modify: `hr_web/src/views/hrm/salary/salary/SalaryManage.vue`
- Modify: `hr_web/src/views/hrm/salary/salary/components/AloneComputeDialog.vue`
- Modify: `hr_web/src/api/hrm/salary/salary.js`
- Test: `hr_web/tests/salary-start-compute-dialog.test.mjs`

**Steps:**
1. 写 RED 源码测试，要求页面只显示“开始核算”，不再显示“核算薪资/单人核算”按钮；弹窗包含 `el-transfer`、按人员/按部门切换和 50 人限制文案。
2. 实现弹窗加载 `queryComputeSalaryEmployeeList` 与部门树，按选择方式构建 payload。
3. 调用原进度弹层展示核算进度，并在核算成功后刷新列表。

### Task 4: 验证和文档

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `hr_web/docs/requirements.md`
- Modify: `hr_web/docs/development.md`
- Modify: `task_plan.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Steps:**
1. 运行前端定向 Node 测试。
2. 运行后端定向 Maven 测试。
3. 运行 `npm run build`。
4. 更新需求、开发文档和计划记录。
