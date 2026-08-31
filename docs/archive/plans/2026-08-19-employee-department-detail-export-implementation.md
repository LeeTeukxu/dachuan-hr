# Employee Department Detail Export Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an employee-management download action that exports all undeleted employees into a multi-sheet department detail Excel workbook.

**Architecture:** The backend adds a dedicated `/hrmEmployee/exportDepartmentDetail` endpoint and service method. `HrmEmployeeServiceImpl` gathers all undeleted employees, dynamic salary-level fields, salary archive option values, and top-level organization name; a new POI support class builds the workbook and filename. The frontend adds an API helper and a dropdown button with independent loading state.

**Tech Stack:** Spring Boot, MyBatis/MyBatis-Plus, Apache POI, Vue 3, Element Plus, Node source tests, JUnit 4.

**Status:** Completed on 2026-08-19. Verified with focused backend tests, backend compile, frontend source tests, and frontend production build. The implementation also includes workbook-level department sheet name de-duplication.

**2026-08-19 Addendum:** Department sheets now also match the reference workbook top summary area. Rows 1-8 include department count, gender ratio, education distribution, company-age distribution, major summary, probation/formal/intern summary, and the right-side cost labels with blank values because department cost has no confirmed data source.

**2026-08-20 Addendum:** Employee dynamic field `固定绩效` now participates in department-detail salary treatment display and monthly fixed salary cost. When present, salary treatment is rendered as `<固定薪资>(固定)+<固定绩效>(绩效)`; monthly fixed salary cost sums `10101/10102/10103 + 固定绩效`. Monthly performance salary cost remains based only on salary archive option `41001 / 绩效工资`.

---

### Task 1: Backend Excel Support

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupport.java`
- Test: `src/test/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupportTest.java`

**Steps:**
1. Write failing tests for:
   - headers equal `序号、姓名、岗位、入职年限、性别、年龄、学历、专业、薪资级别、薪资待遇、合同到期日`
   - score headers are absent
   - salary treatment sums codes `10101/10102/10103`
   - filename format is `<顶层组织名称>人员明细表yyyyMMdd.xlsx`
2. Run `mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` and confirm RED.
3. Implement the support class with POI workbook generation, row mapping, money parsing, date formatting, and filename helper.
4. Re-run the same Maven test and confirm GREEN.

### Task 2: Backend Endpoint And Service

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/controller/HrmEmployeeController.java`
- Modify: `src/main/java/com/tianye/hrsystem/service/employee/IHrmEmployeeService.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/mapper/HrmEmployeeMapper.java`
- Modify: `src/main/resources/mapper/HrmEmployeeMapper.xml`
- Test: `src/test/java/com/tianye/hrsystem/controller/HrmEmployeeControllerTest.java`
- Test: `src/test/java/com/tianye/hrsystem/mapper/HrmEmployeeMapperSqlTest.java`

**Steps:**
1. Write failing tests for:
   - controller delegates `/exportDepartmentDetail`
   - mapper SQL exists, filters only `a.is_del = 0`, includes education major, and does not include `employeeListCondition`
2. Run focused tests and confirm RED.
3. Implement mapper query, service method, and controller endpoint.
4. Re-run focused tests and `mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test`.

### Task 3: Frontend API And UI

**Files:**
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/api/hrm/employee/employee.js`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/employee/Index.vue`
- Test: `/Users/jiangyongming/Project/hr/hr_web/tests/employee-department-detail-export-api.test.mjs`
- Test: `/Users/jiangyongming/Project/hr/hr_web/tests/employee-department-detail-export-ui.test.mjs`

**Steps:**
1. Write failing Node tests for API endpoint, responseType blob, button text, loading state, and no page filter payload.
2. Run the new Node tests and confirm RED.
3. Add API helper and UI handler.
4. Re-run the new tests plus existing employee export/layout tests.

### Task 4: Documentation And Verification

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`
- Modify: `task_plan.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Steps:**
1. Record implementation files, endpoint, frontend entry, and tests.
2. Run focused backend and frontend tests.
3. Run `mvn -DskipTests compile` if backend focused tests compile cleanly.
4. Run `npm run build` in `hr_web` if frontend focused tests pass.
