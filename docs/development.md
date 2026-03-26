# Development Notes

## Architecture Overview
- 接口入口：`src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`
- 核心服务：`src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`
- 数据模型：`src/main/java/com/tianye/hrsystem/model/tbplanlist.java`

## Key Modules and Responsibilities
- `WorkPlanListController#saveAll`：接收前端排班数组并调用服务层。
- `WorkPlanServiceImpl#AddAll`：入库 + 推送钉钉排班。
- `WorkPlanServiceImpl#PostToServer`：按 `groupId/classId/userId/workDate` 组装钉钉请求。

## Recent Changes and Decisions
- 修复 `saveAll` 500 的后端根因：
  - 原因：前端常不传 `groupId`，服务层未兜底，`PostToServer` 直接使用 `groupId` 导致空指针/转换异常，返回 `message=null`。
- 在 `WorkPlanServiceImpl` 新增 `enrichAndValidatePlan(tbplanlist plan)`：
  - 校验 `classId/userId/workDate` 非空。
  - 当 `groupId` 为空时，通过 `classId -> hrm_attendance_shift.group_id` 自动补齐。
  - 仍无法补齐时抛出明确异常：`排班缺少考勤组信息, classId=...`。
- 在 `PostToServer` 增加前置校验：
  - `groupId/classId/workDate/userId` 缺失直接抛出可读错误，避免 `null` 错误信息。
- 修复 `changeGroup` 请求构造错误：
  - 原代码创建了 `req1`（add 请求）但误把参数写入 `req`（remove 请求对象），导致加组调用参数为空并可能触发保存失败。
  - 已改为将 `opUserId/groupKey/userIdList` 正确写入 `req1`。
- 增强异常可观测性：
  - `successResult#raiseException` 现在会打印异常栈，并在 `ax.getMessage()` 为空时回退到 `ax.toString()`，避免前端只看到 `code=500` 且 `message` 为空。
- 增强 `groupId` 兜底策略：
  - 当 `saveAll` 入参缺少 `groupId` 且本地 `hrm_attendance_shift` 未命中 `classId` 时，新增通过 `getAllGroups` 的 `selectedClass` 按 `classId` 反查 `groupId` 的逻辑，适配“班次已在钉钉存在但本地班次表未同步”场景。
- 调整 `saveAll` 事务策略（本地联调友好）：
  - `planRep.saveAll` 后调用钉钉同步改为 `try/catch`，当钉钉返回权限/日期等错误时不再抛出回滚本地事务，保证本地排班可保存、后续 `loadIsLast` 可读取。
  - 同步失败仍记录详细日志（包含 `groupId/shiftId/userId/workDate`），便于后续排查钉钉权限与日期合法性问题。
- 修复 `loadIsLast` 返回数据缺少主键的问题：
  - `WorkPlanMapper.getMaxDate` 查询补充 `ID` 字段，确保“加载上次排班表”返回记录可携带 `id`，前端提交时命中更新分支而非新增分支。
- 修复“加载上次排班覆盖旧日期”问题：
  - 根因：`AddAll` 只要入参有 `id` 就走更新分支，且会直接覆盖 `workDate`，导致旧日期记录被改写为新日期。
  - 处理：`AddAll` 增加“同 `id` 同日期才更新”的判断；若 `id` 存在但日期不同（或已不存在）则按新增保存（新主键），保证旧日期记录不变。
  - 同时补充 `WorkPlanServiceImplTest` 回归用例，覆盖“跨日期应新增”和“同日期应更新”两个行为。
- 调整“加载上次排班”查询规则：
  - `WorkPlanListController#loadIsLast` 新增对 `SelectDate/WorkDate/Begin`（选择日期）和 `LoadLast/isLast`（是否加载上次）的兼容解析。
  - `loadLast=true` 时调用 `WorkPlanServiceImpl#loadBySelectedDate`：按“选择日期之前最近一天”查找目标日期并加载该天排班。
  - `loadLast=false` 时加载“选择日期当天”排班；无数据返回空列表。
  - 若未传选择日期则保留原行为（查询全库最大日期），确保旧前端调用不被破坏。

## Testing
- 编译验证：`mvn -DskipTests compile`（通过）。
- 回归用例（单测）：`mvn -Dtest=WorkPlanServiceImplTest#addAll_shouldCreateNewRecord_whenLoadedFromLastDateAndWorkDateChanges -DfailIfNoTests=false surefire:test`（通过）。
- 加载逻辑回归（单测）：`mvn -Dtest=WorkPlanServiceImplTest -DfailIfNoTests=false surefire:test`（4 通过，0 失败）。
- 由于仓库内存在与本次无关的历史测试编译错误（`SalaryMonthRecordServiceNewTest`），采用 JUnitCore 方式可单独验证 `WorkPlanServiceImplTest`（当前 4 个用例）通过。
- 接口侧排查结论（联调前）：
  - `removeAll` 正常；
  - `saveAll` 失败时会返回 `success=false, code=500`；
  - 现已补齐后端字段兜底与错误信息，便于继续联调验证。

## Known Issues / Future Work
- 若 `classId` 对应班次本身无 `group_id` 配置，接口会继续失败并提示明确错误，需要数据配置侧修正班次归属考勤组。
