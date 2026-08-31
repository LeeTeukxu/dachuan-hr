# 添加排班自定义班次 Implementation Plan

> 已被 2026-04-12 最新业务变更替代：当前自定义班次不再同步到钉钉，只保存到本地库并复用本地相同时间班次。以下计划保留作历史实施记录。

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `/workPlan/saveAll` 增加可影响钉钉考勤的自定义班次能力，并将提交流程改为可重试、可轮询进度、可按行返回中文错误的任务化模式。

**Architecture:** 后端保留既有接口路径，但把同步保存改为“创建任务 + 后台异步执行”。自定义时间通过“缓存/映射复用 -> 钉钉班次创建 -> 考勤组关联 -> 排班提交”解析成最终 `shiftId`，并通过任务进度接口把重试与失败明细返回前端。

**Tech Stack:** Spring Boot, JPA, Redis, DingTalk Java SDK 2.0.0, MySQL, Vue 3, Element Plus

---

### Task 1: 设计后端数据契约

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`
- Modify: `src/main/java/com/tianye/hrsystem/service/IWorkPlanService.java`
- Create: `src/main/java/com/tianye/hrsystem/entity/vo/WorkPlanSubmitProgressVO.java`
- Create: `src/main/java/com/tianye/hrsystem/entity/vo/WorkPlanSubmitResultVO.java`

**Step 1: 明确 `saveAll` 新入参模型**
- 定义 `shiftType/customStart/customEnd` 的读取与校验规则。
- 保持旧 `classId/groupId/userId/workDate` 提交兼容。

**Step 2: 增加进度查询接口契约**
- 新增查询任务状态接口。
- 返回任务状态、当前重试次数、成功/失败数量、中文消息、行级失败明细。

**Step 3: Run compile**
- Run: `mvn -DskipTests compile`
- Expected: 编译通过。

### Task 2: 新增持久化模型

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/model/HrmWorkPlanCustomShiftMap.java`
- Create: `src/main/java/com/tianye/hrsystem/model/HrmWorkPlanSubmitTask.java`
- Create: `src/main/java/com/tianye/hrsystem/model/HrmWorkPlanSubmitTaskDetail.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/hrmWorkPlanCustomShiftMapRepository.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/hrmWorkPlanSubmitTaskRepository.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/hrmWorkPlanSubmitTaskDetailRepository.java`
- Create: `docs/sql/2026-04-12_workplan_custom_shift.sql`

**Step 1: 定义三张表实体**
- 自定义班次映射表。
- 提交任务主表。
- 提交任务明细表。

**Step 2: 补齐仓库与唯一键约束设计**
- 映射表唯一键覆盖 `corpId + groupId + start + end + crossDay`。
- 明细表支持按任务和行号/员工回查。

**Step 3: Run compile**
- Run: `mvn -DskipTests compile`
- Expected: 编译通过。

### Task 3: 实现自定义班次解析器

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/imple/workplan/WorkPlanCustomShiftResolver.java`
- Create: `src/main/java/com/tianye/hrsystem/imple/workplan/WorkPlanDingTalkErrorTranslator.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`

**Step 1: 实现时间段归一化**
- 统一 `HH:mm`、跨天标记、自定义班次命中键。

**Step 2: 实现复用与创建链路**
- 先查 Redis。
- 再查映射表。
- 再查钉钉现有班次。
- 最后创建钉钉班次。

**Step 3: 实现中文错误翻译**
- 将钉钉原始报错统一转换为用户可读中文。

**Step 4: Run compile**
- Run: `mvn -DskipTests compile`
- Expected: 编译通过。

### Task 4: 实现任务化提交与重试

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/imple/workplan/WorkPlanSubmitTaskService.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`

**Step 1: 将 `saveAll` 改为创建任务**
- 同步校验后落主表与明细表。
- 返回 `taskId`。

**Step 2: 后台异步执行最小单元**
- 按“行 x 员工”拆分执行。
- 失败时后端自动重试，最多 `10` 次。

**Step 3: 记录行级失败结果**
- 至少写入行号、员工、考勤组、班次/时间段、中文原因。

**Step 4: Run compile**
- Run: `mvn -DskipTests compile`
- Expected: 编译通过。

### Task 5: 补齐展示缓存

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/controller/HrmAttendanceDataController.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`

**Step 1: 为 `getGoupList/getAllShifts` 增加 10 分钟缓存**
- 按 `corpId` 隔离。
- cache miss 时再调用钉钉。

**Step 2: 在自定义班次创建/挂组成功后主动失效相关缓存**
- 避免前端长时间看不到新班次。

**Step 3: Run compile**
- Run: `mvn -DskipTests compile`
- Expected: 编译通过。

### Task 6: 增加后端测试

**Files:**
- Create: `src/test/java/com/tianye/hrsystem/imple/WorkPlanCustomShiftResolverTest.java`
- Create: `src/test/java/com/tianye/hrsystem/imple/WorkPlanSubmitTaskServiceTest.java`
- Modify: `src/test/java/com/tianye/hrsystem/controller/WorkPlanListControllerTest.java`

**Step 1: 覆盖自定义时间归一化与跨天判定**

**Step 2: 覆盖标准班次精确命中复用**

**Step 3: 覆盖 10 次重试后任务失败与中文错误落库**

**Step 4: Run tests**
- Run: `mvn -Dtest=WorkPlanCustomShiftResolverTest,WorkPlanSubmitTaskServiceTest,WorkPlanListControllerTest -DfailIfNoTests=false test`
- Expected: 所有新增测试通过。

### Task 7: 前端配套改造

**Files:**
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/attendance/records/components/AddOrEdit.vue`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/api/hrm/attendance/workPlan.js`
- Create: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/attendance/records/record-shift-utils.js`

**Step 1: 新增 `班次类型` 列与自定义时间输入**

**Step 2: 提交后按 `taskId` 轮询进度**

**Step 3: 失败时按行展示中文错误且保留当前输入**

**Step 4: Run build**
- Run: `npm run build`
- Expected: 前端构建通过。

### Task 8: 文档与最终验证

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`

**Step 1: 同步最终实现与文档一致**

**Step 2: 执行最终验证**
- Run: `mvn -DskipTests compile`
- Run: `mvn -Dtest=WorkPlanCustomShiftResolverTest,WorkPlanSubmitTaskServiceTest,WorkPlanListControllerTest -DfailIfNoTests=false test`
- Run: `npm run build` in `/Users/jiangyongming/Project/hr/hr_web`
- Expected: 后端编译通过，新增测试通过，前端构建通过。

Plan complete and saved to `docs/plans/2026-04-12-workplan-custom-shift-implementation-plan.md`. Two execution options:

1. Subagent-Driven (this session) - I dispatch fresh subagent per task, review between tasks, fast iteration

2. Parallel Session (separate) - Open new session with executing-plans, batch execution with checkpoints

Which approach?
