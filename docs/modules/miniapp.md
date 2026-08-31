# 小程序后端（/mp/* 接口）

菜单前缀：`/mp/*`（无 PC 菜单）。本模块的完整接口契约、页面与历轮改动以小程序项目文档为准，本文只记录 hainan 后端侧要点，两份文档互相引用：
- 接口契约与小程序端实现：`排班小程序/hr_miniapp/docs/development.md`（MiniAppController 契约表）
- 项目状态与历轮交接：`排班小程序/docs/完成情况.md`

## 需求要点
- 微信员工端"我的排班"：手机号一键登录/多公司绑定（`/mp/login`、`/mp/login/bindCompany`）、月历排班（`/mp/mySchedule`、`/mp/mySchedule/day`）、改班申请与审批（`/mp/application/submit|myList|cancel|toApprove|approve`）、生产排班（`/mp/schedule/*`，仅上级可见 `/mp/isSupervisor`）。
- 生产排班与 PC 排班管理共用 `tbplanlist`（2026-08-30 统一改造，AGENTS.md 硬性约定 #5）：保存复用 PC 的 `IWorkPlanService.saveEmployeeDayAssignments` 按员工拆分，时间段物化到 `hrm_workplan_custom_shift`；按天整表替换（后端 diff 清理未含员工），同员工同日禁止"生产班+休息"并存；员工池与 PC 排班口径一致（在职 `is_del=0` 且 `entry_status in (1,3,4)`）。
- 安全加固四项（详见 完成情况.md"第五轮：安全加固"与 待办清单）：JWT 密钥硬编码待办 #18、登录限流、审批人校验（仅被指派审批人可 `/mp/application/approve`）、敏感值不入源码。改任何登录/token/权限逻辑前先读完成情况.md。
- 契约形态：全部 POST；`/mp/*` 用 `application/x-www-form-urlencoded`（唯一例外 `/mp/schedule/save` 为 JSON body）；token 请求头 `token`（裸 JWT，无 Bearer）。

## 设计与契约
- 后端入口：`MiniAppController`（响应 data 结构见 hr_miniapp 文档契约表）。
- 鉴权：`CompanyInterceptor` 三分支的第一分支——`/mp/*` 仅校验 token（员工 token 可访问）；系统库 `hrsystem.miniapp_user_binding` 保存微信用户↔员工↔公司绑定。
- 鉴权失败统一 HTTP 200 + `success:false`（非 401）。
- 数据路由：按绑定公司 `CompanyContext` 落对应租户库，与 PC 共用同一批表（tbplanlist、hrm_workplan_custom_shift、员工/审批相关表）。
- **部署依赖的建表脚本**（生产执行前先验证是否已建）：`../sql/miniapp_tables.sql`（系统库 `miniapp_user_binding` + 各租户库 `hrm_workplan_application`，幂等可重跑）；`../sql/2026-08-14_miniapp_workplan_application.sql` 为同名历史版本。无其他新表依赖——生产排班共用已有 `tbplanlist`。

## 近期变更
- 2026-08-30 前后端联调审计修复：request.js 重写（form-urlencoded、token 头、鉴权失败跳登录、防重复提交）；detail/approve 字段对齐契约；index 月历缓存与翻月竞态；login code 刷新与多公司弹窗；`/mp/schedule/save` 与 PC tbplanlist 数据统一第四轮落地。
- 更早轮次（登录绑定、月历、申请审批、审批人校验、登录限流等）见 `排班小程序/docs/完成情况.md` 历轮记录，hainan docs 不重复维护。

## 历史摘要
- 本模块历史细节统一沉淀在 `排班小程序/docs/完成情况.md` 与 `排班小程序/docs/待办清单.md`；hainan 侧如新增 /mp/* 接口，须同步 hr_miniapp/docs/development.md 契约表（AGENTS.md 硬性约定 #1）。
