# Findings: 排班管理矩阵多产品多岗位修改排班

## 2026-08-24 Decision
- 当前后端 `WorkPlanServiceImpl#queryEmployeeDayShift(...)` 会调用 `pickPreferredEmployeePlan(...)`，只返回目标员工当天本地排班中的一条首选记录。
- 当前前端 `Scheduling.vue` 使用单个 `dayShiftProductContext` 表示修改排班弹窗的生产产品和岗位上下文，无法准确表达同一员工同一天多个产品或多个独立时间段。
- 用户已确认：同一天不同产品或岗位允许有不同工作时间。
- 设计结论：修改排班的聚合根应为 `employeeId + workDate`，内部包含多条 `assignments[]`。每条 assignment 独立维护生产产品、岗位、班次类型、开始/结束时间、白夜班和连班。
- 保存结论：采用完整替换目标员工当天排班的语义。先查询当天所有包含目标 `userId` 的 `tbplanlist`，从共享行移除目标员工并保留其他员工，目标员工独占行删除，再按提交 assignments 逐条保存。
- 休息/调休结论：休息/调休与工作 assignments 互斥，保存为一条目标员工 `shift_type=rest` 记录，产品和岗位置空。
- 数据模型结论：第一期继续使用扁平 `tbplanlist.ProductName/LinkName/UserID`，不新增排班事实表字段；后续若要稳定保存产品/岗位 ID，再设计规范化明细表。
- 文档：已新增 `docs/plans/2026-08-24-workplan-matrix-multi-assignment-design.md`，并更新后端/前端 requirements 与 development 文档。
- 前端实现复核：`Scheduling.vue` 的弹窗模板已有岗位多行 UI，但仍绑定全局 `dayShiftProductContext` 和单个 `dayShiftForm`，所以多岗位只能共享同一班次时间，多产品也只能落到一个产品。
- 前端加载复核：`handleEditDayShift(...)` 仍调用 `queryEmployeeDayShift(...)`，然后用矩阵摘要补 `productName/positionName`；这会把完整 records 压回合并文本或后端首选记录。
- 前端保存复核：`handleSaveDayShift(...)` 仍通过 `saveWorkPlanAll(JSON.stringify(payload))` 保存，并只构造一个 `dayShiftPayloadRow`；产品/岗位靠 `productContext` 展开，无法支持每条分配不同时间。

## 2026-08-24 Implementation Result
- 后端已新增 `SaveWorkPlanEmployeeDayAssignmentBO`、`SaveWorkPlanEmployeeDayAssignmentsBO`、`WorkPlanEmployeeDayAssignmentVO` 和 `WorkPlanEmployeeDayAssignmentsVO`，以 `employeeId + workDate + assignments[]` 表达完整日排班编辑对象。
- `WorkPlanListController` 已新增 `/workPlan/queryEmployeeDayAssignments` 与 `/workPlan/saveEmployeeDayAssignments`；`IWorkPlanService` 和 `WorkPlanServiceImpl` 同步提供查询与保存能力。
- 查询接口返回目标员工当天所有本地 `tbplanlist` 分配，不再压缩为 `pickPreferredEmployeePlan(...)` 的首选单条记录。
- 保存接口采用完整替换：先查出当天包含目标 `userId` 的旧排班，目标员工独占行删除，共享行只移除目标员工并保留其他 `UserID`，再按提交 assignments 新增目标员工的新排班。
- 休息/调休仍与工作分配互斥；保存休息或调休时只写一条 `shift_type=rest` 记录，并清空生产产品和岗位。
- 前端 `workPlan.js` 已新增完整分配查询和保存 API；`work-plan-utils.js` 新增 `normalizeEmployeeDayAssignmentsForEdit(...)` 与 `buildEmployeeDayAssignmentsSaveRequest(...)`。
- `Scheduling.vue` 已停止在修改排班弹窗中使用 `queryEmployeeDayShift`、`dayShiftProductContext` 和矩阵摘要反推；弹窗改为 `dayShiftAssignments[]` 多分配行，每行独立产品、岗位、班次、时间、白夜班和连班。
- 前端源码测试失败根因是旧契约仍要求 `Scheduling.vue` 包含 `getWorkPlanMatrixCellScheduleMeta/queryEmployeeDayShift/dayShiftProductContext`；修复测试后新契约锁定完整分配接口和状态。
- 验证结论：后端 69 项排班服务/控制器测试通过，`mvn -DskipTests compile` 通过；前端排班 API、工具、矩阵页面、添加排班页面测试和 `npm run build` 通过。

# Findings: 添加排班人员缺失修复

## 2026-08-24 Discovery
- `/attendanceData/getAllUsers` 调用 `WorkPlanServiceImpl#getUsersForDisplay(...)`；添加排班前端通过该接口加载人员下拉。
- 原展示链路依赖 `tbattendanceuser` 快照，快照缺失或 `groupId` 为空时会漏掉员工；这解释了 admin 登录后搜不到陈明成。
- 根因修复为直接查询当前租户 `hrm_employee`：`is_del=0` 且 `entry_status in (1,3,4)`，再过滤姓名；人员 ID 按员工表 `dingtalk_user_id`、`tbattendanceuser.userId`、员工 `employeeId` 依次解析，并按解析后的用户 ID 去重。
- 没有考勤组的员工仍返回，`UserObject.groupId` 统一为 `""`；返回 `employeeId` 保持前端矩阵和提交链路的员工识别能力。
- 展示缓存从 `*_getAllUsers_display_v2` 升级为 `*_getAllUsers_display_v3`，清理时同时删除 v2 和旧版 key。
- 标准班次提交仍以提交的钉钉用户 ID调用原远程接口；`getUsers(companyId)` 仅用于既有考勤组变更判断，找不到本地快照用户不会在 `scheduleStandardPlan(...)` 中直接抛错。
- 验证：`mvn -Dtest=WorkPlanServiceImplTest,HrmAttendanceDataControllerTest test` 通过 62 项，`mvn -DskipTests compile` 通过。

# Findings: 添加排班岗位多行与全员补充选人

## 2026-08-23 Pre-work
- 已读取项目需求/开发文档和既有生产产品/岗位/员工配置实施记录。
- 现有设计保持排班事实表扁平：生产产品保存到 `tbplanlist.ProductName`，岗位保存到 `tbplanlist.LinkName`。
- 既有添加排班已支持按生产产品加载岗位、按岗位加载配置员工，并在提交前把一行多岗位展开为多条 `/workPlan/saveAll` payload。
- 本轮新需求是在既有联动基础上调整前端交互：岗位要以多行展示并通过 `+` 添加；人员选择要支持岗位默认员工加全员自选；岗位和人员都需要在同一排班行内跨子行去重。
- 结果：前端改成 `positionRows` 多行岗位与人员子行后，岗位候选和人员候选都按同一主行去重；提交仍展开为扁平 payload，后端契约不变。
- 验证：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/workplan-product-management.test.mjs` 通过，`npm run build` 通过。

# Findings: 排班产品岗位排序与多岗位提交

## 2026-08-22 Discovery
- 后端生产产品配置模块已存在，入口为 `WorkPlanProductController`，服务为 `WorkPlanProductServiceImpl`。
- 当前 `saveProduct(...)` 与 `savePosition(...)` 在请求未传 `sort` 时都会写入 `0`，不会按库内最大排序号自动生成。
- 当前后端没有岗位重排接口；`HrmWorkPlanProductPositionRepository` 只有按产品查询、按产品列表查询和删除方法。
- 前端生产产品管理页 `workplan-product/Index.vue` 右侧岗位区域是普通 `el-table`，只支持手工编辑排序输入框，不支持拖拽重排并提交新顺序。
- 添加排班页 `AddOrEdit.vue` 当前每行只有单个 `positionId`，`buildWorkPlanSubmitPayload(...)` 每行只生成一条扁平 payload，`linkName` 为单个岗位名称。
- 设计结论：排班事实表继续保持扁平；添加排班“一行多岗位”在前端提交前展开为多条排班项，每条排班项有自己的 `linkName` 和 `userId`，后端 `workPlan/saveAll` 继续按既有数组结构持久化。
- RED 证据：
  - `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest test` 失败于缺少 `SortWorkPlanPositionBO`；
  - `node tests/workplan-product-management.test.mjs` 失败于前端 API 未导出 `saveWorkPlanPositionSort`；
  - `node tests/work-plan-utils.test.mjs` 失败于多岗位行仍只生成 1 条 payload。
- 实现结论：
  - 后端新增 `/workPlanProduct/sortPositions`，同一产品内按提交岗位 ID 顺序持久化 `sort`，未提交岗位追加保留；新增产品和岗位未传 `sort` 时分别按库内最大排序自动生成。
  - 前端生产产品页右侧岗位列表新增拖动手柄，拖放后调用 `saveWorkPlanPositionSort({ productId, positionIds })` 并重新加载产品树。
  - 添加排班页岗位选择改为多选，行数据新增 `positionIds/positions`；每个岗位保留自己的 `users`，提交时由 `buildWorkPlanSubmitPayload(...)` 展开为多条 `/workPlan/saveAll` 扁平保存项。
- GREEN 证据：
  - `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanListControllerTest,WorkPlanServiceImplTest test` 通过，73 个测试 0 failures/errors；
  - `node tests/workplan-product-management.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs` 通过；
  - `mvn -DskipTests compile` 与 `npm run build` 通过。

## 2026-08-23 Drag Sorting UX Follow-up
- 用户反馈：排序号没有加载到排序输入框；岗位拖动排序没有动画或落点反馈；生产产品列表也需要拖动排序。
- 设计结论：
  - 新增产品/岗位弹窗的排序输入框按当前列表最大 `sort + 1` 默认加载；编辑弹窗继续加载已有 `sort`。
  - 左侧产品表新增排序拖动手柄和序号列，调用 `/workPlanProduct/sortProducts` 持久化顺序。
  - 左右两张表统一用行级 `drag-source`、`drag-over-top`、`drag-over-bottom` class 表示拖动源、目标行和上/下插入位置，降低拖动过程中判断落点的成本。
- RED/GREEN 证据：
  - 后端 `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest test` 先失败于 `SortWorkPlanProductBO` 缺失；实现后通过，10 个测试 0 failures/errors。
  - 前端 `node tests/workplan-product-management.test.mjs` 先失败于缺少 `saveWorkPlanProductSort` 导出；实现后通过。

# Findings: 生产产品管理菜单权限

## 2026-08-22 Initial Evidence
- 项目文档确认：`8081` 本机页面读取 `hr_web/html` 静态包；源码变更后若未重新部署，本机页面仍可能加载旧 JS。
- 权限设计确认：前端侧边菜单和路由访问依赖登录返回的 `menuTree`，而 `menuTree` 来自后端菜单与角色授权，不会因为 `src/router/config.js` 新增路由自动出现。
- `ApiPermissionPathSupport` 当前已有 `/workPlan -> /hrm/attendance/records`、`/attendanceData -> /hrm/attendance/scheduling` 等考勤接口映射，但尚未看到 `/workPlanProduct -> /hrm/attendance/workplanProduct` 映射。
- `MenuPermissionSupport#buildAuthorizedMenuTree(...)` 只会返回当前角色授权且启用的父/子菜单；父级菜单没有授权子菜单时不会出现在登录菜单树。

## 2026-08-22 Root Cause and Fix
- 运行时复现：`cdadmin/123` 登录 `127.0.0.1:8081` 成功，`companyId=0002`、`roleId=2`，`menuTree` 中有考勤管理、添加排班等菜单，但没有 `/hrm/attendance/workplanProduct`。
- 静态包排除：`hr_web/html/assets/index-CgKbhgfn.js` 已包含 `workplanProduct` 路由和“生产产品”页面 chunk，说明不是前端未构建。
- 数据库根因一：`hr_0002.tbmenu` 没有 `/hrm/attendance/workplanProduct` 菜单；`roleId=2` 因此不可能获得该菜单授权。
- 代码根因二：`ApiPermissionPathSupport` 未配置 `/workPlanProduct/**`，新页面接口没有明确归属到“生产产品”菜单权限。
- 数据库根因三：补菜单后调用 `/workPlanProduct/queryTree` 返回 `SQLGrammarException`，只读核验显示 `hr_0002` 只有 `hrm_workplan_custom_shift`，缺少 `hrm_workplan_product`、`hrm_workplan_product_position`、`hrm_workplan_position_employee` 三张新配置表。
- 修复：新增 `docs/sql/2026-08-22_workplan_product_menu_permission.sql`，在 `hr_0001` 至 `hr_0005` 创建“生产产品”菜单，并给已拥有“添加排班”权限的角色补授该菜单；新增接口权限映射 `/workPlanProduct -> /hrm/attendance/workplanProduct`。
- 已执行：在 dev MySQL `153.0.237.98` 的 `hr_0001` 至 `hr_0005` 执行生产产品三表脚本和菜单权限脚本。复核每个租户库三张表均存在，生产产品菜单各 1 条。
- 验证：`cdadmin` 重新登录后 `menuTree` 包含 `生产产品:/hrm/attendance/workplanProduct`，`/workPlanProduct/queryTree` 返回 `success=true, code=200, count=0`。

# Findings: 排班生产产品/岗位/员工配置

## 2026-08-22 Result
- 设计落点保持排班事实表扁平：`tbplanlist.ProductName` 继续作为排班保存字段，本轮业务解释为“生产产品”；`tbplanlist.LinkName` 继续作为排班保存字段，本轮业务解释为“岗位”。
- 新增配置模块独立于数据配置页，不复用 `tbdictdata` 的工段/车间字典，避免把“产品 -> 岗位 -> 员工”的层级关系塞进旧字典结构。
- 后端新增三张配置表：
  - `hrm_workplan_product`：生产产品；
  - `hrm_workplan_product_position`：产品岗位；
  - `hrm_workplan_position_employee`：岗位员工，允许 `employee_id` 为空以保存手输临时员工。
- `WorkPlanProductServiceImpl#queryTree()` 会把岗位员工的 `employeeId` 映射到 `tbattendanceuser.userId/groupId`，前端添加排班选择岗位后才能直接得到可提交的排班用户。
- 保存岗位采用替换员工列表语义：前端清空全部员工或删除单个员工后保存，即删除旧关系并写入当前列表。
- 添加排班页现在先用 `queryWorkPlanProductTree()` 加载产品树；选择产品清空旧岗位和人员，选择岗位后：
  - 岗位配置了员工时，自动带出可匹配到考勤用户的员工；
  - 岗位未配置员工时，人员下拉仍显示原有全部可排班人员。
- 排班管理单日修改弹窗新增生产产品、岗位、车间只读展示。后端 `WorkPlanEmployeeDayShiftVO` 新增 `productName/positionName/workshopName`，车间从本地排班 `GroupID` 对应考勤组名称解析。
- 验证：后端产品模块聚焦测试、前端新增/排班回归测试、`mvn -DskipTests compile`、`npm run build` 均通过；构建只保留既有 Maven POM warnings、Vue `::v-deep` warning 和 Vite chunk size warning。

# Findings: 社保详情一键设置保险金额

## 2026-08-22 Pre-work
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 既有社保方案规则：`type=12 / 医疗长期护理保险` 已支持启用/禁用；社保方案与月度项目合计目前只统计启用项目行。
- 既有基本工资金额设置仍保留 `largeMedicalInsuranceAmount` 与 `longTermCareInsuranceAmount`，前端默认值分别为 `15` 和 `3`。
- 旧口径曾要求社保管理详情页一键设置/取消必须受方案中医疗长期护理保险是否启用约束；2026-08-22 用户最新确认后，该一键设置/取消固定追加不再受 `type=12` 启用状态限制。
- 初步落点：
  - 前端社保详情页：`hr_web/src/views/hrm/insurance-scheme/InsuranceDetail.vue`，页面已有“高级筛选”按钮、个人社保费/公司社保费列和上方合计卡片。
  - 后端月度员工记录模型：`HrmInsuranceMonthEmpRecord` 包含 `personalInsuranceAmount`、`corporateInsuranceAmount`、`corporateProvidentFundAmount`。
  - 后端需继续定位详情列表、月报列表和合计 SQL，确认持久化字段和更新接口最小改动。
- 设计结论：直接修改个人/公司社保金额需要避免重复点击叠加，因此后端应先从 `hrm_insurance_month_emp_project_record` 重新汇总基础社保金额，再按员工记录开关和 `type=12` 启用状态加长期护理/大额医疗。
- 持久化结论：需要在 `hrm_insurance_month_emp_record` 增加 `include_salary_basic_insurance_amount`，表示该员工月度社保记录是否累计基本工资金额设置中的长期护理/大额医疗；默认 `0`，旧数据不累加。
- 启用判断结论：优先看员工月度参保项目 `type=12` 的 `is_enabled`；若旧月度项目缺少该行，再按社保方案项目判断；旧方案缺少该项目时沿用既有“默认启用”兼容口径。
- 实现结论：
  - 后端新增 `/hrmInsuranceMonthRecord/updateSalaryBasicInsuranceAmount`，可按月报全员或 `iEmpRecordIds` 选中员工更新累计状态；
  - 设置/取消都会先从月度参保项目重算基础金额，再按员工级累计开关决定是否追加长期护理/大额医疗；最新口径不再判断医疗长期护理启用状态；
  - 详情员工列表返回 `includeSalaryBasicInsuranceAmount`，便于后续 UI 若要展示或决定默认动作时直接消费；
  - 前端详情页在高级筛选后放全员按钮，在表格按钮区放选中员工按钮，成功后刷新合计和行数据。
- 验证结论：后端社保聚焦回归 22 个测试通过，前端社保详情/方案/月报 tooltip 相关 Node 测试通过，`mvn -DskipTests compile` 与 `npm run build` 通过。

## 2026-08-22 cdadmin / hr_0002 Runtime Check
- `cdadmin/123` 登录 `127.0.0.1:8081` 后返回 `companyId=0002`，租户库为 `hr_0002`，组织为 `成都果言果语食品销售有限公司`。
- 最新月报为 2026-03 `2091088562492608513 / 3月社保报表`，3 名参保员工当前个人/公司社保费分别为 `477.15/1160.76`，合计为 `1431.45/3482.28`。
- 3 名员工使用方案 `1831653398776143873 / 成都（一）`；方案项目和员工月度项目中 `type=12 / 医疗长期护理保险` 都是 `is_enabled=0`。
- 基本工资金额设置中大额医疗为 `15.00`、长期护理为 `3.00`。用户最新要求为一键设置不管 `type=12` 是否启用都必须追加这两项金额；基础项目合计仍继续按项目启用状态排除禁用项目。
- 通过 `127.0.0.1:8081/api/hrsystem/hrmInsuranceMonthRecord/updateSalaryBasicInsuranceAmount` 实测：取消返回 `data=3` 并把开关置 `0` 后个人/公司社保费为 `477.15/1160.76`、合计 `1431.45/3482.28`；设置返回 `data=3` 并把开关置 `1` 后个人/公司社保费为 `480.15/1175.76`、合计 `1440.45/3527.28`。
- 本机静态包 `hr_web/html/assets/InsuranceDetail-DWbf-8Nm.js` 已包含新接口 `/updateSalaryBasicInsuranceAmount` 和 `irecordId/iempRecordIds` 请求字段，排除 8081 仍在使用旧详情包导致无效点击。

# Findings: 社保方案医疗长期护理保险项

## 2026-08-22 Discovery
- 前端新增/编辑社保方案入口位于 `hr_web/src/views/manage/insurance-scheme/Add.vue` 与 `Edit.vue`，险种默认行和中文名称集中在 `hr_web/src/views/manage/insurance-scheme/add.js`。
- 当前社保默认行只有 `type=1..5`：养老、医疗、失业、工伤、生育；用户要求新增项应紧跟 `type=5` 生育保险之后。
- 后端保存入口为 `HrmInsuranceProjectController#saveInsuranceProject -> HrmInsuranceSchemeService#saveInsuranceProject`，保存明细表 `hrm_insurance_project`，字段已覆盖 `type/defaultAmount/corporateProportion/personalProportion/corporateAmount/personalAmount`，因此无需新增物理列。
- 后端详情入口为 `HrmInsuranceSchemeService#queryInsuranceSchemeById`，当前按 `Range.closed(1, 9)` 拆分社保项目；若新增类型号大于 11，需要显式纳入社保项目列表，否则编辑保存后再次打开不会回显。
- 社保月员工详情入口 `HrmInsuranceMonthEmpRecordService#queryById` 也按 `Range.closed(1, 9)` 拆分社保项目，需要同步纳入新增类型，避免月报员工详情漏显示该项。
- 现有社保合计 SQL 曾从 `hrm_salary_basic.long_term_care_insurance_amount` 与 `large_medical_insurance_amount` 自动追加长期护理和大额医疗金额；用户最新补充明确社保方案中不需要这两个基本工资设置金额，因此本轮删除该自动累加。
- 当前决定：使用 `type=12` 表示 `医疗长期护理保险`，前端新增默认行；后端旧方案详情缺少该行时补默认启用的空白行；新增项目状态字段 `is_enabled`，`1=启用`、`0=禁用`，旧数据为空按启用兼容。
- 社保方案与月度员工参保项目合计只汇总 `coalesce(is_enabled,1)=1` 的项目行；禁用项目不进入个人/公司社保和公积金合计。
- 新增迁移脚本 `docs/sql/2026-08-22_hrm_insurance_project_is_enabled.sql`，为 `hrm_insurance_project` 和 `hrm_insurance_month_emp_project_record` 补 `is_enabled`。

# Findings: 社保管理名单与方案使用人数悬浮展示

## 2026-08-21 Result
- 用户补充后，本轮涉及两个入口：
  - `hr_web/src/views/hrm/insurance-scheme/InsuranceScheme.vue`：社保管理月报卡片。
  - `hr_web/src/views/manage/insurance-scheme/InsuranceScheme.vue`：系统设置下的社保方案管理表格，“使用人数”列在这里。
- 月报卡片此前已直接展示 `insuredEmployeeNames` 与 `stoppedEmployeeNames`，导致每张卡片下方常驻两块长名单区域，页面密度过高。
- 月报后端已经提供 `insuredNum/insuredEmployeeNames/stoppedEmployeeNames`，因此本轮只需前端把名单收进 `el-tooltip`；人数显示继续使用 `insuredNum ?? num` 和 `stopNum`。
- 社保方案管理后端此前只返回 `useCount`，没有人员名单字段；要悬浮展示“采用当前方案的具体人员”，必须扩展 `/hrmInsuranceScheme/index` 响应。
- 后端实现结论：
  - `InsuranceSchemeListVO` 新增 `useEmployeeNames`。
  - `HrmInsuranceSechemeMapper.xml#index` 和遗留 `HrmInsuranceSchemeMapper.xml#queryInsuranceSchemePageList` 都返回 `useEmployeeNames`，来源为 `hrm_employee_social_security_info -> hrm_employee.employee_name`，按姓名 `GROUP_CONCAT distinct ... SEPARATOR '、'` 聚合。
  - `HrmInsuranceSchemeService#index` 查询前调用 `setGroupConcatMaxLen()`，并加 `@Transactional(readOnly = true)` 保证会话变量与列表查询使用同一连接，降低方案使用人员较多时名单被截断的风险。
- 前端实现结论：
  - 月报卡片删除常驻 `card-extra/card-employee` 名单区域。
  - “本月参保人数”“本月停保人数”的数字使用 `el-tooltip`，悬浮/聚焦时显示对应人员名单和人数；数字触发器带 `tabindex=0` 和虚线提示。
  - 社保方案管理“使用人数”列仍显示人数，悬浮/聚焦时显示“使用人数：N人”和 `useEmployeeNames || '--'`。
- RED/GREEN 证据：
  - `node tests/insurance-advanced-filter.test.mjs` 先失败于缺少 tooltip，修复后通过。
  - `node tests/insurance-scheme-usage-tooltip.test.mjs` 先失败于缺少 tooltip/useEmployeeNames，修复后通过。
  - `mvn -Dtest=HrmInsuranceSchemeMapperXmlTest,InsuranceSchemeListVOTest,HrmInsuranceSchemeServiceTest test` 先失败于缺少 `setUseEmployeeNames/getUseEmployeeNames` 和 `setGroupConcatMaxLen()`，修复后通过。
- 最终验证：社保后端组合测试 13 个通过，`mvn -DskipTests compile` 通过；前端社保相关 5 个 Node 测试通过，`npm run build` 通过。保留既有 Maven POM warnings、Vue `::v-deep` deprecation 和 Vite chunk size warning。

# Findings: 社保管理本月参保/停保人员名单

## 2026-08-21 Pre-work
- 用户确认实际后端项目为 `/Users/jiangyongming/Project/hr/hainan`，不是此前误落点的 `watch`。
- 社保管理主列表接口是 `HrmInsuranceMonthRecordController#queryInsuranceRecordList`，服务为 `HrmInsuranceMonthRecordService#queryInsuranceRecordList`，SQL 位于 `src/main/resources/mapper/HrmInsuranceMonthRecordMapper.xml`。
- 当前列表 SQL 只返回 `num` 和 `stopNum` 数量，其中 `num` 实际是 `b.status=1` 的参保人数，但前端显示为“计薪人数”，不符合本次业务文案。
- 当前列表 SQL 未返回参保/停保员工姓名，因此前端即使改页面也没有数据来源。
- 设计选择：后端新增 `insuredNum`、`insuredEmployeeNames`、`stoppedEmployeeNames`，保留 `num/stopNum`；姓名使用 `GROUP_CONCAT(... ORDER BY c.employee_name SEPARATOR '、')` 聚合，并在查询前调高 `group_concat_max_len` 降低长名单截断风险。
- RED 证据：后台 XML 测试失败于缺少 `insuredNum`；服务测试失败于 Mapper 缺少 `setGroupConcatMaxLen()`。
- 实现结论：`queryInsuranceRecordList` 先执行 `SET SESSION group_concat_max_len = 4194304`，再查询列表；SQL 同时返回旧 `num` 和新 `insuredNum`，名单字段分别按 `status=1/0` 聚合。
- GREEN 证据：`mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 通过，7 个测试 0 failures/errors。

# Findings: cfy 登录 TooManyResultsException

## 2026-08-21 Root Cause
- 登录入口为 `LoginController#Login`，链路是先通过 `LoginUserMapper#getCompanyIdByUserName(account, systemBase)` 从系统库 `tbAllUserList` 定位公司，再通过 `getByAcountAndCompanyID(account, companyId, suffix)` 查询租户库 `view_LoginUser`。
- 远端 `POST http://153.0.237.99:9080/hrsystem/login?account=cfy&password=123` 和本机同接口均稳定返回 `TooManyResultsException`；`hbadmin` 可正常登录。
- 只读 SQL 确认：`hrsystem.tbAllUserList` 中 `cfy` 对 `CompanyID=0001` 有 2 条记录；`hbadmin` 只有 1 条。
- 只读 SQL 确认：`hr_0001.view_LoginUser where Account='cfy'` 只有 1 条有效登录用户，因此异常发生在第一步系统库账号索引查询，不是租户登录视图重复。
- `SHOW CREATE TABLE hrsystem.tbAllUserList` 显示当前表没有主键或唯一索引；`TbLoginUserMapper.xml#saveSystemLoginAccount` 使用 `ON DUPLICATE KEY UPDATE`，但由于表无唯一键，重复账号不会触发 duplicate-key 更新。
- 文档要求：系统库账号索引应按账号唯一定位租户，创建/编辑登录用户禁止跨公司同账号抢占，用户侧错误文案必须为中文，不应把技术细节直接返回前端。
- 当前系统库重复扫描仅发现 `cfy` 一组重复：2 行、1 个公司 `0001`；未发现同账号跨多个公司映射。
- 实现结论：登录侧改为查询去重后的公司 ID 列表；同公司重复索引行去重后继续登录，多个不同公司返回中文配置错误。
- 实现结论：登录用户管理保存系统账号索引前，先删除当前公司同账号旧索引再插入；这能在后续保存时清理同公司重复行，并避免继续累积重复。
- 代码审查结论：`TbLoginUserService#resolveCompanyId(...)` 不能优先信任请求体 `companyId`，否则可能租户库写当前公司、系统库索引写请求体公司；已改为优先当前 `CompanyContext`，无上下文时才回退请求体。
- 测试结论：追加 `resolveCompanyId_shouldPreferCurrentTenantContextOverRequestCompanyId`，先 RED 失败于返回 `0001`，修复后 GREEN 返回当前租户 `0003`。
- 验证结论：`LoginControllerTest,TbLoginUserServiceValidationTest,TbLoginUserMapperSqlTest` 先 RED 编译失败，再 GREEN 通过；审查修复后最终为 14 个测试通过。

# Findings: 薪资档案在职离职卡片筛选

## 2026-08-20 Result
- 薪资档案页面落点为 `hr_web/src/views/hrm/salary/archives/Archives.vue`，列表接口为 `/hrmSalaryArchives/querySalaryArchivesList`。
- 后端 `QuerySalaryArchivesListDto.status` 已有复用口径：`11` 表示在职，SQL 为 `hrm_employee.entry_status in (1,3)`；`15` 表示离职，SQL 为 `hrm_employee.entry_status = 4`。
- 本轮无需新增后端 DTO 字段、接口或数据库字段；前端只需要在列表请求体中显式提交当前卡片对应的 `status`。
- 实现结论：页面新增 `在职`、`离职` 两个卡片式选项卡，默认选中 `在职`；切换卡片时重置到第一页并重新查询。
- UI 结论：状态入口放在标题操作区和搜索框之间，使用 8px 圆角、hover、focus-visible 和 active 反馈；支持鼠标点击和键盘 Enter/Space 触发。
- 验证结论：`node tests/salary-archives-status-filter.test.mjs` 通过；`mvn -Dtest=HrmSalaryArchivesMapperSqlTest test` 通过，锁定后端 `11/15` SQL 口径。

# Findings: 职务补助与花名册表头修复

## 2026-08-20 Result
- 根因 1：员工花名册模板下载改为运行时 POI 插列后，只补了新列单元格，没有同步移动和扩展第 1 行父表头合并区域，导致 `固定绩效` 旁出现第二个孤立的 `薪酬福利` 父表头。
- 根因 2：`职务补助` 不属于员工主表物理列，需和 `固定绩效` 一样走员工动态字段体系；只改前端输入框或模板列都不能保证导入、详情编辑和列表异动保存入库。
- 根因 3：部门明细 `薪资待遇` 旧拼接只读取固定薪资、固定绩效和全勤，部门固定薪资成本也只累加固定绩效，未把新动态字段 `职务补助` 纳入同一口径。
- 实现结论：`HrmEmployeeController#downloadEmployeeRosterTemplate(...)` 现在在已有 `薪资等级` 右侧依次插入 `固定绩效`、`职务补助`，两列格式为 `0.00`，并重新合并覆盖三列的 `薪酬福利` 父表头，避免重复父表头。
- 实现结论：`HrmEmployeeServiceImpl` 把 `职务补助/duty_subsidy/dutySubsidy` 纳入薪资动态字段定义、保存、详情编辑归一化和花名册导入解析；字段类型为 `DECIMAL`、精度 `2`，值按两位小数字符串保存。
- 实现结论：`AddEmployeeBO`、`AddEmployeeFieldManageBO` 和 `HrmEmployeeChangeRecord` 增加 request 字段 `dutySubsidy`；新增/再次入职、办理转正、调整部门/岗位、晋升/降级均能写入 `hrm_employee_data`。
- 实现结论：`EmployeeDepartmentDetailExportSupport#salaryTreatmentText(...)` 现在按 `固定薪资(固定)+固定绩效(绩效)+职务补助(职务补助)+全勤(全勤)` 拼接，空项跳过；全勤金额等于 `100` 时按业务例外不追加；`月固定薪资成本` 同步累加 `职务补助`。
- 前端结论：`AddOrEdit.vue`、`EmployeeBaseInfo.vue`、`employee.js`、`Index.vue` 和 `employee-list-row-normalizer.js` 已补齐 `职务补助` 初始值、两位小数输入、详情兜底字段、列表操作弹窗字段和列表行字段归一化。
- 数据库脚本结论：本轮继续复用 `hrm_employee_field/hrm_employee_data` 动态字段能力，不新增 `hrm_employee` 物理列，因此不需要新增 DDL 脚本。
- 验证结论：后端聚焦 41 个测试通过，后端组合 46 个测试通过，`mvn -DskipTests compile` 通过；前端薪资字段测试、员工管理回归和 `npm run build` 通过。保留既有 Maven POM warning、Vue `::v-deep` warning 和 Vite chunk size warning。

# Findings: 部门明细薪资待遇累加全勤奖

## 2026-08-20 Pre-work
- 已读取 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md` 和 `progress.md` 中与员工管理下载部门明细、基本工资设置、全勤奖相关的内容。
- 现有部门明细需求记录：`薪资待遇` 已读取薪资档案 `10101 / 基本工资`、`10102 / 岗位工资`、`10103 / 职务工资` 三项之和；若员工动态字段维护 `固定绩效`，展示为 `<固定薪资>(固定)+<固定绩效>(绩效)`。
- 现有全勤金额配置记录：基本工资金额设置包含普通员工全勤金额和领导全勤金额；薪资核算 `40102 / 全勤奖` 已有员工级金额优先、基本工资设置兜底、默认 `100/500` 兜底的历史口径。
- 本轮新增口径：部门明细下载时，各部门员工行的 `薪资待遇` 还要累加全勤奖；全勤奖金额取基本工资设置中的领导/普通员工金额；试用期员工不累加全勤奖。
- 当前假设：本轮只调整部门明细 Excel `薪资待遇` 展示，不改变薪资核算、部门成本或薪资档案数据。
- 根因定位：`EmployeeDepartmentDetailExportSupport#salaryTreatmentText(...)` 当前只接收薪资档案项和动态字段，无法获得基本工资设置中的普通/领导全勤金额，也没有按员工岗位和试用状态调整 `薪资待遇`。
- 数据流定位：`HrmEmployeeServiceImpl#exportDepartmentDetail(...)` 当前构造 workbook 时只传入员工列表、员工动态字段和薪资档案项；需要在这里读取最新基本工资设置并传给导出支持类。
- 领导/普通判断参考现有计薪员工 SQL：岗位包含 `经理`、`总监`、`副总`、`董事长`、`高级技师`、`厂长` 时使用领导全勤金额，否则使用普通员工全勤金额。
- 试用期排除口径：复用 `EmployeeDepartmentDetailExportSupport#isProbationStatus(...)`，即员工 `status=2` 或 `试用` 时 `薪资待遇` 不追加全勤金额。
- RED 证据：新增 `EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees` 后，`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees test` 在 testCompile 阶段失败，提示 `buildWorkbook(...)` 实参多出普通/领导全勤金额，证明现有导出支持类没有全勤金额数据入口。
- 实现结论：`EmployeeDepartmentDetailExportSupport` 新增带普通/领导全勤金额的 `buildWorkbook(...)` 重载；薪资待遇在存在全勤或固定绩效时按组件展示，普通员工为 `<固定>(固定)+<普通全勤>(全勤)`，领导岗位为 `<固定>(固定)+<固定绩效>(绩效)+<领导全勤>(全勤)`；试用期员工保持原固定薪资/固定绩效展示，不追加全勤。
- 服务结论：`HrmEmployeeServiceImpl#exportDepartmentDetail(...)` 调用 `HrmSalaryBasicService#findAll()` 读取最新基本工资设置，传入 `ordinaryFullAttendanceAmount` 和 `leaderFullAttendanceAmount`；部门成本区仍保持原固定薪资/绩效工资/社保/公积金汇总口径。
- GREEN 证据：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees test` 通过；`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，14 个测试 0 failures/errors；部门明细组合回归通过 25 个测试；`mvn -DskipTests compile` 通过。

# Findings: 下载部门明细统计与总表修复

## 2026-08-20 Pre-work
- 已读取后端 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md` 和 `progress.md`，本轮仍沿用员工管理“下载部门明细”动态多 sheet Excel 方案。
- 参考文件 `/Users/jiangyongming/Desktop/农谷导入数据/各部门现有人员明细表(更新版).xlsx` 的 `人员总表` 为 15 行结构：标题、企业总人数、月总固定薪资成本、月总绩效薪资成本、月总社保成本、月总公积金成本、月总伙食成本、分部门横向汇总、人员总数、各类成本、合计月人工成本。
- 用户本轮明确伙食成本不需要，因此新 `人员总表` 不应保留“月总伙食成本”“月伙食成本”，合计月人工成本只累加固定薪资、绩效薪资、社保、公积金。
- 当前 `EmployeeDepartmentDetailExportSupport#writeSummarySheet(...)` 只写标题、总人数、部门/人数两列表，和参考 `人员总表` 完全不同。
- 当前部门 sheet 顶部 `majorSummaryText()` 输出“专业相关/专业不相关”，与用户本轮要求冲突。
- 当前 `probationSummaryText()` 中“一年以上实习生”和“正式老员工”都用司龄 `companyAgeYears >= 1` 过滤；用户本轮明确要分别按员工表中实习期一年以上、正式员工人数、试用期人数统计。
- 当前社保/公积金部门成本已经从员工行 `corporateInsuranceAmount/corporateProvidentFundAmount` 汇总；后续需补测试锁定只汇总本次导出的在职员工公司缴纳部分。
- RED 证据：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 失败于 `queryDepartmentDetailExportList` 未返回 `a.probation`、`人员总表` 仍是旧标题/部门人数列表，以及部门 sheet 仍输出“专业相关/专业不相关”。
- 实现结论：`EmployeeDepartmentDetailExportSupport` 现在先按部门构建 `DepartmentSummary`，部门 sheet 与 `人员总表` 共用该汇总对象；`人员总表` 改为参考表同类横向结构并删除伙食成本行，合计月人工成本只累加固定薪资、绩效薪资、社保、公积金。
- 实现结论：部门 sheet 专业汇总区域保留表格位置但内容为空，不再输出“专业相关/专业不相关”或替代描述；实习/正式/试用统计改为读取员工表 `status/probation`，其中一年以上实习生为 `status=3` 且 `probation>=12`。
- SQL 结论：`HrmEmployeeMapper#queryDepartmentDetailExportList` 新增返回 `a.probation`，供导出支持类按员工表试用期月数统计一年以上实习生。
- GREEN 证据：同一聚焦命令通过，21 个测试 0 failures/errors；部门明细组合回归 `mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，24 个测试 0 failures/errors；`mvn -DskipTests compile` 通过，保留既有 Maven 依赖 warning。

# Findings: 员工新增编辑薪资字段

## 2026-08-20 Discovery
- 本轮需求落在 `hainan` 后端员工管理与 `hr_web` 前端员工管理。
- 后端现有花名册导入已经把 `固定绩效` 作为员工动态字段保存，字段定义为 `FieldTypeEnum.DECIMAL` 且 `precisions=2`，值规范为两位小数字符串。
- 员工详情编辑页通过 `/hrmEmployee/personalInformation/{employeeId}` 读取 `hrm_employee_field/hrm_employee_data` 动态字段，再由前端 `EmployeeBaseInfo.vue -> DetailAddOrEdit.vue -> DynamicForm.vue` 保存到 `/hrmEmployee/updateInformation`。
- 前端新增员工弹窗 `hr_web/src/views/hrm/employee/Components/AddOrEdit.vue` 是手写固定表单，直接提交 `/hrmEmployee/addEmployee`；再次入职弹窗同组件提交 `/hrmEmployee/againOnboarding`。
- `/hrmEmployee/addEmployee` 入参 `AddEmployeeBO` 当前没有 `薪资等级`/`固定绩效` 字段，也不会保存个人信息动态字段；只改前端表单无法入库。
- `/hrmEmployee/againOnboarding` 入参是 `AddEmployeeFieldManageBO`，但当前 `AddOrEdit.vue` 再入职提交的是扁平 `ruleForm`，与 `againOnboarding` 期望的 `employeeFieldList/postFieldList` 不一致；本轮需兼容保存薪资动态字段。
- 员工详情页如果库内尚无 `薪资等级`/`固定绩效` 字段定义，编辑基本信息不会显示这两个字段；本轮应在后端读取个人信息前幂等补齐字段定义。
- 前端 `EmployeeBaseInfo.vue#getCustomFieldListValue(...)` 对 `number/floatnumber/percent` 统一 `parseInt`，会把 `固定绩效=1234.50` 回显成 `1234`，需要改为小数字段保留两位数值。
- RED 后端证据：`HrmEmployeeServiceImplSalaryFieldsTest` 首次运行在 testCompile 阶段失败，直接证明新增/再入职 BO 尚未承载 `薪资等级` 与 `固定绩效` 入参。
- RED 前端证据：`employee-salary-fields-ui.test.mjs` 首次运行失败于新增员工弹窗缺少 `salaryLevel` 初始字段，证明当前 UI 不会提交薪资等级。
- 实现结论：后端新增 `ensureEmployeeSalaryDynamicFields()` 幂等补齐/纠正 `薪资等级` 与 `固定绩效` 两个个人信息动态字段；历史 `固定绩效` 若为文本或在社保分组，会复用原字段并纠正为个人信息小数字段。
- 新增/再次入职保存结论：`salaryLevel/fixedPerformance` 作为扁平 BO 字段提交，保存时只删除并重写当前员工这两个字段 ID 的动态值，不影响其它个人动态字段。
- 编辑保存结论：员工详情动态字段保存前会识别 `固定绩效`，并保存为两位小数字符串；`EmployeeBaseInfo.vue` 小数字段回显改为 `Number(...)`，不再用 `parseInt` 截断。
- 前端实现结论：`AddOrEdit.vue` 新增 `薪资等级` 文本框和 `固定绩效` 两位小数数字框，再次入职回显兼容 camelCase、snake_case 和中文字段名。
- 验证结论：后端新增测试与员工导入/司龄回归通过，后端编译通过；前端新增测试、员工表单回归和生产构建通过。最终 fresh 验证中，后端聚焦回归为 26 个测试 0 failures/errors。
- 追加根因：远端当前 `/hrmEmployee/personalInformation/1831601326890434579` 返回的 `information` 共 40 个字段，但不包含 `薪资等级` 或 `固定绩效`；因此前端详情基本信息编辑弹窗按接口字段渲染时没有这两个输入框。
- 追加设计：前端 `EmployeeBaseInfo.vue` 需要在接口字段缺失时补齐两个编辑字段定义，保证用户能看到输入框；后端 `updateInformation(...)` 保存前需要按字段名/中文名解析真实动态字段定义并补上 `fieldId/type/labelGroup`，保证前端兜底字段仍能写入 `hrm_employee_data`。
- 追加 RED 证据：前端测试失败于缺少 `ensureSalaryInformationFields`；后端测试失败于缺少 `normalizeEmployeeSalaryInformationFields(...)`，锁定编辑页字段兜底和保存兜底都尚未实现。
- 追加保存风险：项目 `BaseServiceImpl#saveBatch(...)` 对 `IdType.ASSIGN_ID` 只把生成主键放入插入参数 Map，不回写实体对象；因此 `ensureEmployeeSalaryDynamicFields()` 若用 `saveBatch` 自动创建缺失字段，随后 `saveEmployeeSalaryDynamicFields(...)` 拿到的 `fieldId` 仍可能为空，导致动态值无法关联真实字段。
- 追加修复结论：缺失薪资字段改为逐个 `employeeFieldService.save(...)` 创建；编辑保存前通过 `normalizeEmployeeSalaryInformationFields(...)` 补齐真实字段定义；前端 `EmployeeBaseInfo.vue` 通过 `ensureSalaryInformationFields(...)` 保证基本信息编辑框可见。
- 再次反馈根因：员工列表操作列里的“调整部门/岗位”“晋升/降级”“办理转正”使用 `employee.js` 中的 `changePostModel/officialModel` 和 `DetailAddOrEdit.vue`，不是 `EmployeeBaseInfo.vue` 基本信息编辑链路；这些员工变更弹窗未包含 `薪资等级/固定绩效`，后端 `/become`、`/changePost`、`/promotion` 也只接 `HrmEmployeeChangeRecord`，未保存薪资动态字段。
- 再次反馈 RED 证据：`node tests/employee-salary-fields-ui.test.mjs` 失败于“员工修改弹窗字段模型必须包含薪资等级文本字段”；`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 在 testCompile 阶段失败于 `HrmEmployeeChangeRecord` 缺少 `setSalaryLevel/setFixedPerformance/getSalaryLevel/getFixedPerformance`。
- 再次反馈修复结论：前端 `employee.js` 新增共享 `salaryChangeFields`，并插入 `officialFields` 与 `getChangePostFields(...)`；员工列表 `Index.vue` 打开 `办理转正/调整部门岗位/晋升降级` 弹窗时从当前行回填 `salaryLevel/fixedPerformance`。
- 再次反馈后端结论：`HrmEmployeeChangeRecord` 新增不落库的 `salaryLevel/fixedPerformance` 字段，`HrmEmployeeServiceImpl#change(...)` 在员工异动保存后检测显式薪资输入，并调用既有 `saveEmployeeSalaryDynamicFields(...)` 写入 `hrm_employee_data`；`固定绩效` 继续规范为两位小数字符串。
- 再次反馈 GREEN 证据：`node tests/employee-salary-fields-ui.test.mjs` 通过；`node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过；`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，10 个测试 0 failures/errors；`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeServiceImplCompanyAgeTest test` 通过，28 个测试 0 failures/errors；`mvn -DskipTests compile` 与 `npm run build` 均通过。
- 姓名详情抽屉根因：员工管理点击员工姓名打开 `Detail.vue` 抽屉时默认页签为 `EmployeePostInfo`，而 `薪资等级/固定绩效` 的只读展示和编辑兜底在 `EmployeeBaseInfo`；由于页签 lazy，默认打开页面不会挂载基本信息组件，所以用户在姓名详情页首屏看不到这两个字段。
- 姓名详情抽屉 RED/GREEN 证据：`node tests/employee-salary-fields-ui.test.mjs` 先失败于详情页默认页签不是 `EmployeeBaseInfo`；修复后同一测试通过，锁定 `tabCurrentName` 默认值和 `show(...)` 每次打开重置到基本信息页签。

# Findings: 员工花名册模板薪资字段

## 2026-08-20 Discovery
- 下载员工花名册模板接口为 `HrmEmployeeController#downloadEmployeeRosterTemplate`，旧实现直接读取 `export/employee_module.xlsx` 并原样写入响应流。
- 资源模板 `src/main/resources/export/employee_module.xlsx` 的 `田野农谷` sheet 已有 `薪酬福利` 分组和 `薪资类别/职务级别/薪资等级` 等列，但缺少用户要求的 `薪资级别` 和 `固定绩效`。
- 用户再次确认：模板已有 `薪资等级`，不需要也不应新增 `薪资级别`；仅新增 `固定绩效`，并放在已有 `薪资等级` 右侧。
- 花名册导入入口为 `HrmEmployeeServiceImpl#importEmployee`，读取第 1 行分组、第 2 行表头、第 3 行开始的数据；固定映射外的列会自动创建 `hrm_employee_field` 并写入 `hrm_employee_data`。
- 旧导入逻辑在 `ensureRosterDynamicFields(...)` 中统一把新增动态字段创建为 `FieldTypeEnum.TEXT`，因此 `固定绩效` 不会按小数字段入库。
- `HrmEmployeeField` 支持 `type` 和 `precisions` 字段，`FieldTypeEnum.DECIMAL` 的值为 `6`，可用于保存 `固定绩效` 两位小数字段定义。
- 实现选择：不直接修改二进制模板文件，而是在下载时用 POI 打开模板资源，只对可见且表头同时包含 `姓名`、`个人电话` 的花名册样式 sheet 插入 `固定绩效` 列。
- 模板补列时，`固定绩效` 插入到已有 `薪资等级` 右侧，分组写 `薪酬福利`，默认列样式为 `0.00`；不再新增 `薪资级别` 列。
- 导入时 `固定绩效` 作为 `DECIMAL` 动态字段保存，`precisions=2`。
- 如果历史上已经存在文本型 `固定绩效` 动态字段，导入时会复用该字段并调用 `employeeFieldService.updateById(...)` 将类型纠正为 `DECIMAL`、精度纠正为 `2`，避免继续生成重复字段或保留错误字段定义。
- 写入员工动态数据前，`固定绩效` 会去掉千分位逗号并用 `BigDecimal` 规范为两位小数字符串；例如 Excel 中数字 `1234.5` 保存为 `1234.50`。
- 数据库脚本结论：当前方案没有新增 `hrm_employee` 或其它表的物理列，只复用动态字段元数据和值表，所以不需要新增数据库 DDL 脚本；第一次导入包含 `固定绩效` 的模板时，会自动生成或纠正动态字段定义。
- RED 证据：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 失败于模板缺少 `薪资级别`，以及 `固定绩效应创建为小数字段 expected:<6> but was:<1>`。
- 历史字段兼容 RED 证据：`mvn -Dtest=HrmEmployeeServiceImplImportEmployeeTest#importEmployee_shouldCorrectExistingFixedPerformanceFieldToTwoDecimalNumber test` 失败于 `employeeFieldService.updateById(...)` 未调用。
- GREEN 证据：实现后同一命令通过，14 个测试 0 failures/errors；追加历史字段兼容后，`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeBasicInfoExportSupportTest test` 通过，19 个测试 0 failures/errors。
- 再次修改 RED 证据：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 失败于模板仍包含新增 `薪资级别`。
- 再次修改 GREEN 证据：同一命令通过，15 个测试 0 failures/errors。

# Findings: 员工管理下载部门明细方案

## 2026-08-19 Discovery
- 参考文件 `/Users/jiangyongming/Desktop/农谷导入数据/各部门现有人员明细表(更新版).xlsx` 存在，包含 9 个可见 sheet：`人员总表`、`财务部`、`行政人力资源部`、`采购部`、`仓储部`、`工程部`、`生产部`、`质量管理部`、`源味公司`。
- `人员总表` 是汇总页，包含企业总人数、月固定薪资/绩效/社保/公积金/伙食成本汇总，以及按部门横向汇总。
- 每个部门 sheet 的结构基本一致：第 1-6 行是部门统计和成本区，第 7-8 行是标题/空行，第 9 行为人员明细表头，第 10 行开始是人员数据。
- 部门明细表头为：`序号、姓名、岗位、入职年限、性别、年龄、学历、专业、薪资级别、薪资待遇、合同到期日、年平均考核分、理论考核分`。个别 sheet 的薪资待遇列带说明“主管级以下不含满勤”。
- 当前后端员工管理已有 `/hrmEmployee/exportBasicInfoTemplate`，实现是 `HrmEmployeeController -> IHrmEmployeeService#exportBasicInfoTemplate -> HrmEmployeeServiceImpl#queryPageList -> EmployeeBasicInfoExportSupport`。
- `queryPageList` 已复用员工管理筛选与 `employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.EMPLOYEE_MENU_ID)` 数据权限，并返回 `employeeId/employeeName/mobile/sex/age/highestEducation/entryTime/deptId/deptName/post/postLevel/companyAge/contractSignCount/lastContractEndTime` 等字段。
- 已有基础信息导出会把 `pageType=0, limit=10000` 后调用 `queryPageList`，再用 `WebFileUtils.download(...)` 输出 Excel Blob；这是本功能最适合复用的筛选/权限和下载模式。
- 前端员工管理页 `hr_web/src/views/hrm/employee/Index.vue` 已有“员工导入/导出”下拉，包含“导入员工”“导出员工基础信息”“下载员工花名册模版”；下载动作均有独立 loading/disabled，并复用 `triggerDownloadBlob(...)`。
- 参考表中的“专业”可从 `hrm_employee_education_experience.major` 补充；“薪资级别”当前基础信息导出已通过动态字段兼容“薪资等级/薪资类别/职务级别”；“年平均考核分/理论考核分”在员工列表当前结果中没有直接字段，只看到绩效档案有最近考核分 `recentlyScore`，理论考核分未定位到现成字段。
- 成本类汇总若要完全动态，需要明确口径：固定薪资/绩效可考虑薪资档案项，社保/公积金可考虑当前参保方案或月度社保记录，伙食成本目前未发现稳定员工档案数据源。
- 用户已选择方案 A：新增动态导出，参考 Excel 的多 sheet 样式与结构，数据从系统当前员工、部门、薪资/考核等表读取；不采用固定 Excel 文件原样下载。
- 用户已确认导出范围为当前登录公司内全部未删除且在职员工，即 `hrm_employee.is_del=0 and hrm_employee.entry_status in (1,3)`；该下载不跟随员工管理页面姓名、部门、状态、高级筛选或分页条件。
- 用户已确认 `薪资待遇` 字段口径：读取每个员工薪资档案中设置的 `10101 / 基本工资`、`10102 / 岗位工资`、`10103 / 职务工资`，取三项金额之和。
- 用户已确认考核分相关字段不需要；部门成本当前找不到系统来源，第一版不计算、不填充，不能硬推。
- 用户已确认最终部门明细表头直接删除“年平均考核分”“理论考核分”两列，不保留空列。
- 用户已确认下载文件名规则：公司名称取组织管理中最顶层数据，拼接 `人员明细表` 和导出当天年月日；方案中统一记录为 `<顶层组织名称>人员明细表yyyyMMdd.xlsx`。
- 实现阶段决定：部门明细导出不复用页面当前筛选入参，后端单独查询 `hrm_employee.is_del=0` 的全量员工；仍运行在当前租户/当前登录公司数据源中。
- `薪资级别` 优先取员工动态字段中的 `薪资级别/薪资等级/薪资类别/职务级别`，缺失时回退员工岗位职级 `postLevel`。
- `专业` 从 `hrm_employee_education_experience.major` 取首条有专业的教育经历，排序口径与学历兜底一致。
- 实现结果：
  - 后端已新增 `HrmEmployeeController#exportDepartmentDetail`、`IHrmEmployeeService#exportDepartmentDetail`、`EmployeeDepartmentDetailExportSupport` 和 `HrmEmployeeMapper#queryDepartmentDetailExportList`。
  - 前端已新增 `exportEmployeeDepartmentDetail(...)` 和“下载部门明细”按钮，下载时不提交员工页筛选，文件名优先使用后端 `Content-Disposition`。
  - 部门 sheet 名已做 workbook 级去重，清洗后重名会自动递增后缀，避免 Excel 直接抛 `The workbook already contains a sheet named ...`。
  - `薪资待遇` 按 `10101 + 10102 + 10103` 计算，考核分列已删除；部门成本在初版因来源未确认暂未计算，后续已按第 42-46 条追加口径改为填充。
  - 验证已通过：后端聚焦测试、后端编译、前端 API/UI/布局回归和前端构建均通过。
- 404 排查结论：本地 `127.0.0.1:9080/hrsystem/hrmEmployee/exportDepartmentDetail` 已暴露接口，未带 token 返回“请输入token”；远端 `153.0.237.99:9080` 带 token 返回 `No handler found for POST /hrsystem/hrmEmployee/exportDepartmentDetail`，说明公网后端尚未发布该接口。`npm run dev` 原先没有 `.env.development`，Vite 开发代理回退到远端默认值，所以点击按钮命中了旧后端 404。
- 修复结论：前端新增 `.env.development`，将开发代理固定到 `http://127.0.0.1:9080`；重启 Vite 后通过 `127.0.0.1:8080/api/...` 访问新接口返回 `200 OK` 和有效 xlsx。
- 2026-08-19 追加排查 `127.0.0.1:8081` 点击失败：本机 8081 代理带 token 直接请求 `/api/hrsystem/hrmEmployee/exportDepartmentDetail` 可返回 `200 OK` 和有效 xlsx，但当时 `html/assets/index-*.js` 内仍写死 `http://153.0.237.99:8081`，浏览器点击按钮实际跨到远端 8081；远端同接口带 token 返回 `404 No handler found`。
- 2026-08-19 追加修复：执行 `npm run deploy:access -- --local-only` 重建本机 `html/` 并重启 8081；同时修复 `scripts/redeploy-access.mjs` 的过期加班/夜班静态字段文案校验，并将慢接口验收超时从默认 10 秒对该组请求放宽到 90 秒。最终本机 8081 部门明细接口返回 `200 OK`，下载文件为有效 OOXML。
- 2026-08-19 追加结构差异：当前 `EmployeeDepartmentDetailExportSupport#writeDepartmentSummary(...)` 只输出“部门/人数”两行，和参考表的部门 sheet 顶部 1-8 行不一致。
- 参考表部门 sheet 固定结构为：第 1 行 A-F 分别是 `部门、部门总人数、男女比例、学历层次、工年结构、专业汇总`，F1:H1 合并；A2:A6、B2:B6、C2:C6 合并；F2:H3、F4:H6 合并；I2:J6 为成本区键值，K 列可放备注。第 7-8 行 A:H 合并显示 `<部门>人员明细表`，第 9 行开始人员明细表头。
- 用户已明确“导出的表要和参考的表结构相同”，因此部门 sheet 顶部应补齐男女比例、学历层次、工龄结构、专业汇总、实习期要求统计和成本区结构；当时成本来源仍未确认，后续已按薪资档案和最近有效社保月记录补齐。
- RED 证据：新增 `EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldMatchReferenceDepartmentTopSummaryStructure` 后，旧实现失败于 `expected:<部门总人数> but was:<生产部>`，确认当前导出没有参考表顶部结构。
- 实现结论：部门 sheet 已按参考表补齐第 1-8 行结构和合并区域，人员明细表头仍固定在第 9 行；A-H 统计区按当前部门员工列表动态计算，I-K 成本区后续改为填充 `月固定薪资成本`、`月绩效薪资成本`、`月社保成本`、`月公积金成本` 四项金额，并移除 `月伙食成本`。
- 统计口径：男女比例按 `sex` 统计；学历按本科、大专、中专、高中、初中五档输出，硕士/博士/博士后归入本科，中职/中技归入中专，小学归入初中；工龄优先解析 `companyAge`，缺失时按 `companyAgeStartTime/entryTime` 兜底；专业汇总因无“专业相关性”字段，第一版把有有效专业信息视为“专业相关”，空值或 `/` 视为“专业不相关”。
- 2026-08-19 追加合同到期日根因：部门明细导出原 SQL 使用 `fc.start_time desc, fc.contract_id desc` 取 `lastContractEndTime`，并带 `fc.start_time is not null` 过滤；这会与员工管理“员工合同”页面按 `sort asc` 展示的合同列表不一致，也会跳过开始日期为空但截止日期已维护的合同记录。
- 修复结论：`queryDepartmentDetailExportList` 的 `lastContractEndTime` 改为直接从 `hrm_employee_contract.end_time` 取值，按 `coalesce(fc.sort, -1) desc, fc.start_time desc, fc.contract_id desc` 取最新一条，去掉 `start_time is not null` 过滤。
- 只读抽样：`hr_0003` 当前未发现按 `sort` 最新与按 `start_time` 最新结果不同的员工样本，因此实际下载文件无法用现有数据区分新旧 SQL；但抽样员工黎冬霜、严锦的导出合同到期日分别与新口径查询出的 `2027-04-30`、`2026-07-19` 一致。
- 2026-08-20 全量复核：黎冬霜 `employee_id=1831601326890434579` 为在职未删除，合同列表按 `sort asc` 最后一条为 `sort=39, end_time=2027-04-30`；本地 8081 实际下载文件与 dev 库 `hr_0003` 同口径全量对账，期望 92 人、实际 92 人，有合同到期日均为 15 人，差异 0，黎冬霜 Excel 行为 `采购计划部` 第 10 行，合同到期日 `2027-04-30`。
- 2026-08-19 追加成本区根因：`EmployeeDepartmentDetailExportSupport#writeDepartmentSummary(...)` 旧实现只写成本区标题，J 列金额固定为空，并保留了 `月伙食成本`；这是每个部门 sheet 成本为空的直接原因。
- 数据源确认：月固定薪资成本可复用当前导出已加载的员工薪资档案 `10101/10102/10103`；月绩效薪资成本按当前薪资档案 `41001 / 绩效工资` 汇总，未维护时显示 `0`；月社保成本/月公积金成本读取最近有效 `hrm_insurance_month_emp_record` 的 `corporate_insurance_amount/corporate_provident_fund_amount`。
- 只读数据抽样：`hr_0003` 薪资档案当前未维护 `41001`，因此实际导出的月绩效薪资成本会显示 `0`；最近有效社保月记录为 2026-07，企业社保/企业公积金存在可汇总金额。
- 实现结论：`HrmEmployeeServiceImpl#exportDepartmentDetail` 现在加载薪资档案项 `10101/10102/10103/41001`，并按最近 `status=1` 社保员工月记录回填企业社保、公积金成本；`EmployeeDepartmentDetailExportSupport` 汇总部门员工 J2-J5 四项金额，成本区零值显示为 `0`，并删除 `月伙食成本`。
- 下载抽检：本机 `127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 下载有效 OOXML；抽检 `采购计划部`、`行政人力资源部` 等部门 sheet，J2-J5 已有金额，J6 空白，整本 workbook 未检出 `月伙食成本`。
- 2026-08-20 追加固定绩效根因：部门明细导出已通过 `buildEmployeeBasicInfoDynamicFields(...)` 加载员工动态字段，但 `writeDepartmentSummary(...)` 未接收动态字段，明细 `薪资待遇` 也只把 `salaryTreatment(...)` 的 `10101/10102/10103` 合计直接写出，因此王洪平这类维护了 `固定绩效` 的员工仍显示 `4150`，部门 `月固定薪资成本` 也未加固定绩效。
- 实现结论：`EmployeeDepartmentDetailExportSupport` 现在把 `dynamicFieldValues` 贯通到部门汇总；`薪资待遇` 在动态字段 `固定绩效` 非空时显示为 `<固定薪资>(固定)+<固定绩效>(绩效)`，例如 `4150(固定)+4150(绩效)`；`月固定薪资成本` 按 `10101/10102/10103 + 固定绩效` 汇总。
- 口径边界：动态字段 `固定绩效` 不写入也不影响薪资档案 `41001 / 绩效工资`；`月绩效薪资成本` 继续只按 `41001` 汇总。若 `固定绩效` 显式维护为 `0.00`，明细仍展示绩效段 `0(绩效)`，固定成本金额不额外增加。

## 2026-08-20 王志兰未出现在部门明细下载的核验
- `hr_0003.hrm_employee` 中王志兰记录为 `employee_id=1831601326890434649`，`is_del=0`、`entry_status=1`、`status=1`，部门为“研发部”，岗位为“质量管理部副经理”。
- 部门明细导出 SQL `HrmEmployeeMapper.xml#queryDepartmentDetailExportList` 只过滤 `a.is_del=0`、`a.entry_status in (1,3)`，不使用页面筛选或分页；王志兰满足导出条件。当前 `hr_0003` 符合条件员工共 92 人。
- 实际调用本地接口 `POST /hrsystem/hrmEmployee/exportDepartmentDetail` 下载 `/tmp/hainan_department_detail_check.xlsx`，HTTP 返回 200，文件为合法 xlsx；在文件内容中直接检索到“王志兰”，位置为“研发部”sheet。
- 导出行同时包含“质量管理部副经理”“8年2月27天”“61”“2000(固定)+500(全勤)”，说明不是姓名列偶然命中，而是完整员工明细已生成。
- 当前代码和 `hr_0003` 数据均能导出王志兰。若现场文件缺少她，优先排查旧后端/旧前端静态包、浏览器打开了旧下载文件、实际登录租户不是 `0003`，或请求仍指向公网旧部署；暂未发现需要修改导出筛选 SQL 的证据。

# Findings: 排班上传休假未录入修复

## 2026-08-18 Pre-work
- 已读取项目需求/开发文档和既有规划记录。
- 当前项目为 Spring Boot 后端，排班管理入口集中在 `WorkPlanListController` 与 `WorkPlanServiceImpl`。
- 历史文档明确：上传排班模板每日分组包含“白/夜班、上班时间、下班时间、是否连班、休假/调休”；JSON 进入 `/workPlan/saveAll` 后由服务层按 `shiftType/customStart/customEnd` 等字段创建提交任务。
- `tbplanlist` 已有 `rest_shift_type` 字段用于区分休息类排班子类型：`adjust=调休`、`rest=休息`。
- 当前文档没有明确“休假”是否应作为 `rest` 的输入别名；本轮按用户反馈将“休假”视为模板合法值，需补入解析和测试。
- 根因定位：
  - 横版模板解析 `isHorizontalTruthy(...)` 只识别 `是/休/休息/调休/true/1`，不识别 `休假`，导致“休假/调休”列填写 `休假` 时整天被当成空排班跳过。
  - 服务层 `normalizeShiftTypeValue(...)` 只把 `调休/休息` 归一为 `rest`，不识别 `休假`；单日保存或 JSON 上传传入 `休假` 时会进入需要上下班时间的非休息类分支。
  - `normalizeRestShiftType(...)` 只把 `rest/休息/4` 归一为 `rest_shift_type=rest`，不识别 `休假`；即使上游把班次识别成休息类，也会默认落为 `adjust=调休`。
- RED 证据：`WorkPlanServiceImplTest#previewImportExcel_shouldTreatHorizontalVacationAsRestShift` 失败于期望 2 条实际 1 条；`#saveEmployeeDayShift_shouldTreatVacationTextAsRestShift` 失败于“开始时间不能为空”。
- 修复结论：`WorkPlanServiceImpl` 现在在横版模板有效值识别、横版休息文案构造、`normalizeShiftTypeValue(...)` 和 `normalizeRestShiftType(...)` 中支持 `休假`。
- 行为结果：上传模板中 `休假` 与 `休息/休` 一样保存为 `shift_type=rest, rest_shift_type=rest`；`调休` 仍保存为 `rest_shift_type=adjust`。
- 验证结论：新增聚焦测试通过；排班服务和控制器回归 `WorkPlanServiceImplTest,WorkPlanListControllerTest` 通过 62 个测试。

# Findings: prod 一键打包脚本

## 2026-08-18 Design
- 用户确认 `application-prod.properties` 是服务器部署使用的配置文件，不能删除或改成 dev。
- 新需求：生成一键打包脚本，脚本保存位置需要和 Maven package 产物保存位置相同，打包使用 prod 配置文件。
- 约束冲突：源码 `application.properties` 必须默认 `dev`，否则 `ApplicationProfileConfigTest` 会阻止 `mvn package`；但服务器部署包需要 jar 内默认 `spring.profiles.active=prod`。
- 设计选择：脚本放在 `target/package-prod.sh`；运行时复制一份临时项目到 `/tmp`，只在临时副本里把 `application.properties` 改为 `prod`，执行 `mvn -DskipTests package`，再将 jar 复制回当前项目 `target/`。
- 脚本最后解包校验 `BOOT-INF/classes/application.properties` 是否包含 `spring.profiles.active=prod`，并确认 `BOOT-INF/classes/application-prod.properties` 存在。

## 2026-08-18 Result
- `target/package-prod.sh` 已创建并赋予执行权限。
- 脚本执行成功，生成并覆盖 `target/hrsystem-0.0.1-SNAPSHOT.jar`。
- 产物校验成功：jar 内 `BOOT-INF/classes/application.properties` 为 `spring.profiles.active=prod`，且 `BOOT-INF/classes/application-prod.properties` 存在。
- 源码 `src/main/resources/application.properties` 仍保持 `spring.profiles.active=dev`，`mvn -Dtest=ApplicationProfileConfigTest test` 通过，未破坏本地默认 profile 契约。

## 2026-08-18 One-click Entry
- 用户要求把脚本改成“一键执行”。
- 实现选择：新增 `target/package-prod.command` 作为 macOS Finder 可双击入口；保留 `target/package-prod.sh` 作为底层打包逻辑。
- `.command` 设置常见 Homebrew/Maven PATH，进入自身所在 `target/` 目录后调用 `package-prod.sh`，结束后输出 jar 路径；交互终端中会等待回车，避免双击后窗口立刻关闭。
- 验证结果：`zsh -n target/package-prod.command` 通过；`./target/package-prod.command` 执行成功；生成的 jar 内默认 profile 为 `prod`，源码 `application.properties` 仍保持 `dev`。

# Findings: Maven package 失败排查

## 2026-08-17 Pre-work
- 用户反馈 Maven `package` 阶段报错，但未提供具体日志；本轮先在当前工作区复现。
- 项目文档约束：这是 Java / Spring Boot 后端；开发环境中 IDE 报错需以 Maven 编译/测试结果判断真实源码问题。
- 开发文档记录：项目近期 Maven 编译可保留既有 POM/dependency warnings，不能把 warning 当作失败根因；需要读取 Maven 输出中的首个 `[ERROR]`。
- 当前工作区已有大量历史改动和未跟踪文件，本轮排查需避免回退无关改动。

## 2026-08-18 Root Cause
- `mvn package` 的首个真实失败不是编译错误，而是 Surefire 测试失败：`ApplicationProfileConfigTest#activeProfileLoadsDatasourceUrlForDebugStartup`。
- 失败断言为 `expected:<dev> but was:<prod>`；测试读取 `application.properties` 中的 `spring.profiles.active`，并要求默认 profile 加载 `application-dev.properties` 的 JDBC URL。
- `git diff -- src/main/resources/application.properties` 显示该文件曾从 `spring.profiles.active=dev` 改为 `prod`；这和 `docs/requirements.md` 中“本地/IDE 调试默认 dev profile”的要求冲突。
- 用户已将第一行改回 `spring.profiles.active=dev`；当前 diff 仅剩钉钉调用量配置项，不再包含 profile 从 dev 到 prod 的变更。
- 聚焦验证 `mvn -Dtest=ApplicationProfileConfigTest test` 通过，完整 `mvn package` 通过 505 个测试并生成 jar。

# Findings: 薪资导出 Excel 批注说明

## 2026-08-17 Pre-work
- 用户要求：薪资导出 Excel 每个相关单元格添加注解，说明满勤奖为空原因、超缺勤工资计算过程和依据、个税计算过程和依据、工会费为空原因。
- `watch` 项目未找到真实“导出薪资”链路；真实后端落在 `hainan`，前端 `hr_web` 的“导出薪资”只负责范围选择和下载。
- 前端文档确认：导出薪资由 `SalaryManage.vue` 打开 `AloneComputeDialog.vue`，最终调用后端导出接口；显式选择范围时只提交 `salaryRecordId + employeeIds`。
- 后端文档确认：
  - `40102 / 全勤奖` 必须使用计薪员工查询返回的 `fullMoney`，不能硬编码；未转正、未启用全勤、残疾特殊口径、存在有效病假、应计出勤不足或缺统计数据时不得发放。
  - `200101 / 超缺勤` 来自考勤扣款合计，近期规则要求应出勤与应计出勤统一读取加班/夜班统计落库结果。
  - `230101 / 个人所得税` 采用累计预扣数据闭合计算，不能硬编码补差。
  - `160102 / 工会费` 按应发工资 `0.5%` 计算，但成都/攀枝花、应发工资小于等于 0、实习/离职、半路转正等场景豁免。
- 代码定位：`SalaryMonthRecordServiceNew#exportSalaryNew(...)` 生成表头、构造 `dataRow`、按 `fieldOrder` 写 Excel；`buildSalaryDataRow(...)` 已把 `全勤奖`、`超缺勤`、`个人所得税`、`工会费` 写入导出行。
- Excel 模板确认：两套薪资模板员工明细行从 Excel 第 5 行开始（POI `rowIndex=4`）；目标列固定为 `R(17)=全勤奖`、`T(19)=超缺勤`、`V(21)=个人所得税`、`Z(25)=工会费`。
- 实现结论：新增 `SalaryExportCommentWriteHandler`，在 EasyExcel 写入目标单元格时用 `rowIndex - 4` 映射 `salaryExportList`；小计/合计行跳过，只给员工行写批注。
- 批注内容只使用当前导出行 `HrmSalaryExport` 值和既有业务口径说明，不重新计算薪资，不改变工资项或列顺序。
- TDD 证据：新增测试先因缺少 `SalaryExportCommentWriteHandler` 红灯；实现并注册后 `SalaryMonthRecordServiceNewTest` 通过 68 个测试。

# Findings: 残疾员工工会费计算规则调整

## 2026-08-17 Pre-work
- 用户要求：修改逻辑，残疾人也有工会费。
- 项目文档确认薪资核心规则在 `modules/salary/service`，工会费工资项编码为 `160102`，历史记录明确工会费按应发工资 `0.5%` 生成。
- 既有文档和历史排查记录显示，当前代码约定 `hrm_employee.is_disabled=1` 表示残疾员工，`2` 表示非残疾员工；残疾员工当前被视为免个税，并且旧逻辑也免工会费。
- 初步定位：`SalaryComputeServiceNew#computeSalary(...)` 当前只有 `isDisabled.equals("2")` 时才调用 `calculateUnionFee(...)`；因此残疾员工工资项 `160102` 会保持 `0`。
- 设计决策：新规则应只移除“残疾免工会费”条件，继续保留残疾员工免个税；工会费是否实际收取仍交由 `calculateUnionFee(...)` 判断公司、应发工资、员工状态和半路转正规则。
- RED 证据：新增 `SalaryComputeServiceNewTest#computeSalary_disabledEmployee_shouldPayUnionFeeAndSkipTax` 后，旧逻辑下 `mvn -Dtest=SalaryComputeServiceNewTest test` 失败于残疾员工工会费仍为 `0`。
- 实现结论：`SalaryComputeServiceNew#computeSalary(...)` 现在无论残疾状态都先调用 `calculateUnionFee(...)` 生成工会费；个税仍只有 `isDisabled.equals("2")` 时才计算。
- 验证结论：薪资计算定向测试和薪资主服务组合测试均通过；后端编译通过，保留既有 Maven POM/dependency warnings。

# Findings: 后端调试模式 jdbcUrl 缺失排查

## 2026-08-16 Pre-work
- 用户反馈：后端开启调试模式时提示 `jdbcUrl is required with driverClassName`。
- 已读取项目需求/开发文档：项目为 Java/Spring Boot 后端，存在自建动态多租户数据源；历史连接池改造涉及 `DynamicDataSource`、`CompanyDataSourceProvider`、`MyBatisConfig`、`ConnectionParsor` 和 `DataSourcePoolConfigurator`。
- 文档约束：开发环境中 IDE 红线或调试差异需以 Maven 编译/启动配置为准；不要先按 IDE 提示改业务逻辑。
- 初步判断：该错误是 Hikari 在设置了 `driverClassName` 但没有拿到 `jdbcUrl` 时抛出，重点排查调试 profile、配置键绑定和动态数据源创建路径。
- 根因确认：`src/main/resources/application.properties` 里写的是 `spring.profiles.activ=dev`，缺少末尾 `e`；`ConnectionParsor#getDefaultConnection()` 查找 `spring.profiles.active`，因此读不到 `dev`，不会继续加载 `application-dev.properties` 中的 `spring.datasource.url`。
- 修复结论：将配置键改为 `spring.profiles.active=dev` 后，默认 profile 合并可读取 MySQL JDBC URL；新增 `ApplicationProfileConfigTest` 锁定该契约。

# Findings: 顶栏钉钉 API 调用量展示与提醒

## 2026-08-16 Pre-work
- 用户要求：系统右上角新增钉钉 API 调用量显示；超过 80% 时描述区域红色；每天上午一次、每天下午一次弹窗提醒；弹窗支持“本月不再提示”；鼠标移入显示各功能调用量分布百分比和柱状图。
- 后端文档已有钉钉调用量异常与优化记录：旧同步链路曾因员工数、日期、审批流程多重循环把付费 API 调用量放大，且 `postresultlog` 不是完整调用台账，只覆盖部分写了日志的接口。
- 现有业务约束强调：普通展示/查询链路应尽量读取本地快照，避免为页面展示额外在线调用钉钉。
- 前端文档确认右上角已有 `TopHeader.vue` 系统设置齿轮入口，受登录 `menuTree` 控制；本轮调用量展示应作为全局顶栏状态组件，不改变菜单权限体系。
- 初步设计假设：后端从本地钉钉调用日志或任务结果记录聚合本月调用量与功能分布，前端只展示后端统计结果；提醒节流按当前用户 + 当前月份 + 上午/下午保存在浏览器本地状态，避免新增复杂用户偏好表，除非代码已有可复用偏好接口。
- 实现口径确认：
  - 后端聚合 `postresultlog`，因为该表包含 `postUrl/className/createTime`，能在不新增钉钉请求的前提下展示本月系统已记录调用量；
  - `ddtaskresult` 更偏异步报表任务队列，不直接代表一次 API 请求，本轮不纳入总调用量；
  - 文档已明确该功能展示“系统已记录调用量”，不是钉钉后台账单的完整替代。
- 后端实现结论：
  - 新增 `/dingTalkApiUsage/monthly`，返回本月调用总量、配置月额度、使用百分比、80% 阈值状态、描述和功能分布；
  - 月额度配置为 `dingtalk.api.monthly-limit`，默认 `10000`；阈值配置为 `dingtalk.api.usage-threshold-percent`，默认 `80`；
  - 功能分布按 URL/类名识别同步考勤排班、打卡明细、考勤组/班次、员工花名册、报表/请假时长、审批数据获取、排班提交/调组、获取访问令牌和其他钉钉接口。
- 前端实现结论：
  - `TopHeader.vue` 右侧新增紧凑的调用量按钮；
  - 鼠标悬浮展示每个功能的次数、分布百分比和横向柱状图；
  - 超过阈值时按钮/描述区域使用红色风险样式，并按当前用户、月份、日期、上午/下午在 `localStorage` 中节流提醒；
  - 弹窗提供“本月不再提示”，勾选后当前用户当前月份不再弹。

# Findings: 排班管理考勤信息改读同步数据

## 2026-08-12 Pre-work
- 用户反馈：排班管理中的考勤信息没有读取同步考勤后的数据，而是直接调用钉钉获取考勤数据接口，导致钉钉 API 调用量很快耗尽。
- 已读取项目要求/开发文档的相关约束：排班管理入口集中在 `WorkPlanListController` 和 `WorkPlanServiceImpl`；同步考勤链路负责把钉钉排班、打卡、报表、人员映射等数据落到本地表。
- 既有文档已明确过降低钉钉调用量原则：展示查询链路应优先复用本地已同步快照；仅本地快照为空时才允许有限兜底。本轮用户进一步要求排班管理考勤信息禁止调用钉钉接口，因此排班管理非同步查询链路应移除该兜底。
- 初步修复方向：先定位排班管理页面接口实际调用的服务方法和钉钉 manager，再写测试锁定“排班管理查询不触发钉钉接口”，最后改为本地 repository/mapper 查询。
- 根因定位：
  - `/attendanceData/getPlanDataByGroup` 是排班管理详情页接口，当前实现会调用本类私有 `getUserListByGroup(...)`，该方法直接请求钉钉 `topapi/attendance/group/memberusers/list`；
  - 同一接口随后按 7 天分段请求钉钉 `topapi/attendance/schedule/listbyusers` 获取排班；
  - 这两类调用都属于排班管理展示查询，不是明确的同步考勤入口，应改为读取同步后本地 `hrm_attendance_plan` 与 `tbattendanceuser`。
- 同类风险：
  - `WorkPlanServiceImpl#getUsersForDisplay(...)` 本地人员快照为空时会回退 `loadUsersFromDingTalk(...)`，展示链路仍可能消耗钉钉接口；
  - `WorkPlanServiceImpl#getAllGroupsForDisplay(...)` 缓存为空时仍通过 `getGroups(..., displayCache=true)` 调 `loadGroupsFromDingTalk(...)`；
  - 本轮应把展示缓存刷新改成只读本地快照；提交排班链路 `getUsers/getAllGroups/submitPlans/changeGroup` 保留钉钉调用，因为它们是实际提交/调组动作。
- 实现结论：
  - `/attendanceData/getPlanDataByGroup` 现在读取本地 `hrm_attendance_plan`，不再调用钉钉组成员或排班列表接口；
  - 员工姓名来自本地 `tbattendanceuser`，考勤组过滤兼容 `attendance_group_id/old_group_id`；
  - 同员工同日同班次上下班计划合并为一条展示记录，优先选 `OffDuty` 或较晚计划时间；
  - `getAllGroupsForDisplay` 与 `getUsersForDisplay` 的展示缓存刷新只读本地 `hrm_attendance_group/hrm_attendance_shift/tbattendanceuser`；
  - `getClassListByGroup` 在 Redis 缓存缺失时也能直接从本地组/班次表构造下拉，不再强制先走其它刷新接口。
  - 遗留 `/attendanceData/getShiftList` 也已改为读取本地 `hrm_attendance_shift`，避免班次选择入口实时调用钉钉 `attendance/shift/list`。
- 验证结论：
  - `HrmAttendanceDataControllerTest` 新增覆盖排班详情读取本地排班、兼容历史组 ID、班次缓存缺失本地兜底、遗留班次列表本地读取；
  - `WorkPlanServiceImplTest` 新增/调整覆盖展示组和展示人员缓存刷新不调用钉钉。
  - `HrmAttendanceDataController` 中 `schedule/listbyusers`、`attendance/group/memberusers/list`、`attendance/shift/list` 搜索无命中；`WorkPlanServiceImpl` 剩余组/成员钉钉调用仅保留在提交/调组等写入链路。

# Findings: 排班管理矩阵翻页修复

## 2026-08-12 Evidence
- 用户反馈：排班管理中的翻页没有效果。
- 后端与前端文档均确认排班管理主视图是 `/hrm/attendance/scheduling` 的员工月度矩阵，数据源为 `/workPlan/getData` 加 `getAllUsers`，前端先按员工透视成矩阵行。
- 当前 `Scheduling.vue` 将完整 `employeeMatrixRows` 传给公共 `Table`，并把 `:total` 设置为 `employeeMatrixRows.length`，但没有监听 `@get-data`。
- 公共 `Table.vue` 不会自动切分传入的 `data`；点击分页只改变内部 `pageIndex/pageSize` 并 emit `get-data`。父组件未响应事件时，表格仍展示原完整数组，所以表现为翻页无效。
- 根因假设：排班页缺少员工矩阵行的本地分页切片与分页事件处理；修复应保持后端全量月记录加载，再在前端对矩阵员工行分页。
- 已补 RED 测试：旧代码缺少 `buildWorkPlanMatrixPageRows` 导出，同时 `Scheduling.vue` 未绑定 `pagedEmployeeMatrixRows` 和 `@get-data="handleMatrixPageChange"`。
- 修复后 `Scheduling.vue` 维护独立 `matrixPage`；公共 `Table` 翻页事件更新该状态，表格只展示当前页员工矩阵行。
- 筛选、重置、切换月份会回到第一页；导出仍读取完整 `employeeMatrixRows`，避免只导出当前页。
- 最终验证：排班工具、排班管理页、排班 API、添加排班页、时间选择器相关 Node 测试均通过；`npm run build` 通过，仅保留项目既有 `::v-deep` 和 chunk size warnings。

# Findings: 加班夜班统计三项出勤可编辑保存

## 2026-08-10 Pre-work
- 用户要求：加班/夜班统计中的“单人查看”和“显示所有”两处，应出勤时间、实际出勤时间、应计出勤三列全部改为可编辑，并能保存入库。
- 后端需求文档已明确加班/夜班统计页面、日明细、月度明细和汇总依赖统计明细数据；行政导出和薪资也读取这些三项出勤值，因此保存必须落到后端统计明细来源，不能只做前端临时状态。
- 前端需求文档已记录实际出勤矩阵表头为“姓名、应出勤时间、实际出勤小时(天数)、应计出勤小时(天数)、1日到月末、备注”，并且单人查看弹窗与“显示所有”页面使用同类矩阵；当前没有可编辑保存要求。
- 初步设计选择：复用现有表格内联编辑模式，新增后端单元格保存接口；保存字段白名单只允许三项出勤，避免误改每日明细、加班小时、夜班次数等统计项。
- 进一步代码定位后调整设计：现有生成统计会把月度出勤值写到同一员工同月每条日明细；本轮保存也应按“员工 + 月份”批量覆盖该月所有明细，避免页面聚合多条日明细时取值不一致。
- `actualAttendanceHours` 当前是查询层计算值，`hrm_overtime_night_statistics_detail` 只有 `actual_attendance_days` 整数天，无法保存 `185.5` 这类实际出勤小时；本轮需补 `actual_attendance_hours` 入库列。为防止保存后被 `rest_type` 刷新逻辑覆盖，还需补人工调整标记列。
- RED 验证结果：
  - 后端 `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 编译失败于缺少 `UpdateOvertimeNightAttendanceBO`；
  - 前端 API 测试失败于缺少 `updateOvertimeNightAttendanceSummary` 导出；
  - 前端工具测试失败于缺少 `buildOvertimeNightAttendanceUpdatePayload` 导出；
  - 前端列源码测试失败于两个实际出勤矩阵尚未接入编辑和保存。
- 后端实现结论：
  - 保存接口按 `employeeId + month` 查询该员工当月所有 `hrm_overtime_night_statistics_detail` 行并批量覆盖三项月度出勤小时；
  - `expectedAttendanceDays/actualAttendanceDays` 继续按小时除以 `8` 向下取整，用于旧字段兼容；
  - 新增 `attendance_manual_adjusted` 后，查询汇总、单人月度明细和显示所有日明细在人工标记为 `1` 时优先读取保存值，避免 `rest_type` 查询刷新逻辑覆盖人工调整；
  - 新增 SQL 脚本 `docs/sql/2026-08-10_overtime_night_attendance_manual_hours.sql` 为 `hr_0001` 至 `hr_0005` 补 `expected_attendance_hours`、`actual_attendance_hours`、`attendance_manual_adjusted`。
- 前端实现结论：
  - `Index.vue` 单人查看弹窗和 `DailyDetailPage.vue` 显示所有页面均把三项出勤列改为小时输入框加保存按钮；
  - 点击任一保存按钮时，会提交当前行的 `expectedAttendanceHours/actualAttendanceHours/accruedAttendanceHours` 三项值，避免月度三项口径局部保存后不一致；
  - 保存成功后刷新当前视图，失败时输入框状态保留给用户修正重试，实际出勤和应计出勤原有计算过程 tooltip 仍保留。
- 最终核验结论：
  - 后端定向测试、后端编译、前端四个加班/夜班相关 Node 测试、前端构建均已 fresh 通过；
  - 目标文件 `git diff --check` 与前后端尾随空白扫描无异常；
  - 2026-08-10 19:07 已在 dev MySQL `153.0.237.98` 执行 `docs/sql/2026-08-10_overtime_night_attendance_manual_hours.sql`，`hr_0001` 至 `hr_0005` 三个新增列均复核存在；生产/线上库若不是该实例仍需单独执行同一脚本。

# Findings: 薪资新建次月防重复与7月恢复方案

## 2026-08-10 Self-Service Recovery Evidence
- 用户当前诉求已经从“只防重复”升级为“可自助恢复误建的后续月份”，因此不能再把恢复留给 DBA 手工 SQL。
- `SalaryMonthRecordServiceNew` 现在提供 `previewSalaryMonthRecovery(year, month)` 和 `recoverSalaryMonth(year, month)`：恢复前必须先看后续月份的员工薪资明细、工资条记录、工资条明细和 `is_send` 状态。
- 只有后续月份全部为空月时，恢复操作才会删除目标月份之后的记录；如果任一后续月份已经有真实核算或工资条发送，后端会返回中文阻断原因并拒绝恢复。
- 恢复后目标月份会按当前数据状态回到 `CREATED` 或 `COMPUTE`，让薪资页重新回到可继续处理的月份。
- 前端 `SalaryManage.vue` 已新增“恢复薪资月份”入口和预览弹窗，用户可先看安全性再执行恢复。

## 2026-08-10 Root Cause Evidence
- 前端入口为 `hr_web/src/views/hrm/salary/salary/SalaryManage.vue#addMony`，当前逻辑 `state.date ? state.date.split("-") : getYYYMM()` 会在月份选择器为空时回退到系统当前年月，而不是当前薪资月记录年月。
- 同页面 `init()` 只设置 `dateText/form.srecordId/checkStatus`，未把 `queryLastSalaryMonthRecord()` 返回的 `year/month` 回填到“新建次月薪资”的月份选择器。
- “新建次月薪资”按钮无 loading/disabled 状态，重复点击会并发/连续调用 `/hrmSalaryMonthRecord/updateCheckStatus`。
- 后端 `SalaryMonthRecordServiceNew#updateCheckStatus(...)` 虽然收到请求 `year/month`，但创建次月时重新按 `orderByDesc(createTime).limit 1` 查询最新薪资记录；在 7 月源月份请求被重复提交时，第一次创建 8 月，后续请求会基于最新 8 月继续创建 9 月。
- `addNextMonthSalary()` 也按最新 `createTime` 取源记录，缺少“次月已存在则跳过创建”的幂等保护。
- `salaryAudit(...)` 总经理通过分支已按当前 `srecordId` 对应记录年月推下一月，但同样缺少次月记录已存在时跳过创建的幂等保护。
- `SalaryMonthRecordService_Bak` 仍被 `SalaryComputeTask` 和 `HrmSalarySlipRecordService` 注入，旧服务中的 `addNextMonthSalary/updateCheckStatus` 也需同步修复，避免备用链路恢复后重现问题。
- 代码修复后，`createNextSalaryMonthRecord(...)` 会先按源年月定位下一月记录，已存在时直接复用，不再重复保存或重复写日志。
- 首次回归失败出在测试双向注入：`TestableSalaryMonthRecordService` 里原本的 `salaryActionRecordService` 字段和父类同名，`ReflectionTestUtils` 只写进了子类字段，导致真实服务依赖仍为空。改成单独的 mock 字段名后，`SalaryMonthRecordNextMonthTest` 通过。
- 现有前端行为确认：`addMony` 成功后会保留当前源月，只有在月选择器为空时才从当前薪资月记录回填月份，不会把用户已选的 7 月覆盖成系统最新月。

# Findings: 审批数据手工添加显示修复

## 2026-08-03 Evidence
- 前端添加弹窗的审批时间控件使用 `type="datetime"` 且未设置展示 `format`，界面默认会展示到秒；本轮只隐藏秒，不改变后端入参秒级兼容。
- 手工添加成功后当前实现只 `state.form.page = 1` 并按原筛选刷新列表；若操作人员在 2026-06 列表中通过行级入口新增 `2025-06-01 18:00-19:00`，列表仍查旧月份，表现为接口成功但新增数据不显示。
- 列表时长展示已由前端 `formatApprovalDurationHoursDays(row)` 统一读取快照 `duration/durationUnit`，因此只要行级添加提交 `duration=2,durationUnit=小时`，列表应显示 `2小时(0.25天)`；若业务只要求“时长列为 2 小时”，这是同一展示口径的小时部分。
- RED 前端验证已确认旧代码缺少分钟级时间展示函数、缺少新增后刷新筛选状态函数，添加弹窗日期控件未隐藏秒。
- 修复后：
  - `formatApprovalDateTimeMinute(...)` 统一把列表开始/结束时间显示到分钟；
  - 添加弹窗两个时间选择器通过 `format="YYYY-MM-DD HH:mm"` 隐藏秒，但继续按 `YYYY-MM-DD HH:mm:ss` 提交；
  - `buildManualAddApprovalListRefreshState(...)` 根据新增 payload 的首个审批时间段切换列表月份，行级添加按当前员工姓名辅助筛选，并清理审批类型/标签/子类型/部门筛选。

# Findings: 审批数据行内添加员工审批

## 2026-08-03 Implementation
- 前端行级添加需要从审批数据列表行拿到后端员工主键；旧列表 VO 只够展示审批信息，不能安全锁定同名员工。
- `QueryAttendanceApprovalPageVO` 与 `HrmAttendanceApprovalMapper.xml` 已补充 `employeeId` 和 `mobile`，SQL 从员工表关联结果返回 `e.employee_id as employeeId`、`e.mobile as mobile`。
- `AddAttendanceApprovalBO` 已补充 `duration` 与 `durationUnit`，用于承接前端手工合计时长。
- `HrmAttendanceApprovalServiceImpl#addManualApproval(...)` 在解析日期范围前先解析手工时长：
  - 空时长继续沿用按起止时间自动计算；
  - 单位支持 `小时`、`分钟`、`天`，空单位按小时；
  - 单日期范围直接使用手工小时；
  - 多日期范围按原始时间段占比分摊手工合计小时，最后一段吸收舍入余量，保证合计闭合。
- 这次后端不按姓名或手机号保存审批数据；姓名 + 手机号只作为前端锁定员工时的识别展示，避免同名员工串人。

# Findings: 同步考勤与审批获取进度条增强

## 2026-08-03 Pre-work
- 后端 `hainan` 文档已有审批数据、同步考勤、薪资/社保进度条等多条历史规则；本轮不能用临时消息替代进度弹窗，错误必须留在进度条内。
- 前端 `hr_web` 需求文档已有“审批数据”页面和“考勤汇总同步考勤”规则；同步成功后要刷新目标月份列表，但旧规则没有要求成功必须用户确认后关闭进度条。
- `hr_web` 续接复核确认已存在 `docs/development.md`，本轮需在现有文档中记录同步考勤/审批获取的进度条实现位置、状态流和测试命令。
- 需继续定位具体入口，确认“员工管理的同步考勤”实际对应的 Vue 页面、API 和后端 controller/service。

## 2026-08-03 Implementation Findings
- 员工管理“同步考勤”入口为前端 `hr_web/src/views/hrm/employee/Index.vue` 调用后端 `attendanceData/sync`，进度轮询为 `attendanceData/getSyncProgress`。
- 后端 `HrmAttendanceDataServiceImpl` 已用 Redis 保留 `RUNNING/SUCCESS/FAILED` 状态、当前步骤、已处理员工、参数和友好中文错误；断点接续只在 `FAILED` 且参数一致时启用，避免成功确认态误触发续传。
- `attendance-sync-progress-utils.js` 是员工同步和审批获取共用的前端进度解析工具；续接复核时发现它会把“失败且 done=true”也强制显示 `100%`，已改为只有非错误完成态才收敛到 `100%`，失败保留后端或当前进度。
- “获取审批数据”入口为前端 `hr_web/src/views/hrm/attendance/approval/Index.vue` 调用后端 `hrmAttendanceApproval/fetchMonthData`，新增 `hrmAttendanceApproval/queryFetchProgress` 轮询进度。
- 审批获取进度由 `HrmAttendanceApprovalServiceImpl` 维护公司级内存状态；成功返回新增条数，失败返回业务中文错误，前端成功/失败都等待操作人员确认关闭。

# Findings: 2026-07 审批数据获取失败排查

## 2026-08-03 Pre-work
- 用户反馈：“获取2026-07月的审批数据失败”。
- 已读取 `docs/requirements.md` 与 `docs/development.md`：审批数据相关业务应优先使用本地审批快照 `tbattendanceapprove`，员工关联需经 `tbattendanceuser.empId -> userId`，并排除 `statisticsStatus=取消至统计`。
- 近期开班/考勤/行政导出改动集中在 2026-07，需先定位具体失败入口，避免直接改审批聚合口径。
- 下一步搜索精确报错文本和审批同步/查询接口，确认失败是后端抛错、前端提示还是第三方钉钉接口返回。
- 搜索结果显示本项目已有独立“审批数据”后端入口 `HrmAttendanceApprovalController`，并有同步服务 `HrmAttendanceApprovalSyncServiceImpl`、查询服务 `HrmAttendanceApprovalServiceImpl`、Mapper 与专项测试。
- 需求文档明确：手工获取审批数据已切换为按目标员工调用 `process/listbyuserid` 解析流程编码，再走 `processinstance/listids + processinstance/get`；不得因 `ddAccount.adminId` 异常影响获取。
- 当前工作区已有大量历史修改和未跟踪文件，本轮需限制在审批数据失败链路内，避免覆盖用户既有改动。
- 后端入口为 `POST /hrmAttendanceApproval/fetchMonthData`，controller 直接返回 `attendanceApprovalService.fetchMonthData(queryBO)`。
- `HrmAttendanceApprovalServiceImpl#fetchMonthData(...)` 负责解析月份、员工范围、审批类型，审批类型为空时返回“请选择审批类型”，随后调用 `approvalSyncService.fetchMonthData(...)`。
- 通用失败文案来源于 `HrmAttendanceApprovalSyncServiceImpl.GENERIC_FETCH_ERROR_MESSAGE = "获取审批数据失败，请稍后重试"`；需继续追踪哪些异常会被翻译成该笼统文案。
- 本地/远端登录需用 `POST /hrsystem/login`；之前用 GET 会被 token 拦截，不是业务失败。
- 使用本地 9080 和 `hbadmin` token 实测单员工吴晓霞 `employeeId=1831601326890434565` 获取 `2026-07`、`approvalTypes=["all"]` 成功，返回 `insertedCount=3`。
- 因此 `2026-07` 月份边界、基础审批权限和单员工抓取链路不是根因；失败更可能出现在“全量/多人抓取”的调用放大或某个员工异常导致整批中断。
- 开发文档曾记录 2026-05-14 审批抓取已加入“任务级流程编码缓存”，但当前源码中 `fetchMonthData(...)` 仍在每个员工循环里调用 `resolveProcessCodes(...)`，未看到 `taskLevelProcessCodes` 缓存实现；这会把全员抓取中的 `process/listbyuserid` 放大为员工数级调用。
- 继续核查 `hr_0003` 员工和映射数据后确认，2026-07 全员抓取失败更贴近“数据质量 + 批量容错不足”的组合问题：
  - 姚玖志、朱玲丽、李凤皇当前员工表 `dingtalk_user_id` 为空，但本地 `tbattendanceuser` 存在可校验复用的钉钉映射；
  - 孟建立、程传祥、谢杰杰、马国华等当前员工的钉钉 `userId` 与另一条在职员工重复，代码不能自动猜测归属；
  - 李明明当前员工的钉钉 `userId` 已在员工表存在，但本地映射里还有指向已删除员工的同 `userId` 历史记录；
  - 单独重抓上述问题员工 `2026-07/all` 均能返回成功，说明整批失败不是月份本身不可抓，而是多人路径中任一员工预解析异常会放大为整批失败，且钉钉频控错误旧文案过于笼统。
- 已补后端行为证据：多人/全员抓取时，员工预解析阶段单人异常应跳过并继续其它员工；员工表缺钉钉 ID 时可在姓名手机号反查失败后复用“经钉钉通讯录校验匹配”的本地 `tbattendanceuser`；钉钉限流需返回明确中文提示。

# Findings: 李凤皇行政导出三项出勤修复

## 2026-07-26 Evidence
- 用户反馈：李凤皇行政体系考勤导出应出勤、实出勤、应计出勤当前为 `184 / 180 / 184`，正确应为 `200 / 196 / 200`。
- 既有统计服务修复已确认李凤皇 `employee_id=1831601326890434571`、工号 `TYNG-142`，员工档案 `affiliation_system=1`、`rest_type=2`；加班/夜班统计查询层应按固定月休刷新为应出勤 `25` 天、实际出勤 `196` 小时、应计出勤 `200` 小时。
- 当前怀疑行政导出仍直接使用旧明细聚合值：`expected_attendance_days=23`、`actual_attendance_days=22/22.5`、`accrued_attendance_hours=184`，没有复用统计查询层对 `restType=2` 历史正数明细的刷新逻辑。
- RED 测试已证实：即使 mock 的加班/夜班统计查询结果返回 `expectedAttendanceDays=25`、`actualAttendanceHours=196`、`accruedAttendanceHours=200`，当前行政导出仍输出旧明细 `184` 小时应出勤。
- 修复后行政导出 E/F/G 三列优先使用加班/夜班统计查询当前值；旧明细仅作为行基准和查询缺失时的兜底。
- 只读核查 `hr_0003 / 2026-06`：行政体系且 `rest_type=2`、旧明细仍为 `23天/184小时` 的员工共 4 人。按当前固定月休配置 `production_monthly_rest_days=4`，2026-06 应出勤为 `25天/200小时`。
- 这 4 人旧导出与当前统计口径差异：
  - 李凤皇 `TYNG-142`：旧 `184 / 180 / 184`，当前统计口径 `200 / 196 / 200`。
  - 杨太琴 `TYNG-436`：旧 `184 / 176 / 184`，当前估算统计口径 `200 / 192 / 200`。
  - 潘红琼 `TYNG-437`：旧 `184 / 152 / 184`，当前估算统计口径 `200 / 168 / 200`。
  - 王芳 `TYNG-450`：旧 `184 / 168 / 184`，当前估算统计口径 `200 / 184 / 200`。

# Findings: 行政体系考勤导出出差列修复

## 2026-07-25 Evidence
- 用户反馈：人工统计中所有员工出差均为 `0`，当前行政体系考勤导出与人工统计不同。
- 代码根因定位：`HrmProduceAttendanceServiceImpl#resolveAdministrativeAttendanceApprovalType(...)` 当前把 `bizType=2`、文本包含 `出差`、文本包含 `外出` 的审批统一返回 `"出差"`，随后 `mergeAdministrativeAttendanceApprovalHours(...)` 将小时除以 8 写入 L 列出差天数。
- 开发库 `hr_0003 / 2026-06` 只读核查，当前代码会导出非零出差的行政体系员工共 6 人：
  - 罗毅龙：当前口径 `18.46` 天，其中严格 `出差` `0.96` 天，`外出` 审批 2 条。
  - 李明明：当前口径 `9.55` 天，严格 `出差` `9.55` 天。
  - 张明：当前口径 `2.46` 天，全部来自 `外出` 审批。
  - 赵聪：当前口径 `2.32` 天，全部来自 `外出` 审批。
  - 闫倩：当前口径 `0.88` 天，全部来自 `外出` 审批。
  - 王琪：当前口径 `0.32` 天，全部来自 `外出` 审批。
- 对照业务提供的原始模板文件，历史 L 列曾有张明 `1`；但本轮用户最新确认“所有员工出差为 0”，以后端导出口径应按最新人工统计规则收敛为 0。

# Findings: 行政体系考勤导出实出勤小时修复

## 2026-07-25 Evidence
- 用户反馈吴晓霞行政体系考勤导出“实出勤”不是已入库的 `185.5`，而是导出为 `184.x`。
- 代码根因已定位在 `HrmProduceAttendanceServiceImpl.AdministrativeAttendanceAccumulator#toRow(...)`：当前 F 列实出勤小时由 `actualAttendanceDays * 8` 得出。
- 加班/夜班统计页面的实出勤小时不是持久化的 `actual_attendance_days * 8`，而是查询服务按月重算：`expectedAttendanceDays * 8 + 审批加班小时 - 事假/病假/调休/年假扣减小时`。
- 吴晓霞 `hr_0003 / 2026-06` 的源数据闭合：应出勤 `23天=184小时`，审批加班 `2小时`，调休扣减 `0.5小时`，统计页面实出勤应为 `185.5小时`；旧导出使用 `23 * 8 = 184`，少 `1.5小时`。
- 初步批量 SQL 复核 `hr_0003 / 2026-06 / 行政体系 / expected_days > 0`，同类差异共 `19` 人，需在修复后反馈完整名单。
- 本轮重新只读核查 `hr_0003 / 2026-06`：行政体系且 `expected_attendance_days > 0` 的员工共 `38` 人，其中 `19` 人旧导出值 `actual_attendance_days * 8` 与统计实际小时口径不一致。
- 受影响名单按差异绝对值排序：
  - 何晓光 `TYNG-410`：旧导出 `176`，统计口径 `183.00`，差 `7.00`。
  - 马春霞 `TYNG-231`：旧导出 `176`，统计口径 `183.00`，差 `7.00`。
  - 黎冬霜 `TYNG-134`：旧导出 `168`，统计口径 `174.50`，差 `6.50`。
  - 喻艳 `TYNG-108`：旧导出 `176`，统计口径 `181.00`，差 `5.00`。
  - 张雪梅 `TYNG-371`：旧导出 `160`，统计口径 `165.00`，差 `5.00`。
  - 李红盼 `TYNG-463`：旧导出 `176`，统计口径 `181.00`，差 `5.00`。
  - 王琪 `TYNG-461`：旧导出 `176`，统计口径 `180.50`，差 `4.50`。
  - 李凤皇 `TYNG-142`：旧导出 `176`，统计口径 `180.00`，差 `4.00`。
  - 林艳 `TYNG-373`：旧导出 `192`，统计口径 `196.00`，差 `4.00`。
  - 瞿顺清 `TYNG-286`：旧导出 `64`，统计口径 `68.00`，差 `4.00`。
  - 闫倩 `TYNG-462`：旧导出 `176`，统计口径 `180.00`，差 `4.00`。
  - 方小荣 `TYNG-387`：旧导出 `176`，统计口径 `179.50`，差 `3.50`。
  - 李学飞 `TYNG-1005`：旧导出 `184`，统计口径 `187.00`，差 `3.00`。
  - 高文利 `TYNG-358`：旧导出 `144`，统计口径 `147.00`，差 `3.00`。
  - 王洪平 `TYNG-064`：旧导出 `168`，统计口径 `170.50`，差 `2.50`。
  - 李国文 `TYNG-458`：旧导出 `128`，统计口径 `130.00`，差 `2.00`。
  - 田沂雨 `TYNG-425`：旧导出 `96`，统计口径 `98.00`，差 `2.00`。
  - 胡勇星 `TYNG-408`：旧导出 `104`，统计口径 `105.78`，差 `1.78`。
  - 吴晓霞 `TYNG-110`：旧导出 `184`，统计口径 `185.50`，差 `1.50`。

# Findings: 考勤汇总下载行政体系考勤

## 2026-07-25 追加：下载日期选择
- 用户补充要求：下载前需要能选择日期，下载数据必须基于选择的日期。
- 现有实现直接读取考勤汇总筛选 `form.yearMonth`，为空时默认当前年月；这不满足“下载时明确选择日期”的交互要求。
- 后端导出接口已按 `QueryMonthAttendanceBO.year/month` 查询加班/夜班统计、审批快照和钉钉报表聚合数据；只要前端把所选日期转换为对应年月提交，即可让数据基于所选日期所在月份，无需新增后端查询字段。
- 设计选择：在“下载行政体系考勤”按钮后打开一个独立弹窗，弹窗内用日期选择器选择导出日期；确认后按日期所在月份拆成 `year/month`，并强制 `department=1`。
- 实现结果：`Upload.vue` 新增 `administrativeAttendanceDateDialogVisible/administrativeAttendanceDate`；按钮点击只打开弹窗，确认下载时校验日期并调用 `downloadAdministrativeAttendanceExcel(state.administrativeAttendanceDate)`。
- `resolveAttendanceSummaryExportMonth(...)` 不再为空值默认当前年月；只有传入 `YYYY-MM`、`YYYY-MM-DD` 或有效 `Date` 时才返回导出月份。

## 2026-07-25 Resume
- 当前后端缺口：`HrmProduceAttendanceController` 仅有导入、列表、同步和单元格保存接口；`IHrmProduceAttendanceService` 尚无行政体系考勤下载方法。
- 当前前端缺口：`Upload.vue` 页头只有“同步考勤”按钮，`src/api/hrm/attendance/upload.js` 尚无行政体系考勤 Blob 下载 API。
- 导出实现需新增后端模板资源 `src/main/resources/export/行政体系考勤汇总表.xlsx`；源模板存在于用户桌面路径，首张 sheet 的主体列为 A-S。
- 异常/审批数据来源拆分：事假、病假、调休、年假、出差、加班按员工 `employeeId -> tbattendanceuser.userId -> tbattendanceapprove.userId` 聚合；旷工、迟到、早退、上班缺卡、下班缺卡按已同步的 `hrm_attendance_report_data` 聚合。
- 实现结果：后端新增 `downloadAdministrativeAttendance` 接口，导出时移除模板第二个 sheet、清理 S 后单元格并重建 A-S 表头；前端新增“下载行政体系考勤”按钮，导出参数强制行政体系 `department=1`，年月未选时使用当前年月。
- 导出表最终列序为：A 序号、B 姓名、C 部门、D 月天数、E 应出勤小时、F 实出勤小时、G 应计出勤小时、H 事假、I 病假、J 调休、K 年假、L 出差天数、M 旷工、N 迟到、O 早退、P 上班缺卡、Q 下班缺卡、R 总缺卡、S 加班。

## 2026-07-25 Pre-work
- 已读取后端 `docs/requirements.md`、`docs/development.md`，并按关键词复核考勤汇总、审批数据、加班/夜班统计、下载导出相关段落。
- 已读取前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认考勤汇总页位于 `src/views/hrm/attendance/upload/Upload.vue`，接口文件为 `src/api/hrm/attendance/upload.js`。
- 后端现有考勤汇总入口为 `HrmProduceAttendanceController`，列表接口 `/hrmProduceAttendance/queryMonthAttendanceList`，同步接口 `/hrmProduceAttendance/syncFromOvertimeNightStatistics`。
- 现有 `department=1/2` 表示考勤汇总行政/生产体系类型，来源于员工档案 `hrm_employee.affiliation_system`；真实组织部门需读取员工 `dept_id -> hrm_dept.name`。
- 加班/夜班统计明细表 `hrm_overtime_night_statistics_detail` 已保存 `expectedAttendanceDays/actualAttendanceDays/accruedAttendanceHours`；可用于导出月天数、应出勤小时、实出勤小时、应计出勤小时。
- 审批数据本地快照表为 `tbattendanceapprove`，字段包含 `tagName/subType/bizType/beginTime/endTime/duration/durationUnit/workDate/statisticsStatus/userId`；查询和导出不得临时调用钉钉接口。
- 员工到审批数据的关联需经 `tbattendanceuser.empId -> userId`，避免同名员工串数据。
- 业务模板 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx` 第一张 sheet 为 `6月`，当前 A-S 是主体列，T-W 是剩假类尾部列。
- 模板现有主体列：A 序号、B 姓名、C 月天数、D 应出勤、E 实出勤、F 应计出勤、G 公休/假期、H-J 请假（事假/病假/调休）、K 年假、L 出差、M 旷工、N 迟到、O 早退、P 上班缺卡、Q 下班缺卡、R 总缺卡、S 加班。
- 本轮导出目标将新增 C=部门，并删除原 G=公休/假期，让目标 H-S 继续对应事假、病假、调休、年假、出差、旷工、迟到、早退、上班缺卡、下班缺卡、总缺卡、加班；目标只输出到 S 列。

# Findings: 排班管理导入连班与矩阵标识

## 2026-07-25 Pre-work
- 用户要求拆分为后端与前端两部分：
  - 后端导入：上传排班文档已有“是否连班”列；单元格有内容时按导入值，单元格为空时回退员工档案 `is_continuous_shift`；
  - 后端模板：下载排班模板需新增“电话”列；
  - 后端匹配：上传行电话有内容时按“姓名 + 电话”作为唯一键；电话为空时表示无需电话区分，直接按姓名匹配；
  - 前端展示：排班管理矩阵单元格内需显示连班“连”，休息和调休也要直接显示。
- 已读取后端 `docs/requirements.md`、`docs/development.md`：现有排班核心在 `WorkPlanServiceImpl`，本地自定义排班 `customContinuousShift` 已存在，且只有自定义白班允许连班。
- 已读取前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`：排班管理/添加排班页面位于 `src/views/hrm/attendance/records`，上传排班入口已迁到排班管理并复用添加排班弹窗导入预览。
- 初步设计：
  - 导入解析时保留“是否连班”单元格是否为空的信息，只有为空才按员工档案自动带出；
  - 模板列名使用“电话”，导入解析兼容“电话”和历史“手机号”；
  - 员工匹配在电话有内容时校验姓名 + 电话；电话为空时按姓名匹配，避免强制用户为无重名风险的导入行补电话；
  - 矩阵显示在前端工具函数 `getWorkPlanMatrixCellView(...)` 统一处理，避免散落到 Vue 模板。

## 2026-07-25 Implementation
- 后端 `tbplanlist` 增加 transient 控制位 `customContinuousShiftExplicit`，用于区分 Excel 是否显式填写“是否连班”；该字段只参与导入到提交链路，不落库。
- 后端平铺导入模板列调整为 `排班日期/员工/电话/排班类型/排班时间/白班/夜班/是否连班/备注`，解析兼容旧“手机号”列。
- 后端横向 `workplan.xlsx/xls` 解析支持“姓名、电话”两列后再进入每日 5 列日期组；旧无电话模板仍可解析，未提供电话时按员工姓名匹配。
- 后端员工匹配规则改为：电话有内容时必须匹配该姓名下唯一员工，否则返回“员工姓名与电话不匹配”；电话为空时直接使用姓名匹配，不再因为系统存在同名候选报错。
- “是否连班”单元格有内容时按 Excel 值解析，“是/true/1/连/连班”视为 `true`，其它内容视为 `false`；单元格为空时按员工 `is_continuous_shift` 回退。
- `WorkPlanServiceImpl#buildLocalCustomAssignments(...)` 只有在导入未显式填写连班时才按员工字段覆盖，保证 Excel 中显式“否”不会被员工默认“是”覆盖。
- `WorkPlanListController#downloadImportTemplate(...)` 运行时重建模板前两行，第二行写入“姓名、电话”，日期分组从电话列之后开始。
- 前端 `getWorkPlanMatrixCellView(...)` 将白班连班从主文本拆为 `subText: 连`；休息和调休继续作为单元格主文本直接显示。
- 前端导入预览到提交 payload 透传 `customContinuousShiftExplicit`，只在自定义排班且 Excel 显式填写“是否连班”时提交。

## 2026-07-25 Verification
- 后端定向：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest,HrmEmployeeMapperSqlTest test` 通过 60 个测试。
- 后端编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。

## 2026-07-25 排班上传匹配与模板表头优化追加
- 用户最新反馈修正了上一版“同名必须填电话”的规则：上传行电话单元格有内容时按姓名 + 电话匹配；电话为空时表示没有重名员工，不需要姓名 + 电话，直接用姓名匹配。
- 修复前 `WorkPlanServiceImpl#resolveImportEmployee` 对 `employees.size() > 1 && mobile blank` 会报“存在同名员工，请填写电话区分”，与最新要求冲突。
- 修复前 `WorkPlanListController#fillTemplateDateRow` 每个日期使用 5 个子列，但日期只写在第一个子列上，没有合并覆盖该日期组，也没有居中和循环底色，导致 7 月 31 日后面的同组子列看起来像无日期列。
- 已调整 `resolveImportEmployee(...)`：同名候选且电话为空时返回姓名查询结果中的第一名员工，符合“电话为空直接按姓名匹配”的上传约定；同一员工同一天重复仍按既有重复行规则阻止。
- 已调整 `WorkPlanListController#fillTemplateDateRow(...)`：每个有效日期组在第一行合并 5 列，日期与下方 5 个字段表头水平/垂直居中，同一日期组同色，相邻日期组两色循环；当月最后一天之后的日期行和字段表头整组清空。
- 已更新后端/前端 `docs/requirements.md` 与 `docs/development.md`，删除“系统同名必须补电话”的旧口径。
- 追加验证：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 54 个测试；`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 前端定向：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

# Findings: 排班员工连班自动带出

## 2026-07-25 Pre-work
- 本轮任务：添加排班功能和排班管理修改员工排班时，按员工表维护的“是否连班”字段自动设置排班记录“是否连班”。
- 已初始化/复用 `task_plan.md`、`findings.md`、`progress.md`；历史计划文件很长，本轮记录追加在文件顶部。
- 恢复脚本首次误用 `sh` 执行失败，已改用 `python3` 重新执行且无历史上下文输出。
- 已读取 `docs/requirements.md` 与 `docs/development.md` 的排班相关段落：
  - 排班记录 `tbplanlist.custom_continuous_shift/customContinuousShift` 已存在；
  - 自定义班次事实表 `hrm_workplan_custom_shift.continuous_shift` 已存在；
  - 既有规则要求只有白班自定义排班可保留连班，夜班、休息、标准班次不得保存连班。
- 代码扫描确认批量保存 `/workPlan/saveAll` 和排班管理单日修改 `/workPlan/saveEmployeeDayCustomShift` 最终都进入 `WorkPlanServiceImpl` 的本地自定义排班保存链路。
- 只读查询开发库 `information_schema`：`hr_0001` 至 `hr_0005.hrm_employee` 均已有 `is_continuous_shift`，注释为“是否连班：1是，2否”。
- 当前代码缺口：`HrmEmployee` JPA 模型、MyBatis Plus 员工 PO 和员工列表 Mapper 尚未接入 `isContinuousShift`；排班保存逻辑只使用前端传入的 `customContinuousShift`，不会按员工表自动覆盖。

## 2026-07-25 Implementation
- 已接入员工表固定字段 `isContinuousShift/is_continuous_shift`，并让员工列表 SQL 返回该字段。
- 批量添加排班 `buildLocalCustomAssignments(...)` 现在按 `userId -> empId -> hrm_employee.is_continuous_shift` 自动覆盖 `customContinuousShift`：
  - `1` 转为 `true`；
  - `2` 或其它非空值转为 `false`；
  - 字段为空或映射缺失时保留原入参。
- 排班管理单日修改 `saveEmployeeDayShift(...)` 直接按 `employeeId` 读取员工连班字段，再进入原本地自定义排班保存流程。
- 同一行多员工若连班设置不同，会按员工拆成不同 assignment；相同设置的员工复用同一个本地自定义班次解析结果，避免重复创建 `hrm_workplan_custom_shift`。
- 新增幂等脚本 `docs/sql/2026-07-25_hrm_employee_is_continuous_shift.sql`，用于补齐其它环境 `hrm_employee.is_continuous_shift`。
- 迁移脚本将字段放在 `full_attendance` 后，避免依赖 `rest_type` 脚本先执行；业务逻辑只依赖 `is_continuous_shift` 的 `1/2/NULL` 取值。
- 验证：`mvn -Dtest=WorkPlanServiceImplTest,HrmEmployeeMapperSqlTest test` 通过 42 个测试。
- 最终验证：`mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest,HrmEmployeeMapperSqlTest test` 通过 56 个测试；`mvn -DskipTests compile` 通过；本轮目标文件 `git diff --check` 无输出。

# Findings: 考勤汇总高级筛选部门查询

## 2026-07-24 Pre-work
- 当前考勤汇总列表接口 `/hrmProduceAttendance/queryMonthAttendanceList` 入参 `QueryMonthAttendanceBO` 只有 `year/month/employeeName/department`。
- `department` 在本业务中代表考勤汇总行政/生产类型，来源于员工档案 `affiliation_system`，前端已把它展示为“所属体系”，不能复用为组织部门查询。
- `HrmProduceAttendanceMapper.xml#queryProduceAttendanceList` 已 `left join hrm_employee e on p.employee_id = e.employee_id`，可直接按 `e.dept_id` 支持部门查询。
- 设计方向：新增 `deptIds: List<Long>` 入参，SQL 使用 `e.dept_id in (...)`；保留 `p.department = #{data.department}` 原筛选不变。

## 2026-07-24 Implementation
- `QueryMonthAttendanceBO` 已新增 `deptIds: List<Long>`。
- `HrmProduceAttendanceMapper.xml#queryProduceAttendanceList` 在 `deptIds` 非空时追加 `AND e.dept_id in (...)`，支持多部门组合查询。
- 既有 `p.department` 行政/生产体系过滤保留，可与 `deptIds` 同时组合。

# Findings: 个税附加导入模板下载与年月导入校验

## 2026-07-23 Pre-work
- 已读取 `hainan/docs/requirements.md`、`hainan/docs/development.md`，确认个税累计、附加累计、年度附加扣除导入都已统一要求姓名 + 手机号匹配，并且失败必须返回明确错误。
- 已读取 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认薪资相关页面位于 `src/views/hrm/salary/*`，附加累计已改为按 `YYYY-MM` 查询和导入。
- 现有页面：
  - 个税累计：`hr_web/src/views/hrm/salary/tax/Tax.vue`，已有月份选择器 `form.month`，导入直接提交 `dates: state.form.month`，当前缺未选年月拦截。
  - 附加累计：`hr_web/src/views/hrm/salary/addition/Addition.vue`，已有 `form.yearMonth`、`splitYearMonth(...)` 和“请先选择年-月”导入拦截。
  - 年度附加扣除：`hr_web/src/views/hrm/salary/additionDeduction/Index.vue`，当前只有导入按钮，没有下载模板按钮。
- 现有后端 controller：
  - `HrmPersonalIncomeTaxController` 前缀 `/hrmPersonalIncomeTax`；
  - `HrmAdditionalController` 前缀 `/hrmAdditional`；
  - `HrmEmployeeAdditionalController` 前缀 `/hrmEmployeeAdditional`。
- 既有固定模板下载模式可复用员工/合同/加班夜班统计下载接口：从 `src/main/resources/export` 读取 classpath 资源，设置 Excel content type、`Content-Disposition`、`fileDownload=true` Cookie，并输出 Blob。
- 用户提供的三份模板文件已在 `/Users/jiangyongming/Desktop/导入模版` 下确认存在。

## 2026-07-23 Implementation
- 后端新增 `ExcelTemplateDownloadUtils`，三类模板下载接口共用 classpath 资源读取、响应头设置和 404 处理。
- 三份模板已复制到后端 `src/main/resources/export`，Maven resources 阶段显示资源数从 92 增至 95，说明会进入 classpath。
- 前端新增 `template-download-utils.js` 统一处理 Blob 下载；三个页面分别维护独立下载 loading 状态。
- 个税累计导入已新增空月份拦截；附加累计既有空年-月拦截保留，同时上传按钮在未选年月时置 disabled，防止常规点击提交。

# Findings: 李凤皇 2026-06 出勤统计少 16 小时排查修复

## 2026-07-23 Pre-work
- 已读取 `docs/requirements.md` 与 `docs/development.md`，确认加班/夜班统计是当前薪资和考勤汇总的出勤源口径。
- 目标现象：李凤皇 2026-06 统计结果当前为 `184 / 180 / 184` 小时，业务确认正确应为 `200 / 196 / 200` 小时。
- 初步差异：三项均少 `16` 小时，折算为 `2` 天；当前 `184` 对应 `23` 天，正确 `200` 对应 `25` 天。
- 下一步先查统计服务的应出勤、实际出勤、应计出勤计算路径，再查员工档案和目标月份落库明细。

## 2026-07-23 Evidence
- 代码路径：`HrmOvertimeNightStatisticsServiceImpl#resolveExpectedAttendanceDays(...)` 当前按 `affiliationSystem` 选择应出勤公式，`affiliation_system=1` 走单双休月历，`affiliation_system=2` 才走生产固定月休公式。
- 查询路径：`queryPageList(...)` 先按员工月度明细构建汇总，`resolveExpectedAttendanceDaysForQuery(...)` 只在历史明细应出勤为空或 `0` 时才重算；历史明细若保存了正数旧值 `23`，查询会继续使用该值。
- 只读实库验证 `hr_0003`：李凤皇 `employee_id=1831601326890434571 / TYNG-142 / mobile=17573048926`，当前员工档案为 `affiliation_system=1`、`rest_type=2`、`is_del=0`。
- 只读实库验证 `hr_0003.hrm_overtime_night_statistics_detail`：李凤皇 2026-06 明细 `25` 行，`expected_attendance_days=23`、`actual_attendance_days=22`、`accrued_attendance_hours=184.00`，最大更新时间 `2026-07-23 01:27:08`。
- 最新基本工资设置 `production_monthly_rest_days=4`；按 2026-06 固定月休口径应为 `30 - 4 - 1个工作日法定休 = 25天 = 200小时`。
- 根因假设：应出勤班制应优先由员工 `rest_type` 决定，`rest_type=2` 固定月休员工应按固定月休公式计算；`affiliation_system` 继续用于行政/生产体系和加班/夜班资格判断。当前代码仍用 `affiliation_system` 决定应出勤公式，导致李凤皇这类 `affiliation_system=1 + rest_type=2` 员工被误算成单双休 23 天。

## 2026-07-23 Fix
- 李凤皇钉钉映射：`tbattendanceuser.empId=1831601326890434571` 对应 `userId=02191921034726085466`。
- 2026-06 参与扣减审批中只有一条请假/调休：`2026-06-17 14:00:00 -> 18:00:00`，时长字段为空但起止时间折算为 `4` 小时；补卡审批不参与实际出勤扣减。
- 正确复算：固定月休应出勤 `25天 * 8 = 200小时`；实际出勤 `200 - 调休4 = 196小时`；应计出勤 `196 + 调休4 = 200小时`。
- 代码修复：
  - `resolveExpectedAttendanceDays(...)` 改为优先使用 `restType` 判断应出勤班制：`2` 走固定月休，`1` 走单双休；`restType` 为空时才按 `affiliationSystem` 兼容历史数据。
  - 加班/夜班资格 `canCountOvertimeNight(...)` 未改变，仍要求生产体系且固定月休。
  - 查询层仅在员工显式维护了 `restType` 且它与所属体系旧分流冲突时刷新正数历史应出勤，避免普通历史明细在缺少完整节假日信息时被误覆盖。
- 回归测试覆盖李凤皇式旧数据：历史明细仍是 `23/22/184`，员工为 `affiliation_system=1/rest_type=2`，查询应输出 `expectedAttendanceDays=25`、`actualAttendanceHours=196.00`、`accruedAttendanceHours=200.00`，且加班小时仍为 `0`。

# Findings: 加班夜班开始统计范围选择前端合并

## 2026-07-23
- 本轮未修改 `hainan` 后端统计服务代码。
- 后端既有接口契约继续保留：
  - `/hrmOvertimeNightStatistics/startStatistics` 处理全员开始统计；
  - `/hrmOvertimeNightStatistics/startStatisticsForEmployee` 处理显式 `employeeIds + month` 的员工范围重算。
- 前端 `hr_web` 已将独立“单人统计”按钮合并进“开始统计”弹窗：空范围调用全员接口，显式范围调用员工范围接口。
- 因此后端统计业务逻辑、审批优先级、应出勤/实际出勤/应计出勤公式均不因本次前端入口调整改变。

# Findings: 基本工资设置保存后同步薪资档案基本工资

## 2026-07-22 Pre-work
- 已读取 `docs/requirements.md` 与 `docs/development.md`，确认当前薪资模块以工资项编码驱动薪资档案和核算。
- 用户本次要求不是员工级全勤金额，而是“基本工资金额设置”保存后，把设置里的基本工资同步到员工薪资档案的基本工资项。
- 初步假设：薪资档案基本工资项为 `10101 / 基本工资`，需通过代码扫描确认 `hrm_salary_archives_option` 的实际更新入口和员工过滤口径。

## 2026-07-22 Code Scan
- 保存入口为 `HrmSalaryBasicService#saveSalaryBasic(...)`，当前只保存/更新 `hrm_salary_basic`，未触达员工薪资档案。
- 薪资档案明细实体为 `HrmSalaryArchivesOption`，表名 `hrm_salary_archives_option`，字段包含 `employeeId/isPro/code/name/value`。
- `HrmSalaryArchivesService#setFixSalaryRecord(...)` 保存两套档案：`isPro=1` 为试用期工资，`isPro=0` 为正式工资；两套工资项都可能包含 `10101 / 基本工资`。
- 薪资档案列表和员工档案查询均过滤 `hrm_employee.is_del=0`，本次同步也应沿用未删除员工范围。

## 2026-07-22 RED
- `mvn -Dtest=HrmSalaryBasicServiceTest test` 失败 2 个断言：
  - `saveSalaryBasic_shouldSyncBasicSalaryAmountToEmployeeArchives`：保存设置后未同步 `salaryBasic`；
  - `salaryBasicServiceSource_shouldSyncBasicSalaryArchivesForNotDeletedEmployees`：源码缺少薪资档案同步实现。

## 2026-07-22 Implementation Findings
- `HrmSalaryBasicService#saveSalaryBasic(...)` 保存或更新基本工资设置后，调用 `syncBasicSalaryToEmployeeArchives(...)` 同步本次 `salaryBasic`。
- 同步先查询 `hrm_employee.is_del=0` 的员工 ID，再批量更新这些员工薪资档案明细中 `code=10101` 且 `is_pro in (0,1)` 的 `value`。
- 本次只更新已有 `hrm_salary_archives_option` 行；没有完整薪资档案或缺少 `10101` 明细的员工不会被自动补建，避免产生不完整档案。
- `salaryBasic` 为空时不执行同步，避免把空值写入员工薪资档案。
- `mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 10 个测试；`mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest test` 通过 75 个测试。

## 2026-07-22 Review Follow-up
- 只读代码审查指出：先全量查询未删除员工 ID 再拼单个 `IN (...)` 更新，在员工量大时有 SQL 参数过多和内存风险。
- 已改为 `EXISTS_NOT_DELETED_EMPLOYEE_SQL`，在更新 `hrm_salary_archives_option` 时由数据库通过 `exists` 子查询限定 `hrm_employee.is_del=0`，不再拉取员工 ID 列表。
- 已补测试约束：
  - `salaryBasic` 为空时不触发薪资档案同步；
  - 源码不得再出现 `queryNotDeletedEmployeeIdsForBasicSalarySync` 或 `selectList(new LambdaQueryWrapper...)` 的全量员工 ID 查询路径；
  - 同步仍必须限定 `10101` 和 `is_pro in (0,1)`。
- `mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 10 个测试。

# Findings: 员工级全勤金额设置

## 2026-07-22 Pre-work
- 已读取后端 `docs/requirements.md`、`docs/development.md`，确认当前基本工资金额设置已维护普通员工全勤金额、领导全勤金额、生产月休天数和社保固定金额。
- 当前薪资核算已有“不得硬编码工资项金额”的近期规则，全勤金额当前应来自基本工资设置或计薪员工查询结果；本次要求进一步改为员工表保存员工级金额。
- 需求初步拆分：
  - 后端：员工表新增员工级普通/领导全勤金额字段；新增批量设置接口；薪资核算读取员工表字段，空值才回退基本工资设置/默认值。
  - 前端：基本工资金额设置页两个全勤金额输入后新增“设置”按钮；点击后弹出按人员/按部门选择范围；右侧为空表示全员更新。
  - SQL：若字段不存在，需要为各租户库补充迁移脚本并执行。

## 2026-07-22 Code Scan
- `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 当前 `fullMoney` 仍由最新 `hrm_salary_basic.ordinary_full_attendance_amount/leader_full_attendance_amount` 计算，缺失分别硬编码回退 `100/500`。
- `SalaryMonthRecordServiceNew#fillAttendanceDataForEmployee(...)` 正常全勤路径读取 map 中 `fullMoney` 写入 `40102`，但“市场入职默认有全勤”分支仍直接调用 `HrmSalaryBasicDefaults.leaderFullAttendanceAmount(...)` 和 `ordinaryFullAttendanceAmount(...)`。
- `HrmSalaryBasicController` 当前只有保存/查询基本工资设置接口；`HrmSalaryBasicService` 当前只维护 `hrm_salary_basic`，未写员工表金额。
- 员工 MyBatis PO 与 JPA 模型已有 `restType` 最近新增字段，适合按同样方式新增两个 BigDecimal 员工级全勤金额字段。
- 前端基本工资金额设置页位于 `hr_web/src/views/manage/salary/Index.vue`，已有两个全勤金额输入和默认值；现有 `AloneComputeDialog.vue`/`salary-compute-scope-utils.js` 可作为按人员/按部门范围选择交互参考。

## 2026-07-22 Implementation Findings
- 后端已新增 `UpdateEmployeeFullAttendanceAmountDto` 与 `/hrmSalaryBasic/updateEmployeeFullAttendanceAmount`，服务层按 `amountType=ordinary/leader` 写入员工表对应字段。
- `employeeIds` 为空时，后端只按 `is_del=0` 更新全体未删除员工；非空时去重并追加 `employee_id in (...)`。
- `queryPaySalaryEmployeeList` 的 `fullMoney` 已改为：
  - 领导岗位：`coalesce(a.leader_full_attendance_amount, salary_basic_setting.leader_full_attendance_amount, 500)`；
  - 普通岗位：`coalesce(a.ordinary_full_attendance_amount, salary_basic_setting.ordinary_full_attendance_amount, 100)`。
- `SalaryMonthRecordServiceNew` 中全勤奖 `40102` 写入均使用 `fullMoney`，生产源码扫描不再命中绕过员工表的 `HrmSalaryBasicDefaults.*FullAttendanceAmount(batchData.salaryBasic)`。
- SQL 已在 dev MySQL 执行并复跑幂等；`hr_0001` 至 `hr_0005` 的两个新增字段均为 `decimal(10,2)`。

## 2026-07-22 Review Follow-up
- 代码审查发现前端员工级全勤金额设置弹窗复用了 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList`，会把未进入最新计薪范围或没有正数薪资档案的未删除员工排除；该弹窗语义是员工表级设置，已改为 `/hrmEmployee/queryAllEmployeeList`。
- 为保留同名员工识别能力，`HrmEmployeeServiceImpl#queryAllEmployeeList(...)` 现在选择并返回 `mobile`，`transferSimpleEmp(...)` 会写入 `SimpleHrmEmployeeVO.mobile`。
- 显式选择人员/部门后更新 `0` 行已改为业务失败：后端抛出“所选员工不存在或已删除，未更新任何员工”，前端收到 `data=0` 时也会显示“未更新任何员工，请重新选择人员”而不是成功。
- 旧薪资服务 `SalaryMonthRecordService_Bak` 仍可能被工资条或旧任务引用，已把 `fullMoney` 从 `Long` 强转改为复用新服务的 `BigDecimal/Number/String` 兼容解析。

# Findings: 生产体系固定月休4天才统计加班夜班

## 2026-07-22 Pre-work
- 已读取 `docs/requirements.md` 与 `docs/development.md` 相关章节。
- 当前文档与代码旧口径：
  - `hainan` 现有生效代码尚未接入员工 `hrm_employee.rest_type/restType`。
  - `180101 / 加班费` 旧逻辑只判断 `affiliation_system=2` 生产体系。
  - `180102 / 夜班补贴` 旧逻辑不看所属体系或休息制度，只读取考勤汇总 `night_shift/night_subsidy`。
  - 考勤汇总同步旧逻辑会按加班/夜班统计写入 `work_over_time/overtime_pay/night_shift/night_subsidy`。
- 新业务规则：
  - 只有员工所属体系为生产且休息制度为固定月休 4 天，才统计加班时间、加班费、夜班次数、夜班补贴。
  - 行政体系、未维护所属体系、非固定月休休息制度、未维护休息制度的员工均不应统计或生成这些加班/夜班值。
- 待核准：
  - `rest_type` 的数据库脚本、员工新增/编辑/导入导出契约是否已在本仓库或同级前端存在。
  - 加班/夜班统计明细是否对非目标员工保存 0 值明细，还是完全跳过明细；初步倾向保存 0 值，避免薪资应出勤依赖统计明细时缺失。

## 2026-07-22 Code Scan
- 同级 `watch` 项目已经实现 `hrm_employee.rest_type`，取值 `1=行政单双休`、`2=固定月休4天`，且其历史口径允许“行政体系 + 固定月休4天”按月休规则归类。
- 本次 `hainan` 用户要求比 `watch` 更收紧：必须同时满足 `affiliation_system=2` 和 `rest_type=2` 才统计加班/夜班，不满足任一条件都不统计加班时间、加班费、夜班次数、夜班补贴。
- `hainan` 当前冲突点：
  - `HrmEmployee` JPA 模型和 MyBatis Plus 员工 PO 没有 `restType` 字段。
  - `HrmSalaryMonthEmpRecordMapper.xml` 计薪员工查询/薪资列表没有返回 `restType`，`isProduceDept` 只由 `affiliation_system=2` 派生。
  - `HrmEmployeeMapper.xml#queryHasOverTimePayEmpList` 只筛选 `affiliation_system = 2`。
  - `SalaryMonthRecordServiceNew#getYeBanAndJiaBan(...)` 只按 `isProduce` 防护加班费，夜班补贴无防护；`resolveExportOvertimePay(...)` 只按所属体系防护加班工资导出。
  - `HrmOvertimeNightStatisticsServiceImpl` 计算和汇总日明细时直接保存/求和 `overtimeHours/nightShiftCount`。
  - `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 直接把统计结果同步到考勤汇总的 `workOverTime/overtimePay/nightShift/nightSubsidy`。

## 2026-07-22 Implementation Findings
- 已接入 `hrm_employee.rest_type/restType`：
  - 员工 JPA 模型、MyBatis Plus PO、`AddEmployeeBO`、薪资列表 VO 均新增 `restType`。
  - `HrmEmployeeMapper.xml#queryPageList` 返回 `affiliationSystem/restType`，新增员工通过 `BeanUtil.copyProperties` 可保存 `restType`，员工固定字段编辑在前端提交 `fieldName=restType` 时可落到 `HrmEmployee.restType`。
  - SQL 脚本 `docs/sql/2026-07-22_hrm_employee_rest_type.sql` 为 `hr_0001` 至 `hr_0005` 增加字段；空值按无资格处理。
- 已统一加班/夜班资格：
  - 薪资链路公共 helper 为 `SalaryMonthRecordServiceNew#isFixedRestProductionEmployee(...)`。
  - 计薪员工、薪资列表和有加班费员工筛选均要求 `affiliation_system=2 and rest_type=2`；兼容字段 `isProduceDept` 也改为该共同条件。
  - 薪资核算与导出对不具备资格的员工强制 `180101/180102=0`；历史薪资项非零也不会继续导出。
  - 加班/夜班统计对非目标员工归零 `overtimeHours/nightShiftCount`，但保留应出勤、实际出勤、应计出勤，避免影响薪资主流程。
  - 考勤汇总同步、Excel 导入和列表查询均归零非目标员工的 `workOverTime/overtimePay/nightShift/nightSubsidy`；编辑这些字段时不具备资格会拒绝保存。
- 额外发现并修复一个夜班补贴兜底问题：`getYeBanAndJiaBan(...)` 在 eligible 员工只有 `nightShift`、没有已落库 `nightSubsidy` 时，旧分支会把先计算出的补贴覆盖成 `0`；已调整为优先读 `nightSubsidy`，缺失时按 `nightShift * subsidy` 计算。
- 已知缺口：
  - 本仓库后端已具备字段保存/返回能力，但未看到员工档案前端字段配置或花名册导入模板新增“休息制度”；若业务侧需要页面直接维护或批量导入休息制度，需要在前端和字段配置 SQL 中继续补齐。

# Findings: 单双休与生产月休四天代码排查

## 2026-07-21 Pre-work
- 已读取后端 `hainan/docs/requirements.md`、`hainan/docs/development.md`，以及前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 当前业务规则已确认：
  - 员工所属体系来自 `hrm_employee.affiliation_system` / `HrmEmployee.affiliationSystem`，`1=行政体系`、`2=生产体系`。
  - 行政体系员工的应出勤走“单双休设置”月度日历，即 `hrm_workweek_setting.week_type` 叠加法定休息、调休上班、手动日设置后的 `workDays`。
  - 生产体系员工的应出勤走“自然月天数 - 生产体系月休天数 - 工作日法定休息日”，生产月休天数来自 `hrm_salary_basic.production_monthly_rest_days`，默认 `4`。
  - 前端不直接判断 `affiliationSystem/isProduceDept`，只维护月休配置、单双休日历和展示后端统计结果。

## 2026-07-21 Code Locations
- 员工体系来源：
  - `src/main/java/com/tianye/hrsystem/model/HrmEmployee.java` 定义 JPA 字段 `affiliationSystem`。
  - `src/main/java/com/tianye/hrsystem/entity/po/HrmEmployee.java` 定义 MyBatis Plus 字段 `affiliationSystem`。
  - `src/main/resources/mapper/HrmSalaryMonthEmpRecordMapper.xml` 的计薪员工查询和薪资列表查询返回 `affiliationSystem`，并由 `affiliation_system=2` 派生兼容字段 `isProduceDept`。
  - `src/main/resources/mapper/HrmEmployeeMapper.xml#queryHasOverTimePayEmpList` 只筛选 `affiliation_system = 2` 的生产体系员工。
- 单双休配置维护：
  - `src/main/java/com/tianye/hrsystem/modules/workweek/controller/HrmWorkweekSettingController.java` 提供 `/hrmWorkweekSetting/queryYearSettings`、`queryMonthCalendar`、`initYearSettings`、`updateWeekType`、`saveMonthCalendar`。
  - `src/main/java/com/tianye/hrsystem/modules/workweek/service/HrmWorkweekSettingService.java` 维护 `SINGLE_REST=1`、`DOUBLE_REST=2`，初始化全年交替周，修改某周及后续，生成月度 `workDays/restDays/legalHolidayRestDays/adjustedWorkDays`。
  - `HrmWorkweekSettingService#isWeeklyRestDay` 决定双休为周六周日、单休为周日。
  - `src/main/java/com/tianye/hrsystem/modules/workweek/entity/HrmWorkweekSetting.java` 映射 `hrm_workweek_setting.week_type`。
  - `src/main/java/com/tianye/hrsystem/modules/workweek/entity/HrmWorkweekDaySetting.java` 映射 `hrm_workweek_day_setting.day_type`。
- 生产月休配置维护：
  - `src/main/java/com/tianye/hrsystem/modules/salary/support/HrmSalaryBasicDefaults.java` 定义默认 `PRODUCTION_MONTHLY_REST_DAYS=4`，并对旧配置或空配置补默认值。
  - `src/main/java/com/tianye/hrsystem/modules/salary/entity/HrmSalaryBasic.java`、`dto/QuerySalaryBasicDto.java`、`vo/QuerySalaryBasicVO.java` 暴露 `productionMonthlyRestDays`。
  - `src/main/java/com/tianye/hrsystem/modules/salary/service/HrmSalaryBasicService.java` 保存、查询最新基本工资设置时应用默认值。
  - `src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryBasicController.java` 暴露保存和查询接口。
- 加班/夜班统计分流与落库：
  - `src/main/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImpl.java` 注入 `HrmWorkweekSettingService` 和 `HrmSalaryBasicMapper`。
  - `resolveExpectedAttendanceDays(...)`：`affiliationSystem=2` 走生产月休，`affiliationSystem=1` 走行政单双休日历，未知返回 `0`。
  - `queryProductionExpectedAttendanceDays(...)`：`month.lengthOfMonth() - productionMonthlyRestDays - legalHolidayRestDays`。
  - `queryProductionLegalHolidayRestDays(...)` 只扣工作日法休，周六/周日法休不在月休之外重复扣。
  - `queryAdministrativeExpectedAttendanceDays(...)` 读取单双休日历 `calendar.workDays`。
  - `calculateMonthlyDetails(...)` 和 `applyMonthlyAttendanceDays(...)` 把 `expectedAttendanceDays/actualAttendanceDays/accruedAttendanceHours` 写入 `hrm_overtime_night_statistics_detail`。
- 薪资消费：
  - `SalaryMonthRecordServiceNew#loadOvertimeNightStatisticsDetails` 批量读取统计明细。
  - `buildExpectedAttendanceDaysByEmployee` 和 `buildAccruedAttendanceDaysByEmployee` 从统计明细生成薪资核算 map。
  - `requireSalaryExpectedAttendanceDays` 缺应出勤时提示先到“单双休设置”维护数据，不再回退旧考勤配置。
  - `fillAttendanceDataForEmployee` 使用 `isProductionAffiliationSystem(map)` 判断生产体系，读取统计落库应出勤，写工资项 `1=应出勤天数`、`2=应计出勤天数`。
  - `getWorkHoursFromBatch` 行政体系按 8 小时，生产体系按排班工时。
  - `getYeBanAndJiaBan` 和 `resolveExportOvertimePay` 只允许生产体系员工生成/导出加班费。
  - `SalaryComputeContext` 保存 `expectedAttendanceDaysByEmployee` 供薪资计算复用。
  - `SalaryMonthRecordService_Bak` 仍被工资条服务和已禁用自动任务注入，当前复用 `SalaryMonthRecordServiceNew.isProductionAffiliationSystem(map)` 判断生产体系，但旧提示仍说维护考勤天数配置，属于历史兼容代码。
- 考勤汇总关联：
  - `HrmProduceAttendanceServiceImpl#resolveDepartmentType` 把员工 `affiliationSystem` 写入考勤汇总 `department=1/2`，缺失按行政兜底。
  - `HrmProduceAttendanceMapper.xml` 用 `department` 过滤考勤汇总。
- 前端：
  - `hr_web/src/router/config.js` 注册 `/hrm/attendance/overtimeNight`、`/hrm/attendance/workweek`、`/overtimeNightDaily`。
  - `hr_web/src/api/hrm/attendance/workweek.js` 对接单双休设置后端接口。
  - `hr_web/src/views/hrm/attendance/workweek/Index.vue` 展示/初始化/调整单双休，保存月度手动日历。
  - `hr_web/src/views/hrm/attendance/workweek/workweek-utils.js` 格式化单休/双休、月度概览和更新 payload。
  - `hr_web/src/views/manage/salary/Index.vue` 维护 `productionMonthlyRestDays`，默认 `4`。
  - `hr_web/src/views/hrm/attendance/upload/Upload.vue` 使用后端 `department=1/2` 展示行政/生产部门筛选。
  - `hr_web/src/api/hrm/attendance/overtimeNight.js`、`hr_web/src/views/hrm/attendance/overtime-night/*` 展示后端返回的 `expectedAttendanceDays/actualAttendanceDays/accruedAttendanceHours`。
- SQL：
  - `docs/sql/2026-04-06_hrm_workweek_setting.sql`、`2026-04-06_hrm_workweek_setting_hr_0001_to_hr_0005.sql` 创建年度单双休周表。
  - `docs/sql/2026-06-17_hrm_workweek_day_setting.sql`、`2026-06-17_hrm_workweek_day_setting_hr_0001_to_hr_0005.sql` 创建月度每日上班/休息手动表。
  - `docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql` 给 `hrm_salary_basic` 增加 `production_monthly_rest_days default 4`。
  - `docs/sql/2026-07-12_overtime_night_attendance_days.sql` 和 `2026-07-14_overtime_night_accrued_attendance_hours.sql` 增加统计落库字段。
  - `docs/sql/2026-06-16_menu_permission_seed.sql` 增加加班/夜班统计和单双休设置菜单。

## 2026-07-21 Formula Update Findings
- 用户补充规则：行政体系属于单双休，但应出勤天数必须根据调用方传入的年-月参数读取“单双休设置”该年月出勤时间汇总；生产体系逻辑和其它要求保持不变。
- 当前 `HrmOvertimeNightStatisticsServiceImpl#queryAdministrativeExpectedAttendanceDays(...)` 已按 `YearMonth` 组装 `QueryWorkweekMonthCalendarBO.year/month`，读取 `workweekSettingService.queryMonthCalendar(...).workDays`，主体方向符合补充规则。
- 当前 `HrmOvertimeNightStatisticsServiceImpl#queryProductionExpectedAttendanceDays(...)` 已按 `month.lengthOfMonth() - resolveProductionMonthlyRestDays() - queryProductionLegalHolidayRestDays(month)` 计算；`resolveProductionMonthlyRestDays()` 从最新 `hrm_salary_basic` 读取，缺失时走 `HrmSalaryBasicDefaults.productionMonthlyRestDays(...)`/默认常量。
- 发现薪资链路风险点：`SalaryMonthRecordServiceNew#resolveSalaryNormalDaysForCompute(...)` 在 `ctx` 缺失、考勤数据 map 缺失、统计 map 缺失时仍会回退已有月薪记录 `needWorkDay`，这会绕过公式/统计落库应出勤。
- 发现查询展示潜在风险点：`HrmOvertimeNightStatisticsServiceImpl` 的行政应出勤缓存 key 只有 `YearMonth`；生产体系不使用该缓存，当前没造成行政/生产串值，但测试应锁定行政必须用传入年月调用单双休月历。

# Findings: 清理薪资考勤 deptType 体系硬编码

## 2026-07-21 Pre-work
- 用户要求：凡是用 `deptType` 表示部门属于行政/生产体系的地方全部删除，改为从员工表读取所属体系；核算薪资和导出薪资时行政体系员工没有加班费。
- 现有文档规则已确认：
  - 员工所属体系唯一来源是 `hrm_employee.affiliation_system`。
  - `affiliation_system=1` 行政体系，`affiliation_system=2` 生产体系。
  - 薪资内部历史 `deptType=0/1` 只应是员工所属体系的派生结果，不应再读取考勤配置或部门表判断。
  - 组织管理 `deptType=1公司/2部门` 与员工体系不是同一个概念，本轮不作为删除对象。
- 当时待改重点：
  - `SalaryMonthRecordServiceNew` 仍保留 `normalDaysByDeptType`、`queryNormalDaysByDeptType(...)`、`deptType` 参数传递和 `HrmAttendanceInfoMapper` 依赖。
  - 行政员工无加班费需锁定在 `getYeBanAndJiaBan(...)` 及导出行读取工资项的链路中。

## 2026-07-21 Implementation Findings
- 修复前薪资核算的旧体系类型残留集中在 `SalaryMonthRecordServiceNew`：
  - `HrmAttendanceInfoMapper/HrmAttendanceInfo` 只用于按 `year/month/dept_type` 读取应出勤配置；
  - `normalDaysByDeptType` 在考勤同步批量数据、薪资计算批量数据和 `SalaryComputeContext` 中传递；
  - `resolveSalaryNormalDaysForCompute(...)`、`fillAttendanceDataForEmployee(...)` 和半路转正调用链会继续接收旧体系类型参数。
- 修复后薪资服务只保留 `isProductionAffiliationSystem(...)`，该方法只读取计薪员工 map 中的 `affiliationSystem`；`isProduceDept` 兼容字段和旧员工 ID 规则不会参与判断。
- `180101 / 加班费` 的实际落点仍是 `getYeBanAndJiaBan(...)`；该方法已有 `isProduce` 布尔防护，本轮保留并用测试覆盖行政体系加班费写 `0`。
- 导出链路 `buildSalaryDataRow(...)` 原本直接读取已存工资项 `180101`；如果历史薪资项已有行政员工非零加班费，导出仍会带出。本轮在导出行构建时按 `affiliationSystem` 再次判断生产体系，行政或未知体系导出 `加班工资=0`。
- `HrmAttendanceInfoMapper#queryInfo(...)` 在薪资服务清理后无调用者；考勤天数配置模块中的体系字段可删除，保留按年月查询和保存应出勤天数的接口。
- 2026-07-21 本次追加后，`resolveSalaryNormalDaysForCompute(...)` 又进一步收紧为只读取加班/夜班统计 `expected_attendance_days` 聚合 map，不再从旧考勤 map 或历史月薪记录 `needWorkDay` 取应出勤。
- 全局扫描结果：
  - 后端剩余 `deptType/dept_type` 仅在组织管理 `HrmDept/AddDeptBO/DeptVO/DeptEmployeeVO`，语义为 `公司/部门`；
  - 前端剩余 `deptType` 仅在组织管理页面，语义同样为 `公司/部门`；
  - 薪资模块和考勤天数配置模块已无体系意义的 `deptType/dept_type`。

# Findings: 员工所属体系使用点全系统排查

## 2026-07-21 Pre-work
- 已读取后端项目 `docs/requirements.md` 与 `docs/development.md`。
- 当前文档中的核心规则：
  - 考勤汇总同步时员工部门类型必须使用员工档案 `affiliation_system`。
  - `affiliation_system=1` 对应行政体系；`affiliation_system=2` 对应生产体系。
  - 薪资内部历史 `deptType` 映射为行政 `0`、生产 `1`。
  - 兼容字段 `isProduceDept` 只能由 `affiliation_system` 派生。
  - 未维护所属体系时按行政体系兜底。
- 本轮排查范围先覆盖后端 `src/main/java`、`src/main/resources`、`src/test`、`docs`，随后扩展到同级前端项目 `hr_web`。

## 2026-07-21 Exact Field Scan
- 精确字段检索已覆盖：
  - `src/main/java`
  - `src/main/resources`
  - `src/test`
  - `docs`
- 直接命中：
  - 员工实体 `HrmEmployee.affiliationSystem` / 数据库字段 `affiliation_system`。
  - 薪资 VO `QuerySalaryPageListVO.affiliationSystem/isProduceDept`。
  - `SalaryMonthRecordServiceNew` 的薪资内部 `deptType` 解析和计算调用链。
  - `HrmSalaryMonthEmpRecordMapper.xml` 的计薪员工查询和薪资列表查询。
  - `HrmEmployeeMapper.xml#queryHasOverTimePayEmpList` 的生产体系员工筛选。
  - `HrmAttendanceInfoMapper.xml` 与考勤配置实体/BO/VO 的历史 `deptType`。
  - 单元测试已覆盖“薪资必须用员工所属体系派生生产判断”和“有加班费员工筛选必须用所属体系”。

## 2026-07-21 Usage Classification
- 字段定义：
  - `model/HrmEmployee` 映射数据库 `affiliation_system`。
  - `entity/po/HrmEmployee` 在 MyBatis Plus 员工对象中暴露 `affiliationSystem`。
- 薪资链路：
  - `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 返回 `affiliationSystem`，并由 `affiliation_system=2` 派生兼容字段 `isProduceDept`。
  - `HrmSalaryMonthEmpRecordMapper.xml#querySalaryMonthList` 同样返回 `affiliationSystem/isProduceDept`，用于薪资列表和导出员工行。
  - `SalaryMonthRecordServiceNew#resolveSalaryDeptType(...)` 只读取 `affiliationSystem`，映射为薪资内部 `deptType`：行政 `0`、生产 `1`，缺失兜底行政。
  - `fillAttendanceDataForEmployee(...)` 通过所属体系派生 `isProduce`，影响排班工时、病/事假折算、全勤判断上下文、加班费工资项。
  - `getYeBanAndJiaBan(...)` 中加班费 `180101` 仅对生产体系员工生效。
  - 半路转正计算 `processMidMonthPromotionSalary(...)` 接收同一个 `deptType`。
- 考勤汇总链路：
  - `HrmProduceAttendanceServiceImpl#resolveDepartmentType(...)` 从 `HrmEmployee.getAffiliationSystem()` 写入考勤汇总 `department=1/2`，缺失兜底行政。
  - Excel 导入考勤汇总和“加班/夜班统计 -> 考勤汇总”同步都会调用该方法。
- 加班/夜班统计链路：
  - `HrmOvertimeNightStatisticsServiceImpl` 用 `affiliationSystem` 判断行政/生产体系。
  - 实际出勤加班来源：行政和生产体系都走本地审批加班。
  - 应出勤天数：生产体系走 `当月自然天数 - 生产体系月休天数 - 工作日法定休息日`；行政体系走单双休月历 `workDays`；未知体系返回 `0`。
- 生产体系配置依赖：
  - `HrmSalaryBasic.productionMonthlyRestDays` / `QuerySalaryBasicDto` / `QuerySalaryBasicVO` / `HrmSalaryBasicDefaults`。
  - `HrmSalaryBasicService` 保存和查询基本工资设置时填默认 `4`。
  - SQL `docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql` 给各租户库新增 `production_monthly_rest_days`。
  - 前端 `hr_web/src/views/manage/salary/Index.vue` 展示并保存“生产体系员工月度休息天数”。
- 前端直接引用：
  - `hr_web/src/views/hrm/attendance/upload/Upload.vue` 的考勤汇总筛选和列表展示使用后端落库字段 `department=1/2`，文案为行政/生产部门。
  - 未发现前端直接消费 `affiliationSystem` 或 `isProduceDept`。
- 测试保护：
  - `SalaryMonthRecordServiceNewTest` 覆盖所属体系优先于旧员工 ID/旧 `isProduceDept` 例外。
  - `HrmSalaryMonthEmpRecordMapperXmlTest` 覆盖计薪员工和薪资列表必须返回/派生所属体系。
  - `HrmEmployeeMapperSqlTest` 覆盖有加班费员工筛选必须使用 `affiliation_system=2`。
  - `HrmProduceAttendanceServiceImplTest`、`HrmOvertimeNightStatisticsServiceImplTest` 多处构造 `affiliationSystem=1/2` 覆盖同步和统计口径。

# Findings: 张雪梅两天病假扣全勤

## 2026-07-21 Pre-work
- 已读取后端项目 `docs/requirements.md` 与 `docs/development.md` 的薪资核算相关记录。
- 已有病假扣款规则：病假天数 `<= 2` 时，工资项 `19010401 / 假期扣款(病假)` 写入 `0`；超过 2 天时只对超出部分按最低基本工资日额扣款。
- 已有全勤兜底规则来自 2026-07-20 修复：当应计出勤达到加班/夜班统计应出勤时，允许生成 `40102 / 全勤奖`，用于修复张明等“原始钉钉缺卡/短时迟到但最终无考勤扣款”的漏发场景。
- 本次用户明确新口径：张雪梅有两天病假钉钉申请；病假不扣工资，但应扣全勤。该口径要求全勤兜底不能忽略病假申请本身。
- 只读数据核验 `hr_0003 / 2026-06`：
  - 张雪梅 `employee_id=1831601326890434590 / 手机号 15872992452 / 工号 TYNG-371`，正式员工、已转正、启用全勤，生产体系。
  - 钉钉审批快照中 2026-06-01、2026-06-02 各有 `8` 小时病假，另有 2026-06-06 年假 `4` 小时和 2026-06-15 加班 `1` 小时。
  - 加班/夜班统计应出勤 `25` 天，应计出勤 `200` 小时，即薪资应计 `25` 天；当前 2026-06 薪资明细 `need_work_day=25.00`、`actual_work_day=25.00`。
  - 当前工资项已有 `40102=100.00`、`19010401=0`，说明病假工资免扣已生效，但全勤奖被兜底发放。

## 2026-07-21 Root Cause and Fix
- 代码根因在 `SalaryMonthRecordServiceNew#fillAttendanceDataForEmployee(...)`：
  - 先调用 `checkIsFullAttendance(...)` 判断原始月度汇总是否满勤；
  - 如果非满勤，再调用 `shouldFallbackFullAttendanceByAccruedDays(...)`，只要薪资应计出勤达到应出勤就把 `isFullAttendance` 重新置为 true；
  - 旧 helper 没有“有效病假天数”入参，而 `leaveOfsickDays` 又在全勤兜底之后才计算，导致张雪梅的 2 天病假被应计出勤补满后仍拿到全勤奖。
- 修复实现：
  - 将 `workTimsMap` 和 `leaveOfsickDays` 计算提前到全勤兜底之前；
  - `shouldFallbackFullAttendanceByAccruedDays(...)` 新增 `sickLeaveDays` 入参，正数病假直接拒绝兜底；
  - 病假工资扣款仍在后续原位置按 `<=2` 天写 `19010401=0`，不改变病假工资免扣规则。

# Findings: 开始核算界面计薪员工规则说明

## 2026-07-21 Pre-work
- 用户要求在“开始核算”界面增加一行说明，解释当月计薪员工按什么规则获取。
- 后端计薪员工基础 SQL 位于 `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList`：
  - `hrm_employee.is_del = 0`；
  - `entry_time <= 薪资结束日`；
  - `entry_status in (1,3)` 直接纳入，其中 `1=在职`、`3=待离职`；
  - `entry_status=4` 离职员工仅在 `plan_quit_time > date_sub(薪资结束日, interval 1 month)` 时纳入。
- `SalaryMonthRecordServiceNew#queryComputeSalaryEmployeeList(...)` 继续复用 `queryHasSalaryArchivesEmployeeList(...)`，后者再要求员工存在合计金额大于0的薪资档案。
- 前端 `AloneComputeDialog.vue` 同时被“开始核算”和“导出薪资”复用，因此规则说明需要做成可选 prop，并且只由开始核算实例传入。

## 2026-07-21 Implementation
- `AloneComputeDialog.vue` 新增 `ruleTip` prop，并在“核算方式”下方渲染 `.compute-rule-tip`；默认空字符串时不展示。
- `SalaryManage.vue` 的“开始核算”实例传入规则文案：未删除且入职日期不晚于薪资结束日；在职、待离职直接纳入；离职员工按计划离职日期晚于薪资结束日前1个月纳入；同时必须存在合计金额大于0的薪资档案。
- “导出薪资”实例未传 `ruleTip`，避免导出范围选择弹窗出现开始核算专用说明。

# Findings: 薪资核算穿梭框员工来源统一

## 2026-07-21 Pre-work
- 已读取项目 `docs/requirements.md` 和 `docs/development.md` 中薪资管理开始核算范围选择相关条目。
- 业务约束：
  - “按人员”右侧选择人员时只核算选中人员，未选择时全员核算；一次最多 50 人。
  - “按部门”右侧选择部门时，只核算所选部门及下级部门内的可计薪员工；一次最多 50 人。
  - 同名员工前端必须展示手机号，后端实际核算主键仍为员工 ID。
  - `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList` 是可核算员工列表接口，口径需与 `computeSalaryData` 内 `queryHasSalaryArchivesEmployeeList` 一致。
- 代码现状：
  - 后端入口为 `HrmSalaryMonthRecordController#queryComputeSalaryEmployeeList`。
  - 服务实现 `SalaryMonthRecordServiceNew#queryComputeSalaryEmployeeList` 当前直接调用 `queryHasSalaryArchivesEmployeeList(salaryMonthRecord, Collections.emptyList())`。
  - 前端 `AloneComputeDialog.vue` 打开弹窗时调用该接口填充 `employeeOptions`；部门模式通过 `collectSalaryComputeDeptEmployeeIds(selectedDeptIds, employeeOptions, deptTree)` 从同一员工集合中过滤。
- 本轮实现目标是加固前端工具函数和测试，确保部门模式不能改成从普通员工列表或部门树直接推员工。
- 已读取前端项目 `hr_web/docs/requirements.md` 与 `hr_web/docs/development.md`。
- 前端约束：
  - 薪资管理页“开始核算”只保留一个入口，点击后弹出范围选择弹窗。
  - 按人员使用 `el-transfer`，右侧未选表示全部计薪人员。
  - 按部门使用部门树，右侧有部门时自动展开该部门及下级部门内计薪人员。
  - `AloneComputeDialog.vue` 和 `salary-compute-scope-utils.js` 是当前实现边界；员工选项必须来自 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList`。

## 2026-07-21 Implementation
- `salary-compute-scope-utils.js` 新增 `filterSalaryComputeDeptTreeByEmployees(tree, employeeOptions)`：
  - 从可核算员工选项中收集 `deptId`；
  - 递归过滤部门树；
  - 只保留自身部门存在可核算员工，或下级部门存在可核算员工的节点。
- `AloneComputeDialog.vue` 先将后端部门数据归一化为树，再调用上述函数过滤；`deptEmployeeIds` 和最终 payload 继续只从同一个 `employeeOptions` 集合展开员工 ID。
- 结果：按人员模式左侧员工来自可核算员工接口；按部门模式可选部门和提交员工也被同一批可核算员工约束。

# Findings: SalaryMonthRecordServiceNew 编译缺失排查

## 2026-07-20 Pre-work
- 已读取项目 `docs/requirements.md` 和 `docs/development.md`。
- 与本轮直接相关的约束：
  - 薪资导出“满勤天数”和“超缺勤天数”读取加班/夜班统计落库结果，不再依赖 `hrm_attendance_info`。
  - 导出异常必须向上返回失败响应，不能后端吞异常后让前端下载空白 Blob。
  - `SalaryMonthRecordServiceNew` 近期涉及薪资核算、导出和个税/全勤多个改动，编译错误必须以完整 Maven 报错定位，不能仅凭 IDE 红线猜测。
- 当前工作区存在大量未提交和未跟踪文件，本轮不得回退无关改动。

## 2026-07-20 Root Cause
- `mvn -DskipTests compile` 可成功编译 1044 个主源码文件；`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 可成功编译测试并通过 43 个测试。因此 `SalaryMonthRecordServiceNew` 的 import、字段和方法在 Maven/Javac 视角并未缺失。
- 该类涉及的近期新增类型均在源码树中存在，包括：
  - `com.tianye.hrsystem.model.HrmOvertimeNightStatisticsDetail`
  - `com.tianye.hrsystem.repository.hrmOvertimeNightStatisticsDetailRepository`
  - `com.tianye.hrsystem.modules.salary.support.HrmSalaryBasicDefaults`
  - `com.tianye.hrsystem.modules.salary.vo.SalaryComputeProgressVO`
- 更可能的根因是 IDE Maven 同步/索引异常。证据是全局 Maven 配置 `/Users/jiangyongming/devTools/apache-maven-3.8.4/conf/settings.xml` 在命令行构建时提示两条 settings 警告：
  - `Unrecognised tag: 'profiles'`，原因是 profile 内将 `<properties>` 误写为 `<profiles>`。
  - `mirrors.mirror.id` 非法，原因是 mirror id 写成了 `nexus-aliyun>`。
- 已修正全局 Maven settings 两处 XML 错误；修正后重新运行 `mvn -DskipTests compile` 不再出现 settings 解析警告，编译仍通过。
- 仍存在项目 `pom.xml` 自身警告：`jsch` 依赖重复、`easyexcel` 版本重复、两个 `systemPath` 依赖指向项目内 jar。这些不是本轮 `SalaryMonthRecordServiceNew` 找不到包的直接原因，但后续可以单独清理。

# Findings: 薪资导出满勤天数按加班夜班统计

## 2026-07-20 Correction From User
- 用户纠正：薪资导出的满勤天数逻辑应来自加班/夜班统计“开始统计”功能中统计并入库的“应出勤时间 - 应计出勤时间”；要求自行查找入库字段。
- 上次把“满勤天数”直接改为员工月薪记录 `actualWorkDay` 是错误假设，需要重新定位加班/夜班统计落库结果。
- 已复核文档：
  - 加班/夜班统计页面中“应出勤时间”按 `expectedAttendanceDays * 8` 展示。
  - “应计出勤小时(天数)”使用接口字段 `accruedAttendanceHours`。
  - `hrm_overtime_night_statistics_detail.accrued_attendance_hours` 已作为应计出勤小时落库字段；`expected_attendance_days` 是应出勤天数字段。
- 当前待查：
  - `HrmOvertimeNightStatisticsDetail` 实体与 Repository 是否可按 `statYear/statMonth/employeeId` 查询。
  - “开始统计”保存明细时是否每个员工月度都有可唯一使用的统计行。
  - 薪资导出最终应写“满勤天数”列还是“超缺勤天数”列；用户给出的公式是差额，语义更接近现有“超缺勤天数”，但本轮先以源码和模板核实。
- 代码与库字段证据：
  - 实体 `HrmOvertimeNightStatisticsDetail` 映射 `expected_attendance_days`、`actual_attendance_days`、`accrued_attendance_hours`。
  - Repository 已有 `findAllByStatYearAndStatMonthAndEmployeeIdIn(...)`，可供薪资导出按年月和员工集合一次性读取统计明细。
  - “开始统计”在 `calculateMonthlyDetails(...)` 生成每个日明细时写入月度 `expectedAttendanceDays` 和 `accruedAttendanceHours`，并在最后通过 `applyMonthlyAttendanceDays(...)` 回填到该员工当月所有明细；没有原始明细时会用 `buildZeroDetailRow(...)` 保存占位行。
  - DDL 注释确认单位：`expected_attendance_days` 是“月度应出勤天数”，`accrued_attendance_hours` 是“月度应计出勤小时”。
  - 薪资导出模板第 8 列为 `满勤天数` -> `{.normaldays}`，第 9 列为 `超缺勤天数` -> `{.absencehours}`；用户明确点名“满勤天数”，本轮将按公式写第 8 列，不扩展改动第 9 列。
- 只读 SQL 复核 `hr_0003 / 2026-06`：
  - 李明明：`expected_attendance_days=23`、`accrued_attendance_hours=184.00`，差额 `(23*8-184)/8=0`。
  - 张艳涛：`25 / 0.00`，差额 `25` 天。
  - 存在应计出勤高于应出勤的员工，差额会为负数；现有导出差额类字段也允许负值，本轮不截断为 `0`。

## 2026-07-20 Pre-work and Root Cause
- 已读取 `docs/requirements.md`、`docs/development.md` 和现有计划/记录。
- 既有薪资口径已经要求：薪资核算字段名 `actual_work_day` 保持兼容，但业务含义为“应计出勤天数”；薪资列表、导出表头和前置校验需显示“应计出勤天数”。
- 本轮用户最终明确规则：薪资导出里的“满勤天数”读取加班/夜班统计已落库的“应计出勤时间”，“超缺勤天数”按“应出勤时间 - 应计出勤时间”计算。
- 代码定位：
  - `SalaryMonthRecordServiceNew#exportSalaryNew(...)` 给导出工资项 `9007 / 满勤天数` 写入 `vo.getNeedWorkDay()`。
  - `SalaryMonthRecordServiceNew#buildSalaryDataRow(...)` 再次把导出行 Map 的“满勤天数”写入 `vo.getNeedWorkDay()`。
  - 因最终导出行从 `buildSalaryDataRow(...)` 读取，必须同时修正两个入口，避免字段值前后不一致。
- RED 验证：新增 `SalaryMonthRecordServiceNewTest#buildExportAbsenceDaysByEmployee_shouldCalculateExpectedMinusAccruedFromOvertimeNightStatistics`，首次运行编译失败于 helper 未实现。
- GREEN 实现：`exportSalaryNew(...)` 批量读取导出员工当月 `HrmOvertimeNightStatisticsDetail`；`buildExportFullWorkDaysByEmployee(...)` 计算 `accruedAttendanceHours / 8` 写入 `9007 / 满勤天数`，`buildExportAbsenceDaysByEmployee(...)` 计算 `(expectedAttendanceDays * 8 - accruedAttendanceHours) / 8` 写入 `9008 / 超缺勤天数`；`buildSalaryDataRow(...)` 改为读取预置 `9007/9008`。若对应统计明细缺失，则抛出提示先执行“开始统计”的业务错误。
- 回归验证：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 43 个测试；薪资组合回归通过 65 个测试。
- 不再使用的旧口径：导出“超缺勤天数”不再用 `resolveExportNormalDays(...)` 解析正常天数后减薪资应计出勤天数，也不再依赖 `hrm_attendance_info`。

# Findings: 薪资管理导出薪资空白排查

## 2026-07-20 Pre-work
- 已读取 `docs/requirements.md` 与 `docs/development.md`。
- 相关约束：
  - 薪资列表、导出表头和前置校验错误文案应展示“应计出勤天数”，底层 `actual_work_day` 字段名保持兼容。
  - 非固定工资项初始化不应预插入会遮挡真实计算值的默认 `0` 行；工资项读取需避免重复默认项导致导出显示错误。
  - 薪资核算涉及同名员工时以后端员工 ID 为主键，前端展示姓名 + 手机号辅助识别。
  - 工资核算和导出链路应复用员工 `affiliation_system` 判断行政/生产体系，不再按员工 ID 或部门名硬编码。
- 初始待验证点：
  - 导出接口是否实际查询到了薪资员工记录和工资项明细。
  - Excel 模板是否存在且 sheet/单元格写入位置正确。
  - 导出使用的查询参数是否与列表页一致，尤其是薪资主记录 ID、月份、部门/人员范围。
  - 前端是否把 Blob 空响应当成成功下载，或传错字段名导致后端按空条件导出。

## 2026-07-20 Export Entry Trace
- 后端工资导出入口为 `POST /hrmSalaryMonthRecord/exportSalary`，controller 调用 `SalaryMonthRecordServiceNew#exportSalaryNew(...)`。
- controller 当前捕获所有异常后只 `printStackTrace()`，不返回业务错误；如果服务层在写出前抛异常，前端可能收到空响应并表现为“导出空白”。
- `exportSalaryNew(...)` 主要流程：
  - 根据 `QuerySalaryExportDto.salaryRecordId` 读取 `HrmSalaryMonthRecord` 年月。
  - 查询考勤汇总、考勤汇总导入数据、薪资表头和员工薪资分页数据。
  - 调 `initExportData(dataList, year, month)` 写入中间表 `hrm_salary_export`。
  - 再通过 `HrmSalaryExportMapper#queryExportData(year, month)` 读出，使用 `/export/salary_export.xlsx` 或 `/export/salary_export_cd.xlsx` 模板填充。
- 初步风险点：
  - `salaryRecordId` 为空或不存在时，`salaryMonthRecord.getMonth()` 会空指针；异常被吞后不会给前端明确错误。
  - 模板资源存在：`salary_export.xlsx`、`salary_export_cd.xlsx`、`salary_export_new.xlsx` 均在 `src/main/resources/export` 下。
  - `exportSalaryNew(...)` 没有直接检查模板 `InputStream` 是否为空，资源缺失会在 EasyExcel 内部异常后被 controller 吞掉。

## 2026-07-20 Frontend and Template Trace
- 前端薪资管理页调用：
  - `src/views/hrm/salary/salary/SalaryManage.vue#handleExportSalary(...)` 传 `salaryRecordId: state.form.srecordId`，与后端 `QuerySalaryExportDto.salaryRecordId` 匹配。
  - 同时传了 `jobNummber`，但后端 DTO 当前没有该字段，且导出 SQL 也不按工号筛选；这不是空白主因，但属于筛选无效问题。
  - Axios 响应拦截器对 `responseType=blob` 返回 `response.data`，薪资下载函数再用 `new Blob([res])` 触发下载，基础下载链路可工作。
- Excel 模板占位符：
  - `salary_export.xlsx` 与 `salary_export_cd.xlsx` 都使用 `{header.*}` 和 `{.*}` 形式填充。
  - 普通公司模板用 `{.dutiessalary}`，`0002` 模板用 `{.performance}`；两套模板均使用 `{.deptname}`。
  - `HrmSalaryExportMapper#queryExportData(...)` 返回了 `dept as deptname`，与模板字段一致，因此部门字段别名不是“整表空白”的根因。
- 当前本机 `9080` 进程是另一个 `watch` 工程的 `HrsystemApplication`，不是本仓库 `hainan` 的运行产物；不能用该进程响应验证本代码。

## 2026-07-20 Root Cause
- 新版导出在构造员工行时，每个员工按 `year/month/deptType` 查询 `hrm_attendance_info` 后直接执行 `hrmAllDayInfo.getActualWorkDay()`。
- 当目标薪资月份或员工所属体系没有对应考勤配置行时，`hrmAllDayInfo` 为 `null`，导出服务在写 Excel 前抛空指针。
- controller 捕获异常后只 `printStackTrace()`，没有设置错误响应；前端按 Blob 下载处理时会表现为“空白文件/空白效果”。
- 当前修复方向：导出“满勤天数/超缺勤天数”统一读取加班/夜班统计落库结果，不再使用考勤配置正常天数兜底，避免目标年月缺少 `hrm_attendance_info` 时中断导出。
- 进一步修复：`HrmSalaryMonthRecordController#exportSalary(...)` 不再吞掉 `exportSalaryNew(...)` 抛出的异常，让框架返回失败响应；避免服务层异常时前端仍按成功 Blob 下载空白内容。

## 2026-07-20 Fix Verification
- `SalaryMonthRecordServiceNew#exportSalaryNew(...)` 不再为导出“满勤天数/超缺勤天数”逐员工查询 `hrm_attendance_info`。
- 导出行中的 `满勤天数` 取加班/夜班统计 `accruedAttendanceHours / 8`，`超缺勤天数` 取 `(expectedAttendanceDays * 8 - accruedAttendanceHours) / 8`，并对缺统计明细返回业务错误。
- `HrmSalaryMonthRecordController#exportSalary(...)` 改为直接声明 `throws IOException` 并调用服务层，异常不再被吞掉。
- Fresh verification：薪资组合回归 `mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，63 个测试。
- `git diff --check` 对本轮 tracked 变更文件无输出；整文件尾随空白扫描仍命中 `SalaryMonthRecordServiceNew.java` 的既有非本轮行，本轮未清理无关历史空白。

# Findings: 农谷 2026-06 批量个税支持数据整理与导入

## 2026-07-20 Pre-work
- 目标沿用李明明已成功链路：导入 `2026-05` 个税累计与 `2026-06` 附加累计后，再核算 2026-06 薪资。
- 导入接口与模板列位：
  - `/hrmPersonalIncomeTax/importPersonalIncomeTax`，参数 `personalIncomeTaxFile + dates`，数据从第 3 行开始读取，列位为姓名、岗位、工号、部门、累计收入、累计减除费用、累计公积金社保扣除、累计已缴税额、手机。
  - `/hrmAdditional/importAdditional`，参数 `additionalFile + year + month`，数据从第 3 行开始读取，列位为姓名、岗位、工号、部门、累计子女教育、累计住房租金、累计住房贷款利息、累计赡养老人、累计继续教育、累计养幼女、手机。
  - 两个导入均调用 `TaxImportEmployeeMatcher`，必须能按姓名 + 手机号匹配当前员工；同一文件同一员工年月重复会被拒绝。
- 现有文件对比：
  - `李明明6月个税2.40_个税累计_导入2026-05.xls` 相比原 `个税累计.xls`，过滤了已删除员工 `郑景/杜畅/万怀进`，并且只调整了李明明一行；其他员工仍沿用原 `2026-05` 个税累计数据。
  - `李明明6月个税2.40_附加扣除累计_导入2026-06.xls` 相比原 `附加扣除累计.xls`，过滤了已删除员工 `万怀进`，并将其他员工按年度配置从 5 月累计滚到 6 月累计；李明明被特殊调整为 `12000/4800/0/8857.50/0/0`。
- 原始表列位：
  - `湖北田野农谷生物科技有限公司_综合所得申报_202606.xls`：姓名列 1，累计收入列 29，累计减除费用列 30，累计专项扣除列 31，累计专项附加分项列 32-37，应纳税所得额列 42，应纳税额列 45，已缴税额列 48，应补/退税额列 49。
  - `202607_税款计算_工资薪金所得.xls`：姓名列 1，本期收入列 7，本期专项扣除列 10-13，累计收入列 23，累计减除费用列 25，累计专项扣除列 26，累计附加分项列 27-32，累计应纳税所得额列 40，累计应纳税额列 43，已缴税额列 47，应补/退税额列 48。
- 初步风险：
  - 李明明 `246.90/249.30` 是财务人工确认的特殊值，不是现有批量原始表可直接对所有员工套用的字段。
  - 如果直接用原始 `202606` 的累计已缴税额作为导入 `250105`，系统会按“本月累计税额 - 上月累计已缴”算 6 月工资个税，结果对应原始申报表口径，不一定等于工资表实际扣税口径。

## 2026-07-20 Batch Import
- 最终批量口径：
  - 个税累计导入年月仍为 `2026-05`，供 6 月薪资核算读取为“截至上月”。
  - `累计收入额（截至上月） = 202607 税款计算表累计收入额 - 6月工资表应发工资`。
  - `累计减除费用（截至上月） = 202607 税款计算表累计减除费用 - 5000`。
  - `累计公积金社保扣除（截至上月） = 202607 税款计算表累计专项扣除 - 6月工资表社保 - 6月工资表公积金`。
  - 附加累计导入年月为 `2026-06`，优先使用 `202606` 原始申报表专项附加分项；李明明继续使用已验证的特殊分项 `12000/4800/0/8857.50/0/0`。
  - `累计已缴税额` 按系统公式反推：先用上述累计基础和附加累计计算系统累计应纳税额，再减去 6 月工资表个税，确保本次薪资核算能输出工资表个税。
- 批量结果：
  - `202607` 原始税款计算表共有 180 条员工行，其中 95 条按身份证命中系统当前员工并在 6 月工资表中唯一定位。
  - 未导入的 85 条均为系统当前员工未按身份证命中；样例和明细见预览报告。
  - 6 月工资表个税非零员工为 4 人：李明明 `2.40`、范燕东 `22.19`、王洪平 `2.40`、宁明友 `2.40`。
  - 公式预演 95 条均闭合，预测 6 月个税不一致数为 `0`。
- 生成文件：
  - `final_import_excels/批量6月工资个税_个税累计_导入2026-05.xls`，数据行 `97`，保留原成功导入副本中未更新但可导入的员工行。
  - `final_import_excels/批量6月工资个税_附加扣除累计_导入2026-06.xls`，数据行 `40`。
  - `final_import_excels/批量6月工资个税支持数据预览_20260720.csv` 与同名 `.md`。
- 已通过本机 `9080 / hr_0003` 接口导入：
  - `/hrmAdditional/importAdditional` 参数 `year=2026, month=6` 返回 `code=200`。
  - `/hrmPersonalIncomeTax/importPersonalIncomeTax` 参数 `dates=2026-05` 返回 `code=200`。
- 导入后核验：
  - 数据库 `hr_0003.hrm_personal_income_tax` 中 `2026-05` 为 `97` 行。
  - 数据库 `hr_0003.hrm_additional` 中 `2026-06` 为 `40` 行。
  - 关键员工读回：李明明 `69859/30000/5749/246.90`，范燕东 `45289/30000/3642.30/341.90`，王洪平 `49920/30000/5728.30/371.08`，宁明友 `65446/30000/5749/369.74`。
  - 四名有税员工按读回值复算 6 月个税均与工资表一致。

## 2026-07-20 Employee Baseline Re-import
- 用户修正杨晨雨身份证大小写后，要求以系统员工表作为基准重新匹配原始数据，原始税表多出来的员工不处理。
- 已重新生成员工表基准文件：
  - `final_import_excels/批量6月工资个税_员工表基准_个税累计_导入2026-05.xls`，数据行 `96`。
  - `final_import_excels/批量6月工资个税_员工表基准_附加扣除累计_导入2026-06.xls`，数据行 `40`。
  - `final_import_excels/批量6月工资个税_员工表基准_预览_20260720.csv` 与 `.md`。
- 员工表基准匹配结果：
  - 当前员工表 `113` 人。
  - 原始 `202607` 税表员工行 `180`。
  - 员工表内成功匹配并在 6 月工资表唯一定位 `96` 人。
  - 原始税表多出 `84` 人，已按用户要求忽略。
  - 员工表内 `17` 人未生成：主要为系统员工身份证为空，或原始 `202607` 税表无该身份证。
- 已通过本机接口重新导入，返回均为 `code=200`。
- 导入后核验：
  - 数据库 `hr_0003.hrm_personal_income_tax` 中 `2026-05` 为 `96` 行。
  - 数据库 `hr_0003.hrm_additional` 中 `2026-06` 为 `40` 行。
  - 杨晨雨已纳入 `2026-05` 个税累计，读回为 `9543.06 / 15000.00 / 1179.60 / 0.00`。
  - 李明明、范燕东、王洪平、宁明友四名 6 月工资表个税非零员工复算仍全部闭合。

## 2026-07-20 Name and Mobile Supplement
- 用户要求将员工表基准中未按身份证生成的人员改用“姓名 + 手机号”复核。
- 复核结果：
  - 6 月薪资记录中未进入 `2026-05` 个税累计的人员为吴镜平、刘玉麒、娄艳霞、朱玲丽。
  - 朱玲丽 `18727608635` 在 `final_import_excels/个税累计.xls` 中存在 2026-05 源行，可按姓名 + 手机号补入，值为累计收入 `10650.00`、累计减除费用 `60000.00`、累计专项扣除 `3031.80`、累计已缴税额 `0.00`。
  - 吴镜平、刘玉麒、娄艳霞只在 2025 年历史累计中找到姓名 + 手机号记录，未找到 2026-05 个税累计源，不能用历史年度数据补本次 6 月工资个税。
  - 程传祥、谢杰杰、马国华、孟建立的有证件在职档案已在 96 人版导入；同名另一手机号档案只在公积金表或员工表中出现，缺税表/工资表有效源，未补。
  - 罗毅龙、谷鹏、李娜、罗盛浮、陈潜、李灏铭、单丹、张雄斌、姚玖志未在本次相关 2026 年税表/工资表/累计导入源中找到姓名 + 手机号有效数据，未补。
- 已生成并导入：
  - `final_import_excels/批量6月工资个税_员工表基准含姓名手机号_个税累计_导入2026-05.xls`，数据行 `97`。
  - `final_import_excels/批量6月工资个税_员工表基准含姓名手机号_附加扣除累计_导入2026-06.xls`，数据行 `40`，内容与员工表基准附加累计一致。
  - 预览说明：`final_import_excels/批量6月工资个税_员工表基准含姓名手机号_预览_20260720.csv/.md`。
- 导入后核验：
  - `/hrmAdditional/importAdditional` 和 `/hrmPersonalIncomeTax/importPersonalIncomeTax` 均返回 `code=200`。
  - 数据库 `hr_0003.hrm_personal_income_tax` 中 `2026-05` 为 `97` 行，朱玲丽已写入 `10650.00 / 60000.00 / 3031.80 / 0.00`。
  - 数据库 `hr_0003.hrm_additional` 中 `2026-06` 仍为 `40` 行。
  - 6 月薪资记录仍无 `2026-05` 个税累计的人员剩吴镜平、刘玉麒、娄艳霞，原因均为缺 2026 年有效源数据。

# Findings: 农谷社保医保方案整理与员工参保方案设置

# Findings: 薪资导出社保多 3 元排查

## 2026-07-20 Pre-work
- 已启用项目文档优先、文件化计划和系统性排查流程。
- 已读取项目 `docs/requirements.md`、`docs/development.md` 和现有计划/记录。
- 与本轮直接相关的既有约束：
  - `hrm_salary_basic.long_term_care_insurance_amount` 默认长期护理保险金额为 `3`。
  - 社保方案项目只写养老、医疗、失业、工伤、生育、公积金；长期护理固定金额不得在方案项目中重复落库。
  - 6 月工资表中的“社保扣款”已包含长期护理固定金额 `3`。
  - 社保方案管理中的个人社保合计应在社保项目个人合计基础上加长期护理金额；生成员工月度社保记录时也会按同一固定金额逻辑写入。
- 初始待验证点：
  - 薪资导出“个人社保/代扣社保”字段最终读取的是薪资项 `100101`、社保月员工记录，还是导出中间表二次计算值。
  - 若社保月员工记录已包含长期护理 `3`，薪资核算或导出是否再次调用固定金额逻辑叠加。
  - 影响面是否真为全员，以及是否只发生在有社保扣款的员工。

## 2026-07-20 Code Trace
- 薪资导出链路：
  - `SalaryMonthRecordServiceNew#exportSalaryNew(...)` 查询员工薪资项后调用 `buildSalaryDataRow(...)`。
  - `buildSalaryDataRow(...)` 将导出字段“个人社保”设置为工资项 `100101` 的值。
  - `initExportData(...)` 将该列写入 `hrm_salary_export.social`，后续 `HrmSalaryExportMapper#queryExportData(...)` 从中间表读取并填 Excel。
- 薪资核算社保链路：
  - `SalaryMonthRecordServiceNew#getSocialSecurityOption(...)` 在 `isSyncInsuranceData=true` 时，按薪资配置月份读取 `HrmInsuranceMonthEmpRecord`。
  - 工资项 `100101 / 个人社保` 直接取 `insuranceMonthEmpRecord.getPersonalInsuranceAmount()`，没有在薪资核算或导出阶段再次计算项目明细。
- 社保月报生成/手工刷新链路：
  - `HrmInsuranceMonthRecordService#computeInsuranceData(...)` 调 `HrmInsuranceSchemeMapper#queryInsuranceSchemeCountById(...)` 汇总方案金额，再写入 `hrm_insurance_month_emp_record.personal_insurance_amount`。
  - `HrmInsuranceMonthEmpRecordService#updateInsuranceProject(...)` 手工修改员工参保项目后，调 `HrmInsuranceMonthEmpProjectRecordMapper#queryProjectCount(...)` 回写员工月度社保合计。
  - 两个 mapper 都按“项目个人社保合计 + `hrm_salary_basic.long_term_care_insurance_amount`（缺省 `3`）”生成 `personal_insurance_amount`。
- 因此若业务源文件/方案项目中的“个人社保”已经包含长期护理 `3`，当前社保月记录会再加一次 `3`，薪资项 `100101` 和导出“个人社保”会同步偏高 `3`。

## 2026-07-20 Database Evidence
- 目标库按近期薪资记录确认使用 `hr_0003`；当前 2026-06 薪资主记录为 `2077807285177995265 / 六月薪资报表`。
- `hr_0003.hrm_salary_basic` 最新长期护理保险金额为 `3.00`。
- 2026-06 社保月记录：
  - `hrm_insurance_month_emp_record` 正常参保 `84` 人。
  - 所有人 `personal_insurance_amount - hrm_insurance_month_emp_project_record(type not in 10,11).personal_amount合计` 都等于 `3.00`。
  - 金额分布闭合：`359.82=356.82+3` 1 人，`448.80=445.80+3` 24 人，`469.50=466.50+3` 58 人，`672.50=669.50+3` 1 人。
- 2026-06 薪资记录：
  - 有工资项 `100101` 的薪资员工 `95` 人，其中 `83` 名有社保扣款员工满足 `100101 - 项目个人社保合计 = 3.00`。
  - 另 `12` 名员工无有效社保月记录，工资项 `100101=0`。
  - 2026-06 正常社保月记录中有 1 人（罗毅龙 `2033769831940079619`）未进入当前薪资员工记录。
- `hrm_salary_export` 当前 2026-06 中间表只有 `36` 行，说明最近一次导出是范围导出而非全员导出；该表没有员工 ID，且存在同名员工，不能用姓名 join 做精确员工级证据。
- 精确数据链路结论：`参保项目个人社保合计 + 长期护理3 -> hrm_insurance_month_emp_record.personal_insurance_amount -> 薪资项100101 -> 导出个人社保`。
- 时序补充：
  - 现有 2026-06 社保月记录创建时间为 `2026-07-17 18:31:35`。
  - 6 月工资表口径的当前员工参保方案重整 SQL 是 `2026-07-18` 执行；主流新方案的项目社保合计已减去长期护理 `3`，例如 `2078479646634151940` 项目合计 `463.50`，加固定 `3` 后为 `466.50`。
  - 但 2026-06 已生成的社保月员工明细仍挂旧方案，如严锦当月方案 `2011821299616206849 / 社保(466.50) 公积金(144)`，其月度项目个人社保合计已是 `466.50`，固定金额再加 `3` 后记录为 `469.50`。
  - 黎冬霜 `1831601326890434579` 当前员工档案方案为 `2078479646634151940 / 社保(466.50) 公积金(144.00)`，项目个人社保合计 `463.50`，加长期护理后 `466.50`；但她 2026-06 社保月员工记录仍挂旧方案 `2011821299616206849 / 社保(466.50) 公积金(144)`，月记录 `personal_insurance_amount=469.50`，工资项 `100101=469.50`。
  - 全员范围：按当前员工档案方案目标额对比，2026-06 有效社保月员工记录 `83` 条中 `79` 条个人社保不一致，公积金不一致 `0` 条；2026-06 薪资中非零 `100101` 的 `82` 条里 `79` 条不一致。
- 根因结论：
  - 不是薪资导出 Excel 列映射错误。
  - 是 2026-06 社保月记录在旧方案项目金额已等于工资表扣款口径的基础上，又叠加了长期护理固定金额 `3`；薪资核算和导出只是把这个已偏高的社保月金额继续带出。


## 2026-07-18 Pre-work
- 已读取后端项目 `docs/requirements.md` 与 `docs/development.md` 中社保方案、员工参保和同名员工匹配相关规则。
- 与本次任务直接相关的既有约束：
  - 社保方案管理中的个人社保合计需在社保项目个人社保合计基础上加长期护理保险金额，公司社保合计需在社保项目公司社保合计基础上加大额医疗保险金额。
  - 社保管理生成次月报表、手工修改员工月度参保项目后回写员工月度社保合计时，必须复用同一固定金额逻辑。
  - 员工管理批量设置参保方案统一提交 `employeeIds + schemeId` 到 `/hrmEmployee/updateInsuranceScheme`，后端按员工 ID 更新并在事务中处理。
  - 涉及同名员工时不得只按姓名匹配；本次按用户指定使用“姓名 + 手机号”作为外部数据唯一键，最终更新员工表仍以 `employee_id` 为准。
- 初始待验证点：
  - `/Users/jiangyongming/Desktop/农谷导入数据` 中哪些文件包含社保/医保方案、方案明细和员工参保关系。
  - 系统实际表名、字段和状态值：社保方案主表、方案项目明细表、员工表中的参保方案字段。
  - 当前库是否已有同名/同金额方案可复用，避免重复插入同一方案。
  - 源文件中是否存在姓名或手机号空值、姓名+手机号重复、姓名+手机号无法命中员工、同键命中多个未删除员工等阻断项。

## 2026-07-18 Extraction and Matching
- 目标库确认：
  - `hrsystem.tbcompanylist` 中 `CompanyID=0003` 对应 `database=hr_0003`。
  - `hr_0003` 当前在职员工 `113` 人，导入前员工社保信息 `96` 行。
- 系统表结构：
  - 方案主表：`hrm_insurance_scheme(scheme_id, scheme_name, city, level, create_user_id, create_time, update_user_id, update_time)`。
  - 方案项目表：`hrm_insurance_project(project_id, scheme_id, type, default_amount, corporate_proportion, personal_proportion, corporate_amount, personal_amount, ...)`。
  - 员工参保关系：`hrm_employee_social_security_info(employee_id, scheme_id, ...)`。
- 源文件：
  - 社保/医保：`副本2026年社保医保缴费明细（田野农谷分部门）.et`，本轮使用 `2026年7月89人（增1减1）`。
  - 公积金：`副本2026年公积金缴费清单 (田野农谷分部门).xlsx`，本轮使用 `2026年7月89人调基数（增1减1）`。
  - `.et` 能通过 `xlrd` 直接读取；社保表无手机号，公积金表有身份证号和手机号。
- 合并规则：
  - 社保/医保行先按 `月份 + 姓名` 关联公积金手机号；同名多行时再使用两张表的部门上下文辅助关联。
  - 两名王芳通过部门上下文补手机号后，最终仍按 `姓名 + 手机号` 命中员工：财务王芳 `13872932293`、生产王芳 `13032750052`。
  - 大额医疗 `15` 和长期护理 `3` 使用系统 `hrm_salary_basic` 固定金额规则，不重复写入 `hrm_insurance_project`。
- 预览结果：
  - 7 月社保/医保源行 `89`，公积金源行 `89`。
  - 补手机号后源唯一键重复 `0`。
  - 按姓名 + 手机号命中在职员工 `85`，未命中 `4`。
  - 当前 7 月唯一方案组合 `5`：其中 `社保(672.50) 公积金(320)` 精准复用现有方案，其余 4 个新增。
- 未自动更新异常：
  - 朱君明 `13972908908`：在职员工表无姓名+手机号匹配。
  - 宁明友 `17671851617`：员工表当前手机号为 `17855591722`。
  - 陈爱蓉 `15572492026`：员工表当前手机号为 `15872915345`。
  - 瞿顺清 `13098848611`：员工表当前手机号为 `13597935193`。

## 2026-07-18 Execution and Verification
- 已生成预览报告：`docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview.md`。
- 已生成执行 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import.sql`。
- 已生成回滚 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_rollback.sql`。
- 已执行执行 SQL 到 `hr_0003`，事务返回：
  - `inserted_scheme_count=4`；
  - `updated_or_inserted_employee_count=85`。
- 新增方案：
  - `2078479646634151936`：`社保(672.50) 公积金(560.00)`；
  - `2078479646634151937`：`社保(466.50) 公积金(158.00)`；
  - `2078479646634151938`：`社保(672.50) 公积金(158.00)`；
  - `2078479646634151939`：`社保(445.80) 公积金(158.00)`。
- 写后方案金额核验：
  - 新增/复用的 5 个 7 月方案均有 6 条项目明细；
  - 系统列表口径展示金额与源表一致，包括长期护理/大额医疗固定金额后，社保个人合计分别为 `445.80 / 466.50 / 672.50`。
- 写后员工核验：
  - 用同一解析逻辑反查 85 个命中员工，实际 `scheme_id` 全部等于预期，错配 `0`；
  - 员工社保信息从 `96` 行增加到 `105` 行，新增 `9` 行，符合预览；
  - 在职员工分布：`社保(466.50) 公积金(158.00)` 75 人，`社保(445.80) 公积金(158.00)` 5 人，`社保(672.50) 公积金(320)` 5 人，`社保(672.50) 公积金(158.00)` 1 人，另有未覆盖或历史旧方案员工保留原状态。

## 2026-07-18 Follow-up: 2026-06 工资表临时口径
- 用户明确要求暂时先按 `/Users/jiangyongming/Desktop/农谷导入数据/副本加个税版-田野农谷2026年6月工资表(1)(1).xlsx` 处理。
- 已确认该工作簿包含 `3月`、`4月 `、`5月 `、`1月`、`2月`、`3月份`、`4月`、`5月`、`6月` 等 sheet；本轮只读取 `6月`。
- `6月` sheet 列位：
  - 第 3 列：姓名；
  - 第 6 列：部门；
  - 第 7 列：岗位；
  - 第 22 列：社保；
  - 第 23 列：公积金。
- 解析结果：
  - 工资表员工行 `96`，唯一姓名 `94`；
  - 金额组合为 `0+0` 8 行、`445.80+144` 4 行、`466.50+144` 77 行、`672.50+144` 1 行、`672.50+320` 6 行；
  - 非零参保行 `88`，可自动唯一定位 `87`。
- 方案处理：
  - 复用现有精确方案 `2078034513505673217 / 社保(672.50) 公积金(320)`；
  - 新增 6 月精确方案 `2078479646634151940 / 社保(466.50) 公积金(144.00)`；
  - 新增 6 月精确方案 `2078479646634151941 / 社保(445.80) 公积金(144.00)`；
  - 新增 6 月精确方案 `2078479646634151942 / 社保(672.50) 公积金(144.00)`。
- 三名原异常员工复核：
  - 宁明友在员工表中姓名唯一，员工表手机号 `17855591722`，6 月工资表金额 `672.50+320`，已更新为 `2078034513505673217`；
  - 陈爱蓉在员工表中姓名唯一，员工表手机号 `15872915345`，6 月工资表金额 `466.50+144`，已更新为 `2078479646634151940`；
  - 瞿顺清在员工表中姓名唯一，员工表手机号 `13597935193`，6 月工资表金额 `466.50+144`，已更新为 `2078479646634151940`。
- 执行与核验：
  - 已生成并执行 `docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary.sql`；
  - 回滚脚本为 `docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary_rollback.sql`；
  - 预览与执行报告为 `docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview-2026-06-salary.md`；
  - MySQL 返回 `inserted_or_existing_scheme_count=3`、`touched_employee_count=83`；
  - 4 个目标方案系统展示金额与工资表一致；
  - 83 个触达员工按执行 SQL 反查 `scheme_id` 错配为 `0`。
- 仍未自动更新：
  - 马国华：同名在职候选 `1831601326890434624/13477584601/TYNG-278` 与 `2033769831940079628/13451187963/无工号` 均可能匹配，工资表无手机号；
  - 谢杰杰：两名同名在职候选部门/岗位相同，工资表金额为 `0+0`，未自动清空；
  - 程传祥：两名同名在职候选无法按工资表唯一定位，工资表金额为 `0+0`，未自动清空。

# Findings: 李明明六月工会费和个税未生成排查

## 2026-07-17 Pre-work
- 已读取项目 `docs/requirements.md` 与 `docs/development.md`。
- 与本次问题直接相关的既有规则：
  - 个税累计、附加累计、年度附加扣除导入不得只按姓名匹配；优先按姓名 + 手机号，旧模板缺手机号时仅姓名唯一才兼容。
  - 批量薪资核算会通过 `SalaryComputeServiceNew#computeSalary(...)` 生成个税、工会费等工资项，并由 `saveTaxAccumulationData(...)` 写回 `hrm_personal_income_tax(employee_id, year, end_month)`。
  - 备注员工个税累计减除费用为 `60000`，普通员工按 `5000 * 计薪月份`；12 月不重置个税累计，1 月重新开始本年度累计。
  - 文档中已有李明明 `hr_0003 / employee_id=1831601326890434563 / 2026-06` 的全勤奖修复记录，本轮问题独立聚焦工会费和个税未生成。
- 初始待验证点：
  - 用户提供整理文件 `专项扣除累加表_2026.xlsx` 是否包含李明明的年度专项附加扣除、手机号和匹配身份。
  - 原始综合所得申报文件中李明明 2026-06 的累计收入、累计扣除、已缴税额、当月应补退税等字段是否与期望个税 `2.4` 对齐。
  - 六月薪资核算记录中是否缺少工会费/个税工资项，还是生成后被后续删除/覆盖。
  - 工会费 `29.25` 的基数是否为应发工资或税前收入的 `0.5%`，以及李明明所在公司是否误命中了免工会费分支。

## 2026-07-17 Evidence
- 员工身份：
  - 在职李明明：`employee_id=1831601326890434563`，手机号 `17389819211`，工号 `1718539734976`，`is_del=0`，`is_remark=2`。
  - 已删除同名李明明：`employee_id=2033769831940079620`，手机号 `18889692758`，`is_del=1`。
- 最新 `2026-06` 薪资记录：
  - 主记录 `s_record_id=2077807285177995265`，员工薪资记录 `s_emp_record_id=2077928900758896641`。
  - 工资项当前存在重复计算项：`160102 工会费` 有 `0` 与 `29.25` 两行；`1001 代扣小计` 有 `0` 与 `1021.75` 两行；`41001 绩效奖金` 有两行 `0`。
  - 真实工会费 `29.25 = 5850.00 * 0.5%` 已被计算出来，但默认 `0` 行可能在列表/导出读取时遮住真实值。
  - 个税工资项当前为：`250101/250102/250103/250105 = 0/0/0/0`，`270102=60000`，`270104=0`，`230101=0.00`。
- 用户提供整理文件：
  - `个税累计.xls` 中李明明行使用手机号 `17389819211`，`2026-05` 累计为收入 `58594`、减除费用 `25000`、专项扣除 `4344.5`、已缴税额 `213.99`。
  - `附加扣除累计.xls` 中李明明行使用手机号 `17389819211`，`2026-05` 累计专项附加扣除为子女 `10000`、住房租金 `4000`、赡养老人 `7500`，合计 `21500`。
  - `专项扣除累加表_2026.xlsx` 中李明明行使用手机号 `17389819211`，年度月配置为子女 `2000`、住房租金 `800`、赡养老人 `1500`。
- 数据库现状与整理文件不一致：
  - `hrm_personal_income_tax` 的李明明 `2026-05` 个税累计挂在已删除员工 `2033769831940079620` 上。
  - `hrm_additional` 的李明明 `2026-05/06/07` 附加累计均挂在已删除员工 `2033769831940079620` 上。
  - `hrm_employee_additional` 的李明明 `2026` 年度配置也挂在已删除员工 `2033769831940079620` 上。
- 原始申报文件：
  - `2026-06` 原始申报中李明明累计收入 `69859`、累计减除费用 `30000`、累计专项扣除 `5749`、累计专项附加扣除 `25800`、应纳税额 `249.30`、已缴税额 `232.49`、应补/退税额 `16.81`。
  - 因此用户给出的六月个税 `2.4` 不是原始申报表直接值；它需要结合当前系统六月薪资修正后的基数继续确认。

## 2026-07-17 Root Cause
- 工会费“没出来”的直接原因是非固定工资项初始化预插入了计算项默认 `0`，随后真实计算又插入 `29.25`，同一员工同一工资项出现重复行；读取第一条时会显示 `0`。
- 个税“没出来”至少有两个已证实原因：
  - 上月个税累计、当月专项附加扣除和年度专项附加配置错挂到已删除同名员工，导致在职李明明核算时读不到 `2026-05` 累计基础和 `2026-06` 专项附加扣除。
  - 在职李明明 `is_remark=2`，旧代码在已有上月导入累计减除费用 `25000` 的情况下仍强行把 `270102` 写为 `60000`，与原始 `2026-06` 申报表的累计减除费用 `30000` 冲突，并直接把本月个税压成 `0`。

## 2026-07-17 Implementation
- 已修复非固定项过滤：`SalaryMonthRecordServiceNew#filterNoFixedSalaryOptions(...)` 排除 `1001/160102/210101/220101/230101/240101/250101/250102/250103/250105/270101~270106/41001` 等计算汇总项，避免初始化阶段生成默认 `0` 行。
- 已修复累计减除费用口径：`SalaryComputeServiceNew#resolveCumulativeDeductions(...)` 在上月累计减除费用已导入且大于 `0` 时，优先按 `上月累计 + 5000` 续算并封顶 `60000`；`is_remark=2` 只在缺少可靠导入累计时作为 `60000` 兜底。
- 该修复保留原有 1 月重新开始累计、无备注按月份 `5000 * month`、已导入 `60000` 时不再继续增加的行为。

## 2026-07-17 Follow-up Verification
- 数据修复脚本：`docs/sql/2026-07-17_hr_0003_li_mingming_tax_data_repair.sql`，默认 `ROLLBACK`，用于删除在职李明明错误生成的 `2026-06` 个税累计，并将 `2026-05` 个税累计、`2026-05/06/07` 附加累计、`2026` 年度附加配置从已删除同名员工迁回在职员工。
- 用户目录只读扫描结果：
  - `final_import_excels/个税累计.xls` 中李明明 `2026-05` 上月累计为 `58594 / 25000 / 4344.5 / 213.99`；
  - `final_import_excels/附加扣除累计.xls` 中李明明 `2026-05` 附加累计合计 `21500`；
  - `湖北田野农谷生物科技有限公司_综合所得申报_202606.xls` 原始申报中李明明为累计收入 `69859`、累计减除费用 `30000`、累计专项扣除 `5749`、累计专项附加扣除 `25800`；
  - 未在用户给出的 Excel/CSV/说明文件中发现 `2.4/2.40/21894` 作为源字段。
- 按当前已确认输入复算，修复后代码会把 `270102` 从旧错误的 `60000` 改为 `25000 + 5000 = 30000`，并可生成工会费 `29.25`；但用户期望个税 `2.4` 需要额外确认专项附加扣除累计口径，当前提供的最终导入基础是 `21500`，原始 6 月申报是 `25800`，均不是推导 `2.40` 所需的 `21894`。
- 收尾时尝试直连 `153.0.237.98/hr_0003` 三次复核数据修复和薪资项状态，均被 `ERROR 1040 (HY000): Too many connections` 阻断；未清理数据库连接，也未停止用户正在运行的 IDE 后端。

## 2026-07-17 Follow-up: 14.22 vs 2.40 Difference
- 当前库中李明明 `2026-06` 薪资已生成：
  - `210101 应发工资=5850.00`；
  - `100101 个人社保=672.50`；
  - `100102 个人公积金=320`；
  - `230101 个人所得税=14.22`；
  - `270101/270102/270103/270104/270105/270106 = 64444 / 30000 / 5337 / 21500 / 7607 / 228.21`。
- 系统 `14.22` 复算成立：
  - 累计收入 `58594 + 5850 = 64444`；
  - 累计减除费用 `25000 + 5000 = 30000`；
  - 累计专项扣除 `4344.5 + 672.5 + 320 = 5337`；
  - 累计专项附加扣除 `21500`；
  - 累计应纳税所得额 `64444 - 30000 - 5337 - 21500 = 7607`；
  - 累计税额 `7607 * 3% = 228.21`；
  - 本月个税 `228.21 - 213.99 = 14.22`。
- 员工手算 `2.40` 的倒推条件：
  - 目标累计税额 `213.99 + 2.40 = 216.39`；
  - 目标应纳税所得额 `216.39 / 3% = 7213`；
  - 与系统 `7607` 相比，需要多扣除 `394.00`；
  - 若专项附加不变，则累计专项扣除需为 `5731.00`，即本月个人社保+公积金需为 `1386.50`，不是系统当前的 `992.50`。
- 当前 6 月社保数据来源：
  - 薪资配置 `social_security_month_type=1`，薪资核算取当月社保；
  - 李明明 6 月社保员工明细状态 `status=1`，主记录 `6月社保报表` 状态 `0`；
  - 6 月社保明细为基数 `6500`：养老 `520`、医疗 `130`、失业 `19.50`，加长期护理 `3` 后个人社保 `672.50`；公积金基数 `4000`，个人公积金 `320`。
- 文件对比：
  - 原始 `2026-06` 综合所得申报表中李明明本月专项扣除为养老 `840`、医疗 `213`、失业 `31.5`、公积金 `320`，合计 `1404.50`，累计专项扣除 `5749.00`；
  - 但按当前系统收入 `64444`、专项附加 `21500`、原始申报专项扣除 `5749` 复算，本月个税为 `1.86`，仍不是 `2.40`；
  - 桌面范围内精确扫描未发现 `2.40 / 5731.00 / 1386.50 / 21894.00 / 216.39` 的手算来源文件。
- 结论：`14.22` 与 `2.40` 的差异不是个税税率公式或累计减除费用逻辑问题，而是本月专项扣除输入不一致。系统当前按 6 月社保员工明细 `672.50 + 320` 算；`2.40` 需要个人社保+公积金达到 `1386.50`，该数值目前没有在系统社保数据、用户提供的最终导入文件或桌面可扫描文件中找到直接来源。

## 2026-07-17 Follow-up: Original 202605/202606 Tables Only
- 用户确认 `/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报` 中 `202605`、`202606` 两个原始申报表代表财务做的最详细数据。
- 已只读抽取两张表 `个人所得税扣缴申报表` 中李明明行：
  - `202605`：本月收入 `11115.00`，本月专项扣除 `680+173+25.5+320=1198.50`，累计收入 `58594.00`，累计减除费用 `25000.00`，累计专项扣除 `4344.50`，累计专项附加扣除 `21500.00`，应纳税所得额 `7749.50`，应纳税额 `232.49`，已缴税额 `213.99`，本月应补/退税额 `18.50`。
  - `202606`：本月收入 `11265.00`，本月专项扣除 `840+213+31.5+320=1404.50`，累计收入 `69859.00`，累计减除费用 `30000.00`，累计专项扣除 `5749.00`，累计专项附加扣除 `25800.00`，应纳税所得额 `8310.00`，应纳税额 `249.30`，已缴税额 `232.49`，本月应补/退税额 `16.81`。
- 按申报表自身累计预扣公式复算：
  - `202605`: `58594 - 25000 - 4344.5 - 21500 = 7749.50`，`7749.50 * 3% = 232.49`，`232.49 - 213.99 = 18.50`。
  - `202606`: `69859 - 30000 - 5749 - 25800 = 8310.00`，`8310.00 * 3% = 249.30`，`249.30 - 232.49 = 16.81`。
- 枚举验证：
  - 在不违反累计预扣公式的前提下，用两张表中的累计收入、累计减除费用、累计专项扣除、累计专项附加扣除、已缴税额/上月应纳税额等合理角色组合进行枚举，精确得到 `2.40` 的组合数量为 `0`。
  - 若以上月应纳税额 `232.49` 作为已缴税额，要得到 `2.40`，目标累计应纳税所得额需约 `7829.67`；原始 `202606` 表中是 `8310.00`。
  - 从月度增量看，财务原表 6 月新增应纳税所得额为 `8310.00 - 7749.50 = 560.50`，产生税额 `560.50 * 3% = 16.815 -> 16.81`；`2.40` 只对应约 `80.00` 的新增应纳税所得额，两张原始表没有这一口径。
- 结论：仅使用 `202605` 和 `202606` 两张财务原始申报表中的李明明原始数据，不能计算出李明明 6 月个税 `2.40`；严格表内公式结果为 `16.81`。

## 2026-07-18 Follow-up: Performance Salary Scenarios
- 用户补充李明明可能存在两个绩效工资，要求分别按 `4700`、`7000`、`4700+7000=11700` 计算。
- 按当前系统口径计算：上月累计收入 `58594`，本月基础应发 `5850`，累计减除费用 `30000`，累计专项扣除 `5337`，累计专项附加扣除 `21500`，已缴税额 `213.99`，绩效工资作为本月收入增加：
  - 绩效 `4700`：累计收入 `69144`，应纳税所得额 `12307`，累计税额 `369.21`，本月个税 `155.22`。
  - 绩效 `7000`：累计收入 `71444`，应纳税所得额 `14607`，累计税额 `438.21`，本月个税 `224.22`。
  - 绩效 `11700`：累计收入 `76144`，应纳税所得额 `19307`，累计税额 `579.21`，本月个税 `365.22`。
- 按财务 `202606` 原始申报表口径计算：原表累计收入 `69859`，累计减除费用 `30000`，累计专项扣除 `5749`，累计专项附加扣除 `25800`，已缴税额 `232.49`，在原表累计收入基础上额外加入绩效：
  - 绩效 `4700`：累计收入 `74559`，应纳税所得额 `13010`，累计税额 `390.30`，本月个税 `157.81`。
  - 绩效 `7000`：累计收入 `76859`，应纳税所得额 `15310`，累计税额 `459.30`，本月个税 `226.81`。
  - 绩效 `11700`：累计收入 `81559`，应纳税所得额 `20010`，累计税额 `600.30`，本月个税 `367.81`。
- 结论：无论按当前系统口径还是按财务 `202606` 原表口径，加入 `4700`、`7000` 或两者合计 `11700` 的绩效工资后，李明明 6 月个税都会显著高于 `2.40`，绩效工资不是解释 `2.40` 的来源。

## 2026-07-20 Follow-up: Original 202607 Table
- 用户新增财务原始表：`/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报/202607_税款计算_工资薪金所得.xls`。
- 该文件工作表为 `综合所得申报税款计算`，李明明行：
  - 本期收入 `5850.00`；
  - 本期基本养老保险费 `520.00`、基本医疗保险费 `133.00`、失业保险费 `19.50`、住房公积金 `320.00`，本期专项扣除合计 `992.50`；
  - 累计收入额 `75709.00`；
  - 累计减除费用 `35000.00`；
  - 累计专项扣除 `6741.50`；
  - 累计专项附加扣除：子女教育 `14000.00`、住房租金 `5600.00`、赡养老人 `10500.00`，合计 `30100.00`；
  - 累计应纳税所得额 `3867.50`；
  - 累计应纳税额 `116.03`；
  - 已缴税额 `249.30`；
  - 应补(退)税额 `0.00`。
- 复算：`75709 - 35000 - 6741.5 - 30100 = 3867.5`，`3867.5 * 3% = 116.03`，`116.03 - 249.30 = -133.27`。因本期不倒退扣税，表内应补(退)税额为 `0.00`。
- 结论：按新增 `202607` 财务原始表，李明明 7 月个税应扣为 `0.00`；该表的累计税额低于已缴税额，说明 7 月不再新增扣缴。

## 2026-07-20 Final Import Data: 李明明六月个税 2.40
- 用户确认不再继续追溯 `246.90/249.30` 的原始来源；两者按财务确认的正确值直接使用。
- 系统当前个税工资项公式为：`230101 本月个税 = 270106 累计应纳税额 - 250105 截至上月累计已缴税额`，负数按 `0` 处理。
- 因此，要让系统在李明明 2026-06 薪资中算出 `2.40`，必须让系统本次累计应纳税额为 `249.30`，并导入截至上月累计已缴税额 `246.90`。
- 按财务确认“六月工资个税按 7 月申报”的减除费用口径，导入支持值整理为：
  - 个税累计导入年月选 `2026-05`，李明明行：累计收入 `69859.00`、累计减除费用 `30000.00`、累计公积金社保扣除 `5749.00`、累计已缴税额 `246.90`。
  - 附加累计导入年月选 `2026-06`，李明明行：累计子女教育 `12000.00`、累计住房租金 `4800.00`、累计住房贷款利息 `0.00`、累计赡养老人 `8857.50`、累计继续教育 `0.00`、累计养幼女 `0.00`，合计 `25657.50`。
- 复算结果：`69859 + 5850 = 75709`，`30000 + 5000 = 35000`，`5749 + 992.50 = 6741.50`，`75709 - 35000 - 6741.50 - 25657.50 = 8310.00`，`8310 * 3% = 249.30`，`249.30 - 246.90 = 2.40`。
- 已生成全量导入副本，未覆盖原文件：
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/李明明6月个税2.40_个税累计_导入2026-05.xls`
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/李明明6月个税2.40_附加扣除累计_导入2026-06.xls`
- 注意：若把 `202607` 原始表中的专项附加扣除合计 `30100.00` 原样导入，系统现有公式会得到累计应纳税额 `116.03`，本月个税会被压成 `0.00`，不会得到 `2.40`。
- 实际导入与重算：
  - 首次个税累计导入因源数据包含已删除员工 `郑景/杜畅/万怀进` 被当前导入匹配校验拦截；已重新生成个税累计文件，过滤这 3 条已删除员工，保留 97 条当前可匹配员工。
  - 已通过本机 `9080 / hr_0003` 接口导入个税累计 `2026-05` 与附加累计 `2026-06`。
  - 只重算李明明 `employee_id=1831601326890434563` 的 `2026-06` 薪资；最终重算参数必须使用 `isSyncInsuranceData=true`，否则本月社保/公积金会被置 `0`，无法得到目标值。
  - 最终薪资项复核：`210101=5850.00`、`100101=672.50`、`100102=320`、`160102=29.25`、`230101=2.40`、`270101=75709.00`、`270102=35000.00`、`270103=6741.50`、`270104=25657.50`、`270105=8310.00`、`270106=249.30`。
  - 个税累计表回写复核：李明明 `2026-05` 为 `69859/30000/5749/246.90`，`2026-06` 为 `75709/35000/6741.50/249.30`。

# Findings: 基本工资社保固定金额配置

## 2026-07-17 Pre-work
- 已读取后端项目 `docs/requirements.md` 与 `docs/development.md`。
- 既有相关规则：
  - 基本工资金额设置已承载普通员工全勤金额、领导全勤金额、生产体系月休天数，并按最新一条记录生效。
  - 旧数据或缺字段时已有默认值兜底模式，例如全勤金额 `100/500`、生产月休 `4`。
  - 社保报表生成已支持真实进度查询，生成链路位于 `HrmInsuranceMonthRecordService#computeInsuranceData()`。
- 本轮新增规则尚未写入文档：基本工资金额设置需新增大额医疗保险金额和长期护理保险金额；社保方案合计和次月社保报表生成需按固定金额并入合计。
- 初始代码定位：
  - 基本工资设置实体/DTO/VO/服务：`HrmSalaryBasic`、`QuerySalaryBasicDto`、`QuerySalaryBasicVO`、`HrmSalaryBasicService`、`HrmSalaryBasicDefaults`。
  - 社保方案合计 SQL：`HrmInsuranceSchemeMapper.xml` / `HrmInsuranceSechemeMapper.xml` 中的 `InsuranceSchemeCount`。
  - 社保报表生成入口：`HrmInsuranceMonthRecordService#computeInsuranceData()`。

## 2026-07-17 Implementation
- 基本工资设置新增两项字段：
  - `largeMedicalInsuranceAmount` -> `hrm_salary_basic.large_medical_insurance_amount`，默认 `15`；
  - `longTermCareInsuranceAmount` -> `hrm_salary_basic.long_term_care_insurance_amount`，默认 `3`。
- `HrmSalaryBasicDefaults` 统一补齐新默认值；保存、`findAll()`、`queryById(...)` 均复用默认值兜底。
- 社保方案管理：
  - `HrmInsuranceSchemeMapper.xml` 与 `HrmInsuranceSechemeMapper.xml` 的 `personal_insurance_amount` 改为原项目个人社保合计 + 最新基本工资设置长期护理金额；
  - `corporate_insurance_amount` 改为原项目公司社保合计 + 最新基本工资设置大额医疗金额；
  - 若无基本工资设置或字段为空，分别兜底 `3/15`。
- 社保管理生成次月报表：
  - `HrmInsuranceMonthRecordService#computeInsuranceData()` 原本通过 `queryInsuranceSchemeCountById(...)` 生成员工月度社保记录；本轮修改该 mapper 合计 SQL 后，次月报表员工记录自动写入包含长期护理/大额医疗后的合计。
- 社保月详情手工改员工参保项目：
  - `HrmInsuranceMonthEmpProjectRecordMapper.xml#queryProjectCount` 同步加入长期护理/大额医疗，避免手工修改项目后回写员工月记录合计时丢失固定金额。
- 前端：
  - `hr_web/src/views/manage/salary/Index.vue` 新增“大额医疗保险金额”“长期护理保险金额”两个 `el-input-number` 表单项；
  - 默认值分别为 `15/3`，加载旧接口数据缺字段时由 `DEFAULT_SALARY_BASIC` 补齐；
  - 两项进入 Element Plus 必填校验，并随原 `saveSalaryBasic(state.ruleForm)` 提交。
- SQL：
  - 新增 `docs/sql/2026-07-17_hrm_salary_basic_insurance_amount_settings.sql`，为 `hr_0001` 至 `hr_0005` 幂等添加两个字段。

## 2026-07-17 Verification
- RED 后端：新增测试先失败于基本工资设置 DTO/VO/实体缺少 `largeMedicalInsuranceAmount`、`longTermCareInsuranceAmount`。
- RED 前端：页面测试先失败于缺少“大额医疗保险金额”表单项。
- GREEN/回归：
  - 后端 `HrmSalaryBasicServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest` 通过，13 个测试；
  - 前端 `salary-basic-settings-page`、社保新建次月报表、进度工具、高级筛选测试均通过；
  - 前端 `npm run build` 通过，保留既有构建警告。

# Findings: 李明明薪资全勤奖未计入排查

## 2026-07-17 Pre-work
- 已读取项目 `docs/requirements.md` 与 `docs/development.md` 的薪资/考勤相关规则。
- 与本次问题直接相关的既有规则：
  - 基本工资金额设置持久化普通员工全勤金额、领导全勤金额、生产体系月休天数；薪资核算全勤奖读取最新配置，缺失时分别按 `100/500/4` 兜底。
  - 薪资核算中原“实际出勤天数”的计薪字段已改为“应计出勤天数”口径：优先 `hrm_produce_attendance.probation_attendance`，为空或无汇总行时兜底 `hrm_overtime_night_statistics_detail.accrued_attendance_hours / 8`。
  - 文档已有李明明 `hr_0003`、`2026-06` 校准记录：应出勤 `184` 小时、实际出勤 `160` 小时、应计出勤 `184` 小时。
  - 员工匹配不得只按姓名；李明明存在历史同名已删除员工，排查必须用员工 ID、手机号、钉钉 userId 区分。
- 初始假设：若李明明 `2026-06` 应计出勤等于应出勤但实际出勤少 24 小时，全勤奖是否发放取决于薪资核算代码使用的是应计出勤满勤，还是仍使用实际出勤/请假扣减作为全勤奖条件。

## 2026-07-17 Evidence
- 李明明身份确认：
  - 租户：`hr_0003`；
  - 员工 ID：`1831601326890434563`；
  - 工号：`1718539734976`；
  - 手机：`17389819211`；
  - 部门：行政人力资源部；
  - 岗位：人力资源经理；
  - 状态：正式员工 `status=1`，转正时间 `2025-07-01`，全勤标识 `full_attendance=1`。
- 计薪员工查询返回 `fullMoney=500.00`，说明领导全勤金额配置命中；未发全勤不是金额配置缺失，也不是员工档案“不享有全勤”。
- 最新薪资主记录：`sRecordId=2077807285177995265`，标题 `六月薪资报表`，年月 `2026-06`。
- 李明明该薪资行：
  - `actualWorkDay=23.00`，`needWorkDay=21.75`；
  - 工资项中没有 `40102 全勤奖`；
  - `210101 应发工资=5350.00`，等于 `10101 基本工资 2130 + 10102 岗位工资 3220`，确实未加 `500` 全勤奖。
- 考勤汇总 `hrmProduceAttendance/queryMonthAttendanceList`：
  - `actualAttendance=20.00`；
  - `accruedAttendance=23.00`；
  - `workOverTime=8.00`；
  - `overtimePay=96.00`。
- 加班/夜班统计：
  - 应出勤 `23` 天；
  - 实际出勤 `160.00` 小时 / `20` 天；
  - 应计出勤 `184.00` 小时；
  - 备注 `扣除：年假32.00小时`；
  - 明细只有 `2026-06-06` 和 `2026-06-07` 两条加班各 `4` 小时。
- 本地审批明细：
  - 年假 `2026-06-14` 至 `2026-06-18`，`4` 天；
  - 加班 `2026-06-06 08:00-12:00` 与 `2026-06-07 08:00-12:00`；
  - 出差两段；
  - 未通过审批列表发现事假/病假。
- 打卡概况月度接口返回李明明 `attendDays=0`、`actualDays=0`、`isFullAttendance=null`，说明当前打卡概况接口未能从 `hrm_attendance_report_data` 给出可用的李明明月度汇总。

## 2026-07-17 Root Cause
- `SalaryMonthRecordServiceNew#fillAttendanceDataForEmployee(...)` 发全勤奖的必要条件之一是局部变量 `isFullAttendance=true`。
- 该变量只在 `checkIsFullAttendance(employeeSummaryDayList, empAttendanceSummary, ...)` 返回 true 时置真；如果 `empAttendanceSummary == null`，`checkIsFullAttendance(...)` 直接返回 false。
- 即使后续 `resolveSalaryAttendanceDays(...)` 从考勤汇总/加班夜班统计解析出李明明 `23.00` 天应计出勤，并写入 `actualWorkDay=23.00`，当前代码也不会在 `empAttendanceSummary == null` 或报表汇总不可用的分支重新判定满勤。
- 因此李明明本次没有加全勤奖的直接原因是：薪资核算全勤奖仍依赖钉钉报表汇总满勤判断；当前可见的考勤汇总/加班夜班统计已满足应计出勤口径，但薪资链路没有据此补发 `40102`。
- 这更像薪资全勤判断口径未随“应计出勤天数优先考勤汇总/加班夜班统计”的新规则一起收敛，而不是李明明员工档案或全勤金额配置错误。

## 2026-07-17 Implementation
- 业务口径已明确：李明明这类场景中，薪资应计出勤天数达到加班/夜班统计应出勤天数，且员工正式/已转正、启用全勤奖时，旧钉钉报表月度汇总缺失不应阻止 `40102 全勤奖`。
- `SalaryMonthRecordServiceNew.AttendanceSyncBatchData` 现在从同一批 `hrm_overtime_night_statistics_detail` 明细中缓存：
  - `accruedAttendanceHours / 8` 得到的应计出勤天数；
  - `expectedAttendanceDays` 得到的加班/夜班统计应出勤天数。
- `fillAttendanceDataForEmployee(...)` 在 `checkIsFullAttendance(...)` 返回 false 后增加兜底：仅当 `empAttendanceSummary == null` 且薪资应计出勤天数 `>=` 加班/夜班统计应出勤天数时，才将 `isFullAttendance` 置为 true。
- 该兜底不会覆盖已有报表异常判断：如果 `empAttendanceSummary` 存在，仍由原逻辑识别迟到超过 30 分钟、缺卡、旷工、事假、病假、早退等扣全勤条件。
- 本轮只改核算代码，不直接更新历史薪资数据；李明明 2026-06 需要在部署后重新核算目标薪资记录，才会在已生成薪资中补出 `40102`。

## 2026-07-17 Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 先失败于 `buildExpectedAttendanceDaysByEmployee(...)`、`resolveFullAttendanceExpectedDays(...)`、`shouldFallbackFullAttendanceByAccruedDays(...)` 缺失。
- GREEN：同一命令通过，38 个测试。
- 薪资回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，57 个测试。
- 上游考勤汇总回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，9 个测试。

# Findings: 薪资核算硬编码排查

## 2026-07-17 Pre-work
- 已读取项目 `docs/requirements.md` 与 `docs/development.md`。
- 文档中与薪资核算硬编码直接相关的既有规则：
  - 基本工资金额设置已经承载普通员工全勤金额、领导全勤金额、生产体系月休天数；缺失时允许兜底 `100/500/4`。
  - 薪资核算个税已确认固定月度基本减除费用 `5000` 与全年备注减除费用 `60000`。
  - 薪资核算中“实际出勤天数”业务展示口径已调整为“应计出勤天数”，字段名保持兼容。
  - 员工匹配、导入、审批统计不得只按姓名，需使用员工 ID、工号、手机号、部门、钉钉 userId 等唯一信息。
- 本轮排查目标是识别仍散落在薪资核算代码中的硬编码，尤其是公司 ID、工资项编码、金额/天数/小时换算、岗位名称关键字、年月边界、中文状态值等。

## 2026-07-17 Initial Code Discovery
- 薪资核算主链路候选文件：
  - `src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryMonthRecordController.java`
  - `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`
  - `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNew.java`
  - `src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeContext.java`
  - `src/main/resources/mapper/HrmSalaryMonthEmpRecordMapper.xml`
  - `src/main/resources/mapper/HrmSalaryMonthRecordMapper.xml`
  - `src/main/resources/mapper/HrmSalaryMonthOptionValueMapper.xml`
- 第一轮粗扫出的硬编码候选：
  - 公司/租户分支：`companyId=0002`。
  - 工资项编码：`1/2/3/11/120101/230101/250101...270106` 等。
  - 个税口径：月度减除费用 `5000`、全年备注减除费用 `60000`、个税税率表档位。
  - 工时换算：`8` 小时/天。
  - 核算范围限制：一次显式选择员工 `50` 人。
  - 基本工资配置默认值：普通全勤 `100`、领导全勤 `500`、生产月休 `4`。
  - 部门/岗位关键字：生产相关部门清单、领导/经理类岗位判断。
  - 审核/完成状态：薪资月记录状态数字、社保月记录状态数字。

## 2026-07-17 Confirmed Main-chain Findings
- `SalaryComputeServiceNew`：
  - `MONTHLY_TAX_FREE_DEDUCTION=5000`、`ANNUAL_REMARK_TAX_FREE_DEDUCTION=60000` 是个税规则硬编码，但已在需求文档中明确为当前业务口径。
  - `companyId=0002` 控制奖金是否进入累计收入；`companyId=0002/0005` 控制是否免工会费，属于公司特殊分支硬编码。
  - `parentCode`/`code` 裸值大量参与应发、代扣、专项附加、奖金、个税、实发工资和删除重算：`100/150/170/260/280/281/282/1001/160102/210101/220101/230101/240101/250101/250102/250103/250105/270101~270106/41001`。
  - 工会费固定为应发工资 `0.5%`；正式/实习/离职/半路转正规则依赖员工状态数字 `1/3/4`。
- `SalaryMonthRecordServiceNew`：
  - `MAX_COMPUTE_EMPLOYEE_COUNT=50` 限制一次显式核算人数，已由 2026-07-16 需求确认。
  - `DEFAULT_NORMAL_DAYS=21.75` 在缺少考勤应出勤时兜底，属于高风险默认值；缺少配置或考勤数据时可能影响工资。
  - `MID_MONTH_OVERTIME_UNIT_PRICE=12` 用于半路转正加班费，仍是直接代码常量。
  - `getDeptTypeForEmployee(...)` 写死 7 个员工 ID 做行政/生产体系例外，其中 3 个行政员工强制生产，4 个生产员工强制行政。
  - `fillAttendanceDataForEmployee(...)` 在“无考勤组”分支写死 `21.75`，并写死 3 个员工 ID 直接给全勤。
- `HrmSalaryMonthEmpRecordMapper.xml`：
  - 生产部门识别依赖固定部门名称清单：`质量部/仓库/质检部/设备技术部/生产部/仓储部/研发部/工程部`。
  - 领导全勤金额识别依赖岗位名称关键字：`经理/总监/副总/董事长/高级技师/厂长`。
  - 领导/普通全勤缺失兜底 `500/100` 已有配置和文档依据，但关键字分类仍硬编码。

## 2026-07-17 Attendance / Deduction Findings
- 考勤与全勤：
  - `fillAttendanceDataForEmployee(...)` 中生产体系双休例外写死 3 个员工 ID：`1712718940217/1712718940220/1712718940186`。
  - 成都公司 `0002` 试用期员工也可发全勤奖，是公司分支硬编码。
  - 部门 ID `1481534121629855751` 被特殊处理：部门名包含“总监”按领导全勤，否则按普通全勤，属于部门/岗位特例硬编码。
- 缺勤/请假扣款：
  - 迟到超过 `30` 分钟影响全勤判断。
  - 请假/抵扣类型使用数字编码：早退 `1`、迟到 `2`、事假 `3`、病假 `4`、调休 `5`、补卡 `6`、旷工 `8`、婚假 `9`、丧假 `10`、产假 `11`、陪产假 `12`。
  - 公司 `0001/0003/0005` 的旷工扣款用 4 位小数后保留 2 位；其它公司按日工资保留 2 位后再保留 0 位，属于公司级计算精度分支。
  - 公司 `0001` 的事假扣款走 4 位小数后 2 位；其它公司取整。
  - 病假扣款固定为 `2` 天内不扣，超过部分按最低基本工资 / 应出勤天数扣。
  - 非生产员工默认每日工作时长 `8` 小时，生产员工缺排班日为 `0` 小时；小时/天换算多处写死 `8`。
- 金额兜底：
  - `getYeBanAndJiaBan(...)` 在 `salaryBasic == null` 时仍用 `12` 元/小时加班费和 `30` 元/夜班补贴兜底；但前置校验要求薪资基本设置存在，理论上主核算应少走到该兜底。
- 导出：
  - `exportSalaryNew(...)` 写死导出表头虚拟编码 `9000~9010`、工资项列顺序、模板文件 `salary_export_cd.xlsx/salary_export.xlsx`。
  - 第 12 列通过 `companyId=0002` 在绩效工资 `41001` 和职务补助 `10103` 间切换。
  - `initExportData(...)` 仍按 `dataRow.get(0..26)` 写死列位置，虽然上游用 `fieldOrder` 规整过，维护风险仍较高。
- 遗留/非主链路：
  - `getNormalDays(...)`、`getDepartEmpNormalDays(...)` 仍保留生产月休 `4`、员工 ID 例外、`8` 小时换算等旧逻辑；当前主核算多处改为读取考勤汇总/配置，但这些方法仍在类内，需要后续确认是否还有外部入口调用。
  - `getEmloyeeQuitSalary(...)` 注释标明“有BUG”，并写死离职员工工资项 `180102/210101/240101`，但未在本轮看到主核算入口直接调用。

## 2026-07-17 Final Classification
- 高风险硬编码：
  - 员工 ID 例外：行政/生产体系强制切换、无考勤组直接全勤、生产双休例外。
  - 公司 ID 分支：`0002/0005/0001/0003` 影响奖金累计、工会费、试用期全勤、旷工/事假扣款精度、导出列含义和模板。
  - 部门/岗位硬编码：固定生产部门名称清单、岗位关键词、单个部门 ID `1481534121629855751`。
  - 工资项编码硬编码：薪资计算、考勤扣款、社保公积金、个税累计、附加扣除、导入导出均大量使用裸数字编码。
- 中风险硬编码：
  - 出勤和金额兜底：`21.75`、`8` 小时/天、半路转正加班 `12`、夜班补贴兜底 `30`、迟到 `30` 分钟、病假 `2` 天免扣。
  - Excel/导出映射：模板列下标、导出虚拟编码 `9000~9010`、固定列顺序和 `0002` 列含义切换。
  - 状态/类型数字：员工状态、入职状态、异常变动类型、假期抵扣类型、审核完成 `check_status=10`。
- 可接受但建议集中维护：
  - 个税法定七级税率、速算扣除数、月度减除费用 `5000`、全年备注减除费用 `60000`。
  - 已文档化的默认全勤金额 `100/500`、生产月休 `4`、一次核算人数上限 `50`。
- 遗留风险：
  - `computeSalaryDataAuto` 和 `SalaryComputeTask` 的实际计算调用已注释。
  - `SalaryMonthRecordService_Bak` / `SalaryComputeService_Bak` 仍被部分工资条服务注入。
  - `getEmloyeeQuitSalary(...)` 标注“有BUG”，但仍保留在主服务类内。

## 2026-07-17 Focus: getDeptTypeForEmployee-like Employee Exceptions
- 直接同类硬编码只在 `SalaryMonthRecordServiceNew` 主链路中发现：
  - `getDeptTypeForEmployee(...)` 将原本不是生产部门的员工 `1712718940198/1712718940199/1712718940200` 强制按生产体系 `deptType=1` 取应出勤天数。
  - 同函数将原本是生产部门的员工 `1712718940227/1831601326890434591/1831601326890434610/1831601326890434648` 强制按行政体系 `deptType=0` 取应出勤天数。
  - 该函数在半路转正薪资重算和同步考勤填充中都会生效：`processSalaryForEmployeeFromMemory(...)`、`fillAttendanceDataForEmployee(...)`。
  - 导出链路 `exportSalaryNew(...)` 又复制了同一批员工 ID 和同一套生产/行政切换逻辑，用于导出时查询 `hrm_attendance_info.dept_type`。
- 相关但不是完全同类的员工 ID 硬编码：
  - `fillAttendanceDataForEmployee(...)` 对生产体系员工 `1712718940217/1712718940220/1712718940186` 不改行政/生产体系，但改用“双休天数”覆盖应出勤天数，仍属于按员工 ID 改出勤日历。
  - `getNormalDays(...)` 对 `1712718940327/1712718940184` 走非生产换算逻辑，但当前主服务中唯一调用已被注释；旧 `_Bak` 服务仍有调用，是遗留风险。
  - `1712718940181/1712718940179/1789114659308232706` 是无考勤组直接全勤，不属于行政/生产体系切换，但同属员工 ID 特例。
- 未发现其它主链路文件按员工 ID 强制设置行政/生产体系。
  - `HrmProduceAttendanceServiceImpl#resolveDepartmentType(...)` 现在优先读 `hrm_employee.affiliation_system`，缺失时按部门名称关键词兜底。
  - `HrmOvertimeNightStatisticsServiceImpl#resolveExpectedAttendanceDays(...)` 按 `affiliation_system=1/2` 分支，没有员工 ID 特例。
  - `HrmSalaryMonthEmpRecordMapper.xml` 仍按部门名称生成 `isProduceDept`，这是部门关键词硬编码，不是特定员工硬编码。

# Findings: 薪资核算应计出勤天数来源调整

## 2026-07-17 Pre-work
- 已读取项目 `docs/requirements.md` 和 `docs/development.md`。
- 现有文档约束：
  - 薪资核算前置检查已会校验考勤汇总、应出勤、实际出勤等字段，避免空指针和不可定位错误。
  - 考勤汇总同步链路已经能从加班/夜班统计结果同步实际出勤与应计出勤，考勤汇总表的应计出勤天数字段是薪资核算更靠前的优先来源。
  - 加班/夜班统计明细已有 `accruedAttendanceHours`，同步考勤汇总时按 `/8` 转为应计出勤天数。
- 本轮新增业务口径：
  - 薪资核算中涉及“实际出勤天数”的地方改用“应计出勤天数”。
  - 应计出勤天数先查考勤汇总涉及的表；找不到或为空时再查加班/夜班统计功能涉及的表。
- 初始实现假设：
  - 考勤汇总表字段为 `hrm_produce_attendance.probation_attendance`。
  - 加班/夜班统计兜底字段为 `hrm_overtime_night_statistics_detail.accrued_attendance_hours / 8`。

## 2026-07-17 Implementation
- 当前薪资核算主入口为 `HrmSalaryMonthRecordController#computeSalaryData`，调用 `SalaryMonthRecordServiceNew`。
- `HrmProduceAttendanceMapper#getOvertimeAllowanceStatistics(...)` 从 `hrm_produce_attendance` 读取考勤汇总；实体字段 `probationAttendance` 对应当前考勤汇总“应计出勤天数”，`positiveAttendance` 对应“实际出勤天数”。
- `SalaryMonthRecordServiceNew#loadAttendanceSyncBatchData(...)` 已增加 `hrmOvertimeNightStatisticsDetailRepository#findAllByStatYearAndStatMonthAndEmployeeIdIn(...)`，用于加载同月同员工加班/夜班统计明细。
- 新增 `buildAccruedAttendanceDaysByEmployee(...)`：将 `accruedAttendanceHours / 8` 转为员工月度应计出勤天数；同一员工多条明细保留第一条非空月度值。
- 新增 `resolveSalaryAttendanceDays(...)`：优先使用考勤汇总 `probationAttendance`，为空或无汇总行时使用加班/夜班统计兜底；`0` 是明确值，不触发兜底。
- `fillAttendanceDataForEmployee(...)` 写入编码 `2` 时使用应计出勤天数；`getOrCreateRecordAndApplyAttendance(...)` 将编码 `2` 保存到 `actualWorkDay`，字段名保持历史兼容，但表头和校验文案改为“应计出勤天数”。
- 半路转正分段逻辑仍保留 `probationAttendance/positiveAttendance` 原分段语义，未在本轮改动。

# Findings: 考勤汇总同步出勤天数来源修正

## 2026-07-17 Pre-work
- 已读取项目需求和开发文档。
- 需求中已有“2026-07-15 考勤汇总同步加班/夜班统计”规则：
  - 考勤汇总同步按钮按用户选择的 `YYYY-MM` 年月执行；
  - 后端按所选年月读取“加班/夜班统计”月度汇总结果；
  - 实际出勤小时 `/8` 写入 `hrm_produce_attendance.positive_attendance`；
  - 应计出勤小时 `/8` 写入 `hrm_produce_attendance.probation_attendance`；
  - 同步方向固定为“加班/夜班统计 -> 考勤汇总”；
  - 已存在汇总行重新同步时需保留借款、补贴、扣款、备注等人工维护字段。
- 本轮用户进一步强调：实际出勤小时/应计出勤小时必须在数据库查询出来后再 insert/update 到考勤汇总实际出勤天数和应计出勤天数，不改变其它字段和其它业务逻辑。

## 2026-07-17 Implementation
- 当前 `hrm_overtime_night_statistics_detail` 已落库字段包含 `actual_attendance_days` 和 `accrued_attendance_hours`，没有单独的 `actual_attendance_hours` 物理列；统计查询 VO 中的 `actualAttendanceHours` 是查询层按现有口径计算/换算后的展示字段。
- 本轮实现采用最小影响方案：
  - `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 仍使用 `IHrmOvertimeNightStatisticsService#queryPageList(...)` 驱动员工月度行、加班、夜班、金额等原同步逻辑；
  - 同步开始时额外按 `statYear/statMonth` 查询 `hrm_overtime_night_statistics_detail`，按员工聚合落库明细；
  - `positiveAttendance` 优先使用落库 `actualAttendanceDays * 8 / 8`，即写入落库实际出勤天数；
  - `probationAttendance` 优先使用落库 `accruedAttendanceHours / 8`；
  - 当原月度 VO 缺少某员工但统计明细表有该员工时，使用聚合明细作为兜底同步行；
  - 其它字段仍保持原业务逻辑，不因本轮变更改为直接读取明细表。
- 新增测试 `syncFromOvertimeNightStatistics_shouldUsePersistedStatisticsDetailsForAttendanceDays`：构造月度 VO 为 `23/25` 天，同时落库明细为实际 `26` 天、应计 `224` 小时，断言最终写入 `26.00/28.00`，而加班、夜班、金额和借款仍保持原链路结果。

# Findings: 社保报表生成真实进度提示

## 2026-07-17 Initial Discovery
- 后端社保模块位于 `src/main/java/com/tianye/hrsystem/modules/insurance`，相关控制器包含 `HrmInsuranceMonthRecordController`、`HrmInsuranceMonthEmpRecordController`、`HrmInsuranceSchemeController` 等。
- 社保列表历史需求已要求默认当前年 1-12 月、高级筛选按月份范围和部门过滤；完成生成后前端刷新应复用当前列表查询，而不是整页重载。
- 现有真实进度模式：
  - 薪资核算：`/hrmSalaryMonthRecord/queryComputeProgress` 返回 `progress/status/stage/message/processedCount/totalCount`，服务端用内存 `ConcurrentHashMap` 按业务 key 保存进度。
  - 排班提交：`/workPlan/querySubmitProgress` 返回 `status/stage/retryCount/totalCount/successCount/failCount/errors`，前端轮询后提示并刷新。
- 前端社保管理页面位于独立项目 `/Users/jiangyongming/Project/hr/hr_web`，相关文件包括 `src/views/hrm/insurance-scheme/Index.vue`、`InsuranceScheme.vue`、`InsuranceDetail.vue` 与 `src/api/hrm/insurance-scheme/insurance-scheme.js`。
- 后端当前社保报表生成入口为 `HrmInsuranceMonthRecordController#computeInsuranceData -> HrmInsuranceMonthRecordService#computeInsuranceData`：
  - 该接口当前同步执行，返回完成后的年份；
  - 服务内先确定目标年月，再创建 `HrmInsuranceMonthRecord` 主记录；
  - 随后按 `queryInsuranceEmployee()` 返回的参保员工逐个生成 `HrmInsuranceMonthEmpRecord` 和项目明细；
  - 当前没有进度状态、进度查询接口或失败状态记录。
- 前端当前 `InsuranceScheme.vue#addMony` 已有确认框、按钮 loading、防重复点击、业务失败提示和成功后 `await getListData()` 刷新；本轮应保留这些能力，新增生成中进度弹框和轮询。
- `hr_web` 文档已有“社保管理新建次月报表反馈刷新（2026-07-17）”记录，本轮需要在该记录基础上补充真实进度轮询行为。

## 2026-07-17 Implementation
- 后端新增 `InsuranceComputeProgressVO` 和 `/hrmInsuranceMonthRecord/queryComputeInsuranceProgress`。
- `HrmInsuranceMonthRecordService#computeInsuranceData()` 在同步生成期间按租户 + 用户维护内存进度：
  - `PREPARE` 读取配置与结转上月；
  - `LOAD_EMPLOYEE` 加载参保员工总数；
  - `GENERATE_EMP` 按员工循环真实更新 `processedCount/totalCount` 与百分比；
  - `PERSIST/FINISH` 写入完成态；
  - 异常写入 `FAILED/ERROR`。
- 前端新增 `insurance-progress-utils.js`，并在 `InsuranceScheme.vue` 中打开“社保报表生成进度”对话框；`computeInsurance()` 请求执行期间轮询 `queryComputeInsuranceProgress()`，完成后关闭弹框、刷新列表并提示。
- 当前方案保持生成接口同步返回，不引入任务表；进度状态为应用内存级，服务重启后正在生成的进度不可恢复。

# Findings: 薪资核算个税备注方案实施

## 2026-07-16 Code Findings
- 实施前旧逻辑：`SalaryComputeServiceNew#computeSalary(...)` 先按“上月累计减除费用 + 5000”计算 `270102`，再对 `is_remark=2` 且收入未满 60000 的员工做本月个税免扣分支。
- 该旧逻辑与已确认方案不一致：`is_remark=2` 应改为累计减除费用固定 `60000`，而不是直接免扣；若扣除后应纳税所得额仍为正，需要继续按七级税率计算。
- `SalaryMonthRecordServiceNew#calculateMidMonthPromotionSummary(...)` 和半路转正复算链路也需要同步改成“是否全年 60000 减除费用备注”，否则半路转正员工会覆盖主计算结果。
- `SalaryComputeServiceNew` 与 `SalaryMonthRecordServiceNew#initLastMonthTaxMap(...)` 原存在 `month == 12` 重置累计的逻辑；按累计预扣法应在 `month == 1` 重新开始，12 月应继续累计全年。
- `SalaryComputeServiceNew#computeSalary(...)` 已有 `saveTaxAccumulationData(...)`，批量核算的 `computeSalaryFromMemory(...)` 也调用同一方法；因此核算完成后保存当月 `hrm_personal_income_tax` 的入口已经存在，本轮重点是让保存的数据使用新口径。

## 2026-07-16 Implementation Findings
- 已新增 `SalaryComputeServiceNew#resolveCumulativeDeductions(...)` 和 `hasAnnualDeductionRemark(...)`：
  - 无备注员工按 `5000 * 计薪月份` 写入 `270102`；
  - `is_remark=2` 员工按 `60000` 写入 `270102`。
- 已将主薪资核算、半路转正复算、一致性复算统一到同一累计减除费用方法；备注员工不再直接免税，仍按 `max(累计应纳税额 - 上月累计已缴税额, 0)` 计算本月个税。
- 已将个税累计重置边界改为 1 月；12 月继续累计全年。
- 已移除批量核算上下文中旧备注免税规则使用的上一年累计收入数据，避免新口径下继续加载无用数据。
- 定向验证通过：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test`，37 个测试。

# Findings: 个税/附加导入重名员工匹配修复

## 2026-07-16 Root Cause Evidence
- 用户提供数据源：
  - 原始申报数据：`/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报`；
  - 按系统模板整理数据：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels`。
- `hr_0003.hrm_personal_income_tax` 中 `2026-05` 个税累计共有 100 行、98 个员工，重复不是同名员工自然重复，而是同一员工 ID 重复：
  - `employee_id=1831601326890434614`；
  - 姓名 `王芳`；
  - 工号 `TYNG-107`；
  - 手机 `13032750052`；
  - 身份证 `420801197602274069`；
  - 3 条 `personal_income_tax_id=2077585543764762633,2077585543773151242,2077585543777345539`；
  - 累计收入分别为 `29461.00/19281.00/32031.00`。
- `hr_0003.hrm_employee` 当前存在多个同名员工，例如王芳 3 人：
  - `1831601326890434572 / TYNG-437 / 15871989405 / is_remark=2`；
  - `1831601326890434577 / TYNG-393 / 13872932293 / is_remark=2`；
  - `1831601326890434614 / TYNG-107 / 13032750052 / is_remark=2`。
- 根因判断：历史导入服务按员工姓名匹配，遇到同名员工时可能把多行不同人员数据写入同一个 `employee_id`。

## 2026-07-16 Implementation Evidence
- 已新增 `TaxImportEmployeeMatcher` 作为个税/附加导入共用员工匹配器：
  - 有手机号时使用“姓名 + 手机号”精确匹配员工；
  - 无手机号且姓名唯一时兼容旧模板；
  - 无手机号且姓名重复时拒绝导入并提示补手机号；
  - 同一导入文件内同一员工同一年月或同一年重复时拒绝导入。
- 已改造三个导入服务：
  - `HrmPersonalIncomeTaxService#resolvePersonalIncomeTaxData` 使用第 8 列手机号；
  - `HrmAdditionalService#resolveAdditionalData` 使用第 10 列手机号；
  - `HrmEmployeeAdditionalService#resolveEmployeeAdditionalData` 使用第 8 列手机号，并修复多年份文件只删除最后一个年份的问题。
- 已改造三个导入接口异常处理：导入失败时返回 `Result.Error(ax)`，不再打印异常后仍返回成功。
- 最终导入文件校验：
  - `个税累计.xls`：100 行，手机空值 0，姓名+手机重复 0；
  - `附加扣除累计.xls`：41 行，手机空值 0，姓名+手机重复 0；
  - `专项扣除累加表.xlsx`：64 行，手机空值 0，姓名+手机+年份重复 0。
- 原始申报文件全行搜索核对王芳：
  - `2026-05` 三条王芳分别为身份证 `42010619790625006X/42243219760108652X/420801197602274069`，累计收入 `32031/19281/29461`；
  - 与最终导入文件按手机号映射后的三条王芳一致。
- 已生成现有数据修复 SQL：`docs/sql/2026-07-16_hr_0003_tax_import_duplicate_name_repair.sql`，默认 `ROLLBACK`，已验证语法和事务内核对查询。
- 用户授权后已正式执行王芳重名错归属修复 SQL，执行后王芳 `2026-05` 个税累计归属为：
  - `TYNG-437 / 15871989405 / 19281.00`；
  - `TYNG-107 / 13032750052 / 29461.00`；
  - `TYNG-393 / 13872932293 / 32031.00`。
- 全表重复复查发现年度附加扣除仍有 19 组 2025 年历史重复，其中 18 组值完全相同，1 组孙小虎值不同。
- 已按最终导入模板新增并执行 `docs/sql/2026-07-16_hr_0003_employee_additional_duplicate_cleanup.sql`：
  - 完全相同的重复组删除重复导入行；
  - 孙小虎 `2025` 年保留 `housing_loan_interest=12000.00`、`raising_girls=2000.00`。
- 最终复核：
  - `hrm_personal_income_tax(employee_id, year, end_month)` 重复计数 `0`；
  - `hrm_additional(employee_id, year, month)` 重复计数 `0`；
  - `hrm_employee_additional(employee_id, year)` 重复计数 `0`。

# Findings: 薪资核算个税新场景方案

## 2026-07-16 Pre-work
- 已读取 `docs/requirements.md` 和 `docs/development.md`。
- 项目已有与个税直接相关的文档规则：
  - 个税累计、附加累计列表需展示年月并按年月倒序；
  - 综合所得申报表可作为 `hrm_personal_income_tax`、`hrm_additional`、`hrm_employee_additional` 的基础来源；
  - 个税累计取申报表“累计收入额、累计减除费用、累计专项扣除、已缴税额”；
  - 附加累计取申报表“累计专项附加扣除”分项；
  - 年度附加扣除配置按相邻月份累计差额推导月度配置。
- 现有文档未记录“税务局系统是否生成备注”字段，也未记录薪资核算个税按备注切换减除费用的规则。
- 本轮用户要求先不要动代码，重点输出已有薪资生成代码下的新个税计算解决方案。

## 2026-07-16 Code Search
- 薪资生成入口：`HrmSalaryMonthRecordController#/computeSalaryData` 调用 `SalaryMonthRecordServiceNew#computeSalaryData(...)`。
- 个税计算核心候选：
  - `SalaryComputeServiceNew`：包含累计收入、累计减除费用、累计专项扣除、累计专项附加扣除、累计应纳税所得额、累计应纳税额、当月个税和保存个税累计逻辑。
  - `SalaryMonthRecordServiceNew`：负责批量薪资生成、加载个税累计/附加累计、更新下一月附加累计等流程。
  - `TaxCalculator`：统一七级超额累进税率工具。
- 实施前代码中已经出现 `hrm_employee.is_remark=2` 相关规则：当跨年累计收入条件满足时不计算个税；该字段很可能就是用户反馈的“税务局系统生成备注”，但需要进一步确认语义是否完全一致。
- 现有固定月度基本减除费用常量为 `5000`，`SalaryComputeServiceNew` 当前按“上月累计减除费用 + 5000”累计，而不是直接按生成月 `month * 5000` 重算。

## 2026-07-16 Existing Tax Chain
- `HrmSalaryMonthRecordController#computeSalaryData` 当前调用 `SalaryMonthRecordServiceNew#computeSalaryData(...)`。
- `SalaryMonthRecordServiceNew#doComputeSalaryData(...)` 先调用 `updateAddition(employeeId, year, month)` 生成下月专项附加累计，再准备批量薪资核算数据。
- `loadLastMonthTaxDataMap(...)` 会读取 `hrm_personal_income_tax` 中上月记录，映射为：
  - `250101` 上月累计收入；
  - `250102` 上月累计减除费用；
  - `250103` 上月累计专项扣除；
  - `250105` 上月累计已缴税额。
- `loadAdditionalDeductionMap(...)` 读取当月 `hrm_additional(year, month)`，`addAdditionalDeductionOptions(...)` 写入 `260101~260106`，个税计算时求和作为 `270104` 累计专项附加扣除。
- `SalaryMonthRecordServiceNew#computeSalaryFromMemory(...)` 最终调用 `SalaryComputeServiceNew#computeSalary(...)`，该方法会计算 `270101~270106` 和 `230101`。
- 正常个税公式：
  - `累计收入 = 上月累计收入 + 本月应发 + 发放奖金(公司0002除外) + 只计税奖金 + 福利计税收入`；
  - `累计减除费用 = 上月累计减除费用 + 5000`；
  - `累计专项扣除 = 上月累计专项扣除 + 本月社保公积金代扣`；
  - `累计专项附加扣除 = 当月 hrm_additional 六项累计合计`；
  - `累计应纳税所得额 = max(累计收入 - 累计减除费用 - 累计专项扣除 - 累计专项附加扣除, 0)`；
  - `累计应纳税额 = TaxCalculator 七级税率计算`；
  - `本月个税 = max(累计应纳税额 - 上月累计已缴税额, 0)`。
- 实施前备注逻辑：
  - `is_remark=2` 且跨年累计收入条件满足时，`230101` 本月个税直接为 `0`；
  - 但 `270102` 累计减除费用仍按 `上月 + 5000`，不是直接改为 `60000`。
- 当前主链路风险：
  - `SalaryComputeServiceNew#saveTaxAccumulationData(...)` 会删除并保存 `hrm_personal_income_tax`，但当前主入口 `SalaryMonthRecordServiceNew` 只批量保存工资项和员工月记录，未看到同等保存个税累计表的调用；
  - 如果生产使用主入口，7 月衔接 1-6 月累计仍依赖 `hrm_personal_income_tax` 已经由导入或其他路径维护。

## 2026-07-16 Proposed Tax Rules
- 将“是否生成税务局备注”定义为员工年度个税扣除口径，而不是“本月个税是否直接跳过”。
- 无备注员工：
  - 以 6 月为例，累计减除费用取 `30000`，即 `工资计算月 * 5000`；
  - 累计应纳税所得额 = 截止工资计算月累计收入 - 累计专项扣除 - 累计专项附加扣除 - `30000`。
- 有备注员工：
  - 以 6 月为例，累计减除费用取 `60000`；
  - 累计应纳税所得额 = 截止工资计算月累计收入 - 累计专项扣除 - 累计专项附加扣除 - `60000`；
  - 不应只在累计收入低于 60000 时免扣个税；如果扣除后仍有应纳税所得额，应按七级税率产生累计税额。
- 特殊情况：
  - 不建议以“某月工资 3000 未到 5000”作为独立免税规则来截断累计税；
  - 应统一按累计预扣预缴：当月应补税额 = 本月累计应纳税额 - 截至上月已缴税额，若为负数工资表按 `0` 扣税，负差额不在工资中退税，留待后续月份抵减或年度汇算。

# Findings: 附加累计列表按年-月筛选

## 2026-07-16 当前状态
- `HrmAdditionalController#importAdditional` 已要求 `year/month` 请求参数，导入服务 `resolveAdditionalData(...)` 会将数据写入 `hrm_additional.year/month`，并导入前删除同年月旧数据。
- `QueryAdditionalBO` 当前只有 `employeeName`，没有 `year/month` 字段。
- `HrmAdditionalMapper.xml#queryAdditionalList` 当前只按员工姓名筛选，虽然按 `a.year desc, a.month desc` 排序，但不按年月过滤。
- 选择年月的前端筛选如果只传参数、不补后端 SQL，会返回全部月份数据，功能表现不完整。

## 2026-07-16 Implementation
- `QueryAdditionalBO` 已新增 `Integer year` 与 `Integer month`。
- `HrmAdditionalMapper.xml#queryAdditionalList` 已新增 `data.year/data.month` 可选条件，选择年月后返回该年月附加累计数据。
- 员工姓名条件已补 `data.employeeName != null` 判断，避免空员工名误拼接条件。

# Findings: 按导入模板生成个税/附加 Excel

## 2026-07-16 Template Review
- 用户指定模板目录：`/Users/jiangyongming/Desktop/导入模版`。
- `个税累计.xls`：
  - sheet：`Sheet1`；
  - 第 1 行标题：`上月个税累计信息数据模板`；
  - 第 2 行表头：员工名称、岗位、工号、部门、累计收入额（截至上月）、累计减除费用（截至上月）、累计公积金社保扣除（截至上月）、累计已缴税额、手机；
  - 数据从第 3 行写入。
- `附加扣除累计.xls`：
  - sheet：`sheet1`；
  - 第 1 行标题：`个税专项附加扣除累计数据模板`；
  - 第 2 行表头：员工名称、岗位、工号、部门、累计子女教育、累计住房租金、累计住房贷款利息、累计赡养老人、累计继续教育、累计养幼女、手机；
  - 数据从第 3 行写入。
- `专项扣除累加表.xlsx`：
  - sheet：`Sheet2`；
  - 第 1 行标题：`专项扣除累加数据表`；
  - 第 2 行表头：姓名、累计子女教育支出扣除、累计住房贷款利息支出扣除、累计住房租金支出扣除、累计赡养老人支出扣除、累计继续教育支出扣除、累计3岁以下婴幼儿照护、年份、手机；
  - 数据从第 3 行写入。
- 写 `.xls` 需要 `xlwt/xlutils`；本机原先缺少，已安装到用户 Python 环境。
- 用户补充的关键规则：模板里的手机列用于防止重名员工误匹配，因此生成文件必须填入员工姓名与手机号。

## 2026-07-16 Final Excel Outputs
- 最终输出目录：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels`。
- 输出文件：
  - `个税累计.xls`：基于 `/Users/jiangyongming/Desktop/导入模版/个税累计.xls`，填入 `2026-05` 个税累计 100 行；
  - `附加扣除累计.xls`：基于 `/Users/jiangyongming/Desktop/导入模版/附加扣除累计.xls`，填入 `2026-05` 附加累计 41 行；
  - `专项扣除累加表.xlsx`：基于 `/Users/jiangyongming/Desktop/导入模版/专项扣除累加表.xlsx`，填入 `2025/2026` 年度固定附加扣除值 64 行；
  - `生成说明.txt`：记录输出目录、行数、年份和手机列校验。
- 员工信息补齐来源：`hr_0003.hrm_employee` 与 `hrm_dept`，输出列中的岗位、工号、部门、手机均来自系统员工主数据。
- 校验结果：
  - `个税累计.xls`：表头匹配模板，100 行，手机空值 0，姓名+手机重复 0；
  - `附加扣除累计.xls`：表头匹配模板，41 行，手机空值 0，姓名+手机重复 0；
  - `专项扣除累加表.xlsx`：表头匹配模板，64 行，手机空值 0，姓名+手机+年份重复 0。
- 口径修正：年度附加扣除使用每年每项固定累加值，不使用申报表累计值原数；本轮保留 `2025` 和 `2026` 两个年份，便于按模板年份列导入。

# Findings: 个税计算基础支持数据整理

## 2026-07-16 Pre-work
- 项目需求文档中与本轮直接相关的已知规则：
  - 个税累计接口 `hrmPersonalIncomeTax/queryPersonalIncomeTaxList` 需要展示“年月”，并按年+月倒序；
  - 附加累计接口 `hrmAdditional/queryAdditionalList` 需要展示“年月”，并按年+月倒序；
  - 年度附加扣除接口 `hrmEmployeeAdditional/queryEmployeeAdditionalList` 需要按年份倒序。
- 开发文档显示项目为 Java/Spring Boot 后端，既有文档没有给出个税累计、附加累计、年度附加扣除的字段映射细节，需要从代码模型和申报表表头反推。
- 用户给出的目录 `/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报文件夹` 不存在；桌面实际目录是 `/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报`。
- 本轮目标月份为 `2025-11`、`2025-12`、`2026-05`、`2026-06`。

## 2026-07-16 Field Mapping
- 申报资料包含 4 个 `.xls`：
  - `湖北田野农谷生物科技有限公司_综合所得申报_202511.xls`：主表 `个人所得税扣缴申报表`，202 行 42 列；
  - `湖北田野农谷生物科技有限公司_综合所得申报_202512.xls`：主表 196 行 42 列；
  - `湖北田野农谷生物科技有限公司_综合所得申报_202605.xls`：主表 197 行 51 列，另有 `Sheet1` 汇总；
  - `湖北田野农谷生物科技有限公司_综合所得申报_202606.xls`：主表 195 行 51 列。
- 2025 与 2026 申报表列数不同，2026 年在“其他扣除”下新增了多项字段，导致累计区从 2025 年第 22 列移动到 2026 年第 30 列；后续必须按表头语义定位列，不能按固定列号取数。
- `hrm_personal_income_tax` 需要字段：
  - `employee_id`；
  - `accumulated_income`：申报表“累计收入额”；
  - `accumulated_deduction_of_expenses`：申报表“累计减除费用”；
  - `accumulated_provident_fund`：申报表“累计专项扣除”；
  - `accumulated_tax_payment`：申报表税款计算区“已缴税额”；
  - `year/end_month`：申报月份。
- `hrm_additional` 需要字段：
  - `children_education`：申报表“累计专项附加扣除/子女教育”；
  - `housing_rent`：申报表“住房租金”；
  - `housing_loan_interest`：申报表“住房贷款利息”；
  - `supporting_the_elderly`：申报表“赡养老人”；
  - `continuing_education`：申报表“继续教育”；
  - `raising_girls`：申报表“3岁以下婴幼儿照护”；
  - 注意系统字段顺序和申报表顺序不同，系统导入服务读列顺序是：子女教育、住房租金、住房贷款利息、赡养老人、继续教育、养幼女。
- `hrm_employee_additional` 是年度专项附加配置；薪资服务 `updateAddition(...)` 使用下一年配置生成下月累计，非 12 月为“当前月累计 + 配置值”，12 月跨年直接使用配置值。该表更接近“月度固定配置”，不是申报表中的累计值本身。

## 2026-07-16 Generated Outputs
- 目标租户确认：文档与现场记录均指向 `0003 / hr_0003 / 湖北田野农谷生物科技有限公司`。
- 输出目录：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716`。
- 主要产物：
  - `hr_0003_个税计算基础支持数据.sql`：幂等 SQL，先删除目标年月/年份再插入；
  - `核对报告.md`：字段口径、匹配规则、汇总数量和执行提醒；
  - `个税累计明细_hr_0003.csv`：396 条可匹配个税累计明细；
  - `附加累计明细_hr_0003.csv`：130 条非零附加累计明细；
  - `年度附加扣除配置_hr_0003.csv`：64 条年度/月度配置；
  - `未匹配申报员工_hr_0003.csv`：342 条按身份证号未匹配系统员工的申报行；
  - `excel_import_format_reference/`：10 个系统导入列位参考 Excel。
- 月份汇总：
  - `2025-11`：申报 189 行，个税 SQL 99 行，附加 SQL 24 行，未匹配 90 行；
  - `2025-12`：申报 183 行，个税 SQL 99 行，附加 SQL 25 行，未匹配 84 行；
  - `2026-05`：申报 184 行，个税 SQL 100 行，附加 SQL 41 行，未匹配 84 行；
  - `2026-06`：申报 182 行，个税 SQL 98 行，附加 SQL 40 行，未匹配 84 行。
- 年度附加扣除配置：
  - `2025`：用 `2025-12 - 2025-11` 推导，25 行；
  - `2026`：用 `2026-06 - 2026-05` 推导，39 行；
  - 相邻月份差额未发现负数；`年度附加扣除未生成说明_hr_0003.csv` 仅有表头。
- 现有库数据提示：生成前 `hr_0003` 已有 `2025-12` 个税累计 100 条、附加累计 24 条、`2025/2026` 年度附加配置各 24 条；执行 SQL 会覆盖这些目标范围。
- 验证：将生成 SQL 的 `COMMIT` 替换为 `ROLLBACK` 后通过 MySQL CLI 执行，返回码 `0`，说明 SQL 语法和字段名可被当前库接受且未实际落库。

# Findings: 基本工资金额设置新增全勤和生产月休配置

## 2026-07-16 Implementation
- 用户原话写“添加两条保存项”，但明确列出三项并说明“这三项”；本轮按三项配置实现。
- 三项配置落在现有租户库 `hrm_salary_basic` 表：
  - `ordinary_full_attendance_amount decimal(10,2) default 100.00`：普通员工全勤金额；
  - `leader_full_attendance_amount decimal(10,2) default 500.00`：领导全勤金额；
  - `production_monthly_rest_days int default 4`：生产体系员工月度休息天数。
- `HrmSalaryBasicDefaults` 作为后端统一默认值来源，避免查询旧记录、无记录、统计读取和薪资核算兜底分散硬编码。
- `HrmSalaryBasicService#findAll()` 现在按最新 `createTime/id` 选择一条基本工资设置，并补齐旧记录缺失字段；保存时也会写入默认值，避免新记录空列。
- 薪资核算全勤金额有两条读取路径：
  - `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 从最新 `hrm_salary_basic` 读取岗位全勤金额，缺失时仍兜底 `500/100`；
  - 市场部门总监/普通员工特殊全勤逻辑改为从 `batchData.salaryBasic` 读取领导/普通默认值。
- 生产体系应出勤天数将“月休 4 天”替换为读取最新 `hrm_salary_basic.production_monthly_rest_days`；缺配置时仍为 `4`，工作日法定休息日额外扣减规则保持不变。
- 前端 `src/views/manage/salary/Index.vue` 新增三项表单输入，接口旧数据缺字段时显示 `100/500/4`，保存时沿用原 `saveSalaryBasic` 接口提交。
- 已执行 SQL 脚本 `docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql` 到 dev MySQL `153.0.237.98`；`hr_0001` 至 `hr_0005` 的三列和默认值均已复核存在。
- 本机默认 Maven 全局 settings 会导致 `com.aliyun:tea:[1.1.14,2.0.0)` 版本范围解析失败；用最小 settings 同时覆盖 user/global settings 后测试可正常通过。

# Findings: 考勤汇总行政体系加班同步口径修正

## 2026-07-15 Root Cause
- 张明 `2026-06` 在加班/夜班统计中的 `overtimeHours=47.72` 是页面展示用月度加班汇总，包含日级自动加班。
- 加班/夜班统计实际出勤、应计出勤模块已将行政/生产体系的可计入出勤加班限定为本地钉钉审批加班；张明当月无本地加班审批，所以实际/应计出勤使用的加班为 `0.00`。
- 考勤汇总同步服务 `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 原先直接把 `QueryOvertimeNightStatisticsPageVO.overtimeHours` 写入 `hrm_produce_attendance.work_over_time`，因此把展示用 `47.72` 错误同步到考勤汇总。

## 2026-07-15 Implementation
- `QueryOvertimeNightStatisticsPageVO` 新增 `attendanceOvertimeHours`，表示“可计入实际/应计出勤的加班小时”。
- `HrmOvertimeNightStatisticsServiceImpl#toPageVO(...)` 复用既有 `resolveAccruedAttendanceOvertimeHours(...)` 填充该字段；行政/生产体系取本地加班审批，其他体系仍取月度展示加班。
- `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 写入 `workOverTime` 时优先使用 `attendanceOvertimeHours`；字段为空时回退 `overtimeHours` 兼容旧调用方。
- 加班工资继续按同步后的 `workOverTime * HrmSalaryBasic.overtimePay` 计算，因此张明这种无审批加班场景会同步为 `workOverTime=0.00`、`overtimePay=0.00`。

# Findings: 上传考勤同步加班夜班出勤数据

## 2026-07-15 文档与初始约束
- 后端 `docs/requirements.md` 记录：加班/夜班统计明细已上线实际出勤、应计出勤相关字段，字段缺失会导致 JPA/SQL 查询失败，发布前需执行对应 DDL。
- 现有统计口径要求“上传考勤”导入结果不能反向影响加班/夜班统计；本轮同步方向应是从统计明细写入上传考勤表。
- 前端“上传考勤”入口位于 `hr_web/src/views/hrm/attendance/upload/Upload.vue`，API 文件为 `hr_web/src/api/hrm/attendance/upload.js`。
- 最新用户补充要求删除 `空班/次`、`中班/次`、`当月休假/天` 三列表格展示；只在 `加班/小时` 后新增 `加班工资`。

## 2026-07-15 初始代码定位
- 后端上传考勤接口为 `HrmProduceAttendanceController`：
  - `POST /hrmProduceAttendance/importProduceAttendance`
  - `POST /hrmProduceAttendance/queryMonthAttendanceList`
- 核心服务为 `HrmProduceAttendanceServiceImpl`，当前 Excel 导入会按年月整月删除 `hrm_produce_attendance` 后 `saveBatch`。
- 数据表实体为 `HrmProduceAttendance`，主键 `summary_id`，现有字段包含 `probationAttendance`、`positiveAttendance`、`workOverTime`、`emptyClass`、`middleClass`、`nightShift`、`nightSubsidy`、`currentMonthVacation`、`loan`、`otherSubsidies`、`otherDeductions` 等。
- 列表 VO `QueryMonthAttendanceVO.actualAttendance` 当前没有同名实体字段；需要在 SQL 中明确把 `positive_attendance` 或同步目标字段别名为 `actualAttendance`，避免依赖 `select *` 的隐式映射。
- 工资核算读取 `HrmProduceAttendance.getPositiveAttendance()` 作为实际出勤天数，并用 `getWorkOverTime()` 按薪资基础配置计算工资项 `180101` 加班费；新增 `加班工资` 字段需避免破坏既有按小时乘单价的核算口径，除非同步薪资读取逻辑明确改为使用该字段。

## 2026-07-15 Implementation
- 后端新增 `SyncProduceAttendanceBO(month/year/monthNumber)` 和 `UpdateProduceAttendanceCellBO(summaryId/field/value)`。
- `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 复用加班/夜班统计 `queryPageList`，将 `actualAttendanceHours / 8` 写入 `positiveAttendance`，将 `accruedAttendanceHours / 8` 写入 `probationAttendance`，同时同步 `workOverTime` 与 `nightShift`。
- 同步 upsert 以 `employeeId + year + month` 为键；已有记录保留人工维护的借款、补贴、扣款、备注等字段，缺失记录按员工部门名称推断 `department=1/2` 后插入。
- `HrmProduceAttendance` 新增 `overtimePay`，列表 VO 新增 `accruedAttendance/overtimePay`；Mapper 列表查询改为显式列和别名。
- 单元格保存采用字段白名单，允许保存实际出勤、应计出勤、加班小时、加班工资、夜班次数、夜班补贴、借款、其他补贴、其他扣款和备注；姓名、员工 ID、年月、部门不允许保存。
- 工资核算若 `overtimePay` 有值，生产体系加班费工资项 `180101` 优先使用该金额；没有值时继续按原加班小时乘单价计算。
- 前端上传考勤页新增“同步考勤”按钮和年月弹窗；列表新增“应计出勤天数”和“加班工资”，删除用户补充要求删除的三列。
- 前端业务数据列使用输入框加“保存”按钮的紧凑单元格，按 `summaryId/field/value` 保存入库。

## 2026-07-15 补充规则：考勤汇总命名与金额计算
- 用户补充要求将“上传考勤”菜单与页面文案改为“考勤汇总”，并隐藏“导入考勤”按钮。
- 当前前端 `Upload.vue` 仍有 `<div class="title">上传考勤</div>`、“导入考勤”按钮和 `ImportDialog` 挂载；`src/router/config.js` 中 `/hrm/attendance/upload` 的菜单名仍为“上传考勤”。
- 已先更新 `tests/upload-attendance-page.test.mjs`，要求页面标题/路由菜单为“考勤汇总”，源码不再出现“上传考勤/导入考勤/ImportDialog”。该测试尚需运行 RED。
- 用户补充金额计算：`加班工资 = 加班时长 * 基本工资金额设置的每小时加班费`，`夜班补贴 = 夜班次数 * 基本工资金额设置的夜班补贴额度`。
- 既有薪资核算口径位于 `SalaryMonthRecordServiceNew#getYeBanAndJiaBan(...)`：
  - 最近一条 `HrmSalaryBasic` 作为基本工资金额设置；
  - `HrmSalaryBasic.overtimePay` 是每小时加班费，缺省兜底为 `12`；
  - `HrmSalaryBasic.subsidy` 是夜班补贴额度，缺省兜底为 `30`。
- 当前 `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 只同步 `workOverTime/nightShift`，还未自动写入 `overtimePay/nightSubsidy`。
- 已实现：同步前读取最新 `hrm_salary_basic`，同步每行时写入 `overtimePay` 和 `nightSubsidy`；缺少配置时分别按 `12` 与 `30` 兜底，和薪资核算历史默认值一致。
- 已执行 `docs/sql/2026-07-15_hrm_produce_attendance_overtime_pay.sql` 到 dev MySQL `153.0.237.98`；`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 均有 `overtime_pay decimal(10,2) COMMENT '加班工资'`。

# Findings: 加班/夜班统计单人批量开始统计补全

## 2026-07-15 文档约束
- `docs/requirements.md` 要求“开始统计”和“单人统计”都用于重算加班/夜班统计明细；审批重抓、字段上线或统计口径变化后，目标月份必须重新执行其中之一才会刷新旧明细。
- 统计规则需保持一致：行政/生产体系应出勤口径、审批参与状态过滤、加班来源、实际出勤扣减与应计出勤公式都不能因入口不同而分叉。
- 近期开发文档要求统计重算不得使用长事务包住整月计算；删除旧明细与保存新明细应在短写事务内完成，并使用 JPQL bulk delete。
- 本轮需求的差异只在员工范围：开始统计是全员，单人统计是用户批量选中的员工。

## 2026-07-15 初始代码定位
- 后端核心文件已定位：
  - `HrmOvertimeNightStatisticsController`
  - `IHrmOvertimeNightStatisticsService`
  - `HrmOvertimeNightStatisticsServiceImpl`
  - `QueryOvertimeNightStatisticsPageBO`
  - `HrmOvertimeNightStatisticsServiceImplTest`
- 服务层已有两个入口：`startStatistics(...)` 与 `startStatisticsForEmployee(...)`；下一步需确认单人入口是否只支持一个 `employeeId`，以及前端是否传入选中的批量员工。

## 2026-07-15 Implementation
- 后端 `QueryOvertimeNightStatisticsPageBO` 新增 `employeeIds`，旧 `employeeId` 保留兼容。
- `HrmOvertimeNightStatisticsServiceImpl#startStatisticsForEmployee(...)` 现在优先按 `employeeIds` 批量解析员工；未传批量字段时继续兼容旧单个 `employeeId`。
- 批量单人统计对每个选中员工分别计算、删除该员工当月旧明细并保存新明细；未选员工不删除、不重算。
- 单人批量统计复用与“开始统计”一致的默认月度明细计算入口，避免单人路径和全员路径业务口径分叉。
- 代码评审发现单人统计按 ID 直取员工时会包含 `isDel=1` 逻辑删除员工，而“开始统计”会过滤这些员工；已补测试并修复为单人统计同样跳过已删除员工。

# Findings: 审批数据手工添加支持多日期

## 2026-07-15 文档约束
- 现有需求已规定审批数据手工添加只写本地 `tbattendanceapprove`，不得调用钉钉接口。
- 手工添加保存前必须校验员工已有 `tbattendanceuser` 映射，否则列表关联和后续统计都不可用。
- 时长以后端按开始/结束时间计算为准，保存为 `durationUnit=小时`；`workDate` 使用审批开始时间所在日期，`createTime` 使用服务器当前时间作为列表“同步时间”。
- 统计和列表当前都按单条审批快照读取；多日期手工添加更适合拆成多条本地快照，而不是存成一条跨非连续日期记录。

## 2026-07-15 Implementation
- 后端 `AddAttendanceApprovalBO` 新增 `approvalRanges[]`，每段包含独立 `beginTime/endTime`；旧 `beginTime/endTime` 保留作为单段兼容入参。
- `HrmAttendanceApprovalServiceImpl#addManualApproval(...)` 会先归一化审批时间段，再按“员工 × 时间段”循环保存本地 `tbattendanceapprove` 快照。
- 多日期添加仍使用服务器统一 `createTime`，每条快照的 `workDate` 使用各自开始时间所在日期，`duration/durationUnit` 按各自起止时间计算。
- 前端添加弹窗改为审批日期/时间段列表，可添加或移除日期行，并显示单段时长和合计时长。

# Findings: 审批数据按主键删除

## 2026-07-15 文档与代码现状
- 既有后端文档曾写明审批数据功能不提供删除操作；用户本轮明确要求操作列新增删除功能。
- `tbattendanceapprove.id` 是本地审批快照主键；列表 VO 对外字段为 `approvalId`，同名员工可能存在多条不同审批快照。
- 删除若按姓名、部门、月份、审批类型或时间范围定位，存在同名员工误删风险；必须只按 `approvalId` 删除单条快照。

## 2026-07-15 Implementation
- 后端新增 `DeleteAttendanceApprovalBO(approvalId)`。
- `HrmAttendanceApprovalServiceImpl#deleteApproval(...)` 校验 `approvalId` 非空，使用 `approvalRepository.findById(approvalId)` 查找并删除查到的实体。
- 新增测试明确删除 BO 不包含员工姓名/部门字段，仓库删除不得走 `deleteBy...` / `deleteAllBy...` 范围删除方法。

# Findings: 审批数据手工添加

## 2026-07-15 文档与代码现状
- 后端审批数据列表只读本地 `tbattendanceapprove`，通过 `userId -> tbattendanceuser -> hrm_employee/hrm_dept` 展示姓名、工号、部门、岗位。
- 列表“同步时间”对应 `tbattendanceapprove.createTime`；“申请日期”对应 `tbattendanceapprove.workDate`。
- 现有审批修改接口只支持本地更新 `subType`、`duration/durationUnit`、`statisticsStatus`，没有新增接口。
- 前端审批页已有“获取审批数据”弹窗，已实现员工/部门选择、审批类型多选、部门员工展开和员工 ID 字符串保真。
- UI 约束：保持 Element Plus 后台工具页的紧凑表单和数据表风格；新增弹窗需有明确必填校验、保存 loading、禁用重复提交和可键盘操作的表单控件。

## 2026-07-15 Implementation Direction
- 后端新增手工添加接口，保存到现有 `tbattendanceapprove`，不调用钉钉接口。
- 手工新增按选择员工逐人生成本地审批快照；如果员工没有 `tbattendanceuser.userId` 映射，应返回中文错误，避免列表无法关联员工或统计无法读取。
- 手工审批 ID 使用本地前缀生成，避免与钉钉流程实例 ID 冲突。
- 后端按 `beginTime/endTime` 计算小时数并保存 `durationUnit=小时`；前端同步展示小时和按 `1天=8小时` 换算的天数。
- `workDate` 使用开始时间所在日期；`createTime` 使用服务器当前时间作为同步时间。

# Findings: 潘红琼审批同步缺失与空时长排查

## 2026-07-15 Initial Question
- 用户反馈：潘红琼钉钉后台请假记录有 `9` 条，本地审批数据只获取到 `3` 条。
- 本地已获取到的 `3` 条也都无法计算时长。
- 用户同时要求确认很多员工时长未算出的原因，以及其他员工是否也存在本地钉钉审批数据与钉钉后台数据不符。
- 需要按系统化调试处理，先分层确认：员工钉钉映射、流程模板解析、实例列表抓取、实例详情过滤、审批表单解析、落库覆盖/清理、统计读取。
- 用户补充：希望优先通过钉钉接口获取时长，而不是本地自行计算。当前钉钉开放平台链路是先用 `processinstance/listids` 获取实例 ID，再用 `processinstance/get` 获取实例详情；审批详情包含表单信息，时长通常来自表单组件或请假套件 `extValue`，不是统一顶层字段。
- 当前代码已部分支持：`HrmAttendanceApprovalProcessInstanceParser` 会读取复杂请假组件 `extValue.durationInDay/durationInHour`，并兜底读取“时长/请假时长/加班时长”等字段；如果仍为空，说明需要进一步检查潘红琼这 3 条详情里的组件形态是否未覆盖。

## 2026-07-15 Evidence
- 潘红琼本地员工与映射：
  - `employee_id=1831601326890434570`
  - `job_number=TYNG-437`
  - `mobile=19820594095`
  - `affiliation_system=2`
  - `dingtalk_user_id/userId=02533200433128427058`
  - `dept=行政人力资源部`
- 先用 properties 中的钉钉应用直接调用审批详情接口时，钉钉返回缺少 `qyapi_aflow` 权限；继续追溯 `DDAccessToken` 后确认真实运行时按 `hrsystem.ddaccount` 中 `companyId=0003` 的应用取 token。
- 使用 `0003` 应用调用：
  - `process/listbyuserid(userId=02533200433128427058)` 返回 71 个可见流程，含 `请假申请 / PROC-F083E7CC-FAA7-4BBE-8A58-C3A8B9E8F8FB`；
  - `processinstance/listids(processCode=PROC-F083E7CC-FAA7-4BBE-8A58-C3A8B9E8F8FB, useridList=02533200433128427058, 2026-06 发起时间窗口)` 返回 9 个实例；
  - 本地 `tbattendanceapprove` 当前只有 4 条潘红琼 2026-06 请假快照，分别为 `2026-06-05 调休`、`2026-06-22 调休`、`2026-06-25 调休`、`2026-06-30 年假`，4 条均空时长。
- 对 9 条钉钉实例逐条核对：
  - 4 条业务日期在 2026-06：`2026-06-05 调休8小时`、`2026-06-22 调休8小时`、`2026-06-25 调休8小时`、`2026-06-13 年假8小时`；
  - 5 条为 2026-06 发起但业务日期在 2026-05：`2026-05-04~05-06 调休24小时`、`2026-05-10 调休3小时`、`2026-05-11~05-14 调休32小时`、`2026-05-15~05-17 调休24小时`、`2026-05-29~05-30 调休16小时`；
  - 因当前后端按业务日期判断是否进入目标月份，抓取 2026-06 时跳过 5 条 5 月业务日期审批属于当前代码口径，而不是同一口径下的本地丢数。
- 钉钉详情里的复杂请假组件 `ext_value` 明确包含 `durationInHour` 与 `durationInDay`；潘红琼本地 4 条空时长不是钉钉没给，而是历史解析/落库未保存。
- 扫描 `hr_0003` 2026-06 仍参与统计的请假快照：
  - 请假总数 `333`；
  - 空 `duration/durationUnit` 数 `288`；
  - 有时长数 `45`；
  - 涉及空时长员工 `51` 人。

## 2026-07-15 Code Change
- 已将 `HrmAttendanceApprovalProcessInstanceParser#resolveComplexLeaveDuration(...)` 调整为：复杂请假组件只要有 `durationInHour`，就直接保存为 `小时`；只有没有小时值时才保存 `durationInDay`。
- RED：`parse_shouldReadDingTalkComplexLeaveHourDurationFromExtValue` 旧逻辑失败，期望 `32小时`，实际 `4天`。
- GREEN：同一测试通过；解析全量与审批/统计回归均通过。

# Findings: 行政人力资源部王芳钉钉后台 6 条 vs 接口 13 条差异

## 2026-07-14 Initial Question
- 用户反馈：行政人力资源部王芳在钉钉后台显示的请假审批申请为 6 条，而上一轮接口/本地复核得到 13 条。
- 需要重新按接口调用链核对，不能直接沿用上一轮“13 条均有效”的结论。

## 2026-07-14 API Reproduction
- 后端调用链：
  - `process/listbyuserid(userId=1068600027950408)` 返回唯一请假模板：`请假申请 / PROC-F083E7CC-FAA7-4BBE-8A58-C3A8B9E8F8FB`；
  - `processinstance/listids(processCode=PROC-F083E7CC-FAA7-4BBE-8A58-C3A8B9E8F8FB, useridList=1068600027950408, startTime=2026-06-01 00:00:00, endTime=2026-06-30 23:59:59)` 返回 `13` 个流程实例；
  - `processinstance/get` 逐条详情确认 13 条均为 `COMPLETED / agree`，模板均为 `请假申请`，假别均为 `调休`。
- 13 条的发起时间、完成时间、业务请假区间：
  - `2026-06-07 16:39:31 -> 2026-06-08 08:28:16`，请假 `2026-06-01~06-02`，16h；
  - `2026-06-08 14:01:52 -> 2026-06-08 18:03:03`，请假 `2026-06-03~06-04`，16h；
  - `2026-06-10 08:14:30 -> 2026-06-10 11:33:11`，请假 `2026-06-05~06-06`，16h；
  - `2026-06-10 18:18:41 -> 2026-06-11 07:58:23`，请假 `2026-06-08~06-09`，16h；
  - `2026-06-11 09:04:07 -> 2026-06-11 10:37:52`，请假 `2026-06-10~06-11`，16h；
  - `2026-06-12 13:15:26 -> 2026-06-12 21:51:26`，请假 `2026-06-12~06-13`，16h；
  - `2026-06-18 08:20:43 -> 2026-06-18 08:21:26`，请假 `2026-06-15~06-16`，16h；
  - `2026-06-18 11:25:08 -> 2026-06-19 12:46:48`，请假 `2026-06-17~06-18`，16h；
  - `2026-06-22 08:37:26 -> 2026-06-22 09:36:46`，请假 `2026-06-20`，8h；
  - `2026-06-23 08:01:55 -> 2026-06-23 17:44:26`，请假 `2026-06-22~06-23`，16h；
  - `2026-06-25 08:11:35 -> 2026-06-26 09:25:59`，请假 `2026-06-24~06-25`，16h；
  - `2026-06-27 08:45:50 -> 2026-06-27 09:10:55`，请假 `2026-06-26~06-27`，16h；
  - `2026-06-30 07:52:09 -> 2026-07-01 09:59:31`，请假 `2026-06-29~06-30`，16h。
- 常见筛选口径计数：
  - 发起时间在 2026-06：`13` 条；
  - 完成时间在 2026-06：`12` 条，因为 `2026-06-29~06-30` 这条在 `2026-07-01` 完成；
  - 业务请假开始日在 2026-06：`13` 条；
  - 业务请假开始日 >= `2026-06-15`：`7` 条；
  - 业务请假开始日 >= `2026-06-15` 且完成时间在 2026-06：`6` 条。
- 结论：当前接口拿到 13 条，是因为后端按完整 6 月发起时间窗口抓取，再按业务日期属于 6 月保留；用户后台看到 6 条，很可能是后台筛选条件只覆盖 6 月下半月，并且按完成时间排除了 `2026-07-01` 才完成的最后一条。需要用户提供后台截图或筛选条件，才能确认是哪一个页面筛选项导致。

## 2026-07-14 Screenshot Verification
- 用户确认桌面 `/Users/jiangyongming/Desktop/1.png` 是后台查询截图。
- 用户随后确认桌面 `/Users/jiangyongming/Desktop/2.png` 是其中一条请假详情，且详情包含部门；该图显示审批编号 `202606291142000335491` 的所在部门为 `HB行政人力资源部`，请假类型为 `年假`，开始/结束为 `2026-06-28 上午 ~ 下午`，时长 `1天`。
- 截图查询条件：
  - 发起人：在职人员 `王芳`；
  - 发起时间：`2026-06-01 ~ 2026-06-30`；
  - 完成时间为空；
  - 可见审批编号共 6 条：`202606291142000335491`、`202606270904000229005`、`202606031746000226579`、`202606031745000311483`、`202606031742000340915`、`202606031741000152277`。
- 使用钉钉 `processinstance/get` 按这 6 个业务编号反查：
  - 全部对应 `originator_userid=1957266823950408`；
  - 详情中的 `originator_dept_name` 均为 `HB行政人力资源部`；
  - 其中两条业务日期在 2026-06：`2026-06-26 年假1天`、`2026-06-28 年假1天`，合计 `16小时`；另外四条是 2026-06 发起但业务日期在 2026-05 的调休。
- 钉钉当前通讯录反查：
  - `1957266823950408`：王芳，手机号 `15871989405`，部门 `湖北田野农谷生物科技有限公司_行政人力资源部`；
  - `1068600027950408`：王芳，手机号 `13032750052`，部门 `湖北田野农谷生物科技有限公司_生产部`，岗位 `杀菌机/卧离机`；
  - `0305684530950408`：王芳，手机号 `13872932293`，部门 `财务部`。
- 修正结论：
  - `/Users/jiangyongming/Desktop/1.png` 的 6 条确实是行政人力资源部王芳；
  - 此前得到的 13 条属于生产部王芳 `userId=1068600027950408`，因为本地 `hrm_employee.dingtalk_user_id` 与 `tbattendanceuser` 把行政人力资源部王芳和生产部王芳对调了；
  - 行政人力资源部王芳 2026-06 业务日期内只应保留两条年假，共 `16小时`，与 Excel `年假16 / 实出勤184` 一致。

# Findings: 王芳（行政人力资源部）审批重复排查

## 2026-07-14 Initial Scope
- 用户补充确认：本轮“重新获取”的对象是行政人力资源部王芳，不是财务部或生产部同名王芳。
- 固定目标员工身份：
  - `employee_id=1831601326890434572`
  - `employee_name=王芳`
  - `job_number=TYNG-437`
  - `mobile=15871989405`
  - `dept=行政人力资源部`
  - 钉钉权威 `dingtalk_user_id=1957266823950408`；本地当前保存的 `1068600027950408` 已确认属于生产部王芳。
- 排查时需要同时比对另外两个同名员工的 `userId`，确认没有审批数据串入。

## 2026-07-14 Read-only SQL Evidence
- 行政人力资源部王芳的 `tbattendanceuser` 当前映射是错误补齐：
  - `tbattendanceuser.id=225`
  - `userId=1068600027950408`
  - `empId=1831601326890434572`
  - `depId=1988144032667267073`
  - `createTime=2026-07-15 02:40:32`
- 钉钉通讯录显示 `1068600027950408` 实际是生产部王芳手机号 `13032750052`，所以该映射必须修正，不能作为有效补齐结果。
- 同名员工隔离结果：
  - 行政人力资源部王芳 `TYNG-437 / mobile=15871989405` -> 正确 `userId=1957266823950408`，2026-06 发起审批 6 条，其中业务日期在 6 月的年假 2 条；
  - 财务部王芳 `TYNG-393` -> `userId=0305684530950408`，2026-06 审批 3 条；
  - 生产部王芳 `TYNG-107 / mobile=13032750052` -> 正确 `userId=1068600027950408`，2026-06 发起审批 13 条调休。
- 重复检查结果：
  - `empId=1831601326890434572` 的 `tbattendanceuser` 数量为 `1`；
  - `userId=1068600027950408` 的 `tbattendanceuser` 数量为 `1`；
  - `tbattendanceapprove` 主键为钉钉流程实例 `id`，目标王芳 2026-06 无重复主键；
  - 按 `userId/tagName/subType/beginTime/endTime/duration/durationUnit` 分组，重复业务组为 `0`；
  - 将审批区间展开到 2026-06 日级后，重复覆盖天数为 `0`。
- 此前目标王芳 2026-06 审批为 13 条 `请假/调休` 的结论已废弃：
  - 13 条来自 `userId=1068600027950408`，钉钉详情部门为 `HB生产部`；
  - 这是生产部王芳的审批，不应参与行政人力资源部王芳统计。
- 当前统计明细仍是重抓映射前旧值：
  - `hrm_overtime_night_statistics_detail` 中目标王芳 2026-06 只有 1 条占位记录；
  - `expected_attendance_days=0`、`actual_attendance_days=0`、`accrued_attendance_hours=0.00`；
  - 因此当前差异不是审批重复造成，而是“审批重抓补映射后未重新执行该员工 2026-06 加班/夜班统计”造成。

## 2026-07-14 Excel and DingTalk Cross-check
- Excel `6月` 工作表中行政人力资源部王芳这一行：
  - `应出勤=200`；
  - `实出勤=184`；
  - `应计出勤=200`；
  - 请假列：`事假=0`、`病假=0`、`调休=0`、`年假=16`。
- 钉钉 `processinstance/get` 只读抽查行政人力资源部王芳 6 条流程实例：
  - 全部 `status=COMPLETED`、`result=agree`；
  - 全部 `originator_userid=1957266823950408`，详情部门 `HB行政人力资源部`；
  - `2026-06-26` 与 `2026-06-28` 两条为 `年假1天`，合计 `16小时`；
  - 其余 4 条为 2026-05 业务日期调休，因业务日期不在 2026-06 不应参与 6 月统计。
- 结论：
  - 行政人力资源部王芳的钉钉审批与 Excel 并不冲突：6 月业务日期内正好是 `年假16小时`；
  - 真正根因是本地同名王芳的 `dingtalk_user_id/tbattendanceuser` 对调，导致系统抓错生产部王芳的 13 条调休；
  - 这不是审批重复。
- 安全处理方案：
  - 修复本地两条王芳的 `dingtalk_user_id` 和 `tbattendanceuser`：行政人力资源部王芳 -> `1957266823950408`，生产部王芳 -> `1068600027950408`；
  - 重新获取行政人力资源部王芳 2026-06 审批，再执行单人统计；
  - 生产部王芳的 13 条审批应保留在生产部员工名下，是否统计由生产体系业务口径另行决定。

## 2026-07-14 Code Path Check
- `HrmAttendanceApprovalSyncServiceImpl#resolveAttendanceUserForEmployee(...)` 原来优先使用员工表 `dingtalk_user_id`，且已有值时不校验钉钉通讯录手机号/部门；这正是行政人力资源部王芳被补成 `1068600027950408` 的代码风险。
- 已补回归测试 `fetchMonthData_shouldRefreshExistingDingTalkUserIdWhenNameAndMobileMatchDifferentUser`，要求已有 `dingtalk_user_id` 也必须按钉钉通讯录姓名/手机号校验，不匹配时刷新并写回。
- `saveAttendanceUserMapping(...)` 使用 `attendanceUserRepository.findFirstByUserId(dingTalkUserId)` 复用映射，再写入 `empId/depId`，避免同一 `userId` 因重抓产生多条映射。
- 审批入库使用 `approvalRepository.findById(processInstanceId)` 查旧快照，再 `approvalRepository.save(entity)`；`tbattendanceapprove.id` 是主键，重复获取同一流程实例会更新，不会新增第二条。
- 重抓后的陈旧快照清理会通过 `collectApprovalCleanupUserIds(...)` 收集同一员工所有历史 `tbattendanceuser.userId`，避免因补映射后旧 userId 遗留导致历史审批继续参与统计。

# Findings: 行政体系 Excel 与加班夜班统计出勤对账

## 2026-07-15 代码修正后重算复查
- 已修正生产体系实际出勤加班来源：`affiliation_system=2` 不再把日级/月度自动加班汇总计入实际出勤，只计本地已通过加班审批；月度 `overtimeHours` 仍保留展示。
- 重新执行本机 `9083` 后端 `startStatistics(month=2026-06)` 后，再用 `queryPageList` 与 Excel 主表 `6月` 对账。
- 最新结果：27 人中 16 人三项一致，11 人仍有差异；差异全部在实出勤，应出勤和应计出勤全部一致。
- 代码修正后新增对齐：李凤皇，系统从 `200 / 289.48 / 200` 修正为 `200 / 196 / 200`。
- 剩余不一致人员：
  - 王琪：系统 `184/179.98/184`，Excel `184/180.5/184`；本地调休约 `6.02h`，Excel 调休 `5.5h`。
  - 潘红琼：系统 `200/162/200`，Excel `200/168/200`；系统调休 `38h`，Excel 调休 `24h` + 年假 `8h`。
  - 杨太琴：系统 `200/200/200`，Excel `200/192/200`；Excel 年假 `8h` 缺少系统有效扣减。
  - 高文利：系统 `184/150/184`，Excel `184/147/184`；系统调休 `10h`、年假 `24h`，Excel 调休 `1h`、年假 `36h`。
  - 王洪平：系统 `200/184.5/200`，Excel `200/186.5/200`；系统调休比 Excel 多 `2h`。
  - 余德胜：系统 `200/200/200`，Excel `200/192/200`；Excel 年假 `8h` 缺少系统审批快照。
  - 聂小玲：系统 `200/200/200`，Excel `200/184/200`；Excel 调休 `16h` 缺少系统审批快照。
  - 马春霞：系统 `200/208/200`，Excel `200/199/200`；系统有本地加班审批 `8h`，Excel 加班为 `0`，且 Excel `1h` 扣减缺失。
  - 张雪梅：系统 `200/201/200`，Excel `200/181/200`；系统仅命中加班 `1h`，Excel 病假 `16h` + 年假 `4h` 缺失。
  - 方小荣：系统 `200/200/200`，Excel `200/195.5/200`；Excel 调休 `0.5h` + 年假 `4h` 缺失。
  - 何晓光：系统 `200/209/200`，Excel `200/199/200`；系统有一条时长为空/`null` 的加班审批按 `08:00-17:00` 自然时间计入 `9h`，Excel 还有 `1h` 扣减缺失。

## 2026-07-15 再次重算对比
- 再次执行本机 `9083` 后端 `startStatistics(month=2026-06)`，再查询 `queryPageList` 与 Excel 对账。
- 最新结果：27 人中 21 人三项一致，6 人仍有差异；差异全部为实出勤，应出勤和应计出勤全部一致。
- 本轮新增对齐人员：王琪、潘红琼、杨太琴、高文利、王洪平。
- 剩余差异：
  - 余德胜：Excel 年假 `8h`，系统无有效请假扣减，系统 `200` vs Excel `192`。
  - 聂小玲：Excel 调休 `16h`，系统无有效请假扣减，系统 `200` vs Excel `184`。
  - 马春霞：系统有加班审批 `8h` 参与，Excel 加班 `0` 且有 `1h` 扣减，系统 `208` vs Excel `199`。
  - 张雪梅：系统有加班 `1h`，缺 Excel 病假 `16h` + 年假 `4h`，系统 `201` vs Excel `181`。
  - 方小荣：Excel 调休 `0.5h` + 年假 `4h`，系统无有效扣减，系统 `200` vs Excel `195.5`。
  - 何晓光：系统按空/`null` 时长加班审批 `08:00-17:00` 折算 `9h`，且缺 Excel `1h` 扣减，系统 `209` vs Excel `199`。

## 2026-07-15 再次复查当前系统统计
- 对账文件仍为 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx`，主表 `6月`，共 27 人。
- 当前系统侧使用本机 `9083` 后端 `POST /hrsystem/hrmOvertimeNightStatistics/queryPageList`，查询 `month=2026-06`。
- `hr_0003.hrm_overtime_night_statistics_detail` 当前 `2026-06` 明细数为 `1725`，最大更新时间为 `2026-07-15 04:57:36`。
- 当前对账结果：15 人三项一致，12 人仍有差异。
- 当前 27 人的应出勤全部一致，应计出勤全部一致；剩余差异全部在实出勤。
- 三项一致人员：李明明、张明、赵聪、闫倩、吴晓霞、王芳（行政人力资源部）、程静、王芳（财务）、喻艳、李红盼、黎冬霜、严锦、庞龙斌、李学飞、林艳。
- 差异人员：
  - 王琪：Excel `184/180.5/184`，系统 `184/179.98/184`，实出勤差 `-0.52`；当前系统按本地审批加班 `2h`、调休扣减 `6.02h` 计算，Excel 调休为 `5.5h`。
  - 高文利：Excel `184/147/184`，系统 `184/150/184`，实出勤差 `+3`；系统扣调休 `10h`、年假 `24h`，Excel 为调休 `1h`、年假 `36h`。
  - 李凤皇、潘红琼、杨太琴、王洪平、余德胜、聂小玲、马春霞、张雪梅、方小荣、何晓光：应出勤/应计已对齐，但系统实际出勤计入生产体系月度加班汇总，Excel 实际出勤未按同一加班口径体现，且部分 Excel 请假扣减缺少对应系统审批快照或分类/时长不一致。
- 业务判断点：若 Excel 是权威口径，生产体系实际出勤需重新确认是否应排除日级/月报自动加班，或仅计本地已通过加班审批；否则需让 Excel 补入系统月度加班小时并统一请假扣减。

# Findings: 生产体系月休四天统计与王芳映射补齐

## 2026-07-15 Initial Context
- 用户要求在“开始统计”时把生产体系员工固定为月休 4 天，减少用户每月维护操作。
- 现有统计服务 `HrmOvertimeNightStatisticsServiceImpl` 只有 `AFFILIATION_SYSTEM_ADMINISTRATIVE=1` 行政体系分支；非行政体系在 `resolveExpectedAttendanceDays(...)` 中直接返回 `0`。
- 现有 `WorkweekMonthCalendarVO` 已透出 `legalHolidayRestDays`，可以复用已有法定/业务假期日历数据；生产体系推荐应出勤公式为 `当月自然天数 - 4天固定月休 - legalHolidayRestDays`。
- RED 测试 `startStatistics_shouldUseFixedFourDayMonthlyRestForProductionEmployee` 已证明旧逻辑失败：2026-06 生产员工期望 `25` 天，实际返回 `0` 天。

## 2026-07-15 Implementation and Mapping Result
- `HrmOvertimeNightStatisticsServiceImpl` 已新增 `AFFILIATION_SYSTEM_PRODUCTION=2` 和固定月休 `4` 天规则；`resolveExpectedAttendanceDays(...)` 对生产体系返回 `month.lengthOfMonth() - 4 - legalHolidayRestDays`，并以 `0` 为下限。
- 2026-06 生产体系在 `legalHolidayRestDays=1` 时得到 `25天`，实际出勤小时按 `25 * 8 = 200.00` 计算；回归测试同时断言开始统计保存的日明细写入 `expectedAttendanceDays=25`、`actualAttendanceDays=25`。
- `hr_0003` 当前王芳复核：
  - 行政人力资源部：`employee_id=1831601326890434572`、`TYNG-437`、`mobile=15871989405`，本地当前错误为 `dingtalk_user_id=1068600027950408`，钉钉权威应为 `1957266823950408`；
  - 财务部：`employee_id=1831601326890434577`、`TYNG-393`，已有 `tbattendanceuser.id=200 / userId=0305684530950408 / groupId=1170211404`；
  - 生产部：`employee_id=1831601326890434614`、`TYNG-107`、`mobile=13032750052`，本地当前错误为 `dingtalk_user_id=1957266823950408`，钉钉权威应为 `1068600027950408`。
- 王芳（行政人力资源部）补映射的安全路径：
  - 审批数据抓取需先校验员工表现有 `dingtalk_user_id` 的钉钉通讯录姓名/手机号；不匹配时按姓名+手机号刷新；
  - 若直接 SQL 修复，必须同时限定 `employee_id + mobile + 当前错误userId`，将行政人力资源部王芳改为 `1957266823950408`，生产部王芳改为 `1068600027950408`，不得按姓名批量更新。
- 其他员工映射来源：
  - 考勤同步步骤 2 `AttendanceUserManager#GetAndSave()` 调钉钉在职花名册和用户详情，按手机号、姓名+手机号、唯一姓名和历史映射兜底增量写入 `tbattendanceuser`；
  - 审批抓取链路 `HrmAttendanceApprovalSyncServiceImpl#resolveAttendanceUserForEmployee(...)` 当前已改为先校验 `hrm_employee.dingtalk_user_id` 是否匹配员工姓名/手机号；缺失、失效或不匹配时按姓名+手机号唯一匹配钉钉花名册，写回员工表并保存 `tbattendanceuser`。
- Fresh verification：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，47 个测试，0 失败；
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test` 通过，2 个测试，0 失败。

## 2026-07-15 Final Verification Update
- 生产体系固定月休 4 天规则进一步收紧：固定月休已覆盖周末/休息类日期；法定休息日只有落在工作日时才在 4 天固定月休之外额外扣减。
- 2026-06 工作日历返回 `legalHolidayRestDays=3`，日明细为：
  - `2026-06-19` 周五 `legal_rest`；
  - `2026-06-20` 周六 `legal_rest`；
  - `2026-06-21` 周日 `legal_rest`。
- 因此生产体系 2026-06 只能额外扣 `2026-06-19`，应出勤为 `30 - 4 - 1 = 25天`，不是 `30 - 4 - 3 = 23天`。
- 已执行 guarded SQL 修复 `hr_0003` 三个王芳映射：
  - 行政人力资源部王芳 `1831601326890434572 / TYNG-437 / 15871989405` -> `1957266823950408`，`tbattendanceuser.id=225`；
  - 财务部王芳 `1831601326890434577 / TYNG-393 / 13872932293` -> `0305684530950408`，`tbattendanceuser.id=200`；
  - 生产部王芳 `1831601326890434614 / TYNG-107 / 13032750052` -> `1068600027950408`，`tbattendanceuser.id=226`。
- 已重新获取行政人力资源部王芳 2026-06 审批；6 月业务日期内只有两条年假：
  - `Of80W6CsQSiVJxSiw-9PWQ04041782522262`：`2026-06-26` 年假 `1天`；
  - `6rTwVeUWS_mq9p3rQKurkQ04041782704553`：`2026-06-28` 年假 `1天`。
- 行政人力资源部王芳重复检查：
  - 6 月业务日期审批 `2` 条，合计年假 `16.00小时`；
  - 按流程主键、业务时间组合、同日同类型查询均无重复组；
  - 生产部王芳 `1068600027950408` 的 `13` 条调休合计 `200.00小时`，属于另一名同名员工，不是行政人力资源部王芳重复审批。
- 单人统计接口与落库复核：
  - 接口 `startStatisticsForEmployee` 返回 `expectedAttendanceDays=25`、`actualAttendanceHours=184.00`、`actualAttendanceDays=23`、`accruedAttendanceHours=200.00`、`actualAttendanceRemark=扣除：年假16.00小时`；
  - `hrm_overtime_night_statistics_detail` 已落库 `expected_attendance_days=25`、`actual_attendance_days=23`、`accrued_attendance_hours=200.00`。
- 回归验证：
  - RED：`startStatistics_shouldDeductOnlyWeekdayLegalRestForProductionEmployee` 在旧实现下失败；
  - GREEN：同一测试通过；
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，48 个测试，0 失败；
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，92 个测试，0 失败。

## 2026-07-15 Follow-up: 初始空时长与重复审批状态
- 当前 `hr_0003` 复查显示，王琪、高文利、王芳（财务）、黎冬霜、严锦、庞龙斌、李学飞、林艳这批员工的历史审批快照还没有全部修正。
- 仍参与统计且 `duration/durationUnit` 为空的 2026-06 审批数量：
  - 王琪 `2` 条；
  - 高文利 `13` 条；
  - 王芳（财务）`3` 条；
  - 黎冬霜 `2` 条；
  - 严锦 `1` 条；
  - 庞龙斌 `1` 条；
  - 李学飞 `5` 条；
  - 林艳 `2` 条。
- 仍参与统计的同日同类型重复组：
  - 高文利 `2026-06-01 请假/调休` 两条，时间均为 `08:00~09:00`；
  - 林艳 `2026-06-06 加班` 两条，各 `8小时`；
  - 林艳 `2026-06-18 请假/调休` 两条，均为 `14:00~18:00` 左右。
- 当前统计明细最后更新时间仍为 `2026-07-14 21:57~21:58`，这些员工尚未在修正空时长/重复参与状态后重新统计。
- 因此该问题当前是“代码具备修正入口和参与统计开关，但历史数据尚未处理完成”；需要先按 Excel 或钉钉权威时长修正审批快照，再将重复审批中多余记录标记为 `取消至统计`，最后重新执行单人统计或整月统计。

## 2026-07-14 Initial Context
- 对账文件：`/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx`。
- 目标月份暂按文件名判断为 `2026-06`。
- 系统侧需对比加班/夜班统计的三项月度字段：
  - 应出勤时间：`expectedAttendanceDays * 8` 小时；
  - 实际出勤：`actualAttendanceHours`；
  - 应计出勤：`accruedAttendanceHours`。
- 当前已确认文档口径：行政体系应出勤来自单双休月历；实际出勤按“应出勤 + 本地审批加班 - 事假/病假/调休/年假扣减”；应计出勤按“实际出勤 + 调休 + 年假 - 加班 + 病假”。

## 2026-07-14 Excel Structure
- 主工作表：`6月`，员工数据行 `5-31`，共 27 人。
- 直接用于对账的 Excel 列：
  - `D` 应出勤（小时）；
  - `E` 实出勤（小时）；
  - `F` 应计出勤（小时）；
  - `H/I/J/K/S` 分别为事假、病假、调休、年假、加班小时。
- 辅助工作表：`6月异常打卡情况说明`，记录闫倩、李凤皇、张明缺卡说明，不直接作为三项小时对账字段。

## 2026-07-14 First Reconciliation
- 使用本机后端接口 `POST /hrsystem/hrmOvertimeNightStatistics/queryPageList`，登录租户为 `companyId=0003 / 湖北田野农谷生物科技有限公司`。
- 27 人中 8 人三项一致：李明明、张明、赵聪、闫倩、吴晓霞、程静、喻艳、李红盼。
- 19 人存在差异，分为两类：
  1. 系统应出勤为 `0`，Excel 为 `200` 小时：李凤皇、潘红琼、王芳（行政人力资源部）、杨太琴、王洪平、余德胜、聂小玲、马春霞、张雪梅、方小荣、何晓光。
     - 这些员工在 `hrm_employee.affiliation_system` 多数为 `2`，系统按生产体系处理，不套行政单双休月历。
     - 【已由用户补充修正】Excel 对这组人的 `200` 小时应按生产体系固定月休 4 天，并结合当月业务公休/假期理解，不是行政后勤单休口径。
  2. 系统应出勤与 Excel 一致，但实际出勤小时不同：王琪、高文利、王芳（财务）、黎冬霜、严锦、庞龙斌、李学飞、林艳。
     - 共同根因是本地 `tbattendanceapprove` 多条请假/调休/年假快照 `duration/durationUnit` 为空，统计按 `beginTime/endTime` 自然时长兜底，和 Excel 人工整理小时不一致。
     - 林艳还存在同日同类审批重复参与统计：`2026-06-06` 两条加班审批各 `8小时`，`2026-06-18` 两条调休审批约 `4小时`，系统累加为加班 `24小时`、调休约 `8.02小时`，Excel 只计加班 `16小时`、调休 `4小时`。

## 2026-07-14 User Correction: Production Calendar and Duplicate 王芳
- 用户补充业务规则：生产体系固定月休 4 天。
- 因 Excel 中生产体系相关行的 `公休/假期=5`，2026-06 的 200 小时应按“生产体系固定月休 4 天 + 当月业务公休/假期”理解，不应再描述为“行政后勤/单休 25 天口径”。
- 本次 Excel 只有两行王芳：
  - `王芳`：应匹配行政人力资源部 `TYNG-437`；
  - `王芳（财务）`：应匹配财务部 `TYNG-393`。
- 复核 `hr_0003.hrm_employee` 当前员工主数据时，同名“王芳”共查到 3 条；本次对账只涉及其中两条：
  - 行政人力资源部：`employee_id=1831601326890434572`，`job_number=TYNG-437`，`mobile=15871989405`，`affiliation_system=2`，本地员工表当前错误为 `dingtalk_user_id=1068600027950408`，钉钉权威应为 `1957266823950408`；
  - 财务部：`employee_id=1831601326890434577`，`job_number=TYNG-393`，`mobile=13872932293`，`affiliation_system=1`，员工表和 `tbattendanceuser.userId` 均为 `0305684530950408`；
  - 生产部：`employee_id=1831601326890434614`，`job_number=TYNG-107`，`mobile=13032750052`，`affiliation_system=2`，本地员工表当前错误为 `dingtalk_user_id=1957266823950408`，钉钉权威应为 `1068600027950408`，未出现在本次 Excel 主表。
- 后续对账、审批匹配、导入和统计都必须按员工 ID / 工号 / 手机号 / 部门 / 钉钉 userId 区分同名员工，不能只按姓名匹配。

# Findings: 王琪实际出勤与应计出勤计算过程悬浮展示

## 2026-07-14 Initial Context
- 前端加班/夜班统计的“实际出勤时间统计”矩阵位于：
  - `hr_web/src/views/hrm/attendance/overtime-night/Index.vue`；
  - `hr_web/src/views/hrm/attendance/overtime-night/DailyDetailPage.vue`；
  - 行数据由 `overtime-night-utils.js` 统一生成。
- 现有行数据已经包含：
  - `expectedAttendanceSummaryValue`：应出勤小时与天数；
  - `summaryValue`：实际出勤小时与天数；
  - `accruedAttendanceSummaryValue`：应计出勤小时与天数；
  - `actualAttendanceRemark`：后端返回的请假扣减说明，如 `扣除：调休0.50小时、年假8.00小时`。
- 后端当前未透出独立的“计算过程”字段；本轮可以先在前端从现有字段推导 tooltip 文案：
  - 实际出勤：`应出勤 + 可计入实际出勤的加班 - 请假扣减 = 实际出勤`；
  - 应计出勤：`实际出勤 + 调休 + 年假 + 病假 - 加班 = 应计出勤`。
- “王琪”应作为验收样例处理，不写死姓名；所有员工同类单元格都应显示计算过程。

## 2026-07-14 Fix
- `overtime-night-utils.js` 新增计算过程生成逻辑：
  - 从 `小时(天数)` 汇总值取小时数；
  - 从 `actualAttendanceRemark` 解析 `事假/调休/年假/病假` 等扣减小时；
  - 实际出勤 tooltip 显示应出勤、可计入实际出勤加班、请假扣减和最终结果；
  - 应计出勤 tooltip 显示实际出勤、调休/年假/病假补回、扣减加班和最终结果。
- `Index.vue` 与 `DailyDetailPage.vue` 两处实际出勤矩阵的“实际出勤小时(天数)”和“应计出勤小时(天数)”列均改为 `el-tooltip` 单元格模板。
- tooltip 使用 Vue 插值渲染多行文本，不使用 raw HTML；`teleported=false` 保证 scoped 样式能应用到 tooltip 内容。

# Findings: 闫倩 0.07 天请假时长舍入修正

## 2026-07-14 Follow-up: 实际出勤 181 小时来源
- 当前接口返回闫倩 `2026-06`：
  - `expectedAttendanceDays=23`，应出勤 `184.00` 小时；
  - `actualAttendanceHours=181.00`；
  - `actualAttendanceRemark=扣除：调休0.50小时、年假8.00小时`；
  - `overtimeHours=44.92`。
- 当前审批明细：
  - 可扣减请假：`2026-06-13 年假 1天 = 8.00小时`，`2026-06-26 调休 17:30-18:00 = 0.50小时`；
  - 行政体系可计入实际出勤的本地审批加班：`2026-06-03 2小时`，`2026-06-17 3.5小时`，合计 `5.50小时`。
- 月度日历确认 `2026-06-13` 是 `上班` 日，因此这条年假按当前规则应扣。
- 复算公式：`184.00 + 5.50 - 8.50 = 181.00`。
- 结论：`181` 不是没有扣年假；年假已扣 `8.00` 小时。如果年假未扣，当前口径会是 `189.00` 小时。

## 2026-07-14 Initial Context
- 闫倩本地审批快照 `2026-06-26 17:30:00 -> 18:00:00` 保存为 `duration=0.07`、`durationUnit=天`。
- 旧后端统计和前端列表均按 `1天=8小时` 直接折算，得到 `0.07 * 8 = 0.56` 小时。
- 业务按起止时间判断该请假只有 30 分钟，即 `0.50` 小时。
- 后端交接摘要显示已经完成两层修正：
  - 未来钉钉复杂请假同时返回 `durationInDay` 与 `durationInHour` 且不一致时，优先保存小时值；
  - 历史小数天快照在统计中若与起止时间只差小范围舍入误差，则按起止时间小时数扣减。
- 剩余缺口：前端审批列表 `approval-duration-utils.js` 仍会把 `0.07天` 显示成 `0.56小时(0.07天)`。

## 2026-07-14 Frontend Fix
- `approval-duration-utils.js` 新增小数天起止时间纠偏：
  - `durationUnit=天` 且 `duration` 是小数天时，先按 `duration * 8` 得到小时；
  - 再按 `beginTime/endTime` 计算实际小时；
  - 两者差异不超过 `0.30` 小时时，使用起止时间小时数展示。
- 该逻辑只影响非编辑态展示，不改变点击单元格编辑时保存的 `duration/durationUnit` 字段结构。
- 闫倩历史快照现在显示为 `0.5小时(0.06天)`，与统计扣减口径一致。

# Findings: 闫倩实际出勤加法排查

## 2026-07-14 Follow-up: 审批通过过滤与取消至统计复核
- 用户明确要求：
  - 获取审批数据时只获取申请通过的数据；
  - 取消、拒绝、终止、撤销、进行中等其他状态一律不获取；
  - 带 `预计加班时长` 字段但不是通过审批的实例也不得获取；
  - 统计实际出勤时，`取消至统计` 状态的申请不参与逻辑运算。
- 源码复核：
  - `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 在 `processinstance/get` 后调用 `isApprovedProcessInstance(...)`，仅允许 `status=COMPLETED` 且 `result=agree` 继续解析和保存；
  - 非通过实例在解析前直接跳过，不会调用 `approvalRepository.save(...)`，也不会调用 `addRetainedApprovalId(...)`；
  - `deleteStaleMonthDataForSelectedEmployees(...)` 会用 retained IDs 清理本轮未保留的旧快照，因此同员工同月重抓后拒绝/终止历史快照会被删除；
  - `HrmOvertimeNightStatisticsServiceImpl#buildAttendanceApprovalMap(...)` 和实际出勤扣减汇总都先调用 `isApprovalIncludedInStatistics(...)`，`statisticsStatus=取消至统计` 会被跳过。
- 回归测试：
  - `fetchMonthData_shouldPersistOnlyApprovedWorkflowInstances` 覆盖 `COMPLETED/agree` 保存、`COMPLETED/refuse` 和 `TERMINATED` 跳过；
  - `startStatistics_shouldIgnoreCancelledOvertimeApprovalWhenBuildingOvertimeMap` 覆盖取消至统计的加班审批不覆盖自动加班；
  - `queryPageList_shouldIgnoreCancelledLeaveApprovalWhenCalculatingActualAttendance` 覆盖取消至统计的请假审批不扣减实际出勤。

## 2026-07-14 Follow-up: 51 小时加班审批来源
- 用户追问闫倩审批数据中为什么有一条 51 小时加班，以及该时长是否符合逻辑。
- 当前 dev 库 `hr_0003` 复核：
  - 员工 `1831601326890434568 / 闫倩 / TYNG-462`，钉钉 `userId=30352228121210270`；
  - 本地 `tbattendanceapprove.id=3Pi7UG_qSyi9uJSL1zFsWQ04041782526625` 仍存在，字段为 `tagName=加班`、`beginTime=2026-06-18 05:00:00`、`endTime=2026-06-20 00:00:00`、`duration=51`、`durationUnit=小时`、`statisticsStatus=NULL`；
  - 起止时间自然跨度为 43 小时，但审批表单时长字段为 51 小时，审批数据列表显示的是 `duration + durationUnit`，不是按起止时间实时计算。
- 钉钉 `processinstance/get` 只读复核该流程实例：
  - `status=COMPLETED`、`result=refuse`，属于已完成但被拒绝的审批；
  - 表单字段包含 `加班日期=2026-06-18 05:00`、`结束日期=2026-06-20 00:00`、`预计加班时长=51`；
  - `加班原因` 写明 18 号和 19 号各 17 小时，并把 19 日端午按额外 2 倍折算，因此业务填报为 51 小时。
- 结论：
  - 51 小时不是列表查询临时生成，也不是后端按 43 小时跨度误算出来的；它来自钉钉审批表单中的时长字段。
  - 该流程实例是 `refuse`，按当前同步代码不应入库；它能继续显示，是 2026-07-11 旧同步逻辑未过滤拒绝审批留下的历史快照。
  - 当前代码 `HrmAttendanceApprovalSyncServiceImpl#isApprovedProcessInstance(...)` 已限制仅保存 `COMPLETED + agree`，同员工同月重抓后应通过 retained-ID 陈旧快照清理删除该拒绝记录。
  - 在未重抓清理前，该行 `statisticsStatus=NULL` 会参与加班/夜班统计；可通过页面“取消至统计”临时排除。

## 2026-07-14 Root Cause Evidence
- 本地 API `queryPageList` 查询闫倩 `2026-06` 返回：
  - `expectedAttendanceDays=23`，应出勤 `184.00` 小时；
  - `actualAttendanceHours=225.72`；
  - `overtimeHours=93.89`；
  - `actualAttendanceRemark=扣除：调休0.50小时、年假14.28小时`。
- 闫倩员工信息：
  - `employee_id=1831601326890434568`；
  - `job_number=TYNG-462`；
  - `affiliation_system=1`，属于行政体系；
  - 钉钉 `userId=30352228121210270`。
- 闫倩 `2026-06` 本地审批中存在 3 条 `加班` 审批：
  - `2026-06-03 18:00 -> 20:00`，`2.00` 小时；
  - `2026-06-17 15:30 -> 19:00`，`3.50` 小时；
  - `2026-06-18 05:00 -> 2026-06-20 00:00`，`51.00` 小时；
  - 本地审批加班合计 `56.50` 小时。
- 实际出勤公式复算：
  - `184.00 + 56.50 - 14.78 = 225.72`；
  - 当前“加法”来自本地审批加班，不是张明那类日级自动加班。

# Findings: 张明行政体系日级自动加班排除修复

## 2026-07-14 Accrued Attendance Follow-up
- 用户追问“为什么张明应计出勤有减扣”后确认根因：实际出勤已排除行政体系日级自动加班，但应计出勤仍按月度 `overtimeHours` 汇总扣减。
- 张明 `2026-06` 因 `overtimeHours=47.72` 被算成 `184.00 - 47.72 = 136.28`，这个扣减来自日级自动加班，不是请假或审批扣减。
- 修正后应计出勤中的“加班”与实际出勤使用同一可计入口径：
  - 行政体系：只扣本地审批加班；
  - 非行政体系：仍扣月度加班汇总。
- 张明无审批加班，因此应计出勤为 `184.00`；李明明审批加班 `8.00` 仍扣，应计出勤保持 `160 + 32 - 8 = 184`。

## 2026-07-14 Root Cause / Fix
- 张明 `hr_0003 / 2026-06` 是行政体系员工，月度加班汇总 `47.72` 小时来自日级自动加班，不是本地钉钉加班审批。
- 行政体系实际出勤应使用 `应出勤小时 + 本地审批加班小时 - 事假/病假/调休/年假扣减小时`；日级自动加班只保留在 `overtimeHours` 加班汇总展示中。
- 当前实现把展示用 `monthlyOvertimeHours` 与实际出勤用 `actualAttendanceOvertimeHours` 拆开：
  - 行政体系：`actualAttendanceOvertimeHours = 本地审批加班小时`；
  - 非行政体系：`actualAttendanceOvertimeHours = 月度加班汇总`。
- 张明只有 `外出` 审批、没有本地加班审批和可扣减请假，因此正确实际出勤为 `23 * 8 + 0 - 0 = 184` 小时，而不是 `184 + 47.72 = 231.72`。
- 李明明仍保留审批加班计入实际出勤：`184 + 8 - 32 = 160`，应计出勤为 `160 + 32 - 8 = 184`。
- Fresh verification：定向统计测试通过，完整 `HrmOvertimeNightStatisticsServiceImplTest` 46 个通过；本地 API 张明返回实际出勤 `184.00`、应计出勤 `184.00`，李明明返回 `160.00 / 184.00`。

# Findings: 李明明实际出勤与应计出勤小时差异修复

## 2026-07-14 Root Cause
- 李明明 `hr_0003` 2026-06 正确小时口径应为：应出勤 `184`、实际出勤 `160`、应计出勤 `184`。
- 数据结构对应关系：
  - 应出勤：`expectedAttendanceDays=23`，按 `23 * 8 = 184` 小时展示；
  - 加班：两条本地钉钉加班审批，各 `4` 小时，月度加班合计 `8` 小时；
  - 年假：钉钉复杂请假组件 `extValue` 含 `durationInDay=4`、`durationInHour=32`、`tag=年假`。
- 旧错误有两个来源：
  - 审批解析器没有读取复杂请假组件 `extValue.durationInDay/durationInHour`，导致年假 `duration/durationUnit` 为空；
  - 统计查询遇到空时长后按审批起止自然跨度兜底，叠加旧的应计出勤持久化值，页面出现 `207.4(25.93)`。

## 2026-07-14 Fix / Verification
- `HrmAttendanceApprovalProcessInstanceParser` 已解析复杂请假组件 `extValue.durationInDay/durationInHour`，李明明年假审批应保存为 `duration=4`、`durationUnit=天`。
- `HrmOvertimeNightStatisticsServiceImpl` 当前统一按 `实际出勤小时 = 应出勤小时 + 月度加班小时 - 事假/病假/调休/年假扣减小时` 计算，不再使用“行政体系或存在加班审批时加班按 0”的早期规则。
- 应计出勤小时按 `实际出勤时间 + 调休 + 年假 - 加班 + 病假` 计算；李明明为 `160 + 32 - 8 = 184`。
- dev MySQL 已复核：
  - `hr_0001` 至 `hr_0005` 均存在 `hrm_overtime_night_statistics_detail.accrued_attendance_hours`；
  - `hr_0003.tbattendanceapprove` 中李明明年假审批为 `duration=4`、`durationUnit=天`；
  - `hr_0003.hrm_overtime_night_statistics_detail` 中李明明 2026-06 两条加班明细均为 `expected_attendance_days=23`、`actual_attendance_days=20`、`accrued_attendance_hours=184.00`。

# Findings: 加班/夜班统计应计出勤计算项

## 2026-07-14 Initial Context
- 后端现有实际出勤：有正数应出勤天数时，按 `actualAttendanceHours = expectedAttendanceDays * 8 + 可计入实际出勤加班 - 事假/病假/调休/年假扣减` 计算；行政体系和存在加班审批时，加班不计入实际出勤公式。
- 用户本轮新增“应计出勤”字段，公式是 `实际出勤时间 + 调休 + 年假 - 加班 + 病假`，并要求“开始统计”时计算后入库保存。
- 前端实际出勤矩阵当前列顺序为“姓名、应出勤时间、实际出勤小时(天数)、每日列、备注”；新增列需放在第四列，即“实际出勤小时(天数)”之后、每日列之前。

## 2026-07-14 Fix
- 后端新增 `HrmOvertimeNightStatisticsDetail.accruedAttendanceHours`，对应 `hrm_overtime_night_statistics_detail.accrued_attendance_hours`。
- 应计出勤公式使用小时计算：`actualAttendanceHours + 调休小时 + 年假小时 - 可扣加班小时 + 病假小时`；可扣加班小时与实际出勤的可计入加班口径一致，行政体系只取本地审批加班；`statisticsStatus=取消至统计` 的审批不会进入调休/年假/病假小时。
- “开始统计”在生成月度日明细后，把统一的月度应计出勤小时回填到当月所有明细行；汇总列表、单人月度明细、每日明细总览均透出 `accruedAttendanceHours`。
- 历史明细若 `accrued_attendance_hours` 为空，查询接口按当前公式兜底计算；新统计明细优先读取已落库值。
- 前端 `Index.vue` 与 `DailyDetailPage.vue` 在实际出勤矩阵新增“应计出勤小时(天数)”列；`overtime-night-utils.js` 按 `accruedAttendanceHours / 8` 展示天数。
- 新增迁移脚本 `docs/sql/2026-07-14_overtime_night_accrued_attendance_hours.sql`。

# Findings: 加班/夜班统计实际出勤年假扣减与列顺序调整

## 2026-07-14 Initial Context
- 后端当前文档与源码的实际出勤扣减类型为 `事假`、`病假`、`调休`，用户本轮要求新增 `年假` 扣减，并用“应出勤天数 - 事假 - 病假 - 调休 - 年假 + 加班”描述统计口径。
- `HrmOvertimeNightStatisticsServiceImpl#resolveActualAttendanceDeductibleType(...)` 是扣减审批类型识别点；`calculateActualAttendanceFromExpected(...)` 已按“应出勤小时 + 可计入加班小时 - 扣减小时”计算。
- 前端实际出勤矩阵当前在 `Index.vue`、`DailyDetailPage.vue` 中按“姓名、小时(天数)汇总、应出勤时间、每日列、备注”展示；用户要求“小时(天数)汇总”改名为“实际出勤小时(天数)”，并与“应出勤时间”互换顺序。
- 相关前端测试位于 `tests/attendance-display-columns.test.mjs`、`tests/overtime-night-utils.test.mjs`；后端测试位于 `HrmOvertimeNightStatisticsServiceImplTest`。

## 2026-07-14 Fix
- 后端只需在 `resolveActualAttendanceDeductibleType(...)` 中增加 `年假` 分支；扣减小时累加、备注生成、按小时折算实际出勤天数的既有流程可复用。
- RED 后端测试证明旧实现忽略年假：23 天应出勤扣事假 4 小时、病假 8 小时、调休 4 小时、年假 8 小时应为 20 天，旧实现返回 21 天。
- 前端调整只涉及实际出勤矩阵列声明：
  - `DailyDetailPage.vue`；
  - `Index.vue`；
  - 列顺序为“姓名、应出勤时间、实际出勤小时(天数)、每日列、备注”。
- `summaryValue` 数据字段不变，避免影响现有 `overtime-night-utils.js` 聚合逻辑和实际出勤汇总卡。

# Findings: 审批数据管理员模板失败回退修复

## 2026-07-14 Superseding Update
- 用户确认业务口径是“右侧框选了员工或部门，就应该围绕右侧框员工查询审批数据”，管理员模板预查询持续失败会造成每个员工都像查询失败的误解。
- 最新实现已取消管理员模板预查询主链路：
  - `resolveProcessTemplates(token, userId)` 直接调用 `fetchUserVisibleProcessTemplates(token, userId)`；
  - 不再读取 `ddAccount.adminId`，也不再调用 `topapi/process.template.manage.get`；
  - 流程编码解析、`processinstance/listids.useridList` 都使用当前目标员工的钉钉 `userId`。
- 影响边界：
  - 解决“换任意员工都会出现管理员模板查询失败”的现场现象；
  - 若某个员工可见模板中没有目标审批类型，仍会解析不到 `processCode`，这是员工可见范围/钉钉权限问题，不再与管理员 ID 混淆。
- 验证：
  - RED：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` 失败 3 个新口径用例，旧实现仍会调用管理员模板；
  - GREEN：同一测试通过，7 个测试 0 失败；
  - 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` 通过，50 个测试 0 失败。

## 2026-07-14 Initial Context
- 项目文档要求“审批数据”查询链路只读本地 `tbattendanceapprove`；手工获取审批数据可调用钉钉审批接口并写入本地快照。
- 手工获取审批数据的模板解析口径：优先调用管理员可管理模板接口；若管理员模板为空、失败或 `adminUserId` 返回 `400023/用户不存在`，需回退到实际员工可见模板 `process/listbyuserid`。
- `adminUserId` 是钉钉管理员配置，不是当前员工；排查日志必须明确这一点。
- 本轮需要确认回退调用、流程编码缓存和错误处理是否仍会把管理员失败误算成员工抓取失败。

## 2026-07-14 Root Cause / Fix
- `HrmAttendanceApprovalSyncServiceImpl#resolveProcessTemplates(...)` 已经在每个目标员工上调用 `resolveProcessCodes(token, user.getUserId(), ...)`，不存在把 `adminUserId` 当作员工传入 `process/listbyuserid` 的主流程问题。
- 根因在管理员模板预查询的失败边界：`shouldFallbackFromManageableTemplateError(...)` 只允许 `400023/用户不存在` 回退；管理员模板接口若返回其他管理员侧错误，会直接抛出并中断当前员工抓取。
- 修复后管理员模板接口成为完全可选的预查询：任何管理员模板查询异常都会记录 `adminUserId`、`employeeUserId` 和原因，然后回退到当前员工可见模板解析流程编码。
- 真正的员工不存在仍由回退后的 `process/listbyuserid` / `processinstance/listids` 错误判断，保持全员抓取中跳过失效员工、全部失效时报明确提示的既有逻辑。
- 验证覆盖：
  - RED/GREEN 锁定管理员模板非 `400023` 错误也必须回退到员工 `userId`；
  - 审批工作流回归确认按员工解析流程编码、失效员工跳过、拒绝/终止审批过滤和陈旧快照清理仍通过。

# Findings: 加班/夜班统计实际出勤加入加班时间

## 2026-07-13 Initial Context
- 后端需求文档当前记录的实际出勤口径为：有正数应出勤时，实际出勤小时按 `expectedAttendanceDays * 8 - 事假/病假/调休扣减` 计算；无正数应出勤时保留旧打卡有效分钟兜底。
- 用户本轮明确要求“开始统计”时应出勤时间还要加上加班时间，再减去事假、病假、调休。
- 本轮拟调整为：`actualAttendanceHours = expectedAttendanceDays * 8 + overtimeHours - deductionHours`，其中 `overtimeHours` 取统计服务已算出的本月加班小时。
- 仍需确认源码中实际出勤计算发生在日级明细生成前还是生成后，避免加班小时尚未汇总时被漏加。

## 2026-07-13 Source Findings
- `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyAttendanceDays(...)` 当前在 `calculateMonthlyDetails(...)` 生成日明细前调用，只能拿到应出勤和请假扣减，拿不到本月最终 `overtimeHours`。
- `buildSummary(...)` 已能从日明细汇总出月度 `overtimeHours`，但随后 `resolveActualAttendanceForQuery(...)` 仍只按 `expectedAttendanceDays - deductionHours` 重算实际出勤小时。
- 因此修复需要覆盖两层：
  - 开始统计落库的 `actualAttendanceDays` 应在日明细生成后，按月度加班小时回填到当月所有明细行；
  - 查询响应的 `actualAttendanceHours/actualAttendanceDays` 也要把明细汇总出的 `overtimeHours` 传入实际出勤计算。

# Findings: 加班/夜班统计实际出勤加入加班时间

## 2026-07-13 Root Cause / Rule Change
- 用户明确要求“开始统计”中的应出勤时间还要加上加班时间，再减去事假、病假、调休。
- 旧实现中 `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyAttendanceDays(...)` 在日级明细生成前执行，只能拿到应出勤天数和可扣减审批，无法纳入后续计算出的 `overtimeHours`。
- 查询响应路径 `queryPageList`、`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 会基于统计明细重新计算 `actualAttendanceHours`；旧查询兜底同样只按“应出勤 - 扣减”计算，如果只改落库值，页面仍可能显示旧口径。
- 当前月度加班小时来源应复用统计服务已经算出的 `overtimeHours`，包括本地钉钉加班审批优先覆盖后的结果；不单独重扫审批或打卡。

## 2026-07-13 Fix
- 开始统计生成日级明细后，按当月所有明细汇总 `overtimeHours`，再回填统一的月度 `actualAttendanceDays` 到每条明细。
- 该阶段曾按 `expectedAttendanceDays * 8 + overtimeHours - deductionHours` 计算；2026-07-14 已被行政/非行政加班来源分流替代，行政体系只加本地审批加班。
- 查询响应计算实际出勤时也传入月度加班小时；每日明细总览先按员工聚合整月加班小时，再转换每行响应，避免单行只带当天加班导致月度实际出勤偏小。
- 实际出勤缓存键加入月度加班小时，避免相同员工/月/应出勤天数但加班小时不同的响应复用错误结果。

# Findings: 审批数据闫倩调休同步与子类型手工修改

# Findings: 吴晓霞加班审批异常与用户不存在日志

## 2026-07-13 Runtime Evidence
- 用户补充的后端日志来自选择吴晓霞后的抓取请求，但日志中的 `userid=296842114330963873` 是 `hrsystem.ddAccount.AdminID`，不是吴晓霞员工钉钉 ID。
- `hr_0003.hrm_employee` 与 `tbattendanceuser` 当前均确认吴晓霞 `employee_id=1831601326890434565` 对应钉钉 `userId=262756571521566047`，手机号 `13268968337`。
- 直接使用 `hrsystem.ddAccount` 中 `CompanyID=0003` 的钉钉应用只读核对：
  - `topapi/process/template/manage/get` + `userid=296842114330963873` 返回 `400023/用户不存在`；
  - 回退后 `topapi/process/listbyuserid` + `userid=262756571521566047` 返回正常；
  - 吴晓霞可见 `加班审批` 流程编码为 `PROC-E0626BE7-26B7-4EB9-AFDA-91348E747B40`；
  - `topapi/processinstance/listids` 在 `2026-06-01 00:00:00` 至 `2026-06-30 23:59:59` 返回 4 个实例：
    - `0X8AleRkTw6I2gi2zXTiRw04041781530997`：`COMPLETED/agree`，发起 `2026-06-15 21:43:17`，表单加班 `2026-06-15 18:00 -> 19:05`；
    - `XvF7uDgOQh21n4d8bA7TrQ04041781530927`：`TERMINATED`，发起 `2026-06-15 21:42:07`，表单加班 `2026-06-15 18:00 -> 2026-03-15 19:06`；
    - `NzMSp1MiT5GkPu20k2IsPA04041781527092`：`COMPLETED/refuse`，发起 `2026-06-15 20:38:12`，表单加班 `2026-06-15 18:00 -> 2026-06-15 07:05`，本地解析修复为 `06:05 -> 07:05`；
    - `rryVNtASRlmfPLT0E88mhQ04041780574018`：`COMPLETED/agree`，发起 `2026-06-04 19:53:38`，表单加班 `2026-06-04 12:00 -> 13:00`。
- 当前本地 `tbattendanceapprove` 保存了 3 条加班 + 1 条调休；其中 `06:05 -> 07:05` 是拒绝审批被同步入库，不应参与加班统计。
- 钉钉后台截图的 4 行是按“发起时间”筛选的审批申请列表，并未展示表单内“加班开始/结束时间”；本地审批数据列表按 `beginTime` 展示表单业务时间，两者口径不同。
- 根因假设更新：同步链路缺少钉钉流程实例状态过滤，导致 `result=refuse` 的审批也落库；`TERMINATED` 实例由于表单结束日期异常被修复到 2026-03 并被业务月份过滤跳过。

## 2026-07-13 Fix
- `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData` 在 `processinstance/get` 后新增审批实例状态过滤，仅保存 `status=COMPLETED` 且 `result=agree` 的实例。
- 非同意完成实例会记录“审批实例非同意完成，跳过入库”日志，并且不会加入 retained IDs；同员工同月同类型重抓后的陈旧快照清理会删除历史残留的拒绝/终止审批。
- 管理员模板预查询失败日志补充说明 `adminUserId` 是钉钉管理员，不是当前员工，避免把 `296842114330963873` 的 `400023/用户不存在` 误认为吴晓霞不存在。
- 验证：
  - RED：`fetchMonthData_shouldPersistOnlyApprovedWorkflowInstances` 失败，旧逻辑保存 3 个模拟实例；
  - GREEN：同一测试通过，拒绝/终止实例跳过；
  - 回归：审批/统计相关 87 个测试通过；
  - 本地接口按用户 payload 重抓吴晓霞 `2026-06/all` 返回 `insertedCount=3`，库中只剩 2 条同意加班 + 1 条调休，`06:05 -> 07:05` 已清理。

## 2026-07-13 Initial Context
- 后端需求文档中“审批数据”功能当前定义为只读本地 `tbattendanceapprove`，查询链路禁止调用钉钉接口。
- 用户反馈闫倩 `2026-06` 业务上只有 `06-13`、`06-26` 两条调休数据，但同步钉钉后本地列表多出 `06-04 请假-调休`。
- 同时新增需求：审批数据列表中的“审批子类型”列需要支持点击单元格后以下拉列表方式修改，下拉选项包含所有子选项，供特殊情况手工修正。
- 本轮需确认 `06-04` 的入库来源和是否应由同步逻辑过滤/纠正，并把“审批数据当前只读”的文档约束更新为“仅审批子类型可手工修改”。

## 2026-07-13 Source Findings
- 后端列表入口：`HrmAttendanceApprovalController#queryPageList`，服务层直接调用 `HrmAttendanceApprovalMapper#queryPageList`。
- 列表 SQL 只读取本地 `tbattendanceapprove`，并关联 `tbattendanceuser/hrm_employee/hrm_dept` 回显员工、部门、岗位；当前没有任何钉钉接口调用，符合只查本地约束。
- 手工同步入口：`HrmAttendanceApprovalServiceImpl#fetchMonthData` -> `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData`。
- 同步服务会先 `deleteExistingMonthDataForSelectedEmployees(...)` 删除目标员工、目标月份、目标审批类型的本地旧快照，再逐流程实例解析并保存；因此如果钉钉仍返回 `06-04` 流程实例，重复同步会把该记录重新写回。
- 前端审批页：`hr_web/src/views/hrm/attendance/approval/Index.vue` 已有固定 `leaveSubtypeOptions`（事假、调休、病假、婚假、丧假等）和审批子类型筛选，但表格“审批子类型”列当前只是文本展示。
- UI 决策：保持现有企业后台数据表风格，做单元格内联下拉编辑；交互需有 hover/cursor、loading 和保存失败提示。

## 2026-07-13 Data Evidence
- dev 库 `hr_0003` 中闫倩员工映射：
  - `employee_id=1831601326890434568`，`employee_name=闫倩`，`job_number=TYNG-462`；
  - 钉钉 `userId=30352228121210270`。
- `tbattendanceapprove` 中该用户 `2026-06` 实际有三条 `tagName=请假/subType=调休`：
  - `id=ichrEbNFT1KknK-6tk09CA04041780560125`，`2026-06-04 16:02:06 -> 20:19:27`；
  - `id=L5If7i6YSlC_1L4U2MFB5g04041781248571`，`2026-06-13 08:00:00 -> 18:00:00`；
  - `id=2vjoPShUTW6PjZH8z-IMwg04041782463727`，`2026-06-26 17:30:00 -> 18:00:00`。
- 三条请假/调休记录的 `createTime` 均为 `2026-07-11 15:59` 左右，说明它们来自最近一次钉钉审批同步，不是列表查询层临时生成。
- 当前同步前置删除会清空目标员工、月份、审批类型旧快照，导致用户后续若手工改了 `06-04` 的 `subType`，下一次同步仍会丢失修正并重新变成钉钉解析值。

## 2026-07-13 Fix
- 根因结论：
  - `06-04` 记录已经存在于本地 `tbattendanceapprove`，列表查询只负责展示本地快照；
  - 现有数据无法仅凭查询层判断该审批是否业务上应排除，因此本轮不在列表层硬编码过滤闫倩 `06-04`，也不执行自动删库。
- 后端修复：
  - 新增 `UpdateAttendanceApprovalSubtypeBO`；
  - `HrmAttendanceApprovalController` 新增 `/querySubtypeOptions` 和 `/updateSubtype`；
  - `HrmAttendanceApprovalServiceImpl#querySubtypeOptions` 合并系统默认请假子类型与本地已有 `subType`；
  - `HrmAttendanceApprovalServiceImpl#updateSubtype` 只更新本地 `tbattendanceapprove.subType`；
  - `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData` 不再重抓前先删除目标月份审批，改为保存本次钉钉返回实例后再按 retained IDs 清理陈旧快照；
  - 当同一 `procInstId` 已存在本地记录且 `subType` 与钉钉解析值不一致时，保留本地 `subType`，避免覆盖人工修正。
- 前端修复：
  - `src/api/hrm/attendance/approval.js` 新增子类型选项查询与更新 API；
  - `src/views/hrm/attendance/approval/Index.vue` 中“审批子类型”列支持点击后进入行内 `el-select`；
  - 下拉选项合并前端默认请假子类型与后端本地已有子类型；
  - 保存成功后更新当前行，保存失败时保留原值并提示。
- 测试覆盖：
  - 后端覆盖子类型选项合并、子类型保存、控制器委托、同一流程实例重抓时保留本地子类型；
  - 前端覆盖新增 API 路径、页面导入并调用子类型选项/更新接口、审批子类型列进入行内编辑。

## 2026-07-13 Runtime Error Fix
- 现场报错：`Modifying queries can only use void or int/Integer as return type!`。
- 根因：
  - `tbattendanceapproveRepository#deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(...)` 是显式 `@Modifying @Query` 删除语句；
  - 该方法之前返回 `long`；
  - 当前 Spring Data JPA 对 modifying query 的返回类型限制为 `void`、`int` 或 `Integer`。
- 修复：
  - 将该仓库方法返回类型改为 `int`；
  - `HrmAttendanceApprovalSyncServiceImpl` 中的 `deletedCount` 仍可用 `long` 变量接收，不影响日志与业务逻辑；
  - 测试代理返回值同步改为 `Integer`；
  - 新增反射测试锁定返回类型为 `int`，防止回归。

# Findings: 加班/夜班统计出勤区域与行政应出勤修复

## 2026-07-12 文档口径
- 后端 `docs/requirements.md` 要求：
  - 加班/夜班统计的应出勤天数仅对行政体系员工套用单双休设置；
  - 员工体系来源固定为 `hrm_employee.affiliation_system`，`1` 行政体系，`2` 生产体系；
  - 行政体系员工按单双休月历中的月度上班天数统计；
  - 员工月度明细和指定月份每日明细总览均需返回应出勤天数、实际出勤天数。
- 前端 `hr_web/docs/requirements.md` 的旧口径要求：
  - 单人查看与“显示所有”的两张矩阵表都在“汇总”列后展示“应出勤时间(天数)”和“实际出勤时间(天数)”。
- 本轮新需求覆盖旧前端口径：
  - 应出勤时间、实际出勤时间需要单独区域展示；
  - “每天加班小时”和“每天夜班次数”矩阵表不再展示应/实际出勤列。

## Current Evidence
- 前端源码中：
  - `src/views/hrm/attendance/overtime-night/Index.vue` 的单人查看顶部已有出勤摘要，同时两张矩阵表仍各自包含应/实际出勤列。
  - `src/views/hrm/attendance/overtime-night/DailyDetailPage.vue` 的显示所有顶部已有出勤摘要，同时两张矩阵表仍各自包含应/实际出勤列。
  - `tests/attendance-display-columns.test.mjs` 当前测试旧行为：断言两张表都包含应/实际出勤列。
- 后端源码中：
  - `HrmOvertimeNightStatisticsServiceImpl` 已有 `resolveExpectedAttendanceDays(...)`、`queryAdministrativeExpectedAttendanceDays(...)` 和 `calculateActualAttendanceDays(...)`。
  - 现有测试包含行政体系使用单双休、生产体系不套用行政单双休的用例，需继续细看是否覆盖员工体系字段缺失/默认值场景。

## 2026-07-12 Root Cause
- 前端根因：
  - 两个页面已经有独立的 `attendance-overview` 出勤展示区；
  - 但旧表格列配置仍把 `expectedAttendanceDaysText`、`actualAttendanceDaysText` 放在每天加班小时表和每天夜班次数表内，造成重复且不符合新口径。
- 后端根因：
  - `startStatistics` / `startStatisticsForEmployee` 重算时会给新明细写入应/实际出勤天数；
  - 但 `queryPageList`、`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 读取历史旧明细时，只从明细行取 `expectedAttendanceDays`；
  - 旧明细新增列为空或为 0 时，查询链路不会再根据员工 `affiliation_system=1` 调单双休月历补出应出勤天数；
  - 因此潘红琼这类行政体系员工查看旧统计结果时，应出勤时间会显示为空或 0。
# Findings: 李凤皇实际出勤天数调休折算排查

## 2026-07-12 文档口径
- 后端需求文档要求加班/夜班统计同步统计应出勤天数和实际出勤天数。
- 现有开发文档记录的实际出勤口径为“只基于 `hrm_attendance_clock`；同一业务日期最早到最晚实际打卡跨度达到 `480` 分钟计 `1` 天”。
- 用户反馈的新场景：李凤皇应出勤 `200` 小时、实际出勤 `196` 小时、调休 `4` 小时；当前实际出勤汇总显示 `24` 天，业务判断该结果错误。
- 待验证假设：当前算法只统计达到 8 小时的自然工作日，或只用打卡跨度折算整天，未把调休小时合并到月度实际出勤小时后再折算。

## 2026-07-12 Root Cause
- `HrmOvertimeNightStatisticsServiceImpl#calculateActualAttendanceDays(...)` 原实现按业务日期分组，只在当天最早/最晚打卡跨度达到 `480` 分钟时 `actualAttendanceDays++`。
- 该实现会忽略不足 `480` 分钟的半天实际出勤；李凤皇场景中 `196` 小时会落成 `24` 个整天。
- 同一服务已有 `tbattendanceapprove` 审批快照接入，但 `buildAttendanceApprovalMap(...)` 只筛选加班审批覆盖加班小时，`请假/调休` 审批未参与实际出勤汇总。

## 2026-07-12 Fix
- 实际出勤天数改为月度有效分钟折算：
  - 每个业务日期取最早到最晚打卡跨度，单日最多计入 `480` 分钟；
  - `tbattendanceapprove` 中当前员工钉钉 `userId` 对应、`tagName/subType` 包含“调休”的审批时长计入调休分钟；
  - `(有效打卡分钟 + 调休分钟) / 480` 取整数天。
- RED 测试复现 `196` 小时实际出勤 + `4` 小时调休返回 `24` 天；修复后返回 `25` 天。

# Findings: 李凤皇本地查看显示 0(0) 排查

## 2026-07-12 Root Cause
- 前端实际出勤矩阵的 `0(0)` 不是换算问题，而是接口返回的 `actualAttendanceDays` 为 `0`。
- 后端 `startStatistics` / `startStatisticsForEmployee` 已能把“196 小时打卡 + 4 小时调休”写成 `25` 天。
- 但历史统计明细新增列可能仍为 `0` 或空；`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 和 `queryPageList` 之前只对 `expectedAttendanceDays` 做查询兜底，没有对 `actualAttendanceDays` 做查询兜底。
- 因此页面读取旧明细时会显示 `0(0)`，即使审批数据功能里已经能看到调休。
- 运行态复核发现李凤皇 `2026-06` 还有第二类旧数据：`actualAttendanceDays` 已是正数 `24`，但按打卡 `196` 小时加调休 `4` 小时应为 `25`；此前查询兜底遇到正数会直接返回旧值。
- 同月调休审批快照的 `duration/durationUnit` 为空，仅有 `beginTime/endTime=14:00-18:00`；原 `resolveAttendanceApprovalHours(...)` 在 `duration` 为空时直接返回 `0`，导致调休分钟无法参与兜底。

## 2026-07-12 Fix
- 查询层新增实际出勤天数兜底：
  - 按员工+月份读取 `hrm_attendance_clock` 和 `tbattendanceapprove`，复用实际出勤算法推导实际出勤天数；
  - 查询响应返回 `max(历史落库 actualAttendanceDays, 实时推导 actualAttendanceDays)`，修正旧值 `24 -> 25`，同时不降低历史已落库的较大值；
  - 同一次查询按 `employeeId#yyyy-MM` 缓存结果，避免每日明细多行重复查询。
- 审批时长解析补充兜底：当 `duration/durationUnit` 为空或无法解析，但审批存在 `beginTime/endTime` 时，按时间差折算小时，覆盖 `14:00-18:00 = 4` 小时调休。

# Findings: 张明实际出勤 160 小时来源排查

## 2026-07-12 Initial Evidence
- 文档口径要求“实际出勤时间统计”读取接口返回的 `actualAttendanceDays`，前端按 `actualAttendanceDays * 8` 换算为小时展示。
- 后端实际出勤算法按员工月度汇总有效出勤分钟：
  - 优先使用 `hrm_attendance_clock.work_date` 作为业务日期；
  - 同一员工同一业务日期按最早到最晚打卡跨度计算；
  - 单日最多计入 `480` 分钟；
  - 再加本地 `tbattendanceapprove` 中当前员工钉钉 `userId` 对应的调休审批分钟；
  - 最后按 `(有效出勤分钟 + 调休分钟) / 480` 取整数天。

## 2026-07-12 Root Cause Evidence
- 张明定位结果：
  - 租户库：`hr_0003`
  - 员工 ID：`1831601326890434564`
  - 钉钉 userId：`1853391326781038`
  - 统计月份：`2026-06`
- `hrm_overtime_night_statistics_detail` 中张明 `2026-06` 每日明细均保存：
  - `expected_attendance_days=23`，页面显示 `184` 小时；
  - `actual_attendance_days=20`，页面显示 `160` 小时。
- 按当前后端算法复算 `hrm_attendance_clock`：
  - 命中打卡业务日期 `22` 天；
  - 其中 `18` 天按单日上限计满 `480` 分钟，共 `8640` 分钟；
  - `2026-06-10` 计 `471` 分钟；
  - `2026-06-13` 计 `361` 分钟；
  - `2026-06-26` 计 `357` 分钟；
  - `2026-06-12` 只有一条打卡，因同日不足两条打卡计 `0` 分钟；
  - 合计有效出勤分钟 `9829`，即 `163.82` 小时。
- `tbattendanceapprove` 中张明 `2026-06` 有两条“外出”审批：
  - `2026-06-24 08:03:00 -> 17:04:00`
  - `2026-06-30 07:30:00 -> 18:08:00`
  - 但当前算法只把 `tagName/subType` 包含“调休”的审批作为可抵扣分钟，`外出` 不计入实际出勤天数。
- 因此最终计算为：
  - `(9829 + 0) / 480 = 20` 天（整数截断）；
  - 前端显示 `20 * 8 = 160` 小时。

# Findings: 张明实际出勤多出 47.72 小时排查（历史口径，已被替代）

## 2026-07-14 Root Cause Evidence
- 该段记录的是 2026-07-14 早期排查结论；当前已被“行政体系实际出勤只计本地审批加班”规则替代。
- 当时后端需求文档要求：行政体系员工存在正数应出勤天数时，实际出勤小时按 `应出勤天数 * 8 + 月度加班小时 - 事假/病假/调休扣减` 计算。
- 当时开发文档记录：`HrmOvertimeNightStatisticsServiceImpl#buildSummary(...)` 会汇总日级 `overtimeHours`，再把该月度加班小时传给 `resolveActualAttendanceForQuery(...)`；`calculateActualAttendanceFromExpected(...)` 实际执行 `expectedHours + overtimeHours - deductionHours`。
- 张明定位结果：
  - 租户库：`hr_0003`
  - 员工 ID：`1831601326890434564`
  - 员工工号：`TYNG-012`
  - 钉钉 userId：`1853391326781038`
  - 统计月份：`2026-06`
- `hrm_overtime_night_statistics_detail` 中张明 `2026-06` 汇总：
  - `expected_attendance_days=23`，应出勤小时 `184`；
  - `sum(overtime_hours)=47.72`；
  - `actual_attendance_days=28`；
  - 日级加班小时来自统计明细自动计算，张明同月没有本地加班审批覆盖。
- `tbattendanceapprove` 中张明 `2026-06` 只有两条 `外出` 审批：
  - `2026-06-24 08:03:00 -> 17:04:00`
  - `2026-06-30 07:30:00 -> 18:08:00`
  - 没有 `事假`、`病假`、`调休`，因此可扣减小时为 `0`；`外出` 不参与扣减。
- 公式复算：
  - 实际出勤小时 = `23 * 8 + 47.72 - 0 = 231.72`；
  - 实际出勤天数 = `floor(231.72 / 8) = 28`；
  - 因此当时“多出的 47.72 小时”来自张明本月汇总加班小时，不是额外重复计算；当前新口径下该 `47.72` 不再计入行政实际出勤。

# Findings: 加班/夜班统计重算锁等待治理

## 2026-07-14 Root Cause Evidence
- 用户现场报错：`Lock wait timeout exceeded; try restarting transaction`，发生在加班/夜班统计相关重算后。
- 代码风险点：
  - `HrmOvertimeNightStatisticsServiceImpl#startStatistics(...)` 和 `startStatisticsForEmployee(...)` 原先使用方法级 `@Transactional`；
  - 删除旧统计明细后，事务还会继续执行保存、flush 和结果回查，`hrm_overtime_night_statistics_detail` 上的删除/插入锁要等整个入口方法返回才释放；
  - 仓库删除方法若使用 Spring Data 派生 delete，存在逐实体加载/删除风险，不适合整月重算场景。
- 数据库风险点：
  - 整月重算按 `work_date between 月初 and 月末` 删除旧明细；
  - 单人统计按 `employee_id + work_date between` 删除旧明细；
  - 若现场表缺少对应左前缀索引，MySQL 需要扫描更多行，会放大锁等待概率。

## 2026-07-14 Fix
- 统计入口取消方法级长事务；排班、打卡、审批数据计算在事务外完成。
- 新增短写事务辅助：
  - `replaceMonthlyDetails(...)` 只负责整月 bulk delete + flush + saveAll + flush；
  - `replaceEmployeeMonthlyDetails(...)` 只负责单人月度 bulk delete + flush + saveAll + flush；
  - 通过 `TransactionOperations` 把写库窗口收敛到最短。
- `hrmOvertimeNightStatisticsDetailRepository` 删除方法改为显式 `@Modifying @Query` JPQL bulk delete，返回 `int`。
- 新增幂等索引脚本 `docs/sql/2026-07-14_overtime_night_statistics_detail_indexes.sql`：
  - 检测已有以 `work_date` 为左前缀的索引，缺失时创建 `idx_otnight_detail_work_date`；
  - 检测已有以 `employee_id, work_date` 为左前缀的普通索引或唯一键，缺失时创建 `idx_otnight_detail_employee_work_date`。
- 若现场仍立即报锁等待，需要检查是否已有旧统计事务占锁未释放，可用 `SHOW PROCESSLIST`、`information_schema.INNODB_TRX` 或 `SHOW ENGINE INNODB STATUS` 定位阻塞会话。

## 2026-07-14 Verification
- `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#overtimeNightStatisticsEntryPoints_shouldNotUseLongRunningTransactions+startStatistics_shouldCalculateRowsBeforeDeletingExistingRowsToReduceLockTime+overtimeNightDetailRepositoryDeleteMethods_shouldUseBulkModifyingQueries test`：通过，3 个测试，0 失败。
- `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`：通过，44 个测试，0 失败。
- `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`：通过，94 个测试，0 失败。

# Findings: 行政体系无加班审批时实际出勤不叠加加班汇总（早期实现，已被替代）

## 2026-07-14 Rule Change
- 用户追加新口径：无加班审批时，若员工所属体系为行政体系，实际出勤时间也不要累加日级加班汇总。
- 当前实现中：
  - 开始统计路径在 `calculateMonthlyDetails(...)` 日明细生成后，只有 `attendanceApprovalMap.isEmpty()` 时才传入 `sumOvertimeHours(rows)`；
  - 查询路径在 `calculateActualAttendanceForQuery(...)` 中只有存在参与统计的加班审批时才把加班项置 `0`；
  - 因此“无加班审批 + 行政体系”仍会把日级 `overtimeHours` 叠加到实际出勤。
- 本轮设计：是否把月度加班传入实际出勤公式，统一由“员工是否允许叠加月度加班”判断决定；行政体系员工不允许叠加。

## 2026-07-14 Fix
- 早期实现：`calculateMonthlyDetails(...)` 回填月度实际出勤时曾改用 `shouldIncludeOvertimeInActualAttendance(...)` 判断；行政体系员工传入实际出勤公式的加班小时为 `0`。
- 当前实现已替代为显式 `actualAttendanceOvertimeHours` 选择：行政体系取本地审批加班小时，非行政体系取月度加班汇总。
- 查询路径同样按 `resolveActualAttendanceOvertimeHours(...)` 分流，不再依赖临时布尔判断。
- 加班汇总展示值 `overtimeHours` 不清零；只影响 `actualAttendanceHours/actualAttendanceDays`。
- RED/GREEN 覆盖开始统计和查询两条路径；统计服务 41 个测试通过，审批/统计组合回归 91 个测试通过。

# Findings: 加班审批存在时实际出勤不叠加加班汇总（早期规则，已被替代）

## 2026-07-14 Rule Change
- 用户明确新口径：审批数据里有加班记录时，实际出勤时间不要再累加日级加班汇总。
- 当前实现中 `buildSummary(...)` 汇总所有日级 `overtimeHours` 后传给 `resolveActualAttendanceForQuery(...)`；`calculateMonthlyDetails(...)` 也在日明细生成后用 `sumOvertimeHours(rows)` 回填实际出勤天数。
- RED 测试：
  - `startStatistics_shouldNotAddDailyOvertimeToActualAttendanceWhenOvertimeApprovalExists` 失败：旧实现返回 `2` 天，期望 `1` 天；
  - `queryPageList_shouldNotAddDailyOvertimeToActualAttendanceWhenOvertimeApprovalExists` 失败：旧实现返回 `2` 天，期望 `1` 天；
  - 当时的无加班审批旧口径回归通过，说明旧口径下无加班审批会继续叠加自动加班；该结论已被后续行政体系例外覆盖。
- 早期设计决策：加班汇总字段 `overtimeHours` 不清零、不隐藏；仅实际出勤公式中的 `overtimeHours` 在“员工当月存在参与统计的加班审批”时按 `0` 处理。
- 当前规则已替代为：行政体系实际出勤只加本地审批加班；李明明这类审批加班仍计入实际出勤，张明这类日级自动加班不计入实际出勤。

## 2026-07-14 Fix
- 开始统计路径：
  - `calculateMonthlyDetails(...)` 仍按审批优先规则生成日级 `overtimeHours`；
  - 回填月度实际出勤天数时，如果 `buildAttendanceApprovalMap(...)` 命中参与统计的加班审批，则传入实际出勤计算的加班小时为 `0`；
  - 无加班审批时曾继续使用 `sumOvertimeHours(rows)`，该点已被后续行政体系例外覆盖：行政体系员工同样传入 `0`。
- 查询路径：
  - `calculateActualAttendanceForQuery(...)` 曾新增 `hasIncludedOvertimeApproval(...)` 判断；
  - 员工当月存在参与统计的加班审批时，`calculateActualAttendanceFromExpected(...)` 曾使用 `0` 加班小时；
  - 当前该临时规则已被 `resolveActualAttendanceOvertimeHours(...)` 的行政/非行政加班来源分流替代。
- GREEN 验证：
  - 定向 3 个用例通过；
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，41 个测试，0 失败。

# Findings: 实际出勤扣减口径与备注列改造

## 2026-07-12 Initial Evidence
- 后端当前实际出勤算法仍以打卡有效分钟为核心：
  - `calculateActualAttendanceDays(...)` 汇总 `hrm_attendance_clock` 有效分钟；
  - `resolveCompensatoryLeaveMinutes(...)` 只额外加入 `调休` 审批分钟；
  - 查询兜底与重算均会使用这一套算法。
- 前端当前实际出勤矩阵：
  - `overtime-night-utils.js` 的 `actualAttendance` 类型汇总列显示 `actualAttendanceDays * 8`；
  - 单人查看与“显示所有”页面的实际出勤矩阵列为姓名、小时(天数)汇总、每日列；
  - 还没有“应出勤时间”和“备注”列。
- 新口径需要将实际出勤从“打卡分钟折算”改为“应出勤时间 - 指定请假扣减”，并把扣减原因透出给前端。

## 2026-07-12 Root Cause / Rule Change
- 张明显示 `160` 的根因来自旧算法：`9829` 有效打卡分钟按 `480` 分钟/天截断为 `20` 天，再显示为 `160` 小时。
- 用户确认的新规则不是把 `外出` 作为出勤补足，而是改变实际出勤基准：
  - 有正数应出勤时，实际出勤从应出勤小时开始算；
  - 只在存在 `事假`、`病假`、`调休` 时扣减；
  - 其他审批不扣减。
- 因此旧的“李凤皇调休补足到 25 天”口径已被覆盖：调休现在是扣减项，而不是补足项。

## 2026-07-12 Fix
- 后端：
  - `resolveActualAttendanceDeduction(...)` 汇总 `事假/病假/调休` 审批小时并生成备注；
  - `calculateActualAttendanceFromExpected(...)` 返回 `actualAttendanceDays`、`actualAttendanceHours`、`actualAttendanceRemark`；
  - 查询接口和月度明细/每日明细 VO 透出实际出勤小时和备注；
  - 只有当应出勤天数不是正数时，才回落到旧打卡有效分钟算法。
- 前端：
  - 实际出勤矩阵新增 `应出勤时间` 和最终 `备注` 列；
  - 汇总列优先使用 `actualAttendanceHours`，支持 `180(22.5)` 这类半天扣减展示；
  - 旧接口没有 `actualAttendanceHours` 时仍按 `actualAttendanceDays * 8` 显示，避免兼容性回退成 `0(0)`。

# Findings: 闫倩实际出勤扣两天与备注缺失排查

## 2026-07-13 Root Cause Evidence
- 闫倩定位结果：
  - 租户库：`hr_0003`
  - 员工 ID：`1831601326890434568`
  - 钉钉 userId：`30352228121210270`
  - 统计月份：`2026-06`
- `hrm_overtime_night_statistics_detail` 中闫倩 `2026-06` 每日明细落库：
  - `expected_attendance_days=23`；
  - `actual_attendance_days=21`。
- `tbattendanceapprove` 中闫倩 `2026-06` 命中 3 条可扣减审批，均为 `请假 / 调休`，`duration/durationUnit` 为空但有起止时间：
  - `2026-06-04 16:02:06 -> 20:19:27`，按分钟折算 `4.28` 小时；
  - `2026-06-13 08:00:00 -> 18:00:00`，折算 `10.00` 小时；
  - `2026-06-26 17:30:00 -> 18:00:00`，折算 `0.50` 小时。
- 按当前扣减口径复算：
  - 应出勤小时：`23 * 8 = 184`；
  - 调休扣减合计：`14.78` 小时；
  - 实际出勤小时：`184 - 14.78 = 169.22`；
  - 兼容旧字段天数：`floor(169.22 / 8) = 21` 天。
- 本地后端接口已正确返回：
  - `actualAttendanceHours=169.22`；
  - `actualAttendanceRemark=扣除：调休14.78小时`。
- 备注不显示的根因在前端单人查看弹窗：
  - `Index.vue#buildDetailMatrixRows` 把月度明细 `dailyDetails` 展开为矩阵行时，只透传 `expectedAttendanceDays/actualAttendanceDays`；
  - 漏传月度级 `actualAttendanceHours/actualAttendanceRemark`；
  - 因此单人查看显示会回退成 `actualAttendanceDays * 8 = 168(21)`，备注为空。

## 2026-07-13 Fix
- `hr_web/src/views/hrm/attendance/overtime-night/Index.vue` 在单人查看弹窗展开 `dailyDetails` 时补传：
  - `actualAttendanceHours: item.actualAttendanceHours ?? detail.actualAttendanceHours`
  - `actualAttendanceRemark: item.actualAttendanceRemark || detail.actualAttendanceRemark`
- `hr_web/tests/attendance-display-columns.test.mjs` 增加回归断言，要求单人查看把月度级实际出勤小时和备注传入每日矩阵。
# Findings: 吴晓霞加班审批异常与统计操作列

## 2026-07-13 Initial Context
- 用户反馈：`hr_0003` 吴晓霞在审批数据中出现一条早上 `06:05-07:05` 的加班数据，但钉钉后台没有这条数据。
- 同一员工钉钉后台显示有 `4` 条加班审批申请，本地开发环境审批数据只有 `3` 条。
- 新增页面需求：审批数据列表每行末尾新增“操作”列；当行数据状态为“取消至统计”时显示“添加至统计”，否则显示“取消至统计”。
- 当前文档约束：审批数据列表查询只读本地 `tbattendanceapprove`，不得在查询链路调用钉钉接口；加班/夜班统计会优先使用本地加班审批数据。
- 本轮先按根因调查处理本地异常快照与缺失审批，再增加本地统计参与状态，不删除审批记录。

## 2026-07-13 Document and Source Context
- 后端文档要求“审批数据”列表只查询本地 `tbattendanceapprove`，严禁列表查询链路调用钉钉接口。
- 后端文档要求加班/夜班统计优先使用本地 `tbattendanceapprove` 中的加班审批数据，同一员工同一天多条审批按日累加。
- 前端文档要求“审批数据”页来自本地审批快照接口，已有内联修改审批子类型能力；本轮新增的“操作”列应沿用本地修改模式。
- 因此“取消至统计/添加至统计”的合理落点是审批快照本地状态字段：列表保留该审批，统计服务过滤已取消统计的审批；操作本身不触发钉钉接口。

## 2026-07-13 Data Evidence
- `hr_0003` 吴晓霞员工映射：
  - `employee_id=1831601326890434565`；
  - `job_number=TYNG-110`；
  - 钉钉 `userId=262756571521566047`；
  - `tbattendanceuser.groupId=1170211404`。
- 当前 `hr_0003.tbattendanceapprove` 表结构没有统计参与状态字段，仅有 `id/subType/tagName/bizType/beginTime/endTime/durationUnit/userId/groupId/createTime/duration/workDate`。
- 吴晓霞本地审批快照中 `2026-06` 有 3 条加班：
  - `rryVNtASRlmfPLT0E88mhQ04041780574018`：`2026-06-04 12:00:00 -> 13:00:00`，`1小时`；
  - `NzMSp1MiT5GkPu20k2IsPA04041781527092`：`2026-06-15 06:05:00 -> 07:05:00`，`1小时`；
  - `0X8AleRkTw6I2gi2zXTiRw04041781530997`：`2026-06-15 18:00:00 -> 19:05:00`，`1小时`。
- 这 3 条 `2026-06` 加班记录的 `createTime` 均为 `2026-07-11 15:58:59`，来自同一次审批抓取落库。
- 本地 `hrm_attendance_approval_fetch_mark` 记录显示吴晓霞 `2026-06` 的 `all` 类型抓取在 `2026-07-11 16:25:42` 标记完成。

## 2026-07-13 Root-Cause Hypotheses
- 本地表未保存钉钉 `processinstance/get` 原始表单 JSON，`postresultlog/ddtaskresult` 也未命中对应流程实例原始响应；当前只能证明异常记录来自同步入库，不能从本地还原钉钉当前后台为何已无该记录。
- 代码风险 1：`HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 用任务级 `taskLevelProcessCodes` 复用流程编码；当管理员模板接口不可用并回退到“员工可见模板”时，全员抓取会用第一个员工的可见流程编码代表所有员工，可能漏抓吴晓霞可见但第一个员工不可见的第 4 条加班审批流程。
- 代码风险 2：`deleteStaleMonthDataForSelectedEmployees(...)` 只有在 `requestedEmployeeIds` 非空时才清理陈旧快照；前端“按员工但右侧未选人”会发起全员抓取，此时若钉钉当前不再返回某条旧审批，本地仍不会删除该旧快照。
- 代码风险 3：`topapi/processinstance/listids` 当前按所选月份设置 `startTime/endTime`；若钉钉接口该时间范围按流程发起时间过滤，而业务侧按加班发生时间查看，则“业务日期在目标月、发起时间在其他月”的加班申请可能被漏抓。

## 2026-07-13 Fix
- 根因边界：
  - `06:05-07:05` 行是本地 `tbattendanceapprove` 中的历史审批快照，不是列表查询临时生成；
  - 本地没有该流程实例的原始钉钉详情响应，无法证明钉钉后台当前不展示该行的直接原因；
  - 钉钉显示 4 条、本地只有 3 条的差异存在同步链路风险，已按可验证风险点修复。
- 后端修复：
  - `tbattendanceapprove`、`QueryAttendanceApprovalPageVO`、`HrmAttendanceApprovalMapper.xml` 新增/透出 `statisticsStatus`；
  - 新增 `UpdateAttendanceApprovalStatisticsStatusBO` 与 `POST /hrmAttendanceApproval/updateStatisticsStatus`；
  - `statisticsStatus=取消至统计` 的审批在加班审批优先汇总和实际出勤请假扣减中均被跳过；
  - 手工抓取审批数据改为按员工解析流程编码；
  - 全员抓取后也会按实际解析员工范围清理陈旧审批快照。
- 前端修复：
  - 审批数据列表新增“操作”列；
  - `statisticsStatus=取消至统计` 时显示“添加至统计”，其他状态显示“取消至统计”；
  - 操作调用本地状态更新接口，成功后更新当前行，失败恢复旧状态。
- 数据库变更：
  - 新增幂等脚本 `docs/sql/2026-07-13_tbattendanceapprove_statistics_status.sql`；
  - dev MySQL `153.0.237.98` 已执行脚本并验证 `hr_0001` 至 `hr_0005` 均存在 `statisticsStatus varchar(20) NULL`；
  - 其他目标库未执行脚本前，包含 `statisticsStatus` 的列表查询会因缺列失败。

## 2026-07-13 Verification
- `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`：通过，81 个测试，0 失败。
- `node tests/attendance-approval-api.test.mjs`：通过。
- `node tests/attendance-approval-dialog.test.mjs`：通过。
- `node tests/attendance-approval-fetch-utils.test.mjs`：通过。
- `npm run build`：通过，仅保留既有 `::v-deep` 与 chunk size warning。

## 2026-07-13 Follow-up: 钉钉用户不存在
- 用户现场反馈：点击“获取审批数据”时日志显示“管理员审批模板查询失败，回退按员工可见模板解析流程编码”，钉钉接口返回“用户不存在”。
- 根因定位：
  - 管理员模板接口的“用户不存在”会按既有设计回退到员工可见模板；
  - 回退后的 `topapi/process.listbyuserid` 若对某个员工 `userId` 返回 `400023/用户不存在`，说明本地 `tbattendanceuser.userId` 在钉钉侧已失效或员工已离职；
  - 原实现会将该异常翻译为通用“获取审批数据失败，请稍后重试”并中断整个抓取。
- 修复：
  - 多员工/全员抓取时跳过钉钉用户不存在的员工，继续抓取其他员工；
  - 跳过员工不写完成标记，也不参与陈旧审批快照清理；
  - 若本次命中的员工全部不存在，返回明确提示“钉钉用户不存在或已离职，请先同步员工钉钉用户后重试：...”。
- 验证：
  - RED：新增两个 workflow 测试后，当前实现中断抓取并失败；
  - GREEN：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched test` 通过，2 个测试 0 失败；
  - 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，83 个测试 0 失败。

## 2026-07-13 Follow-up: 单选员工抓取顺序问题
- 用户反馈：单选某员工时抓不到审批，但切换“全部员工”后却能抓到。
- 根因定位：
  - 同一员工在 `tbattendanceuser` 中存在多条映射时，原实现只取仓库返回的第一条；
  - `findAllByEmpIdIn` 与 `findAll` 的返回顺序并不稳定，单选和全选可能命中不同的 `userId`；
  - 这会造成“单选抓不到、全选抓得到”的错觉，本质是映射顺序不稳定。
- 修复：
  - 现已按 `createTime` 优先、`id` 次优先选择同一员工的最新有效映射；
  - 单选和全选都改为稳定使用最新有效 `tbattendanceuser.userId`，不再依赖数据库返回顺序。
- 验证：
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched+fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees test` 通过，3 个测试 0 失败；
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，84 个测试 0 失败。

## 2026-07-13 Follow-up: 按员工表钉钉ID和姓名手机号补映射
- 用户补充要求：
  - 单个员工、批量员工、全量员工获取审批数据都应先从员工表解析员工；
  - 员工表有钉钉 ID 时直接抓取；
  - 员工表没有钉钉 ID，或当前钉钉 ID 已失效时，按员工姓名 + 手机号作为唯一键去钉钉花名册查询，唯一命中后保存到 `hrm_employee.dingtalk_user_id`，再抓取审批；
  - 每次抓取应以新落库的审批数据为准，不保留同员工同月同类型本次未返回的旧审批。
- 实现结论：
  - `fetchMonthData(...)` 获取 token 后调用新的 `resolveTargetUsers(employeeIds, token)`，单选、批量、全量都会进入同一解析流程；
  - 该流程读取 `hrm_employee` 在职员工，优先使用 `HrmEmployee.dingtalkUserId`；
  - 缺失钉钉 ID 时调用钉钉在职员工接口和员工花名册接口，按姓名 + 手机号严格唯一匹配；
  - 唯一命中后写回员工表，并同步保存或修正 `tbattendanceuser` 映射；
  - 未命中或重复命中时不猜测，避免审批数据串人。
- 抓取失败恢复：
  - 当 `process.listbyuserid` 或 `processinstance/listids` 返回用户不存在时，会按员工表强制刷新钉钉 ID；
  - 刷新后若 ID 变化，则用新 ID 重试该员工本次抓取。
- 旧审批清理：
  - 抓后清理会通过 `collectApprovalCleanupUserIds(...)` 收集同一员工所有历史 `tbattendanceuser.userId`；
  - 只对本次成功抓取完成的员工清理同月同审批类型旧快照；
  - 这样换过钉钉 ID 的员工不会继续保留旧 userId 下的旧审批。
- 验证：
  - RED：新增两个 workflow 测试后，旧实现无法在缺少 `tbattendanceuser` 映射时使用员工表钉钉 ID，也不会按姓名手机号反查并写回；
  - GREEN：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test` 通过，2 个测试 0 失败；
  - 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，86 个测试 0 失败。

## 2026-07-13 Follow-up: 吴晓霞仍提示用户不存在排查
- dev 库 `hr_0003` 当前员工主数据：
  - `employee_id=1831601326890434565`；
  - `employee_name=吴晓霞`；
  - `mobile=13268968337`；
  - `hrm_employee.dingtalk_user_id=262756571521566047`；
  - `tbattendanceuser` 也只有同一个 `userId=262756571521566047`。
- 本地 9080 运行态确认：
  - `HrmAttendanceApprovalSyncServiceImpl.class` 已在 `2026-07-13 19:43:38` 编译到 `target/classes`；
  - 运行进程 `45879` 的 classpath 指向 `/Users/jiangyongming/Project/hr/hainan/target/classes`，即本地服务加载的是新编译产物。
- 本地接口复现：
  - 使用 `hbadmin` 登录本地 `127.0.0.1:9080`，请求 `POST /hrsystem/hrmAttendanceApproval/fetchMonthData`；
  - 请求体：`{"month":"2026-06","employeeIds":[1831601326890434565],"approvalTypes":["overtime"]}`；
  - 返回：`{"success":true,"month":"2026-06","insertedCount":"3"}`，未出现“用户不存在”。
- 结论：
  - 在本地 dev 库 + 本地新代码下，吴晓霞当前钉钉 `userId` 可以成功抓取审批；
  - 若页面仍提示“用户不存在：吴晓霞”，优先怀疑请求打到了未发布/未重启的新后端之外的环境，或该环境中吴晓霞员工手机号、钉钉 ID 与本地 dev 库不一致；
  - 只有当钉钉旧 `userId` 不存在，且按该环境员工表的姓名 + 手机号无法在钉钉在职花名册唯一匹配时，新逻辑才会继续给出用户不存在/无法匹配的提示。
- 用户确认页面 payload 为：
  - `month=2026-06`；
  - `employeeIds=["1831601326890434565"]`；
  - `approvalTypes=["all"]`。
- 复用该 exact payload 通过 `http://127.0.0.1:8081/api/hrsystem/hrmAttendanceApproval/fetchMonthData` 调用本地代理，返回成功：`insertedCount=4`，没有用户不存在。
- 抓取后 `hr_0003.tbattendanceapprove` 中吴晓霞 `2026-06` 本地审批快照为 4 条：
  - 3 条 `tagName=加班`；
  - 1 条 `tagName=请假/subType=调休`；
  - 因此 `approvalTypes=all` 的 4 条不等于 4 条加班。

## 2026-07-14 Follow-up: 李明明管理员模板回退日志
- 用户现场选择李明明后看到日志“管理员审批模板查询失败，回退按员工可见模板解析流程编码（adminUserId 为钉钉管理员，不是当前员工）”。
- 代码口径确认：
  - 前端员工模式使用右侧穿梭框 `fetchSelectedEmployeeIds` 生成 `employeeIds`；
  - 部门模式使用右侧部门展开后的 `fetchDeptEmployeeIds` 生成 `employeeIds`；
  - 后端 `fetchMonthData(...)` 先按 `employeeIds` 解析目标员工，再逐个打印 `审批抓取员工映射` 并用员工 `userId` 调 `process/listbyuserid`。
- `hr_0003` 数据复核：
  - 在职李明明：`employee_id=1831601326890434563`、手机号 `17389819211`、`dingtalk_user_id=024662561026250638`、`is_del=0`；
  - 离职/删除李明明：`employee_id=2033769831940079620`、手机号 `18889692758`、`dingtalk_user_id=NULL`、`is_del=1`；
  - `tbattendanceuser` 只有在职李明明映射：`empId=1831601326890434563`、`userId=024662561026250638`；
  - `tbattendanceapprove` 已有该 `userId` 的 5 条本地审批快照；
  - 最近钉钉在职员工列表日志包含 `024662561026250638`。
- `hrsystem.ddAccount` 中 `companyId=0003` 的 `adminId=296842114330963873`；该管理员触发管理员模板回退不代表当前选择员工李明明失败。
- 判断标准：
  - 只看到管理员模板回退日志不能判定失败；
  - 后续若出现 `审批抓取流程编码解析结果 ... employeeId=1831601326890434563, dingTalkUserId=024662561026250638`，说明已经按李明明继续抓取；
  - 若后续 `process/listbyuserid` 对 `024662561026250638` 报错，才是李明明员工映射或权限问题；
  - 若日志没有 `employeeUserId=` 参数，说明运行后端不是包含 2026-07-14 回退日志增强的新包。
# Findings: 薪资管理开始核算穿梭框改造

## 2026-07-16 Pre-work
- 后端项目 `hainan` 文档中已有约束：薪资核算入口为 `/hrmSalaryMonthRecord/computeSalaryData`，进度查询为 `/hrmSalaryMonthRecord/queryComputeSalaryProgress`，计薪员工列表为 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList`。
- 后端文档反复强调员工匹配不得只按姓名，遇到同名员工需要姓名 + 手机号或更强唯一键；本轮薪资核算选择器也应展示并按员工 ID/姓名+手机号区分。
- 前端项目 `hr_web` 文档中已有薪资管理页真实进度弹层要求，且员工同步考勤穿梭框已复用薪资核算同口径员工列表。
- 前端文档已有类似“按员工/按部门”选择模式和穿梭框工具经验：审批数据、员工同步考勤、批量参保均保持员工雪花 ID 字符串，不做 `Number()` 转换。
- 当前工作区已有大量既有变更；本轮只应触碰薪资核算入口、相关测试和文档，不整理无关文件。

## 2026-07-16 Implementation Findings
- 前端 `SalaryManage.vue` 已删除页面可见的“核算薪资”和“单人核算”双按钮，只保留“开始核算”。
- 旧 `AloneComputeDialog.vue` 已改造成“开始核算”弹窗，支持“按人员 / 按部门”两种模式。
- 前端新增 `salary-compute-scope-utils.js`：
  - 员工选项按 `employeeId` 提交，标签展示 `姓名（手机号） - 部门`；
  - 部门树可扁平化供旧穿梭框使用，也可归一化为树结构供部门选择面板展示；
  - 按部门模式会包含所选部门及下级部门员工；
  - 显式选择范围超过 50 人时前端直接拦截。
- 后端 `computeSalaryData` 新增 `employeeIds` 参数，优先使用批量员工 ID；旧 `employeeId` 参数继续兼容。
- 后端空 `employeeIds` / 未传员工参数保持原全员核算；显式选择范围超过 50 人时抛 `HrmException`。
- `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 补充返回 `a.mobile as mobile`，支撑前端同名员工展示。
- 核算进度查询同步支持 `employeeIds`，保证批量核算时进度 key 与发起任务一致。

## 2026-07-17 Department Tree Findings
- 用户补充要求明确为“按部门的穿梭框用树结构展示”，因此部门模式不应继续使用扁平部门 `el-transfer`。
- 前端已新增 `normalizeSalaryComputeDeptTree(...)`，把后端可能返回的 `deptId/dept_id/id` 与 `name/deptName/dept_name/label` 统一为 Element Tree 节点 `{ deptId, label, children }`。
- `AloneComputeDialog.vue` 部门模式现为两栏树形选择：
  - 左侧 `el-tree` 展示完整部门层级，使用 `show-checkbox` 和 `node-key="deptId"`；
  - 右侧 `el-tree` 展示已选部门的层级路径；
  - 右侧为空时仍按原全员核算处理；
  - 右侧有部门时仍由 `collectSalaryComputeDeptEmployeeIds(...)` 展开所选部门及下级部门员工，再执行 50 人上限校验。
- 人员模式未调整，继续使用员工 `el-transfer`；后端接口和薪资核算范围 payload 未新增字段。
# Findings: 薪资核算失败集中提示

## 2026-07-17 Pre-work
- 后端项目文档已有薪资核算真实进度要求：`computeSalaryData` 执行期间返回百分比、阶段和可读错误状态。
- 后端项目文档已有“部门批量查询返回 VO 转换异常”和“社保数据未生成但存在有效员工明细时应允许核算”的历史问题记录，这两类都属于薪资核算前置数据异常不能直接中断接口的场景。
- 本轮新增目标：对核算前可发现的数据/配置问题尽量集中校验并一次性返回到进度窗口，减少用户重复修一项、再执行、再发现下一项的工作量。

## 2026-07-17 Root Cause
- 报错点位于 `SalaryMonthRecordServiceNew#getOrCreateRecordAndApplyAttendance(...)`，约 2098 行：新建员工月薪资记录时直接执行 `new BigDecimal(cv.get(2))`，`cv` 来自 `attendanceDataMap.get(jobNumber)`。
- `resolveAttendanceData(...)` 只会给“有工号”的员工初始化默认考勤数据；员工工号为空时不会放入 `attendanceDataMap`。
- 当员工工号为空，或考勤同步/考勤汇总阶段未给该工号生成 `1=应出勤天数`、`2=实际出勤天数` 时，`cv` 或 `cv.get(1/2)` 为空，导致空指针或数字转换异常。
- 当前异常被 `catch (Exception e) { throw new HrmException(HrmCodeEnum.ATTENDANCE_DATA_ERROR); }` 包成泛化“考勤数据错误”，没有告诉用户具体是哪个员工、缺了什么。

## 2026-07-17 Salary Compute Risk Scan
- 缺工号：计薪员工没有工号时，默认考勤数据不会生成，随后按工号取考勤数据会失败。
- 缺考勤天数：考勤 map 缺少 `1` 或 `2`，或值不是数字，会在员工月记录写入应出勤/实际出勤时失败。
- 缺考勤规则：`hrmAttendanceRule` 为空时，迟到、早退、缺卡扣款规则读取会失败。
- 缺基本工资设置或字段：加班费 `overtimePay`、夜班补贴 `subsidy`、最低基本工资 `salaryBasic` 为空时，生产员工加班/夜班/病假扣款计算可能失败。
- 生产员工缺排班工时或异常日数据：生产体系请假扣减依赖每日排班工时和日考勤 `workDate`，空数据可能导致请假扣减计算异常或结果不可信。
- 计薪员工基础字段异常：员工姓名为空、部门信息为空、岗位为空等虽然部分 SQL 已兜底，但仍会影响错误提示和部分特殊规则判断。
- 社保前置数据：已有 `validateInsuranceData(...)` 会校验社保月记录；若开启同步社保且目标年月主记录缺失仍会失败，应并入集中提示。

## 2026-07-17 Implementation Findings
- 后端进度返回对象 `SalaryComputeProgressVO` 已增加 `errors`，前端可在进度窗口直接读取失败明细。
- `collectComputePrerequisiteErrors(...)` 已覆盖可提前发现的员工和配置问题：缺员工 ID、缺姓名、缺工号、缺考勤汇总、应/实际出勤为空或不是数字、缺考勤扣款规则、缺最低基本工资、缺每小时加班费、缺夜班补贴。
- `collectInsuranceDataErrors(...)` 保留原社保校验行为，同时允许薪资核算准备阶段把社保月记录缺失或无有效员工社保明细的问题并入同一次失败提示。
- `getOrCreateRecordAndApplyAttendance(...)` 已从直接读取 `cv.get(1/2)` 改为显式校验考勤 map 和数字字段，运行期仍遇到脏数据时会提示员工 ID、工号和具体字段。
- 前端 `SalaryManage.vue` 已改为失败时保持“核算薪资进度”窗口打开，逐条展示 `computeProgressErrors`；全局 `ElMessage.error(errorMessage)` 不再作为核算失败明细的主要承载方式。
# Findings: 考勤汇总同步漏员工排查

## 2026-07-17 Initial Context
- 用户反馈：考勤汇总中执行“同步考勤”时，有些员工没有同步过来，样例员工为“潘红琼”。
- 项目需求中与本问题相关的既有约束：
  - 考勤同步必须避免只按姓名匹配，优先使用员工 ID、工号、手机号、部门和钉钉 `userId` 区分员工。
  - `tbattendanceuser` 映射去重应以 `userId` 为主，禁止按“同名 + 当前周期无排班”破坏性清理。
  - 审批重抓或映射补齐后，既有加班/夜班统计明细不会自动重算，目标员工或月份需重新执行统计。
  - 考勤汇总同步应从加班/夜班统计月度汇总取数，并按 `employeeId + year + month` upsert `hrm_produce_attendance`。

## 2026-07-17 Root Cause Evidence
- `hr_0003 / 2026-06` 只读核对：
  - 活跃员工 `113` 人；
  - `hrm_overtime_night_statistics_detail` 有统计员工 `113` 人；
  - `hrm_produce_attendance` 有考勤汇总员工 `113` 人；
  - 活跃员工中没有“统计有但汇总无”的缺行员工。
- 潘红琼本地数据：
  - `hrm_employee.employee_id=1831601326890434570`，工号 `TYNG-437`，手机号 `19820594095`，部门 `行政人力资源部`，`affiliation_system=2`；
  - `tbattendanceuser` 映射正常：`userId=02533200433128427058`；
  - `hrm_overtime_night_statistics_detail` 2026-06 有 `21` 条日明细，汇总加班 `98.27h`，应出勤 `25` 天、实际 `21` 天、应计 `200h`；
  - `hrm_produce_attendance` 2026-06 已有汇总行，实际出勤 `21.00` 天、应计 `25.00` 天、加班 `0.00`。
- 复现“看起来没同步”的筛选差异：
  - 不带部门筛选查潘红琼：`1` 条；
  - `department=1` 查潘红琼：`1` 条；
  - `department=2` 查潘红琼：`0` 条。
- 根因假设：
  - 考勤汇总同步插入新行时使用部门名称关键字推断 `hrm_produce_attendance.department`，没有优先使用员工 `affiliation_system`；
  - 已有行同步时不更新 `employeeName/department` 等归属字段，导致既有错误分类重新同步也不会被纠正；
  - 因此潘红琼这类 `affiliation_system=2` 但部门名称为“行政人力资源部/质量管理部”的员工，会在“生产部门”筛选下看起来没有同步。

## 2026-07-17 Implementation Evidence
- `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 现对新增行和已有行统一刷新员工姓名与部门类型。
- `resolveDepartmentType(...)` 优先使用员工档案 `affiliationSystem=1/2`，只有未维护所属体系时才回退部门名称关键字推断；空所属体系保持原兼容行为。
- 新增回归测试 `HrmProduceAttendanceServiceImplTest#syncFromOvertimeNightStatistics_shouldRefreshExistingDepartmentFromEmployeeAffiliationSystem`：
  - RED 时更新对象仍为 `department=1`；
  - GREEN 后更新对象变为 `department=2`。
- 回归验证：`HrmProduceAttendanceServiceImplTest` 全类 7 个测试通过。

## 2026-07-17 Follow-up: 远端字段为空
- 用户反馈“还是没有”后复核远端接口：
  - `POST /hrmProduceAttendance/queryMonthAttendanceList`，条件 `year=2026/month=6/employeeName=潘红琼/department=2` 返回 `total=1`，说明员工行已经存在于生产部门筛选结果中；
  - 同一响应中 `actualAttendance=null`、`accruedAttendance=null`、`overtimePay=null`，前端表格绑定这些字段，因此页面看起来像同步数据为空；
  - 数据库同一行 `hr_0003.hrm_produce_attendance` 为 `positive_attendance=21.00`、`probation_attendance=25.00`、`overtime_pay=0.00`。
- 本地可发布 JAR 复核：
  - 源码与 `target/classes` 中 mapper 已有 `positive_attendance as actualAttendance`；
  - 但旧 `target/hrsystem-0.0.1-SNAPSHOT.jar` 内 `HrmProduceAttendanceMapper.xml` 仍是 `select * from hrm_produce_attendance`，会导致列表 VO 拿不到 `actualAttendance/accruedAttendance`。
- 修复：
  - `HrmProduceAttendanceMapper.xml#queryProduceAttendanceList` 现在对所有前端展示字段使用显式别名，包括 `summaryId/employeeId/employeeName/actualAttendance/accruedAttendance/workOverTime/overtimePay/nightShift/nightSubsidy` 等；
  - 新增 `HrmProduceAttendanceMapperXmlTest`，禁止列表查询退回 `select *`，并校验核心字段别名；
  - 已重新执行 `mvn ... -DskipTests package` 生成新 JAR，包内 mapper 已确认是显式别名版本。

## 2026-07-17 Follow-up: 本地 8081 复核
- 用户确认“本地没有”后，复核本地运行态：
  - 本地后端 `127.0.0.1:9080` 为 IntelliJ 启动的 Java 进程，classpath 指向 `/Users/jiangyongming/Project/hr/hainan/target/classes`，不是旧 JAR；
  - 本地前端 `127.0.0.1:8081` 由 `hr_web/scripts/local-deploy-server.mjs` 服务 `hr_web/html`，代理 `/api` 到本机 `9080`；
  - `hr_web/html/assets/Upload-BbctS4ME.js` 已包含“考勤汇总”、`actualAttendance/accruedAttendance` 字段绑定和 `/hrmProduceAttendance/syncFromOvertimeNightStatistics` 调用。
- 本地接口取证：
  - `POST http://127.0.0.1:8081/api/hrsystem/hrmProduceAttendance/queryMonthAttendanceList`，条件 `2026-06 + 潘红琼 + department=2` 返回 `total=1`，`actualAttendance=21.00`、`accruedAttendance=25.00`；
  - 同一条件改为 `2026-07` 时，未执行同步前考勤汇总返回 `total=0`；
  - 上游 `hrmOvertimeNightStatistics/queryPageList` 中 `2026-07 + 潘红琼` 已存在，`actualAttendanceHours=216.00`、`accruedAttendanceHours=216.00`。
- 本地执行同步：
  - 调用 `POST /hrmProduceAttendance/syncFromOvertimeNightStatistics`，请求 `{"month":"2026-07"}`，返回 `data=113`；
  - 同步完成后再查 `2026-07 + 潘红琼 + department=2` 返回 `total=1`，`actualAttendance=27.00`、`accruedAttendance=27.00`、`workOverTime=0.00`、`overtimePay=0.00`；
  - 结论：本地“7 月没有”是同步前 `hrm_produce_attendance` 尚未生成目标月份记录；同步完成后接口和当前静态包均能显示该员工。

## 2026-07-17 Follow-up: 界面 DOM 复核
- 通过无界面 Chrome 打开真实本地页面 `http://127.0.0.1:8081/#/hrm/attendance/upload`，设置登录 token 和 `user_info` 后复核页面：
  - 初始页面确实加载的是 `考勤汇总`，当前静态入口引用 `Upload-BbctS4ME.js`；
  - 在页面搜索框输入并触发 `潘红琼` 查询，页面实际发出请求体 `{"employeeName":"潘红琼","year":"","month":"","department":"","limit":15,"page":1}`；
  - 响应返回 3 条：`2025-12 department=1`、`2026-06 department=2`、`2026-07 department=2`；
  - DOM 表格中存在 3 行潘红琼，输入框值分别包含 `2026-06 actual/accrued=21/25` 和 `2026-07 actual/accrued=27/27`。
- 进一步复核筛选差异：
  - `2026-07 + department=1 + 潘红琼` 返回 `total=0`；
  - `2026-07 + department=2 + 潘红琼` 返回 `total=1`。
- 这说明如果界面筛选为“行政部门”，会看不到潘红琼；当前同步规则按员工档案 `affiliation_system=2` 将她展示在“生产部门”，即使她的部门名称是“行政人力资源部”。

## 2026-07-17 Follow-up: 7月汇总测试数据清理
- 用户明确要求“删除 7 月的考勤汇总，别的数据不要动”，本次仅操作租户库 `hr_0003.hrm_produce_attendance`。
- 删除前证据：
  - `hrm_produce_attendance` 中 `year=2026/month=7` 共有 `113` 行；
  - 潘红琼 7 月汇总存在，`department=2`，`positive_attendance=27.00`，`probation_attendance=27.00`；
  - 6 月汇总共有 `113` 行，潘红琼 6 月汇总为 `21.00/25.00`。
- 执行语句限定为 `DELETE FROM hrm_produce_attendance WHERE year=2026 AND month=7`；事务结果为删除前 `113`、删除行数 `113`、删除后 `0`。
- 删除后验证：
  - 数据库 `hrm_produce_attendance` 中 `2026-07` 汇总为 `0` 行；
  - 数据库 `2026-06 + 潘红琼` 仍存在，`department=2`，`positive_attendance=21.00`，`probation_attendance=25.00`；
  - 本地 8081 接口查询 `2026-07 + department=2 + 潘红琼` 返回 `records=[]/total=0`；
  - 本地 8081 接口查询 `2026-06 + department=2 + 潘红琼` 返回 `total=1`，`actualAttendance=21.00`，`accruedAttendance=25.00`。
- 本次未执行对上游加班/夜班统计、审批快照、员工档案或其他月份汇总的删除/更新语句。
## 2026-07-17 薪资行政/生产体系硬编码移除

- 薪资主链路原有三类硬编码：
  - `SalaryMonthRecordServiceNew#getDeptTypeForEmployee(...)` 按特定员工 ID 强制行政/生产体系；
  - `HrmSalaryMonthEmpRecordMapper.xml` 按部门名称列表派生 `isProduceDept`；
  - `exportSalaryNew(...)` 另维护一份同样的员工 ID 例外名单。
- `SalaryMonthRecordService_Bak` 虽然文件名带 `Bak`，但仍被 `SalaryComputeTask` 和 `HrmSalarySlipRecordService` 注入，是活动 Spring Bean；本轮不能忽略其中的旧体系判断。
- 考勤汇总链路存在同类问题：
  - `HrmProduceAttendanceServiceImpl#resolveDepartmentType(...)` 原先员工 `affiliationSystem` 缺失时回退按部门名称关键字推断；
  - 生产考勤 Excel 导入路径也按部门名称关键字写入 `department`。
- 处理后的统一口径：
  - `hrm_employee.affiliation_system=1` -> 行政体系；
  - `hrm_employee.affiliation_system=2` -> 生产体系；
  - 工资内部 `deptType` 仍保持旧契约：行政 `0`，生产 `1`；
  - 员工档案未维护所属体系时按行政体系兜底，不再按部门名、岗位名或员工 ID 猜测。
- 保留的中文“生产部/仓库”等命中项来自接口文案、枚举名称、实体说明或历史备份说明，不再参与本轮行政/生产体系判断。

# Findings: 员工管理唯一性与表单字段调整

## 2026-07-17 Pre-work
- 已读取项目 `docs/requirements.md` 与 `docs/development.md`。
- 已读取前端项目 `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md` 与 `docs/development.md`。
- 既有文档约束强调员工匹配、导入、审批和薪资相关流程不得只按姓名，需要使用员工 ID、工号、手机号、身份证号等唯一信息区分。
- 前端文档中的旧员工管理需求曾要求新增“是否有全勤 / 是否加入钉钉”，本轮用户要求删除新建和编辑页面中的这两个字段，属于对旧需求的显式覆盖。
- 本轮新增要求：
  - 员工管理列表的导入、新增、修改需要对工号、手机号、身份证号做唯一处理；
  - 新建和编辑页面删除“是否有全勤”和“是否加入钉钉”项。
- 初始假设：唯一性按未删除员工生效，编辑时排除当前员工，空值不参与唯一性校验。

## 2026-07-17 Initial Discovery
- 后端员工管理候选文件：
  - `src/main/java/com/tianye/hrsystem/controller/HrmEmployeeController.java`
  - `src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java`
  - `src/main/java/com/tianye/hrsystem/mapper/HrmEmployeeMapper.java`
  - `src/main/resources/mapper/HrmEmployeeMapper.xml`
  - `src/test/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImplImportEmployeeTest.java`
  - `src/test/java/com/tianye/hrsystem/controller/HrmEmployeeControllerTest.java`
- 前端员工管理候选文件：
  - `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/employee/Components/AddOrEdit.vue`
  - `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/employee/Components/DetailAddOrEdit.vue`
  - `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/employee/model/employee.js`
  - `/Users/jiangyongming/Project/hr/hr_web/tests/employee-edit-save-regression.test.mjs`
  - `/Users/jiangyongming/Project/hr/hr_web/tests/employee-import-result-ui.test.mjs`

## 2026-07-17 Code Discovery
- 新增员工入口 `HrmEmployeeController#addEmployee -> HrmEmployeeServiceImpl#add(...)`。
- `HrmEmployeeServiceImpl#add(...)` 中旧工号重复校验被注释，且当前会把前端传入的 `jobNumber` 覆盖为 `System.currentTimeMillis()` 字符串；这与“新增修改工号唯一处理”和前端工号必填要求冲突。
- 花名册导入入口 `HrmEmployeeController#importEmployee -> HrmEmployeeServiceImpl#importEmployee(...)`：
  - 当前按“姓名 + 个人电话/手机”构建 `employeeMap`，匹配到则更新，未匹配则新增；
  - 同一文件中只校验“姓名 + 手机号”重复；
  - 尚未对导入文件内和系统未删除员工中的工号、手机号、身份证号做全局唯一校验。
- 前端新增/再次入职弹窗 `AddOrEdit.vue`：
  - 初始表单包含 `fullAttendance=1`、`expandProduction=1`；
  - 模板渲染“是否有全勤”和“是否加入钉钉”两个 `el-radio-group`；
  - 编辑详情岗位信息可能另有动态表单字段，需要继续定位是否属于用户所说“编辑页面”。

## 2026-07-17 Review Follow-up
- `updateCommunication(...)` 原先会把通讯信息里的固定字段直接组装为 `HrmEmployee` 并 `updateById(...)`，没有复用 `validateEmployeeUniqueFields(...)`；如果手机号在通讯页可编辑，会绕过“修改手机号唯一”规则。
- `importEmployee(...)` 原先在读取 Excel 行时边校验边保存，同一文件第二行才发现工号/手机号/身份证号重复时，第一行已经可能保存；修复方向改为先构建待导入记录并用已校验行集合作为额外查重范围，整份文件通过后再落库。
- 空工号仍按“空值不参与唯一性校验”处理；导入文件未填写工号时不再在校验后生成时间戳工号，避免生成值绕过预检。

## 2026-07-20 Salary Export Scope Initial Context
- 当前项目根目录：`/Users/jiangyongming/Project/hr/hainan`。
- 已存在项目文档：`docs/requirements.md`、`docs/development.md`。
- 已存在计划记录：`task_plan.md`、`findings.md`、`progress.md`。
- 本轮需求：薪资导出时添加选择人员和部门界面，参考“开始核算”功能界面和控件逻辑。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 文档中已有“薪资管理开始核算范围选择”规则：按人员/按部门两种模式，未选范围时全员，选中范围时以员工 ID 核算，单次最多 50 人，部门选择必须是树结构并包含子部门员工。
- 薪资导出相关文档要求工资列表/导出涉及行政/生产体系时统一读取员工档案 `affiliation_system`，且导出异常不能被 controller 吞成空白下载。
- 本轮导出选择范围应优先复用开始核算范围选择的 UI 与工具逻辑，默认未选范围保持现有全量导出行为。
- UI 约束：这是数据密集型后台管理界面，应沿用现有 Element Plus 表单/弹窗/树/穿梭框，不引入新的视觉风格、配色或字体；交互需有 loading/disabled、明确错误提示和可键盘访问的表单控件。
- 前端薪资导出入口：`hr_web/src/views/hrm/salary/salary/SalaryManage.vue#handleExportSalary`，按钮文案为“导出薪资”。
- 前端开始核算弹窗：`hr_web/src/views/hrm/salary/salary/components/AloneComputeDialog.vue`。
- 前端范围选择工具：`hr_web/src/views/hrm/salary/salary/components/salary-compute-scope-utils.js`，已有员工/部门范围归一化和 payload 组装逻辑。
- 前端薪资 API：`hr_web/src/api/hrm/salary/salary.js#exportSalary`，当前对 `/hrmSalaryMonthRecord/exportSalary` 发起 blob 下载请求。
- 后端导出入口：`src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryMonthRecordController.java#exportSalary`，调用 `SalaryMonthRecordServiceNew#exportSalaryNew(...)`。
- 后端导出服务：`src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java#exportSalaryNew(...)`；需继续确认 DTO 是否已有员工/部门字段。
- `QuerySalaryExportDto` 当前只有 `salaryRecordId/employeeId/deptId/type/employeeName`，不支持人员集合。
- `SalaryManage.vue#handleExportSalary` 当前直接调用 `exportSalary(...)`，不会打开选择范围弹窗。
- `AloneComputeDialog.vue` 目前逻辑已满足范围选择需求，但文案和提交按钮固定为“核算”；可以参数化后复用给“导出薪资”。
- `exportSalaryNew(...)` 当前先构造全量候选员工 ID，再调用 `salaryMonthEmpRecordMapper.querySalaryMonthList(queryDto, employeeIds)`；新增导出范围时可在服务层把候选员工 ID 与 DTO 的 `employeeIds` 取交集，让 mapper 现有 `employeeIds` 过滤继续生效。
- 实现后导出规则：显式选择人员或部门时，前端只提交 `salaryRecordId + employeeIds`；未选择范围时保留当前页面 `employeeName/jobNummber/deptId` 筛选条件。
- 后端 `resolveExportEmployeeIds(...)` 保留候选员工顺序并与选择员工取交集；选择 ID 不在候选范围内时导出结果为空，不越权扩大范围。

# Findings: 采购计划部超缺勤工资排查

## 2026-07-20 Pre-work
- 已读取 `docs/requirements.md`、`docs/development.md` 和既有 `task_plan.md/findings.md/progress.md`。
- 与本轮直接相关的既有约束：
  - 薪资核算中原“实际出勤天数”的业务口径已统一改为“应计出勤天数”，字段名 `actual_work_day` 保持兼容。
  - 薪资导出“满勤天数/超缺勤天数”读取 `hrm_overtime_night_statistics_detail` 的应出勤与应计出勤落库结果。
  - 加班/夜班统计中行政体系和生产体系的应出勤、实际出勤、应计出勤口径近期有多次调整，排查必须对齐员工部门、体系和统计明细。
  - 涉及员工范围时应以员工 ID、工号、手机号、部门 ID 等稳定字段定位，不按姓名猜测。
- 本轮先查根因，不先改代码或工资数据。

## 2026-07-20 Code Trace
- “超缺勤工资”对应实际工资项 `200101 / 超缺勤`；导出里的 `9008 / 超缺勤天数` 是另一个展示天数字段，不是本次工资金额来源。
- `SalaryMonthRecordServiceNew#getFixedOptionValue(...)` 会把考勤扣款子项累加为 `200101`，参与累加的扣款项包括迟到、早退、旷工、病假、事假、缺卡、综合扣款等；`180101/180102/40102/281/280/282/20102/20105` 等加班、夜班、全勤、补贴和其他扣款不计入该合计。
- `190103 / 旷工扣款` 的当前计算公式为：`empSalary / normalDays * absenteeismDays`；`absenteeismDays` 在有钉钉月度汇总时会取 `normalDays - empAttendanceSummary.actualityDays`。
- `normalDays` 来自 `loadNormalDaysByDeptType(...) -> hrm_attendance_info.actual_work_day`；目标年月和体系缺配置时回退代码默认 `21.75`。
- `actualityDays` 当前会被 `resolveSalaryAttendanceDays(...)` 覆盖为薪资“应计出勤天数”，优先读取 `hrm_produce_attendance.probation_attendance`，缺失时才读取加班/夜班统计 `accrued_attendance_hours / 8`。

## 2026-07-20 Database Evidence
- 目标租户确认：`companyId=0003`，公司名“湖北公司”，租户库 `hr_0003`。
- 采购计划部部门：`dept_id=2033769625647431684`，父级 `1481534121629855747`。
- 最新有数据的薪资主记录：`s_record_id=2077807285177995265`，标题“六月薪资报表”，年月 `2026-06`，员工数 `95`。
- 采购计划部当前有 6 名未删除员工，其中 3 名有工号并进入 2026-06 薪资：严锦 `TYNG-332`、庞龙斌 `TYNG-445`、黎冬霜 `TYNG-134`；李灏铭、罗盛浮、陈潜无工号且未进入该薪资记录。
- 三名进入薪资的采购计划部员工数据一致呈现：
  - 薪资月记录 `need_work_day=21.75`、`actual_work_day=23.00`。
  - `200101 / 超缺勤` 全部非零且为负数；来源子项全部是 `190103 / 旷工扣款`，其他迟到/早退/事假/病假/缺卡均为 `0`。
  - 严锦、庞龙斌 `200101=-229.89`，定薪总额 `4000`；黎冬霜 `200101=-252.87`，定薪总额 `4400`。
  - 金额按代码公式闭合：严锦/庞龙斌 `4000 / 21.75 * (21.75 - 23) = -229.89`；黎冬霜 `4400 / 21.75 * (21.75 - 23) = -252.87`。
- `hr_0003.hrm_attendance_info` 当前没有 `2026-06` 配置行；最近只查到 `2026-01` 和 `2025-12/11` 等历史配置，因此薪资核算回退默认应出勤 `21.75`。
- 同一批采购计划部员工的其他考勤来源显示 6 月应出勤不是 `21.75`：
  - `hrm_overtime_night_statistics_detail`：三人 `expected_attendance_days=23`、`accrued_attendance_hours=184.00`，即应计出勤 `23` 天。
  - `hrm_attendance_report_data`：三人月度 `应出勤天数=23`；出勤天数分别为严锦 `22`、庞龙斌 `23`、黎冬霜 `22`。
  - `hrm_produce_attendance`：三人 `probation_attendance=23`，该字段被薪资核算优先用作应计出勤天数。
- 全公司 2026-06 并非采购计划部独有：95 名薪资员工中 87 名 `200101` 非零、84 名为负数，说明根因是 2026-06 应出勤配置缺失与核算口径混用导致的系统性问题；采购计划部因三名参与核算员工都满足 `actual_work_day=23 > need_work_day=21.75`，所以看起来“都有超缺勤工资”。

# Findings: 删除薪资核算 21.75 硬编码

## 2026-07-20 Pre-work
- 已读取项目需求/开发文档、采购计划部排查记录和现有计划文件。
- 本轮新增业务决定：允许按“应出勤也统一读取加班/夜班统计落库结果”的方案修改；薪资核算代码中所有硬编码 `21.75` 的调用必须删除。
- 初步扫描命中：
  - `SalaryMonthRecordServiceNew` 中 `DEFAULT_NORMAL_DAYS=21.75`、`DEFAULT_NORMAL_DAYS_DECIMAL`、缺 `hrm_attendance_info` 时回退默认值、无考勤组分支写入 `"21.75"`。
  - `SalaryMonthRecordService_Bak` 中仍有 `21.75` 默认值和字符串兜底。
  - `HrmSalaryMonthRecordServiceImpl` 中只有注释掉的旧 `21.75` 代码，需要清掉注释避免继续误导。
- 历史文档中的 `21.75` 记录保留为排查证据，不作为本轮“调用”删除范围。

## 2026-07-20 Fix
- 主核算服务新增 `resolveSalaryExpectedAttendanceDays(...)`：使用加班/夜班统计明细的 `expected_attendance_days`，缺失、非法或非正数不再替换默认值。
- `queryNormalDaysByDeptType(...)` 在缺少 `hrm_attendance_info` 或配置无效时返回 `null`；`fillAttendanceDataForEmployee(...)` 通过 `requireSalaryExpectedAttendanceDays(...)` 阻断缺应出勤来源的员工核算，并返回含员工、工号、年月的中文错误。
- 无考勤组员工不再写入默认应出勤；`1 / 应出勤天数` 使用解析出的应出勤，`2 / 应计出勤天数` 优先用考勤汇总/加班夜班应计出勤，缺失时回到同一个应出勤值。
- 半月转正分段薪资不再用默认天数兜底；`SalaryComputeContext` 增加 `expectedAttendanceDaysByEmployee`，计算阶段优先从本次考勤数据、加班/夜班统计、考勤配置或既有月薪记录读取有效应出勤。
- `SalaryMonthRecordService_Bak` 删除默认天数回退，缺行政应出勤天数时抛业务错误；旧 `HrmSalaryMonthRecordServiceImpl` 注释中的默认值也已替换。
- 生产源码和资源扫描 `rg -n "21\\.75|DEFAULT_NORMAL_DAYS" src/main/java src/main/resources -S` 无命中；历史文档和源码扫描测试中的字符串保留用于证据与防回归。

## 2026-07-20 Follow-up
- 用户明确追加：如果逻辑走到“缺少加班/夜班统计应出勤、准备读取考勤配置”的第 2 步，应直接提示用户去“单双休设置”设置数据；否则生成薪资停止，导出薪资也停止。
- 因此 `resolveSalaryExpectedAttendanceDays(...)` 不再返回 `configuredNormalDays`；传入 `hrm_attendance_info.actual_work_day` 也只作为旧签名兼容参数，不参与薪资应出勤解析。
- 生成薪资缺应出勤时抛 `HrmException(6001)`，文案包含“单双休设置”；导出薪资缺加班/夜班统计明细时抛 `HrmException(7001)`，文案同样提示“单双休设置”。
- RED 覆盖：
  - 缺员工加班/夜班统计时，即使传入 `22.0` 配置天数，`resolveSalaryExpectedAttendanceDays(...)` 也返回 `null`。
  - `requireSalaryExpectedAttendanceDays(...)` 和导出缺明细错误都必须包含“单双休设置”。
- 2026-07-20 17:05 复核：`fillAttendanceDataForEmployee(...)` 在员工级核算前调用 `requireSalaryExpectedAttendanceDays(...)`；导出在写 `9007 / 满勤天数`、`9008 / 超缺勤天数` 前调用加班/夜班统计解析，缺明细时抛提示“单双休设置”的业务异常。薪资组合回归 72 个测试通过，生产源码和资源无 `21.75` / `DEFAULT_NORMAL_DAYS` 命中。
# Findings: 全员全勤奖漏发排查

## 2026-07-20 Pre-work
- 已读取项目 `docs/requirements.md` 和 `docs/development.md`。
- 直接相关规则：
  - 全勤奖工资项为 `40102 / 全勤奖`。
  - 员工需正式/已转正、档案 `full_attendance=1`，金额按最新基本工资金额设置区分普通员工与领导。
  - 当 `hrm_attendance_report_data` 月度汇总存在时，仍保留迟到超过 30 分钟、缺卡、旷工、事假、病假、早退等扣全勤条件。
  - 当钉钉月度汇总缺失或未识别，但加班/夜班统计显示应计出勤达到应出勤时，应兜底发放全勤奖。
  - 年假/调休且应计出勤已满的场景不应单独阻止全勤奖。
- 历史李明明排查已经指出同类风险：全勤判断仍依赖钉钉报表汇总时，会导致已满额应计出勤的员工不自动生成 `40102`。
- 本轮用户点名张明，需要扩展为全员检查，必须避免只按姓名判断。

## 2026-07-20 Root Cause
- 目标租户确认：`companyId=0003 / 湖北公司 / hr_0003`。
- 目标薪资主记录：`2077807285177995265 / 六月薪资报表 / 2026-06`，共 95 名薪资员工，当前 76 人已有 `40102 全勤奖`。
- 张明唯一在职记录：`employee_id=1831601326890434564`，工号 `TYNG-012`，手机号 `13197133302`，部门“行政人力资源部”，正式员工，已转正，`full_attendance=1`，非残疾，`expand_production=1`。
- 张明 2026-06 薪资明细：`need_work_day=23.00`、`actual_work_day=23.00`，`40102` 为空；迟到、早退、旷工、事假、病假、缺卡、综合扣款、超缺勤均为 `0`。
- 张明加班/夜班统计：`expected_attendance_days=23`、`accrued_attendance_hours=184.00`，即应计出勤 `23` 天已达到应出勤。
- 张明钉钉报表原始汇总仍有 `上班/下班缺卡次数=16`、`迟到时长=1`；当前考勤规则迟到、早退、缺卡扣款金额均为 `0.00`。
- 代码根因：`SalaryMonthRecordServiceNew#shouldFallbackFullAttendanceByAccruedDays(...)` 在 `empAttendanceSummary != null` 时直接返回 `false`，导致张明这类“钉钉月汇总存在但薪资应计出勤已满”的员工不会走全勤兜底。
- 全员影响：按“档案启用全勤、非残疾、加班/夜班应计出勤达到应出勤、`40102` 为空”有 12 人；其中最终考勤扣款为 0 的 6 人是张雪梅、吴晓霞、张明、王琪、闫倩、高文利。
- 当前薪资管理页面核算入口 `HrmSalaryMonthRecordController#computeSalaryData` 注入并调用 `SalaryMonthRecordServiceNew`；旧 `SalaryComputeTask` 中对 `SalaryMonthRecordService_Bak#computeSalaryData(...)` 的调用已注释，定时任务也处于注释禁用状态。

## 2026-07-20 Fix
- RED：新增 `shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected`，旧代码失败于返回 `false`。
- GREEN：`shouldFallbackFullAttendanceByAccruedDays(...)` 不再因为 `empAttendanceSummary` 存在而拒绝兜底，只要薪资应计出勤天数达到加班/夜班统计应出勤天数即可返回 `true`。
- 现有工资数据不会自动变化；需重新核算 2026-06 薪资后，`40102` 才会按新逻辑生成。

# Findings: 农谷 2026-06 高温补贴与薪资档案补齐

## 2026-07-20 Pre-work
- 本轮以 `/Users/jiangyongming/Downloads/薪资 (1).xlsx` 为基准人员范围，对照 `/Users/jiangyongming/Desktop/农谷导入数据/薪资/薪资-手工.xlsx` 的 `6月` sheet。
- 两个 Excel 源文件没有手机号列；为满足“姓名 + 电话唯一键”，预览阶段先用姓名结合入职日期、部门、岗位定位源行和系统员工，执行 SQL 时通过 `hr_0003.hrm_employee.employee_name + mobile` 校验后更新对应 `employee_id`。
- 表结构确认：
  - 考勤汇总表为 `hrm_produce_attendance`，其它补贴字段为 `other_subsidies`。
  - 薪资档案三项在 `hrm_salary_archives_option`，编码为 `10101 / 基本工资`、`10102 / 岗位工资`、`10103 / 职务工资`。

## 2026-07-20 Result
- 预览报告：`docs/reports/2026-07-20-nonggu-salary-attendance-supplement-preview.md`。
- 执行 SQL：`docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement.sql`；回滚 SQL：`docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement_rollback.sql`。
- 高温补贴：19 名员工从 `other_subsidies=0` 更新为 `100`；赵聪 `15926668783` 当前其它补贴已是目标值 `100`，未重复累加。
- 薪资档案：6 条正式工资项 `is_pro=0` 已更新为手工表目标值：
  - 李红盼岗位工资 `2550 -> 3370`；
  - 喻艳岗位工资 `1070 -> 1970`；
  - 王洪平岗位工资 `670 -> 1520`；
  - 李学飞岗位工资 `2670 -> 2970`；
  - 王志兰基本工资 `2130 -> 2000`；
  - 王志兰岗位工资 `1070 -> 0`。
- 写后核验：20 个相关考勤汇总行 `other_subsidies=100`，6 条薪资档案目标项均等于目标值，异常数为 0。
- 未处理：吴镜平在手工表 `6月` sheet 无同名行；本轮未直接修改已生成薪资月记录或薪资项，需要重新核算/导出后才反映到 2026-06 工资表。

# Findings: 员工合同无固定期限结束日期

## 2026-08-21 Current Behavior
- 后端合同类型枚举已有 `2 / 无固定期限劳动合同`。
- 旧后端规则仍统一要求 `startTime/endTime` 都存在：
  - `HrmEmployeeContractServiceImpl#addOrUpdateContract(...)` 保存前调用 `calculateContractTerm(startTime, endTime)`；
  - `calculateContractTerm(...)` 对任一日期为空抛出“合同开始日期和合同结束日期不能为空”；
  - 合同导入 `buildImportContract(...)` 在解析合同类型前后没有例外，直接用 `getRequiredImportDate(...)` 读取 `合同结束日期`。
- 前端员工合同模型已有 `contractType=2` 时隐藏 `合同期限` 的分支，但 `getRules(...)` 仍始终把 `endTime` 设为必填。
- RED 证据：
  - 单独运行 `HrmEmployeeContractServiceImplTest` 失败于 `addOrUpdateContract_shouldAllowOpenEndedContractWithoutEndDate`，异常为 `7001:合同开始日期和合同结束日期不能为空`；
  - 同一测试失败于 `importContracts_shouldAllowOpenEndedContractWithBlankEndDate`，异常为 `7001:第2行合同结束日期不能为空`。

## 2026-08-21 Implementation
- 后端保存规则以 `contractType=2` 作为唯一无固定期限信号。`normalizeContractTerm(...)` 对无固定期限合同只校验 `startTime`，然后清空 `endTime/term`；其它类型继续要求 `startTime/endTime` 并按日期计算期限。
- 合同导入先解析 `合同类型`，无固定期限劳动合同不再要求 `合同结束日期`；即使导入文件写了 `2099-12-31` 等占位日期，保存归一化也会清空。
- 员工基础信息导出和部门明细导出通过 Mapper 新增的 `latestContractType/lastContractType` 区分无固定期限合同，把对应期限、结束时间、合同到期日显示为 `无固定期限`。
- `hrm_employee_contract.end_time` 在 dev MySQL `153.0.237.98` 的 `hr_0001` 至 `hr_0005` 均为 nullable，本次无需 DDL；合同到期提醒继续只按非空 `end_time` 查询。
- 后端测试已覆盖手工保存、导入空结束日期、导入占位日期清空、固定期限仍要求结束日期、基础信息导出、部门明细导出和 Mapper SQL 别名。
- Fresh 验证：合同后端测试组 38 个测试通过，`mvn -DskipTests compile` 通过；仅保留既有 Maven POM warning。

# Findings: 吴镜平离职后仍生成 2026-06 薪资排查

## 2026-07-21 Root Cause
- 吴镜平当前员工档案：`employee_id=1831601326890434575`，手机号 `13797928108`，工号 `1718539734988`，`entry_status=4`，`is_del=0`，入职时间 `2020-04-01`，部门财务部，所属体系行政。
- 离职信息：`plan_quit_time=2026-06-10`，`salary_settlement_time=2026-06-10`。
- 计薪员工 SQL 规则来自 `HrmSalaryMonthEmpRecordMapper.xml`：
  - 在职/待离职 `entry_status in (1,3)` 纳入；
  - 离职 `entry_status=4` 时，只要 `plan_quit_time > date_sub(endTime, interval 1 month)` 也纳入；
  - 2026-06 薪资结束日是 `2026-06-30`，阈值为 `2026-05-30`，吴镜平 `2026-06-10 > 2026-05-30`，所以命中计薪员工范围。
- 2026-06 薪资主记录 `2077807285177995265 / 六月薪资报表` 有 95 个员工明细，包含吴镜平；主表 `num=1` 与实际明细数不一致，不应作为薪资员工数证据。
- 吴镜平 2026-06 考勤汇总：`positive_attendance=23.00`、`probation_attendance=23.00`；薪资明细：`need_work_day=23.00`、`actual_work_day=23.00`。
- 离职员工计算逻辑将旷工/缺勤天数设置为 `normalDays - actualityDays`；当前数据为 `23 - 23 = 0`，所以不会因 6 月 10 日离职自动扣掉离职后的天数。
- 工资项闭合：基本工资 `2130` + 岗位工资 `3070` + 全勤奖 `100` = 应发/实发 `5300`；社保、公积金、个税均为 `0`。

## 2026-07-21 Implication
- 当前系统行为不是导出对比造成的，而是薪资核算员工范围和考勤汇总数据共同导致的。
- 若业务希望离职员工只结算到离职日，需要改变离职员工的计薪范围或考勤/应计出勤口径；仅删除导出表行会造成薪资明细、个税累计、导出不一致。

# Findings: 张雪梅 2026-06 应发与实发工资计算过程排查

## 2026-07-21 Current Salary Snapshot
- 本轮只读查询 `hr_0003`，未修改薪资业务表。
- 员工档案：张雪梅 `employee_id=1831601326890434590`，手机号 `15872992452`，工号 `TYNG-371`，部门仓储部，正式在职，已转正，非残疾，`full_attendance=1`，当前 `affiliation_system=1`。
- 目标薪资：`s_record_id=2077807285177995265 / 六月薪资报表 / 2026-06`；员工薪资记录 `s_emp_record_id=2077929043646251010`，`need_work_day=25.00`，`actual_work_day=25.00`。
- 当前工资项闭合：
  - 基本工资 `10101=2130`、岗位工资 `10102=1570`、职务工资 `10103=0`，来自正式薪资档案 `hrm_salary_archives_option.is_pro=0`。
  - 考勤汇总 `hrm_produce_attendance`：`positive_attendance=22.00`、`probation_attendance=25.00`、`work_over_time=1.00`、`overtime_pay=12.00`、`other_subsidies=100`；由于员工当前所属体系为行政，`180101 / 加班工资=0`。
  - 加班/夜班统计明细去重后为 `expected_attendance_days=25`、`actual_attendance_days=22`、`accrued_attendance_hours=200.00`，即薪资应计出勤 `25` 天。
  - 本地审批快照：`2026-06-01` 病假 `8小时`、`2026-06-02` 病假 `8小时`、`2026-06-06` 年假 `4小时`、`2026-06-15` 加班 `1小时`。
  - 考勤报表 `hrm_attendance_report_data` 汇总 `病假=16.00`，当前薪资代码行政体系分支把该小时数直接作为 `16.00` 天。
  - 病假扣款按当前落库逻辑：最低基本工资 `1860` / 应出勤 `25` * `(16 - 2)` = `1041.60`，四舍五入为 `19010401=1042`，并汇总到 `200101=1042.00`。
  - 当前不发全勤奖，`40102` 未生成。
- 应发工资 `210101=2758.00`：`2130 + 1570 + 0 + 100 - 1042 = 2758`。
- 代扣小计 `1001=624.29`：个人社保 `100101=466.50` + 个人公积金 `100102=144.00` + 工会费 `160102=13.79` + 个税 `230101=0.00` + 其它扣款/借款 `0`。
- 工会费 `13.79`：正式员工按当前应发工资 `2758 * 0.5% = 13.79`。
- 个税：上月累计 `250101=24367.00`、`250102=55000.00`、`250103=3642.30`、`250105=0.00`；本月累计 `270101=27125.00`、`270102=60000.00`、`270103=4252.80`，累计应纳税所得额为 `0`，所以 `230101=0.00`。
- 实发工资 `240101=2133.71`：`2758.00 - 466.50 - 144.00 - 13.79 - 0.00 = 2133.71`。

## 2026-07-21 Fix
- 修复前落库金额可以闭合，但病假扣款口径不符合已确认需求：两条各 `8小时` 病假应折算为 `2天`，按“2 天内病假工资不扣”规则，`19010401` 应为 `0`。
- 已修复 `SalaryMonthRecordServiceNew#sickDeductDays(..., type="2")` 行政体系分支，将 `hrm_attendance_report_data` 的病假小时数按 `/8` 折算成天数；生产体系原有分支已按同一口径折算。
- 若仅按已确认业务口径重算且其它数据不变，张雪梅应发工资应为 `2130 + 1570 + 100 = 3800.00`，不发全勤奖；工会费按 `3800 * 0.5% = 19.00`，个税仍为 `0`，实发工资应为 `3800 - 466.50 - 144.00 - 19.00 = 3170.50`。
- 验证：新增测试先失败后通过；`SalaryMonthRecordServiceNewTest` 通过 58 个测试；薪资组合回归通过 80 个测试。

# Findings: 余德胜薪资无工会费排查

## 2026-08-17 Current Salary Snapshot
- 本轮只读查询 `hr_0003`，未修改薪资业务表。
- 员工档案：余德胜 `employee_id=1831601326890434582`，手机号 `13872902745`，工号 `TYNG-009`，部门 `1988144158630604801`，岗位“冷库设备机组管理员”，正式在职，已转正，`is_del=0`。
- 关键字段：`hrm_employee.is_disabled=1`。当前薪资代码约定 `1=残疾/免工会费与个税`，`2=非残疾/参与计算`。
- `SalaryComputeServiceNew#computeSalary(...)` 只有 `isDisabled.equals("2")` 时才调用 `calculateUnionFee(...)`；残疾员工会直接保持 `labourunionPay=0`。
- 最新薪资明细：
  - `2026-07`：应发工资 `210101=4100.00`，工会费 `160102=0`；若不命中残疾免收规则，理论工会费为 `20.50`。
  - `2026-06`：应发工资 `210101=4200.00`，工会费 `160102=0`；若不命中残疾免收规则，理论工会费为 `21.00`。
- 结论：余德胜当前薪资没有工会费不是显示层漏项，而是员工档案标记为残疾，核算规则显式不收工会费。

# Findings: 员工其他补助导入导出与保存

## 2026-08-21 Current Context
- 需求/开发文档显示，2026-08-20 已为 `薪资等级`、`固定绩效`、`职务补助` 建立员工动态字段链路。
- 花名册模板已有运行时补列能力：`固定绩效`、`职务补助` 位于 `薪资等级` 右侧，同属 `薪酬福利` 父表头，数字格式为 `0.00`。
- 员工导入保存动态字段时已对 `固定绩效`、`职务补助` 做 `DECIMAL + precisions=2` 类型纠正和值格式化。
- 员工新增、详情编辑和员工变更保存已通过 BO/request-only 字段把既有薪资字段写入 `hrm_employee_data`，本轮“其他补助”应复用该机制。
- 部门明细“薪资待遇”当前按 `固定薪资(固定)+固定绩效(绩效)+职务补助(职务补助)+全勤(全勤)` 拼接，本轮需要把“其他补助”纳入展示和相关成本汇总口径。

## 2026-08-21 Implementation Notes
- “其他补助”已确认走员工动态字段体系，后端字段名为 `other_subsidy`，前端 payload 字段为 `otherSubsidy`。
- 后端模板、导入、员工保存和详情编辑均把 `其他补助` 识别为 `DECIMAL + precisions=2`，保存为两位小数字符串。
- 部门明细 `薪资待遇` 展示顺序为 `固定薪资(固定)+固定绩效(绩效)+职务补助(职务补助)+其他补助(其他补助)+全勤(全勤)`；固定薪资成本同步累加该字段。
- 本轮组合测试暴露一个既有导入匹配缺口：员工手机号变化但姓名和身份证号不变时，旧逻辑按 `姓名+手机号` 未匹配到原员工，会按新员工校验并报工号重复；已补按 `姓名+身份证号` 匹配原员工。

# Findings: 数据配置类型和值可自定义

## 2026-08-22 Discovery
- 数据配置页面原实现把“类型”固定成工段/车间两档，本质上是 `tbdictdata` 的 `sn` 字段没有被前端直接展示，`name` 也只被当成“值”使用。
- 这次需求只需要把“类型/值”都开放成用户可输入文本，不需要新增表结构；现有 `dtid/pid` 继续作为内部树结构兼容字段即可。
- 前端页面继续按现有字典接口加载已有数据，但新增/编辑不再提供工段/车间下拉，直接提交 `sn/name`。
- 后端保存时需要保留历史记录的 `dtid/pid/canUse/createMan/createTime`，否则单纯改 `sn/name` 会把旧数据的元信息洗掉。
- 为了让列表直接显示用户自定义类型，后端 `TreeNode` 增加了 `type` 字段，并在字典查询时把 `sn` 回填给它。
- RED/GREEN 证据：
  - `node /Users/jiangyongming/Project/hr/hr_web/tests/data-config-api.test.mjs` 先失败于仍传固定 `AddType`，修复后通过；
  - `node /Users/jiangyongming/Project/hr/hr_web/tests/data-config-page.test.mjs` 先失败于固定工段/车间下拉，修复后通过；
  - `mvn -Dtest=tbDictDataServiceImplTest test` 通过，锁定 `sn/name` 保存与 `TreeNode.type` 回填。

# Findings: 添加排班与社保方案列表分页截断

## 2026-08-24 Backend Contract
- `/workPlan/getData` 的控制器参数名是 `pageSize/pageNum`，未传时会使用默认页大小；保存接口 `/workPlan/saveAll` 不参与加载截断。
- `/workPlan/loadIsLast` 与 `/workPlan/getData` 使用相同分页字段，但前端旧代码只有 `loadIsLast` 做了 `page/limit` 到 `pageNum/pageSize` 的映射。
- `/hrmInsuranceScheme/index` 接收 `PageEntity`，`PageEntity.getLimit()` 默认 `15`；传 `pageType=0` 时后端按不分页上限返回列表。
- 因此两个用户反馈均是列表/加载请求参数问题，不是排班保存或社保方案保存只能写入固定条数。

# Findings: 员工部门明细跨公司加密导出

## 2026-08-24 Backend Contract
- `HrmEmployeeMapper#queryDepartmentDetailExportList` 必须返回 `a.full_attendance as fullAttendance`，否则 `EmployeeDepartmentDetailExportSupport` 无法区分员工是否有全勤。
- `EmployeeDepartmentDetailExportSupport` 的新口径是 `fullAttendance=1` 才追加全勤；`fullAttendance=2` 或空值不追加。试用期不追加、全勤金额为 `100` 不追加继续保留。
- `HrmEmployeeServiceImpl#exportDepartmentDetail` 已抽出 `buildDepartmentDetailExportData()`，普通角色和行政经理跨公司导出复用同一套员工查询、动态字段、薪资档案、社保公积金、顶层组织和 workbook 构建逻辑。
- 行政经理严格按 `roleName == "行政经理"` 进入跨公司导出，遍历 `0001/0002/0003/0004/0005`，包括成都 `0002`。每个公司通过 `EmployeeDepartmentDetailCrossCompanyExportSupport.withCompanyContext(...)` 切换上下文，finally 恢复原 `CompanyContext`。
- 跨公司上下文复制时会清空非当前公司的 `companyName`，避免目标库缺顶层组织时错误复用登录公司名；服务层降级使用 `hr_公司ID`。
- ZIP 使用 Zip4j AES-256 加密，响应头 `X-Archive-Password` 返回密码；`CrossDomainFilter` 必须暴露 `Content-Disposition, X-Archive-Password`。

## 2026-08-24 Frontend Contract
- 全局 axios 响应拦截器对 `responseType='blob'` 返回 `{ data, headers }`，普通 JSON 继续返回 `response.data`。
- 员工管理部门明细下载从 `x-archive-password` / `X-Archive-Password` 读取密码，有密码时提示 `下载成功，压缩包打开密码：...`。
- 密码提示必须是可交互消息而不是纯文本 toast：使用 VNode 渲染密码和复制按钮，复制按钮优先调用 `navigator.clipboard.writeText`，失败时降级 `document.execCommand('copy')`，成功提示 `压缩包密码已复制`。
- 其它 Blob 下载入口需要继续按 `res?.data || res` 取二进制，避免全局拦截器改动后下载对象字符串。

# Findings: 排班矩阵岗位独立开始/结束时间

## 2026-08-24 Current Contract
- 添加排班前端 `AddOrEdit.vue` 当前把 `shiftType/customStart/customEnd/customShiftPeriod/customContinuousShift` 放在产品主行，`positionRows` 只保存岗位和人员；同一产品主行中的多个岗位因此共用一套班次时间。
- 添加排班提交前由 `work-plan-utils.js#buildWorkPlanSubmitPayload` 将岗位子行展开为多个扁平保存项，但当前班次字段仍从主行读取。
- 后端 `/workPlan/saveAll` 已支持每个扁平保存项独立携带 `productName/linkName/userId/shiftType/customStart/customEnd`，`WorkPlanServiceImpl` 会继续按员工拆分并保存本地自定义班次；不需要新增表结构。
- 排班管理矩阵的 `/workPlan/queryEmployeeDayAssignments` 与 `/workPlan/saveEmployeeDayAssignments` 已按 `employeeId + workDate` 聚合，并且每条 `assignment` 已包含独立产品、岗位、班次类型、标准班次 ID、开始/结束时间和连班状态；矩阵改单日链路已经具备独立时间基础。

## 2026-08-24 Design Direction
- 推荐把时间归属下沉到“岗位分配行”：`产品 -> 岗位分配行 -> 人员集合 + 班次/开始时间/结束时间`。
- 同一岗位需要不同时间时，允许新增同名岗位分配行并选择不同人员；同一岗位同一人员同一时间的完全重复分配在提交前拦截。
- 为保持录入效率，可保留主行的“默认班次”作为新岗位行继承模板，并提供将默认班次应用到全部岗位行的动作；真正提交和回显以岗位分配行字段为准。
- 采用岗位分配行粒度可兼容当前 `tbplanlist` 扁平结构、现有 `saveAll` 接口和矩阵 assignment 接口；只有未来要求同一岗位内同一批人员分别设置不同时间时，才需要进一步把人员拆成独立分配项。
# 2026-08-24 排班矩阵岗位级车间字段

- 用户已确认：添加排班矩阵和排班管理修改排班矩阵都增加“车间”文本框；车间由用户自由填写。
- 车间字段必须挂在每条岗位/分配行上，与该行的开始时间、结束时间、班次类型、白夜班和连班一起保存，不能挂在员工日期主行上。
- 后端 `tbplanlist.GroupID` 继续表示考勤组，不能写入用户填写的车间；现有 `WorkPlanEmployeeDayShiftVO.workshopName` 是由考勤组推导的展示字段，也不能复用为持久化值。
- 推荐新增独立属性 `workshopName`，映射数据库列 `tbplanlist.workshop_name`，JSON 保持驼峰字段名。
- 添加排班保存继续兼容 `/workPlan/saveAll` 扁平数组：岗位行展开后每条 item 携带自己的 `workshopName/customStart/customEnd`。
- 排班管理修改保存继续使用 `/workPlan/saveEmployeeDayAssignments` 完整替换语义：每条 assignment 透传并回显自己的 `workshopName`。
- 需要覆盖标准班次、自定义班次、休息/调休、复制/加载已有排班和共享用户排班行，避免只在新增界面显示而保存/回显丢失。
