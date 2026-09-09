# Development Notes

## Architecture Overview
- 三端一系统：`hainan/`（Spring Boot 2.1.6 / Java 8，JPA + MyBatis-Plus，本地端口 29080、测试实例 29081）+ `hr_web/`（Vue3 + Element Plus PC 前端）+ `miniapp/`（uni-app Vue3，目标 mp-weixin）。
- 多租户动态数据源：业务表在各租户库 `hr_XXXX`（JPA/MyBatis 按 `CompanyContext` 路由，`hr_0001~hr_0005` 同实例分 schema）；系统库 `hrsystem` 只放跨租户数据（`tbCompanyList`、`miniapp_user_binding`、`tb_api_permission` 等）。租户连接元数据存 `hrsystem.tbCompanyList.url`，改密需同步。
- 鉴权：JWT（请求头 `token`，裸 token 无 Bearer）；鉴权失败返回 HTTP 200 + `success:false`（非 401）。拦截器四分支：① `/mp/*` 命中 `tb_api_permission` 映射（现仅 /mpPermission 配置页）走菜单/角色权限校验；② 命中 `ApiPermissionPathSupport.ABILITY_*`（/mp/dashboard→数据统计、/mp/mySchedule* 与 /mp/schedule/query→排班数据加载、/mp/schedule/save|employees|standardProducts|batchSetRestDay→添加排班）→ 按员工 `mp_schedule_permission` 字段判定（严格模式，未配置即拒，2026-09-09 起）；③ 其余 `/mp/*` 仅校验 token；④ 映射表外的非 `/mp` 路径要求操作员 token（account 非空），员工 token 一律拒绝。
- 统一排班语义：PC 添加排班/排班管理与小程序生产排班共用扁平事实表 `tbplanlist`（ProductName=产品、LinkName=岗位、UserID=逗号拼接员工、shift_source=standard/custom/rest、custom_shift_id→hrm_workplan_custom_shift），不建独立排班表。
- 全部功能文档见 `modules/`（每份 ≤150 行：需求要点 → 设计与契约 → 近期变更）。

## 功能模块索引
| 模块 | 文档路径 | 菜单前缀 | 关键端点 |
|---|---|---|---|
| 系统管理 | modules/system.md | /hrm/system、/manage、/mpPermission | /hrsystem/login、/hrsystem/switchCompany、/tbLoginUser、/tbMenu |
| 员工管理 | modules/employee.md | /hrm/dept、/hrm/employee* | /hrmEmployee/queryPageList、exportDepartmentDetail |
| 排班与单双休 | modules/attendance-scheduling.md | /workPlan*、/hrmWorkweekSetting | /workPlan/saveAll、queryEmployeeDayAssignments |
| 考勤同步 | modules/attendance-sync.md | /attendanceData、/hrmAttendanceApproval | /attendanceData/sync、/hrmAttendanceApproval/fetchMonthData、/attendanceData/judgeRecompute、/attendanceData/judgeQuery（本地考勤判定） |
| 加班/夜班与考勤汇总 | modules/overtime.md | /hrmOvertimeNightStatistics、/hrmProduceAttendance | startStatistics、syncFromOvertimeNightStatistics |
| 薪资管理 | modules/salary.md | /hrmSalary*、/hrmPersonalIncomeTax | computeSalaryData、exportSalary |
| 奖金中心 | modules/bonus.md | /hrmBonus | importBonus、importTaxOnlyBonus |
| 社保管理 | modules/insurance.md | /hrmInsurance* | computeInsuranceData、updateSalaryBasicInsuranceAmount |
| 数据与运行配置 | modules/dataconfig.md | /dict、/tbCompanyList、/report | /dict/add、跨域/备份/日志清理 |
| 小程序后端 | modules/miniapp.md | /mp/* | /mp/login、/mp/mySchedule、/mp/schedule/save（排班小程序权限见 modules/miniapp.md 与 hr_web system.md） |

## 跨模块约定（仅当前生效规则）
- token：请求头 `token` 裸 JWT；PC 菜单权限在 `menuTree`（登录返回），接口权限经 `ApiPermissionPathSupport` 按 `tb_api_permission`/菜单前缀校验；退出 `/hrsystem/logout` 将 jti 写 Redis 黑名单，改密 `bumpSessionSeed` 使在途会话失效。
- 参数绑定：PC 接口大量使用 `application/x-www-form-urlencoded`/FormData（JSON body 仅在 `@RequestBody` 接口使用）；`/mp/*` 统一 form-urlencoded（唯一例外 `/mp/schedule/save` 为 JSON）。Tomcat `server.max-http-header-size=65536`（token 含菜单权限约 8KB）。
- 多租户路由：写操作依赖 `CompanyContext`；系统库账号索引等跨库操作须暂停外层租户事务并临时清空上下文（见 modules/system.md）。
- 契约兼容：不破坏既有接口路径与入参；雪花 ID 一律按字符串传输（18 位精度）；列表 Map 结果须显式驼峰别名；用户侧错误文案必须中文，技术细节只进日志；异步长任务统一"提交即返回 + 进度轮询"模式（同步考勤、审批获取、排班提交、薪资/社保核算均如此）。
- DDL 纪律：JPA `hbm2ddl.auto=none`，任何新列/新表先出 `docs/sql/` 幂等脚本并在 `hr_0001~hr_0005` 执行复核后再发布代码。
- 安全加固要点：敏感值不入源码；密码 BCrypt；导出 ZIP 走 AES-256 + `X-Archive-Password` 响应头（CORS 需暴露）。
- 算法知识库（hr_web 顶栏弹窗，纯前端）：算法口径数据维护在 `hr_web/src/constants/knowledgeBase.js`，**任何模块的算法/计算口径改动须同步该文件**（大白话描述，勿写代码细节），设计见 `hr_web/docs/modules/system.md`、Prompt 清单 7.10。

## 其他文档
- `reference/PRD.md`：产品视角主文档（业务背景、角色、模块目标、验收口径）。
- `reference/钉钉接口及数据库.md` / `.xlsx`：sync/syncAll 同步链路的钉钉接口与落库表说明（业务阅读版以 xlsx 为准）。
- `sql/`（DDL 部署脚本）、`reference/saas-migration/`（env 样例）：SaaS 改造产物、DDL 脚本、专项设计文档与排查报告。
- `reference/employee-roster-field-comparison-2026-06-12.md`：花名册字段比对结论。
- `archive/`（含归档的 plans/、reports/、code-review 评审件）：历史轮次细节（`changelog.md` 按模块分节 + `development-2026-08-30.bak.md` 原文留底 + `考勤同步并发风险与优化方案-2026-08-30.md`）。

## Testing
- 后端定向回归：`mvn -Dtest=<TestClass> test`（常用：WorkPlanServiceImplTest、HrmOvertimeNightStatisticsServiceImplTest、SalaryMonthRecordServiceNewTest、HrmAttendanceApprovalServiceImplTest、HrmInsuranceSchemeServiceTest、HrmEmployeeServiceImplImportEmployeeTest、ApiPermissionPathSupportTest）。
- 后端全量：`mvn test`（基线：约 16 个存量失败与本次改动无关时视为通过）；编译校验 `mvn -DskipTests compile`。
- 前端：`node tests/<feature>.test.mjs` 逐功能脚本 + `npm run build`（既有 `::v-deep` 与 chunk size warning 可忽略）。
- 本地环境：MySQL `127.0.0.1:13306`（root/test123456，系统库 hrsystem + 租户库 hr_0001~0005）；dev 库 `153.0.237.98`；生产库拒外网直连，DDL 需在可访问机器执行。
