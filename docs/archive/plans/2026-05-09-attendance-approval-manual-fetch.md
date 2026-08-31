# Attendance Approval Manual Fetch Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为“审批数据”页面增加“获取审批数据”按钮与月份弹窗，确认时先检查本地该月是否已有审批快照；若没有，再调用钉钉接口拉取并入库。

**Architecture:** 后端在现有审批查询接口旁新增“月份数据存在性检查”和“手工获取审批数据”两个接口。列表查询仍然只读本地 `tbattendanceapprove`；手工获取时复用现有钉钉 `attendance/getupdatedata` 审批入库逻辑，但只针对用户选定月份执行。前端在审批数据页新增按钮、弹窗、加载态和结果提示，并在获取成功后切换到所选月份重新查询。

**Tech Stack:** Spring Boot, JPA, MyBatis, DingTalk Java SDK, Vue3, Element Plus, axios, JUnit4, Node test scripts

---

### Task 1: 补后端失败测试

**Files:**
- Modify: `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImplTest.java`
- Modify: `src/test/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalControllerTest.java`

**Step 1: 写失败测试**

- 为服务层增加测试：
  - `checkMonthData_shouldReturnExistsFlagByRepositoryCount`
  - `fetchMonthData_shouldCallSyncServiceWhenMonthNotFetched`
  - `fetchMonthData_shouldRejectWhenMonthAlreadyFetched`
- 为控制器增加测试：
  - `checkMonthData_shouldDelegateToService`
  - `fetchMonthData_shouldDelegateToService`

**Step 2: 跑测试确认失败**

Run:

```bash
mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test
```

Expected:

- 因接口/方法/依赖尚不存在而失败。

### Task 2: 补前端 API 失败测试

**Files:**
- Modify: `/Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs`

**Step 1: 写失败测试**

- 增加：
  - `checkAttendanceApprovalMonthData`
  - `fetchAttendanceApprovalMonthData`
- 断言请求路径、请求体与 `silentError` 配置。

**Step 2: 跑测试确认失败**

Run:

```bash
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs
```

Expected:

- 因导出函数不存在而失败。

### Task 3: 实现后端月份检查与手工获取能力

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/entity/bo/AttendanceApprovalMonthBO.java`
- Create: `src/main/java/com/tianye/hrsystem/entity/vo/AttendanceApprovalMonthStatusVO.java`
- Create: `src/main/java/com/tianye/hrsystem/entity/vo/AttendanceApprovalFetchResultVO.java`
- Create: `src/main/java/com/tianye/hrsystem/service/IHrmAttendanceApprovalSyncService.java`
- Create: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalSyncServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/service/IHrmAttendanceApprovalService.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalController.java`
- Modify: `src/main/java/com/tianye/hrsystem/repository/tbattendanceapproveRepository.java`

**Step 1: 最小实现**

- 新增月份 BO/VO。
- `tbattendanceapproveRepository` 增加按 `beginTime` 月份计数方法。
- `HrmAttendanceApprovalServiceImpl` 增加：
  - 月份字符串解析；
  - 月份是否已有本地数据检查；
  - 手工获取前再次检查，防止重复拉取。
- `HrmAttendanceApprovalSyncServiceImpl`：
  - 读取全部 `tbattendanceuser`；
  - 逐日逐人调用钉钉 `topapi/attendance/getupdatedata`；
  - 只把 `approveList` 写入 `tbattendanceapprove`；
  - 返回本次新增审批条数。
- 控制器新增：
  - `/hrmAttendanceApproval/checkMonthData`
  - `/hrmAttendanceApproval/fetchMonthData`

**Step 2: 跑后端测试**

Run:

```bash
mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test
```

Expected:

- 测试通过。

### Task 4: 实现前端按钮、弹窗与调用链

**Files:**
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/api/hrm/attendance/approval.js`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/attendance/approval/Index.vue`

**Step 1: 最小实现**

- API 层新增：
  - `checkAttendanceApprovalMonthData`
  - `fetchAttendanceApprovalMonthData`
- 页面新增：
  - “获取审批数据”按钮；
  - 月份选择弹窗；
  - 确认按钮点击时：
    - 先调检查接口；
    - 若已存在，提示“所选月份已经获取过审批数据了”；
    - 若不存在，再调手工获取接口；
    - 成功后切换筛选月份并刷新列表。

**Step 2: 跑前端 API 测试**

Run:

```bash
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs
```

Expected:

- 测试通过。

### Task 5: 构建、文档与验收

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `task_plan.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Step 1: 验证**

Run:

```bash
mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test
```

Run:

```bash
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs
```

Run:

```bash
npm run build
```

Expected:

- 后端测试通过；
- 前端 API 测试通过；
- 前端构建通过。

**Step 2: 文档同步**

- 更新后端/前端需求与开发文档：
  - 列表查询仍只读本地表；
  - 手工获取按钮可在月份未入库时触发钉钉拉取；
  - 不新增审批表，继续复用 `tbattendanceapprove`。
