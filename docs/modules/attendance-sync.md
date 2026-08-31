# 考勤同步（钉钉 sync/syncAll / 打卡 / 考勤组 / 审批数据获取 / 每月 1 号集中同步）

菜单前缀：`/attendanceData`（同步链路）、`/hrmAttendanceApproval`（审批数据）、`/hrmAttendanceEmpMonthRecord`（打卡概况）、`/dingTalkApiUsage`。

## 需求要点
- 同步入口 `/attendanceData/sync|syncAll` 是唯一允许调钉钉并落库的边界；展示查询（排班管理、打卡记录、审批数据列表）只读本地快照，禁止实时调钉钉。
- 审批数据：数据源限定本地 `tbattendanceapprove`；手工"获取审批数据"按 月份→员工→审批实例→详情 拉取，只入库 `status=COMPLETED && result=agree` 的实例；重抓按员工清理陈旧快照（基于 `employeeId→userId`，禁按姓名）；支持手工添加/删除/时长与子类型修正/统计状态（`取消至统计`）行内维护。
- 员工映射：禁按"同名+无排班"破坏性清理；去重以 userId 为主键；员工表 `dingtalk_user_id` 需经钉钉通讯录校验，缺失/失效按"姓名+手机号"唯一反查后写回。
- 钉钉调用量：`/dingTalkApiUsage/monthly` 读本地 `postresultlog` 统计（月额度 `dingtalk.api.monthly-limit` 默认 10000、阈值 80%），不在线请求钉钉。
- 每月 1 号集中同步（硬性要求）：≤8 家公司同一时间触发同步/审批获取，各公司数据必须落各公司租户库，互不干扰、不允许排队等待；同公司重复触发当场拒绝。

## 设计与契约
- 钉钉接口与落库表详细说明：`../reference/钉钉接口及数据库.md`（业务阅读版以同名 `.xlsx` 为准）。
- 7 步链路（`HrmAttendanceDataServiceImpl#SyncDataWithResume`）：①考勤组+班次（`hrm_attendance_group(_relation_*)`、`hrm_attendance_shift`，快照整体替换、先拉全量成功才落库）→②用户映射（`tbattendanceuser`）→③排班计划（`hrm_attendance_plan`）→④打卡明细（`hrm_attendance_clock`+`tbattendancedetail`）→⑤报表字段→⑥删旧报表→⑦报表数据回灌（`hrm_attendance_report_data`）。`date_shift/emp_schedule/history_shift` 不属于本链路直写。
- 审批抓取：按目标员工 `process/listbyuserid` 解析流程编码（管理员模板已退出主链路）→ `processinstance/listids` → `processinstance/get` → 解析落库；需应用权限 `qyapi_aflow`；业务时间二次过滤所选月份；完成标记表 `hrm_attendance_approval_fetch_mark`（不再作前置拦截）。
- 集中同步方案（已实施，源文档 `../archive/考勤同步并发风险与优化方案-2026-08-30.md`）：接口提交路径同步抢占按公司锁后立即返回 `{queued:true, queuedAt}`——考勤锁 Redis `attendance:sync:running:<companyId>`（TTL 2h，finally 释放），审批为 JVM 内按公司标记（2h 陈旧自动放行）；全部 11 个进度键追加 `:companyId`；托管线程池 `AttendanceSyncTaskLauncher` 核心=上限=8/队列 8，AbortPolicy 队列满返回"繁忙"；失败自动重试（默认 2 次，指数退避 60s→5min，考勤断点续传、审批幂等重拉；参数类/数据类错误不重试），前端轮询上限 60 分钟。明确不做：不拆微服务、不分库、不改内部限流参数（PARALLEL_THREADS=2、50 人批量、100ms 节流）。
- 钉钉限流对照（8 家并发）：每应用 20 QPS 独立不叠加；同源 IP ≈500 QPS 上限余量 ≥5 倍；接口全局池阈值不可证实但有退避+断点兜底；月配额每应用独立，靠 80% 用量提醒。
- 打卡概况：历史月不得用 `hrm_employee.create_time` 拦截；缺卡补 `-3`；同日多次打卡返回完整时间线；月查询不固定 `clock_stage=1`。

## 近期变更
- 2026-08-31 排班分页节流（钉钉限流对照落地）：`AttendancePlanRecord` 三处 `attendance/listschedule` 分页循环补 100ms sleep，单公司峰值压至 ≈10 QPS（应用维度上限 20 QPS）；限流维度对照见上方方案节。
- 2026-08-30 每月 1 号集中同步实施（含失败自动重试，详见上方方案节）：`/sync|/syncAll|/fetchMonthData` 异步化 + 按公司锁 + 进度键隔离 + `AttendanceSyncTaskLauncher`；`SyncData` 内部断点/重试逻辑零改动；前端完成信号改为"轮询 done 且 `updateTime >= queuedAt`"。
- 2026-08-30 性能第一批（同步部分）：`AttendanceUserManager` 在职/离职分页失败 1s×3 重试后抛 `ApiException` 快速失败（禁死循环、禁半截数据落库），事务只包落库段；`AttendanceGroupManager` 同模式重构；同步 Record 类的 `users` 单例共享字段全部取消改参数传递；共享 `SimpleDateFormat` 改 ThreadLocal；同步防重入 Redis 锁（后被 launcher 按公司锁收编）。
- 2026-08-03 审批抓取韧性与进度：单员工预解析失败记录并跳过不中断全员；缺 `dingtalk_user_id` 回退"经校验的 tbattendanceuser 映射"；`isv.limitedFrequency` 翻译为中文限流提示；同步考勤与审批获取统一进度接口（`SUCCESS/FAILED` 保留、中文错误文案、断点续传仅在参数完全一致时启用）。

## 历史摘要
- 2026-04-03/04：同步实时进度接口与参数校验；大批量同步连接稳态（`DataSourcePoolConfigurator`、连接重试）；考勤同步漏人专项（禁 DeleteRepeatUser、normalizeUsers、失效映射清理）。
- 2026-04-09：sync/syncAll 入库链路梳理与"仅最新快照"口径确认；2026-04-13：钉钉月额度超限不污染本地快照。
- 2026-05-08~15：审批数据功能上线、手工获取全链路（权限错误翻译、月内过滤、陈旧清理、时间列去 ON UPDATE）、钉钉调用量放大排查与最小化优化（日期级批量排班、50 人批、任务级流程编码缓存）。
- 2026-07-12~15：审批时长解析（durationInHour 优先、小数天纠偏）、通用审批误判修复、员工可见模板解析、COMPLETED+agree 过滤、statisticsStatus、重抓清理 @Modifying 返回类型热修。
- 全部细节见 `../archive/changelog.md` 与 `../archive/development-2026-08-30.bak.md`。
