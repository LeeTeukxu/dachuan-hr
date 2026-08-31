# Minimize DingTalk API Calls Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Reduce DingTalk API usage in attendance sync and approval fetch flows to the minimum practical level without changing external endpoints.

**Architecture:** Reorganize sync around each DingTalk API's natural batch capability instead of per-employee loops. For workflow approval fetch, resolve reusable process metadata once per task and reuse it through the employee iteration.

**Tech Stack:** Java, Spring Boot, JPA repositories, DingTalk Java SDK, JUnit 4, Mockito

---

### Task 1: Lock sync batching regressions

**Files:**
- Create: none
- Modify: `src/test/java/com/tianye/hrsystem/imple/AttendanceDetailServiceImplTest.java`
- Create: `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceDataServiceImplTest.java`

**Step 1: Write the failing test**

- Add a test proving step 4 should call detail sync once for a multi-employee batch instead of once per employee.
- Add a test proving schedule sync should fetch each work date once for the whole target employee set.

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AttendanceDetailServiceImplTest,HrmAttendanceDataServiceImplTest -DfailIfNoTests=false test`

Expected: new tests fail because current implementation still uses single-employee batching.

**Step 3: Write minimal implementation**

- Refactor `HrmAttendanceDataServiceImpl` to use step-specific batching.
- Introduce a schedule-specific bulk path instead of repeated single-employee service calls.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AttendanceDetailServiceImplTest,HrmAttendanceDataServiceImplTest -DfailIfNoTests=false test`

Expected: PASS

### Task 2: Refactor attendance schedule sync to date-level fetch

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/imple/AttendancePlanServiceImpl.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/ddTalk/AttendancePlanRecord.java`

**Step 1: Write the failing test**

- Add/extend tests to prove one date should trigger one DingTalk fetch for all target employees.

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=HrmAttendanceDataServiceImplTest -DfailIfNoTests=false test`

Expected: FAIL

**Step 3: Write minimal implementation**

- Add a method that fetches one date once and filters/saves only target employees.
- Remove the per-employee/per-date outer amplification.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=HrmAttendanceDataServiceImplTest -DfailIfNoTests=false test`

Expected: PASS

### Task 3: Reduce approval workflow metadata calls

**Files:**
- Modify: `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalSyncServiceImplWorkflowTest.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalSyncServiceImpl.java`

**Step 1: Write the failing test**

- Add a test proving one fetch task should resolve process codes once and reuse them across multiple employees when the codes are task-global.

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest -DfailIfNoTests=false test`

Expected: FAIL

**Step 3: Write minimal implementation**

- Cache task-level process codes by approval type set.
- Reuse the cached result in the employee loop.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest -DfailIfNoTests=false test`

Expected: PASS

### Task 4: Optimize smaller repeated DingTalk calls

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/imple/ddTalk/AttendanceGroupManager.java`
- Modify: `src/test/java/com/tianye/hrsystem/imple/ddTalk/AttendanceGroupManagerTest.java`

**Step 1: Write the failing test**

- Add a test proving identical `shiftId` values within one group sync should reuse one `shift/query` result.

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AttendanceGroupManagerTest -DfailIfNoTests=false test`

Expected: FAIL

**Step 3: Write minimal implementation**

- Add an in-memory map for `shiftId -> shift detail` during one `GetAndSave()` execution.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AttendanceGroupManagerTest -DfailIfNoTests=false test`

Expected: PASS

### Task 5: Verify and document

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `findings.md`
- Modify: `progress.md`

**Step 1: Run focused regression suite**

Run: `mvn -Dtest=AttendanceDetailServiceImplTest,HrmAttendanceDataServiceImplTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,AttendanceGroupManagerTest -DfailIfNoTests=false test`

Expected: PASS

**Step 2: Update docs**

- Record the new batching strategy, cache scope, and remaining unavoidable per-user API constraints.

**Step 3: Final verification**

Run the same focused suite again after doc updates only if code changed during verification.
