# Attendance Approval Data Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在考勤管理模块新增“审批数据”独立功能页，只读取本地同步审批入库表 `tbattendanceapprove`，展示各种审批数据且严禁调用钉钉接口。

**Architecture:** 后端新增独立“审批数据”查询接口，使用 MyBatis 以 `tbattendanceapprove` 为主表，关联 `tbattendanceuser`、`hrm_employee`、`hrm_dept` 补齐员工中文信息；前端在考勤管理下新增菜单、API 和只读分页列表页，复用现有 `Table`、`appFilter`、`deptSelect` 交互模式。整个实现链路只依赖本地数据库，不复用任何钉钉拉取逻辑。

**Tech Stack:** Java, Spring Boot, MyBatis XML, JUnit4, Mockito, Vue3, Element Plus

---

### Task 1: Define backend query contract

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/entity/bo/QueryAttendanceApprovalPageBO.java`
- Create: `src/main/java/com/tianye/hrsystem/entity/vo/QueryAttendanceApprovalPageVO.java`

**Step 1: Write the failing test**

Add a service/controller unit test that expects:
- request supports `page/limit/search/deptIds/bizTypes/times`
- response row contains `approvalId/employeeName/jobNumber/deptName/post/tagName/subType/bizType/beginTime/endTime/duration/durationUnit/workDate/createTime`

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`
Expected: FAIL because BO/VO/service/controller do not exist.

**Step 3: Write minimal implementation**

Create BO and VO classes with the exact fields required by the test.

**Step 4: Run test to verify it passes or advances**

Run: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`
Expected: earlier class-not-found failure disappears; remaining failures move to service/controller wiring.

### Task 2: Add backend query service and mapper

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/service/IHrmAttendanceApprovalService.java`
- Create: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImpl.java`
- Create: `src/main/java/com/tianye/hrsystem/mapper/HrmAttendanceApprovalMapper.java`
- Create: `src/main/resources/mapper/HrmAttendanceApprovalMapper.xml`

**Step 1: Write the failing test**

In `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImplTest.java`, assert:
- service paginates local approval rows
- service only delegates to mapper
- no DingTalk dependency is involved

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest -DfailIfNoTests=false test`
Expected: FAIL because service/mapper are missing.

**Step 3: Write minimal implementation**

Implement:
- service method `queryPageList(QueryAttendanceApprovalPageBO queryBO)`
- mapper method `queryPageList(BasePage<QueryAttendanceApprovalPageVO> page, @Param("data") QueryAttendanceApprovalPageBO queryBO)`
- SQL joining:
  - `tbattendanceapprove a`
  - `left join tbattendanceuser u on a.userId = u.userId`
  - `left join hrm_employee e on u.empId = e.employee_id`
  - `left join hrm_dept d on e.dept_id = d.dept_id`
- filters:
  - `search` on `employee_name/job_number`
  - `deptIds`
  - `bizTypes`
  - `times` on `beginTime`
- default ordering: `a.beginTime desc, a.createTime desc`

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest -DfailIfNoTests=false test`
Expected: PASS

### Task 3: Add backend controller endpoint

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalController.java`
- Test: `src/test/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalControllerTest.java`

**Step 1: Write the failing test**

Assert controller:
- path is `/hrmAttendanceApproval/queryPageList`
- returns `Result<BasePage<QueryAttendanceApprovalPageVO>>`
- delegates exactly once to service

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`
Expected: FAIL because controller is missing.

**Step 3: Write minimal implementation**

Create controller tagged `考勤管理-审批数据`, inject service, expose query endpoint only.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`
Expected: PASS

### Task 4: Add frontend API and menu entry

**Files:**
- Modify: `../hr_web/src/router/config.js`
- Create: `../hr_web/src/api/hrm/attendance/approval.js`
- Create: `../hr_web/tests/attendance-approval-api.test.mjs`

**Step 1: Write the failing test**

Assert frontend API calls:
- `POST /api/hrsystem/hrmAttendanceApproval/queryPageList`

**Step 2: Run test to verify it fails**

Run: `node ../hr_web/tests/attendance-approval-api.test.mjs`
Expected: FAIL because API file/test target is missing.

**Step 3: Write minimal implementation**

Add:
- menu route `/hrm/attendance/approval`
- API helper `queryAttendanceApprovalPageList`

**Step 4: Run test to verify it passes**

Run: `node ../hr_web/tests/attendance-approval-api.test.mjs`
Expected: PASS

### Task 5: Add frontend approval data page

**Files:**
- Create: `../hr_web/src/views/hrm/attendance/approval/Index.vue`

**Step 1: Write the failing test**

If a page-level test is too heavy, use existing API-level tests plus `npm run build` as verification target for this step.

**Step 2: Run test to verify it fails**

Run: `npm run build`
Expected: FAIL if route points to a missing component.

**Step 3: Write minimal implementation**

Implement a page consistent with existing attendance pages:
- title `审批数据`
- filters: search, month picker, dept select, bizType select
- table columns:
  - 姓名
  - 工号
  - 部门
  - 岗位
  - 审批类型
  - 审批子类型
  - 开始时间
  - 结束时间
  - 时长
  - 单位
  - 申请日期
  - 同步时间

**Step 4: Run test to verify it passes**

Run: `npm run build`
Expected: PASS

### Task 6: Documentation and verification

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `../hr_web/docs/requirements.md`
- Modify: `../hr_web/docs/development.md`

**Step 1: Write the verification checklist**

Document:
- data source is local `tbattendanceapprove`
- no DingTalk API is called by the new query path
- backend/frontend verification commands

**Step 2: Run verification**

Run:
- `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`
- `node ../hr_web/tests/attendance-approval-api.test.mjs`
- `npm run build`

Expected:
- all tests pass
- build succeeds

**Step 3: Commit**

Do not commit unless the user requests it.
