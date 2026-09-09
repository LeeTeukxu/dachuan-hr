# 小程序后端（/mp/* 接口）

菜单前缀：`/mp/*`（无 PC 菜单）。本模块的完整接口契约、页面与历轮改动以小程序项目文档为准，本文只记录 hainan 后端侧要点，两份文档互相引用：
- 接口契约与小程序端实现：`miniapp/hr_miniapp/docs/development.md`（MiniAppController 契约表）
- 项目状态与历轮交接：`miniapp/docs/完成情况.md`

## 需求要点
- 微信员工端"我的排班"：免费 OpenID 登录/首次员工自助绑定/多公司选择（`/mp/login`、`/mp/login/bindEmployee`、`/mp/login/bindCompany`）、月历排班（`/mp/mySchedule`、`/mp/mySchedule/day`）、改班申请与审批（`/mp/application/submit|myList|cancel|toApprove|approve`）、生产排班（`/mp/schedule/*`，仅上级可见 `/mp/isSupervisor`）。
- 生产排班与 PC 排班管理共用 `tbplanlist`（2026-08-30 统一改造，AGENTS.md 硬性约定 #5）：保存复用 PC 的 `IWorkPlanService.saveEmployeeDayAssignments` 按员工拆分，时间段物化到 `hrm_workplan_custom_shift`；按天整表替换（后端 diff 清理未含员工），同员工同日禁止"生产班+休息"并存；员工池与 PC 排班口径一致（在职 `is_del=0` 且 `entry_status in (1,3,4)`）。
- 安全加固四项（详见 完成情况.md"第五轮：安全加固"与 待办清单）：JWT 密钥硬编码待办 #18、登录限流、审批人校验（仅被指派审批人可 `/mp/application/approve`）、敏感值不入源码。改任何登录/token/权限逻辑前先读完成情况.md。
- 契约形态：全部 POST；`/mp/*` 用 `application/x-www-form-urlencoded`（唯一例外 `/mp/schedule/save` 为 JSON body）；token 请求头 `token`（裸 JWT，无 Bearer）。

## 设计与契约
- 后端入口：`MiniAppController`（响应 data 结构见 hr_miniapp 文档契约表）。
- 登录：小程序 `wx.login` 的 code 经 `code2Session` 换 OpenID；租户库 `hrm_employee.openid`（`ascii_bin`）是映射事实源。登录按 openid 跨全公司枚举匹配数三分支（`MiniAppServiceImpl#doLogin`，绑定仅存 `hrm_employee.openid`，无独立绑定表；`hrsystem.miniapp_user_binding` 是未启用空表）：①匹配 0 → 弹「验证员工身份」（选公司+姓名+完整证件号核验一次，条件 UPDATE 原子写入 OpenID，同名同证件号多条拒绑）；②匹配 1 → **直接登录进首页，不弹任何框**；③匹配 ≥2（同 openid 在多家租户库各有一条员工记录）→ 弹「选择所属公司」纯列表，无需再输信息。同一微信绑多家=同名 openid 已在多库 `hrm_employee` 建档（通常是测试期同一手机号/微信在多库各绑一次造成），属正常多租户设计非 bug。ticket 存 Redis、TTL 600 秒。
- 鉴权：CompanyInterceptor 对 `/mp/*` 按接口分三类判定——①命中 `tb_api_permission` 菜单映射（如 `/mpPermission` 配置页）走菜单/角色权限；②命中员工级能力映射（`ApiPermissionPathSupport.ABILITY_*`：`/mp/dashboard`→数据统计、`/mp/mySchedule*`、`/mp/schedule/query`→排班数据加载、`/mp/schedule/save|employees|standardProducts|batchSetRestDay`→添加排班）按员工在 `mp_schedule_permission` 的配置判定（严格模式，未配置即拒）；③其余 `/mp/*` 仅校验 token（员工 token 可访问）。员工 token 无 menuTree，故 `/mp` 能力统一走员工级字段而非菜单。
- 权限体系（2026-09-09 重构为员工级）：三个能力统一按员工配置在租户库 `mp_schedule_permission`（`can_schedule`/`can_view_statistics`/`can_load_schedule`，默认 0=关）——①添加排班（`/mp/schedule/*` 保存/员工池/标准产品/批量设休息日）；②数据统计（`/mp/dashboard/*`）；③排班数据加载（`/mp/mySchedule*`、`/mp/schedule/query`）。未配置任何能力的员工上述接口一律拒绝（严格模式）。PC 配置页 `/mpPermission/*`（菜单 1021，已改名「小程序权限」）按角色控制谁能进页；原 1030/5000/5020 三菜单及其角色授权已删除。审批可见范围仍按员工三档（1直属下属默认/2全部/3自定义 `mp_schedule_visible_employee`），审批动作限直属上级；`/mp/application/submit|myList|cancel` 不进权限体系。
- 鉴权失败统一 HTTP 200 + `success:false`（非 401）。
- 数据路由：按绑定公司 `CompanyContext` 落对应租户库，与 PC 共用同一批表（tbplanlist、hrm_workplan_custom_shift、员工/审批相关表）。
- **部署依赖 SQL**：先对全部现有租户执行 `../sql/2026-09-02_hrm_employee_openid.sql`（`hrm_employee.openid VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL` + 租户内唯一索引），再部署后端和小程序；新租户基线已内建同字段。历史申请表脚本仍见 `../sql/miniapp_tables.sql`。排班小程序权限见 `../sql/2026-09-06_miniapp_schedule_permission.sql`（存量租户两表+菜单+角色映射、主库 tb_api_permission 4 行，执行后热刷新权限映射）。小程序权限员工级改造见 `../sql/2026-09-09_mp_employee_permission.sql`（各租户库加三列 + 存量 `can_schedule=1` + 合并菜单 1021/删除 1030/5000/5020；主库清 `/mp` 菜单映射保留 `/mpPermission`）。公司切换可见公司系统库表见 `../sql/2026-09-10_mp_employee_visible_company.sql`（hrsystem 建表）。公司切换能力字段见 `../sql/2026-09-10_mp_employee_switch_company.sql`（各租户库 mp_schedule_permission 加 can_switch_company 列；幂等）。部门编制字段见 `../sql/2026-09-10_hrm_dept_plan_num.sql`（各租户库 hrm_dept 加 plan_num 列；幂等）。

## 近期变更
- **2026-09-10（集团总表语义更正，重要）**：回滚 2a 对集团聚合的"按员工可见公司收敛"。定位：集团总表 /group、/group/salary 在小程序里对所有能进数据统计（can_view_statistics）的员工开放，但此前 guardGroup 把 `scopeCompanyIds` 收敛成员工可见集合，普通员工未被授权别家时集团只剩本登录公司，破坏"集团"语义。更正为——集团总表授权边界=能否访问 /mp/dashboard（CompanyInterceptor 已按 can_view_statistics 门控），**集团聚合全量公司**（`DashboardServiceImpl.resolveScopeCompanies` 当 scopeCompanyIds 为空时遍历 `getCompanyList()` 全集团），不再注入可见公司白名单；仅保留"显式传越界 companyId 拒绝"。单公司接口（personnel/flow/salary/dept 等）仍按可见公司切换语义走 guardCompany 不变。改动文件：`MiniAppDashboardController#guardGroup`。关联 `miniapp/docs/完成情况.md` 第三十九轮「体验优化」。
- 2026-09-10 入/离职趋势卡片增强（第三十九轮，miniapp 侧为主）：MiniAppDashboardController 新增 `POST /mp/dashboard/flow/detail` → `dashboardService.flowPageList`（复用既有 PC 入离职人员分页服务，含 guardCompany 越权收敛；组装 `{records,total}`，records 含 `recType=in/quit`、姓名/部门/岗位/入离职日期/离职类型/离职原因）。miniapp `api/modules/miniapp.js` 加 `getFlowDetail`；`statistics.vue` 把顶部周期改快捷+自定义起止月、趋势卡左右滑平移窗口（默认 6 个月尾=当月）、flow 看板新增部门汇总表点数字下钻明细弹窗。详见 `miniapp/docs/完成情况.md` 第三十九轮。
- 2026-09-10 「公司切换」可见公司授权（B方案，最新 → 已闭环见下 2a）：员工在 /mp/dashboard 看板页可经公司chip切换到其可见的其它公司看板。授权键=openid+mobile、公司级，落系统库 `hrsystem.mp_employee_visible_company`（见 `../sql/2026-09-10_mp_employee_visible_company.sql`）。可见集合={本登录公司}∪(openid匹配的授权company_id)，无授权行=只自家公司（严格）。越权边界：MiniAppDashboardController 对员工 token（无 account）下单公司接口越界 companyId 直接拒（单公司接口按可见公司切换语义）；集团聚合原 2a 曾注入 `scopeCompanyIds` 收敛——**已被 2026-09-10 顶部更正回滚为聚合全量公司**（见本文件顶部"集团总表语义更正"，guardGroup 不再收敛）。新增 GET `/mp/dashboard/visibleCompanies`（当前员工可见公司清单，公司chip数据源）；PC 配置页 `/mpPermission/employeeList` 行新增 openid/visibleCompanies、新增 POST `/mpPermission/saveVisibleCompanies`（saveVisibleCompanies，需员工已绑定 openid）。hr_web「小程序权限」页新增「公司切换可见公司」列（下拉全量公司）；miniapp statistics.vue 公司chip联动 companyId、deptStatistics.vue 部门统计同样接公司切换（可见公司>1 时顶部出公司chip，切换重拉部门人数）。详见 `miniapp/docs/完成情况.md`。
- 2026-09-10 第二批闭项（2a/2b/3/4）：
  - **2a 公司切换权限（字面闭环）**：`mp_schedule_permission` 加 `can_switch_company` 字段（`can_view_statistics=1 且 can_switch_company=1` 才允许带非本司 companyId）；SQL=`../sql/2026-09-10_mp_employee_switch_company.sql`；ApiPermissionPathSupport 加 `ABILITY_SWITCH_COMPANY`；MiniAppDashboardController 的 guardCompany/guardGroup 在 employee 请求带非本司 companyId 时额外校验该能力（缺则 401「未授予可切换公司权限」）；IMiniAppPermissionService/MpPermissionSaveBO/MiniAppBatchPermissionSaveBO 加 canSwitchCompany 字段；hr_web MiniappPermission.vue 加「可切换公司」开关列（依赖「数据统计」开关），批量设置面板同字段。
  - **2b /mp 看板端点补齐**：MiniAppDashboardController 新增 `POST /mp/dashboard/personnel/detail` → `dashboardService.personnelPageList`（复用现有 PC 服务）；离职原因TOP/各部门入职vs离职/部门人均应发/集团薪酬明细 4 项已在 2026-09-09/10 端点矩阵中齐备（flow/quitDist + flow/deptCompare + salary/deptCompare + group/salary），本次无需新增。
  - **3 图表类型对齐演示.html**：
    * donut/ring：性别结构 / 离职类型分布 / 薪酬成本构成 → `type="ring"` + `ringOpts`（uCharts 环形）
    * hbar：各部门人数分布 → `type="bar"` + `hbarOpts`（uCharts 横向 bar）
    * groupbar：各部门应发/实发 + 各部门入职/离职对比 → `type="bar"` + `groupbarOpts`（多 series 自动分组）
    * progress：绩效完成率 → 自绘进度条 UI（既有 progress-list）
    * radar：绩效综合 → 既有 radar
    * 折叠 + 员工明细：人员看板底部加 `.collapse-card` 折叠区，展开时调 `getPersonnelDetail` 分页显示姓名/部门/岗位/入职日期/状态；支持翻页。
  - **4 部门统计编制真实化**：hrm_dept 加 `plan_num` 字段（SQL=`../sql/2026-09-10_hrm_dept_plan_num.sql`）；`HrmDept`/`HrmDept`(po)/`AddDeptBO`/`DeptVO` 加该字段；`/mp/dashboard/deptOverview` 返回每行带 `planNum`（NULL=未配置）；miniapp deptStatistics.vue 取 `planNum` 计算缺编/超编/已满角标（颜色：橙/红/绿）+ 进度条上编制位置竖线；hr_web 部门维护 `Add.vue`/`Edit.vue` 加「部门编制（人）」`el-input-number` 输入框，0=未配置；`HrmDeptServiceImpl.addOrUpdate` 编辑时若前端未传 planNum(null) 保留原值，避免误清空。
- 2026-09-09 小程序权限改员工级 + 菜单合并：原挂菜单的 /mp/dashboard、/mp/mySchedule*、/mp/schedule/query 改为按员工在 `mp_schedule_permission` 的能力字段判定（ApiPermissionPathSupport.ABILITY_* + CompanyInterceptor 员工能力分支，严格模式）；新增三能力开关（can_schedule/can_view_statistics/can_load_schedule）；菜单 1021 改名「小程序权限」，删除 1030/5000/5020 三菜单及其角色授权；新增批量设置接口 /mpPermission/batchSave。迁移脚本 `../sql/2026-09-09_mp_employee_permission.sql`。详见 `miniapp/docs/完成情况.md`。
- 2026-09-07：新增 `/mp/schedule/batchSetRestDay` 批量设置休息日接口——复用 `IWorkPlanService.batchSetRestDay`，将生产体系月休四天的所有在职员工当日设为休息；小程序添加排班页日期选择弹窗二选一（休息/继续排班）。
- 2026-09-06 入口显隐精确化+done 并集口径（第二十八轮）：`/mp/isSupervisor` 拆 `{isSupervisor, canSchedule}`（恢复"有下属"原义）；`listToApprove` done=我批的∪可见已处理（去重、approveTime 倒序），pending 不变。小程序 index.vue/addschedule.vue 同步改。详见 `miniapp/docs/完成情况.md` 第二十八轮。

## 历史摘要
- 2026-09-09（被本轮替代）：曾将 /mp/dashboard/* 映射 PC「数据统计」菜单做公司级开关（tb_api_permission + 2026-09-09_miniapp_dashboard_api_permission.sql / 2026-09-09_miniapp_dashboard_permission.sql），后改为员工级，该映射已随迁移删除；**这两个 dashboard 脚本已改名 `.abandoned.sql`，切勿执行**。
- 2026-09-06 弃用钉钉排班推送+排班域弃用 tbattendanceuser+映射前置+本地考勤判定：`hrm.dingtalk.schedule-push.enabled` 默认关；排班身份一律取 hrm_employee.dingtalk_user_id（保存时姓名+手机号映射，查无此人拒保存，`/hrmEmployee/remapDingTalkUser` 手动补齐）；读侧双键兼容历史 employeeId 键行；新表 hrm_attendance_judge_result/card_repair（阈值接入考勤规则），同步考勤后自动重算。详见 `miniapp/docs/完成情况.md` 第二十七轮。
- 本模块历史细节统一沉淀在 `miniapp/docs/完成情况.md` 与 `miniapp/docs/待办清单.md`；hainan 侧如新增 /mp/* 接口，须同步 hr_miniapp/docs/development.md 契约表（AGENTS.md 硬性约定 #1）。
