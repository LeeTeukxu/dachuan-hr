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
- 登录：小程序 `wx.login` 的 code 经 `code2Session` 换 OpenID；租户库 `hrm_employee.openid`（`ascii_bin`）是映射事实源。未绑定时，员工按公司+姓名+完整证件号码核验一次，并通过条件 UPDATE 原子写入 OpenID；同名同证件号多条时拒绝绑定。ticket 存 Redis、TTL 600 秒，支持多实例严格过期。
- 鉴权：`CompanyInterceptor` 三分支的第一分支——`/mp/*` 命中 `tb_api_permission` 映射的路径走菜单权限（2026-09-06 起排班数据加载 3 个接口）；映射表内其余 `/mp/*` 仅校验 token（员工 token 可访问）；命中映射且员工 token 无 menuTree 时按租户级判定（任一角色勾选所需菜单即开通，`LoginUserMapper.countTenantMenuPermission`）。成功匹配员工后签发包含 companyId/employeeId 的 JWT，并由 `CompanyContext` 路由租户库。
- 权限体系（2026-09-06）：①排班数据加载=租户级开关（tbmenu 5000/5020 隐藏节点，权限页默认勾选）；②添加排班=员工级（租户库 `mp_schedule_permission` 有记录即有权限，PC 配置页 `/mpPermission/*` + hr_web 系统管理"排班小程序权限"页）；③审批可见范围=员工三档（1直属下属默认/2全部/3自定义 `mp_schedule_visible_employee`），审批动作仍限直属上级；④`/mp/application/submit|myList|cancel` 不进权限体系。
- 鉴权失败统一 HTTP 200 + `success:false`（非 401）。
- 数据路由：按绑定公司 `CompanyContext` 落对应租户库，与 PC 共用同一批表（tbplanlist、hrm_workplan_custom_shift、员工/审批相关表）。
- **部署依赖 SQL**：先对全部现有租户执行 `../sql/2026-09-02_hrm_employee_openid.sql`（`hrm_employee.openid VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL` + 租户内唯一索引），再部署后端和小程序；新租户基线已内建同字段。历史申请表脚本仍见 `../sql/miniapp_tables.sql`。排班小程序权限见 `../sql/2026-09-06_miniapp_schedule_permission.sql`（存量租户两表+菜单+角色映射、主库 tb_api_permission 4 行，执行后热刷新权限映射）。

## 近期变更
- 2026-09-06 入口显隐精确化+done 并集口径（第二十八轮）：`/mp/isSupervisor` 拆 `{isSupervisor, canSchedule}`（恢复"有下属"原义）；`listToApprove` done=我批的∪可见已处理（去重、approveTime 倒序），pending 不变。小程序 index.vue/addschedule.vue 同步改。详见 `miniapp/docs/完成情况.md` 第二十八轮。
- 2026-09-06 弃用钉钉排班推送+排班域弃用 tbattendanceuser+映射前置+本地考勤判定：`hrm.dingtalk.schedule-push.enabled` 默认关；排班身份一律取 hrm_employee.dingtalk_user_id（保存时姓名+手机号映射，查无此人拒保存，`/hrmEmployee/remapDingTalkUser` 手动补齐）；读侧双键兼容历史 employeeId 键行；新表 hrm_attendance_judge_result/card_repair（阈值接入考勤规则），同步考勤后自动重算。详见 `miniapp/docs/完成情况.md` 第二十七轮。
- 2026-09-06 直属下属档追加员工：范围 1 可见集合=直属下属∪追加明细（save/list 同步支持），`checkApprovalPermission` 放行名单内跨部门审批；`listToApprove` 范围 1 按员工集合查询。PC 配置页范围 1 开放"追加员工"穿梭框。详见 `miniapp/docs/完成情况.md` 第二十六轮。更早轮次（自定义穿梭框 25、权限体系 23、登录绑定等）见 `miniapp/docs/完成情况.md` 历轮记录。

## 历史摘要
- 本模块历史细节统一沉淀在 `miniapp/docs/完成情况.md` 与 `miniapp/docs/待办清单.md`；hainan 侧如新增 /mp/* 接口，须同步 hr_miniapp/docs/development.md 契约表（AGENTS.md 硬性约定 #1）。
