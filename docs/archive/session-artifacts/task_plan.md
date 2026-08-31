# Task Plan: 排班矩阵车间字段自由输入

## Goal
- 添加排班矩阵和排班管理“修改排班”矩阵都支持 `车间` 自由文本输入。
- `车间` 随每个岗位/分配行独立保存，后端持久化到 `tbplanlist.workshop_name`。
- 历史记录没有 `workshop_name` 时继续按考勤组名称回填展示，保证旧数据兼容。

## Phases
1. `in_progress` 补 RED 测试，锁定保存/回显/矩阵合并对 `workshopName` 的行为。
2. `pending` 后端实体、BO/VO、控制器、服务、Mapper 和迁移脚本补齐 `workshopName`。
3. `pending` 前端添加排班与修改排班矩阵增加车间输入框并贯穿提交/回显。
4. `pending` 更新后端和前端需求/开发文档，以及 findings / progress 记录。
5. `pending` 跑定向测试、编译、前端测试、构建和 diff 检查。

## Current Assumptions
- `车间` 是纯自由文本，不做字典或联动校验。
- 休息/调休记录不强制要求车间，但旧数据若只有考勤组名称仍可继续展示。
- 同产品同岗位同时间但车间不同的记录必须保留为不同条目，不能在矩阵里被合并掉。

## Verification
- 待执行。

# Task Plan: 排班管理矩阵多产品多岗位修改排班

## Goal
- 排班管理矩阵“修改排班”在同一员工同一天存在多个岗位、多个生产产品时，按完整 `员工 + 日期 -> assignments[]` 编辑和保存。
- 用户已确认不同产品/岗位可以有不同工作时间，因此每条分配必须独立维护产品、岗位、班次和时间。
- 保存采用完整替换目标员工当天排班，保留共享旧行中的其他员工。

## Phases
1. `complete` 复核当前后端 `queryEmployeeDayShift` 单条首选记录和前端 `dayShiftProductContext` 单产品上下文限制。
2. `complete` 设计 `员工 + 日期 -> assignments[]` 的多分配编辑模型。
3. `complete` 记录完整替换保存语义：先移除目标员工旧排班，再逐条保存新分配，保留共享行其他员工。
4. `complete` 后端新增完整分配 BO/VO、`queryEmployeeDayAssignments`、`saveEmployeeDayAssignments` 和完整替换服务实现。
5. `complete` 前端新增完整分配 API、工具函数和 `Scheduling.vue` 多分配弹窗保存链路。
6. `complete` 更新后端和前端需求/开发文档，并新增设计文档。

## Current Assumptions
- 第一期不改 `tbplanlist` 表结构，继续用 `ProductName/LinkName/UserID` 保存扁平事实。
- 标准产品和岗位 ID 只用于前端回显；历史排班仍按产品名、岗位名保存。
- 休息/调休和工作分配互斥。

## Verification
- 后端：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 69 项。
- 后端：`mvn -DskipTests compile` 通过。
- 前端：`node tests/work-plan-api.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-records-page.test.mjs` 通过。
- 前端：`npm run build` 通过，保留既有 Vue `::v-deep` 和 Vite chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 员工部门明细跨公司加密导出

## Goal
- 员工管理“下载部门明细”在普通角色下保持当前公司单 Excel 下载。
- 登录用户角色严格为 `行政经理` 时，下载加密 ZIP，包含 `hr_0001/hr_0002/hr_0003/hr_0004/hr_0005` 五个公司部门明细 Excel 和一个集团总表。
- 集团总表统计所有五个公司的数据，包括成都 `hr_0002`；集团总表不统计伙食相关数据。
- `薪资待遇` 只有员工 `full_attendance=1` 时才追加全勤金额。
- 前端下载 ZIP 后提示用户压缩包打开密码，并提供一键复制能力。

## Phases
1. `complete` 复核项目文档、既有部门明细导出和跨公司设计文档。
2. `complete` 补 RED 测试：SQL 返回 fullAttendance、CORS 暴露密码头、行政经理服务分支、前端 Blob 头保留和密码提示。
3. `complete` 后端实现：Mapper 字段、行政经理跨库导出、集团总表、AES 加密 ZIP、密码响应头和 CORS 暴露。
4. `complete` 前端实现：Blob 响应保留 headers，部门明细下载解析 `x-archive-password`，提示密码并支持一键复制，兼容其它 Blob 下载入口。
5. `complete` 更新前后端需求/开发文档、findings/progress。
6. `complete` 运行最终后端编译、后端定向测试、前端构建验证。

## Current Assumptions
- 行政经理按 `LoginUserInfo.roleName == "行政经理"` 严格判断，不做 trim 或模糊匹配。
- 公司名优先读取目标租户顶层组织；目标库缺顶层组织时用 `hr_公司ID` 降级，不能复用当前登录公司名。
- 任一目标公司导出失败时请求整体失败，不生成不完整 ZIP。

## Verification
- RED：后端定向测试先失败于 CORS 未暴露 `X-Archive-Password`、Mapper 未返回 `fullAttendance`、服务未进入行政经理跨公司分支。
- GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,CrossDomainFilterTest,EmployeeDepartmentDetailCrossCompanyExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过 35 个测试。
- RED：前端 `node tests/employee-department-detail-export-api.test.mjs` 先失败于 Axios Blob 响应未保留 headers。
- GREEN：前端部门明细与相关下载测试通过。
- GREEN：复制补充后 `node tests/employee-department-detail-export-ui.test.mjs` 覆盖 VNode 消息、复制按钮、`CopyDocument` 图标和复制成功反馈。
- 编译：`mvn -DskipTests compile` 通过。
- 构建：`npm run build` 通过，保留既有 `::v-deep` 和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| Zip4j AES/加密枚举包名编译失败 | 1 | 改为 `net.lingala.zip4j.model.enums.AesKeyStrength/EncryptionMethod`，并移除未使用导入。 |
| 新全勤规则导致旧测试未显式 `fullAttendance` 的员工不再追加全勤 | 1 | 测试数据中需要有全勤的员工显式设置 `fullAttendance=1`，保留 `fullAttendance=2` 不追加的新增测试。 |
| `commons-lang` 版本没有 `StringUtils.defaultIfBlank(...)`，后端 compile 失败 | 1 | 改为 `StringUtils.isBlank(...) ? ... : ...` 的显式兼容写法。 |
| `tests/work-plan-scheduling-records.test.mjs` 旧断言仍要求 `Scheduling.vue` 包含 `queryEmployeeDayShift/dayShiftProductContext/getWorkPlanMatrixCellScheduleMeta` | 1 | 改为新契约断言 `queryEmployeeDayAssignments/saveEmployeeDayAssignments/dayShiftAssignments`，并明确页面不再使用旧单条接口和旧单产品上下文。 |
| 误用 `sh` 执行 Python `session-catchup.py`，shell 将 Python 当 sh 解析 | 1 | 已改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan` 正确执行。 |

# Task Plan: 添加排班人员缺失修复

## Goal
- 添加排班“人员”下拉改为来自员工表中的在职、待离职和离职人员。
- 陈明成这类没有考勤组或没有 `tbattendanceuser` 展示快照的员工仍应能在 `getAllUsers` 中出现。
- 展示缓存需要避开旧的缺人缓存，避免修复后仍读到旧结果。

## Phases
1. `complete` 复现并定位 `groupId=null` 导致的过滤逻辑。
2. `complete` 改为从 `hrm_employee` 按删除标记和入离职状态查询展示人员。
3. `complete` 保留空组员工、按钉钉用户 ID去重并升级展示缓存 key。
4. `complete` 更新需求、开发、findings 和 progress 记录。

## Verification
- `mvn -Dtest=WorkPlanServiceImplTest,HrmAttendanceDataControllerTest test` 通过 62 项。
- `mvn -DskipTests compile` 通过。

# Task Plan: 添加排班岗位多行与全员补充选人

## Goal
- 添加排班时，人员除按岗位配置自动带出外，还能从所有员工中自选补充。
- 岗位列提供 `+` 按钮；每个排班行可按已选生产产品添加多个岗位，并以多行岗位显示，不再把多个岗位挤在同一个下拉框中。
- 用户点击 `+` 新增岗位行时，已在当前排班行选过的岗位不能再出现在其它岗位下拉选项中。
- 人员下拉框同样按当前排班行去重，已被任一岗位行选过的人员不能再出现在其它人员下拉选项中。
- 提交仍沿用既有扁平 `/workPlan/saveAll` payload，每个岗位行展开为独立保存项。

## Phases
1. `complete` 复核现有添加排班行模型、岗位/人员选择工具函数和测试覆盖。
2. `complete` 补 RED 前端测试，锁定岗位多行、全员补充选人、岗位/人员跨行去重和提交展开。
3. `complete` 实现添加排班前端行模型、交互和工具函数调整。
4. `complete` 运行前端聚焦测试和构建验证。
5. `complete` 更新需求/开发文档、findings/progress。

## Current Assumptions
- 去重范围限定在同一条添加排班行内；不同排班行可以选择相同岗位或人员。
- 每个岗位子行独立生成一条保存 payload，`linkName` 使用该岗位名称，`userId` 使用该岗位子行最终选择的人员集合。
- 岗位配置员工只作为默认带出；人员下拉候选仍允许来自全员列表，以支持临时补充。

## Verification
- RED：`node tests/work-plan-utils.test.mjs` 失败于 `getAvailableWorkPlanPositionOptions` 未导出。
- GREEN：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/workplan-product-management.test.mjs` 均通过。
- 构建：`npm run build` 通过，保留既有 Vue `::v-deep` 和 Vite chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 添加排班与社保方案列表分页截断

## Goal
- 复核后端分页契约，确认两个用户反馈是否由后端保存限制导致。
- 将根因记录到后端项目文档，避免后续误改保存链路。

## Phases
1. [complete] 读取后端 `/workPlan/getData`、`/workPlan/loadIsLast`、`/hrmInsuranceScheme/index` 和分页实体。
2. [complete] 对比前端调用参数，确认截断点在前端请求参数。
3. [complete] 前端修复后，后端文档记录接口契约和排查结论。

## Current Evidence
- `/workPlan/getData` 后端只读 `pageSize/pageNum`，前端旧调用传 `page/limit`。
- `/hrmInsuranceScheme/index` 后端 `PageEntity` 默认 `limit=15`，前端旧调用传 `{}`。
- 保存接口未参与这两个现象。

## Verification
- 后端本轮未改源码，未运行 Maven。
- 前端验证记录见 `/Users/jiangyongming/Project/hr/hr_web/progress.md`。

# Task Plan: 排班产品岗位排序与多岗位提交

## Goal
- 新增生产产品、添加岗位时，排序号根据数据库内已有最大排序自动生成。
- 生产产品管理页右侧岗位区域支持拖动排序，并把新排序持久化到后端。
- 生产产品管理页左侧生产产品列表也支持拖动排序，并把新排序持久化到后端。
- 产品/岗位拖动排序过程中提供被拖动行、目标行和插入位置的视觉反馈。
- 添加排班时，一行可以同时给多个岗位排班；点击提交后按新的多岗位结构持久化到 `workPlan/saveAll` 保存链路。

## Phases
1. `complete` 复核现有产品岗位配置与添加排班提交结构。
2. `complete` 补 RED 测试，锁定岗位排序自动生成、拖拽排序持久化、一行多岗位 payload 展开保存。
3. `complete` 实现后端排序生成/更新接口与最小前端拖拽交互。
4. `complete` 实现添加排班一行多岗位选择与提交结构落库。
5. `complete` 运行聚焦测试、编译/构建验证。
6. `complete` 更新前后端需求/开发文档、findings/progress。

## Current Assumptions
- 排班事实表仍不新增字段，继续用 `tbplanlist.ProductName` 保存生产产品名称、`tbplanlist.LinkName` 保存岗位名称。
- “一行多岗位”保存时需要在提交前展开为多个排班保存项，确保每个岗位都有自己的 `linkName` 且岗位员工能被正确带入。
- 拖动排序只调整同一产品下岗位的 `sort`，不跨产品移动岗位。

## Verification
- RED：`mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest test` 失败于 `SortWorkPlanPositionBO` 缺失；`node tests/workplan-product-management.test.mjs` 失败于缺少 `saveWorkPlanPositionSort`；`node tests/work-plan-utils.test.mjs` 失败于一行多岗位仍只生成 1 条 payload。
- GREEN：`mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanListControllerTest,WorkPlanServiceImplTest test` 通过，73 个测试 0 failures/errors。
- GREEN：`node tests/workplan-product-management.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs` 均通过。
- 2026-08-23 追加 RED/GREEN：后端测试先失败于 `SortWorkPlanProductBO` 缺失，前端测试先失败于缺少 `saveWorkPlanProductSort`；实现后 `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest test` 通过 10 个测试，`node tests/workplan-product-management.test.mjs` 通过。
- 编译/构建：`mvn -DskipTests compile` 与 `npm run build` 通过，保留既有 Maven POM warning、Vue `::v-deep` warning 和 Vite chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 生产产品管理菜单权限

## Goal
- 让 `cdadmin/123` 从 `http://127.0.0.1:8081/` 登录后能在考勤管理下看到并访问“生产产品”页面。
- 新页面前端路由、后端菜单表、角色菜单授权和接口权限映射保持一致。
- 本机 8081 静态包和当前开发数据库同步到可验收状态。

## Phases
1. `complete` 复现 `cdadmin` 登录返回的 `menuTree`，确认是否缺少 `/hrm/attendance/workplanProduct`。
2. `complete` 对照已有考勤子菜单，确认 `tbmenu/tbrolemenu` 配置和 `ApiPermissionPathSupport` 映射缺口。
3. `complete` 补 RED 测试锁定生产产品菜单、角色授权脚本和接口权限路径。
4. `complete` 实现最小权限配置修复，并应用到本机/dev 数据库。
5. `complete` 验证当前 8081 静态包已包含路由、菜单可见、接口可访问。

## Current Assumptions
- 前端源码已有 `/hrm/attendance/workplanProduct` 路由，但侧边栏受后端登录 `menuTree` 控制。
- 新增 `WorkPlanProductController` 的 `/workPlanProduct/**` 接口也应纳入菜单权限，否则页面可见后接口可能被权限拦截。
- `cdadmin` 当前角色只会看到已授权 `tbrolemenu` 中存在的菜单，补菜单记录后还需要补角色菜单授权并重新登录刷新 token/localStorage。

## Verification
- RED：`mvn -Dtest=ApiPermissionPathSupportTest,WorkPlanProductMigrationSqlTest test` 失败于 `/workPlanProduct/queryTree` 未映射菜单和菜单权限 SQL 文件缺失。
- GREEN：`mvn -Dtest=ApiPermissionPathSupportTest,WorkPlanProductMigrationSqlTest test` 通过，7 个测试。
- 回归：`mvn -Dtest=ApiPermissionPathSupportTest,MenuPermissionSupportTest,WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest test` 通过，19 个测试。
- 前端契约：`node tests/workplan-product-management.test.mjs` 通过。
- 运行时：`cdadmin/123` 通过 `127.0.0.1:8081` 重新登录后 `menuTree` 包含 `/hrm/attendance/workplanProduct`；`POST /api/hrsystem/workPlanProduct/queryTree` 返回 `code=200`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `/workPlanProduct/queryTree` 返回 `SQLGrammarException` | 1 | 确认 `hr_0002` 未执行生产产品三表建表脚本；已在 `hr_0001` 至 `hr_0005` 执行 `2026-08-22_hrm_workplan_product_position_employee.sql`。 |

# Task Plan: 排班生产产品/岗位/员工配置

## Goal
- 新增独立生产产品配置模块，支持 `生产产品 -> 岗位 -> 员工`。
- 添加排班时按生产产品加载岗位，按岗位加载岗位员工；岗位没有配置员工时保留原有全员选择。
- 排班管理修改排班弹窗展示生产产品、岗位、车间。

## Phases
1. `complete` 读取项目与前端文档，确认排班事实表继续复用 `ProductName/LinkName`。
2. `complete` 补 RED 测试：后端产品树/保存/迁移 SQL、前端 API/工具/页面/添加排班源码契约、排班管理展示字段。
3. `complete` 实现后端产品、岗位、岗位员工配置模块和迁移 SQL。
4. `complete` 实现前端生产产品管理页、添加排班产品/岗位/员工联动、排班管理只读展示。
5. `complete` 运行聚焦测试、编译/构建验证。
6. `complete` 更新前后端需求/开发文档、findings/progress。

## Current Assumptions
- 新配置只用于前端排班录入时的候选项和默认员工集合，不改变排班保存表结构。
- `tbplanlist.ProductName` 表示生产产品，`tbplanlist.LinkName` 表示岗位。
- 车间展示从本地排班 `GroupID` 对应考勤组名称解析。

## Verification
- `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanServiceImplTest#queryEmployeeDayShift_shouldExposeProductPositionAndWorkshopForEditDialog test` 通过，6 个测试 0 failures/errors。
- `node tests/workplan-product-management.test.mjs` 通过。
- `node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/clock-overview-utils.test.mjs` 均通过。
- `mvn -DskipTests compile` 通过；`npm run build` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `tests/work-plan-records-page.test.mjs` 仍按旧工段/车间字典分流断言 `flattenDictNodes` | 1 | 按新需求更新测试，改为断言 `queryWorkPlanProductTree`、产品/岗位列和员工清空/删除函数。 |
| `tests/clock-overview-utils.test.mjs` 严格对象相等未包含新增 `productName/positionName/workshopName` | 1 | 更新测试期望，并覆盖修改排班弹窗新字段透传。 |

# Task Plan: 社保详情一键设置保险金额

## Goal
- 社保管理社保详情页在“高级筛选”按钮后新增“一键设置/取消保险金额”按钮。
- 表格姓名前复选框支持选择员工；选中后在表格上方提供同样的“一键设置/取消保险金额”按钮，只作用于选中员工。
- 设置时：
  - 不再受员工社保方案中的“医疗长期护理保险”启用状态限制，个人社保费需累加“基本工资金额设置”的“长期护理保险金额”。
  - 不再受员工社保方案中的“医疗长期护理保险”启用状态限制，公司社保费需累加“基本工资金额设置”的“大额医疗保险金额”。
- 取消时，对目标员工的个人社保费和公司社保费不再累加上述两项基本工资设置金额。
- 社保管理列表和界面上方合计的个人社保、公司社保也必须按该员工级设置/取消状态一致计算。

## Phases
1. `complete` 定位社保详情页、月度员工记录查询/更新链路、社保管理列表合计 SQL 和现有测试。
2. `complete` 补 RED 测试，锁定全员按钮、选中员工按钮、员工级设置状态和列表/详情合计口径。
3. `complete` 实现后端员工范围更新接口与合计计算逻辑。
4. `complete` 实现前端按钮、选中态入口和刷新逻辑。
5. `complete` 运行聚焦测试、编译/构建验证。
6. `complete` 更新需求、开发、findings 与 progress 文档。

## Current Assumptions
- “设置/取消”需要持久化到社保月度员工记录，否则列表、详情、刷新和后续薪资使用口径无法稳定一致。
- 个人社保费累加长期护理金额、公司社保费累加大额医疗金额只以员工级 `include_salary_basic_insurance_amount=1` 为准，不再判断 `type=12 / 医疗长期护理保险` 的 `is_enabled`。
- 当前 `hrm_salary_basic.long_term_care_insurance_amount` 默认 `3`、`large_medical_insurance_amount` 默认 `15`，实际值以后端最新基本工资设置为准。
- 前端按钮文案使用同一个状态入口，具体是“设置”还是“取消”可由当前目标员工中是否已启用累加来决定；若后端接口返回明确状态，以后端为准。

## Verification
- RED：`mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 在 testCompile 阶段失败于 `UpdateInsuranceSalaryBasicAmountBO` 缺失。
- RED：`node tests/insurance-detail-amount-actions.test.mjs` 失败于详情 API 未暴露更新接口。
- GREEN：`mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 通过，11 个测试 0 failures/errors。
- 回归：`mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest test` 通过，22 个测试 0 failures/errors。
- GREEN：`node tests/insurance-detail-amount-actions.test.mjs`、`node tests/insurance-scheme-projects.test.mjs`、`node tests/insurance-advanced-filter.test.mjs`、`node tests/insurance-scheme-usage-tooltip.test.mjs` 均通过。
- 编译/构建：`mvn -DskipTests compile` 通过；`npm run build` 通过，保留既有 Vue `::v-deep` warning 和 Vite chunk size warning。
- 检查：本轮后端目标文件 `git diff --check -- ...` 无输出；前端目标文件 `git -C /Users/jiangyongming/Project diff --check -- ...` 无输出；前端目标文件 `rg -n "[[:blank:]]$" ...` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 误用 `sh` 执行 Python `session-catchup.py`，导致脚本被 shell 当作 sh 脚本解析 | 1 | 已改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py ...` 正确执行。 |
| 查询 `hrm_insurance_month_record` 时误以为主表有个人/公司社保金额字段，返回 `Unknown column 'personal_insurance_amount'` | 1 | 已 `SHOW COLUMNS` 确认金额在 `hrm_insurance_month_emp_record` 员工明细表，并改查员工明细汇总。 |
| 检查 8081 首页资源时正则引号写错，触发 zsh bad pattern / rg unclosed character class | 1 | 已改用直接 `rg` 本机 `html/assets` 静态包，确认 `InsuranceDetail-DWbf-8Nm.js` 包含新接口和 `irecordId/iempRecordIds`。 |
| 误发占位命令 `SHOW_COLUMNS_PLACEHOLDER`，返回 `command not found` | 1 | 已改用实际 SQL 查询 `hrm_insurance_scheme` 字段和方案名称，确认方案为 `成都（一）`。 |
| `rg` 查询模式中包含反引号且外层使用双引号，shell 先执行了反引号内容 | 1 | 后续查询避免在双引号模式中直接放反引号；本次已通过前后文 `sed` 和实际 SQL 完成复核。 |

# Task Plan: 社保方案医疗长期护理保险项

## Goal
- 在社保方案新建和编辑页面的“生育保险”下方新增“医疗长期护理保险”一行。
- 新字段需参与前端表单回显、提交、编辑保存。
- 后端需接收该字段并保存入库，列表/编辑详情再次打开时能读回。
- 该行需要支持启用/禁用状态，状态随社保方案保存入库并在编辑时回显。
- 社保方案与月度员工参保项目合计不再从“基本工资金额设置”自动累加大额医疗保险金额或长期护理保险金额，只按已保存且启用的项目行汇总。

## Phases
1. `complete` 定位社保方案前端页面、后端实体/BO/VO/Mapper 和数据库字段结构。
2. `complete` 补 RED 测试，锁定新增险种、启用状态入库回显、启用过滤和取消基本工资设置自动累加。
3. `complete` 实现前端启用/禁用按钮与后端持久化字段、迁移脚本。
4. `complete` 调整社保方案/月度项目合计 SQL，只汇总启用项目行。
5. `complete` 运行聚焦测试、编译/构建验证。
6. `complete` 更新需求、开发、findings 与 progress 文档。

## Current Assumptions
- “医疗长期护理护理保险项”按业务语义规范显示为“医疗长期护理保险”。
- 新项与现有“生育保险”使用相同字段形态和保存链路；如果现有险种是个人/公司比例或金额成对字段，新项也保持同样结构。
- 启用状态采用项目行级字段 `is_enabled`，默认 `1` 启用，`0` 禁用；旧数据为空时按启用兼容。
- 需要新增幂等 SQL 脚本给 `hrm_insurance_project` 与 `hrm_insurance_month_emp_project_record` 补 `is_enabled` 字段。

## Verification
- RED：`mvn -Dtest=HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest test` testCompile 失败于 `isEnabled` getter/setter 缺失。
- RED：`node tests/insurance-scheme-projects.test.mjs` 失败于 `new long-term care project should default to enabled`。
- GREEN：`mvn -Dtest=HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordMapperXmlTest test` 通过，18 个测试 0 failures/errors，包含保存链路中 `医疗长期护理保险 isEnabled=0` 入库状态不被覆盖、旧方案虚拟补行在月度参保编辑中落库的回归。
- GREEN：`node tests/insurance-scheme-projects.test.mjs` 通过。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven duplicate dependency / systemPath warnings。
- 前端回归：`node tests/insurance-advanced-filter.test.mjs` 与 `node tests/insurance-scheme-usage-tooltip.test.mjs` 通过。
- 构建：`npm run build` 通过，保留既有 Vue `::v-deep` warning 和 Vite chunk size warning。
- 补丁检查：前后端目标文件 `git diff --check -- ...` 均无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 员工合同无固定期限结束日期

## Goal
- 员工管理手工新增/编辑合同与合同导入支持 `合同类型=无固定期限劳动合同` 时 `合同结束日期` 为空。
- 非无固定期限合同继续要求合同结束日期必填，并继续校验结束日期不能早于开始日期。
- 展示和导出中，无固定期限合同的合同到期/结束日期不显示占位远期日期，优先显示“无固定期限”或留空，避免误导为真实到期日。

## Phases
1. `complete` 读取当前合同服务、导入、导出和测试结构，确认最小修改点。
2. `complete` 补 RED 后端测试，覆盖手工保存、合同导入、合同信息展示和员工导出显示。
3. `complete` 实现无固定期限合同结束日期为空的服务层规则和展示/导出格式。
4. `complete` 运行聚焦测试与编译验证。
5. `complete` 更新需求、开发、findings 与 progress 文档。

## Current Assumptions
- `hrm_employee_contract.end_time` 可保存 `NULL`；若目标库实际为 `NOT NULL`，需要补充 DDL 迁移。
- 现有 `term` 为整数年，不适合表达“无固定期限”；无固定期限合同保存时将 `term` 置空，展示层用合同类型判断文本。
- 合同列表/导出里需要让业务用户看出“无固定期限”，但高级筛选的合同结束日期仍只筛选有结束日期的合同。

## Verification
- RED：窄路径 `HrmEmployeeContractServiceImplTest` 失败于旧逻辑强制 `合同结束日期` 必填；导出测试失败于 SQL 缺少合同类型别名且显示为空。
- GREEN：`mvn -Dtest=HrmEmployeeContractServiceImplTest,EmployeeBasicInfoExportSupportTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest test` 通过，38 个测试 0 failures/errors。
- GREEN：`mvn -DskipTests compile` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 员工其他补助导入导出与保存

## Goal
- 在员工花名册模板、导入保存、员工新建/详情编辑，以及部门明细下载中新增“其他补助”。
- 字段按员工动态字段保存，类型为两位小数数字，位置紧邻“职务补助”，父表头为“薪酬福利”。
- 部门明细“薪资待遇”展示和成本口径纳入“其他补助”。

## Phases
1. `complete` 复核文档与现有薪资动态字段实现，定位模板、导入、员工保存和部门明细导出链路。
2. `complete` 先补 RED 测试，覆盖模板列、导入保存、员工保存和部门明细薪资待遇。
3. `complete` 实现“其他补助”动态字段定义、数值格式化、导入/表单保存、导出展示。
4. `complete` 运行定向回归测试和编译验证。
5. `complete` 更新需求/开发文档、findings/progress。

## Current Assumptions
- “其他补助”与既有“固定绩效”“职务补助”一样使用员工动态字段体系，不新增 `hrm_employee` 物理列。
- 员工管理前端在单独的 `hr_web` 项目中；本轮先完成后端链路和后端可验证的接口/导出行为，若本仓库包含前端引用再同步修改。
- 非数字输入应沿用既有模板参数错误处理；有效数字统一保存为两位小数字符串。

## Verification
- RED：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 失败于 `HrmEmployeeChangeRecord#setOtherSubsidy(...)` 缺失。
- RED：`node tests/employee-salary-fields-ui.test.mjs` 失败于新增员工表单缺少 `otherSubsidy`。
- RED：`node tests/employee-list-row-normalizer.test.mjs` 失败于列表行未归一化 `other_subsidy`。
- GREEN：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 通过，44 个测试。
- GREEN：`node tests/employee-salary-fields-ui.test.mjs`、`node tests/employee-list-row-normalizer.test.mjs` 通过。
- 回归：`node tests/employee-form-field-removal.test.mjs`、`node tests/employee-edit-save-regression.test.mjs`、`node tests/employee-company-age-ui.test.mjs` 通过。
- 构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 后端组合测试先失败于 `HrmEmployeeServiceImplImportEmployeeTest#importEmployee_shouldUpdateSameNameEmployeeWhenIdNumberMatchesAndPhoneChanged`，错误 `YL-001工号已存在` | 1 | 按已有失败测试修复导入匹配：`姓名+手机号` 未命中时，补按 `姓名+身份证号` 匹配未删除员工，避免手机号变更被误判为新员工。 |
| 误用 `sh` 执行 Python `session-catchup.py`，导致脚本被 shell 当作 sh 脚本解析 | 1 | 已改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py ...` 正确执行。 |

# Task Plan: 社保管理名单与方案使用人数悬浮展示

## Goal
- 社保管理月报卡片不再直接展开本月参保/停保人员名单。
- 鼠标悬停或键盘聚焦“本月参保人数”“本月停保人数”的人数数字时展示对应人员名单。
- 保留 `insuredNum ?? num` 的参保人数兼容口径，停保人数继续使用 `stopNum`；人员名单为空时悬浮内容显示 `--`。
- 社保方案管理表格的“使用人数”列继续显示 `useCount`，悬停后显示采用当前方案的具体人员名单和人数。
- 后端社保方案列表接口 `/hrmInsuranceScheme/index` 需返回 `useEmployeeNames`，供前端 tooltip 使用；无人员时显示 `--`。

## Phases
1. `complete` 读取后端和前端需求/开发文档，确认当前字段、页面和样式约束。
2. `complete` 补 RED 前后端测试，锁定名单不直接渲染而由 tooltip 展示，并锁定方案使用人员字段。
3. `complete` 修改后端 VO 与社保方案列表 SQL，返回 `useEmployeeNames`。
4. `complete` 修改社保月报页和社保方案管理页，复用 Element Plus tooltip 收起名单区域。
5. `complete` 运行聚焦前后端测试、编译/构建验证。
6. `complete` 更新需求、开发、findings 与 progress 文档。

## Current Assumptions
- 后端已经返回月报 `insuredEmployeeNames` 与 `stoppedEmployeeNames`；方案管理还需要新增 `useEmployeeNames`。
- 该页面是后台数据管理页，交互应保持紧凑；人数仍直接显示，名单只作为辅助明细。
- 移动端没有 hover 时，Element Plus tooltip 的 focus/click 触发能力可作为可访问兜底。

## Verification
- RED：`node tests/insurance-advanced-filter.test.mjs` 失败于缺少月报人数 tooltip。
- RED：`node tests/insurance-scheme-usage-tooltip.test.mjs` 失败于方案管理“使用人数”没有 tooltip/useEmployeeNames。
- RED：`mvn -Dtest=HrmInsuranceSchemeMapperXmlTest,InsuranceSchemeListVOTest,HrmInsuranceSchemeServiceTest test` 先失败于 `InsuranceSchemeListVO` 缺少 `useEmployeeNames` getter/setter，追加截断保护后失败于 `HrmInsuranceSechemeMapper#setGroupConcatMaxLen()` 缺失。
- GREEN：`mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceSchemeServiceTest,InsuranceSchemeListVOTest test` 通过，13 个测试 0 failures/errors。
- GREEN：`mvn -DskipTests compile` 通过。
- GREEN：`node tests/insurance-advanced-filter.test.mjs && node tests/insurance-scheme-usage-tooltip.test.mjs && node tests/insurance-delete-feedback.test.mjs && node tests/insurance-create-next-month.test.mjs && node tests/insurance-progress-utils.test.mjs` 通过。
- GREEN：`npm run build` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 社保管理本月参保/停保人员名单

## Goal
- 社保管理主列表接口返回本月参保人数统计，并能返回本月参保人员、停保人员姓名。
- 保留旧字段 `num` 与 `stopNum` 兼容现有详情/前端逻辑，同时新增语义更明确的 `insuredNum`。
- 列表统计口径继续以 `hrm_insurance_month_emp_record.status=1` 为本月参保、`status=0` 为本月停保。

## Phases
1. `complete` 读取项目文档、定位社保月报列表接口、SQL 与前端调用链路。
2. `complete` 补 RED 测试，锁定列表 SQL 返回参保人数别名和参保/停保人员姓名。
3. `complete` 实现后端 VO、Mapper 与 Service 最小改动。
4. `in_progress` 运行聚焦测试、编译验证和空白检查。
5. `complete` 更新需求/开发文档、findings 与 progress。

## Current Assumptions
- `status=1` 表示本月参保，`status=0` 表示本月停保。
- 人员名单来源为同一社保月报下员工明细关联的 `hrm_employee.employee_name`，按姓名用 `、` 拼接。
- 已完成归档的月报保留历史员工；未完成月报仍沿用现有口径排除已删除员工。

## Verification
- RED：`mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest test` 失败于列表 SQL 缺少 `insuredNum` 和人员名单字段。
- RED：`mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 在 testCompile 阶段失败于 `HrmInsuranceMonthRecordMapper` 缺少 `setGroupConcatMaxLen()`。
- GREEN：`mvn -Dtest=HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest test` 通过，7 个测试 0 failures/errors。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: cfy 登录 TooManyResultsException

## Goal
- 修复使用 `cfy` 账号登录时报错 `nested exception is org.apache.ibatis.exceptions.TooManyResultsException: Expected one result (or null) to be returned by selectOne(), but found: 2`。
- 查明登录链路中哪个 `selectOne()` 查询在 `cfy` 场景返回 2 条记录。
- 用测试锁定重复账号或重复员工/用户映射场景，避免再次抛 MyBatis 原始异常给用户。

## Phases
1. `complete` 读取项目文档、恢复会话上下文并记录本轮计划。
2. `complete` 定位 `/hrsystem/login` 入口、账号查询 Mapper/Service 和 `selectOne()` 调用点。
3. `complete` 查找同类工作代码，确认登录账号唯一性或公司/租户限定口径。
4. `complete` 补 RED 测试复现 `cfy` 重复结果场景。
5. `complete` 实现最小修复并运行聚焦回归。
6. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- 当前错误来自登录阶段某个 MyBatis-Plus `selectOne()` 或 mapper 方法期望唯一记录，但账号 `cfy` 在当前查询条件下命中了 2 条。
- 不能简单改为取第一条，除非能证明查询排序和业务唯一性明确；优先修复查询条件或返回可理解的业务错误。

## Verification
- RED：`mvn -Dtest=LoginControllerTest,TbLoginUserServiceValidationTest,TbLoginUserMapperSqlTest test` 在 testCompile 阶段失败，缺少 `getCompanyIdsByUserName(...)` 与 `findSystemCompanyIdsByAccount(...)`。
- GREEN：同一命令通过，13 个测试 0 failures/errors；保留既有 Maven POM/dependency warnings，且 `successResult.raiseException(...)` 对预期错误分支会打印异常堆栈。
- Code review RED/GREEN：`mvn -Dtest=TbLoginUserServiceValidationTest#resolveCompanyId_shouldPreferCurrentTenantContextOverRequestCompanyId test` 先失败于返回请求体 `0001`，修复后通过。
- Final：`mvn -Dtest=LoginControllerTest,TbLoginUserServiceValidationTest,TbLoginUserMapperSqlTest test` 通过，14 个测试 0 failures/errors。
- Final：`mvn -Dtest=ApiPermissionPathSupportTest,MenuPermissionSupportTest,LoginControllerTest,TbLoginUserControllerTest test` 通过，17 个测试 0 failures/errors。
- Final：`mvn -DskipTests compile` 通过。
- Final：目标文件 `git diff --check -- ...` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 误用 `sh session-catchup.py` 导致 Python 脚本被 shell 解释并报语法错误 | 1 | 已改用 `python3 session-catchup.py /Users/jiangyongming/Project/hr/hainan` 正确执行。 |

# Task Plan: 薪资档案在职离职卡片筛选

## Goal
- 薪资档案页面新增两个卡片式选项卡：`在职` 与 `离职`。
- 页面默认展示在职员工薪资档案。
- 点击 `在职` 时按后端既有 `status=11` 查询，即员工 `entry_status in (1,3)`。
- 点击 `离职` 时按后端既有 `status=15` 查询，即员工 `entry_status=4`。
- 切换卡片时重置到第一页并重新查询，不影响姓名/工号搜索和分页。

## Phases
1. `complete` 读取项目需求/开发文档，定位薪资档案页面、API、后端 DTO 和 SQL。
2. `complete` 补 RED 前端测试，锁定默认在职、两张卡片、切换离职重置页码并提交状态参数。
3. `complete` 实现薪资档案页面卡片式状态筛选。
4. `complete` 运行聚焦前端测试、构建验证和必要后端 SQL 保护检查。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- 后端 `QuerySalaryArchivesListDto.status` 已支持员工状态筛选；`11` 表示在职，`15` 表示离职。
- 本轮不新增后端接口或数据库字段，只复用现有 `/hrmSalaryArchives/querySalaryArchivesList` 查询参数。
- “在职/离职”筛选按员工入离职状态 `entry_status`，不是员工试用/正式状态 `status`。

## Verification
- RED：`node tests/salary-archives-status-filter.test.mjs` 失败于 `salary archives page should define status filter cards`，证明现有页面没有在职/离职卡片筛选。
- GREEN 前端：`node tests/salary-archives-status-filter.test.mjs` 通过。
- 后端 SQL 保护：`mvn -Dtest=HrmSalaryArchivesMapperSqlTest test` 通过，1 个测试 0 failures/errors。
- 前端构建：`npm run build` 通过，保留既有 Vue `::v-deep` deprecation 和 Vite chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 职务补助与花名册表头修复

## Goal
- 下载员工花名册模版时，`固定绩效` 和新增 `职务补助` 均位于已有 `薪资等级` 右侧，并归入 `薪酬福利` 父表头。
- 修复运行时插列导致的重复父表头问题：父表头需要随子表头范围合并/调整，不能出现多个独立的同名 `薪酬福利`。
- `职务补助` 为两位小数数字类型，支持花名册导入保存、员工新增/再入职、员工详情基本信息编辑和员工列表修改类弹窗保存入库。
- 部门明细 `薪资待遇` 需在固定薪资、固定绩效、全勤之外加入 `职务补助`；部门固定薪资成本同步纳入该金额，保持明细和汇总一致。
- 下载部门明细中 `薪资待遇` 累加全勤时，若该员工全勤金额为 `100`，不再追加全勤段。

## Phases
1. `complete` 复核文档、模板表头结构、动态字段保存链路、部门明细全勤追加逻辑和前端员工表单字段模型。
2. `complete` 补 RED 测试覆盖模板父表头合并/命名、`职务补助` 数字列、导入保存、员工接口字段、部门明细全勤=100 不追加、前端字段展示/提交。
3. `complete` 实现后端模板插列和父表头合并修复。
4. `complete` 实现后端动态字段定义、导入规范化和员工保存链路。
5. `complete` 实现部门明细全勤追加例外。
6. `complete` 实现前端员工表单、详情兜底和列表操作弹窗字段。
7. `complete` 运行聚焦后端测试、前端静态测试、编译/构建和补丁检查。
8. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- `职务补助` 和 `固定绩效` 一样使用员工动态字段保存到 `hrm_employee_field/hrm_employee_data`，不新增 `hrm_employee` 物理列。
- `职务补助` 导入和编辑保存为两位小数字符串，例如 `123.4` 保存为 `123.40`。
- 模板中已有 `薪资等级`，`固定绩效` 与 `职务补助` 都应插在该列右侧；最终顺序暂定为 `薪资等级、固定绩效、职务补助`。
- “全勤为100的员工不用累加”按部门明细导出的实际全勤追加金额判断；普通全勤配置为 `100` 或员工级解析到 `100` 时，不追加到 `薪资待遇` 文本。
- `薪资待遇` 展示顺序为 `固定薪资(固定)+固定绩效(绩效)+职务补助(职务补助)+全勤(全勤)`，空项不展示。

## Verification
- RED 后端：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 先失败于 `dutySubsidy` BO/变更记录字段缺失、模板缺少 `职务补助`/父表头合并断言和部门明细 `职务补助` 展示缺失。
- RED 前端：`node tests/employee-salary-fields-ui.test.mjs` 先失败于新增/再次入职表单缺少 `dutySubsidy` 初始字段和输入控件。
- GREEN 后端聚焦：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 通过，41 个测试 0 failures/errors。
- 后端组合回归：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest,EmployeeBasicInfoExportSupportTest test` 通过，46 个测试 0 failures/errors。
- 后端编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM duplicate/systemPath warnings。
- 前端聚焦：`node tests/employee-salary-fields-ui.test.mjs` 通过。
- 前端员工管理回归：`node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 Vue `::v-deep` deprecation 和 Vite chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 部门明细薪资待遇累加全勤奖

## Goal
- 员工管理“下载部门明细”时，各部门明细行的 `薪资待遇` 需要在既有固定薪资/固定绩效展示基础上累加全勤奖。
- 全勤奖金额来源为最新“基本工资金额设置”中的普通员工全勤金额与领导全勤金额。
- 试用期员工不在 `薪资待遇` 列累加全勤奖。

## Phases
1. `complete` 复核项目需求/开发文档、现有部门明细导出实现和基本工资设置全勤金额读取链路。
2. `complete` 补 RED 测试覆盖普通员工、领导员工和试用期员工三类薪资待遇展示。
3. `complete` 最小修改部门明细导出数据流，读取基本工资设置全勤金额并按员工身份追加展示。
4. `complete` 运行部门明细聚焦测试和必要编译验证。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- 本轮只调整部门明细 Excel 明细行 `薪资待遇` 展示；不改变薪资核算工资项 `40102 / 全勤奖` 的生成逻辑。
- “试用期员工”按员工表 `status=2` 判断；这与部门 sheet 顶部“试用期人员”统计口径一致。
- 普通/领导全勤金额沿用薪资核算既有领导岗位识别与基本工资设置兜底规则，避免另造一套判断。

## Verification
- RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldAddBasicSalaryFullAttendanceToSalaryTreatmentExceptProbationEmployees test` 在 testCompile 阶段失败，原因是 `EmployeeDepartmentDetailExportSupport#buildWorkbook(...)` 还没有普通/领导全勤金额入参。
- GREEN：同一命令通过，1 个测试 0 failures/errors。
- 聚焦回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，14 个测试 0 failures/errors。
- 部门明细组合回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，25 个测试 0 failures/errors。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 补丁检查：`git diff --check -- ...` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 下载部门明细统计与总表修复

## Goal
- 修复员工管理“下载部门明细”导出的部门 sheet 顶部统计和 `人员总表` 汇总页。
- 部门 sheet 不再输出“专业相关/专业不相关”描述。
- “一年以上实习生”按员工表中实习期一年以上员工统计；“正式老员工”按员工表正式员工人数统计；“试用期人员”按员工表试用期人数统计。
- 月社保成本、月公积金成本分别汇总本次导出的各部门在职员工公司缴纳部分。
- `人员总表` 参考 `/Users/jiangyongming/Desktop/农谷导入数据/各部门现有人员明细表(更新版).xlsx` 的总表结构，去掉伙食成本，其他数据从各部门 sheet 汇总结果获取。

## Phases
1. `complete` 复核项目需求/开发文档、参考 Excel、现有部门明细导出实现和测试。
2. `complete` 补 RED 测试覆盖专业汇总文案删除、实习/正式/试用统计口径、人员总表结构和成本汇总口径。
3. `complete` 最小修改 `EmployeeDepartmentDetailExportSupport` 与必要的服务/SQL 取数字段。
4. `complete` 运行部门明细聚焦测试、相关 mapper/service 测试和编译验证。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- “实习期为一年以上”优先解释为员工表 `status=3/实习` 且 `probation >= 12`，因为员工表现有“试用期/实习期时长”字段为 `probation`，单位是月。
- “正式老员工”本轮按用户明确口径统计员工表 `status=1/正式` 的员工数，不再要求司龄一年以上。
- “专业汇总”区域仍保留参考表位置，但不再输出“专业相关/专业不相关”或替代描述。
- `人员总表` 汇总值全部由本次导出的部门汇总对象累加，不额外查询伙食成本。

## Verification
- RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 失败，命中 `probation` 未查询、人员总表结构不同、专业相关/不相关文案仍存在。
- GREEN：同一命令通过，21 个测试 0 failures/errors。
- 回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，24 个测试 0 failures/errors。
- 编译：`mvn -DskipTests compile` 通过。
- 补丁检查：`git diff --check -- ...` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `summarySheet_shouldMatchReferenceTotalSheetWithoutMealCost` 首次 GREEN 阶段期望财务部合计写错 | 1 | 复核公式为固定薪资 `4400.5` + 绩效 `800` + 社保 `700.25` + 公积金 `200`，修正测试期望为 `6100.75`。 |

# Task Plan: 员工新增编辑薪资字段

## Goal
- 员工新增和编辑表单都增加 `薪资等级` 输入框和 `固定绩效` 输入框。
- `薪资等级` 按文本保存。
- `固定绩效` 按带两位小数的数字保存。
- 新增员工和修改员工后，两个字段都能保存入库，并在再次编辑时回显。

## Phases
1. `complete` 定位员工新增/编辑前端表单、提交 API、后端保存链路和现有动态字段模型。
2. `complete` 补 RED 测试，锁定新增/编辑保存 `薪资等级` 与两位小数 `固定绩效` 的行为。
3. `complete` 实现前端输入、校验、回显和提交。
4. `complete` 实现或补齐后端保存入库链路，优先复用员工动态字段能力。
5. `complete` 运行聚焦验证并更新需求、开发、发现和进度文档。

## Current Assumptions
- `薪资等级` 和 `固定绩效` 继续作为员工动态字段保存到 `hrm_employee_field/hrm_employee_data`，不新增 `hrm_employee` 物理列。
- `固定绩效` 字段定义应保持 `DECIMAL + precisions=2`，值保存为两位小数字符串，例如 `1234.50`。
- 若页面已有动态字段渲染能力，应优先通过字段配置/默认字段补齐，而不是新增一套平行保存接口。

## Verification
- RED 后端：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 在 testCompile 阶段失败，原因是 `AddEmployeeBO` 与 `AddEmployeeFieldManageBO` 尚无 `salaryLevel/fixedPerformance` getter/setter。
- RED 前端：`node tests/employee-salary-fields-ui.test.mjs` 失败于 `AddOrEdit.vue` 缺少 `salaryLevel` 初始值。
- 编辑页缺字段 RED 前端：`node tests/employee-salary-fields-ui.test.mjs` 失败于 `EmployeeBaseInfo.vue` 缺少 `ensureSalaryInformationFields`，证明编辑基本信息没有接口字段缺失兜底。
- 编辑页缺字段 RED 后端：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 失败于缺少 `normalizeEmployeeSalaryInformationFields(...)`，证明编辑保存链路没有为前端兜底字段补真实动态字段 ID。
- 自动创建字段 ID RED：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest#saveEmployeeSalaryDynamicFields_shouldCreateMissingFieldsWithIdsBeforeSavingValues test` 失败于保存动态值时 `fieldId=null`，证明旧 `saveBatch` 自动建字段后没有回填主键。
- 列表操作弹窗 RED：`node tests/employee-salary-fields-ui.test.mjs` 失败于“员工修改弹窗字段模型必须包含薪资等级文本字段”，证明 `officialModel/changePostModel` 未展示两个薪资字段。
- 员工异动保存 RED：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 在 testCompile 阶段失败于 `HrmEmployeeChangeRecord` 缺少 `salaryLevel/fixedPerformance` getter/setter，证明 `/become`、`/changePost`、`/promotion` 入参未承载两个薪资字段。
- 姓名详情抽屉 RED：`node tests/employee-salary-fields-ui.test.mjs` 失败于“点击员工姓名打开详情页时默认必须展示基本信息页签”，证明详情抽屉默认停在岗位信息页签，基本信息中的薪资字段没有出现在打开后的页面。
- GREEN 后端：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，6 个测试 0 failures/errors。
- 编辑页缺字段 GREEN 前端：`node tests/employee-salary-fields-ui.test.mjs` 通过。
- 编辑页缺字段 GREEN 后端：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，8 个测试 0 failures/errors。
- 列表操作弹窗 GREEN 前端：`node tests/employee-salary-fields-ui.test.mjs` 通过，验证新建/再入职、详情基本信息编辑、办理转正、调整部门/岗位、晋升/降级均包含并提交两个薪资字段。
- 员工异动保存 GREEN 后端：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest test` 通过，10 个测试 0 failures/errors。
- 姓名详情抽屉 GREEN 前端：`node tests/employee-salary-fields-ui.test.mjs` 通过，验证员工姓名详情抽屉默认进入基本信息页签，并在每次打开时重置到基本信息。
- 后端聚焦回归：`mvn -Dtest=HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeServiceImplCompanyAgeTest test` 通过，28 个测试 0 failures/errors。
- GREEN 前端：`node tests/employee-salary-fields-ui.test.mjs` 通过。
- 前端员工管理回归：`node tests/employee-salary-fields-ui.test.mjs && node tests/employee-form-field-removal.test.mjs && node tests/employee-edit-save-regression.test.mjs && node tests/employee-company-age-ui.test.mjs` 通过。
- 后端编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 前端构建：`npm run build` 通过，保留既有 Vue `::v-deep` deprecation 和 chunk size warnings。
- 补丁检查：后端与前端目标文件 `git diff --check -- ...` 均无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 远端 `/hrmEmployee/personalInformation/1831601326890434579` 响应没有 `薪资等级/固定绩效` | 1 | 追加前端编辑字段兜底和后端保存字段 ID 归一化，避免接口字段定义未发布/未触发补齐时编辑框缺失或无法保存。 |
| dev MySQL `153.0.237.98` 查询时报 `Too many connections` | 1 | 停止继续压数据库，改用远端 HTTP 个人信息接口验证响应形态。 |
| 自动创建薪资动态字段后保存值时 `fieldId` 为空 | 1 | 将缺失薪资字段创建从 `saveBatch` 改为逐个 `save`，让实体主键回填后再保存员工动态值。 |

# Task Plan: 员工花名册模板薪资字段

## Goal
- 员工管理“下载员工花名册模版”不再新增 `薪资级别` 列，继续使用已有 `薪资等级` 列。
- 员工管理“下载员工花名册模版”增加 `固定绩效` 列，放在 `薪酬福利` 表头下、已有 `薪资等级` 旁边。
- 员工导入需能读取 `固定绩效` 并作为两位小数动态字段入库。

## Phases
1. `complete` 复核项目文档、员工花名册模板下载接口和导入动态字段链路。
2. `complete` 补 RED 测试锁定模板新增列、列格式、导入字段类型和值格式。
3. `complete` 实现下载模板运行时补列，不直接修改二进制模板资源。
4. `complete` 实现导入时 `固定绩效` 动态字段类型、精度和值规范化。
5. `complete` 运行回归验证并更新需求、开发、发现和进度文档。

## Current Assumptions
- `固定绩效` 属于花名册导入字段，不等同于薪资档案定薪流程；本轮作为员工动态字段入库。
- `固定绩效` 只要求导入保存两位小数，不自动写入薪资档案 `41001 / 绩效工资`。
- 花名册模板下载仍以 `src/main/resources/export/employee_module.xlsx` 为基础，下载时由后端运行时补齐缺失列和格式。
- 因为使用既有 `hrm_employee_field/hrm_employee_data` 动态字段能力，不新增员工主表物理字段，所以不需要新增数据库 DDL 脚本。

## Verification
- RED：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 失败于模板缺少 `薪资级别`，以及 `固定绩效` 动态字段仍创建为文本。
- GREEN：同一命令通过，14 个测试 0 failures/errors。
- 历史字段兼容 RED：`mvn -Dtest=HrmEmployeeServiceImplImportEmployeeTest#importEmployee_shouldCorrectExistingFixedPerformanceFieldToTwoDecimalNumber test` 失败于未纠正已有文本型 `固定绩效` 字段。
- 历史字段兼容 GREEN：同一命令通过，1 个测试 0 failures/errors。
- 回归：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeBasicInfoExportSupportTest test` 通过，19 个测试 0 failures/errors。
- 编译：`mvn -DskipTests compile` 通过。
- 补丁检查：`git diff --check -- ...` 无输出。
- 再次修改 RED：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest test` 失败于模板仍包含新增 `薪资级别` 列。
- 再次修改 GREEN：同一命令通过，15 个测试 0 failures/errors。
- 最终回归：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeBasicInfoExportSupportTest test` 通过，19 个测试 0 failures/errors。
- 最终编译：`mvn -DskipTests compile` 通过。
- 最终补丁检查：`git diff --check -- ...` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 下载员工花名册模板直接原样输出资源文件，无法包含新增列 | 1 | 改为 POI 打开模板资源，运行时为花名册样式 sheet 补齐两列并输出 workbook。 |
| 导入动态字段统一创建为文本，`固定绩效` 无法保存小数字段类型 | 1 | 按表头识别 `固定绩效`，创建为 `DECIMAL` 且 `precisions=2`，写值前规范为两位小数。 |

# Task Plan: 员工管理下载部门明细方案

## Goal
- 参考 `/Users/jiangyongming/Desktop/农谷导入数据/各部门现有人员明细表(更新版).xlsx` 的表格结构，在员工管理中实现“下载部门明细”按钮。
- 导出当前登录公司内全部未删除且在职员工，字段、薪资待遇、文件名规则按用户已确认口径落地。
- 后端生成动态多 sheet Excel，前端在员工管理“员工导入/导出”下拉中触发下载。
- 2026-08-19 追加：部门 sheet 顶部统计区需与参考表结构一致，包括部门、部门总人数、男女比例、学历层次、工龄结构、专业汇总和成本区；成本区需填充月固定薪资、月绩效薪资、月社保、月公积金四项金额。
- 2026-08-19 追加：合同到期日列需按员工管理“员工合同”最新一次合同的截止日期导出。
- 2026-08-19 追加：部门 sheet 成本区删除“月伙食成本”。
- 2026-08-20 追加：员工动态字段 `固定绩效` 需参与部门明细 `薪资待遇` 显示和 `月固定薪资成本`，但不影响 `41001 / 绩效工资` 对应的 `月绩效薪资成本`。

## Phases
1. `complete` 读取项目需求/开发文档和既有员工管理、Excel 下载约束。
2. `complete` 检查参考 Excel 的 sheet、表头、样式和示例数据口径。
3. `complete` 定位后端员工管理导出/部门查询代码和前端员工管理页面入口。
4. `complete` 提出 2-3 个实现方案，给出推荐方案和取舍。
5. `complete` 与用户确认字段、范围、文件名和筛选口径；实现阶段按现有导出模式生成 Excel 与下载响应。
6. `complete` 按 TDD 实现后端导出支持类、接口、服务和员工全量查询。
7. `complete` 按 TDD 实现前端下载 API、按钮、loading/disabled 和失败提示。
8. `complete` 运行聚焦验证并更新后端/前端文档。
9. `complete` 补齐部门 sheet 顶部统计区结构，与参考表第 1-8 行对齐，并保留删除考核分后的明细表头。
10. `complete` 修正合同到期日列取数，按员工合同列表最新一条合同的 `end_time` 导出。
11. `complete` 填充部门 sheet 成本区四项金额，并删除“月伙食成本”。

## Current Assumptions
- “部门明细”指按部门分组导出当前未删除且在职员工的部门人员明细，而不是下载空白导入模板。
- 按钮放在员工管理列表页工具栏，导出应复用员工列表现有筛选与权限口径。
- 参考 Excel 主要用于确定导出格式、字段顺序、分组方式和样式；数据来源仍以系统 `hrm_employee/hrm_dept` 等当前表为准。
- 用户已确认采用方案 A：参考 Excel 样式生成动态部门明细报表，而不是固定文件下载或纯代码无模板样式导出。
- 用户已确认导出范围：当前登录公司内全部未删除且在职员工，即 `hrm_employee.is_del=0 and hrm_employee.entry_status in (1,3)`；不跟随员工管理页面当前筛选条件、状态页签或分页。
- 用户已确认 `薪资待遇` 取员工薪资档案中 `10101 / 基本工资`、`10102 / 岗位工资`、`10103 / 职务工资` 三项之和；若员工动态字段维护了 `固定绩效`，显示为 `<固定薪资>(固定)+<固定绩效>(绩效)`。
- 用户已确认考核分相关字段不需要；部门成本后续确认需要填充四项：固定薪资、绩效薪资、社保、公积金。
- 用户已确认最终部门明细表头直接删除“年平均考核分”“理论考核分”两列，不保留空列。
- 用户已确认下载文件名为 `<组织管理顶层组织名称>人员明细表yyyyMMdd.xlsx`，公司名称取组织管理中最顶层数据，日期取导出当天。
- 用户已确认导出的每个部门 sheet 上半部分也要与参考表结构相同；成本区保留四项金额，月伙食成本不再保留。
- 用户已确认合同到期日列来源为员工管理中“员工合同”的最新一次合同截止日期；实现口径按合同 `sort` 最大值优先，缺少 `sort` 时按 `start_time desc, contract_id desc` 兜底。
- 成本区当前实现口径：月固定薪资成本按当前薪资档案 `10101/10102/10103` 加员工动态字段 `固定绩效` 汇总；月绩效薪资成本按当前薪资档案 `41001` 汇总，未维护显示 `0`，不受动态字段 `固定绩效` 影响；月社保成本/月公积金成本按最近有效社保月员工记录 `corporate_insurance_amount/corporate_provident_fund_amount` 汇总。

## Verification
- 实现计划已记录到 `docs/plans/2026-08-19-employee-department-detail-export-implementation.md`。
- RED/GREEN 验证已完成：
  - 后端：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test`
  - 后端编译：`mvn -DskipTests compile`
  - 前端：`node tests/employee-department-detail-export-api.test.mjs`
  - 前端：`node tests/employee-department-detail-export-ui.test.mjs`
  - 旁路回归：`node tests/employee-basic-info-export-api.test.mjs`、`node tests/employee-basic-info-export-ui.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs`
  - 前端构建：`npm run build`
- 2026-08-19 追加顶部结构验证：
  - RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` 失败于旧部门 sheet 缺少“部门总人数”等参考表顶部结构。
  - GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` 通过，6 个测试 0 failures/errors。
  - 回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test` 通过，15 个测试 0 failures/errors。
  - 编译：`mvn -DskipTests compile` 通过。
  - 本机运行：`127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 下载为有效 OOXML，部门 sheet 顶部抽检已包含参考表合并区域和统计表头。
- 2026-08-19 追加合同到期日验证：
  - RED：`mvn -Dtest=HrmEmployeeMapperSqlTest test` 失败于旧 SQL 未按员工合同列表排序取最新一条。
  - GREEN：`mvn -Dtest=HrmEmployeeMapperSqlTest test` 通过，8 个测试 0 failures/errors。
  - 回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test` 通过，16 个测试 0 failures/errors。
  - 编译：`mvn -DskipTests compile` 通过。
  - 本机运行：`127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 返回 `200 OK`，下载文件为有效 Microsoft OOXML，`unzip -t` 无错误；首个部门 sheet 明细表头包含“合同到期日”，抽样员工导出日期与 `hrm_employee_contract.end_time` 按新口径查询结果一致。
  - 2026-08-20 复核：本机 8081 下载文件与 dev 库 `hr_0003` 全量对账一致，期望 92 人、实际 92 人，有合同到期日均为 15 人，差异 0；黎冬霜合同到期日导出为 `2027-04-30`。
- 2026-08-19 追加部门成本验证：
  - RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest test` 先失败于缺少 `performanceSalaryCost(...)`，证明旧实现没有绩效成本计算入口。
  - GREEN：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，9 个测试 0 failures/errors。
  - 回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，19 个测试 0 failures/errors。
  - 编译：`mvn -DskipTests compile` 通过。
  - 本机运行：`127.0.0.1:8081/api/hrsystem/hrmEmployee/exportDepartmentDetail` 带 token 返回 `200 OK`，下载文件为有效 Microsoft OOXML，`unzip -t` 无错误；抽检部门 sheet J2-J5 已写出固定薪资、绩效薪资、社保、公积金金额，整本 workbook 无“月伙食成本”残留。
- 2026-08-20 追加固定绩效验证：
  - RED：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest#buildWorkbook_shouldIncludeFixedPerformanceInSalaryTreatmentAndFixedSalaryCost test` 先失败于薪资待遇仍为 `4150`，证明旧实现没有读取动态字段 `固定绩效`。
  - GREEN：同一命令通过，1 个测试 0 failures/errors；无固定绩效既有场景 `buildWorkbook_shouldGroupEmployeesByDepartmentAndWriteSalaryTreatment` 同步通过。
  - 边界：`buildWorkbook_shouldShowFixedPerformanceSectionWhenFixedPerformanceIsExplicitZero` 覆盖 `固定绩效=0.00` 时仍展示绩效段，固定成本不额外增加。
  - 回归：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest,HrmEmployeeServiceImplDepartmentDetailCostTest test` 通过，22 个测试 0 failures/errors。
  - 编译：`mvn -DskipTests compile` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 部门 sheet 名在 Excel 安全命名后可能重复，导致 `The workbook already contains a sheet named ...` | 1 | 增加 workbook 级去重命名，按 `(2)(3)...` 递增后缀生成唯一 sheet 名 |
| 点击“下载部门明细”返回 404 | 1 | 确认为 Vite dev server 未配置 `.env.development`，代理回退到远端旧后端；新增 `hr_web/.env.development` 指向 `http://127.0.0.1:9080` 并重启前端服务 |
| 访问 `127.0.0.1:8081` 点击“下载部门明细”提示失败 | 2 | 确认为本机 `html/assets/index-*.js` 仍写入 `http://153.0.237.99:8081`，浏览器跨到远端旧后端返回 404；执行并修复 `npm run deploy:access -- --local-only`，重建本机包并重启 8081 |
| 本地登录 curl 首次使用 GET 且 URL 未加引号，zsh 将 `?` 当作通配符解析 | 1 | 改为 `curl -X POST` 并对 URL 加引号 |
| `logs/hainan-dev.log` 不存在，无法从该路径读取下载 SQL 日志 | 1 | 改用接口下载、OOXML 检查和数据库只读抽样验证 |
| openpyxl 一行命令中错误使用换行转义导致 `SyntaxError` | 1 | 改为单行列表推导完成 Excel 抽检 |
| `session-catchup.py` 误用 `sh` 执行导致脚本语法错误 | 1 | 改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py ...` |
| 成本抽检 openpyxl 命令再次混入换行转义导致 `SyntaxError` | 1 | 改为纯一行列表推导抽检 J2-J6 |

# Task Plan: 排班上传休假未录入修复

## Goal
- 复现排班管理上传模板中“调休”能录入、“休假”未录入的问题。
- 找到 Excel 上传解析到 `/workPlan/saveAll` 和本地 `tbplanlist` 落库之间的根因。
- 修复时保持既有“调休”和自定义班次导入行为不变，并补充回归测试覆盖“休假”模板值。

## Phases
1. `complete` 读取项目文档和历史排班上传约束，定位上传解析链路。
2. `complete` 用现有测试结构补 RED 测试，证明模板“休假”当前不会生成休息类排班。
3. `complete` 做最小修复，让“休假”按休息类排班正常落库，并保留 `rest_shift_type` 区分。
4. `complete` 运行排班定向测试和必要编译验证。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- 用户反馈中的“休假”指排班导入模板每日分组里的“休假/调休”列填写为 `休假`。
- “调休录入成功”说明员工匹配、日期分组和休息类排班基础链路可用，故优先排查休息类文本归一化或模板列解析。
- `tbplanlist.rest_shift_type` 应继续用于区分 `adjust=调休` 和 `rest=休息/休假`。

## Verification
- RED：`mvn -Dtest=WorkPlanServiceImplTest#previewImportExcel_shouldTreatHorizontalVacationAsRestShift+saveEmployeeDayShift_shouldTreatVacationTextAsRestShift test` 失败；横版模板只生成 1 条记录，服务层 `休假` 报“开始时间不能为空，格式必须为HH:mm”。
- GREEN：同一命令通过，2 个测试 0 failures/errors。
- 排班回归：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过，62 个测试 0 failures/errors。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 补丁检查：`git diff --check -- ...` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 横版模板 `休假` 行未生成，服务层 `休假` 被当成需时间的排班 | 1 | 已在横版模板布尔识别、班次类型归一化和休息子类型归一化中支持 `休假`。 |

# Task Plan: prod 一键打包脚本

## Goal
- 在 Maven package 产物目录 `target/` 中提供可双击的一键 prod 打包入口。
- 生成的 `target/hrsystem-0.0.1-SNAPSHOT.jar` 内部默认 profile 必须为 `prod`，并包含 `application-prod.properties`。
- 源码中的 `src/main/resources/application.properties` 仍保持默认 `dev`，避免破坏本地调试和 `ApplicationProfileConfigTest`。

## Phases
1. `complete` 复核项目 profile、Maven package 和生产配置文档约束。
2. `complete` 设计不污染源码默认 dev 的 prod 打包流程。
3. `complete` 创建 `target/package-prod.sh` 并设置执行权限。
4. `complete` 增加可双击的一键入口 `target/package-prod.command`。
5. `complete` 运行一键入口生成 prod jar，并验证 jar 内配置。
6. `complete` 更新需求、开发文档、findings 和 progress。

## Current Assumptions
- package 产物位置为 Maven 默认输出目录 `target/`，脚本也放在该目录。
- 由于现有回归测试要求源码默认 profile 为 `dev`，prod 打包脚本不能直接把源码 `application.properties` 改成 `prod` 后运行完整测试。
- prod 包使用临时项目副本生成，脚本执行完成后只把 jar 复制回 `target/`。
- macOS 双击入口使用 `.command` 文件，底层仍调用同目录 `package-prod.sh`。

## Verification
- `mvn -Dtest=ApplicationProfileConfigTest test` 通过，源码默认 profile 保持 `dev`。
- `./target/package-prod.sh` 通过，复制生成的 `target/hrsystem-0.0.1-SNAPSHOT.jar`。
- 解包验证通过：jar 内 `BOOT-INF/classes/application.properties` 为 `spring.profiles.active=prod`，且包含 `BOOT-INF/classes/application-prod.properties`。
- `bash -n target/package-prod.sh` 通过。
- `zsh -n target/package-prod.command` 通过。
- `./target/package-prod.command` 通过，生成 `target/hrsystem-0.0.1-SNAPSHOT.jar`。
- 再次解包验证通过：jar 内 `BOOT-INF/classes/application.properties` 为 `spring.profiles.active=prod`，且包含 `BOOT-INF/classes/application-prod.properties`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: Maven package 失败排查

## Goal
- 复现当前 `mvn package` 失败，提取首个真实错误。
- 按系统化调试流程确认根因，区分 Maven/POM 警告、测试失败、编译错误和环境问题。
- 如是代码或测试问题，做最小修复并验证 `mvn package` 通过；如是环境或外部依赖问题，给出可执行处理方式。

## Phases
1. `complete` 读取项目需求/开发文档和既有计划记录，确认 Maven 编译作为真实错误判断依据。
2. `complete` 运行 `mvn package` 复现错误并保存关键信息。
3. `complete` 根据首个 error 追踪到具体源码、测试或配置入口。
4. `complete` 如需修改，先补或定位失败测试，再做最小修复。
5. `complete` 重新运行聚焦测试和 `mvn package` 验证。
6. `complete` 更新需求/开发文档、findings 和 progress。

## Current Assumptions
- 用户未提供错误日志，因此先以本机当前工作区直接执行 `mvn package` 复现为准。
- 当前工作区已有大量历史改动和未跟踪文件；本轮不会回退无关改动。
- 既有 POM/dependency warnings 可能存在，只有导致 Maven 非零退出的首个 error 作为本轮根因入口。

## Verification
- 失败复现：`mvn package` 失败于 `ApplicationProfileConfigTest#activeProfileLoadsDatasourceUrlForDebugStartup`，`expected:<dev> but was:<prod>`。
- 聚焦验证：`mvn -Dtest=ApplicationProfileConfigTest test` 通过，1 个测试，0 failures/errors。
- 完整验证：`mvn package` 通过，505 个测试，0 failures/errors，生成 `target/hrsystem-0.0.1-SNAPSHOT.jar`。
- 保留既有 Maven warnings：`jsch` 重复依赖、项目内 `systemPath`、`easyexcel` 重复版本声明。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `ApplicationProfileConfigTest` 期望默认 profile 为 `dev`，实际为 `prod` | 1 | 用户已将 `application.properties` 改回 `spring.profiles.active=dev`；聚焦测试和完整 `mvn package` 均通过。 |

# Task Plan: 薪资导出 Excel 批注说明

## Goal
- 薪资管理导出 Excel 时，为关键薪资字段写入单元格批注，帮助财务直接查看为空原因或计算依据。
- 覆盖字段：`40102 / 全勤奖`、`200101 / 超缺勤`、`230101 / 个人所得税`、`160102 / 工会费`。
- 批注只解释现有导出结果，不改变薪资核算、工资项金额或导出列顺序。

## Phases
1. `complete` 读取后端/前端项目文档，确认薪资导出范围选择和字段业务规则。
2. `complete` 定位 `SalaryMonthRecordServiceNew#exportSalaryNew(...)`、Excel 模板写出和已有测试。
3. `complete` 补 RED 测试，验证导出工作簿目标单元格包含批注。
4. `complete` 实现批注内容生成与 POI 单元格批注写入。
5. `complete` 更新后端需求/开发文档、findings 和 progress。
6. `complete` 运行聚焦 Maven 测试和编译验证。

## Current Assumptions
- 本次落点在后端 `hainan`，前端 `hr_web` 已经只负责触发 `/hrmSalaryMonthRecord/exportSalary` 下载，暂不需要前端改动。
- 批注内容使用导出行已有数据和薪资项值推导；若精确源明细不足，批注应明确说明“按薪资项/导出字段取值”，避免伪造不存在的计算来源。
- 空值解释以工资项缺失或导出值为空/0 为触发条件；非空时仍给出计算过程或业务依据。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` testCompile 失败于缺少 `SalaryExportCommentWriteHandler`。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 68 个测试。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。
- 补丁检查：目标文件 `git diff --check` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 残疾员工工会费计算规则调整

## Goal
- 薪资核算中残疾员工也需要生成 `160102 / 工会费`。
- 保留残疾员工原有免个税口径，不把“有工会费”误扩展为“需要计个税”。
- 半路转正、实习/离职、成都/攀枝花公司、应发工资小于等于 0 等既有工会费豁免规则保持不变。

## Phases
1. `complete` 复核项目文档、既有薪资规则和工会费历史记录。
2. `complete` 定位工会费计算入口并补 RED 测试，证明残疾员工当前不会生成工会费。
3. `complete` 最小修改工会费计算条件，保留个税残疾免税分支。
4. `complete` 运行定向 Maven 测试/编译验证。
5. `complete` 更新需求、开发文档、findings 和 progress。

## Current Assumptions
- `hrm_employee.is_disabled=1` 表示残疾员工，`2` 表示非残疾员工。
- `160102 / 工会费` 按应发工资 `0.5%` 计算，具体是否收取仍由 `calculateUnionFee(...)` 内部公司、员工状态、转正日期和应发工资规则决定。
- 新规则只改变残疾员工是否参与工会费计算，不改变残疾员工个税 `230101` 免税行为。

## Verification
- RED：`mvn -Dtest=SalaryComputeServiceNewTest test` 先失败于 `残疾员工也应按应发工资0.5%生成工会费`，旧逻辑返回 `160102=0`。
- GREEN：
  - `mvn -Dtest=SalaryComputeServiceNewTest test` 通过 14 个测试。
  - `mvn -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest test` 通过 79 个测试。
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 后端调试模式 jdbcUrl 缺失排查

## Goal
- 后端在 IDE/调试模式启动时不再报 `jdbcUrl is required with driverClassName`。
- 找到调试模式和正常启动之间的数据源配置加载差异，修复配置源或数据源构造逻辑中的根因。
- 保持现有动态多租户数据源和连接池配置行为不变，只做最小必要调整。

## Phases
1. `complete` 复核项目文档约束，定位启动配置、profile 和数据源创建代码。
2. `complete` 复现或用定向测试固定 `driverClassName` 有值但 `jdbcUrl` 缺失的失败场景。
3. `complete` 对比项目中正常数据源配置键与 Hikari/Spring Boot 期望字段，确认根因。
4. `complete` 实施最小修复，并补充回归测试或启动配置验证。
5. `complete` 更新需求/开发文档、findings 和 progress，运行验证命令。

## Current Assumptions
- 报错来自 HikariDataSource 初始化阶段：存在 `driverClassName`，但对应数据源未拿到 `jdbcUrl`。
- 调试模式可能加载了不同 profile、不同 working directory 或不同环境变量，导致配置文件中的 `spring.datasource.url` / `jdbc-url` 未绑定到当前数据源。
- 项目已有动态数据源链路，修复优先兼容现有 `DynamicDataSource`、`CompanyDataSourceProvider`、`MyBatisConfig` 与 `DataSourcePoolConfigurator`。

## Verification
- RED：`mvn -Dtest=ApplicationProfileConfigTest test` 先失败于 `expected:<dev> but was:<null>`，证明 `spring.profiles.active` 未被读取。
- GREEN：`mvn -Dtest=ApplicationProfileConfigTest test` 通过；`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 顶栏钉钉 API 调用量展示与提醒

## Goal
- 系统右上角展示本月钉钉 API 调用量，显示总调用量、额度占比和状态。
- 调用量超过 80% 时，调用量描述区域使用红色风险样式。
- 每天上午一次、每天下午一次弹窗提醒超 80% 风险，弹窗提供“本月不再提示”供用户选择。
- 鼠标移入调用量区域时悬浮展示各功能调用量的分布百分比和柱状图。

## Phases
1. `complete` 复核后端/前端文档约束，定位钉钉调用日志来源、顶栏组件和现有测试。
2. `complete` 补 RED 测试覆盖后端调用量汇总、前端 API 封装、顶栏展示、超阈值样式、提醒节流和悬浮分布。
3. `complete` 实现后端统计接口与调用功能分类。
4. `complete` 实现前端顶栏组件、API helper、悬浮分布图和提醒弹窗。
5. `complete` 更新后端/前端需求与开发文档，运行定向测试和构建验证。

## Current Assumptions
- “调用量超过 80%”按本月已使用次数 / 本月额度计算；若系统已有配置额度则复用，若没有则后端提供保守默认值并在文档中标明。
- 提醒只在超过 80% 后触发；上午/下午各一次按浏览器本地时间区分，用户勾选“本月不再提示”后当月不再弹。
- 功能分布按系统能从现有钉钉调用日志识别的模块分类，不直接在线调用钉钉查询额度，避免为展示功能额外消耗调用量。

## Verification
- RED：
  - 后端 `mvn -Dtest=HrmDingTalkApiUsageServiceImplTest,HrmDingTalkApiUsageControllerTest test` 先失败于缺少 `HrmDingTalkApiUsageServiceImpl`。
  - 前端 `node tests/dingtalk-api-usage-api.test.mjs` 先失败于缺少 `src/api/hrm/dingtalkApiUsage.js`。
- GREEN：
  - `mvn -Dtest=HrmDingTalkApiUsageServiceImplTest,HrmDingTalkApiUsageControllerTest test` 通过 3 个测试。
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
  - `node tests/dingtalk-api-usage-api.test.mjs`、`node tests/dingtalk-api-usage-utils.test.mjs`、`node tests/top-header-dingtalk-api-usage.test.mjs` 通过。
  - `npm run build` 通过，保留既有 `::v-deep` 与 chunk size warnings。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 排班管理考勤信息改读同步数据

## Goal
- 排班管理中的考勤信息必须读取“同步考勤”后已经落库的数据。
- 排班管理展示/查询链路禁止实时调用钉钉获取考勤数据接口，避免消耗钉钉 API 调用量。
- 只有明确的同步考勤动作可以调用钉钉接口；排班管理页面查询应依赖本地快照。

## Phases
1. `complete` 复核项目文档约束并记录本轮任务。
2. `complete` 定位排班管理考勤信息的数据流和钉钉调用点。
3. `complete` 补 RED 回归测试，证明旧逻辑会触发钉钉考勤接口。
4. `complete` 改为读取同步后的本地考勤数据，并禁止展示查询链路调用钉钉。
5. `complete` 运行定向验证，更新需求/开发文档、findings 和 progress。

## Current Assumptions
- “排班管理中的考勤信息”优先按排班管理页面查询、下拉、人员/班次/排班展示等非同步动作理解。
- 同步后的本地考勤数据包括已落库的考勤组、班次、员工映射、排班计划、打卡明细和钉钉报表快照。
- 本轮不改变 `/attendanceData/sync`、`/attendanceData/syncAll` 这类明确同步入口的钉钉调用行为。

## Verification
- RED：`mvn -Dtest=HrmAttendanceDataControllerTest test` 先失败于 `getPlanDataByGroup_shouldReadSyncedLocalAttendancePlanWithoutDingTalk` 缺少本地日期范围查询并仍依赖钉钉路径，固定旧问题。
- GREEN：
  - `mvn -Dtest=HrmAttendanceDataControllerTest,WorkPlanServiceImplTest test` 通过 55 个测试。
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
  - `rg -n "schedule/listbyusers|attendance/group/memberusers/list|attendance/shift/list" src/main/java/com/tianye/hrsystem/controller/HrmAttendanceDataController.java` 无命中。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 新增 `getShiftList` 回归测试未声明 `IAccessToken` checked exception，测试编译失败 | 1 | 给测试方法补 `throws Exception` 后重跑同一 Maven 命令通过。 |

# Task Plan: 排班管理矩阵翻页修复

## Goal
- 排班管理员工月度矩阵点击分页页码或切换每页条数时，表格展示的员工行必须随分页变化。
- 后端仍按当前月份一次加载排班记录与员工列表，前端按员工矩阵行做本地分页，避免只加载部分排班记录导致月度矩阵不完整。
- 筛选、重置、切换月份后分页回到第一页，分页总数统计员工矩阵行数。

## Phases
1. `complete` 复核文档和当前 `Scheduling.vue`、公共 `Table` 分页事件契约。
2. `complete` 补 RED 测试覆盖排班矩阵本地分页切片与页面绑定分页事件。
3. `complete` 实现排班矩阵本地分页与筛选重置回第一页。
4. `complete` 更新前后端需求/开发文档、findings 和 progress。
5. `complete` 运行前端定向测试与构建验证。

## Current Assumptions
- “翻页没有效果”发生在 `/hrm/attendance/scheduling` 排班管理员工月度矩阵主表。
- 公共 `Table` 组件只维护分页控件状态并通过 `get-data` 事件通知父组件，不会自动切分父组件传入的 `data`。
- 排班矩阵必须先基于当月全量排班记录和全员列表透视成员工行，再对员工行分页。

## Verification
- RED：`node tests/work-plan-scheduling-records.test.mjs` 先失败于缺少 `buildWorkPlanMatrixPageRows` 导出，确认旧排班页没有本地分页工具和表格分页事件绑定。
- GREEN：
  - `node tests/work-plan-utils.test.mjs` 通过。
  - `node tests/work-plan-scheduling-records.test.mjs` 通过。
  - `node tests/work-plan-api.test.mjs` 通过。
  - `node tests/work-plan-records-page.test.mjs` 通过。
  - `node tests/work-plan-time-picker.test.mjs` 通过。
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 加班夜班统计三项出勤可编辑保存

## Goal
- 加班/夜班统计的“单人查看”和“显示所有”页面中，应出勤时间、实际出勤时间、应计出勤三列均可编辑。
- 修改后的三项出勤值必须能调用后端接口保存，并持久化到统计明细表，刷新后仍按保存值展示。
- 交互保持现有后台矩阵表风格：表格内紧凑编辑、保存中有 loading、失败时保留原值并展示中文错误。

## Phases
1. `complete` 复核加班/夜班统计前后端页面、接口、落库字段和现有测试。
2. `complete` 补 RED 测试，覆盖单人查看和显示所有三列编辑保存，以及后端字段持久化。
3. `complete` 实现后端保存接口/服务/Mapper 最小改动。
4. `complete` 实现前端 API helper、单元格编辑组件/状态和两个视图接入。
5. `complete` 更新后端/前端需求与开发文档，运行定向验证和构建。

## Current Assumptions
- “应出勤时间”按现有页面展示的小时值理解，保存时后端折算并更新 `expected_attendance_days`，因为明细表当前以天为主要落库字段。
- “实际出勤时间”和“应计出勤”按小时保存，分别对应统计明细的实际出勤小时/天数派生字段和 `accrued_attendance_hours`。
- 本轮只让月度三项汇总列可编辑；每日 1 日到月末矩阵值、加班小时、夜班次数和备注不在本轮修改范围。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 失败于缺少 `UpdateOvertimeNightAttendanceBO`；前端 `node tests/overtime-night-api.test.mjs`、`node tests/overtime-night-utils.test.mjs`、`node tests/attendance-display-columns.test.mjs` 均按预期失败。
- GREEN：后端 `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 77 个测试，`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings；前端 `node tests/overtime-night-api.test.mjs`、`node tests/overtime-night-utils.test.mjs`、`node tests/attendance-display-columns.test.mjs`、`node tests/overtime-night-page.test.mjs` 与 `npm run build` 通过，保留既有 `::v-deep` 和 chunk size warnings。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 前端测试补丁路径误写为后端项目下 `tests/...` | 1 | 改用 `../hr_web/tests/...` 分开补丁。 |

# Task Plan: 薪资月份误建自助恢复功能

## Goal
- 在薪资管理中新增长期可用的“薪资月份恢复”能力，处理误点“新建次月薪资”导致当前月份推进到错误月份的问题。
- 用户先选择目标源月份并预览误建后续月份；系统只允许恢复“后续月份没有工资明细、没有工资条发送记录”的安全场景。
- 复用现有薪资月记录和月薪资明细，不新增复杂流程表；恢复动作需后端校验，前端只提交目标年月。

## Phases
1. `in_progress` 复核薪资月记录、工资明细、工资条记录和前端薪资管理入口。
2. `pending` 先补后端/前端 RED 测试，覆盖预览、阻止有数据恢复、恢复安全误建月份。
3. `pending` 实现后端预览/恢复 API 和服务逻辑。
4. `pending` 实现前端恢复弹窗、API helper、按钮 loading/disabled 和中文提示。
5. `pending` 更新后端/前端需求与开发文档，运行定向验证。

## Current Assumptions
- “长期处理”按管理员/薪资负责人自助处理理解；本轮不做复杂角色系统改造，按钮仍挂在薪资管理页，由后端安全校验兜底。
- 后续月份只要存在员工薪资明细、工资条发送记录或月记录 `is_send=1`，就不允许自动恢复，避免破坏真实核算数据。
- 可恢复场景采用删除误建后续空薪资月记录的方式，而不是只改状态；因为现有最新记录查询按 `create_time desc` 返回最新月。

## Verification
- RED：待运行。
- GREEN：待运行。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 薪资新建次月防重复与7月恢复方案

## Goal
- 用户核算 2026-07 薪资时，多次点击“新建次月薪资”不应把当前核算月份推进到 2026-09。
- 前端“新建次月薪资”必须以用户选择的源薪资月份为准，不能在月份为空时回退到系统当前月份。
- 后端创建次月薪资必须基于请求源年月/源记录，并在次月记录已存在时跳过创建，避免重复点击继续推进月份。
- 提供现有错误数据恢复到 2026-07 薪资的处理方案。

## Phases
1. `complete` 读取后端/前端需求与开发文档，确认薪资管理和月记录约束。
2. `complete` 定位“新建次月薪资”前端入口、后端 `updateCheckStatus/addNextMonthSalary/salaryAudit` 创建链路和根因。
3. `complete` 补 RED 测试，覆盖前端防重复提交、默认源月份、后端基于源月份创建且次月已存在时不重复创建。
4. `complete` 实现前后端最小修复，并补历史错误数据恢复方案。
5. `complete` 运行定向 Maven/Node 验证，更新后端和前端需求/开发文档。

## Current Assumptions
- “改回7月份的薪资”优先按 2026-07 为业务希望继续核算的源薪资月份处理；已经误建的 2026-08/2026-09 记录需要单独数据修复确认后清理。
- `queryLastSalaryMonthRecord()` 被多个页面和任务复用，保持其按最新记录返回/初始化的既有语义，不在本轮全局改动。
- `updateCheckStatus(year, month)` 的 `year/month` 是“从哪个薪资月份创建次月”的业务源月份。

## Verification
- RED：前端 `node tests/salary-create-next-month.test.mjs` 在缺少 loading/禁用和系统月回退保护时失败；后端测试在 helper 未实现时无法编译。
- GREEN：`mvn -Dtest=SalaryMonthRecordNextMonthTest test` 通过 2 个测试；`node tests/salary-create-next-month.test.mjs` 通过。
- 文档：已更新后端/前端 `docs/requirements.md` 与 `docs/development.md`，新增 `docs/plans/2026-08-10-salary-next-month-recovery-solution.md`，并同步更新本轮 `findings.md`、`progress.md`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `init-session.sh` 无执行权限 | 1 | 改用 `sh ~/.codex/skills/planning-with-files/scripts/init-session.sh` 初始化/复用计划文件。 |
| 回归测试 NPE | 1 | 测试双向的 mock 字段遮蔽父类依赖；改用独立 mock 字段名后通过。 |

# Task Plan: 审批数据手工添加显示修复

## Goal
- 审批数据列表中的审批日期展示不带秒。
- 行级“添加数据”按闫倩 `2025-06-01 18:00-19:00` 添加后，接口成功返回且数据必须能在列表中查到。
- 对操作人员按上述场景添加的数据，列表“时长”列显示为 `2小时`。

## Phases
1. `complete` 复核手工添加请求、列表查询过滤和日期/时长展示链路，确认新增成功但列表不显示的根因。
2. `complete` 补 RED 测试覆盖审批日期无秒、闫倩手工添加后可查询、列表时长显示 2 小时。
3. `complete` 实现后端和必要前端的最小修复。
4. `complete` 运行定向后端/前端验证。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- “审批日期不需要秒”优先按前端列表展示格式理解，不改变数据库字段精度。
- 闫倩这条数据新增成功但列表不显示，优先排查手工新增保存的 `workDate/year/month/userId/employeeId` 与列表查询条件是否不一致。
- `18:00-19:00` 本身自然时长为 1 小时；列表要求显示 2 小时说明应保留并展示操作人员手工输入/描述的合计时长，而不是强制按起止时间重算。

## Verification
- RED：
  - `node tests/approval-duration-utils.test.mjs` 失败于 `formatApprovalDateTimeMinute is not a function`。
  - `node tests/attendance-approval-fetch-utils.test.mjs` 失败于缺少 `buildManualAddApprovalListRefreshState` 导出。
  - `node tests/attendance-approval-dialog.test.mjs` 失败于添加审批日期控件未隐藏秒。
- GREEN：
  - `node tests/approval-duration-utils.test.mjs` 通过。
  - `node tests/attendance-approval-fetch-utils.test.mjs` 通过。
  - `node tests/attendance-approval-dialog.test.mjs` 通过。
  - `node tests/attendance-approval-api.test.mjs` 通过。
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过 35 个测试。
  - `npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 审批数据行内添加员工审批

## Goal
- 支持前端审批数据列表按行添加当前员工的本地审批快照。
- 后端审批数据列表必须返回可唯一锁定员工的 `employeeId` 和用于同名识别的 `mobile`。
- 手工添加接口支持前端提交手工合计时长，并按小时落库或拆分到多日期审批快照。

## Phases
1. `complete` 复核审批数据列表查询、手工添加接口和现有测试契约。
2. `complete` 补 RED 测试覆盖列表返回 `employeeId/mobile` 与手工合计时长入参。
3. `complete` 实现 VO、Mapper、BO 与服务层手工时长处理。
4. `complete` 运行后端定向测试和前端配套验证。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- 同名员工识别由界面展示 `姓名 + 手机号` 完成，后端实际主键仍为 `employeeId`。
- `durationUnit` 为空时按小时理解；前端当前只提交 `小时`。
- 多个审批日期范围共用一个手工合计时长时，后端按各时间段原始时长比例拆分，最后一段承接舍入余量。

## Verification
- RED：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest test` 先失败于审批列表 VO 缺少 `employeeId`、手工添加 BO 缺少 `duration`。
- GREEN：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest test` 通过，23 个测试。
- 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，35 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 同步考勤与审批获取进度条增强

## Goal
- 员工管理同步考勤执行中出现错误时，进度条内持续展示操作人员能看懂的错误原因，禁止展示 Java/SQL/HTTP/异常类名等技术描述。
- 同步失败后进度条不得自动消失；操作人员修正数据后，重新执行需支持断点接续同步考勤。
- 同步成功后进度条也不得自动关闭，必须由用户点击确认后关闭。
- “获取审批数据”新增进度条，交互逻辑复制员工管理同步考勤进度条：成功确认后关闭、失败保留进度和中文错误原因。

## Phases
1. `complete` 定位员工管理同步考勤、进度查询、审批数据获取的前后端入口。
2. `complete` 补 RED 测试锁定失败错误文案、弹窗保留、成功确认关闭和断点续同步。
3. `complete` 实现后端进度状态、业务错误翻译和断点续同步所需字段/逻辑。
4. `complete` 实现前端同步考勤与审批数据获取进度条交互。
5. `complete` 运行定向验证并更新后端/前端需求与开发文档。

## Current Assumptions
- “员工管理的同步考勤”优先按员工管理页面触发的钉钉员工/考勤同步入口理解；如代码中存在多个“同步考勤”，以员工管理页面按钮实际调用为准。
- 断点续同步以“上次任务已成功处理的员工/日期/页”为边界，重试时跳过已完成部分，只处理未完成或失败部分；不改变已成功落库数据。
- 技术错误需统一翻译为业务语言，例如“员工缺少钉钉用户”“钉钉接口繁忙，请稍后重试”“请选择同步月份”，不能把异常类名、SQL、堆栈、接口路径直接展示给操作人员。

## Verification
- 已有一次后端聚焦验证：`mvn -Dtest=HrmAttendanceDataServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过。
- 已有一次后端组合回归：`mvn -Dtest=HrmAttendanceDataServiceImplTest,HrmAttendanceDataControllerTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest test` 通过，79 个测试。
- 已有一次前端聚焦验证：`node tests/attendance-sync-progress-utils.test.mjs`、`node tests/employee-attendance-sync-progress-dialog.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs`、`node tests/attendance-approval-api.test.mjs`、`node tests/attendance-approval-dialog.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs` 均通过。
- 2026-08-03 续接后补充修正：失败完成态不再强制进度显示为 `100%`，重新运行前后端 fresh 验证通过。
- Fresh 后端：`mvn -Dtest=HrmAttendanceDataServiceImplTest,HrmAttendanceDataControllerTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest test` 通过，79 个测试，0 失败。
- Fresh 前端：`node tests/attendance-sync-progress-utils.test.mjs`、`node tests/employee-attendance-sync-progress-dialog.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs`、`node tests/attendance-approval-api.test.mjs`、`node tests/attendance-approval-dialog.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs` 均通过。
- Fresh 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端目标文件 `git diff --check -- ...` 无输出；前后端目标文件尾随空白扫描无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 2026-07 审批数据获取失败排查

## Goal
- 定位并修复“获取 2026-07 月的审批数据失败”的根因。
- 先确认失败发生在前端接口、后端服务、钉钉接口调用还是本地审批快照查询/同步链路。
- 修复需保持现有考勤、加班/夜班统计、行政体系考勤导出对审批数据的口径一致。

## Phases
1. `complete` 定位报错入口、调用接口和完整错误信息。
2. `complete` 复现或用日志/测试固定 2026-07 失败场景。
3. `complete` 追踪审批数据月份参数、员工映射、钉钉接口/本地快照数据流并确认根因。
4. `complete` 补最小 RED 测试并实施单点修复。
5. `complete` 运行定向验证，更新需求/开发文档、findings 和 progress。

## Current Assumptions
- “审批数据”大概率指考勤同步或加班/夜班统计中读取钉钉审批/本地审批快照的链路。
- 2026-07 是完整过去月份，查询边界应为 `2026-07-01 00:00:00` 至 `2026-07-31 23:59:59` 或等价半开区间，不应误落到当前月默认值。
- 取消至统计的审批仍需排除；本轮不改变行政/生产体系和休息制度口径。

## Verification
- 已完成 RED/GREEN 聚焦验证，覆盖钉钉限流文案、多人预解析单人失败跳过、员工表缺钉钉 ID 时校验复用本地映射。
- Fresh 回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，63 个测试，0 失败。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 空白检查：本轮目标文件尾随空白检查无输出；已跟踪文档 `git diff --check -- docs/requirements.md docs/development.md findings.md progress.md task_plan.md` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `curl` 登录 URL 未加引号被 zsh 通配符拦截 | 1 | 改用加引号的 `POST /hrsystem/login` 表单请求确认本地登录和 token。 |

# Task Plan: 李凤皇行政导出三项出勤修复

## Goal
- 修复“下载行政体系考勤”中李凤皇 2026-06 应出勤、实出勤、应计出勤导出为 `184 / 180 / 184` 的问题。
- 正确结果需与加班/夜班统计页面一致：`200 / 196 / 200`。
- 根因需围绕行政导出是否仍直接读取旧 `hrm_overtime_night_statistics_detail` 明细值，而不是统计查询层按 `restType=2` 刷新后的当前口径。

## Phases
1. `complete` 复核行政导出链路、统计查询链路和既有李凤皇统计修复。
2. `complete` 补 RED 测试，锁定行政导出 E/F/G 三列必须匹配统计页面当前口径。
3. `complete` 实现最小修复，导出三项出勤优先使用统计查询刷新后的月度值。
4. `complete` 运行定向回归和编译验证。
5. `complete` 更新需求/开发文档、findings 和 progress，并反馈根因与验证结果。

## Current Assumptions
- 行政导出仍以已落库统计明细作为员工行基准，但 E/F/G 三项小时值应优先采用加班/夜班统计查询结果。
- 若统计查询未返回目标员工，则继续保留明细聚合值作为兼容兜底。
- H-S 审批、出差固定 0、异常报表列不在本轮修改范围内。

## Verification
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于 `expected:<200.0> but was:<184.0>`，确认旧导出仍读取旧明细应出勤。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过 15 个测试。
- 回归：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 19 个测试。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 统计服务链路：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldUseFixedMonthlyRestAttendanceWhenRestTypeIsFixedEvenIfAffiliationIsAdministrative test` 通过。
- 空白检查：本轮目标文件 `git diff --check -- ...` 无输出；全仓库 `git diff --check` 被其它未关联改动中的尾随空白拦截，本轮未修改这些文件。
- Final fresh：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 19 个测试；`mvn -DskipTests compile` 通过；本轮目标文件空白检查无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 行政体系考勤导出出差列修复

## Goal
- 修复“下载行政体系考勤”中 L 列出差与人工统计不一致的问题。
- 用户确认人工统计中所有员工出差为 `0`，行政体系导出不得再把钉钉 `出差/外出/bizType=2` 审批折算进出差列。
- 核查 `hr_0003 / 2026-06` 当前导出中哪些员工因审批映射产生了非零出差。

## Phases
1. `complete` 复核代码映射、人工模板和开发库审批数据，确认根因。
2. `complete` 补 RED 测试，锁定出差/外出审批不得写入行政导出 L 列。
3. `complete` 实现最小修复，行政导出出差列固定为 0。
4. `complete` 运行后端定向回归和编译验证。
5. `complete` 更新需求/开发文档、findings 和 progress，并反馈核查结果。

## Current Assumptions
- 本轮只修改行政体系考勤导出 L 列“出差天数”口径，不影响请假、年假、异常、加班和 F 列实出勤重算。
- 钉钉 `外出` 不属于行政体系考勤汇总模板的“出差”列。
- 即使本地审批快照存在 `tagName=出差` 或 `bizType=2`，本月行政体系人工统计口径仍要求导出为 0。

## Verification
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于 `expected:<0.0> but was:<1.5>`。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过 14 个测试。
- 回归：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 18 个测试。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 数据核查：`hr_0003 / 2026-06` 当前旧映射会导致 6 名行政体系员工出差列非 0；修复后导出 L 列不再消费这些审批。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 行政体系考勤导出实出勤小时修复

## Goal
- 修复“下载行政体系考勤”中 F 列实出勤小时与加班/夜班统计页面不一致的问题。
- 以吴晓霞 `2026-06` 为回归样例：加班/夜班统计应为 `185.5` 小时，导出不得再按 `actual_attendance_days * 8` 只输出 `184`。
- 批量核查 `hr_0003 / 2026-06` 行政体系员工是否存在同类差异，并把影响名单反馈给用户。

## Phases
1. `complete` 复核导出链路、统计链路和数据库证据，确认根因。
2. `complete` 补 RED 测试，锁定导出实出勤小时必须按统计页面口径计算。
3. `complete` 实现最小修复，让行政导出复用审批加班/扣减后的实际小时。
4. `complete` 运行后端定向回归和编译验证。
5. `complete` 更新需求/开发文档、findings 和 progress，并反馈批量核查结果。

## Current Assumptions
- 本轮只修改行政体系考勤导出的实出勤小时来源，不改变 H-S 审批/异常列聚合，也不改变前端下载日期弹窗。
- 对 `expected_attendance_days > 0` 的员工，实出勤小时应与加班/夜班统计页面同口径：`应出勤天数 * 8 + 审批加班小时 - 事假/病假/调休/年假扣减小时`，结果小于 0 时按 0。
- 行政体系员工的实际出勤加班来源为本地审批快照 `tbattendanceapprove`，仍按 `employeeId -> tbattendanceuser.userId` 精确匹配，并排除 `statisticsStatus=取消至统计`。
- `expected_attendance_days = 0` 的极端历史行暂不套用上述公式，避免把页面的打卡兜底逻辑误替换为负数公式。

## Verification
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于 `expected:<185.5> but was:<184.0>`。
- GREEN：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过 14 个测试。
- 回归：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 18 个测试。
- 编译：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 数据核查：`hr_0003 / 2026-06` 行政体系应出勤大于 0 员工共 38 人，其中 19 人旧导出实出勤与统计口径不一致。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 考勤汇总下载行政体系考勤

## Goal
- 在考勤汇总页新增“下载行政体系考勤”按钮。
- 后端新增导出接口，按用户提供模板生成行政体系考勤汇总 Excel。
- 导出表头动态写入当前年月、当前登录账号所属公司、当前日期；表格只保留 A-S 列。
- 追加：下载前必须选择日期，导出数据按所选日期所在月份生成。

## Phases
1. `complete` 复核考勤汇总、加班/夜班统计、审批数据和模板结构。
2. `complete` 补 RED 测试，锁定导出接口、模板列结构、数据来源和前端按钮/API。
3. `complete` 实现后端模板资源、导出数据聚合和 Excel 写入。
4. `complete` 实现前端下载按钮、API 和 Blob 下载交互。
5. `complete` 运行定向后端/前端验证，更新需求/开发文档、findings 和 progress。
6. `complete` 追加下载日期选择弹窗，导出请求按所选日期拆分年月。

## Current Assumptions
- 追加后目标月份由下载弹窗选择日期决定；例如选择 `2026-06-15` 时，后端收到 `year=2026/month=06` 并导出 2026 年 6 月数据。
- “当前年月”用于标题；“制表时间”使用服务器当前日期。
- “编制单位”优先读取 `CompanyContext.get().getCompanyName()`，缺失时尝试使用顶级公司部门名称兜底。
- 新增 C 列为员工真实部门名称；删除原模板“公休/假期”列，保留原 H-S 的审批/异常/加班字段作为目标 H-S，最终只输出到 S 列。
- 月天数、应出勤小时、实出勤小时、应计出勤小时来自 `hrm_overtime_night_statistics_detail` 聚合；H-S 来自本地审批快照 `tbattendanceapprove` 和已同步钉钉报表/统计中可归属审批的本地数据。

## Verification
- RED 后端：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于缺少 `AdministrativeAttendanceReportMetricVO`，确认行政体系考勤导出契约尚未实现。
- RED 前端：`node tests/upload-attendance-page.test.mjs` 失败于缺少 `downloadAdministrativeAttendance` API，确认前端下载入口尚未实现。
- GREEN 后端：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 17 个测试。
- GREEN 前端：`node tests/upload-attendance-page.test.mjs` 通过。
- BUILD 后端：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- BUILD 前端：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- FRESH 复核：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 17 个测试；`mvn -DskipTests compile`、`node tests/upload-attendance-page.test.mjs`、`npm run build` 均通过。
- 追加日期选择验证：`node tests/upload-attendance-page.test.mjs` 通过；`npm run build` 通过；`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 17 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| Python 单行读取 Excel 时换行转义导致 `SyntaxError` | 1 | 改用 heredoc 只读脚本成功解析模板结构。 |

# Task Plan: 排班管理导入连班与矩阵标识

## Goal
- 排班管理上传排班时，Excel 的“是否连班”列有内容则按表格值保存；为空时按员工表 `is_continuous_shift` 自动配置。
- 下载排班模板新增“电话”列；上传时电话有内容按“姓名 + 电话”匹配，电话为空则按姓名匹配。
- 排班管理矩阵单元格中，自定义白班连班显示“连”，休息/调休也直接显示在单元格中。

## Phases
1. `complete` 复核后端/前端文档和现有导入、模板、矩阵展示链路。
2. `complete` 补 RED 测试覆盖模板电话列、导入姓名电话匹配、导入连班空值回退员工字段、矩阵“连/休息/调休”显示。
3. `complete` 实现后端导入和模板生成改动。
4. `complete` 实现前端矩阵显示与必要导入预览兼容。
5. `complete` 运行后端/前端定向验证，更新需求/开发文档、findings 和 progress。

## Current Assumptions
- “电话列”对应员工表手机号 `mobile`，模板列名使用“电话”，导入时兼容现有“手机号”列。
- “是否连班”列仅对自定义白班生效；夜班、休息、调休仍按既有规则强制非连班。
- 导入行有姓名和电话时必须共同匹配同一员工；电话为空表示业务侧确认无需电话区分，后端直接按姓名匹配。

## 2026-07-25 排班上传匹配与模板表头优化追加

### Goal
- 按用户最新反馈调整排班上传员工匹配：电话单元格有内容时按“姓名 + 电话”唯一匹配；电话为空时不再因为系统存在同名员工而报错，直接按姓名匹配。
- 修复下载 7 月排班模板时日期组视觉问题：日期表头应横向覆盖其子表头、居中显示，并按两种颜色循环区分每个日期组。

### Phases
1. `complete` 定位旧规则测试和实现。
2. `complete` 调整红灯测试覆盖新匹配规则和模板表头样式。
3. `complete` 修改 `WorkPlanServiceImpl` 与 `WorkPlanListController`。
4. `complete` 更新需求/开发文档、findings 和 progress。
5. `complete` 运行聚焦测试、必要回归和编译验证。

## Verification
- RED 后端：新增/调整测试先失败于缺少 `customContinuousShiftExplicit` getter/setter、模板电话列和同名缺电话规则。
- RED 前端：矩阵单元格最初仍把“连班”放在主文案，未拆成 `subText: 连`。
- GREEN 后端：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest,HrmEmployeeMapperSqlTest test` 通过 60 个测试。
- GREEN 追加后端：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 54 个测试，覆盖电话为空按姓名匹配、日期组合并居中、31 号后无效列清空和双色分组。
- GREEN 后端：`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- GREEN 前端：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs` 通过。
- BUILD 前端：`npm run build` 通过，保留既有 `::v-deep` 过时警告与 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 排班员工连班自动带出

## Goal
- 添加或修改员工排班时，根据员工表维护的“是否连班”字段自动设置排班记录的“是否连班”。
- 保留排班功能与排班管理现有入口、字段语义和手动编辑边界；先通过测试锁定期望行为，再做最小代码改动。

## Phases
1. `complete` 复核项目需求/开发文档和既有排班、员工字段规则。
2. `complete` 定位排班添加/修改服务、排班管理编辑接口、员工“是否连班”字段来源。
3. `complete` 补 RED 测试，覆盖添加排班与排班管理修改员工排班时自动读取员工字段。
4. `complete` 实现最小后端逻辑，并按需要补充前端或 SQL 契约。
5. `complete` 运行定向验证，更新需求/开发文档、findings 和 progress。

## Current Assumptions
- 员工表连班字段为开发库已存在的 `hrm_employee.is_continuous_shift`，取值 `1=是，2=否`。
- 自动设置应发生在保存排班记录前，以员工档案当前值为准。
- 只有白班自定义排班可保留连班；夜班、休息、标准班次仍强制非连班。

## Verification
- RED：`mvn -Dtest=WorkPlanServiceImplTest test` 失败于 `HrmEmployee#setIsContinuousShift` 和 `tbattendanceuserRepository#findAllByUserIdIn` 缺失。
- RED：`mvn -Dtest=HrmEmployeeMapperSqlTest#employeeContinuousShiftMigrationScript_shouldAddEmployeeContinuousShiftColumn test` 失败于迁移脚本不存在。
- RED：`mvn -Dtest=WorkPlanServiceImplTest#buildLocalCustomAssignments_shouldReuseResolvedCustomShiftWhenUsersShareContinuousFlag test` 失败于相同设置多员工重复创建自定义班次。
- GREEN：`mvn -Dtest=WorkPlanServiceImplTest,HrmEmployeeMapperSqlTest test` 通过 42 个测试。
- GREEN：`mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest,HrmEmployeeMapperSqlTest test` 通过 56 个测试。
- BUILD：`mvn -DskipTests compile` 通过，主源码 1046 个文件编译成功。
- CHECK：本轮目标文件 `git diff --check` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 误用 `sh` 执行，出现 shell 语法错误 | 1 | 改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan` 重新执行。 |

# Task Plan: 考勤汇总高级筛选部门查询

## Goal
- 配合前端考勤汇总高级筛选新增真实部门查询条件。
- 后端查询入参新增 `deptIds`，通过员工档案 `hrm_employee.dept_id` 过滤考勤汇总列表。
- 保留现有 `department=1/2` 所属体系/考勤汇总部门类型筛选语义。

## Phases
1. `complete` 复核考勤汇总后端查询文档、BO 和 Mapper。
2. `complete` 补 RED 测试，锁定 `deptIds` 入参和 SQL 过滤。
3. `complete` 实现最小后端查询契约。
4. `complete` 运行后端定向测试，配合前端联调验证。
5. `complete` 更新需求/开发文档和进度记录。

## Verification
- RED：`mvn -Dtest=HrmProduceAttendanceMapperXmlTest test` 失败于 `QueryMonthAttendanceBO` 缺少 `deptIds`。
- GREEN：`mvn -Dtest=HrmProduceAttendanceMapperXmlTest test` 通过 3 个测试。
- 前端验证：`node tests/upload-attendance-page.test.mjs`、`npm run build` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 个税附加导入模板下载与年月导入校验

## Goal
- 个税累计、附加累计、年度附加扣除页面分别新增“下载数据模版”按钮。
- 三个模板以业务提供文件为准：
  - `/Users/jiangyongming/Desktop/导入模版/个税累计.xls`
  - `/Users/jiangyongming/Desktop/导入模版/附加扣除累计.xls`
  - `/Users/jiangyongming/Desktop/导入模版/专项扣除累加表.xlsx`
- 个税累计和附加累计导入前必须先选择年月；未选择年月时前端禁止提交并给出中文提示。

## Phases
1. `complete` 复核前后端文档、页面、API 与既有下载模式。
2. `complete` 补 RED 测试，锁定三个下载接口、三个页面按钮和两个年月导入拦截。
3. `complete` 实现后端固定模板下载接口与 classpath 模板资源。
4. `complete` 实现前端 API、按钮、loading/disabled、Blob 下载和导入前置校验。
5. `complete` 运行定向测试/构建，更新需求与开发文档。

## Current Assumptions
- 固定模板应放入后端 `src/main/resources/export`，由对应 controller 前缀下的 `GET` 接口输出 Excel Blob。
- 前端下载沿用现有员工模板下载交互：`responseType=blob`、按钮 loading/disabled、失败中文提示。
- 附加累计已有年月导入校验，本轮需要保留并补下载按钮；个税累计需要新增未选择年月禁止导入。

## Verification
- RED 后端：`mvn -Dtest=HrmPersonalIncomeTaxControllerTest,HrmAdditionalTemplateControllerTest test` 测试编译失败，三个 controller 缺下载方法。
- RED 前端：`node tests/salary-tax-additional-template-api.test.mjs` 失败于下载 API 未导出；`node tests/salary-tax-additional-template-ui.test.mjs` 失败于页面缺下载按钮。
- GREEN 后端：同一 Maven 命令通过 3 个测试。
- GREEN 前端：`node tests/salary-tax-additional-template-api.test.mjs`、`node tests/salary-tax-additional-template-ui.test.mjs`、`node tests/addition-month-filter.test.mjs` 通过。
- 代码审查跟进 RED：后端空年月用例失败于返回成功且提示为空；前端 UI 测试失败于上传入口静默 disabled。
- 代码审查跟进 GREEN：后端同一 Maven 命令通过 5 个测试；前端同一 API/UI/附加累计年月测试通过。
- 构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端/前端本轮目标文件 `git diff --check` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 李凤皇 2026-06 出勤统计少 16 小时排查修复

## Goal
- 查明李凤皇 2026-06 加班/夜班统计中应出勤、实际出勤、应计出勤显示 `184 / 180 / 184` 的根因。
- 正确结果应为 `200 / 196 / 200`，即三项均比当前多 `16` 小时。
- 如确认是代码缺陷，先补失败测试再做最小修复；如是数据未重算或配置缺失，给出可核验证据和处理步骤。

## Phases
1. `complete` 复核统计服务、接口字段和既有测试，定位 `184/180/184` 的来源。
2. `complete` 查询李凤皇员工档案、所属体系、2026-06 统计明细和相关审批/日历配置，复算正确值。
3. `complete` 形成单一根因假设并用代码/数据证据验证。
4. `complete` 补 RED 测试并实现最小修复。
5. `complete` 运行定向回归，更新需求/开发文档和计划记录。

## Current Assumptions
- 目标租户优先沿用近期农谷/湖北 2026-06 排查中的 `hr_0003`，若姓名存在多条记录，必须用员工 ID、手机号、工号区分。
- 页面小时值来自加班/夜班统计月度字段：`expectedAttendanceDays * 8`、`actualAttendanceHours` 或 `actualAttendanceDays * 8`、`accruedAttendanceHours`。
- `184 -> 200` 表示应出勤从 `23` 天变为 `25` 天；`180 -> 196` 表示实际出勤从 `22.5` 天变为 `24.5` 天。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldUseFixedMonthlyRestAttendanceWhenRestTypeIsFixedEvenIfAffiliationIsAdministrative test` 旧实现失败，断言 `expected:<25> but was:<23>`。
- GREEN：同一测试修复后通过，1 个测试 0 失败。
- 统计服务回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过 56 个测试。
- 薪资/考勤汇总组合回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest test` 通过 133 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `tbattendanceuser.empName` 字段不存在 | 1 | 改用 `empId=1831601326890434571` 查询员工钉钉映射。 |
| 首次查询层直接覆盖所有正数历史应出勤导致部分测试在缺节假日 mock 时把 25 天误算为 26/27 天 | 1 | 收窄为仅在显式 `restType` 与所属体系旧分流冲突时刷新历史应出勤；完整统计服务测试通过。 |

# Task Plan: 加班夜班开始统计范围选择前端合并

## Goal
- 记录本轮跨项目前端改动：加班/夜班统计页删除独立“单人统计”按钮，将员工范围选择合并到“开始统计”入口。
- 后端全员统计和员工范围重算接口、统计业务口径不变。

## Phases
1. `complete` 复核后端文档中加班/夜班统计与单人统计接口契约。
2. `complete` 在前端 `hr_web` 完成入口合并和测试。
3. `complete` 更新后端项目需求/开发文档，记录本轮前端入口变更不影响后端统计逻辑。

## Verification
- 前端定向测试：`node tests/overtime-night-single-selection.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/salary-export-scope-dialog.test.mjs`、`node tests/overtime-night-page.test.mjs`、`node tests/overtime-night-api.test.mjs`、`node tests/salary-compute-scope-utils.test.mjs` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 基本工资设置保存后同步薪资档案基本工资

## Goal
- 保存“基本工资金额设置”后，将设置中的基本工资金额同步到每个未删除员工薪资档案中的 `10101 / 基本工资`。
- 同步动作应复用现有薪资档案表结构和工资项编码，不影响岗位工资、职务工资、全勤金额等其它工资项。
- 通过测试锁定保存基本工资设置会触发员工薪资档案基本工资更新。

## Phases
1. `complete` 定位基本工资设置保存接口、薪资档案表/Mapper 和现有测试结构。
2. `complete` 补 RED 测试，覆盖保存设置后同步所有未删除员工薪资档案 `10101`。
3. `complete` 实现最小同步逻辑，并保护空值/删除员工/其它工资项不被误改。
4. `complete` 运行定向测试和必要回归检查。
5. `complete` 更新需求/开发文档、findings 和 progress。

## Current Assumptions
- “薪资档案中的基本工资”对应 `hrm_salary_archives_option` 中工资项编码 `10101`。
- 只同步未删除员工；不存在薪资档案或不存在 `10101` 档案项的员工需先按现有代码确认是否创建还是跳过。
- 本轮只改后端保存设置后的同步行为；前端保存入口保持不变。

## Verification
- RED：`mvn -Dtest=HrmSalaryBasicServiceTest test` 失败 2 个断言，确认保存设置未同步 `salaryBasic`，源码缺少薪资档案同步逻辑。
- GREEN：`mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 9 个测试。
- 代码审查跟进 RED：`mvn -Dtest=HrmSalaryBasicServiceTest test` 失败 2 个断言，覆盖空 `salaryBasic` 跳过同步和避免全量员工 ID 拼 `IN`。
- 代码审查跟进 GREEN：`mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 10 个测试。
- 回归：`mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest test` 通过 75 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 误用 `sh` 执行，出现 shell 语法错误 | 1 | 改用 `python3 ~/.codex/skills/planning-with-files/scripts/session-catchup.py "$(pwd)"` 重新执行。 |

# Task Plan: 员工级全勤金额设置

## Goal
- 在基本工资金额设置中，普通员工全勤金额和领导全勤金额后分别新增“设置”入口。
- 设置入口支持按人员、按部门选择范围，把对应类型的全勤金额批量写入员工表。
- 右侧范围为空时表示更新全体员工对应全勤金额。
- 薪资核算删除 `40102 / 全勤奖` 的 `100/500` 硬编码，优先读取员工表中的员工级全勤金额；员工表金额为空时回退基本工资金额设置及默认值。

## Phases
1. `complete` 复核基本工资设置、员工表、计薪员工查询和薪资核算现有实现。
2. `complete` 补 RED 测试：员工表字段、批量设置接口、计薪员工全勤金额来源、薪资核算不再硬编码 100/500。
3. `complete` 实现后端字段、SQL、接口、服务和薪资读取逻辑。
4. `complete` 实现前端两个设置按钮和范围选择弹窗，复用现有按人员/按部门选择工具。
5. `complete` 执行 SQL（如需要）、运行后端/前端定向测试和构建验证。
6. `complete` 更新需求/开发文档、findings 和 progress。

## Current Assumptions
- 员工表新增两个员工级金额字段：普通员工全勤金额、领导全勤金额；核算时按员工岗位/领导识别结果选择对应字段。
- 批量设置金额使用基本工资设置页面当前输入框金额；若范围为空，则更新所有未删除员工的对应字段。
- 按部门选择由前端展开为员工 ID 集合提交，后端统一按员工 ID 列表或空列表处理。

## Verification
- RED 后端：`mvn -Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryMonthRecordServiceNewTest test` 初次失败 6 个断言，覆盖员工表金额字段、迁移脚本、批量设置接口、计薪员工 SQL 和薪资服务兜底分支。
- RED 前端：`node tests/salary-basic-settings-page.test.mjs` 初次失败于普通员工全勤金额缺少员工级“设置”按钮。
- GREEN 后端：同一 Maven 定向测试通过，77 个测试 0 失败。
- GREEN 前端：`node tests/salary-basic-settings-page.test.mjs` 通过。
- SQL：`docs/sql/2026-07-22_hrm_employee_full_attendance_amount.sql` 已在 dev MySQL `153.0.237.98` 执行；`hr_0001` 至 `hr_0005.hrm_employee` 均查到 2 个新增字段。
- SQL 幂等：同一脚本复跑返回码 0，字段已存在时跳过 DDL。
- 回归：`mvn -Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryMonthRecordServiceNewTest,HrmEmployeeServiceImplCompanyAgeTest,HrmSalaryMonthEmpRecordMapperXmlTest test` 通过 83 个测试。
- 前端回归：`node tests/salary-basic-settings-page.test.mjs`、`node tests/salary-compute-scope-utils.test.mjs`、`npm run build` 通过；构建保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端目标文件 `git diff --check -- ...` 无输出；前端目标文件 `git diff --check -- ...` 无输出。
- 代码审查跟进 RED/GREEN：
  - RED：`node tests/salary-basic-settings-page.test.mjs` 失败于弹窗仍使用计薪员工接口；
  - RED：`mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest,HrmEmployeeServiceImplTransferSimpleEmpTest test` 失败于显式范围更新 0 行未报错、旧薪资服务 `fullMoney` 强转 `Long`、员工简表缺手机号；
  - GREEN：上述前端测试通过，上述 Maven 命令通过 74 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| shell 反引号导致 SQL 脚本替换命令失败 | 1 | 已改用 `apply_patch` 直接把 `AFTER rest_type` 替换为 `AFTER full_attendance`。 |

# Task Plan: 生产体系固定月休4天才统计加班夜班

## Goal
- 新增并接入员工休息制度字段：只有 `affiliation_system=2` 生产体系且 `rest_type=2` 固定月休 4 天的员工，才统计加班时间、加班费、夜班次数、夜班补贴等加班/夜班数据。
- 查找并修改与新规则冲突的后端业务逻辑，包括员工模型/Mapper、加班夜班统计、考勤汇总同步、薪资核算/导出、有加班费员工筛选、测试和项目文档。
- 保持行政/非固定月休员工的应出勤和薪资主流程可运行，但其加班/夜班相关金额和次数不参与统计或写薪资。

## Phases
1. `complete` 复核现有文档、休息制度来源和全项目冲突点。
2. `complete` 补 RED 测试，覆盖员工休息制度字段接入、加班夜班统计过滤、考勤汇总同步归零、薪资核算/导出归零。
3. `complete` 最小实现：接入 `rest_type`，统一 helper 判断“生产 + 固定月休4天”，替换旧生产体系单条件。
4. `complete` 运行定向测试、组合回归和冲突关键字扫描。
5. `complete` 更新 `docs/requirements.md`、`docs/development.md`、`findings.md`、`progress.md`。

## Current Assumptions
- `rest_type=1` 表示行政单双休，`rest_type=2` 表示固定月休 4 天，沿用文档中 `watch` 模块口径。
- 数据库已有或将通过迁移新增 `hrm_employee.rest_type`；后端本轮需要补模型、查询、导入/导出或 SQL 脚本中必要契约。
- 新规则主要限制“加班/夜班相关统计值和补贴金额”，不改变员工所属体系字段本身，也不把组织管理 `deptType=公司/部门` 纳入本次修改。

## Verification
- RED：`mvn -Dtest=HrmEmployeeMapperSqlTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryMonthRecordServiceNewTest,HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest test` 初次失败于缺少 `restType` 和 `isFixedRestProductionEmployee(...)`。
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldCalculateNightSubsidyFromNightShiftWhenUploadedSubsidyMissing test` 初次失败于 eligible 员工夜班次数存在但补贴为空时返回 `0`。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldCalculateNightSubsidyFromNightShiftWhenUploadedSubsidyMissing test` 通过。
- 编译：`mvn -DskipTests compile` 通过。
- 组合回归：`mvn -Dtest=HrmEmployeeMapperSqlTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmProduceAttendanceMapperXmlTest,HrmEmployeeServiceImplCompanyAgeTest,HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest,SalaryMonthRecordServiceNewTest test` 通过 144 个测试。
- 空白检查：本轮相关代码、Mapper、测试和文档 `git diff --check` 无输出。
- 文档冲突扫描：旧“尚未接入休息制度 / 夜班不看体系”结论已清理；剩余“只看生产体系”命中为明确的历史规则收紧说明。
- 冲突扫描：`rg -n "affiliationSystem|affiliation_system|restType|rest_type|isProduce|isProductionAffiliationSystem|overtimePay|nightSubsidy|nightShift|workOverTime|180101|180102|queryHasOverTimePayEmpList" src/main/java src/main/resources src/test -S`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 单双休与生产月休四天代码排查

## Goal
- 找出员工应出勤口径中“单双休”和“生产体系月休四天”的所有相关代码位置。
- 覆盖后端 `hainan` 项目；必要时扩展同级前端 `hr_web`，把页面入口、配置项、接口契约一并列出。
- 本轮只读排查，不修改业务逻辑。

## Phases
1. `complete` 复核项目文档中单双休、月休四天、应出勤统计相关规则。
2. `complete` 扫描后端源码、Mapper XML、SQL、测试与文档中的关键字和调用链。
3. `complete` 扩展扫描前端项目相关页面、接口和测试。
4. `complete` 汇总代码清单、业务含义和入口关系。

## Current Assumptions
- “单双休”对应后端 workweek 模块和加班/夜班统计中的行政体系月历工作日口径。
- “月休四天”对应生产体系员工的 `productionMonthlyRestDays` 配置，默认值为 `4`。
- 员工属于行政/生产体系仍以 `hrm_employee.affiliation_system` 为准：行政体系走单双休月历，生产体系走月休天数配置。

## Verification
- 后端关键词扫描：`rg -n "affiliation_system|affiliationSystem|isProduceDept|productionMonthlyRestDays|PRODUCTION_MONTHLY_REST_DAYS|monthlyRestDays|单双休|单休|双休|weekType|DAY_TYPE_|expectedAttendanceDays|legalHolidayRestDays|HrmWorkweek|hrm_workweek|月休|生产体系|行政体系" src/main/java src/main/resources src/test docs/sql -S`。
- 前端关键词扫描：`rg -n "workweek|Workweek|单双休|单休|双休|productionMonthlyRestDays|生产体系员工月度休息天数|overtimeNight|expectedAttendanceDays|affiliationSystem|isProduceDept|department\\s*[:=]|行政体系|生产体系|月休" src tests docs -S`。
- 已按真实路径逐段核准后端 `modules/workweek`、`imple/HrmOvertimeNightStatisticsServiceImpl`、`SalaryMonthRecordServiceNew`、Mapper XML、SQL、测试，以及前端 `workweek`、`overtime-night`、`manage/salary`、`attendance/upload` 代码行号。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 首次补测试 patch 未匹配真实上下文 | 1 | 已重新读取 import 和全勤测试附近代码，按真实位置补丁。 |

# Task Plan: 张雪梅 2026-06 应发与实发工资计算过程排查

## Goal
- 只读调取张雪梅在 `hr_0003` 2026-06 薪资中的应发工资 `210101` 和实发工资 `240101` 计算过程。
- 覆盖薪资月记录、工资项明细、薪资档案、考勤汇总、加班/夜班统计、请假审批、社保、公积金和个税来源。
- 输出能闭合到当前落库金额的计算公式和数据来源，不直接修改业务数据。

## Phases
1. `complete` 定位张雪梅员工档案、2026-06 薪资主记录和员工薪资明细。
2. `complete` 查询工资项明细并按应发、扣款、实发分类闭合。
3. `complete` 复核考勤、病假、全勤、社保公积金和个税来源。
4. `complete` 汇总结论并更新项目文档/排查记录。

## Current Assumptions
- 目标租户沿用近期薪资排查中的 `hr_0003`。
- 目标年月沿用近期张雪梅规则复核中的 `2026-06`。
- 本轮为只读排查；除文档和计划记录外，不修改薪资业务表。

## Verification
- 使用数据库只读查询核对员工、薪资主记录、薪资员工记录和工资项明细。
- 使用源码中的薪资项编码和计算逻辑解释金额闭合。
- 已用工资项明细重算验证：`应发=2758.00`，`应发-代扣小计=2133.71`，与 `240101 / 实发工资` 一致。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `hrm_salary_month_record.record_name` 不存在 | 1 | 已查看表结构，改用真实字段 `title` 查询薪资主记录。 |
| `tbattendanceuser.emp_id` 不存在、`hrm_tax` 表不存在 | 1 | 已查看实际表结构，改用 `empId`；通过表名扫描定位个税累计相关表。 |
| 手工 SQL 交叉校验把 `160102/工会费` 误放入应发工资扣减 | 1 | 已按源码应发父级范围修正，并单独加入 `281 / 其他补贴` 后重算闭合。 |

# Task Plan: 病假小时误当作天数扣款修复

## Goal
- 修复 `SalaryMonthRecordServiceNew#sickDeductDays(..., type="2")` 行政体系病假折算问题。
- `hrm_attendance_report_data` 中病假按小时累计；行政体系和生产体系都应按 `小时 / 8` 转成病假天数。
- 张雪梅 2026-06 两条 `8小时` 病假应按 `2天` 判断，`19010401 / 病假扣款` 为 `0`，但仍不生成 `40102 / 全勤奖`。

## Phases
1. `complete` 补 RED 测试，复现行政体系 `16小时` 病假被误当 `16天` 的问题。
2. `complete` 修改最小生产代码：行政体系病假与事假一样按小时除以 8。
3. `complete` 运行定向测试和薪资组合回归。
4. `complete` 更新需求/开发文档、findings 和 progress。

## Current Assumptions
- `HrmAttendanceSummaryDayVo.bingjia` 来源为 `hrm_attendance_report_data.field_name='病假'` 的小时数。
- 生产体系分支当前已按 `小时 / 8` 折天，本次主要修复行政体系分支。
- 本轮不直接修改历史已生成工资项；修复后需重新核算对应员工/月度薪资才会改落库金额。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#sickDeductDays_shouldConvertAdministrativeSickLeaveHoursToDays test` 在旧逻辑下失败，期望行政体系 `8+8小时` 病假返回 `2.00` 天。
- GREEN：同一命令修复后通过，1 个测试 0 失败。
- 定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 58 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过 80 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 应出勤天数按所属体系公式获取

## Goal
- 修改并加固应出勤天数获取逻辑：
  - `affiliation_system=1` 行政体系属于单双休，按调用方传入的年-月读取“单双休设置”当月日历汇总中的出勤天数。
  - `affiliation_system=2` 生产体系属于月休员工，按 `当月自然天数 - 基本工资金额设置.productionMonthlyRestDays - 工作日法定休息日` 获取应出勤天数。
  - `productionMonthlyRestDays` 必须从基本工资金额设置读取，缺失时只允许走 `HrmSalaryBasicDefaults` 默认逻辑，禁止在业务公式中硬编码 `4`。
  - 所有薪资核算、导出、应出勤参与计算的地方，都必须使用上述公式落库后的加班/夜班统计应出勤结果，不允许硬编码或旧考勤配置兜底。

## Phases
1. `complete` 复核当前实现和所有出勤/应出勤关键字，找出可能绕过公式的路径。
2. `complete` 补 RED 测试：行政应出勤必须按传入年月读取单双休月历汇总；生产应出勤必须读取配置月休天数；薪资消费不得出现硬编码默认应出勤。
3. `complete` 实现最小修正，删除或阻断违规兜底。
4. `complete` 运行加班/夜班统计、基本工资设置、薪资核算和 Mapper 定向回归。
5. `complete` 更新 `docs/requirements.md`、`docs/development.md`、`findings.md`、`progress.md`。

## Current Assumptions
- 员工所属体系字段仍以 `hrm_employee.affiliation_system` 为准，`1=行政体系`、`2=生产体系`。
- 行政体系的单双休汇总来源为 `HrmWorkweekSettingService#queryMonthCalendar(year, month).workDays`。
- 生产体系法定休息日扣减只扣工作日法休，周末法休不重复扣。
- 薪资链路消费加班/夜班统计明细的 `expected_attendance_days`，缺失时阻断，不再回退 `need_work_day`、`hrm_attendance_info` 或默认天数。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveSalaryNormalDaysForCompute_shouldNotFallbackToExistingNeedWorkDayWhenStatisticsMissing+resolveSalaryNormalDaysForCompute_shouldUseOvertimeNightExpectedDaysInsteadOfAttendanceMapValue test` 初次失败于历史 `needWorkDay=21.75` 和旧考勤 map `21.75` 被优先读取。
- 行政年月保护：`HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldReadAdministrativeExpectedAttendanceFromRequestedMonthWorkweekSummary` 验证请求 `2026-05` 时调用单双休月历 `year=2026/month=5` 并读取 `workDays=24`。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveSalaryNormalDaysForCompute_shouldNotFallbackToExistingNeedWorkDayWhenStatisticsMissing+resolveSalaryNormalDaysForCompute_shouldUseOvertimeNightExpectedDaysInsteadOfAttendanceMapValue,HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldReadAdministrativeExpectedAttendanceFromRequestedMonthWorkweekSummary test` 通过 3 个测试。
- 定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 57 个测试。
- 定向：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmSalaryBasicServiceTest test` 通过 58 个测试。
- 组合回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,AttendanceInfoSourceTest test` 通过 122 个测试。
- 扫描：`rg -n "21\\.75|DEFAULT_NORMAL_DAYS|daysInMonth\\s*-\\s*4|lengthOfMonth\\(\\)\\s*-\\s*4|resolveAttendanceDataExpectedDays|normalizePositiveAttendanceDays|record\\.getNeedWorkDay\\(\\)" src/main/java src/main/resources -S` 无命中。
- 格式检查：`git diff --check -- src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java src/test/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImplTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 清理薪资考勤 deptType 体系硬编码

## Goal
- 删除薪资/考勤链路中用 `deptType` 代表行政/生产体系的硬编码。
- 行政/生产体系统一从员工表 `hrm_employee.affiliation_system` 获取。
- 核算薪资和导出薪资时，行政体系员工不得生成加班费 `180101`；只有 `affiliation_system=2` 生产体系员工可按考勤汇总加班工资或加班小时计算加班费。

## Phases
1. `complete` 复核文档和当前 `deptType`/`affiliation_system` 调用点，区分组织类型与员工体系。
2. `complete` 补 RED 测试：薪资服务源码不得再使用 `normalDaysByDeptType/queryNormalDaysByDeptType`，行政员工即使旧 `isProduceDept=1` 也无加班费，考勤天数配置不再暴露行政/生产体系类型。
3. `complete` 实现最小代码改动，清理薪资体系 `deptType` 参数和考勤配置兜底。
4. `complete` 运行定向和薪资组合回归。
5. `complete` 更新项目需求/开发文档、findings/progress。

## Current Assumptions
- 组织管理里的 `deptType=1公司/2部门` 不是员工所属体系，本轮不删除，否则会破坏组织管理。
- 后端员工所属体系字段仍为 `affiliation_system`，`1=行政体系`、`2=生产体系`。
- 考勤配置表 `hrm_attendance_info.dept_type` 不再参与薪资核算/导出体系判断。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#isProductionAffiliationSystem_shouldUseEmployeeAffiliationAndIgnoreLegacyEmployeeOverrides+salaryMonthRecordServiceSource_shouldNotUseAttendanceInfoOrInternalSystemType test` 初次失败于生产代码缺少新方法且旧应出勤方法仍带配置参数。
- RED：`mvn -Dtest=AttendanceInfoSourceTest test` 初次失败于 `QueryAttendanceInfoBO` 仍暴露行政/生产体系类型字段。
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#buildSalaryDataRow_shouldZeroOvertimePayForAdministrativeEmployee test` 初次失败于行政体系员工仍按历史薪资项导出 `180101=999.00`。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#isProductionAffiliationSystem_shouldUseEmployeeAffiliationAndIgnoreLegacyEmployeeOverrides+salaryMonthRecordServiceSource_shouldNotUseAttendanceInfoOrInternalSystemType+getYeBanAndJiaBan_shouldNotCreateOvertimePayForAdministrativeEmployee test` 通过 3 个测试。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#buildSalaryDataRow_shouldZeroOvertimePayForAdministrativeEmployee test` 通过 1 个测试。
- 定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test` 通过 57 个测试。
- 考勤配置：`mvn -Dtest=AttendanceInfoSourceTest test` 通过 1 个测试。
- 薪资组合回归：`mvn -Dtest=AttendanceInfoSourceTest,SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过 78 个测试。
- 扫描：`rg -n "deptType|dept_type" src/main/java src/main/resources src/test -S` 仅剩组织管理 `HrmDept/AddDeptBO/DeptVO/DeptEmployeeVO`；前端同类扫描仅剩组织管理页公司/部门类型。
- 格式检查：目标代码、测试、文档和计划文件 `git diff --check` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `isProductionAffiliationSystem(null)` 重载歧义 | 1 | 测试改为 `(Integer) null` 明确调用所属体系值判断。 |

# Task Plan: 员工所属体系使用点全系统排查

## Goal
- 找出整个后端项目中所有使用员工所属体系的代码、SQL、配置和文档位置。
- 重点识别 `hrm_employee.affiliation_system`、`HrmEmployee.affiliationSystem`、`isProduceDept`、`deptType`、行政/生产体系推导逻辑，以及可能仍按部门名称/岗位/员工 ID 判断体系的遗留代码。
- 输出按业务模块归类的清单，便于后续业务逻辑变更时逐项评估影响。

## Phases
1. `complete` 复核项目需求/开发文档中所属体系相关业务规则。
2. `complete` 扫描后端源码、Mapper XML、SQL、测试与文档中的所属体系字段和派生字段。
3. `complete` 扩展扫描可能相关的前端项目引用。
4. `complete` 汇总使用点、业务含义、风险点和建议调整顺序。

## Current Assumptions
- “员工所属体系”优先指员工档案字段 `hrm_employee.affiliation_system` / Java 字段 `affiliationSystem`。
- 现有业务约定：`1=行政体系`，`2=生产体系`；薪资内部历史 `deptType` 映射为行政 `0`、生产 `1`。
- 本轮先做只读影响面排查，不修改业务代码。

## Verification
- 精确字段扫描：`rg -n "affiliation_system|affiliationSystem|isProduceDept|deptType" src/main/java src/main/resources src/test docs -S`。
- 间接口径扫描：`rg -n "getAffiliationSystem|setAffiliationSystem|AFFILIATION_SYSTEM|resolve.*Affiliation|affiliation" src/main/java src/main/resources src/test -S`。
- 中文/旧逻辑扫描：`rg -n "所属体系|行政体系|生产体系|生产部门|行政部门|部门类型" src/main/java src/main/resources src/test -S`。
- 前端扫描：`rg -n "affiliationSystem|affiliation_system|isProduceDept|deptType|所属体系|行政体系|生产体系|生产部门|行政部门|productionMonthlyRestDays" src tests docs -S`。
- SQL 配置扫描：`rg -n "affiliation_system|affiliationSystem|所属体系|生产体系|行政体系" docs/sql -S`。
- 空白检查：后端 `git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出；前端 `git -C /Users/jiangyongming/Project/hr/hr_web diff --check -- docs/requirements.md docs/development.md` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 张雪梅两天病假扣全勤

## Goal
- 确认并修复“病假 2 天内不扣病假工资，但应扣全勤奖”的薪资核算规则。
- 保持 `19010401 / 假期扣款(病假)` 现有 2 天内为 `0` 的规则不变。
- 防止全勤兜底逻辑仅因应计出勤达到应出勤，就给存在病假申请的员工生成 `40102 / 全勤奖`。

## Phases
1. `complete` 复核项目文档、历史全勤/病假规则和张雪梅相关记录。
2. `complete` 定位薪资核算全勤判断、病假天数计算和应计出勤兜底的调用顺序。
3. `complete` 补 RED 测试，复现“两天病假不扣病假工资但不发全勤奖”。
4. `complete` 实现最小修复并运行定向薪资测试。
5. `complete` 更新需求/开发文档、findings/progress 并做完成前验证。

## Current Assumptions
- 目标问题延续近期 `hr_0003 / 2026-06 / 张雪梅` 薪资与全勤奖排查。
- 病假工资扣款阈值仍为 2 天内不扣；本轮只调整全勤奖资格判断，不改变病假扣款金额。
- 张明式“缺卡/短时迟到但最终无考勤扣款”的全勤兜底仍应保留；本轮只把病假这类明确扣全勤的请假排除在兜底之外。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#shouldFallbackFullAttendanceByAccruedDays_shouldRejectEffectiveSickLeaveEvenWhenAccruedReachesExpected test` 失败于 `shouldFallbackFullAttendanceByAccruedDays` 缺少有效病假天数入参。
- GREEN：同一新增测试通过。
- 定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 52 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过 74 个测试。
- 格式检查：`git diff --check -- src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 开始核算界面计薪员工规则说明

## Goal
- 在薪资管理“开始核算”弹窗中增加一行说明，明确当月计薪员工按什么规则获取。
- 说明文案必须对应后端当前计薪员工口径：未删除、入职不晚于薪资结束日、在职/待离职直接纳入、离职员工按计划离职日期阈值纳入，并要求有合计金额大于0的薪资档案。
- 共享弹窗继续服务“导出薪资”，但导出弹窗不展示开始核算专用规则说明。

## Phases
1. `complete` 复核项目文档、后端 SQL 规则、前端共享弹窗和现有测试。
2. `complete` 补前端 RED 测试，要求开始核算传入规则说明、共享弹窗支持可选说明、导出弹窗不传该说明。
3. `complete` 实现最小前端改动：`AloneComputeDialog.vue` 新增 `ruleTip`，`SalaryManage.vue` 只在开始核算实例传入文案。
4. `complete` 运行定向测试、共享范围回归和前端构建。
5. `complete` 更新前后端需求/开发文档和计划记录。

## Verification
- RED：`node tests/salary-start-compute-dialog.test.mjs` 失败于开始核算弹窗缺少规则说明。
- RED：`node tests/salary-export-scope-dialog.test.mjs` 失败于共享弹窗缺少 `ruleTip` 支持。
- GREEN：`node tests/salary-start-compute-dialog.test.mjs` 通过。
- GREEN：`node tests/salary-export-scope-dialog.test.mjs` 通过。
- 回归：`node tests/salary-compute-scope-utils.test.mjs` 通过 9 个用例。
- 回归：`node tests/compute-progress-utils.test.mjs` 通过 4 个用例。
- 构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 首次补测试时使用了不存在的断言上下文 | 1 | 按实际 `salary-start-compute-dialog.test.mjs` 断言位置拆成小补丁。 |

# Task Plan: 薪资核算穿梭框员工来源统一

## Goal
- 确保薪资核算弹窗中“按人员”和“按部门”两种模式，都只使用 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList` 返回的可核算员工。
- 部门树只作为范围选择器，最终展开出的 `employeeIds` 不能混入普通员工列表或非计薪员工。
- 用前端工具函数测试固定该行为，避免后续误改为按部门重新查全员。

## Phases
1. `complete` 复核项目文档、当前前后端实现和工作区状态。
2. `complete` 补前端 RED 测试，覆盖部门模式只从可核算员工列表展开员工。
3. `complete` 实现最小前端逻辑调整。
4. `complete` 运行定向测试和必要构建检查。
5. `complete` 更新需求/开发文档、findings/progress。

## Current Assumptions
- 后端 `queryComputeSalaryEmployeeList` 已经复用 `queryHasSalaryArchivesEmployeeList`，本轮优先修正/加固前端范围构建逻辑。
- 若当前代码已经满足运行行为，也需要补充测试和文档，明确部门模式不得另查全员。
- 当前工作区存在大量未提交改动，本轮只修改与薪资核算范围选择直接相关的前端文件、测试和文档。

## Verification
- RED：`node tests/salary-compute-scope-utils.test.mjs` 失败于缺少 `filterSalaryComputeDeptTreeByEmployees` 导出；`node tests/salary-start-compute-dialog.test.mjs` 失败于弹窗未使用该过滤函数。
- GREEN：`node tests/salary-compute-scope-utils.test.mjs` 通过 9 个用例。
- GREEN：`node tests/salary-start-compute-dialog.test.mjs` 通过。
- 回归：`node tests/salary-export-scope-dialog.test.mjs` 通过。
- 回归：`node tests/compute-progress-utils.test.mjs` 通过 4 个用例。
- 构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：`git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出；前端目标文件空白检查无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: SalaryMonthRecordServiceNew 编译缺失排查

## Goal
- 查清 `SalaryMonthRecordServiceNew` 中“包找不到 / 找不到符号”的真实根因。
- 判断问题来自源码导入、类名/包名不一致、缺少未跟踪源码、Maven 依赖缺失，还是 IDE 未正确导入 Maven 项目。
- 在不回退工作区其他未提交改动的前提下，做最小修复并运行定向验证。

## Phases
1. `complete` 复核项目文档、历史记录和当前工作区状态。
2. `complete` 运行定向 Maven 编译/测试，收集完整错误信息。
3. `complete` 按错误定位 `SalaryMonthRecordServiceNew` 的 import、字段、方法和依赖来源。
4. `complete` 实施最小修复或给出明确 IDE/Maven 导入处理结论。
5. `complete` 运行验证并更新需求/开发文档、findings/progress。

## Current Assumptions
- 用户反馈来自 IDE 或编译器对 `SalaryMonthRecordServiceNew` 的提示，需要以 Maven 编译结果为准先复现。
- 当前工作区已有大量未提交改动，本轮只处理与 `SalaryMonthRecordServiceNew` 编译错误直接相关的文件。
- 若 Maven 编译通过而 IDE 仍报错，优先排查 IDE 项目导入、JDK、Maven profile 和注解处理配置。

## Verification
- `mvn -DskipTests compile` 初次通过，主源码 1044 个 Java 文件编译成功；说明 `SalaryMonthRecordServiceNew` 本身不存在 Maven/Javac 视角的“包找不到/符号找不到”。
- `mvn -Dtest=SalaryMonthRecordServiceNewTest test` 初次通过，43 个测试。
- 修正全局 Maven `settings.xml` 后，`mvn -DskipTests compile` 再次通过，原 `settings.xml` 的两条警告消失。
- 修正后 `mvn -Dtest=SalaryMonthRecordServiceNewTest test` 再次通过，43 个测试。

## Errors Encountered

# Task Plan: 薪资导出社保多 3 元排查

## Goal
- 查清导出的薪资表中社保部分对所有员工多出 `3` 元的根因。
- 固定涉及的导出字段、薪资项编码、社保月记录/方案金额来源，以及是否为长期护理保险固定金额重复叠加。
- 先定位根因和影响范围，不在未确认前修改薪资、社保或员工参保数据。

## Phases
1. `complete` 复核项目文档、历史社保固定金额和薪资导出口径。
2. `complete` 定位薪资导出中个人社保字段的取数与写出链路。
3. `complete` 对比薪资项、社保员工月记录、参保方案项目和导出中间表金额。
4. `complete` 形成单一根因假设并用全员样本验证。
5. `complete` 更新 findings/progress 和项目文档，向用户汇总根因与处理建议。

## Current Assumptions
- 用户说“所有员工多 3 块钱”，优先怀疑固定长期护理保险金额 `3` 在社保明细或导出字段中被重复叠加。
- 目标租户优先按近期薪资/社保排查记录中的 `hr_0003` 理解；目标月份从最新薪资记录或导出参数/落库数据确认。
- 本轮先做只读根因排查，除计划和文档记录外不修改业务数据。

## Verification
- 代码链路：导出“个人社保”取工资项 `100101`，工资项同步社保时取 `hrm_insurance_month_emp_record.personal_insurance_amount`。
- 【历史排查值，2026-08-22 起不再参与社保合计】社保固定金额：当时最新 `hrm_salary_basic.long_term_care_insurance_amount=3.00`。
- 数据闭合：`hr_0003` 2026-06 社保月记录 84 名正常参保员工全部满足 `personal_insurance_amount - 项目个人社保合计 = 3.00`。
- 数据闭合：`hr_0003` 2026-06 薪资中 83 名有社保扣款员工全部满足 `100101 - 项目个人社保合计 = 3.00`。
- 文档/计划格式检查：`git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 吴镜平离职后仍生成 2026-06 薪资排查

## Goal
- 查清吴镜平已离职但仍出现在 2026-06 薪资中的原因。
- 固定其员工档案状态、离职记录、薪资档案、考勤汇总/统计、2026-06 薪资月员工记录和计薪员工 SQL 过滤条件。
- 先给出根因；未经确认不直接删除薪资记录或修改员工数据。

## Phases
1. `complete` 查询吴镜平员工状态、离职日期、薪资档案和 2026-06 薪资记录。
2. `complete` 定位计薪员工查询 SQL 和离职员工纳入口径。
3. `complete` 判断是业务口径、旧数据残留、手工选择范围，还是代码过滤缺陷。
4. `complete` 给出处理建议和必要的后续修正点。

## Current Assumptions
- 目标租户为 `hr_0003`。
- 目标薪资月份为 `2026-06`，薪资主记录沿用近期 `2077807285177995265 / 六月薪资报表`。
- 本轮先只读排查，不修改薪资数据。

## Verification
- 吴镜平档案：`entry_status=4`、`is_del=0`、手机号 `13797928108`、工号 `1718539734988`。
- 离职记录：`plan_quit_time=2026-06-10`、`salary_settlement_time=2026-06-10`。
- 计薪范围 SQL：离职员工 `entry_status=4` 且 `plan_quit_time > date_sub(endTime, interval 1 month)` 纳入；2026-06 薪资 `endTime=2026-06-30`，阈值为 `2026-05-30`，吴镜平命中。
- 2026-06 薪资明细：`need_work_day=23.00`、`actual_work_day=23.00`；考勤汇总同月 `positive_attendance=23.00`、`probation_attendance=23.00`。
- 工资项闭合：`10101=2130`、`10102=3070`、`40102=100`，`210101=5300.00`、`240101=5300.00`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 查询离职信息时误用 `hrm_employee_quit_info.id` | 1 | 读取表结构后改用实际主键 `quit_info_id`。 |
| 查询月薪员工表时误用 `hrm_salary_month_emp_record.employee_name` | 1 | 改为关联 `hrm_employee` 读取员工姓名。 |
| `rg` 查询 `src/main/resources/*.yml` 时 zsh 报 `no matches found` | 1 | 改查实际存在的 `application-*.properties` 文件。 |
| 查询 `hrm_salary_month_record.status` 报字段不存在 | 1 | 读取表结构后改用实际字段 `check_status`。 |
| 按姓名 join `hrm_salary_export` 与员工表时报 `Illegal mix of collations` | 1 | 改用显式 `utf8mb4_general_ci` collation 做只读抽样对账。 |
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 采购计划部超缺勤工资排查

## Goal
- 查清采购计划部员工核算后为什么普遍生成“超缺勤工资”。
- 固定涉及的工资项编码、计算公式、员工范围和年月数据来源。
- 先定位根因，不在未确认前修改业务代码或工资数据。

## Phases
1. `complete` 复核项目文档、历史薪资/考勤口径和本轮问题范围。
2. `complete` 定位“超缺勤工资”工资项编码、计算入口和依赖字段。
3. `complete` 查询采购计划部员工的薪资项、薪资月记录、考勤汇总和加班/夜班统计。
4. `complete` 对比工作正常/异常样本，形成并验证单一根因假设。
5. `complete` 更新 findings/progress 和项目文档，向用户汇总根因与处理建议。

## Current Assumptions
- “采购计划部”按当前租户 `hr_0003` 的部门名称理解；若数据库显示多个同名部门，需按部门 ID 区分。
- “超缺勤工资”先按工资项名称反查编码，不假设固定 code。
- 目标月份未由用户指定，优先从最新已生成薪资记录或采购计划部异常工资项所在年月确认。

## Verification
- 代码定位：`SalaryMonthRecordServiceNew#getFixedOptionValue(...)` 将考勤扣款子项累加为 `200101 / 超缺勤`；`190103 / 旷工扣款` 由 `empSalary / normalDays * absenteeismDays` 计算。
- 数据闭合：采购计划部 2026-06 三名有薪资记录员工 `need_work_day=21.75`、`actual_work_day=23.00`，`190103` 与 `200101` 均为负数。
- 金额闭合：严锦/庞龙斌 `4000 / 21.75 * (21.75 - 23) = -229.89`；黎冬霜 `4400 / 21.75 * (21.75 - 23) = -252.87`。
- 文档验证：已更新 `docs/requirements.md` 与 `docs/development.md` 记录根因、影响面和重算前处理要求。
- 格式验证：`git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 首次查询 `tbcompanylist.DbName` 报字段不存在 | 1 | 改查表结构，实际字段为 `companyId/companyName/database`。 |
| 并行查询中误放无效占位命令 `showcolumns_not_a_command` | 1 | 丢弃该无效输出，使用 MySQL `show columns` 继续读取真实表结构。 |

# Task Plan: 删除薪资核算 21.75 硬编码

## Goal
- 按采购计划部 2026-06 根因，修改薪资核算应出勤来源。
- 删除薪资核算代码中所有硬编码 `21.75` 的调用和默认兜底。
- 缺少应出勤来源时返回可定位的业务错误，不再静默用默认天数核算。

## Phases
1. `complete` 复核项目需求/开发文档和采购计划部排查证据。
2. `complete` 扫描薪资核算相关代码中所有 `21.75` 使用点和应出勤来源。
3. `complete` 补 RED 测试，锁定缺 `hrm_attendance_info` 时优先用加班/夜班统计应出勤，缺来源时报错。
4. `complete` 实现最小修复，移除 `SalaryMonthRecordServiceNew` 与活动备份核算服务里的硬编码默认天数。
5. `complete` 运行薪资定向/组合回归，更新需求、开发文档和计划记录。

## Current Assumptions
- 当前主核算入口使用 `SalaryMonthRecordServiceNew`；`SalaryMonthRecordService_Bak` 仍作为 Spring Bean 存在，需清理其中硬编码，避免后续任务/工资条链路误用旧默认。
- 2026-06 采购计划部应出勤来源以加班/夜班统计 `expected_attendance_days=23` 为准；缺少加班/夜班统计应出勤时直接提示到“单双休设置”维护数据，不再继续读取 `hrm_attendance_info` 作为兜底。
- 历史文档中记录“旧问题曾使用 21.75”的文字保留为排查证据；本轮删除范围聚焦核算生产代码和相关测试口径。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveSalaryExpectedAttendanceDays_shouldPreferOvertimeNightAndNotFallbackToDefaultDays+salaryComputeSources_shouldNotContainHardcodedDefaultAttendanceDays test` 首次编译失败，缺少 `resolveSalaryExpectedAttendanceDays(...)`。
- GREEN：同一 RED 命令修复后通过，2 个测试。
- 定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，49 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，72 个测试。
- 生产源码扫描：`rg -n "21\\.75|DEFAULT_NORMAL_DAYS" src/main/java src/main/resources -S` 无命中。
- 追加 RED/GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveSalaryExpectedAttendanceDays_shouldRequireOvertimeNightAndNotFallbackToAttendanceInfo+requireSalaryExpectedAttendanceDays_shouldPromptSingleDoubleRestSettingWhenOvertimeNightMissing+resolveFullAttendanceExpectedDays_shouldPreferOvertimeNightExpectedDays+resolveExportFullWorkDays_shouldRejectMissingOvertimeNightStatistics test` 先失败于旧 fallback 和旧错误文案，修复后通过 4 个测试。
- 2026-07-20 17:05 复核：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，72 个测试；生产源码 `rg -n "21\\.75|DEFAULT_NORMAL_DAYS" src/main/java src/main/resources -S` 无命中，相关文件 `git diff --check` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 薪资导出选择人员和部门

## Goal
- 在薪资导出时增加选择人员和部门的界面。
- 界面和控件逻辑参考“开始核算”功能，尽量复用现有选择范围交互。
- 保持导出范围传参和后端导出行为一致，避免影响默认全量导出。

## Phases
1. `complete` 复核项目需求/开发文档和现有计划记录。
2. `complete` 定位薪资导出入口、开始核算界面、人员/部门选择控件和后端导出参数。
3. `complete` 先补前端/后端定向 RED 测试，固定薪资导出范围选择行为。
4. `complete` 实现薪资导出选择人员/部门界面和必要的后端参数过滤。
5. `complete` 运行定向验证并更新需求/开发文档。

## Current Assumptions
- “薪资导出”指薪资管理页面中导出月度薪资相关 Excel 的入口，具体按钮需通过代码确认。
- “参考开始核算功能界面也控件逻辑”按复用开始核算的人员/部门范围选择组件、默认值、校验和传参结构理解。
- 若当前导出接口未接收人员/部门范围，需补后端按人员 ID 和部门 ID 过滤；若已支持，则只补前端传参。

## Verification
- RED 前端：`node tests/salary-compute-scope-utils.test.mjs` 失败于缺少 `buildSalaryExportPayload` 导出；`node tests/salary-export-scope-dialog.test.mjs` 失败于页面缺少导出范围弹窗状态。
- RED 后端：`mvn -Dtest=SalaryMonthRecordServiceNewTest#resolveExportEmployeeIds_shouldUseSelectedEmployeeIdsAndKeepCandidateOrder+resolveExportEmployeeIds_shouldFallbackToCandidatesWhenSelectionEmpty+resolveExportEmployeeIds_shouldReturnEmptyWhenSelectionHasNoCandidateMatch+salaryExportSource_shouldApplySelectedEmployeeIdsBeforeQueryingMonthList test` 编译失败，缺少 `resolveExportEmployeeIds(...)`。
- GREEN 前端：`node tests/salary-compute-scope-utils.test.mjs`、`node tests/salary-export-scope-dialog.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/compute-progress-utils.test.mjs` 均通过。
- GREEN 构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- GREEN 后端：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，69 个测试。
- 空白检查：后端目标文件 `git diff --check -- ...` 通过；前端目标文件尾随空白扫描无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 计划记录追加补丁上下文未匹配 | 初次使用较窄上下文同时更新三份计划文件 | 改为按文件末尾稳定上下文分别追加，并记录本错误 |
| 全局 Maven `settings.xml` 提示 `Unrecognised tag: 'profiles'` 和 mirror id 非法 | 1 | 修正 `/Users/jiangyongming/devTools/apache-maven-3.8.4/conf/settings.xml`：`<profiles>` 改为 `<properties>`，`nexus-aliyun>` 改为 `nexus-aliyun` |

# Task Plan: 薪资导出满勤天数按加班夜班统计

## Goal
- 修正薪资导出中“满勤天数”相关口径：按用户最新反馈，从加班/夜班统计“开始统计”已落库的应出勤时间与应计出勤时间取数。
- 查清 `hrm_overtime_night_statistics_detail` 中应出勤与应计出勤字段，并确认薪资导出应使用的计算公式和单位换算。
- 回退上次“满勤天数直接取员工月薪记录 actualWorkDay”的错误假设，按落库统计结果补回归测试和最小修复。

## Phases
1. `complete` 复核项目文档和纠正本次需求理解。
2. `complete` 定位加班/夜班统计落库字段、Repository/Mapper 和薪资导出取数链路。
3. `complete` 补 RED 测试，覆盖导出从统计落库结果计算目标值。
4. `complete` 实现最小修复，避免继续使用错误的 `actualWorkDay` 直取假设。
5. `complete` 运行定向验证，更新需求/开发文档、findings/progress。

## Current Assumptions
- 已确认用户点名的是薪资导出第 8 列“满勤天数” / `9007` / `{.normaldays}`，不是“超缺勤天数”列。
- 加班/夜班统计应出勤时间按 `expected_attendance_days * 8` 小时换算，应计出勤时间取 `accrued_attendance_hours`。
- 导出“满勤天数”按 `accrued_attendance_hours / 8` 输出天数。
- 导出“超缺勤天数”按 `(expected_attendance_days * 8 - accrued_attendance_hours) / 8` 输出天数；结果允许为负数。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 先编译失败于 `buildExportAbsenceDaysByEmployee(...)` 未实现。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，43 个测试。
- 回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，65 个测试。
- 格式检查：`git diff --check -- docs/requirements.md docs/development.md task_plan.md findings.md progress.md src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 薪资管理导出薪资空白排查

## Goal
- 查清薪资管理中“导出薪资”生成空白文件或空白内容的根因。
- 覆盖后端导出接口、Excel 模板/写出逻辑、工资项数据映射和必要的前端调用参数。
- 在确认根因后补最小回归测试并修复，避免只修症状。

## Phases
1. `complete` 复核项目文档与既有薪资导出相关历史约束。
2. `complete` 定位薪资导出入口、模板资源、服务方法和前端调用参数。
3. `complete` 通过测试或本地调用复现空白导出，记录证据和根因假设。
4. `complete` 补失败测试并实现最小修复。
5. `complete` 运行定向验证，更新需求/开发文档、findings/progress。

## Current Assumptions
- “导出薪资”指薪资管理页面导出工资明细/工资表的用户操作。
- 目标项目优先按当前后端 `hainan` 排查；如果根因在前端 `hr_web` 调用参数或下载处理，再按同一文档流程读取前端项目文档后修改。
- 工作区已有大量未提交改动，本轮只改薪资导出相关文件，不回退无关改动。

## Verification
- 本轮旧 `resolveExportNormalDays(...)` 兜底测试已被最新加班/夜班统计导出口径取代；导出不再依赖 `hrm_attendance_info` 正常天数。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，43 个测试。
- RED：`mvn -Dtest=HrmSalaryMonthRecordControllerTest test` 失败于导出异常被 controller 吞掉。
- GREEN：`mvn -Dtest=HrmSalaryMonthRecordControllerTest test` 通过，1 个测试。
- 回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，65 个测试。
- 格式检查：`git diff --check -- docs/requirements.md docs/development.md src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryMonthRecordController.java src/test/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNewTest.java` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 农谷 2026-06 批量个税支持数据整理与导入

## Goal
- 参考李明明已成功的 `2026-05` 个税累计与 `2026-06` 附加累计支持数据格式，整理其他员工的同类数据。
- 使用财务原始申报表和系统可导入模板格式，按“姓名 + 手机号”匹配当前在职员工。
- 将整理后的个税累计、附加累计数据上传到本机 `hr_0003` 数据库，供 2026-06 薪资核算自动计算使用。
- 不覆盖财务原始文件；生成批量导入副本并保留预览/异常清单。

## Phases
1. `complete` 复核李明明成功导入文件、财务原始申报文件、导入接口和当前库员工匹配状态。
2. `complete` 抽取其他员工 `2026-05` 个税累计、`2026-06` 附加累计、手机号和匹配结果，形成批量预览。
3. `complete` 生成可导入 Excel 副本，过滤已删除或无法匹配员工，保留异常清单。
4. `complete` 调用本机后端导入接口上传到 `hr_0003`，并读回数据库/API 核验导入数量和关键员工数据。
5. `complete` 更新需求/开发文档、findings/progress，汇总结果和未导入异常。

## Current Assumptions
- 目标仍是湖北田野农谷 `companyId=0003 / hr_0003`，本机后端为 `http://127.0.0.1:9080/hrsystem`。
- 6 月薪资按 7 月申报口径时，导入支持数据仍采用李明明成功的格式：个税累计导入年月 `2026-05`，附加累计导入年月 `2026-06`。
- 其他员工先按财务原始 `202607_税款计算_工资薪金所得.xls` 中同名行的累计字段整理；若原始表缺手机号，则优先沿用既有 `final_import_excels` 中的手机号匹配。
- `246.90/249.30` 这类李明明专门调整值只用于李明明；其他员工不做人工倒推补差，除非原始表或用户另行提供明确值。

## Verification
- 员工表基准预览文件：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税_员工表基准_预览_20260720.csv`。
- 员工表基准个税累计导入文件：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税_员工表基准_个税累计_导入2026-05.xls`，数据行 `96`。
- 员工表基准附加累计导入文件：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税_员工表基准_附加扣除累计_导入2026-06.xls`，数据行 `40`。
- 本机接口导入结果：`/hrmAdditional/importAdditional` 返回 `code=200`；`/hrmPersonalIncomeTax/importPersonalIncomeTax` 返回 `code=200`。
- 数据库只读核验：`hr_0003.hrm_personal_income_tax` 中 `2026-05` 个税累计 `96` 行；`hr_0003.hrm_additional` 中 `2026-06` 附加累计 `40` 行。
- 公式复核：李明明、范燕东、王洪平、宁明友四名 6 月工资表个税非零员工，按导入后的累计基础和附加累计复算，预测 6 月个税分别为 `2.40 / 22.19 / 2.40 / 2.40`，与工资表一致。
- 杨晨雨身份证修正后已纳入员工表基准导入，`2026-05` 个税累计为 `9543.06 / 15000.00 / 1179.60 / 0.00`。
- 姓名+手机号补充导入文件：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels/批量6月工资个税_员工表基准含姓名手机号_个税累计_导入2026-05.xls`，数据行 `97`；同后缀附加累计文件保持 `40` 行。
- 补充导入后数据库核验：`hr_0003.hrm_personal_income_tax` 中 `2026-05` 个税累计 `97` 行，新增朱玲丽 `10650.00 / 60000.00 / 3031.80 / 0.00`；`hrm_additional` 中 `2026-06` 仍为 `40` 行。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 农谷社保医保方案整理与员工参保方案设置

## Goal
- 从 `/Users/jiangyongming/Desktop/农谷导入数据` 中整理所有社保和医保参保方案。
- 按系统“社保方案管理”的表结构和金额合计口径，将整理后的方案写入租户库。
- 以员工表为基准，用“姓名 + 手机号”唯一匹配外部数据，把对应参保方案设置到员工档案。
- 全程避免只按姓名匹配，重名、缺手机号、无匹配和多匹配必须形成差异清单。

## Phases
1. `complete` 复核项目文档、社保方案/员工参保相关表结构、数据库连接和桌面源文件清单。
2. `complete` 抽取源文件中的社保/医保方案字段，归一化方案名称、项目、基数、个人/公司金额和员工姓名手机号。
3. `complete` 对照系统当前方案管理数据，生成待新增/复用方案清单、员工匹配清单和异常清单。
4. `complete` 先生成可回滚 SQL/导入脚本和预览统计；确认无阻断异常后执行写库。
5. `complete` 写入或复核社保方案、方案项目明细，并按员工 ID 更新员工参保方案。
6. `complete` 执行写后核验，更新需求/开发文档和本轮记录。

## Current Assumptions
- 目标租户已确认为 `hr_0003`；`hrsystem.tbcompanylist` 中 `CompanyID=0003` 对应数据库 `hr_0003`。
- 外部文件的“社保”和“医保”都归入系统社保方案管理；如文件中医保单列存在，需映射到系统可用的社保项目/方案明细。
- 员工唯一键按用户要求为 `姓名 + 手机号`；最终更新员工表时使用匹配出的 `employee_id`，不按姓名直接更新。
- 初始已按 `2026-07` 普通月度表执行一版当前员工参保方案设置；用户后续要求暂时改按 6 月工资表处理。
- 当前临时依据为 `/Users/jiangyongming/Desktop/农谷导入数据/副本加个税版-田野农谷2026年6月工资表(1)(1).xlsx` 的 `6月` sheet；其他 sheet 未参与本轮重整。
- 6 月工资表缺手机号时，只有员工表内姓名唯一，或姓名+部门+岗位能唯一定位时才自动更新；仍不能唯一定位的同名员工进入异常清单。

## Verification
- 执行 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import.sql`，MySQL 返回 `inserted_scheme_count=4`、`updated_or_inserted_employee_count=85`。
- 回滚 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_rollback.sql`。
- 预览报告：`docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview.md`。
- 写后方案核验：5 个 7 月方案均为 6 个项目，系统展示金额分别为 `社保445.80/公积金158`、`社保466.50/公积金158`、`社保672.50/公积金158`、`社保672.50/公积金320`、`社保672.50/公积金560`。
- 写后员工核验：按同一解析逻辑反查 85 个命中员工，实际 `scheme_id` 与预期一致，错配数 `0`。
- 未自动更新 4 条源数据：朱君明 `13972908908` 未在在职员工中找到；宁明友 `17671851617`、陈爱蓉 `15572492026`、瞿顺清 `13098848611` 与员工表当前手机号不一致。
- 6 月工资表重整执行 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary.sql`，MySQL 返回 `inserted_or_existing_scheme_count=3`、`touched_employee_count=83`。
- 6 月工资表重整回滚 SQL：`docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary_rollback.sql`。
- 6 月工资表重整报告：`docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview-2026-06-salary.md`。
- 写后方案核验：`466.50+144`、`445.80+144`、`672.50+144`、`672.50+320` 四个目标方案系统展示金额均与工资表一致。
- 写后员工核验：83 个触达员工 `scheme_id` 反查错配数 `0`；宁明友、陈爱蓉、瞿顺清均已按姓名唯一命中后更新到 6 月目标方案。
- 6 月未自动更新 3 条工资表数据：马国华、谢杰杰、程传祥仍存在同名在职候选且无法仅凭工资表唯一定位。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 首次查询 `hrsystem.companyinfo` 返回表不存在 | 1 | 改查 `hrsystem.tbcompanylist` 确认租户数据库 |
| 最大 ID 查询中 `scheme_id` 字段歧义 | 1 | 改为分别查询 `hrm_insurance_scheme`、`hrm_insurance_project`、`hrm_employee_social_security_info` |

# Task Plan: 李明明六月工会费和个税未生成排查

## Goal
- 查清系统核算 `2026-06` 薪资时，李明明工会费和个税工资项未生成的原因。
- 对照财务原始申报数据和整理后的系统导入文件，确认李明明六月应有工会费 `29.25`、个税 `2.4` 的数据来源。
- 在找到根因后补最小回归测试并修复核算逻辑或导入解析逻辑；本轮只以六月薪资数据为验证范围。

## Phases
1. `complete` 复核项目文档、现有计划记录和用户提供的原始/整理数据文件。
2. `complete` 定位李明明员工身份、六月薪资记录、工资项明细、个税/专项扣除累计数据和工会费计算入口。
3. `complete` 复现“工会费和个税未生成”的代码路径，形成根因假设并用最小证据验证。
4. `complete` 按 TDD 补失败测试，锁定李明明六月应生成工会费和个税的场景。
5. `complete` 实现最小修复，准备李明明历史数据修复脚本，运行定向验证，并更新需求/开发文档与排查记录。

## Current Assumptions
- “六月薪资”按 `2026-06` 理解，目标租户按用户文件和历史记录理解为 `hr_0003 / 湖北田野农谷生物科技有限公司`。
- “李明明”优先按当前未删除员工 `employee_id=1831601326890434563` 排查；如文件或数据库出现同名，必须用手机号/身份证/员工 ID 区分。
- 工会费期望值 `29.25` 和个税期望值 `2.4` 以财务原始申报/整理数据为权威验收值；若代码计算口径与导入数据口径冲突，优先找出具体字段差异再修复。
- 本轮不直接改历史薪资数据，除非用户明确要求执行数据修复；代码修复后需要重新核算六月薪资才会更新已生成记录。

## Verification
- 工会费重复 0 行修复：`SalaryMonthRecordServiceNewTest#filterNoFixedSalaryOptions_shouldNotMutateInputAndExcludeCodes` 已通过。
- 个税累计减除费用 RED：`SalaryComputeServiceNewTest#resolveCumulativeDeductions_shouldContinueImportedPriorDeductionBeforeRemarkFallback` 先失败于旧代码返回 `60000`。
- 个税累计减除费用 GREEN：同一测试通过；`SalaryComputeServiceNewTest` 全类 13 个测试通过。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，60 个测试。
- 个税/附加导入同名匹配回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=TaxImportEmployeeMatcherTest,TaxImportServiceDuplicateNameTest test` 通过，9 个测试。
- 用户目录 Excel/CSV 只读扫描未发现 `2.4/2.40/21894` 作为源字段；提供的 `2026-05` 导入基础与原始 `2026-06` 申报数据均已记录。
- `2026-07-17` 收尾时 MySQL 直连复核三次被 `ERROR 1040 (HY000): Too many connections` 阻断；未强行清理连接或停止现有 IDE 后端。
- `2026-07-17` 继续复核 `14.22` 与员工手算 `2.40` 差异：系统当前值按 `应发5850 + 个人社保672.50 + 公积金320 + 专项附加21500` 复算闭合；`2.40` 需要本月个人社保+公积金为 `1386.50`，即比系统当前 `992.50` 多 `394.00`。当前系统 6 月社保明细、最终导入文件、原始申报文件和桌面扫描均未找到 `1386.50/5731.00/2.40` 的直接来源；差异定位为本月专项扣除输入口径不一致，非个税公式错误。
- `2026-07-17` 按用户指定仅用 `202605`、`202606` 两张财务原始申报表复算：`202605` 表内结果为 `18.50`，`202606` 表内结果为 `16.81`；枚举两张表中的合理累计预扣字段组合，精确得到 `2.40` 的组合数为 `0`。仅靠这两张原始表不能推出李明明 6 月个税 `2.40`。
- `2026-07-20` 用户确认 `246.90/249.30` 为财务正确值后，已生成并导入能让系统现有公式算出 `2.40` 的全量导入副本：`李明明6月个税2.40_个税累计_导入2026-05.xls` 与 `李明明6月个税2.40_附加扣除累计_导入2026-06.xls`；读回及接口复核结果为累计收入 `75709.00`、累计减除费用 `35000.00`、累计专项扣除 `6741.50`、累计专项附加扣除 `25657.50`、累计应纳税额 `249.30`、本月个税 `2.40`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| MySQL 直连复核返回 `ERROR 1040 (HY000): Too many connections` | 1 | 暂停并发数据库直连，改用本地文件证据和定向测试继续验证 |
| MySQL 直连复核再次返回 `ERROR 1040 (HY000): Too many connections` | 2 | 不清理连接/不停止用户 IDE 后端；记录为运行态复核限制 |
| MySQL 直连复核第三次返回 `ERROR 1040 (HY000): Too many connections` | 3 | 停止重复同一失败动作，最终说明数据库重算复核受连接数限制 |
| 个税累计导入失败 `7001:第11行未找到员工：郑景 + 18670334666` | 1 | 源行对应已删除员工；重新生成导入文件时过滤 3 条 `matched_deleted` 员工后导入成功 |
| 单人重算时 `isSyncInsuranceData=false` 导致李明明本月社保/公积金被置 `0`，个税变为 `32.18` | 1 | 改用 `isSyncInsuranceData=true` 只重算李明明，恢复 `672.50+320` 并得到个税 `2.40` |

# Task Plan: 基本工资社保固定金额配置（历史记录，社保合计部分已由 2026-08-22 新规则覆盖）

## Goal
- 在基本工资金额设置中新增“大额医疗保险金额”和“长期护理保险金额”两项配置。
- 默认值分别为大额医疗 `15`、长期护理 `3`，旧数据或无配置时后端需兜底。
- 【已废弃，2026-08-22 新规则覆盖】社保方案管理中个人社保合计加上长期护理金额，公司社保合计加上大额医疗金额。
- 【已废弃，2026-08-22 新规则覆盖】社保管理生成次月报表时按同一逻辑写入员工月度社保记录。
- 当前社保方案/月度项目合计只统计已保存且启用的项目行，不再从基本工资金额设置自动累加大额医疗或长期护理金额。

## Phases
1. `complete` 定位基本工资金额设置、社保方案合计、社保次月报表生成和前端展示入口。
2. `complete` 补 RED 测试，锁定默认值、保存查询、社保方案合计和报表生成金额。
3. `complete` 实现后端实体/DTO/VO/默认值/合计计算/报表生成逻辑。
4. `complete` 如前端存在对应设置页和社保方案页，同步补 UI 字段、API 工具和前端测试。
5. `complete` 运行定向验证，更新需求/开发文档与本轮记录。

## Current Assumptions
- 【已废弃，2026-08-22 新规则覆盖】“大额医疗保险金额”按公司社保合计固定附加项处理，默认 `15`。
- 【已废弃，2026-08-22 新规则覆盖】“长期护理保险金额”按个人社保合计固定附加项处理，默认 `3`。
- 两项配置随最新一条 `hrm_salary_basic` 生效；历史记录缺字段时按默认值兜底，但当前不再参与社保方案/月度项目合计。
- 【历史方案，2026-08-22 社保合计新规则不再沿用】若需要新增 SQL 字段，优先为 `hrm_salary_basic` 增加 `large_medical_insurance_amount` 与 `long_term_care_insurance_amount`，并提供幂等迁移脚本。

## Verification
- RED 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmSalaryBasicServiceTest,HrmInsuranceSchemeMapperXmlTest test` 先失败于基本工资设置 DTO/VO/实体缺少 `largeMedicalInsuranceAmount`、`longTermCareInsuranceAmount`。
- RED 前端：`node tests/salary-basic-settings-page.test.mjs` 先失败于页面缺少“大额医疗保险金额”表单项。
- GREEN 后端定向：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmSalaryBasicServiceTest,HrmInsuranceSchemeMapperXmlTest test` 通过，7 个测试。
- 后端社保/基本工资回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmSalaryBasicServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest test` 通过，13 个测试。
- 前端定向：`node tests/salary-basic-settings-page.test.mjs`、`node tests/insurance-create-next-month.test.mjs`、`node tests/insurance-progress-utils.test.mjs`、`node tests/insurance-advanced-filter.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- SQL 迁移：已执行 `docs/sql/2026-07-17_hrm_salary_basic_insurance_amount_settings.sql` 到 dev MySQL；`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 两个新字段均存在，默认值为 `15.00/3.00`。
- 格式检查：后端和前端本轮相关文件 `git diff --check -- <相关文件>` 均无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 尾随空白扫描命令中 awk 表达式未加正确引号，zsh 返回 `parse error near '}'` | 1 | 改为 `awk '/[[:blank:]]$/ { ... }'` 后重跑，相关文件无尾随空白输出 |

# Task Plan: 李明明薪资全勤奖未计入排查

## Goal
- 查清李明明的核算薪资中全勤奖未加上的直接原因。
- 用员工档案、考勤汇总/加班夜班统计、薪资月记录和全勤奖计算条件形成可复核证据。
- 先排查根因；确认是代码口径问题后，补最小修复和回归测试。

## Phases
1. `complete` 复核项目需求/开发文档中全勤奖、应计出勤和李明明 2026-06 历史校准规则。
2. `complete` 定位李明明员工 ID、租户、薪资年月、薪资月记录和考勤汇总记录。
3. `complete` 追踪全勤奖计算条件：全勤金额来源、领导/普通分类、是否满勤、迟到/请假/缺勤扣减。
4. `complete` 对比数据库已核算工资项与代码条件，确认未加全勤奖的根因。
5. `complete` 补测试并修复：旧报表汇总缺失时，若应计出勤达到加班/夜班统计应出勤天数，则兜底发放全勤奖。

## Current Assumptions
- 用户反馈的“李明明”优先按 `hr_0003` 当前在职员工 `employee_id=1831601326890434563` 理解；如数据中存在其它同名员工，必须以员工 ID、手机号和删除状态区分。
- 目标月份优先按已有文档频繁提到的 `2026-06` 排查；如用户实际指其它薪资年月，后续按同一链路换月复核。
- 本轮不直接修改薪资数据；代码修复后需重新核算目标薪资记录，历史已生成工资项不会自动补写。

## Verification
- 本轮未修改薪资数据；已修改薪资全勤奖判定代码。
- 本机 API 复核：`hr_0003` 登录用户，最新薪资主记录为 `2077807285177995265`，`2026-06`，员工李明明 `employee_id=1831601326890434563`。
- 薪资列表复核：李明明 `actualWorkDay=23.00`、`needWorkDay=21.75`，工资项无 `40102`，应发工资为 `2130 + 3220 = 5350.00`。
- 计薪员工列表复核：李明明 `fullMoney=500.00`、`isFullAttendance=1`、`status=1`、`becomeTime=2025-07-01`，说明全勤资格和金额配置正常。
- 考勤汇总复核：`2026-06` 李明明实际出勤 `20.00`、应计出勤 `23.00`、加班 `8.00`。
- 加班/夜班统计复核：应出勤 `23` 天、实际出勤 `160.00` 小时、应计出勤 `184.00` 小时，备注为 `扣除：年假32.00小时`。
- 审批数据复核：李明明 `2026-06` 有年假 `4` 天、加班 `4+4` 小时、出差两段；未查到事假/病假审批。
- 打卡概况接口返回李明明月度字段 `attendDays=0/actualDays=0/isFullAttendance=null`，与薪资代码依赖的钉钉报表汇总缺失/未识别现象一致。
- RED：新增 `SalaryMonthRecordServiceNewTest` 断言加班/夜班统计应出勤天数映射、全勤应出勤天数优先取加班/夜班统计，以及报表汇总缺失但应计达到应出勤时允许兜底满勤；先失败于新增 helper 缺失。
- GREEN：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 通过，38 个测试。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，57 个测试；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，9 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 远端 MySQL 直连返回 `ERROR 1040 (HY000): Too many connections` | 1 | 停止并发直连，改用本机后端 API 读取同一租户数据 |
| 薪资列表接口初次请求 `sRecordId` 触发空指针 | 1 | 改用历史兼容字段名 `srecordId` 后查询成功 |

# Task Plan: 薪资行政/生产体系硬编码移除

## Goal
- 薪资核算中凡涉及行政体系/生产体系判断的逻辑，统一读取员工表 `hrm_employee.affiliation_system`。
- 删除按特定员工 ID 强制行政/生产体系、按部门名称推断薪资体系、导出链路重复维护员工名单等硬编码。
- 在不破坏现有薪资核算、半路转正、同步考勤、导出工资流程的前提下，收敛重复逻辑。

## Phases
1. `complete` 补 RED 测试，锁定体系来源只看 `affiliation_system`，旧员工 ID/部门名称逻辑不再生效。
2. `complete` 修改薪资员工查询 mapper，返回 `affiliationSystem` 并由该字段派生现有兼容字段。
3. `complete` 修改 `SalaryMonthRecordServiceNew`，移除员工 ID 特例和导出重复逻辑，统一 helper。
4. `complete` 删除不再使用的旧出勤体系方法/重复代码，并同步处理仍被注入的 `SalaryMonthRecordService_Bak`。
5. `complete` 运行定向测试和格式检查，更新需求/开发文档与排查记录。

## Current Assumptions
- `hrm_employee.affiliation_system=1` 表示行政体系，对应薪资内部 `deptType=0`。
- `hrm_employee.affiliation_system=2` 表示生产体系，对应薪资内部 `deptType=1`。
- 兼容字段 `isProduceDept` 可以暂时保留给现有 VO/前端，但必须由 `affiliation_system` 派生，不再由部门名称或员工 ID 派生。
- 员工表未维护所属体系时，为避免空值导致核算中断，薪资核算默认按行政体系处理；后续若业务需要强校验，可再加前置校验。

## Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest test` 先失败于缺少 `resolveSalaryDeptType(...)` / `resolveSalaryDeptTypeFromAffiliation(...)`。
- GREEN 定向：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmProduceAttendanceServiceImplTest,HrmEmployeeMapperSqlTest test` 通过，50 个测试。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest,HrmProduceAttendanceServiceImplTest test` 通过，67 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 员工管理唯一性与表单字段调整

## Goal
- 员工管理列表导入、新增、修改时，对工号、手机号、身份证号做唯一校验。
- 新增/编辑页面删除“是否有全勤”和“是否加入钉钉”字段。
- 校验失败返回可读中文提示，避免同一唯一标识写入多个未删除员工。

## Phases
1. `complete` 定位员工管理后端导入、新增、修改接口和前端新增/编辑页面。
2. `complete` 补 RED 测试，覆盖导入/新增/编辑唯一性校验，以及前端表单不再渲染全勤/钉钉字段。
3. `complete` 实现后端唯一性校验，新增时查重，编辑时排除当前员工。
4. `complete` 实现前端新增/编辑字段删除，并确认接口提交不再依赖这两个字段。
5. `complete` 运行定向验证，更新需求/开发文档和本轮记录。

## Current Assumptions
- 唯一性范围按未删除员工理解；历史已删除员工不阻止复用工号、手机号或身份证号。
- 空值不参与唯一性校验；只有填写了对应字段才校验重复。
- 编辑员工时允许保留自身原工号、手机号、身份证号。

## Verification
- RED 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest test` 先失败于缺少统一员工唯一性校验方法。
- RED 前端：`node tests/employee-form-field-removal.test.mjs` 先失败于新建/编辑页面仍包含“是否有全勤 / 是否加入钉钉”字段。
- GREEN 定向：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest test` 通过，13 个测试；`node tests/employee-form-field-removal.test.mjs` 通过。
- 审查反馈 RED/GREEN：补充 `updateCommunication_shouldRejectDuplicateMobileBeforeSaving` 和“导入唯一性失败不应保存前序员工”断言；修复后 `HrmEmployeeServiceImplUniqueValidationTest` 3 个测试、`HrmEmployeeServiceImplImportEmployeeTest` 10 个测试均通过。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeControllerTest,HrmEmployeeServiceImplEducationExperienceTest,HrmEmployeeServiceImplQueryPageListTest test` 通过，16 个测试。
- 前端回归：`node tests/employee-form-field-removal.test.mjs`、`node tests/employee-edit-save-regression.test.mjs`、`node tests/employee-import-file.test.mjs`、`node tests/employee-import-result-ui.test.mjs`、`node tests/employee-template-download-ui.test.mjs`、`node tests/employee-template-download-api.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 薪资核算硬编码排查

## Goal
- 找出“核算工资/薪资核算”链路中所有明显硬编码的业务规则、常量、编码、表字段映射和特殊分支。
- 区分已由文档明确认可的固定口径、仍可接受的兜底默认值，以及需要配置化/治理的高风险硬编码。
- 本轮只做只读排查和结论整理，不修改业务代码。

## Phases
1. `complete` 定位薪资核算入口、核心 service、mapper、测试和相邻考勤/社保/个税模块。
2. `complete` 扫描薪资核算相关 Java/XML 中的字面量、固定编码、公司/租户特殊分支和中文规则。
3. `complete` 逐项复核调用上下文，判断是否是真硬编码、配置默认值还是枚举/协议编码。
4. `complete` 汇总风险等级、文件位置、代码片段含义和建议处理方式。
5. `complete` 更新 `findings.md`、`progress.md`，必要时新增审计报告。

## Current Assumptions
- “核算工资”按后端薪资核算入口 `/hrmSalaryMonthRecord/computeSalaryData` 及其直接调用链理解。
- 相邻模块纳入范围：工资项计算、员工月薪记录、考勤汇总同步数据、社保同步数据、个税/附加累计读取、工资项编码 mapper。
- 硬编码不等同于所有字面量；DTO 字段名、稳定枚举值、数据库协议字段仅在影响业务规则可配置性时列为问题。

## Verification
- 本轮为只读排查，未修改业务代码，未运行测试。
- 已用 `rg` / `awk` 复核薪资核算入口、主计算服务、员工月薪记录服务、薪资 mapper、薪资档案 mapper、考勤汇总同步和加班/夜班统计相关硬编码行号。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 初次按 `src/main/java/com/tianye/hrsystem/modules/salary/task/SalaryComputeTask.java` 查找自动薪资任务，路径不存在 | 1 | 改用 `rg --files` 全局定位，实际文件为 `src/main/java/com/tianye/hrsystem/autoTask/SalaryComputeTask.java` |

# Task Plan: 薪资核算应计出勤天数来源调整

## Goal
- 薪资核算中所有原来使用“实际出勤天数”的薪资项或扣款逻辑，改为使用“应计出勤天数”。
- 应计出勤天数优先从考勤汇总涉及的表读取。
- 当考勤汇总找不到该员工目标年月数据或应计出勤天数字段为空时，再从“加班/夜班统计”涉及的表读取兜底值。
- 保持已有薪资核算入口、核算范围、进度提示、人工维护字段和其它薪资项计算口径不变。

## Phases
1. `complete` 定位薪资核算中读取实际出勤天数的所有路径，以及考勤汇总和加班/夜班统计表的现有 mapper/service。
2. `complete` 补 RED 测试，锁定“优先考勤汇总应计出勤，缺失时兜底加班/夜班统计应计出勤”的薪资核算行为。
3. `complete` 实现最小修复，替换实际出勤读取点并封装应计出勤来源选择。
4. `complete` 运行定向测试和必要格式检查。
5. `complete` 更新 `docs/requirements.md`、`docs/development.md`、`findings.md`、`progress.md`。

## Current Assumptions
- “考勤汇总中应计出勤天数”对应 `hrm_produce_attendance.probation_attendance` 及前端/VO 中的 `accruedAttendance`。
- “实际出勤天数”对应当前薪资核算中从考勤汇总读取的 `positive_attendance` / `actualAttendance` 或考勤 map 编码 `2=实际出勤天数`。
- “找不到”包括目标年月无考勤汇总行，或该员工该字段为空；若为 `0` 则按明确业务值处理，不自动兜底。
- 加班/夜班统计兜底优先使用已落库明细的 `accrued_attendance_hours / 8`。

## Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 编译失败，缺少 `resolveSalaryAttendanceDays(...)` 与 `buildAccruedAttendanceDaysByEmployee(...)`。
- GREEN：同一命令通过，32 个测试。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，50 个测试。
- 上游考勤汇总同步：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，8 个测试。
- 格式检查：`git diff --check -- <本轮相关文件>` 无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 考勤汇总同步出勤天数来源修正

## Goal
- 修改“考勤汇总”同步逻辑：按同步时选择的月份，从已落库的“加班/夜班统计”结果中读取实际出勤小时和应计出勤小时。
- 将小时换算为天数后，新增或更新到考勤汇总表的实际出勤天数与应计出勤天数。
- 保持其它字段和既有业务逻辑不变，尤其不覆盖借款、补贴、扣款、备注等人工维护字段。

## Phases
1. `complete` 定位考勤汇总同步入口、统计结果查询来源、汇总 upsert 字段映射和既有测试。
2. `complete` 补 RED 测试，锁定同步时必须按选择月份读取数据库统计结果中的实际/应计出勤小时并折算天数。
3. `complete` 实现最小修复，保持其它同步字段和人工字段不变。
4. `complete` 运行定向测试和必要格式检查。
5. `complete` 更新 `docs/requirements.md`、`docs/development.md`、`findings.md`、`progress.md`。

## Current Assumptions
- 用户说的“同步考勤时选择的月份”指考勤汇总页 `syncFromOvertimeNightStatistics` 传入的 `YYYY-MM` 月份。
- “数据库查询出来后 insert/update”指优先读取 `hrm_overtime_night_statistics_detail` 已落库统计明细聚合结果，不在考勤汇总同步中重新计算实际/应计出勤。
- “要获取天数”按现有口径理解为小时除以 `8` 后写入考勤汇总天数字段。

## Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest#syncFromOvertimeNightStatistics_shouldUsePersistedStatisticsDetailsForAttendanceDays test` 先失败，实际写入仍为原 VO 的 `positiveAttendance=23.00/probationAttendance=25.00`。
- GREEN：同一命令通过，1 个测试。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，8 个测试。
- 格式检查：本轮相关跟踪文件 `git diff --check -- ...` 无输出；未跟踪计划文件 `task_plan.md/findings.md/progress.md` 尾随空白扫描无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 考勤汇总同步漏员工排查

## Goal
- 排查“考勤汇总”中执行同步考勤时部分员工没有同步到汇总表的问题。
- 以用户反馈的员工“潘红琼”为样例，确认她在员工档案、钉钉考勤用户映射、加班/夜班统计、考勤汇总表中的数据状态。
- 找到根因后补最小回归测试，再修复同步逻辑，避免同类员工继续漏同步。

## Phases
1. `complete` 定位考勤汇总同步入口、入参和上下游表。
2. `complete` 查询潘红琼相关员工主数据、`tbattendanceuser` 映射、统计明细与 `hrm_produce_attendance` 汇总记录。
3. `complete` 基于证据形成根因假设，并与同类已同步员工对比。
4. `complete` 补 RED 测试锁定漏同步场景。
5. `complete` 实现修复，运行定向测试和必要的数据核对。
6. `complete` 更新 `docs/requirements.md`、`docs/development.md` 与本次排查记录。

## Current Assumptions
- “同步考勤”指考勤汇总页从加班/夜班统计或同步考勤链路写入 `hrm_produce_attendance` 的操作。
- 当前先按“潘红琼”定位样例；如果存在同名或跨租户员工，后续以员工 ID、手机号、钉钉 `userId` 为准。
- 不先改代码；必须先确认漏同步发生在员工候选集、映射表、统计结果还是汇总 upsert 环节。

## Verification
- 只读数据核对：`hr_0003 / 2026-06` 活跃员工 `113` 人、统计员工 `113` 人、考勤汇总员工 `113` 人；潘红琼统计与汇总记录均存在，但汇总 `department=1`，在 `department=2` 筛选下不可见。
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest#syncFromOvertimeNightStatistics_shouldRefreshExistingDepartmentFromEmployeeAffiliationSystem test` 失败于更新对象仍为 `department=1`。
- GREEN：同一命令通过，1 个测试。
- 回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，7 个测试。
- 二次复核：远端 API 已能按 `2026-06 + department=2 + 潘红琼` 查到 1 条记录，但 `actualAttendance/accruedAttendance/overtimePay` 返回空；数据库同一行分别为 `21.00/25.00/0.00`。
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceMapperXmlTest test` 失败于 `work_over_time` 未显式映射为 `workOverTime`。
- GREEN：同一 mapper 测试通过；`HrmProduceAttendanceServiceImplTest` 继续通过 7 个测试。
- 打包：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -DskipTests package` 通过，`target/hrsystem-0.0.1-SNAPSHOT.jar` 内 mapper 已包含显式别名。
- 本地复核：`127.0.0.1:8081/api` 按 `2026-06 + department=2 + 潘红琼` 返回 `actualAttendance=21.00/accruedAttendance=25.00`；`2026-07` 同步前为 `0` 条，同步 `{"month":"2026-07"}` 返回 `data=113` 后，潘红琼返回 `actualAttendance=27.00/accruedAttendance=27.00`。
- 用户要求重测 6 月后，已仅删除 `hr_0003.hrm_produce_attendance` 的 `2026-07` 汇总数据：删除前 `113` 行、删除 `113` 行、删除后 `0` 行；6 月汇总仍 `113` 行，潘红琼接口复核仍为 `21.00/25.00`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 新实现初次用 `int == Integer` 判断 `affiliationSystem`，旧测试中所属体系为空触发空指针 | 1 | 改为 `Integer.valueOf(...).equals(...)`，空体系继续回退部门名称推断 |
| 初次并行跑两个 Maven 测试命令同时写 `target/`，存在构建目录竞争风险 | 1 | 两个命令最终均通过；后续用重新打包和包内 mapper 核对补充验证，避免依赖并行结果 |

# Task Plan: 社保报表生成真实进度提示

## Goal
- 社保管理“生成社保报表”需要展示真实生成进度条提示框。
- 生成过程中前端按后端真实进度轮询展示状态、阶段、百分比与文案。
- 生成完成后自动关闭提示框，展示完成提示，并刷新社保管理界面。
- 失败时展示可读中文错误，并停止轮询。

## Phases
1. `complete` 定位后端社保报表生成入口、前端社保管理页面和既有进度轮询模式。
2. `complete` 补 RED 测试覆盖后端进度对象/接口与前端进度弹框/完成刷新。
3. `complete` 实现后端社保报表生成进度状态缓存和查询能力。
4. `complete` 实现前端进度提示框、轮询、完成关闭提示与列表刷新。
5. `complete` 运行定向测试、构建校验并更新项目文档。

## Current Assumptions
- “真实生成进度”按后端社保报表生成实际处理步骤或处理记录数量计算，不使用纯前端假进度。
- 保持既有生成接口业务语义；如需新增接口，优先新增独立进度查询接口，避免破坏已有调用。
- 完成刷新指刷新社保管理当前列表/筛选结果，而不是整页重载。

## Verification
- RED 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordControllerTest test` 先失败于缺少 `InsuranceComputeProgressVO`。
- RED 前端：`node tests/insurance-progress-utils.test.mjs` 先失败于缺少 `insurance-progress-utils.js`；`node tests/insurance-create-next-month.test.mjs` 先失败于缺少 `queryComputeInsuranceProgress` API。
- GREEN 后端：同一 Maven 命令通过，5 个测试。
- GREEN 前端：`node tests/insurance-progress-utils.test.mjs`、`node tests/insurance-create-next-month.test.mjs`、`node tests/insurance-advanced-filter.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 本轮相关文件空白检查：后端相关文件 `git diff --check -- <本轮相关文件>` 通过；前端相关文件 `git -C /Users/jiangyongming/Project/hr/hr_web diff --check -- <本轮相关文件>` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `rg: tests: No such file or directory` | 1 | 后端项目测试目录为 `src/test`，后续搜索改用实际目录。 |

# Task Plan: 薪资管理开始核算穿梭框改造

## Goal
- 薪资管理页删除“核算”和“薪资”按钮，只保留原单人核算入口并改名为“开始核算”。
- 点击“开始核算”后展示左右穿梭框，支持按人员和按部门核算。
- 按人员时：右侧未选员工则执行原“核算薪资”全员功能；右侧有员工则只核算选中员工，且一次不超过 50 人。
- 按部门时：右侧未选部门则执行原“核算薪资”全员功能；右侧有部门则核算这些部门下员工，最终员工数不超过 50 人。
- 薪资核算涉及同名员工时必须按“姓名 + 手机号”作为前端唯一键展示和处理，避免同名员工串人。

## Phases
1. `complete` 复核后端/前端文档、现有薪资核算入口、员工和部门数据接口。
2. `complete` 补 RED 测试覆盖前端按钮、穿梭框参数组装、50 人限制和同名唯一键。
3. `complete` 补后端 RED 测试，覆盖批量员工 ID 入参、超过 50 人拒绝、选中部门展开员工。
4. `complete` 实现后端核算范围扩展并保持空选择走原全员核算。
5. `complete` 实现前端“开始核算”弹窗与按人员/按部门穿梭框。
6. `complete` 运行定向测试、构建校验并更新项目文档。

## Follow-up 2026-07-17: 按部门树结构展示
1. `complete` 记录补充需求：按部门的选择区必须用树结构展示。
2. `complete` 补 RED 测试，要求部门模式渲染 `el-tree`、复选框和部门树面板。
3. `complete` 将部门模式从扁平 `el-transfer` 改为左侧部门树勾选、右侧已选部门树展示。
4. `complete` 运行前端定向测试和构建，并更新文档。

## Current Assumptions
- 用户原文“热锅按部门”按“如果按部门”理解。
- “删除核算和薪资按钮”指薪资管理页顶部原有批量核算/发放类按钮，不删除列表数据或菜单。
- 原“核算薪资”全员功能仍需要保留，只是入口并入“开始核算”弹窗：未选择员工/部门时执行全员核算。
- 右侧部门最终展开后的员工数量上限是 50 人，而不是部门数量上限。
- 按部门选择的“右侧有部门选项”按已勾选的部门 ID 判断；员工数量仍按所选部门及其下级部门展开后计算。

## Verification
- RED 前端：`node tests/salary-compute-scope-utils.test.mjs` 先失败于缺少 `salary-compute-scope-utils.js`。
- RED 前端页面：`node tests/salary-start-compute-dialog.test.mjs` 先失败于薪资页未暴露“开始核算”按钮。
- RED 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 先编译失败于缺少 `normalizeComputeEmployeeIds(...)` / `buildComputeScopeKey(...)`。
- GREEN 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest test` 通过，28 个测试。
- GREEN 前端：`node tests/salary-compute-scope-utils.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/compute-progress-utils.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端相关文件 `git diff --check` 通过；前端相关文件 `git -C /Users/jiangyongming/Project diff --check -- ...` 通过。
- 2026-07-17 RED：`node tests/salary-start-compute-dialog.test.mjs` 失败于缺少 `compute-tree-transfer`；`node tests/salary-compute-scope-utils.test.mjs` 失败于缺少 `normalizeSalaryComputeDeptTree` 导出。
- 2026-07-17 GREEN：`node tests/salary-start-compute-dialog.test.mjs`、`node tests/salary-compute-scope-utils.test.mjs`、`node tests/compute-progress-utils.test.mjs`、`npm run build` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `find .. -maxdepth 2 -name ... vite.config.*` 在 zsh 下因无匹配 glob 报 `no matches found` | 1 | 后续改用 `rg --files` 查找前端配置和薪资相关文件，避免 shell glob 展开。 |
| 后端新增 `queryHasSalaryArchivesEmployeeList(HrmSalaryMonthRecord, List<Long>)` 后，`queryHasSalaryArchivesEmployeeList(salaryMonthRecord, null)` 编译歧义 | 1 | 将调用改为 `Collections.emptyList()`，明确表示全员范围。 |

# Task Plan: 薪资核算个税备注方案实施

## Goal
- 将已确认的个税备注方案落到薪资核算代码：
  - 无税务局备注：累计减除费用按 `5000 * 工资计算月份`；
  - 有税务局备注：累计减除费用固定 `60000`；
  - 本月个税按累计应纳税额减累计已缴税额计算，结果小于 0 时工资表写 0；
  - 核算完成后同步写入当月 `hrm_personal_income_tax`，供下月继续累计。

## Phases
1. `complete` 复核项目文档和既有个税方案。
2. `complete` 定位现有薪资个税计算、半路转正复算和个税累计保存链路。
3. `complete` 补 RED 测试覆盖无备注、有备注、低工资抵减、1月/12月累计边界。
4. `complete` 实现新个税累计减除费用口径，并确认既有核算链路自动保存当月个税累计。
5. `complete` 运行定向测试并更新文档。

## Current Assumptions
- `hrm_employee.is_remark=2` 视为税务局系统已生成全年 `60000` 减除费用备注。
- 不新增新表或字段；现有个税累计、附加累计、年度附加固定扣除和员工备注字段足够支持。
- 工资表不生成负个税；多缴部分通过后续月份抵减或年度汇算处理。

## Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest test` 先失败于新增的 `resolveCumulativeDeductions(...)` 方法不存在。
- GREEN：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test` 通过，37 个测试。
- 相关文件格式检查：`git diff --check -- <本轮相关文件>` 通过。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 旧半路转正测试仍按“上月累计减除费用 + 5000”和“12月重置累计”断言 | 1 | 已按新方案更新为 `5000 * month` 与 1 月重置、12 月继续累计。 |

# Task Plan: 个税/附加导入重名员工匹配修复

## Goal
- 解决个税累计、附加累计、年度附加扣除导入时同名员工匹配错误问题。
- 使用用户提供的原始申报数据和已整理导入模板数据核对重名员工，确保后续导入不再按姓名误写。

## Phases
1. `complete` 复核项目文档和既有个税备注方案。
2. `complete` 查询实库重复记录，确认重复是否为同一 `employee_id` 还是同名不同员工。
3. `complete` 补 RED 测试，锁定导入员工匹配必须优先使用姓名+手机号，缺手机号遇重名需失败。
4. `complete` 修改个税累计、附加累计、年度附加扣除导入匹配逻辑。
5. `complete` 用用户提供的原始/整理数据核对王芳等同名员工，生成修复建议或修复 SQL。
6. `complete` 运行定向测试并更新文档。

## Current Assumptions
- 现有前端模板已经包含手机号列；导入服务应使用姓名+手机号匹配员工。
- 对旧模板或缺少手机号的数据，如果员工姓名唯一可兼容导入；如果同名则必须拒绝并提示补手机号。
- 不直接执行破坏性数据库修复；先生成可核对 SQL 或修复报告。

## Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=TaxImportEmployeeMatcherTest,TaxImportServiceDuplicateNameTest test` 先失败于缺少 `TaxImportEmployeeMatcher`。
- GREEN：同一命令通过，9 个测试。
- 修复 SQL 验证：`mysql -h 153.0.237.98 -P 3306 -uroot -p... --default-character-set=utf8mb4 < docs/sql/2026-07-16_hr_0003_tax_import_duplicate_name_repair.sql` 返回成功；脚本默认 `ROLLBACK`。
- 用户授权后正式修复：
  - `docs/sql/2026-07-16_hr_0003_tax_import_duplicate_name_repair.sql` 通过临时 `COMMIT` 执行；
  - `docs/sql/2026-07-16_hr_0003_employee_additional_duplicate_cleanup.sql` 通过临时 `COMMIT` 执行；
  - 最终复核三类唯一键重复计数均为 `0`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 查询 `hrm_employee.id_card` 字段失败，实际字段不存在 | 1 | 已查表结构，身份证字段为 `id_number`，并用该字段重新查询重复记录。 |

# Task Plan: 薪资核算个税新场景方案

## Goal
- 基于现有薪资生成代码，梳理税务局备注、无备注和内部特殊月份工资低于起征点等场景下的个税计算方案。
- 本轮只提交计算方案和改造建议，不修改业务代码。

## Phases
1. `complete` 复核项目需求/开发文档与既有个税数据整理记录。
2. `complete` 定位薪资生成、个税累计、附加累计和年度附加扣除相关代码。
3. `complete` 还原现有个税计算链路、字段来源和持久化更新逻辑。
4. `complete` 设计新个税公式、累计税处理和特殊月份补税/退税规则。
5. `complete` 输出落地改造方案、数据项、测试场景和待确认问题。

## Current Assumptions
- 用户提到的“税务局系统内员工生成备注”需要在系统中新增或映射为员工年度个税扣除口径标识；现有文档未看到该字段定义。
- “内部特殊情况”不能按单月工资是否低于 5000 直接独立判断，应纳入全年累计预扣预缴公式，通过本期应补/退税额自然体现。
- 本轮不改代码；若需要落库字段或接口调整，只在方案中列出。

## Verification
- 已核对 `HrmSalaryMonthRecordController#computeSalaryData`、`SalaryMonthRecordServiceNew#computeSalaryData/doComputeSalaryData/computeEmployeeSalary/computeSalaryFromMemory`、`SalaryComputeServiceNew#computeSalary/calculateTaxAccumulation/calculateMonthTax`、`TaxCalculator`、`HrmPersonalIncomeTaxMapper.xml`、`HrmAdditional/HrmEmployeeAdditional`。
- 已新增方案文档 `docs/plans/2026-07-16-salary-tax-remark-solution.md`，并更新 `docs/requirements.md`、`docs/development.md` 中的方案摘要。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 附加累计列表按年-月筛选

## Goal
- 配合前端“附加累计”选择年-月筛选改造，后端列表查询真正按 `year/month` 过滤。
- 保持现有导入和工资计算接口契约不变。

## Phases
1. `complete` 定位附加累计 BO、Controller、Service 和 Mapper 查询条件。
2. `complete` 补 RED 测试，锁定 Mapper XML 必须包含 `data.year/data.month` 条件。
3. `complete` 实现 Query BO 字段和 Mapper SQL 条件。
4. `complete` 运行定向测试。
5. `complete` 更新需求/开发文档和进度记录。

## Current Assumptions
- 前端会将 `YYYY-MM` 拆分后提交 `year/month`；后端 BO 使用 `Integer` 承接，兼容 JSON 数字和数字字符串。
- 导入接口已接收 `year/month`，本轮不改导入服务。

## Verification
- RED：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmAdditionalMapperXmlTest test` 失败于 BO 和 mapper 缺少年月筛选。
- GREEN：同一 Maven 命令通过，2 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 按导入模板生成个税/附加 Excel

## Goal
- 基于上一轮个税计算基础支持数据整理结果，按用户指定的三个导入模板生成最终 Excel：
  - 个税累计表模板：`/Users/jiangyongming/Desktop/导入模版/个税累计.xls`；
  - 附加累计模板：`/Users/jiangyongming/Desktop/导入模版/附加扣除累计.xls`；
  - 年度附加扣除模板：`/Users/jiangyongming/Desktop/导入模版/专项扣除累加表.xlsx`。
- 个税累计和附加累计均填入截至 `2026-05` 的数据。
- 年度附加扣除填入每年每项专项附加扣除的固定累加值，并使用姓名 + 手机辅助唯一识别员工。

## Phases
1. `complete` 复核项目文档、上一轮整理产物和三个模板结构。
2. `complete` 补齐模板所需员工岗位、工号、部门、手机字段。
3. `complete` 按模板复制并生成三份最终 Excel。
4. `complete` 读取生成文件做行数、表头和关键字段校验。
5. `complete` 更新发现、进度和项目文档。

## Current Assumptions
- “截止到5月份的数据”按 `2026-05` 解释，用于后续 2026-06 薪资/个税计算的上月累计基础。
- 前两个 `.xls` 原模板作为格式来源，不直接覆盖；最终文件写入新的输出目录。
- 年度附加扣除保留 `2025` 与 `2026` 两个年份的固定值，因为模板包含年份列，且上一轮已经能从相邻月份差额推导两个年度。

## Verification
- 输出目录：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716/final_import_excels`。
- `个税累计.xls`：100 行，数据月份 `2026-05`，手机列 0 空值，姓名+手机 0 重复。
- `附加扣除累计.xls`：41 行，数据月份 `2026-05`，手机列 0 空值，姓名+手机 0 重复。
- `专项扣除累加表.xlsx`：64 行，年份 `2025/2026`，手机列 0 空值，姓名+手机+年份 0 重复。
- 已读取生成文件复核模板表头和样例数据。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 本机只有 `xlrd/openpyxl`，缺少写 `.xls` 所需 `xlwt/xlutils` | 1 | 已安装最小用户级依赖 `xlwt==1.3.0`、`xlutils==2.0.0`，用于复制并写入 `.xls` 模板。 |

# Task Plan: 个税计算基础支持数据整理

## Goal
- 基于桌面“湖北田野农谷生物科技有限公司_综合所得申报”资料中 `2025-11`、`2025-12`、`2026-05`、`2026-06` 的申报表，整理系统个税计算所需基础支持数据。
- 覆盖三个系统功能的数据口径：
  - 个税累计功能；
  - 附加累计功能；
  - 年度附加扣除功能。
- 产出可导入、可核对的数据文件或 SQL，并给出字段来源和校验结果。

## Phases
1. `complete` 读取项目需求/开发文档，定位实际申报资料目录。
2. `complete` 清点申报资料文件、工作表和表头，识别四个月数据结构。
3. `complete` 定位后端个税累计、附加累计、年度附加扣除相关实体、接口、表结构和导入口径。
4. `complete` 建立申报表字段到系统字段的映射规则，明确缺失、冲突和需要人工确认的数据。
5. `complete` 生成整理后的导入数据文件或 SQL，并输出核对摘要。
6. `complete` 更新发现、进度和必要的项目文档。

## Current Assumptions
- 用户给出的路径末尾“文件夹”是描述词；实际目录为 `/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报`。
- 目标公司是湖北田野农谷生物科技有限公司，结合既有文档大概率对应租户/公司 `0003`，但需要从项目配置或数据中复核。
- 先不改业务代码，优先整理基础数据；若现有系统缺少导入入口或字段口径不完整，再单独确认是否需要补功能。

## Verification
- 生成目录：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716`。
- 输出 SQL：`hr_0003_个税计算基础支持数据.sql`，637 行。
- 输出明细：个税累计 396 条、附加累计 130 条、年度附加扣除配置 64 条；未匹配申报员工 342 条。
- 系统导入列位参考 Excel：4 个个税累计、4 个附加累计、2 个年度附加扣除文件。
- SQL 验证：将生成 SQL 中 `COMMIT` 替换为 `ROLLBACK` 后提交 MySQL 事务执行，返回码 `0`，未落库。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 按用户原文 `/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报文件夹` 执行 `ls` 返回目录不存在 | 1 | 在桌面数据目录下搜索 `*综合所得申报*`，定位到实际目录 `/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报`。 |
| Python 通过 MySQL CLI 读取员工 TSV 时默认按 UTF-8 解码，遇到 GBK 中文输出报 `UnicodeDecodeError` | 1 | 改为以二进制读取 stdout/stderr，再按 `gbk` 解码。 |

# Task Plan: 基本工资金额设置新增全勤和生产月休配置

## Goal
- 在“基本工资金额设置”中新增并持久化三项配置：
  - 普通员工全勤金额，默认 `100`；
  - 领导全勤金额，默认 `500`；
  - 生产体系员工月度休息天数，默认 `4`。
- 查询配置时旧数据也应返回上述默认值，保存后需写入租户库。
- 生产体系应出勤统计中固定月休天数优先读取该配置，缺失时仍兜底 `4`，保持现有口径兼容。

## Phases
1. `complete` 定位后端基本工资金额设置实体、接口、服务、Mapper/Repository、测试，以及前端设置页面。
2. `complete` 补后端 RED 测试，覆盖默认值、保存入库、最新配置选择和生产体系月休配置读取。
3. `complete` 实现后端字段、默认值、保存/查询、SQL 脚本和统计服务读取配置。
4. `complete` 补前端测试并更新“基本工资金额设置”表单，保证三项可编辑保存。
5. `complete` 执行新 SQL 脚本、运行后端/前端定向验证，更新需求与开发文档。

## Current Assumptions
- 用户原话写“添加两条保存项”，但随后列出三项并说明“这三项”，本轮按三项配置处理。
- `hrm_salary_basic` 是基本工资金额设置的现有持久化表，优先在该表上加字段，避免新增独立配置表和额外接口。
- 生产体系员工月度休息天数只替代现有硬编码 `4`，法定休息日额外扣减规则保持不变。

## Verification
- RED 前端：`node tests/salary-basic-settings-page.test.mjs` 失败于页面缺少“普通员工全勤金额”等三项表单字段。
- 初次后端验证：`mvn -Dtest=HrmSalaryBasicServiceTest test` 被本机全局 Maven settings 阻断，错误为 `com.aliyun:tea:jar:[1.1.14, 2.0.0)` 无可用版本。
- 隔离 Maven settings 后后端服务测试：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmSalaryBasicServiceTest test` 通过，4 个测试。
- 后端组合：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml '-Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldUseConfiguredProductionMonthlyRestDaysForProductionEmployee' test` 通过，6 个测试。
- 后端统计完整回归：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，53 个测试。
- 前端：`node tests/salary-basic-settings-page.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- SQL：已执行 `docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql`；`hr_0001` 至 `hr_0005` 均存在三列，默认值为 `100.00/500.00/4`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 本机默认 Maven 全局 settings 解析 `com.aliyun:tea:[1.1.14,2.0.0)` 失败 | 1 | 使用 `/tmp/codex-empty-maven-settings.xml` 同时作为 user/global settings 隔离全局配置后测试通过。 |

# Task Plan: 考勤汇总行政体系加班同步口径修正

## Goal
- 修复张明 2026-06 在“考勤汇总”中被同步出 `47.72` 小时加班的问题。
- 考勤汇总同步到 `hrm_produce_attendance.work_over_time` 的加班口径需与“加班/夜班统计”实际出勤和应计出勤模块使用的加班口径一致。
- 行政体系员工不应把日级自动/月度自动加班同步为考勤汇总加班；只同步本地钉钉审批加班。加班工资按修正后的同步加班小时重新计算。

## Phases
1. `complete` 定位考勤汇总同步服务、加班/夜班统计服务中实际/应计出勤加班来源，以及现有测试结构。
2. `complete` 补 RED 测试，复现行政体系员工统计展示加班 `47.72` 但无审批加班时，考勤汇总同步应写入 `0` 加班和 `0` 加班工资。
3. `complete` 实现最小修复，复用或对齐加班/夜班统计实际/应计出勤的可计入加班口径。
4. `complete` 运行后端定向回归，并按需要补充工资读取相关回归。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- `QueryOvertimeNightStatisticsPageVO.overtimeHours` 是页面展示用月度加班汇总，可能包含日级自动加班。
- 行政体系和生产体系的实际/应计出勤公式使用“可计入实际出勤的加班”，即本地钉钉审批加班；考勤汇总同步应使用同一口径写入 `work_over_time`。
- 生产体系若已有相同规则，也应保持一致；本次先用张明行政体系场景锁定回归。

## Verification
- RED：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldAddOvertimeApprovalToActualAttendance test` 编译失败，`QueryOvertimeNightStatisticsPageVO` 缺少 `get/setAttendanceOvertimeHours`。
- GREEN：同一命令通过，8 个测试，0 失败。
- 回归：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，58 个测试，0 失败。
- 工资读取回归：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，7 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 本轮首次将 `session-catchup.py` 用 `sh` 执行，shell 把 Python 内容当脚本解析失败 | 1 | 已改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan`，脚本正常执行且无输出。 |

# Task Plan: 上传考勤同步加班夜班出勤数据

## Goal
- 将“上传考勤”菜单和页面文案改为“考勤汇总”，隐藏原“导入考勤”入口。
- 在“考勤汇总”页新增同步按钮，选择年月后从“加班/夜班统计”明细读取实际出勤和应计出勤，新增或更新上传考勤列表对应数据表。
- 上传考勤列表支持单元格编辑后显式点击保存入库。
- 上传考勤列表删除 `空班/次`、`中班/次`、`当月休假/天` 三列，并在 `加班/小时` 后新增 `加班工资` 列。
- 同步时自动计算金额：`加班工资 = 加班小时 * 基本工资金额设置.每小时加班费`，`夜班补贴 = 夜班次数 * 基本工资金额设置.夜班补贴额度`。

## Phases
1. `complete` 定位上传考勤后端接口、数据表/实体/Mapper、前端页面和现有测试。
2. `complete` 补 RED 测试：年月同步新增/更新上传考勤、单元格保存接口、列表列结构变更。
3. `complete` 实现后端同步接口、单元格保存接口、字段映射和必要 SQL/实体字段。
4. `complete` 实现前端同步按钮、年月弹窗、表格列调整、单元格编辑保存交互。
5. `complete` 运行后端/前端定向验证和构建。
6. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- 同步数据来源为已落库的 `hrm_overtime_night_statistics_detail` 月度明细，不重新计算加班/夜班统计。
- 年月选择只需要月份粒度，接口入参按 `YYYY-MM` 传输。
- “加班工资”和“夜班补贴”同步时按最近一条 `hrm_salary_basic` 设置计算，若缺少设置则沿用薪资核算兜底：加班费 `12元/小时`、夜班补贴 `30元/次`。
- 同步自动计算后的“加班工资”和“夜班补贴”仍可在考勤汇总列表中手工编辑保存，用于业务修正。

## Verification
- RED 后端：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于缺少 `SyncProduceAttendanceBO`、`UpdateProduceAttendanceCellBO`。
- RED 前端：`node tests/upload-attendance-page.test.mjs` 失败于上传考勤 API 缺少 `syncProduceAttendance`。
- GREEN 后端定向：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，5 个测试。
- GREEN 前端定向：`node tests/upload-attendance-page.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 补充 RED 前端：`node tests/upload-attendance-page.test.mjs` 失败于页面标题仍是“上传考勤”。
- 补充 RED 后端：`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 失败于同步行未写入 `overtimePay/nightSubsidy`。
- 补充 GREEN：`node tests/upload-attendance-page.test.mjs` 通过；`mvn -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，5 个测试。
- 最终验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，6 个测试；`npm run build` 通过，保留既有 `::v-deep` 和 chunk size warning。
- SQL：已执行 `docs/sql/2026-07-15_hrm_produce_attendance_overtime_pay.sql`；`hr_0001` 至 `hr_0005` 均存在 `hrm_produce_attendance.overtime_pay decimal(10,2)`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 将 `session-catchup.py` 用 `sh` 执行，shell 把 Python 内容当脚本解析失败 | 1 | 改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan`，脚本正常执行且无输出。 |
| `application*.properties` 未加引号导致 zsh 报 `no matches found` | 1 | 改为直接读取 `src/main/resources/application-dev.properties` 并结合历史文档确认连接配置。 |
| `rg` 搜索包含不存在的 `scripts` 目录导致返回码 2 | 1 | 改用已有 `docs`、配置文件和历史进度记录定位 SQL 执行口径。 |

# Task Plan: 加班/夜班统计单人批量开始统计补全

## Goal
- 将加班/夜班统计页“开始统计”的现有业务能力补齐到“单人统计”中。
- 两边统计口径、后端计算逻辑、进度/提示和重算写库规则保持一致。
- 差异仅在员工范围：开始统计处理目标月份所有员工；单人统计按页面批量选中的员工集合逐人重算。

## Phases
1. `complete` 定位加班/夜班统计后端接口、服务方法、测试，以及前端“开始统计/单人统计”入口。
2. `complete` 补 RED 测试，约束批量单人统计会对多个员工执行同一统计口径，且不退化为只支持单个员工。
3. `complete` 实现最小后端/前端改动，复用已有开始统计逻辑，只改变员工范围。
4. `complete` 运行后端定向测试和必要前端测试/构建。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- “单人统计”名称沿用现有页面文案，但业务实际是对用户选中的一批员工重算。
- 后端若已有单员工接口，优先扩展为可接收员工 ID 集合；若已有批量接口但前端未接入，则优先修前端。
- 统计计算必须继续遵守短写事务与 bulk delete 规则，不能把整月计算包进长事务。

## Verification
- RED 后端：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldCalculateAllSelectedEmployeesInBatch test` 失败于 `QueryOvertimeNightStatisticsPageBO` 缺少 `setEmployeeIds(...)`。
- RED 前端：`node tests/overtime-night-single-selection.test.mjs` 失败，单人统计仍逐个员工循环调用后端。
- GREEN 后端定向：同一 Maven 命令通过，1 个测试。
- GREEN 前端定向：`node tests/overtime-night-api.test.mjs`、`node tests/overtime-night-single-selection.test.mjs` 通过。
- 代码评审后补充后端 RED/GREEN：`startStatisticsForEmployee_shouldSkipDeletedSelectedEmployeesLikeFullStatistics` 先失败于 `isDel=1` 员工仍被单人统计处理，修复后通过。
- 后端统计测试：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，52 个测试。
- 后端组合回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，112 个测试。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 将 `session-catchup.py` 用 `sh` 执行，shell 把 Python 内容当脚本解析失败 | 1 | 改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan`，脚本正常执行且无输出。 |

# Task Plan: 审批数据手工添加支持多日期

## Goal
- “添加审批数据”需要支持一次给同一批员工添加多个非连续日期的审批数据。
- 典型场景：同一员工在 `13日` 和 `22日` 都有调休，用户不应重复打开弹窗逐日提交。
- 保存后仍落入本地 `tbattendanceapprove`，供审批列表和加班/夜班统计按既有口径读取。

## Phases
1. `complete` 定位当前后端 `addManual` 接口、测试和前端添加弹窗实现。
2. `complete` 补 RED 测试，约束后端一次请求可保存多条日期/时间段，且兼容旧单段入参。
3. `complete` 实现后端多日期保存，保持时长由服务端计算、缺员工映射时报中文错误。
4. `complete` 补前端工具/弹窗测试，覆盖多日期构建请求与时长展示。
5. `complete` 实现前端多日期添加交互，提交后刷新列表。
6. `complete` 运行后端/前端定向验证，并更新需求/开发文档。

## Current Assumptions
- 后端应按“员工 × 日期/时间段”生成独立本地审批快照，而不是把多个非连续日期挤进一条记录；这样列表、删除、修正和统计都能沿用现有单条审批语义。
- 为兼容已有前端或调用方，旧字段 `beginTime/endTime` 继续保留；新增多段字段优先使用。
- 多日期场景允许每个日期有自己的开始/结束时间；前端可先以多行日期时间段形式实现，覆盖 `13日`、`22日` 这种非连续日期。

## Verification
- RED 后端：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest#addManualApproval_shouldPersistEachSelectedApprovalRangeForEachEmployee test` 失败于 `AddAttendanceApprovalBO` 缺少 `approvalRanges`。
- GREEN 后端：同一命令通过。
- 后端定向：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，31 个测试。
- 后端组合回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，110 个测试。
- RED 前端：`node tests/attendance-approval-fetch-utils.test.mjs` 与 `node tests/attendance-approval-dialog.test.mjs` 分别失败于请求体缺少 `approvalRanges` 和弹窗未渲染多时间段行。
- GREEN 前端：`node tests/attendance-approval-api.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs`、`node tests/approval-duration-utils.test.mjs`、`node tests/attendance-approval-dialog.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 将 `session-catchup.py` 用 `sh` 执行，shell 把 Python 内容当脚本解析失败 | 1 | 改用 `python3 ~/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan`，脚本正常执行且无输出。 |
| 代码审查发现手工添加取 `findAllByEmpIdIn` 返回的第一条映射，可能命中旧 `userId` | 1 | 补 RED 测试复现旧映射被使用，再按 `createTime/id` 选择最新映射并通过回归。 |

# Task Plan: 审批数据按主键删除

## Goal
- 在审批数据列表“操作”列新增删除功能。
- 删除必须以本地审批快照唯一主键 `approvalId` 为唯一定位条件，避免同名员工误删。
- 删除仅维护本地 `tbattendanceapprove`，不调用钉钉接口，不影响其他同名员工或同时间段审批数据。

## Phases
1. `complete` 补后端 RED 测试，约束控制器委托、服务按 `approvalId` 查删和缺 ID 中文错误。
2. `complete` 实现后端 `POST /hrmAttendanceApproval/delete`、`DeleteAttendanceApprovalBO` 和服务方法。
3. `complete` 运行审批数据后端组合回归。
4. `complete` 更新需求/开发/计划文档。

## Current Decisions
- 删除确认文案可以展示员工姓名、部门和审批时间，但这些信息不得参与后端删除条件。
- 服务层先 `findById(approvalId)` 再删除该实体，显式避免按姓名、部门、类型、月份、时间范围做批量删除。

## Verification
- RED：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 失败于 `DeleteAttendanceApprovalBO` 未创建。
- GREEN：同一命令通过，30 个测试，0 失败。
- 回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，108 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 审批数据手工添加

## Goal
- 在审批数据页面“获取审批数据”按钮旁新增“添加审批数据”按钮。
- 点击后弹窗展示手工添加审批数据表单，包含人员和部门穿梭框、审批类型、审批子类型、开始时间、结束时间、时长、保存时间。
- 时长根据开始时间和结束时间自动计算小时数，并换算成天数展示。
- 保存时间使用服务器当前 `yyyy-MM-dd HH:mm:ss`，并作为列表上的同步时间。
- 用户保存后，将记录写入现有审批数据功能使用的本地库表，供列表查询与后续统计口径复用。

## Phases
1. `complete` 定位现有审批数据后端接口、实体、仓库、测试以及前端页面。
2. `complete` 补后端 RED 测试，约束手工添加入库字段、服务器保存时间和时长计算。
3. `complete` 实现后端添加接口与必要请求/响应模型。
4. `complete` 补前端 RED 测试或工具测试，覆盖时长计算与保存参数。
5. `complete` 实现前端按钮、弹窗、穿梭框、表单联动与保存刷新。
6. `complete` 运行定向测试/构建，并更新需求与开发文档。

## Current Assumptions
- 手工新增记录保存到现有 `tbattendanceapprove` 表，不调用钉钉接口。
- 手工新增记录没有钉钉流程实例 ID，后端需生成稳定本地主键，避免与钉钉流程实例冲突。
- “人员和部门的穿梭框”优先复用现有审批获取或考勤同步员工/部门选择组件和接口。
- 时长保存单位优先使用 `小时`，天数按 `小时 / 8` 展示。

## Verification
- RED：后端测试失败于缺少 `AddAttendanceApprovalBO`；前端测试失败于缺少新增 API、时长计算函数、添加请求体工具和添加弹窗。
- GREEN：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，27 个测试。
- 回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，105 个测试。
- 前端定向：`node tests/attendance-approval-api.test.mjs`、`node tests/approval-duration-utils.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs`、`node tests/attendance-approval-dialog.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 潘红琼审批同步缺失与空时长排查

## Goal
- 解释潘红琼钉钉后台请假记录 `9` 条，但本地审批数据仅获取到 `3` 条的原因。
- 查清这 `3` 条本地审批为什么都无法计算时长。
- 确认是否能优先通过钉钉 `processinstance/get` 审批详情中的结构化时长字段获取时长，而不是依赖本地按开始/结束时间自行计算。
- 扩展检查其他员工是否也存在“钉钉后台数据 vs 本地 `tbattendanceapprove` 快照”数量不一致、时长为空或统计不可用的问题。
- 先定位根因，再决定是代码修复、数据重抓、手工修正还是对账脚本输出。

## Phases
1. `complete` 读取审批同步、审批解析、统计扣减的当前实现和测试覆盖。
2. `complete` 只读查询潘红琼本地员工、钉钉映射、审批快照、统计明细。
3. `complete` 对比本地抓取范围、流程模板、保留/清理逻辑与钉钉后台 9 条记录口径。
4. `complete` 扫描其他员工 2026-06 审批快照的空时长、数量异常、重复参与统计风险。
5. `complete` 给出根因结论；如确认代码缺陷，补 RED 测试后修复；如是历史数据，给出安全重抓或修复方案。
6. `complete` 验证并更新需求/开发/调查记录。

## Current Assumptions
- 先以本地 `hr_0003`、`2026-06` 为主要排查对象，因为当前对账记录显示潘红琼是该月剩余实出勤差异员工。
- “钉钉后台 9 条”可能与后端抓取的发起时间、完成时间、业务请假时间、审批状态、流程模板可见性、员工 userId 映射任一口径不一致。
- 本地 `duration/durationUnit` 为空时不应直接视为可用；需要先追溯钉钉审批详情表单组件是否包含 `durationInHour/durationInDay` 或“时长/请假时长”等字段，只有钉钉无可识别时长时才允许起止时间兜底。

## Verification
- 钉钉接口：`0003` 应用 `processinstance/listids` 返回潘红琼 2026-06 发起的 9 条请假；详情显示 4 条业务日期在 6 月、5 条业务日期在 5 月，且所有抽查详情均包含 `durationInHour`。
- 本地 DB：潘红琼 6 月业务日期 4 条请假快照均为空时长；`hr_0003` 2026-06 参与统计请假共 333 条，其中 288 条空时长，涉及 51 人。
- RED/GREEN：`parse_shouldReadDingTalkComplexLeaveHourDurationFromExtValue` 旧逻辑失败后通过。
- 回归：`mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest test` 通过，8 个测试；审批同步/服务/统计组合回归通过，94 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 直接用 properties 钉钉应用调用 `processinstance/get` 返回缺少 `qyapi_aflow` 权限 | 1 | 追溯 `DDAccessToken`，确认运行时按 `hrsystem.ddaccount` 的 `companyId=0003` 应用取 token；改用正确应用后可读取审批详情。 |
| 并行运行两组 Maven 测试导致 surefire fork 启动失败 | 1 | 顺序重跑同一测试组合，通过；不是代码断言失败。 |

# Task Plan: 行政人力资源部王芳钉钉后台 6 条 vs 接口 13 条差异

## Goal
- 解释为什么钉钉后台确认行政人力资源部王芳请假审批申请为 6 条，而此前接口/本地快照得到 13 条。
- 固定本地员工：行政人力资源部王芳 `employee_id=1831601326890434572 / TYNG-437 / mobile=15871989405`。
- 最终结论需区分本地错误映射与钉钉权威通讯录：该员工正确钉钉 `userId=1957266823950408`，本地当前错误保存为 `1068600027950408`。

## Phases
1. `complete` 复核后端调用的钉钉模板列表、`processinstance/listids` 参数和返回实例。
2. `complete` 拉取 13 条实例的发起时间、完成时间、业务请假时间、模板/流程编码和状态。
3. `complete` 根据 `1.png` 和 `2.png` 反查 6 条审批详情与钉钉当前通讯录。
4. `complete` 定位根因：行政人力资源部王芳与生产部王芳本地钉钉 ID / `tbattendanceuser` 映射对调。
5. `complete` 用 TDD 修复审批抓取对已有 `dingtalk_user_id` 的盲目信任。
6. `complete` 给出 guarded SQL，经用户确认后修复数据库映射并重跑行政人力资源部王芳 2026-06 审批/统计。

## Current Assumptions
- 以钉钉当前通讯录的 `userId -> 手机号 -> 部门` 为权威来源，本地 `hrm_employee.dingtalk_user_id` 需要被校验。
- 2026-06 统计应按审批业务日期过滤；6 条中业务日期在 2026-05 的 4 条调休不进入行政人力资源部王芳 2026-06 统计。

## Verification
- `2.png` 确认审批编号 `202606291142000335491` 所在部门为 `HB行政人力资源部`，假别为 `年假`，业务日期 `2026-06-28`。
- 钉钉通讯录只读校验：`1957266823950408` 是行政人力资源部王芳 `15871989405`；`1068600027950408` 是生产部王芳 `13032750052`。
- 本地只读 SQL：行政人力资源部王芳当前错误保存 `dingtalk_user_id=1068600027950408` 且 `tbattendanceuser.id=225` 也挂该 userId；生产部王芳当前错误保存 `1957266823950408`。
- 只读钉钉 `processinstance/listids`：`1957266823950408` 在 2026-06 发起时间窗口返回 `6` 条，详情部门均为 `HB行政人力资源部`；`1068600027950408` 返回 `13` 条，详情部门均为 `HB生产部`。
- 6 条中只有 `2026-06-26 年假1天`、`2026-06-28 年假1天` 属于 2026-06 业务日期，合计 `16小时`，与 Excel 行 `年假16 / 实出勤184` 一致。
- 已执行 guarded SQL 修复映射：行政人力资源部王芳 `1957266823950408 / tbattendanceuser.id=225`，生产部王芳 `1068600027950408 / tbattendanceuser.id=226`，财务部王芳不变。
- 已重新获取行政人力资源部王芳 2026-06 审批并执行单人统计：接口和落库均为 `expectedAttendanceDays=25`、`actualAttendanceHours=184.00`、`accruedAttendanceHours=200.00`，备注 `扣除：年假16.00小时`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 王芳（行政人力资源部）审批重复排查

## Goal
- 仅针对行政人力资源部王芳重新获取审批数据后的结果做复核。
- 员工身份固定为 `employee_id=1831601326890434572 / TYNG-437 / mobile=15871989405`；钉钉权威 `userId=1957266823950408`，本地当前 `1068600027950408` 为错误映射。
- 检查是否存在重复 `tbattendanceuser` 映射、重复 `tbattendanceapprove` 流程实例、语义重复审批或旧 userId 陈旧快照。
- 若确认重复，先定位根因，再按 TDD 修复代码；涉及数据库清理时先给出 SELECT 预览和安全方案，不直接执行破坏性 SQL。

## Phases
1. `complete` 用只读 SQL 确认行政人力资源部王芳当前映射、审批快照和同名员工隔离情况。
2. `complete` 对重复类型分组：映射重复、流程实例重复、业务时间语义重复、跨同名员工串数。
3. `complete` 如有代码缺陷，补 RED 测试并修复同步/清理逻辑。
4. `complete` 如有历史数据重复，给出可审核的数据修复 SQL 或执行经确认的安全修复。
5. `complete` 运行回归验证并更新需求/开发/调查记录。

## Current Assumptions
- 用户补充“重新获取的是行政人力资源部的王芳”，所以本轮不再按财务部王芳或生产部王芳排查为主对象。
- 审批重复可能来自重抓保留逻辑、缺失映射后补映射、同名员工 userId 串入、或同一业务时间多条审批同时参与统计。

## Verification
- 只读 SQL：行政人力资源部王芳当前 `tbattendanceuser.id=225 / userId=1068600027950408 / empId=1831601326890434572`，但钉钉通讯录显示该 userId 属于生产部王芳。
- 只读 SQL：生产部王芳当前 `dingtalk_user_id=1957266823950408` 且无 `tbattendanceuser`，但钉钉通讯录显示该 userId 属于行政人力资源部王芳。
- 只读钉钉：`1957266823950408` 返回 6 条 2026-06 发起的请假申请，详情部门均为 `HB行政人力资源部`；业务日期在 2026-06 的为 2 条年假，共 `16小时`。
- 只读钉钉：`1068600027950408` 返回 13 条 2026-06 发起的请假申请，详情部门均为 `HB生产部`；这些不属于行政人力资源部王芳。
- 重复检查结论：行政人力资源部王芳的 6 条不是重复；此前 13 条是本地映射对调导致抓错人。
- Excel 复核：行政人力资源部王芳为 `应出勤200 / 实出勤184 / 应计200`，请假列为 `年假16`、`调休0`。
- RED：`fetchMonthData_shouldRefreshExistingDingTalkUserIdWhenNameAndMobileMatchDifferentUser` 在旧逻辑下失败，旧代码继续用 `wrong-ding` 抓取。
- GREEN：同一测试与既有员工映射测试通过，错误 `dingtalk_user_id` 会按姓名手机号刷新。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 生产体系月休四天统计与王芳映射补齐

## Goal
- “开始统计”时生产体系员工默认按固定月休 4 天计算应出勤，减少用户每月维护操作。
- 尽量复用现有假期/业务公休数据，让 2026-06 生产体系可得到 `25天 * 8 = 200小时`。
- 查明并说明 `王芳(行政人力资源部)` 如何补齐 `tbattendanceuser` 映射，以及其他员工当前如何建立映射。

## Phases
1. `complete` 定位加班/夜班统计应出勤计算和员工钉钉映射链路。
2. `complete` 补 RED 测试覆盖生产体系月休 4 天应出勤。
3. `complete` 实现最小统计规则并保持行政体系月历口径不变。
4. `complete` 复核王芳映射数据，给出补齐方案或安全 SQL。
5. `complete` 运行定向测试并更新需求/开发/调查记录。

## Current Assumptions
- `affiliation_system=2` 表示生产体系。
- 生产体系基础应出勤天数按 `当月自然天数 - 4` 计算；若现有系统已有当月法定/业务假期配置，应只额外扣除落在工作日的法定休息日，以匹配 2026-06 的 `30 - 4 - 1 = 25天`。
- 员工映射不能按姓名补齐；王芳必须使用 `employee_id + 手机号 + 钉钉通讯录部门` 校验，不能直接信任本地当前对调的 `dingtalk_user_id`。

## Verification
- RED：`startStatistics_shouldUseFixedFourDayMonthlyRestForProductionEmployee` 在旧逻辑下失败，2026-06 生产员工期望 `25` 天，实际为 `0`。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，47 个测试，0 失败。
- 映射链路定向：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test` 通过，2 个测试，0 失败。
- 只读 DB + 钉钉复核：`hr_0003` 行政人力资源部王芳与生产部王芳的 `dingtalk_user_id` 对调；修复应将行政人力资源部王芳改为 `1957266823950408`，生产部王芳改为 `1068600027950408`，财务部王芳保持 `0305684530950408`。
- GREEN：`startStatistics_shouldDeductOnlyWeekdayLegalRestForProductionEmployee` 通过，覆盖 2026-06 端午 3 天仅扣工作日 `2026-06-19`，不重复扣周末 `2026-06-20/21`。
- 实库验证：行政人力资源部王芳单人统计已落库 `expected_attendance_days=25`、`actual_attendance_days=23`、`accrued_attendance_hours=200.00`。
- 回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，92 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| Mockito `UnnecessaryStubbingException` | 1 | 对齐既有测试月份 stub，删除新生产体系测试不参与断言的回查 stub，并恢复被误删的 `2026-03` 保存后回查 stub。 |

# Task Plan: 行政体系 Excel 与加班夜班统计出勤对账

## Goal
- 读取 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx` 中行政体系员工的应出勤、实出勤、应计出勤数据。
- 与系统“加班/夜班统计”中 2026-06 的应出勤时间、实际出勤、应计出勤逐员工对比。
- 找出不同数据的员工，并追溯导致不一致的具体原因。
- 给出可执行解决方案，区分“系统口径/数据需要修正”和“Excel 口径或数据需要调整”。

## Phases
1. `complete` 解析 Excel 工作表结构、员工匹配字段和三项出勤列。
2. `complete` 获取系统 2026-06 加班/夜班统计数据与相关审批/员工体系明细。
3. `complete` 生成逐员工差异清单并按差异类型归因。
4. `complete` 给出解决方案并更新项目文档/调查记录。

## Current Assumptions
- 文件名中的“6月”对应统计月份 `2026-06`。
- Excel 中三项数据与系统矩阵列对应为：应出勤时间、实际出勤小时、应计出勤小时；若 Excel 以天为主，按 `1天=8小时` 折算后比较。
- 员工匹配优先使用工号/手机号等稳定字段；没有稳定字段时再用姓名，并检查同名风险。
- 用户补充后确认：生产体系固定月休 4 天；2026-06 Excel 中 `200小时` 行应按生产体系固定月休和当月业务公休/假期口径分析。
- `hr_0003` 存在多个王芳；王芳相关行必须按员工 ID、工号、手机号、部门和钉钉 `userId` 区分。

## Verification
- Excel 主表 `6月` 读取到 27 名员工。
- 本机接口 `POST /hrsystem/hrmOvertimeNightStatistics/queryPageList` 逐员工查询 `2026-06` 成功。
- 直接查询 `hr_0003.hrm_employee / tbattendanceuser / tbattendanceapprove` 复核员工体系、钉钉映射、审批时长和重复审批。
- 生成报告：`docs/reports/2026-07-14-admin-attendance-excel-reconciliation.md`。
- 2026-07-15 再次复查：当前统计明细最大更新时间 `2026-07-15 04:57:36`；Excel 27 人中 15 人三项一致，12 人仍有差异；应出勤和应计出勤已全部一致，剩余差异全部为实出勤。
- 2026-07-15 代码修正后重算：生产体系实际出勤不再叠加月度自动加班，重算 `2026-06` 后 Excel 27 人中 16 人三项一致，11 人仍有差异；应出勤和应计出勤全部一致，剩余差异全部为实出勤。
- 2026-07-15 再次重算对比：Excel 27 人中 21 人三项一致，6 人仍有差异；应出勤和应计出勤全部一致，剩余差异全部为实出勤。剩余人员为余德胜、聂小玲、马春霞、张雪梅、方小荣、何晓光。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `ERROR 1054 Unknown column 'e.name'` | 复核 `hr_0003.hrm_employee` 王芳同名数据时按 `name` 字段查询 | `DESCRIBE hrm_employee` 确认姓名字段为 `employee_name`，改用正确字段后查到同名王芳员工主数据 |

# Task Plan: 王琪实际出勤与应计出勤计算过程悬浮展示

## Goal
- 在加班/夜班统计的“实际出勤时间统计”矩阵中，王琪等员工的“实际出勤小时(天数)”单元格鼠标悬浮时展示具体计算过程。
- 同一矩阵的“应计出勤小时(天数)”单元格鼠标悬浮时展示具体计算过程。
- 不改变既有实际出勤、应计出勤计算结果，只补充前端可读的计算说明。

## Phases
1. `complete` 读取前后端文档、定位矩阵列和数据来源。
2. `complete` 补前端 RED 测试，覆盖实际出勤/应计出勤单元格 tooltip 明细。
3. `complete` 实现最小前端展示逻辑；如接口缺少必要字段，再补后端字段与测试。
4. `complete` 运行定向测试和必要构建。
5. `complete` 更新需求/开发文档、findings、progress。

## Current Assumptions
- “王琪”是业务验收样例，不应写死姓名；应对所有员工的同类单元格生效。
- 实际出勤公式：`应出勤小时 + 可计入实际出勤的加班小时 - 可扣减审批小时`。
- 应计出勤公式：`实际出勤时间 + 调休 + 年假 - 加班 + 病假`。
- 若前端只能拿到最终值和备注，先展示可解释到字段级的过程；若要显示每项精确数值，需要后端透出明细项。

## Verification
- RED：`node tests/overtime-night-utils.test.mjs` 失败，`actualAttendanceCalculationText` 为空。
- RED：`node tests/attendance-display-columns.test.mjs` 失败，实际/应计出勤列缺少 tooltip。
- GREEN：`node tests/overtime-night-utils.test.mjs` 通过。
- GREEN：`node tests/attendance-display-columns.test.mjs` 通过。
- 构建：`npm run build` 通过，保留既有 `::v-deep` deprecation warning 和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 闫倩 0.07 天请假时长舍入修正

## Goal
- 解释闫倩 `2026-06-26 17:30 -> 18:00` 请假为什么旧逻辑显示/扣减为 `0.56` 小时。
- 后端已按业务口径修正：未来同步优先小时值，历史 `0.07天` 小数天快照在统计时可按起止时间纠偏为 `0.50` 小时。
- 前端审批数据列表也需按同一口径显示，避免历史 `0.07天` 继续展示为 `0.56小时(0.07天)`。

## Phases
1. `complete` 读取前后端需求/开发文档，确认审批时长展示与统计口径。
2. `complete` 为前端审批时长工具补 RED 测试，覆盖 `0.07天 + 17:30-18:00`。
3. `complete` 实现前端小数天舍入纠偏展示。
4. `complete` 运行前端审批相关测试、构建和后端回归确认。
5. `complete` 更新前端文档与计划记录。

## Current Assumptions
- 该审批的本地历史快照为 `duration=0.07`、`durationUnit=天`；旧显示按 `0.07 * 8 = 0.56` 小时。
- 当天起止时间差为 30 分钟，业务正确值为 `0.50` 小时。
- 纠偏只用于小数天与起止时间存在小范围舍入差异的展示，不改变用户点击编辑时保存的原始字段结构。

## Verification
- RED：`node tests/approval-duration-utils.test.mjs` 失败，旧输出为 `0.56小时(0.07天)`，期望 `0.5小时(0.06天)`。
- GREEN：`node tests/approval-duration-utils.test.mjs` 通过。
- 前端审批回归：`node tests/attendance-approval-dialog.test.mjs`、`node tests/attendance-approval-api.test.mjs` 均通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` deprecation warning 和 chunk-size warning。
- 后端定向：`mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest#parse_shouldPreferHourDurationForFractionalComplexLeave,HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldUseTimeRangeForRoundedFractionalDayLeaveDeduction test` 通过，2 个测试，0 失败。
- 后端审批/统计回归：`mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，75 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 审批通过过滤与取消至统计复核

## Goal
- 获取审批数据时只保存钉钉实例 `status=COMPLETED` 且 `result=agree` 的申请。
- 取消、拒绝、终止、撤销、进行中等其他状态一律不保存、不加入 retained IDs。
- 带 `预计加班时长` 字段但审批结果不是 `agree` 的实例仍不保存。
- `statisticsStatus=取消至统计` 的本地审批不参与实际出勤、应计出勤、加班审批覆盖和请假扣减等统计计算。

## Phases
1. `complete` 读取需求/开发文档并确认目标规则。
2. `complete` 检查审批抓取和加班/夜班统计实现。
3. `complete` 运行现有审批与统计回归测试确认规则已覆盖。
4. `complete` 更新文档和计划记录。

## Verification
- `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，96 个测试，0 失败。
- 测试日志确认拒绝/终止实例输出“审批实例非同意完成，跳过入库”，retained IDs 仅包含通过实例 `PROC-AGREE`。
- `HrmOvertimeNightStatisticsServiceImplTest` 覆盖 `取消至统计` 加班审批不得覆盖自动加班、`取消至统计` 请假审批不得扣减实际出勤。

## Notes
- 本轮未改生产代码；现有实现已经满足规则。
- 历史拒绝审批快照需要对对应员工和月份重新执行“获取审批数据”才会被陈旧数据清理删除，或先通过页面“取消至统计”临时排除统计影响。

# Task Plan: 闫倩实际出勤加法排查

## Goal
- 查清闫倩实际出勤是否错误叠加了加班。
- 若闫倩属于行政体系，则实际出勤只能加本地审批加班，不得加日级自动加班。
- 保持张明、李明明已校准口径不回退。

## Phases
1. `in_progress` 查询闫倩接口返回、员工体系、加班来源和审批扣减。
2. `pending` 将闫倩场景抽成 RED 测试。
3. `pending` 修复实际出勤加法口径或数据识别口径。
4. `pending` 回归验证并更新文档记录。

## Current Assumptions
- 当前用户说的“有做了加法运算”指实际出勤小时大于应出勤减扣后的结果。
- 本轮先按上下文默认月份 `2026-06` 排查；如接口数据不匹配，再扩大月份范围。

## Verification
- 待补。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 行政体系实际出勤只计审批加班

## Goal
- 修正张明这类行政体系员工实际出勤小时：日级自动加班不计入实际出勤，只计入本地钉钉审批加班。
- 保持李明明场景正确：审批加班 `8` 小时仍计入实际出勤，因此 `184 + 8 - 32 = 160`。
- 查询列表、开始统计、单人明细和每日总览需使用同一口径。

## Phases
1. `complete` 读取当前文档和统计服务，确认冲突来源。
2. `complete` 补 RED 测试覆盖张明行政体系不加日级自动加班。
3. `complete` 实现行政体系实际出勤加班来源切换为审批加班。
4. `complete` 回归验证、接口复核并更新文档/计划。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee test` 失败；旧实现返回 `13.00` 和 `28` 天。
- GREEN：同一命令通过，2 个测试，0 失败；开始统计中行政体系日级自动加班 `5.00` 仍展示为加班汇总，但实际出勤保持 `8.00` 小时。
- GREEN：扩展定向命令 `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployee+queryPageList_shouldAddOvertimeApprovalToActualAttendance test` 通过，3 个测试，0 失败。
- 应计出勤 RED：同一张明定向命令失败；开始统计应计出勤返回 `3.00` 而不是 `8.00`，查询张明返回 `136.28` 而不是 `184.00`。
- 应计出勤 GREEN：同一张明定向命令通过，2 个测试，0 失败。
- 回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，46 个测试，0 失败。
- 本地 API：`queryPageList` 查询张明 `2026-06` 返回 `expectedAttendanceDays=23`、`overtimeHours=47.72`、`actualAttendanceHours=184.00`、`actualAttendanceDays=23`、`accruedAttendanceHours=184.00`。
- 本地 API：`queryPageList` 查询李明明 `2026-06` 仍返回 `expectedAttendanceDays=23`、`overtimeHours=8.00`、`actualAttendanceHours=160.00`、`actualAttendanceDays=20`、`accruedAttendanceHours=184.00`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 李明明实际出勤与应计出勤小时差异修复

## Goal
- 查明李明明“实际出勤时间统计”中显示 `184(23) / 0(0) / 207.4(25.93)`，但业务正确小时值应为 `184 / 160 / 184` 的根因。
- 修复加班/夜班统计开始统计与查询兜底中的月度实际出勤小时、应计出勤小时口径，确保行政体系或有加班审批场景不会把日级加班错误叠加进实际出勤。
- 通过后端 RED/GREEN 测试锁定该口径，并在需要时重新统计李明明目标月份数据。

## Phases
1. `complete` 复现并定位李明明统计差异的数据来源。
2. `complete` 补后端 RED 测试覆盖正确小时口径。
3. `complete` 修正统计计算并回归验证。
4. `complete` 必要时重算数据库数据并更新文档记录。

## Current Assumptions
- 用户给出的三个正确小时值依次对应：应出勤时间 `184`、实际出勤小时 `160`、应计出勤小时 `184`。
- 当前错误的 `207.4` 表明加班小时可能被错误叠加进了实际出勤或应计出勤公式。
- 当前错误的 `0(0)` 可能来自查询列表未读取/未回填 `actualAttendanceHours`，或前端列取值映射到了错误字段。

## Verification
- RED：`HrmAttendanceApprovalProcessInstanceParserTest#parse_shouldReadDingTalkComplexLeaveDurationFromExtValue` 在旧实现下失败，`duration` 为空。
- RED：李明明 2026-06 统计场景在旧口径下实际出勤/应计出勤不等于 `160 / 184`。
- GREEN：`mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，52 个测试，0 失败。
- GREEN：审批抓取/审批服务/统计 broader 回归 `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalProcessInstanceParserTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，102 个测试，0 失败。
- GREEN：前端 `node tests/overtime-night-utils.test.mjs`、`node tests/attendance-display-columns.test.mjs` 和 `npm run build` 通过。
- 数据库：`docs/sql/2026-07-14_overtime_night_accrued_attendance_hours.sql` 已在 dev MySQL 执行；`hr_0001` 至 `hr_0005` 均存在 `accrued_attendance_hours`。
- 数据库：`hr_0003` 李明明 `2026-06` 明细现为应出勤 `23` 天、实际出勤 `20` 天、加班 `8.00` 小时、应计出勤 `184.00` 小时。
- 本地 API：`queryPageList`、`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 均返回李明明 `expectedAttendanceDays=23`、`actualAttendanceHours=160.00`、`accruedAttendanceHours=184.00`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 加班/夜班统计应计出勤计算项

## Goal
- “开始统计”新增应计出勤计算项，公式为：`应计出勤小时 = 实际出勤时间 + 调休 + 年假 - 加班 + 病假`。
- 应计出勤结果需要随统计明细入库保存，并通过汇总列表、单人月度明细和显示所有接口返回给前端。
- “实际出勤时间统计”矩阵新增第四列“应计出勤小时(天数)”，展示新计算结果，列位置为：姓名、应出勤时间、实际出勤小时(天数)、应计出勤小时(天数)、每日列、备注。

## Phases
1. `complete` 定位统计明细实体/VO/服务/前端矩阵工具和现有测试。
2. `complete` 补 RED 测试覆盖后端应计出勤计算和前端第四列展示。
3. `complete` 实现后端字段、计算、落库、查询透出和数据库迁移脚本。
4. `complete` 实现前端矩阵列与工具函数展示。
5. `complete` 运行后端/前端定向测试与构建。
6. `complete` 更新前后端需求/开发文档、findings、progress。

## Current Assumptions
- 公式中的“实际出勤时间”使用当前后端已经计算出的 `actualAttendanceHours`。
- 公式中的“调休 / 年假 / 病假”使用当前员工目标月份参与统计的本地审批小时；`statisticsStatus=取消至统计` 不参与。
- 公式中的“加班”使用与实际出勤一致的可计入口径：行政体系只取本地审批加班，非行政体系取统计服务月度 `overtimeHours` 汇总。
- 应计出勤小时按小时保存并展示为 `小时(天数)`，天数按小时除以 `8` 展示，和实际出勤矩阵格式一致。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeductPersonalSickCompensatoryAndAnnualLeaveFromExpectedAttendanceDays+startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployeeWithoutOvertimeApproval test` 编译失败，缺少 `getAccruedAttendanceHours()`。
- RED：`node tests/overtime-night-utils.test.mjs` 失败，缺少 `accruedAttendanceSummaryValue`。
- RED：`node tests/attendance-display-columns.test.mjs` 失败，实际出勤矩阵缺少“应计出勤小时(天数)”列。
- GREEN：上述后端定向测试通过，2 个测试，0 失败。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，44 个测试，0 失败。
- GREEN：`node tests/overtime-night-utils.test.mjs` 通过。
- GREEN：`node tests/attendance-display-columns.test.mjs` 通过。
- 构建：`npm run build` 通过，保留既有 `::v-deep` deprecation warning 和 chunk-size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 加班/夜班统计实际出勤年假扣减与列顺序调整

## Goal
- 将“加班/夜班统计”开始统计与查询兜底中的实际出勤扣减口径调整为：`应出勤天数 - 事假 - 病假 - 调休 - 年假 + 加班`（按小时折算后计算）。
- 保持既有规则：无正数应出勤天数时仍使用打卡有效分钟兜底；不参与统计的审批不扣减。
- 前端“实际出勤时间统计”矩阵中，将“小时(天数)汇总”列改名为“实际出勤小时(天数)”，并与“应出勤时间”互换显示顺序。

## Phases
1. `complete` 定位后端实际出勤计算、前端矩阵列和现有测试。
2. `complete` 先补 RED 测试覆盖年假扣减与实际出勤列名/顺序。
3. `complete` 实现后端扣减类型新增年假、前端列名顺序调整。
4. `complete` 运行后端/前端定向测试和构建验证。
5. `complete` 更新后端与前端需求/开发文档、发现和进度记录。

## Current Assumptions
- “加班”继续复用当前服务传入 `calculateActualAttendanceFromExpected(...)` 的可计入实际出勤加班小时；既有行政体系/加班审批例外规则暂不额外扩大修改。
- 年假审批识别方式与事假/病假/调休一致，按 `tbattendanceapprove.tagName/subType` 包含“年假”判断。
- 列顺序调整仅作用于“实际出勤时间统计”矩阵，不改变每天加班小时和每天夜班次数矩阵。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeductPersonalSickCompensatoryAndAnnualLeaveFromExpectedAttendanceDays test` 失败，旧实现返回 `21` 天，期望 `20` 天。
- RED：`node tests/attendance-display-columns.test.mjs` 失败，旧页面仍按“小时(天数)汇总 -> 应出勤时间”展示。
- GREEN：后端单测通过，1 个测试，0 失败。
- GREEN：`node tests/attendance-display-columns.test.mjs` 通过。
- 回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，44 个测试，0 失败。
- 回归：`node tests/overtime-night-utils.test.mjs` 通过。
- 构建：`npm run build` 通过，保留既有 `::v-deep` deprecation warning 和 chunk size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 审批数据按员工可见模板抓取修复

## Goal
- 修复“考勤数据/审批数据获取”中任意员工都会先触发管理员审批模板查询失败日志的问题。
- 流程编码解析必须按右侧框实际目标员工的钉钉 `userId` 调用员工可见模板，不再优先调用管理员可管理模板。
- 员工范围继续由前端右侧员工/部门展开结果决定；`employeeIds` 非空时后端只能抓取这些员工。

## Phases
1. `complete` 读取审批同步链路、现有测试和相关日志文案，定位失败边界。
2. `complete` 补 RED 测试覆盖“解析流程编码不再调用管理员模板，只按当前员工 userId 查询员工可见模板”。
3. `complete` 实现最小后端修复并避免影响已缓存流程编码、员工缺失跳过逻辑。
4. `complete` 运行定向审批同步测试和必要回归。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- `ddAccount.adminId` 不再参与手工获取审批数据的流程编码解析主链路。
- `process/listbyuserid` 必须使用目标员工解析出的 `dingTalkUserId`。
- 全员抓取需逐员工解析可见流程，不能用某个员工的可见流程代表其他员工。

## Verification
- RED：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` 失败，新增用例 `resolveProcessCodes_shouldFallbackByEmployeeUserWhenManageableTemplateQueryFails` 暴露管理员模板 `errcode=500001` 时旧实现直接抛错、不回退。
- GREEN：同一命令通过，7 个测试，0 失败；日志包含 `adminUserId=admin-user, employeeUserId=ding-101`。
- 工作流回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest test` 通过，20 个测试，0 失败。
- 审批组合回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` 通过，50 个测试，0 失败。
- 李明明 follow-up（2026-07-14）：`hr_0003` 在职李明明 `employee_id=1831601326890434563` 映射钉钉 `userId=024662561026250638`，本地已有该 `userId` 的 5 条审批快照；此前 `companyId=0003` 的 `adminId=296842114330963873` 会触发管理员模板回退日志，最新实现已移除该管理员模板预查询。
- 最新修复 RED：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` 失败 3 个用例，证明旧实现仍调用管理员模板。
- 最新修复 GREEN：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplTest test` 通过，7 个测试 0 失败；`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test` 通过，50 个测试 0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 加班/夜班统计实际出勤加入加班时间

## Goal
- 调整“加班/夜班统计”的开始统计逻辑：实际出勤时间应按 `应出勤时间 + 加班时间 - 事假/病假/调休` 计算。
- 继续只扣减本地 `tbattendanceapprove` 中可统计的 `事假`、`病假`、`调休` 审批；`statisticsStatus=取消至统计` 的审批不参与扣减。
- 保持无正数应出勤天数时的旧打卡有效分钟兜底，不影响生产体系或无应出勤场景。

## Phases
1. `complete` 定位统计服务和现有实际出勤测试。
2. `complete` 先补 RED 测试覆盖“应出勤 + 加班 - 可扣减请假”的开始统计口径。
3. `complete` 实现最小后端修改并保持查询响应字段兼容。
4. `complete` 运行定向测试。
5. `complete` 更新需求、开发、发现和进度文档。

## Current Assumptions
- “加班时间”使用当前统计服务已计算出的月度/日级 `overtimeHours`，包含本地钉钉加班审批优先覆盖后的结果。
- 实际出勤天数字段继续由实际出勤小时除以 8 后向下取整，实际小时字段用于展示小数小时。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldAddOvertimeHoursBeforeDeductingLeaveFromExpectedAttendanceDays test` 失败，旧实现返回 `actualAttendanceDays=1`，期望 `2`。
- GREEN：同一单测通过，1 个测试，0 失败。
- 回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，39 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 吴晓霞加班审批异常与统计操作列

## Goal
- 查清 `hr_0003` 吴晓霞审批数据中 `06:05-07:05` 加班记录的本地来源，解释为什么钉钉后台没有该记录且钉钉显示 4 条、本地开发环境只有 3 条。
- 在审批数据列表新增“操作”列，按行提供“取消至统计”或“添加至统计”按钮。
- 当某行审批状态为“取消至统计”时，只显示“添加至统计”；其他状态只显示“取消至统计”。
- 后端需支持本地标记审批是否参与加班/夜班统计，统计查询需尊重该标记。

## Phases
1. `complete` 读取后端/前端审批数据文档、代码与本地 `hr_0003` 数据证据。
2. `complete` 定位吴晓霞 `06:05-07:05` 记录和缺失第 4 条审批的根因。
3. `complete` 先补后端/前端 RED 测试，覆盖统计状态按钮和统计排除/恢复。
4. `complete` 实现后端审批统计状态更新接口与统计读取过滤。
5. `complete` 实现前端审批数据“操作”列按钮显示与调用。
6. `complete` 运行定向测试和必要构建。
7. `complete` 更新需求/开发文档、计划、发现和进度记录。

## Current Outcome
- “取消至统计”表示该审批保留在审批数据列表中，但不再参与加班/夜班统计的审批优先加班小时口径。
- “添加至统计”表示恢复参与统计。
- 操作是本地状态修改，不触发钉钉接口，也不删除审批快照。
- 吴晓霞 `2026-06` 本地异常 `06:05-07:05` 已确认来自钉钉 `COMPLETED/refuse` 拒绝实例；钉钉后台截图中的 4 条是按“发起时间”展示的申请列表，其中实际只有 2 条 `COMPLETED/agree` 加班、1 条 `COMPLETED/refuse`、1 条 `TERMINATED`。
- 日志中的 `userid=296842114330963873` 是 `hrsystem.ddAccount.AdminID` 钉钉管理员，用于管理员模板预查询，不是吴晓霞；吴晓霞实际 `userId=262756571521566047`，回退 `process/listbyuserid` 正常。
- 审批抓取现只保存钉钉 `status=COMPLETED` 且 `result=agree` 的实例；拒绝和终止实例跳过入库且不加入 retained IDs，重复抓取后会被陈旧快照清理。
- 使用用户同款 payload `approvalTypes=["all"]`、`employeeIds=["1831601326890434565"]`、`month="2026-06"` 本地重抓后返回 `insertedCount=3`；库中保留 2 条同意加班和 1 条调休，`06:05-07:05` 已清理。
- 同步链路已改为按员工解析可见流程编码，并在全员抓取后按实际员工范围清理陈旧快照，降低漏抓第 4 条或保留旧快照的风险。
- 后端新增 `statisticsStatus` 字段、状态更新接口和统计过滤；前端新增“操作”列。
- 已新增 `docs/sql/2026-07-13_tbattendanceapprove_statistics_status.sql`；dev MySQL `153.0.237.98` 的 `hr_0001` 至 `hr_0005` 已执行并验证，新列均存在。若公网/生产连接私有库，仍需在可访问线上库的机器执行同一脚本。
- 获取审批数据回退到员工可见模板时，若个别员工钉钉 `userId` 返回 `用户不存在`，现跳过该员工继续抓取其他员工；被跳过员工不写完成标记，也不参与陈旧快照清理。若本次员工全部不存在，则返回明确提示要求同步或修正员工钉钉映射。
- 单选员工抓不到但全员能抓到的差异已定位为同一员工多条 `tbattendanceuser` 映射时取值依赖数据库返回顺序；现单选和全员均优先最新映射（`createTime`、`id`），避免命中历史失效 `userId`。
- 单个、批量、全量获取审批数据现统一从 `hrm_employee` 解析员工：优先使用 `hrm_employee.dingtalk_user_id`，缺失或失效时按员工姓名 + 手机号调用钉钉花名册唯一匹配，唯一命中后写回员工表并补齐 `tbattendanceuser`，再开始抓取审批。
- 每次成功抓取后以本次钉钉返回结果为准保留审批快照，并按该员工所有历史钉钉 `userId` 清理同月同类型本次未返回的旧审批，避免旧 `userId` 或旧快照残留。

## Verification
- `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`：通过，84 个测试，0 失败。
- `node tests/attendance-approval-api.test.mjs`：通过。
- `node tests/attendance-approval-dialog.test.mjs`：通过。
- `node tests/attendance-approval-fetch-utils.test.mjs`：通过。
- `npm run build`：通过，保留既有 `::v-deep` 与 chunk size warning。
- `2026-07-13 16:17` 在 dev MySQL `153.0.237.98` 执行 `docs/sql/2026-07-13_tbattendanceapprove_statistics_status.sql`：通过；`information_schema.columns` 返回 `existing_columns=5`，`hr_0001` 至 `hr_0005` 均存在 `statisticsStatus varchar(20) NULL`。
- RED：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched test` 失败，原实现会中断整个抓取并返回通用失败。
- GREEN：同一命令通过，2 个测试，0 失败。
- GREEN：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched+fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees test`：通过，3 个测试，0 失败。
- 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`：通过，84 个测试，0 失败。
- RED：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test` 失败，原实现只依赖 `tbattendanceuser`，员工表已有或可反查的钉钉 ID 不会被用于抓取。
- GREEN：同一命令通过，2 个测试，0 失败。
- 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`：通过，86 个测试，0 失败。
- RED：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldPersistOnlyApprovedWorkflowInstances test` 失败，原实现会保存 `COMPLETED/refuse` 和 `TERMINATED` 实例，返回 `3` 而不是期望的 `1`。
- GREEN：同一命令通过，1 个测试，0 失败；日志显示拒绝/终止实例跳过入库，retained IDs 仅保留同意完成实例。
- 回归：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test`：通过，87 个测试，0 失败。
- 本地接口：`POST http://127.0.0.1:8081/api/hrsystem/hrmAttendanceApproval/fetchMonthData`，payload `{"approvalTypes":["all"],"employeeIds":["1831601326890434565"],"month":"2026-06"}` 返回 `insertedCount=3`。
- 数据库复核：`hr_0003.tbattendanceapprove` 中吴晓霞 `2026-06` 保留 `2026-06-04 12:00-13:00` 加班、`2026-06-15 18:00-19:05` 加班、`2026-06-08 08:00-08:30` 调休；拒绝实例 `06:05-07:05` 不再存在。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 误用 `sh` 执行，出现 shell 语法错误 | 1 | 改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan` 重新执行。 |

# Task Plan: 审批数据闫倩调休同步与子类型手工修改

## Goal
- 查明闫倩 `2026-06` 审批数据同步后多出 `2026-06-04 请假-调休` 的根因，确保“审批数据”列表只保留业务应展示的两条 `06-13`、`06-26` 调休记录或提供可解释的修正路径。
- 在“审批数据”列表中将“审批子类型”列改为可点击编辑，下拉选项包含系统已知全部子类型，便于特殊情况下手工修改。
- 后端需提供稳定的本地子类型更新接口，禁止通过列表查询链路调用钉钉接口。

## Phases
1. `complete` 读取本轮相关后端/前端文档和当前审批数据同步、查询代码。
2. `complete` 定位闫倩 `2026-06` 三条审批记录的入库来源、字段差异和列表展示口径。
3. `complete` 为同步保留手工修正逻辑和子类型更新能力补回归测试。
4. `complete` 实现后端最小修复：子类型选项与更新接口、重抓保留本地子类型并抓后清理陈旧快照。
5. `complete` 实现前端“审批子类型”单元格点击下拉修改交互。
6. `complete` 运行后端/前端定向测试和必要构建。
7. `complete` 更新需求/开发文档和本轮计划记录。

## Current Outcome
- `2026-06-04 请假-调休` 已确认是本地 `tbattendanceapprove` 中同步入库的真实审批快照，不是列表查询临时生成。
- 本轮没有执行破坏性数据库写操作，也没有自动删除或隐藏闫倩 `06-04` 记录；若业务确认该审批不应按“调休”计入，可通过新增的子类型内联编辑手工修正。
- 手工修正只更新本地 `subType`，保留 `tagName/bizType` 等原始分类字段；后续对同一流程实例重复手工获取时会保留本地修正后的 `subType`。

## Verification
- `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test`：通过，37 个测试，0 失败。
- `node tests/attendance-approval-api.test.mjs`：通过。
- `node tests/attendance-approval-dialog.test.mjs`：通过。
- `node tests/attendance-approval-fetch-utils.test.mjs`：通过。
- `npm run build`：通过，保留既有 `::v-deep` 与 chunk-size warning。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `Modifying queries can only use void or int/Integer as return type!` | 1 | 定位到 `tbattendanceapproveRepository#deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(...)` 使用 `@Modifying @Query` 但返回 `long`；已改为 `int` 并新增仓库契约测试。 |

# Task Plan: 加班/夜班统计出勤区域与行政应出勤修复

## Goal
- 在“加班/夜班统计”的单人查看和显示所有页面中，把应出勤时间、实际出勤时间从“每天加班小时”和“每天夜班次数”矩阵表移出，单独作为出勤时间区域展示。
- 每天加班小时矩阵和每天夜班次数矩阵不再包含应出勤时间、实际出勤时间列。
- 修复行政体系员工（例如潘红琼）应出勤时间无数据的问题，确保按单双休设置月历的上班天数显示。

## Phases
1. `complete` 读取后端与前端需求/开发文档，确认当前旧口径和新口径差异。
2. `complete` 定位前端单人查看、显示所有页面的出勤字段渲染与测试。
3. `complete` 定位后端应出勤天数计算链路，找出行政员工无数据的根因。
4. `complete` 先补失败测试：前端列结构/独立出勤区域；后端行政员工应出勤计算。
5. `complete` 实现前端区域拆分与后端应出勤修复。
6. `complete` 运行定向测试和必要构建验证。
7. `complete` 更新前后端需求/开发文档与进度记录。

## Current Hypotheses
- 前端问题：旧实现同时把应/实际出勤时间放在顶部摘要和两张矩阵表中，新需求要求只保留独立区域，矩阵表删除两列。
- 后端问题：应出勤计算只在 `affiliationSystem == 1` 时调用单双休月历；如果员工体系字段未被查询结果带出、字段名映射不一致，或缓存/旧统计明细未重算，会导致行政员工显示 0/空。

## Verification
- `node tests/attendance-display-columns.test.mjs`：通过。
- `node tests/overtime-night-utils.test.mjs`：通过。
- `npm run build`：通过，保留既有 `::v-deep` 与 chunk size warning。
- `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`：通过，29 个测试，0 失败。
- 前端开发服务已启动：`http://localhost:8080/`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 前端 RED：显示所有加班表仍有应/实际出勤列 | 1 | 删除显示所有与单人查看两张日矩阵中的出勤列，保留独立“出勤时间统计”区域。 |
| 后端 RED：旧明细缺出勤字段时行政应出勤返回 0/null | 1 | 三个查询路径补员工体系上下文，行政员工按单双休月历 `workDays` 兜底。 |
# Task Plan: 李凤皇实际出勤天数调休折算排查

## Goal
- 定位“李凤皇应出勤 200 小时、实际出勤 196 小时、调休 4 小时，但实际出勤汇总显示 24 天”的根因。
- 明确现有实际出勤天数是否只按打卡小时折算，是否遗漏调休审批/请假调休小时。
- 用失败测试锁定“196 小时实出勤 + 4 小时调休应补足实际出勤”的业务口径后再修复。

## Phases
1. `complete` 读取现有加班/夜班统计、审批/调休、实际出勤计算链路，确认 24 天来源。
2. `complete` 找到同类工作示例或现有测试，比较实际出勤与请假/调休数据的处理差异。
3. `complete` 形成单一根因假设并补 RED 测试。
4. `complete` 实现最小修复，避免影响加班小时、夜班次数和应出勤天数口径。
5. `complete` 运行定向测试，更新需求/开发文档和排查记录。

## Current Evidence
- 项目文档当前写明实际出勤天数“只基于 `hrm_attendance_clock`”，同一业务日期最早到最晚打卡跨度达到 480 分钟计 1 天。
- 用户反馈的业务场景包含调休 4 小时，说明实际出勤汇总可能需要把可抵扣调休小时纳入月度实际出勤折算。
- 根因已确认：`calculateActualAttendanceDays(...)` 只数每日打卡跨度满 480 分钟的整天，`buildAttendanceApprovalMap(...)` 只纳入加班审批，`请假/调休` 审批未进入实际出勤汇总。
- 修复后实际出勤天数按月汇总有效打卡分钟（单日最多 480 分钟）与调休审批分钟，再按 480 分钟折算整数天。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` 失败，新增用例 `expected:<25> but was:<24>`。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` 通过，30 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| RED：196 小时实际出勤 + 4 小时调休仍返回 24 天 | 1 | 按月累计有效打卡分钟与 `请假/调休` 审批分钟后再按 480 分钟折算。 |

# Task Plan: 李凤皇本地查看显示 0(0) 排查

## Goal
- 修复加班/夜班统计单人查看和显示所有页面中，李凤皇历史统计数据实际出勤汇总显示 `0(0)` 或旧值 `192(24)` 的问题。
- 当历史统计明细 `actualAttendanceDays` 为空、为 `0` 或低于打卡与审批数据可推导出的实际出勤天数时，查询接口需兜底返回正确天数。

## Phases
1. `complete` 沿页面、接口、查询汇总链路定位 `0(0)` 来源。
2. `complete` 补 RED 测试覆盖单人查看和显示所有接口的旧明细兜底场景。
3. `complete` 在后端查询层实现实际出勤天数兜底计算和缓存。
4. `complete` 运行后端定向测试并更新文档记录。

## Current Evidence
- 前端 `0(0)` 来自接口返回的 `actualAttendanceDays=0`，前端只负责显示 `actualAttendanceDays * 8`。
- 后端此前只在“开始统计/单人统计”重算时写入实际出勤天数；查询旧明细时只对 `expectedAttendanceDays` 做了兜底，没有对 `actualAttendanceDays` 做兜底。
- 运行态进一步确认李凤皇 `2026-06` 的历史明细实际出勤已是旧正数 `24`，且审批数据中调休记录存在 `beginTime=2026-06-17 14:00:00`、`endTime=2026-06-17 18:00:00`，但 `duration/durationUnit` 为空。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 失败 2 个新增断言，单人查看和显示所有接口均返回 `0` 而不是 `25`。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，32 个测试，0 失败。
- RED：新增旧正数与空时长审批用例后，同一测试命令失败 2 个断言，分别返回 `24` 而不是 `25`。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，34 个测试，0 失败。
- 本地接口复核 `2026-06` 李凤皇：
  - `queryPageList` 返回 `actualAttendanceDays=25`；
  - `queryEmployeeMonthlyDetail` 返回 `actualAttendanceDays=25`；
  - `queryDailyDetailPageList` 返回每日行 `actualAttendanceDays=25`。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| RED：历史旧值 24 和空 duration 调休审批仍返回 24 天 | 1 | 查询兜底重算值与落库值取较大值；审批时长为空时按 `beginTime/endTime` 折算。 |

# Task Plan: 行政体系无加班审批时实际出勤不叠加加班汇总

## Goal
- 调整加班/夜班统计实际出勤口径：即使员工当月没有参与统计的加班审批，只要员工所属体系为行政体系，实际出勤时间也不叠加日级加班汇总。
- 加班小时汇总本身仍正常展示；该规则只影响 `actualAttendanceHours/actualAttendanceDays`。
- 查询已有统计明细和重新开始统计两条路径保持一致。

## Phases
1. `complete` 读取当前文档、统计服务和测试，确认行政体系判断在实际出勤计算中的落点。
2. `complete` 补 RED 测试覆盖“无加班审批 + 行政体系不叠加日级加班汇总”。
3. `complete` 实现最小后端改动，覆盖开始统计落库和查询兜底。
4. `complete` 运行定向测试和统计/审批组合回归，更新需求、开发、发现和进度文档。

## Current Assumption
- “员工对应的所属体系为行政”指 `hrm_employee.affiliation_system=1`。
- 行政体系不叠加日级加班汇总，不影响 `overtimeHours` 统计展示值。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployeeWithoutOvertimeApproval+queryPageList_shouldNotAddDailyOvertimeToActualAttendanceForAdministrativeEmployeeWithoutOvertimeApproval test` 失败 2 个断言，旧实现分别返回 `13.00` 小时和 `28` 天。
- GREEN：同一命令通过，2 个测试，0 失败。
- 回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，41 个测试，0 失败。
- 审批/统计组合回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，91 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 误用 `sh` 执行，出现 shell 语法错误 | 1 | 改用 `python3 ~/.codex/skills/planning-with-files/scripts/session-catchup.py "$(pwd)"` 重新执行。 |
| `rg` 搜索串含未转义反引号，触发 shell 命令替换 | 1 | 改用单引号包裹搜索表达式重新执行。 |

# Task Plan: 张明实际出勤多出 47.72 小时排查

## Goal
- 查明加班/夜班统计中张明实际出勤时间比应出勤时间多出 `47.72` 小时的来源。
- 区分这是现行统计口径导致的正常结果，还是加班小时、审批扣减或查询兜底重复计算。
- 给出可复核的数据证据：员工、月份、应出勤、加班、扣减、实际出勤公式。

## Phases
1. `complete` 读取当前文档口径、前后端显示规则和既有张明排查记录。
2. `complete` 定位后端实际出勤计算代码与查询响应字段。
3. `complete` 核对张明当月统计明细、加班小时来源和可扣减审批。
4. `complete` 汇总根因并更新排查记录。

## Current Evidence
- 该排查段落记录的是 2026-07-14 早期口径，已被“行政体系实际出勤只计审批加班”规则替代。
- 当前文档规则为：行政体系员工实际出勤小时 = `应出勤天数 * 8 + 本地审批加班小时 - 事假/病假/调休/年假扣减`。
- 张明 `hr_0003 / 2026-06` 为行政体系员工，统计明细汇总 `expected_attendance_days=23`、月度加班 `47.72` 小时。
- 张明同月本地审批只有两条 `外出`，没有本地审批加班，也没有 `事假`、`病假`、`调休`、`年假`，可扣减小时为 `0`。
- 当前正确实际出勤小时 = `23 * 8 + 0 - 0 = 184`，实际出勤天数 = `floor(184 / 8) = 23`。
- `47.72` 来自 `hrm_overtime_night_statistics_detail.overtime_hours` 的日级加班汇总；张明同月没有本地加班审批覆盖。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 误用 `sh` 执行，出现 shell 语法错误 | 1 | 改用 `python3 ~/.codex/skills/planning-with-files/scripts/session-catchup.py "$(pwd)"` 重新执行。 |

# Task Plan: 加班审批存在时实际出勤不叠加加班汇总

## Goal
- 调整加班/夜班统计实际出勤口径：当员工目标月份存在参与统计的本地加班审批记录时，实际出勤时间不再叠加日级加班汇总。
- 加班小时汇总本身仍按现有逻辑展示；仅实际出勤公式中的加班叠加项置为 `0`。
- `statisticsStatus=取消至统计` 的加班审批不触发该规则。

## Phases
1. `complete` 读取当前文档、统计服务和测试，确认加班审批影响实际出勤的入口。
2. `complete` 补 RED 测试覆盖“有加班审批时实际出勤不加 overtimeHours”；当时保留过一条无加班审批旧口径回归，已被后续行政体系例外覆盖。
3. `complete` 实现最小后端改动，覆盖开始统计落库和查询兜底。
4. `complete` 运行定向测试，更新需求、开发、发现和进度文档。

## Current Assumption
- “审批数据里有加班记录”指 `tbattendanceapprove` 中当前员工钉钉 `userId` 对应、目标月份内、参与统计且可识别为加班的审批记录。
- 加班审批只影响实际出勤公式是否叠加月度 `overtimeHours`，不隐藏、不清零加班统计展示值。

## Verification
- RED：当时定向命令失败 2 个断言，审批存在时旧实现仍返回 `2` 天；命令中包含的一条无审批旧口径回归已被后续行政体系例外替换。
- GREEN：同一命令通过，3 个测试，0 失败。
- 回归：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，41 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |

# Task Plan: 张明实际出勤 160 小时来源排查

## Goal
- 定位本地“加班/夜班统计”里张明实际出勤时间显示 `160` 小时的计算来源。
- 给出可核验的计算路径：接口字段、后端算法、原始打卡/审批数据如何折算成 `160`。
- 暂不修改代码，先完成根因和数据解释。

## Phases
1. `complete` 读取现有统计口径文档、计划记录和关键服务代码。
2. `complete` 定位张明对应员工、月份、接口返回的 `actualAttendanceDays`。
3. `complete` 反查实际出勤算法输入：打卡分钟、调休审批分钟、最终折算天数。
4. `complete` 汇总“160 = ?”的逐步计算证据并回复用户。

## Current Hypotheses
- 前端实际出勤小时显示来自 `actualAttendanceDays * 8`。
- `160` 小时对应 `actualAttendanceDays=20` 天。
- 已确认后端只基于本地打卡有效分钟和“调休”审批折算出 `20` 天；张明 `2026-06` 的“外出”审批不参与当前实际出勤折算。

## Verification
- SQL 复算 `hr_0003.hrm_attendance_clock`：张明 `2026-06` 有效出勤分钟合计 `9829`。
- SQL 查询 `hr_0003.tbattendanceapprove`：张明 `2026-06` 无 `调休` 审批，只有两条 `外出` 审批。
- SQL 查询 `hr_0003.hrm_overtime_night_statistics_detail`：张明 `2026-06` 落库 `actual_attendance_days=20`。

# Task Plan: 实际出勤扣减口径与备注列改造

## Goal
- 将加班/夜班统计中的实际出勤时间改为以应出勤时间为基准，只在存在 `事假`、`病假`、`调休` 审批时扣减；其他审批类型不扣减。
- 在实际出勤时间统计矩阵中，在“小时(天数)汇总”后增加“应出勤时间”列并展示数据。
- 在实际出勤时间统计矩阵最后增加“备注”列；发生扣减时写明扣减原因。

## Phases
1. `complete` 确认当前后端实际出勤算法、前端矩阵构建和现有测试。
2. `complete` 新增后端 RED 测试：只扣事假/病假/调休，不扣外出；返回扣减备注。
3. `complete` 新增前端 RED 测试：实际出勤矩阵列顺序为姓名、小时(天数)汇总、应出勤时间、日列、备注。
4. `complete` 实现后端口径与 VO 字段透出。
5. `complete` 实现前端矩阵字段与表格列。
6. `complete` 运行后端/前端定向测试和必要构建。
7. `complete` 更新前后端需求/开发文档与排查记录。

## Current Assumptions
- 当前口径已在 2026-07-13 追加月度加班小时，并在 2026-07-14 增加行政体系加班来源分流：行政体系员工实际出勤只加本地审批加班，非行政体系仍按月度加班汇总计入。`actualAttendanceDays` 继续按小时除以 `8` 向下取整以兼容旧字段。
- 可扣减审批仅识别 `tagName/subType` 包含 `事假`、`病假`、`调休` 的本地 `tbattendanceapprove` 记录。
- 备注列展示扣减类型和小时，例如 `扣除：事假4.00小时、病假8.00小时`；无扣减时为空。
- 外出、出差、加班、补卡等审批不进入扣减，也不写备注。

## Verification
- RED：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 曾失败，缺少 `actualAttendanceRemark` 字段并且旧口径断言仍期望“调休补足”。
- RED：`node tests/overtime-night-utils.test.mjs` 曾失败，缺少 `expectedAttendanceSummaryValue`。
- RED：`node tests/attendance-display-columns.test.mjs` 曾失败，实际出勤矩阵缺少“应出勤时间”和“备注”列。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，36 个测试，0 失败。

# Task Plan: 加班/夜班统计重算锁等待治理

## Goal
- 处理“开始统计/单人统计”出现 `Lock wait timeout exceeded; try restarting transaction` 的问题。
- 缩短 `hrm_overtime_night_statistics_detail` 旧明细删除锁持有时间。
- 保持此前实际出勤规则不变：行政体系员工或当月存在参与统计的加班审批时，实际出勤不叠加日级加班汇总。

## Phases
1. `complete` 复核需求/开发文档和当前加班/夜班统计实现。
2. `complete` 定位锁等待风险点：长事务、删除顺序、删除方法实现和删除索引。
3. `complete` 补/确认回归测试覆盖先计算后删除、bulk delete 和入口不再长事务。
4. `complete` 实现短写事务、bulk delete 和索引脚本。
5. `complete` 运行定向与完整加班/夜班统计测试。
6. `complete` 更新需求、开发、发现和进度文档。

## Current Evidence
- 原风险：`startStatistics(...)` / `startStatisticsForEmployee(...)` 方法级 `@Transactional` 会让删除旧统计明细后的锁一直持有到方法返回。
- 原风险：Spring Data 派生 delete 容易逐实体删除，慢于 JPQL bulk delete。
- 数据库风险：按 `work_date between` 删除整月旧明细时，如果缺少 `work_date` 左前缀索引，会扩大扫描和锁等待。

## Verification
- 定向：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#overtimeNightStatisticsEntryPoints_shouldNotUseLongRunningTransactions+startStatistics_shouldCalculateRowsBeforeDeletingExistingRowsToReduceLockTime+overtimeNightDetailRepositoryDeleteMethods_shouldUseBulkModifyingQueries test` 通过，3 个测试，0 失败。
- 完整：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，44 个测试，0 失败。
- 审批/统计组合：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，94 个测试，0 失败。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| `session-catchup.py` 误用 `sh` 执行，出现 shell 语法错误 | 1 | 改用 `python3 /Users/jiangyongming/.codex/skills/planning-with-files/scripts/session-catchup.py /Users/jiangyongming/Project/hr/hainan` 重新执行。 |
- GREEN：`node tests/overtime-night-utils.test.mjs` 通过。
- GREEN：`node tests/attendance-display-columns.test.mjs` 通过。
- GREEN：`npm run build` 通过，保留既有 `::v-deep` 与 chunk size warning。

# Task Plan: 闫倩实际出勤扣两天与备注缺失排查

## Goal
- 查清 `hr_0003` 闫倩实际出勤时间为什么少了两天。
- 查清扣减原因为什么没有显示在实际出勤矩阵的备注列。
- 如确认是代码缺陷，补回归测试并修复；如是数据或发布状态问题，给出可核验证据。

## Phases
1. `complete` 读取当前加班/夜班实际出勤口径文档、代码入口和计划记录。
2. `complete` 定位闫倩员工 ID、钉钉 userId、统计月份、应出勤/实际出勤落库值。
3. `complete` 查询 `tbattendanceapprove` 中可扣减审批，复算扣减小时和应显示备注。
4. `complete` 对比后端接口实际返回的 `actualAttendanceHours/actualAttendanceRemark`。
5. `complete` 检查前端矩阵是否读取并展示备注字段，确认是否为旧静态包或接口字段缺失。
6. `complete` 根据根因修复代码或给出数据/发布结论，并运行验证。

## Current Hypotheses
- 若扣两天是正确扣减，则闫倩当月应存在累计约 `16` 小时的 `事假/病假/调休` 审批。
- 备注列不显示可能来自：
  - 后端运行实例不是最新代码，没有返回 `actualAttendanceRemark`；
  - 前端页面不是最新构建，未渲染备注列或未读取字段；
  - 后端月度保存只落了 `actualAttendanceDays`，但查询路径没有重新计算备注。

## Current Evidence
- 闫倩 `employee_id=1831601326890434568`，钉钉 `userId=30352228121210270`，`2026-06` 明细落库 `expected_attendance_days=23`、`actual_attendance_days=21`。
- `2026-06` 命中 3 条可扣减 `请假/调休` 审批，时长字段为空，按 `beginTime/endTime` 折算为 `4.28 + 10.00 + 0.50 = 14.78` 小时。
- 新口径复算：应出勤 `23 * 8 = 184` 小时；实际出勤 `184 - 14.78 = 169.22` 小时；兼容旧天数字段 `floor(169.22 / 8) = 21` 天。
- 本地后端 `queryPageList`、`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 均返回 `actualAttendanceHours=169.22` 和 `actualAttendanceRemark=扣除：调休14.78小时`。
- 前端单人查看弹窗在 `Index.vue#buildDetailMatrixRows` 展开 `dailyDetails` 时漏传月度级 `actualAttendanceHours/actualAttendanceRemark`，导致单人查看仍按 `actualAttendanceDays * 8 = 168` 显示，并且备注为空。

## Verification
- RED：`node tests/attendance-display-columns.test.mjs` 失败，提示单人查看未把实际出勤小时和备注传入每日矩阵行。
- GREEN：`node tests/attendance-display-columns.test.mjs` 通过。
- GREEN：`node tests/overtime-night-utils.test.mjs` 通过。
- GREEN：`npm run build` 通过，保留既有 `::v-deep` 与 chunk size warning。
- GREEN：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，36 个测试，0 失败。
# Task Plan: 薪资核算失败集中提示

## Goal
- 定位 `getOrCreateRecordAndApplyAttendance` 约 2098 行“空接口”报错的根因。
- 扫描薪资核算接口主链路，找出同类空值/缺配置/前置数据不完整等会导致执行失败的风险。
- 将用户能理解的错误信息尽量一次性汇总，并改为在薪资核算进度窗口展示，避免弹出几秒后消失的提示和反复执行才发现下一个问题。

## Phases
1. `complete` 复核项目文档、前端文档、既有计划记录和薪资核算进度设计。
2. `complete` 定位 `getOrCreateRecordAndApplyAttendance` 报错位置、调用链和输入数据来源，确认根因。
3. `complete` 扫描薪资核算接口中类似风险点，归类为可提前检查的问题和运行期异常问题。
4. `complete` 先补 RED 测试，覆盖集中校验结果、进度失败消息和前端进度窗口展示。
5. `complete` 实现后端集中预检/异常归一化和前端进度窗口错误展示。
6. `complete` 运行定向验证并更新项目需求/开发文档。

## Current Assumptions
- “空接口”按后端运行时空指针/空返回导致接口失败理解，需通过代码和测试定位实际对象。
- 不改变既有核算接口路径；优先复用已有薪资核算进度查询接口。
- 用户侧提示使用普通业务语言，例如“该员工缺少考勤汇总/社保记录/部门信息”，不直接暴露 Java 异常、空指针、Mapper 或字段名。
- 对可提前发现的问题，后端应先汇总后失败；对核算过程中才暴露的问题，也要写入进度状态供前端进度窗口显示。

## Verification
- RED 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 先失败于缺少 `collectComputePrerequisiteErrors(...)`。
- RED 前端：`node tests/compute-progress-utils.test.mjs` 先失败于缺少 `normalizeSalaryComputeProgress`；`node tests/salary-start-compute-dialog.test.mjs` 先失败于进度窗口未渲染 `computeProgressErrors`。
- GREEN 后端：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，47 个测试。
- GREEN 前端：`node tests/compute-progress-utils.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/salary-compute-scope-utils.test.mjs` 通过。
- 前端构建：`npm run build` 通过，保留既有 `::v-deep` 过时警告和 chunk size warning。
- 空白检查：后端相关文件 `git diff --check -- ...` 通过；前端目标文件因当前顶层仓库未跟踪，已用尾随空白扫描补充检查且无输出。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
# Task Plan: 全员全勤奖漏发排查

## Goal
- 查清核算薪资中张明等员工未生成 `40102 / 全勤奖` 的根因。
- 以当前租户和最近薪资月份为准，检查所有计薪员工的全勤资格、考勤异常、加班/夜班统计应出勤/应计出勤和已生成工资项。
- 如确认是代码缺陷，先补失败测试再做最小修复；如是数据缺失或业务规则命中，给出全员影响清单和处理建议。

## Phases
1. `complete` 复核项目文档、历史全勤奖规则和当前工作区状态。
2. `complete` 定位全勤奖计算代码、工资项编码和依赖数据来源。
3. `complete` 查询张明及全员薪资数据，分类“应发未发 / 规则扣除 / 数据不足”。
4. `complete` 形成单一根因假设并用全员样本验证。
5. `complete` 视根因补测试、修复代码或输出数据修复建议，并更新项目文档。

## Current Assumptions
- “张明”优先按近期记录中的 `hr_0003 / 2026-06` 行政体系员工理解；若数据库存在多个张明，必须用员工 ID、工号、手机号和薪资月份区分。
- “有些员工没有全勤奖”指已生成的薪资明细缺少工资项 `40102` 或金额为 `0`。
- 本轮先做只读根因排查，不直接修改薪资数据。

## Verification
- RED：`mvn -Dtest=SalaryMonthRecordServiceNewTest#shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected test` 失败，旧代码返回 `false`。
- GREEN：`mvn -Dtest=SalaryMonthRecordServiceNewTest#shouldFallbackFullAttendanceByAccruedDays_shouldUseAccruedDaysWhenAccruedReachesExpected+shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected test` 通过，2 个测试。
- 定向：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过，51 个测试。
- 薪资组合回归：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，73 个测试。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 查询 `hrm_salary_month_record.employee_count` 报字段不存在 | 1 | 读取表结构后改用实际字段 `num`，并以 `hrm_salary_month_emp_record` 明细统计员工。 |
| 查询假期抵扣时误用不存在表 `hrm_holidays` | 1 | 读取 mapper 后确认正确关联表为 `hrm_remaining_vacation`。 |

# Task Plan: 数据配置类型和值可自定义

## Goal
- `hr_web` 的数据配置页面不再固定“工段/车间”下拉，类型和值都由用户直接输入。
- 列表展示用户录入的类型文本和值文本，保存时同步落库。
- 后端保持现有字典表兼容，不新增表结构。

## Phases
1. `complete` 读取数据配置页面、接口和字典实现，确认固定类型逻辑的位置。
2. `complete` 补 RED 测试锁定类型和值输入、payload 与后端回填行为。
3. `complete` 修改前后端实现，取消固定工段/车间下拉。
4. `complete` 运行聚焦测试与编译验证。
5. `complete` 更新需求、开发、progress 与 findings 文档。

## Verification
- `node /Users/jiangyongming/Project/hr/hr_web/tests/data-config-api.test.mjs`
- `node /Users/jiangyongming/Project/hr/hr_web/tests/data-config-page.test.mjs`
- `mvn -Dtest=tbDictDataServiceImplTest test`

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| 前端测试最初按旧实现断言 `AddType` 两参调用 | 1 | 改为断言仅提交 payload，并同步更新页面契约。 |
| 后端测试最初按 `TreeNode.getType()` 失败 | 1 | 为 `TreeNode` 增加 `type` 字段，并从 `tbdictdata.sn` 回填。 |

# Task Plan: 农谷 2026-06 高温补贴与薪资档案补齐

## Goal
- 以 `/Users/jiangyongming/Downloads/薪资 (1).xlsx` 为基准人员范围，对比 `/Users/jiangyongming/Desktop/农谷导入数据/薪资/薪资-手工.xlsx` 的 2026 年 6 月工资表。
- 对手工表中已录入但系统/导出基准未录入的高温补贴，将高温补贴金额补入考勤汇总表 `hrm_produce_attendance` 的其它补贴字段。
- 对基本工资、岗位工资、职务工资不一致的员工，将手工表对应金额补入薪资档案三项。
- 全程使用姓名 + 电话定位员工；最终落库仍使用员工 ID，避免同名员工串数据。

## Phases
1. `complete` 复核表结构、员工手机号字段、考勤汇总字段和薪资档案字段。
2. `complete` 解析两个 Excel 和员工档案，生成可执行数据修正预览。
3. `complete` 执行数据库修正或生成待执行 SQL，并排除无法按姓名 + 电话唯一定位的员工。
4. `complete` 复核写后金额、异常清单和影响范围。
5. `complete` 更新项目需求/开发文档、findings/progress。

## Current Assumptions
- 目标租户沿用近期农谷数据处理的 `hr_0003`。
- 目标月份为 2026-06。
- “考勤汇总表”指 `hrm_produce_attendance`；“其它补贴”字段需以表结构确认为准。
- “薪资档案”指员工薪资档案/工资信息表，具体表和字段需通过项目代码与数据库结构确认。

## Verification
- 预览报告：`docs/reports/2026-07-20-nonggu-salary-attendance-supplement-preview.md`。
- 执行 SQL：`docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement.sql`。
- 回滚 SQL：`docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement_rollback.sql`。
- 写后只读核验：20 个相关考勤汇总行（19 个本轮更新 + 赵聪已达目标）`other_subsidies=100`，异常数 0。
- 写后只读核验：6 条薪资档案目标项全部等于手工表目标值，异常数 0。
- 未处理：吴镜平在手工表 `6月` sheet 无同名行。

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
