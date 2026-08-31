# Workplan Product/Position/Employee Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a standalone production product management feature, then wire it into add-schedule so users can pick a product, load its positions, and load the position's employees before saving the schedule.

**Architecture:** Keep schedule records flat in `tbplanlist.productName/linkName`, and add a separate product hierarchy module for configuration only. The new module stores product -> position -> employee assignments, exposes a tree query plus CRUD endpoints, and the add-schedule page consumes that tree to drive product/position/employee selection. The scheduling edit dialog will show product, position, and workshop context using the existing schedule record plus the attendance group name.

**Tech Stack:** Spring Boot + Spring Data JPA + JUnit/Mockito; Vue 3 + Element Plus + Axios; SQL migration scripts under `docs/sql`.

---

### Task 1: Lock the data model with failing tests

**Files:**
- Create: `src/test/java/com/tianye/hrsystem/modules/workplan/WorkPlanProductServiceTest.java`
- Create: `src/test/java/com/tianye/hrsystem/modules/workplan/WorkPlanProductControllerTest.java`
- Create: `src/test/java/com/tianye/hrsystem/modules/workplan/WorkPlanProductMapperXmlTest.java`
- Create: `hr_web/tests/workplan-product-management.test.mjs`

**Step 1: Write the failing test**

Add tests that expect:
- a product tree query returns product -> position -> employee nodes
- save/delete endpoints exist for product and position nodes
- add-schedule can load product/position options from the new API
- the scheduling day edit payload exposes product, position, and workshop display fields

**Step 2: Run test to verify it fails**

Run:
`mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMapperXmlTest test`
Expected: FAIL because the new module does not exist yet.

Run:
`node hr_web/tests/workplan-product-management.test.mjs`
Expected: FAIL because the page and API do not exist yet.

**Step 3: Write minimal implementation**

Do not implement anything yet.

**Step 4: Run test to verify it passes**

Expected: still failing until implementation lands.

**Step 5: Commit**

Not required for this session.

### Task 2: Implement backend workplan product module

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/entity/HrmWorkPlanProduct.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/entity/HrmWorkPlanProductPosition.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/entity/HrmWorkPlanPositionEmployee.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/vo/WorkPlanProductTreeVO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/vo/WorkPlanPositionVO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/vo/WorkPlanPositionEmployeeVO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/controller/WorkPlanProductController.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/service/IWorkPlanProductService.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workplan/service/impl/WorkPlanProductServiceImpl.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/HrmWorkPlanProductRepository.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/HrmWorkPlanProductPositionRepository.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/HrmWorkPlanPositionEmployeeRepository.java`
- Create: `docs/sql/2026-08-22_hrm_workplan_product_position_employee.sql`
- Modify: `src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/entity/vo/WorkPlanEmployeeDayShiftVO.java`
- Modify: `src/main/java/com/tianye/hrsystem/model/tbplanlist.java`

**Step 1: Write the failing test**

Make the new tests expect concrete tree/query/save behavior and updated day-shift display fields.

**Step 2: Run test to verify it fails**

Run:
`mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMapperXmlTest test`
Expected: FAIL before the new tables/service/controller exist.

**Step 3: Write minimal implementation**

Add the new JPA entities, repositories, service, controller, and SQL script. Keep schedule records flat and only use the product hierarchy for configuration lookup.

**Step 4: Run test to verify it passes**

Run the same Maven command and make it pass.

**Step 5: Commit**

Not required for this session.

### Task 3: Build the standalone frontend management page

**Files:**
- Create: `hr_web/src/views/hrm/attendance/workplan-product/Index.vue`
- Create: `hr_web/src/api/hrm/attendance/workplan-product.js`
- Create: `hr_web/src/views/hrm/attendance/workplan-product/workplan-product-utils.js`
- Modify: `hr_web/src/router/config.js`
- Modify: `hr_web/src/views/hrm/attendance/Index.vue`
- Modify: `hr_web/src/main.js` only if the page needs shared global registration

**Step 1: Write the failing test**

Add a frontend test that expects:
- a product tree table/list
- add/edit/delete product and position actions
- employee assignment selection supports keyword search and manual input

**Step 2: Run test to verify it fails**

Run:
`node hr_web/tests/workplan-product-management.test.mjs`
Expected: FAIL before the page exists.

**Step 3: Write minimal implementation**

Implement the page with compact tree editing and batch employee assignment.

**Step 4: Run test to verify it passes**

Run the same Node test and make it pass.

**Step 5: Commit**

Not required for this session.

### Task 4: Wire add-schedule and edit display

**Files:**
- Modify: `hr_web/src/views/hrm/attendance/records/components/AddOrEdit.vue`
- Modify: `hr_web/src/views/hrm/attendance/records/components/work-plan-utils.js`
- Modify: `hr_web/src/views/hrm/attendance/scheduling/Scheduling.vue`
- Modify: `hr_web/src/views/hrm/attendance/scheduling/SchedulingDetail.vue`
- Modify: `hr_web/src/api/hrm/attendance/workPlan.js`
- Modify: `src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`

**Step 1: Write the failing test**

Add tests that expect:
- add-schedule first loads products, then positions for the selected product, then employees for the selected position
- configured employees populate the selector, with clear-all and clear-single behavior
- when a position has no employees, the current manual selection behavior remains
- the management edit dialog shows product, position, and workshop labels

**Step 2: Run test to verify it fails**

Run:
`node hr_web/tests/workplan-product-management.test.mjs`
Expected: FAIL until the add-schedule page is wired.

**Step 3: Write minimal implementation**

Hook the new API into the add-schedule page and schedule edit display. Keep `tbplanlist.productName/linkName` as the persisted values and derive workshop display from the attendance group.

**Step 4: Run test to verify it passes**

Run the Node test again and make it pass.

**Step 5: Commit**

Not required for this session.

### Task 5: Verify and update docs

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Step 1: Run verification**

Run focused Maven tests, Node tests, and build checks.

**Step 2: Confirm results**

Expected commands:
- `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMapperXmlTest,WorkPlanServiceImplTest,WorkPlanListControllerTest test`
- `node hr_web/tests/workplan-product-management.test.mjs`
- `mvn -DskipTests compile`
- `npm run build`

**Step 3: Update docs**

Record the final data model, API surface, UI behavior, and any remaining edge cases.

**Step 4: Commit**

Not required for this session.
