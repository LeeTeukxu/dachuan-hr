# Attendance Approval Employee Fetch Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 扩展“审批数据”页面的“获取审批数据”弹窗，支持按选中员工定向获取审批数据；未选择员工时保持“获取全部员工”行为。确认前仍需先检查本地是否已有该月份对应范围的数据，避免重复获取。

**Architecture:** 保持审批数据列表查询链路完全本地化，只读 `tbattendanceapprove`。手工获取链路继续单独走审批同步服务，但新增 `employeeIds` 入参。后端按“选中员工集合或全部考勤用户”决定钉钉拉取范围，并按相同范围执行本地月份判重；前端在弹窗中增加员工穿梭框，复用现有员工选择接口，将员工选择和请求参数组装抽成纯函数并用 Node 测试锁定。

**Tech Stack:** Spring Boot, JPA, DingTalk Java SDK, Vue3, Element Plus, axios, JUnit4, Node `.mjs` tests

---

### Task 1: 先补失败测试

**Files:**
- Modify: `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImplTest.java`
- Modify: `src/test/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalControllerTest.java`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs`
- Create: `/Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-fetch-utils.test.mjs`

**Step 1: 写失败测试**

- 后端服务测试新增断言：
  - `AttendanceApprovalMonthBO` 支持 `employeeIds`
  - `checkMonthData` 在传入员工集合时按员工范围判重
  - `fetchMonthData` 会把 `employeeIds` 透传到同步服务
- 控制器测试新增断言：
  - `employeeIds` 可透传到服务层
- 前端测试新增断言：
  - 检查/获取审批数据接口请求体允许携带 `employeeIds`
  - 纯函数正确归一化员工列表、组装请求体

**Step 2: 运行测试确认失败**

Run:

```bash
mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-fetch-utils.test.mjs
```

Expected:

- 因 `employeeIds` 字段、同步服务签名和前端纯函数尚不存在而失败。

### Task 2: 实现后端按员工范围获取

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/entity/bo/AttendanceApprovalMonthBO.java`
- Modify: `src/main/java/com/tianye/hrsystem/service/IHrmAttendanceApprovalService.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/service/IHrmAttendanceApprovalSyncService.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalSyncServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/repository/tbattendanceapproveRepository.java`

**Step 1: 最小实现**

- `AttendanceApprovalMonthBO` 增加 `employeeIds`
- 仓库增加“按月份 + userId 集合计数”能力
- 服务层：
  - 解析并归一化员工 ID 集合
  - 未选员工时维持整月全量判重
  - 已选员工时仅对这些员工执行月份判重
  - 获取时将 `employeeIds` 透传给同步服务
- 同步服务：
  - 支持 `fetchMonthData(YearMonth month, List<Long> employeeIds)`
  - `employeeIds` 为空时抓取全部 `tbattendanceuser`
  - `employeeIds` 非空时只抓取映射到 `tbattendanceuser` 的用户

**Step 2: 跑后端测试**

Run:

```bash
mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test
```

Expected:

- 后端测试通过

### Task 3: 实现前端穿梭框与纯函数

**Files:**
- Create: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/attendance/approval/approval-fetch-utils.js`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/attendance/approval/Index.vue`

**Step 1: 最小实现**

- 新增纯函数：
  - 归一化员工列表为穿梭框数据
  - 根据月份和已选员工组装检查/获取请求
- 页面弹窗增加：
  - 提示文案
  - 月份选择器
  - 员工穿梭框
- 复用员工接口加载待选员工
- 右侧未选择员工时表示“获取全部员工”

**Step 2: 跑前端测试**

Run:

```bash
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-fetch-utils.test.mjs
```

Expected:

- 前端测试通过

### Task 4: 构建、文档与验收

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`
- Modify: `task_plan.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Step 1: 验证**

Run:

```bash
mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs
node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-fetch-utils.test.mjs
npm run build
```

Expected:

- 后端测试通过
- 前端测试通过
- 前端构建通过

**Step 2: 文档同步**

- 记录“获取审批数据”支持按员工定向获取
- 明确“未选择员工 = 获取全部员工”
- 明确月份判重按当前选择范围执行
