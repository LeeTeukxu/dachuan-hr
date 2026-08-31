# Overtime Night Approval Priority Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让加班/夜班统计在命中本地钉钉加班审批数据时，优先按审批数据统计加班小时，并正确处理同员工多条审批累加与同名员工隔离。

**Architecture:** 保持现有“按员工逐月生成日明细再汇总”的统计结构不变，在 `HrmOvertimeNightStatisticsServiceImpl` 的月度明细计算阶段补充“本地审批加班聚合”来源。审批来源通过 `employeeId -> tbattendanceuser.userId` 映射绑定到员工，再按 `tbattendanceapprove` 的业务日期与时长聚合到日级明细；命中审批日时覆盖原有自动计算的加班小时，但夜班次数、计划/实际下班时间仍沿用现有打卡/排班链路。

**Tech Stack:** Java, Spring Boot, Spring Data JPA, JUnit4, Mockito

---

### Task 1: Lock the new behavior with failing tests

**Files:**
- Modify: `src/test/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImplTest.java`

**Step 1: Write the failing tests**

Add tests for:
- monthly statistics should prefer local DingTalk overtime approvals over auto-calculated overtime
- multiple approval rows for the same employee and same day should be accumulated
- employees sharing the same name should only use approvals matched by their own DingTalk `userId`

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldPreferAttendanceApprovalOverAutoCalculatedOvertimeWhenApprovalExists,HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldAccumulateMultipleAttendanceApprovalsForSameEmployeeAndDay,HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldIgnoreSameNameOtherEmployeesAttendanceApprovals -DfailIfNoTests=false test
```

Expected:
- at least one assertion fails because production code does not yet read `tbattendanceapprove`

### Task 2: Add approval aggregation to the statistics service

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/repository/tbattendanceapproveRepository.java`

**Step 1: Add repository query support**

Add a query method that reads local approval rows for a set of DingTalk `userId`s in the target month range.

**Step 2: Implement minimal aggregation**

Inside `HrmOvertimeNightStatisticsServiceImpl`:
- inject `tbattendanceapproveRepository`
- resolve current employee DingTalk `userId`
- load that employee's local overtime approvals for the month
- parse `duration + durationUnit` into hours
- group by business day using `workDate -> beginTime -> endTime`
- sum multiple approval rows on the same day

**Step 3: Apply approval priority**

When building each day detail row:
- if the current day has positive approval overtime hours, use that value as `overtimeHours`
- otherwise keep the existing overtime computation result
- do not change night shift count logic

**Step 4: Keep behavior scoped**

Do not switch matching to employee name. Matching must stay `employeeId -> tbattendanceuser.userId -> tbattendanceapprove.userId`.

### Task 3: Verify and document the new rule

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `task_plan.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Step 1: Run the targeted tests**

Run:
```bash
mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test
```

Expected:
- new tests pass
- existing overtime/night statistics tests stay green

**Step 2: Update documentation**

Record:
- approval-priority business rule
- same-day accumulation rule
- same-name employee isolation rule
- which statistics flows now use local approval overtime data
