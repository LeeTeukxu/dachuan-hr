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
- 集中同步方案（已实施，源文档 `../archive/考勤同步并发风险与优化方案-2026-08-30.md`）：接口提交路径同步抢占按公司锁后立即返回 `{queued:true, queuedAt}`——考勤锁 Redis `attendance:sync:running:<companyId>`（TTL 2h，finally 释放），审批为 JVM 内按公司标记（2h 陈旧自动放行）；全部 11 个进度键追加 `:companyId`；托管线程池 `AttendanceSyncTaskLauncher` 核心=上限=8/队列 8，AbortPolicy 队列满返回"繁忙"；失败自动重试（默认 2 次，指数退避 60s→5min，考勤断点续传、审批幂等重拉；参数类/数据类错误不重试），前端轮询上限 60 分钟。浏览器 F5 只中断旧页面轮询，不取消后台任务、不释放公司锁；新页面加载不查询进度，用户再次点击"同步考勤"时才按当前公司查询，检测到 `RUNNING` 后恢复进度弹窗。明确不做：不拆微服务、不分库、不改内部限流参数（PARALLEL_THREADS=2、50 人批量、100ms 节流）。
- 钉钉限流对照（8 家并发）：每应用 20 QPS 独立不叠加；同源 IP ≈500 QPS 上限余量 ≥5 倍；接口全局池阈值不可证实但有退避+断点兜底；月配额每应用独立，靠 80% 用量提醒。
- 打卡概况：历史月不得用 `hrm_employee.create_time` 拦截；缺卡补 `-3`；同日多次打卡返回完整时间线；月查询不固定 `clock_stage=1`。

## 近期变更
- 2026-09-02 数据源稳定性修复：`DynamicDataSource` 新增 `HikariDataSource.isClosed()` 检测，evictor 驱逐关闭池后自动从 `OX` 移除并重建，修复"HikariDataSource has been closed"导致的数据加载失败；`CompanyDataSourceProvider` 新增活跃租户集合（`markActive/markInactive`），后台任务期间 evictor 跳过该租户防驱逐；`AttendanceSyncTaskLauncher.submit()` 在任务开始/结束时注册/注销活跃租户；`tbattendanceapprove.userId` 与 `tbattendancerecord.userId` 从 `varchar(20)` 扩大到 `varchar(100)`（匹配 `tbattendanceuser`），修复钉钉 userId 超长导致的 Data truncation。
- 2026-09-02 进度追踪统一化：考勤同步与审批获取进度追踪统一使用 `ProgressTracker` 工具类（Redis Hash 存储，字段级 TTL 24h）；心跳检测由 `ProgressHeartbeatTask` 定时扫描（60s 间隔，10min 超时自动标记 FAILED，可通过 `hrm.sync.heartbeat-timeout-ms`/`hrm.sync.heartbeat-enabled` 配置）；审批获取删除冗余 JVM 锁 `FETCH_RUNNING_MAP`，改用 Redis；前端轮询从固定 800ms 改为动态递增（1s→2s→3s→5s→8s）；新建 `useProgressPolling` composable 公共轮询组件，员工页和审批页共用；两个页面均新增取消按钮（RUNNING 状态下显示）和 F5 刷新自动恢复。
- 2026-09-01 同步可靠性修复：进度 Redis 值统一字符串写入/读取，解决重试时间戳与阶段值的序列化失败；公司锁记录企业名、账号、姓名和建立时间，重复提示明确显示企业（名称+ID）及当前操作账号。新版残留锁在两种情况下由下一次提交立即回收：①进度为 `SUCCESS/FAILED` 且终态更新时间晚于锁建立时间；②单机服务重启后，锁建立时间早于当前 JVM 启动时间（重启已中断原后台线程）。当前 JVM 内的 `RUNNING` 锁仍保持互斥；旧版 `"1"` 锁缺少时间信息，仍按 TTL 或人工核实后清理。前端轮询超时不再伪装成"完成"，且 F5 后不自动弹共享进度，只有再次点击"同步考勤"并检测到当前公司 `RUNNING` 才恢复；新登录令牌补充 `companyName`，提交成功判断兼容 `success=true`/`code=200`。

## 历史摘要
- 2026-08-30~31：每月 1 号集中同步实施（按公司锁 + 进度键隔离 + `AttendanceSyncTaskLauncher`）；排班分页节流 100ms sleep 峰值压至 ≈10 QPS；失败自动重试（指数退避 60s→5min）。
- 2026-04-03~07-15：同步实时进度、大批量连接稳态、考勤同步漏人专项、审批数据功能上线、钉钉调用量优化、审批时长解析、通用审批误判修复。全部细节见 `../archive/changelog.md`。
