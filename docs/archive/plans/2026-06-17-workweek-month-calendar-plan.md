# 单双休月度日历配置 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在单双休设置月度概览中支持点击月份打开日历弹窗、逐日切换上班/休息，并保存该月每日结果入库。

**Architecture:** 后端新增 `hrm_workweek_day_setting` 作为月度每日覆盖表；查询月历时优先读取已保存覆盖，否则按现有周休规则和 `hrm_attendance_legal_holidays` 法定节假日/调休数据生成默认状态。前端在现有 `workweek/Index.vue` 月度卡片上增加点击入口和 Element Plus 弹窗，以 7 列日历网格展示每天状态并提交保存。

**Tech Stack:** Spring Boot + Spring Data JPA + JUnit/Mockito；Vue 3 + Element Plus + Axios；SQL 脚本落在 `docs/sql`。

---

### Task 1: 后端月历模型和接口

**Files:**
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/entity/HrmWorkweekDaySetting.java`
- Create: `src/main/java/com/tianye/hrsystem/repository/hrmWorkweekDaySettingRepository.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/bo/QueryWorkweekMonthCalendarBO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/bo/SaveWorkweekMonthCalendarBO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/bo/WorkweekDaySettingBO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/vo/WorkweekMonthCalendarVO.java`
- Create: `src/main/java/com/tianye/hrsystem/modules/workweek/vo/WorkweekDayCalendarVO.java`
- Modify: `src/main/java/com/tianye/hrsystem/modules/workweek/service/HrmWorkweekSettingService.java`
- Modify: `src/main/java/com/tianye/hrsystem/modules/workweek/controller/HrmWorkweekSettingController.java`
- Test: `src/test/java/com/tianye/hrsystem/modules/workweek/service/HrmWorkweekSettingServiceTest.java`

**Steps:**
1. Add failing service tests for querying a month calendar with legal holiday defaults and saved overrides.
2. Add failing service test for saving one month of day settings and returning updated counts.
3. Implement entity/repository/BO/VO classes and service methods.
4. Add controller endpoints `/queryMonthCalendar` and `/saveMonthCalendar`.
5. Run `mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false test`.

### Task 2: 数据库脚本

**Files:**
- Create: `docs/sql/2026-06-17_hrm_workweek_day_setting.sql`
- Create: `docs/sql/2026-06-17_hrm_workweek_day_setting_hr_0001_to_hr_0005.sql`

**Steps:**
1. Create single-schema table script with unique key `(setting_year, work_date)`.
2. Create multi-tenant dev script for `hr_0001` through `hr_0005`.

### Task 3: 前端日历弹窗

**Files:**
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/api/hrm/attendance/workweek.js`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/attendance/workweek/Index.vue`
- Test: `/Users/jiangyongming/Project/hr/hr_web/tests/workweek-page.test.mjs`

**Steps:**
1. Add failing page/source test for month card click, calendar dialog, day toggle, save API.
2. Add API methods `queryWorkweekMonthCalendar` and `saveWorkweekMonthCalendar`.
3. Add dialog state, open/query handler, toggle handler, save handler.
4. Add accessible 7-column calendar grid styles and save loading state.
5. Run `node tests/workweek-page.test.mjs` and `npm run build`.

### Task 4: 文档和验证

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`

**Steps:**
1. Record backend API/table and frontend behavior requirements.
2. Record validation commands and known caveats.
3. Run targeted backend and frontend checks before final handoff.
