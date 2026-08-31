# Development Notes

## Architecture Overview
- 接口入口：`src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`
- 核心服务：`src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`
- 数据模型：`src/main/java/com/tianye/hrsystem/model/tbplanlist.java`

## Key Modules and Responsibilities
- `WorkPlanListController#saveAll`：接收前端排班数组，解析 `shiftType/customStart/customEnd`，创建排班提交任务。
- `WorkPlanListController#querySubmitProgress`：返回排班提交进度、重试次数与按行中文错误。
- `WorkPlanListController#queryCustomShiftList`：返回本地自定义班次下拉选项，供前端“选已有班次 + 手输时间”混合交互使用。
- `WorkPlanServiceImpl#AddAll`：创建异步提交任务，不再同步阻塞等待钉钉返回。
- `WorkPlanServiceImpl#submitPlans`：按 `shiftType` 拆分；标准班次继续走钉钉排班，自定义班次只解析本地可复用班次并落库。
- `WorkPlanCustomShiftResolver`：负责 `HH:mm` 归一化、跨天判定，以及本地单段班次时间匹配；当前实现仍要求开始/结束时间都存在，后续 Excel 导入“无固定下班时间”需扩展为空结束时间。
- `HrmWorkPlanCustomShift`：本地自定义班次事实表，只承载手工自定义时间，不与钉钉同步班次混用。

## Recent Changes and Decisions
- 个人信息页与顶栏调整（2026-08-30）：
  - 本轮只改前端 `hr_web`，后端 `hainan` 无代码修改；复用既有 `POST /hrsystem/changePassword` 与 `POST /hrsystem/logout`。
  - 新增 `src/views/hrm/profile/Index.vue`：基本信息（姓名/登录账号/所属公司/所属部门/账号角色）读取 Vuex `user_info` 登录快照；"安全设置"提供"修改密码"（`store.commit('changRestPassword', true)` 复用全局 `RestPassword` 弹窗）与"退出登录"。
  - 退出登录链路：`api/login/user.js` 新增 `authLogOut(token)` → `POST /hrsystem/logout?token=...`（后端按请求参数取 token，jti 写 Redis 黑名单），随后前端清除 `token`/`user_info`、重置 store、跳 `/login`；吊销接口失败时本地清理照常执行，保证可退出。
  - 路由与权限：`router/config.js` 在 `/hrm` 下新增 `path: 'profile'`，`meta` 为 `{ parentName: 'hrm', name: '个人信息', hidden: true, checkPath: '/hrm/profile' }`；`utils/permission.js` 的 `DEFAULT_ALLOWED_PATHS` 增加 `/hrm/profile`。`hidden` 保证不进侧边菜单；`isRoutePermitted` 因 `hidden && checkPath` 不走早退分支，再经 `checkPath` 命中白名单，故无 `menuTree` 的账号也可访问。
  - `Layout.vue` 的顶栏 `tabsData` 过滤条件增加 `&& !x.meta.hidden`：因 profile 的 `checkPath` 命中白名单后 `isRoutePermitted` 返回 true，若不过滤 hidden 会把"个人信息"渲染成可见顶栏 tab；对有/无 `menuTree` 的账号行为一致。
  - `TopHeader.vue`：顶栏左侧 `.title` 移除用户名文字只留 Grid 图标；下拉菜单替换为单项"个人信息"；删除不再使用的 `openRestPassword`/`logOut` 与 `ElMessage` 导入。
  - `RestPassword.vue`：非强制改密成功后提示"密码修改成功，请重新登录"并清除本地凭证后 `window.location.href = '/#/login'`（与 `requset.js` 会话失效清理同口径）；强制改密分支保持原逻辑。
  - 验证：`npm run build` 通过，"个人信息"相关代码进入 `html/assets` 产物。

- 排班管理未找到员工对应的考勤用户修复（2026-08-25）：
  - 根因：新员工添加成功后，系统没有自动在 `tbattendanceuser` 表中创建对应的考勤用户记录。`tbattendanceuser` 表是通过 `AttendanceUserManager.GetAndSave()` 从钉钉同步考勤用户时创建的，这个同步任务每月1日 00:05 执行一次。当在排班管理中点击日历矩阵单元格时，会调用 `queryEmployeeDayAssignments`，该方法内部调用 `resolveAttendanceUserByEmployeeId`，它会查找 `tbattendanceuser` 表中的考勤用户，如果找不到就会抛出"未找到员工对应的考勤用户"异常。
  - 修复方案：修改 `WorkPlanServiceImpl#resolveAttendanceUserByEmployeeId` 方法，当找不到考勤用户时，尝试从员工表获取钉钉用户ID并创建考勤用户记录；如果没有钉钉用户ID，则使用员工ID作为临时考勤用户ID，确保新员工也能在排班管理中使用。
  - 验证：`mvn -Dtest=WorkPlanServiceImplTest test` 通过，`mvn compile` 通过。

- 排班矩阵车间自由输入与岗位时间组统一（2026-08-25）：
  - `AddOrEdit.vue` 和 `Scheduling.vue` 都新增了 `workshopName` 文本输入；提交和回显都直接使用该文本，`tbplanlist.workshop_name` 作为事实字段持久化。
  - `work-plan-utils.js` 的行归一化、提交 payload、矩阵合并和 tooltip 逻辑都把 `workshopName` 纳入，避免不同车间的同产品同岗位记录被折叠。
  - `Scheduling.vue` 的修改排班继续沿用 `dayShiftAssignments[]` 多分配模型，每个岗位分配各自维护开始/结束时间、白/夜班、连班和车间，和添加排班保持同一模型。
  - `resolveWorkshopName(groupId)` 只在历史空值回显时兜底，不再作为新增/修改排班的来源。
  - 验证：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test`、`mvn -DskipTests compile`、`npm run build` 通过。

- 应用环境配置文件乱码修复（2026-08-24）：
  - 清理 `application-dev.properties`、`application-prod.properties` 和 `application-temp.properties` 中 `server.servlet.encoding.*` 属性尾部的乱码注释，保留属性值不变。
  - 修正开发环境微信小程序 appid 占位文本，使其与生产环境保持一致。
  - 三个配置文件继续使用 UTF-8 编码，未改变数据源、Profile 或运行时配置行为。
  - 验证方式：检查源码和 `target/classes` 文件编码、扫描乱码模式，重新打包并检查 JAR 内 `BOOT-INF/classes/application-*.properties`，最后执行 `git diff --check`。

- 添加排班与社保方案列表分页契约复核（2026-08-24）：
  - `WorkPlanListController#getData(...)` 接收 `pageSize/pageNum`，缺省时进入默认页大小；本轮排查确认添加排班当天加载少数据的根因在前端未映射分页字段，后端保存和查询 SQL 不改。
  - `WorkPlanListController#loadIsLast(...)` 同样接收 `pageSize/pageNum`，前端该路径已正确映射，因此“加载上次排班表”不属于本轮截断点。
  - `HrmInsuranceSchemeController#index(...)` 接收 `PageEntity`，`PageEntity` 默认 `limit=15`，`pageType=0` 时按不分页上限返回；社保方案管理列表显示 15 条的根因是前端提交 `{}` 且页面无分页控件。
  - 代码修复位于 `hr_web`：`getWorkPlanData(...)` 补 `pageNum/pageSize`，`getInsurance(...)` 默认提交 `pageType=0`。后端本轮无源码修改。

- 添加排班人员缺失修复（2026-08-24）：
  - `WorkPlanServiceImpl#loadUsersFromLocalSnapshot()` 改为调用 `hrmEmployeeRepository#findAllByIsDelAndEntryStatusIn(0, [1,3,4])`，展示人员直接来自 `hrm_employee`，覆盖在职、待离职和离职员工。
- 展示层过滤已删除、待入职和无姓名员工；人员 ID 按员工表 `dingtalk_user_id`、`tbattendanceuser.userId`、员工 `employeeId` 依次解析，并按解析后的用户 ID 去重；返回 `UserObject.employeeId` 供前端和排班矩阵识别员工。
  - 员工没有 `tbattendanceuser` 考勤组时仍保留在下拉，`UserObject.groupId` 统一返回空字符串；因此陈明成这类没有快照/考勤组的员工不会再因展示快照缺失而搜不到。
  - 展示缓存 key 升级为 `*_getAllUsers_display_v3`，`clearUserCache(...)` 同时清理 v2 和旧版 key，避免发布后继续命中历史漏人缓存。
  - `getUsers(companyId)` 的标准班次提交链路保持原有钉钉缓存和调组判断；排班调用继续使用前端提交的钉钉用户 ID，不要求该人员先存在 `tbattendanceuser` 展示快照。
  - 回归：`mvn -Dtest=WorkPlanServiceImplTest,HrmAttendanceDataControllerTest test` 通过 62 项，`mvn -DskipTests compile` 通过。

- 排班管理矩阵多产品多岗位修改排班实现（2026-08-24）：
  - 设计文档见 `docs/plans/2026-08-24-workplan-matrix-multi-assignment-design.md`。
  - 当前 `WorkPlanServiceImpl#queryEmployeeDayShift(...)` 通过 `findEmployeeDayPlans(...)` 找到员工当天本地排班后调用 `pickPreferredEmployeePlan(...)`，只返回一条首选记录；该接口可继续作为旧单排班视图，但不适合作为多产品、多岗位矩阵编辑数据源。
  - 已新增 `POST /workPlan/queryEmployeeDayAssignments` 和 `POST /workPlan/saveEmployeeDayAssignments`。新接口将修改排班的聚合根定义为 `employeeId + workDate`，其下包含 `assignments[]`；每条 assignment 独立保存生产产品、岗位、班次类型、标准班次 ID、自定义开始/结束时间、白夜班、是否连班和休息类型。
  - 用户已确认同一天不同产品或岗位可以使用不同工作时间，因此前端不再用单个 `dayShiftProductContext` 表示整个单元格；`Scheduling.vue` 已改为 `dayShiftAssignments[]` 多分配行，每行都有自己的产品上下文和班次时间。
  - 查询接口返回目标员工当天所有原始本地排班；保存接口采用完整替换语义，先从当天所有旧 `tbplanlist.UserID` 中移除目标员工，再逐条保存提交的分配明细。
  - 保存时要保留多人共享旧行中的其他员工：旧行 `UserID` 只有目标员工则删除整行，包含其他员工则只移除目标员工并保存剩余 `UserID`。
  - 第一期继续复用 `tbplanlist.ProductName/LinkName/UserID` 扁平事实表，assignment 保存时展开为多条 `tbplanlist`；休息/调休和工作 assignment 互斥，休息/调休保存为一条 `shift_type=rest` 记录并清空产品岗位。
  - 后端测试覆盖完整查询、多产品多岗位独立时间、替换保存、共享行保留其他员工、休息/调休互斥和控制器委托；前端测试覆盖新 API、完整分配集合归一、保存请求构造和排班矩阵页面源码契约。
  - 验证：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过 69 项；`mvn -DskipTests compile` 通过。前端 `node tests/work-plan-api.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-records-page.test.mjs` 与 `npm run build` 通过。

- 排班添加与单元格编辑产品模式（2026-08-23）：
  - 本轮后端 `hainan` 不改代码，继续保持 `/workPlan/saveAll`、`WorkPlanListController#saveAll`、`WorkPlanServiceImpl#submitPlans` 和 `tbplanlist` 事实表契约不变。
  - 前端 `hr_web` 的添加排班不再新增独立 `生产产品区域`；新增行前选择标准/自定义产品模式，每条排班主行独立维护产品、岗位和人员，并在保存前按岗位子行展开为扁平数组。排班管理单元格编辑仍有产品区域，用于单日单员工修改。
  - 标准产品和自定义产品最终都只提交既有 `productName/linkName/userId` 字段。
  - 排班管理单元格编辑提交给 `/workPlan/saveAll` 时不携带旧 `planId`，让 `persistResolvedAssignments(...)` 通过员工 `UserID + workDate` 复用/清理当天已有记录，避免只更新首条记录后留下旧岗位排班。
  - 自定义产品不新增后端产品配置记录；持久化只发生在排班事实记录的 `ProductName/LinkName/UserID`。
  - 休息/调休继续使用 `shift_type=rest` 与 `rest_shift_type=rest/adjust`；前端可清空 `productName/linkName`，但单元格编辑必须保留目标员工 `UserID` 才能落到具体员工。
  - 车间字段已恢复为自由文本输入并贯穿添加/修改排班；后端 `resolveWorkshopName(groupId)` 仅在历史空值回显时兜底，不再作为新增/修改排班来源。
  - 前端验证：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/work-plan-time-picker.test.mjs`、`node tests/clock-overview-utils.test.mjs`、`node tests/clock-overview-source.test.mjs`、`node tests/workplan-product-management.test.mjs` 和 `npm run build` 通过。

- 添加排班岗位多行与全员补充选人（2026-08-23）：
  - 本轮只修改前端 `hr_web`，后端 `hainan` 的 `/workPlan/saveAll`、`WorkPlanListController#saveAll` 和 `WorkPlanServiceImpl#submitPlans` 契约保持不变。
  - 前端添加排班把岗位从单个多选框改为 `positionRows` 子行；提交前仍展开为多条扁平保存项，每条保存项使用一个岗位名称写入 `linkName`，人员逗号拼接写入 `userId`。
  - 排班事实表继续保持扁平，不新增产品/岗位/人员关系到 `tbplanlist`；生产产品配置模块仍只作为添加排班候选来源。
  - 前端验证：`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/work-plan-scheduling-records.test.mjs`、`node tests/work-plan-api.test.mjs`、`node tests/workplan-product-management.test.mjs` 和 `npm run build` 通过。

- 社保详情一键设置保险金额（2026-08-22）：
  - 后端新增 `UpdateInsuranceSalaryBasicAmountBO` 和 `POST /hrmInsuranceMonthRecord/updateSalaryBasicInsuranceAmount`，入参为 `iRecordId`、可选 `iEmpRecordIds`、`includeSalaryBasicInsuranceAmount`。`iEmpRecordIds` 为空时更新当前月报全部参保员工，非空时只更新选中员工。
  - `HrmInsuranceMonthEmpRecord` 增加 `includeSalaryBasicInsuranceAmount`，映射 `hrm_insurance_month_emp_record.include_salary_basic_insurance_amount`，默认 `0`。详情员工列表 `QueryInsurancePageListVO` 同步返回该字段。
  - `HrmInsuranceMonthRecordService#refreshSalaryBasicInsuranceAmount(...)` 先调用 `HrmInsuranceMonthEmpProjectRecordService#queryProjectCount(...)` 重算月度项目基础金额，再在 `includeSalaryBasicInsuranceAmount=1` 时直接追加基本工资设置金额：个人社保加 `longTermCareInsuranceAmount`，公司社保加 `largeMedicalInsuranceAmount`。该固定追加不再判断 `type=12 / 医疗长期护理保险` 是否启用。
  - 医疗长期护理项目的 `is_enabled` 只影响员工月度参保项目基础金额是否进入 `queryProjectCount(...)`；一键设置/取消保险金额的 `3/15` 固定追加只看员工级累计开关。
  - `HrmInsuranceMonthEmpRecordService#updateInsuranceProject(...)` 在员工记录已设置累计时，会在编辑参保方案后重新调用金额刷新逻辑，避免项目禁用或金额变化后旧累计金额残留。
  - 2026-08-22 修复“一键设置保险金额”提示“社保记录不能为空”和点击后金额不变：根因是前端提交 `iRecordId/iEmpRecordIds`，而 Jackson 对 Lombok 生成的 `getIRecordId/getIEmpRecordIds` 默认识别为 `irecordId/iempRecordIds`。后端 `UpdateInsuranceSalaryBasicAmountBO` 用 `@JsonProperty` 固定既有对外名并用 `@JsonAlias` 兼容旧提交；前端 `InsuranceDetail.vue` 也改为提交 `irecordId/iempRecordIds`，保证旧后端命名规则下也能定位月报和选中员工记录。
  - 2026-08-22 继续排查用户反馈“还是一样”：`hbadmin` 登录实际租户为 `companyId=0003`，不是此前只读核验的 `hr_0001`。`hr_0003` 的 2026-06 社保月报 `2079207580227342338` 存在 88 条历史脏数据：员工记录金额已等于月度项目基础金额加 `3/15`，但 `include_salary_basic_insurance_amount=0`。因此点击“一键设置”只把开关写成 `1`，金额看起来不变。
  - 已用 SQL 将这 88 条 `hr_0003` / 2026-06 记录修正回未累计基础金额：个人社保合计从 `42149.10` 回到 `41885.10`，公司社保合计从 `105230.05` 回到 `103910.05`；随后直接调用 `/updateSalaryBasicInsuranceAmount` 验证设置会更新 88 人并把合计恢复到 `42149.10/105230.05`，取消会再次回到 `41885.10/103910.05`。详情行、详情合计和社保管理列表接口均同步读取到变化。
  - 2026-08-22 按用户最新要求调整：一键设置/取消保险金额必须不管 `医疗长期护理保险` 是否启用都能改变金额。`cdadmin/companyId=0002` 对应 `hr_0002`，2026-03 月报 `2091088562492608513` 的 3 名员工使用方案 `1831653398776143873 / 成都（一）`，该方案和员工月度项目 `type=12` 均为 `is_enabled=0`；调整后设置仍会在基础金额 `477.15/1160.76` 上追加 `3.00/15.00`。
  - 已通过 `127.0.0.1:8081/api` 用 `cdadmin` token 验证：取消接口返回 `data=3` 后 3 人回到基础金额 `477.15/1160.76`、合计 `1431.45/3482.28`；设置接口返回 `data=3` 后 3 人变为 `480.15/1175.76`、合计 `1440.45/3527.28`。详情合计、详情员工列表和社保管理列表接口均同步返回新金额。
  - 新增迁移脚本 `docs/sql/2026-08-22_hrm_insurance_month_emp_include_salary_basic_insurance_amount.sql`，面向 `hr_0001` 至 `hr_0005` 幂等补齐员工月度社保记录累计开关字段，脚本显式 `SET NAMES utf8mb4` 并刷新中文注释。
  - 2026-08-22 已在 dev MySQL `153.0.237.98` 执行迁移脚本；`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 的 `hrm_insurance_month_emp_record.include_salary_basic_insurance_amount` 均存在，类型 `tinyint`，`NOT NULL`，默认值 `0`，注释为 `是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是`。
  - 前端 `hr_web/src/views/hrm/insurance-scheme/InsuranceDetail.vue` 在“高级筛选”后增加全员“一键设置保险金额/一键取消保险金额”，并在表格按钮区增加选中员工版本；操作成功后刷新详情合计和员工行。
  - 验证：`mvn -Dtest=HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest test` 通过，22 个测试 0 failures/errors；前端 `node tests/insurance-detail-amount-actions.test.mjs`、`node tests/insurance-scheme-projects.test.mjs`、`node tests/insurance-advanced-filter.test.mjs`、`node tests/insurance-scheme-usage-tooltip.test.mjs` 通过；`mvn -DskipTests compile` 与 `npm run build` 通过。

- 社保方案医疗长期护理保险（2026-08-22）：
  - 前端 `hr_web/src/views/manage/insurance-scheme/add.js` 的默认社保项目在 `type=5 / 生育保险` 后新增 `type=12 / 医疗长期护理保险`，默认 `isEnabled=1`。
  - 前端 `Add.vue` 与 `Edit.vue` 的社保表格新增 `状态` 列，仅 `医疗长期护理保险` 行展示启用/禁用按钮；按钮直接切换行内 `isEnabled`，并随原社保方案保存 payload 一起提交。
  - 后端 `HrmInsuranceProject`、`HrmInsuranceMonthEmpProjectRecord`、`InsuranceSchemeDto.HrmInsuranceProjectBO`、`AddInsuranceSchemeBO.HrmInsuranceProjectBO` 与 `EmpInsuranceByIdVO.HrmInsuranceProjectBO` 新增 `isEnabled` 字段，旧数据为空时按启用处理。
  - `HrmInsuranceSchemeService#queryInsuranceSchemeById(...)` 会在旧方案缺少 `type=12` 时补出默认启用的 `医疗长期护理保险` 行；已保存的 `isEnabled=0` 会原样回显。
  - `HrmInsuranceMonthEmpRecordService#updateInsuranceProject(...)` 对月度参保编辑中 `projectId=null,type=12` 的补行会先创建真实 `hrm_insurance_project`，再写入 `hrm_insurance_month_emp_project_record`，避免旧方案未先保存时月度编辑失败。
  - `HrmInsuranceSchemeMapper.xml`、`HrmInsuranceSechemeMapper.xml` 和 `HrmInsuranceMonthEmpProjectRecordMapper.xml` 的社保/公积金合计均增加 `coalesce(is_enabled, 1) = 1` 过滤；个人社保和公司社保合计已删除对 `hrm_salary_basic.long_term_care_insurance_amount`、`large_medical_insurance_amount` 的自动累加。
  - 新增迁移脚本 `docs/sql/2026-08-22_hrm_insurance_project_is_enabled.sql`，面向 `hr_0001` 至 `hr_0005` 幂等补齐方案项目和月度员工项目的启用状态列。脚本显式 `SET NAMES utf8mb4`，字段已存在时也会 `MODIFY COLUMN` 刷新 `tinyint not null default 1` 定义和中文注释，避免不同客户端字符集执行后注释乱码。
  - 2026-08-22 已在 dev MySQL `153.0.237.98` 执行迁移脚本；`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 的 `hrm_insurance_project.is_enabled` 与 `hrm_insurance_month_emp_project_record.is_enabled` 共 10 个字段均存在，类型 `tinyint`，`NOT NULL`，默认值 `1`，注释为 `是否启用：0禁用 1启用`。
  - 验证：`mvn -Dtest=HrmInsuranceSchemeServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordMapperXmlTest test` 通过，18 个测试 0 failures/errors，其中 `HrmInsuranceSchemeServiceTest#saveInsuranceProject_shouldPersistMedicalLongTermCareEnabledState` 锁定保存时 `isEnabled=0` 不被覆盖，`#updateInsuranceProject_shouldPersistBackfilledMedicalLongTermCareProjectWhenProjectIdMissing` 锁定旧方案虚拟补行可在月度编辑中落库；前端 `node tests/insurance-scheme-projects.test.mjs` 通过。

- 数据配置类型和值自定义（2026-08-22）：
  - `DictDataController#add(...)` 的 `AddType` 改为可选，前端当前不再依赖它提交固定 `gongduan/chejian`。
  - `tbDictDataServiceImpl#getbyDtId(...)` 与 `#getbyPId(...)` 现在会把 `tbdictdata.sn` 回填到 `TreeNode.type`，前端列表可直接展示用户自定义类型文本。
  - `tbDictDataServiceImpl#add(...)` 现在会在新增时为缺失的 `dtid/pid/canUse/createMan/createTime` 补默认值；编辑时会合并已有记录的 `dtid/pid/canUse/createMan/createTime/sn/name`，避免只提交 `sn/name` 时把历史元数据洗掉。
  - 该模块不再强制 `sn` 使用“工段/车间”字典含义，用户输入的 `sn` 就是列表上的类型文本，`name` 就是值文本；`dtid` 仍保留为兼容旧树数据的内部字段。
  - 验证：`mvn -Dtest=tbDictDataServiceImplTest test` 通过，2 个测试 0 failures/errors。
- 员工其他补助导入导出与保存（2026-08-21）：
  - `AddEmployeeBO`、`AddEmployeeFieldManageBO`、`HrmEmployeeChangeRecord` 新增 `otherSubsidy` 入参，用于新建员工、再次入职、详情基本信息保存以及办理转正/调岗/晋升降级等员工变更弹窗提交。
  - `HrmEmployeeServiceImpl#ensureEmployeeSalaryDynamicFields()` 在既有 `薪资等级`、`固定绩效`、`职务补助` 基础上补齐 `其他补助`，字段名 `other_subsidy`，显示名 `其他补助`，个人信息分组，类型 `FieldTypeEnum.DECIMAL`，精度 `2`；历史字段会复用原 ID 并纠正类型、分组和可见/可编辑属性。
  - `saveEmployeeSalaryDynamicFields(...)` 现在只删除并重写四个薪资动态字段，`其他补助` 和其它金额一样通过 `normalizeEmployeeSalaryDecimalValue(...)` 保存为两位小数字符串。
  - `updateInformation(...)` 的兜底动态字段解析支持 `其他补助/other_subsidy/otherSubsidy`，前端缺少真实 `fieldId` 时仍能绑定到后端字段定义后保存。
  - `HrmEmployeeController#downloadEmployeeRosterTemplate(...)` 运行时在 `职务补助` 右侧插入 `其他补助`，设置列格式 `0.00`，并把 `薪酬福利` 合并父表头扩展到该列。
  - `HrmEmployeeServiceImpl#importEmployee(...)` 的花名册动态列识别将 `其他补助` 纳入两位小数字段，导入值保存到 `hrm_employee_data.field_value/field_value_desc`。
  - `EmployeeDepartmentDetailExportSupport` 的 `薪资待遇` 展示和固定薪资成本汇总纳入 `其他补助`，展示顺序为固定、绩效、职务补助、其他补助、全勤。
  - 同步修复员工导入匹配：花名册行若 `姓名+手机号` 未匹配，但 `姓名+身份证号` 命中未删除员工，则更新原员工，避免手机号变更时被误判为新员工并触发工号重复。
  - 验证：`mvn -Dtest=HrmEmployeeControllerTest,HrmEmployeeServiceImplSalaryFieldsTest,HrmEmployeeServiceImplImportEmployeeTest,EmployeeDepartmentDetailExportSupportTest test` 通过，44 个测试。

- 员工合同无固定期限结束日期（2026-08-21）：
  - `HrmEmployeeContractServiceImpl#addOrUpdateContract(...)` 保存前统一调用 `normalizeContractTerm(...)`。当 `contractType=EmployeeContractType.NO_FIXED_TERM_LABOR_CONTRACT.getValue()`（当前值为 `2`）时，只要求 `startTime` 必填，并主动清空 `endTime` 与 `term`；其它合同类型继续走 `calculateContractTerm(startTime, endTime)`，保留开始/结束日期必填和结束日期不能早于开始日期的旧校验。
  - 合同 Excel 导入 `buildImportContract(...)` 先解析 `合同类型`，再决定 `合同结束日期` 是否必填。无固定期限劳动合同导入时允许该列为空，即使填写了 `2099-12-31` 等占位日期也会在保存归一化阶段清空；固定期限、劳务、派遣等其它类型仍通过 `getRequiredImportDate(...)` 强制结束日期必填。
  - 员工基础信息导出 `EmployeeBasicInfoExportSupport` 新增无固定期限显示判断：当最近/最后一次合同类型为 `2` 时，“劳动/劳务合同期限”和“结束时间”均输出 `无固定期限`，不再展示空白或远期占位日期。
  - 部门明细导出 `EmployeeDepartmentDetailExportSupport` 对 `合同到期日` 使用相同判断，`contractType=2` 时输出 `无固定期限`。
  - `HrmEmployeeMapper.xml#queryPageList` 新增 `latestContractType`、`lastContractType`，`#queryDepartmentDetailExportList` 新增 `lastContractType`，供导出格式化层区分“没有合同结束日期”和“无固定期限合同”。
  - 到期提醒和本月到期统计仍基于 `hrm_employee_contract.end_time` 的年月查询；无固定期限合同保存为 `NULL end_time` 后自然不会进入到期提醒范围。
  - 回归测试：`HrmEmployeeContractServiceImplTest` 覆盖手工保存、导入空结束日期、导入占位日期清空、固定期限仍必填；`EmployeeBasicInfoExportSupportTest`、`EmployeeDepartmentDetailExportSupportTest` 和 `HrmEmployeeMapperSqlTest` 覆盖导出显示与 SQL 别名。
  - Fresh 验证：`mvn -Dtest=HrmEmployeeContractServiceImplTest,EmployeeBasicInfoExportSupportTest,EmployeeDepartmentDetailExportSupportTest,HrmEmployeeMapperSqlTest test` 通过，38 个测试 0 failures/errors；`mvn -DskipTests compile` 通过，保留既有 Maven POM duplicate/systemPath warnings。

- 社保管理本月参保/停保人员名单（2026-08-21）：
  - 后端社保月报主列表链路为 `HrmInsuranceMonthRecordController#queryInsuranceRecordList -> HrmInsuranceMonthRecordService#queryInsuranceRecordList -> HrmInsuranceMonthRecordMapper#queryInsuranceRecordList`。
  - `QueryInsuranceRecordListVO` 新增 `insuredNum`、`insuredEmployeeNames`、`stoppedEmployeeNames`；旧 `num` 与 `stopNum` 保持不变。
  - `HrmInsuranceMonthRecordMapper.xml#queryInsuranceRecordList` 对 `b.status=1` 返回 `num/insuredNum`，对 `b.status=0` 返回 `stopNum`，并用 `GROUP_CONCAT` 按姓名拼接本月参保/停保人员名单。
  - `HrmInsuranceMonthRecordService#queryInsuranceRecordList` 在查询前调用 `setGroupConcatMaxLen()`，并保留事务边界，确保 `SET SESSION group_concat_max_len = 4194304` 与列表 SQL 使用同一连接。
  - 前端 `hr_web/src/views/hrm/insurance-scheme/InsuranceScheme.vue` 不再在月报卡片下方直接铺开名单；“本月参保人数”和“本月停保人数”的数字使用 `el-tooltip`，悬浮/聚焦时显示对应人员名单和人数，页面主体只保留紧凑统计项。
  - 社保方案管理链路为 `HrmInsuranceSchemeController#index -> HrmInsuranceSchemeService#index -> HrmInsuranceSechemeMapper#index`。`InsuranceSchemeListVO` 新增 `useEmployeeNames`，`HrmInsuranceSechemeMapper.xml#index` 与遗留 `HrmInsuranceSchemeMapper.xml#queryInsuranceSchemePageList` 均在 `use_count` 旁返回按姓名聚合的 `useEmployeeNames`。
  - `HrmInsuranceSchemeService#index` 查询前调用 `HrmInsuranceSechemeMapper#setGroupConcatMaxLen()`，确保方案使用人员名单不会被默认 `GROUP_CONCAT` 长度截断。
  - 前端 `hr_web/src/views/manage/insurance-scheme/InsuranceScheme.vue` 的“使用人数”列仍显示 `useCount`，并通过 `el-tooltip` 显示“使用人数：N人”和 `useEmployeeNames || '--'`。
  - 回归测试：`HrmInsuranceMonthRecordMapperXmlTest` 锁定新增 SQL 字段、名单聚合和会话参数设置；`HrmInsuranceMonthRecordServiceTest` 锁定查询前先调高 `group_concat_max_len`。
  - 追加回归测试：`HrmInsuranceSchemeMapperXmlTest` 锁定方案列表 `useEmployeeNames` 聚合与 `group_concat_max_len`；`HrmInsuranceSchemeServiceTest` 锁定查询顺序；`InsuranceSchemeListVOTest` 锁定 VO 字段；前端 `insurance-advanced-filter.test.mjs` 与 `insurance-scheme-usage-tooltip.test.mjs` 锁定两个 tooltip 行为。

- `cfy` 登录 `TooManyResultsException` 修复（2026-08-21）：
  - 根因：登录第一步 `LoginUserMapper#getCompanyIdByUserName` 直接按 `account` 从系统库 `hrsystem.tbAllUserList` 查询单个 `CompanyID`；现场 `cfy` 在该表中存在两条同公司 `0001` 索引行，MyBatis 单值查询因此抛 `TooManyResultsException`。只读核验显示 `hr_0001.view_LoginUser` 中 `cfy` 只有一条有效用户，问题不在租户登录视图。
  - `LoginUserMapper` 改为 `getCompanyIdsByUserName(...)`，SQL 使用 `SELECT DISTINCT CompanyID`；`LoginController` 统一解析公司 ID：空结果返回“账号不存在”，同一公司重复行去重后继续登录，多个不同公司返回 `<账号>登录账号配置重复，请联系管理员处理!`。
  - `TbLoginUserMapper#findSystemCompanyIdsByAccount(...)` 同步改为返回去重后的公司 ID 列表，避免登录用户管理保存校验也因历史重复索引行抛 MyBatis 原始异常。
  - `TbLoginUserService#syncSystemLoginAccount(...)` 保存系统库账号索引前会先删除当前公司同账号旧索引，再插入新索引；编辑账号变更时仍先删除旧账号索引，避免无唯一约束的 `tbAllUserList` 继续累积同公司重复行。
  - 代码审查后修正：`TbLoginUserService#resolveCompanyId(...)` 优先使用当前 `CompanyContext.companyId`，只有没有登录上下文时才回退请求体 `companyId`。这样登录用户管理接口不会因前端传错或伪造 `companyId`，把租户库用户保存到当前公司、却把系统库账号索引写到另一个公司。
  - 现场只读数据：`hrsystem.tbAllUserList` 目前只有 `cfy` 存在重复，且 `COUNT(DISTINCT CompanyID)=1`；发布本代码后该重复不再阻断登录，后续编辑/保存该登录用户会顺带清理为单行索引。
  - 2026-08-21 本地复测：`POST http://127.0.0.1:9080/hrsystem/login?account=cfy&password=123` 与 `POST http://127.0.0.1:8081/api/hrsystem/login?account=cfy&password=123` 均返回 `success=true`。如果有人仍看到旧 `TooManyResultsException`，优先排查前端请求是否打到了旧后端、旧代理或未刷新的页面缓存，而不是当前 `target/classes` 进程。
  - 回归测试：`LoginControllerTest` 覆盖同公司重复索引可登录、跨公司重复索引返回中文错误；`TbLoginUserServiceValidationTest` 覆盖系统账号查询上下文恢复、同公司重复允许、跨公司抢占禁止和保存前删除去重；`TbLoginUserMapperSqlTest` 锁定系统账号索引查询使用 `SELECT DISTINCT CompanyID`。

- 薪资档案在职离职筛选（2026-08-20）：
  - 前端 `hr_web/src/views/hrm/salary/archives/Archives.vue` 新增 `在职`、`离职` 两个卡片式状态筛选入口，默认选中 `在职`。
  - 页面继续调用既有 `/hrmSalaryArchives/querySalaryArchivesList`，但请求体会把当前卡片状态写入 `status`：`在职 -> 11`，`离职 -> 15`。
  - 后端 `HrmSalaryArchivesMapper.xml#querySalaryArchivesList` 已有对应 SQL：`status=11` 过滤 `entry_status in (1,3)`，`status=15` 过滤 `entry_status=4`；本次不新增后端接口、DTO 字段或数据库字段。
  - 切换卡片时前端会把分页重置为第一页再刷新列表；姓名/工号搜索与分页会保留当前卡片状态一起查询。
  - 回归测试：`hr_web/tests/salary-archives-status-filter.test.mjs` 锁定默认在职、卡片渲染、`status` 参数提交和切换刷新；`HrmSalaryArchivesMapperSqlTest` 锁定后端 `11/15` 入离职 SQL 口径。

- 员工新增编辑薪资字段（2026-08-20）：
  - `AddEmployeeBO` 与 `AddEmployeeFieldManageBO` 新增 `salaryLevel`、`fixedPerformance`、`dutySubsidy`，分别承载新增/再次入职弹窗提交的 `薪资等级`、`固定绩效` 与 `职务补助`。
  - `HrmEmployeeServiceImpl#personalInformation(...)` 和 `#personalArchives(...)` 读取前调用 `ensureEmployeeSalaryDynamicFields()`，幂等补齐或纠正 `薪资等级`、`固定绩效`、`职务补助` 三个个人信息动态字段。
  - 字段定义规则：`薪资等级` 为 `FieldTypeEnum.TEXT`，`固定绩效`、`职务补助` 为 `FieldTypeEnum.DECIMAL` 且 `precisions=2`；历史字段若在社保等其它分组，会保留原 `field_id` 并改回个人信息分组，避免数据迁移或重复字段。
  - 新增/再次入职保存时通过 `saveEmployeeSalaryDynamicFields(...)` 删除当前员工这三个字段 ID 的旧动态值后重写非空值，避免使用 `saveEmployeeField(PERSONAL)` 误删其它个人动态字段。
  - `updateInformation(...)` 与 `addEmployeeField(...)` 在保存动态字段前调用 `convertEmployeeDynamicFieldValue(...)`，其中 `固定绩效`、`职务补助` 会统一转成两位小数字符串；其它字段仍走 `employeeFieldService.convertObjectValueToString(...)` 原逻辑。
  - 2026-08-20 追加：`updateInformation(...)` 在固定/非固定字段分组前调用 `normalizeEmployeeSalaryInformationFields(...)`。当前端因接口字段定义缺失而提交只有 `salary_level/fixed_performance/duty_subsidy` 或中文名的兜底字段时，后端会重新解析真实动态字段定义，补齐 `fieldId/type/labelGroup/isFixed` 后再交给原动态字段保存链路。
  - 2026-08-20 追加：`ensureEmployeeSalaryDynamicFields()` 自动创建缺失字段时改为逐个 `employeeFieldService.save(...)`，避免项目自定义 `saveBatch(...)` 不回填雪花主键导致后续保存薪资动态值缺少 `field_id`。
  - 2026-08-20 追加：员工列表操作列的 `办理转正`、`调整部门/岗位`、`晋升/降级` 走 `/become`、`/changePost`、`/promotion`，入参为 `HrmEmployeeChangeRecord`。该对象新增 request-only 的 `salaryLevel/fixedPerformance/dutySubsidy`，不映射物理列；`HrmEmployeeServiceImpl#change(...)` 检测到非空薪资输入后调用 `saveEmployeeSalaryDynamicFields(...)` 写入动态字段值。
  - 回归测试：`HrmEmployeeServiceImplSalaryFieldsTest` 覆盖 BO 入参、动态字段创建/历史纠正、只覆盖薪资字段、固定绩效/职务补助两位小数、详情编辑格式化、编辑兜底字段元数据补齐、自动创建字段后带 ID 保存，以及员工变更记录携带薪资字段后写入动态字段；并联回归 `HrmEmployeeServiceImplImportEmployeeTest`、`HrmEmployeeServiceImplCompanyAgeTest`。

- 员工花名册模板薪资字段（2026-08-20）：
  - `HrmEmployeeController#downloadEmployeeRosterTemplate(...)` 仍读取 classpath 资源 `export/employee_module.xlsx`，但下载前会用 Apache POI 运行时补齐 `固定绩效`、`职务补助` 两列，而不是要求维护二进制模板文件。
  - 补列只处理可见且第 2 行表头同时包含 `姓名`、`个人电话` 的花名册样式 sheet；隐藏 sheet 和非员工花名册简表不处理。
  - 不再新增 `薪资级别` 列，模板中已有 `薪资等级` 列继续保留；`固定绩效`、`职务补助` 依次插入到 `薪资等级` 右侧，第 1 行分组写 `薪酬福利`，默认列格式为 `0.00`。
  - 运行时插列会先移动受影响合并区域，再重新合并覆盖 `薪资等级`、`固定绩效`、`职务补助` 的 `薪酬福利` 父表头，避免 POI 插列后出现两个独立同名父表头。
  - `HrmEmployeeServiceImpl#importEmployee(...)` 的花名册导入继续把未固定映射的列保存为员工动态字段；`固定绩效`、`职务补助` 通过表头识别为 `FieldTypeEnum.DECIMAL`，字段 `precisions=2`，其余新增动态字段默认仍为 `FieldTypeEnum.TEXT`。
  - 若库里已经存在历史误建的文本型 `固定绩效` 或 `职务补助` 动态字段，导入时会复用原字段 ID 并把字段定义纠正为 `DECIMAL + precisions=2`。
  - 写入 `hrm_employee_data` 前会对 `固定绩效`、`职务补助` 去掉千分位逗号并用 `BigDecimal.setScale(2, HALF_UP)` 规范成两位小数字符串。
  - 当前方案使用既有动态字段表 `hrm_employee_field` 与动态值表 `hrm_employee_data`，不新增员工主表字段；因此不需要数据库 DDL 脚本。若后续业务要求固定绩效或职务补助成为员工主表固定列，则需要另行设计迁移脚本、实体字段和页面字段配置。
  - 回归测试：`HrmEmployeeControllerTest#downloadEmployeeRosterTemplate_shouldPutFixedPerformanceNextToSalaryGradeWithoutSalaryLevel` 锁定模板列位置、格式和父表头合并；`HrmEmployeeServiceImplImportEmployeeTest` 锁定 `固定绩效`、`职务补助` 字段类型、精度和值保存，并覆盖历史字段类型纠正。

- 员工管理下载部门明细方案（2026-08-19）：
  - 业务选择方案 A：参考业务提供的 `/Users/jiangyongming/Desktop/农谷导入数据/各部门现有人员明细表(更新版).xlsx` 样式，生成动态 Excel，而不是固定下载原文件。
  - 导出范围已确认为当前登录公司内全部未删除且在职员工，按 `hrm_employee.is_del=0 and hrm_employee.entry_status in (1,3)` 查询；该下载不跟随员工管理页面筛选条件、状态页签或分页。
  - 后端已新增 `/hrmEmployee/exportDepartmentDetail`，由 `HrmEmployeeController` 委托 `IHrmEmployeeService`；`EmployeeDepartmentDetailExportSupport` 负责多 sheet workbook、汇总区、部门明细区、样式和文件名。
  - 数据字段复用员工管理已有映射：员工档案、`hrm_dept`、`hrm_employee_contract`、`hrm_employee_education_experience` 和动态字段查询。`薪资待遇` 读取薪资档案中 `10101 / 基本工资`、`10102 / 岗位工资`、`10103 / 职务工资` 三项金额之和；员工动态字段存在 `固定绩效`、`职务补助` 时，`EmployeeDepartmentDetailExportSupport#salaryTreatmentText(...)` 按 `固定薪资(固定)+固定绩效(绩效)+职务补助(职务补助)+全勤(全勤)` 拼接，例如 `4000(固定)+300(绩效)+200(职务补助)+500(全勤)`；员工当前状态为试用期时继续沿用薪资档案既有试用/正式取数口径。
  - 2026-08-20 追加：部门明细 `薪资待遇` 会在固定薪资/固定绩效/职务补助基础上追加全勤奖。`HrmEmployeeServiceImpl#exportDepartmentDetail(...)` 调用 `HrmSalaryBasicService#findAll()` 读取最新基本工资设置中的 `ordinaryFullAttendanceAmount` 和 `leaderFullAttendanceAmount`，并传给 `EmployeeDepartmentDetailExportSupport`；岗位包含 `经理`、`总监`、`副总`、`董事长`、`高级技师`、`厂长` 时使用领导全勤金额，否则使用普通全勤金额；员工 `status=2/试用` 时不追加全勤；解析出的全勤金额等于 `100` 时按业务例外视为不追加。展示示例：`4000`、`4000(固定)+300(绩效)+200(职务补助)+500(全勤)`。
  - 2026-08-24 追加：全勤追加新增员工级开关判断，`HrmEmployeeMapper#queryDepartmentDetailExportList` 返回 `a.full_attendance as fullAttendance`，`EmployeeDepartmentDetailExportSupport` 只有在 `fullAttendance=1` 且非试用期时才追加全勤；`fullAttendance=2` 或空值不追加，金额为 `100` 的历史例外继续保留。
  - 2026-08-24 追加行政经理跨公司导出：`HrmEmployeeServiceImpl#exportDepartmentDetail(...)` 先按 `EmployeeDepartmentDetailCrossCompanyExportSupport.isAdministrativeManager(...)` 判断角色。普通角色继续下载当前租户单个 `.xlsx`；`roleName=行政经理` 时遍历 `0001/0002/0003/0004/0005`，通过 `withCompanyContext(...)` 切换公司上下文并复用 `buildDepartmentDetailExportData()` 分别构建每个公司明细，包括成都 `0002`。
  - 跨公司导出时每个明细文件名由目标库顶层组织生成 `公司名部门明细.xlsx`；如果目标库缺顶层组织，降级为 `hr_公司ID`，并且 `withCompanyContext(...)` 不再把当前登录公司的 `companyName` 带到其它库。
- `EmployeeDepartmentDetailGroupSummarySupport` 生成 ZIP 内 `集团总表.xlsx`，汇总五个公司的企业人数、固定薪资成本、绩效薪资成本、社保成本、公积金成本和总成本；总表不包含伙食列。
- `EmployeeDepartmentDetailCrossCompanyExportSupport#buildEncryptedZip(...)` 使用 Zip4j AES-256 加密所有 Excel，`HrmEmployeeServiceImpl` 将随机密码写入 `X-Archive-Password` 响应头并下载 `部门明细.zip`。`CrossDomainFilter` 暴露 `Content-Disposition, X-Archive-Password`，让浏览器前端可读取文件名和密码。
- `hr_web/src/views/hrm/employee/Index.vue` 读取到压缩包密码后使用可关闭的长驻消息展示密码，并提供“复制”按钮；按钮优先调用 Clipboard API，失败时降级为隐藏文本框复制，复制结果给出中文反馈。
- 2026-08-19 追加：`EmployeeDepartmentDetailExportSupport` 已补齐部门 sheet 顶部第 1-8 行参考表结构。A-H 区域包含部门、部门总人数、男女比例、学历层次、工龄结构、专业汇总位置和实习/正式/试用统计；A2:A6、B2:B6、C2:C6、F1:H1、F2:H3、F4:H6、A7:H8 按参考表合并。
  - 2026-08-19 追加：部门明细 `合同到期日` 由 `HrmEmployeeMapper#queryDepartmentDetailExportList` 的 `lastContractEndTime` 提供，直接读取 `hrm_employee_contract.end_time`。最新合同按员工合同页面列表顺序取最后一条，SQL 排序为 `coalesce(fc.sort, -1) desc, fc.start_time desc, fc.contract_id desc`；移除旧的 `fc.start_time is not null` 过滤，避免有合同截止日期但开始日期为空时导出为空。
  - 2026-08-20 复核：本地 8081 实际下载 `/tmp/hainan_department_detail_contract_check.xlsx` 与 dev 库 `hr_0003` 同口径全量对账通过，导出 92 人、数据库期望 92 人，有合同到期日均为 15 人，差异 0；黎冬霜所在 `采购计划部` 行合同到期日为 `2027-04-30`。
  - 2026-08-20 追加核验：当前本地 `hr_0003` 导出文件 `/tmp/hainan_department_detail_check.xlsx` 中可直接搜到 `王志兰`，其所在 `研发部` sheet 正常生成；如果现场下载缺少该员工，优先排查是否拿到了旧后端、旧缓存文件或非 `0003` 租户数据，而不是当前导出逻辑。
  - 顶部统计区只使用当前部门导出员工列表计算，不新增额外查询：性别按 `sex`，学历按 `highestEducation` 归档，本科行兼容硕士/博士/博士后，中专行兼容中职/中技，初中行兼容小学；工龄优先解析已填充的 `companyAge`，缺失时回退 `companyAgeStartTime/entryTime`；专业汇总位置不再输出“专业相关/专业不相关”文案；`一年以上实习生` 使用员工表 `status=3` 且 `probation>=12`，`正式老员工` 使用 `status=1`，`试用期人员` 使用 `status=2`。
  - 部门明细表头最终为 11 列：序号、姓名、岗位、入职年限、性别、年龄、学历、专业、薪资级别、薪资待遇、合同到期日；参考表中的“年平均考核分”“理论考核分”两列直接删除，不保留空列。
  - 部门 sheet 命名已补充去重保护，先做 Excel 安全命名，再按 workbook 现有 sheet 名递增后缀避免重复。
  - 2026-08-20 追加：部门 sheet 右侧成本区不再留空，`EmployeeDepartmentDetailExportSupport` 汇总并写入 J2-J5：`月固定薪资成本` 取本部门员工当前薪资档案 `10101/10102/10103` 三项之和，并额外加上员工动态字段 `固定绩效`、`职务补助`；`月绩效薪资成本` 只取当前薪资档案 `41001 / 绩效工资`，未维护时显示 `0`，不把动态字段 `固定绩效`、`职务补助` 计入绩效成本；`月社保成本`、`月公积金成本` 取最近有效 `hrm_insurance_month_emp_record` 月份中本次导出在职员工的公司缴纳社保 `corporate_insurance_amount` 和公司缴纳公积金 `corporate_provident_fund_amount`。`月伙食成本` 已从部门 sheet 成本区删除。
  - 2026-08-20 追加：`人员总表` 不再只是部门人数两列表，而是基于 `DepartmentSummary` 生成参考表同类横向总表：第 1 行标题为 `<公司> 公司人员明细总表`，第 2-6 行为企业总人数和四项月总成本，第 7 行起按部门横向输出人员总数、固定薪资、绩效薪资、社保、公积金和合计月人工成本；总表不再保留伙食成本相关行。
  - `HrmEmployeeServiceImpl#exportDepartmentDetail` 在生成 workbook 前加载薪资档案项 `10101/10102/10103/41001`，并通过 `buildLatestDepartmentDetailInsuranceCostMap(...)` 按 `year desc, month desc` 查最近 `status=1` 的社保员工月记录，再按本次未删除员工范围批量回填企业社保和企业公积金成本。
  - 下载文件名由后端统一生成，格式为 `<组织管理顶层组织名称>人员明细表yyyyMMdd.xlsx`；顶层组织名称取组织管理中最顶层数据，日期取导出当天服务器日期。
  - 前端已在 `hr_web/src/views/hrm/employee/Index.vue` 的“员工导入/导出”下拉中新增“下载部门明细”，使用独立 loading 状态，并优先按 `Content-Disposition` 回收后端文件名。
  - 验证：`mvn -Dtest=EmployeeDepartmentDetailExportSupportTest,HrmEmployeeControllerTest,HrmEmployeeMapperSqlTest test`、`mvn -DskipTests compile`、`node tests/employee-department-detail-export-api.test.mjs`、`node tests/employee-department-detail-export-ui.test.mjs`、`node tests/employee-basic-info-export-api.test.mjs`、`node tests/employee-basic-info-export-ui.test.mjs`、`node tests/employee-actions-layout-ui.test.mjs`、`npm run build` 通过。2026-08-19 追加结构测试 `buildWorkbook_shouldMatchReferenceDepartmentTopSummaryStructure`，锁定部门 sheet 顶部统计区、合并单元格和成本区；追加 `buildWorkbook_shouldWriteDepartmentCostValuesFromSalaryAndInsuranceSources`、`HrmEmployeeServiceImplDepartmentDetailCostTest` 锁定四项成本填值和“月伙食成本”删除；追加 SQL 测试 `queryDepartmentDetailExportList_shouldUseLatestEmployeeContractEndTimeFromContractList`，锁定合同到期日按员工合同最新一条结束日期取数；2026-08-20 追加 `summarySheet_shouldMatchReferenceTotalSheetWithoutMealCost` 和 `buildWorkbook_shouldRemoveProfessionalRelationTextAndUseEmployeeStatusCounters`，锁定总表结构、专业相关描述删除和员工状态统计口径；2026-08-24 追加 `EmployeeDepartmentDetailCrossCompanyExportSupportTest`、`CrossDomainFilterTest#corsShouldExposeArchivePasswordForEncryptedDownload` 和 `HrmEmployeeServiceImplDepartmentDetailCostTest#exportDepartmentDetail_shouldBuildEncryptedArchiveForAdministrativeManager`，锁定行政经理跨公司 ZIP、密码响应头、上下文恢复和集团总表无伙食列。

- 排班上传休假录入修复（2026-08-18）：
  - 根因：`WorkPlanServiceImpl#isHorizontalTruthy(...)` 未把模板“休假/调休”列中的 `休假` 视为有效休息类值，导致横版上传模板中填 `休假` 的日期被当成空排班跳过。
  - 同类根因：`normalizeShiftTypeValue(...)` 未把 `休假` 归一为 `rest`，服务层收到 `休假` 时会走需要上下班时间的非休息类分支；`normalizeRestShiftType(...)` 也未把 `休假` 归一为 `rest_shift_type=rest`。
  - 修复：横版模板解析、班次类型归一化和休息子类型归一化均支持 `休假`；`休假/休息/休` 保存为 `shift_type=rest, rest_shift_type=rest`，`调休` 仍保存为 `rest_shift_type=adjust`。
  - 回归测试：`WorkPlanServiceImplTest#previewImportExcel_shouldTreatHorizontalVacationAsRestShift` 覆盖横版模板填写 `休假` 后生成休息类排班；`#saveEmployeeDayShift_shouldTreatVacationTextAsRestShift` 覆盖服务层直接收到 `休假` 文本。
  - 验证：`mvn -Dtest=WorkPlanServiceImplTest#previewImportExcel_shouldTreatHorizontalVacationAsRestShift+saveEmployeeDayShift_shouldTreatVacationTextAsRestShift test` 通过；`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过（62 tests，0 failures/errors）。

- 服务器部署跨域修复（2026-08-18）：
  - `CrossDomainFilter` 已注册为 Spring bean，按请求 `Origin` 回显允许源，统一允许 `token`、`Content-Type`、`Authorization` 等请求头，并对预检 `OPTIONS` 直接返回 200。
  - `CompanyInterceptor` 在 token 校验前先放行 `OPTIONS`，避免浏览器预检被登录鉴权挡住后误报 CORS。
  - 去掉了少数局部 `@CrossOrigin` 注解，避免局部注解与全局跨域过滤器叠加出重复响应头。
  - 验证：新增 `CrossDomainFilterTest`、`CompanyInterceptorTest`，覆盖预检放行、跨域头回显和普通请求继续链路；`mvn -Dtest=CompanyInterceptorTest,CrossDomainFilterTest test` 通过。
  - 2026-08-18 额外线上核验：`curl -i -X OPTIONS http://153.0.237.99:9080/hrsystem/login` 与 `curl -i -X POST http://153.0.237.99:9080/hrsystem/login?...` 都返回正确 CORS 头且登录成功；`http://153.0.237.99:8081/api/hrsystem/login` 也可正常返回。若浏览器仍报 CORS，问题更可能在前端 bundle、`VITE_API_BASE_URL` 或 nginx `/api` 代理层。

- prod 一键打包脚本（2026-08-18）：
  - 新增 `target/package-prod.sh`，脚本与 Maven package 产物 `target/hrsystem-0.0.1-SNAPSHOT.jar` 位于同一目录。
  - 新增 `target/package-prod.command` 作为 macOS 可双击的一键入口；该入口设置常见 Maven/Homebrew PATH 后调用同目录 `package-prod.sh`，并在交互终端中等待回车关闭窗口。
  - 脚本不直接修改源码配置；执行时用 `rsync` 复制临时项目副本，排除 `.git` 和 `target`，只在临时副本中将 `src/main/resources/application.properties` 改为 `spring.profiles.active=prod`。
  - 临时副本执行 `mvn -DskipTests package`。跳过测试是因为现有 `ApplicationProfileConfigTest` 明确要求源码默认 profile 为 `dev`，而该脚本的目标是生成服务器部署用 prod 包。
  - 构建完成后脚本复制临时 jar 回 `target/hrsystem-0.0.1-SNAPSHOT.jar`，并用 `jar xf` 解包校验 `BOOT-INF/classes/application.properties` 为 `prod` 且 `application-prod.properties` 存在。
  - 验证：`mvn -Dtest=ApplicationProfileConfigTest test` 通过；`./target/package-prod.sh` 通过并覆盖生成 `target/hrsystem-0.0.1-SNAPSHOT.jar`；`jar xf` 复核产物内默认 profile 为 `prod`。
  - 一键入口验证：`zsh -n target/package-prod.command` 通过；`./target/package-prod.command` 成功调用底层打包脚本并生成 prod jar。

- Maven package 默认 Profile 回归（2026-08-18）：
  - 现象：`mvn package` 在 Surefire 阶段失败，首个失败测试为 `ApplicationProfileConfigTest#activeProfileLoadsDatasourceUrlForDebugStartup`，断言 `expected:<dev> but was:<prod>`。
  - 根因：`src/main/resources/application.properties` 默认 profile 被改为 `spring.profiles.active=prod`，与本地/IDE 调试默认加载 `application-dev.properties` 的契约不一致。
  - 处理：将默认 profile 恢复为 `dev`；生产库地址继续由 `application-prod.properties` 承载，按显式生产 profile 启动时使用。
  - 验证：`mvn -Dtest=ApplicationProfileConfigTest test` 通过（1 test，0 failures/errors）；`mvn package` 通过（505 tests，0 failures/errors），生成 `target/hrsystem-0.0.1-SNAPSHOT.jar`。Maven 仍保留既有 POM/dependency warnings。

- 薪资导出 Excel 批注说明（2026-08-17）：
  - `SalaryMonthRecordServiceNew#exportSalaryNew(...)` 在原有 `CustomCellWriteHandler` 小计/合计样式处理器后，注册 `SalaryExportCommentWriteHandler(salaryExportList)`。
  - 新增 `modules/salary/support/SalaryExportCommentWriteHandler`，基于模板固定明细起始行 `rowIndex=4` 将 EasyExcel 当前单元格映射回 `salaryExportList[rowIndex - 4]`。
  - 批注列固定为 `R(17) 全勤奖`、`T(19) 超缺勤`、`V(21) 个人所得税`、`Z(25) 工会费`；`小计`、`合计` 行跳过，避免汇总行出现员工级原因说明。
  - 批注内容只读取 `HrmSalaryExport` 当前导出值，不重新计算薪资；全勤奖和工会费为 `0` 时输出为空/未生成的常见原因，超缺勤和个税输出计算口径与当前金额。
  - 回归测试：`SalaryMonthRecordServiceNewTest#salaryExportComments_shouldExplainTargetSalaryCells` 覆盖四个目标单元格的批注内容；`#salaryExportComments_shouldSkipSubtotalAndTotalRows` 覆盖汇总行不加批注；`#salaryExportSource_shouldRegisterCommentWriteHandler` 锁定导出链路注册。
  - 验证：红灯阶段 `mvn -Dtest=SalaryMonthRecordServiceNewTest test` testCompile 失败于缺少 `SalaryExportCommentWriteHandler`；实现后同一命令通过（68 tests，0 failures/errors）。

- 残疾员工工会费计算规则调整（2026-08-17）：
  - `SalaryComputeServiceNew#computeSalary(...)` 不再用 `isDisabled` 拦截工会费计算；`160102 / 工会费` 统一交由 `calculateUnionFee(...)` 判断公司、员工状态、转正日期和应发工资条件。
  - 残疾员工免个税分支仍保留在 `230101 / 个人所得税` 计算前，避免把“残疾员工有工会费”误扩展为“残疾员工参与个税计算”。
  - 回归测试：`SalaryComputeServiceNewTest#computeSalary_disabledEmployee_shouldPayUnionFeeAndSkipTax` 覆盖残疾正式员工应发 `4200.00` 时生成工会费 `21.00`、个税 `0`、代扣小计包含工会费、实发工资扣除工会费。
  - 验证：红灯阶段 `mvn -Dtest=SalaryComputeServiceNewTest test` 失败于工会费为 `0`；修复后同一命令通过（14 tests，0 failures/errors）。

- 排班管理单日排班删除（2026-08-17）：
  - `IWorkPlanService` 新增 `removeEmployeeDayShift(Long employeeId, Date workDate)`。
  - `WorkPlanListController#removeEmployeeDayShift(...)` 新增 `/workPlan/removeEmployeeDayShift` 路由，校验 `employeeId` 并复用 `parseDateValue(...)` 兼容已有日期格式。
  - `WorkPlanServiceImpl#removeEmployeeDayShift(...)` 复用 `resolveAttendanceUserByEmployeeId(...)`、`findEmployeeDayPlans(...)` 和 `parseUserIds(...)`；对单用户排班行调用 `planRep.deleteAll(...)`，对多用户共享排班行只从 `UserID` 中移除目标用户并 `saveAll(...)`。
  - 删除逻辑只处理目标员工目标日期的本地 `tbplanlist` 记录；找不到记录时不做写库操作，避免误删其他员工或其他日期。
  - 验证：红灯阶段新增控制器与服务测试后 testCompile 报缺少 `removeEmployeeDayShift`；修复后 `mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过（60 tests，0 failures/errors），`mvn -DskipTests compile` 通过，保留既有 Maven POM/dependency warnings。

- 排班白班未勾连班显式保存（2026-08-17）：
  - 根因：手工界面未勾选“连班”只传 `customContinuousShift=false`，没有传 `customContinuousShiftExplicit=true`；后端单日保存和批量保存链路会继续按员工档案 `is_continuous_shift=1` 自动覆盖为连班。
  - `WorkPlanListController#saveEmployeeDayCustomShift(...)` 新增 `customContinuousShiftExplicit` 可选参数，并委托到新的服务重载。
  - `IWorkPlanService` 与 `WorkPlanServiceImpl#saveEmployeeDayShift(...)` 新增兼容重载；旧调用保持员工档案自动带出，新调用在显式标记为 true 时优先采用前端传入值。
  - `saveEmployeeDayShift(...)` 仍通过 `normalizeCustomContinuousShift(...)` 收敛：只有自定义白班能连班，夜班、休息、调休和标准班次均为非连班。
  - `buildLocalCustomAssignments(...)` 既有导入显式值保护继续生效；Excel “是否连班”空白仍按员工档案自动带出。
  - 验证：红灯阶段 `WorkPlanServiceImplTest#saveEmployeeDayShift_shouldKeepExplicitManualNonContinuousShiftValue` 报缺少显式重载；修复后 `mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test` 通过（57 tests，0 failures/errors）。

- 后端调试模式 JDBC 配置修复（2026-08-16）：
  - 根因：`src/main/resources/application.properties` 中默认 profile 键误写为 `spring.profiles.activ=dev`，缺少末尾 `e`；`ConnectionParsor#getDefaultConnection()` 手动读取该文件时只认 `spring.profiles.active`，因此调试启动未加载 `application-dev.properties`。
  - 失败表现：默认 `HikariDataSource` 构建时设置了 `driverClassName`，但 `spring.datasource.url` 为空，启动阶段报 `jdbcUrl is required with driverClassName`。
  - 修复：将配置键改为 `spring.profiles.active=dev`，恢复默认加载开发环境数据源配置。
  - 回归测试：新增 `ApplicationProfileConfigTest#activeProfileLoadsDatasourceUrlForDebugStartup`，验证默认 active profile 为 `dev`，且合并 profile 后能读取 MySQL JDBC URL。
  - 验证：`mvn -Dtest=ApplicationProfileConfigTest test` 通过；`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 顶栏钉钉 API 调用量展示（2026-08-16）：
  - 新增 `HrmDingTalkApiUsageController`，接口为 `POST /dingTalkApiUsage/monthly`，返回统一 `Result<DingTalkApiUsageVO>`。
  - 新增 `IHrmDingTalkApiUsageService` 与 `HrmDingTalkApiUsageServiceImpl`，按当前服务器月份查询 `postresultlog.createTime` 区间内的本地调用记录。
  - `postresultlogRepository` 新增 `findAllByCreateTimeGreaterThanEqualAndCreateTimeLessThan(...)`，用于按月只读聚合调用记录。
  - 新增 `DingTalkApiUsageVO` 与 `DingTalkApiUsageDistributionVO`：主对象包含 `month/totalCount/monthlyLimit/usagePercent/thresholdPercent/overThreshold/description/limitSource/distributions`；分布对象包含 `featureName/count/percentage`。
  - 功能分类基于 `postUrl + className` 的稳定特征归类为同步考勤排班、打卡明细、考勤组/班次、员工花名册、报表/请假时长、审批数据获取、排班提交/调组、获取访问令牌和其他钉钉接口。
  - 配置项：`dingtalk.api.monthly-limit` 默认 `10000`，`dingtalk.api.usage-threshold-percent` 默认 `80`；已写入 `application.properties` 与 `application-dev.properties`，现场可按钉钉实际月额度覆盖。
  - 该接口未映射到具体业务菜单权限，按当前拦截器语义只要求登录 token，适合作为所有已登录用户可见的全局顶栏状态。
  - 验证：`mvn -Dtest=HrmDingTalkApiUsageServiceImplTest,HrmDingTalkApiUsageControllerTest test` 通过 3 个测试。
- 排班管理考勤信息改读同步数据（2026-08-12）：
  - `HrmAttendanceDataController#getPlanDataByGroup(...)` 不再调用钉钉 `attendance/group/memberusers/list` 或 `attendance/schedule/listbyusers`。
  - 排班详情现在按请求日期范围读取本地 `hrm_attendance_plan`，用 `tbattendanceuser.userId` 回填员工姓名，并按 `GroupID` 过滤；过滤时兼容 `hrm_attendance_group.attendance_group_id` 与 `old_group_id`。
  - 本地排班计划同一员工同一天同班次存在多条上下班记录时，展示侧优先选择 `OffDuty`，否则选择 `planCheckTime` 较晚的记录，避免矩阵出现重复班次。
  - `WorkPlanServiceImpl#getAllGroupsForDisplay(...)` 改为从本地 `hrm_attendance_group.shift_setting` 与 `hrm_attendance_shift` 构建展示考勤组和班次，不再在展示缓存为空时请求钉钉 `getsimplegroups`。
  - `WorkPlanServiceImpl#getUsersForDisplay(...)` 展示缓存刷新只读本地 `tbattendanceuser`，本地为空时返回空列表，不再回退钉钉成员接口。
  - `/attendanceData/getClassListByGroup` 在 Redis 班次缓存缺失时，从本地考勤组/班次快照构造“休假 + 当前组班次”下拉，避免排班详情页因未先刷新 Redis 而失败。
  - `/attendanceData/getShiftList` 遗留班次列表接口也改为读取本地 `hrm_attendance_shift` 并返回钉钉最小班次 VO 兼容结构，不再调用钉钉 `attendance/shift/list`。
  - 保留标准排班提交、跨考勤组调组等明确写操作的钉钉调用边界；本次只收敛排班管理展示查询的数据源。
  - 验证：`mvn -Dtest=HrmAttendanceDataControllerTest,WorkPlanServiceImplTest test` 通过 55 个测试；`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 排班管理矩阵前端分页（2026-08-12）：
  - 本轮未修改后端 `WorkPlanListController` 或 `WorkPlanServiceImpl`，`/workPlan/getData` 仍按月份区间返回排班记录。
  - 翻页无效根因在前端公共 `Table` 只发出分页事件，不会自动切分父组件传入数据；排班页此前传入完整员工矩阵且未监听事件。
  - 前端已在 `hr_web/src/views/hrm/attendance/scheduling/Scheduling.vue` 中按员工矩阵行做本地分页，并在筛选、重置、切换月份时回到第一页。
- 加班/夜班统计出勤时间手工保存（2026-08-10）：
  - `HrmOvertimeNightStatisticsController` 新增 `POST /hrmOvertimeNightStatistics/updateAttendanceSummary`，服务接口为 `IHrmOvertimeNightStatisticsService#updateAttendanceSummary(...)`。
  - 新增 `UpdateOvertimeNightAttendanceBO`，接收 `employeeId/month/expectedAttendanceHours/actualAttendanceHours/accruedAttendanceHours`；服务层校验三项小时不能为空且不能小于 0。
  - `HrmOvertimeNightStatisticsDetail` 映射新增 `expectedAttendanceHours`、`actualAttendanceHours`、`attendanceManualAdjusted`；开始统计和零值占位行会写入小时字段并把人工标记置为 `0`。
  - 保存接口按 `employeeId + month` 查询当月所有明细，批量覆盖三项小时、同步折算 `expectedAttendanceDays/actualAttendanceDays`，并设置 `attendanceManualAdjusted=1`。
  - `queryPageList`、`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 在发现人工标记后优先返回持久化小时值，绕过 `restType` 查询刷新和实际出勤重新计算，避免保存后刷新页面被自动规则覆盖。
  - `HrmProduceAttendanceServiceImpl` 的考勤汇总同步和行政体系导出兜底也改为优先读取 `expectedAttendanceHours/actualAttendanceHours`，旧数据才按天数字段乘以 8。
  - 新增迁移脚本 `docs/sql/2026-08-10_overtime_night_attendance_manual_hours.sql`，幂等补齐 `hr_0001` 至 `hr_0005` 的三个新增列。
  - SQL 执行：2026-08-10 19:07 已在 dev MySQL `153.0.237.98` 执行该脚本；`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 均存在 `expected_attendance_hours`、`actual_attendance_hours`、`attendance_manual_adjusted` 三列。
  - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 77 个测试；`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 薪资月份自助恢复（2026-08-10）：
  - `HrmSalaryMonthRecordController` 新增 `previewSalaryMonthRecovery` 和 `recoverSalaryMonth` 两个接口，前端可按目标年月先预览再恢复，不再依赖 DBA 手工 SQL。
  - `SalaryMonthRecordServiceNew` 新增恢复预览/恢复逻辑：按目标年月查找其后的薪资月记录，逐月检查是否存在员工薪资明细、工资条记录、工资条明细或 `isSend=1`；只要存在上述真实数据，就阻断恢复并返回中文原因。
  - 可恢复时只删除目标月份之后的空薪资月记录，并在目标月仍处于历史状态时按当前是否已有员工薪资明细回退为 `CREATED` 或 `COMPUTE`，让薪资管理页恢复为可继续处理的状态。
  - `SalaryMonthRecordRecoveryTest`、`HrmSalaryMonthRecordControllerTest` 新增回归，覆盖预览可恢复、预览阻断、恢复删除和恢复状态回填。
  - 验证：`mvn -Dtest=SalaryMonthRecordRecoveryTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordNextMonthTest test` 通过 9 个测试；前端 `node tests/salary-month-recovery.test.mjs && node tests/salary-create-next-month.test.mjs` 通过。
- 薪资新建次月防重复与 7 月恢复（2026-08-10）:
  - `SalaryManage.vue#addMony` 改为 `async/await`，新建次月按钮使用独立 `createLoading`，并在 `init()` 中仅当月份为空时才用当前薪资月回填选择器。
  - `SalaryMonthRecordServiceNew#updateCheckStatus(...)` 现在按请求 `year/month` 找到源记录，再调用 `createNextSalaryMonthRecord(...)`；`createNextSalaryMonthRecord(...)` 若发现下一月记录已存在则直接复用，避免重复点击继续推进到 2026-09。
  - `SalaryMonthRecordService_Bak` 同步同一幂等逻辑，防止旧链路恢复后再次按最新 `createTime` 生成重复次月记录。
  - 恢复方案原先记录在 `docs/plans/2026-08-10-salary-next-month-recovery-solution.md`；当前已升级为系统内自助恢复能力，历史手工 SQL 仅保留作紧急参考。
  - `SalaryMonthRecordNextMonthTest` 覆盖“从 7 月创建 8 月”和“8 月已存在时直接复用”两个场景。
  - 验证：`mvn -Dtest=SalaryMonthRecordNextMonthTest test` 通过，2 个测试；前端 `node tests/salary-create-next-month.test.mjs` 通过。
- 审批数据行内添加员工审批（2026-08-03）：
  - `QueryAttendanceApprovalPageVO` 新增 `employeeId` 与 `mobile`，审批数据列表查询可返回员工主键和手机号；前端用手机号辅助识别同名员工，提交仍使用 `employeeId`。
  - `src/main/resources/mapper/HrmAttendanceApprovalMapper.xml` 的审批列表 SQL 补充 `e.employee_id as employeeId`、`e.mobile as mobile`。
  - `AddAttendanceApprovalBO` 新增 `duration` 与 `durationUnit`，用于接收前端合计时长手工输入。
  - `HrmAttendanceApprovalServiceImpl#addManualApproval(...)` 在解析审批日期范围前调用 `resolveManualDurationHours(...)`；支持 `小时/分钟/天` 单位并统一换算为小时。
  - `resolveManualApprovalRanges(...)` 对手工时长做落库分配：单时间段直接覆盖该段时长，多时间段按原始时间段占比分摊，最后一段吸收舍入余量，避免多段合计与用户输入不一致。
  - 后端仍保留未传 `duration` 时按起止时间自动计算时长的旧兼容行为。
  - 2026-08-03 追加：前端添加弹窗和列表展示均按分钟展示审批时间，不显示秒；后端 `AddAttendanceApprovalBO` 仍保留秒级 `JsonFormat`，兼容现有接口请求。
  - 2026-08-03 追加：前端手工添加成功后会把列表月份切换到新增时间段所在月份，并清理审批类型/标签/子类型筛选；行级添加会用当前行员工姓名作为可见筛选，避免新增 `2025-06-01` 数据后仍停在原月份而看不到。
  - 2026-08-03 追加：时长列继续以审批快照 `duration/durationUnit` 为准，`闫倩 2025-06-01 18:00-19:00` 若手工填 `2小时`，列表展示小时部分为 `2小时`，不会按 1 小时时间差覆盖。
  - 验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest test` 通过 23 个测试；`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过 35 个测试。
- 行政体系考勤汇总下载（2026-07-25）：
  - `HrmProduceAttendanceController` 新增 `POST /hrmProduceAttendance/downloadAdministrativeAttendance`，直接写出 Excel Blob；服务接口为 `IHrmProduceAttendanceService#downloadAdministrativeAttendance(...)`。
  - 前端下载弹窗选择具体下载日期后，按日期所在月份提交 `year/month`；后端导出仍只按 `year/month` 查询月度统计、审批和报表数据。
  - `HrmProduceAttendanceServiceImpl` 以 `hrm_overtime_night_statistics_detail` 为导出行基准，按员工档案过滤行政体系和真实部门筛选；导出部门名批量读取 `hrmDeptRepository#findAllByDeptIdIn(...)`。
  - 审批列聚合使用 `tbattendanceuserRepository#findAllByEmpIdIn(...)` 和 `tbattendanceapproveRepository#findAllByUserIdInAndWorkDateBetween(...)`，严格按员工钉钉 userId 映射，过滤 `statisticsStatus=取消至统计`。
  - 行政导出 F 列实出勤小时不再使用 `actual_attendance_days * 8`；审批聚合后按 `应出勤小时 + 审批加班小时 - 事假/病假/调休/年假扣减小时` 重算，与加班/夜班统计页面 `actualAttendanceHours` 保持同一口径。
  - 2026-07-25 修复吴晓霞式半小时差异：`23天应出勤 + 2小时加班 - 0.5小时调休 = 185.5小时`，导出应保留小数而不是取 `actual_attendance_days=23` 后写 `184`。
  - 2026-07-25 修复出差列差异：行政导出 L 列按人工统计口径保持 `0`，`resolveAdministrativeAttendanceApprovalType(...)` 不再把 `bizType=2`、`出差` 或 `外出` 审批映射为导出出差。
  - 2026-07-26 修复李凤皇式固定月休历史明细差异：行政导出在审批聚合和实出勤兜底重算后，调用 `IHrmOvertimeNightStatisticsService#queryPageList(...)` 获取所选月份统计页面当前值，并用 `expectedAttendanceDays * 8`、`actualAttendanceHours`、`accruedAttendanceHours` 覆盖 E/F/G 三列；查询未返回员工时保留旧明细兜底。
  - 新增 `AdministrativeAttendanceReportMetricVO` 和 `HrmProduceAttendanceMapper#queryAdministrativeAttendanceReportMetrics(...)`，从 `hrm_attendance_report_data` 聚合旷工、迟到、早退、上班缺卡、下班缺卡；总缺卡在服务层相加。
  - 新增 `AdministrativeAttendanceExportSupport` 负责读取 `export/行政体系考勤汇总表.xlsx`、清理 S 列之后内容、重建 A-S 表头、移除第二张 sheet 并写入数据行。
  - 验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmProduceAttendanceMapperXmlTest test` 通过 19 个测试；`mvn -DskipTests compile` 通过，保留既有 Maven POM warnings。
- 排班员工连班自动带出（2026-07-25）：
  - 员工固定模型已接入 `isContinuousShift`：JPA `model/HrmEmployee` 映射 `hrm_employee.is_continuous_shift`，MyBatis Plus `entity/po/HrmEmployee`、新增员工 BO、员工 VO 均增加同名字段。
  - `HrmEmployeeMapper.xml#queryPageList` 返回 `a.is_continuous_shift as isContinuousShift`，便于员工列表和固定字段维护链路读取。
  - `WorkPlanServiceImpl#saveEmployeeDayShift(...)` 在保存排班管理单日自定义排班前，会用 `employeeRep.findById(employeeId)` 读取员工连班字段，并覆盖原 `customContinuousShift` 入参；员工字段为空时沿用原入参。
  - `WorkPlanServiceImpl#buildLocalCustomAssignments(...)` 在添加排班批量保存时，先通过 `tbattendanceuserRepository#findAllByUserIdIn(...)` 获取 `empId`，再通过 `hrmEmployeeRepository#findAllByEmployeeIdIn(...)` 批量读取员工 `isContinuousShift`。
  - 同一行多员工会按每个 `userId` 生成独立 `ResolvedWorkPlanAssignment`，支持不同员工不同连班值；相同开始/结束/跨天/白夜班别/连班组合会走本地解析缓存，避免重复创建 `hrm_workplan_custom_shift`。
  - 连班最终仍走原有 `normalizeCustomContinuousShift(...)`：只有白班自定义排班可为 `true`，夜班、休息、调休、标准班次强制为 `false`。
  - 新增 SQL：`docs/sql/2026-07-25_hrm_employee_is_continuous_shift.sql`，幂等补齐 `hr_0001` 至 `hr_0005.hrm_employee.is_continuous_shift`；字段放在 `full_attendance` 后，避免依赖 `rest_type` 脚本先执行。
  - 测试覆盖：`WorkPlanServiceImplTest` 覆盖批量按员工字段拆分、同设置多人复用、单日修改自动带出和夜班强制非连班；`HrmEmployeeMapperSqlTest` 覆盖员工列表 SQL 与迁移脚本契约。
- 排班导入电话匹配与连班显式值（2026-07-25）：
  - `tbplanlist` 新增 `@Transient customContinuousShiftExplicit`，仅用于本地保存链路判断导入文件是否显式填写“是否连班”；该字段不落库。
  - `WorkPlanServiceImpl#buildPlanFromImportRow(...)` 读取导入行“是否连班”：有内容时按 `是/true/1/连/连班` 解析并设置 `customContinuousShiftExplicit=true`；空值时用员工表 `isContinuousShift` 生成默认连班值。
  - `WorkPlanServiceImpl#buildLocalCustomAssignments(...)` 在按员工表自动覆盖连班前会检查 `customContinuousShiftExplicit`，避免导入文件显式填写“否”又被员工默认连班改回“是”。
  - 平铺导入表头从“手机号”调整为“电话”，并新增“是否连班”；解析仍兼容历史“手机号”列。
  - 横向 `workplan.xlsx/xls` 解析支持第二行第 2 列“电话”，日期分组从第 3 列开始；旧无电话列模板仍兼容，未填写电话时按员工姓名匹配，不再因为系统同名候选直接报错。
  - `WorkPlanListController#downloadImportTemplate(...)` 下载固定模板时运行时重建前两行表头：第二行写入“姓名、电话”，后续每日 5 列仍为“白/夜班、上班时间、下班时间、是否连班、休假/调休”；日期行按 5 列合并、居中，并与下方 5 个字段表头使用同组底色，相邻日期组两色循环区分。
  - `/workPlan/saveAll` JSON 解析会保留前端传回的 `customContinuousShiftExplicit`，确保直接上传后的提交任务与预览解析口径一致。
- 考勤汇总部门查询（2026-07-24）：
  - `QueryMonthAttendanceBO` 新增 `deptIds: List<Long>`，用于承载前端考勤汇总高级筛选中的真实部门多选条件。
  - `HrmProduceAttendanceMapper.xml#queryProduceAttendanceList` 保留 `p.department = #{data.department}` 作为所属体系/考勤汇总部门类型过滤，同时新增 `e.dept_id in (...)` 过滤员工档案部门。
  - 本次不改变考勤汇总同步时 `department=1/2` 的写入规则；该字段继续来源于员工 `affiliation_system`。
  - 前端 `hr_web` 的高级筛选新增 `deptSelect` 多选并提交 `deptIds`。
  - 验证：`mvn -Dtest=HrmProduceAttendanceMapperXmlTest test` 通过 3 个测试；前端 `node tests/upload-attendance-page.test.mjs` 通过。
- 个税/附加导入数据模版下载（2026-07-23）：
  - 新增 `ExcelTemplateDownloadUtils`，统一从 classpath 读取固定 Excel 模板资源并设置下载响应头、Excel content type、`fileDownload=true` Cookie；资源缺失时返回 404。
  - 新增并纳入构建资源：`src/main/resources/export/个税累计.xls`、`src/main/resources/export/附加扣除累计.xls`、`src/main/resources/export/专项扣除累加表.xlsx`，来源为业务提供的 `/Users/jiangyongming/Desktop/导入模版`。
  - `HrmPersonalIncomeTaxController` 新增 `GET /hrmPersonalIncomeTax/downloadPersonalIncomeTaxTemplate`。
  - `HrmAdditionalController` 新增 `GET /hrmAdditional/downloadAdditionalTemplate`。
  - `HrmEmployeeAdditionalController` 新增 `GET /hrmEmployeeAdditional/downloadEmployeeAdditionalTemplate`。
  - 前端负责在个税累计、附加累计、年度附加扣除页面展示“下载数据模版”按钮；个税累计和附加累计导入前必须先选年月，后端导入参数契约不变。
  - 个税累计和附加累计导入接口在 controller 层兜底校验空年月，直接 API 调用传空字符串时返回中文错误，不进入导入 service。
  - 验证：`mvn -Dtest=HrmPersonalIncomeTaxControllerTest,HrmAdditionalTemplateControllerTest test` 通过 5 个测试。
- 固定月休员工应出勤修复（2026-07-23）:
  - 根因：`HrmOvertimeNightStatisticsServiceImpl#resolveExpectedAttendanceDays(...)` 仍按 `affiliationSystem` 选择应出勤公式；李凤皇当前档案为 `affiliation_system=1`、`rest_type=2`，因此 2026-06 被误按行政单双休算成 `23天/184小时`。
  - 修复后应出勤班制优先读取 `restType`：`restType=2` 走固定月休公式，`restType=1` 走单双休月历；`restType` 为空时才按 `affiliationSystem` 兼容历史员工。
  - 加班/夜班资格不变，仍必须同时满足 `affiliation_system=2` 和 `rest_type=2`；李凤皇这种行政归属、固定月休员工只按固定月休计算应出勤/实际出勤/应计出勤，不产生加班/夜班小时和补贴资格。
  - 查询接口对已显式维护 `restType` 且与所属体系旧分流冲突的历史明细，会按当前公式刷新应出勤并重新计算实际/应计出勤，避免正数旧值 `23` 持续污染页面。
  - 回归测试：`HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldUseFixedMonthlyRestAttendanceWhenRestTypeIsFixedEvenIfAffiliationIsAdministrative` 覆盖李凤皇式 `184/180/184 -> 200/196/200`；`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest,HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest test` 通过 133 个测试。
- 加班/夜班开始统计范围选择前端合并（2026-07-23）：
  - 前端 `hr_web/src/views/hrm/attendance/overtime-night/Index.vue` 删除独立“单人统计”按钮，将员工范围选择合并到“开始统计”入口。
  - “开始统计”弹窗复用薪资管理 `AloneComputeDialog.vue` 的按人员穿梭框和按部门树形选择；弹窗额外显示统计月份，关闭薪资记录 ID 必填校验，并在统计运行期间保留 loading。
  - 空范围继续调用后端 `/hrmOvertimeNightStatistics/startStatistics`，保留全员开始统计逻辑；显式范围调用 `/hrmOvertimeNightStatistics/startStatisticsForEmployee` 并提交 `employeeIds`，后端计算口径和员工范围重算逻辑不变。
  - 前端验证：`node tests/overtime-night-single-selection.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/salary-export-scope-dialog.test.mjs`、`node tests/overtime-night-page.test.mjs`、`node tests/overtime-night-api.test.mjs`、`node tests/salary-compute-scope-utils.test.mjs` 通过。
- 基本工资设置同步薪资档案基本工资（2026-07-22）：
  - `HrmSalaryBasicService#saveSalaryBasic(...)` 保存或更新 `hrm_salary_basic` 后，会调用 `syncBasicSalaryToEmployeeArchives(...)`。
  - 同步时直接批量更新已有 `hrm_salary_archives_option` 明细，并通过数据库端 `exists` 子查询限定员工仍为 `is_del=0`。
  - 更新条件固定为 `code=10101` 且 `is_pro in (0, 1)`，即同时覆盖正式工资和试用期工资中的“基本工资”；写入值为本次 `salaryBasic.toPlainString()`。
  - 本次不自动补建缺失的薪资档案或缺失的 `10101` 明细，避免为未定薪员工生成不完整档案；需要完整建档仍通过薪资档案定薪流程处理。
  - 代码审查后确认同步范围不再全量读取未删除员工 ID 后拼单个 `IN (...)` 更新，避免员工量较大时 SQL 参数过多。
  - `salaryBasic` 为空时不触发薪资档案同步，避免把空设置传播到员工薪资档案。
  - 回归测试：`HrmSalaryBasicServiceTest` 新增保存设置触发同步、空金额跳过同步和源码约束测试；`mvn -Dtest=HrmSalaryBasicServiceTest test` 通过 10 个测试；`mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest test` 通过 75 个测试。
- 员工级全勤金额设置（2026-07-22）：
  - `HrmEmployee` JPA 模型与 MyBatis Plus 员工 PO 新增 `ordinaryFullAttendanceAmount`、`leaderFullAttendanceAmount`，对应员工表 `ordinary_full_attendance_amount` 与 `leader_full_attendance_amount`。
  - 新增 SQL `docs/sql/2026-07-22_hrm_employee_full_attendance_amount.sql`，为 `hr_0001` 至 `hr_0005.hrm_employee` 增加两列 `decimal(10,2) default null`；脚本幂等，字段为空表示未设置员工级金额。
  - `HrmSalaryBasicController` 新增 `POST /hrmSalaryBasic/updateEmployeeFullAttendanceAmount`，入参为 `UpdateEmployeeFullAttendanceAmountDto(amountType, amount, employeeIds)`；`amountType=ordinary/leader` 分别更新普通/领导全勤金额，`employeeIds` 为空时更新全体未删除员工。
  - `HrmSalaryBasicService#updateEmployeeFullAttendanceAmount(...)` 通过 `HrmEmployeeMapper.update(...)` 批量写员工表；非空员工范围会去重并追加 `employee_id in (...)`，所有更新均过滤 `is_del=0`；显式员工范围更新 `0` 行会抛业务错误，避免误提示成功。
  - `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 的 `fullMoney` 改为员工表金额优先、最新基本工资金额设置兜底、最后默认 `100/500`；岗位领导判断沿用现有岗位关键字。
  - `SalaryMonthRecordServiceNew` 全勤奖写入分支统一使用计薪员工 map 的 `fullMoney`，包括市场入职默认全勤分支，不再直接调用 `HrmSalaryBasicDefaults.ordinaryFullAttendanceAmount(...)` 或 `leaderFullAttendanceAmount(...)` 绕过员工表。
  - `SalaryMonthRecordService_Bak` 也改为复用 `SalaryMonthRecordServiceNew.resolveFullAttendanceMoney(...)` 解析 `fullMoney`，兼容员工表/基本工资字段返回 `BigDecimal`，避免旧链路恢复时因 `Long` 强转崩溃。
  - `HrmEmployeeServiceImpl#queryAllEmployeeList(...)` 的员工简表补充 `mobile`，供员工级全勤金额设置和其它穿梭框区分同名员工。
  - 前端 `hr_web/src/views/manage/salary/Index.vue` 在普通/领导全勤金额后新增“设置”按钮；新增 `FullAttendanceAmountDialog.vue` 支持按人员穿梭框和按部门树选择，右侧为空时提交全员更新；弹窗员工来源为 `/hrmEmployee/queryAllEmployeeList` 返回的未删除员工，不使用计薪员工接口过滤。
  - 验证：`mvn -Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryMonthRecordServiceNewTest test` 通过 77 个测试；代码审查后追加 `mvn -Dtest=HrmSalaryBasicServiceTest,SalaryMonthRecordServiceNewTest,HrmEmployeeServiceImplTransferSimpleEmpTest test` 通过 74 个测试；前端 `node tests/salary-basic-settings-page.test.mjs` 通过；SQL 已在 dev MySQL `153.0.237.98` 执行并确认 5 个租户库字段存在。
- 员工所属体系与休息制度补贴口径实现（2026-07-22）：
  - `HrmEmployee` JPA 模型、MyBatis Plus 员工 PO、`AddEmployeeBO` 和 `QuerySalaryPageListVO` 已接入 `restType`；`HrmEmployeeMapper.xml#queryPageList` 返回 `a.rest_type as restType`。
  - 新增 SQL `docs/sql/2026-07-22_hrm_employee_rest_type.sql`，为 `hr_0001` 至 `hr_0005.hrm_employee` 增加 `rest_type int default null`，取值 `1=行政单双休`、`2=固定月休4天`；空值按无加班/夜班资格处理。
  - 加班/夜班资格统一为 `affiliationSystem=2 && restType=2`。`SalaryMonthRecordServiceNew#isFixedRestProductionEmployee(...)` 是薪资链路公共 helper，`HrmProduceAttendanceServiceImpl` 和 `HrmOvertimeNightStatisticsServiceImpl` 各自用同一口径的 `canCountOvertimeNight(...)`。
  - `HrmSalaryMonthEmpRecordMapper.xml` 的计薪员工查询、薪资列表均返回 `restType`，兼容字段 `isProduceDept` 改为只在 `affiliation_system=2 and rest_type=2` 时为 `1`；`HrmEmployeeMapper.xml#queryHasOverTimePayEmpList` 同步要求 `affiliation_system = 2 and rest_type = 2`。
  - `SalaryMonthRecordServiceNew#getYeBanAndJiaBan(...)` 对不具备资格的员工强制写 `180101=0`、`180102=0`；具备资格时，加班费优先读取考勤汇总 `overtimePay`，夜班补贴优先读取 `nightSubsidy`，缺少已落库夜班补贴时按 `nightShift * subsidy` 兜底计算。
  - 薪资导出 `resolveExportOvertimePay(...)` 与 `resolveExportNightSubsidy(...)` 对不具备资格的员工强制导出 `0`，避免历史薪资项非零继续出现在 Excel。
  - `HrmOvertimeNightStatisticsServiceImpl` 在统计明细、每日明细、月度明细和汇总 VO 中对非目标员工归零 `overtimeHours/nightShiftCount`；应出勤、实际出勤、应计出勤仍按所属体系主流程保留。
  - `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 和 Excel 导入归零非目标员工的 `workOverTime/overtimePay/nightShift/nightSubsidy`；`updateProduceAttendanceCell(...)` 保存这些字段前校验员工资格，不符合时返回“只有生产体系且固定月休4天员工才能统计加班/夜班字段”。
  - `HrmProduceAttendanceMapper.xml#queryProduceAttendanceList` 关联 `hrm_employee`，对历史非目标员工的加班/夜班字段用 SQL `case when e.affiliation_system = 2 and e.rest_type = 2 then ... else 0 end` 展示归零。
  - 活动旧薪资服务 `SalaryMonthRecordService_Bak` 仍被工资条/旧任务引用，已补 `canCountOvertimeNight` 防护，防止旧链路继续生成 `180101/180102`。
  - 验证：`mvn -DskipTests compile` 通过；`mvn -Dtest=HrmEmployeeMapperSqlTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmProduceAttendanceMapperXmlTest,HrmEmployeeServiceImplCompanyAgeTest,HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest,SalaryMonthRecordServiceNewTest test` 通过 144 个测试；相关文件 `git diff --check` 无输出。
  - 已知缺口：当前后端新增/列表/编辑固定字段可承接 `restType`，但员工档案前端字段配置、花名册导入模板若未增加“休息制度”，需要单独补齐；否则未维护员工按无加班/夜班资格生效。
- 员工体系 `deptType` 硬编码清理（2026-07-21，2026-07-22 加班/夜班资格已收紧）：
  - `SalaryMonthRecordServiceNew` 删除对 `HrmAttendanceInfoMapper` 和 `hrm_attendance_info.dept_type` 的依赖；同步考勤、核算薪资和导出薪资的应出勤均继续从员工级加班/夜班统计明细读取。
  - 薪资服务不再维护内部 `deptType=行政/生产` 映射，改为 `isProductionAffiliationSystem(...)` 直接基于计薪员工 map 中的 `affiliationSystem` 判断生产体系；未知值按行政处理。
  - `SalaryComputeContext` 和批量数据对象移除 `normalDaysByDeptType`，半路转正、全勤兜底和薪资主计算不再接收体系类型参数。
  - `HrmAttendanceInfo`、`QueryAttendanceInfoBO`、`QueryAttendanceInfoVO` 与 `HrmAttendanceInfoMapper.xml` 移除行政/生产体系类型字段和按 `dept_type` 查询的旧 SQL；保留按年月维护应出勤天数的接口结构。
  - `getYeBanAndJiaBan(...)` 原本保持 `180101 / 加班费` 的生产体系布尔防护；2026-07-22 后已升级为生产体系且固定月休 4 天共同防护，并同时覆盖 `180102 / 夜班补贴`。
  - 组织管理 `HrmDept/AddDeptBO/DeptVO` 与前端组织管理页中的 `deptType=公司/部门` 属于组织节点类型，已确认不纳入本次删除。
  - 回归测试：`AttendanceInfoSourceTest` 防止考勤天数配置重新暴露体系类型；`SalaryMonthRecordServiceNewTest` 防止薪资服务重新读取考勤配置或恢复内部体系类型。
- 员工所属体系使用点排查（2026-07-21）：
  - 员工档案字段位于 `HrmEmployee.affiliationSystem` / `hrm_employee.affiliation_system`，当前约定 `1=行政体系`、`2=生产体系`。
  - `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 与 `#querySalaryMonthList` 返回 `affiliationSystem`，并用 `affiliation_system=2` 派生兼容字段 `isProduceDept`。
  - `SalaryMonthRecordServiceNew#isProductionAffiliationSystem(...)` 直接基于 `affiliationSystem` 判断是否生产体系，缺失或未知值按行政兜底；该结果影响排班工时、病/事假折算和生产体系加班费。
  - `HrmProduceAttendanceServiceImpl#resolveDepartmentType(...)` 将员工所属体系写入考勤汇总 `department=1/2`，导入和同步都会刷新该字段，缺失时按行政兜底。
  - `HrmOvertimeNightStatisticsServiceImpl#resolveExpectedAttendanceDays(...)` 优先使用休息制度区分应出勤班制：`restType=2` 固定月休按自然月天数、生产月休天数和工作日法定休息日计算，`restType=1` 按单双休月历工作日计算；`restType` 为空时再按所属体系兼容旧分流。
  - 前端直接引用仅集中在考勤汇总页 `department=1/2` 筛选展示，以及系统设置页 `productionMonthlyRestDays`；未发现前端直接消费 `affiliationSystem` 或 `isProduceDept`。
- 薪资核算病假扣款规则定位（2026-07-21）：
  - 当前生效逻辑在 `SalaryMonthRecordServiceNew#getFixedOptionValue(...)`：病假天数 `leaveOfsickDays <= 2` 时，工资项 `19010401 / 假期扣款(病假)` 写入 `0`；病假天数大于 `2` 时，先减免 `2` 天，只对剩余天数扣款。
  - 扣款公式为 `(hrm_salary_basic.salary_basic / normalDaysDecimal) * (leaveOfsickDays - 2)`，结果 `setScale(0, RoundingMode.HALF_UP)`；病假天数来源由 `sickDeductDays(..., type="2", ...)` 计算，并可能被假期抵扣记录 `type=4` 抵扣。
  - 旧备份 `SalaryMonthRecordService_Bak` 中也有 2 天阈值，但超过 2 天后按全部病假天数扣；当前新服务已改为只扣超过 2 天的部分。
  - 2026-07-21 追加确认：病假 2 天内不扣病假工资，但仍扣全勤；有效病假天数 `>0` 时，不得因为应计出勤达到应出勤而兜底生成 `40102 / 全勤奖`。
  - 修复实现：`fillAttendanceDataForEmployee(...)` 将 `leaveOfsickDays` 提前到全勤兜底前计算，并把它传入 `shouldFallbackFullAttendanceByAccruedDays(...)`；helper 在 `sickLeaveDays > 0` 时返回 false。后续 `19010401` 仍按原病假免扣阈值计算。
  - 回归测试：`SalaryMonthRecordServiceNewTest#shouldFallbackFullAttendanceByAccruedDays_shouldRejectEffectiveSickLeaveEvenWhenAccruedReachesExpected` 覆盖张雪梅式“两天病假、应计出勤已满但不发全勤”的场景；薪资组合回归 `SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest` 通过 74 个测试。
  - 2026-07-21 追加只读排查张雪梅应发/实发工资：当前 `hr_0003` 2026-06 员工薪资记录 `2077929043646251010` 的工资项为 `10101=2130`、`10102=1570`、`281=100`、`19010401=1042`、`200101=1042`、`210101=2758.00`、`100101=466.50`、`100102=144`、`160102=13.79`、`230101=0.00`、`1001=624.29`、`240101=2133.71`，落库金额可按 `2130 + 1570 + 100 - 1042 = 2758` 和 `2758 - 624.29 = 2133.71` 闭合。
  - 本次排查发现张雪梅 2026-06 病假扣款仍有小时/天折算问题：`hrm_attendance_report_data` 中 `病假=16.00` 来源于两条各 `8小时` 病假审批，但 `sickDeductDays(..., type="2")` 的行政体系分支直接返回 `16.00`，导致按 `1860 / 25 * (16 - 2) = 1041.60 -> 1042` 生成 `19010401`。已修复为行政体系也按 `小时 / 8` 折算病假天数，并用 `SalaryMonthRecordServiceNewTest#sickDeductDays_shouldConvertAdministrativeSickLeaveHoursToDays` 覆盖。
- 吴镜平离职后仍生成 2026-06 薪资排查（2026-07-21）：
  - 本轮只读排查 `hr_0003` 数据和薪资核算员工范围 SQL，未修改薪资数据。
  - 吴镜平 `employee_id=1831601326890434575 / 手机号 13797928108 / 工号 1718539734988` 档案为 `entry_status=4`、`is_del=0`；离职记录 `plan_quit_time=2026-06-10`、`salary_settlement_time=2026-06-10`。
  - 计薪员工来源在 `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeIdList` 和 `#queryPaySalaryEmployeeList`：`entry_status in (1,3)` 直接纳入，`entry_status=4` 且 `plan_quit_time > date_sub(endTime, interval 1 month)` 也纳入。2026-06 薪资 `end_time=2026-06-30`，阈值为 `2026-05-30`，所以吴镜平命中离职员工纳入口径。
  - 6 月薪资主记录 `2077807285177995265 / 六月薪资报表` 实际已有 95 名员工明细，包含吴镜平；主表 `num=1` 与明细数不一致，不能用作当前明细人数判断。
  - 吴镜平 2026-06 考勤汇总 `positive_attendance=23.00`、`probation_attendance=23.00`，薪资明细 `need_work_day=23.00`、`actual_work_day=23.00`；离职员工逻辑将缺勤天数设为 `normalDays - actualityDays`，因此当前数据下缺勤为 `0`。
  - 工资项闭合：`10101=2130`、`10102=3070`、`40102=100`，`210101=5300.00`、`240101=5300.00`，社保、公积金、个税均为 `0`。若业务要求按离职日截断，应调整离职员工计薪范围或应计出勤来源，而不是只改导出。
- 农谷 2026-06 高温补贴与薪资档案补齐（2026-07-20）：
  - 数据来源：`/Users/jiangyongming/Downloads/薪资 (1).xlsx` 作为人员基准，对比 `/Users/jiangyongming/Desktop/农谷导入数据/薪资/薪资-手工.xlsx` 的 `6月` sheet。
  - Excel 源文件未提供手机号；匹配预览先按姓名结合入职日期、部门、岗位定位，生成和执行 SQL 时再通过 `hr_0003.hrm_employee.employee_name + mobile` 校验并更新对应 `employee_id`。
  - 考勤汇总修正：19 名员工的高温补贴差额 `100` 已补入 `hrm_produce_attendance.other_subsidies`；赵聪当前其它补贴已为目标值 `100`，未重复累加；`hrm_produce_attendance.high_temperature` 未改动。
  - 薪资档案修正：更新 `hrm_salary_archives_option` 正式工资项 `is_pro=0` 共 6 条，涉及 `10101 / 基本工资` 和 `10102 / 岗位工资`；本轮未发现需更新的 `10103 / 职务工资`。
  - 产物：预览报告 `docs/reports/2026-07-20-nonggu-salary-attendance-supplement-preview.md`，执行 SQL `docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement.sql`，回滚 SQL `docs/sql/2026-07-20_hr_0003_nonggu_salary_attendance_supplement_rollback.sql`。
  - 执行核验：20 个相关考勤汇总行（含已达目标的赵聪）`other_subsidies=100`，6 条薪资档案目标项全部等于手工表目标值；吴镜平在手工表无同名行，未处理。
  - 本轮只修正源数据，未直接修改已生成薪资月记录或工资项；需要业务重新核算/导出 2026-06 薪资后，工资表才会体现这些源数据变更。
- 张明及全员 2026-06 全勤奖漏发修复（2026-07-20）：
  - 本轮只读排查 `hr_0003 / 2026-06` 薪资数据，未直接修改已生成工资表或工资项。
  - 张明 `employee_id=1831601326890434564 / TYNG-012` 已转正、`full_attendance=1`、非残疾；薪资明细 `need_work_day=23.00`、`actual_work_day=23.00`，加班/夜班统计 `expected_attendance_days=23`、`accrued_attendance_hours=184.00`，但 `40102` 为空。
  - 数据影响面：95 名薪资员工中 76 人已有全勤奖；按“档案启用全勤、非残疾、加班/夜班应计出勤达到应出勤、但 `40102` 为空”口径有 12 人。其中吴晓霞、张明、王琪、闫倩、高文利 5 人最终考勤扣款合计为 `0`，属于应计出勤已满但被原始钉钉汇总异常挡住全勤奖；另 6 名工程部员工仍受旧 `21.75` 应出勤批次影响，需随重新核算一起复核。
  - 代码根因：`SalaryMonthRecordServiceNew#shouldFallbackFullAttendanceByAccruedDays(...)` 只在 `empAttendanceSummary == null` 时允许按应计出勤兜底；张明这类员工的 `hrm_attendance_report_data` 月度汇总存在，所以即使 `resolveSalaryAttendanceDays(...)` 得到满额应计出勤，也不会重新把 `isFullAttendance` 置为 true。
  - 修复实现：`shouldFallbackFullAttendanceByAccruedDays(...)` 改为判断薪资应计出勤天数是否达到加班/夜班统计应出勤天数，不再因钉钉月度汇总对象存在而拒绝兜底；2026-07-21 后又增加有效病假天数排除条件。既有员工正式/已转正、`full_attendance=1`、非残疾和应计出勤数据存在等资格判断仍在外层保留。
  - 2026-07-21 复核张雪梅：`employee_id=1831601326890434590 / TYNG-371` 在 2026-06 有 2 天病假审批，当前薪资 `19010401=0` 但 `40102=100.00`；修复后重新核算应保留病假工资免扣，同时不再生成全勤奖。
  - 回归测试：`SalaryMonthRecordServiceNewTest#shouldFallbackFullAttendanceByAccruedDays_shouldAllowReportAnomaliesWhenAccruedReachesExpected` 覆盖张明式“月度汇总存在缺卡/短时迟到，但应计出勤已满”的漏发场景。
- 采购计划部 2026-06 超缺勤工资排查（2026-07-20）：
  - 本轮未修改业务代码和薪资数据，仅做只读根因排查并记录结论。
  - 工资项定位：`200101 / 超缺勤` 是 `SalaryMonthRecordServiceNew#getFixedOptionValue(...)` 中生成的考勤扣款合计；采购计划部三名员工的 `200101` 全部来自 `190103 / 旷工扣款`。
  - 代码公式：`190103` 按 `empSalary / normalDays * absenteeismDays` 计算；在有钉钉月度汇总时，`absenteeismDays` 会被设置为 `normalDays - empAttendanceSummary.actualityDays`。
  - 数据根因：`hr_0003.hrm_attendance_info` 缺少 `2026-06` 配置，`loadNormalDaysByDeptType(...)` 回退默认 `21.75`；同月 `hrm_produce_attendance.probation_attendance` 和加班/夜班统计均给采购计划部员工提供 `23` 天应计出勤，导致 `21.75 - 23 = -1.25` 天形成负数旷工扣款。
  - 数据闭合：严锦、庞龙斌定薪 `4000`，`4000 / 21.75 * -1.25 = -229.89`；黎冬霜定薪 `4400`，`4400 / 21.75 * -1.25 = -252.87`，均与 `190103/200101` 落库值一致。
  - 影响面：同一 2026-06 薪资主记录中 95 名员工里 87 名 `200101` 非零、84 名为负数，说明问题不是采购计划部专属，而是该月应出勤配置缺失与薪资核算应计出勤来源不一致的系统性表现。
- 薪资核算默认应出勤天数清理（2026-07-20）：
  - `SalaryMonthRecordServiceNew#resolveSalaryExpectedAttendanceDays(...)` 统一解析薪资应出勤：只按员工读取加班/夜班统计 `expected_attendance_days`；缺失或非正数时返回空，由主流程抛出提示“单双休设置”的中文业务错误，不再读取 `hrm_attendance_info.actual_work_day` 作为兜底。
  - `fillAttendanceDataForEmployee(...)` 不再初始化或回退默认应出勤天数；无考勤组员工的 `1 / 应出勤天数` 也使用同一解析结果，`2 / 应计出勤天数` 仅在缺应计来源时回到解析出的应出勤天数。
  - 半月转正分段薪资不再用默认天数兜底；主计算上下文新增 `expectedAttendanceDaysByEmployee`，本次同步考勤必须先由加班/夜班统计取得应出勤，取不到则阻断核算。
  - 2026-07-21 追加加固：未勾选“同步考勤数据”时，薪资计算上下文也会按员工集合预加载当月 `hrm_overtime_night_statistics_detail.expected_attendance_days`；`resolveSalaryNormalDaysForCompute(...)` 只读取该员工级统计 map，不再读取旧考勤 map 的编码 `1`、历史 `needWorkDay` 或已有月薪记录。
  - 2026-07-21 追加测试：`HrmOvertimeNightStatisticsServiceImplTest#queryPageList_shouldReadAdministrativeExpectedAttendanceFromRequestedMonthWorkweekSummary` 固定行政体系必须用请求年月调用 `HrmWorkweekSettingService#queryMonthCalendar(year, month).workDays`；`SalaryMonthRecordServiceNewTest#resolveSalaryNormalDaysForCompute_shouldUseOvertimeNightExpectedDaysInsteadOfAttendanceMapValue` 固定薪资应出勤计算优先使用加班/夜班统计落库值。
  - `SalaryMonthRecordService_Bak` 与 `HrmSalaryMonthRecordServiceImpl` 注释中的旧默认天数已清理；生产源码和资源扫描 `rg -n "21\\.75|DEFAULT_NORMAL_DAYS" src/main/java src/main/resources -S` 无命中。
  - 用户追加要求：如果薪资生成或导出缺少加班/夜班统计应出勤，不再进入考勤配置兜底，直接提示到“单双休设置”维护数据；验证命令 `mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 50 个测试，薪资组合回归通过 72 个测试。
- `SalaryMonthRecordServiceNew` IDE 包/符号误报排查（2026-07-20）：
  - 结论：项目源码在 Maven/Javac 视角可正常编译，`SalaryMonthRecordServiceNew` 当前不是源码级“包找不到/符号找不到”问题。
  - 验证：`mvn -DskipTests compile` 编译 1044 个主源码文件成功；`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 43 个测试。
  - 环境修复：全局 Maven 配置 `/Users/jiangyongming/devTools/apache-maven-3.8.4/conf/settings.xml` 存在两处会影响 IDE Maven 同步的错误，已将 profile 内误写的 `<profiles>` 改为 `<properties>`，并将非法 mirror id `nexus-aliyun>` 改为 `nexus-aliyun`。修正后同一编译和定向测试仍通过，且 settings 解析警告消失。
  - 后续若 IDE 仍红：重新导入 Maven 项目、刷新 Maven 依赖并重建索引；不要按 IDE 红线直接改 `SalaryMonthRecordServiceNew` import，先用 Maven 报错确认真实缺失符号。
- 薪资导出满勤天数口径修正（2026-07-20）：
  - 根因：`SalaryMonthRecordServiceNew#exportSalaryNew(...)` 中 `9007 / 满勤天数` 曾取 `vo.getNeedWorkDay()`，上一轮又误按“应出勤时间 - 应计出勤时间”理解为满勤天数；用户确认真实口径是“满勤天数列读取应计出勤时间，超缺勤天数列为应出勤时间 - 应计出勤时间”。
  - 修复实现：导出前按本次薪资员工集合批量读取 `hrm_overtime_night_statistics_detail`；`9007 / 满勤天数` 写入 `accrued_attendance_hours / 8`，`9008 / 超缺勤天数` 写入 `(expected_attendance_days * 8 - accrued_attendance_hours) / 8`。`buildSalaryDataRow(...)` 从已准备好的 `9007/9008` 工资项取值，避免再直接取员工月薪记录字段。
  - 边界：若导出员工缺少对应年月加班/夜班统计明细，则抛出业务错误提示先到“单双休设置”维护数据，不再回退 `actualWorkDay`、`needWorkDay` 或 `hrm_attendance_info`。
  - 回归测试：`SalaryMonthRecordServiceNewTest#buildExportFullWorkDaysByEmployee_shouldReadAccruedAttendanceFromOvertimeNightStatistics` 覆盖 `192小时 -> 24.00`、`224小时 -> 28.00`；`buildExportAbsenceDaysByEmployee_shouldCalculateExpectedMinusAccruedFromOvertimeNightStatistics` 覆盖 `25天/192小时 -> 1.00`、`25天/224小时 -> -3.00`；`buildSalaryDataRow_shouldUsePreparedOvertimeNightValuesForFullAndAbsenceDays` 覆盖最终导出行读取预置 `9007/9008`。
  - 验证命令：`mvn -Dtest=SalaryMonthRecordServiceNewTest test` 通过 43 个测试；薪资组合回归 `mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过 65 个测试。
- 薪资管理导出薪资空白修复（2026-07-20）：
  - 根因：`SalaryMonthRecordServiceNew#exportSalaryNew(...)` 在员工行循环中按年月和部门类型查询 `hrm_attendance_info` 后直接读取 `actualWorkDay`；当目标年月或员工所属体系缺少考勤配置行时会在写 Excel 前空指针。
  - `HrmSalaryMonthRecordController#exportSalary(...)` 原先捕获所有异常后只 `printStackTrace()`，前端 Blob 下载链路会把这种失败表现成空白文件或空白下载效果。
  - 当前修复实现：薪资导出“满勤天数/超缺勤天数”已统一改为读取加班/夜班统计落库结果，不再按员工循环查询 `hrm_attendance_info`；因此缺少考勤配置不会再导致这两列空指针。
  - 导出 controller 不再吞掉 `exportSalaryNew(...)` 异常，服务层失败会交由框架返回失败响应，避免前端误判为成功下载。
  - 回归测试：`SalaryMonthRecordServiceNewTest` 覆盖导出列读取加班/夜班统计落库结果；`HrmSalaryMonthRecordControllerTest` 覆盖导出异常必须传播。
  - 验证命令：`mvn -Dtest=SalaryMonthRecordServiceNewTest test`、`mvn -Dtest=HrmSalaryMonthRecordControllerTest test`，并追加薪资相关组合回归。
- 薪资导出范围选择（2026-07-20）：
  - `QuerySalaryExportDto` 新增 `employeeIds`，用于接收前端显式选择的导出员工范围。
  - `SalaryMonthRecordServiceNew#resolveExportEmployeeIds(...)` 将导出候选员工与 `employeeIds` 取交集，保留候选员工顺序；未显式选择员工时继续沿用原候选范围和页面筛选条件。
  - `exportSalaryNew(...)` 在调用 `salaryMonthEmpRecordMapper.querySalaryMonthList(...)` 前应用上述过滤，继续复用 mapper 既有 `employeeIds` 条件，不新增 SQL 分支。
  - 显式选择部门由前端展开成员工 ID 后提交，后端导出不按姓名或部门名猜测员工。
  - 回归测试：`SalaryMonthRecordServiceNewTest#resolveExportEmployeeIds_shouldUseSelectedEmployeeIdsAndKeepCandidateOrder`、`resolveExportEmployeeIds_shouldFallbackToCandidatesWhenSelectionEmpty`、`resolveExportEmployeeIds_shouldReturnEmptyWhenSelectionHasNoCandidateMatch`、`salaryExportSource_shouldApplySelectedEmployeeIdsBeforeQueryingMonthList`。
  - 验证命令：`mvn -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthRecordControllerTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，69 个测试。
- 农谷社保医保方案整理与员工参保方案设置（2026-07-18）：
  - 数据来源：`/Users/jiangyongming/Desktop/农谷导入数据/副本2026年社保医保缴费明细（田野农谷分部门）.et` 的 `2026年7月89人（增1减1）`，以及 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年公积金缴费清单 (田野农谷分部门).xlsx` 的 `2026年7月89人调基数（增1减1）`。
  - 目标库：`hr_0003`。本轮未修改业务代码，仅执行数据整理 SQL。
  - 【历史实现，已由 2026-08-22 新规则覆盖】方案落库规则：`hrm_insurance_scheme` 写方案主数据，`hrm_insurance_project` 写养老、医疗、失业、工伤、生育、公积金 6 个项目；大额医疗 `15`、长期护理 `3` 继续由 `hrm_salary_basic` 固定金额配置参与列表和报表合计。当前社保方案已改为在 `hrm_insurance_project` 中保存 `医疗长期护理保险` 项目行及启用状态，方案/月度项目合计不再读取基本工资固定金额。
  - 员工匹配规则：社保表无手机号时从同月公积金表补手机号；同名多行先用部门上下文拆分，例如财务/生产两名王芳，然后最终按 `姓名 + 手机号` 命中 `hrm_employee.employee_id`。
  - 产物：预览报告 `docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview.md`，执行 SQL `docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import.sql`，回滚 SQL `docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_rollback.sql`。
  - 执行结果：新增 4 个缺失方案，复用现有精准方案 `2078034513505673217 / 社保(672.50) 公积金(320)`；新增员工社保信息 9 行，更新已有员工社保信息 76 行，合计设置 85 名员工。
  - 核验：5 个 7 月方案系统展示金额与源表一致；同一解析逻辑反查 85 个命中员工 `scheme_id` 错配为 0。
  - 未自动更新：朱君明 `13972908908` 未在在职员工表找到；宁明友、陈爱蓉、瞿顺清源文件手机号与员工表当前手机号不一致。
- 农谷参保方案按 2026-06 工资表临时重整（2026-07-18）：
  - 数据来源改为 `/Users/jiangyongming/Desktop/农谷导入数据/副本加个税版-田野农谷2026年6月工资表(1)(1).xlsx` 的 `6月` sheet；同一工作簿其他月份 sheet 未参与本轮整理。
  - 工资表仅提供姓名、部门、岗位、社保扣款、公积金扣款；社保扣款已包含长期护理 `3`，方案项目继续只写养老、医疗、失业、工伤、生育、公积金。
  - 6 月金额组合为 `466.50+144`、`445.80+144`、`672.50+144`、`672.50+320`，另有 8 行 `0+0`；其中 `672.50+320` 复用现有精确方案 `2078034513505673217`，其余 3 个 6 月精确方案新增为 `2078479646634151940`、`2078479646634151941`、`2078479646634151942`。
  - 产物：预览与执行记录 `docs/reports/2026-07-18-nonggu-insurance-scheme-import-preview-2026-06-salary.md`，执行 SQL `docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary.sql`，回滚 SQL `docs/sql/2026-07-18_hr_0003_nonggu_insurance_scheme_import_2026_06_salary_rollback.sql`。
  - 执行结果：更新 82 名员工参保方案，清空胡勇星 1 条当前方案；宁明友、陈爱蓉、瞿顺清按姓名唯一命中后以 `employee_id` 更新。
  - 核验：4 个 6 月目标方案系统展示金额与工资表一致；83 个触达员工按执行 SQL 反查 `scheme_id` 错配为 0。
  - 未自动更新：马国华、谢杰杰、程传祥仍存在同名在职候选且无法仅凭工资表唯一定位；旧 7 月方案 `2078479646634151937`、`2078479646634151939` 各剩 1 条引用，正对应马国华和程传祥。
- 薪资导出个人社保多 3 元排查（2026-07-20）：
  - 本轮未修改业务代码和薪资/社保数据，仅做只读根因排查并记录结论。
  - 导出链路定位：`SalaryMonthRecordServiceNew#exportSalaryNew(...)` 查询工资项后，`buildSalaryDataRow(...)` 将“个人社保”写为 `getValueByCode(vo.getSalary(), 100101)`，`initExportData(...)` 再写入 `hrm_salary_export.social`；导出阶段没有额外加 `3`。
  - 薪资核算社保来源：`SalaryMonthRecordServiceNew#getSocialSecurityOption(...)` 在同步社保时将 `hrm_insurance_month_emp_record.personal_insurance_amount` 原样写为工资项 `100101`。
  - 【历史实现，已由 2026-08-22 新规则覆盖】固定金额叠加点：`HrmInsuranceSchemeMapper.xml#InsuranceSchemeCount` 和 `HrmInsuranceMonthEmpProjectRecordMapper.xml#queryProjectCount` 当时都会在个人社保项目合计基础上加 `hrm_salary_basic.long_term_care_insurance_amount`，当时值为 `3.00`。当前 mapper 已删除该自动累加，改为只汇总启用项目行。
  - 数据证据：`hr_0003` 2026-06 社保月记录有 84 名正常参保员工，全部满足 `personal_insurance_amount - 员工月度参保项目个人社保合计 = 3.00`；2026-06 薪资中 83 名有社保扣款员工也全部满足 `100101 - 项目个人社保合计 = 3.00`。
  - 【历史口径，已由 2026-08-22 新规则覆盖】时序根因：2026-06 社保月记录生成于 2026-07-17，早于 2026-07-18 按 6 月工资表重整参保方案；该月记录仍使用旧方案项目合计，例如旧 `社保(466.50) 公积金(144)` 的项目个人社保合计为 `466.50`，再加长期护理后写成 `469.50`。
  - 【历史口径，已由 2026-08-22 新规则覆盖】当时员工参保关系中主流新方案已满足“项目合计减 3，展示/生成合计加 3 后等于工资表扣款”的口径，例如 `2078479646634151940 / 社保(466.50)` 的项目个人社保合计为 `463.50`，加长期护理后为 `466.50`。当前以方案项目行和 `is_enabled` 控制合计。
  - 黎冬霜专项复核：当前员工档案方案为 `2078479646634151940 / 社保(466.50) 公积金(144.00)`，但 2026-06 社保月员工记录仍挂旧方案 `2011821299616206849 / 社保(466.50) 公积金(144)`，月记录 `personal_insurance_amount=469.50`，工资项 `100101=469.50`；当前档案方案不会自动改写已生成的 6 月月记录。
  - 全员对比：按当前员工档案方案重新计算目标社保额，2026-06 有效社保月员工记录 `83` 条中 `79` 条个人社保不一致，公积金不一致 `0` 条；2026-06 薪资中非零 `100101` 的 `82` 条里 `79` 条不一致。
  - 处理建议：修正/重生成 2026-06 社保月员工记录后重新核算并导出 2026-06 薪资；不要仅在导出 Excel 层减 `3`，否则薪资项、个税专项扣除和社保记录仍不一致。
- 李明明 2026-06 工会费与个税未生成排查（2026-07-17）：
  - 工会费根因：非固定工资项初始化阶段预插入了计算汇总项默认 `0`，正式核算随后又生成真实 `160102=29.25`，列表/导出读取第一条时可能显示为 `0`。`SalaryMonthRecordServiceNew#filterNoFixedSalaryOptions(...)` 现排除 `1001/160102/210101/220101/230101/240101/250101/250102/250103/250105/270101~270106/41001`，确保这些项只由正式核算生成。
  - 个税数据根因：用户最终导入文件均使用在职李明明手机号 `17389819211`，但库内 `2026-05` 个税累计、`2026-05/06/07` 附加累计和 `2026` 年度附加配置错挂到已删除同名员工 `2033769831940079620`；另有在职员工错误重算出的 `2026-06` 个税累计 `5850/60000/992.50/0`。
  - 个税代码根因：`is_remark=2` 旧逻辑在已有上月导入累计减除费用 `25000` 时仍覆盖为 `60000`。`SalaryComputeServiceNew#resolveCumulativeDeductions(...)` 现优先使用已导入上月累计 `+5000` 续算并封顶 `60000`，只有缺少可靠导入累计时才回退备注员工全年 `60000`。
  - 数据脚本：新增 `docs/sql/2026-07-17_hr_0003_li_mingming_tax_data_repair.sql`，默认 `ROLLBACK`；脚本删除错误在职 `2026-06` 个税累计，并将 `2026-05` 个税累计、`2026-05/06/07` 附加累计、`2026` 年度附加配置迁回在职李明明。
  - 文件证据：用户提供的最终导入基础中李明明 `2026-05` 个税累计为 `58594/25000/4344.5/213.99`，附加累计合计为 `21500`；原始 `2026-06` 申报累计收入 `69859`、累计减除费用 `30000`、累计专项扣除 `5749`、累计专项附加扣除 `25800`。未在提供文件中发现 `2.4/2.40/21894` 作为源字段。
  - `14.22` 与员工手算 `2.40` 复核：当前系统按 `58594+5850` 累计收入、`25000+5000` 累计减除费用、`4344.5+672.5+320` 累计专项扣除、`21500` 累计专项附加扣除、上月已缴 `213.99` 计算，得到 `14.22`。`2.40` 需要比系统当前多扣除 `394.00`，在专项附加不变时等价于本月个人社保+公积金 `1386.50`；当前 6 月社保员工明细为 `672.50+320`，原始申报本月专项扣除为 `1404.50`，均不能直接推出 `2.40`。
  - 仅用财务原始 `202605/202606` 两表复算：`202605` 李明明表内本月税额为 `18.50`，`202606` 表内本月税额为 `16.81`；在累计预扣公式下枚举两表中合理字段组合，精确得到 `2.40` 的组合数为 `0`。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，60 个测试；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=TaxImportEmployeeMatcherTest,TaxImportServiceDuplicateNameTest test` 通过，9 个测试。
  - 限制：收尾时直连 dev MySQL 三次复核均返回 `ERROR 1040 (HY000): Too many connections`；本轮未强制清理连接或停止用户正在运行的 IDE 后端，因此未完成重算后数据库值的最终截图式复核。
- 李明明 2026-06 个税 `2.40` 支持导入数据（2026-07-20）：
  - 用户确认 `246.90/249.30` 为财务正确值，后续不再从原始表追溯；现有系统公式仍是 `本月个税 = 累计应纳税额(270106) - 截至上月累计已缴税额(250105)`。
  - 为在六月薪资核算中输出 `2.40`，生成了两份全量导入副本，只调整李明明一行：`final_import_excels/李明明6月个税2.40_个税累计_导入2026-05.xls` 与 `final_import_excels/李明明6月个税2.40_附加扣除累计_导入2026-06.xls`。
  - 计算支持值：个税累计 `69859/30000/5749/246.90`，附加累计合计 `25657.50`；系统复算为累计收入 `75709`、累计减除费用 `35000`、累计专项扣除 `6741.50`、累计应纳税所得额 `8310.00`、累计应纳税额 `249.30`、本月个税 `2.40`。
  - 已在本机 `9080 / hr_0003` 实际导入并只重算李明明：个税累计导入文件需过滤已删除员工行，否则当前导入匹配校验会因 `郑景 + 18670334666` 等删除员工失败；重算时需传 `isSyncInsuranceData=true`，确保本月社保 `672.50` 和公积金 `320` 继续进入 `270103`。
  - 最终接口复核：李明明 `2026-06` 薪资项 `230101=2.40`、`270101=75709.00`、`270102=35000.00`、`270103=6741.50`、`270104=25657.50`、`270105=8310.00`、`270106=249.30`；`hrm_personal_income_tax` 回写 `2026-06=75709/35000/6741.50/249.30`。
  - 薪资核算滚动累计口径：`SalaryMonthRecordServiceNew#doComputeSalaryData(...)` 开始阶段调用 `updateAdditionForComputeScope(...)`，按本次核算范围自动生成/更新下一月 `hrm_additional`；`SalaryComputeServiceNew#saveTaxAccumulationData(...)` 在员工工资项算完后删除并写入当前薪资月份的 `hrm_personal_income_tax`。因此 6 月工资核算后会有 7 月附加累计，以及 `end_month=6` 的个税累计；7 月工资核算时通过 `getLastMonthTaxData(..., month=7)` 精确读取 `end_month=6`，不会也不应提前生成 `end_month=7` 个税累计。
  - 2026-07-20 本机接口复核：李明明 `2026-07` 附加累计已存在，值为子女教育 `14000.00`、住房租金 `5600.00`、赡养老人 `10357.50`、合计 `29957.50`；个税累计列表存在 `2026-05` 与 `2026-06`，其中 `2026-06=75709/35000/6741.50/249.30`，等待 7 月薪资核算后才会生成 `2026-07` 行。
  - 批量整理其他员工 6 月工资个税支持数据（2026-07-20）：最终生成并导入员工表基准文件 `final_import_excels/批量6月工资个税_员工表基准_个税累计_导入2026-05.xls`、`final_import_excels/批量6月工资个税_员工表基准_附加扣除累计_导入2026-06.xls` 和预览报告。批量口径为：`2026-05` 个税累计基础取 `202607` 税款计算表累计数减 6 月工资表本月数；`2026-06` 附加累计优先取 `202606` 原始申报分项，李明明保留 `25657.50` 特殊合计；累计已缴税额按“系统累计应纳税额 - 6 月工资表个税”反推。用户修正杨晨雨身份证大小写后，按系统当前员工表为基准重新生成，员工表内成功匹配并生成 `96` 人，原始税表多出 `84` 人按要求忽略，员工表内 `17` 人因缺身份证或原始税表无对应身份证暂不导入；数据库核验 `2026-05` 个税累计 `96` 行、`2026-06` 附加累计 `40` 行；李明明、范燕东、王洪平、宁明友四名 6 月工资表个税非零员工复算均与工资表一致，杨晨雨已纳入 `2026-05` 个税累计。
  - 员工表基准未生成人员改用“姓名 + 手机号”复核（2026-07-20）：只在已有 2026 年个税累计整理文件中补匹配，不使用 2025 年历史累计或仅社保/公积金文件倒推个税累计。生成并导入 `final_import_excels/批量6月工资个税_员工表基准含姓名手机号_个税累计_导入2026-05.xls`，在 96 人版基础上新增朱玲丽 `18727608635` 一行 `10650/60000/3031.80/0`；附加累计文件同后缀复制原 40 行。导入后数据库核验 `2026-05` 个税累计 `97` 行、`2026-06` 附加累计 `40` 行。
  - 该处理未修改业务代码；若后续要严格按 `202607` 原始表的专项附加合计 `30100.00` 计算，则现有系统公式会得到本月个税 `0.00`，需要另行定义“税局累计已缴与工资表实际已扣差额补扣”的业务规则和数据字段。
- 李明明 2026-06 全勤奖未计入排查（2026-07-17）：
  - 本轮未修改薪资数据；代码侧补齐薪资全勤奖兜底判定。
  - 最新薪资主记录 `2077807285177995265` 中，李明明 `employee_id=1831601326890434563` 的薪资行 `actualWorkDay=23.00`、`needWorkDay=21.75`，工资项无 `40102 全勤奖`，`210101 应发工资=5350.00` 仅包含基本工资 `2130` 和岗位工资 `3220`。
  - 计薪员工查询显示李明明 `fullMoney=500.00`、`isFullAttendance=1`、`status=1`、`becomeTime=2025-07-01`，说明全勤资格、领导全勤金额和转正状态都正常。
  - 考勤汇总显示 `actualAttendance=20.00`、`accruedAttendance=23.00`、`workOverTime=8.00`；加班/夜班统计显示应出勤 `23` 天、实际出勤 `160.00` 小时、应计出勤 `184.00` 小时，备注为 `扣除：年假32.00小时`。
  - 代码根因：`SalaryMonthRecordServiceNew#fillAttendanceDataForEmployee(...)` 只有 `checkIsFullAttendance(...)` 返回 true 后才会写入 `40102`；而 `checkIsFullAttendance(...)` 在 `empAttendanceSummary == null` 时直接返回 false。后续即使 `resolveSalaryAttendanceDays(...)` 从考勤汇总/加班夜班统计得到满额应计出勤并写入 `actualWorkDay`，也不会重新把 `isFullAttendance` 置为 true。
  - 修复实现：`AttendanceSyncBatchData` 现在同时缓存加班/夜班统计明细中的应出勤天数和应计出勤天数；`fillAttendanceDataForEmployee(...)` 在旧报表月度汇总缺失时，若薪资应计出勤天数达到加班/夜班统计应出勤天数，则兜底判为满勤并允许后续写入 `40102`。
  - 当时边界：旧报表月度汇总存在时仍由 `checkIsFullAttendance(...)` 处理迟到超 30 分钟、缺卡、旷工、事假、病假、早退等扣全勤条件；该边界已在 2026-07-20 后续全勤兜底修复中放宽，2026-07-21 又追加有效病假排除条件。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 通过，38 个测试；薪资相关回归 `SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest` 通过，57 个测试；`HrmProduceAttendanceServiceImplTest` 通过，9 个测试。
- 员工管理唯一性与表单字段调整（2026-07-17）：
  - `HrmEmployeeServiceImpl#validateEmployeeUniqueFields(...)` 统一校验未删除员工的 `jobNumber/mobile/idNumber` 唯一；编辑和确认入职保存时排除当前员工。
  - `transferEmployee(...)` 接入统一唯一性校验，因此新增员工和动态新增员工都会在保存前校验工号、手机号、身份证号；新增员工不再把前端传入工号覆盖成时间戳。
  - 员工花名册导入 `importEmployee(...)` 先读取并校验整份文件，再统一落库；同一导入文件中重复的工号、手机号、身份证号会在任何员工保存前被拦截，导入方法仍保留事务兜底后续动态字段保存失败。
  - 通讯信息保存 `updateCommunication(...)` 同样复用统一唯一性校验，避免通过通讯页修改手机号时绕过重复校验。
  - 身份证号归一化为去空格并大写后比较，手机号归一化去除空格和 `-` 后比较；空值不参与对应字段校验。
  - 前端 `hr_web` 新增/编辑页面删除“是否有全勤 / 是否加入钉钉”表单项，后端模型字段暂保留兼容历史数据和既有表结构。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmEmployeeServiceImplUniqueValidationTest,HrmEmployeeServiceImplImportEmployeeTest,HrmEmployeeControllerTest,HrmEmployeeServiceImplEducationExperienceTest,HrmEmployeeServiceImplQueryPageListTest test` 通过，16 个测试。
- 薪资行政/生产体系硬编码移除（2026-07-17）：
  - `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 和 `querySalaryMonthList` 现在返回 `affiliationSystem`；2026-07-22 后同时返回 `restType`，兼容字段 `isProduceDept` 由 `hrm_employee.affiliation_system=2 and rest_type=2` 派生，不再按部门名称列表判断生产体系。
  - 2026-07-21 后，`SalaryMonthRecordServiceNew#isProductionAffiliationSystem(...)` 直接将 `affiliation_system=2` 识别为生产体系，其他值均按行政处理；不再维护薪资内部行政/生产类型映射。
  - `computeEmployeeSalary(...)`、`fillAttendanceDataForEmployee(...)`、`exportSalaryNew(...)` 已全部复用统一 helper；删除了旧员工 ID 强制行政/生产、生产体系特定双休员工名单、未使用的生产 `-4` 应出勤函数，以及考勤天数配置兜底。
  - 活动旧服务 `SalaryMonthRecordService_Bak` 仍被自动任务/工资条服务注入，因此先同步改为复用 `SalaryMonthRecordServiceNew.isProductionAffiliationSystem(...)`，2026-07-22 后又补充 `isFixedRestProductionEmployee(...)` 防护，并删除旧生产 `-4` 逻辑和部门名注释。
  - `HrmProduceAttendanceServiceImpl` 的考勤汇总导入与“加班/夜班统计 -> 考勤汇总”同步均只读取员工 `affiliationSystem` 写入 `department=1/2`；缺失所属体系时按行政兜底，不再按部门名称关键字推断。
  - `HrmEmployeeMapper.xml#queryHasOverTimePayEmpList` 先改为 `affiliation_system = 2`，2026-07-22 后收紧为 `affiliation_system = 2 and rest_type = 2`，删除部门/岗位名称例外列表。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmProduceAttendanceServiceImplTest,HrmEmployeeMapperSqlTest test` 通过，50 个测试；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmEmployeeMapperSqlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest,HrmProduceAttendanceServiceImplTest test` 通过，67 个测试。
- 薪资核算应计出勤天数来源调整（2026-07-17）：
  - `SalaryMonthRecordServiceNew` 中工资项编码 `2` 和 `HrmSalaryMonthEmpRecord.actualWorkDay` 的业务口径调整为“应计出勤天数”；字段名保持不变，避免改动历史表结构和 mapper 契约。
  - 勾选“同步考勤数据”核算时，`loadAttendanceSyncBatchData(...)` 会额外读取 `hrm_overtime_night_statistics_detail`，并通过 `buildAccruedAttendanceDaysByEmployee(...)` 将 `accruedAttendanceHours / 8` 聚合为员工月度应计出勤天数兜底 map。
  - `resolveSalaryAttendanceDays(...)` 按优先级解析计薪出勤天数：先用考勤汇总 `HrmProduceAttendance.probationAttendance`，为空或无汇总行时再用加班/夜班统计明细兜底；考勤汇总为 `0` 时视为明确值，不回退。
  - `fillAttendanceDataForEmployee(...)` 在写入编码 `2`、创建/更新员工月薪记录、满勤/缺勤判断时使用该应计出勤天数口径；原 `positiveAttendance` 仍只保留在半路转正分段和既有应出勤兼容逻辑中。
  - 薪资列表、导出表头、前置校验和 `HrmSalaryMonthEmpRecord` API 描述同步显示“应计出勤天数”。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest test` 通过，32 个测试；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，50 个测试；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，8 个测试。
- 社保报表生成真实进度提示（2026-07-17）：
  - `HrmInsuranceMonthRecordController` 新增 `POST /hrmInsuranceMonthRecord/queryComputeInsuranceProgress`，返回 `progress/status/stage/message/processedCount/totalCount/done/success`。
  - 新增 `InsuranceComputeProgressVO`，状态字段为 `IDLE/RUNNING/SUCCESS/FAILED`，阶段字段为 `PREPARE/LOAD_EMPLOYEE/GENERATE_EMP/PERSIST/FINISH/ERROR`。
  - `HrmInsuranceMonthRecordService#computeInsuranceData()` 保持原同步生成接口契约，但在执行期间按当前租户 + 当前用户维护内存进度状态；员工明细循环由 `forEach` 改为普通循环，每生成一名员工即按 `processedCount/totalCount` 更新真实百分比。
  - 生成完成写入 `SUCCESS/FINISH/100%`，异常写入 `FAILED/ERROR` 与中文错误文案；Controller 异常时改为 `Result.Error(ax)`，避免失败仍返回成功。
  - 进度状态增加 1 小时过期清理，避免内存状态无限堆积；当前仍是应用内存级，服务重启后历史进度不可恢复。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordControllerTest test` 通过，5 个测试。
- 薪资核算失败集中提示（2026-07-17）：
  - 根因定位：`SalaryMonthRecordServiceNew#getOrCreateRecordAndApplyAttendance(...)` 原先直接按工号从 `attendanceDataMap` 取考勤 map，并读取 `1=应出勤天数`、`2=实际出勤天数`；计薪员工缺工号或考勤汇总缺字段时会触发空指针/数字转换异常，再被泛化为“考勤数据错误”。
  - `SalaryComputeProgressVO` 新增 `errors` 明细列表，`queryComputeProgress(...)` 返回失败问题明细，进度状态仍按租户 + 薪资月记录 + 核算范围保存。
  - `SalaryMonthRecordServiceNew#collectComputePrerequisiteErrors(...)` 在核算前聚合员工缺工号、缺员工姓名/ID、缺考勤汇总、应出勤/应计出勤为空或不是数字、缺考勤扣款规则、缺基本工资关键金额等问题；`collectInsuranceDataErrors(...)` 将同步社保前置错误纳入同一批提示。
  - `failComputePrecheck(...)` 会把汇总后的错误写入进度状态并抛中文业务异常；`resolveComputeErrorMessage(...)` 优先使用 `HrmException#getMsg()`，避免把 `6001:` 这类技术前缀透给用户。
  - `getOrCreateRecordAndApplyAttendance(...)` 改为通过 `requireAttendanceCodeValueMap(...)` 和 `readAttendanceDecimal(...)` 读取考勤数据，运行期仍遇到缺工号/缺考勤/非数字时返回带员工 ID 和工号的中文业务错误。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,SalaryMonthRecordInsuranceValidationTest,HrmSalaryMonthEmpRecordMapperXmlTest,SalaryComputeServiceNewTest,SalaryComputeContextTest test` 通过，47 个测试。
- 薪资管理开始核算范围选择（2026-07-16）：
  - `HrmSalaryMonthRecordController#computeSalaryData` 新增可选 `employeeIds` 请求参数；旧 `employeeId` 单人参数继续兼容。
  - `SalaryMonthRecordServiceNew#computeSalaryData(...)` 统一规范化核算范围：`employeeIds` 优先，未传时回退旧 `employeeId`，两者都为空时按全员核算。
  - 后端统一限制显式选择员工范围最多 `50` 人，超过时抛出中文业务错误“穿梭框右侧人员不能超过50人”。
  - `queryComputeProgress` 同步支持 `employeeIds`，批量核算进度按同一员工范围 key 查询；空范围继续使用全员 key。
  - `queryPaySalaryEmployeeList` 补充返回 `mobile`，供前端同名员工按“姓名 + 手机号”展示识别。
  - 前端部门模式在提交前将所选部门及子部门展开为员工 ID 集合，后端不新增部门核算分支，统一复用员工范围核算。
  - `2026-07-17` 前端补充调整：按部门选择界面从扁平部门穿梭框改为 `el-tree` 树形选择；左侧树形勾选部门，右侧树形展示已选部门，提交契约仍是展开后的 `employeeIds`。
  - `2026-07-21` 前端补充加固：开始核算弹窗按人员和按部门都以 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList` 为唯一员工来源；部门树会按该可核算员工集合过滤，只保留自身或下级部门内存在可核算员工的部门节点，避免普通员工列表或空部门混入核算范围。
  - `2026-07-21` 前端补充说明：开始核算弹窗在“核算方式”下方展示当月计薪人员规则说明，文案对应后端 `queryPaySalaryEmployeeList` 和 `queryHasSalaryArchivesEmployeeList` 口径；共享范围弹窗通过可选 `ruleTip` 渲染，导出薪资弹窗不传该说明。
  - `2026-07-21` 追加：规则说明改为面向业务用户的短句分段表达，将“薪资结束日”解释为“本次薪资月份的最后一天”，并用 `6 月薪资中，张三 6 月 10 日计划离职且有薪资档案` 的例子说明员工纳入计薪人员列表的条件。
  - `2026-07-21` 追加收敛：前端 `SalaryManage.vue` 将开始核算规则从单段长字符串改为 4 条短提示；`AloneComputeDialog.vue` 支持 `ruleTip` 传入数组，并通过 `compute-rule-tip__item` 逐行渲染，导出薪资弹窗仍不传该说明。
  - `2026-07-21` 本次文案验证：前端 `node tests/salary-start-compute-dialog.test.mjs`、`node tests/salary-export-scope-dialog.test.mjs`、`npm run build` 通过；构建仍仅有既有 `::v-deep` 过时警告和 chunk size warning。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryMonthRecordServiceNewTest,HrmSalaryMonthEmpRecordMapperXmlTest test` 通过，28 个测试；前端 `node tests/salary-compute-scope-utils.test.mjs`、`node tests/salary-start-compute-dialog.test.mjs`、`node tests/compute-progress-utils.test.mjs`、`npm run build` 通过。
- 个税/附加导入重名员工匹配修复（2026-07-16）：
  - 新增 `TaxImportEmployeeMatcher`，统一处理个税累计、附加累计、年度附加扣除导入的员工匹配：优先姓名+手机号，姓名唯一时兼容旧模板，重名缺手机号或姓名+手机号找不到时抛业务错误。
  - `HrmPersonalIncomeTaxService#resolvePersonalIncomeTaxData(...)` 改为读取模板第 8 列手机号，并按 `employee_id + year + end_month` 校验导入文件内重复。
  - `HrmAdditionalService#resolveAdditionalData(...)` 改为读取模板第 10 列手机号，并按 `employee_id + year + month` 校验导入文件内重复。
  - `HrmEmployeeAdditionalService#resolveEmployeeAdditionalData(...)` 改为读取模板第 8 列手机号，并按 `employee_id + year` 校验导入文件内重复；同时修复多年份文件只删除最后一个年份旧数据的问题。
  - 三个导入 Controller 在 catch 中改为 `return Result.Error(ax)`，避免导入失败仍返回成功。
  - 已用用户提供的原始申报目录和 `final_import_excels` 核对三名王芳：最终模板手机号完整且唯一，王芳个税累计 `29461/19281/32031` 分别归属 TYNG-107/TYNG-437/TYNG-393。
  - 已生成报告 `docs/reports/2026-07-16-tax-import-duplicate-name-repair.md` 和默认回滚修复 SQL `docs/sql/2026-07-16_hr_0003_tax_import_duplicate_name_repair.sql`；用户授权后已用临时 `COMMIT` 正式执行，王芳 2026-05 个税累计已按 TYNG-107/TYNG-437/TYNG-393 分开归属。
  - 已新增并执行 `docs/sql/2026-07-16_hr_0003_employee_additional_duplicate_cleanup.sql`，清理旧年度附加导入造成的 2025 年 `hrm_employee_additional(employee_id, year)` 历史重复；孙小虎 2025 年按最终导入模板保留 `housing_loan_interest=12000.00`、`raising_girls=2000.00`。
  - 数据修复后复核：`hrm_personal_income_tax(employee_id,year,end_month)`、`hrm_additional(employee_id,year,month)`、`hrm_employee_additional(employee_id,year)` 三类重复计数均为 0。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=TaxImportEmployeeMatcherTest,TaxImportServiceDuplicateNameTest test` 通过，9 个测试。
- 薪资核算个税备注场景实施（2026-07-16）：
  - `SalaryComputeServiceNew#resolveCumulativeDeductions(...)` 统一计算累计减除费用：`hrm_employee.is_remark=2` 时固定 `60000`，否则按 `5000 * 计薪月份`。
  - `SalaryComputeServiceNew#computeSalary(...)` 和 `SalaryMonthRecordServiceNew#calculateMidMonthPromotionSummary(...)` 均使用该口径；备注员工不再按收入低于 60000 直接免扣个税，累计应纳税所得额为正时仍按七级税率计算。
  - 1 月重新开始本年度累计；12 月不再重置个税累计，继续累计全年数据。
  - 批量核算链路仍通过 `computeSalaryFromMemory(...) -> SalaryComputeServiceNew#computeSalary(...)` 生成 `230101` 和 `270101~270106`，并由 `saveTaxAccumulationData(...)` 写回 `hrm_personal_income_tax(employee_id, year, end_month)`，供下月读取。
  - 已移除批量核算上下文中旧备注免税规则所需的上一年累计收入加载，避免新口径下继续执行无意义查询。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=SalaryComputeServiceNewTest,SalaryMonthRecordServiceNewTest,SalaryComputeContextTest test` 通过，37 个测试。
- 附加累计列表按年-月筛选（2026-07-16）：
  - `QueryAdditionalBO` 新增 `year/month` 查询字段，承接前端“选择年-月”拆分后的筛选值。
  - `HrmAdditionalMapper.xml#queryAdditionalList` 新增 `data.year/data.month` 可选过滤条件；员工姓名条件同步改为非空时拼接，避免空值误入 SQL。
  - 导入接口 `HrmAdditionalController#importAdditional` 与 `HrmAdditionalService#resolveAdditionalData(...)` 原本已按 `year/month` 写入和覆盖导入数据，本轮未改变导入契约。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmAdditionalMapperXmlTest test` 通过。
- 个税计算基础支持数据整理（2026-07-16）：
  - 数据来源：`/Users/jiangyongming/Desktop/农谷导入数据/湖北田野农谷生物科技有限公司_综合所得申报`，覆盖 `2025-11`、`2025-12`、`2026-05`、`2026-06` 四个月综合所得申报 `.xls`。
  - 目标租户确认：`hr_0003 / 0003 / 湖北田野农谷生物科技有限公司`。
  - 输出目录：`/Users/jiangyongming/Desktop/农谷导入数据/个税计算基础支持数据整理_20260716`，包含幂等 SQL、核对报告、明细 CSV、未匹配员工清单和系统导入列位参考 Excel。
  - 字段口径：`hrm_personal_income_tax` 取申报表累计收入额、累计减除费用、累计专项扣除、已缴税额；`hrm_additional` 取累计专项附加扣除分项；`hrm_employee_additional` 以相邻月份累计差额推导月度配置。
  - 匹配口径：只按身份证号解析到 `hr_0003.hrm_employee.employee_id`，未匹配行不写 SQL；多条同身份证时仅在只有一条未删除员工的情况下自动选该员工。
  - 本次未直接执行导入 SQL；已将生成 SQL 中的 `COMMIT` 替换为 `ROLLBACK` 做 MySQL 事务语法验证，返回码 0。
  - 最终导入模板文件输出到 `.../个税计算基础支持数据整理_20260716/final_import_excels`：
    - `个税累计.xls` 基于 `/Users/jiangyongming/Desktop/导入模版/个税累计.xls`，填入 `2026-05` 个税累计 100 行；
    - `附加扣除累计.xls` 基于 `/Users/jiangyongming/Desktop/导入模版/附加扣除累计.xls`，填入 `2026-05` 附加累计 41 行；
    - `专项扣除累加表.xlsx` 基于 `/Users/jiangyongming/Desktop/导入模版/专项扣除累加表.xlsx`，填入 `2025/2026` 年度固定附加扣除值 64 行；
    - 三份文件均补齐手机列；校验结果为手机空值 0，个税/附加姓名+手机重复 0，年度姓名+手机+年份重复 0。
- 基本工资金额设置新增社保固定金额配置（2026-07-17）：
  - `HrmSalaryBasic`、`QuerySalaryBasicDto`、`QuerySalaryBasicVO` 新增 `largeMedicalInsuranceAmount`、`longTermCareInsuranceAmount`，对应 `hrm_salary_basic.large_medical_insurance_amount` 与 `long_term_care_insurance_amount`。
  - `HrmSalaryBasicDefaults` 统一维护默认值：大额医疗保险金额 `15`、长期护理保险金额 `3`；`saveSalaryBasic(...)`、`findAll()`、`queryById(...)` 继续按旧数据缺字段兜底。
  - 【已废弃，2026-08-22 新规则覆盖】`HrmInsuranceSchemeMapper.xml` 与历史拼写 mapper `HrmInsuranceSechemeMapper.xml` 的社保方案合计均读取最新 `hrm_salary_basic`：个人社保合计加长期护理金额，公司社保合计加大额医疗金额。
  - 【已废弃，2026-08-22 新规则覆盖】`HrmInsuranceMonthRecordService#computeInsuranceData()` 继续通过 `queryInsuranceSchemeCountById(...)` 生成员工月度社保记录，因此次月报表生成自动复用上述合计口径。
  - 【已废弃，2026-08-22 新规则覆盖】`HrmInsuranceMonthEmpProjectRecordMapper.xml#queryProjectCount` 同步加入固定金额，避免社保月详情手工修改员工参保项目后回写合计时丢失长期护理/大额医疗。
  - 当前社保合计 SQL 只按 `hrm_insurance_project` / `hrm_insurance_month_emp_project_record` 中已启用项目行汇总，不再读取 `large_medical_insurance_amount` 或 `long_term_care_insurance_amount`。
  - SQL 脚本：`docs/sql/2026-07-17_hrm_salary_basic_insurance_amount_settings.sql`，已在 dev MySQL `153.0.237.98` 执行；`hr_0001` 至 `hr_0005` 均存在 `large_medical_insurance_amount decimal(10,2) default 15.00` 与 `long_term_care_insurance_amount decimal(10,2) default 3.00`。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmSalaryBasicServiceTest,HrmInsuranceSchemeMapperXmlTest,HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordControllerTest,HrmInsuranceMonthRecordMapperXmlTest test` 通过，13 个测试。
- 基本工资金额设置新增全勤和生产月休配置（2026-07-16）：
  - `HrmSalaryBasic`、`QuerySalaryBasicDto`、`QuerySalaryBasicVO` 新增 `ordinaryFullAttendanceAmount`、`leaderFullAttendanceAmount`、`productionMonthlyRestDays`。
  - `HrmSalaryBasicDefaults` 统一维护默认值：普通员工全勤金额 `100`、领导全勤金额 `500`、生产体系员工月度休息天数 `4`。
  - `HrmSalaryBasicService#saveSalaryBasic(...)` 保存前补默认值；`findAll()` 和 `queryById(...)` 对旧数据或空配置补默认值，其中 `findAll()` 按 `createTime/id` 选择最新一条配置。
  - `HrmSalaryMonthEmpRecordMapper.xml#queryPaySalaryEmployeeList` 读取最新 `hrm_salary_basic` 的全勤金额，领导类岗位缺失时兜底 `500`，普通员工缺失时兜底 `100`；`SalaryMonthRecordServiceNew` 读取 `fullMoney` 时按 `BigDecimal` 处理，避免配置金额不是整数类型时转换异常。
  - `HrmOvertimeNightStatisticsServiceImpl#queryProductionExpectedAttendanceDays(...)` 将生产体系月休天数从硬编码 `4` 改为读取最新基本工资金额设置，缺失时继续兜底 `4`；法定休息日额外扣减规则不变。
  - SQL 脚本：`docs/sql/2026-07-16_hrm_salary_basic_attendance_settings.sql`，已在 dev MySQL `153.0.237.98` 执行；`hr_0001` 至 `hr_0005` 均存在 `ordinary_full_attendance_amount decimal(10,2) default 100.00`、`leader_full_attendance_amount decimal(10,2) default 500.00`、`production_monthly_rest_days int default 4`。
  - 验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml '-Dtest=HrmSalaryBasicServiceTest,HrmSalaryMonthEmpRecordMapperXmlTest,HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldUseConfiguredProductionMonthlyRestDaysForProductionEmployee' test` 通过，6 个测试；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，53 个测试。
  - 本机默认 Maven 全局 settings 仍会导致 `com.aliyun:tea:[1.1.14,2.0.0)` 版本范围解析失败；本次验证用最小 user/global settings 隔离该环境问题。
- 考勤汇总同步加班/夜班统计（2026-07-15）：
  - `HrmProduceAttendanceController` 新增 `POST /hrmProduceAttendance/syncFromOvertimeNightStatistics` 和 `POST /hrmProduceAttendance/updateCell`。
  - `HrmProduceAttendanceServiceImpl#syncFromOvertimeNightStatistics(...)` 复用 `IHrmOvertimeNightStatisticsService#queryPageList(...)` 获取所选月份月度汇总；实际出勤小时与应计出勤小时统一按 `/8` 转为考勤汇总天数字段，分别写入 `positiveAttendance` 与 `probationAttendance`。
  - 2026-07-17 出勤天数来源修正：同步开始时会按所选年月额外查询 `hrmOvertimeNightStatisticsDetailRepository#findAllByStatYearAndStatMonthOrderByWorkDateDescEmployeeIdAsc(...)`，按员工聚合已落库统计明细；`positiveAttendance` 优先使用落库 `actualAttendanceDays * 8 / 8`，`probationAttendance` 优先使用落库 `accruedAttendanceHours / 8`。其它字段仍沿用 `queryPageList(...)` 的原有月度 VO 口径；若月度 VO 缺少员工但明细表存在，则使用聚合明细作为兜底同步行。
  - `QueryOvertimeNightStatisticsPageVO` 新增 `attendanceOvertimeHours`，由 `HrmOvertimeNightStatisticsServiceImpl#toPageVO(...)` 复用实际/应计出勤模块的可计入加班口径填充；行政/生产体系为本地钉钉审批加班，其他未明确体系回退月度展示加班。
  - 考勤汇总同步 `workOverTime` 优先使用 `attendanceOvertimeHours`，字段为空时才兼容回退 `overtimeHours`；这避免张明 `2026-06` 这类展示加班 `47.72` 但无审批加班的行政体系员工被同步出错误加班工资。
  - 同步时按 `employeeId + year + month` upsert `hrm_produce_attendance`；已有行更新出勤、加班小时、加班工资、夜班次数、夜班补贴字段，保留借款、其他补贴、其他扣款、备注等人工维护字段；新增和更新行均按员工档案 `affiliationSystem=1/2` 写入 `department`，缺失所属体系时按行政兜底。
  - 2026-07-17 修复潘红琼这类员工在部门筛选下看起来未同步的问题：同步已有行和新增行都会刷新 `employeeName/department`；`department` 使用员工档案 `affiliationSystem=1/2`，不再回退部门名称推断。既有错误分类行在重新同步后会被纠正，不影响借款、补贴、扣款、备注等人工维护字段。
  - `HrmProduceAttendanceServiceImpl` 新增读取最新 `hrm_salary_basic` 设置：`overtimePay` 作为每小时加班费、`subsidy` 作为夜班补贴单价；同步时写入 `overtimePay = workOverTime * overtimePay` 与 `nightSubsidy = nightShift * subsidy`，无设置时分别兜底 `12元/小时` 与 `30元/次`。
  - `HrmProduceAttendance` 新增 `overtimePay`，`QueryMonthAttendanceVO` 新增 `accruedAttendance/overtimePay`，`HrmProduceAttendanceMapper.xml` 不再对列表使用裸 `select *`，改为显式列和别名。
  - 2026-07-17 追加修复“远端还是没有/字段为空”：远端接口可查到潘红琼记录，但返回 `actualAttendance/accruedAttendance/overtimePay=null`，而数据库同一行 `positive_attendance=21.00`、`probation_attendance=25.00`；本地旧 `target/hrsystem-0.0.1-SNAPSHOT.jar` 内 mapper 仍是 `select *`，说明发布包未重新打入最新 mapper。现已将列表字段全部显式别名为 VO 驼峰字段，并新增 `HrmProduceAttendanceMapperXmlTest` 锁定该契约。
  - 2026-07-17 本地复核：`127.0.0.1:9080` 当前由 IntelliJ 运行 `target/classes`，`127.0.0.1:8081` 服务的 `hr_web/html` 已包含新 `Upload-BbctS4ME.js`；本地 8081 API 查询 `2026-06 + department=2 + 潘红琼` 返回 `actualAttendance=21.00/accruedAttendance=25.00`。`2026-07` 在同步前考勤汇总无记录，但上游加班/夜班统计已有月度结果；调用 `syncFromOvertimeNightStatistics` 同步 `2026-07` 返回 `113` 条后，潘红琼返回 `actualAttendance=27.00/accruedAttendance=27.00`。
  - 2026-07-17 界面 DOM 复核：无界面 Chrome 打开本地 `#/hrm/attendance/upload` 后，搜索“潘红琼”会触发页面请求并返回/渲染 3 行；`2026-07 + department=1 + 潘红琼` 返回 0，`2026-07 + department=2 + 潘红琼` 返回 1。潘红琼当前按员工档案 `affiliation_system=2` 展示在“生产部门”，而不是按部门名称“行政人力资源部”展示在“行政部门”。
  - `updateProduceAttendanceCell(...)` 使用字段白名单，只允许保存实际出勤、应计出勤、加班小时、加班工资、夜班次数、夜班补贴、借款、其他补贴、其他扣款、备注等业务字段。
  - `SalaryMonthRecordServiceNew#getYeBanAndJiaBan(...)` 在生产体系且固定月休 4 天员工存在 `overtimePay` 时优先使用该金额作为 `180101` 加班费，否则继续按原有加班小时乘单价计算；夜班补贴同样受该资格限制，并在 `nightSubsidy` 缺失时按 `nightShift * subsidy` 兜底。
  - SQL 脚本：`docs/sql/2026-07-15_hrm_produce_attendance_overtime_pay.sql`。
  - SQL 执行：已在 dev MySQL `153.0.237.98` 执行脚本，`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 均存在 `hrm_produce_attendance.overtime_pay decimal(10,2)`。
  - 验证：`mvn -Dtest=HrmProduceAttendanceServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，58 个测试；`mvn -Dtest=HrmProduceAttendanceServiceImplTest,SalaryMonthRecordServiceNewTest#getYeBanAndJiaBan_shouldPreferUploadedOvertimePayWhenPresent test` 通过，7 个测试；`node tests/upload-attendance-page.test.mjs` 通过。2026-07-17 追加验证：`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -Dtest=HrmProduceAttendanceServiceImplTest test` 通过，8 个测试；`HrmProduceAttendanceMapperXmlTest` 先失败于 `work_over_time` 未显式别名，修复后通过；`mvn -gs /tmp/codex-empty-maven-settings.xml -s /tmp/codex-empty-maven-settings.xml -DskipTests package` 通过，JAR 内 mapper 已包含显式别名。
- 加班/夜班统计单人统计批量化并对齐开始统计口径（2026-07-15）：
  - `QueryOvertimeNightStatisticsPageBO` 新增 `employeeIds`，保留旧 `employeeId` 作为单员工兼容字段；`startStatisticsForEmployee(...)` 会优先使用非空 `employeeIds`，仅在列表为空时回退旧字段。
  - `HrmOvertimeNightStatisticsServiceImpl#startStatisticsForEmployee(...)` 支持一次请求处理多个选中员工；批量查询员工后按前端提交顺序重算，逐个执行该员工当月旧明细替换，未选中的员工明细不受影响。
  - 单人统计入口与“开始统计”共用同一个默认月度明细计算入口，统计来源、审批优先、应出勤、实际出勤和应计出勤业务逻辑保持一致；两个入口的差异只保留在员工范围。
  - 接口返回合并后的选中员工月度明细；单选员工场景继续满足前端打开该员工明细弹窗的旧交互。
  - 回归测试新增 `startStatisticsForEmployee_shouldCalculateAllSelectedEmployeesInBatch`，验证批量选中员工一次请求即可分别删除并重算各自当月明细。
  - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，51 个测试；`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，111 个测试。
- 审批数据按主键删除（2026-07-15）：
  - `HrmAttendanceApprovalController` 新增 `POST /hrmAttendanceApproval/delete`，请求体为 `DeleteAttendanceApprovalBO(approvalId)`。
  - `HrmAttendanceApprovalServiceImpl#deleteApproval(...)` 仅通过 `approvalRepository.findById(approvalId)` 查找目标快照，再删除该实体；不按员工姓名、部门、时间、类型或月份执行删除，避免同名员工误删。
  - 删除操作只维护本地 `tbattendanceapprove`，不调用钉钉接口；若 `approvalId` 为空或记录不存在，返回中文业务错误。
  - 验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，30 个测试；`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，108 个测试。
- 审批数据手工添加（2026-07-15）：
  - `HrmAttendanceApprovalController` 新增 `POST /hrmAttendanceApproval/addManual`，请求体为 `AddAttendanceApprovalBO(employeeIds, tagName, subType, beginTime, endTime, approvalRanges)`；`approvalRanges[]` 支持一次提交多个非连续审批日期/时间段，旧 `beginTime/endTime` 单段入参继续兼容。
  - `HrmAttendanceApprovalServiceImpl#addManualApproval(...)` 不调用钉钉接口，只写本地 `tbattendanceapprove`；按“员工 × 审批时间段”保存独立本地审批快照，例如 1 名员工 2 个调休日期会保存 2 条记录。
  - 手工审批 ID 使用 `MANUAL-` 前缀，避免与钉钉流程实例 ID 冲突。
  - 保存前通过 `tbattendanceuserRepository.findAllByEmpIdIn(...)` 校验员工已有考勤映射，并使用映射中的 `userId/groupId` 写入审批快照；缺映射时返回中文错误，避免列表无法关联员工或统计无法读取。
  - 当同一员工返回多条 `tbattendanceuser` 历史映射时，手工添加按 `createTime` 较新优先、时间相同按 `id` 较大优先选择映射，避免审批快照写入旧钉钉 `userId` 后无法正确展示或参与统计。
  - 后端按 `beginTime/endTime` 重新计算时长并保存为 `durationUnit=小时`，不信任前端展示值；`workDate` 写入开始时间所在日期，`createTime` 使用服务器当前时间作为列表“同步时间”。
  - 回归验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，32 个测试；`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，110 个测试；前端配套 `node tests/attendance-approval-api.test.mjs`、`node tests/approval-duration-utils.test.mjs`、`node tests/attendance-approval-fetch-utils.test.mjs`、`node tests/attendance-approval-dialog.test.mjs` 通过，`npm run build` 通过并保留既有 `::v-deep` 过时警告和 chunk size warning。
- 潘红琼审批记录数量与空时长排查（2026-07-15）：
  - 本地 `hr_0003` 潘红琼为 `employee_id=1831601326890434570 / userId=02533200433128427058 / dept=行政人力资源部`。
  - 使用 `0003` 公司 `ddaccount` 对应钉钉应用调用审批接口确认：`processinstance/listids` 按 `2026-06` 发起时间和请假流程返回 `9` 个实例；其中 `4` 条业务请假日期在 2026-06，另外 `5` 条为 2026-06 发起但业务请假日期在 2026-05。
  - 本地 `tbattendanceapprove` 当前只有这 `4` 条 6 月业务日期请假快照，且 `duration/durationUnit` 均为空；这不是钉钉未返回时长，`processinstance/get` 详情里的复杂请假组件均包含 `durationInHour` 与 `durationInDay`。
  - 现有统计扣减按业务日期归属月份，因此 6 月统计只应使用业务日期在 6 月的 4 条；若审批数据页面需要与钉钉后台“发起时间”结果条数一致，需要后续新增钉钉流程发起时间字段/筛选口径。
  - `HrmAttendanceApprovalProcessInstanceParser#resolveComplexLeaveDuration(...)` 已调整为优先保存钉钉 `durationInHour` 为“小时”，只有没有小时值时才保存 `durationInDay`。
  - 当前 `hr_0003` 2026-06 参与统计的请假快照共有 `333` 条，其中 `288` 条空时长，影响 `51` 名员工；潘红琼不是个例。
  - 验证：`mvn -Dtest=HrmAttendanceApprovalProcessInstanceParserTest test` 通过，8 个测试 0 失败；`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，94 个测试 0 失败。
- 生产体系实际出勤加班来源修正（2026-07-15）：
  - `HrmOvertimeNightStatisticsServiceImpl` 将生产体系 `affiliation_system=2` 的实际出勤加班来源从月度自动/日级加班汇总改为本地已通过加班审批，和行政体系保持一致。
  - 月度 `overtimeHours` 仍作为加班汇总展示；生产体系实际出勤和应计出勤公式中的加班项均只使用本地审批加班，避免李凤皇这类员工被 `93.48h` 自动加班抬高实际出勤。
  - 已重新执行 `2026-06` 开始统计并与 Excel 对账：27 人中 16 人三项一致，剩余 11 人仅实出勤不一致；应出勤与应计出勤全部一致。
  - 代码口径修正后李凤皇已对齐；剩余差异来自本地审批快照与 Excel 请假/加班列不一致，包括审批时长缺失后按自然时间折算、Excel 有请假但系统无对应审批快照、系统有本地加班审批但 Excel 加班列为 0。
  - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，50 个测试 0 失败；`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，94 个测试 0 失败。
- 2026-06 行政体系 Excel 与当前统计再次复查（2026-07-15）：
  - 使用 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx` 主表 `6月` 重新对当前系统 `queryPageList(month=2026-06)` 做逐人核对；当前统计明细最大更新时间为 `2026-07-15 04:57:36`。
  - 本次仍按员工 ID 固定区分两名王芳：行政人力资源部王芳 `1831601326890434572`，财务部王芳 `1831601326890434577`。
  - 当前 27 人中 15 人三项一致；12 人仍存在差异。差异全部发生在 `actualAttendanceHours`，`expectedAttendanceDays * 8` 与 `accruedAttendanceHours` 已全部和 Excel 对齐。
  - 已对齐人员包括：李明明、张明、赵聪、闫倩、吴晓霞、王芳（行政人力资源部）、程静、王芳（财务）、喻艳、李红盼、黎冬霜、严锦、庞龙斌、李学飞、林艳。
  - 剩余差异中，王琪和高文利来自本地审批时长/分类与 Excel 扣减口径不一致；生产体系员工主要来自当前系统实际出勤仍计入月度加班汇总，而 Excel 实际出勤未体现同一加班口径，且部分 Excel 请假扣减在系统审批快照中缺失或分类/时长不同。
- 王芳（行政人力资源部）审批重抓重复复核（2026-07-14）：
  - 用户确认本轮重抓对象是行政人力资源部王芳；固定本地员工身份为 `employee_id=1831601326890434572 / TYNG-437 / mobile=15871989405 / dept=行政人力资源部`。
  - `/Users/jiangyongming/Desktop/2.png` 确认审批编号 `202606291142000335491` 的所在部门为 `HB行政人力资源部`，假别为 `年假`，业务日期为 `2026-06-28`。
  - 钉钉通讯录权威校验显示：`1957266823950408` 是行政人力资源部王芳 `15871989405`；`1068600027950408` 是生产部王芳 `13032750052 / 杀菌机/卧离机`；财务部王芳仍为 `0305684530950408`。
  - 本地 `hr_0003` 原数据对调：行政人力资源部王芳错误保存 `dingtalk_user_id=1068600027950408`，且 `tbattendanceuser.id=225` 也错误挂到该 userId；生产部王芳错误保存 `dingtalk_user_id=1957266823950408` 且缺少正确 `tbattendanceuser`。
  - 钉钉只读复核：`1957266823950408` 在 2026-06 发起时间窗口有 6 条请假申请，详情部门均为 `HB行政人力资源部`；其中业务日期在 2026-06 的只有 `2026-06-26 年假1天` 和 `2026-06-28 年假1天`，合计 `16小时`，与 Excel `年假16 / 实出勤184` 一致。
  - 此前得到的 13 条 `调休200小时` 来自 `1068600027950408`，钉钉详情部门均为 `HB生产部`，应归属于生产部王芳，不是行政人力资源部王芳重复审批。
  - 代码修复：`HrmAttendanceApprovalSyncServiceImpl#resolveAttendanceUserForEmployee(...)` 不再盲信员工表现有 `dingtalk_user_id`；已有值也会先用钉钉通讯录姓名/手机号校验，不匹配时按姓名+手机号唯一匹配刷新，并写回 `hrm_employee.dingtalk_user_id` 与 `tbattendanceuser`。
  - 数据修复已执行：行政人力资源部王芳改为 `1957266823950408`，生产部王芳改为 `1068600027950408`，财务部王芳保持 `0305684530950408`；`tbattendanceuser.id=225` 已改回行政人力资源部王芳，生产部王芳已补 `tbattendanceuser.id=226`。
  - 重抓结果：重新获取行政人力资源部王芳 `2026-06` 审批插入/保留 2 条 6 月业务日期年假，合计 `16小时`；按流程主键、业务时间组合、同日同类型复查均无重复。生产部王芳 `1068600027950408` 的 13 条调休仍归生产部员工，不属于行政人力资源部王芳。
  - 单人统计验证：行政人力资源部王芳 `2026-06` 重算后落库 `expected_attendance_days=25`、`actual_attendance_days=23`、`actualAttendanceHours=184.00`、`accrued_attendance_hours=200.00`，备注 `扣除：年假16.00小时`，与 Excel `200 / 184 / 200` 对齐。
- 生产体系月休配置应出勤与王芳映射复核（2026-07-15，2026-07-16 更新为可配置）：
  - `HrmOvertimeNightStatisticsServiceImpl` 新增生产体系分支：`affiliation_system=2` 时，应出勤天数按 `month.lengthOfMonth() - 生产体系月度休息天数配置 - 生产体系需额外扣减的工作日法定休息日` 计算，并以 `0` 为下限；行政体系 `affiliation_system=1` 仍走单双休月历 `workDays`。
  - 生产月休天数默认 `4`，现由基本工资金额设置维护；配置月休已覆盖周末/休息类日期。当 `WorkweekMonthCalendarVO.days` 有日明细时，仅统计 `sourceType=legal_rest` 且非周六、周日的日期。若日明细缺失，才回退使用 `legalHolidayRestDays` 汇总值。
  - 2026-06 场景中，端午 3 天 `legal_rest` 只有 `2026-06-19` 是工作日，`2026-06-20`、`2026-06-21` 是周末，不再额外扣减；生产体系应出勤为 `30 - 4 - 1 = 25天`，即 `200小时`。
  - 当前 `hr_0003` 复核有 3 个王芳：行政人力资源部 `employee_id=1831601326890434572 / TYNG-437 / mobile=15871989405` 已映射 `1957266823950408`；财务部 `employee_id=1831601326890434577 / TYNG-393` 已映射 `0305684530950408`；生产部 `employee_id=1831601326890434614 / TYNG-107 / mobile=13032750052` 已映射 `1068600027950408`。
  - 其他员工映射主要来自两条链路：考勤同步步骤 2 `AttendanceUserManager#GetAndSave()` 按钉钉花名册、手机号/姓名和考勤组增量写入 `tbattendanceuser`；审批抓取链路先校验 `hrm_employee.dingtalk_user_id` 与钉钉通讯录姓名/手机号，缺失、失效或不匹配时按姓名 + 手机号唯一匹配钉钉花名册并写回员工表、同步保存 `tbattendanceuser`。
  - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，48 个测试 0 失败；`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，92 个测试 0 失败。
- 2026-06 行政体系 Excel 与加班夜班统计对账（2026-07-14）：
  - 对账文件为 `/Users/jiangyongming/Desktop/农谷导入数据/副本2026年行政系统考勤汇总表（6月）.xlsx`，系统侧使用本机后端 `queryPageList` 查询 `companyId=0003`、`2026-06`。
  - 27 名 Excel 员工中，李明明、张明、赵聪、闫倩、吴晓霞、程静、喻艳、李红盼三项一致；19 人存在差异。
  - 差异分两类：
    - 李凤皇、潘红琼、王芳（行政人力资源部）、杨太琴及多名仓储人员在员工表中为 `affiliation_system=2`。旧统计按生产体系 `0` 应出勤处理；本轮已补齐生产体系固定月休 4 天并扣当月假期的应出勤口径。
    - 王琪、高文利、王芳（财务）、黎冬霜、严锦、庞龙斌、李学飞、林艳的应出勤一致但实际出勤不同，主要由本地 `tbattendanceapprove.duration/durationUnit` 为空后按自然起止时间兜底，以及林艳同日同类重复审批参与统计导致。
  - 本次 Excel 只有“王芳”和“王芳（财务）”两行，分别匹配行政人力资源部 `employee_id=1831601326890434572 / TYNG-437 / mobile=15871989405` 与财务部 `employee_id=1831601326890434577 / TYNG-393 / mobile=13872932293`；复核 `hr_0003.hrm_employee` 当前员工主数据时还查到生产部同名王芳 `employee_id=1831601326890434614 / TYNG-107 / mobile=13032750052`，但该员工不在本次 Excel 主表。后续对账和统计必须按员工 ID、工号、手机号、部门、钉钉 `userId` 区分，禁止仅按姓名匹配。
  - 详细报告见 `docs/reports/2026-07-14-admin-attendance-excel-reconciliation.md`。
- 闫倩 2026-06 实际出勤 181 小时复核（2026-07-14）：
  - 本机接口 `queryPageList` 查询闫倩 `2026-06` 当前返回：`expectedAttendanceDays=23`、`actualAttendanceHours=181.00`、`actualAttendanceDays=22`、`actualAttendanceRemark=扣除：调休0.50小时、年假8.00小时`、`overtimeHours=44.92`。
  - 审批明细确认当前参与统计的请假为两条：`2026-06-13 年假 1天` 和 `2026-06-26 调休 0.07天`；后者按起止时间 `17:30 -> 18:00` 纠偏为 `0.50` 小时。
  - 审批明细确认当前行政体系实际出勤可计入的本地审批加班为两条：`2026-06-03 2小时`、`2026-06-17 3.5小时`，合计 `5.50` 小时；页面展示的月度 `overtimeHours=44.92` 含日级自动加班，不全部计入行政体系实际出勤。
  - `2026-06-13` 在单双休月度日历中为 `dayType=1 / 上班`，因此该日年假按当前业务口径应扣 `8.00` 小时。
  - 当前实际出勤复算为：`23 * 8 + 5.50 - 8.50 = 181.00`，不是年假未扣；若未扣年假则会显示 `189.00` 小时。
- 闫倩 2026-06 半小时请假扣减修正（2026-07-14）：
  - 现场本机查询确认闫倩 `2026-06-26 17:30:00 -> 18:00:00` 的本地请假快照为 `duration=0.07`、`durationUnit=天`，旧统计按 `0.07 * 8 = 0.56` 小时扣减，导致半小时请假显示为 `0.56` 小时。
  - `HrmAttendanceApprovalProcessInstanceParser#resolveComplexLeaveDuration(...)` 调整为：钉钉复杂请假组件同时返回 `durationInDay` 和 `durationInHour` 时，若按 `1天=8小时` 换算后两者不一致，则优先保存小时值，后续重抓该审批会落为 `0.5小时`。
  - `HrmOvertimeNightStatisticsServiceImpl#resolveAttendanceApprovalHours(...)` 对历史已落库的小数天数快照增加纠偏：当 `durationUnit=天` 且是小数天数，并且 `beginTime/endTime` 同步折算出的小时数与 `duration * 8` 只存在小范围舍入差异时，按起止时间差扣减；因此 `17:30-18:00` 扣 `0.50` 小时。
  - 回归测试新增 `parse_shouldPreferHourDurationForFractionalComplexLeave` 和 `startStatistics_shouldUseTimeRangeForRoundedFractionalDayLeaveDeduction`。
- 审批数据通用审批误判请假修复（2026-07-14）：
  - 现场核验闫倩 `2026-06-04` 本地 `请假/年假` 快照对应的钉钉流程实例 `ichrEbNFT1KknK-6tk09CA04041780560125`，钉钉接口返回标题为“闫倩提交的通用审批”，表单内容为“关于王志兰5月工时预留的申请”，并非请假审批。
  - 根因：`HrmAttendanceApprovalProcessInstanceParser#resolveLeaveSubType(...)` 过去会扫描全部表单正文，只要正文包含“调休/年假”等关键字就可能把通用审批归类为 `请假`。
  - 修复：请假子类型只从明确的请假字段、复杂请假组件值中的 `请假类型/假期类型/假别` 标记，或 `extValue.extension.tag` 等结构化假别标签中解析；不再从“审批详情”正文全文兜底识别请假。
  - 已新增回归测试 `parse_shouldIgnoreGenericApprovalThatOnlyMentionsCompensatoryLeaveInDescription`，防止通用审批正文提到“调休”后再次入库为请假。
  - 既有历史脏快照不会被代码自动改库；发布后对目标员工目标月份重新“获取审批数据”时，重抓清理会删除本轮未返回或不再解析为目标类型的陈旧本地快照。
- 审批数据时长手工修正（2026-07-14）：
  - `HrmAttendanceApprovalController` 新增 `POST /hrmAttendanceApproval/updateDuration`，请求体为 `UpdateAttendanceApprovalDurationBO(approvalId, duration, durationUnit)`。
  - `HrmAttendanceApprovalServiceImpl#updateDuration(...)` 只更新本地审批快照 `tbattendanceapprove.duration/durationUnit`，不调用钉钉接口；时长需为大于 `0` 的数值，单位限定为 `小时`、`分钟`、`天`（`日` 入参会归一为 `天`）。
  - 加班/夜班统计和实际出勤扣减仍读取 `tbattendanceapprove.duration + durationUnit` 统一折算，因此手工修正后的时长会参与后续统计重算和查询兜底。
  - 回归验证：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmOvertimeNightStatisticsServiceImplTest test`，98 个测试通过。
- 加班/夜班统计应计出勤计算项（2026-07-14）：
  - “开始统计”新增月度应计出勤小时，公式为 `实际出勤时间 + 调休 + 年假 - 加班 + 病假`；实际出勤时间复用 `actualAttendanceHours`，加班使用与实际出勤一致的可计入口径（行政体系只取本地审批加班，非行政体系取月度 `overtimeHours` 汇总），调休/年假/病假使用参与统计的本地审批小时。
  - `HrmOvertimeNightStatisticsDetail` 新增 `accruedAttendanceHours`，映射数据库列 `accrued_attendance_hours`；`QueryOvertimeNightStatisticsPageVO`、`EmployeeOvertimeNightMonthlyDetailVO`、`QueryOvertimeNightDailyDetailPageVO` 同步透出 `accruedAttendanceHours`。
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails(...)` 在月度明细生成后按可计入实际出勤的加班小时回填应计出勤小时到当月所有明细行；查询旧明细时按当前公式兜底计算，避免张明这类行政体系日级自动加班被错误扣减。
  - 前端 `hr_web` 于同日为“实际出勤小时(天数)”和“应计出勤小时(天数)”单元格增加鼠标悬浮计算过程：实际出勤展示 `应出勤 + 可计入加班 - 请假扣减`，应计出勤展示 `实际出勤 + 调休/年假/病假补回 - 加班`；本轮未新增后端接口字段。
  - 新增迁移脚本 `docs/sql/2026-07-14_overtime_night_accrued_attendance_hours.sql`，为 `hr_0001` 至 `hr_0005` 幂等增加 `accrued_attendance_hours DECIMAL(10,2)`。
  - 回归验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`，44 个测试通过。
- 李明明 2026-06 实际出勤与应计出勤校准（2026-07-14）：
  - 根因：钉钉复杂请假组件中的年假时长在 `extValue.durationInDay/durationInHour`，旧解析未读取该字段，导致本地 `tbattendanceapprove.duration` 为空；统计查询又用审批起止自然跨度兜底，造成应计出勤显示 `207.40` 小时。
  - `HrmAttendanceApprovalProcessInstanceParser` 已读取复杂组件 `extValue` 的 `durationInDay/durationInHour`，并保留日期字段解析，李明明年假审批应保存为 `duration=4`、`durationUnit=天`。
  - 当前按 `应出勤小时 + 可计入实际出勤的加班小时 - 事假/病假/调休/年假扣减小时` 计算实际出勤；行政体系的可计入实际出勤加班只包含本地审批加班。
  - 李明明 `hr_0003`、`2026-06` 的校准值为：应出勤 `184` 小时、实际出勤 `160` 小时、应计出勤 `184` 小时。
- 张明 2026-06 行政体系日级自动加班排除（2026-07-14）：
  - 张明 `2026-06` 汇总加班 `47.72` 小时来自日级自动加班，不是本地审批加班；该值继续在加班汇总展示，但不计入行政体系实际出勤。
  - `HrmOvertimeNightStatisticsServiceImpl` 将展示用 `monthlyOvertimeHours` 与实际出勤用 `actualAttendanceOvertimeHours` 拆开：
    - 行政体系：`actualAttendanceOvertimeHours = 本地审批加班小时`；
    - 非行政体系：仍使用月度加班汇总。
  - 查询路径通过 `buildAttendanceApprovalMap(...)` 重新汇总本地审批加班，避免旧明细中只有日级自动加班时把实际出勤抬高；张明正确实际出勤为 `184` 小时。
  - 应计出勤同样使用该加班分流口径；张明正确应计出勤为 `184` 小时，不再扣 `47.72` 小时日级自动加班。
  - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test` 通过，46 个测试 0 失败；本地 API 查询张明 `2026-06` 返回 `actualAttendanceHours=184.00`、`accruedAttendanceHours=184.00`、`overtimeHours=47.72`，查询李明明返回 `actualAttendanceHours=160.00`、`accruedAttendanceHours=184.00`。
- 加班/夜班统计实际出勤年假扣减与列顺序调整（2026-07-14）：
  - 业务口径更新为：有正数应出勤天数时，实际出勤按“应出勤 + 可计入实际出勤的加班 - 事假/病假/调休/年假扣减”折算小时，再向下取整得到 `actualAttendanceDays`。
  - `HrmOvertimeNightStatisticsServiceImpl#resolveActualAttendanceDeductibleType(...)` 新增 `年假` 识别，和既有 `事假`、`病假`、`调休` 一样使用 `tbattendanceapprove.tagName/subType` 关键字匹配。
  - 前端实际出勤矩阵列顺序同步调整为“姓名、应出勤时间、实际出勤小时(天数)、每日列、备注”。
  - 回归测试新增/更新 `startStatistics_shouldDeductPersonalSickCompensatoryAndAnnualLeaveFromExpectedAttendanceDays`；验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`，44 个测试通过。
- 审批数据按员工可见模板解析流程编码（2026-07-14）：
  - 根因：`HrmAttendanceApprovalSyncServiceImpl#resolveProcessTemplates(...)` 之前会优先调用管理员可管理模板接口；`companyId=0003` 的管理员 `adminId=296842114330963873` 不可用时，任意右侧框员工都会先出现管理员模板失败日志，容易被误判为当前员工查询失败。
  - 修复：审批抓取不再优先调用 `process.template.manage.get` 管理员模板接口，流程编码解析直接使用当前目标员工 `userId` 调 `process/listbyuserid`；右侧框选择谁，模板解析和实例列表都围绕该员工执行。
  - 边界：管理员 `adminId` 退出审批抓取主链路，不再影响“获取审批数据”；只有员工可见模板或实例列表接口对当前员工 `userId` 返回 `400023/用户不存在`，才按员工映射失效处理并跳过或报错。
  - 李明明 `hr_0003` 复核：在职员工 `employee_id=1831601326890434563`、手机号 `17389819211`、`hrm_employee.dingtalk_user_id=024662561026250638`，`tbattendanceuser` 也只有该有效映射；另一个同名 `employee_id=2033769831940079620` 为 `is_del=1` 且无钉钉 ID。
  - 回归测试更新为 `resolveProcessCodes_shouldUseEmployeeVisibleTemplatesWithoutManageableTemplatePrequery`、`resolveProcessCodes_shouldNotFailWhenManageableTemplateUserWouldFail`、`resolveProcessCodes_shouldIgnoreManageableTemplateQueryFailures`；验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test`。
- 加班/夜班统计实际出勤加入加班小时（2026-07-13，2026-07-14 更新年假扣减、李明明/张明校准）：
  - 业务口径调整为：员工存在正数应出勤天数时，实际出勤小时按 `expectedAttendanceDays * 8 + actualAttendanceOvertimeHours - deductionHours` 计算，最低为 `0`；行政体系员工的 `actualAttendanceOvertimeHours` 只取本地审批加班，非行政体系仍取月度加班汇总。实际出勤天数仍按小时除以 `8` 后向下取整。
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails(...)` 先生成日级加班/夜班明细，再汇总展示用当月 `overtimeHours` 和实际出勤用审批加班小时，最后把“应出勤 + 可计入实际出勤的加班 - 事假/病假/调休/年假扣减”得到的月度实际出勤天数回填到当月所有明细行。
  - `queryPageList`、`queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 的查询兜底也同步区分展示加班和实际出勤加班；实际出勤查询缓存键新增加班小时，避免同员工、同月份、同应出勤天数但加班时长不同的结果互相复用。
  - 无正数应出勤天数时仍保留旧的打卡有效分钟兜底，不改变生产体系或无应出勤场景。
  - 回归测试新增 `startStatistics_shouldAddOvertimeHoursBeforeDeductingLeaveFromExpectedAttendanceDays`；验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`，39 个测试通过。
- 【已废弃】加班审批存在时实际出勤不叠加加班汇总（2026-07-14 早期规则）：
  - 该规则已被更细的行政/非行政口径替代；当前开始统计和查询路径将展示用月度 `overtimeHours` 与实际出勤用 `actualAttendanceOvertimeHours` 分开。
  - 行政体系只把本地审批加班传入 `calculateActualAttendanceFromExpected(...)`，避免张明这类日级自动加班把实际出勤抬高；李明明这类审批加班仍会计入。
  - 当前回归测试改为覆盖：
    - `startStatistics_shouldAddOvertimeApprovalToActualAttendance`；
    - `startStatistics_shouldAddDailyOvertimeToActualAttendanceForAdministrativeEmployee`；
    - `queryPageList_shouldAddDailyOvertimeToActualAttendanceForAdministrativeEmployee`；
    - `queryPageList_shouldAddOvertimeApprovalToActualAttendance`。
- 加班/夜班统计重算锁等待治理（2026-07-14）：
  - 现场报错：`Lock wait timeout exceeded; try restarting transaction`。
  - 根因判断：`startStatistics(...)` / `startStatisticsForEmployee(...)` 原先在方法级长事务中完成整月计算、删除旧明细、保存新明细和回查，删除 `hrm_overtime_night_statistics_detail` 后写锁会持续到整个方法返回；同时旧 Spring Data 派生 delete 容易退化为逐实体删除，锁窗口更长。
  - 修复：
    - 统计入口取消方法级 `@Transactional`；
    - 整月或单人明细先在事务外完成排班、打卡、审批数据计算；
    - `replaceMonthlyDetails(...)` / `replaceEmployeeMonthlyDetails(...)` 通过 `TransactionOperations` 只包住“bulk delete + flush + saveAll + flush”；
    - `hrmOvertimeNightStatisticsDetailRepository` 的删除方法改为 `@Modifying @Query` JPQL bulk delete，返回类型为 Spring Data 支持的 `int`；
    - 新增 `docs/sql/2026-07-14_overtime_night_statistics_detail_indexes.sql`，为租户库统计明细表补充 `work_date` 和 `employee_id, work_date` 删除条件索引（已有同等左前缀索引时跳过）。
  - 回归测试新增/更新：
    - `startStatistics_shouldCalculateRowsBeforeDeletingExistingRowsToReduceLockTime`；
    - `overtimeNightDetailRepositoryDeleteMethods_shouldUseBulkModifyingQueries`；
    - `overtimeNightStatisticsEntryPoints_shouldNotUseLongRunningTransactions`。
  - 验证：
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`，44 个测试通过；
    - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test`，94 个测试通过。
- 审批数据重抓陈旧快照清理 `@Modifying` 返回类型热修（2026-07-13）：
  - 现场报错：`Modifying queries can only use void or int/Integer as return type!`。
  - 根因：`tbattendanceapproveRepository#deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(...)` 使用了 `@Modifying @Query`，但返回类型定义为 `long`；当前 Spring Data JPA 版本只接受 `void`、`int` 或 `Integer`。
  - 修复：该方法返回类型改为 `int`，调用方仍以 `long deletedCount` 接收用于日志展示，不改变清理逻辑和业务范围。
  - 回归测试新增 `approvalRepositoryModifyingDeleteWithRetainedIds_shouldUseSpringSupportedReturnType`，锁定该仓库契约，避免以后再次改回 `long`。
  - 验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest test`，38 个测试通过。
- 审批数据子类型手工修正与重抓保留（2026-07-13）：
  - `HrmAttendanceApprovalController` 新增：
    - `POST /hrmAttendanceApproval/querySubtypeOptions`：返回系统默认请假子类型与本地 `tbattendanceapprove.subType` 去重后的下拉选项；
    - `POST /hrmAttendanceApproval/updateSubtype`：仅更新本地审批快照 `subType`，不调用任何钉钉接口。
  - `HrmAttendanceApprovalServiceImpl#updateSubtype(...)` 校验审批实例 ID 与子类型后保存本地 `tbattendanceapprove.subType`；当前用于处理闫倩 `2026-06-04` 这类同步后子类型不符合业务判断的特殊数据。
  - `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 调整指定员工重抓策略：
    - 不再在抓取前全量删除目标员工当月该类型审批；
    - 先按钉钉本次返回的 `procInstId` 保存或更新快照；
    - 若本地已存在同一 `procInstId` 且 `subType` 与钉钉解析值不一致，则保留本地 `subType`，避免覆盖用户手工修正；
    - 抓取完成后再按 `beginTime + userId + tagName + id not in 本次返回ID` 清理陈旧快照；
    - 清理范围仍基于 `employeeId -> tbattendanceuser.userId`，不会按同名员工扩散。
  - `tbattendanceapproveRepository` 新增 `findDistinctSubTypes()` 与 `deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(...)`。
  - 回归测试：
    - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest test`；
    - 前端配套：`node tests/attendance-approval-api.test.mjs`、`node tests/attendance-approval-dialog.test.mjs`。
- 加班/夜班统计实际出勤扣减口径与备注列（2026-07-12，2026-07-13 追加加班小时，2026-07-14 行政体系审批加班分流）：
  - 后端实际出勤查询和重算口径已从“打卡有效分钟 + 调休补足”调整为“应出勤时间 + 可计入实际出勤的加班小时 - 可扣减审批时间”：
    - 员工当月应出勤天数为正数时，实际出勤小时按 `expectedAttendanceDays * 8 + actualAttendanceOvertimeHours - deductionHours` 计算；
    - 行政体系员工的 `actualAttendanceOvertimeHours` 只取本地钉钉审批加班，不取日级自动加班；非行政体系仍取月度加班汇总；
    - 可扣减审批只识别 `tbattendanceapprove.tagName/subType` 包含 `事假`、`病假`、`调休`、`年假` 的记录；
    - `外出` 等其他审批类型不扣减，也不进入备注；
    - 无正数应出勤天数时继续保留旧打卡有效分钟兜底，避免生产体系员工被错误归零。
  - `QueryOvertimeNightStatisticsPageVO`、`EmployeeOvertimeNightMonthlyDetailVO`、`QueryOvertimeNightDailyDetailPageVO` 新增 `actualAttendanceHours` 与 `actualAttendanceRemark`，查询响应可展示半天扣减结果和扣减原因。
  - `HrmOvertimeNightStatisticsServiceImpl` 新增 `resolveActualAttendanceDeduction(...)` 与 `ActualAttendanceResolution`，查询缓存键包含员工、月份、应出勤天数和月度加班小时，避免不同应出勤/加班上下文复用错误结果。
  - 张明 `2026-06` 场景中仅有 `外出` 审批且无本地加班审批；月度加班汇总 `47.72` 小时继续展示为加班汇总，但不计入行政体系实际出勤，因此应出勤 `184` 小时会返回实际出勤 `184` 小时、`23` 天。
  - 回归测试：
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`；
    - 前端配套：`node tests/overtime-night-utils.test.mjs`、`node tests/attendance-display-columns.test.mjs`、`npm run build`。
- 加班/夜班统计张明 2026-06 实际出勤 160 小时来源排查（2026-07-12）：
  - 本地库 `hr_0003` 中张明 `employee_id=1831601326890434564`，`2026-06` 统计明细落库为 `expected_attendance_days=23`、`actual_attendance_days=20`，前端分别显示为 `184` 小时和 `160` 小时。
  - 旧实际出勤算法按 `hrm_attendance_clock.work_date` 分组，取同一业务日最早到最晚打卡跨度，单日最多计 `480` 分钟；月度有效分钟合计后加“调休”审批分钟，再除以 `480` 取整数天。
  - 张明 `2026-06` 打卡有效分钟复算为 `9829` 分钟，折算 `20` 天；同月审批快照只有“外出”审批，没有 `tagName/subType` 包含“调休”的审批，因此审批抵扣分钟为 `0`。
  - 因此旧页面 `160` 小时的来源是 `floor(9829 / 480) * 8 = 160`；该排查结论已促成本次新口径：有应出勤时改按应出勤扣 `事假/病假/调休`。
- 加班/夜班统计出勤时间展示与行政应出勤查询兜底（2026-07-12）：
  - 前端最新口径要求删除旧的“出勤时间统计”摘要区域；应出勤时间不再单独展示，实际出勤时间改为与加班小时矩阵一致的“`yyyy-MM` 实际出勤时间统计”区域。
  - 实际出勤矩阵表头为姓名、小时(天数)汇总、1日到月末；小时(天数)汇总列继续读取“开始统计”落库后接口返回的 `actualAttendanceDays`，前端按 `actualAttendanceDays * 8` 换算小时并显示为 `小时(天数)`，当前接口无日级实际出勤字段时，日列按 `0` 占位。
  - 后端查询链路补充旧统计数据兜底：
    - `queryPageList` 在汇总行的 `expectedAttendanceDays` 为空或 `0` 且员工 `affiliation_system=1` 时，按 `HrmWorkweekSettingService#queryMonthCalendar(...)` 的 `workDays` 返回应出勤天数；
    - `queryEmployeeMonthlyDetail` 会读取员工表体系字段，并在旧明细缺少应出勤天数时按行政单双休月历补齐响应；
    - `queryDailyDetailPageList` 会按明细员工 ID 批量读取员工体系字段，并对行政员工逐行补齐应出勤天数响应；
    - `queryEmployeeMonthlyDetail`、`queryDailyDetailPageList` 和 `queryPageList` 会按员工+月份+应出勤天数缓存实际出勤计算结果；有正数应出勤时按新扣减口径重算，无正数应出勤时保留打卡有效分钟兜底；
    - 该兜底不重新计算加班小时或夜班次数，只修复旧统计明细缺少或口径过期导致页面显示旧值的问题。
  - 回归测试：
    - `node tests/attendance-display-columns.test.mjs`（前端，表格列结构与独立出勤区域）；
    - `node tests/overtime-night-utils.test.mjs`（前端矩阵工具函数）；
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`（后端，32 个测试通过）。
- 加班/夜班统计补充应出勤与实际出勤天数（2026-07-12）：
  - `HrmEmployee` 补齐现场库已有字段 `affiliation_system`，用于区分员工所属体系：`1` 行政、`2` 生产。
  - `HrmOvertimeNightStatisticsDetail` 新增 `expectedAttendanceDays`、`actualAttendanceDays`，对应数据库列 `expected_attendance_days`、`actual_attendance_days`。
  - `HrmOvertimeNightStatisticsServiceImpl#startStatistics` / `startStatisticsForEmployee` 在重算加班/夜班明细时同步计算月度应出勤天数和实际出勤天数，并写入本月该员工的统计明细行；无原始候选工作日时，零值占位行也会带同一组月度出勤天数。
  - 应出勤口径：
    - 仅 `hrm_employee.affiliation_system=1` 的行政体系员工读取 `HrmWorkweekSettingService#queryMonthCalendar(...)` 的 `workDays`；
    - 生产体系员工不套用行政单双休设置，应出勤天数按 `0` 写入。
  - 旧实际出勤口径曾基于 `hrm_attendance_clock` 的有效打卡分钟和 `tbattendanceapprove` 中 `调休` 审批时长；该口径已被 2026-07-12 的“应出勤 - 事假/病假/调休扣减”规则替代，仅在无正数应出勤天数时作为兜底保留。
  - 汇总页、员工月度明细和导出汇总从明细行读取月度出勤天数字段，不按日重复累加。
  - `QueryOvertimeNightDailyDetailPageVO` 和 `toDailyPageVO(...)` 同步透出 `expectedAttendanceDays`、`actualAttendanceDays`，保证“显示所有/查看所有”每日明细总览可直接展示月度出勤天数。
  - 2026-07-12 公网运行态复核：
    - `http://153.0.237.99:8081/` 仍加载旧前端入口 `assets/index-BapWR6G4.js`，本地最新 release 入口为 `assets/index-9YpfAlYm.js`；
    - 公网后端 `queryDailyDetailPageList`、`queryPageList`、`queryEmployeeMonthlyDetail` 返回行仍缺少 `expectedAttendanceDays/actualAttendanceDays`；
    - 因此现场“单人查看/查看所有无效果”需要同时发布后端新包、替换前端 nginx 静态目录，并对目标月份重新执行“开始统计”或“单人统计”后才会有真实非零出勤天数。
  - 新增迁移脚本：`docs/sql/2026-07-12_overtime_night_attendance_days.sql`，为 `hr_0001` 到 `hr_0005` 的 `hrm_overtime_night_statistics_detail` 幂等增加两列。
  - 回归测试：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（2026-07-12，通过，30 个测试，0 失败）。
- 加班/夜班统计固定导出（2026-06-03）：
  - `HrmOvertimeNightStatisticsController` 新增 `GET /hrmOvertimeNightStatistics/downloadStatisticsTemplate`；
  - 当前接口固定读取 classpath `export/jbtj.xlsx`，并以附件名 `jbtj.xlsx` 输出；响应类型使用 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，通过流复制原文件字节；
  - 原 `/exportStatistics` 动态导出接口保留，暂不作为统计页“导出统计数据”按钮的调用链；
  - 回归测试：`mvn -Dtest=HrmOvertimeNightStatisticsControllerTest test`。
- Maven package 模板资源修复（2026-06-17）：
  - `mvn package` 的 Surefire 失败来自两个 classpath 模板缺失：`export/jbtj.xlsx` 工作区缺失，以及 `export/副本人资系统导出员工基础信息模版.xlsx` 未放入项目资源目录；
  - 已恢复 `src/main/resources/export/jbtj.xlsx`，并从业务提供目录复制员工基础信息导出模板到 `src/main/resources/export/副本人资系统导出员工基础信息模版.xlsx`；
  - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsControllerTest,EmployeeBasicInfoExportSupportTest test` 通过；`mvn package` 通过，266 个测试 0 失败。
- 添加排班生产车间/工段可空（2026-05-31）：
  - 后端 `tbplanlist.ProductName/LinkName` 仅作为排班记录展示信息，不参与 `WorkPlanServiceImpl#validatePlanForSubmit` 的基础必填校验。
  - 排班保存核心必填仍为人员、排班日期，以及标准班次的班次/考勤组信息；自定义班次仍按开始/结束时间与白夜班别规则处理。
  - 前端 `hr_web` 已同步移除“生产车间/工段”必填拦截，空值会按空字符串/空值透传给 `/workPlan/saveAll`。
- 排班自定义班次补充白/夜班别（2026-05-30）：
  - `tbplanlist` 新增 `custom_shift_period`，`hrm_workplan_custom_shift` 新增 `shift_period`，取值 `day/night` 分别表示白班/夜班。
  - 添加排班 `saveAll` 解析 `customShiftPeriod`，本地自定义班次按 `start1/end1/cross_day/shift_period` 去重，避免相同时间的白班和夜班被错误合并。
  - 打卡概况 `saveEmployeeDayCustomShift` 新增 `customShiftPeriod` 参数，保存单员工单日自定义排班时同步保留白/夜选择。
  - 自定义班次下拉和员工单日排班查询会返回 `customShiftPeriod`，前端回显时可恢复原班别。
  - 新增迁移脚本：`docs/sql/2026-05-30_workplan_custom_shift_period.sql`。
  - 回归验证：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test`（2026-05-30，通过，30 通过，0 失败）。
- 添加排班 Excel 导入模板口径确认（2026-05-30，员工匹配口径已于 2026-07-25 调整）：
  - 业务确认 Excel 导入排班全部归类为本地自定义班次，不导入标准班次。
  - 早期模板曾精简为“排班日期、员工、手机号、排班时间、备注”；当前上传口径已升级为“电话”列，电话有内容时按“姓名 + 电话”匹配，电话为空时按姓名匹配，导入表不包含部门。
  - 排班时间按业务原文填写，支持 `8:00-17:30`、`20:00-8:00`、`8:00-结束`、`8:00`。
  - `8:00-结束` 与 `8:00` 表示“有上班时间、无固定下班时间”；后续实现需改造自定义班次解析和持久化，允许空结束时间，不用 `23:59` 等默认值兜底。
  - 导入预校验需阻止员工姓名与电话不匹配、同一员工同一天重复行；前端和后端均不再因系统同名候选要求用户必须补电话。
- 加班/夜班统计接入本地钉钉加班审批优先口径（2026-05-26）：
  - 背景：
    - 业务新增要求：当开始统计夜班/加班数据时，如果员工在目标月份存在本地钉钉加班审批数据，则按审批数据统计加班小时；
    - 同一员工多条审批需要累加；
    - 同名员工必须严格隔离，不能按姓名共用审批时长。
  - 实现位置：
    - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails(...)`
    - `tbattendanceapproveRepository#findAllByUserIdInAndWorkDateBetween(...)`
  - 实现方式：
    - 统计服务先通过 `employeeId -> tbattendanceuser.userId` 解析当前员工对应的钉钉 `userId`；
    - 再读取该 `userId` 在目标月份范围内的本地审批快照 `tbattendanceapprove`；
    - 只保留“加班”审批：
      - 优先识别 `bizType=1`
      - 兜底识别 `tagName/subType` 中包含“加班”的记录
    - 时长统一按 `duration + durationUnit` 解析：
      - `小时` 直接按小时累计
      - `分钟` 折算为小时
      - `天` 暂按 `8` 小时折算
      - 加班纯数字时长沿用审批解析器现有约定，默认按小时处理
    - 业务日期按 `workDate -> beginTime -> endTime` 顺序归组到日级明细；
    - 同一员工同一天的多条审批会先聚合求和，再覆盖该日 `overtimeHours`。
  - 口径边界：
    - 审批优先只覆盖加班小时，不改变夜班次数与计划/实际下班时间的打卡/排班判断链路；
    - 若当天没有命中本地加班审批，则继续保留原有自动计算结果；
    - 为防仓库宽查询或脏数据串入，同一员工统计时还会再次按允许的 `userId` 集合做本地过滤，不信任姓名。
  - 回归测试：
    - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldPreferAttendanceApprovalOverAutoCalculatedOvertimeWhenApprovalExists`
    - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldAccumulateMultipleAttendanceApprovalsForSameEmployeeAndDay`
    - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldIgnoreSameNameOtherEmployeesAttendanceApprovals`
    - 验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（2026-05-26，通过，24 通过，0 失败）

- 加班/夜班统计修复“2026-04 开始统计只落 3 人”（2026-05-26）：
  - 现场库 `hr_0003` 核验：
    - `hrm_employee.is_del=0` 有 `127` 人；
    - `hrm_overtime_night_statistics_detail` 在 `2026-04` 仅有 `3` 名员工、`34` 条日明细；
    - 同期 `hrm_attendance_plan / hrm_attendance_clock / tbattendancedetail / hrm_attendance_report_data(加班字段)` 也都只覆盖 `3` 名员工。
  - 根因：
    - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails` 以前只有在员工当月存在原始候选工作日时才会返回落库行；
    - `queryDailyDetailPageList`、月度明细和汇总列表都只读 `hrm_overtime_night_statistics_detail`，因此没有原始数据的员工会被整个月完全省略，页面表现就是“开始统计后只剩少数几个人”。
  - 修复：
    - 当员工在目标月份完全没有 `hrm_attendance_plan / hrm_attendance_clock / tbattendancedetail` 候选工作日时，仍写入一条零值占位统计明细：
      - `work_date` 取目标月份 `1` 号；
      - `overtime_hours=0.00`；
      - `night_shift_count=0`。
    - 这样全员重算后，汇总列表、每日总览和单人月度明细都能保留该员工，只是整月结果为 `0`。
  - 回归测试：
    - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldKeepEmployeesWithoutRawMonthlyDataInResultAsZeroSummary`；
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（2026-05-26，通过，21 通过，0 失败）。
- 钉钉付费 API 调用量异常排查（2026-05-13）：
  - 用户反馈“本月只执行了 `sync` 相关接口和审批数据抓取接口，但钉钉付费 API 已达 4900+ 次”。
  - 根因不是单一隐藏接口，而是同步链路的调用放大：
    - `HrmAttendanceDataServiceImpl` 当前 `BATCH_SIZE=1`，步骤 3/4/7 都按“单员工”进入服务层；
    - 步骤 3 `AttendancePlanServiceImpl#Sync(...)` 又会按日期循环，并在 `AttendancePlanRecord#GetAndSaveWithoutDelete(...)` 中对每天整天的钉钉排班结果重新全量扫描后再过滤员工；
    - 因此一次整月同步会把 `topapi/attendance/listschedule` 放大成“员工数 × 日期数”级别调用；以 `hr_0003` 现场 `141` 个考勤映射员工、`31` 天为例，仅这一步理论上就约 `141 × 31 = 4371` 次，且未计入分页 `hasMore` 的追加请求；
    - 步骤 4 `AttendanceDetailServiceImpl#Sync(...)` 按 `7` 天分段，仍以 `BATCH_SIZE=1` 调 `attendance/listRecord`，整月约为“员工数 × 日期段数”；按 `141` 人、`31` 天估算约 `141 × 5 = 705` 次；
    - 步骤 7a `LeaveRecordDataServiceImpl#Sync(...)` 实际调用的是 `HrmAttendanceReportManager#UpdateAttendanceReportQuick(...)`，会按 `45` 个 type=1 报表字段每 `20` 个一页分页请求 `topapi/attendance/getcolumnval`；按 `141` 人、整月分 `3` 个 15 天区间估算约 `141 × 3 × 3 = 1269` 次；
    - 步骤 7b `HolidayDataServiceImpl#Sync(...)` 调 `HrmAttendanceReportManager#UpdateHolidayReportQuick(...)`，每个员工每个 `15` 天区间至少一次 `topapi/attendance/getleavetimebynames`；按 `141` 人、`3` 个区间估算约 `141 × 3 = 423` 次；
    - 步骤 6 会先删除本次区间内的报表数据，导致重复执行 `sync` 时，步骤 7 的 `getcolumnval/getleavetimebynames` 会再次全量消耗。
  - 审批抓取链路也会放大：
    - `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 会对每个目标员工依次执行“流程模板解析 -> `processinstance/listids` -> `processinstance/get`”；
    - 若按整月、多人、多个审批类型抓取，调用量会随“员工数 × 流程编码数 × 实例数”继续增长。
  - 观测口径说明：
    - `postresultlog` 不是完整调用台账，只覆盖部分写了 `DDTalkResposeLogger` 的接口；
    - 报表抓取大量走 `ddtaskresult` 队列或根本未记入 `postresultlog`，因此不能仅靠该表反推钉钉后台付费计数。
  - 定时任务补充：
    - 当前仓库 `application-dev.properties` 明确配置 `scheduling.enabled=false`，本地 `dev` 配置下不会自动跑月初定时任务；
    - 若部署环境使用其他 profile，或通过外部配置覆盖为 `true`，则 `AbstractDingTalkTask` 会遍历 `tbCompanyList` 中全部公司执行月初同步任务，调用量会进一步叠加。
- 钉钉调用量最小化优化（2026-05-14）：
  - 已优先改造两条最高频链路：
    - `HrmAttendanceDataServiceImpl` 中步骤 3、4 不再复用“单员工批次”默认实现；
    - 审批抓取 `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 在单次任务内复用流程编码解析结果。
  - 步骤 3 现改为“日期级批量同步排班”：
    - `syncByEmployeeBatchParallelSafe(...)` 遇到 `IAttendancePlanService` 时，直接走 `syncPlanByDateForAllEmployees(...)`；
    - 一次任务仅把待同步员工集合拼成一个 `EmpIDS` 传给 `AttendancePlanServiceImpl#Sync(...)`；
    - `AttendancePlanServiceImpl#Sync(...)` 仍按日期循环，但每个日期只调用一次 `AttendancePlanRecord#GetAndSaveWithoutDelete(...)`；
    - 相比旧实现“员工数 × 日期数”次 `listschedule`，现在降为“日期数”次 `listschedule`。
  - 步骤 4 现改为“最多 50 人一批同步明细”：
    - 新增 `DETAIL_BATCH_SIZE = 50`；
    - `syncByEmployeeBatchParallelSafe(...)` 遇到 `IAttendanceDetailService` 时，改走 `syncDetailByBatch(...)`；
    - 每批最多 50 个员工拼成一次 `attendance/listRecord` 请求，贴合钉钉接口能力，不再按单员工反复调用。
  - 审批抓取现改为“任务级流程编码缓存”：
    - `fetchMonthData(...)` 中新增 `taskLevelProcessCodes`；
    - 当单次抓取命中多员工时，只在首个员工上执行一次 `resolveProcessCodes(...)`；
    - 后续员工直接复用同一组 `processCode`，避免重复调用模板解析接口。
  - `sync/syncAll` 入口现补充“员工 ID 规范化”：
    - `HrmAttendanceDataServiceImpl#SyncData(...)` / `SyncDataWithResume(...)` 在进入 7 步同步前，会先去掉空串并按原顺序去重 `EmpIDS`；
    - 避免手工传参或 `syncAll` 拼串中出现重复员工时，把整条同步链路再次重复执行一遍。
  - 步骤 7 现补充“复用已去重的用户映射”：
    - `LeaveRecordDataServiceImpl#Sync(...)` 与 `HolidayDataServiceImpl#Sync(...)` 不再无条件重新调用 `tbattendanceuserRepository.findAllByEmpIdIn(...)`；
    - 若上游已通过 `setUsers(...)` 注入去重后的用户映射，则直接按该映射筛选目标员工并调用 `getcolumnval/getleavetimebynames`；
    - 避免 `tbattendanceuser` 历史重复映射把同一钉钉 `userId` 的步骤 7 请求放大成多次。
  - 审批“全部员工抓取”现补充“本地映射去重”：
    - `HrmAttendanceApprovalSyncServiceImpl#resolveTargetUsers(...)` 在 `employeeIds` 为空时，不再直接透传 `attendanceUserRepository.findAll()` 结果；
    - 改为先按 `empId + userId` 有效性过滤，并按 `empId` 保留首条有效映射；
    - 避免全员抓取场景因为本地重复 `tbattendanceuser` 脏数据，对同一钉钉用户重复执行 `processinstance/listids/get`。
  - 当前刻意未改：
    - `getcolumnval` / `getleavetimebynames` 的 `15` 天分段尚未调整，因为本轮未拿到足够明确的上游时间范围上限证据；
    - `AttendanceGroupManager#shift/query` 的任务级缓存尚未落地，本轮优先完成收益最大的主链路重排。
- 审批数据页支持“手工获取审批数据”（2026-05-09）：
  - 保持原有列表查询链路不变：`/hrmAttendanceApproval/queryPageList` 仍只读取本地 `tbattendanceapprove`。
  - 新增后端接口：
    - `/hrmAttendanceApproval/checkMonthData`：历史保留的月份检查接口；
    - `/hrmAttendanceApproval/fetchMonthData`：按所选月份、员工范围与审批类型执行手工拉取。
  - 新增后端服务：
    - `IHrmAttendanceApprovalSyncService`
    - `HrmAttendanceApprovalSyncServiceImpl`
    - `HrmAttendanceApprovalProcessInstanceParser`
  - 同步实现策略：
    - 不新建表，继续复用 `tbattendanceapprove`；
    - 不复用 `attendanceData/sync` 主链路；
    - 也不直接复用旧 `AttendanceRecordManager#GetAndSave(Date Begin, Date End)`，因为该方法当前无视传入区间，只处理“今天/昨天”；
    - `2026-05-13` 起，手工获取已切换为按“所选月份 -> 所选员工或全部员工 -> 审批实例列表 -> 审批实例详情”拉取并落库：
      - 先优先调钉钉 `topapi/process.template.manage.get` 读取管理员可管理审批模板；若当前上下文无法拿到管理员 `userId`、返回空模板，或管理员模板接口返回 `errcode=400023 / 用户不存在`，再回退调 `topapi/process.listbyuserid` 读取用户可见审批模板；
      - 再按模板名称自动归类出所选审批类型对应的 `processCode`；
      - 再按每个 `processCode` 调钉钉 `topapi/processinstance/listids` 获取流程实例 ID；
      - 再调 `topapi/processinstance/get` 获取实例详情与表单字段；
      - 最后通过 `HrmAttendanceApprovalProcessInstanceParser` 解析为本地 `tbattendanceapprove` 结构并按 `procInstId` 去重写入。
    - 钉钉上游限制说明：
      - `topapi/processinstance/listids` / `topapi/processinstance/get` 依赖钉钉审批权限 `qyapi_aflow`；
      - 当前 SDK 版本下，`topapi/processinstance/listids` 的 `processCode` 是必填项，不能再按“用户 + 月份”直接不带编码调用；
      - `2026-05-13` 现场进一步确认：`topapi/process.template.list` 在当前应用上会误报缺少 `qyapi_dingpay_alipay`，但同一应用调用 `topapi/process.template.manage.get` / `topapi/process.listbyuserid` 时，钉钉返回的真实缺失权限是 `qyapi_aflow`；
      - 因此实现已不再依赖 `topapi/process.template.list`，避免把运维排查方向误导到“支付权限”；
      - 若模板列表中没有任何匹配到的 `processCode`，后端会直接返回“当前应用未匹配到所选审批类型对应的审批流程，请先确认钉钉审批模板名称和权限配置”；
      - 若应用未开通该权限，会直接报权限错误，无法读取管理员在钉钉审批中心看到的完整审批实例。
      - `2026-05-13` 起，审批实例接口失败不再统一吞成“获取审批数据失败，请稍后重试”：
        - 当原始错误包含 `qyapi_aflow / qyapi_dingpay_alipay / PermissionDenied / AccessDenied / 无权限` 时，后端会直接翻译为“当前钉钉应用未开通审批读取权限，请联系管理员开通后重试”；
        - 原始钉钉错误细节仅写入服务端日志，供管理员排查权限配置。
    - 审批实例解析口径：
      - 通过标题与表单字段名/字段值识别 `加班 / 补卡 / 请假 / 出差 / 外出`；
      - 请假子类型优先从 `请假类型/假别/假期类型` 等字段读取；
      - 开始/结束时间优先从表单字段读取，缺失时回退实例 `createTime/finishTime`；
      - 时长支持解析 `1天 / 4小时 / 30分钟` 这类中文文本并拆成 `duration + durationUnit`。
      - `2026-05-13` 修复钉钉日期区间组件兼容性：
        - 现场新增加班实例已入库，但 `beginTime/workDate` 被错误写成了流程 `createTime`，表现为 `2026-04-24 10:46:52 -> 2026-04-17 18:00:00` 这类“开始时间晚于结束时间”的脏数据；
        - 根因是钉钉审批表单中的开始/结束时间在部分模板下并不是单个纯文本时间，而是日期区间组件返回的数组/JSON 形态；旧解析器只对单个 `yyyy-MM-dd HH:mm` 文本做 `parseDate(...)`，解析失败后直接回退成了 `createTime`；
        - 现已调整 `HrmAttendanceApprovalProcessInstanceParser`：
          - 同时从 `FormComponentValueVo.value` 与 `extValue` 提取日期；
          - 支持从数组/JSON 字符串中抽取多个时间点；
          - 当“开始时间”组件本身同时包含起止两个时间时，首个时间记为 `beginTime`，最后一个时间记为 `endTime`；
          - 仅在所有组件都解析不到有效时间时，才回退到实例 `createTime/finishTime`。
        - `2026-05-13` 晚间继续修复真实加班模板字段别名与异常时间回填：
          - 现场再次核验 `processinstance/get` 明细后确认，范小艳两条加班实例并不使用 `开始时间` 字段名，而是 `加班日期` + `结束日期` + `预计加班时长`；
          - 因旧解析器未把 `加班日期` 识别为开始时间，且未把纯数字 `预计加班时长` 识别为“小时”，所以仍会把开始时间错误回退成流程 `createTime`；
          - 现已补充：
            - `加班日期 / 加班开始日期` 作为加班开始时间别名；
            - `预计加班时长` 作为时长字段别名；
            - 加班审批中纯数字时长默认按“小时”解析；
            - 当加班 `beginTime/endTime` 与时长明显冲突时，按 `endTime - duration` 反推 `beginTime`，用于兜底修复钉钉表单原始值异常或历史脏数据重抓。
          - 运行态已在 `9083` 实例实测通过：对范小艳 `2026-04` 重新执行手工抓取后，数据库中
            - `Q9rNLrsTTLyhmPPkuk8adA04041776998811` 已修正为 `2026-04-17 17:30:00 -> 2026-04-17 18:00:00`
            - `L6PsDFBwQFSye6Pz7crC7w04041775443807` 已修正为 `2026-03-31 13:30:00 -> 2026-03-31 17:30:00`
        - 同步策略同时调整：手工获取再次命中同一 `procInstId` 时，不再“已存在即跳过”，而是允许覆盖更新已有审批快照，用于修正先前已写入的错误时间。
    - `AttendanceApprovalMonthBO` 新增 `employeeIds` 与 `approvalTypes` 字段：
      - `employeeIds` 空列表表示全部员工，非空时只处理这些员工映射出的 `tbattendanceuser`；
      - `approvalTypes` 为空时直接报错 `请选择审批类型`；
      - 后端在写入 `tbattendanceapprove` 前，会按 `all/overtime/misscard/leave/travel` 对 `approveList` 做二次过滤，只保存本次选中的审批类型。
    - `2026-05-13` 审批类型匹配口径补强：
      - 用户反馈“获取审批数据功能没获取到钉钉类型的审批数据”；
      - 根因之一是 `HrmAttendanceApprovalSyncServiceImpl#matchesApprovalTypes(...)` 初版只按 `tagName == 补卡/请假/加班/出差/外出` 的精确值匹配；
      - 旧 `AttendanceRecordManager#SaveApproveList(...)` 则是把 `approveList` 原样落库，不会天然漏掉 `补卡申请 / 请假审批 / 外出审批` 这类名称变体；
      - 现已调整为“`bizType + tagName/subType` 关键字宽匹配”：
        - `misscard`：匹配 `tagName/subType` 中包含“补卡”；
        - `overtime`：匹配 `bizType=1` 或名称包含“加班”；
        - `leave`：匹配 `bizType=3` 或 `tagName/subType` 包含 `请假/事假/调休/病假/婚假/丧假/产假/陪产假/年假/补休`；
        - `travel`：匹配 `bizType=2` 或名称包含“出差/外出”。
    - 月份判重口径已于 `2026-05-13` 调整：
      - 不再通过 `tbattendanceapprove` 是否已有审批明细判断“该月是否已获取过”；
      - 新增完成标记表 `hrm_attendance_approval_fetch_mark`，按 `month_key + user_id + approval_type + fetch_version` 记录某个员工在某个月是否已按新版规则完整抓取成功；
      - 上线前必须先执行 `docs/sql/2026-05-13_hrm_attendance_approval_fetch_mark.sql`，否则新版判重与完成标记写入无法生效；
      - `2026-05-13` 现场已确认：`hr_0001 ~ hr_0005` 曾存在“表已创建但仍是旧结构”的状态，表现为缺少 `approval_type` 列且唯一键仍为 `uk_approval_fetch_mark_month_user_version`，会直接触发 `Unknown column '...approval_type' in 'where clause'`；
      - 因此迁移脚本已补充为兼容旧表升级：不仅补 `approval_type` 列，还会把旧唯一键切换为 `uk_approval_fetch_mark_month_user_type_version`；
      - 未选择员工时，仍可按新版完成标记统计实际抓取完成范围；
      - 选择员工时，仍可按所选员工映射出的 `userId` 查询对应完成标记；
      - `2026-05-13` 后续调整：`fetchMonthData` 已不再因为“当前员工范围 + 所选审批类型已全部完成”而拒绝再次抓取，前端也不再在手工获取前调用 `checkMonthData` 做拦截；
      - 若库里存在历史旧审批明细，但当前范围缺少新版完成标记，后端仍允许重新获取，避免补卡等类型永远补不回来。
    - 成功标记策略：
      - `HrmAttendanceApprovalSyncServiceImpl` 只有在整个月份抓取过程中所有目标员工、所有目标日期都成功拿到 `approveList` 响应后，才为这些员工写入完成标记；
      - 完成标记会按本次所选 `approvalType` 分别写入，避免“先抓加班后抓请假”被误拦截；
      - 若任一请求失败，当前实现直接抛错并终止，不写完成标记，确保后续还能重试补齐。
    - `2026-05-13` 晚间补充修复“4 月筛选混入 3 月审批”：
      - 现场库 `hr_0003.tbattendanceapprove` 已出现 `L6PsDFBwQFSye6Pz7crC7w04041775443807` 这类记录：本次手工抓取选择的是 `2026-04`，但审批业务开始时间实际是 `2026-03-31 13:30:00`；
      - 根因不是审批列表 SQL 误筛，而是 `fetchMonthData(...)` 先按钉钉 `processinstance/listids` 的“流程发生月份”取到实例，再把解析出的审批明细直接入库，缺少“业务时间是否仍属于所选月份”的二次校验；
      - 现已在 `HrmAttendanceApprovalSyncServiceImpl` 增加月内过滤：
        - 优先按 `beginTime` 判断业务月份；
        - `beginTime` 为空时回退 `workDate`；
        - 再为空时回退 `endTime`；
        - 若最终业务日期不属于所选 `YearMonth`，则跳过该审批实例，不写入 `tbattendanceapprove`。
      - 回归测试：
        - 新增 `fetchMonthData_shouldSkipApprovalWhenBusinessDateFallsOutsideSelectedMonth`；
        - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest -DfailIfNoTests=false test`（2026-05-13，通过，6 通过，0 失败）。
    - `2026-05-13` 夜间曾补充“按员工重抓前删旧”，该策略已在 `2026-07-13` 调整为“按员工重抓后清理陈旧快照”：
      - 业务要求：当用户只重抓某几个员工时，本地旧审批不能和新抓结果并存，否则会出现同月同类型脏混合；
      - 当前实现会先把 `employeeIds` 归一化，再通过 `tbattendanceuser.empId -> userId` 精确解析目标钉钉用户；
      - 同一 `procInstId` 已存在时允许覆盖业务时间等快照字段，但若本地 `subType` 与钉钉解析值不一致，则保留本地 `subType`，用于保护用户手工修正；
      - 本次返回实例全部保存后，再按 `beginTime` 月份区间 + `userId in (...)` + `tagName in (...)` + `id not in 本次返回ID` 删除该员工该月该类型陈旧审批；
      - 只有“指定员工重抓”才执行陈旧清理，“全部员工抓取”不会先整月清空，避免误删整公司历史快照。
      - 同名员工保护：
        - 清理口径严格基于 `employeeId -> userId`；
        - 即使两个员工 `userName` 同为“张三”，只会清理当前 `employeeId` 对应 `userId` 的审批快照。
      - 当前 `approvalType -> tagName` 清理映射：
        - `misscard -> 补卡`
        - `leave -> 请假`
        - `overtime -> 加班`
        - `travel -> 出差 + 外出`
        - `all -> 补卡 + 请假 + 加班 + 出差 + 外出`
      - 回归测试：
        - `fetchMonthData_shouldDeleteSelectedEmployeeMonthDataBeforeReFetch` 已调整为校验重抓后清理陈旧数据；
        - `fetchMonthData_shouldDeleteOnlyMatchedUserIdWhenEmployeesShareSameName` 已调整为校验同名员工场景只清理目标 `userId`；
        - 新增 `fetchMonthData_shouldPreserveManuallyEditedSubtypeWhenReFetchSameProcessInstance`。
    - `2026-05-15` 发布环境补充修复“审批重抓前删旧缺事务”：
      - 现场发布后调用“获取审批数据/审核数据”报错：`No EntityManager with actual transaction available for current thread - cannot reliably process 'remove' call`；
      - 根因是 `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 开头会调用 `tbattendanceapproveRepository#deleteByBeginTimeBetweenAndUserIdInAndTagNameIn(...)` 删旧，但该仓库派生删除方法最初没有显式事务边界；
      - Spring Data 默认仓库 `save` 有事务，而这个自定义派生 `deleteBy...` 在当前链路里会直接触发 `EntityManager.remove(...)`，发布环境因此在无事务线程上失败；
      - 本次采用最小修复：仅给 `tbattendanceapproveRepository#deleteByBeginTimeBetweenAndUserIdInAndTagNameIn(...)` 增加 `@Transactional`，避免把整个月份抓取过程包成长事务；
      - 新增契约测试 `HrmAttendanceApprovalServiceImplTest#deleteSelectedMonthDataRepositoryMethod_shouldDeclareTransactionalBoundary`，防止后续再次删掉该事务约束。
    - `2026-05-13` 夜间继续修复 `tbattendanceapprove` 时间列被数据库自动改写的结构性风险：
      - 现场确认 `hr_0001 ~ hr_0005` 的 `tbattendanceapprove` 都还是旧结构，`beginTime/endTime/createTime/workDate` 均带 `ON UPDATE CURRENT_TIMESTAMP`；
      - 这会导致审批重抓覆盖更新时，即使 Java 代码没有主动改这些业务时间字段，MySQL 也可能把它们自动刷成当前时间，进而污染月份筛选、排序和审批时间本身；
      - 已新增迁移脚本：`docs/sql/2026-05-13_tbattendanceapprove_remove_on_update.sql`；
      - 已执行到 `hr_0001 ~ hr_0005`，当前 5 个租户库的 `tbattendanceapprove` 均已修正为普通 `DATETIME DEFAULT NULL`，不再自动更新时间列。
  - 前端审批数据页新增：
    - 页头“获取审批数据”按钮；
    - 月份选择 + 审批类型多选 + 选择方式切换弹窗；
    - 审批类型未选择时前端直接提示 `请选择审批类型`；
    - 选择方式=员工时继续使用员工穿梭框，右侧未选员工时默认获取全部员工；
    - 选择方式=部门时，前端按所选部门树自动展开员工集合，并展示到穿梭框右侧；
    - 确认时直接调用手工获取接口；
    - 不再提示“所选月份已经获取过审批数据了”；
    - 获取成功后切换筛选月份并刷新列表。
  - 为支撑审批页按部门归集员工：
    - `SimpleHrmEmployeeVO` 新增 `deptId`；
    - `HrmEmployeeServiceImpl#transferSimpleEmp(...)` 现同步返回 `deptId + deptName`，前端不再依赖部门名称反推员工归属。
  - `2026-05-13` 调整审批数据页默认查询行为：
    - 页面首开时不再默认构造“当前月 `times` 范围”；
    - 因此首开会直接查询全部历史审批数据，而不是只看当前月；
    - 只有用户手动选择月份时，前端才通过 `buildApprovalMonthRange(...)` 生成 `times` 并带入查询；
    - 手工获取成功后的 `applyMonthFilter(...)` 行为保留，仍会自动切换到刚获取的月份。
  - `2026-05-13` 新增审批页快捷筛选：
    - 前端纯函数 `buildApprovalQuickFilter(...)` 负责生成一级快捷筛选条件：
      - `all`
      - `misscard`
      - `leave`
      - `overtime`
      - `travel`
    - `buildApprovalLeaveSubtypeQuickFilter(...)` 负责生成“请假子类型快捷按钮”的查询条件；
    - 页面点击快捷按钮后，会直接覆盖 `bizTypes/tagNames/subTypes` 并刷新列表；
    - 当前激活状态通过比较当前表单筛选值与快捷筛选目标值计算。
  - `2026-05-13` 查询筛选口径补强：
    - `HrmAttendanceApprovalMapper.xml` 中 `tagName/subType` 已从精确 `in (...)` 改为 `like '%关键词%'`；
    - 页面选择“补卡”“病假”等筛选时，可以命中钉钉侧常见的名称变体，而不是只认完全相等的中文值。
  - `2026-05-13` 新增根因证据：
    - 现场继续反馈“手工获取后仍是 0 条”，但钉钉管理员确认企业里存在大量加班、调休、请假等审批；
    - 已用当前应用 `appKey=ding3xvbtg2wl2np2yjc` 获取到的真实 token 直接调用钉钉 `topapi/processinstance/listids`；
    - 钉钉返回明确权限错误：缺少 `qyapi_aflow`；
    - 这说明当前应用没有审批/工作流读取权限，至少无法通过审批实例接口读取管理员在钉钉里看到的完整审批数据；
    - 因此当前实现依赖的 `attendance/getupdatedata -> approveList` 更可能只是考勤侧的“当天更新快照”，不能等价视为“企业全部审批实例来源”。
    - `2026-05-13` 后续修复动作：
      - 后端同步实现已完成从 `attendance/getupdatedata` 到“`process.template.list` 自动发现流程编码 + `processinstance/listids + processinstance/get`”的切换；
      - 已修复 `processinstance/listids` 缺少 `processCode` 导致的 `errcode=40`；
      - 审批抓取失败提示已补强：缺少 `qyapi_aflow` 时，前端将直接看到明确中文权限提示，而不是泛化重试文案；
      - 单元测试已覆盖审批实例解析、workflow 抓取落库、既有 controller/service 契约不变；
      - 剩余阻塞点是钉钉管理员为当前应用开通 `qyapi_aflow` 后，再做真实联调验证。
    - `2026-05-13` 现场新增根因证据：
      - 直接核验 `hr_0003.tbattendanceuser` 与钉钉 `topapi/v2/user/get` 后确认：
        - `1831601326890434651 -> 185339132332994951 -> 范小艳`
        - `1831601326890434600 -> 185339132832825925 -> 苏中心`
      - 直接调用钉钉 `topapi/processinstance/listids` 验证：
        - 对 `范小艳 / 185339132332994951 / 2026-04 / 加班审批`，可查到 `2` 条实例；
        - 其中一条详情为“范小艳提交的加班”，审批日期 `2026-04-17 17:30 ~ 18:00`，结果 `agree`；
        - 对 `苏中心 / 185339132832825925 / 2026-04 / 加班审批`，返回 `0` 条实例。
      - 因此前一轮“查不到范小艳 4 月加班”的服务端日志，实质是在查 `苏中心`，不是审批接口本身拿不到范小艳的数据。
      - 为缩短后续排查路径，`HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 现已在开始日志中追加 `resolvedUsers` 摘要，格式为 `employeeId/姓名/dingTalkUserId`，可直接核对前端选人和后端实际抓取对象是否一致。

- 审批数据页无数据排查结论（2026-05-08）：
  - 前端审批数据页当前只调用本地接口 `/hrmAttendanceApproval/queryPageList`，未直接调用钉钉接口。
  - 后端审批数据查询只读本地表 `tbattendanceapprove`，SQL 本身没有额外联表过滤到“必须存在员工主数据”才返回；即使员工档案缺失，也会回退展示 `tbattendanceuser.userName`。
  - 现场核验 `hr_0001 ~ hr_0005` 多个租户库后，`tbattendanceapprove` 均为 `0` 条，因此页面“无数据”的直接原因不是前端展示问题，也不是当前筛选条件把已有数据过滤空，而是本地审批快照表本身为空。
  - 当前 `attendanceData/sync` / `attendanceData/syncAll` 主同步链路只包含：
    - 组织架构
    - 用户信息
    - 考勤计划
    - 考勤明细
    - 报表字段
    - 请假与假期数据
  - 当前主同步链路未注入也未调用 `AttendanceRecordManager` / `IRecordManager`，因此不会把审批数据写入 `tbattendanceapprove`。
  - 现存审批入库逻辑仅位于 `AttendanceRecordManager#SaveApproveList`，其数据源是钉钉 `attendance/getupdatedata` 返回的 `approveList`；该逻辑目前未接入用户前台“同步考勤”入口。
  - 结论：审批数据页本身已按“只读本地审批快照”要求实现，但若要让页面真正有数据，必须先解决“同步考勤是否允许补充审批同步步骤”的产品约束；在用户明确禁止私自调用钉钉接口的前提下，后续开发需先征得确认。
  - `2026-05-13` 现场补充：
    - 再次核验 `hr_0001 ~ hr_0005` 后，`tbattendanceapprove` 当前仍均为 `0` 条；
    - `hr_0003` 曾出现“`hrm_attendance_approval_fetch_mark` 有 `overtime` 完成标记，但 `tbattendanceapprove` 仍为空”的不一致状态；
    - 对真实用户 `185339132832825925` 和日期 `2026-04-15 00:00:00` 直调钉钉 `attendance/getupdatedata`，返回成功但 `approve_list=[]`；
    - 随后又直调钉钉 `topapi/processinstance/listids`，返回缺少 `qyapi_aflow` 权限；
    - 这说明现场问题除“名称变体匹配过窄”外，更大的架构性风险是“当前应用选用的上游接口与实际审批权限模型不匹配”。

- 新增考勤管理“审批数据”功能（2026-05-08）：
  - 后端新增独立查询链路：
    - 控制器：`HrmAttendanceApprovalController`
    - 服务：`IHrmAttendanceApprovalService`、`HrmAttendanceApprovalServiceImpl`
    - Mapper：`HrmAttendanceApprovalMapper` + `HrmAttendanceApprovalMapper.xml`
  - 数据源限定为本地 `tbattendanceapprove`，并通过以下本地关联补齐展示字段：
    - `tbattendanceapprove.userId -> tbattendanceuser.userId`
    - `tbattendanceuser.empId -> hrm_employee.employee_id`
    - `hrm_employee.dept_id -> hrm_dept.dept_id`
  - SQL 查询能力：
    - 支持按姓名/手机号关键字、员工、部门、审批业务类型、月份范围筛选；
    - `2026-05-13` 起补充支持按 `tagName`（如补卡/请假/加班/出差/外出）和 `subType`（如事假/调休/病假等）筛选；
    - 默认按 `beginTime desc, createTime desc` 排序；
    - 若员工主数据缺失，则姓名回退显示 `tbattendanceuser.userName`。
  - 约束落实：
    - 新增查询服务未注入任何钉钉 SDK、`IAccessToken` 或其他在线拉取依赖；
    - 新功能只消费本地审批快照，不会触发钉钉接口调用。

- 同步考勤“待选员工 99 人”排查结论（2026-05-08）：
  - 前端入口：`hr_web/src/views/hrm/employee/Index.vue` 中“同步考勤”弹窗左侧待选员工，已改为调用 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList`，不再调用普通员工列表接口。
  - 后端口径：`HrmSalaryMonthRecordController#queryComputeSalaryEmployeeList` -> `SalaryMonthRecordServiceNew#queryComputeSalaryEmployeeList` -> `queryHasSalaryArchivesEmployeeList(...)`。
  - 该口径不是 `hrm_employee` 全量，也不是 `/hrmEmployee/queryPageList` 的“员工列表”口径，而是“最新薪资月记录对应的计薪员工 + 薪资档案合计大于 0”的交集。
  - `hr_0003` 现场核验结果：
    - `hrm_employee.is_del=0` 共 `127` 人；
    - 其中满足同步考勤待选基础口径的共 `99` 人：`is_del=0`、`entry_time <= 2025-12-31`，且 `entry_status in (1,3)`，或 `entry_status=4` 且离职计划日晚于月末前 1 个月；
    - 上述 `99` 人的薪资档案合计（`SUM(hrm_salary_archives_option.value)`）也全部大于 `0`，因此同步考勤弹窗最终显示 `99` 人；
    - 另外有 `26` 人因 `entry_time` 为空未进入待选列表，`2` 人因 `entry_time > 2025-12-31`（最新薪资月记录月末）未进入待选列表；
    - 在当前 `hr_0003` 数据下，进入基础口径的 `99` 人薪资档案合计全部大于 `0`，因此本次人数差异不是薪资档案再额外过滤出来的。
  - 结论：用户看到的“员工列表一百多人”与“同步考勤待选员工 99 人”本身就是两个不同业务口径，当前表现符合现有前后端实现与需求定义。
- 登录“账号不存在”排查结论（2026-05-08）：
  - 入口：`LoginController#Login`，先调用 `LoginUserMapper#getCompanyIdByUserName`，再调用 `getByAcountAndCompanyID`。
  - 实际 SQL 链路：
    - `hrsystem.tbAllUserList`：按 `account` 查询租户 `CompanyID`；
    - `hr_${companyId}.view_loginuser`：按 `account` 查询登录用户视图；
    - `view_loginuser` 底层依赖 `tbloginuser`、`hrm_dept`、`tbroletypes` 三张表的内连接。
  - 现场核验：
    - `hbadmin` 在 `hrsystem.tbAllUserList` 中存在，映射到租户 `0003`；
    - `hr_0003.tbloginuser` 中不存在 `hbadmin`；
    - 因此 `hr_0003.view_loginuser` 也查不到该账号，控制器返回“`hbadmin在系统中不存在!`”。
  - 风险补充：
    - 即使 `tbloginuser` 中存在账号，只要 `depId -> hrm_dept.dept_id` 或 `roleId -> tbroletypes.id` 任一关联缺失，`view_loginuser` 仍会因为内连接被过滤，最终同样表现为“账号不存在”。
- 新增版本级 PRD 文档（2026-04-20）：
  - 新建 `docs/PRD.md`，将现有 `requirements.md` 与 `development.md` 中分散的内容重组为产品视角主文档；
  - PRD 主体聚焦业务背景、角色、模块目标、关键规则、非功能要求与验收口径；
  - 原 `requirements.md` 继续保留为规则台账，`development.md` 继续保留为实现说明与测试记录。
- 新增同步考勤链路 Excel 文档（2026-04-19）：
  - 新增 `docs/generate_dingtalk_attendance_excel.py`，用于生成业务阅读版 `docs/钉钉接口及数据库.xlsx`；
  - Excel 按“同步总览、接口明细、落库表明细、非直接落库表、代码位置”分工作表输出，比 Markdown 更适合筛选和查阅；
  - `docs/钉钉接口及数据库.md` 继续保留为补充说明版本，方便后续直接编辑文本源。
- 新增同步考勤链路说明文档（2026-04-19）：
  - 新建 `docs/钉钉接口及数据库.md`，专门整理 `/attendanceData/sync`、`/attendanceData/syncAll` 的钉钉接口与直接落库表；
  - 文档按同步步骤拆分为“组织架构/用户映射/排班计划/打卡明细/报表字段/报表数据”六类内容；
  - 明确区分“本同步入口直接写入的表”和“考勤相关但非本入口直接写入的表”，避免后续排查时把 `hrm_attendance_date_shift`、`hrm_emp_schedule`、`hrm_attendance_history_shift` 混入同步主链路。
- 修复“跨天班次夜班次数未计入”问题（2026-04-16）：
  - 现象：`HrmOvertimeNightStatisticsServiceImpl` 在 `开始统计` 与 `单人统计` 中，会先按 `attendanceTime/workDate` 给打卡记录分业务日期；当跨天班次的下班卡落在次日 `04:00` 一类时间点时，该 `OffDuty` 打卡会被分到次日，导致原工作日只拿到上班卡，夜班次数始终为 `0`。
  - 影响范围：同时影响标准班次与本地自定义班次；只要班次跨天且次日下班卡晚于凌晨 `03:00`，原实现都会漏算夜班，跨天工时也会少算。
  - 月份边界补充：若该跨天班次发生在月末最后一天，旧实现还会把次月 `1` 号凌晨下班卡对应的业务日单独保留在候选日集合里，最终多生成一条 `work_date=次月1号`、但统计月份仍属于上月的脏明细。
  - 处理：
    - 统计服务在解析出“班次结束时间跨天”后，会额外读取次日的打卡记录与考勤明细；
    - 只把“次日首个上班卡之前的下班卡”并回原工作日，避免把第二天白班/午休等无关 `OffDuty` 误算进前一晚；
    - 原工作日的夜班判断、实际下班时间与工时计算统一使用合并后的打卡序列，`开始统计` 与 `单人统计` 共享同一套逻辑；
    - 新增“已并回前一天的次日下班卡消费标记”，后续在次日统计时会先剔除这些已消费的 `OffDuty` 记录，避免再次生成重复日明细；
    - 候选工作日按日期顺序处理，确保“前一天消费次日凌晨下班卡”一定先于“次日生成明细”发生。
  - 验证：
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（2026-04-16，通过，19 通过，0 失败）。
- 修复“加班统计未读取本地自定义排班”问题（2026-04-13）：
  - 现象：员工当天已通过 `tbplanlist.shift_source=custom + custom_shift_id` 保存本地自定义排班后，`HrmOvertimeNightStatisticsServiceImpl` 仍只按 `hrm_attendance_plan.classId`、考勤组 `shiftSetting` 或 `hrm_attendance_date_shift` 恢复班次；
  - 根因：加班统计链路完全没读取 `tbplanlist` 与 `hrm_workplan_custom_shift`，导致像“许泽刚 2026-03-20”这类场景仍按同步来的旧班次计算；
  - 处理：
    - 新增 `tbattendanceuserRepository`、`tbPlanListRepository`、`hrmWorkPlanCustomShiftRepository` 注入；
    - 先按 `employeeId -> tbattendanceuser.userId -> tbplanlist` 找到员工当天本地排班；
    - 命中 `shift_source=custom` 或 `custom_shift_id` 后，把 `hrm_workplan_custom_shift` 转成统计链路可复用的 `HrmAttendanceShift`；
    - 统计时本地自定义班次优先级高于 `classId/group/date_shift` 回退，用于开始时间、结束时间和夜班判断；
    - 若当天没有本地自定义班次，则继续按已落库的钉钉标准班次 `classId -> hrm_attendance_shift / groupId -> shiftSetting` 取排班时间，只有标准班次也无法恢复时才回退 `date_shift`。
  - 验证：
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldPreferLocalCustomShiftWhenEmployeeHasCustomPlan -DfailIfNoTests=false test`（2026-04-13，通过）。
    - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（2026-04-13，通过，15 通过，0 失败）。
- 修复同步考勤组在钉钉 API 月额度超限时污染本地快照的问题（2026-04-13）：
  - 现象：`AttendanceGroupManager#GetAndSave` 原实现一进入方法就先 `deleteAll` 本地考勤组/关联表，再循环调用钉钉 `topapi/attendance/getsimplegroups`；
  - 根因：当钉钉返回“您的企业本月api调用量已超过限制”等失败时，旧代码只在 `rsp.isSuccess()` 为 `false` 时静默结束，不抛异常，导致事务正常提交，本地留下“组表和班次表不一致”的半套快照；
  - 处理：
    - 新增 `fetchCurrentSnapshot()`，先把远端考勤组、班次、部门关系、人员关系完整拉到内存；
    - 远端任一页拉取失败时直接抛 `ApiException(rsp.getErrmsg())`；
    - 新增 `replaceLocalSnapshot(...)`，只有在快照拉取成功后才统一 `deleteAll + saveAll` 替换本地表；
    - `buildAttendanceShifts(...)` 改为纯构建列表，不再在远端遍历过程中边删边写本地表。
  - 影响：即使后续再次遇到钉钉月额度超限，也只会同步失败，不会再把本地 `hrm_attendance_group.shift_setting` 和 `hrm_attendance_shift` 留成半残状态。
- 修复“添加排班”人员列表偶发空白问题（2026-04-12）：
  - 现象：前端 `AddOrEdit.vue` 打开时会并行调用 `attendanceData/getAllUsers` 预加载人员；当展示缓存过期后，旧实现会在 `WorkPlanServiceImpl#refreshUsersCache(..., displayCache=true)` 中重新逐组调用钉钉 `group/memberusers/list`。
  - 风险：该接口分组逐页直连钉钉，且失败时旧代码只 `break` 不抛错；一旦遇到限流、权限或网络波动，前端就可能只看到空人员列表，而不是明确报错。
  - 处理：展示链路改为优先读取本地 `tbattendanceuser` 快照构建 `getAllUsers_display` 缓存，仅在本地快照为空时才回退钉钉接口；提交链路 `getUsers` 仍保留原有 10 分钟钉钉缓存逻辑。
- 修复“添加排班”标准班次时间缺失问题（2026-04-12）：
  - 现象：同样在 `AddOrEdit.vue` 打开时，标准班次下拉调用的是 `attendanceData/getAllShifts`；旧实现仍走 `planService.getAllGroups()`，与 `getGoupList` 的展示缓存链路不一致。
  - 风险：当钉钉考勤组实时刷新受限流、权限或瞬时失败影响时，班次列表可能退化为只剩班次名/休假，无法稳定带出 `begin1/end1` 等时间段。
  - 根因补充：`HrmAttendanceDataController` 原本按“`times.size()==2` 且第 1 个是 `OnDuty`、第 2 个是 `OffDuty`”硬编码提取时间；一旦钉钉返回顺序变化，即使接口成功也会把班次时间解析成空。
  - 进一步处理：`HrmAttendanceDataController#getAllShiftList` 现改为优先读取 `planService.getAllGroupsForDisplay()` 的考勤组展示缓存，直接按 `selectedClass` 生成标准班次选项；仅当展示缓存为空时，才回退本地 `hrm_attendance_shift + hrm_attendance_group` 快照兜底。
  - 标准班次下拉兼容策略：为了不改前端现有 `getShiftLabel` 逻辑，后端返回的 `ShiftItem.groupName` 已改为 `考勤组 / 班次名` 组合文案，因此页面最终展示为 `考勤组 / 班次名(时间)`。
  - 同一 `classId` 若出现在不同考勤组下，展示链路不再按 `shiftId` 全局去重，而是按 `groupId + shiftId` 保留独立选项，避免不同考勤组互相覆盖。
  - 兜底解析：展示缓存回退链路中的 `selectedClass.sections` 仍改为按 `checkType=OnDuty/OffDuty` 过滤，不再依赖数组固定顺序与固定长度。
- 收敛“标准班次下拉垃圾数据”范围（2026-04-13）：
  - 现象：即使同步修库后，标准班次下拉仍可能混入固定班制、无时段班次或本地 `shift_setting` 已失效的旧班次。
  - 处理：
    - `HrmAttendanceDataController#getAllShiftList` 在读取展示缓存时，新增 `type=TURN` 过滤，并要求班次至少存在一段完整上下班时间；
    - 若本地 `hrm_attendance_group.shift_setting` 与 `hrm_attendance_shift` 快照可用，则展示缓存结果会和本地“当前仍挂载的班次”求交集，只保留已经修复成一致口径的班次；
    - 当展示缓存为空或交集后无有效班次时，再退回本地快照；本地兜底链路同样只保留 `shift_setting` 中仍挂载、且具备完整时段的班次。
  - 验证：
    - 为 `HrmAttendanceDataControllerTest` 新增 2 条回归用例，分别覆盖“展示缓存含垃圾数据时只保留有效班次”和“本地兜底时过滤失效班次”。
- 新增“打卡概况单员工单日自定义排班”后端能力（2026-04-13）：
  - 新增接口：`/workPlan/queryEmployeeDayShift`、`/workPlan/saveEmployeeDayCustomShift`。
  - 查询逻辑：
    - 先用 `employeeId -> tbattendanceuser.userId` 找到考勤用户；
    - 再查当天 `tbplanlist` 中包含该 `userId` 的本地排班，优先返回本地自定义班次；
    - 若当天没有本地排班，则回退 `IHrmAttendanceShiftService#getEmpHrmAttendanceShift(...)`，把同步考勤链路中的当天班次作为编辑参考返回。
  - 保存逻辑：
    - 仍复用 `hrm_workplan_custom_shift` 做本地自定义班次去重与复用；
    - 当目标员工当天排班来自“多人共用一条 `tbplanlist`”时，先把该员工从共享记录拆出，保留其他员工原记录；
    - 再单独为该员工写入/更新一条 `shiftType=custom` 的本地排班记录，避免改单人时误伤同组其他员工。
    - 若当天没有任何本地 `tbplanlist` 可复用，保存入口会把 `ProductName/LinkName/GroupID` 兜底为非空字符串/当前考勤组，避免数据库对旧表结构的非空约束触发 `ConstraintViolationException`。
  - 当前状态：
    - 后端接口、拆分逻辑和回归测试已落地；
    - 打卡概况前端页面位于独立仓库 `../hr_web`，本次会话写权限仅覆盖 `hainan` 项目，前端弹窗接线未能在当前沙箱内直接提交代码，需要在前端仓库补上调用。
    - 前端接入说明已整理到 `docs/plans/2026-04-13-clock-overview-custom-shift-frontend-integration.md`。
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
- 修复打卡概况日期缺失导致前端错位问题：
  - 位置：`HrmAttendanceClockServiceImpl#queryAttendanceEmpDetailByDate`。
  - 原逻辑在 `startAttendanceRecordVO` 或 `endAttendanceRecordVO` 为空时直接 `continue`，会跳过该日期。
  - 新逻辑改为写入 `time=[]` 后继续保留当天 `date`，保证每位员工在查询区间内日期维度完整。
- 修复排班日期解析不稳定问题（`Parse [2026-01-04] with format [yyyy-MM-dd HH:mm:ss] error!`）：
  - 位置：`WorkPlanListController`。
  - `saveAll` 改为手动解析 `Data` JSON，`workDate/createTime` 统一走多格式日期解析（支持 `yyyy-MM-dd`、`yyyy-MM-dd HH:mm:ss`、常见 ISO、13位时间戳）。
  - `getData/loadIsLast/exportExcel` 的 `Begin/End/SelectDate` 改为同一套多格式解析，避免调用方日期格式略有差异时随机失败。
  - 保持原接口路径与参数不变，仅增强日期兼容能力。
- 优化打卡概况缺失记录兜底策略（`HrmAttendanceClockServiceImpl#queryAttendanceEmpDetailByDate`）：
  - 当上下班记录都缺失时仍返回空 `time` 占位；
  - 当仅缺一端打卡时不再整体置空，改为缺失端按 `-3`（缺卡）补齐，保留另一端真实打卡状态，避免“大部分无数据”假象。
- 修复“同一天多次打卡只显示两条”问题（2026-04-16）：
  - 现象：打卡概况分页接口 `/hrmAttendanceEmpMonthRecord/queryAttendanceEmpMonthDailyDetailPageList` 在同一天存在 4 次及以上打卡时，页面仍只显示 2 条时间。
  - 根因：
    - `HrmAttendanceClockMapper#queryAttendanceClockList` 按“员工 + 日期 + 打卡类型”分组后只保留一条记录；
    - `HrmAttendanceClockServiceImpl#queryAttendanceEmpDetailByDate` 也按“一个上班卡 + 一个下班卡”固定组装 `time` 数组。
  - 处理：
    - 新增月概况专用查询 `queryAttendanceClockTimelineList`，按日期区间返回全部打卡记录，不再按天裁剪；
    - `queryAttendanceEmpMonthDetailPageList` 改用该专用查询，避免影响仍依赖旧模型的其他统计逻辑；
    - `queryAttendanceEmpDetailByDate` 在检测到“同一天多次上班卡/下班卡”时，改为按 `clock_time` 顺序返回当天完整时间线；普通两次打卡场景继续保持现有缺卡兜底行为。
  - 验证：
    - 新增 `HrmAttendanceClockServiceImplTest#queryAttendanceEmpMonthDetailPageList_shouldReturnAllClockTimesForSingleDay_whenEmployeeHasMultipleClocks`；
    - `mvn -Dtest=HrmAttendanceClockServiceImplTest -DfailIfNoTests=false test`（2026-04-16，通过，1 通过，0 失败）。
- 修复工资核算社保校验误拦截（6012）：
  - 入口：`SalaryMonthRecordServiceNew#validateInsuranceData`。
  - 原逻辑：社保月主记录存在但 `status != 1` 时直接抛 `6012`，导致“有员工社保明细但月主记录未完成”的场景无法生成工资。
  - 新逻辑：
    - 先按 `socialSecurityMonthType` 计算目标社保年月并校验月主记录是否存在；
    - 若月主记录未完成，再检查该年月是否存在 `status=1` 的员工社保月记录；
    - 存在有效员工明细则放行薪资核算并记录告警日志；
    - 不存在有效明细才继续抛 `6012`。
- 修复部门查询转换异常：
  - 现象：`hrmDeptRepository.findAllByDeptIdIn` 声明返回 `List<SimpleHrmDeptVO>`，运行期 JPA 实际返回 `HrmDept` 实体，触发 `No converter found ... HrmDept -> SimpleHrmDeptVO`。
  - 处理：
    - 将仓库方法签名改为返回 `List<HrmDept>`（实体）；
    - 在 `SalaryMonthRecordServiceNew` 中手动映射 `deptId/name` 到 `deptNameMap`，去掉对 JPA 自动 VO 转换的依赖。
- 新增薪资核算真实进度能力：
  - 新增接口：`/hrmSalaryMonthRecord/queryComputeProgress`，返回 `progress/status/stage/message/processedCount/totalCount`。
  - 在 `SalaryMonthRecordServiceNew#computeSalaryData` 中增加进度状态缓存（按租户 + 薪资记录 + 员工范围隔离）。
  - 进度更新策略：
    - 预处理阶段写入 `PREPARE/LOAD_DATA`；
    - 员工循环核算阶段按真实 `processedCount/totalCount` 计算百分比（`CALCULATE`）；
    - 落库阶段写入 `PERSIST`，完成后写入 `FINISH`，异常写入 `ERROR`。
  - 增加过期清理策略，避免进度缓存无限堆积。
- 补齐考勤同步实时进度接口与参数校验（2026-04-03）：
  - `HrmAttendanceDataController` 新增 `/attendanceData/getSyncProgress`，直接返回服务层进度对象，供前端轮询；
  - `sync/syncAll` 增加参数校验，`Begin/End`（以及 `sync` 的 `EmpID`）为空时返回可读错误，避免 `SimpleDateFormat.parse` 空指针；
  - `HrmAttendanceDataServiceImpl#getSyncProgress` 扩展返回：
    - `currentStep/processedCount/totalCount/progress/percent/status/message`；
    - 进度百分比按步骤与已处理人数计算，默认限制在 `0~99`，由前端在成功态收敛到 `100`。
- 补齐薪资核算口径员工查询接口（2026-04-03）：
  - `HrmSalaryMonthRecordController` 新增 `/hrmSalaryMonthRecord/queryComputeSalaryEmployeeList`（`srecordId` 可选）；
  - `SalaryMonthRecordServiceNew` 新增 `queryComputeSalaryEmployeeList(Long sRecordId)`，直接复用 `queryHasSalaryArchivesEmployeeList`，确保与 `computeSalaryData` 口径一致。
- 修复大批量同步考勤 `Communications link failure`（2026-04-03）：
  - 根因：项目使用自建动态多租户数据源，部分租户 `HikariDataSource` 在运行期动态创建时未统一配置连接生命周期与校验策略，长任务后可能借出失效连接；
  - 新增 `DataSourcePoolConfigurator`，统一为默认库/租户库应用连接池参数（`maxLifetime/idleTimeout/validationTimeout/connectionTestQuery` 等）；
  - `ConnectionParsor`、`MyBatisConfig`、`DynamicDataSource`、`CompanyDataSourceProvider` 统一接入该配置器；
  - 在 `HrmAttendanceDataServiceImpl` 并行同步链路增加数据库连接异常有限重试（默认 3 次），对 `CommunicationsException`/`JDBCConnectionException` 等瞬时断连场景自动恢复。
- 修复动态数据源 `Public Key Retrieval is not allowed`（2026-05-13）：
  - 根因：`application-dev.properties` / `application-prod.properties` 的主数据源 URL 已包含 `allowPublicKeyRetrieval=true`，但运行期为租户库手工拼接的 JDBC URL 仍沿用旧模板，缺少该参数；
  - `application-temp.properties` 的主数据源 URL 也存在同样缺参风险；
  - 在 `ConnectionParsor` 新增共享方法 `buildMysqlJdbcUrl(server, port, database)`，统一输出带 `allowPublicKeyRetrieval=true` 的 MySQL URL；
  - `DynamicDataSource`、`CompanyDataSourceProvider`、`MyBatisConfig` 全部切换为复用该共享方法，避免 3 处字符串再次漂移；
  - `application-temp.properties` 同步补齐 `allowPublicKeyRetrieval=true`；
  - 新增回归测试 `ConnectionParsorTest`，锁定动态数据源 URL 必须携带该参数。
- 修复租户数据源 `Access denied for user 'root'@'175.7.12.32'`（2026-05-13）：
  - 根因：主 `dev` 数据源 `application-dev.properties` 已改为可用密码 `hainandachuan_123456`，但运行期租户数据源账号密码并不直接来自该文件，而是来自主库 `hrsystem.tbCompanyList.url` 中保存的连接串；
  - `tbCompanyList` 中 `0001 ~ 0005` 的 5 条租户连接元数据仍保存旧密码 `tianyegufen_123456`，导致默认库可连、租户库在首次切换时统一报 `Access denied`；
  - 现场验证：
    - 直连 `hrsystem` 使用 `root / hainandachuan_123456` 成功；
    - 直连 `hr_0001` 使用旧密码 `tianyegufen_123456` 精确复现 `Access denied for user 'root'@'175.7.12.32'`；
    - 改用 `hainandachuan_123456` 后直连 `hr_0001` 成功；
  - 处理：
    - 直接更新 `tbCompanyList` 中 5 条租户连接串，把 `Password=tianyegufen_123456` 替换为 `Password=hainandachuan_123456`；
  - 验证：
    - 更新后再次查询 `tbCompanyList`，`0001 ~ 0005` 均已切换为新密码；
    - 逐个直连 `hr_0001 ~ hr_0005` 全部成功。

## Recent Changes and Decisions (2026-04-12, 添加排班自定义班次后端实现)
- `/workPlan/saveAll` 已从“同步保存 + 同步推钉钉”改为“创建任务 + 后台异步执行”：
  - 返回体 `data` 现在包含 `taskId/status/message`；
  - 新增 `/workPlan/querySubmitProgress`，返回 `status/stage/retryCount/totalCount/successCount/failCount/errors`。
- 当前实现采用轻量落地方案，避免本次改动强依赖数据库新增字段：
  - 自定义班次输入字段通过 `tbplanlist` 的 `shiftType/customStart/customEnd/customCrossDay` 承载，其中 `shiftType` 已持久化到列 `shift_source`；
  - `tbplanlist.custom_shift_id` 用于关联本地自定义班次事实表；
  - 已保存排班的自定义班次元数据优先通过 `custom_shift_id -> hrm_workplan_custom_shift` 回填到 `getData/loadIsLast/exportExcel`；
  - 本次未新增任务表/映射表，进度状态使用内存 `ConcurrentHashMap` 保存；
  - 本地自定义班次事实数据迁移为独立表 `hrm_workplan_custom_shift`，通过时间段去重，不再复用 `hrm_attendance_shift`。
- 自定义班次执行路径已按最新业务改为“仅本地保存”：
  - `submitPlans` 先提交标准班次到钉钉，再单独处理自定义班次；
  - `buildLocalCustomAssignments` 会先按 `HH:mm + 跨天` 归一化自定义时间，再查询 `hrm_workplan_custom_shift`；
  - 若已存在相同单段班次，则直接复用现有 `id`；
  - 若不存在，则新增一条本地自定义班次，名称格式为 `自定义班次 HH:mm-HH:mm`；
  - 自定义班次最终通过 `tbplanlist.shift_source=custom + custom_shift_id` 落库，不再调用钉钉创建班次、挂考勤组或排班接口。
- 已补充本地自定义班次下拉接口：
  - 新增 `/workPlan/queryCustomShiftList`；
  - 服务层按 `updateTime desc, id desc` 返回本地自定义班次；
  - 返回字段包含 `id/shiftName/customStart/customEnd/customCrossDay/displayName`，前端可直接用于下拉选择；
  - 若用户在前端选中已有班次后再手动改时间，应清空 `customShiftId`，后端会重新按时间去重。
- 本地排班持久化已按“最终解析结果”收口：
  - 远端提交成功后，先得到“行 + 员工 -> resolvedGroupId/resolvedShiftId”结果；
  - 同一输入行若最终落到不同考勤组或班次，本地会拆成多条 `tbplanlist`；
  - 同一输入行若多个员工最终落到相同考勤组和班次，本地会合并为同一条 `tbplanlist`，`userId` 以逗号拼接保存。
- 失败与缓存策略已按业务确认实现：
  - 后端自动重试最多 10 次；
  - 错误统一转中文，不直接透出英文 DingTalk 文案；
  - 错误明细精确到“行 + 员工 + 考勤组 + 班次/时间段”；
  - 提交链路 `getAllGroups/getUsers` 保持 10 分钟缓存；
  - 展示链路 `attendanceData/getGoupList/getAllUsers/getUsersByGroup` 改为独立 30 分钟缓存；
  - 员工调组后，主动清理相关展示/提交缓存；
  - 自定义班次新增时不再触发钉钉缓存失效，因为该路径已经完全脱离钉钉。

## Testing
- 审批数据手工获取验证：
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`（2026-05-09，通过；覆盖按月份/按员工范围判重与员工范围透传）。
  - `node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-api.test.mjs`（2026-05-09，通过）。
  - `node /Users/jiangyongming/Project/hr/hr_web/tests/attendance-approval-fetch-utils.test.mjs`（2026-05-09，通过）。
  - `npm run build`（2026-05-09，通过；保留既有 `::v-deep` 过时警告与 chunk size warning）。

- 审批数据后端验证：
  - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest -DfailIfNoTests=false test`（2026-05-08，通过）。
- 编译验证：`mvn -DskipTests compile`（通过）。
- 回归用例（单测）：`mvn -Dtest=WorkPlanServiceImplTest#addAll_shouldCreateNewRecord_whenLoadedFromLastDateAndWorkDateChanges -DfailIfNoTests=false surefire:test`（通过）。
- 加载逻辑回归（单测）：`mvn -Dtest=WorkPlanServiceImplTest -DfailIfNoTests=false surefire:test`（4 通过，0 失败）。
- 社保校验回归（单测）：`mvn -Dtest=SalaryMonthRecordInsuranceValidationTest -DfailIfNoTests=false surefire:test`（2 通过，0 失败）。
- 本次修复验证：`mvn -DskipTests compile`（通过，包含 `hrmDeptRepository` 与 `SalaryMonthRecordServiceNew` 变更编译校验）。
- 本次修复验证：`mvn -DskipTests compile`（2026-03-27，通过，包含 `HrmAttendanceClockServiceImpl` 变更编译校验）。
- 本次修复验证：`mvn -Dtest=WorkPlanListControllerTest -DfailIfNoTests=false surefire:test`（2026-03-27，通过）。
- 本次修复验证：`mvn -DskipTests compile`（2026-03-27，通过，包含 `WorkPlanListController` 日期兼容解析改动）。
- 本次修复验证：`mvn -DskipTests compile`（2026-03-27，通过，包含 `HrmAttendanceClockServiceImpl` 单端缺卡补齐逻辑改动）。
- 本次修复验证：`mvn -DskipTests compile`（2026-04-02，通过，包含 `HrmSalaryMonthRecordController`、`SalaryMonthRecordServiceNew`、`SalaryComputeProgressVO` 的真实进度改造）。
- 本次修复验证：`mvn -DskipTests compile`（2026-04-03，通过，包含 `HrmAttendanceDataController`、`HrmAttendanceDataServiceImpl` 实时进度接口与参数校验改造）。
- 本次缓存拆分验证：`mvn -Dtest=WorkPlanListControllerTest,HrmAttendanceDataControllerTest,WorkPlanCustomShiftResolverTest,WorkPlanServiceImplTest -DfailIfNoTests=false test`（2026-04-12，通过，21 通过，0 失败）。
- 本次修复验证：`mvn -DskipTests compile`（2026-04-03，通过，包含 `HrmSalaryMonthRecordController`、`SalaryMonthRecordServiceNew` 计薪员工查询接口改造）。
- 本次修复验证：`mvn -DskipTests compile`（2026-04-03，通过，包含动态数据源连接池统一配置与同步链路数据库连接异常重试改造）。
- 本次修复验证：`mvn -Dtest=AttendanceGroupManagerTest,HrmAttendanceDataControllerTest -DfailIfNoTests=false test`（2026-04-13，通过，12 通过，0 失败）。
- 本次修复验证：`mvn -Dtest=HrmAttendanceDataControllerTest -DfailIfNoTests=false test`（2026-04-13，通过，10 通过，0 失败）。
- 由于仓库内存在与本次无关的历史测试编译错误（`SalaryMonthRecordServiceNewTest`），采用 JUnitCore 方式可单独验证 `WorkPlanServiceImplTest`（当前 4 个用例）通过。
- 接口侧排查结论（联调前）：
  - `removeAll` 正常；
  - `saveAll` 失败时会返回 `success=false, code=500`；
  - 现已补齐后端字段兜底与错误信息，便于继续联调验证。
- 本次修复验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`（2026-04-07，通过，5 通过，0 失败）。
- 本次修复验证：`mvn -DskipTests compile`（2026-04-07，通过，包含 `HrmOvertimeNightStatisticsServiceImpl` 与 `hrmEmployeeOverTimeRecordRepository` 变更编译校验）。
- 本次修复验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeleteExistingRowsByWorkDateRangeBeforeSave test`（2026-04-07，通过）。
- 本次修复验证：`mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest test`（2026-04-07，通过，6 通过，0 失败，包含唯一键冲突回归用例）。
- 本次修复验证：`mvn -Dtest=WorkPlanListControllerTest,WorkPlanCustomShiftResolverTest,WorkPlanServiceImplTest -DfailIfNoTests=false test`（2026-04-12，通过，12 通过，0 失败）。
- 本次修复验证：`mvn -Dtest=WorkPlanListControllerTest,HrmAttendanceDataControllerTest,WorkPlanCustomShiftResolverTest,WorkPlanServiceImplTest -DfailIfNoTests=false test`（2026-04-12，通过，24 通过，0 失败）。
- 本次修复验证：`mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest -DfailIfNoTests=false test`（2026-04-12，通过，20 通过，0 失败，包含自定义班次下拉接口）。

## Known Issues / Future Work
- 审批数据页当前依赖本地 `tbattendanceapprove` 快照表：
  - 若业务希望页面显示“同步考勤中的审批数据”，则必须先让同步考勤链路实际写入该表；
  - 现阶段主同步入口 `attendanceData/sync` / `syncAll` 并未执行审批同步，因此即使页面和查询接口正常，列表仍会为空；
  - 若后续确认允许在同步考勤链路中补接审批同步，需要单独评估钉钉调用约束、同步范围、分页/限流和重入更新策略。

- 登录链路依赖“总库账号映射 + 租户用户/部门/角色主数据”三处同时一致：
  - `hrsystem.tbAllUserList` 存在账号但租户库 `tbloginuser` 缺失时，会返回“账号不存在”；
  - `tbloginuser` 存在但部门或角色主数据缺失时，也会因 `view_loginuser` 内连接过滤而返回同样错误；
  - 若后续需要更易排查的报错，需要把“账号缺失”“部门缺失”“角色缺失”拆成显式诊断，而不是统一吞成“账号不存在”。
- 若 `classId` 对应班次本身无 `group_id` 配置，接口会继续失败并提示明确错误，需要数据配置侧修正班次归属考勤组。
- 排班提交任务状态当前仅保存在应用内存；若服务重启，已创建但未完成的任务进度无法继续查询。
- 自定义班次元数据当前仍保留 Redis 回填兼容逻辑，用于兼容历史“负数 classId + Redis 元数据”的旧排班；新数据优先读 `custom_shift_id`。
- 本地自定义班次去重目前依赖 `hrm_workplan_custom_shift(start1,end1,cross_day)` 精确匹配；若后续存在多实例并发创建场景，应依赖数据库唯一键兜底。

## Recent Changes and Decisions (2026-04-12, 添加排班自定义班次设计)
- 【已被 2026-04-12 最新业务变更替代】以下设计记录保留作历史决策参考，当前实现不再把自定义班次同步到钉钉。
- 需求范围确认：
  - “添加排班”在保留现有标准班次能力的同时，新增自定义班次输入；
  - 自定义班次只支持单段上下班时间，但允许跨天（`end <= start` 视为次日下班）；
  - 同一行允许多员工，且员工可以属于不同考勤组；
  - 用户侧错误文案必须使用中文业务语言，禁止出现英文错误与技术术语。
- 后端提交模型调整方向：
  - 现有 `WorkPlanListController#saveAll -> WorkPlanServiceImpl#AddAll/PostToServer` 的同步直传模式将改为“创建任务 + 异步执行 + 进度轮询”；
  - `/workPlan/saveAll` 仍保留原路径，但语义调整为“创建排班提交任务并返回 `taskId`”；
  - 新增排班进度查询接口，前端据此展示“正在第 X 次重试提交”。
- 钉钉链路设计确认：
  - `group/schedule/async` 本身只接受 `shiftId`，不能直接提交自定义开始/结束时间；
  - 因此自定义班次必须先解析成钉钉 `shiftId`：优先复用本地映射，其次匹配钉钉现有标准班次，再次才创建新班次；
  - 若员工所属考勤组尚未可用该班次，还需额外更新考勤组班次列表后再排班；
  - 不采用“通用考勤组统一承载自定义时间”的方案，避免破坏员工原有考勤规则。
- 缓存与持久化设计确认：
  - 提交排班继续复用现有 `getAllGroups/getUsers` 10 分钟 Redis 缓存；
  - 展示查询新增独立缓存键：`getAllGroups_display/getAllUsers_display`，TTL 为 30 分钟；
  - `attendanceData/getUsersByGroup` 不再直接调用钉钉成员列表接口，改为复用展示缓存后的全量人员并按 `groupId` 过滤；
  - 新增“自定义班次映射表”，按 `corpId + groupId + start + end + crossDay` 唯一定位钉钉 `shiftId`；
  - Redis 作为热缓存，本地表作为长效事实数据，系统生成班次不做自动清理。
- 失败与重试策略确认：
  - 重试放在后端，不由前端重复调用 `saveAll`；
  - 每个提交任务最多重试 `10` 次；
  - `10` 次后仍失败则整次失败；
  - 失败结果按“行 x 员工”维度返回，至少带行号、员工、考勤组、班次/时间段与中文错误原因。

## Recent Changes and Decisions (2026-04-09, sync/syncAll 入库链路梳理)
- `attendanceData/sync` 与 `attendanceData/syncAll` 统一进入 `HrmAttendanceDataServiceImpl#SyncDataWithResume`，当前固定分 7 步执行：组织架构、用户映射、考勤计划、打卡明细、报表字段、报表旧数据删除、报表数据回灌。
- 步骤 1 `AttendanceGroupManager#GetAndSave`：
  - 调 `topapi/attendance/getsimplegroups`，落 `hrm_attendance_group`、`hrm_attendance_group_relation_dept`、`hrm_attendance_group_relation_employee`；
  - 同时按考勤组下 `selectedClass` 调 `topapi/attendance/shift/query`，将班次时间明细落到 `hrm_attendance_shift`；
  - 当前同步链路不会把班次时间写入 `hrm_attendance_group.shift_setting`。
- 步骤 2 `AttendanceUserManager#GetAndSave`：
  - 调 `topapi/smartwork/hrm/employee/queryonjob`、`topapi/smartwork/hrm/employee/v2/list`，并补充离职员工接口，最终增量写入 `tbattendanceuser`；
  - `tbattendanceuser.groupId` 优先复用步骤 1 已生成的 `hrm_attendance_group_relation_employee`，缺失时才兜底调 `topapi/attendance/getusergroup`。
- 步骤 3 `AttendancePlanRecord#GetAndSaveWithoutDelete`：
  - 调 `topapi/attendance/listschedule`，按员工/日期写 `hrm_attendance_plan`，包含 `group_id/class_id/class_setting_id/plan_check_time/check_type`。
- 步骤 4 `AttendanceDetailRecord#GetAndSave`：
  - 调 `attendance/listRecord`；
  - 写两张表：`hrm_attendance_clock`（业务打卡明细）和 `tbattendancedetail`（接口原始明细镜像）；
  - 其中外层同步只会先删 `hrm_attendance_clock`，`tbattendancedetail` 当前按 record id 幂等补写，不会整段清空重灌。
- 步骤 5~7 `HrmAttendanceReportManager`：
  - 步骤 5 调 `topapi/attendance/getattcolumns`，重建 `hrm_attendance_report_field`；
  - 步骤 6 删除目标范围内 `hrm_attendance_report_data`；
  - 步骤 7a 调 `topapi/attendance/getcolumnval` 回写 `hrm_attendance_report_data` 的 type=1 日报字段；
  - 步骤 7b 调 `topapi/attendance/getleavetimebynames` 回写 `hrm_attendance_report_data` 的 type=2 请假字段。
- 当前已确认以下表不属于 `sync/syncAll` 直接入库结果：
  - `hrm_attendance_date_shift`：由 `HrmAttendanceGroupServiceImpl` 在考勤组配置/生效时生成；
  - `hrm_emp_schedule`：由 `AttendanceEmpScheduleTask` 另一路 `schedule/listbyusers` 定时任务写入，且该任务当前注释为“已禁用”；
  - `hrm_attendance_history_shift`：来自班次维护链路，不是考勤同步链路。

## Candidate Design (2026-04-09, 仅基于 sync 入库表的加班明细统计)
- 候选口径限定表：
  - `hrm_attendance_plan`
  - `hrm_attendance_shift`
  - `hrm_attendance_clock`
  - `tbattendancedetail`
  - `hrm_attendance_report_field`
  - `hrm_attendance_report_data`
  - `tbattendanceuser`
  - `hrm_attendance_group / hrm_attendance_group_relation_employee / hrm_attendance_group_relation_dept` 仅作员工与考勤组辅助信息，不参与核心加班时长换算。
- `2026-04-09` 已按业务确认落地临时方案：
  - 只使用 `attendanceData/sync` / `syncAll` 直接落库表中的 `hrm_attendance_plan / hrm_attendance_shift / hrm_attendance_clock / tbattendancedetail`；
  - 以 `hrm_attendance_plan.class_id -> hrm_attendance_shift` 还原计划下班时间；
  - 以 `hrm_attendance_clock` 为主、`tbattendancedetail` 为补充，归一成同一批上下班打卡；
  - 有效上班时间取“首次上班打卡”与“排班开始时间”两者中的较晚值，再与“最后下班打卡”计算实际工作跨度；
  - 当员工早于排班开始时间提前打卡时，提前部分不计入工作时长；
  - 上述实际工作跨度超过 `8` 小时时，将超出的部分记为当天加班小时；
  - 该实现明确不再读取 `hrm_attendance_report_data`、`hrm_attendance_group.shift_setting`、`hrm_attendance_history_shift`、`hrm_attendance_date_shift`、`hrm_employee_over_time_record` 参与当前加班小时换算。

## Recent Changes and Decisions (2026-03-27, 多租户打卡缺失专项)
- 现场核验（租户：admin -> `CompanyID=0001`）确认：`hr_0001.hrm_attendance_clock` 在 2025-11 存在大量真实数据（`7426` 条，`142` 名员工）。
- 根因确认：`HrmAttendanceClockServiceImpl#queryAttendanceEmpDetailByDate` 里用 `hrm_employee.create_time` 参与历史日期拦截，导致迁移/回填后 `create_time` 晚于查询月份时，整月被判空。
  - SQL 证据：2025-11 有打卡的 `142` 名员工中，`139` 名员工 `create_time > 2025-11-30`，会被旧逻辑误拦截。
- 修复：在 `queryAttendanceEmpDetailByDate` 中移除 `createTime` 过滤，仅保留“未来日期”与“入职日期”限制。
- 兼容性：保留此前“单端打卡缺失补 `-3`”逻辑，不影响正常/异常状态显示策略。

## Testing (2026-03-27)
- 已执行：`mvn -DskipTests compile`。
- 结果：`BUILD SUCCESS`。
- 运行态说明：本机 9080 为 IDE 常驻进程，可能未加载最新 class；建议重启该进程后再做页面回归。
- 进一步修复打卡概况“大面积无数据/超时”问题（2026-03-27）：
  - 原因1：月度概况查询链路使用 `parallelStream + ArrayList` 并发写，存在数据竞态（行丢失/混乱风险）。
  - 原因2：月度概况固定按 `clock_stage=1` 查询，导致 `clock_stage` 多阶段数据（如 2~14）被误过滤。
  - 原因3：概况接口按“员工*天”回查排班与状态，查询量过大导致接口容易超时。
  - 处理：
    - `queryAttendanceEmpMonthDetailPageList` 改为串行流处理；
    - `queryAttendanceClockList` SQL 改为 `clockStage!=null` 才追加 stage 条件；月度概况调用传 `null`；
    - `queryAttendanceEmpDetailByDate` 改为优先使用当月已拉取的原始打卡记录生成 `time`，移除该接口内高频排班回查。
- 验证（本地 9083 实例）：
  - `queryAttendanceEmpMonthDailyDetailPageList`（2025-11，limit=100）返回成功，`nonEmptyEmpCount=85`，平均非空天数 `17.54`；
  - 员工 `许林彦` 返回 `nonEmpty=23`，示例时间：`07:55-0 / 12:14-0`。

## Recent Changes and Decisions (2026-04-04, 考勤同步漏人专项)
- 根因定位：`AttendancePlanServiceImpl#Sync` 在每轮同步后调用 `AttendancePlanRecord#DeleteRepeatUser`，会按“同名且本周期无排班”删除 `tbattendanceuser`；该逻辑会误删跨部门同名员工映射，导致后续步骤无法拉取考勤。
- 修复1：移除 `AttendancePlanServiceImpl#Sync` 中对 `DeleteRepeatUser` 的调用，禁止按姓名做破坏性清理。
- 修复2：`HrmAttendanceDataServiceImpl` 在步骤 2 后加载 `tbattendanceuser` 时增加 `normalizeUsers`，按 `userId` 去重并优先保留有效 `empId` / 新记录，避免历史脏数据导致映射错配。
- 修复3：`AttendanceUserManager` 同步用户时改为“按 `userId` 去重 + 映射刷新”：
  - 预加载现有映射按 `userId` 选优；
  - 对已有 `userId` 不再盲目复用，按手机号/姓名重新匹配并更新 `empId/depId/groupId`；
  - 无法匹配时使用历史 `empId` 映射兜底，避免本轮任务把员工丢失；
  - 持久化前按 `userId` 二次去重，减少重复记录。
- `2026-05-15` 新增 `tbattendanceuser` 失效映射清理：
  - 现场库 `hr_0003.tbattendanceuser` 查到 5 组重复员工映射，共 10 条记录，`empId` 分别为：
    - `1831601326890434567`
    - `1831601326890434620`
    - `1831601326890434621`
    - `1831601326890434638`
    - `2033769831940079640`
  - 逐条用钉钉 `topapi/v2/user/get` 只读核验后确认：
    - `010013312008850281`
    - `01632416074432231286`
    - `193050226626273737`
    - `02070142381723237944`
    - `133930484266042`
    均已返回 `errcode=60121 / 找不到该用户`；
    - 同组对应的现行有效 `userId` 分别为：
      - `02642841571826358761`
      - `01632416216432231286`
      - `01650566501326273737`
      - `01056325626423237944`
    - 第 5 组员工 `邓秀强` 当前两条历史 `userId` 都已失效，本轮按用户要求仅清理其中 1 条 `id=210`，保留另一条待后续业务确认。
  - 代码修复：
    - `HrmEmployee` 补充 `dingtalk_user_id` 字段映射；
    - `AttendanceUserManager#GetAndSave()` 同步结束后会精确删除两类失效映射：
      - 同一在职员工已存在明确现行 `dingtalk_user_id` 或本次同步回写出的有效 `userId`，则删除该员工其他旧 `userId`；
      - 本次钉钉详情接口未再返回、且员工仍在职的孤儿 `userId`，在确认不属于本次远端可见列表后自动清理。
    - 删除动作不再依赖“同名 + 无排班”等脆弱规则，完全基于 `employeeId -> userId` 与钉钉当前可见性判断。
- 本轮提供的现场精确清理脚本：
  - `docs/sql/2026-05-15_hr_0003_tbattendanceuser_cleanup.sql`
  - 当前只删除 `id in (207,208,205,206,210)` 这 5 条，避免超出用户本轮授权范围。
- 验证：`mvn -DskipTests compile`（2026-04-04）通过。

## Recent Changes and Decisions (2026-04-04, 个税与附加列表排序/展示)
- `HrmPersonalIncomeTaxMapper.xml#queryRemainingVacationList` 增加排序：`year desc, end_month desc, personal_income_tax_id desc`，保证个税累计分页结果按年月倒序。
- `HrmAdditionalMapper.xml#queryAdditionalList` 增加排序：`year desc, month desc, additional_id desc`，保证附加累计分页结果按年月倒序。
- `HrmEmployeeAdditionalMapper.xml#queryEmployeeAdditionalList` 增加排序：`year desc, employee_additional_id desc`，保证年度附加扣除按年份倒序。
- 前端列表同步增强：
  - `Tax.vue` 与 `Addition.vue` 新增“年月”列（`YYYY-MM`）；
  - 三个列表前端增加兜底排序（即使后端旧版本未发布，也按要求展示顺序）。
- 验证：
  - `npm run build`（`hr_web`）通过；
  - `mvn -DskipTests compile`（`hainan`）通过。

## Recent Changes and Decisions (2026-04-04, 加班/夜班统计一期)
- 新增后端接口：
  - `HrmOvertimeNightStatisticsController#queryPageList`
  - `HrmOvertimeNightStatisticsController#startStatistics`
  - `HrmOvertimeNightStatisticsController#queryEmployeeMonthlyDetail`
  - `HrmOvertimeNightStatisticsController#exportStatistics`
- 新增服务：`HrmOvertimeNightStatisticsServiceImpl`
  - 列表接口按全员维度分页，未产生统计数据的员工也返回，统计值默认为 `0`；
  - “开始统计”当前实现为实时计算指定月份，不额外落库统计表；
  - 员工明细接口按员工排班月份倒序返回加班小时与夜班次数，并在每个月下附带按日期倒序排列的每日明细。
- 新增导出支持：`OvertimeNightStatisticsExportSupport`
  - 导出文件名统一为 `加班夜班统计_YYYY-MM.xlsx`；
  - Excel 导出包含 `汇总`、`每日明细` 两个 sheet；
  - 导出直接复用实时计算结果，不新增统计落库表。
- 修复“导出 Excel 全员加班/夜班为 0”问题：
  - 根因：统计逻辑强依赖 `hrm_attendance_shift` 按 `plan.classId` 反查班次结束时间；当本地班次表未同步齐或 `classId` 无法命中时，该工作日会被直接跳过。
  - 处理：新增 `OvertimeNightClockResolver`，优先使用下班打卡记录里的 `attendanceTime` 作为计划下班时间、`clockTime` 作为实际下班时间；仅在缺失时才回退班次表。
  - 结果：跨天班次、未完全同步的本地班次、以及多段班次的最后下班时间都能被正常纳入统计。
- 当前算法假设：
  - 统计只使用 `hrm_attendance_plan`、`hrm_attendance_shift`、`hrm_attendance_clock`；
  - 加班小时与夜班次数为两套独立规则；
  - 加班小时 = 当日最后一次下班打卡时间晚于排班最后下班时间的超出时长，无起算门槛；
  - 夜班次数 = 排班跨天且存在次日下班打卡，且次日下班时间达到凌晨 `03:00` 及以后时计 1 次。
- 验证：
  - `mvn -DskipTests compile`（`hainan`）通过；
  - 当前按用户要求未保留本功能新增测试文件，后续如需恢复自动化覆盖需重新补建测试集。

## Recent Changes and Decisions (2026-04-05, 加班/夜班统计落库设计)
- 需求调整：加班/夜班统计不再仅依赖实时计算与 Excel 导出，新增“统计结果落库”设计，便于列表查询、月度明细查询与后续重复导出复用。
- 当前更贴近业务页面的设计改为优先采用一张明细表：
  - `hrm_overtime_night_statistics_detail`：员工每日明细；
  - 员工月汇总、导出汇总、查看页月度总计统一基于明细表实时聚合。
- 改为单表的原因：
  - 当前列表页只加载全员并进入“查看”页，不直接展示月汇总列；
  - 查看页核心是“按月看每天的加班/夜班明细”，明细表才是主数据；
  - 若新增独立汇总表，后续重算时还需维护双写一致性，复杂度更高。
- 当前建议的数据唯一性约束：
  - 明细表按 `employee_id + work_date` 唯一；
  - 默认按“同员工同工作日覆盖最新统计结果”处理，不保留历史版本。
- 字段设计原则：
  - 加班时长使用 `decimal(10,2)`；
  - 夜班次数使用 `int`；
  - 计划下班、实际下班保留 `datetime`，便于后续追溯统计依据；
  - 保留 `class_id/plan_id` 作为排班来源关联键，便于排查。

## Recent Changes and Decisions (2026-04-05, 加班/夜班统计落库实现)
- 新增持久化模型与仓库：
  - `HrmOvertimeNightStatisticsDetail`
  - `hrmOvertimeNightStatisticsDetailRepository`
- `HrmOvertimeNightStatisticsServiceImpl` 调整为“写库 + 读库”模式：
  - `startStatistics`：按所选月份删除旧明细、基于排班与打卡重算、重新写入 `hrm_overtime_night_statistics_detail`；
  - `queryPageList`：对当前页员工按月从明细表聚合加班小时与夜班次数；
  - `queryEmployeeMonthlyDetail`：按员工从明细表读取后按月份分组，并返回每日明细；
  - `exportStatistics`：保留，但改为从明细表读取导出数据，不再依赖实时计算。

## Recent Changes and Decisions (2026-05-08, 员工月度明细按月份过滤)
- `QueryEmployeeOvertimeNightDetailBO`
  - 新增 `month` 字段，格式为 `YYYY-MM`。
- `hrmOvertimeNightStatisticsDetailRepository`
  - 新增 `findAllByEmployeeIdAndStatYearAndStatMonthOrderByWorkDateDesc(...)`。
- `HrmOvertimeNightStatisticsServiceImpl#queryEmployeeMonthlyDetail`
  - 当前端未传 `month` 时，保持既有“返回该员工全部月份并按月分组”的兼容行为；
  - 当前端传入 `month` 时，解析为 `YearMonth` 后仅查询该员工该年月明细；
  - 这样可支撑前端月度统计明细弹窗使用真正的月份选择器主动切月，而不是受限于首次返回的月份列表。
- 新增回归测试：
  - `HrmOvertimeNightStatisticsServiceImplTest#queryEmployeeMonthlyDetail_shouldFilterBySelectedMonth`
  - 验证传入 `month=2026-03` 时，仅返回 `2026-03` 的明细，并优先命中按员工+年月的仓库查询。

## Recent Changes and Decisions (2026-04-09, sync 历史班次缺失根因确认)
- 入口 `HrmAttendanceDataServiceImpl#SyncDataWithResume` 的步骤 1 会调用 `AttendanceGroupManager#GetAndSave` 同步考勤组。
- `AttendanceGroupManager#GetAndSave` 的当前实现特征：
  - 开始即执行 `groupRep.deleteAll()`、`depRelRep.deleteAll()`、`empRelRep.deleteAll()`，再从钉钉 `topapi/attendance/getsimplegroups` 拉取当前考勤组快照重建；
  - 保存 `hrm_attendance_group` 时原先只写 `attendanceGroupId/name/isDefault/effectTime` 等基础字段，未写 `shift_setting`、`old_group_id`；
  - 对每个组调用 `AddAttendanceShift` 时，会先执行 `shiftRep.deleteAllByGroupId(groupId)`，再仅按当前 `selectedClass` 重建 `hrm_attendance_shift`。
- `AttendancePlanRecord#GetAndSave` 只会把日排班接口返回的 `planId/checkType/planCheckTime/classId/classSettingId/groupId/workDate` 写入 `hrm_attendance_plan`，不保存当日完整班次时间快照。
- 结论：
  - `attendanceData/sync` 并不保存“历史月份曾经生效的考勤组班次定义”；
  - 若钉钉在后续月份调整过考勤组或班次，再跑一次 `sync` 只会覆盖为当前配置，无法补回上个月的真实班次时间；
  - 这也是 `2026-03-20` 这类数据在本地只能看到 `hrm_attendance_plan.plan_check_time=07:00/15:00`，却无法从 `hrm_attendance_group.shift_setting` 或 `hrm_attendance_shift` 还原出业务口径 `08:00-17:30` 的根因。

## Recent Changes and Decisions (2026-04-09, sync 最新考勤组班次补齐)
- 按业务最新口径，不再要求 `sync` 保留历史月份班次版本，只要求本地始终与钉钉“当前最新”考勤组和班次时间保持一致。
- `AttendanceGroupManager` 已调整：
  - 新增组快照构建逻辑，将钉钉 `selectedClass.classId` 按返回顺序拼成逗号串，写入 `hrm_attendance_group.shift_setting`；
  - `AddAttendanceShift` 保留“先删后写当前班次”的最新快照策略，但现在即使 `selectedClass` 为空也不会抛空指针；
  - 班次同步从部门关系判断中拆出，避免因 `deptNameList` 为空直接 `continue`，导致“考勤组同步了但最新班次时间没同步”。
- 验证覆盖：
  - 新增 `AttendanceGroupManagerTest`，覆盖 `buildShiftSetting` 的顺序拼接行为；
  - 覆盖组实体构建时会带上当前最新 `shiftSetting` 的行为。
- 统计明细落库策略：
  - 以“员工 + 工作日”为粒度写入；
  - 即使当天无加班/夜班，也保留该排班日的 0 值明细，便于查看页按天展示；
  - 重新统计同月数据时，先删当月旧明细再整月重写，避免残留脏数据。
- 验证：
  - `mvn -DskipTests compile`（`hainan`）通过；
  - 由于仓库内存在与本次无关的历史测试编译错误，未使用 `mvn test` 全量验证；
  - 单独编译并运行 `HrmOvertimeNightStatisticsServiceImplTest` 通过，覆盖“列表读库聚合”“员工月度明细读库分组”两项核心行为。

## Recent Changes and Decisions (2026-04-05, 加班/夜班明细总览页)
- 新增后端接口：
  - `HrmOvertimeNightStatisticsController#queryDailyDetailPageList`
- 新增查询对象：
  - `QueryOvertimeNightDailyDetailPageBO`
  - `QueryOvertimeNightDailyDetailPageVO`
- 新接口实现方式：
  - 仅从 `hrm_overtime_night_statistics_detail` 读取；
  - 按 `stat_year + stat_month` 查询指定月份明细；
  - 支持按 `employeeName/jobNumber` 关键字过滤；
  - 返回分页后的员工-日期粒度明细行，用于前端“显示所有”新标签页。
- 现有员工月度明细接口保持不变，继续返回“按月汇总 + 每日明细”结构，供弹窗展示。
- 验证：
  - 单独编译并运行 `HrmOvertimeNightStatisticsServiceImplTest` 通过，当前共 4 个用例，新增覆盖“每日明细总览分页”和“关键字筛选”。

## Recent Changes and Decisions (2026-04-07, 加班统计兼容既有加班记录)
- 现场问题：
  - 租户 `hr_0003` 中员工 `许泽刚` 在 `2026-03-05 / 03-06 / 03-10 / 03-15 / 03-16 / 03-20` 已有加班小时，但加班夜班统计未完整体现。
- 根因判断：
  - 新统计模块只按“排班 + 下班打卡差值”重算加班小时；
  - 系统原有 `hrm_employee_over_time_record` 会在打卡处理时按既有规则落库加班小时，新统计模块未复用该结果，导致“库里已有加班记录，但统计明细仍为 0 或偏小”。
- 本次修复：
  - 为 `hrmEmployeeOverTimeRecordRepository` 补充按员工与日期区间查询的方法；
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails` 在重算当月时额外读取 `hrm_employee_over_time_record`；
  - 同一员工同一工作日若存在已落库加班记录，则以该日加班记录汇总小时数回填 `overtimeHours`；
  - 若原始打卡时间缺失，则同步用加班记录的开始/结束时间补齐统计明细中的计划/实际下班时间；
  - 夜班次数仍继续只按排班与打卡规则计算，不受加班记录回填逻辑影响。
- 兼容性：
  - 没有既有加班记录的工作日，仍沿用原先“排班结束时间 vs 实际下班打卡时间”的计算方式；
  - 本次只补充加班小时兜底，不改接口路径、分页结构和夜班口径。

## Recent Changes and Decisions (2026-04-07, 加班统计重算唯一键冲突)
- 现场报错：
  - `Duplicate entry '1831601326890434563-2026-03-27' for key 'hrm_overtime_night_statistics_detail.uk_employee_work_date'`。
- 根因判断：
  - 统计明细表唯一约束为 `employee_id + work_date`；
  - `startStatistics` 重算前原先按 `stat_year + stat_month` 删除旧数据；
  - 当库里存在“`work_date` 落在目标月份，但 `stat_year/stat_month` 历史值异常或未同步”的旧行时，旧删除条件无法清掉这些行，重算写入会直接撞唯一键。
- 本次修复：
  - `hrmOvertimeNightStatisticsDetailRepository` 新增按 `work_date` 区间删除的方法；
  - `HrmOvertimeNightStatisticsServiceImpl#startStatistics` 改为按目标月份 `work_date` 整月范围清理旧明细，再执行重算写入；
  - 删除口径现在与唯一键一致，不再依赖辅助统计字段是否准确。
- 回归覆盖：
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldDeleteExistingRowsByWorkDateRangeBeforeSave` 新增验证，防止后续再回退到 `stat_year/stat_month` 删除方式。

## Recent Changes and Decisions (2026-04-09, 加班统计临时 sync 口径收口)
- `HrmOvertimeNightStatisticsServiceImpl` 改为只走 `sync/syncAll` 直接落库链路：
  - 活跃数据源收敛为 `hrm_attendance_plan / hrm_attendance_shift / hrm_attendance_clock / tbattendancedetail`；
  - `resolveShift` 当前只查 `hrm_attendance_shift`，不再沿 `history_shift/date_shift/group` 做运行期兜底。
- 加班小时计算规则当前固定为一段：
  - 把 `hrm_attendance_clock` 与 `tbattendancedetail` 归一为 `PunchRecord`；
  - 有效上班时间取“首次上班打卡”和“排班开始时间”两者中的较晚值；
  - 再按“有效上班时间 -> 最后下班打卡”的实际跨度计算，超过 `8` 小时的部分记为当天加班小时。
- 为避免 `clock` 与 `tbattendancedetail` 同时存在时重复计时，内部按 `type + actualTime + scheduledTime` 去重后再参与计算。
- 调试日志仍保留到业务现场回归结束：
  - 文件：`logs/hainan-overtime-debug.log`
  - 重点字段：`workDate / scheduledEndSource / actualOffTime / actualWorkMinutes / overtimeHours`
  - 控制台当前只额外输出 `许泽刚 2026-03-20` 的中文说明，包含排班开始/结束、首次上班打卡、计入工时的开始时间、最后下班打卡以及逐步计算过程。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` 通过（12 通过，0 失败）；
  - `mvn -DskipTests compile` 待本次收尾后再次确认。

## Recent Changes and Decisions (2026-04-06, 每周单双休生成功能)
- 新增独立模块：`modules/workweek`
  - `controller/HrmWorkweekSettingController`
  - `service/HrmWorkweekSettingService`
  - `entity/HrmWorkweekSetting`
  - `bo/*`、`vo/*`
- 新增仓库：`hrmWorkweekSettingRepository`
  - 按 `settingYear` 读取全年周配置，避免每次查询时重复推算。
- 新增接口：
  - `POST /hrmWorkweekSetting/queryYearSettings`
  - `POST /hrmWorkweekSetting/initYearSettings`
  - `POST /hrmWorkweekSetting/updateWeekType`
- 生成规则实现：
  - 使用自然周，周一至周日；
  - 包含 `1 月 1 日` 的周记为第 `1` 周；
  - 单休固定映射为 `周日`，双休固定映射为 `周六、周日`；
  - 初始化时从第一周类型开始按“单休/双休”交替生成，直到包含 `12 月 31 日` 的最后一周。
- 修改联动规则实现：
  - 用户修改第 `N` 周后，仅从第 `N` 周开始向后重算到年末；
  - `1..N-1` 周保持原值；
  - 被手动修改的周标记 `manualOverride=1`，其后重算周重置为 `0`。
- 持久化设计：
  - 新表：`hrm_workweek_setting`
  - 字段：`setting_id/setting_year/week_no/week_type/week_start_date/week_end_date/rest_day_text/manual_override/create_time/update_time`
  - 建表脚本：`docs/sql/2026-04-06_hrm_workweek_setting.sql`
  - 多租户开发库建表脚本：`docs/sql/2026-04-06_hrm_workweek_setting_hr_0001_to_hr_0005.sql`
  - 执行口径：使用 `application-dev.properties` 中的开发库地址，对 `hr_0001` ~ `hr_0005` 分别执行 `CREATE TABLE IF NOT EXISTS`。

## Testing (2026-04-06, 每周单双休生成功能)
- 已执行：`mvn -DskipTests compile`
- 结果：`BUILD SUCCESS`
- 受仓库内历史无关测试 `SalaryMonthRecordServiceNewTest` 编译失败影响，未使用 `mvn test` 做全量验证。
- 已执行隔离验证：
  - `java -cp <target/test-classes + target/classes + junit/mockito/spring/hutool jars> org.junit.runner.JUnitCore com.tianye.hrsystem.modules.workweek.service.HrmWorkweekSettingServiceTest`
- 结果：
  - `OK (3 tests)`
  - 覆盖“首次全年生成”“读取已存在记录”“修改某周后向后重算”三项核心行为。

## Recent Changes and Decisions (2026-04-07, 后端专项测试链路恢复)
- 根因定位：
  - `SalaryMonthRecordServiceNew` 的半路转正税算方法新增了 `welfareTaxableIncome` 参数；
  - `SalaryMonthRecordServiceNewTest` 仍按旧签名调用，导致 `testCompile` 阶段直接失败，进而阻断 `HrmWorkweekSettingServiceTest`、`HrmOvertimeNightStatisticsServiceImplTest` 等新模块的标准 Maven 验证。
- 处理：
  - 对齐 `SalaryMonthRecordServiceNewTest` 中 `calculateMidMonthPromotionSummary` 与 `recalculateMidMonthPromotionTaxAndPay` 的调用签名；
  - 新增福利计税收入回归用例，覆盖 `welfareTaxableIncome` 会参与累计应纳税所得额计算的口径。
- 结果：
  - 仓库已可重新使用标准 `mvn -Dtest=... test` 跑后端专项测试，无需再依赖手工 `JUnitCore` 绕过历史测试编译问题。
- 验证：
  - `mvn -Dtest=SalaryMonthRecordServiceNewTest -DfailIfNoTests=false test` 通过（18 通过，0 失败）；
  - `mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false test` 通过；
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test` 通过（4 通过，0 失败）。

## Recent Changes and Decisions (2026-04-07, 同步考勤调试遗留清理)
- `HrmAttendanceDataServiceImpl#SyncDataWithResume` 中存在“强制从步骤7开始”的临时调试注释，但实际代码值为 `startStep = 1`，容易误导后续联调与排查。
- 已移除该临时调试注释，保留当前真实行为：默认从步骤 1 开始，仅在检测到有效断点缓存时按缓存步骤恢复。
- 本次为代码可读性与联调可维护性清理，不改变同步业务逻辑。

## Recent Changes and Decisions (2026-04-07, 加班统计排班解析增强 + 单人统计 + 调试日志)

### 问题背景
- 租户 `hr_0003` 员工许泽刚3月份加班统计始终为 0，但人工统计有 9.5 小时加班。
- 排查发现三层根因：
  1. `OvertimeNightClockResolver.resolveScheduledEndTime()` 使用打卡记录的 `attendanceTime` 作为"计划下班时间"，但下班打卡的 `attendanceTime == clockTime`，导致加班 = 0；
  2. 68.9% 的 `hrm_attendance_plan` 记录 `class_id` 为 NULL，无法通过 `classId` 查找班次信息；
  3. 考勤组 `shiftSetting` 为空（客户在3月18日重新配置了考勤组）。

### 修复内容

**1. 班次结束时间解析优先级调整（`OvertimeNightClockResolver.java`）**
- `resolveScheduledEndTime()` 方法优先使用 `HrmAttendanceShift` 的班次结束时间；
- 仅在无班次信息时回退到打卡记录的 `attendanceTime`。

**2. 班次查找回退机制（`HrmOvertimeNightStatisticsServiceImpl.java`）**
- 新增 `resolveShiftByGroupAndDate()` 方法，当 `classId` 为 NULL 时：
  - 通过 `plan.groupId` 查找考勤组 `HrmAttendanceGroup`；
  - 从考勤组 `shiftSetting`（逗号分隔的班次ID列表）按星期几映射获取对应班次；
  - 星期映射逻辑与 `HrmAttendanceClockServiceImpl.getHrmAttendanceShiftByGroup` 保持一致。
- 新增 `hrmAttendanceGroupRepository` 注入，引入考勤组缓存 `Map<Long, HrmAttendanceGroup> groupCache`。
- `calculateMonthlyDetails` 方法签名新增 `groupCache` 参数。
- 从排班计划中提取 `fallbackGroupId`，在单条计划 `groupId` 为空时使用。

**3. 调试日志（`HrmOvertimeNightStatisticsServiceImpl.java`）**
- 新增 `Logger` 字段和 `firstNotBlank()` 辅助方法。
- 在 `calculateMonthlyDetails` 每日循环中，当员工姓名为"许泽刚"时，输出详细日志：
  - 关键字：`[加班统计-许泽刚]`
  - 内容：`workDate, classId, groupId, fallbackGroupId, shiftId, shiftTimes, scheduledEndTime, actualOffTime, overtimeHours, nightShiftCount, offDutyClocksCount`

**4. 单人统计功能（新增端到端链路）**
- 后端改动：
  - `QueryOvertimeNightStatisticsPageBO`：新增 `employeeId` 字段。
  - `IHrmOvertimeNightStatisticsService`：新增 `startStatisticsForEmployee()` 接口方法。
  - `HrmOvertimeNightStatisticsServiceImpl`：实现 `startStatisticsForEmployee()`：
    - 按 `employeeId + workDate` 区间仅删除该员工该月的统计数据；
    - 调用 `calculateMonthlyDetails` 重算并写入；
    - 返回该员工的月度统计明细。
  - `hrmOvertimeNightStatisticsDetailRepository`：新增 `deleteAllByEmployeeIdAndWorkDateBetween()` 方法。
  - `HrmOvertimeNightStatisticsController`：新增 `/startStatisticsForEmployee` 端点。

### 修改文件清单
| 文件 | 改动 |
|------|------|
| `imple/OvertimeNightClockResolver.java` | `resolveScheduledEndTime` 优先使用班次时间 |
| `imple/HrmOvertimeNightStatisticsServiceImpl.java` | Logger + 调试日志 + `resolveShiftByGroupAndDate` + `startStatisticsForEmployee` + `firstNotBlank` |
| `entity/bo/QueryOvertimeNightStatisticsPageBO.java` | 新增 `employeeId` 字段 |
| `service/IHrmOvertimeNightStatisticsService.java` | 新增 `startStatisticsForEmployee` 接口 |
| `controller/HrmOvertimeNightStatisticsController.java` | 新增 `/startStatisticsForEmployee` 端点 |
| `repository/hrmOvertimeNightStatisticsDetailRepository.java` | 新增 `deleteAllByEmployeeIdAndWorkDateBetween` |

### 待验证
- 部署后执行许泽刚3月份单人统计，观察 `[加班统计-许泽刚]` 日志中3月18-24日的 `shift`、`scheduledEndTime`、`actualOffTime` 是否正确。
- 如果考勤组 `shiftSetting` 仍为空且 `classId` 仍为 NULL，日志将输出 `shift=null, shiftTimes=[null-null]`，说明需要进一步排查3月18日之后的考勤组配置数据。

## Recent Changes and Decisions (2026-04-08, 加班统计 OffDuty 排班兜底)
- 对照 `../hr_web/docs/hainan-overtime-fix-staging.md` 锚点 `hainan-overtime-plan-checktime-fallback` 完成后端补齐。
- 根因确认：
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails` 之前按天只保留第一条排班，遇到同一天同时存在 `OnDuty` / `OffDuty` 时，常会误选 `OnDuty`；
  - 下班卡之前按 `workDate` 分组，现场存在 `workDate` 脏值时，会把本应属于当天的下班卡归到次日；
  - 班次主数据缺失时，计划下班时间直接回退到下班卡 `attendanceTime`，而现场 `attendanceTime == clockTime`，导致加班时长被算成 `0`。
- 本次调整：
  - `calculateMonthlyDetails` 的按天结构从单条计划改为当天计划列表；
  - 每天优先选择 `check_type=OffDuty` 且 `planCheckTime` 最大的排班；若当天没有 `OffDuty`，再回退到 `planCheckTime` 最大的排班；
  - 统计明细里的 `planId/classId`、班次解析入口、调试日志上下文统一改为基于这条“最终 OffDuty 排班”；
  - 下班卡按业务日期分组时改为优先 `attendanceTime`，为空再回退 `workDate`；
  - `OvertimeNightClockResolver#resolveScheduledEndTime` 优先级改为：班次结束时间 > 最终 `OffDuty planCheckTime` > 下班卡 `attendanceTime`。
- 回归覆盖：
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldUseLastOffDutyPlanCheckTimeWhenShiftMissing`
  - 场景：班次主数据缺失、同一天存在 `OnDuty 07:00` 与 `OffDuty 15:00` 两条排班、下班卡 `attendanceTime=clockTime=17:30` 且 `workDate` 为次日；
  - 期望：仍按 `2026-03-18 15:00 -> 17:30` 计算出 `2.50` 小时，并把统计明细关联到 `OffDuty` 那条排班。

## Recent Changes and Decisions (2026-04-08, 加班统计 OffDuty 空计划时间择优)
- 继续排查“修复后现场仍为 0”时，补出第二个可复现根因：
  - `selectPrimaryPlan()` 之前使用 `Comparator.nullsLast(...).max(...)`；
  - 当同一天存在多条 `OffDuty`，且其中一条 `planCheckTime=null` 时，空时间记录会被当成“更大”的那条选中；
  - 后续 `resolveScheduledEndTime()` 因拿不到有效 `planCheckTime`，又退回到下班卡 `attendanceTime`，导致加班重新被算成 `0`。
- 本次修正：
  - `selectPrimaryPlan()` 改为优先选择 `planCheckTime` 非空的排班，再按时间、创建时间、主键择优；
  - 针对 `许泽刚` 的调试日志增加 `selectedPlanId/selectedCheckType/selectedPlanCheckTime`，便于部署后直接确认到底选中了哪条排班。
- 回归覆盖：
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldPreferNonNullOffDutyPlanCheckTimeWhenDuplicateOffDutyPlansExist`
  - 场景：同一天存在两条 `OffDuty`，其中一条 `planCheckTime=null`、另一条为 `15:00`；
  - 期望：必须选中 `15:00` 那条 `OffDuty`，最终计算 `2.50` 小时，而不是再次落回 `0.00`。

## Recent Changes and Decisions (2026-04-08, 单人统计返回值去旧值)
- 继续排查“单人统计仍显示 0”时，确认单人接口还有一条独立数据路径问题：
  - `startStatisticsForEmployee()` 内部虽然已重算出正确的 `detailRows`；
  - 但旧实现随后立即调用 `queryEmployeeMonthlyDetail()` 再查仓库；
  - 若当前事务内仓库查询仍拿到旧的当月明细，前端弹窗会继续展示历史 `0.00`，看起来像“单人统计没有生效”。
- 本次修正：
  - `startStatisticsForEmployee()` 在保存后显式 `flush()`；
  - 返回值组装改为“当前月份优先使用本次刚重算的 `detailRows`，其他月份再补仓库已有明细”，避免当前月即时响应被旧值覆盖。
- 回归覆盖：
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldReturnFreshCurrentMonthDetailsInsteadOfStalePersistedRows`
  - 场景：仓库查询仍返回当前月旧的 `0.00` 明细，但本次重算结果为 `2.50`；
  - 期望：单人统计接口返回的当前月汇总必须是 `2.50`，不能再回退成旧值。

## Recent Changes and Decisions (2026-04-08, 按 Sheet3 收敛 fallback 加班口径)
- 新现场证据：
  - 对照桌面 `/Users/jiangyongming/Desktop/无标题.xls` 的 `Sheet3`，“许泽刚-加班”在 `2026-03-18` 到 `2026-03-24` 之间只有 `2026-03-20` 为 `1.5`；
  - 这说明此前按 `OffDuty planCheckTime=15:00` 直接推导 `17:30` 为加班的 staging 假设不成立。
- 口径调整：
  - `OvertimeNightClockResolver` 继续输出计划下班时间来源：`SHIFT / PLAN_CHECK_TIME / CLOCK_ATTENDANCE / NONE`；
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails` 仅在来源为 `SHIFT` 时，才按“实际下班 - 计划下班”自动生成工作日加班；
  - 若只有 `PLAN_CHECK_TIME` 或 `CLOCK_ATTENDANCE` 回退来源，则默认不自动产出加班，除非该工作日已存在 `hrm_employee_over_time_record` 加班记录。
- 这样处理后的效果：
  - `3/18、3/19、3/21、3/22、3/23、3/24` 不会再因为 fallback `15:00 -> 17:30` 被误算成加班；
  - `3/20` 这类已有真实班次或既有加班记录的日期仍保留正确加班小时。
- 回归覆盖：
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldKeepZeroWhenOnlyFallbackPlanCheckTimeExistsWithoutOvertimeRecord`
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatistics_shouldPreferNonNullOffDutyPlanCheckTimeWhenDuplicateOffDutyPlansExist`

## Recent Changes and Decisions (2026-04-08, 既有加班记录 attendanceTime 为空时仍需回填)
- 新可复现根因：
  - 单人统计会先删该员工该月旧明细，再按排班/打卡/既有加班记录重算；
  - 原实现读取 `hrm_employee_over_time_record` 时同时存在两处对 `attendance_time` 的硬依赖：
    - 仓库查询方法只按 `attendance_time between begin/end` 取数；
    - `buildOvertimeRecordMap()` 仅当 `attendanceTime != null` 才把记录归到某个工作日。
  - 如果历史加班记录 `attendance_time` 为空，但 `over_time_start_time / over_time_end_time / over_times` 有值，则重算时这些记录会被整体忽略；
  - 结果是：旧明细先被删掉，新的回填又拿不到，现场就表现成“3 月这个员工又都变成 0 了”。
- 本次修正：
  - `hrmEmployeeOverTimeRecordRepository#findAllByEmployeeIdAndAttendanceTimeBetween` 改为自定义 JPQL：
    - 优先按 `attendance_time` 命中；
    - 对 `attendance_time is null` 的历史记录，补充按 `over_time_start_time / over_time_end_time` 的区间重叠查询。
  - `HrmOvertimeNightStatisticsServiceImpl#buildOvertimeRecordMap` 改为：
    - `attendanceTime` 有值时继续按它归组；
    - 否则回退 `overTimeStartTime`，再回退 `overTimeEndTime`，确保历史记录仍能归到正确工作日。
- 回归覆盖：
  - `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldUseOvertimeRecordWhenAttendanceTimeIsMissing`
  - 场景：`attendanceTime=null`，但 `over_time_start_time / over_time_end_time / over_times=1.50` 存在；
  - 期望：单人统计结果仍返回 `1.50`，而不是重算后回到 `0.00`。

## Recent Changes and Decisions (2026-04-09, dev 环境日志落文件)
- 现场继续要求“直接看日志”时，确认当前本地服务由 IntelliJ 直接启动，stdout/stderr 只写到 IDE Run 控制台 pipe，没有项目内文件日志可 grep。
- 为便于后续直接定位加班统计分支命中情况，`application-dev.properties` 新增：
  - `logging.file.name=logs/hainan-dev.log`
- 影响：
  - 重启本地 dev 服务后，`[加班统计-许泽刚]` 这类业务日志会同时落到项目内 `logs/hainan-dev.log`，后续可直接在会话里检索，不再依赖 IDE 控制台。

## Recent Changes and Decisions (2026-04-09, 单人统计临时文件追踪)
- 继续排查“单人统计后 3 月仍为 0”时，现场受限于两个条件：
  - 当前本地服务虽然从项目根目录启动，但 IDE Run 控制台输出在会话里不可直接读取；
  - `logging.file.name` 方案尚未在实际运行实例上稳定落出目标文件，无法立即拿到新的业务日志。
- 为了先拿到一手证据，在 `HrmOvertimeNightStatisticsServiceImpl` 增加了最小化的临时文件追踪：
  - 单人统计入口会把 `employeeId / employeeName / rawMonth / resolvedMonth` 写入项目内 `logs/hainan-overtime-debug.log`；
  - 对员工 `许泽刚`，额外记录：
    - 当月命中的排班数量、排班日期；
    - 下班打卡数量、按天归组键；
    - `hrm_employee_over_time_record` 命中数量、归组键和关键时间字段；
    - 每个工作日最终使用的 `scheduledEndSource / selectedPlan / overtimeHours / nightShiftCount`。
- 目的：
  - 不再依赖 IDE 控制台即可确认“单人统计到底查没查到排班、打卡、加班记录”；
  - 下一次现场重现后，可直接读取 `logs/hainan-overtime-debug.log` 判断根因是在入参、排班、打卡、既有加班记录，还是日级归组逻辑。

## Recent Changes and Decisions (2026-04-09, 改按 attendance_report_data 对齐 Sheet3)
- 新根因确认：
  - 桌面 `/Users/jiangyongming/Desktop/无标题.xls` 中：
    - `Sheet3` 的“加班”行在 `2026-03` 只有 `03-05 / 03-06 / 03-10 / 03-15 / 03-16 / 03-20` 有值；
    - 其中 `03-18 ~ 03-24` 只有 `03-20 = 1.5`；
  - 但原统计服务在命中真实班次 `SHIFT(07:00-16:00)` 时，会把 `03-18 ~ 03-24` 全部按 `17:30 - 16:00 = 1.5h` 自动算成加班，和 `Sheet3` 完全不一致。
- 进一步排查结论：
  - 把月报加班字段转成 `hrm_employee_over_time_record` 的逻辑位于 `CreateOverTimeAndLeaveTimeRecordTask`，当前代码已标注“当前已禁用”；
  - 因此不能再假设 `hrm_employee_over_time_record` 一定完整覆盖月报中的日级加班小时；
  - `Sheet3` 的日级加班口径更接近 `hrm_attendance_report_data` 中的 `工作日加班 / 休息日加班 / 节假日加班` 字段。
- 本次实现：
  - `HrmOvertimeNightStatisticsServiceImpl` 新增注入 `hrmAttendanceReportDataRepository`；
  - `calculateMonthlyDetails` 在重算员工当月数据时，先读取该员工该月份的月报加班字段并按工作日聚合；
  - 若该员工该月份存在任意月报加班记录，则该月所有工作日的加班小时全部优先按月报口径处理：
    - 命中的工作日取月报值；
    - 未命中的工作日视为 `0.00`；
    - 不再继续按 `SHIFT` 自动补算出额外加班。
  - 只有当该员工该月份完全不存在月报加班记录时，才继续沿用原先的 `hrm_employee_over_time_record` 与 `SHIFT` 差值口径。
- 调试追踪补充：
  - 原临时文件路径 `/tmp/hainan-overtime-debug.log` 在 macOS 上会因 `/tmp` 为符号链接而触发 `FileAlreadyExistsException: /tmp`；
  - 调试文件已改为项目内 `logs/hainan-overtime-debug.log`，并仅在父目录不存在时创建目录，避免再次因 `/tmp` 失败。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldPreferAttendanceReportOvertimeOverShiftAutoCalculation`；
  - 场景：`03-18 ~ 03-24` 每天都有 `SHIFT(07:00-16:00)` 和 `17:30` 下班卡，但月报加班字段只有 `03-20 = 1.5`；
  - 期望：单人统计结果必须只有 `03-20` 保留 `1.50`，其余日期均为 `0.00`。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldPreferAttendanceReportOvertimeOverShiftAutoCalculation -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（11 通过，0 失败）
  - `mvn -DskipTests compile`（通过）

## Recent Changes and Decisions (2026-04-09, 单人统计补审批加班来源)
- 新现场证据：
  - `/Users/jiangyongming/Desktop/无标题.xls` 的 `Sheet6` / `Sheet7` 已证明：员工 `许泽刚` 在 `2026-03-18 ~ 2026-03-24` 的本地月报“加班”字段全部为 `0`；
  - 同期 `hrm_employee_over_time_record` 为空，说明单靠本地月报字段和既有加班记录无法还原 `Sheet3` 的人工统计口径；
  - 继续排查同步链路后确认：`/attendanceData/syncAll` 走的是 `ILeaveRecordDtaService`，只会补 `hrm_attendance_report_data`，不会补 `getupdatedata` 里的审批加班。
- 本次实现：
  - 新增 `SingleEmployeeOvertimeHoursProvider`：
    - 先按 `employeeId -> tbattendanceuser.userId` 映射读取本地 `tbattendanceapprove` 的加班审批；
    - 若本地审批为空，再补查钉钉历史加班列 `1078299679`；
    - 仅保留正数加班日，避免“全 0 数据源”再次错误覆盖现有结果。
  - `HrmOvertimeNightStatisticsServiceImpl#startStatisticsForEmployee` 在保存明细前引入该提供器：
    - 单人统计命中审批/钉钉日级加班后，以该来源统一覆盖当月 `overtimeHours`；
    - 夜班次数、计划下班时间、实际下班时间仍沿用原统计链路，不改变夜班口径。
  - 调试日志补充：
    - 当员工命中单人专用加班来源时，额外写入 `[single-stat-provider]`，记录提供器返回值及覆盖后的日明细。
- 代码变更：
  - 新增：`src/main/java/com/tianye/hrsystem/imple/SingleEmployeeOvertimeHoursProvider.java`
  - 调整：`HrmOvertimeNightStatisticsServiceImpl`
  - 调整：`tbattendanceuserRepository`
  - 调整：`tbattendanceapproveRepository`
  - 调整测试：`HrmOvertimeNightStatisticsServiceImplTest`
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldPreferSingleEmployeeProviderOverZeroedAttendanceReport -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（12 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计回退为纯入库数据统计，已被 2026-07-15 口径取代)
- 业务最新要求变更：
  - 不再考虑审批加班与在线钉钉补查；
  - 由于钉钉月度接口存在调用次数限制，单人统计必须优先且仅使用已同步入库的数据计算。
- 本次调整（历史记录，2026-07-15 起单人统计已改回与“开始统计”一致的 `OvertimeHoursSourceMode.DEFAULT`）：
  - `HrmOvertimeNightStatisticsServiceImpl#startStatisticsForEmployee` 当时改为走 `RAW_SYNC_DATA_ONLY` 模式：
    - 只使用 `hrm_attendance_plan / hrm_attendance_group / hrm_attendance_shift / hrm_attendance_clock`；
    - 不再读取 `hrm_attendance_report_data`；
    - 不再读取 `hrm_employee_over_time_record`；
    - 不再在线调用钉钉 `1078299679`。
  - 该“纯入库数据”约束已被 2026-07-15 的“单人统计与开始统计业务逻辑一致”要求取代；当前代码仅保留“不在线调用钉钉补查”的约束，默认统计链路中的本地月报、既有加班记录、本地审批、排班与打卡等已入库来源均可按同一规则参与。
  - 统计结果含义同步收口：
    - 若当天能解析到真实 `SHIFT` 班次结束时间，且下班打卡晚于该时间，则直接按差值计入加班；
    - 若当天只能回退到 `OffDuty planCheckTime / attendanceTime`，仍维持现有规则，不自动产出加班。
  - 清理了上个回合新增的单人专用在线兜底实现与仓库方法，避免后续误用。
- 影响：
  - 对员工 `许泽刚` 的 `2026-03-18 ~ 2026-03-24`，若班次能解析为 `07:00-16:00` 且下班卡约为 `17:30`，单人统计将按每天约 `1.50` 小时返回；
  - 该结果不再尝试与桌面 `Sheet3` 的审批/人工口径对齐。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（11 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计班次解析继续回退考勤组)
- 新根因确认：
  - 最新 `logs/hainan-overtime-debug.log` 显示，`许泽刚` 在 `2026-03-18 ~ 2026-03-24` 的单人统计已经进入纯入库数据分支，但多天日志仍为：
    - `classId=1516625133`
    - `scheduledEndSource=PLAN_CHECK_TIME`
    - `shift=null`
    - `overtimeHours=0.00`
  - 进一步代码核对确认两个断点：
    - `resolveShiftByGroupAndDate()` 之前只要 `classId != null` 就直接返回 `resolveShift(classId)` 结果；当本地 `hrm_attendance_shift` 已无该班次时，不会继续回退考勤组；
    - `shiftSetting` 的索引沿用了旧服务的星期偏移逻辑，与当前需求“按周一到周日顺序排列”不一致。
- 本次修复：
  - `HrmOvertimeNightStatisticsServiceImpl#resolveShiftByGroupAndDate` 改为：
    - 先尝试 `classId`；
    - 若 `classId` 查不到真实班次，则继续尝试当前排班 `groupId`；
    - 当前 `groupId` 失效时，再继续尝试该员工当月排班中出现过的其他 `groupId`；
    - 命中任一有效考勤组后，再按 `shiftSetting` 映射班次。
  - `shiftSetting` 映射索引改为严格按“周一到周日”取值，即 `MONDAY -> index 0 ... SUNDAY -> index 6`。
  - 调试日志中的月度组兜底信息改为输出 `fallbackGroupIds`，便于继续核对现场实际走到哪一个考勤组。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToGroupShiftSettingWhenClassIdShiftMissing`
  - 场景：`classId` 为旧值且本地班次表查空、当前排班 `groupId` 失效，但该员工当月另有已同步的有效考勤组，且其 `shiftSetting` 中周五班次为 `07:00-16:00`；
  - 期望：`2026-03-20` 仍能解析出 `SHIFT`，并按 `17:30 - 16:00 = 1.50` 小时返回，而不是继续落成 `0.00`。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToGroupShiftSettingWhenClassIdShiftMissing -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（12 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计兼容历史组ID与历史班次)
- 现场证据：
  - `logs/hainan-overtime-debug.log` 中用户真实触发的 `2026-04-09 11:37:14` 单人统计日志仍显示：
    - `employeeId=1831601326890434598`
    - `rawMonth=2026-03 resolvedMonth=2026-03`
    - `2026-03-18 ~ 2026-03-24` 多天均为 `classId=1516625133 groupId=1449530106 scheduledEndSource=PLAN_CHECK_TIME shift=null overtimeHours=0.00`
  - 这说明前端月份参数已正确传到后端，但后端当时运行的代码仍无法把该批历史排班解析为真实 `SHIFT`。
- 根因补充：
  - 现场数据形态与原先假设不完全一致，除了“当前组失效后回退其他 `groupId`”外，还存在两种历史数据情况：
    - 排班里的 `groupId` 可能不是当前 `attendance_group_id`，而是 `hrm_attendance_group.old_group_id`；
    - 排班里的 `classId` 可能已从 `hrm_attendance_shift` 删除，但仍存在于 `hrm_attendance_history_shift`。
- 本次修复：
  - `hrmAttendanceGroupRepository` 新增 `findFirstByOldGroupId(Long oldGroupId)`；
  - `HrmOvertimeNightStatisticsServiceImpl#resolveAttendanceGroup` 改为先查 `findById(groupId)`，查不到时再查 `findFirstByOldGroupId(groupId)`；
  - `hrmAttendanceHistoryShiftRepository` 新增 `findFirstByShiftIdOrderByUpdateTimeDesc(Integer shiftId)`；
  - `HrmOvertimeNightStatisticsServiceImpl#resolveShift` 改为：
    - 先查当前 `hrm_attendance_shift`；
    - 若为空，再查 `hrm_attendance_history_shift`；
    - 命中历史班次后转换为 `HrmAttendanceShift` 继续复用现有统计流程。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToAttendanceGroupOldGroupIdWhenCurrentGroupIdMissing`
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToHistoryShiftWhenCurrentShiftMissing`
  - 两个场景都验证 `2026-03-20` 可恢复为 `scheduledEndSource=SHIFT`，并返回 `1.50` 小时，而不是 `0.00`。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToAttendanceGroupOldGroupIdWhenCurrentGroupIdMissing,HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToHistoryShiftWhenCurrentShiftMissing -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（14 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计班次解析诊断增强)
- 现场最新状态：
  - 真实员工 `1831601326890434598` 在 `2026-04-09 11:53` 的单人统计仍然全部落到 `scheduledEndSource=PLAN_CHECK_TIME / shift=null / overtimeHours=0.00`；
  - 说明“历史组ID/历史班次”兜底仍未覆盖真实库中的全部数据形态，需要继续看运行时解析链路，而不能再盲改统计规则。
- 为缩小根因范围，`HrmOvertimeNightStatisticsServiceImpl` 对调试员工新增 `[daily-resolution]` 诊断日志：
  - 输出当天所有排班的 `planId/checkType/classId/groupId/planCheckTime`；
  - 输出所选排班 `classId` 的解析结果；
  - 输出每个候选 `groupId` 的命中方式（当前ID/旧组ID）、`shiftSetting` 原值、星期索引、解析出的班次 token 与最终班次结果。
- 目的：
  - 判断真实库里究竟是“同日还有其他可用排班未被当前选择逻辑命中”，还是“考勤组能命中但 `shiftSetting` 无法解析”，或“历史班次表本身无对应记录”。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（通过，14 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计回退日级班次快照)
- 现场最新根因确认：
  - 用户重新触发后，`logs/hainan-overtime-debug.log` 的 `[daily-resolution]` 显示：
    - `classId=1516625133` 在当前班次表与历史班次表都解析不到；
    - 候选考勤组 `1449530106 / 1205897854` 均能命中当前组记录，但它们当前库里的 `shiftSetting=null`；
    - 同时两条考勤组记录的 `effectTime=2026-04-04 03:18:47`，明显是 4 月生效的新配置，而统计目标是 2026-03。
- 结论：
  - 对这批真实 3 月排班数据，`hrm_attendance_group` 与 `hrm_attendance_shift/history_shift` 已不足以还原历史班次；
  - 但项目内仍存在库内日级班次快照表 `hrm_attendance_date_shift`，它按“员工 + 日期”保存当天班次的上下班时间，更适合作为“只用已入库原始数据”的最后一层兜底。
- 本次修复：
  - `hrmAttendanceDateShiftRepository` 新增 `findFirstByEmployeeIdAndUserShiftTimeBetween(Long employeeId, Date begin, Date end)`；
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails` 在 `classId -> historyShift -> group.shiftSetting` 全部失败后，新增 `resolveDateShift(employeeId, workDate)`；
  - 命中 `hrm_attendance_date_shift` 后，将该日快照转换为 `HrmAttendanceShift`，继续沿用既有 `SHIFT` 计算逻辑。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToDateShiftWhenGroupAndShiftAreUnavailable`
  - 场景：`classId` 失效、历史班次为空、考勤组存在但 `shiftSetting=null`，而 `hrm_attendance_date_shift` 中该员工该日有 `07:00-16:00` 班次快照；
  - 期望：`2026-03-20` 仍按 `17:30 - 16:00 = 1.50` 小时返回。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToDateShiftWhenGroupAndShiftAreUnavailable -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（15 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计 date_shift 诊断补强)
- 现场继续为 `0` 时，上一版 `[daily-resolution]` 只输出了 `classId` 和候选 `groupId` 的解析结果，仍无法回答两个关键问题：
  - 当前运行实例是否真的已经部署到包含 `hrm_attendance_date_shift` 回退的代码；
  - 若已部署，`hrm_attendance_date_shift` 对目标员工/日期究竟是“有快照但没被后续逻辑使用”，还是“压根查不到快照”。
- 本次调整：
  - `HrmOvertimeNightStatisticsServiceImpl#buildShiftResolutionTrace` 新增输出 `selectedEmployeeId` 与 `dateShiftResolved`；
  - 运行时每日统计先实际解析一次 `resolveDateShift(employeeId, workDate)`，`[daily-resolution]` 直接复用该结果，避免仅为日志再次查询数据库；
  - 保留原有 `selectedClassResolvedShift` 与 `candidateGroups` 诊断信息，便于把“班次表失效”“考勤组被 4 月配置覆盖”“日级快照缺失”三种情况一次性区分开。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#buildShiftResolutionTrace_shouldIncludeDateShiftResolutionWhenDateShiftFallbackExists`；
  - 先验证旧日志格式不包含 `dateShiftResolved` 会失败，再补齐实现；
  - 同时保持 `startStatisticsForEmployee_shouldFallbackToDateShiftWhenGroupAndShiftAreUnavailable` 仍只查询一次 `hrm_attendance_date_shift`。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#buildShiftResolutionTrace_shouldIncludeDateShiftResolutionWhenDateShiftFallbackExists -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（16 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计复用最近 date_shift 快照)
- 新根因确认：
  - 真实运行日志已经证明实例加载了新代码，因为 `[daily-resolution]` 中出现了 `dateShiftResolved=`；
  - 但真实员工 `1831601326890434598` 在 `2026-03-18 ~ 2026-03-24` 仍全部为 `dateShiftResolved=null`，说明“按当天 `between begin/end` 精确查询 `hrm_attendance_date_shift`”仍可能漏掉历史快照；
  - 结合项目内 `HrmAttendanceDateShiftMapper.xml` 现有设计，`date_shift` 业务上更接近“目标日期及之前最近一次生效班次快照”，而不是保证每天都精确存在一条记录。
- 本次修复：
  - `hrmAttendanceDateShiftRepository` 新增 `findFirstByEmployeeIdAndUserShiftTimeLessThanEqualOrderByUserShiftTimeDesc(Long employeeId, Date end)`；
  - `HrmOvertimeNightStatisticsServiceImpl#resolveDateShift` 改为按“`user_shift_time <= 目标工作日结束时间`，倒序取最近一条”恢复班次；
  - 因此当目标工作日没有精确快照，但此前最近一天存在有效班次快照时，单人统计也能继续恢复 `SHIFT` 计划下班时间。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToLatestPriorDateShiftWhenSameDaySnapshotMissing`；
  - 场景：`2026-03-20` 当天没有 `date_shift`，但 `2026-03-18` 有 `07:00-16:00` 快照；
  - 期望：仍按最近快照恢复 `SHIFT`，返回 `1.50` 小时，而不是继续落成 `0.00`。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldFallbackToLatestPriorDateShiftWhenSameDaySnapshotMissing -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（17 通过，0 失败）

## Recent Changes and Decisions (2026-04-09, 单人统计主流程接入班次恢复)
- 最新根因确认：
  - `HrmOvertimeNightStatisticsServiceImpl#calculateMonthlyDetails` 主循环里虽然已经存在 `resolveShiftByGroupAndDate / resolveDateShift / resolveHistoryShift` 等 helper，但实际仍只调用 `resolveShift(classId)`；
  - 这会导致 `classId` 失效场景下，排班开始时间直接回退成 `OnDuty planCheckTime` 里的提前打卡窗口时间，例如 `07:00`，从而把 `2026-03-20 07:48 -> 17:30` 误算为 `1.68` 小时。
- 本次修复：
  - 主循环改为优先走 `classId -> 当前班次/历史班次`，失败后继续按 `groupId + shiftSetting` 恢复班次，最后再回退 `date_shift`；
  - 因此当真实班次能恢复出 `08:00-17:30` 时，有效上班时间会被截到 `08:00`，不再错误使用 `07:48` 或 `07:00`；
  - 补齐缺失的 `ScheduledStartTimeResolution / ScheduledStartTimeSource` 类型，恢复编译；
  - `3月20日` 控制台诊断输出统一改为中文来源文案，直接打印“原始数据 + 计算步骤”，便于和人工统计逐项对照。
- 回归覆盖：
  - 新增 `HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldUseGroupShiftWhenClassShiftMissing`；
  - 场景：`classId` 无法命中班次表，`planCheckTime` 为 `07:00/15:00`，但考勤组 `shiftSetting` 可恢复出 `08:00-17:30`；
  - 期望：`2026-03-20` 最终按 `08:00 -> 17:30` 计入工时，返回 `1.50` 小时，同时计划下班时间恢复为 `17:30`。
- 验证：
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest#startStatisticsForEmployee_shouldUseGroupShiftWhenClassShiftMissing -DfailIfNoTests=false test`（通过）
  - `mvn -Dtest=HrmOvertimeNightStatisticsServiceImplTest -DfailIfNoTests=false test`（14 通过，0 失败）
  - `mvn -DskipTests compile`（通过）

## Recent Changes and Decisions (2026-05-31, 排班管理只读记录展示)
- 前端“排班管理”从钉钉考勤组列表调整为排班记录列表；后端暂不新增接口，继续复用 `/workPlan/getData` 按日期范围读取 `tbplanlist`。
- 标准排班通过 `classId` 结合标准班次选项显示班次名称/时间；自定义排班通过 `shift_source/custom_shift_id/customStart/customEnd/customShiftPeriod/customCrossDay` 显示时间、跨天与白/夜班别。
- `GroupID/ClassID` 继续作为旧标准排班兼容字段保留，但不再作为排班管理主页面的用户概念。
- 本轮仅做只读展示，不改变 `saveAll`、Excel 导入、单员工单日自定义排班保存等写入链路。

## Recent Changes and Decisions (2026-05-31, 排班管理日历矩阵展示)
- 前端“排班管理”主视图从只读表格列表调整为月历矩阵。
- 数据源仍复用 `/workPlan/getData`，按所选月份首日至末日一次加载排班记录。
- `buildWorkPlanCalendarMatrix` 在前端按周一到周日生成矩阵，并按日期聚合标准排班、自定义排班、白/夜班和摘要文本。
- 日期格只展示概览，点击日期后在下方明细表展示当天完整排班记录。
- 写入链路不变：本轮不修改 `saveAll`、Excel 导入、单员工单日自定义排班保存等接口。

## Recent Changes and Decisions (2026-05-31, 排班管理员工日期矩阵展示)
- 前端“排班管理”最终改为员工月度横向矩阵，结构对齐“打卡记录”：员工一行，日期为列。
- `Scheduling.vue` 使用 `buildMonthDates(form.month)` 生成当月日期列，左侧固定员工、车间、工段、统计列。
- `buildWorkPlanEmployeeMatrix` 将 `/workPlan/getData` 返回的排班记录按员工聚合，并为每个日期初始化 `dayMap`。
- `getWorkPlanMatrixCellView` 负责将某员工某天的排班记录转为单元格显示文本与色调：空、自定义、夜班、标准。
- 已移除主页面月历格视图，不再使用 `calendarMatrix/calendar-day-card`。

## Recent Changes and Decisions (2026-05-31, 排班矩阵全员显示)
- `Scheduling.vue` 已加载 `getAllUsers`，矩阵行基准改为 `userOptions` 全员列表，而不是仅按 `/workPlan/getData` 中有记录的员工生成。
- `buildWorkPlanEmployeeMatrix(records, monthDates, userOptions)` 先为所有用户初始化 `dayMap` 空单元格，再把排班记录填入对应员工日期。
- 该口径为后续在空单元格上扩展补排班/编辑能力预留入口。

## 2026-05-31 排班管理员工矩阵与设置入口迁移
- 后端 `UserObject` 新增 `employeeId` 字段；`WorkPlanServiceImpl` 在本地展示人员快照和钉钉人员回退路径中从 `tbattendanceuser.empId` 填充该字段。
- 前端 `Scheduling.vue` 作为排班管理主页面：使用 `getAllUsers` 初始化全员矩阵行，再用 `getWorkPlanData` 填充当月排班记录。
- 前端 `buildWorkPlanEmployeeMatrix` 保留每行 `employeeId`，支持无排班员工空单元格和已有记录员工的单日弹窗保存。
- 已从排班管理矩阵移除车间/工段固定列，并同步移除关键字对 `productName/linkName` 的匹配。
- 已将打卡记录页原有“点击单元格修改自定义排班”弹窗逻辑迁移到排班管理；`Overview.vue` 移除 `queryEmployeeDayShift/saveEmployeeDayCustomShift` 和相关弹窗状态。
- 验证通过：`node tests/work-plan-scheduling-records.test.mjs`、`node tests/clock-overview-source.test.mjs`、`node tests/work-plan-utils.test.mjs`、`node tests/work-plan-records-page.test.mjs`、`node tests/clock-overview-utils.test.mjs`。
- 验证通过：`mvn -Dtest=HrmAttendanceDataControllerTest#getAllUsers_shouldUseDisplayUsersCache test` 和 `mvn -Dtest=HrmAttendanceDataControllerTest,WorkPlanListControllerTest test`。
- 验证通过：`npm run build`；仍仅保留项目既有 `::v-deep` 过时提示与 chunk size warning。

## 2026-05-31 排班管理关键字查询修复
- 根因：`Scheduling.vue` 先通过 `filterWorkPlanRecords` 过滤排班记录，但 `buildWorkPlanEmployeeMatrix` 随后总是按 `getAllUsers` 补齐全员，导致输入关键字后矩阵仍显示未命中的员工空行，看起来像“没有查询结果”。
- 修复：`buildWorkPlanEmployeeMatrix(records, monthDates, userOptions, filters)` 新增筛选上下文；存在关键字、排班类型或班别筛选时，仅保留命中记录对应员工，或关键字直接命中员工本人信息的空排班员工。
- 匹配字段：记录关键字匹配员工、员工标识/手机号、班次文本、排班类型、白/夜班别和排班日期；继续排除车间/工段字段。
- 回归：`tests/work-plan-scheduling-records.test.mjs` 覆盖员工姓名、手机号、班次关键字和无排班员工关键字查询，确保筛选态不会重新补回全员。

## 2026-05-31 排班管理矩阵导出
- 前端 `Scheduling.vue` 在筛选按钮区域新增“导出Excel”按钮，导出当前 `employeeMatrixRows`，文件名为 `排班管理_YYYY-MM.xlsx`。
- `work-plan-utils.js` 新增 `buildWorkPlanMatrixExportRows`，将矩阵行转换为二维数组：首列员工、第二列统计、后续为每日列，单元格文本复用 `getWorkPlanMatrixCellView`，保持与页面显示一致。
- 导出使用前端 `xlsx` 依赖的 `aoa_to_sheet / book_new / book_append_sheet / writeFile`，不调用后端导出接口。
- 测试 `tests/work-plan-scheduling-records.test.mjs` 覆盖导出表头、员工行、空单元格、自定义排班和标准排班导出文本。

## 2026-05-31 打卡记录查看与 2026-04 历史月显示修复
- 调用链确认：`HrmAttendanceEmpMonthRecordController` 的 `/queryAttendanceEmpMonthDailyDetailPageList` 和 `/queryAttendanceEmpMonthDailyDetail` 均进入 `HrmAttendanceClockServiceImpl`，普通查看链路通过 `HrmAttendanceClockMapper` 查询本地 `hrm_attendance_clock`，不是每次查看都请求钉钉。
- 查看列表链路中的 `leaveRecordService.queryOaLeaveExamineList()` 当前只是读取本地审批配置占位逻辑；`attendanceGroupService.checkInitAttendData()` 只检查本地默认考勤组并在缺失时走 `configService.initAttendData()` 初始化本地默认数据。
- 钉钉接口调用集中在同步链路：`HrmAttendanceDataController#sync/syncAll` -> `HrmAttendanceDataServiceImpl#SyncData`，再由考勤计划、打卡明细、组织/用户/报表等组件调用 `AttendancePlanRecord`、`AttendanceDetailRecord`、`AttendanceGroupManager`、`AttendanceUserManager` 等钉钉封装。
- 2026-04 已拉取但界面显示为空的根因在单员工月明细组装：`queryAttendEmpMonthDetailByDate` 以 `hrm_employee.create_time` 参与历史日期拦截，员工档案创建时间晚于查询月份时会把本地已有打卡明细过滤掉。
- 修复：`queryAttendEmpMonthDetailByDate` 移除 `createTime` 展示拦截，仅保留“非未来日期”和“入职日期已到”的判断；该口径与 2026-03-27 多租户打卡缺失排查要求保持一致。
- 回归：`HrmAttendanceClockServiceImplTest` 覆盖员工 `createTime` 早于/晚于历史查询月份时，已同步的 2025-10 打卡仍能返回 `08:00/17:00`。
- 验证命令：`mvn -Dtest=HrmAttendanceClockServiceImplTest test`。


## 2026-06-01 打卡记录 proxy request failed 排查
- 现象：本地 `http://127.0.0.1:8081` 打卡记录页面请求 `/api/hrsystem/hrmAttendanceEmpMonthRecord/queryAttendanceEmpMonthDailyDetailPageList` 返回 `502`，消息为 `proxy request failed:`。
- 边界定位：8081 是 `hr_web/scripts/local-deploy-server.mjs` 静态服务，默认将 `/api/**` 代理到 `http://localhost:9080`；打卡记录业务接口本身仍是本地后端 `HrmAttendanceEmpMonthRecordController`。
- 根因：复现时本机 `9080` 后端不可达或处于异常状态；当后端恢复监听后，同一路径不再返回代理错误，未登录时返回“请输入token”，登录后打卡概况接口返回 `code=0` 和分页数据。
- 前端配套修复：`hr_web` 新增 `formatProxyRequestError(error, targetUrl)`，代理失败时返回 `proxy request failed: <错误码或unknown error>; target=<origin>`，避免底层 `error.message` 为空时页面只显示空白错误。
- 2026-06-16 补充：若存在 `HrsystemApplication` Java 进程但 `lsof -nP -iTCP:9080 -sTCP:LISTEN` 无输出，仍会出现 `connect ECONNREFUSED 127.0.0.1:9080`；本次现场看到 IDE Debug 进程带 `suspend=y` 但未监听，待其恢复并绑定 `*:9080` 后，`POST http://127.0.0.1:8081/api/hrsystem/login?...` 返回 `200`。

## 2026-06-01 排班调休实现
- 后端 `WorkPlanServiceImpl` 将 `shiftType=rest` 归一为本地调休排班类型，`submitPlans(...)` 新增调休分流，复用本地落库路径生成 `tbplanlist`，不进入钉钉标准排班提交。
- `validatePlanForSubmit(...)` 对调休仅校验人员和日期，并清空 `classId/customShiftId/customStart/customEnd/customShiftPeriod/customCrossDay`。
- `saveEmployeeDayShift(...)` 新增通用单日保存入口，兼容原 `saveEmployeeDayCustomShift(...)`，支持排班管理弹窗将单日改为调休。
- `/workPlan/saveEmployeeDayCustomShift` 新增可选 `shiftType` 参数；未传时保持旧行为按自定义班次保存，传 `rest/调休/休息/3` 时保存调休。
- 测试覆盖：`WorkPlanServiceImplTest` 验证批量调休本地保存且不调用钉钉、单日调休保存清空班次时间；`WorkPlanListControllerTest` 验证控制器透传 `shiftType=rest`。

## 2026-06-02 排班管理上传与模板下载
- 后端保留 `IWorkPlanService#createImportTemplateExcel()` 兼容既有服务测试；`/workPlan/downloadImportTemplate` 改为读取 classpath 固定资源 `src/main/resources/export/workplan.xlsx`，根据请求参数 `templateMonth=yyyy-MM` 重写第一行日期栏，写入完整 `yyyy-MM-dd` 字符串日期；当月最后一天正常写入；每个有效日期组会合并第一行对应 5 列，日期行和第二行 5 个字段表头统一水平/垂直居中，同一日期组使用同一种填充色，相邻日期组使用两种填充色循环区分；超出月份天数的日期组会同时清空第一行日期和第二行 5 个字段表头，避免没有 31 号时仍显示第 31 天整列内容，有 31 号月份也会清空 31 号之后的无效字段列，再返回 `workplan.xlsx`。
- 后端 `/workPlan/downloadImportTemplate` 下载响应使用 Excel MIME、`Content-Disposition` 和 `fileDownload=true` Cookie，并在资源缺失时返回 404。
- 后端导入预览 `previewImportExcel(...)` 新增读取“白班/夜班”列，并写入 `tbplanlist.customShiftPeriod`；导入行 VO 同步新增 `customShiftPeriod` 字段用于前端预览。
- 测试覆盖：`WorkPlanServiceImplTest` 验证导入白/夜班映射与模板表头，`WorkPlanListControllerTest` 验证下载接口不调用运行时模板生成服务，并会按 `templateMonth` 写入完整日期、覆盖当月最后一天、清空不存在日期整组 5 列以及正文行，同时验证有 31 号月份保留第 31 天整组表头。

## 2026-06-02 排班导入模板支持调休
- `WorkPlanServiceImpl` 的导入模板表头新增 `排班类型`，最新列顺序为 `排班日期/员工/电话/排班类型/排班时间/白班/夜班/是否连班/备注`，历史 `手机号` 列仍作为兼容别名读取。
- 导入预览 `WorkPlanImportPreviewRowVO` 新增 `shiftType`；解析时 `排班类型=调休/rest/休息/3` 归一为 `shiftType=rest`，不解析排班时间与白夜班别。
- 调休导入行仍走本地排班保存链路，最终由既有 `validatePlanForSubmit(...)` 和 `buildLocalRestAssignments(...)` 清空班次时间字段并落库。

## 2026-06-03 桌面排班.xlsx 上传适配预检
- 已按 `/workPlan/previewImportExcel` 当前解析逻辑检查 `/Users/jiangyongming/Desktop/排班.xlsx`。
- 当前文件表头为 `员工/日期/上班时段/备注`，缺少后端必需表头 `排班日期` 与 `排班时间`，因此无法直接通过上传排班预览。
- 当前文件 149 条数据行的日期均为 `5 月 X 日`，而 `parseImportWorkDate(...)` 只接受 `yyyy-MM-dd`、`yyyy/M/d`、`yyyy/MM/dd`。
- 当前文件有 34 行时间文本会被 `parseImportScheduleTime(...)` 拒绝，主要包括 `次日` 文案和 `、` 分隔的多段上下班时间。
- 当前文件有 23 行夜班/跨天班次线索，导入前应将 `白班/夜班` 明确填为 `夜班`，并将跨天时间改为 `开始时间-结束时间` 形式由后端自动判定跨天。

## 2026-06-03 生成桌面排班_上传版.xlsx
- 已根据用户确认的转换口径生成 `/Users/jiangyongming/Desktop/排班_上传版.xlsx`，不覆盖原文件。
- 生成文件工作表为 `排班明细`，表头为 `排班日期/员工/手机号/排班类型/排班时间/白班/夜班/备注`，另含 `转换说明` 工作表记录来源与规则。
- 日期转换：原始 `5 月 X 日` 统一按 2026 年转换为 `2026-05-XX`。
- 时间转换：用正则提取原始 `上班时段` 中所有 `HH:mm`，取第一个时间和最后一个时间组成单段 `HH:mm-HH:mm`；多段时间因此压平为首段开始到末段结束。
- 夜班判断：备注或时间含 `夜`、含 `次日`，或结束时间早于/等于开始时间时，`白班/夜班` 填 `夜班`，否则填 `白班`。
- 独立校验结果：149 条数据行，必需表头齐全，日期/时间/白夜班格式错误 0 条，同员工同日期重复 0 条；白班 124 条、夜班 25 条。

## 2026-06-03 排班上传直提与历史同日同名手机号规则
- 后端 `WorkPlanServiceImpl#readImportRows(...)` 曾先读取所有导入行，再按 `排班日期 + 员工姓名` 统计本次导入中同一天同名行数量，最后逐行构建 `tbplanlist`；该“同日同名才要求手机号”的判断已在 2026-07-25 被最新电话规则取代。
- 2026-07-25 起，`resolveImportEmployee(...)` 统一按导入行电话是否有内容判断：电话有内容时按“姓名 + 电话”匹配员工；电话为空时直接按姓名匹配，不再调用同日同名计数，也不再因为系统存在同名候选直接返回“请填写电话区分”。
- 前端 `Scheduling.vue` 不再挂载 `AddOrEdit` 作为上传导入界面；选择文件后直接调用 `previewWorkPlanImportExcel(...)`，若无错误则通过 `buildWorkPlanRowsFromImportPreview(...)` 和 `buildWorkPlanSubmitPayload(...)` 生成 payload 并调用 `saveWorkPlanAll(...)`。
- 前端直传后通过 `queryWorkPlanSubmitProgress(...)` 轮询提交任务，完成后用消息提示成功数量；解析错误时直接汇总前 5 条行级错误提示给用户。
- 回归验证：`mvn -Dtest=WorkPlanServiceImplTest#previewImportExcel_shouldAllowSystemSameNameWithoutMobileWhenImportDateHasSingleNameRow,WorkPlanServiceImplTest#previewImportExcel_shouldRequireMobileOnlyWhenSameNameAppearsOnSameImportDate test`；`node tests/work-plan-scheduling-records.test.mjs`。

## 2026-06-03 李冬 5月2日排班未显示排查
- 源文件 `/Users/jiangyongming/Desktop/排班_上传版.xlsx` 中李冬 2026-05-01 和 2026-05-02 都存在。
- 当前后端 `/workPlan/previewImportExcel` 解析上传版时，149 行均有效；李冬 5月1日/5月2日都解析为 userId `023950085243840734`、自定义班次 `07:00-08:00`。
- 当前 `hr_0003.tbplanlist` 实际只有李冬 2026-05-01 一条，缺少 2026-05-02，因此排班管理只显示 5月1日不是前端矩阵漏渲染，而是库里没有 5月2日排班记录。
- 排查发现前端旧 `saveWorkPlanAll(...)` 把 149 行提交 JSON 放在 `/workPlan/saveAll?Data=...` 查询串中，编码后约 64KB，存在大批量上传不可靠风险；已在 `hr_web` 改为 `FormData` 请求体提交。

## 2026-06-03 李冬同日显示两次排班排查与修复
- 直查 `hr_0003.tbplanlist` 确认李冬 userId `023950085243840734` 在 2026-05-01 有两条本地排班：ID=56 与 ID=57，排班管理矩阵是按库内行展示，因此同一天显示两次不是前端重复渲染。
- 根因是批量导入生成的 `tbplanlist` 没有历史 `id`，`WorkPlanServiceImpl#persistPlans(...)` 旧逻辑只在 sourcePlan 带 `id` 时复用旧记录；无 `id` 导入每次都会 `buildInsertPlan(...)` 新增。
- 已在 `persistPlans(...)` 保存前增加按 `WorkDate + UserID` 查找旧排班的兜底：无 `id` 或历史 `id` 不可复用时，先复用同员工同日旧记录；已有多条同员工同日记录时，保留一条并删除多余重复行。
- 对包含多人的历史排班行，修复逻辑只移除本次导入覆盖的员工并保存剩余人员，避免把同一行里的其他员工排班清掉。
- 新增单测覆盖：无 `id` 导入命中同员工同日旧记录时更新旧记录；历史已有两条同员工同日重复记录时复用最新记录并删除旧重复。
- 聚焦验证通过：`mvn -Dtest=WorkPlanServiceImplTest,WorkPlanListControllerTest test`，38 tests, 0 failures/errors。

## Recent Changes and Decisions (2026-06-03, 单双休后续变化与节假日统计)
- `HrmWorkweekSettingService#updateWeekType(...)` 新增 `recalculateFollowing` 入参语义：
  - `true` 或未传：保持原行为，从被修改周开始向后按单休/双休交替重算到年末；
  - `false`：仅修改当前周，后续周次保持原有单双休结果。
- 年度查询结果 `WorkweekYearSettingVO` 新增月度统计字段：
  - `monthSummaries`：按 1-12 月返回 `workDays/restDays/weeklyRestDays/legalHolidayRestDays/adjustedWorkDays`；
  - `totalWorkDays/totalRestDays/totalLegalHolidayRestDays/totalAdjustedWorkDays`：年度合计。
- 月度上班/休息天数计算复用现有 `hrm_attendance_legal_holidays` 表：
  - `type=1` 为调休上班日，优先覆盖为上班；
  - `type=2` 为法定休息日，优先覆盖为休息；
  - 无节假日覆盖时再按该日期所属周的单休/双休规则判断周休。
- 本次不新增数据库表，不改变既有接口路径；旧客户端不传 `recalculateFollowing` 时兼容原“向后重算”行为。

## Testing (2026-06-03, 单双休后续变化与节假日统计)
- 已执行：`mvn -Dtest=HrmWorkweekSettingServiceTest test`。
- 结果：通过；覆盖年度生成、已存在记录读取、向后重算、仅修改本周、法定节假日/调休工作日参与月度上班休息天数统计。

## 2026-06-03 排班休息与白班连班实现
- `tbplanlist` 新增 `customContinuousShift` 映射 `custom_continuous_shift`，用于记录单条自定义排班是否连班；休息和标准班次保存时强制置为 `false`。
- `HrmWorkPlanCustomShift` 新增 `continuousShift` 映射 `hrm_workplan_custom_shift.continuous_shift`；`hrmWorkPlanCustomShiftRepository` 新增按“开始/结束/跨天/班别/连班”复用本地自定义班次的方法。
- `WorkPlanServiceImpl` 通过 `normalizeCustomContinuousShift(...)` 保证只有白班可为连班；夜班即使前端误传 `true` 也会归一为 `false`。
- `/workPlan/saveAll` JSON 解析新增 `customContinuousShift`；`/workPlan/saveEmployeeDayCustomShift` 新增同名可选参数，并在非白班自定义排班时忽略该标记。
- 本地自定义班次回填、单日排班查询、提交任务持久化、Redis 兼容元数据和排班错误标签均透传连班；展示标签在白班连班时追加“连班”。
- 新增迁移脚本：`docs/sql/2026-06-03_workplan_rest_continuous_shift.sql`，为自定义班次事实表和排班记录表增加连班字段，并调整自定义班次唯一索引。
- 回归验证：`mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest test` 覆盖控制器参数、休息保存、自定义班次复用和单日保存链路。

## 2026-06-03 排班调休/休息/连班后端实现
- `tbplanlist` 新增 `rest_shift_type` 字段并映射为 `restShiftType`，取值 `adjust/rest`，用于区分调休与休息。
- `/workPlan/saveAll` 解析 `restShiftType`，旧客户端若只传 `shiftType=调休/休息` 则以该文本作为子类型归一化来源。
- `/workPlan/saveEmployeeDayCustomShift` 与 `IWorkPlanService.saveEmployeeDayShift(...)` 新增 `restShiftType` 参数，休息类保存时清空班次/自定义时间/连班字段。
- 本地排班查询、矩阵展示和错误行班次标签按 `restShiftType` 返回“调休”或“休息”；历史空值默认显示“调休”。
- 迁移脚本：`docs/sql/2026-06-03_workplan_rest_continuous_shift.sql` 增加 `tbplanlist.rest_shift_type`。
- 验证：`mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest test`。

## 2026-06-03 排班修改 ResultSet 报错修复
- 现象：排班管理“修改排班”提示 `could not extract ResultSet`。
- 根因：`tbplanlist` 实体和 `WorkPlanMapper.xml#getMaxDate` 已读取 `rest_shift_type`，但实际 dev 多租户库 `hr_0001` ~ `hr_0005` 的 `tbplanlist` 只有 `custom_continuous_shift`，缺少同一迁移中的 `rest_shift_type`，属于迁移半执行。
- 最小复现：`SELECT rest_shift_type FROM hr_0003.tbplanlist LIMIT 1;` 在修复前返回 `ERROR 1054 (42S22): Unknown column 'rest_shift_type' in 'field list'`。
- 修复：新增并执行 `docs/sql/2026-06-03_workplan_rest_shift_type_repair.sql`，按租户库检测缺列后补齐 `rest_shift_type VARCHAR(20) DEFAULT 'adjust'`。
- 验证：`/workPlan/getData`、`/workPlan/loadIsLast` 与 `/workPlan/queryEmployeeDayShift` 已能返回排班记录及 `restShiftType`，不再出现 ResultSet 异常。
- 2026-06-04 下载失败修复：`templateMonth/templateDate` 均为空时后端默认当前月份，避免旧前端/缓存页面无参调用导致下载失败；本地 8081 静态部署需执行 `npm run build` 更新 `hr_web/html`。
- 2026-06-04 日期行格式修正：用户最终确认日期行需显示 `yyyy-MM-dd`，不是单独日号；后端改为按 `YearMonth` 写入完整日期字符串，并补测试确认 `2026-06-30` 最后一天正常显示、`2026-07-01` 所在槽位为空。
- 2026-06-04 第 31 天列显示修复：没有 31 号的月份会清空第 31 天日期行和第二行字段表头整组 5 列；有 31 号的月份保留 `yyyy-MM-31` 和“白/夜班、上班时间、下班时间、是否连班、休假/调休”表头。

## 2026-06-04 workplan.xls 横向排班上传适配
- `WorkPlanServiceImpl#readImportRows` 新增横向日期分组表识别：当第 1 行为“日期”、第 2 行为“姓名、电话”或旧版“姓名”，且后续按“白/夜班、上班时间、下班时间、是否连班、休假/调休”5 列重复时，走横向表解析；否则继续走原 `排班日期/员工/排班时间` 表头解析。
- 横向表解析会把每天每名员工的填写格转换为 `WorkPlanImportPreviewRowVO`：Excel 数字日期按 POI `DateUtil` 转成 `yyyy-MM-dd`，带备注的日期文本会提取其中的 `yyyy-M-d` 日期。
- 自定义排班转换规则：白/夜班列归一为 `day/night`，上下班时间拼成 `开始-结束` 后复用原 `parseImportScheduleTime`，电话列写入预览行 `mobile`，是否连班通过 `customContinuousShift/customContinuousShiftExplicit` 传入 `tbplanlist`。
- 休息/调休转换规则：`休假/调休` 为“是/休/休息/休假”时生成 `shiftType=rest, restShiftType=rest`；为“调休”时生成 `restShiftType=adjust`；休息类不解析时间。
- 新增回归测试：`WorkPlanServiceImplTest#previewImportExcel_shouldAdaptWorkplanHorizontalDateGroupedXls`，覆盖 `.xls`、合并日期、日期备注、Excel 序列号日期、白班连班和休息行。

## 2026-06-10 员工管理批量设置参保方案

- 后端 `HrmEmployeeServiceImpl#updateInsuranceScheme` 已补充批量场景校验：员工 ID 列表不能为空、参保方案必填且必须存在，员工不存在时返回中文业务错误。
- `/hrmEmployee/updateInsuranceScheme` 继续复用原接口，请求体仍为 `employeeIds + schemeId`；方法新增事务注解，批量设置中任一员工失败时整体回滚。
- 新增单元测试 `HrmEmployeeServiceImplUpdateInsuranceSchemeTest` 覆盖空员工列表、缺少参保方案两个批量参数校验场景。
- 前端 `hr_web/src/views/hrm/employee/Components/UpdateSchemeDialog.vue` 批量模式已改为弹窗内左右穿梭框选择员工，按钮不再依赖列表勾选；前端保存前同时校验员工和参保方案必选。

## 2026-06-10 员工编辑部门与学历保存修复

- 后端 `HrmEmployeeServiceImpl#addOrUpdateEduExperience(...)` 新增员工存在性校验；当 `employeeId` 不存在时抛出中文业务错误“员工不存在，无法保存学历信息”，避免学历保存时出现空指针异常。
- 员工岗位信息保存继续兼容前端以字符串形式提交 `dept_id`、`parent_id` 等雪花 ID，避免 JavaScript 大整数精度丢失后导致部门保存失败。
- 新增 `HrmEmployeeServiceImplEducationExperienceTest` 覆盖学历保存无效员工的中文错误。

## 2026-06-11 员工年龄与司龄口径排查
- 字段定义：`HrmEmployee` 映射 `hrm_employee.age/date_of_birth/company_age_start_time/company_age`，其中年龄和司龄均为库字段。
- 年龄写入：`HrmEmployeeServiceImpl#transferEmployee` 在新增员工时根据身份证/出生日期调用 `DateUtil.ageOfNow(...)` 写入 `age`；字段化编辑链路同样会在保存时重新计算并写入。
- 导入写入：员工 Excel 导入通过 `EmployeeImportVO.age` 直接 `setAge(...)`，同时会从身份证或导入出生日期补 `dateOfBirth/birthday`，但不会在导入后建立年度刷新机制。
- 司龄展示：员工列表 `queryPageList` 查询出 `companyAgeStartTime` 后，运行时计算“开始日期到当前时间”的天数并用 `EmployeeUtil.computeCompanyAge(...)` 转成 `X年X月X天`，覆盖返回 Map 中的 `companyAge`。
- 定时任务：现有 `autoTask` 中未发现年龄或司龄年度递增任务；考勤类任务按月刷新考勤/排班/报表数据，不批量更新 `hrm_employee.age/company_age`。

## 2026-06-11 员工列表固定字段 Map Key 修复
- 根因：`/hrmEmployee/queryPageList` 返回 `Map<String,Object>`，`HrmEmployeeMapper.xml#queryPageList` 中固定字段使用 `id_type/id_number/date_of_birth` 等下划线列名或别名；项目未配置 Map 结果下划线转驼峰包装器，导致前端按 `idType/idNumber/dateOfBirth` 读取时为空。
- 修复：员工列表 SQL 对固定列显式设置驼峰别名，包括 `idType`、`idNumber`、`dateOfBirth`、`entryTime`、`companyAgeStartTime`、`deptName` 等，保持与服务层后处理和前端列字段一致。
- 测试：新增 `HrmEmployeeMapperSqlTest#queryPageList_shouldExposeEmployeeIdentityAndBirthdayFieldsAsCamelCaseKeys`，锁定证件类型、证件号码、出生日期、生日、年龄字段的列表返回契约。

## Employee List Display Stability (2026-06-11)
- 后端员工列表链路：`HrmEmployeeController#queryPageList` -> `HrmEmployeeServiceImpl#queryPageList` -> `HrmEmployeeMapper#queryPageList`。
- `HrmEmployeeMapper.xml#queryPageList` 对固定字段显式返回驼峰 key，避免 `Map` 结果受下划线列名影响。
- `HrmEmployeeServiceImpl#queryPageList` 在返回前执行两类兜底：
  - 对身份证员工使用 `idNumber` 派生 `dateOfBirth/birthday/age`，只在原字段为空时填充，不覆盖数据库已有值；
  - 动态字段 `fieldName/type` 为空时跳过该条动态字段，避免 `map.put(null, ...)` 导致 Jackson 序列化报 `Null key for a Map not allowed in JSON`。
- 回归测试：`HrmEmployeeMapperSqlTest` 覆盖固定字段驼峰返回；`HrmEmployeeServiceImplQueryPageListTest` 覆盖动态字段空 key 和身份证生日兜底。

## 2026-06-11 员工生日短横杠与司龄链路修复
- `HrmEmployeeServiceImpl#queryPageList` 新增列表后处理：身份证生日/年龄兜底后继续调用司龄填充逻辑；司龄开始日期优先取 `companyAgeStartTime`，为空时使用 `entryTime` 回退，并返回给前端用于展示和编辑入口。
- `HrmEmployeePostServiceImpl#postInformation` 新增司龄刷新逻辑：岗位详情读取时对在职员工按司龄开始日期动态计算 `companyAge`；旧数据缺 `companyAgeStartTime` 时回填入职日期，旧数据缺 `companyAge` 时避免 `EmployeeUtil.computeCompanyAge(null)` 报错。
- `AddEmployeeBO` 增加 `companyAgeStartTime`，新增员工后端 `prepareEmployeeForAdd` 保留前端显式传入的司龄开始日期；未传时默认使用入职日期。
- 编辑岗位信息沿用固定字段 `company_age_start_time`，FastJSON 下划线字段可映射为实体 `companyAgeStartTime`，因此前端补出该固定字段即可保存。
- 新增/更新测试：`HrmEmployeeServiceImplCompanyAgeTest`、`HrmEmployeePostServiceImplCompanyAgeTest`、`HrmEmployeeServiceImplQueryPageListTest`、`HrmEmployeeMapperSqlTest`，覆盖新增默认值、编辑字段映射、列表旧数据回退、岗位详情旧数据空司龄 NPE 和分页动态字段空 key。
- 真实接口验证（2026-06-11，本机 9083）：`POST /hrsystem/hrmEmployee/queryPageList` 每页 15 条第 1 页与第 6 页均返回 `code=0`、`count=15`；旧员工返回 `companyAgeStartTime=entryTime` 与非空 `companyAge`。
- 真实接口验证（2026-06-11，本机 9083）：`POST /hrsystem/hrmEmployeePost/postInformation/{employeeId}` 返回 HTTP 200，包含 `company_age_start_time`（司龄开始日期）与 `company_age`（司龄描述）。
- 已验证：`mvn -Dtest=HrmEmployeeServiceImplCompanyAgeTest,HrmEmployeePostServiceImplCompanyAgeTest,HrmEmployeeServiceImplQueryPageListTest,HrmEmployeeMapperSqlTest test` 通过，8 个测试 0 失败。
- 追加稳健性处理：`EmployeeUtil.computeCompanyAge(null)` 返回空字符串，避免离职员工、员工档案等其他入口遇到历史空司龄天数时 NPE。

## 2026-06-12 员工高级查询增强

- `QueryEmployeePageListBO` 新增 `ageMin/ageMax/birthMonth/companyAgeMin/companyAgeMax/contractEndTime/contractSignCount/employmentNature/politicalStatus` 等查询字段。
- `HrmEmployeeMapper.xml#employeeListCondition` 追加高级筛选 SQL：
  - 年龄筛选优先使用 `hrm_employee.age`，为空时按 `date_of_birth` 或有效身份证号出生日期兜底计算；
  - 出生月份筛选兼容 `date_of_birth`、`birthday` 和身份证号月份；
  - 司龄按 `company_age_start_time` 回退 `entry_time` 计算整年区间；
  - 合同结束时间使用当前执行中合同别名 `f.end_time`，合同签订次数通过员工合同表计数；
  - 政治面貌通过 `hrm_employee_data` 与 `hrm_employee_field` 的自定义字段查询，兼容字段名 `politicsStatus`、中文名“政治面貌”和历史字段 ID。
- 转正日期 `becomeTime` 范围条件与入职日期、合同结束时间保持一致，仅在 `data.becomeTime != null and data.becomeTime.size == 2` 时拼接 SQL，避免前端清空日期范围后出现空集合 `[0]/[1]` 下标访问。
- `HrmEmployeeMapperSqlTest` 增加高级查询条件静态测试，防止后续改动漏删筛选条件。
- `HrmEmployeeMapperSqlTest` 同时覆盖转正日期范围必须首尾日期完整才生效，防止再次退回只判断非空。
- 合同签订次数统计（2026-06-12）：`HrmEmployeeMapper.xml#queryPageList` 返回 `(select count(1) from hrm_employee_contract fc where fc.employee_id = a.employee_id) as contractSignCount`；筛选条件 `contractSignCount` 继续使用同一子查询口径，确保展示值与筛选值一致。
- `HrmEmployeeMapperSqlTest` 增加列表返回 `contractSignCount` 与员工合同表计数口径断言。

## 2026-06-12 员工合同新增不覆盖与期限自动计算

- `HrmEmployeeContractController#addContract` 保存前清空请求体中的 `contractId`，确保添加合同永远走新增路径，避免前端残留旧合同主键时覆盖原合同。
- `HrmEmployeeContractController#setContract` 保持原编辑语义，仍使用请求体中的 `contractId` 更新指定合同。
- `HrmEmployeeContractServiceImpl#addOrUpdateContract` 保存前统一根据 `startTime/endTime` 重算 `term`，覆盖前端传入期限；当前 `term` 单位沿用前端展示口径“年”。
- 自动计算规则：按合同日期区间折算自然月并向上折算成年，最少 1 年；例如 `2024-01-01` 至 `2026-12-31` 计算为 3 年。
- 服务端增加基础校验：合同开始日期和结束日期不能为空，结束日期不能早于开始日期；不满足时抛出中文业务错误。
- 回归测试：`HrmEmployeeContractControllerTest#addContract_shouldIgnoreSubmittedContractId` 覆盖添加接口忽略旧主键；`HrmEmployeeContractServiceImplTest#addOrUpdateContract_shouldCalculateTermFromStartAndEndDates` 覆盖期限由日期自动计算并覆盖前端传值。

## 2026-06-12 组织部门编码自动生成

- 后端新增 `GenerateDeptCodeBO` 和 `/hrmDept/generateCode`，前端可在新增/编辑弹窗打开时传入可选 `deptId` 获取当前可用组织编码。
- `IHrmDeptService#generateCode` 与 `HrmDeptServiceImpl#generateCode` 负责扫描当前租户 `hrm_dept.code`，解析正整数编码并返回最小未使用正整数；编辑场景会排除当前部门 ID。
- `HrmDeptServiceImpl#addOrUpdate` 不再信任前端传入 `code`，保存前统一调用 `generateCode(AddDeptBO.deptId)` 重新写入编码，作为后端兜底防重。
- `AddDeptBO.code` 取消必填校验，新增/编辑仍要求部门名称、类型和上级组织；编码由后端生成后保存。
- 前端 `src/api/hrm/dept/dept.js` 增加 `generateDeptCode`；`Add.vue` 与 `Edit.vue` 初始化时调用该接口并把返回值填入只读组织编码框。
- 回归测试：`HrmDeptServiceImplTest` 覆盖最小可用编码、编辑排除当前部门和保存时覆盖陈旧编码；`dept-api.test.mjs` 覆盖前端 API 地址；`dept-code-auto-load.test.mjs` 覆盖新增/编辑弹窗自动加载编码。

## 2026-06-12 员工花名册字段比对

- 比对文件：`/Users/jiangyongming/Desktop/农谷导入数据/副本湖北田野员工花名册-2026年6月.xlsx`。
- 花名册读取：通过 `openpyxl` 读取可见工作表第 2 行字段，并使用第 1 行合并单元格作为分组；隐藏 `田野农谷 (2)` 第 2 行为员工数据，未纳入字段来源。
- 员工编辑字段读取：前端详情页选项卡来自 `hr_web/src/views/hrm/employee/Detail.vue`；个人/通讯/岗位动态字段通过只读接口 `/hrmEmployee/personalInformation/{employeeId}`、`/hrmEmployeePost/postInformation/{employeeId}` 获取；合同、工资社保、材料附件字段来自前端模型组件。
- 工资社保接口样本返回 `selectOne` 多记录异常，字段比对按前端兜底模型纳入工资卡、社保、薪资列表字段。
- 结果报告：`docs/employee-roster-field-comparison-2026-06-12.md`。

## 2026-06-12 员工花名册导入重构

- `HrmEmployeeController#importEmployee` 不再吞掉异常；导入失败时记录日志并返回 `Result.error(500, message)`。
- `HrmEmployeeServiceImpl#importEmployee` 已从旧固定列解析改为表头驱动解析：
  - 读取工作簿当前打开/激活的可见工作表，不再固定要求 `田野农谷` sheet；
  - 第 1 行维护当前字段分组，第 2 行维护字段名；
  - 第 3 行起为员工数据；
  - 使用 Apache POI `WorkbookFactory` + `DataFormatter` 读取 xlsx 值，兼容日期、数字手机号、公式等单元格。
- 工作表选择（2026-06-16）：
  - `findRosterImportSheet(...)` 优先读取 `Workbook#getActiveSheetIndex()` 指向的可见工作表，贴近“打开哪个 sheet 就导哪个”的业务口径；
  - 若激活工作表隐藏、不可用或索引异常，则回退第一个可见工作表；
  - 若工作簿不存在任何可见工作表，返回“导入文件未找到可读取的工作表”。
- 公式容错（2026-06-16）：
  - `getCellText(...)` 调用 POI 公式计算失败时不再中断导入；
  - 失败后回退 `formatCachedFormulaCellValue(...)` 读取缓存结果，缓存也不可用时返回空字符串，避免用户文件中的 `DATEDIF` 等未实现函数阻断员工主信息保存。
- 员工匹配使用 `employeeName + normalized mobile`：
  - `buildEmployeeImportUniqueMap(...)` 扫描未删除员工并检查系统内重复键；
  - 导入文件内用 `rowUniqueKeys` 阻止重复员工行；
  - 新员工设置在职、正式、未删除等默认值；已有员工走 `updateById`。
- 固定/子表字段导入：
  - 基本信息写入 `hrm_employee`，包含姓名、个人电话、工号、身份证号、籍贯、户籍地址、民族、职位、职级、工作地点、性别、入职日期、出生日期、年龄、工龄、部门；
  - 工作经历、教育经历、联系人、证书、培训、合同、工资卡、社保、离职信息按现有服务模型保存；对应服务不可用或模板值为空时跳过。
- 工号导入补充（2026-06-14）：
  - 当前 `/hrmEmployee/import` 表头驱动导入已通过 `HrmEmployeeServiceImpl#applyRosterEmployeeFields(...)` 将 `基本信息-工号` 映射到 `HrmEmployee.jobNumber`；新员工只有在导入值为空时才调用 `generateImportJobNumber()` 自动生成。
  - 旧 EasyExcel VO 链路 `EmployeeImportVO` 原先对 `jobNumber` 使用 `@ExcelIgnore`，会导致调用 `ImportDatas(...)` 时模板工号被忽略并回退为时间戳；现改为 `@ExcelProperty(value="工号", index=23)`，保留旧链路的工号导入能力。
  - 回归测试 `HrmEmployeeServiceImplImportEmployeeTest#employeeImportVO_shouldReadJobNumberColumn` 覆盖旧 VO 能读取工号列，`#importEmployee_shouldUseRosterJobNumberForNewEmployee` 覆盖当前表头导入能把 `基本信息-工号` 写入新员工 `jobNumber`，避免再次被忽略。
- 扩展字段导入：
  - `buildRosterFieldMap(...)` 扫描 `hrm_employee_field` 全部字段，并建立跨选项卡同名字段索引；
  - `ensureRosterDynamicFields(...)` 为模板中未被固定/子表承接且系统不存在的列新增文本型自定义字段；
  - 重复表头使用 `分组-字段名` 作为显示名，避免 `姓名/开始日期/结束日期/年龄` 等跨分组字段混淆；
  - `saveRosterDynamicData(...)` 仅替换当前员工本次涉及的扩展字段值。
- 回归测试：`HrmEmployeeServiceImplImportEmployeeTest` 覆盖同名不同手机号新增、姓名+手机号匹配更新、跨选项卡同名字段复用、重复表头按分组命名、唯一可见工作表兜底导入、多工作表时优先读取当前激活工作表，以及包含 `DATEDIF` 公式列时导入不中断。

## 2026-06-13 员工基本信息子女信息

- 后端新增 `EmployeeChildrenInfoFieldFactory`，统一定义个人信息扩展字段 `children_info`（显示名“子女信息”）及其明细子字段：`childName`（姓名）、`childSex`（性别）、`childBirthDate`（出生日期）。
- `HrmEmployeeServiceImpl#personalInformation(...)` 与 `personalArchives()` 在查询前调用 `ensureChildrenInfoField()`，当 `hrm_employee_field` 或 `hrm_field_extend` 缺少子女信息配置时自动补齐；主字段使用保留的小整数 `field_id` 起始值 `900001`，以兼容 `hrm_field_extend.parent_field_id` 的 `Integer` 存储。
- 子女信息复用既有 `detail_table` 动态字段能力，数据仍写入 `hrm_employee_data`，避免新增专用子女表和额外 CRUD 接口。
- 新增单元测试 `EmployeeChildrenInfoFieldFactoryTest`，覆盖主字段和三个明细子字段定义。

## 2026-06-13 子女信息字段线上配置修复

- 现场问题：前端提示“子女信息字段配置不存在，请刷新后重试”。接口排查确认远端 `153.0.237.99:8081` 当前登录公司 `companyId=0003` 的 `/hrmEmployee/personalInformation/{employeeId}` 未返回 `children_info`，即数据库字段配置未初始化。
- 处理：已对现有公司库 `hr_0001` 至 `hr_0005` 幂等补齐 `hrm_employee_field.children_info` 与 `hrm_field_extend` 三个子字段（姓名、性别、出生日期）。
- 验证：补齐后远端接口返回 `children_info`，`fieldId=900001`，`formType=detail_table`，明细子字段为 `childName/childSex/childBirthDate`。
- 补充脚本：`docs/sql/2026-06-13_employee_children_info_field.sql` 可用于后续新公司库或环境迁移时初始化同一字段配置。

## 2026-06-13 子女信息保存 JSON 解析修复

- 现场问题：保存子女信息时报 `Cannot deserialize instance of java.lang.String out of START_ARRAY token`，错误链路为 `UpdateInformationBO.dataList[].fieldValueDesc`。
- 根因：后端 `UpdateInformationBO.InformationFieldBO#fieldValueDesc` 类型为 `String`，前端子女信息提交时误将 `detail_table` 二维数组赋给该字段。
- 处理：前端已改为仅 `fieldValue` 保留明细数组，`fieldValueDesc` 使用 JSON 字符串，后端无需调整 BO 类型。

## 2026-06-13 组织编码生成接口 404 兼容

- 现场问题：组织管理打开新增/编辑弹窗时提示 `No handler found for POST /hrsystem/hrmDept/generateCode`。
- 排查结果：本地源码已有 `HrmDeptController#generateCode`，远端 `153.0.237.99:8081/api/hrsystem/hrmDept/generateCode` 带 token 请求仍返回 404，说明当前运行后端包未包含该接口映射。
- 后端补充回归：新增 `HrmDeptControllerTest`，校验 `POST /generateCode` 映射存在并委托 `IHrmDeptService#generateCode`。
- 前端兼容：`src/api/hrm/dept/dept.js#generateDeptCode` 优先请求后端接口；仅当响应为 `No handler found` 的 404 时，静默调用 `queryTreeList` 拉取组织树，递归扫描正整数 `code` 并返回最小未使用值，编辑时排除当前 `deptId`。
- 发布建议：后端仍需重新发布包含 `/hrmDept/generateCode` 的包，前端兜底仅用于接口缺失期间避免页面阻断。

## 2026-06-13 Maven clean package BOOT-INF/lib 路径修复

- 现场问题：执行 `mvn clean` 后再执行 `mvn package`，在 `maven-jar-plugin:3.1.2:jar` 阶段失败，错误路径为 `target/classes/BOOT-INF/lib`。
- 根因：`pom.xml` 的资源配置使用了 `src\main\resources\lib` 与 `BOOT-INF\lib`。在 macOS/Linux 下，反斜杠会被当作普通文件名字符，资源插件实际生成 `target/classes/BOOT-INF\lib`，而 JAR 插件查找的是 `target/classes/BOOT-INF/lib`，clean 后正确目录不存在而触发 `NoSuchFileException`。
- 修复：将 Maven 资源目录与目标路径统一改为跨平台写法 `src/main/resources/lib` 和 `BOOT-INF/lib`，保留本地短信 SDK JAR 进入 Spring Boot 包内 `BOOT-INF/lib` 的行为。
- 验证：`mvn clean package -DskipTests` 通过；资源阶段日志为 `Copying 3 resources to BOOT-INF/lib`，产物 `target/hrsystem-0.0.1-SNAPSHOT.jar` 包含 `BOOT-INF/lib/alicom-mns-receive-sdk-1.1.3.jar`、`BOOT-INF/lib/aliyun-sdk-mns-1.1.9.1.jar`、`BOOT-INF/lib/aliyun-java-sdk-dybaseapi-1.0.1.jar`。
- 注意：Maven settings 中 `profiles` 标签位置、mirror id，以及 POM 中重复依赖/systemPath 的 warning 为既有警告，本次未扩大处理。

## 2026-06-13 员工离职状态列表显示与筛选

- 后端员工列表链路仍为 `HrmEmployeeController#queryPageList` -> `HrmEmployeeServiceImpl#queryPageList` -> `HrmEmployeeMapper#queryPageList`。
- `QueryEmployeePageListBO` 新增 `quitTime` 日期范围字段；`HrmEmployeeMapper.xml` 左关联 `hrm_employee_quit_info`，返回 `date_format(q.plan_quit_time, '%Y-%m-%d') as quitTime` 和 `q.quit_reason as quitReason`，并在 `quitTime` 首尾日期完整时用 `q.plan_quit_time between ...` 筛选。
- `HrmEmployeeMapperSqlTest` 增加离职信息表关联、`quitTime/quitReason` 返回字段和 `plan_quit_time` 筛选静态断言，防止后续改动漏删离职显示支持。

## 2026-06-14 员工基础信息模板导出

- 后端新增 `/hrmEmployee/exportBasicInfoTemplate`，参数沿用 `QueryEmployeePageListBO`，导出前设置 `pageType=0/limit=10000` 并调用 `HrmEmployeeServiceImpl#queryPageList(...)`，复用员工列表筛选、数据权限、身份证生日兜底和司龄兜底逻辑。
- 模板资源存放在 `src/main/resources/export/副本人资系统导出员工基础信息模版.xlsx`，来源为业务提供的 `/Users/jiangyongming/Desktop/农谷导入数据/副本人资系统导出员工基础信息模版.xlsx`。
- `EmployeeBasicInfoExportSupport` 统一维护模板表头和字段映射：
  - `hrm_employee` / `hrm_dept` / `hrm_employee_contract` 提供基础、部门、岗位、学历和合同信息；“劳动/劳务合同期限”输出最近一次合同的开始时间-结束时间，“开始时间”输出第一次合同开始时间，“结束时间”输出最后一次合同结束时间；
  - `hrm_employee_contract` 总数提供“劳动合同次数”，与员工管理列表 `contractSignCount` 口径保持一致；
  - `hrm_employee_contacts` 第一条联系人记录提供“紧急联系人/与本人关系/电话号码”；
  - `hrm_employee_data` 联合 `hrm_employee_field.name/field_name` 提供“现居地址/政治面貌/薪资等级”等动态字段；
  - 用工性质按 `status=5` 或当前合同 `contract_type in (5,7)` 输出“劳务用工”，否则输出“劳动用工”，若动态字段“用工性质”有值则优先展示动态字段值。
- `HrmEmployeeMapper.xml#queryPageList` 增补 `emergencyContact/emergencyRelation/emergencyPhone` 三个联系人字段；`HrmEmployeeDataMapper.xml#queryFiledListByEmployeeId` 增补动态字段中文名 `name`，供导出按模板中文表头匹配。
- 回归测试：`EmployeeBasicInfoExportSupportTest` 覆盖 24 个模板表头和字段值映射；`HrmEmployeeMapperSqlTest` 覆盖联系人字段 SQL 来源。
- 2026-06-16 排查导出失败：远端 `POST /hrsystem/hrmEmployee/exportBasicInfoTemplate` 返回 404，旧 `target/hrsystem-0.0.1-SNAPSHOT.jar` 构建于 2026-06-13，未包含 2026-06-14 新增接口和中文模板资源；已重新执行 `mvn -DskipTests package`，新 JAR 包含 `BOOT-INF/classes/export/副本人资系统导出员工基础信息模版.xlsx` 与 `HrmEmployeeController#exportBasicInfoTemplate`。
- 2026-06-16 修复导出四列缺值：`HrmEmployeeDataMapper.xml#queryFiledListByEmployeeId` 改为显式返回 `fieldName/fieldValue/fieldValueDesc` 驼峰别名；`HrmEmployeeServiceImpl` 读取动态字段时兼容蛇形 key，并用 `field_value` 兜底空 `field_value_desc`；`HrmEmployeeMapper.xml#queryPageList` 的 `highestEducation` 兜底读取 `hrm_employee_education_experience`；`EmployeeBasicInfoExportSupport` 为“薪资等级/学历”增加“薪资类别/职务级别/最高学历”等历史花名册字段兜底。
- 2026-06-16 调整导出合同列口径：`HrmEmployeeMapper.xml#queryPageList` 新增 `firstContractStartTime/latestContractStartTime/latestContractEndTime/lastContractEndTime`，其中最近一次合同按 `start_time desc, contract_id desc` 取数；`EmployeeBasicInfoExportSupport` 优先使用这些别名输出合同期限、首次开始和最后结束，旧 `startTime/endTime/term` 仅作为兼容兜底。
## 2026-06-16 员工合同导入

- 后端新增 `IHrmEmployeeContractService#importContracts(MultipartFile)` 和 `HrmEmployeeContractServiceImpl#importContracts(...)`，使用 Apache POI 读取当前激活的可见工作表，首行为表头，第二行起为合同数据。
- 导入员工匹配在合同服务内构建 `employeeName + normalized mobile` 索引；`listContractImportCandidates()` 仅读取未删除员工，系统内重复键会抛出中文业务错误，避免同名或重复手机号数据错写。
- 合同字段解析规则：`合同类型` 兼容中文枚举名、数字编码和“1、固定期限劳动合同”类文本；`合同状态` 兼容中文枚举名、数字编码和组合文本；日期兼容 Excel 日期、`YYYY-MM-DD`、`YYYY/MM/DD`、`YYYY.MM.DD`、`YYYYMMDD`、带时分秒文本。
- 每个非空数据行构造一条 `HrmEmployeeContract`，显式清空 `contractId` 后调用既有 `addOrUpdateContract(...)`，继续复用期限自动计算和新增行为记录逻辑；导入接口在任一行失败时通过事务整体回滚。
- `HrmEmployeeContractController` 新增 `/hrmEmployeeContract/import` 上传入口，返回导入条数；异常时记录“员工合同导入失败”日志并返回 `Result.error(500, message)`。
- 回归测试：`HrmEmployeeContractServiceImplTest#importContracts_shouldAddOneContractPerRowAndMatchEmployeeByNameAndPhone` 覆盖一人多合同、同名员工按电话匹配、合同类型/状态解析和新增主键清空。
- 2026-06-22 修复合同导入日期提示：`HrmEmployeeContractServiceImpl#getRequiredImportDate(...)` 专用于必填的合同开始/结束日期，先判断单元格文本是否为空，再复用 `getImportDate(...)` 解析；解析失败时返回“第X行合同结束日期无效：原始值”，避免将有内容但无效的日期误报为“开始日期和结束日期不能为空”。回归测试：`HrmEmployeeContractServiceImplTest#importContracts_shouldReportInvalidRequiredDateWithColumnNameAndOriginalValue` 覆盖第40行 `2027-4-31` 场景，`importContracts_shouldReportInvalidEightDigitRequiredDateWithColumnNameAndOriginalValue` 覆盖 `20270431` 场景。

## 2026-06-16 员工管理固定模板下载

- `HrmEmployeeController` 新增 `GET /hrmEmployee/downloadEmployeeRosterTemplate`，从 classpath `export/employee_module.xlsx` 读取员工花名册固定模版并按 Excel Blob 输出。
- `HrmEmployeeContractController` 新增 `GET /hrmEmployeeContract/downloadContractTemplate`，从 classpath `export/hetong_module.xlsx` 读取合同固定模版并按 Excel Blob 输出。
- 两个接口均设置 `Content-Disposition`、Excel content type 和 `fileDownload=true` cookie；资源缺失时返回 404，避免前端下载空文件。
- 回归测试：`HrmEmployeeControllerTest#downloadEmployeeRosterTemplate_shouldWriteFixedRosterWorkbook` 与 `HrmEmployeeContractControllerTest#downloadContractTemplate_shouldWriteFixedContractWorkbook` 覆盖响应头和输出字节与资源文件一致。

## Recent Changes and Decisions (2026-06-16, 权限管理与登录用户创建)
- 新增权限规则辅助类 `MenuPermissionSupport`：负责角色菜单归一化、自动补父模块、校验角色至少有一个启用页面权限，以及构建登录返回菜单树；顶级菜单若没有子菜单也按可访问页面处理。
- 新增接口权限映射类 `ApiPermissionPathSupport`，由 `CompanyInterceptor` 在 token 解析后校验当前账号是否具备接口对应菜单权限。
- `LoginController` 登录成功前会加载角色菜单，校验账号角色已配置权限，并把 `rolemenu/menuTree` 写入 token 和响应。
- 2026-06-17 登录 SQLGrammarException 修复：登录接口在 `CompanyInterceptor` 跳过列表中，请求线程没有 token 派生的 `CompanyContext`；因此 `LoginController#fillPermissionMenus` 加载 `tbrolemenu/tbmenu` 期间需临时设置当前登录用户到 `CompanyContext`，确保 JPA 多租户查询落到 `hr_${companyId}` 而不是默认总库 `hrsystem`。
- 2026-06-17 现场库结构修复：`hr_0003.tbrolemenu.role_menu_id` 为 `bigint` 雪花 ID，开发库示例值已达到 `1984881405614632960`；JPA 实体 `model.tbrolemenu` 与 `rolemenuRepository` 的主键类型已改为 `Long`，与 MyBatis-Plus 实体 `modules/menu/entity/TbRoleMenu` 保持一致。
- `TbRoleMenuService#saveRoleMenuList` 保存角色权限时会过滤停用菜单、自动补父级菜单，并阻止空权限角色。
- `TbRoleTypes.canUse` 显式映射到历史驼峰列 `canUse`，避免 MyBatis-Plus 默认生成 `can_use` 导致角色/登录用户保存时报 “Unknown column 'can_use'”。
- `TbLoginUserService#Add` 创建/编辑登录用户时按登录链路涉及库表保存：系统库账号索引使用配置项 `${hrm.system.database}.tbAllUserList`，租户库用户主数据使用当前租户 `tbloginuser`。
- 登录用户保存前会校验账号、密码、部门、启用角色和角色权限；其中部门必须存在于 `hrm_dept`，角色必须存在于 `tbroletypes` 且未停用，角色权限必须至少包含一个可访问子菜单，避免保存成功后被 `view_LoginUser` 或登录菜单链路过滤。
- `tbloginuser.id` 映射为数据库自增主键；系统库账号索引会阻止跨公司同账号抢占，编辑账号时会删除当前公司旧账号索引并写入新账号索引，确保登录时按账号能定位到正确 `CompanyID`。
- 系统库账号索引操作必须暂停外层租户事务并临时清空 `CompanyContext` 后执行独立事务，确保 `DynamicDataSource` 选择 `Default` 数据源，而不是当前登录用户所在租户库。
- `TbRoleTypesService#saveRoleType` 新增角色保存能力，供前端系统管理页创建角色后再分配菜单权限。
- 新增菜单种子 SQL：`docs/sql/2026-06-16_menu_permission_seed.sql`，按现场 `tbmenu` 既有 22 条且 `path` 全部为 `NULL` 的数据形态先补齐 `path`，保留历史 `tbrolemenu.menu_id` 权限关系，只新增系统管理和缺失子菜单，不清空/重建菜单表；系统管理权限需确认管理员角色后手动补授，不自动给所有角色扩权。
- 2026-06-17 右上角齿轮系统设置权限：菜单种子 SQL 新增 `/manage`“系统设置”模块及 `/manage/insurance-scheme`、`/manage/vacation`、`/manage/attendance`、`/manage/salary` 四个设置子菜单；脚本只建菜单，不自动给所有角色扩权。
- 2026-06-17 接口权限映射支持多菜单路径：`ApiPermissionPathSupport#resolveRequiredMenuPaths` 对系统设置与业务页共用的接口返回多个可授权路径，`CompanyInterceptor` 改为任一路径命中即可访问；例如考勤规则查询可由 `/hrm/attendance/scheduling` 或 `/manage/attendance` 授权，社保方案查询可由 `/hrm/insurance-scheme/index` 或 `/manage/insurance-scheme` 授权。
- 验证：`mvn -Dtest=ApiPermissionPathSupportTest,MenuPermissionSupportTest,LoginControllerTest test` 通过，12 个测试 0 失败；`node tests/permission-utils.test.mjs` 通过；`npm run build` 通过，保留既有 `::v-deep` 与 chunk size warning。


## 2026-06-17 系统设置权限可见性修复

- 根因一：`hr_0003` 中 `hbadmin` 所属角色 `roleId=2` 只授权了 `/hrm/system` 和 `/manage` 父级菜单，未授权任何对应子菜单；`MenuPermissionSupport#buildAuthorizedMenuTree` 对有子菜单的父模块只在存在已授权子菜单时返回，因此重新登录后菜单和齿轮均被隐藏。
- 根因二：`TbMenuService#queryMenuList` 使用 `x.getPid() == f.getId()` 比较 `Integer`，当新增菜单 ID 为 `1000/1010` 时父子匹配失败，角色权限页菜单树只显示父节点、不显示系统管理和系统设置子菜单。
- 修复：`TbMenuService#queryMenuList` 改用 `Objects.equals(x.getPid(), f.getId())` 进行值比较；`MenuPermissionSupport#normalizeRoleMenus` 新增父级菜单单独授权校验，防止保存“只有父节点、没有任何可访问子页面”的无效权限。
- 数据修复：新增 `docs/sql/2026-06-17_grant_system_settings_admin.sql`，按 `hbadmin` 所属角色补授 `/hrm/system/*` 与 `/manage/*` 子菜单；已在 `hr_0003` 执行，`roleId=2` 现包含 `1001/1002/1003/1011/1012/1013/1014`。
- 部署注意：数据库补授权后重新登录即可恢复右上角齿轮和系统管理入口；角色权限页要展示 1000 以上菜单的子节点，需要发布包含 `TbMenuService` 修复的后端代码。


## 2026-06-17 假期扣减请求失败修复

- 现象：齿轮权限恢复后，进入系统设置-假期管理相关页面时 `/hrmHolidayDeduction/queryHolidayDeductionList` 返回 500，错误为 `Error attempting to get column 'deduction_id' ... NumberFormatException: For input string: "0a4be..."`。
- 根因：`hr_0003.hrm_holiday_deduction.deduction_id` 为 `varchar(64)`，现场已有 45 条非数字 UUID 主键；`HrmHolidayDeduction`、`UpdateHolidayDeductionBO`、`QueryHolidayDeductionVO` 中 `deductionId` 定义为 `Long`，MyBatis 查询 VO 时按数字解析失败。
- 修复：将上述三处 `deductionId` 改为 `String`，兼容 UUID 历史数据与后续字符串主键返回。
- 验证：`mvn -Dtest=HrmHolidayDeductionIdTypeTest,ApiPermissionPathSupportTest test` 通过，5 个测试 0 失败；临时 9083 服务调用 `/hrmHolidayDeduction/queryHolidayDeductionList` 返回 `code=200`，首条 `deductionId` 为字符串 UUID。

## 2026-06-17 远端请求失败发布链路确认

- 本机 `127.0.0.1:9080` 已运行新代码，登录、`/tbMenu/queryMenuList`、`/tbRoleMenu/getRoleMenu`、`/hrmHolidayDeduction/queryHolidayDeductionList`、`/hrmRemainingVaction/queryRemainingVacationList`、`/hrmAttendanceRule/queryAttendanceRulePageList`、`/hrmSalaryBasic/findAll`、`/hrmInsuranceScheme/index` 均返回正常。
- 公网 `153.0.237.99:8081/api/hrsystem/hrmHolidayDeduction/queryHolidayDeductionList` 仍返回 `deduction_id` UUID 转 Long 的旧错误，证明公网 nginx 代理的远端 `153.0.237.99:9080` 后端尚未替换为本次修复包。
- 已生成新后端包：`target/hrsystem-0.0.1-SNAPSHOT.jar`，大小约 110MB，包含 `HrmHolidayDeduction`、`QueryHolidayDeductionVO`、`UpdateHolidayDeductionBO` 的 `deductionId=String` 修复。
- 远端 22 端口 SSH 检测超时，本机会话无法直接上传/重启远端服务；需要在远端手动替换运行 JAR 并重启 9080 后端。

## 2026-06-17 Tomcat 请求头过大导致员工页 400

- 现象：浏览器访问 `http://127.0.0.1:8081/#/hrm/employee` 时，`queryPageList`、`queryEmployeeStatusNum`、`hrmInsuranceScheme/index`、`hrmDept/queryTreeList` 均返回 HTTP 400；curl 少量请求头访问同接口可返回 200。
- 根因：登录 token 长度约 7935 字符，浏览器额外携带 `sec-ch-ua`、`User-Agent`、`Referer` 等请求头后超过 Tomcat 默认 8KB 请求头限制，请求在进入 Spring Controller 前被拒绝。
- 修复：`application-dev.properties`、`application-prod.properties`、`application-temp.properties` 增加 `server.max-http-header-size=65536`。
- 新增 `TomcatHeaderSizeConfigTest`，校验所有 profile 均配置至少 64KB 请求头上限。
- 验证：`mvn -Dtest=TomcatHeaderSizeConfigTest test` 通过；真实 Chrome headless 打开 `/hrm/employee` 抓包确认 4 条首屏 API 均返回 HTTP 200、`badCount=0`。

## 2026-06-17 登录用户默认部门与删除

- `TbLoginUserService#validateLoginUser` 新增缺省部门处理：新建登录用户且 `QueryLoginUserBO.depid` 为空时，通过 `HrmDeptMapper.selectList` 查询当前租户 `hrm_dept.parent_id=0` 的最顶级部门，按 `dept_id` 升序取第一条并回填到 BO；编辑场景仍保留“必须选择部门”的校验。
- `TbLoginUserService#Delete` 新增删除登录用户能力：先在当前租户 `tbloginuser` 查询并删除用户，再通过 `runWithDefaultCompanyContext` 临时清空 `CompanyContext`，使用默认数据源删除 `${hrm.system.database}.tbAllUserList` 中当前公司与账号的索引记录。
- `TbLoginUserController` 新增 `POST /tbLoginUser/Delete/{id}`，返回结构与原 `/Add` 一致，便于前端在登录用户列表中调用删除操作。
- 回归测试：`TbLoginUserServiceValidationTest` 覆盖默认顶级部门、删除租户用户和系统账号索引、默认数据源上下文恢复；`TbLoginUserControllerTest` 覆盖删除路由和服务调用。

## Recent Changes and Decisions (2026-06-17, 单双休月度日历配置)
- 后端新增“月度每日上班/休息设置”能力：
  - 新实体：`modules/workweek/entity/HrmWorkweekDaySetting`，对应表 `hrm_workweek_day_setting`；
  - 新仓库：`hrmWorkweekDaySettingRepository`，支持按年份和日期范围查询；历史按月删除方法保留但月历保存流程不再依赖；
  - 新 BO/VO：`QueryWorkweekMonthCalendarBO`、`SaveWorkweekMonthCalendarBO`、`WorkweekDaySettingBO`、`WorkweekMonthCalendarVO`、`WorkweekDayCalendarVO`。
- `HrmWorkweekSettingService` 新增：
  - `queryMonthCalendar`：按年度周休规则、法定节假日/调休和已保存每日设置生成月份日历；
  - `saveMonthCalendar`：只处理前端提交的日期，先查询当月已有记录，再按 `setting_year + work_date` 复用主键增量更新或新增；
  - 年度月度概览统计复用相同每日判定逻辑，保存后的结果会影响概览上班/休息天数。
- `HrmWorkweekSettingController` 新增接口：
  - `POST /hrmWorkweekSetting/queryMonthCalendar`；
  - `POST /hrmWorkweekSetting/saveMonthCalendar`。
- 新增建表脚本：
  - `docs/sql/2026-06-17_hrm_workweek_day_setting.sql`；
  - `docs/sql/2026-06-17_hrm_workweek_day_setting_hr_0001_to_hr_0005.sql`。
- 验证：`mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false test` 通过，10 个测试 0 失败。

## Recent Changes and Decisions (2026-06-17, 单双休月度日历节日名称展示)
- `WorkweekDayCalendarVO` 新增 `holidayName` 字段，用于在月度日历中展示“元旦/春节/清明节/劳动节/端午节/中秋节/国庆节”等节日名称。
- `HrmWorkweekSettingService` 在构建每日月历时补充 `holidayName`：
  - 优先使用 2026 年国务院放假安排对应日期映射；
  - 其他年份在 `hrm_attendance_legal_holidays` 有法定休息/调休上班记录时，按月份区间进行节日名称兜底推断；
  - 手动设置日期仍保留节日名称，只把来源改为“手动设置”。
- 回归测试新增 `queryMonthCalendar_shouldExposeHolidayNameForLegalHolidayDates`，覆盖劳动节法定休息日和调休上班日均返回 `holidayName=劳动节`。


## Recent Changes and Decisions (2026-06-17, 单双休月度日历增量保存修复)
- 现场问题：保存月度日历时报 `uk_workweek_day_year_date` 唯一键冲突；且用户只修改 5 月 31 日时，前端提交整月导致该月所有日期都被保存为手动结果。
- 后端 `HrmWorkweekSettingService#saveMonthCalendar` 改为增量 upsert：
  - 查询当月已有 `hrm_workweek_day_setting` 记录并按日期建索引；
  - 只为请求体 `days` 中提交的日期生成待保存记录；
  - 已存在日期复用原 `daySettingId` 执行更新，新日期生成新 ID；
  - 不再先删除整月数据，从根因上避免 Hibernate 删除/插入 flush 顺序引发唯一键冲突。
- 回归测试新增 `saveMonthCalendar_shouldUpsertOnlySubmittedDaysWithoutDeletingWholeMonth`，并将整月提交场景同步调整为“不删除、只 upsert 已提交日期”。

## Recent Changes and Decisions (2026-06-17, 单双休月度概览统计显示)
- 前端月度概览卡片的统计标签从“法定休 / 调休上班”明确调整为“法定休天数 / 调休上班天数”，对应后端 `WorkweekMonthSummaryVO.legalHolidayRestDays` 与 `adjustedWorkDays`。
- 本次未修改后端统计口径；年度查询接口已返回上述两个字段，页面继续使用 `monthSummaries` 作为月度概览数据源。
- 前端回归测试收紧为只在 `month-stats` 月度概览块内断言这两个字段，避免仅弹窗显示而概览缺失。
- 验证：`node tests/workweek-page.test.mjs` 通过；`npm run build` 通过，仍仅有既有 `::v-deep` 和 chunk size 警告。

## Recent Changes and Decisions (2026-06-17, 2026 法定节假日兜底统计)
- 现场排查确认 `hr_0001` 至 `hr_0005` 的 `hrm_attendance_legal_holidays` 均为空，导致年度接口 `totalLegalHolidayRestDays` 和 `legalHolidayRestDays` 返回 0。
- `HrmWorkweekSettingService#queryHolidayOverrides` 调整为：当前年份数据库节假日记录为空时，使用内置 2026 放假调休表；若数据库存在记录，则完全以数据库为准。
- 内置 2026 兜底包含 33 天法定休息日、6 天调休上班日；仍保留已保存日级设置优先级，高于调休上班、法定休息和周休规则。
- 新增回归测试 `queryYearSettings_shouldUseBuiltIn2026HolidayScheduleWhenHolidayTableIsEmpty`，覆盖空表时 2026 年汇总、1 月、2 月、5 月、9 月、10 月统计。
- 验证：`mvn -Dtest=HrmWorkweekSettingServiceTest -DfailIfNoTests=false test` 通过，11 个测试 0 失败；`node tests/workweek-page.test.mjs` 通过。


## Recent Changes and Decisions (2026-06-18, 奖金中心双表只计税上传)

- `src/main/java/com/tianye/hrsystem/modules/bonus/entity/HrmBonusTaxOnly.java` 与 `HrmBonusTaxOnlyMapper` 新增 `hrm_bonus_tax_only` 只计税奖金映射。
- `HrmBonusService` 复用现有奖金 Excel 解析规则，分别提供 `resolveBonusData(...)` 与 `resolveTaxOnlyBonusData(...)`，两者按年月各自清理 `hrm_bonus` / `hrm_bonus_tax_only` 后入库。
- `HrmBonusController` 新增 `POST /hrmBonus/importTaxOnlyBonus`，原 `POST /hrmBonus/importBonus` 保持为发放奖金上传。
- `HrmBonusMapper.xml#queryHrmBonusList` 改为基于 `hrm_bonus` 与 `hrm_bonus_tax_only` 的员工年月 union 查询，避免只上传只计税奖金时列表查不到记录。
- `SalaryComputeServiceNew` 新增 `getTaxOnlyBonusSalary(...)` 和 `calculateCumulativeIncome(...)`：普通公司累计收入包含发放奖金与只计税奖金，成都公司继续排除发放奖金但包含只计税奖金。
- SQL 脚本：`docs/sql/2026-06-18_hrm_bonus_tax_only.sql` 创建只计税奖金表及员工年月索引。
- 验证：`mvn -Dtest=SalaryComputeServiceNewTest -DfailIfNoTests=false test` 通过，覆盖只计税奖金进入累计收入且不改变成都发放奖金特殊口径。


## Recent Changes and Decisions (2026-06-18, 奖金中心菜单拆分)

- 后端接口权限从旧 `/hrm/bonus/index` 拆分为：`/hrmBonus/importBonus -> /hrm/bonus/payroll`、`/hrmBonus/importTaxOnlyBonus -> /hrm/bonus/taxOnly`，奖金列表查询允许两个子菜单访问。
- 菜单 SQL 将 `tbmenu.id=600` 改为“上传奖金(累加至工资计税)”并新增“上传奖金(只计税)”子菜单，同时给已拥有原奖金菜单权限的角色补授新子菜单。
- 多租户菜单已同步修复：`hr_0001` 至 `hr_0005` 的 `60` 均重定向到 `/hrm/bonus/payroll`，`600` 为发放奖金菜单，`601` 为只计税菜单；已给原拥有 `600` 的角色补授 `601`。
- 验证：`ApiPermissionPathSupportTest`、`MenuPermissionSupportTest` 通过；登录返回的 `menuTree` 中“奖金中心”已有两个子菜单；`hr_0001` 至 `hr_0005` 均存在 `hrm_bonus_tax_only` 表。

## Recent Changes and Decisions (2026-06-18, 薪资核算只计税奖金复算口径)

- `SalaryMonthRecordServiceNew#calculateMidMonthPromotionSummary(...)` 新增 `taxOnlyBonusSalary` 参数，累计收入在发放奖金、福利计税收入之外显式加入只计税奖金。
- `SalaryMonthRecordServiceNew#computeEmployeeSalary(...)` 和半路转正一致性复算会按员工、年月读取 `HrmBonusTaxOnlyMapper#getEmpTaxOnlyBonus(...)`，确保 `hrm_bonus_tax_only` 进入个税累计收入。
- 只计税奖金不写入 `41001`，不直接改写 `210101` 应发工资；`240101` 实发工资仅因重新计算出的个人所得税变化而减少。
- 回归测试：`SalaryMonthRecordServiceNewTest#calculateMidMonthPromotionSummary_shouldTaxTaxOnlyBonusWithoutAddingPayAmounts` 覆盖只计税奖金增加累计收入和个税、不增加应发工资的口径。
- 验证：`mvn -Dtest=SalaryMonthRecordServiceNewTest,SalaryComputeServiceNewTest -DfailIfNoTests=false test` 通过，27 个测试 0 失败。

## Recent Changes and Decisions (2026-06-18, 社保详情与加班夜班总览权限)

- 前端路由权限采用 `meta.checkPath` 继承父菜单授权：内部页面不新增可见菜单，只复用业务入口菜单权限。
- 社保详情 `/hrm/insurance-scheme/detail` 继承 `/hrm/insurance-scheme/index`，对应列表“查看详情”入口。
- 加班/夜班每日明细 `/overtimeNightDaily` 继承 `/hrm/attendance/overtimeNight`，对应统计页“显示所有/查看所有”入口。
- 后端 `ApiPermissionPathSupport` 现有映射已覆盖本次详情页调用的 `/hrmInsuranceMonthRecord`、`/hrmInsuranceMonthEmpRecord` 与 `/hrmOvertimeNightStatistics` 接口，无需新增特殊放行。
- 验证：`node tests/permission-utils.test.mjs`、`node tests/overtime-night-page.test.mjs`、`npm run build` 通过；构建仅保留既有 `::v-deep` 过时警告和大 chunk warning。

## Recent Changes and Decisions (2026-06-18, 多租户内部页面权限补授权)

- 新增 SQL 脚本 `docs/sql/2026-06-18_grant_internal_page_permissions_hr_0001_to_hr_0005.sql`，用于同步修复 `hr_0001` 至 `hr_0005` 的社保详情与加班/夜班每日明细权限。
- 补权策略：拥有 `/hrm/insurance-scheme` 父菜单的角色补授 `/hrm/insurance-scheme/index`；拥有 `/hrm/attendance` 父菜单的“管理员/系统管理员”角色补授 `/hrm/attendance/overtimeNight`。
- 已在现场库执行该脚本；复核结果显示目标规则下 `remaining_missing=(none)`。
- 公网前端 `http://153.0.237.99:8081/` 当前仍是旧入口包，未包含本次 `checkPath` 修复；已重新执行 `npm run package` 生成 `hr_web/release/nginx-1.25.4.zip`，包内 `index-HDO8QZtu.js` 已包含社保详情与加班/夜班每日明细的 `checkPath` 修复。
- 发布注意：将 `hr_web/release/nginx-1.25.4.zip` 部署到公网 nginx 后，用户需退出重新登录或清理旧 `localStorage.user_info`。

## Recent Changes and Decisions (2026-06-18, 社保管理高级筛选)

- `QueryInsuranceRecordListBO` 新增 `times`、`deptIds`、`employeeIds`、`startPeriod`、`endPeriod`，查询前通过 `normalizeFilters()` 统一解析月份范围与筛选范围。
- `HrmInsuranceMonthRecordService#queryInsuranceRecordList` 在 BO 为空或未传时间段时兜底当前年份 1-12 月，旧 `year` 参数仍按全年范围兼容。
- `HrmInsuranceMonthRecordMapper.xml#queryInsuranceRecordList` 增加 `(a.year * 100 + a.month)` 时间段过滤，以及 `c.dept_id`、`c.employee_id` 范围过滤，使人数与金额按命中员工聚合。
- 回归测试：`HrmInsuranceMonthRecordServiceTest` 覆盖默认当前年和高级筛选参数归一化；`HrmInsuranceMonthRecordMapperXmlTest` 覆盖 mapper SQL 条件存在性。
- 验证：`mvn -Dtest=HrmInsuranceMonthRecordServiceTest,HrmInsuranceMonthRecordMapperXmlTest test` 通过，3 个测试 0 失败。

## Recent Changes and Decisions (2026-06-19, 社保筛选与部门批量参保)

- 前端社保列表高级筛选删除人员筛选项，`insurance-filter-utils.js` 不再向主列表接口提交 `employeeIds`；后端 BO 暂保留 `employeeIds` 兼容历史调用。
- 员工管理批量参保弹窗新增“选择方式”：`按员工` 继续使用员工穿梭框，`按部门` 使用部门多选。
- 新增前端工具 `update-scheme-dialog-utils.js`，将部门选择展开为员工 ID 列表，并全程以字符串保留员工/部门 ID。
- 部门模式仍复用 `/hrmEmployee/updateInsuranceScheme`，请求体保持 `employeeIds + schemeId`，不新增后端接口。

## Recent Changes and Decisions (2026-07-12, 加班/夜班统计出勤天数字段库表补齐)

- 现象：访问加班/夜班统计明细时报 `Unknown column 'hrmovertim0_.actual_attendance_days' in 'field list'`。
- 根因：`HrmOvertimeNightStatisticsDetail` 已映射 `expected_attendance_days` / `actual_attendance_days`，且 `spring.jpa.properties.hibernate.hbm2ddl.auto=none`，但 dev MySQL `hr_0001` 至 `hr_0005` 的 `hrm_overtime_night_statistics_detail` 尚未执行 `docs/sql/2026-07-12_overtime_night_attendance_days.sql`。
- 处理：已在可达 dev MySQL `153.0.237.98` 上执行该幂等脚本，五个租户库均补齐两列。
- 验证：
  - `information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 的 `expected_attendance_days` 与 `actual_attendance_days` 均存在；
  - 本地 `127.0.0.1:9080` 登录 `hbadmin` 后调用 `POST /hrsystem/hrmOvertimeNightStatistics/queryDailyDetailPageList`，`month=2026-03` 返回 `code=200` 和 5 条明细，不再触发缺列异常。
- 限制：`application-prod.properties` 中的私有 MySQL `192.168.0.26:3306` 本机不可达；若公网后端使用该库，仍需在可访问线上库的机器上执行同一脚本。

## Recent Changes and Decisions (2026-07-13, 审批数据统计参与状态)

- 吴晓霞 `hr_0003` 本地数据复核：
  - 员工 `1831601326890434565`，工号 `TYNG-110`，钉钉 `userId=262756571521566047`；
  - 本地 `2026-06` 加班审批快照为 3 条，其中 `NzMSp1MiT5GkPu20k2IsPA04041781527092` 是 `2026-06-15 06:05:00 -> 07:05:00`；
  - 这 3 条快照均来自 `2026-07-11 15:58:59` 左右的同一次审批抓取。
- 后续使用 `CompanyID=0003` 的 `hrsystem.ddAccount` 钉钉应用只读复核：
  - `process.template.manage.get` 使用管理员 `296842114330963873` 返回 `400023/用户不存在`，这是管理员模板预查询失败，不是吴晓霞用户不存在；
  - 回退 `process/listbyuserid` 使用吴晓霞 `262756571521566047` 正常返回 `加班审批` 流程编码；
  - `processinstance/listids` 按 `2026-06` 发起时间返回 4 个加班实例：2 个 `COMPLETED/agree`，1 个 `COMPLETED/refuse`，1 个 `TERMINATED`；
  - 本地异常 `06:05 -> 07:05` 来自 `COMPLETED/refuse` 实例 `NzMSp1MiT5GkPu20k2IsPA04041781527092`，该实例表单中 `加班日期=2026-06-15 18:00`、`结束日期=2026-06-15 07:05`、`预计加班时长=1`，解析修复逻辑按结束时间倒推为 `06:05 -> 07:05`。
- 审批抓取新增状态过滤：`processinstance/get` 后仅保存 `status=COMPLETED` 且 `result=agree` 的审批实例；`refuse`、`TERMINATED`、取消、撤销、进行中等非同意完成实例直接跳过入库，也不加入 retained IDs，后续陈旧快照清理会删除历史残留。`预计加班时长` 是表单字段，不是通过状态；即使该字段存在，只要审批结果不是 `agree` 仍会跳过。
- 管理员模板回退日志补充说明 `adminUserId` 是钉钉管理员而不是当前员工，降低单选员工时误判“员工用户不存在”的联调成本。
- 本地运行验证：通过 `http://127.0.0.1:8081/api` 使用 payload `{"approvalTypes":["all"],"employeeIds":["1831601326890434565"],"month":"2026-06"}` 重抓后返回 `insertedCount=3`；`hr_0003.tbattendanceapprove` 中吴晓霞 `2026-06` 只保留 2 条同意加班和 1 条调休，拒绝实例 `06:05 -> 07:05` 已被清理。
- `tbattendanceapprove` 新增 `statisticsStatus` 字段：空值或空字符串表示参与统计，`取消至统计` 表示保留审批快照但不参与加班/夜班统计。
- `HrmAttendanceApprovalController` 新增 `POST /hrmAttendanceApproval/updateStatisticsStatus`，通过 `UpdateAttendanceApprovalStatisticsStatusBO` 更新单条审批的本地统计状态；`QueryAttendanceApprovalPageVO` 与 `HrmAttendanceApprovalMapper.xml` 同步透出 `statisticsStatus`。
- `HrmOvertimeNightStatisticsServiceImpl` 在加班审批优先汇总、实际出勤 `事假/病假/调休/年假` 扣减和应计出勤公式中统一跳过 `statisticsStatus=取消至统计` 的审批。
- 审批抓取同步链路同时修复两个风险点：
  - 流程编码改为按员工解析，避免全员抓取时复用第一个员工可见模板而漏抓其他员工可见的审批流程；
  - 全员抓取也会按实际解析到的员工范围执行陈旧快照清理，避免钉钉当前不再返回的旧审批长期残留。
- 新增 SQL 脚本 `docs/sql/2026-07-13_tbattendanceapprove_statistics_status.sql`，用于给 `hr_0001` 至 `hr_0005` 的 `tbattendanceapprove` 补齐 `statisticsStatus`。发布后端前必须在目标租户库执行该脚本，否则列表 SQL 读取新列时会触发缺列异常。
- 2026-07-13 16:17 已在 dev MySQL `153.0.237.98` 执行该脚本；`information_schema.columns` 复核 `hr_0001` 至 `hr_0005` 均存在 `tbattendanceapprove.statisticsStatus varchar(20) NULL`，并已在 `hr_0003.tbattendanceapprove` 直接查询新列成功。
- 限制：若公网后端连接的是 `application-prod.properties` 中的私有 MySQL `192.168.0.26:3306`，本机仍不可达，需要在可访问线上库的机器上执行同一脚本。
- 验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，81 个测试，0 失败。Maven 仍输出既有 settings/dependency 警告，权限异常栈为审批权限错误翻译测试的预期日志。

## Recent Changes and Decisions (2026-07-13, 审批抓取钉钉用户不存在处理)

- 审批抓取目标员工解析补充（2026-07-13）：
  - `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData(...)` 在单个、批量、全量获取审批数据时统一先读取 `hrm_employee`，不再只依赖 `tbattendanceuser` 的历史映射。
  - 解析顺序：
    - 员工表已有 `HrmEmployee.dingtalkUserId` 时，先调用钉钉通讯录校验该 `userId` 的姓名和手机号是否匹配当前员工；匹配后再用于获取审批，并通过 `saveAttendanceUserMapping(...)` 补齐或修正 `tbattendanceuser`；
    - 员工表没有钉钉 `userId`，或已有 `userId` 与钉钉通讯录姓名/手机号不匹配时，调用 `topapi/smartwork/hrm/employee/queryonjob` 取在职员工 userId，再分批调用 `topapi/smartwork/hrm/employee/v2/list` 读取姓名、手机号，按归一化后的姓名 + 手机号唯一匹配；
    - 唯一匹配后写回 `hrm_employee.dingtalk_user_id`，再保存 `tbattendanceuser` 映射并继续抓取；
    - 未命中或命中多个人时不猜测，避免审批串到同名或手机号不一致的员工。
  - 当钉钉在模板解析或实例列表接口返回 `400023/用户不存在` 时，会强制按员工表姓名 + 手机号刷新钉钉 `userId`；刷新后若 userId 变化，则用新 userId 重试本次员工抓取。
  - 抓取完成后的陈旧快照清理使用 `collectApprovalCleanupUserIds(...)`，会收集当前员工所有历史 `tbattendanceuser.userId`，保证换过钉钉 ID 的员工旧审批快照也会被清掉；未成功抓取的员工仍不会参与清理。
  - 回归测试新增：
    - `fetchMonthData_shouldRefreshExistingDingTalkUserIdWhenNameAndMobileMatchDifferentUser`；
    - `fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing`；
    - `fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt`。
  - 验证：
    - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldUseEmployeeDingTalkUserIdWhenAttendanceMappingMissing+fetchMonthData_shouldLookupAndSaveDingTalkUserIdByNameAndMobileWhenEmployeeMissingIt test` 通过，2 个测试，0 失败；
    - `mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，86 个测试，0 失败。

- 同一员工多条钉钉映射取值排序修复（2026-07-13）：
  - 现场现象：单选某员工时抓不到审批，但选“全部员工”却能抓到。
  - 根因：`resolveTargetUsers` 之前对同一 `empId` 只取 `findAllByEmpIdIn` / `findAll` 返回的第一条映射；当同一员工存在旧 `missing-user` 与新有效 `userId` 并存时，单选和全选会因数据库返回顺序不同拿到不同映射。
  - 修复：
    - `resolveTargetUsers` 和 `deduplicateUsersByEmployeeId` 现在按 `createTime` 优先、`id` 次优先选择同一员工的有效映射；
    - 这样单选和全选都会稳定落到最新的有效 `tbattendanceuser.userId`，不会再依赖数据库物理顺序。
  - 回归测试：
    - 新增 `fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee`；
    - 继续保留 `fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees` 和 `fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched`。
  - 验证：`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest#fetchMonthData_shouldPreferNewestAttendanceUserMappingForSelectedEmployee+fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched+fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees test` 通过，3 个测试 0 失败。
  - 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，84 个测试 0 失败。

- 现象：获取审批数据时，管理员模板接口先出现“管理员审批模板查询失败，回退按员工可见模板解析流程编码”，随后 `topapi/process.listbyuserid` 返回 `400023/用户不存在`。
- 根因：管理员模板 `用户不存在` 只表示管理员 `userId` 不可用于可管理模板查询，代码会按设计回退；真正阻断任务的是回退到员工可见模板后，某个本地 `tbattendanceuser.userId` 在钉钉侧已不存在。
- 处理：
  - `HrmAttendanceApprovalSyncServiceImpl#fetchMonthData` 在单个员工解析流程编码或拉取流程实例列表时识别 `400023/用户不存在/invalid user/userId not exist`；
  - 全员或多员工抓取时跳过该失效员工，继续处理其他有效员工；
  - 被跳过员工不写 `hrm_attendance_approval_fetch_mark` 完成标记，也不参与本轮陈旧审批快照清理，避免因未成功抓取而误删其本地旧审批；
  - 若本次命中的员工全部都是失效钉钉用户，则直接返回明确提示：`钉钉用户不存在或已离职，请先同步员工钉钉用户后重试：...`。
- 新增回归测试：
  - `fetchMonthData_shouldSkipMissingDingTalkUserWhenFetchingAllEmployees`；
  - `fetchMonthData_shouldReportMissingDingTalkUsersWhenNoEmployeeCanBeFetched`。
- 验证：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmOvertimeNightStatisticsServiceImplTest test` 通过，83 个测试，0 失败。Maven 仍输出既有 settings/dependency 警告，权限异常栈为审批权限错误翻译测试的预期日志。

## Recent Changes and Decisions (2026-08-03, 2026-07 审批抓取失败韧性)

- 现象：用户获取 `2026-07` 审批数据时前端提示“获取审批数据失败”。本地用 `hbadmin` 单员工抓取吴晓霞 `2026-07/all` 成功返回 `insertedCount=3`，说明月份边界、基础审批权限和单员工落库链路可用，风险集中在多人/全员抓取中的员工钉钉用户解析和钉钉调用放大。
- 数据核查：
  - `hr_0003` 当前登录公司为 `0003`；
  - 姚玖志、朱玲丽、李凤皇等当前员工的 `hrm_employee.dingtalk_user_id` 为空，但 `tbattendanceuser` 存在可通过钉钉通讯录校验的历史映射；
  - 孟建立、程传祥、谢杰杰、马国华等存在当前员工与另一条在职员工共享同一钉钉 `userId` 的重复档案；代码不得在这种情况下盲目把一个钉钉账号绑定给两个员工；
  - `hrm_attendance_approval_fetch_mark` 中仍可见部分 `2026-07/all` 完成标记落在重复员工或旧映射员工上，这属于数据质量问题，需业务侧清理员工档案或钉钉映射。
- 实现调整：
  - `HrmAttendanceApprovalSyncServiceImpl#resolveTargetUsers(employeeIds, token)` 对单个员工预解析异常改为记录失败并跳过该员工，避免全员/月度抓取被一个异常员工整体中断；若全部员工都失败，则抛出包含员工姓名、手机号和员工 ID 摘要的中文提示；
  - 员工表缺 `dingtalk_user_id` 且按姓名 + 手机号反查钉钉未命中时，新增回退 `resolveVerifiedAttendanceMappingUserId(...)`，只复用经过 `topapi/v2/user/get` 校验姓名、手机号匹配的 `tbattendanceuser.userId`，复用后写回员工表并刷新本地映射；
  - `preferAttendanceUser(...)` 复用 `compareAttendanceUserPreference(...)`，让同员工多映射排序规则在全量去重和本地映射回退里保持一致；
  - 钉钉返回 `isv.limitedFrequency`、限流、频控、调用频率等错误时，统一翻译为“钉钉审批接口触发限流，请稍后重试或缩小员工范围”。
- 新增回归测试：
  - `translateWorkflowErrorMessage_shouldExposeDingTalkFrequencyLimit`；
  - `fetchMonthData_shouldSkipEmployeeResolutionFailureWhenFetchingMultipleEmployees`；
  - `fetchMonthData_shouldUseVerifiedAttendanceMappingWhenEmployeeDingTalkUserIdMissing`。
- 验证：
  - `mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest test` 通过，63 个测试，0 失败；
  - `mvn -DskipTests compile` 通过，保留既有 `jsch/easyexcel` 重复依赖和 `systemPath` Maven warnings；
  - 本轮目标文件尾随空白检查无输出，已跟踪文档 `git diff --check -- docs/requirements.md docs/development.md findings.md progress.md task_plan.md` 无输出。

## Recent Changes and Decisions (2026-08-03, 同步考勤与审批获取进度条增强)

- 员工管理同步考勤：
  - `HrmAttendanceDataServiceImpl#getSyncProgress()` 统一返回 `status/done/success/error/message/errors/progress/percent/currentStep/processedCount/totalCount/params`。
  - Redis 新增 `attendance:sync:status`、`attendance:sync:message`、`attendance:sync:errors`，同步成功写入 `SUCCESS` 和 100% 完成文案，失败写入 `FAILED` 和友好中文错误文案；不再在同步成功或失败后立即清除进度。
  - `toFriendlySyncErrorMessage(...)` 负责把空参数、登录失效、权限、钉钉接口、数据库连接/表结构、员工钉钉信息等异常归一化为操作人员可理解的中文提示，避免前端进度条展示异常类名、SQL、HTTP 或堆栈文本。
  - 断点恢复仅在 Redis 状态为 `FAILED` 且 `EmpID|Begin|End` 与本次请求完全一致时启用；`SUCCESS` 最终状态只用于前端确认展示，不会误触发续传。
  - `HrmAttendanceDataController#syncAll`、`#getSyncProgress` 捕获异常时返回友好中文错误，避免接口响应直接透出技术异常。
- 审批数据手工获取：
  - `IHrmAttendanceApprovalService#queryFetchProgress()` 与 `POST /hrmAttendanceApproval/queryFetchProgress` 提供获取审批数据进度查询。
  - `HrmAttendanceApprovalServiceImpl#fetchMonthData(...)` 在准备、抓取、成功和失败阶段维护公司级内存进度状态，字段包括 `status/progress/message/done/success/error/insertedCount/errors`。
  - 审批获取失败会通过 `toFriendlyApprovalFetchMessage(...)` 翻译为业务文案，例如审批类型或月份缺失、员工缺少有效钉钉用户、钉钉限流、审批权限未开通、数据库连接或表结构需管理员检查。
  - 成功状态写入 `SUCCESS/100%` 和新增条数，失败状态保留当前进度与错误文案，均由前端等待操作人员确认关闭。
- 验证：
  - 后端新增 `HrmAttendanceDataServiceImplTest` 覆盖成功进度保留、失败进度友好文案和断点续传基础状态；
  - `HrmAttendanceApprovalControllerTest#queryFetchProgress_shouldDelegateToService` 覆盖审批获取进度接口委托。

## Recent Changes and Decisions (2026-08-22, 排班生产产品/岗位/员工配置)

- 新增后端模块 `modules/workplan` 下的生产产品配置能力：
  - `HrmWorkPlanProduct` 保存生产产品；
  - `HrmWorkPlanProductPosition` 保存产品下的岗位；
  - `HrmWorkPlanPositionEmployee` 保存岗位员工，允许 `employeeId` 为空以兼容手动输入的临时员工。
- `WorkPlanProductController` 暴露 `/workPlanProduct/queryTree`、`/saveProduct`、`/savePosition`、`/sortProducts`、`/sortPositions`、`/deleteProduct`、`/deletePosition`；`WorkPlanProductServiceImpl#queryTree()` 返回产品、岗位、员工树，并按 `tbattendanceuser.empId` 补出可排班的钉钉 `userId/groupId`。
- `WorkPlanProductServiceImpl#saveProduct(...)` 与 `#savePosition(...)` 在新增且请求未传 `sort` 时，分别按当前产品最大排序、同一产品下岗位最大排序自动生成下一排序号；编辑时未传 `sort` 会保留原排序。
- `WorkPlanProductServiceImpl#sortProducts(productIds)` 按提交生产产品 ID 顺序持久化产品排序，未提交但仍存在的产品追加保留，最终从 `0` 起连续写入 `sort`。
- `WorkPlanProductServiceImpl#sortPositions(productId, positionIds)` 只调整同一生产产品下的岗位排序：请求中的岗位 ID 按提交顺序排在前面，未提交但仍属于该产品的岗位保留并追加在后，最终从 `0` 起连续写入 `sort`。
- 保存岗位时采用“替换岗位员工列表”的语义：先删除该岗位旧员工，再按当前请求保存员工；这样前端清空或删单个员工后保存即可落库。
- 排班事实表继续保持扁平结构，`tbplanlist.ProductName` 作为生产产品名称，`tbplanlist.LinkName` 作为岗位名称；本轮不新增排班记录字段。
- 添加排班的一行多岗位仍在前端提交前展开为多条扁平保存项，每条保存项携带同一 `productName` 和对应岗位 `linkName/userId`；后端 `/workPlan/saveAll` 和 `tbplanlist` 持久化结构保持不变。
- `WorkPlanEmployeeDayShiftVO` 新增 `productName`、`positionName`、`workshopName`，`WorkPlanServiceImpl#applyLocalPlanToDayShift(...)` 从本地排班记录回显自由输入的车间值，历史空值才用考勤组名兜底。
- 新增迁移脚本 `docs/sql/2026-08-22_hrm_workplan_product_position_employee.sql`，显式创建三张配置表；因 `spring.jpa.hibernate.hbm2ddl.auto=none`，发布时必须执行脚本。
- 新增菜单权限迁移脚本 `docs/sql/2026-08-22_workplan_product_menu_permission.sql`：在 `/hrm/attendance` 下创建 `/hrm/attendance/workplanProduct / 生产产品` 子菜单，并给已拥有 `/hrm/attendance/records / 添加排班` 的角色补授权。
- `ApiPermissionPathSupport` 将 `/workPlanProduct/**` 映射到 `/hrm/attendance/workplanProduct`，避免新页面接口绕过菜单权限或被错误归到其它考勤菜单。
- 2026-08-22 本机排查：`cdadmin/companyId=0002/roleId=2` 登录 `127.0.0.1:8081` 时，前端静态包已包含 `workplanProduct` 路由，但 `hr_0002.tbmenu/tbrolemenu` 缺菜单和授权，导致 `menuTree` 不显示“生产产品”；补菜单后又因配置表未建导致 `/workPlanProduct/queryTree` 返回 SQLGrammarException。
- 2026-08-22 已在 dev MySQL `153.0.237.98` 的 `hr_0001` 至 `hr_0005` 执行生产产品三表建表脚本和菜单权限脚本；复核五个租户库均有三张配置表和“生产产品”菜单，`cdadmin` 重新登录后 `menuTree` 包含 `/hrm/attendance/workplanProduct`，`/workPlanProduct/queryTree` 返回 `code=200`。
- 验证：
  - `mvn -Dtest=ApiPermissionPathSupportTest,MenuPermissionSupportTest,WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest test` 通过，19 个测试 0 failures/errors；
  - `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanServiceImplTest#queryEmployeeDayShift_shouldExposeProductPositionAndWorkshopForEditDialog test` 通过，6 个测试 0 failures/errors；
  - `mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest,WorkPlanProductMigrationSqlTest,WorkPlanListControllerTest,WorkPlanServiceImplTest test` 通过，73 个测试 0 failures/errors；
  - 2026-08-23 追加验证：`mvn -Dtest=WorkPlanProductServiceTest,WorkPlanProductControllerTest test` 通过，10 个测试 0 failures/errors，覆盖产品排序接口和服务持久化顺序；
  - `mvn -DskipTests compile` 通过，保留既有 Maven POM duplicate/systemPath warnings。

- 应出勤天数计算逻辑修复（2026-08-26）：
  - 问题背景：7月薪资测试发现吴晓霞、余德胜、聂小玲等行政体系员工全勤奖缺失或薪资计算异常。
  - 根因分析：
    1. 吴晓霞：`is_disabled=1`（禁用状态），导致全勤判断逻辑被跳过，`isFullAttendance` 保持默认值 `false`。
    2. 余德胜、聂小玲：行政体系员工（`affiliation_system=1`）但 `rest_type=2`（固定月休4天），被分配到"生产系统"考勤组，导致应出勤天数计算为 31-4=27天，而行政单双休正确值应为22-25天。
  - 修复方案：修改 `HrmOvertimeNightStatisticsServiceImpl#resolveExpectedAttendanceDays` 方法：
    - `rest_type=2`（月休4天）：应出勤天数 = 当月总天数 - 基本工资设置的 `productionMonthlyRestDays`（不再扣除法定节假日）。
    - `rest_type=1`（行政单双休）：应出勤天数 = 单双休设置的月出勤天数（保持不变）。
  - 影响范围：
    - 加班/夜班统计的应出勤天数计算。
    - 工资计算中的应出勤天数（从 `hrm_overtime_night_statistics_detail` 表读取）。
  - 修复后操作：
    1. 用户需在页面重新执行"开始统计"更新 `hrm_overtime_night_statistics_detail` 表。
    2. 重新计算工资使用正确的应出勤天数。
  - 验证：`mvn compile` 通过。

- 全勤判断逻辑修改（2026-08-26）：
  - 问题背景：残疾员工（`is_disabled=1`）无法获得满勤奖，因为全勤判断逻辑被跳过。
  - 修改方案：修改 `SalaryMonthRecordServiceNew#fillAttendanceDataForEmployee` 方法，移除 `is_disabled` 检查，所有员工都可以获得满勤奖。
  - 修改逻辑：
    - 修改前：`if (IS_DISABLED_NO.equals(String.valueOf(map.get("isDisabled"))))` → 只有正常员工才执行全勤判断
    - 修改后：直接执行全勤判断，不再检查 `is_disabled` 字段
  - 影响范围：工资计算中的全勤奖判断逻辑。
  - 保留逻辑：`fullAttendanceFlag`（是否有全勤）设置仍然有效，只有设置为 "1" 的员工才会获得满勤奖。
  - 验证：`mvn compile` 通过。

## 后端性能审查与第一批修复（2026-08-30）

### 审查结论（全库只读审查，高危项已逐条人工核实）
基础面良好：JDBC 资源纪律、租户数据源池、ThreadLocal 生命周期、Excel 产品导入流式化（9347942）复核通过。剩余风险集中在四类：钉钉同步死循环、报表/导出 SQL 风暴、并发锁缺陷、无界增长。需求侧规则见 `requirements.md` 2026-08-30 小节。

### 本批修复明细
- **死循环快速失败**：`imple/ddTalk/AttendanceUserManager.java` 在职用户循环（原 success=false 无 else 无限重入）与离职用户循环（原 catch 吞异常永久重放）均改为 1s 间隔重试 3 次后抛 `ApiException` 中止；`Thread.sleep` 收敛到 `sleepQuietly`（恢复中断标记）。`GetAndSave` 去掉 `@Transactional`，落库段（`batchSaveUsers`+`deleteStaleMappings`）经 `TransactionTemplate` 在事务内执行，钉钉分页不再持有租户连接。
- **单例共享状态清零**：`users` 共享字段从 `AttendancePlanRecord/AttendanceDetailRecord/AttendanceLeaveRecord/AttendancePlanServiceImpl/AttendanceDetailServiceImpl/LeaveRecordDataServiceImpl/HolidayDataServiceImpl` 全部移除，`setUsers` 从 `IAttendancePlanService/IAttendanceDetailService/ILeaveRecordDtaService/IHolidayDataService/IDetailRecord/ILeaveRecord` 移除，改为方法参数逐层传递；`HrmAttendanceDataServiceImpl` 四处调用点与 `autoTask/AttendancePlanRecordTask、AttendanceDetailRefreshTask` 同步更新；`AttendanceLeaveRecord.LeaveKey` 改局部变量。此前"同步块保证 setUsers 和 Sync 原子性"的补丁失去存在意义。
- **SimpleDateFormat**：`MyDateUtils/HrmAttendanceDataController/HrmReportController/AutoTaskTokenUtil/UpdateRecordTemplate/HrmAttendanceReportManager` 及三个 Record 类的字段全部改 `ThreadLocal.withInitial`。
- **同步防重入**：`HrmAttendanceDataServiceImpl.SyncData`（`/sync`、`/syncAll` 共用入口）用 `redis.setNx("attendance:sync:running_lock", TTL 2h)` 原子抢占，finally `del` 释放；进程崩溃最多 2h 自动放行。三处 `Executors.newFixedThreadPool` 的租户上下文校验移到建池之前，消除校验抛出时非 daemon 线程池泄漏。
- **薪资锁**：`SalaryMonthRecordServiceNew` 的 `COMPUTE_RECORD_LOCK_MAP` 锁对象常驻不再 remove（消除 hasQueuedThreads 竞态）；新增 `protected runInTransaction(Callable)`（生产走 `TransactionTemplate`），`computeSalaryData/recoverSalaryMonth/addNextMonthSalary/updateCheckStatus` 去掉 `@Transactional`，改为"先取锁、锁内开事务"，等待锁的请求不再占用租户池连接。
- **社保互斥**：`HrmInsuranceMonthRecordService.computeInsuranceData` 按公司 `tryLock`（`INSURANCE_COMPUTE_LOCK_MAP`），重复点击立即报错；该类原本没有 logger，已补充。
- **动态调度**：`ScheduledController` 注册表改 `ConcurrentHashMap`、startCron 同编号先 cancel 旧 future、`ThreadPoolTaskScheduler` 设 poolSize=4 + 线程名前缀；start/stop 对 vo 判空防 NPE。
- **锁粒度**：`common/AttendanceDbLock` 改 `lockFor(companyId)` 按公司锁（缺失上下文退化全局锁），唯一使用点 `AttendancePlanRecord.GetAndSave(EmpIDS,WorkDate,users)` 更新。
- **Redis TTL**：`WorkPlanServiceImpl.persistCustomPlanMeta` 加 30 天 TTL（`PLAN_META_CACHE_TTL_DAYS`，读取端有 DB 兜底链）；`HrmAttendanceDataController` 的 `ClassList_*` 写入加 30 分钟 TTL（`getClassListByGroup` 缺失时走 `buildLocalClassItemsByGroup` 本地快照）。
- **资源泄漏**：`FTPUtil` 两个 download finally `close()`、`checkSubfolder` 补 `completePendingCommand`；`AttachmentController.Download` finally 关闭 FTP 连接并删除 Temp 临时文件；`AttachmentServiceImpl.Upload` 改 `MultipartFile.transferTo` 流式落盘、流 finally 关闭、临时文件即时删除；`EmployeeDepartmentDetailCrossCompanyExportSupport.buildEncryptedZip` 的 zip4j `ZipFile` try-with-resources。
- **日志表保留期清理**：新增 `task/DingTalkLogRetentionCleanupTask`（独立 daemon 调度，绕过 `hrm.scheduling.enabled=false` 的全局关闭），每天 5 点遍历 `LoginUserMapper.getAllCompanies(systemDatabase)` 各租户切换 CompanyContext 后批量删除：`postresultlog` 保留 90 天、`ddtaskresult`（processed=200）保留 7 天；2000 条/批独立事务、每表每公司单轮 ≤50 批。配置项 `hrm.dingtalk.log-cleanup.*`（enabled/postresultlog-retention-days/ddtaskresult-retention-days）。`postresultlogRepository/ddtaskresultRepository` 新增分批查询与 `@Modifying deleteByIdIn`。
- **其它**：`getOvertTime`（getTotalByFields）复用客户端 + 100ms 间隔 + 非数字列值跳过告警；审批 `FETCH_PROGRESS_MAP` 增加终态 2h/运行态 24h 的 removeIf 清理（state 记录 `lastUpdatedMillis`）。
- **测试**：4 个使用旧 `setUsers/Sync` 签名的测试（`AttendanceDetailServiceImplTest/LeaveRecordDataServiceImplTest/HolidayDataServiceImplTest/HrmAttendanceDataServiceImplTest`）更新为新签名；`SalaryMonthRecordRecoveryTest` 的 `TestableSalaryMonthRecordService` 覆写 `runInTransaction` 直跑（无事务）。定向回归 13 个测试全绿。

### 测试回归结论
`mvn test` 全量 634 项：16 个失败/错误**全部为工作区存量问题**，与本次改动零关联（已逐一核实失败点不在任何被修改文件中）：
- `ApplicationProfileConfigTest`（1）：工作区把 `application.properties` 默认 profile 改为 `local`（用户未提交改动），守卫测试按设计报错；
- `HrmEmployeeContractServiceImplTest`（6）：Excel 合同导入域，EasyExcel `Can not instance class: java.util.List`——本次明确不触碰 Excel 批量导入；
- `LoginControllerTest`（3）：用户未提交的 `LoginController` 改动与新测试不匹配的 NPE；
- `HrmOvertimeNightStatisticsServiceImplTest`（5）：应出勤 25/26 断言与用户未提交实现不一致；
- `HrmEmployeePostServiceImplCompanyAgeTest`（1）：mock 链 NPE。
`mvn -DskipTests compile` 全程通过。

### 第二批优化（2026-08-30 同日，低风险遗留项）
- `AttendanceGroupManager.GetAndSave`：与 `AttendanceUserManager` 同模式重构——`@Transactional` 移除，钉钉分页抓取（含逐班次 `shift/query` 外呼）在事务外执行，4 张表 `deleteAll`+`saveAll` 的快照替换经 `TransactionTemplate` 单事务原子完成；共享 `timeFormat` 改 `ThreadLocal`。
- `AttendanceDetailRecord` 两个 `GetAndSave` 的逐条 `findById` N+1 消除：每页先 `findAllById` 批量预查已存在的 clock/detail 主键（每页 2 条 SQL 代替 2N 条），循环内改用集合判断，行为与逐条查询完全等价。
- 排班提交重试退避改为指数增长（200ms 起步、封顶 30s，总计约 111s），抽成 `protected submitRetryBackoffMillis(int)`；钉钉限流窗口内不再 10 连击放大调用量。`WorkPlanServiceImplTest` 用 spy stub 退避为 0 保持单测速度。
- `DatabaseBackupService.cleanupExpired` 增加 `restore-snapshot_*` 目录清理（保留 24 小时），递归删除工具为私有方法。
- `RedisImpl.mSet/mSetNx` 修复 ConcurrentModificationException（原实现遍历 keySet 时 remove+put，多项必炸、当前无调用方），改为前缀补齐后写入新 Map。
- 各进度状态类字段补 `volatile`（薪资/社保/审批/排班提交），消除轮询可见性问题，仅影响进度显示时效。
- 薪资导出与零时工导出的 `ExcelWriter.finish()` 收进 finally（异常时释放 POI 临时资源），模板 classpath 流显式关闭。
- `application-prod.properties` 两处 mapper `DEBUG` 日志降为 `INFO`，消除生产环境每条 SQL 的日志 IO。
- 测试：`AttendanceGroupManagerTest` 注入 no-op `PlatformTransactionManager`；`WorkPlanServiceImplTest` stub 退避为 0。全量 634 项测试回到 16 个存量失败的基线，本次改动范围内零失败。

### 每月 1 号集中同步方案实施（2026-08-30，针对用户文档《考勤同步并发风险与优化方案》）
- 文档核实结论：微服务零收益、连接池容量、同步阻塞 HTTP、重复触发风险四点均成立；但文档 4.1.4/4.1.6 已被本日上一批修复部分覆盖（互斥锁已存在但是全局粒度、线程池异常路径泄漏已修），3.3"限流已充分"对审批链路不成立。文档遗漏 5 点：进度键全局粒度、考勤前端完成信号绑定触发响应、旧进度（24h TTL SUCCESS）误读竞态、CallerRunsPolicy 与立即返回矛盾、锁应在提交路径抢占。
- 实施内容（需求规则见 requirements.md 2026-08-30 集中同步小节）：
  - `HrmAttendanceDataServiceImpl`：新增 `syncKey(base)` 给全部 11 个同步进度/标记键追加 `:companyId`；`saveSyncProgress/saveFinalSyncProgress/saveFailedSyncProgress` 写入 `update_time`；新增 `markSyncQueued()`；`getSyncProgress` 返回 `updateTime/queuedAt`；移除内部全局运行锁（上移到 launcher）。
  - 新增 `task/AttendanceSyncTaskLauncher`：核心=上限=8/队列 8 的有界线程池（AbortPolicy，拒绝返回"繁忙"）+ Redis 按公司运行锁（`attendance:sync:running:<companyId>`，TTL 2h，任务 finally 释放）；submit 恢复 CompanyContext、任务异常只记日志（进度已由 SyncData 内部标 FAILED）。
  - `HrmAttendanceDataController /sync|/syncAll`：tryBegin → markSyncQueued → submit → 立即返回 `{queued:true, queuedAt}`；提交失败路径归还锁；`SyncData` 签名与断点/重试逻辑零改动。
  - `HrmAttendanceApprovalServiceImpl`：新增按公司运行标记（`FETCH_RUNNING_MAP`，AtomicReference\<Long\> 时间戳，2 小时陈旧自动放行）的 `tryBeginFetch/finishFetch` 与 `beginFetchProgress()`（提交后同步写 RUNNING 覆盖旧完成状态）；`HrmAttendanceApprovalController /fetchMonthData` 同模式异步化；`HrmAttendanceApprovalSyncServiceImpl` 三处外呼（listbyuserid/listids/processinstance-get）后 sleep 100ms。
  - 接口补充：`IHrmAttendanceDataService.markSyncQueued`、`IHrmAttendanceApprovalService.tryBeginFetch/finishFetch/beginFetchProgress`。
  - 前端 `hr_web`：`employee/Index.vue` 完成信号改为"轮询 done 且 updateTime >= queuedAt"（新增 `pollSyncUntilDone`，最长 40 分钟，卸载中断），排队期显示"已提交，正在排队准备同步数据..."；`approval/Index.vue` 新增 `pollFetchUntilDone`，完成文案与 insertedCount 改取自进度数据。
- 测试：新增 `AttendanceSyncTaskLauncherTest`（按公司锁抢占/任务执行恢复上下文并还锁/队列拒绝返回 false）；更新 `HrmAttendanceDataServiceImplTest` 键断言为 `:0003` 后缀、`HrmAttendanceApprovalControllerTest` 两个用例适配异步委托（等待后台任务执行后再断言）。全量 637 项测试回到 16 个存量失败基线，本次改动零新增失败；`hr_web npm run build` 通过。

### 失败自动重试（2026-08-30 补充实施）
- 考勤同步：`HrmAttendanceDataServiceImpl.SyncDataWithAutoRetry`（接口同步声明）——首次运行走 `SyncData`（自动判定断点），失败后 `markSyncRetrying` 保持 RUNNING + 重试文案，指数退避（60s×2ⁿ，封顶 5 分钟）后以 `SyncDataWithResume(resume=true)` **从断点续传**；默认 2 次自动重试，`hrm.attendance-sync.auto-retry.attempts/backoff-ms` 可配。最终失败时内部已置 FAILED，前端轮询正常收尾。
- 审批获取：`HrmAttendanceApprovalServiceImpl.fetchMonthDataWithAutoRetry`——**重复拉取幂等已核实**（`approvalRepository.findById(processInstanceId)` 主键跳过已入库记录），失败后同样退避重试；"请选择..."参数类错误与"重复员工"数据类错误不自动重试。
- 控制器后台任务 lambda 改调两个 WithAutoRetry 入口；公司锁仍由 launcher 在整个重试周期内持有（重试期间用户点击返回"正在进行中"）。
- 前端两页轮询上限 40 分钟 → 60 分钟以容纳重试窗口；无其它前端改动（重试期间进度条显示重试文案）。
- 测试：`HrmAttendanceDataServiceImplTest#syncDataWithAutoRetry_shouldResumeAfterTransientFailure`（首次失败→断点续传→RUNNING 保持）；`HrmAttendanceApprovalControllerTest` 代理方法名更新为 `fetchMonthDataWithAutoRetry`。全量 638 项回到 16 个存量失败基线。

### 企业权限保存失败修复（2026-08-30）
- 现象：编辑企业权限偶发保存失败，`INSERT INTO hr_0001.tbrolemenu (role_id, menu_id) VALUES (?, ?)` 报 `Field 'role_menu_id' doesn't have a default value`。
- 根因：`tbrolemenu.role_menu_id` 为 bigint 非自增主键（schema-baseline.sql:1816，种子数据均为显式雪花式 ID）；"角色权限分配"页走 MyBatis-Plus `saveBatch`，实体 `TbRoleMenu` 主键为 `IdType.ASSIGN_ID` 自动生成雪花 ID 所以正常；而"企业权限"保存走 `LoginUserMapper.insertRoleMenu` 跨库原生 SQL，插入列缺少 `role_menu_id`。"偶发"实为该分支仅在基准角色确有菜单权限需要同步时才执行。
- 修复：`insertRoleMenu` 增加 `roleMenuId` 参数并写入主键列；`CompanyPermissionController` 循环内用 `IdWorker.getId()`（与 ASSIGN_ID 同源的雪花算法）逐行生成，显式提供主键——对自增与非自增的表结构都兼容，无需 DDL 变更。JPA 侧 `tbrolemenu` 实体（IDENTITY）全项目只读不写，无同类风险。
- 验证：`mvn -DskipTests compile` 通过；跨库 SQL 需联调环境实际保存一次企业权限确认。

### 企业权限“未配置菜单权限”修复（2026-08-30，第二轮）
- 症状：企业权限为账号设置全部分公司后，用该账号登录并选择某分公司，报“该账号绑定的角色未配置菜单权限”。
- 因果链（两层 bug 叠加）：
  1. 触发层：保存流程为“先删该账号全部企业映射 → 逐公司循环（建角色→同步菜单→建号/统一角色）”，此前循环在 `insertRoleMenu`（缺 role_menu_id，见上一条修复）处抛异常——**失败公司的映射已插入但菜单未同步，后续公司全部未处理**；登录时该角色的 menuIds 找不到可启用页面菜单，`MenuPermissionSupport.requireHasPageMenuIds` 抛出该错误。
  2. 放大层：`CompanyPermissionController.save` 标注 `@Transactional` 但 **catch 异常在方法内部**返回 `Result.error`，异常不穿透事务代理 → 事务不回滚 → 半途而废的中间状态（映射删了只补一半、菜单缺失）被保留。
  3. 隐患层：菜单同步只复制 `tbrolemenu` 映射，不复制 `tbmenu` 菜单定义；目标库 tbmenu 缺这些菜单 ID（租户库菜单曾发生漂移）时同样报错。
- 修复：
  1. `CompanyPermissionController.save` 的 catch 中显式 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`——任一公司失败**整体回滚**（含循环前删除的映射），用户重试时状态干净；
  2. 新增 `LoginUserMapper.insertMenuIfMissing`：同步菜单权限前，从基准库把目标库缺失的 `tbmenu` 菜单定义按 id 补齐（INSERT...SELECT + NOT EXISTS，同实例跨 schema）；
  3. role_menu_id 显式雪花主键（上一条修复）保持不变。
- 存量坏数据修复方式：部署后**对该账号重新保存一次企业权限**即可（全量替换语义会重做全部公司的映射/角色/菜单/账号统一），无需手工 SQL。
- 验证：`mvn -DskipTests compile` 通过；跨库写入需联调实测（重存后用该账号登录各分公司核对菜单）。

### 数据看板集团总表接口缺失修复（2026-08-30）
- 现象：数据看板"集团总表"tab 提示"请求的接口不存在: /hrsystem/hrmEmployee/departmentDetailGroupSummary"。
- 根因：服务层已实现（`IHrmEmployeeService#departmentDetailGroupSummary` + `HrmEmployeeServiceImpl:1909`，内部自带行政经理校验与 `groupSummary:*` 看板权限断言），前端 `GroupSummaryBoard.vue` 也在调用，但 `HrmEmployeeController` 漏建对应端点。
- 修复：`HrmEmployeeController` 新增 `POST /hrmEmployee/departmentDetailGroupSummary` 返回 `Result.ok(...)`。权限行为：拦截器按 `/hrmEmployee` 前缀要求"员工管理"菜单，方法内部再校验行政经理与集团看板权限（无权者 403"无权访问集团数据"）。
- 验证：`mvn -DskipTests compile` 通过；需重新打包部署后端，前端无改动。

### 田野农谷用户反馈 9 项问题修复（2026-08-30）
1. **高级筛选【用工性质】无数据**：真实数据源是花名册动态字段"用工性质"(hrm_employee_data,field_name=roster_d580da9d)，而筛选 SQL 用 status=5/合同类型推算的另一套口径，两套口径对不上导致选劳务必空。`HrmEmployeeMapper.xml` 的用工性质条件改为按动态字段匹配（bind 变量 1→劳动用工/2→劳务用工，LIKE 匹配 field_value/field_value_desc），与导出口径（EmployeeBasicInfoExportSupport 优先读动态字段）一致。
2. **司龄计算错误**（入职 2020-04-13 显示 5 年）：三层缺陷——导入路径把转正日期当司龄开始日期兜底；按天 /365 换算累积偏小；已离职员工直接显示库内单位混乱的旧值。修复：导入兜底改用入职日期（entryDate.atStartOfDay()）；`fillCompanyAgeFields` 改为 Period 精确计算（基准=司龄开始日期→入职日期兜底，已离职冻结在离职日期），已离职不再走原样展示旧值；存量污染数据见 `docs/sql/2026-08-30_fix_company_age_start_time.sql`（指纹 company_age_start_time=become_time 的行改回 entry_time）。
3. **入职/合同/离职时间筛选联动**：三个 daterange 缺 `unlink-panels`（Element Plus 默认双面板联动翻页），已加 unlink-panels+分隔符+占位符。
4. **补充学历筛选**：BO 加 `education`，`HrmEmployeeMapper.xml` 加与 SELECT 同口径的最高学历过滤（主表最高学历空则取教育经历最高值），前端加 1-12 学历下拉。
5. **合同到期状态不自动更新**：`hrm_employee_contract.status` 仅人工写入。修复（查询时动态纠正，零 DDL）：`HrmEmployeeContractServiceImpl.contractInformation` 返回前按日期覆盖——endTime<今天→已到期、未到开始日期→未执行、无固定期限合同不判到期。后续如需状态落库统计再加独立定时任务。
6. **工作地点字段重复**：`hrm_employee_field` 配置冗余（work_city/work_address/work_detail_address 同组可见）。按用户确认保留工作城市+详细工作地点，`docs/sql/2026-08-30_hide_duplicate_fields.sql` 将 work_address 置 is_hidden=1（可逆）。
7. **生日重复**：员工列表移除"生日"列（保留出生日期）；同脚本隐藏动态字段"birthday/生日"；补齐 mapper 中缺失的 `queryBirthdayEmp/queryBirthdayListByTime` SQL（原声明无实现，生日提醒路径触发即 Invalid bound statement；新 SQL 阳历按出生日期 MMDD、农历/文本按 birthday 字段，GROUP BY 去重）。
8. **列表表头不固定**：`Table.vue` 新增可选 `maxHeight` prop 绑定 el-table（不传行为不变），员工列表改传 `:max-height="590"`，表头固定由 el-table 内部滚动实现。
9. **字体偏暗淡/详情文字过浅**：`style.scss` 覆盖 EP 文本色变量（regular #303133、secondary #606266、disabled #606266、table-text #303133），body 字重 500→400；`DynamicFormItem.vue` 禁用态输入框文字加深为 #172b4d（scoped :deep）。
- 验证：`mvn test` 638 项回到 16 个存量失败基线（零新增）；`hr_web npm run build` 通过；Mapper XML xmllint 校验通过。
- 部署：前端需重新发布（本轮有前端改动）；后端重新打 prod 包；两个 SQL 脚本在各租户库执行（可逆 UPDATE）。

### 已知问题 / Future Work（未修复，留待后续批次）
- 加班/夜班统计"开始统计"逐员工 4~8 条全月查询 + 每天班次查询 + tbplanlist 全月×N 次（`HrmOvertimeNightStatisticsServiceImpl` :925/:941/:960/:2196/:2254）——建议按月 IN 预载 + 按 employeeId 分组，复用薪资核算批量模式；因直接驱动薪资口径，需配合数据对账后实施。
- 员工花名册/部门明细导出动态字段与部门名 N+1（`HrmEmployeeServiceImpl` :2418/:1602/:1859）；集团汇总导出每公司 XSSF DOM + byte[] + zip 全堆内（:2109）。
- 薪资导出每员工工资项 N+1 + O(n²) 过滤 + 3 份副本 + `forceNewRow(true)`（`SalaryMonthRecordServiceNew` :5128-5441）。
- 审批抓取/钉钉报表同步仍是同步长 HTTP + 串行无限流（限流器仅在 autoTask 生效）。
- Excel 批量导入侧的 POI/hutool DOM 用法（员工花名册导入、奖金/假期/个税/附加导入等）按要求**保持不动**。
- multipart 上限 3000MB（业务设定，未调整）。
