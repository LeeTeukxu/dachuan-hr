# Alternating Workweek Module Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an independent module that stores and serves yearly alternating single-rest/double-rest week records, auto-generates a year from the first week type when no records exist, and recalculates from an edited week through year end.

**Architecture:** Create a new `modules/workweek` backend module with BO/VO/entity/controller/service classes plus a JPA repository. Persist one row per year/week so repeated queries read from the database directly. Keep generation logic in a dedicated service helper that computes natural weeks (`Monday-Sunday`, week 1 contains `January 1`) and alternates `single rest` / `double rest` through the last week of the year.

**Tech Stack:** Spring Boot 2.1, Java 8, Lombok, MyBatis-Plus entities, Spring Data JPA, JUnit 4, Mockito

---

### Task 1: Add the failing service tests

**Files:**
- Create: `src/test/java/com/tianye/hrsystem/modules/workweek/service/HrmWorkweekSettingServiceTest.java`

**Step 1: Write the failing test**

Add tests for:
- generating a missing year from `firstWeekType`
- recalculating from a manually edited week to year end while keeping previous weeks unchanged
- reading an existing year without regenerating

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false surefire:test`
Expected: FAIL because the service/module classes do not exist yet.

**Step 3: Write minimal implementation**

Create the new module classes needed to satisfy the tests without adding extra behavior.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false surefire:test`
Expected: PASS

### Task 2: Implement the new workweek module

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/controller/HrmWorkweekSettingController.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/service/HrmWorkweekSettingService.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/entity/HrmWorkweekSetting.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/bo/QueryWorkweekSettingBO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/bo/InitWorkweekSettingBO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/bo/UpdateWorkweekSettingBO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/vo/WorkweekSettingVO.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/hrmWorkweekSettingRepository.java`

**Step 1: Implement query/init/update APIs**

Provide endpoints:
- `POST /hrmWorkweekSetting/queryYearSettings`
- `POST /hrmWorkweekSetting/initYearSettings`
- `POST /hrmWorkweekSetting/updateWeekType`

**Step 2: Keep generation rules explicit**

Implement:
- natural weeks start on Monday and end on Sunday
- week 1 is the week containing January 1
- `single rest = Sunday rest only`
- `double rest = Saturday and Sunday rest`
- after editing week `N`, recalculate weeks `N..lastWeek`

**Step 3: Persist stable yearly rows**

Each row stores:
- `settingId`
- `settingYear`
- `weekNo`
- `weekType`
- `weekStartDate`
- `weekEndDate`
- `restDayText`
- `manualOverride`

### Task 3: Update project docs

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`

**Step 1: Record the new module requirements**

Document initialization, query, and forward recalculation rules.

**Step 2: Record the implementation notes**

Document module path, endpoints, persistence model, and targeted verification command.

### Task 4: Verify the implementation

**Files:**
- Test: `src/test/java/com/tianye/hrsystem/modules/workweek/service/HrmWorkweekSettingServiceTest.java`

**Step 1: Run targeted tests**

Run: `mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false surefire:test`

**Step 2: Run compile verification**

Run: `mvn -DskipTests compile`

**Step 3: Review docs and changed files**

Confirm only the new workweek module and project docs changed.
