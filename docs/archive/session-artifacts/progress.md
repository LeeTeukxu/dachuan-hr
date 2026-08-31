# Progress: 排班管理矩阵多产品多岗位修改排班

## 2026-08-24
- 用户询问：排班管理矩阵修改排班时，如果一个员工被安排多个岗位甚至多个产品，解决方案是什么。
- 已复核当前实现：
  - 后端 `queryEmployeeDayShift` 只返回 `pickPreferredEmployeePlan(...)` 选出的单条记录。
  - 前端矩阵可把同产品同班次多岗位聚合展示，但修改弹窗仍以单个 `dayShiftProductContext` 表示产品/岗位上下文。
- 用户已确认允许不同产品/岗位使用不同工作时间。
- 已形成设计：以 `员工 + 日期` 为编辑聚合对象，内部维护多条 `assignments[]`，每条分配独立维护产品、岗位、班次和时间。
- 已记录保存策略：修改保存采用完整替换目标员工当天排班，先从旧 `tbplanlist.UserID` 中移除目标员工，再逐条保存新 assignments；多人共享旧行保留其他员工。
- 已新增设计文档：`docs/plans/2026-08-24-workplan-matrix-multi-assignment-design.md`。
- 已同步更新：后端 `docs/requirements.md`、`docs/development.md`，前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 本轮未改业务代码，未运行 Maven 或前端构建。
- 用户确认“可以按照这个方案进行开发”后，已进入实现阶段。
- 后端 RED：
  - `WorkPlanServiceImplTest#queryEmployeeDayAssignments_shouldReturnAllLocalProductPositionAssignments` 锁定同一员工同一天多产品/多岗位查询必须返回完整 `assignments[]`。
  - `WorkPlanServiceImplTest#saveEmployeeDayAssignments_shouldReplaceEmployeeDayWithMultipleProductPositionRows` 锁定保存完整替换、共享旧行保留其他员工、两条新分配分别保存不同工作时间。
  - `WorkPlanListControllerTest#queryEmployeeDayAssignments_shouldParseDateAndReturnAllAssignments` 和 `#saveEmployeeDayAssignments_shouldDelegateCompleteRequestToService` 锁定新接口契约。
  - `mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 在 testCompile 阶段失败于 `SaveWorkPlanEmployeeDayAssignmentBO`、`SaveWorkPlanEmployeeDayAssignmentsBO`、`WorkPlanEmployeeDayAssignmentVO`、`WorkPlanEmployeeDayAssignmentsVO` 不存在，符合预期红灯。
- 后端 GREEN：新增完整分配 BO/VO、`/workPlan/queryEmployeeDayAssignments`、`/workPlan/saveEmployeeDayAssignments` 与服务层完整替换保存；`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 69 项。
- 前端 RED：
  - `work-plan-api.test.mjs` 新增新接口契约，当前失败于 `workPlan.js` 未导出 `queryEmployeeDayAssignments`；
  - `work-plan-utils.test.mjs` 新增完整分配集合归一和保存请求构造测试；
  - `work-plan-scheduling-records.test.mjs` 新增源码契约，要求 `Scheduling.vue` 使用 `queryEmployeeDayAssignments/saveEmployeeDayAssignments/dayShiftAssignments` 且不再引用 `queryEmployeeDayShift`。
- 前端 GREEN：
  - `src/api/hrm/attendance/workPlan.js` 新增 `queryEmployeeDayAssignments(...)` 和 `saveEmployeeDayAssignments(...)`；
  - `work-plan-utils.js` 新增 `normalizeEmployeeDayAssignmentsForEdit(...)` 和 `buildEmployeeDayAssignmentsSaveRequest(...)`；
  - `Scheduling.vue` 修改排班弹窗改为 `dayShiftAssignments[]` 多分配行，加载使用完整分配接口，保存使用完整替换接口，不再调用 `queryEmployeeDayShift`。
- 旧测试修复：
  - `tests/work-plan-scheduling-records.test.mjs` 后半段仍要求 `getWorkPlanMatrixCellScheduleMeta/queryEmployeeDayShift/dayShiftProductContext`，与新页面契约冲突；
  - 已改为断言 `Scheduling.vue` 使用 `queryEmployeeDayAssignments/saveEmployeeDayAssignments/dayShiftAssignments/buildEmployeeDayAssignmentsSaveRequest`，并断言页面不再使用旧单条编辑来源。
- 验证通过：
  - `node tests/work-plan-api.test.mjs`；
  - `node tests/work-plan-utils.test.mjs`；
  - `node tests/work-plan-scheduling-records.test.mjs`；
  - `node tests/work-plan-records-page.test.mjs`；
  - `npm run build`，保留既有 Vue `::v-deep` 和 Vite chunk size warning；
  - `mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 69 项；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 文档收尾：
  - 已更新后端 `docs/requirements.md`、`docs/development.md`；
  - 已更新前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`；
  - 已更新 `task_plan.md`、`findings.md`、`progress.md`。

# Progress: 添加排班人员缺失修复

## 2026-08-24
- 用户反馈：admin 登录后“添加排班”人员下拉里找不到陈明成。
- 已定位：展示人员原来依赖 `tbattendanceuser` 快照，员工没有快照或没有考勤组时无法进入展示列表。
- 已修复：改为从 `hrm_employee` 查询 `is_del=0` 且 `entry_status in (1,3,4)` 的员工，过滤无姓名；人员 ID 按员工表 `dingtalk_user_id`、`tbattendanceuser.userId`、员工 `employeeId` 依次解析，并按解析后的用户 ID 去重。
- 无考勤组员工仍返回，`groupId` 使用空字符串；陈明成等员工可以按姓名搜索。
- 展示缓存 key 升级为 `_getAllUsers_display_v3`，并清理 v2/旧版 key，避免历史缓存继续漏人。
- 已确认标准班次保存仍使用提交的钉钉用户 ID调用原排班接口，展示来源切换不会改变 `/workPlan/saveAll` 契约。
- 验证通过：`mvn -Dtest=WorkPlanServiceImplTest,HrmAttendanceDataControllerTest test`（62 项）、`mvn -DskipTests compile`。

# Progress: 添加排班岗位多行与全员补充选人

## 2026-08-23
- 用户要求添加排班时：
  - 人员除按岗位加载外，还能在所有员工中自选添加；
  - 岗位列提供 `+` 按钮，按已选生产产品显示可选岗位；
  - 多个岗位多行显示，不再挤在一个下拉列表框中；
  - 已选岗位不能在其它岗位下拉中重复出现；
  - 已选人员不能在其它人员下拉中重复出现。
- 已读取后端项目需求/开发文档、既有生产产品排班实施计划、当前 task/findings/progress。
- 已补 RED：`node tests/work-plan-utils.test.mjs` 先失败于新增岗位/人员去重工具未导出。
- 已实现前端：`AddOrEdit.vue` 改为岗位 `positionRows` 多行展示，岗位列提供 `+` 新增行；人员列按岗位子行独立选择，候选来自全员并排除其它岗位子行已选人员。
- 已实现工具层：`work-plan-utils.js` 增加岗位子行归一、状态同步和岗位/人员候选去重；提交仍展开为 `/workPlan/saveAll` 扁平 payload。
- 验证通过：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/workplan-product-management.test.mjs`。
- 构建通过：`npm run build`，保留既有 Vue `::v-deep` 和 Vite chunk size warning。

# Progress: 排班产品岗位排序与多岗位提交

## 2026-08-22
- 用户要求补充：
  - 新增生产产品、添加岗位时排序号按数据库已有排序自动生成；
  - 生产产品管理页右侧岗位区域支持拖动排序并持久化；
  - 添加排班一行可以同时给多个岗位排班，点击提交后按新结构持久化。
- 已读取后端 `docs/requirements.md`、`docs/development.md`、现有生产产品实施计划，以及前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 已定位当前后端：
  - `WorkPlanProductServiceImpl#saveProduct/#savePosition` 未传排序时写 `0`；
  - 后端没有岗位重排接口；
  - `/workPlan/saveAll` 继续解析扁平数组并落 `ProductName/LinkName`。
- 已定位当前前端：
  - `workplan-product/Index.vue` 岗位表没有拖拽能力；
  - `AddOrEdit.vue` 每行只有单个 `positionId`；
  - `buildWorkPlanSubmitPayload(...)` 每行只返回一条 payload。
- 已补 RED 测试：
  - 后端服务测试要求新增产品和岗位自动生成最大排序 + 1，并要求重排岗位按提交顺序保存；
  - 后端控制器测试要求新增 `sortPositions`；
  - 前端产品管理测试要求新增排序 API 和拖拽源码契约；
  - 前端排班工具测试要求一个行内两个岗位展开成两条提交 payload。
- RED 结果符合预期：
  - Maven testCompile 失败于 `SortWorkPlanPositionBO` 缺失；
  - 前端产品管理测试失败于 `saveWorkPlanPositionSort` 未导出；
  - 前端排班工具测试失败于多岗位 payload 实际长度仍为 1。
- 已实现后端：
  - `saveProduct(...)` 新增产品未传 `sort` 时按当前最大产品排序 + 1 自动生成；
  - `savePosition(...)` 新增岗位未传 `sort` 时按同一产品下最大岗位排序 + 1 自动生成；
  - 新增 `SortWorkPlanPositionBO`、`IWorkPlanProductService#sortPositions(...)` 和 `/workPlanProduct/sortPositions`，按提交顺序持久化同一产品下岗位排序。
- 已实现前端：
  - 生产产品和岗位新增弹窗将 `sort` 置为空值，由后端自动生成排序号；
  - 生产产品管理右侧岗位区域新增拖动手柄，拖放后调用 `saveWorkPlanPositionSort({ productId, positionIds })`；
  - 添加排班岗位选择改为 `multiple`，行模型新增 `positionIds/positions`，`handlePositionsChange()` 为每个岗位维护独立人员集合；
  - `buildWorkPlanSubmitPayload(...)` 将一行多岗位展开成多条扁平保存项，继续通过 `/workPlan/saveAll` 落 `ProductName/LinkName`。
- GREEN/回归验证：
  - `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanListControllerTest,WorkPlanServiceImplTest test` 通过，73 个测试 0 failures/errors；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings；
  - `node tests/workplan-product-management.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs` 通过；
  - `npm run build` 通过，保留既有 Vue `::v-deep` warning 和 Vite chunk size warning。
- 已更新后端与前端需求/开发文档，记录自动排序、拖动排序持久化、一行多岗位提交前展开保存的最终口径。
- 2026-08-23 用户继续反馈拖动排序体验：
  - 已补 RED 测试：后端产品排序服务/控制器，前端产品排序 API、产品/岗位拖拽源码契约和排序输入框默认值计算；
  - 已实现 `/workPlanProduct/sortProducts`，按提交产品 ID 顺序持久化生产产品排序；
  - 已实现左侧生产产品表拖动排序，右侧岗位表拖动反馈增强，两张表都展示 `sort` 序号；
  - 新增产品/岗位时排序输入框默认加载当前最大排序 + 1，编辑时加载已有排序；
  - 拖动时增加被拖动行淡化、目标行高亮、顶部/底部插入线和 reduced-motion 兼容；
  - GREEN：`mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest test` 通过，10 个测试 0 failures/errors；`node tests/workplan-product-management.test.mjs` 通过。

# Progress: 生产产品管理菜单权限

## 2026-08-22
- 用户反馈：本机 `http://127.0.0.1:8081/` 用 `cdadmin/123` 登录后没有看到新增的“生产产品”管理页，怀疑新页面未配置权限。
- 已读取后端和前端需求/开发文档，确认生产产品功能源码已实现，权限菜单和本机 8081 静态部署需要单独验证。
- 已建立当前专项计划，先复现登录 `menuTree` 和菜单授权，再做最小权限修复。
- 初步源码证据：`ApiPermissionPathSupport` 未包含 `/workPlanProduct` 接口映射；后端登录菜单树只会包含当前角色已授权菜单。
- 已复现 `cdadmin/123`：`companyId=0002`、`roleId=2`，登录 `menuTree` 初始不包含 `/hrm/attendance/workplanProduct`。
- 已确认 `hr_web/html` 当前静态包包含 `workplanProduct` 路由，前端包不是根因。
- 已查询 `hr_0002.tbmenu/tbrolemenu`：缺“生产产品”菜单；`roleId=2` 已有“添加排班”权限。
- 已补 RED 测试并确认失败：`ApiPermissionPathSupportTest` 失败于 `/workPlanProduct/queryTree` 返回 `null` 菜单路径，`WorkPlanProductMigrationSqlTest` 失败于菜单权限 SQL 文件不存在。
- 已实现：`ApiPermissionPathSupport` 新增 `/workPlanProduct -> /hrm/attendance/workplanProduct`；新增 `docs/sql/2026-08-22_workplan_product_menu_permission.sql`，按“已有添加排班权限的角色”补授权。
- GREEN：`mvn -Dtest=ApiPermissionPathSupportTest,WorkPlanProductMigrationSqlTest test` 通过，7 个测试。
- 已执行菜单权限脚本到 dev MySQL `hr_0001` 至 `hr_0005`；`cdadmin` 重新登录后 `menuTree` 已包含“生产产品”。
- 首次调用 `/workPlanProduct/queryTree` 返回 SQLGrammarException，经查是生产产品三张配置表未在租户库创建；已在 `hr_0001` 至 `hr_0005` 执行 `docs/sql/2026-08-22_hrm_workplan_product_position_employee.sql`。
- 运行时复核：五个租户库均有三张配置表；`cdadmin` 调用 `/workPlanProduct/queryTree` 返回 `success=true, code=200, count=0`。
- 回归验证：`mvn -Dtest=ApiPermissionPathSupportTest,MenuPermissionSupportTest,WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest test` 通过，19 个测试；`node tests/workplan-product-management.test.mjs` 通过。

# Progress: 排班生产产品/岗位/员工配置

## 2026-08-22
- 用户要求新增独立生产产品配置功能，并在添加排班里完成“选择产品 -> 加载岗位 -> 加载岗位员工”的联动。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 已沿用既有实施计划 `docs/plans/2026-08-22-workplan-product-position-employee-implementation-plan.md`，确认不新增排班事实表字段，继续复用 `productName/linkName`。
- 后端已完成：
  - 新增 `HrmWorkPlanProduct`、`HrmWorkPlanProductPosition`、`HrmWorkPlanPositionEmployee`；
  - 新增 `SaveWorkPlanProductBO`、`SaveWorkPlanPositionBO`、树形 VO 和三个 Repository；
  - 新增 `IWorkPlanProductService`、`WorkPlanProductServiceImpl`、`WorkPlanProductController`；
  - 新增 `docs/sql/2026-08-22_hrm_workplan_product_position_employee.sql`；
  - `WorkPlanEmployeeDayShiftVO` 和 `WorkPlanServiceImpl` 支持返回生产产品、岗位、车间。
- 前端已完成：
  - 新增 `src/api/hrm/attendance/workplan-product.js`；
  - 新增 `src/views/hrm/attendance/workplan-product/Index.vue` 和 `workplan-product-utils.js`；
  - `src/router/config.js` 在考勤管理下新增“生产产品”路由；
  - `AddOrEdit.vue` 改为“生产产品/岗位/人员”列，选择岗位自动带出岗位员工，并提供清空全部/删除单个员工；
  - `Scheduling.vue` 修改排班弹窗展示生产产品、岗位、车间；
  - `overview-utils.js` 透传新增展示字段。
- 验证通过：
  - `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanServiceImplTest#queryEmployeeDayShift_shouldExposeProductPositionAndWorkshopForEditDialog test`；
  - `node tests/workplan-product-management.test.mjs`；
  - `node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/clock-overview-utils.test.mjs`；
  - `mvn -DskipTests compile`；
  - `npm run build`。
- 已更新前后端需求/开发文档、`task_plan.md`、`findings.md`、`progress.md`。

# Progress: 社保详情一键设置保险金额

## 2026-08-22
- 已启用项目文档优先、文件化计划、brainstorming 与 TDD 流程。
- 已读取后端与前端需求/开发文档，确认当前社保方案已有医疗长期护理保险启用状态，基本工资设置仍保留长期护理和大额医疗金额。
- 已记录当前目标：社保详情页需要对全员或选中员工一键设置/取消是否累计长期护理和大额医疗金额，列表和详情合计同步采用同一口径。
- 当前设计倾向：后端持久化员工级“是否累计保险金额”状态，前端只触发设置/取消并刷新列表；实际金额由后端按最新基本工资设置计算，避免浏览器端自行拼金额。最早版本曾同时判断方案启用状态，已被 2026-08-22 最新要求覆盖。
- 下一步：定位现有社保详情查询、月报列表 SQL 和测试结构，补 RED 测试。
- 已定位社保详情数据流：
  - 前端详情页为 `hr_web/src/views/hrm/insurance-scheme/InsuranceDetail.vue`，已有 `showSelection=true` 和表格上方按钮插槽；
  - 员工行数据来自后端 `/hrmInsuranceMonthRecord/queryInsurancePageList`；
  - 详情上方合计来自 `/hrmInsuranceMonthRecord/queryInsuranceRecordList/{iRecordId}`；
  - 社保管理列表合计来自 `/hrmInsuranceMonthRecord/queryInsuranceRecordList`；
  - 员工月度记录 `hrm_insurance_month_emp_record` 当前只存个人/公司社保金额，没有是否累计基本工资保险金额的持久化字段。
- 已补 RED 测试：
  - 后端 `HrmInsuranceMonthRecordControllerTest` 期望新增 `/updateSalaryBasicInsuranceAmount`；
  - 后端 `HrmInsuranceMonthRecordServiceTest` 最早期望按月度项目基础金额重新计算，且医疗长期护理启用时才累加长期护理/大额医疗；该启用限制已被后续新口径覆盖；
  - 后端 `HrmInsuranceMonthRecordMapperXmlTest` 期望详情员工列表返回累计开关字段；
  - 前端 `tests/insurance-detail-amount-actions.test.mjs` 期望详情页全员/选中员工按钮和 API。
- RED 结果符合预期：
  - Maven testCompile 失败于 `UpdateInsuranceSalaryBasicAmountBO` 缺失；
  - 前端 Node 测试失败于详情 API 未暴露更新接口。
- 已实现后端：
  - 新增 `UpdateInsuranceSalaryBasicAmountBO`；
  - `HrmInsuranceMonthRecordController` 新增 `/updateSalaryBasicInsuranceAmount`；
  - `HrmInsuranceMonthEmpRecord` 新增 `includeSalaryBasicInsuranceAmount` 字段；
  - `HrmInsuranceMonthRecordService#updateSalaryBasicInsuranceAmount(...)` 支持全员或选中员工范围；
  - `refreshSalaryBasicInsuranceAmount(...)` 从员工月度参保项目重算基础金额，再按当时旧口径的医疗长期护理启用状态追加长期护理/大额医疗；后续已改为设置即追加、不看启用状态；
  - `HrmInsuranceMonthEmpRecordService#updateInsuranceProject(...)` 在员工已设置累计时，参保方案编辑后重新刷新累计金额；
  - 新增迁移脚本 `docs/sql/2026-08-22_hrm_insurance_month_emp_include_salary_basic_insurance_amount.sql`。
- 已实现前端：
  - `detail.js` 新增 `updateSalaryBasicInsuranceAmount(...)`；
  - `InsuranceDetail.vue` 在高级筛选后新增全员设置/取消按钮；
  - 表格按钮区新增选中员工设置/取消按钮；
  - 操作成功后刷新 `getDetailData()` 和 `getListData()`。
- GREEN/回归验证：
  - `mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 通过，11 个测试；
  - `mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest test` 通过，22 个测试；
  - `node tests/insurance-detail-amount-actions.test.mjs`、`node tests/insurance-scheme-projects.test.mjs`、`node tests/insurance-advanced-filter.test.mjs`、`node tests/insurance-scheme-usage-tooltip.test.mjs` 通过；
  - `mvn -DskipTests compile` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 和 chunk size warning；
  - 本轮后端目标文件 `git diff --check -- ...` 无输出；前端目标文件 `git -C /Users/jiangyongming/Project diff --check -- ...` 无输出；前端目标文件 `rg -n "[[:blank:]]$" ...` 无输出。
- 迁移执行：
  - 已按用户授权在 dev MySQL `153.0.237.98` 执行 `docs/sql/2026-08-22_hrm_insurance_month_emp_include_salary_basic_insurance_amount.sql`；
  - `information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 的 `hrm_insurance_month_emp_record.include_salary_basic_insurance_amount` 均为 `tinyint / NOT NULL / default 0 / 是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是`。
- 故障修复：
  - 已复现“一键设置保险金额”入参 JSON 使用 `iRecordId/iEmpRecordIds` 时，`UpdateInsuranceSalaryBasicAmountBO.iRecordId` 反序列化为 `null`，触发“社保记录不能为空”；
  - 已为 `UpdateInsuranceSalaryBasicAmountBO` 增加 `@JsonProperty("irecordId")/@JsonProperty("iempRecordIds")` 和 `@JsonAlias("iRecordId")/@JsonAlias("iEmpRecordIds")`，兼容前端当前提交和项目既有对外字段名；
  - RED/GREEN：`mvn -Dtest=HrmInsuranceMonthRecordControllerTest#updateSalaryBasicInsuranceAmountRequestShouldAcceptFrontendIRecordFieldNames test` 先失败于 `iRecordId=null`，修复后通过；
  - 回归：`mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest test` 通过，23 个测试 0 failures/errors。
- 金额不刷新修复：
  - 只读核验 dev MySQL `hr_0001` 至 `hr_0005` 的基本工资保险金额均为 `largeMedicalInsuranceAmount=15.00`、`longTermCareInsuranceAmount=3.00`，排除配置金额为 0 导致视觉不变；
  - 追加前端 RED/GREEN：`tests/insurance-detail-amount-actions.test.mjs` 先失败于页面未提交 `irecordId/iempRecordIds`，修复 `InsuranceDetail.vue` 请求体后通过；
  - 追加后端服务测试 `updateSalaryBasicInsuranceAmountShouldPersistRecomputedEmployeeAmounts`，验证接口拿到月报 ID 后会把 `100/200` 重算为 `103/215` 并提交 `updateBatchById`；
  - 结论：一键操作后详情行、详情合计和社保管理列表都读取 `hrm_insurance_month_emp_record` 的持久化金额；前端需提交后端既有 JSON 字段名才能确保更新落到目标月报。
- 用户继续反馈“还是一样”的运行时排查：
  - 重新登录 `hbadmin` 后确认 token 中 `companyId=0003`，所以应检查 `hr_0003`，不是 `hr_0001`；
  - 本机 `127.0.0.1:8081` 静态包入口为 `assets/index-BrxqCap-.js`，详情 chunk 已包含 `irecordId/iempRecordIds` 新请求体；本机 `9080` 后端进程 PID `1732` 运行 `target/classes`；
  - 直接调用 `hr_0003` 2026-07 月报 `2090269617723400193` 的设置接口返回 `data=87`，开关从 `0` 到 `1`，但金额未变。继续查项目明细发现 7 月员工记录在操作前已等于基础项目金额加 `3/15`，属于历史金额和新开关状态不一致；
  - 按全库扫描确认 `hr_0001/hr_0002/hr_0004/hr_0005` 无同类脏数据，`hr_0003` 仅 2026-06 月报 `2079207580227342338` 有 88 条“金额已含累计、开关为 0”的历史脏数据；
  - 已执行 SQL 将这 88 条修正回未累计基础金额，并用同一接口验证：一键设置后 88 人合计从 `41885.10/103910.05` 变为 `42149.10/105230.05`，一键取消后回到 `41885.10/103910.05`；详情合计、详情列表和社保管理列表接口同步变化。
- 用户反馈 `cdadmin` 在 `127.0.0.1:8081` 点击仍无视觉变化：
  - 重新登录 `cdadmin/123` 确认 token 中 `companyId=0002`，应检查 `hr_0002`；
  - 最新 2026-03 月报 `2091088562492608513` 当前 3 人均已设置开关 `include_salary_basic_insurance_amount=1`，个人/公司社保费为 `477.15/1160.76`，合计 `1431.45/3482.28`；
  - 方案 `1831653398776143873 / 成都（一）` 和 3 条员工月度项目中 `type=12 / 医疗长期护理保险` 均为 `is_enabled=0`，所以按旧需求规则设置/取消都不追加长期护理 `3.00` 或大额医疗 `15.00`；
  - 已通过 8081 API 实测取消返回 `data=3` 后开关变为 `0`、金额不变；设置返回 `data=3` 后开关恢复为 `1`、金额仍不变；
  - 详情合计接口、详情员工列表接口、社保管理列表接口均返回一致金额，结论是当前 `cdadmin` 无视觉变化由方案禁用配置导致，不是接口未生效。
- 用户最新确认：不管 `医疗长期护理保险` 是否启用，一键设置/取消都要能起效：
  - 已先改测试 `HrmInsuranceMonthRecordServiceTest#refreshSalaryBasicInsuranceAmountShouldAddAmountsWheneverSettingIsEnabled`，RED 失败于旧实现禁用状态返回 `100.00/200.00` 而非新期望 `103.00/215.00`；
  - 已修改 `HrmInsuranceMonthRecordService#refreshSalaryBasicInsuranceAmount(...)`，删除 `hasEnabledMedicalLongTermCareInsurance(...)` 分支；设置时只看 `includeSalaryBasicAmount=true` 追加基本工资设置金额，取消时回到月度项目基础金额；
  - 已删除旧的 `hasEnabledMedicalLongTermCareInsurance(...)` 方法，避免后续误以为一键设置仍要受 `type=12 is_enabled` 约束；
  - GREEN：单测通过；社保组合回归 `mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest test` 通过，24 个测试 0 failures/errors；
  - 运行时验证：通过 `127.0.0.1:8081/api` 用 `cdadmin` token 对 `hr_0002` 2026-03 月报先取消再设置，取消后 3 人为 `477.15/1160.76`、合计 `1431.45/3482.28`，设置后 3 人为 `480.15/1175.76`、合计 `1440.45/3527.28`；详情合计、详情列表和社保管理列表接口一致。

# Progress: 社保方案医疗长期护理保险项

## 2026-08-22
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认社保方案页是后台紧凑数据维护页，前后端改动后需同步更新文档。
- 已定位前端新增/编辑默认险种行：`hr_web/src/views/manage/insurance-scheme/add.js`、`Add.vue`、`Edit.vue`。
- 已定位后端保存/详情链路：
  - `HrmInsuranceProjectController#saveInsuranceProject`
  - `HrmInsuranceSchemeService#saveInsuranceProject`
  - `HrmInsuranceSchemeService#queryInsuranceSchemeById`
  - `HrmInsuranceMonthEmpRecordService#queryById`
  - `HrmInsuranceSchemeMapper.xml`、`HrmInsuranceSechemeMapper.xml`、`HrmInsuranceMonthEmpProjectRecordMapper.xml`
- 当前实现决策：新增项目类型 `12` 作为 `医疗长期护理保险`；旧方案详情缺行时由后端补默认启用的空白行；合计 SQL 只汇总启用项目行，不再自动追加基本工资设置中的大额医疗或长期护理金额。
- 用户追加启用/禁用要求后，已补 RED 测试：
  - 后端 `HrmInsuranceSchemeServiceTest` 锁定方案详情补行默认启用、已保存禁用状态回显、保存 DTO/实体/月度 VO 暴露 `isEnabled`；
  - 后端 `HrmInsuranceSchemeMapperXmlTest` 锁定方案和月度项目合计不得再读取 `hrm_salary_basic`、`large_medical_insurance_amount`、`long_term_care_insurance_amount`，且必须按 `is_enabled` 过滤；
  - 前端 `tests/insurance-scheme-projects.test.mjs` 锁定默认行 `isEnabled=1`、新增/编辑弹窗存在状态列和启用/禁用按钮。
- RED 结果符合预期：
  - 后端 testCompile 失败于 `getIsEnabled/setIsEnabled` 缺失；
  - 前端测试失败于长期护理默认行 `isEnabled` 为 `undefined`。
- 已实现：
  - `hrm_insurance_project`、`hrm_insurance_month_emp_project_record` 对应实体/DTO/VO 增加 `isEnabled`；
  - `HrmInsuranceSchemeService` 对旧方案补出的 `医疗长期护理保险` 设置 `isEnabled=1`，查询/保存时空状态按启用归一；
  - 月度员工参保详情 `queryById` 对项目 `isEnabled=null` 按启用回显；
  - 月度参保方案编辑对旧方案补出的 `projectId=null,type=12` 长期护理行会创建真实方案项目并写入员工月度项目；
  - `HrmInsuranceSchemeMapper.xml`、`HrmInsuranceSechemeMapper.xml`、`HrmInsuranceMonthEmpProjectRecordMapper.xml` 删除基本工资设置自动累加逻辑，并加 `coalesce(is_enabled, 1) = 1` 过滤；
  - 新增 `docs/sql/2026-08-22_hrm_insurance_project_is_enabled.sql`；
  - 前端默认社保/公积金项目增加 `isEnabled=1`，新增/编辑社保表格在 `type=12` 行展示启用/禁用按钮。
- GREEN 验证：
  - 追加保存链路回归：`HrmInsuranceSchemeServiceTest#saveInsuranceProject_shouldPersistMedicalLongTermCareEnabledState` 捕获保存实体，验证 `医疗长期护理保险 isEnabled=0` 会随 `saveOrUpdateBatch` 入库，旧空状态按启用处理；
  - 追加 review 回归：`HrmInsuranceSchemeServiceTest#updateInsuranceProject_shouldPersistBackfilledMedicalLongTermCareProjectWhenProjectIdMissing` 先失败于月度更新 BO 缺少 `type/projectName/isEnabled`，修复后锁定旧方案补行可在月度参保编辑中落库；
  - `mvn -Dtest=HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordMapperXmlTest test` 通过，18 个测试 0 failures/errors；
  - `node tests/insurance-scheme-projects.test.mjs` 通过。
- 最终验证：
  - `mvn -DskipTests compile` 通过，保留既有 Maven duplicate dependency / systemPath warnings；
  - `node tests/insurance-advanced-filter.test.mjs`、`node tests/insurance-scheme-usage-tooltip.test.mjs` 通过；
  - `npm run build` 通过，保留既有 Vue `::v-deep` warning 和 Vite chunk size warning；
  - 前后端目标文件 `git diff --check -- ...` 均无输出。
- 迁移执行：
  - 已按用户授权在 dev MySQL `153.0.237.98` 执行 `docs/sql/2026-08-22_hrm_insurance_project_is_enabled.sql`；
  - 首次按应用 JDBC 的 `gbk` 客户端字符集执行后字段功能正确，但中文 COMMENT 出现乱码；根因是脚本为 UTF-8 而客户端会话字符集不匹配；
  - 已修正迁移脚本：开头增加 `SET NAMES utf8mb4`，字段已存在时执行 `MODIFY COLUMN` 刷新类型、默认值和中文注释；
  - 已用 `utf8mb4` 连接重新执行脚本，`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 两张表共 10 个 `is_enabled` 字段均为 `tinyint / NOT NULL / default 1 / 是否启用：0禁用 1启用`。

# Progress: 社保管理名单与方案使用人数悬浮展示

## 2026-08-21
- 已读取后端 `docs/requirements.md`、`docs/development.md`，以及前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认本轮应保持后台页面紧凑，不改变已有统计口径。
- 已定位月报卡片页面 `hr_web/src/views/hrm/insurance-scheme/InsuranceScheme.vue`：当前直接在卡片下方展开 `insuredEmployeeNames/stoppedEmployeeNames`。
- 已定位“使用人数”列在 `hr_web/src/views/manage/insurance-scheme/InsuranceScheme.vue`，后端接口为 `/hrmInsuranceScheme/index`，当前 VO 只有 `useCount` 没有人员名单。
- 已补 RED 测试：
  - `tests/insurance-advanced-filter.test.mjs` 断言月报人数使用 tooltip、名单不再直接展开。
  - `tests/insurance-scheme-usage-tooltip.test.mjs` 断言方案管理“使用人数”tooltip 渲染 `useEmployeeNames`。
  - `HrmInsuranceSchemeMapperXmlTest`、`InsuranceSchemeListVOTest`、`HrmInsuranceSchemeServiceTest` 断言方案列表返回人员名单并调高 `group_concat_max_len`。
- RED 结果：
  - 前端两个 Node 测试分别失败于缺少 tooltip。
  - 后端测试先失败于 `InsuranceSchemeListVO` 缺少 `useEmployeeNames` getter/setter，追加截断保护后失败于 `HrmInsuranceSechemeMapper` 缺少 `setGroupConcatMaxLen()`。
- 已实现：
  - 后端 `InsuranceSchemeListVO` 新增 `useEmployeeNames`。
  - `HrmInsuranceSechemeMapper.xml#index` 与 `HrmInsuranceSchemeMapper.xml#queryInsuranceSchemePageList` 返回 `useEmployeeNames`。
  - `HrmInsuranceSechemeMapper` 和遗留 `HrmInsuranceSchemeMapper` 新增 `setGroupConcatMaxLen()`，XML 设置 `SET SESSION group_concat_max_len = 4194304`。
  - `HrmInsuranceSchemeService#index` 查询前调用 `setGroupConcatMaxLen()`，并使用只读事务保证会话变量生效于同次列表查询。
  - 前端月报卡片删除直接展开名单区域，改为“本月参保人数/本月停保人数”数字悬浮显示人员名单。
  - 前端社保方案管理“使用人数”列改为数字触发 tooltip，显示使用人数和采用人员名单。
- GREEN/回归验证：
  - `mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceSchemeServiceTest,InsuranceSchemeListVOTest test` 通过，13 个测试 0 failures/errors。
  - `mvn -DskipTests compile` 通过。
  - `node tests/insurance-advanced-filter.test.mjs && node tests/insurance-scheme-usage-tooltip.test.mjs && node tests/insurance-delete-feedback.test.mjs && node tests/insurance-create-next-month.test.mjs && node tests/insurance-progress-utils.test.mjs` 通过。
  - `npm run build` 通过，保留既有 Vue `::v-deep` deprecation 和 Vite chunk size warning。

# Progress: 社保管理本月参保/停保人员名单

## 2026-08-21
- 已读取 `hainan/docs/requirements.md` 与 `docs/development.md`，确认本轮应保持既有社保月报接口契约并补充列表统计字段。
- 已定位后端列表链路：
  - `HrmInsuranceMonthRecordController#queryInsuranceRecordList`
  - `HrmInsuranceMonthRecordService#queryInsuranceRecordList`
  - `HrmInsuranceMonthRecordMapper#queryInsuranceRecordList`
  - `src/main/resources/mapper/HrmInsuranceMonthRecordMapper.xml`
- 已确认当前 SQL 只返回数量，不返回参保/停保姓名；下一步补 RED 测试锁定新增字段。
- 已补 RED 测试：
  - `HrmInsuranceMonthRecordMapperXmlTest` 断言列表 SQL 返回 `insuredNum/insuredEmployeeNames/stoppedEmployeeNames`、使用 `GROUP_CONCAT` 和 `group_concat_max_len`；
  - `HrmInsuranceMonthRecordServiceTest` 断言列表查询前先调用 `setGroupConcatMaxLen()`。
- RED 结果：
  - `mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest test` 失败于缺少新增字段；
  - `mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` testCompile 失败于 Mapper 缺少 `setGroupConcatMaxLen()`。
- 已实现：
  - `QueryInsuranceRecordListVO` 新增 `insuredNum/insuredEmployeeNames/stoppedEmployeeNames`；
  - `HrmInsuranceMonthRecordMapper` 和 XML 新增 `setGroupConcatMaxLen`；
  - `queryInsuranceRecordList` SQL 返回本月参保人数、停保人数和两类人员名单；
  - `HrmInsuranceMonthRecordService#queryInsuranceRecordList` 使用事务包住会话变量设置和查询。
- GREEN：`mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 通过，7 个测试 0 failures/errors。

# Progress: cfy 登录 TooManyResultsException

## 2026-08-21
- 已读取项目需求/开发文档和既有登录用户创建保存设计，确认登录链路依赖系统库 `${hrm.system.database}.tbAllUserList` 与租户库 `view_LoginUser`。
- 已创建本轮计划并记录 session-catchup 误用 `sh` 的错误和修正。
- 已定位登录入口和查询点：
  - `LoginController#Login`
  - `LoginUserMapper#getCompanyIdByUserName`
  - `LoginUserMapper#getByAcountAndCompanyID`
  - `TbLoginUserService#Add` / `TbLoginUserMapper.xml#saveSystemLoginAccount`
- 已复现：远端与本机 `POST /hrsystem/login?account=cfy&password=123` 均返回 `TooManyResultsException`；`hbadmin` 正常。
- 只读 SQL 根因：
  - `hrsystem.tbAllUserList` 中 `cfy` / `0001` 有 2 条账号索引；
  - `hr_0001.view_LoginUser` 中 `cfy` 只有 1 条有效登录用户；
  - `hrsystem.tbAllUserList` 当前没有主键或唯一索引，导致 `ON DUPLICATE KEY UPDATE` 不会防重。
- 已补 RED 测试：
  - `LoginControllerTest` 覆盖同公司重复索引可登录、跨公司重复索引返回中文配置错误；
  - `TbLoginUserServiceValidationTest` 覆盖同公司重复索引允许、跨公司抢占禁止、保存前删除当前公司同账号索引；
  - `TbLoginUserMapperSqlTest` 覆盖系统库账号索引查询使用 `SELECT DISTINCT CompanyID`。
- RED：`mvn -Dtest=LoginControllerTest,TbLoginUserServiceValidationTest,TbLoginUserMapperSqlTest test` 在 testCompile 阶段失败，缺少 `getCompanyIdsByUserName(...)` 与 `findSystemCompanyIdsByAccount(...)`。
- 已实现：
  - `LoginUserMapper#getCompanyIdsByUserName(...)` 返回 `SELECT DISTINCT CompanyID` 列表并显式标注 `@Param`；
  - `LoginController#resolveCompanyIdByAccount(...)` 对系统账号索引空结果、同公司重复、多公司重复分别处理；
  - `TbLoginUserMapper#findSystemCompanyIdsByAccount(...)` 返回去重公司列表；
  - `TbLoginUserService#requireSystemAccountAvailable(...)` 只在账号映射到其它公司时拒绝；
  - `TbLoginUserService#syncSystemLoginAccount(...)` 保存前删除当前公司同账号旧索引，避免后续重复。
- GREEN：同一 Maven 命令通过，13 个测试 0 failures/errors；保留既有 Maven POM/dependency warnings，预期错误分支仍会由 `successResult.raiseException(...)` 打印异常堆栈。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md` 和 `findings.md`。
- 代码审查反馈处理：
  - 确认 `TbLoginUserService#resolveCompanyId(...)` 原实现优先请求体 `companyId` 存在风险；
  - 追加 RED `resolveCompanyId_shouldPreferCurrentTenantContextOverRequestCompanyId`，失败于返回请求体 `0001`；
  - 修复为优先 `CompanyContext.companyId`，无登录上下文时才回退请求体 `companyId`；
  - 补强 `TbLoginUserMapperSqlTest`，对硬编码 `hrsystem.tbAllUserList` 的检查改为大小写不敏感；
  - 单测转 GREEN，完整登录修复测试组最终通过 14 个测试。

# Progress: 薪资档案在职离职卡片筛选

## 2026-08-20
- 已读取 `hainan` 与 `hr_web` 的需求/开发文档，确认本轮应保持薪资模块既有接口契约和后台管理页紧凑交互风格。
- 已定位前端页面 `hr_web/src/views/hrm/salary/archives/Archives.vue`、接口定义 `src/api/hrm/salary/salary.js`、后端入口 `HrmSalaryArchivesController#querySalaryArchivesList`、DTO `QuerySalaryArchivesListDto` 和 SQL `HrmSalaryArchivesMapper.xml#querySalaryArchivesList`。
- 已确认后端现有状态筛选口径：`status=11` 对应 `entry_status in (1,3)`，`status=15` 对应 `entry_status=4`；本轮不需要新增后端生产代码。
- 已补 RED 前端测试 `hr_web/tests/salary-archives-status-filter.test.mjs`，首次运行失败于页面缺少 `salaryArchivesStatusTabs`，证明现有页面没有在职/离职卡片筛选。
- 已实现 `Archives.vue`：
  - 新增 `salaryArchivesStatusTabs`，包含 `在职/status=11` 与 `离职/status=15`；
  - `form.salaryArchivesStatus` 默认 `11`；
  - `getTableData()` 请求体附加 `status: form.salaryArchivesStatus`；
  - `handleSalaryArchivesStatusChange(...)` 切换时重置 `page=1` 并刷新；
  - 模板新增两个卡片式状态入口，支持点击、Enter 和 Space；
  - 样式新增 hover、focus-visible 和 active 反馈。
- 已补后端 SQL 保护测试 `HrmSalaryArchivesMapperSqlTest`，锁定薪资档案列表 SQL 中 `status=11/15` 的入离职过滤口径。
- 当前验证：
  - `node tests/salary-archives-status-filter.test.mjs` 通过；
  - `mvn -Dtest=HrmSalaryArchivesMapperSqlTest test` 通过，1 个测试 0 failures/errors。

# Progress: 职务补助与花名册表头修复

## 2026-08-20
- 已读取 `hainan` 与 `hr_web` 的需求/开发文档，确认员工薪资扩展字段继续走动态字段体系，不新增员工主表物理列。
- 已定位根因：花名册模板运行时插入 `固定绩效` 时只移动单元格，未同步移动/扩展第 1 行父表头合并区域，导致 `固定绩效` 生成孤立的第二个 `薪酬福利` 父表头。
- 已补 RED 测试：
  - 后端 `HrmEmployeeControllerTest` 覆盖 `固定绩效/职务补助` 都在 `薪酬福利` 合并父表头下、列格式为 `0.00`；
  - 后端 `HrmEmployeeServiceImplSalaryFieldsTest` 覆盖 `dutySubsidy` 入参、动态字段定义、保存和编辑规范化；
  - 后端 `HrmEmployeeServiceImplImportEmployeeTest` 覆盖花名册导入 `职务补助` 为两位小数字段和值；
  - 后端 `EmployeeDepartmentDetailExportSupportTest` 覆盖全勤金额为 `100` 时不追加到 `薪资待遇`；
  - 前端 `employee-salary-fields-ui.test.mjs` 覆盖新建/再入职、详情编辑兜底和列表异动弹窗都展示/提交 `职务补助`。
- RED 结果：后端聚焦测试在 testCompile 阶段失败于 `AddEmployeeBO/AddEmployeeFieldManageBO/HrmEmployeeChangeRecord` 缺少 `dutySubsidy` getter/setter；前端测试失败于 `AddOrEdit.vue` 缺少 `dutySubsidy` 初始字段。
- 已完成后端实现：
  - 花名册模板在已有 `薪资等级` 右侧依次补齐 `固定绩效`、`职务补助`，两列均为 `0.00` 数字格式，并重新合并 `薪酬福利` 父表头覆盖三列，避免重复同名父表头；
  - `职务补助` 纳入员工动态字段定义、导入、详情编辑归一化、新增/再次入职保存和员工异动保存链路，字段类型为两位小数；
  - 部门明细 `薪资待遇` 增加 `职务补助(职务补助)`，`月固定薪资成本` 同步累加该动态字段；
  - 部门明细全勤金额解析为 `100` 时不再追加到 `薪资待遇`。
- 已完成前端实现：
  - 新建/再次入职弹窗新增 `职务补助` 两位小数输入框；
  - 员工详情基本信息编辑兜底补齐 `职务补助` 动态字段定义；
  - 员工列表 `办理转正`、`调整部门/岗位`、`晋升/降级` 弹窗新增 `职务补助` 并从当前行回显；
  - 员工列表行归一化支持 `duty_subsidy <-> dutySubsidy`。
- GREEN/回归验证：
  - `mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 通过，41 个测试 0 failures/errors；
  - `mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest,EmployeeBasicInfoExportSupportTest test` 通过，46 个测试 0 failures/errors；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM duplicate/systemPath warnings；
  - `node tests/employee-salary-fields-ui.test.mjs` 通过；
  - `node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过；
  - `npm run build` 通过，保留既有 Vue `::v-deep` deprecation 和 Vite chunk size warning。
- 已更新 `hainan/docs/requirements.md`、`hainan/docs/development.md`、`hr_web/docs/requirements.md`、`hr_web/docs/development.md`、`task_plan.md` 和 `findings.md`，记录 `职务补助`、花名册父表头修复、部门明细薪资待遇/成本口径和无需 DDL 的结论。
- 最终补丁检查：后端目标文件 `git diff --check -- ...` 无输出；前端目标文件 `git -C /Users/jiangyongming/Project diff --check -- ...` 无输出；前后端目标文件 `rg -n '[[:blank:]]$' ...` 均无输出。
- 已按完成前代码审查流程启动只读审查代理，审查范围为花名册父表头、`职务补助` 动态字段保存链路、部门明细薪资待遇/成本和前端字段提交；代理多轮等待未返回结果，已改由本地代码复核、回归测试、编译/构建和空白检查作为最终验收依据。

# Progress: 部门明细薪资待遇累加全勤奖

## 2026-08-20
- 已启用项目文档优先、文件化计划、系统化调试和 TDD 流程。
- 已读取项目需求/开发文档及现有计划记录，确认本轮落点为员工管理“下载部门明细”的后端导出 `薪资待遇` 展示。
- 已记录当前目标：`薪资待遇` 在既有固定薪资/固定绩效基础上累加基本工资设置中的普通/领导全勤奖；试用期员工不累加全勤奖。
- 已定位实现：
  - `EmployeeDepartmentDetailExportSupport#salaryTreatmentText(...)` 是部门 sheet 明细行 `薪资待遇` 写入点；
  - `HrmEmployeeServiceImpl#exportDepartmentDetail(...)` 是组装导出数据并调用 workbook 构造的位置；
  - `HrmSalaryBasicService#findAll()` 会返回最新基本工资设置并通过 `HrmSalaryBasicDefaults` 补默认普通/领导全勤金额；
  - 计薪员工 SQL 已有领导岗位关键字判断，可复用到部门明细展示。
- 下一步：补 RED 测试覆盖普通员工、领导员工、试用期员工的 `薪资待遇` 展示。
- 已补 RED 测试 `buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees`，覆盖：
  - 普通员工 `4000 + 普通全勤100` 展示为 `4000(固定)+100(全勤)`；
  - 领导岗位 `生产经理` 使用领导全勤 `500`，并与 `固定绩效` 同时展示；
  - 试用期员工不追加全勤，仍展示原固定薪资。
- RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees test` 在 testCompile 阶段失败于 `buildWorkbook(...)` 参数不匹配。
- 已实现：
  - `EmployeeDepartmentDetailExportSupport#buildWorkbook(...)` 新增带普通/领导全勤金额的重载，旧重载保留并默认不加全勤，避免影响既有测试和调用；
  - `薪资待遇` 追加全勤时按组件展示，普通员工使用普通全勤金额，岗位含 `经理/总监/副总/董事长/高级技师/厂长` 使用领导全勤金额；
  - 试用期员工 `status=2/试用` 不追加全勤；
  - `HrmEmployeeServiceImpl#exportDepartmentDetail(...)` 读取最新基本工资设置并传入普通/领导全勤金额。
- GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees test` 通过，1 个测试 0 failures/errors。
- 已补服务层保护断言：`HrmEmployeeServiceImplDepartmentDetailCostTest` 现在锁定部门明细导出必须调用 `salaryBasicService.findAll()`，并把普通/领导全勤金额传入 workbook。
- 聚焦回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，14 个测试 0 failures/errors。
- 部门明细组合回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，25 个测试 0 failures/errors。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，记录部门明细 `薪资待遇` 累加全勤奖、普通/领导金额来源、领导岗位关键字和试用期排除口径。
- 补丁检查：`git diff --check -- src/main/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupport.java src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java src/test/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupportTest.java src/test/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImplDepartmentDetailCostTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

# Progress: 下载部门明细统计与总表修复

## 2026-08-20
- 已启用项目文档优先、文件化计划、系统化调试和 TDD 流程。
- 已读取项目需求/开发文档及现有计划记录，确认本轮只修复员工管理“下载部门明细”的后端导出结构和统计口径。
- 已抽取参考 Excel：
  - `人员总表` 是横向部门汇总结构，包含企业总人数、月总固定薪资成本、月总绩效薪资成本、月总社保成本、月总公积金成本、月总伙食成本、分部门汇总和合计月人工成本；本轮删除伙食成本行，不保留空白占位；
  - 部门 sheet 顶部仍是 A-H 统计区和 I-J 成本区，但本轮需要删除专业相关性描述和伙食成本。
- 已定位实现：
  - `EmployeeDepartmentDetailExportSupport` 负责 workbook、`人员总表`、部门 sheet 顶部统计、成本汇总和行数据；
  - `HrmEmployeeServiceImpl#exportDepartmentDetail` 加载员工、动态字段、薪资档案和最近有效社保月企业缴纳金额；
  - `HrmEmployeeMapper.xml#queryDepartmentDetailExportList` 负责导出员工基础字段，目前未返回 `probation`。
- 已补 RED 测试：
  - `buildWorkbook_shouldRemoveProfessionalRelationTextAndUseEmployeeStatusCounters` 覆盖专业相关/不相关文案删除，以及 `status/probation` 统计口径；
  - `summarySheet_shouldMatchReferenceTotalSheetWithoutMealCost` 覆盖人员总表参考结构、四项总成本、分部门横向汇总、合计月人工成本和伙食成本删除；
  - `HrmEmployeeMapperSqlTest` 增加 `a.probation` 查询断言。
- RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 失败于 `probation` 未查询、旧总表结构和专业相关文案。
- 已实现：
  - `HrmEmployeeMapper.xml#queryDepartmentDetailExportList` 返回 `a.probation`；
  - `EmployeeDepartmentDetailExportSupport` 先构建部门汇总对象，再同时用于部门 sheet 和 `人员总表`；
  - `人员总表` 改成参考表同类横向汇总结构，删除月总伙食成本/月伙食成本行；
  - 专业汇总区域不再输出专业相关/专业不相关文案；
  - “一年以上实习生/正式老员工/试用期人员”改按员工表 `status/probation` 统计。
- GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，21 个测试 0 failures/errors。
- 部门明细组合回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，24 个测试 0 failures/errors。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 补丁检查：`git diff --check -- src/main/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupport.java src/main/resources/mapper/HrmEmployeeMapper.xml src/test/java/com/tianye/hrsystem/imple/employee/EmployeeDepartmentDetailExportSupportTest.java src/test/java/com/tianye/hrsystem/mapper/HrmEmployeeMapperSqlTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

# Progress: 员工新增编辑薪资字段

## 2026-08-20
- 已读取 `hainan/docs/requirements.md`、`hainan/docs/development.md`、`task_plan.md`、`findings.md`、`progress.md`，确认本轮需复用员工动态字段体系，`固定绩效` 继续为两位小数动态字段。
- 已读取 `hr_web/docs/requirements.md` 与 `hr_web/docs/development.md`，确认员工管理新增/编辑页面已有字段调整历史，前端需保持紧凑后台表单风格和保存 loading/错误提示口径。
- 已定位后端链路：`HrmEmployeeController#addEmployee` -> `HrmEmployeeServiceImpl#add` 是新增员工旧接口；`#personalInformation`/`#updateInformation` 是详情基本信息编辑链路；`#againOnboarding` 是再次入职链路。
- 已定位前端链路：`AddOrEdit.vue` 负责新建员工和再入职手写表单；`EmployeeBaseInfo.vue`、`DetailAddOrEdit.vue`、`DynamicForm.vue` 和 `DynamicFormItem.vue` 负责员工详情基本信息编辑。
- 当前结论：需要后端幂等补齐 `薪资等级`/`固定绩效` 字段定义，并让新增员工接口保存两个动态字段；前端新增弹窗显式增加两个输入项，详情编辑保留小数字段精度。
- 已补 RED 测试：
  - 后端 `HrmEmployeeServiceImplSalaryFieldsTest` 覆盖 BO 入参、动态字段定义补齐/历史字段纠正、薪资字段覆盖保存和固定绩效两位小数；
  - 前端 `tests/employee-salary-fields-ui.test.mjs` 覆盖新增/再入职弹窗字段、固定绩效控件精度和详情小数字段回显。
- RED 结果：
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 失败于 `AddEmployeeBO` 与 `AddEmployeeFieldManageBO` 缺少 `salaryLevel/fixedPerformance` getter/setter；
  - `node tests/employee-salary-fields-ui.test.mjs` 失败于 `AddOrEdit.vue` 缺少 `salaryLevel` 初始值。
- 已完成后端实现：
  - `AddEmployeeBO`、`AddEmployeeFieldManageBO` 增加 `salaryLevel/fixedPerformance`；
  - `HrmEmployeeServiceImpl` 在个人信息读取前幂等补齐动态字段定义；
  - 新增/再次入职保存时只覆盖当前员工 `薪资等级`、`固定绩效` 两个动态字段；
  - 详情编辑动态保存时把 `固定绩效` 规范为两位小数字符串。
- 已完成前端实现：
  - 新建/再次入职弹窗增加 `薪资等级` 文本框和 `固定绩效` 两位小数数字框；
  - 再次入职回显兼容 `salaryLevel/salary_level/薪资等级` 与 `fixedPerformance/fixed_performance/固定绩效`；
  - 员工详情小数字段回显改用 `Number`，动态小数字段控件绑定 `:precision="2"`。
- GREEN/回归验证：
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，6 个测试；
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeServiceImplCompanyAgeTest test` 通过，24 个测试；
  - `node tests/employee-salary-fields-ui.test.mjs` 通过；
  - `node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings；
  - `npm run build` 通过，保留既有 Vue `::v-deep` deprecation 和 chunk size warnings；
  - 后端与前端目标文件 `git diff --check -- ...` 均无输出。
- 用户反馈“编辑或者修改的时候没有薪资等级和固定绩效两个输入框”后已重新排查：
  - 远端 `/hrmEmployee/personalInformation/1831601326890434579` 响应 `information` 中没有 `薪资等级/固定绩效`，前端详情编辑按该数组渲染，所以输入框缺失；
  - 本机 9080 当前登录失败于 JDBC 连接不可用，dev MySQL 只读查询报 `Too many connections`，未继续压数据库；
  - 已补 RED：`node tests/employee-salary-fields-ui.test.mjs` 失败于缺少 `ensureSalaryInformationFields`；
  - 已补 RED：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 失败于缺少 `normalizeEmployeeSalaryInformationFields(...)`。
  - 已补 RED：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest#saveEmployeeSalaryDynamicFields_shouldCreateMissingFieldsWithIdsBeforeSavingValues test` 失败于保存动态值时 `fieldId=null`。
- 已完成编辑页缺字段修复：
  - `EmployeeBaseInfo.vue` 新增 `ensureSalaryInformationFields(...)`，接口未返回字段时也补出 `薪资等级` 文本输入和 `固定绩效` 两位小数输入；
  - `HrmEmployeeServiceImpl#updateInformation(...)` 保存前新增 `normalizeEmployeeSalaryInformationFields(...)`，按 `salary_level/fixed_performance` 或中文名解析真实动态字段定义并补 `fieldId/type/labelGroup/isFixed`；
  - `ensureEmployeeSalaryDynamicFields()` 自动创建缺失字段时改为逐个 `save`，避免自定义 `saveBatch` 不回填字段 ID。
- 聚焦 GREEN：
  - `node tests/employee-salary-fields-ui.test.mjs` 通过；
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，8 个测试。
- 最终 fresh 验证：
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeServiceImplCompanyAgeTest test` 通过，26 个测试 0 failures/errors；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings；
  - `node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - 后端目标文件与前端目标文件 `git diff --check -- ...` 均无输出。
- 用户反馈“编辑员工没看到你新增的那两个输入框”后继续排查：
  - 员工列表行操作列实际包含“办理转正”“调整部门/岗位”“晋升/降级”“再入职”等操作；除“再入职”外，其余操作走 `DetailAddOrEdit.vue + employee.js` 字段模型，不走 `AddOrEdit.vue` 或详情基本信息兜底链路；
  - 已补 RED：`node tests/employee-salary-fields-ui.test.mjs` 失败于员工修改弹窗字段模型缺少 `薪资等级`；
  - 已补 RED：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` testCompile 失败于 `HrmEmployeeChangeRecord` 缺少薪资字段 getter/setter。
- 已完成列表操作弹窗修复：
  - `employee.js` 新增 `salaryChangeFields`，并插入 `officialModel.fields` 与 `changePostModel.fieldsFunc(...)`，覆盖 `办理转正`、`调整部门/岗位`、`晋升/降级`；
  - `employee-list-row-normalizer.js` 归一 `salary_level/fixed_performance` 与 `salaryLevel/fixedPerformance`；
  - `Index.vue` 打开列表操作弹窗时从当前行回填两个薪资字段，固定绩效转成数字；
  - `HrmEmployeeChangeRecord` 新增 request-only `salaryLevel/fixedPerformance`，`HrmEmployeeServiceImpl#change(...)` 保存员工异动时同步写入薪资动态字段。
- 当前 GREEN：
  - `node tests/employee-salary-fields-ui.test.mjs` 通过；
  - `node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过；
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，10 个测试 0 failures/errors。
- 追加回归：
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeServiceImplCompanyAgeTest test` 通过，28 个测试 0 failures/errors；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 用户反馈“员工管理中点击员工名称弹出的页面没有薪资等级和固定绩效”后已继续排查：
  - 根因：姓名详情抽屉 `Detail.vue` 默认页签为 `EmployeePostInfo`，薪资字段所在的 `EmployeeBaseInfo` 因 lazy 未挂载，打开详情首屏看不到薪资字段；
  - RED：`node tests/employee-salary-fields-ui.test.mjs` 失败于“点击员工姓名打开详情页时默认必须展示基本信息页签”；
  - 已修改：`Detail.vue` 的 `tabCurrentName` 默认改为 `EmployeeBaseInfo`，并在 `show(...)` 每次打开详情时重置到 `EmployeeBaseInfo`；
  - GREEN：`node tests/employee-salary-fields-ui.test.mjs` 通过。
- 姓名详情抽屉最终验证：
  - `node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - `mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，10 个测试 0 failures/errors；
  - 前后端目标文件 `git diff --check -- ...` 无输出，尾随空白检查无输出。

# Progress: 员工花名册模板薪资字段

## 2026-08-20
- 已按项目文档优先、文件化计划、TDD 和完成前验证流程处理本轮需求。
- 已确认下载员工花名册模板旧实现为原样输出 `export/employee_module.xlsx`，该模板缺少 `薪资级别`、`固定绩效` 两列。
- 已确认导入员工花名册时，固定映射外的列会作为员工动态字段保存；旧实现统一创建文本字段。
- 已补并运行 RED：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 失败于模板缺少 `薪资级别`，以及 `固定绩效` 动态字段类型仍为 `TEXT`。
- 已修改 `HrmEmployeeController#downloadEmployeeRosterTemplate(...)`：下载前用 POI 打开模板资源，对花名册样式 sheet 运行时补齐 `薪资级别`、`固定绩效` 两列，并分别设置文本列格式和 `0.00` 数字列格式。
- 已修改 `HrmEmployeeServiceImpl`：导入创建动态字段时把 `固定绩效` 识别为 `DECIMAL` 且 `precisions=2`，写入动态数据前规范为两位小数字符串；`薪资级别` 保持文本保存。
- 已更新控制器测试，把旧“字节必须等于原始模板资源”改为验证下载响应和有效 workbook，避免和运行时补列需求冲突。
- GREEN：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 通过，14 个测试 0 failures/errors。
- 已追加历史字段兼容测试：若库里已有文本型 `固定绩效` 动态字段，导入时应把字段定义纠正为 `DECIMAL + precisions=2`；RED 先失败于未调用 `updateById`，实现后单测通过。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md` 和 `progress.md` 记录本轮规则、实现和验证。
- 回归验证：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeBasicInfoExportSupportTest test` 通过，19 个测试 0 failures/errors。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 补丁检查：`git diff --check -- src/main/java/com/tianye/hrsystem/controller/HrmEmployeeController.java src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java src/test/java/com/tianye/hrsystem/controller/HrmEmployeeControllerTest.java src/test/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImplImportEmployeeTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。
- 用户再次确认模板已有 `薪资等级`，不需要新增 `薪资级别`；`固定绩效` 要挪到 `薪酬福利` 分组下、已有 `薪资等级` 旁边，仍需支持导入入库。
- 已说明并记录数据库脚本结论：`固定绩效` 当前作为员工动态字段写入 `hrm_employee_field/hrm_employee_data`，不新增员工主表物理字段，因此不需要新增 DDL 脚本。
- 已调整 RED 测试：模板不应包含新增 `薪资级别`，应包含已有 `薪资等级`，`固定绩效` 应紧邻 `薪资等级` 右侧；导入测试只验证 `固定绩效` 两位小数动态字段入库。
- 再次修改 RED：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 失败于模板仍包含新增 `薪资级别`。
- 已修改 `HrmEmployeeController#downloadEmployeeRosterTemplate(...)`：不再补 `薪资级别`；找到已有 `薪资等级` 后在右侧插入 `固定绩效` 列，并右移后续模板列。
- 再次修改 GREEN：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 通过，15 个测试 0 failures/errors。
- 最终回归验证：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeBasicInfoExportSupportTest test` 通过，19 个测试 0 failures/errors。
- 最终编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 最终补丁检查：`git diff --check -- src/main/java/com/tianye/hrsystem/controller/HrmEmployeeController.java src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java src/test/java/com/tianye/hrsystem/controller/HrmEmployeeControllerTest.java src/test/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImplImportEmployeeTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

# Progress: 员工管理下载部门明细方案

## 2026-08-19
- 已启用项目文档优先、brainstorming 和文件化计划流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 中与员工管理、部门、Excel 下载、员工基础信息导出相关的约束片段。
- 已在 `task_plan.md` 顶部记录本轮目标：只讨论方案，不写业务代码。
- 已检查参考 Excel：包含 `人员总表` 和多个部门 sheet；部门 sheet 前 6 行为统计/成本区，第 9 行起为人员明细表头和数据。
- 已定位后端员工管理导出链路：`HrmEmployeeController#exportBasicInfoTemplate`、`HrmEmployeeServiceImpl#exportBasicInfoTemplate`、`EmployeeBasicInfoExportSupport`；可复用 `queryPageList` 的筛选和数据权限。
- 已定位前端员工管理操作区：`Index.vue` 的“员工导入/导出”下拉已有导出/模板下载项和 loading 模式；新按钮适合加入该下拉。
- 用户已确认采用方案 A：模板样式 + 动态填数。
- 用户已确认导出范围：当前登录公司内全部未删除员工，不跟随员工管理页面当前筛选、状态页签或分页。
- 用户已确认字段口径：`薪资待遇` 取员工薪资档案 `10101 / 基本工资`、`10102 / 岗位工资`、`10103 / 职务工资` 三项之和；考核分相关字段不需要；部门成本无系统来源，第一版不计算、不填充。
- 用户已确认最终列结构：直接删除“年平均考核分”“理论考核分”两列，不保留空列。
- 用户已确认文件名：公司名称取组织管理最顶层数据，拼接 `人员明细表` 和导出当天年月日；方案记录为 `<顶层组织名称>人员明细表yyyyMMdd.xlsx`。
- 2026-08-19 进入实现阶段，已创建实现计划 `docs/plans/2026-08-19-employee-department-detail-export-implementation.md`。
- 当前工作区存在大量既有代码改动和未跟踪文件；本轮只增量处理部门明细导出相关文件，不回退无关改动。
- 代码与验证已完成：
  - 后端实现多 sheet 导出、全量未删除员工查询、薪资待遇求和和部门 sheet 去重；
  - 前端新增部门明细下载入口，支持 loading/disabled 和后端文件名回收；
  - 已通过 `mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test`、`mvn -DskipTests compile`、`node tests/employee-department-detail-export-api.test.mjs`、`node tests/employee-department-detail-export-ui.test.mjs`、`node tests/employee-basic-info-export-api.test.mjs`、`node tests/employee-basic-info-export-ui.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs` 和 `npm run build`。
- 用户反馈点击“下载部门明细”返回 404 后已完成排查：
  - 本地后端 `127.0.0.1:9080` 有新接口，远端 `153.0.237.99:9080` 带 token 返回 `No handler found`；
  - Vite dev server 原先没有 `.env.development`，代理默认落到远端旧后端；
  - 已新增 `hr_web/.env.development` 指向本机后端，并新增 `tests/development-proxy-config.test.mjs`；
  - 重启前端 dev server 后，经 `127.0.0.1:8080/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 验证返回 `200 OK` 和有效 xlsx。
- 用户反馈访问 `http://127.0.0.1:8081/` 点击“下载部门明细”提示失败后已完成第二轮排查：
  - 直接 curl 本机 8081 `/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 返回 `200 OK` 和有效 xlsx；
  - 远端 `http://153.0.237.99:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 返回 `404 No handler found`；
  - 根因是本机 `html/assets/index-*.js` 仍写入 `http://153.0.237.99:8081`，浏览器点击时实际请求远端旧站点；
  - 已执行并修复 `npm run deploy:access -- --local-only`，重新构建本机 `html/`、重启 `scripts/local-deploy-server.mjs`，当前本机入口为 `assets/index-BJtjeH-p.js`，接口入口为 `http://127.0.0.1:8081`；
  - 已补 `tests/redeploy-access-script.test.mjs` 约束，并调整 `scripts/redeploy-access.mjs`：加班/夜班静态校验使用当前页面文案，慢接口验收单独放宽到 90 秒；
  - 验证：`node tests/redeploy-access-script.test.mjs` 通过；`npm run deploy:access -- --local-only` 通过；本机 8081 部门明细下载返回 `200 OK`，`unzip -t` 校验 xlsx 无错误。
- 用户继续反馈下载表中每个部门 sheet 上半部分与参考表不同，要求男女比例、学历层次、工龄结构、专业汇总等顶部结构与参考表一致。
- 已重新读取参考 workbook，确认部门 sheet 固定 1-8 行结构和合并单元格；当前实现只写“部门/人数”两行，需要后端导出支持类补齐。
- 已补 RED 测试 `EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldMatchReferenceDepartmentTopSummaryStructure`，旧实现失败于 `expected:<部门总人数> but was:<生产部>`。
- 已修改 `EmployeeDepartmentDetailExportSupport`：部门 sheet 顶部第 1-8 行按参考表输出，包含男女比例、学历层次、工龄结构、专业汇总、实习/正式/试用统计，右侧成本区标题保留但金额留空。
- GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` 通过，6 个测试 0 failures/errors。
- 导出组合回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test` 通过，15 个测试 0 failures/errors。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 本机运行验证：登录 `127.0.0.1:9080` 后，通过 `127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 下载 `/tmp/hainan_department_detail.xlsx`，`file` 确认为 Microsoft OOXML，`unzip -t` 无错误；openpyxl 抽检首个部门 sheet，合并区域包含 `C2:C6/F2:H3/F4:H6/A7:H8/F1:H1/A2:A6/B2:B6`，第 1 行为 `部门/部门总人数/男女比例/学历层次/工年结构/专业汇总`，第 9 行为 11 列人员明细表头。

## 2026-08-20
- 用户最新要求将部门明细导出范围从“全部未删除员工”收紧为“未删除且在职员工”。
- 已将 `HrmEmployeeMapper#queryDepartmentDetailExportList` 的过滤条件改为 `hrm_employee.is_del=0 and hrm_employee.entry_status in (1,3)`，与项目里其它在职口径保持一致。
- 已同步更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md` 和 `findings.md` 的范围描述。
- 已补红绿验证：
  - RED：`mvn -Dtest=HrmEmployeeMapperSqlTest#queryDepartmentDetailExportList_shouldExportUndeletedOnJobEmployeesWithoutPageFilters test` 先失败于缺少在职条件；
  - GREEN：同一命令后续通过。
- 用户追加要求：合同到期日列需根据员工管理中“员工合同”的最新一次合同截止日期加载。
- 已定位根因：员工合同页 `contractInformation` 按 `sort asc` 展示合同列表；部门明细导出旧 SQL 按 `start_time desc, contract_id desc` 取 `lastContractEndTime`，且过滤 `start_time is not null`。
- 已补 RED 测试 `HrmEmployeeMapperSqlTest#queryDepartmentDetailExportList_shouldUseLatestEmployeeContractEndTimeFromContractList`，旧 SQL 失败于未按员工合同列表排序取最新一条。
- 已修改 `HrmEmployeeMapper.xml#queryDepartmentDetailExportList`：合同到期日改为按 `coalesce(fc.sort, -1) desc, fc.start_time desc, fc.contract_id desc` 取 `hrm_employee_contract.end_time`，并去掉 `fc.start_time is not null` 过滤。
- GREEN：`mvn -Dtest=HrmEmployeeMapperSqlTest test` 通过，8 个测试 0 failures/errors。
- 部门明细组合回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test` 通过，16 个测试 0 failures/errors。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 本机下载抽检：`127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 返回 `200 OK`，响应文件名为 `湖北田野农谷生物科技有限公司人员明细表20260819.xlsx`；`file` 识别为 Microsoft OOXML，`unzip -t` 无错误；首个部门 sheet 表头含“合同到期日”，抽样员工黎冬霜、严锦与数据库新口径查询的截止日期一致。
- 用户反馈黎冬霜等员工合同到期日仍未读取后已重新复核：
  - dev 库 `hr_0003` 中黎冬霜 `employee_id=1831601326890434579` 为在职未删除，员工合同按 `sort asc` 最后一条为 `sort=39, end_time=2027-04-30`；
  - 本地 8081 重新下载 `/tmp/hainan_department_detail_contract_check.xlsx` 返回 `200 OK`，文件名为 `湖北田野农谷生物科技有限公司人员明细表20260820.xlsx`；
  - Excel 与数据库同口径全量对账结果：期望 92 人、实际 92 人，有合同到期日均为 15 人，差异 0；黎冬霜在 `采购计划部` 第 10 行，合同到期日为 `2027-04-30`。
- 用户继续反馈每个部门 sheet 的 `月固定薪资成本/月绩效薪资成本/月社保成本/月公积金成本` 没有值，并要求删除 `月伙食成本`。
- 已确认根因：导出支持类此前按“部门成本没数据源”口径主动保留标题并把 J 列金额写空，且 I6 仍写 `月伙食成本`。
- 已确认数据源：固定薪资成本使用当前薪资档案 `10101/10102/10103` 汇总；绩效薪资成本使用当前薪资档案 `41001` 汇总，未维护显示 `0`；社保、公积金使用最近有效社保月员工记录的企业社保、企业公积金字段汇总。
- 只读抽样 `hr_0003`：薪资档案没有 `41001`，最近有效社保月为 2026-07，企业社保和企业公积金存在可汇总金额。
- RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` 失败于缺少 `performanceSalaryCost(...)`，证明旧实现没有绩效成本计算入口。
- 已修改 `EmployeeDepartmentDetailExportSupport`：部门页 J2-J5 写入固定薪资、绩效薪资、社保、公积金金额；成本区零值显示 `0`；不再写 `月伙食成本`。
- 已修改 `HrmEmployeeServiceImpl`：部门明细薪资档案项加载范围增加 `41001`，并通过最近有效 `hrm_insurance_month_emp_record` 按本次员工范围回填 `corporateInsuranceAmount/corporateProvidentFundAmount`。
- GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，9 个测试 0 failures/errors。
- 部门明细组合回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，19 个测试 0 failures/errors。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 本机下载抽检：`127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 返回 `200 OK`；`file` 识别为 Microsoft OOXML，`unzip -t` 无错误；抽检部门 sheet J2-J5 已有金额，J6 空白，整本 workbook 无 `月伙食成本` 残留。
- 用户追加要求：下载部门明细中员工动态字段 `固定绩效` 需参与 `薪资待遇` 展示和 `月固定薪资成本`，例如王洪平应显示 `4150(固定)+4150(绩效)`，并且固定成本为 `8300`。
- 已确认根因：`dynamicFieldValues` 已在 `HrmEmployeeServiceImpl#exportDepartmentDetail` 中加载并传入 workbook，但 `EmployeeDepartmentDetailExportSupport#writeDepartmentSummary(...)` 未接收动态字段，明细 `薪资待遇` 也只输出 `10101/10102/10103` 固定薪资合计。
- RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldIncludeFixedPerformanceInSalaryTreatmentAndFixedSalaryCost test` 失败于实际薪资待遇仍为 `4150`。
- 已修改 `EmployeeDepartmentDetailExportSupport`：部门汇总接收 `dynamicFieldValues`，`薪资待遇` 在 `固定绩效` 非空时输出 `<固定薪资>(固定)+<固定绩效>(绩效)`；`月固定薪资成本` 改为 `10101/10102/10103 + 固定绩效`；`月绩效薪资成本` 仍只读取 `41001`。
- 已新增边界测试：`固定绩效=0.00` 时仍显示 `4150(固定)+0(绩效)`，固定成本保持 `4150`。
- GREEN/回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` 通过 10 个测试；`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过 22 个测试；`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。

# Progress: 排班上传休假未录入修复

## 2026-08-18
- 已启用项目文档优先、文件化计划、系统化调试、TDD 和完成前验证流程。
- 已读取 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md`、`progress.md`。
- 已确认本轮排查点：排班上传模板每日分组包含“休假/调休”，本地 `tbplanlist.rest_shift_type` 应区分 `adjust=调休` 与 `rest=休息/休假`。
- 当前工作区存在大量既有修改和未跟踪文件；本轮只处理排班上传相关代码与测试，不回退无关变更。
- 已补 RED 测试：
  - `previewImportExcel_shouldTreatHorizontalVacationAsRestShift` 覆盖横版模板“休假/调休”列填 `休假`；
  - `saveEmployeeDayShift_shouldTreatVacationTextAsRestShift` 覆盖服务层传入 `休假`。
- RED：`mvn -Dtest=WorkPlanServiceImplTest#previewImportExcel_shouldTreatHorizontalVacationAsRestShift+saveEmployeeDayShift_shouldTreatVacationTextAsRestShift test` 失败，确认 `休假` 缺少休息类归一化。
- 已修复 `WorkPlanServiceImpl`：
  - 横版模板“休假/调休”列把 `休假` 识别为休息类输入；
  - `normalizeShiftTypeValue(...)` 把 `休假` 归一为 `rest`；
  - `normalizeRestShiftType(...)` 把 `休假` 归一为 `rest_shift_type=rest`。
- GREEN：`mvn -Dtest=WorkPlanServiceImplTest#previewImportExcel_shouldTreatHorizontalVacationAsRestShift+saveEmployeeDayShift_shouldTreatVacationTextAsRestShift test` 通过，2 个测试。
- 排班回归：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过，62 个测试。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 补丁检查：`git diff --check -- src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java src/test/java/com/tianye/hrsystem/imple/WorkPlanServiceImplTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录排班上传 `休假` 的业务规则和实现位置，并同步修正 2026-06-04 横向模板历史段落。

# Progress: prod 一键打包脚本

## 2026-08-18
- 已读取项目 profile 与 Maven package 相关需求/开发文档，确认源码默认 profile 需保持 `dev`，服务器部署配置保留在 `application-prod.properties`。
- 已创建 `target/package-prod.sh`，脚本与 Maven package 产物目录一致。
- 脚本实现方式：复制临时项目，修改临时副本 `application.properties` 为 `spring.profiles.active=prod`，执行 `mvn -DskipTests package`，复制 jar 回 `target/hrsystem-0.0.1-SNAPSHOT.jar`，并解包验证 prod 配置。
- 已给脚本设置执行权限。
- 已运行 `mvn -Dtest=ApplicationProfileConfigTest test` 复核源码默认 profile 为 `dev`。
- 已运行 `target/package-prod.sh` 并成功生成 prod 版 jar。
- 已解包验证 jar 内 `BOOT-INF/classes/application.properties` 为 `prod`，同时包含 `application-prod.properties`。
- 已新增 `target/package-prod.command` 作为 macOS 可双击一键执行入口，并设置执行权限。
- 已验证 `zsh -n target/package-prod.command` 通过。
- 已执行 `./target/package-prod.command`，一键入口成功调用底层脚本并生成 `target/hrsystem-0.0.1-SNAPSHOT.jar`。
- 已再次解包验证 jar 内 `BOOT-INF/classes/application.properties` 为 `prod`，源码 `src/main/resources/application.properties` 仍为 `dev`。

# Progress: Maven package 失败排查

## 2026-08-17
- 已启用项目文档优先、文件化计划和系统化调试流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 与既有 `task_plan.md/findings.md/progress.md`。
- 已确认本轮项目根目录为 `/Users/jiangyongming/Project/hr/hainan`，文档存在且要求以 Maven 编译/测试作为真实错误依据。
- 已在计划文件顶部记录本轮 Maven package 排查目标和阶段。
- 已运行 `mvn package` 复现：编译通过，Surefire 阶段失败；首个失败为 `ApplicationProfileConfigTest#activeProfileLoadsDatasourceUrlForDebugStartup`，断言 `expected:<dev> but was:<prod>`。
- 已读取 `ApplicationProfileConfigTest`、`application.properties`、`application-dev.properties`、`application-prod.properties` 和 `ConnectionParsor#getDefaultConnection()`，确认根因是默认 profile 被改成 `prod`，与本地/IDE 调试默认 `dev` 契约冲突。
- 用户已将 `src/main/resources/application.properties` 第一行改回 `spring.profiles.active=dev`。
- 聚焦验证：`mvn -Dtest=ApplicationProfileConfigTest test` 通过，1 个测试，0 failures/errors。
- 完整验证：`mvn package` 通过，505 个测试，0 failures/errors，生成 `target/hrsystem-0.0.1-SNAPSHOT.jar`。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md` 和 `findings.md` 记录根因与验证结果。

# Progress: 薪资导出 Excel 批注说明

## 2026-08-17
- 已按项目文档优先、文件化计划、brainstorming、TDD 和完成前验证流程处理本轮需求。
- 已从 `watch` 扩展定位到真实薪资导出链路在 `hainan` 后端；`hr_web` 前端仅触发下载，暂不需要调整。
- 已读取后端 `hainan/docs/requirements.md`、`hainan/docs/development.md` 中薪资导出、全勤奖、超缺勤、个税、工会费相关规则；已读取前端 `hr_web` 薪资导出范围选择文档片段。
- 已定位导出核心：`SalaryMonthRecordServiceNew#exportSalaryNew(...)` 写出 Excel，`buildSalaryDataRow(...)` 提供目标字段值。
- 已确认模板列位：员工明细从第 5 行开始，`R/T/V/Z` 分别对应全勤奖、超缺勤、个人所得税、工会费。
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` testCompile 失败于缺少 `SalaryExportCommentWriteHandler`。
- 已新增 `SalaryExportCommentWriteHandler`，对目标列写 POI 批注，并跳过 `小计/合计` 行。
- 已在 `SalaryMonthRecordServiceNew#exportSalaryNew(...)` 注册 `new SalaryExportCommentWriteHandler(salaryExportList)`，不改变原有模板、金额和列顺序。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录批注覆盖范围、计算依据和实现位置。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 68 个测试，保留既有 Maven POM warnings。
- 编译验证：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 目标文件补丁检查：`git diff --check -- src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java src/main/java/com/tianye/hrsystem/modules/salary/support/SalaryExportCommentWriteHandler.java src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

# Progress: 残疾员工工会费计算规则调整

## 2026-08-17
- 已启用项目文档优先、文件化计划、brainstorming、TDD 和完成前验证流程。
- 已读取 `docs/requirements.md`、`docs/development.md`、既有 `task_plan.md/findings.md/progress.md`，确认本轮落在薪资核算工会费规则。
- 已定位旧规则：`SalaryComputeServiceNew#computeSalary(...)` 把残疾员工同时排除在工会费和个税计算之外；本轮应拆开两条规则。
- 已按 TDD 补 `SalaryComputeServiceNewTest#computeSalary_disabledEmployee_shouldPayUnionFeeAndSkipTax`：
  - RED：`mvn -Dtest=SalaryComputeServiceNewTest test` 失败于残疾员工工会费为 `0`。
  - GREEN：改动后同一命令通过 14 个测试。
- 已修改 `SalaryComputeServiceNew#computeSalary(...)`：工会费不再按残疾状态豁免，仍由 `calculateUnionFee(...)` 判断公司、员工状态、转正日期和应发工资条件；残疾员工个税免税分支保持不变。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录残疾员工工会费规则和测试证据。
- 验证完成：
  - `mvn -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest test` 通过 79 个测试；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。

# Progress: 后端调试模式 jdbcUrl 缺失排查

## 2026-08-16
- 已启用项目文档优先、文件化计划、系统化调试和 TDD 流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 的总览与数据库/调试相关记录，确认本轮需要围绕调试 profile 和动态数据源配置排查根因。
- 已在 `task_plan.md`、`findings.md`、`progress.md` 顶部记录本轮目标、假设和下一步验证计划。
- 已定位根因：`application.properties` 中 `spring.profiles.activ=dev` 拼写错误，导致 `ConnectionParsor#getDefaultConnection()` 没有加载 `application-dev.properties`，默认数据源缺少 `spring.datasource.url`。
- 已新增 RED 测试 `ApplicationProfileConfigTest#activeProfileLoadsDatasourceUrlForDebugStartup`，初次运行失败于 `expected:<dev> but was:<null>`。
- 已修复配置键为 `spring.profiles.active=dev`。
- 验证完成：
  - `mvn -Dtest=ApplicationProfileConfigTest test` 通过；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录调试模式默认 profile 和 JDBC URL 配置契约。

# Progress: 顶栏钉钉 API 调用量展示与提醒

## 2026-08-16
- 已启用项目文档优先、文件化计划、brainstorming、TDD、UI/UX 和完成前验证流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 与前端 `../hr_web/docs/requirements.md`、`../hr_web/docs/development.md` 中钉钉调用量、右上角顶栏和系统设置权限相关约束。
- 已在 `task_plan.md`、`findings.md` 顶部记录本轮目标、假设和文档约束。
- 已定位后端 `postresultlog`、`DDTalkResposeLogger`、`ddtaskresult`、前端 `TopHeader.vue`、API helper 和 Node 源码测试结构。
- 已完成 RED/GREEN：
  - 后端 RED 先失败于缺少 `HrmDingTalkApiUsageServiceImpl`；
  - 后端新增 `DingTalkApiUsageVO/DingTalkApiUsageDistributionVO/IHrmDingTalkApiUsageService/HrmDingTalkApiUsageServiceImpl/HrmDingTalkApiUsageController`，并扩展 `postresultlogRepository`；
  - 前端 RED 先失败于缺少 `src/api/hrm/dingtalkApiUsage.js`；
  - 前端新增调用量 API helper、`dingtalk-api-usage` 工具函数，并在 `TopHeader.vue` 接入按钮、悬浮分布和提醒弹窗。
- 已更新后端/前端 `docs/requirements.md` 与 `docs/development.md`，记录接口、配置项、统计口径、UI 交互和提醒规则。
- 已通过验证：
  - `mvn -Dtest=HrmDingTalkApiUsageServiceImplTest,HrmDingTalkApiUsageControllerTest test` 通过 3 个测试；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings；
  - `node tests/dingtalk-api-usage-api.test.mjs` 通过；
  - `node tests/dingtalk-api-usage-utils.test.mjs` 通过；
  - `node tests/top-header-dingtalk-api-usage.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 和 chunk size warnings。

# Progress: 排班管理考勤信息改读同步数据

## 2026-08-12
- 已启用项目文档优先、文件化计划、系统化调试和 TDD 流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 的排班管理、同步考勤和钉钉调用量相关约束。
- 已在 `task_plan.md`、`findings.md`、`progress.md` 顶部记录本轮目标、假设和已知约束。
- 已定位排班管理展示查询的直接钉钉调用点：
  - `HrmAttendanceDataController#getPlanDataByGroup(...)` 通过 `getUserListByGroup(...)` 请求钉钉组成员；
  - 同方法按 7 天批次请求钉钉 `attendance/schedule/listbyusers`；
  - `WorkPlanServiceImpl#getUsersForDisplay/getAllGroupsForDisplay` 缓存刷新时仍可能回退钉钉。
- 已补 RED 并完成修复：
  - `getPlanDataByGroup(...)` 改为读取本地 `hrm_attendance_plan`，回填 `tbattendanceuser`，并兼容新旧考勤组 ID；
  - `getAllGroupsForDisplay(...)` 改为本地 `hrm_attendance_group + hrm_attendance_shift` 构造展示组和班次；
  - `getUsersForDisplay(...)` 本地快照为空时返回空列表，不再回退钉钉；
  - `getClassListByGroup(...)` Redis 缓存缺失时从本地组/班次表构造下拉；
  - `getShiftList(...)` 遗留班次列表接口改为读取本地 `hrm_attendance_shift` 快照。
- 验证完成：
  - `mvn -Dtest=HrmAttendanceDataControllerTest,WorkPlanServiceImplTest test` 通过 55 个测试；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings；
  - `HrmAttendanceDataController` 中 `schedule/listbyusers`、`attendance/group/memberusers/list`、`attendance/shift/list` grep 无命中。

# Progress: 排班管理矩阵翻页修复

## 2026-08-12
- 已启用项目文档优先、文件化计划、系统化调试、TDD、UI/UX 和完成前验证流程。
- 已读取后端 `hainan/docs/requirements.md`、`hainan/docs/development.md` 与前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md` 的排班管理相关段落。
- 已定位根因：`Scheduling.vue` 传入公共 `Table` 的是完整 `employeeMatrixRows`，未监听 `get-data` 分页事件，也未按页码切片；公共 `Table` 自身只发事件不改变外部数据。
- 已补 RED 测试固定分页切片和页面事件绑定，初始失败于缺少 `buildWorkPlanMatrixPageRows` 导出。
- 已实现前端修复：
  - `work-plan-utils.js` 新增 `buildWorkPlanMatrixPageRows(...)`；
  - `Scheduling.vue` 新增 `matrixPage/pagedEmployeeMatrixRows/handleMatrixPageChange/resetMatrixPage`；
  - 表格改为展示当前页矩阵行，分页总数仍取完整矩阵行数；
  - 筛选、重置、切换月份回到第一页，导出仍使用完整筛选结果。
- 已更新后端/前端 `docs/requirements.md` 与 `docs/development.md`。
- RED：`node tests/work-plan-scheduling-records.test.mjs` 先失败于缺少 `buildWorkPlanMatrixPageRows` 导出。
- GREEN 验证：
  - `node tests/work-plan-utils.test.mjs` 通过；
  - `node tests/work-plan-scheduling-records.test.mjs` 通过；
  - `node tests/work-plan-api.test.mjs` 通过；
  - `node tests/work-plan-records-page.test.mjs` 通过；
  - `node tests/work-plan-time-picker.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

# Progress: 加班夜班统计三项出勤可编辑保存

## 2026-08-10
- 已启用项目文档优先、文件化计划、brainstorming、TDD 和 UI/UX 流程。
- 已读取后端 `hainan/docs/requirements.md`、`hainan/docs/development.md` 和前端 `../hr_web/docs/requirements.md`、`../hr_web/docs/development.md`。
- 已确认本轮跨后端 `hainan` 与前端 `hr_web`：统计明细应落库保存，两个前端视图都需要展示可编辑单元格。
- 已定位统计接口、明细实体、repository、前端矩阵工具函数和现有加班夜班测试。
- 已补 RED 测试并确认失败：
  - 后端 `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 失败于新 BO 缺失；
  - 前端 `node tests/overtime-night-api.test.mjs` 失败于新保存 API 缺失；
  - 前端 `node tests/overtime-night-utils.test.mjs` 失败于新保存 payload 工具缺失；
  - 前端 `node tests/attendance-display-columns.test.mjs` 失败于页面未接入编辑保存。
- 已实现后端保存接口、实体字段、VO 字段和人工调整优先查询：
  - `POST /hrmOvertimeNightStatistics/updateAttendanceSummary` 按 `employeeId + month` 批量更新该员工当月所有统计明细；
  - 保存 `expected_attendance_hours`、`actual_attendance_hours`、`accrued_attendance_hours`，并同步折算旧兼容天数字段；
  - `attendance_manual_adjusted=1` 后，列表、单人月度明细和显示所有日明细优先返回人工保存值，不再被休息制度刷新覆盖。
- 已新增迁移脚本 `docs/sql/2026-08-10_overtime_night_attendance_manual_hours.sql`，覆盖 `hr_0001` 至 `hr_0005`。
- 2026-08-10 19:07 已执行该迁移脚本到 dev MySQL `153.0.237.98`，并复核 `hr_0001` 至 `hr_0005` 均存在 `expected_attendance_hours`、`actual_attendance_hours`、`attendance_manual_adjusted` 三列。
- 后端验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过 58 个测试，保留既有 Maven POM warnings。
- 已实现前端接入：
  - `overtimeNight.js` 新增保存 API；
  - `overtime-night-utils.js` 新增编辑初始值和保存 payload 构造；
  - 单人查看弹窗与显示所有页面的实际出勤矩阵三列均改为输入框 + 保存按钮，保存时提交当前行三项小时值；
  - 保存成功刷新当前视图，实际/应计出勤计算过程 tooltip 保留。
- 已补下游小时优先读取：考勤汇总同步和行政体系导出在新字段存在时优先读取手工小时，旧数据才按天数乘 8 兜底。
- Fresh 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 77 个测试；
  - `mvn -DskipTests compile` 通过；
  - `node tests/overtime-night-api.test.mjs`、`node tests/overtime-night-utils.test.mjs`、`node tests/attendance-display-columns.test.mjs`、`node tests/overtime-night-page.test.mjs` 均通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 2026-08-10 18:02 最终 fresh 复核：
  - 后端目标文件 `git diff --check` 无输出，后端和前端目标文件尾随空白扫描无输出；
  - 后端 `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 77 个测试；
  - 后端 `mvn -DskipTests compile` 通过；
  - 前端 `node tests/overtime-night-api.test.mjs`、`node tests/overtime-night-utils.test.mjs`、`node tests/attendance-display-columns.test.mjs`、`node tests/overtime-night-page.test.mjs` 均通过；
  - 前端 `npm run build` 通过，仍仅保留既有 `::v-deep` 过时警告和 chunk size warning。

# Progress: 薪资新建次月防重复与7月恢复方案

## 2026-08-10
- 已将“误建后续月份”的处理从 DBA 手工 SQL 升级为前端可见、后端校验的“恢复薪资月份”能力。
- 已实现并验证：预览后续月份、阻断已有员工薪资明细/工资条/发送记录的恢复、删除空后续月份、恢复目标月状态为可继续处理。
- 已完成回归：`mvn -Dtest=SalaryMonthRecordRecoveryTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordNextMonthTest test` 通过；前端 `node tests/salary-month-recovery.test.mjs` 与 `node tests/salary-create-next-month.test.mjs` 通过。
- 已启用项目文档优先、文件化计划、系统化调试、TDD 和完成前验证流程。
- 已读取后端 `hainan/docs/requirements.md`、`hainan/docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认薪资管理页和月薪资记录为本轮修改范围。
- 已定位前端根因：`SalaryManage.vue#addMony` 未防重复提交，且月份选择器为空时使用系统当前年月兜底；`init()` 未把后端当前薪资记录年月回填到选择器。
- 已定位后端根因：`SalaryMonthRecordServiceNew#updateCheckStatus(...)` 收到源年月后仍按最新 `create_time` 推下一月；重复提交 7 月源月份会继续基于新建的 8 月创建 9 月。
- 已确认 `addNextMonthSalary()` 和旧服务 `SalaryMonthRecordService_Bak` 存在同类风险；`salaryAudit(...)` 源记录选择正确但缺少次月已存在时跳过的幂等保护。
- 下一步补 RED 测试：前端薪资新建次月按钮 loading/默认源月/移除系统月兜底；后端次月创建 helper 基于源年月且次月已存在时不重复保存。
- 后端聚焦测试已完成：`mvn -Dtest=SalaryMonthRecordNextMonthTest test` 通过，2 个测试、0 失败。
- 前端聚焦测试已完成：`node tests/salary-create-next-month.test.mjs` 通过。
- 代码修复已落地：次月创建 helper 先查下一月是否已存在，存在则复用；前端按钮保留 `createLoading`，成功后保持当前源月不变。
- 已更新恢复方案文档：`docs/plans/2026-08-10-salary-next-month-recovery-solution.md`，主方案为系统内“恢复薪资月份”，只读核查 SQL 仅保留为接口不可用时的应急参考；本轮未直接执行数据库删除/更新。

# Progress: 审批数据手工添加显示修复

## 2026-08-03
- 已读取后端和前端需求/开发文档，确认本轮仍沿用 `employeeId` 精确锁定员工、手工添加只写本地快照、手工 `duration/durationUnit` 优先于起止时间自动计算的规则。
- 已定位新增成功但列表不显示的前端原因：保存成功后只按原筛选刷新，没有切换到新增审批时间段所在月份，也没有清理可能排除新增数据的审批类型/子类型筛选。
- RED 验证：
  - `node tests/approval-duration-utils.test.mjs` 失败于 `formatApprovalDateTimeMinute is not a function`；
  - `node tests/attendance-approval-fetch-utils.test.mjs` 失败于缺少 `buildManualAddApprovalListRefreshState` 导出；
  - `node tests/attendance-approval-dialog.test.mjs` 失败于添加审批日期控件未隐藏秒。
- 已实现前端修复：
  - 审批数据列表开始/结束时间按分钟展示；
  - 添加弹窗审批日期控件不展示秒，提交仍保留秒级格式；
  - 手工添加成功后切换到新增时间段所在月份，清理审批类型/标签/子类型/部门筛选；行级添加按当前行员工姓名辅助筛选；
  - `18:00-19:00` 手工填写 `2小时` 时，时长列按本地快照展示 `2小时(0.25天)`。
- 验证完成：
  - `node tests/approval-duration-utils.test.mjs` 通过；
  - `node tests/attendance-approval-fetch-utils.test.mjs` 通过；
  - `node tests/attendance-approval-dialog.test.mjs` 通过；
  - `node tests/attendance-approval-api.test.mjs` 通过；
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过 35 个测试；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 收尾复核：前端四个审批数据测试已在缩进修正后重跑通过；`npm run build` 重跑通过；目标文件尾随空白检查无输出。

# Progress: 审批数据行内添加员工审批

## 2026-08-03
- 用户要求：审批数据列表操作列后新增行级“添加数据”，按当前行员工添加本地审批数据；同名员工用姓名 + 手机号识别；删除审批日期后的自动时长展示；合计时长可手工输入且只允许数字和小数点，末尾小数点补 `0`。
- 已实现后端：
  - 审批数据列表 VO 与 Mapper 返回 `employeeId/mobile`；
  - 手工添加 BO 接收 `duration/durationUnit`；
  - 手工添加服务按小时解析手工合计时长，单范围直接写入，多范围按原始时长比例拆分。
- 前端配套已实现：
  - 列表展示手机号和行级“添加数据”按钮；
  - 行级添加弹窗锁定当前行 `employeeId`，不展示员工/部门穿梭框；
  - 合计时长输入支持手工编辑、输入过滤和末尾小数点兜底；
  - 添加 payload 提交 `duration/durationUnit='小时'`。
- 已完成验证：
  - 后端 `mvn -Dtest=HrmAttendanceApprovalServiceImplTest test` 通过，23 个测试；
  - 后端回归 `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，35 个测试；
  - 前端 `node tests/approval-duration-utils.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs`、`node tests/attendance-approval-dialog.test.mjs`、`node tests/attendance-approval-api.test.mjs` 通过；
  - 前端 `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 收尾 fresh 复核：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，35 个测试、0 失败；前端同一组审批数据测试与 `npm run build` 通过。

# Progress: 同步考勤与审批获取进度条增强

## 2026-08-03
- 用户要求：员工管理同步考勤执行错误必须在进度条中提示非技术型中文原因，错误后进度条不能消失；同步成功后必须用户点确认后关闭；错误修正后支持断点接续同步；获取审批数据也新增并复用该进度条逻辑。
- 已读取后端项目需求/开发文档和前端项目需求文档；前端开发文档缺失，计划本轮结束创建 `hr_web/docs/development.md`。
- 已在 `task_plan.md` 和 `findings.md` 顶部记录本轮目标、假设和文档约束。
- 下一步定位员工管理同步考勤、进度查询和审批数据获取的前后端代码入口。
- 已实现并复核员工管理同步考勤进度增强：后端保留 `SUCCESS/FAILED` 进度状态，失败写入非技术型中文错误，断点接续只在失败且参数一致时启用；前端成功/失败都停留进度弹窗，点击“我知道了”才关闭。
- 已实现并复核审批数据获取进度条：后端新增 `/hrmAttendanceApproval/queryFetchProgress`，前端“获取审批数据”确认后展示进度弹窗，复用同步考勤进度解析逻辑，成功/失败均需用户确认关闭。
- 续接复核发现并修正前端共用进度解析细节：失败完成态不再强制显示 `100%`，而是保留后端或当前进度，同时过滤技术型错误文本。
- 已更新后端与前端 `docs/requirements.md`、`docs/development.md`，并补充本轮 `task_plan.md`、`findings.md`。
- 下一步运行 fresh 后端/前端定向测试、构建和空白检查。
- Fresh 验证完成：
  - 后端 `mvn -Dtest=HrmAttendanceDataServiceImplTest,HrmAttendanceDataControllerTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest test` 通过，79 个测试，0 失败；
  - 前端 `node tests/attendance-sync-progress-utils.test.mjs`、`node tests/employee-attendance-sync-progress-dialog.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs`、`node tests/attendance-approval-api.test.mjs`、`node tests/attendance-approval-dialog.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs` 均通过；
  - 前端 `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - 后端目标文件 `git diff --check -- ...` 无输出，前后端目标文件尾随空白扫描无输出。

# Progress: 2026-07 审批数据获取失败排查

## 2026-08-03
- 用户反馈：获取 2026-07 月审批数据失败。
- 已启用项目文档优先、文件化计划和系统化调试流程。
- 已读取项目需求/开发文档和既有计划/发现/进度文件；确认审批快照和考勤统计链路是本轮重点。
- 下一步定位精确报错文本、接口入口和 2026-07 月份参数处理。
- 已搜索审批相关代码和文档，确认后端存在独立审批数据 controller/service/mapper，下一步读取该链路实现和测试。
- 已读取 `HrmAttendanceApprovalSyncServiceImpl` 主要流程：目标员工解析、员工可见流程模板、审批实例列表、审批实例详情、业务月份过滤、陈旧数据清理和完成标记。
- 本地日志未查到审批抓取异常，`logs/hainan-overtime-debug.log` 主要是加班夜班统计调试内容。
- 复现准备中首次 `curl` 登录命令未给 URL 加引号，被 zsh 通配符解析拦截；下一步用引号重试。
- 已确认登录接口需 `POST /hrsystem/login`，GET 会被拦截；本地 9080 可以正常拿到 hbadmin token。
- 单员工复现：调用本地 `/hrmAttendanceApproval/fetchMonthData`，payload `{"month":"2026-07","employeeIds":[1831601326890434565],"approvalTypes":["all"]}` 返回成功，`insertedCount=3`。
- 当前判断：需要重点检查全员/多人抓取路径，尤其是流程编码解析是否按员工重复调用和全员中断策略。
- 已实现审批抓取韧性修复：
  - `resolveTargetUsers(employeeIds, token)` 中单个员工钉钉用户预解析异常改为记录并跳过，避免全员/月度抓取被单人异常中断；
  - 员工表缺 `dingtalk_user_id` 且姓名手机号反查失败时，尝试复用已存在且经钉钉通讯录姓名手机号校验通过的 `tbattendanceuser` 映射；
  - 钉钉限流、频控、调用频率错误翻译为明确中文提示。
- 已新增回归测试覆盖限流文案、多人预解析单人失败跳过、本地映射校验复用。
- Fresh 验证：
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，63 个测试，0 失败；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings；
  - 本轮目标文件尾随空白检查无输出，已跟踪文档 `git diff --check -- docs/requirements.md docs/development.md findings.md progress.md task_plan.md` 无输出。

# Progress: 李凤皇行政导出三项出勤修复

## 2026-07-26
- 用户反馈：李凤皇行政导出应出勤、实出勤、应计出勤应为 `200 / 196 / 200`，当前导出为 `184 / 180 / 184`。
- 已读取项目需求/开发文档和现有计划/发现/进度记录。
- 当前判断：加班/夜班统计页面已有 `restType=2` 固定月休刷新逻辑，行政导出需要对齐统计查询结果，不能只取旧统计明细落库值。
- 已读取行政导出服务、统计查询 helper 和现有测试，确认导出行由旧明细 `AdministrativeAttendanceAccumulator#toRow(...)` 直接写入 E/F/G。
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于 `expected:<200.0> but was:<184.0>`，确认旧导出没有使用统计查询层刷新后的出勤值。
- 已实现：行政导出在审批聚合和实出勤兜底重算后，调用 `overtimeNightStatisticsService.queryPageList(...)` 获取同月统计页面当前值，匹配员工后覆盖 E/F/G；查询缺失时保留旧明细兜底。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过 15 个测试。
- 回归验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 19 个测试。
- 编译验证：`mvn -DskipTests compile` 通过；保留既有 Maven POM warnings。
- 统计链路验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldUseFixedMonthlyRestAttendanceWhenRestTypeIsFixedEvenIfAffiliationIsAdministrative test` 通过。
- 只读核查 `hr_0003 / 2026-06`：行政体系且固定月休、旧明细仍停留在 23 天的员工共 4 人：李凤皇、杨太琴、潘红琼、王芳（TYNG-450）。本轮修复后这些人的导出三项出勤会随统计页面当前口径刷新。
- 目标文件空白检查通过；全仓库 `git diff --check` 仍被其它未关联文件中的尾随空白拦截，本轮未处理那些文件。
- Final fresh 验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 19 个测试；`mvn -DskipTests compile` 通过；本轮目标文件 `git diff --check -- ...` 无输出。

# Progress: 行政体系考勤导出出差列修复

## 2026-07-25
- 用户反馈：人工统计中所有员工出差均为 `0`，当前行政体系考勤导出与人工统计不同。
- 已读取本轮相关流程、项目需求/开发文档和计划文件。
- 已定位当前代码映射：`bizType=2`、`出差`、`外出` 都会被 `resolveAdministrativeAttendanceApprovalType(...)` 归为 `"出差"`，并写入导出 L 列。
- 只读核查 `hr_0003 / 2026-06`：当前映射会导致 6 名行政体系员工出差列非 0，分别为罗毅龙、李明明、张明、赵聪、闫倩、王琪。
- 下一步补 RED 测试，锁定钉钉出差/外出审批不得写入行政体系导出 L 列。
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于 `expected:<0.0> but was:<1.5>`，确认旧实现会把出差和外出审批折算进 L 列。
- 已修复：行政导出审批类型映射不再把 `bizType=2`、`出差`、`外出` 归入导出列，L 列保持默认 `0`。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过 14 个测试。
- 回归验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 18 个测试。
- 编译验证：`mvn -DskipTests compile` 通过；仍有既有 Maven POM warning：`jsch/easyexcel` 重复依赖声明和两个 `systemPath` 警告。
- 已更新 `docs/requirements.md` 与 `docs/development.md`：行政导出 L 列出差按人工统计口径为 0，钉钉出差/外出审批不进入该列。

# Progress: 行政体系考勤导出实出勤小时修复

## 2026-07-25
- 用户反馈：吴晓霞实出勤应为已入库/统计结果 `185.5`，当前行政体系考勤导出为 `184.x`；要求检查所有行政体系员工是否存在同类问题。
- 已启用项目文档优先、文件化计划、brainstorming、系统化调试、TDD 和完成前验证流程。
- 已读取 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md`、`progress.md`，确认行政体系导出旧需求写的是 `actual_attendance_days * 8`，与加班/夜班统计页面当前实际小时口径不一致。
- 已定位代码根因：`HrmProduceAttendanceServiceImpl.AdministrativeAttendanceAccumulator#toRow(...)` 当前用 `actualAttendanceDays * 8` 写 F 列实出勤小时，无法体现审批加班与请假/调休扣减后的小时小数。
- 下一步补 RED 测试，固定吴晓霞式 `23天 + 2小时加班 - 0.5小时调休 = 185.5小时` 的导出行为。
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于 `expected:<185.5> but was:<184.0>`，确认旧导出 F 列没有使用统计实际小时口径。
- 已修复：行政体系导出在审批数据聚合后按 `应出勤小时 + 加班审批小时 - 事假/病假/调休/年假扣减小时` 重算 F 列；`expectedAttendanceHours <= 0` 的历史异常行保留原明细小时兜底。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过 14 个测试。
- 批量只读核查：`hr_0003 / 2026-06` 行政体系且应出勤大于 0 的员工共 `38` 人，旧导出实出勤与统计口径不一致 `19` 人；吴晓霞为旧 `184`、新 `185.50`。
- 回归验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 18 个测试。
- 编译验证：`mvn -DskipTests compile` 通过；仍有既有 Maven POM warning：`jsch/easyexcel` 重复依赖声明和两个 `systemPath` 警告。

# Progress: 考勤汇总下载行政体系考勤

## 2026-07-25
- 用户追加要求：下载行政体系考勤时必须能选择日期，下载数据基于选择的日期。
- 已复核现有实现：前端直接用筛选年月或当前年月下载；后端按 `year/month` 聚合数据，因此本轮优先改前端下载弹窗和年月参数来源。
- RED 前端：`node tests/upload-attendance-page.test.mjs` 失败于缺少 `openAdministrativeAttendanceDownloadDialog`，确认当前按钮未先打开下载日期弹窗。
- 已实现前端下载日期弹窗：点击“下载行政体系考勤”只打开弹窗；确认时必须选择 `YYYY-MM-DD` 日期，导出年月按该日期所在月份拆分并提交。
- 已更新后端/前端需求和开发文档，删除“未选年月默认当前年月”的旧导出口径。
- 追加验证：
  - `node tests/upload-attendance-page.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 17 个测试。
- 用户要求：考勤汇总中新增“下载行政体系考勤”按钮；导出表结构参考桌面行政系统考勤汇总表模板，标题年月用当前/所选年月，编制单位用当前登录账号所属公司，制表时间用当前日期；姓名后新增部门列；月天数、应出勤小时、实出勤小时读取加班/夜班统计；H-S 列读取本地审批数据；S 列之后不导出。
- 已启用项目文档优先、文件化计划、brainstorming、TDD、UI/UX 和完成前验证流程。
- 已读取后端与前端需求/开发文档，确认考勤汇总页面和后端服务入口。
- 已解析业务 Excel 模板：第一张 sheet `6月`，A-S 为主体，T-W 为剩假尾部列；本轮计划通过新增 C=部门、删除原 G=公休/假期、删除 S 后尾部列保持最终 A-S。
- 当前准备补 RED 测试，先让后端导出接口和前端按钮/API 在旧代码下失败，再实现。
- 续接复核：当前后端 controller/service/interface 均无行政体系考勤下载方法；前端 `Upload.vue` 尚无下载按钮，API 文件尚无 Blob 下载 helper。下一步补后端服务导出测试和前端源码契约测试并确认红灯。
- RED 后端：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 编译失败于 `AdministrativeAttendanceReportMetricVO` 缺失。
- RED 前端：`node tests/upload-attendance-page.test.mjs` 断言失败于 `downloadAdministrativeAttendance` API 未导出。
- 已实现后端：
  - 复制业务模板到 `src/main/resources/export/行政体系考勤汇总表.xlsx`；
  - 新增 `POST /hrmProduceAttendance/downloadAdministrativeAttendance`；
  - 新增 `AdministrativeAttendanceExportSupport` 动态生成只到 S 列的行政体系考勤 Excel；
  - 新增 `AdministrativeAttendanceReportMetricVO` 和 Mapper SQL 聚合钉钉报表异常字段；
  - 服务层按加班/夜班统计明细生成行，按员工档案过滤行政体系和真实部门，审批列按本地审批快照聚合。
- 已实现前端：
  - `upload.js` 新增 Blob 下载 API；
  - `Upload.vue` 新增“下载行政体系考勤”按钮、loading/disabled 和下载处理；
  - `upload-filter-utils.js` 新增默认导出年月 helper。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 17 个测试；`node tests/upload-attendance-page.test.mjs` 通过。
- 构建：`mvn -DskipTests compile` 通过；`npm run build` 通过，保留既有 Maven POM warnings、`::v-deep` 过时警告和 chunk size warning。
- 已更新后端/前端 `docs/requirements.md` 与 `docs/development.md`。
- Fresh 收尾验证：
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 17 个测试；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings；
  - `node tests/upload-attendance-page.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

# Progress: 排班管理导入连班与矩阵标识

## 2026-07-25
- 用户要求：排班管理上传排班文档中“是否连班”列为空时按员工表是否连班自动配置；下载排班模板新增电话列；上传时电话有内容按姓名+电话、电话为空按姓名；排班管理矩阵单元格显示“连/休息/调休”。
- 已启用项目文档优先、文件化计划、brainstorming、TDD、UI/UX 和完成前验证流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md`，以及前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 已在 `task_plan.md` 和 `findings.md` 顶部记录本轮目标、假设和初步设计。
- 已实现后端导入和模板规则：
  - 下载模板新增“电话”列，并按电话列后的位置重建横向日期分组；
  - 平铺导入支持“电话”和旧“手机号”，新增“是否连班”；
  - 横向导入支持“姓名 + 电话”，旧无电话模板或电话为空时按员工姓名匹配；
  - 导入“是否连班”有内容时按 Excel 值，空值按员工表 `is_continuous_shift`；
  - `customContinuousShiftExplicit` 控制导入显式连班值，防止后续员工默认值覆盖。
- 已实现前端矩阵和提交兼容：
  - 白班连班矩阵单元格主文案不再追加“连班”，副文案显示“连”；
  - 休息/调休继续直接显示在单元格；
  - 导入预览到提交 payload 透传 `customContinuousShiftExplicit`。
- 已更新后端/前端 `docs/requirements.md`、`docs/development.md`，并把本轮 `task_plan.md` 顶部阶段标记完成。
- 已完成一轮验证：后端 `WorkPlanServiceImplTest,WorkPlanListControllerTest,HrmEmployeeMapperSqlTest` 60 个测试通过，`mvn -DskipTests compile` 通过；前端三个排班测试和 `npm run build` 通过，构建仅保留既有警告。
- Fresh 复核验证：
  - `mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest,HrmEmployeeMapperSqlTest test` 通过 60 个测试；
  - `mvn -DskipTests compile` 通过，保留既有 `jsch/easyexcel` 重复依赖和 `systemPath` warning；
  - `node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - 后端目标文件 `git diff --check` 无输出；前端目标文件因位于父级仓库未跟踪路径，已用尾随空白扫描补检且无输出。

## 2026-07-25 追加：排班上传匹配与模板表头优化
- 已复核用户最新规则：电话单元格为空时直接按姓名匹配，不再要求同名员工必须补电话。
- 已定位旧规则测试：`WorkPlanServiceImplTest` 中同名缺电话相关测试仍期望报错。
- 已定位模板样式问题：`WorkPlanListController#fillTemplateDateRow` 未合并每个日期组，也未设置居中和交替底色。
- 已调整测试并验证红灯：旧实现会在电话为空且系统存在同名员工时报错，模板日期组也没有合并区域。
- 已实现新规则：电话为空时 `resolveImportEmployee(...)` 直接按姓名匹配；电话有内容时仍按“姓名 + 电话”唯一匹配。
- 已实现模板样式：日期组第一行合并 5 列，日期和下方 5 个字段表头居中，同一日期组同色，相邻日期组两色循环；31 号后无效日期组清空。
- 已更新后端/前端需求和开发文档，清理“系统同名必须补电话”的旧口径。
- 追加验证：
  - `mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 54 个测试；
  - `mvn -DskipTests compile` 通过，保留既有 `jsch/easyexcel` 重复依赖和 `systemPath` warning。

# Progress: 排班员工连班自动带出

## 2026-07-25
- 用户要求：添加排班功能和排班管理修改员工排班时，根据员工表设置的“是否连班”字段自动设置排班记录“是否连班”。
- 已启用项目文档优先、文件化计划、brainstorming、TDD 和完成前验证流程。
- 已复用现有 `task_plan.md`、`findings.md`、`progress.md` 并在顶部追加本轮任务。
- 过程错误：`session-catchup.py` 首次误用 `sh` 运行失败，已改用 `python3` 运行且无恢复输出。
- 已读取项目需求/开发文档排班相关段落，确认现有连班规则只允许白班自定义排班保留连班。
- 已定位批量保存与单日修改共同落库链路：`WorkPlanServiceImpl#buildLocalCustomAssignments -> buildPersistPlanRows`。
- 只读查库确认员工表列名为 `hrm_employee.is_continuous_shift`，取值 `1=是，2=否`。
- 当前准备补 RED 测试：员工 `isContinuousShift=1` 时自定义白班排班自动保存连班，员工为否或夜班时保存非连班。
- RED：`mvn -Dtest=WorkPlanServiceImplTest test` 失败于缺少 `setIsContinuousShift` 与 `findAllByUserIdIn`。
- 已实现：员工模型/PO/BO/VO 和员工列表 SQL 接入 `isContinuousShift`；排班批量保存与单日修改按员工表字段覆盖连班；夜班、休息、标准班次仍强制非连班。
- RED：迁移脚本测试失败于 `docs/sql/2026-07-25_hrm_employee_is_continuous_shift.sql` 不存在；已新增幂等脚本。
- RED：多人同设置测试失败于重复创建自定义班次；已新增解析缓存复用相同开始/结束/跨天/白夜/连班组合。
- GREEN：`mvn -Dtest=WorkPlanServiceImplTest,HrmEmployeeMapperSqlTest test` 通过 42 个测试。
- 收尾调整：`docs/sql/2026-07-25_hrm_employee_is_continuous_shift.sql` 改为 `AFTER full_attendance`，避免脚本依赖 `rest_type` 字段先存在；`HrmEmployeeMapperSqlTest` 已增加契约断言。
- 最终验证：
  - `mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest,HrmEmployeeMapperSqlTest test` 通过 56 个测试；
  - `mvn -DskipTests compile` 通过，主源码 1046 个文件编译成功；
  - 本轮目标文件 `git diff --check` 无输出。

# Progress: 考勤汇总高级筛选部门查询

## 2026-07-24
- 用户要求前端考勤汇总高级筛选新增“部门”查询；本轮后端需配套支持真实部门 ID 查询。
- 已读取 `docs/requirements.md` 和 `docs/development.md` 相关考勤汇总规则：`department=1/2` 是所属体系/考勤汇总部门类型，不是真实组织部门。
- 已确认当前 `QueryMonthAttendanceBO` 缺少 `deptIds`，`HrmProduceAttendanceMapper.xml` 仅支持 `p.department` 筛选。
- RED：`mvn -Dtest=HrmProduceAttendanceMapperXmlTest test` 失败于 `QueryMonthAttendanceBO` 缺少 `deptIds`。
- 已实现：`QueryMonthAttendanceBO` 新增 `deptIds`，`HrmProduceAttendanceMapper.xml` 通过 `e.dept_id in (...)` 支持真实部门查询，原 `p.department` 所属体系筛选不变。
- GREEN：`mvn -Dtest=HrmProduceAttendanceMapperXmlTest test` 通过 3 个测试。
- 前端配套验证：`node tests/upload-attendance-page.test.mjs`、`npm run build` 通过。

# Progress: 个税附加导入模板下载与年月导入校验

## 2026-07-23
- 用户要求：个税累计、附加累计、年度附加扣除分别新增“下载数据模版”按钮；模板来源为桌面 `导入模版` 目录三份 Excel；个税累计和附加累计未选择年月禁止导入。
- 已启用项目文档优先、文件化计划、brainstorming、TDD、UI/UX 和完成前验证流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md`，以及前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 已确认现有风格：下载按钮应放在页面右侧操作区，使用 Element Plus 按钮 loading/disabled，下载失败给中文提示。
- 已确认需要新增后端 classpath 固定资源和三个下载接口；前端需要新增三个 API helper 和三个页面的按钮/下载处理。
- RED 后端：`mvn -Dtest=HrmPersonalIncomeTaxControllerTest,HrmAdditionalTemplateControllerTest test` 测试编译失败，三个 controller 均缺对应下载方法。
- RED 前端：`node tests/salary-tax-additional-template-api.test.mjs` 失败于 API 未导出 `downloadAdditionalTemplate`；`node tests/salary-tax-additional-template-ui.test.mjs` 失败于个税页面缺“下载数据模版”按钮。
- 已实现后端：三份模板复制到 `src/main/resources/export`；新增 `ExcelTemplateDownloadUtils`；三个 controller 分别新增固定模板下载接口。
- 已实现前端：三个薪资导入页面新增“下载数据模版”按钮和下载 loading；三个 API helper 使用 `responseType=blob`；个税累计导入未选月份时提示并中止。
- GREEN：`mvn -Dtest=HrmPersonalIncomeTaxControllerTest,HrmAdditionalTemplateControllerTest test` 通过 3 个测试；`node tests/salary-tax-additional-template-api.test.mjs`、`node tests/salary-tax-additional-template-ui.test.mjs`、`node tests/addition-month-filter.test.mjs` 通过。
- 代码审查跟进：
  - 前端不再把个税累计/附加累计上传入口静默禁用；改为点击导入时检查年月，未选则阻止文件选择并即时提示；
  - 后端个税累计/附加累计导入 controller 增加空年月兜底，直接调用 API 传空字符串时不再进入 service。
- 审查跟进 RED：后端空年月测试先失败于返回成功且提示为空；前端 UI 测试先失败于缺少点击 guard 且上传入口静默 disabled。
- 审查跟进 GREEN：`mvn -Dtest=HrmPersonalIncomeTaxControllerTest,HrmAdditionalTemplateControllerTest test` 通过 5 个测试；`node tests/salary-tax-additional-template-ui.test.mjs`、`node tests/salary-tax-additional-template-api.test.mjs`、`node tests/addition-month-filter.test.mjs` 通过。
- 前端构建：`npm run build` 通过；构建输出仍有既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端本轮目标文件 `git diff --check -- ...` 无输出；前端本轮目标文件 `git -C /Users/jiangyongming/Project/hr/hr_web diff --check -- ...` 无输出。

# Progress: 李凤皇 2026-06 出勤统计少 16 小时排查修复

## 2026-07-23
- 用户反馈：李凤皇 6 月应出勤、实际出勤、应计出勤统计结果为 `184 / 180 / 184`，正确应为 `200 / 196 / 200`。
- 已启用项目文档优先、文件化计划、系统性调试和 TDD 流程。
- 已读取项目需求/开发文档，确认相关口径：行政体系按单双休日历应出勤，生产体系按生产月休配置；薪资和考勤汇总依赖加班/夜班统计落库结果。
- 已记录本轮专项计划；下一步定位加班/夜班统计字段计算与查询路径，并查询李凤皇 2026-06 源数据。
- 已定位代码风险：统计服务当前按 `affiliationSystem` 选择应出勤公式，查询又只在历史应出勤为空/0 时才重算，正数旧值 `23` 会被保留。
- 已只读查询 `hr_0003`：李凤皇当前员工档案 `affiliation_system=1`、`rest_type=2`；2026-06 统计明细 25 行均为 `expected_attendance_days=23`、`actual_attendance_days=22`、`accrued_attendance_hours=184.00`，最新更新时间 `2026-07-23 01:27:08`。
- 当前根因假设：固定月休员工的应出勤应由 `rest_type=2` 驱动，不能只看所属体系；所属体系仍保留用于加班/夜班资格判断。
- RED：新增 `HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldUseFixedMonthlyRestAttendanceWhenRestTypeIsFixedEvenIfAffiliationIsAdministrative`，旧实现失败于 `expected:<25> but was:<23>`。
- 已实现修复：统计应出勤班制优先读取 `restType`；`restType=2` 固定月休员工按固定月休公式，`restType=1` 按单双休月历，`restType` 为空才按所属体系兼容。
- 查询层收窄刷新历史正数应出勤：仅在显式 `restType` 与所属体系旧分流冲突时重新按当前公式解析，避免无完整节假日上下文时误覆盖普通历史明细。
- 只读审批核验：李凤皇 2026-06 有一条参与扣减的 `请假/调休`，业务时间 `2026-06-17 14:00-18:00`，折算 `4` 小时；补卡审批不扣实际出勤。
- GREEN：新增单测通过；`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过 56 个测试；`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest test` 通过 133 个测试。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，明确 `restType` 决定应出勤班制、`affiliationSystem + restType` 决定加班/夜班资格。

# Progress: 加班夜班开始统计范围选择前端合并

## 2026-07-23
- 用户要求：加班/夜班统计删除独立“单人统计”按钮，点击“开始统计”加载与薪资“开始核算”一致的穿梭框/部门树选择，开始统计业务逻辑不变。
- 本轮实际代码改动位于前端 `hr_web`，后端 `hainan` 仅更新需求/开发文档记录入口合并后的接口调用关系。
- 已记录：空范围继续走 `/startStatistics`，显式员工范围继续走 `/startStatisticsForEmployee`，后端统计口径不变。
- 前端验证已通过：加班/夜班统计范围测试、薪资共享弹窗回归测试、加班/夜班 API/页面测试和 `npm run build` 均通过；构建保留既有 `::v-deep` 过时警告和 chunk size warning。
- 后端文档/计划文件 `git diff --check` 无输出。

# Progress: 基本工资设置保存后同步薪资档案基本工资

## 2026-07-22
- 用户要求：基本工资金额设置保存之后，将基本工资同步至每个员工薪资档案中的基本工资。
- 已启用项目文档优先、文件化计划、brainstorming 和 TDD 流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 与现有计划/发现/进度文件。
- 已记录本轮计划；下一步定位基本工资设置保存服务与薪资档案 `10101 / 基本工资` 的持久化入口。
- 已定位代码：`saveSalaryBasic(...)` 当前只保存 `hrm_salary_basic`；薪资档案明细为 `hrm_salary_archives_option`，`is_pro=1` 试用期、`is_pro=0` 正式。
- RED：`mvn -Dtest=HrmSalaryBasicServiceTest test` 失败 2 个断言，确认保存设置未同步员工薪资档案基本工资。
- 已实现：保存基本工资设置后按未删除员工 ID 批量更新已有薪资档案明细 `code=10101`、`is_pro in (0,1)` 的 `value`。
- GREEN：`mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 9 个测试。
- 薪资回归：`mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest test` 通过 75 个测试。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录同步范围、试用期/正式两套档案和“不自动补建完整档案”的边界。
- 代码审查反馈处理：
  - RED：`mvn -Dtest=HrmSalaryBasicServiceTest test` 失败 2 个断言，确认旧实现仍会在 `salaryBasic=null` 时调用同步，且同步仍全量查询员工 ID 后拼 `IN`。
  - 已改为数据库 `exists` 子查询按 `hrm_employee.is_del=0` 过滤同步范围，不再全量加载员工 ID；空 `salaryBasic` 直接跳过同步。
  - GREEN：`mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 10 个测试。

# Progress: 员工级全勤金额设置

## 2026-07-22
- 用户要求：在基本工资金额设置的普通员工全勤金额和领导全勤金额后分别新增设置按钮；点击后弹出按人员/按部门的左右选择范围，把对应全勤金额保存到员工表；右侧无选择时更新全体员工；薪资核算删除全勤奖 `100/500` 硬编码，改读员工表对应金额；如有 SQL 允许执行。
- 已启用项目文档优先、文件化计划、brainstorming、TDD、UI/UX 和完成前验证流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md`，确认现有基本工资金额设置和薪资核算全勤规则。
- 已读取前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认基本工资金额设置页与现有薪资范围选择弹窗风格。
- 已在后端计划和发现文件中记录本次任务；下一步定位代码与测试点。
- 已完成代码定位：`HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 当前从基本工资设置计算 `fullMoney`，`SalaryMonthRecordServiceNew` 市场默认全勤分支仍直接读基本工资默认金额，基本工资服务/控制器没有员工级批量设置接口，前端设置页没有按钮/弹窗。
- RED 后端：`mvn -Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryMonthRecordServiceNewTest test` 失败 6 个断言，分别对应员工表金额字段、迁移脚本、批量设置接口、计薪员工 SQL 和薪资服务兜底分支。
- RED 前端：`node tests/salary-basic-settings-page.test.mjs` 失败于普通员工全勤金额缺少员工级“设置”按钮。
- 已实现后端：员工 PO/JPA 模型新增 `ordinaryFullAttendanceAmount/leaderFullAttendanceAmount`；基本工资服务和控制器新增员工级全勤金额批量设置接口；计薪员工 SQL 改为员工表金额优先、基本工资设置兜底；薪资服务全勤奖分支统一使用 `fullMoney`。
- 已实现前端：基本工资金额设置页两个全勤金额输入后新增“设置”按钮；新增 `FullAttendanceAmountDialog.vue`，支持按人员穿梭框和按部门树形范围选择；新增前端 API helper。
- GREEN：后端同一 Maven 定向测试通过 77 个测试；前端 `node tests/salary-basic-settings-page.test.mjs` 通过。
- 已新增并执行 SQL `docs/sql/2026-07-22_hrm_employee_full_attendance_amount.sql`，开发库 `hr_0001` 至 `hr_0005` 均确认存在 `ordinary_full_attendance_amount` 和 `leader_full_attendance_amount` 两列。
- 过程错误：尝试用 shell 命令替换 SQL 脚本中的 `AFTER rest_type` 时被反引号解析拦截；已用补丁改为 `AFTER full_attendance`，降低脚本对休息制度字段脚本的执行顺序依赖。
- SQL 幂等复跑成功，字段已存在时输出 `SELECT 1` 结果并跳过 DDL。
- 最终后端回归：`mvn -Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryMonthRecordServiceNewTest,HrmEmployeeServiceImplCompanyAgeTest,HrmSalaryMonthEmpRecordMapperXmlTest test` 通过 83 个测试。
- 最终前端验证：
  - `node tests/salary-basic-settings-page.test.mjs` 通过；
  - `node tests/salary-compute-scope-utils.test.mjs` 通过 9 个用例；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端和前端目标文件 `git diff --check` 均无输出。
- 代码审查跟进：
  - 修正 `FullAttendanceAmountDialog.vue` 员工来源，从计薪员工接口改为 `/hrmEmployee/queryAllEmployeeList`，避免未进入最新计薪范围的未删除员工无法被选择；
  - `HrmEmployeeServiceImpl#queryAllEmployeeList/transferSimpleEmp` 补充返回手机号，方便同名员工识别；
  - `HrmSalaryBasicService#updateEmployeeFullAttendanceAmount` 对显式员工范围更新 `0` 行改为业务失败，前端也检查 `updatedCount` 避免误报“设置成功”；
  - `SalaryMonthRecordService_Bak` 改为兼容解析 `fullMoney`，不再强转 `Long`。
- 审查跟进 RED：`node tests/salary-basic-settings-page.test.mjs` 失败于弹窗仍用计薪员工接口；`mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest,HrmEmployeeServiceImplTransferSimpleEmpTest test` 失败 3 个断言，分别覆盖 0 行更新、旧服务 `Long` 强转和员工简表缺手机号。
- 审查跟进 GREEN：同一前端测试通过；同一 Maven 命令通过 74 个测试。
- 最终复核：
  - `mvn -Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryMonthRecordServiceNewTest,HrmEmployeeServiceImplCompanyAgeTest,HrmEmployeeServiceImplTransferSimpleEmpTest test` 通过 87 个测试；
  - 后端源码扫描 `HrmSalaryBasicDefaults.*FullAttendanceAmount(batchData.salaryBasic)` 和 `(Long)map.get("fullMoney")` 无活动代码命中；
  - MySQL 只读复核 `hr_0001` 至 `hr_0005.hrm_employee`，每个库均存在 `ordinary_full_attendance_amount` 与 `leader_full_attendance_amount` 两列；
  - 后端目标文件 `git diff --check` 无输出。

# Progress: 生产体系固定月休4天才统计加班夜班

## 2026-07-22
- 用户要求修改逻辑：员工必须同时满足“所属体系=生产”和“休息制度=固定月休4天”，才统计加班时间、加班费、夜班次数、夜班补贴等。
- 已启用项目文档优先、文件化计划、brainstorming 和 TDD 流程。
- 已读取 `docs/requirements.md`、`docs/development.md`：当前 `hainan` 尚未接入 `rest_type/restType`，旧规则与新要求存在冲突，需新增字段接入并替换旧的生产体系单条件。
- 已在 `task_plan.md` 顶部新增本次任务计划。
- 已补 RED 测试：
  - Mapper SQL 必须返回/筛选 `rest_type=2`；
  - 薪资服务必须新增 `isFixedRestProductionEmployee(...)`，并对非目标员工归零 `180101/180102`；
  - 加班/夜班统计页旧明细对非固定月休生产员工展示为 0；
  - 考勤汇总同步对非固定月休生产员工写入 0 加班/夜班值。
- RED 运行：`mvn -Dtest=HrmEmployeeMapperSqlTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryMonthRecordServiceNewTest,HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest test` 在测试编译阶段失败，原因符合预期：缺少 `HrmEmployee.setRestType`、`QuerySalaryPageListVO.setRestType` 和 `SalaryMonthRecordServiceNew.isFixedRestProductionEmployee(...)`。
- 已实现后端规则收口：
  - 员工模型/PO/新增 BO/薪资 VO 接入 `restType`；
  - 员工列表、计薪员工、薪资列表、有加班费员工筛选返回或筛选 `rest_type`；
  - 薪资核算、薪资导出、旧薪资服务、加班/夜班统计、考勤汇总同步/导入/编辑/查询均改为同时要求 `affiliation_system=2` 且 `rest_type=2`。
- 已新增 SQL：`docs/sql/2026-07-22_hrm_employee_rest_type.sql`，为 `hr_0001` 至 `hr_0005.hrm_employee` 增加 `rest_type` 字段，空值按无加班/夜班资格处理。
- 冲突复核时发现 `SalaryMonthRecordServiceNew#getYeBanAndJiaBan(...)` 的夜班补贴兜底分支会把 eligible 员工 `nightShift * subsidy` 计算结果覆盖为 `0`。
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldCalculateNightSubsidyFromNightShiftWhenUploadedSubsidyMissing test` 失败于 `expected 70 but was 0`。
- 已修复夜班补贴分支：优先读取已落库 `nightSubsidy`，缺失时按 `nightShift * subsidy` 计算，最后才置 `0`。
- GREEN：同一新增测试通过，1 个测试 0 失败。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，明确“所属体系”和“休息制度”共同决定加班/夜班资格，并记录员工档案前端字段配置/花名册导入仍需另行补齐的缺口。
- 最终验证：
  - `mvn -DskipTests compile` 通过，主源码 1044 个文件编译成功；
  - `mvn -Dtest=HrmEmployeeMapperSqlTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmProduceAttendanceMapperXmlTest,HrmEmployeeServiceImplCompanyAgeTest,HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest,SalaryMonthRecordServiceNewTest test` 通过 144 个测试；
  - 本轮相关代码、Mapper、测试和文档 `git diff --check` 无输出；
  - 关键字扫描剩余命中均为新规则实现、测试断言、SQL 脚本、文档说明或旧注释代码，无新的“只看生产体系即可统计加班/夜班”的活动业务路径。
- Maven 仍输出既有 POM warning：`jsch/easyexcel` 重复依赖声明和两个 `systemPath` 警告，本轮未处理。

# Progress: 单双休与生产月休四天代码排查

## 2026-07-21
- 用户要求找出所有涉及员工是“单双休”还是“月休四天”的代码，本轮只读排查，不改业务逻辑。
- 已读取后端和前端项目文档，确认规则：员工所属体系来自 `affiliation_system`；行政体系走单双休日历；生产体系走 `productionMonthlyRestDays`，默认 `4`。
- 已完成后端扫描和行号核准：
  - 员工体系来源、薪资 Mapper、员工 Mapper；
  - `modules/workweek` 单双休配置服务、实体、BO/VO、Repository；
  - `HrmOvertimeNightStatisticsServiceImpl` 行政/生产应出勤分流、生产月休和法休扣减、统计落库；
  - `SalaryMonthRecordServiceNew` 读取统计明细、薪资核算/导出消费、缺失提示；
  - 考勤汇总 `department=1/2` 同步与筛选；
  - SQL 脚本和后端测试。
- 已完成前端扫描和行号核准：
  - 路由、单双休 API、单双休页面与工具；
  - 基本工资金额设置 `productionMonthlyRestDays`；
  - 加班/夜班统计 API、页面和矩阵工具；
  - 考勤汇总行政/生产筛选；
  - 前端测试。
- 已将完整排查结果写入 `findings.md`，并把本轮 `task_plan.md` 标记完成。

# Progress: 应出勤天数按所属体系公式获取

## 2026-07-21
- 用户补充：行政体系属于单双休，应出勤天数必须按传入年-月读取“单双休设置”当月出勤时间汇总；生产体系仍按基本工资设置里的生产月休天数计算，所有出勤天数获取/计算不得硬编码。
- 已复核文档和当前实现：加班/夜班统计主体已按员工 `affiliationSystem` 分流，行政读取 `queryMonthCalendar(year, month).workDays`，生产读取 `productionMonthlyRestDays` 并扣工作日法休。
- RED：`HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldReadAdministrativeExpectedAttendanceFromRequestedMonthWorkweekSummary` 覆盖行政体系必须按请求的 `2026-05` 调用单双休月历并读取 `workDays`，现有实现已通过。
- RED：`SalaryMonthRecordServiceNewTest#resolveSalaryNormalDaysForCompute_shouldNotFallbackToExistingNeedWorkDayWhenStatisticsMissing` 初次失败于返回历史 `needWorkDay=21.75`。
- RED：`SalaryMonthRecordServiceNewTest#resolveSalaryNormalDaysForCompute_shouldUseOvertimeNightExpectedDaysInsteadOfAttendanceMapValue` 初次失败于优先取旧考勤 map `21.75`，而不是加班/夜班统计 `23.00`。
- 已实现最小修正：薪资计算用到应出勤天数时只从 `expectedAttendanceDaysByEmployee` 获取；计算上下文在未勾选同步考勤时也会按员工集合读取当月加班/夜班统计明细，避免回退旧工资项或历史月薪记录。
- GREEN：上述 3 个目标测试通过。
- 定向验证：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 57 个测试。
- 定向验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmSalaryBasicServiceTest test` 通过 58 个测试。
- 组合回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,AttendanceInfoSourceTest test` 通过 122 个测试。
- 源码扫描 `21.75/DEFAULT_NORMAL_DAYS/daysInMonth - 4/lengthOfMonth() - 4/旧 needWorkDay 兜底 helper` 无命中；本轮相关文件 `git diff --check` 无输出。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，记录行政体系按传入年月读取单双休月历 `workDays`，薪资应出勤只消费加班/夜班统计落库 `expected_attendance_days`。

# Progress: 清理薪资考勤 deptType 体系硬编码

## 2026-07-21
- 用户要求：整个项目中用于硬编码部门属于哪个体系的 `deptType` 相关代码全部删除，统一从员工表所属体系获取；核算/导出薪资时行政体系员工没有加班费。
- 已按文档优先、文件化计划、TDD 流程启动。
- 已确认边界：组织管理 `deptType=1公司/2部门` 不属于员工行政/生产体系，本轮不删除；薪资/考勤里的历史 `deptType=0/1` 才是清理对象。
- 下一步补 RED 测试，先固定薪资服务不再使用 `normalDaysByDeptType/queryNormalDaysByDeptType`，以及行政员工即使有旧兼容字段也不能产生 `180101` 加班费。
- RED：薪资测试初次失败于生产代码还没有 `isProductionAffiliationSystem(...)`，且应出勤解析方法仍带旧配置参数。
- 已实现薪资清理：
  - `SalaryMonthRecordServiceNew` 删除 `HrmAttendanceInfoMapper/HrmAttendanceInfo` 依赖；
  - 删除 `load/query/resolveNormalDaysByDeptType` 与批量上下文 `normalDaysByDeptType`；
  - 薪资生产体系判断改为 `isProductionAffiliationSystem(...)` 直接读取 `affiliationSystem`；
  - 半路转正和主计算不再传体系类型参数；
  - 行政体系员工加班费 `180101` 保持写 `0`。
- 已同步 `SalaryComputeContext` 和测试，删除旧应出勤配置 map。
- RED：新增 `AttendanceInfoSourceTest` 初次失败于考勤天数配置 BO 仍暴露行政/生产体系类型字段。
- 已清理考勤天数配置模块：`QueryAttendanceInfoBO`、`HrmAttendanceInfo`、`QueryAttendanceInfoVO` 删除体系字段；`HrmAttendanceInfoMapper#queryInfo` 和 XML 中 `dept_type` SQL 删除。
- 已补导出二次防护：`buildSalaryDataRow(...)` 根据 `affiliationSystem` 判断生产体系，行政或未知体系员工导出 `加班工资` 时强制为 `0`，避免历史非零 `180101` 被继续导出。
- 验证：
  - `mvn -Dtest=SalaryMonthRecordServiceNewTest#isProductionAffiliationSystem_shouldUseEmployeeAffiliationAndIgnoreLegacyEmployeeOverrides+salaryMonthRecordServiceSource_shouldNotUseAttendanceInfoOrInternalSystemType+getYeBanAndJiaBan_shouldNotCreateOvertimePayForAdministrativeEmployee test` 通过 3 个测试；
  - `mvn -Dtest=SalaryMonthRecordServiceNewTest#buildSalaryDataRow_shouldZeroOvertimePayForAdministrativeEmployee test` 先失败于导出历史非零加班费，修复后通过 1 个测试；
  - `mvn -Dtest=SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test` 通过 57 个测试；
  - `mvn -Dtest=AttendanceInfoSourceTest test` 通过 1 个测试；
  - `mvn -Dtest=AttendanceInfoSourceTest,SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过 78 个测试。
- 扫描确认：后端剩余 `deptType/dept_type` 仅在组织管理公司/部门类型；前端剩余 `deptType` 也仅在组织管理页。

# Progress: 员工所属体系使用点全系统排查

## 2026-07-21
- 用户要求：将整个系统中所有使用员工所属体系的地方找出来并列出，因业务逻辑有改动。
- 已按项目文档优先和文件化计划流程处理。
- 已读取后端 `docs/requirements.md` 与 `docs/development.md`，确认当前所属体系规则以员工档案 `affiliation_system` 为准，行政/生产体系映射需统一，未维护时按行政兜底。
- 下一步进行全库文本检索和调用链归类，本轮先只读排查，不改业务代码。
- 已完成第一轮精确检索：命中员工实体、薪资服务、薪资 Mapper、员工 Mapper、考勤配置 Mapper、薪资 VO、相关测试和文档记录。
- 已完成补充检索：
  - `getAffiliationSystem` 命中考勤汇总同步和加班/夜班统计服务；
  - 前端只命中考勤汇总 `department=1/2` 筛选展示，以及基本工资设置中的 `productionMonthlyRestDays`；
  - SQL 只命中生产体系月休天数字段新增脚本，未发现员工 `affiliation_system` 的新增/迁移脚本。
- 已将后端、前端、SQL、测试使用点按业务链路写入 `findings.md`。
- 已更新后端 `docs/requirements.md`、`docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，记录本次影响面排查结论。
- 已完成空白检查：
  - 后端 `git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出；
  - 前端 `git -C /Users/jiangyongming/Project/hr/hr_web diff --check -- docs/requirements.md docs/development.md` 无输出。

# Progress: 张雪梅两天病假扣全勤

## 2026-07-21
- 用户反馈：张雪梅有两天病假钉钉申请；病假不扣工资，但扣全勤。
- 已启用项目文档优先、文件化计划、系统性调试和 TDD 流程。
- 已确认现有文档中同时存在病假 2 天内不扣工资规则，以及近期全勤兜底发放规则；下一步定位这两条规则在 `SalaryMonthRecordServiceNew` 中的交互。
- 已用只读 SQL 核验张雪梅 `hr_0003 / 2026-06` 数据：两条病假审批各 8 小时，薪资应计出勤达到应出勤，当前工资项 `40102=100.00`、`19010401=0`。
- 已定位根因：`shouldFallbackFullAttendanceByAccruedDays(...)` 只比较应计出勤和应出勤，且在病假天数计算前执行，导致病假被应计出勤补回后仍发全勤奖。
- RED：新增 `shouldFallbackFullAttendanceByAccruedDays_shouldRejectEffectiveSickLeaveEvenWhenAccruedReachesExpected`，首次运行失败于 helper 缺少有效病假天数入参。
- 已实现最小修复：提前计算 `leaveOfsickDays`，并把它传入全勤兜底 helper；有效病假天数大于 0 时不允许兜底发放 `40102`。
- GREEN：新增测试通过；`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 52 个测试。
- 已更新 `docs/requirements.md`、`docs/development.md`，明确病假工资免扣不等于全勤免扣，并将张雪梅从补发全勤名单中剔除。
- 已完成薪资组合回归：`SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest` 共 74 个测试通过。
- 已完成格式检查：本轮相关代码、测试、文档、计划文件 `git diff --check` 无输出。

# Progress: 开始核算界面计薪员工规则说明

## 2026-07-21
- 用户要求：在“开始核算”界面添加一行，说明根据什么规则获取当月计薪员工。
- 已复核前后端文档、后端 `queryPaySalaryEmployeeList` SQL、`queryHasSalaryArchivesEmployeeList(...)` 和前端共享弹窗 `AloneComputeDialog.vue`。
- 已补 RED 测试：
  - `node tests/salary-start-compute-dialog.test.mjs` 失败于开始核算弹窗缺少规则说明；
  - `node tests/salary-export-scope-dialog.test.mjs` 失败于共享弹窗缺少 `ruleTip` 支持。
- 已实现：
  - `AloneComputeDialog.vue` 新增可选 `ruleTip` 并在核算方式下方展示；
  - `SalaryManage.vue` 只给“开始核算”实例传入计薪员工规则说明，导出薪资实例不传。
- 已验证：
  - `node tests/salary-start-compute-dialog.test.mjs` 通过；
  - `node tests/salary-export-scope-dialog.test.mjs` 通过；
  - `node tests/salary-compute-scope-utils.test.mjs` 通过 9 个用例；
  - `node tests/compute-progress-utils.test.mjs` 通过 4 个用例；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 已更新后端项目 `docs/requirements.md`、`docs/development.md` 和前端项目 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。

# Progress: 薪资核算穿梭框员工来源统一

## 2026-07-21
- 用户确认需求：薪资核算穿梭框里只加载“获取核算薪资”的员工；按人员和按部门都是这个逻辑。
- 已启用项目文档优先、文件化计划、TDD 和完成前验证流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 相关条目，并确认当前工作区存在大量未提交改动，本轮不回退无关文件。
- 已确认目标接口为 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList`，前端核算弹窗已有调用点，下一步补前端 RED 测试固定部门模式的员工来源。
- 已读取前端项目 `hr_web/docs/requirements.md` 与 `hr_web/docs/development.md`，确认本轮前端改动边界为 `AloneComputeDialog.vue`、`salary-compute-scope-utils.js` 及相关测试。
- RED：`node tests/salary-compute-scope-utils.test.mjs` 失败于 `salary-compute-scope-utils.js` 未导出 `filterSalaryComputeDeptTreeByEmployees`；`node tests/salary-start-compute-dialog.test.mjs` 失败于弹窗未使用该过滤函数，符合预期。
- GREEN：新增 `filterSalaryComputeDeptTreeByEmployees(...)`，并让 `AloneComputeDialog.vue` 的部门树使用可核算员工集合过滤后的结果；`node tests/salary-compute-scope-utils.test.mjs` 与 `node tests/salary-start-compute-dialog.test.mjs` 已通过。
- 已更新后端项目 `docs/requirements.md`、`docs/development.md` 和前端项目 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，记录按人员/按部门都以 `queryComputeSalaryEmployeeList` 为唯一员工来源。
- 最终验证：
  - `node tests/salary-compute-scope-utils.test.mjs` 通过 9 个用例；
  - `node tests/salary-start-compute-dialog.test.mjs` 通过；
  - `node tests/salary-export-scope-dialog.test.mjs` 通过；
  - `node tests/compute-progress-utils.test.mjs` 通过 4 个用例；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - 后端项目相关文件 `git diff --check` 无输出，前端目标文件空白检查无输出。

# Progress: SalaryMonthRecordServiceNew 编译缺失排查

## 2026-07-20
- 用户反馈 `SalaryMonthRecordServiceNew` 类中很多包提示找不到，还有找不到符号。
- 已启用项目文档优先、文件化计划和系统性排查流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 和现有 `task_plan.md/findings.md/progress.md`。
- 已确认当前工作区存在大量未提交改动，本轮只处理编译缺失直接相关问题。
- 已运行 `mvn -DskipTests compile`，主源码 1044 个 Java 文件编译成功。
- 已运行 `mvn -Dtest=SalaryMonthRecordServiceNewTest test`，43 个测试通过。
- 已确认 `SalaryMonthRecordServiceNew` 近期引用的 `HrmOvertimeNightStatisticsDetail`、`hrmOvertimeNightStatisticsDetailRepository`、`HrmSalaryBasicDefaults`、`SalaryComputeProgressVO` 等源码均存在。
- 已定位更可能的根因：全局 Maven `settings.xml` 存在两处错误，可能导致 IDE Maven 同步/依赖索引异常。
- 已修正 `/Users/jiangyongming/devTools/apache-maven-3.8.4/conf/settings.xml`：`<profiles>` 改为 `<properties>`，`nexus-aliyun>` 改为 `nexus-aliyun`。
- 修正后已重跑 `mvn -DskipTests compile`，settings 解析警告消失，编译成功。
- 修正后已重跑 `mvn -Dtest=SalaryMonthRecordServiceNewTest test`，43 个测试通过。

# Progress: 薪资导出满勤天数按加班夜班统计

## 2026-07-20
- 用户纠正上次理解：薪资导出的满勤天数逻辑不是简单取薪资月记录 `actualWorkDay`，而是应从加班/夜班统计“开始统计”已落库的应出勤时间与应计出勤时间查找并计算。
- 已重新读取项目需求/开发文档中薪资导出、加班/夜班统计应出勤和应计出勤相关段落。
- 文档确认：加班/夜班统计应出勤时间展示为 `expectedAttendanceDays * 8`，应计出勤时间来自 `accruedAttendanceHours`，应计出勤小时落库到 `hrm_overtime_night_statistics_detail.accrued_attendance_hours`。
- 已定位实体、Repository 和开始统计保存链路。
- 已复核后端项目文档和现有工作记录，确认薪资应计出勤口径已落在 `HrmSalaryMonthEmpRecord.actualWorkDay` / `hrm_salary_month_emp_record.actual_work_day`，字段名保留兼容。
- 已定位导出链路：`SalaryMonthRecordServiceNew#exportSalaryNew(...)` 给 `9007 / 满勤天数` 写入 `vo.getNeedWorkDay()`，`buildSalaryDataRow(...)` 最终导出行也写入 `vo.getNeedWorkDay()`。
- 用户再次明确两列口径：满勤天数列读取应计出勤时间；超缺勤天数列为应出勤时间减应计出勤时间。
- 已补 RED 测试 `buildExportAbsenceDaysByEmployee_shouldCalculateExpectedMinusAccruedFromOvertimeNightStatistics`，首次运行编译失败于 helper 未实现，符合预期。
- 已实现最小修复：导出前批量读取 `hrm_overtime_night_statistics_detail`，按 `accrued_attendance_hours / 8` 计算 `9007 / 满勤天数`，按 `(expected_attendance_days * 8 - accrued_attendance_hours) / 8` 计算 `9008 / 超缺勤天数`；最终导出行从预置 `9007/9008` 取值；缺统计明细时抛业务错误提示先执行“开始统计”。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，记录薪资导出两列均取加班/夜班统计落库结果的业务规则和实现位置。
- 已运行 `mvn -Dtest=SalaryMonthRecordServiceNewTest test`，43 个测试通过。
- 已运行薪资组合回归 `mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test`，65 个测试通过。
- 已运行相关文件 `git diff --check`，无输出。

# Progress: 薪资管理导出薪资空白排查

## 2026-07-20
- 已启用项目文档优先、文件化计划和系统性排查流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 以及现有 `task_plan.md/findings.md/progress.md`。
- 已确认本轮先从后端薪资导出接口、Excel 模板和写出逻辑排查；若发现前端传参/下载处理问题，再扩展到 `hr_web` 前端项目并先读取其项目文档。
- 已定位后端导出入口 `/hrmSalaryMonthRecord/exportSalary` 和新版服务方法 `SalaryMonthRecordServiceNew#exportSalaryNew(...)`。
- 已发现 controller 会吞掉导出异常，存在“后端异常但前端下载到空响应”的风险；继续追查服务层是否因空薪资记录、空模板、空查询或写表字段错位导致异常/空数据。
- 已读取前端 `hr_web` 需求/开发文档并对比调用参数：`salaryRecordId` 传参正确，`jobNummber` 拼写字段后端未使用但不解释整表空白。
- 已检查两套薪资导出模板占位符，模板字段与 `queryExportData` 的 `deptname/performance/dutiessalary` 基本对齐。
- 旧方案曾补 `resolveExportNormalDays...` 正常天数兜底测试；当前已被“满勤天数/超缺勤天数均读取加班/夜班统计落库结果”的口径取代，相关 helper 和测试已删除。
- 当前导出循环不再为这两列逐员工查询 `hrm_attendance_info`，缺少考勤配置不会再导致这两列空指针。
- 已补 controller RED 测试：`HrmSalaryMonthRecordControllerTest#exportSalary_shouldPropagateServiceExceptionInsteadOfReturningEmptyResponse`，首次运行失败于 controller 吞异常。
- 已修改导出 controller 直接传播异常；再次运行 `mvn -Dtest=HrmSalaryMonthRecordControllerTest test` 通过。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，记录导出两列取加班/夜班统计落库结果、导出异常不得吞掉以及验证命令。
- 已运行薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，63 个测试。
- 已运行本轮 tracked 变更格式检查：`git diff --check -- docs/requirements.md docs/development.md src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryMonthRecordController.java src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java` 无输出。

# Progress: 农谷 2026-06 批量个税支持数据整理与导入

## 2026-07-20
- 已复核李明明成功文件、导入接口和原始表结构。
- 已确认本次批量不直接改薪资工资项，只导入 6 月薪资核算读取的支持数据：`2026-05` 个税累计与 `2026-06` 附加累计。
- 已解析并匹配：
  - `202607_税款计算_工资薪金所得.xls`；
  - `湖北田野农谷生物科技有限公司_综合所得申报_202606.xls`；
  - `副本加个税版-田野农谷2026年6月工资表(1)(1).xlsx` 的 `6月` sheet；
  - 系统当前 `hr_0003` 员工接口数据。
- 批量预览结果：
  - `202607` 原始税款计算表 180 条员工行；
  - 按身份证命中系统当前员工并在 6 月工资表中唯一定位 95 条；
  - 未导入 85 条，原因为系统当前员工未按身份证命中；
  - 6 月工资表个税非零员工 4 人；
  - 95 条公式预演预测个税均等于工资表个税。
- 已生成：
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税_个税累计_导入2026-05.xls`；
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税_附加扣除累计_导入2026-06.xls`；
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税支持数据预览_20260720.csv`；
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税支持数据预览_20260720.md`。
- 已上传数据库：
  - `/hrmAdditional/importAdditional` 返回 `code=200`；
  - `/hrmPersonalIncomeTax/importPersonalIncomeTax` 返回 `code=200`。
- 已核验：
  - `hr_0003.hrm_personal_income_tax` 的 `2026-05` 个税累计为 `97` 行；
  - `hr_0003.hrm_additional` 的 `2026-06` 附加累计为 `40` 行；
  - 李明明、范燕东、王洪平、宁明友四名 6 月工资表个税非零员工，按导入后累计基础复算均与工资表个税一致。
- 用户已修正杨晨雨身份证大小写后，按“员工表为基准，原始税表多出的员工不处理”重新生成并导入：
  - 新文件：`批量6月工资个税_员工表基准_个税累计_导入2026-05.xls`；
  - 新文件：`批量6月工资个税_员工表基准_附加扣除累计_导入2026-06.xls`；
  - 预览：`批量6月工资个税_员工表基准_预览_20260720.csv/.md`；
  - 当前员工表 `113` 人中成功生成 `96` 人，员工表内 `17` 人因缺身份证或原始税表无对应身份证未生成；
  - 原始税表多出 `84` 人已忽略；
  - 重新导入后数据库 `2026-05` 个税累计为 `96` 行，`2026-06` 附加累计为 `40` 行；
  - 杨晨雨已纳入 `2026-05` 个税累计，读回 `9543.06 / 15000.00 / 1179.60 / 0.00`。
- 用户要求这批未生成人员改用“姓名 + 手机号”后，已复核现有 2026 年源文件：
  - 只有朱玲丽 `18727608635` 可在 `个税累计.xls` 中按姓名 + 手机号找到 2026-05 个税累计源，已补入新文件；
  - 吴镜平、刘玉麒、娄艳霞虽有 6 月薪资记录，但只找到 2025 年历史累计，未找到 2026-05 个税累计源，未补；
  - 其他未生成员工未在本次 2026 年税表/工资表/累计源中找到姓名 + 手机号有效数据，或同名有证件在职档案已导入、另一手机号档案缺税表来源。
- 已生成并导入：
  - `批量6月工资个税_员工表基准含姓名手机号_个税累计_导入2026-05.xls`，数据行 `97`；
  - `批量6月工资个税_员工表基准含姓名手机号_附加扣除累计_导入2026-06.xls`，数据行 `40`；
  - `批量6月工资个税_员工表基准含姓名手机号_预览_20260720.csv/.md`。
- 接口返回均为 `code=200`；数据库核验 `2026-05` 个税累计为 `97` 行，朱玲丽读回 `10650.00 / 60000.00 / 3031.80 / 0.00`，`2026-06` 附加累计仍为 `40` 行。

# Progress: 农谷社保医保方案整理与员工参保方案设置

# Progress: 薪资导出社保多 3 元排查

## 2026-07-20
- 用户反馈：导出的薪资表中社保部分多了 `3` 元，且涉及所有员工。
- 已启用项目文档优先、文件化计划和系统性排查流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和现有 `task_plan.md/findings.md/progress.md`。
- 文档中当时确认长期护理保险固定金额默认 `3`，且 6 月工资表“社保扣款”已包含长期护理金额；方案项目不得重复写长期护理。该社保合计口径已由 2026-08-22 新规则覆盖，当前长期护理通过社保方案项目行和启用状态控制。
- 已追加本轮计划与初始发现，接下来定位薪资导出社保字段的代码取数链路和数据库金额闭合关系。
- 已定位导出代码：导出“个人社保”取工资项 `100101`，工资项同步社保时直接取 `hrm_insurance_month_emp_record.personal_insurance_amount`。
- 【历史实现，已由 2026-08-22 新规则覆盖】已定位社保生成代码：社保月报生成和员工参保项目手工修改都会把项目个人社保合计额外加长期护理金额 `3` 后写入 `personal_insurance_amount`。
- 错误记录：用 `rg` 查 `src/main/resources/*.yml` 时因项目无 yml 文件触发 zsh `no matches found`；已改查现有 `application-*.properties`，不影响后续只读 SQL。
- 错误记录：首次查询 `hrm_salary_month_record.status` 报字段不存在；已读取表结构并改用实际字段 `check_status`。
- 已用只读 SQL 对比 2026-06：84 名正常参保员工的 `hrm_insurance_month_emp_record.personal_insurance_amount - 参保项目个人社保合计` 全部为 `3.00`。
- 已用只读 SQL 对比 2026-06 薪资：83 名有社保扣款的薪资员工 `100101 - 参保项目个人社保合计` 全部为 `3.00`；另 12 名无社保记录员工工资项为 `0`。
- 错误记录：按姓名关联导出中间表与员工表时遇到 MySQL collation 不一致；后续改用显式 collation 或避免依赖姓名关联。
- 已确认 `hrm_salary_export` 当前 2026-06 中间表只有 36 行且无员工 ID，因同名员工存在，不作为精确根因证据；精确对账以薪资员工记录与社保员工记录为准。
- 已确认时序根因：2026-06 社保月记录在 2026-07-17 生成，早于 2026-07-18 的 6 月工资表参保方案重整，因此本月薪资读取的是旧社保月记录金额。
- 【历史口径，已由 2026-08-22 新规则覆盖】已确认旧方案示例：严锦 2026-06 社保月记录使用旧 `社保(466.50) 公积金(144)`，月度项目个人社保合计已是 `466.50`，记录金额为 `469.50`。
- 【历史口径，已由 2026-08-22 新规则覆盖】已确认新方案示例：当时主流新 `社保(466.50) 公积金(144.00)` 方案项目个人社保合计为 `463.50`，加长期护理 `3` 后为 `466.50`，适合后续重新生成社保月记录。
- 【历史口径，已由 2026-08-22 新规则覆盖】已按用户反馈复核黎冬霜：当时员工档案方案确为新 `社保(466.50) 公积金(144.00)`，但 2026-06 已生成社保月记录仍挂旧 `社保(466.50) 公积金(144)`，月记录和工资项 `100101` 均为 `469.50`。
- 已按当前员工档案方案重算目标额并对比全员范围：2026-06 有效社保月员工记录 `83` 条中 `79` 条个人社保不一致，公积金不一致 `0` 条；薪资非零 `100101` 的 `82` 条里 `79` 条不一致。
- 已更新 `docs/requirements.md` 与 `docs/development.md` 记录本轮根因、影响范围和处理建议。


## 2026-07-18
- 已启用项目文档优先和文件化计划流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md` 的相关条目，确认本次写库必须遵守社保方案固定金额合计口径、员工参保按 `employeeIds + schemeId` 更新、同名员工不得只按姓名匹配。
- 已建立本轮计划并完成数据整理；本轮未修改业务代码。
- 已解析 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年社保医保缴费明细（田野农谷分部门）.et` 和 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年公积金缴费清单 (田野农谷分部门).xlsx` 的 `2026-07` 普通月度表。
- 预览结果：源社保/医保 89 行、公积金 89 行；补手机号后源唯一键重复 0；按姓名+手机号命中在职员工 85 人，未命中 4 人；7 月唯一方案组合 5 个。
- 已生成材料：
  - 预览报告：`docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview.md`；
  - 执行 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import.sql`；
  - 回滚 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_rollback.sql`。
- 已执行 SQL 到 `hr_0003`：新增 4 个缺失参保方案，复用 1 个现有精准方案；员工社保信息新增 9 行、更新已有 76 行，合计设置 85 名员工。
- 写后核验：
  - 5 个 7 月方案系统展示金额与源表一致；
  - 用同一解析逻辑反查 85 个命中员工，实际 `scheme_id` 与预期一致，错配 0；
  - 4 个异常员工未自动更新：朱君明 `13972908908` 不在在职员工表；宁明友、陈爱蓉、瞿顺清源文件手机号与员工表手机号不一致。
- 用户后续要求暂时改按 `/Users/jiangyongming/Desktop/农谷导入数据/副本加个税版-田野农谷2026年6月工资表(1)(1).xlsx` 处理；已确认只使用该工作簿 `6月` sheet。
- 已解析 6 月工资表：
  - 工资表员工行 96 行，非零参保行 88 行；
  - 唯一金额组合为 `0+0`、`445.80+144`、`466.50+144`、`672.50+144`、`672.50+320`；
  - 宁明友、陈爱蓉、瞿顺清在员工表中均为姓名唯一，可以按姓名定位后仍以 `employee_id` 更新。
- 已生成 6 月工资表口径材料：
  - 预览与执行报告：`docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview-2026-06-salary.md`；
  - 执行 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary.sql`；
  - 回滚 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary_rollback.sql`。
- 已执行 6 月工资表口径 SQL 到 `hr_0003`：
  - 新增 3 个 6 月精确方案，复用 `2078034513505673217 / 社保(672.50) 公积金(320)`；
  - 更新 82 名员工参保方案，清空胡勇星 1 条当前方案；
  - MySQL 返回 `inserted_or_existing_scheme_count=3`、`touched_employee_count=83`。
- 6 月写后核验：
  - `466.50+144`、`445.80+144`、`672.50+144`、`672.50+320` 四个目标方案系统展示金额均与工资表一致；
  - 83 个触达员工按执行 SQL 反查 `scheme_id` 错配为 0；
  - 三名原异常员工结果：宁明友 `2078034513505673217`，陈爱蓉/瞿顺清 `2078479646634151940`。
- 6 月仍未自动更新 3 条：马国华、谢杰杰、程传祥，原因均为同名在职员工无法仅凭工资表唯一定位。

# Progress: 李明明六月工会费和个税未生成排查

## 2026-07-17
- 已启用项目文档优先、计划文件、系统性排查和 TDD 流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md`，确认本次问题落在薪资核算个税/工会费生成与个税导入匹配链路。
- 已建立本轮专项计划，当前处于数据和代码定位阶段。
- 初始约束：只测试 `2026-06` 薪资；目标员工优先按 `hr_0003 / employee_id=1831601326890434563 / 李明明` 处理；期望工会费 `29.25`、六月个税 `2.4`。
- 已复核数据库与用户 Excel：
  - 当前薪资项中 `160102 工会费` 同时存在 `0` 和 `29.25`，`1001 代扣小计` 同时存在 `0` 和 `1021.75`，说明真实值已算出但被重复默认 0 行遮挡。
  - 用户整理后的 `个税累计.xls`、`附加扣除累计.xls`、`专项扣除累加表_2026.xlsx` 均使用在职李明明手机号 `17389819211`。
  - 实库中李明明 `2026-05` 个税累计、`2026-05/06/07` 附加累计、`2026` 年度附加配置均挂在已删除同名员工 `2033769831940079620` 上。
  - 在职李明明当前 `2026-06` 个税累计被重新写为收入 `5850`、减除费用 `60000`、专项扣除 `992.50`、已缴税 `0`，导致本月个税为 `0`。
- 已完成工会费方向修复：
  - `SalaryMonthRecordServiceNew#filterNoFixedSalaryOptions(...)` 排除计算汇总项，避免非固定工资项初始化预插入默认 `0`。
  - RED/GREEN：`SalaryMonthRecordServiceNewTest#filterNoFixedSalaryOptions_shouldNotMutateInputAndExcludeCodes` 已通过。
- 已完成个税累计减除费用修复：
  - 新增 RED：`SalaryComputeServiceNewTest#resolveCumulativeDeductions_shouldContinueImportedPriorDeductionBeforeRemarkFallback`，先失败于旧代码返回 `60000`。
  - 实现后该测试通过；`SalaryComputeServiceNewTest` 全类 13 个测试通过。
  - 新口径：已有上月导入累计减除费用时按 `上月累计 + 5000` 续算并封顶 `60000`；`is_remark=2` 只在缺少可靠导入累计时兜底 `60000`。
- 已新增并执行数据修复脚本流程：
  - `docs/sql/2026-07-17_hr_0003_li_mingming_tax_data_repair.sql` 默认 `ROLLBACK`，用于迁回在职李明明的个税/附加基础数据并删除错误 `2026-06` 个税累计；
  - 前序执行记录显示改为 `COMMIT` 后已成功执行；本轮收尾三次直连复核被 MySQL `Too many connections` 阻断，未强制清理连接。
- 已补充用户文件扫描：
  - 未在 `/Users/jiangyongming/Desktop/农谷导入数据` 下的 Excel/CSV/说明文件中发现 `2.4/2.40/21894`；
  - 已确认最终导入基础为 `2026-05` 个税累计和附加累计，原始 `2026-06` 综合所得申报为另一套累计申报值。
- 最新验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，60 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=TaxImportEmployeeMatcherTest,TaxImportServiceDuplicateNameTest test` 通过，9 个测试。
- 用户继续反馈：李明明个税系统为 `14.22`，员工手工计算为 `2.40`。
- 已重新直连 `hr_0003` 复核当前库：
  - 李明明当前 6 月薪资项为 `应发工资=5850.00`、`个人社保=672.50`、`个人公积金=320`、`累计专项扣除=5337.00`、`累计专项附加扣除=21500.00`、`累计应纳税额=228.21`、`本月个税=14.22`；
  - 当前 `14.22 = (64444 - 30000 - 5337 - 21500) * 3% - 213.99`，公式完全闭合；
  - 若要得到 `2.40`，需要比当前多扣除 `394.00`，在专项附加不变时等价于本月个人社保+公积金从 `992.50` 提高到 `1386.50`。
- 已复核社保来源：
  - 薪资配置按当月社保取数；
  - 李明明 6 月社保员工明细为个人社保 `672.50`、个人公积金 `320.00`，员工明细状态完成，社保主记录状态未完成；
  - 社保项目明细显示基数 `6500/4000`，个人养老 `520`、医疗 `130`、失业 `19.50`、长期护理 `3`、公积金 `320`。
- 已复核文件来源：
  - 原始 2026-06 个税申报表李明明本月专项扣除为 `840 + 213 + 31.5 + 320 = 1404.50`，累计专项扣除 `5749.00`；
  - 但把该原始专项扣除套入当前系统收入后，本月个税为 `1.86`，仍不是 `2.40`；
  - 扫描 `/Users/jiangyongming/Desktop` 未发现 `2.40/5731.00/1386.50/21894.00/216.39` 等能直接支撑员工手算结果的文件值。
- 本轮结论：`14.22` 与 `2.40` 的差别不是个税计算公式问题，而是本月专项扣除输入不一致；系统用了当前 6 月社保数据 `672.50 + 320`，员工手算 `2.40` 需要另一套个人社保+公积金 `1386.50`，该数值目前未在现有系统数据或用户提供文件中找到来源。本轮只定位原因，未改业务代码和薪资/社保数据。
- 用户进一步指定 `202605`、`202606` 两张财务原始申报表为最详细数据后，已按表内字段重新计算：
  - `202605` 李明明严格复算为 `58594 - 25000 - 4344.50 - 21500 = 7749.50`，累计税额 `232.49`，本月税额 `232.49 - 213.99 = 18.50`；
  - `202606` 李明明严格复算为 `69859 - 30000 - 5749 - 25800 = 8310.00`，累计税额 `249.30`，本月税额 `249.30 - 232.49 = 16.81`；
  - 用两张表里的合理累计预扣字段组合枚举，精确等于 `2.40` 的组合数为 `0`；
  - 仅从这两张财务原始表不能得出李明明 6 月个税 `2.40`，表内严格结果是 `16.81`。
- 用户补充可能存在两个绩效工资 `4700` 与 `7000` 后，已分别按 `4700/7000/11700` 计算：
  - 当前系统口径下，本月个税分别为 `155.22 / 224.22 / 365.22`；
  - 财务 `202606` 原表累计收入基础上额外加入绩效时，本月个税分别为 `157.81 / 226.81 / 367.81`；
  - 三种绩效金额都会提高个税，不能解释 `2.40`。
- 用户新增 `202607_税款计算_工资薪金所得.xls` 后，已解析李明明行并复算：
  - 本期收入 `5850.00`，本期专项扣除 `520+133+19.5+320=992.50`；
  - 累计收入 `75709.00`，累计减除费用 `35000.00`，累计专项扣除 `6741.50`，累计专项附加扣除 `30100.00`；
  - 累计应纳税所得额 `3867.50`，累计应纳税额 `116.03`，已缴税额 `249.30`；
  - `116.03 - 249.30 = -133.27`，表内应补(退)税额为 `0.00`；
  - 因此按 202607 财务原始表，李明明 7 月个税应扣为 `0.00`。
- 用户最终确认不再追溯 `246.90/249.30` 原始来源，按财务确认值直接用于系统支持数据整理。
- 已生成两份全量导入副本，不覆盖原始 `final_import_excels/个税累计.xls` 和 `final_import_excels/附加扣除累计.xls`：
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/李明明6月个税2.40_个税累计_导入2026-05.xls`；
  - `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/李明明6月个税2.40_附加扣除累计_导入2026-06.xls`。
- 新文件保留同年月其他可匹配员工数据，只调整李明明一行；个税累计导入文件已过滤 3 条当前系统不允许导入的已删除员工 `郑景/杜畅/万怀进`：
  - 个税累计导入 `2026-05`：累计收入 `69859.00`、累计减除费用 `30000.00`、累计专项扣除 `5749.00`、累计已缴税额 `246.90`；
  - 附加累计导入 `2026-06`：子女教育 `12000.00`、住房租金 `4800.00`、住房贷款 `0.00`、赡养老人 `8857.50`、继续教育 `0.00`、养幼女 `0.00`，合计 `25657.50`。
- 已读回生成文件并按系统公式复核：累计收入 `75709.00`、累计减除费用 `35000.00`、累计专项扣除 `6741.50`、累计专项附加扣除 `25657.50`、累计应纳税所得额 `8310.00`、累计应纳税额 `249.30`、截至上月累计已缴税额 `246.90`，本月个税 `2.40`。
- 已实际导入本机 `9080 / hr_0003`：
  - 附加累计 `2026-06` 首次导入成功；
  - 个税累计 `2026-05` 首次被已删除员工行拦截，过滤后重试成功；
  - 单人重算李明明 `2026-06` 薪资，第一次未同步社保导致本月社保/公积金被置 `0` 且个税变为 `32.18`；随后用 `isSyncInsuranceData=true` 重新单人重算成功。
- 最终接口复核：
  - 工资项：应发 `5850.00`、个人社保 `672.50`、个人公积金 `320`、工会费 `29.25`、个税 `2.40`；
  - 当前累计：收入 `75709.00`、减除费用 `35000.00`、专项扣除 `6741.50`、专项附加扣除 `25657.50`、应纳税所得额 `8310.00`、累计应纳税 `249.30`；
  - 个税累计表：`2026-05=69859/30000/5749/246.90`，`2026-06=75709/35000/6741.50/249.30`。

# Progress: 基本工资社保固定金额配置

## 2026-07-17
- 已启用项目文档优先、计划文件和 TDD 流程。
- 已读取后端 `docs/requirements.md`、`docs/development.md`，确认本次规则是对既有“基本工资金额设置”和“社保报表生成”规则的新增。
- 已追加本轮任务计划，当前处于代码定位阶段。
- 已注意到工作区存在大量既有未提交改动；本轮只改与基本工资设置、社保合计、次月报表生成和必要前端展示相关的文件，不回退无关改动。
- 已完成后端 RED/GREEN：
  - `HrmSalaryBasicServiceTest` 覆盖新默认值、旧配置兜底、最新配置读取和保存；
  - `HrmInsuranceSchemeMapperXmlTest` 覆盖社保方案合计、员工月度参保项目合计和 SQL 迁移脚本；
  - 后端实现新增 `largeMedicalInsuranceAmount=15`、`longTermCareInsuranceAmount=3` 两项配置及 `hrm_salary_basic` 字段映射；
  - 【已废弃，2026-08-22 新规则覆盖】社保方案合计、次月报表生成、员工月详情手工改项目后回写合计均已加入长期护理/大额医疗固定金额。当前社保方案/月度项目合计不再读取基本工资金额设置中的这两个字段。
- 已读取前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，并完成前端 RED/GREEN：
  - `src/views/manage/salary/Index.vue` 新增“大额医疗保险金额”“长期护理保险金额”输入项；
  - 前端默认值为 `15/3`，校验和提交复用原基本工资设置表单。
- 已新增后端 SQL 脚本 `docs/sql/2026-07-17_hrm_salary_basic_insurance_amount_settings.sql`，为 `hr_0001` 至 `hr_0005` 幂等添加两个字段。
- 已执行该 SQL 到 dev MySQL，并复核 `hr_0001` 至 `hr_0005` 的 `hrm_salary_basic` 均存在 `large_medical_insurance_amount` 默认 `15.00`、`long_term_care_insurance_amount` 默认 `3.00`。
- 已更新后端和前端需求/开发文档。
- 验证：
  - 后端社保/基本工资回归通过，13 个测试；
  - 前端 `node tests/salary-basic-settings-page.test.mjs`、`node tests/insurance-create-next-month.test.mjs`、`node tests/insurance-progress-utils.test.mjs`、`node tests/insurance-advanced-filter.test.mjs` 均通过；
  - 前端 `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - 本轮相关文件 `git diff --check` 均无输出。

# Progress: 李明明薪资全勤奖未计入排查

## 2026-07-17
- 已启用系统性排查流程：先读文档和历史记录，不直接猜测或改数据。
- 已建立本轮专项计划。
- 已确认文档中存在李明明 `2026-06` 历史校准值：应出勤 `184` 小时、实际出勤 `160` 小时、应计出勤 `184` 小时。
- 已查询本机 `0003` 租户 API：
  - 计薪员工列表显示李明明 `fullMoney=500.00`、`isFullAttendance=1`、`status=1`、`becomeTime=2025-07-01`；
  - 薪资行 `actualWorkDay=23.00`、`needWorkDay=21.75`，工资项无 `40102`，应发工资 `5350.00`；
  - 考勤汇总 `actualAttendance=20.00`、`accruedAttendance=23.00`、`workOverTime=8.00`；
  - 加班/夜班统计为应出勤 `23` 天、实际 `160.00` 小时、应计 `184.00` 小时，备注 `扣除：年假32.00小时`；
  - 审批明细为年假 4 天、加班 8 小时、出差两段，未见事假/病假。
- 根因结论：全勤奖未加不是员工档案、金额配置或转正状态问题；是薪资全勤判断仍依赖 `hrm_attendance_report_data` 汇总的 `empAttendanceSummary`，而当前可见链路中该汇总缺失/未识别时，即使应计出勤已满，代码也不会补置 `isFullAttendance=true`，因此不生成 `40102`。
- 已按用户确认的口径修复：薪资应计出勤天数达到加班/夜班统计应出勤天数，且旧报表月度汇总缺失时，薪资核算可兜底判满勤并生成 `40102`；旧报表汇总存在时仍保留原异常扣全勤判断。
- 已补 RED/GREEN 测试：
  - 加班/夜班统计 `expectedAttendanceDays` 会进入薪资全勤期望天数 map；
  - 全勤期望天数优先取加班/夜班统计应出勤天数，缺失时才回退薪资原应出勤天数；
  - `empAttendanceSummary == null` 且应计出勤达到应出勤时允许兜底满勤。
- 验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 通过，38 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，57 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，9 个测试。
- 本轮未直接修改薪资数据；李明明 2026-06 既有薪资记录需要重新核算后才会显示补出的 `40102 全勤奖`。

# Progress: 薪资核算硬编码排查

## 2026-07-17
- 已读取项目需求文档和开发文档。
- 已确认本次任务只读排查，不修改薪资核算业务代码。
- 已在 `task_plan.md` 建立本轮排查计划，并在 `findings.md` 记录文档约束。
- 已完成第一轮粗检索，定位薪资核算主链路和硬编码候选类别。
- 已复核主计算链路、考勤填充入口和计薪员工 SQL，确认第一批公司分支、员工 ID 例外、部门/岗位关键字和工资项编码硬编码。
- 已继续复核请假/考勤扣款、全勤奖、导出和离职工资遗留方法，记录第二批硬编码候选与主链路关系。
- 已复核 mapper、上游考勤汇总同步、加班/夜班统计和遗留自动任务，完成硬编码分类汇总；本轮只读排查，未修改业务代码，未运行测试。
- 已按用户追问专项搜索 `getDeptTypeForEmployee` 同类“特定员工强制行政/生产体系”逻辑：主链路仅发现 `SalaryMonthRecordServiceNew` 中该函数及导出链路复刻；另记录生产双休员工 ID、旧 `getNormalDays` 员工 ID 和无考勤组直接全勤员工 ID 作为相关但不同类风险。

# Progress: 薪资核算应计出勤天数来源调整

## 2026-07-17
- 已读取项目需求文档和开发文档。
- 已确认本次需求与既有“考勤汇总同步加班/夜班统计”和“薪资核算失败集中提示”规则相邻。
- 已建立本轮计划：先定位薪资核算使用实际出勤天数的位置，再补失败测试，最后实现考勤汇总优先、加班/夜班统计兜底的应计出勤来源选择。
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 编译失败，缺少 `resolveSalaryAttendanceDays(...)` 与 `buildAccruedAttendanceDaysByEmployee(...)`。
- 已实现：
  - `SalaryMonthRecordServiceNew` 批量同步考勤数据时加载加班/夜班统计明细兜底；
  - `resolveSalaryAttendanceDays(...)` 统一应计出勤来源选择；
  - 编码 `2`、员工月薪记录 `actualWorkDay`、薪资列表/导出表头和校验提示统一改为“应计出勤天数”业务口径；
  - `HrmSalaryMonthEmpRecord` API 描述同步改为“实际计薪时长/应计出勤天数”。
- GREEN：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 通过，32 个测试。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md`。
- 回归验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，50 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，8 个测试。
- 格式检查：本轮相关文件 `git diff --check -- ...` 无输出。

# Progress: 考勤汇总同步出勤天数来源修正

## 2026-07-17
- 已读取项目 `docs/requirements.md`、`docs/development.md`，并确认本轮需求落在既有“考勤汇总同步加班/夜班统计”链路。
- 已记录本轮计划到 `task_plan.md`，记录初始发现到 `findings.md`。
- 已定位后端同步服务：`HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)`，既有实现用 `IHrmOvertimeNightStatisticsService#queryPageList(...)` 获取所选月份月度 VO 后 upsert `hrm_produce_attendance`。
- 已确认统计明细表模型 `HrmOvertimeNightStatisticsDetail` 落库字段包含 `actualAttendanceDays` 与 `accruedAttendanceHours`；实际出勤小时不是单独物理列。
- RED：新增 `HrmProduceAttendanceServiceImplTest#syncFromOvertimeNightStatistics_shouldUsePersistedStatisticsDetailsForAttendanceDays`，先失败于同步仍写入原 VO 的 `positiveAttendance=23.00/probationAttendance=25.00`，未优先使用落库明细中的 `26天/224小时`。
- 已实现：
  - 同步开始按所选年月查询 `hrm_overtime_night_statistics_detail` 并按员工聚合；
  - 实际出勤天数优先来自落库 `actualAttendanceDays`，应计出勤天数优先来自落库 `accruedAttendanceHours / 8`；
  - 加班、夜班、加班工资、夜班补贴、员工部门刷新和人工维护字段保留原同步逻辑；
  - 当原月度 VO 缺员工但明细表有员工时，使用聚合明细作为兜底同步行。
- GREEN：新增测试通过。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，8 个测试。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md`。
- 格式检查：本轮相关跟踪文件 `git diff --check -- ...` 无输出；`task_plan.md/findings.md/progress.md` 尾随空白扫描无输出。

# Progress: 考勤汇总同步漏员工排查

## 2026-07-17
- 已读取项目 `docs/requirements.md`、`docs/development.md`。
- 已确认本轮样例员工为“潘红琼”，先按系统性排查流程定位根因，不直接猜改。
- 已将本次排查计划追加到 `task_plan.md`，并在 `findings.md` 记录初始上下文。
- 已定位后端同步入口：`HrmProduceAttendanceController#syncFromOvertimeNightStatistics` 调用 `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)`。
- 已只读查询 `hr_0003`：
  - 潘红琼员工、钉钉考勤用户映射、2026-06 加班/夜班统计明细、2026-06 考勤汇总行均存在；
  - 当前库活跃员工、统计员工、考勤汇总员工均为 `113` 人，不存在统计有但汇总无的员工。
- 已形成根因假设：同步已有行不会纠正 `department`，而新增行的 `department` 只按部门名称推断；潘红琼 `affiliation_system=2` 但汇总 `department=1`，所以在“生产部门”筛选下看起来没有同步。
- RED：新增 `HrmProduceAttendanceServiceImplTest#syncFromOvertimeNightStatistics_shouldRefreshExistingDepartmentFromEmployeeAffiliationSystem`，先失败于更新对象仍为 `department=1`。
- 已实现：考勤汇总同步时，已有行和新增行都刷新员工姓名与部门类型；部门类型优先取员工 `affiliationSystem=1/2`，缺失时才回退部门名称关键字推断。
- GREEN：同一新增测试通过。
- 回归验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，7 个测试。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，记录考勤汇总同步部门类型以员工所属体系为准，并在重新同步时纠正既有错误分类行。
- 用户反馈“还是没有”后继续复核：
  - 远端 API 按 `2026-06 + 生产部门 + 潘红琼` 返回 `total=1`，员工行已在列表中；
  - 响应中的 `actualAttendance/accruedAttendance/overtimePay` 为空，而数据库同一行 `positive_attendance/probation_attendance/overtime_pay` 分别为 `21.00/25.00/0.00`。
- 已确认旧 `target/hrsystem-0.0.1-SNAPSHOT.jar` 内 `HrmProduceAttendanceMapper.xml` 仍为 `select *`，未包含源码中的显式别名，这是远端字段为空的直接风险。
- RED：新增 `HrmProduceAttendanceMapperXmlTest` 后，先失败于 `work_over_time` 未显式映射为 `workOverTime`。
- 已实现：`queryProduceAttendanceList` 对列表字段全部显式别名，避免依赖 MyBatis 下划线转驼峰配置或旧 `select *`。
- GREEN：`HrmProduceAttendanceMapperXmlTest` 通过；`HrmProduceAttendanceServiceImplTest` 通过 7 个测试。
- 已重新打包：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -DskipTests package` 通过，`target/hrsystem-0.0.1-SNAPSHOT.jar` 约 110M；包内 mapper 已确认包含显式别名。
- 用户确认本地测试后继续复核：
  - 本地后端 PID `13516` 从 `target/classes` 运行，不是旧 JAR；本地 8081 服务 `hr_web/html`，当前 `Upload-BbctS4ME.js` 已包含新考勤汇总页面和 `actualAttendance/accruedAttendance` 字段绑定；
  - 本地 8081 API 按 `2026-06 + 生产部门 + 潘红琼` 返回 `total=1`，实际出勤 `21.00`，应计出勤 `25.00`；
  - 本地 8081 API 按 `2026-07 + 生产部门 + 潘红琼` 在同步前返回 `total=0`，但上游加班/夜班统计已有 `2026-07` 月度统计；
  - 已通过本地 8081 调用 `syncFromOvertimeNightStatistics` 同步 `2026-07`，接口返回 `data=113`；
  - 同步完成后复查 `2026-07 + 生产部门 + 潘红琼` 返回 `total=1`，实际出勤 `27.00`，应计出勤 `27.00`。
- 已通过无界面 Chrome 复核真实本地页面：
  - 页面搜索框输入“潘红琼”后，页面发出的查询返回 3 条，DOM 中也出现 3 行潘红琼；
  - 表格输入框值显示 `2026-06` 为 `21/25`，`2026-07` 为 `27/27`；
  - `2026-07 + 行政部门 + 潘红琼` 返回 0，`2026-07 + 生产部门 + 潘红琼` 返回 1；当前看不到的最可能原因是筛选部门选成了“行政部门”。
- 用户要求只删除 7 月考勤汇总、别的数据不要动后，已在 `hr_0003.hrm_produce_attendance` 执行单表清理：
  - 删除前 `year=2026/month=7` 汇总共 `113` 行，潘红琼为 `department=2`、实际/应计出勤 `27.00/27.00`；
  - 事务内执行 `DELETE FROM hrm_produce_attendance WHERE year=2026 AND month=7`，实际删除 `113` 行，删除后 7 月汇总为 `0` 行；
  - 未对 `hrm_overtime_night_statistics_detail`、`tbattendanceapprove`、员工档案或 6 月汇总执行删除/更新。
- 删除后复核：
  - 数据库 `2026-06` 汇总仍为 `113` 行，潘红琼仍为 `department=2`、实际/应计出勤 `21.00/25.00`；
  - 本地 8081 接口查询 `2026-07 + department=2 + 潘红琼` 返回 `records=[]/total=0`；
  - 本地 8081 接口查询 `2026-06 + department=2 + 潘红琼` 返回 `total=1`，实际/应计出勤 `21.00/25.00`。

# Progress: 社保报表生成真实进度提示

## 2026-07-17
- 已复核后端项目 `docs/requirements.md`、`docs/development.md` 中和社保管理、真实进度相关的要求。
- 已记录本轮计划到 `task_plan.md`。
- 初步定位：
  - 后端社保代码在 `src/main/java/com/tianye/hrsystem/modules/insurance`；
  - 前端社保页面在 `/Users/jiangyongming/Project/hr/hr_web/src/views/hrm/insurance-scheme`；
  - 既有可参考进度模式为薪资核算 `queryComputeProgress` 和排班提交 `querySubmitProgress`。
- 已确认当前生成链路：
  - 后端 `/hrmInsuranceMonthRecord/computeInsuranceData` 是同步生成接口；
  - 前端 `InsuranceScheme.vue#addMony` 已等待 `computeInsurance()` 完成后刷新列表并提示成功；
  - 当前缺少后端进度状态、进度查询 API、前端进度弹框与轮询。
- RED 验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordControllerTest test` 失败于缺少 `InsuranceComputeProgressVO`；
  - `node tests/insurance-progress-utils.test.mjs` 失败于缺少 `insurance-progress-utils.js`；
  - `node tests/insurance-create-next-month.test.mjs` 失败于缺少 `queryComputeInsuranceProgress` API。
- 已实现后端：
  - 新增 `InsuranceComputeProgressVO`；
  - `HrmInsuranceMonthRecordController` 新增 `/queryComputeInsuranceProgress`；
  - `HrmInsuranceMonthRecordService` 按真实员工处理数量更新内存进度，并在完成/异常时写入终态。
- 已实现前端：
  - 新增 `queryComputeInsuranceProgress()` API；
  - 新增 `insurance-progress-utils.js`；
  - `InsuranceScheme.vue` 新增进度对话框、轮询、完成关闭、成功提示和刷新。
- GREEN 验证：
  - 后端 Maven 定向测试通过，5 个测试；
  - `node tests/insurance-progress-utils.test.mjs`、`node tests/insurance-create-next-month.test.mjs`、`node tests/insurance-advanced-filter.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 最终复核：
  - 重新执行后端 Maven 定向测试，通过 5 个测试；
  - 重新执行三条前端 Node 测试，均通过；
  - 重新执行 `npm run build`，构建通过，保留既有警告；
  - 本轮相关文件的后端/前端 `git diff --check -- <相关文件>` 均通过；
  - 全仓库级 `git diff --check` 命中大量既有未提交改动尾随空白，未改动这些无关文件。
- 已更新后端 `docs/requirements.md`、`docs/development.md`，前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 错误记录：初次搜索命令把不存在的 `tests` 目录传给 `rg`，返回 `rg: tests: No such file or directory`；后续改用 `src/test` 和实际前端测试目录。

# Progress: 薪资管理开始核算穿梭框改造

## 2026-07-17
- 用户补充要求：按部门的穿梭框用树结构展示。
- 已按 TDD 补充前端断言：
  - `tests/salary-start-compute-dialog.test.mjs` 要求部门模式包含 `compute-tree-transfer`、`el-tree`、`show-checkbox` 和 `node-key="deptId"`；
  - `tests/salary-compute-scope-utils.test.mjs` 要求新增 `normalizeSalaryComputeDeptTree(...)` 并保持部门层级。
- RED 验证：
  - `node tests/salary-start-compute-dialog.test.mjs` 失败于缺少树形部门选择结构；
  - `node tests/salary-compute-scope-utils.test.mjs` 失败于缺少 `normalizeSalaryComputeDeptTree` 导出。
- 已实现前端：
  - `AloneComputeDialog.vue` 的人员模式继续使用原 `el-transfer`；
  - 部门模式改为左侧部门树复选框勾选、右侧已选部门树形展示，并显示所选部门展开后的员工数量；
  - 部门树使用字符串 `deptId` 作为 `node-key`，避免雪花 ID 精度风险；
  - 提交 payload 继续复用 `buildSalaryComputePayload(...)`，部门范围仍展开为员工 ID 后受 50 人上限限制。
- GREEN/构建验证：
  - `node tests/salary-start-compute-dialog.test.mjs` 通过；
  - `node tests/salary-compute-scope-utils.test.mjs` 通过；
  - `node tests/compute-progress-utils.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 与 chunk size warning。
- 已更新 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`、后端项目 `docs/requirements.md`、`docs/development.md` 和当前计划/发现/进度记录。

## 2026-07-16
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 已确认后端已有：
  - `/hrmSalaryMonthRecord/computeSalaryData` 核算入口；
  - `/hrmSalaryMonthRecord/queryComputeSalaryProgress` 真实进度查询；
  - `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList` 与核算同口径的员工列表。
- 已确认前端既有要求：薪资核算执行期间展示真实百分比进度；员工相关选择必须保留雪花 ID 字符串，避免大整数精度丢失。
- 已记录本轮计划：前端只保留“开始核算”入口，弹窗支持按人员/按部门穿梭框；后端扩展员工范围并限制一次最多 50 人。
- 已新增实施计划：`docs/plans/2026-07-16-salary-start-compute-transfer-plan.md`。
- 已补 RED/GREEN 测试：
  - `tests/salary-compute-scope-utils.test.mjs` 覆盖员工/部门选项、部门展开、空选择全员、50 人限制；
  - `tests/salary-start-compute-dialog.test.mjs` 覆盖页面只显示“开始核算”和弹窗控件/API 依赖；
  - `SalaryMonthRecordServiceNewTest` 覆盖后端 `employeeIds` 规范化、旧 `employeeId` 兼容、50 人限制、批量进度 scope key；
  - `HrmSalaryMonthEmpRecordMapperXmlTest` 覆盖计薪员工查询返回手机号。
- 已实现后端：
  - `computeSalaryData/queryComputeProgress` 支持 `employeeIds`；
  - `SalaryMonthRecordServiceNew` 统一规范化员工范围，空范围为全员，非空范围最多 50 人；
  - 多人员范围复用原员工核算链路，部门范围由前端展开为员工 ID；
  - `queryPaySalaryEmployeeList` 返回 `mobile`。
- 已实现前端：
  - `SalaryManage.vue` 仅保留“开始核算”按钮；
  - `AloneComputeDialog.vue` 改为开始核算弹窗，支持按人员/按部门穿梭框；
  - `salary.js` 的 FormData helper 支持数组逐项提交；
  - 新增 `salary-compute-scope-utils.js` 统一构造核算 payload。
- 验证通过：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest test`，28 个测试；
  - `node tests/salary-compute-scope-utils.test.mjs`；
  - `node tests/salary-start-compute-dialog.test.mjs`；
  - `node tests/compute-progress-utils.test.mjs`；
  - `npm run build`，保留既有 `::v-deep` 与 chunk size warning；
  - 前后端相关文件 `git diff --check` 通过。

# Progress: 薪资核算个税备注方案实施

## 2026-07-16
- 已复核 `docs/requirements.md`、`docs/development.md` 和 `docs/plans/2026-07-16-salary-tax-remark-solution.md`。
- 已确认本轮实施口径：
  - `hrm_employee.is_remark=2` 使用全年 `60000` 累计减除费用；
  - 无备注按工资计算月份累计 `5000 * month`；
  - 个税不按单月是否超过 5000 单独判断，始终按累计应纳税额减已缴税额计算，负数写 0；
  - 不新增表字段。
- 已定位并修复旧逻辑：
  - `SalaryComputeServiceNew#computeSalary(...)` 原备注分支会按收入条件免扣本月个税；
  - `SalaryMonthRecordServiceNew#calculateMidMonthPromotionSummary(...)` 原半路转正复算也会按备注分支免扣；
  - 原 `month == 12` 重置累计已改为 `month == 1` 重置。
- 已补 RED 测试：
  - 无备注员工 6 月累计减除费用应为 `30000`；
  - 备注员工累计减除费用应为 `60000` 且收入超过扣除后仍需计税；
  - 低工资月份不得产生负个税；
  - 1 月重新开始累计、12 月继续累计全年。
- 已实现：
  - `SalaryComputeServiceNew#resolveCumulativeDeductions(...)` 统一累计减除费用口径；
  - 主薪资核算和半路转正复算均使用该口径；
  - 备注员工不再直接免税；
  - 批量核算上下文移除旧备注免税规则所需的上一年累计收入加载；
  - 既有 `computeSalaryFromMemory(...) -> SalaryComputeServiceNew#computeSalary(...) -> saveTaxAccumulationData(...)` 会在核算后写入当月个税累计。
- GREEN 验证通过：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test`，37 个测试。
- 相关文件格式检查通过：`git diff --check -- <本轮相关文件>` 无输出。

# Progress: 个税/附加导入重名员工匹配修复

## 2026-07-16
- 已复核项目需求、开发文档和个税备注方案。
- 已查询 `hr_0003` 实库确认重复记录详情：`2026-05` 个税累计重复发生在同一个 `employee_id=1831601326890434614`，不是同名员工各自一条。
- 已确认当前同名王芳共有 3 个不同员工 ID，后续导入必须按姓名+手机号或更强唯一键匹配。
- 已补 RED/GREEN 测试：
  - `TaxImportEmployeeMatcherTest` 覆盖姓名+手机号匹配、旧模板姓名唯一兼容、重名缺手机号失败、姓名+手机号未匹配失败；
  - `TaxImportServiceDuplicateNameTest` 覆盖个税累计、附加累计、年度附加扣除三类 Excel 导入按手机号区分同名员工，以及同一员工同一年月重复导入失败。
- 已实现：
  - 新增 `TaxImportEmployeeMatcher`；
  - 三类导入服务改为读取模板手机号列；
  - 导入文件内同员工同期间重复直接报错；
  - 年度附加扣除多年份文件会分别删除涉及年份旧数据；
  - 三个导入接口异常时返回 `Result.Error`。
- 已用用户提供目录核对最终导入文件：三份文件手机列均无空值，唯一键均无重复；王芳三条个税累计按手机号/身份证映射正确。
- 已生成修复报告 `docs/reports/2026-07-16-tax-import-duplicate-name-repair.md` 和默认回滚 SQL `docs/sql/2026-07-16_hr_0003_tax_import_duplicate_name_repair.sql`。
- 用户授权后已正式执行王芳重名错归属修复 SQL，执行方式为临时将脚本末尾 `ROLLBACK` 替换为 `COMMIT` 输入 MySQL，原脚本文件仍保持默认回滚。
- 已新增并正式执行 `docs/sql/2026-07-16_hr_0003_employee_additional_duplicate_cleanup.sql`，清理 2025 年年度附加扣除历史重复；孙小虎 2025 年按最终模板保留住房贷款利息 `12000.00`、婴幼儿照护 `2000.00`。
- 数据修复后复核三类唯一键重复计数均为 `0`：
  - `hrm_personal_income_tax(employee_id, year, end_month)`；
  - `hrm_additional(employee_id, year, month)`；
  - `hrm_employee_additional(employee_id, year)`。
- 定向验证通过：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=TaxImportEmployeeMatcherTest,TaxImportServiceDuplicateNameTest test`，9 个测试。

# Progress: 薪资核算个税新场景方案

## 2026-07-16
- 已读取项目需求文档、开发文档和既有计划/发现/进度记录。
- 已确认本轮目标：只给个税计算方案，不修改业务代码。
- 已将本轮分析任务写入 `task_plan.md`、`findings.md`、`progress.md`。
- 已搜索薪资生成和个税相关代码，定位到 `SalaryComputeServiceNew`、`SalaryMonthRecordServiceNew`、`TaxCalculator`、`HrmPersonalIncomeTax`、`HrmAdditional`、`HrmEmployeeAdditional`。
- 初步发现已有 `hrm_employee.is_remark=2` 低于 6 万不计税逻辑，需要继续确认它与税务局备注规则的对应关系。
- 已还原主链路：`computeSalaryData` 走 `SalaryMonthRecordServiceNew`，加载上月个税累计、当月专项附加累计，再调用 `SalaryComputeServiceNew` 算 `230101/270101~270106`。
- 已确认方案关键差异：新需求应改变累计减除费用取值规则（无备注按月累计、有备注按全年 60000），而不是保留当前 `is_remark=2 && 年收入<60000` 直接跳过本月个税。
- 已新增方案文档 `docs/plans/2026-07-16-salary-tax-remark-solution.md`。
- 已更新 `docs/requirements.md` 和 `docs/development.md`，记录个税备注口径方案。
- 本轮未修改 Java/XML/SQL 业务代码。

# Progress: 附加累计列表按年-月筛选

## 2026-07-16
- 已读取后端 `docs/requirements.md`、`docs/development.md` 和既有计划记录。
- 已定位附加累计相关代码：
  - `HrmAdditionalController`；
  - `HrmAdditionalService`；
  - `QueryAdditionalBO`；
  - `HrmAdditionalMapper.xml`。
- 已确认后端导入与工资核算均已有 `year/month` 口径，但列表查询缺少年月过滤。
- 已在后端计划、发现、进度文件新增本轮记录。
- 已补 RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmAdditionalMapperXmlTest test` 失败于 `QueryAdditionalBO` 无 `year/month` 字段，`HrmAdditionalMapper.xml#queryAdditionalList` 无年月过滤条件。
- 已实现：
  - `QueryAdditionalBO` 新增 `year/month` 查询字段；
  - `HrmAdditionalMapper.xml#queryAdditionalList` 新增 `data.year/data.month` 可选过滤条件，并把员工姓名条件改为非空时才拼接。
- GREEN：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmAdditionalMapperXmlTest test` 通过。
- 已更新后端 `docs/requirements.md` 与 `docs/development.md`，记录附加累计列表按年-月筛选。

# Progress: 按导入模板生成个税/附加 Excel

## 2026-07-16
- 已复核三个导入模板：
  - `/Users/jiangyongming/Desktop/导入模版/个税累计.xls`；
  - `/Users/jiangyongming/Desktop/导入模版/附加扣除累计.xls`；
  - `/Users/jiangyongming/Desktop/导入模版/专项扣除累加表.xlsx`。
- 已安装 `.xls` 写入所需最小 Python 依赖 `xlwt/xlutils`。
- 已从 `hr_0003.hrm_employee` 与 `hrm_dept` 补齐岗位、工号、部门、手机。
- 已生成最终导入文件到 `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels`：
  - `个税累计.xls`：100 行，`2026-05`；
  - `附加扣除累计.xls`：41 行，`2026-05`；
  - `专项扣除累加表.xlsx`：64 行，`2025/2026`；
  - `生成说明.txt`。
- 已读取生成文件复核：三份文件手机列均无空值；个税/附加按姓名+手机无重复，年度附加按姓名+手机+年份无重复。
- 已更新 `docs/requirements.md`、`docs/development.md` 和本轮计划/发现/进度文件。

# Progress: 个税计算基础支持数据整理

## 2026-07-16
- 启动本轮个税计算基础支持数据整理任务。
- 已读取项目需求文档和开发文档；确认现有文档仅记录个税/附加列表展示排序规则，未记录申报表到系统字段的详细映射。
- 已定位实际申报资料目录：`/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报`。
- 已清点四个月综合所得申报 `.xls`，确认 2025 年主表为 42 列，2026 年主表为 51 列，累计区列位置不同，需按表头语义定位。
- 已定位后端三类数据表和导入口径：`hrm_personal_income_tax`、`hrm_additional`、`hrm_employee_additional`。
- 已按身份证号匹配 `hr_0003.hrm_employee` 并生成整理产物到 `/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716`。
- 已生成幂等 SQL、核对报告、明细 CSV、未匹配员工清单和导入列位参考 Excel。
- 已用 MySQL 事务 `ROLLBACK` 方式验证 SQL，返回码 `0`，未实际落库。
- 已更新 `docs/requirements.md` 与 `docs/development.md` 记录本轮数据整理口径和产物位置。

# Progress: 基本工资金额设置新增全勤和生产月休配置

## 2026-07-16
- 已复核后端/前端需求与开发文档、计划文件和当前源码，确认本轮按三项配置处理：普通员工全勤金额、领导全勤金额、生产体系员工月度休息天数。
- 后端已实现：
  - `HrmSalaryBasic`、`QuerySalaryBasicDto`、`QuerySalaryBasicVO` 增加三项字段；
  - 新增 `HrmSalaryBasicDefaults` 统一默认值 `100/500/4`；
  - `HrmSalaryBasicService` 保存、按 ID 查询、查询最新配置时补默认值，并按 `createTime/id` 选择最新记录；
  - `HrmSalaryMonthEmpRecordMapper.xml` 和 `SalaryMonthRecordServiceNew` 的全勤金额读取改为使用配置；
  - `HrmOvertimeNightStatisticsServiceImpl` 的生产体系应出勤月休天数改为读取最新基本工资设置，缺失兜底 `4`。
- 前端已实现：
  - `src/views/manage/salary/Index.vue` 新增“普通员工全勤金额”“领导全勤金额”“生产体系员工月度休息天数”；
  - 表单默认值为 `100/500/4`，加载旧接口数据时合并默认值；
  - 三项参与表单校验并随原接口保存。
- 已新增 SQL：`docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql`。
- SQL 已执行：`mysql -h 153.0.237.98 -P 3306 -uroot -p... hrsystem < docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql` 返回成功。
- SQL 复核：`hr_0001` 至 `hr_0005` 均存在：
  - `leader_full_attendance_amount decimal(10,2) default 500.00`；
  - `ordinary_full_attendance_amount decimal(10,2) default 100.00`；
  - `production_monthly_rest_days int default 4`。
- RED 记录：
  - `node tests/salary-basic-settings-page.test.mjs` 先失败于页面缺少三项新字段；
  - 初次 `mvn -Dtest=HrmSalaryBasicServiceTest test` 被本机全局 Maven settings 阻断，报 `com.aliyun:tea:[1.1.14,2.0.0)` 无可用版本。
- GREEN/回归验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmSalaryBasicServiceTest test` 通过，4 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml '-Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldUseConfiguredProductionMonthlyRestDaysForProductionEmployee' test` 通过，6 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，53 个测试；
  - `node tests/salary-basic-settings-page.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 已更新后端 `docs/requirements.md`、`docs/development.md`，前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，以及本轮 `task_plan.md/findings.md/progress.md`。

# Progress: 考勤汇总行政体系加班同步口径修正

## 2026-07-15
- 已复核项目需求/开发文档和既有计划记录，确认行政/生产体系实际出勤与应计出勤只使用本地钉钉审批加班，不使用日级自动/月度自动加班。
- 已定位根因：考勤汇总同步服务直接读取加班/夜班统计页面展示用 `overtimeHours`，导致张明 `2026-06` 的日级自动加班 `47.72` 被写入 `hrm_produce_attendance.work_over_time`。
- 已补 RED：
  - `HrmProduceAttendanceServiceImplTest#syncFromOvertimeNightStatistics_shouldUseAttendanceOvertimeHoursForAdministrativeAttendanceSummary`；
  - `HrmOvertimeNightStatisticsServiceImplTest` 中张明无审批加班和有审批加班两类查询断言。
- RED 验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldAddOvertimeApprovalToActualAttendance test` 编译失败，缺少 `QueryOvertimeNightStatisticsPageVO.get/setAttendanceOvertimeHours`。
- 已实现 GREEN：
  - `QueryOvertimeNightStatisticsPageVO` 新增 `attendanceOvertimeHours`；
  - 统计查询复用实际/应计出勤模块的加班口径填充该字段；
  - 考勤汇总同步优先用该字段写入 `workOverTime` 并计算 `overtimePay`。
- GREEN 验证：同一 Maven 命令通过，8 个测试，0 失败。
- 完整相关回归：
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，58 个测试，0 失败；
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，7 个测试，0 失败。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录 `attendanceOvertimeHours` 与考勤汇总同步口径。

# Progress: 上传考勤同步加班夜班出勤数据

## 2026-07-15
- 已收到补充要求：上传考勤列表删除 `空班/次`、`中班/次`、`当月休假/天` 三列。
- 已读取本项目需求/开发文档和既有计划文件；确认同步方向应为从已生成的加班/夜班统计明细写入上传考勤数据表。
- 已读取 UI/UX 指南，确认本轮前端按数据密集型后台页处理，沿用现有表格/弹窗风格。
- 已定位前端上传考勤入口和 API 文件，下一步定位后端 `getChecking/importProduceAttendance` 对应接口、表结构与现有数据模型。
- 已补 RED 测试：
  - 后端 `mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于缺少 `SyncProduceAttendanceBO`、`UpdateProduceAttendanceCellBO`。
  - 前端 `node tests/upload-attendance-page.test.mjs` 失败于上传考勤 API 缺少 `syncProduceAttendance`。
- 已实现后端同步与保存：
  - 新增同步/单元格更新 BO；
  - 新增 `/hrmProduceAttendance/syncFromOvertimeNightStatistics` 和 `/hrmProduceAttendance/updateCell`；
  - 新增 `overtimePay` 字段、列表 VO 字段、显式 SQL 别名和租户库 DDL 脚本；
  - 薪资核算优先使用上传考勤 `overtimePay` 作为加班费。
- 已实现前端：
  - 上传考勤页新增同步按钮、年月弹窗；
  - 删除 `空班/次`、`中班/次`、`当月休假/天` 三列；
  - 新增 `应计出勤天数`、`加班工资` 列；
  - 业务数据列增加输入框和保存按钮。
- 当前已通过：
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test`，5 个测试；
  - `node tests/upload-attendance-page.test.mjs`。
- 最终验证：
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，5 个测试；
  - `node tests/upload-attendance-page.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 前端开发服务已启动：`http://localhost:8081/`。

## 2026-07-15 补充：考勤汇总命名、隐藏导入、金额自动计算
- 用户补充要求：
  - “上传考勤”菜单和页面文字改为“考勤汇总”；
  - 隐藏“导入考勤”按钮；
  - 加班工资按加班时长乘基本工资金额设置中的每小时加班费；
  - 夜班补贴按夜班次数乘基本工资金额设置中的夜班补贴额度。
- 已按 TDD 更新前端源码断言：
  - RED：`node tests/upload-attendance-page.test.mjs` 失败于页面标题仍为“上传考勤”；
  - GREEN：改路由菜单名、页面标题，移除导入按钮和 `ImportDialog` 后通过。
- 已按 TDD 更新后端同步服务测试：
  - 首次 RED 因 matcher 对空金额字段 NPE，已改为空安全断言；
  - RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于同步结果 `overtimePay/nightSubsidy` 为空；
  - GREEN：同步时读取最新 `hrm_salary_basic` 并计算金额后通过，5 个测试。
- 已执行数据库脚本：
  - `mysql -h 153.0.237.98 -P 3306 -uroot -p... < docs/sql/2026-07-15_hrm_produce_attendance_overtime_pay.sql` 返回成功；
  - `information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 均存在 `hrm_produce_attendance.overtime_pay decimal(10,2) COMMENT '加班工资'`。
- 最终验证：
  - `mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，6 个测试；
  - `node tests/upload-attendance-page.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - `rg` 复核构建产物 `html/assets` 包含“考勤汇总”，上传考勤页面 chunk 不再包含“导入考勤”。

# Progress: 加班/夜班统计单人批量开始统计补全

## 2026-07-15
- 已读取 `using-superpowers`、`project-modification`、`brainstorming`、`test-driven-development`、`planning-with-files` 工作流说明。
- 已读取项目 `docs/requirements.md`、`docs/development.md` 以及现有 `task_plan.md/findings.md/progress.md`。
- 已确认本轮需求：把“开始统计”的功能和业务逻辑补齐到“单人统计”，差异仅为统计员工范围由全员变为选中的批量员工。
- 已记录并修正计划恢复脚本解释器错误：第一次用 `sh` 执行 Python 脚本失败，第二次改用 `python3` 成功。
- 已新增本轮计划与初始发现，下一步定位后端统计接口/服务和前端按钮入口。
- 已初步定位后端统计文件和测试文件；`rg` 首次搜索误带不存在的 `test/tests` 路径，返回码为 2，但有效输出仍定位到 `src/main/java` 与 `src/test/java` 下的统计代码。
- 已补后端 RED：批量单人统计测试先失败于 `QueryOvertimeNightStatisticsPageBO` 缺少 `employeeIds`。
- 已补前端 RED：单人选择测试先失败于页面仍循环逐个员工调用后端。
- 已实现后端批量单人统计：
  - `QueryOvertimeNightStatisticsPageBO` 新增 `employeeIds`；
  - `startStatisticsForEmployee` 优先按 `employeeIds` 加载选中员工，逐员工执行“删除该员工当月旧明细 + 保存新明细”；
  - 旧 `employeeId` 单人入参继续兼容；
  - 按全员“开始统计”的员工有效性口径跳过 `isDel=1` 的已删除员工。
- 已实现前端一次提交选中员工集合：`confirmSingle` 改为请求体携带 `employeeIds: targetEmployeeIds`，不再循环逐个员工请求。
- 定向验证已通过：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldCalculateAllSelectedEmployeesInBatch test`
  - `node tests/overtime-night-api.test.mjs`
  - `node tests/overtime-night-single-selection.test.mjs`
- 后端完整统计相关回归已通过：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，52 个测试；
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，112 个测试。
- 前端完整相关验证已通过：
  - `node tests/overtime-night-utils.test.mjs`
  - `node tests/attendance-display-columns.test.mjs`
  - `node tests/overtime-night-page.test.mjs`
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 代码评审后补充 RED/GREEN：
  - `startStatisticsForEmployee_shouldSkipDeletedSelectedEmployeesLikeFullStatistics` 先失败于单人统计仍处理 `isDel=1` 员工，修复后通过。
- 已更新后端 `docs/requirements.md` 与 `docs/development.md`，记录 `employeeIds` 批量单人统计、与“开始统计”共用同一默认计算入口、旧 `RAW_SYNC_DATA_ONLY` 说明已为历史口径。

# Progress: 审批数据手工添加支持多日期

## 2026-07-15
- 已读取 `docs/requirements.md`、`docs/development.md` 和既有计划文件。
- 与本轮相关的既有规则已确认：
  - 审批数据手工添加只写本地 `tbattendanceapprove`；
  - 不调用钉钉接口；
  - 必须校验员工考勤映射；
  - 时长由后端根据开始/结束计算；
  - 后续统计读取单条审批快照。
- 初步实现方向：新增多日期/多时间段请求字段，服务端按员工和时间段笛卡尔生成独立审批快照，并保留旧 `beginTime/endTime` 入参兼容。
- 已记录恢复脚本解释器错误：第一次用 `sh` 运行 Python 脚本失败，第二次改用 `python3` 成功。
- 已按 TDD 补后端 RED：`addManualApproval_shouldPersistEachSelectedApprovalRangeForEachEmployee` 先失败于 `AddAttendanceApprovalBO` 缺少 `approvalRanges`。
- 已实现后端多日期添加：
  - `AddAttendanceApprovalBO` 新增 `approvalRanges[]` 与嵌套 `ApprovalRangeBO`；
  - 服务层按“员工 × 时间段”保存独立本地审批快照；
  - 旧单段 `beginTime/endTime` 入参仍兼容。
- 已按 TDD 补前端 RED：添加请求体工具和添加弹窗源码测试先失败于仍只支持单个 `beginTime/endTime`。
- 已实现前端添加弹窗多日期行，可增删审批时间段，按段展示时长并合计展示。
- 根据代码审查补充后端映射选择修复：
  - RED：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest#addManualApproval_shouldUseLatestAttendanceUserMappingWhenEmployeeHasHistory test` 失败，旧逻辑把手工审批写入 `ding-old`；
  - GREEN：同一测试通过，手工添加在多条 `tbattendanceuser` 历史映射中优先使用较新的 `createTime`，时间相同再按较大 `id`。
- 当前定向验证：
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest#addManualApproval_shouldPersistEachSelectedApprovalRangeForEachEmployee test` 通过；
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，32 个测试；
  - `node tests/attendance-approval-api.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs`、`node tests/approval-duration-utils.test.mjs`、`node tests/attendance-approval-dialog.test.mjs` 均通过。
- 最终回归验证：
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，110 个测试；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

# Progress: 审批数据按主键删除

## 2026-07-15
- 已确认新增需求覆盖旧文档“审批数据不提供删除操作”的限制。
- 已按 TDD 补后端 RED：
  - 控制器需委托 `deleteApproval(DeleteAttendanceApprovalBO)`；
  - 服务层删除 BO 只允许包含 `approvalId`；
  - 服务层必须通过 `approvalRepository.findById(approvalId)` 删除查到的单条实体；
  - 缺少 `approvalId` 时返回“审批数据ID不能为空”。
- RED 验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 失败于 `DeleteAttendanceApprovalBO` 未创建。
- 已实现后端：
  - 新增 `DeleteAttendanceApprovalBO`；
  - 新增 `POST /hrmAttendanceApproval/delete`；
  - 新增 `IHrmAttendanceApprovalService#deleteApproval(...)` 与 `HrmAttendanceApprovalServiceImpl#deleteApproval(...)`。
- 后端定向验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，30 个测试。
- 后端组合回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，108 个测试。
- 已更新后端 `docs/requirements.md` 与 `docs/development.md`，记录删除必须按 `approvalId` 执行，不能按姓名/部门/时间范围删除。

# Progress: 审批数据手工添加

## 2026-07-15
- 已读取项目需求/开发文档与既有计划记录。
- 本次新增需求与既有文档存在一处变化：原审批数据功能“不提供新增和删除操作”，本轮需要新增手工添加入口，并保存到同一审批数据本地表。
- 初始设计假设：
  - 手工新增不调用钉钉接口；
  - 保存到 `tbattendanceapprove`；
  - 保存时间使用服务器当前时间，作为列表同步时间；
  - 时长按开始/结束自动计算小时，按 `1天=8小时` 换算展示。
- 已在 `task_plan.md` 新增本轮计划，下一步定位现有审批数据后端和前端实现。
- 已定位后端：
  - 审批数据列表读取 `tbattendanceapprove`，通过 `userId -> tbattendanceuser -> hrm_employee/hrm_dept` 展示人员与部门；
  - `createTime` 即列表同步时间，`workDate` 即列表申请日期。
- 已按 TDD 补 RED：
  - 后端服务/控制器失败于 `AddAttendanceApprovalBO` 不存在；
  - 前端失败于新增 API、时长计算函数、添加请求体工具和添加弹窗不存在。
- 已实现后端最小闭环：
  - 新增 `POST /hrmAttendanceApproval/addManual`；
  - 按选中员工逐人写入 `tbattendanceapprove`；
  - 后端按开始/结束时间重新计算小时数，保存 `durationUnit=小时`；
  - 服务器当前时间写入 `createTime`，开始日期写入 `workDate`；
  - 缺少 `tbattendanceuser` 映射时返回中文错误。
- 定向验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，27 个测试。
- 后端组合回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，105 个测试。
- 已更新 `docs/requirements.md` 与 `docs/development.md`，记录手工添加本地审批数据的业务规则、接口和验证结果。

# Progress: 潘红琼审批同步缺失与空时长排查

## 2026-07-15
- 已读取本项目 `docs/requirements.md`、`docs/development.md` 以及既有 `task_plan.md/findings.md/progress.md`。
- 已确认与本次相关的既有约束：
  - 审批数据列表只读取本地 `tbattendanceapprove`，不在查询链路调用钉钉；
  - 员工匹配必须使用员工 ID、手机号、部门、钉钉 userId 等稳定字段，禁止只按姓名；
  - 请假审批 `duration/durationUnit` 为空时需提示或修正，不能长期静默使用不可靠兜底；
  - 修正或重抓审批快照后，目标员工/月仍需重新执行单人统计或整月统计。
- 已新增本轮排查计划，下一步读取审批同步/解析代码与本地数据状态。
- 用户补充要求已纳入排查口径：优先使用钉钉审批详情返回的结构化时长，只有无可识别字段时才允许本地起止时间兜底；后续需针对潘红琼审批详情原始组件验证解析器是否漏读。
- 只读确认潘红琼本地映射：`employee_id=1831601326890434570`、`userId=02533200433128427058`、行政人力资源部。
- 钉钉 `0003` 应用接口复核：
  - 6 月发起时间窗口下请假流程返回 9 条；
  - 4 条业务请假日期在 6 月，本地应参与 6 月统计；
  - 5 条业务请假日期在 5 月，本地当前 6 月统计口径会跳过。
- 本地 4 条潘红琼 6 月业务日期请假均为空时长，但钉钉详情均返回 `durationInHour`，根因不是钉钉缺字段，而是历史解析/落库未保存。
- 已按 TDD 调整解析器：
  - RED：旧逻辑把复杂请假 `durationInHour=32` 保存为 `4天`；
  - GREEN：复杂请假优先保存钉钉小时值 `32小时`。
- 验证：
  - `mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest#parse_shouldReadDingTalkComplexLeaveHourDurationFromExtValue test` 通过；
  - `mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest test` 通过，8 个测试；
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，94 个测试。
- 注意：曾并行运行两组 Maven 测试导致 surefire fork 启动失败；顺序重跑后通过，非代码断言失败。

# Progress: 王芳（行政人力资源部）审批重复排查

## 2026-07-15 最终修复与复核
- 已按用户确认执行 `docs/sql/2026-07-14_fix_hr_0003_wangfang_dingtalk_mapping.sql` 的 guarded 修复：
  - 行政人力资源部王芳 `employee_id=1831601326890434572 / TYNG-437 / mobile=15871989405` -> `dingtalk_user_id=1957266823950408`，`tbattendanceuser.id=225`；
  - 生产部王芳 `employee_id=1831601326890434614 / TYNG-107 / mobile=13032750052` -> `dingtalk_user_id=1068600027950408`，`tbattendanceuser.id=226`；
  - 财务部王芳保持 `0305684530950408`。
- 已重新获取行政人力资源部王芳 2026-06 审批；6 月业务日期只有 `2026-06-26`、`2026-06-28` 两条年假，合计 `16.00小时`。
- 已复查行政人力资源部王芳审批重复：
  - `tbattendanceapprove.id` 主键无重复；
  - 按 `userId/tagName/subType/beginTime/endTime/workDate/duration/durationUnit` 无重复组；
  - 按同日同类型无重复组；
  - 生产部王芳 `1068600027950408` 的 13 条调休是另一名同名员工数据，不是重复。
- 已修正生产体系 2026-06 应出勤算法：
  - 固定月休 4 天；
  - 只额外扣工作日上的 `legal_rest`；
  - 2026-06 端午 `2026-06-19/20/21` 中仅扣周五 `2026-06-19`，不再重复扣周末 `2026-06-20/21`；
  - 行政人力资源部王芳重算后应出勤 `25天/200小时`。
- 本地 9083 接口复核后已停服：
  - `startStatisticsForEmployee` 返回 `expectedAttendanceDays=25`、`actualAttendanceHours=184.00`、`actualAttendanceDays=23`、`accruedAttendanceHours=200.00`、`actualAttendanceRemark=扣除：年假16.00小时`；
  - DB 落库 `hrm_overtime_night_statistics_detail.detail_id=2077126831228878848`，同样为 `expected_attendance_days=25`、`actual_attendance_days=23`、`accrued_attendance_hours=200.00`。
- 验证命令：
  - RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeductOnlyWeekdayLegalRestForProductionEmployee test` 旧实现失败；
  - GREEN：同一命令通过；
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，48 个测试，0 失败；
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，92 个测试，0 失败。

## 2026-07-15 空时长与重复审批复查
- 用户追问最初发现的审批快照时长为空、重复审批参与统计问题是否已经解决。
- 已按当前 `hr_0003` 只读复查 8 名员工：王琪、高文利、王芳（财务）、黎冬霜、严锦、庞龙斌、李学飞、林艳。
- 结论：尚未全部解决，历史数据仍需清理。
- 当前仍参与统计且 `duration/durationUnit` 为空的审批数量：
  - 王琪 2 条，高文利 13 条，王芳（财务）3 条，黎冬霜 2 条，严锦 1 条，庞龙斌 1 条，李学飞 5 条，林艳 2 条。
- 当前仍参与统计的重复组：
  - 高文利 `2026-06-01 请假/调休` 2 条；
  - 林艳 `2026-06-06 加班` 2 条；
  - 林艳 `2026-06-18 请假/调休` 2 条。
- 当前统计明细仍是 `2026-07-14 21:57~21:58` 生成；处理完审批快照后还需重新执行单人统计或整月统计。

## 2026-07-14 2.png 部门信息更正
- 用户确认 `/Users/jiangyongming/Desktop/2.png` 是行政人力资源部王芳的请假详情，且图中审批编号 `202606291142000335491` 显示所在部门 `HB行政人力资源部`、请假类型 `年假`、日期 `2026-06-28`、时长 `1天`。
- 重新只读校验钉钉通讯录：
  - `1957266823950408` = 王芳 / `15871989405` / 行政人力资源部；
  - `1068600027950408` = 王芳 / `13032750052` / 生产部 / 杀菌机/卧离机；
  - `0305684530950408` = 王芳 / `13872932293` / 财务部。
- 重新只读校验钉钉请假实例：
  - `1957266823950408` 在 2026-06 发起时间窗口返回 6 条，详情部门均为 `HB行政人力资源部`；
  - 其中 2 条业务日期在 2026-06，分别为 `2026-06-26 年假1天` 和 `2026-06-28 年假1天`，合计 `16小时`；另外 4 条业务日期在 2026-05，不应进入 6 月统计；
  - `1068600027950408` 返回 13 条，详情部门均为 `HB生产部`，这些属于生产部王芳，不是行政人力资源部王芳。
- 只读 SQL 确认本地 `hr_0003` 两条同名王芳的 `dingtalk_user_id/tbattendanceuser` 对调：
  - 行政人力资源部王芳 `employee_id=1831601326890434572 / mobile=15871989405` 当前错误挂 `1068600027950408`；
  - 生产部王芳 `employee_id=1831601326890434614 / mobile=13032750052` 当前错误挂 `1957266823950408` 且缺少正确 `tbattendanceuser`。
- 结论更正：
  - 之前“截图 6 条属于生产部王芳、行政人力资源部王芳为 13 条”的结论作废；
  - 真实原因是本地映射对调，导致审批抓取错把生产部王芳 13 条调休当成行政人力资源部王芳；
  - 行政人力资源部王芳的 6 条不是重复，其中进入 2026-06 统计的年假 16 小时与 Excel `200 / 184 / 200` 一致。
- 已补 RED/GREEN 测试和代码修复：
  - RED：`fetchMonthData_shouldRefreshExistingDingTalkUserIdWhenNameAndMobileMatchDifferentUser` 旧逻辑失败，继续用旧 ID；
  - GREEN：审批抓取已有 `dingtalk_user_id` 时会先校验钉钉通讯录姓名/手机号，不匹配则按姓名+手机号刷新并写回。
- 已准备但尚未执行数据修复脚本：`docs/sql/2026-07-14_fix_hr_0003_wangfang_dingtalk_mapping.sql`。
- 回归验证：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，91 个测试 0 失败。

## 2026-07-14
- Continued after user clarification: the re-fetch target is 王芳 from 行政人力资源部.
- Re-read backend requirements/development docs and existing planning records.
- Added a focused task plan for duplicate approval investigation.
- Ran read-only SQL against `hr_0003`:
  - target 王芳 now has `tbattendanceuser.id=225 / userId=1068600027950408 / empId=1831601326890434572`;
  - 财务部王芳 remains isolated at `userId=0305684530950408`;
  - 生产部王芳 has no June approval records.
- Duplicate checks completed:
  - target `empId` mapping count `1`;
  - target `userId` mapping count `1`;
  - exact business duplicate groups `0`;
  - duplicate day coverage after expanding approval ranges `0`.
- Target 王芳 June approvals:
  - 13 rows, all `请假/调休`;
  - total `25.00` days / `200.00` hours;
  - uncovered dates: `2026-06-07,2026-06-14,2026-06-19,2026-06-21,2026-06-28`.
- Current statistics table still has old zero placeholder for target 王芳 in `2026-06`; this needs a single-employee or full-month statistics rerun after deployment, not duplicate approval cleanup.
- Re-read the Excel row:
  - admin HR 王芳 is `200 / 184 / 200`;
  - leave columns are `事假0 / 病假0 / 调休0 / 年假16`.
- Performed DingTalk read-only `processinstance/get` spot-check for all 13 target approval IDs:
  - all are `COMPLETED / agree`;
  - all originate from `originator_userid=1068600027950408`;
  - all are `调休` form rows, totaling `200` hours.
- Conclusion:
  - no duplicate approval and no same-name contamination were found;
  - current mismatch is a data-source conflict: DingTalk has approved `调休200h`, Excel records `年假16h`.
- Verification:
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` passed, 90 tests, 0 failures.

## 2026-07-14 钉钉后台 6 条 vs 接口 13 条
- User challenged the 13-row result because DingTalk backend shows 6 leave approval applications for admin HR 王芳.
- Reproduced backend API chain directly against DingTalk:
  - `process/listbyuserid(userId=1068600027950408)` returned one leave template: `请假申请 / PROC-F083E7CC-FAA7-4BBE-8A58-C3A8B9E8F8FB`;
  - `processinstance/listids` for `2026-06-01 00:00:00` through `2026-06-30 23:59:59` returned 13 process instance IDs;
  - `processinstance/get` confirmed all 13 are `COMPLETED / agree`, originator `1068600027950408`, leave type `调休`.
- Counted common filter variants:
  - created/launched in June: 13;
  - completed in June: 12;
  - business leave start date in June: 13;
  - business leave start date >= `2026-06-15`: 7;
  - business leave start date >= `2026-06-15` and completed in June: 6.
- Working explanation:
  - backend 6-row view likely uses a narrower filter such as June second half plus completion time in June;
  - full-month launch-time API returns 13, so current evidence does not support local duplicate insertion.
- Updated `task_plan.md`, `findings.md`, `docs/development.md`, and the reconciliation report.
- User clarified `/Users/jiangyongming/Desktop/1.png` is the backend screenshot.
- Inspected the image and extracted visible business IDs:
  - `202606291142000335491`;
  - `202606270904000229005`;
  - `202606031746000226579`;
  - `202606031745000311483`;
  - `202606031742000340915`;
  - `202606031741000152277`.
- Rechecked DingTalk details for all three 王芳 `userId`s:
  - those 6 business IDs all belong to `originator_userid=1957266823950408`;
  - this is production-department 王芳 `employee_id=1831601326890434614 / TYNG-107`;
  - admin HR 王芳 remains `originator_userid=1068600027950408` with 13 June leave instances.
- Updated findings/report/development notes to replace the earlier filter-only hypothesis with the same-name employee mismatch conclusion.

# Progress: 行政体系 Excel 与加班夜班统计出勤对账

## 2026-07-15 代码先修后复查
- 已将生产体系实际出勤加班来源调整为本地加班审批口径，不再使用月度自动加班汇总抬高实出勤。
- 验证通过：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`：50 个测试，0 失败；
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`：94 个测试，0 失败。
- 本机启动 `9083` 后端，执行 `POST /hrsystem/hrmOvertimeNightStatistics/startStatistics` 重算 `2026-06`；接口返回 `code=200`。
- 重新查询 `queryPageList(month=2026-06)` 并对比 Excel 主表 `6月`：27 人中 16 人三项一致，11 人仍有差异；差异全部为实出勤。
- 仍不一致人员：王琪、潘红琼、杨太琴、高文利、王洪平、余德胜、聂小玲、马春霞、张雪梅、方小荣、何晓光。

## 2026-07-15 再次和 Excel 对比
- 按用户要求再次启动本地 `9083` 最新后端并执行 `startStatistics(month=2026-06)`。
- 重算完成后查询 `queryPageList(month=2026-06)`，系统返回 113 名员工统计。
- 与 Excel 主表 `6月` 的 27 人逐行对比结果：21 人三项一致，6 人仍有差异；差异全部为实出勤。
- 仍不一致人员：余德胜、聂小玲、马春霞、张雪梅、方小荣、何晓光。
- 已查询这 6 人审批快照：
  - 余德胜、聂小玲、方小荣没有有效请假扣减审批；
  - 马春霞有 `8h` 加班审批参与统计；
  - 张雪梅有 `1h` 加班审批参与统计，但缺病假/年假扣减；
  - 何晓光有一条 `duration=null` 的加班审批，系统按 `08:00-17:00` 折算 `9h`。

## 2026-07-15 再次复查
- 按用户要求重新检查 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx` 的应出勤、实出勤、应计出勤是否与当前系统统计全部一致。
- 读取 Excel 主表 `6月`，确认仍是 27 名员工，小时列为 D/E/F。
- 使用本机 `9083` 后端登录 `hbadmin`，调用 `POST /hrsystem/hrmOvertimeNightStatistics/queryPageList` 查询 `month=2026-06`，返回 113 名员工统计。
- 用员工 ID 固定匹配两名王芳：
  - `王芳` -> 行政人力资源部 `1831601326890434572`；
  - `王芳（财务）` -> 财务部 `1831601326890434577`。
- 当前系统统计明细最大更新时间为 `2026-07-15 04:57:36`。
- 对账结果：
  - 27 人中 15 人三项完全一致；
  - 12 人仍有差异；
  - 当前全部差异只发生在实出勤，应出勤和应计出勤均已对齐。
- 已将本次复查结果追加到 `docs/reports/2026-07-14-admin-attendance-excel-reconciliation.md` 和 `findings.md`。

## 2026-07-15 生产体系月休四天与王芳映射
- Continued the focused follow-up for production-system fixed monthly rest and duplicate-name 王芳 mapping.
- Implemented production expected-attendance rule in `HrmOvertimeNightStatisticsServiceImpl`:
  - `affiliation_system=2` uses `month length - 4 fixed rest days - legalHolidayRestDays`;
  - administrative employees continue using the existing workweek month calendar `workDays`;
  - 2026-06 with `legalHolidayRestDays=1` yields `25` days / `200.00` hours.
- Updated `HrmOvertimeNightStatisticsServiceImplTest`:
  - added/kept `startStatistics_shouldUseFixedFourDayMonthlyRestForProductionEmployee`;
  - fixed strict Mockito stubs that had mismatched months or were unnecessary;
  - preserved existing saved-detail assertions in older tests.
- Rechecked `hr_0003` 王芳 rows:
  - 行政人力资源部王芳 `employee_id=1831601326890434572 / TYNG-437 / dingtalk_user_id=1068600027950408` still has no `tbattendanceuser`;
  - 财务部王芳 `employee_id=1831601326890434577 / TYNG-393` maps to `tbattendanceuser.id=200 / userId=0305684530950408 / groupId=1170211404`;
  - 生产部王芳 `employee_id=1831601326890434614 / TYNG-107` is not in this Excel main sheet.
- Rechecked mapping code:
  - attendance sync `AttendanceUserManager#GetAndSave()` writes `tbattendanceuser` from DingTalk roster/group data;
  - approval fetch `HrmAttendanceApprovalSyncServiceImpl#resolveAttendanceUserForEmployee(...)` can create/fix `tbattendanceuser` from `hrm_employee.dingtalk_user_id` or unique DingTalk name+mobile lookup.
- Verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 47 tests, 0 failures.
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test` passed, 2 tests, 0 failures.
- Updated backend docs and reconciliation report:
  - `docs/requirements.md`;
  - `docs/development.md`;
  - `docs/reports/2026-07-14-admin-attendance-excel-reconciliation.md`;
  - `task_plan.md`;
  - `findings.md`;
  - `progress.md`.

## 2026-07-14
- Read required skills: `using-superpowers`, `project-modification`, `planning-with-files`, `systematic-debugging`.
- Refreshed backend requirements/development/PRD documentation for overtime-night attendance formulas.
- Started a focused task plan for comparing the June administrative attendance Excel against system overtime/night statistics.
- Parsed the Excel workbook:
  - main sheet `6月`, 27 employees;
  - direct hour columns: expected, actual, accrued;
  - supporting columns: personal/sick/compensatory/annual leave and overtime.
- Queried local backend API per employee for `2026-06`.
- Initial result:
  - 8 employees matched all three values;
  - 19 employees had at least one mismatch.
- Root cause evidence gathered so far:
  - expected=0 mismatches come from employees stored as `affiliation_system=2` while the system currently has no production fixed-monthly-rest expected-attendance calculation;
  - actual-hour mismatches come from approval duration blanks and duplicate approval rows participating in statistics.
- Created detailed report:
  - `docs/reports/2026-07-14-admin-attendance-excel-reconciliation.md`.
- Updated backend documentation:
  - `docs/requirements.md` records the missing production fixed-monthly-rest calendar requirement and approval-duration/duplicate-check requirements;
  - `docs/development.md` records the 27-person reconciliation result and root cause classes.
- Follow-up correction from user:
  - production-system employees have a fixed monthly rest of 4 days;
  - the 200-hour Excel rows should be described as production-system fixed-rest plus business holiday/rest days, not administrative/logistics single-rest;
  - the current Excel has two 王芳 rows, while `hr_0003.hrm_employee` has multiple same-name employee records, so reconciliation must use employee ID/job number/mobile/department/DingTalk userId.
- Rechecked `hr_0003` same-name data:
  - first query attempt used the wrong field `hrm_employee.name`; schema check confirmed the correct field is `employee_name`;
  - current employee master data has three same-name 王芳 records: 行政人力资源部 `TYNG-437`, 财务部 `TYNG-393`, 生产部 `TYNG-107`;
  - this Excel reconciliation only involves the first two rows; production-department 王芳 is excluded from the June Excel main sheet.
- Updated report and documentation accordingly.

# Progress: 王琪实际出勤与应计出勤计算过程悬浮展示

## 2026-07-14
- Read backend and frontend requirements/development docs for the overtime/night actual-attendance matrix.
- Confirmed this is a display enhancement: existing actual/accrued attendance calculations should not change.
- Located relevant frontend implementation:
  - `src/views/hrm/attendance/overtime-night/overtime-night-utils.js`;
  - `src/views/hrm/attendance/overtime-night/Index.vue`;
  - `src/views/hrm/attendance/overtime-night/DailyDetailPage.vue`.
- Current implementation already has enough row fields to build a readable calculation tooltip from frontend data; no backend field is required unless later validation needs more granular server-provided terms.
- Added RED frontend tests:
  - `node tests/overtime-night-utils.test.mjs` failed because calculation text fields did not exist;
  - `node tests/attendance-display-columns.test.mjs` failed because the actual/accrued attendance cells were still plain columns.
- Implemented frontend changes:
  - `overtime-night-utils.js` builds `actualAttendanceCalculationText` and `accruedAttendanceCalculationText`;
  - `Index.vue` and `DailyDetailPage.vue` render actual/accrued attendance values inside `el-tooltip`;
  - tooltip content is multiline, safe Vue text interpolation, and not teleported so scoped styling applies.
- GREEN verification so far:
  - `node tests/overtime-night-utils.test.mjs` passed;
  - `node tests/attendance-display-columns.test.mjs` passed.
- Build verification:
  - `npm run build` passed, with existing `::v-deep` deprecation warnings and chunk-size warning.
- Local dev server:
  - `npm run dev -- --host 127.0.0.1` started Vite at `http://127.0.0.1:8080/`.
- Updated backend and frontend requirements/development docs for the hover calculation-process behavior.

# Progress: 闫倩 0.07 天请假时长舍入修正

## 2026-07-14
- Continued from backend handoff for 闫倩 `2026-06-26 17:30 -> 18:00`.
- Read required process skills and refreshed backend/frontend requirements and development docs.
- Confirmed business rule:
  - old value `0.56` came from stored `0.07天 * 8`;
  - correct business duration is 30 minutes / `0.50` hours by `beginTime/endTime`.
- Backend fix and tests were already implemented in the handoff; current remaining work is frontend approval-list display consistency.
- Added focused task plan and findings entries.
- Added RED frontend test for `duration=0.07`, `durationUnit=天`, `beginTime=2026-06-26 17:30:00`, `endTime=2026-06-26 18:00:00`.
  - RED output: actual `0.56小时(0.07天)`, expected `0.5小时(0.06天)`.
- Implemented frontend display correction in `approval-duration-utils.js`:
  - small fractional-day records compare `duration * 8` with the `beginTime/endTime` range;
  - if the difference is within `0.30` hours, display the range hours.
- Updated frontend docs:
  - `hr_web/docs/requirements.md`;
  - `hr_web/docs/development.md`.
- Verification:
  - `node tests/approval-duration-utils.test.mjs` passed.
  - `node tests/attendance-approval-dialog.test.mjs` passed.
  - `node tests/attendance-approval-api.test.mjs` passed.
  - `npm run build` passed with existing `::v-deep` and chunk-size warnings.
  - Backend targeted tests passed: parser prefers `durationInHour=0.5`, statistics uses time range for historical `0.07天`.
  - Backend approval/statistics regression passed: 75 tests, 0 failures.

## 2026-07-14 实际出勤 181 小时复核
- User asked whether 闫倩 actual attendance showing `181` means annual leave was not deducted.
- Queried local backend `queryPageList` for 闫倩 `2026-06`:
  - `expectedAttendanceDays=23`;
  - `actualAttendanceHours=181.00`;
  - `actualAttendanceRemark=扣除：调休0.50小时、年假8.00小时`;
  - `overtimeHours=44.92`.
- Queried approval list with correct filters `search=闫倩`, `times=[2026-06-01,2026-06-30]`:
  - 年假: `2026-06-13 08:00 -> 18:00`, `1天`;
  - 调休: `2026-06-26 17:30 -> 18:00`, `0.07天`;
  - 加班 approvals: `2026-06-03 2小时`, `2026-06-17 3.5小时`.
- Queried workweek month calendar:
  - `2026-06-13` is `dayType=1 / 上班`.
- Formula verified: `23 * 8 + (2 + 3.5) - (8 + 0.5) = 181`.
- Updated `docs/development.md` and `findings.md`; no production code change was needed.

# Progress: 张明行政体系日级自动加班排除修复

# Progress: 审批通过过滤与取消至统计复核

## 2026-07-14
- User clarified the desired rules:
  - approval fetch should only keep approved applications;
  - cancelled/refused/terminated/withdrawn/running and other non-approved instances should not be fetched;
  - an overtime form containing `预计加班时长` must still be skipped when the workflow result is not `agree`;
  - approvals marked `statisticsStatus=取消至统计` must not participate in actual-attendance/statistics calculations.
- Rechecked implementation:
  - `HrmAttendanceApprovalSyncServiceImpl#isApprovedProcessInstance(...)` requires `status=COMPLETED` and `result=agree`;
  - skipped instances are not saved and not added to retained IDs;
  - stale cleanup removes old snapshots not retained by a successful re-fetch;
  - `HrmOvertimeNightStatisticsServiceImpl` skips `取消至统计` approvals in overtime approval aggregation and deductible leave calculations.
- Verification:
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` passed, 96 tests, 0 failures.
- Updated `docs/requirements.md`, `docs/development.md`, `task_plan.md`, `findings.md`, and `progress.md`.
- No production code change was needed; current code already matches the clarified rules.

# Progress: 闫倩 51 小时加班审批来源排查

## 2026-07-14
- Refreshed backend requirements/development docs and current approval/statistics findings.
- Rechecked dev MySQL `hr_0003`:
  - 闫倩 employee `1831601326890434568` maps to DingTalk `userId=30352228121210270`;
  - local approval `3Pi7UG_qSyi9uJSL1zFsWQ04041782526625` is still present as `加班`, `2026-06-18 05:00 -> 2026-06-20 00:00`, `duration=51`, `durationUnit=小时`, `statisticsStatus=NULL`;
  - natural begin/end span is 43 hours, but the approval list displays stored `duration + durationUnit`.
- Performed read-only DingTalk `processinstance/get` verification for that instance:
  - `status=COMPLETED`, `result=refuse`;
  - form values include `预计加班时长=51`;
  - form reason explains two 17-hour days and extra Dragon Boat Festival multiplier, totaling 51 hours by the applicant's business calculation.
- Conclusion:
  - 51 hours came from the DingTalk form duration field, not from local time-range calculation;
  - because the instance is refused, current sync code should not save it; its presence is an old local snapshot from before approved-instance filtering;
  - no code or database writes were performed in this investigation.

## 2026-07-14 应计出勤扣日级自动加班修复
- 用户追问张明应计出勤为什么有减扣。
- Root cause:
  - actual attendance already uses administrative approval overtime only;
  - accrued attendance still subtracted display `overtimeHours`;
  - for 张明 this was `184.00 - 47.72 = 136.28`.
- TDD RED:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee test` failed as expected;
  - start-statistics got `3.00` accrued hours instead of `8.00`;
  - queryPageList got `136.28` accrued hours instead of `184.00`.
- Implemented fix:
  - start-statistics passes `actualAttendanceOvertimeHours` into accrued-attendance calculation;
  - query fallback resolves accrued-attendance overtime with the same administrative/non-administrative split as actual attendance.
- Verification:
  - same targeted command passed, 2 tests, 0 failures;
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 46 tests, 0 failures;
  - local API 张明 `2026-06` returned `actualAttendanceHours=184.00`, `accruedAttendanceHours=184.00`, `overtimeHours=47.72`.

## 2026-07-14
- Re-read backend requirements/development docs and current planning records for the 张明 `2026-06` actual attendance correction.
- Confirmed current implementation in `HrmOvertimeNightStatisticsServiceImpl` separates:
  - display monthly overtime: `monthlyOvertimeHours`;
  - actual-attendance overtime: `actualAttendanceOvertimeHours`.
- Current rule:
  - administrative employees use local DingTalk approval overtime only for actual attendance;
  - non-administrative employees keep using monthly overtime summary;
  - the monthly `overtimeHours` display still includes daily auto overtime.
- Verified tests currently cover both paths:
  - start statistics keeps administrative daily auto overtime visible but does not add it to actual attendance;
  - query list recalculates old persisted rows so 张明 with `47.72` daily overtime returns `184.00` actual attendance hours;
  - approval overtime still adds to actual attendance, preserving 李明明 `160.00` actual / `184.00` accrued.
- Fresh verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldAddOvertimeApprovalToActualAttendance test` passed, 3 tests, 0 failures.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 46 tests, 0 failures.
  - Local API `queryPageList` for 张明 `2026-06` returned `actualAttendanceHours=184.00`, `actualAttendanceDays=23`, `accruedAttendanceHours=184.00`, `overtimeHours=47.72`.
  - Local API `queryPageList` for 李明明 `2026-06` returned `actualAttendanceHours=160.00`, `accruedAttendanceHours=184.00`, `overtimeHours=8.00`.
- Updated `docs/development.md`, `task_plan.md`, `findings.md`, and `progress.md` to mark the earlier `231.72` conclusion as historical and superseded.

# Progress: 李明明实际出勤与应计出勤小时差异修复

## 2026-07-14
- Continued from previous investigation for 李明明 `2026-06` actual/accrued attendance mismatch.
- Re-read backend docs, task plan, findings, and progress under the project documentation workflow.
- Confirmed current code path now passes monthly `overtimeHours` into actual-attendance calculation:
  - start statistics: `calculateMonthlyDetails(...)` sums monthly overtime rows and calls `calculateMonthlyAttendanceDays(..., monthlyOvertimeHours, monthlyOvertimeHours)`;
  - query fallback: `resolveActualAttendanceForQuery(...)` receives summary/monthly overtime and recalculates stale persisted rows.
- Removed obsolete private methods `shouldIncludeOvertimeInActualAttendance(...)` and `hasIncludedOvertimeApproval(...)` to avoid reintroducing the deprecated “administrative/overtime-approval does not add overtime” rule.
- Updated backend `docs/requirements.md` and `docs/development.md`:
  - current actual attendance formula is `应出勤小时 + 月度加班小时 - 事假/病假/调休/年假扣减小时`;
  - early rule excluding overtime for administrative employees or local overtime approvals is marked deprecated;
  - 李明明 `2026-06` calibration is documented as `184 / 160 / 184` hours.
- Executed `docs/sql/2026-07-14_overtime_night_accrued_attendance_hours.sql` against dev MySQL `153.0.237.98`; script completed successfully and skipped existing columns idempotently.
- Database verification:
  - `information_schema.columns` shows `accrued_attendance_hours` exists in `hr_0001` through `hr_0005`;
  - 李明明 annual leave approval `gAEi7v06SEmawOi-H9JlHQ04041781396068` now has `duration=4`, `durationUnit=天`;
  - 李明明 2026-06 statistics rows now aggregate to expected days `23`, actual days `20`, overtime `8.00`, accrued attendance `184.00`.
- Verification:
  - `mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest,HrmOvertimeNightStatisticsServiceImplTest test` passed: 52 tests, 0 failures.
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalProcessInstanceParserTest,HrmOvertimeNightStatisticsServiceImplTest test` passed: 102 tests, 0 failures.
  - Frontend tests passed: `node tests/overtime-night-utils.test.mjs`, `node tests/attendance-display-columns.test.mjs`.
  - `npm run build` passed with existing `::v-deep` deprecation warnings and chunk-size warning.
  - Local API `POST /hrsystem/hrmOvertimeNightStatistics/queryPageList` with `month=2026-06, keyword=李明明` returned `expectedAttendanceDays=23`, `actualAttendanceHours=160.00`, `actualAttendanceDays=20`, `accruedAttendanceHours=184.00`, `overtimeHours=8.00`.
  - Local API `queryEmployeeMonthlyDetail` and `queryDailyDetailPageList` returned the same `actualAttendanceHours=160.00` and `accruedAttendanceHours=184.00`.

# Progress: 加班/夜班统计应计出勤计算项

## 2026-07-14
- Read required skills and refreshed backend/frontend docs for overtime/night actual-attendance behavior.
- Confirmed current documented display order: `姓名 -> 应出勤时间 -> 实际出勤小时(天数) -> 日列 -> 备注`.
- Started focused task plan for the new accrued/accountable attendance calculation.
- Working assumption: formula uses hours: `accruedAttendanceHours = actualAttendanceHours + compensatoryLeaveHours + annualLeaveHours - overtimeHours + sickLeaveHours`.
- Added RED backend assertions for `accruedAttendanceHours` in start-statistics response and saved detail rows; first run failed at compilation because the VO/entity getters did not exist.
- Added RED frontend assertions for the new “应计出勤小时(天数)” column and `accruedAttendanceSummaryValue`; first runs failed as expected.
- Implemented backend changes:
  - added `accruedAttendanceHours` to `HrmOvertimeNightStatisticsDetail` and three overtime/night response VOs;
  - calculated and persisted monthly accrued attendance hours during start-statistics;
  - exposed persisted values through summary, monthly detail and daily detail responses, with formula fallback for old rows;
  - added `docs/sql/2026-07-14_overtime_night_accrued_attendance_hours.sql`.
- Implemented frontend changes:
  - added `accruedAttendanceSummaryValue` to `overtime-night-utils.js`;
  - inserted “应计出勤小时(天数)” after “实际出勤小时(天数)” in `Index.vue` and `DailyDetailPage.vue`;
  - propagated monthly `accruedAttendanceHours` into single-person daily matrix rows.
- Verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeductPersonalSickCompensatoryAndAnnualLeaveFromExpectedAttendanceDays+startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployeeWithoutOvertimeApproval test` passed, 2 tests, 0 failures.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 44 tests, 0 failures.
  - `node tests/overtime-night-utils.test.mjs` passed.
  - `node tests/attendance-display-columns.test.mjs` passed.
  - `npm run build` passed, with existing `::v-deep` and chunk-size warnings.

# Progress: 加班/夜班统计实际出勤年假扣减与列顺序调整

## 2026-07-14
- Read required skills: `using-superpowers`, `project-modification`, `brainstorming`, `test-driven-development`, `planning-with-files`, `ui-ux-pro-max`, `verification-before-completion`.
- Read backend docs: `docs/requirements.md`, `docs/development.md`.
- Read frontend docs: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`, `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`.
- Confirmed current backend written rule only deducts `事假/病假/调休`; user request adds `年假`.
- Confirmed current frontend actual-attendance matrix column order is `姓名 -> 小时(天数)汇总 -> 应出勤时间 -> 日列 -> 备注`; user request changes it to `姓名 -> 应出勤时间 -> 实际出勤小时(天数) -> 日列 -> 备注`.
- Started focused task plan and findings entries for this change.
- Added RED backend test by extending the actual-attendance deduction case with a `年假` approval; first run failed with `expected:<20> but was:<21>`.
- Added RED frontend assertions requiring “应出勤时间” before “实际出勤小时(天数)” and rejecting old “小时(天数)汇总”; first run failed on the old column order.
- Implemented backend fix in `HrmOvertimeNightStatisticsServiceImpl#resolveActualAttendanceDeductibleType(...)` by adding `年假`.
- Implemented frontend fix in:
  - `hr_web/src/views/hrm/attendance/overtime-night/Index.vue`;
  - `hr_web/src/views/hrm/attendance/overtime-night/DailyDetailPage.vue`.
- Updated backend docs: `docs/requirements.md`, `docs/development.md`.
- Updated frontend docs: `hr_web/docs/requirements.md`, `hr_web/docs/development.md`.
- Verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeductPersonalSickCompensatoryAndAnnualLeaveFromExpectedAttendanceDays test` passed, 1 test, 0 failures.
  - `node tests/attendance-display-columns.test.mjs` passed.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 44 tests, 0 failures.
  - `node tests/overtime-night-utils.test.mjs` passed.
  - `npm run build` passed, with existing `::v-deep` deprecation warnings and chunk-size warning.
- Frontend dev server started with `npm run dev -- --host 127.0.0.1` at `http://127.0.0.1:8080/`.

# Progress: 审批数据管理员模板失败回退修复

## 2026-07-14
- Read required skills: `using-superpowers`, `project-modification`, `planning-with-files`, `systematic-debugging`, `test-driven-development`, `verification-before-completion`.
- Read backend project docs: `docs/requirements.md`, `docs/development.md`.
- Confirmed documented requirement: `adminUserId` is the DingTalk administrator used only for administrator template pre-query; fallback must parse process codes by the actual employee visible templates.
- Created focused task plan and initial findings for the fallback regression.
- Located the implementation in `HrmAttendanceApprovalSyncServiceImpl#resolveProcessTemplates(...)`; no second approval-template implementation exists in the old `AttendanceRecordManager` path.
- Added RED test `resolveProcessCodes_shouldFallbackByEmployeeUserWhenManageableTemplateQueryFails`; first run failed because `errcode=500001` from the admin template API was thrown instead of falling back.
- Implemented the minimal fix:
  - any administrator template query exception now falls back to `fetchUserVisibleProcessTemplates(token, employeeUserId)`;
  - fallback log now includes both `adminUserId` and `employeeUserId`.
- GREEN verification so far:
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` passed, 7 tests, 0 failures.
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest test` passed, 20 tests, 0 failures.
- Updated backend `docs/requirements.md` and `docs/development.md` for the administrator-template fallback rule.
- Final approval regression passed:
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` passed, 50 tests, 0 failures.

# Progress: 加班/夜班统计实际出勤加入加班时间

## 2026-07-13
- Read required skills: `using-superpowers`, `project-modification`, `brainstorming`, `test-driven-development`, `planning-with-files`.
- Read backend project docs: `docs/requirements.md`, `docs/development.md`.
- Confirmed current documented rule: actual attendance uses expected attendance minus deductible `事假/病假/调休` when expected attendance is positive.
- Recorded new target rule: actual attendance should use expected attendance plus overtime hours, then subtract deductible leave hours.

# Progress: 加班/夜班统计实际出勤加入加班时间

## 2026-07-13
- Read required skills and backend project docs for this modification.
- Confirmed current written requirement used `应出勤 - 事假/病假/调休扣减` and needed to become `应出勤 + 加班 - 事假/病假/调休扣减`.
- Located backend implementation in `HrmOvertimeNightStatisticsServiceImpl` and regression tests in `HrmOvertimeNightStatisticsServiceImplTest`.
- Added RED test `startStatistics_shouldAddOvertimeHoursBeforeDeductingLeaveFromExpectedAttendanceDays`:
  - administrative employee with `expectedAttendanceDays=1`;
  - local DingTalk overtime approval contributes `8.00` overtime hours;
  - expected actual attendance becomes `16.00` hours / `2` days.
- RED verification failed as expected: old implementation returned `actualAttendanceDays=1`.
- Implemented backend fix:
  - `calculateMonthlyDetails(...)` now recalculates monthly attendance after daily overtime rows are built and applies monthly overtime hours to all rows;
  - query paths now pass monthly overtime hours into actual attendance calculation;
  - daily detail overview aggregates monthly overtime by employee before row conversion;
  - actual-attendance cache key includes overtime hours.
- GREEN verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldAddOvertimeHoursBeforeDeductingLeaveFromExpectedAttendanceDays test` passed, 1 test, 0 failures.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 39 tests, 0 failures.
- Updated backend `docs/requirements.md` and `docs/development.md`, plus planning/findings/progress records.

# Progress: 审批数据闫倩调休同步与子类型手工修改

## 2026-07-13
- Read required skills: `using-superpowers`, `project-modification`, `planning-with-files`, `brainstorming`, `systematic-debugging`, `test-driven-development`, `verification-before-completion`.
- Read backend project docs: `docs/requirements.md`, `docs/development.md`.
- Created focused task plan and initial findings for the approval-data sync anomaly and editable subtype requirement.
- Read frontend project docs: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`, `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`.
- Investigated 闫倩 `2026-06` approval snapshots in `hr_0003`:
  - confirmed employee `1831601326890434568` maps to DingTalk `userId=30352228121210270`;
  - confirmed local `tbattendanceapprove` contains three `请假/调休` rows on `06-04`、`06-13`、`06-26`;
  - confirmed `06-04` was synced into the local table and is not generated by the list query.
- Implemented backend subtype editing support:
  - added `UpdateAttendanceApprovalSubtypeBO`;
  - added `querySubtypeOptions` and `updateSubtype` service/controller endpoints;
  - added repository support for distinct subtype options and retained-ID stale cleanup;
  - changed manual approval re-fetch to preserve an existing local `subType` for the same `procInstId`.
- Implemented frontend inline subtype editing:
  - added `queryAttendanceApprovalSubtypeOptions` and `updateAttendanceApprovalSubtype` API helpers;
  - changed the “审批子类型” table cell to clickable inline `el-select`;
  - merged default and backend-loaded subtype options, with success/failure row handling.
- Updated backend and frontend requirements/development docs for the new editable subtype rule and re-fetch preservation behavior.
- Verification:
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` passed, 37 tests, 0 failures.
  - `node tests/attendance-approval-api.test.mjs` passed.
  - `node tests/attendance-approval-dialog.test.mjs` passed.
  - `node tests/attendance-approval-fetch-utils.test.mjs` passed.
  - `npm run build` passed, with existing `::v-deep` and chunk-size warnings.
- Frontend dev server started at `http://localhost:8081/` because port `8080` was already in use.
- Fixed runtime error `Modifying queries can only use void or int/Integer as return type!`:
  - Added RED test `approvalRepositoryModifyingDeleteWithRetainedIds_shouldUseSpringSupportedReturnType`; first run failed with `expected:<int> but was:<long>`.
  - Changed `tbattendanceapproveRepository#deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(...)` return type from `long` to `int`.
  - Updated workflow test proxies to return `Integer` values for that method.
  - GREEN single-test verification: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#approvalRepositoryModifyingDeleteWithRetainedIds_shouldUseSpringSupportedReturnType test` passed.
  - GREEN approval-data regression: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` passed, 38 tests, 0 failures.

# Progress: 加班/夜班统计出勤区域与行政应出勤修复

## 2026-07-12
- Read required skills: `using-superpowers`, `project-modification`, `brainstorming`, `systematic-debugging`, `test-driven-development`, `planning-with-files`.
- Read backend docs: `docs/requirements.md`, `docs/development.md`.
- Read frontend docs: `/Users/jiangyongming/Project/hr/hr_web/docs/requirements.md`, `/Users/jiangyongming/Project/hr/hr_web/docs/development.md`.
- Confirmed requirement mismatch:
  - Old frontend requirement placed expected/actual attendance columns inside both daily overtime and daily night-shift matrices.
  - New user requirement asks for expected/actual attendance to be shown in a separate area, while both daily matrices remove those two columns.
- Initial source search confirmed relevant frontend files and tests:
  - `src/views/hrm/attendance/overtime-night/Index.vue`
  - `src/views/hrm/attendance/overtime-night/DailyDetailPage.vue`
  - `src/views/hrm/attendance/overtime-night/overtime-night-utils.js`
  - `tests/attendance-display-columns.test.mjs`
- Initial backend search confirmed relevant service/test:
  - `src/main/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImpl.java`
  - `src/test/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImplTest.java`
- RED tests added and run:
  - `node tests/attendance-display-columns.test.mjs` failed because “显示所有”加班表仍渲染应/实际出勤列。
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` failed 3 new assertions:
    - 汇总列表旧明细缺出勤字段时行政员工应出勤返回 `0`。
    - 单人明细旧明细缺出勤字段时行政员工应出勤返回 `0`。
    - 显示所有每日明细旧明细缺出勤字段时行政员工应出勤返回 `null`。
- Implemented frontend changes:
  - Removed `expectedAttendanceDaysText` / `actualAttendanceDaysText` columns from single-person overtime/night matrix tables.
  - Removed the same two columns from view-all overtime/night matrix tables.
  - Renamed the independent attendance area title to “出勤时间统计”.
- Implemented backend changes:
  - `queryPageList` fills missing/zero expected attendance days for administrative employees from workweek month calendar.
  - `queryEmployeeMonthlyDetail` now loads employee affiliation context and fills missing administrative expected attendance days.
  - `queryDailyDetailPageList` now loads employee affiliation context by detail employee IDs and fills missing administrative expected attendance days per row.
- Verification:
  - `node tests/attendance-display-columns.test.mjs` passed.
  - `node tests/overtime-night-utils.test.mjs` passed.
  - `npm run build` passed, with existing `::v-deep` and chunk-size warnings.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` passed: 29 tests, 0 failures.
  - Frontend dev server started at `http://localhost:8080/`.
- Updated docs:
  - Backend `docs/requirements.md` and `docs/development.md`.
  - Frontend `hr_web/docs/requirements.md` and `hr_web/docs/development.md`.
# Progress: 李凤皇实际出勤天数调休折算排查

## 2026-07-12
- Read required skills: `using-superpowers`, `project-modification`, `systematic-debugging`, `test-driven-development`, `planning-with-files`, `verification-before-completion`.
- Read backend `docs/requirements.md` and `docs/development.md`.
- Confirmed current written implementation notes say actual attendance days are based only on `hrm_attendance_clock` and 480-minute clock span per business date.
- Created current task plan and findings entries for the Li Fenghuang compensatory leave scenario.
- Traced root cause:
  - `calculateActualAttendanceDays(...)` only counted days whose clock span reached 480 minutes.
  - `buildAttendanceApprovalMap(...)` only used overtime approvals; `请假/调休` approvals were filtered out of actual attendance days.
- Added RED test `startStatistics_shouldIncludeCompensatoryLeaveHoursWhenSummarizingActualAttendanceDays`; first run failed with `expected:<25> but was:<24>`.
- Implemented fix:
  - Monthly actual attendance now sums effective clock minutes, capped at 480 minutes per work date.
  - Compensatory leave approvals from `tbattendanceapprove` are converted to minutes and added before dividing by 480.
- GREEN verification: `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` passed, 30 tests, 0 failures.
- Final verification after documentation updates: `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` passed, 30 tests, 0 failures.

# Progress: 李凤皇本地查看显示 0(0) 排查

## 2026-07-12
- Confirmed frontend `0(0)` comes from backend `actualAttendanceDays=0`; frontend displays `actualAttendanceDays * 8` as `小时(天数)`.
- Added RED tests:
  - `queryEmployeeMonthlyDetail_shouldFillActualAttendanceDaysFromClockAndCompensatoryLeaveWhenPersistedRowsAreZero`;
  - `queryDailyDetailPageList_shouldFillActualAttendanceDaysFromClockAndCompensatoryLeaveWhenPersistedRowsAreZero`.
- RED verification failed as expected: both paths returned `0` instead of `25`.
- Implemented backend query fallback in `HrmOvertimeNightStatisticsServiceImpl`:
  - `resolveActualAttendanceDaysForQuery(...)`;
  - `calculateActualAttendanceDaysForQuery(...)`;
  - employee-month cache passed through page, monthly detail, and daily detail query paths.
- GREEN verification: `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 32 tests, 0 failures.
- Runtime follow-up found the `2026-06` live local result was a stale positive value: `actualAttendanceDays=24`, not only `0`.
- Added RED tests:
  - `queryPageList_shouldRefreshActualAttendanceDaysWhenPersistedRowsAreStalePositiveValue`;
  - `queryEmployeeMonthlyDetail_shouldUseApprovalBeginEndWhenCompensatoryLeaveDurationIsBlank`.
- RED verification failed as expected: both new paths returned `24` instead of `25`.
- Implemented backend fixes:
  - query fallback now returns the larger value between persisted `actualAttendanceDays` and the employee-month recalculated value;
  - `resolveAttendanceApprovalHours(...)` falls back to `beginTime/endTime` when `duration/durationUnit` is blank or unparsable.
- GREEN verification: `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 34 tests, 0 failures.
- Runtime verification on local `9080` for 李凤皇 `2026-06`:
  - `queryPageList`: `actualAttendanceDays=25`;
  - `queryEmployeeMonthlyDetail`: `actualAttendanceDays=25`, `dailyCount=25`;
  - `queryDailyDetailPageList`: first page rows all carry `actualAttendanceDays=25`, total `25`.

# Progress: 张明实际出勤 160 小时来源排查

## 2026-07-12
- Read required skills and project docs for the backend project.
- Confirmed documented display rule: actual attendance hours are derived from backend `actualAttendanceDays * 8`.
- Created a focused investigation plan to trace why 张明 displays `160` hours.
- Located 张明 in `hr_0003.hrm_employee`: `employee_id=1831601326890434564`, `job_number=TYNG-012`, administrative affiliation.
- Confirmed `hr_0003.hrm_overtime_night_statistics_detail` for `2026-06` stores `expected_attendance_days=23` and `actual_attendance_days=20`.
- Recomputed the current backend actual-attendance algorithm from `hrm_attendance_clock`:
  - daily capped effective minutes sum to `9829`;
  - no matching `调休` approvals are present;
  - `9829 / 480` truncates to `20` actual attendance days;
  - display hours are `20 * 8 = 160`.
- Fresh verification SQL returned: `expected_days=23`, `actual_days=20`, `effective_minutes=9829`, `effective_hours=163.82`, `calculated_days=20`, `displayed_hours=160`, `compensatory_count=0`.
- Updated `docs/development.md` and `docs/requirements.md` with the investigation result and the open business question around whether `外出` approvals or manual audit hours should count toward actual attendance.

# Progress: 实际出勤扣减口径与备注列改造

## 2026-07-12
- Read required skills: `using-superpowers`, `project-modification`, `brainstorming`, `systematic-debugging`, `test-driven-development`, `planning-with-files`, `ui-ux-pro-max`.
- Read backend docs and frontend docs.
- Generated UI guidance for an enterprise HR admin data table; decision is to keep existing Element Plus dense table style and only add stable columns.
- Started a focused task plan for the new deduction logic and actual-attendance matrix columns.
- Added backend RED tests:
  - `startStatistics_shouldDeductOnlyPersonalSickAndCompensatoryLeaveFromExpectedAttendanceDays`;
  - `queryPageList_shouldUseExpectedAttendanceWhenOnlyNonDeductibleApprovalExists`.
- Added frontend RED assertions for actual-attendance matrix expected-attendance and remark columns.
- RED verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` failed at test compilation because `QueryOvertimeNightStatisticsPageVO#getActualAttendanceRemark()` does not exist yet.
  - `node tests/overtime-night-utils.test.mjs` failed because `expectedAttendanceSummaryValue` is missing.
  - `node tests/attendance-display-columns.test.mjs` failed because actual-attendance tables do not render `应出勤时间` / `备注`.
- Implemented backend changes:
  - Added `actualAttendanceHours` and `actualAttendanceRemark` to the overtime/night statistics response VOs.
  - Changed administrative actual-attendance calculation to use expected attendance hours minus deductible approval hours.
  - Deductible approvals are only `事假`、`病假`、`调休`; `外出` and other approval types are ignored for deduction and remark.
  - Query fallback cache now includes expected attendance days in the key, so stale or missing expected-attendance contexts do not share an actual-attendance result.
  - Kept clock-based fallback only when expected attendance days are not positive.
- Implemented frontend changes:
  - Added `应出勤时间` immediately after `小时(天数)汇总` in the actual-attendance matrix for both single-person detail dialog and view-all page.
  - Added final `备注` column with overflow tooltip.
  - Updated matrix utilities to prefer `actualAttendanceHours` for partial-day deductions and fall back to `actualAttendanceDays * 8` for old responses.
- Updated old tests that expected “调休补足” so they now verify “调休扣减”.
- GREEN verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed: 36 tests, 0 failures.
  - `node tests/overtime-night-utils.test.mjs` passed.
  - `node tests/attendance-display-columns.test.mjs` passed.
  - `npm run build` passed with existing `::v-deep` and chunk-size warnings.
- Updated backend and frontend requirements/development docs for the new deduction rule and matrix columns.

# Progress: 闫倩实际出勤扣两天与备注缺失排查

## 2026-07-13
- Read required skills and refreshed backend/frontend requirements and development docs.
- Located 闫倩 in `hr_0003.hrm_employee`: `employee_id=1831601326890434568`，行政体系；`tbattendanceuser.userId=30352228121210270`。
- Confirmed `2026-06` statistics detail stores `expected_attendance_days=23` and `actual_attendance_days=21`.
- Queried `tbattendanceapprove` and found 3 deductible `请假/调休` approvals in `2026-06`; with blank duration fields they are calculated from `beginTime/endTime` as `14.78` hours total.
- Recomputed the expected result: `23 * 8 = 184` expected hours, `184 - 14.78 = 169.22` actual hours, and compatible `actualAttendanceDays=floor(169.22/8)=21`.
- Verified local backend APIs all return `actualAttendanceHours=169.22` and `actualAttendanceRemark=扣除：调休14.78小时` for 闫倩:
  - `queryPageList`;
  - `queryEmployeeMonthlyDetail`;
  - `queryDailyDetailPageList`.
- Found frontend root cause in `Index.vue#buildDetailMatrixRows`: single-person detail dialog expanded `dailyDetails` without carrying `actualAttendanceHours/actualAttendanceRemark`.
- Added RED assertion in `tests/attendance-display-columns.test.mjs`; it failed as expected before implementation.
- Fixed `Index.vue` so daily matrix rows inherit monthly actual attendance hours and remark when day-level values are absent.
- Verification:
  - `node tests/attendance-display-columns.test.mjs` passed.
  - `node tests/overtime-night-utils.test.mjs` passed.
  - `npm run build` passed with existing `::v-deep` and chunk-size warnings.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 36 tests, 0 failures.

# Progress: 张明实际出勤多出 47.72 小时排查（历史口径，已被替代）

## 2026-07-14
- Read required skills: `using-superpowers`, `project-modification`, `planning-with-files`, `systematic-debugging`.
- Read backend project docs: `docs/requirements.md`, `docs/development.md`.
- At that point, the documented rule still allowed administrative actual attendance to use `expectedAttendanceDays * 8 + overtimeHours - deductibleLeaveHours`; this was later replaced by the current approval-overtime-only rule for administrative employees.
- Started focused plan and findings entries for the `47.72` hour delta.
- Read `HrmOvertimeNightStatisticsServiceImpl` actual-attendance query and calculation path:
  - `buildSummary(...)` sums daily `overtimeHours`;
  - `resolveActualAttendanceForQuery(...)` passes monthly overtime into `calculateActualAttendanceFromExpected(...)`;
  - `calculateActualAttendanceFromExpected(...)` calculates `expectedHours + overtimeHours - deductionHours`.
- Queried dev MySQL `hr_0003` for 张明 `2026-06`:
  - employee `1831601326890434564`, `TYNG-012`, administrative affiliation, DingTalk `userId=1853391326781038`;
  - statistics detail summary: `expected_attendance_days=23`, `sum(overtime_hours)=47.72`, `actual_attendance_days=28`;
  - approvals: only two `外出` rows, no `事假/病假/调休`, deductible hours `0`.
- Recalculated the then-current formula: `23 * 8 + 47.72 - 0 = 231.72` actual attendance hours; `floor(231.72 / 8) = 28` days.
- This conclusion is retained as historical evidence of why the old result appeared; the current rule now excludes that `47.72` daily auto overtime from administrative actual attendance.

# Progress: 行政体系无加班审批时实际出勤不叠加加班汇总

## 2026-07-14
- Read required skills: `using-superpowers`, `project-modification`, `brainstorming`, `test-driven-development`, `planning-with-files`, `verification-before-completion`.
- Read backend requirements/development docs around overtime/night actual attendance.
- Current rule to change: no included overtime approval still allowed `overtimeHours` into actual attendance; new rule excludes it for administrative employees as well.
- Planned tests:
  - start-statistics path should keep `overtimeHours` visible but return actual attendance based only on expected attendance for administrative employees with no overtime approval;
  - query path should do the same when persisted daily details already contain overtime.
- RED verification: `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployeeWithoutOvertimeApproval+queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployeeWithoutOvertimeApproval test` failed as expected:
  - start-statistics returned `13.00` actual attendance hours instead of `8.00`;
  - query returned `28` actual attendance days instead of `23`.
- Early implementation added `shouldIncludeOvertimeInActualAttendance(...)` in `HrmOvertimeNightStatisticsServiceImpl` and reused it in start-statistics and query paths; this was later superseded by explicit `actualAttendanceOvertimeHours` selection.
- GREEN verification:
  - targeted command passed, 2 tests, 0 failures;
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 41 tests, 0 failures.
- Approval/statistics regression passed:
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` passed, 91 tests, 0 failures.
- Updated backend requirements/development docs, task plan, findings, and progress records.
- Follow-up implementation superseded the temporary `shouldIncludeOvertimeInActualAttendance(...)` / `hasIncludedOvertimeApproval(...)` approach with explicit `actualAttendanceOvertimeHours` selection: administrative employees receive approval overtime only, while non-administrative employees receive monthly overtime.

# Progress: 加班审批存在时实际出勤不叠加加班汇总

## 2026-07-14
- Read required skills: `project-modification`, `brainstorming`, `test-driven-development`, `planning-with-files`.
- Read backend requirements/development docs and current task/finding records.
- Assumption for implementation: when current employee/month has included local overtime approval, keep `overtimeHours` summary visible but pass `0` overtime into actual-attendance calculation.
- Added RED tests:
  - `startStatistics_shouldNotAddDailyOvertimeToActualAttendanceWhenOvertimeApprovalExists`;
  - `queryPageList_shouldNotAddDailyOvertimeToActualAttendanceWhenOvertimeApprovalExists`;
  - a no-approval old-rule regression, later replaced by the administrative-system exception.
- RED verification: the targeted approval-present command failed 2 assertions as expected; both approval-present paths returned `2` actual attendance days instead of `1`. Its no-approval old-rule regression was later replaced by the administrative-system exception.
- Implemented an early backend fix in `HrmOvertimeNightStatisticsServiceImpl`:
  - start-statistics monthly actual attendance used `0` overtime when `buildAttendanceApprovalMap(...)` had included overtime approval;
  - query fallback used `hasIncludedOvertimeApproval(...)` before calling `calculateActualAttendanceFromExpected(...)`;
  - cancelled approvals did not trigger the exclusion.
  This early rule was later superseded because approval overtime must still count for administrative employees such as 李明明.
- GREEN verification:
  - targeted approval-present command passed, 3 tests, 0 failures.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 41 tests, 0 failures.
- Approval/statistics regression:
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` passed, 91 tests, 0 failures.
- Updated backend requirements/development docs, task plan, and findings.

# Progress: 吴晓霞加班审批异常与统计操作列

## 2026-07-13
- Read required skills: `using-superpowers`, `project-modification`, `planning-with-files`, `brainstorming`, `systematic-debugging`, `test-driven-development`, `verification-before-completion`.
- Read backend project docs: `docs/requirements.md`, `docs/development.md`.
- Started focused plan for 吴晓霞 approval-data mismatch and the new approval-data statistics operation column.
- Initial assumptions recorded: operation toggles local participation in statistics and must not call DingTalk or delete local approval snapshots.
- Read frontend project docs and approval page implementation in `hr_web`.
- Queried `hr_0003` local data:
  - 吴晓霞 `employee_id=1831601326890434565` maps to DingTalk `userId=262756571521566047`;
  - local `2026-06` overtime approvals are 3 rows, including `NzMSp1MiT5GkPu20k2IsPA04041781527092` as `2026-06-15 06:05:00 -> 07:05:00`;
  - `tbattendanceapprove` currently has no statistics status column.
- Added RED tests:
  - backend approval statistics status BO/service/controller/VO/mapper contract;
  - overtime/night statistics ignores approvals with `statisticsStatus=取消至统计`;
  - all-employee approval re-fetch cleans stale snapshots;
  - workflow process codes are resolved per employee when visible templates differ;
  - frontend approval API and operation column.
- RED verification:
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmOvertimeNightStatisticsServiceImplTest test` failed on missing statistics status BO/getters and the two sync defects.
  - `node tests/attendance-approval-api.test.mjs` failed because `updateAttendanceApprovalStatisticsStatus` is not exported.
  - `node tests/attendance-approval-dialog.test.mjs` failed because the “操作” column is missing.
- Implemented backend changes:
  - added `statisticsStatus` to `tbattendanceapprove` and `QueryAttendanceApprovalPageVO`;
  - added `UpdateAttendanceApprovalStatisticsStatusBO`;
  - added `POST /hrmAttendanceApproval/updateStatisticsStatus`;
  - exposed `a.statisticsStatus as statisticsStatus` from `HrmAttendanceApprovalMapper.xml`;
  - skipped `statisticsStatus=取消至统计` approvals in overtime approval aggregation and actual-attendance leave deduction;
  - changed approval fetch to resolve process codes per employee;
  - changed all-employee approval fetch to clean stale local snapshots using resolved target users.
- Added SQL migration `docs/sql/2026-07-13_tbattendanceapprove_statistics_status.sql` for `hr_0001` through `hr_0005`.
- Implemented frontend changes in `hr_web`:
  - added `updateAttendanceApprovalStatisticsStatus` API helper;
  - added fixed-right “操作” column to the approval data table;
  - rows with `statisticsStatus=取消至统计` show “添加至统计”, all other rows show “取消至统计”;
  - added per-row loading, success feedback, and failure rollback.
- GREEN verification:
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` passed, 81 tests, 0 failures.
  - `node tests/attendance-approval-api.test.mjs` passed.
  - `node tests/attendance-approval-dialog.test.mjs` passed.
  - `node tests/attendance-approval-fetch-utils.test.mjs` passed.
  - `npm run build` passed with existing `::v-deep` and chunk-size warnings.
- Updated backend and frontend requirements/development docs for the approval statistics operation column, local-only status update, statistics filtering, and DB migration requirement.
- Executed `docs/sql/2026-07-13_tbattendanceapprove_statistics_status.sql` on dev MySQL `153.0.237.98` at `2026-07-13 16:17 CST`.
- SQL verification:
  - `information_schema.columns` returned `existing_columns=5` for `hr_0001` through `hr_0005`;
  - each tenant now has `tbattendanceapprove.statisticsStatus varchar(20) NULL`;
  - direct query `SELECT id, statisticsStatus FROM hr_0003.tbattendanceapprove LIMIT 1` succeeded and returned `statisticsStatus=NULL` for an existing row.
- Follow-up for “用户不存在” during approval fetch:
  - traced the error to `process.listbyuserid` for an employee DingTalk `userId` that no longer exists;
  - added RED tests for skipping invalid users in all-employee fetch and returning a clear message when every selected user is invalid;
  - implemented invalid-user detection in `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData`;
  - skipped invalid users are not marked complete and are excluded from stale snapshot cleanup;
  - GREEN single-case verification passed: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched test`;
  - GREEN approval regression passed: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`, 83 tests, 0 failures.
- Follow-up for “single employee fetch cannot get data, all employees can”:
  - traced the inconsistency to duplicate `tbattendanceuser` mappings for the same employee and unordered repository results;
  - updated target-user resolution to prefer the newest mapping by `createTime`, then `id`;
  - added `fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee`;
  - targeted verification passed: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched+fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees test`, 3 tests, 0 failures.
  - broader approval/statistics regression passed: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`, 84 tests, 0 failures.
- Follow-up for resolving DingTalk IDs from employee name + mobile:
  - updated approval fetch so single, batch, and all-employee fetch paths resolve users from `hrm_employee` with access to the DingTalk token;
  - employee table `dingtalk_user_id` is now preferred over old `tbattendanceuser` mappings for fetch target resolution;
  - when employee table has no DingTalk ID, the service calls DingTalk on-job roster APIs and strictly matches by normalized employee name + mobile;
  - unique matches are written back to `hrm_employee.dingtalk_user_id`, then saved into `tbattendanceuser`, before approval fetch begins;
  - DingTalk user-not-found during process-code or instance-list fetch triggers the same name + mobile refresh path and retries if the userId changes;
  - stale approval cleanup now collects all historical `tbattendanceuser.userId` values for completed employees so old snapshots from old DingTalk IDs are removed after the new fetch;
  - added workflow tests:
    - `fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing`;
    - `fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt`;
  - targeted verification passed: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test`, 2 tests, 0 failures.
  - approval/statistics regression passed: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`, 86 tests, 0 failures.
- Follow-up investigation for “吴晓霞 still says user not found”:
  - checked `hr_0003.hrm_employee` and `tbattendanceuser`: 吴晓霞 has mobile `13268968337` and DingTalk userId `262756571521566047` in both employee and attendance mapping tables;
  - confirmed local running backend on port `9080` loads `target/classes`, and `HrmAttendanceApprovalSyncServiceImpl.class` was compiled after the new logic;
  - reproduced local fetch with `month=2026-06`, `employeeIds=[1831601326890434565]`, `approvalTypes=["overtime"]`;
  - local response succeeded with `insertedCount=3`, so the local dev backend did not reproduce the user-not-found error for 吴晓霞;
  - recorded likely causes: user is hitting an older/unrestarted backend, a different environment has different 吴晓霞 mobile/DingTalk ID data, or that environment cannot uniquely match 吴晓霞 by name + mobile in DingTalk roster.
- Follow-up for screenshot/log mismatch:
  - inspected `/Users/jiangyongming/Desktop/1.png`; DingTalk backend list is filtered by approval launch time and shows 4 overtime applications for 吴晓霞 in 2026-06, not the business overtime start/end time.
  - queried `hrsystem.ddAccount` and confirmed `CompanyID=0003` uses app `dingqbfjn6bzqq78g7po`, admin ID `296842114330963873`.
  - direct DingTalk read-only verification with the 0003 app:
    - `process.template.manage.get` with admin `296842114330963873` returns `400023/用户不存在`;
    - fallback `process/listbyuserid` with 吴晓霞 `262756571521566047` succeeds;
    - `processinstance/listids` returns 4 overtime instances: 2 `COMPLETED/agree`, 1 `COMPLETED/refuse`, 1 `TERMINATED`.
  - confirmed local `06:05-07:05` comes from the rejected instance `NzMSp1MiT5GkPu20k2IsPA04041781527092`: form value has `加班日期=2026-06-15 18:00`, `结束日期=2026-06-15 07:05`, `预计加班时长=1`; parser repaired it by subtracting duration from end time.
  - added RED test `fetchMonthData_shouldPersistOnlyApprovedWorkflowInstances`; first run failed because old code saved all 3 simulated instances instead of only the approved one.
  - implemented approved-instance filter in `HrmAttendanceApprovalSyncServiceImpl`: only `status=COMPLETED` and `result=agree` is saved; skipped instances are not retained, so stale cleanup removes previous rejected/terminated snapshots.
  - clarified admin template fallback log text so `adminUserId` is explicitly described as the DingTalk administrator, not the selected employee.
  - GREEN targeted verification passed: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldPersistOnlyApprovedWorkflowInstances test`.
  - GREEN regression passed: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test`, 87 tests, 0 failures.
  - ran local endpoint through `http://127.0.0.1:8081/api` with the user's payload `{"approvalTypes":["all"],"employeeIds":["1831601326890434565"],"month":"2026-06"}`; response returned `insertedCount=3`.
  - database verification now shows 吴晓霞 `2026-06` has 2 approved overtime snapshots plus 1 compensatory leave snapshot; rejected `06:05-07:05` is gone.
- Follow-up investigation for 李明明 admin-template fallback log:
  - refreshed backend docs and source for approval fetch target selection;
  - confirmed frontend fetch target selection sends the right-side employee transfer IDs, or department-expanded employee IDs, into `employeeIds`;
  - queried `hr_0003.hrm_employee`: active 李明明 is `employee_id=1831601326890434563`, mobile `17389819211`, DingTalk `userId=024662561026250638`; deleted same-name row is `employee_id=2033769831940079620` with no DingTalk ID;
  - queried `hr_0003.tbattendanceuser`: only active 李明明 maps to `024662561026250638`;
  - queried `hr_0003.tbattendanceapprove`: that `userId` already has 5 local approval snapshots;
  - queried `hrsystem.ddAccount`: `companyId=0003` uses admin ID `296842114330963873`;
  - checked recent DingTalk on-job roster logs in `postresultlog`; `024662561026250638` appears in the returned on-job user list.
  - verification:
    - `node tests/attendance-approval-fetch-utils.test.mjs` passed;
    - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` passed, 7 tests, 0 failures; test log includes both `adminUserId` and `employeeUserId`.
  - command corrections:
    - an `rg` verification command initially used unescaped backticks around `hr_0003`; reran with single-quoted patterns successfully;
    - a `git status` command initially mixed backend and frontend repository paths; reran with the correct repository roots.
- Follow-up implementation for repeated admin-template failure on every selected employee:
  - user clarified the desired behavior: approval fetch should be driven by the right-side employee/department selection, without a confusing administrator-template prequery;
  - updated `HrmAttendanceApprovalSyncServiceImpl#resolveProcessTemplates(...)` to directly call `fetchUserVisibleProcessTemplates(token, userId)`;
  - removed the unused administrator-template query method, admin user resolver, and related DingTalk request/response imports from `HrmAttendanceApprovalSyncServiceImpl`;
  - updated `HrmAttendanceApprovalSyncServiceImplTest` so process-code resolution must not call manageable/admin templates even when they are configured or would fail;
  - RED verification: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` failed 3 tests under the old implementation;
  - GREEN verification after cleanup: `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` passed, 7 tests, 0 failures;
  - source check: `rg \"fetchManageableProcessTemplates|resolveAdminUserId|OapiProcessTemplateManageGet|shouldFallbackFromManageableTemplateError\" ...` returned no matches in the implementation/test files;
  - approval regression after cleanup: `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` passed, 50 tests, 0 failures.

# Progress: 李明明实际出勤与应计出勤小时差异修复

## 2026-07-14
- 用户反馈李明明“实际出勤时间统计”显示 `184(23) / 0(0) / 207.4(25.93)`，正确小时值应为 `184 / 160 / 184`。
- 已刷新相关需求/开发文档，确认当前文档要求：行政体系员工实际出勤不叠加日级加班；应计出勤公式为 `实际出勤时间 + 调休 + 年假 - 加班 + 病假`。
- 已新建本轮计划，下一步按系统化调试流程从数据库、后端计算和前端列取值三层追踪根因。

# Progress: 加班/夜班统计重算锁等待治理

## 2026-07-14
- Refreshed backend requirements/development docs and planning files for the current lock-wait report.
- Ran planning session catchup correctly with `python3`; first attempted with `sh` and recorded that invocation error in `task_plan.md`.
- Confirmed existing lock-wait risk in `HrmOvertimeNightStatisticsServiceImpl`:
  - statistics entry points were method-level long transactions;
  - old detail deletion needed to happen after row calculation;
  - repository deletes needed to be JPQL bulk deletes.
- Implemented backend lock-window reduction:
  - removed method-level `@Transactional` from `startStatistics(...)` and `startStatisticsForEmployee(...)`;
  - added `TransactionOperations` and short write helpers `replaceMonthlyDetails(...)` / `replaceEmployeeMonthlyDetails(...)`;
  - retained compute-before-delete behavior so source-data scanning does not hold old detail-row locks;
  - ensured the short write transaction flushes after bulk delete and after save.
- Confirmed repository delete contract:
  - `deleteAllByWorkDateBetween(...)` uses `@Modifying @Query` and returns `int`;
  - `deleteAllByEmployeeIdAndWorkDateBetween(...)` uses `@Modifying @Query` and returns `int`.
- Added regression assertion `overtimeNightStatisticsEntryPoints_shouldNotUseLongRunningTransactions`.
- Added SQL script `docs/sql/2026-07-14_overtime_night_statistics_detail_indexes.sql` for idempotent `work_date` and `employee_id, work_date` delete-condition indexes across `hr_0001` to `hr_0005`.
- Verification:
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#overtimeNightStatisticsEntryPoints_shouldNotUseLongRunningTransactions+startStatistics_shouldCalculateRowsBeforeDeletingExistingRowsToReduceLockTime+overtimeNightDetailRepositoryDeleteMethods_shouldUseBulkModifyingQueries test` passed, 3 tests, 0 failures.
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` passed, 44 tests, 0 failures.
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` passed, 94 tests, 0 failures.
- Updated `docs/requirements.md`, `docs/development.md`, `task_plan.md`, `findings.md`, and `progress.md`.
# Progress: 薪资核算失败集中提示

## 2026-07-17
- 已读取后端项目 `docs/requirements.md`、`docs/development.md` 和既有计划/发现/进度记录。
- 已记录本轮目标、阶段和假设到 `task_plan.md`。
- 已读取前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认薪资核算已有真实进度弹窗，排班提交已有行级错误留在提交面板的类似模式。
- 已定位 `getOrCreateRecordAndApplyAttendance` 2098 行根因：员工考勤数据 `cv` 为空或缺少应/实际出勤天数字段时直接取值，尤其是计薪员工缺工号时默认考勤 map 不会生成。
- 已扫描薪资核算链路的首批风险：缺工号、缺应/实际出勤天数、缺考勤规则、基本工资设置字段为空、生产员工排班工时异常、社保前置记录缺失等。
- RED 验证：
  - 后端 `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 失败于缺少 `SalaryMonthRecordServiceNew.collectComputePrerequisiteErrors(...)`；
  - 前端 `node tests/compute-progress-utils.test.mjs` 失败于缺少 `normalizeSalaryComputeProgress` 导出；
  - 前端 `node tests/salary-start-compute-dialog.test.mjs` 失败于薪资核算进度窗口未渲染 `computeProgressErrors`。
- 已实现后端集中预检和运行期保护：
  - `SalaryComputeProgressVO` 增加 `errors`；
  - `collectComputePrerequisiteErrors(...)` 聚合缺工号、缺考勤、应/实际出勤异常、缺考勤规则、缺基本工资关键字段等；
  - `collectInsuranceDataErrors(...)` 将同步社保前置失败并入预检；
  - `getOrCreateRecordAndApplyAttendance(...)` 改为显式校验考勤 map 与数字字段，返回员工可定位的中文错误。
- 已实现前端进度窗口错误详情：
  - `normalizeSalaryComputeProgress(...)` 保留后端 `errors`；
  - `SalaryManage.vue` 失败时不再依赖短暂错误 toast，而是在“核算薪资进度”窗口展示错误列表并保持窗口打开；
  - 已补充主接口失败但进度轮询未拿到失败状态时的错误文案兜底。
- GREEN 验证：
  - 后端 `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，47 个测试；
  - 前端 `node tests/compute-progress-utils.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/salary-compute-scope-utils.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：
  - 后端本轮相关文件 `git diff --check -- ...` 通过；
  - 前端目标文件在当前顶层 Git 仓库中为未跟踪文件，`git diff --check` 不覆盖其内容；已直接扫描目标文件尾随空白，结果无输出。
- 已更新后端 `docs/requirements.md`、`docs/development.md`，前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，以及当前计划/发现/进度记录。
## 2026-07-17 薪资行政/生产体系硬编码移除

- 已补测试：
  - `SalaryMonthRecordServiceNewTest` 覆盖员工 ID 例外不再影响体系判断、活动旧服务不得保留部门名/固定休息天数生产规则；
  - `HrmSalaryMonthEmpRecordMapperXmlTest` 覆盖计薪员工查询和工资列表使用 `affiliation_system`；
  - `HrmProduceAttendanceServiceImplTest` 覆盖考勤汇总同步缺失所属体系时不再按“生产部”部门名推断；
  - `HrmEmployeeMapperSqlTest` 覆盖有加班费员工筛选使用 `affiliation_system=2` 且不保留部门/岗位名称例外。
- 已实现：
  - `SalaryMonthRecordServiceNew` 新增统一体系 helper，主核算、同步考勤填充、导出工资全部复用；
  - 删除旧员工 ID 强制行政/生产、生产体系双休员工名单、未使用的生产 `-4` 应出勤函数；
  - `SalaryMonthRecordService_Bak` 改为复用新 helper，并移除旧生产 `-4` 逻辑；
  - `HrmSalaryMonthEmpRecordMapper.xml` 返回 `affiliationSystem`，兼容 `isProduceDept` 由该字段派生；
  - `QuerySalaryPageListVO` 新增 `affiliationSystem`；
  - `HrmProduceAttendanceServiceImpl` 导入和同步均按员工 `affiliationSystem` 写入考勤汇总部门类型，缺失时行政兜底；
  - `HrmEmployeeMapper.xml#queryHasOverTimePayEmpList` 改为 `affiliation_system=2`。
- 验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmProduceAttendanceServiceImplTest,HrmEmployeeMapperSqlTest test` 通过，50 个测试；
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest,HrmProduceAttendanceServiceImplTest test` 通过，67 个测试。

# Progress: 员工管理唯一性与表单字段调整

## 2026-07-17
- 已读取项目需求文档和开发文档。
- 已追加本轮计划，初始方案为导入、新增、编辑统一校验未删除员工的工号、手机号、身份证号唯一；编辑排除当前员工；前端新增/编辑页面移除全勤与加入钉钉字段。
- 已读取前端 `hr_web` 需求文档和开发文档，确认前端旧字段要求被本轮需求覆盖。
- 已初步定位后端员工控制器/服务/mapper/导入测试，以及前端员工新增编辑组件和现有员工测试。
- 已确认新增员工服务当前覆盖前端工号为时间戳，需要改为保留用户输入并做唯一校验。
- 已确认花名册导入当前只按姓名+手机号匹配和文件内去重，需补工号/手机号/身份证号唯一校验。
- 已确认 `AddOrEdit.vue` 仍渲染“是否有全勤 / 是否加入钉钉”字段。
- RED 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest test` 编译失败，缺少 `validateEmployeeUniqueFields(...)`。
- RED 前端：`node tests/employee-form-field-removal.test.mjs` 失败，提示新增/编辑页面仍渲染全勤字段。
- 已实现后端：
  - `HrmEmployeeServiceImpl#validateEmployeeUniqueFields(...)` 统一校验未删除员工的工号、手机号、身份证号唯一；
  - 新增/动态新增复用 `transferEmployee(...)` 校验，编辑和确认入职保存前排除当前员工校验；
  - 花名册导入先完成整份文件唯一性预检，再统一落库；同一文件后续重复行会在保存任何员工前被拦截，导入方法同时加事务兜底动态字段保存失败；
  - 通讯信息保存 `updateCommunication(...)` 也接入手机号等唯一字段校验，避免通讯页修改手机号绕过规则；
  - 新增员工不再把前端传入工号覆盖为当前时间戳。
- 已实现前端：
  - `AddOrEdit.vue` 移除新增/再次入职弹窗中的“是否有全勤 / 是否加入钉钉”字段和提交值；
  - `EmployeePostInfo.vue` 不再追加这两个岗位编辑字段，并过滤后端返回的旧字段，避免编辑页面继续显示。
- GREEN 定向：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest test` 通过，13 个测试；
  - `node tests/employee-form-field-removal.test.mjs` 通过。
- 前端员工回归：
  - `node tests/employee-edit-save-regression.test.mjs` 通过；
  - `node tests/employee-import-file.test.mjs` 通过；
  - `node tests/employee-import-result-ui.test.mjs` 通过；
  - `node tests/employee-template-download-ui.test.mjs` 通过；
  - `node tests/employee-template-download-api.test.mjs` 通过。
- 后端员工回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeControllerTest,HrmEmployeeServiceImplEducationExperienceTest,HrmEmployeeServiceImplQueryPageListTest test` 通过，16 个测试。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 审查前收尾复核（2026-07-17 12:29）：
  - 重新执行后端员工回归 Maven 命令，通过 15 个测试；
  - 重新执行 6 个前端员工 Node 回归，均通过；
  - 重新执行 `npm run build`，构建通过，仍仅保留既有 `::v-deep` 过时警告和 chunk size warning。
- 代码审查反馈处理：
  - RED：`HrmEmployeeServiceImplUniqueValidationTest` 新增通讯手机号重复测试，旧实现未在保存前拦截；`HrmEmployeeServiceImplImportEmployeeTest` 增加导入失败无部分保存断言，旧实现会先保存第一行；
  - GREEN：通讯保存接入统一唯一性校验，导入改为整文件预检后落库；
  - 定向验证：`HrmEmployeeServiceImplUniqueValidationTest` 3 个测试通过，`HrmEmployeeServiceImplImportEmployeeTest` 10 个测试通过。
- 最终验证：
  - `mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeControllerTest,HrmEmployeeServiceImplEducationExperienceTest,HrmEmployeeServiceImplQueryPageListTest test` 通过，16 个测试；
  - `node tests/employee-form-field-removal.test.mjs`、`node tests/employee-edit-save-regression.test.mjs`、`node tests/employee-import-file.test.mjs`、`node tests/employee-import-result-ui.test.mjs`、`node tests/employee-template-download-ui.test.mjs`、`node tests/employee-template-download-api.test.mjs` 均通过；
  - `npm run build` 通过，仍仅保留既有 `::v-deep` 过时警告和 chunk size warning；
  - 后端/前端目标文件 `git diff --check` 通过，未跟踪测试和计划文件尾随空白扫描无输出。

## 2026-07-20 Salary Export Scope
- 已启动本轮任务：薪资导出添加人员/部门选择界面，参考开始核算功能。
- 已确认当前后端项目根目录和项目文档/计划文件位置。
- 已读取后端和前端项目的需求/开发文档。
- 已向用户摘要当前相关约束：导出弹窗按开始核算的人员/部门范围选择逻辑实现，默认未选保持全量导出。
- 已使用 UI 技能确认保持数据密集型后台风格，复用现有 Element Plus 控件。
- 已定位前端 `SalaryManage.vue`、`AloneComputeDialog.vue`、`salary-compute-scope-utils.js`、`salary.js`，以及后端 `HrmSalaryMonthRecordController#exportSalary`、`SalaryMonthRecordServiceNew#exportSalaryNew`。
- 已确认需要补前端导出范围弹窗、导出 DTO 的 `employeeIds`，以及服务层候选员工过滤。
- 已补 RED 测试：
  - 前端工具测试失败于缺少 `buildSalaryExportPayload`；
  - 前端弹窗测试失败于缺少导出范围弹窗状态；
  - 后端测试编译失败于缺少 `resolveExportEmployeeIds(...)`。
- 已实现：
  - `salary-compute-scope-utils.js` 新增 `buildSalaryExportPayload(...)`，选中人员时导出只传 `employeeIds`，未选范围时保留当前列表筛选条件；
  - `AloneComputeDialog.vue` 参数化按钮、提示、确认文案和部门空状态文案，供开始核算和导出复用；
  - `SalaryManage.vue` 点击“导出薪资”改为先打开范围选择弹窗，确认后下载，并在下载期间展示 loading/disabled；
  - `QuerySalaryExportDto` 新增 `employeeIds`；
  - `SalaryMonthRecordServiceNew#exportSalaryNew(...)` 在查询月薪资明细前把候选员工与导出选择员工取交集。
- GREEN 定向：
  - `node tests/salary-compute-scope-utils.test.mjs` 通过；
  - `node tests/salary-export-scope-dialog.test.mjs` 通过；
  - `node tests/salary-start-compute-dialog.test.mjs` 通过；
  - `mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveExportEmployeeIds_shouldUseSelectedEmployeeIdsAndKeepCandidateOrder+resolveExportEmployeeIds_shouldFallbackToCandidatesWhenSelectionEmpty+resolveExportEmployeeIds_shouldReturnEmptyWhenSelectionHasNoCandidateMatch+salaryExportSource_shouldApplySelectedEmployeeIdsBeforeQueryingMonthList test` 通过，4 个测试。
- 已更新项目文档：
  - 后端 `docs/requirements.md`、`docs/development.md`；
  - 前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`。
- 最终验证：
  - `node tests/salary-compute-scope-utils.test.mjs` 通过；
  - `node tests/salary-export-scope-dialog.test.mjs` 通过；
  - `node tests/salary-start-compute-dialog.test.mjs` 通过；
  - `node tests/compute-progress-utils.test.mjs` 通过；
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning；
  - `mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，69 个测试；
  - 后端目标文件 `git diff --check -- ...` 通过，前端目标文件尾随空白扫描无输出。

# Progress: 采购计划部超缺勤工资排查

## 2026-07-20
- 用户反馈“采购计划部的员工算出来都有超缺勤工资”，要求查找原因。
- 已启用项目文档优先、文件化计划和系统性根因排查流程。
- 已读取后端项目需求/开发文档和既有计划记录。
- 已建立本轮专项排查计划，当前先定位工资项编码、计算公式和数据来源。
- 已定位工资项：`200101 / 超缺勤` 是考勤扣款合计，`9008 / 超缺勤天数` 只是导出天数字段。
- 已读取代码公式：`200101` 由考勤扣款子项累加；本次采购计划部的非零来源是 `190103 / 旷工扣款`。
- 已只读查询 `hr_0003`：
  - 采购计划部 `dept_id=2033769625647431684`；
  - 2026-06 薪资主记录 `2077807285177995265 / 六月薪资报表`；
  - 参与薪资的 3 名采购计划部员工均为 `need_work_day=21.75`、`actual_work_day=23.00`，`200101` 为负数；
  - `hrm_attendance_info` 缺少 `2026-06` 配置，薪资核算回退默认应出勤 `21.75`；
  - 加班/夜班统计与考勤报表均显示采购计划部 6 月应出勤为 `23` 天。
- 已用公式复核金额：`定薪总额 / 21.75 * (21.75 - 23)` 精确等于三人的 `190103` 与 `200101`。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md`、`findings.md`、`progress.md` 记录本轮排查结论。
- 已运行 `git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md`，无输出。

# Progress: 删除薪资核算 21.75 硬编码

## 2026-07-20
- 用户确认按建议修改，并要求检查整个薪资核算代码，删除所有硬编码 `21.75` 天调用。
- 已重新读取项目需求/开发文档和采购计划部排查记录。
- 已扫描 `src/main/java/src/test/java/src/main/resources/docs` 中 `21.75`、`DEFAULT_NORMAL_DAYS`、`normalDaysByDeptType` 等引用。
- 当前计划：先补 RED 测试，再改 `SalaryMonthRecordServiceNew` 的应出勤来源和默认缺失处理，同时清理 `SalaryMonthRecordService_Bak`、旧注释和受影响测试。
- 已复跑 RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveSalaryExpectedAttendanceDays_shouldPreferOvertimeNightAndNotFallbackToDefaultDays+salaryComputeSources_shouldNotContainHardcodedDefaultAttendanceDays test` 编译失败于缺少 `resolveSalaryExpectedAttendanceDays(...)`，符合预期。
- 已实现修复：
  - `SalaryMonthRecordServiceNew` 删除 `DEFAULT_NORMAL_DAYS` / `DEFAULT_NORMAL_DAYS_DECIMAL`，新增薪资应出勤解析 helper。
  - `hrm_attendance_info` 缺失或配置无效时不再返回默认值；缺加班/夜班统计和显式配置时抛 `HrmException(6001)`，错误包含员工、工号和年月。
  - 同步考勤、无考勤组员工、半月转正分段薪资均不再使用硬编码默认应出勤。
  - `SalaryComputeContext` 增加 `expectedAttendanceDaysByEmployee`，用于主计算阶段传递按员工应出勤。
  - `SalaryMonthRecordService_Bak` 和旧实现注释中的默认值已清理。
- 已更新相关测试中的示例应出勤值为 `22.00`，并保留源码扫描测试禁止生产代码包含旧默认值。
- GREEN 验证：同一 RED 命令修复后通过，2 个测试。
- 定向验证：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，49 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，71 个测试。
- 生产源码扫描：`rg -n "21\\.75|DEFAULT_NORMAL_DAYS" src/main/java src/main/resources -S` 无命中。
- 用户追加要求：如果缺少加班/夜班统计应出勤，不再进入读取考勤配置的第 2 步，生成薪资和导出薪资都直接提示到“单双休设置”维护数据。
- 已补追加 RED 测试并确认失败：
  - `resolveSalaryExpectedAttendanceDays_shouldRequireOvertimeNightAndNotFallbackToAttendanceInfo` 旧代码仍返回配置天数 `22.00`；
  - `requireSalaryExpectedAttendanceDays_shouldPromptSingleDoubleRestSettingWhenOvertimeNightMissing` 旧代码没有抛异常；
  - `resolveExportFullWorkDays_shouldRejectMissingOvertimeNightStatistics` 旧文案不包含“单双休设置”。
- 已实现追加修复：`resolveSalaryExpectedAttendanceDays(...)` 不再返回 `configuredNormalDays`；生成薪资和导出薪资缺应出勤时错误文案均包含“单双休设置”。
- 追加 GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveSalaryExpectedAttendanceDays_shouldRequireOvertimeNightAndNotFallbackToAttendanceInfo+requireSalaryExpectedAttendanceDays_shouldPromptSingleDoubleRestSettingWhenOvertimeNightMissing+resolveFullAttendanceExpectedDays_shouldPreferOvertimeNightExpectedDays+resolveExportFullWorkDays_shouldRejectMissingOvertimeNightStatistics test` 通过，4 个测试。
- 追加定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，50 个测试。
- 最终薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，72 个测试。
- 最终检查：`rg -n "21\\.75|DEFAULT_NORMAL_DAYS" src/main/java src/main/resources -S` 无命中；`git diff --check` 对本轮相关文件无输出。
- 2026-07-20 17:05 按用户最新要求复核：生成薪资缺加班/夜班应出勤会提示到“单双休设置”并停止；导出薪资缺统计明细也提示到“单双休设置”并停止。已重跑薪资组合回归 72 个测试通过；生产源码扫描 `21.75` / `DEFAULT_NORMAL_DAYS` 无命中；相关文件 `git diff --check` 无输出。
# Progress: 全员全勤奖漏发排查

## 2026-07-20
- 用户反馈核算薪资中有些员工没有全勤奖，例如张明，要求检查所有员工全勤奖问题。
- # Progress: `cfy` 登录 TooManyResultsException 本地复测

## 2026-08-21
- 已在本地复测 `POST http://127.0.0.1:9080/hrsystem/login?account=cfy&password=123`，返回 `success=true`，说明当前 `target/classes` 对应的后端进程已加载新代码。
- 已复测前端代理入口 `POST http://127.0.0.1:8081/api/hrsystem/login?account=cfy&password=123`，同样返回 `success=true`。
- 当前本地 9080 进程是 IntelliJ 启动的调试 JVM，classpath 指向 `/Users/jiangyongming/Project/hr/hainan/target/classes`，不是旧 jar 包。
- 因此用户若仍看到 `TooManyResultsException`，优先怀疑浏览器/前端请求实际上打到了其他地址、旧后端进程或未刷新的页面缓存，而不是这次代码修改没有生效。

- 已启用项目文档优先、文件化计划和系统性根因排查流程。
- 已读取 `docs/requirements.md`、`docs/development.md` 和现有计划/发现/进度记录。
- 已记录本轮专项排查计划。
- 已确认张明唯一在职记录：`1831601326890434564 / TYNG-012 / 13197133302`，档案启用全勤、已转正、非残疾。
- 已确认目标薪资：`hr_0003` 2026-06 `2077807285177995265 / 六月薪资报表`。
- 张明薪资明细 `need_work_day=23.00`、`actual_work_day=23.00`，加班/夜班统计 `expected=23`、`accrued=184.00小时`，但 `40102` 为空。
- 张明最终迟到、早退、旷工、事假、病假、缺卡、综合扣款、超缺勤工资均为 `0`；钉钉原始汇总仍有缺卡 `16` 和迟到 `1` 分钟。
- 全员统计：95 名薪资员工中 76 人已有全勤奖；12 人满足“档案启用全勤、非残疾、加班/夜班应计出勤达到应出勤、但 `40102` 为空”；其中 6 人最终考勤扣款为 0，分别是张雪梅、吴晓霞、张明、王琪、闫倩、高文利。
- 已补 RED 测试 `shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected`，旧代码按预期失败。
- 已修复 `SalaryMonthRecordServiceNew#shouldFallbackFullAttendanceByAccruedDays(...)`，不再因为钉钉月度汇总对象存在而拒绝按应计出勤兜底。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#shouldFallbackFullAttendanceByAccruedDays_shouldUseAccruedDaysWhenAccruedReachesExpected+shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected test` 通过，2 个测试。
- 已复核当前薪资管理 controller 走 `SalaryMonthRecordServiceNew`；旧 `SalaryComputeTask` 的核算调用和定时任务均为注释状态，本轮不修改 `SalaryMonthRecordService_Bak`。
- 完整定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，51 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，73 个测试。

# Progress: 农谷 2026-06 高温补贴与薪资档案补齐

## 2026-07-20
- 用户要求：高温补贴未录入的补到考勤汇总表其它补贴；基本工资、岗位工资、职务工资不一致的补到薪资档案三项；采用姓名 + 电话作为唯一键。
- 已读取项目需求/开发文档并新增本轮计划。
- 已确认目标租户沿用 `hr_0003`，目标年月为 `2026-06`。
- 已确认考勤汇总表字段：`hrm_produce_attendance.other_subsidies`；薪资档案三项字段：`hrm_salary_archives_option` 的 `10101/10102/10103`。
- 已扫描两个 Excel，确认源文件没有手机号列；预览阶段用姓名、入职日期、部门、岗位定位源行和系统员工，SQL 执行条件使用 `hrm_employee.employee_name + mobile`。
- 已生成预览报告 `docs/reports/2026-07-20-nonggu-salary-attendance-supplement-preview.md`：
  - 基准人员 36 人；
  - 高温补贴需补入其它补贴 19 人；
  - 赵聪其它补贴已是目标值 100，跳过；
  - 薪资档案需更新 6 条；
  - 吴镜平在手工表无同名行，未处理；
  - 异常 0 条。
- 已生成执行 SQL `docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement.sql` 和回滚 SQL `docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement_rollback.sql`。
- 已执行 SQL 到 `hr_0003`。
- 写后核验通过：
  - 20 个相关考勤汇总行（19 个本轮更新 + 赵聪已达目标）`other_subsidies=100`，异常 0；
  - 6 条薪资档案目标项均等于手工表目标值，异常 0。
- 已更新 `docs/requirements.md`、`docs/development.md`、`task_plan.md` 和 `findings.md`。

# Progress: 员工合同无固定期限结束日期

## 2026-08-21
- 用户要求按长期方案修改：`无固定期限劳动合同` 不应强制填写合同结束日期。
- 已读取后端 `docs/requirements.md`、`docs/development.md`，确认旧规则要求合同开始/结束日期均必填，并由后端按日期计算期限。
- 已读取前端 `hr_web/docs/requirements.md`、`hr_web/docs/development.md`，确认员工管理前端在 `src/views/hrm/employee/**`。
- 已新增实施计划 `docs/plans/2026-08-21-employee-open-ended-contract.md`。
- 已补后端 RED 用例到 `HrmEmployeeContractServiceImplTest`：
  - 手工保存 `contractType=2,endTime=null`；
  - 导入 `合同类型=无固定期限劳动合同,合同结束日期为空`；
  - 固定期限合同空结束日期仍报错。
- `mvn -Dtest=HrmEmployeeContractServiceImplTest test` 当前因工作区既有全量 `testCompile` 问题失败，未执行到本测试；失败集中在无关测试引用缺失类。
- 已用单独 classpath 编译并运行 `HrmEmployeeContractServiceImplTest`：红灯符合预期，2 个失败均来自旧代码强制 `endTime` 必填。
- 已补前端 RED 测试 `hr_web/tests/employee-contract-model.test.mjs`：红灯符合预期，`contractType=2` 时 `endTime` 仍为必填。
- 已补后端导出 RED 用例：
  - `EmployeeBasicInfoExportSupportTest#buildRows_shouldDisplayOpenEndedContractWithoutEndDate`；
  - `EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldDisplayOpenEndedContractDeadlineText`；
  - `HrmEmployeeMapperSqlTest` 要求基础信息/部门明细查询返回最新合同类型别名。
- 窄路径导出测试红灯符合预期：基础信息/部门明细显示为空，SQL 缺少合同类型别名。
- 已实现后端：
  - `HrmEmployeeContractServiceImpl` 对 `contractType=2` 的合同只要求开始日期，保存时清空 `endTime/term`；
  - 合同导入先解析合同类型，无固定期限合同允许结束日期为空并清空占位日期；
  - `EmployeeBasicInfoExportSupport` 与 `EmployeeDepartmentDetailExportSupport` 对无固定期限合同显示 `无固定期限`；
  - `HrmEmployeeMapper.xml` 为基础信息导出和部门明细导出返回最新/最后合同类型别名。
- 已实现前端：
  - `employeeContractModel.js#getRules(...)` 在 `contractType=2` 时不再要求 `endTime/term`；
  - 合同类型切到无固定期限时，`EmployeeContract.vue` 清空残留 `endTime/term` 并刷新字段和校验；
  - 合同只读展示用 `formatFieldValue(...)` 把空结束日期显示为 `无固定期限`。
- Fresh 验证：
  - `mvn -Dtest=HrmEmployeeContractServiceImplTest,EmployeeBasicInfoExportSupportTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest test` 通过，38 个测试 0 failures/errors；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM duplicate/systemPath warnings。

# Progress: 吴镜平离职后仍生成 2026-06 薪资排查

## 2026-07-21
- 用户确认吴镜平已离职，追问为什么仍计算了薪资。
- 已按项目文档优先、文件化计划和系统性排查流程处理，本轮只读排查，未修改业务数据。
- 已确认吴镜平档案：`employee_id=1831601326890434575`，手机号 `13797928108`，工号 `1718539734988`，`entry_status=4`，`is_del=0`。
- 已确认离职记录：`plan_quit_time=2026-06-10`，`salary_settlement_time=2026-06-10`。
- 已定位计薪员工 SQL：离职员工 `entry_status=4` 且 `plan_quit_time > date_sub(endTime, interval 1 month)` 会纳入计薪；6 月工资 `end_time=2026-06-30`，阈值为 `2026-05-30`，吴镜平命中。
- 已确认 2026-06 薪资主记录 `2077807285177995265` 实际有 95 个员工明细，包含吴镜平；主表 `num=1` 与明细数不一致。
- 已确认吴镜平 2026-06 薪资明细 `need_work_day=23.00`、`actual_work_day=23.00`，考勤汇总 `positive_attendance=23.00`、`probation_attendance=23.00`。
- 已确认工资项闭合：`10101=2130`、`10102=3070`、`40102=100`，最终 `210101/240101=5300.00`。
- 已更新 `docs/requirements.md`、`docs/development.md` 和 `findings.md` 记录本轮结论。

# Progress: 张雪梅 2026-06 应发与实发工资计算过程排查

## 2026-07-21
- 用户要求调出张雪梅应发工资和实发工资计算过程。
- 已读取项目需求/开发文档，确认相关口径：`hr_0003`、2026-06、张雪梅存在 2 天病假；病假工资 2 天内免扣，但有效病假仍应扣全勤。
- 已追加本轮只读排查计划；除文档和计划记录外，不修改业务数据。
- 已定位当前薪资：`s_record_id=2077807285177995265`，`s_emp_record_id=2077929043646251010`，`need_work_day=25.00`、`actual_work_day=25.00`。
- 已查询工资项并闭合当前落库结果：`10101=2130`、`10102=1570`、`281=100`、`19010401=1042`、`200101=1042`，得到 `210101=2758.00`；再扣 `100101=466.50`、`100102=144`、`160102=13.79`、`230101=0`，得到 `240101=2133.71`。
- 已确认病假来源：本地审批两条病假各 `8小时`，考勤报表 `病假=16.00`；当前源码行政体系病假分支把 `16小时` 直接当 `16天`，因此生成了 `1042` 病假扣款。
- 已更新 `docs/requirements.md`、`docs/development.md` 和 `findings.md` 记录本轮结论与后续需修复点。

# Progress: 病假小时误当作天数扣款修复

## 2026-07-21
- 用户要求修复病假扣款问题。
- 已复核需求/开发文档：`hrm_attendance_report_data` 中 `病假/事假` 字段按小时累计，病假扣款天数必须按 `小时 / 8` 折算；2 天内不扣病假工资，但有效病假仍扣全勤。
- RED：新增 `SalaryMonthRecordServiceNewTest#sickDeductDays_shouldConvertAdministrativeSickLeaveHoursToDays`，旧逻辑下失败，行政体系两条 `8小时` 病假未返回 `2.00` 天。
- GREEN：修改 `SalaryMonthRecordServiceNew#sickDeductDays(..., type="2")` 行政体系分支，按 `sickHours / 8` 折算病假天数；同一测试通过。
- 定向验证：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 58 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过 80 个测试。
- 本轮只修复代码逻辑和测试，不直接修改已生成工资项；张雪梅 2026-06 需重新核算后才会从当前落库的 `19010401=1042` 变为按 2 天病假免扣。

# Progress: 余德胜薪资无工会费排查

## 2026-08-17
- 用户询问 `hr_0003` 中余德胜薪资为什么没有工会费。
- 已只读查询员工档案：余德胜 `employee_id=1831601326890434582`，`is_disabled=1`，正式在职且已转正。
- 已复核薪资规则：`SalaryComputeServiceNew` 仅在 `isDisabled == "2"` 时计算工会费；`is_disabled=1` 命中残疾员工免工会费分支。
- 已复核薪资项：`2026-07` 应发 `4100.00`、工会费 `0`；`2026-06` 应发 `4200.00`、工会费 `0`。
- 本轮没有修改代码或业务数据；结论已记录到 `findings.md`。

# Progress: 员工其他补助导入导出与保存

## 2026-08-21
- 用户要求员工导入导出新增“其他补助”：花名册模板在“职务补助”旁新增两位小数数字列，父表头为“薪酬福利”，导入可保存入库。
- 用户要求员工管理新建员工与点击员工姓名弹窗分别新增“其他补助”输入框，两位小数数字类型，并可保存入库。
- 用户要求下载部门明细的“薪资待遇”也加上“其他补助”。
- 已读取项目需求/开发文档，确认既有“薪资等级/固定绩效/职务补助”使用员工动态字段体系保存，不新增员工主表物理列；本轮按同一模式扩展“其他补助”。
- RED 后端：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 失败于 `HrmEmployeeChangeRecord#setOtherSubsidy(...)` 缺失，证明新入参尚未接入。
- RED 前端：`node tests/employee-salary-fields-ui.test.mjs` 失败于新增员工表单缺少 `otherSubsidy`；`node tests/employee-list-row-normalizer.test.mjs` 失败于缺少 `other_subsidy` 归一化。
- 已实现后端：
  - 花名册模板在 `职务补助` 右侧新增 `其他补助`，设置 `0.00` 列格式，并扩展 `薪酬福利` 合并父表头；
  - 员工动态字段自动补齐/纠正 `其他补助` 为个人信息小数字段、精度 2；
  - 新建员工、再次入职、员工详情基本信息编辑和员工变更操作均接收 `otherSubsidy` 并保存为两位小数字符串；
  - 花名册导入读取 `其他补助` 并保存到 `hrm_employee_data`；
  - 部门明细 `薪资待遇` 和固定薪资成本纳入 `其他补助`。
- 已实现前端：新建/再次入职、员工详情基本信息兜底字段、办理转正/调岗/晋升降级弹窗和员工列表行归一化均接入 `otherSubsidy/other_subsidy/其他补助`。
- 调试中发现并修复同一导入模块已有失败：花名册行 `姓名+手机号` 未命中但 `姓名+身份证号` 命中时，现在更新原员工，避免手机号变更被误判新员工并触发工号重复。
- GREEN 后端：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 通过，44 个测试。
- GREEN 前端：`node tests/employee-salary-fields-ui.test.mjs`、`node tests/employee-list-row-normalizer.test.mjs`、`node tests/employee-form-field-removal.test.mjs`、`node tests/employee-edit-save-regression.test.mjs`、`node tests/employee-company-age-ui.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

# Progress: 数据配置类型和值可自定义

## 2026-08-22
- 已读取 `hainan` 与 `hr_web` 的需求/开发文档，确认数据配置页原实现把类型固定为工段/车间。
- 已定位前端页面 `hr_web/src/views/hrm/dataConfig/Index.vue`、前端 API `src/api/hrm/dataConfig/index.js`、后端入口 `DictDataController` 和 `tbDictDataServiceImpl`。
- 已补 RED 测试：
  - `hr_web/tests/data-config-api.test.mjs` 断言 `addDictData` 仅提交 payload，不再携带固定 `AddType`；
  - `hr_web/tests/data-config-page.test.mjs` 断言页面使用“类型/值”两个输入框，不再提供工段/车间固定下拉；
  - `src/test/java/com/tianye/hrsystem/imple/tbDictDataServiceImplTest.java` 断言 `TreeNode.type` 来源于 `sn`，保存时保留 `sn/name` 与历史元数据。
- RED 结果：
  - 前端 API 测试先失败于仍在传两参调用；
  - 前端页面测试先失败于仍显示工段/车间固定下拉；
  - 后端测试先失败于 `TreeNode` 缺少 `getType()`。
- 已实现：
  - `TreeNode` 新增 `type` 字段；
  - `tbDictDataServiceImpl#getbyDtId/getbyPId` 将 `sn` 回填到 `type`；
  - `tbDictDataServiceImpl#add` 新增时补默认元数据，编辑时合并已有记录元数据；
  - `DictDataController#add` 的 `AddType` 改为可选；
  - 前端数据配置页面改为 `类型/值` 两个文本输入框，保存提交 `sn/name`；
  - 前端 `addDictData(...)` 取消固定 `AddType` 参数。
- GREEN 验证：
  - `node /Users/jiangyongming/Project/hr/hr_web/tests/data-config-api.test.mjs` 通过；
  - `node /Users/jiangyongming/Project/hr/hr_web/tests/data-config-page.test.mjs` 通过；
  - `mvn -Dtest=tbDictDataServiceImplTest test` 通过，2 个测试 0 failures/errors。

# Progress: 添加排班与社保方案列表分页截断

## 2026-08-24
- 已按用户澄清拆成两个独立问题排查。
- 后端只读复核：
  - `WorkPlanListController#getData(...)` 只接收 `pageSize/pageNum`；
  - `WorkPlanListController#loadIsLast(...)` 接收同一分页字段；
  - `HrmInsuranceSchemeController#index(...)` 使用 `PageEntity`，默认 `limit=15`，`pageType=0` 为不分页。
- 结论：根因在 `hr_web` 请求参数，后端本轮无源码修改。
- 前端已修复并验证，详见 `hr_web/progress.md`。

# Progress: 员工部门明细跨公司加密导出

## 2026-08-24
- 用户要求修改员工管理“下载部门明细”：无全勤员工不累加全勤；行政经理下载五个公司独立 Excel、一个集团总表，统一加密压缩包并提示密码。2026-08-24 继续补充：密码提示需要支持用户一键复制。
- 已读取后端和前端需求/开发文档，确认普通部门明细导出已有多 sheet workbook、成本区、人员总表和前端按钮；本轮在此基础上扩展行政经理跨库 ZIP。
- 已补/修测试：
  - 后端 `HrmEmployeeMapperSqlTest` 锁定 `fullAttendance` 字段；
  - 后端 `CrossDomainFilterTest` 锁定 `X-Archive-Password` 暴露；
  - 后端 `EmployeeDepartmentDetailCrossCompanyExportSupportTest` 锁定公司白名单、上下文恢复、公司名不复用、无伙食集团总表和加密 ZIP；
  - 后端 `HrmEmployeeServiceImplDepartmentDetailCostTest` 锁定行政经理服务分支；
  - 前端 `employee-department-detail-export-api/ui` 锁定 Blob headers 保留和密码提示。
- RED 结果：
  - 后端测试先失败于 Zip4j 枚举导入包名、CORS 未暴露密码头、Mapper 未返回 `fullAttendance`、服务入口无行政经理分支；
  - 前端测试先失败于 `src/api/requset.js` Blob 响应未保留 headers。
- 已实现后端：
  - `HrmEmployeeMapper.xml` 增加 `a.full_attendance as fullAttendance`；
  - `EmployeeDepartmentDetailExportSupport` 现有逻辑只在 `fullAttendance=1` 时追加全勤；
  - `HrmEmployeeServiceImpl#exportDepartmentDetail` 普通角色保持单 Excel，行政经理遍历 `0001/0002/0003/0004/0005`，每库独立构建明细和公司汇总；
  - 新增集团总表进入 ZIP，删除伙食口径；
  - Zip4j AES-256 加密 ZIP，响应头返回 `X-Archive-Password`；
  - `CrossDomainFilter` 暴露 `Content-Disposition, X-Archive-Password`。
- 已实现前端：
  - Axios Blob 响应返回 `{ data, headers }`；
  - 员工页面解析 `x-archive-password` 并提示压缩包打开密码；
  - 密码提示改为 Vue VNode 消息，提供“复制”按钮，优先使用 Clipboard API，失败时降级隐藏文本框复制，并给出中文复制结果反馈；
  - `downloadExcelWithResData` 和请假导出兼容 `res?.data || res`。
- GREEN：
  - `mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,CrossDomainFilterTest,EmployeeDepartmentDetailCrossCompanyExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过 35 个测试；
  - `node tests/employee-department-detail-export-api.test.mjs`、`node tests/employee-department-detail-export-ui.test.mjs`、`node tests/employee-template-download-api.test.mjs`、`node tests/employee-template-download-ui.test.mjs`、`node tests/employee-basic-info-export-api.test.mjs`、`node tests/employee-basic-info-export-ui.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs`、`node tests/salary-tax-additional-template-api.test.mjs`、`node tests/salary-tax-additional-template-ui.test.mjs`、`node tests/upload-attendance-page.test.mjs`、`node tests/overtime-night-utils.test.mjs`、`node tests/overtime-night-api.test.mjs` 通过。
- 已更新后端与前端需求/开发文档、`task_plan.md` 和 `findings.md`。
- 最终验证：
  - `mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,CrossDomainFilterTest,EmployeeDepartmentDetailCrossCompanyExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过 35 个测试；
  - `mvn -DskipTests compile` 通过；
  - 前端下载相关 Node 测试通过；
  - `npm run build` 通过，保留既有 Vue `::v-deep` 和 Vite chunk size warning。
# 2026-08-24 排班矩阵岗位级车间字段

- 已完成需求和代码现状复核，确认两个矩阵都要使用岗位/分配行级“车间”自由文本。
- 已确认不能复用 `GroupID` 或通过考勤组名称推导用户车间；需要新增 `tbplanlist.workshop_name` 并贯穿后端、前端添加排班和排班管理修改排班。
- 当前阶段：准备先补后端/前端 RED 测试，再实现保存、查询、回显、复制和迁移脚本。
