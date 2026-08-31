# Employee Open-Ended Contract Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Support employee contracts whose type is `无固定期限劳动合同` without requiring a contract end date.

**Architecture:** Keep `hrm_employee_contract.end_time` nullable for open-ended contracts and keep existing validation for every other contract type. Use contract type `2` as the single business signal for "open-ended" across save, import, contract display, and employee export formatting.

**Tech Stack:** Java / Spring Boot / MyBatis-Plus backend, Vue3 / Element Plus frontend, JUnit and Node static/module tests.

---

### Task 1: Backend Contract Service Rules

**Files:**
- Modify: `src/test/java/com/tianye/hrsystem/imple/employee/HrmEmployeeContractServiceImplTest.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeContractServiceImpl.java`

**Step 1: Write the failing tests**
- Add a test that saves contract type `2` with `startTime` and `endTime=null`, expecting no exception and saved `term=null`.
- Add a test that imports contract type `无固定期限劳动合同` with blank `合同结束日期`, expecting `endTime=null` and `term=null`.
- Add a test that imports a fixed-term contract with blank `合同结束日期`, expecting the existing required-date error.

**Step 2: Run RED**
- Run: `mvn -Dtest=HrmEmployeeContractServiceImplTest test`
- Expected: fails because `calculateContractTerm(...)` still rejects null `endTime`.

**Step 3: Implement minimal backend logic**
- Add an `isOpenEndedContract(...)` helper for `contractType == EmployeeContractType.NO_FIXED_TERM_LABOR_CONTRACT.getValue()`.
- In `addOrUpdateContract(...)`, set `term=null` for open-ended contracts; otherwise calculate term as before.
- In import parsing, parse contract type before required end date. For open-ended contracts, use optional date parsing for `合同结束日期`; for other contracts, keep `getRequiredImportDate(...)`.

**Step 4: Run GREEN**
- Run: `mvn -Dtest=HrmEmployeeContractServiceImplTest test`
- Expected: all tests pass.

### Task 2: Backend Export Display

**Files:**
- Modify: `src/test/java/com/tianye/hrsystem/imple/employee/EmployeeBasicInfoExportSupportTest.java`
- Modify: `src/test/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupportTest.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/employee/EmployeeBasicInfoExportSupport.java`
- Modify: `src/main/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupport.java`
- Modify: `src/main/resources/mapper/HrmEmployeeMapper.xml`
- Modify: `src/test/java/com/tianye/hrsystem/mapper/HrmEmployeeMapperSqlTest.java`

**Step 1: Write RED tests**
- Employee basic export should render `劳动/劳务合同期限` and `结束时间` as `无固定期限` when latest contract type is `2` and end date is blank.
- Department detail export should render `合同到期日` as `无固定期限` for the same case.
- Mapper SQL should expose the latest/last contract type aliases needed by export formatting.

**Step 2: Implement display helpers**
- Add helpers that return `无固定期限` when `latestContractType/lastContractType/contractType` equals `2`.
- Otherwise keep existing date formatting.

**Step 3: Verify**
- Run: `mvn -Dtest=EmployeeBasicInfoExportSupportTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest test`

### Task 3: Frontend Contract Form Rules

**Files:**
- Create or modify: `/Users/jiangyongming/Project/hr/hr_web/tests/employee-contract-model.test.mjs`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/employee/model/employeeContractModel.js`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/employee/Components/EmployeeContract.vue`

**Step 1: Write RED tests**
- `getRules({ contractType: 2 })` must not contain `endTime` or `term` required rules.
- `getRules({ contractType: 1 })` must still require `endTime` and `term`.
- The contract display component must render blank open-ended end dates as `无固定期限`.

**Step 2: Implement minimal frontend logic**
- Remove `endTime` required rule for contract type `2`.
- Add display formatting in `EmployeeContract.vue` for field `endTime` when `contractType == 2` and value is blank.

**Step 3: Verify**
- Run: `node tests/employee-contract-model.test.mjs`
- Run existing contract import tests.

### Task 4: Final Verification and Docs

**Files:**
- Modify: `docs/requirements.md`
- Modify: `docs/development.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`
- Modify: `task_plan.md`, `findings.md`, `progress.md`
- Modify: `/Users/jiangyongming/Project/hr/hr_web/task_plan.md`, `findings.md`, `progress.md`

**Step 1: Run combined verification**
- Backend: `mvn -Dtest=HrmEmployeeContractServiceImplTest,EmployeeBasicInfoExportSupportTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest test`
- Backend compile: `mvn -DskipTests compile`
- Frontend tests: `node tests/employee-contract-model.test.mjs`, existing employee contract tests.
- Frontend build: `npm run build`

**Step 2: Update docs**
- Record the new business rule: only open-ended labor contracts may omit end date.
- Record implementation notes and verification output.
