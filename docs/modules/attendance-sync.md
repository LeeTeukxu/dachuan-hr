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
- **部署依赖 SQL**：存量租户库需执行 `../sql/2026-09-05_tbattendanceapprove_duration_day.sql`（`tbattendanceapprove.durationDay` 时长(天)列；**推荐整库执行一次全租户遍历版 `../sql/2026-09-05_tbattendanceapprove_duration_day_all_tenants.sql`**，自动遍历全部 hr_数字 库、幂等可重跑——租户库缺该列时考勤汇总-下载行政体系考勤报 `Unknown column 'tbattendan0_.durationDay'`，2026-09-07 生产即因漏执行此脚本触发）。
- 7 步链路（`HrmAttendanceDataServiceImpl#SyncDataWithResume`）：①考勤组+班次（`hrm_attendance_group(_relation_*)`、`hrm_attendance_shift`，快照整体替换、先拉全量成功才落库）→②用户映射（`tbattendanceuser`）→③排班计划（`hrm_attendance_plan`）→④打卡明细（`hrm_attendance_clock`+`tbattendancedetail`）→⑤报表字段→⑥删旧报表→⑦报表数据回灌（`hrm_attendance_report_data`）。`date_shift/emp_schedule/history_shift` 不属于本链路直写。
- 审批抓取：按目标员工 `process/listbyuserid` 解析流程编码（管理员模板已退出主链路）→ `processinstance/listids` → `processinstance/get` → 解析落库；需应用权限 `qyapi_aflow`。**月/窗双模式**：老入口/定时任务=月模式，按目标业务月拉取并对业务日期二次过滤所选月份；前端手动=窗口模式（带 `fetchStartTime/fetchEndTime`），listids 按发起时间窗口 ≤120 天分片拉取、已通过审批全量幂等落库、不再按业务月丢弃（跨月单修复），列表/统计仍按业务 `beginTime` 过滤。完成标记表 `hrm_attendance_approval_fetch_mark`（不再作前置拦截）。
- 集中同步方案（已实施，源文档 `../archive/考勤同步并发风险与优化方案-2026-08-30.md`）：接口提交路径同步抢占按公司锁后立即返回 `{queued:true, queuedAt}`——考勤锁 Redis `attendance:sync:running:<companyId>`（TTL 2h，finally 释放），审批为 JVM 内按公司标记（2h 陈旧自动放行）；全部 11 个进度键追加 `:companyId`；托管线程池 `AttendanceSyncTaskLauncher` 核心=上限=8/队列 8，AbortPolicy 队列满返回"繁忙"；失败自动重试（默认 2 次，指数退避 60s→5min，考勤断点续传、审批幂等重拉；参数类/数据类错误不重试），前端轮询上限 60 分钟。浏览器 F5 只中断旧页面轮询，不取消后台任务、不释放公司锁；新页面加载不查询进度，用户再次点击"同步考勤"时才按当前公司查询，检测到 `RUNNING` 后恢复进度弹窗。明确不做：不拆微服务、不分库、不改内部限流参数（PARALLEL_THREADS=2、50 人批量、100ms 节流）。
- 钉钉限流对照（8 家并发）：每应用 20 QPS 独立不叠加；同源 IP ≈500 QPS 上限余量 ≥5 倍；接口全局池阈值不可证实但有退避+断点兜底；月配额每应用独立，靠 80% 用量提醒。
- 打卡概况：历史月不得用 `hrm_employee.create_time` 拦截；缺卡补 `-3`；同日多次打卡返回完整时间线；月查询不固定 `clock_stage=1`。

## 近期变更
- 2026-09-09 **审批抓取钉钉配额治理 L1-a(反查花名册缓存) + L1-b(用量监控补盲)**。纯后端，前端无改动。
  - **根因**：`fetchDingTalkUserIdByNameAndMobile`（反查缺号员工钉钉 userId）每次都调 `fetchOnJobDingTalkUserIds`(queryonjob 翻页拉全量在职) + `fetchDingTalkEmployeeProfiles`(v2/list 按50分批拉姓名手机号)，**每个缺号员工重复整轮拉全公司花名册**(N 倍浪费)；且这 4 处直连调用不经 `DingTalkApiTemplate`/`ddLogger`，`/dingTalkApiUsage` 用量页漏计(尤其 listids 大头)，改完也看不到降幅。
  - **L1-a**：`getRosterProfiles(token)` 按 token(=公司)懒加载并缓存全量在职花名册到内存(`rosterCacheByToken` + 每 token 锁，防 @Service 单例跨公司串数据)；本轮内所有缺号员工本地比对，N× 重复拉压成 1×。新增 `loadAllDingTalkEmployeeProfiles`。
  - **L1-b**：`listbyuserid`/`listids`/`queryonjob`/`v2/list` 4 处直连 `client.execute` 后各加 `ddLogger.Info(...)`(同 AttendanceUserManager:119 机制)，落 `postresultlog` → 用量页真实计数。保留原 100ms 串行间隔。
  - **本地库取证(6 租户)**：在职缺号人数 hr_0001=15 / hr_0004=11 / hr_0005=67(全员缺号,数据质量异常) / 其余0。按 `(缺号数−1)×2×⌈在职/50⌉` 估算，单次全量反查调用：改前 (15×6+11×6+67×4)=424 次 → 改后固定 6+6+4=16 次，**单次省≈408 次**；若每月 2~4 次全量≈月省 800~1600 次。
  - **注意**：L1-b 上线后用量页数字会**短暂上升**(此前 listids 大头未计入)，属监控补盲非回归，勿误判。真正大头 listids(~1200/次全量)需 L2-1 增量抓取才降，本轮未动。编译 `mvn -q compile` BUILD SUCCESS。
- 2026-09-09 **审批拉取 UI：范围/单点双模式 + 进度明细显示（不再干等）+ 开始日期限 1 个月**。
  - **双模式 radio（hr_web Index.vue 获取审批数据弹窗）**：`fetchTimeMode`='range'范围(开始日期→今天，穿梭框可空=全量，受月闸门) / 'single'单点补拉(单选某一天，强制勾选员工/部门)。**价值**：补历史单点只扫勾选员工，省历史段整段空探测(达川 165/275 无单员工不再被全量扫)。single 窗口=当天00:00~23:59，end 落回单点日→天然不触发"开始不得早于今天往前N月"护栏，可补任意历史。confirmFetchMonth 按模式分叉；targetMonth single 取单点日所在月。
  - **开始日期选择器限 1 个月（可选任意天，不锁 1 号）**：`MAX_FETCH_START_MONTHS_BEFORE` 3→1，`earliestAllowedFetchStartDate` 锚点=「上月 1 号」(如今天 9/9 则最早 8/1)、最晚=今天；区间外(含未来)禁用。即 range 模式开始日期可选**最近 1 个月内任意一天**(8/1~9/9 任选)，选哪天即从哪天起拉取整段，前后端均按用户选择、不篡改输入。`isFetchStartDateDisabled` **不再**锁每月 1 号(此前误解需求已实现后回退)。更早历史(上月 1 号之前)改走 single 单点补拉模式(精确到天、强制定向勾人)。后端 `hrm.approval-fetch.max-months-before` 默认保持 3 作 API 兜底；单点补拉 `disableFutureOnly` 不变(仅禁未来)。
  - **进度明细（界面弹窗 + 通知中心）**：根因=后端通知 content 只写 `进度 X%` 未带 message 员工明细。后端三处补 detail：`HrmAttendanceApprovalSyncServiceImpl.updateNotificationProgress(companyId,percent,message)`→`进度 X% · <message>`；`HrmAttendanceApprovalServiceImpl.updateNotificationProgress` 同构加 message；`HrmAttendanceDataServiceImpl` 加 `buildSyncDetail(步骤·已处理X/共Y员工)`。前端 `NotificationCenter.vue` 正则 `^进度 \d+%$`→`^进度 \d+%`(允许后缀，bar %照常 parse，progress-text 显示整串)；`ProgressDialog.vue` 加 `employeeProgress`(解析 message 显示当前员工/已处理X/共Y/剩余Z)。
  - 校验：后端 `mvn -o compile` BUILD SUCCESS；前端 `@vue/compiler-sfc` 校验 Index.vue/ProgressDialog.vue/NotificationCenter.vue 全 OK。
- 2026-09-09 **审批「获取审批数据」按员工断点续传 + 通用组件 + 通知中心「继续」按钮**（失败/中断重跑不从头）。**目标**：省用户等待（非省配额）；抽公用组件供其它批处理复用。设计文档：`../plans/2026-09-09-resumable-job-checkpoint-design.md`。
  - **通用组件 `common/ResumableJobCheckpoint`**（@Component, Redis, key `resumable:{biz}:{companyId}:{fingerprint|payload|processed|status}`, 默认24h TTL）：`begin/markProcessed/complete/fail/markTerminalFailed/hasResumable/getSavedPayload/clearAll` + `ResumeContext`。续传规则：fingerprint 不一致→从头；一致且 status∈{FAILED,RUNNING}→续传（跳过已处理员工）；SUCCESS→从头(刷新)。Redis 异常兜底。
  - **审批接入 `HrmAttendanceApprovalSyncServiceImpl`**：`doFetchMonthData` 入口 `buildApprovalJobKey(month,窗口,types,emps)`(canonical，兼作 fingerprint+payload) → `begin`；员工循环顶跳过已完成员工；每员工完成 `markProcessed(empId)`；整轮成功 `complete`。
  - **「过早 FAILED」修正（对齐考勤语义）**：断点终态 FAILED 从"每次尝试失败"上移到"自动重试全耗尽才置"——SyncServiceImpl finally 改为只清 ctx(`clearActiveResumeCtxIfNeeded`)，逐尝试失败断点留 RUNNING 供下次续传；终态 throw 在 `HrmAttendanceApprovalServiceImpl#fetchMonthDataWithAutoRetry` 调 `markTerminalFailed`。
  - **后端「继续」接口**：审批 `POST /hrmAttendanceApproval/continueFetch`(hasResumable→读payload重建BO→`submitApprovalFetch`，无可续 code4002)；考勤 `POST /attendanceData/continueSync`(getSyncProgress 判 status=FAILED+params→重建→SyncDataWithAutoRetry，无可续 code4002)。共用抽取 `submitApprovalFetch`。
  - **失败通知 content 规范化**：考勤 `saveFailedSyncProgress` 置运行中通知 content=`同步失败`(审批已是 `获取失败`)，修复失败后误判仍在轮询的 gap，供前端识别。
  - **前端**：`NotificationCenter.vue`(右上弹窗)+`NotificationList.vue`(/hrm/notification 查看全部页) 对可续传失败行(type203+获取失败 / type201+同步失败)显示「继续」，点它 `continueAttendanceSync`/`continueAttendanceApprovalFetch` 原地续传；4002→"暂无任务可继续"、202→"任务进行中"、否则 success+刷新(新运行通知被轮询展示)。
  - **考勤暂不迁组件**（其续传已跑生产且无此 bug，留待 L2）。验收补充见设计文档 9.6。

## 历史摘要
- 2026-09-05：撤销审批三轮修复定稿——`isApprovedProcessInstance` 三大作废判据齐备（撤销动作单/REVOKE+mainProcessInstanceId 替身/attached 非空被顶替原单），任一命中不入库并删残留快照；用户定稿"两层（业务级+单条审核）均通过才入库"。
- 2026-09-04 自动同步定时任务（默认关）+ 跨月抓取丢数据修复（V4）+ 农谷连接修复：①`DingTalkAutoSyncConfig`/`DingTalkAttendanceAutoSyncTask`/`DingTalkApprovalAutoSyncTask`（有界线程池、复用公司互斥锁），任务进行中接口返回 202；②「获取审批数据」新增**发起窗口入口**（5 参 `fetchMonthData(month,start,end,empIds,types)`，前端锁终点+开始日期弹窗），窗口内已通过审批全量幂等落库、不再按业务月丢弃（发起月≠业务月的单不再永久丢失），落库与展示解耦，详见 `../plans/2026-09-04-attendance-approval-cross-month-fetch-review.md`；③`tbattendanceapprove`/`tbattendancerecord`.`userId` 扩到 varchar(100)+补 openid（农谷 Hr_0003 修复）。
- 2026-09-02~03：通知中心实时进度、进度提前 100% 修复、三入口锁释放增强、Hikari 防驱逐；农谷公司 userId 列过窄/openid 缺失修复（全租户 `MODIFY COLUMN userId VARCHAR(100)` + 补 `openid`，baseline 已改）。
- 2026-08-30~31：每月 1 号集中同步实施（按公司锁 + 进度键隔离 + `AttendanceSyncTaskLauncher`）；排班分页节流 100ms sleep 峰值压至 ≈10 QPS；失败自动重试（指数退避 60s→5min）。
