# Attendance Sync Redis Serialization Bugfix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restore asynchronous attendance synchronization progress by serializing retry-state values as strings before Redis persistence.

**Architecture:** The controller continues to submit work to `AttendanceSyncTaskLauncher`; the service records retry/progress state in Redis. The fix is constrained to the service-to-Redis value boundary so frontend polling and existing Redis configuration remain unchanged.

**Tech Stack:** Java 8, Spring Boot 2.1, Spring Data Redis, JUnit, Mockito.

---

### Task 1: Reproduce the Redis Serialization Boundary Failure

**Files:**
- Test: `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceDataServiceImplTest.java`

**Step 1:** Add a focused test that invokes retry-state persistence through the service and asserts the Redis abstraction receives a string value.

**Step 2:** Run `mvn -Dtest=HrmAttendanceDataServiceImplTest test` and verify the test fails because a `Long` is passed.

### Task 2: Apply the Service Boundary Fix

**Files:**
- Modify: `src/main/java/com/tianye/hrsystem/imple/HrmAttendanceDataServiceImpl.java`
- Test: `src/test/java/com/tianye/hrsystem/imple/HrmAttendanceDataServiceImplTest.java`

**Step 1:** Convert the retry value to its string representation at the call to the String-serialized Redis abstraction.

**Step 2:** Re-run the focused test and verify it passes.

### Task 3: Verify and Document

**Files:**
- Modify: `docs/modules/attendance-sync.md`

**Step 1:** Run the focused service test and `mvn -DskipTests compile`.

**Step 2:** Verify controller and frontend contracts remain unchanged; run relevant frontend source-contract tests if frontend code changes.

**Step 3:** Replace the oldest recent-change entry in the module document with this root cause, scope, and verification result.
